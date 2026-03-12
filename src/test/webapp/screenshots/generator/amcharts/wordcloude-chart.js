Feature('stat.search-engines');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot wordCloud chart from search engines', async ({I, DTE, Document}) => {
    // We go 2 times on same page on purpose, for better styling of chart

    I.resizeWindow(2700, 900);

    I.amOnPage("/apps/stat/admin/search-engines/");

    within("#searchEnginesQueryDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.11.2023");
        I.fillField({css: "input.dt-filter-to-dayDate"}, "30.03.2026");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    DTE.waitForLoader();

    I.amOnPage("/apps/stat/admin/search-engines/");

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    Document.screenshotElement("#graphsDiv div.col-md-12:nth-child(2)", "/developer/frameworks/charts/frontend/wordcloud-chart.png");
});