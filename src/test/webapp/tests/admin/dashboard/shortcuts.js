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

function searchShortcut(I, value) {
    I.fillField(modal + ' [name="dashboardShortcutSearch"]', value);
    I.waitForVisible(modal + ' [role="listbox"]', 10);
}

function chooseShortcut(I, title) {
    I.click(locate(modal + ' [role="option"]').withText(title));
    I.waitForInvisible(modal + ' [role="listbox"]', 10);
}

function customIcon(I, name) {
    I.clickCss(modal + ' .md-dashboard__shortcut-icon-choices input[value="custom"] + span');
    I.fillField(modal + ' [name="dashboardShortcutIcon"]', name);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    loaded(I);
});

/**
 * Checks that shortcuts are visible near the top of the dashboard and fit narrow screens. Shortcut editing
 * and widget editing must expose their own controls without being active together.
 */
Scenario('Welcome shortcuts fit above the fold and own their editing mode', async ({ I }) => {
    originalSettings = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    originalBookmarks = await I.executeScript(() => localStorage.getItem('bookmarks'));
    I.seeElement(`${actions} .md-dashboard__shortcut-edit .ti-pencil`);
    I.see('Pridať skratku', actions);
    const dimensions = await I.executeScript(() => {
        const actions = document.querySelector('.md-dashboard__shortcut-actions');
        const add = actions.firstElementChild.getBoundingClientRect();
        const edit = actions.lastElementChild.getBoundingClientRect();
        const label = document.createRange();
        label.selectNodeContents([...actions.firstElementChild.childNodes].find(node => node.nodeType === Node.TEXT_NODE && node.textContent.trim()));
        return {
            heights: [...document.querySelectorAll('.md-dashboard__shortcut-list > [data-instance-id]')].map(card => card.getBoundingClientRect().height),
            addHeight: add.height, addWidth: add.width, editHeight: edit.height, editWidth: edit.width,
            editBorder: getComputedStyle(actions.lastElementChild).borderTopWidth,
            addLabelLines: label.getClientRects().length
        };
    });
    I.assertTrue(dimensions.heights.every(height => height === 32), 'Shortcut cards must be exactly 32 pixels high.');
    I.assertEqual(dimensions.addHeight, 32);
    I.assertEqual(dimensions.addWidth, 136);
    I.assertEqual(dimensions.editHeight, 32);
    I.assertEqual(dimensions.editWidth, 32);
    I.assertEqual(dimensions.editBorder, '0px');
    I.assertEqual(dimensions.addLabelLines, 1, 'The add label must stay on a single line within its 136 × 32 pixel button.');

    I.assertTrue(await I.executeScript(() => {
        const list = document.querySelector('.md-dashboard__shortcut-list');
        return list.lastElementChild.classList.contains('md-dashboard__shortcut-actions');
    }), 'Shortcut actions must follow the links in reading and wrapping order.');
    I.dontSeeElement(`${links} .md-dashboard__edit-control`);
    I.moveCursorTo('.md-dashboard__shortcut-edit');
    I.waitForText('Upraviť skratky', 5, '.tooltip');
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.waitForInvisible('.tooltip', 5);
    I.see('Hotovo', '.md-dashboard__welcome-heading');
    I.dontSeeElement(`${actions} .md-dashboard__shortcut-edit`);
    I.assertTrue(await I.executeScript(() => {
        const done = document.querySelector('.md-dashboard__welcome-heading .md-dashboard__shortcut-edit').getBoundingClientRect();
        const heading = document.querySelector('.md-dashboard__welcome-heading').getBoundingClientRect();
        const list = document.querySelector('.md-dashboard__shortcut-list').getBoundingClientRect();
        return done.height === 32 && done.bottom <= list.top && Math.abs(done.right - heading.right) < 1;
    }), 'Done must be above the shortcuts at the right edge of the welcome heading.');
    I.saveScreenshot('dashboard-shortcut-done.png', true);
    I.click('Hotovo', '.md-dashboard__welcome-heading');
    I.seeElement(`${actions} .md-dashboard__shortcut-edit .ti-pencil`);
    I.assertTrue(await I.executeScript(() => {
        const link = document.querySelector('.md-dashboard__shortcut-card a');
        const label = link.querySelector('.md-dashboard-widget__shortcut-label');
        if (label.scrollWidth > label.clientWidth) return true;
        window.bootstrap.Tooltip.getInstance(link).show();
        return !link.hasAttribute('aria-describedby') && !link.hasAttribute('title');
    }), 'A fully visible shortcut title must not have a duplicate tooltip.');
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.see('Pridať skratku', actions);
    I.dontSee('Obnoviť', actions);
    I.dontSeeElement(`${links} .ti-dots-vertical`);
    I.dontSeeElement('.md-dashboard__layout .md-dashboard__widget-controls');
    I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    I.dontSeeElement(`${links} .md-dashboard__edit-control`);
    I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="true"]');
    for (const width of [320, 390, 768, 1337]) {
        I.resizeWindow(width, 900);
        if (width < 768 && await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
        I.executeScript(() => { window.scrollbarMain.setMomentum(0, 0); window.scrollbarMain.setPosition(0, 0); });
        I.waitForFunction(() => window.scrollbarMain.offset.y === 0, 10);
        const geometry = await I.executeScript(() => {
            const region = document.querySelector('.md-dashboard__shortcuts');
            const rect = region.getBoundingClientRect();
            return { top: rect.top, bottom: rect.bottom, right: rect.right, viewport: innerWidth, overflow: region.scrollWidth > region.clientWidth + 1 };
        });
        I.assertTrue(geometry.top >= 48 && geometry.bottom < 900, `Shortcuts must be immediately available at ${width}px.`);
        I.assertTrue(geometry.right <= geometry.viewport + 1 && !geometry.overflow, `Shortcuts must wrap at ${width}px.`);
    }
    I.wjSetDefaultWindowSize();
});

