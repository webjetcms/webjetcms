Feature('admin.translations');

Scenario("CZ verzia preklady", ({ I, DTE }) => {
    I.logout();

    I.selectOption("language", "Česky");
    I.wait(5);

    I.see("PŘIHLÁŠENÍ");
    I.see("Zapomenuté heslo");
    I.fillField("username", "tester");
    I.fillField("password", secret("*********"));
    I.click("login-submit");

    I.wait(3);
    I.see("Nápověda");
    I.dontSee("Pomocník");
    I.see("Zpětná vazba");
    I.see("Vítejte, Tester Playwright");
    I.see("Změněné stránky");
    I.see("Novinky ve WebJETu");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=141");
    DTE.waitForEditor();
    I.see("Existuje rozpracovaná nebo neschválená verze této stránky.");
    I.click("#pills-dt-datatableInit-basic-tab");
    I.see("Nadřazený adresář");
    DTE.cancel();

    I.logout();
});

Scenario("EN verzia preklady", ({ I, DTE }) => {
    I.logout();

    I.selectOption("language", "English");
    I.wait(5);

    I.see("LOGIN");
    I.see("Lost password");
    I.fillField("username", "tester");
    I.fillField("password", secret("*********"));
    I.click("login-submit");

    I.wait(3);
    I.see("Help");
    I.dontSee("Pomocník");
    I.see("Feedback");
    I.see("Welcome, Tester Playwright");
    I.see("Changed Web pages");
    I.see("News in WebJET");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=141");
    DTE.waitForEditor();
    I.see("There is a pending or unapproved version of this page.");
    I.click("#pills-dt-datatableInit-basic-tab");
    I.see("Parent folder");
    DTE.cancel();

    I.logout();
});

Scenario("odhlasenie", ({ I }) => {
    I.logout();
});

Scenario("Konfiguracia jazykov @singlethread", ({ I, Document, DTE }) => {
    I.relogin('admin');

    Document.setConfigValue("languages", "sk,cz,en,de,pl,hu,cho,ru,esp");
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");
    I.click("div.DTE_Field_Name_lng button");
    I.dontSee("language.aut", "div.dropdown-menu.show div.inner.show");

    Document.setConfigValue("languages", "sk,cz,en,de,pl,hu,cho,ru,esp,aut");
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");
    I.click("div.DTE_Field_Name_lng button");
    I.see("language.aut", "div.dropdown-menu.show div.inner.show");

    I.amOnPage("/admin/v9/settings/translation-keys/");
    I.see("language.aut", "th.dt-th-fieldJ");

    I.click("button.buttons-create");
    DTE.waitForEditor();
    I.see("language.aut", "div.DTE_Action_Create div.DTE_Field_Name_fieldJ")

    Document.setConfigValue("languages", "sk,cz,en,de,pl,hu,cho,ru,esp");
});

Scenario("reset jazyka", ({ I, Document }) => {
    I.relogin('admin');

    Document.setConfigValue("languages", "sk,cz,en,de,pl,hu,cho,ru,esp");
});

Scenario("filter by date and keyword", ({ I, DT }) => {
    I.relogin('admin');
    I.amOnPage("/admin/v9/settings/translation-keys/");
    DT.filter("from-updateDate", "06.07.2021");
    DT.filter("to-updateDate", "07.07.2021");
    I.click({"css":"div.dataTables_scrollHeadInner button.dt-filtrujem-updateDate"});

    I.see("components.gdpr.cookies.Test.provider", "#datatableInit");
    I.see("components.gdpr.cookies.Test.purpouse", "#datatableInit");
    I.see("components.gdpr.cookies.Test.validity", "#datatableInit");
    I.see("AAX", "#datatableInit");
    I.see("BBX", "#datatableInit");
    I.see("CCX", "#datatableInit");

    //
    I.say("Set secondary filter");
    DT.filter("fieldA", "AAX");
    I.see("components.gdpr.cookies.Test.provider", "#datatableInit");
    I.dontSee("components.gdpr.cookies.Test.purpouse", "#datatableInit");
    I.dontSee("components.gdpr.cookies.Test.validity", "#datatableInit");
    I.see("AAX", "#datatableInit");
    I.dontSee("BBX", "#datatableInit");
    I.dontSee("CCX", "#datatableInit");

    //
    I.say("Clear fieldA filter and click on search button on date fields");
    I.fillField(".dt-filter-fieldA", "");
    I.click("button.dt-filtrujem-updateDate");
    DT.waitForLoader();
    I.see("components.gdpr.cookies.Test.provider", "#datatableInit");
    I.see("components.gdpr.cookies.Test.purpouse", "#datatableInit");
    I.see("components.gdpr.cookies.Test.validity", "#datatableInit");
    I.see("AAX", "#datatableInit");
    I.see("BBX", "#datatableInit");
    I.see("CCX", "#datatableInit");
});

Scenario("edit from searchall", ({ I, DT, DTE }) => {
    I.relogin('admin');
    I.amOnPage("/admin/searchall.jsp");
    I.fillField("#text1", "cookies.bar.");
    I.clickCss("#tabMenu1 .btn");

    I.click("cookies.bar.preferencne");

    I.switchToNextTab();

    DT.waitForLoader();
    DTE.waitForEditor();

    I.see("cookies.bar.preferencne", "#datatableInit_modal .modal-title");

    I.closeCurrentTab();
});

Scenario("show HTML value for show-html columns", ({ I, DT }) => {
    I.relogin('admin');
    I.amOnPage("/admin/v9/settings/translation-keys/");
    DT.waitForLoader();
    DT.filter("key", "admin.update.databaseNotUpdatedToWebJET7");
    I.waitForText("admin.update.databaseNotUpdatedToWebJET7", 10, "#datatableInit tbody");
    I.see("<a href=\"/admin/update/update_webjet7.jsp", "#datatableInit tbody");
});