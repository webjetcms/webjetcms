Feature('apps.reservation.day-book-app');

const assert = require('assert');
const SL = require("./shared-logic");
const TempMail = require('../../../pages/TempMail');

let randomNumber;

Before(({ I, login, DT }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
    DT.addContext('reservation','#reservationDataTable_wrapper');
});

Scenario('Delete reservations', async ({ I, DT }) => {
    I.amOnPage('/apps/reservation/admin/');
    DT.waitForLoader();

    await deleteFilteredReservations(DT, I, 'name', 'autotest-');
    I.refreshPage();
    await deleteFilteredReservations(DT, I, 'editorFields.selectedReservation', 'Spa celodenné');
});

Scenario('Verifying elements of day book app', ({ I }) => {
    I.amOnPage("/apps/rezervacie/rezervacie-vsetko.html");
    I.waitForVisible("#calendar > .vc-controls");

    I.say('Check that year in past is disabled');
    I.clickCss("button.vc-year");
    I.waitForVisible(".vc-years");
    I.seeElement(locate('button').withAttr({'data-vc-years-year':'2023', 'aria-disabled' : 'true'}));
    I.clickCss("button.vc-year");

    I.say('Verify calendar behaviour');
    setDate(I, 2031, 1);
    setDate(I, 2038, 1);
    setDate(I, 2042, 10);

    checkCalendarDates(I, 'Október', '2042', 'November', '2042');
    I.clickCss('.vc-arrow.vc-arrow_next');
    checkCalendarDates(I, 'November', '2042', 'December' ,'2042');
    I.clickCss('.vc-arrow.vc-arrow_next');
    checkCalendarDates(I, 'December', '2042', 'Január' ,'2043');
    I.clickCss('.vc-arrow.vc-arrow_next');
    checkCalendarDates(I, 'Január', '2043', 'Február', '2043');
    setDate(I, 2044, 1);
    checkCalendarDates(I, 'Január', '2044', 'Február', '2044');
    I.clickCss('.vc-arrow.vc-arrow_prev');
    checkCalendarDates(I, 'December', '2043', 'Január', '2044');
});

Scenario('Check reservation TABLE + logic', async ({I, DT}) => {
    I.amOnPage("/apps/rezervacie/rezervacie-vsetko.html");
    I.waitForVisible("#calendar > .vc-controls");
    I.selectOption('#reservationObjectId', 'Spa celodenné');

    I.say("Set date in future");
    setDate(I, 2031, 1);
    setDate(I, 2038, 1);
    setDate(I, 2045, 1);

    I.say("Check table composition");
    for (let day = 1; day <= 31; day++) {
        checkOccupancy(I, 2045, 1, day, '0/2');
    }

    clickDate(I, 2045, 1, 1);
    clickDate(I, 2045, 1, 7);
    I.seeInField('#price', '760');
    submitReservation(I);

    checkReservation(I, DT, 1, [
        "", "Tester", "Playwright", "tester@balat.sk",
        "Spa celodenné", "01.01.2045", "14:00", "07.01.2045",
        "10:30", "760,00", "Schválená" ]);

    setDate(I, 2045, 1)
    clickDate(I, 2045, 1, 5);
    clickDate(I, 2045, 1, 11);
    I.seeInField('#price', '720');
    submitReservation(I);

    checkReservation(I, DT, 2, [
        "", "Tester", "Playwright", "tester@balat.sk",
        "Spa celodenné", "05.01.2045", "14:00", "11.01.2045",
        "10:30", "720,00", "Schválená"
    ]);

    checkCheckOutOnly(I, 2045, 1, 5);
    checkDisabled(I, 2045, 1, 6);

    for (let day = 1; day <= 4; day++) {
        checkOccupancy(I, 2045, 1, day, '1/2');
    }
    checkOccupancy(I, 2045, 1, 5, '-');
    checkOccupancy(I, 2045, 1, 6, '');
    for (let day = 7; day <= 10; day++) {
        checkOccupancy(I, 2045, 1, day, '1/2');
    }

    I.say("Check if I choose day before check-out days after are disabled");
    clickDate(I, 2045, 1, 2);

    for (let day = 3; day <= 5; day++) {
        checkDisabled(I, 2045, 1, day, false);
    }
    for (let day = 6; day <= 20; day++) {
        checkDisabled(I, 2045, 1, day);
    }

    I.say("Check reservation FORM as LOGGED user");
    I.seeInField("#name", "Tester");
    I.seeInField("#surname", "Playwright");
    I.seeInField("#email", "tester@balat.sk");

    I.say("Check reservation FORM as NOT LOGGED user");
    I.logout();
    I.amOnPage("/apps/rezervacie/rezervacie-vsetko.html");
    I.waitForVisible("#calendar > .vc-controls");
    I.seeInField("#name", "");
    I.seeInField("#surname", "");
    I.seeInField("#email", "");
});


