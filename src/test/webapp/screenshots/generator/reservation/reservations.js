Feature('reservations');

Before(({ I, login }) => {
    login('admin');
});

Scenario('reservation screens', async ({ I, DT, DTE, Document }) => {
    let reservationObjectNameA = "screenshotReservationA";
    let reservationObjectNameB = "screenshotReservationB";
    let deletePassword = "right_password";
    let confLng = I.getConfLng();

    I.say("Prepare some reservation object");
    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    DT.filterEquals("name", reservationObjectNameB);
    I.click(reservationObjectNameB);
    DTE.waitForEditor("reservationObjectDataTable");

    I.clickCss("#pills-dt-reservationObjectDataTable-advanced-tab");
    I.say("Set approver");
    I.checkOption("#DTE_Field_mustAccepted_0");

    I.say("Set bad email on purpose ... we want to be rejected :D");
    I.fillField("#DTE_Field_emailAccepter", "BAD_EMAIL@balat.sk");

    I.say("Set delete password");
    I.checkOption("#DTE_Field_editorFields-addPassword_0");
    I.fillField("#DTE_Field_editorFields-newPassword", deletePassword);
    I.fillField("#DTE_Field_editorFields-passwordCheck", deletePassword);

    DTE.save();

    I.amOnPage("/apps/reservation/admin/");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-datatable.png");

    I.clickCss("button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");


    setReservation(I, reservationObjectNameA, "01.01.2050", "01.01.2050", "08:00", "16:00");
    I.resizeWindow(1280, 1110);
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_1.png");
    I.resizeWindow(1280, 760);

    I.scrollTo("#DTE_Field_editorFields-reservationTimeRangeG");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_4.png");

    setReservationObject(I, reservationObjectNameB);
    setReservationCheckTime(I, "15:00", "10:00");

    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_2.png");

    if("sk" === confLng || "cs" === confLng) {
        setReservationDate(I, "01.01.2000", "01.01.2000");
    } else if("en" === confLng) {
        setReservationDate(I, "01/01/2000", "01/01/2000");
    }

    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_3.png");

    I.clickCss("#pills-dt-reservationDataTable-personalInfo-tab");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_personalInfo_tab.png");

    I.clickCss("#pills-dt-reservationDataTable-specialPrice-tab");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_specialPrice_tab.png");

    I.clickCss("#pills-dt-reservationDataTable-basic-tab");

    if("sk" === confLng || "cs" === confLng) {
        setReservationDate(I, "01.01.2100", "01.01.2100");
        setReservationCheckTime(I, "15:00", "10:00");
    } else if("en" === confLng) {
        setReservationDate(I, "01/01/2100", "01/01/2100");
        setReservationCheckTime(I, "15:00", "10:00");
    }

    DTE.save();

    //
    DT.filterEquals("editorFields.selectedReservation", reservationObjectNameB);

    I.clickCss("td.sorting_1");

    Document.screenshotElement("button.buttons-approve", "/redactor/apps/reservation/reservations/button-approve.png");
    Document.screenshotElement("button.buttons-reject", "/redactor/apps/reservation/reservations/button-reject.png");
    Document.screenshotElement("button.buttons-reset", "/redactor/apps/reservation/reservations/button-reset.png");

    I.clickCss("button.buttons-approve");

    if("sk" === confLng) {
        I.click("Potvrdiť", "div.toastr-buttons");
    } else if("en" === confLng) {
        I.click("Submit", "div.toastr-buttons");
    } else if("cs" === confLng){
        I.click("Potvrdit", "div.toastr-buttons");
    }

    I.waitForVisible('#toast-container-webjet');
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/approve_no_right.png");
    I.clickCss("button.toast-close-button");

    I.say("Change approver email");
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    DT.filterEquals("name", reservationObjectNameB);
    I.click(reservationObjectNameB);
    DTE.waitForEditor("reservationObjectDataTable");

    I.say("Set good email now");
    I.clickCss('#pills-dt-reservationObjectDataTable-advanced-tab');
    I.fillField("#DTE_Field_emailAccepter", "tester@balat.sk");
    DTE.save();

    I.say("Return to reservation page");
    I.amOnPage("/apps/reservation/admin/");
    DT.filterEquals("editorFields.selectedReservation", reservationObjectNameB);

    I.click(reservationObjectNameB);
    I.clickCss("#pills-dt-reservationDataTable-acceptation-tab");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_acceptation_tab.png");

    I.say("Approve - are you sure");
        I.clickCss("button.approve");
        I.waitForVisible("#toast-container-webjet", 30);
        I.moveCursorTo('#toast-container-webjet');
        Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/approve_sure.png");
        I.clickCss("button.toast-close-button");

    I.say("Was approved");
        I.clickCss(".toastr-buttons button.btn-primary");
        I.wait(2);
        I.waitForElement("#toast-container-webjet", 30);
        I.moveCursorTo('#toast-container-webjet');
        Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/approved.png");
        I.clickCss("button.toast-close-button");

    I.say("Reject - are you sure");
        I.clickCss("button.reject");
        I.waitForElement("#toast-container-webjet", 30);
        I.moveCursorTo('#toast-container-webjet');
        Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/reject_sure.png");
        I.clickCss("button.toast-close-button");

    I.say("Was rejected");
        I.clickCss(".toastr-buttons button.btn-primary");
        I.wait(2);
        I.waitForElement("#toast-container-webjet", 30);
        I.moveCursorTo('#toast-container-webjet');
        Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/rejected.png");
        I.clickCss("button.toast-close-button");

    I.say("Reset - are you sure");
        I.clickCss("button.reset");
        I.waitForElement("#toast-container-webjet", 30);
        I.moveCursorTo('#toast-container-webjet');
        Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/reset_sure.png");
        I.clickCss("button.toast-close-button");

    I.say("Was reset");
        I.clickCss(".toastr-buttons button.btn-primary");
        I.wait(2);
        I.waitForElement("#toast-container-webjet", 30);
        I.moveCursorTo('#toast-container-webjet');
        Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/reset.png");
        I.clickCss("button.toast-close-button");

    //Close editor
    I.clickCss("#reservationDataTable_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-outline-secondary.btn-close-editor");

    I.say("Logic about delete system");
    I.clickCss("td.sorting_1");

    I.say("Try delete, use WRONG password");
        I.clickCss("button.custom-buttons-remove");
        I.moveCursorTo('#toast-container-webjet');
        Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/set_password.png");
        I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", "Wrong_password_test");
        I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
        Document.screenshotElement("#reservationDataTable_modal > div > div", "/redactor/apps/reservation/reservations/delete_dialog.png");

    if("sk" === confLng) {
        I.click("Zmazať", "div.DTE_Action_Remove");
    } else if("en" === confLng) {
        I.click("Delete", "div.DTE_Action_Remove");
    } else if ("cs" === confLng){
        I.click("Smazat", "div.DTE_Action_Remove")
    }

    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/delete_error.png");
    I.clickCss("button.toast-close-button");
    I.say("Try delete, use GOOD password");
        I.clickCss("button.custom-buttons-remove");
        I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", deletePassword);
        I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
});

