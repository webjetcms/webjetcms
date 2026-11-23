module.exports = {

    PRODUCTS : "/apps/elektronicky-obchod/produkty/",
    ORDERS: "/apps/elektronicky-obchod/objednavky/",
    METHODS: "/apps/basket/admin/payment-methods/",
    BASKET: "/apps/elektronicky-obchod/kosik/",
    BASKET_ADMIN: "/apps/basket/admin/",
    PRODUCTS_ADMIN: "/apps/basket/admin/product-list/",

    red: 'rgb(224, 0, 40)',
    black: 'rgb(19, 21, 27)',

    DeliveryMethods: {
        inStoreDelivery: "Vyzdvihnutie v predajni",
        byMailDelivery: "Štandardná pošta"
    },

    Countries: {
        sk: "Slovensko",
        cz: "Česká republika",
        pl: "Poľsko"
    },

    PaymentMethods: {
        cashOnDelivery: "Dobierka",
        GoPay: "GoPay",
        moneyTransfer: "Prevodom"
    },

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

    fillDeliveryForm(I, TempMail, testerName, deliveryMethodName = null, paymentMethodName = null) {
        I.say("Filling delivery form");
        I.clickCss(".accordion-button[aria-controls=orderFormDeliveryInfo]");
        I.clickCss(".accordion-button[aria-controls=orderFormCompany]");
        I.fillField('#contactFirstNameId', testerName);
        I.fillField('#contactLastNameId', 'Playwright');
        I.clearField('#contactEmailId');
        I.fillField('#contactEmailId', 'webjetbasket'+TempMail.getTempMailDomain());
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

        await I.usePlaywrightTo('Enter the card details in the GoPay sandbox frame', async ({ page }) => {
            // GoPay navigates its iframe to the secure origin after attaching it.
            const secureUrl = /^https:\/\/secure\.sandbox\.gopay\.com\//;
            const frame = page.frame({ url: secureUrl }) || await page.waitForEvent('framenavigated', {
                predicate: frame => secureUrl.test(frame.url()), timeout: 15000
            });
            await frame.locator('#cardPan').fill(GOPAY_CARD_PAN);
            await frame.locator('#cardExp').fill(GOPAY_CARD_EXP);
            await frame.locator('#cardCvc').fill(GOPAY_CARD_CVC);
            await frame.locator('#cardCvc').blur();
            await page.getByTestId('card.submit').click({ timeout: 15000 });
        });

        const challengeButton = shouldBeSuccess ? 'button#confirm' : 'button#deny';
        const returnButton = 'button:has-text("Späť do obchodu"), a:has-text("Späť do obchodu")';
        const resultAlert = shouldBeSuccess ? '.alert-success' : '.alert-danger';
        const resultText = shouldBeSuccess ? 'Platba prebehla úspešne.' : 'Platba sa nepodarila!';
        I.waitForVisible([challengeButton, returnButton, resultAlert].join(', '), 40);
        if (await I.grabNumberOfVisibleElements(challengeButton)) {
            I.clickCss(challengeButton);
            I.waitForVisible(returnButton + ', ' + resultAlert, 40);
        }
        await I.clickIfVisible(returnButton);
        I.waitForText(resultText, 30, resultAlert);
    },

    selectCurrency(I, currency) {
        I.say("Selecting currency");
        I.clickCss("button[data-id='currencySelect']");
        I.waitForVisible( locate("#currencySelect_wrapper").find("div.dropdown-menu.show") );
        within(locate("#currencySelect_wrapper").find("div.dropdown-menu.show"), () => {
            I.click( locate("a.dropdown-item > span").withText(currency));
        });
    },

    validateCurrencyOptions(I, wantSee, dontWantSee) {
        I.say("Checking currency options");
        I.clickCss("button[data-id='currencySelect']");
        I.waitForVisible( locate("#currencySelect_wrapper").find("div.dropdown-menu.show") );
        within(locate("#currencySelect_wrapper").find("div.dropdown-menu.show"), () => {
            for(const currency of wantSee) {
                I.seeElement( locate("a.dropdown-item > span").withText(currency));
            }

            for(const currency of dontWantSee) {
                I.dontSeeElement( locate("a.dropdown-item > span").withText(currency));
            }
        });
        I.pressKey('Escape');
    }
}