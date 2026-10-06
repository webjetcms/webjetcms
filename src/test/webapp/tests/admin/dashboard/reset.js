const { waitForWidgets, showWidget, readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

const assert = require('node:assert/strict');

Feature('admin.dashboard.reset').tag('@singlethread');

let fixtureLogin = process.env.DASHBOARD_RESET_FIXTURE;
let originalAdminPreferences;
const resetDialog = '#toast-container-webjet .toast[role="dialog"]';
const confirmation = `${resetDialog} button[id^="confirmationYes"]`;
const expectedSizes = {
    search: 'fullauto', 'recent-pages': '3x2', forms: '1x1', sessions: '2x3', publishing: '2x2',
    traffic: '3x3', referrers: '2x2', newsletter: '2x2',
    news: '3x2', approvals: '1x1', errors: '1x1', shortcut: '1x1'
};

function waitForDashboard(I) {
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
}

function waitForSave(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

async function settings(I) {
    return I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
}

function effectiveDefaults(profile) {
    return profile.items.map(({ type, size, options, id }) => ({ type, size, options, domainOptions: profile.domainOptions[id] || {} }));
}

function saveOverview(I) {
    I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="true"]');
    waitForSave(I);
}

async function assertFixtureIdentity(I) {
    assert.equal(await I.executeScript(() => window.currentUser?.login), fixtureLogin,
        'A destructive reset must only run in the disposable test account');
}

async function openReset(I, allSizes = false) {
    I.executeScript(() => {
        const toolbar = document.querySelector('.md-dashboard__toolbar');
        window.scrollbarMain.setMomentum(0, 0);
        window.scrollbarMain.setPosition(0, window.scrollbarMain.offset.y + toolbar.getBoundingClientRect().top - 64);
    });
    await I.clickIfVisible('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    I.waitForVisible('.md-dashboard__toolbar-actions .md-dashboard__reset', 10);
    I.assertTrue(await I.executeScript(() => {
        const reset = document.querySelector('.md-dashboard__toolbar-actions .md-dashboard__reset');
        return reset.nextElementSibling.matches('.md-dashboard__edit-control:not(.md-dashboard__reset):not(.md-dashboard__cancel)')
            && reset.nextElementSibling.nextElementSibling.matches('.md-dashboard__cancel')
            && reset.nextElementSibling.nextElementSibling.nextElementSibling.matches('button[aria-pressed="true"]')
            && (reset.getAttribute('title') || reset.getAttribute('data-bs-original-title')) === WJ.translate('admin.dashboard.resetTooltip.js');
    }), 'The explained Reset action must precede Add widget, Cancel and Save.');
    I.dontSeeElement(confirmation);
    if (allSizes) I.pressKeyDown('Shift');
    I.clickCss('.md-dashboard__toolbar-actions .md-dashboard__reset');
    if (allSizes) I.pressKeyUp('Shift');
    I.waitForVisible(confirmation, 10);
    I.seeElement(`${resetDialog}[aria-modal="true"][aria-describedby]`);
    I.seeElement(`${resetDialog} button[id^="confirmationNo"]`);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    waitForDashboard(I);
});

/**
 * Creates a separate temporary account with the permissions needed for dashboard checks. Reset tests use
 * this account so the existing administrator layout and domain preferences remain untouched.
 */
Scenario('Create a disposable dashboard reset account', async ({ I, DT, DTE }) => {
    originalAdminPreferences = (await I.executeScript(readDashboardBootstrap)).settings;
    fixtureLogin = `dashboard-reset-autotest-${I.getRandomTextShort()}`;
    I.amOnPage('/admin/v9/users/user-list/');
    DT.waitForLoader();
    I.clickCss('button.buttons-create');
    DTE.waitForEditor();
    I.fillField('#DTE_Field_editorFields-login', fixtureLogin);
    I.fillField('#DTE_Field_firstName', 'Dashboard');
    I.fillField('#DTE_Field_lastName', fixtureLogin);
    I.fillField('#DTE_Field_email', `${fixtureLogin}@example.invalid`);
    I.seeInField('#DTE_Field_editorFields-login', fixtureLogin);
    I.fillField('#DTE_Field_password', secret(I.getDefaultPassword()));
    I.checkOption('#DTE_Field_authorized_0');
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
    I.checkOption('#DTE_Field_admin_0');
    I.waitForElement('#DTE_Field_editorFields-enabledItems .jstree-node', 10);
    // Select exact module permissions through the permission tree API, without granting user administration or adding mail-enabled user groups.
    const selected = await I.executeScript(() => {
        const tree = window.jQuery('#DTE_Field_editorFields-enabledItems').jstree(true);
        const ids = ['menuWebpages', 'cmp_form', 'cmp_stat', 'menuEmail', 'cmp_adminlog', 'welcomeShowLoggedAdmins', 'cmp_server_monitoring'].map(key => `perms_${key}`);
        tree.deselect_all();
        ids.forEach(id => { if (!tree.get_node(id)) throw new Error(`Missing fixture permission: ${id}`); tree.select_node(id); });
        return ids.every(id => tree.is_selected(id));
    });
    assert.equal(selected, true);
    DTE.save();
    DTE.waitForModalClose();
    DT.filterEquals('lastName', fixtureLogin);
    const fixtureRecord = await I.executeScript(() => window.usersDatatable.rows().data().toArray().map(({ id, login, lastName }) => ({ id, login, lastName })));
    assert.deepEqual(fixtureRecord.map(record => record.login), [fixtureLogin]);
    I.see(fixtureLogin, '#datatableInit tbody');
    I.seeNumberOfElements('#datatableInit tbody tr', 1);
});

/**
 * Checks that reset asks for confirmation and keeps both the saved layout and draft when saving fails. A successful
 * reset restores the standard widgets while preserving personal shortcuts, and later personal edits still
 * survive reloading.
 */
Scenario('Reset confirms changes, retains a failed draft and saves only the curated defaults', async ({ I }) => {
    assert.ok(fixtureLogin, 'The disposable account setup must run first');
    await session('dashboard reset autotest', async () => {
        I.amOnPage('/admin/logon/');
        I.relogin(fixtureLogin, false);
        I.amOnPage('/admin/v9/');
        waitForDashboard(I);
        await assertFixtureIdentity(I);
        const saved = await I.executeScript(async () => {
            const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
            const next = JSON.parse(JSON.stringify(controller.settings));
            next.items = next.items.filter(item => ['sessions', 'shortcut', 'forms'].includes(item.type));
            next.items.find(item => item.type === 'shortcut').options.title = 'dashboard-reset-autotest custom';
            next.domainOptions = { [next.items.find(item => item.type === 'forms').id]: { formName: 'dashboard-reset-autotest filter' } };
            next.acknowledgedNewsVersion = 'dashboard-reset-autotest acknowledged';
            return controller._commit(next);
        });
        assert.equal(saved, true, 'The isolated custom profile must be persisted');
        const custom = await settings(I);
        await I.mockRoute('**/admin/rest/dashboard/settings/reset', route => route.request().method() === 'PUT'
            ? route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"autotest reset failure"}' })
            : route.continue());
        await openReset(I);
        assert.deepEqual(await settings(I), custom, 'Opening confirmation must not change the profile');
        I.clickCss(confirmation);
        I.waitForInvisible(resetDialog, 10);
        const draft = await settings(I);
        assert.deepEqual((await I.executeScript(readDashboardBootstrap)).settings, custom, 'Confirming reset must leave the saved profile unchanged until Save');
        saveOverview(I);
        I.waitForVisible('.md-dashboard__status .text-danger', 10);
        assert.deepEqual(await settings(I), draft, 'A failed save must retain the reset draft for retry');
        assert.deepEqual((await I.executeScript(readDashboardBootstrap)).settings, custom, 'A failed save must preserve the stored layout, filters and acknowledged news');
        await I.stopMockingRoute('**/admin/rest/dashboard/settings/reset');
        await assertFixtureIdentity(I);
        saveOverview(I);
        I.waitForElement('.md-dashboard:not(.is-editing)', 10);
        I.waitForText(await I.executeScript(() => WJ.translate('admin.dashboard.overviewSaved.js')), 10, '#toast-container-webjet .toast-success');
        I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__status').textContent), '', 'Successful reset must use the standard notification instead of persistent inline text.');
        const defaults = await settings(I);
        assert.equal(defaults.configured, true, 'Saving the reset confirms the chosen default layout');
        assert.equal(defaults.acknowledgedNewsVersion, null);
        assert.deepEqual(defaults.items.filter(item => item.type === 'shortcut'), custom.items.filter(item => item.type === 'shortcut'), 'Overview reset preserves custom shortcuts, order and stable ids');
        assert.equal(defaults.shortcutsConfigured, true);
        assert.deepEqual([...new Set(defaults.items.map(item => item.type))].sort(), Object.keys(expectedSizes).sort());
        for (const item of defaults.items) {
            assert.equal(item.size, expectedSizes[item.type], `Default footprint for ${item.type}`);
        }
        assert.deepEqual(defaults.items.filter(item => item.type !== 'shortcut').map(item => item.type), [
            'search', 'sessions', 'news', 'traffic', 'forms', 'approvals',
            'errors', 'recent-pages', 'referrers', 'publishing', 'newsletter'
        ], 'The overview must retain its curated default order');
        assert.equal(defaults.items.filter(item => item.type === 'sessions').length, 1);
        I.assertEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__grid .md-dashboard__widget')].at(-1)?.dataset.widgetType),
            'newsletter', 'The reset grid must end with Newsletter');
        I.dontSeeElementInDOM('.md-dashboard__legacy');
        assert.equal(defaults.domainOptions[defaults.items.find(item => item.type === 'forms').id].formName, '');
        I.refreshPage();
        waitForDashboard(I);
        const reloaded = await settings(I);
        assert.equal(reloaded.configured, true);
        assert.deepEqual(effectiveDefaults(reloaded), effectiveDefaults(defaults), 'Reload must retain the saved defaults');
        await waitForWidgets(I);
        I.resizeWindow(1337, 1052);
        I.saveScreenshot('dashboard-default-desktop.png', true);
        I.executeScript(() => window.scrollbarMain.scrollIntoView(document.querySelector('[data-widget-type="traffic"]')));
        I.waitForFunction(() => {
            const bounds = document.querySelector('[data-widget-type="traffic"]').getBoundingClientRect();
            return bounds.top >= 0 && bounds.bottom <= window.innerHeight;
        }, 10);
        I.saveScreenshot('dashboard-default-charts.png', true);
        I.resizeWindow(390, 1052);
        if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
        I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
        I.executeScript(() => window.scrollbarMain.scrollTo(0, 0));
        I.saveScreenshot('dashboard-default-mobile.png', true);
        I.wjSetDefaultWindowSize();

        const shortcut = reloaded.items.find(item => item.type === 'shortcut');
        await showWidget(I, shortcut.id);
        I.clickCss('.md-dashboard__shortcut-actions button[aria-pressed="false"]');
        I.waitForElement('.md-dashboard.is-editing-shortcuts', 10);
        I.clickCss(`[data-instance-id="${shortcut.id}"] .md-dashboard-widget__shortcut`);
        I.waitForVisible('.md-dashboard__settings [name="dashboardShortcutTitle"]', 10);
        I.fillField('.md-dashboard__settings [name="dashboardShortcutTitle"]', 'dashboard-reset-autotest personalized');
        I.clickCss('.md-dashboard-modal .modal-footer .btn-primary');
        I.waitForInvisible('.md-dashboard-modal', 10);
        waitForSave(I);
        I.refreshPage();
        waitForDashboard(I);
        const personalized = await settings(I);
        assert.equal(personalized.configured, true);
        assert.equal(personalized.items.find(item => item.id === shortcut.id)?.options.title, 'dashboard-reset-autotest personalized');
        assert.deepEqual(personalized.items.map(item => [item.type, item.size]), reloaded.items.map(item => [item.type, item.size]));
        I.logout();
    });
});

