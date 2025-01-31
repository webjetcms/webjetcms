Feature('apps.dmail.unsubscribed');

Before(({ I, login }) => {
    login('admin');
});

Scenario('unsubscribed-zakladne testy @baseTest', async ({I, DataTables}) => {

    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    await DataTables.baseTest({
        dataTable: 'unsubscribedDataTable',
        perms: 'menuEmail',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

function unsubscribeEmail(I, url, email) {
    I.amOnPage(url);
    I.fillField("div.unsubscribeForm input.emailField", email);
    I.amAcceptingPopups();
    I.click("div.unsubscribeForm .bSubmit");
}

function verifyUnsubscribed(I, DT, emailCorrect, emailNotFound) {
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    DT.filterContains("email", emailCorrect);
    DT.checkTableRow("unsubscribedDataTable", 1, [null, emailCorrect]);

    DT.filterContains("email", emailNotFound);
    I.see("Nenašli sa žiadne vyhovujúce záznamy", "#unsubscribedDataTable_wrapper");
}

function deleteUnsubscribed(I, DT, DTE, email) {
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    DT.filterContains("email", email);
    I.click("button.dt-filter-id");
    I.click("button.btn.btn-sm.buttons-selected.buttons-remove");
    DTE.waitForEditor("unsubscribedDataTable");
    I.click("Zmazať", "div.DTE_Action_Remove");
}

Scenario("Unsubscibed emails", ({I, DT, DTE, Document}) => {
    var random = I.getRandomText();
    var email1 = "autotest-unsub-webjet9-"+random+"@balat.sk";
    var email2 = "autotest-unsub-test23-"+random+"@balat.sk";

    unsubscribeEmail(I, "/newsletter/odhlasenie-z-newsletra.html", email1);
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    Document.switchDomain("test23.tau27.iway.sk");
    unsubscribeEmail(I, "/test23-newsletter/email-unsubscribe.html", email2);
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    Document.switchDomain("demotest.webjetcms.sk");

    I.amOnPage("/apps/dmail/admin/unsubscribed/");

    DT.filterContains("email", "test_domain_filter");
    DT.checkTableRow("unsubscribedDataTable", 1, ["81", "test_domain_filter_webjet9@test.sk"]);
    I.dontSee("test_domain_filter_test23@test.sk");

    verifyUnsubscribed(I, DT, email1, email2);

    //
    I.say("Change domain");
    Document.switchDomain("test23.tau27.iway.sk");
    DT.filterContains("email", "test_domain_filter");
    DT.checkTableRow("unsubscribedDataTable", 1, ["82", "test_domain_filter_test23@test.sk"]);
    I.dontSee("test_domain_filter_webjet9@test.sk");

    verifyUnsubscribed(I, DT, email2, email1);

    //
    I.say("Deleting emails");
    deleteUnsubscribed(I, DT, DTE, email2);
    Document.switchDomain("demotest.webjetcms.sk");
    deleteUnsubscribed(I, DT, DTE, email1);
});

Scenario('logout', async ({I}) => {
    I.logout();
});

Scenario("BUG - unsubscribe multiple - set domainId", ({I, DT, DTE}) => {
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    I.click("button.buttons-create");
    DTE.waitForEditor("unsubscribedDataTable");

    var randomText = I.getRandomText();
    var email = "autotest-unsub1-"+randomText+"@balat.sk, autotest-unsub2-"+randomText+"@balat.sk";
    DTE.fillField("email", email);
    DTE.save();
    I.dontSeeElement("div.DTE_Form_Error");
    I.dontSee("SQLIntegrityConstraintViolationException");
    I.dontSeeElement("#unsubscribedDataTable_modal");

    //
    I.say("Check emails");
    DT.filterContains("email", randomText+"@balat.sk");
    I.waitForText("Záznamy 1 až 2 z 2", 10, "div.dt-info");
    for (var e of email.split(", ")) {
        I.see(e, "#unsubscribedDataTable_wrapper");
    };

    //
    I.say("Deleting emails");
    I.click("button.buttons-select-all");
    I.waitForText("2 riadky označené", 10, "div.dt-info");
    I.click("button.buttons-remove");
    DTE.waitForEditor("unsubscribedDataTable");
    DTE.save();
});