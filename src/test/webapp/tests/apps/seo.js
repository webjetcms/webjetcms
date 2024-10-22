Feature('apps.seo');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

let dayDateFrom = "01.07.2021";
let dayDateTo = "30.07.2021";

function checkDates(I) {
    I.seeInField("div.md-breadcrumb input.dt-filter-from-dayDate", dayDateFrom);
    I.seeInField("div.md-breadcrumb input.dt-filter-to-dayDate", dayDateTo);
}

function checkCustomDates(I, dateFrom, dateTo) {
    I.seeInField("div.md-breadcrumb input.dt-filter-from-dayDate", dateFrom);
    I.seeInField("div.md-breadcrumb input.dt-filter-to-dayDate", dateTo);
}

function cleanFilter(I, DT) {
    I.click("button.btn-vue-jstree-item-edit");
    I.click(locate(".jstree-anchor").withText("Koreňový priečinok"));
    DT.waitForLoader();
}

function setDates(I, filterName, waitForGraphLoader=true) {

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    within(filterName, () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, dayDateFrom);
        I.fillField({css: "input.dt-filter-to-dayDate"}, dayDateTo);
        I.click({css: "button.dt-filtrujem-dayDate"});
    });

    if (waitForGraphLoader) {
        waitForTableLoader(I);
    } else {
        I.dtWaitForLoader();
    }
}

function setCustomDates(I, filterName, dateFrom, dateTo, waitForGraphLoader=true) {

    I.say("Setting dates: "+dateFrom+"-"+dateTo);

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    within(filterName, () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, dateFrom);
        I.fillField({css: "input.dt-filter-to-dayDate"}, dateTo);
        I.click({css: "button.dt-filtrujem-dayDate"});
    });

    if (waitForGraphLoader) {
        waitForTableLoader(I);
    } else {
        I.dtWaitForLoader();
    }
}

function waitForTableLoader(I) {
    var name = "div.dataTables_processing";
    //first wait for loader to show, because we must wait for graph loading
    //originally i was waitForVisible(name, 10) but it's unreliable (sometimes it's allready hidden)
    //wait for it to disapear
    for (var i=0; i<5; i++) {
        I.wait(1);
        I.waitForInvisible(name, 40);
    }
    //ak by sa este nestihol zobrazit kym sa vykona prva podmienka, pre istotu pockame a potom skusime znova
    I.wait(1);
    I.waitForInvisible(name, 40);
    I.wait(0.5);
}

Scenario("admin", ({ I, DT }) => {
    I.amOnPage("/apps/seo/admin/");

    I.seeElement("#bots-pieVisits");
    I.seeElement("#bots-lineVisits");
    I.seeElement("#botsDataTable");

    setDates(I, "#botsDataTable_extfilter");

    //refresh page
    I.amOnPage("/apps/seo/admin/");

    checkDates(I);

    DT.checkTableRow("botsDataTable", 1, ["1", "Googlebot 2.0", "24 861", "12,76", "30.07.2021"]);
    DT.checkTableRow("botsDataTable", 2, ["2", "Slackbot-LinkExpanding 1.0", "96 504", "49,52", "30.07.2021"]);

    DT.filter("name", "ThinkChaos 0.0");
    DT.waitForLoader();
    DT.checkTableRow("botsDataTable", 1, ["20", "ThinkChaos 0.0", "98", "0,05", "30.07.2021"]);
    I.click("ThinkChaos 0.0");

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    checkDates(I);

    I.waitForElement("#botsDetails-lineVisits", 10);
    I.seeElement("#botsDetailsDataTable");

    DT.filter("document", "News");
    DT.checkTableRow("botsDetailsDataTable", 1, ["50 291 842", "06.07.2021", "News", "", "English"]);
    DT.checkTableRow("botsDetailsDataTable", 2, ["50 487 296", "15.07.2021", "News", "", "English"]);
    DT.checkTableRow("botsDetailsDataTable", 3, ["50 684 027", "23.07.2021", "News", "", "English"]);

    //Back to index.html
    I.click(locate("span.seoPageTitle").withText("ThinkChaos 0.0"));

    I.waitForElement("#bots-pieVisits", 30);
    I.waitForElement("#bots-lineVisits", 30);
    I.waitForElement("#botsDataTable", 30);
});

