Feature('apps.app-formsimple');

const applicationName = "Formulár ľahko";
const applicationSelector = "#components-formsimple-title";
const formSimpleFolder = 22704;

let randomNumber;
let newPageName;

const allItemFields = ["#DTE_Field_required_0", "#DTE_Field_label", "#DTE_Field_value", "#DTE_Field_placeholder", "#DTE_Field_tooltip"];

Before(({ I, DT, login }) => {
    login('admin');
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
        newPageName = `formSimple_${randomNumber}_autotest`;
    }

    DT.addContext("simpleformItems", "#datatableFieldDTE_Field_editorData_wrapper");
});

Scenario("Form simple - check defautl values on empty page", async ({ I, DT, DTE, Apps }) => {
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${formSimpleFolder}`);
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.fillField("#DTE_Field_title", "Test name - do not care");

    I.say("Open app");
        Apps.openAppForInsert(applicationName, applicationSelector);

    I.say("Check default values");
        // We set page name BUT because page is new, BE set default formName and subject
        I.assertContain(await I.grabValueFrom("#DTE_Field_formName"), applicationName);
        I.assertContain(await I.grabValueFrom("#DTE_Field_attribute_recipients"), "tester@balat.sk");
        I.dontSeeCheckboxIsChecked("#DTE_Field_attribute_forceTextPlain_0");
        I.seeCheckboxIsChecked("#DTE_Field_attribute_addTechInfo_0");
        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        I.assertEqual(await I.grabValueFrom("#DTE_Field_attribute_forwardType"), "");
});

Scenario("Form simple - test insert process", async ({ I, DT, DTE, Apps }) => {
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${formSimpleFolder}`);
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.fillField("#DTE_Field_title", newPageName);

    I.say("Open app");
        Apps.openAppForInsert(applicationName, applicationSelector);

    I.say("Prepare app for insert - tab basic");
        I.fillField("#DTE_Field_formName", newPageName);
        I.fillField("#DTE_Field_attribute_emailTextBefore", "email text before");
        I.uncheckOption("#DTE_Field_attribute_forceTextPlain_0");

    I.say("Prepare app for insert - tab advanced");
        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        I.fillField("#DTE_Field_attribute_subject", newPageName);
        I.fillField("#DTE_Field_attribute_ccEmails", "tester@balat.sk");
        I.fillField(locate(".DTE_Field_Name_attribute_forward").find("input.form-control"), "/images/gallery/chrysanthemum.jpg");

        I.click( locate("#editorAppDTE_Field_attribute_useFormMailDocId").find("button.btn-vue-jstree-item-edit") );
        I.waitForVisible("#jsTree");
        I.click(locate('.jstree-node.jstree-closed').withText('Jet portal 4').find('.jstree-icon.jstree-ocl'));
        I.click( locate(".jstree-anchor").withText('Jet portal 4 - testovacia stranka') );

    I.say("Prepare app for insert - tab items");
    I.say("Check its empty - leave it empty");
        I.clickCss("#pills-dt-component-datatable-items-tab");
        I.see("Nenašli sa žiadne vyhovujúce záznamy", "table#datatableFieldDTE_Field_editorData");

    savingApp(I);

    await checkPageParams(Apps, newPageName, "false", "JTVCJTVE");

    I.say("Check, other params aer not there");
        Apps.switchEditor('html');
        I.dontSee("attribute_subject");
        I.dontSee("attribute_ccEmails");
        I.dontSee("attribute_emailTextBefore");
        // etc. ...

    I.say('Save page');
        DTE.save();
});

Scenario("Form simple - test update process", async ({ I, DT, DTE, Apps }) => {
    openCreatedApp(I, DT, DTE, Apps);

    I.say("Check and edit values - basic tab");
        I.seeInField("#DTE_Field_formName", newPageName);
        I.seeInField("#DTE_Field_attribute_recipients", "tester@balat.sk");
        I.seeInField("#DTE_Field_attribute_emailTextBefore", "email text before");
        I.dontSeeCheckboxIsChecked("#DTE_Field_attribute_forceTextPlain_0");
        I.seeCheckboxIsChecked("#DTE_Field_attribute_addTechInfo_0");

        I.fillField("#DTE_Field_formName", newPageName + "_changed");
        I.fillField("#DTE_Field_attribute_emailTextBefore", "email text before - changed");
        I.fillField("#DTE_Field_attribute_emailTextAfter", "email text agter");
        I.uncheckOption("#DTE_Field_attribute_addTechInfo_0");

    I.say("Check and edit values - advanced tab");
        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        I.seeInField("#DTE_Field_attribute_ccEmails", "tester@balat.sk");
        I.seeInField("#DTE_Field_attribute_subject", newPageName);
        I.seeInField(locate(".DTE_Field_Name_attribute_forward").find("input.form-control"), "/images/gallery/chrysanthemum.jpg");
        I.seeInField(locate("#editorAppDTE_Field_attribute_useFormMailDocId").find("input.form-control"), "/Jet portal 4/Jet portal 4 - testovacia stranka");
        I.click(locate("#editorAppDTE_Field_attribute_useFormMailDocId").find("button.btn-vue-jstree-item-remove"));

    I.say("Check and edit values - items tab");
    I.say("Check its empty - leave it empty");
        I.clickCss("#pills-dt-component-datatable-items-tab");
        I.see("Nenašli sa žiadne vyhovujúce záznamy", "table#datatableFieldDTE_Field_editorData");

    savingApp(I);

    await checkPageParams(Apps, newPageName + "_changed", "false", "JTVCJTVE");

    I.say('Check, that changed values are saved');
        Apps.openCurrentAppEditor("component-datatable_modal", false);
        I.seeInField("#DTE_Field_formName", newPageName + "_changed");
        I.seeInField("#DTE_Field_attribute_emailTextBefore", "email text before - changed");
        I.dontSeeCheckboxIsChecked("#DTE_Field_attribute_addTechInfo_0");
        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        I.dontSeeInField(locate("#editorAppDTE_Field_attribute_useFormMailDocId").find("input.form-control"), "/Jet portal 4/Jet portal 4 - testovacia stranka");
        I.switchTo();
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_cancel');

    I.say('Save page');
        DTE.save();
});

