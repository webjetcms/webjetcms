Feature('admin.login');

Scenario('odhlasenie @singlethread', ({ I }) => {
    //I.clickIfVisible("a.js-logout-toggler");
    I.logout();
});

//Kazdy scenar sa spusta samostatne a samostatne sa vyhodnocuje
Scenario('zle zadane heslo @singlethread', ({ I }) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");

    //do pola password vyplnim zle heslo
    I.fillField("password", "wrongpassword");
    //kliknem na tlacitko
    I.click("login-submit");
    //overim, ci sa zobrazi uvedena hlaska
    I.see("Zadané meno alebo heslo je nesprávne.");
});

function login(I, correctPassword=false, checkText=true) {
    I.wait(1);
    I.fillField("username", "tester");
    if (correctPassword) I.fillField("password", secret(I.getDefaultPassword()));
    else I.fillField("password", "tralala");
    I.clickCss("#login-submit");

    if (checkText) {
        if (correctPassword) I.see("Moje posledné stránky");
        else I.dontSee("Moje posledné stránky");
    }
}

Scenario('prihlasenie zablokovane @singlethread', ({ I }) => {
    I.amOnPage('/admin/');

    I.say("Waiting for logon cache expiring");
    I.wait(60);

    login(I, false);
    I.see("Zadané meno alebo heslo je nesprávne.");

    login(I, false);
    I.see("Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované");
    I.say("Cakam 10 sekund na exspirovanie zablokovanej IP adresy");
    //je potrebne cakat 10 sekund na exspirovanie zleho hesla
    I.wait(10);

    login(I, true);
    I.click(".js-logout-toggler");

    I.amOnPage('/admin/');

    //over zablokovanie na 60 sekund po neuspesnom prihlaseni
    for (var i=2; i<=6; i++) {
        I.say("Loggin attempt "+i);
        login(I, false);

        //lebo pocitadlo funguje len ked nie je blokovane
        I.wait(10)
    }

    I.say("Cakam 60 sekund na exspirovanie zablokovanej IP adresy");
    I.wait(10);
    login(I, true, false);
    I.see("Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované");

    I.wait(55);

    login(I, true);
    I.click(".js-logout-toggler");
});

Scenario('uspesne prihlasenie @singlethread', ({ I }) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");

    I.fillField("password", secret(I.getDefaultPassword()));
    I.click("login-submit");
    //konecne som prihlaseny, pockame na VUE load
    I.wait(5);
    I.see("Vitajte, Tester Playwright");
    //menu polozky
    I.wait(1);
    I.see("Úvod");
    I.see("Monitorovanie servera");

    I.amOnPage('/admin/');
    I.wait(1);
    //zobrazim dropdown s menom usera, ten vyberam cez CSS selector
    I.click({css: "button.js-profile-toggler"});
    //overim, ci vidim moznost Odhlasenia
    I.see("Odhlásenie");
    //kliknem na odhlasenie, vsimnite si, ze to selectujem podla textu linky
    I.click("Odhlásenie");
    //overim, ci sa zobrazi text Prihlasenie (som korektne odhlaseny)
    I.see("Meno alebo e-mail používateľa");
});

