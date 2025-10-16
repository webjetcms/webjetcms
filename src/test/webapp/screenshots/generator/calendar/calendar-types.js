Feature('calendar-types');

Before(({ I, login }) => {
    login('admin');
});

Scenario('calendar-types', ({ I, Document }) => {
    I.amOnPage("/apps/calendar/admin/calendar-types/");

    Document.screenshot("/redactor/apps/calendar/calendar-types/datatable.png", 1280, 700);

    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/calendar/calendar-types/editor.png");
});