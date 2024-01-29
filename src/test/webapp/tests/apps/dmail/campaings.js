Feature('apps.dmail.campaings');

var randomNumber, entityNameOriginal;

var excelFile = "tests/apps/dmail/emails-import.xlsx";

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/apps/dmail/admin/");


    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        entityNameOriginal = "name-autotest-" + randomNumber;
        console.log(entityNameOriginal);
    }
});

Scenario('campaings-zakladne testy', ({I, DTE}) => {

    var entityName = entityNameOriginal+"-zt";

    /* CREATE TEST */
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    //Hidden tabs control
    I.dontSeeElement('Otvorenia');
    I.dontSeeElement('Kliknutia');

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);

    I.clickCss("button.btn-vue-jstree-item-edit")

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
    I.click("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
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
    DT.filter("subject", entityName);

    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.clickCss("#datatableFieldDTE_Field_recipientsTab_wrapper button.btn-import-dialog");
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
    I.click("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
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

    DT.filter("subject", entityName);
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    //First import WITHOUT set default custom name
    I.clickCss("#datatableFieldDTE_Field_recipientsTab_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    I.waitForElement("#datatableFieldDTE_Field_recipientsTab_modal");
    I.wait(1);
    var importEmails = "vipklient@balat.sk, admin@balat.sk"
    I.clickCss("#DTE_Field_recipientEmail")
    I.fillField("#DTE_Field_recipientEmail", importEmails);
    I.clickCss("#datatableFieldDTE_Field_recipientsTab_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");

    //Chceck inserted emails
    I.see("VIP Klient");
    I.see("WebJET Administrátor");

    //First import WITH set default custom name
    I.clickCss("#datatableFieldDTE_Field_recipientsTab_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    I.wait(1);
    importEmails = "vipklient@balat.sk, partner@balat.sk"
    I.clickCss("#DTE_Field_recipientEmail")
    I.fillField("#DTE_Field_recipientEmail", importEmails);
    I.clickCss("#DTE_Field_recipientName")
    var customName = "Custom Name";
    I.fillField("#DTE_Field_recipientName", customName);
    I.clickCss("#datatableFieldDTE_Field_recipientsTab_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");

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
    I.click("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(entityName);
 });

 Scenario('testy skupiny pouzivatelov', ({I, DT, DTE}) => {

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
    DT.filter("subject", entityName);
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
    I.see("Filip Lukáč");
    I.see("Matej Pavlík");

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

    I.dontSee("Filip Lukáč");
    I.dontSee("Matej Pavlík");

    I.see("VIP Klient");
    I.see("Meno Priezvisko");

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
    I.click("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(entityName);
});

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

Scenario('BUG pocty prijemcov', ({I, DTE}) => {

    var entityName = entityNameOriginal+"-prijemcovia";
    var pocetPrijemcovNewsletter = 3;
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

    DTE.save();

    //
    overPocetPrijemcov(I, entityName, pocetPrijemcovNewsletter);

    //
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
    I.click("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(entityName);
});

Scenario('zobrazenie nahladu emailu', ({I, DT}) => {
    DT.waitForLoader();
    DT.filter("subject", "test", "Začína na");
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

Scenario('Test sort in inner tables', async ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");
    DT.filter("subject", "Test_filtrovania_a_sortovania");

    I.click("Test_filtrovania_a_sortovania");
    DTE.waitForEditor("campaingsDataTable");

    //Check
    I.see("Test_filtrovania_a_sortovania");

    /* TAB receivers */
        I.say("Tab receivers");
        I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
        I.see("recipient_A");
        I.see("recipient_B");

        //Default sorted by sendDate desc
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_A");

        //sort by ID ASC
        I.click(locate('//*[@id="datatableFieldDTE_Field_recipientsTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[1]'));
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_B");

        //sort by ID DESC
        I.click(locate('//*[@id="datatableFieldDTE_Field_recipientsTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[1]'));
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_A");

        //sort by sendDate ASC
        I.click(locate('//*[@id="datatableFieldDTE_Field_recipientsTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[4]'));
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_B");

        //sort by sendDate DESC
        I.click(locate('//*[@id="datatableFieldDTE_Field_recipientsTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[4]'));
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_recipientsTab", 2, 2, "recipient_A");

    /* TAB opens*/
        I.say("Tab opens");
        I.clickCss("#pills-dt-campaingsDataTable-opens-tab");
        I.see("recipient_A");
        I.see("recipient_B");

        //Default sorted by ID desc
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_A");

        //sort by sendDate DESC
        I.click(locate('//*[@id="datatableFieldDTE_Field_opensTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[4]'));
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_A");

        //sort by sendDate ASC
        I.click(locate('//*[@id="datatableFieldDTE_Field_opensTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[4]'));
        DT.waitForLoader();
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_B");

        //sort by ID ASC
        I.click(locate('//*[@id="datatableFieldDTE_Field_opensTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[1]'));
        DT.waitForLoader();
        I.wait(1);
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_A");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_B");

        //sort by ID DESC
        I.click(locate('//*[@id="datatableFieldDTE_Field_opensTab_wrapper"]/div[2]/div/div/div[1]/div[1]/div/table/thead/tr[1]/th[1]'));
        DT.waitForLoader();
        I.wait(1);
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 1, 2, "recipient_B");
        DT.checkTableCell("datatableFieldDTE_Field_opensTab", 2, 2, "recipient_A");
});

function addEmail(email, I, DTE) {
    I.click("#datatableFieldDTE_Field_recipientsTab_wrapper button.buttons-create");
    DTE.waitForEditor("datatableFieldDTE_Field_recipientsTab");
    I.fillField("#DTE_Field_recipientEmail", email);
    I.clickCss("#datatableFieldDTE_Field_recipientsTab_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");
    I.waitForInvisible("#datatableFieldDTE_Field_recipientsTab_modal");
    I.wait(2);
}

Scenario("Duplicity check", ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");
    DT.waitForLoader();
    I.click("button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    I.click("#pills-dt-campaingsDataTable-receivers-tab");
    //
    I.say("Checking unsubscribed email");
    addEmail("test@temp-mail.org", I, DTE);
    I.see("Nenašli sa žiadne vyhovujúce záznamy", "#datatableFieldDTE_Field_recipientsTab_wrapper");

    addEmail("test1@temp-mail.org", I, DTE);
    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_wrapper");

    //
    I.say("Checking duplicity email");
    addEmail("TEST1@temp-mail.org", I, DTE);
    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_wrapper");

    I.amOnPage("/apps/dmail/admin/?id=2120");
    DTE.waitForEditor("campaingsDataTable");

    I.click("#pills-dt-campaingsDataTable-receivers-tab");
    //
    I.say("Checking unsubscribed email");
    addEmail("test@temp-mail.org", I, DTE);
    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_wrapper");

    addEmail("test1@temp-mail.org", I, DTE);
    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_wrapper");

    //
    I.say("Checking duplicity email");
    addEmail("TEST1@temp-mail.org", I, DTE);
    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_wrapper");

    I.amOnPage("/apps/dmail/admin/?id=2120");
    DTE.waitForEditor("campaingsDataTable");

    //
    I.say("Selecting groups with multiple vipklient@balat.sk email");
    I.click("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.checkOption("Bankári", "#pills-dt-campaingsDataTable-groupsTab");
    I.checkOption("VIP Klienti", "#pills-dt-campaingsDataTable-groupsTab");
    DTE.save();
    I.click("Duplicity check campaign");
    DTE.waitForEditor("campaingsDataTable");
    I.click("#pills-dt-campaingsDataTable-receivers-tab");
    DT.filter("recipientEmail", "vipklient");
    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_recipientsTab_wrapper");

    //deselect groups
    I.click("#pills-dt-campaingsDataTable-groupsTab-tab");
    I.uncheckOption("Bankári", "#pills-dt-campaingsDataTable-groupsTab");
    I.uncheckOption("VIP Klienti", "#pills-dt-campaingsDataTable-groupsTab");
    DTE.save();
});