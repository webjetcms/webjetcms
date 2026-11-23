const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');
const jquery = require('jquery');
const moment = require('moment/min/moment-with-locales');

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
    const dom = new JSDOM('<!doctype html><button id="trigger">Search</button>', {
        url: 'http://localhost/admin/v9/', runScripts: 'outside-only', pretendToBeVisual: options.aiChat === true
    });
    const { window } = dom;
    window.adminSearchContext = options.adminSearchContext;
    window.adminHelpContext = { documentationBaseUrl: options.documentationBaseUrl || 'https://docs.webjetcms.sk' };
    window.userLng = options.language;
    window.jQuery = options.aiChat ? jquery(window) : element => element;
    window.csrfToken = 'private-admin-csrf-token';
    const downloads = [], blobs = new Map(), revokedUrls = [], notifications = [];
    const adminSettings = new Map(Object.entries(options.adminSettings ?? { aiChatHistoryOpen: 'false' }));
    let nextBlob = 0;
    class ChatURL extends URL {
        static createObjectURL(blob) {
            const url = 'blob:http://localhost/chat-' + ++nextBlob;
            blobs.set(url, blob);
            return url;
        }
        static revokeObjectURL(url) { revokedUrls.push(url); blobs.delete(url); }
    }
    let now = 0, nextId = 0, clicks = 0;
    const timers = new Map(), requests = [], chatRequests = [];
    const setTimeout = (fn, delay) => { timers.set(++nextId, { fn, at: now + delay }); return nextId; };
    const clearTimeout = id => timers.delete(id);
    if (options.aiChat) {
        window.setTimeout = setTimeout;
        window.clearTimeout = clearTimeout;
        window.eval(fs.readFileSync(require.resolve('bootstrap/dist/js/bootstrap.bundle.js'), 'utf8'));
        window.eval(fs.readFileSync(require.resolve('bootstrap-select/dist/js/bootstrap-select.js'), 'utf8'));
        window.document.dispatchEvent(new window.Event('DOMContentLoaded'));
    }
    const context = vm.createContext({
        window, document: window.document, navigator: { platform: options.platform || 'Win32', clipboard: options.clipboard }, URL: ChatURL, URLSearchParams, AbortController, setTimeout, clearTimeout,
        crypto: require('node:crypto').webcrypto,
        ResizeObserver: class { observe() {} disconnect() {} },
        $: window.jQuery,
        moment: value => moment(value).locale(options.language === 'cz' ? 'cs' : options.language || 'sk'),
        AiChatTransport: class {
            execute(assistant, inputData, callbacks) {
                return new Promise(resolve => {
                    this.request = { assistant, inputData, callbacks, resolve, cancellations: 0 };
                    chatRequests.push(this.request);
                });
            }
            cancel() {
                if (!this.request) return;
                this.request.cancellations++;
                this.request.resolve({ stopped: true });
            }
            dispose() {}
        },
        WJ: { translate: (key, params = []) => [key, ...params].join(' '), initTooltip: () => {},
            getAdminSetting: name => adminSettings.get(name),
            setAdminSetting: (name, value) => adminSettings.set(name, value),
            keepSessionShowLogoffMessage: () => notifications.push('signedOut'),
            keepSessionShowTokenMessage: () => notifications.push('csrf'),
            hasPermission: permission => (permission !== 'cmp_ai_button' || options.aiChat === true) && !denied.includes(permission) },
        bootstrap: { Modal: class {
            constructor(root) { this.root = root; }
            show() { this.root.classList.add('show'); this.root.dispatchEvent(new window.Event('shown.bs.modal')); }
            hide() { this.root.dispatchEvent(new window.Event('hide.bs.modal')); this.root.classList.remove('show'); }
        } },
        fetch: (url, options) => new Promise(resolve => requests.push({ url: new URL(url, window.location.origin), options, signal: options.signal, resolve }))
    });
    const webjet = readAdmin('webjet.js');
    const dateStart = webjet.indexOf('    function _formatTime(');
    vm.runInContext(`{ ${webjet.slice(dateStart, webjet.indexOf('    function formatDateTimeSeconds(', dateStart))}\nWJ.formatDateTime = formatDateTime; }`, context);
    window.webjetTranslationService = { translate: key => key.endsWith('.documentationExternalNotice.js') ? `${key} {1}`
        : key.endsWith('.resultCount.js') ? `${key} {1}: {2}` : key };
    const translateStart = webjet.indexOf('    function translate(');
    vm.runInContext(`{ ${webjet.slice(translateStart, webjet.indexOf('    function showHelpWindow(', translateStart))}\nWJ.translate = (key, ...params) => translate(key, params); }`, context);
    const markdownStart = webjet.indexOf('    function parseMarkdown(');
    vm.runInContext(`${webjet.slice(markdownStart, webjet.indexOf('    function initTooltip(', markdownStart))}\nWJ.parseMarkdown = parseMarkdown;`, context);
    vm.runInContext(`${readAdmin('documentation-links.js')}\nWJ.getDocumentationUrl = getDocumentationUrl;`, context);
    vm.runInContext(`{ ${readAdmin('dashboard/widget-utils.js')}; Object.assign(this, { node, icon, localUrl, containNativeScroll, fetchJson }); }`, context);
    vm.runInContext(readAdmin('admin-menu.js'), context);
    vm.runInContext(`{ ${readAdmin('documentation-search.js')}; Object.assign(this, { documentationUrl, documentationMarkdown }); }`, context);
    if (options.aiChat) {
        vm.runInContext(`${readAdmin('select-picker.js')}\nWJ.initSelectPicker = initSelectPicker;`, context);
        const tooltipStart = webjet.indexOf('    function initTooltip(');
        vm.runInContext(`${webjet.slice(tooltipStart, webjet.indexOf('\n    /**', tooltipStart))}\nWJ.initTooltip = initTooltip;`, context);
        vm.runInContext(readAdmin('ai/chat-history-api.js'), context);
        vm.runInContext(readAdmin('ai/chat-tooltips.js'), context);
        vm.runInContext(readAdmin('ai/ai-response-parser.js'), context);
        vm.runInContext(`{ ${readAdmin('ai/html-preview.js')}; Object.assign(this, { HtmlPreview, editorPreviewContext }); }`, context);
        vm.runInContext(`${readAdmin('ai/ai-chat.js')}\nthis.AiChat = AiChat;`, context);
        vm.runInContext(`{ ${readAdmin('ai/ai-chat-host.js')}; Object.assign(this, { AiChatHost }); }`, context);
        if (options.editorChat) {
            vm.runInContext(readAdmin('../../npm_packages/webjetdatatables/ai/ai-execution-result.js'), context);
            vm.runInContext(`${readAdmin('../../npm_packages/webjetdatatables/editor-ai.js')}\nthis.EditorAi = EditorAi;`, context);
        }
    }
    vm.runInContext(`${readAdmin('quick-search.js')}\nthis.QuickSearch = QuickSearch;`, context);
    const controller = new context.QuickSearch(window.document.querySelector('#trigger'));
    controller.root.addEventListener('click', event => {
        const download = event.target.closest('a[download]');
        if (download) downloads.push({ name: download.download, url: download.href });
        if (event.target.closest('a')) { clicks++; event.preventDefault(); }
    });
    controller.open();
    t.after(() => { controller.cancel(); controller.chat?.destroy(); window.close(); });
    const editorChat = (assistant, inputValue = '<p>Existing page</p>', columnOptions = {}, record = {}) => {
        const values = { data: inputValue, id: record.id };
        const editor = Object.assign(Object.create(context.EditorAi.prototype), {
            EDITOR: { get: field => values[field], mode: () => record.mode || 'edit', ids: () => record.ids || [values.id],
                TABLE: record.url ? { DATA: { url: record.url, editorId: 'id' } } : undefined },
            chatConversations: new Map(), chatApplyingFields: new Map(), undoField: null,
            aiUserInterface: { _showLoader() {}, _hideLoader() {}, setCurrentStatus() {} }
        });
        const conversation = editor.getChatConversation(window.document.querySelector('#trigger'),
            { name: 'data', type: 'textarea', ...columnOptions }, { from: 'data', to: 'data', ...assistant });
        conversation.view.mount(window.document.body);
        t.after(() => conversation.view.destroy());
        return Object.assign(conversation, { values, editor });
    };
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
    return { controller, requests, chatRequests, type, enter, advance, respond, autocomplete, rest, activeRest,
        downloads, blobs, revokedUrls, notifications, adminSettings, editorChat, clicks: () => clicks };
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
    const f = fixture(t, [], { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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
                language, adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl: `${documentationSearchUrl}&language=old&directory=/old/&query=old` }
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
    const f = fixture(t, [], { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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

test('Typing restarts the automatic documentation delay and explicit selection starts it immediately', t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
    f.type('original query');
    f.advance(1500);
    f.type('updated query');
    f.advance(1500);
    assert.equal(f.requests.length, 0);
    f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
    assert.equal(f.requests.length, 1);
    f.advance(2000);
    assert.equal(f.requests.length, 1, 'Explicit search must cancel the automatic documentation timer.');
    assert.equal(f.rest()[0].url.searchParams.get('query'), 'updated query');
});

test('Enter permits a nonempty one-character documentation query without lowering other source limits', async t => {
    for (const scope of ['all', 'documentation']) {
        await t.test(`Scope: ${scope}`, async t => {
            const f = fixture(t, [], { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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
    const f = fixture(t, ['edit_text'], { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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
    const f = fixture(t, [], { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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
    const f = fixture(t, [], { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl } });
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
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationAlwaysSearch: true, documentationSearchUrl }, documentationBaseUrl: 'https://manual.example.com' });
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

test('Selecting documentation from another scope immediately searches short and long queries', async t => {
    for (const query of ['q', 'banner', 'banner setup']) {
        await t.test(query, t => {
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
            f.controller.scopeBar.querySelector('[data-scope="admin"]').click();
            f.type(query);
            f.advance(5000);
            assert.equal(f.requests.length, 0);
            f.controller.scopeBar.querySelector('[data-scope="documentation"]').click();
            assert.deepEqual(f.rest().map(source), ['documentation']);
            assert.equal(f.rest()[0].url.searchParams.get('query'), query);
            f.advance(5000);
            assert.equal(f.requests.length, 1);
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
    assert.deepEqual([...card.querySelectorAll('th')].map(cell => cell.textContent), ['Name', 'Value']);
    assert.deepEqual([...card.querySelectorAll('td')].map(cell => cell.textContent), ['Width', '300']);
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

test('An empty documentation endpoint hides its scope and every search action even in automatic mode', async t => {
    for (const endpoint of [undefined, '', '   ']) {
        await t.test(`Endpoint: ${JSON.stringify(endpoint)}`, t => {
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl: endpoint, documentationAlwaysSearch: true } });
            assert.equal(f.controller.scopeBar.querySelector('[data-scope="documentation"]'), null);
            f.type('private intranet information');
            f.enter();
            f.advance(5000);
            assert.equal(f.requests.length, 0);
            assert.equal(f.controller.results.querySelector('[data-source="documentation"]'), null);
            assert.equal(f.controller.results.querySelector('.md-quick-search__documentation-search'), null);
        });
    }
});

test('Default and false modes never send documentation queries from All, including Enter', async t => {
    for (const automatic of [undefined, false]) {
        await t.test(`Automatic: ${automatic}`, t => {
            const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl, documentationAlwaysSearch: automatic } });
            f.type('confidential client information');
            f.advance(5000);
            f.enter();
            assert.equal(f.requests.length, 0);
            const chip = f.controller.chips.get('documentation');
            assert.equal(chip.getAttribute('aria-busy'), 'false');
            assert.equal(chip.querySelector('.md-quick-search__count').textContent, '—');
            assert.match(chip.textContent, /documentationNotSearched/);
            assert.equal(chip.querySelector('.ti-world'), null);
            assert.equal(chip.querySelector('.ti-external-link'), null);
            assert.match(chip.title, /documentationExternalHint/);
            const prompt = f.controller.results.querySelector('[data-source="documentation"]');
            assert.match(prompt.textContent, /documentationExternalSearch/);
            assert.match(prompt.textContent, /manual.example.com/);
            assert.equal(prompt.querySelector('a'), null, 'The prompt must search inside the dialog without navigating.');
            assert.equal(f.controller.results.getAttribute('aria-busy'), 'false');
        });
    }
});

test('The external documentation action searches once and exposes real counts without navigation', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.type('q');
    f.controller.results.querySelector('.md-quick-search__documentation-search').click();
    assert.equal(f.controller.scope, 'documentation');
    assert.deepEqual(f.rest().map(source), ['documentation']);
    assert.equal(f.rest()[0].url.searchParams.get('query'), 'q');
    assert.equal(f.controller.chips.get('documentation').getAttribute('aria-busy'), 'true');
    assert.equal(f.controller.chips.get('documentation').querySelector('.md-quick-search__count'), null);
    f.controller.chips.get('documentation').click();
    f.enter();
    await f.respond(f.rest()[0], { results: [documentationResult] });
    assert.equal(f.controller.chips.get('documentation').querySelector('.md-quick-search__count').textContent, '1');
    assert.match(f.controller.chips.get('documentation').getAttribute('aria-label'), /resultCount.js admin.quickSearch.documentation.js: 1/);
    f.controller.chips.get('all').click();
    f.controller.chips.get('documentation').click();
    assert.equal(f.requests.length, 1, 'Returning to completed results must reuse the query cache.');
    assert.equal(f.clicks(), 0);
});

test('Changed documentation queries await a new explicit action and reset zero and error states', async t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.type('initial query');
    f.controller.chips.get('documentation').click();
    await f.respond(f.rest()[0], { results: [] });
    assert.equal(f.controller.chips.get('documentation').querySelector('.md-quick-search__count').textContent, '0');
    f.type('new confidential query');
    f.advance(5000);
    assert.equal(f.requests.length, 1, 'Selecting documentation once must not authorize later queries.');
    assert.equal(f.controller.chips.get('documentation').querySelector('.md-quick-search__count').textContent, '—');
    assert.ok(f.controller.results.querySelector('.md-quick-search__documentation-search'));
    f.controller.chips.get('documentation').click();
    assert.equal(f.requests.length, 2, 'Clicking the already selected scope must search the new query.');
    await f.respond(f.rest()[1], {}, 503);
    assert.equal(f.controller.chips.get('documentation').querySelector('.md-quick-search__count'), null, 'A failed request must not be displayed as zero results.');
    f.type('another query');
    f.advance(5000);
    assert.equal(f.requests.length, 2);
    f.enter();
    assert.equal(f.requests.length, 3, 'Enter in documentation explicitly authorizes the current query.');
});

test('Keyboard scope navigation waits for Enter instead of silently sending an external query', t => {
    const f = fixture(t, deniedRestPermissions, { adminSearchContext: { documentationSearchUrl } });
    f.type('private query');
    const { KeyboardEvent } = f.controller.root.ownerDocument.defaultView;
    f.controller.input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', altKey: true, bubbles: true, cancelable: true }));
    assert.equal(f.controller.scope, 'documentation');
    f.advance(5000);
    assert.equal(f.requests.length, 0);
    f.enter();
    assert.equal(f.requests.length, 1);
});

for (const [platform, modifier, label, shortcut] of [
    ['Win32', 'ctrlKey', 'Ctrl + Enter', 'Control+Enter'],
    ['Linux x86_64', 'ctrlKey', 'Ctrl + Enter', 'Control+Enter'],
    ['MacIntel', 'metaKey', '⌘ Enter', 'Meta+Enter']
]) {
    test(`Documentation shortcut explicitly searches the current query on ${platform}`, async t => {
        const f = fixture(t, deniedRestPermissions, { platform, adminSearchContext: { documentationSearchUrl } });
        f.enter({ [modifier]: true });
        assert.equal(f.requests.length, 0, 'An empty query must never be sent.');
        f.type(' q ');
        const action = f.controller.results.querySelector('.md-quick-search__documentation-search');
        assert.equal(action.querySelector('kbd').textContent, label);
        assert.equal(action.getAttribute('aria-keyshortcuts'), shortcut);
        f.advance(5000);
        f.enter();
        assert.equal(f.requests.length, 0, 'Plain Enter in All must not authorize an external search.');
        f.enter({ [modifier]: true });
        assert.equal(f.controller.scope, 'documentation');
        assert.equal(f.requests.length, 1);
        assert.equal(f.requests[0].url.searchParams.get('query'), 'q');
        f.enter({ [modifier]: true, repeat: true });
        f.enter({ [modifier]: true });
        await f.respond(f.requests[0], { results: [] });
        f.enter({ [modifier]: true });
        assert.equal(f.requests.length, 1, 'Repeated shortcuts must reuse pending and completed results.');
        f.controller.chips.get('all').click();
        f.type('new query');
        f.advance(5000);
        assert.equal(f.requests.length, 1, 'Typing a new query must require a new explicit action.');
        const { KeyboardEvent } = f.controller.root.ownerDocument.defaultView;
        const event = new KeyboardEvent('keydown', { key: 'Enter', [modifier]: true, bubbles: true, cancelable: true });
        f.controller.chips.get('admin').dispatchEvent(event);
        assert.equal(event.defaultPrevented, true, 'The shortcut must also work when another dialog control has focus.');
        assert.equal(f.requests.length, 2);
        assert.equal(f.requests[1].url.searchParams.get('query'), 'new query');
        assert.equal(f.controller.root.ownerDocument.activeElement, f.controller.input);
        assert.equal(f.clicks(), 0, 'The shortcut must load results in the dialog without navigation.');
    });
}

test('Documentation shortcuts ignore composition and unrelated modifiers', async t => {
    for (const [platform, modifiers] of [
        ['Win32', { metaKey: true }], ['MacIntel', { ctrlKey: true }],
        ['Win32', { ctrlKey: true, altKey: true }], ['MacIntel', { metaKey: true, shiftKey: true }],
        ['Win32', { ctrlKey: true, metaKey: true }], ['Win32', { ctrlKey: true, isComposing: true }]
    ]) {
        await t.test(`${platform}: ${JSON.stringify(modifiers)}`, t => {
            const f = fixture(t, deniedRestPermissions, { platform, adminSearchContext: { documentationSearchUrl } });
            f.type('private query');
            f.controller.chips.get('documentation').click();
            f.type('another private query');
            const { KeyboardEvent } = f.controller.root.ownerDocument.defaultView;
            const event = new KeyboardEvent('keydown', { key: 'Enter', ...modifiers, bubbles: true, cancelable: true });
            f.controller.input.dispatchEvent(event);
            assert.equal(event.defaultPrevented, false);
            assert.equal(f.requests.length, 1, 'Unrelated key combinations must not send the changed query.');
        });
    }
});

test('Documentation shortcuts remain inactive when documentation is disabled', async t => {
    for (const [platform, modifier] of [['Win32', 'ctrlKey'], ['MacIntel', 'metaKey']]) {
        await t.test(platform, t => {
            const f = fixture(t, deniedRestPermissions, { platform, adminSearchContext: { documentationSearchUrl: '' } });
            f.type('private query');
            const { KeyboardEvent } = f.controller.root.ownerDocument.defaultView;
            const event = new KeyboardEvent('keydown', { key: 'Enter', [modifier]: true, bubbles: true, cancelable: true });
            f.controller.input.dispatchEvent(event);
            assert.equal(event.defaultPrevented, false);
            assert.equal(f.controller.scope, 'all');
            assert.equal(f.requests.length, 0);
        });
    }
});

const chatAssistants = [
    { assistantId: 7, action: 'live_chat', provider: 'openai', providerTitle: 'First provider', description: 'First assistant' },
    { assistantId: 8, action: 'live_chat', provider: 'gemini', providerTitle: 'Second provider', description: 'Second assistant' }
];

/** Opens the real chat host and supplies the authorized assistant catalogue. */
async function openChat(f, assistants = chatAssistants) {
    f.controller.chips.get('aiChat').click();
    const request = f.requests.find(request => request.url.pathname === '/admin/rest/ai/assistant/chat-assistants/');
    assert.ok(request, 'The chat tab must load its page-independent assistant catalogue.');
    await f.respond(request, assistants);
    return f.controller.chatPanel.querySelector('.ai-chat-input');
}

function typeChat(input, value) {
    input.value = value;
    input.dispatchEvent(new input.ownerDocument.defaultView.Event('input', { bubbles: true }));
}

function sendChat(input) {
    input.dispatchEvent(new input.ownerDocument.defaultView.KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true }));
}

function selectChatAssistant(host, id) {
    host.select.value = String(id);
    host.select.dispatchEvent(new host.select.ownerDocument.defaultView.Event('change', { bubbles: true }));
}

/** Opens a saved conversation through the real history request and response path. */
async function openSavedChat(f, id, answer) {
    const opening = f.controller.chat.openThread(id);
    await f.respond(f.requests.at(-1), {
        thread: { id, assistantId: 7, canContinue: true, revision: 1 },
        messages: [
            { id: `${id}-question`, role: 'user', text: 'Saved question', status: 'complete' },
            { id: `${id}-answer`, turnId: `${id}-question`, role: 'assistant', text: answer, status: 'complete' }
        ]
    });
    await opening;
}

test('Chat tooltips keep conversation titles literal and disappear when the chat closes', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    const document = input.ownerDocument;
    const { window } = document.defaultView;
    const hover = element => element.dispatchEvent(new window.MouseEvent('mouseover', { bubbles: true }));
    host.toggleHistory(true);
    const title = '**Notes** <img src=x onerror=alert(1)>';
    await f.respond(f.requests.at(-1), { items: [{ id: 'one', title, assistantId: 7 }], hasMore: false });
    const row = host.historyList.querySelector('.ai-chat-host__history-select');
    row.focus();
    f.advance(1000);
    assert.ok([...document.querySelectorAll('.tooltip-inner')].some(tooltip => tooltip.textContent === title));
    assert.equal(document.querySelector('.tooltip-inner img, .tooltip-inner b'), null, 'Conversation titles remain plain text.');

    host.toggleHistory(false);
    f.advance(1000);
    assert.ok([...document.querySelectorAll('.tooltip-inner')].every(tooltip => tooltip.textContent !== title),
        'The hidden conversation must not leave its tooltip over the chat.');

    typeChat(input, 'Draft');
    const send = host.chat.$send[0];
    hover(send);
    f.controller.chips.get('all').click();
    f.advance(1000);
    assert.equal(document.querySelector('.tooltip'), null, 'Closing the chat removes overlays and pending hover timers.');
    f.controller.chips.get('aiChat').click();
    hover(send);
    f.advance(1000);
    assert.ok(document.querySelector('.tooltip'), 'Reopened chat controls still show their help text.');
});

test('Chat history search debounces typing, retains its query across pages and restores all threads when cleared', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    f.controller.root.ownerDocument.defaultView.webjetTranslationService.translate = key => `Translated ${key}`;
    await openChat(f);
    const host = f.controller.chat;
    host.toggleHistory(true);
    const search = host.historyPanel.querySelector('input[type="search"]');
    assert.equal(host.historySearchRow.hidden, true);
    assert.equal(search.ownerDocument.activeElement, host.historySearchToggle);
    host.historySearchToggle.click();
    assert.equal(host.historySearchRow.hidden, false);
    assert.equal(host.historySearchToggle.getAttribute('aria-expanded'), 'true');
    assert.equal(search.ownerDocument.activeElement, search);
    assert.equal(host.historyPanel.querySelector(`label[for="${search.id}"]`).textContent, search.placeholder);
    const historyRequests = () => f.requests.filter(request => request.url.pathname === '/admin/rest/ai/chat/threads');
    await f.respond(historyRequests().at(-1), { items: [{ id: 'old', title: 'Old conversation' }], hasMore: true });
    typeChat(search, 'first');
    f.advance(200);
    typeChat(search, ' otázka & odpoveď? ');
    f.advance(299);
    assert.equal(historyRequests().length, 1);
    assert.equal(host.historyList.querySelector('button'), null, 'Previous rows and pagination must disappear when the query changes.');
    f.advance(1);
    const request = historyRequests().at(-1);
    assert.equal(request.url.searchParams.get('page'), '0');
    assert.equal(request.url.searchParams.get('query'), 'otázka & odpoveď?');
    assert.equal(request.options.headers['X-CSRF-Token'], 'private-admin-csrf-token');
    await f.respond(request, { items: [{ id: 'match', title: 'Matching conversation' }], hasMore: true });
    [...host.historyList.querySelectorAll('button')].find(button => button.textContent.endsWith('.more.js')).click();
    const next = historyRequests().at(-1);
    assert.equal(next.url.searchParams.get('page'), '1');
    assert.equal(next.url.searchParams.get('query'), 'otázka & odpoveď?');
    await f.respond(next, { items: [{ id: 'older', title: 'Older match' }], hasMore: false });
    assert.equal(host.historyList.querySelectorAll('li').length, 2);
    host.historySearchToggle.click();
    assert.equal(search.value, '');
    assert.equal(host.historySearchRow.hidden, true);
    assert.equal(host.historySearchToggle.getAttribute('aria-expanded'), 'false');
    assert.equal(search.ownerDocument.activeElement, host.historySearchToggle);
    f.advance(300);
    const cleared = historyRequests().at(-1);
    assert.equal(cleared.url.searchParams.get('page'), '0');
    assert.equal(cleared.url.searchParams.has('query'), false);
    await f.respond(cleared, { items: [{ id: 'old', title: 'Old conversation' }], hasMore: false });
    assert.match(host.historyList.textContent, /Old conversation/);
    host.historySearchToggle.click();
    assert.equal(search.ownerDocument.activeElement, search, 'Reopening search must return focus to its input.');
    const { KeyboardEvent } = search.ownerDocument.defaultView;
    search.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
    assert.equal(host.historySearchRow.hidden, true);
    assert.equal(host.historyPanel.hidden, false, 'The first Escape closes only history search.');
    assert.equal(search.ownerDocument.activeElement, host.historySearchToggle);
    host.historySearchToggle.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
    assert.equal(host.historyPanel.hidden, true, 'A second Escape closes the history panel.');
    assert.equal(f.chatRequests.length, 0);
});

test('Chat history ignores stale search responses and distinguishes no matches from an empty history', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    f.controller.root.ownerDocument.defaultView.webjetTranslationService.translate = key => `Translated ${key}`;
    await openChat(f);
    const host = f.controller.chat;
    host.toggleHistory(true);
    const initial = f.requests.at(-1);
    host.historySearchToggle.click();
    const search = host.historySearch;
    typeChat(search, 'first');
    assert.equal(initial.signal.aborted, true);
    f.advance(300);
    const first = f.requests.at(-1);
    typeChat(search, 'second');
    assert.equal(first.signal.aborted, true);
    await f.respond(first, { items: [{ id: 'stale', title: 'Stale conversation' }], hasMore: true });
    assert.equal(host.historyList.querySelector('li'), null);
    assert.equal(host.historyList.getAttribute('aria-busy'), 'true');
    f.advance(300);
    await f.respond(f.requests.at(-1), { items: [], hasMore: false });
    assert.match(host.historyList.textContent, /history.noResults.js/);
    await f.respond(initial, { items: [{ id: 'older', title: 'Old unfiltered result' }], hasMore: true });
    assert.match(host.historyList.textContent, /history.noResults.js/);
    assert.equal(host.historyList.querySelector('li'), null);
    typeChat(search, '');
    f.advance(300);
    await f.respond(f.requests.at(-1), { items: [], hasMore: false });
    assert.match(host.historyList.textContent, /history.empty.js/);
    typeChat(search, 'pending');
    const count = f.requests.length;
    host.destroy();
    f.advance(1000);
    assert.equal(f.requests.length, count, 'Destroying the host must cancel a delayed history search.');
});

test('Saved chat attachments can be selected, removed and sent without a text prompt', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    f.controller.chips.get('aiChat').click();
    await f.respond(f.requests[0], [{ ...chatAssistants[0], persistentChat: true }]);
    const chat = f.controller.chat.chat;
    const file = new File(['Private document'], 'notes.txt', { type: 'text/plain' });
    assert.equal(chat.element.querySelector('.btn-ai-chat-attach').disabled, false);
    chat._addAttachments([file]);
    assert.match(chat.$attachments.text(), /notes.txt/);
    assert.equal(chat.$attachments.find('a').length, 0, 'Unsent documents must not open active blob URLs.');
    assert.equal(chat.$send.prop('disabled'), false);
    chat.$attachments.find('button').trigger('click');
    assert.equal(chat.state.attachments.length, 0);
    assert.equal(chat.$send.prop('disabled'), true);
    chat._addAttachments([file]);
    chat.$send.trigger('click');
    assert.equal(f.chatRequests.length, 1);
    assert.equal(f.chatRequests[0].inputData.files[0], file);
    assert.match(f.chatRequests[0].inputData.userPrompt, /attachmentPrompt.js/);
    assert.equal(chat.state.attachments.length, 0);
    assert.equal(chat.state.messages[0].attachments[0].file, file);
    assert.equal(chat.$attachmentButton.prop('disabled'), true);
    f.chatRequests[0].resolve({ stopped: true });
    await tick();
    const retry = chat.send(chat.state.messages.at(-1).userPrompt, chat.state.messages.at(-1));
    assert.equal(f.chatRequests[1].inputData.files[0], file, 'An unconfirmed retry must resend the original attachment.');
    f.chatRequests[1].resolve({ success: true, response: 'Document summary', persisted: true });
    await retry;
});

