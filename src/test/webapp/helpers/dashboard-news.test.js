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
function fixture(t, { reduced = false, saved, response = true, random = 0.75, fetchResponse, config = {} } = {}) {
    const dom = new JSDOM('<div class="md-dashboard__hero"><div class="md-dashboard__welcome"><h1>Welcome back</h1><a href="/pages">Pages</a></div><div class="md-dashboard__news"></div></div>', { url: 'http://cms.example/admin/v9/' });
    const { window } = dom;
    const container = window.document.querySelector('.md-dashboard__news');
    window.userLng = 'en';
    window.csrfToken = 'autotest-csrf';
    window.currentUser = { adminSettings: saved || {} };
    const errors = [];
    window.WJ = {
        notifyError: (...args) => errors.push(args),
        translate: key => key,
        getDocumentationUrl: path => `https://customer.example/manual/latest/en${path}`
    };
    const media = { matches: reduced, addEventListener() {}, removeEventListener() {} };
    window.matchMedia = () => media;
    const timers = new Map();
    let counter = 0;
    window.setTimeout = (callback, delay) => { timers.set(++counter, { callback, delay }); return counter; };
    window.clearTimeout = id => timers.delete(id);
    Object.defineProperty(window.document, 'hidden', { configurable: true, value: false });
    const requests = [];
    const math = Object.create(Math); math.random = () => random;
    const scope = vm.createContext({ window, document: window.document, DOMParser: window.DOMParser, URL, TextEncoder, TextDecoder, Math: math,
        fetch: async (url, options) => {
            requests.push({ url, options });
            const result = fetchResponse ? await fetchResponse(url, options) : { ok: true, json: async () => response };
            if (!result.text) result.text = async () => JSON.stringify(await result.json());
            return result;
        } });
    for (const file of ['dashboard/registry.js', 'dashboard/widget-utils.js', 'release-news.js', 'dashboard/social-news.js', 'dashboard/news-widget.js']) {
        vm.runInContext(fs.readFileSync(path.join(sourceRoot, file), 'utf8').replace(/^import .+;\r?$/gm, '').replace(/^export /gm, ''), scope);
    }
    scope.registerNewsWidget();
    const context = { labels: { changelog: announcement }, config, translate: (key, ...params) => [key, ...params].join(' ') };
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
        const news = scope.releaseNews(context.labels.changelog);
        assert.equal(news.version, '2026.18');
        assert.equal(news.items.length, 5);
        assert.ok(news.items.every(item => item.title && item.description));
    }
});

test('Older paragraphs remain available without losing formatting in the full release', t => {
    const { scope, context } = fixture(t);
    context.labels.changelog = 'WebJET CMS <b>2026.18</b> first feature.<br><br>Second <i>feature</i>.';
    const news = scope.releaseNews(context.labels.changelog);
    assert.equal(news.items.length, 2);
    assert.equal(news.items[1].description, 'Second feature.');
    assert.equal(news.html, context.labels.changelog);
});

test('Shared layout announcements are rendered once and refreshed when the localized text changes', t => {
    const { scope, window } = fixture(t);
    const webjet = fs.readFileSync(path.join(sourceRoot, 'webjet.js'), 'utf8');
    const start = webjet.indexOf('    function parseMarkdown(');
    vm.runInContext(webjet.slice(start, webjet.indexOf('    function initTooltip(', start)), scope);
    let renders = 0;
    window.WJ.parseMarkdown = (...args) => { renders++; return scope.parseMarkdown(...args); };
    window.adminHelpContext = { announcement: '## WebJET CMS 2026.18\\n\\n- **First update** – First description.' };
    const first = scope.getReleaseNews();
    assert.equal(first.version, '2026.18');
    assert.equal(first.items.length, 1);
    assert.equal(first.items[0].title, 'First update');
    assert.equal(scope.getReleaseNews(), first);
    assert.equal(renders, 1);
    window.adminHelpContext.announcement += '\\n- **Second update** – Second description.';
    const updated = scope.getReleaseNews();
    assert.equal(updated.items.length, 2);
    assert.notEqual(scope.newsFingerprint(updated.html), scope.newsFingerprint(first.html));
    assert.equal(renders, 2);
});

