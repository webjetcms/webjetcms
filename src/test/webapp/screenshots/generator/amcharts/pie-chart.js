Feature('stat.top');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot pie chart from referer', ({I, DTE, Document}) => { 
    I.amOnPage("/apps/stat/admin/referer/");

    I.clickCss("#editorApprootDir > section > div > div > div > div > button");
    I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Jet portal 4").find('.jstree-icon.jstree-ocl'));
    I.click(locate('a.jstree-anchor').withText("Úvodná stránka"));
    DTE.waitForLoader();

    within("#refererDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.01.2022");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    DTE.waitForLoader();

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    Document.screenshotElement("#graphsDiv", "/developer/frameworks/charts/frontend/pie-chart.png");
});