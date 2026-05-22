Feature('settings.custom-fields');

Before(({ I, login }) =>{
    login('admin');
});

const docId_1 = "163799";
const docId_2 = "163800";
const docClass = "sk.iway.iwcm.doc.DocDetails";

const templateId = "4319";
const templateClass = "sk.iway.iwcm.doc.TemplateDetails";

Scenario('Custom fields required logic test @screenshot', async ({ I, DT, DTE, Document }) =>{
    I.say("Do some basic checks");
    I.amOnPage("/admin/v9/settings/custom-fields/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("customFieldsDataTable");

    I.say("Test autocomplete for Entities");
    I.fillField("#DTE_Field_className", "doc");
    I.waitForVisible("ul.dt-autocomplete-select");
    I.seeElement( locate("ul.dt-autocomplete-select li.ui-menu-item div").withText("sk.iway.iwcm.components.forum.jpa.DocForumEntity") );
    I.seeElement( locate("ul.dt-autocomplete-select li.ui-menu-item div").withText(docClass) );
    I.seeElement( locate("ul.dt-autocomplete-select li.ui-menu-item div").withText("sk.iway.iwcm.doc.DocHistory") );

    I.say("Go test DOC entiy - not fields required");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId_1);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");
    DTE.save();

    I.say("Same for second page");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId_2);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");
    DTE.save();

    I.say("Now set 3 global rules for DocDetails entity - fields required");
    I.amOnPage("/admin/v9/settings/custom-fields/");
    // add small / big alphabet, it should proceed correctly
    addCustomFieldSetting(I, DTE, docClass, "G", null, true);
    addCustomFieldSetting(I, DTE, docClass, "h", null, true);
    addCustomFieldSetting(I, DTE, docClass, "I", null, true);

    checkDocCustomFields(I, DTE, docId_1, ["G", "H", "I"], ["F", "J"]);
    checkDocCustomFields(I, DTE, docId_2, ["G", "H", "I"], ["F", "J"]);

    I.say("Now test duplicity check");
    I.amOnPage("/admin/v9/settings/custom-fields/");
    addCustomFieldSetting(I, DTE, docClass, "g", null, false, "Kombinácia polí „Použiť pre entitu“, „Označenie poľa“ a „ID entity“ musí byť jedinečná. Zadajte inú hodnotu pre jedno z týchto polí.");

    I.say("Try save it but for specific entityId - should pass");
    I.fillField("#DTE_Field_entityId", docId_1);
    DTE.save();
    I.dontSeeElement(".DTE_Form_Error");

    I.say('Allso for another entityId');
    addCustomFieldSetting(I, DTE, docClass, "I", docId_2, false);

    I.say("Now test how doc's fields changed");
    checkDocCustomFields(I, DTE, docId_1, ["H", "I"], ["F", "G", "J"]);
    checkDocCustomFields(I, DTE, docId_2, ["G", "H"], ["F", "J", "I"]);

    I.say("DocDetails are SPECIAL they custom fields logic can be ovewrite by TEMPLATES logic");
    I.amOnPage("/admin/v9/settings/custom-fields/");
    addCustomFieldSetting(I, DTE, templateClass, "f", templateId, true);
    addCustomFieldSetting(I, DTE, templateClass, "g", templateId, true);

    Document.screenshot("/frontend/webpages/customfields/custom-fields-settings-datatable.png");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("customFieldsDataTable");

    Document.screenshotElement("div.DTE_Action_Create", "/frontend/webpages/customfields/custom-fields-settings-editor.png");

    I.say("GO check that only DOC with thi TEMP is affected - bonus TEMP has highest priority");
    checkDocCustomFields(I, DTE, docId_1, ["F", "G", "H", "I"], ["J"]);
    checkDocCustomFields(I, DTE, docId_2, ["G", "H"], ["F", "J", "I"]);
});

Scenario('Custom fields required logic test - AFTER', async ({ I, DT, DTE }) =>{
    I.say('Return it back to basic state - REMOVE added settings');

    I.amOnPage("/admin/v9/settings/custom-fields/");
    DT.filterEquals("className", docClass);

    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    DT.filterEquals("className", templateClass);
    I.fillField("input.dt-filter-from-entityId", templateId);
    I.fillField("input.dt-filter-to-entityId", templateId);
    I.clickCss("button.dt-filtrujem-entityId");
    DT.waitForLoader();

    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

function checkDocCustomFields(I, DTE, docId, requiredFields, notRequiredFields) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-fields-tab");

    DTE.save();

    requiredFields.forEach(requiredField => {
        I.seeElement( locate("div.DTE_Field_Name_field" + requiredField + " div.form-text.text-danger").withText("Voliteľné pole je nastavené ako povinné.") );
    });

    notRequiredFields.forEach(notRequiredField => {
        I.dontSeeElement( locate("div.DTE_Field_Name_field" + notRequiredField + " div.form-text.text-danger").withText("Voliteľné pole je nastavené ako povinné.") );
    });
}

function addCustomFieldSetting(I, DTE, className, alphabet, entityId, isRequired, seeError = null) {
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("customFieldsDataTable");
    I.fillField("#DTE_Field_className", className);
    I.fillField("#DTE_Field_alphabet", alphabet);

    if(entityId !== null) {
        I.fillField("#DTE_Field_entityId", entityId);
    }

    if(isRequired === true) {
        I.checkOption("#DTE_Field_required_0");
    } else {
        I.uncheckOption("#DTE_Field_required_0");
    }

    DTE.save();

    if(seeError === null) {
        I.dontSeeElement(".DTE_Form_Error");
    } else {
        I.see(seeError);
    }
}