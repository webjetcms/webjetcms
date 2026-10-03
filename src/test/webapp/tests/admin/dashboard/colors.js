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

/** Reads actual AmCharts colors, including the final point, after asynchronous canvas validation. */
async function assertTrafficColors(I, primary, comparison, surface) {
    await showWidget(I, widgetId);
    I.waitForFunction(id => {
        const host = document.querySelector(`[data-instance-id="${id}"] .md-dashboard-widget__chart`);
        const root = window.am5?.registry.rootElements.find(root => root.dom === host);
        const findChart = item => item?.series ? item : item?.children?.values.map(findChart).find(Boolean);
        return findChart(root?.container)?.series.getIndex(0).dataItems.at(-1)?.bullets?.length > 0;
    }, [widgetId], 20);
    const colors = await I.executeScript(id => {
        const card = document.querySelector(`[data-instance-id="${id}"]`);
        const host = card.querySelector('.md-dashboard-widget__chart');
        const root = window.am5.registry.rootElements.find(root => root.dom === host);
        const findChart = item => item.series ? item : item.children?.values.map(findChart).find(Boolean);
        const chart = findChart(root.container);
        const current = chart.series.getIndex(0);
        const bullet = current.dataItems.at(-1).bullets[0].get('sprite');
        const hex = value => '#' + value.match(/[\d.]+/g).slice(0, 3).map(channel => Math.round(Number(channel)).toString(16).padStart(2, '0')).join('');
        return {
            primary: current.get('stroke').toCSSHex(), fill: current.get('fill').toCSSHex(),
            comparison: chart.series.getIndex(1).get('stroke').toCSSHex(),
            dash: chart.series.getIndex(1).strokes.template.get('strokeDasharray'),
            bullet: bullet.get('fill').toCSSHex(), ring: bullet.get('stroke').toCSSHex(),
            legend: [...card.querySelectorAll('.md-dashboard-widget__chart-key')].map(key => hex(getComputedStyle(key, '::before').borderTopColor)),
            roots: window.am5.registry.rootElements.length
        };
    }, widgetId);
    I.assertEqual(colors.primary, primary);
    I.assertEqual(colors.fill, primary);
    I.assertEqual(colors.comparison, comparison);
    I.assertEqual(colors.bullet, primary);
    I.assertEqual(colors.ring, surface);
    I.assertDeepEqual(colors.legend, [primary, comparison]);
    I.assertDeepEqual(colors.dash, [5, 4]);
    I.assertEqual(colors.roots, 1, 'Background changes must dispose the previous chart root.');
}

/** Verifies the approved palette, staged persistence, custom picker cancellation and default restoration. */
Scenario('Widget backgrounds keep defaults and persist palette or readable custom colors', async ({ I }) => {
    saved = { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true, domainOptions: {},
        items: [{ id: widgetId, type: 'traffic', size: '1x1', options: { days: 7, metric: 'sessions' } }] };
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
    I.clickCss(editButton);
    await openSettings(I);
    I.see('Farba pozadia', palette);
    I.seeCheckboxIsChecked(palette + ' input[value="default"]');
    I.seeNumberOfElements(palette + ' .md-dashboard__shortcut-swatch', 12);
    I.seeElement(palette + ' input[value="yellow"][aria-label="Jemná žltá"]');
    I.see('Vlastná…', palette + ' input[value="custom"] + span');
    const defaultSwatch = palette + ' input[value="default"] + span .md-dashboard__shortcut-custom-color-swatch';
    I.assertEqual(await I.grabCssPropertyFrom(defaultSwatch, 'background-color'), original);
    for (const [name, expected] of [['yellow', 'rgb(254, 242, 204)'], ['figma-cream', 'rgb(255, 242, 225)'],
        ['peach', 'rgb(255, 241, 236)'], ['pink', 'rgb(255, 240, 241)'],
        ['figma-lavender', 'rgb(245, 242, 255)'], ['figma-blue', 'rgb(241, 243, 255)'],
        ['light-blue', 'rgb(242, 247, 255)'], ['cyan', 'rgb(225, 247, 255)'],
        ['green', 'rgb(228, 251, 210)'], ['figma-mint', 'rgb(223, 249, 241)']]) {
        I.clickCss(`${palette} input[value="${name}"] + span`);
        I.assertEqual(await I.grabCssPropertyFrom(modal + ' .md-dashboard__color-preview', 'background-color'), expected);
    }
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
    I.assertEqual(await I.grabCssPropertyFrom(widget, 'background-color'), 'rgb(223, 249, 241)');
    I.assertEqual(mutations.length, 0, 'Apply must not persist the draft');
    I.clickCss(editButton);
    await waitForSave(I);
    I.assertEqual(saved.items[0].options.backgroundColor, 'figma-mint');
    I.refreshPage();
    await showWidget(I, widgetId);
    I.assertEqual(await I.grabCssPropertyFrom(widget, 'background-color'), 'rgb(223, 249, 241)');
    I.clickCss(editButton);
    await openSettings(I);
    const custom = palette + ' input[value="custom"]';
    const hex = modal + ' color-picker [part="hex-input"]';
    I.clickCss(custom + ' + span');
    I.waitForVisible(hex, 10);
    I.fillField(hex, '#FFF1EC');
    I.pressKey('Escape');
    I.waitForInvisible(hex, 10);
    I.seeCheckboxIsChecked(palette + ' input[value="figma-mint"]');
    I.seeElement(modal);
    I.clickCss(custom + ' + span');
    I.waitForVisible(hex, 10);
    I.fillField(hex, '#112233');
    I.clickCss(modal + ' color-picker [part="confirm"]');
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.see('Pre čitateľný text zvoľte svetlejšiu farbu pozadia.', modal + ' [role="alert"]');
    I.clickCss(custom + ' + span');
    I.waitForVisible(hex, 10);
    I.fillField(hex, '#FFF1EC');
    I.clickCss(modal + ' color-picker [part="confirm"]');
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForInvisible(modal, 10);
    I.assertEqual(await I.grabCssPropertyFrom(widget, 'background-color'), 'rgb(255, 241, 236)');
    I.clickCss(editButton);
    await waitForSave(I);
    I.assertEqual(saved.items[0].options.backgroundColor, '#fff1ecff');
    I.refreshPage();
    await showWidget(I, widgetId);
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
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(dashboardPageRoute);
});

