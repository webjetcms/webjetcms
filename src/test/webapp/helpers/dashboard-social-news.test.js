const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

const sourceRoot = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js');
const feedUrl = 'https://docs.webjetcms.sk/json/wjnews.sk.json';
const HOUR = 3600000;
const item = (id = 'youtube:one', overrides = {}) => ({ id, source: 'youtube', title: 'A video', perex: 'A useful description', link: 'https://www.youtube.com/watch?v=one', ...overrides });
const tick = () => new Promise(resolve => setImmediate(resolve));
const plain = value => JSON.parse(JSON.stringify(value));

/** Loads the real module with isolated browser storage, deterministic time and intercepted requests. */
function fixture(t) {
    const dom = new JSDOM('', { url: 'http://iwcm.interway.sk/admin/v9/' });
    const { window } = dom;
    window.currentUser = { adminSettings: {} };
    window.csrfToken = 'test-csrf';
    const notifications = [];
    window.WJ = { translate: key => key, notifyError: (...args) => notifications.push(args) };
    let now = Date.UTC(2026, 9, 18);
    const timers = new Map();
    let timerId = 0;
    window.setTimeout = callback => { timers.set(++timerId, callback); return timerId; };
    window.clearTimeout = id => timers.delete(id);
    const requests = [];
    let response = async () => ({ ok: true, json: async () => ({ news: [item()] }) });
    class Clock extends Date { static now() { return now; } }
    const scope = vm.createContext({ window, document: window.document, URL, TextEncoder, TextDecoder, Date: Clock,
        fetch: async (url, options) => {
            requests.push({ url, options });
            const result = await response(url, options);
            if (!result.text && result.json) result.text = async () => JSON.stringify(await result.json());
            return result;
        } });
    for (const file of ['release-news.js', 'dashboard/social-news.js']) {
        vm.runInContext(fs.readFileSync(path.join(sourceRoot, file), 'utf8').replace(/^import .+;\r?$/gm, '').replace(/^export /gm, ''), scope);
    }
    const controllers = [];
    const client = (options = {}) => {
        const controller = new window.AbortController();
        controllers.push(controller);
        return { controller, ...scope.createSocialNewsClient({ url: feedUrl, refreshHours: 8, signal: controller.signal, ...options }) };
    };
    t.after(() => { controllers.forEach(controller => controller.abort()); window.close(); });
    return { scope, window, client, requests, timers, notifications,
        respond: callback => { response = callback; }, advance: milliseconds => { now += milliseconds; } };
}

test('Feed configuration preserves cs filenames and local installations and disables unsafe settings', t => {
    const { scope } = fixture(t);
    for (const [language, expected] of [['sk', 'sk'], ['en', 'en'], ['cs', 'cs'], ['cz', 'cs'], ['de', 'sk']]) {
        const options = scope.socialNewsOptions({ overviewJsonUrl: 'https://docs.webjetcms.sk/json' }, language);
        assert.equal(options.url, `https://docs.webjetcms.sk/json/wjnews.${expected}.json`);
        assert.equal(options.refreshHours, 8);
    }
    assert.equal(scope.socialNewsOptions({ overviewJsonUrl: '/admin/v9/json/', socialRefreshHours: '2' }, 'en').url,
        'http://iwcm.interway.sk/admin/v9/json/wjnews.en.json');
    assert.equal(scope.socialNewsOptions({ overviewJsonUrl: 'http://news.intranet.test:8080/custom-news', socialRefreshHours: 8 }, 'cz').url,
        'http://news.intranet.test:8080/custom-news/wjnews.cs.json');
    assert.equal(scope.socialNewsOptions({ overviewJsonUrl: 'https://customer.example/news/' }, 'sk').url,
        'https://customer.example/news/wjnews.sk.json');
    for (const overviewJsonUrl of ['javascript:alert(1)', '//evil.test/', 'https://a:b@evil.test/', 'https://evil.test/\\x', 'https://evil.test/?x=1']) {
        assert.equal(scope.socialNewsOptions({ overviewJsonUrl }).enabled, false);
    }
    for (const socialRefreshHours of [0, -1, '', 'invalid', Infinity]) {
        assert.equal(scope.socialNewsOptions({ overviewJsonUrl: '/json/', socialRefreshHours }).enabled, false);
    }
});

