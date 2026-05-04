Feature('admin.passkeys');

Scenario('passkeys screenshots', async ({ I, DT, DTE, Document, i18n }) =>{

    I.amOnPage('/logoff.do?forward=/admin/');
    I.amOnPage('/admin/logon/?language='+I.getConfLng());
    I.selectOption("#language", i18n.get("English"));

    //show passkey button
    await I.executeScript(() => {
        document.getElementById('passkey-login-btn-wrapper')?.style?.setProperty('display', 'block');
    });

    Document.screenshot("/redactor/admin/passkey-logon.png", 1080, 685, "#passkey-login-submit");
    Document.screenshotElement("#passkey-login-submit", "/redactor/admin/passkey-logon-button.png");

    I.say('Prihlasenie sa ako admin');
    I.relogin('admin');

    Document.screenshot("/redactor/admin/passkeys-userselect.png", 1280, 450, "#dropdownMenuUser");

    I.clickCss("#dropdownMenuUser");
    I.waitForVisible( locate("a.dropdown-item").withText(i18n.get("PassKey")), 10);

    Document.screenshot("/redactor/admin/passkey-menu.png", 1280, 450, 'a.dropdown-item.passkey');

    I.click( locate("a.dropdown-item").withText(i18n.get("PassKey")));
    I.waitForElement("#modalIframe", 10);

    var baseSelector = "#modalIframe div.modal-body-content div.dt-scroll div.dt-scroll-body table.datatableInit";
    I.waitForElement(baseSelector, 10);

    I.waitForText("Chrome Macbook PRO", 10, baseSelector);
    I.see("Safari", baseSelector);
    I.see("Firefox PC desktop", baseSelector);

    Document.screenshot("/redactor/admin/passkey-table.png", 1280, 650);

    I.click("#modalIframe button.buttons-create");

    Document.screenshot("/redactor/admin/passkey-register.png", 1280, 650);
});