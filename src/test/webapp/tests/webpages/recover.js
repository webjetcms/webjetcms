Feature('webpages.recover');

let recoverBtnWebPage = (locate('#datatableInit_wrapper').find("button.btn.btn-sm.btn-outline-secondary.button-recover-page"));
let recoverBtnFolder = (locate('.col-md-4.tree-col').find("button.btn.btn-sm.buttons-selected.btn-outline-secondary.button-recover-group"));

let delete_webpage_button = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger'));
let delete_folder_button = (locate('.col-md-4.tree-col').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger'));

let edit_folder_button = (locate('.col-md-4.tree-col').find('.btn.btn-sm.buttons-selected.buttons-edit.noperms-editDir.btn-warning'));

Before(({ I }) => {
    I.relogin("admin");
});


Scenario('Recovery buttons visibility logic', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");
    DT.waitForLoader();

    I.say("We are not in trash tab, dont see recovery buttons");
    I.dontSeeElement(recoverBtnWebPage);
    I.dontSeeElement(recoverBtnFolder);

    I.say("IN trash tab see recovery buttons");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    I.waitForElement(recoverBtnFolder, 10);
    I.seeElement(recoverBtnWebPage);
    I.wait(1);

    I.say("Recovery web page is allowed only in pages tab");
    I.clickCss("#pills-changes-tab");
    DT.waitForLoader();
    I.waitForInvisible(recoverBtnWebPage);

    I.clickCss("#pills-folders-tab");
    DT.waitForLoader();
    I.dontSeeElement(recoverBtnWebPage);
    I.dontSeeElement(recoverBtnFolder);
});

Scenario('Recovery web page logic', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");

    I.say("Deleting web page");
    DT.filter("title", "page_to_delete");
    I.wait(1);
    I.clickCss("#datatableInit_wrapper button.buttons-select-all");
    I.click(delete_webpage_button);

    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.see("page_to_delete", ".DTE.modal-content.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Recovering web page");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();

    //
    I.say("Test recover of unrecoverable page");
    DT.filter("title", "Test zmazania stránky");
    I.see("Test zmazania stránky", "#datatableInit_wrapper .dataTables_scrollBody");
    I.clickCss("#datatableInit_wrapper button.buttons-select-all");
    I.click(recoverBtnWebPage);
    I.waitForText("Stránku sa nepodarilo obnoviť", 10, "div.toast-title");
    I.toastrClose();

    //
    I.say("Test recover of recoverable page");
    DT.filter("title", "page_to_delete");
    I.see("page_to_delete");
    I.clickCss("#datatableInit_wrapper button.buttons-select-all");
    I.click(recoverBtnWebPage);
    I.waitForText("Stránka bola úspešne obnovená", 10, "div.toast-title");
    I.see("bola úspešne obnovená do priečinka:", "div.toast-message");
    I.see("/Test stavov/page_folder_recovery/page_to_delete", "div.toast-message");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10, "#datatableInit_wrapper .dataTables_scrollBody");

    //
    I.say("Check recovered page");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");
    DT.filter("title", "page_to_delete");
    I.waitForText("page_to_delete", 10, "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Recovery folder logic', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59672");

    DT.waitForLoader();
    //verigy we are not in trash tab
    I.seeElement("#pills-folders-tab.active");
    I.dontSeeElement("#pills-trash-tab.active");

    I.say("Deleting folder");
    I.click(delete_folder_button);
    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.see("recoverSubFolderOne", ".DTE.modal-content.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();

    I.say("Recovering folder");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    I.fillField("#tree-folder-id", 59672);
    I.pressKey('Enter');
    DT.waitForLoader();
    I.wait(1);
    I.click(recoverBtnFolder);
    I.waitForElement("div.toast-info");
    I.see("Ste si istý, že chete obnoviť priečinok", "div.toast-title");
    I.see("recoverSubFolderOne", "div.toast-message");
    I.clickCss(".toastr-buttons button.btn-primary");
    DTE.waitForLoader();

    //
    I.say("Check recover notification");
    I.waitForText("Priečinok bol úspešne obnovený", 10, "div.toast-title");
    I.see("Priečinok recoverSubFolderOne bol úspešne obnovený do", "div.toast-message");
    I.see("/Test stavov/page_folder_recovery", "div.toast-message");
    I.toastrClose();

    //
    I.say("Check folder position");
    I.click("#pills-folders-tab");
    I.jstreeNavigate(["Test stavov", "page_folder_recovery", "recoverSubFolderOne"]);
    I.click(edit_folder_button);
    DTE.waitForLoader();
    I.seeInField('#editorAppDTE_Field_editorFields-parentGroupDetails input.form-control', '/Test stavov/page_folder_recovery', 10);
    DTE.cancel();

    I.say("Check recover pages's available stauts");
    checkAvailable(I, DTE, "recoverSubFolderOne_notAvailable", false);
    checkAvailable(I, DTE, "recoverSubFolderOne_notActualHistory", false);
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59673");
    checkAvailable(I, DTE, "recoverSubFolderTwo_noHistory", true);

});

function checkAvailable(I, DTE, pageName, available) {
    I.say("Check available status of page: " + pageName);
    I.click(pageName);
    DTE.waitForLoader();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.waitForElement("#DTE_Field_available_0")
    if (available) {
        I.seeCheckboxIsChecked("#DTE_Field_available_0");
    } else {
        I.dontSeeCheckboxIsChecked("#DTE_Field_available_0");
    }
    DTE.cancel();
}