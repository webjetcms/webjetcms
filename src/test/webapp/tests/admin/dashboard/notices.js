const { dashboardPageRoute, mockDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.notices').tag('@singlethread');

const preferencesRoute = '**/admin/rest/admin-settings/';
const codeRoute = '**/admin/rest/security/login-events/*/code';
const confirmRoute = '**/admin/rest/security/login-events/*/confirm';
const reportRoute = '**/admin/rest/security/login-events/*/report';
const historyRoute = '**/admin/rest/sessions/login-history*';
const administratorsRoute = '**/admin/rest/sessions/administrators';
const locationRoute = /^https:\/\/(ipwho\.is|ipwhois\.pro)\//;
const notice = (id, severity) => ({ id: `notice-autotest-${id}`, severity, icon: 'ti-info-circle', title: `${severity} notice autotest`, description: 'Notice explanation autotest', action: { type: 'link', url: '/admin/v9/', label: 'Open autotest' } });
let state, notices, currentSessions, failSave;

Before(async ({ I, login }) => {
    await I.mockRoute(locationRoute, route => route.fulfill({ contentType: 'application/json', body: '{"success":false}' }));
    login('admin');
});

After(async ({ I }) => {
    for (const route of [dashboardPageRoute, preferencesRoute, codeRoute, confirmRoute, reportRoute, historyRoute, administratorsRoute, locationRoute]) {
        await I.stopMockingRoute(route);
    }
    I.wjSetDefaultWindowSize();
});

/** Supplies deterministic notices and account preferences without writing to the test account. */
async function openNotices(I) {
    state = { dismissedUntil: {} };
    notices = [notice('warning', 'warning'), notice('info', 'info'), notice('error', 'error')];
    currentSessions = { userSessions: [] };
    failSave = false;
    await I.mockRoute(preferencesRoute, route => {
        if (!failSave) state = JSON.parse(route.request().postDataJSON().value);
        return route.fulfill({ status: failSave ? 503 : 200, contentType: 'application/json', body: failSave ? 'false' : 'true' });
    });
    await mockDashboardBootstrap(I, () => ({ notices, currentSessions }), () => state);
    await I.amOnPage('/admin/v9/');
    await ready(I);
}

function ready(I) { return I.waitForElement('.md-dashboard__notice-list[aria-busy="false"]', 20); }

Scenario('Notices are ordered by severity and errors cannot be dismissed', async ({ I }) => {
    await openNotices(I);
    await I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__notice')].map(row => row.dataset.severity)), ['error', 'warning', 'info']);
    await I.dontSeeElement('[data-severity="error"] .md-dashboard__notice-dismiss');
    for (const severity of ['error', 'warning', 'info']) {
        await I.see('Notice explanation autotest', `[data-severity="${severity}"]`);
        await I.seeElement(`[data-severity="${severity}"] .md-dashboard__notice-action`);
    }
});

Scenario('Notice preferences support dismissal, undo and save failures', async ({ I }) => {
    await openNotices(I);
    await I.clickCss('[data-severity="warning"] .btn-link:not(.md-dashboard__notice-dismiss)');
    await I.waitForVisible('.md-dashboard__notice-toast', 10);
    await I.dontSeeElement('[data-severity="warning"]');
    await I.clickCss('.md-dashboard__notice-toast button:first-of-type');
    await I.waitForVisible('[data-severity="warning"]', 10);
    failSave = true;
    await I.clickCss('[data-severity="warning"] .md-dashboard__notice-dismiss');
    await I.waitForVisible('.md-dashboard__notice-status:not(:empty)', 10);
    await I.seeElement('[data-severity="warning"]');
    failSave = false;
    await I.clickCss('[data-severity="info"] .md-dashboard__notice-dismiss');
    await I.waitForVisible('.md-dashboard__notice-toast', 10);
    await I.refreshPage();
    await ready(I);
    await I.dontSeeElement('[data-severity="info"]');
    await I.seeElement('[data-severity="warning"]');
});

Scenario('Notice rows fit desktop, tablet and mobile widths', async ({ I }) => {
    await openNotices(I);
    for (const width of [1440, 1100, 390]) {
        await I.resizeWindow(width, 1100);
        await I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__notice-row')].every(row => row.scrollWidth <= row.clientWidth + 1)), `Notice rows must fit a ${width}px viewport.`);
    }
    await I.wjSetDefaultWindowSize();
});

Scenario('New-device notices require an explicit server confirmation and cannot be postponed', async ({ I }) => {
    await openNotices(I);
    const securityEvent = { id: 42, createDate: Date.now(), expiresAt: Date.now() + 7 * 86400000, browserName: 'Firefox autotest', operatingSystem: 'Linux', ipAddress: '127.0.0.1' };
    notices.push({ ...notice('security', 'warning'), id: 'newDevice:42', kind: 'newDevice', securityEvent });
    state.dismissedUntil['newDevice:42'] = Date.now() + 30 * 86400000;
    let failConfirm = true;
    const proofs = [];
    await I.mockRoute(codeRoute, route => route.fulfill({ status: 204, body: '' }));
    await I.mockRoute(confirmRoute, route => {
        proofs.push(route.request().postDataJSON());
        if (!failConfirm) securityEvent.confirmedAt = Date.now();
        return route.fulfill({ status: failConfirm ? 503 : 200, contentType: 'application/json', body: JSON.stringify(failConfirm ? {} : securityEvent) });
    });
    await I.refreshPage();
    await ready(I);
    const row = '[data-notice-id="newDevice:42"]';
    await I.dontSeeElement(`${row} .md-dashboard__notice-dismiss`);
    await I.clickCss(`${row} .md-dashboard__notice-confirm`);
    await I.waitForText('Zadajte 6-miestny kód', 10, row);
    await I.resizeWindow(390, 1100);
    await I.assertTrue(await I.executeScript(() => {
        const notice = document.querySelector('[data-notice-id="newDevice:42"]');
        const bounds = notice.getBoundingClientRect();
        return notice.scrollWidth <= notice.clientWidth + 1
            && [...notice.querySelectorAll('input, button')].every(control => {
                const rect = control.getBoundingClientRect();
                return rect.left >= bounds.left && rect.right <= bounds.right && rect.bottom <= bounds.bottom;
            });
    }), 'Code entry and its actions must remain accessible on mobile.');
    await I.fillField(`${row} input[name="deviceConfirmationCode"]`, '012345');
    await I.clickCss(`${row} .md-dashboard-device-confirmation [type="submit"]`);
    await I.waitForText('Kód sa nepodarilo overiť', 10, row);
    await I.seeElement(row);
    failConfirm = false;
    await I.clickCss(`${row} .md-dashboard-device-confirmation [type="submit"]`);
    await I.waitForInvisible(row, 10);
    await I.assertDeepEqual(proofs, [{ code: '012345' }, { code: '012345' }]);
});

Scenario('Keyboard device reporting opens the compact dialog and removes only a successfully blocked warning', async ({ I }) => {
    await openNotices(I);
    const now = Date.now();
    const securityEvent = { id: 43, createDate: now, expiresAt: now + 7 * 86400000, browserName: 'Firefox autotest', browserVersion: '155.0', operatingSystem: 'Linux' };
    notices = [
        { ...notice('security', 'warning'), id: 'newDevice:43', kind: 'newDevice', securityEvent },
        { ...notice('blocked', 'warning'), id: 'newDevice:44', kind: 'newDevice', securityEvent: { ...securityEvent, id: 44, reportedAt: now } },
        notice('unrelated', 'warning')
    ];
    let reports = 0, failReport = true;
    const reads = [];
    for (const routePattern of [historyRoute, administratorsRoute]) await I.mockRoute(routePattern, route => {
        reads.push(route.request().url());
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify(routePattern === historyRoute
            ? { content: [], totalElements: 0, totalPages: 0, first: true, last: true } : []) });
    });
    await I.mockRoute(reportRoute, route => {
        reports++;
        if (failReport) return route.fulfill({ status: 503, contentType: 'application/json', body: '{}' });
        securityEvent.reportedAt = Date.now();
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify(securityEvent) });
    });
    await I.refreshPage();
    await ready(I);
    await I.dontSeeElement('[data-notice-id="newDevice:44"]');
    await I.see('Firefox autotest 155 · Linux', '[data-notice-id="newDevice:43"]');
    const trigger = '[data-notice-id="newDevice:43"] .md-dashboard__notice-report';
    const dialog = '.md-dashboard-modal--device-security';
    await I.executeScript(selector => document.querySelector(selector).focus(), trigger);
    await I.pressKey('Enter');
    await I.waitForVisible(dialog, 10);
    await I.waitForText('Zmenu sa nepodarilo uložiť', 10, dialog);
    await I.see('Firefox autotest 155 · Linux', `${dialog} .md-dashboard-device-security__device`);
    await I.waitForFunction(selector => getComputedStyle(document.querySelector(selector)).opacity === '1'
        && getComputedStyle(document.querySelector(`${selector} .modal-dialog`)).transform === 'none', [dialog], 10);
    await I.assertTrue(await I.executeScript(selector => document.querySelector(selector).contains(document.activeElement), dialog), 'Focus must remain inside the security dialog after reporting fails.');
    await I.assertEqual(reports, 1, 'The explicit in-app report must immediately attempt blocking.');
    await I.assertDeepEqual(reads, [], 'The compact dialog must not load administrator or history data.');
    await I.dontSeeElement('.md-dashboard-modal--sessions');
    await I.dontSeeElement(`${dialog} [role="tab"]`);
    await I.seeElement('[data-notice-id="newDevice:43"]');
    await I.pressKey('Escape');
    await I.waitForDetached(dialog, 10);
    await I.seeElement(`${trigger}:focus`);
    await I.pressKey('Enter');
    await I.waitForText('Zmenu sa nepodarilo uložiť', 10, dialog);
    await I.executeScript(selector => document.querySelector(selector).focus(), `${dialog} .md-dashboard-device-security__report`);
    failReport = false;
    await I.pressKey('Space');
    await I.waitForElement(`${dialog} .md-dashboard-device-security__result`, 10);
    await I.see('Zabezpečte svoj účet', dialog);
    await I.see('Firefox autotest 155 · Linux sme zablokovali.', dialog);
    await I.see('Zariadenie je zablokované a jeho relácie sa odhlasujú.', dialog);
    await I.see('Ak ste sa neprihlásili vy, niekto môže poznať vaše heslo.', dialog);
    await I.dontSeeElement('[data-notice-id="newDevice:43"]');
    await I.seeElement('[data-notice-id="notice-autotest-unrelated"]');
    await I.assertEqual(reports, 3, 'Only explicit block attempts may submit a report.');
    await I.click('Neskôr', dialog);
    await I.waitForDetached(dialog, 10);
    await I.assertEqual(reports, 3, 'Later must only close the dialog.');
});
