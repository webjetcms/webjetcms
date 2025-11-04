Feature('apps.basket');

const SL = require("./SL.js");

var randomNumber, testerName;

const BasketActions = {
    REMOVE: '.deleteItem',
    INCREASE: '.addItem',
    DECREASE: '.removeItem'
};

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        testerName = 'autotest-' + randomNumber
    }
});

Scenario('basketDisplayCurrency to default', ({ I, Document }) => {
    Document.setConfigValue("basketDisplayCurrency", "eur");
});

Scenario('Test sorting', async ({ I }) => {
    I.amOnPage(SL.PRODUCTS);
    I.waitForText('ELEKTRONICKÝ OBCHOD');
    await sortAndCheck(I, 'asc_title',  ['Džínsy' , 'Ponožky', 'Tričko']);
    await sortAndCheck(I, 'desc_title', ['Tričko' , 'Ponožky', 'Džínsy']);
    await sortAndCheck(I, 'desc_date',   ['Tričko' , 'Ponožky', 'Džínsy']);
    await sortAndCheck(I, 'asc_date',  ['Tričko' , 'Ponožky', 'Džínsy']);
    await sortAndCheck(I, 'asc_price',  ['Ponožky', 'Tričko' , 'Džínsy']);
    await sortAndCheck(I, 'desc_price', ['Džínsy' , 'Tričko' , 'Ponožky']);
});

Scenario('Verify product list and UI elements in electronic shop', ({ I }) => {
    I.amOnPage(SL.PRODUCTS);
    I.waitForText('ELEKTRONICKÝ OBCHOD');

    I.see('Ponožky', '.basket .col-md-3 h4');
    I.waitForElement('.basket .col-md-3 img[src*="s_socks.jpg"]', 10);
    I.see('8,61', '.basket .col-md-3 .price');

    I.see('Tričko', '.basket .col-md-3 h4');
    I.waitForElement('.basket .col-md-3 img[src*="s_tshirt.jpg"]', 10);
    I.see('12,30', '.basket .col-md-3 .price');

    I.see('Džínsy', '.basket .col-md-3 h4');
    I.waitForElement('.basket .col-md-3 img[src*="s_jeans.jpg"]', 10);
    I.see('30,75', '.basket .col-md-3 .price');
    I.seeElement('.basket .col-md-3 .btn.addToBasket');

    I.seeNumberOfVisibleElements('div.productsOrder > select.filterKategorii', 2);
});

Scenario('Validate basket operations: add, modify, remove, and view', async ({ I }) => {
    I.amOnPage(SL.PRODUCTS);
    I.waitForText('ELEKTRONICKÝ OBCHOD');
    I.dontSeeElement('.basketSmallBox')

    I.say('Adding one product');
    SL.addToBasket(I, 'Ponožky');
    checkBasketSmallBox(I, '1', '8,61');

    I.say('Adding duplicated product');
    SL.addToBasket(I, 'Ponožky');
    checkBasketSmallBox(I, '2', '17,22');

    I.say('Adding different products');
    SL.addToBasket(I, 'Tričko');
    SL.addToBasket(I, 'Džínsy');
    checkBasketSmallBox(I, '4', '60,27');

    I.say('Opening and closing basket');
    SL.openBasket(I);
    I.clickCss('#orderContinurButton > a');
    I.waitForInvisible('.basketBox', 10);
    SL.openBasket(I);
    SL.closeBasket(I);

    I.say('Removing item from basket');
    SL.openBasket(I);
    I.see('60,27\u00A0€','span.basketPrice');
    modifyBasket(I, 'Džínsy', BasketActions.REMOVE);
    I.see('29,52\u00A0€','span.basketPrice');
    checkBasketSmallBox(I, '3', '29,52');

    I.say('Decreasing amout of product in basket');
    await checkAmountInBasket(I, 'Ponožky', '2');
    modifyBasket(I, 'Ponožky', BasketActions.DECREASE);
    await checkAmountInBasket(I, 'Ponožky', '1');

    I.say('Increasing amout of product in basket');
    await checkAmountInBasket(I, 'Tričko', '1');
    modifyBasket(I, 'Tričko', BasketActions.INCREASE);
    await checkAmountInBasket(I, 'Tričko', '2');

    I.amOnPage(SL.BASKET);
    modifyBasket(I, 'Tričko', BasketActions.DECREASE, 'td.w-5> a');
    await checkAmountInBasket(I);

    modifyBasket(I, 'Ponožky', BasketActions.INCREASE, 'td.w-5> a');
    await checkAmountInBasket(I);

    modifyBasket(I, 'Ponožky', BasketActions.REMOVE, 'td.w-5> a');
    await checkAmountInBasket(I);

    I.amOnPage(SL.PRODUCTS);
    SL.openBasket(I);
    modifyBasket(I, 'Tričko', BasketActions.DECREASE, 'td.w-5> a');
    I.waitForInvisible('.basketSmallBox', 10);
    I.dontSeeElement(".basketBox");
});

