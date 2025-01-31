Feature('apps.cronjob');


Before(({ login }) => {
    login('admin');
});

Scenario('cronjob screenshots', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/settings/cronjob");

    Document.screenshot("/admin/settings/cronjob/dataTable.png");
    DT.filterContains('taskName', 'Zmazanie starých udalostí v kalendári');
    I.clickCss('.buttons-select-all');
    I.click(DT.btn.edit_button);
    Document.screenshotElement('.DTE.modal-content.DTE_Action_Edit',"/admin/settings/cronjob/editor.png", 1280,1000 );
});