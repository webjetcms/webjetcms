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

Scenario('Set config value to default', ({ Document }) => {
    Document.setConfigValue("basketInvoiceSupportedCountries", ".sk,.cz,.pl");
});

Scenario("GoPay test unsuccessful, try to pay again and verify invoice", async ({ I, Document, TempMail }) => {
    const testerName = "autotest-noPay-" + randomNumber;
    const deliveryMethodTitle = "Štandardná pošta: 6,15 €"; //JSP title with info
    const paymentMethodTitle = "GoPay: 0,43 €"; //JSP title with info

    SL.clearBasket(I);

    //
    I.say("Starting GoPay unsuccessful payment test");
    I.amOnPage(SL.PRODUCTS);
    SL.addToBasket(I, "Ponožky");
    SL.openBasket(I);
    I.clickCss("#orderButton > a");

    //
    I.say("Filling out order details and selecting GoPay");
    SL.fillDeliveryForm(I, TempMail, testerName, deliveryMethodTitle, paymentMethodTitle);
    I.click(locate("input").withAttr({ name: "bSubmit" }));
    I.waitForText("Objednávka úspešne odoslaná", 20);
    I.see("Dakujeme za platbu GoPay-om");

    //
    I.say("Proceeding to payment 1");
    I.waitForVisible(locate("button.btn-primary").withText("Zaplatiť"), 10);
    I.click(locate("button.btn-primary").withText("Zaplatiť"));
    I.waitForText("Platobná karta", 10, "p");
    I.click(locate("p").withText("Platobná karta"));

    await SL.doGoPayCardPayment(I, false);

    //
    I.say("Verifying invoice");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    seeInTable(I, 1, "15.19", "Nová (nezaplatená)");
    I.click(locate("tr").at(2));
    verifyInvoice(I, TempMail, testerName, SL.DeliveryMethods.byMailDelivery, SL.PaymentMethods.GoPay, "15.19");

    //
    I.say("Check pdf invoice");
    I.handleDownloads("invoice-" + randomNumber + ".pdf");
    I.clickCss("#downloadInvoiceDetails");
    //I.amInPath("../../../build/test/downloads");
    I.waitForFile("../../../build/test/invoice-" + randomNumber + ".pdf", 30);
    const filePath = path.resolve(
      "../../../build/test/invoice-" + randomNumber + ".pdf"
    );

    if (Document.isPdfViewerEnabled()) {
      I.amOnPage("file://" + filePath);
      await Document.compareScreenshotElement("body > embed", "invoice.png", 1280, 760, 5);
    }

    //
    I.say("Proceeding to payment 2");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    I.click(locate("tr").at(2));
    I.clickCss("#payForOrderBtn");
    I.click(locate("button.btn-primary").withText("Zaplatiť"));
    I.waitForText("Zvoľte platobnú metódu", 10);

    I.waitForText("Bankový prevod", 10, "p");
    I.click(locate("p").withText("Bankový prevod"));
    I.waitForText("Tatra banka", 10, "p");
    I.click(locate("p").withText("Tatra banka"));

    I.clickCss('button[data-cy="bankSubmit"]');
    I.waitForElement("#confirm");
    I.clickCss("#confirm");
    I.waitForText("Platba prebehla úspešne.", 30);
});

Scenario("GoPay test", async ({ I, DT, DTE, TempMail }) => {
  const testerName = "autotest-goPay-" + randomNumber;

  //
  I.say("Starting GoPay successful payment test");
  I.amOnPage(SL.PRODUCTS);
  SL.addToBasket(I, "Tričko");
  SL.openBasket(I);
  I.clickCss("#orderButton > a");

  //
  I.say("Filling out order details and selecting GoPay");
  SL.fillDeliveryForm(I, TempMail, testerName, null, SL.PaymentMethods.GoPay);
  I.click(locate("input").withAttr({ name: "bSubmit" }));
  I.waitForText("Objednávka úspešne odoslaná", 20);
  I.see("Dakujeme za platbu GoPay-om");

  //
  I.say("Proceeding to payment 3");
  I.waitForVisible(locate("button.btn-primary").withText("Zaplatiť"), 10);
  I.click(locate("button.btn-primary").withText("Zaplatiť"));
  I.waitForText("Platobná karta", 10, "p");
  I.click(locate("p").withText("Platobná karta"));

  // GoPay payment
  await SL.doGoPayCardPayment(I, true);

  //
  I.say("Verifying invoice");
  I.amOnPage(SL.ORDERS);
  I.waitForElement(locate("h1").withText("Objednávky"), 10);
  seeInTable(I, 1, "18.88", "Zaplatená");
  I.click(locate("tr").at(2));

  //
  I.say("Processing partial refund");
  openPayments(I, DT, DTE, testerName);
  I.waitForText("Zaplatená suma: 18,88 eur zo sumy: 18,88 eur", 10, ".dt-footer-row > div > p");
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
  I.waitForText("Zaplatená suma: 18,88 eur zo sumy: 18,88 eur", 10, ".dt-footer-row > div > p");
  DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 2, ["", SL.PaymentMethods.GoPay, "-10,00", "eur", "Nie", "Neúspešná refundácia", getCurrentDate()]);

  //
  I.say("Processing refund");
  I.click(DT.btn.editorpayment_refund_button);
  I.clickCss(".toastr-buttons button[id^=confirmationYes]");
  I.waitForInvisible("#datatableFieldDTE_Field_editorFields-payments_processing", 60);
  I.waitForText("Vrátenie platby bolo úspešné.", 10, ".toast-message");
  I.toastrClose();

  //
  I.say("Verifying refund status");
  I.waitForText("Zaplatená suma: 0,00 eur zo sumy: 18,88 eur (nedoplatok : 18,88)", 10, ".dt-footer-row > div > p");
  DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 1, ["", SL.PaymentMethods.GoPay, "18,88", "eur", "Áno", "Refundovaná platba", getCurrentDate() , "GoPay payment state : PAID"]);
  DTE.cancel("basketInvoiceDataTable");
  DT.filterContains("editorFields.firstName", testerName);
  DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName, "Playwright", getCurrentDate(), "Nová (nezaplatená)", SL.PaymentMethods.GoPay, SL.DeliveryMethods.byMailDelivery, "3", "15,40", "18,88", "18,88"]);
});

