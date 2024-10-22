Feature('apps.app-htmlembed');

Before(({ login }) => {
    login('admin');
});

Scenario('vlozenie html kodu', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/vlozenie-html-kodu/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-htmlembed/app-htmlembed.png");

    Document.screenshotAppEditor(77873, "/redactor/apps/app-htmlembed/editor.png");
});
