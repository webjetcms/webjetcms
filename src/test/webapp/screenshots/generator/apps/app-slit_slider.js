Feature('apps.app-slit_slider');

Before(({ login }) => {
    login('admin');
});

Scenario('slit slider', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/slit-slider/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-slit_slider/app-slit_slider.png");

    Document.screenshotAppEditor(77870, "/redactor/apps/app-slit_slider/editor-style.png", function(Document, I, DT, DTE) {
        I.click("#tabLink2");
        Document.screenshot("/redactor/apps/app-slit_slider/editor-items.png");
        I.click("#tabLink1");
    });
});
