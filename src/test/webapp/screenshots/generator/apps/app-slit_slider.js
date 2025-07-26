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

        I.waitForVisible("#datatableFieldDTE_Field_editorData_wrapper", 5);

        I.click("Na streche", "#datatableFieldDTE_Field_editorData");

        DTE.waitForEditor("datatableFieldDTE_Field_editorData");

        Document.screenshot("/redactor/apps/app-slit_slider/editor-items-edit.png");

        I.click("#datatableFieldDTE_Field_editorData_modal .DTE_Header button.btn-close-editor");

        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });
});
