Feature('data-deleting');

Before(({ I, login }) => {
    login('admin');
});

Scenario('data-deleting', ({ I, DTE, Document }) => {
    I.amOnPage("/apps/gdpr/admin/data-deleting/");

    //Data deleting data table
    Document.screenshot("/redactor/apps/gdpr/data-deleting-dataTable.png");

    //Data deleting editor
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    
    DTE.waitForEditor("dataDeletingDataTable");

    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/gdpr/data-deleting-editor.png");
});