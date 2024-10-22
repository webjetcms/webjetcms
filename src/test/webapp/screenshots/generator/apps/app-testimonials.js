Feature('apps.app-testimonials');

Before(({ login }) => {
    login('admin');
});

Scenario('app testimonials', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/odporucania/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-testimonials/app-testimonials.png");

    Document.screenshotAppEditor(77773, "/redactor/apps/app-testimonials/editor-style.png", function(Document, I, DT, DTE) {
        I.click("#tabLink2");
        Document.screenshot("/redactor/apps/app-testimonials/editor-items.png");
        I.click("#tabLink1");
    });
});
