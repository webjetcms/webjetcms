Feature('unsubscribed');

Before(({ I, login }) => {
    login('admin');
});

Scenario('unsubscribed', ({ I, Document }) => {
    I.amOnPage("/apps/dmail/admin/unsubscribed/");

    Document.screenshot("/redactor/apps/dmail/unsubscribed/unsubscribed-datatable.png", 1280, 400);

    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/unsubscribed/unsubscribed-editor.png", 1280, 400);
});