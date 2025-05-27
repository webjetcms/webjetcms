Feature('password-recovery');

var basePath = "/redactor/admin/password-recovery/"

Scenario('admin-password-recovery', ({ I, Document, TempMail }) => {
    I.logout();
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

    const confLng = I.getConfLng();

    I.waitForVisible("#language");
    switch (confLng) {
        case "sk":
        case "cs":
            I.selectOption("#language", "Česky");
            break;
        case "en":
            I.selectOption("#language", "English");
            break;
        default:
            throw new Error("Unknown language: " + confLng);
    }

    Document.screenshotElement(lostPasswordBtn, basePath + screensPrefix + "-recover-password-btn.png");

    I.clickCss(lostPasswordBtn);
    I.wait(1);

    Document.screenshot(basePath + screensPrefix + "-recovery-page.png", 1080, 685);
    Document.screenshotElement("#register-submit-btn", basePath + screensPrefix + "-send-btn.png");

    I.fillField('input[name="loginName"]', "sameMail@fexpost.com");
    I.clickCss('button#register-submit-btn');

    switch (confLng) {
        case "sk": I.see('Ak Vaše konto existuje heslo Vám bolo zaslané na e-mailovú adresu.'); break;
        case "cs": I.see("Pokud Vaše konto existuje heslo Vám bylo zasláno na e-mailovou adresu."); break;
        case "en": I.see("If your account exists password has been sent to your email address."); break;
        default:
            throw new Error("Unknown language: " + confLng);
    }

    Document.screenshot(basePath + screensPrefix + "-recovery-page-notif.png", 1080, 685);

    TempMail.login("sameMail");
    TempMail.openLatestEmail();

    Document.screenshotElement("#info", basePath + "email.png");

    I.say("Go change password");
    switch (confLng) {
        case "sk": I.click('Ak si chcete zmeniť heslo, kliknite sem do 30 minút'); break;
        case "cs": I.click('Pokud si chcete heslo změnit, klikněte do 30 minut sem'); break;
        case "en": I.click('To change your password, click here to 30 minutes'); break;
        default:
            throw new Error("Unknown language: " + confLng);
    }

    I.wait(1);
    I.switchToNextTab();

    pause();

    I.fillField('input[type="password"][name="newPassword"]', "asd");
    I.fillField(retypeInput, "asddd");
    Document.screenshotElement(".container", basePath + screensPrefix + "-recovery-form-1.png");

    I.clickCss(submitBtn);
    Document.screenshotElement(".container", basePath + screensPrefix + "-recovery-form-2.png");

    I.fillField('input[type="password"][name="newPassword"]', "asd");
    I.fillField(retypeInput, "asd");
    I.clickCss(submitBtn);
    Document.screenshotElement(".container", basePath + screensPrefix + "-recovery-form-3.png");

    const randomPassword = 'password_P' + I.getRandomText();
    I.fillField('input[type="password"][name="newPassword"]', randomPassword);
    I.fillField(retypeInput, randomPassword);
    I.clickCss(submitBtn);
    Document.screenshot(".container", basePath + screensPrefix + "-recovery-form-4.png");

    I.closeCurrentTab();

    switch (confLng) {
        case "sk": I.click('Ak si chcete zmeniť heslo, kliknite sem do 30 minút'); break;
        case "cs": I.click('Pokud si chcete heslo změnit, klikněte do 30 minut sem'); break;
        case "en": I.click('To change your password, click here to 30 minutes'); break;
        default:
            throw new Error("Unknown language: " + confLng);
    }

    I.wait(1);
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

    switch (confLng) {
        case "sk": I.click('Ak ste nepožiadali o zmenu hesla môžete túto akciu zrušiť kliknutím sem'); break;
        case "cs": I.click('Pokud si chcete heslo změnit, klikněte do 30 minut sem'); break;
        case "en": I.click('To change your password, click here to 30 minutes'); break;
        default:
            throw new Error("Unknown language: " + confLng);
    }
    I.switchToNextTab();

    Document.screenshot(basePath + screensPrefix + "-recovery-form-cancel.png", 700, 400);
}