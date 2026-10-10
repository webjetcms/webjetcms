const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

const sourceRoot = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js');
const announcement = '<h2>WebJET CMS 2026.18</h2><ul><li><b>First update</b> – First description.</li><li><b>Second update</b> – Second description.</li></ul>';
const tick = () => new Promise(resolve => setImmediate(resolve));

/** Runs the real release widget with deterministic timers, randomness and account storage. */
function fixture(t, { reduced = false, saved, response = true, random = 0.75 } = {}) {
    const dom = new JSDOM('<div class="md-dashboard__hero"><div class="md-dashboard__welcome"><h1>Welcome back</h1><a href="/pages">Pages</a></div><div class="md-dashboard__news"></div></div>');
    const { window } = dom;
    const container = window.document.querySelector('.md-dashboard__news');
    window.userLng = 'en';
    window.csrfToken = 'autotest-csrf';
    window.currentUser = { adminSettings: saved || {} };
    const errors = [];
    window.WJ = { notifyError: (...args) => errors.push(args) };
    const media = { matches: reduced, addEventListener() {}, removeEventListener() {} };
    window.matchMedia = () => media;
    const timers = new Map();
    let counter = 0;
    window.setTimeout = (callback, delay) => { timers.set(++counter, { callback, delay }); return counter; };
    window.clearTimeout = id => timers.delete(id);
    Object.defineProperty(window.document, 'hidden', { configurable: true, value: false });
    const requests = [];
    const math = Object.create(Math); math.random = () => random;
    const scope = vm.createContext({ window, document: window.document, DOMParser: window.DOMParser, Math: math,
        fetch: async (url, options) => { requests.push({ url, options }); return { ok: true, json: async () => response }; } });
    for (const file of ['registry.js', 'widget-utils.js', 'news-widget.js']) {
        vm.runInContext(fs.readFileSync(path.join(sourceRoot, 'dashboard', file), 'utf8').replace(/^import .+;\r?$/gm, '').replace(/^export /gm, ''), scope);
    }
    scope.registerNewsWidget();
    const context = { labels: { changelog: announcement }, config: {}, translate: (key, ...params) => [key, ...params].join(' ') };
    const controller = new window.AbortController();
    let cleanup;
    const render = () => { cleanup = scope.getWidget('news').render({ container, context, signal: controller.signal }); };
    t.after(() => { controller.abort(); cleanup?.(); window.close(); });
    const advance = () => { const [id, timer] = [...timers.entries()][0]; timers.delete(id); timer.callback(); };
    return { scope, context, window, container, controller, render, timers, requests, errors, advance, cleanup: () => cleanup?.() };
}

test('Every shipped translation parses as five titled updates through the real Markdown renderer', t => {
    const { scope, context } = fixture(t);
    const webjet = fs.readFileSync(path.join(sourceRoot, 'webjet.js'), 'utf8');
    const start = webjet.indexOf('    function parseMarkdown(');
    vm.runInContext(webjet.slice(start, webjet.indexOf('    function initTooltip(', start)), scope);
    for (const locale of ['', '_cz', '_en']) {
        const properties = fs.readFileSync(path.resolve(sourceRoot, `../../../../WEB-INF/classes/text${locale}-webjet9.properties`), 'utf8');
        const raw = properties.split('\n').find(line => line.startsWith('admin.overview.changelog=')).split('=').slice(1).join('=');
        context.labels.changelog = scope.parseMarkdown('\n' + raw.replace(/\\\\n/g, '\n') + '\n\n', { link: true, imgSrcPrefix: '' });
        const news = scope.releaseNews(context);
        assert.equal(news.version, '2026.18');
        assert.equal(news.items.length, 5);
        assert.ok(news.items.every(item => item.title && item.description));
    }
});

test('Older paragraphs remain available without losing formatting in the full release', t => {
    const { scope, context } = fixture(t);
    context.labels.changelog = 'WebJET CMS <b>2026.18</b> first feature.<br><br>Second <i>feature</i>.';
    const news = scope.releaseNews(context);
    assert.equal(news.items.length, 2);
    assert.equal(news.items[1].description, 'Second feature.');
    assert.equal(news.html, context.labels.changelog);
});

test('Each account gets thirty days from its first view, and changed text resets the window', t => {
    const { scope, window } = fixture(t);
    const now = Date.UTC(2026, 9, 10);
    const first = scope.newsSeenState(announcement, now);
    assert.equal(first.isNew, true);
    window.currentUser.adminSettings[first.key] = first.value;
    assert.equal(scope.newsSeenState(announcement, now + 30 * 86400000 - 1).isNew, true);
    const expired = scope.newsSeenState(announcement, now + 30 * 86400000);
    assert.equal(expired.isNew, false);
    assert.equal(expired.changed, false);
    assert.equal(scope.newsSeenState(announcement + 'Updated', now + 40 * 86400000).isNew, true);
    window.currentUser = { adminSettings: {} };
    const lateUser = scope.newsSeenState(announcement, now + 50 * 86400000);
    assert.equal(lateUser.isNew, true);
    assert.equal(JSON.parse(lateUser.value).firstSeen, now + 50 * 86400000);
});

