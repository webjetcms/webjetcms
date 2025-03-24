Feature('apps.user.user-authorize');

var randomText = null;
var tempMailAddress = "webjetcms@fexpost.com";
let userName = "autotestApproveUser_";
let password;
let firstName = "firstName_";
let lastName = "lastName_";

Before(({ I }) => {
    if (randomText == null) {
        randomText = I.getRandomTextShort();

        userName += randomText;
        firstName += randomText;
        lastName += randomText;

        password = "Pas!23"+randomText;
    }
});

async function loginIN(I, password, correctPassword=false, checkText=true, userName="tester") {
    I.wait(1);
    I.fillField("username", userName);
    if (correctPassword) I.fillField("password", secret(password));
    else I.fillField("password", "tralala");
    I.clickCss(".login-submit");

    if (checkText) {
        if (correctPassword){
            let errorCount = await I.grabNumberOfVisibleElements('.error');
            if (errorCount > 0){
              let errorMessage = await I.grabTextFrom('.error');
              if (errorMessage.includes('Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované')) {
                I.wait(10.1);
                I.fillField("password", secret(password));
                I.clickCss(".login-submit");
                }
            }
            I.waitForText("tento text sa zobrazí len prihlásenému používateľovi.");
        }
        else I.dontSee("tento text sa zobrazí len prihlásenému používateľovi.");
    }
}

const RegistrationType = {
	One: "one", //no authetication needed, user is auto logged after registration
	Two: "two", //there is need auth, user must verify throu email
	Three: "three" //there is user group auth, admin must verify user
}

/********  CASE A   ******/
Scenario('Instant approval @singlethread', async ({ I, DT, DTE, TempMail }) => {
    //Prepare for scenario by deleting old users and emails
    TempMail.login("WebJetCMS");
    await TempMail.destroyInbox();
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);

    //Prepare regiter form
    await prepareRegistrationForm(I, DTE, RegistrationType.One);

    //Log off and go for logIn form
    I.amOnPage('/logoff.do');
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    //loginIN(I, password, correctPassword=false, checkText=true, userName="tester")

    //I am not registered YET
        await loginIN(I, password, false, true, userName);
    //Do a registration
        I.click(locate("#menu > li:nth-child(1) > a").withText("Registrácia"));
        register(I, false, RegistrationType.One);
    //Check we care loged
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        I.see("tento text sa zobrazí len prihlásenému používateľovi.");
    //Remove
        removeUser(I, DT);
    //Verify emmail
        await checkVerifyEmail(I, TempMail, RegistrationType.One);
});


/********  CASE B   ******/
Scenario('Email auth @singlethread', async ({ I, DT, DTE, TempMail }) => {
    //Prepare for scenario by deleting old users and emails
    TempMail.login("WebJetCMS");
    await TempMail.destroyInbox();
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);

    //Prepare regiter form
    await prepareRegistrationForm(I, DTE, RegistrationType.Two);

    //Log off and go for logIn form
    I.amOnPage('/logoff.do');
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    //I am not registered YET
    await loginIN(I, password, false, true, userName);
    //Do a registration
        I.click(locate("#menu > li:nth-child(1) > a").withText("Registrácia"));
        register(I, false, RegistrationType.Two);
    //Check we CANT log YET
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        I.wait(10);
        await loginIN(I, password, false, true, userName);

    //DO a verification
        let link = await checkVerifyEmail(I, TempMail, RegistrationType.Two, 1);
        I.amOnPage(link);
        I.see("Registračný formulár");
        I.see("Autorizácia používateľa úspešná. Teraz sa môžete prihlásiť.");
        I.dontSee("Používateľa sa nepodarilo autorizovať, skontrolujte prosím autorizačnú linku. Ak ste ju skopírovali z e-mailu, skontrolujte či je celá a či nie je zalomená na viac riadkov.");
    //Log in
        I.say("For some reason there must be little wait. Like user is not yet in DB verified sooo login will fail");
        I.wait(3);
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        //TODO error, user group is not set after verification
        await loginIN(I, password, true, true, userName);
    //Verify second mail
        await checkVerifyEmail(I, TempMail, RegistrationType.Two, 2);

    //Remove
        removeUser(I, DT);
});