test('Legacy releases and enriched entries normalize safely without changing title text or feed order', t => {
    const { scope, window } = fixture(t);
    const normalized = scope.normalizeFeed({ news: [
        item('youtube:one', { title: '<img src=x onerror=alert(1)>', perex: '**Bold**\\n\\nA &amp; B &lt;tag&gt; &#91;literal&#93;', image: 'https://images.example/video.jpg', publishedAt: '2026-10-18T08:00:00Z' }),
        { title: 'Verzia 2026.18', perex: 'Release **summary**.', link: 'https://docs.webjetcms.sk/latest/sk/CHANGELOG-2026?id=_202618' },
        item('bad', { link: 'javascript:alert(1)' }),
        item('youtube:one', { title: 'Duplicate' }),
        item('facebook:two', { source: 'facebook', image: 'http://unsafe.test/image.png' })
    ] }, feedUrl);
    assert.equal(normalized.length, 3);
    assert.equal(normalized[0].title, '<img src=x onerror=alert(1)>');
    assert.equal(normalized[0].description, 'Bold A & B <tag> [literal]');
    assert.equal(normalized[0].publishedAt, '2026-10-18T08:00:00.000Z');
    assert.equal(normalized[1].source, 'webjet');
    assert.equal(normalized[1].description, 'Release summary.');
    assert.equal(normalized[2].image, null);
    assert.equal(window.document.querySelector('img'), null);
    assert.throws(() => scope.normalizeFeed({ items: [] }, feedUrl), /Invalid news feed/);
    assert.throws(() => scope.normalizeFeed({ news: [item('x', { perex: null })] }, feedUrl), /no valid records/);
    assert.deepEqual(plain(scope.normalizeFeed({ news: [] }, feedUrl)), []);
});

test('Extended summaries remain plain text, preserve the short summary and respect Unicode limits', t => {
    const { scope, window } = fixture(t);
    const records = scope.normalizeFeed({ news: [
        item('rich', { perex: 'Short text', perexLong: '**Long** [text](https://example.test/) <img src=x> &lt;safe&gt; &amp;amp;' }),
        item('unicode', { perexLong: '🚀'.repeat(1001) })
    ] }, feedUrl);
    assert.equal(records[0].description, 'Short text');
    assert.equal(records[0].descriptionLong, 'Long text <safe> &amp;');
    assert.equal(Array.from(records[1].descriptionLong).length, 1000);
    assert.equal(records[1].descriptionLong, '🚀'.repeat(1000));
    assert.equal(window.document.querySelector('img'), null);
    for (const perexLong of [undefined, null, {}, 42, '', ' \n ', '<br>', '&nbsp;']) {
        const record = scope.normalizeFeed({ news: [item('legacy', { perex: 'Legacy text', perexLong })] }, feedUrl)[0];
        assert.equal(record.descriptionLong, '');
        assert.equal(record.description, 'Legacy text');
    }
});

test('Extended summaries roundtrip cache without double decoding and old cached records keep their reading identity', async t => {
    const first = fixture(t);
    first.respond(async () => ({ ok: true, json: async () => ({ news: [item('cached-rich', { perexLong: 'Long &amp;amp; &lt;tag&gt;' })] }) }));
    const reader = first.client();
    const records = await reader.refresh();
    assert.equal(reader.readCache()[0].descriptionLong, 'Long &amp; <tag>');
    const cacheKey = first.window.localStorage.key(0);
    const cached = first.window.localStorage.getItem(cacheKey);
    const second = fixture(t);
    second.window.localStorage.setItem(cacheKey, cached);
    assert.equal(second.client().readCache()[0].descriptionLong, 'Long &amp; <tag>');
    const legacyCache = JSON.parse(cached);
    delete legacyCache.items[0].descriptionLong;
    const legacy = fixture(t);
    legacy.window.localStorage.setItem(cacheKey, JSON.stringify(legacyCache));
    const oldRecords = legacy.client().readCache();
    assert.equal(oldRecords[0].descriptionLong, '');
    assert.equal(oldRecords[0].description, 'A useful description');
    const seen = first.scope.getExternalNewsState(records, feedUrl);
    first.window.currentUser.adminSettings[seen.key] = seen.value;
    const changed = first.scope.normalizeFeed({ news: [item('cached-rich', { perexLong: 'Changed extended text' })] }, feedUrl);
    assert.equal(first.scope.getExternalNewsState(changed, feedUrl).isNew, false);
    assert.equal(first.scope.getExternalNewsState(oldRecords, feedUrl).isNew, false);
});

