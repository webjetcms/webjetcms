const fs = require('fs');
const path = require('path');

Feature('admin.dashboard.social-news');

const news = '.md-dashboard__news';
const toggle = '.md-dashboard-widget__news-toggle';
const cards = '.md-dashboard-widget__news-card';
const images = '.md-dashboard-widget__news-image';
const badge = '.md-dashboard-widget__news-badge';
const external = '.md-dashboard-widget__news-external';
const preferenceRoute = '**/admin/rest/admin-settings/';
const feedRoute = '**/wjnews.*.json';
const imageRoutes = ['https://news.example/images/**', '**/social-media/images/**'];
const thumbnail = fs.readFileSync(path.join(__dirname, '../../../../../../docs/sk/redactor/admin/dashboard.png'));
let fixture;
let preferenceWrites;
let sequence = 0;

function socialItem(source, number, options = {}) {
    return {
        id: `${source}:autotest-${number}`, source,
        title: `autotest ${source} ${number}`,
        perex: `autotest ${source} description ${number}`,
        link: source === 'youtube' ? `https://www.youtube.com/watch?v=autotest-${number}` : `https://www.facebook.com/100066653358802/posts/autotest-${number}`,
        publishedAt: `2026-10-${String(18 - number).padStart(2, '0')}T09:00:00Z`,
        image: `https://news.example/images/${source}-${number}.png`,
        ...options
    };
}

/** Configures only the current browser component; all preference writes are intercepted. */
async function configureFeed(I, refreshDays = 3) {
    await I.executeScript(({ baseUrl, refreshDays }) => {
        const dashboard = document.querySelector('webjet-overview-dashboard');
        dashboard.configure({ data: dashboard.data, labels: dashboard.labels,
            config: { ...dashboard.config, overviewJsonUrl: baseUrl, socialRefreshDays: refreshDays } });
    }, { baseUrl: fixture.baseUrl, refreshDays });
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
}

/** Keeps the release badge expired so assertions isolate external unread announcements. */
async function openFixture(I, items, status = 200, baseUrl) {
    fixture = { baseUrl: baseUrl || `https://news.example/autotest-${Date.now()}-${++sequence}/`, items,
        requests: [], imageRequests: [], status, pending: null };
    await I.amOnPage('/admin/v9/');
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await I.waitForFunction(() => Boolean(window.currentUser.adminSettings[`dashboard.news.${window.userLng}`]), 10);
    await I.executeScript(() => {
        const key = `dashboard.news.${window.userLng}`;
        const state = JSON.parse(window.currentUser.adminSettings[key]);
        state.firstSeen = Date.now() - 31 * 86400000;
        window.currentUser.adminSettings[key] = JSON.stringify(state);
    });
    await configureFeed(I);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
}

Before(async ({ I, login }) => {
    fixture = null;
    preferenceWrites = [];
    await I.mockRoute(preferenceRoute, route => {
        preferenceWrites.push(route.request().postDataJSON());
        return route.fulfill({ status: 200, contentType: 'application/json', body: 'true' });
    });
    await I.mockRoute(feedRoute, async route => {
        const current = fixture;
        if (!current || !route.request().url().startsWith(current.baseUrl)) {
            return route.fulfill({ status: 200, contentType: 'application/json',
                headers: { 'Access-Control-Allow-Origin': '*' }, body: '{"news":[]}' });
        }
        current.requests.push({ url: route.request().url(), headers: route.request().headers() });
        const items = current.pending ? await current.pending : current.items;
        return route.fulfill({ status: current.status, contentType: 'application/json',
            headers: { 'Access-Control-Allow-Origin': '*' }, body: JSON.stringify({ news: items }) });
    });
    for (const imageRoute of imageRoutes) {
        await I.mockRoute(imageRoute, route => {
            fixture?.imageRequests.push({ url: route.request().url(), headers: route.request().headers() });
            if (route.request().url().includes('missing')) return route.fulfill({ status: 404,
                headers: { 'Access-Control-Allow-Origin': '*' }, contentType: 'text/plain', body: 'Missing autotest image' });
            return route.fulfill({ status: 200,
                headers: { 'Access-Control-Allow-Origin': '*' }, contentType: 'image/png', body: thumbnail });
        });
    }
    login('admin');
});

