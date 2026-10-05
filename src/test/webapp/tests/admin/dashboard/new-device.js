const { readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.new-device').tag('@singlethread');

const cookieName = 'wjAdminDevice';
const dialog = '.md-dashboard-modal--security';
const createdEvents = { tester: [], tester2: [] };
let originalCookie;
let currentAccount;

/** Uses the real login form without changing account settings or passwords. */
async function signIn(I, account = 'tester', logoff = true) {
    await I.relogin(account, logoff);
    currentAccount = account;
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 30);
}

/** Records only events created by this test so cleanup cannot acknowledge unrelated warnings. */
function rememberCreated(bootstrap, account, since) {
    for (const notice of bootstrap.notices.filter(item => item.kind === 'newDevice' && item.securityEvent.createdAt >= since)) {
        const id = notice.securityEvent.id;
        if (!createdEvents[account].includes(id)) createdEvents[account].push(id);
    }
}

function eventIds(bootstrap) {
    return bootstrap.notices.filter(item => item.kind === 'newDevice').map(item => item.securityEvent.id).sort();
}

/** Sends an owned action through the real CSRF-protected endpoint. */
async function postEvent(I, id, action, userId) {
    return I.executeScript(async ({ id, action, userId }) => {
        const query = userId == null ? '' : `?userId=${userId}`;
        const response = await fetch(`/admin/rest/security/login-events/${encodeURIComponent(id)}/${action}${query}`, {
            method: 'POST', headers: { 'X-CSRF-Token': window.csrfToken }
        });
        return { status: response.status, event: response.ok ? await response.json() : null };
    }, { id, action, userId });
}

/** Submits the same CSRF form as the administration's normal logout control. */
async function normalLogout(I) {
    await I.executeScript(() => document.forms.namedItem('adminLogoffForm').requestSubmit());
    await I.waitForVisible('#username', 20);
    currentAccount = null;
}

async function waitForSecurityDialog(I) {
    await I.waitForVisible(dialog, 10);
    await I.waitForFunction(() => {
        const root = document.querySelector('.md-dashboard-modal--security');
        return root && getComputedStyle(root).opacity === '1' && getComputedStyle(root.querySelector('.modal-dialog')).transform === 'none';
    }, 10);
}

