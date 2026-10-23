Feature('webpages.user-groups-bulk');

let name;
let groupIds;
let pageIds;
const menu = '#datatableInit_wrapper button.buttons-more-actions';
const picker = 'button[data-id="datatableInit_userGroupAction"]';
const pickerMenu = '.bs-container.user-group-picker';

Before(({ I, login }) => {
    login('admin');
    if (!name) name = 'user-groups-bulk-autotest-' + I.getRandomText();
});

Scenario('Group pickers fill the field and open above the confirmation at all widths', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');
    for (const width of [1216, 1100, 767]) {
        I.resizeWindow(width, 853);
        for (const type of ['user', 'perex']) {
            const fieldId = 'datatableInit_' + type + 'GroupAction';
            const button = 'button[data-id="' + fieldId + '"]';
            const dropdown = '.bs-container.' + type + '-group-picker';
            I.clickCss(menu);
            I.clickCss('.buttons-add' + (type === 'user' ? 'User' : 'Perex') + 'Group');
            I.waitForVisible(button);
            I.clickCss(button);
            I.waitForVisible(dropdown + ' .bs-searchbox input');
            I.waitForFunction(() => getComputedStyle(document.querySelector('.toast')).opacity === '1');
            const layout = await I.executeScript(({ button, dropdown }) => {
                const toggle = document.querySelector(button);
                const field = toggle.parentElement.getBoundingClientRect();
                const buttonBounds = toggle.getBoundingClientRect();
                const search = document.querySelector(dropdown + ' .bs-searchbox input');
                const searchBounds = search.getBoundingClientRect();
                return {
                    fillsField: Math.abs(field.width - buttonBounds.width) < 1,
                    searchOnTop: document.elementFromPoint(searchBounds.right - 5, searchBounds.top + searchBounds.height / 2) === search
                };
            }, { button, dropdown });
            I.assertTrue(layout.fillsField, type + ' picker must fill its field at ' + width + 'px');
            I.assertTrue(layout.searchOnTop, type + ' search must receive pointer input above the toast at ' + width + 'px');
            I.saveScreenshot('group-picker-' + type + '-' + width + '.png');
            I.clickCss(dropdown + ' .bs-searchbox input');
            I.pressKey('Escape');
            I.waitForInvisible(dropdown);
            I.click('Zrušiť', '.toastr-buttons');
            I.waitToHide(button);
            I.dontSeeElement(dropdown);
        }
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Create pages with different user group types', async ({ I, DT, DTE }) => {
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
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + name + ' content ' + index + '</p>');
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        DTE.fillField('title', name + '-' + index);
        DTE.fillField('navbar', name + '-' + index);
        I.clickCss('#pills-dt-datatableInit-access-tab');
        I.checkOption('.DTE_Field_Name_editorFields\\.' + (index === 0 ? 'permisions' : 'emails') + ' input[value="' + groupIds[index] + '"]');
        DTE.save();
    }
    DT.filterContains('title', name);
    pageIds = await I.executeScript(() => webpagesDatatable.rows({ search: 'applied' }).data().toArray().map(row => row.docId));
    I.assertLengthOf(pageIds, 2);
});

Scenario('Combined group picker, cancellation and persisted deltas', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');
    I.clickCss(menu);
    I.clickCss('.buttons-addUserGroup');
    I.waitForVisible(picker);
    I.see('Zvolená skupina používateľov sa pridá k 2 označeným stránkam.', '.toast-message');
    const options = await I.executeScript(() => Array.from(document.querySelector('#datatableInit_userGroupAction').options).map(option => Number(option.value)));
    for (const id of groupIds) I.assertContain(options, id);
    selectGroup(I, 0);
    I.saveScreenshot('user-groups-bulk-dialog.png');
    I.click('Zrušiť', '.toastr-buttons');
    I.waitToHide(picker);
    I.dontSeeElement(pickerMenu);
    // The detached dropdown overlaps Cancel, so reproduce pointer dismissal with the close button.
    I.clickCss(menu);
    I.clickCss('.buttons-addUserGroup');
    I.waitForVisible(picker);
    I.clickCss(picker);
    I.waitForVisible(pickerMenu + ' .bs-searchbox input');
    I.clickCss('.toast-close-button');
    I.waitToHide(picker);
    I.dontSeeElement(pickerMenu);
    await verifyPages(I, DTE, [[groupIds[0]], [groupIds[1]]]);

    for (let repeat = 0; repeat < 2; repeat++) {
        changeGroup(I, DT, false, 0);
        changeGroup(I, DT, false, 1);
        await verifyPages(I, DTE, [groupIds, groupIds]);
    }
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

Scenario('Both user group actions require pageSave permission', async ({ I, DT }) => {
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

Scenario('Delete created pages and user groups', ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    DT.deleteAll();
    I.amOnPage('/admin/v9/users/user-groups/');
    DT.filterContains('userGroupName', name);
    DT.deleteAll('userGroupsDataTable');
});

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
        const content = await I.executeScript(() => ckEditorInstance.getData());
        I.assertContain(content, name + ' content ' + index);
        I.clickCss('#pills-dt-datatableInit-access-tab');
        for (let type = 0; type < 2; type++) {
            const target = '.DTE_Field_Name_editorFields\\.' + (type === 0 ? 'permisions' : 'emails') + ' input[value="' + groupIds[type] + '"]';
            if (expectedGroups[index].includes(groupIds[type])) I.seeCheckboxIsChecked(target);
            else I.dontSeeCheckboxIsChecked(target);
        }
        DTE.cancel();
    }
}
