Feature('forms');

Before(({ I, login }) => {
    login('admin');
});

Scenario('forms', ({ I , DT, DTE, Document }) => {
    I.amOnPage("/apps/form/admin/#/detail/Dotaznik-spokojnosti-externy");
    DT.waitForLoader();

    Document.screenshot("/redactor/apps/form/detail.png");

    I.click("08.07.2019 14:31:40");

    Document.screenshot("/redactor/apps/form/detail-editnote.png");

    DTE.cancel();

    I.click("button.btn-export-dialog");
    I.wait(2);
    I.click("#pills-export-advanced-tab");
    Document.screenshotElement("div.modal.show div.DTE_Action_Edit.modal-content", "/redactor/apps/form/export-advanced.png");
});
