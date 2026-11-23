Feature('apps.file-archive.folder_permissions').tag('@singlethread');

const SL = require('./SL.js');
const WebjetDteJsTree = require('../../../pages/WebjetDteJsTree');

// Run the whole feature so setup and permission cleanup accompany the tree checks.
const restrictedUser = 'tester';
const archiveRoot = '/files/archiv';
const allowedPath = archiveRoot + '/marketing';
const nestedPath = allowedPath + '/webjet-cms';
const deniedPath = archiveRoot + '/sales';
const destination = '#editorAppDTE_Field_editorFields-dir';
const writableFolders = '#editorAppDTE_Field_editorFields-writableFolders';

Before(({ login }) => {
    login('admin');
});

Scenario('Restrict filesystem permissions to the archive subfolder', async ({ I, DT, DTE }) => {
    openUserRights(I, DT, DTE);
    await clearWritableFolders(I);
    I.clickCss(writableFolders + ' .btn-webjet-jstree-add');
    I.waitForVisible(WebjetDteJsTree.tree, 10);
    await expandPickerFolder(I, '/files');
    await expandPickerFolder(I, archiveRoot);
    I.click(pickerAnchor(allowedPath));
    I.waitForInvisible(WebjetDteJsTree.modal, 10);
    I.seeInField(writableFolders + ' input.form-control', allowedPath);
    I.assertEqual(await I.grabNumberOfVisibleElements(writableFolders + ' input.form-control'), 1);
    DTE.save();
    I.waitForInvisible('#datatableInit_modal', 10);
    I.relogin(restrictedUser);
});

Scenario('Navigate the archive tree with permission only for a subfolder', async ({ I, DT }) => {
    I.amOnPage(SL.fileArchive);
    DT.waitForLoader('fileArchiveDataTable');
    await selectArchiveFolder(I, DT, archiveRoot);
    I.seeElement(archiveAnchor(archiveRoot).find('.ti-folder-x'));
    I.seeElement('#btn-create-folder[disabled]');

    await selectArchiveFolder(I, DT, allowedPath);
    I.seeElement(archiveAnchor(allowedPath).find('.ti-folder-filled'));
    I.seeElement('#btn-create-folder:not([disabled])');
    await selectArchiveFolder(I, DT, nestedPath);
    I.seeElement(archiveAnchor(nestedPath).find('.ti-folder-filled'));

    await selectArchiveFolder(I, DT, deniedPath);
    I.seeElement(archiveAnchor(deniedPath).find('.ti-folder-x'));
    I.seeElement('#btn-create-folder[disabled]');

    // Direct navigation also verifies the server-rendered initial tree selection.
    I.amOnPage(SL.fileArchive + '?dir=' + encodeURIComponent(nestedPath));
    DT.waitForLoader('fileArchiveDataTable');
    I.waitForElement(archiveAnchor(nestedPath, '.jstree-clicked'), 10);
    I.seeElement(archiveAnchor(archiveRoot).find('.ti-folder-x'));
    I.seeElement(archiveAnchor(allowedPath).find('.ti-folder-filled'));
    I.seeElement('#btn-create-folder:not([disabled])');
});

