const SL = require("./shared-logic");

Feature('apps.reservation.price-logic');

Before(({ login }) => {
    login('admin');
});

const reservation_object_name = "reservation_object_prices_test";

Scenario('Reservation prices calculating logic', ({I, DTE}) => {
    I.say("Go create reservation and test set prices");
    I.amOnPage("/apps/reservation/admin/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    SL.verifyReservationPrice(I, reservation_object_name, "01.01.2045", "01.01.2045", "08:00", "09:00", "190");
    SL.verifyReservationPrice(I, reservation_object_name, "01.01.2045", "02.01.2045", "08:00", "15:00", "2660");
    SL.verifyReservationPrice(I, reservation_object_name, "03.01.2045", "03.01.2045", "08:00", "16:00", "1120");
    SL.verifyReservationPrice(I, reservation_object_name, "01.01.2045", "03.01.2045", "08:00", "16:00", "4160");
});

Scenario('Reservation price DISCOUNT by userGroup', ({I, DT, DTE}) => {
    const reservationObjectName = "Tenisovy kurt A";
    const adminDateFrom = "01.01.2045";
    const adminDateTo = "03.01.2045";
    const adminTimeFrom = "08:00";
    const adminTimeTo = "16:00";
    const userReservationDate = "01-01-2050";
    const slots = ["2560_13", "2560_14", "2560_15"];

    I.say("From start set 0% discount");
    SL.setDiscount(I, DT, DTE, 0);

    I.say("Check reservation prices without discount");

    checkAdminReservationPrice(I, DT, DTE, reservationObjectName, adminDateFrom, adminDateTo, adminTimeFrom, adminTimeTo, "2160");
    checkUserReservationPrice(I, userReservationDate, slots, 300);

    I.say("From start set 33% discount");
    SL.setDiscount(I, DT, DTE, 33);

    I.say("Check reservation prices WITH discount");
    checkAdminReservationPrice(I, DT, DTE, reservationObjectName, adminDateFrom, adminDateTo, adminTimeFrom, adminTimeTo, "1447.2");
    checkUserReservationPrice(I, userReservationDate, slots, 201);
});

Scenario('Remove discount', ({I, DT, DTE}) => {
    SL.setDiscount(I, DT, DTE, 0);
});

function checkUserReservationPrice(I, reservationDate, slots, expectedPrice) {
    I.amOnPage("/apps/spring-app/rezervacie/");
    I.fillField("#reservationDate", reservationDate);
    for (const slot of slots) {
        I.click(locate(`td[id='${slot}'].free`));
    }
    I.seeInField("#price", expectedPrice);
}

function checkAdminReservationPrice(I, DT, DTE, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo, expectedPrice) {
    I.amOnPage("/apps/reservation/admin/");
    DT.waitForLoader();
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("reservationDataTable");
    SL.setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo);
    I.seeInField("#DTE_Field_price", expectedPrice);
}