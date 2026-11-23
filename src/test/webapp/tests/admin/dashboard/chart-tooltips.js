const { waitForWidgets, mockDashboardBootstrap, dashboardPageRoute } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.chart-tooltips').tag('@singlethread');

const routes = ['**/admin/rest/dashboard/settings', '**/admin/rest/stat/views/search/findByColumns?*',
    '**/admin/rest/monitoring/actual', '**/admin/rest/dashboard/data/server-memory*', '**/admin/rest/dashboard/data/server-cpu*'];
const from = new Date(2026, 8, 20).getTime();
const values = [40, 200, 104, 190, 110, 45, 5];
let actualRequests;
let historicalRequests;

Before(async ({ I, login }) => {
    login('admin');
    // Intercept preferences and chart data without changing the user's persisted dashboard.
    let settings = {
        version: 1, configured: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: 'tooltip-autotest-news', type: 'news', size: '3x2', options: {} },
            ...['traffic', 'recent-pages', 'search-terms', 'top-pages', 'server-memory', 'server-cpu'].map(type => ({
                id: `tooltip-autotest-${type}`, type, size: '3x3', options: { days: 7 }
            }))
        ]
    };
    await I.mockRoute(routes[0], route => {
        if (route.request().method() === 'PUT') settings = route.request().postDataJSON();
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(settings) });
    });
    await mockDashboardBootstrap(I, () => ({ settings, notices: [] }));
    await I.mockRoute(routes[1], route => {
        const start = Number(new URL(route.request().url()).searchParams.get('searchDayDate').slice(10).split('-')[0]);
        const content = [...[350, 300, 110, 210, 160, 220, 100], ...values].map((sessions, index) => {
            const dayDate = new Date(start); dayDate.setDate(dayDate.getDate() + index);
            return { dayDate: dayDate.getTime(), sessions };
        });
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content }) });
    });
    actualRequests = 0;
    historicalRequests = 0;
    await I.mockRoute(routes[2], route => {
        const value = ++actualRequests;
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
            serverActualTime: from + value * 5000, memUsed: (128 + value) * 1048576, memFree: (384 - value) * 1048576,
            memTotal: 512 * 1048576, cpuUsageProcess: value, cpuUsage: value + 10
        }) });
    });
    for (const route of routes.slice(3)) await I.mockRoute(route, request => {
        historicalRequests++;
        return request.fulfill({ status: 400, contentType: 'application/json', body: '{}' });
    });
    I.resizeWindow(1337, 1241);
    I.amOnPage('/admin/v9/');
    I.waitForFunction(() => {
        const element = document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart');
        const root = window.am5?.registry.rootElements.find(root => root.dom === element);
        const chart = root?.container.children.values.find(child => child.series && child.xAxes);
        return Boolean(chart?.series.getIndex(0)?.dataItems.length === 7 && chart.plotContainer.width() > 0);
    }, 20);
    I.executeScript(() => {
        window.autotestDashboardChartErrors = [];
        window.addEventListener('error', event => window.autotestDashboardChartErrors.push(event.message));
    });
});

/** Moves the real pointer to a plotted date without scrolling the target into view. */
async function hoverDate(I, index, type = 'traffic') {
    const host = `[data-widget-type="${type}"] .md-dashboard-widget__chart`;
    I.moveCursorTo('.ly-header');
    if (type !== 'traffic') I.waitForFunction(([index, type]) => {
        const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        return chart.series.values.every(series => Number.isFinite(series.dataItems[index]?.get('point')?.x));
    }, [index, type], 5);
    const offset = await I.executeScript(({ index, type }) => {
        const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        const x = type === 'traffic' ? chart.plotContainer.width() * index / 6
            : chart.series.getIndex(0).dataItems[index].get('point').x;
        const point = chart.plotContainer.toGlobal({ x, y: chart.plotContainer.height() / 2 });
        return { x: point.x - element.clientWidth / 2, y: point.y - element.clientHeight / 2 };
    }, { index, type });
    I.moveCursorTo(host, offset.x - 2, offset.y);
    I.moveCursorTo(host, offset.x, offset.y);
    I.assertDeepEqual(await I.executeScript(() => window.autotestDashboardChartErrors), [],
        'Moving over the chart must not invoke disposed chart elements.');
    I.waitForFunction(([index, type]) => {
        const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        return chart.series.values.every(series => series.get('tooltip')?.isVisible()
            && series.get('tooltip').dataItem === series.dataItems[index]);
    }, [index, type], 5);
}

