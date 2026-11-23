const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Executes the product page and ChartTools option loading with deferred module and HTTP responses. */
function fixture(t) {
    const dom = new JSDOM('<div class="dt-buttons"></div><table id="productListDataTable"></table>', { url: 'http://localhost/' });
    t.after(() => dom.window.close());
    const window = dom.window;
    const $ = require('jquery')(window);
    $.fn.selectpicker = function () { return this; };
    const requests = new Map();
    const reloads = [];
    const notifications = [];
    const optionLoads = [];
    let tableUrl = '/admin/rest/eshop/product-list';
    const table = {
        showButton() {}, button: () => ({ add() {} }), EDITOR: { on() {} },
        getAjaxUrl: () => tableUrl, setAjaxUrl: value => { tableUrl = value; },
        ajax: { reload: () => reloads.push(tableUrl) }
    };
    $.ajaxTransport('+*', options => ({
        send(headers, complete) {
            requests.set(options.url, (json, status = 200) => complete(status, 'Autotest response',
                { text: JSON.stringify(json) }, 'Content-Type: application/json'));
        },
        abort() {}
    }));
    let resolveModule;
    const modulePromise = new Promise(resolve => { resolveModule = resolve; });
    window.importWebPagesDatatable = () => modulePromise;
    window.domReady = { add: callback => callback() };
    const onload = () => {};
    window.onload = onload;
    const context = vm.createContext({
        window, document: window.document, Option: window.Option, URLSearchParams, $, columns: [], url: tableUrl,
        WJ: window.WJ = {
            DataTable: { mergeColumns() {} }, translate: key => key,
            notifyError: (...args) => notifications.push(args),
            urlUpdateParam(url, name, value) {
                const result = new URL(url, window.location.href);
                result.searchParams.set(name, value);
                return result.pathname + result.search;
            }
        }
    });
    const root = path.resolve(__dirname, '../../../main/webapp');
    const chartSource = fs.readFileSync(path.join(root, 'admin/v9/src/js/libs/chart/chart-tools.js'), 'utf8');
    const start = chartSource.indexOf('export async function setSelect(');
    vm.runInContext(chartSource.slice(start, chartSource.indexOf('\n/**', start)).replace('export ', ''), context);
    context.ChartTools = { setSelect(...args) {
        const promise = context.setSelect(...args);
        optionLoads.push(promise);
        return promise;
    } };
    const pageSource = fs.readFileSync(path.join(root, 'apps/basket/admin/product-list.html'), 'utf8');
    vm.runInContext(pageSource.match(/<script type="text\/javascript">([\s\S]*?)<\/script>/)[1], context);
    return { $, window, onload, requests, reloads, notifications,
        async loadModule() {
            resolveModule({ WebPagesDatatable: class { createDatatable() { return table; } } });
            await modulePromise;
        },
        async settled() { await Promise.allSettled(optionLoads); await Promise.resolve(); }
    };
}

const groupsUrl = '/admin/rest/eshop/product-list/product-groups';
const currenciesUrl = '/admin/rest/eshop/product-list/supported-currencies';
const groups = [{ label: 'Autotest products', value: 123 }, { label: 'Autotest phones', value: 456 }];
const currencies = [{ label: 'USD', value: 'usd' }, { label: 'EUR', value: 'eur' }];

for (const first of ['groups', 'currencies']) {
    test(`Product filters wait for both option lists when ${first} arrive first`, async t => {
        const f = fixture(t);
        assert.equal(f.window.onload, f.onload, 'Initialization must not replace the page load handler');
        assert.equal(f.reloads.length, 0, 'A delayed module must not cause an early reload');
        await f.loadModule();
        assert.equal(f.requests.size, 2);
        const firstUrl = first === 'groups' ? groupsUrl : currenciesUrl;
        f.requests.get(firstUrl)(first === 'groups' ? groups : currencies);
        await Promise.resolve();
        f.$('#groupSelect').trigger('change');
        assert.equal(f.reloads.length, 0, 'One loaded filter must not trigger a request with an empty parameter');
        f.requests.get(first === 'groups' ? currenciesUrl : groupsUrl)(first === 'groups' ? currencies : groups);
        await f.settled();
        assert.deepEqual(f.reloads, ['/admin/rest/eshop/product-list?groupId=123&currency=usd']);
        assert.equal(f.$('#groupSelect').prop('disabled'), false);
        assert.equal(f.$('#currencySelect').prop('disabled'), false);
        f.$('#groupSelect').val('456').trigger('change');
        f.$('#currencySelect').val('eur').trigger('change');
        assert.equal(f.reloads.at(-1), '/admin/rest/eshop/product-list?groupId=456&currency=eur');
        assert.equal(f.notifications.length, 0);
    });
}

test('Empty product groups do not send null filter values', async t => {
    const f = fixture(t);
    await f.loadModule();
    f.requests.get(groupsUrl)([]);
    f.requests.get(currenciesUrl)(currencies);
    await f.settled();
    f.$('#currencySelect').val('eur').trigger('change');
    assert.equal(f.reloads.length, 0);
});

test('Failed option loading reports the error and keeps incomplete filters disabled', async t => {
    const f = fixture(t);
    await f.loadModule();
    f.requests.get(groupsUrl)({}, 503);
    f.requests.get(currenciesUrl)(currencies);
    await f.settled();
    assert.equal(f.reloads.length, 0);
    assert.equal(f.$('#groupSelect').prop('disabled'), true);
    assert.equal(f.$('#currencySelect').prop('disabled'), true);
    assert.deepEqual(f.notifications, [['datatables.error.title.js', 'datatables.error.network.js']]);
});
