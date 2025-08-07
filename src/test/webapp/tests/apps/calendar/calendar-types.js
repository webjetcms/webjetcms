Feature('apps.calendar.types');

var entityName, randomNumber;

Before(({ I, login }) => {

    login('admin');
    I.amOnPage("/apps/calendar/admin/calendar-types/");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        entityName = "name-autotest-" + randomNumber;
    }
});

Scenario('zakladne testy', ({I, DT, DTE}) => {

    I.click("button.buttons-create");
    DTE.waitForEditor('calendarTypesDataTable');

    I.clickCss("#DTE_Field_name");
    I.fillField("#DTE_Field_name", entityName);
    I.see("Žiaden schvaľovateľ");
    I.click(locate('button').withText('Žiaden schvaľovateľ'));
    I.wait(1);
    I.click(locate('span').withText('WebJET Admin'));
    DTE.save();

    DT.filterContains("name", entityName);
    I.click(entityName);
    DTE.waitForEditor('calendarTypesDataTable');

    I.clickCss("#DTE_Field_name");
    I.fillField("#DTE_Field_name", entityName + ".changed");
    I.see("WebJET Admin");
    I.click(locate('button').withText('WebJET Admin'));
    I.wait(1);
    I.click(locate('span').withText('WebJET Administrátor'));
    DTE.save();

    DT.filterContains("name", entityName + ".changed");
    I.see(entityName + ".changed");
    //I.see('WebJET Administrátor');

    /* DELETE TEST */
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    DTE.waitForEditor('calendarTypesDataTable');
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();

    I.dontSee(entityName + ".changed")
});

Scenario('Domain test', ({I, DT, Document}) => {
    I.amOnPage("/apps/calendar/admin/calendar-types");
    DT.filterContains("name", "Výstava");
    I.see("Výstava");

    DT.filterContains("name", "DomainTest_Test23_type");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    Document.switchDomain("test23.tau27.iway.sk");
    DT.filterContains("name", "Výstava");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    DT.filterContains("name", "DomainTest_Test23_type");
    I.see("DomainTest_Test23_type");
});

Scenario('logout', async ({I}) => {
    I.logout();
});