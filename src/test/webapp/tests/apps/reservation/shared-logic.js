module.exports = {

    verifyReservationPrice(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo, expectedPrice) {
        // Nastav rezerváciu
        setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo);
        
        //Click to another field so change is triggered
        I.clickCss("#DTE_Field_purpose");
        I.waitForValue("#DTE_Field_price", expectedPrice, 10);
    },

    setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo, infoMsg = null) {
        setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo, infoMsg);
    },

    async deleteReservationObject(I, DT, reservationObjectName) {
        I.amOnPage("/apps/reservation/admin/reservation-objects/");
        DT.filter("name", reservationObjectName);
        let rows = await I.getTotalRows();
        if (rows > 0) {
            I.clickCss("td.dt-select-td.sorting_1");
            I.clickCss("button.buttons-remove");
            I.waitForElement("div.DTE_Action_Remove");
            I.click("Zmazať", "div.DTE_Action_Remove");
            I.see("Nenašli sa žiadne vyhovujúce záznamy");
        }
    },

    setDiscount(I, DT, DTE, discount) {
        I.amOnPage("/admin/v9/users/user-groups/");
        DT.waitForLoader();
        DT.filter("id", 1273);
        I.click("ReservationDiscount");
        DTE.waitForEditor('userGroupsDataTable');
    
        I.fillField("#DTE_Field_priceDiscount", discount);
        DTE.save();
    }
}

function setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo, infoMsg) {
    //Select our reservation object
    if(reservationObjectName !== null) {
        I.clickCss("#panel-body-dt-reservationDataTable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_select.DTE_Field_Name_reservationObjectId > div.col-sm-7 > div.DTE_Field_InputControl > div > button");
        I.fillField("body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input", reservationObjectName);
        I.pressKey('Enter');
        I.clickCss("#DTE_Field_purpose"); //Click to hide dropdown with options (fasten test)
    }

    //Set reservation date
    setReservationFields(I, "#DTE_Field_dateFrom", dateFrom, "#DTE_Field_dateTo", dateTo);

    //Set reservation time
    setReservationFields(I, "#DTE_Field_editorFields-reservationTimeFrom", timeFrom, "#DTE_Field_editorFields-reservationTimeTo", timeTo);

    //returns infoMsg if necessary 
    if (infoMsg != null) {
        I.checkOption("#DTE_Field_editorFields-showReservationValidity_0");
        I.seeElement("#DTE_Field_editorFields-infoLabel2");
        I.waitForValue("#DTE_Field_editorFields-infoLabel2", infoMsg, 10);
    }
}

function setReservationFields(I, fromSelector, fromValue, toSelector, toValue) {
    I.clickCss(fromSelector);
    I.fillField(fromSelector, fromValue);
    I.pressKey('Enter');

    I.clickCss(toSelector);
    I.fillField(toSelector, toValue);
    I.pressKey('Enter');
}

