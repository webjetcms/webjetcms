Feature('webpages.recover');

Before(({ I }) => {
    I.relogin("admin");
});


Scenario('recover screens', ({ I, DT, DTE, Document, i18n }) => {
    let confLng = I.getConfLng();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");
    DT.filterContains("title", "page_to_delete");
    I.clickCss("td.sorting_1");
    I.click(DT.btn.delete_button);
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');

    switch (confLng) {
        case "sk":
            I.click('Zmazať');
            DTE.waitForLoader();
            I.see("Nenašli sa žiadne vyhovujúce záznamy", "#datatableInit_wrapper .dt-scroll-body");
            break;
        case "en":
            I.click('Delete');
            DTE.waitForLoader();
            I.see("No matching records found", "#datatableInit_wrapper .dt-scroll-body");
            break;
        case "cs":
            I.click('Smazat');
            DTE.waitForLoader();
            I.see("Nenašly se žádné vyhovující záznamy", "#datatableInit_wrapper .dt-scroll-body");
            break;
        default:
            throw new Error("Unknown language: " + confLng);
    }


    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    DT.filterContains("title", "page_to_delete");
    Document.screenshot("/redactor/webpages/recover.png", 1410, 760);
    I.see("page_to_delete");

    I.clickCss("#datatableInit_wrapper > div:nth-child(2) > div > div > div.dt-scroll > div.dt-scroll-head > div > table > thead > tr:nth-child(2) > th.dt-format-selector.dt-th-id > form > div > button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    Document.screenshotElement(DT.btn.recovery_button, "/redactor/webpages/recover-button.png");
    //I.fillField("#folderIdInputWrapper", "");
    Document.screenshotElement("#folderIdInputWrapper", "/redactor/webpages/recover-folder-id-1.png");
    I.click(DT.btn.recovery_button);

    I.waitForText(i18n.get("No matching records found"));


    I.moveCursorTo("div.toast-success");
    Document.screenshotElement("div.toast-success", "/redactor/webpages/recover-page-success.png");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");
    I.click( locate("a.jstree-anchor").withText("recoverSubFolderOne") );
    I.click(DT.btn.tree_delete_button);
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');

    switch (confLng) {
        case "sk":
            I.click('Zmazať');
            break;
        case "en":
            I.click('Delete');
            break;
        case "cs":
            I.click('Smazat');
            break;
        default:
            I.click('Unknown action');
    }

    DTE.waitForLoader();
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    I.fillField("#tree-folder-id", 59672);
    I.pressKey('Enter');
    DT.waitForLoader();
    Document.screenshotElement("#folderIdInputWrapper", "/redactor/webpages/recover-folder-id-2.png");
    I.click(DT.btn.tree_recovery_button);
    I.waitForElement("div.toast-info");

    switch (confLng) {
        case "sk":
            I.see("Ste si istý, že chcete obnoviť priečinok", "div.toast-title");
            break;
        case "en":
            I.see("Are you sure you want to recover this folder?", "div.toast-title");
            break;
        case "cs":
            I.see("Jste si jisti, že chcete adresář obnovit?", "div.toast-title");
            break;
        default:
            I.see("Unknown language message", "div.toast-title");
    }

    I.moveCursorTo("div.toast-title");
    Document.screenshotElement("div.toast-info", "/redactor/webpages/recover-folder-info.png");
    I.clickCss(".toastr-buttons button.btn-primary");
    I.waitForElement("div.toast-success");
    I.moveCursorTo("div.toast-title");
    Document.screenshotElement("div.toast-success", "/redactor/webpages/recover-folder-success.png");
});