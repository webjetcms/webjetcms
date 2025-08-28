Feature('ai.ai-assistants');

Before(({ login }) => {
    login('admin');
});

Scenario('ai-assistants table screenshots', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/settings/ai-assistants/");

    Document.screenshot("/redactor/ai/settings/datatable.png");

    DT.filterEquals("name", "DOC Perex Generator");
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor();

    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-basic-tab.png");

    I.clickCss("#pills-dt-datatableInit-action-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-action-tab.png");

    I.clickCss("#pills-dt-datatableInit-provider-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-provider-tab.png");

    I.clickCss("#pills-dt-datatableInit-instructions-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-instructions-tab.png");

    I.clickCss("#pills-dt-datatableInit-advanced-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-advanced-tab.png");
});