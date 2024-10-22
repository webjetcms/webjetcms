Feature('apps.app-docsembled');

Before(({ login }) => {
    login('admin');
});

Scenario('vlozenie dokumentu', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/vlozenie-dokumentu/");

    I.waitForInvisible(".loader", 80);
    Document.screenshot("/redactor/apps/app-docsembed/app-docsembed.png");

    Document.screenshotAppEditor(77874, "/redactor/apps/app-docsembed/editor.png");
});
