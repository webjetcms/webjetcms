Feature('webpages.mirroring-preview').tag('@singlethread');

const pageUrl = '/admin/v9/webpages/web-pages-list/';
const domain = 'mirroring.tau27.iway.sk';
const configName = 'structureMirroringConfig';
const groupModal = '#groups-datatable_modal';
const pageModal = '#datatableInit_modal';
const folders = {};
const roots = {};
let suffix, pageTitle, originalConfig, originalTitleSync, originalWarnings;

Before(async ({ I, login, Document }) => {
    login('admin');
    I.amOnPage(pageUrl);
    if ((await I.grabTextFrom('div.js-domain-toggler div.bootstrap-select button')).trim() !== domain) {
        Document.switchDomain(domain);
    }
    if (!suffix) suffix = I.getRandomText();
});

function node(folder) {
    return `#SomStromcek li[id="${folder.id}"]`;
}

function openFolder(I, DT, folder) {
    I.amOnPage(pageUrl + '?groupid=' + folder.id);
    I.waitForElement(node(folder) + ' > a.jstree-clicked', 30);
    DT.waitForLoader();
}

async function readConfiguration(I, DT, DTE, name) {
    I.amOnPage('/admin/v9/settings/configuration/');
    I.click('#SomStromcek li[data-configuration-view="all"] > a');
    DT.waitForLoader('configurationDatatable');
    DT.filterEquals('name', name);
    if (await I.grabNumberOfVisibleElements('[data-conf-value="current"]')) {
        return await I.grabTextFrom('[data-conf-value="current"]');
    }
    I.click(name, '#configurationDatatable tbody');
    DTE.waitForEditor('configurationDatatable');
    const value = await I.grabValueFrom('#DTE_Field_value');
    DTE.cancel();
    return value;
}

function showWarnings(I, enabled) {
    I.click('button.buttons-jstree-settings');
    I.waitForVisible('#jstreeSettingsModal', 10);
    if (enabled) I.checkOption('#jstree-settings-showbadmirrorings');
    else I.uncheckOption('#jstree-settings-showbadmirrorings');
    I.click('#jstree-settings-submit');
    I.waitForInvisible('#jstreeSettingsModal', 10);
    I.jstreeWaitForLoader();
}

function openPageDelete(I, DT, DTE, folder) {
    openFolder(I, DT, folder);
    DT.filterEquals('title', pageTitle);
    I.see(pageTitle, '#datatableInit tbody');
    I.click('#datatableInit_wrapper button.buttons-select-all');
    I.click(DT.btn.delete_button);
    DTE.waitForEditor();
}

function confirmDelete(I, DTE, modal) {
    I.waitForEnabled(modal + ' button.btn-primary', 20);
    DTE.save();
    I.waitForInvisible(modal, 30);
}

