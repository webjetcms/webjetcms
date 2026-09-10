Feature('apps.basket.rounding');

const SL = require('./SL');

const configuration = {};
const products = [];
let marker;

Before(({ I, login }) => {
    login('admin');
    if (marker == null) marker = 'rounding-autotest-' + I.getRandomText();
});

Scenario('Prepare isolated rounding products and preserve configuration', async ({ I, DT }) => {
    I.amOnPage(SL.PRODUCTS_ADMIN);
    DT.waitForLoader();
    for (const name of ['basketRoundPrices', 'basketDisplayCurrency', 'basketProductCurrency', 'basketMaxQty', 'currencyTagRound', 'currencyFormat']) {
        configuration[name] = await browserRequest(I, '/admin/rest/settings/configuration/autocomplete/detail?name=' + name);
    }
    await setConfig(I, 'basketRoundPrices', 'false');
    await setConfig(I, 'basketDisplayCurrency', 'eur');
    await setConfig(I, 'basketProductCurrency', 'eur');
    await setConfig(I, 'currencyTagRound', 'false');
    await setConfig(I, 'currencyFormat', '###,##0.00');

    const source = await I.executeScript(() => {
        const rows = productListDataTable.rows().data().toArray();
        return rows.find(row => row.title === 'Ponožky') || rows[0];
    });
    I.assertTrue(source != null, 'The standard basket product directory must contain a source product');
    const sourceDetail = await browserRequest(I, '/admin/rest/web-pages/' + (source.docId || source.id));
    for (const fixture of [
        { suffix: 'down', net: '1.594', vat: '0', display: '1,59', quantity: 3, total: '4,77' },
        { suffix: 'tie', net: '1.595', vat: '0', display: '1,60', quantity: 2, total: '3,20' },
        { suffix: 'vat', net: '1.30', vat: '23', display: '1,60', quantity: 3, total: '4,80' },
        { suffix: 'micro', net: '0.0001', vat: '0' }
    ]) {
        const product = await browserRequest(I, '/admin/rest/web-pages/-1?groupId=' + sourceDetail.groupId);
        Object.assign(product, {
            id: -1, docId: -1, title: marker + '-' + fixture.suffix, navbar: marker + '-' + fixture.suffix,
            groupId: sourceDetail.groupId, tempId: sourceDetail.tempId, available: true,
            data: sourceDetail.data + '\n!INCLUDE(/components/basket/addbasket.jsp)!',
            perex: sourceDetail.perex, perexImage: sourceDetail.perexImage,
            fieldJ: 'eur', fieldK: fixture.net, fieldL: fixture.vat, fieldM: '0', virtualPath: ''
        });
        const result = await browserRequest(I, '/admin/rest/web-pages/editor', { action: 'create', data: { 0: product } });
        I.assertTrue(result.data != null && result.data.length === 1, 'The rounding product must be created');
        products.push({ ...fixture, ...result.data[0], docId: result.data[0].docId || result.data[0].id });
    }
    SL.clearBasket(I);
});

Scenario('Legacy mode retains basket rounding after multiplication', async ({ I }) => {
    I.assertEqual(4, products.length, 'All rounding fixtures must exist');
    const product = products[0];
    await addProduct(I, product, 3);
    I.waitForText('4,78', 10, '.basketSmallPrice');
    I.amOnPage(SL.BASKET);
    I.waitForText('4,78', 10, '.basketListTable');
    SL.clearBasket(I);
});

