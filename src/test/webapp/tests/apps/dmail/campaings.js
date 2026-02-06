Feature('apps.dmail.campaings');

var randomNumber, entityNameOriginal;
var recipientsWrapper = "#datatableFieldDTE_Field_recipientsTab_wrapper";
var recipientsModal = "#datatableFieldDTE_Field_recipientsTab_modal";
var opensWrapper = "#datatableFieldDTE_Field_opensTab_wrapper";

var excelFile = "tests/apps/dmail/emails-import.xlsx";

var baseEntityName, xlsEntityName, rawEntityName, ugEntityName, prijemcoviaEntityName;

Before(({ I, login, DT }) => {
    login('admin');
    I.amOnPage("/apps/dmail/admin/");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        entityNameOriginal = "name-autotest-" + randomNumber;
        console.log(entityNameOriginal);
    }
    DT.addContext("recipients","#datatableFieldDTE_Field_recipientsTab_wrapper");
    DT.addContext("campaings" ,"#campaingsDataTable_wrapper");
});

Scenario('campaings-zakladne testy @baseTest', ({I, DT, DTE}) => {
    baseEntityName = entityNameOriginal+"-zt";

    I.say("CREATE TEST");
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    //Hidden tabs control
    I.dontSeeElement('Otvorenia');
    I.dontSeeElement('Kliknutia');

    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", baseEntityName);

    DTE.save();

    DT.filterEquals("subject", baseEntityName);
    I.see(baseEntityName);

    I.say("EDIT TEST");
    I.click(baseEntityName);

    //Hidden tabs control - now showed tabs
    I.dontSeeElement('Otvorenia');
    I.dontSeeElement('Kliknutia');

    baseEntityName += ".changed"

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", baseEntityName);
    DTE.save();

    DT.filterEquals("subject", baseEntityName);
    I.see(baseEntityName);
});

Scenario('campaings-base delete', ({I, DT, DTE}) => {
    deleteCampaingByName(I, DT, DTE, baseEntityName);
});

