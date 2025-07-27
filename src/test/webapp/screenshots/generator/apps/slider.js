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

        I.waitForVisible("#datatableFieldDTE_Field_editorData_wrapper", 5);

        I.click("Pohľad z boku", "#datatableFieldDTE_Field_editorData");

        DTE.waitForEditor("datatableFieldDTE_Field_editorData");

        Document.screenshot("/redactor/apps/slider/editor-items-edit.png");

        I.click("#datatableFieldDTE_Field_editorData_modal .DTE_Header button.btn-close-editor");

        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });

});
