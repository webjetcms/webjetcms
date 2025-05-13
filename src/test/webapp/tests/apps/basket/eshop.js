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
  DTE.save("paymentMethodsDataTable");
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
    I.click(locate("button").withText("Zaplatiť"));
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
    I.waitForText("Platba bola zamietnutá v autorizačnom centre. Ak chcete viac informácií, kontaktujte svoju banku.", 30);
    I.clickCss('button[data-cy="submitRedirectButton"]');
    I.waitForText("Platba sa nepodarila!", 30);

    //
    I.say("Verifying invoice");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    seeInTable(I, 1, "11,04", "Nová (nezaplatená)");
    I.click(locate("tr").at(2));
    verifyInvoice(I, testerName, deliveryMethodName, paymentMethodName, "11,04", "11,04");

    //
    I.say("Check pdf invoice");
    I.handleDownloads("downloads/invoice-" + randomNumber + ".pdf");
    I.clickCss("#downloadInvoiceDetails");
    I.amInPath("../../../build/test/downloads");
    I.waitForFile("invoice-" + randomNumber + ".pdf", 30);
    const filePath = path.resolve(
      "../../../build/test/downloads/invoice-" + randomNumber + ".pdf"
    );
    //chromium PDF viewer is not working in headless mode
    if ("false"!==process.env.CODECEPT_SHOW) {
      I.amOnPage("file://" + filePath);
      await Document.compareScreenshotElement("body > embed", "invoice.png", 1280, 760, 5);
    }

    //
    I.say("Proceeding to payment");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    I.click(locate("tr").at(2));
    I.clickCss("#payForOrderBtn");
    I.wait(3);
    I.click(locate("button").withText("Zaplatiť"));
    I.waitForText("Zvoľte platobnú metódu", 10);
    I.click("//div[contains(text(), 'Bankový prevod')]");
    I.click("//div[contains(text(), 'Tatra banka')]");
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
  I.click(locate("button").withText("Zaplatiť"));
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
  seeInTable(I, 1, "14,73", "Zaplatená");
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
  DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 2, [null, "GoPay", "-10,00", null, "Neúspešná refundácia", getCurrentDate(), "Vrátenie platby id: 183 RC[409] Name: PAYMENT_REFUND_FAILED[330] Msg: Payment refund failed."]);

  //
  I.say("Processing refund");
  I.click(DT.btn.editorpayment_refund_button);
  I.clickCss(".toastr-buttons button[id^=confirmationYes]");
  I.waitForInvisible("#datatableFieldDTE_Field_editorFields-payments_processing", 60);
  I.waitForText("Vrátenie platby bolo úspešné.", 10, ".toast-message");
  I.toastrClose();

  //
  I.say("Verifying refund status");
  I.waitForText("Zaplatená suma: 0,00 eur zo sumy: 14,73 eur", 10, ".dt-footer-row > div > p");
  DT.checkTableRow("datatableFieldDTE_Field_editorFields-payments", 1, [null, "GoPay", "14,73", null, "Refundovaná platba", getCurrentDate(), "GoPay payment state : PAID",]);
  DTE.cancel("basketInvoiceDataTable");
  DT.filterContains("editorFields.firstName", testerName);
  DT.checkTableRow("basketInvoiceDataTable", 1, ["", testerName, null, getCurrentDate(), "Nová (nezaplatená)", "GoPay", "Kuriér", "3", null, "14,73"]);
});

Scenario("Test Orders for Logged-in and Unauthenticated User", async ({ I }) => {
    //
    I.say("Check the latest order of Tester");
    I.amOnPage(SL.ORDERS);
    I.waitForElement(locate("h1").withText("Objednávky"), 10);
    seeInTable(I, 1, "14,73", "Nová (nezaplatená)");

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
  I.dontSeeElement(locate("button").withText("Zaplatiť"));
});

Scenario("Bank transfer", async ({ I }) => {
  I.amOnPage(SL.PRODUCTS);
  SL.addToBasket(I, "Ponožky");
  SL.openBasket(I);
  I.clickCss("#orderButton > a");
  SL.fillDeliveryForm(I, "autotest-cash-" + randomNumber, null, "Prevod");

  I.click(locate("input").withAttr({ name: "bSubmit" }));
  I.waitForText("Objednávka úspešne odoslaná", 20);
  I.see("Vašu objednávku uhradíte prevodom na účet : SK59 1100 0000 0026 1000 1237");
  I.see("Dakujeme za platbu prevodom");
  I.dontSeeElement(locate("button").withText("Zaplatiť"));
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
    DTE.waitForModalClose("basketInvoiceDataTable_modal");
    I.wait(0.5);
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
  I.see(deliveryMethodName, "table.invoiceDetailTable");
  I.see("webjetbasket@fexpost.com", "table.invoiceInnerTable");
  I.see("0912345678", "table.invoiceInnerTable");
  I.see("Interway a.s.", "table.invoiceInnerTable");
  I.see(paymentMethodName, ".invoiceDetailTable");
  I.see("Toto je poznamka k objednavke", 'td[colspan="2"]');
  I.see("Ponožky", "table.basketListTable");
  I.see(`${price.replace(".", ",")} €`, ".basketListTableTotalVat .basketPrice");
  I.switchTo();
}

function getCurrentDate() {
  const today = new Date();
  const date =
    today.getDate().toString().padStart(2, "0") +
    "." +
    (today.getMonth() + 1).toString().padStart(2, "0") +
    "." +
    today.getFullYear();
  return date;
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
  I.see(price, row.find("td").at(3));
  I.see(status, row.find("td").at(5));
}