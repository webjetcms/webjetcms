Feature('apps.delivery-methods');

Before(({ login }) => {
    login('admin');
});


Scenario('Delivery methods logic tests', ({I, DT, DTE, Document, i18n}) => {
    I.amOnPage("/apps/basket/admin/delivery-methods/");

    Document.screenshot("/redactor/apps/eshop/delivery-methods/datatable.png");

    I.click(locate("td.dt-select-td.cell-not-editable.dt-type-numeric").first());
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor("deliveryMethodsDataTable");


    Document.screenshotElement("#deliveryMethodsDataTable_modal > div > div.DTE_Action_Edit", "/redactor/apps/eshop/delivery-methods/editor.png");
});
