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

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("calendarEventsDataTable");

    I.click("#DTE_Field_title");
    I.fillField("#DTE_Field_title", entityName);

    //Select type
    I.click("#pills-dt-calendarEventsDataTable-advanced-tab");
    I.click("#panel-body-dt-calendarEventsDataTable-advanced > div.DTE_Field.form-group.row.DTE_Field_Type_select.DTE_Field_Name_typeId > div.col-sm-7 > div.DTE_Field_InputControl > div > button");
    I.click("Rodina");

    //
    I.click("#pills-dt-calendarEventsDataTable-notification-tab");
    I.click("Obchodní partneri");
    I.click("VIP Klienti");

    DTE.save();

    //Check
    I.fillField("input.dt-filter-title", entityName);
    I.pressKey('Enter', "input.dt-filter-title");

    DT.waitForLoader();
    I.click(entityName);
    DTE.waitForEditor("calendarEventsDataTable");

    I.click("#pills-dt-calendarEventsDataTable-advanced-tab");
    I.see("Rodina");

    I.click("#pills-dt-calendarEventsDataTable-notification-tab");
    I.seeElement("#DTE_Field_editorFields-notifyEmailsUserGroups_3:checked");
    I.seeElement("#DTE_Field_editorFields-notifyEmailsUserGroups_5:checked");

    //un-check
    I.click("Obchodní partneri");
    I.click("VIP Klienti");
    //check
    I.click("Bankári");
    I.click("Redaktori");

    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("calendarEventsDataTable");

    I.click("#pills-dt-calendarEventsDataTable-notification-tab");
    I.dontSeeElement("#DTE_Field_editorFields-notifyEmailsUserGroups_1:checked");
    I.dontSeeElement("#DTE_Field_editorFields-notifyEmailsUserGroups_3:checked");
    I.seeElement("#DTE_Field_editorFields-notifyEmailsUserGroups_0:checked");
    I.seeElement("#DTE_Field_editorFields-notifyEmailsUserGroups_4:checked");

    DTE.save();

    /* DELETE TEST */
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dtEditorWaitForLoader();

    I.dontSee(entityName);
});
