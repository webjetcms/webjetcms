Feature('apps.slider');

Before(({ login }) => {
    login('admin');
});

Scenario('slider', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/slider/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/slider/slider.png");


    Document.screenshotAppEditor(77869, "/redactor/apps/slider/editor-settings.png", function(Document, I, DT, DTE) {
        Document.screenshot();
        I.click("#tabLink2");
        Document.screenshot("/redactor/apps/slider/editor-transitions.png");
        I.click("#tabLink3");
        Document.screenshot("/redactor/apps/slider/editor-items.png");
        I.click("#tabLink4");
        Document.screenshot("/redactor/apps/slider/editor-advanced.png");
        I.click("#tabLink1");
    });

});