/********  CASE C   ******/
Scenario('Admin auth @singlethread', async ({ I, DT, DTE, TempMail }) => {
    //Prepare for scenario by deleting old users and emails
    TempMail.login("WebJetCMS");
    await TempMail.destroyInbox();
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);

    //Prepare regiter form
    await prepareRegistrationForm(I, DTE, RegistrationType.Three);

    //Log off and go for logIn form
    I.amOnPage('/logoff.do');
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    //I am not registered YET
        await loginIN(I, password, false, true, userName);
    //Do a registration
        I.click(locate("#menu > li:nth-child(1) > a").withText("Registrácia"));
        register(I, false, RegistrationType.Three);
        I.amOnPage("/apps/prihlaseny-pouzivatel/registracia/");
        register(I, true, RegistrationType.Three);
    //Check we CANT log YET
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        I.wait(10);
        await loginIN(I, password, false, true, userName);

    //Approve user, with generated password in mail
        I.wait(15);

        I.relogin("admin");
        I.amOnPage("/admin/v9/users/user-list/");
        DT.filterContains("login", userName);
        DT.filterContains("firstName", firstName);
        DT.filterContains("lastName", lastName);
        I.see(userName);

        I.clickCss("td.dt-select-td.sorting_1");
        I.wait(1);
        I.clickCss("button.btn-auth-user-with-gen");

        I.wait(1);
        I.waitForElement("div.toast-message", 10);
        I.see("Autorizácia používateľa " + firstName + " " + lastName +  " (" + userName + "), bola úspešna.");
    //DO a verification
        let generatedPassword = await checkVerifyEmail(I, TempMail, RegistrationType.Three);
        // I.say(generatedPassword);
    //Do login
        I.amOnPage('/logoff.do');
        I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');
        await loginIN(I, generatedPassword, true, true, userName);
    //Delete user
        removeUser(I, DT);
});

Scenario('remove all old fexpost users @singlethread', async ({ I, DT, DTE }) => {
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);
});

Scenario('remove all old fexpost emails @singlethread', async ({ I, DT, TempMail }) => {
    TempMail.login("WebJetCMS");
    await TempMail.destroyInbox();
});

Scenario('delete cache objects to prevent logon form wrong password counting @singlethread', async ({ I }) => {
    I.amOnPage("/admin/v9/settings/cache-objects/");
    I.clickCss("button.btn-delete-all");
    I.waitForElement("div.toast-message");
    I.clickCss("div.toast-message button.btn-primary");
    I.closeOtherTabs();
});

Scenario('Test email sending after adding to userGroup @singlethread', async ({ I, DT, DTE, TempMail }) => {
    I.relogin("admin");

    let userName = "autotest_sendinguserGroupMails_" + randomText;

    I.amOnPage("/admin/v9/users/user-list/");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();

    //Just because we need to fill it
    I.fillField("#DTE_Field_firstName", userName);
    I.fillField("#DTE_Field_lastName", "kokos");
    I.fillField("#DTE_Field_editorFields-login", userName);
    I.fillField("#DTE_Field_email", tempMailAddress);
    I.fillField("#DTE_Field_password", "password" + randomText);

    //Allow sending mail and add user group
    I.clickCss("#pills-dt-datatableInit-groupsTab-tab");
    I.click("Redaktori", locate("#panel-body-dt-datatableInit-groupsTab label").withText("Redaktori"));
    I.checkOption("#DTE_Field_editorFields-sendAllUserGroupsEmails_0");

    DTE.save();

    //Open email - !! beware, can be false, depend is we call it already
    TempMail.login("WebJetCMS");
    await TempMail.openLatestEmail();
    I.see("Redaktori");
    I.see("Dobrý deň,");
    I.see("práve ste boli pridaný adminom do");
    I.see("skupiny Redaktori.");
});

Scenario('Remove left over emails @singlethread', async ({ TempMail }) => {
    TempMail.login("WebJetCMS");
    await TempMail.destroyInbox();
});

async function prepareRegistrationForm(I, DTE, registrationType) {
    //Info about prepared upcoming scenario
    let phrase = "";
    if(registrationType == RegistrationType.One) {
        phrase = "no authetication needed, user is auto logged after registration";
    } else if(registrationType == RegistrationType.Two) {
        phrase = "there is need auth, user must verify throu email";
    } else if(registrationType == RegistrationType.Three) {
        phrase = "there is user group auth, admin must verify user";
    }
    I.say("Preparing registration form for scenario where : " + phrase);


    I.relogin('admin');

    //
    await openRegisterForm(I, DTE);
    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.waitForElement("#editorComponent", 5);
    I.switchTo("#editorComponent");
    I.waitForElement("input[name=requireEmailVerification]", 5);

    if(registrationType == RegistrationType.One) {
        //NE Vyžadovať potvrdenie e-mailovej adresy
        I.uncheckOption("input[name=requireEmailVerification]");
    } else {
        //Vyžadovať potvrdenie e-mailovej adresy
        I.checkOption("input[name=requireEmailVerification]");
    }

    //Select wanted user groups
    if(registrationType == RegistrationType.One) await selectUserGroups(I, [529]);
    else if(registrationType == RegistrationType.Two) await selectUserGroups(I, [532]);
    else if(registrationType == RegistrationType.Three) await selectUserGroups(I, [2, 4, 5]);
    else if(registrationType == RegistrationType.Four) await selectUserGroups(I, []);

    //
    await save(I, DTE);
}

