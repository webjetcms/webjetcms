Feature('admin.topdf');

Before(({ I }) => {
    I.logout();
});

Scenario("topdf print", async ({ I, Document }) => {
    //chromium PDF viewer is not working in headless mode
    if ("false"!==process.env.CODECEPT_SHOW) {
        I.amOnPage("/topdf/nieco.pdf?docid=57");

        await Document.compareScreenshotElement(null, "pdf.png", 1280, 270, 20);

        I.wait(30);
        I.amOnPage("/produktova-stranka/produktova-stranka-pagebuilder.html?_printAsPdf=true&_printAsPdfNoAttachment=true");

        await Document.compareScreenshotElement(null, "pdf.png", 1280, 270, 20);
    }
});