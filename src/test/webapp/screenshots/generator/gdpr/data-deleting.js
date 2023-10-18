Feature('data-deleting');

Before(({ I, login }) => {
    login('admin');
});

Scenario('data-deleting', ({ I, Document }) => {
    I.amOnPage("/apps/gdpr/admin/data-deleting/");

    //Data deleting data table
    Document.screenshot("/redactor/apps/gdpr/data-deleting-dataTable.png");

    //Data deleting editor
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-edit");
    I.wait(1);

    Document.screenshotElement("div.DTE_Action_Edit", "/redactor/apps/gdpr/data-deleting-editor.png");
});
