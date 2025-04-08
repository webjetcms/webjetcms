Feature('apps.app-impress_slideshow');

Before(({ login }) => {
    login('admin');
});

Scenario('posobiva prezentacia', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/posobiva-prezentacia/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-impress_slideshow/app-impress_slideshow.png");

    Document.screenshotAppEditor(77868, "/redactor/apps/app-impress_slideshow/editor-style.png", function(Document, I, DT, DTE) {
        I.clickCss("#tabLink2");
        Document.screenshot("/redactor/apps/app-impress_slideshow/editor-items.png");
        I.clickCss("#tabLink1");
    });
});
