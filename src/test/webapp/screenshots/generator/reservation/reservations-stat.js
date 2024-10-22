Feature('reservation.reservations-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('reservations-stat screenshots', async ({ I, DT, Document }) => {
    I.amOnPage("/apps/reservation/admin/reservation-stat/");

    I.waitForVisible("#monthFilter");
    I.waitForInvisible("#loader", 20);

    I.say("Set date filter");

   I.executeScript(function() {
        try {
            $("#monthFilter").val("2050-08");
            $("#monthFilter").change();
        }
        catch (e) {}
    });
    I.waitForInvisible("#loader", 20);
    DT.waitForLoader();

    Document.screenshotElement("ul#pills-reservationStat", "/redactor/apps/reservation/reservations-stat/extfilter.png");

    Document.screenshot("/redactor/apps/reservation/reservations-stat/datatable_hours.png", 1900, 1300);
    I.clickCss("button#typeDays");
    I.waitForInvisible("#loader", 20);
    DT.waitForLoader();
    I.clickCss("button#typeDays");
    I.waitForInvisible("#loader", 20);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/reservation/reservations-stat/datatable_days.png", 1900, 1400);
});