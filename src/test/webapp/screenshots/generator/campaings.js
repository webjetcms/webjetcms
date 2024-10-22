Feature('campaings');

var randomNumber, entityName;

var excelFile = "tests/apps/dmail/testExcel.xls";
var csvFile = "tests/apps/dmail/testCSV.txt";
var recipientsWrapper = "#datatableFieldDTE_Field_recipientsTab_wrapper";
var recipientsModal = "#datatableFieldDTE_Field_recipientsTab_modal";

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/apps/dmail/admin/");


    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        entityName = "name-autotest-" + randomNumber;
        console.log(entityName);
    }
});

Scenario('campaings', ({ I, DTE, Document }) => {

    //Campaings data table
    Document.screenshot("/redactor/apps/dmail/campaings/dataTable.png");

    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);
    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/campaings/editor.png");

    DTE.save();

    I.fillField("input.dt-filter-subject", entityName);
    I.pressKey('Enter', "input.dt-filter-subject");
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-advanced-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/advanced.png");

    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/users.png");

    I.click(locate('label').withText('Bankári'));
    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/receivers.png");

    I.clickCss("#datatableFieldDTE_Field_recipientsTab_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    I.wait(1);

    I.clickCss("#DTE_Field_recipientEmail")
    I.fillField("#DTE_Field_recipientEmail", "test1@gmail.com, test2@interway.sk");

    I.clickCss("#DTE_Field_recipientName")
    I.fillField("#DTE_Field_recipientName", "Testovacie emaily");

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/campaings/raw-import.png");
    I.clickCss("#datatableFieldDTE_Field_recipientsTab_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");

    I.wait(2);

    I.click(locate(recipientsWrapper + ' button.btn-import-dialog'));
    Document.screenshotElement("#datatableImportModal div.modal-content", "/redactor/apps/dmail/campaings/xlsx-import.png");

    I.clickCss("#datatableImportModal div.modal-footer button.btn-outline-secondary");

    I.clickCss("#pills-dt-campaingsDataTable-opens-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/opens.png");

    I.clickCss("#pills-dt-campaingsDataTable-clicks-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/clicks.png");

    DTE.save();

    //Remove entity
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-remove");

    let confLng = I.getConfLng();
    if("sk" === confLng) {
        I.click("Zmazať", "div.DTE_Action_Remove");
    } else if("en" === confLng) {
        I.click("Delete", "div.DTE_Action_Remove");
    }
});

Scenario('campaings raw import', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/dmail/admin/?id=2763");
    let insertRecipient_error = "Test1@test.sk, Test2@test.sk; Test13Wrong Test4@test.sk Test2@test.sk";

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    addEmail(I, DTE, "Testovacie emaily", insertRecipient_error, false, false);

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/campaings/recipients_editor_err.png");

    I.click( locate(recipientsModal).find("button.btn-close-editor") );
    I.click( locate(recipientsWrapper).find("button.buttons-refresh") );
    DT.waitForLoader();
    I.moveCursorTo("#pills-dt-campaingsDataTable-receivers-tab");

    Document.screenshotElement("#pills-dt-campaingsDataTable-receivers", "/redactor/apps/dmail/campaings/recipients_A.png");

    I.click( locate(recipientsWrapper).find("button.buttons-select-all") );
    I.click( locate(recipientsWrapper).find("button.buttons-remove") );
    I.waitForElement("div.DTE_Action_Remove");
    I.clickCss("div.DTE_Action_Remove div.DTE_Footer div.DTE_Form_Buttons button.btn-primary");

    addEmail(I, DTE, "Testovacie emaily", insertRecipient_error, true, true);

    I.waitForElement("#toast-container-webjet");
    I.moveCursorTo("#toast-container-webjet");
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/dmail/campaings/recipients_notification.png");
    I.click( locate("#toast-container-webjet").find("button.toast-close-button") );
    I.waitForInvisible("#toast-container-webjet");

    Document.screenshotElement("#pills-dt-campaingsDataTable-receivers", "/redactor/apps/dmail/campaings/recipients_B.png");

    I.click( locate(recipientsWrapper).find("button.buttons-select-all") );
    I.click( locate(recipientsWrapper).find("button.buttons-remove") );
    I.waitForElement("div.DTE_Action_Remove");
    I.clickCss("div.DTE_Action_Remove div.DTE_Footer div.DTE_Form_Buttons button.btn-primary");
});

function addEmail(I, DTE, recipientName, recipientEmail, checkModalClose=true, skipWrong = false) {
    I.click(recipientsWrapper + " button.buttons-create");
    DTE.waitForEditor("datatableFieldDTE_Field_recipientsTab");

    I.fillField("#DTE_Field_recipientName", recipientName);
    I.fillField("#DTE_Field_recipientEmail", recipientEmail);

    if(skipWrong) {
        I.checkOption("#DTE_Field_skipWrongEmails_0");
    } else {
        I.uncheckOption("#DTE_Field_skipWrongEmails_0");
    }

    I.clickCss(recipientsModal + "> div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");

    if(checkModalClose) {
        I.waitForInvisible(recipientsModal);
        I.wait(2);
    }
}