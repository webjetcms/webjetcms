Feature('apps.news');

Before(({ login }) => {
    login('admin');
});

Scenario('zoznam noviniek', ({ I, DT, DTE }) => {

    I.amOnPage("/apps/news/admin/");
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("/English/News"));

    I.see("McGregor sales force");
    I.see("News");
    I.dontSee("Čím je človek bohatší, tým má menej hotovosti")

    //
    var pageName = "Trhy sú naďalej vydesené";
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("/Jet portal 4/Zo sveta financií"));
    DT.waitForLoader();
    I.dontSee("McGregor sales force");
    I.see(pageName);
    I.see("Čím je človek bohatší, tým má menej hotovosti")

    //
    I.say("Checking ckeditor");
    I.click(pageName);
    DTE.waitForEditor("newsDataTable");
    I.see(pageName, "#newsDataTable_modal h5.modal-title");

    I.switchTo("iframe.cke_wysiwyg_frame.cke_reset");
    I.see("Na trhoch minulý týždeň pretrvávala volatilita", "h2");

    I.switchTo();

    //
    I.say("Check permissions");
    I.amOnPage("/apps/news/admin/?removePerm=cmp_news");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario('logoff', ({ I }) => {
    I.logout();
});

Scenario('set groupIds parameter in webpage', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=10");
    DTE.waitForEditor();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.waitForElement("iframe.wj_component", 10);
    I.wait(2);
    I.clickCss("iframe.wj_component");
    I.wait(2);

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.waitForElement("#tabLink1")
    I.wait(3);

    //
    I.say("check pages only from group 24");
    I.switchTo("#componentIframeWindowTab");
    I.waitForElement("th.dt-th-title input", 20);

    DT.waitForLoader("newsDataTable");
    I.see("Zo sveta financií");
    I.dontSee("Produktová stránka - B verzia");

    //
    I.say("set groupIds to 24,25");

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.clickCss("#tabLink1");
    I.fillField("#groupIds", "24,25");

    //
    I.say("check pages from group 24 and 25");
    I.clickCss("#tabLink5");
    I.wait(3);
    I.switchTo("#componentIframeWindowTab");
    I.waitForElement("th.dt-th-title input", 20);

    DT.waitForLoader("newsDataTable");
    I.see("Zo sveta financií");
    I.see("Produktová stránka - B verzia")

    I.switchTo();
});

Scenario('show perex groups by selected groupIds pageParams', async ({ I, DT, DTE, Document }) => {
    //perex groups may be show only on selected folders
    //when you setup news component it will show it by current folder
    //they should be dependent also on selected groupIds by pageParams
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=10");
    DTE.waitForEditor();

    Document.editorComponentOpen();

    I.waitForElement("#tabLink3")
    I.clickCss("#tabLink3"); //znacky
    let options = await I.grabHTMLFrom('#disabledItemsLeft1');
    I.assertContain(options, "podnikanie (id:2)");
    I.assertNotContain(options, "Newsletter perex skupina (id:10)");

    I.clickCss("#tabLink1"); //parametre aplikacie
    I.fillField("#groupIds", "24,27");

    I.say("Updating news component pageParams");
    Document.editorComponentOk();

    Document.editorComponentOpen();

    I.waitForElement("#tabLink3");
    I.clickCss("#tabLink3"); //znacky
    options = await I.grabHTMLFrom('#disabledItemsLeft1');
    I.assertContain(options, "podnikanie (id:2)");
    I.assertContain(options, "Newsletter perex skupina (id:10)");

    I.switchTo();
});

Scenario('Test of permissions and filtering select by permissions', async ({ I, DT }) => {
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader();

    I.say("Check that user see folder to select");
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/blogger") ) );
    I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/English/News") ) );

    //
    I.say('Check filtering by permissions');
    I.relogin("tester2");
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader();

    I.say("Check that user see folder to select");
    I.clickCss('button[data-id="groupIdFilterSelect"]');
    I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/blogger") ) );
    I.dontSeeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/English/News") ) );

    //
    I.say("Check no perms");
    I.relogin("jtester");
    I.amOnPage("/apps/news/admin/");

    I.say("Check that permission error is showed");
    I.waitForElement("#toast-container-webjet");
    I.see("Prístup na adresár zamietnutý", "#toast-container-webjet > .toast-error > .toast-message");

    I.say("Check that user DONT have groups to select");
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    const numberOfGroups = await I.grabNumberOfVisibleElements( locate("div.dropdown-menu").find( locate("a.dropdown-item > span") ) );
    I.assertEqual(0, numberOfGroups, "ERROR - User should not see any group to select");
});

Scenario("logout", ({ I }) => {
    I.logout();
});