/**
 * Searches authorized menu breadcrumbs, selects a tab with the keyboard and preserves its optional
 * title and appearance. The combobox and edit dialog must remain usable at narrow widths.
 */
Scenario('Choose a menu target through autocomplete and restore its settings when editing', async ({ I }) => {
    I.click('Pridať skratku', actions);
    I.waitForVisible(modal, 10);
    searchShortcut(I, 'banner');
    I.see('Sekcie administrácie', modal);
    I.see('Aplikácie', modal);
    I.see('Zoznam bannerov', modal);
    I.see('Štatistika bannerov', modal);
    I.pressKey('ArrowDown');
    I.pressKey('Enter');
    I.waitForInvisible(modal + ' [role="listbox"]', 10);
    I.seeInField(modal + ' [name="dashboardShortcutSearch"]', 'Štatistika bannerov');
    customIcon(I, 'chart-bar');
    I.clickCss(modal + ' .md-dashboard__shortcut-colors input[value="mint"] + span');
    I.seeElement(modal + ' .md-dashboard__shortcut-preview .ti-chart-bar');
    const title = 'banner-tab-autotest-' + I.getRandomTextShort();
    I.fillField(modal + ' [name="dashboardShortcutTitle"]', title);
    I.saveScreenshot('dashboard-shortcut-menu.png', true);
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.waitForInvisible(modal, 10);
    saved(I);
    I.refreshPage();
    loaded(I);
    const id = await I.executeScript(title => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.options.title === title)?.id, title);
    I.assertTrue(Boolean(id), 'The selected destination must persist as a shortcut.');
    I.seeElement('[data-instance-id="' + id + '"] a[href="/apps/banner/admin/banner-stat/"] .ti-chart-bar');
    I.assertTrue(await I.executeScript(id => {
        const card = document.querySelector('[data-instance-id="' + id + '"]');
        const icon = card.querySelector('.md-dashboard-widget__shortcut > .ti');
        return getComputedStyle(card).backgroundColor === 'rgb(255, 255, 255)' && getComputedStyle(icon).backgroundColor === 'rgb(0, 126, 105)';
    }, id), 'Only the shortcut icon must use the selected green color.');
    I.clickCss(actions + ' button[aria-pressed="false"]');
    I.clickCss('[data-instance-id="' + id + '"] .md-dashboard-widget__shortcut');
    I.waitForVisible(modal, 10);
    I.seeInField(modal + ' [name="dashboardShortcutSearch"]', 'Štatistika bannerov');
    I.seeInField(modal + ' [name="dashboardShortcutIcon"]', 'chart-bar');
    I.seeCheckboxIsChecked(modal + ' .md-dashboard__shortcut-colors input[value="mint"]');
    searchShortcut(I, 'formul');
    I.waitForText('Zoznam formulárov', 10, modal);
    I.saveScreenshot('dashboard-shortcut-autocomplete.png', true);
    I.pressKey('Escape');
    I.waitForInvisible(modal + ' [role="listbox"]', 10);
    I.seeElement(modal);
    for (const width of [390, 1024, 1337]) {
        I.resizeWindow(width, 900);
        I.assertTrue(await I.executeScript(() => {
            const dialog = document.querySelector('.md-dashboard-modal .modal-dialog').getBoundingClientRect();
            return dialog.left >= 0 && dialog.right <= innerWidth && document.querySelector('.md-dashboard-modal .modal-body').scrollWidth <= document.querySelector('.md-dashboard-modal .modal-body').clientWidth;
        }), 'Shortcut settings must fit at ' + width + 'px.');
    }
    I.wjSetDefaultWindowSize();
    I.clickCss(modal + ' .btn-close');
    I.waitForInvisible(modal, 10);
});

