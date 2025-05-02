Feature('apps.basket.basket-invoice');

let paymentDataTable_wrapper = "#datatableFieldDTE_Field_editorFields-payments_wrapper";
let paymentDataTable_modal = "#datatableFieldDTE_Field_editorFields-payments_modal";
let itemsDataTable = "#datatableFieldDTE_Field_editorFields-items_wrapper";
let toaster = "#toast-container-webjet";

Before(({ login }) => {
    login('admin');
});

Scenario('Eshop invoice screens', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/basket/admin/");
    Document.screenshot("/redactor/apps/eshop/invoice/datatable.png");

    DT.filterEquals("editorFields.firstName", "Tester_Screens");
    I.click("Tester_Screens");
    DTE.waitForEditor("basketInvoiceDataTable");

    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_basic.png");

    I.click( locate("#panel-body-dt-basketInvoiceDataTable-basic").find("button.dropdown-toggle") );
    I.waitForElement("div.dropdown-menu.show", 5);

    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/eshop/invoice/editor_basic_status.png");

    I.resizeWindow(1400, 1500);
    I.clickCss("#pills-dt-basketInvoiceDataTable-personal_info-tab");
    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_personal-info.png");
    I.resizeWindow(1280, 760);

    I.clickCss("#pills-dt-basketInvoiceDataTable-fields-tab");
    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_fields.png");

    I.clickCss("#pills-dt-basketInvoiceDataTable-notify-tab");
    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_notify.png");

    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");

    I.click("Džínsy");
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-items");
    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-items_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_items_editor.png");

    I.click( locate("#datatableFieldDTE_Field_editorFields-items_modal > div > div.DTE_Action_Edit").find("button.btn-close-editor") );

    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-items_wrapper > div.row.dt-footer-row > div.col-auto.col-sum", "/redactor/apps/eshop/invoice/editor_items_footer.png");

    I.clickCss("#pills-dt-basketInvoiceDataTable-order_status-tab");
    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_order_status.png");
});

Scenario('Invoice items', ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/basket/admin/");

    DT.filterEquals("editorFields.firstName", "Tester_Screens");
    I.click("Tester_Screens");
    DTE.waitForEditor("basketInvoiceDataTable");

    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
    Document.screenshot("/redactor/apps/eshop/invoice/editor_items.png");

    I.click( locate(itemsDataTable).find("button.buttons-create") );
    I.waitForElement("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");

    I.clickCss("td.sorting_1");

    I.switchTo();
    Document.screenshotElement(locate("#modalIframe").find(".modal-content"), "/redactor/apps/eshop/invoice/editor_items_add.png");
});

Scenario('Invoice payments', ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/basket/admin/");

    DT.filterEquals("editorFields.firstName", "Tester_Sivan");
    I.click("Tester_Sivan");
    DTE.waitForEditor("basketInvoiceDataTable");

    I.clickCss("#pills-dt-basketInvoiceDataTable-payments-tab");
    Document.screenshot("/redactor/apps/eshop/invoice/editor_payments.png");

    Document.screenshotElement(paymentDataTable_wrapper + " > div.row.dt-footer-row > div.col-auto.col-sum", "/redactor/apps/eshop/invoice/editor_payments_footer_a.png");

    I.click( locate(paymentDataTable_wrapper).find("button.buttons-create") );
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    Document.screenshotElement(paymentDataTable_modal + " > div > div.DTE_Action_Create", "/redactor/apps/eshop/invoice/editor_payments_editor.png");

    I.fillField("#DTE_Field_payedPrice", 0.001);
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    Document.screenshotElement(paymentDataTable_modal + " > div > div.DTE_Action_Create", "/redactor/apps/eshop/invoice/editor_payments_editor_minPayment.png");

    I.fillField("#DTE_Field_payedPrice", 100000);
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    Document.screenshotElement(paymentDataTable_modal + " > div > div.DTE_Action_Create", "/redactor/apps/eshop/invoice/editor_payments_editor_maxPayment.png");

    I.clickCss("#DTE_Field_editorFields-saveAsRefund_0");
    I.fillField("#DTE_Field_payedPrice", -15);
    Document.screenshotElement(paymentDataTable_modal + " > div > div.DTE_Action_Create", "/redactor/apps/eshop/invoice/editor_payments_editor_refundation.png");

    I.click( locate(paymentDataTable_modal).find("button.close.btn-close-editor") );

    DT.filterEquals("paymentDescription", "GoPay payment state : PAID");

    I.click( locate(paymentDataTable_wrapper).find("button.buttons-select-all") );

    I.click( locate(paymentDataTable_wrapper).find("button.buttons-refund-payment") );

    I.waitForVisible(toaster);
    I.moveCursorTo(toaster);
    Document.screenshotElement(toaster + " > div.toast-info", "/redactor/apps/eshop/invoice/refund_payment_dialog.png");

    I.click( locate(toaster).find("button.btn-primary") );

    I.waitForVisible( locate(toaster).find("div.toast-error") );
    I.moveCursorTo(toaster);
    Document.screenshotElement(toaster + " > div.toast-error", "/redactor/apps/eshop/invoice/refund_payment_err.png");
    I.clickCss("button.toast-close-button");

    DT.filterContainsForce("paymentDescription", "PAYMENT_REFUND_FAILED");
    Document.screenshot("/redactor/apps/eshop/invoice/editor_payments_refund_err.png");

    DT.filterContainsForce("paymentDescription", "");
    DT.filterSelect("confirmed", "Áno");

    DT.filterSelect("paymentMethod", "GoPay", paymentDataTable_wrapper);

    I.click( locate(paymentDataTable_wrapper).find("button.buttons-select-all") );
    Document.screenshot("/redactor/apps/eshop/invoice/editor_payments_refund_success.png");
});