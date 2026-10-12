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
const pageItems = (source, offset = 0) => Array.from({ length: 10 }, (_, index) => {
    const id = offset + index + 1;
    const title = `${source} result ${id}`;
    if (source === 'files') return { id, title, externalLink: `/files/result-${id}.pdf` };
    if (source === 'users') return { id, firstName: title, login: `user${id}` };
    if (source === 'translationKeys') return { key: title, fieldA: `Translation ${id}` };
    return { id, title };
});
const source = request => request.url.searchParams.has('directory') ? 'documentation'
    : request.url.searchParams.get('searchType') || (request.url.pathname.includes('/users/') ? 'users' : 'translationKeys');
const documentationSearchUrl = 'https://manual.example.com/custom/search?collection=manual';
const deniedRestPermissions = ['menuWebpages', 'users_edit_admins', 'users_edit_public_users', 'edit_text'];
const documentationResult = {
    title: 'Banner guide', url: '/latest/#/en/editor/banner/', sourcePath: '/latest/en/editor/banner/README.md',
    snippet: 'Use **banners** to display announcements.', score: 0.95
};

/** Runs the production search, DOM rendering and request parsing with a deterministic clock. */
function fixture(t, denied = [], options = {}) {
    const dom = new JSDOM('<!doctype html><button id="trigger">Search</button>', { url: 'http://localhost/admin/v9/' });
    const { window } = dom;
    window.adminSearchContext = options.adminSearchContext;
    window.adminHelpContext = { documentationBaseUrl: options.documentationBaseUrl || 'https://docs.webjetcms.sk' };
    window.userLng = options.language;
    window.csrfToken = 'private-admin-csrf-token';
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
        fetch: (url, options) => new Promise(resolve => requests.push({ url: new URL(url, window.location.origin), options, signal: options.signal, resolve }))
    });
    const webjet = readAdmin('webjet.js');
    const markdownStart = webjet.indexOf('    function parseMarkdown(');
    vm.runInContext(`${webjet.slice(markdownStart, webjet.indexOf('    function initTooltip(', markdownStart))}\nWJ.parseMarkdown = parseMarkdown;`, context);
    vm.runInContext(`${readAdmin('documentation-links.js')}\nWJ.getDocumentationUrl = getDocumentationUrl;`, context);
    vm.runInContext(`{ ${readAdmin('dashboard/widget-utils.js')}; Object.assign(this, { node, icon, localUrl, containNativeScroll, fetchJson }); }`, context);
    vm.runInContext(readAdmin('admin-menu.js'), context);
    vm.runInContext(`{ ${readAdmin('documentation-search.js')}; Object.assign(this, { documentationUrl, documentationMarkdown }); }`, context);
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