Scenario('campaings-XLS import testy', ({I, DT, DTE}) => {
    xlsEntityName = entityNameOriginal + "-xls";

    /* PREPARE ENTITY */
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", xlsEntityName);

    DTE.save();
    DT.filterContains("subject", xlsEntityName);

    I.click(xlsEntityName);
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
    I.click(xlsEntityName);
    DTE.waitForEditor("campaingsDataTable");
    DTE.save();
    I.click(xlsEntityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.see("Import 06a");
    I.see("Import 06b");
 });

 Scenario('campaings-XLS delete', ({I, DT, DTE}) => {
    deleteCampaingByName(I, DT, DTE, xlsEntityName);
 });

 Scenario('campaings-RAW import testy', ({I, DT, DTE}) => {
    rawEntityName = entityNameOriginal + "-raw";

    /* PREPARE ENTITY */
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", rawEntityName);

    DTE.save();

    DT.filterContains("subject", rawEntityName);
    I.click(rawEntityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    //First import WITHOUT set default custom name
    I.clickCss(recipientsWrapper + " > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success");
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
    I.clickCss(recipientsWrapper + " > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success");
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
    I.click(rawEntityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    I.see("Custom Name");
    I.see("VIP Klient");
    I.see("WebJET Administrátor");
    I.dontSeeElement("ULOŽENÝ");

    //If we save editor and open it again, records must be there and saved sa PERNAMENT
    DTE.save();

    I.click(rawEntityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");

    I.see("Custom Name");
    I.see("VIP Klient");
    I.see("WebJET Administrátor");
    I.dontSeeElement("NEULOŽENÝ");
 });

 Scenario('campaings-RAW delete', ({I, DT, DTE}) => {
    deleteCampaingByName(I, DT, DTE, rawEntityName);
 });

 Scenario('duplicity in groups check', async ({ I, DT, DTE }) => {
    I.click(DT.btn.campaings_add_button);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.click(DT.btn.recipients_group_button);
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");

    let groups = await I.grabTextFromAll("div.custom-control.form-switch label");
    groups = [...groups];
    const hasDuplicates = groups.some((group, index) =>
        groups.indexOf(group) !== index
    );
    I.assertFalse(hasDuplicates, "Existujú duplicity v zozname.");
    I.switchTo();
    I.click(locate("#modalIframe button").withText("OK"));
});

Scenario('testy skupiny pouzivatelov @baseTest', ({I, DT, DTE}) => {
    ugEntityName = entityNameOriginal + "-ug";

    /* PREPARE ENTITY */
    I.click(DT.btn.campaings_add_button);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", ugEntityName);

    DTE.save();

    //Try add users by group
    DT.filterContains("subject", ugEntityName);
    I.click(ugEntityName);
    DTE.waitForEditor("campaingsDataTable");
    editGroups(I, DT, ["Bankári"], []);
    I.wait(1);
    DTE.save();

    I.click(ugEntityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.wait(1);

    I.dontSeeElement("NEULOŽENÝ");
    DT.filterContains('recipientName', 'Lukáč');
    I.see("Filip Lukáč");
    DT.filterContains('recipientName', 'Pavlík');
    I.see("Matej Pavlík");

    DT.clearFilter('recipientName');
    editGroups(I, DT, ['Obchodní partneri', 'VIP Klienti'],['Bankári']);
    DTE.save();

    I.click(ugEntityName);
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

    editGroups(I, DT, [], ['Obchodní partneri', 'VIP Klienti']);
    DTE.save();

    I.click(ugEntityName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.wait(1);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('campaings skupiny delete', ({I, DT, DTE}) => {
    deleteCampaingByName(I, DT, DTE, ugEntityName);
});

Scenario('BUG pocty prijemcov', ({I, DT, DTE}) => {
    prijemcoviaEntityName = entityNameOriginal+"-prijemcovia";

    //It's 3 not 2 BUT one of them is invalid
    var pocetPrijemcovNewsletter = 2;
    // SOOO VianocnePozdravy got 3 emails. One is invalid, second is valid BUT is already in newsletter group, third is valid and only one not in newsletter group
    var pocetPrijemcovVianocnePozdravy = 1;

    /* CREATE TEST */
    I.clickCss("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("campaingsDataTable");

    I.clickCss("button.btn-vue-jstree-item-edit")
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click( locate(".jstree-anchor").withText('Testovaci newsletter') );

    //Set subject
    I.fillField("#DTE_Field_subject", prijemcoviaEntityName);

    editGroups(I, DT, ['Newsletter'], []);

    //TODO - after CodeceptJS update start using withTextEquals that finds EXACT text not just LIKE match
    //I.click( locate("div").withChild( locate("label").withTextEquals('Newsletter') ).find("input") );

    DTE.save();

    //
    overPocetPrijemcov(I, prijemcoviaEntityName, pocetPrijemcovNewsletter);

    // IN the end, number of recipients must be 3
    I.say("Pridam skupinu Vianocne pozdravy, overim pocet prijemcov");
    I.click(prijemcoviaEntityName);
    I.dtWaitForEditor("campaingsDataTable");
    editGroups(I, DT, ["Vianočné pozdravy"], []);
    DTE.save();

    overPocetPrijemcov(I, prijemcoviaEntityName, pocetPrijemcovNewsletter+pocetPrijemcovVianocnePozdravy);

    //
    I.say("Odoberem skupinu Vianocne pozdravy, overim pocet prijemcov");
    I.click(prijemcoviaEntityName);
    I.dtWaitForEditor("campaingsDataTable");
    editGroups(I, DT, [], ["Vianočné pozdravy"]);
    DTE.save();

    overPocetPrijemcov(I, prijemcoviaEntityName, pocetPrijemcovNewsletter);
});

Scenario('campaings pocty prijemcov delete', ({I, DT, DTE}) => {
    deleteCampaingByName(I, DT, DTE, prijemcoviaEntityName);
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
    I.click( locate(".jstree-anchor").withText('Testovaci newsletter') );

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
    editGroups(I, DT, ["Bankári", "VIP Klienti"], []);
    DTE.save();
    I.click("Duplicity check campaign");
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    DT.filterContains("recipientEmail", "vipklient");
    I.see("Záznamy 1 až 1 z 1", recipientsWrapper);

    //deselect groups
    editGroups(I, DT, [], ["Bankári", "VIP Klienti" ]);
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
        I.see("Záznamy 1 až 1 z 1", recipientsWrapper); //TODO recipient sa strati

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

function editGroups(I, DT, groupsToAdd = [], groupsToRemove = []) {
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.click(DT.btn.recipients_group_button);
    I.waitForElement("#modalIframeIframeElement", 10);

    I.switchTo("#modalIframeIframeElement");
    groupsToAdd.forEach((group) => {
        const checkbox = `input[type=checkbox][value='${group}']`;
        I.dontSeeCheckboxIsChecked(checkbox);
        I.checkOption(checkbox);
    });

    groupsToRemove.forEach((group) => {
        const checkbox = `input[type=checkbox][value='${group}']`;
        I.seeCheckboxIsChecked(checkbox)
        I.uncheckOption(checkbox);
    });

    I.switchTo();
    I.click(locate("#modalIframe button").withText("OK"));

    //Wait to recipients table to refresh
    I.waitForInvisible("#datatableFieldDTE_Field_recipientsTab_processing", 30);
}

function prepareCampaignForInsert(I, DTE, entityName) {
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    //Needed fields
    I.clickCss("button.btn-vue-jstree-item-edit");
    I.waitForElement("#jsTree");
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click( locate(".jstree-anchor").withText('Testovaci newsletter') );

    I.clickCss("#DTE_Field_subject")
    I.fillField("#DTE_Field_subject", entityName);
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

    I.say("Close dialog");
    I.waitForVisible("#toast-container-webjet");
    I.click(locate("#toast-container-webjet").find("button.btn-outline-secondary"));

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

let duplicatedName = "";
Scenario('BUG recipients missing in duplicated entity', ({I, DT, DTE}) => {
    let entityToDuplicate = "testRecipientsAfterDuplicate";
    duplicatedName = entityToDuplicate + "_duplicated" + randomNumber + "_autotest";

    I.say("Found entity for duplication and start duplication");
    I.amOnPage("/apps/dmail/admin/");
    DT.filterEquals("subject", entityToDuplicate);
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.btn-duplicate");
    DTE.waitForEditor("campaingsDataTable");

    I.say("Check we dont see tabs for opens/clicks");
    I.dontSeeElement("#pills-dt-campaingsDataTable-opens-tab");
    I.dontSeeElement("#pills-dt-campaingsDataTable-clicks-tab");

    I.say("Change name and save");
    I.fillField("#DTE_Field_subject", duplicatedName);
    DTE.save();

    I.say("Check recipients in duplicated entity");
    DT.filterContains("subject", duplicatedName);
    I.click(duplicatedName);
    DTE.waitForEditor("campaingsDataTable");
    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.see("testRepicientA");
    I.see("testrepicienta@balat.sk");
    I.see("testRepicientB");
    I.see("testrepicientb@balat.sk");
});

Scenario('BUG duplicate delete', ({I, DT, DTE}) => {
    deleteCampaingByName(I, DT, DTE, duplicatedName);
});

function deleteCampaingByName(I, DT, DTE, entityName) {
    I.amOnPage("/apps/dmail/admin/");

    DT.filterEquals("subject", entityName);

    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("campaingsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(entityName);
}

Scenario('Feature setting of subject', ({I, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("campaingsDataTable");

    I.say("Check that subject is empty");
    I.seeInField("#DTE_Field_subject", "");

    I.say("Select webpage and check that subject is set");
    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Produktová stránka - B verzia');
    I.seeInField("#DTE_Field_subject", "Produktová stránka - B verzia");

    I.say("Change webpage and wait for dialog");
    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Registracia do newsletra');
    I.waitForVisible("#toast-container-webjet");

    I.say("IF click cancel - subject wont change");
    I.click(locate("#toast-container-webjet").find("button.btn-outline-secondary"));
    I.seeInField("#DTE_Field_subject", "Produktová stránka - B verzia");

    I.say("Change webpage and wait for dialog");
    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Registracia do newsletra');
    I.waitForVisible("#toast-container-webjet");

    I.say("IF click OK - subject should be changed");
    I.click(locate("#toast-container-webjet").find("button.btn-primary"));
    I.seeInField("#DTE_Field_subject", "Registracia do newsletra");
});

Scenario('BUG - remove users from unselected groups while campain is not save yet', ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/");

    I.click(DT.btn.campaings_add_button);
    DTE.waitForEditor("campaingsDataTable");

    I.say("add email to campain");
    editGroups(I, DT, ['Newsletter'], []);

    I.say("check that email is in campain");
    I.see("vipklient@balat.sk");
    I.see("user_sha512@balat.sk");

    I.say("Add default recipient");
    I.clickCss(recipientsWrapper + " > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success");
    I.waitForElement(recipientsModal);
    I.fillField("#DTE_Field_recipientName", "test");
    I.fillField("#DTE_Field_recipientEmail", "testdefault@balat.sk");
    I.clickCss(recipientsModal + " > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");
    I.waitForInvisible("#datatableFieldDTE_Field_recipientsTab_processing", 30);

    I.say("Check that see default recipient");
    I.see("testdefault@balat.sk");

    I.say("Remove group and test if recipients are removed");
    editGroups(I, DT, [], ['Newsletter']);

    I.dontSee("vipklient@balat.sk");
    I.dontSee("user_sha512@balat.sk");

    I.see("testdefault@balat.sk");

    I.click( locate(recipientsWrapper).find("button.buttons-select-all") );
    I.click( locate("#datatableFieldDTE_Field_recipientsTab_wrapper").find("button.buttons-remove") );
    I.waitForElement("div.DTE_Action_Remove");
    I.waitForText("Naozaj chcete zmazať položku?", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee("testdefault@balat.sk");
});

Scenario('BUG multiple users same email', ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/?id=-1");

    const usersEmail = "test_campaign@balat.sk";
    let entityName = "sameMailBug_" + randomNumber + "_autotest";

    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('Newsletter').find('.jstree-icon.jstree-ocl'));
    I.click('Testovaci newsletter');
    I.wait(0.5);
    I.fillField("#DTE_Field_subject", entityName);
    DTE.save();

    DT.filterEquals("subject", entityName);
    I.click(entityName);
    DTE.waitForEditor("campaingsDataTable");

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.click("button.btn-add-group");
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");
    I.click(locate("div.custom-control.form-switch label").withText("CampaingnTestBug"));
    I.switchTo();
    I.click(locate("#modalIframe button").withText("OK"));

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

    I.clickCss("#pills-dt-campaingsDataTable-receivers-tab");
    I.click("button.btn-add-group");
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");
    I.click(locate("div.custom-control.form-switch label").withText("CampaingnTestBug"));
    I.switchTo();
    I.click(locate("#modalIframe button").withText("OK"));
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