Scenario("Form simple - test items inner table", async ({ I, DT, DTE, Apps }) => {
    openCreatedApp(I, DT, DTE, Apps);
    I.clickCss("#pills-dt-component-datatable-items-tab");
    I.see("Nenašli sa žiadne vyhovujúce záznamy", "table#datatableFieldDTE_Field_editorData");

    I.say('Check fields visibility in editor by field type');
        I.click(DT.btn.simpleformItems_add_button);
        DTE.waitForEditor("datatableFieldDTE_Field_editorData");

        checkItemFieldsVisibility(I, "Adresa", allItemFields);
        checkItemFieldsVisibility(I, "Captcha", []);
        checkItemFieldsVisibility(I, "Medzera", []);
        checkItemFieldsVisibility(I, "Nahrať obrázky", ["#DTE_Field_label", "#DTE_Field_value", "#DTE_Field_tooltip"]);

    I.say("Add item");
        changeItemType(I, "Adresa");
        I.clickCss("#DTE_Field_label > div.editor.ql-container > div.ql-editor");
        I.fillField('#DTE_Field_label div.ql-editor', "Label field");
        I.checkOption("#DTE_Field_required_0");
        advancedTab(I);
        I.fillField("#DTE_Field_value", "Value field");

        DTE.save("datatableFieldDTE_Field_editorData");

    I.say("Check and edit item");
        I.click(locate("#datatableFieldDTE_Field_editorData tr").withAttr({ id: "1" }).find(".dt-select-td"));
        I.click(DT.btn.simpleformItems_edit_button);
        I.seeInField('#DTE_Field_label div.ql-editor', "Label field");
        I.seeCheckboxIsChecked("#DTE_Field_required_0");

        advancedTab(I);
        I.seeInField("#DTE_Field_value", "Value field");
        I.clickCss("#DTE_Field_tooltip > div.editor.ql-container > div.ql-editor");
        I.fillField('#DTE_Field_tooltip div.ql-editor', "This is tooltip");
        I.fillField('#DTE_Field_placeholder', "Placeholder field");
        DTE.save("datatableFieldDTE_Field_editorData");

    I.say('Add 2 more fields');
        I.click(DT.btn.simpleformItems_add_button);
        DTE.waitForEditor("datatableFieldDTE_Field_editorData");
        changeItemType(I, "Medzera");
        DTE.save("datatableFieldDTE_Field_editorData");

        I.click(DT.btn.simpleformItems_add_button);
        DTE.waitForEditor("datatableFieldDTE_Field_editorData");
        changeItemType(I, "Captcha");
        DTE.save("datatableFieldDTE_Field_editorData");

    I.say('Check items table');
        DT.checkTableRow("datatableFieldDTE_Field_editorData", 1, ["1", "10", "Adresa"]);
        DT.checkTableRow("datatableFieldDTE_Field_editorData", 2, ["2", "20", "Medzera"]);
        DT.checkTableRow("datatableFieldDTE_Field_editorData", 3, ["3", "30", "Captcha"]);

    savingApp(I);

    await checkPageParams(Apps, newPageName + "_changed", "false", "JTVCJTdCJTIyZmllbGRUeXBlJTIyOiUyMmFkcmVzYSUyMiwlMjJyZXF1aXJlZCUyMjolNUJ0cnVlJTVELCUyMmxhYmVsJTIyOiUyMiUzQ3AlM0VMYWJlbCUyMGZpZWxkJTNDL3AlM0UlMjIsJTIydmFsdWUlMjI6JTIyVmFsdWUlMjBmaWVsZCUyMiwlMjJwbGFjZWhvbGRlciUyMjolMjJQbGFjZWhvbGRlciUyMGZpZWxkJTIyLCUyMnRvb2x0aXAlMjI6JTIyJTNDcCUzRVRoaXMlMjBpcyUyMHRvb2x0aXAlM0MvcCUzRSUyMiU3RCwlN0IlMjJmaWVsZFR5cGUlMjI6JTIybWVkemVyYSUyMiwlMjJyZXF1aXJlZCUyMjolNUJmYWxzZSU1RCwlMjJsYWJlbCUyMjolMjIlMjIsJTIydmFsdWUlMjI6JTIyJTIyLCUyMnBsYWNlaG9sZGVyJTIyOiUyMiUyMiwlMjJ0b29sdGlwJTIyOiUyMiUyMiU3RCwlN0IlMjJmaWVsZFR5cGUlMjI6JTIyY2FwdGNoYSUyMiwlMjJyZXF1aXJlZCUyMjolNUJmYWxzZSU1RCwlMjJsYWJlbCUyMjolMjIlMjIsJTIydmFsdWUlMjI6JTIyJTIyLCUyMnBsYWNlaG9sZGVyJTIyOiUyMiUyMiwlMjJ0b29sdGlwJTIyOiUyMiUyMiU3RCU1RA==");

    I.say('Save page');
        DTE.save();
});

