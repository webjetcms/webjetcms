const { dashboardPageRoute, mockDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.notices').tag('@singlethread');

const preferencesRoute = '**/admin/rest/admin-settings/';
const notice = (id, severity) => ({ id: `notice-autotest-${id}`, severity, icon: 'ti-info-circle', title: `${severity} notice autotest`, description: 'Notice explanation autotest', action: { type: 'link', url: '/admin/v9/', label: 'Open autotest' } });
let state, notices, currentSessions, failSave;

Before(({ login }) => { login('admin'); });

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

async function assertWhiteControl(I, selector, state) {
    I.assertEqual(await I.grabCssPropertyFrom(selector, 'background-color'), 'rgb(255, 255, 255)', `The ${state} control must have a white background.`);
    I.assertEqual(await I.grabCssPropertyFrom(selector, 'opacity'), '1', `The ${state} control must not blend with its colored container.`);
}

Scenario('Notices are ordered by severity and errors cannot be dismissed', async ({ I }) => {
    await openNotices(I);
    await I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__notice')].map(row => row.dataset.severity)), ['error', 'warning', 'info']);
    await I.dontSeeElement('[data-severity="error"] .md-dashboard__notice-dismiss');
    for (const severity of ['error', 'warning', 'info']) {
        await I.see('Notice explanation autotest', `[data-severity="${severity}"]`);
        await I.seeElement(`[data-severity="${severity}"] .md-dashboard__notice-action`);
    }
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(preferencesRoute);
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
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(preferencesRoute);
});

Scenario('Notice rows fit desktop, tablet and mobile widths', async ({ I }) => {
    await openNotices(I);
    for (const width of [1440, 1100, 390]) {
        await I.resizeWindow(width, 1100);
        await I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__notice-row')].every(row => row.scrollWidth <= row.clientWidth + 1)), `Notice rows must fit a ${width}px viewport.`);
    }
    await I.wjSetDefaultWindowSize();
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(preferencesRoute);
});

Scenario('New-device notices require an explicit server confirmation and cannot be postponed', async ({ I }) => {
    await openNotices(I);
    const securityEvent = { id: 42, createDate: Date.now(), expiresAt: Date.now() + 7 * 86400000, browserName: 'Firefox autotest', operatingSystem: 'Linux', ipAddress: '127.0.0.1' };
    notices.push({ ...notice('security', 'warning'), id: 'newDevice:42', kind: 'newDevice', securityEvent });
    state.dismissedUntil['newDevice:42'] = Date.now() + 30 * 86400000;
    let failConfirm = true;
    let releaseCode;
    const codeReady = new Promise(resolve => { releaseCode = resolve; });
    const codeRoute = '**/admin/rest/security/login-events/*/code';
    await I.mockRoute(codeRoute, async route => {
        await codeReady;
        return route.fulfill({ status: 204, body: '' });
    });
    const routePattern = '**/admin/rest/security/login-events/*/confirm';
    await I.mockRoute(routePattern, route => {
        if (!failConfirm) securityEvent.confirmedAt = Date.now();
        return route.fulfill({ status: failConfirm ? 503 : 200, contentType: 'application/json', body: JSON.stringify(failConfirm ? {} : securityEvent) });
    });
    await I.refreshPage();
    await ready(I);
    const row = '[data-notice-id="newDevice:42"]';
    await I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__notice').dataset.noticeId), 'newDevice:42');
    await I.dontSeeElement(`${row} .md-dashboard__notice-dismiss`);
    await I.assertEqual(await I.grabNumberOfVisibleElements(`${row} button`), 2);
    for (const width of [1440, 1100, 390]) {
        await I.resizeWindow(width, 1100);
        await I.assertTrue(await I.executeScript(() => {
            const element = document.querySelector('[data-notice-id="newDevice:42"] .md-dashboard__notice-row');
            return element.scrollWidth <= element.clientWidth + 1;
        }), `Security actions must fit a ${width}px viewport.`);
    }
    await I.wjSetDefaultWindowSize();
    const confirm = `${row} .md-dashboard__notice-confirm`;
    const input = `${row} input[name="deviceConfirmationCode"]`;
    const resend = `${row} .md-dashboard-device-confirmation button[type="button"]`;
    await assertWhiteControl(I, confirm, 'default');
    await I.moveCursorTo(confirm);
    await assertWhiteControl(I, confirm, 'hovered');
    await I.executeScript(selector => document.querySelector(selector).focus(), confirm);
    await I.pressKey('Shift+Tab');
    await I.pressKey('Tab');
    await assertWhiteControl(I, confirm, 'keyboard-focused');
    await I.pressKeyDown('Space');
    await assertWhiteControl(I, confirm, 'pressed');
    await I.pressKeyUp('Space');
    await I.waitForElement(`${row} form[aria-busy="true"]`, 10);
    for (const selector of [confirm, input, resend]) await assertWhiteControl(I, selector, 'disabled while sending');
    releaseCode();
    await I.waitForText('Zadajte 6-miestny kód', 10, row);
    for (const selector of [confirm, input, resend]) await assertWhiteControl(I, selector, 'expanded');
    await I.moveCursorTo(resend);
    await assertWhiteControl(I, resend, 'hovered resend');
    for (const width of [1440, 1100, 390]) {
        await I.resizeWindow(width, 1100);
        await I.assertTrue(await I.executeScript(() => {
            const notice = document.querySelector('[data-notice-id="newDevice:42"]');
            const form = notice.querySelector('.md-dashboard-device-confirmation');
            const bounds = notice.getBoundingClientRect();
            const controls = [form, ...form.querySelectorAll('input, button')];
            return getComputedStyle(notice).backgroundColor !== 'rgba(0, 0, 0, 0)'
                && notice.scrollWidth <= notice.clientWidth + 1
                && controls.every(control => {
                    const rect = control.getBoundingClientRect();
                    return rect.left >= bounds.left && rect.right <= bounds.right && rect.bottom <= bounds.bottom;
                });
        }), `Code entry must remain inside the colored notice at ${width}px.`);
    }
    await I.saveScreenshot('dashboard-notice-code-mobile.png');
    await I.wjSetDefaultWindowSize();
    await I.saveScreenshot('dashboard-notice-code.png');
    await I.fillField(`${row} input[name="deviceConfirmationCode"]`, '012345');
    await I.clickCss(`${row} .md-dashboard-device-confirmation [type="submit"]`);
    await I.waitForText('Kód sa nepodarilo overiť', 10, row);
    await I.seeElement(row);
    failConfirm = false;
    await I.clickCss(`${row} .md-dashboard-device-confirmation [type="submit"]`);
    await I.waitForInvisible(row, 10);
    await I.refreshPage();
    await ready(I);
    await I.dontSeeElement(row);
    await I.stopMockingRoute(routePattern);
    await I.stopMockingRoute(codeRoute);
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(preferencesRoute);
});

Scenario('Keyboard review and reporting an unfamiliar login preserve its warning and restore focus', async ({ I }) => {
    await openNotices(I);
    const securityEvent = { id: 43, createDate: Date.now(), expiresAt: Date.now() + 7 * 86400000, browserName: 'Firefox autotest', browserVersion: '123', operatingSystem: 'Linux', ipAddress: '127.0.0.1' };
    notices = [{ ...notice('security', 'warning'), id: 'newDevice:43', kind: 'newDevice', securityEvent }];
    currentSessions = { currentSessionId: 'autotest-current', userSessions: [{ userSessions: [
        { sessionId: 'autotest-current', browserName: 'Chrome autotest', logonTime: Date.now(), remoteAddr: '127.0.0.1' },
        { sessionId: 'autotest-other', browserName: 'Firefox autotest', logonTime: Date.now(), remoteAddr: '192.0.2.2' }
    ] }] };
    let reports = 0;
    let historyReads = 0;
    let administratorReads = 0;
    const historyRoute = '**/admin/rest/sessions/login-history*';
    const administratorsRoute = '**/admin/rest/sessions/administrators';
    await I.mockRoute(historyRoute, route => {
        historyReads++;
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify({
            content: [{ createDate: Date.now(), ip: '192.0.2.2', description: 'Login history autotest' }],
            totalElements: 1, totalPages: 1, first: true, last: true
        }) });
    });
    await I.mockRoute(administratorsRoute, route => {
        administratorReads++;
        return route.fulfill({ contentType: 'application/json', body: '[]' });
    });
    const routePattern = '**/admin/rest/security/login-events/*/report';
    await I.mockRoute(routePattern, route => {
        reports++;
        securityEvent.reportedAt = Date.now();
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(securityEvent) });
    });
    await I.refreshPage();
    await ready(I);
    await I.executeScript(() => document.querySelector('.md-dashboard__notice-report').focus());
    await I.pressKey('Enter');
    const dialog = '.md-dashboard-modal--sessions';
    await I.waitForVisible(dialog, 10);
    await I.waitForFunction(() => document.activeElement.matches('.md-dashboard-modal--sessions [role="tab"]'), 10);
    await I.assertEqual(reports, 0, 'Opening the review must not report the login.');
    await I.see('Aktívne prihlásenia', `${dialog} .modal-header`);
    await I.assertEqual(await I.grabNumberOfVisibleElements(`${dialog} [role="tab"]`), 3);
    await I.assertEqual(historyReads, 0, 'History must wait for its tab.');
    await I.assertEqual(administratorReads, 0, 'Administrators must wait for their tab.');
    await I.seeElement(`${dialog} .md-dashboard-sessions__summary button`);
    await I.seeElement(`${dialog} .md-dashboard-sessions__password`);
    await I.see('Firefox autotest 123', `${dialog} .md-dashboard-sessions__security`);
    for (const width of [1440, 1100, 390]) {
        await I.resizeWindow(width, 1100);
        await I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard-modal--sessions .modal-content, .md-dashboard-sessions__tabs, .md-dashboard-sessions__security')]
            .every(element => element.scrollWidth <= element.clientWidth + 1)), `The integrated dialog must fit a ${width}px viewport.`);
    }
    await I.wjSetDefaultWindowSize();
    await I.saveScreenshot('dashboard-active-sessions-security.png');
    await I.pressKey('End');
    await I.waitForText('Login history autotest', 10, `${dialog} .md-dashboard-sessions__history`);
    await I.assertEqual(historyReads, 1);
    await I.see('192.0.2.2', `${dialog} .md-dashboard-sessions__history`);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__security`);
    await I.seeElement(`${dialog} .md-dashboard-sessions__password`);
    await I.pressKey('ArrowLeft');
    await I.waitForElement(`${dialog} .md-dashboard-sessions__admins[aria-busy="false"]`, 10);
    await I.assertEqual(administratorReads, 1);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__security`);
    await I.assertEqual(reports, 0, 'Reviewing other tabs must not report the login.');
    await I.pressKey('Home');
    await I.seeElement(`${dialog} .md-dashboard-sessions__security`);
    await I.pressKey('Escape');
    await I.waitForDetached(dialog, 10);
    await I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard__notice-report')), 'Closing the dialog must restore focus to the notice action.');
    await I.pressKey('Enter');
    await I.waitForVisible(dialog, 10);
    await I.waitForFunction(() => document.activeElement.matches('.md-dashboard-modal--sessions [role="tab"]'), 10);
    await I.pressKey('Tab');
    await I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard-sessions__mine')), 'Tab must reach the personal tab panel.');
    await I.pressKey('Tab');
    await I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard-sessions__report')), 'Tab must reach the explicit report action.');
    await I.pressKey('Space');
    await I.waitForVisible(`${dialog} .md-dashboard-sessions__security [role="status"]`, 10);
    await I.assertEqual(reports, 1);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__report`);
    await I.clickCss(`${dialog} .modal-footer > button:last-child`);
    await I.waitForDetached(dialog, 10);
    await I.seeElement('[data-notice-id="newDevice:43"]');
    await I.stopMockingRoute(routePattern);
    await I.stopMockingRoute(historyRoute);
    await I.stopMockingRoute(administratorsRoute);
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(preferencesRoute);
});

Scenario('Email confirmation shows a success toast without opening the session dialog', async ({ I }) => {
    const securityEvent = { id: 45, createDate: Date.now(), browserName: 'Email browser autotest', operatingSystem: 'Linux' };
    const confirmRoute = '**/admin/rest/security/login-events/45/confirm';
    const emailPageRoute = '**/admin/v9/?securityEvent=45&deviceConfirmation=autotest-email-secret';
    const proofs = [];
    await I.mockRoute(confirmRoute, route => {
        proofs.push(route.request().postDataJSON());
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ...securityEvent, confirmedAt: Date.now() }) });
    });
    await mockDashboardBootstrap(I, () => ({ notices: [{ id: 'newDevice:45', kind: 'newDevice', securityEvent }],
        currentSessions: { userSessions: [] }, securityEventRequested: true, requestedSecurityEvent: securityEvent }), null, emailPageRoute);
    I.amOnPage('/admin/v9/?securityEvent=45&deviceConfirmation=autotest-email-secret');
    I.waitForText('Prihlásenie bolo potvrdené.', 10, '.toast-success');
    I.see('Moje aktívne prihlásenia', '.toast-success .toast-title');
    I.dontSeeElement('.md-dashboard-modal--sessions');
    I.dontSeeElement('[data-notice-id="newDevice:45"]');
    I.dontSeeInCurrentUrl('deviceConfirmation=');
    I.assertDeepEqual(proofs, [{ token: 'autotest-email-secret' }]);
    I.saveScreenshot('dashboard-email-confirmation-toast.png');
    I.waitForDetached('.toast-success', 15);
    await I.stopMockingRoute(confirmRoute);
    await I.stopMockingRoute(emailPageRoute);
});

Scenario('Email bootstrap opens login details and unavailable links without a mutation', async ({ I }) => {
    const securityEvent = { id: 44, createDate: Date.now(), browserName: 'Email browser autotest', operatingSystem: 'Linux', ipAddress: '127.0.0.1' };
    let requestedSecurityEvent = securityEvent;
    await mockDashboardBootstrap(I, () => ({ notices: [], currentSessions: { userSessions: [] }, securityEventRequested: true, requestedSecurityEvent }));
    await I.amOnPage('/admin/v9/');
    const dialog = '.md-dashboard-modal--sessions';
    await I.waitForVisible(dialog, 10);
    await I.see('Email browser autotest', `${dialog} .md-dashboard-sessions__security`);
    await I.seeElement(`${dialog} .md-dashboard-sessions__report`);
    await I.clickCss(`${dialog} .modal-footer > button:last-child`);
    await I.waitForDetached(dialog, 10);
    requestedSecurityEvent = null;
    await I.refreshPage();
    await I.waitForVisible(dialog, 10);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__report`);
    await I.dontSeeElement(`${dialog} table`);
    await I.assertEqual(await I.grabNumberOfVisibleElements(`${dialog} .modal-footer button`), 1);
    await I.clickCss(`${dialog} .modal-footer > button:last-child`);
    await I.waitForDetached(dialog, 10);
    await I.stopMockingRoute(dashboardPageRoute);
});
