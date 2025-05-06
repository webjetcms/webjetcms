Feature("apps.basket.eshop");

const path = require("path");
const SL = require("./SL");

let randomNumber;

Before(({ I, login, DT }) => {
  login("admin");
  if (typeof randomNumber == "undefined") {
    randomNumber = I.getRandomText();
  }
  DT.addContext("basket", "#basketInvoiceDataTable_wrapper");
  DT.addContext("payment", "#paymentMethodsDataTable_wrapper");
  DT.addContext("editorpayment", "#datatableFieldDTE_Field_editorFields-payments_wrapper");
  DT.addContext("editoritems", "#datatableFieldDTE_Field_editorFields-items_wrapper");

});

Scenario("Nastav sposoby platby", ({ I, DT, DTE }) => {
  I.amOnPage(SL.METHODS);

  //
  I.say("Setting the payment method to cash on delivery");
  I.click(locate("a").withText("Dobierka"));
  DTE.waitForEditor("paymentMethodsDataTable");
  DTE.seeInField("paymentMethodName", "Dobierka");
  DTE.fillField("fieldA", "1,50");
  DTE.fillField("fieldB", "20");
  I.waitForElement("#DTE_Field_fieldC");
  DTE.fillQuill("fieldC", "Dakujeme za platbu dobierkou");
  I.checkOption("#DTE_Field_fieldD");
  DTE.save("paymentMethodsDataTable");
  //
  I.say("Setting the payment method GoPay");
  I.click(locate("a").withText("GoPay"));
  DTE.waitForEditor("paymentMethodsDataTable");
  DTE.seeInField("paymentMethodName", "GoPay");
  DTE.fillField("fieldA", "1494406507");
  DTE.fillField("fieldB", "vFJzstcz");
  DTE.fillField("fieldC", "https://gw.sandbox.gopay.com/api/");
  DTE.fillField("fieldD", "8748690720");
  DTE.fillField("fieldE", "0,4");
  DTE.fillField("fieldF", "8");
  DTE.fillField("fieldG", "Platba GoPay-om");
  I.waitForElement("#DTE_Field_fieldH");
  DTE.fillQuill("fieldH", "Dakujeme za platbu GoPay-om");
  DTE.save("paymentMethodsDataTable");

  //
  I.say("Setting the payment method to bank transfer.");
  I.click(locate("a").withText("Prevodom"));
  DTE.waitForEditor("paymentMethodsDataTable");
  DTE.seeInField("paymentMethodName", "Prevodom");
  DTE.fillField("fieldA", "SK59 1100 0000 0026 1000 1237");
  DTE.fillField("fieldB", "0");
  DTE.fillField("fieldC", "20");
  I.waitForElement("#DTE_Field_fieldD");
  DTE.fillQuill("fieldD", "Dakujeme za platbu prevodom");
  I.checkOption("#DTE_Field_fieldE");
  DTE.save("paymentMethodsDataTable");
});

