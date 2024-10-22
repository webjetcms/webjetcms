Feature('setup');

let confLng = "sk";

Before(({ I }) => {
    confLng = I.getConfLng();
});

Scenario('Setup action', ({I, Document}) => {

    I.say("POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR !");
    I.say("Databaza musí byť prázdna !!! Nesmie obsahovať žiadne tabuľky");
    I.say("-----------------------------------------------------------------");

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

    Document.screenshot("/install/setup/setup.png", 660, 520);

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

    I.click("#btnOk");

    if("sk" === confLng) {
        I.waitForText("WebJET je nakonfigurovaný", 300);
    } else if("en" === confLng){
        I.waitForText("WebJET successfully configured", 300);
    } else if("cs" === confLng){
        I.waitForText("WebJET je nakonfigurován");
    }

    Document.screenshot("/install/setup/setup-saved.png", 660, 180);

});

Scenario('First login/password change /and others', ({I, DTE, Document}) => {
    I.say("POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR !");
    I.say("Tento test musí byť vykonaný po inicializácií DB (setup akcie) a po následnom reštartovaní");
    I.say("WebJET musí byť naštartovaný už aj s licenciou !!");
    I.say("-----------------------------------------------------------------");

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

    I.click("#login-submit");

    I.waitForElement("#newPassword");

    I.fillField("#newPassword", "*********");
    I.fillField("#retypeNewPassword", "*********");

    Document.screenshot("/install/setup/change-password.png");

    I.click("#login-submit");

    Document.screenshot("/install/setup/main-page.png");

    I.amOnPage("/admin/v9/users/user-list/");
    Document.screenshot("/install/setup/users.png");

    I.click("admin");
    DTE.waitForEditor();

    Document.screenshot("/install/setup/user-edit.png");

    I.click("#pills-dt-datatableInit-rightsTab-tab");

    I.scrollTo("#perms_config-leaf");

    I.click('//*[@id="perms_conf.show_all_variables_anchor"]/i[1]');

    Document.screenshot("/install/setup/user-perms.png");
});

Scenario('Pridanie/zmena licencie', ({I, Document}) => {

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
    pause();

    I.click("#btnOk");

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

Scenario('License expiration notification test', ({I, login, Document}) => {
    login("admin");

    let actualDate = new Date();
    let monthMillis = 30 * 24 * 60 * 60 * 1000;

    I.say("Set license expiration date to 1 month from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() - monthMillis);

    I.amOnPage("/admin/v9/");
    I.waitForElement(".license-expiration-warning", 5);

    Document.screenshot("/install/license/license-expiration-notification.png", 1200, 300);

    I.say("Set license expiration date to value 0");
    Document.setConfigValue("licenseExpiryDate", 0);
});