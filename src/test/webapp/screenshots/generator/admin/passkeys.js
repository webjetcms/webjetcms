Feature('admin.passkeys');

Scenario('passkeys screenshots', async ({ I, DT, DTE, Document, i18n }) =>{

    I.amOnPage('/logoff.do?forward=/admin/');
    I.amOnPage('/admin/logon/?language='+I.getConfLng()+'&id=2');
    I.selectOption("#language", i18n.get("English"));

    //show passkey button
    await I.executeScript(() => {
        document.getElementById('passkey-login-btn-wrapper')?.style?.setProperty('display', 'block');
    });

    Document.screenshot("/redactor/admin/passkey-logon.png", 1080, 685, "#passkey-login-submit");
    Document.screenshotElement("#passkey-login-submit", "/redactor/admin/passkey-logon-button.png");

    I.say('Prihlasenie sa ako admin');
    I.relogin('admin');
    I.amOnPage('/admin/v9/users/user-list/');

    Document.screenshot("/redactor/admin/passkeys-userselect.png", 1280, 450, "#dropdownMenuUser");

    I.clickCss("#dropdownMenuUser");
    I.waitForVisible( locate("a.dropdown-item").withText(i18n.get("PassKey")), 10);

    let path = '/admin/passkey.jsp';
    Document.screenshot("/redactor/admin/passkey-menu.png", 1280, 450, 'a[onclick*="WJ.openPopupDialog(\'' + path + '\')"]');

    I.amOnPage(path);
    I.waitForElement('#passkey-register-section', 10);

    Document.screenshot("/redactor/admin/passkey-register.png", 1280, 450);
});