function removeUser(I, DT) {
    I.amOnPage('/logoff.do');
    I.wait(10);
    I.relogin("admin");
    I.amOnPage("/admin/v9/users/user-list/");
    DT.filterContains("login", userName);
    DT.filterContains("firstName", firstName);
    DT.filterContains("lastName", lastName);

    I.clickCss("td.dt-select-td.sorting_1");
    I.click(DT.btn.delete_button);
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(userName);
}

async function openRegisterForm(I, DTE) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=64542");
    DTE.waitForEditor();


    I.switchTo("iframe.cke_wysiwyg_frame");
    I.waitForElement("iframe.wj_component");
    I.switchTo("iframe.wj_component");
    I.waitForElement("div.inlineComponentButtons > a:nth-child(1)", 10);
    I.wait(1);
    I.clickCss("div.inlineComponentButtons > a:nth-child(1)");
    I.switchTo();

}
async function selectUserGroups(I, wantedGroupIds) {
    const userGroupsSelector = locate("#div_1 > form > div:nth-child(1) > div:nth-child(1)").withText("Používateľské skupiny:");

    let numOfElements = await I.grabNumberOfVisibleElements(userGroupsSelector.find("div > label > input"));
    I.say("numOfElements=" + numOfElements);
    for(let i = 1; i <= numOfElements; i++) {

        let inputLocator = userGroupsSelector.find("div > label > input").at(i);
        let userGroupId = await I.grabValueFrom(inputLocator);

        if(wantedGroupIds.includes(Number(userGroupId)))
            I.checkOption(inputLocator);
        else
            I.uncheckOption(inputLocator);
    }

}

async function save(I, DTE) {
    I.switchTo();
    I.clickCss("a.cke_dialog_ui_button.cke_dialog_ui_button_ok");
    I.wait(1);

    DTE.save();
}

function register(I, isEmailUsed, registrationType) {
    I.fillField("#usrLogin", userName);
    I.fillField("#usrPassword", password);
    I.fillField("#usrPassword2", password);
    I.fillField("#usrFirstName", firstName);
    I.fillField("#usrLastName", lastName);
    //Clear and set
    I.fillField("#usrEmail", "");
    I.fillField("#usrEmail", tempMailAddress);

    //Submit
    I.waitForElement('#bSubmitIdAjax');
    I.clickCss("#bSubmitIdAjax");
    I.wait(1);

    if(!isEmailUsed) {
        //
        if(registrationType == RegistrationType.One) I.waitForText("Registrácia úspešne uložená. Boli ste automaticky prihlásený, môžete pristupovať do privátných zón web sídla.", 10);
        else if(registrationType == RegistrationType.Two) I.waitForText("Na vašu e-mailovú adresu bol zaslaný e-mail v ktorom musíte kliknutím na autorizačnú linku potvrdiť vašu registráciu.",10);
        else if(registrationType == RegistrationType.Three) I.waitForText("Vaša registrácia podlieha schváleniu, po schválení registrácie dostanete e-mailovú správu s prihlasovacími údajmi.", 10);

        I.dontSee("Zadaný e-mail je už použitý, ak neviete prístupové heslo, môžete si ho nechať poslať");
    } else {
        //
        I.dontSee("Registrácia úspešne uložená. Boli ste automaticky prihlásený, môžete pristupovať do privátných zón web sídla.");
        I.dontSee("Na vašu e-mailovú adresu bol zaslaný e-mail v ktorom musíte kliknutím na autorizačnú linku potvrdiť vašu registráciu.")
        I.dontSee("Vaša registrácia podlieha schváleniu, po schválení registrácie dostanete e-mailovú správu s prihlasovacími údajmi.");

        I.see("Zadaný e-mail je už použitý, ak neviete prístupové heslo, môžete si ho nechať poslať");
    }
}

