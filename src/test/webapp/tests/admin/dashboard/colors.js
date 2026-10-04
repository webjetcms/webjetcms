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
        return window.am5?.registry.rootElements.some(root => root.dom === host);
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
    I.see('Pre čitateľný text zvoľte svetlejšiu farbu pozadia.', modal + ' [role="alert"]');
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
