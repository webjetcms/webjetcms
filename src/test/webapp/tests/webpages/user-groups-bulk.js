Feature('webpages.user-groups-bulk');

let name;
const fixtureNames = [];
let groupIds;
let pageIds;
const menu = '#datatableInit_wrapper button.buttons-more-actions';
const picker = 'button[data-id="datatableInit_userGroupAction"]';
const pickerMenu = '.bs-container.user-group-picker';

Before(({ login }) => {
    login('admin');
});

Scenario('Combined group picker, cancellation and persisted deltas', async ({ I, DT, DTE }) => {
    await createPages(I, DT, DTE);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    I.resizeWindow(767, 853);
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');
    I.clickCss(menu);
    I.clickCss('.buttons-addUserGroup');
    I.waitForVisible(picker);
    I.see('Zvolená skupina používateľov sa pridá k 2 označeným stránkam.', '.toast-message');
    const options = await I.executeScript(() => Array.from(document.querySelector('#datatableInit_userGroupAction').options).map(option => Number(option.value)));
    for (const id of groupIds) I.assertContain(options, id);
    selectGroup(I, 0);
    // Closing an open detached list must discard the selected change as well.
    I.clickCss(picker);
    I.waitForVisible(pickerMenu + ' .bs-searchbox input');
    I.clickCss('.toast-close-button');
    I.waitToHide(picker);
    I.dontSeeElement(pickerMenu);
    I.wjSetDefaultWindowSize();
    await verifyPages(I, DTE, [[groupIds[0]], [groupIds[1]]]);

    changeGroup(I, DT, false, 0);
    changeGroup(I, DT, false, 1);
    await verifyPages(I, DTE, [groupIds, groupIds]);
    changeGroup(I, DT, true, 0);
    await verifyPages(I, DTE, [[groupIds[1]], [groupIds[1]]]);
    changeGroup(I, DT, true, 1);
    await verifyPages(I, DTE, [[], []]);
    I.executeScript(() => webpagesDatatable.rows().select());
    I.clickCss(menu);
    I.clickCss('.buttons-removeUserGroup');
    I.waitForText('Pre označené stránky nie je dostupná žiadna skupina používateľov pre túto akciu.', 10, '.toast-title');
    I.dontSeeElement(picker);
});

Scenario('Reopening a page waits for slow CKEditor iframe restoration', async ({ I, DT, DTE }) => {
    await createPages(I, DT, DTE);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    I.click(name + '-0', '#datatableInit');
    DTE.waitForEditor();
    await I.executeScript(async () => {
        const conf = webpagesDatatable.EDITOR.field('data').s.opts;
        await conf.wjeditorReadyPromise;
        conf.wjeditor.ckEditorInstance.config.contentsCss.push('/autotest-editor-recreate.css');
        const originalSetData = conf.wjeditor.setData;
        window.autotestEditorWrite = { started: false, ready: false, overlappingWrites: 0 };
        conf.wjeditor.setData = function(data) {
            const state = window.autotestEditorWrite;
            state.started = true;
            state.status = this.ckEditorInstance.status;
            const listener = this.ckEditorInstance.on('afterSetData', () => {
                if (this.ckEditorInstance.status === 'recreating') state.overlappingWrites++;
            });
            const ready = originalSetData.call(this, data);
            ready.then(() => { state.ready = true; listener.removeListener(); });
            return ready;
        };
    });
    DTE.cancel();

    let release;
    const loadGate = new Promise(resolve => { release = resolve; });
    const routePattern = '**/autotest-editor-recreate.css*';
    const holdStylesheet = async route => {
        await loadGate;
        await route.fulfill({ status: 200, contentType: 'text/css', body: '' });
    };
    await I.mockRoute(routePattern, holdStylesheet);
    try {
        I.click(name + '-1', '#datatableInit');
        DTE.waitForEditor();
        I.waitForFunction(() => window.autotestEditorWrite.started, [], 10);
        const pending = await I.executeScript(() => window.autotestEditorWrite);
        I.assertEqual(pending.status, 'recreating', 'The previous iframe restoration must still be pending');
        I.assertFalse(pending.ready);
        I.assertEqual(pending.overlappingWrites, 0, 'The new page write must wait for iframe restoration');
        release();
        const content = await I.executeScript(async () => {
            const editor = webpagesDatatable.EDITOR;
            await editor.field('data').s.opts.wjeditorReadyPromise;
            return { id: editor.currentJson.docId, stored: editor.currentJson.data,
                rendered: ckEditorInstance.getData(), dirty: editor.field('data').isDirty() };
        });
        I.assertEqual(content.id, pageIds[1]);
        I.assertContain(content.stored, name + ' content 1');
        I.assertContain(content.rendered, name + ' content 1');
        I.assertFalse(content.dirty, 'Loading the selected page must establish its clean baseline');
        DTE.cancel();
    } finally {
        release();
        await I.stopMockingRoute(routePattern, holdStylesheet);
    }
});

Scenario('Both user group actions require pageSave permission', async ({ I, DT, DTE }) => {
    await createPages(I, DT, DTE);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67&removePerm=pageSave');
    DT.waitForLoader();
    I.clickCss(menu);
    I.dontSeeElement('.buttons-addUserGroup');
    I.dontSeeElement('.buttons-removeUserGroup');
    I.pressKey('Escape');
    for (const action of ['addUserGroup', 'removeUserGroup']) {
        const response = await I.executeScript(async ({ action, id, groupId }) => $.post({
            url: '/admin/rest/web-pages/action/' + action,
            data: { ids: [id], customData: groupId }
        }), { action, id: pageIds[0], groupId: groupIds[0] });
        I.assertContain(response.error, 'Nemáte právo na editáciu web stránky');
    }
    I.logout();
});

