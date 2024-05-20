Feature('apps.calendar.non-approved-events');

var randomNumber;

Before(({ I }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('calendar non-approved events screens', async ({I, DT, DTE, Document}) => {
    I.relogin('tester2');

    let eventNameA = "event_toBeApproved" + randomNumber + "_autotest";
    let eventNameB = "event_toBeReject" + randomNumber + "_autotest";

    I.amOnPage("/apps/calendar/admin/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("calendarEventsDataTable");
    I.fillField("#DTE_Field_title", eventNameA);
    I.clickCss("#pills-dt-calendarEventsDataTable-advanced-tab");
    DTE.selectOption("typeId", "ApproveByTester");
    DTE.save();

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("calendarEventsDataTable");
    I.fillField("#DTE_Field_title", eventNameB);
    I.clickCss("#pills-dt-calendarEventsDataTable-advanced-tab");
    DTE.selectOption("typeId", "ApproveByTester");
    DTE.save();

    I.relogin("tester");
    I.amOnPage("/apps/calendar/admin/non-approved-events/");
    Document.screenshot("/redactor/apps/calendar/non-approved-events/page.png");

    DT.filter("title", eventNameA);
    I.clickCss("td.sorting_1");
    Document.screenshotElement("button.buttons-approve", "/redactor/apps/calendar/non-approved-events/approve_button.png");
    Document.screenshotElement("button.buttons-reject", "/redactor/apps/calendar/non-approved-events/reject_button.png");

    I.clickCss("button.buttons-approve");
    I.waitForElement("div.toast-container div.toast");
    I.moveCursorTo("div.toast-container div.toast");
    Document.screenshotElement("div.toast-container div.toast", "/redactor/apps/calendar/non-approved-events/approved_toast.png");
    I.forceClickCss("div.toast-container .toast-close-button");

    DT.filter("title", eventNameB);
    I.clickCss("td.sorting_1");

    I.clickCss("button.buttons-reject");
    I.waitForElement("div.toast-container div.toast");
    I.moveCursorTo("div.toast-container div.toast");
    Document.screenshotElement("div.toast-container div.toast", "/redactor/apps/calendar/non-approved-events/rejected_toast.png");

    I.amOnPage("/apps/calendar/admin/");
    DT.filter("title", eventNameA);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    DT.filter("title", eventNameB);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
});