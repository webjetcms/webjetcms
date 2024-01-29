Feature('apps.user-authorize');

var randomText = null;
var tempMailAddress = "webjetcms@fexpost.com";
var tempMailPin = "264583092304377";
let userName = "autotestApproveUser_";
let password;
let firstName = "firstName_";
let lastName = "lastName_";

var defaultPassword = "*********";

Before(({ I }) => {
    if (randomText == null) {
        randomText = I.getRandomTextShort();

        userName += randomText;
        firstName += randomText;
        lastName += randomText;

        password = "Pas!23"+randomText;
    }
});

function loginIN(I, password, correctPassword=false, checkText=true, userName="tester") {
    I.wait(1);
    I.fillField("username", userName);
    if (correctPassword) I.fillField("password", secret(password));
    else I.fillField("password", "tralala");
    I.clickCss(".login-submit");

    if (checkText) {
        if (correctPassword) I.see("tento text sa zobrazí len prihlásenému používateľovi.");
        else I.dontSee("tento text sa zobrazí len prihlásenému používateľovi.");
    }
}

const RegistrationType = {
	One: "one", //no authetication needed, user is auto logged after registration
	Two: "two", //there is need auth, user must verify throu email
	Three: "three" //there is user group auth, admin must verify user
}

/********  CASE A   ******/
Scenario('Instant approval @singlethread', async ({ I, DT, DTE }) => {
    //Prepare for scenario by deleting old users and emails
    await deleteAllStroredEmails(I);
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);

    //Prepare regiter form
    await prepareRegistrationForm(I, DTE, RegistrationType.One);

    //Log off and go for logIn form
    I.amOnPage('/logoff.do');
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    //loginIN(I, password, correctPassword=false, checkText=true, userName="tester")

    //I am not registered YET
        loginIN(I, password, false, true, userName);
    //Do a registration
        I.click(locate("#menu > li:nth-child(1) > a").withText("Registrácia"));
        register(I, false, RegistrationType.One);
    //Check we care loged
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        I.see("tento text sa zobrazí len prihlásenému používateľovi.");
    //Remove
        removeUser(I, DT);
    //Verify emmail
        await checkVerifyEmail(I, RegistrationType.One);
});


/********  CASE B   ******/
Scenario('Email auth @singlethread', async ({ I, DT, DTE }) => {
    //Prepare for scenario by deleting old users and emails
    await deleteAllStroredEmails(I);
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);

    //Prepare regiter form
    await prepareRegistrationForm(I, DTE, RegistrationType.Two);

    //Log off and go for logIn form
    I.amOnPage('/logoff.do');
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    //I am not registered YET
        loginIN(I, password, false, true, userName);
    //Do a registration
        I.click(locate("#menu > li:nth-child(1) > a").withText("Registrácia"));
        register(I, false, RegistrationType.Two);
    //Check we CANT log YET
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        I.wait(10);
        loginIN(I, password, false, true, userName);

    //DO a verification
        let link = await checkVerifyEmail(I, RegistrationType.Two, 1);
        I.amOnPage(link);
        I.see("Registračný formulár");
        I.see("Autorizácia používateľa úspešná. Teraz sa môžete prihlásiť.");
        I.dontSee("Používateľa sa nepodarilo autorizovať, skontrolujte prosím autorizačnú linku. Ak ste ju skopírovali z e-mailu, skontrolujte či je celá a či nie je zalomená na viac riadkov.");
    //Log in
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        //TODO error, user group is not set after verification
        loginIN(I, password, true, true, userName);
    //Verify second mail
        await checkVerifyEmail(I, RegistrationType.Two, 2);

    //Remove
        removeUser(I, DT);
});

/********  CASE C   ******/
Scenario('Admin auth @singlethread', async ({ I, DT, DTE }) => {
    //Prepare for scenario by deleting old users and emails
    await deleteAllStroredEmails(I);
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);

    //Prepare regiter form
    await prepareRegistrationForm(I, DTE, RegistrationType.Three);

    //Log off and go for logIn form
    I.amOnPage('/logoff.do');
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    //I am not registered YET
        loginIN(I, password, false, true, userName);
    //Do a registration
        I.click(locate("#menu > li:nth-child(1) > a").withText("Registrácia"));
        register(I, false, RegistrationType.Three);
        I.amOnPage("/apps/prihlaseny-pouzivatel/registracia/");
        register(I, true, RegistrationType.Three);
    //Check we CANT log YET
        I.click(locate("#menu > li.protected > a").withText("Zákaznícka zóna"));
        I.wait(10);
        loginIN(I, password, false, true, userName);

    //Approve user, with generated password in mail
        I.wait(15);

        I.relogin("admin");
        I.amOnPage("/admin/v9/users/user-list/");
        DT.filter("login", userName);
        DT.filter("firstName", firstName);
        DT.filter("lastName", lastName);
        I.see(userName);

        I.clickCss("td.dt-select-td.sorting_1");
        I.wait(1);
        I.clickCss("button.btn-auth-user-with-gen");

        I.wait(1);
        I.waitForElement("div.toast-message", 10);
        I.see("Autorizácia používateľa " + firstName + " " + lastName +  " (" + userName + "), bola úspešna.");
    //DO a verification
        let generatedPassword = await checkVerifyEmail(I, RegistrationType.Three);
        // I.say(generatedPassword);
    //Do login
        I.amOnPage('/logoff.do');
        I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');
        loginIN(I, generatedPassword, true, true, userName);
    //Delete user
        removeUser(I, DT);
});

