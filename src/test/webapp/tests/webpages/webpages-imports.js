Feature('webpages.webpages-imports');

var randomNumber;
var importGroupName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
   }
});

let fileInput = "#form > div > input[type=file][name=archive]";
let zipFileLocation = "tests/webpages/demotest.webjetcms.sk-export.zip";
Scenario('Over importovanie web stránok zo zip súboru (XML)', async ({ I, DT, DTE }) => {

    importGroupName = "Zip-Import-autotest-" + randomNumber;

    I.closeOtherTabs();
    I.switchTo();

    I.say("Preparing folder phase");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    createTestFolder(I, DTE, DT, importGroupName);
    const folderId = await prepareImportWindow(I, importGroupName);

    I.say("Choose ZIP import");
    I.waitForElement("form.importExportTable");
    I.click("#type3"); //aka import web pages from zip file (xml)
    I.click("#btnOk");

    I.say("Select ZIP file then press OK");
    I.waitForElement(fileInput);
    I.attachFile(fileInput, zipFileLocation);
    I.click("#btnOk");

    I.say("IMPORT menu - check and selecting options");
    I.waitForElement("#syncForm");
    I.resizeWindow(1920, 1080); //Original small size have problems with clicking on checkboxes

    I.say("Check pre-selected values of info menu");
    I.seeInField("#group_options_info", "0 / 1");
    I.seeInField("#doc_options_info", "2 / 2");
    I.seeInField("#file_options_info", "0 / 1");
    I.seeInField("#banner_options_info", "0 / 0");
    I.seeInField("#inquiry_options_info", "0 / 0");
    I.seeInField("#galleryInfo_options_info", "0 / 0");
    I.seeInField("#galleryImage_options_info", "0 / 0");

    I.say("Check sync table");
    I.seeElement( locate("table.sort_table").find("input.btnMinus") );
    I.seeElement( locate("table.sort_table").find( locate("tr[data-collapse-id='59160']").withText("Import_Export") ) );
    I.seeElement( locate("table.sort_table").find( locate("tr[data-collapse-id='59160']").withText("Import_Export_Second") ) );

    I.say("Test collapse logic");
    I.clickCss("button#closeAllFoldersBtn");
    I.dontSeeElement( locate("table.sort_table").find("input.btnMinus") );
    I.dontSeeElement(locate("table.sort_table").find ( locate("tr[data-collapse-id='59160']").withText("Import_Export") ), 5);
    I.dontSeeElement(locate("table.sort_table").find ( locate("tr[data-collapse-id='59160']").withText("Import_Export_Second") ), 5);

    I.clickCss("button#openAllFoldersBtn");
    I.dontSeeElement( locate("table.sort_table").find("input.btnPlus") );
    I.seeElement(locate("table.sort_table").find ( locate("tr[data-collapse-id='59160']").withText("Import_Export") ), 5);
    I.seeElement(locate("table.sort_table").find ( locate("tr[data-collapse-id='59160']").withText("Import_Export_Second") ), 5);

    I.say("Uncheck main page so we import only second page");
    let primaryPageCheckbox = locate("#syncForm").find("input.doc-checkbox[data-id='doc_73280']");
    I.waitForElement(primaryPageCheckbox, 10);
    I.uncheckOption(primaryPageCheckbox);
    I.click("#btnOk");

    I.say("Check returned message's");
    I.waitForText("Synchronizujem priečinok: - / -");
    I.waitForText("Synchronizujem web stránku: 1 / 1");
    I.waitForText("Synchronizujem súbor: - / -");
    I.waitForText("Synchronizujem banner: - / -");
    I.waitForText("Synchronizujem anketu: - / -");
    I.waitForText("Synchronizujem informáciu galérie: - / -");
    I.waitForText("Synchronizujem obrázok galérie: - / -");

    I.say("Check values of info menu after import");
    I.seeInField("#group_options_info", "0 / 1");
    //Here was chnged value from 2/2 to 1/2, because we imported one page, so only one left to import
    I.seeInField("#doc_options_info", "1 / 2");
    I.seeInField("#file_options_info", "0 / 1");
    I.seeInField("#banner_options_info", "0 / 0");
    I.seeInField("#inquiry_options_info", "0 / 0");
    I.seeInField("#galleryInfo_options_info", "0 / 0");
    I.seeInField("#galleryImage_options_info", "0 / 0");

    I.say("Check that pages are marked corrrectlz in  table");
    I.seeCheckboxIsChecked(primaryPageCheckbox);
    I.dontSeeCheckboxIsChecked(locate("#syncForm").find("input.doc-checkbox[data-id='doc_73362']"));

    I.say("Do all ZIP import checks");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + folderId);

    I.say("Check that second page was imported correctly");
    checkBody(I, DT, DTE, "Import_Export_Second", "This is SECOND page", true);

    I.say("Check that main page is unchanged");
    DT.filterEquals("title", importGroupName);
    I.say("Filter main page");
    DT.filterSelect("editorFields.statusIcons", "Hlavná stránka");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('BUG - test multidomain - cant see other domain data during import', async ({ I, DT, DTE, Document }) => {
    I.say("Switch domqain to www.novy-podadresar.sk");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    Document.switchDomain("www.novy-podadresar.sk");

    I.wait(2);

    I.say('Create groups with same name and location');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    createTestFolder(I, DTE, DT, importGroupName);
    const folderId = await prepareImportWindow(I, importGroupName);

    I.say("Choose ZIP import");
    I.waitForElement("form.importExportTable");
    I.click("#type3"); //aka import web pages from zip file (xml)
    I.click("#btnOk");

    I.say("Select ZIP file then press OK");
    I.waitForElement(fileInput);
    I.attachFile(fileInput, zipFileLocation);
    I.click("#btnOk");

    I.say("Now verify that other domain data are not visible");
    I.seeInField("#doc_options_info", "2 / 2");
    I.seeCheckboxIsChecked(locate("#syncForm").find("input.doc-checkbox[data-id='doc_73280']"));
    I.seeCheckboxIsChecked(locate("#syncForm").find("input.doc-checkbox[data-id='doc_73362']"));
    I.seeElement( locate("#syncForm > table.sort_table > tbody > tr:nth-child(4)").find( locate("span").withText("Lokálne neexistuje") ) );
    I.seeElement( locate("#syncForm > table.sort_table > tbody > tr:nth-child(5)").find( locate("span").withText("Lokálne neexistuje") ) );
});

Scenario('Over importovanie web stránok zo zip súboru (XML) - DELETE (other domain)', async ({ I, DT, DTE }) => {
    I.say("Remove group");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    await deleteFolderPermanently(importGroupName, I, DT, DTE);
});

Scenario("logout", ({ I }) => {
    I.logout();
});

Scenario('Over importovanie web stránok zo zip súboru (XML) - DELETE', async({ I, DT, DTE  }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    await deleteFolderPermanently(importGroupName, I, DT, DTE );
});

Scenario('Over importovanie web stránok z Excel súboru', async ({ I, DT, DTE }) => {
    importGroupName = "Excel-Import-autotest-" + randomNumber;
    let fileInput = "#xlsImportForm > table > tbody > tr:nth-child(1) > td:nth-child(2) > input[type=file]";
    let excelFileLocation = "tests/webpages/import_struct.xls";

    //Preparing folder phase
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    createTestFolder(I, DTE, DT, importGroupName);
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
    DT.waitForLoader();

    //Main imported folder
    I.jstreeClick("Aktuality");
    DT.filterContains("title", "Aktuality");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    //Sub folder A
    I.jstreeClick("Aktualita_A");
    DT.filterContains("title", "Aktualita_A");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    //Sub folder B
    I.jstreeClick("Aktualita_B");
    DT.filterContains("title", "Aktualita_B");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    //

});

Scenario('Over importovanie web stránok z Excel súboru - DELETE', async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    await deleteFolderPermanently(importGroupName, I, DT, DTE);
});

