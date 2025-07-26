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

        I.waitForVisible("#datatableFieldDTE_Field_editorData_wrapper", 5);

        I.click("Koleso", "#datatableFieldDTE_Field_editorData");

        DTE.waitForEditor("datatableFieldDTE_Field_editorData");

        Document.screenshot("/redactor/apps/carousel_slider/editor-items-edit.png");

        I.click("#datatableFieldDTE_Field_editorData_modal .DTE_Header button.btn-close-editor");

        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });
});
