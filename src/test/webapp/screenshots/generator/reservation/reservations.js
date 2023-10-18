Feature('reservations');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('reservation screens', async ({ I, DataTables, Document, DTE }) => {
    let reservationObjectNameA = "autotest-reservation_object_A_screen-" + randomNumber;
    let reservationObjectNameB = "autotest-reservation_object_B_screen-" + randomNumber;
    let deletePassword = "right_password";

    //Prepare some reservation objects
    I.amOnPage("/apps/reservation/admin/reservation-objects/");

    //Object A is normal reservation obejct, NO for all day, without approver
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationObjectDataTable");
    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", reservationObjectNameA);
    //Not important but required
    I.click("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Random text");
    //Save this reservation object
    DTE.save();
    I.wait(1);

    //Object B is advanced reservation obejct, for all day, with approver
    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationObjectDataTable");
    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", reservationObjectNameB);
    //Not important but required
    I.click("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Random text");
    //Set for all day
    I.click("#DTE_Field_reservationForAllDay_0");
    //Change tab
    I.click("#pills-dt-reservationObjectDataTable-advanced-tab");
    //Set approver
    I.click("#DTE_Field_mustAccepted_0");
    I.click("#DTE_Field_emailAccepter");
    //Set bad email on purpose ... we want to be rejected :D
    I.fillField("#DTE_Field_emailAccepter", "BAD_EMAIL@balat.sk");
    //Set delete password
    I.click("#DTE_Field_editorFields-addPassword_0");
    I.click("#DTE_Field_editorFields-newPassword");
    I.fillField("#DTE_Field_editorFields-newPassword", deletePassword);
    I.click("#DTE_Field_editorFields-passwordCheck");
    I.fillField("#DTE_Field_editorFields-passwordCheck", deletePassword);
    //Save this reservation object
    DTE.save();
    I.wait(1);

    I.amOnPage("/apps/reservation/admin/");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-datatable.png");

    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationDataTable");

    setReservationObejct(I, reservationObjectNameA);
    I.click("#DTE_Field_purpose");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_1.png");

    setReservationObejct(I, reservationObjectNameB);
    I.click("#DTE_Field_purpose");
    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_2.png");

    setReservationDate(I, "01.01.2000", "01.01.2000");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_3.png");

    I.click("#DTE_Field_editorFields-showReservationValidity_0");
    I.scrollTo("#DTE_Field_editorFields-reservationTimeRangeG");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_basic_tab_4.png");

    I.click("#pills-dt-reservationDataTable-personalInfo-tab");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_personalInfo_tab.png");

    I.click("#pills-dt-reservationDataTable-specialPrice-tab");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_specialPrice_tab.png");

    I.click("#pills-dt-reservationDataTable-basic-tab");
    setReservationDate(I, "01.01.2100", "01.01.2100");
    DTE.save();
    I.wait(1);

    I.fillField("#reservationDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-text.dt-th-editorFields-selectedReservation > form > div > input", reservationObjectNameB);
    I.pressKey('Enter', "#reservationDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-text.dt-th-editorFields-selectedReservation > form > div > input");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("#reservationDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(5)");
    I.click("Potvrdiť", "div.toastr-buttons");
    I.wait(1);
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/approve_no_right.png");

    //Change approver email
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    I.fillField("input.dt-filter-name", reservationObjectNameB);
    I.pressKey('Enter', "dt-filter-name");
    I.wait(1);
    I.click(reservationObjectNameB);
    I.click('#pills-dt-reservationObjectDataTable-advanced-tab');
    I.click("#DTE_Field_emailAccepter");
    //Set good email now
    I.fillField("#DTE_Field_emailAccepter", "tester@balat.sk");
    DTE.save();
    I.wait(1);

    //Return to reservation page
    I.amOnPage("/apps/reservation/admin/");
    I.fillField("#reservationDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-text.dt-th-editorFields-selectedReservation > form > div > input", reservationObjectNameB);
    I.pressKey('Enter', "#reservationDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-text.dt-th-editorFields-selectedReservation > form > div > input");
    I.wait(1);

    I.click(reservationObjectNameB);
    I.click("#pills-dt-reservationDataTable-acceptation-tab");
    Document.screenshot("/redactor/apps/reservation/reservations/reservation-editor_acceptation_tab.png");

    //Approve
    I.click("#DTE_Field_editorFields-acceptation_0");
    I.wait(1);

    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/approve_sure.png");
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.wait(1);
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/approved.png");
    I.click("#toast-container-webjet > div > button");

    //Reject
    I.click("#DTE_Field_editorFields-acceptation_1");
    I.wait(1);
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/reject_sure.png");
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.wait(1);
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/rejected.png");
    I.click("#toast-container-webjet > div > button");

    //Reset
    I.click("#DTE_Field_editorFields-acceptation_2");
    I.wait(1);
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/reset_sure.png");
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.wait(1);
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/reset.png");
    I.click("#toast-container-webjet > div > button");

    //Close editor
    I.click("#reservationDataTable_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-outline-secondary.btn-close-editor");

    //Logic about delete system
    I.click("td.dt-select-td.sorting_1");
    //Try delete, use WRONG password
    I.click("#reservationDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(4)");
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/set_password.png");
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-input > input");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", "Wrong_password_test");
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    Document.screenshotElement("#reservationDataTable_modal > div > div", "/redactor/apps/reservation/reservations/delete_dialog.png");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/apps/reservation/reservations/delete_error.png");
    //Try delete, use GOOD password
    I.click("td.dt-select-td.sorting_1");
    I.click("#reservationDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(4)");
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-input > input");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", deletePassword);
    I.click("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Remove reservation objects
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    //Object A
    I.fillField("input.dt-filter-name", reservationObjectNameA);
    I.pressKey('Enter', "dt-filter-name");
    I.wait(1);
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    //Object B
    I.fillField("input.dt-filter-name", reservationObjectNameB);
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
        setReservationObejct(I, reservationObjectName);
    }

    //Set reservation date
    setReservationDate(I, dateFrom, dateTo);

    //Set reservation time
    setReservationTime(I, timeFrom, timeTo);
}

function setReservationObejct(I, reservationObjectName) {
    I.click("#panel-body-dt-reservationDataTable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_select.DTE_Field_Name_reservationObjectId > div.col-sm-7 > div.DTE_Field_InputControl > div > button");
    I.fillField("body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input", reservationObjectName);
    I.pressKey('Enter');
}

function setReservationDate(I, dateFrom, dateTo) {
    //From
    I.click("#DTE_Field_dateFrom");
    I.fillField("#DTE_Field_dateFrom", dateFrom);
    I.pressKey('Enter');

    //To
    I.click("#DTE_Field_dateFrom");
    I.fillField("#DTE_Field_dateTo", dateTo);
    I.pressKey('Enter');
}

function setReservationTime(I, timeFrom, timeTo) {
    I.click("#DTE_Field_editorFields-reservationTimeFrom");
    I.fillField("#DTE_Field_editorFields-reservationTimeFrom", timeFrom);
    I.pressKey('Enter');

    I.click("#DTE_Field_editorFields-reservationTimeTo");
    I.fillField("#DTE_Field_editorFields-reservationTimeTo", timeTo);
    I.pressKey('Enter');
}