Scenario('Test admin product list', ({ I, DT}) => {
    I.amOnPage(SL.PRODUCTS_ADMIN);
    DT.checkTableRow("productListDataTable", 1, ["", "", "Tričko", "Tester Playwright", "", "12,30", "10,00"]);
    DT.checkTableRow("productListDataTable", 2, ["", "", "Ponožky", "Tester Playwright", "", "8,61", "7,00"]);
    DT.checkTableRow("productListDataTable", 3, ["", "", "Džínsy", "Tester Playwright", "", "30,75", "25,00"]);
    I.clickCss("button[data-id=currencySelect]");
    I.waitForElement(locate("a[role=option]").withText("skk"));
    I.click(locate("a[role=option]").withText("skk"));
    DT.checkTableRow("productListDataTable", 1, ["", "", "Tričko", "Tester Playwright", "", "370,55", "301,26"]);
    DT.checkTableRow("productListDataTable", 2, ["", "", "Ponožky", "Tester Playwright", "", "259,38", "210,88"]);
    DT.checkTableRow("productListDataTable", 3, ["", "", "Džínsy", "Tester Playwright", "", "926,37", "753,15"]);
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


Scenario("GoPay test unsuccessful, try to pay again and verify invoice", async ({ I, Document }) => {
    const testerName = "autotest-noPay-" + randomNumber;
    const deliveryMethodName = "Kuriér";
    const paymentMethodName = "GoPay";

    //
    I.say("Starting GoPay unsuccessful payment test");
    I.amOnPage(SL.PRODUCTS);
    SL.addToBasket(I, "Ponožky");
    SL.openBasket(I);
    I.clickCss("#orderButton > a");

    //
    I.say("Filling out order details and selecting GoPay");
    SL.fillDeliveryForm(I, testerName, deliveryMethodName, paymentMethodName);
    I.click(locate("input").withAttr({ name: "bSubmit" }));
    I.waitForText("Objednávka úspešne odoslaná", 20);
    I.see("Dakujeme za platbu GoPay-om");

    //
    I.say("Proceeding to payment");
    I.click(locate("#wjInline-docdata button").withText("Zaplatiť"));
    I.waitForElement("//div[contains(text(), 'Platobná karta')]", 10);
    I.clickCss("//div[contains(text(), 'Platobná karta')]");
    I.waitForText("Platba kartou");

    //
    I.say("Entering card details");
    I.switchTo('iframe[data-cy="cardCommIframe"]');
    I.waitForElement("#cardPan", 10);
    I.fillField("#cardPan", "5447380000000006");
    I.fillField("#cardExp", "1230");
    I.fillField("#cardCvc", "123");
    I.switchTo();
    I.clickCss("button[data-cy=cardSubmit]");
    I.waitForInvisible(locate("div").withText("Prebieha komunikácia s vašou bankou…"), 10);

    //
    I.say("Verifying payment failure");
    I.wait(5);
    await I.clickIfVisible("#confirm");
    //I.waitForText("Platba bola zamietnutá v autorizačnom centre. Ak chcete viac informácií, kontaktujte svoju banku.", 30);
    I.clickCss('button[data-cy="submitRedirectButton"]');
    I.waitForText("Platba sa nepodarila!", 30);

    //
    I.say("Verifying invoice");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    seeInTable(I, 1, "11.04", "Nová (nezaplatená)");
    I.click(locate("tr").at(2));
    verifyInvoice(I, testerName, deliveryMethodName, paymentMethodName, "11.04", "11.04");

    //
    I.say("Check pdf invoice");
    I.handleDownloads("downloads/invoice-" + randomNumber + ".pdf");
    I.clickCss("#downloadInvoiceDetails");
    I.amInPath("../../../build/test/downloads");
    I.waitForFile("invoice-" + randomNumber + ".pdf", 30);
    const filePath = path.resolve(
      "../../../build/test/downloads/invoice-" + randomNumber + ".pdf"
    );
    I.amOnPage("file://" + filePath);
    await Document.compareScreenshotElement("body > embed", "invoice.png", 1280, 760, 5);

    //
    I.say("Proceeding to payment");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    I.click(locate("tr").at(2));
    I.clickCss("#payForOrderBtn");
    I.wait(3);
    I.click(locate("#wjInline-docdata button").withText("Zaplatiť"));
    I.waitForText("Zvoľte platobnú metódu", 10);
    I.clickCss("//div[contains(text(), 'Bankový prevod')]");
    I.clickCss("//div[contains(text(), 'Tatra banka')]");
    I.clickCss('button[data-cy="bankSubmit"]');
    I.waitForElement("#confirm");
    I.clickCss("#confirm");
    I.waitForText("Platba prebehla úspešne.", 30);
  }
);

Scenario("GoPay test", async ({ I, DT, DTE }) => {
  const testerName = "autotest-goPay-" + randomNumber;

  //
  I.say("Starting GoPay successful payment test");
  I.amOnPage(SL.PRODUCTS);
  SL.addToBasket(I, "Tričko");
  SL.openBasket(I);
  I.clickCss("#orderButton > a");

  //
  I.say("Filling out order details and selecting GoPay");
  SL.fillDeliveryForm(I, testerName, null, "GoPay");
  I.click(locate("input").withAttr({ name: "bSubmit" }));
  I.waitForText("Objednávka úspešne odoslaná", 20);
  I.see("Dakujeme za platbu GoPay-om");

  //
  I.say("Proceeding to payment");
  I.click(locate("#wjInline-docdata button").withText("Zaplatiť"));
  I.waitForElement("//div[contains(text(), 'Platobná karta')]", 10);
  I.clickCss("//div[contains(text(), 'Platobná karta')]");
  I.waitForText("Platba kartou");

  //
  I.say("Entering card details");
  I.switchTo('iframe[data-cy="cardCommIframe"]');
  I.waitForElement("#cardPan", 10);
  I.fillField("#cardPan", "5447380000000006");
  I.fillField("#cardExp", "1230");
  I.fillField("#cardCvc", "123");
  I.switchTo();
  I.clickCss("button[data-cy=cardSubmit]");
  I.waitForInvisible(locate("div").withText("Prebieha komunikácia s vašou bankou…"), 10);
  I.wait(2);
  await I.clickIfVisible("#confirm");
  I.waitForText("Platba prebehla úspešne.", 30);

  //
  I.say("Verifying invoice");
  I.amOnPage(SL.ORDERS);
  I.waitForElement(locate("h1").withText("Objednávky"), 10);
  seeInTable(I, 1, "14.73", "Zaplatená");
  I.click(locate("tr").at(2));
  //I.dontSeeElement("#payForOrderBtn");

  //
  I.say("Processing partial refund");
  openPayments(I, DT, DTE, testerName);
  I.waitForText("Zaplatená suma: 14,73 eur zo sumy: 14,73 eur", 10, ".dt-footer-row > div > p");
  I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
  I.click(DT.btn.editorpayment_refund_button);
  I.waitForText("Vrátenie platby", 10, "div.toast-title");
  I.fillField(".toast input[id^=toastrPromptInput]", "10");
  I.clickCss(".toastr-buttons button[id^=confirmationYes]");
  I.waitForInvisible("#datatableFieldDTE_Field_editorFields-payments_processing", 60);
  I.waitForText("Vrátenie platby bolo neúspešné.", 10, ".toast-message");
  I.toastrClose();

  //
  I.say("Verify partial refund");
  I.waitForText("Zaplatená suma: 14,73 eur zo sumy: 14,73 eur", 10, ".dt-footer-row > div > p");
  DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 2, ["", "GoPay", "-10,00", "Nie", "Neúspešná refundácia", getCurrentDate()]);

  //
  I.say("Processing refund");
  I.click(DT.btn.editorpayment_refund_button);
  I.clickCss(".toastr-buttons button[id^=confirmationYes]");
  I.waitForInvisible("#datatableFieldDTE_Field_editorFields-payments_processing", 60);
  I.waitForText("Vrátenie platby bolo úspešné.", 10, ".toast-message");
  I.toastrClose();

  //
  I.say("Verifying refund status");
  I.waitForText("Zaplatená suma: 0,00 eur zo sumy: 14,73 eur (nedoplatok : 14,73)", 10, ".dt-footer-row > div > p");
  DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 1, ["", "GoPay", "14,73", "Áno", "Refundovaná platba", getCurrentDate() , "GoPay payment state : PAID"]);
  DTE.cancel("basketInvoiceDataTable");
  DT.filterContains("editorFields.firstName", testerName);
  DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName, "Playwright", getCurrentDate(), "Nová (nezaplatená)", "GoPay", "Kuriér", "3", "12,40", "14,73"]);
});