test('Release news stays unread until acknowledged and changed text becomes unread again without a time limit', t => {
    const { scope, window } = fixture(t);
    const now = Date.UTC(2026, 9, 10);
    const first = scope.newsSeenState(announcement, now);
    assert.equal(first.isNew, true);
    assert.equal(scope.newsSeenState(announcement, now + 365 * 86400000).isNew, true);
    window.currentUser.adminSettings[first.key] = JSON.stringify({ fingerprint: scope.newsFingerprint(announcement), firstSeen: now - 31 * 86400000 });
    assert.equal(scope.newsSeenState(announcement, now).isNew, true, 'Legacy exposure is not proof of reading the full text.');
    window.currentUser.adminSettings[first.key] = first.value;
    const read = scope.newsSeenState(announcement, now + 365 * 86400000);
    assert.equal(read.isNew, false);
    assert.equal(read.changed, false);
    assert.equal(scope.newsSeenState(announcement + 'Updated', now + 40 * 86400000).isNew, true);
    window.currentUser = { adminSettings: {} };
    const lateUser = scope.newsSeenState(announcement, now + 50 * 86400000);
    assert.equal(lateUser.isNew, true);
    assert.equal(JSON.parse(lateUser.value).readAt, now + 50 * 86400000);
});

test('Language changes and invalid records cannot corrupt another language acknowledgement', t => {
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

test('Only expansion acknowledges local news and a failed save keeps the badge unread', async t => {
    for (const response of [true, false]) {
        const { render, requests, window, errors, container } = fixture(t, { response });
        render(); await tick();
        assert.equal(requests.length, 0, 'Collapsed previews must not acknowledge local news.');
        assert.equal(container.querySelector('.md-dashboard-widget__news-badge').hidden, false);
        container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
        assert.equal(requests.length, 1);
        assert.equal(requests[0].url, '/admin/rest/admin-settings/');
        const body = JSON.parse(requests[0].options.body);
        assert.equal(body.label, 'dashboard.news.en');
        assert.equal(requests[0].options.headers['X-CSRF-Token'], 'autotest-csrf');
        assert.equal(Boolean(window.currentUser.adminSettings[body.label]), response);
        assert.ok(JSON.parse(body.value).readAt > Date.now() - 60000);
        assert.equal(container.querySelector('.md-dashboard-widget__news-badge').hidden, response);
        assert.equal(errors.length, response ? 0 : 1);
    }
});

test('An unchanged acknowledged announcement does not write on subsequent renders or expansions', async t => {
    const { scope, window, render, requests, container } = fixture(t);
    const state = scope.newsSeenState(announcement);
    window.currentUser.adminSettings[state.key] = state.value;
    render();
    container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    assert.equal(requests.length, 0);
});

test('Repeated expansion shares one pending save even when acknowledgement timestamps differ', async t => {
    let finish;
    const { scope, requests, window } = fixture(t, { fetchResponse: () => new Promise(resolve => { finish = resolve; }) });
    const now = Date.UTC(2026, 9, 10);
    const dashboardState = scope.newsSeenState(announcement, now);
    const dashboardSave = scope.saveNewsSeen(dashboardState);
    const repeatedState = scope.newsSeenState(announcement, now + 500);
    const repeatedSave = scope.saveNewsSeen(repeatedState);
    assert.equal(repeatedSave, dashboardSave);
    assert.equal(requests.length, 1);
    finish({ ok: true, json: async () => true });
    assert.equal(await repeatedSave, true);
    assert.equal(window.currentUser.adminSettings[dashboardState.key], dashboardState.value);
    assert.equal(await scope.saveNewsSeen(repeatedState), true);
    assert.equal(requests.length, 1, 'A stale caller must not overwrite a successfully saved acknowledgement.');
});

test('Shared failures notify once, retain confirmed settings and allow a later retry', async t => {
    let finish;
    const { scope, requests, window, errors } = fixture(t, { fetchResponse: () => new Promise(resolve => { finish = resolve; }) });
    const state = scope.newsSeenState(announcement);
    const firstSave = scope.saveNewsSeen(state);
    assert.equal(scope.saveNewsSeen(state), firstSave);
    finish({ ok: true, json: async () => false });
    assert.equal(await firstSave, false);
    assert.equal(errors.length, 1);
    assert.equal(window.currentUser.adminSettings[state.key], undefined);
    const retry = scope.saveNewsSeen(state);
    assert.equal(requests.length, 2);
    finish({ ok: true, json: async () => true });
    assert.equal(await retry, true);
    assert.equal(window.currentUser.adminSettings[state.key], state.value);
    assert.equal(errors.length, 1);
});

test('Removing the dashboard after expansion does not cancel its explicit acknowledgement', async t => {
    let finish;
    const { scope, render, container, controller, cleanup, requests, window } = fixture(t, { fetchResponse: () => new Promise(resolve => { finish = resolve; }) });
    render();
    container.querySelector('.md-dashboard-widget__news-toggle').click();
    controller.abort();
    cleanup();
    const state = scope.newsSeenState(announcement);
    const helpSave = scope.saveNewsSeen(state);
    assert.equal(requests.length, 1);
    finish({ ok: true, json: async () => true });
    assert.equal(await helpSave, true);
    assert.equal(JSON.parse(window.currentUser.adminSettings[state.key]).fingerprint, scope.newsFingerprint(announcement));
});

test('Both dashboard changelog links use the configured documentation URL', t => {
    const { render, container } = fixture(t);
    render();
    for (const selector of ['.md-dashboard-widget__news-version', '.md-dashboard-widget__news-more']) {
        assert.equal(container.querySelector(selector).href, 'https://customer.example/manual/latest/en/CHANGELOG');
    }
});

test('A hidden expanded dashboard acknowledges the release only when it becomes visible', async t => {
    const f = fixture(t);
    Object.defineProperty(f.window.document, 'hidden', { configurable: true, value: true });
    f.render();
    f.container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    assert.equal(f.requests.length, 0);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, false);
    Object.defineProperty(f.window.document, 'hidden', { configurable: true, value: false });
    f.window.document.dispatchEvent(new f.window.Event('visibilitychange')); await tick();
    assert.equal(f.requests.length, 1);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, true);
});

