const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

const tick = () => new Promise(resolve => setImmediate(resolve));
const readAdmin = file => fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js', file), 'utf8')
    .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');
const suggestion = title => [{ doc_id: 42, title, label: '/banner.html' }];

/** Runs the production search, DOM rendering and request parsing with a deterministic clock. */
function fixture(t) {
    const dom = new JSDOM('<!doctype html><button id="trigger">Search</button>', { url: 'http://localhost/admin/v9/' });
    const { window } = dom;
    let now = 0, nextId = 0, clicks = 0;
    const timers = new Map(), requests = [];
    const setTimeout = (fn, delay) => { timers.set(++nextId, { fn, at: now + delay }); return nextId; };
    const clearTimeout = id => timers.delete(id);
    const context = vm.createContext({
        window, document: window.document, URL, URLSearchParams, AbortController, setTimeout, clearTimeout,
        WJ: { translate: key => key, hasPermission: () => true },
        bootstrap: { Modal: class {
            constructor(root) { this.root = root; }
            show() { this.root.classList.add('show'); this.root.dispatchEvent(new window.Event('shown.bs.modal')); }
            hide() { this.root.dispatchEvent(new window.Event('hide.bs.modal')); this.root.classList.remove('show'); }
        } },
        fetch: (url, options) => new Promise(resolve => requests.push({ url: new URL(url, window.location.origin), signal: options.signal, resolve }))
    });
    vm.runInContext(`{ ${readAdmin('dashboard/widget-utils.js')}; Object.assign(this, { node, icon, localUrl, containNativeScroll, fetchJson }); }`, context);
    vm.runInContext(readAdmin('admin-menu.js'), context);
    vm.runInContext(`${readAdmin('quick-search.js')}\nthis.QuickSearch = QuickSearch;`, context);
    const controller = new context.QuickSearch(window.document.querySelector('#trigger'));
    controller.root.addEventListener('click', event => {
        if (event.target.closest('a')) { clicks++; event.preventDefault(); }
    });
    controller.open();
    t.after(() => { controller.cancel(); window.close(); });
    const type = value => {
        controller.input.value = value;
        controller.input.dispatchEvent(new window.Event('input', { bubbles: true }));
    };
    const enter = (options = {}) => {
        const event = new window.KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true, ...options });
        controller.input.dispatchEvent(event);
        assert.equal(event.defaultPrevented, true, 'Enter in the input must not navigate.');
    };
    const advance = milliseconds => {
        const end = now + milliseconds;
        while (true) {
            const next = [...timers].filter(([, timer]) => timer.at <= end).sort((a, b) => a[1].at - b[1].at)[0];
            if (!next) break;
            const [id, timer] = next;
            now = timer.at;
            timers.delete(id);
            timer.fn();
        }
        now = end;
    };
    const respond = async (request, data) => {
        request.resolve({ ok: true, json: async () => data });
        await tick();
    };
    const autocomplete = () => requests.filter(request => request.url.pathname.endsWith('/_doc_autocomplete.jsp'));
    const rest = () => requests.filter(request => !request.url.pathname.endsWith('/_doc_autocomplete.jsp'));
    return { controller, requests, type, enter, advance, respond, autocomplete, rest, clicks: () => clicks };
}

test('Autocomplete keeps its 300 ms delay while REST waits one second and preserves pending autocomplete', async t => {
    const f = fixture(t);
    f.type('banner');
    f.advance(299);
    assert.equal(f.requests.length, 0);
    f.advance(1);
    assert.equal(f.autocomplete().length, 1);
    assert.equal(f.rest().length, 0);
    f.advance(699);
    assert.equal(f.rest().length, 0);
    f.advance(1);
    assert.equal(f.autocomplete().length, 1, 'Starting REST must not repeat autocomplete.');
    assert.deepEqual(f.rest().map(request => request.url.pathname).sort(), [
        '/admin/rest/search/all', '/admin/rest/search/all', '/admin/rest/users/all', '/admin/v9/settings/translation-keys/all'
    ]);
    assert.equal(f.autocomplete()[0].signal.aborted, false, 'Starting REST must not cancel pending autocomplete.');
    await f.respond(f.autocomplete()[0], suggestion('Pending autocomplete result'));
    assert.match(f.controller.results.textContent, /Pending autocomplete result/);
});

