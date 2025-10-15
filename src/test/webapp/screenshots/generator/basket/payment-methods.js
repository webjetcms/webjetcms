Feature('apps.payment-methods');

Before(({ login }) => {
    login('admin');
});

Scenario('Payment methods logic tests', ({I, DT, DTE, Document}) => {
    I.amOnPage('/apps/basket/admin/payment-methods/');

    Document.screenshot("/redactor/apps/eshop/payment-methods/datatable.png");

    DT.filterEquals("paymentMethodName", "Prevodom");
    I.click("Prevodom");
    DTE.waitForEditor("paymentMethodsDataTable");

    Document.screenshotElement("#paymentMethodsDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/payment-methods/editor_A.png");
    DTE.cancel();

    DT.filterEquals("paymentMethodName", "GoPay");
    I.click("GoPay");
    DTE.waitForEditor("paymentMethodsDataTable");

    // Hide all values - they are private
    I.fillField("#DTE_Field_fieldA", "");
    I.fillField("#DTE_Field_fieldB", "");
    I.fillField("#DTE_Field_fieldC", "");
    I.fillField("#DTE_Field_fieldD", "");
    Document.screenshotElement("#paymentMethodsDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/payment-methods/editor_B.png");
    DTE.cancel();
});