for (const [platform, modifier] of [['Win32', 'ctrlKey'], ['MacIntel', 'metaKey']]) {
    test(`Quick search ignores keydown events without a key and preserves the shortcut on ${platform}`, t => {
        const { window } = new JSDOM('<!doctype html><button class="js-search-toggler">Search <kbd></kbd></button>');
        t.after(() => window.close());
        const errors = [];
        let opened = 0;
        window.addEventListener('error', event => { errors.push(event.error); event.preventDefault(); });
        const context = vm.createContext({
            window, document: window.document, navigator: { platform }, onOpen: () => opened++
        });
        vm.runInContext(`${readAdmin('quick-search.js')}\nsearchDialog = { open: onOpen }; initQuickSearch();`, context);

        const incomplete = new window.Event('keydown', { bubbles: true, cancelable: true });
        Object.assign(incomplete, { [modifier]: true });
        window.document.body.dispatchEvent(incomplete);
        assert.deepEqual(errors, [], 'Synthetic keydown events without a key must not throw.');
        assert.equal(opened, 0);
        assert.equal(incomplete.defaultPrevented, false);

        for (const key of ['k', 'K']) {
            const shortcut = new window.KeyboardEvent('keydown', { key, [modifier]: true, bubbles: true, cancelable: true });
            window.document.body.dispatchEvent(shortcut);
            assert.equal(shortcut.defaultPrevented, true);
        }
        assert.equal(opened, 2, 'The platform shortcut must still open quick search.');
        assert.deepEqual(errors, []);
    });
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
        assert.equal(f.rest()[index].url.searchParams.get('searchAllGroups'), expected[index] === 'docs' ? 'true' : null,
            'Only full-text page search must request all permitted folders.');
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

test('Scope switches preserve active requests, cache ten rows per source and append ten more on demand', async t => {
    const f = fixture(t);
    const sources = ['docs', 'files', 'users', 'translationKeys'];
    const select = scope => f.controller.scopeBar.querySelector(`[data-scope="${scope}"]`).click();
    const rows = source => f.controller.results.querySelectorAll(`[data-source="${source}"] .md-quick-search__result`);
    f.type('banner');
    f.advance(1000);
    for (let index = 0; index < sources.length; index++) {
        const request = f.rest()[index];
        assert.equal(source(request), sources[index]);
        assert.equal(request.url.searchParams.get('size'), '10');
        assert.equal(request.url.searchParams.get('page'), '0');
        if (index === 0) {
            select('files');
            assert.equal(request.signal.aborted, false, 'Changing scope must preserve the running query.');
            assert.equal(f.rest().length, 1);
        }
        await f.respond(request, { content: pageItems(sources[index]), totalElements: 25, last: false });
    }
    await f.respond(f.autocomplete()[0], []);
    select('all');
    for (const name of sources) assert.equal(rows(name).length, 3, 'All scope must retain the compact three-row preview.');
    for (const name of sources) {
        const before = f.rest().length;
        select(name);
        assert.equal(rows(name).length, 10, 'Selected scope must immediately show all ten cached rows.');
        assert.equal(f.rest().length, before, 'Changing scope must not fetch page zero again.');
        f.controller.results.querySelector('.md-quick-search__more').click();
        const request = f.rest().at(-1);
        assert.equal(source(request), name);
        assert.equal(request.url.searchParams.get('size'), '10');
        assert.equal(request.url.searchParams.get('page'), '1');
        assert.deepEqual(f.activeRest().map(source), [name]);
        const chip = f.controller.scopeBar.querySelector(`[data-scope="${name}"]`);
        assert.equal(chip.getAttribute('aria-busy'), 'true');
        assert.ok(chip.querySelector('.md-quick-search__spinner'));
        assert.equal(chip.querySelector('.md-quick-search__count'), null, 'A spinner must replace the count while another page loads.');
        await f.respond(request, { content: pageItems(name, 10), totalElements: 25, last: false });
        assert.equal(chip.getAttribute('aria-busy'), 'false');
        assert.equal(chip.querySelector('.md-quick-search__spinner'), null);
        assert.equal(chip.querySelector('.md-quick-search__count').textContent, '25');
        assert.equal(rows(name).length, 20);
        select('all');
        assert.equal(rows(name).length, 3);
        select(name);
        assert.equal(rows(name).length, 20, 'Additional loaded pages must survive scope switches.');
        assert.equal(f.rest().length, before + 1);
    }
    f.type('fresh query');
    assert.equal(f.controller.results.querySelector('.md-quick-search__result'), null, 'Changing the query must discard cached rows.');
    f.advance(1000);
    assert.equal(f.rest().at(-1).url.searchParams.get('searchText'), 'fresh query');
});

test('Scope chips expose pending loads and autocomplete without spinning for documentation awaiting Enter', async t => {
    const f = fixture(t, [], { adminSearchContext: { documentationSearchUrl } });
    const busy = (scope, expected) => {
        const chip = f.controller.scopeBar.querySelector(`[data-scope="${scope}"]`);
        assert.equal(chip.getAttribute('aria-busy'), String(expected), `${scope} must expose its loading state.`);
        assert.equal(Boolean(chip.querySelector('.md-quick-search__spinner')), expected);
        if (expected) assert.equal(chip.querySelector('.md-quick-search__count'), null);
    };
    f.type('banner');
    for (const name of ['all', 'docs', 'files', 'users', 'translationKeys']) busy(name, true);
    busy('admin', false);
    busy('documentation', false);
    f.advance(1000);
    for (let index = 0; index < 4; index++) await f.respond(f.rest()[index]);
    busy('docs', true);
    busy('all', true);
    for (const name of ['files', 'users', 'translationKeys', 'documentation']) busy(name, false);
    await f.respond(f.autocomplete()[0], []);
    busy('docs', false);
    busy('all', false);
    f.enter();
    busy('documentation', true);
    busy('all', true);
    f.controller.scopeBar.querySelector('[data-scope="admin"]').click();
    busy('documentation', true);
    await f.respond(f.rest()[4], { results: [] });
    busy('documentation', false);
    busy('all', false);
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

test('Documentation uses the configured endpoint, matching language and directory without admin credentials', async t => {
    for (const [language, expected] of [['sk', 'sk'], ['en', 'en'], ['cz', 'cs'], ['cs', 'cs'], ['de', 'en']]) {
        await t.test(`Administration language: ${language}`, t => {
            const f = fixture(t, deniedRestPermissions, {
                language, adminSearchContext: { documentationSearchUrl: `${documentationSearchUrl}&language=old&directory=/old/&query=old` }
            });
            f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
            f.type('banners & announcements + examples');
            f.advance(1999);
            assert.equal(f.requests.length, 0);
            f.advance(1);
            assert.equal(f.requests.length, 1);
            const request = f.requests[0];
            assert.equal(request.url.origin, 'https://manual.example.com');
            assert.equal(request.url.pathname, '/custom/search');
            assert.equal(request.url.searchParams.get('collection'), 'manual');
            assert.equal(request.url.searchParams.get('query'), 'banners & announcements + examples');
            assert.equal(request.url.searchParams.get('language'), expected);
            assert.equal(request.url.searchParams.get('directory'), `/${expected}/`);
            assert.equal(request.options.credentials, 'omit', 'Documentation must not receive administration cookies.');
            assert.doesNotMatch(JSON.stringify(request.options), /private-admin-csrf-token|csrf|authorization/i);
            assert.equal(request.url.searchParams.has('searchText'), false);
        });
    }
});

test('Confirmed documentation shares the permitted REST queue and stays cached in unrelated scopes', async t => {
    const f = fixture(t, [], { adminSearchContext: { documentationSearchUrl } });
    f.type('banner');
    f.enter();
    const expected = ['docs', 'files', 'users', 'translationKeys', 'documentation'];
    for (let index = 0; index < expected.length; index++) {
        assert.deepEqual(f.activeRest().map(source), [expected[index]]);
        await f.respond(f.rest()[index], expected[index] === 'documentation' ? { results: [] } : emptyPage);
    }
    assert.deepEqual(f.rest().map(source), expected);
    f.controller.scopeBar.querySelector('[data-scope="files"]').click();
    f.enter();
    f.advance(2000);
    assert.deepEqual(f.rest().map(source), expected);
    assert.equal(f.controller.results.querySelector('[data-source="documentation"]'), null);
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    assert.deepEqual(f.rest().map(source), expected, 'Scope switches must not repeat documentation or other sources.');
    assert.equal(f.activeRest().length, 0);
});

test('Automatic documentation requires eight trimmed characters and a two-second pause', t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.type(' 1234567 ');
    f.advance(5000);
    assert.equal(f.requests.length, 0, 'Whitespace must not make a short query eligible for automatic AI search.');
    f.type(' 12345678 ');
    f.advance(1999);
    assert.equal(f.requests.length, 0);
    f.advance(1);
    assert.deepEqual(f.activeRest().map(source), ['documentation']);
    assert.equal(f.rest()[0].url.searchParams.get('query'), '12345678');
});

test('Typing restarts the documentation delay while scope switches preserve its existing timer', t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.type('original query');
    f.advance(1500);
    f.type('updated query');
    f.advance(1500);
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.advance(499);
    assert.equal(f.requests.length, 0);
    f.advance(1);
    assert.equal(f.requests.length, 1);
    assert.equal(f.rest()[0].url.searchParams.get('query'), 'updated query');
});

test('Enter permits a nonempty one-character documentation query without lowering other source limits', async t => {
    for (const scope of ['all', 'documentation']) {
        await t.test(`Scope: ${scope}`, async t => {
            const f = fixture(t, [], { adminSearchContext: { documentationSearchUrl } });
            f.controller.scopeBar.querySelector(`[data-scope="${scope}"]`).click();
            f.type('   ');
            f.enter();
            f.advance(2500);
            assert.equal(f.requests.length, 0, 'Enter must never send an empty query.');
            f.type(' q ');
            f.enter();
            assert.deepEqual(f.rest().map(source), ['documentation']);
            assert.equal(f.rest()[0].url.searchParams.get('query'), 'q');
            f.enter();
            f.enter({ repeat: true });
            f.advance(2500);
            assert.equal(f.requests.length, 1, 'One-character queries must not start autocomplete or other REST sources.');
            await f.respond(f.rest()[0], { results: [], answer: 'A one-character query answer.' });
            assert.match(f.controller.results.querySelector('.md-quick-search__answer').textContent, /one-character query answer/);
            f.controller.scopeBar.querySelector('[data-scope="admin"]').click();
            assert.equal(f.controller.results.querySelector('.md-quick-search__answer'), null, 'Cached documentation must not appear in another selected scope.');
            assert.match(f.controller.results.textContent, /minLength/);
            f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
            assert.match(f.controller.results.querySelector('.md-quick-search__answer').textContent, /one-character query answer/);
            f.enter();
            assert.equal(f.requests.length, 1, 'Repeated Enter must not restart completed documentation requests.');
            assert.equal(f.clicks(), 0);
        });
    }
});

test('Documentation starts at two seconds after the permitted REST queue has already completed', async t => {
    const f = fixture(t, ['edit_text'], { adminSearchContext: { documentationSearchUrl } });
    f.type('banner setup');
    f.advance(1000);
    const expected = ['docs', 'files', 'users'];
    for (let index = 0; index < expected.length; index++) {
        assert.deepEqual(f.activeRest().map(source), [expected[index]]);
        await f.respond(f.rest()[index]);
    }
    assert.equal(f.activeRest().length, 0, 'Documentation must not block or join the first-second queue early.');
    f.advance(999);
    assert.deepEqual(f.rest().map(source), expected);
    f.advance(1);
    assert.deepEqual(f.rest().map(source), [...expected, 'documentation']);
    assert.deepEqual(f.activeRest().map(source), ['documentation']);
    f.enter();
    f.advance(2000);
    assert.equal(f.rest().length, 4);
    assert.equal(f.autocomplete().length, 1);
});

test('Documentation becoming eligible during a slow REST queue waits without overlapping or duplicating requests', async t => {
    const f = fixture(t, [], { adminSearchContext: { documentationSearchUrl } });
    f.type('banner setup');
    f.advance(1000);
    f.advance(1000);
    assert.deepEqual(f.rest().map(source), ['docs']);
    f.enter();
    f.enter({ repeat: true });
    const expected = ['docs', 'files', 'users', 'translationKeys', 'documentation'];
    for (let index = 0; index < expected.length; index++) {
        assert.deepEqual(f.activeRest().map(source), [expected[index]], 'Documentation must use the same serial REST queue.');
        await f.respond(f.rest()[index], expected[index] === 'documentation' ? { results: [] } : emptyPage);
    }
    f.advance(2000);
    f.enter();
    assert.deepEqual(f.rest().map(source), expected);
    assert.equal(f.activeRest().length, 0);
});

test('Enter during a manual retry starts documentation only after that retry finishes', async t => {
    const f = fixture(t, [], { adminSearchContext: { documentationSearchUrl } });
    f.type('banner');
    f.advance(1000);
    await f.respond(f.rest()[0], { error: 'Service unavailable' }, 503);
    for (let index = 1; index <= 3; index++) await f.respond(f.rest()[index]);
    const retry = f.controller.results.querySelector('[data-source="docs"] .md-quick-search__retry');
    assert.equal(retry.getAttribute('aria-disabled'), 'false');
    retry.click();
    assert.deepEqual(f.activeRest().map(source), ['docs']);
    f.enter();
    f.enter();
    assert.equal(f.rest().length, 5, 'Confirming documentation must not overlap the manual retry.');
    await f.respond(f.rest()[4]);
    assert.deepEqual(f.activeRest().map(source), ['documentation']);
    await f.respond(f.rest()[5], { results: [] });
    f.advance(2000);
    assert.equal(f.rest().length, 6);
    assert.equal(f.activeRest().length, 0);
});

test('Shortening the query or closing cancels scheduled documentation', async t => {
    for (const action of ['short query', 'close']) {
        await t.test(action, t => {
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
            f.type('original query');
            f.advance(1999);
            if (action === 'short query') f.type('q');
            else f.controller.modal.hide();
            f.advance(2000);
            assert.equal(f.requests.length, 0, 'The obsolete documentation timer must not send a request.');
        });
    }
});

test('Documentation keeps its active request, error fallback and completed answer across scope switches', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl }, documentationBaseUrl: 'https://manual.example.com' });
    const select = scope => f.controller.scopeBar.querySelector(`[data-scope="${scope}"]`).click();
    f.type('banner setup');
    f.advance(2000);
    const request = f.rest()[0];
    select('admin');
    assert.equal(request.signal.aborted, false);
    select('documentation');
    assert.equal(f.rest().length, 1, 'Returning to documentation must keep its pending request.');
    await f.respond(request, { error: 'Service unavailable' }, 503);
    select('all');
    select('documentation');
    const fallback = f.controller.results.querySelector('[data-source="documentation"] .md-quick-search__documentation-link');
    assert.equal(fallback.href, 'https://manual.example.com/latest/en/', 'The cached error must retain its localized documentation fallback.');
    assert.equal(f.controller.results.querySelector('.md-quick-search__retry'), null);
    assert.equal(f.rest().length, 1, 'Switching scopes must not retry failed AI requests automatically.');
    f.type('new banner setup');
    f.enter();
    await f.respond(f.rest()[1], { results: [documentationResult], answer: 'Cached documentation answer.' });
    select('admin');
    assert.equal(f.controller.results.querySelector('.md-quick-search__answer'), null);
    select('documentation');
    assert.match(f.controller.results.querySelector('.md-quick-search__answer').textContent, /Cached documentation answer/);
    assert.match(f.controller.results.textContent, /Banner guide/);
    f.advance(3000);
    assert.equal(f.rest().length, 2, 'Returning to a completed AI answer must not repeat its request.');
});