Scenario('Round two-decimal gross units before quantity and ignore coarse currency rounding', async ({ I }) => {
    I.amOnPage(SL.PRODUCTS_ADMIN);
    await setConfig(I, 'basketRoundPrices', 'true');
    await setConfig(I, 'currencyTagRound', 'true');
    await setConfig(I, 'currencyFormat', '0.00');
    SL.clearBasket(I);

    for (const product of products.filter(product => product.suffix !== 'micro')) {
        I.amOnPage(SL.PRODUCTS);
        const card = locate('.thumbnail').withText(product.title);
        I.waitForVisible(card, 10);
        I.assertEqual(product.display + ' €', await sellingPrice(I, product),
            'The product list must show exactly two decimal places');
        await addProduct(I, product, product.quantity);
    }

    I.waitForText('12,77', 10, '.basketSmallPrice');
    I.amOnPage(SL.BASKET);
    for (const product of products.filter(product => product.suffix !== 'micro')) {
        const row = locate('tr.itemTr').withText(product.title);
        I.waitForVisible(row, 10);
        I.assertEqual(product.display + ' €', normalizePrice(await I.grabTextFrom(row.find('td.basketPrice'))),
            'The basket unit price must match the product list');
        I.assertEqual(product.total + ' €', normalizePrice(await I.grabTextFrom(row.find('td').at(4))),
            'The basket line must equal the displayed unit price multiplied by quantity');
    }
    I.assertEqual('12,77 €', normalizePrice(await I.grabTextFrom('.basketPriceText + .basketPrice')),
        'The basket total must equal the sum of its displayed lines');

    I.click(locate('tr.itemTr').withText(products[0].title).find('.addItem'));
    I.waitForText('14,36', 10, '.basketPriceText + .basketPrice');
    I.refreshPage();
    I.waitForText('14,36', 10, '.basketPriceText + .basketPrice');

    I.amOnPage('/showdoc.do?docid=' + products[0].docId);
    I.waitForText('1,59', 10, '.priceSpan, .priceDiv, .price');
    I.dontSee('1,5900', '.priceSpan, .priceDiv, .price');
});

Scenario('Four-decimal units preserve small prices and settle quantity totals to cents', async ({ I }) => {
    I.amOnPage(SL.PRODUCTS_ADMIN);
    await setConfig(I, 'currencyFormat', '0.0000');
    await setConfig(I, 'basketMaxQty', '10000');
    SL.clearBasket(I);
    const product = products.find(product => product.suffix === 'down');
    const micro = products.find(product => product.suffix === 'micro');
    I.assertEqual('1,5940 €', await sellingPrice(I, product));
    I.assertEqual('0,0001 €', await sellingPrice(I, micro));
    await addProduct(I, product, 3);
    await addProduct(I, micro, 1);

    I.amOnPage(SL.BASKET);
    const productRow = locate('tr.itemTr').withText(product.title);
    const microRow = locate('tr.itemTr').withText(micro.title);
    I.assertEqual('1,5940 €', normalizePrice(await I.grabTextFrom(productRow.find('td.basketPrice'))));
    I.assertEqual('4,7800 €', normalizePrice(await I.grabTextFrom(productRow.find('td').at(4))));
    for (const update of [{ quantity: '100', total: '4,7900' }, { quantity: '155', total: '4,8000' }]) {
        I.refreshPage();
        I.waitForVisible(microRow, 10);
        // Replace the selected value without triggering the minimum clamp on an empty field.
        I.click(microRow.find('input.basketQty'));
        I.pressKey(['CommandOrControl', 'A']);
        I.type(update.quantity);
        I.assertEqual(update.quantity, await I.grabValueFrom(microRow.find('input.basketQty')),
            'The exact requested quantity must be present before submitting the update');
        I.pressKey('Tab');
        I.waitForFunction(() => jQuery.active === 0, 10);
        I.waitForText(update.total, 10, '.basketPriceText + .basketPrice');
    }
    I.assertEqual('0,0001 €', normalizePrice(await I.grabTextFrom(microRow.find('td.basketPrice'))));
    I.assertEqual('0,0200 €', normalizePrice(await I.grabTextFrom(microRow.find('td').at(4))));
    I.refreshPage();
    I.waitForText('4,8000', 10, '.basketPriceText + .basketPrice');

    I.amOnPage('/showdoc.do?docid=' + micro.docId);
    I.waitForText('0,0001', 10, '.priceSpan');
});

