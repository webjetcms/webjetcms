module.exports = {

    PRODUCTS : "/apps/elektronicky-obchod/produkty/",
    ORDERS: "/apps/elektronicky-obchod/zoznam-objednavok/",
    METHODS: "/apps/eshop/admin/payment-methods/",
    BASKET: "/apps/elektronicky-obchod/kosik/",
    BASKET_ADMIN: "/apps/basket/admin/",

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
        I.clickCss('tr.basketListTableHeader > th > a.closeBasket');
        I.waitForInvisible('.basketBox', 10);
        I.wait(0.2);
    },

    fillDeliveryForm(I, testerName, deliveryMethodName = null, paymentMethodName = null) {
        I.say("Filling delivery form");
        I.clickCss("button[aria-controls='orderFormDeliveryInfo']");
        I.fillField('#contactFirstNameId', testerName);
        I.fillField('#deliveryNameId', testerName);
        I.fillField('#deliverySurNameId', 'Playwright');
        I.clearField('#contactEmailId');
        I.fillField('#contactEmailId', 'webjetbasket@fexpost.com');
        I.fillField('#deliveryStreetId', "Mlýnske Nivy 71");
        I.clickCss("button[aria-controls='orderFormCompany']");
        I.fillField("#contactCompanyId", "Interway a.s.");
        I.fillField("#contactPhoneId", "0912345678")
        I.fillField('#deliveryCityId', 'Bratislava');
        I.fillField('#deliveryZipId', '82105');
        I.fillField("#contactCityId", "Bratislava");
        I.fillField("#contactZipId", "82105");

        if (deliveryMethodName)I.selectOption("#deliveryMethodId", deliveryMethodName);
        if (paymentMethodName) I.selectOption("#paymentMethodId", paymentMethodName);
        I.fillField("#userNoteId", "Toto je poznamka k objednavke");
    }
}