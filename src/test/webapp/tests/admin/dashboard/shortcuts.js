Feature('admin.dashboard.shortcuts').tag('@singlethread');

let originalSettings;
let originalBookmarks;
const actions = '.md-dashboard__shortcut-actions';
const links = '.md-dashboard__shortcuts';
const modal = '.md-dashboard-modal';
const bannerHref = '/apps/banner/admin/?autotest=shortcut#detail';

function loaded(I) {
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    saved(I);
}

function saved(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

async function chooseShortcutOption(I, name, label, search = false) {
    const id = await I.grabAttributeFrom(`${modal} [name="dashboardShortcut${name}"]`, 'id');
    I.clickCss(`${modal} button[data-id="${id}"]`);
    if (search) I.fillField(`${modal} .bs-container > .dropdown-menu.show .bs-searchbox input`, label);
    I.click(locate(`${modal} .dropdown-menu.show .dropdown-item`).withText(label));
    I.waitForInvisible(`${modal} .bs-container > .dropdown-menu.show`, 10);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    loaded(I);
});

/**
 * Checks that shortcut editing and widget editing expose their own controls without being active together.
 */
Scenario('Shortcuts and widgets have independent editing modes', async ({ I }) => {
    originalSettings = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    originalBookmarks = await I.executeScript(() => localStorage.getItem('bookmarks'));
    I.see('Upraviť skratky', actions);
    I.dontSeeElement(`${actions} > button:first-child`);
    I.dontSeeElement(`${links} .md-dashboard__widget-controls`);
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.see('Pridať skratku', actions);
    I.see('Obnoviť', actions);
    I.dontSeeElement('.md-dashboard__layout .md-dashboard__widget-controls');
    I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    I.dontSeeElement(`${links} .md-dashboard__widget-controls`);
    I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="true"]');
});

/**
 * Creates a shortcut by choosing an administration area, section and tab, then changes its title, icon and
 * color. Reopening its settings on mobile must restore those choices.
 */
Scenario('Choose a banner tab through the administration hierarchy and restore it when editing', async ({ I }) => {
    I.resizeWindow(390, 900);
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.click('Pridať skratku', actions);
    I.waitForVisible(modal, 10);
    const tab = `${modal} [name="dashboardShortcutMenu"]`;
    await chooseShortcutOption(I, 'Group', 'Aplikácie', true);
    await chooseShortcutOption(I, 'Section', 'Bannerový systém', true);
    I.assertDeepEqual(await I.executeScript(selector => [...document.querySelector(selector).options].slice(1).map(option => option.textContent), tab), ['Zoznam bannerov', 'Štatistika bannerov']);
    await chooseShortcutOption(I, 'Menu', 'Štatistika bannerov');
    I.seeInField(`${modal} [name="dashboardShortcutIcon"]`, 'ad');
    I.fillField(`${modal} [name="dashboardShortcutIcon"]`, 'chart-bar');
    I.clickCss(`${modal} input[type="radio"][value="mint"] + span`);
    I.seeElement(`${modal} .md-dashboard__shortcut-preview .ti-chart-bar`);
    const title = `banner-tab-autotest-${I.getRandomTextShort()}`;
    I.fillField(`${modal} [name="dashboardShortcutTitle"]`, title);
    I.saveScreenshot('dashboard-shortcut-menu.png', true);
    I.clickCss(`${modal} .modal-footer .btn-primary`);
    I.waitForInvisible(modal, 10);
    saved(I);
    I.refreshPage();
    loaded(I);
    const id = await I.executeScript(title => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.options.title === title)?.id, title);
    I.assertTrue(Boolean(id), 'The selected card must persist as a shortcut.');
    I.seeElement(`${links} [data-instance-id="${id}"] a[href="/apps/banner/admin/banner-stat/"] .ti-chart-bar`);
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.clickCss(`[data-instance-id="${id}"] .dropdown > button`);
    I.clickCss(`[data-instance-id="${id}"] [data-dashboard-action="settings"]`);
    I.waitForVisible(modal, 10);
    I.assertDeepEqual(await I.executeScript(() => ['Group', 'Section', 'Menu'].map(name => document.querySelector(`[name="dashboardShortcut${name}"]`).selectedOptions[0].textContent)), ['Aplikácie', 'Bannerový systém', 'Štatistika bannerov']);
    I.seeInField(`${modal} [name="dashboardShortcutIcon"]`, 'chart-bar');
    I.seeCheckboxIsChecked(`${modal} input[value="mint"]`);
    const tabId = await I.grabAttributeFrom(tab, 'id');
    I.clickCss(`${modal} button[data-id="${tabId}"]`);
    I.fillField(`${modal} .bs-container > .dropdown-menu.show .bs-searchbox input`, 'Zoznam');
    I.pressKey('Escape');
    I.waitForInvisible(`${modal} .bs-container > .dropdown-menu.show`, 10);
    I.seeElement(modal);
    I.clickCss(`${modal} .btn-close`);
    I.waitForInvisible(modal, 10);
    I.dontSeeElement('.bs-container');
    I.wjSetDefaultWindowSize();
});

