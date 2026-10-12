const { waitForWidgets } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.news');

const hero = '.md-dashboard__hero';
const news = '.md-dashboard__news';
const toggle = '.md-dashboard-widget__news-toggle';
const title = '.md-dashboard-widget__news-title';
const preferenceRoute = '**/admin/rest/admin-settings/';

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.waitForText('Čo je nové (5)', 10, news);
});

Scenario('Header configuration accepts CSS gradients and image paths and falls back for invalid values', async ({ I }) => {
    const defaultBackground = await I.executeScript(() => getComputedStyle(document.querySelector('.md-dashboard__hero')).backgroundImage);
    const origin = await I.executeScript(() => window.location.origin);
    I.assertEqual(await I.executeScript(() => document.querySelector('webjet-overview-dashboard').config.heroBackgroundImage),
        'linear-gradient(111.7deg, #031D46 0%, #073780 76.92%)');
    const backgrounds = [
        ['linear-gradient(135deg, #102A43 0%, #486581 100%)', 'linear-gradient(135deg, rgb(16, 42, 67) 0%, rgb(72, 101, 129) 100%)'],
        ['radial-gradient(circle, #102A43, #486581)', 'radial-gradient(circle, rgb(16, 42, 67), rgb(72, 101, 129))'],
        ['conic-gradient(#102A43, #486581)', 'conic-gradient(rgb(16, 42, 67), rgb(72, 101, 129))'],
        ['repeating-linear-gradient(45deg, #102A43 0px, #486581 20px)', 'repeating-linear-gradient(45deg, rgb(16, 42, 67) 0px, rgb(72, 101, 129) 20px)'],
        ['/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg', `url("${origin}/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg")`],
        ['', defaultBackground],
        ['linear-gradient(invalid)', defaultBackground],
        ['linear-gradient(red, blue); color: red', defaultBackground],
        ['linear-gradient(red, blue), url(javascript:alert(1))', defaultBackground]
    ];
    try {
        for (const [value, expected] of backgrounds) {
            const actual = await I.executeScript(value => {
                const dashboard = document.querySelector('webjet-overview-dashboard');
                dashboard.configure({ data: dashboard.data, labels: dashboard.labels, config: { ...dashboard.config, heroBackgroundImage: value } });
                const hero = document.querySelector('.md-dashboard__hero');
                return { background: getComputedStyle(hero).backgroundImage, size: getComputedStyle(hero).backgroundSize, color: hero.style.color };
            }, value);
            I.assertEqual(actual.background, expected);
            I.assertEqual(actual.size, 'cover');
            I.assertEqual(actual.color, '');
            if (value.startsWith('linear-gradient(135deg') || value.startsWith('/admin/skins/')) {
                I.waitForFunction(() => document.querySelector('.md-dashboard__hero').getAnimations({ subtree: true }).every(animation => animation.playState !== 'running'), 5);
                I.saveScreenshot(value.startsWith('/admin/skins/') ? 'dashboard-hero-custom-image.png' : 'dashboard-hero-custom-gradient.png');
            }
        }
    } finally {
        I.refreshPage();
    }
});

Scenario('Release carousel keeps its size and expands over the unchanged welcome content', async ({ I }) => {
    I.resizeWindow(1440, 1000);
    await waitForWidgets(I);
    I.dontSeeElementInDOM('.md-dashboard__sessions');
    I.dontSeeElementInDOM(`${hero} [data-widget-type="sessions"]`);
    I.see('Verzia 2026.18', news);
    const original = await I.executeScript(() => ({
        welcome: document.querySelector('.md-dashboard__welcome').innerHTML,
        height: document.querySelector('.md-dashboard__hero').getBoundingClientRect().height,
        background: getComputedStyle(document.querySelector('.md-dashboard__hero')).backgroundImage
    }));
    I.assertEqual(original.background, 'linear-gradient(111.7deg, rgb(3, 29, 70) 0%, rgb(7, 55, 128) 76.92%)');
    for (let index = 0; index < 5; index++) {
        I.clickCss('.md-dashboard-widget__news-control[aria-label="Nasledujúca novinka"]');
        I.assertEqual(await I.grabNumberOfVisibleElements(`${news} [aria-current="true"]`), 1);
        I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__hero').getBoundingClientRect().height), original.height);
    }
    I.waitForFunction(() => document.querySelector('.md-dashboard-widget__news-preview').getAnimations().length === 0, 5);
    I.saveScreenshot('dashboard-news-compact.png');
    I.clickCss(toggle);
    I.waitForVisible('.md-dashboard__hero.is-news-expanded', 10);
    I.dontSeeElement('.md-dashboard__welcome');
    I.seeNumberOfVisibleElements('.md-dashboard-widget__news-highlights li', 5);
    I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('md-dashboard-widget__news-close')));
    I.see('OAuth2', '.md-dashboard-widget__news-highlights');
    I.saveScreenshot('dashboard-news-expanded.png');
    I.pressKey('Escape');
    I.waitForVisible('.md-dashboard__welcome', 10);
    I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__welcome').innerHTML), original.welcome);
    I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('md-dashboard-widget__news-toggle')));
    I.assertEqual(await I.grabAttributeFrom('.md-dashboard-widget__news-more', 'href'), 'https://docs.webjetcms.sk/latest/sk/CHANGELOG');
});