test('File drops across the chat host attach once to the active conversation', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    const first = host.chat;
    const file = new File(['Document'], 'notes.txt', { type: 'text/plain' });
    const drop = target => {
        for (const type of ['dragover', 'drop']) {
            const event = new target.ownerDocument.defaultView.Event(type, { bubbles: true, cancelable: true });
            Object.defineProperty(event, 'dataTransfer', { value: { types: ['Files'], files: [file] } });
            target.dispatchEvent(event);
            assert.equal(event.defaultPrevented, true);
        }
    };
    for (const target of [host.element, first.$history[0], first.$input[0]]) drop(target);
    assert.equal(first.state.attachments.length, 3);
    host.selectAssistant(String(chatAssistants[1].assistantId));
    drop(host.element);
    assert.equal(host.chat.state.attachments.length, 1);
    assert.equal(host.chat.state.attachments[0].file, file);
    assert.equal(first.state.attachments.length, 3, 'Inactive chats must not receive the drop.');
});

test('Saved chat rejects unsupported and excessive attachments without discarding the draft', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    f.controller.chips.get('aiChat').click();
    await f.respond(f.requests[0], [{ ...chatAssistants[0], persistentChat: true }]);
    const chat = f.controller.chat.chat;
    typeChat(chat.$input[0], 'Keep this draft');
    chat._addAttachments([new File(['test'], 'script.jsp')]);
    assert.equal(chat.state.attachments.length, 0);
    assert.match(chat.state.notice, /attachmentType.js/);
    chat._addAttachments(Array.from({ length: 6 }, () => new File(['test'], 'notes.txt')));
    assert.equal(chat.state.attachments.length, 0);
    assert.match(chat.state.notice, /attachmentLimit.js/);
    assert.equal(chat.state.draft, 'Keep this draft');
    assert.equal(f.chatRequests.length, 0);
});

