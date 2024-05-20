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

    DT.filter("name", key);

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
    DT.filterSelect("logType", "AUDIT_NOTIFY");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    I.say("skus prejst na stranku 5 a vyskusaj zmenit selector");
    I.click({css: "ul.pagination li:nth-child(6) a"});
    I.wait(2);
    DT.filterSelect("logType", "AUDIT_NOTIFY");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    I.say("vybereme inu hodnotu a overime, ze sa zvolila");
    DT.filterSelect("logType", "AUDIT_NOTIFY");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.wait(2);
    clearFilter(I, "logType");

    //
    I.say("Check set filter not clicking on search button");
    I.amOnPage("/admin/v9/apps/audit-search/");
    I.click({ css: "div.dataTables_scrollHeadInner div.dt-filter-logType button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("USER_LOGON"));
    DT.filter("description", "node");
    I.see("USER_LOGON", "#datatableInit tbody");
    I.dontSee("CRON", "#datatableInit tbody");

    I.say("Clear node value, check logType is applyed");
    DT.filter("description", "");
    I.see("USER_LOGON", "#datatableInit tbody");
    I.dontSee("CRON", "#datatableInit tbody");

    //
    I.say("Check boolean filter");
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    I.click({ css: "div.dataTables_scrollHeadInner div.dt-filter-mustAccepted button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Áno"));
    DT.filter("name", "test");
    I.see("Test", "#reservationObjectDataTable tbody");
    I.dontSee("testB", "#reservationObjectDataTable tbody");
    I.dontSee("Zasadačka veľká", "#reservationObjectDataTable tbody");

    I.say("Clear name value, check mustAccepted is applyed");
    DT.filter("name", "");
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
    I.click({ css: "div.dataTables_scrollHeadInner div.dt-filter-templatesGroupId button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Demo JET"));
    DT.filter("availableGrooupsList", "jet");
    I.see("Demo JET", "#datatableInit tbody");
    I.see("Microsite - blue", "#datatableInit tbody");

    I.say("Clear jet value, check templatesGroupId is applyed");
    DT.filter("availableGrooupsList", "");
    I.see("Demo JET", "#datatableInit tbody");
    I.dontSee("nepriradené", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");

    //
    I.say("Check boolean filter");
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click({ css: "div.dataTables_scrollHeadInner div.dt-filter-disableSpamProtection button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("Áno"));
    DT.filter("tempName", "yellow");
    I.see("Microsite - yellow", "#datatableInit tbody");

    I.say("Clear tempName value, check disableSpamProtection is applyed");
    DT.filter("tempName", "");
    I.see("Microsite - yellow", "#datatableInit tbody");
    I.dontSee("nepriradené", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");
});

Scenario('Check contains,startswith,endswith,equals', ({ I, DT }) => {
    //
    I.say("LOCAL search");
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filter("tempName", "Newsletter", "Obsahuje");
    I.see("Newsletter", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filter("tempName", "Newsletter", "Začína na");
    I.see("Newsletter", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filter("tempName", "EN", "Končí na");
    I.see("Subpage EN", "#datatableInit tbody");
    I.see("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    DT.filter("tempName", "Newsletter", "Rovná sa");
    I.see("Newsletter", "#datatableInit tbody");
    I.dontSee("Newsletter EN", "#datatableInit tbody");
    I.dontSee("Generic", "#datatableInit tbody");

    //
    I.say("SERVER side search");
    I.amOnPage("/admin/v9/webpages/media/");
    DT.filter("mediaLink", ".sk", "Obsahuje");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.see("www.pluska.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filter("mediaLink", "s", "Začína na");
    I.see("sdasdfasdf", "#mediaTable tbody");
    I.dontSee("www.sme.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filter("mediaLink", ".sk", "Končí na");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.see("www.pluska.sk", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");

    DT.filter("mediaLink", "www.sme.sk", "Rovná sa");
    I.see("www.sme.sk", "#mediaTable tbody");
    I.dontSee("www.sme.sk/en", "#mediaTable tbody");
    I.dontSee("Cenník", "#mediaTable tbody");
});

Scenario('BUG-zobrazenie selectov vo vnorenej DT',  async({ I, DT, DTE, Document }) => {
    //pri druhom zobrazeni DT v editore bol bug, kedy sa zle zobrazia selecty
    I.amOnPage("/admin/v9/users/user-groups/?id=2");
    DTE.waitForEditor("userGroupsDataTable");
    I.click("#pills-dt-userGroupsDataTable-sites-tab");
    I.wait(1);
    await Document.compareScreenshotElement("#datatableFieldDTE_Field_docDetailsList_wrapper th.dt-th-title div.input-group div.filter-option", "autotest-bug-zobrazenie-selectov-vo-vnorenej-dt.png", null, null, 5);

    DTE.cancel();
    I.click("Obchodní partneri");
    DTE.waitForEditor("userGroupsDataTable");
    I.click("#pills-dt-userGroupsDataTable-sites-tab");
    I.wait(1);
    await Document.compareScreenshotElement("#datatableFieldDTE_Field_docDetailsList_wrapper th.dt-th-title div.input-group div.filter-option", "autotest-bug-zobrazenie-selectov-vo-vnorenej-dt.png", null, null, 5);
});