const SL = require("./shared-logic");

Feature('apps.reservation.time-book-app');

Scenario('Remove discount', ({I, DT, DTE}) => {
    I.relogin('admin');
    SL.setDiscount(I, DT, DTE, 0);
});

async function deleteOldReservations(I, DT) {
    I.amOnPage("/apps/reservation/admin/");

    I.fillField({css: "input.dt-filter-from-dateFrom"}, "01.01.2045");
    I.fillField({css: "input.dt-filter-to-dateFrom"}, "01.01.2045");
    I.click({css: "button.dt-filtrujem-dateFrom"});

    DT.filterContains("editorFields.selectedReservation", "Tenisovy kurt");

    let rows = await I.getTotalRows();
    if (rows > 0) {
        I.clickCss("button.buttons-select-all");
        I.clickCss("button.custom-buttons-remove");
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
}

async function setReservationDate(I, date) {
    I.say("Set reservation date: " + date);
    I.waitForVisible("#reservationDate");
    I.wait(1);
    I.fillField("#reservationDate", date);
    I.wait(3);
    //I.fillField("#reservationDate", date);
    //I.wait(1);
    const datePickerValue = await I.grabValueFrom("#reservationDate");

    //change date from 01-01-2045 to 2045-01-01
    date = date.split("-").reverse().join("-");
    I.assertEqual(datePickerValue, date);
}

Scenario('Remove old reservations', async ({I, DT}) => {
    I.relogin('admin');
    await deleteOldReservations(I, DT);
});

Scenario('Check reservation TABLE + logic', async ({I}) => {
    I.relogin('admin');
    I.amOnPage("/apps/rezervacie/rezervacia-tenisovych-kurtov.html");

    I.say("Set date in future");
    await setReservationDate(I, "01-01-2045");

    I.say("Check table composition");
        I.seeElement( locate("td[id='2560_13'].free").withText("0/2") );
        I.seeElement( locate("td[id='2560_14'].free").withText("0/2") );
        I.seeElement( locate("td[id='2560_15'].free").withText("0/2") );

        I.seeElement( locate("td[id='2561_13'].unsupported") );
        I.seeElement( locate("td[id='2561_14'].free").withText("0/3") );
        I.seeElement( locate("td[id='2561_15'].free").withText("0/3") );

    I.say("Check interval selection logic");
        prevent429(I);
        I.click(locate("td[id='2560_13'].free"));
        I.seeElement( locate("td[id='2560_13'].free.selectedTableCell") );

        prevent429(I);
        I.click(locate("td[id='2560_15'].free"));
        I.seeElement( locate("td[id='2560_15'].free.selectedTableCell") );
        I.dontSeeElement( locate("td[id='2560_13'].free.selectedTableCell") );

        prevent429(I);
        I.click(locate("td[id='2560_14'].free"));
        I.seeElement( locate("td[id='2560_15'].free.selectedTableCell") );
        I.seeElement( locate("td[id='2560_14'].free.selectedTableCell") );

        prevent429(I);
        I.click(locate("td[id='2560_13'].free"));
        I.seeElement( locate("td[id='2560_15'].free.selectedTableCell") );
        I.seeElement( locate("td[id='2560_14'].free.selectedTableCell") );
        I.seeElement( locate("td[id='2560_13'].free.selectedTableCell") );

        prevent429(I);
        I.click(locate("td[id='2560_14'].free"));
        I.dontSeeElement( locate("td[id='2560_15'].free.selectedTableCell") );
        I.dontSeeElement( locate("td[id='2560_14'].free.selectedTableCell") );
        I.dontSeeElement( locate("td[id='2560_13'].free.selectedTableCell") );

        prevent429(I);
        I.click(locate("td[id='2560_14'].free"));
        I.dontSeeElement( locate("td[id='2560_15'].free.selectedTableCell") );
        I.seeElement( locate("td[id='2560_14'].free.selectedTableCell") );
        I.dontSeeElement( locate("td[id='2560_13'].free.selectedTableCell") );

        prevent429(I);
        I.click(locate("td[id='2561_15'].free"));
        I.seeElement( locate("td[id='2561_15'].free.selectedTableCell") );
        I.dontSeeElement( locate("td[id='2560_15'].free.selectedTableCell") );
        I.dontSeeElement( locate("td[id='2560_14'].free.selectedTableCell") );
        I.dontSeeElement( locate("td[id='2560_13'].free.selectedTableCell") );

    I.say("Reload page and check if selected DATE is still same");
        I.refreshPage();
        const datePickerValue = await I.grabValueFrom("#reservationDate");
        I.assertEqual(datePickerValue, "2045-01-01");

    I.say("Check reservation FORM as LOGGED user");
        I.seeInField("#name", "Tester");
        I.seeInField("#surname", "Playwright");
        I.seeInField("#email", "tester@balat.sk");

    I.say("Check reservation FORM as NOT LOGGED user");
        I.logout();
        I.amOnPage("/apps/rezervacie/rezervacia-tenisovych-kurtov.html");
        I.waitForVisible("#reservationDate");
        await setReservationDate(I, "01-01-2045");

        I.seeInField("#name", "");
        I.seeInField("#surname", "");
        I.seeInField("#email", "");
});

/**
 * Prevent error 429 too many requests
 * @param {*} I
 */
function prevent429(I) {
    I.wait(1);
}

Scenario('Check reservation create logic', async ({I}) => {
    I.relogin('admin');
    I.amOnPage("/apps/rezervacie/rezervacia-tenisovych-kurtov.html");
    I.waitForVisible("#reservationDate");
    I.say("Set date in future");
    await setReservationDate(I, "01-01-2045");

    I.waitForInvisible("button.btn.btn-primary", 1);

    I.say("Create reservation - as LOGGED USER");
    I.seeElement( locate("td[id='2560_13'].free").withText("0/2") );
    I.click( locate("td[id='2560_13'].free") );
    I.seeInField("#timeRange", "13:00 - 14:00");
    I.seeInField("#price", "70");

    prevent429(I)
    I.click( locate("td[id='2560_14'].free") );
    prevent429(I)
    I.click( locate("td[id='2560_15'].free") );
    I.seeInField("#timeRange", "13:00 - 16:00");
    I.seeInField("#price", "210");

    I.waitForVisible("button.btn.btn-primary", 1);
    I.clickCss("button.btn.btn-primary");

    I.say("This object is auto approve, see changes");
        I.waitForVisible( locate("div.alert.alert-success").withText("Vaša rezervácia bola úspešne vytvorená a schválená.") );
        I.seeElement( locate("td[id='2560_15'].free").withText("1/2") );
        I.seeElement( locate("td[id='2560_14'].free").withText("1/2") );
        I.seeElement( locate("td[id='2560_13'].free").withText("1/2") );

    I.say("Reserve same object different interval and see changes");
        prevent429(I);
        I.click( locate("td[id='2560_13'].free") );
        prevent429(I);
        I.click( locate("td[id='2560_14'].free") );
        I.seeInField("#timeRange", "13:00 - 15:00");
        I.seeInField("#price", "140");
        I.clickCss("button.btn.btn-primary");

        I.waitForVisible( locate("div.alert.alert-success").withText("Vaša rezervácia bola úspešne vytvorená a schválená.") );
        I.seeElement( locate("td[id='2560_15'].free").withText("1/2") );
        I.seeElement( locate("td[id='2560_14'].full").withText("2/2") );
        I.seeElement( locate("td[id='2560_13'].full").withText("2/2") );

    I.say("Create reservation for Another object that NEEDS approval - so changes will be visible AFTER approval by approver");
        prevent429(I);
        I.click( locate("td[id='2561_14'].free") );
        prevent429(I);
        I.click( locate("td[id='2561_15'].free") );
        I.seeInField("#timeRange", "14:00 - 16:00");

        I.say("CHECK that not logged user does NOT have discount");
        I.seeInField("#price", "60");
        I.clickCss("button.btn.btn-primary");

        I.waitForVisible( locate("div.alert.alert-success").withText("Vaša rezervácia bola úspešne vytvorená a teraz čaká na schválenie.") );
        I.seeElement( locate("td[id='2561_14'].free").withText("0/3") );
        I.seeElement( locate("td[id='2561_15'].free").withText("0/3") );


    I.say("Create reservation - as NOT LOGGED USER");
        I.logout();
        I.amOnPage("/apps/rezervacie/rezervacia-tenisovych-kurtov.html");

        await setReservationDate(I, "01-01-2045");

        prevent429(I);
        I.click( locate("td[id='2561_14'].free") );
        prevent429(I);
        I.click( locate("td[id='2561_15'].free") );
        I.seeInField("#timeRange", "14:00 - 16:00");
        I.seeInField("#price", "60");

        I.fillField("#name", "UnknowUser");
        I.fillField("#surname", "UnknowSurname");
        I.fillField("#email", "sebastian.ivan@interway.sk");
        I.clickCss("button.btn.btn-primary");

        I.waitForVisible( locate("div.alert.alert-success").withText("Vaša rezervácia bola úspešne vytvorená a teraz čaká na schválenie.") );
        I.seeElement( locate("td[id='2561_14'].free").withText("0/3") );
        I.seeElement( locate("td[id='2561_15'].free").withText("0/3") );
});

Scenario('Check created reservations', ({I, DT}) => {
    I.relogin('admin');
    I.amOnPage("/apps/reservation/admin/");

    I.fillField({css: "input.dt-filter-from-dateFrom"}, "01.01.2045");
    I.fillField({css: "input.dt-filter-to-dateFrom"}, "01.01.2045");
    I.click({css: "button.dt-filtrujem-dateFrom"});

    DT.filterContains("editorFields.selectedReservation", "Tenisovy kurt");

    DT.checkTableRow("reservationDataTable", 1, ["", "Tester", "Playwright", "tester@balat.sk", "Tenisovy kurt A", "01.01.2045", "01.01.2045", "13:00", "16:00", "210,00", "Schválená"]);
    DT.checkTableRow("reservationDataTable", 2, ["", "Tester", "Playwright", "tester@balat.sk", "Tenisovy kurt A", "01.01.2045", "01.01.2045", "13:00", "15:00", "140,00", "Schválená"]);
    DT.checkTableRow("reservationDataTable", 3, ["", "Tester", "Playwright", "tester@balat.sk", "Tenisovy kurt B", "01.01.2045", "01.01.2045", "14:00", "16:00", "60,00", "Nepotvrdená"]);
    DT.checkTableRow("reservationDataTable", 4, ["", "UnknowUser", "UnknowSurname", "sebastian.ivan@interway.sk", "Tenisovy kurt B", "01.01.2045", "01.01.2045", "14:00", "16:00", "60,00", "Nepotvrdená"]);
});

Scenario('Delete created reservations', async ({I, DT}) => {
    I.relogin('admin');
    await deleteOldReservations(I, DT);
});