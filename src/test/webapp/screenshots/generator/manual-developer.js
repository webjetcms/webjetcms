Feature('manual-developer');

Before(({ I, login }) => {
    login('admin');
});

Scenario('field-json', async({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/apps/insert-script/");
    I.click("Testovaci 1x", "#insertScriptTable");
    DTE.waitForEditor("insertScriptTable");
    I.click("#pills-dt-insertScriptTable-scriptPerms-tab");

    I.executeScript(function() {
        $('div.DTE_Action_Edit div.DTE_Field_Type_json').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Type_json').css("padding-bottom", "10px");
    });

    Document.screenshotElement("div.DTE_Field_Name_groupIds", "/developer/datatables-editor/field-json-group-array.png");
    Document.screenshotElement("div.DTE_Field_Name_docIds", "/developer/datatables-editor/field-json-page-array.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=31");

    I.click("div.tree-col button.buttons-edit");
    DTE.waitForEditor("groups-datatable");

    I.executeScript(function() {
        $('div.DTE_Action_Edit div.DTE_Field_Type_json').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Type_json').css("padding-bottom", "10px");
    });

    Document.screenshotElement("div.DTE_Field_Name_editorFields\\.parentGroupDetails", "/developer/datatables-editor/field-json-group.png");
    Document.screenshotElement("div.DTE_Field_Name_editorFields\\.defaultDocDetails", "/developer/datatables-editor/field-json-page.png");

    I.amOnPage("/admin/v9/users/user-groups/");
    I.click("VIP Klienti");
    DTE.waitForEditor("userGroupsDataTable");

    I.executeScript(function() {
        $('div.DTE_Action_Edit div.DTE_Field_Type_json').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Type_json').css("padding-bottom", "10px");
    });

    Document.screenshotElement("div.DTE_Field_Name_emailDoc", "/developer/datatables-editor/field-json-page-null.png");
});


Scenario('datatable-opener', async({ I, Document }) => {
    I.amOnPage("/apps/qa/admin/");
    I.fillField("#datatable-qaDataTable-id", "38");

    Document.screenshotElement("div.dt-header-row.clearfix", "/developer/libraries/datatable-opener.png");
});

Scenario('datatable-notify', async({ I, DT, DTE, Document }) => {

    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
    DT.waitForLoader();

    I.jstreeNavigate(["English", "News"]);

    //Vyfiltruj si zaznam
    DT.filter("title", "Test");

    //Edituj stranku
    I.click("Test");
    DTE.waitForEditor();

    I.moveCursorTo("div.toast-title");
    I.wait(5);

    Document.screenshot("/developer/datatables-editor/notify.png", 1360, 460);
});

Scenario('field-select-editable', async({ I, DTE, Document }) => {

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=23485');
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-template-tab");
    Document.screenshot("/developer/datatables-editor/field-select.png", 1280, 400)
    Document.screenshotElement("div.DTE_Field_Name_tempId .btn-edit", "/developer/datatables-editor/field-select-icon-edit.png")
    Document.screenshotElement("div.DTE_Field_Name_tempId .btn-add", "/developer/datatables-editor/field-select-icon-add.png")

    I.click("div.DTE_Field_Name_tempId .btn-edit");
    I.wait(5);

    Document.screenshot("/developer/datatables-editor/field-select-editable.png")

});

Scenario('appstore', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=27032");
    DTE.waitForEditor();
    I.wait(6);

    I.click("a.cke_button__components");

    Document.screenshot("/custom-apps/appstore/appstore.png");

    //demo komponenta
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.click("#demo-komponenta");
    I.wait(3);

    Document.screenshot("/custom-apps/appstore/democomponent-desc.png");
    I.click("Vložiť do stránky");
    I.wait(6);

    Document.screenshot("/custom-apps/appstore/democomponent-prop.png");


    //Kontakty
    I.switchTo();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=27032");
    DTE.waitForEditor();
    I.wait(6);

    I.click("a.cke_button__components");

    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.click("#kontakty");
    I.wait(3);
    I.click("Vložiť do stránky");
    I.wait(6);

    Document.screenshot("/custom-apps/appstore/contacts-prop.png");

});

Scenario('allure', ({ I, Document }) => {
    I.amOnPage("http://docs.webjetcms.sk/allure/chromium/");

    Document.screenshot("/developer/testing/allure-overview.png");
});