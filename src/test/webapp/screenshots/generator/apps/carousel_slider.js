Feature('apps.carousel_slider');

Before(({ login }) => {
    login('admin');
});

Scenario('app carousel slider', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/carousel-slider/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/carousel_slider/carousel.png");

    Document.screenshotAppEditor(77872, "/redactor/apps/carousel_slider/editor-style.png", function(Document, I, DT, DTE) {
        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        Document.screenshot("/redactor/apps/carousel_slider/editor-settings.png");

        I.clickCss("#pills-dt-component-datatable-files-tab");
        Document.screenshot("/redactor/apps/carousel_slider/editor-items.png");

        I.waitForVisible("#DTE_Field_iframe", 5);
        I.switchTo("#DTE_Field_iframe");

        I.clickCss("td.sorting_1");
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor("carouselSliderItemsDataTable");

        Document.screenshot("/redactor/apps/carousel_slider/editor-items-edit.png");

        DTE.cancel();

        I.switchTo();
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });
});
