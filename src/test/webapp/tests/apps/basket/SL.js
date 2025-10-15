module.exports = {

    PRODUCTS : "/apps/elektronicky-obchod/produkty/",
    ORDERS: "/apps/elektronicky-obchod/objednavky/",
    METHODS: "/apps/basket/admin/payment-methods/",
    BASKET: "/apps/elektronicky-obchod/kosik/",
    BASKET_ADMIN: "/apps/basket/admin/",
    PRODUCTS_ADMIN: "/apps/basket/admin/product-list/",

    red: 'rgb(255, 75, 88)',
    black: 'rgb(19, 21, 27)',

    clearBasket(I) {
        I.amOnPage(this.PRODUCTS+"?act=deleteall");
        I.amOnPage(this.PRODUCTS);
        I.dontSeeElement(".basketSmallBox")
    },

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
        I.fillField('#contactStreetId', "Mlynské Nivy 71");
        I.fillField("#contactCompanyId", "InterWay, a. s.");
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

    async doGoPayCardPayment(I, shouldBeSuccess = true) {
        const GOPAY_CARD_PAN = process.env.GOPAY_CARD_PAN;
        const GOPAY_CARD_EXP = process.env.GOPAY_CARD_EXP;
        const GOPAY_CARD_CVC = process.env.GOPAY_CARD_CVC;

        I.say("GoPay payment");

        I.waitForText("Platba kartou");

        I.say("Entering card details");
        I.switchTo('iframe[data-cy="cardCommIframe"]');
        I.waitForElement("#cardPan", 10);
        I.fillField("#cardPan", GOPAY_CARD_PAN);
        I.fillField("#cardExp", GOPAY_CARD_EXP);
        I.fillField("#cardCvc", GOPAY_CARD_CVC);
        I.switchTo();
        I.clickCss("button[data-cy=cardSubmit]");

        // Payment is in process of authorization
        I.waitForVisible(locate("div").withText("Prebieha komunikácia s vašou bankou…"), 10);
        I.waitForInvisible(locate("div").withText("Prebieha komunikácia s vašou bankou…"), 15);

        // Now, there is change, that payment test gate give us chnace to allow/blockk payment
        I.wait(5);

        I.say("Check, if bonus approval / denial is needed");
        const num = await I.grabNumberOfVisibleElements("button#confirm");
        if(num > 0) {
            I.say("neede, do bonus logic.");

            if(shouldBeSuccess == true) {
                I.say("Approving payment");
                I.clickCss("button#confirm");
            } else {
                I.say("Denying payment");
                I.clickCss("button#deny");
            }

            I.waitForVisible(locate("div").withText("Dokončovanie platby…"), 20);
            I.waitForInvisible(locate("div").withText("Dokončovanie platby…"), 20);
        }

        I.say("Waiting for bank response");
        //Just in case
        I.wait(2);
        await I.clickIfVisible('button[data-cy="submitRedirectButton"]');

        I.say("Check payment returned message");
        if(shouldBeSuccess == true) {
            I.waitForText("Platba prebehla úspešne.", 20);
        } else {
            I.waitForText("Platba sa nepodarila!", 20);
        }
    },
}