/**
 * Checks that holding Shift during reset saves every supported widget size while retaining shortcuts. Each
 * size variant can be removed and restored independently, and a normal reset returns to the standard
 * selection.
 */
Scenario('Shift reset persists every size and allows individual variants to be removed and restored', async ({ I }) => {
    assert.ok(fixtureLogin, 'The disposable account setup must run first');
    await session('dashboard reset autotest', async () => {
        I.amOnPage('/admin/logon/');
        I.relogin(fixtureLogin, false);
        I.amOnPage('/admin/v9/');
        waitForDashboard(I);
        await assertFixtureIdentity(I);
        const original = await settings(I);
        await openReset(I, true);
        I.see(await I.executeScript(() => WJ.translate('admin.dashboard.resetAllConfirm.js')), resetDialog);
        I.clickCss(confirmation);
        I.waitForInvisible(resetDialog, 10);
        saveOverview(I);
        I.waitForElement('.md-dashboard:not(.is-editing)', 10);
        const generated = await settings(I);
        const variants = {
            'recent-pages': ['2x3', '3x2', '3x3'], approvals: ['1x1', '3x3'], publishing: ['2x2', '2x3'],
            forms: ['1x1', '3x3'], traffic: ['1x1', '3x3'], 'top-pages': ['2x3', '3x3'],
            'search-terms': ['2x3', '3x3'], referrers: ['2x2', '2x3', '3x3'], newsletter: ['2x2', '3x3'],
            errors: ['1x1', '3x3'], sessions: ['2x3'], news: ['3x2'], search: ['fullauto'],
            'changed-pages': ['3x2', '3x3'], audit: ['3x2', '3x3'], 'logged-admins': ['2x2', '2x3'],
            'server-memory': ['3x2', '3x3'], 'server-cpu': ['3x2', '3x3']
        };
        for (const [type, sizes] of Object.entries(variants)) {
            assert.deepEqual(generated.items.filter(item => item.type === type).map(item => item.size).sort(), sizes.slice().sort(), `${type} must include every supported size exactly once`);
        }
        assert.equal(generated.configured, true);
        assert.equal(new Set(generated.items.map(item => item.id)).size, generated.items.length);
        assert.deepEqual(generated.items.filter(item => item.type === 'shortcut'), original.items.filter(item => item.type === 'shortcut'));
        I.refreshPage();
        waitForDashboard(I);
        assert.deepEqual(await settings(I), generated, 'Generated variants must survive reloading before any personal edit');
        await waitForWidgets(I);
        I.executeScript(() => {
            const toolbar = document.querySelector('.md-dashboard__toolbar');
            window.scrollbarMain.setPosition(0, window.scrollbarMain.offset.y + toolbar.getBoundingClientRect().top - 64);
        });
        I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
        I.resizeWindow(1337, 1052);
        const preview = generated.items.find(item => item.type === 'recent-pages' && item.size === '3x3');
        await showWidget(I, preview.id);
        I.saveScreenshot('dashboard-all-sizes-desktop.png', true);
        I.resizeWindow(390, 1052);
        await showWidget(I, preview.id);
        I.saveScreenshot('dashboard-all-sizes-mobile.png', true);
        I.wjSetDefaultWindowSize();
        I.executeScript(() => window.scrollTo(0, 0));
        await showWidget(I, preview.id);
        I.waitForVisible(`[data-instance-id="${preview.id}"] .dropdown > button`, 10);
        I.clickCss(`[data-instance-id="${preview.id}"] .dropdown > button`);
        I.clickCss(`[data-instance-id="${preview.id}"] [data-dashboard-action="remove"]`);
        waitForSave(I);
        assert.equal((await settings(I)).items.some(item => item.id === preview.id), false);
        I.waitForVisible('[data-dashboard-widget-undo]', 10);
        I.clickCss('[data-dashboard-widget-undo]');
        waitForSave(I);
        assert.deepEqual((await settings(I)).items, generated.items, 'Undo must restore one variant while other instances of its type remain');
        await openReset(I);
        I.clickCss(confirmation);
        I.waitForInvisible(resetDialog, 10);
        saveOverview(I);
        I.waitForElement('.md-dashboard:not(.is-editing)', 10);
        const defaults = await settings(I);
        assert.equal(defaults.items.filter(item => item.type !== 'shortcut').at(-1).type, 'newsletter');
        assert.equal(defaults.items.filter(item => item.type === 'recent-pages').length, 1);
        I.logout();
    });
});