test('Language changes and invalid records cannot corrupt another language reading window', t => {
    const { scope, window } = fixture(t);
    const first = scope.newsSeenState(announcement);
    window.currentUser.adminSettings[first.key] = first.value;
    window.userLng = 'sk';
    assert.equal(scope.newsSeenState(announcement).changed, true);
    window.currentUser.adminSettings['dashboard.news.sk'] = '{invalid';
    assert.equal(scope.newsSeenState(announcement).isNew, true);
    window.userLng = 'en';
    assert.equal(scope.newsSeenState(announcement).changed, false);
});

test('New text saves only its account record; failed saves never update the cached preference', async t => {
    for (const response of [true, false]) {
        const { render, requests, window, errors } = fixture(t, { response });
        render(); await tick();
        assert.equal(requests.length, 1);
        assert.equal(requests[0].url, '/admin/rest/admin-settings/');
        const body = JSON.parse(requests[0].options.body);
        assert.equal(body.label, 'dashboard.news.en');
        assert.equal(requests[0].options.headers['X-CSRF-Token'], 'autotest-csrf');
        assert.equal(Boolean(window.currentUser.adminSettings[body.label]), response);
        assert.equal(errors.length, response ? 0 : 1);
    }
});

test('An unchanged persisted announcement does not write on subsequent renders', t => {
    const { scope, window, render, requests } = fixture(t);
    const state = scope.newsSeenState(announcement);
    window.currentUser.adminSettings[state.key] = state.value;
    render();
    assert.equal(requests.length, 0);
});

test('Random initial item, wrapping arrows, dots and eight-second automatic rotation share the same state', t => {
    const { render, container, timers, advance } = fixture(t);
    render();
    const title = () => container.querySelector('.md-dashboard-widget__news-title').textContent;
    assert.equal(title(), 'Second update');
    assert.equal([...timers.values()][0].delay, 8000);
    advance(); assert.equal(title(), 'First update');
    container.querySelector('[aria-label="admin.dashboard.newsPrevious.js"]').click();
    assert.equal(title(), 'Second update');
    container.querySelector('.md-dashboard-widget__news-dot').click();
    assert.equal(title(), 'First update');
    assert.equal(container.querySelectorAll('[aria-current="true"]').length, 1);
});

test('Hover, focus, reduced motion, hidden pages and explicit pause stop rotation', t => {
    const { render, container, window, timers } = fixture(t);
    render();
    container.dispatchEvent(new window.Event('mouseenter')); assert.equal(timers.size, 0);
    container.dispatchEvent(new window.Event('mouseleave')); assert.equal(timers.size, 1);
    Object.defineProperty(window.document, 'hidden', { value: true });
    window.document.dispatchEvent(new window.Event('visibilitychange')); assert.equal(timers.size, 0);
    Object.defineProperty(window.document, 'hidden', { value: false });
    window.document.dispatchEvent(new window.Event('visibilitychange')); assert.equal(timers.size, 1);
    const playback = container.querySelector('.md-dashboard-widget__news-playback');
    playback.focus(); playback.click(); assert.equal(timers.size, 0, 'Focusing the pause button must not invert the click.');
    playback.click(); assert.equal(timers.size, 1);
    container.querySelector('.md-dashboard-widget__news-dot').focus(); assert.equal(timers.size, 0);
    const reduced = fixture(t, { reduced: true }); reduced.render(); assert.equal(reduced.timers.size, 0);
});

test('Expanding preserves welcome nodes, shows complete HTML, restores focus and releases timers', t => {
    const { render, window, container, timers, cleanup } = fixture(t);
    const welcome = window.document.querySelector('.md-dashboard__welcome');
    const original = welcome.innerHTML;
    render();
    const toggle = container.querySelector('.md-dashboard-widget__news-toggle');
    toggle.click();
    assert.equal(welcome.hidden, true);
    assert.equal(container.querySelector('.md-dashboard-widget__news-highlights').innerHTML, announcement);
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(timers.size, 0);
    assert.equal(window.document.activeElement, container.querySelector('.md-dashboard-widget__news-close'));
    container.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    assert.equal(welcome.hidden, false);
    assert.equal(welcome.innerHTML, original);
    assert.equal(window.document.activeElement, toggle);
    toggle.click(); cleanup();
    assert.equal(welcome.hidden, false);
    assert.equal(timers.size, 0);
    window.document.dispatchEvent(new window.Event('visibilitychange'));
    assert.equal(timers.size, 0);
});

test('One or zero updates never schedule automatic rotation', t => {
    for (const html of ['', '<p>Single update</p>']) {
        const { context, render, timers, container } = fixture(t);
        context.labels.changelog = html;
        render();
        assert.equal(timers.size, 0);
        const navigation = container.querySelector('.md-dashboard-widget__news-navigation');
        assert.ok(!navigation || navigation.hidden);
    }
});
