Feature('apps.basket');

const SL = require("./SL.js");

var randomNumber, testerName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        testerName = 'autotest-' + randomNumber
    }
});

Scenario('Test sorting', async ({ I }) => {
    I.amOnPage(SL.PRODUCTS);
    I.waitForText('ELEKTRONICKÝ OBCHOD');
    await sortAndCheck(I, 'asc_title',  ['Džínsy' , 'Ponožky', 'Tričko']);
    await sortAndCheck(I, 'desc_title', ['Tričko' , 'Ponožky', 'Džínsy']);
    await sortAndCheck(I, 'desc_date',   ['Džínsy' , 'Ponožky', 'Tričko']);
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
    DT.checkTableRow('basketInvoiceDataTable', 1, ['', testerName, '', null, 'Nová (nezaplatená)', 'Dobierka', deliveryMethod.split(":")[0], '4', '']);
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

const BasketActions = {
    REMOVE: '.deleteItem',
    INCREASE: '.addItem',
    DECREASE: '.removeItem'
};

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
    I.see(`${expectedNumOfItems}`,'.basketSmallBox .basketSmallItems > span');
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