test('Pending release acknowledgements serialize changed text and remain isolated between accounts', async t => {
    const finishes = [];
    const { scope, requests, window } = fixture(t, { fetchResponse: () => new Promise(resolve => finishes.push(resolve)) });
    const firstUser = window.currentUser;
    const first = scope.saveNewsSeen(scope.newsSeenState(announcement));
    const changed = scope.newsSeenState(announcement + 'Changed');
    const second = scope.saveNewsSeen(changed);
    assert.equal(requests.length, 1, 'A newer acknowledgement must wait for the older write.');
    finishes.shift()({ ok: true, json: async () => true });
    await first; await tick();
    assert.equal(requests.length, 2);
    window.currentUser = { adminSettings: {} };
    const secondUser = window.currentUser;
    const other = scope.saveNewsSeen(scope.newsSeenState(announcement + 'Changed'));
    assert.equal(requests.length, 3, 'Another account must not reuse an in-flight acknowledgement.');
    finishes.shift()({ ok: true, json: async () => true });
    await second;
    assert.equal(firstUser.adminSettings[changed.key], changed.value);
    assert.equal(secondUser.adminSettings[changed.key], undefined);
    finishes.shift()({ ok: true, json: async () => true });
    await other;
    assert.equal(scope.newsSeenState(announcement + 'Changed').isNew, false);
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
    const channels = container.querySelector('.md-dashboard-widget__news-channels');
    assert.equal(channels.hidden, true);
    toggle.click();
    assert.equal(channels.hidden, false);
    assert.equal(welcome.hidden, true);
    assert.equal(container.querySelector('.md-dashboard-widget__news-highlights').innerHTML, announcement);
    assert.equal(toggle.getAttribute('aria-expanded'), 'true');
    assert.equal(timers.size, 0);
    assert.equal(window.document.activeElement, container.querySelector('.md-dashboard-widget__news-close'));
    container.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    assert.equal(welcome.hidden, false);
    assert.equal(channels.hidden, true);
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

const socialFeed = { news: [
    { id: 'youtube:video', source: 'youtube', title: 'New video', perex: 'Video summary', link: 'https://www.youtube.com/watch?v=video', image: 'https://news.example/video.jpg', publishedAt: '2026-10-18T09:00:00Z' },
    { id: 'facebook:post', source: 'facebook', title: 'New post', perex: 'Post summary', link: 'https://www.facebook.com/posts/post', image: 'https://news.example/post.jpg' }
] };
const socialConfig = { overviewJsonUrl: 'https://news.example/json/', socialRefreshHours: 8 };

/** Acknowledges the local release so tests exercise only external unread content. */
function acknowledgeRelease(f) {
    const state = f.scope.newsSeenState(announcement);
    f.window.currentUser.adminSettings[state.key] = state.value;
}

test('Social cards supplement local release HTML, defer unselected images, and acknowledge only expanded content', async t => {
    const f = fixture(t, { config: socialConfig, fetchResponse: async (url, options) => ({ ok: true, json: async () => options?.method === 'POST' ? true : socialFeed }) });
    acknowledgeRelease(f);
    f.render(); await tick();
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-highlights').innerHTML, announcement);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-dot').length, 4);
    assert.equal(f.container.querySelectorAll('img').length, 0);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, false);
    assert.equal(f.requests.filter(request => request.options?.method === 'POST').length, 0);
    f.container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-card').length, 2);
    assert.equal(f.container.querySelectorAll('img').length, 2);
    assert.equal(f.requests.filter(request => request.url.startsWith('https://news.example')).length, 2, 'Expansion revalidates even a fresh feed.');
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, true);
    const image = f.container.querySelector('img');
    image.dispatchEvent(new f.window.Event('error'));
    assert.equal(f.container.querySelectorAll('img').length, 1);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-card').length, 2, 'Image failure keeps the text card.');
});

