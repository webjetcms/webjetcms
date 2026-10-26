Feature('webpages.perex-bulk');

let name;
const fixtureNames = [];
let groupIds = [];
let addedGroupId;
let pageIds;
const menu = '#datatableInit_wrapper button.buttons-more-actions';
const select = '#datatableInit_perexGroupAction';
const picker = `button[data-id="${select.substring(1)}"]`;
const pickerMenu = '.bs-container.perex-group-picker';

Before(({ login }) => {
    login('admin');
});

Scenario('Selection, cancellation and persisted perex group deltas', async ({ I, DT, DTE }) => {
    await createPages(I, DT, DTE);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    I.clickCss(menu);
    I.seeElement('.dt-more-actions-menu .buttons-addPerexGroup.disabled');
    I.seeElement('.dt-more-actions-menu .buttons-removePerexGroup.disabled');
    I.pressKey('Escape');
    I.waitToHide('.dt-more-actions-menu');
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');

    I.clickCss(menu);
    I.clickCss('.buttons-addPerexGroup');
    I.waitForVisible(picker);
    I.see('Zvolená značka sa pridá k 2 označeným stránkam.', '.toast-message');
    I.see('Značka', '.toast-message label');
    selectGroup(I, name + '-group-2');
    I.click('Zrušiť', '.toastr-buttons');
    I.waitToHide(picker);
    I.dontSeeElement(pickerMenu);
    await verifyPages(I, DTE, false);

    changeGroup(I, DT, false, addedGroupId);
    await verifyPages(I, DTE, true);
    changeGroup(I, DT, true, addedGroupId);
    await verifyPages(I, DTE, false);

    // Remove a group that only one selected page has, then remove the last remaining group.
    changeGroup(I, DT, true, groupIds[0]);
    changeGroup(I, DT, true, groupIds[1]);
    const groups = await I.executeScript(() => webpagesDatatable.rows().data().toArray().map(row => row.perexGroups));
    I.assertDeepEqual(groups, [[], []]);
});

Scenario('Both perex group actions require pageSave permission', async ({ I, DT, DTE }) => {
    await createPages(I, DT, DTE);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67&removePerm=pageSave');
    DT.waitForLoader();
    I.clickCss(menu);
    I.dontSeeElement('.buttons-addPerexGroup');
    I.dontSeeElement('.buttons-removePerexGroup');
    I.pressKey('Escape');
    for (const action of ['addPerexGroup', 'removePerexGroup']) {
        const response = await I.executeScript(async ({ action, id, groupId }) => $.post({
            url: '/admin/rest/web-pages/action/' + action,
            data: { ids: [id], customData: groupId }
        }), { action, id: pageIds[0], groupId: addedGroupId });
        I.assertContain(response.error, 'Nemáte právo na editáciu web stránky');
    }
    I.logout();
});

Scenario('Delete created pages', async ({ I, DT }) => {
    for (const fixtureName of fixtureNames) {
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
        DT.filterContains('title', fixtureName);
        if (await I.executeScript(() => webpagesDatatable.rows({ search: 'applied' }).count()) > 0) DT.deleteAll();
        I.amOnPage('/admin/v9/webpages/perex/');
        DT.filterContains('perexGroupName', fixtureName);
        if (await I.executeScript(() => $('#perexDataTable').DataTable().rows({ search: 'applied' }).count()) > 0) DT.deleteAll('perexDataTable');
    }
});

/** Creates this suite's data on demand, including when only one functional scenario is selected. */
async function createPages(I, DT, DTE) {
    if (pageIds) return;
    name = 'perex-bulk-autotest-' + I.getRandomText();
    fixtureNames.push(name);
    I.amOnPage('/admin/v9/webpages/perex/');
    DT.waitForLoader();
    for (let index = 0; index < 3; index++) {
        I.clickCss('#perexDataTable_wrapper button.buttons-create');
        DTE.waitForEditor('perexDataTable');
        DTE.fillField('perexGroupName', name + '-group-' + index);
        DTE.save('perexDataTable');
    }
    DT.filterContains('perexGroupName', name);
    const ids = await I.executeScript(() => $('#perexDataTable').DataTable().rows({ search: 'applied' }).data().toArray()
        .sort((a, b) => a.perexGroupName.localeCompare(b.perexGroupName)).map(row => Number(row.id)));
    groupIds = ids.slice(0, 2);
    addedGroupId = ids[2];
    I.assertLengthOf(ids, 3);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    for (let index = 0; index < groupIds.length; index++) {
        I.clickCss('#datatableInit_wrapper button.buttons-create');
        DTE.waitForEditor();
        // New-page initialization opens Basic asynchronously; fill it before switching to Content.
        I.waitForVisible('#DTE_Field_title');
        DTE.fillField('title', name + '-' + index);
        DTE.fillField('navbar', name + '-' + index);
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + name + ' content ' + index + '</p>');
        I.clickCss('#pills-dt-datatableInit-perex-tab');
        I.checkOption('.DTE_Field_Name_perexGroups input[value="' + groupIds[index] + '"]');
        DTE.save();
    }
    DT.filterContains('title', name);
    pageIds = await I.executeScript(() => webpagesDatatable.rows({ search: 'applied' }).data().toArray().map(row => row.docId));
    I.assertLengthOf(pageIds, 2);
}

/** Runs a delta through the visible dialog and waits for the reloaded row data. */
function changeGroup(I, DT, remove, id) {
    I.executeScript(() => webpagesDatatable.rows().select());
    I.clickCss(menu);
    I.clickCss(remove ? '.buttons-removePerexGroup' : '.buttons-addPerexGroup');
    I.waitForVisible(picker);
    selectGroup(I, name + '-group-' + (id === addedGroupId ? 2 : groupIds.indexOf(id)));
    I.click('Potvrdiť', '.toastr-buttons');
    I.waitToHide(picker);
    DT.waitForLoader();
    I.waitForFunction(([id, remove]) => webpagesDatatable.rows().data().toArray()
        .every(row => row.perexGroups.includes(id) === !remove), [id, remove], 20);
}

/** Searches and selects a perex group through the visible bootstrap-select control. */
function selectGroup(I, label) {
    I.clickCss(picker);
    I.waitForVisible(pickerMenu + ' .bs-searchbox input');
    I.fillField(pickerMenu + ' .bs-searchbox input', label);
    I.click(locate(pickerMenu + ' .dropdown-item').withText(label));
    I.waitForInvisible(pickerMenu);
    I.see(label, picker);
}

/** Reopens each saved page to verify persistence and unrelated content. */
async function verifyPages(I, DTE, added) {
    for (let index = 0; index < groupIds.length; index++) {
        I.click(name + '-' + index, '#datatableInit');
        DTE.waitForEditor();
        const content = await I.executeScript(() => ckEditorInstance.getData());
        I.assertContain(content, name + ' content ' + index);
        I.clickCss('#pills-dt-datatableInit-perex-tab');
        I.seeCheckboxIsChecked('.DTE_Field_Name_perexGroups input[value="' + groupIds[index] + '"]');
        const target = '.DTE_Field_Name_perexGroups input[value="' + addedGroupId + '"]';
        if (added) I.seeCheckboxIsChecked(target);
        else I.dontSeeCheckboxIsChecked(target);
        DTE.cancel();
    }
}