/**
 * Creates a custom-address shortcut with a chosen icon and color, reloads the dashboard and checks that both
 * its appearance and editable settings are retained.
 */
Scenario('Custom URL shortcuts retain their icon and color after saving and reopening', async ({ I }) => {
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.click('Pridať skratku', actions);
    I.waitForVisible(modal, 10);
    searchShortcut(I, '');
    I.clickCss(modal + ' .md-dashboard__shortcut-result-url');
    I.dontSeeElement(`${modal} [name="dashboardShortcutGroup"]`);
    I.fillField(`${modal} [name="dashboardShortcutUrl"]`, 'https://example.com/autotest');
    const title = `custom-icon-autotest-${I.getRandomTextShort()}`;
    I.fillField(`${modal} [name="dashboardShortcutTitle"]`, title);
    customIcon(I, 'rockcet');
    I.see('v knižnici neexistuje', modal);
    I.clickCss(modal + ' .modal-footer .btn-primary');
    I.seeElement(modal);
    I.see('Zadajte platný názov ikony', modal);
    I.saveScreenshot('dashboard-shortcut-invalid-icon.png', true);
    customIcon(I, 'heart');
    I.assertEqual(await I.grabNumberOfVisibleElements(`${modal} .md-dashboard__shortcut-swatch`), 9);
    I.clickCss(`${modal} .md-dashboard__shortcut-colors input[value="amber"] + span`);
    I.assertTrue(await I.executeScript(() => {
        const icon = document.querySelector('.md-dashboard__shortcut-preview > .ti');
        return getComputedStyle(icon).backgroundColor === 'rgb(246, 190, 63)' && getComputedStyle(icon).color === 'rgb(19, 21, 27)';
    }), 'Yellow icons must use the dark foreground from the design.');
    I.clickCss(`${modal} .md-dashboard__shortcut-colors input[value="cyan"] + span`);
    I.clickCss(`${modal} .modal-footer .btn-primary`);
    I.waitForInvisible(modal, 10);
    saved(I);
    I.refreshPage();
    loaded(I);
    const shortcut = await I.executeScript(title => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.options.title === title), title);
    I.assertEqual(shortcut.options.icon, 'ti-heart');
    I.assertEqual(shortcut.options.color, 'cyan');
    I.seeElement(`[data-instance-id="${shortcut.id}"] a[href="https://example.com/autotest"] .ti-heart`);
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.clickCss(`[data-instance-id="${shortcut.id}"] .md-dashboard-widget__shortcut`);
    I.waitForVisible(modal, 10);
    I.seeInField(`${modal} [name="dashboardShortcutIcon"]`, 'heart');
    I.seeCheckboxIsChecked(`${modal} input[value="cyan"]`);
    I.clickCss(`${modal} .btn-close`);
    I.waitForInvisible(modal, 10);
});

