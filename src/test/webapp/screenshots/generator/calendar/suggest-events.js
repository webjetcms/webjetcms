Feature('apps.calendar.suggest-events');

var randomNumber;

Before(({ I }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('calendar suggest events screens', async ({I, DT, DTE, Document}) => {
    let eventName = "suggest_event" + randomNumber + "_autotest";
    I.relogin('tester');

    I.amOnPage("/apps/calendar/admin/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("calendarEventsDataTable");
    I.fillField("#DTE_Field_title", eventName);
    I.clickCss("#pills-dt-calendarEventsDataTable-advanced-tab");
    DTE.selectOption("typeId", "Výstava");
    DTE.save();

    I.amOnPage("/apps/calendar/admin/suggest-events/");
    Document.screenshot("/redactor/apps/calendar/suggest-events/page.png");

    DT.filter("title", eventName);
    I.clickCss("td.sorting_1");
    Document.screenshotElement("button.buttons-suggest", "/redactor/apps/calendar/suggest-events/suggest_button.png");
    Document.screenshotElement("button.buttons-not-suggest", "/redactor/apps/calendar/suggest-events/not_suggest_button.png");

    I.clickCss("button.buttons-suggest");
    I.waitForElement("div.toast-container div.toast");
    I.moveCursorTo("div.toast-container div.toast");
    Document.screenshotElement("div.toast-container div.toast", "/redactor/apps/calendar/suggest-events/suggested_toast.png");
    I.forceClickCss("div.toast-container .toast-close-button");

    I.clickCss("button.buttons-not-suggest");
    I.waitForElement("div.toast-container div.toast");
    I.moveCursorTo("div.toast-container div.toast");
    Document.screenshotElement("div.toast-container div.toast", "/redactor/apps/calendar/suggest-events/not_suggested_toast.png");

    I.amOnPage("/apps/calendar/admin/");
    DT.filter("title", eventName);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.clickCss("div.DTE_Action_Remove div.DTE_Footer div.DTE_Form_Buttons button.btn-primary");
});