Scenario('Verify all objects are displayed when no object is selected', async ({ I, Apps }) => {
    Apps.insertApp('Rezervácia dní', '#components-reservation-day_book-title', null, false);
    I.switchTo('#cke_121_iframe');
    I.switchTo('#editorComponent');
    I.clickCss('.DTE_Field_InputControl');
    const options = await I.grabTextFromAll('.dropdown-menu.inner.show .dropdown-item .text');
    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.clickCss('.btn-preview');

    I.wait(5);
    I.switchToNextTab();
    I.waitForElement('#reservationObjectId', 10);
    const actualOptions = await I.grabTextFromAll('#reservationObjectId');
    const cleanedOptionTexts = actualOptions[0].split('\n');
    assert.deepStrictEqual(cleanedOptionTexts, options, 'Options are not same');
});

Scenario('Selecting two random objects and verify only these objects are displayed', async ({ I, Apps, DTE }) => {
    Apps.insertApp('Rezervácia dní', '#components-reservation-day_book-title', null, false);
    I.switchTo('#cke_121_iframe');
    I.switchTo('#editorComponent');
    let chosenOptions = [];

    for(let i = 0; i < 2; i++){
        I.clickCss('.DTE_Field_InputControl');
        const options = await I.grabTextFromAll('.dropdown-menu.inner.show .dropdown-item .text');
        const randomOption = options[Math.floor(Math.random() * options.length)];
        while (!chosenOptions.includes(randomOption)){
            I.clickCss('.DTE_Field_InputControl');
            DTE.selectOption('reservationObjectIds', randomOption);
            I.clickCss('.DTE_Field_InputControl');
            chosenOptions.push(randomOption);
        }
    }

    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.clickCss('.btn-preview');

    I.wait(5);
    I.switchToNextTab();
    const actualOptions = await I.grabTextFromAll('#reservationObjectId');
    const cleanedActualOptions = actualOptions[0].split('\n');
    cleanedActualOptions.sort((a, b) => a.localeCompare(b));
    chosenOptions.sort((a, b) => a.localeCompare(b));

    assert.deepStrictEqual(cleanedActualOptions, chosenOptions, 'Options are not same');
});

Scenario('Verify discount on user in discount group 40 percent', ({ I, DT }) => {
    I.relogin('tester3');
    I.amOnPage('/apps/rezervacie/rezervacia-spa.html');
    I.waitForVisible("#calendar > .vc-controls");
    setDate(I, 2030, 12);
    clickDate(I, 2030, 12, 10);
    clickDate(I, 2030, 12, 13);
    I.seeInField('#price', 120*3*0.6);
    submitReservation(I);

    checkReservation(I, DT, 3, [
        "", "Tester_L2", "Playwright", "tester3@balat.sk",
        "Spa celodenné", "10.12.2030", "14:00", "13.12.2030",
         "10:30", "216,00", "Schválená"
    ]);
});

Scenario('Verify reservation creation and price validation based on user type and date range selection in the calendar works correctly', ({ I, DT }) => {
    I.amOnPage('/apps/rezervacie/rezervacia-spa.html');
    I.waitForVisible("#calendar > .vc-controls");
    setDate(I, 2030, 12);

    clickDate(I, 2030, 12, 24);
    clickDate(I, 2030, 12, 25);

    I.fillField('#name', `autotest-${randomNumber}`);
    I.seeInField('#price', '120');
    I.seeInField('#dateFrom', '24.12.2030 - 14:00');
    I.seeInField('#dateTo', '25.12.2030 - 10:30');
    I.fillField('#phoneNumber', '0912345678');
    I.fillField('#purpose', 'Rekreácia');

    submitReservation(I);

    checkOccupancy(I, 2030, 12, 23, '0/2');
    checkOccupancy(I, 2030, 12, 24, '1/2');
    checkOccupancy(I, 2030, 12, 25, '0/2');

    checkReservation(I, DT, 4, [
        "", `autotest-${randomNumber}`, "Playwright", "tester@balat.sk",
         "Spa celodenné", "24.12.2030", "14:00", "25.12.2030",
         "10:30", "120,00", "Schválená"
    ]);
});

