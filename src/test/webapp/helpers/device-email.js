const { readDashboardBootstrap } = require('./dashboard-browser');

/** Opens only an email for the device created by the current notification-account scenario. */
async function openDeviceEmail(I, TempMail, id, expectedText) {
    for (let attempt = 0; attempt < 6; attempt++) {
        await TempMail.login('webjetcmsnotif@fexpost.com');
        await TempMail.openLatestEmail();
        const body = await I.grabTextFrom(TempMail.getContentSelector());
        const links = await I.grabAttributeFromAll(`${TempMail.getContentSelector()} a`, 'href');
        if (body.includes(expectedText) && links.some(link => new URL(link).searchParams.get('securityEvent') === String(id))) return body;
    }
    throw new Error(`No matching device email arrived for event ${id}.`);
}

/** Confirms the scenario's device through its email proof without changing other stored devices. */
async function confirmDeviceFromEmail(I, TempMail, device) {
    const origin = await I.executeScript(() => location.origin);
    await I.openNewTab();
    await openDeviceEmail(I, TempMail, device.id, device.browserName);
    const emailLink = `${TempMail.getContentSelector()} a[href*="/admin/v9/?securityEvent=${device.id}&deviceConfirmation="]`;
    const confirmationUrl = new URL(await I.grabAttributeFrom(emailLink, 'href'));
    // Local databases may retain a production canonical domain; stay on the test server.
    await I.amOnPage(origin + confirmationUrl.pathname + confirmationUrl.search);
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 30);
    const confirmed = await I.executeScript(readDashboardBootstrap, `?securityEvent=${device.id}`);
    await I.assertTrue(confirmed.requestedSecurityEvent.confirmedAt > 0, 'The scenario must confirm its own device.');
    await I.dontSeeElement(`[data-notice-id="newDevice:${device.id}"]`);
    await I.closeCurrentTab();
}

module.exports = { openDeviceEmail, confirmDeviceFromEmail };
