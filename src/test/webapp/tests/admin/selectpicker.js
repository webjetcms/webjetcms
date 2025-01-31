Feature('admin.selectpicker');

/**
 * Otestovanie problemov so selectpickerom, ktory sa nedal zmenit ked sa:
 * - preslo na dalsiu stranku v zozname
 * - nastavil sa filter a nasledne sa zmazal
 * - otvorilo sa popup okno DT editora, zatvorilo a otvorilo sa znova
 */

Before(({ I, login }) => {
    login('admin');
});

function clearFilter(I, name) {
    I.click("#dt-filter-labels-link-"+name);
    I.wait(2);
}

Scenario('datatables header select', ({ I, DT, DTE }) => {
    var key = "Demo JET";

    I.say("odfiltruj data");
    I.amOnPage("/admin/v9/templates/temps-groups-list/");

    DT.filterContains("name", key);

    I.see('InterWay Developer SK');
    I.dontSee('Developer CZ');

    I.say("otvor popup okno");
    I.click(key);
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-metadata-tab");

    I.seeInField("#DTE_Field_projectDeveloper", 'InterWay Developer SK');
    DTE.cancel();

    I.say("prepni jazyk v hlavicke");

    I.click({css: "div.breadcrumb-language-select"});
    I.click(locate('.dropdown-item').withText("Český jazyk"));
    DT.waitForLoader();

    I.see('Developer CZ');
    I.dontSee('InterWay Developer SK');

    I.click(key);
    DTE.waitForEditor();

    I.seeInField("#DTE_Field_projectDeveloper", 'Developer CZ');
    DTE.cancel();
});

Scenario('nastavenie filtra, zmazanie filtra', ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-search/");

    I.say("vyber moznost INIT a odfiltruj data");
    DT.filterSelect("logType", "INIT");
    clearFilter(I, "logType");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("logType", "Helpdesk");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    I.say("skus prejst na stranku 5 a vyskusaj zmenit selector");
    I.click({css: "ul.pagination li:nth-child(6) button"});
    I.wait(2);
    DT.filterSelect("logType", "Helpdesk");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("logType", "Helpdesk");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    //
    I.say("Check set filter not clicking on search button");
    I.amOnPage("/admin/v9/apps/audit-search/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-logType button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("USER_LOGON"));
    DT.filterContains("description", "node");
    I.see("USER_LOGON", "#datatableInit tbody");
    I.dontSee("CRON", "#datatableInit tbody");

    I.say("Clear node value, check logType is applyed");
    DT.filterContains("description", "");
    I.see("USER_LOGON", "#datatableInit tbody");
    I.dontSee("CRON", "#datatableInit tbody");

    //
    I.say("Check boolean filter");
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-mustAccepted button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Áno"));
    DT.filterContains("name", "test");
    I.see("Test", "#reservationObjectDataTable tbody");
    I.dontSee("testB", "#reservationObjectDataTable tbody");
    I.dontSee("Zasadačka veľká", "#reservationObjectDataTable tbody");

    I.say("Clear name value, check mustAccepted is applyed");
    DT.filterContains("name", "");
    I.see("Test", "#reservationObjectDataTable tbody");
    I.dontSee("testB", "#reservationObjectDataTable tbody");
    I.dontSee("Zasadačka veľká", "#reservationObjectDataTable tbody");
});

Scenario('nastavenie filtra LOCAL, zmazanie filtra LOCAL', ({ I, DT }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");

    I.say("vyber moznost Anglicky a odfiltruj data");
    DT.filterSelect("lng", "Anglický");
    I.see("Záznamy 1 až 2 z 2");
    clearFilter(I, "lng");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("lng", "Španielsky");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "lng");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("lng", "Ruský");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "lng");

    //
    I.say("Check set filter not clicking on search button");
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-templatesGroupId button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Demo JET"));
    DT.filterContains("availableGrooupsList", "jet");
    I.see("Demo JET", "#datatableInit tbody");
    I.see("Microsite - blue", "#datatableInit tbody");

    I.say("Clear jet value, check templatesGroupId is applyed");
    DT.filterContains("availableGrooupsList", "");
    I.see("Demo JET", "#datatableInit tbody");
    I.dontSee("nepriradené", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");

    //
    I.say("Check boolean filter");
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-disableSpamProtection button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Áno"));
    DT.filterContains("tempName", "yellow");
    I.see("Microsite - yellow", "#datatableInit tbody");

    I.say("Clear tempName value, check disableSpamProtection is applyed");
    DT.filterContains("tempName", "");
    I.see("Microsite - yellow", "#datatableInit tbody");
    I.dontSee("nepriradené", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");
});