Scenario('Create own language folders and preserve settings', async ({ I, DT, DTE, Document }) => {
    originalConfig = await readConfiguration(I, DT, DTE, configName);
    originalTitleSync = await readConfiguration(I, DT, DTE, 'syncGroupAndWebpageTitle');
    Document.setConfigValue('syncGroupAndWebpageTitle', 'false', true);

    I.amOnPage(pageUrl);
    I.click('button.buttons-jstree-settings');
    I.waitForVisible('#jstreeSettingsModal', 10);
    originalWarnings = await I.grabCheckedElementStatus('#jstree-settings-showbadmirrorings');
    I.click('#jstreeSettingsModal .btn-close-editor');
    I.waitForInvisible('#jstreeSettingsModal', 10);

    for (const [key, language, parent] of [
        ['sk', 'Slovenský'], ['en', 'Anglický'], ['duplicate-sk', 'Slovenský'],
        ['container', 'Slovenský'], ['cz', 'Český', 'container'], ['plain', 'Slovenský']
    ]) {
        const name = 'mirror-preview-autotest-' + key + '-' + suffix;
        I.amOnPage(pageUrl + '?groupid=' + (parent ? roots[parent].id : 0));
        DT.waitForLoader();
        I.click(DT.btn.tree_add_button);
        DTE.waitForEditor('groups-datatable');
        I.fillField('#DTE_Field_groupName', name);
        if (!parent) I.groupSetRootParent();
        I.clickCss('#pills-dt-groups-datatable-template-tab');
        DTE.selectOption('lng', language);
        DTE.save();
        I.waitForText(name, 20, '#SomStromcek');
        I.jstreeClick(name);
        roots[key] = { name, id: await I.grabValueFrom('#tree-folder-id'), path: (parent ? roots[parent].path + '/' : '') + name };
        I.see(name, '#datatableInit tbody');
    }
    const mirroredKeys = ['sk', 'en', 'cz', 'duplicate-sk'];
    const mapping = mirroredKeys.map(key => roots[key].id).join(',') + ':autotest-' + suffix;
    Document.setConfigValue(configName, [originalConfig, mapping].filter(Boolean).join('\n'), true);

    // Creating a child under the temporary mapping links its mirrors and default pages through the normal save flow.
    pageTitle = 'mirror-preview-autotest-' + suffix;
    openFolder(I, DT, roots.sk);
    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor('groups-datatable');
    I.fillField('#DTE_Field_groupName', pageTitle);
    DTE.save();
    for (const key of mirroredKeys) {
        openFolder(I, DT, roots[key]);
        if (await I.grabNumberOfVisibleElements(node(roots[key]) + '.jstree-closed')) {
            I.click(node(roots[key]) + ' > .jstree-ocl');
            I.jstreeWaitForLoader();
        }
        I.click(locate(node(roots[key]) + ' > ul > li > a.jstree-anchor').withText(pageTitle));
        DT.waitForLoader();
        folders[key] = { name: pageTitle, id: await I.grabValueFrom('#tree-folder-id'), path: roots[key].path + '/' + pageTitle };
        I.see(pageTitle, '#datatableInit tbody');
    }
    // Give the source a unique folder name so tree search hides its mirrors.
    openFolder(I, DT, folders.sk);
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor('groups-datatable');
    folders.sk.name += '-source';
    folders.sk.path += '-source';
    I.fillField('#DTE_Field_groupName', folders.sk.name);
    DTE.save();
});

Scenario('Navigate to a mirror during search and return to the source', async ({ I, DT }) => {
    openFolder(I, DT, folders.sk);
    const link = node(folders.sk) + ` .jstree-mirror-link[data-group-id="${folders.en.id}"]`;
    I.see('EN', link);
    I.dontSeeElement(node(folders.sk) + ` .jstree-mirror-link[data-group-id="${folders.sk.id}"]`);
    I.jstreeFilter(folders.sk.name);
    I.waitForInvisible(node(folders.en), 20);
    I.click(link);
    I.waitForElement(node(folders.en) + ' > a.jstree-clicked', 30);
    I.assertEqual(await I.grabValueFrom('#tree-folder-search-input'), '');
    I.seeInField('#tree-folder-id', folders.en.id);
    I.seeElement(node(folders.sk) + '.jstree-mirror-peer');
    I.click('.buttons-mirror-back');
    I.waitForElement(node(folders.sk) + ' > a.jstree-clicked', 30);
    I.seeElement(node(folders.en) + '.jstree-mirror-peer');
    I.dontSeeElement('.buttons-mirror-back');
});