test('Dialog window controls restore the default size without interrupting a chat or losing its draft', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f);
    const { dialog, root } = f.controller;
    const control = root.querySelector('.md-quick-search__window-control');
    const close = root.querySelector('.md-quick-search__close');
    assert.equal(root.querySelectorAll('.md-quick-search__window-control').length, 1);
    assert.equal(control.getAttribute('aria-label'), 'datatables.modal.maximize.js');
    assert.equal(control.hasAttribute('title'), false, 'The control must use the standard tooltip instead of a native title.');
    assert.ok(control.querySelector('.ti-arrows-maximize'));
    assert.equal(close.getAttribute('aria-label'), 'datatables.modal.close.js');
    assert.equal(close.getAttribute('aria-keyshortcuts'), 'Escape');
    assert.equal(close.hasAttribute('title'), false);
    typeChat(input, 'First question');
    sendChat(input);
    typeChat(input, 'An unsent follow-up');
    const chat = f.controller.chat.chat;
    const pending = chat.state.pending;

    dialog.style.width = '1000px';
    dialog.style.height = '750px';
    control.focus();
    f.advance(1000);
    assert.equal(root.querySelector('.tooltip-inner')?.textContent, 'datatables.modal.maximize.js');
    assert.ok(root.querySelector('.tooltip.wj-tooltip-hoverable'));
    control.click();
    assert.equal(root.classList.contains('md-quick-search--maximized'), true);
    assert.equal(control.getAttribute('aria-label'), 'datatables.modal.minimize.js');
    f.advance(1000);
    assert.equal(root.querySelector('.tooltip-inner')?.textContent, 'datatables.modal.minimize.js');
    assert.ok(control.querySelector('.ti-arrows-minimize'));
    assert.equal(root.ownerDocument.activeElement, control, 'Changing the icon must preserve keyboard focus.');
    assert.equal(dialog.style.width, '');
    assert.equal(dialog.style.height, '');
    assert.equal(chat.state.pending, pending, 'Resizing must preserve the in-flight request.');
    assert.equal(input.value, 'An unsent follow-up');

    f.chatRequests[0].resolve({ success: true, response: 'First reply', conversationId: 'conversation' });
    await tick();
    control.click();
    assert.equal(root.classList.contains('md-quick-search--maximized'), false);
    assert.equal(control.getAttribute('aria-label'), 'datatables.modal.maximize.js');
    f.advance(1000);
    assert.equal(root.querySelector('.tooltip-inner')?.textContent, 'datatables.modal.maximize.js');
    assert.ok(control.querySelector('.ti-arrows-maximize'));
    assert.match(f.controller.chatPanel.textContent, /First reply/);
    assert.equal(input.value, 'An unsent follow-up');
    assert.equal(chat.state.conversationId, 'conversation');

    dialog.style.width = '920px';
    dialog.style.height = '720px';
    control.click();
    assert.equal(dialog.style.width, '');
    assert.equal(dialog.style.height, '');
    assert.equal(root.classList.contains('md-quick-search--maximized'), true);
    control.click();
    assert.equal(root.classList.contains('md-quick-search--maximized'), false);
    assert.equal(f.chatRequests.length, 1);

    f.controller.modal.hide();
    f.advance(1000);
    assert.equal(root.ownerDocument.querySelector('.tooltip'), null, 'Closing the dialog must remove the tooltip.');
    f.controller.open();
    control.focus();
    f.advance(1000);
    assert.equal(root.querySelector('.tooltip-inner')?.textContent, 'datatables.modal.maximize.js');

    control.blur();
    f.advance(1000);
    control.focus();
    f.controller.modal.hide();
    f.advance(1000);
    assert.equal(root.ownerDocument.querySelector('.tooltip'), null, 'A pending focus tooltip must not appear over a closed dialog.');

    f.controller.open();
    close.focus();
    f.advance(1000);
    assert.equal(root.querySelector('.tooltip-inner')?.textContent, 'datatables.modal.close.js');
    close.click();
    f.advance(1000);
    assert.equal(root.classList.contains('show'), false);
    assert.equal(root.ownerDocument.querySelector('.tooltip'), null, 'The close button must dismiss both the dialog and its tooltip.');
});

test('AI Chat requires permission and ordinary searches never load or send a chat', t => {
    const denied = fixture(t, [...deniedRestPermissions, 'cmp_ai_button'], { aiChat: true });
    assert.equal(denied.controller.chips.has('aiChat'), false);
    const allowed = fixture(t, deniedRestPermissions, { aiChat: true });
    assert.ok(allowed.controller.chips.has('aiChat'));
    allowed.type('a private search term');
    allowed.enter();
    allowed.advance(5000);
    assert.equal(allowed.requests.length, 0);
    assert.equal(allowed.chatRequests.length, 0);
});