function createTestFolder(I, DTE, DT, importGroupName) {
    I.say("Create tmp folder where we will import pages");
    I.click(DT.btn.tree_add_button);
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
    DT.filterContains("title", pageName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.click("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dt-scroll > div.dt-scroll-head > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(DT.btn.edit_button);
    DTE.waitForEditor();

    within({frame: ["#cke_data > div >div > iframe.cke_reset"]}, () => {
        if(shouldBeThere) {
            I.see(pageBody);
        } else {
            I.dontSee(pageBody);
        }
    });
    DTE.cancel();
}

async function deleteFolderPermanently(importGroupName, I, DT, DTE) {
    I.jstreeWaitForLoader();
    I.jstreeClick(importGroupName);
    let groupId = await I.grabValueFrom("#tree-folder-id");

    I.say("Delete folder - soft delete");
    deleteFolder(I, DT, DTE, importGroupName);

    I.say("Delete folder - hard delete");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    I.fillField("#tree-folder-id", groupId);
    I.pressKey('Enter');
    DT.waitForLoader();
    I.jstreeWaitForLoader();

    I.say("Delete folder");
    deleteFolder(I, DT, DTE, importGroupName);
}

function deleteFolder(I, DT, DTE, importGroupName) {
    I.say("Delete folder");
    I.jstreeWaitForLoader();
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.see(importGroupName, "div.DTE_Action_Remove .DTE_Body_Content");
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.see(importGroupName, "div.DTE_Action_Remove .DTE_Body_Content");
    I.click('Zmazať', "div.DTE_Action_Remove .DTE_Form_Buttons");
    DTE.waitForLoader();
    DTE.waitForModalClose("groups-datatable_modal");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
}