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
    Document.screenshotElement("div.modal.show div.modal-dialog", "/redactor/datatables/export-dialog-advanced.png");
});

Scenario('import-users', ({ I, Document }) => {
    I.amOnPage("/admin/v9/users/user-list/");

    Document.screenshotElement("button[data-dtbtn=import]", "/redactor/datatables/import-icon.png");

    //import
    I.click("button[data-dtbtn=import]");
    I.wait(1);
    I.click("Aktualizovať existujúce záznamy");
    Document.screenshot("/redactor/datatables/import-dialog.png", 1280, 500);

});
