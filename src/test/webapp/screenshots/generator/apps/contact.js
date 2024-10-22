Feature('apps.contact');
Before(({ login }) => {
    login('admin');
});

Scenario('Kontakt', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/kontakty");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/contact/contact.png");
    Document.screenshotAppEditor(106343, "/redactor/apps/contact/editor.png");
});