/** Checks keyboard cancellation, pointer insertion and persistence without changing other widgets. */
Scenario('Shortcut grips support keyboard lift and pointer drop across the strip', async ({ I }) => {
    const ids = await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__shortcut-list [data-instance-id]')].map(card => card.dataset.instanceId));
    I.assertTrue(ids.length >= 2, 'Earlier shortcut scenarios must provide two movable links.');
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    const grip = `[data-instance-id="${ids[0]}"] .md-dashboard__drag`;
    I.executeScript(selector => document.querySelector(selector).focus(), grip);
    I.pressKey('Space');
    I.pressKey('ArrowRight');
    I.seeElement('.md-dashboard__shortcut-drag-helper');
    I.seeElement('.md-dashboard__shortcut-drop-marker');
    I.assertEqual(await I.executeScript(() => getComputedStyle(document.querySelector('.md-dashboard__shortcut-drag-helper')).backgroundColor), 'rgb(255, 255, 255)');
    I.saveScreenshot('dashboard-shortcut-keyboard-drag.png', true);
    I.pressKey('Escape');
    I.dontSeeElement('.md-dashboard__shortcut-drag-helper');
    I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__shortcut-list [data-instance-id]')].map(card => card.dataset.instanceId)), ids);
    I.pressKey('Space');
    I.pressKey('ArrowRight');
    I.pressKey('Enter');
    saved(I);
    I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__shortcut-list [data-instance-id]')].map(card => card.dataset.instanceId)), [ids[1], ids[0], ...ids.slice(2)]);
    I.waitForFunction(() => [...document.querySelectorAll('.md-dashboard__shortcut-list [data-instance-id]')].every(card => card.getAnimations().length === 0), 5);
    // jQuery UI needs intermediate pointer moves; keep the pointer down to inspect its helper and marker.
    await I.usePlaywrightTo('drag the second shortcut before the first', async ({ page }) => {
        const source = await page.locator(grip).boundingBox();
        const target = await page.locator(`[data-instance-id="${ids[1]}"]`).boundingBox();
        await page.mouse.move(source.x + source.width / 2, source.y + source.height / 2);
        await page.mouse.down();
        await page.mouse.move(source.x + source.width / 2 - 12, source.y + source.height / 2, { steps: 2 });
        await page.mouse.move(target.x + 4, target.y + target.height / 2, { steps: 10 });
    });
    I.seeElement('.is-shortcut-placeholder');
    I.seeElement('.md-dashboard__shortcut-drop-marker');
    I.assertTrue(await I.executeScript(() => {
        const helper = document.querySelector('.md-dashboard__shortcut-drag-helper');
        const marker = document.querySelector('.md-dashboard__shortcut-drop-marker');
        return getComputedStyle(helper).boxShadow !== 'none' && getComputedStyle(marker).backgroundColor === 'rgb(255, 255, 255)' && marker.getBoundingClientRect().right <= helper.getBoundingClientRect().left;
    }), 'The raised shortcut must leave the white insertion marker visible.');
    I.saveScreenshot('dashboard-shortcut-pointer-drag.png', true);
    await I.usePlaywrightTo('drop the shortcut at its insertion marker', async ({ page }) => { await page.mouse.up(); });
    saved(I);
    I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__shortcut-list [data-instance-id]')].map(card => card.dataset.instanceId)), ids);
    I.refreshPage();
    loaded(I);
    I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__shortcut-list [data-instance-id]')].map(card => card.dataset.instanceId)), ids);
});

