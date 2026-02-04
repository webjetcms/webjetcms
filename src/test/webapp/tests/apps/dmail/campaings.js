Feature('apps.dmail.campaings');

var randomNumber, entityNameOriginal;
var recipientsWrapper = "#datatableFieldDTE_Field_recipientsTab_wrapper";
var recipientsModal = "#datatableFieldDTE_Field_recipientsTab_modal";
var opensWrapper = "#datatableFieldDTE_Field_opensTab_wrapper";

var excelFile = "tests/apps/dmail/emails-import.xlsx";

Before(({ I, DT, login }) => {
    login('admin');
    I.amOnPage("/apps/dmail/admin/");


    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        entityNameOriginal = "name-autotest-" + randomNumber;
        console.log(entityNameOriginal);
    }

    DT.addContext('recipients','#datatableFieldDTE_Field_recipientsTab_wrapper');
});

Scenario('campaings-zakladne testy @baseTest', ({I, DTE}) => {

    var entityName = entityNameOriginal+"-zt";

    /* CREATE TEST */
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    //Hidden tabs control
    I.dontSeeElement('Otvorenia');
    I.dontSeeElement('Kliknutia');

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);

    I.clickCss("button.btn-vue-jstree-item-edit");

    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');
    DTE.save();

    I.fillField("input.dt-filter-subject", entityName);
    I.pressKey('Enter', "input.dt-filter-subject");
    I.see(entityName);
    I.see('Produktová stránka - B verzia');

    /* EDIT TEST */
    I.click(entityName);

    //Hidden tabs control - now showed tabs
    I.dontSeeElement('Otvorenia');
    I.dontSeeElement('Kliknutia');

    entityName += ".changed"

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);
    DTE.save();

    I.fillField("input.dt-filter-subject", entityName);
    I.pressKey('Enter', "input.dt-filter-subject");
    I.see(entityName);

    /* DELETE TEST */
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");

    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(entityName);
});

