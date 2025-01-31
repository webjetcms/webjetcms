Feature('unsubscribed');

Before(({ I, login }) => {
    login('admin');
});

Scenario('unsubscribed', ({ I, Document }) => {
    I.amOnPage("/apps/dmail/admin/unsubscribed/");

    Document.screenshot("/redactor/apps/dmail/unsubscribed/unsubscribed-datatable.png", 1280, 400);

    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/unsubscribed/unsubscribed-editor.png", 1280, 400);

    I.amOnPage("/newsletter/odhlasenie-z-newsletra.html?NO_WJTOOLBAR=true");
    Document.screenshot("/redactor/apps/dmail/unsubscribed/unsubscribed-form.png", 760, 600);
});

Scenario('Unsubscribed aproval', ({ Document, I }) => {
    Document.screenshotAppEditor(87513, "/redactor/apps/dmail/form/editor.png");
    I.amOnPage('/newsletter/odhlasenie-z-newsletra.html');
    I.waitForElement('#wjInline-docdata', 10);
    Document.screenshot("/redactor/apps/dmail/form/form.png")
});