test('AI Chat sends no webpage context and completed responses expose only Copy', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    f.type('a search term that must not become the chat prompt');
    const input = await openChat(f);
    assert.equal(input.value, '');
    typeChat(input, 'Write a short announcement');
    sendChat(input);
    assert.equal(f.chatRequests.length, 1);
    assert.deepEqual(JSON.parse(JSON.stringify(f.chatRequests[0].inputData)), {
        inputValue: '', inputValueType: 'text', outputValueType: 'text', userPrompt: 'Write a short announcement'
    });
    const reply = '<p>Our announcement</p>';
    f.chatRequests[0].resolve({ success: true, response: reply });
    await tick();
    const message = f.controller.chatPanel.querySelector('.ai-chat-assistant');
    assert.equal(message.querySelector('details code').textContent, reply);
    assert.equal(message.querySelector('details').open, true);
    assert.equal(message.querySelector('.ai-chat-message-text p'), null, 'Raw HTML must remain inert text.');
    const actions = message.querySelectorAll('.ai-chat-message-actions button');
    assert.equal(actions.length, 1);
    assert.match(actions[0].textContent, /chat.copy.js/);
});

test('AI Chat renders streamed Markdown tables and copies the original response while user prompts stay literal', async t => {
    let copied;
    const f = fixture(t, deniedRestPermissions, { aiChat: true, clipboard: { writeText: async value => { copied = value; } } });
    const input = await openChat(f);
    const prompt = '**My question** <img src=x onerror=alert(1)>';
    typeChat(input, prompt);
    sendChat(input);
    const user = f.controller.chatPanel.querySelector('.ai-chat-user .ai-chat-message-text');
    assert.equal(user.textContent, prompt);
    assert.equal(user.querySelector('b, img'), null);
    const request = f.chatRequests[0];
    const header = '## Boxes\n\n| Size | Count |\n| :--- | ---: |';
    request.callbacks.onUpdate(header + '\n| Small |');
    const answer = f.controller.chatPanel.querySelector('.ai-chat-assistant .ai-chat-message-text');
    assert.equal(answer.querySelector('h2').textContent, 'Boxes');
    assert.deepEqual([...answer.querySelectorAll('th')].map(cell => cell.textContent), ['Size', 'Count']);
    const reply = header + '\n| **Small** | 28 |\n| Large | 19 |\n\n- Total: `47`';
    request.callbacks.onUpdate(reply);
    assert.equal(answer.querySelectorAll('tbody tr').length, 2);
    assert.equal(answer.querySelector('td b').textContent, 'Small');
    assert.equal(answer.querySelector('td.text-end').textContent, '28');
    assert.equal(answer.querySelector('li code').textContent, '47');
    request.resolve({ success: true, response: reply });
    await tick();
    f.controller.chatPanel.querySelector('.ai-chat-assistant .ai-chat-message-actions button').click();
    await tick();
    assert.equal(copied, reply);
    f.controller.modal.hide();
    f.controller.open();
    f.controller.chips.get('aiChat').click();
    assert.equal(answer.querySelectorAll('tbody tr').length, 2);
});

test('HTML disclosures preserve explanations, literal source, focus and expanded state while streaming', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f);
    typeChat(input, 'Create a PageBuilder section');
    sendChat(input);
    const request = f.chatRequests[0];
    const prefix = '**Návrh stránky**\n\n```html\n';
    request.callbacks.onUpdate(prefix + '<section');
    const answer = f.controller.chatPanel.querySelector('.ai-chat-assistant .ai-chat-message-text');
    const details = answer.querySelector('details');
    assert.equal(details.open, false);
    assert.equal(answer.querySelector('b').textContent, 'Návrh stránky');
    const summary = details.querySelector('summary');
    summary.click();
    summary.focus();
    const html = '<section><h1>**Literal heading**</h1><img src=x onerror="alert(1)"><script>alert(1)</script></section>';
    const reply = prefix + html + '\n```\n\nHotovo. **Skontrolujte obsah.**';
    request.callbacks.onUpdate(reply);
    assert.equal(answer.querySelector('details'), details, 'Streaming must retain the native disclosure element.');
    assert.equal(details.open, true);
    assert.equal(input.ownerDocument.activeElement, summary);
    assert.equal(details.querySelector('code').textContent, html);
    assert.equal(answer.querySelector('section, img, script'), null);
    assert.equal(answer.querySelectorAll('b')[1].textContent, 'Skontrolujte obsah.');
    request.resolve({ success: true, response: reply });
    await tick();
    summary.click();
    assert.equal(details.open, false);
});

test('AI Chat Markdown strips active content and attributes while preserving safe links and code', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f);
    typeChat(input, 'Format this answer');
    sendChat(input);
    const reply = 'Formatting examples:\n\n<script>alert(1)</script>\n\n<img src=x onerror=alert(1)>\n\n'
        + '[Unsafe](javascript:alert(1)) [Obfuscated](java&#x73;cript:alert(1)) '
        + '[Control](java&#x09;script:alert(1)) [Data](data:text/html,danger)\n\n'
        + '[Safe](https://example.com/guide) [Breakout](https://example.com/" onclick="alert(1)" style="color:red)\n\n'
        + "![Image alt' onerror='alert(1)](https://example.com/image.png)\n\n"
        + '```\nconst value = "<safe>";\n```';
    f.chatRequests[0].resolve({ success: true, response: reply });
    await tick();
    const answer = f.controller.chatPanel.querySelector('.ai-chat-assistant .ai-chat-message-text');
    assert.equal(answer.querySelector('script, img, svg, iframe, object, [onclick], [onerror], [style]'), null);
    assert.match(answer.textContent, /<script>alert\(1\)<\/script>/);
    assert.equal(answer.querySelector('pre code').textContent.trim(), 'const value = "<safe>";');
    const links = [...answer.querySelectorAll('a')];
    assert.equal(links.length, 2);
    for (const link of links) {
        assert.equal(new URL(link.href).protocol, 'https:');
        assert.equal(link.target, '_blank');
        assert.equal(link.rel, 'noopener noreferrer');
    }
});

test('Chat timestamps stay unchanged while streaming and reopening, and a retry dates only the new answer attempt', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f);
    const started = Date.now();
    typeChat(input, 'First question');
    sendChat(input);
    const chat = f.controller.chat.chat;
    const [question, answer] = chat.state.messages;
    const times = () => [...chat.element.querySelectorAll('time')].map(time => time.dateTime);
    for (const message of [question, answer]) assert.ok(message.created >= started && message.created <= Date.now());
    const original = times();
    assert.deepEqual(original, [question, answer].map(message => new Date(message.created).toISOString()));
    f.chatRequests[0].callbacks.onUpdate('Partial answer');
    assert.deepEqual(times(), original);
    f.chatRequests[0].resolve({ stopped: true });
    await tick();
    f.controller.modal.hide();
    f.controller.open();
    f.controller.chips.get('aiChat').click();
    assert.deepEqual(times(), original);

    answer.created = new Date(2020, 0, 1).getTime();
    chat.refresh();
    const retry = chat.send('First question', answer);
    assert.equal(times()[0], original[0], 'Retrying an answer must preserve the question time.');
    assert.ok(answer.created >= started, 'The replacement answer must use its new attempt time.');
    f.chatRequests[1].resolve({ success: true, response: 'Completed answer' });
    await retry;
    assert.equal(times()[1], new Date(answer.created).toISOString());
});

test('Saved chat messages retain their server timestamps and use the administration date format', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, language: 'sk' });
    await openChat(f);
    const questionTime = new Date(2025, 0, 2, 14, 5);
    const answerTime = new Date(2025, 0, 2, 14, 6);
    const opening = f.controller.chat.openThread('saved-thread');
    await f.respond(f.requests.find(request => request.url.pathname.endsWith('/threads/saved-thread')), {
        thread: { id: 'saved-thread', assistantId: 7, canContinue: true, revision: 2 },
        messages: [
            { id: 'question', role: 'user', text: 'Saved question', created: questionTime.getTime() },
            { id: 'answer', role: 'assistant', turnId: 'question', text: '| Box | Count |\n| --- | --- |\n| Small | 28 |', status: 'complete', created: answerTime.toISOString() }
        ]
    });
    await opening;
    const times = [...f.controller.chatPanel.querySelectorAll('time')];
    assert.deepEqual(times.map(time => time.dateTime), [questionTime, answerTime].map(date => date.toISOString()));
    assert.deepEqual(times.map(time => time.textContent), ['02.01.2025 14:05', '02.01.2025 14:06']);
    assert.ok(times.every(time => time.style.display !== 'none'));
    assert.equal(f.controller.chatPanel.querySelector('.ai-chat-assistant table tbody td').textContent, 'Small');
});

test('Chat files collects all turns, searches filenames and downloads assistant attachments through the protected API', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f);
    const host = f.controller.chat;
    const threadId = '15d5965c-cbc8-481a-a18f-82185dd86417';
    const attachment = (id, name) => ({ id, name, size: 12, mediaType: 'text/plain',
        url: `/admin/rest/ai/chat/threads/${threadId}/attachments/${id}` });
    const first = attachment('1.txt', 'notes.txt');
    const generated = attachment('2.txt', 'report.txt');
    const later = attachment('3.txt', 'notes.txt');
    const opening = host.openThread(threadId);
    await f.respond(f.requests.find(request => request.url.pathname.endsWith('/threads/' + threadId)), {
        thread: { id: threadId, assistantId: 7, canContinue: false, unavailableReason: 'expired', revision: 1 },
        messages: [
            { id: 'question', role: 'user', text: 'First question', attachments: [first] },
            { id: 'answer', role: 'assistant', text: 'Export ready', status: 'complete', attachments: [generated] },
            { id: 'follow-up', role: 'user', text: 'Another upload with the same name', attachments: [later, first] }
        ]
    });
    await opening;
    const toggle = host.chat.element.querySelector('.ai-chat-toolbar-end-actions [data-ai-action="files"]');
    toggle.click();
    assert.equal(toggle.getAttribute('aria-controls'), host.filesPanel.id);
    assert.equal(host.chat.element.querySelector('[data-ai-action="files"]'), null, 'The opener must disappear while the files panel is open.');
    assert.equal(host.filesSearch.ownerDocument.activeElement, host.filesSearch);
    assert.equal(host.filesList.querySelectorAll('li').length, 3, 'Distinct same-name uploads remain separate; repeated attachment references appear once.');
    assert.match(host.filesList.textContent, /files.uploaded.js/);
    assert.match(host.filesList.textContent, /files.generated.js/);
    typeChat(host.filesSearch, 'REPORT');
    assert.equal(host.filesList.querySelectorAll('li').length, 1);
    const download = host.filesList.querySelector('button');
    download.focus();
    host.chat._changed();
    assert.equal(host.filesSearch.ownerDocument.activeElement, download, 'Unrelated chat updates must retain the file list and its focus.');
    download.click();
    const request = f.requests.find(request => request.url.pathname === generated.url);
    assert.equal(request.options.headers['X-CSRF-Token'], 'private-admin-csrf-token');
    assert.equal(request.options.credentials, 'same-origin');
    request.resolve(new Response('Generated file', { headers: { 'Content-Type': 'text/plain' } }));
    await tick();
    assert.equal(f.downloads[0].name, 'report.txt');
    assert.equal(await f.blobs.get(f.downloads[0].url).text(), 'Generated file');
    typeChat(host.filesSearch, 'missing');
    assert.match(host.filesList.textContent, /files.noResults.js/);
    typeChat(host.filesSearch, '');
    assert.equal(host.filesList.querySelectorAll('li').length, 3);
    host.selectAssistant('8');
    assert.equal(host.chat.element.querySelector('[data-ai-action="files"]'), null, 'Switching conversations must keep the opener hidden while the panel is open.');
    assert.equal(host.filesList.querySelectorAll('li').length, 0, 'Switching conversations must not expose the previous chat files.');
    assert.match(host.filesList.textContent, /files.empty.js/);
});

