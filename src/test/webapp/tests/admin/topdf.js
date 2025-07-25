Feature('admin.topdf');

Scenario("topdf print", async ({ I, Document }) => {
    //chromium PDF viewer is not working in headless mode
    if (Document.isPdfViewerEnabled()) {
        I.logout();
        I.wait(5);
        I.resizeWindow(1280, 350);
        I.amOnPage("/topdf/nieco.pdf?docid=57");
        I.wait(5);

        await Document.compareScreenshotElement(null, "pdf.png", null, null, 20);

        I.wait(30);
        I.amOnPage("/produktova-stranka/produktova-stranka-pagebuilder.html?_printAsPdf=true&_printAsPdfNoAttachment=true");
        I.wait(5);

        await Document.compareScreenshotElement(null, "pdf.png", null, null, 20);
        I.wjSetDefaultWindowSize();
    }
});