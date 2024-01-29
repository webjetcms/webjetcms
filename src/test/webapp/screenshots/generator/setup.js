Feature('setup');

Scenario('Setup action', ({I, Document}) => {

    I.say("POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR !");
    I.say("Databaza musí byť prázdna !!! Nesmie obsahovať židne tabuľky");
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

    I.amOnPage("/wjerrorpages/setup/setup");
    I.say("Refreshni obrazovku a zadaj licencne cislo");
    pause();

    I.click("#btnOk");
    I.waitForText("WebJET je nakonfigurovaný", 300);

    Document.screenshot("/install/setup/setup-saved.png", 660, 180);

    pause();
});

Scenario('First login/password change /and others', ({I, DTE, Document}) => {
    I.say("POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR ! POZOR !");
    I.say("Tento test musí byť vykonaný po inicializácií DB (setup akcie) a po následnom reštartovaní");
    I.say("WebJET musí byť naštartovaný už aj s licenciou !!");
    I.say("-----------------------------------------------------------------");

    I.amOnPage("/admin/logon/");

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

    I.amOnPage("/wjerrorpages/setup/license");

    Document.screenshot("/install/license/license.png");

    I.fillField("#username", "admin");

    I.say("Zadaj heslo a licencne cislo");
    pause();

    I.click("#btnOk");
    I.waitForText("Licencia úspešne zmenená.", 10);

    Document.screenshot("/install/license/license-saved.png");
});