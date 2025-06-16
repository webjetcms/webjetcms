Feature('apps.user');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Pouzivatelia', async ({ I, Apps, Document, DTE }) => {
    Apps.insertApp('Používatelia', '#menu-users');

    const defaultParams = {
        userGroups: "",
        show: "login+password+password2+firstName+lastName+email",
        required: "login+password+password2+firstName+lastName+email+city+zip+country",
        groupIdsEditable: "",
        emailUnique: "false",
        successDocId: "-1",
        infoemail: "",
        requireEmailVerification: "false",
        notAuthorizedEmailDocId: "-1",
        loginNewUser: "false",
        useAjax: "false",
        useCustomFields: "false",
        useCustomFieldA: "false",
        fieldALabel: "",
        useCustomFieldB: "false",
        fieldBLabel: "",
        useCustomFieldC: "false",
        fieldCLabel: "",
        useCustomFieldD: "false",
        fieldDLabel: "",
        useCustomFieldE: "false",
        fieldELabel: "",
        regToUserGroups: ""
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText("Úprava údajov používateľa", 10);
    ["Prihlasovacie meno", "Heslo", "Staré heslo", "Meno", "Priezvisko", "E-mail"].forEach(field => I.see(field));
    I.dontSee("Bankári");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        userGroups: "802",
        show: "login+password+password2+firstName+lastName+company+email",
        required: "login+password+password2+firstName+lastName+company+email+city+zip+country",
        groupIdsEditable: "4",
        emailUnique: "true",
        successDocId: "141",
        infoemail: "webjeteditor@fexpost.com",
        requireEmailVerification: "false",
        notAuthorizedEmailDocId: "141",
        loginNewUser: "true",
        useAjax: "true",
        useCustomFields: "true",
        useCustomFieldA: "true",
        fieldALabel: "Pole A",
        useCustomFieldB: "false",
        fieldBLabel: "",
        useCustomFieldC: "false",
        fieldCLabel: "",
        useCustomFieldD: "false",
        fieldDLabel: "",
        useCustomFieldE: "false",
        fieldELabel: "",
        regToUserGroups: ""
    };

    multiselectOption(I, "userGroups", ["Blog"])
    multiselectOption(I, "show", ["Firma"])
    multiselectOption(I, "required", ["Firma"])
    multiselectOption(I, "groupIdsEditable", ["Bankári"])
    I.checkOption("#DTE_Field_emailUnique_0");
    I.clickCss("#editorAppDTE_Field_successDocId > section > div > div > div > div > button");
    I.click(locate('.jstree-node.jstree-closed').withText('Jet portal 4').find('.jstree-icon.jstree-ocl'));
    I.clickCss('#docId-141_anchor');
    I.waitForElement('input[value="/Jet portal 4/Jet portal 4 - testovacia stranka"]', 10);
    DTE.fillField("infoemail", "webjeteditor@fexpost.com");
    I.uncheckOption("#DTE_Field_requireEmailVerification_0", )
    I.clickCss("#editorAppDTE_Field_notAuthorizedEmailDocId > section > div > div > div > div > button");
    I.click(locate('.jstree-node.jstree-closed').withText('Jet portal 4').find('.jstree-icon.jstree-ocl'));
    I.clickCss('#docId-141_anchor');
    I.waitForElement('input[value="/Jet portal 4/Jet portal 4 - testovacia stranka"]', 10);
    I.checkOption("#DTE_Field_loginNewUser_0");
    I.checkOption("#DTE_Field_useAjax_0");
    I.checkOption("#DTE_Field_useCustomFields_0");
    I.checkOption("#DTE_Field_useCustomFieldA_0");
    I.fillField("#DTE_Field_fieldALabel", "Pole A");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.see("Úprava údajov používateľa");
    ["Prihlasovacie meno", "Heslo", "Staré heslo", "Meno", "Priezvisko", "E-mail", "Pole A", "Firma"].forEach(field => I.see(field));
    I.see("Bankári");
});

function multiselectOption(I, name, options){
    I.clickCss(`//div[./*[@id="DTE_Field_${name}"]]`);
    I.wait(1);
    options.forEach(option => I.click(locate("a[role=option]").withText(option)));
    I.pressKey('Enter');
}