Feature('apps.slider');

Before(({ login }) => {
    login('admin');
});

Scenario('slider app screens', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/slider/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/slider/slider.png");


    Document.screenshotAppEditor(77869, "/redactor/apps/slider/editor-settings.png", function(Document, I, DT, DTE) {
        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        Document.screenshot("/redactor/apps/slider/editor-advanced.png");

        I.clickCss("#pills-dt-component-datatable-transitions-tab");
        Document.screenshot("/redactor/apps/slider/editor-transitions.png");

        I.clickCss("#pills-dt-component-datatable-files-tab");
        Document.screenshot("/redactor/apps/slider/editor-items.png");

        I.waitForVisible("#DTE_Field_iframe", 5);
        I.switchTo("#DTE_Field_iframe");

        I.clickCss("td.sorting_1");
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor("sliderItemsDataTable");

        Document.screenshot("/redactor/apps/slider/editor-items-edit.png");

        DTE.cancel();

        I.switchTo();
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });

});
