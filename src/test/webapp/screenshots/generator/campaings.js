Feature('campaings');

var randomNumber, entityName;

var excelFile = "tests/apps/dmail/testExcel.xls";
var csvFile = "tests/apps/dmail/testCSV.txt";

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

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");
    I.click("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);
    I.click("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/campaings/editor.png");

    DTE.save();

    I.fillField("input.dt-filter-subject", entityName);
    I.pressKey('Enter', "input.dt-filter-subject");
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.click("#pills-dt-campaingsDataTable-advanced-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/advanced.png");

    I.click("#pills-dt-campaingsDataTable-groupsTab-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/users.png");

    I.click(locate('label').withText('Bankári'));
    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.click("#pills-dt-campaingsDataTable-receivers-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/receivers.png");

    I.click("#datatableFieldDTE_Field_recipientsTab_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    I.wait(1);

    I.click("#DTE_Field_recipientEmail")
    I.fillField("#DTE_Field_recipientEmail", "test1@gmail.com, test2@interway.sk");

    I.click("#DTE_Field_recipientName")
    I.fillField("#DTE_Field_recipientName", "Testovacie emaily");

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/dmail/campaings/raw-import.png");
    I.click("#datatableFieldDTE_Field_recipientsTab_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");

    I.wait(2);

    I.click(locate('#datatableFieldDTE_Field_recipientsTab_wrapper button.btn-import-dialog'));
    Document.screenshotElement("#datatableImportModal div.modal-content", "/redactor/apps/dmail/campaings/xlsx-import.png");

    I.click("#datatableImportModal div.modal-footer button.btn-outline-secondary");

    I.click("#pills-dt-campaingsDataTable-opens-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/opens.png");

    I.click("#pills-dt-campaingsDataTable-clicks-tab");
    Document.screenshotElement("#campaingsDataTable_modal > div > div", "/redactor/apps/dmail/campaings/clicks.png");

    DTE.save();

    //Remove entity
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
});