Scenario('Show mirrored page and folder paths in delete dialogs', ({ I, DT, DTE }) => {
    openPageDelete(I, DT, DTE, folders.sk);
    I.waitForText(folders.en.path, 20, pageModal + ' .delete-mirrors-info.alert-warning');
    I.dontSee(folders.sk.path, pageModal + ' .delete-mirrors-info');
    I.seeElement(pageModal + ' button.btn-primary:enabled');
    DTE.cancel();

    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor('groups-datatable');
    I.waitForText(folders.en.path, 20, groupModal + ' .delete-mirrors-info.alert-warning');
    I.dontSee(folders.sk.path, groupModal + ' .delete-mirrors-info');
    I.seeElement(groupModal + ' button.btn-primary:enabled');
    DTE.cancel();

    openFolder(I, DT, roots.plain);
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor('groups-datatable');
    I.waitForEnabled(groupModal + ' button.btn-primary', 20);
    I.dontSeeElement(groupModal + ' .delete-mirrors-info');
    DTE.cancel();
});

Scenario('Hide mirror links without the mirroring permission', ({ I }) => {
    I.amOnPage(pageUrl + '?groupid=' + folders.sk.id + '&removePerm=cmp_mirroring');
    I.waitForElement(node(folders.sk) + ' > a.jstree-clicked', 30);
    I.dontSeeElement(node(folders.sk) + ' .jstree-mirror-link');
    I.relogin('admin');
});

Scenario('Reject folder deletion without deleteDir while editing remains allowed', async ({ I, DT }) => {
    I.amOnPage(pageUrl + '?groupid=' + folders.sk.id + '&removePerm=deleteDir');
    I.waitForElement(node(folders.sk) + ' > a.jstree-clicked', 30);
    DT.waitForLoader();
    I.assertDeepEqual(await I.executeScript(() => [WJ.hasPermission('editDir'), WJ.hasPermission('deleteDir')]), [true, false]);
    const response = await I.executeScript(async id => {
        const folder = $('#SomStromcek').jstree(true).get_node(id).original.groupDetails;
        return fetch('/admin/rest/groups/editor', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: JSON.stringify({ action: 'remove', data: { [id]: folder } })
        }).then(result => result.json());
    }, folders.sk.id);
    I.assertContain(response.error || '', 'nemáte právo na zmazanie adresáru');
    for (const folder of Object.values(folders)) {
        openFolder(I, DT, folder);
        I.dontSeeElement('#pills-trash-tab.active');
        I.see(folder.name, node(folder) + ' > a');
    }
    I.relogin('admin');
});

Scenario('Show warning icons beside the affected folders and navigate overflow links', ({ I, DT, DTE }) => {
    openFolder(I, DT, folders.sk);
    showWarnings(I, true);
    openFolder(I, DT, folders.sk);
    I.waitForElement(node(folders.sk) + ' > a .ti-exclamation-circle.text-danger', 30);
    I.dontSeeElement(node(folders.sk) + ' > a .ti-alert-triangle');
    I.see('+1', node(folders.sk) + ' .jstree-mirror-more');
    I.click(node(folders.sk) + ' .jstree-mirror-more');
    I.click(node(folders.sk) + ` .jstree-mirror-link[data-group-id="${folders['duplicate-sk'].id}"]`);
    I.waitForElement(node(folders['duplicate-sk']) + ' > a.jstree-clicked', 30);

    // Resolving the duplicate language leaves only the different folder-depth warning.
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor('groups-datatable');
    I.clickCss('#pills-dt-groups-datatable-template-tab');
    DTE.selectOption('lng', 'Nemecký');
    DTE.save();
    openFolder(I, DT, folders.sk);
    I.waitForElement(node(folders.sk) + ' > a .ti-alert-triangle.text-warning', 30);
    I.dontSeeElement(node(folders.sk) + ' > a .ti-exclamation-circle.text-danger');
    I.see('CS', node(folders.sk) + ` .jstree-mirror-link[data-group-id="${folders.cz.id}"]`);

    showWarnings(I, false);
    openFolder(I, DT, folders.sk);
    I.dontSeeElement(node(folders.sk) + ' > a .ti-alert-triangle.text-warning');
    I.seeElement(node(folders.sk) + ' .jstree-mirror-link');
});