test('Slow typing resets both delays and only the final query reaches REST', async t => {
    const f = fixture(t);
    f.type('ba');
    f.advance(300);
    const stale = f.autocomplete()[0];
    f.type('ban');
    assert.equal(stale.signal.aborted, true);
    f.advance(300);
    f.type('banner');
    f.advance(299);
    assert.equal(f.autocomplete().length, 2);
    f.advance(1);
    assert.equal(f.autocomplete().length, 3);
    f.advance(699);
    assert.equal(f.rest().length, 0, 'Each new keystroke must restart the REST delay.');
    f.advance(1);
    assert.equal(f.rest().length, 4);
    assert.ok(f.rest().every(request => request.url.searchParams.get('searchText') === 'banner'));
    await f.respond(stale, suggestion('Stale autocomplete result'));
    assert.doesNotMatch(f.controller.results.textContent, /Stale autocomplete result/);
});

test('Enter immediately loads REST without navigating, repeating requests or losing completed autocomplete', async t => {
    const f = fixture(t);
    f.type('banner');
    f.advance(300);
    await f.respond(f.autocomplete()[0], suggestion('Completed autocomplete result'));
    f.enter();
    assert.equal(f.rest().length, 4);
    assert.equal(f.autocomplete().length, 1);
    assert.match(f.controller.results.textContent, /Completed autocomplete result/);
    assert.equal(f.clicks(), 0);
    f.enter({ repeat: true });
    f.enter();
    f.advance(1000);
    assert.equal(f.requests.length, 5, 'Enter must cancel delayed requests and avoid repeating active requests.');
    assert.match(f.controller.results.textContent, /Completed autocomplete result/);
});

test('Enter before autocomplete starts flushes REST and preserves the autocomplete delay', t => {
    const f = fixture(t);
    f.type('banner');
    f.advance(100);
    f.enter();
    assert.equal(f.rest().length, 4);
    assert.equal(f.autocomplete().length, 0);
    f.advance(199);
    assert.equal(f.autocomplete().length, 0);
    f.advance(1);
    assert.equal(f.autocomplete().length, 1);
    f.advance(1000);
    assert.equal(f.requests.length, 5);
    assert.equal(f.clicks(), 0);
});

test('Short queries and closing the dialog cancel pending timers', t => {
    const f = fixture(t);
    f.type('b');
    f.enter();
    f.advance(1000);
    assert.equal(f.requests.length, 0);
    f.type('banner');
    f.advance(200);
    f.type('b');
    f.enter();
    f.advance(1000);
    assert.equal(f.requests.length, 0, 'Dropping below the minimum length must cancel both pending searches.');
    f.type('banner');
    f.advance(200);
    f.controller.modal.hide();
    f.advance(1000);
    assert.equal(f.requests.length, 0, 'Closing the dialog must cancel both pending searches.');
});

test('Closing the dialog aborts active sources and ignores late responses after reopening', async t => {
    const f = fixture(t);
    f.type('banner');
    f.advance(1000);
    assert.equal(f.requests.length, 5);
    f.controller.modal.hide();
    assert.ok(f.requests.every(request => request.signal.aborted));
    f.controller.open();
    await f.respond(f.autocomplete()[0], suggestion('Closed dialog result'));
    for (const request of f.rest()) await f.respond(request, { content: [], totalElements: 0, last: true });
    assert.equal(f.controller.input.value, '');
    assert.equal(f.controller.results.querySelector('.md-quick-search__result'), null);
    f.advance(1000);
    assert.equal(f.requests.length, 5);
});
