const { dashboardPageRoute, mockDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.notices').tag('@singlethread');

const preferencesRoute = '**/admin/rest/admin-settings/';
const codeRoute = '**/admin/rest/security/login-events/*/code';
const confirmRoute = '**/admin/rest/security/login-events/*/confirm';
const reportRoute = '**/admin/rest/security/login-events/*/report';
const historyRoute = '**/admin/rest/sessions/login-history*';
const administratorsRoute = '**/admin/rest/sessions/administrators';
const notice = (id, severity) => ({ id: `notice-autotest-${id}`, severity, icon: 'ti-info-circle', title: `${severity} notice autotest`, description: 'Notice explanation autotest', action: { type: 'link', url: '/admin/v9/', label: 'Open autotest' } });
let state, notices, currentSessions, failSave;

Before(({ login }) => { login('admin'); });

After(async ({ I }) => {
    for (const route of [dashboardPageRoute, preferencesRoute, codeRoute, confirmRoute, reportRoute, historyRoute, administratorsRoute]) {
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

Scenario('Keyboard review is read-only and only successful blocking removes the selected warning', async ({ I }) => {
    await openNotices(I);
    const now = Date.now();
    const securityEvent = { id: 43, createDate: now, expiresAt: now + 7 * 86400000, browserName: 'Firefox autotest', operatingSystem: 'Linux' };
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
    const trigger = '[data-notice-id="newDevice:43"] .md-dashboard__notice-report';
    const dialog = '.md-dashboard-modal--sessions';
    await I.executeScript(selector => document.querySelector(selector).focus(), trigger);
    await I.pressKey('Enter');
    await I.waitForVisible(dialog, 10);
    await I.waitForElement(`${dialog}:focus`, 10);
    await I.assertEqual(reports, 0, 'Opening the review must not block a device.');
    await I.assertDeepEqual(reads, [], 'Inactive tabs must not load administrator or history data.');
    await I.pressKey('Tab');
    await I.seeElement(`${dialog} .btn-close:focus-visible`);
    await I.pressKey('Tab');
    await I.seeElement(`${dialog} [role="tab"][id$="-mine"]:focus-visible`);
    await I.pressKey('End');
    await I.waitForElement(`${dialog} [role="tab"][id$="-history"][aria-selected="true"]`, 10);
    await I.dontSeeElement(`${dialog} .md-dashboard-sessions__security`);
    await I.pressKey('Home');
    await I.seeElement(`${dialog} .md-dashboard-sessions__security`);
    await I.assertEqual(reports, 0, 'Reviewing tabs must not block a device.');
    await I.pressKey('Escape');
    await I.waitForDetached(dialog, 10);
    await I.seeElement(`${trigger}:focus`);
    await I.pressKey('Enter');
    await I.waitForElement(`${dialog}:focus`, 10);
    for (let step = 0; step < 4; step++) await I.pressKey('Tab');
    await I.seeElement(`${dialog} .md-dashboard-sessions__report:focus-visible`);
    await I.pressKey('Space');
    await I.waitForText('Zmenu sa nepodarilo uložiť', 10, dialog);
    await I.seeElement('[data-notice-id="newDevice:43"]');
    failReport = false;
    await I.clickCss(`${dialog} .md-dashboard-sessions__report`);
    await I.waitForElement(`${dialog} .md-dashboard-sessions__security [role="status"]`, 10);
    await I.dontSeeElement('[data-notice-id="newDevice:43"]');
    await I.seeElement('[data-notice-id="notice-autotest-unrelated"]');
    await I.assertEqual(reports, 2, 'Only explicit block attempts may submit a report.');
    await I.pressKey('Escape');
    await I.waitForDetached(dialog, 10);
});
