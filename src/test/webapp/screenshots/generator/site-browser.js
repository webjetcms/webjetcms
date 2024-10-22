Feature('site-browser');

Before(({ I, login }) => {
    login('admin');
});

Scenario('site-browser', async ({ I, DTE, Document }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=72299");
    DTE.waitForEditor();
    I.wait(5);

    Document.editorComponentOpen();
    Document.screenshot("/redactor/apps/site-browser/editor-dialog.png");
    I.switchTo();

    let postfix = ("en" === I.getCongLng) ? "?language=en" : "";
    I.amOnPage("/apps/site-browser/");
    Document.screenshot("/redactor/apps/site-browser/site-browser.png" + postfix);
});
