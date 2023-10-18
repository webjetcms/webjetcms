Feature('regexp');

Before(({ I, login }) => {
    login('admin');
});

Scenario('gdpr-regexps', ({ I, Document }) => {
    I.amOnPage("/apps/gdpr/admin/regexps/");

    //Regexp data table
    Document.screenshot("/redactor/apps/gdpr/regexp-datatable.png");

    //Regexp editor
    I.click("button.buttons-create");
    I.wait(1);

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/gdpr/regexp-editor.png");
});