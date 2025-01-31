Feature('apps.calendar.non-approved-events');

var eventName, randomNumber;

Before(({ I }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        eventName = "event_toBeApproved" + randomNumber + "_autotest";
    }
});

Scenario('Test the logic of approving and suggesting existing events', async ({I, DT, DTE, Document}) => {

    I.say("Create new event, that must be approved (NON-Approver)")
    I.relogin('tester2');
    I.amOnPage("/apps/calendar/admin/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("calendarEventsDataTable");
    I.fillField("#DTE_Field_title", eventName);
    I.clickCss("#pills-dt-calendarEventsDataTable-advanced-tab");
    DTE.selectOption("typeId", "ApproveByTester");
    DTE.save();

    I.say("Check that I NON-Approver CANT see this event");
    I.amOnPage("/apps/calendar/admin/non-approved-events/");
    DT.filterContains("title", eventName);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("check, that non-approved event isn't visible in suggest-events.");
    I.amOnPage("/apps/calendar/admin/suggest-events/");
    DT.filterContains("title", eventName);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check it's not on webpage");
    I.amOnPage("/apps/kalendar-udalosti/");
    I.dontSeeInSource(eventName);

    I.say("Relogin as approver - and check, that you see this event");
    I.relogin("tester");
    I.amOnPage("/apps/calendar/admin/non-approved-events/");
    DT.filterContains("title", eventName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Approve event")
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-approve");
    Document.notifyCheckAndClose("Akcia schválenia udalosti s názvom: " + eventName + " bola úspešná.");

    I.say("Check that event is approved (not visible in table)");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check it's on webpage");
    I.amOnPage("/apps/kalendar-udalosti/");
    I.seeInSource(eventName);

    I.say("Check, that approved EVENT is NOW visible in suggest-events.");
    I.amOnPage("/apps/calendar/admin/suggest-events/");
    DT.filterContains("title", eventName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    //
    I.say("Suggest event");
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-suggest");
    Document.notifyCheckAndClose("Akcia odporučenia udalosti s názvom: " + eventName + " bola úspešná.");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check it's not on webpage");
    I.amOnPage("/apps/kalendar-udalosti/");
    I.seeInSource(eventName);

    I.say("Remove this event");
    I.amOnPage("/apps/calendar/admin/");
    DT.filterContains("title", eventName);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
});