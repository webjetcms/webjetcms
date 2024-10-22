Feature('apps.stat');

Before(({ I, login }) => {
    login('admin');
});

function checkDates(I, dateFrom, dateTo) {
    I.seeInField("div.md-breadcrumb input.dt-filter-from-dayDate", dateFrom);
    I.seeInField("div.md-breadcrumb input.dt-filter-to-dayDate", dateTo);
}

function setDates(I, dateFrom, dateTo) {
    I.amOnPage("/apps/stat/admin/");

    I.seeElement("#graphsDiv");
    I.seeElement("#tableDiv");

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    within("#statsDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, dateFrom);
        I.fillField({css: "input.dt-filter-to-dayDate"}, dateTo);
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    I.dtWaitForLoader();
}

Scenario("visits", ({ I, DT }) => {

    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("statsDataTable", 1, ["31", "31.05.2022", "77", "25", "1"]);
    DT.checkTableRow("statsDataTable", 2, ["30", "30.05.2022", "2", "2", "1"]);

    //change from stat days to stat weeks
    I.click("#statsDataTable_extfilter button[data-stat-type=weeks]");

    DT.checkTableRow("statsDataTable", 1, ["6", "2 022", "22", "79", "27", "1"]);
    DT.checkTableRow("statsDataTable", 2, ["5", "2 022", "21", "410", "124", "1"]);

    within("#statsDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.05.2022");
        I.fillField({css: "input.dt-filter-to-dayDate"}, "31.07.2022");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    I.dtWaitForLoader()

    DT.checkTableRow("statsDataTable", 2, ["13", "2 022", "29", "583", "217", "1"]);
    DT.checkTableRow("statsDataTable", 3, ["12", "2 022", "28", "637", "294", "1"]);

    //change from stat weeks to stat months
    I.click("#statsDataTable_extfilter button[data-stat-type=months]");

    DT.checkTableRow("statsDataTable", 1, ["3", "2 022", "7", "2 197", "944", "2"]);

    //change from stat months to hours months
    I.click("#statsDataTable_extfilter button[data-stat-type=hours]");

    DT.checkTableRow("statsDataTable", 1, ["1", "0", "159", "12", "3"]);
});

Scenario("top", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/top/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("topDataTable", 2, ["2", "/Jet portal 4/Úvodná stránka/Úvodná stránka", "260", "92", "1"]);
});

Scenario("country", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/country/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("countryDataTable", 2, ["2", "Slovensko", "7", "0,34"]);
});

Scenario("browser", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/browser/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("browserDataTable", 2, ["2", "Chrome 101.0", "macOS", "7", "0,33"]);
});

Scenario("search-engines", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/search-engines/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("searchEnginesQueryDataTable", 1, ["1", "kontakt", "8", "32,00"]);
    DT.checkTableRow("searchEnginesQueryDataTable", 2, ["2", "interway", "3", "12,00"]);

    DT.checkTableRow("searchEnginesDataTable", 1, ["1", "WebJET", "25"]);

    I.forceClick("interway", "#searchEnginesQueryDataTable tbody");
    I.dtWaitForLoader();
    checkDates(I, "01.05.2022", "31.05.2022");
    I.seeInField("#searchQuery", "interway");
    DT.checkTableRow("searchEnginesDetailsDataTable", 2, ["2", "25.05.2022 08:33:12", "WebJET", "/Jet portal 4/Jet portal 4 - testovacia stranka", "109-230-50-58.dynamic.orange.sk"]);
});

Scenario("referer", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/referer/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("refererDataTable", 1, ["1", "iwcm.interway.sk", "7", "87,50"]);
    DT.checkTableRow("refererDataTable", 2, ["2", "www.netcraft.com", "1", "12,50"]);

});

Scenario("error", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/error/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("errorDataTable", 2, ["2", "2 022", "22", "/templates/aceintegration/jet/assets/fonts/geomanist/geomanist", "referer: http://demotest.webjetcms.sk/components/_common/combine", "14"]);
});

Scenario("logon-user", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/logon-user/");

    checkDates(I, "01.05.2022", "31.05.2022");

    DT.checkTableRow("logonUserDataTable", 2, ["2", "Áno", "WebJET Administrátor", "InterWay, a. s.", "", "3", "4"]);
    I.click("WebJET Administrátor");
    I.dtWaitForLoader();
    checkDates(I, "01.05.2022", "31.05.2022");
    I.see("WebJET Administrátor", "span.statUserName");
    DT.checkTableRow("logonUserDetailsDataTable", 2, ["2", "02.05.2022 09:56:06", "0", "localhost"]);
});

Scenario("logon-current", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.amOnPage("/apps/stat/admin/logon-current/");

    //tu nevieme overist aktualne prihlaseneho pouzivatela
    I.see("Tester Playwright", "#actualLogonUserDataTable tbody");
    I.click("Tester Playwright", "#actualLogonUserDataTable tbody");
    I.dtWaitForLoader();
    checkDates(I, "01.05.2022", "31.05.2022");
    I.see("Tester Playwright", "span.statUserName");
    DT.checkTableRow("logonUserDetailsDataTable", 2, ["2", "26.05.2022 10:32:40", "0", "localhost"]);
});