test('Collapsed social slides show their source and only their selected thumbnail without acknowledging the feed', async t => {
    const f = fixture(t, { config: socialConfig, fetchResponse: async (url, options) => ({ ok: true, json: async () => options?.method === 'POST' ? true : socialFeed }) });
    acknowledgeRelease(f);
    f.render(); await tick();
    const version = f.container.querySelector('.md-dashboard-widget__news-version');
    const source = f.container.querySelector('.md-dashboard-widget__news-source');
    const thumbnail = () => f.container.querySelector('.md-dashboard-widget__news-preview-image');
    assert.equal(version.hidden, false);
    assert.equal(source.hidden, true);
    f.advance();
    assert.equal(source.textContent, 'YouTube');
    assert.ok(source.querySelector('.ti-brand-youtube'));
    assert.equal(version.hidden, true);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-title').textContent, 'New video');
    assert.equal(thumbnail().src, socialFeed.news[0].image);
    assert.equal(thumbnail().alt, '');
    assert.equal(thumbnail().crossOrigin, 'anonymous');
    assert.equal(thumbnail().referrerPolicy, 'no-referrer');
    assert.equal(f.container.querySelectorAll('img').length, 1);
    f.advance();
    assert.equal(source.textContent, 'Facebook');
    assert.ok(source.querySelector('.ti-brand-facebook'));
    assert.equal(thumbnail().src, socialFeed.news[1].image);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, false);
    assert.equal(f.requests.filter(request => request.options?.method === 'POST').length, 0);
    f.container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    assert.equal(source.hidden, true);
    assert.equal(version.hidden, true);
    assert.equal(thumbnail(), null);
    f.container.querySelector('.md-dashboard-widget__news-close').click();
    assert.equal(source.hidden, false);
    assert.equal(version.hidden, true);
    assert.equal(thumbnail().src, socialFeed.news[1].image);
    f.container.querySelector('.md-dashboard-widget__news-dot').click();
    assert.equal(source.hidden, true);
    assert.equal(version.hidden, false);
    assert.equal(thumbnail(), null);
});

test('Broken or absent preview images keep the text and a late image error cannot remove the next slide', async t => {
    const feed = { news: [socialFeed.news[0], { ...socialFeed.news[1], image: null }] };
    const f = fixture(t, { config: socialConfig, fetchResponse: async () => ({ ok: true, json: async () => feed }) });
    f.render(); await tick();
    const dots = f.container.querySelectorAll('.md-dashboard-widget__news-dot');
    dots[2].click();
    const failed = f.container.querySelector('.md-dashboard-widget__news-preview-image');
    dots[3].click();
    failed.dispatchEvent(new f.window.Event('error'));
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-title').textContent, 'New post');
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-source').textContent, 'Facebook');
    assert.equal(f.container.querySelectorAll('img').length, 0);
    dots[2].click();
    assert.equal(f.container.querySelectorAll('img').length, 0, 'Failed previews must not be requested on every rotation.');
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-description').textContent, 'Video summary');
});

