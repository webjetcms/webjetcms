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

Scenario('zobrazenie sitemap.xml', ({ I, Browser }) => {
    if (Browser.isFirefox()) {
        I.say("Firefox, skipping test");
        return;
    }

    I.amOnPage("/sitemap.xml");
    I.see('<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">');
    I.see('/apps/spring-app/kontakty/');
    I.dontSee('/admin/');
});