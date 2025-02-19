Feature('admin.license-expiration');

Before(({ login }) => {
    login("admin");
});

Scenario('Test and check license expiration notification', ({I, Document}) => {
    //Base
    let actualDate = new Date();
    let monthMillis = 30 * 24 * 60 * 60 * 1000;
    let elementSelector = "#toast-container-overview .toast.toast-warning";
    let text = "Platnosť vašej licencie čoskoro vyprší";

    I.say("Say license expiration date to 3 months from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() + (3 * monthMillis));

    I.say("Now check that the license expiration notification is NOT displayed");
    I.amOnPage("/admin/v9/");
    I.wait(1);
    I.dontSeeElement(elementSelector);
    I.dontSee(text);

    I.say("Set license expiration date to 1 month from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() - monthMillis);

    I.say("Now check that the license expiration notification IS displayed");
    I.amOnPage("/admin/v9/");
    I.waitForElement(elementSelector, 10);
    I.see(text);

    I.say("Set license expiration date to value 0");
    Document.setConfigValue("licenseExpiryDate", 0);

    I.say("Now check that the license expiration notification is AGAIN NOT displayed");
    I.amOnPage("/admin/v9/");
    I.wait(1);
    I.dontSeeElement(elementSelector);
    I.dontSee(text);
});