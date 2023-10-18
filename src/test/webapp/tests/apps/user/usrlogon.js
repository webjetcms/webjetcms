Feature('apps.usrlogon');

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
    I.click(".login-submit");

    if (checkText) {
        if (correctPassword) I.see("tento text sa zobrazí len prihlásenému používateľovi.");
        else I.dontSee("tento text sa zobrazí len prihlásenému používateľovi.");
    }
}

Scenario('odhlasenie @singlethread', ({ I }) => {
    //I.clickIfVisible("a.js-logout-toggler");
    I.amOnPage('/logoff.do');
});

Scenario('zle zadane heslo @singlethread', ({ I }) => {
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');
    I.fillField("username", "tester");

    //do pola password vyplnim zle heslo
    I.fillField("password", "wrongpassword");
    //kliknem na tlacitko
    I.click(".login-submit");
    //overim, ci sa zobrazi uvedena hlaska
    I.see("Zadané meno alebo heslo je nesprávne.");
});

Scenario('prihlasenie zablokovane @singlethread', ({ I }) => {
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    I.say("Waiting for logon cache expiring");
    I.wait(60);

    loginIN(I, defaultPassword, false);
    I.see("Zadané meno alebo heslo je nesprávne.");

    loginIN(I, defaultPassword, false);
    I.see("Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované");
    I.say("Cakam 10 sekund na expirovanie zablokovanej IP adresy");
    //je potrebne cakat 10 sekund na expirovanie zleho hesla
    I.wait(10);

    loginIN(I, defaultPassword, true);
    I.click("Odhlásenie", "#menu");

    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    //over zablokovanie na 60 sekund po neuspesnom prihlaseni
    for (var i=2; i<=6; i++) {
        I.say("Loggin attempt "+i);
        loginIN(I, defaultPassword, false);

        //lebo pocitadlo funguje len ked nie je blokovane
        I.wait(10);
    }

    I.say("Cakam 60 sekund na expirovanie zablokovanej IP adresy");
    I.wait(10);
    loginIN(I, defaultPassword, true, false);
    I.see("Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované");

    I.wait(55);

    loginIN(I, defaultPassword, true);
    I.click("Odhlásenie", "#menu");
});

Scenario('odhlasenie 2 @singlethread', ({ I }) => {
    I.amOnPage('/logoff.do');
});

Scenario('uspesne prihlasenie @singlethread', ({ I }) => {
    I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona/');

    I.dontSee("Môj profil", "#menu");
    I.dontSee("Odhlásenie", "#menu");

    I.fillField("username", "tester");
    I.fillField("password", secret("*********"));
    I.click(".login-submit");

    I.see("tento text sa zobrazí len prihlásenému používateľovi.");

    //menu polozky
    I.see("Zákaznícka zóna", "#menu");
    I.see("Môj profil", "#menu");
    I.see("Odhlásenie", "#menu");

    I.amOnPage('/apps/prihlaseny-pouzivatel/moj-profil/');

    I.see("Prihlasovacie meno:");
    I.see("Dátum narodenia");

    I.seeInField("#usrLogin", "tester");
    I.seeInField("#usrLastName", "Playwright");
    I.seeInField("#usrFirstName", "Tester");
    I.seeInField("#usrEmail", "tester@balat.sk");

    var address = "autotest-address-"+I.getRandomText();
    I.fillField("#usrAdress", address);
    I.fillField("#usrOldPassword", secret("*********"));

    I.click("#bSubmitIdAjax");
    I.dontSee("Prosím, opravte nasledovné chyby", "#ajaxFormResultContainer");
    I.see("Profil úspešne uložený", "#ajaxFormResultContainer");

    //reload profile, check changes save
    I.amOnPage('/apps/prihlaseny-pouzivatel/moj-profil/');
    I.seeInField("#usrAdress", address);

    I.click("Odhlásenie", "#menu");
    //overim, ci sa zobrazi text Prihlasenie (som korektne odhlaseny)
    I.see("Zadajte vaše prihlasovacie údaje");
    I.dontSee("Môj profil", "#menu");
    I.dontSee("Odhlásenie", "#menu");
});

Scenario('odhlasenie 3 @singlethread', ({ I }) => {
    //I.clickIfVisible("a.js-logout-toggler");
    I.amOnPage('/logoff.do');
});

Scenario('protected file @singlethread', ({ I }) => {
    I.amOnPage("/files/protected/bankari/test-forward.txt");

    I.see("Zadajte vaše prihlasovacie údaje");
    loginIN(I, defaultPassword, true, false);

    I.waitForText("This is test file for fileforward after logon", 20);

    I.amOnPage('/logoff.do');

    I.amOnPage("/files/protected/bankari/test-forward.txt");
    I.see("Zadajte vaše prihlasovacie údaje");
    loginIN(I, defaultPassword, false, false);
    I.see("Zadané meno alebo heslo je nesprávne.");
    loginIN(I, defaultPassword, false, false);
    I.see("Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované");

    I.wait(13);

    loginIN(I, defaultPassword, true, false);
    I.waitForText("This is test file for fileforward after logon", 20);
});