test('Chat files excludes unsent drafts, downloads local uploads and updates when an assistant returns a file', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    f.controller.chips.get('aiChat').click();
    await f.respond(f.requests[0], [{ ...chatAssistants[0], persistentChat: true }]);
    const host = f.controller.chat;
    const chat = host.chat;
    host.toggleFiles(true);
    const file = new File(['Local document'], 'notes.txt', { type: 'text/plain' });
    chat._addAttachments([file]);
    assert.equal(host.filesList.querySelector('li'), null, 'A composer attachment is not part of the conversation until sent.');
    chat.$send.trigger('click');
    assert.equal(host.filesList.querySelectorAll('li').length, 1);
    const requestCount = f.requests.length;
    host.filesList.querySelector('button').click();
    await tick();
    assert.equal(f.requests.length, requestCount, 'A local upload download must not request an unsaved server attachment.');
    assert.equal(f.downloads[0].name, 'notes.txt');
    assert.equal(await f.blobs.get(f.downloads[0].url).text(), 'Local document');
    const generated = { id: '2.txt', name: 'answer.txt', mediaType: 'text/plain',
        url: '/admin/rest/ai/chat/threads/15d5965c-cbc8-481a-a18f-82185dd86417/attachments/2.txt' };
    f.chatRequests[0].resolve({ success: true, response: 'File ready', attachments: [generated] });
    await tick();
    assert.equal(host.filesList.querySelectorAll('li').length, 2);
    assert.match(host.filesList.textContent, /answer.txt/);
    assert.match(host.filesList.textContent, /files.generated.js/);
});

test('Narrow chat panels replace each other and Escape closes files without closing the chat', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const labels = new Map([
        ['components.ai_assistants.chat.history.close.js', 'Close'],
        ['components.ai_assistants.chat.files.title.js', 'Chat files']
    ]);
    f.controller.root.ownerDocument.defaultView.webjetTranslationService.translate = key => labels.get(key) || key;
    await openChat(f);
    const host = f.controller.chat;
    Object.defineProperty(host.element, 'clientWidth', { value: 1000, configurable: true });
    host.toggleHistory(true);
    const opener = () => host.chat.element.querySelector('[data-ai-action="files"]');
    opener().click();
    assert.equal(host.historyPanel.hidden, false);
    assert.equal(host.filesPanel.hidden, false);
    assert.equal(opener(), null);
    const close = host.filesPanel.querySelector('button');
    assert.equal(close.getAttribute('aria-label'), 'Close: Chat files');
    close.click();
    assert.equal(host.filesPanel.hidden, true);
    assert.equal(host.filesSearch.ownerDocument.activeElement, opener(), 'Closing from the panel must restore focus to the recreated opener.');
    opener().click();
    assert.equal(host.filesSearch.ownerDocument.activeElement, host.filesSearch);
    Object.defineProperty(host.element, 'clientWidth', { value: 500, configurable: true });
    host.toggleFiles(true);
    assert.equal(host.historyPanel.hidden, true);
    const escape = new host.filesSearch.ownerDocument.defaultView.KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true });
    host.filesSearch.dispatchEvent(escape);
    assert.equal(escape.defaultPrevented, true);
    assert.equal(host.filesPanel.hidden, true);
    assert.ok(opener(), 'Escape must restore the files opener.');
    assert.equal(host.filesSearch.ownerDocument.activeElement.getAttribute('aria-controls'), host.filesPanel.id);
    assert.equal(f.controller.root.classList.contains('show'), true);
    host.toggleFiles(true);
    host.toggleHistory(true);
    assert.equal(host.filesPanel.hidden, true);
    assert.equal(host.historyPanel.hidden, false);
    assert.equal(host.historySearch.ownerDocument.activeElement, host.historySearchToggle);
});

test('Reopened history fetches numbered downloads and image previews with the required CSRF header', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f);
    const threadId = '15d5965c-cbc8-481a-a18f-82185dd86417';
    const attachments = [
        { id: '1.txt', name: 'notes.txt', mediaType: 'text/plain' },
        { id: '2.png', name: 'obrázok.png', mediaType: 'image/png' }
    ].map(attachment => ({ ...attachment, url: `/admin/rest/ai/chat/threads/${threadId}/attachments/${attachment.id}` }));
    const opening = f.controller.chat.openThread(threadId);
    await f.respond(f.requests.find(request => request.url.pathname.endsWith('/threads/' + threadId)), {
        thread: { id: threadId, assistantId: 7, canContinue: false, unavailableReason: 'expired', revision: 1 },
        messages: [{ id: 'question', role: 'user', text: 'Saved attachments', attachments }]
    });
    await opening;
    const buttons = [...f.controller.chatPanel.querySelectorAll('.ai-chat-attachment-download')];
    assert.equal(buttons.length, 2);
    assert.equal(f.controller.chatPanel.querySelector('a[href*="/attachments/"]'), null);
    const preview = f.requests.find(request => request.url.pathname === attachments[1].url);
    assert.equal(preview.options.headers['X-CSRF-Token'], 'private-admin-csrf-token');
    assert.equal(preview.options.credentials, 'same-origin');
    preview.resolve(new Response('image bytes', { headers: { 'Content-Type': 'image/png' } }));
    await tick();
    const imageUrl = f.controller.chatPanel.querySelector('img').getAttribute('src');
    assert.match(imageUrl, /^blob:/);
    buttons[1].click();
    await tick();
    assert.deepEqual(f.downloads[0], { name: 'obrázok.png', url: imageUrl });
    f.controller.chatPanel.querySelector('.ai-chat-attachment-link').click();
    const file = f.requests.find(request => request.url.pathname === attachments[0].url);
    assert.equal(file.options.headers['X-CSRF-Token'], 'private-admin-csrf-token');
    file.resolve(new Response('Saved document bytes', { headers: { 'Content-Type': 'text/plain' } }));
    await tick();
    assert.equal(f.downloads[1].name, 'notes.txt');
    assert.equal(await f.blobs.get(f.downloads[1].url).text(), 'Saved document bytes');
    buttons[0].click();
    await tick();
    assert.equal(f.downloads.length, 3, 'Both the filename and download icon must save the file.');
    assert.equal(f.requests.filter(request => request.url.pathname === attachments[0].url).length, 1);
    assert.equal(f.chatRequests.length, 0, 'Downloads must remain available without sending a new provider request.');
    f.controller.chat.chat.destroy();
    assert.equal(f.blobs.size, 0, 'Destroying the conversation must release its attachment blobs.');
    assert.ok(f.revokedUrls.includes(imageUrl));
});


test('Attachment failures stay in chat without navigating to an access-denied page', async t => {
    for (const status of [401, 403, 404, 200]) {
        await t.test(String(status), async t => {
            const f = fixture(t, deniedRestPermissions, { aiChat: true });
            await openChat(f);
            const attachment = { name: 'notes.txt', mediaType: 'text/plain',
                url: '/admin/rest/ai/chat/threads/15d5965c-cbc8-481a-a18f-82185dd86417/attachments/1.txt' };
            const chat = f.controller.chat.chat;
            chat.state.messages = [{ role: 'user', text: 'File', attachments: [attachment] }];
            chat.refresh();
            const button = chat.element.querySelector('.ai-chat-attachment-download');
            button.click();
            const request = f.requests.find(request => request.url.pathname === attachment.url);
            request.resolve(new Response('<html>Access denied</html>', { status, headers: { 'Content-Type': 'text/html' } }));
            await tick();
            assert.equal(f.downloads.length, 0);
            assert.match(chat.state.notice, /attachmentDownloadFailed/);
            assert.equal(button.disabled, false);
            if (status === 401) assert.deepEqual(f.notifications, ['signedOut']);
            if (status === 403) assert.deepEqual(f.notifications, ['csrf']);
        });
    }
});

test('Chat hides unavailable or invalid dates instead of displaying the current time or an invalid date', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f);
    const chat = f.controller.chat.chat;
    chat.state.messages = [undefined, null, '', 'invalid-date'].map(created => ({ role: 'user', text: 'Older question', created }));
    chat.refresh();
    for (const time of chat.element.querySelectorAll('time')) {
        assert.equal(time.style.display, 'none');
        assert.equal(time.textContent, '');
        assert.equal(time.hasAttribute('datetime'), false);
    }
});

test('AI Chat preserves independent conversations and drafts through tabs and reopening', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f);
    typeChat(input, 'First question');
    sendChat(input);
    f.chatRequests[0].resolve({ success: true, response: 'First reply', conversationId: 'first-conversation' });
    await tick();
    typeChat(input, 'First draft');
    const selector = f.controller.chatPanel.querySelector('select');
    const $selector = selector.ownerDocument.defaultView.jQuery(selector);
    const picker = $selector.data('selectpicker');
    assert.ok(picker, 'The assistant selector must use the shared Bootstrap picker.');
    assert.equal(picker.$menu.find('.bs-searchbox').length, 0, 'The assistant picker must omit its search field.');
    assert.match(picker.$button.text(), /First assistant/);
    assert.ok(selector.closest('.ai-chat-composer-actions'), 'Assistant selection belongs inside the composer.');
    selector.value = '8';
    selector.dispatchEvent(new input.ownerDocument.defaultView.Event('change', { bubbles: true }));
    let current = f.controller.chatPanel.querySelector('.ai-chat-input');
    assert.equal($selector.data('selectpicker'), picker, 'Switching conversations must reuse the picker.');
    assert.match(picker.$button.text(), /Second assistant/);
    assert.equal(current.value, '');
    assert.doesNotMatch(f.controller.chatPanel.textContent, /First reply/);
    typeChat(current, 'Second draft');
    selector.value = '7';
    selector.dispatchEvent(new input.ownerDocument.defaultView.Event('change', { bubbles: true }));
    assert.equal(f.controller.chatPanel.querySelector('.ai-chat-input').value, 'First draft');
    assert.equal(f.controller.chatPanel.querySelector('select'), selector, 'Switching conversations must retain the live selector.');
    assert.match(f.controller.chatPanel.textContent, /First reply/);
    assert.match(picker.$button.text(), /First assistant/);
    $selector.selectpicker('toggle');
    assert.equal(selector.ownerDocument.querySelectorAll('.bs-container').length, 1);
    const escape = new selector.ownerDocument.defaultView.KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true });
    picker.$menuInner[0].dispatchEvent(escape);
    assert.equal(escape.defaultPrevented, true);
    assert.equal(selector.ownerDocument.querySelector('.bs-container'), null, 'Escape must close the picker without closing the chat.');
    assert.equal(f.controller.root.classList.contains('show'), true);
    assert.equal(selector.ownerDocument.activeElement, picker.$button[0]);
    $selector.selectpicker('toggle');
    f.controller.chips.get('admin').click();
    assert.equal($selector.data('selectpicker'), undefined);
    assert.equal(selector.ownerDocument.querySelector('.bs-container'), null, 'Hiding the chat must remove its detached dropdown.');
    f.controller.chips.get('aiChat').click();
    assert.ok($selector.data('selectpicker'));
    assert.equal(f.controller.chatPanel.querySelectorAll('.ai-chat-host__assistant > .bootstrap-select').length, 1);
    assert.equal(f.controller.chatPanel.querySelector('.ai-chat-input').value, 'First draft');
    f.controller.modal.hide();
    f.controller.open();
    f.controller.chips.get('aiChat').click();
    current = f.controller.chatPanel.querySelector('.ai-chat-input');
    assert.equal(current.value, 'First draft');
    assert.match($selector.data('selectpicker').$button.text(), /First assistant/);
    sendChat(current);
    assert.equal(f.chatRequests[1].inputData.conversationId, 'first-conversation');
    assert.equal(f.requests.length, 1, 'Returning to chat must reuse the assistant catalogue.');
});

test('Global chat restores the last selected assistant after reload and keeps it for New chat', async t => {
    const first = fixture(t, deniedRestPermissions, { aiChat: true });
    const assistants = chatAssistants.map(assistant => ({ ...assistant, persistentChat: true }));
    await openChat(first, assistants);
    selectChatAssistant(first.controller.chat, 8);
    first.controller.chat.newChatButton.click();
    assert.equal(first.controller.chat.select.value, '8');

    const reloaded = fixture(t, deniedRestPermissions, { aiChat: true,
        adminSettings: Object.fromEntries(first.adminSettings) });
    await openChat(reloaded, assistants);
    assert.equal(reloaded.controller.chat.select.value, '8', 'Reload must restore the last chosen assistant instead of the first option.');
    reloaded.controller.chat.newChatButton.click();
    assert.equal(reloaded.controller.chat.chat.assistant.assistantId, 8);
    assert.equal(reloaded.chatRequests.length, 0);
});

