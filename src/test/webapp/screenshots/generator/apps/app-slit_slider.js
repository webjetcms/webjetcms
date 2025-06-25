Feature('apps.app-slit_slider');

Before(({ login }) => {
    login('admin');
});

Scenario('slit slider', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/slit-slider/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-slit_slider/app-slit_slider.png");

    Document.screenshotAppEditor(77870, "/redactor/apps/app-slit_slider/editor-style.png", function(Document, I, DT, DTE) {
        I.clickCss("#pills-dt-component-datatable-files-tab");
        Document.screenshot("/redactor/apps/app-slit_slider/editor-items.png");

        I.waitForVisible("#DTE_Field_iframe", 5);
        I.switchTo("#DTE_Field_iframe");

        I.clickCss("td.sorting_1");
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor("slitSliderItemsDataTable");

        Document.screenshot("/redactor/apps/app-slit_slider/editor-items-edit.png");

        DTE.cancel();

        I.switchTo();
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });
});
