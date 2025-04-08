Feature('setup');

let confLng = "sk";

Before(({ I }) => {
    confLng = I.getConfLng();
});

/*
POZOR: tieto testy je potrebne vykonavat postupne cez @current po jednom scenari a zakazdym restartovat server.
Kod ale obsahuje pause(), takze da sa pustit aj naraz a pri kazdom pause len spravit restart app servera.

Execute SQL to create blank database and connect to it in poolman-local.xml:
DROP DATABASE IF EXISTS blank_web_2;
CREATE DATABASE blank_web_2 DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
*/


Scenario('Setup action', ({I, Document}) => {

    I.say("POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR !");
    I.say("Databaza musí byť prázdna !!! Nesmie obsahovať žiadne tabuľky");
    I.say("-----------------------------------------------------------------");
    pause();

    I.amOnPage("/admin");
    Document.screenshot("/install/setup/error.png", 660, 200);

    I.amOnPage("/wjerrorpages/setup/setup");

    I.say("Nastavte udaje na screenshot");
    I.fillField("#dbDomain", "mariadb.srv.local");
    I.fillField("#dbUsername", "dbuser");
    I.fillField("#dbPassword", "**********");
    I.fillField("#dbName", "webjetcms");
    I.fillField("#conf_installName", "webjetcms");
    I.fillField("#conf_license", "");

    Document.screenshot("/install/setup/setup.png", 900, 1040);

    switch (confLng) {
        case 'sk':
            I.amOnPage("/wjerrorpages/setup/setup");
            break;
        case 'en':
            I.amOnPage("/wjerrorpages/setup/setup?language=en");
            break;
        case 'cs':
            I.amOnPage("/wjerrorpages/setup/setup?language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    I.say("We dont set license key - start as free version");

    I.clickCss("#btnOk");

    if("sk" === confLng) {
        I.waitForText("WebJET je nakonfigurovaný", 300);
    } else if("en" === confLng){
        I.waitForText("WebJET successfully configured", 300);
    } else if("cs" === confLng){
        I.waitForText("WebJET je nakonfigurován");
    }

    Document.screenshot("/install/setup/setup-saved.png", 900, 400);

});

Scenario('First login/password change /and others', ({I, DTE, Document}) => {
    I.say("POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR !");
    I.say("Tento test musí byť vykonaný po inicializácií DB (setup akcie) a po následnom reštartovaní");
    I.say("WebJET musí byť naštartovaný !!");
    I.say("-----------------------------------------------------------------");
    pause();

    I.amOnPage("/admin/logon/");

    switch (confLng) {
        case 'en':
            I.selectOption("language", "English");
            break;
        case 'cs':
            I.selectOption("language", "Česky");
            break;
        case 'sk':
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }


    I.fillField("#username", "admin");
    I.fillField("#password", "heslo");

    Document.screenshot("/install/setup/first-login.png");

    I.clickCss("#login-submit");

    I.waitForElement("#newPassword");

    I.fillField("#newPassword", secret(I.getDefaultPassword()));
    I.fillField("#retypeNewPassword", secret(I.getDefaultPassword()));

    Document.screenshot("/install/setup/change-password.png", 1280, 950);

    I.clickCss("#login-submit");

    Document.screenshot("/install/setup/main-page.png");

    I.amOnPage("/admin/v9/users/user-list/");
    Document.screenshot("/install/setup/users.png");

    I.click("admin");
    DTE.waitForEditor();

    Document.screenshot("/install/setup/user-edit.png");

    I.clickCss("#pills-dt-datatableInit-rightsTab-tab");

    I.scrollTo("#perms_config-leaf");

    I.click('//*[@id="perms_conf.show_all_variables_anchor"]/i[1]');

    Document.screenshot("/install/setup/user-perms.png");

    DTE.save();
});

Scenario('License expiration notification test', ({I, Document}) => {
    I.relogin("ADMIN");

    let actualDate = new Date();
    let monthMillis = 30 * 24 * 60 * 60 * 1000;

    I.say("Set license expiration date to 1 month from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() - monthMillis);

    I.amOnPage("/admin/v9/");
    I.waitForElement("#toast-container-overview > div.toast.toast-warning", 5);

    Document.screenshot("/install/license/license-expiration-notification.png", 1200, 300);

    I.say("Set license expiration date to value 0");
    Document.setConfigValue("licenseExpiryDate", 0);
});

Scenario('Pridanie/zmena licencie', ({I, Document}) => {

    I.say("POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR !");
    I.say("Tento test vykonaj ta, že zadaj sasdfadsfasdf (neplatné licenčné číslo, min. 10 znakov) do konf. premennej license a reštartuj WebJET");
    I.say("Prihlasujes sa ako ADMIN ale s heslom testera");
    I.say("-----------------------------------------------------------------");

    I.relogin("ADMIN");
    Document.setConfigValue("license", "sasdfadsfasdf");
    I.logout();

    pause();

    switch (confLng) {
        case 'sk':
            I.amOnPage("/wjerrorpages/setup/license");
            break;
        case 'en':
            I.amOnPage("/wjerrorpages/setup/license?language=en");
            break;
        case 'cs':
            I.amOnPage("/wjerrorpages/setup/license?language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    Document.screenshot("/install/license/license.png");

    I.fillField("#username", "admin");

    I.say("Zadaj heslo a licencne cislo");
    I.fillField("#password", secret(I.getDefaultPassword()));

    I.clickCss("#btnOk");

    switch (confLng) {
        case 'sk':
            I.waitForText("Licencia úspešne zmenená.", 10);
            break;
        case 'en':
            I.waitForText("License successfully changed.", 10);
            break;
        case 'cs':
            I.waitForText("Licence byla úspěšně změněna.", 10);
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }


    Document.screenshot("/install/license/license-saved.png");
});

