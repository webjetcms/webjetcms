const SL = require("./shared-logic");

Feature('apps.reservation.reservations');

var randomNumber;
const base_reservation_object = "reservation_base_tests";
const delete_password = "right_password";
const error_reservation_object = "reservation_error_tests";

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

//Zakladne testy na preverenie logiky prelínania sa pri rezerváciach
Scenario('Reservation validation tests + delete logic', ({I, DT, DTE}) => {
    let succMsgInfo = 'Vami zadanú rezerváciu je možné uložiť.';
    let maxResCountErr = 'Bol dosiahnutý maximalný počet rezervácií pre zvolené obdobie a rezervačný objekt.';

    //Go to reservation page
    I.amOnPage("/apps/reservation/admin/");

    I.say("ADDING 1st reservation");
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    SL.setReservation(I, base_reservation_object, "05.02.2030", "07.02.2030", "09:10", "10:00", succMsgInfo);
    DTE.save();

    I.say("ADDING 2nd reservation");
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    SL.setReservation(I, base_reservation_object, "05.02.2030", "07.02.2030", "11:00", "12:00", succMsgInfo);
    DTE.save();

    I.say("ADDING 3rd reservation");
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    SL.setReservation(I, base_reservation_object, "05.02.2030", "07.02.2030", "09:30", "12:30", succMsgInfo);
    DTE.save();

    I.say("ADDING 4th reservation -- must FAILED");
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    SL.setReservation(I, base_reservation_object, "05.02.2030", "07.02.2030", "11:00", "12:00", maxResCountErr);

    //Try save
    DTE.save();
    //Check returned error
    I.see("Bol dosiahnutý maximalný počet rezervácií pre zvolené obdobie a rezervačný objekt.");

    //Set other params and ADDING 4th reservation
    SL.setReservation(I, null, "07.02.2030", "07.02.2030", "08:00", "09:00");
    DTE.save();

    I.say("ADDING 5th reservation");
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    SL.setReservation(I, base_reservation_object, "07.02.2030", "07.02.2030", "08:00", "09:00");
    DTE.save();

    I.say("ADDING 6th reservation -- MUST FAILED");
    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    SL.setReservation(I, base_reservation_object, "07.02.2030", "07.02.2030", "08:00", "09:00", maxResCountErr);

    I.say('Try save entity and wait for returned error. Check if error is correct.');
    DTE.save();
    I.waitForText("Bol dosiahnutý maximalný počet rezervácií pre zvolené obdobie a rezervačný objekt.", 5);

    DTE.cancel();
    DT.filter("editorFields.selectedReservation", base_reservation_object);

    I.say("Delete all reservations bind with our object + delete logic test");
    deleteReservationsWitPassword(I, delete_password+ "_wrong");

    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.seeElement("#toast-container-webjet");
    I.see("Overenie hesla zlyhalo");
    I.clickCss("#toast-container-webjet > div > button");

    deleteReservationsWitPassword(I, delete_password);

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('remove reservations', async ({I, DT}) => {
    I.amOnPage("/apps/reservation/admin/");
    DT.filter("editorFields.selectedReservation", base_reservation_object);
    let rows = await I.getTotalRows();
    if(rows > 0) {
        deleteReservationsWitPassword(I, delete_password);
    }
});


//Testy na preverenie vratenych error sprav
Scenario('Reservation error logic tests', async ({I, DTE}) => {
    let dateInPastErr = 'Zadaný dátum a čas rezervácie sa nachádza v minulosti.';
    let badDateRangeOrdErr = 'Dátum rezervácie "od", musí byť väčší alebo rovný ako dátum rezervácie "do".';
    let badTimeRangeOrdErr = 'Čas rezervácie "od", musí byť väčší ako čas rezervácie "do".';
    let timeRangeValidityError = 'Čas rezervácie pre zvolený dátum a čas je mimo rozsah, ktorý je daný rezervačným objektom.';

    //Go to reservation page
    I.amOnPage("/apps/reservation/admin/");

    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationDataTable");

    I.say("TEST 1 - Check Range in past Error");
    await checkReservation(I, DTE, dateInPastErr, error_reservation_object, "01.01.2000", "01.01.2000", "10:00", "11:00");

    I.say("TEST 2 - Check bad date range order");
    await checkReservation(I, DTE, badDateRangeOrdErr, null, "02.01.2030", "01.01.2030", "10:00", "11:00");

    I.say("TEST 3 - Check bad time range order");
    await checkReservation(I, DTE, badTimeRangeOrdErr, null, "01.01.2030", "01.01.2030", "11:00", "10:00");

    I.say("TEST 4 - Check bad time range validity (aka if set reservation time range is out of reservation_object time range)");
    await checkReservation(I, DTE, timeRangeValidityError, null, "01.01.2030", "01.01.2030", "02:00", "03:00");
});

Scenario('Admin EDIT test with discount logic', ({I, DT, DTE}) => {
    I.say("Log as TESTER 2 and create reservation");
    I.say("TESTER 2 has 40% discount");

    I.relogin("tester3");
    I.amOnPage("/apps/reservation/admin/");
    DT.waitForLoader();
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("reservationDataTable");
    SL.setReservation(I, "Tenisovy kurt A", "01.01.2045", "01.01.2045", "13:00", "16:00");
    I.seeInField("#DTE_Field_price", 126); //40% discount
    //Price 126 bECAUSE this day has special price 35e for 30 minutes, so it's 35*6 = 210, 40% discount = 126 (discount work even with special prices)
    DTE.save();

    I.say("Relogin as ADMIN");
    I.relogin("admin");
    I.say("admin has 0% discount");

    I.say("Edit reservation by admin, discount must by STILL 40% - as user who created reservation");
    I.amOnPage("/apps/reservation/admin/");
    DT.waitForLoader();
    DT.filter("name", "Tester_L2");
    DT.filter("editorFields.selectedReservation", "Tenisovy kurt A");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.click("Tester_L2");
    DTE.waitForEditor("reservationDataTable");
    I.say("Change day to 10.01.2045");
    SL.setReservation(I, "Tenisovy kurt A", "10.01.2045", "10.01.2045", "13:00", "16:00");
    I.seeInField("#DTE_Field_price", 180); //STILL 40% discount
    DTE.save();

    I.say("Do a check");
    I.click("Tester_L2");
    DTE.waitForEditor("reservationDataTable");
    I.seeInField("#DTE_Field_price", 180); //STILL 40% discount
});

Scenario('remove reservations by object and user', async ({I, DT}) => {
    I.relogin("admin");
    I.amOnPage("/apps/reservation/admin/");
    DT.waitForLoader();

    DT.filter("name", "Tester_L2");
    DT.filter("editorFields.selectedReservation", "Tenisovy kurt A");

    let rows = await I.getTotalRows();
    if(rows > 0) {
        I.clickCss("button.buttons-select-all");
        I.clickCss("button.custom-buttons-remove");
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
});

Scenario('Domain test', ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/reservation/admin/");
    DT.filter("editorFields.selectedReservation", "TestDomain_webjet9_object");
    I.see("TestDomain_webjet9_object");
    DT.filter("editorFields.selectedReservation", "TestDomain_test23_object");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("reservationDataTable");
    I.waitForElement("div.DTE_Field_Name_reservationObjectId");
    I.click( locate("div.DTE_Field_Name_reservationObjectId").find("button.dropdown-toggle") );
    I.waitForElement("div.dropdown-menu.show");
    I.fillField(locate("div.dropdown-menu.show").find("input"), "TestDomain_test23_object");
    I.see("No results matched \"TestDomain_test23_object\"");
    I.pressKey('Escape');
    DTE.cancel();

    Document.switchDomain("test23.tau27.iway.sk");
    DT.filter("editorFields.selectedReservation", "TestDomain_webjet9_object");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filter("editorFields.selectedReservation", "TestDomain_test23_object");
    I.see("TestDomain_test23_object");
});

Scenario('logout', async ({I}) => {
    I.logout();
});

async function checkReservation(I, DTE, Error,  ...tableData) {
    const [reservationObjectName, dateFrom, dateTo, timeFrom, timeTo] = tableData;
    SL.setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo, Error);
    I.clickCss("#DTE_Field_editorFields-showReservationValidity_0");
    I.clickCss("#DTE_Field_editorFields-showReservationValidity_0");

    //Try save if error msg will be shown
    DTE.save();
    //Check returned error
    I.see(Error);
}

function deleteReservationsWitPassword(I, deletePassword) {
    I.clickCss("button.buttons-select-all");
    I.clickCss(".custom-buttons-remove");
    I.seeElement("#toast-container-webjet");
    I.see("Zadajte heslo");

    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-input > input");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", deletePassword);
    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.seeElement("#reservationDataTable_modal > div > div");
    I.click("Zmazať", "div.DTE_Action_Remove");
}