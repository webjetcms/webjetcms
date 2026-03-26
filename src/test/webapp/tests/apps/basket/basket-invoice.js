Feature('apps.basket.basket-invoice');

const SL = require("./SL");

var randomNumber;
let paymentDataTable = "#datatableFieldDTE_Field_editorFields-payments_wrapper";
let itemsDataTable = "#datatableFieldDTE_Field_editorFields-items_wrapper";
let editorItems = "datatableFieldDTE_Field_editorFields-items";

let testerName;
let invoiceNote = "This is nooote";

Before(({ I, DT, login }) => {
    login('admin');

    DT.addContext("basket", "#basketInvoiceDataTable_wrapper");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        testerName = "Tester_" + randomNumber + "-autotest";
    }
});

const basketUserEmail = "basket.notifyuser";

Scenario('Eshop - Switch domain and empty email inbox', async ({I, Document, TempMail}) => {
    I.say("Change domain");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    Document.switchDomain("shop.tau27.iway.sk");

    I.say("empty email inbox");
    await TempMail.login(basketUserEmail + TempMail.getTempMailDomain());
    await TempMail.destroyInbox();
});

Scenario('Eshop - create new invoice', async ({I, TempMail}) => {
    I.say("First create new INVOICE");

    I.say("Add 2 phone's");
    I.amOnPage("/produkty/mobily/iphone/iphone-x-128gb.html");
    I.waitForVisible("button.addToBasket");
    I.clickCss("button.addToBasket");
    I.amOnPage("/produkty/mobily/android/smasung-galaxy-s9-64gb.html");
    I.waitForVisible("button.addToBasket");
    I.clickCss("button.add");
    I.clickCss("button.addToBasket");

    I.say("Go to basket");
    await I.clickIfVisible('.cookies-bar-wrapper .btn-akcept');
    I.clickCss("body > div.ly-page-wrapper > header > div.container > nav > div.menu-holder > div > a");
    I.waitForElement(".md-basket-dropdown.open", 10);
    I.clickCss("#orderButton > a");
    I.fillField("#contactFirstNameId", testerName);
    I.fillField("#contactCityId", "Prešov");
    I.fillField("#contactZipId", "08005");
    I.fillField("#userNoteId", invoiceNote);

    I.seeInField("#contactEmailId", "tester@balat.sk");
    I.fillField("#contactEmailId", basketUserEmail + TempMail.getTempMailDomain());

    I.say("Confirm new invoice");
    I.click({css: 'div.form-group > input[type=submit]'});
    I.waitForText("Objednávka úspešne odoslaná", 10);
});

Scenario('Eshop - check created invoice email notification', async ({I, TempMail}) => {
    await TempMail.login(basketUserEmail + TempMail.getTempMailDomain());
    TempMail.openLatestEmail();
    I.waitForText("Nová objednávka", 10, TempMail.getSubjectSelector());
    I.see("potvrdzujeme prijatie vašej objednávky číslo");
    //verify invoice_email.jsp fields
    I.see("Fakturačná adresa");
    I.see("Smasung Galaxy S9 64GB");
    I.see("iPhone X 128GB");
    I.see("Celková suma k úhrade: 3 126,36 €");
    I.see(invoiceNote);
    TempMail.closeEmail();
    await TempMail.destroyInbox();
});

