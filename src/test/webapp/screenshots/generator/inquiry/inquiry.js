Feature('apps.inquiry');

Before(({ I, login }) => {
    login('admin');
});


Scenario('inquiry screeny', async ({I, DT, DTE, Document}) => {
    let confLng = I.getConfLng();

    async function takeScreenshots(languagePath, filterText, clickText) {
        I.amOnPage(languagePath);
        Document.screenshot("/redactor/apps/inquiry/inquiry-example.png");

        I.amOnPage("/apps/inquiry/admin/");
        Document.screenshot("/redactor/apps/inquiry/inquiry-dataTable.png");

        DT.filterContains("questionText", filterText);
        I.click(clickText);
        DTE.waitForEditor("inquiryDataTable");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_basic.png");

        I.clickCss("#pills-dt-inquiryDataTable-advanced-tab");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_advanced.png");

        I.clickCss("#pills-dt-inquiryDataTable-answers-tab");
        Document.screenshot("/redactor/apps/inquiry/inquiry-editor_answers.png");

        I.clickCss("#datatableFieldDTE_Field_editorFields-answers_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
        Document.screenshot("/redactor/apps/inquiry/inquiry-answers_editor.png", "#datatableFieldDTE_Field_editorFields-answers_modal > div > div");
    }

    switch(confLng) {
        case "sk":
            await takeScreenshots("/apps/anketa/?NO_WJTOOLBAR=true", "Ako sa vám páči WebJET", "Ako sa vám páči WebJET");
            break;
        case "en":
            await takeScreenshots("/apps/anketa/poll.html?NO_WJTOOLBAR=true", "How do you like WebJET", "How do you like WebJET");
            break;
        case "cs":
            await takeScreenshots("/apps/anketa/anketa-cs.html?NO_WJTOOLBAR=true", "Jak se vám líbí WebJET?", "Jak se vám líbí WebJET?");
            break;
    }
});