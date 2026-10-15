const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Runs the adapter, WebJET xhr handler and footer renderer with real DataTables and jQuery. */
function fixture(t, serverSide, initialData) {
    const dom = new JSDOM(`<table id="autotest">
        <thead><tr><th>Title</th><th>Amount</th></tr></thead>
        <tfoot><tr><th></th><th></th></tr></tfoot></table>`, { url: 'http://localhost/' });
    t.after(() => dom.window.close());
    const $ = require('jquery')(dom.window);
    require('datatables.net')(dom.window, $);
    const requests = [], notifications = [], events = [], onXhr = [], optionsUpdates = [];
    const serverNotifications = [], statusMessages = [], timers = [];
    $.ajaxSetup({ statusCode: {
        401: () => statusMessages.push(401),
        403: () => statusMessages.push(403)
    } });
    $.ajaxTransport('+*', options => ({
        send(headers, complete) {
            requests.push({
                url: options.url,
                respond(json, status = 200) {
                    complete(status, status === 200 ? 'OK' : 'Service unavailable', {
                        text: typeof json === 'string' ? json : JSON.stringify(json)
                    }, 'Content-Type: application/json');
                }
            });
        },
        abort() {}
    }));
    const context = vm.createContext({
        $, window: dom.window, console,
        setTimeout: (callback, delay) => timers.push({ callback, delay }),
        DATA: {
            id: 'autotest', url: '/admin/rest/autotest', columns: [], serverSide,
            summary: { mode: 'datatable', columns: ['amount'], title: 'Total' },
            onXhr: (table, event, settings, json) => onXhr.push(json),
            initialData, autoPageLength: 10, order: [[0, 'asc']]
        },
        TABLE: undefined, urlSuffix: '/all', ajaxRequestNumber: 0,
        updateOptionsFromJson: json => optionsUpdates.push(json.options),
        showNotify: value => serverNotifications.push(value),
        WJ: { urlAddPath: (url, suffix) => url + suffix, translate: key => key,
            notifyError: (...args) => notifications.push(args) }
    });
    const directory = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables');
    const source = fs.readFileSync(path.join(directory, 'index.js'), 'utf8').replace(/\r/g, '');
    const start = source.indexOf('        function datatable2SpringData(');
    vm.runInContext(source.slice(start, source.indexOf('\n        FooterSum.bindEvents', start)), context);
    const xhrStart = source.indexOf(".on('xhr.dt', ") + ".on('xhr.dt', ".length;
    const xhrEnd = source.indexOf(".on('preXhr.dt'", xhrStart);
    const xhrHandler = vm.runInContext('(' + source.slice(xhrStart, xhrEnd).trim().slice(0, -1) + ')', context);
    vm.runInContext(fs.readFileSync(path.join(directory, 'footer-sum.js'), 'utf8').replace(/^export /gm, ''), context);
    const table = $('#autotest').attr('data-server-side', String(serverSide))
        .on('xhr.dt', xhrHandler)
        .on('xhr.dt draw.dt init.dt', event => events.push(event.type))
        .DataTable({
            serverSide, processing: true, paging: false, info: false, scrollX: true,
            columns: [{ data: 'title', name: 'title' }, { data: 'amount', name: 'amount' }],
            ajax: context.datatable2SpringData,
            footerCallback: () => context.footerCallback(context.TABLE)
        });
    table.getAjaxUrl = () => context.DATA.url;
    table.DATA = context.DATA;
    context.TABLE = table;
    events.length = 0;
    return { table, requests, notifications, events, onXhr, optionsUpdates, serverNotifications, statusMessages, timers,
        loading: () => dom.window.document.querySelector('.dt-processing').style.display !== 'none',
        emptyMessage: () => $(table.table().body()).find('td.dt-empty').text(),
        footer: () => $(table.table().footer()).text().trim() };
}

const products = (title, amount = 100) => ({
    content: [{ title, amount }], totalElements: 1, size: 10,
    options: { title: [{ label: title, value: title }] },
    summary: { amount }, notify: [{ title }]
});
const failures = [
    ['validation error', { error: 'Autotest invalid filter' }, 200, 'Autotest invalid filter'],
    ['access denied', { error: 'Access is denied' }, 200, 'datatables.accessDenied.desc.js'],
    ['HTTP error', {}, 503, 'datatables.error.network.js'],
    ['network error', {}, 0, 'datatables.error.network.js'],
    ['malformed JSON', 'Autotest invalid JSON', 200, 'datatables.error.network.js'],
    ['null response', null, 200, 'datatables.error.network.js']
];

