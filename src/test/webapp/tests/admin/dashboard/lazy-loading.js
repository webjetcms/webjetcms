const { showWidget, mockDashboardBootstrap, dashboardPageRoute } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.lazy-loading');

const dataRoutes = ['**/admin/rest/forms-list/all', '**/admin/rest/audit/log/all?*'];
const monitoringRoute = '**/admin/rest/monitoring/actual';

Before(({ login }) => login('admin'));

for (const width of [1337, 390]) {
    /**
     * Checks on desktop and mobile that widgets below the visible screen load only after the user scrolls to
     * them. Returning to a loaded card reuses its content, manual refresh loads it again, and older
     * minimized widgets display their full saved size.
     */
    Scenario(`Offscreen data loads only on viewport entry at ${width}px`, async ({ I }) => {
        const requests = [];
        const settings = {
            version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true,
            domainOptions: {}, acknowledgedNewsVersion: null,
            items: [
                ...Array.from({ length: 6 }, (_, index) => ({ id: `lazy-autotest-${index}`, type: 'forms', size: '3x3', options: { days: 7 } })),
                { id: 'lazy-autotest-audit', type: 'audit', size: '3x3', options: {} },
                { id: 'lazy-autotest-memory', type: 'server-memory', size: '3x3', options: {} },
                { id: 'lazy-autotest-cpu', type: 'server-cpu', size: '3x3', collapsed: true, options: {} }
            ]
        };
        await mockDashboardBootstrap(I, () => ({ settings, notices: [], currentSessions: { userSessions: [] } }));
        for (const dataRoute of dataRoutes) await I.mockRoute(dataRoute, route => {
            const type = route.request().url().includes('/audit/') ? 'audit' : 'forms';
            requests.push(type);
            const body = type === 'audit' ? { content: [], options: { logType: [] } } : { content: [] };
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
        });
        await I.mockRoute(monitoringRoute, route => {
            requests.push('monitoring');
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
                serverActualTime: Date.now(), memUsed: 128 * 1048576, memFree: 384 * 1048576,
                memTotal: 512 * 1048576, cpuUsageProcess: 2, cpuUsage: 10
            }) });
        });
        try {
            I.resizeWindow(width, 900);
            I.amOnPage('/admin/v9/');
            I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
            await showWidget(I, 'lazy-autotest-0');
            I.assertTrue(await I.executeScript(() => document.querySelector('[data-instance-id="lazy-autotest-audit"]').getBoundingClientRect().top > window.innerHeight),
                'The target must start below the real viewport.');
            I.assertEqual(requests.filter(type => type === 'audit').length, 0, 'Offscreen widgets must not send an initial REST request.');
            I.assertEqual(requests.filter(type => type === 'monitoring').length, 0, 'Offscreen monitoring must not fetch its initial snapshot.');
            await showWidget(I, 'lazy-autotest-audit');
            I.assertEqual(requests.filter(type => type === 'audit').length, 1, 'Entering the viewport must load the widget once.');
            await showWidget(I, 'lazy-autotest-0');
            await showWidget(I, 'lazy-autotest-audit');
            I.assertEqual(requests.filter(type => type === 'audit').length, 1, 'Returning to a loaded card must reuse its content.');
            await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.refresh('lazy-autotest-audit'));
            I.assertEqual(requests.filter(type => type === 'audit').length, 2, 'Manual refresh must remain available.');
            await showWidget(I, 'lazy-autotest-cpu');
            I.seeElementInDOM('[data-instance-id="lazy-autotest-cpu"] .md-dashboard-widget__monitoring-values');
            I.seeElementInDOM('[data-instance-id="lazy-autotest-cpu"][data-size="3x3"] .md-dashboard-widget__chart');
            I.dontSeeElementInDOM('.md-dashboard__widget.is-collapsed, [data-dashboard-action="collapse"]');
            I.assertFalse(await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.some(item => 'collapsed' in item)),
                'Legacy minimized widgets must use their saved size and discard the removed state.');
            I.assertAbove(requests.filter(type => type === 'monitoring').length, 0, 'Visible monitoring must load its full chart and values.');
            I.executeScript(() => {
                const scrollbar = window.scrollbarMain;
                scrollbar.setMomentum(0, 0);
                scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('.md-dashboard__toolbar').getBoundingClientRect().top - 64);
            });
            I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
            await showWidget(I, 'lazy-autotest-cpu');
            I.clickCss('[data-instance-id="lazy-autotest-cpu"] .dropdown > button');
            I.see('Nastavenia widgetu', '[data-instance-id="lazy-autotest-cpu"] .dropdown-menu');
            I.dontSee('Minimalizovať', '[data-instance-id="lazy-autotest-cpu"] .dropdown-menu');
            I.saveScreenshot(`dashboard-no-minimize-${width}.png`, false);
        } finally {
            for (const route of [dashboardPageRoute, ...dataRoutes, monitoringRoute]) await I.stopMockingRoute(route);
            I.wjSetDefaultWindowSize();
        }
    });
}
