Feature('in-memory-logging');

Before(({ I, login }) => {
    login('admin');
});

Scenario('logovanie v pamati', ({ I, Document }) => {
    I.amOnPage("/admin/v9/settings/in-memory-logging/");
    I.wait(5);
    Document.screenshot("/sysadmin/audit/memory-logging.png", null, null);
});