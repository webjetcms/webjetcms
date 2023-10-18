Feature('stat.admin');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot line chart from admin', ({I, DTE, Document}) => { 
    I.amOnPage("/apps/stat/admin/");

    within("#statsDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.01.2022");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    DTE.waitForLoader();

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    Document.screenshotElement("#graphsDiv", "/developer/frameworks/charts/frontend/line-chart.png");

});