Scenario('Check emails - need approve', ({ I, DT, DTE, TempMail }) => {
    let reservationObjectNameB = "screenshotReservationB";
    let authorName = "autotest-A-" + randomNumber;

    I.say("Set approver of object");
        I.amOnPage("/apps/reservation/admin/reservation-objects/");
        DT.filterEquals("name", reservationObjectNameB);
        I.click(reservationObjectNameB);
        DTE.waitForEditor("reservationObjectDataTable");

        I.clickCss("#pills-dt-reservationObjectDataTable-advanced-tab");
        I.checkOption("#DTE_Field_mustAccepted_0");
        I.fillField("#DTE_Field_emailAccepter", "tester@balat.sk");
        DTE.save();

    I.say("Create reservation as another user - not approver");
        I.relogin("tester2");
        I.amOnPage('/apps/rezervacie/rezervacia-screenshotreservationb.html');
        I.waitForVisible("#calendar > .vc-controls");
        setDate(I, 2032, 4);

        clickDate(I, 2032, 4, 8);
        clickDate(I, 2032, 4, 10);

        I.fillField('#name', authorName);
        I.fillField('#phoneNumber', '0912345678');
        I.fillField('#purpose', 'Rekreácia');
        I.fillField("#email", "webjetcustomer@fexpost.com");

        submitReservation(I, true);

    TempMail.login("webjetcustomer");
    TempMail.openLatestEmail();

    I.see("Vytvorenie rezervácie");
    I.see(`Dobrý deň ${authorName} Playwright2`);
    I.see(`Vaša rezervácia objektu ${reservationObjectNameB} na celý deň v termíne`);
    I.see("bola vytvorená a čaká na schválenie.");
    TempMail.deleteCurrentEmail();

    I.say("Log as approver and reject reservation");
    I.relogin("tester");
    I.amOnPage("/apps/reservation/admin/");
    DT.filterContains('name', authorName);
    I.clickCss("button.buttons-select-all");
    SL.changeReservationStatus(I, DT, DTE, '.btn-danger.reject', 'Zamietnuť rezerváciu', 'Ste si istý, že chcete zamietnuť túto rezerváciu ?', 'Zamietnutá');

    TempMail.login("webjetcustomer");
    TempMail.openLatestEmail();
    I.see("Zamietnutie rezervácie");
    I.see(`Dobrý deň ${authorName} Playwright2`);
    I.see(`Vaša rezervácia objektu ${reservationObjectNameB} na celý deň v termíne`);
    I.see("bola zamietnutá schvaľovateľom : Tester Playwright .");
    TempMail.deleteCurrentEmail();
});

Scenario('Check emails - DONT need approve', ({ I, DT, DTE, TempMail }) => {
    I.relogin("tester");

    let reservationObjectNameB = "screenshotReservationB";
    let authorName = "autotest-B-" + randomNumber;

    I.say("Set approver of object");
        I.amOnPage("/apps/reservation/admin/reservation-objects/");
        DT.filterEquals("name", reservationObjectNameB);
        I.click(reservationObjectNameB);
        DTE.waitForEditor("reservationObjectDataTable");

        I.clickCss("#pills-dt-reservationObjectDataTable-advanced-tab");
        I.checkOption("#DTE_Field_mustAccepted_0");
        I.fillField("#DTE_Field_emailAccepter", "tester@balat.sk");
        DTE.save();

    I.say("Create reseravtion as approver");
        I.amOnPage('/apps/rezervacie/rezervacia-screenshotreservationb.html');
        I.waitForVisible("#calendar > .vc-controls");
        setDate(I, 2032, 4);

        clickDate(I, 2032, 4, 8);
        clickDate(I, 2032, 4, 10);

        I.fillField('#name', authorName);
        I.fillField('#phoneNumber', '0912345678');
        I.fillField('#purpose', 'Rekreácia');
        I.fillField("#email", "webjetcustomer@fexpost.com");

        submitReservation(I, false);

    I.say("Terst mail");
    TempMail.login("webjetcustomer");
    TempMail.openLatestEmail();

    I.see("Vytvorenie rezervácie");
    I.see(`Dobrý deň ${authorName} Playwright`);
    I.see(`Vaša rezervácia objektu ${reservationObjectNameB} na celý deň v termíne`);
    I.see("bola vytvorená a schválená.");
    TempMail.deleteCurrentEmail();
});

Scenario('Revert', async ({ I }) => {
    TempMail.login("webjetcustomer");
    await TempMail.destroyInbox();
});

