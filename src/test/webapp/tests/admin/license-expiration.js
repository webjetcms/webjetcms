const { dashboardPageRoute, mockDashboardBootstrap } = require('../../helpers/dashboard-browser');

Feature('admin.license-expiration').tag('@singlethread');

Before(({ login }) => {
    login("admin");
});

Scenario('Test and check license expiration notification', async ({I, Document}) => {
    await mockDashboardBootstrap(I, () => ({}), () => ({ dismissedUntil: {} }));
    //Base
    let actualDate = new Date();
    let monthMillis = 30 * 24 * 60 * 60 * 1000;
    let elementSelector = '#toast-container-overview [data-notice-id="license"]';

    I.say("Say license expiration date to 3 months from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() + (3 * monthMillis));

    I.say("Now check that the license expiration notification is NOT displayed");
    I.amOnPage("/admin/v9/");
    I.waitForElement('.md-dashboard__notice-list[aria-busy="false"]', 20);
    I.dontSeeElement(elementSelector);

    I.say("Set license expiration date to 1 month from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() + monthMillis);

    I.say("Now check that the license expiration notification IS displayed");
    I.amOnPage("/admin/v9/");
    I.waitForElement(elementSelector, 10);
    I.waitForVisible(`${elementSelector} .md-dashboard__notice-description`, 10);
    I.seeElement(`${elementSelector} .md-dashboard__notice-action`);

    I.say("Set license expiration date to value 0");
    Document.setConfigValue("licenseExpiryDate", 0);

    I.say("Now check that the license expiration notification is AGAIN NOT displayed");
    I.amOnPage("/admin/v9/");
    I.waitForElement('.md-dashboard__notice-list[aria-busy="false"]', 20);
    I.dontSeeElement(elementSelector);
    await I.stopMockingRoute(dashboardPageRoute);
});
