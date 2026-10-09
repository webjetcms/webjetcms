const { mockDashboardBootstrap, dashboardPageRoute, showWidget } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.colors');

const settingsRoute = '**/admin/rest/dashboard/settings';
const widgetId = 'colors-autotest-traffic';
const widget = `[data-instance-id="${widgetId}"]`;
const modal = '.md-dashboard-modal--settings';
const palette = modal + ' .md-dashboard__widget-colors';
const editButton = '.md-dashboard__toolbar-actions > button[aria-pressed]';
let saved, mutations;

Before(({ login }) => { login('admin'); });

async function openSettings(I) {
    await showWidget(I, widgetId);
    I.clickCss(widget + ' [data-bs-toggle="dropdown"]');
    I.clickCss(widget + ' [data-dashboard-action="settings"]');
    I.waitForVisible(palette, 10);
}

function waitForSave(I) {
    return I.waitForFunction(() => document.querySelector('webjet-overview-dashboard').dashboardController.saving === false, 20);
}

/** Reads the rendered chart to catch missing color propagation and leaked preview roots. */
async function trafficColor(I) {
    await showWidget(I, widgetId);
    I.waitForFunction(id => {
        const host = document.querySelector(`[data-instance-id="${id}"] .md-dashboard-widget__chart`);
        // Bootstrap disposes the settings preview after its closing transition.
        const roots = window.am5?.registry.rootElements;
        return !document.querySelector('.md-dashboard-modal--settings')
            && roots?.length === 1 && roots[0].dom === host;
    }, [widgetId], 20);
    const chart = await I.executeScript(id => {
        const host = document.querySelector(`[data-instance-id="${id}"] .md-dashboard-widget__chart`);
        const root = window.am5.registry.rootElements.find(root => root.dom === host);
        const findChart = item => item?.series ? item : item?.children?.values.map(findChart).find(Boolean);
        const series = findChart(root.container).series;
        return { primary: series.getIndex(0).get('stroke').toCSSHex(),
            comparison: series.getIndex(1).get('stroke').toCSSHex(), roots: window.am5.registry.rootElements.length };
    }, widgetId);
    I.assertNotEqual(chart.primary, chart.comparison, 'Current and comparison lines must remain distinguishable.');
    I.assertEqual(chart.roots, 1, 'Applying settings must dispose the previous chart and preview roots.');
    return chart.primary;
}

/** Verifies the approved palette, staged persistence, custom picker cancellation and default restoration. */
Scenario('Widget backgrounds keep defaults and persist palette or readable custom colors', async ({ I }) => {
    saved = { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true, domainOptions: {},
        items: [{ id: widgetId, type: 'traffic', size: '3x3', options: { days: 7, metric: 'sessions' } }] };
    mutations = [];
    await I.mockRoute(settingsRoute, route => {
        mutations.push(route.request().postDataJSON());
        saved = { ...route.request().postDataJSON(), configured: true };
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(saved) });
    });
    await mockDashboardBootstrap(I, () => ({ settings: saved }));
    I.resizeWindow(1448, 1231);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await showWidget(I, widgetId);
    const original = await I.grabCssPropertyFrom(widget, 'background-color');
    const originalLine = await trafficColor(I);
    I.clickCss(editButton);
    await openSettings(I);
    I.seeCheckboxIsChecked(palette + ' input[value="default"]');
    const defaultSwatch = palette + ' input[value="default"] + span .md-dashboard__shortcut-custom-color-swatch';
    I.assertEqual(await I.grabCssPropertyFrom(defaultSwatch, 'background-color'), original);
    I.clickCss(palette + ' input[value="figma-blue"] + span');
    const selectedSurface = await I.grabCssPropertyFrom(modal + ' .md-dashboard__widget-preview .md-dashboard__widget', 'background-color');
    I.assertNotEqual(selectedSurface, original, 'A preset must update the preview.');
    I.saveScreenshot('dashboard-widget-background-settings.png');
    I.resizeWindow(390, 900);
    I.waitForFunction(() => {
        const body = document.querySelector('.md-dashboard-modal--settings .modal-body');
        const bounds = body.getBoundingClientRect();
        return bounds.left >= 0 && bounds.right <= window.innerWidth && body.scrollWidth <= body.clientWidth;
    }, 10);
    I.saveScreenshot('dashboard-widget-background-settings-mobile.png');
    I.resizeWindow(1448, 1231);
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForInvisible(modal, 10);
    I.assertEqual(await I.grabCssPropertyFrom(widget, 'background-color'), selectedSurface);
    I.assertEqual(mutations.length, 0, 'Apply must not persist the draft');
    I.clickCss(editButton);
    await waitForSave(I);
    I.assertEqual(saved.items[0].options.backgroundColor, 'figma-blue');
    const selectedLine = await trafficColor(I);
    I.assertNotEqual(selectedLine, originalLine, 'The chart must use the selected surface after Apply.');
    I.refreshPage();
    await showWidget(I, widgetId);
    I.assertEqual(await I.grabCssPropertyFrom(widget, 'background-color'), selectedSurface);
    I.assertEqual(await trafficColor(I), selectedLine, 'Reload must keep the saved chart colors.');
    I.clickCss(editButton);
    await openSettings(I);
    const custom = palette + ' input[value="custom"]';
    const hex = modal + ' color-picker [part="hex-input"]';
    I.clickCss(custom + ' + span');
    I.waitForVisible(hex, 10);
    I.fillField(hex, '#FFF1EC');
    I.pressKey('Escape');
    I.waitForInvisible(hex, 10);
    I.seeCheckboxIsChecked(palette + ' input[value="figma-blue"]');
    I.seeElement(modal);
    I.clickCss(custom + ' + span');
    I.waitForVisible(hex, 10);
    I.fillField(hex, '#112233');
    I.clickCss(modal + ' color-picker [part="confirm"]');
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForText('Pre čitateľný text zvoľte svetlejšiu farbu pozadia.', 10, modal + ' [role="alert"]');
    I.clickCss(custom + ' + span');
    I.waitForVisible(hex, 10);
    I.fillField(hex, '#DFF9F180');
    I.clickCss(modal + ' color-picker [part="confirm"]');
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForInvisible(modal, 10);
    const customLine = await trafficColor(I);
    I.assertNotEqual(customLine, selectedLine, 'A translucent custom surface must update chart colors.');
    I.clickCss(editButton);
    await waitForSave(I);
    I.assertEqual(saved.items[0].options.backgroundColor, '#dff9f180');
    I.refreshPage();
    await showWidget(I, widgetId);
    I.assertEqual(await trafficColor(I), customLine);
    I.clickCss(editButton);
    await openSettings(I);
    I.seeCheckboxIsChecked(custom);
    I.assertEqual(await I.grabCssPropertyFrom(defaultSwatch, 'background-color'), original, 'The default swatch must show the original surface even with a saved custom background');
    I.clickCss(palette + ' input[value="default"] + span');
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForInvisible(modal, 10);
    I.assertEqual(await I.grabCssPropertyFrom(widget, 'background-color'), original);
    I.clickCss(editButton);
    await waitForSave(I);
    I.assertEqual(saved.items[0].options.backgroundColor, undefined);
    I.assertEqual(await trafficColor(I), originalLine, 'Restoring the default must restore the chart colors.');
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(dashboardPageRoute);
});

