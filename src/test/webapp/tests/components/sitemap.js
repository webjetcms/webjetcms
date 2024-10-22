Feature('components.sitemap');

Before(({ I, login }) => {
    login('admin');
});

Scenario('zobrazenie v mape stranok', ({ I }) => {
    I.amOnPage("/apps/mapa-stranok/");

    I.see("Stránka sa zobrazí v mape sránok", "div.sitemaptest");
    I.dontSee("Stránka sa nezobrazí v mape sránok", "div.sitemaptest");
    I.see("Stránka sa zobrazí v mape sránok podľa menu", "div.sitemaptest");
    I.dontSee("Stránka sa nezobrazí v mape sránok podľa menu", "div.sitemaptest");
});

function testSitemap(I, Browser) {
    if (Browser.isFirefox()) {
        I.say("Firefox, skipping test");
        return;
    }

    I.amOnPage("/sitemap.xml");
    I.see('<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">');
    I.see('/apps/spring-app/kontakty/');
    I.dontSee('/admin/');

    //check pages
    I.see("/apps/mapa-stranok/podadresar-1/stranka-zobrazi-mape-sranok.html");
    I.dontSee("/apps/mapa-stranok/podadresar-1/stranka-nezobrazi-mape-sranok.html");
    I.see("/apps/mapa-stranok/podadresar-1/stranka-zobrazi-mape-sranok-podla-menu.html");
    I.dontSee("/apps/mapa-stranok/podadresar-1/stranka-nezobrazi-mape-sranok-podla-menu.html");
    I.see("/apps/mapa-stranok/podadresar-2/");
    I.see("/apps/mapa-stranok/podadresar-2/pod-podadresar-1/");
}

Scenario('zobrazenie sitemap.xml', ({ I, Browser }) => {
    testSitemap(I, Browser);
    I.logout();
    testSitemap(I, Browser);
});