Scenario("ext-filter behavior", ({ I, DT }) => {
    setDates(I, "01.05.2022", "31.05.2022");

    I.click(locate('.ext-filter-out > .custom-control.form-switch'));
    I.dtWaitForLoader();

    DT.checkTableRow("statsDataTable", 1, ["31", "31.05.2022", "0", "0", "0"]);
    DT.checkTableRow("statsDataTable", 2, ["30", "30.05.2022", "0", "0", "0"]);

    I.amOnPage("/apps/stat/admin/search-engines/");
    checkDates(I, "01.05.2022", "31.05.2022");

    within("#searchEnginesQueryDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "");
        I.fillField({css: "input.dt-filter-to-dayDate"}, "18.05.2022");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    I.dtWaitForLoader();

    I.amOnPage("/apps/stat/admin/");

    checkDates(I, "", "18.05.2022");
    DT.checkTableRow("statsDataTable", 1, ["31", "18.05.2022", "3", "2", "2"]);
    DT.checkTableRow("statsDataTable", 2, ["30", "17.05.2022", "4", "1", "1"]);

});

Scenario("stat-groupTree-perms", async ({ I }) => {
    I.say("Overenie prednastaveneho ROOT priečinka keď uživateľ nema obmedzene priečinky.");
    I.relogin("tester");
    I.amOnPage("/apps/stat/admin/");
    I.see("Prihlásenia", "div.md-main-menu");
    I.see("Aktuálni návštevníci", "div.md-main-menu");
    const caseA = await I.grabValueFrom("#editorApprootDir > section > div > div > div > div > input");
    I.assertEqual("Všetky (zo všetkých domén)", caseA);
    I.clickCss("#editorApprootDir > section > div > div > div > div > button.btn-vue-jstree-item-edit");
    I.say("Over zobraznie ROOT priečinka pri výbere.");
    I.seeElement( locate("div#jsTree > ul.jstree-container-ul li.jstree-node > a.jstree-anchor").withText("Koreňový priečinok") );

    I.say("Overenie že sa nenaství ROOT priečinok keď uživateľ má obmedzené priečinky.");
    I.relogin("tester2");
    I.amOnPage("/apps/stat/admin?removePerm=cmp_stat_seeallgroups");
    I.dontSee("Prihlásenia", "div.md-main-menu");
    I.dontSee("Aktuálni návštevníci", "div.md-main-menu");
    const caseB = await I.grabValueFrom("#editorApprootDir > section > div > div > div > div > input");
    I.say(caseB);
    I.assertEqual("Test podadresar", caseB);
    I.clickCss("#editorApprootDir > section > div > div > div > div > button.btn-vue-jstree-item-edit");
    I.say("Over NEzobrazenie ROOT priečinka pri výbere.");
    I.dontSeeElement( locate("div#jsTree > ul.jstree-container-ul > li.jstree-node > a.jstree-anchor").withText("Koreňový priečinok") );
    I.seeElement( locate("div#jsTree > ul.jstree-container-ul > li.jstree-node.jstree-closed > a.jstree-anchor").withText("Jet portal 4") );
    I.say("Check the icons");
    I.seeElement( locate('//*[@id="1"]/a').withChild("i.jstree-icon.ti-folder-x") );
    I.seeElement( locate('//*[@id="67"]/a').withChild("i.jstree-icon.ti-folder-filled") );

    I.say("Check that folder without permision (the one with X icon) can't be selected");
    //Nothing should happen
    I.click( locate("div#jsTree > ul.jstree-container-ul > li.jstree-node.jstree-closed > a.jstree-anchor").withText("Jet portal 4") );
    I.seeElement("div#jsTree");

    I.say("Overenie prednastaveneho ROOT priečinka keď uživateľ ma obmedzene priečinky ale MA pravo vidieť všetky v stat");
    I.relogin("tester2");
    I.amOnPage("/apps/stat/admin/");
    I.dontSee("Prihlásenia", "div.md-main-menu");
    I.dontSee("Aktuálni návštevníci", "div.md-main-menu");
    const caseC = await I.grabValueFrom("#editorApprootDir > section > div > div > div > div > input");
    I.assertEqual("Všetky (zo všetkých domén)", caseC);
    I.clickCss("#editorApprootDir > section > div > div > div > div > button.btn-vue-jstree-item-edit");
    I.say("Over zobraznie ROOT priečinka pri výbere.");
    I.seeElement( locate("div#jsTree > ul.jstree-container-ul li.jstree-node > a.jstree-anchor").withText("Koreňový priečinok") );

    I.say("Check, that cmp_stat_seeallgroups permission wont work in another section than STAT. Check it using webpages tree");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.dontSeeElement( locate("div#SomStromcek > ul.jstree-container-ul > li.jstree-node > a.jstree-anchor").withText("Koreňový priečinok") );
});


Scenario('testovanie app - Monitoring kliknutí', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Monitoring kliknutí', '#components-stat-title');

    const defaultParams = {
        container: 'div.mainContainer'
    };

    await Apps.assertParams(defaultParams);

    Apps.openAppEditor();

    const changedParams = {
        container: 'body'
    };

    DTE.fillField('container', changedParams.container);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);
});


Scenario("logoff", ({ I }) => {
    I.logout();
});