test('Relative thumbnails follow the configured feed directory without changing link validation', async t => {
    const { scope, client, respond, requests } = fixture(t);
    const relative = 'social-media/images/autotest-thumbnail.jpg';
    for (const overviewJsonUrl of ['https://docs.webjetcms.sk/json/new/', '/custom/json/new/', 'http://news.intranet.test:8080/json/new/']) {
        const options = scope.socialNewsOptions({ overviewJsonUrl }, 'sk');
        const expected = new URL(relative, options.url).href;
        respond(async () => ({ ok: true, json: async () => ({ news: [item('relative', { image: relative })] }) }));
        const reader = client(options);
        assert.equal((await reader.refresh())[0].image, expected);
        assert.equal(requests.at(-1).url, options.url);
        assert.equal(reader.readCache()[0].image, expected, 'Cached absolute URLs retain the same validated image origin.');
        assert.equal((await reader.refresh())[0].image, expected);
        const normalized = scope.normalizeFeed({ news: [
            item('absolute', { image: 'https://legacy.example/image.jpg' }),
            item('root', { image: '/images/root.jpg' }),
            item('legacy'),
            item('relative-link', { link: 'articles/page.html', image: relative })
        ] }, options.url);
        assert.equal(normalized.length, 3, 'Image support must not allow previously invalid relative article links.');
        assert.equal(normalized[0].image, 'https://legacy.example/image.jpg');
        assert.equal(normalized[1].image, new URL('/images/root.jpg', options.url).href);
        assert.equal(normalized[2].image, null);
    }
});

test('Thumbnail resolution rejects ambiguous URLs and remote HTTP without discarding the text card', t => {
    const { scope } = fixture(t);
    for (const url of [feedUrl, 'http://news.intranet.test:8080/json/new/wjnews.sk.json']) {
        for (const image of ['javascript:alert(1)', 'data:image/png;base64,aaa', 'blob:https://docs.webjetcms.sk/image',
            '//evil.test/image.jpg', 'https://user:password@evil.test/image.jpg', 'https:evil.test/image.jpg',
            'social-media\\images\\image.jpg', 'social-media/images/\nimage.jpg', 'social-media/images/\u0000image.jpg',
            'http://unsafe.test/image.jpg', '', '?image.jpg', '#image.jpg']) {
            const records = scope.normalizeFeed({ news: [item('unsafe-image', { image })] }, url);
            assert.equal(records.length, 1);
            assert.equal(records[0].image, null, image);
        }
    }
});

test('Customer legacy feeds without optional fields remain readable and omit the installed release', t => {
    const { scope } = fixture(t);
    for (const locale of ['sk', 'cs', 'en']) {
        const legacy = { news: ['2026.18', '2026.0', '2025.40', '2025.18', '2025.0'].map(version => ({
            title: `${locale}: WebJET CMS ${version}`,
            perex: '**Release summary**\\nSecond paragraph\nThird paragraph',
            link: `https://docs.webjetcms.sk/latest/${locale}/CHANGELOG-${version.slice(0, 4)}?id=_${version.replace('.', '')}`
        })) };
        const records = scope.normalizeFeed(JSON.parse(JSON.stringify(legacy)), `http://news.intranet.test/json/wjnews.${locale}.json`);
        assert.equal(records.length, 5);
        assert.ok(records.every(record => record.description && !record.description.includes('\\n')));
        assert.ok(records.every(record => record.description === 'Release summary Second paragraph Third paragraph'
            && record.source === 'webjet' && record.image === null && record.publishedAt === null));
        const visible = scope.filterExternalNews(records, '2026.18');
        assert.equal(visible.length, records.length - 1);
        assert.ok(visible.every(record => !record.link.endsWith('id=_202618')));
    }
    const releases = scope.normalizeFeed({ news: [
        item('release', { source: 'webjet', link: 'https://docs.webjetcms.sk/latest/sk/CHANGELOG-2026?id=_202600' }),
        ...Array.from({ length: 8 }, (_, index) => item(`youtube:${index}`))
    ] }, feedUrl);
    assert.equal(scope.filterExternalNews(releases, '2026.0').length, 6);
    assert.equal(scope.filterExternalNews(releases, '2026.0')[0].id, 'youtube:0');
});

