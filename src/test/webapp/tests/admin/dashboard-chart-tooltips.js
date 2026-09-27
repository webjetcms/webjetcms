Feature('admin.dashboard-chart-tooltips').tag('@singlethread');

const routes = ['**/admin/rest/dashboard/settings', '**/admin/rest/dashboard/notices', '**/admin/rest/dashboard/data/traffic*',
    '**/admin/rest/monitoring/actual', '**/admin/rest/dashboard/data/server-memory*', '**/admin/rest/dashboard/data/server-cpu*'];
const day = 24 * 60 * 60 * 1000;
const from = new Date(2026, 8, 20).getTime();
const values = [40, 200, 104, 190, 110, 45, 5];
let actualRequests;
let historicalRequests;

Before(async ({ I, login }) => {
    login('admin');
    // Intercept preferences and chart data without changing the user's persisted dashboard.
    let settings = {
        version: 1, configured: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: 'tooltip-autotest-news', type: 'news', size: '3x2', collapsed: true, options: {} },
            ...['traffic', 'recent-pages', 'search-terms', 'top-pages', 'server-memory', 'server-cpu'].map(type => ({
                id: `tooltip-autotest-${type}`, type, size: '3x3', collapsed: false, options: { days: 7 }
            }))
        ]
    };
    await I.mockRoute(routes[0], route => {
        if (route.request().method() === 'PUT') settings = route.request().postDataJSON();
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(settings) });
    });
    await I.mockRoute(routes[1], route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
    await I.mockRoute(routes[2], route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        total: 694, previous: 1450, metric: 'sessions', from, to: from + 6 * day, items: [],
        series: values.map((value, index) => ({ date: from + index * day, value })),
        previousSeries: [350, 300, 110, 210, 160, 220, 100].map((value, index) => ({ date: from + (index - 7) * day, value }))
    }) }));
    actualRequests = 0;
    historicalRequests = 0;
    await I.mockRoute(routes[3], route => {
        const value = ++actualRequests;
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
            serverActualTime: from + value * 5000, memUsed: (128 + value) * 1048576, memFree: (384 - value) * 1048576,
            memTotal: 512 * 1048576, cpuUsageProcess: value, cpuUsage: value + 10
        }) });
    });
    for (const route of routes.slice(4)) await I.mockRoute(route, request => {
        historicalRequests++;
        return request.fulfill({ status: 400, contentType: 'application/json', body: '{}' });
    });
    I.resizeWindow(1337, 1241);
    I.amOnPage('/admin/v9/');
    I.waitForFunction(() => {
        const element = document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart');
        const root = window.am5?.registry.rootElements.find(root => root.dom === element);
        const chart = root?.container.children.values.find(child => child.series && child.xAxes);
        return Boolean(chart?.series.getIndex(0)?.dataItems.length === 7 && chart.plotContainer.width() > 100);
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
    I.saveScreenshot('dashboard-chart-tooltip-scrolled.png');
    I.executeScript(() => window.scrollbarMain.setPosition(0, 0));
    I.waitForFunction(() => window.scrollbarMain.offset.y === 0, 5);
    await hoverDate(I, 1);
});

Scenario('Traffic date tooltip is fully inside its rendering surface', async ({ I }) => {
    await hoverDate(I, 2);
    const bounds = await I.executeScript(() => {
        const element = document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart');
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        const tooltip = chart.xAxes.getIndex(0).get('tooltip');
        return { visible: tooltip.isVisible(), ...tooltip.globalBounds(), height: element.clientHeight };
    });
    I.assertTrue(bounds.visible, 'The hovered date tooltip must be visible.');
    I.assertTrue(bounds.top >= 0 && bounds.bottom <= bounds.height + 1,
        `The complete date tooltip must fit the chart rendering surface: ${JSON.stringify(bounds)}.`);
    I.saveScreenshot('dashboard-chart-tooltip-complete.png');
});

