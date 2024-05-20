Feature('apps.inquiry');

Before(({ I, login }) => {
    login('admin');
});



Scenario('inquiry screeny', async ({I, DT, DTE, Document}) => {
    let confLng = I.getConfLng();

    if("sk" === confLng) {
        I.amOnPage("/apps/anketa/?NO_WJTOOLBAR=true");
        Document.screenshot("/redactor/apps/inquiry/inquiry-example.png");
    
        I.amOnPage("/apps/inquiry/admin/");
        Document.screenshot("/redactor/apps/inquiry/inquiry-dataTable.png");
    
        DT.filter("questionText", "Ako sa vám páči WebJET");
        I.click("Ako sa vám páči WebJET");
        DTE.waitForEditor("inquiryDataTable");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_basic.png");
    
        I.click("#pills-dt-inquiryDataTable-advanced-tab");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_advanced.png");
    
        I.click("#pills-dt-inquiryDataTable-answers-tab");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_answers.png");
    
        I.click("#datatableFieldDTE_Field_editorFields-answers_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
        Document.screenshot("/redactor/apps/inquiry/inquiry-answers_editor.png", "#datatableFieldDTE_Field_editorFields-answers_modal > div > div");
    } else {
        I.amOnPage("/apps/anketa/poll.html?NO_WJTOOLBAR=true");
        Document.screenshot("/redactor/apps/inquiry/inquiry-example.png");
    
        I.amOnPage("/apps/inquiry/admin/");
        Document.screenshot("/redactor/apps/inquiry/inquiry-dataTable.png");
    
        DT.filter("questionText", "How do you like WebJET");
        I.click("How do you like WebJET");
        DTE.waitForEditor("inquiryDataTable");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_basic.png");
    
        I.click("#pills-dt-inquiryDataTable-advanced-tab");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_advanced.png");
    
        I.click("#pills-dt-inquiryDataTable-answers-tab");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_answers.png");
    
        I.click("#datatableFieldDTE_Field_editorFields-answers_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
        Document.screenshot("/redactor/apps/inquiry/inquiry-answers_editor.png", "#datatableFieldDTE_Field_editorFields-answers_modal > div > div");
    }
});