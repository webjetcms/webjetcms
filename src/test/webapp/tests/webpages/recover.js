Feature('webpages.recover');


var randomNumber;

Before(({ I }) => {
    I.relogin("admin");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Recovery doc button visibility logic', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();

    I.say("We are not in trash tab, dont see recovery button");
    I.dontSeeElement(DT.btn.recovery_button);

    I.say("IN trash tab see recovery buttons - with confition");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();

    I.say("Doc recovery button visible if NO deleted groups are selected");
    I.fillField("#tree-folder-id", "");
    I.pressKey("Enter");
    DT.waitForLoader();
    I.waitForElement(DT.btn.recovery_button, 10);

    I.say("Recovery web page is allowed only in pages tab");
    I.clickCss("#pills-changes-tab");
    DT.waitForLoader();
    I.waitForInvisible(DT.btn.recovery_button);

    I.clickCss("#pills-folders-tab");
    DT.waitForLoader();
    I.dontSeeElement(DT.btn.recovery_button);
});

Scenario('Recovery tree buttons visibility logic', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();

    I.say("We are not in trash tab, dont see recovery buttons");
    I.dontSeeElement(DT.btn.tree_recovery_button);

    I.say("IN trash tab see recovery buttons - with confition");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();

    I.say("Tree recovery button visible ONLY IF deleted group is selected");
    I.fillField("#tree-folder-id", 81986);
    I.pressKey("Enter");
    DT.waitForLoader();
    I.waitForElement(DT.btn.tree_recovery_button, 10);

    I.clickCss("#pills-folders-tab");
    DT.waitForLoader();
    I.dontSeeElement(DT.btn.tree_recovery_button);
});

Scenario('Recovery web page logic', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");

    I.say("Deleting web page");
    DT.filterContains("title", "page_to_delete");
    I.wait(1);
    I.clickCss("#datatableInit_wrapper button.buttons-select-all");
    I.click(DT.btn.delete_button);

    I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
    I.see("page_to_delete", ".DTE.modal-content.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy", "#datatableInit_wrapper .dt-scroll-body");

    I.say("Recovering web page");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();

    //
    I.say("Test recover of unrecoverable page");
    DT.filterContains("title", "Test zmazania stránky");
    I.see("Test zmazania stránky", "#datatableInit_wrapper .dt-scroll-body");
    I.clickCss("#datatableInit_wrapper button.buttons-select-all");
    I.click(DT.btn.recovery_button);
    I.waitForText("Stránku sa nepodarilo obnoviť", 10, "div.toast-title");
    I.toastrClose();

    //
    I.say("Test recover of recoverable page");
    DT.filterContains("title", "page_to_delete");
    I.see("page_to_delete");
    I.clickCss("#datatableInit_wrapper button.buttons-select-all");
    I.click(DT.btn.recovery_button);
    I.waitForText("Stránka bola úspešne obnovená", 10, "div.toast-title");
    I.see("bola úspešne obnovená do priečinka:", "div.toast-message");
    I.see("/Test stavov/page_folder_recovery/page_to_delete", "div.toast-message");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 15, "#datatableInit_wrapper .dt-scroll-body");

    //
    I.say("Check recovered page");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59609");
    DT.filterContains("title", "page_to_delete");
    I.waitForText("page_to_delete", 10, "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Before - Recovery folder logic', async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59672");

    I.say("Check if folder is deleted - if YES, recover it");
    const count = await I.grabNumberOfVisibleElements("#pills-trash-tab.active");
    if(count > 0) {
        I.click(DT.btn.tree_recovery_button);
        I.waitForElement("div.toast-info");
        I.see("Ste si istý, že chete obnoviť priečinok", "div.toast-title");
        I.see("recoverSubFolderOne", "div.toast-message");
        I.clickCss(".toastr-buttons button.btn-primary");
        DTE.waitForLoader();
    }
});

Scenario('Recovery folder logic', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59672");

    DT.waitForLoader();
    I.say("verify we are not in trash tab");
    I.seeElement("#pills-folders-tab.active");
    I.dontSeeElement("#pills-trash-tab.active");

    I.say("Deleting folder");
    I.click(DT.btn.tree_delete_button);
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
    I.click(DT.btn.tree_recovery_button);
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
    DT.waitForLoader();

    //
    I.say("Check folder position");
    I.clickCss("#pills-folders-tab");
    DT.waitForLoader();
    I.jstreeNavigate(["Test stavov", "page_folder_recovery", "recoverSubFolderOne"]);
    I.click(DT.btn.tree_edit_button);
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
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.waitForElement("#DTE_Field_available_0")
    if (available) {
        I.seeCheckboxIsChecked("#DTE_Field_available_0");
    } else {
        I.dontSeeCheckboxIsChecked("#DTE_Field_available_0");
    }
    DTE.cancel();
}

