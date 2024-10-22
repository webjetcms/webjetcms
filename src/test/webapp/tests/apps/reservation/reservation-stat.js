Feature('apps.reservation.reservation-stat');

Before(({ login }) => {
    login('admin');
});

Scenario('Reservation stat section', ({I, DT}) => {
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

    I.say('Check table data - HOURS');
    DT.checkTableRow("reservationStatDataTable", 1, ["Duch Phantom", "Tenisovy kurt A", "1", "200,00", "1", "1,00", "2", "2,00"]);
    DT.checkTableRow("reservationStatDataTable", 2, ["Tester Playwright", "statReservation_C", "3", "1 055,88", "5", "1,67", "22", "4,40"]);
    DT.checkTableRow("reservationStatDataTable", 3, ["Tester Playwright", "statReservation_A", "3", "1 152,00", "7", "2,33", "32", "4,57"]);
    DT.checkTableRow("reservationStatDataTable", 4, ["Tester2 Playwright2", "statReservation_C", "2", "576,00", "4", "2,00", "12", "3,00"]);
    DT.checkTableRow("reservationStatDataTable", 5, ["TestSivan WebJet-ovi", "Tenisovy kurt A", "1", "300,00", "1", "1,00", "3", "3,00"]);

    I.say("Check HOURS chart are visible, DAYS chart are not visible");
    I.see("Pomer počtu rezervácií a rezervovaných hodín jednotlivými používateľmi");
    I.waitForVisible("#reservationStat-doublePieTimeUsers");
    I.see("Pomer počtu rezervácií a rezervovaných hodín jednotlivých rezervačných objektov");
    I.waitForVisible("#reservationStat-doublePieTimeObjects");
    I.see("Celkový počet rezervovaných hodín za jednotlivé dni");
    I.waitForVisible("#reservationStat-lineDays");

    I.dontSee("Pomer počtu rezervácií a rezervovaných dní jednotlivými používateľmi");
    I.waitForInvisible("#reservationStat-doublePieDayUsers");
    I.dontSee("Pomer počtu rezervácií a rezervovaných dní jednotlivých rezervačných objektov");
    I.waitForInvisible("#reservationStat-doublePieDayObjects");
    I.dontSee("Celkový počet celodenných rezervácií na daný deň");

    I.say("Change hours chart to days chart");
    I.clickCss("button#typeDays");
    I.waitForInvisible("#loader", 20);

    I.say("Check DAYS chart are visible, HOURS chart are not visible");
    I.dontSee("Pomer počtu rezervácií a rezervovaných hodín jednotlivými používateľmi");
    I.waitForInvisible("#reservationStat-doublePieTimeUsers");
    I.dontSee("Pomer počtu rezervácií a rezervovaných hodín jednotlivých rezervačných objektov");
    I.waitForInvisible("#reservationStat-doublePieTimeObjects");
    I.dontSee("Celkový počet rezervovaných hodín za jednotlivé dni");

    I.see("Pomer počtu rezervácií a rezervovaných dní jednotlivými používateľmi");
    I.waitForVisible("#reservationStat-doublePieDayUsers");
    I.see("Pomer počtu rezervácií a rezervovaných dní jednotlivých rezervačných objektov");
    I.waitForVisible("#reservationStat-doublePieDayObjects");
    I.see("Celkový počet celodenných rezervácií na daný deň");
    I.waitForVisible("#reservationStat-lineDays");

    I.say('Check table data - DAYS');
    DT.checkTableRow("reservationStatDataTable", 1, ["Tester Playwright", "statReservation_D", "3", "688,00", "8", "2,67"]);
    DT.checkTableRow("reservationStatDataTable", 2, ["Tester Playwright", "statReservation_B", "2", "572,00", "4", "2,00"]);
    DT.checkTableRow("reservationStatDataTable", 3, ["Tester2 Playwright2", "statReservation_D", "1", "430,00", "5", "5,00"]);
});