Scenario('remove all old fexpost users @singlethread', async ({ I, DT, DTE }) => {
    await removeFexpostUsers(I, DT, DTE, tempMailAddress);
});

Scenario('remove all old fexpost emails @singlethread', async ({ I, DT }) => {
    await deleteAllStroredEmails(I);
});

Scenario('delete cache objects to prevent logon form wrong password counting @singlethread', async ({ I }) => {
    I.amOnPage("/admin/v9/settings/cache-objects/");
    I.clickCss("button.btn-delete-all");
    I.waitForElement("div.toast-message");
    I.clickCss("div.toast-message button.btn-primary");
    I.closeOtherTabs();
});

Scenario('Test email sending after adding to userGroup @singlethread', async ({ I, DT, DTE }) => {
    I.relogin("admin");

    let userName = "autotest_sendinguserGroupMails_" + randomText;

    I.amOnPage("/admin/v9/users/user-list/");
    I.clickCss("button.buttons-create");
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

    //Open email - !! beware, can be false, depend is we call it allready
    await openTempEmail(I, true);
    I.see("Redaktori");
    I.see("Dobrý deň,");
    I.see("práve ste boli pridaný adminom do");
    I.see("skupiny Redaktori.");
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

    if(registrationType == RegistrationType.One) {
        //Vyžadovať potvrdenie e-mailovej adresy
        I.uncheckOption("#div_1 > form > div:nth-child(6) > div > span > input");
    } else {
        //Vyžadovať potvrdenie e-mailovej adresy
        I.checkOption("#div_1 > form > div:nth-child(6) > div > span > input");
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
    DT.filter("login", userName);
    DT.filter("firstName", firstName);
    DT.filter("lastName", lastName);

    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee(userName);
}

async function openRegisterForm(I, DTE) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=64542");
    DTE.waitForEditor();


    I.switchTo("iframe.cke_wysiwyg_frame");
    I.waitForElement("iframe.wj_component");
    I.switchTo("iframe.wj_component");
    I.seeElement("div.inlineComponentButtons > a:nth-child(1)");
    I.wait(2);
    I.clickCss("div.inlineComponentButtons > a:nth-child(1)");
    I.switchTo();

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");
    I.switchTo("#tabMenu1");
}
async function selectUserGroups(I, wantedGroupIds) {
    I.switchTo(locate("#div_1 > form > div:nth-child(1) > div:nth-child(1)").withText("Používateľské skupiny:"));

    let numOfElements = await I.grabNumberOfVisibleElements("label");
    let index = 0;
    for(let i = 1; i <= numOfElements; i++) {

        if(i == 1) index = 1;
        else index += 2;

        let userGroupId = await I.grabValueFrom("div > label:nth-child(" + index + ") > div > span > input");

        if(wantedGroupIds.includes(Number(userGroupId)))
            I.checkOption("div > label:nth-child(" + index + ") > div");
        else
            I.uncheckOption("div > label:nth-child(" + index + ") > div");
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
    I.clickCss("#bSubmitIdAjax");
    I.wait(1);

    if(!isEmailUsed) {
        //
        if(registrationType == RegistrationType.One) I.see("Registrácia úspešne uložená. Boli ste automaticky prihlásený, môžete pristupovať do privátných zón web sídla.");
        else if(registrationType == RegistrationType.Two) I.see("Na vašu e-mailovú adresu bol zaslaný e-mail v ktorom musíte kliknutím na autorizačnú linku potvrdiť vašu registráciu.");
        else if(registrationType == RegistrationType.Three) I.see("Vaša registrácia podlieha schváleniu, po schválení registrácie dostanete e-mailovú správu s prihlasovacími údajmi.");

        I.dontSee("Zadaný e-mail je už použitý, ak neviete prístupové heslo, môžete si ho nechať poslať");
    } else {
        //
        I.dontSee("Registrácia úspešne uložená. Boli ste automaticky prihlásený, môžete pristupovať do privátných zón web sídla.");
        I.dontSee("Na vašu e-mailovú adresu bol zaslaný e-mail v ktorom musíte kliknutím na autorizačnú linku potvrdiť vašu registráciu.")
        I.dontSee("Vaša registrácia podlieha schváleniu, po schválení registrácie dostanete e-mailovú správu s prihlasovacími údajmi.");

        I.see("Zadaný e-mail je už použitý, ak neviete prístupové heslo, môžete si ho nechať poslať");
    }
}

async function openTempEmail(I) {
    //Logg into tempMail page
    I.amOnPage("https://tempmail.plus/en/#!mail");


    //Set domain
    I.clickCss("#domain");
    I.wait(1);
    I.click(locate("button.dropdown-item").withText("fexpost.com"));
    //email name
    I.fillField("#pre_button", "WebJetCMS");
    I.clickCss("#pre_copy");

    I.wait(2);
    /*I.see("Inbox is protected by a PIN-code");
    I.fillField("#pin", tempMailPin);
    I.clickCss("#verify");
    I.wait(2);*/

    //Wait for mail 30 seconds
    I.waitForElement('div.mail', 30);

    //Open mail
    I.clickCss("div.mail");
}

async function checkVerifyEmail(I, registrationType, phase=null) {

    //Open email
    await openTempEmail(I);

    if(registrationType == RegistrationType.One) {
        checkVerifyEmail_One(I);
    } else if(registrationType == RegistrationType.Two) {
        if(phase == null) return;
        else if(phase == 1) return checkVerifyEmail_Two_Phase_1(I);
        else if(phase == 2) return checkVerifyEmail_Two_Phase_2(I);
    }
    else if(registrationType == RegistrationType.Three) {
        return checkVerifyEmail_Three(I);
    }
}

async function checkVerifyEmail_One(I) {
    //Vefiry mail body context
    I.see("Prihlasovacie údaje na");
    I.see("Ďakujeme za registráciu na našom web sídle.");
    I.see("Pre prihlásenie použite nasledovné údaje:");
    I.see("Prihlasovacie meno: " + userName);
    I.see("Heslo: " + password);

    //Remove email
    await removeEmail(I);
}

async function checkVerifyEmail_Two_Phase_1(I) {
    let authLink = "";

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
    await removeEmail(I);

    return authLink;
}

async function checkVerifyEmail_Two_Phase_2(I) {
    //Vefiry mail body context
    I.see("Prihlasovacie údaje na");
    I.see("Ďakujeme za registráciu na našom web sídle.");
    I.see("Pre prihlásenie použite nasledovné údaje:");
    I.see("Prihlasovacie meno: " + userName);

    //Remove email
    await removeEmail(I);
}

async function checkVerifyEmail_Three(I) {
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
    await removeEmail(I);

    return generatedPasswd;
}

async function removeEmail(I) {
    I.clickCss("#delete_mail");
    I.see("Do you really want to delete mail?");
    I.clickCss("#confirm_mail");
    I.wait(2);
    I.see("Waiting for mails...");
    I.dontSeeElement("div.mail");
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
    DT.filter("email", emailAddress);

    //Find gow many rows in DB we have (have many users have this email)
    let numberOfUsers = await I.getTotalRows();

    //If there is more that 0, delete them all
    if(numberOfUsers > 0) {
        I.clickCss("button.buttons-select-all");
        I.clickCss("button.buttons-remove");
        DTE.waitForEditor();
        //bug wrong filter, delete all users on first page
        I.dontSee("vipklient", "div.DTE_Action_Remove");

        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader();
    }

    //Check, we cant see that email
    I.dontSee(emailAddress);
}

/**
 * Remove all old emails from inbox.
 * Need to remove them at start of Scenario. Scenario allways work with 1 email in inbox, more emails will make test fall.
 *
 * @param {*} I
 */
async function deleteAllStroredEmails(I) {
    I.say("Removing old emails from : " + tempMailAddress);

    //Logg into tempMail page
    I.amOnPage("https://tempmail.plus/en/#!mail");
    I.wait(1);

    //Set domain
        I.clickCss("#domain");
        I.wait(1);
        I.click(locate("button.dropdown-item").withText("fexpost.com"));
    //email name
        I.fillField("#pre_button", "WebJetCMS");
        I.clickCss("#pre_copy");

    //If we see this eleent, there are email to delete
    let numberOfEmails = await I.grabNumberOfVisibleElements("#delete");

    if(numberOfEmails > 0) {
        I.clickCss("#delete");
        I.clickCss("#confirm");
        I.wait(2);
    }

    I.see("Waiting for mails...");
}