Scenario('Local highlights and six safe social cards combine with lazy images and responsive layout', async ({ I, a11y }) => {
    await I.resizeWindow(1440, 1600);
    await openFixture(I, [
        { title: 'autotest duplicate installed release', perex: 'Already displayed locally.',
            link: 'https://docs.webjetcms.sk/latest/sk/CHANGELOG-2026?id=_202618' },
        socialItem('youtube', 1, { image: 'https://news.example/images/missing.png' }),
        socialItem('facebook', 1), socialItem('youtube', 2), socialItem('facebook', 2),
        socialItem('youtube', 3), socialItem('facebook', 3),
        { title: 'autotest unsafe link', perex: 'Invalid link.', link: 'javascript:alert(1)' },
        { title: 'autotest seventh item', perex: 'Must not fit.', link: 'https://docs.webjetcms.sk/latest/sk/CHANGELOG-2025?id=_202518' }
    ]);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-dot', 7);
    await I.dontSeeElementInDOM(images);
    await I.assertEqual(fixture.requests.length, 1);
    await I.assertEqual(fixture.imageRequests.length, 0, 'Collapsed news must not contact image servers.');
    await I.assertEqual(fixture.requests[0].headers.referer, undefined);
    await I.assertEqual(fixture.requests[0].headers.cookie, undefined);
    await I.seeElement(badge);
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(6)');
    await I.see('YouTube: autotest youtube 1', '.md-dashboard-widget__news-title');
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(7)');
    await I.see('Facebook: autotest facebook 1', '.md-dashboard-widget__news-title');
    await I.clickCss(toggle);
    await I.waitForVisible('.md-dashboard__hero.is-news-expanded', 10);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-highlights li', 5);
    await I.seeNumberOfVisibleElements(cards, 6);
    await I.dontSee('autotest duplicate installed release', news);
    await I.dontSee('autotest seventh item', news);
    await I.dontSee('autotest unsafe link', news);
    await I.waitForFunction(() => !document.querySelector('.md-dashboard-widget__news-card:first-child img'), 10);
    await I.see('autotest youtube 1', `${cards}:first-child`);
    await I.assertTrue(await I.executeScript(() => {
        const highlights = document.querySelector('.md-dashboard-widget__news-highlights');
        const first = document.querySelector('.md-dashboard-widget__news-card');
        return Boolean(highlights.compareDocumentPosition(first) & Node.DOCUMENT_POSITION_FOLLOWING);
    }), 'The installed release must remain before the external cards.');
    await I.assertEqual(await I.grabAttributeFrom(`${cards}:nth-child(2)`, 'rel'), 'noopener noreferrer');
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-channels a', 2);
    await I.waitForElement(`${badge}[hidden]`, 10);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
    await I.assertEqual(fixture.requests.length, 2, 'Expansion must revalidate the feed.');
    await I.assertTrue(fixture.imageRequests.length > 0);
    await I.assertTrue(fixture.imageRequests.every(request => !request.headers.referer && !request.headers.cookie));
    await a11y.check(news);
    await I.saveScreenshot('dashboard-social-news-desktop.png', true);

    for (const width of [1024, 390]) {
        await I.resizeWindow(width, 1100);
        await I.assertTrue(await I.executeScript(() => {
            const hero = document.querySelector('.md-dashboard__hero').getBoundingClientRect();
            return document.documentElement.scrollWidth <= window.innerWidth
                && [...document.querySelectorAll('.md-dashboard-widget__news-card')].every(card => {
                    const bounds = card.getBoundingClientRect();
                    return bounds.left >= hero.left && bounds.right <= hero.right;
                });
        }), `Social cards must fit at ${width}px.`);
        if (width === 390) {
            await I.assertTrue(await I.executeScript(() => {
                const bounds = [...document.querySelectorAll('.md-dashboard-widget__news-card')].map(card => card.getBoundingClientRect());
                return bounds.every((item, index) => Math.abs(item.left - bounds[0].left) < 1 && (index === 0 || item.top >= bounds[index - 1].bottom));
            }), 'Mobile cards must form one vertical column.');
            await I.saveScreenshot('dashboard-social-news-mobile.png', true);
            await a11y.check(news);
            await I.executeScript(() => {
                const scrollbar = window.scrollbarMain;
                scrollbar.setMomentum(0, 0);
                scrollbar.update();
                scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('.md-dashboard-widget__news-cards').getBoundingClientRect().top - 80);
            });
            await I.saveScreenshot('dashboard-social-news-mobile-cards.png', true);
        }
    }
    await I.wjSetDefaultWindowSize();
});

