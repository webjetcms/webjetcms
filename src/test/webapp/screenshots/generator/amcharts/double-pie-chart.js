Feature('reservation.reservation-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('screenshot double pie chart from reservation stat', ({I, Document}) => { 
    I.amOnPage("/apps/reservation/admin/reservation-stat/");

    I.waitForVisible("#monthFilter");
    I.waitForInvisible("#loader", 20);

    I.executeScript(function() {
        try {
            $("#monthFilter").val("2050-08");
            $("#monthFilter").change();
        }
        catch (e) {}
    });
    I.waitForInvisible("#loader", 20);

    I.resizeWindow(1920, 1080);

    Document.screenshotElement("#timeCharts > div:nth-child(1)", "/developer/frameworks/charts/frontend/double-pie-chart.png");
});