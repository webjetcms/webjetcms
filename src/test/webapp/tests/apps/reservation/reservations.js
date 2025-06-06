const SL = require("./shared-logic");

Feature('apps.reservation.reservations');

let randomNumber;
const base_reservation_object = "reservation_base_tests";
const delete_password = "right_password";
const error_reservation_object = "reservation_error_tests";

let dateInPastErr = 'Zadaný dátum a čas rezervácie sa nachádza v minulosti.';
let badDateRangeOrdErr = 'Dátum rezervácie "od", musí byť väčší alebo rovný ako dátum rezervácie "do".';
let badTimeRangeOrdErr = 'Čas rezervácie "od", musí byť väčší ako čas rezervácie "do".';
let timeRangeValidityError = 'Čas rezervácie pre zvolený dátum a čas je mimo rozsah, ktorý je daný rezervačným objektom.';

Before(({ I, login , DT}) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        DT.addContext('reservation','#reservationDataTable_wrapper');
    }
});

Scenario('remove reservations', async ({I, DT}) => {
    I.amOnPage("/apps/reservation/admin/");
    DT.filterContains("editorFields.selectedReservation", base_reservation_object);
    let rows = await I.getTotalRows();
    if(rows > 0) {
        I.clickCss("button.buttons-select-all");
        deleteReservationsWitPassword(I, delete_password);
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
    DT.filterContains("editorFields.selectedReservation", base_reservation_object);

    I.say("Delete all reservations bind with our object + delete logic test");
    I.clickCss("button.buttons-select-all");
    deleteReservationsWitPassword(I, delete_password+ "_wrong");

    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.seeElement("#toast-container-webjet");
    I.see("Overenie hesla zlyhalo");
    I.clickCss("#toast-container-webjet > div > button");

    deleteReservationsWitPassword(I, delete_password);

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

//Testy na preverenie vratenych error sprav
Scenario('Reservation error logic tests', async ({I, DTE}) => {
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
    DT.filterContains("name", "Tester_L2");
    DT.filterContains("editorFields.selectedReservation", "Tenisovy kurt A");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    I.click("Tester_L2");
    DTE.waitForEditor("reservationDataTable");
    I.say("Change day to 10.01.2045");
    SL.setReservation(I, null, "10.01.2045", "10.01.2045", "13:00", "16:00");
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

    DT.filterContains("name", "Tester_L2");
    DT.filterContains("editorFields.selectedReservation", "Tenisovy kurt A");

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
    DT.filterContains("editorFields.selectedReservation", "TestDomain_webjet9_object");
    I.see("TestDomain_webjet9_object");
    DT.filterContains("editorFields.selectedReservation", "TestDomain_test23_object");
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
    DT.filterContains("editorFields.selectedReservation", "TestDomain_webjet9_object");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filterContains("editorFields.selectedReservation", "TestDomain_test23_object");
    I.see("TestDomain_test23_object");
});

Scenario('logout to change domain', async ({I}) => {
    I.logout();
});

Scenario('Allow Creation in the Past', async ({ I, DT, DTE }) => {
    I.relogin("tester");
    I.amOnPage("/apps/reservation/admin/");

    I.click(DT.btn.reservation_add_button);
    SL.setWholeDayReservation(I, DTE, "Spa celodenné", getShiftedDate(-2), getShiftedDate(-1), "08:00", "20:00", `autotest-inthepast-${randomNumber}`);
    I.seeInField("#DTE_Field_editorFields-infoLabel", dateInPastErr);
    DTE.clickSwitch("editorFields-allowHistorySave_0");
    I.seeInField("#DTE_Field_editorFields-infoLabel", "Vami zadanú rezerváciu je možné uložiť.");
    DTE.save("reservationDataTable");

    DT.waitForLoader();
    await DT.showColumn("Účel", "reservationDataTable");
    DT.filterContains('purpose', `autotest-inthepast-${randomNumber}`);
    DT.checkTableRow("reservationDataTable", 1, ["", "Tester", "Playwright", "tester@balat.sk", "Spa celodenné", getShiftedDate(-2), "08:00", getShiftedDate(-1), "20:00"]);
    DT.resetTable("reservationDataTable");
});

Scenario('Overbooking', async ({ I, DT, DTE }) => {
    I.amOnPage("/apps/reservation/admin/") ;

    I.click(DT.btn.reservation_add_button);
    SL.setWholeDayReservation(I, DTE, "Spa celodenné", getShiftedDate(1), getShiftedDate(2), "09:00", "19:00", `autotest-overbooking1-${randomNumber}`);
    DTE.save("reservationDataTable");

    I.click(DT.btn.reservation_add_button);
    SL.setWholeDayReservation(I, DTE, "Spa celodenné", getShiftedDate(1), getShiftedDate(2), "09:00", "19:00", `autotest-overbooking2-${randomNumber}`);
    DTE.save("reservationDataTable");

    I.click(DT.btn.reservation_add_button);
    SL.setWholeDayReservation(I, DTE, "Spa celodenné", getShiftedDate(1), getShiftedDate(2), "09:00", "19:00", `autotest-overbooking3-${randomNumber}`);
    I.seeInField("#DTE_Field_editorFields-infoLabel", "Bol dosiahnutý maximalný počet rezervácií pre zvolené obdobie a rezervačný objekt.");
    DTE.clickSwitch("editorFields-allowOverbooking_0");
    I.seeInField("#DTE_Field_editorFields-infoLabel", "Vami zadanú rezerváciu je možné uložiť.");
    DTE.save("reservationDataTable");

    DT.waitForLoader();
    await DT.showColumn("Účel", "reservationDataTable");
    DT.filterContains("purpose", "autotest-overbooking");
    DT.checkTableRow("reservationDataTable", 1, ["", "Tester", "Playwright", "tester@balat.sk", "Spa celodenné", getShiftedDate(1), "09:00", getShiftedDate(2), "19:00", "120,00", `autotest-overbooking1-${randomNumber}`]);
    DT.checkTableRow("reservationDataTable", 2, ["", "Tester", "Playwright", "tester@balat.sk", "Spa celodenné", getShiftedDate(1), "09:00", getShiftedDate(2), "19:00", "120,00", `autotest-overbooking2-${randomNumber}`]);
    DT.checkTableRow("reservationDataTable", 3, ["", "Tester", "Playwright", "tester@balat.sk", "Spa celodenné", getShiftedDate(1), "09:00", getShiftedDate(2), "19:00", "120,00", `autotest-overbooking3-${randomNumber}`]);
});

Scenario('Edit record', async ({ I, DT, DTE }) => {
    I.amOnPage("/apps/reservation/admin/");
    DT.filterContains('purpose', `autotest-inthepast-${randomNumber}`);
    I.clickCss("button.buttons-select-all");
    I.click(DT.btn.reservation_edit_button);
    DTE.waitForEditor("reservationDataTable");
    const editFields = ["#DTE_Field_editorFields-allowHistorySave_0", "#DTE_Field_editorFields-allowOverbooking_0", "#DTE_Field_editorFields-infoLabel"];

    editFields.forEach(editField => {
        I.dontSeeElement(editField);
    });

    DTE.fillField("editorFields-departureTime", "19:00");
    I.pressKey("Enter");
    DTE.clickSwitch("purpose");
    editFields.forEach(editField => {
        I.waitForElement(editField, 10);
    });

    DTE.fillField("editorFields-departureTime", "20:00");
    I.pressKey("Enter");
    DTE.clickSwitch("purpose");
    editFields.forEach(editField => {
        I.waitForInvisible(editField, 10);
    });

    DTE.appendField("purpose", "-chan.ge");
    DTE.save("reservationDataTable");
    await DT.showColumn("Účel", "reservationDataTable");
    DT.filterContains('purpose', `autotest-inthepast-${randomNumber}-chan.ge`);
    DT.checkTableRow("reservationDataTable", 1, ["", "Tester", "Playwright", "tester@balat.sk", "Spa celodenné", getShiftedDate(-2), "08:00", getShiftedDate(-1), "20:00"]);
});

Scenario('Delete all Spa celodenne reservations ', async ({ I, DT, DTE }) => {
    await SL.deleteReservation(I, DT, DTE, 'editorFields.selectedReservation', 'Spa celodenné');
    I.amOnPage("/apps/reservation/admin/");
    DT.resetTable("reservationDataTable");
});

Scenario('Tab actions and buttons verifications', ({ I, DT, DTE }) => {
    I.amOnPage("/apps/reservation/admin/");
    DT.resetTable("reservationDataTable");
    DT.filterContains("id", "5528");
    I.clickCss("button.buttons-select-all");

    SL.changeReservationStatus(I, DT, DTE, '.btn-success.approve', 'Schváliť rezerváciu', 'Ste si istý, že chcete schváliť túto rezerváciu ?', 'Schválená');
    SL.changeReservationStatus(I, DT, DTE, '.btn-danger.reject', 'Zamietnuť rezerváciu', 'Ste si istý, že chcete zamietnuť túto rezerváciu ?', 'Zamietnutá');
    SL.changeReservationStatus(I, DT, DTE, '.btn-primary.reset', 'Resetovať stav rezervácie', 'Ste si istý, že chcete resetovať stav rezervácie ?', 'Nepotvrdená');
});

Scenario('logout', async ({I}) => {
    I.logout();
});

Scenario('Verify all objects are displayed when no object is selected', async ({ I }) => {
    var columnData;
    I.amOnPage('/apps/rezervacie');
    I.waitForText('Vyhľadávanie v rezerváciách', 10);
    columnData = await getTableColumnData(I, 'Objekt');
    const areObjectsDifferent = !areAllElementsEqual(columnData);
    I.assertTrue(areObjectsDifferent, 'Objects in the table are all the same.');
});

Scenario('Verify filtering by selected object works correctly', async ({ I }) => {
    var columnData;
    I.amOnPage('/apps/rezervacie');
    I.waitForText('Vyhľadávanie v rezerváciách', 10);

    I.selectOption('#filterObjectId1', 'Tenisovy kurt A');
    I.clickCss('input.submit');
    I.waitForText('Vyhľadávanie v rezerváciách', 10);
    columnData = await getTableColumnData(I, 'Objekt');
    let areObjectsCorrect = columnData.every(item => item.includes('Tenisovy kurt A'));
    I.assertTrue(areObjectsCorrect, 'All objects in the table should be same i.e. "Tenisovy kurt A".');

    I.selectOption('#filterObjectId1', 'Tenisovy kurt B');
    I.clickCss('input.submit');
    I.waitForText('Vyhľadávanie v rezerváciách', 10);
    columnData = await getTableColumnData(I, 'Objekt');
    areObjectsCorrect = columnData.every(item => item.includes('Tenisovy kurt B'));
    I.assertTrue(areObjectsCorrect, 'All objects in the table should be same i.e. "Tenisovy kurt B".');
});

Scenario('Verify reservation creation in admin and price validation based on user type and date range selection in the calendar works correctly', async ({ I, DT, DTE }) => {
    I.amOnPage('/apps/reservation/admin/');
    DT.waitForLoader();
    DT.addContext('reservation', '#reservationDataTable_wrapper');
    I.click(DT.btn.reservation_add_button);
    DTE.waitForEditor('reservationDataTable');
    SL.setReservation(I, 'Sauna', "24.07.2028", "25.07.2028", "08:00", "16:00", "Vami zadanú rezerváciu je možné uložiť.");
    DTE.seeInField('price', '640');
    DTE.fillField('purpose', `autotest-${randomNumber}`);
    DTE.save();
    I.amOnPage('/apps/rezervacie');
    I.clickCss('#filterDateFromId');
    await selectDate(I, '23', 'Júl', '2028');
    I.clickCss('#filterDateToId');
    await selectDate(I, '26', 'Júl', '2028');
    I.selectOption('#filterObjectId1', 'Sauna');
    I.clickCss('input.submit');
    I.waitForText('Vyhľadávanie v rezerváciách', 10);
    I.see('24.07.2028');
    I.see('25.07.2028');
    I.see(`autotest-${randomNumber}`);
});

Scenario('Delete all Sauna reservations ', async ({ I, DT, DTE }) => {
    await SL.deleteReservation(I, DT, DTE, 'editorFields.selectedReservation', 'Sauna');
});

Scenario('Verify reservation data in the table after applying filters', async ({ I }) => {
    const filterDateFrom = '1. 1. 2045';
    const filterDateTo = '3. 1. 2045';
    I.amOnPage('/apps/rezervacie');
    I.selectOption('#filterObjectId1', 'Tenisovy kurt B');
    I.fillField('#filterDateFromId', filterDateFrom);
    I.fillField('#filterDateToId', filterDateTo);
    I.clickCss('input.submit');
    I.waitForText('Vyhľadávanie v rezerváciách', 10);
    const columnDataFrom = await getTableColumnData(I, 'Dátum od');
    let isFromFilteredCorrectly = columnDataFrom.every(date => new Date(date) >= new Date(filterDateFrom));
    I.assertTrue(isFromFilteredCorrectly, 'Nesprávne filtrované hodnoty v Dátum od');
    let columnDataTo = await getTableColumnData(I, 'Dátum do');
    let isToFilteredCorrectly = columnDataTo.every(date => new Date(date) <= new Date(filterDateTo + " 23:59:59"));
    I.assertTrue(isToFilteredCorrectly, 'Nesprávne filtrované hodnoty v Dátum do:');
});

Scenario('Create reservation in calendar and check if works correctly', async ({ I }) => {
    I.amOnPage('/apps/rezervacie');
    I.waitForElement('.addReservation > a', 10);
    I.clickCss('.addReservation > a');
    I.wait(5);

    I.switchToNextTab();
    I.see('Pridajte rezerváciu');

    I.selectOption('#reservationObjectId1', 'Sauna');
    I.fillField('#dateFrom', '24.10.2028');
    I.fillField('#dateTo', '25.10.2028');
    I.selectOption('#startTimeId1', '13:00');
    I.selectOption('#finishTimeId1', '14:00');
    I.fillField('#purposeId1', `autotest-B${randomNumber}`);
    I.clickCss('#btnOk');
    I.wait(2);
 });

 Scenario('Verify reservation creation in admin from previous scenario', ({ I }) => {
    I.amOnPage('/apps/rezervacie');

    I.waitForText('Vyhľadávanie v rezerváciách', 10);
    I.fillField('#filterDateFromId', '24.10.2028');
    I.fillField('#filterDateToId', '25.10.2028');
    I.selectOption('#filterObjectId1', 'Sauna');
    I.clickCss('input.submit');
    const getCell = (column) => locate('#tableReservations > tbody > tr:nth-child(1)').find(`td:nth-child(${column})`);
    I.waitForText('Sauna', 10, getCell(2));
    I.see('24.10.2028 13:00', getCell(3));
    I.see('25.10.2028 14:00', getCell(4));
    I.see('Tester Playwright', getCell(5));
    I.see(`autotest-B${randomNumber}`, getCell(6));
 });

Scenario('Delete Sauna reservations from previous scenario', async ({ I, DT, DTE }) => {
    await SL.deleteReservation(I, DT, DTE, 'editorFields.selectedReservation', 'Sauna');
});

Scenario('cleanup', ({ I, DT, DTE }) => {
    I.openNewTab();
    I.closeOtherTabs();
});


Scenario('Testovanie app -  Rezervácie', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Rezervácie', '#components-reservation-title');

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see("Vyhľadávanie v rezerváciách");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    DTE.selectOption("reservationType", "Zoznam izieb");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    I.see("Vyberte si izbu");
});

/**
 * Selects a specific date from a date picker.
 *
 * This function automates the process of navigating through a date picker widget
 * to select a specific day. It first ensures the date picker displays the desired
 * month and year, then clicks on the target day to select it.
 */
async function selectDate(I, targetDay, targetMonth, targetYear) {
    const targetDate = `${targetMonth} ${targetYear}`;
    let currentText;
    let moveCount = 0;
    const moveLimit = 12*5;  //5 years
    do {
        currentText = (await I.grabTextFrom('#ui-datepicker-div > div > div')).normalize('NFKC').trim();

        if (currentText !== targetDate) {
            I.click('#ui-datepicker-div a.ui-datepicker-next');
            I.wait(0.2);
            moveCount++;
        }
        if (moveCount >= moveLimit) {
            throw new Error('Out of move limit');
        }
    } while (currentText !== targetDate);
    I.click(`td[data-handler="selectDay"] a[data-date="${targetDay}"]`);
}



/**
 * Function to extract column data from a table from all pages.
 * @param {string} columnName - The column name (e.g., 'Objekt', 'Dátum od').
 * @returns {Promise<Array<string>>} - An array of strings containing the values of the specified column.
 */
async function getTableColumnData(I, columnName) {
    const tableHeaderSelector = "#tableReservations thead tr th";
    const tableRowSelector = "#tableReservations tbody tr";

    const headers = await I.grabTextFromAll(tableHeaderSelector);
    const columnIndex = headers.indexOf(columnName);

    if (columnIndex === -1) {
        throw new Error(`Column with name '${columnName}' not found in the table.`);
    }

    const columnData = [];
    let hasNextPage = true;
    while (hasNextPage) {
        const rows = await I.grabNumberOfVisibleElements(tableRowSelector);
        for (let i = 1; i <= rows; i++) {
            const cellSelector = `#tableReservations tbody tr:nth-child(${i}) td:nth-child(${columnIndex + 1})`;
            const cellText = await I.grabTextFrom(cellSelector);
            columnData.push(cellText.trim());
        }
        hasNextPage = await I.grabNumberOfVisibleElements('a i.far.fa-angle-right') > 0;
        if (hasNextPage) {
            I.clickCss('a i.far.fa-angle-right');
            I.waitForElement('#tableReservations tbody', 5);
        }
    }
    return columnData;
}


/**
 * Function to check if all elements in an array are the same.
 * @param {Array<any>} array - The array to check.
 * @returns {boolean} - True if all elements are the same, otherwise false.
 */
function areAllElementsEqual(array) {
    if (array.length === 0) return true;
    return array.every(element => element === array[0]);
}

async function checkReservation(I, DTE, Error,  ...tableData) {
    const [reservationObjectName, dateFrom, dateTo, timeFrom, timeTo] = tableData;
    SL.setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo, Error);

    //Try save if error msg will be shown
    DTE.save();
}

function deleteReservationsWitPassword(I, deletePassword) {
    I.clickCss(".custom-buttons-remove");
    I.seeElement("#toast-container-webjet");
    I.see("Zadajte heslo");

    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-input > input");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", deletePassword);
    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.wait(5);
    I.waitForElement("#reservationDataTable_modal > div > div", 10);
    I.click("Zmazať", "div.DTE_Action_Remove");
}

/**
 * Returns a formatted date (DD.MM.YYYY) shifted by a given number of days from today.
 *
 * @param {number} dayOffset - Number of days to shift (positive for future, negative for past).
 * @returns {string} Formatted date in "DD.MM.YYYY" format.
 */
function getShiftedDate(dayOffset) {
    const currentDate = new Date();
    currentDate.setDate(currentDate.getDate() + dayOffset);

    const day = String(currentDate.getDate()).padStart(2, '0');
    const month = String(currentDate.getMonth() + 1).padStart(2, '0');
    const year = currentDate.getFullYear();

    return `${day}.${month}.${year}`;
}