Scenario('Delete created pages and user groups', async ({ I, DT }) => {
    for (const fixtureName of fixtureNames) {
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
        DT.filterContains('title', fixtureName);
        if (await I.executeScript(() => webpagesDatatable.rows({ search: 'applied' }).count()) > 0) DT.deleteAll();
        I.amOnPage('/admin/v9/users/user-groups/');
        DT.filterContains('userGroupName', fixtureName);
        if (await I.executeScript(() => $('#userGroupsDataTable').DataTable().rows({ search: 'applied' }).count()) > 0) DT.deleteAll('userGroupsDataTable');
    }
});

/** Creates this suite's data on demand, including when only one functional scenario is selected. */
async function createPages(I, DT, DTE) {
    if (pageIds) return;
    name = 'user-groups-bulk-autotest-' + I.getRandomText();
    fixtureNames.push(name);
    I.amOnPage('/admin/v9/users/user-groups/');
    DT.waitForLoader();
    for (let index = 0; index < 2; index++) {
        I.clickCss('#userGroupsDataTable_wrapper button.buttons-create');
        DTE.waitForEditor('userGroupsDataTable');
        DTE.fillField('userGroupName', name + '-group-' + index);
        DTE.selectOption('userGroupType', index === 0 ? 'Prístupov k zaheslovanej sekcii web sídla' : 'Prihlásenie k hromadnému e-mailu');
        DTE.save('userGroupsDataTable');
    }
    DT.filterContains('userGroupName', name);
    groupIds = await I.executeScript(() => userGroupsDataTable.rows({ search: 'applied' }).data().toArray()
        .sort((a, b) => a.userGroupType - b.userGroupType).map(row => Number(row.id)));
    I.assertLengthOf(groupIds, 2);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    for (let index = 0; index < 2; index++) {
        I.clickCss('#datatableInit_wrapper button.buttons-create');
        DTE.waitForEditor();
        // New-page initialization opens Basic asynchronously; fill it before switching to Content.
        I.waitForVisible('#DTE_Field_title');
        DTE.fillField('title', name + '-' + index);
        DTE.fillField('navbar', name + '-' + index);
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + name + ' content ' + index + '</p>');
        I.clickCss('#pills-dt-datatableInit-access-tab');
        I.checkOption('.DTE_Field_Name_editorFields\\.' + (index === 0 ? 'permisions' : 'emails') + ' input[value="' + groupIds[index] + '"]');
        DTE.save();
    }
    DT.filterContains('title', name);
    pageIds = await I.executeScript(() => webpagesDatatable.rows({ search: 'applied' }).data().toArray()
        .sort((a, b) => a.title.localeCompare(b.title)).map(row => row.docId));
    I.assertLengthOf(pageIds, 2);
}

/** Uses the visible search control shared by permission and email groups. */
function selectGroup(I, index) {
    const label = name + '-group-' + index;
    I.clickCss(picker);
    I.waitForVisible(pickerMenu + ' .bs-searchbox input');
    I.fillField(pickerMenu + ' .bs-searchbox input', label);
    I.click(locate(pickerMenu + ' .dropdown-item').withText(label));
    I.waitForInvisible(pickerMenu);
    I.see(label, picker);
}

/** Applies a delta and waits for freshly loaded passwordProtected values. */
function changeGroup(I, DT, remove, index) {
    I.executeScript(() => webpagesDatatable.rows().select());
    I.clickCss(menu);
    I.clickCss(remove ? '.buttons-removeUserGroup' : '.buttons-addUserGroup');
    I.waitForVisible(picker);
    selectGroup(I, index);
    I.click('Potvrdiť', '.toastr-buttons');
    I.waitToHide(picker);
    DT.waitForLoader();
    I.waitForFunction(([id, remove]) => webpagesDatatable.rows().data().toArray()
        .every(row => (row.passwordProtected || '').split(',').includes(String(id)) === !remove), [groupIds[index], remove], 20);
}

/** Reopens saved pages to verify both split editor fields and unchanged page content. */
async function verifyPages(I, DTE, expectedGroups) {
    for (let index = 0; index < 2; index++) {
        I.click(name + '-' + index, '#datatableInit');
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        // Modal focus can be ready before the asynchronous content write finishes.
        // Check the server-loaded value separately so waiting cannot conceal corrupted page data.
        const content = await I.executeScript(async () => {
            const editor = webpagesDatatable.EDITOR;
            await editor.field('data').s.opts.wjeditorReadyPromise;
            return { id: editor.currentJson.docId, stored: editor.currentJson.data,
                rendered: ckEditorInstance.getData() };
        });
        I.assertEqual(content.id, pageIds[index], 'The selected page must be loaded');
        I.assertContain(content.stored, name + ' content ' + index, 'Bulk actions must preserve persisted page content');
        I.assertContain(content.rendered, name + ' content ' + index,
            'The editor must display the selected page content: ' + JSON.stringify(content));
        I.clickCss('#pills-dt-datatableInit-access-tab');
        for (let type = 0; type < 2; type++) {
            const target = '.DTE_Field_Name_editorFields\\.' + (type === 0 ? 'permisions' : 'emails') + ' input[value="' + groupIds[type] + '"]';
            if (expectedGroups[index].includes(groupIds[type])) I.seeCheckboxIsChecked(target);
            else I.dontSeeCheckboxIsChecked(target);
        }
        DTE.cancel();
    }
}
