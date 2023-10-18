Feature('apps.calendar.types');

var entityName, randomNumber;

Before(({ I, login }) => {

    login('admin');
    I.amOnPage("/apps/calendar/admin/calendar-types");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        entityName = "name-autotest-" + randomNumber;
    }
});

Scenario('zakladne testy', ({I, DTE}) => {

    I.click("button.buttons-create");
    DTE.waitForEditor('calendarTypesDataTable');

    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", entityName);
    I.see("Žiaden schvaľovateľ");
    I.click(locate('button').withText('Žiaden schvaľovateľ'));
    I.wait(1);
    I.click(locate('span').withText('WebJET Admin'));
    DTE.save();

    I.fillField("input.dt-filter-name", entityName);
    I.pressKey('Enter', "input.dt-filter-name");
    I.click(entityName);
    DTE.waitForEditor('calendarTypesDataTable');

    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", entityName + ".changed");
    I.see("WebJET Admin");
    I.click(locate('button').withText('WebJET Admin'));
    I.wait(1);
    I.click(locate('span').withText('WebJET Administrátor'));
    DTE.save();

    I.fillField("input.dt-filter-name", entityName + ".changed");
    I.pressKey('Enter', "input.dt-filter-name");
    I.see(entityName + ".changed");
    //I.see('WebJET Administrátor');

    /* DELETE TEST */
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(entityName + ".changed")
});