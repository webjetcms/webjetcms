Feature('admin.passkey');

Before(({ I, login }) => {
    login('admin');
});

// to test with invalid SSL/TLS certificate run chrome with --ignore-certificate-errors
// /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --ignore-certificate-errors

Scenario('passkey-zakladne testy', async ({ I, i18n }) => {

    I.clickCss("#dropdownMenuUser");
    I.waitForVisible( locate("a.dropdown-item").withText(i18n.get("PassKey")), 10);
    I.click( locate("a.dropdown-item").withText(i18n.get("PassKey")));
    I.waitForElement("#modalIframe", 10);

    var baseSelector = "#modalIframe div.modal-body-content div.dt-scroll div.dt-scroll-body table.datatableInit";
    I.waitForElement(baseSelector, 10);

    I.waitForText("Chrome Macbook PRO", 10, baseSelector);
    I.see("Safari", baseSelector);
    I.see("Firefox PC desktop", baseSelector);

    //verify button on http VS https page
    I.amOnPage('/logoff.do?forward=/admin/');
    I.amOnPage('/admin/logon/');

    //switch to http - HSTS will change to https:
    /*await I.executeScript(() => {
        console.log("Current protocol: "+window.location.protocol);
        if(window.location.protocol === "https:"){
            window.location.protocol = "http:";
            console.log("Switched to protocol: "+window.location.protocol);
        }
    });

    I.wait(3);
    I.dontSeeInSource('id="passkey-login-submit"');

    //switch to httpS
    await I.executeScript(() => {
        if(window.location.protocol === "http:"){
            window.location.protocol = "https:";
        }
    });*/

    I.wait(3);
    //we cant test real button because in chromium there is no window.PublicKeyCredential object
    I.seeInSource('id="passkey-login-submit"');

    //test passkeys for different users - check that user can see only their passkeys
    I.relogin("tester2");
    I.clickCss("#dropdownMenuUser");
    I.waitForVisible( locate("a.dropdown-item").withText(i18n.get("PassKey")), 10);
    I.click( locate("a.dropdown-item").withText(i18n.get("PassKey")));
    I.waitForElement("#modalIframe", 10);

    I.waitForElement(baseSelector, 10);

    I.waitForText("ntb tester2", 10, baseSelector);
    I.dontSee("Safari", baseSelector);
    I.dontSee("Firefox PC desktop", baseSelector);

});

Scenario('logoff', ({ I }) => {
    I.logout();
});