/** Edits a shortcut through its label, undoes the save, then removes it from the same modal. */
Scenario('Shortcut labels open settings in edit mode and both save and modal removal offer undo', async ({ I }) => {
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    const previous = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.type === 'shortcut'));
    const target = `[data-instance-id="${previous.id}"] .md-dashboard-widget__shortcut`;
    const pageUrl = await I.grabCurrentUrl();
    I.clickCss(target);
    I.waitForVisible(modal, 10);
    I.assertEqual(await I.grabCurrentUrl(), pageUrl, 'An editing shortcut must not navigate.');
    I.see('Upraviť skratku', `${modal} .modal-title`);
    I.see('Odstrániť skratku', `${modal} .modal-footer`);
    I.fillField(`${modal} [name="dashboardShortcutTitle"]`, 'shortcut-edited-autotest');
    I.clickCss(`${modal} .md-dashboard__shortcut-colors input[value="red"] + span`);
    I.resizeWindow(1448, 1231);
    I.saveScreenshot('dashboard-shortcut-edit-dialog.png', true);
    I.wjSetDefaultWindowSize();
    I.click('Uložiť zmeny', `${modal} .modal-footer`);
    I.waitForInvisible(modal, 10);
    saved(I);
    I.see('shortcut-edited-autotest', target);
    I.clickCss('[data-dashboard-shortcut-undo]');
    saved(I);
    I.assertDeepEqual(await I.executeScript(id => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.id === id), previous.id), previous);
    I.clickCss(target);
    I.waitForVisible(modal, 10);
    I.click('Odstrániť skratku', `${modal} .modal-footer`);
    I.waitForInvisible(modal, 10);
    saved(I);
    I.dontSeeElement(`[data-instance-id="${previous.id}"]`);
    I.see('Späť', '.md-dashboard__shortcut-toast');
    I.saveScreenshot('dashboard-shortcut-remove-undo.png', true);
    I.clickCss('[data-dashboard-shortcut-undo]');
    saved(I);
    I.seeElement(target);
});