Scenario('Check contains,startswith,endswith,equals', ({ I, DT }) => {
    //
    I.say("LOCAL search");
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContainsForce("tempName", "Newsletter");
    I.see("Newsletter", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filterStartsWith("tempName", "Newsletter");
    I.see("Newsletter", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filterEndsWith("tempName", "EN");
    I.see("Subpage EN", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filterEquals("tempName", "Newsletter");
    I.see("Newsletter", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    //
    I.say("SERVER side search");
    I.amOnPage("/admin/v9/webpages/media/");
    DT.filterContainsForce("mediaLink", ".sk");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.see("www.pluska.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filterStartsWith("mediaLink", "s");
    I.see("sdasdfasdf", "#mediaTable tbody");
    I.dontSee("www.sme.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filterEndsWith("mediaLink", ".sk");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.see("www.pluska.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filterEquals("mediaLink", "www.sme.sk");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.dontSee("www.sme.sk/en", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");
});

Scenario('BUG-zobrazenie selectov vo vnorenej DT',  async({ I, DTE, Document }) => {
    //pri druhom zobrazeni DT v editore bol bug, kedy sa zle zobrazia selecty
    I.amOnPage("/admin/v9/users/user-groups/?id=2");
    DTE.waitForEditor("userGroupsDataTable");
    I.click("#pills-dt-userGroupsDataTable-sites-tab");
    I.wait(1);
    await Document.compareScreenshotElement("#datatableFieldDTE_Field_docDetailsList_wrapper th.dt-th-title div.input-group div.filter-option", "autotest-bug-zobrazenie-selectov-vo-vnorenej-dt.png", null, null, 10);

    DTE.cancel();
    I.click("Obchodní partneri");
    DTE.waitForEditor("userGroupsDataTable");
    I.click("#pills-dt-userGroupsDataTable-sites-tab");
    I.wait(1);
    await Document.compareScreenshotElement("#datatableFieldDTE_Field_docDetailsList_wrapper th.dt-th-title div.input-group div.filter-option", "autotest-bug-zobrazenie-selectov-vo-vnorenej-dt.png", null, null, 10);
});

Scenario('BUG-set selectpickerbinded after fields visibility change', ({ I, DT }) => {
    var selector = "#datatableInit_wrapper .dt-scroll-headInner th.dt-th-fieldA select.filter-input-prepend.selectpickerbinded";

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.resetTable();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=21686");
    DT.waitForLoader();
    I.jstreeClick("Voliteľné polia");
    DT.showColumn("text - A");

    I.waitForElement(selector, 10);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=7625");

    I.waitForElement(selector, 10);
});

Scenario("reset table", ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.resetTable();
});

Scenario("BUG selectpicker BS 5.3 duplicate value", ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/users/user-groups/?id=1");
    DTE.waitForEditor("userGroupsDataTable");
    const field = ".DTE_Field_Name_userGroupType button.dropdown-toggle div.filter-option-inner-inner";
    const value1 = "Prístupov k zaheslovanej sekcii web sídla";
    const value2 = "Prihlásenie k hromadnému e-mailu"
    I.see(value1, field);
    I.dontSee(value2, field);
    DTE.cancel();
    I.click("Newsletter");
    DTE.waitForEditor("userGroupsDataTable");
    I.dontSee(value1, field);
    I.see(value2, field);
    DTE.cancel();
});