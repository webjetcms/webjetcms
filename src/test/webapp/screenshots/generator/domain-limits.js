Feature('domain-limits');

Before(({ I, login }) => {
    login('admin');
});

Scenario('domain-limits', ({ I, Document }) => {
    I.amOnPage("/apps/dmail/admin/domain-limits/");

    Document.screenshot("/redactor/apps/dmail/domain-limits/datatable.png", 1280, 700);

    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/domain-limits/editor.png");
});