test('An unavailable saved assistant falls back to the first available global chat assistant', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, adminSettings: { aiChatAssistantId: '999' } });
    await openChat(f);
    assert.equal(f.controller.chat.select.value, '7');
    assert.equal(f.controller.chat.chat.assistant.assistantId, 7);
});

test('Field chat keeps its explicit assistant without overwriting the global assistant preference', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true,
        adminSettings: { aiChatAssistantId: '8', aiChatHistoryOpen: 'false' } });
    await openChat(f);
    const field = f.editorChat(chatAssistants[0], '', {}, { url: '/admin/rest/web-pages', id: 42 });
    assert.equal(field.host.select.value, '7');
    assert.equal(f.adminSettings.get('aiChatAssistantId'), '8');
});

test('Global and field chat hosts share the history preference without sharing assistant catalogues', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true, adminSettings: {} });
    await openChat(f);
    const globalHost = f.controller.chat;
    assert.equal(globalHost.historyOpen, true, 'History opens by default when no preference has been saved.');
    globalHost.toggleHistory(false);
    assert.equal(f.adminSettings.get('aiChatHistoryOpen'), 'false');

    const first = f.editorChat(chatAssistants[0], '', {}, { url: '/admin/rest/web-pages', id: 42 });
    assert.equal(first.host.historyOpen, false, 'Field chat uses the explicit preference from global chat.');
    assert.equal(first.host.select.options.length, 1, 'Field chat offers only its supplied assistants.');
    first.host.toggleHistory(true);
    assert.equal(f.adminSettings.get('aiChatHistoryOpen'), 'true');
    first.host.toggleHistory(false, false);
    assert.equal(f.adminSettings.get('aiChatHistoryOpen'), 'true', 'Automatic closures retain the explicit preference.');

    const second = f.editorChat(chatAssistants[1], '', {}, { url: '/admin/rest/web-pages', id: 43 });
    assert.equal(second.host.historyOpen, true);
    assert.equal(second.host.select.value, String(chatAssistants[1].assistantId));
    assert.equal(f.requests.filter(request => request.url.pathname.endsWith('/chat-assistants/')).length, 1,
        'Opening field chats must never load the global assistant catalogue.');
});

for (const saved of [false, true]) {
    test(`Editor chat lifecycle retains pending work while hidden and cancels it on disposal (saved=${saved})`, async t => {
        const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
        const assistants = chatAssistants.map(item => ({ ...item, from: 'data', to: 'data', persistentChat: true }));
        const conversation = f.editorChat(assistants[0], '', { ai: assistants },
            { url: '/admin/rest/web-pages', id: saved ? 42 : -1 });
        const { view, chat } = conversation;
        const container = view.element.parentElement;
        assert.equal(view.isPending(), false);
        const sending = chat.send('Keep working while hidden');
        const request = f.chatRequests[0];
        assert.equal(view.isPending(), true);
        if (saved) {
            selectChatAssistant(conversation.host, assistants[1].assistantId);
            assert.equal(conversation.chat.isPending(), false);
            assert.equal(view.isPending(), true, 'Pending work includes the previous assistant after switching.');
        }
        view.unmount();
        assert.equal(view.element.isConnected, false);
        assert.equal(view.isPending(), true);
        assert.equal(request.cancellations, 0);
        view.mount(container);
        assert.equal(view.element.isConnected, true);
        assert.equal(view.isPending(), true);
        view.destroy();
        await sending;
        assert.equal(request.cancellations, 1);
        assert.equal(view.isPending(), false);
        assert.equal(view.element.isConnected, false);
    });
}

test('Saved field chats send their record context and reopen only matching history with insertion actions', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
    const conversation = f.editorChat({ ...chatAssistants[0], persistentChat: true }, '<p>Existing page</p>', {},
        { url: '/admin/rest/web-pages/?groupId=1', id: 42 });
    const { chat, host, editor } = conversation;
    const context = { entityType: '/admin/rest/web-pages', entityId: '42', fieldName: 'data' };
    let loading = false;
    editor.aiUserInterface._showLoader = () => { loading = true; };
    editor.aiUserInterface._hideLoader = () => { loading = false; };
    assert.equal(chat.options.persistent, true);
    const sending = chat.send('Improve this page');
    assert.equal(loading, true);
    assert.deepEqual(JSON.parse(JSON.stringify(f.chatRequests[0].inputData.context)), context);
    assert.equal(f.chatRequests[0].inputData.inputValue, '<p>Existing page</p>');
    f.chatRequests[0].resolve({ success: true, persisted: true, response: 'Improved page' });
    await sending;
    assert.equal(loading, false);

    host.toggleHistory(true);
    const history = f.requests.at(-1);
    for (const [key, value] of Object.entries(context)) assert.equal(history.url.searchParams.get(key), value);
    await f.respond(history, { items: [{ id: 'saved-field', title: 'Saved field question' }], hasMore: true });
    host.historySearch.value = 'Earlier question';
    host.searchHistory();
    f.advance(300);
    const search = f.requests.at(-1);
    for (const [key, value] of Object.entries(context)) assert.equal(search.url.searchParams.get(key), value);
    assert.equal(search.url.searchParams.get('query'), 'Earlier question');
    await f.respond(search, { items: [{ id: 'saved-field', title: 'Saved field question' }], hasMore: true });
    host.loadHistory(true);
    assert.equal(f.requests.at(-1).url.searchParams.get('page'), '1');
    for (const [key, value] of Object.entries(context)) assert.equal(f.requests.at(-1).url.searchParams.get(key), value);
    await f.respond(f.requests.at(-1), { items: [], hasMore: false });

    const opening = host.openThread('saved-field');
    await f.respond(f.requests.at(-1), { thread: { id: 'saved-field', assistantId: 7, revision: 2, canContinue: true, context },
        messages: [{ id: 'answer', role: 'assistant', text: 'Saved answer', status: 'complete' }] });
    await opening;
    assert.equal(conversation.chat.state.threadId, 'saved-field');
    const inserted = [];
    editor.insertChatAnswer = (target, message, mode) => inserted.push({ target, text: message.text, mode });
    conversation.chat.element.querySelector('[data-ai-action="replace"]').click();
    assert.equal(inserted[0].target, conversation);
    assert.equal(inserted[0].text, 'Saved answer');
    assert.equal(inserted[0].mode, 'replace');

    const foreign = host.openThread('another-record');
    await f.respond(f.requests.at(-1), { thread: { id: 'another-record', assistantId: 7, context: { ...context, entityId: '43' } }, messages: [] });
    await foreign;
    assert.equal(conversation.chat.state.threadId, 'saved-field');
    assert.match(host.historyError, /unavailable/);
});

for (const pageBuilder of [false, true]) {
    for (const mode of ['append', 'replace']) {
        test(`Chat ${mode} inserts a reply and undo restores the original content (PageBuilder=${pageBuilder})`, async t => {
            const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
            const conversation = f.editorChat({ ...chatAssistants[0], persistentChat: true }, undefined, {},
                pageBuilder ? { url: '/admin/rest/web-pages/', id: 42 } : {});
            const { editor, values, chat } = conversation;
            const original = values.data;
            const wjeditor = pageBuilder ? {
                editingMode: 'pageBuilder', options: { fieldid: 'data' }, getData: () => values.data,
                pbInsertContent: (content, action) => { values.data = (action === 'append' ? values.data : '') + content; }
            } : null;
            editor.EDITOR.field = () => ({ s: { opts: { wjeditor } } });
            editor.EDITOR.set = (field, value) => { values[field] = value; };
            const message = { role: 'assistant', status: 'complete', text: '```html\n<p>Answer</p>\n```' };
            chat.state.messages.push(message);
            await editor.insertChatAnswer(conversation, message, mode);
            assert.equal(values.data, (mode === 'append' ? original : '') + '<p>Answer</p>');
            assert.ok(conversation.undo);
            assert.ok(chat.element.querySelector('[data-ai-action="undo"]'));
            await editor.undoChatInsertion(conversation);
            assert.equal(values.data, original);
            assert.equal(conversation.undo, null);
            assert.equal(chat.element.querySelector('[data-ai-action="undo"]'), null);
        });
    }
}

test('Ordinary PageBuilder generation still edits each region and can undo all regions', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
    const { editor } = f.editorChat(chatAssistants[0]);
    const regions = ['First region', 'Second region'].map(value => ({
        value, getData() { return this.value; }, setData(value) { this.value = value; }, getSelection: () => null
    }));
    editor.EDITOR.field = () => ({ s: { opts: {
        wjeditor: { editingMode: 'pageBuilder', getWysiwygEditors: () => regions }
    } } });
    editor._executeSingleAction = async (button, column, assistant, input, reuse, update) => {
        update(input.inputValue + ' edited', true);
        return { success: true, totalTokens: 3 };
    };
    await editor._executeAction(null, { name: 'data', type: 'wysiwyg' }, {
        ...chatAssistants[0], action: 'generate_text', from: 'data', to: 'data'
    });
    assert.deepEqual(regions.map(region => region.value), ['First region edited', 'Second region edited']);
    editor.undo();
    assert.deepEqual(regions.map(region => region.value), ['First region', 'Second region']);
});

test('Editor chat identity separates records and fields while retaining each assistant in the same field host', t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
    const assistants = chatAssistants.map(item => ({ ...item, from: 'data', to: 'data', persistentChat: true }));
    const column = { name: 'data', type: 'textarea', ai: assistants };
    const first = f.editorChat(assistants[0], '', column, { url: '/admin/rest/web-pages', id: 42 });
    const { editor, values } = first;
    const initialChat = first.chat;
    initialChat.state.draft = 'Unsent draft';
    initialChat.refresh();
    const revision = first.revision;
    first.undo = { value: 'Old content' };
    const secondAssistant = editor.getChatConversation(null, column, assistants[1]);
    assert.equal(secondAssistant, first);
    assert.notEqual(secondAssistant.chat, initialChat);
    assert.ok(first.revision > revision);
    assert.equal(first.undo, null);
    assert.equal(secondAssistant.host.mounted, false);
    assert.equal(secondAssistant.chat.mounted, false);
    secondAssistant.host.mount(secondAssistant.host.element.ownerDocument.body);
    const restored = editor.getChatConversation(null, column, assistants[0]);
    assert.equal(restored.chat, initialChat);
    assert.equal(restored.chat.state.draft, 'Unsent draft');
    values.id = 43;
    const otherRecord = editor.getChatConversation(null, column, assistants[0]);
    assert.notEqual(otherRecord.host, first.host);
    assert.equal(otherRecord.host.options.context.entityId, '43');
    t.after(() => otherRecord.host.destroy());
    const otherField = editor.getChatConversation(null, { name: 'title', type: 'text' }, { ...assistants[0], to: 'title' });
    assert.notEqual(otherField.host, otherRecord.host);
    assert.equal(otherField.host.options.context.fieldName, 'title');
    t.after(() => otherField.host.destroy());
});

test('Unsaved and multi-record editors never fall back to standalone saved history', t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
    for (const record of [{ id: -1 }, { id: 0 }, { id: 42, mode: 'create' }, { id: 42, ids: [42, 43] }]) {
        const conversation = f.editorChat({ ...chatAssistants[0], persistentChat: true }, '', {},
            { url: '/admin/rest/web-pages', ...record });
        assert.equal(conversation.host, undefined);
        assert.equal(conversation.chat.options.persistent, undefined);
    }
    assert.equal(f.requests.length, 0);
});

