Feature('apps.language-redirect');

Before(({ login }) => {
    login('admin');
});

Scenario('editor', ({ I, DT, DTE, Document }) => {
    Document.screenshotAppEditor(163462, '/redactor/apps/language-redirect/editor-basic.png', function(Document, I, DT, DTE, Apps) {
        Apps.switchToTabByIndex(1);
        Document.screenshot("/redactor/apps/language-redirect/editor-advanced.png");
        Apps.switchToTabByIndex(0);
    });
});