/**
 * Clears the temporary account's dashboard, deletes that account and confirms that the original administrator
 * preferences have not changed.
 */
Scenario('Remove disposable reset preferences and account without changing the administrator', async ({ I, DT, DTE }) => {
    if (!fixtureLogin) return;
    assert.match(fixtureLogin, /^dashboard-reset-autotest-[A-Za-z0-9-]+$/, 'Cleanup only accepts explicitly marked disposable dashboard accounts');
    I.amOnPage('/admin/v9/users/user-list/');
    DT.waitForLoader();
    DT.filterEquals('lastName', fixtureLogin);
    const records = await I.executeScript(() => window.usersDatatable.rows().data().toArray().map(({ id, login, lastName }) => ({ id, login, lastName })));
    const exists = records.some(record => record.lastName === fixtureLogin);
    if (exists) {
        assert.equal(records.length, 1, 'Cleanup must select only the uniquely named disposable fixture');
        // Resolve the exact saved login even if an unrelated editor autofill issue interrupted account setup.
        fixtureLogin = records[0].login;
        await session('dashboard reset cleanup autotest', async () => {
            I.amOnPage('/admin/logon/');
            I.relogin(fixtureLogin, false);
            I.amOnPage('/admin/v9/');
            waitForDashboard(I);
            await assertFixtureIdentity(I);
            const status = await I.executeScript(async () => (await fetch('/admin/rest/dashboard/settings', {
                method: 'DELETE', credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken }
            })).status);
            assert.equal(status, 200, 'Reset widget preferences before deleting the disposable account');
            I.logout();
        });
        I.seeNumberOfElements('#datatableInit tbody tr', 1);
        DT.deleteAll();
        DTE.waitForModalClose();
        DT.waitForLoader();
        I.dontSee(fixtureLogin, '#datatableInit tbody');
    }
    I.amOnPage('/admin/v9/');
    waitForDashboard(I);
    const preserved = (await I.executeScript(readDashboardBootstrap)).settings;
    if (originalAdminPreferences) assert.deepEqual(preserved, originalAdminPreferences, 'The existing administrator layout and active-domain preferences must remain untouched');
});
