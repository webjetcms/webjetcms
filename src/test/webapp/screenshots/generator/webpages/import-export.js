Feature('webpages.import-export');
var delete_folder_button = (locate('.col-auto').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider'));

let zipFileInput = "#form > input[type=file]:nth-child(3)";
let excelFileInput = "#xlsImportForm > table > tbody > tr:nth-child(1) > td:nth-child(2) > input[type=file]";

let zipFileLocation = "tests/webpages/webjet9.tau27.iway.sk-export.zip";
let excelFileLocation = "tests/webpages/import_struct.xls";

Before(({ login }) => {
    login('admin');
});

Scenario('Screens', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59160");
    Document.screenshotElement("button.buttons-import-export", "/redactor/webpages/import-export-button.png");

    I.amOnPage("/components/import_web_pages/import_pages.jsp?groupId=59160");
    Document.screenshot("/redactor/webpages/import-export-window.png");

    I.checkOption("#type5");
    I.click("#btnOk");
    I.waitForElement( locate("h2").withText("Hotovo"), 60 );
    Document.screenshot("/redactor/webpages/exported-window.png");

    I.amOnPage("/components/import_web_pages/import_pages.jsp?groupId=59160");
    I.checkOption("#type3");
    I.click("#btnOk");
    I.waitForElement(zipFileInput);
    Document.screenshot("/redactor/webpages/import-zip-window.png");
    I.attachFile(zipFileInput, zipFileLocation);
    I.click("#btnOk");
    I.waitForElement("#syncForm");
    Document.screenshot("/redactor/webpages/imported-zip-window.png");

    I.amOnPage("/components/import_web_pages/import_pages.jsp?groupId=59160");
    I.waitForElement("#dialogCentralRow");
    I.click("#type2");
    I.click("#btnOk");
    I.waitForElement("#xlsImportForm");
    I.fillField("#xlsImportForm > table > tbody > tr:nth-child(2) > td:nth-child(2) > input[type=text]:nth-child(1)", "59160");
    Document.screenshot("/redactor/webpages/import-excel-window.png");
    I.waitForElement(excelFileInput);
    I.attachFile(excelFileInput, excelFileLocation);
    I.click("#btnOk");
    I.waitForText("Import dokončený");
    Document.screenshot("/redactor/webpages/imported-excel-window.png");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59160");

    //Delete
    I.click( locate("a.jstree-anchor").withText("Aktuality") );
    I.say("Delete folder");
    I.click(delete_folder_button);
    DT.waitForLoader();
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.click('Zmazať');
});