test('A custom local feed base is used by the request and supports the original JSON contract', async t => {
    const { scope, client, respond, requests } = fixture(t);
    const options = scope.socialNewsOptions({ overviewJsonUrl: 'http://news.intranet.test:8080/json' }, 'cz');
    respond(async () => ({ ok: true, json: async () => ({ news: [
        { title: 'Zákaznícka novinka', perex: '**Prvý bod**\\nDruhý bod\nTretí bod', link: '/clanky/novinka.html' }
    ] }) }));
    const records = await client(options).refresh();
    assert.equal(requests[0].url, 'http://news.intranet.test:8080/json/wjnews.cs.json');
    assert.equal(records[0].link, 'http://news.intranet.test:8080/clanky/novinka.html');
    assert.equal(records[0].description, 'Prvý bod Druhý bod Tretí bod');
    assert.equal(records[0].image, null);
    assert.equal(requests[0].options.credentials, 'omit');
});

test('Fresh cache avoids requests, exactly eight hours expires it, and force refresh bypasses the interval', async t => {
    const { client, requests, advance } = fixture(t);
    const reader = client();
    assert.equal(reader.readCache(), null);
    await reader.refresh();
    assert.equal(requests.length, 1);
    assert.equal(requests[0].options.credentials, 'omit');
    assert.equal(requests[0].options.referrerPolicy, 'no-referrer');
    assert.equal(requests[0].options.cache, 'no-cache');
    advance(8 * HOUR - 1);
    await reader.refresh();
    assert.equal(requests.length, 1);
    advance(1);
    await reader.refresh();
    assert.equal(requests.length, 2);
    await reader.refresh({ force: true });
    assert.equal(requests.length, 3);
    assert.equal(requests[2].options.cache, 'no-cache');
});

test('Oversized declared, chunked and text responses cannot replace the last valid feed', async t => {
    const { client, respond } = fixture(t);
    const reader = client();
    await reader.refresh();
    let cancelled = false;
    respond(async () => ({ ok: true, headers: { get: () => String(256 * 1024 + 1) },
        body: { cancel: async () => { cancelled = true; } }, text: async () => { throw new Error('Must reject before reading'); } }));
    await assert.rejects(reader.refresh({ force: true }), /size limit/);
    assert.equal(cancelled, true);
    const tooLarge = JSON.stringify({ news: [item('oversized', { perex: 'x'.repeat(256 * 1024) })] });
    respond(async () => new Response(tooLarge));
    await assert.rejects(reader.refresh({ force: true }), /size limit/);
    respond(async () => ({ ok: true, text: async () => JSON.stringify({ news: [item('unicode', { perex: '€'.repeat(100000) })] }) }));
    await assert.rejects(reader.refresh({ force: true }), /size limit/);
    assert.equal(reader.readCache()[0].id, 'youtube:one');
    respond(async () => new Response(JSON.stringify({ news: [item('valid-stream')] })));
    assert.equal((await reader.refresh({ force: true }))[0].id, 'valid-stream');
});

test('Disabled readers ignore populated cache and never issue feed requests', async t => {
    const { client, requests } = fixture(t);
    await client().refresh();
    const disabled = client({ refreshHours: 0 });
    assert.equal(disabled.readCache(), null);
    assert.deepEqual(plain(await disabled.refresh({ force: true })), []);
    assert.equal(requests.length, 1);
});

test('Malformed and failed refreshes keep the successful cache and timestamp', async t => {
    const { client, respond, requests, advance, window } = fixture(t);
    const reader = client();
    await reader.refresh();
    const key = window.localStorage.key(0);
    const original = window.localStorage.getItem(key);
    advance(9 * HOUR);
    respond(async () => ({ ok: true, json: async () => ({ news: [item('x', { link: '//unsafe.test/' })] }) }));
    await assert.rejects(reader.refresh(), /no valid records/);
    assert.equal(reader.readCache()[0].id, 'youtube:one');
    assert.equal(window.localStorage.getItem(key), original);
    respond(async () => ({ ok: false }));
    await assert.rejects(reader.refresh(), /unavailable/);
    assert.equal(requests.length, 3);
    assert.equal(window.localStorage.getItem(key), original);
});

