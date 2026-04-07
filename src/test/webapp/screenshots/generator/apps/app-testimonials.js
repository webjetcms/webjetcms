Feature('apps.app-testimonials');

Before(({ login }) => {
    login('admin');
});

Scenario('app testimonials', ({ I, DT, Document }) => {
    I.amOnPage("/apps/odporucania/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-testimonials/app-testimonials.png");

    Document.screenshotAppEditor(77773, "/redactor/apps/app-testimonials/editor-style.png", function(Document, I, DT, DTE) {
        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        Document.screenshot("/redactor/apps/app-testimonials/editor-settings.png");

        I.clickCss("#pills-dt-component-datatable-items-tab");
        Document.screenshot("/redactor/apps/app-testimonials/editor-items.png");

        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });
});