Scenario('campaings-XLS import testy', ({I, DT, DTE}) => {

    var entityName = entityNameOriginal+"-xls";

    /* PREPARE ENTITY */
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);
    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');
    DTE.save();
    DT.filterContains("subject", entityName);

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.clickCss(recipientsWrapper + " button.btn-import-dialog");
    DTE.waitForModal("datatableImportModal");

    I.attachFile('#insert-file', excelFile);
    I.waitForEnabled("#submit-import", 5);
    I.clickCss("#submit-import");

    DT.waitForLoader();

    //Check inserted names, records and statuses
    I.waitForText("Import 06a", 15);
    I.see("Import 06b");
    DTE.cancel();

    //Test changing status
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    DTE.save();
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.see("Import 06a");
    I.see("Import 06b");

    /* DELETE TEST */
    DTE.cancel();
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
 });

 Scenario('campaings-RAW import testy', ({I, DT, DTE}) => {

    var entityName = entityNameOriginal+"-raw";

    /* PREPARE ENTITY */
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);
    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');
    DTE.save();

    DT.filterContains("subject", entityName);
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    //First import WITHOUT set default custom name
    I.clickCss(recipientsWrapper + " > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    I.waitForElement(recipientsModal);
    I.wait(1);
    var importEmails = "vipklient@balat.sk, admin@balat.sk"
    I.clickCss("#DTE_Field_recipientEmail")
    I.fillField("#DTE_Field_recipientEmail", importEmails);
    I.clickCss(recipientsModal + " > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");

    //Chceck inserted emails
    I.see("VIP Klient");
    I.see("WebJET Administrátor");

    //First import WITH set default custom name
    I.clickCss(recipientsWrapper + " > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    I.wait(1);
    importEmails = "vipklient@balat.sk, partner@balat.sk"
    I.clickCss("#DTE_Field_recipientEmail")
    I.fillField("#DTE_Field_recipientEmail", importEmails);
    I.clickCss("#DTE_Field_recipientName")
    var customName = "Custom Name";
    I.fillField("#DTE_Field_recipientName", customName);
    I.clickCss(recipientsModal + " > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");

    I.see("VIP Klient");
    I.see("WebJET Administrátor");

    I.wait(1);

    //Duplicity check
    I.fillField("input.dt-filter-recipientEmail", "vipklient@balat.sk");
    I.clickCss("button.dt-filtrujem-recipientEmail");
    I.wait(1);
    I.see("VIP Klient");
    I.dontSee(customName);

    I.wait(1);

    //Insert check
    I.fillField("input.dt-filter-recipientEmail", "partner@balat.sk");
    I.clickCss("button.dt-filtrujem-recipientEmail");
    I.wait(1);
    I.see(customName);

    //Check that insert is temporal
    I.dontSee("ULOŽENÝ");
    I.fillField("input.dt-filter-recipientEmail", ""); //Erase filter params
    I.clickCss("button.dt-filtrujem-recipientEmail");
    I.clickCss("button.btn-close-editor", "div.DTE_Footer");

    //If we close (withou saving) and open same editor, records must be still there and saved as TEMPORAL
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    I.see("Custom Name");
    I.see("VIP Klient");
    I.see("WebJET Administrátor");
    I.dontSeeElement("ULOŽENÝ");

    //If we save editor and open it again, records must be there and saved sa PERNAMENT
    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    I.see("Custom Name");
    I.see("VIP Klient");
    I.see("WebJET Administrátor");
    I.dontSeeElement("NEULOŽENÝ");

    //Remove entity
    I.clickCss("button.btn-close-editor", "div.DTE_Footer");
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("campaingsDataTable");
    I.toastrClose();
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(entityName);
 });

 Scenario('testy skupiny pouzivatelov @baseTest', ({I, DT, DTE}) => {

    var entityName = entityNameOriginal+"-ug";
    DT.waitForLoader();

    /* PREPARE ENTITY */
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);
    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');
    DTE.save();

    //Try add users by group
    DT.filterContains("subject", entityName);
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.wait(1);

    I.waitForElement(locate('#pills-dt-campaingsDataTable-groupsTab label.form-check-label').withText('Bankári'), 5);
    I.click(locate('#pills-dt-campaingsDataTable-groupsTab label.form-check-label').withText('Bankári'));
    I.wait(1);
    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.wait(1);

    I.dontSeeElement("NEULOŽENÝ");
    DT.filterContains('recipientName', 'Lukáč');
    I.see("Filip Lukáč");
    DT.filterContains('recipientName', 'Pavlík');
    I.see("Matej Pavlík");

    DT.clearFilter('recipientName');
    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.click(locate('label').withText('Bankári'));
    I.wait(0.5);
    I.click(locate('label').withText('Obchodní partneri'));
    I.wait(0.5);
    I.click(locate('label').withText('VIP Klienti'));
    I.wait(0.5);
    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.wait(1);
    DT.waitForLoader("datatableFieldDTE_Field_recipientsTab");

    I.dontSee("Filip Lukáč");
    I.dontSee("Matej Pavlík");

    I.see("VIP Klient");
    I.see("Obchodny Partner");

    //Can't see Meno Priezvisko and TestUser Test, because they are not valid
    I.dontSee("Meno Priezvisko");
    I.dontSee("TestUser Test");

    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.wait(1);
    I.click(locate('label').withText('Obchodní partneri'));
    I.wait(0.5);
    I.click(locate('label').withText('VIP Klienti'));
    I.wait(0.5);
    DTE.save();

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.wait(1);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Remove entity
    I.clickCss("button.btn-close-editor", "div.DTE_Footer");
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(entityName);
});

