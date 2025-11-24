Feature('apps.app-captcha');

Before(({ I, login }) => {
    login('admin');
});

function setTempSpamProtection(I, DTE, disabled) {
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=4");
    DTE.waitForEditor();
    if (disabled==false) {
        //enable spam protection
        I.uncheckOption({css: "#DTE_Field_disableSpamProtection_0"});
    } else {
        //disable spam protection
        I.checkOption({css: "#DTE_Field_disableSpamProtection_0"});
    }
    DTE.save();
}

function checkCaptchaPresence(I, shouldExist) {
    if (shouldExist) {
        I.seeElement({css: "#wjcaptcha1"});
    } else {
        I.dontSeeElement({css: "#wjcaptcha1"});
    }
}

Scenario("Verify captcha status by spam protection @singlethread", ({ I, DTE, Document }) => {
    Document.setConfigValue('captchaType', 'internal');
    setTempSpamProtection(I, DTE, false);

    //
    I.say("captcha=internal, tempDisabledSpamProtection=false");
    I.amOnPage("/newsletter/registracia-do-newsletra.html");
    checkCaptchaPresence(I, true);

    //
    I.say("captcha=internal, tempDisabledSpamProtection=true");
    setTempSpamProtection(I, DTE, true);
    I.amOnPage("/newsletter/registracia-do-newsletra.html");
    checkCaptchaPresence(I, false);

    //
    I.say("captcha=none, tempDisabledSpamProtection=false");
    Document.setConfigValue('captchaType', 'none');
    setTempSpamProtection(I, DTE, false);
    I.amOnPage("/newsletter/registracia-do-newsletra.html");
    checkCaptchaPresence(I, false);

});

Scenario("reset-type @singlethread", ({ I, Document }) => {
    Document.setConfigValue('captchaType', 'internal');
});

Scenario("reset-temp @singlethread", ({ I, DTE }) => {
    setTempSpamProtection(I, DTE, false);
});