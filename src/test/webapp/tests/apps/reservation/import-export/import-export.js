const SL = require("../shared-logic");
Feature('apps.reservation.import-export');

var randomNumber;
const reservationObjectName = 'reservation_object_iexport_test';
const reservationObjectPastFuture = 'reservation_object_past_future_test';
const succMsgInfo = 'Vami zadanú rezerváciu je možné uložiť.';

Before(({ I, DT, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
    DT.addContext('reservation', '#reservationDataTable_wrapper');
});

Scenario('Delete reservation_object_iexport_test records', async ({ I, DT, DTE }) => {
    await SL.deleteReservation(I, DT, DTE, 'editorFields.selectedReservation', reservationObjectName);
});

Scenario('Export and import reservation', async ({ I, DT, DTE }) => {
    I.say("ADDING a reservation");
    I.amOnPage("/apps/reservation/admin/");
    I.click(DT.btn.reservation_add_button);
    DTE.waitForEditor("reservationDataTable");

    const { startDate, endDate, startTime, endTime } = SL.generateReservationDatesAndTimes();
    SL.setReservation(I, reservationObjectName, startDate, endDate, startTime, endTime, succMsgInfo);
    DTE.save();
    DT.filterEquals('editorFields.selectedReservation', reservationObjectName);
    I.clickCss('.buttons-select-all');
    I.click(DT.btn.reservation_export_button);
    DTE.waitForModal('datatableExportModal');
    const downloadedFile = `export-${randomNumber}.xlsx`;
    I.handleDownloads(`downloads/${downloadedFile}`);
    I.clickCss('#submit-export');
    I.amInPath('../../../build/test/downloads');
    I.waitForFile(downloadedFile, 30);

    await SL.deleteReservation(I, DT, DTE,'editorFields.selectedReservation', reservationObjectName);

    I.click(DT.btn.reservation_import_button);
    DTE.waitForModal('datatableImportModal');
    I.attachFile('#insert-file',`../../../build/test/downloads/${downloadedFile}`);
    I.clickCss('#submit-import');
    DT.waitForLoader();
    DT.checkTableRow("reservationDataTable", 1, [
            "", "Tester", "Playwright", "tester@balat.sk","reservation_object_iexport_test", 
            startDate, endDate, startTime, endTime]
    );
});

Scenario('Delete import.sk records', async ({ I, DT, DTE }) => {
    I.amOnPage('/apps/reservation/admin/');
    DT.waitForLoader();
    DT.filterEndsWith('email', '@import.sk');
    let rows = await I.getTotalRows();
    if (rows > 0) {
        I.clickCss('.buttons-select-all');
        I.clickCss("button.custom-buttons-remove");
        DTE.waitForModal('reservationDataTable_modal > div > div');
        DTE.save();
    }
    I.see('Nenašli sa žiadne vyhovujúce záznamy');
});

Scenario('Import reservation with xlsx file', async ({ I, DT, DTE }) => {
    I.amOnPage("/apps/reservation/admin/");
    I.click(DT.btn.reservation_import_button);
    DTE.waitForModal('datatableImportModal');
    I.attachFile('#insert-file', 'tests/apps/reservation/import-export/autotest-import.xlsx');
    I.clickCss('#submit-import');
    DTE.waitForModalClose('datatableImportModal');
    DT.waitForLoader();
    DT.filterContains('email', 'import.sk');
    
    DT.checkTableRow("reservationDataTable", 1, [
        "", "Tester2", "Playwright2", "tester2@import.sk", "Sauna", 
        "21.03.2010", "21.03.2010", "12:00", "15:00"
    ]);
    
    DT.checkTableRow("reservationDataTable", 2, [
        "", "Tester2", "Playwright2", "tester2@import.sk", "Sauna", 
        "18.03.2055", "18.03.2055", "12:00", "14:00"
    ]);
    
    DT.checkTableRow("reservationDataTable", 3, [
        "", "Tester", "Playwright", "tester@import.sk", "Sauna", 
        "18.10.2055", "19.10.2055", "08:00", "10:00"
    ]);
    
    DT.checkTableRow("reservationDataTable", 4, [
        "", "Tester", "Playwright", "tester@import.sk", "reservation_object_iexport_test", 
        "03.09.2055", "03.09.2055", "08:00", "16:00"
    ]);
    
    DT.checkTableRow("reservationDataTable", 5, [
        "", "Tester", "Playwright", "tester@import.sk", "Spa celodenné", 
        "15.01.2055", "16.01.2055", "00:00", "00:00"
    ]);
    
});

Scenario('Delete reservation_object_past_future_test', async ({ I, DT, DTE }) => {
    await SL.deleteReservation(I, DT, DTE, 'editorFields.selectedReservation ', reservationObjectPastFuture);
});

Scenario('Update reservation with xlsx file in past and future', ({ I, DT, DTE }) => {
    I.amOnPage("/apps/reservation/admin/");
    I.click(DT.btn.reservation_import_button);
    DTE.waitForModal('datatableImportModal');
    I.attachFile('#insert-file', 'tests/apps/reservation/import-export/autotest-update-before.xlsx');
    I.clickCss('#submit-import');
    DTE.waitForModalClose('datatableImportModal');
    DT.waitForLoader();

    DT.filterContains('editorFields.selectedReservation', reservationObjectPastFuture);
    
    DT.checkTableRow("reservationDataTable", 2, [
        "", "Tester", "Playwright", "tester@balat.sk", "reservation_object_past_future_test", 
        "25.07.2002", "25.07.2002", "12:00", "15:00"
    ]);
    
    DT.checkTableRow("reservationDataTable", 1, [
        "", "Tester", "Playwright", "tester@balat.sk", "reservation_object_past_future_test", 
        "30.07.2050", "30.07.2050", "13:00", "14:00"
    ]);
    
    I.click(DT.btn.reservation_import_button);
    DTE.waitForModal('datatableImportModal');
    I.attachFile('#insert-file', 'tests/apps/reservation/import-export/autotest-update-after.xlsx');
    I.clickCss('#dt-settings-import3');
    I.selectOption('#dt-settings-update-by-column','reservationObjectId');

    I.clickCss('#submit-import');
    //tu spadne test
    DTE.waitForModalClose('datatableImportModal');
    DT.waitForLoader();

    DT.filterContains('editorFields.selectedReservation', reservationObjectPastFuture);

    DT.checkTableRow("reservationDataTable", 1, [
        "", "Testerchange", "Playwrightchange", "tester@change.sk", "reservation_object_past_future_test", 
        "12.06.2050", "12.06.2050", "14:00", "16:00"
    ]); 
});