for (const record of [
    { label: 'saved record', id: 42 },
    { label: 'new record', id: -1, mode: 'create' },
    { label: 'multiple records', id: 42, ids: [42, 43] }
]) {
    for (const scenario of [
        { label: 'empty source', from: '', expected: '' },
        { label: 'null source', from: null, expected: '' },
        { label: 'missing source', from: undefined, expected: '' },
        { label: 'configured source', from: 'title', expected: 'Source title' },
        { label: 'selection without a source', from: '', selection: true, expected: '<p>Selected paragraph</p>' },
        { label: 'selection overrides the source', from: 'title', selection: true, expected: '<p>Selected paragraph</p>' },
        { label: 'stale selection without a source', from: '', selection: true, stale: true, expected: '' },
        { label: 'stale selection with a source', from: 'title', selection: true, stale: true, expected: 'Source title' }
    ]) {
        test(`Editor chat sends the intended content for ${record.label}: ${scenario.label}`, async t => {
            const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
            const target = '<p>Unselected introduction</p><p>Selected paragraph</p><p>Unselected ending</p>';
            const conversation = f.editorChat({ ...chatAssistants[0], persistentChat: true, from: scenario.from }, target, {},
                { url: '/admin/rest/web-pages', ...record });
            const { chat, values } = conversation;
            values.title = 'Source title';
            if (scenario.selection) {
                const editable = chat.element.ownerDocument.createElement('div');
                editable.innerHTML = target;
                chat.element.ownerDocument.body.append(editable);
                const selected = editable.children[1];
                conversation.selection = {
                    editor: { status: 'ready', getData: () => values.data, editable: () => ({ $: editable }) },
                    ranges: [{ startContainer: { $: selected.firstChild }, endContainer: { $: selected.firstChild } }],
                    value: target, inputValue: selected.outerHTML
                };
                if (scenario.stale) values.data = '<p>Changed after selection</p>';
            }

            const sending = chat.send('Prepare a short paragraph');
            const request = f.chatRequests[0];
            assert.equal(request.inputData.inputValue, scenario.expected);
            assert.equal(request.inputData.userPrompt, 'Prepare a short paragraph');
            assert.equal(Boolean(request.inputData.persistentChat), record.label === 'saved record');
            request.resolve({ success: true, response: 'Suggested paragraph' });
            await sending;
        });
    }
}

for (const provider of ['openai', 'gemini']) {
    test(`Page chat with ${provider} sends selected files with editor context and retains them for a retry`, async t => {
        const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
        const { chat, values } = f.editorChat({ ...chatAssistants[0], provider });
        const file = new File(['Document content'], 'notes.txt', { type: 'text/plain' });
        const attach = chat.element.querySelector('.btn-ai-chat-attach');
        assert.equal(attach.disabled, false);
        const fileInput = chat.element.querySelector('input[type="file"]');
        Object.defineProperty(fileInput, 'files', { configurable: true, value: [file] });
        fileInput.dispatchEvent(new fileInput.ownerDocument.defaultView.Event('change', { bubbles: true }));
        assert.match(chat.$attachments.text(), /notes.txt/);
        assert.equal(chat.$send.prop('disabled'), false, 'A file alone can be sent.');
        chat.state.conversationId = 'previous-turn';

        const sending = chat.send('');
        const request = f.chatRequests[0];
        assert.match(request.inputData.userPrompt, /attachmentPrompt/);
        assert.equal(request.inputData.files[0], file);
        assert.equal(request.inputData.inputValue, '<p>Existing page</p>');
        assert.equal(request.inputData.conversationId, 'previous-turn');
        assert.equal(request.inputData.persistentChat, undefined, 'Editor uploads must retain the editor request path.');
        assert.equal(chat.state.attachments.length, 0);
        assert.equal(attach.disabled, true, 'Do not accept files while sending.');
        chat._addAttachments([new File(['Not accepted'], 'pending.txt')]);
        assert.equal(chat.state.attachments.length, 0);
        request.resolve({ success: false, errorText: 'Temporary failure', retryable: true });
        await sending;
        assert.equal(attach.disabled, false);

        values.data = '<p>Edited after the failed request</p>';
        const failed = chat.state.messages.at(-1);
        const retrying = chat.send(failed.userPrompt, failed);
        assert.equal(f.chatRequests[1].inputData.files[0], file);
        assert.equal(f.chatRequests[1].inputData.inputValue, '<p>Existing page</p>', 'Retry the original page context.');
        assert.equal(f.chatRequests[1].inputData.conversationId, 'previous-turn');
        f.chatRequests[1].resolve({ success: true, response: 'Suggested content', conversationId: 'next-turn' });
        await retrying;

        const following = chat.send('Continue editing');
        assert.equal(f.chatRequests[2].inputData.files, undefined, 'A new turn must not silently re-upload previous files.');
        assert.equal(f.chatRequests[2].inputData.inputValue, values.data);
        assert.equal(f.chatRequests[2].inputData.conversationId, 'next-turn');
        f.chatRequests[2].resolve({ success: true, response: 'Follow-up content' });
        await following;
    });
}

test('Page chat rejects uploads for unsupported providers and image fields', t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
    for (const [provider, renderFormat] of [['browser', ''], ['local', ''], ['openai', 'dt-format-image'], ['gemini', 'dt-format-image']]) {
        const { chat } = f.editorChat({ ...chatAssistants[0], provider }, '/images/photo.png', { renderFormat });
        assert.equal(chat.element.querySelector('.btn-ai-chat-attach').disabled, true);
        chat._addAttachments([new File(['Document content'], 'notes.txt')]);
        assert.equal(chat.state.attachments.length, 0);
        assert.equal(chat.$send.prop('disabled'), true);
    }
    assert.equal(f.chatRequests.length, 0);
});

for (const draft of ['', 'A separate follow-up draft']) {
    test(`Page chat restart ${draft ? 'preserves a separate draft' : 'restores failed attachments'} after an expired continuation`, async t => {
        const f = fixture(t, deniedRestPermissions, { aiChat: true, editorChat: true });
        const { chat } = f.editorChat(chatAssistants[0]);
        const file = new File(['Document content'], 'notes.txt', { type: 'text/plain' });
        chat._addAttachments([file]);
        chat.state.conversationId = 'expired-turn';
        const sending = chat.send('Update the page using this document');
        if (draft) typeChat(chat.$input[0], draft);
        f.chatRequests[0].resolve({ success: false, errorText: 'Expired continuation', errorCategory: 'INVALID_CONTINUATION' });
        await sending;
        assert.equal(chat.state.needsNewChat, true);
        chat.element.querySelector('[data-ai-action="restart"]').click();

        assert.equal(chat.state.needsNewChat, false);
        assert.equal(chat.state.conversationId, null);
        assert.equal(chat.$input.val(), draft || 'Update the page using this document');
        assert.equal(f.chatRequests.length, 1, 'Restarting restores the composer without sending it.');
        assert.deepEqual(Array.from(chat.state.attachments, item => item.file), draft ? [] : [file]);

        const restarted = chat.send(chat.state.draft);
        assert.equal(f.chatRequests[1].inputData.conversationId, undefined);
        assert.equal(f.chatRequests[1].inputData.files?.[0], draft ? undefined : file);
        f.chatRequests[1].resolve({ success: true, response: 'Fresh conversation answer' });
        await restarted;
    });
}

for (const scenario of [
    { label: 'non-retryable error', retryable: false, canRetry: false },
    { label: 'retryable error', retryable: true, canRetry: true },
    { label: 'unexpected generation failure', retryable: null, canRetry: true },
    { label: 'invalid attachment', errorCategory: 'ATTACHMENT_INVALID', retryable: null, canRetry: false },
    { label: 'unsupported operation', errorCategory: 'UNSUPPORTED_OPERATION', retryable: true, canRetry: false },
    { label: 'invalid continuation', errorCategory: 'INVALID_CONTINUATION', retryable: true, canRetry: false },
    { label: 'stopped response', status: 'stopped', errorCategory: 'CANCELLED', retryable: false, canRetry: true }
]) {
    test(`Saved chat restores retry controls for a ${scenario.label}`, async t => {
        const f = fixture(t, deniedRestPermissions, { aiChat: true });
        await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
        const host = f.controller.chat;
        const detail = {
            thread: { id: 'failed-thread', assistantId: 7, canContinue: true, busy: false, revision: 1 },
            messages: [
                { id: 'question', role: 'user', text: 'Saved question', status: 'complete' },
                { id: 'answer', turnId: 'question', role: 'assistant', text: '', status: scenario.status || 'error',
                    errorCategory: scenario.errorCategory || 'UNKNOWN',
                    retryable: scenario.retryable }
            ]
        };
        const opening = host.openThread(detail.thread.id);
        await f.respond(f.requests.at(-1), detail);
        await opening;
        const chat = host.chat;
        const assertRetryControls = () => {
            assert.equal(Boolean(chat.element.querySelector('[data-ai-action="retry"]')), scenario.canRetry);
        };
        assertRetryControls();

        const refreshing = host.refreshConversation(chat);
        await f.respond(f.requests.at(-1), detail);
        await refreshing;
        assertRetryControls();
        assert.equal(f.chatRequests.length, 0, 'Opening or refreshing history must never submit another generation.');
    });
}

for (const newerDraft of [false, true]) {
    test(`Reloading rejected delivery ${newerDraft ? 'preserves a newer draft and files' : 'restores the original prompt and files'}`, async t => {
        const f = fixture(t, deniedRestPermissions, { aiChat: true });
        await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
        await openSavedChat(f, 'saved-thread', 'Saved answer');
        const chat = f.controller.chat.chat;
        const originalFile = new File(['Original attachment'], 'original.txt', { type: 'text/plain' });
        const newerFile = new File(['New attachment'], 'newer.txt', { type: 'text/plain' });
        chat._addAttachments([originalFile]);
        const sending = chat.send('Rejected question');
        f.chatRequests.at(-1).resolve({ success: false, persisted: false, errorText: 'Rejected by server' });
        await sending;
        if (newerDraft) {
            typeChat(chat.$input[0], 'Newer draft');
            chat._addAttachments([newerFile]);
        }

        await f.respond(f.requests.at(-1), {
            thread: { id: 'saved-thread', assistantId: 7, canContinue: true, busy: false, revision: 1 },
            messages: [
                { id: 'question', role: 'user', text: 'Saved question', status: 'complete' },
                { id: 'answer', turnId: 'question', role: 'assistant', text: 'Saved answer', status: 'complete' }
            ]
        });

        assert.equal(chat.$input.val(), newerDraft ? 'Newer draft' : 'Rejected question');
        assert.deepEqual(Array.from(chat.state.attachments, attachment => attachment.file), [newerDraft ? newerFile : originalFile]);
        assert.equal(chat.state.messages.length, 2);
        assert.equal(chat.state.messages.at(-1).text, 'Saved answer');
        assert.equal(chat.state.notice, 'Rejected by server');
        assert.equal(f.chatRequests.length, 1, 'Restoring history must not resend the rejected request.');
    });
}

test('Persistent assistant switching preserves an unsent draft and its original attachment', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    const file = new File(['Private draft attachment'], 'notes.txt', { type: 'text/plain' });
    typeChat(input, 'First unsent draft');
    host.chat._addAttachments([file]);

    selectChatAssistant(host, 8);
    assert.equal(host.chat.$input.val(), '');
    typeChat(host.chat.$input[0], 'Second unsent draft');
    selectChatAssistant(host, 7);

    assert.equal(host.chat.$input.val(), 'First unsent draft');
    assert.match(host.chat.$attachments.text(), /notes.txt/);
    assert.equal(f.chatRequests.length, 0, 'Switching assistants must not submit either draft.');
    sendChat(host.chat.$input[0]);
    assert.equal(f.chatRequests[0].inputData.userPrompt, 'First unsent draft');
    assert.equal(f.chatRequests[0].inputData.files[0], file);
    selectChatAssistant(host, 8);
    assert.equal(host.chat.$input.val(), 'Second unsent draft');
});

test('Persistent New chat starts empty and assistant switching resumes the new draft', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    typeChat(input, 'Previous draft');
    host.chat._addAttachments([new File(['Previous attachment'], 'previous.txt', { type: 'text/plain' })]);
    host.chat.element.querySelector('[data-ai-action="new"]').click();

    assert.equal(host.chat.$input.val(), '');
    assert.equal(host.chat.$attachments.text(), '');
    assert.equal(host.chat.element.querySelector('.ai-chat-message'), null);
    typeChat(host.chat.$input[0], 'New conversation draft');
    selectChatAssistant(host, 8);
    selectChatAssistant(host, 7);

    assert.equal(host.chat.$input.val(), 'New conversation draft');
    assert.equal(host.chat.$attachments.text(), '');
    sendChat(host.chat.$input[0]);
    assert.equal(f.chatRequests[0].inputData.userPrompt, 'New conversation draft');
    assert.equal(f.chatRequests[0].inputData.files, undefined);
});