Scenario('Traffic colors follow palette and transparent custom backgrounds after apply, save and default restoration', async ({ I }) => {
    saved = { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true, domainOptions: {},
        items: [{ id: widgetId, type: 'traffic', size: '3x3', options: { days: 7, metric: 'sessions' } }] };
    await I.mockRoute(settingsRoute, route => {
        saved = { ...route.request().postDataJSON(), configured: true };
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(saved) });
    });
    await mockDashboardBootstrap(I, () => ({ settings: saved }));
    I.resizeWindow(1448, 1231);
    I.amOnPage('/admin/v9/');
    await assertTrafficColors(I, '#0e816b', '#40776d', '#e3f8f4');
    I.clickCss(editButton);
    for (const [name, primary, comparison, surface] of [['figma-blue', '#0e1f81', '#404877', '#f1f3ff'],
        ['pink', '#810e16', '#774044', '#fff0f1'], ['white', '#474747', '#5c5c5c', '#ffffff'],
        ['gray', '#474747', '#5c5c5c', '#f3f3f6']]) {
        await openSettings(I);
        I.clickCss(`${palette} input[value="${name}"] + span`);
        I.clickCss(modal + ' .modal-footer .btn-primary');
        I.waitForInvisible(modal, 10);
        await assertTrafficColors(I, primary, comparison, surface);
        if (name === 'figma-blue') I.saveElementScreenshot(widget, 'dashboard-traffic-derived-colors-blue.png');
    }
    await openSettings(I);
    I.clickCss(palette + ' input[value="custom"] + span');
    const hex = modal + ' color-picker [part="hex-input"]';
    I.waitForVisible(hex, 10);
    I.fillField(hex, '#DFF9F180');
    I.clickCss(modal + ' color-picker [part="confirm"]');
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForInvisible(modal, 10);
    await assertTrafficColors(I, '#0e815d', '#407766', '#effcf8');
    I.clickCss(editButton);
    await waitForSave(I);
    I.assertEqual(saved.items[0].options.backgroundColor, '#dff9f180');
    I.refreshPage();
    await assertTrafficColors(I, '#0e815d', '#407766', '#effcf8');
    I.clickCss(editButton);
    await openSettings(I);
    I.clickCss(palette + ' input[value="default"] + span');
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForInvisible(modal, 10);
    await assertTrafficColors(I, '#0e816b', '#40776d', '#e3f8f4');
    I.clickCss(editButton);
    await waitForSave(I);
    I.assertEqual(saved.items[0].options.backgroundColor, undefined);
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(dashboardPageRoute);
});
