Feature('apps.map');

Before(({ login }) => {
    login('admin');
});

Scenario('app map', ({ I, DT, Document }) => {

    I.amOnPage("/apps/map/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/map/map.png");

    Document.screenshotAppEditor(59889, "/redactor/apps/map/map-editor.png", function(Document, I) {

        I.clickCss("#pills-dt-component-datatable-mapSettings-tab");
        Document.screenshot("/redactor/apps/map/editor-map_settings.png");

        I.clickCss("#pills-dt-component-datatable-pinSettings-tab");
        Document.screenshot("/redactor/apps/map/editor-pin_settings.png");

        // apply change of height
        I.clickCss("#pills-dt-component-datatable-mapSettings-tab");
        I.fillField("#DTE_Field_heightPercent", "100");
        I.clickCss("#pills-dt-component-datatable-mapSettings button.btn-primary");
    });
});
