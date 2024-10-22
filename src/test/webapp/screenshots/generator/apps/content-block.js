Feature('apps.content-block');

Before(({ login }) => {
    login('admin');
});

Scenario('app content block', ({ I, DT, DTE, Document }) => {

    I.amOnPage("/apps/content-block/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/content-block/content-block.png");

    Document.screenshotAppEditor(77766, "/redactor/apps/content-block/editor.png");
});