/** Checks the two autocomplete result groups, the full-title tooltip and unavailable-link edit access. */
Scenario('Shortcut search includes pages and long or unavailable destinations retain accessible states', async ({ I }) => {
    I.resizeWindow(1448, 1231);
    await I.mockRoute('**/_doc_autocomplete.jsp?*', route => route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify([{ doc_id: 42, title: 'Kontaktný formulár autotest', label: '/kontakt/formular' }])
    }));
    I.click('Pridať skratku', actions);
    I.waitForVisible(modal, 10);
    searchShortcut(I, 'formul');
    I.waitForText('Kontaktný formulár autotest', 10, modal);
    I.see('Sekcie administrácie', modal);
    I.see('Webové stránky', modal);
    I.see('Použiť vlastnú adresu URL', modal);
    I.saveScreenshot('dashboard-shortcut-add-search.png', true);
    chooseShortcut(I, 'Kontaktný formulár autotest');
    I.seeInField(modal + ' [name="dashboardShortcutSearch"]', 'Kontaktný formulár autotest');
    I.see('Kontaktný formulár autotest', modal + ' .md-dashboard__shortcut-preview');
    I.click('Zrušiť', modal + ' .modal-footer');
    I.waitForInvisible(modal, 10);
    await I.stopMockingRoute('**/_doc_autocomplete.jsp?*');
    I.wjSetDefaultWindowSize();

    const id = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.type === 'shortcut' && item.options.source !== 'url').id);
    const target = '[data-instance-id="' + id + '"] .md-dashboard-widget__shortcut';
    I.clickCss(actions + ' button[aria-pressed="false"]');
    I.clickCss(target);
    I.waitForVisible(modal, 10);
    const fullTitle = 'Registrácie na jesennú konferenciu 2026 autotest s dlhým názvom';
    I.fillField(modal + ' [name="dashboardShortcutTitle"]', fullTitle);
    I.click('Uložiť zmeny', modal + ' .modal-footer');
    I.waitForInvisible(modal, 10);
    saved(I);
    I.click('Hotovo', '.md-dashboard__welcome-heading');
    I.assertTrue(await I.executeScript(id => {
        const card = document.querySelector('[data-instance-id="' + id + '"]');
        const label = card.querySelector('.md-dashboard-widget__shortcut-label');
        return card.getBoundingClientRect().width <= 240 && card.getBoundingClientRect().height === 32 && label.scrollWidth > label.clientWidth;
    }, id), 'A long shortcut must stay on one line within 240 × 32 pixels.');
    I.executeScript(selector => document.querySelector(selector).focus(), target);
    I.waitForText(fullTitle, 5, '.tooltip');
    I.saveScreenshot('dashboard-shortcut-long-title.png', true);
    I.pressKey('Escape');
    I.waitForInvisible('.tooltip', 5);
    I.clickCss('[data-dashboard-shortcut-undo]');
    saved(I);

    I.executeScript(id => {
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        controller.context.data.dashboardMenu = [];
        return controller.refresh(id);
    }, id);
    I.waitForVisible(target + '[aria-disabled="true"]', 5);
    I.assertEqual(await I.grabAttributeFrom(target, 'href'), null);
    I.executeScript(selector => document.querySelector(selector).focus(), target);
    I.waitForText('Cieľ nie je dostupný', 5, '.tooltip');
    I.clickCss(actions + ' button[aria-pressed="false"]');
    I.clickCss(target);
    I.waitForVisible(modal, 10);
    I.see('Odstrániť skratku', modal);
    I.dontSeeElement('.tooltip');
    I.click('Zrušiť', modal + ' .modal-footer');
    I.waitForInvisible(modal, 10);
    I.refreshPage();
    loaded(I);
});

/**
 * Checks that old browser bookmarks are imported once as personal shortcuts without duplicates. A failed
 * save preserves both the original bookmarks and layout; a successful retry retains other widgets and
 * survives reloading.
 */
Scenario('Automatically import old bookmarks on load with failure recovery and server persistence', async ({ I }) => {
    I.assertTrue(Boolean(originalSettings), 'The fixture must preserve the original profile first.');
    const before = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    const legacy = JSON.stringify([
        { name: 'Banner autotest', path: bannerHref },
        { name: 'Banner duplicate autotest', path: 'http://iwcm.interway.sk' + bannerHref },
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
 * Checks that removing shortcuts leaves widget settings and release-news choices intact. After the user
 * removes every shortcut, undo can restore the last one and an empty section survives reloading.
 */
Scenario('Removing every shortcut preserves widgets and the empty section survives reload', async ({ I }) => {
    const before = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    I.clickCss(`${actions} button[aria-pressed="false"]`);
    I.dontSee('Obnoviť', actions);
    for (const item of before.items.filter(item => item.type === 'shortcut')) {
        I.clickCss(`[data-instance-id="${item.id}"] .md-dashboard__shortcut-remove`);
        saved(I);
        I.waitForInvisible(`[data-instance-id="${item.id}"]`, 10);
    }
    I.see('Žiadna skratka', links);
    const after = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    I.assertDeepEqual(after.items.filter(item => item.type !== 'shortcut'), before.items.filter(item => item.type !== 'shortcut'));
    I.assertEqual(after.acknowledgedNewsVersion, before.acknowledgedNewsVersion);
    for (const item of before.items.filter(item => item.type !== 'shortcut')) I.assertDeepEqual(after.domainOptions[item.id], before.domainOptions[item.id]);
    I.clickCss('[data-dashboard-shortcut-undo]');
    saved(I);
    I.seeNumberOfElements(`${links} [data-instance-id]`, 1);
    I.clickCss(`${links} .md-dashboard__shortcut-remove`);
    saved(I);
    I.waitForInvisible('.md-dashboard__shortcut-toast', 10);
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