Scenario('Move the previewed pages and both selected mirrored folders to Trash', async ({ I, DT, DTE }) => {
    const mirrored = [folders.sk, folders.en, folders.cz, folders['duplicate-sk']];
    openPageDelete(I, DT, DTE, folders.sk);
    for (const folder of mirrored.slice(1)) I.waitForText(folder.path, 20, pageModal + ' .delete-mirrors-info');
    confirmDelete(I, DTE, pageModal);
    I.click('#pills-trash-tab');
    DT.waitForLoader();
    DT.filterEquals('title', pageTitle);
    I.seeNumberOfElements('#datatableInit tbody tr td.dt-row-edit', mirrored.length);

    openFolder(I, DT, folders.sk);
    if (await I.grabNumberOfVisibleElements(node(roots.en) + '.jstree-closed')) {
        I.click(node(roots.en) + ' > .jstree-ocl');
        I.jstreeWaitForLoader();
    }
    I.click(node(folders.en) + ' > a', null, { modifiers: ['Meta'] });
    DT.waitForLoader();
    I.seeElement(node(folders.sk) + ' > a.jstree-clicked');
    I.seeElement(node(folders.en) + ' > a.jstree-clicked');
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor('groups-datatable');
    for (const folder of mirrored.slice(2)) I.waitForText(folder.path, 20, groupModal + ' .delete-mirrors-info');
    confirmDelete(I, DTE, groupModal);
    for (const folder of mirrored) {
        openFolder(I, DT, folder);
        I.seeElement('#pills-trash-tab.active');
        I.see(folder.name, node(folder) + ' > a');
    }
    openFolder(I, DT, roots.plain);
    I.dontSeeElement('#pills-trash-tab.active');
    I.see(roots.plain.name, '#datatableInit tbody');
});

Scenario('Restore settings and remove own fixtures through the UI', async ({ I, DT, DTE, Document }) => {
    I.relogin('admin');
    I.amOnPage(pageUrl);
    Document.switchDomain(domain);
    if (originalConfig !== undefined) Document.setConfigValue(configName, originalConfig, true);
    if (originalTitleSync !== undefined) Document.setConfigValue('syncGroupAndWebpageTitle', originalTitleSync, true);
    I.amOnPage(pageUrl);
    if (originalWarnings !== undefined) showWarnings(I, originalWarnings);

    // Remove only our exact page titles from Trash, including pages detached from their folders.
    I.click('#pills-trash-tab');
    DT.waitForLoader();
    if (pageTitle) {
        DT.filterEquals('title', pageTitle);
        if (await I.grabNumberOfVisibleElements('#datatableInit tbody tr td.dt-row-edit')) {
            I.click('#datatableInit_wrapper button.buttons-select-all');
            I.click(DT.btn.delete_button);
            DTE.waitForEditor();
            confirmDelete(I, DTE, pageModal);
            I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 20, '#datatableInit tbody');
        }
    }
    // Delete the nested folder before its container; a second deletion removes live fixtures from Trash.
    for (const folder of [...Object.values(folders), roots.cz, roots.sk, roots.en, roots['duplicate-sk'], roots.plain, roots.container]) {
        if (!folder) continue;
        openFolder(I, DT, folder);
        if (!await I.grabNumberOfVisibleElements('#pills-trash-tab.active')) {
            I.click(DT.btn.tree_delete_button);
            DTE.waitForEditor('groups-datatable');
            confirmDelete(I, DTE, groupModal);
            openFolder(I, DT, folder);
        }
        I.seeElement('#pills-trash-tab.active');
        I.click(DT.btn.tree_delete_button);
        DTE.waitForEditor('groups-datatable');
        I.see(folder.name, groupModal);
        confirmDelete(I, DTE, groupModal);
        I.waitForInvisible(node(folder), 20);
    }
});

Scenario('Log out to reset the selected domain', ({ I }) => {
    I.logout();
});