test('Cached text is decoded once and blocked localStorage retains successful data in memory', async t => {
    const { client, respond, window } = fixture(t);
    respond(async () => ({ ok: true, json: async () => ({ news: [item('x', { perex: '&amp;lt;tag&amp;gt; &lt;visible&gt;' })] }) }));
    Object.defineProperty(window, 'localStorage', { get() { throw new Error('Storage denied'); } });
    const reader = client();
    const records = await reader.refresh();
    assert.equal(records[0].description, '&lt;tag&gt; <visible>');
    assert.equal(reader.readCache()[0].description, records[0].description);
});

test('Untrusted storage is revalidated and malformed cache does not prevent a network recovery', async t => {
    const { client, window } = fixture(t);
    const key = 'webjet.externalNews.v1';
    window.localStorage.setItem(key, JSON.stringify({ version: 1, url: feedUrl, fetchedAt: Date.UTC(2026, 9, 18), items: [
        { ...item(), description: 'Unsafe', link: 'javascript:alert(1)' }
    ] }));
    const reader = client();
    assert.equal(reader.readCache(), null);
    assert.equal((await reader.refresh())[0].link, 'https://www.youtube.com/watch?v=one');
});

test('One storage key survives reloads and is replaced when the language or feed URL changes', async t => {
    const key = 'webjet.externalNews.v1';
    const first = fixture(t);
    await first.client().refresh();
    assert.equal(first.window.localStorage.length, 1);
    assert.equal(first.window.localStorage.key(0), key);
    const saved = first.window.localStorage.getItem(key);
    const reloaded = fixture(t);
    reloaded.window.localStorage.setItem(key, saved);
    assert.equal((await reloaded.client().refresh())[0].id, 'youtube:one');
    assert.equal(reloaded.requests.length, 0);

    for (const url of [feedUrl.replace('.sk.', '.en.'), 'https://other.example/json/wjnews.sk.json']) {
        const changed = fixture(t);
        changed.window.localStorage.setItem(key, saved);
        const reader = changed.client({ url });
        assert.equal(reader.readCache(), null);
        await reader.refresh();
        assert.equal(changed.requests.length, 1);
        assert.equal(changed.requests[0].url, url);
        assert.equal(changed.window.localStorage.length, 1);
        assert.equal(JSON.parse(changed.window.localStorage.getItem(key)).url, url);
    }
});

test('Overlapping clients share one request while aborting one subscriber leaves the other active', async t => {
    const { client, respond, requests, timers } = fixture(t);
    let finish;
    respond(() => new Promise(resolve => { finish = resolve; }));
    const first = client();
    const second = client();
    const one = first.refresh();
    const two = second.refresh({ force: true });
    assert.equal(requests.length, 1);
    first.controller.abort();
    await assert.rejects(one, { name: 'AbortError' });
    assert.equal(requests[0].options.signal.aborted, false);
    finish({ ok: true, json: async () => ({ news: [item()] }) });
    assert.equal((await two)[0].id, 'youtube:one');
    assert.equal(timers.size, 0);
});

test('Timeout and final subscriber abort reject promptly and late fetch results never poison cache', async t => {
    for (const timedOut of [true, false]) {
        const { client, respond, timers } = fixture(t);
        let finish;
        respond(() => new Promise(resolve => { finish = resolve; }));
        const reader = client();
        const pending = reader.refresh();
        if (timedOut) [...timers.values()][0]();
        else reader.controller.abort();
        await assert.rejects(pending, { name: 'AbortError' });
        finish({ ok: true, json: async () => ({ news: [item('late')] }) });
        await tick();
        assert.equal(client().readCache(), null);
        assert.equal(timers.size, 0);
    }
});

