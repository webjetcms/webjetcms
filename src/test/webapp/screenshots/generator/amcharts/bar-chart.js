Feature('stat.serach-engines');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot bar chart from search engines', async ({I, DTE, Document}) => {
    I.amOnPage("/apps/stat/admin/search-engines/");

    await I.executeScript(function() {
        $("#graphsDiv").css("padding", "0px 10px 10px 10px");
    });

    within("#searchEnginesQueryDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.01.2022");
        I.fillField({css: "input.dt-filter-to-dayDate"}, "30.12.2023");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    DTE.waitForLoader();

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    Document.screenshotElement("#graphsDiv", "/developer/frameworks/charts/frontend/bar-chart.png");
});