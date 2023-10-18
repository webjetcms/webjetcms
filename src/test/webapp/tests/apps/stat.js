Feature('apps.stat');

Before(({ I, login }) => {
    login('admin');
});

function checkDates(I) {
    I.seeInField("div.md-breadcrumb input.dt-filter-from-dayDate", "01.05.2022");
    I.seeInField("div.md-breadcrumb input.dt-filter-to-dayDate", "31.05.2022");
}

function setDates(I) {
    I.amOnPage("/apps/stat/admin/");

    I.seeElement("#graphsDiv");
    I.seeElement("#tableDiv");

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    within("#statsDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.05.2022");
        I.fillField({css: "input.dt-filter-to-dayDate"}, "31.05.2022");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    I.dtWaitForLoader();
}

Scenario("visits", ({ I, DT }) => {

    setDates(I);

    I.amOnPage("/apps/stat/admin/");

    checkDates(I);

    DT.checkTableRow("statsDataTable", 2, ["30", "30.05.2022", "2", "2", "1"]);

    //change from stat days to stat weeks
    I.click("#statsDataTable_extfilter button[data-stat-type=weeks]");

    DT.checkTableRow("statsDataTable", 2, ["4", "2 022", "21", "448", "136", "1"]);

    within("#statsDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.05.2022");
        I.fillField({css: "input.dt-filter-to-dayDate"}, "31.07.2022");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    I.dtWaitForLoader();

    DT.checkTableRow("statsDataTable", 1, ["13", "2 022", "30", "533", "229", "1"]);

    //change from stat weeks to stat months
    I.click("#statsDataTable_extfilter button[data-stat-type=months]");

    DT.checkTableRow("statsDataTable", 1, ["3", "2 022", "7", "2 196", "943", "2"]);

    //change from stat months to hours months
    I.click("#statsDataTable_extfilter button[data-stat-type=hours]");

    DT.checkTableRow("statsDataTable", 1, ["1", "0", "159", "12", "3"]);

    setDates(I);
});

Scenario("top", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/top/");

    checkDates(I);

    DT.checkTableRow("topDataTable", 2, ["2", "/Jet portal 4/Úvodná stránka/Úvodná stránka", "260", "92", "1"]);
});

Scenario("country", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/country/");

    checkDates(I);

    DT.checkTableRow("countryDataTable", 2, ["2", "Slovensko", "7", "0,34"]);
});

Scenario("browser", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/browser/");

    checkDates(I);

    DT.checkTableRow("browserDataTable", 2, ["2", "Chrome 101.0", "macOS", "7", "0,33"]);
});

Scenario("search-engines", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/search-engines/");

    checkDates(I);

    DT.checkTableRow("searchEnginesQueryDataTable", 2, ["2", "interway", "3", "12,00"]);
    DT.checkTableRow("searchEnginesDataTable", 1, ["1", "WebJET", "25"]);

    I.forceClick("interway", "#searchEnginesQueryDataTable tbody");
    I.dtWaitForLoader();
    checkDates(I);
    I.seeInField("#searchQuery", "interway");
    DT.checkTableRow("searchEnginesDetailsDataTable", 2, ["2", "25.05.2022 08:33:12", "WebJET", "/Jet portal 4/Jet portal 4 - testovacia stranka", "109-230-50-58.dynamic.orange.sk"]);
});

Scenario("referer", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/referer/");

    checkDates(I);

    DT.checkTableRow("refererDataTable", 1, ["1", "iwcm.interway.sk", "7", "87,50"]);
    DT.checkTableRow("refererDataTable", 2, ["2", "www.netcraft.com", "1", "12,50"]);

});

Scenario("error", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/error/");

    checkDates(I);

    DT.checkTableRow("errorDataTable", 2, ["2", "2 022", "22", "/templates/aceintegration/jet/assets/fonts/geomanist/geomanist", "referer: http://demotest.webjetcms.sk/components/_common/combine", "14"]);
});

Scenario("logon-user", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/logon-user/");

    checkDates(I);

    DT.checkTableRow("logonUserDataTable", 2, ["2", "Áno", "WebJET Administrátor", "InterWay, a. s.", "", "3", "4"]);
    I.click("WebJET Administrátor");
    I.dtWaitForLoader();
    checkDates(I);
    I.see("WebJET Administrátor", "span.statUserName");
    DT.checkTableRow("logonUserDetailsDataTable", 2, ["2", "02.05.2022 09:56:06", "0", "localhost"]);
});

Scenario("logon-current", ({ I, DT }) => {
    setDates(I);

    I.amOnPage("/apps/stat/admin/logon-current/");

    //tu nevieme overist aktualne prihlaseneho pouzivatela
    I.see("Tester Playwright", "#actualLogonUserDataTable tbody");
    I.click("Tester Playwright", "#actualLogonUserDataTable tbody");
    I.dtWaitForLoader();
    checkDates(I);
    I.see("Tester Playwright", "span.statUserName");
    DT.checkTableRow("logonUserDetailsDataTable", 2, ["2", "26.05.2022 10:32:40", "0", "localhost"]);
});

