Feature('admin.dashboard-chart-tooltips').tag('@singlethread');

const host = '[data-widget-type="traffic"] .md-dashboard-widget__chart';
const routes = ['**/admin/rest/dashboard/settings', '**/admin/rest/dashboard/notices', '**/admin/rest/dashboard/data/traffic*'];
const day = 24 * 60 * 60 * 1000;
const from = new Date(2026, 8, 20).getTime();
const values = [40, 200, 104, 190, 110, 45, 5];

Before(async ({ I, login }) => {
    login('admin');
    // Intercept preferences and chart data without changing the user's persisted dashboard.
    await I.mockRoute(routes[0], route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        version: 1, configured: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: 'tooltip-autotest-news', type: 'news', size: '3x2', collapsed: true, options: {} },
            ...['traffic', 'recent-pages', 'search-terms', 'top-pages'].map(type => ({
                id: `tooltip-autotest-${type}`, type, size: '3x3', collapsed: false, options: { days: 7 }
            }))
        ]
    }) }));
    await I.mockRoute(routes[1], route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
    await I.mockRoute(routes[2], route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        total: 694, previous: 1450, metric: 'sessions', from, to: from + 6 * day, items: [],
        series: values.map((value, index) => ({ date: from + index * day, value })),
        previousSeries: [350, 300, 110, 210, 160, 220, 100].map((value, index) => ({ date: from + (index - 7) * day, value }))
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
async function hoverDate(I, index) {
    I.moveCursorTo('.ly-header');
    const offset = await I.executeScript(index => {
        const element = document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart');
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        const point = chart.plotContainer.toGlobal({ x: chart.plotContainer.width() * index / 6, y: chart.plotContainer.height() / 2 });
        return { x: point.x - element.clientWidth / 2, y: point.y - element.clientHeight / 2 };
    }, index);
    I.moveCursorTo(host, offset.x - 2, offset.y);
    I.moveCursorTo(host, offset.x, offset.y);
    I.assertDeepEqual(await I.executeScript(() => window.autotestDashboardChartErrors), [],
        'Moving over the chart must not invoke disposed chart elements.');
    I.waitForFunction(([index]) => {
        const element = document.querySelector('[data-widget-type="traffic"] .md-dashboard-widget__chart');
        const root = window.am5.registry.rootElements.find(root => root.dom === element);
        const chart = root.container.children.values.find(child => child.series && child.xAxes);
        return chart.series.values.every(series => series.get('tooltip')?.isVisible()
            && series.get('tooltip').dataItem === series.dataItems[index]);
    }, [index], 5);
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

Scenario('Restore unmocked dashboard requests', async ({ I }) => {
    for (const route of routes) await I.stopMockingRoute(route);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