Scenario('News rotates automatically and remains within the hero on tablet and mobile', async ({ I }) => {
    I.moveCursorTo('.md-dashboard__greeting');
    const previous = await I.grabTextFrom(title);
    I.waitForFunction(([{ selector, previous }]) => document.querySelector(selector)?.textContent !== previous, [{ selector: title, previous }], 15);
    for (const width of [1024, 390]) {
        I.resizeWindow(width, 950);
        const contained = () => I.executeScript(() => {
            const hero = document.querySelector('.md-dashboard__hero').getBoundingClientRect();
            const card = document.querySelector('.md-dashboard__news .md-dashboard__widget').getBoundingClientRect();
            return card.left >= hero.left && card.right <= hero.right && card.bottom <= hero.bottom
                && document.documentElement.scrollWidth <= window.innerWidth;
        });
        I.assertTrue(await contained(), `Compact news must fit at ${width}px.`);
        if (width === 390) I.saveScreenshot('dashboard-news-mobile-compact.png');
        I.clickCss(toggle);
        I.waitForVisible('.md-dashboard__hero.is-news-expanded', 10);
        I.assertTrue(await contained(), `Expanded news must fit at ${width}px.`);
        if (width === 390) I.saveScreenshot('dashboard-news-mobile.png');
        I.clickCss('.md-dashboard-widget__news-close');
        I.waitForVisible('.md-dashboard__welcome', 10);
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Expired account badge returns when the announcement text changes within the same version', async ({ I }) => {
    const writes = [];
    await I.mockRoute(preferenceRoute, route => {
        writes.push(route.request().postDataJSON());
        return route.fulfill({ status: 200, contentType: 'application/json', body: 'true' });
    });
    I.executeScript(() => {
        const dashboard = document.querySelector('webjet-overview-dashboard');
        const key = `dashboard.news.${window.userLng}`;
        const state = JSON.parse(window.currentUser.adminSettings[key]);
        state.firstSeen = Date.now() - 31 * 86400000;
        window.currentUser.adminSettings[key] = JSON.stringify(state);
        dashboard.configure({ data: dashboard.data, config: dashboard.config, labels: dashboard.labels });
    });
    I.waitForElement(`${news} .md-dashboard-widget__news-badge[hidden]`, 10);
    I.executeScript(() => {
        const dashboard = document.querySelector('webjet-overview-dashboard');
        dashboard.configure({ data: dashboard.data, config: dashboard.config,
            labels: { ...dashboard.labels, changelog: dashboard.labels.changelog + '<p>autotest updated release</p>' } });
    });
    I.waitForVisible('.md-dashboard-widget__news-badge', 10);
    await I.waitForFunction(() => JSON.parse(window.currentUser.adminSettings[`dashboard.news.${window.userLng}`]).firstSeen > Date.now() - 60000, 10);
    I.assertEqual(writes.length, 1);
    I.assertEqual(writes[0].label, 'dashboard.news.sk');
    I.assertTrue(JSON.parse(writes[0].value).firstSeen > Date.now() - 60000);
    await I.stopMockingRoute(preferenceRoute);
    I.refreshPage();
});