Scenario('Remove all items from basket', async ({ I }) => {
    I.amOnPage(SL.PRODUCTS);
    I.waitForText('ELEKTRONICKÝ OBCHOD');
    const isBasketVisible = await I.grabNumberOfVisibleElements('.showBasket');
    if(isBasketVisible){
        SL.openBasket(I);
        while(await I.grabNumberOfVisibleElements('.deleteItem') > 0){
            I.clickCss('.deleteItem');
            I.wait(0.2);
        }
    }
});

Scenario('Create and submit order', async ({ I, DT }) => {
    I.amOnPage(SL.PRODUCTS);
    I.waitForText('ELEKTRONICKÝ OBCHOD');

    I.say('Adding products to the basket');
    SL.addToBasket(I, 'Tričko');
    SL.addToBasket(I, 'Ponožky');
    checkBasketSmallBox(I, '2', '20,91');

    I.say('Opening basket and filling in delivery details');
    SL.openBasket(I);
    I.clickCss('#orderButton > a');
    SL.fillDeliveryForm(I, testerName);

    I.say('Selecting random delivery method');
    const deliveryMethodOptions = await I.grabTextFromAll('select[name="deliveryMethod"] option');
    const randomIndex = Math.floor(Math.random() * deliveryMethodOptions.length);
    const deliveryMethod = deliveryMethodOptions[randomIndex];
    I.selectOption('#deliveryMethodId', deliveryMethod);

    I.say('Submitting the order');
    I.click(locate('input').withAttr({name : 'bSubmit'}));
    I.waitForText('Objednávka úspešne odoslaná', 20);

    I.say('Verifying the order details in the admin panel');
    I.amOnPage(SL.BASKET_ADMIN);
    DT.filterEquals('editorFields.firstName', testerName);
    I.dontSeeElement('Nenašli sa žiadne vyhovujúce záznamy');
    DT.checkTableRow('basketInvoiceDataTable', 1, ['', testerName, '', null, 'Nová (nezaplatená)', SL.PaymentMethods.cashOnDelivery, deliveryMethod.split(":")[0], '4', '']);
});

Scenario('Delete order', ({ I, DT, DTE }) => {
    I.amOnPage(SL.BASKET_ADMIN);
    DT.waitForLoader();
    DT.filterEquals('editorFields.firstName', testerName);
    I.clickCss('.buttons-select-all');
    I.clickCss('.buttons-edit');
    DTE.waitForEditor('basketInvoiceDataTable');
    I.selectOption('#DTE_Field_statusId', 'Stornovaná');
    DTE.save('basketInvoiceDataTable');
    I.clickCss('.buttons-remove');
    I.click('Zmazať');
    DT.waitForLoader();
});

