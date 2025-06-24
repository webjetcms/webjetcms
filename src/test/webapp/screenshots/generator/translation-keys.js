Feature('settings.translation_keys');

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/v9/settings/translation-keys/");
});

Scenario('translation keys', async ({ I, DTE, Document }) => {

    I.click("button.btn-import-dialog");
    I.waitForElement("#datatableImportModal");
    I.clickCss("label[for=dt-settings-import4]");
    Document.screenshot("/admin/settings/translation-keys/dataTable-import.png", 1280, 500);
    I.clickCss("#datatableImportModal .modal-footer button.btn-outline-secondary");

    var entityName = "editor.disableAfterEnd";
    var entityName2 = "admin.fck.last_pages";

    I.fillField("input.dt-filter-key", entityName);
    I.pressKey('Enter', "input.dt-filter-key");
    I.wait(5);

    //Translation keys data table
    Document.screenshot("/admin/settings/translation-keys/dataTable.png");

    I.click(entityName);
    DTE.waitForEditor();
    Document.screenshotElement("#datatableInit_modal > div > div", "/admin/settings/translation-keys/dataTable_edit.png");
    I.wait(2);

    I.clickCss("#datatableInit_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-outline-secondary.btn-close-editor");

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor();
    Document.screenshotElement("div.DTE_Action_Create", "/admin/settings/translation-keys/dataTable_create.png");
    I.wait(2);

    I.clickCss("#datatableInit_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-outline-secondary.btn-close-editor");

    I.fillField("input.dt-filter-key", entityName2);
    I.pressKey('Enter', "input.dt-filter-key");
    I.wait(5);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");

    switch (I.getConfLng()) {
        case "sk":
            I.click("Zmazať", "div.DTE_Action_Remove");
            break;
        case "en":
            I.click("Delete", "div.DTE_Action_Remove");
            break;
        case "cs":
            I.click("Smazat", "div.DTE_Action_Remove");
            break;
        default:
            throw new Error("Unknown language: " + I.getConfLng());
    }

    I.wait(1);

    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/admin/settings/translation-keys/delete-notification.png");
});