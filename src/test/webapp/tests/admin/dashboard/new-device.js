const { readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.new-device').tag('@singlethread');

const cookieName = 'wjdevice';
const mailbox = 'webjetcmsnotif@fexpost.com';
const dashboard = '.md-dashboard[data-loaded="true"]';
const dialog = '.md-dashboard-modal--sessions';
let originalCookie;
let eventId;

Scenario('New device login sends an email and can be confirmed from its detail', async ({ I, TempMail, Document }) => {
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
    I.see('Nové prihlásenie z neznámeho zariadenia', row);
    I.see(event.browserName, row);
    I.see(event.operatingSystem, row);
    I.see(event.ipAddress, row);

    I.logout();
    await TempMail.login(mailbox);
    TempMail.openLatestEmail();
    I.see('Nové prihlásenie do WebJET CMS', TempMail.getSubjectSelector());
    I.see(event.browserName, TempMail.getContentSelector());
    I.see(event.browserVersion, TempMail.getContentSelector());
    I.see(event.operatingSystem, TempMail.getContentSelector());
    I.see(event.ipAddress, TempMail.getContentSelector());
    // Matching the new device ID prevents an older notification from satisfying the test.
    const emailLink = `${TempMail.getContentSelector()} a[href="${origin}/admin/v9/?securityEvent=${eventId}"]`;
    I.seeElement(emailLink);
    const tabs = await I.grabNumberOfOpenTabs();
    I.click(emailLink, null, { modifiers: ['ControlOrMeta'] });
    await Document.waitForTab(tabs + 1);
    I.switchToNextTab();
    I.waitForVisible('#username', 10);
    I.relogin('publishNotification', false);
    I.waitForElement(dashboard, 30);
    I.waitForVisible(dialog, 10);
    I.seeInCurrentUrl(`securityEvent=${eventId}`);
    I.see(event.browserName, `${dialog} .md-dashboard-sessions__security`);
    I.see(event.operatingSystem, `${dialog} .md-dashboard-sessions__security`);
    I.see(event.ipAddress, `${dialog} .md-dashboard-sessions__security`);

    I.clickCss(`${dialog} .modal-footer > button:last-child`);
    I.waitForDetached(dialog, 10);
    I.clickCss(`${row} .md-dashboard__notice-confirm`);
    I.waitForInvisible(row, 10);
    I.amOnPage('/admin/v9/');
    I.waitForElement(dashboard, 30);
    I.dontSeeElement(row);
    const confirmed = await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}`);
    I.assertTrue(confirmed.requestedSecurityEvent.confirmedAt > 0, 'Confirmation must be persisted for the device from the email.');
});

Scenario('Clean up the new device notice and restore browser recognition', async ({ I }) => {
    I.setPlaywrightRequestHeaders({});
    if (eventId) {
        I.relogin('publishNotification');
        I.waitForElement(dashboard, 30);
        await I.clickIfVisible(`[data-notice-id="newDevice:${eventId}"] .md-dashboard__notice-confirm`);
    }
    I.logout();
    if (originalCookie) I.setCookie(originalCookie);
    else {
        const domain = await I.executeScript(() => location.hostname);
        I.setCookie({ name: cookieName, value: 'autotest-expired', domain, path: '/', expires: 1 });
    }
    I.closeOtherTabs();
});
