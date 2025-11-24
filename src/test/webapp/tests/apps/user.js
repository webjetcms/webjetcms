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
        groupIds: "",
        show: "login+password+password2+firstName+lastName+email+sexMale+adress+city+zip+country",
        required: "login+password+password2+firstName+lastName+email",
        groupIdsEditable: "",
        emailUnique: "false",
        successDocId: "-1",
        infoemail: "",
        requireEmailVerification: "false",
        notAuthorizedEmailDocId: "-1",
        loginNewUser: "false",
        useAjax: "true"
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText("Úprava údajov používateľa", 10);
    ["Prihlasovacie meno", "Heslo", "Staré heslo", "Meno", "Priezvisko", "E-mail", "Pohlavie", "Ulica", "Mesto", "PSČ", "Krajina"].forEach(field => I.see(field));
    I.dontSee("Bankári");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        groupIds: "802",
        show: "login+password+password2+firstName+lastName+company+email+sexMale+adress+city+zip+country+fieldA",
        required: "login+password+password2+firstName+lastName+company+email",
        groupIdsEditable: "4",
        emailUnique: "true",
        successDocId: "141",
        infoemail: "webjeteditor@fexpost.com",
        requireEmailVerification: "false",
        notAuthorizedEmailDocId: "141",
        loginNewUser: "true",
        useAjax: "true"
    };

    I.say("Check change of structure based on FIELD");
        DTE.selectOption('field', 'Prihlasovací formulár');
        I.dontSeeElement("#DTE_Field_groupIds");
        I.dontSeeElement("#DTE_Field_infoemail");
        I.dontSeeElement("#DTE_Field_emailUnique_0");

        DTE.selectOption('field', 'Meno');
        I.dontSeeElement("#DTE_Field_groupIds");
        I.dontSeeElement("#DTE_Field_infoemail");
        I.dontSeeElement("#DTE_Field_emailUnique_0");

    I.say("Go back to default form");
    DTE.selectOption('field', 'Registračný formulár');
    I.seeElement("#DTE_Field_groupIds");
    I.seeElement("#DTE_Field_infoemail");
    I.seeElement("#DTE_Field_emailUnique_0");

    I.say("Set changed parameters");
    multiselectOption(I, "groupIds", ["Blog"]);

    //advanced tab
    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    multiselectOption(I, "groupIdsEditable", ["Bankári"]);
    I.clickCss("#editorAppDTE_Field_successDocId > section > div > div > div > div > button");
    I.click(locate('.jstree-node.jstree-closed').withText('Jet portal 4').find('.jstree-icon.jstree-ocl'));
    I.clickCss('#docId-141_anchor');
    I.waitForElement('input[value="/Jet portal 4/Jet portal 4 - testovacia stranka"]', 10);

    I.clickCss("#editorAppDTE_Field_notAuthorizedEmailDocId > section > div > div > div > div > button");
    I.click(locate('.jstree-node.jstree-closed').withText('Jet portal 4').find('.jstree-icon.jstree-ocl'));
    I.clickCss('#docId-141_anchor');
    I.waitForElement('input[value="/Jet portal 4/Jet portal 4 - testovacia stranka"]', 10);

    I.checkOption("#DTE_Field_loginNewUser_0");
    I.checkOption("#DTE_Field_useAjax_0");

    //basic tab
    I.clickCss("#pills-dt-component-datatable-basic-tab");

    I.checkOption("#DTE_Field_emailUnique_0");

    DTE.fillField("infoemail", "webjeteditor@fexpost.com");
    I.uncheckOption("#DTE_Field_requireEmailVerification_0", )


    I.say("Add showed and required fields");
        I.clickCss("#pills-dt-component-datatable-showed-tab");
        I.click( locate(".form-check-label").withText("Firma") );
        I.click( locate(".form-check-label").withText("Pole A") );

        I.clickCss("#pills-dt-component-datatable-required-tab");
        I.click( locate(".form-check-label").withText("Firma") );

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