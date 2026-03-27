Feature('apps.form-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot bar chart from search engines', async ({I, Document}) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=stattestform");

    I.resizeWindow(1280, 900);
    await I.executeScript(() => {
        let allCharts = document.querySelectorAll(".stat-chart-wrapper");
        allCharts.forEach(chart => {
            if(chart.id !== "form-stats_select-1_container") chart.style.display = "none";
            else {
                chart.style.paddingBottom = "10px";

                const settingsBtn = chart.querySelector(".chart-more-btn");
                if(settingsBtn) settingsBtn.style.display = "none";
            }
        });
    });
    I.wait(1);

    Document.screenshotElement("#form-stats_select-1_container", "/developer/frameworks/charts/frontend/table-chart.png");
});