/** Waits for the responsive sidebar and aligns a notice inside the administration's smooth scroller. */
async function showNotice(I, id) {
    const mobileMenuOpen = await I.executeScript(() => innerWidth < 1200 && document.querySelector('.ly-sidebar').classList.contains('active'));
    if (mobileMenuOpen) await I.clickCss('.js-sidebar-toggler');
    await I.waitForFunction(() => innerWidth >= 1200 || document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
    await I.executeScript(id => {
        const element = document.querySelector(`[data-notice-id="newDevice:${id}"]`);
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        scrollbar.setPosition(0, scrollbar.offset.y + element.getBoundingClientRect().top - 100);
    }, id);
    await I.waitForFunction(id => {
        const bounds = document.querySelector(`[data-notice-id="newDevice:${id}"]`).getBoundingClientRect();
        return bounds.top >= 48 && bounds.bottom <= innerHeight;
    }, [id], 10);
}

/** Real authentication verifies detection, cookie renewal, recovery actions and account isolation. */
Scenario('Real browser recognition, login warnings and protected account actions', async ({ I }) => {
    const started = Date.now();
    await signIn(I);
    originalCookie = await I.grabCookie(cookieName);
    const baseline = await I.executeScript(readDashboardBootstrap);
    rememberCreated(baseline, 'tester', started);
    const testerId = await I.executeScript(() => window.currentUser.userId);
    await I.assertTrue(Boolean(originalCookie), 'A completed administrator login must issue a recognition cookie.');

    await normalLogout(I);
    await I.assertTrue((await I.grabCookie(cookieName)).value === originalCookie.value, 'Normal logout must retain browser recognition.');
    await I.setCookie({ ...originalCookie, value: 'expired-autotest', expires: 1 });
    await I.dontSeeCookie(cookieName);
    const freshStarted = Date.now();
    await signIn(I, 'tester', false);
    const fresh = await I.executeScript(readDashboardBootstrap);
    rememberCreated(fresh, 'tester', freshStarted);
    const newIds = eventIds(fresh).filter(id => !eventIds(baseline).includes(id));
    await I.assertEqual(newIds.length, 1, 'A fresh browser must create exactly one new login event.');
    const eventId = newIds[0];
    const event = fresh.notices.find(item => item.securityEvent?.id === eventId).securityEvent;
    const cookie = await I.grabCookie(cookieName);
    const secureOrigin = await I.executeScript(() => location.protocol === 'https:');
    await I.assertTrue(/^[A-Za-z0-9_-]{43}$/.test(cookie.value), 'The cookie must contain an opaque random token.');
    await I.assertTrue(cookie.value !== originalCookie.value, 'Clearing the cookie must create a different browser identity.');
    await I.assertTrue(cookie.httpOnly, 'JavaScript must not be able to read the device cookie.');
    await I.assertEqual(cookie.sameSite, 'Lax');
    await I.assertEqual(cookie.path, '/');
    await I.assertEqual(cookie.domain, await I.executeScript(() => location.hostname), 'Recognition cookies must be scoped to the current host.');
    await I.assertEqual(cookie.secure, secureOrigin, 'HTTPS requires Secure; local HTTP must remain testable.');
    await I.assertTrue(cookie.expires > Date.now() / 1000 + 89 * 86400 && cookie.expires < Date.now() / 1000 + 91 * 86400,
        'The default cookie lifetime must be ninety days.');
    await I.assertFalse(await I.executeScript(() => document.cookie.split(';').some(cookie => cookie.trim().startsWith('wjAdminDevice='))), 'HttpOnly must also hold in the actual document.');
    await I.assertTrue(Boolean(event.browserName && event.browserVersion && event.operatingSystem && event.ipAddress), 'The event must contain the actual login snapshot.');
    await I.assertEqual(event.expiresAt - event.createdAt, 7 * 86400000);
    const row = `[data-notice-id="newDevice:${eventId}"]`;
    await I.see(event.browserName, row);
    await I.see(event.browserVersion, row);
    await I.see(event.operatingSystem, row);
    await I.see(event.ipAddress, row);

    for (const width of [1440, 390]) {
        await I.resizeWindow(width, 1000);
        await showNotice(I, eventId);
        await I.assertTrue(await I.executeScript(id => {
            const element = document.querySelector(`[data-notice-id="newDevice:${id}"] .md-dashboard__notice-row`);
            return element.scrollWidth <= element.clientWidth + 1;
        }, eventId), `The real warning must fit ${width}px.`);
        await I.saveScreenshot(`dashboard-new-device-banner-${width}.png`);
        await I.clickCss(`${row} .md-dashboard__notice-report`);
        await waitForSecurityDialog(I);
        await I.assertTrue(await I.executeScript(() => {
            const body = document.querySelector('.md-dashboard-modal--security .modal-body');
            return body.scrollWidth <= body.clientWidth + 1;
        }), `The security dialog must fit ${width}px.`);
        await I.saveScreenshot(`dashboard-new-device-dialog-${width}.png`);
        await I.clickCss(`${dialog} .modal-footer button:last-child`);
        await I.waitForDetached(dialog, 10);
    }
    await I.wjSetDefaultWindowSize();
    await showNotice(I, eventId);

    const rejected = await I.executeScript(async id => {
        const results = [];
        for (const action of ['confirm', 'report']) {
            for (const token of [null, 'autotest-invalid-csrf']) {
                const response = await fetch(`/admin/rest/security/login-events/${id}/${action}`, {
                    method: 'POST', headers: token ? { 'X-CSRF-Token': token } : {}
                });
                results.push({ action, status: response.status });
            }
        }
        return results;
    }, eventId);
    for (const result of rejected) await I.assertEqual(result.status, 403, `${result.action} must reject missing or invalid CSRF tokens.`);
    const unchanged = (await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}`)).requestedSecurityEvent;
    await I.assertTrue(unchanged.confirmedAt == null && unchanged.reportedAt == null, 'Rejected actions must preserve the event.');

    await I.clickCss(`${row} .md-dashboard__notice-confirm`);
    await I.waitForInvisible(row, 10);
    const confirmed = await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}`);
    await I.assertTrue(confirmed.requestedSecurityEvent.confirmedAt > 0);
    const confirmedIds = eventIds(confirmed);
    await normalLogout(I);
    await I.assertTrue((await I.grabCookie(cookieName)).value === cookie.value, 'Logout must preserve the fresh recognition token.');
    await I.amOnPage(`/admin/v9/?securityEvent=${eventId}`);
    await I.waitForVisible('#username', 10);
    await signIn(I, 'tester', false);
    await waitForSecurityDialog(I);
    const repeated = await I.executeScript(readDashboardBootstrap);
    await I.assertDeepEqual(eventIds(repeated), confirmedIds, 'A regular login from the recognized browser must not create another event.');
    const renewed = await I.grabCookie(cookieName);
    await I.assertTrue(renewed.value === cookie.value && renewed.expires > cookie.expires, 'Successful login must retain and extend the recognition cookie.');
    await I.see(event.browserName, `${dialog} .md-dashboard-sessions__security`);
    const sessionBeforeReport = repeated.currentSessions.currentSessionId;
    await I.clickCss(`${dialog} .md-dashboard-sessions__report`);
    await I.waitForVisible(`${dialog} .md-dashboard-sessions__security [role="status"]`, 10);
    await I.clickCss(`${dialog} .modal-footer button:last-child`);
    await I.waitForDetached(dialog, 10);
    await I.seeElement(row);
    const reported = await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}`);
    await I.assertTrue(reported.requestedSecurityEvent.reportedAt > 0 && reported.requestedSecurityEvent.confirmedAt == null);
    await I.assertEqual(reported.currentSessions.currentSessionId, sessionBeforeReport, 'Reporting must not sign out the current session.');
    await normalLogout(I);
    const afterReportStarted = Date.now();
    await signIn(I, 'tester', false);
    const afterReport = await I.executeScript(readDashboardBootstrap);
    rememberCreated(afterReport, 'tester', afterReportStarted);
    const refreshed = afterReport.notices.find(item => item.securityEvent?.id === eventId)?.securityEvent;
    await I.assertTrue(Boolean(refreshed), 'A reported browser must renew the warning on the same device.');
    await I.assertTrue(refreshed.createdAt > event.createdAt && refreshed.confirmedAt == null && refreshed.reportedAt == null,
        'The next login must replace the previous notice and clear its acknowledgment.');

    const otherStarted = Date.now();
    await signIn(I, 'tester2');
    const other = await I.executeScript(readDashboardBootstrap);
    rememberCreated(other, 'tester2', otherStarted);
    for (const action of ['confirm', 'report']) {
        await I.assertEqual((await postEvent(I, eventId, action, testerId)).status, 404, `Another account must not ${action} the tester event, even with a forged userId.`);
    }
    const foreign = await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}&userId=${testerId}`);
    const absent = await I.executeScript(readDashboardBootstrap, '?securityEvent=9223372036854775807');
    await I.assertTrue(foreign.securityEventRequested && absent.securityEventRequested);
    await I.assertEqual(foreign.requestedSecurityEvent, null);
    await I.assertEqual(absent.requestedSecurityEvent, null, 'Missing and foreign events must expose the same neutral result.');
});

/** Leaves only the test's own event acknowledgments and restores its original recognition cookie. */
Scenario('Restore recognition cookie and acknowledge only login events created by this test', async ({ I }) => {
    for (const account of ['tester2', 'tester']) {
        if (!createdEvents[account].length) continue;
        if (currentAccount !== account) await signIn(I, account);
        for (const id of createdEvents[account]) {
            const result = await postEvent(I, id, 'confirm');
            await I.assertEqual(result.status, 200, 'Cleanup may acknowledge only the signed-in account\'s generated events.');
        }
    }
    if (currentAccount !== 'tester') await signIn(I);
    if (originalCookie) await I.setCookie(originalCookie);
    await I.wjSetDefaultWindowSize();
    await I.amOnPage('/admin/v9/');
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 30);
});