Scenario('Delivery method by country logic', async ({ I }) => {
    I.amOnPage(SL.PRODUCTS);

    I.say("Add product to basket so we can show order form");
        SL.addToBasket(I, 'Tričko');
        SL.openBasket(I);
        I.clickCss('#orderButton > a');
        I.waitForVisible("#orderFormAccordion");

    I.say("Check selected delivery method");
        checkDeliveryValue(I, "Doručenie poštou: 6,15 €");

    I.say("Change constact and delivery country and verify value");
        I.selectOption('#contactCountryId', SL.Countries.cz);
        checkDeliveryValue(I, "Vyzdvihnutie v predajni: 0,00 €");

        I.clickCss("button.accordion-button.collapsed[data-bs-target='#orderFormDeliveryInfo']");
        I.selectOption('#deliveryCountryId', SL.Countries.sk);
        checkDeliveryValue(I, "Doručenie poštou: 6,15 €");

        I.selectOption('#contactCountryId', SL.Countries.pl);
        checkDeliveryValue(I, "Doručenie poštou: 6,15 €");

        I.selectOption('#deliveryCountryId', "-");
        checkDeliveryValue(I, "Vyzdvihnutie v predajni: 0,00 €");
});

Scenario('Check price and currency based on selected basketDisplayCurrency', async ({ I, Document }) => {
    //Clear cache
    I.amOnPage("/admin/v9/settings/cache-objects/");
    I.clickCss("#datatableInit_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(3)");
    I.click(locate("button.btn-primary").withText("Potvrdiť"));

    I.relogin("tester");

    Document.setConfigValue("basketDisplayCurrency", "eur");

    I.say("Do EUR check");
        I.amOnPage(SL.PRODUCTS);
        await checkItemPriceLabel(I, "Tričko", "12,30 €");
        await checkItemPriceLabel(I, "Ponožky", "8,61 €");
        await checkItemPriceLabel(I, "Džínsy", "30,75 €");

        SL.addToBasket(I, 'Džínsy');
        SL.addToBasket(I, 'Ponožky');
        SL.openBasket(I);

        await checkItemPriceLabelInBasketBox(I, "itemId_126527", "30,75 €");
        await checkItemPriceLabelInBasketBox(I, "itemId_126526", "8,61 €");

        const basketPriceA = await I.grabTextFrom(locate("div.basketBox").find("span.basketPrice"));
        I.assertEqual("39,36 €", sanitazeValue(basketPriceA));

        I.clickCss('#orderButton > a');
        I.waitForVisible("#orderFormAccordion");

        checkDeliveryValue(I, "Doručenie poštou: 6,15 €");

        const totalOrderPriceA = await I.grabTextFrom("span.totalOrderPrice");
        I.assertEqual("46,71 eur", sanitazeValue(totalOrderPriceA));

    Document.setConfigValue("basketDisplayCurrency", "czk");
    I.say("Do CZK check");
        I.amOnPage(SL.PRODUCTS);
        await checkItemPriceLabel(I, "Tričko", "298,03 Kč");
        await checkItemPriceLabel(I, "Ponožky", "208,62 Kč");
        await checkItemPriceLabel(I, "Džínsy", "745,07 Kč");

        SL.addToBasket(I, 'Džínsy');
        SL.addToBasket(I, 'Ponožky');
        SL.openBasket(I);

        await checkItemPriceLabelInBasketBox(I, "itemId_126527", "745,07 Kč");
        await checkItemPriceLabelInBasketBox(I, "itemId_126526", "208,62 Kč");

        //2x that items
        const basketPriceB = await I.grabTextFrom(locate("div.basketBox").find("span.basketPrice"));
        I.assertEqual("1 907,39 Kč", sanitazeValue(basketPriceB));

        I.clickCss('#orderButton > a');
        I.waitForVisible("#orderFormAccordion");

        checkDeliveryValue(I, "Doručenie poštou: 149,01 Kč");

        const totalOrderPriceB = await I.grabTextFrom("span.totalOrderPrice");
        I.assertEqual("2085,48 czk", sanitazeValue(totalOrderPriceB));
});

Scenario('basketDisplayCurrency to default', ({ I, Document }) => {
    Document.setConfigValue("basketDisplayCurrency", "eur");
});


