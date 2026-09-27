const { showWidget } = require('../../helpers/dashboard-browser');

Feature('admin.dashboard-lazy-loading');

const settingsRoute = '**/admin/rest/dashboard/settings';
const noticesRoute = '**/admin/rest/dashboard/notices';
const dataRoute = '**/admin/rest/dashboard/data/**';
const monitoringRoute = '**/admin/rest/monitoring/actual';

Before(({ login }) => login('admin'));

for (const width of [1337, 390]) {
    Scenario(`Offscreen data loads only on viewport entry at ${width}px`, async ({ I }) => {
        const requests = [];
        const settings = {
            version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true,
            domainOptions: {}, acknowledgedNewsVersion: null,
            items: [
                ...Array.from({ length: 6 }, (_, index) => ({ id: `lazy-autotest-${index}`, type: 'forms', size: '3x3', collapsed: false, options: { days: 7 } })),
                { id: 'lazy-autotest-audit', type: 'audit', size: '3x3', collapsed: false, options: {} },
                { id: 'lazy-autotest-memory', type: 'server-memory', size: '3x3', collapsed: false, options: {} },
                { id: 'lazy-autotest-cpu', type: 'server-cpu', size: '3x3', collapsed: true, options: {} }
            ]
        };
        await I.mockRoute(settingsRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(settings) }));
        await I.mockRoute(noticesRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
        await I.mockRoute(dataRoute, route => {
            const type = new URL(route.request().url()).pathname.split('/').pop();
            requests.push(type);
            const body = type === 'sessions' ? { currentSessions: { userSessions: [] } }
                : { total: 0, items: [], options: [], from: Date.now() - 86400000, to: Date.now() };
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
            I.seeElementInDOM('[data-instance-id="lazy-autotest-cpu"].is-collapsed .md-dashboard-widget__monitoring-values');
            I.assertAbove(requests.filter(type => type === 'monitoring').length, 0, 'Visible collapsed monitoring must load its numeric preview.');
        } finally {
            for (const route of [settingsRoute, noticesRoute, dataRoute, monitoringRoute]) await I.stopMockingRoute(route);
            I.wjSetDefaultWindowSize();
        }
    });
}
