Feature('calendar-admin');

Before(({ I, login }) => {
    login('admin');
});

Scenario('calendar-admin editor', ({ I, DTE, Document }) => {

    I.amOnPage("/apps/calendar/admin/");
    Document.screenshot("/redactor/apps/calendar/calendar-datatable.png");

    const confLng = I.getConfLng();

    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/calendar/admin/?id=1");
            break;
        case 'en':
            I.amOnPage("/apps/calendar/admin/?id=986");
            break;
        case 'cs':
            I.amOnPage("/apps/calendar/admin/?id=1588");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    
    DTE.waitForEditor("calendarEventsDataTable");

    //basic tab
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-basic.png");

    //description tab
    I.clickCss("#pills-dt-calendarEventsDataTable-description-tab");
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-description.png");

    //advanced tab
    I.clickCss("#pills-dt-calendarEventsDataTable-advanced-tab");
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-advanced.png");

    //notification tab
    I.clickCss("#pills-dt-calendarEventsDataTable-notification-tab");
    Document.screenshotElement("#calendarEventsDataTable_modal > div > div", "/redactor/apps/calendar/calendar-editor-notification.png");
});

Scenario('calendar-admin web', ({ I, Document }) => {
    I.amOnPage("/apps/kalendar-udalosti/?NO_WJTOOLBAR&language=" + I.getConfLng());

    Document.screenshot("/redactor/apps/calendar/webpage.png");

});