Scenario('Relative thumbnails follow nested configured feed directories and keep legacy HTTPS images', async ({ I }) => {
    await I.resizeWindow(1440, 1600);
    for (const origin of ['https://news.example', 'http://news.intranet.test:8080']) {
        const baseUrl = `${origin}/json/new/autotest-${Date.now()}-${++sequence}/`;
        const relativeImage = 'social-media/images/autotest-relative.png';
        await openFixture(I, [socialItem('youtube', 1, { image: relativeImage }), socialItem('facebook', 1)], 200, baseUrl);
        await I.assertEqual(fixture.imageRequests.length, 0);
        await I.clickCss(toggle);
        await I.waitForVisible(`${cards}:first-child ${images}`, 10);
        await I.waitForFunction(() => [...document.querySelectorAll('.md-dashboard-widget__news-image')]
            .every(image => image.complete && image.naturalWidth > 0), 10);
        await I.assertEqual(await I.grabAttributeFrom(`${cards}:first-child ${images}`, 'src'), `${baseUrl}${relativeImage}`);
        await I.assertEqual(await I.grabAttributeFrom(`${cards}:nth-child(2) ${images}`, 'src'), 'https://news.example/images/facebook-1.png');
        await I.assertTrue(fixture.imageRequests.some(request => request.url === `${baseUrl}${relativeImage}`));
        await I.assertTrue(fixture.imageRequests.every(request => !request.headers.referer && !request.headers.cookie));
    }
    await I.wjSetDefaultWindowSize();
});

Scenario('Fresh cache avoids background requests while expansion refreshes and records account reading', async ({ I }) => {
    await openFixture(I, [socialItem('youtube', 1), socialItem('facebook', 1)]);
    await I.seeElement(badge);
    await I.assertEqual(preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.')).length, 0);
    const cache = await I.executeScript(() => Object.keys(localStorage)
        .filter(key => key.startsWith('webjet.externalNews.'))
        .map(key => JSON.parse(localStorage.getItem(key)))
        .find(value => value.items?.some(item => item.title === 'autotest youtube 1')));
    await I.assertEqual(cache.version, 1);
    await I.assertTrue(cache.fetchedAt > Date.now() - 60000);
    await configureFeed(I);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
    await I.assertEqual(fixture.requests.length, 1, 'Recreating the dashboard must use its fresh cache.');
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-dot', 7);
    fixture.items = [socialItem('youtube', 2), socialItem('facebook', 1)];
    await I.clickCss(toggle);
    await I.waitForText('autotest youtube 2', 10, cards);
    await I.waitForElement(`${badge}[hidden]`, 10);
    await I.assertEqual(fixture.requests.length, 2);
    await I.assertTrue(preferenceWrites.some(write => write.label.startsWith('dashboard.externalNews.')));
    await I.assertTrue(await I.executeScript(() => JSON.parse(window.currentUser.adminSettings[`dashboard.news.${window.userLng}`]).firstSeen < Date.now() - 30 * 86400000),
        'Reading social cards must not change the existing release window.');

    await I.pressKey('Escape');
    await I.waitForVisible('.md-dashboard__welcome', 10);
    const cacheBeforeFailure = await I.executeScript(() => Object.fromEntries(Object.entries(localStorage)
        .filter(([key]) => key.startsWith('webjet.externalNews.'))));
    fixture.status = 503;
    await I.clickCss(toggle);
    await I.waitForElement(`${external}[aria-busy="false"][hidden]`, 10);
    await I.dontSeeElementInDOM(cards);
    await I.dontSeeElementInDOM(images);
    await I.dontSeeElementInDOM('.md-dashboard-widget__news-feed-status');
    await I.dontSeeElement(badge);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-highlights li', 5);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-channels a', 2);
    await I.assertDeepEqual(await I.executeScript(() => Object.fromEntries(Object.entries(localStorage)
        .filter(([key]) => key.startsWith('webjet.externalNews.')))), cacheBeforeFailure,
    'A failed refresh must preserve cache internally without displaying stale cards.');
    await I.assertEqual(fixture.requests.length, 3);
    await I.saveScreenshot('dashboard-social-news-unavailable.png');
    await I.pressKey('Escape');
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-dot', 5);
    fixture.status = 200;
    await I.clickCss(toggle);
    await I.waitForText('autotest youtube 2', 10, cards);
    await I.seeNumberOfVisibleElements(cards, 2);
});

