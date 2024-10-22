Feature('admin.audit-log-levels');

var randomNumber;
var defaultLogLevel = "Hlavná úroveň logovania";

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Logic tests basic log level", async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/apps/audit-log-levels/");

    let newLogLevel = "log_level_" + randomNumber + "-autotest";
    I.say("Test create new log level");
        I.click("button.buttons-create");
        DTE.waitForEditor();
        I.fillField("#DTE_Field_packageName", newLogLevel);

        I.say("Check, that we dont see option NORMAL in dropdown list");
        I.click( locate("div.DTE_Field_Name_logLevel").find("button.dropdown-toggle") );
        I.waitForElement("div.dropdown-menu.show");

        I.seeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("NORMAL") ) );

        I.dontSeeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("DEBUG") ) );
        I.dontSeeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("ERROR") ) );
        I.dontSeeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("INFO") ) );
        I.dontSeeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("TRACE") ) );
        I.dontSeeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("WARN") ) );

        I.click( locate("div.DTE_Field_Name_logLevel").find("button.dropdown-toggle") );

        selectEditorLogLevel(I, "INFO");
        DTE.save();

    I.say("Check it");
        checkValue(I, DT, newLogLevel, "INFO");

    I.say("Change value");
        I.click(newLogLevel);
        DTE.waitForEditor();
        selectEditorLogLevel(I, "WARN");
        DTE.save();

    I.say("Check it");
        checkValue(I, DT, newLogLevel, "WARN");

    I.say("Create with same package name - should perform EDIT");
        I.click("button.buttons-create");
        DTE.waitForEditor();
        I.fillField("#DTE_Field_packageName", newLogLevel);
        selectEditorLogLevel(I, "ERROR");
        DTE.save();

    I.say("Check it");
        DT.filter("packageName", newLogLevel);
        const rowsA = await I.grabNumberOfVisibleElements("div.dataTables_scrollBody > table.datatableInit > tbody > tr");
        I.assertEqual(rowsA, 1, "Create with existing package name should perform EDIT. There can be only one row with same package name.");

    I.say('Delete');
        I.clickCss("td.dt-select-td.sorting_1");
        I.clickCss("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.filter("packageName", newLogLevel);
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario("Logic tests DEFAULT log level", async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/apps/audit-log-levels/");

    I.say("DEFAULT log level must always be present");
        checkValue(I, DT, defaultLogLevel, null);

    I.say("Edit DEFAULT log level");
        I.click(defaultLogLevel);
        DTE.waitForEditor();

        I.say("Check that we have only 2 options - INFO and WARN");
        I.click( locate("div.DTE_Field_Name_logLevel").find("button.dropdown-toggle") );
        I.waitForElement("div.dropdown-menu.show");

        I.dontSeeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("NORMAL") ) );
        I.dontSeeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("DEBUG") ) );

        I.seeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("ERROR") ) );
        I.seeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("INFO") ) );
        I.seeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("TRACE") ) );
        I.seeElement( locate("div.dropdown-menu.show").find( locate("li.disabled > a.dropdown-item").withText("WARN") ) );

        I.click( locate("div.DTE_Field_Name_logLevel").find("button.dropdown-toggle") );

        DTE.cancel();

    I.say("Delete CANT remove DEFAULT log level");
        I.clickCss("td.dt-select-td.sorting_1");
        I.clickCss("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.filter("packageName", defaultLogLevel);
        I.see(defaultLogLevel);
});

Scenario("Test DB save ", async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/apps/audit-log-levels/");
    let newLogLevel = "log_level_DB_" + randomNumber + "-autotest";

    I.click("button.buttons-create");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_packageName", newLogLevel);
    selectEditorLogLevel(I, "ERROR");
    I.checkOption("#DTE_Field_saveIntoDB_0");
    DTE.save();

    I.say('Say, check value in conf');
        I.amOnPage("/admin/v9/settings/configuration/");
        DT.filter("name", "logLevels");
        I.click("logLevels");
        DTE.waitForEditor("configurationDatatable");
        const value = await I.grabValueFrom("#DTE_Field_value");
        I.assertContain(value, newLogLevel+"=ERROR", "CONF value do not contain new log level. Problem with DB saving.");
});

Scenario("Reset log levels", async ({ I, Document }) => {
    Document.setConfigValue("logLevel", "debug");
    Document.setConfigValue("logLevels", "sk.iway.iwcm.io=INFO");
});

function checkValue(I, DT, packageName, logLevel) {
    DT.filter("packageName", packageName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    if(logLevel != null) {
        I.see(logLevel);
    }
}

function selectEditorLogLevel(I, logLevel) {
    I.click( locate("div.DTE_Field_Name_logLevel").find("button.dropdown-toggle") );
    I.waitForElement("div.dropdown-menu.show");
    I.click( locate("div.dropdown-menu.show").find( locate("a.dropdown-item").withText(logLevel.toUpperCase()) ) );
    I.wait(1);
}