/**
 * Creates a custom-address shortcut with a chosen icon and color, reloads the dashboard and checks that both
 * its appearance and editable settings are retained.
 */
Scenario('Custom URL shortcuts retain their icon and color after saving and reopening', async ({ I }) => {
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.click('Pridať skratku', actions);
    I.waitForVisible(modal, 10);
    await chooseShortcutOption(I, 'Source', 'Vlastná URL adresa');
    I.dontSeeElement(`${modal} [name="dashboardShortcutGroup"]`);
    I.fillField(`${modal} [name="dashboardShortcutUrl"]`, 'https://example.com/autotest');
    const title = `custom-icon-autotest-${I.getRandomTextShort()}`;
    I.fillField(`${modal} [name="dashboardShortcutTitle"]`, title);
    I.fillField(`${modal} [name="dashboardShortcutIcon"]`, 'heart');
    I.clickCss(`${modal} input[value="lavender"] + span`);
    I.clickCss(`${modal} .modal-footer .btn-primary`);
    I.waitForInvisible(modal, 10);
    saved(I);
    I.refreshPage();
    loaded(I);
    const shortcut = await I.executeScript(title => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.options.title === title), title);
    I.assertEqual(shortcut.options.icon, 'ti-heart');
    I.assertEqual(shortcut.options.color, 'lavender');
    I.seeElement(`[data-instance-id="${shortcut.id}"] a[href="https://example.com/autotest"] .ti-heart`);
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.clickCss(`[data-instance-id="${shortcut.id}"] .dropdown > button`);
    I.clickCss(`[data-instance-id="${shortcut.id}"] [data-dashboard-action="settings"]`);
    I.waitForVisible(modal, 10);
    I.seeInField(`${modal} [name="dashboardShortcutIcon"]`, 'heart');
    I.seeCheckboxIsChecked(`${modal} input[value="lavender"]`);
    I.clickCss(`${modal} .btn-close`);
    I.waitForInvisible(modal, 10);
});

/**
 * Checks that old browser bookmarks are imported once as personal shortcuts without duplicates. A failed
 * save preserves both the original bookmarks and layout; a successful retry retains other widgets and
 * survives reloading.
 */
