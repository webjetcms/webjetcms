Feature('webpages.perex-bulk');

let name;
let groupIds = [];
let addedGroupId;
let pageIds;
const menu = '#datatableInit_wrapper button.buttons-more-actions';
const select = '#datatableInit_perexGroupAction';
const picker = `button[data-id="${select.substring(1)}"]`;
const pickerMenu = '.bs-container.perex-group-picker';

Before(({ I, login }) => {
    login('admin');
    if (!name) name = 'perex-bulk-autotest-' + I.getRandomText();
});

Scenario('Create pages with different perex groups', async ({ I, DT, DTE }) => {
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
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor('<p>' + name + ' content ' + index + '</p>');
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        DTE.fillField('title', name + '-' + index);
        DTE.fillField('navbar', name + '-' + index);
        I.clickCss('#pills-dt-datatableInit-perex-tab');
        I.checkOption('.DTE_Field_Name_perexGroups input[value="' + groupIds[index] + '"]');
        DTE.save();
    }
    DT.filterContains('title', name);
    pageIds = await I.executeScript(() => webpagesDatatable.rows({ search: 'applied' }).data().toArray().map(row => row.docId));
});

Scenario('Menu selection state, cancellation and perex group deltas', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    I.clickCss(menu);
    I.saveScreenshot('perex-bulk-more-actions.png');
    I.see('Kontrola odkazov a prázdnych stránok', '.dt-more-actions-menu');
    I.see('Uložiť ako AB test', '.dt-more-actions-menu');
    I.see('Štatistika stránky', '.dt-more-actions-menu');
    I.seeElement('.dt-more-actions-menu .buttons-addPerexGroup.disabled');
    I.seeElement('.dt-more-actions-menu .buttons-removePerexGroup.disabled');
    I.seeElement('.dt-more-actions-menu .buttons-addUserGroup.disabled');
    I.seeElement('.dt-more-actions-menu .buttons-removeUserGroup.disabled');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('button.buttons-more-actions')),
        'Opening with a pointer keeps focus on the opener');
    I.pressKey('Escape');
    I.waitToHide('.dt-more-actions-menu');
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');

    I.clickCss(menu);
    I.clickCss('.buttons-addPerexGroup');
    I.waitForVisible(picker);
    I.see('Zvolená značka sa pridá k 2 označeným stránkam.', '.toast-message');
    I.see('Značka', '.toast-message label');
    selectGroup(I, name + '-group-2');
    I.clickCss(picker);
    I.waitForVisible(pickerMenu + ' .bs-searchbox input');
    I.pressKey('Escape');
    I.waitForInvisible(pickerMenu);
    I.seeElement(picker);
    I.click('Zrušiť', '.toastr-buttons');
    I.waitToHide(picker);
    I.dontSeeElement(pickerMenu);
    // The detached dropdown overlaps Cancel, so reproduce pointer dismissal with the close button.
    I.clickCss(menu);
    I.clickCss('.buttons-addPerexGroup');
    I.waitForVisible(picker);
    I.clickCss(picker);
    I.waitForVisible(pickerMenu + ' .bs-searchbox input');
    I.clickCss('.toast-close-button');
    I.waitToHide(picker);
    I.dontSeeElement(pickerMenu);
    await verifyPages(I, DTE, false);

    for (let repeat = 0; repeat < 2; repeat++) {
        changeGroup(I, DT, false, addedGroupId);
        await verifyPages(I, DTE, true);
    }
    changeGroup(I, DT, true, addedGroupId);
    await verifyPages(I, DTE, false);

    // Remove a group that only one selected page has, then remove the last remaining group.
    changeGroup(I, DT, true, groupIds[0]);
    changeGroup(I, DT, true, groupIds[1]);
    const groups = await I.executeScript(() => webpagesDatatable.rows().data().toArray().map(row => row.perexGroups));
    I.assertDeepEqual(groups, [[], []]);
});