Scenario('Monitoring tooltips stay complete in compact and default charts with a taller plotted graph', async ({ I }) => {
    const heights = {};
    for (const size of ['3x2', '3x3']) {
        I.assertTrue(await I.executeScript(async size => {
            const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
            const next = JSON.parse(JSON.stringify(controller.settings));
            next.items.filter(item => item.type.startsWith('server-')).forEach(item => { item.size = size; });
            return controller._commit(next);
        }, size), 'Mocked settings must apply the chart size without changing persisted preferences.');
        I.waitForFunction(() => ['server-memory', 'server-cpu'].every(type => {
            const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
            const root = window.am5?.registry.rootElements.find(root => root.dom === element);
            const chart = root?.container.children.values.find(child => child.series && child.xAxes);
            return Boolean(chart?.series.getIndex(0)?.dataItems.length >= 1 && chart.plotContainer.width() > 100);
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
                return Boolean(chart?.series.getIndex(0)?.dataItems.length >= 1 && chart.plotContainer.width() > 100);
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
                const expectedColor = document.createElement('span').style;
                expectedColor.backgroundColor = getComputedStyle(element.closest('.md-dashboard')).getPropertyValue(type === 'server-memory' ? '--wj-dashboard-lavender' : '--wj-dashboard-mint').trim();
                return {
                    visible: tooltip.isVisible(), bounds: tooltip.globalBounds(), height: element.clientHeight, plotHeight: chart.plotContainer.height(),
                    series: chart.series.values.map(series => ({ text: series.get('tooltip').label.getText(), value: series.dataItems.at(-1).get('valueY') })),
                    summary: [...element.closest('[data-widget-type]').querySelectorAll('.md-dashboard-widget__monitoring-values dd')].map(node => node.textContent),
                    tableCount: element.closest('[data-widget-type]').querySelectorAll('.visually-hidden table tbody tr').length,
                    pointCount: chart.series.getIndex(0).dataItems.length,
                    baseInterval: chart.xAxes.getIndex(0).get('baseInterval'),
                    sameRoot: window.autotestMonitoringRoots[type] === root && !root.isDisposed(),
                    domTooltips: [...element.querySelectorAll('[role="tooltip"]')].map(node => node.textContent),
                    color: getComputedStyle(element.closest('[data-widget-type]')).backgroundColor,
                    background: expectedColor.backgroundColor
                };
            }, type);
            I.assertTrue(state.visible && state.bounds.top >= 0 && state.bounds.bottom <= state.height + 1,
                `The complete ${type} date tooltip must fit its ${size} canvas: ${JSON.stringify(state.bounds)}.`);
            I.assertTrue(state.sameRoot, 'Live updates must retain the same live AmCharts root object.');
            I.assertDeepEqual(state.baseInterval, { timeUnit: 'second', count: 5 }, 'The date axis must cover each complete five-second monitoring interval.');
            for (const series of state.series) {
                I.assertContain(series.text, String(series.value), 'Series tooltip values must be populated.');
                I.assertContain(series.text, type === 'server-memory' ? 'MB' : '%', 'Monitoring tooltip units must remain visible.');
            }
            I.assertEqual(state.tableCount, state.pointCount, 'The accessible table must grow with the live chart.');
            state.series.forEach((series, index) => I.assertContain(state.summary[index], String(series.value), 'Current monitoring numbers must follow the newest chart sample.'));
            I.assertFalse(state.domTooltips.some(value => /\[bold\]|\{(?:name|valueY)\}/.test(value)), 'Accessible tooltip nodes must not duplicate unresolved canvas formatting.');
            I.assertEqual(state.color, state.background, 'Monitoring cards must use their assigned dashboard color.');
            heights[`${type}-${size}`] = state.plotHeight;
            I.saveScreenshot(`dashboard-monitoring-tooltip-${type}-${size}.png`);
        }
    }
    for (const type of ['server-memory', 'server-cpu']) I.assertAbove(heights[`${type}-3x3`], heights[`${type}-3x2`] + 40,
        'The larger default must increase the plotted graph height, not only the card background.');
    I.assertEqual(historicalRequests, 0, 'Live cards must never depend on removed historical dashboard providers.');
    I.assertAbove(actualRequests, 1, 'Monitoring must fetch current snapshots after the initial sample.');
});

Scenario('Restore unmocked dashboard requests', async ({ I }) => {
    for (const route of routes) await I.stopMockingRoute(route);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
