Feature('datatables');

Before(({ I, login }) => {
    login('admin');
});


Scenario('Zobrazenie nazvu v hlavicke', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=11");
    DTE.waitForEditor();
    I.wait(4);

    Document.screenshot("/redactor/datatables/dt-header-title.png", 1280, 450);

    I.say("Oznac 2 riadky");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=221");
    I.waitForText("page-2021-02-23-134924-937")

    I.forceClick("#datatableInit tbody tr:nth-child(1) td.dt-select-td");
    I.forceClick("#datatableInit tbody tr:nth-child(2) td.dt-select-td");

    I.clickCss("#datatableInit_wrapper button.buttons-remove");

    Document.screenshot("/redactor/datatables/dt-delete-confirm.png", 1280, 450);

});