Scenario('Eshop invoice tests', async ({I, DT, DTE, Document, TempMail}) => {
    I.say("Check that invoice was created");
    I.amOnPage(SL.BASKET_ADMIN);
    DT.filterEquals("editorFields.firstName", testerName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check that buttons are hidden");
    I.dontSeeElement("button.buttons-create");
    I.dontSeeElement("button.buttons-duplicate");
    I.dontSeeElement("button.btn-import-dialog");

    I.say("Check base data")
        I.clickCss("button.buttons-select-all");
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor("basketInvoiceDataTable");
        const statusA = await I.grabValueFrom("#DTE_Field_statusId");
        I.assertEqual(statusA, "1");

    I.say("Check notification tab");
        I.clickCss("#pills-dt-basketInvoiceDataTable-notify-tab");
        const body = await I.grabHTMLFrom("#DTE_Field_editorFields-body > div.editor.ql-container.ql-snow > div.ql-editor");
        I.assertEqual(body, "<p>Nastala zmena stavu objednávky.</p><p>Vaša objednávka je aktuálne v stave {STATUS}.</p><p><br></p><p>&nbsp;</p><p>Rekapitulácia vašej objednávky:</p><p> </p><p>&nbsp;</p><p> {ORDER_DETAILS}</p>");

    I.say("Check payment tab - inner table");
        I.clickCss("#pills-dt-basketInvoiceDataTable-payments-tab");
        I.seeElement( locate("div.col-sum").withText("Zaplatená suma: 0,00 eur zo sumy: 3 126,36 eur") );

        I.say("I need allready prepared, not confirmed payment for whole sum");
        DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments_wrapper", 1, ["", "", "3 126,36", "eur", "Nie", "Neznámy stav"]);

        I.say("Check inner table functionality + footer update");
            I.seeElement(paymentDataTable);
            I.say('Test valid price value - MIN');
                payment(I, DTE, 0.001, false, true, false);
            I.say('Test valid price value - MAX');
                payment(I, DTE, 100000, false, false, true);
            I.say("Create new payment");
                payment(I, DTE, 100, false, false, false);

            DT.filterSelect("confirmed", "Áno");
            DT.waitForLoader();

            I.say("Edit new payment");
                payment(I, DTE, 200, true, false, false);

            //Deselect
            I.click( locate(paymentDataTable).find("#dt-filter-labels-link-confirmed > i.ti-x") );
            I.click( locate(paymentDataTable).find("button.buttons-select-all") );

    I.say("Check items tab - inner table");
        I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
        I.seeElement( locate("div.col-sum").withText("Suma k zaplateniu: 3 126,36 eur") );

            I.say("Check buttons visibility");
                I.seeElement( locate(itemsDataTable).find("button.buttons-edit") );
                //I.dontSeeElement( locate(itemsDataTable).find("button.buttons-create") );
                I.dontSeeElement( locate(itemsDataTable).find("button.buttons-duplicate") );

            I.say("Edit item");
                I.click("Smasung Galaxy S9 64GB");
                DTE.waitForEditor(editorItems);
                I.fillField("#DTE_Field_itemQty", 0);
                DTE.save(editorItems);
                I.see("musí byť väčšie alebo rovné 1");
                I.fillField("#DTE_Field_itemQty", 3);
                DTE.save(editorItems);
                I.seeElement( locate("div.col-sum").withText("Suma k zaplateniu: 4 145,04 eur") );
            I.say("Delete item");
                DT.filterEquals("itemTitle", "iPhone X 128GB");
                I.click( locate(itemsDataTable).find("td.sorting_1") );
                I.click( locate(itemsDataTable).find("button.buttons-remove") );
                I.waitForElement(".DTE_Action_Remove");
                I.click("Zmazať", "div.DTE_Action_Remove");
                DTE.waitForLoader();
                I.seeElement( locate("div.col-sum").withText("Suma k zaplateniu: 3 056,04 eur") );

    I.say("Check order tab - order status iframe");
        I.waitForElement("#pills-dt-basketInvoiceDataTable-order_status-tab");
        I.waitForElement("iframe#dataDiv", 3);

    I.say("Also - send change email notification");
    I.clickCss("#pills-dt-basketInvoiceDataTable-basic-tab");
    I.checkOption("#DTE_Field_editorFields-sendNotification_0");
    DTE.save();

    I.say("Status was updated");
        DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName, "Playwright", "", "Čiastočne zaplatená", "Prevodom", "Vyzdvihnutie v predajni", "5", "2 546,70", "3 056,04", "2 856,04"]);

    I.say("Add bonus statuses to config");
        Document.setConfigValue("basketInvoiceBonusStatuses", "9|not_here\n10|here_A\n20|here_B");
        I.say("Check that they are in select");
        checkBonusOptions(I, DT, DTE, testerName, true);
        I.say("Remove value from config");
        Document.setConfigValue("basketInvoiceBonusStatuses", "");
        I.say("Check that bonus options are gone.");
        checkBonusOptions(I, DT, DTE, testerName, false);
});

Scenario('Eshop invoice change status email test', async ({I, TempMail}) => {
    I.say("Previous test updated status, go check email format and content");

    await TempMail.login(basketUserEmail + TempMail.getTempMailDomain());
    TempMail.openLatestEmail();
    I.waitForText("Zmena stavu objednavky", 10, TempMail.getSubjectSelector());
    I.see("Nastala zmena stavu objednávky.");
    I.see("Vaša objednávka je aktuálne v stave Čiastočne zaplatená.");
    TempMail.closeEmail();
    await TempMail.destroyInbox();
});

