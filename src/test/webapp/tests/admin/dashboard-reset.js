const assert = require('node:assert/strict');

Feature('admin.dashboard-reset').tag('@singlethread');

let fixtureLogin = process.env.DASHBOARD_RESET_FIXTURE;
let originalAdminPreferences;
const confirmation = '.md-dashboard-modal button[aria-describedby^="dashboard-reset-"]';
const expectedSizes = {
    search: 'fullauto', 'recent-pages': '3x3', forms: '1x1', sessions: '2x3', publishing: '2x3',
    'search-terms': '2x3', traffic: '3x3', referrers: '3x3', 'top-pages': '3x3', newsletter: '2x2',
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
    return profile.items.map(({ type, size, collapsed, options, id }) => ({ type, size, collapsed, options, domainOptions: profile.domainOptions[id] }));
}

async function assertFixtureIdentity(I) {
    assert.equal(await I.executeScript(() => window.currentUser?.login), fixtureLogin,
        'A destructive reset must only run in the disposable test account');
}

async function openReset(I) {
    await I.clickIfVisible('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    I.clickCss('.md-dashboard__toolbar-actions > .md-dashboard__edit-control');
    I.waitForVisible('.md-dashboard__reset', 10);
    I.dontSeeElement(confirmation);
    I.clickCss('.md-dashboard__reset');
    I.waitForVisible(confirmation, 10);
    I.seeElement('.md-dashboard__reset[aria-expanded="true"]');
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    waitForDashboard(I);
});

Scenario('Create a disposable dashboard reset account', async ({ I, DT, DTE }) => {
    originalAdminPreferences = await I.executeScript(() => Object.fromEntries(Object.entries(window.currentUser.adminSettings).filter(([key]) => key.startsWith('overview.'))));
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
        const ids = ['menuWebpages', 'cmp_form', 'cmp_stat', 'menuEmail'].map(key => `perms_${key}`);
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

Scenario('Reset confirms deletion, keeps failed changes and restores every available default', async ({ I }) => {
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
        await I.mockRoute('**/admin/rest/dashboard/settings', route => route.request().method() === 'DELETE'
            ? route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"autotest reset failure"}' })
            : route.continue());
        await openReset(I);
        assert.deepEqual(await settings(I), custom, 'Opening confirmation must not change the profile');
        I.clickCss(confirmation);
        waitForSave(I);
        I.waitForVisible('.md-dashboard-modal .modal-footer .text-danger', 10);
        assert.deepEqual(await settings(I), custom, 'A failed DELETE must preserve layout, filters and acknowledged news');
        I.seeElement('.md-dashboard-modal');
        await I.stopMockingRoute('**/admin/rest/dashboard/settings');
        await assertFixtureIdentity(I);
        I.clickCss(confirmation);
        waitForSave(I);
        I.waitForInvisible('.md-dashboard-modal', 10);
        const defaults = await settings(I);
        assert.equal(defaults.configured, false, 'Reset defaults remain unconfigured until a personal edit');
        assert.equal(defaults.acknowledgedNewsVersion, null);
        assert.deepEqual([...new Set(defaults.items.map(item => item.type))].sort(), Object.keys(expectedSizes).sort());
        for (const item of defaults.items) {
            assert.equal(item.size, expectedSizes[item.type], `Default footprint for ${item.type}`);
            assert.equal(item.collapsed, false);
        }
        assert.deepEqual(defaults.items.slice(0, 11).map(item => item.type), [
            'search', 'sessions', 'news', 'traffic', 'forms', 'approvals',
            'errors', 'recent-pages', 'referrers', 'publishing', 'newsletter'
        ], 'The overview must retain its curated default order');
        assert.equal(defaults.items.filter(item => item.type === 'sessions').length, 1);
        assert.equal(defaults.domainOptions[defaults.items.find(item => item.type === 'forms').id].formName, '');
        I.refreshPage();
        waitForDashboard(I);
        const reloaded = await settings(I);
        assert.equal(reloaded.configured, false);
        assert.deepEqual(effectiveDefaults(reloaded), effectiveDefaults(defaults), 'Reload must regenerate the same defaults without depending on instance ids');
        I.waitForFunction(() => [...document.querySelectorAll('.md-dashboard__widget-body')].every(body => body.getAttribute('aria-busy') !== 'true'), 30);
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
        await I.clickIfVisible('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
        I.clickCss(`[data-instance-id="${shortcut.id}"] .dropdown > button`);
        I.forceClick(`[data-instance-id="${shortcut.id}"] [data-dashboard-action="settings"]`);
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
            assert.equal(status, 200, 'Remove every dashboard record before deleting the disposable account');
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
    const preserved = await I.executeScript(() => Object.fromEntries(Object.entries(window.currentUser.adminSettings).filter(([key]) => key.startsWith('overview.'))));
    if (originalAdminPreferences) assert.deepEqual(preserved, originalAdminPreferences, 'The existing administrator and all its domain preferences must remain untouched');
});
