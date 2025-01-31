Feature('webpages.webpage-virtual-path');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=49441");
});

/**
 * ticket #55161
 */
Scenario('BUG - URL address without slash at end', ({ I, DT, DTE }) => {

    //
    I.say("set and check virtual path without slash at end");
    I.click("News 2");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("#DTE_Field_virtualPath", "/test-stavov/automaticke-generovanie-url/news-2");
    DTE.save();

    I.click("News 2");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/automaticke-generovanie-url/news-2");
    DTE.cancel();

    //
    I.say("set same virtual path for second page");
    I.click("News duplikat");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("#DTE_Field_virtualPath", "/test-stavov/automaticke-generovanie-url/news-2");
    DTE.save();

    I.see("Zadaná URL adresa je už použitá na stránke:", "div.toast-message");
    I.click(".toast-close-button");

    //
    I.say("verify virtual path for second page");
    I.click("News duplikat");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/automaticke-generovanie-url/news-2-2.html");
    DTE.cancel();

    //
    I.say("regenerate virtual path for second page");
    I.click("News duplikat");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("#DTE_Field_virtualPath", "");
    DTE.save();

    I.click("News duplikat");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/automaticke-generovanie-url/news-2.html");
    DTE.cancel();

    var text = "News duplikat stranka.";
    I.amOnPage("/test-stavov/automaticke-generovanie-url/news-2.html");
    I.see(text);

    //
    I.say("verify page show");
    text = "Toto je News dva stránka.";
    I.amOnPage("/test-stavov/automaticke-generovanie-url/news-2");
    I.see(text);
    //should be redirected to page with slash at end
    I.seeInCurrentUrl("/test-stavov/automaticke-generovanie-url/news-2/")

    I.amOnPage("/test-stavov/automaticke-generovanie-url/news-2/");
    I.see(text);
});

Scenario('Verify asterisk URL address', ({ I, DT, DTE }) => {

    //
    I.say("set virtual path");
    I.click("Test hviezdičky");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("#DTE_Field_virtualPath", "/test-stavov/automaticke-generovanie-url/test-hviezdicky/*");
    DTE.save();

    //
    I.say("verify virtual path");
    I.click("Test hviezdičky");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/automaticke-generovanie-url/test-hviezdicky/*");
    DTE.cancel();

    //
    I.say("verify page show with different urls");
    var text = "Testovacia stránka hniezdičková URL adresa.";
    I.amOnPage("/test-stavov/automaticke-generovanie-url/test-hviezdicky/");
    I.see(text);

    I.amOnPage("/test-stavov/automaticke-generovanie-url/test-hviezdicky");
    I.see(text);

    I.amOnPage("/test-stavov/automaticke-generovanie-url/test-hviezdicky/tralala");
    I.see(text);

    I.amOnPage("/test-stavov/automaticke-generovanie-url/test-hviezdicky/tralala/nieco");
    I.see(text);

    I.amOnPage("/test-stavov/automaticke-generovanie-url/test-hviezdicky/pokus.html");
    I.see(text);

    I.amOnPage("/test-stavov/automaticke-generovanie-url/test-hviezdicky/tralala/pokus.html");
    I.see(text);
});