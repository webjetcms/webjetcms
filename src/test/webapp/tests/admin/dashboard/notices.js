const { dashboardPageRoute, mockDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.notices').tag('@singlethread');

const preferencesRoute = '**/admin/rest/admin-settings/';
const notice = (id, severity) => ({ id: `notice-autotest-${id}`, severity, icon: 'ti-info-circle', title: `${severity} notice autotest`, description: 'Notice explanation autotest', action: { type: 'link', url: '/admin/v9/', label: 'Open autotest' } });
let state, notices, failSave;

Before(({ login }) => { login('admin'); });

/** Supplies deterministic notices and account preferences without writing to the test account. */
async function openNotices(I) {
    state = { dismissedUntil: {} };
    notices = [notice('warning', 'warning'), notice('info', 'info'), notice('error', 'error')];
    failSave = false;
    await I.mockRoute(preferencesRoute, route => {
        if (!failSave) state = JSON.parse(route.request().postDataJSON().value);
        return route.fulfill({ status: failSave ? 503 : 200, contentType: 'application/json', body: failSave ? 'false' : 'true' });
    });
    await mockDashboardBootstrap(I, () => ({ notices, currentSessions: { userSessions: [] } }), () => state);
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
    const securityEvent = { id: 'autotest-login', createdAt: Date.now(), expiresAt: Date.now() + 7 * 86400000, browserName: 'Firefox autotest', operatingSystem: 'Linux', ipAddress: '127.0.0.1' };
    notices.push({ ...notice('security', 'warning'), id: 'newDevice:autotest-login', kind: 'newDevice', securityEvent });
    state.dismissedUntil['newDevice:autotest-login'] = Date.now() + 30 * 86400000;
    let failConfirm = true;
    const routePattern = '**/admin/rest/security/login-events/*/confirm';
    await I.mockRoute(routePattern, route => {
        if (!failConfirm) securityEvent.confirmedAt = Date.now();
        return route.fulfill({ status: failConfirm ? 503 : 200, contentType: 'application/json', body: JSON.stringify(failConfirm ? {} : securityEvent) });
    });
    await I.refreshPage();
    await ready(I);
    const row = '[data-notice-id="newDevice:autotest-login"]';
    await I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__notice').dataset.noticeId), 'newDevice:autotest-login');
    await I.dontSeeElement(`${row} .md-dashboard__notice-dismiss`);
    await I.assertEqual(await I.grabNumberOfVisibleElements(`${row} button`), 2);
    for (const width of [1440, 1100, 390]) {
        await I.resizeWindow(width, 1100);
        await I.assertTrue(await I.executeScript(() => {
            const element = document.querySelector('[data-notice-id="newDevice:autotest-login"] .md-dashboard__notice-row');
            return element.scrollWidth <= element.clientWidth + 1;
        }), `Security actions must fit a ${width}px viewport.`);
    }
    await I.wjSetDefaultWindowSize();
    await I.clickCss(`${row} .md-dashboard__notice-confirm`);
    await I.waitForVisible('.md-dashboard__notice-status:not(:empty)', 10);
    await I.seeElement(row);
    failConfirm = false;
    await I.clickCss(`${row} .md-dashboard__notice-confirm`);
    await I.waitForInvisible(row, 10);
    await I.refreshPage();
    await ready(I);
    await I.dontSeeElement(row);
    await I.stopMockingRoute(routePattern);
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(preferencesRoute);
});

Scenario('Keyboard review and reporting an unfamiliar login preserve its warning and restore focus', async ({ I }) => {
    await openNotices(I);
    const securityEvent = { id: 'autotest-review', createdAt: Date.now(), expiresAt: Date.now() + 7 * 86400000, browserName: 'Firefox autotest', browserVersion: '123', operatingSystem: 'Linux', ipAddress: '127.0.0.1' };
    notices = [{ ...notice('security', 'warning'), id: 'newDevice:autotest-review', kind: 'newDevice', securityEvent }];
    let reports = 0;
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
    const dialog = '.md-dashboard-modal--security';
    await I.waitForVisible(dialog, 10);
    await I.waitForFunction(() => document.activeElement.matches('.md-dashboard-modal--security [role="tab"]'), 10);
    await I.assertEqual(reports, 0, 'Opening the review must not report the login.');
    await I.assertEqual(await I.grabNumberOfVisibleElements(`${dialog} [role="tab"]`), 1);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__summary button`);
    await I.see('Firefox autotest 123', `${dialog} .md-dashboard-sessions__security`);
    await I.pressKey('Escape');
    await I.waitForDetached(dialog, 10);
    await I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard__notice-report')), 'Closing the dialog must restore focus to the notice action.');
    await I.pressKey('Enter');
    await I.waitForVisible(dialog, 10);
    await I.waitForFunction(() => document.activeElement.matches('.md-dashboard-modal--security [role="tab"]'), 10);
    await I.pressKey('Tab');
    await I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard-sessions__report')), 'Tab must reach the explicit report action.');
    await I.pressKey('Space');
    await I.waitForVisible(`${dialog} .md-dashboard-sessions__security [role="status"]`, 10);
    await I.assertEqual(reports, 1);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__report`);
    await I.clickCss(`${dialog} .modal-footer button:last-child`);
    await I.waitForDetached(dialog, 10);
    await I.seeElement('[data-notice-id="newDevice:autotest-review"]');
    await I.stopMockingRoute(routePattern);
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(preferencesRoute);
});

Scenario('Email bootstrap opens login details and unavailable links without a mutation', async ({ I }) => {
    const securityEvent = { id: 'autotest-email', createdAt: Date.now(), browserName: 'Email browser autotest', operatingSystem: 'Linux', ipAddress: '127.0.0.1' };
    let requestedSecurityEvent = securityEvent;
    await mockDashboardBootstrap(I, () => ({ notices: [], currentSessions: { userSessions: [] }, securityEventRequested: true, requestedSecurityEvent }));
    await I.amOnPage('/admin/v9/');
    const dialog = '.md-dashboard-modal--security';
    await I.waitForVisible(dialog, 10);
    await I.see('Email browser autotest', `${dialog} .md-dashboard-sessions__security`);
    await I.seeElement(`${dialog} .md-dashboard-sessions__report`);
    await I.clickCss(`${dialog} .modal-footer button:last-child`);
    await I.waitForDetached(dialog, 10);
    requestedSecurityEvent = null;
    await I.refreshPage();
    await I.waitForVisible(dialog, 10);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__report`);
    await I.dontSeeElement(`${dialog} table`);
    await I.assertEqual(await I.grabNumberOfVisibleElements(`${dialog} .modal-footer button`), 1);
    await I.clickCss(`${dialog} .modal-footer button:last-child`);
    await I.waitForDetached(dialog, 10);
    await I.stopMockingRoute(dashboardPageRoute);
});