Scenario('Test prihlasenia uzivatela SHA512/BCrypto @singlethread', ({ I }) => {
    //SHA512
    I.amOnPage('/admin/');
    I.fillField("username", "user_sha512");

    I.fillField("password", secret("N0v3H3sL0"));
    I.click("login-submit");

    I.wait(5);
    I.see("Vitajte, TestUser SHA512");

    I.click("body > div.ly-page-wrapper > div.ly-header > div > div.header-link-wrapper > div:nth-child(4) > a");

    //BCrypto
    I.amOnPage('/admin/');
    I.fillField("username", "user_bcrypt");

    I.fillField("password", secret("N0v3H3sL0"));
    I.click("login-submit");

    I.wait(5);
    I.see("Vitajte, TestUser Bcrypto");

    I.click("body > div.ly-page-wrapper > div.ly-header > div > div.header-link-wrapper > div:nth-child(4) > a");
 });

 function changePasswordHeslo(I, DT, DTE) {
    I.logout();

    I.wait(5);

    //musel som spravit takto skaredo, lebo login funkcia inak nesla
    I.fillField("username", "tester");
    I.fillField("password", secret(I.getDefaultPassword()));
    I.forceClick("Prihlásiť sa");
    I.waitForText("Tester Playwright", 60);

    I.wait(3);

    I.amOnPage("/admin/v9/users/user-list/");
    DT.filterContains("login", "user_slabeheslo");
    I.click("user_slabeheslo");
    DTE.waitForEditor();

    DTE.fillField("password", "heslo");
    I.checkOption("#DTE_Field_editorFields-allowWeakPassword_0");
    DTE.save();

    I.logout();

    I.fillField("username", "user_slabeheslo");
    I.fillField("password", "heslo");
    I.forceClick("Prihlásiť sa");
 }

 Scenario('Test slabeho hesla a jeho zmeny @singlethread', ({ I, DT, DTE, Browser }) => {
    //Spring nedokazal po zmene hesla zobrazit FE, lebo nemal nastavene locales
    //najskor userovi nastavime slabe heslo a overime, ze zmena prebehne korektne
    changePasswordHeslo(I, DT, DTE);

    I.see("Vaše heslo nespĺňa bezpečnostné nastavenia aplikácie, alebo mu vypršala platnosť");
    if (Browser.isFirefox()) I.wait(1);

    var randomNumber = I.getRandomTextShort();
    var newPassword = "Passw0rd"+randomNumber;

    I.fillField("#newPassword", newPassword);

    I.say("over vyplnenie nespravneho potvrdenia hesla");
    I.fillField("#retypeNewPassword", "x"+newPassword);
    I.clickCss("#login-submit");
    I.see("Nové heslo a opakovanie nového hesla sa nezhodujú");
    if (Browser.isFirefox()) I.wait(1);

    I.say("vypln a over prihlasenie");
    I.fillField("#newPassword", newPassword);
    I.fillField("#retypeNewPassword", newPassword);
    I.clickCss("#login-submit");

    I.say("over zobrazenie welcome obrazovky");
    I.see("Vitajte, User Slabeheslo");
    I.see("Prehľad");

    I.logout();

    I.say("skus sa este prihlasit normalne");
    I.fillField("username", "user_slabeheslo");
    I.fillField("password", newPassword);
    I.forceClick("Prihlásiť sa");

    I.see("Vitajte, User Slabeheslo");
    I.see("Prehľad");

    I.say("over znova zmenu hesla, ci nepovoli pass history");
    I.logout();

    changePasswordHeslo(I, DT, DTE);
    I.see("Vaše heslo nespĺňa bezpečnostné nastavenia aplikácie, alebo mu vypršala platnosť");
    if (Browser.isFirefox()) I.wait(1);

    I.fillField("#newPassword", newPassword);
    I.fillField("#retypeNewPassword", newPassword);
    I.clickCss("#login-submit");

    I.see("Pri spracovaní formuláru nastali chyby")
    I.see("toto heslo bolo už použité v minulosti, použite nové heslo");
    if (Browser.isFirefox()) I.wait(1);

    I.fillField("#newPassword", "x"+newPassword);
    I.fillField("#retypeNewPassword", "x"+newPassword);
    I.clickCss("#login-submit");

    I.see("Vitajte, User Slabeheslo");
    I.see("Prehľad");

 });

 Scenario('odhlasenie na konci @singlethread', ({ I }) => {
    //I.clickIfVisible("a.js-logout-toggler");
    I.logout();
});

Scenario('kontrola presmerovanie .do linky @singlethread', ({ I }) => {
    I.amOnPage("/admin/logon.do");

    I.dontSee("Chyba 404 - požadovaná stránka neexistuje");
    I.see("Meno alebo e-mail používateľa");
});

Scenario('NTLM prihlasenie', async ({ I }) => {
    I.amOnPage("/ntlm/logon.do");

    let title = await I.grabTitle();
    I.assertEqual("403", title);

    I.see("Chyba 404 - požadovaná stránka neexistuje");
});