Scenario('BUG pocty prijemcov', ({I, DTE}) => {

    var entityName = entityNameOriginal+"-prijemcovia";
    //It's 3 not 2 BUT one of them is invalid
    var pocetPrijemcovNewsletter = 2;
    // SOOO VianocnePozdravy got 3 emails. One is invalid, second is valid BUT is already in newsletter group, third is valid and only one not in newsletter group
    var pocetPrijemcovVianocnePozdravy = 1;

    /* CREATE TEST */
    I.clickCss("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("campaingsDataTable");

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);

    I.clickCss("button.btn-vue-jstree-item-edit")

    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Testovaci newsletter');

    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");

    I.click("Newsletter", "div.DTE_Field_Name_editorFields\\.emails");

    //TODO - after CodeceptJS update start using withTextEquals that finds EXACT text not just LIKE match
    //I.click( locate("div").withChild( locate("label").withTextEquals('Newsletter') ).find("input") );

    DTE.save();

    //
    overPocetPrijemcov(I, entityName, pocetPrijemcovNewsletter);

    // IN the end, number of recipients must be 3
    I.say("Pridam skupinu Vianocne pozdravy, overim pocet prijemcov");
    I.click(entityName);
    I.dtWaitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.click("Vianočné pozdravy", "div.DTE_Field_Name_editorFields\\.emails");
    DTE.save();

    overPocetPrijemcov(I, entityName, pocetPrijemcovNewsletter+pocetPrijemcovVianocnePozdravy);

    //
    I.say("Odoberem skupinu Vianocne pozdravy, overim pocet prijemcov");
    I.click(entityName);
    I.dtWaitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.click("Vianočné pozdravy", "div.DTE_Field_Name_editorFields\\.emails");
    DTE.save();

    overPocetPrijemcov(I, entityName, pocetPrijemcovNewsletter);

    /* DELETE TEST */
    I.amOnPage("/apps/dmail/admin/");
    I.dtFilter("subject", entityName)
    I.dtWaitForLoader();
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(entityName);
});

Scenario('zobrazenie nahladu emailu', ({I, DT}) => {
    DT.waitForLoader();
    DT.filterContains("subject", "Testovaci email");
    I.click("Testovaci email");
    I.dtWaitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-preview-tab");
    I.wait(1);
    I.switchTo("#emailPreviewIframe");
    I.waitForElement("div.container", 5);
    I.see("Formular", "div.container");
    I.switchTo();

    I.dtEditorCancel();

    //
    DT.filterContains("subject", "Test_1");
    I.click("Test_1");
    I.dtWaitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-preview-tab");
    I.wait(1);
    I.switchTo("#emailPreviewIframe");
    I.waitForElement("div.container", 5);
    I.see("Lorem ipsum", "p.text-center");
    I.dontSee("Formular", "div.container");
    I.switchTo();

    //
    I.say("Zmenim stranku a mal by sa zmenit aj text");
    I.clickCss("#pills-dt-campaingsDataTable-main-tab");
    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Testovaci newsletter');

    I.clickCss("#pills-dt-campaingsDataTable-preview-tab");
    I.wait(1);
    I.switchTo("#emailPreviewIframe");
    I.see("Vážený pán", "td h1");
    I.see("Testovací newsletter");
    I.switchTo();

    I.dtEditorCancel();

    //
    I.say("Vyskusam novy email");
    I.clickCss("button.buttons-create");
    I.dtWaitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-preview-tab");
    I.wait(1);
    I.switchTo("#emailPreviewIframe");
    I.dontSee("Formular");
    I.dontSee("Lorem ipsum");
    I.dontSee("Testovací newsletter");
    I.switchTo();

    I.dtEditorCancel();
});

