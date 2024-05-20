Feature('calendar-admin');

Before(({ I, login }) => {
    login('admin');
});

Scenario('calendar-admin editor', ({ I, DTE, Document }) => {

    I.amOnPage("/apps/calendar/admin/");
    Document.screenshot("/redactor/apps/calendar/calendar-datatable.png");

    if("sk" === I.getConfLng()) {
        I.amOnPage("/apps/calendar/admin/?id=1");
    } else if("en" === I.getConfLng()) {
        I.amOnPage("/apps/calendar/admin/?id=986");
    }
    DTE.waitForEditor("calendarEventsDataTable");

    //basic tab
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-basic.png");

    //description tab
    I.click("#pills-dt-calendarEventsDataTable-description-tab");
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-description.png");

    //advanced tab
    I.click("#pills-dt-calendarEventsDataTable-advanced-tab");
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-advanced.png");

    //notification tab
    I.click("#pills-dt-calendarEventsDataTable-notification-tab");
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-notification.png");
});

Scenario('calendar-admin web', ({ I, Document }) => {
    I.amOnPage("/apps/kalendar-udalosti/?NO_WJTOOLBAR&language=" + I.getConfLng());

    Document.screenshot("/redactor/apps/calendar/webpage.png");

});