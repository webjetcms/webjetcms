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

        I.waitForVisible("#datatableFieldDTE_Field_editorData_wrapper", 5);

        I.click("Koleso", "#datatableFieldDTE_Field_editorData");
        DTE.waitForEditor("datatableFieldDTE_Field_editorData");

        Document.screenshot("/redactor/apps/app-impress_slideshow/editor-items-edit.png");

        I.click("#datatableFieldDTE_Field_editorData_modal .DTE_Header button.btn-close-editor");

        I.clickCss("#pills-dt-component-datatable-style-tab");
    });
});