Scenario('Automatically import old bookmarks on load with failure recovery and server persistence', async ({ I }) => {
    I.assertTrue(Boolean(originalSettings), 'The fixture must preserve the original profile first.');
    const before = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    const origin = await I.executeScript(() => window.location.origin);
    const legacy = JSON.stringify([
        { name: 'Banner autotest', path: bannerHref },
        { name: 'Banner duplicate autotest', path: origin + bannerHref },
        { name: 'Forms autotest', path: '/apps/form/admin/' }
    ]);
    const prepared = await I.executeScript(async bookmarks => {
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        const next = JSON.parse(JSON.stringify(controller.settings));
        next.legacyBookmarksHandled = false;
        localStorage.setItem('bookmarks', bookmarks);
        return controller._commit(next);
    }, legacy);
    I.assertTrue(prepared);
    await I.mockRoute('**/admin/rest/dashboard/settings', route => route.request().method() === 'PUT'
        ? route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"autotest import failure"}' }) : route.continue());
    I.refreshPage();
    loaded(I);
    I.seeElement(`${links} .md-dashboard__status .text-danger`);
    I.assertFalse(await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.legacyBookmarksHandled));
    I.assertEqual(await I.executeScript(() => localStorage.getItem('bookmarks')), legacy);
    I.assertDeepEqual(await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings.items))), before.items);
    await I.stopMockingRoute('**/admin/rest/dashboard/settings');
    I.refreshPage();
    loaded(I);
    I.seeNumberOfElements(`${links} a[href="${bannerHref}"]`, 1);
    I.dontSeeElement('.md-dashboard__legacy-shortcuts');
    I.dontSeeElement(modal);
    const after = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    I.assertDeepEqual(after.items.filter(item => item.type === 'shortcut').map(item => item.options), [
        { source: 'url', href: bannerHref, title: 'Banner autotest' },
        { source: 'url', href: '/apps/form/admin/', title: 'Forms autotest' }
    ]);
    I.assertDeepEqual(after.items.filter(item => item.type !== 'shortcut'), before.items.filter(item => item.type !== 'shortcut'));
    for (const item of before.items.filter(item => item.type !== 'shortcut')) I.assertDeepEqual(after.domainOptions[item.id], before.domainOptions[item.id]);
    I.assertTrue(Object.keys(after.domainOptions).every(id => after.items.some(item => item.id === id)), 'Removed shortcuts must not leave orphaned preferences.');
    I.assertEqual(after.acknowledgedNewsVersion, before.acknowledgedNewsVersion);
    I.assertTrue(after.legacyBookmarksHandled);
    I.assertEqual(await I.executeScript(() => localStorage.getItem('bookmarks')), null);
    I.refreshPage();
    loaded(I);
    I.assertDeepEqual(await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings))), after);
    I.saveScreenshot('dashboard-shortcuts-welcome.png', true);
});

/**
 * Checks that restoring default shortcuts leaves widget settings and release-news choices intact. After the
 * user removes every shortcut, the section must remain empty after reloading.
 */
Scenario('Reset shortcuts preserves widgets and an empty shortcut section stays empty after reload', async ({ I }) => {
    const before = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.click('Obnoviť', actions);
    const confirm = '#toast-container-webjet .toast[role="dialog"]';
    I.waitForVisible(confirm, 10);
    I.clickCss(`${confirm} button[id^="confirmationYes"]`);
    I.waitForInvisible(confirm, 10);
    saved(I);
    const after = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    I.assertDeepEqual(after.items.filter(item => item.type !== 'shortcut'), before.items.filter(item => item.type !== 'shortcut'));
    I.assertEqual(after.acknowledgedNewsVersion, before.acknowledgedNewsVersion);
    for (const item of before.items.filter(item => item.type !== 'shortcut')) I.assertDeepEqual(after.domainOptions[item.id], before.domainOptions[item.id]);
    I.assertTrue(after.items.some(item => item.type === 'shortcut'));
    for (const item of after.items.filter(item => item.type === 'shortcut')) {
        I.clickCss(`[data-instance-id="${item.id}"] .dropdown > button`);
        I.clickCss(`[data-instance-id="${item.id}"] [data-dashboard-action="remove"]`);
        saved(I);
        I.waitForInvisible(`[data-instance-id="${item.id}"]`, 10);
    }
    I.refreshPage();
    loaded(I);
    I.seeElement('.md-dashboard__shortcuts-empty');
    I.dontSeeElement(`${links} a`);
});

/**
 * Restores the original dashboard preferences and browser bookmarks after the shortcut checks and removes
 * the simulated save response.
 */
Scenario('Restore the original dashboard and browser bookmarks', async ({ I }) => {
    await I.stopMockingRoute('**/admin/rest/dashboard/settings');
    if (!originalSettings) return;
    const restored = await I.executeScript(async ({ settings, bookmarks }) => {
        if (bookmarks === null) localStorage.removeItem('bookmarks');
        else localStorage.setItem('bookmarks', bookmarks);
        return document.querySelector('webjet-overview-dashboard').dashboardController._commit(settings);
    }, { settings: originalSettings, bookmarks: originalBookmarks });
    I.assertTrue(restored, 'Restore the original personal preferences.');
    I.wjSetDefaultWindowSize();
});