async function checkItemPriceLabelInBasketBox(I, index, priceLabel) {
    const value = await I.grabTextFrom("div.basketBox > table > tbody > tr." + (index) + " > td.basketPrice");
    I.assertEqual(priceLabel, sanitazeValue(value));
}

async function checkItemPriceLabel(I, itemLabel, priceLabel) {
    const foundPriceLabel = await I.grabTextFrom( (locate("div.basket > div").withChild( locate("span > h4 > a").withText(itemLabel))).find(locate("p.price")) );
    I.assertEqual(priceLabel, sanitazeValue(foundPriceLabel));
}

function sanitazeValue(value) {
    value = value.trim();
    value = value.replace(/&nbsp;/g, ' ');
    value = value.replace(/\u00A0/g, ' ');
    value = value.replace(/\s+/g, ' ');
    return value;
}

async function checkDeliveryValue(I, wantedValue) {
    const deliveryValue = await I.grabTextFrom("#deliveryMethodId");
    I.assertEqual(deliveryValue.trim(), wantedValue.trim());
}

async function sortAndCheck(I, sortMethod, expectedOrder) {
    await I.executeScript((sortMethod) => {
        $(".row > .productsOrder").find("select.filterKategorii[name='orderType']").val(sortMethod).change();
    }, sortMethod);

    I.waitForElement('.row.basket .thumbnail > h4', 10);
    I.wait(0.2);

    const sortedProducts = await I.grabTextFromAll('.row.basket .thumbnail > h4');
    I.say("FOUND : " + sortedProducts);
    I.assertDeepEqual(sortedProducts, expectedOrder, `Items are not sorted correctly according to sort method ${sortMethod}`);
}

function modifyBasket(I, nameProduct, action, selector = 'td > a.newWindow') {
    I.click(locate('tr')
        .withDescendant(selector)
        .withText(nameProduct)
        .find(action)
    );
    I.wait(0.2);
}

function checkBasketSmallBox(I, expectedNumOfItems, expectedTotalPrice){
    I.seeElement('.basketSmallBox')
    I.waitForText(`${expectedNumOfItems}`,'.basketSmallBox .basketSmallItems > span');
    I.see(`${expectedTotalPrice}\u00A0€`,'.basketSmallBox .basketSmallPrice > span');
}

async function checkAmountInBasket(I, nameProduct = null, expectedAmount = null){
    if (nameProduct){
        I.seeElement(locate('tr')
            .withDescendant('td > a.newWindow')
            .withText(nameProduct)
            .find('.basketQty')
            .withAttr({ value: expectedAmount }));
        I.openNewTab();
        I.amOnPage(SL.BASKET);
    }
    let totalCalculated = 0;
    const rows = await I.grabNumberOfVisibleElements('.basketListTable .itemTr');
    for (let i = 1; i <= rows; i++) {
        const quantity = await I.grabValueFrom(locate('.basketQty').at(i));
        const unitPriceText = await I.grabTextFrom(locate('.basketPrice').at(i));
        const totalPriceText = await I.grabTextFrom(locate('tr.itemTr > td:nth-child(4)').at(i));
        const unitPrice = parseFloat(unitPriceText.replace(',', '.').replace(' €', ''));
        const totalPrice = parseFloat(totalPriceText.replace(',', '.').replace(' €', ''));
        const expectedTotal = unitPrice * parseInt(quantity, 10);
        I.say(`Checking item ${i}: ${quantity} x ${unitPrice} = ${expectedTotal}`);
        I.assertEqual(totalPrice, expectedTotal, `Total price for item ${i} is incorrect`);
        totalCalculated += expectedTotal;
    }
    const basketTotalText = await I.grabTextFrom('.basketPriceText + .basketPrice');
    const basketTotal = parseFloat(basketTotalText.replace(',', '.').replace(' €', ''));
    I.assertEqual(basketTotal, totalCalculated, 'Basket total price is incorrect');
    if (nameProduct){
        I.switchToPreviousTab();
        I.closeOtherTabs();
    }
}