test('Introducing documentation from another scope preserves its minimum length and two-second delay', async t => {
    for (const query of ['banner', 'banner setup']) {
        await t.test(query, t => {
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
            f.controller.scopeBar.querySelector('[data-scope="admin"]').click();
            f.type(query);
            f.advance(5000);
            assert.equal(f.requests.length, 0);
            f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
            f.advance(1999);
            assert.equal(f.requests.length, 0);
            f.advance(1);
            if (query.length < 8) {
                assert.equal(f.requests.length, 0);
                f.enter();
            }
            assert.deepEqual(f.rest().map(source), ['documentation']);
            assert.equal(f.rest()[0].url.searchParams.get('query'), query);
        });
    }
});

test('Enter in an unrelated scope does not release cached documentation awaiting confirmation', async t => {
    const f = fixture(t, [], { adminSearchContext: { documentationSearchUrl } });
    f.type('banner');
    f.advance(1000);
    for (let index = 0; index < 4; index++) await f.respond(f.rest()[index]);
    f.controller.scopeBar.querySelector('[data-scope="files"]').click();
    f.enter();
    f.advance(2000);
    assert.equal(f.rest().length, 4);
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.enter();
    assert.deepEqual(f.activeRest().map(source), ['documentation']);
    assert.equal(f.rest().length, 5);
});

