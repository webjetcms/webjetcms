Feature('export-dat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('export-dat', ({ I, Document }) => {
    I.amOnPage("/apps/export-dat/admin/");

    //ExportDat data table
    Document.screenshot("/redactor/apps/export/exportDat-datatable.png");

    //ExportDat editor
    I.click("button.buttons-create");
    I.wait(1);

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/export/exportDat-editor.png");
});