/** Exercises visible series and date tooltips against every preset and custom alpha backgrounds. */
Scenario('Chart tooltips remain readable across widget background colors', async ({ I }) => {
    const backgrounds = ['default', 'white', 'gray', 'yellow', 'figma-cream', 'peach', 'pink', 'figma-lavender',
        'figma-blue', 'light-blue', 'cyan', 'figma-mint', 'green', '#fff1ec', '#dff9f180', '#11223300'];
    const settings = { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true, domainOptions: {},
        items: backgrounds.map((backgroundColor, index) => ({ id: `colors-autotest-tooltip-${index}`, type: 'traffic', size: '3x3',
            options: { days: 7, metric: 'sessions', ...(backgroundColor === 'default' ? {} : { backgroundColor }) } })) };
    const trafficRoute = '**/admin/rest/stat/views/search/findByColumns?*';
    await I.mockRoute(trafficRoute, route => {
        const from = Number(new URL(route.request().url()).searchParams.get('searchDayDate').match(/daterange:(\d+)-/)[1]);
        const content = Array.from({ length: 14 }, (_, index) => {
            const day = new Date(from);
            day.setDate(day.getDate() + index);
            return { dayDate: day.getTime(), sessions: index < 7 ? 80 + index * 10 : 20 + index * 5 };
        });
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content }) });
    });
    await mockDashboardBootstrap(I, () => ({ settings, statRootGroupId: 1 }));
    I.resizeWindow(1448, 1231);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    for (const [index, background] of backgrounds.entries()) {
        const id = settings.items[index].id;
        await showWidget(I, id);
        const host = `[data-instance-id="${id}"] .md-dashboard-widget__chart`;
        I.moveCursorTo(host);
        I.waitForFunction(selector => {
            const root = window.am5?.registry.rootElements.find(root => root.dom === document.querySelector(selector));
            const findChart = item => item?.series ? item : item?.children?.values.map(findChart).find(Boolean);
            const chart = root && findChart(root.container);
            return chart && [...chart.series.values, ...chart.xAxes.values].every(item => {
                const tooltip = item.get('tooltip');
                return tooltip.isVisible() && !tooltip.isHidden() && tooltip.get('opacity') === 1;
            });
        }, [host], 10);
        const tooltips = await I.executeScript(selector => {
            const root = window.am5.registry.rootElements.find(root => root.dom === document.querySelector(selector));
            const findChart = item => item?.series ? item : item?.children?.values.map(findChart).find(Boolean);
            const chart = findChart(root.container);
            const luminance = channels => channels.map(value => {
                return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
            }).reduce((sum, value, index) => sum + value * [0.2126, 0.7152, 0.0722][index], 0);
            return [...chart.series.values, ...chart.xAxes.values].map(item => {
                const tooltip = item.get('tooltip');
                const surface = tooltip.get('background');
                const channels = color => color.toCSSHex().slice(1).match(/../g).map(channel => parseInt(channel, 16) / 255);
                // A black backdrop is the worst case for dark text, including a chart line under the tooltip.
                const light = luminance(channels(surface.get('fill')).map(value => value * surface.get('fillOpacity')));
                const dark = luminance(channels(tooltip.label.get('fill')));
                return { fill: surface.get('fill').toCSSHex().toLowerCase(),
                    contrast: (Math.max(light, dark) + 0.05) / (Math.min(light, dark) + 0.05) };
            });
        }, host);
        I.assertEqual(tooltips.length, 3, `${background}: both periods and the date must have tooltips.`);
        for (const tooltip of tooltips) {
            I.assertEqual(tooltip.fill, '#ffffff', `${background}: tooltip surfaces must stay white after hover.`);
            I.assertTrue(tooltip.contrast >= 4.5, `${background}: tooltip text contrast over the darkest backdrop must be at least 4.5:1; got ${tooltip.contrast}.`);
        }
        if (['default', 'pink', 'figma-blue'].includes(background)) I.saveElementScreenshot(`[data-instance-id="${id}"]`, `dashboard-chart-tooltip-${background}.png`);
    }
    await I.stopMockingRoute(trafficRoute);
    await I.stopMockingRoute(dashboardPageRoute);
});
