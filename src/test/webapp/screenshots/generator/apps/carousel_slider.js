Feature('apps.carousel_slider');

Before(({ login }) => {
    login('admin');
});

Scenario('app carousel slider', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/carousel-slider/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/carousel_slider/carousel.png");

    Document.screenshotAppEditor(77872, "/redactor/apps/carousel_slider/editor-style.png", function(Document, I, DT, DTE) {
        I.clickCss("#tabLink2");
        Document.screenshot("/redactor/apps/carousel_slider/editor-items.png");
        I.clickCss("#tabLink3");
        I.checkOption('input[name="custom_properties"]');
        Document.screenshot("/redactor/apps/carousel_slider/editor-settings.png");
        I.clickCss("#tabLink1");
    });
});