Scenario('Menu keyboard navigation, pointer handoff and page statistics', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');
    I.clickCss(menu);
    I.pressKey('Escape');
    I.pressKey('Enter');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.buttons-abtest')));
    I.pressKey('ArrowDown');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.buttons-stat')));
    I.moveCursorTo('.buttons-addPerexGroup');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('button.buttons-more-actions')),
        'Moving the pointer removes the previous item focus');
    I.saveScreenshot('perex-bulk-more-actions-hover.png');
    I.pressKey('ArrowDown');
    I.pressKey('End');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.buttons-import-export')));
    I.pressKey('Home');
    I.saveScreenshot('perex-bulk-more-actions-keyboard.png');
    I.pressKey(['Shift', 'Tab']);
    I.waitToHide('.dt-more-actions-menu');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('button.buttons-more-actions')));
    I.pressKey('Enter');
    I.pressKey('End');
    I.pressKey('Tab');
    I.waitToHide('.dt-more-actions-menu');
    I.assertFalse(await I.executeScript(() => document.activeElement.matches('button.buttons-more-actions')),
        'Tab from the final action continues outside the popup');

    I.executeScript(id => {
        webpagesDatatable.rows().deselect();
        webpagesDatatable.row('#' + id).select();
    }, pageIds[0]);
    I.clickCss(menu);
    I.clickCss('.dt-more-actions-menu .buttons-stat');
    I.switchToNextTab();
    I.seeInCurrentUrl('/apps/stat/admin/top-details/?docId=' + pageIds[0]);
    I.waitForElement('div#topDetails-lineVariantA');
    I.closeCurrentTab();
});

Scenario('Actions respect pageSave permission and tab visibility', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67&removePerm=pageSave');
    DT.waitForLoader();
    I.clickCss(menu);
    I.dontSeeElement('.buttons-addPerexGroup');
    I.dontSeeElement('.buttons-removePerexGroup');
    I.dontSeeElement('.buttons-addUserGroup');
    I.dontSeeElement('.buttons-removeUserGroup');
    I.pressKey('Escape');
    const response = await I.executeScript(async ({id, groupId}) => $.post({
        url: '/admin/rest/web-pages/action/addPerexGroup',
        data: { ids: [id], customData: groupId }
    }), { id: pageIds[0], groupId: addedGroupId });
    I.assertContain(response.error, 'Nemáte právo na editáciu web stránky');
    I.clickCss('#pills-waiting-tab');
    DT.waitForLoader();
    I.clickCss(menu);
    I.see('Štatistika stránky', '.dt-more-actions-menu');
    I.dontSeeElement('.dt-more-actions-menu .buttons-abtest');
    I.dontSeeElement('.dt-more-actions-menu .buttons-linkcheck');
    I.dontSeeElement('.dt-more-actions-menu .buttons-addPerexGroup');
    I.dontSeeElement('.dt-more-actions-menu .buttons-addUserGroup');
    I.dontSeeElement('.dt-more-actions-menu .buttons-removeUserGroup');
    I.pressKey('Escape');
    I.executeScript(() => webpagesDatatable.hideButton('stat'));
    I.dontSeeElement(menu);
    I.executeScript(() => webpagesDatatable.showButton('stat'));
    I.clickCss('#pills-folders-tab');
    DT.waitForLoader();
    I.seeElement(menu);
    I.logout();
});

Scenario('Menu fits narrow windows and returns keyboard focus', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    for (const width of [1202, 1190, 700]) {
        I.resizeWindow(width, 900);
        I.clickCss(menu);
        const bounds = await I.executeScript(() => {
            const menu = document.querySelector('.dt-more-actions-menu').getBoundingClientRect();
            return { left: menu.left, right: menu.right, viewport: window.innerWidth };
        });
        I.assertTrue(bounds.left >= 0 && bounds.right <= bounds.viewport, 'The menu must fit the viewport');
        if (width === 1202) I.saveScreenshot('perex-bulk-more-actions.png');
        I.pressKey('Tab');
        I.pressKey('Escape');
        I.waitToHide('.dt-more-actions-menu');
        I.assertTrue(await I.executeScript(() => document.activeElement.matches('button.buttons-more-actions')));
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Delete created pages', ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.filterContains('title', name);
    DT.deleteAll();
    I.amOnPage('/admin/v9/webpages/perex/');
    DT.filterContains('perexGroupName', name);
    DT.deleteAll('perexDataTable');
});

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
