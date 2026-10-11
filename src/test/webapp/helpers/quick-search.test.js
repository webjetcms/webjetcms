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
const emptyPage = { content: [], totalElements: 0, last: true };
const source = request => request.url.searchParams.get('searchType') || (request.url.pathname.includes('/users/') ? 'users' : 'translationKeys');

/** Runs the production search, DOM rendering and request parsing with a deterministic clock. */
function fixture(t, denied = []) {
    const dom = new JSDOM('<!doctype html><button id="trigger">Search</button>', { url: 'http://localhost/admin/v9/' });
    const { window } = dom;
    let now = 0, nextId = 0, clicks = 0;
    const timers = new Map(), requests = [];
    const setTimeout = (fn, delay) => { timers.set(++nextId, { fn, at: now + delay }); return nextId; };
    const clearTimeout = id => timers.delete(id);
    const context = vm.createContext({
        window, document: window.document, URL, URLSearchParams, AbortController, setTimeout, clearTimeout,
        WJ: { translate: key => key, hasPermission: permission => !denied.includes(permission) },
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
    const respond = async (request, data = emptyPage, status = 200) => {
        request.settled = true;
        request.resolve({ ok: status === 200, status, json: async () => data });
        await tick();
    };
    const autocomplete = () => requests.filter(request => request.url.pathname.endsWith('/_doc_autocomplete.jsp'));
    const rest = () => requests.filter(request => !request.url.pathname.endsWith('/_doc_autocomplete.jsp'));
    const activeRest = () => rest().filter(request => !request.settled && !request.signal.aborted);
    return { controller, requests, type, enter, advance, respond, autocomplete, rest, activeRest, clicks: () => clicks };
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
    assert.deepEqual(f.rest().map(source), ['docs']);
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
    assert.equal(f.rest().length, 1);
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
    assert.equal(f.rest().length, 1);
    assert.equal(f.autocomplete().length, 1);
    assert.match(f.controller.results.textContent, /Completed autocomplete result/);
    assert.equal(f.clicks(), 0);
    f.enter({ repeat: true });
    f.enter();
    f.advance(1000);
    assert.equal(f.requests.length, 2, 'Enter must cancel delayed requests and avoid starting another queue.');
    assert.match(f.controller.results.textContent, /Completed autocomplete result/);
});

test('Enter before autocomplete starts flushes REST and preserves the autocomplete delay', t => {
    const f = fixture(t);
    f.type('banner');
    f.advance(100);
    f.enter();
    assert.equal(f.rest().length, 1);
    assert.equal(f.autocomplete().length, 0);
    f.advance(199);
    assert.equal(f.autocomplete().length, 0);
    f.advance(1);
    assert.equal(f.autocomplete().length, 1);
    f.advance(1000);
    assert.equal(f.requests.length, 2);
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
    assert.equal(f.requests.length, 2);
    f.controller.modal.hide();
    assert.ok(f.requests.every(request => request.signal.aborted));
    f.controller.open();
    await f.respond(f.autocomplete()[0], suggestion('Closed dialog result'));
    for (const request of f.rest()) await f.respond(request);
    assert.equal(f.controller.input.value, '');
    assert.equal(f.controller.results.querySelector('.md-quick-search__result'), null);
    f.advance(1000);
    assert.equal(f.requests.length, 2, 'Finishing an aborted request must not advance its queue.');
});

test('REST sources run one at a time in order and repeated Enter cannot start another queue', async t => {
    const f = fixture(t);
    const expected = ['docs', 'files', 'users', 'translationKeys'];
    f.type('banner');
    f.advance(1000);
    for (let index = 0; index < expected.length; index++) {
        assert.deepEqual(f.rest().map(source), expected.slice(0, index + 1));
        assert.deepEqual(f.activeRest().map(source), [expected[index]], 'Only the current REST source may be in flight.');
        f.enter();
        f.enter({ repeat: true });
        f.advance(1000);
        assert.equal(f.rest().length, index + 1, 'Queued sources must wait until the active request finishes.');
        await f.respond(f.rest()[index]);
    }
    f.enter();
    assert.equal(f.rest().length, expected.length, 'Completed sources must not restart on Enter.');
    assert.equal(f.activeRest().length, 0);
    assert.equal(f.autocomplete().length, 1);
});

test('Failed sources continue the queue and retries wait for other REST sources to finish', async t => {
    const f = fixture(t);
    const retry = source => f.controller.results.querySelector(`[data-source="${source}"] .md-quick-search__retry`);
    f.type('banner');
    f.enter();
    f.advance(300);
    await f.respond(f.rest()[0], { error: 'Service unavailable' }, 503);
    assert.deepEqual(f.rest().map(source), ['docs', 'files']);
    assert.deepEqual(f.activeRest().map(source), ['files']);
    assert.equal(retry('docs').getAttribute('aria-disabled'), 'true');
    retry('docs').click();
    assert.equal(f.rest().length, 2, 'Retry must not overlap an active queued request.');
    await f.respond(f.rest()[1], { error: 'Service unavailable' }, 503);
    assert.deepEqual(f.rest().map(source), ['docs', 'files', 'users']);
    await f.respond(f.rest()[2]);
    await f.respond(f.rest()[3]);
    assert.equal(retry('docs').getAttribute('aria-disabled'), 'false');
    assert.equal(retry('files').getAttribute('aria-disabled'), 'false');
    assert.equal(f.autocomplete()[0].settled, undefined, 'Pending autocomplete must not disable REST retries.');
    retry('docs').click();
    assert.deepEqual(f.activeRest().map(source), ['docs']);
    assert.equal(retry('files').getAttribute('aria-disabled'), 'true');
    retry('files').click();
    retry('docs').click();
    assert.equal(f.rest().length, 5, 'Neither retry may overlap the currently running retry.');
    await f.respond(f.rest()[4]);
    assert.equal(retry('docs'), null);
    assert.equal(retry('files').getAttribute('aria-disabled'), 'false');
    retry('files').click();
    assert.deepEqual(f.activeRest().map(source), ['files']);
    await f.respond(f.rest()[5]);
    assert.equal(retry('files'), null);
    assert.equal(f.activeRest().length, 0);
});

test('More results wait for the REST queue and prevent overlapping page requests', async t => {
    const f = fixture(t);
    const more = () => f.controller.results.querySelector('[data-source="docs"] .md-quick-search__more');
    f.controller.scopeBar.querySelector('[data-scope="docs"]').click();
    f.type('banner');
    f.advance(1000);
    await f.respond(f.rest()[0], { content: [{ id: 1, title: 'Initial page result' }], totalElements: 2, last: false });
    assert.deepEqual(f.activeRest().map(source), ['files']);
    assert.equal(more().getAttribute('aria-disabled'), 'true');
    more().click();
    assert.equal(f.rest().length, 2, 'Pagination must not overlap the automatic REST queue.');
    for (let index = 1; index <= 3; index++) await f.respond(f.rest()[index]);
    assert.equal(more().getAttribute('aria-disabled'), 'false');
    assert.equal(f.autocomplete()[0].settled, undefined, 'Pending autocomplete must not disable REST pagination.');
    more().click();
    assert.deepEqual(f.activeRest().map(source), ['docs']);
    assert.equal(f.rest()[4].url.searchParams.get('page'), '1');
    assert.equal(more().getAttribute('aria-disabled'), 'true');
    more().click();
    assert.equal(f.rest().length, 5, 'Pagination must not start another page while the current page loads.');
    await f.respond(f.rest()[4], { content: [{ id: 2, title: 'Next page result' }], totalElements: 2, last: true });
    assert.match(f.controller.results.textContent, /Initial page result/);
    assert.match(f.controller.results.textContent, /Next page result/);
    assert.equal(more(), null);
    assert.equal(f.activeRest().length, 0);
});

test('Denied sources are skipped while the remaining permitted REST sources continue', async t => {
    const cases = [
        { denied: ['edit_text'], expected: ['docs', 'files', 'users'] },
        { denied: ['menuWebpages'], expected: ['users', 'translationKeys'] },
        { denied: ['users_edit_admins', 'users_edit_public_users'], expected: ['docs', 'files', 'translationKeys'] },
        { denied: ['users_edit_admins'], expected: ['docs', 'files', 'users', 'translationKeys'] },
        { denied: ['menuWebpages', 'users_edit_admins', 'users_edit_public_users', 'edit_text'], expected: [] }
    ];
    for (const { denied, expected } of cases) {
        await t.test(`Denied permissions: ${denied.join(', ')}`, async t => {
            const f = fixture(t, denied);
            f.type('banner');
            f.advance(1000);
            for (let index = 0; index < expected.length; index++) {
                assert.deepEqual(f.activeRest().map(source), [expected[index]]);
                await f.respond(f.rest()[index]);
            }
            assert.deepEqual(f.rest().map(source), expected);
            assert.equal(f.activeRest().length, 0);
            assert.equal(f.autocomplete().length, denied.includes('menuWebpages') ? 0 : 1);
            if (denied.includes('edit_text')) assert.equal(f.controller.scopeBar.querySelector('[data-scope="translationKeys"]'), null);
        });
    }
});

test('A late response from an old query neither advances its queue nor unlocks a newer active queue', async t => {
    const f = fixture(t);
    f.type('banner');
    f.advance(1000);
    const oldRest = f.rest()[0];
    const oldAutocomplete = f.autocomplete()[0];
    f.type('news');
    assert.equal(oldRest.signal.aborted, true);
    f.advance(1000);
    assert.equal(f.rest().length, 2);
    await f.respond(oldRest, { content: [{ id: 42, title: 'Stale REST result' }], totalElements: 1, last: true });
    await f.respond(oldAutocomplete, suggestion('Stale autocomplete result'));
    f.enter();
    assert.equal(f.rest().length, 2, 'The old queue must stop without starting or unlocking new sources.');
    assert.deepEqual(f.activeRest().map(source), ['docs']);
    assert.doesNotMatch(f.controller.results.textContent, /Stale/);
    for (let index = 1; index <= 4; index++) await f.respond(f.rest()[index]);
    assert.deepEqual(f.rest().slice(1).map(source), ['docs', 'files', 'users', 'translationKeys']);
    assert.ok(f.rest().slice(1).every(request => request.url.searchParams.get('searchText') === 'news'));
    assert.equal(f.activeRest().length, 0);
});