test('Persistent assistant switching resumes the last selected saved thread even when it was created earlier', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    await openSavedChat(f, 'older-thread', 'Older saved answer');
    typeChat(host.chat.$input[0], 'Older thread draft');
    await openSavedChat(f, 'newer-thread', 'Newer saved answer');
    typeChat(host.chat.$input[0], 'Newer thread draft');
    await openSavedChat(f, 'older-thread', 'Older saved answer');

    selectChatAssistant(host, 8);
    selectChatAssistant(host, 7);

    assert.equal(host.chat.$input.val(), 'Older thread draft');
    assert.match(host.chat.element.textContent, /Older saved answer/);
    assert.doesNotMatch(host.chat.element.textContent, /Newer saved answer/);
});

test('Returning to a busy persistent thread refreshes its completed response and retains the draft', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    const opening = host.openThread('busy-thread');
    const detail = {
        thread: { id: 'busy-thread', assistantId: 7, canContinue: true, busy: true, revision: 1 },
        messages: [
            { id: 'question', role: 'user', text: 'Saved question', status: 'complete' },
            { id: 'answer', turnId: 'question', role: 'assistant', text: 'Partial answer', status: 'pending' }
        ]
    };
    await f.respond(f.requests.at(-1), detail);
    await opening;
    typeChat(host.chat.$input[0], 'Keep this follow-up draft');
    assert.equal(host.chat.$send.prop('disabled'), true);
    selectChatAssistant(host, 8);
    const requestsBeforeReturn = f.requests.length;
    f.advance(1500);
    assert.equal(f.requests.length, requestsBeforeReturn, 'An inactive thread does not keep polling.');

    selectChatAssistant(host, 7);
    assert.equal(f.requests.length, requestsBeforeReturn + 1, 'Returning must refresh the cached busy state.');
    assert.equal(f.requests.at(-1).url.pathname, '/admin/rest/ai/chat/threads/busy-thread');
    await f.respond(f.requests.at(-1), {
        thread: { ...detail.thread, busy: false },
        messages: [detail.messages[0], { ...detail.messages[1], text: 'Completed answer', status: 'complete' }]
    });

    assert.equal(host.chat.$input.val(), 'Keep this follow-up draft');
    assert.equal(host.chat.$send.prop('disabled'), false);
    assert.match(host.chat.element.textContent, /Completed answer/);
});

for (const [reload, revision] of [['refresh', 1], ['open', 1], ['open', 2]]) {
    test(`A completed turn survives delayed ${reload} history at revision ${revision}`, async t => {
        const f = fixture(t, deniedRestPermissions, { aiChat: true });
        await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
        await openSavedChat(f, 'saved-thread', 'Saved answer');
        const host = f.controller.chat;
        const chat = host.chat;
        const detail = { thread: { ...chat.threadInfo }, messages: [...chat.state.messages] };
        if (reload === 'open') host.openThread('saved-thread');
        else host.refreshConversation(chat);
        const olderRefresh = f.requests.at(-1);
        const requestsBeforeCompletion = f.requests.length;

        typeChat(chat.$input[0], 'Second question');
        sendChat(chat.$input[0]);
        const turn = f.chatRequests.at(-1);
        turn.callbacks.onMetadata({ threadId: detail.thread.id, revision: 2,
            messageId: 'second-answer', userMessageId: 'second-question' });
        if (revision === 2) {
            detail.thread = { ...detail.thread, revision: 2, busy: true };
            detail.messages = chat.state.messages.map(message => ({ ...message, requestId: turn.inputData.requestId }));
        }
        turn.resolve({ success: true, persisted: true, response: 'Second answer' });
        await tick();
        assert.equal(f.requests.length, requestsBeforeCompletion + 1, 'A completed turn immediately requests fresh history.');
        assert.equal(olderRefresh.signal.aborted, true, 'The old history read must be cancelled.');
        const freshRefresh = f.requests.at(-1);
        await f.respond(olderRefresh, detail);

        assert.equal(chat.state.revision, 2);
        assert.equal(Boolean(chat.state.serverBusy), false);
        assert.equal(chat.state.messages.at(-1).status, 'complete');
        assert.deepEqual(Array.from(chat.state.messages, message => message.text),
            ['Saved question', 'Saved answer', 'Second question', 'Second answer']);
        assert.equal(chat.$input.val(), '');
        assert.equal(f.requests.length, requestsBeforeCompletion + 1, 'A stale completion must not start another history request.');
        assert.equal(freshRefresh.url.pathname, olderRefresh.url.pathname);
        await f.respond(freshRefresh, {
            thread: { ...detail.thread, revision: 2, busy: false },
            messages: chat.state.messages.map(message => ({ ...message, requestId: turn.inputData.requestId }))
        });
        assert.equal(chat.state.messages.at(-1).text, 'Second answer');
        assert.equal(chat.state.serverBusy, false);
    });
}

test('Opening cached history selects immediately and preserves later assistant selection', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    await openSavedChat(f, 'saved-thread', 'Saved answer');
    const host = f.controller.chat;
    const saved = host.chat;
    selectChatAssistant(host, 8);
    const other = host.chat;

    host.openThread('saved-thread');
    const refresh = f.requests.at(-1);
    assert.equal(host.chat, saved);
    selectChatAssistant(host, 8);
    await f.respond(refresh, {
        thread: saved.threadInfo,
        messages: [{ id: 'answer', role: 'assistant', text: 'Refreshed answer', status: 'complete' }]
    });
    assert.equal(host.chat, other);
    assert.equal(saved.state.messages[0].text, 'Refreshed answer');
});

test('Persistent assistant switching starts a usable chat after its last selected thread is removed', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    await openSavedChat(f, 'removed-thread', 'Removed saved answer');
    typeChat(host.chat.$input[0], 'Removed thread draft');
    selectChatAssistant(host, 8);
    typeChat(host.chat.$input[0], 'Keep second assistant draft');
    host.removeChat('removed-thread');

    assert.equal(host.chat.$input.val(), 'Keep second assistant draft');
    selectChatAssistant(host, 7);
    const input = f.controller.chatPanel.querySelector('.ai-chat-input');
    assert.ok(input, 'The removed conversation must not leave an empty chat panel.');
    assert.equal(input.disabled, false);
    assert.equal(input.value, '');
    assert.doesNotMatch(host.chat.element.textContent, /Removed saved answer/);
    typeChat(input, 'A new usable draft');
    selectChatAssistant(host, 8);
    selectChatAssistant(host, 7);
    assert.equal(host.chat.$input.val(), 'A new usable draft');
});

test('Removing an older saved thread preserves the newer selection for the same persistent assistant', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    await openChat(f, chatAssistants.map(assistant => ({ ...assistant, persistentChat: true })));
    const host = f.controller.chat;
    await openSavedChat(f, 'older-thread', 'Older saved answer');
    await openSavedChat(f, 'newer-thread', 'Newer saved answer');
    typeChat(host.chat.$input[0], 'Keep newer thread draft');
    selectChatAssistant(host, 8);
    host.removeChat('older-thread');
    selectChatAssistant(host, 7);

    assert.equal(host.chat.$input.val(), 'Keep newer thread draft');
    assert.match(host.chat.element.textContent, /Newer saved answer/);
    assert.doesNotMatch(host.chat.element.textContent, /Older saved answer/);
});

test('History New chat resets only the selected conversation and returns focus to its composer', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const firstInput = await openChat(f);
    typeChat(firstInput, 'First question');
    sendChat(firstInput);
    f.chatRequests[0].resolve({ success: true, response: 'First reply', conversationId: 'first-conversation' });
    await tick();
    typeChat(firstInput, 'First draft');

    const host = f.controller.chat;
    host.select.value = '8';
    host.select.dispatchEvent(new firstInput.ownerDocument.defaultView.Event('change', { bubbles: true }));
    const secondInput = host.chat.$input[0];
    typeChat(secondInput, 'Second question');
    sendChat(secondInput);
    f.chatRequests[1].resolve({ success: true, response: 'Second reply', conversationId: 'second-conversation' });
    await tick();
    typeChat(secondInput, 'Second draft');
    host.toggleHistory(true);
    host.newChatButton.click();

    assert.equal(host.chat.state.messages.length, 0);
    assert.equal(secondInput.value, '');
    assert.equal(secondInput.ownerDocument.activeElement, secondInput);
    assert.ok(host.select.closest('.ai-chat-composer-actions'));
    assert.equal(host.chat.$toolbar.find('select').length, 0);
    host.select.value = '7';
    host.select.dispatchEvent(new firstInput.ownerDocument.defaultView.Event('change', { bubbles: true }));
    assert.equal(host.chat.$input.val(), 'First draft');
    assert.match(host.chat.element.textContent, /First reply/);
});

test('Composer Stop cancels only the selected assistant and preserves partial replies and drafts', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    const input = await openChat(f);
    typeChat(input, 'First question');
    sendChat(input);
    const first = f.chatRequests[0];
    const selector = f.controller.chatPanel.querySelector('select');
    selector.value = '8';
    selector.dispatchEvent(new input.ownerDocument.defaultView.Event('change', { bubbles: true }));
    const secondInput = f.controller.chatPanel.querySelector('.ai-chat-input');
    typeChat(secondInput, 'Second question');
    f.controller.chatPanel.querySelector('.btn-ai-chat-send').click();
    const second = f.chatRequests[1];
    second.callbacks.onUpdate('Partial reply');
    typeChat(secondInput, 'My next question');
    const stop = f.controller.chatPanel.querySelector('.btn-ai-chat-stop');
    assert.notEqual(stop.style.display, 'none');
    assert.equal(f.controller.chatPanel.querySelector('.btn-ai-chat-send').style.display, 'none');
    stop.click();
    second.callbacks.onUpdate('Late reply that must be ignored');
    await tick();

    assert.equal(second.cancellations, 1);
    assert.equal(first.cancellations, 0);
    assert.match(f.controller.chatPanel.textContent, /Partial reply/);
    assert.doesNotMatch(f.controller.chatPanel.textContent, /Late reply/);
    assert.equal(secondInput.value, 'My next question');
    assert.equal(secondInput.ownerDocument.activeElement, secondInput);
    assert.equal(stop.style.display, 'none');
    assert.equal(f.controller.chatPanel.querySelector('.btn-ai-chat-send').disabled, false);
    const attachment = f.controller.chatPanel.querySelector('.btn-ai-chat-attach');
    assert.equal(attachment.disabled, true, 'Unsaved chats cannot upload attachments.');

    selector.value = '7';
    selector.dispatchEvent(new input.ownerDocument.defaultView.Event('change', { bubbles: true }));
    const firstStop = f.controller.chatPanel.querySelector('.btn-ai-chat-stop');
    assert.notEqual(firstStop.style.display, 'none');
    firstStop.click();
    await tick();
    assert.equal(first.cancellations, 1);
});

test('Chat assistant loading errors can be retried and an empty catalogue stays usable', async t => {
    const f = fixture(t, deniedRestPermissions, { aiChat: true });
    f.controller.chips.get('aiChat').click();
    await f.respond(f.requests[0], {}, 503);
    assert.match(f.controller.chatPanel.textContent, /chatUnavailable.js/);
    const retry = [...f.controller.chatPanel.querySelectorAll('button')].find(button => /retry.js/.test(button.textContent));
    assert.ok(retry);
    retry.click();
    assert.equal(f.requests.length, 2);
    await f.respond(f.requests[1], []);
    assert.match(f.controller.chatPanel.textContent, /chatEmpty.js/);
    assert.equal(f.controller.chatPanel.querySelector('.ai-chat-input'), null);
    f.controller.chips.get('all').click();
    assert.equal(f.controller.results.hidden, false);
    assert.equal(f.chatRequests.length, 0);
});
