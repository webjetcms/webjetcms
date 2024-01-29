Feature('webpages.recover-action');

let recoveryWebPage = (locate('#datatableInit_wrapper').find("button.btn.btn-sm.btn-outline-secondary.button-recover-page"));
let recoveryFolder = (locate('.col-md-4.tree-col').find("button.btn.btn-sm.buttons-selected.btn-outline-secondary.button-recover-group"));

let delete_webpage_button = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger'));
let delete_folder_button = (locate('.col-md-4.tree-col').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger'));

let edit_folder_button = (locate('.col-md-4.tree-col').find('.btn.btn-sm.buttons-selected.buttons-edit.noperms-editDir.btn-warning'));

Before(({ I }) => {
    I.relogin("admin");
});


Scenario('recover screens', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");
    DT.filter("title", "page_to_delete");
    I.click("td.dt-select-td.sorting_1");
    I.click(delete_webpage_button);
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.click('Zmazať');
    DTE.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.click("#pills-trash-tab");
    DT.waitForLoader();
    DT.filter("title", "page_to_delete");
    Document.screenshot("/redactor/webpages/recover.png", 1410, 760);
    I.see("page_to_delete");
    I.click("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    Document.screenshotElement(recoveryWebPage, "/redactor/webpages/recover-button.png");
    Document.screenshotElement("#folderIdInputWrapper", "/redactor/webpages/recover-folder-id-1.png");
    I.click(recoveryWebPage);
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy");
    I.moveCursorTo("div.toast-success");
    Document.screenshotElement("div.toast-success", "/redactor/webpages/recover-page-success.png");


    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");
    I.click( locate("a.jstree-anchor").withText("recoverSubFolderOne") );
    I.click(delete_folder_button);
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.click('Zmazať');
    DTE.waitForLoader();
    I.click("#pills-trash-tab");
    DT.waitForLoader();
    I.fillField("#tree-folder-id", 59672);
    I.pressKey('Enter');
    DT.waitForLoader();
    Document.screenshotElement("#folderIdInputWrapper", "/redactor/webpages/recover-folder-id-2.png");
    I.click(recoveryFolder);
    I.waitForElement("div.toast-info");
    I.see("Ste si istý, že chete obnoviť priečinok", "div.toast-title");
    I.moveCursorTo("div.toast-title");
    Document.screenshotElement("div.toast-info", "/redactor/webpages/recover-folder-info.png");
    I.click(".toastr-buttons button.btn-primary");
    I.waitForElement("div.toast-success");
    I.moveCursorTo("div.toast-title");
    Document.screenshotElement("div.toast-success", "/redactor/webpages/recover-folder-success.png");
});