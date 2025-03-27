Feature('reservation-object');

Before(({ I, login }) => {
    login('admin');
});

Scenario('reservation object screens', async ({ I, DT, DTE, Document }) => {
    let reservationObjectNameA = "screenshotReservationA";
    let reservationObjectNameB = "screenshotReservationB";

    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-datatable.png");

    DT.filterContains("name", reservationObjectNameB);
    I.click(reservationObjectNameB);
    DTE.waitForEditor("reservationObjectDataTable");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_basic_tab_2.png");
    DTE.cancel();

    DT.filterContains("name", reservationObjectNameA);
    I.click(reservationObjectNameA);
    DTE.waitForEditor("reservationObjectDataTable");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_basic_tab.png");

    I.clickCss("#pills-dt-reservationObjectDataTable-advanced-tab");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_advance_tab.png");

    I.clickCss("#pills-dt-reservationObjectDataTable-chooseDays-tab");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_chooseDay_tab.png");

    I.clickCss("#pills-dt-reservationObjectDataTable-specialPrice-tab");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_prices_tab.png");

    I.clickCss("#datatableFieldDTE_Field_editorFields-objectPrices_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    Document.screenshot("/redactor/apps/reservation/reservation-objects/reservation_object-editor_prices_add.png");
});