Feature('a11y.webpages');

Before(({ I, login }) => {
    login('admin');
});

Scenario('jstree settings', async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.click("button.buttons-jstree-settings");
    I.waitForElement("#jstreeSettingsModal", 5);
    await a11y.check("#jstreeSettingsModal");
});

Scenario('p26: image dialog @current', async ({ I, DTE, Browser, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=16");
    DTE.waitForEditor();
    I.wait(3);

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    //
    I.say("Opening image dialog and navigating to test folder");

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), 20);

    await a11y.check();
    I.switchTo();
});

Scenario('p36: tabs navigation', async ({ I, DTE, Browser, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    //TODO: pills-pages as Obsahová navigácia
    await a11y.check();
});