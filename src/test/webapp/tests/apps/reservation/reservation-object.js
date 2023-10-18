Feature('reservation.objects');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('zakladne testy', async ({I, DataTables}) => {
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

Scenario('reservation object times test', async ({I, DataTables, DTE}) => {
    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    let reservationObjectName = "autotest-reservation_object-" + randomNumber;
    let dateValueFromB = "07:25";
    let dateValueToB = "21:38";

    let dateValueFromA = "13:36";
    let dateValueToA = "14:24";

    let dateValueFromC = "00:00";
    let dateValueToC = "23:59";

    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationObjectDataTable");

    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", reservationObjectName);

    //Not important but required
    I.click("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Random text");

    //Set specific time for specific days of week
    I.click('#pills-dt-reservationObjectDataTable-chooseDays-tab');

    I.click('#DTE_Field_editorFields-chooseDayB_0');

        I.click("#DTE_Field_editorFields-reservationTimeFromB");
        I.fillField("#DTE_Field_editorFields-reservationTimeFromB", dateValueFromB);
        I.pressKey('Enter');

        I.click("#DTE_Field_editorFields-reservationTimeToB");
        I.fillField("#DTE_Field_editorFields-reservationTimeToB", dateValueToB);
        I.pressKey('Enter');

    //Save this reservation object
    DTE.save();
    I.wait(1);

    I.fillField("input.dt-filter-name", reservationObjectName);
    I.pressKey('Enter', "dt-filter-name");
    I.wait(1);

    I.click(reservationObjectName);
    I.click('#pills-dt-reservationObjectDataTable-chooseDays-tab');

    //Check if field are vivible and vith good value
    I.seeElement("#DTE_Field_editorFields-reservationTimeFromB");
    I.seeElement("#DTE_Field_editorFields-reservationTimeToB");

    const fromB = await I.grabValueFrom("#DTE_Field_editorFields-reservationTimeFromB");
    const toB = await I.grabValueFrom("#DTE_Field_editorFields-reservationTimeToB");

    if(fromB != dateValueFromB || toB != dateValueToB) {
        //TODO add legit error
        I.error("KOKOS");
    }

    //Hide Tuesday reservation time
    I.click('#DTE_Field_editorFields-chooseDayB_0');

    //Add Monday
    I.click('#DTE_Field_editorFields-chooseDayA_0');
        I.click("#DTE_Field_editorFields-reservationTimeFromA");
        I.fillField("#DTE_Field_editorFields-reservationTimeFromA", dateValueFromA);
        I.pressKey('Enter');

        I.click("#DTE_Field_editorFields-reservationTimeToA");
        I.fillField("#DTE_Field_editorFields-reservationTimeToA", dateValueToA);
        I.pressKey('Enter');

    //Add Wednesday
    I.click('#DTE_Field_editorFields-chooseDayC_0');
        I.click("#DTE_Field_editorFields-reservationTimeFromC");
        I.fillField("#DTE_Field_editorFields-reservationTimeFromC", dateValueFromC);
        I.pressKey('Enter');

        I.click("#DTE_Field_editorFields-reservationTimeToC");
        I.fillField("#DTE_Field_editorFields-reservationTimeToC", dateValueToC);
        I.pressKey('Enter');

    //Save this edited reservation object
    DTE.save();
    I.wait(1);

    I.click(reservationObjectName);
    I.click('#pills-dt-reservationObjectDataTable-chooseDays-tab');

    //Check if fields are vivible/invisible and vith good value
    I.dontSeeElement("#DTE_Field_editorFields-reservationTimeFromB");
    I.dontSeeElement("#DTE_Field_editorFields-reservationTimeToB");

    I.seeElement("#DTE_Field_editorFields-reservationTimeFromA");
    I.seeElement("#DTE_Field_editorFields-reservationTimeToA");

    const fromA = await I.grabValueFrom("#DTE_Field_editorFields-reservationTimeFromA");
    const toA = await I.grabValueFrom("#DTE_Field_editorFields-reservationTimeToA");

    if(fromA != dateValueFromA || toA != dateValueToA) {
        //TODO add legit error
        I.error("KOKOS");
    }

    I.seeElement("#DTE_Field_editorFields-reservationTimeFromC");
    I.seeElement("#DTE_Field_editorFields-reservationTimeToC");

    const fromC = await I.grabValueFrom("#DTE_Field_editorFields-reservationTimeFromC");
    const toC = await I.grabValueFrom("#DTE_Field_editorFields-reservationTimeToC");

    if(fromC != dateValueFromC || toC != dateValueToC) {
        //TODO add legit error
        I.error("KOKOS");
    }

    //Close and delete reservation object
    I.click("div.DTE_Header button.btn-close-editor");
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});