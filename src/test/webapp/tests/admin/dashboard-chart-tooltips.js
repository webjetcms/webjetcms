Feature('admin.dashboard-chart-tooltips').tag('@singlethread');

const routes = ['**/admin/rest/dashboard/settings', '**/admin/rest/dashboard/notices', '**/admin/rest/dashboard/data/traffic*',
    '**/admin/rest/dashboard/data/server-memory*', '**/admin/rest/dashboard/data/server-cpu*'];
const day = 24 * 60 * 60 * 1000;
const from = new Date(2026, 8, 20).getTime();
const values = [40, 200, 104, 190, 110, 45, 5];

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
    for (const route of routes.slice(3)) await I.mockRoute(route, request => request.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        from, to: from + 6 * 60000,
        series: values.map((value, index) => ({ date: from + index * 60000, used: value, free: 512 - value, total: 512, process: index + 2, system: index + 12 }))
    }) }));
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
    const offset = await I.executeScript(({ index, type }) => {
        const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        const point = chart.plotContainer.toGlobal({ x: chart.plotContainer.width() * index / 6, y: chart.plotContainer.height() / 2 });
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
        for (const type of ['server-memory', 'server-cpu']) {
            I.waitForFunction(([type]) => {
                const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
                const root = window.am5?.registry.rootElements.find(root => root.dom === element);
                const chart = root?.container.children.values.find(child => child.series && child.xAxes);
                return Boolean(chart?.series.getIndex(0)?.dataItems.length === 7 && chart.plotContainer.width() > 100);
            }, [type], 20);
            I.executeScript(type => {
                const scrollbar = window.scrollbarMain;
                scrollbar.setMomentum(0, 0);
                scrollbar.update();
                scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector(`[data-widget-type="${type}"]`).getBoundingClientRect().top - 64);
            }, type);
            await hoverDate(I, 2, type);
            const state = await I.executeScript(type => {
                const element = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
                const root = window.am5.registry.rootElements.find(root => root.dom === element);
                const chart = root.container.children.values.find(child => child.series && child.xAxes);
                const tooltip = chart.xAxes.getIndex(0).get('tooltip');
                const expectedColor = document.createElement('span').style;
                expectedColor.backgroundColor = getComputedStyle(element.closest('.md-dashboard')).getPropertyValue(type === 'server-memory' ? '--wj-dashboard-lavender' : '--wj-dashboard-mint').trim();
                return {
                    visible: tooltip.isVisible(), bounds: tooltip.globalBounds(), height: element.clientHeight, plotHeight: chart.plotContainer.height(),
                    series: chart.series.values.map(series => ({ text: series.get('tooltip').label.getText(), value: series.dataItems[2].get('valueY') })),
                    domTooltips: [...element.querySelectorAll('[role="tooltip"]')].map(node => node.textContent),
                    color: getComputedStyle(element.closest('[data-widget-type]')).backgroundColor,
                    background: expectedColor.backgroundColor
                };
            }, type);
            I.assertTrue(state.visible && state.bounds.top >= 0 && state.bounds.bottom <= state.height + 1,
                `The complete ${type} date tooltip must fit its ${size} canvas: ${JSON.stringify(state.bounds)}.`);
            for (const series of state.series) {
                I.assertContain(series.text, String(series.value), 'Series tooltip values must be populated.');
                I.assertContain(series.text, type === 'server-memory' ? 'MB' : '%', 'Monitoring tooltip units must remain visible.');
            }
            I.assertFalse(state.domTooltips.some(value => /\[bold\]|\{(?:name|valueY)\}/.test(value)), 'Accessible tooltip nodes must not duplicate unresolved canvas formatting.');
            I.assertEqual(state.color, state.background, 'Monitoring cards must use their assigned dashboard color.');
            heights[`${type}-${size}`] = state.plotHeight;
            I.saveScreenshot(`dashboard-monitoring-tooltip-${type}-${size}.png`);
        }
    }
    for (const type of ['server-memory', 'server-cpu']) I.assertAbove(heights[`${type}-3x3`], heights[`${type}-3x2`] + 40,
        'The larger default must increase the plotted graph height, not only the card background.');
});

Scenario('Restore unmocked dashboard requests', async ({ I }) => {
    for (const route of routes) await I.stopMockingRoute(route);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