test('Documentation uses WebJET Markdown for optional AI answers, chunk lists and code with usable result links', async t => {
    const f = fixture(t, deniedRestPermissions, { language: 'en', adminSearchContext: { documentationSearchUrl } });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner');
    f.enter();
    await f.respond(f.rest()[0], {
        answer: 'Choose **Banner management** to start.',
        results: [{ ...documentationResult, snippet: '## Settings\n\n- First option\n- Second option\n\n| Name | Value |\n| --- | --- |\n| Width | 300 |\n\n```js\nconst width = 300;\n```\n\n[Details](settings.md) [Section](settings.md?id=old#základné-nastavenia) [Query section](settings.md?id=advanced-settings)' }]
    });
    const answer = f.controller.results.querySelector('.md-quick-search__answer');
    assert.equal(answer.querySelector('strong').textContent, 'Banner management');
    const card = f.controller.results.querySelector('.md-quick-search__documentation-result');
    assert.equal(card.querySelectorAll('li').length, 2);
    assert.equal(card.querySelector('table'), null, 'Pipe tables remain readable text in the shared WebJET renderer.');
    assert.match(card.textContent, /\| Name \| Value \|/);
    assert.match(card.textContent, /\| Width \| 300 \|/);
    assert.match(card.querySelector('pre code').textContent, /const width = 300;/);
    const resultLink = card.querySelector('.md-quick-search__result');
    assert.equal(resultLink.href, 'https://manual.example.com/latest/en/editor/banner/');
    assert.equal(resultLink.target, '_blank');
    const detailLink = [...card.querySelectorAll('a')].find(link => link.textContent.startsWith('Details'));
    assert.equal(detailLink.href, 'https://manual.example.com/latest/en/editor/banner/settings');
    assert.equal(detailLink.target, '_blank');
    assert.ok(detailLink.rel.split(' ').includes('noopener'));
    const sectionLink = [...card.querySelectorAll('a')].find(link => link.textContent.startsWith('Section'));
    const sectionUrl = new URL(sectionLink.href);
    assert.equal(sectionUrl.hash, '');
    assert.equal(sectionUrl.pathname, '/latest/en/editor/banner/settings');
    assert.equal(sectionUrl.searchParams.get('id'), 'základné-nastavenia', 'Unicode heading anchors must decode correctly after one pass.');
    const queryLink = [...card.querySelectorAll('a')].find(link => link.textContent.startsWith('Query section'));
    const queryUrl = new URL(queryLink.href);
    assert.equal(queryUrl.hash, '');
    assert.equal(queryUrl.pathname, '/latest/en/editor/banner/settings');
    assert.equal(queryUrl.searchParams.get('id'), 'advanced-settings');
    f.type('another banner');
    f.enter();
    await f.respond(f.rest()[1], { results: [documentationResult] });
    assert.equal(f.controller.results.querySelector('.md-quick-search__answer'), null, 'An omitted answer must not preserve the prior AI response.');
    assert.match(f.controller.results.textContent, /Banner guide/);
});

