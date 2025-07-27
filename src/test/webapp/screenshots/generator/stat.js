Feature('stat');

let confLng = "sk";

Before(({ I, login }) => {
    login('admin');

    confLng = I.getConfLng();
});

Scenario('visits', ({ I, DT, Document }) => {

    I.amOnPage("/apps/stat/admin/");
    DT.waitForLoader("statsDataTable");

    if("sk" === confLng) {
        I.fillField("#statsDataTable_extfilter input.dt-filter-from-dayDate", "31.5.2022");
        I.fillField("#statsDataTable_extfilter input.dt-filter-to-dayDate", "30.6.2022");
    } else if("en" === confLng) {
        I.fillField("#statsDataTable_extfilter input.dt-filter-from-dayDate", "05/31/2022");
        I.fillField("#statsDataTable_extfilter input.dt-filter-to-dayDate", "06/30/2022");
    }

    I.clickCss("#statsDataTable_extfilter button.dt-filtrujem-dayDate");
    DT.waitForLoader("statsDataTable");

    //Days page
    Document.screenshot("/redactor/apps/stat/stats-page.png", 1280, 1270);

    //Ext filter
    Document.screenshotElement("#pills-dateRange-tab", "/redactor/apps/stat/stats-extFilter.png");

    //
    Document.screenshotElement("#statType", "/redactor/apps/stat/stats-statType.png");

    //
    Document.screenshotElement("#editorApprootDir", "/redactor/apps/stat/stats-domainSelect.png");

    //
    Document.screenshotElement("#statsDataTable_extfilter > div > div.col-auto div.form-switch", "/redactor/apps/stat/stats-filterBotsOut.png");
});

Scenario('stat - page screenshots', ({ I, DT, Document }) => {

    I.amOnPage("/apps/stat/admin/top/");
    DT.waitForLoader();

    if("sk" === confLng) {
        I.fillField("#topDataTable_extfilter input.dt-filter-from-dayDate", "31.5.2022");
        I.fillField("#topDataTable_extfilter input.dt-filter-to-dayDate", "30.6.2022");
    } else if("en" === confLng) {
        I.fillField("#topDataTable_extfilter input.dt-filter-from-dayDate", "05/31/2022");
        I.fillField("#topDataTable_extfilter input.dt-filter-to-dayDate", "06/30/2022");
    }

    I.clickCss("#topDataTable_extfilter button.dt-filtrujem-dayDate");
    DT.waitForLoader("topDataTable");

    //Top page
    Document.screenshot("/redactor/apps/stat/top-page.png");

    visitAndScreenshot(I, DT, Document, "search-engines");
    visitAndScreenshot(I, DT, Document, "error");
    visitAndScreenshot(I, DT, Document, "logon-user");
    visitAndScreenshot(I, DT, Document, "logon-current");
});

function visitAndScreenshot(I, DT, Document, name) {
    I.amOnPage("/apps/stat/admin/"+name+"/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/stat/"+name+"-page.png");
}

Scenario('page screenshots-vesmir', ({ I, DT, Document }) => {

    I.say("POZOR: toto treba spustit na DB ktora ma data, napr. vesmir2022_web");
    I.say("POZOR: toto treba spustit na DB ktora ma data, napr. vesmir2022_web");
    I.say("POZOR: toto treba spustit na DB ktora ma data, napr. vesmir2022_web");
    I.say("POZOR: toto treba spustit na DB ktora ma data, napr. vesmir2022_web");
    I.say("POZOR: toto treba spustit na DB ktora ma data, napr. vesmir2022_web");
    I.say("POZOR: toto treba spustit na DB ktora ma data, napr. vesmir2022_web");
    I.say("POZOR: toto treba spustit na DB ktora ma data, napr. vesmir2022_web");


    I.amOnPage("/apps/stat/admin/top/");
    DT.waitForLoader();
    if("en" === confLng) {
        I.fillField("#topDataTable_extfilter input.dt-filter-from-dayDate", "05/31/2022");
        I.fillField("#topDataTable_extfilter input.dt-filter-to-dayDate", "06/30/2022");
    } else {
        I.fillField("#topDataTable_extfilter input.dt-filter-from-dayDate", "31.5.2022");
        I.fillField("#topDataTable_extfilter input.dt-filter-to-dayDate", "30.6.2022");
    }
    I.clickCss("#topDataTable_extfilter button.dt-filtrujem-dayDate");
    DT.waitForLoader("topDataTable");

    //dalsie stranky
    visitAndScreenshot(I, DT, Document, "country");
    visitAndScreenshot(I, DT, Document, "browser");
    visitAndScreenshot(I, DT, Document, "referer");
});

Scenario("ext filter - screenshots", ({ I, Document }) => {
    I.amOnPage("/apps/seo/admin/stat-keywords/");

    if("sk" === confLng) {
        I.fillField("#statKeywordsDataTable_extfilter input.dt-filter-from-dayDate", "01.07.2021");
        I.fillField("#statKeywordsDataTable_extfilter input.dt-filter-to-dayDate", "30.07.2021");
    } else if("en" === confLng) {
        I.fillField("#statKeywordsDataTable_extfilter input.dt-filter-from-dayDate", "07/01/2021");
        I.fillField("#statKeywordsDataTable_extfilter input.dt-filter-to-dayDate", "30/07/2021");
    }

    Document.screenshotElement("div.md-breadcrumb", "/redactor/apps/stat/ext-filter-1.png");

    I.amOnPage("/apps/stat/admin/");

    Document.screenshot("/redactor/apps/stat/ext-filter-2.png", 1500, 800);

    I.amOnPage("/apps/stat/admin/search-engines/");

    Document.screenshotElement("#searchEnginesQueryDataTable_extfilter > div > div:nth-child(4)", "/redactor/apps/stat/ext-filter-searchEngineSelect.png");

    Document.screenshotElement("#searchEnginesQueryDataTable_extfilter > div > div:nth-child(5)", "/redactor/apps/stat/ext-filter-webPageSelect.png");
});

Scenario("Obmedzene prava na adresare", async ({ I, Document }) => {
    I.relogin("tester2");
    I.amOnPage("/apps/stat/admin?removePerm=cmp_stat_seeallgroups");

    Document.screenshotElement("#editorApprootDir", "/redactor/apps/stat/stats-groupSelect.png");

    I.clickCss("#editorApprootDir > section > div > div > div > div > button.btn-vue-jstree-item-edit");

    Document.screenshotElement(locate('//*[@id="1"]/a/i'), "/redactor/apps/stat/groupNonActiveIcon.png");

    I.clickCss('//*[@id="1"]/i');

    Document.screenshotElement("div#jsTree > ul.jstree-container-ul", "/redactor/apps/stat/groupSelect_noAllRights.png");
});
