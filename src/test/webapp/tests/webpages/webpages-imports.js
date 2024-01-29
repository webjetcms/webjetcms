Feature('webpages.imports');

var add_folder_button = (locate('.tree-col').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider'));
var delete_folder_button = (locate('.tree-col').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider'));
var edit_webpage_button = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning'));
var randomNumber;
var importGroupName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
   }
});

Scenario('Over importovanie web stránok zo zip súboru (XML)', async ({ I, DT, DTE }) => {
    importGroupName = "Zip-Import-autotest-" + randomNumber;
    let fileInput = "#form > input[type=file]:nth-child(3)";
    let zipFileLocation = "tests/webpages/webjet9.tau27.iway.sk-export.zip";

    //Preparing folder phase
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    createTestFolder(I, DTE, importGroupName);
    const folderId = await prepareImportWindow(I, importGroupName);

    //
    I.say("Choose ZIP import");
    I.waitForElement("#dialogCentralRow");
    I.click("#uniform-type3"); //aka import web pages from zip file (xml)
    I.click("#btnOk");

    I.say("Select ZIP file then press OK");
    I.waitForElement(fileInput);
    I.attachFile(fileInput, zipFileLocation);
    I.click("#btnOk");

    I.say("IMPORT menu check and selecting options");
    I.waitForElement("#syncForm");
    I.seeElement( locate("#syncForm > table.sort_table > tbody > tr:nth-child(3) > td:nth-child(1)").withText("Import_Export") );
    I.seeElement( locate("#syncForm > table.sort_table > tbody > tr:nth-child(4) > td:nth-child(1)").withText("Import_Export_Second") );
    I.seeElement( locate("#syncForm > table.sort_table > tbody > tr:nth-child(4) > td:nth-child(2) > span").withText("Lokálne neexistuje") );

    I.say("Uncheck main page so we import only second page");
    I.uncheckOption("#syncForm > table.sort_table > tbody > tr:nth-child(3) > td:nth-child(3) > div > span > input[type=checkbox]");
    I.click("#btnOk");
    I.wait(2);
    I.waitForElement("#syncForm");
    I.seeElement( locate("#syncForm > table.sort_table > tbody > tr:nth-child(4) > td:nth-child(2) > span").withText("Lokálne už existuje") );

    //
    I.say("Do all ZIP import checks");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + folderId);

    I.say("Check that main page hasn't changed")
    checkBody(I, DT, DTE, importGroupName, "MAIN import page", false);

    I.say("Check that second page was imported correctly");
    checkBody(I, DT, DTE, "Import_Export_Second", "This is SECOND page", true);
});

Scenario('Over importovanie web stránok zo zip súboru (XML) - DELETE', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    deleteSelectedFolder(importGroupName, I, DT, DTE);
});

Scenario('Over importovanie web stránok z Excel súboru', async ({ I, DT, DTE }) => {
    importGroupName = "Excel-Import-autotest-" + randomNumber;
    let fileInput = "#xlsImportForm > table > tbody > tr:nth-child(1) > td:nth-child(2) > input[type=file]";
    let excelFileLocation = "tests/webpages/import_struct.xls";

    //Preparing folder phase
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    createTestFolder(I, DTE, importGroupName);
    const folderId = await prepareImportWindow(I, importGroupName);

    //
    I.say("Choose EXCEL import");
    I.waitForElement("#dialogCentralRow");
    I.click("#type2"); //aka import web pages from excel file
    I.click("#btnOk");

    I.say("Verify import menu");
    I.waitForElement("#xlsImportForm");
    const inportMenuFolderId = await I.grabValueFrom("#xlsImportForm > table > tbody > tr:nth-child(2) > td:nth-child(2) > input[type=text]:nth-child(1)");
    I.assertEqual(folderId, inportMenuFolderId);

    I.say("Insert file and pres OK");
    I.waitForElement(fileInput);
    I.attachFile(fileInput, excelFileLocation);
    I.click("#btnOk");

    I.say("Check importing process");
    I.waitForElement("body.closeTableBody");
    I.see("Importujem súbor, čakajte prosím...");
    I.waitForText("Import dokončený");

    I.say("Do all Excel import checks");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + folderId);
    DT.waitForLoader();

    //Main imported folder
    I.jstreeClick("Aktuality");
    DT.filter("title", "Aktuality");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    //Sub folder A
    I.jstreeClick("Aktualita_A");
    DT.filter("title", "Aktualita_A");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    //Sub folder B
    I.jstreeClick("Aktualita_B");
    DT.filter("title", "Aktualita_B");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    //

});

Scenario('Over importovanie web stránok z Excel súboru - DELETE', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    deleteSelectedFolder(importGroupName, I, DT, DTE);
});

function createTestFolder(I, DTE, importGroupName) {
    I.say("Create tmp folder where we will import pages");
    I.click(add_folder_button);
    DTE.waitForEditor("groups-datatable");
    I.waitForElement("#DTE_Field_groupName");
    I.click("#DTE_Field_groupName");
    I.fillField("#DTE_Field_groupName", importGroupName);
    I.groupSetRootParent();
    DTE.save();
}

async function prepareImportWindow(I, importGroupName) {
    I.say("Import page inside new folder via Excel file");
    I.click( locate("a.jstree-anchor").withText(importGroupName) );
    const folderId = await I.grabValueFrom("#tree-folder-id");
    I.say("'Folder id : " + folderId);
    I.amOnPage("/components/import_web_pages/import_pages.jsp?groupId=" + folderId);
    return folderId;
}

function checkBody(I, DT, DTE, pageName, pageBody, shouldBeThere) {
    DT.filter("title", pageName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.click("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(edit_webpage_button);
    DTE.waitForEditor();

    I.switchTo("#cke_data");
    I.switchTo("iframe.cke_reset");
        if(shouldBeThere) {
            I.see(pageBody);
        } else {
            I.dontSee(pageBody);
        }
    I.switchTo();
    I.switchTo();

    DTE.cancel();
}

function deleteSelectedFolder(importGroupName, I, DT, DTE) {
    I.jstreeClick(importGroupName);

    I.say("Delete folder");
    I.click(delete_folder_button);
    DTE.waitForEditor("groups-datatable");
    I.see(importGroupName, "div.DTE_Action_Remove .DTE_Body_Content");
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.click('Zmazať', "div.DTE_Action_Remove .DTE_Form_Buttons");
    DTE.waitForLoader("groups-datatable");
}