test('Documentation upgrades versioned hash routes while preserving custom documentation routes', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner');
    f.enter();
    await f.respond(f.rest()[0], { results: ['v202618', 'manual'].map(prefix => ({
        ...documentationResult, url: `/${prefix}/index.html#/en/editor/banner/`, sourcePath: `/${prefix}/en/editor/banner/README.md`,
        snippet: '[Section](settings.md?id=advanced-settings)'
    })) });
    const cards = [...f.controller.results.querySelectorAll('.md-quick-search__documentation-result')];
    assert.equal(cards[0].querySelector('.md-quick-search__result').href, 'https://manual.example.com/v202618/en/editor/banner/');
    assert.equal(cards[0].querySelector('.md-quick-search__markdown a').href, 'https://manual.example.com/v202618/en/editor/banner/settings?id=advanced-settings');
    assert.equal(cards[1].querySelector('.md-quick-search__result').href, 'https://manual.example.com/manual/index.html#/en/editor/banner/');
    assert.equal(cards[1].querySelector('.md-quick-search__markdown a').href, 'https://manual.example.com/manual/#/en/editor/banner/settings?id=advanced-settings');
});

test('Untrusted documentation Markdown cannot create active HTML, images or unsafe destinations', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner');
    f.enter();
    const markdown = '<script>alert(1)</script>\n\n<img src="x" onerror="alert(1)">\n\n'
        + '[Unsafe](javascript:alert(1)) [Foreign](https://foreign.example.com/path) [Protocol relative](//foreign.example.com/path) '
        + '[Safe](/en/guide/) ![Tracker](https://foreign.example.com/tracker.png)';
    await f.respond(f.rest()[0], {
        answer: markdown,
        results: [
            { ...documentationResult, title: '<img src=x onerror=alert(1)>', snippet: markdown },
            { ...documentationResult, title: 'Unsafe result URL', url: 'javascript:alert(1)' },
            { ...documentationResult, title: 'Foreign result URL', url: 'https://foreign.example.com/path' }
        ]
    });
    const section = f.controller.results.querySelector('[data-source="documentation"]');
    assert.equal(section.querySelector('script, img, iframe, object, [onerror], [onclick]'), null);
    assert.match(section.textContent, /<script>alert\(1\)<\/script>/, 'Raw HTML must remain inert text.');
    assert.match(section.textContent, /Foreign/, 'Rejected Markdown links must retain readable link labels.');
    for (const link of section.querySelectorAll('a[href]')) {
        const url = new URL(link.href);
        assert.equal(url.origin, 'https://manual.example.com');
        assert.equal(url.protocol, 'https:');
        assert.equal(link.target, '_blank');
        assert.ok(link.rel.split(' ').includes('noopener'));
    }
    assert.ok([...section.querySelectorAll('a')].some(link => link.href === 'https://manual.example.com/en/guide/'));
});