const skDocRecoverGroupId = 113666;
const enDocRecoverGroupId = 113667;
Scenario('Before - mirrored recovery logic', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    Document.switchDomain("mirroring.tau27.iway.sk");

    Document.setConfigValue("structureMirroringConfig", skDocRecoverGroupId + "," + enDocRecoverGroupId + ":mirroring.tau27.iway.sk");

    DT.filterContains("name", "structureMirroringConfig");
    I.see(skDocRecoverGroupId + "," + enDocRecoverGroupId + ":mirroring.tau27.iway.sk");
});

const skDocRecoverGroup = "sk_doc_recover";
const enDocRecoverGroup = "en_doc_recover";
Scenario('Recovery of mirrored doc logic', ({ I, DT, DTE }) => {
    // STROM - TREE auto translate
    const skRecoverPage = "strom_page_to_recover_" + randomNumber;
    const enRecoverPage = "tree_page_to_recover_" + randomNumber;

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + skDocRecoverGroupId);
    DT.waitForLoader();

    // SOME problems occur when we works with NEW pages, so do create
    I.say("First create page");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.fillField("#DTE_Field_title", skRecoverPage);
    DTE.save();

    I.say("I remove one page, both pages are removed");
    DT.filterEquals("title", skRecoverPage);
    I.see(skRecoverPage, "#datatableInit_wrapper");
    I.clickCss("#datatableInit_wrapper td.dt-select-td");
    I.click(DT.btn.delete_button);
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();

    I.say('Check that both pages are removed');
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    DT.filterContainsForce("title", "_page_to_recover_" + randomNumber);
    I.see(skRecoverPage, "#datatableInit_wrapper");
    I.see(enRecoverPage, "#datatableInit_wrapper");

    I.say("Choose EN version and do recover");
    DT.filterEquals("title", enRecoverPage);
    I.clickCss("#datatableInit_wrapper td.dt-select-td");
    I.click(DT.btn.recovery_button);
    DT.waitForLoader();

    I.say("Check that there are 2 notification about recovery - SK and EN page");

    within("#toast-container-webjet div.toast.toast-success:nth-child(1)", () => {
        I.waitForText("Stránka bola úspešne obnovená", 10, "div.toast-title");
        I.see("bola úspešne obnovená do priečinka:", "div.toast-message");
        I.see("/" + enDocRecoverGroup + "/" + enRecoverPage, "div.toast-message");
    });

    within("#toast-container-webjet div.toast.toast-success:nth-child(2)", () => {
        I.waitForText("Stránka bola úspešne obnovená", 10, "div.toast-title");
        I.see("bola úspešne obnovená do priečinka:", "div.toast-message");
        I.see("/" + skDocRecoverGroup + "/" + skRecoverPage, "div.toast-message");
    });

    I.say('Check that they are not in trash anymore');
    DT.filterContainsForce("title", "_page_to_recover_" + randomNumber);
    I.dontSee(skRecoverPage, "#datatableInit_wrapper");
    I.dontSee(enRecoverPage, "#datatableInit_wrapper");

    I.say("Now check that both were recovered");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + skDocRecoverGroupId);
    DT.waitForLoader();
    DT.filterEquals("title", skRecoverPage);
    I.see(skRecoverPage, "#datatableInit_wrapper");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + enDocRecoverGroupId);
    DT.waitForLoader();
    DT.filterEquals("title", enRecoverPage);
    I.see(enRecoverPage, "#datatableInit_wrapper");
});

Scenario('Recovery of mirrored groups logic', ({ I, DT, DTE }) => {
    I.say("I remove one group, both groups are removed");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + enDocRecoverGroupId);
    DT.waitForLoader();
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.dontSeeElement(locate("a.jstree-anchor").withText(enDocRecoverGroup));

    I.say('Check that both groups are removed');
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    I.jstreeFilter(enDocRecoverGroup);
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(enDocRecoverGroup));
    I.jstreeFilter(skDocRecoverGroup);
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(skDocRecoverGroup));

    I.say("Choose SK version and do recover");
    I.click(locate('.jstree-anchor.jstree-search').withText(skDocRecoverGroup));
    I.click(DT.btn.tree_recovery_button);
    I.waitForElement("div.toast-info");
    I.clickCss(".toastr-buttons button.btn-primary");
    DTE.waitForLoader();

    I.say("Check that there are 2 notification about recovery - SK and EN page");

    within("#toast-container-webjet div.toast.toast-success:nth-child(1)", () => {
        I.waitForText("Priečinok bol úspešne obnovený", 10, "div.toast-title");
        I.see("Priečinok " + enDocRecoverGroup + " bol úspešne obnovený do", "div.toast-message");
        I.see("Koreňový priečinok", "div.toast-message");
    });

    within("#toast-container-webjet div.toast.toast-success:nth-child(2)", () => {
        I.waitForText("Priečinok bol úspešne obnovený", 10, "div.toast-title");
        I.see("Priečinok " + skDocRecoverGroup + " bol úspešne obnovený do", "div.toast-message");
        I.see("Koreňový priečinok", "div.toast-message");
    });
});

Scenario('Logout', ({ I }) => {
    //Logout to refresh set domain
    I.logout();
});
