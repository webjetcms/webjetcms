const { readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.new-device').tag('@singlethread');

const cookieName = 'wjdevice';
const mailbox = 'webjetcmsnotif@fexpost.com';
const dashboard = '.md-dashboard[data-loaded="true"]';
const dialog = '.md-dashboard-modal--sessions';
let originalCookie;
let eventId;

Scenario('New device login sends an email and confirms its link with a success toast', async ({ I, TempMail }) => {
    I.amOnPage('/logoff.do?forward=/admin/logon/');
    I.wjSetDefaultWindowSize();
    originalCookie = await I.grabCookie(cookieName);
    const origin = await I.executeScript(() => location.origin);
    // Expire only browser recognition, preserving the login form's session and CSRF cookie.
    I.setCookie({ name: cookieName, value: 'autotest-expired', domain: new URL(origin).hostname, path: '/', expires: 1 });
    I.dontSeeCookie(cookieName);

    await TempMail.login(mailbox);
    await TempMail.destroyInbox();

    // Exercise device detection even when the regular E2E User-Agent is excluded.
    const userAgent = await I.executeScript(() => navigator.userAgent);
    I.setPlaywrightRequestHeaders({ 'User-Agent': `${userAgent} WebJET-autotest-new-device` });
    const started = Date.now();
    I.relogin('publishNotification');
    I.waitForElement(dashboard, 30);
    const bootstrap = await I.executeScript(readDashboardBootstrap);
    const newEvents = bootstrap.notices.filter(notice => notice.kind === 'newDevice' && notice.securityEvent.createDate >= started);
    I.assertEqual(newEvents.length, 1, 'A login without browser recognition must create one new device notice.');
    const event = newEvents[0].securityEvent;
    eventId = event.id;
    const row = `[data-notice-id="newDevice:${eventId}"]`;
    I.seeCookie(cookieName);
    I.see('Prihlásili ste sa z nového prehliadača', row);
    I.see('Tento prehliadač', row);
    I.see(event.browserName, row);
    I.see(event.operatingSystem, row);
    I.dontSeeElement(`${row} .md-dashboard__notice-report`);

    I.logout();
    await TempMail.login(mailbox);
    TempMail.openLatestEmail();
    I.see('Nové prihlásenie do WebJET CMS', TempMail.getSubjectSelector());
    I.see(event.browserName, TempMail.getContentSelector());
    I.see(event.browserVersion, TempMail.getContentSelector());
    I.see(event.operatingSystem, TempMail.getContentSelector());
    I.see(event.ipAddress, TempMail.getContentSelector());
    // Matching the new device ID prevents an older notification from satisfying the test.
    const emailLink = `${TempMail.getContentSelector()} a[href*="/admin/v9/?securityEvent=${eventId}&deviceConfirmation="]`;
    I.seeElement(emailLink);
    const confirmationUrl = new URL(await I.grabAttributeFrom(emailLink, 'href'));
    I.assertTrue(/^[A-Za-z0-9_-]{43}$/.test(confirmationUrl.searchParams.get('deviceConfirmation')), 'Email confirmation requires a 256-bit secret.');
    // Local databases may retain a production canonical domain; follow the emailed path only on this test server.
    I.openNewTab();
    I.amOnPage(origin + confirmationUrl.pathname + confirmationUrl.search);
    I.waitForVisible('#username', 10);
    I.relogin('publishNotification', false);
    I.waitForElement(dashboard, 30);
    I.seeInCurrentUrl(`securityEvent=${eventId}`);
    I.waitForText('Prihlásenie bolo potvrdené.', 10, '.toast-success');
    I.see('Moje aktívne prihlásenia', '.toast-success .toast-title');
    I.dontSeeElement(dialog);
    I.dontSeeInCurrentUrl('deviceConfirmation=');
    I.dontSeeElement(row);
    I.amOnPage('/admin/v9/');
    I.waitForElement(dashboard, 30);
    I.dontSeeElement(row);
    const confirmed = await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}`);
    I.assertTrue(confirmed.requestedSecurityEvent.confirmedAt > 0, 'Confirmation must be persisted for the device from the email.');
});

Scenario('The current browser requires the emailed six-digit code and rejects replay', async ({ I, TempMail }) => {
    I.amOnPage('/logoff.do?forward=/admin/logon/');
    const origin = await I.executeScript(() => location.origin);
    const userAgent = await I.executeScript(() => navigator.userAgent);
    I.setCookie({ name: cookieName, value: 'autotest-expired', domain: new URL(origin).hostname, path: '/', expires: 1 });
    await TempMail.login(mailbox);
    await TempMail.destroyInbox();
    I.setPlaywrightRequestHeaders({ 'User-Agent': `${userAgent} WebJET-autotest-new-device` });
    I.relogin('publishNotification');
    I.waitForElement(dashboard, 30);
    const bootstrap = await I.executeScript(readDashboardBootstrap);
    const current = bootstrap.currentSessions.userSessions.flatMap(cluster => cluster.userSessions)
        .find(session => session.sessionId === bootstrap.currentSessions.currentSessionId);
    eventId = current.deviceId;
    I.assertTrue(eventId > 0 && current.deviceConfirmed === false, 'New browsers must stay unconfirmed until email verification.');
    const row = `[data-notice-id="newDevice:${eventId}"]`;
    I.clickCss(`${row} .md-dashboard__notice-confirm`);
    I.waitForText('Zadajte 6-miestny kód', 10, row);
    I.seeElement(row);

    I.openNewTab();
    await TempMail.login(mailbox);
    I.waitForText('Kód na potvrdenie prihlásenia do WebJET CMS', 60);
    TempMail.openLatestEmail();
    I.see('Kód na potvrdenie prihlásenia do WebJET CMS', TempMail.getSubjectSelector());
    I.seeElement(`${TempMail.getContentSelector()} a[href*="/admin/v9/?securityEvent=${eventId}"]`);
    const body = await I.grabTextFrom(TempMail.getContentSelector());
    const code = body.match(/(?:^|[^0-9])([0-9]{6})(?![0-9])/)?.[1];
    I.assertTrue(Boolean(code), 'The verification email must contain a six-digit code.');
    I.closeCurrentTab();
    I.fillField(`${row} input[name="deviceConfirmationCode"]`, code);
    I.clickCss(`${row} .md-dashboard-device-confirmation [type="submit"]`);
    I.waitForInvisible(row, 10);
    const replayStatus = await I.executeScript(async ({ id, code }) => {
        const response = await fetch(`/admin/rest/security/login-events/${id}/confirm`, {
            method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: JSON.stringify({ code })
        });
        return response.status;
    }, { id: eventId, code });
    I.assertEqual(replayStatus, 400, 'A consumed email code must never be accepted again.');
    I.amOnPage('/admin/v9/');
    I.waitForElement(dashboard, 30);
    I.dontSeeElement(row);
});

Scenario('Clean up the new device notice and restore browser recognition', async ({ I }) => {
    I.setPlaywrightRequestHeaders({});
    I.logout();
    if (originalCookie) I.setCookie(originalCookie);
    else {
        const domain = await I.executeScript(() => location.hostname);
        I.setCookie({ name: cookieName, value: 'autotest-expired', domain, path: '/', expires: 1 });
    }
    I.closeOtherTabs();
});