Scenario('Unavailable and empty feeds quietly leave the local release and channel links', async ({ I }) => {
    await openFixture(I, [], 404);
    await I.dontSeeElement(badge);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-dot', 5);
    await I.clickCss(toggle);
    await I.waitForElement(`${external}[aria-busy="false"][hidden]`, 10);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-highlights li', 5);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-channels a', 2);
    await I.dontSeeElementInDOM(cards);
    await I.dontSeeElementInDOM(images);
    await I.dontSeeElementInDOM('.md-dashboard-widget__news-feed-status');
    await I.pressKey('Escape');
    fixture.status = 200;
    await I.clickCss(toggle);
    await I.waitForElement(`${external}[aria-busy="false"][hidden]`, 10);
    await I.dontSeeElementInDOM(cards);
    await I.dontSeeElementInDOM('.md-dashboard-widget__news-feed-status');
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-channels a', 2);
    await I.assertEqual(fixture.imageRequests.length, 0);
});

Scenario('A feed response arriving after collapse remains unread until the next expansion', async ({ I }) => {
    await openFixture(I, [socialItem('youtube', 1)]);
    await I.clickCss(toggle);
    await I.waitForElement(`${badge}[hidden]`, 10);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
    await I.pressKey('Escape');
    await I.waitForVisible('.md-dashboard__welcome', 10);
    const writesBefore = preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.')).length;
    let releaseResponse;
    fixture.pending = new Promise(resolve => { releaseResponse = resolve; });
    await I.clickCss(toggle);
    await I.waitForElement(`${external}[aria-busy="true"]`, 10);
    await I.pressKey('Escape');
    await I.waitForVisible('.md-dashboard__welcome', 10);
    fixture.items = [socialItem('youtube', 2)];
    releaseResponse(fixture.items);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
    await I.waitForVisible(badge, 10);
    await I.assertEqual(preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.')).length, writesBefore);
    fixture.pending = null;
    await I.clickCss(toggle);
    await I.waitForText('autotest youtube 2', 10, cards);
    await I.waitForElement(`${badge}[hidden]`, 10);
});

Scenario('Zero and negative intervals suppress cached cards, images, rotation and unread badges', async ({ I }) => {
    await openFixture(I, [socialItem('youtube', 1), socialItem('facebook', 1)]);
    await I.seeElement(badge);
    const requestsBefore = fixture.requests.length;
    for (const interval of [0, -1]) {
        await configureFeed(I, interval);
        await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-dot', 5);
        await I.dontSeeElement(badge);
        await I.clickCss(toggle);
        await I.waitForVisible('.md-dashboard__hero.is-news-expanded', 10);
        await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-highlights li', 5);
        await I.dontSeeElementInDOM(cards);
        await I.dontSeeElementInDOM(images);
        await I.dontSeeElement('.md-dashboard-widget__news-external');
        await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-channels a', 2);
        await I.assertEqual(fixture.requests.length, requestsBefore);
        await I.assertEqual(fixture.imageRequests.length, 0);
    }
});

Scenario('Legacy records load from a custom HTTP base and keep acknowledgement across languages', async ({ I }) => {
    await openFixture(I, [
        { title: 'autotest legacy release', perex: '**Prvý bod**\\nDruhý bod\nTretí bod', link: 'https://docs.webjetcms.sk/latest/sk/CHANGELOG-2025?id=_202518' },
        socialItem('youtube', 1, { image: 'javascript:alert(1)', perex: '<img src="https://news.example/images/unsafe.png" onerror="alert(1)">Plain autotest text' })
    ], 200, `http://news.intranet.test:8080/autotest-${Date.now()}/`);
    await I.assertTrue(fixture.requests[0].url.startsWith('http://news.intranet.test:8080/'));
    await I.clickCss(toggle);
    await I.waitForText('Prvý bod Druhý bod Tretí bod', 10, cards);
    await I.see('Plain autotest text', cards);
    await I.dontSeeElementInDOM(images);
    await I.waitForElement(`${badge}[hidden]`, 10);
    await I.assertEqual(fixture.imageRequests.length, 0);
    const writesBefore = preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.')).length;
    fixture.items[0] = { ...fixture.items[0], title: 'autotest české vydání', link: 'https://docs.webjetcms.sk/latest/cs/CHANGELOG-2025?id=_202518' };
    await I.executeScript(() => {
        const originalLanguage = window.userLng;
        window.userLng = 'cz';
        window.currentUser.adminSettings['dashboard.news.cz'] = window.currentUser.adminSettings[`dashboard.news.${originalLanguage}`];
    });
    await configureFeed(I);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
    await I.dontSeeElement(badge);
    await I.assertTrue(fixture.requests.at(-1).url.endsWith('/wjnews.cs.json'));
    await I.clickCss(toggle);
    await I.waitForText('autotest české vydání', 10, cards);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
    await I.assertEqual(preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.')).length, writesBefore,
        'The same IDs and legacy changelog across languages must remain read.');
});