Scenario('Test sort in inner tables', ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");
    DT.filterContains("subject", "Test_filtrovania_a_sortovania");

    I.click("Test_filtrovania_a_sortovania");
    DTE.waitForEditor("campaingsDataTable");

    //Check
    I.see("Test_filtrovania_a_sortovania");

    /* TAB receivers */
        I.say("Tab receivers");
        I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
        I.see("recipient_A");
        I.see("recipient_B");

        I.say("Default sorted by sendDate desc")
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_A");

        I.say("sort by ID ASC");
        I.click( locate(recipientsWrapper).find("th.dt-th-id") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_B");

        I.say("sort by ID DESC");
        I.click( locate(recipientsWrapper).find("th.dt-th-id") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_A");

        I.say("sort by sendDate ASC");
        I.click( locate(recipientsWrapper).find("th.dt-th-sentDate") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_B");

        I.say("sort by sendDate DESC");
        I.click( locate(recipientsWrapper).find("th.dt-th-sentDate") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_A");

    /* TAB opens*/
        I.say("Tab opens");
        I.clickCss("#pills-dt-campaingsDataTable-opens-tab");
        I.see("recipient_A");
        I.see("recipient_B");

        I.say("Default sorted by ID desc");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_A");

        I.say("sort by seenDate DESC")
        I.click( locate(opensWrapper).find("th.dt-th-seenDate") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_A");

        I.say("sort by seenDate ASC");
        I.click( locate(opensWrapper).find("th.dt-th-seenDate") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_B");

        I.say("sort by ID ASC");
        I.click( locate(opensWrapper).find("th.dt-th-id") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_B");

        I.say("sort by ID DESC");
        I.click( locate(opensWrapper).find("th.dt-th-id") );
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_A");
});

Scenario("Duplicity check", ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");
    DT.waitForLoader();
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    //
    I.say("Checking unsubscribed email");
    addEmail(I, DTE, "", "test@temp-mail.org");
    I.see("Nenašli sa žiadne vyhovujúce záznamy", recipientsWrapper);

    addEmail(I, DTE, "", "test1@temp-mail.org");
    I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    //
    I.say("Checking duplicity email");
    addEmail(I, DTE, "", "TEST1@temp-mail.org");
    I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    I.amOnPage("/apps/dmail/admin/?id=2120");
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    //
    I.say("Checking unsubscribed email");
    addEmail(I, DTE, "", "test@temp-mail.org");
    I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    addEmail(I, DTE, "", "test1@temp-mail.org");
    I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    I.toastrClose();
    I.toastrClose();

    //
    I.say("Checking duplicity email");
    addEmail(I, DTE, "", "TEST1@temp-mail.org");
    I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    I.amOnPage("/apps/dmail/admin/?id=2120");
    DTE.waitForEditor("campaingsDataTable");

    //
    I.say("Selecting groups with multiple vipklient@balat.sk email");
    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.checkOption("Bankári", "#pills-dt-campaingsDataTable-groupsTab");
    I.checkOption("VIP Klienti", "#pills-dt-campaingsDataTable-groupsTab");
    DTE.save();
    I.click("Duplicity check campaign");
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    DT.filterContains("recipientEmail", "vipklient");
    I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    //deselect groups
    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.uncheckOption("Bankári", "#pills-dt-campaingsDataTable-groupsTab");
    I.uncheckOption("VIP Klienti", "#pills-dt-campaingsDataTable-groupsTab");
    DTE.save();
});

Scenario("Domain separation check", ({I, DT, Document}) => {
    I.amOnPage("/apps/dmail/admin/");

    DT.filterContains("subject", "test_domain_filter");
    DT.checkTableRow("campaingsDataTable", 1, ["2476", "test_domain_filter_webjet9"]);
    I.dontSee("test_domain_filter_test23");

    //Change domain
    Document.switchDomain("test23.tau27.iway.sk");
    DT.filterContains("subject", "test_domain_filter");
    DT.checkTableRow("campaingsDataTable", 1, ["2477", "test_domain_filter_test23"]);
    I.dontSee("test_domain_filter_webjet9");
});

Scenario('logout', async ({I}) => {
    I.logout();
});

Scenario("BUG check - disabling buttons + emails/stat delete", ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");

    DT.filterContains("subject", "test_domain_filter");
    I.click("test_domain_filter_webjet9");
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.waitForElement("#pills-dt-campaingsDataTable-receivers");

    I.click( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-create") );
    DTE.waitForEditor("datatableFieldDTE_Field_recipientsTab");

    within("#datatableFieldDTE_Field_recipientsTab_modal", () => {
        I.fillField({css: "#DTE_Field_recipientName"}, "testForDelete");
        I.fillField({css: "#DTE_Field_recipientEmail"}, "testForDelete@test.sk");
    });

    DTE.save("datatableFieldDTE_Field_recipientsTab");
    DT.waitForLoader();

    DT.filterContains("recipientName", "testForDelete");

    I.see("testForDelete");
    I.seeElement( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-resend") );

    I.click( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-select-all") );

    I.dontSeeElement( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-resend.disabled") );

    I.click( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-remove") );
    I.waitForElement("div.DTE_Action_Remove");
    I.waitForText("Naozaj chcete zmazať položku?", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.seeElement( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-resend") );
});

Scenario('logout2', async ({I}) => {
    I.logout();
});
/* CHECK new custom constraint MultipleEmails */

Scenario('Check BUG recipients + constraint MultipleEmails', ({ I, DT, DTE}) => {
    let entityName = "someTest_" + randomNumber + "_autotest";
    let recipient = "testBind@balat.sk";
    I.amOnPage("/apps/dmail/admin/");

    I.say("MultipleEmails check");
    prepareCampaignForInsert(I, DTE, entityName);

    //Check constraint
        I.clickCss("#pills-dt-campaingsDataTable-advanced-tab");

        I.say("Only ',' no valid emails");
        I.fillField("#DTE_Field_replyTo", ",,,,");
        DTE.save();
        I.waitForText("Hodnota neobsahuje žiadne emailové adresy", 10);

        I.say("Valid email + invalid email");
        I.fillField("#DTE_Field_replyTo", "test@test.sk,,,,wrongEmail");
        DTE.save();
        I.waitForText("Zadaný email nie je platný : wrongEmail", 10);

        I.say("Ducplicity");
        I.fillField("#DTE_Field_replyTo", "test@test.sk,test@test.sk");
        DTE.save();
        I.waitForText("Hodnota obsahuje duplicitné emailové adresy", 10);

        I.say("More valid emails");
        I.fillField("#DTE_Field_replyTo", "test@test.sk,test2@test.sk");

    //Add recipient
        I.say("Insert recipient");
        I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
        addEmail(I, DTE, "", recipient);

    I.say("Insert of campaing with constraint MultipleEmails is OK");
    DTE.save();

    I.say("Edit campaing and check that recipient is bind to campaing");
        DT.filterContains("subject", entityName);
        I.see(entityName);
        I.click(entityName);
        DTE.waitForEditor("campaingsDataTable");
        I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
        I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    I.say("Remove campaing");
        DTE.cancel();
        I.clickCss("td.dt-select-td.sorting_1");
        I.clickCss("button.buttons-remove");
        DTE.waitForEditor("campaingsDataTable");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.dontSee(entityName);
});

Scenario('FIX recipients adding via Editor and Import', ({ I, DT, DTE}) => {
    //Error in this string (different split symbols)
    let insertRecipient_error = "Test1@test.sk, Test2@test.sk; Test13Wrong Test4@test.sk Test2@test.sk";
    let entityName = "someTest_" + randomNumber + "_autotest";
    let recipientName = "recipientName_" + randomNumber + "_autotest";

    I.amOnPage("/apps/dmail/admin/");

    I.say("Open new Campaign");
        prepareCampaignForInsert(I, DTE, entityName);

    I.say("Try adding recipients via Editor WITHOUT skipping invalid emails");
        I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
        addEmail(I, DTE, recipientName, insertRecipient_error, false, false);
        I.waitForText("Zadaný email nie je platný : Test13Wrong", 5);

    I.say("Check that first 2 emails are inserted - we insert by one not all at once");
        I.click( locate(recipientsModal).find("button.btn-close-editor") );
        I.click( locate(recipientsWrapper).find("button.buttons-refresh") );
        DT.waitForLoader();

        I.seeElement( locate(recipientsWrapper).find("a").withText("test1@test.sk") );
        I.seeElement( locate(recipientsWrapper).find("a").withText("test2@test.sk") );

    I.say("Try adding recipients via Editor WITH skipping invalid emails");
        I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
        addEmail(I, DTE, recipientName, insertRecipient_error, true, true);

    I.waitForElement("#toast-container-webjet > div.toast-warning");
    I.waitForElement( locate("#toast-container-webjet > div.toast-warning > div.toast-message").withText("Zadaný email je duplicitný alebo už existuje: Test1@test.sk") );
    I.waitForElement( locate("#toast-container-webjet > div.toast-warning > div.toast-message").withText("Zadaný email je duplicitný alebo už existuje: Test2@test.sk") );
    I.waitForElement( locate("#toast-container-webjet > div.toast-warning > div.toast-message").withText("Zadaný email nie je platný : Test13Wrong") );
    I.waitForElement( locate("#toast-container-webjet > div.toast-warning > div.toast-message").withText("Zadaný email je duplicitný alebo už existuje: Test2@test.sk") );

    I.toastrClose();

    I.say("Check and delete inserted emails");
        I.click( locate(recipientsWrapper).find("button.buttons-refresh") );
        DT.waitForLoader();

        I.seeElement( locate(recipientsWrapper).find("a").withText("test1@test.sk") );
        I.seeElement( locate(recipientsWrapper).find("a").withText("test2@test.sk") );
        I.seeElement( locate(recipientsWrapper).find("a").withText("test4@test.sk") );

        I.click(locate(recipientsWrapper).find("button.buttons-select-all") );
        I.click(locate(recipientsWrapper).find("button.buttons-remove") );
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");

        I.dontSeeElement( locate(recipientsWrapper).find("a").withText("test1@test.sk") );
        I.dontSeeElement( locate(recipientsWrapper).find("a").withText("test2@test.sk") );
        I.dontSeeElement( locate(recipientsWrapper).find("a").withText("test4@test.sk") );
});

Scenario('FEATURE - before save/action remove all un-subscribed emails', ({ I, DT, DTE}) => {
    let email = "emailToUnsubscibe_" + randomNumber + "@test.sk";

    I.amOnPage('/apps/dmail/admin/?id=2744');
    DTE.waitForEditor("campaingsDataTable");


    I.say("Add email");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    addEmail(I, DTE, "", email);

    I.say("Check email");
    DT.filterEquals("recipientEmail", email);
    I.seeElement( locate(recipientsWrapper).find("a").withText( email.toLowerCase() ) );

    I.say("Unsubscribe email");
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("unsubscribedDataTable");
    I.fillField("#DTE_Field_email", email);
    DTE.save();

    I.say("Check that email is unsubscribed");
    DT.filterContains("email", email);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check that email is STILL visible in campaign - PROBLEM");
    I.amOnPage('/apps/dmail/admin/?id=2744');
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    DT.filterEquals("recipientEmail", email);
    I.seeElement( locate(recipientsWrapper).find("a").withText( email.toLowerCase() ) );

    I.say("DO save and then check that email is no longer visible in campaign");
    DTE.save();
    I.amOnPage('/apps/dmail/admin/?id=2744');
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    DT.filterEquals("recipientEmail", email);
    I.dontSeeElement( locate(recipientsWrapper).find("a").withText( email.toLowerCase() ) );

    I.say("Remove email form un-subscribed");
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    DT.filterContains("email", email);
    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("unsubscribedDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(email);
});

function prepareCampaignForInsert(I, DTE, entityName) {
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    //Needed fields
        I.clickCss("#DTE_Field_subject")
        I.fillField("#DTE_Field_subject", entityName);
        I.clickCss("button.btn-vue-jstree-item-edit");
        I.waitForElement("#jsTree");
        I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
        I.click('Testovaci newsletter');
}

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

function overPocetPrijemcov(I, entityName, pocetPrijemcov) {
    I.say("Over pocet prijemcov");
    I.dtFilter("subject", entityName);
    I.see("0 / "+pocetPrijemcov, "#campaingsDataTable tbody tr td.cell-not-editable");
    I.click(entityName);
    I.dtWaitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.see("Záznamy 1 až "+pocetPrijemcov+" z "+pocetPrijemcov, "#datatableFieldDTE_Field_recipientsTab_info");
    I.dtEditorCancel();
}

Scenario('BUG recipients for new email', ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    let campaign = "BUG-reciepients-autotest-" + randomNumber;
    I.fillField("#DTE_Field_subject", campaign);

    I.clickCss("button.btn-vue-jstree-item-edit");
    I.waitForElement("div#jsTree");

    I.click(locate('.jstree-node.jstree-closed').withText('Aplikácie').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jstree-node.jstree-closed').withText('Hromadný e-mail').find('.jstree-icon.jstree-ocl'));
    I.click( locate("li.jstree-node.jstree-leaf > a.jstree-anchor").withText("Hromadný e-mail") );

    I.click("#pills-dt-campaingsDataTable-receivers-tab");
    I.click( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-create") );

    I.waitForElement( locate("#datatableFieldDTE_Field_recipientsTab_modal").find("div.DTE_Action_Create") );

    let name = "testInsert_" + randomNumber;
    let email = "testInsertEmail" + randomNumber + "@test.sk";

    I.click("#DTE_Field_recipientName");
    I.fillField("#DTE_Field_recipientName", name);
    I.fillField("#DTE_Field_recipientEmail", email);

    DTE.save("datatableFieldDTE_Field_recipientsTab");

    I.see(name);
    DTE.save();

    DT.filterContains("subject", campaign);
    I.click(campaign);
    DTE.waitForEditor("campaingsDataTable");

    I.click("#pills-dt-campaingsDataTable-receivers-tab");
    I.see(name);

    I.clickCss('#datatableFieldDTE_Field_recipientsTab_wrapper button.btn.btn-sm.btn-outline-secondary.buttons-refresh');
    DT.waitForLoader('#datatableFieldDTE_Field_recipientsTab_processing');
    I.click( locate("#pills-dt-campaingsDataTable-receivers").find("button.buttons-select-all") );
    I.click( locate("#pills-dt-campaingsDataTable-receivers").find("button.buttons-remove") );
    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose('datatableFieldDTE_Field_recipientsTab_modal');
    DT.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    DTE.cancel();

    I.click( locate("#campaingsDataTable_wrapper").find("button.buttons-select-all") );
    I.click( locate("#campaingsDataTable_wrapper").find("button.buttons-remove") );
    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('BUG multiple users same email', ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/?id=-1");

    const usersEmail = "test_campaign@balat.sk";
    let entityName = "sameMailBug_" + randomNumber + "_autotest";

    DTE.waitForEditor("campaingsDataTable");

    I.fillField("#DTE_Field_subject", entityName);

    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Testovaci newsletter');
    DTE.save();

    DT.filterEquals("subject", entityName);
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.click("CampaingnTestBug", "div.DTE_Field_Name_editorFields\\.permisions");
    DTE.save();

    DT.filterEquals("subject", entityName);
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    DT.filterEquals("recipientEmail", usersEmail);

    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_info");
    I.see("First Campaign");

    I.click(DT.btn.recipients_add_button);
    DTE.waitForEditor('datatableFieldDTE_Field_recipientsTab')
    DTE.fillField('recipientEmail', usersEmail);
    DTE.save('datatableFieldDTE_Field_recipientsTab');

    I.waitForElement("#toast-container-webjet > div.toast-warning");
    I.waitForElement( locate("#toast-container-webjet > div.toast-warning > div.toast-message").withText("Zadaný email je duplicitný alebo už existuje: " + usersEmail) );
    I.toastrClose();

    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_info");
    I.see("First Campaign");

    I.clickCss("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.click("CampaingnTestBug", "div.DTE_Field_Name_editorFields\\.permisions");
    DTE.save();

    DT.filterEquals("subject", entityName);
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    I.click(DT.btn.recipients_add_button);
    DTE.waitForEditor('datatableFieldDTE_Field_recipientsTab')
    DTE.fillField('recipientEmail', usersEmail);
    DTE.save('datatableFieldDTE_Field_recipientsTab');

    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_info");
    I.see("Third Campaign");
    DTE.cancel();

    I.say("Remove entity");
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(entityName);
});