test('Expanded cards prefer extended text while the carousel stays short and missing images use the text-only layout', async t => {
    const feed = { news: [
        { ...socialFeed.news[0], perex: 'Short video summary', perexLong: '**Long video text** &lt;img src=x&gt;' },
        { ...socialFeed.news[1], image: null, perexLong: 'Long Facebook text' },
        { ...socialFeed.news[0], id: 'youtube:old', perex: 'Old format text', perexLong: undefined },
        { ...socialFeed.news[1], id: 'facebook:malformed', perex: 'Fallback text', perexLong: { text: 'Must not render' } }
    ] };
    const f = fixture(t, { config: socialConfig, fetchResponse: async (url, options) => ({ ok: true, json: async () => options?.method === 'POST' ? true : feed }) });
    acknowledgeRelease(f);
    f.render(); await tick();
    f.container.querySelectorAll('.md-dashboard-widget__news-dot')[2].click();
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-description').textContent, 'Short video summary');
    f.container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    const cards = [...f.container.querySelectorAll('.md-dashboard-widget__news-card')];
    assert.equal(cards[0].querySelector('p').textContent, 'Long video text <img src=x>');
    assert.equal(cards[0].querySelector('p img'), null);
    assert.equal(cards[0].classList.contains('is-text-only'), false);
    assert.equal(cards[1].querySelector('p').textContent, 'Long Facebook text');
    assert.equal(cards[1].classList.contains('is-text-only'), true);
    assert.equal(cards[2].querySelector('p').textContent, 'Old format text');
    assert.equal(cards[3].querySelector('p').textContent, 'Fallback text');
    cards[0].querySelector('img').dispatchEvent(new f.window.Event('error'));
    assert.equal(cards[0].classList.contains('is-text-only'), true);
    assert.equal(cards[0].querySelector('img'), null);
    assert.equal(cards[0].querySelector('p').textContent, 'Long video text <img src=x>');
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, true);
});

test('A response arriving after collapse remains unread and never creates hidden image requests', async t => {
    let finish;
    const f = fixture(t, { config: socialConfig, fetchResponse: () => new Promise(resolve => { finish = resolve; }) });
    acknowledgeRelease(f);
    f.render();
    f.container.querySelector('.md-dashboard-widget__news-toggle').click();
    f.container.querySelector('.md-dashboard-widget__news-close').click();
    assert.equal(f.requests.length, 1, 'Background and expansion share the pending request.');
    finish({ ok: true, json: async () => socialFeed }); await tick();
    assert.equal(f.container.querySelectorAll('img').length, 0);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, false);
    assert.equal(f.requests.length, 1, 'Unseen content is not acknowledged.');
});

test('Disabling social news ignores populated cache and retains channel links even without release content', async t => {
    const f = fixture(t, { config: { ...socialConfig, socialRefreshHours: 0 }, fetchResponse: async () => ({ ok: true, json: async () => socialFeed }) });
    await f.scope.createSocialNewsClient({ url: 'https://news.example/json/wjnews.en.json', refreshHours: 8, signal: f.controller.signal }).refresh();
    const count = f.requests.length;
    f.context.labels.changelog = '';
    f.render();
    f.container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    assert.equal(f.requests.length, count);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-card, img').length, 0);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-channel').length, 2);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, true);
});

test('A failed refresh leaves only local news and channel links while preserving cache for a successful retry', async t => {
    let unavailable = false;
    const f = fixture(t, { config: socialConfig, fetchResponse: async (url, options) => {
        if (options?.method === 'POST') return { ok: true, json: async () => true };
        if (unavailable) throw new Error('Offline');
        return { ok: true, json: async () => socialFeed };
    } });
    acknowledgeRelease(f);
    f.render(); await tick();
    const cacheKey = f.window.localStorage.key(0);
    const cached = f.window.localStorage.getItem(cacheKey);
    unavailable = true;
    f.container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-card, img').length, 0);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-channel').length, 2);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-dot').length, 2);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-badge').hidden, true);
    const external = f.container.querySelector('.md-dashboard-widget__news-external');
    assert.equal(external.hidden, true);
    assert.equal(external.getAttribute('aria-busy'), 'false');
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-feed-status'), null);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-highlights').innerHTML, announcement);
    assert.equal(f.window.localStorage.getItem(cacheKey), cached, 'Failure preserves the cache and successful-fetch timestamp.');
    unavailable = false;
    f.container.querySelector('.md-dashboard-widget__news-close').click();
    f.container.querySelector('.md-dashboard-widget__news-toggle').click(); await tick();
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-card').length, 2);
    assert.equal(external.hidden, false);
    assert.equal(external.getAttribute('aria-busy'), 'false');
});

