Feature('apps.multistep-forms.email-verification');

/**
 * Test email verification using FormEmailVerificationProcessor.java in multistep forms.
 */

const maxAttempts = 3;
const verifyEmail = "baseMultistepForm_verify";

Scenario('Check email verification', async ({ I, TempMail }) => {
    I.say("Go on page and submit forst page");

    I.amOnPage("/apps/multistep-formular/email-verification.html");

    I.fillField("#meno-1", "Jozko");
    I.fillField("#priezvisko-1", "Mrkva");
    I.fillField("#email-1", verifyEmail + TempMail.getTempMailDomain());
    I.clickCss("button[type='submit']");

    I.say("Get verify code from email - BUT beware, it must be in new tab");
        I.executeScript(() => {
        window.open('https://example.com', '_blank');
    });
    I.waitForNumberOfTabs(2);
    I.switchToNextTab();

    await TempMail.login(verifyEmail);
    await TempMail.openLatestEmail();

    const verifyCode = await I.executeScript(() => {
        const info = document.querySelector("div#info");
        const html = info.innerHTML;

        const match = html.match(/\b\d{5}\b/);
        return match ? match[0] : null;
    });
    I.say("'Verify code from mail is : " + verifyCode);
    I.closeCurrentTab();

    I.say("Try set bad code and test it");
    I.fillField("#verify_code-1", "11111");
    I.clickCss("button[type='submit']");
    I.waitForText('Zadaný overovací kód je neplatný.', 10);

    I.say("Now enter correct code");
    I.fillField("#verify_code-1", verifyCode);
    I.clickCss("button[type='submit']");
    I.waitForText('Formulár bol úspešne odoslaný', 10);
});

Scenario('Check email verification - max attempts', async ({ I, TempMail }) => {
    I.amOnPage("/apps/multistep-formular/email-verification.html");

    I.fillField("#email-1", verifyEmail + TempMail.getTempMailDomain());
    I.clickCss("button[type='submit']");

    I.say("Clear email inbox");
    I.executeScript(() => {
        window.open('https://example.com', '_blank');
    });
    I.waitForNumberOfTabs(2);
    I.switchToNextTab();

    await TempMail.login(verifyEmail);
    await TempMail.destroyInbox();
    I.closeCurrentTab();

    I.say("Try now max attempts");
    for(let i = 1; i <= maxAttempts; i++) {
        I.fillField("#verify_code-1", "11111");
        I.clickCss("button[type='submit']");

        if(i < maxAttempts) {
            I.waitForText('Zadaný overovací kód je neplatný.', 10);
        }
    }

    I.waitForText("Dosiahli ste maximálny počet pokusov na zadanie overovacieho kódu.");
    I.dontSeeElement("form");
});
