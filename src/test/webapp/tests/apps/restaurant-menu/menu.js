Feature('apps.restaurant-menu.menu');

Before(({ login, DT }) => {
    login('admin');
    DT.addContext('menu', '#menuDataTable_wrapper');
});

Scenario('basic tests', async ({I, DT, DTE}) => {
    I.amOnPage("/apps/restaurant-menu/admin/");
    DT.setExtfilterDate("25.11.2023");
    I.clickCss("#menuTypeDays");
    DT.waitForLoader();

    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
    I.click(DT.btn.menu_add_button);
    DTE.waitForLoader("menuDataTable");
    DTE.selectOption('editorFields\\.mealCathegory', 'Hlavné jedlo');
    DTE.selectOption('editorFields\\.selectedMealId', 'Rezeň so zemiakmi');

    const dateValue = await I.grabValueFrom("#DTE_Field_dayDate");
    I.assertEqual("25.11.2023", dateValue);
    const priority = await I.grabValueFrom("#DTE_Field_priority");
    I.assertEqual("10", priority);
    DTE.save();

    I.see("Hlavné jedlo");
    I.see("Rezeň so zemiakmi");
    I.see("10");
    I.see("1 - Obilniny, 3 - Vajcia");

    //First
    I.clickCss("#menuDataTable > tbody > tr > td.dt-select-td.cell-not-editable");
    I.click("button.btn.btn-sm.buttons-selected.buttons-edit");
    DTE.waitForLoader("menuDataTable");
    DTE.selectOption('editorFields\\.mealCathegory', 'Príloha');
    DTE.selectOption('editorFields\\.selectedMealId', 'Hranolky');
    DTE.save();

    I.see("Príloha");
    I.see("Hranolky");
    I.see("10");
    I.dontSee("1 - Obilniny, 3 - Vajcia");

    I.click("button.btn.btn-sm.buttons-selected.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('days-weeks dates setting tests', async ({I, DT}) => {
    I.amOnPage("/apps/restaurant-menu/admin/");

    I.say("Test of filter and sorting")
    DT.setExtfilterDate("29.11.2023");
    I.clickCss("#menuTypeDays");
    DT.waitForLoader();

    DT.checkTableRow("menuDataTable", 1, ["69", "Streda", "29.11.2023", "Polievka", "Vývar"]);
    DT.checkTableRow("menuDataTable", 2, ["71", "Streda", "29.11.2023", "Hlavné jedlo", "Kung Pao"]);
    DT.checkTableRow("menuDataTable", 3, ["70", "Streda", "29.11.2023", "Príloha", "Ryza"]);

    I.say("Test info box");
    await testDateInfoInput(I, "Streda - 48. týždeň");

    I.say("Test jump one day back + test's");
    I.click("button.day-back");
    await testDateInputValue(I, "28.11.2023");
    DT.checkTableRow("menuDataTable", 1, ["66", "Utorok", "28.11.2023", "Polievka", "Hrachová"]);
    DT.checkTableRow("menuDataTable", 2, ["67", "Utorok", "28.11.2023", "Hlavné jedlo", "Rezeň so zemiakmi"]);
    DT.checkTableRow("menuDataTable", 3, ["68", "Utorok", "28.11.2023", "Dezert", "Veterník"]);

    I.say("Test jump one day forward + test's");
    I.click("button.day-forward");
    I.click("button.day-forward");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    await testDateInputValue(I, "30.11.2023");

    I.say("Now try weeks version");
    I.clickCss("#menuType > button[data-menu-type=weeks]");
    //Changed type of info input format
    await testDateInfoInput(I, "48. týždeň - 2023");
    //date must be still same
    await testDateInputValue(I, "30.11.2023");

    //this time the must be here day in week column
    DT.checkTableRow("menuDataTable", 1, ["54", "Pondelok", "27.11.2023", "Polievka", "Vývar"]);
    DT.checkTableRow("menuDataTable", 2, ["61", "Pondelok", "27.11.2023", "Polievka", "Gulášová"]);
    DT.checkTableRow("menuDataTable", 3, ["57", "Pondelok", "27.11.2023", "Hlavné jedlo", "Kung Pao"]);

    I.say("Test jump one WEEK back + test's");
    I.click("button.day-back");
    await testDateInputValue(I, "23.11.2023");
    await testDateInfoInput(I, "47. týždeň - 2023");
    DT.checkTableRow("menuDataTable", 1, ["42", "Pondelok", "20.11.2023", "Dezert", "chese kake A"]);
    DT.checkTableRow("menuDataTable", 2, ["43", "Pondelok", "20.11.2023", "Dezert", "chese kake B"]);
    DT.checkTableRow("menuDataTable", 3, ["39", "Streda", "22.11.2023", "Hlavné jedlo", "Boloňské špagety"]);

    I.say("Go back o days format");
    I.clickCss("#menuType > button[data-menu-type=days]");
    await testDateInputValue(I, "23.11.2023");
    await testDateInfoInput(I, "Štvrtok - 47. týždeň");
    DT.checkTableRow("menuDataTable", 1, ["52", "Štvrtok", "23.11.2023", "Polievka", "Fazuľová"]);
    DT.checkTableRow("menuDataTable", 2, ["53", "Štvrtok", "23.11.2023", "Polievka", "Hrachová"]);
    DT.checkTableRow("menuDataTable", 3, ["41", "Štvrtok", "23.11.2023", "Príloha", "Zemiaky"]);
});

Scenario('priority logic test', async ({I, DT, DTE}) => {
    I.amOnPage("/apps/restaurant-menu/admin/");

    DT.setExtfilterDate("27.11.2023");
    I.click(DT.btn.menu_add_button);
    DTE.waitForLoader("menuDataTable");

    const dateValue = await I.grabValueFrom("#DTE_Field_dayDate");
    I.assertEqual("27.11.2023", dateValue);

    let priority = await I.grabValueFrom("#DTE_Field_priority");
    I.assertEqual("20", priority);

    DTE.selectOption('editorFields\\.mealCathegory', 'Hlavné jedlo');

    //Changed cathegory = changed priority (based on date and cathegory max priority + 10)
    priority = await I.grabValueFrom("#DTE_Field_priority");
    I.assertEqual("30", priority);
});

Scenario('test of menu link to meal table', ({I, DT, DTE}) => {
    I.amOnPage("/apps/restaurant-menu/admin/");
    DT.setExtfilterDate("27.11.2023");

    I.click("Losos s cintronom");
    DTE.waitForLoader("mealsDataTable");

    I.see("Losos s cintronom");
    I.see("Ikea losos kvalita");
});

Scenario('all menu types test', async ({I}) => {
    I.amOnPage("/apps/restaurant-menu/all_menu_types.html");
    I.waitForText("ALL_MENU_TYPES");

    I.waitForElement("div.restaurant-menu.restaurant-menu-01");
    I.waitForElement("div.restaurant-menu.restaurant-menu-02");
    I.waitForElement("div.restaurant-menu.restaurant-menu-03");
    I.waitForElement("div.restaurant-menu.restaurant-menu-04");

    within("div.restaurant-menu.restaurant-menu-01", () => {
        I.fillField("#datepicker1_input", "48-2023");
        I.click("input.button50");

        I.waitForElement(locate("h2.menu").withText("Pondelok (27.11.2023)"));
        I.waitForText("Utorok (28.11.2023)", 10);
        I.see("Streda (29.11.2023)");
        //..
    });

    within("div.restaurant-menu.restaurant-menu-02", () => {
        I.fillField("#datepicker2_input", "48-2023");
        I.click("input.button50");

        I.waitForElement(locate("h2.menu").withText("Pondelok (27.11.2023)"));
        I.waitForText("Utorok (28.11.2023)", 10);
        I.see("Streda (29.11.2023)");
        //..
    });

    within("div.restaurant-menu.restaurant-menu-03", () => {
        I.fillField("#datepicker3_input", "48-2023");
        I.click("input.button50");

        I.waitForElement(locate("button.collapsed").withText("Pondelok (27.11.2023)"));
        I.seeElement(locate("button.collapsed").withText("Utorok (28.11.2023)"));
        I.seeElement(locate("button.collapsed").withText("Streda (29.11.2023)"));
        //..
    });

    within("div.restaurant-menu.restaurant-menu-04", () => {
        I.see("Dnes nie je v ponuke žiadne menu");
    });
});

async function testDateInfoInput(I, value) {
    let info_box = await I.grabValueFrom("#dateStatus");
    I.assertEqual(value, info_box);
}

async function testDateInputValue(I, value) {
    const date_input = await I.grabValueFrom("div.dt-extfilter-dayDate > form > div.input-group > input.datepicker.min");
    I.assertEqual(value, date_input);
}

Scenario('testy domainId', ({I, DT, DTE, Document}) => {

    //
    I.say("Testing webjet9 domain");
    I.amOnPage("/apps/restaurant-menu/?week=2023-W48");
    I.see("Gulášová", ".div_menu_02");
    I.dontSee("Test23 polievka", ".div_menu_02");
    I.dontSee("Test23 jedlo", ".div_menu_02");

    I.amOnPage("/apps/restaurant-menu/admin/");
    DT.waitForLoader();
    DT.setExtfilterDate("29.11.2023");

    I.see("Gulášová", "#menuDataTable");
    I.dontSee("Test23 polievka", "#menuDataTable");
    I.dontSee("Test23 jedlo", "#menuDataTable");

    I.click(DT.btn.menu_add_button);
    DTE.waitForLoader("menuDataTable");
    I.click({ css: "div.modal-dialog div.DTE_Field_Name_editorFields\\.selectedMealId button.dropdown-toggle" });
    I.waitForElement(locate('div.dropdown-menu.show .dropdown-menu.show'), 5);
    I.see('Gulášová', 'div.dropdown-menu.show .dropdown-menu.show');
    I.dontSee('Test23', 'div.dropdown-menu.show .dropdown-menu.show');
    I.pressKey('Escape');
    DTE.cancel();

    //
    I.say("Testing test23.tau27.iway.sk domain");
    Document.switchDomain("test23.tau27.iway.sk");
    DT.waitForLoader();
    DT.setExtfilterDate("29.11.2023");

    I.dontSee("Gulášová", "#menuDataTable");
    I.see("Test23 polievka", "#menuDataTable");
    I.see("Test23 jedlo", "#menuDataTable");

    I.click(DT.btn.menu_add_button);
    DTE.waitForLoader("menuDataTable");
    I.click({ css: "div.modal-dialog div.DTE_Field_Name_editorFields\\.selectedMealId button.dropdown-toggle" });
    I.waitForElement(locate('div.dropdown-menu.show .dropdown-menu.show'), 5);
    I.dontSee('Gulášová', 'div.dropdown-menu.show .dropdown-menu.show');
    I.see('Test23', 'div.dropdown-menu.show .dropdown-menu.show');
    I.pressKey('Escape');
    DTE.cancel();

    I.amOnPage("/apps/restaurant-menu/?week=2023-W48");
    I.dontSee("Gulášová", ".div_menu_02");
    I.see("Test23 polievka", ".div_menu_02");
    I.see("Test23 jedlo", ".div_menu_02");
});

Scenario('logoff', ({I}) => {
    I.logout();
});