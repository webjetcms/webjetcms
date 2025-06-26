Feature('apps.app-social_icon');

Before(({ login }) => {
    login('admin');
});

Scenario('odkazy na socialne siete', ({ I, DT, Document }) => {
    I.amOnPage("/apps/odkazy-socialne-siete/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-social_icon/app-social_icon.png");

    Document.screenshotAppEditor(77772, "/redactor/apps/app-social_icon/editor-base.png", function(Document, I) {
        I.clickCss("#pills-dt-component-datatable-style-tab");
        Document.screenshot("/redactor/apps/app-social_icon/editor-style.png");

        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });
});
