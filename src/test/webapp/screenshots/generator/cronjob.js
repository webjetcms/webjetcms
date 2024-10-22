Feature('apps.cronjob');

var edit_button = (locate("button.btn.btn-sm.buttons-selected.buttons-edit.btn-warning"));

Before(({ login }) => {
    login('admin');
});

Scenario('cronjob screenshots', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/settings/cronjob");

    Document.screenshot("/admin/settings/cronjob/dataTable.png");
    DT.filter('taskName', 'Zmazanie starých udalostí v kalendári');
    I.clickCss('.buttons-select-all');
    I.click(edit_button);
    Document.screenshotElement('.DTE.modal-content.DTE_Action_Edit',"/admin/settings/cronjob/editor.png", 1280,1000 );
});