for (const serverSide of [true, false]) {
    for (const [name, response, status, message] of failures) {
        test(`${serverSide ? 'Server' : 'Client'}-side ${name} stops loading without success events and permits recovery`, t => {
            const f = fixture(t, serverSide);
            const { table, requests, notifications, events } = f;
            assert.equal(f.loading(), true);
            requests[0].respond(response, status);
            assert.equal(f.loading(), false, 'The initial failed request must finish processing');
            assert.notEqual(table.settings()[0]._bInitComplete, true, 'Initialization must wait for a successful load');
            assert.equal(table.rows().count(), 0);
            assert.equal(f.emptyMessage(), message, 'The first failure must replace the loading placeholder');
            assert.equal(notifications[0][1], message);
            assert.deepEqual(events, []);
            assert.equal(f.onXhr.length, 0);
            assert.equal(f.optionsUpdates.length, 0);

            table.ajax.reload();
            requests[1].respond(products('Autotest first product'));
            assert.equal(f.loading(), false);
            assert.equal(table.cell(0, 0).data(), 'Autotest first product');
            assert.equal(table.settings()[0]._bInitComplete, true);
            assert.deepEqual(events, ['xhr', 'draw', 'init']);
            assert.equal(f.footer(), 'Total100');
            const previousJson = table.ajax.json();
            events.length = 0;

            if (serverSide) {
                table.column(0).search('autotest').draw();
                assert.match(requests[2].url, /search\/findByColumns/);
            } else {
                table.ajax.reload();
            }
            requests[2].respond(response, status);
            assert.equal(f.loading(), false);
            assert.deepEqual(events, [], 'Errors must not emit xhr, draw or initialization events');
            assert.equal(f.onXhr.length, 1, 'Application success callbacks must not run for failed loads');
            assert.equal(f.optionsUpdates.length, 1);
            assert.equal(f.serverNotifications.length, 1);
            assert.equal(table.cell(0, 0).data(), 'Autotest first product');
            assert.equal(table.page.info().recordsTotal, 1);
            assert.equal(table.ajax.json(), previousJson);
            assert.equal(table.DATA.json, previousJson);
            assert.equal(f.footer(), 'Total100', 'The preserved totals must match the preserved rows');
            assert.equal(notifications.length, 2);

            let reloaded = 0;
            table.ajax.reload(() => reloaded++);
            requests[3].respond(products('Autotest recovered product', 200));
            assert.equal(f.loading(), false);
            assert.equal(table.cell(0, 0).data(), 'Autotest recovered product');
            assert.equal(f.footer(), 'Total200');
            assert.equal(f.onXhr.length, 2);
            assert.equal(reloaded, 1);
            assert.deepEqual(events, ['xhr', 'draw'], 'Recovery must not initialize the table a second time');
        });
    }

    test(`${serverSide ? 'Server' : 'Client'}-side deferred initial data initializes the table normally`, t => {
        const f = fixture(t, serverSide, products('Autotest initial product'));
        f.timers.find(timer => timer.delay === 10).callback();
        assert.equal(f.table.cell(0, 0).data(), 'Autotest initial product');
        assert.equal(f.onXhr.length, 1);
        assert.deepEqual(f.events, ['xhr', 'draw', 'init']);
        assert.equal(f.loading(), false);
    });

    test(`${serverSide ? 'Server' : 'Client'}-side reload callbacks wait for an actual successful draw after an error`, t => {
        const f = fixture(t, serverSide);
        f.requests[0].respond(products('Autotest first product'));
        let callbackJson;
        f.table.ajax.reload(json => { callbackJson = json; });
        f.requests[1].respond({}, 503);
        assert.equal(callbackJson, undefined, 'A failed reload must not invoke its success callback');
        f.table.ajax.reload();
        f.requests[2].respond(products('Autotest recovered product'));
        assert.equal(callbackJson, f.table.ajax.json(), 'DataTables must retain its normal next-draw callback behavior');
        assert.equal(callbackJson.data[0].title, 'Autotest recovered product');
    });

    test(`${serverSide ? 'Server' : 'Client'}-side obsolete initial data and its sort timer cannot affect a later request`, t => {
        const f = fixture(t, serverSide, products('Autotest obsolete initial product'));
        f.table.order([0, 'desc']);
        f.table.ajax.reload();
        for (const timer of f.timers) timer.callback();
        assert.equal(f.requests.length, 1, 'The obsolete initial sort timer must not start another reload');
        assert.equal(f.onXhr.length, 0);
        assert.equal(f.optionsUpdates.length, 0);
        assert.equal(f.loading(), true);
        f.requests[0].respond(products('Autotest current product', 300));
        assert.equal(f.table.cell(0, 0).data(), 'Autotest current product');
        assert.equal(f.footer(), 'Total300');
        assert.equal(f.onXhr.length, 1);
    });
}

