Feature('reservation.reservations');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

//Zakladne testy na preverenie logiky prelínania sa pri rezerváciach
Scenario('Reservation validation tests + delete logic', async ({I, DTE}) => {
    let reservationObjectName = "autotest-reservation_object-" + randomNumber;
    let succMsgInfo = 'Vami zadanú rezerváciu je možné uložiť.';
    let maxResCountErr = 'Bol dosiahnutý maximalný počet rezervácií pre zvolené obdobie a rezervačný objekt.';
    let deletePassword = "right_password";

    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationObjectDataTable");

    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", reservationObjectName);

    //Not important but required
    I.click("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Random text");

    //Set max reservation count to 2
    I.click("#DTE_Field_maxReservations");
    I.fillField("#DTE_Field_maxReservations", 2);

    //Set default time
    I.click("#DTE_Field_reservationTimeFrom");
    I.fillField("#DTE_Field_reservationTimeFrom", "08:00");
    I.pressKey('Enter');

    I.click("#DTE_Field_reservationTimeTo");
    I.fillField("#DTE_Field_reservationTimeTo", "16:00");
    I.pressKey('Enter');

    //Set specific time for specific days of week
    I.click('#pills-dt-reservationObjectDataTable-chooseDays-tab');

        //Tuesday
        I.click('#DTE_Field_editorFields-chooseDayB_0');

        I.click("#DTE_Field_editorFields-reservationTimeFromB");
        I.fillField("#DTE_Field_editorFields-reservationTimeFromB", "08:00");
        I.pressKey('Enter');

        I.click("#DTE_Field_editorFields-reservationTimeToB");
        I.fillField("#DTE_Field_editorFields-reservationTimeToB", "16:00");
        I.pressKey('Enter');

        //Wednesday
        I.click('#DTE_Field_editorFields-chooseDayC_0');

        I.click("#DTE_Field_editorFields-reservationTimeFromC");
        I.fillField("#DTE_Field_editorFields-reservationTimeFromC", "09:00");
        I.pressKey('Enter');

        I.click("#DTE_Field_editorFields-reservationTimeToC");
        I.fillField("#DTE_Field_editorFields-reservationTimeToC", "15:00");
        I.pressKey('Enter');

        //Thursday
        I.click('#DTE_Field_editorFields-chooseDayD_0');

        I.click("#DTE_Field_editorFields-reservationTimeFromD");
        I.fillField("#DTE_Field_editorFields-reservationTimeFromD", "07:00");
        I.pressKey('Enter');

        I.click("#DTE_Field_editorFields-reservationTimeToD");
        I.fillField("#DTE_Field_editorFields-reservationTimeToD", "13:00");
        I.pressKey('Enter');

    I.click("#pills-dt-reservationObjectDataTable-advanced-tab");
        //Set delete password
        I.click("#DTE_Field_editorFields-addPassword_0");
        I.click("#DTE_Field_editorFields-newPassword");
        I.fillField("#DTE_Field_editorFields-newPassword", deletePassword);
        I.click("#DTE_Field_editorFields-passwordCheck");
        I.fillField("#DTE_Field_editorFields-passwordCheck", deletePassword);

    //Save this reservation obeject
    DTE.save();
    I.wait(1);

    //Go to reservation page
    I.amOnPage("/apps/reservation/admin/");

    //ADDING 1st reservation
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    setReservation(I, reservationObjectName, "05.02.2030", "07.02.2030", "09:10", "10:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.seeElement("#DTE_Field_editorFields-infoLabel2");
    I.wait(1);
    let infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, succMsgInfo);

    DTE.save();
    I.wait(1);

    //ADDING 2nd reservation
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    setReservation(I, reservationObjectName, "05.02.2030", "07.02.2030", "11:00", "12:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, succMsgInfo);

    DTE.save();
    I.wait(1);

    //ADDING 3rd reservation
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    setReservation(I, reservationObjectName, "05.02.2030", "07.02.2030", "09:30", "12:30");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, succMsgInfo);

    DTE.save();
    I.wait(1);

    //ADDING 4th reservation -- must FAILED
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    setReservation(I, reservationObjectName, "05.02.2030", "07.02.2030", "11:00", "12:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, maxResCountErr);

    //Try save
    DTE.save();
    //Check returned error
    I.see("Bol dosiahnutý maximalný počet rezervácií pre zvolené obdobie a rezervačný objekt.");

    //Set other params and ADDING 4th reservation
    setReservation(I, null, "07.02.2030", "07.02.2030", "08:00", "09:00");
    DTE.save();
    I.wait(1);

    //ADDING 5th reservation
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    setReservation(I, reservationObjectName, "07.02.2030", "07.02.2030", "08:00", "09:00");
    DTE.save();
    I.wait(1);

    // //ADDING 6th reservation -- MUST FAILED
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    setReservation(I, reservationObjectName, "07.02.2030", "07.02.2030", "08:00", "09:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, maxResCountErr);

    //Try save
    DTE.save();
    //Check returned error
    I.see("Bol dosiahnutý maximalný počet rezervácií pre zvolené obdobie a rezervačný objekt.");

    I.click("div.DTE_Header button.btn-close-editor");

    I.fillField("#reservationDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-text.dt-th-editorFields-selectedReservation > form > div > input", reservationObjectName);
    I.pressKey('Enter', "#reservationDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-text.dt-th-editorFields-selectedReservation > form > div > input");
    I.wait(1);

    //Delete all reservations bind with our object + delete logic test
    I.click("button.buttons-select-all");
    I.click("#reservationDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(4)");

    I.seeElement("#toast-container-webjet");
    I.see("Zadajte heslo");

    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-input > input");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", deletePassword + "_wrong");
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.seeElement("#reservationDataTable_modal > div > div");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.seeElement("#toast-container-webjet");
    I.see("Overenie hesla zlyhalo");
    I.click("#toast-container-webjet > div > button");

    I.click("button.buttons-select-all");
    I.click("#reservationDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(4)");

    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-input > input");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", deletePassword);
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.seeElement("#reservationDataTable_modal > div > div");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Go back to reservation objects and remove our object
    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    I.fillField("input.dt-filter-name", reservationObjectName);
    I.pressKey('Enter', "dt-filter-name");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

//Testy na preverenie vratenych error sprav
Scenario('Reservation error logic tests', async ({I, DTE}) => {
    let reservationObjectName = "autotest-reservation_object-" + randomNumber;
    let dateInPastErr = 'Zadaný dátum a čas rezervácie sa nachádza v minulosti.';
    let badDateRangeOrdErr = 'Dátum rezervácie "od", musí byť väčší alebo rovný ako dátum rezervácie "do".';
    let badTimeRangeOrdErr = 'Čas rezervácie "od", musí byť väčší ako čas rezervácie "do".';
    let timeRangeValidityError = 'Čas rezervácie pre zvolený dátum a čas je mimo rozsah, ktorý je daný rezervačným objektom.';
    let infoMsg = "";

    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationObjectDataTable");

    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", reservationObjectName);

    //Not important but required
    I.click("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Random text");

    //Set default time
    I.click("#DTE_Field_reservationTimeFrom");
    I.fillField("#DTE_Field_reservationTimeFrom", "08:00");
    I.pressKey('Enter');

    I.click("#DTE_Field_reservationTimeTo");
    I.fillField("#DTE_Field_reservationTimeTo", "16:00");
    I.pressKey('Enter');

    //Save this reservation obeject
    DTE.save();
    I.wait(1);

    //Go to reservation page
    I.amOnPage("/apps/reservation/admin/");

    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    //TEST 1 - Check Range in past Errror
    setReservation(I, reservationObjectName, "01.01.2000", "01.01.2000", "10:00", "11:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, dateInPastErr);

    //Try save if error msg will be shown
    DTE.save();
    //Check returned error
    I.see(dateInPastErr);

    //TEST 2 - Check bad date range order
    setReservation(I, null, "02.01.2030", "01.01.2030", "10:00", "11:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, badDateRangeOrdErr);

    //Try save if error msg will be shown
    DTE.save();
    //Check returned error
    I.see(badDateRangeOrdErr);

    //TEST 3 - Check bad time range order
    setReservation(I, null, "01.01.2030", "01.01.2030", "11:00", "10:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, badTimeRangeOrdErr);

    //Try save if error msg will be shown
    DTE.save();
    //Check returned error
    I.see(badTimeRangeOrdErr);


    //TEST 4 - Check bad time range validity (aka if set reservation time range is out of reservation_object time range)
    setReservation(I, null, "01.01.2030", "01.01.2030", "02:00", "03:00");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.wait(1);
    infoMsg = await I.grabValueFrom("#DTE_Field_editorFields-infoLabel2");

    I.assertEqual(infoMsg, timeRangeValidityError);

    //Try save if error msg will be shown
    DTE.save();
    //Check returned error
    I.see(timeRangeValidityError);

    I.click("div.DTE_Header button.btn-close-editor");

    //Go back to reservation objects and remove our object
    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    I.fillField("input.dt-filter-name", reservationObjectName);
    I.pressKey('Enter', "dt-filter-name");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

function setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo) {
    //Select our reservation object
    if(reservationObjectName !== null) {
        I.click("#panel-body-dt-reservationDataTable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_select.DTE_Field_Name_reservationObjectId > div.col-sm-7 > div.DTE_Field_InputControl > div > button");
        I.fillField("body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input", reservationObjectName);
        I.pressKey('Enter');
        I.wait(2);
    }

    //Set reservation date
    setReservationDate(I, dateFrom, dateTo);

    //Set reservation time
    setReservationTime(I, timeFrom, timeTo);
}

function setReservationDate(I, dateFrom, dateTo) {
    //From
    I.click("#DTE_Field_dateFrom");
    I.fillField("#DTE_Field_dateFrom", dateFrom);
    I.pressKey('Enter');
    I.wait(2);

    //To
    I.click("#DTE_Field_dateFrom");
    I.fillField("#DTE_Field_dateTo", dateTo);
    I.pressKey('Enter');
    I.wait(2);
}

function setReservationTime(I, timeFrom, timeTo) {
    I.click("#DTE_Field_editorFields-reservationTimeFrom");
    I.fillField("#DTE_Field_editorFields-reservationTimeFrom", timeFrom);
    I.pressKey('Enter');

    I.click("#DTE_Field_editorFields-reservationTimeTo");
    I.fillField("#DTE_Field_editorFields-reservationTimeTo", timeTo);
    I.pressKey('Enter');
}