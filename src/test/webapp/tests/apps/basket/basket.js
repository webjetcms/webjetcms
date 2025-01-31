Feature('apps.basket');

var randomNumber, testerName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        testerName = 'autotest-' + randomNumber
    }
});

Scenario('Test sorting', async ({ I }) => {
    I.amOnPage('/apps/elektronicky-obchod/');
    I.waitForText('ELEKTRONICKÝ OBCHOD');
    await sortAndCheck(I, 'asc_title',  ['Džínsy' , 'Ponožky', 'Tričko']);
    await sortAndCheck(I, 'desc_title', ['Tričko' , 'Ponožky', 'Džínsy']);
    await sortAndCheck(I, 'asc_date',   ['Ponožky', 'Tričko' , 'Džínsy']);
    await sortAndCheck(I, 'desc_date',  ['Džínsy' , 'Tričko' , 'Ponožky']);
    await sortAndCheck(I, 'asc_price',  ['Ponožky', 'Tričko' , 'Džínsy']);
    await sortAndCheck(I, 'desc_price', ['Džínsy' , 'Tričko' , 'Ponožky']);
});

Scenario('Verify product list and UI elements in electronic shop', ({ I }) => {
    I.amOnPage('/apps/elektronicky-obchod/');
    I.waitForText('ELEKTRONICKÝ OBCHOD');

    I.see('Ponožky', '.basket .col-md-3 h4');
    I.seeElement('.basket .col-md-3 img[src*="s_socks.jpg"]');
    I.see('8,61', '.basket .col-md-3 .price');

    I.see('Tričko', '.basket .col-md-3 h4');
    I.seeElement('.basket .col-md-3 img[src*="s_tshirt.jpg"]');
    I.see('12,30', '.basket .col-md-3 .price');

    I.see('Džínsy', '.basket .col-md-3 h4');
    I.seeElement('.basket .col-md-3 img[src*="s_jeans.jpg"]');
    I.see('30,75', '.basket .col-md-3 .price');
    I.seeElement('.basket .col-md-3 .btn.addToBasket');

    I.seeNumberOfVisibleElements('div.productsOrder > select.filterKategorii', 2);
});

Scenario('Validate basket operations: add, modify, remove, and view', ({ I }) => {
    I.amOnPage('/apps/elektronicky-obchod/');
    I.waitForText('ELEKTRONICKÝ OBCHOD');
    I.dontSeeElement('.basketSmallBox')

    I.say('Adding one product');
    addToBasket(I, 'Ponožky');
    checkBasketSmallBox(I, '1', '8,61');

    I.say('Adding duplicated product');
    addToBasket(I, 'Ponožky');
    checkBasketSmallBox(I, '2', '17,22');

    I.say('Adding different products');
    addToBasket(I, 'Tričko');
    addToBasket(I, 'Džínsy');
    checkBasketSmallBox(I, '4', '60,27');

    I.say('Opening and closing basket');
    openBasket(I);
    I.clickCss('#orderContinurButton > a');
    I.dontSeeElement('.basketBox');
    openBasket(I);
    closeBasket(I);

    I.say('Removing item from basket');
    openBasket(I);
    I.see('60,27\u00A0€','span.basketPrice');
    modifyBasket(I, 'Džínsy', BasketActions.REMOVE);
    I.see('29,52\u00A0€','span.basketPrice');
    checkBasketSmallBox(I, '3', '29,52');

    I.say('Decreasing amout of product in basket');
    checkAmountInBasket(I, 'Ponožky', '2');
    modifyBasket(I, 'Ponožky', BasketActions.DECREASE);
    checkAmountInBasket(I, 'Ponožky', '1');

    I.say('Increasing amout of product in basket');
    checkAmountInBasket(I, 'Tričko', '1');
    modifyBasket(I, 'Tričko', BasketActions.INCREASE);
    checkAmountInBasket(I, 'Tričko', '2');
});

Scenario('Remove all items from basket', async ({ I }) => {
    I.amOnPage('/apps/elektronicky-obchod/');
    I.waitForText('ELEKTRONICKÝ OBCHOD');
    const isBasketVisible = await I.grabNumberOfVisibleElements('.showBasket');
    if(isBasketVisible){
        openBasket(I);
        while(await I.grabNumberOfVisibleElements('.deleteItem') > 0){
            I.clickCss('.deleteItem');
            I.wait(0.2);
        }
    }
});