test('WebJET Markdown output strips injected attributes and entity-obfuscated destinations', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner');
    f.enter();
    const markdown = '[Attribute breakout](/en/guide/" onclick="alert(1)" style="color:red)\n\n'
        + "![Image alt' onerror='alert(1)](/image.png)\n\n"
        + '[Entity script](java&#x73;cript:alert(1)) [Control script](java&#x09;script:alert(1))\n\n'
        + '<svg onload="alert(1)"><a href="javascript:alert(1)">SVG content</a></svg>\n\n'
        + '&lt;svg onload="alert(1)"&gt;Encoded SVG&lt;/svg&gt; &amp; readable entity\n\n'
        + 'Use *emphasis* and `inline code`.\n\n```js\nconst value = "<safe>";\nconst other = 2;\n```';
    await f.respond(f.rest()[0], { answer: markdown, results: [{ ...documentationResult, snippet: markdown }] });
    for (const content of f.controller.results.querySelectorAll('.md-quick-search__markdown')) {
        assert.equal(content.querySelector('img, script, svg, iframe, object, [onclick], [onerror], [onload], [style]'), null);
        assert.match(content.textContent, /Image alt/, 'Suppressed images must retain their alternative text.');
        assert.match(content.textContent, /Encoded SVG/);
        assert.match(content.textContent, /& readable entity/);
        assert.equal(content.querySelector('em').textContent, 'emphasis');
        assert.equal(content.querySelector('code').textContent, 'inline code');
        assert.match(content.querySelector('pre code').textContent, /const value = "<safe>";\nconst other = 2;/);
        assert.equal([...content.querySelectorAll('a')].some(link => /script$/.test(link.textContent)), false,
            'Decoded JavaScript schemes must never become links.');
        for (const link of content.querySelectorAll('a[href]')) {
            assert.equal(new URL(link.href).origin, 'https://manual.example.com');
            assert.ok([...link.attributes].every(attribute => ['href', 'target', 'rel', 'data-focus-key'].includes(attribute.name)),
                'Links may only retain the rebuilt safe navigation attributes.');
        }
    }
});

