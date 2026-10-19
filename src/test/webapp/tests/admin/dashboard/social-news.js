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
async function configureFeed(I, refreshHours = 8) {
    await I.executeScript(({ baseUrl, refreshHours }) => {
        const dashboard = document.querySelector('webjet-overview-dashboard');
        dashboard.configure({ data: dashboard.data, labels: dashboard.labels,
            config: { ...dashboard.config, overviewJsonUrl: baseUrl, socialRefreshHours: refreshHours } });
    }, { baseUrl: fixture.baseUrl, refreshHours });
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
}

/** Acknowledges the local release before configuring the feed so assertions isolate external unread announcements. */
async function openFixture(I, items, status = 200, baseUrl) {
    fixture = { baseUrl: baseUrl || `https://news.example/autotest-${Date.now()}-${++sequence}/`, items,
        requests: [], imageRequests: [], status, pending: null };
    await I.amOnPage('/admin/v9/');
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await I.clickCss(toggle);
    await I.waitForFunction(() => JSON.parse(window.currentUser.adminSettings[`dashboard.news.${window.userLng}`] || 'null')?.readAt > 0, 10);
    await I.clickCss('.md-dashboard-widget__news-close');
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
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-dot', 10);
    await I.dontSeeElementInDOM(images);
    await I.assertEqual(fixture.requests.length, 1);
    await I.assertEqual(fixture.imageRequests.length, 0, 'Local release slides must not preload social thumbnails.');
    await I.assertEqual(fixture.requests[0].headers.referer, undefined);
    await I.assertEqual(fixture.requests[0].headers.cookie, undefined);
    await I.seeElement(badge);
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(6)');
    await I.see('autotest youtube 1', '.md-dashboard-widget__news-title');
    await I.see('YouTube', '.md-dashboard-widget__news-source');
    await I.waitForFunction(() => !document.querySelector('.md-dashboard-widget__news-preview-image'), 10);
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(7)');
    await I.see('autotest facebook 1', '.md-dashboard-widget__news-title');
    await I.see('Facebook', '.md-dashboard-widget__news-source');
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(10)');
    await I.see('autotest youtube 3', '.md-dashboard-widget__news-title');
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

Scenario('Collapsed social previews keep the layout, load only the current image and distinguish the source', async ({ I, a11y }) => {
    const preview = '.md-dashboard-widget__news-preview';
    const thumbnail = '.md-dashboard-widget__news-preview-image';
    const source = '.md-dashboard-widget__news-source';
    const version = '.md-dashboard-widget__news-version';
    await I.resizeWindow(1202, 900);
    const videoTitle = 'autotest Ako nastaviť štýly textu a kontajnera | WebJET CMS';
    await openFixture(I, [socialItem('youtube', 1, { image: 'social-media/images/video.png', title: videoTitle,
        perex: 'Nastavte vzhľad textu a jednotlivých častí stránky pomocou ponuky Štýly v editore WebJET CMS.' }),
        socialItem('facebook', 1), socialItem('youtube', 2), socialItem('facebook', 2), socialItem('youtube', 3), socialItem('facebook', 3)]);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-dot', 10);
    await I.clickCss('.md-dashboard-widget__news-dot:first-child');
    await I.seeElement(version);
    await I.dontSeeElement(source);
    await I.dontSeeElementInDOM(thumbnail);
    await I.assertEqual(fixture.imageRequests.length, 0);
    const initialHeight = await I.executeScript(() => document.querySelector('.md-dashboard-widget__news-preview').getBoundingClientRect().height);
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(6)');
    await I.waitForFunction(() => {
        const image = document.querySelector('.md-dashboard-widget__news-preview-image');
        return image?.complete && image.naturalWidth > 0;
    }, 10);
    await I.see('YouTube', source);
    await I.seeElement(`${source} .ti-brand-youtube`);
    await I.dontSeeElement(version);
    await I.assertEqual(await I.grabTextFrom('.md-dashboard-widget__news-title'), videoTitle);
    await I.assertEqual(await I.grabAttributeFrom(thumbnail, 'src'), `${fixture.baseUrl}social-media/images/video.png`);
    await I.assertTrue(fixture.imageRequests.every(request => request.url.endsWith('/social-media/images/video.png')));
    await I.assertEqual(preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.')).length, 0);
    await I.seeElement(badge);
    for (const width of [1202, 390]) {
        await I.resizeWindow(width, 900);
        if (width < 768) await I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
        const layout = await I.executeScript(() => {
            const preview = document.querySelector('.md-dashboard-widget__news-preview').getBoundingClientRect();
            const image = document.querySelector('.md-dashboard-widget__news-preview-image').getBoundingClientRect();
            const title = document.querySelector('.md-dashboard-widget__news-title').getBoundingClientRect();
            const description = document.querySelector('.md-dashboard-widget__news-description').getBoundingClientRect();
            const navigation = document.querySelector('.md-dashboard-widget__news-navigation').getBoundingClientRect();
            const dots = [...document.querySelectorAll('.md-dashboard-widget__news-dot')].map(dot => dot.getBoundingClientRect());
            return { height: preview.height, imageHeight: image.height,
                navigationFits: navigation.height === 24 && dots.every(dot => dot.left >= preview.left && dot.right <= preview.right),
                fits: image.left >= preview.left && image.right < title.left && title.right <= preview.right
                && image.height <= preview.height && description.top >= title.bottom
                && description.bottom <= preview.bottom && description.right <= preview.right && document.documentElement.scrollWidth <= window.innerWidth };
        });
        await I.assertEqual(layout.height, initialHeight, 'A thumbnail must not change the preview height.');
        await I.assertEqual(layout.imageHeight, 64, 'The thumbnail must fill the height of three text lines.');
        await I.assertTrue(layout.navigationFits, `Ten navigation dots must fit on one row at ${width}px.`);
        await I.assertTrue(layout.fits, `The thumbnail must sit to the left of the title and description at ${width}px.`);
        await a11y.check(news);
        await I.saveScreenshot(`dashboard-social-preview-${width === 390 ? 'mobile' : 'desktop'}.png`, true);
    }
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(7)');
    await I.see('Facebook', source);
    await I.seeElement(`${source} .ti-brand-facebook`);
    await I.assertEqual(await I.grabAttributeFrom(thumbnail, 'src'), 'https://news.example/images/facebook-1.png');
    await I.assertTrue(fixture.imageRequests.every(request => !request.headers.referer && !request.headers.cookie));
    await I.clickCss(preview);
    await I.waitForVisible('.md-dashboard__hero.is-news-expanded', 10);
    await I.seeElement(version);
    await I.dontSeeElement(source);
    await I.dontSeeElementInDOM(thumbnail);
    await I.pressKey('Escape');
    await I.see('Facebook', source);
    await I.seeElement(thumbnail);
    await I.clickCss('.md-dashboard-widget__news-dot:first-child');
    await I.seeElement(version);
    await I.dontSeeElement(source);
    await I.dontSeeElementInDOM(thumbnail);
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

Scenario('Extended summaries fill available card height while the carousel stays short', async ({ I }) => {
    const longText = 'WebJET CMS prináša novinky pre redaktorov aj správcov. Podrobný popis vysvetľuje možnosti úpravy stránok, správu obsahu a nastavenie aplikácií. '.repeat(6).trim();
    await I.resizeWindow(1440, 1600);
    await openFixture(I, [
        socialItem('youtube', 1, { perex: 'autotest short preview', perexLong: longText }),
        socialItem('facebook', 1, { title: 'autotest Dlhší názov novinky opisuje nové možnosti správy obsahu, úpravy stránok a nastavenia aplikácií vo WebJET CMS.', perexLong: longText }),
        socialItem('youtube', 2, { perexLong: longText }),
        socialItem('facebook', 2, { image: null, perexLong: longText }),
        socialItem('youtube', 3, { image: 'https://news.example/images/missing.png', perexLong: longText }),
        socialItem('facebook', 3, { perexLong: longText })
    ]);
    await I.clickCss('.md-dashboard-widget__news-dot:nth-child(6)');
    await I.see('autotest short preview', '.md-dashboard-widget__news-description');
    await I.dontSee(longText, '.md-dashboard-widget__news-description');
    await I.clickCss(toggle);
    await I.waitForElement(`${external}[aria-busy="false"]`, 10);
    await I.seeNumberOfVisibleElements(cards, 6);
    await I.waitForFunction(() => {
        const cards = document.querySelectorAll('.md-dashboard-widget__news-card');
        const firstImage = cards[0]?.querySelector('img');
        return firstImage?.complete && firstImage.naturalWidth > 0 && cards[4]?.classList.contains('is-text-only');
    }, 10);

    for (const width of [1440, 1202, 1024, 390, 1440]) {
        await I.resizeWindow(width, 1600);
        if (width < 768) {
            if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) await I.clickCss('.js-sidebar-toggler');
            await I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
        }
        await I.waitForFunction(() => [...document.querySelectorAll('.md-dashboard-widget__news-card p')].every(paragraph => {
            const style = getComputedStyle(paragraph);
            return Number(style.webkitLineClamp) === Math.floor((paragraph.parentElement.getBoundingClientRect().height + 0.1) / parseFloat(style.lineHeight));
        }), 10);
        const layout = await I.executeScript(() => {
            const hero = document.querySelector('.md-dashboard__hero').getBoundingClientRect();
            const cards = [...document.querySelectorAll('.md-dashboard-widget__news-card')];
            const paragraphs = cards.map(card => card.querySelector('p'));
            return {
                text: paragraphs.map(paragraph => paragraph.textContent),
                clamps: paragraphs.map(paragraph => Number(getComputedStyle(paragraph).webkitLineClamp)),
                spareLines: paragraphs.map(paragraph => {
                    const style = getComputedStyle(paragraph);
                    return paragraph.parentElement.getBoundingClientRect().height / parseFloat(style.lineHeight) - Number(style.webkitLineClamp);
                }),
                actionGaps: cards.filter(card => card.querySelector('p').scrollHeight > card.querySelector('p').clientHeight + 1)
                    .map(card => card.querySelector('.md-dashboard-widget__news-card-action').getBoundingClientRect().top - card.querySelector('p').getBoundingClientRect().bottom),
                actions: cards.map(card => card.querySelector('.md-dashboard-widget__news-card-action').getBoundingClientRect().bottom),
                wholeLines: paragraphs.every(paragraph => {
                    const style = getComputedStyle(paragraph);
                    const lines = paragraph.getBoundingClientRect().height / parseFloat(style.lineHeight);
                    return Math.abs(lines - Math.round(lines)) < 0.01 && lines <= Number(style.webkitLineClamp);
                }),
                textOnly: cards.map(card => card.classList.contains('is-text-only')),
                fits: document.documentElement.scrollWidth <= window.innerWidth && cards.every(card => {
                    const bounds = card.getBoundingClientRect();
                    return bounds.left >= hero.left && bounds.right <= hero.right;
                })
            };
        });
        await I.assertDeepEqual(layout.text, Array(6).fill(longText));
        await I.assertDeepEqual(layout.textOnly, [false, false, false, true, true, false]);
        await I.assertTrue(layout.spareLines.every(lines => lines >= -0.01 && lines < 1), 'Every card must use all available complete lines.');
        await I.assertTrue(layout.wholeLines, 'The final line must not reveal clipped fragments of the following line.');
        await I.assertTrue(layout.actionGaps.every(gap => gap >= 10 && gap < 36), 'Text must reach the action spacing without a large empty gap.');
        if (width >= 1200) {
            await I.assertTrue(layout.clamps[0] > layout.clamps[1] && layout.clamps[0] > 3, 'A shorter title must release more space for its description.');
            await I.assertTrue(Math.abs(layout.actions[0] - layout.actions[1]) < 1, 'Actions must stay aligned in the same row.');
        }
        await I.assertTrue(layout.clamps[3] >= 8 && layout.clamps[4] >= 8, 'Missing or failed images must give more room to text.');
        await I.assertTrue(layout.fits, `Extended summaries must fit at ${width}px.`);
        await I.executeScript(() => {
            const scrollbar = window.scrollbarMain;
            scrollbar.setMomentum(0, 0);
            scrollbar.update();
            scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('.md-dashboard-widget__news-external').getBoundingClientRect().top - 80);
        });
        if (width === 390 || width === 1202) await I.saveScreenshot(`dashboard-social-news-adaptive-${width === 390 ? 'mobile' : 'desktop'}.png`, true);
    }
    await I.wjSetDefaultWindowSize();
});

Scenario('Fresh cache avoids background requests while expansion refreshes and records account reading', async ({ I }) => {
    await openFixture(I, [socialItem('youtube', 1), socialItem('facebook', 1)]);
    await I.seeElement(badge);
    await I.assertEqual(preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.')).length, 0);
    const cache = await I.executeScript(() => JSON.parse(localStorage.getItem('webjet.externalNews.v1')));
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
    await I.assertTrue(await I.executeScript(() => JSON.parse(window.currentUser.adminSettings[`dashboard.news.${window.userLng}`]).readAt > 0),
        'The expanded panel must keep the local release acknowledged.');
    await I.assertTrue(preferenceWrites.filter(write => write.label.startsWith('dashboard.externalNews.'))
        .every(write => JSON.parse(write.value).readAt > Date.now() - 60000), 'Social acknowledgements must record their reading date.');

    await I.pressKey('Escape');
    await I.waitForVisible('.md-dashboard__welcome', 10);
    const cacheBeforeFailure = await I.executeScript(() => localStorage.getItem('webjet.externalNews.v1'));
    fixture.status = 503;
    await I.clickCss(toggle);
    await I.waitForElement(`${external}[aria-busy="false"][hidden]`, 10);
    await I.dontSeeElementInDOM(cards);
    await I.dontSeeElementInDOM(images);
    await I.dontSeeElementInDOM('.md-dashboard-widget__news-feed-status');
    await I.dontSeeElement(badge);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-highlights li', 5);
    await I.seeNumberOfVisibleElements('.md-dashboard-widget__news-channels a', 2);
    await I.assertEqual(await I.executeScript(() => localStorage.getItem('webjet.externalNews.v1')), cacheBeforeFailure,
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
