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
