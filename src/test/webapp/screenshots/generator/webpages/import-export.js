Feature('webpages.import-export');

let zipFileInput = "#form input[type=file]";
let excelFileInput = "#xlsImportForm > table > tbody > tr:nth-child(1) > td:nth-child(2) > input[type=file]";

let zipFileLocation = null;
let excelFileLocation = "tests/webpages/import_struct.xls";

Before(({ login, I }) => {
    login('admin');
    if (zipFileLocation==null) zipFileLocation = "tests/webpages/"+I.getDefaultDomainName()+"-export.zip"
});

Scenario('Screens', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59160");
    Document.screenshotElement("button.buttons-import-export", "/redactor/webpages/import-export-button.png");
    let confLng = I.getConfLng();

    I.amOnPage("/components/import_web_pages/import_pages.jsp?groupId=59160");
    Document.screenshot("/redactor/webpages/import-export-window.png");

    I.checkOption("#type5");
    I.clickCss("#btnOk");

    if("sk" === confLng) {
        I.waitForElement( locate("h2").withText("Hotovo"), 60 );
    } else if("en" === confLng) {
        I.waitForElement( locate("h2").withText("Done"), 60 );
    } else if("cs" === confLng) {
        I.waitForElement( locate("h2").withText("Hotovo"), 60 );
    }

    Document.screenshot("/redactor/webpages/exported-window.png");

    I.amOnPage("/components/import_web_pages/import_pages.jsp?groupId=59160");
    I.checkOption("#type3");
    I.clickCss("#btnOk");
    I.waitForElement(zipFileInput);
    Document.screenshot("/redactor/webpages/import-zip-window.png");
    I.attachFile(zipFileInput, zipFileLocation);
    I.clickCss("#btnOk");
    I.waitForElement("#syncForm");
    Document.screenshot("/redactor/webpages/imported-zip-window.png");

    Document.screenshotElement("#selectAllBtn", "/redactor/webpages/selectAllBtn.png");
    Document.screenshotElement("#deselectAllBtn", "/redactor/webpages/deselectAllBtn.png");
    Document.screenshotElement("#closeAllFoldersBtn", "/redactor/webpages/closeAllFoldersBtn.png");
    Document.screenshotElement("#openAllFoldersBtn", "/redactor/webpages/openAllFoldersBtn.png");

    I.amOnPage("/components/import_web_pages/import_pages.jsp?groupId=59160");
    I.waitForElement("#dialogCentralRow");
    I.clickCss("#type2");
    I.clickCss("#btnOk");
    I.waitForElement("#xlsImportForm");
    I.fillField("#xlsImportForm input[name=parentGroupIdString]", "59160");
    Document.screenshot("/redactor/webpages/import-excel-window.png");
    I.waitForElement(excelFileInput);
    I.attachFile(excelFileInput, excelFileLocation);
    I.clickCss("#btnOk");

    if("sk" === confLng) {
        I.waitForText("Import dokončený", 120);
    } else if("en" === confLng) {
        I.waitForText("Import done", 120);
    } else if("cs" === confLng) {
        I.waitForText("Import dokončen", 120)
    }

    Document.screenshot("/redactor/webpages/imported-excel-window.png");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59160");

    //Delete
    I.click( locate("a.jstree-anchor").withText("Aktuality") );
    I.say("Delete folder");
    I.click(DT.btn.tree_delete_button);
    DT.waitForLoader();
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');

    if("sk" === confLng) {
        I.click('Zmazať');
    } else if("en" === confLng) {
        I.click('Delete');
    } else if("cs" === confLng) {
        I.click('Smazat');
    }
});