test('Acknowledgement is account scoped, language independent and separate from release badge state', async t => {
    const { scope, window, respond, requests } = fixture(t);
    respond(async () => ({ ok: true, json: async () => true }));
    window.currentUser.adminSettings['dashboard.news.sk'] = 'original release record';
    const sk = scope.normalizeFeed({ news: [{ title: 'Verzia', perex: 'Popis', link: 'https://docs.webjetcms.sk/latest/sk/CHANGELOG-2026?id=_202618' }] }, feedUrl);
    const enUrl = feedUrl.replace('.sk.', '.en.');
    const en = scope.normalizeFeed({ news: [{ title: 'Version', perex: 'Description', link: 'https://docs.webjetcms.sk/latest/en/CHANGELOG-2026?id=_202618' }] }, enUrl);
    assert.equal(scope.getExternalNewsState(sk, feedUrl).isNew, true);
    assert.equal(await scope.saveExternalNewsSeen(sk, feedUrl), true);
    assert.equal(scope.getExternalNewsState(en, enUrl).isNew, false);
    assert.equal(scope.getExternalNewsState(en, 'https://other.test/json/wjnews.en.json').isNew, true);
    assert.equal(window.currentUser.adminSettings['dashboard.news.sk'], 'original release record');
    assert.equal(requests[0].options.headers['X-CSRF-Token'], 'test-csrf');
    assert.equal(requests[0].options.credentials, 'same-origin');
    window.currentUser = { adminSettings: {} };
    assert.equal(scope.getExternalNewsState(en, enUrl).isNew, true);
});

test('Concurrent acknowledgements merge, unchanged items do not save again, and records stay under 2000 characters', async t => {
    const { scope, respond, requests, window } = fixture(t);
    respond(async () => ({ ok: true, json: async () => true }));
    const first = scope.normalizeFeed({ news: [item('first')] }, feedUrl);
    const second = scope.normalizeFeed({ news: [item('second')] }, feedUrl);
    await Promise.all([scope.saveExternalNewsSeen(first, feedUrl), scope.saveExternalNewsSeen(second, feedUrl)]);
    assert.equal(scope.getExternalNewsState([...first, ...second], feedUrl).isNew, false);
    await scope.saveExternalNewsSeen([...first, ...second], feedUrl);
    assert.equal(requests.length, 2);
    const many = Array.from({ length: 150 }, (_, index) => ({ id: `id:${index}` }));
    await scope.saveExternalNewsSeen(many, feedUrl);
    const state = scope.getExternalNewsState([], feedUrl);
    assert.equal(JSON.parse(window.currentUser.adminSettings[state.key]).seen.length, 100);
    assert.ok(window.currentUser.adminSettings[state.key].length < 2000);
});

test('Failed acknowledgement keeps the badge unread and can be retried', async t => {
    const { scope, respond, window, notifications } = fixture(t);
    const items = scope.normalizeFeed({ news: [item()] }, feedUrl);
    respond(async () => ({ ok: false }));
    assert.equal(await scope.saveExternalNewsSeen(items, feedUrl), false);
    assert.equal(scope.getExternalNewsState(items, feedUrl).isNew, true);
    assert.equal(Object.keys(window.currentUser.adminSettings).length, 0);
    assert.equal(notifications.length, 1);
    respond(async () => ({ ok: true, json: async () => true }));
    assert.equal(await scope.saveExternalNewsSeen(items, feedUrl), true);
    assert.equal(scope.getExternalNewsState(items, feedUrl).isNew, false);
});

test('Identical simultaneous failed acknowledgements share one write and one notification', async t => {
    const { scope, respond, requests, notifications } = fixture(t);
    const items = scope.normalizeFeed({ news: [item('one'), item('two')] }, feedUrl);
    let finish;
    respond(() => new Promise(resolve => { finish = resolve; }));
    const first = scope.saveExternalNewsSeen(items, feedUrl);
    const second = scope.saveExternalNewsSeen([...items].reverse(), feedUrl);
    const third = scope.saveExternalNewsSeen(items, feedUrl.replace('.sk.', '.en.'));
    assert.equal(first, second);
    assert.equal(first, third);
    await tick();
    assert.equal(requests.length, 1);
    finish({ ok: false });
    assert.deepEqual(await Promise.all([first, second, third]), [false, false, false]);
    assert.equal(notifications.length, 1);
    assert.equal(scope.getExternalNewsState(items, feedUrl).isNew, true);
    respond(async () => ({ ok: true, json: async () => true }));
    assert.equal(await scope.saveExternalNewsSeen(items, feedUrl), true);
    assert.equal(requests.length, 2);
});
