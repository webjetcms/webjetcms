Feature('apps.dmail.subscribe');

let randomNumber;
let userGroupName = "Dmail Testeri";

Before(({ I , login, DT}) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
    login("admin");
    DT.addContext("users" ,"#userGroupsDataTable_wrapper");
});

Scenario('Delete users', async ({ I, DT }) => {
    await deleteDmailUsers(I, DT);
});

Scenario('Dmail', async ({ I, Document, DT, DTE, TempMail }) => {
    const userName = `autotest-name-${randomNumber}`;
    const userSurname = `autotest-surname-${randomNumber}`;
    const userEmail = "webjetcmsdmail@fexpost.com";

    I.say("Disabling CAPTCHA for testing");
    Document.setConfigValue("captchaComponents", "send_link");

    I.say("Navigating to newsletter registration page");
    I.amOnPage("/newsletter/registracia-do-newsletra.html");
    I.fillField("#meno", userName);
    I.fillField("#priez", userSurname);
    I.fillField("#mail", userEmail);
    I.click(locate('label').withText(userGroupName));
    I.clickCss("input[name=bSubmit]");

    I.say("Checking confirmation email");
    await TempMail.loginAsync("webjetcmsdmail");
    TempMail.openLatestEmail();
    I.waitForText("Potvrdenie odberu newslettra", 10);
    I.see(userName);
    I.see(userSurname);
    I.click("#info > div > p > a");
    I.wait(2);
    I.switchToNextTab();

    I.say("Verifying user in subscribers");
    openSubscribers(I, DT, DTE);
    DT.checkTableRow("datatableFieldDTE_Field_usersList_wrapper", 1, ["", "", userName, userSurname, userEmail]);
});

Scenario('Revert - dmail', async ({ I, Document, DT}) => {
    const userName = `autotest-name-${randomNumber}`;

    I.say("Restoring CAPTCHA configuration");
    Document.setConfigValue("captchaComponents", "send_link,dmail");

    removeGroupUsers(I, DT);

    I.say("Deleting autotest user from users");
    await deleteDmailUsers(I, DT);
    I.closeOtherTabs();
});

Scenario('Dmail simple', async ({ I, TempMail, DT, DTE }) => {
    const userEmail = "webjetcmsdmailsimple@fexpost.com";

    I.say("Navigating to simple newsletter registration page");
    I.amOnPage("/newsletter/registracia-do-newslettera-simple.html");
    I.waitForElement("#subscribeForm input[name='email']");
    I.fillField("#subscribeForm input[name='email']", userEmail);
    I.wait(3);

    I.say("Submitting the subscription form");
    I.click(locate("button[type=submit]").withText("Odoslať"));
    I.wait(2);
    I.waitForElement(locate(".modal-content > .modal-header > .modal-title").withText("Newsletter"), 10);
    I.click(locate("button").withText("OK"));

    I.amOnPage("/admin/v9/users/user-list/");
    DT.filterContains("email", "webjetcmsdmail");
    I.dontSee('Nenašli sa žiadne vyhovujúce záznamy');
    let lineColor = await getFontColor(I, 1,1);
    I.assertEqual(lineColor, "rgb(255, 75, 88)");

    I.say("Checking confirmation email");
    await TempMail.loginAsync("webjetcmsdmailsimple");
    TempMail.openLatestEmail();
    I.waitForText("Potvrdenie odberu newslettra", 10);
    I.wait(2);
    I.click("#info > div > p > a");

    I.switchToNextTab();
    I.waitForElement(locate(".modal-content > .modal-header > .modal-title").withText("Newsletter"), 10);
    I.waitForElement(locate("#subscribeSimpleModal > div > div > div.modal-body").withText("Vaša voľba je autorizovaná"), 10);

    I.say("Verifying user in subscribers");
    I.closeOtherTabs();
    openSubscribers(I, DT, DTE);
    DT.checkTableRow("datatableFieldDTE_Field_usersList_wrapper", 1, ["", "", "webjetcmsdmailsimple", "@fexpost.com", "webjetcmsdmailsimple@fexpost.com"]);
});

Scenario('Revert - dmail simple', async ({ I, DT}) => {
    removeGroupUsers(I, DT);
    await deleteDmailUsers(I, DT);
});

async function deleteDmailUsers(I, DT) {
    I.amOnPage("/admin/v9/users/user-list/");
    DT.filterContains("email", "webjetcmsdmail");
    if (await I.grabNumberOfVisibleElements(".dt-empty") === 0) {
        DT.deleteAll();
    }
}

function removeGroupUsers(I, DT) {
    I.say("Removing users from "+userGroupName+" group");
    I.amOnPage("/admin/v9/users/user-groups/");
    DT.filterContains("userGroupName", userGroupName);
    I.clickCss("button.buttons-select-all");
    I.click(DT.btn.users_remove_group_button);
    I.waitForElement(locate(".toast-title").withText("Naozaj chcete odobrať všetkým túto skupinu?"), 10);
    I.click(locate(".toastr-buttons > button").withText("Potvrdiť"));
    I.waitForInvisible("div.dt-processing", 100);
}

function openSubscribers(I, DT, DTE) {
    I.say("Opening user group subscribers");
    I.amOnPage("/admin/v9/users/user-groups/");
    DT.filterContains("userGroupName", userGroupName);
    I.clickCss("button.buttons-select-all");
    I.click(DT.btn.users_edit_button);
    DTE.waitForEditor("userGroupsDataTable");
    I.clickCss("#pills-dt-userGroupsDataTable-users-tab");
}

async function getFontColor(I, row, col){
    const elementSelector = locate("#datatableInit_wrapper tbody tr:nth-child("+row+") td:nth-child("+col+")");
    return await I.grabCssPropertyFrom(elementSelector, 'color');
}
