Feature('apps.basket.basket-invoice');

var randomNumber;
let paymentDataTable = "#datatableFieldDTE_Field_editorFields-payments_wrapper";
let itemsDataTable = "#datatableFieldDTE_Field_editorFields-items_wrapper";
let editorItems = "datatableFieldDTE_Field_editorFields-items";

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Eshop invoice tests', async ({I, DT, DTE, Document}) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    Document.switchDomain("shop.tau27.iway.sk");

    let testerName = "Tester_" + randomNumber + "-autotest";
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
    I.click(".webjetToolbarClose");
    I.clickCss("body > div.ly-page-wrapper > header > div.container > nav > div.menu-holder > div > a");
    I.clickCss("#orderButton > a");
    I.fillField("#deliveryNameId", testerName);
    I.fillField("#deliveryCityId", "Prešov");
    I.fillField("#deliveryZipId", "08005");
    I.fillField("#userNoteId", "This is nooote");

    I.say("Confirm new invoice");
    I.click({css: 'div.form-group > input[type=submit]'});
    I.waitForText("Objednávka úspešne odoslaná", 10);

    I.say("Check that invoice was created");
    I.amOnPage("/apps/basket/admin/");
    DT.filterEquals("deliveryName", testerName);
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
        I.seeElement( locate("h5").withText("Prehľad platieb") );
        I.seeElement( locate("div.col-sum").withText("Zaplatená suma: 0,00 eur zo sumy 3 126,36 eur") );

        I.say("Check inner table functionality + footer update");
            I.seeElement(paymentDataTable);
            I.say('Test valid price value - MIN');
                payment(I, DTE, 0.001, false, true, false);
            I.say('Test valid price value - MAX');
                payment(I, DTE, 100000, false, false, true);
            I.say("Create new payment");
                payment(I, DTE, 100, false, false, false);
            I.say("Edit new payment");
                payment(I, DTE, 200, true, false, false);

    I.say("Check items tab - inner table");
        I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
        I.seeElement( locate("h5").withText("Prehľad položiek objednávky") );
        I.seeElement( locate("div.col-sum").withText("Suma k zaplateniu: 3 126,36 eur") );

            I.say("Check buttons visibility");
                I.seeElement( locate(itemsDataTable).find("button.buttons-edit") );
                I.dontSeeElement( locate(itemsDataTable).find("button.buttons-create") );
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
                I.click( locate(itemsDataTable).find("button.buttons-select-all") );
                I.click( locate(itemsDataTable).find("button.buttons-remove") );
                I.waitForElement(".DTE_Action_Remove");
                I.click("Zmazať", "div.DTE_Action_Remove");
                DTE.waitForLoader();
                I.seeElement( locate("div.col-sum").withText("Suma k zaplateniu: 3 056,04 eur") );

    I.say("Check order tab - order status iframe");
        I.waitForElement("#pills-dt-basketInvoiceDataTable-order_status-tab");
        I.waitForElement("iframe#dataDiv", 3);

    DTE.save();

    I.say("Status was updated");
        I.see("Čiastočne zaplatená");


    I.say("Add bonus statuses to config");
        Document.setConfigValue("basketInvoiceBonusStatuses", "9|not_here\n10|here_A\n20|here_B");
        I.say("Check that they are in select");
        checkBonusOptions(I, DT, DTE, testerName, true);
        I.say("Remove value from config");
        Document.setConfigValue("basketInvoiceBonusStatuses", "");
        I.say("Check that bonus options are gone.");
        checkBonusOptions(I, DT, DTE, testerName, false);

    I.say("Test delete logic");
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

function checkBonusOptions(I, DT, DTE, testerName, seeOptions) {
    I.amOnPage("/apps/basket/admin/");
    DT.filterEquals("deliveryName", testerName);
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
        I.click( locate(paymentDataTable).find("button.buttons-select-all") );
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
        I.seeElement( locate("div.col-sum").withText("Zaplatená suma: " + price + ",00 eur zo sumy 3 126,36 eur") );
    }
}

Scenario("logout", ({I}) => {
    I.logout();
});

//TODO: test multidomain - filter and search for invoice on different domain