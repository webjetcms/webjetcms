Feature('reservation-object');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('reservation object screens', async ({ I, DataTables, Document, DTE }) => {
    let reservationObjectName = "autotest-reservation_object_screen-" + randomNumber;

    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-datatable.png");

    I.click("div.dt-buttons button.buttons-create");
    I.dtWaitForEditor("reservationObjectDataTable");

    I.click("#DTE_Field_name");
    I.fillField("#DTE_Field_name", reservationObjectName);
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_basic_tab.png");

    //Not important but required
    I.click("#DTE_Field_description");
    I.fillField("#DTE_Field_description", "Random text");

    I.click("#pills-dt-reservationObjectDataTable-advanced-tab");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_advance_tab.png");

    I.click("#pills-dt-reservationObjectDataTable-chooseDays-tab");
    I.click("#DTE_Field_editorFields-chooseDayA_0");
    I.click("#DTE_Field_editorFields-chooseDayC_0");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_chooseDay_tab.png");

    //Save this reservation object
    DTE.save();
    I.wait(1);

    I.fillField("input.dt-filter-name", reservationObjectName);
    I.pressKey('Enter', "dt-filter-name");
    I.wait(1);
    I.click(reservationObjectName);

    I.click("#pills-dt-reservationObjectDataTable-specialPrice-tab");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_prices_tab.png");

    I.click("#datatableFieldDTE_Field_editorFields-objectPrices_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_prices_add.png");

    I.click("#datatableFieldDTE_Field_editorFields-objectPrices_modal > div > div > div.DTE_Header.modal-header > button");
    I.click("#reservationObjectDataTable_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-outline-secondary.btn-close-editor");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});