Scenario('Low-precision unit policies still display at least two decimals everywhere', async ({ I }) => {
    const product = products.find(product => product.suffix === 'down');
    for (const example of [
        { pattern: '0', scale: 0, unit: '2,00', total: '6,00' },
        { pattern: '0.0', scale: 1, unit: '1,60', total: '4,80' }
    ]) {
        I.amOnPage(SL.PRODUCTS_ADMIN);
        await setConfig(I, 'currencyFormat', example.pattern);
        SL.clearBasket(I);
        I.assertEqual(example.unit + ' €', await sellingPrice(I, product),
            'Unit display must retain two decimals without changing the configured rounding precision');
        await addProduct(I, product, 3);
        I.waitForText(example.total, 10, '.basketSmallPrice');

        I.amOnPage(SL.BASKET);
        const row = locate('tr.itemTr').withText(product.title);
        I.waitForVisible(row, 10);
        I.assertEqual(example.unit + ' €', normalizePrice(await I.grabTextFrom(row.find('td.basketPrice'))));
        I.assertEqual(example.total + ' €', normalizePrice(await I.grabTextFrom(row.find('td').at(4))));
        I.amOnPage('/showdoc.do?docid=' + product.docId);
        I.waitForText(example.unit, 10, '.priceSpan');
    }
});

Scenario('Remove rounding products and clear the test basket', async ({ I }) => {
    SL.clearBasket(I);
    I.amOnPage(SL.PRODUCTS_ADMIN);
    for (const product of products) {
        I.assertTrue(product.title.startsWith(marker), 'Cleanup may only delete products created by this test');
        const result = await browserRequest(I, '/admin/rest/web-pages/editor', {
            action: 'remove', data: { [product.docId]: { id: product.docId, docId: product.docId } }
        });
        I.assertFalse(Boolean(result.error), 'The rounding product must be removed');
    }
});

Scenario('Restore rounding configuration', async ({ I }) => {
    I.amOnPage('/admin/v9/settings/configuration/');
    for (const [name, original] of Object.entries(configuration)) {
        if (original.databaseValuePresent) {
            await setConfig(I, name, original.value);
        } else {
            const current = await browserRequest(I, '/admin/rest/settings/configuration/autocomplete/detail?name=' + name);
            await browserRequest(I, '/admin/rest/settings/configuration/editor', {
                action: 'remove', data: { [current.id]: current }
            });
        }
        if (original.runtimeValueDifferent) {
            await browserRequest(I, '/admin/rest/settings/configuration/editor', {
                action: 'create', data: { 0: { name, value: original.displayValue, temporary: true } }
            });
        }
    }
});

/** Adds a fixture through the existing product UI and waits for each quantity update. */
async function addProduct(I, product, quantity) {
    I.amOnPage(SL.PRODUCTS);
    const button = locate('.thumbnail').withText(product.title).find('.addToBasket');
    I.waitForVisible(button, 10);
    for (let index = 0; index < quantity; index++) {
        I.click(button);
        I.waitForFunction(() => typeof jQuery === 'undefined' || jQuery.active === 0, 10);
    }
}

/** Reads the current price without the crossed-out previous price in the same container. */
async function sellingPrice(I, product) {
    const value = await I.executeScript(title => {
        const productCard = [...document.querySelectorAll('.thumbnail')].find(element => element.textContent.includes(title));
        const price = productCard.querySelector('.price').cloneNode(true);
        price.querySelectorAll('.cenaOld').forEach(element => element.remove());
        return price.textContent;
    }, product.title);
    return normalizePrice(value);
}

/** Sends requests with the same session and CSRF token as the browser. */
async function browserRequest(I, url, body) {
    return I.executeScript(async ({ url, body }) => {
        const response = await fetch(url, {
            method: body == null ? 'GET' : 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: body == null ? undefined : JSON.stringify(body)
        });
        if (!response.ok) throw new Error('The rounding fixture request failed: ' + response.status + ' ' + url);
        const result = await response.json();
        if (result.error || (result.fieldErrors != null && result.fieldErrors.length > 0)) {
            throw new Error('The rounding fixture request was rejected: ' + JSON.stringify(result));
        }
        return result;
    }, { url, body });
}

async function setConfig(I, name, value) {
    await browserRequest(I, '/admin/rest/settings/configuration/editor', {
        action: 'create', data: { 0: { name, value } }
    });
}

function normalizePrice(value) {
    return value.replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
}
