Feature('apps.app-impress_slideshow');

Before(({ login }) => {
    login('admin');
});

Scenario('impress slideshow app screens', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/posobiva-prezentacia/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-impress_slideshow/app-impress_slideshow.png");

    Document.screenshotAppEditor(77868, "/redactor/apps/app-impress_slideshow/editor-style.png", function(Document, I, DT, DTE) {
        I.clickCss("#pills-dt-component-datatable-tabLink2-tab");
        Document.screenshot("/redactor/apps/app-impress_slideshow/editor-items.png");

        I.waitForVisible("#DTE_Field_iframe", 5);
        I.switchTo("#DTE_Field_iframe");

        I.clickCss("td.sorting_1");
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor("impressSlideshowDataTable");

        Document.screenshot("/redactor/apps/app-impress_slideshow/editor-items-edit.png");

        DTE.cancel();

        I.switchTo();
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        I.clickCss("#pills-dt-component-datatable-style-tab");
    });
});