Feature('apps.inquiry');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screeny', async ({I, DTE, Document}) => {
    I.amOnPage("/apps/anketa/?NO_WJTOOLBAR=true");
    Document.screenshot("/redactor/apps/inquiry/inquiry-example.png");

    I.amOnPage("/apps/inquiry/admin/");

    Document.screenshot("/redactor/apps/inquiry/inquiry-dataTable.png");

    I.click("Ako sa vám páči WebJET");
    DTE.waitForEditor("inquiryDataTable");

    Document.screenshot("/redactor/apps/inquiry/inquiry-editor_basic.png");

    I.click("#pills-dt-inquiryDataTable-advanced-tab");

    Document.screenshot("/redactor/apps/inquiry/inquiry-editor_advanced.png");

    I.click("#pills-dt-inquiryDataTable-answers-tab");

    Document.screenshot("/redactor/apps/inquiry/inquiry-editor_answers.png");

    I.click("#datatableFieldDTE_Field_editorFields-answers_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");

    Document.screenshot("/redactor/apps/inquiry/inquiry-answers_editor.png", "#datatableFieldDTE_Field_editorFields-answers_modal > div > div");

});