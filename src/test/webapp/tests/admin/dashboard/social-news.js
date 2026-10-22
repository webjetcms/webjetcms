Feature('admin.dashboard.social-news');

const cards = '.md-dashboard-widget__news-card';
const socialCards = `${cards}[data-news-id^="youtube:"], ${cards}[data-news-id^="facebook:"]`;

Before(({ login }) => {
    login('admin');
});

Scenario('Published social posts appear with titles, links and a working thumbnail', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.clickCss('.md-dashboard-widget__news-toggle');
    I.waitForVisible('.md-dashboard__hero.is-news-expanded', 10);
    // Expansion refreshes the configured public feed, even when a browser cache exists.
    I.waitForElement('.md-dashboard-widget__news-external[aria-busy="false"]', 15);
    I.waitForVisible(socialCards, 15);

    const posts = await I.executeScript(selector => [...document.querySelectorAll(selector)].map(card => ({
        source: card.dataset.newsId.split(':')[0],
        title: card.querySelector('h4').textContent.trim(),
        link: card.href
    })), socialCards);
    I.assertTrue(posts.length > 0, 'The configured feed must provide real social posts.');
    for (const post of posts) {
        I.assertTrue(post.title.length > 0, 'A social post must have a title.');
        I.see(post.title, socialCards);
        const link = new URL(post.link);
        const hosts = post.source === 'youtube'
            ? ['youtube.com', 'www.youtube.com', 'youtu.be']
            : ['facebook.com', 'www.facebook.com'];
        I.assertEqual(link.protocol, 'https:');
        I.assertTrue(hosts.includes(link.hostname), `The ${post.source} post must link to its social network.`);
    }

    const image = `${cards} .md-dashboard-widget__news-image`;
    I.waitForElement(image, 10);
    I.scrollTo(image);
    I.waitForFunction(([selector]) => {
        const thumbnail = document.querySelector(selector);
        return thumbnail?.complete && thumbnail.naturalWidth > 0;
    }, [image], 15);

    I.clickCss('.md-dashboard-widget__news-close');
    I.waitForInvisible('.md-dashboard__hero.is-news-expanded', 10);
    I.seeElement('.md-dashboard__welcome');
});
