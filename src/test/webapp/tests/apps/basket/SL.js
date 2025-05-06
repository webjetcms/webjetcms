module.exports = {

    PRODUCTS : "/apps/elektronicky-obchod/produkty/",
    ORDERS: "/apps/elektronicky-obchod/objednavky/1.html",
    METHODS: "/apps/eshop/admin/payment-methods/",
    BASKET: "/apps/elektronicky-obchod/kosik/",
    BASKET_ADMIN: "/apps/basket/admin/",
    PRODUCTS_ADMIN: "/apps/basket/admin/product-list/",

    red: 'rgb(255, 75, 88)',
    black: 'rgb(19, 21, 27)',

    addToBasket(I, productName){
        I.say(`Adding ${productName} to the basket`);
        const addToBasketButton = locate('.thumbnail').withText(productName).find('.addToBasket');
        I.click(addToBasketButton);
        I.wait(0.2);
    },

    openBasket(I) {
        I.say('Opening basket');
        I.clickCss('.showBasket');
        I.waitForElement('.basketBox', 10);
        I.wait(0.2);
    },

    closeBasket(I) {
        I.clickCss('.btn.btn-secondary.closeBasket');
        I.waitForInvisible('.basketBox', 10);
        I.wait(0.2);
    },

    fillDeliveryForm(I, testerName, deliveryMethodName = null, paymentMethodName = null) {
        I.say("Filling delivery form");
        I.clickCss(".accordion-button[aria-controls=orderFormDeliveryInfo]");
        I.clickCss(".accordion-button[aria-controls=orderFormCompany]");
        I.fillField('#contactFirstNameId', testerName);
        I.fillField('#contactLastNameId', 'Playwright');
        I.clearField('#contactEmailId');
        I.fillField('#contactEmailId', 'webjetbasket@fexpost.com');
        I.fillField('#contactStreetId', "Mlýnske Nivy 71");
        I.fillField("#contactCompanyId", "Interway a.s.");
        I.fillField("#contactPhoneId", "0912345678")
        I.fillField('#contactCityId', 'Bratislava');
        I.fillField('#contactZipId', '82105');

        if (deliveryMethodName)I.selectOption("#deliveryMethodId", deliveryMethodName);
        if (paymentMethodName) I.selectOption("#paymentMethodId", paymentMethodName);
        I.fillField("#userNoteId", "Toto je poznamka k objednavke");
    },

    async getFontColor(I, row, col){
        const elementSelector = locate("#datatableFieldDTE_Field_editorFields-payments tbody tr:nth-child("+row+") td:nth-child("+col+")");
        return await I.grabCssPropertyFrom(elementSelector, 'color');
    },
}