test('Documentation failures link to the localized configured origin instead of retrying, and new queries can succeed', async t => {
    for (const [label, response, status] of [
        ['HTTP failure', {}, 503], ['Invalid results', { results: {} }, 200], ['Null result', { results: [null] }, 200]
    ]) {
        await t.test(label, async t => {
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl }, documentationBaseUrl: 'https://manual.example.com', language: 'cz' });
            f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
            f.type('banner');
            f.enter();
            await f.respond(f.rest()[0], response, status);
            const fallback = f.controller.results.querySelector('[data-source="documentation"] .md-quick-search__documentation-link');
            assert.equal(fallback.href, 'https://manual.example.com/latest/cs/', 'The fallback must use the documentation language without the endpoint path and query.');
            assert.equal(fallback.target, '_blank');
            assert.ok(fallback.rel.split(' ').includes('noopener'));
            assert.equal(f.controller.results.querySelector('.md-quick-search__retry'), null);
            fallback.click();
            f.enter();
            f.advance(3000);
            assert.equal(f.rest().length, 1, 'The documentation fallback and repeated Enter must not retry the failed request.');
            f.type('another banner');
            f.enter();
            assert.equal(f.rest().length, 2);
            await f.respond(f.rest()[1], { results: [], answer: 'A useful **answer** without source chunks.' });
            assert.equal(f.controller.results.querySelector('.md-quick-search__documentation-link'), null);
            assert.match(f.controller.results.querySelector('.md-quick-search__answer').textContent, /A useful answer/);
            assert.equal(f.controller.results.querySelector('.md-quick-search__empty'), null);
        });
    }
});