Scenario("Test Orders for Logged-in and Unauthenticated User", async ({ I }) => {
    //
    I.say("Check the latest order of Tester");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    seeInTable(I, 1, "14.73", "Nová (nezaplatená)");

    //
    I.say("Check that unautenticated user cannot see table");
    I.logout();
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    I.dontSeeElement("#listOfInvoices");
  }
);

Scenario("Cash on delivery", async ({ I }) => {
  I.amOnPage(SL.PRODUCTS);
  SL.addToBasket(I, "Ponožky");
  SL.openBasket(I);
  I.clickCss("#orderButton > a");
  SL.fillDeliveryForm(I, "autotest-cash-" + randomNumber, null, "Dobierka");

  I.click(locate("input").withAttr({ name: "bSubmit" }));
  I.waitForText("Objednávka úspešne odoslaná", 20);
  I.see("Dakujeme za platbu dobierkou");
  I.dontSeeElement(locate("#wjInline-docdata button").withText("Zaplatiť"));
});

Scenario("Bank transfer", async ({ I }) => {
  I.amOnPage(SL.PRODUCTS);
  SL.addToBasket(I, "Ponožky");
  SL.openBasket(I);
  I.clickCss("#orderButton > a");
  SL.fillDeliveryForm(I, "autotest-transfer-" + randomNumber, null, "Prevod");

  I.click(locate("input").withAttr({ name: "bSubmit" }));
  I.waitForText("Objednávka úspešne odoslaná", 20);
  I.see("Vašu objednávku uhradíte prevodom na účet : SK59 1100 0000 0026 1000 1237");
  I.see("Dakujeme za platbu prevodom");
  I.dontSeeElement(locate("#wjInline-docdata button").withText("Zaplatiť"));
});

