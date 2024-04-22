Feature('apps.calendar');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('zakladne testy', async ({I, DataTables}) => {
    I.amOnPage("/apps/calendar/admin/");
    await DataTables.baseTest({
        dataTable: 'calendarEventsDataTable',
        perms: 'cmp_calendar',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('testy zaloziek', async ({I, DT, DTE}) => {

    let entityName = "autotest-zalozky-" + randomNumber;
    I.amOnPage("/apps/calendar/admin/");

    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("calendarEventsDataTable");

    I.clickCss("#DTE_Field_title");
    I.fillField("#DTE_Field_title", entityName);

    //Select type
    I.clickCss("#pills-dt-calendarEventsDataTable-advanced-tab");
    DTE.selectOption("typeId", "Rodina");

    //
    I.clickCss("#pills-dt-calendarEventsDataTable-notification-tab");
    DTE.clickSwitchLabel("Obchodní partneri");
    DTE.clickSwitchLabel("VIP Klienti");

    DTE.save();

    //Check
    I.fillField("input.dt-filter-title", entityName);
    I.pressKey('Enter', "input.dt-filter-title");

    DT.waitForLoader();
    I.click(entityName);
    DTE.waitForEditor("calendarEventsDataTable");

    I.clickCss("#pills-dt-calendarEventsDataTable-advanced-tab");
    I.see("Rodina");

    I.clickCss("#pills-dt-calendarEventsDataTable-notification-tab");
    I.seeCheckboxIsChecked("Obchodní partneri");
    I.seeCheckboxIsChecked("VIP Klienti");

    //un-check
    DTE.clickSwitchLabel("Obchodní partneri");
    DTE.clickSwitchLabel("VIP Klienti");
    //check
    DTE.clickSwitchLabel("Bankári");
    DTE.clickSwitchLabel("Redaktori");

    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("calendarEventsDataTable");

    I.clickCss("#pills-dt-calendarEventsDataTable-notification-tab");
    I.dontSeeCheckboxIsChecked("Obchodní partneri");
    I.dontSeeCheckboxIsChecked("VIP Klienti");
    I.seeCheckboxIsChecked("Bankári");
    I.seeCheckboxIsChecked("Redaktori");

    DTE.save();

    /* DELETE TEST */
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dtEditorWaitForLoader();

    I.dontSee(entityName);
});