async function checkVerifyEmail(I, TempMail, registrationType, phase=null) {

    //Open email
    TempMail.login("WebJetCMS");
    await TempMail.openLatestEmail();

    if(registrationType == RegistrationType.One) {
        checkVerifyEmail_One(I, TempMail);
    } else if(registrationType == RegistrationType.Two) {
        if(phase == null) return;
        else if(phase == 1) return checkVerifyEmail_Two_Phase_1(I, TempMail);
        else if(phase == 2) return checkVerifyEmail_Two_Phase_2(I, TempMail);
    }
    else if(registrationType == RegistrationType.Three) {
        return checkVerifyEmail_Three(I, TempMail);
    }
}

async function checkVerifyEmail_One(I, TempMail) {
    //Vefiry mail body context
    I.see("Prihlasovacie údaje na");
    I.see("Ďakujeme za registráciu na našom web sídle.");
    I.see("Pre prihlásenie použite nasledovné údaje:");
    I.see("Prihlasovacie meno: " + userName);
    I.see("Heslo: " + password);

    //Remove email
    TempMail.closeEmail();
    await TempMail.destroyInbox();
}

async function checkVerifyEmail_Two_Phase_1(I, TempMail) {
    let authLink = "";

    I.switchTo();

    //Vefiry mail body context
    I.see("Potvrdenie registrácie na serveri");
    I.see("Ďakujeme za registráciu na stránke");
    I.see("Váš účet je vytvorený a musí byť aktivovaný pred prvým použitím.");

    let text = await I.grabTextFrom("#info > div.overflow-auto");
    if(text.includes("Pre aktiváciu účtu kliknite na nasledovný odkaz:")) {
        authLink = ( text.split("Pre aktiváciu účtu kliknite na nasledovný odkaz:")[1] ).split(" ").join("");

        if(authLink === undefined || authLink === null || authLink.length < 1) {
            //Problem .... link is empty
            I.say("ERROR");
            I.assertEqual("", "Auth-link parse failed");
        }
    } else {
        //Problem .... there is no auth-link in email
        I.say("ERROR");
        I.assertEqual("", "There is no auth-link");
    }

    //Remove email
    TempMail.closeEmail();
    await TempMail.destroyInbox();

    return authLink;
}

async function checkVerifyEmail_Two_Phase_2(I, TempMail) {
    //Vefiry mail body context
    I.see("Prihlasovacie údaje na");
    I.see("Ďakujeme za registráciu na našom web sídle.");
    I.see("Pre prihlásenie použite nasledovné údaje:");
    I.see("Prihlasovacie meno: " + userName);

    //Remove email
    TempMail.closeEmail();
    await TempMail.destroyInbox();
}

async function checkVerifyEmail_Three(I, TempMail) {
    let generatedPasswd = "";

    //Vefiry mail body context
    I.see("Prihlasovacie údaje na");
    I.see("Ďakujeme za registráciu na našom web sídle.");
    I.see("Pre prihlásenie použite nasledovné údaje:");
    I.see("Prihlasovacie meno: " + userName);

    let text = await I.grabTextFrom("#info > div.overflow-auto");
    if(text.includes("Heslo:")) {
        generatedPasswd = ( text.split("Heslo:")[1] ).split("http")[0];
        //remove whitespaces
        generatedPasswd = generatedPasswd.split(" ").join("");

        if(generatedPasswd === undefined || generatedPasswd === null || generatedPasswd.length != 8) {
            //Problem .... there must be 8 length passwd
            I.say("ERROR");
            I.assertEqual("", "Password parse failed");
        }
    } else {
        //Problem .... there is no password in email
        I.say("ERROR");
        I.assertEqual("", "There is no passwd");
    }

    //Remove email
    TempMail.closeEmail();
    await TempMail.destroyInbox();

    return generatedPasswd;
}

/**
 * Remove all users with set email.
 * Need to remove them in case that email in login must be uniq and this user's are here maybe from failed test.
 *
 * @param {*} I
 * @param {*} DT
 * @param {String} emailAddress
 */
async function removeFexpostUsers(I, DT, DTE, emailAddress) {
    I.say("Removing all users with email address : " + emailAddress);

    I.relogin('admin');
    I.amOnPage("/admin/v9/users/user-list/");

    //Find users with that email
    DT.filterContains("email", emailAddress);

    //Find gow many rows in DB we have (have many users have this email)
    let numberOfUsers = await I.getTotalRows();

    //If there is more that 0, delete them all
    if(numberOfUsers > 0) {
        I.clickCss("button.buttons-select-all");
        I.click(DT.btn.delete_button);
        DTE.waitForEditor();
        //bug wrong filter, delete all users on first page
        I.dontSee("vipklient", "div.DTE_Action_Remove");

        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader();
    }

    //Check, we cant see that email
    I.dontSee(emailAddress);
}

