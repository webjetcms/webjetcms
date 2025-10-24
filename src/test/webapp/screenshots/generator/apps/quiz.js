Feature('apps.quiz');

Before(({ login }) => {
    login('admin');
});

var quizQuestions = "#datatableFieldDTE_Field_quizQuestions_";
var quizResults = "#datatableFieldDTE_Field_quizResults_";

Scenario("Test quiz stat sekcie", async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/quiz/admin/");
    Document.screenshot("/redactor/apps/quiz/quiz-datatable.png");

    DT.filterContains("name", "Answer_quiz");
    I.clickCss("button.buttons-select-all.dt-filter-id");
    Document.screenshotElement("button.buttons-statistics", "/redactor/apps/quiz/quizStat-button.png");

    I.click("Answer_quiz");
    DTE.waitForEditor("quizDataTable");

    Document.screenshotElement("#quizDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/quiz/quiz-editor.png");

    I.clickCss("#pills-dt-quizDataTable-questions-tab");
    I.clickCss("#quizDataTable_modal .ti.ti-arrows-maximize");
    Document.screenshotElement("#quizDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/quiz/quizQuestion-datatable_A.png", 1500, 700);
    I.clickCss("#quizDataTable_modal .ti.ti-arrows-minimize");
    I.clickCss(quizQuestions + "wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.buttons-create");
    I.waitForElement(quizQuestions + "modal", 10);
    Document.screenshotElement("#datatableFieldDTE_Field_quizQuestions_modal > div > div.DTE_Action_Create", "/redactor/apps/quiz/quizQuestion-editor_A.png", 1000, 800);

    I.amOnPage("/apps/quiz/admin/?id=7");
    DTE.waitForEditor("quizDataTable");
    I.clickCss("#pills-dt-quizDataTable-questions-tab");
    I.clickCss("#quizDataTable_modal .ti.ti-arrows-maximize");
    Document.screenshotElement("#quizDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/quiz/quizQuestion-datatable_B.png", 1500, 700);
    I.clickCss("#quizDataTable_modal .ti.ti-arrows-minimize");
    I.clickCss(quizQuestions + "wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.buttons-create");
    I.waitForElement(quizQuestions + "modal", 10);
    Document.screenshotElement("#datatableFieldDTE_Field_quizQuestions_modal > div > div.DTE_Action_Create", "/redactor/apps/quiz/quizQuestion-editor_B.png", 1000, 800);

    I.amOnPage("/apps/quiz/admin/?id=7");
    I.clickCss("#pills-dt-quizDataTable-results-tab");
    Document.screenshotElement("#quizDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/quiz/quizResults-datatable.png");
    I.clickCss(quizResults + "wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.buttons-create");
    Document.screenshotElement("#datatableFieldDTE_Field_quizResults_modal > div > div.DTE_Action_Create", "/redactor/apps/quiz/quizResults-editor.png");

    I.amOnPage("/apps/quiz/admin/stat/?id=2");
    I.waitForVisible("#quizStat-correctAnswers");
    Document.screenshotElement("#pills-quizStat", "/redactor/apps/quiz/quizStat-header.png");
    if("en" === I.getConfLng()) {
        I.fillField({css: "input.dt-filter-to-dayDate"}, "12/23/2023");
    } else {
        I.fillField({css: "input.dt-filter-to-dayDate"}, "20.12.2023");
    }
    I.clickCss("button.dt-filtrujem-dayDate");
    Document.screenshot("/redactor/apps/quiz/quizStat.png", 1400, 1100);
});