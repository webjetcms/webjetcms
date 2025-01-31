Feature('apps.basket.basket-invoice');

let paymentDataTable = "#datatableFieldDTE_Field_editorFields-payments_wrapper";
let itemsDataTable = "#datatableFieldDTE_Field_editorFields-items_wrapper";

Before(({ login }) => {
    login('admin');
});

Scenario('Eshop invoice screens', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/basket/admin/");
    Document.screenshot("/redactor/apps/eshop/invoice/datatable.png");

    DT.filter("deliveryName", "Tester_screens");
    I.click("Tester_screens");
    DTE.waitForEditor("basketInvoiceDataTable");

    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_basic.png");

    I.click( locate("#panel-body-dt-basketInvoiceDataTable-basic").find("button.dropdown-toggle") );
    I.waitForElement("div.dropdown-menu.show", 5);

    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/eshop/invoice/editor_basic_status.png");

    I.clickCss("#pills-dt-basketInvoiceDataTable-notify-tab");
    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_notify.png");

    I.clickCss("#pills-dt-basketInvoiceDataTable-payments-tab");
    Document.screenshot("/redactor/apps/eshop/invoice/editor_payments.png");

    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-payments_wrapper > div.row.dt-footer-row > div.col-auto.col-sum", "/redactor/apps/eshop/invoice/editor_payments_footer_a.png");

    I.click( locate(paymentDataTable).find("button.buttons-create") );
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-payments");
    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-payments_modal > div > div.DTE_Action_Create", "/redactor/apps/eshop/invoice/editor_payments_editor.png");

    I.fillField("#DTE_Field_payedPrice", 0.001);
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-payments_modal > div > div.DTE_Action_Create", "/redactor/apps/eshop/invoice/editor_payments_editor_minPayment.png");

    I.fillField("#DTE_Field_payedPrice", 100000);
    DTE.save("datatableFieldDTE_Field_editorFields-payments");
    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-payments_modal > div > div.DTE_Action_Create", "/redactor/apps/eshop/invoice/editor_payments_editor_maxPayment.png");

    I.click( locate("#datatableFieldDTE_Field_editorFields-payments_modal").find("button.dropdown-toggle") );
    I.waitForElement("div.dropdown-menu.show");
    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/eshop/invoice/editor_payments_editor_paymentMethods.png");

    I.fillField("#DTE_Field_payedPrice", 150);
    DTE.save("datatableFieldDTE_Field_editorFields-payments");

    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-payments_wrapper > div.row.dt-footer-row > div.col-auto.col-sum", "/redactor/apps/eshop/invoice/editor_payments_footer_b.png");

    I.say("remove all payments");
        I.click( locate(paymentDataTable).find("button.buttons-select-all") );
        I.click( locate(paymentDataTable).find("button.buttons-remove") );
        I.waitForElement(".DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader();

    I.clickCss("#pills-dt-basketInvoiceDataTable-items-tab");
    Document.screenshot("/redactor/apps/eshop/invoice/editor_items.png");

    I.click("Smasung Galaxy S9 64GB");
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-items");
    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-items_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_items_editor.png");

    I.click( locate("#datatableFieldDTE_Field_editorFields-items_modal > div > div.DTE_Action_Edit").find("button.btn-close-editor") );

    Document.screenshotElement("#datatableFieldDTE_Field_editorFields-items_wrapper > div.row.dt-footer-row > div.col-auto.col-sum", "/redactor/apps/eshop/invoice/editor_items_footer.png");

    I.clickCss("#pills-dt-basketInvoiceDataTable-order_status-tab");
    Document.screenshotElement("#basketInvoiceDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/invoice/editor_order_status.png");
});