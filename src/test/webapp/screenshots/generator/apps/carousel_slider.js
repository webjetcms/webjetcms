Feature('apps.carousel_slider');

Before(({ login }) => {
    login('admin');
});

Scenario('app carousel slider', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/carousel-slider/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/carousel_slider/carousel.png");

    Document.screenshotAppEditor(77872, "/redactor/apps/carousel_slider/editor-style.png", function(Document, I, DT, DTE) {
        I.click("#tabLink2");
        Document.screenshot("/redactor/apps/carousel_slider/editor-items.png");
        I.click("#tabLink3");
        I.checkOption('.checker input[name="custom_properties"]');
        Document.screenshot("/redactor/apps/carousel_slider/editor-settings.png");
        I.click("#tabLink1");
    });
});