Scenario("Test Orders for Logged-in and Unauthenticated User", async ({ I }) => {
    //
    I.say("Check the latest order of Tester");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    seeInTable(I, 1, "18,88", "Nová (nezaplatená)");

    //
    I.say("Check that unautenticated user cannot see table");
    I.logout();
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    I.dontSeeElement("#listOfInvoices");
  }
);

Scenario("Cash on delivery", async ({ I, TempMail }) => {
  I.amOnPage(SL.PRODUCTS);
  SL.addToBasket(I, "Ponožky");
  SL.openBasket(I);
  I.clickCss("#orderButton > a");
  SL.fillDeliveryForm(I, TempMail, "autotest-cash-" + randomNumber, null, SL.PaymentMethods.cashOnDelivery);

  I.click(locate("input").withAttr({ name: "bSubmit" }));
  I.waitForText("Objednávka úspešne odoslaná", 20);
  I.see("Dakujeme za platbu dobierkou");
  I.dontSeeElement(locate("button.btn-primary").withText("Zaplatiť"));
});

Scenario("Bank transfer", async ({ I, TempMail }) => {
  I.amOnPage(SL.PRODUCTS);
  SL.addToBasket(I, "Ponožky");
  SL.openBasket(I);
  I.clickCss("#orderButton > a");
  SL.fillDeliveryForm(I, TempMail, "autotest-transfer-" + randomNumber, null, "Prevod");

  I.click(locate("input").withAttr({ name: "bSubmit" }));
  I.waitForText("Objednávka úspešne odoslaná", 20);
  I.see("Vašu objednávku uhradíte prevodom na účet : SK59 1100 0000 0026 1000 1237");
  I.see("Dakujeme za platbu prevodom");
  I.dontSeeElement(locate("button.btn-primary").withText("Zaplatiť"));
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
    DTE.selectOption("paymentMethod", SL.PaymentMethods.GoPay);
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForElement(locate(".DTE_Form_Error").withText("Tento spôsob platby neumožňuje editáciu/duplikovanie/mazanie platby."));

    DTE.selectOption("paymentMethod", SL.PaymentMethods.cashOnDelivery);
    I.uncheckOption("#DTE_Field_confirmed_0");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
    lineColor = await SL.getFontColor(I, 1,2);
    I.assertEqual(lineColor, SL.red);

    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
    I.click(DT.btn.editorpayment_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    I.checkOption("#DTE_Field_confirmed_0");
    const closedDate = I.parseDateTime(getCurrentDate(true));

    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    //uncheck all items
    I.click("#datatableFieldDTE_Field_editorFields-payments_wrapper button.buttons-select-all");

    lineColor = await SL.getFontColor(I, 1,2);
    I.assertEqual(lineColor, SL.black);

    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());
    I.click(DT.btn.editorpayment_edit_button);

    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    const actual = I.parseDateTime(await I.grabValueFrom('#DTE_Field_closedDate'));

    // Calculate absolute difference in milliseconds
    const diffMs = Math.abs(closedDate - actual);
    I.say(`Closed date expected: ${closedDate}, actual: ${actual}, diff: ${diffMs} ms`);
    // Check if difference is within 2 minutes (120,000 ms)
    I.assertTrue(diffMs <= 120000, `Close date difference was too large! Diff: ${diffMs} ms`);

    DTE.save("datatableFieldDTE_Field_editorFields-payments");

    // uncheck item
    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric.sorting_1").first());

    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
    DT.filterContains("itemTitle", "Ponožky");
    I.clickCss("#datatableFieldDTE_Field_editorFields-items_wrapper button.buttons-select-all");
    I.click(DT.btn.editoritems_delete_button);
    I.waitForVisible(".DTE.modal-content.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.clickCss("#pills-dt-basketInvoiceDataTable-payments-tab");
    I.waitForText("Zaplatená suma: 16,56 eur zo sumy: 7,95 eur (preplatok : 8,61)", 10, ".dt-footer-row > div > p");

    I.click(DT.btn.editorpayment_add_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    DTE.selectOption("paymentMethod", SL.PaymentMethods.cashOnDelivery);
    I.checkOption("#DTE_Field_editorFields-saveAsRefund_0");
    DTE.fillField("payedPrice", "-23");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForElement(locate(".DTE_Form_Error").withText("Nemôžete refundovať viac ako je zaplatená suma."));

    DTE.fillField("payedPrice", "-8");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForElement(locate(".DTE_Form_Error").withText("Celková zaplatená suma nesmie byť väčšia ako suma objednávky"));
    DTE.fillField("payedPrice", "-9");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    I.waitForText("Zaplatená suma: 7,56 eur zo sumy: 7,95 eur (nedoplatok : 0,39)", 10, ".dt-footer-row > div > p");

    DT.filterSelect("paymentStatus", "Úspešná refundácia");
    I.clickCss("#datatableFieldDTE_Field_editorFields-payments_wrapper button.buttons-select-all");
    I.click(DT.btn.editorpayment_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    I.uncheckOption("#DTE_Field_confirmed_0");
    DTE.save("datatableFieldDTE_Field_editorFields-payments");

    DT.filterSelect("paymentStatus", "Neznámy stav");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 1, ["", SL.PaymentMethods.cashOnDelivery, "-9,00", "eur", "Nie", "Neznámy stav"]);
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
    I.seeElement(locate('tr').withText(SL.DeliveryMethods.byMailDelivery).find('i.ti-truck-delivery'));
    I.seeElement(locate('tr').withText(SL.PaymentMethods.moneyTransfer).find('i.ti-cash'));

    I.click(locate('tr').withText('Ponožky').find('.dt-select-td'));
    I.click(DT.btn.editoritems_preview_button);
    I.switchToNextTab();
    I.waitForText("Ponožky", 10);
    I.switchToPreviousTab();
    I.closeOtherTabs();

    DT.filterContains("itemTitle", SL.DeliveryMethods.byMailDelivery);
    I.clickCss("#datatableFieldDTE_Field_editorFields-items_wrapper button.buttons-select-all");
    I.clickCss("#datatableFieldDTE_Field_editorFields-items_wrapper button.buttons-select-all");
    I.click(DT.btn.editoritems_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-items");
    DTE.fillField("itemPrice", "3" );
    DTE.fillField("itemQty", "2" );
    DTE.fillField("itemNote", "Opakované doručenie + zdraženie doručenia poštou.");

    DTE.save("datatableFieldDTE_Field_editorFields-items");
    I.waitForText("Suma k zaplateniu: 15,99 eur", 10, ".dt-footer-row > div > p");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-items", 1, ["", "", SL.DeliveryMethods.byMailDelivery, "3,00", "2", "6,00", "23%", "7,38", "Opakované doručenie + zdraženie doručenia poštou."]);

    I.amOnPage(SL.BASKET_ADMIN);
    DT.waitForLoader();
    DT.filterContains("editorFields.firstName", testerName);
    DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName, "Playwright", "", "Čiastočne zaplatená", SL.PaymentMethods.moneyTransfer, SL.DeliveryMethods.byMailDelivery, "4", "13,00", "15,99", "1,23"]);

    openPayments(I, DT, DTE, testerName);
    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
    I.click(DT.btn.editoritems_add_button);
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");

    I.waitForElement(locate('tr').withText('Tričko').find('.dt-select-td'));
    I.click(locate('tr').withText('Tričko').find('.dt-select-td'));
    I.switchTo();
    I.click(locate(".btn.btn-primary").withText("Pridať"))

    I.waitForText("Suma k zaplateniu: 28,29 eur", 10, ".dt-footer-row > div > p");
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
    I.waitForInvisible("div.DTE_Action_Remove");
    DT.waitForLoader();
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

function verifyInvoice(I, TempMail, testerName, deliveryMethodName, paymentMethodName, price) {
  const date = getCurrentDate();
  I.switchTo("#dataDiv");
  I.see("Potvrdenie objednávky", "td.invoiceHeader.alignRight");
  I.see(`Dátum vytvorenia: ${date}`, "td.alignRight");
  I.see("Nová (nezaplatená)", 'td[colspan="2"]');
  I.see(testerName, "table.invoiceInnerTable");
  I.see("Playwright", "table.invoiceInnerTable");
  I.see("Mlynské Nivy 71", "table.invoiceInnerTable");
  I.see("Bratislava", "table.invoiceInnerTable");
  I.see("82105", "table.invoiceInnerTable");
  I.see(SL.Countries.sk, "table.invoiceInnerTable");
  I.see(deliveryMethodName);
  I.see("webjetbasket"+TempMail.getTempMailDomain(), "table.invoiceInnerTable");
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

function seeInTable(I, rowIndex, price, status) {
  const row = locate("tr").at(rowIndex + 1);
  const date = getCurrentDate();
  I.see(date, row.find("td").at(2));
  I.see(price.replace(".",","), row.find("td").at(3));
  I.see(status, row.find("td").at(5));
}