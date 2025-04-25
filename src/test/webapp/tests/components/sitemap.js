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

Scenario('testovanie app - Mapa stránok', async ({ I, DTE, Apps, Document }) => {
    Apps.insertApp('Mapa stránok', '#components-sitemap-title');

    const defaultParams = {
        "groupId": "1",
        "maxDepth": "5",
        "colsNum": "1"
    }
    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.seeElement(locate('a').withText('Úvod'));
    I.seeElement(locate('a').withText('Zo sveta financií'));
    I.seeElement(locate('a').withText('Produktová stránka'));
    I.seeElement(locate('a').withText('Kontakt'));

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        "groupId": "15257",
        "maxDepth": "1",
        "colsNum": "2"
    }
    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('a').withText('Aplikácie').withAttr({ role: 'treeitem' }));
    I.waitForElement('input[value="/Aplikácie"]', 10);
    DTE.fillField("maxDepth", "1");
    DTE.fillField("colsNum", "2");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);
    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.dontSeeElement(locate('a').withText('Poll'));
    I.dontSeeElement(locate('a').withText('Telefony'));
    I.dontSeeElement(locate('a').withText('Úvod'));
    I.seeElement(locate('a').withText('Anketa'));
    I.seeNumberOfElements('#sitemap tr:first-child td', 2);
});