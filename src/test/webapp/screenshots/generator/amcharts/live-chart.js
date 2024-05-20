Feature('stat.server-monitoring');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot live chart from server monitoring', async ({I, Document}) => { 
    I.amOnPage("/apps/server_monitoring/admin/records/");

    await I.executeScript(function(){
        window.location.href="/apps/server_monitoring/admin/";
    });

    //Let the data load
    I.wait(600);

    Document.screenshotElement("div.amchart-monitoring-server", "/developer/frameworks/charts/frontend/live-chart.png", 2000, 1500);
});