for (const currentFirst of [true, false]) {
    for (const failure of [false, true]) {
        test(`Obsolete ${failure ? 'failed' : 'successful'} responses are ignored ${currentFirst ? 'after' : 'before'} the current response`, t => {
            const f = fixture(t, true);
            f.table.ajax.reload();
            if (currentFirst) f.requests[1].respond(products('Autotest current product', 300));
            f.requests[0].respond(failure ? { error: 'Autotest obsolete failure' } : products('Autotest obsolete product'));
            if (!currentFirst) {
                assert.equal(f.loading(), true);
                assert.equal(f.onXhr.length, 0);
                assert.equal(f.optionsUpdates.length, 0);
                f.requests[1].respond(products('Autotest current product', 300));
            }
            assert.equal(f.loading(), false);
            assert.equal(f.notifications.length, 0);
            assert.equal(f.onXhr.length, 1);
            assert.equal(f.optionsUpdates.length, 1);
            assert.equal(f.serverNotifications.length, 1);
            assert.deepEqual(f.events, ['xhr', 'draw', 'init']);
            assert.equal(f.table.cell(0, 0).data(), 'Autotest current product');
            assert.equal(f.table.ajax.json().data[0].title, 'Autotest current product');
            assert.equal(f.table.DATA.json, f.table.ajax.json());
            assert.equal(f.optionsUpdates[0].title[0].label, 'Autotest current product');
            assert.equal(f.footer(), 'Total300');
        });
    }
}

for (const status of [401, 403]) {
    test(`HTTP ${status} retains the global status handler without a duplicate error toast`, t => {
        const f = fixture(t, true);
        f.requests[0].respond({}, status);
        assert.equal(f.loading(), false);
        assert.deepEqual(f.statusMessages, [status]);
        assert.deepEqual(f.notifications, []);
        assert.deepEqual(f.events, []);
    });
}

test('A newer failed filter does not allow an older successful response to replace the last good data', t => {
    const f = fixture(t, true);
    f.requests[0].respond(products('Autotest last good product'));
    const previousJson = f.table.ajax.json();
    f.table.ajax.reload();
    f.table.ajax.reload();
    f.requests[2].respond({}, 503);
    f.requests[1].respond(products('Autotest obsolete product', 900));
    assert.equal(f.table.ajax.json(), previousJson);
    assert.equal(f.table.DATA.json, previousJson);
    assert.equal(f.onXhr.length, 1);
    assert.equal(f.footer(), 'Total100');
    assert.equal(f.loading(), false);
});

test('Replacing an in-flight client request does not report an abort or stop the replacement loader', t => {
    const f = fixture(t, false);
    f.table.ajax.reload();
    assert.equal(f.notifications.length, 0);
    assert.equal(f.loading(), true);
    f.requests[1].respond(products('Autotest replacement product'));
    f.requests[0].respond(products('Autotest cancelled product'));
    assert.equal(f.onXhr.length, 1);
    assert.equal(f.table.ajax.json().data[0].title, 'Autotest replacement product');
    assert.equal(f.loading(), false);
});

test('Destroying a table prevents its pending response from invoking application callbacks', t => {
    const f = fixture(t, true);
    f.table.destroy();
    f.requests[0].respond(products('Autotest destroyed table'));
    assert.equal(f.onXhr.length, 0);
    assert.equal(f.optionsUpdates.length, 0);
    assert.equal(f.notifications.length, 0);
});
