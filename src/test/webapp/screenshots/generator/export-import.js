Feature('export-import');

Before(({ I, login }) => {
    login('admin');
});

Scenario('export-users', ({ I, Document }) => {
    I.amOnPage("/admin/v9/users/user-list/");

    Document.screenshotElement("button[data-dtbtn=export]", "/redactor/datatables/export-icon.png");

    //export
    I.click("button[data-dtbtn=export]");
    Document.screenshot("/redactor/datatables/export-dialog.png", 1280, 430);

    I.click("#pills-export-advanced-tab");
    Document.screenshotElement("#datatableExportModal > div > div.DTE_Action_Edit", "/redactor/datatables/export-dialog-advanced.png");
});

Scenario('import-users', ({ I, Document }) => {
    I.amOnPage("/admin/v9/users/user-list/");

    Document.screenshotElement("button[data-dtbtn=import]", "/redactor/datatables/import-icon.png");

    //import
    I.click("button[data-dtbtn=import]");
    I.wait(1);

    if("sk" === I.getConfLng()) {
        I.click("Aktualizovať existujúce záznamy");
    } else if("en" === I.getConfLng()) {
        I.click("Update existing records");
    }

    Document.screenshot("/redactor/datatables/import-dialog.png", 1280, 500);
});

Scenario('Import skip wrong data', ({ I, Document }) => {
    let fileName = 'screenshots/generator/wrong-empty-data-user-list.xlsx';

    I.relogin("tester");
    I.amOnPage("/admin/v9/users/user-list/");

    I.say("IF SkipWrong IS NOT checked, import will fail");
    insertFile(I, fileName, false);
    I.waitForText("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");
    I.waitForText("email - Nesprávna emailová adresa. Zadajte email vo formáte meno@domena.");

    Document.screenshotElement("div.DTE_Action_Edit", "/redactor/datatables/import_error.png");
    I.clickCss("#datatableImportModal > div > div > div.modal-footer > button.btn-outline-secondary");

    I.say("IF SkipWrong IS checked, import will NOT fail");
    insertFile(I, fileName, true);
    I.dontSee("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");
    I.dontSee("email - Nesprávna emailová adresa. Zadajte email vo formáte meno@domena.");
    I.dontSee("editorFields.login - Povinné pole. Zadajte aspoň jeden znak.");
    I.waitForInvisible("#datatableImportModal");

    I.waitForElement("#toast-container-webjet");
    I.moveCursorTo('#toast-container-webjet');
    Document.screenshotElement("#toast-container-webjet", "/redactor/datatables/import_err_notification.png");
});

function insertFile(I, fileName, skipWrong) {
    I.clickCss("button.btn-import-dialog");
    I.waitForElement("#datatableImportModal");

    I.wait(1);
    I.attachFile('#insert-file', fileName);
    I.waitForEnabled("#submit-import", 5);

    if(skipWrong === true) {
         I.checkOption( locate("#datatableImportModal").find("#skip-wrong-data") );
    } else {
         I.uncheckOption( locate("#datatableImportModal").find("#skip-wrong-data") );
    }

    I.click("#submit-import");
}
