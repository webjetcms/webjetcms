Feature('form.regexp');

Before(({ I, login }) => {
    login('admin');
});

Scenario('form-regexps', ({ I, Document }) => {
    I.amOnPage("/apps/form/admin/regexps/");

    //Form regexp data table
    Document.screenshot("/redactor/apps/form/regexp-datatable.png");

    //from regexp editor
    I.click("button.buttons-create");
    I.wait(1);

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/form/regexp-editor.png");
});