function checkReservation(I, DT, row, values) {
    I.openNewTab();
    I.amOnPage('/apps/reservation/admin/');
    DT.waitForLoader();

    DT.filterContains('editorFields.selectedReservation', 'Spa celodenné');

    DT.checkTableRow("reservationDataTable", row, values);
    I.closeCurrentTab();
}

function checkCalendarDates(I, firstMonth, firstYear, secondMonth, secondYear ) {
    I.seeElement(locate('.vc-month').first().withText(firstMonth));
    I.seeElement(locate('.vc-month').last().withText(secondMonth));
    I.seeElement(locate('.vc-year').first().withText(firstYear));
    I.seeElement(locate('.vc-year').last().withText(secondYear));
}

function submitReservation(I, requiresApproval = false) {
    I.click(locate('#reservationForm > button').withAttr({ type: 'submit' }));

    if(requiresApproval == false)
        I.waitForText('Vaša rezervácia bola úspešne vytvorená a schválená.', 10);
    else
        I.waitForText('Vaša rezervácia bola úspešne vytvorená a teraz čaká na schválenie.', 10);

    I.refreshPage();
}

function clickDate(I, year, month, day) {
    const formattedMonth = month.toString().padStart(2, '0');
    const formattedDay = day.toString().padStart(2, '0');

    I.click(locate('div').withAttr({ 'data-vc-date': `${year}-${formattedMonth}-${formattedDay}` }));
    I.waitForElement(locate(locate('div').withAttr({ 'data-vc-date': `${year}-${formattedMonth}-${formattedDay}`})).find('button').withAttr({'aria-selected':'true'}));
}

function checkOccupancy(I, year, month, day, fraction){
    const formattedMonth = month.toString().padStart(2, '0');
    const formattedDay = day.toString().padStart(2, '0');
    I.see(fraction , locate(locate('div').withAttr({ 'data-vc-date': `${year}-${formattedMonth}-${formattedDay}` })).find('button > span').at(2));
}

function checkDisabled(I, year, month, day, shouldBeDisabled = true){
    const formattedMonth = month.toString().padStart(2, '0');
    const formattedDay = day.toString().padStart(2, '0');
    const dateLocator = locate('div').withAttr({ 'data-vc-date': `${year}-${formattedMonth}-${formattedDay}` }).find('button').withAttr({ 'aria-disabled': "true" });

    if (shouldBeDisabled) {
        I.seeElement(dateLocator);
    } else {
        I.dontSeeElement(dateLocator);
    }
}

function checkCheckOutOnly(I, year, month, day) {
    const formattedMonth = month.toString().padStart(2, '0');
    const formattedDay = day.toString().padStart(2, '0');
    const spanSelector = locate(locate('div').withAttr({ 'data-vc-date': `${year}-${formattedMonth}-${formattedDay}`})).find('button');
    I.waitForElement(spanSelector, 5);
    I.seeElement(spanSelector);

    I.moveCursorTo(locate('div').withAttr({ 'data-vc-date': `${year}-${formattedMonth}-${formattedDay}` }));
    I.waitForElement('.vc-date__popup.custom-popup[data-vc-date-popup]', 5);
    I.see('Check-out ONLY', '.vc-date__popup.custom-popup[data-vc-date-popup]');
}

/**
 * Set Date
 * @param {number} year
 * @param {number} month - starts from 1
 */
function setDate(I, year, month) {
    I.clickCss("button.vc-year");
    I.waitForVisible(".vc-years");
    I.clickCss("button[data-vc-years-year='" + year + "']");
    I.waitForInvisible(".vc-years");

    I.clickCss("button.vc-month");
    I.waitForVisible("button.vc-months__month[data-vc-months-month='" + (month - 1) + "']");
    I.clickCss("button.vc-months__month[data-vc-months-month='" + (month - 1) + "']");
    I.waitForInvisible("button.vc-months__month[data-vc-months-month='" + (month - 1) + "']");

    const monthId = month < 10 ? "0" + month : "" + month;
    I.waitForVisible("div.vc-date[data-vc-date='" + year + "-" + monthId + "-01']");
}

async function deleteFilteredReservations(DT, I, filter, value) {
    DT.filterContains(filter, value);
    if (await I.getTotalRows() > 0) {
        I.clickCss("button.buttons-select-all");
        I.clickCss("button.custom-buttons-remove");

        if (await I.grabNumberOfVisibleElements("#toast-container-webjet div.toastr-input > input")){
            I.fillField("#toast-container-webjet div.toastr-input > input", "right_password");
            I.clickCss("#toast-container-webjet div.toastr-buttons > button.btn.btn-primary");
        }

        I.waitForElement("div.DTE_Action_Remove", 10);
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 30);
    }
}