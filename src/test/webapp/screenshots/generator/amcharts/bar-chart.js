Feature('stat.serach-engines');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot bar chart from search engines', ({I, DTE, Document}) => { 
    I.amOnPage("/apps/stat/admin/search-engines/");

    within("#searchEnginesQueryDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.01.2022");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    DTE.waitForLoader();

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    Document.screenshotElement("#graphsDiv", "/developer/frameworks/charts/frontend/bar-chart.png");
});