Scenario('Reservation app screen', ({ I, DT, Document }) => {
    Document.screenshotAppEditor(39096, "/redactor/apps/reservation/reservation-app/editor.png", function(Document, I) {
        I.clickCss("#pills-dt-component-datatable-componentIframeWindowTabList-tab");
        Document.screenshot("/redactor/apps/reservation/reservation-app/reservation-list.png");
        I.clickCss("#pills-dt-component-datatable-componentIframeWindowTabListObjects-tab");
        Document.screenshot("/redactor/apps/reservation/reservation-app/reservation-object-list.png");
        I.clickCss("#pills-dt-component-datatable-basic-tab");
    });

    I.amOnPage("apps/rezervacie/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/reservation/reservation-app/reservation-app.png");
});

function setReservation(I, reservationObjectName, dateFrom, dateTo, timeFrom, timeTo) {
    //Select our reservation object
    if(reservationObjectName !== null) {
        setReservationObject(I, reservationObjectName);
    }

    //Set reservation date
    setReservationDate(I, dateFrom, dateTo);

    //Set reservation time
    setReservationTime(I, timeFrom, timeTo);
}

function setReservationObject(I, reservationObjectName) {
    I.clickCss("#panel-body-dt-reservationDataTable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_select.DTE_Field_Name_reservationObjectId > div.col-sm-7 > div.DTE_Field_InputControl > div > button");
    I.fillField("body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input", reservationObjectName);
    I.pressKey('Enter');
}

function setReservationDate(I, dateFrom, dateTo) {
    //From
    I.clickCss("#DTE_Field_dateFrom");
    I.fillField("#DTE_Field_dateFrom", dateFrom);
    I.pressKey('Enter');

    //To
    I.clickCss("#DTE_Field_dateFrom");
    I.fillField("#DTE_Field_dateTo", dateTo);
    I.pressKey('Enter');
}

function setReservationTime(I, timeFrom, timeTo) {
    I.clickCss("#DTE_Field_editorFields-reservationTimeFrom");
    I.fillField("#DTE_Field_editorFields-reservationTimeFrom", timeFrom);
    I.pressKey('Enter');

    I.clickCss("#DTE_Field_editorFields-reservationTimeTo");
    I.fillField("#DTE_Field_editorFields-reservationTimeTo", timeTo);
    I.pressKey('Enter');

    I.clickCss("#DTE_Field_purpose");
}

function setReservationCheckTime(I, arrivingTime, departureTime) {
    I.clickCss("#DTE_Field_editorFields-arrivingTime");
    I.fillField("#DTE_Field_editorFields-arrivingTime", arrivingTime);
    I.pressKey('Enter');

    I.clickCss("#DTE_Field_editorFields-departureTime");
    I.fillField("#DTE_Field_editorFields-departureTime", departureTime);
    I.pressKey('Enter');

    I.clickCss("#DTE_Field_purpose");
}