test('Loading and empty feeds show no external heading or status text', async t => {
    let finish;
    const f = fixture(t, { config: socialConfig, fetchResponse: (url, options) => {
        if (options?.method === 'POST') return Promise.resolve({ ok: true, json: async () => true });
        return new Promise(resolve => { finish = resolve; });
    } });
    acknowledgeRelease(f);
    f.render();
    f.container.querySelector('.md-dashboard-widget__news-toggle').click();
    const external = f.container.querySelector('.md-dashboard-widget__news-external');
    assert.equal(external.hidden, true);
    assert.equal(external.getAttribute('aria-busy'), 'true');
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-feed-status'), null);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-channel').length, 2);
    finish({ ok: true, json: async () => ({ news: [] }) }); await tick();
    assert.equal(external.hidden, true);
    assert.equal(external.getAttribute('aria-busy'), 'false');
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-card').length, 0);
    assert.equal(f.container.querySelector('.md-dashboard-widget__news-highlights').innerHTML, announcement);
});

test('A refresh failure after collapse removes previously rendered cards and images', async t => {
    let fail;
    let calls = 0;
    const f = fixture(t, { config: socialConfig, fetchResponse: (url, options) => {
        if (options?.method === 'POST') return Promise.resolve({ ok: true, json: async () => true });
        if (++calls === 1) return Promise.resolve({ ok: true, json: async () => socialFeed });
        return new Promise((resolve, reject) => { fail = reject; });
    } });
    acknowledgeRelease(f);
    f.render(); await tick();
    f.container.querySelector('.md-dashboard-widget__news-toggle').click();
    assert.equal(f.container.querySelectorAll('img').length, 2);
    f.container.querySelector('.md-dashboard-widget__news-close').click();
    fail(new Error('Offline')); await tick();
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-card, img').length, 0);
    assert.equal(f.container.querySelectorAll('.md-dashboard-widget__news-dot').length, 2);
    const external = f.container.querySelector('.md-dashboard-widget__news-external');
    assert.equal(external.hidden, true);
    assert.equal(external.getAttribute('aria-busy'), 'false');
});

test('Background carousel updates preserve the focused release dot and fall back when a social dot disappears', async t => {
    let finish;
    const f = fixture(t, { config: socialConfig, fetchResponse: (url, options) => {
        if (options?.method === 'POST') return Promise.resolve({ ok: true, json: async () => true });
        return new Promise(resolve => { finish = resolve; });
    } });
    acknowledgeRelease(f);
    f.render();
    const originalDot = f.container.querySelector('.md-dashboard-widget__news-dot');
    originalDot.focus();
    finish({ ok: true, json: async () => socialFeed }); await tick();
    const updatedDot = f.container.querySelector('.md-dashboard-widget__news-dot');
    assert.notEqual(updatedDot, originalDot);
    assert.equal(f.window.document.activeElement, updatedDot);
    f.container.querySelector('.md-dashboard-widget__news-toggle').click();
    f.container.querySelector('.md-dashboard-widget__news-close').click();
    f.container.querySelectorAll('.md-dashboard-widget__news-dot')[2].focus();
    finish({ ok: true, json: async () => ({ news: [socialFeed.news[1]] }) }); await tick();
    assert.equal(f.window.document.activeElement, f.container.querySelector('.md-dashboard-widget__news-preview'));
    assert.equal(f.window.document.activeElement.hidden, false);
});

test('Expanded updates restore the focused card by identity or the visible close control when removed', async t => {
    for (const removed of [false, true]) {
        let finish;
        let calls = 0;
        const f = fixture(t, { config: socialConfig, fetchResponse: (url, options) => {
            if (options?.method === 'POST') return Promise.resolve({ ok: true, json: async () => true });
            if (++calls === 1) return Promise.resolve({ ok: true, json: async () => socialFeed });
            return new Promise(resolve => { finish = resolve; });
        } });
        acknowledgeRelease(f);
        f.render(); await tick();
        f.container.querySelector('.md-dashboard-widget__news-toggle').click();
        const original = f.container.querySelector('[data-news-id="youtube:video"]');
        original.focus();
        const news = removed ? [socialFeed.news[1]] : [socialFeed.news[1], { ...socialFeed.news[0], title: 'Updated video' }];
        finish({ ok: true, json: async () => ({ news }) }); await tick();
        const target = f.container.querySelector(removed ? '.md-dashboard-widget__news-close' : '[data-news-id="youtube:video"]');
        assert.equal(f.window.document.activeElement, target);
        assert.notEqual(target, original);
        assert.equal(target.hidden, false);
    }
});
