const { waitForWidgets, mockDashboardBootstrap, dashboardPageRoute } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.data');

Before(({ login }) => login('admin'));

/**
 * Checks that the initial layout, notices and active sessions are supplied with the dashboard page.
 * They must still appear without additional requests to the removed dashboard data services.
 */
Scenario('Initial settings, notices and sessions render from HTML without REST requests', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    const requests = [];
    const routes = ['**/admin/rest/dashboard/settings', '**/admin/rest/dashboard/notices', '**/admin/rest/dashboard/data/sessions*', '**/admin/rest/dashboard/data/logged-admins*'];
    for (const pattern of routes) await I.mockRoute(pattern, route => {
        if (route.request().method() !== 'GET') return route.continue();
        requests.push(route.request().url());
        return route.fulfill({ status: 503, contentType: 'application/json', body: '{}' });
    });
    try {
        I.refreshPage();
        I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
        I.waitForElement('.md-dashboard__sessions .md-dashboard-widget__session-current', 20);
        await I.waitForFunction(() => document.querySelector('.md-dashboard__notice-list')?.getAttribute('aria-busy') === 'false', 20);
        I.assertEqual(requests.length, 0, 'The initial dashboard must render even when the removed read endpoints are unavailable.');
        I.assertTrue(await I.executeScript(() => {
            const dashboard = document.querySelector('webjet-overview-dashboard');
            return Array.isArray(dashboard.data.notices) && dashboard.dashboardController.settings.items.length > 0
                && !Object.hasOwn(dashboard.data, 'loggedAdmins');
        }), 'The dashboard must initialize its embedded data and widget preferences.');
    } finally {
        for (const pattern of routes) await I.stopMockingRoute(pattern);
    }
});

const recentPagesRoute = '**/admin/rest/web-pages/all*';
const card = '[data-instance-id="autotest-data-loading"]';

/** Exercises empty and failed loads, then retries through the widget's real API request. */
Scenario('A widget handles empty results and recovers from a failed load through retry', async ({ I }) => {
    const settings = {
        version: 1, configured: true, legacyBookmarksHandled: true, domainOptions: {},
        items: [{ id: 'autotest-data-loading', type: 'recent-pages', size: '3x3', options: {} }]
    };
    await mockDashboardBootstrap(I, () => ({ settings, notices: [] }));
    let responseMode = 'empty';
    let realResponseStatus;
    await I.mockRoute(recentPagesRoute, async route => {
        if (responseMode === 'real') {
            const response = await route.fetch();
            realResponseStatus = response.status();
            return route.fulfill({ response });
        }
        return route.fulfill({
            status: responseMode === 'error' ? 503 : 200, contentType: 'application/json',
            body: JSON.stringify(responseMode === 'error' ? { error: 'autotest unavailable' } : { content: [] })
        });
    });
    I.amOnPage('/admin/v9/');
    await waitForWidgets(I);
    I.see(await I.executeScript(() => WJ.translate('admin.dashboard.empty.js')), card);
    I.dontSeeElementInDOM(`${card} .text-danger`);

    responseMode = 'error';
    await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.refresh('autotest-data-loading'));
    I.see(await I.executeScript(() => WJ.translate('admin.dashboard.widgetError.js')), `${card} .text-danger`);
    I.seeElement(`${card} .md-dashboard__widget-content > button`);

    responseMode = 'real';
    I.clickCss(`${card} .md-dashboard__widget-content > button`);
    await waitForWidgets(I);
    I.assertEqual(realResponseStatus, 200, 'Retry must send the widget request to the application server.');
    I.dontSeeElementInDOM(`${card} .text-danger`);
    I.dontSeeElementInDOM(`${card} .md-dashboard__widget-content > button`);
    I.assertTrue(await I.executeScript(selector => {
        const content = document.querySelector(`${selector} .md-dashboard__widget-content`);
        return Boolean(content.querySelector('.md-dashboard-widget__pages'))
            || content.textContent.includes(WJ.translate('admin.dashboard.empty.js'));
    }, card), 'The widget must render the response, including a valid empty result.');
});

/** Removes the simulated dashboard layout and widget responses before subsequent tests. */
Scenario('Restore unmocked dashboard data', async ({ I }) => {
    await I.stopMockingRoute(recentPagesRoute);
    await I.stopMockingRoute(dashboardPageRoute);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
