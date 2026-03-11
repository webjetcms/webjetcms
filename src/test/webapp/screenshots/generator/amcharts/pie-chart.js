Feature('stat.top');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot pie chart from referer', async ({I, DTE, Document}) => {
    I.amOnPage("/apps/stat/admin/referer/");

    I.clickCss("#editorApprootDir > section > div > div > div > div > button");
    I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Jet portal 4").find('.jstree-icon.jstree-ocl'));
    I.click(locate('a.jstree-anchor').withText("Úvodná stránka"));
    DTE.waitForLoader();

    within("#refererDataTable_extfilter", () => {
        I.fillField({css: "input.dt-filter-from-dayDate"}, "01.01.2022");
        I.fillField({css: "input.dt-filter-to-dayDate"}, "30.12.2023");
        I.click({css: "button.dt-filtrujem-dayDate"});
    });
    DTE.waitForLoader();

    //pockaj na loader pre grafy
    I.waitForInvisible("#loader", 20);

    await I.executeScript(function() {
        $("#graphsDiv").css("padding", "0px 10px 10px 10px");
    });

    Document.screenshotElement("#graphsDiv", "/developer/frameworks/charts/frontend/pie-chart-donut.png");

    I.amOnPage("/apps/form/admin/form-stats/?formName=stattestform");
    I.resizeWindow(1280, 900);
    await I.executeScript(() => {
        let allCharts = document.querySelectorAll(".stat-chart-wrapper");
        allCharts.forEach(chart => {
            if(chart.id !== "form-stats_select-1_container") chart.style.display = "none";
            else chart.style.paddingBottom = "10px";
        });
    });
    I.wait(1);
    Document.screenshotElement("#form-stats_select-1_container", "/developer/frameworks/charts/frontend/pie-chart-classic.png");
});