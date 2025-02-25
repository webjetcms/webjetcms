const SL = require("./shared-logic");

Feature('apps.reservation.reservation-objects');

var randomNumber;
let base_reservation_object;
let prices_reservation_object;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        base_reservation_object = "reservation_object-" + randomNumber + "-autotest";
        prices_reservation_object = "reservation_object_prices-" + randomNumber + "-autotest";
    }
});

Scenario('zakladne testy @baseTest', async ({I, DataTables}) => {
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    await DataTables.baseTest({
        dataTable: 'reservationObjectDataTable',
        perms: 'cmp_reservation',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('reservation object + custom times test', ({I, DT, DTE}) => {
    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    let dateValueFromB = "07:25";
    let dateValueToB = "21:38";

    let dateValueFromA = "13:36";
    let dateValueToA = "14:24";

    let dateValueFromC = "00:00";
    let dateValueToC = "23:59";

    I.clickCss("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("reservationObjectDataTable");

    I.clickCss("#DTE_Field_name");
    I.fillField("#DTE_Field_name", base_reservation_object);

    //Not important but required
    I.clickCss("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Reservation entity test + test of times by days.");

    //Set specific time for specific days of week
    I.clickCss('#pills-dt-reservationObjectDataTable-chooseDays-tab');

    setSpecialTimeRange(I, "B", dateValueFromB, dateValueToB);

    //Save this reservation object
    DTE.save();

    DT.filterContains("name", base_reservation_object)
    I.click(base_reservation_object);
    DTE.waitForEditor("reservationObjectDataTable");
    I.clickCss('#pills-dt-reservationObjectDataTable-chooseDays-tab');

    //Check if field are visible and with good value
    I.seeElement("#DTE_Field_editorFields-reservationTimeFromB");
    I.seeElement("#DTE_Field_editorFields-reservationTimeToB");

    I.waitForValue("#DTE_Field_editorFields-reservationTimeFromB", dateValueFromB, 10);
    I.waitForValue("#DTE_Field_editorFields-reservationTimeToB", dateValueToB, 10);

    //Hide Tuesday reservation time
    I.clickCss('#DTE_Field_editorFields-chooseDayB_0');

    //Add Monday
    setSpecialTimeRange(I, "A", dateValueFromA, dateValueToA);

    //Add Wednesday
    setSpecialTimeRange(I, "C", dateValueFromC, dateValueToC);

    //Save this edited reservation object
    DTE.save();

    I.click(base_reservation_object);
    DTE.waitForEditor("reservationObjectDataTable");
    I.clickCss('#pills-dt-reservationObjectDataTable-chooseDays-tab');

    //Check if fields are vivible/invisible and vith good value
    I.dontSeeElement("#DTE_Field_editorFields-reservationTimeFromB");
    I.dontSeeElement("#DTE_Field_editorFields-reservationTimeToB");

    I.seeElement("#DTE_Field_editorFields-reservationTimeFromA");
    I.seeElement("#DTE_Field_editorFields-reservationTimeToA");
    I.waitForValue("#DTE_Field_editorFields-reservationTimeFromA", dateValueFromA, 10);
    I.waitForValue("#DTE_Field_editorFields-reservationTimeToA", dateValueToA, 10);

    I.seeElement("#DTE_Field_editorFields-reservationTimeFromC");
    I.seeElement("#DTE_Field_editorFields-reservationTimeToC");
    I.waitForValue("#DTE_Field_editorFields-reservationTimeFromC", dateValueFromC, 10);
    I.waitForValue("#DTE_Field_editorFields-reservationTimeToC", dateValueToC, 10);
});

function setSpecialTimeRange(I, fieldLetter, dateValueFrom, dateValueTo) {
    //Just in case capitalize letter
    fieldLetter = fieldLetter.toUpperCase();

    //Show this day
    I.checkOption('#DTE_Field_editorFields-chooseDay' + fieldLetter + '_0');

    I.clickCss("#DTE_Field_editorFields-reservationTimeFrom" + fieldLetter);
    I.fillField("#DTE_Field_editorFields-reservationTimeFrom" + fieldLetter, dateValueFrom);
    I.pressKey('Enter');

    I.clickCss("#DTE_Field_editorFields-reservationTimeTo" + fieldLetter);
    I.fillField("#DTE_Field_editorFields-reservationTimeTo" + fieldLetter, dateValueTo);
    I.pressKey('Enter');
}

Scenario('remove BASE test reservation object', async ({I, DT}) => {
    SL.deleteReservationObject(I, DT, base_reservation_object);
});

Scenario('reservation object + special prices test', ({I, DT, DTE}) => {
    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    I.clickCss("button.buttons-create");
    I.dtWaitForEditor("reservationObjectDataTable");

    I.clickCss("#DTE_Field_name");
    I.fillField("#DTE_Field_name", prices_reservation_object);

    //Not important but required
    I.clickCss("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Reservation entity test + test of special prices inner data table.");

    I.say("During create cant see special prices tab");
    I.dontSeeElement("#pills-dt-reservationObjectDataTable-specialPrice-tab");
    DTE.save();

    I.say("During edit we see tab special prices");
    DT.filterContains("name", prices_reservation_object);
    I.click(prices_reservation_object);
    DTE.waitForEditor("reservationObjectDataTable");
    I.seeElement("#pills-dt-reservationObjectDataTable-specialPrice-tab");

    I.clickCss("#pills-dt-reservationObjectDataTable-specialPrice-tab");
    I.say('Test special prices inner table');
    let modal = "#datatableFieldDTE_Field_editorFields-objectPrices_modal";
    let wrapper = "#datatableFieldDTE_Field_editorFields-objectPrices_wrapper";

    I.say("Required fields");
    addPriceEntity(I, DTE, modal, "", "", "");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).", modal);
    I.click( locate(modal).find("button.btn-close-editor") );

    I.say("Adding price entity");
    addPriceEntity(I, DTE, modal, "01.01.2045", "02.01.2045", "100");
    I.waitForInvisible("#datatableFieldDTE_Field_editorFields-objectPrices_modal", 20);
    DTE.waitForModalClose("datatableFieldDTE_Field_editorFields-objectPrices_modal");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-objectPrices", 1, ["", "", "01.01.2045", "02.01.2045", "100"]);

    I.say("Check duplicity validation");
    addPriceEntity(I, DTE, modal, "02.01.2045", "03.01.2045", "200");
    I.see("Dátumový rozsah sa prekrýva s iným dátumovým rozsahom špeciálnej ceny pre tento rezervačný objekt.", modal);
    I.click( locate(modal).find("button.btn-close-editor") );
    DTE.waitForModalClose("datatableFieldDTE_Field_editorFields-objectPrices_modal");

    I.say('Check edit price entity');
    I.click( locate(wrapper).find("button.buttons-select-all") );
    I.click( locate(wrapper).find("button.buttons-edit") );
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-objectPrices");
    I.fillField(locate(modal).find("#DTE_Field_price"), "300");
    I.click( locate(modal).find("button.btn-primary") );
    DTE.waitForModalClose("datatableFieldDTE_Field_editorFields-objectPrices_modal");
    DT.waitForLoader();
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-objectPrices", 1, ["", "", "01.01.2045", "02.01.2045", "300"]);

    I.say('Test price delete');
    I.click( locate(wrapper).find("button.buttons-remove") );
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy", wrapper);
});

Scenario('remove prices reservation object', async ({I, DT}) => {
    SL.deleteReservationObject(I, DT, prices_reservation_object);
});

function addPriceEntity(I, DTE, modal, dateFrom, dateTo, price) {
    I.click( locate("#datatableFieldDTE_Field_editorFields-objectPrices_wrapper").find("button.buttons-create") );
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-objectPrices");

    I.click( locate(modal).find("#DTE_Field_dateFrom") );
    I.fillField(locate(modal).find("#DTE_Field_dateFrom"), dateFrom);
    I.pressKey('Enter');

    I.click( locate(modal).find("#DTE_Field_dateTo") );
    I.fillField(locate(modal).find("#DTE_Field_dateTo"), dateTo);
    I.pressKey('Enter');

    I.fillField(locate(modal).find("#DTE_Field_price"), price);
    I.click( locate(modal).find("button.btn-primary") );
}

Scenario('Domain test', ({I, DT, Document}) => {
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    DT.filterContains("name", "TestDomain_webjet9_object");
    I.see("TestDomain_webjet9_object");
    DT.filterContains("name", "TestDomain_test23_object");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    Document.switchDomain("test23.tau27.iway.sk");
    DT.filterContains("name", "TestDomain_webjet9_object");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filterContains("name", "TestDomain_test23_object");
    I.see("TestDomain_test23_object");
});

Scenario('logout', async ({I}) => {
    I.logout();
});