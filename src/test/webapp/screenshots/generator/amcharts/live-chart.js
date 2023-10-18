Feature('stat.server-monitoring');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot live chart from server monitoring', async ({I, DTE, Document}) => { 
    I.amOnPage("/admin/v9/settings/server-monitoring-records/");

    await I.executeScript(function(){
        window.location.href="/admin/v9/settings/server-monitoring/";
    });

    //Let the data load
    I.wait(600);

    Document.screenshotElement("div.amchart-monitoring-server", "/developer/frameworks/charts/frontend/live-chart.png", 2000, 1500);
});