test('Malformed and non-HTTP documentation endpoints retain safe localized public navigation without requests', async t => {
    for (const endpoint of ['http://[', 'javascript:alert(1)', 'data:text/html,unsafe']) {
        await t.test(endpoint, t => {
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl: endpoint }, language: 'sk' });
            f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
            f.type('banner');
            f.enter();
            assert.equal(f.requests.length, 0);
            const error = f.controller.results.querySelector('[data-source="documentation"] [role="alert"]');
            assert.ok(error);
            const fallback = error.querySelector('.md-quick-search__documentation-link');
            assert.equal(fallback.href, 'https://docs.webjetcms.sk/latest/sk/');
            assert.equal(fallback.target, '_blank');
            assert.ok(fallback.rel.split(' ').includes('noopener'));
            assert.equal(error.querySelector('button'), null, 'An invalid endpoint must not produce a retry.');
        });
    }
});

test('Long AI answers expand and collapse while retaining keyboard focus on the toggle', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner');
    f.enter();
    await f.respond(f.rest()[0], { results: [], answer: `${'Useful documentation. '.repeat(40)}**Final details.**` });
    const toggle = () => f.controller.results.querySelector('.md-quick-search__answer-toggle');
    assert.equal(toggle().getAttribute('aria-expanded'), 'false');
    assert.doesNotMatch(f.controller.results.textContent, /Final details/);
    toggle().focus();
    toggle().click();
    assert.equal(toggle().getAttribute('aria-expanded'), 'true');
    assert.equal(f.controller.root.ownerDocument.activeElement, toggle());
    assert.equal(f.controller.results.querySelector('.md-quick-search__answer strong').textContent, 'Final details.');
    toggle().click();
    assert.equal(toggle().getAttribute('aria-expanded'), 'false');
    assert.equal(f.controller.root.ownerDocument.activeElement, toggle());
    assert.doesNotMatch(f.controller.results.textContent, /Final details/);
});

test('Keyboard navigation skips documentation titles that have no safe result URL', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner');
    f.enter();
    await f.respond(f.rest()[0], { results: [
        { ...documentationResult, title: 'Unsafe title', url: 'javascript:alert(1)' },
        documentationResult
    ] });
    const { document, KeyboardEvent } = f.controller.root.ownerDocument.defaultView;
    f.controller.input.focus();
    f.controller.input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true, cancelable: true }));
    assert.equal(document.activeElement.tagName, 'A');
    assert.match(document.activeElement.textContent, /Banner guide/);
    document.activeElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true, cancelable: true }));
    assert.equal(document.activeElement, f.controller.input);
});

test('Documentation ignores stale answers after query changes and closing the dialog', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('old query');
    f.enter();
    const oldRequest = f.rest()[0];
    f.type('new query');
    f.enter();
    assert.equal(oldRequest.signal.aborted, true);
    await f.respond(oldRequest, { results: [documentationResult], answer: 'Stale AI answer' });
    assert.doesNotMatch(f.controller.results.textContent, /Stale AI answer|Banner guide/);
    assert.deepEqual(f.activeRest().map(source), ['documentation']);
    f.controller.modal.hide();
    assert.equal(f.rest()[1].signal.aborted, true);
    f.controller.open();
    await f.respond(f.rest()[1], { results: [documentationResult], answer: 'Closed dialog answer' });
    assert.equal(f.controller.input.value, '');
    assert.equal(f.controller.results.querySelector('.md-quick-search__answer'), null);
    assert.equal(f.controller.results.querySelector('.md-quick-search__documentation-result'), null);
});

test('An unconfigured documentation endpoint preserves the external documentation search', t => {
    const f = fixture(t, deniedRestPermissions, { language: 'cz' });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner');
    f.advance(1000);
    assert.equal(f.requests.length, 0);
    const link = f.controller.results.querySelector('[data-source="documentation"] a');
    assert.equal(link.href, 'https://docs.webjetcms.sk/latest/cs/?q=banner');
    assert.equal(link.target, '_blank');
});

test('External documentation search follows the shared configured origin and safely encodes its query', t => {
    const f = fixture(t, deniedRestPermissions, { language: 'sk', documentationBaseUrl: 'http://manual.intranet:8080' });
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    f.type('banner & images');
    f.advance(1000);
    assert.equal(f.requests.length, 0);
    const link = f.controller.results.querySelector('[data-source="documentation"] a');
    assert.equal(link.href, 'http://manual.intranet:8080/latest/sk/?q=banner%20%26%20images');
    assert.equal(link.target, '_blank');
});