Scenario('Create and submit order', async ({ I, DT }) => {
    I.amOnPage('/apps/elektronicky-obchod/');
    I.waitForText('ELEKTRONICKÝ OBCHOD');

    I.say('Adding products to the basket');
    addToBasket(I, 'Tričko');
    addToBasket(I, 'Ponožky');
    checkBasketSmallBox(I, '2', '20,91');

    I.say('Opening basket and filling in delivery details');
    openBasket(I);
    I.clickCss('#orderButton > a');
    fillDeliveryForm(I)

    I.say('Selecting random delivery method');
    const deliveryMethodOptions = await I.grabTextFromAll('select[name="deliveryMethod"] option');
    const randomIndex = Math.floor(Math.random() * deliveryMethodOptions.length);
    const deliveryMethod = deliveryMethodOptions[randomIndex];
    I.selectOption('#deliveryMethodId', deliveryMethod);

    I.say('Submitting the order');
    I.click(locate('input').withAttr({name : 'bSubmit'}));
    I.waitForText('Objednávka úspešne odoslaná', 20);

    I.say('Verifying the order details in the admin panel');
    I.amOnPage('/apps/basket/admin/');
    DT.filterEquals('deliveryName', testerName);
    I.dontSeeElement('Nenašli sa žiadne vyhovujúce záznamy');
    DT.checkTableCell('basketInvoiceDataTable', '1', '2', testerName );
    DT.checkTableCell('basketInvoiceDataTable', '1', '4', 'Nová' );
    DT.checkTableCell('basketInvoiceDataTable', '1', '6', deliveryMethod );
    DT.checkTableCell('basketInvoiceDataTable', '1', '7', '2' );
    DT.checkTableCell('basketInvoiceDataTable', '1', '8', '20,91');
});

Scenario('Delete order', ({ I, DT, DTE }) => {
    I.amOnPage('/apps/basket/admin/');
    DT.waitForLoader();
    DT.filterEquals('deliveryName', testerName);
    I.clickCss('.buttons-select-all');
    I.clickCss('.buttons-edit');
    DTE.selectOption('statusId', 'Stornovaná');
    DTE.save();
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
    I.selectOption('div.productsOrder > select.filterKategorii', sortMethod);
    I.waitForElement('.row.basket .thumbnail > h4', 10);
    I.wait(0.2);
    const sortedProducts = await I.grabTextFromAll('.row.basket .thumbnail > h4');
    I.assertDeepEqual(sortedProducts, expectedOrder, `Items are not sorted correctly according to sort method ${sortMethod}`);
}

function addToBasket(I, productName){
    const addToBasketButton = locate('.thumbnail').withText(productName).find('.addToBasket');
    I.click(addToBasketButton);
    I.wait(0.2);

}

function openBasket(I) {
    I.clickCss('.showBasket');
    I.waitForElement('.basketBox', 10);
    I.wait(0.2);
}

function closeBasket(I) {
    I.clickCss('tr.basketListTableHeader > th > a.closeBasket');
    I.waitForInvisible('.basketBox', 10);
    I.wait(0.2);
}

function modifyBasket(I, nameProduct, action) {
    I.click(locate('tr')
        .withDescendant('td > a.newWindow')
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

function checkAmountInBasket(I, nameProduct, expectedAmount){
    I.seeElement(locate('tr')
        .withDescendant('td > a.newWindow')
        .withText(nameProduct)
        .find('.basketQty')
        .withAttr({ value: expectedAmount }));
}

function fillDeliveryForm(I) {
    I.fillField('#deliveryNameId', testerName);
    I.fillField('#deliverySurNameId', 'buyer');
    I.clearField('#contactEmailId');
    I.fillField('#contactEmailId', 'webjetbasket@fexpost.com');
    I.fillField('#deliveryStreetId', 'Mlýnske Nivy 71');
    I.fillField('#deliveryCityId', 'Bratislava');
    I.fillField('#deliveryZipId', '82105');
}