Scenario('Select and preselect permitted destinations in the folder picker', async ({ I, DT, DTE }) => {
    I.amOnPage(SL.fileArchive);
    DT.waitForLoader('fileArchiveDataTable');
    await selectArchiveFolder(I, DT, archiveRoot);
    I.clickCss('#fileArchiveDataTable_wrapper .buttons-create');
    DTE.waitForEditor('fileArchiveDataTable');
    const initialDestination = await I.grabValueFrom(destination + ' input.form-control');
    I.assertEqual(initialDestination.replace(/\/$/, ''), archiveRoot);
    openDestinationPicker(I);
    I.waitForText('Do predvoleného priečinka „' + initialDestination + '“ nemáte právo zápisu.', 10, '.toast-warning');
    I.toastrClose();
    I.seeElement(pickerAnchor(archiveRoot, '.jstree-disabled'));
    I.seeElement(pickerAnchor(deniedPath, '.jstree-disabled'));
    I.seeElement(WebjetDteJsTree.modal);
    I.dontSeeElement(pickerAnchor(deniedPath, '.jstree-clicked'));
    I.seeInField(destination + ' input.form-control', initialDestination);

    I.click(pickerAnchor(allowedPath));
    I.waitForInvisible(WebjetDteJsTree.modal, 10);
    I.seeInField(destination + ' input.form-control', allowedPath);
    openDestinationPicker(I);
    I.waitForElement(pickerAnchor(allowedPath, '.jstree-clicked'), 10);
    I.dontSeeElement('.toast-warning');
    I.seeElement(WebjetDteJsTree.modal);
    I.click(pickerAnchor(allowedPath));
    I.waitForInvisible(WebjetDteJsTree.modal, 10);

    // A manually entered nested path must expand and select without a root-folder warning.
    I.fillField(destination + ' input.form-control', nestedPath);
    openDestinationPicker(I);
    I.waitForElement(pickerAnchor(nestedPath, '.jstree-clicked'), 10);
    I.dontSeeElement('.toast-warning');
    I.click(pickerAnchor(nestedPath));
    I.waitForInvisible(WebjetDteJsTree.modal, 10);
    I.seeInField(destination + ' input.form-control', nestedPath);
    DTE.cancel('fileArchiveDataTable');
});

Scenario('Clear filesystem directory restrictions after the tree checks', async ({ I, DT, DTE }) => {
    openUserRights(I, DT, DTE);
    await clearWritableFolders(I);
    DTE.save();
    I.waitForInvisible('#datatableInit_modal', 10);
    I.relogin(restrictedUser);

    openUserRights(I, DT, DTE);
    I.assertEqual(await I.grabNumberOfVisibleElements(writableFolders + ' input.form-control'), 0);
    DTE.cancel();
    I.logout();
});

async function clearWritableFolders(I) {
    const count = await I.grabNumberOfVisibleElements(writableFolders + ' .btn-webjet-jstree-item-remove');
    for (let i = 0; i < count; i++) {
        I.click(locate(writableFolders + ' .btn-webjet-jstree-item-remove').first());
    }
}

function openUserRights(I, DT, DTE) {
    I.amOnPage('/admin/v9/users/user-list/');
    DT.filterEquals('login', restrictedUser);
    I.click(locate('#datatableInit td.dt-row-edit a').withText(restrictedUser));
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-rightsTab-tab');
    I.waitForVisible(writableFolders, 10);
}

function archiveAnchor(folder, state = '') {
    return locate('#SomStromcek a.jstree-anchor' + state).withAttr({ id: folder + '_anchor' });
}

function pickerAnchor(folder, state = '') {
    return locate(WebjetDteJsTree.anchors + state).withAttr({ id: folder + '_anchor' });
}

/** Selects a folder through the archive tree, expanding its parent when needed. */
async function selectArchiveFolder(I, DT, folder) {
    const parts = folder.substring(archiveRoot.length).split('/').filter(Boolean);
    let parent = archiveRoot;
    for (const part of parts) {
        await I.clickIfVisible('#SomStromcek li.jstree-closed[id="' + parent + '"] > .jstree-ocl');
        parent += '/' + part;
        I.waitForVisible(archiveAnchor(parent), 10);
    }
    I.waitForVisible(archiveAnchor(folder), 10);
    I.click(archiveAnchor(folder));
    I.waitForElement(archiveAnchor(folder, '.jstree-clicked'), 10);
    DT.waitForLoader('fileArchiveDataTable');
}

function openDestinationPicker(I) {
    I.clickCss(destination + ' .btn-webjet-jstree-item-edit');
    I.waitForVisible(WebjetDteJsTree.tree, 10);
    I.waitForInvisible(WebjetDteJsTree.loading, 10);
}

async function expandPickerFolder(I, folder) {
    I.waitForVisible(pickerAnchor(folder), 10);
    await I.clickIfVisible(WebjetDteJsTree.tree + ' li.jstree-closed[id="' + folder + '"] > .jstree-ocl');
    I.waitForInvisible(WebjetDteJsTree.loading, 10);
}
