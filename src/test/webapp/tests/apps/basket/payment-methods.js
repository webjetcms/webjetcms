Feature('apps.payment-methods');

const paymentMethodName = "GoPay";

Before(({ login }) => {
    login('admin');
});

Scenario('Payment methods logic tests', async ({I, DT, DTE, Document}) => {
    I.amOnPage('/apps/basket/admin/payment-methods/');
    Document.switchDomain("test23.tau27.iway.sk");

    I.say('Cant see certain buttons');
    I.dontSeeElement("button.buttons-create");
    I.dontSeeElement("button.buttons-duplicate");
    I.dontSeeElement("button.buttons-celledit");
    I.dontSeeElement("button.buttons-import");
    I.dontSeeElement("button.buttons-export");

    I.say("Check some payment methods");
    I.see(paymentMethodName, "#paymentMethodsDataTable_wrapper");

    I.say("Check edit function");
    DT.filterContains("paymentMethodName", paymentMethodName);
    DT.checkTableRow("paymentMethodsDataTable", 1, ["", paymentMethodName, "nie je nakonfigurovaná"]);
    I.click(paymentMethodName);
    DTE.waitForEditor("paymentMethodsDataTable");

    I.fillField("#DTE_Field_fieldA", "field_a_value");
    I.fillField("#DTE_Field_fieldB", "field_b_value");
    I.fillField("#DTE_Field_fieldC", "field_c_value");
    I.fillField("#DTE_Field_fieldD", "100000");
    I.fillField("#DTE_Field_fieldE", "10");
    I.fillField("#DTE_Field_fieldF", "21");

    I.say("Save and check status change");
    DTE.save();
    DT.checkTableRow("paymentMethodsDataTable", 1, ["", paymentMethodName, "nakonfigurovaná"]);

    I.say("Test delete");
    I.click(".buttons-select-all");
    DT.addContext("payments", "#paymentMethodsDataTable_wrapper")
    I.click(DT.btn.payments_delete_button);
    I.waitForElement("div.DTE_Action_Remove");
    I.waitForText("Naozaj chcete zmazať položku?", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");

    DT.waitForLoader();
    DT.checkTableRow("paymentMethodsDataTable", 1, ["", paymentMethodName, "nie je nakonfigurovaná"]);
});

Scenario('cleanup', ({ I, DT, DTE }) => {
    I.amOnPage('/apps/basket/admin/payment-methods/');
    I.see("test23.tau27.iway.sk", "div.js-domain-toggler div.filter-option-inner-inner");

    DT.filterContains("paymentMethodName", paymentMethodName);
    I.click("button.buttons-select-all");
    DT.addContext("payments", "#paymentMethodsDataTable_wrapper")
    I.click(DT.btn.payments_delete_button);
    DTE.waitForEditor("paymentMethodsDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose("paymentMethodsDataTable_modal");
});

Scenario('logout', ({ I }) => {
    I.logout();
});