/**
 * Checks that pointing at a traffic-chart date shows the corresponding values before and after scrolling the
 * dashboard. The tooltip must follow the pointer without chart errors.
 */
Scenario('Traffic tooltips follow the real pointer after transformed page scrolling', async ({ I }) => {
    await hoverDate(I, 2);
    const before = await I.executeScript(() => document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart').getBoundingClientRect().top);
    I.executeScript(() => {
        window.scrollbarMain.setMomentum(0, 0);
        window.scrollbarMain.update();
        window.scrollbarMain.setPosition(0, 160);
    });
    I.waitForFunction(([before]) => document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart').getBoundingClientRect().top <= before - 150, [before], 5);
    await hoverDate(I, 4);
    I.executeScript(() => window.scrollbarMain.setPosition(0, 0));
    I.waitForFunction(() => window.scrollbarMain.offset.y === 0, 5);
    await hoverDate(I, 1);
});

/**
 * Checks that hovering over the traffic chart exposes its date tooltip as well as the series values.
 */
Scenario('Traffic date tooltip displays the hovered date', async ({ I }) => {
    await hoverDate(I, 2);
    const tooltip = await I.executeScript(() => {
        const element = document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart');
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        const tooltip = chart.xAxes.getIndex(0).get('tooltip');
        return { visible: tooltip.isVisible(), text: tooltip.label.getText() };
    });
    I.assertTrue(tooltip.visible, 'The hovered date tooltip must be visible.');
    I.assertTrue(tooltip.text.trim().length > 0, 'The date tooltip must contain text.');
});

/**
 * Checks that memory and CPU readings update together in the chart, summary and accessible table. Both chart
 * sizes must show tooltip values and units.
 */
Scenario('Monitoring charts update values and tooltips in both supported sizes', async ({ I }) => {
    for (const size of ['3x2', '3x3']) {
        I.assertTrue(await I.executeScript(async size => {
            const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
            const next = JSON.parse(JSON.stringify(controller.settings));
            next.items.filter(item => item.type.startsWith('server-')).forEach(item => { item.size = size; });
            return controller._commit(next);
        }, size), 'Mocked settings must apply the chart size without changing persisted preferences.');
        await waitForWidgets(I);
        I.waitForFunction(() => ['server-memory', 'server-cpu'].every(type => {
            const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
            const root = window.am5?.registry.rootElements.find(root => root.dom === element);
            const chart = root?.container.children.values.find(child => child.series && child.xAxes);
            return Boolean(chart?.series.getIndex(0)?.dataItems.length >= 1 && chart.plotContainer.width() > 0);
        }), 20);
        const initialByType = await I.executeScript(() => {
            window.autotestMonitoringRoots = {};
            return Object.fromEntries(['server-memory', 'server-cpu'].map(type => {
                const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
                const root = window.am5.registry.rootElements.find(root => root.dom === element);
                const chart = root.container.children.values.find(child => child.series && child.xAxes);
                window.autotestMonitoringRoots[type] = root;
                return [type, { id: element.id, count: chart.series.getIndex(0).dataItems.length,
                    value: chart.series.getIndex(0).dataItems.at(-1).get('valueY') }];
            }));
        });
        for (const type of ['server-memory', 'server-cpu']) {
            I.waitForFunction(([type]) => {
                const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
                const root = window.am5?.registry.rootElements.find(root => root.dom === element);
                const chart = root?.container.children.values.find(child => child.series && child.xAxes);
                return Boolean(chart?.series.getIndex(0)?.dataItems.length >= 1 && chart.plotContainer.width() > 0);
            }, [type], 20);
            I.executeScript(type => {
                const scrollbar = window.scrollbarMain;
                scrollbar.setMomentum(0, 0);
                scrollbar.update();
                scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector(`[data-widget-type="${type}"]`).getBoundingClientRect().top - 64);
            }, type);
            const initial = initialByType[type];
            I.waitForFunction(([type, initial]) => {
                const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
                const root = window.am5?.registry.rootElements.find(root => root.dom === element);
                const chart = root?.container.children.values.find(child => child.series && child.xAxes);
                return Boolean(chart && chart.series.getIndex(0).dataItems.length > initial.count
                    && chart.series.getIndex(0).dataItems.at(-1).get('valueY') !== initial.value);
            }, [type, initial], 15);
            I.assertEqual(await I.grabAttributeFrom(`[data-widget-type="${type}"] .md-dashboard-widget__chart`, 'id'), initial.id,
                'New live samples must update the current chart without replacing its root.');
            const index = await I.executeScript(type => {
                const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
                const root = window.am5.registry.rootElements.find(root => root.dom === element);
                return root.container.children.values.find(child => child.series && child.xAxes).series.getIndex(0).dataItems.length - 1;
            }, type);
            await hoverDate(I, index, type);
            const state = await I.executeScript(type => {
                const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
                const root = window.am5.registry.rootElements.find(root => root.dom === element);
                const chart = root.container.children.values.find(child => child.series && child.xAxes);
                const tooltip = chart.xAxes.getIndex(0).get('tooltip');
                return {
                    visible: tooltip.isVisible(),
                    series: chart.series.values.map(series => ({ text: series.get('tooltip').label.getText(), value: series.dataItems.at(-1).get('valueY') })),
                    summary: [...element.closest('[data-widget-type]').querySelectorAll('.md-dashboard-widget__monitoring-values dd')].map(node => node.textContent),
                    tableCount: element.closest('[data-widget-type]').querySelectorAll('.visually-hidden table tbody tr').length,
                    pointCount: chart.series.getIndex(0).dataItems.length,
                    baseInterval: chart.xAxes.getIndex(0).get('baseInterval'),
                    sameRoot: window.autotestMonitoringRoots[type] === root && !root.isDisposed(),
                    domTooltips: [...element.querySelectorAll('[role="tooltip"]')].map(node => node.textContent)
                };
            }, type);
            I.assertTrue(state.visible, `The ${type} date tooltip must be available in the ${size} chart.`);
            I.assertTrue(state.sameRoot, 'Live updates must retain the same live AmCharts root object.');
            I.assertDeepEqual(state.baseInterval, { timeUnit: 'second', count: 5 }, 'The date axis must cover each complete five-second monitoring interval.');
            for (const series of state.series) {
                I.assertContain(series.text, String(series.value), 'Series tooltip values must be populated.');
                I.assertContain(series.text, type === 'server-memory' ? 'MB' : '%', 'Monitoring tooltip units must remain visible.');
            }
            I.assertEqual(state.tableCount, state.pointCount, 'The accessible table must grow with the live chart.');
            state.series.forEach((series, index) => I.assertContain(state.summary[index], String(series.value), 'Current monitoring numbers must follow the newest chart sample.'));
            I.assertFalse(state.domTooltips.some(value => /\[bold\]|\{(?:name|valueY)\}/.test(value)), 'Accessible tooltip nodes must not duplicate unresolved canvas formatting.');
        }
    }
    I.assertEqual(historicalRequests, 0, 'Live cards must never depend on removed historical dashboard providers.');
    I.assertAbove(actualRequests, 1, 'Monitoring must fetch current snapshots after the initial sample.');
});

/**
 * Removes the simulated chart data and settings responses, then reopens the dashboard using the normal
 * server data.
 */
Scenario('Restore unmocked dashboard requests', async ({ I }) => {
    for (const route of [...routes, dashboardPageRoute]) await I.stopMockingRoute(route);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
