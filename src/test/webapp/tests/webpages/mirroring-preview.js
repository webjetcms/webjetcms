// Suite cleanup uses the browser after the final scenario, including filtered runs.
Feature('webpages.mirroring-preview').tag('@singlethread').config('Playwright', { restart: 'session' });

const WebjetDteJsTree = require('../../pages/WebjetDteJsTree');

const pageUrl = '/admin/v9/webpages/web-pages-list/';
const domain = process.env.CODECEPT_MIRRORING_DOMAIN || 'mirroring.tau27.iway.sk';
const configName = 'structureMirroringConfig';
const groupModal = '#groups-datatable_modal';
const pageModal = '#datatableInit_modal';
const folders = {};
const roots = {};
let suffix, pageTitle, originalConfig, originalTitleSync, originalMirroring, originalShowPages;
let cancelSyncUser;
let deletePermissionGroups;
let fixtureStarted = false, fixtureReady = false;
const editableGroups = '#editorAppDTE_Field_editorFields-editableGroups';
const enabledItems = '#DTE_Field_editorFields-enabledItems';
const clonePermission = enabledItems + ' li[id="perms_cmp_clone_structure"] > a';

Before(async ({ I, login, DT, DTE, Document }) => {
    login('admin');
    I.amOnPage(pageUrl);
    if ((await I.grabTextFrom('div.js-domain-toggler div.bootstrap-select button')).trim() !== domain) {
        Document.switchDomain(domain);
    }
    if (!fixtureStarted) {
        fixtureStarted = true;
        suffix = I.getRandomText();
        await prepareFixtures(I, DT, DTE, Document);
        fixtureReady = true;
    }
    I.assertTrue(fixtureReady, 'Mirroring fixture setup must complete before running a scenario');
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

function showMirroring(I, enabled) {
    I.click('button.buttons-jstree-settings');
    I.waitForVisible('#jstreeSettingsModal', 10);
    I.see('Zobraziť zrkadlenie', '#jstreeSettingsModal');
    if (enabled) I.checkOption('#jstree-settings-showmirrorings');
    else I.uncheckOption('#jstree-settings-showmirrorings');
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

async function requestHeaders(I) {
    const session = await I.grabCookie('JSESSIONID');
    return {
        'x-auth-token': '', Cookie: 'JSESSIONID=' + session.value,
        'X-CSRF-Token': await I.grabValueFrom('form[name="adminLogoffForm"] input[name="__token"]'),
        Referer: await I.grabCurrentUrl()
    };
}

async function cancelSync(I, rootGroupId) {
    // Invalid IDs and forbidden subtrees must be checked even when the UI cannot select them.
    return (await I.sendPostRequest('/apps/clone_structure/admin/cancel_sync?rootGroupId=' + rootGroupId, {}, await requestHeaders(I))).status;
}

async function syncMembers(I, DT, DTE, lookup) {
    I.amOnPage('/admin/v9/webpages/mirroring/');
    const result = [];
    for (const [index, type] of ['groups', 'docs'].entries()) {
        const table = type + 'MirroringTable';
        const wrapper = '#' + table + '_wrapper';
        I.clickCss('#pills-' + type + '-tab');
        DT.waitForLoader(table);
        const showId = Array.isArray(lookup) && !await I.grabNumberOfVisibleElements(wrapper + ' input.dt-filter-id');
        if (Array.isArray(lookup)) {
            DT.filterContains('fieldA', '', wrapper);
            if (showId) await DT.showColumn('Sync ID', table);
            DT.filterId('id', String(lookup[index].syncId), wrapper);
        } else DT.filterContains('fieldA', lookup, wrapper);
        I.seeNumberOfElements('#' + table + ' tbody tr[id]', 1);
        const syncId = Number(await I.grabAttributeFrom('#' + table + ' tbody tr[id]', 'id'));
        I.assertAbove(syncId, 0);
        I.clickCss('#' + table + ' tbody td.sorting_1');
        I.clickCss('#' + table + '_wrapper button.buttons-edit');
        DTE.waitForEditor(table);
        const members = (await I.grabValueFromAll('#' + table + '_modal input[id^="DTE_Field_selector"]')).map(Number).filter(id => id > 0).sort((a, b) => a - b);
        result.push({ syncId, members });
        DTE.cancel(table);
        if (showId) await DT.hideColumn('Sync ID', table);
    }
    return result;
}

async function prepareFixtures(I, DT, DTE, Document) {
    originalConfig = await readConfiguration(I, DT, DTE, configName);
    originalTitleSync = await readConfiguration(I, DT, DTE, 'syncGroupAndWebpageTitle');
    Document.setConfigValue('syncGroupAndWebpageTitle', 'false', true);

    I.amOnPage(pageUrl);
    I.click('button.buttons-jstree-settings');
    I.waitForVisible('#jstreeSettingsModal', 10);
    originalMirroring = await I.grabCheckedElementStatus('#jstree-settings-showmirrorings');
    originalShowPages = await I.grabCheckedElementStatus('#jstree-settings-showpages');
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
        folders[key] = { name: pageTitle, id: await I.grabValueFrom('#tree-folder-id'), path: roots[key].path + '/' + pageTitle,
            docId: await I.grabAttributeFrom('#datatableInit tbody tr[id]', 'id') };
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
}

Scenario('Navigate between language mirrors during search', async ({ I, DT }) => {
    openFolder(I, DT, folders.sk);
    showMirroring(I, true);
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
    I.click(node(folders.en) + ` .jstree-mirror-link[data-group-id="${folders.sk.id}"]`);
    I.waitForElement(node(folders.sk) + ' > a.jstree-clicked', 30);
    I.seeElement(node(folders.en) + '.jstree-mirror-peer');
    I.click(node(folders.sk) + ' .jstree-mirror-more');
    I.click(node(folders.sk) + ` .jstree-mirror-link[data-group-id="${folders['duplicate-sk'].id}"]`);
    I.waitForElement(node(folders['duplicate-sk']) + ' > a.jstree-clicked', 30);
});

Scenario('Open mirrored pages from language links in the tree', async ({ I, DT, DTE }) => {
    openFolder(I, DT, folders.sk);
    I.click('button.buttons-jstree-settings');
    I.waitForVisible('#jstreeSettingsModal', 10);
    I.checkOption('#jstree-settings-showpages');
    I.checkOption('#jstree-settings-showmirrorings');
    I.click('#jstree-settings-submit');
    I.waitForInvisible('#jstreeSettingsModal', 10);
    I.jstreeWaitForLoader();
    openFolder(I, DT, folders.sk);
    const pageNode = '#docId-' + folders.sk.docId;
    if (await I.grabNumberOfVisibleElements(node(folders.sk) + '.jstree-closed')) I.click(node(folders.sk) + ' > .jstree-ocl');
    const link = pageNode + ` > .jstree-mirrors .jstree-mirror-link[data-doc-id="${folders.en.docId}"]`;
    I.waitForVisible(link, 20);
    I.see('EN', link);
    I.see('CS', pageNode + ' > .jstree-mirrors');
    I.dontSeeElement(pageNode + ` .jstree-mirror-link[data-doc-id="${folders.sk.docId}"]`);
    I.see('+1', pageNode + ' > .jstree-mirrors .jstree-mirror-more');
    I.click(link);
    DTE.waitForEditor();
    I.seeInCurrentUrl('?docid=' + folders.en.docId);
    I.seeInField('#tree-folder-id', folders.en.id);
    DTE.cancel();

    openFolder(I, DT, folders.sk);
    I.waitForVisible(link, 20);
    I.click(pageNode + ' > .jstree-mirrors .jstree-mirror-more');
    I.click(pageNode + ` .jstree-mirror-link[data-doc-id="${folders['duplicate-sk'].docId}"]`);
    DTE.waitForEditor();
    I.seeInCurrentUrl('?docid=' + folders['duplicate-sk'].docId);
    DTE.cancel();

});

Scenario('Open affected mappings and update diagnostics after correcting a duplicate language', async ({ I, DT, DTE }) => {
    const mappings = await syncMembers(I, DT, DTE, pageTitle);
    openFolder(I, DT, folders.sk);
    I.click('button.buttons-jstree-settings');
    I.waitForVisible('#jstreeSettingsModal', 10);
    I.checkOption('#jstree-settings-showpages');
    I.checkOption('#jstree-settings-showmirrorings');
    I.click('#jstree-settings-submit');
    I.waitForInvisible('#jstreeSettingsModal', 10);
    I.jstreeWaitForLoader();

    for (const [index, type] of ['groups', 'docs'].entries()) {
        openFolder(I, DT, folders.sk);
        if (await I.grabNumberOfVisibleElements(node(folders.sk) + '.jstree-closed')) I.click(node(folders.sk) + ' > .jstree-ocl');
        const source = type === 'groups' ? node(folders.sk) : '#docId-' + folders.sk.docId;
        const link = source + ' > .jstree-mirrors > a.jstree-mirror-problem';
        const href = '/admin/v9/webpages/mirroring/?id=' + mappings[index].syncId + (type === 'docs' ? '#pills-docs' : '');
        I.waitForVisible(link, 20);
        I.seeElement(link + ' .ti-exclamation-circle.text-danger');
        I.assertEqual(await I.grabAttributeFrom(link, 'href'), href);
        I.click(link);
        const table = type + 'MirroringTable';
        DTE.waitForEditor(table);
        I.seeElement('#pills-' + type + '-tab.active');
        I.seeNumberOfElements('div.modal.show', 1);
        I.dontSeeElement('#mirroringConfDataTable_modal');
        const members = (await I.grabValueFromAll('#' + table + '_modal input[id^="DTE_Field_selector"]')).map(Number).filter(id => id > 0).sort((a, b) => a - b);
        I.assertDeepEqual(members, mappings[index].members);
        DTE.cancel(table);
    }
    openFolder(I, DT, folders['duplicate-sk']);
    // Resolving the duplicate language leaves only the different folder-depth warning.
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor('groups-datatable');
    I.clickCss('#pills-dt-groups-datatable-template-tab');
    DTE.selectOption('lng', 'Nemecký');
    DTE.save();
    openFolder(I, DT, folders.sk);
    I.waitForElement(node(folders.sk) + ' > .jstree-mirrors .ti-alert-triangle.text-warning', 30);
    I.dontSeeElement(node(folders.sk) + ' > .jstree-mirrors .ti-exclamation-circle.text-danger');
    I.see('CS', node(folders.sk) + ` .jstree-mirror-link[data-group-id="${folders.cz.id}"]`);

    showMirroring(I, false);
    I.dontSeeElement('#SomStromcek .jstree-mirrors');
    openFolder(I, DT, folders.sk);
    I.dontSeeElement('#SomStromcek .jstree-mirrors');
    I.dontSeeElement('#SomStromcek .jstree-mirror-peer');
});

Scenario('Hide mirror links without the mirroring permission', ({ I }) => {
    showMirroring(I, true);
    I.amOnPage(pageUrl + '?groupid=' + folders.sk.id + '&removePerm=cmp_mirroring');
    I.waitForElement(node(folders.sk) + ' > a.jstree-clicked', 30);
    I.dontSeeElement(node(folders.sk) + ' .jstree-mirror-link');
    I.relogin('admin');
});

Scenario('Keep original page and folder mirrors when moving to another mirrored folder', async ({ I, DT, DTE }) => {
    const sourceName = pageTitle + '-move-source', targetName = pageTitle + '-move-target';
    const title = pageTitle + '-move-page', translatedName = pageTitle + '-move-translated';
    const pageRow = locate('#datatableInit tbody tr').withText(title);
    const branches = {};
    for (const name of [sourceName, targetName]) {
        openFolder(I, DT, roots.sk);
        I.click(DT.btn.tree_add_button);
        DTE.waitForEditor('groups-datatable');
        I.fillField('#DTE_Field_groupName', name);
        DTE.save();
    }
    for (const key of ['sk', 'en', 'cz', 'duplicate-sk']) {
        openFolder(I, DT, roots[key]);
        if (await I.grabNumberOfVisibleElements(node(roots[key]) + '.jstree-closed')) I.click(node(roots[key]) + ' > .jstree-ocl');
        I.jstreeWaitForLoader();
        I.click(locate(node(roots[key]) + ' > ul > li > a.jstree-anchor').withText(sourceName));
        DT.waitForLoader();
        const source = { id: await I.grabValueFrom('#tree-folder-id') };
        I.click('#datatableInit tbody tr.is-default-page td.dt-row-edit a');
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.checkOption('#DTE_Field_generateUrlFromTitle_0');
        DTE.save();
        I.click('#datatableInit tbody tr.is-default-page td.dt-row-edit a');
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        source.oldUrl = await I.grabValueFrom('#DTE_Field_virtualPath');
        DTE.cancel();
        if (key === 'sk') {
            I.click(DT.btn.add_button);
            DTE.waitForEditor();
            I.fillField('#DTE_Field_title', title);
            DTE.save();
        }
        I.seeNumberOfElements(pageRow, 1);
        const pageId = await I.grabAttributeFrom(pageRow, 'id');
        I.click(locate(node(roots[key]) + ' > ul > li > a.jstree-anchor').withText(targetName));
        DT.waitForLoader();
        branches[key] = { source, target: { id: await I.grabValueFrom('#tree-folder-id') }, pageId };
    }
    for (const type of ['page', 'folder']) {
        const isPage = type === 'page';
        if (!isPage) {
            // A translated name must not make the move create another folder.
            openFolder(I, DT, branches.en.source);
            I.click(DT.btn.tree_edit_button);
            DTE.waitForEditor('groups-datatable');
            I.fillField('#DTE_Field_groupName', translatedName);
            DTE.save();
        }
        openFolder(I, DT, branches.sk.source);
        if (isPage) I.click(title, '#datatableInit tbody');
        else I.click(DT.btn.tree_edit_button);
        DTE.waitForEditor(isPage ? 'datatableInit' : 'groups-datatable');
        if (isPage) I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.clickCss('#editorAppDTE_Field_editorFields-' + (isPage ? 'groupDetails' : 'parentGroupDetails') + ' button.btn-webjet-jstree-item-edit');
        I.waitForVisible(WebjetDteJsTree.tree);
        const pickerRoot = WebjetDteJsTree.tree + ` li[id="${roots.sk.id}"]`;
        if (await I.grabNumberOfVisibleElements(pickerRoot + '.jstree-closed')) I.click(pickerRoot + ' > .jstree-ocl');
        I.waitForInvisible(WebjetDteJsTree.loading);
        I.click(WebjetDteJsTree.tree + ` li[id="${branches.sk.target.id}"] > a.jstree-anchor`);
        I.waitForInvisible(WebjetDteJsTree.tree);
        DTE.save();
    }
    for (const [key, branch] of Object.entries(branches)) {
        openFolder(I, DT, branch.source);
        I.dontSeeElement(pageRow);
        I.click('#datatableInit tbody tr.is-default-page td.dt-row-edit a');
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        const movedUrl = await I.grabValueFrom('#DTE_Field_virtualPath');
        I.assertNotEqual(movedUrl, branch.source.oldUrl);
        I.assertContain(movedUrl, '/' + targetName + '/');
        DTE.cancel();
        I.dontSeeElement(node(roots[key]) + ` > ul > li[id="${branch.source.id}"]`);
        openFolder(I, DT, branch.target);
        I.seeNumberOfElements(pageRow, 1);
        I.assertEqual(await I.grabAttributeFrom(pageRow, 'id'), branch.pageId);
        if (await I.grabNumberOfVisibleElements(node(branch.target) + '.jstree-closed')) I.click(node(branch.target) + ' > .jstree-ocl');
        I.jstreeWaitForLoader();
        I.seeNumberOfElements(node(branch.target) + ' > ul > li:not([id^="docId-"])', 1);
        I.seeElement(node(branch.target) + ` > ul > li[id="${branch.source.id}"]`);
        I.see(key === 'en' ? translatedName : sourceName, node(branch.source) + ' > a');
    }
});

Scenario('Reject folder deletion without deleteDir while editing remains allowed', async ({ I, DT, DTE }) => {
    I.amOnPage(pageUrl + '?groupid=' + folders.sk.id + '&removePerm=deleteDir');
    I.waitForElement(node(folders.sk) + ' > a.jstree-clicked', 30);
    DT.waitForLoader();
    I.seeElement(DT.btn.tree_edit_button);
    I.dontSeeElement(DT.btn.tree_delete_button);
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor('groups-datatable');
    DTE.cancel('groups-datatable');
    // The hidden delete button must not be the only protection against a forged remove request.
    const headers = await requestHeaders(I);
    const folder = (await I.sendGetRequest('/admin/rest/groups/' + folders.sk.id, headers)).data;
    const response = await I.sendPostRequest('/admin/rest/groups/editor', { action: 'remove', data: { [folders.sk.id]: folder } }, headers);
    I.assertContain(response.data.error || '', 'nemáte právo na zmazanie adresáru');
    for (const folder of Object.values(folders)) {
        openFolder(I, DT, folder);
        I.dontSeeElement('#pills-trash-tab.active');
        I.see(folder.name, node(folder) + ' > a');
    }
    I.relogin('admin');
});

Scenario('Reject mirrored page deletion if the last mirror is forbidden', async ({ I, DT, DTE, Document }) => {
    const ids = {};
    for (const [key, folder] of Object.entries(folders)) {
        openFolder(I, DT, folder);
        ids[key] = await I.grabAttributeFrom(locate('#datatableInit tbody tr').withText(pageTitle), 'id');
    }
    // Cascades use page ID order; language creation order can vary.
    const forbidden = Object.keys(ids).filter(key => key !== 'sk').sort((a, b) => Number(ids[b]) - Number(ids[a]))[0];
    const allowed = Object.keys(ids).filter(key => key !== forbidden);
    for (const key of allowed) I.assertAbove(Number(ids[forbidden]), Number(ids[key]));

    I.amOnPage('/admin/v9/users/user-list/');
    DT.filterEquals('login', 'tester2');
    I.click('#datatableInit tbody td.dt-row-edit a');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
    for (const permission of ['deletePage', 'pageSave']) {
        I.seeNumberOfElements(`#DTE_Field_editorFields-enabledItems li[id="perms_${permission}"] > a.jstree-clicked`, 1);
    }
    deletePermissionGroups = await I.grabValueFromAll(editableGroups + ' input.form-control');
    for (const root of allowed.map(key => key === 'cz' ? roots.container : roots[key])) {
        I.click(editableGroups + ' button.btn-webjet-jstree-add');
        I.waitForVisible(WebjetDteJsTree.tree);
        I.waitForVisible(locate(WebjetDteJsTree.anchors).withText(domain));
        I.click(locate(WebjetDteJsTree.anchors).withText(domain));
        I.waitForVisible(WebjetDteJsTree.tree + ` li[id="${root.id}"] > a`);
        I.click(WebjetDteJsTree.tree + ` li[id="${root.id}"] > a`);
        I.waitForInvisible(WebjetDteJsTree.tree);
    }
    DTE.save();
    I.relogin('tester2');
    I.amOnPage(pageUrl);
    Document.switchDomain(domain);
    openPageDelete(I, DT, DTE, folders.sk);
    I.waitForText('ku ktorým nemáte prístup', 20, pageModal + ' .delete-mirrors-info');
    I.waitForEnabled(pageModal + ' button.btn-primary', 20);
    DTE.save();
    I.waitForText('nemáte právo na zmazanie web stránky', 20, pageModal + ' .DTE_Form_Error');
    DTE.cancel();

    I.relogin('admin');
    I.amOnPage(pageUrl);
    Document.switchDomain(domain);
    for (const [key, folder] of Object.entries(folders)) {
        openFolder(I, DT, folder);
        I.dontSeeElement('#pills-trash-tab.active');
        const row = locate('#datatableInit tbody tr').withText(pageTitle);
        I.seeNumberOfElements(row, 1);
        I.assertEqual(await I.grabAttributeFrom(row, 'id'), ids[key]);
    }
});

async function restoreDeletePermissions(I, DT, DTE) {
    if (deletePermissionGroups === undefined) return;
    I.relogin('admin');
    I.amOnPage('/admin/v9/users/user-list/');
    DT.filterEquals('login', 'tester2');
    I.click('#datatableInit tbody td.dt-row-edit a');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
    const paths = await I.grabValueFromAll(editableGroups + ' input.form-control');
    for (let index = paths.length - 1; index >= 0; index--) {
        if ([roots.sk, roots.en, roots['duplicate-sk'], roots.container].some(root => paths[index].endsWith('/' + root.name))) {
            I.click(locate(editableGroups + ' button.btn-webjet-jstree-item-remove').at(index + 1));
        }
    }
    I.assertDeepEqual(await I.grabValueFromAll(editableGroups + ' input.form-control'), deletePermissionGroups);
    DTE.save();
    await I.waitForInvisible(pageModal);
    deletePermissionGroups = undefined;
}

Scenario('Preview and bulk-delete mirrors without permanently deleting selected counterparts or descendants', async ({ I, DT, DTE }) => {
    const mirrored = Object.values(folders);
    const descendants = [];
    const childTitle = pageTitle + '-descendant';
    openFolder(I, DT, folders.sk);
    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor('groups-datatable');
    I.fillField('#DTE_Field_groupName', childTitle);
    DTE.save();
    for (const folder of mirrored) {
        openFolder(I, DT, folder);
        if (await I.grabNumberOfVisibleElements(node(folder) + '.jstree-closed')) I.click(node(folder) + ' > .jstree-ocl');
        I.jstreeWaitForLoader();
        I.click(locate(node(folder) + ' > ul > li > a.jstree-anchor').withText(childTitle));
        DT.waitForLoader();
        descendants.push({ name: childTitle, id: await I.grabValueFrom('#tree-folder-id'),
            docId: await I.grabAttributeFrom('#datatableInit tbody tr.is-default-page', 'id') });
    }

    openPageDelete(I, DT, DTE, folders.sk);
    for (const folder of mirrored.slice(1)) I.waitForText(folder.path, 20, pageModal + ' .delete-mirrors-info.alert-warning');
    I.dontSee(folders.sk.path, pageModal + ' .delete-mirrors-info');
    I.seeElement(pageModal + ' button.btn-primary:enabled');
    DTE.cancel();

    // Submit two counterparts from different folders in one request, including a page reached by the first cascade.
    const selected = [folders.sk.docId, folders.en.docId];
    const headers = await requestHeaders(I);
    const preview = await I.sendPostRequest('/admin/rest/web-pages/deletion-preview?ids=' + selected.join(','), {}, headers);
    I.assertEqual(preview.status, 200);
    I.assertDeepEqual(preview.data.items.map(item => item.id).sort((a, b) => a - b),
        [folders.cz.docId, folders['duplicate-sk'].docId].map(Number).sort((a, b) => a - b));
    const data = {};
    for (const id of selected) data[id] = (await I.sendGetRequest('/admin/rest/web-pages/' + id, headers)).data;
    const deleted = await I.sendPostRequest('/admin/rest/web-pages/editor', { action: 'remove', data }, headers);
    I.assertEqual(deleted.status, 200);
    I.assertEmpty(deleted.data.error || '');
    I.assertEmpty(deleted.data.fieldErrors || []);
    I.click('#pills-trash-tab');
    DT.waitForLoader();
    DT.filterEquals('title', pageTitle);
    I.assertDeepEqual((await I.grabAttributeFromAll('#datatableInit tbody tr[id]', 'id')).sort(),
        mirrored.map(folder => folder.docId).sort());

    openFolder(I, DT, folders.sk);
    for (const folder of [roots.en, folders.sk]) {
        if (await I.grabNumberOfVisibleElements(node(folder) + '.jstree-closed')) {
            I.click(node(folder) + ' > .jstree-ocl');
            I.jstreeWaitForLoader();
        }
    }
    // Select a descendant as well as two mirrored parents to exercise request-wide deletion deduplication.
    for (const folder of [folders.en, descendants[0]]) {
        I.click(node(folder) + ' > a', null, { modifiers: ['Meta'] });
        DT.waitForLoader();
    }
    for (const folder of [folders.sk, folders.en, descendants[0]]) I.seeElement(node(folder) + ' > a.jstree-clicked');
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor('groups-datatable');
    for (const folder of mirrored.slice(2)) I.waitForText(folder.path, 20, groupModal + ' .delete-mirrors-info.alert-warning');
    confirmDelete(I, DTE, groupModal);
    for (const folder of [...mirrored, ...descendants]) {
        openFolder(I, DT, folder);
        I.seeElement('#pills-trash-tab.active');
        I.see(folder.name, node(folder) + ' > a');
    }
    I.amOnPage(pageUrl);
    I.click('#pills-trash-tab');
    DT.waitForLoader();
    DT.filterEquals('title', childTitle);
    I.assertDeepEqual((await I.grabAttributeFromAll('#datatableInit tbody tr[id]', 'id')).sort(),
        descendants.map(folder => folder.docId).sort());

    openFolder(I, DT, roots.plain);
    I.dontSeeElement('#pills-trash-tab.active');
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor('groups-datatable');
    I.waitForEnabled(groupModal + ' button.btn-primary', 20);
    I.dontSeeElement(groupModal + ' .delete-mirrors-info');
    DTE.cancel();
});

Scenario('Cancel mirroring only in an editable subtree of the current domain', async ({ I, DT, DTE, Document }) => {
    openFolder(I, DT, roots.sk);
    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor('groups-datatable');
    I.fillField('#DTE_Field_groupName', pageTitle + '-unlink');
    DTE.save();
    I.jstreeClick(pageTitle + '-unlink');
    DT.waitForLoader();
    const sourceIds = [Number(await I.grabValueFrom('#tree-folder-id')), Number(await I.grabAttributeFrom('#datatableInit tbody tr.is-default-page', 'id'))];
    const before = await syncMembers(I, DT, DTE, pageTitle + '-unlink');
    I.assertDeepEqual(before.map(row => row.members.length), [4, 4]);
    before.forEach((row, i) => I.assertContain(row.members, sourceIds[i]));
    for (const id of [-1, 2147483647]) I.assertEqual(await cancelSync(I, id), 400);

    I.amOnPage('/admin/v9/users/user-list/');
    DT.filterEquals('login', 'tester2');
    const userId = await I.grabAttributeFrom('#datatableInit tbody tr[id]', 'id');
    I.click('#datatableInit tbody td.dt-row-edit a');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
    cancelSyncUser = {
        id: userId,
        enabledItems: (await I.grabAttributeFromAll(enabledItems + ' a.jstree-clicked', 'id')).sort(),
        editableGroups: await I.grabValueFromAll(editableGroups + ' input.form-control')
    };
    if (!cancelSyncUser.enabledItems.includes('perms_cmp_clone_structure_anchor')) I.click(clonePermission);
    I.click(editableGroups + ' button.btn-webjet-jstree-add');
    I.waitForVisible(locate(WebjetDteJsTree.anchors).withText(domain));
    I.click(locate(WebjetDteJsTree.anchors).withText(domain));
    I.waitForVisible(WebjetDteJsTree.tree + ` li[id="${roots.plain.id}"] > a`);
    I.click(WebjetDteJsTree.tree + ` li[id="${roots.plain.id}"] > a`);
    I.waitForInvisible(WebjetDteJsTree.tree);
    DTE.save();
    I.relogin('tester2');
    I.amOnPage(pageUrl);
    Document.switchDomain(domain);
    I.amOnPage('/components/clone_structure/clone_structure.jsp');
    I.waitForVisible('#undoSyncGroupId');
    I.seeElement('#btnUndoSync');
    I.amOnPage(pageUrl);
    I.assertEqual(await cancelSync(I, roots.sk.id), 403);

    I.relogin('admin');
    I.amOnPage(pageUrl);
    I.assertNotEqual((await I.grabTextFrom('div.js-domain-toggler div.bootstrap-select button')).trim(), domain);
    I.assertEqual(await cancelSync(I, roots.sk.id), 403);
    Document.switchDomain(domain);
    I.assertEqual(await cancelSync(I, 0), 400);
    I.assertDeepEqual(await syncMembers(I, DT, DTE, before), before);
    I.amOnPage('/components/clone_structure/clone_structure.jsp');
    I.fillField('#undoSyncGroupId', roots.sk.id);
    I.click('#btnUndoSync');
    I.waitForVisible('#undo_succ', 30);
    I.assertDeepEqual(await syncMembers(I, DT, DTE, before), before.map((row, i) => ({ ...row, members: row.members.filter(id => id !== sourceIds[i]) })));
});

async function restoreCancelSyncPermissions(I, DTE) {
    if (!cancelSyncUser) return;
    I.relogin('admin');
    I.amOnPage('/admin/v9/users/user-list/?id=' + cancelSyncUser.id);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
    if (!cancelSyncUser.enabledItems.includes('perms_cmp_clone_structure_anchor') && await I.grabNumberOfVisibleElements(clonePermission + '.jstree-clicked')) I.click(clonePermission);
    const paths = await I.grabValueFromAll(editableGroups + ' input.form-control');
    for (let index = paths.length - 1; index >= 0; index--) {
        if (paths[index].endsWith('/' + roots.plain.name)) I.click(locate(editableGroups + ' button.btn-webjet-jstree-item-remove').at(index + 1));
    }
    DTE.save();
    I.amOnPage('/admin/v9/users/user-list/?id=' + cancelSyncUser.id);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
    I.assertDeepEqual((await I.grabAttributeFromAll(enabledItems + ' a.jstree-clicked', 'id')).sort(), cancelSyncUser.enabledItems);
    I.assertDeepEqual(await I.grabValueFromAll(editableGroups + ' input.form-control'), cancelSyncUser.editableGroups);
    DTE.cancel();
    await I.waitForInvisible(pageModal);
    cancelSyncUser = undefined;
}

After(async ({ I, DT, DTE }) => {
    await restoreDeletePermissions(I, DT, DTE);
    await restoreCancelSyncPermissions(I, DTE);
});

AfterSuite(async ({ I, DT, DTE, Document }) => {
    if (!fixtureStarted) return;
    await restoreDeletePermissions(I, DT, DTE);
    await restoreCancelSyncPermissions(I, DTE);
    I.relogin('admin');
    I.amOnPage(pageUrl);
    if ((await I.grabTextFrom('div.js-domain-toggler div.bootstrap-select button')).trim() !== domain) {
        Document.switchDomain(domain);
    }
    if (originalConfig !== undefined) Document.setConfigValue(configName, originalConfig, true);
    if (originalTitleSync !== undefined) Document.setConfigValue('syncGroupAndWebpageTitle', originalTitleSync, true);
    I.amOnPage(pageUrl);
    if (originalMirroring !== undefined) showMirroring(I, originalMirroring);
    if (originalShowPages !== undefined) {
        I.click('button.buttons-jstree-settings');
        I.waitForVisible('#jstreeSettingsModal', 10);
        if (originalShowPages) I.checkOption('#jstree-settings-showpages');
        else I.uncheckOption('#jstree-settings-showpages');
        I.click('#jstree-settings-submit');
        I.waitForInvisible('#jstreeSettingsModal', 10);
        I.jstreeWaitForLoader();
    }

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
    I.logout();
});