Scenario('Test of name concatenation', ({ I, DT, DTE }) => {
    const testerName = "autotest-goPay-" + randomNumber;
    const deliveryTester = "name" + I.getRandomText();
    I.amOnPage(SL.BASKET_ADMIN);

    DT.filterContains("editorFields.firstName", testerName);
    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric").first());
    I.click(DT.btn.basket_edit_button);
    DTE.waitForEditor("basketInvoiceDataTable");
    I.clickCss("#pills-dt-basketInvoiceDataTable-personal_info-tab");
    DTE.fillField("deliveryName", deliveryTester);
    DTE.save("basketInvoiceDataTable");
    DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName + ` (${deliveryTester})`, "Playwright"]);
    DT.filterContains("editorFields.firstName", deliveryTester);
    DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName + ` (${deliveryTester})`, "Playwright"]);
});

Scenario('Verify that cannot change payment method in Payments tab, verify closed date and refundation', async ({ I, DT, DTE }) => {
    const testerName = "autotest-cash-" + randomNumber;
    openPayments(I, DT, DTE, testerName);
    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
    I.click(DT.btn.editorpayment_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    DTE.selectOption("paymentMethod", "GoPay");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForElement(locate(".DTE_Form_Error").withText("Tento spôsob platby neumožňuje editáciu/duplikovanie/mazanie platby."));

    DTE.selectOption("paymentMethod", "Dobierka");
    I.uncheckOption("#DTE_Field_confirmed_0");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
    lineColor = await SL.getFontColor(I, 1,2);
    I.assertEqual(lineColor, SL.red);

    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
    I.click(DT.btn.editorpayment_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    I.checkOption("#DTE_Field_confirmed_0");
    const closedDate = getCurrentDate(true).slice(0, 16);;

    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    lineColor = await SL.getFontColor(I, 1,2);
    I.assertEqual(lineColor, SL.black);

    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
    I.click(DT.btn.editorpayment_edit_button);

    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    const actual = (await I.grabValueFrom('#DTE_Field_closedDate')).slice(0, 16);
    I.assertEqual(closedDate, actual, "Close date was not correct!");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");

    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
    DT.filterContains("itemTitle", "Ponožky");
    I.clickCss("#datatableFieldDTE_Field_editorFields-items_wrapper button.buttons-select-all");
    I.click(DT.btn.editoritems_delete_button);
    I.waitForVisible(".DTE.modal-content.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.clickCss("#pills-dt-basketInvoiceDataTable-payments-tab");
    I.waitForText("Zaplatená suma: 12,41 eur zo sumy: 3,80 eur (preplatok : 8,61)", 10, ".dt-footer-row > div > p");

    I.click(DT.btn.editorpayment_add_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    I.checkOption("#DTE_Field_editorFields-saveAsRefund_0");
    DTE.fillField("payedPrice", "-13");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForElement(locate(".DTE_Form_Error").withText("Nemôžete refundovať viac ako je zaplatená suma."));

    DTE.fillField("payedPrice", "-8");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForElement(locate(".DTE_Form_Error").withText("Celková zaplatená suma nesmie byť väčšia ako suma objednávky"));
    DTE.fillField("payedPrice", "-9");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForText("Zaplatená suma: 3,41 eur zo sumy: 3,80 eur (nedoplatok : 0,39)", 10, ".dt-footer-row > div > p");

    DT.filterSelect("paymentStatus", "Úspešná refundácia");
    I.clickCss("#datatableFieldDTE_Field_editorFields-payments_wrapper button.buttons-select-all");
    I.click(DT.btn.editorpayment_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    I.uncheckOption("#DTE_Field_confirmed_0");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");

    DT.filterSelect("paymentStatus", "Neznámy stav");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 1, ["", "Dobierka", "-9,00", "Nie", "Neznámy stav"]);
});

Scenario('Verify admin modification of paid order', ({ I, DT, DTE }) => {
    const testerName = "autotest-transfer-" + randomNumber;
    openPayments(I, DT, DTE, testerName);

    I.clickCss("#datatableFieldDTE_Field_editorFields-payments_wrapper button.buttons-select-all");
    I.click(DT.btn.editorpayment_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    I.checkOption("#DTE_Field_confirmed_0");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");

    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
    I.seeElement(locate('tr').withText('Ponožky').find('i.ti-shopping-bag'));
    I.seeElement(locate('tr').withText('Kuriér').find('i.ti-truck-delivery'));
    I.seeElement(locate('tr').withText('Prevodom').find('i.ti-cash'));

    I.click(locate('tr').withText('Ponožky').find('.dt-select-td'));
    I.click(DT.btn.editoritems_preview_button);
    I.switchToNextTab();
    I.waitForText("Ponožky", 10);
    I.switchToPreviousTab();
    I.closeOtherTabs();

    DT.filterContains("itemTitle", "Kuriér");
    I.clickCss("#datatableFieldDTE_Field_editorFields-items_wrapper button.buttons-select-all");
    I.clickCss("#datatableFieldDTE_Field_editorFields-items_wrapper button.buttons-select-all");
    I.click(DT.btn.editoritems_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-items");
    DTE.fillField("itemPrice", "3" );
    DTE.fillField("itemQty", "2" );
    DTE.fillField("itemNote", "Opakované doručenie + zdraženie kuriéra");

    DTE.save("datatableFieldDTE_Field_editorFields-items");
    I.waitForText("Suma k zaplateniu: 14,61 eur", 10, ".dt-footer-row > div > p");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-items", 1, ["", "", "Kuriér", "3,00", "2", "6,00", "0%", "Opakované doručenie + zdraženie kuriéra"]);

    I.amOnPage(SL.BASKET_ADMIN);
    DT.waitForLoader();
    DT.filterContains("editorFields.firstName", testerName);
    DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName, "Playwright", "", "Čiastočne zaplatená", "Prevodom", "Kuriér", "4", "13,00", "14,61", "4,00"]);

    openPayments(I, DT, DTE, testerName);
    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
    I.click(DT.btn.editoritems_add_button);
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");

    I.waitForElement(locate('tr').withText('Tričko').find('.dt-select-td'));
    I.click(locate('tr').withText('Tričko').find('.dt-select-td'));
    I.switchTo();
    I.click(locate(".btn.btn-primary").withText("Pridať"))

    I.waitForText("Suma k zaplateniu: 26,91 eur", 10, ".dt-footer-row > div > p");
    DT.filterContains("itemTitle", "Tričko");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-items", 1, ["", "", "Tričko", "10,00", "1", "10,00", "23%", "12,30"]);
    DTE.save("basketInvoiceDataTable");
});

Scenario("Delete", async ({ I, DT, DTE }) => {
  I.amOnPage(SL.BASKET_ADMIN);
  DT.filterContains("editorFields.firstName", "autotest-");

  for (let i = 0; i < 10 && (await I.grabNumberOfVisibleElements("td.dt-empty")) === 0; i++) {
    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric").first());
    I.click(DT.btn.basket_edit_button);
    DTE.waitForEditor("basketInvoiceDataTable");
    DTE.selectOption("statusId", "Stornovaná");
    DTE.save("basketInvoiceDataTable");
    I.click(DT.btn.basket_delete_button);
    I.waitForVisible(".DTE.modal-content.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
  }
});

function openPayments(I, DT, DTE, testerName) {
  I.amOnPage(SL.BASKET_ADMIN);
  DT.waitForLoader();
  DT.filterContains("editorFields.firstName", testerName);
  I.click(
    locate("td.dt-row-edit.cell-not-editable > div > a")
      .withText(testerName)
      .first()
  );
  DTE.waitForEditor("basketInvoiceDataTable");
  I.clickCss("#pills-dt-basketInvoiceDataTable-payments-tab");
}

function verifyInvoice(I, testerName, deliveryMethodName, paymentMethodName, price) {
  const date = getCurrentDate();
  I.switchTo("#dataDiv");
  I.see("Potvrdenie objednávky", "td.invoiceHeader.alignRight");
  I.see(`Dátum vytvorenia: ${date}`, "td.alignRight");
  I.see("Nová (nezaplatená)", 'td[colspan="2"]');
  I.see(testerName, "table.invoiceInnerTable");
  I.see("Playwright", "table.invoiceInnerTable");
  I.see("Mlýnske Nivy 71", "table.invoiceInnerTable");
  I.see("Bratislava", "table.invoiceInnerTable");
  I.see("82105", "table.invoiceInnerTable");
  I.see("Slovensko", "table.invoiceInnerTable");
  I.see(deliveryMethodName);
  I.see("webjetbasket@fexpost.com", "table.invoiceInnerTable");
  I.see("0912345678", "table.invoiceInnerTable");
  I.see(paymentMethodName, ".invoiceDetailTable");
  I.see("Toto je poznamka k objednavke", 'td[colspan="2"]');
  I.see("Ponožky", "table.basketListTable");
  I.see(`${price.replace(".", ",")} €`, ".basketListTableTotalVat .basketPrice");
  I.switchTo();
}

function getCurrentDate(includeTime = false) {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, '0');

    const day = pad(now.getDate());
    const month = pad(now.getMonth() + 1);
    const year = now.getFullYear();

    let result = `${day}.${month}.${year}`;

    if (includeTime) {
      const hours = pad(now.getHours());
      const minutes = pad(now.getMinutes());
      const seconds = pad(now.getSeconds());
      result += ` ${hours}:${minutes}:${seconds}`;
    }

    return result;
  }

function deletePaymentMethod(I, DT, DTE, index) {
  I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric").at(index));
  I.click(DT.btn.payment_delete_button);
  I.waitForVisible(".DTE.modal-content.DTE_Action_Remove");
  I.click("Zmazať", "div.DTE_Action_Remove");
  DTE.waitForLoader();
}

function seeInTable(I, rowIndex, price, status) {
  const row = locate("tr").at(rowIndex + 1);
  const date = getCurrentDate();
  I.see(date, row.find("td").at(2));
  I.see(price.replace(".",","), row.find("td").at(3));
  I.see(status, row.find("td").at(5));
}