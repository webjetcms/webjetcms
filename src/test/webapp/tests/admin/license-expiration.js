const { dashboardPageRoute, mockDashboardBootstrap } = require('../../helpers/dashboard-browser');

Feature('admin.license-expiration').tag('@singlethread');

let originalExpiry;
Before(async ({ I, login }) => {
    login('admin');
    if (originalExpiry === undefined) {
        I.amOnPage('/admin/v9/');
        originalExpiry = await I.executeScript(async () => {
            const response = await fetch('/admin/rest/settings/configuration/autocomplete/detail?name=licenseExpiryDate', { headers: { 'X-CSRF-Token': window.csrfToken } });
            if (!response.ok) throw new Error('Cannot preserve license expiry configuration: ' + response.status);
            return (await response.json()).value;
        });
    }
});

Scenario('Test and check license expiration notification', async ({I, Document}) => {
    await mockDashboardBootstrap(I, () => ({}), () => ({ dismissedUntil: {} }));
    //Base
    let actualDate = new Date();
    let monthMillis = 30 * 24 * 60 * 60 * 1000;
    let elementSelector = '#toast-container-overview [data-notice-id="license"]';

    I.say("Say license expiration date to 3 months from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() + (3 * monthMillis), true);

    I.say("Now check that the license expiration notification is NOT displayed");
    I.amOnPage("/admin/v9/");
    I.waitForElement('.md-dashboard__notice-list[aria-busy="false"]', 20);
    I.dontSeeElement(elementSelector);

    I.say("Set license expiration date to 1 month from now");
    Document.setConfigValue("licenseExpiryDate", actualDate.getTime() + monthMillis, true);

    I.say("Now check that the license expiration notification IS displayed");
    I.amOnPage("/admin/v9/");
    I.waitForElement(elementSelector, 10);
    I.waitForVisible(`${elementSelector} .md-dashboard__notice-description`, 10);
    I.seeElement(`${elementSelector} .md-dashboard__notice-action`);

    I.say("Set license expiration date to value 0");
    Document.setConfigValue("licenseExpiryDate", 0, true);

    I.say("Now check that the license expiration notification is AGAIN NOT displayed");
    I.amOnPage("/admin/v9/");
    I.waitForElement('.md-dashboard__notice-list[aria-busy="false"]', 20);
    I.dontSeeElement(elementSelector);
    await I.stopMockingRoute(dashboardPageRoute);
});

/** Restores the effective value even when a notification assertion fails. */
Scenario('Restore the original license expiry configuration', async ({ I, Document }) => {
    await I.stopMockingRoute(dashboardPageRoute);
    if (originalExpiry !== undefined) Document.setConfigValue('licenseExpiryDate', originalExpiry, true);
});
