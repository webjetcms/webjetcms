Feature('apps.htmlbox');

Before(({ login }) => {
    login('admin');
});

Scenario('app htmlbox', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/bloky/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/htmlbox/htmlbox.png");

    Document.screenshotAppEditor(81104, "/redactor/apps/htmlbox/editor-our.png", function(Document, I, DT, DTE) {
        I.click("#tabLink2");
        Document.screenshot("/redactor/apps/htmlbox/editor-general.png");
        I.click("#tabLink1");
    });
});
