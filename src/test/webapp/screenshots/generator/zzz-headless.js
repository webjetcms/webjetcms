Feature('headless');

let confLng = "sk";

Before(({ I }) => {
    confLng = I.getConfLng();
});

Scenario('headless', async({ I, DT, DTE, Document }) => {

    I.say("YOU NEED TO START ASTRO APP!!!");
    I.say("YOU NEED TO START ASTRO APP!!!");
    I.say("YOU NEED TO START ASTRO APP!!!");
    I.say("YOU NEED TO START ASTRO APP!!!");
    I.say("YOU NEED TO START ASTRO APP!!!");
    I.say("YOU NEED TO START ASTRO APP!!!");
    I.say("cd docs/examples/headless-astro/");
    I.say("npm run dev");
    pause();

    I.amOnPage("https://headless.interway.sk:8443/");
    Document.screenshot("/frontend/headless/home.png");

    I.click(locate("a").withText("News SSR"));
    Document.screenshot("/frontend/headless/news.png");

    I.click(locate("a").withText("News CSR"));
    Document.screenshot("/frontend/headless/news-client.png");

    I.click(locate("a").withText("Apps"));
    I.amOnPage("https://headless.interway.sk:8443/apps/galeria/");
    Document.screenshot("/frontend/headless/gallery.png");

    I.amOnPage("https://headless.interway.sk:8443/apps/multistep-formular/for-screenshots.html");
    Document.screenshot("/frontend/headless/multistep-form.png");

    I.amOnPage("https://headless.interway.sk:8443/apps/gdpr-cookies/");
    Document.screenshot("/frontend/headless/gdpr-cookies.png");

    I.amOnPage("https://headless.interway.sk:8443/search?q=konsolidacia");
    Document.screenshot("/frontend/headless/search.png");

});
