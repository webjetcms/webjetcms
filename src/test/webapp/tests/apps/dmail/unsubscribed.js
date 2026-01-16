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

async function unsubscribeEmail(I, TempMail, url, email) {
    I.amOnPage(url);
    I.fillField("div.unsubscribeForm input.emailField", email);
    I.amAcceptingPopups();
    I.click("div.unsubscribeForm .bSubmit");

    I.waitForText("Bol Vám zaslaný e-mail, v ktorom prosím potvrďte Vašu voľbu.");

    await handleTempMailSubmission(I, TempMail, email);
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

async function handleTempMailSubmission(I, TempMail, email) {
    await TempMail.login(email);
    TempMail.openLatestEmail();
    I.waitForElement( TempMail.getContentSelector() + ' > p > a[href*=".html"]', 10);
    const url = await I.grabAttributeFrom( TempMail.getContentSelector() + ' > p > a[href*=".html"]', 'href');
    TempMail.deleteCurrentEmail();
    I.amOnPage(url.replace("https", "http"));
    I.waitForText("Email úspešne odhlásený.", 10);
}

Scenario("Unsubscibed emails", async ({I, DT, DTE, Document, TempMail}) => {
    var random = I.getRandomTextShort();
    //replace - with . to avoid issues with some email providers
    random = random.replace(/-/g, '.');
    var email1 = "autotest.demo."+random+TempMail.getTempMailDomain();
    var email2 = "autotest.test23."+random+TempMail.getTempMailDomain();

    await unsubscribeEmail(I, TempMail, "/newsletter/odhlasenie-z-newsletra.html", email1);
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    Document.switchDomain("test23.tau27.iway.sk");
    await unsubscribeEmail(I, TempMail, "/test23-newsletter/email-unsubscribe.html", email2);
    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    Document.switchDomain(I.getDefaultDomainName());

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
    Document.switchDomain(I.getDefaultDomainName());
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