Scenario('Eshop - remove invoice', ({I, DT, DTE}) => {
    I.say("Test delete logic");

    I.amOnPage(SL.BASKET_ADMIN);
    DT.filterEquals("editorFields.firstName", testerName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.clickCss("td.dt-select-td");
    I.clickCss("button.buttons-remove");
    I.waitForElement(".DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Zmazanie objednávky nie je možné, pretože sa nenachádza v stave Stornovaná.");
    I.click("Zrušiť", "div.DTE_Action_Remove");

    I.click("button.buttons-edit");
    DTE.waitForEditor("basketInvoiceDataTable");
    DTE.selectOption("statusId", "Stornovaná");
    DTE.save();

    I.clickCss("button.buttons-remove");
    I.waitForElement(".DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Currency convertion', ({I, DT, DTE, Document}) => {
    I.amOnPage(SL.BASKET_ADMIN);

    Document.switchDomain("demo.webjetcms.sk");

    I.say("Check that covertion in table is valid");
    SL.validateCurrencyOptions(I, ["eur", "usd", "czk", "gbp"], []);
    SL.selectCurrency(I, "eur");
    DT.filterEquals("editorFields.lastName", "CurrencyConvertion");
    DT.checkTableRow("basketInvoiceDataTable", 1, ["", "", "CurrencyConvertion", "", "", "", "", "", "48,50", "59,61", "59,61"]);

    SL.selectCurrency(I, "czk");
    DT.checkTableRow("basketInvoiceDataTable", 1, ["", "", "CurrencyConvertion", "", "", "", "", "", "1 175,16", "1 444,35", "1 444,35"]);

    I.say("Check, that inside its still in EUR currency");
        I.click("Tester");
        DTE.waitForEditor("basketInvoiceDataTable");

        I.clickCss("#pills-dt-basketInvoiceDataTable-payments-tab");
        I.see("Zaplatená suma: 0,00 eur zo sumy: 59,61 eur (nedoplatok : 59,61)");
        DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 1, ["", SL.PaymentMethods.cashOnDelivery, "59,61"]);

        I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
        DT.checkTableRow("datatableFieldDTE_Field_editorFields-items", 1, ["", "", "Tričko", "10,00"]);
        DT.checkTableRow("datatableFieldDTE_Field_editorFields-items", 2, ["", "", "Ponožky", "7,00"]);
        DT.checkTableRow("datatableFieldDTE_Field_editorFields-items", 3, ["", "", "Džínsy", "25,00"]);
});

Scenario('Verify behaviour of config value basketInvoiceSupportedCountries', async ({Document, I, DT, DTE }) => {
    Document.setConfigValue("basketInvoiceSupportedCountries", ".sk,.pl,.fr,.de");
    I.amOnPage(SL.BASKET_ADMIN);
    DT.filterContains("editorFields.firstName", "Test");
    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric").first());
    I.click(DT.btn.basket_edit_button);
    DTE.waitForEditor("basketInvoiceDataTable");
    I.clickCss("#pills-dt-basketInvoiceDataTable-personal_info-tab");
    const countries = await I.grabTextFromAll('#DTE_Field_contactCountry option');
    I.assertEqual(countries.join(), "Slovensko,Poľsko,Francúzsko,Nemecko");
});

Scenario('Set config value to default', ({ Document }) => {
    Document.setConfigValue("basketInvoiceSupportedCountries", ".sk,.cz,.pl");
});

function checkBonusOptions(I, DT, DTE, testerName, seeOptions) {
    I.amOnPage(SL.BASKET_ADMIN);
    DT.filterEquals("editorFields.firstName", testerName);
    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor("basketInvoiceDataTable");
    I.click( locate("div.DTE_Field_Name_statusId").find("button.dropdown-toggle") );

    //This one never can be seen
    I.dontSeeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("not_here") ) );

    if(seeOptions == true) {
        I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("here_A") ) );
        I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("here_B") ) );
    } else {
        I.dontSeeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("here_A") ) );
        I.dontSeeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("here_B") ) );
    }

    //eed to be there. we must close the dropdown, or .cancel() will not work
    I.click( locate("div.DTE_Field_Name_statusId").find("button.dropdown-toggle") );
    DTE.cancel();
}

function payment(I, DTE, price, isEdit, minErr, maxErr) {
    if(isEdit) {
        I.click( locate(paymentDataTable).find("td.sorting_1") );
        I.click( locate(paymentDataTable).find("button.buttons-edit") );
    } else {
        I.click( locate(paymentDataTable).find("button.buttons-create") );
    }

    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    I.fillField("#DTE_Field_payedPrice", price);
    DTE.save("datatableFieldDTE_Field_editorFields-payments");

    if(minErr == true) {
        I.see("Zadaná suma, nesmie byť menšia ako 0.01");
        I.clickCss("#datatableFieldDTE_Field_editorFields-payments_modal > div > div > div.DTE_Header.modal-header > button");
    } else if(maxErr == true) {
        I.see("Celková zaplatená suma nesmie byť väčšia ako suma objednávky");
        I.clickCss("#datatableFieldDTE_Field_editorFields-payments_modal > div > div > div.DTE_Header.modal-header > button");
    } else {
        I.seeElement( locate("div.col-sum").withText("Zaplatená suma: " + price + ",00 eur zo sumy: 3 126,36 eur") );
    }
}

Scenario("logout", ({I}) => {
    I.logout();
});

//TODO: test multidomain - filter and search for invoice on different domain