Scenario("management-keywords", ({ I, DT, DTE }) => {
    I.amOnPage("/apps/seo/admin/management-keywords/");

    DT.filter("name", "Redakčný systém WebJET");

    DT.checkTableRow("managementKeywordsDataTable", 1, ["9", "Redakčný systém WebJET", "www.webjetcms.sk", "bing.com", "15.08.2023 08:37:35", "Tester Playwright", "5"]);
    DT.checkTableRow("managementKeywordsDataTable", 2, ["17", "Redakčný systém WebJET", "www.interway.sk", "bing.com", "15.08.2023 08:55:23", "Tester Playwright", "4"]);

    DT.filter("domain", "www.webjetcms.sk");
    I.dontSee("www.interway.sk");
    DT.checkTableRow("managementKeywordsDataTable", 1, ["9", "Redakčný systém WebJET", "www.webjetcms.sk", "bing.com", "15.08.2023 08:37:35", "Tester Playwright", "5"]);

    //Test create/update/delete

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("managementKeywordsDataTable");

    DTE.save();
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    let name = "autotest_" + randomNumber;
    I.fillField("#DTE_Field_name", name);
    I.fillField("#DTE_Field_domain", "www.interway.sk");
    I.fillField("#DTE_Field_searchBot", "google.sk");

    DTE.save();
    I.dontSee("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    DT.filter("name", name);
    DT.filter("domain", "www.interway.sk");
    DT.filter("searchBot", "google.sk");

    I.click(name);
    DTE.waitForEditor("managementKeywordsDataTable");
    I.fillField("#DTE_Field_name", name + "_change");
    DTE.save();

    DT.filter("name", name + "_change");
    I.see(name + "_change");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(name + "_change");
    I.dontSee(name);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario("stat-keywords", ({ I, DT, Browser }) => {
    if (Browser.isFirefox) {
        I.resizeWindow(1280, 900);
    }
    I.amOnPage("/apps/seo/admin/stat-keywords/");
    DT.waitForLoader();

    I.seeElement("#statKeywords-barKeywords");
    I.seeElement("#statKeywordsDataTable");

    setDates(I, "#statKeywordsDataTable_extfilter");

    I.amOnPage("/apps/seo/admin/stat-keywords/");
    DT.waitForLoader();

    checkDates(I);

    DT.filter("queryName", "archiv");

    DT.checkTableRow("statKeywordsDataTable", 1, ["1", "archiv", "27", "24,11"]);

    I.click("button.btn-vue-jstree-item-edit");
    I.click(locate(".jstree-anchor").withText("Jet portal 4"));

    waitForTableLoader(I);

    DT.checkTableRow("statKeywordsDataTable", 1, ["1", "archiv", "26", "29,89"]);

    setCustomDates(I, "#statKeywordsDataTable_extfilter", "10.07.2021", dayDateTo);

    DT.checkTableRow("statKeywordsDataTable", 1, ["3", "archiv", "11", "26,83"]);

    I.forceClick(locate("#statKeywordsDataTable td div.datatable-column-width a").withText("archiv"));

    I.waitForElement("#statKeywordsDetailsDataTable", 10);
    I.seeElement("#statKeywordsDetails-pieAccessCount");
    I.seeElement("#enginesCountDataTable");

    checkCustomDates(I, "10.07.2021", dayDateTo);

    DT.checkTableRow("statKeywordsDetailsDataTable", 1, ["1", "10.07.2021 02:26:15", "WebJET", "//??? docId=7047", "msnbot-40-77-167-63.search.msn.com"]);
    DT.checkTableRow("statKeywordsDetailsDataTable", 2, ["2", "11.07.2021 16:48:59", "WebJET", "//??? docId=7047", "msnbot-157-55-39-91.search.msn.com"]);
    DT.checkTableRow("statKeywordsDetailsDataTable", 3, ["3", "12.07.2021 05:07:46", "WebJET", "//??? docId=7047", "msnbot-157-55-39-167.search.msn.com"]);

    I.fillField("#searchQuery", "web stránka v cloude");
    I.click("Button.filtrujem.dt-filtrujem-query");

    DT.checkTableRow("statKeywordsDetailsDataTable", 1, ["1", "12.07.2021 13:32:11", "WebJET", "//??? docId=7047", "93-153-24-125.customers.tmcz.cz"]);
    DT.checkTableRow("statKeywordsDetailsDataTable", 2, ["2", "13.07.2021 15:34:36", "WebJET", "//??? docId=7047", "msnbot-40-77-167-38.search.msn.com"]);
    DT.checkTableRow("statKeywordsDetailsDataTable", 3, ["3", "14.07.2021 06:33:00", "WebJET", "//??? docId=7047", "msnbot-40-77-167-38.search.msn.com"]);

    setCustomDates(I, "#statKeywordsDetailsDataTable_extfilter", "27.07.2021 00:00",  "27.07.2021 15:00", false);

    DT.checkTableRow("statKeywordsDetailsDataTable", 1, ["1", "27.07.2021 09:10:29", "WebJET", "//??? docId=7047", "msnbot-207-46-13-165.search.msn.com"]);
    DT.checkTableRow("statKeywordsDetailsDataTable", 2, ["2", "27.07.2021 14:31:58", "WebJET", "//??? docId=7047", "msnbot-40-77-167-72.search.msn.com"]);
});

Scenario("positions", ({ I, DT }) => {
    I.amOnPage("/apps/seo/admin/positions/");

    DT.filter("name", "WebJET CMS");
    DT.filter("domain", "www.interway.sk");

    I.see("WebJET CMS");
    I.click("WebJET CMS");

    setDates(I, "#googlePoitionDataTable_extfilter", false);

    I.refreshPage();

    checkDates(I);

    I.see("08.07.2021");

    setCustomDates(I, "#googlePoitionDataTable_extfilter", "10.07.2021", dayDateTo, false);

    I.dontSee("08.07.2021");

    I.click(locate("span.seoPageTitle").withText("WebJET CMS"));

    DT.filter("name", "Redakčný systém WebJET");
    DT.filter("domain", "www.interway.sk");

    I.see("Redakčný systém WebJET");
    I.click("Redakčný systém WebJET");

    checkCustomDates(I, "10.07.2021", dayDateTo);

    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 30);

    setDates(I, "#googlePoitionDataTable_extfilter", false);

    I.waitForText("04.07.2021", 20);
    I.waitForText("06.07.2021", 20);

    //There was problem with encoding, so we need to check it
    I.seeElement(locate("span.seoPageTitle").withText("Redakčný systém WebJET"));
});

Scenario("number-keywords", ({ I, Browser, DT }) => {
    if (Browser.isFirefox) {
        I.resizeWindow(1280, 900);
    }
    I.amOnPage("/apps/seo/admin/number-keywords/");

    DT.waitForLoader();

    I.click("button.btn-vue-jstree-item-edit");
    I.click(locate(".jstree-anchor").withText("Koreňový priečinok"));

    DT.filter("name", "rozpočet");

    DT.checkTableRow("numberKeywordsDataTable", 1, [null, "rozpočet", "2", "2", "2"]);

    I.click("button.btn-vue-jstree-item-edit");
    I.click(locate(".jstree-anchor").withText("Jet portal 4"));

    waitForTableLoader(I);

    DT.checkTableRow("numberKeywordsDataTable", 1, [null, "rozpočet", "1", "1", "1"]);

    I.amOnPage("/apps/seo/admin/number-keywords/");

    DT.waitForLoader();
    DT.filter("name", "rozpočet");

    DT.checkTableRow("numberKeywordsDataTable", 1, [null, "rozpočet", "1", "1", "1"]);

    I.click(locate("#numberKeywordsDataTable a").withText("rozpočet"));

    I.waitForText("Konsolidácia naprieč trhmi 05.11.2018 06:00", 20);
});

Scenario("Special cross pages (stat and seo section) ext filter test", ({ I, DT }) => {
    //
    I.amOnPage("/apps/stat/admin/search-engines/");
    DT.waitForLoader();
    cleanFilter(I, DT);

    setDates(I, "#searchEnginesQueryDataTable_extfilter", false);

        DT.checkTableRow("searchEnginesQueryDataTable", 1, ["1", "Redakčný systém WebJET CMS", "120", "4,30"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 2, ["2", "správa web stránok", "108", "3,87"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 3, ["3", "WebJET produkty", "105", "3,76"]);

        DT.checkTableRow("searchEnginesDataTable", 1, ["1", "WebJET", "4 105"]);
        DT.checkTableRow("searchEnginesDataTable", 2, ["2", "seznam.cz", "1 058"]);
        DT.checkTableRow("searchEnginesDataTable", 3, ["3", "google.com", "117"]);

    setCustomDates(I, "#searchEnginesQueryDataTable_extfilter", "10.07.2021", dayDateTo);

        DT.checkTableRow("searchEnginesQueryDataTable", 1, ["1", "Redakčný systém WebJET CMS", "101", "4,96"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 2, ["2", "správa web stránok", "89", "4,37"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 3, ["3", "WebJET", "87", "4,28"]);

        DT.checkTableRow("searchEnginesDataTable", 1, ["1", "WebJET", "2 940"]);
        DT.checkTableRow("searchEnginesDataTable", 2, ["2", "seznam.cz", "781"]);
        DT.checkTableRow("searchEnginesDataTable", 3, ["3", "google.com", "81"]);

    I.click({ css: "button[data-id=webPageSelect]" });
    I.dontSee("Jet portal 4 - testovacia stranka");

    I.click({ css: "button[data-id=searchEngineSelect]" });
    I.see("WebJET");
    I.see("google.com");
    I.see("seznam.cz");

    I.click("button.btn-vue-jstree-item-edit");
    I.click(locate(".jstree-anchor").withText("Jet portal 4"));

    I.click({ css: "button[data-id=webPageSelect]" });
    I.see("Jet portal 4 - testovacia stranka");

    I.click({ css: "button[data-id=searchEngineSelect]" });
    I.see("WebJET", "ul.dropdown-menu.show");
    I.dontSee("google.com", "ul.dropdown-menu.show");
    I.dontSee("seznam.cz", "ul.dropdown-menu.show");
    I.pressKey("Escape");

        DT.checkTableRow("searchEnginesQueryDataTable", 1, ["1", "Redakčný systém WebJET CMS", "101", "5,91"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 2, ["2", "správa web stránok", "89", "5,21"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 3, ["3", "formuláre", "76", "4,39"]);

        DT.checkTableRow("searchEnginesDataTable", 1, ["1", "WebJET", "2 865"]);
        I.dontSee("seznam.cz", "#searchEnginesDataTable");
        I.dontSee("google.com", "#searchEnginesDataTable");

    //
    I.amOnPage("/apps/seo/admin/number-keywords/");

    DT.filter("name", "rozpočet");
    DT.checkTableRow("numberKeywordsDataTable", 1, [null, "rozpočet", "1", "1", "1"]);

    I.click({ css: "button[data-id=webPageSelect]" });
    I.see("Jet portal 4 - testovacia stranka");
    I.click("Jet portal 4 - testovacia stranka");
    I.waitForInvisible("#loader", 20);
    DT.waitForLoader();

    I.waitForText("rozpočet", 10, "#numberKeywordsDataTable");
    DT.checkTableRow("numberKeywordsDataTable", 1, [null, "rozpočet", "0", "0", "0"]);

    //
    I.amOnPage("/apps/seo/admin/stat-keywords/");

    checkCustomDates(I, "10.07.2021", dayDateTo);

    I.click({ css: "button[data-id=webPageSelect]" });
    I.see("Jet portal 4 - testovacia stranka");

    I.click({ css: "button[data-id=searchEngineSelect]" });
    I.dontSee("WebJET");
    I.dontSee("google.com");
    I.dontSee("seznam.cz");

        //DT.checkTableRow("statKeywordsDataTable", 1, ["1", "intranetové riešenie", "0", "0,00"]);
        //DT.checkTableRow("statKeywordsDataTable", 2, ["2", "primátor", "0", "0,00"]);
        I.see("intranetové riešenie", "#statKeywordsDataTable");
        I.see("primátor", "#statKeywordsDataTable");

    I.click("button.btn-vue-jstree-item-edit");
    I.click(locate(".jstree-anchor").withText("Koreňový priečinok"));

    I.click({ css: "button[data-id=searchEngineSelect]" });
    I.see("WebJET");
    I.see("google.com");
    I.see("seznam.cz");

        DT.checkTableRow("statKeywordsDataTable", 1, ["1", "primátor", "12", "27,27"]);
        DT.checkTableRow("statKeywordsDataTable", 2, ["2", "web stránka v cloude", "12", "27,27"]);
        DT.checkTableRow("statKeywordsDataTable", 3, ["3", "archiv", "11", "25"]);

    I.click("seznam.cz");

        DT.checkTableRow("statKeywordsDataTable", 1, ["1", "intranetové riešenie", "4", "100,00"]);
        DT.checkTableRow("statKeywordsDataTable", 2, ["2", "primátor", "0", "0,00"]);

    //
    I.amOnPage("/apps/stat/admin/search-engines/");

        DT.checkTableRow("searchEnginesQueryDataTable", 1, ["1", "WebJET produkty", "85", "14,78"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 2, ["2", "WebJET", "69", "12,00"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 3, ["3", "CMS systém", "62", "10,78"]);

        DT.checkTableRow("searchEnginesDataTable", 1, ["1", "WebJET", "2 940"]);
        DT.checkTableRow("searchEnginesDataTable", 2, ["2", "seznam.cz", "781"]);
        DT.checkTableRow("searchEnginesDataTable", 3, ["3", "google.com", "81"]);

    I.click("button.btn-vue-jstree-item-edit");
    I.click(locate(".jstree-anchor").withText("English"));

        DT.checkTableRow("searchEnginesQueryDataTable", 1, ["1", "Hotels", "1", "25,00"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 2, ["2", "Tourism", "1", "25,00"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 3, ["3", "Education", "1", "25,00"]);
        DT.checkTableRow("searchEnginesQueryDataTable", 4, ["4", "fishing", "1", "25,00"]);

        DT.checkTableRow("searchEnginesDataTable", 1, ["1", "WebJET", "4"]);
        I.dontSee("seznam.cz");
        I.dontSee("google.com");

        I.click({ css: "button[data-id=searchEngineSelect]" });
        I.see("WebJET");
        I.dontSee("seznam.cz");
        I.dontSee("google.com");

    //
    I.amOnPage("/apps/seo/admin/");

    DT.waitForLoader();

        DT.checkTableRow("botsDataTable", 2, ["2", "Slackbot-LinkExpanding 1.0", "126", "51,43", "30.07.2021"]);
        DT.checkTableRow("botsDataTable", 3, ["3", "Microsoft 0.0", "72", "29,39", "30.07.2021"]);
});

Scenario("cleanup", ({ I, DT }) => {
    I.amOnPage("/apps/seo/admin/");
    cleanFilter(I, DT);
});