Scenario("Form simple - test items inner table 2", async ({ I, DT, DTE, Apps }) => {
    I.say("Check items are there after save");
        openCreatedApp(I, DT, DTE, Apps);
        I.clickCss("#pills-dt-component-datatable-items-tab");
        DT.checkTableRow("datatableFieldDTE_Field_editorData", 1, ["1", "10", "Adresa"]);
        DT.checkTableRow("datatableFieldDTE_Field_editorData", 2, ["2", "20", "Medzera"]);
        DT.checkTableRow("datatableFieldDTE_Field_editorData", 3, ["3", "30", "Captcha"]);

    I.say("Delete item, save app, anch check editorData");
        I.click(locate("#datatableFieldDTE_Field_editorData tr").withAttr({ id: "1" }).find(".dt-select-td"));
        I.click(DT.btn.simpleformItems_delete_button);
        I.waitForVisible(".DTE.modal-content.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader("datatableFieldDTE_Field_editorData");

    savingApp(I);

    await checkPageParams(Apps, newPageName + "_changed", "false", "JTVCJTdCJTIyZmllbGRUeXBlJTIyOiUyMm1lZHplcmElMjIsJTIycmVxdWlyZWQlMjI6JTVCZmFsc2UlNUQsJTIybGFiZWwlMjI6JTIyJTIyLCUyMnZhbHVlJTIyOiUyMiUyMiwlMjJwbGFjZWhvbGRlciUyMjolMjIlMjIsJTIydG9vbHRpcCUyMjolMjIlMjIlN0QsJTdCJTIyZmllbGRUeXBlJTIyOiUyMmNhcHRjaGElMjIsJTIycmVxdWlyZWQlMjI6JTVCZmFsc2UlNUQsJTIybGFiZWwlMjI6JTIyJTIyLCUyMnZhbHVlJTIyOiUyMiUyMiwlMjJwbGFjZWhvbGRlciUyMjolMjIlMjIsJTIydG9vbHRpcCUyMjolMjIlMjIlN0QlNUQ=");

    DTE.cancel();
});

Scenario("Remove page", ({ I, DT, DTE }) => {
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${formSimpleFolder}`);
    DT.filterEquals("title", newPageName);
    I.clickCss("td.sorting_1");
    I.click(DT.btn.delete_button);
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
});

async function checkPageParams(Apps, formName, rowView, editorData) {
    const params = {
        formName: formName,
        rowView: rowView,
        editorData: editorData
    };

    await Apps.assertParams(params, "/components/formsimple/form.jsp");
}

function savingApp(I) {
    I.say('Save app');
        I.switchTo();
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');
}

function openCreatedApp(I, DT, DTE, Apps) {
    I.say("Open app in page");
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${formSimpleFolder}`);
        DT.filterEquals("title", newPageName);
        I.clickCss("td.sorting_1");
        I.click(DT.btn.edit_button);

        DTE.waitForEditor();
        DTE.waitForCkeditor();

        Apps.openCurrentAppEditor("component-datatable_modal", false);
}

function changeItemType(I, fieldType) {
    basicTab(I);
    I.clickCss("button[data-id='DTE_Field_fieldType']");
    I.waitForVisible("div.dropdown-menu.show");
    I.click( locate("div.dropdown-menu.show").find( locate("a.dropdown-item").withText(fieldType) ) );
    I.waitForInvisible("div.dropdown-menu.show");
}

function checkItemFieldsVisibility(I, fieldType, visibleFields) {
    I.say(`Checking item fields for type: ${fieldType}`);
        changeItemType(I, fieldType);
        allItemFields.forEach(field => {
            if ("#DTE_Field_value"===field || "#DTE_Field_placeholder"===field || "#DTE_Field_tooltip"===field) {
                //value field is not visible for upload images type
                advancedTab(I);
            } else {
                basicTab(I);
            }

            if (visibleFields.includes(field)) {
                I.seeElement(field);
            } else {
                I.dontSeeElement(field);
            }
        });
}

function basicTab(I) {
    I.clickCss("#pills-dt-datatableFieldDTE_Field_editorData-basic-tab");
}
function advancedTab(I) {
    I.clickCss("#pills-dt-datatableFieldDTE_Field_editorData-advanced-tab");
}