Feature('password-recovery');

var basePath = "/redactor/admin/password-recovery/"

Scenario('admin-password-recovery', ({ I, Document, TempMail }) => {
    doScreens(I, Document, TempMail, true);
});

Scenario('delete cache objects to prevent logon form wrong password counting, 4 @singlethread', ({ I, Document }) => {
    Document.deleteAllCacheObjects();
});

Scenario('user-password-recovery', ({ I, Document, TempMail }) => {
    I.wait(11);
    doScreens(I, Document, TempMail, false);
});

Scenario('password-recovery remove emails', async ({ TempMail }) => {
    TempMail.login("sameMail");
    await TempMail.destroyInbox();
});

function doScreens(I, Document, TempMail, isAdmin) {

    let screensPrefix;
    let lostPasswordBtn;
    let retypeInput;
    let logonLink;
    let submitBtn;

    if(isAdmin === true) {
        screensPrefix = "admin";
        lostPasswordBtn = "button.lost-password";
        retypeInput = 'input[type="password"][name="retypeNewPassword"]';
        logonLink = "/admin/logon/#/";
        submitBtn = 'button#login-submit';
    } else {
        screensPrefix = "user";
        lostPasswordBtn = 'a[href="#sendPassword"]';
        retypeInput = 'input[type="password"][name="retypePassword"]';
        logonLink = "/apps/prihlaseny-pouzivatel/zakaznicka-zona/";
        submitBtn = 'input[type="submit"]';
    }

    I.say("Ask for password change");
    I.amOnPage(logonLink);
    I.waitForVisible(lostPasswordBtn);
    I.clickCss(lostPasswordBtn);

    I.waitForVisible("#sendPasswd");
    Document.screenshot(basePath + screensPrefix + "-recovery-page.png", 1080, 685);

    Document.screenshotElement(lostPasswordBtn, basePath + screensPrefix + "-recover-password-btn.png");
    Document.screenshotElement("#register-submit-btn", basePath + screensPrefix + "-send-btn.png");

    I.fillField('input[name="loginName"]', "sameMail@fexpost.com");
    I.clickCss('button#register-submit-btn');
    I.see('Ak Vaše konto existuje heslo Vám bolo zaslané na e-mailovú adresu.');

    Document.screenshot(basePath + screensPrefix + "-recovery-page-notif.png", 1080, 685);

    TempMail.login("sameMail");
    TempMail.openLatestEmail();

    Document.screenshotElement("#info", basePath + "email.png");

    I.say("Go change password");
    I.click("Ak si chcete zmeniť heslo, kliknite sem do 30 minút.");
    I.switchToNextTab();

    I.fillField('input[type="password"][name="newPassword"]', "asd");
    I.fillField(retypeInput, "asddd");
    Document.screenshot(basePath + screensPrefix + "-recovery-form-1.png", 700, 550);

    I.clickCss(submitBtn);
    Document.screenshot(basePath + screensPrefix + "-recovery-form-2.png", 700, 650);

    I.fillField('input[type="password"][name="newPassword"]', "asd");
    I.fillField(retypeInput, "asd");
    I.clickCss(submitBtn);
    Document.screenshot(basePath + screensPrefix + "-recovery-form-3.png", 700, 650);

    const randomPassword = 'password_P' + I.getRandomText();
    I.fillField('input[type="password"][name="newPassword"]', randomPassword);
    I.fillField(retypeInput, randomPassword);
    I.clickCss(submitBtn);
    Document.screenshot(basePath + screensPrefix + "-recovery-form-4.png", 700, 400);

    I.closeCurrentTab();
    I.click("Ak si chcete zmeniť heslo, kliknite sem do 30 minút.");
    I.switchToNextTab();
    Document.screenshot(basePath + screensPrefix + "-recovery-form-notWorking.png", 700, 400);

    I.say("Ask for change again and cancel it");
    I.wait(11);
    I.say("Ask for password change");
    I.amOnPage(logonLink);
    I.waitForVisible(lostPasswordBtn);
    I.clickCss(lostPasswordBtn);
    I.waitForVisible("#sendPasswd");
    I.fillField('input[name="loginName"]', "sameMail@fexpost.com");
    I.clickCss('button#register-submit-btn');
    I.see('Ak Vaše konto existuje heslo Vám bolo zaslané na e-mailovú adresu.');

    TempMail.login("sameMail");
    TempMail.openLatestEmail();

    I.click("Ak ste nepožiadali o zmenu hesla môžete túto akciu zrušiť kliknutím sem.");
    I.switchToNextTab();

    Document.screenshot(basePath + screensPrefix + "-recovery-form-cancel.png", 700, 400);
}