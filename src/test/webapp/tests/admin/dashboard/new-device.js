const { readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.new-device').tag('@singlethread');

const cookieName = 'wjdevice';
const mailbox = 'webjetcmsnotif@fexpost.com';
const dashboard = '.md-dashboard[data-loaded="true"]';
const dialog = '.md-dashboard-modal--device-security';
let originalCookie, origin, device;

Before(async ({ I }) => {
    await I.amOnPage('/logoff.do?forward=/admin/logon/');
    await I.wjSetDefaultWindowSize();
    originalCookie = await I.grabCookie(cookieName);
    origin = await I.executeScript(() => location.origin);
    // Each scenario owns a fresh browser record, even when run on its own.
    await I.setCookie({ name: cookieName, value: 'autotest-expired', domain: new URL(origin).hostname, path: '/', expires: 1 });
    await I.dontSeeCookie(cookieName);
    const userAgent = await I.executeScript(() => navigator.userAgent);
    await I.setPlaywrightRequestHeaders({ 'User-Agent': `${userAgent} WebJET-autotest-new-device` });
    await I.relogin('publishNotification');
    await I.waitForElement(dashboard, 30);
    const bootstrap = await I.executeScript(readDashboardBootstrap);
    const current = bootstrap.currentSessions.userSessions.flatMap(cluster => cluster.userSessions)
        .find(session => session.sessionId === bootstrap.currentSessions.currentSessionId);
    await I.assertTrue(current.deviceId > 0 && current.deviceConfirmed === false, 'A new browser must remain unconfirmed.');
    const notices = bootstrap.notices.filter(notice => notice.kind === 'newDevice' && notice.securityEvent.id === current.deviceId);
    await I.assertEqual(notices.length, 1, 'The current browser must have exactly one new-device notice.');
    device = notices[0].securityEvent;
});

After(({ I }) => {
    I.setPlaywrightRequestHeaders({});
    I.amOnPage('/logoff.do?forward=/admin/logon/');
    if (originalCookie) I.setCookie(originalCookie);
    else I.setCookie({ name: cookieName, value: 'autotest-expired', domain: new URL(origin).hostname, path: '/', expires: 1 });
    I.closeOtherTabs();
});

async function openDeviceEmail(I, TempMail, id, expectedText) {
    for (let attempt = 0; attempt < 6; attempt++) {
        await TempMail.login(mailbox);
        await TempMail.openLatestEmail();
        const body = await I.grabTextFrom(TempMail.getContentSelector());
        const links = await I.grabAttributeFromAll(`${TempMail.getContentSelector()} a`, 'href');
        if (body.includes(expectedText) && links.some(link => new URL(link).searchParams.get('securityEvent') === String(id))) return body;
    }
    throw new Error(`No matching device email arrived for event ${id}.`);
}

Scenario('New device login sends an email and confirms its link with a success toast @screenshot', async ({ I, TempMail, Document, i18n }) => {
    const event = device;
    const eventId = event.id;
    const row = `[data-notice-id="newDevice:${eventId}"]`;
    await I.seeCookie(cookieName);
    await I.see(i18n.get('You signed in from a new browser'), row);
    await I.see(i18n.get('This browser'), row);
    await I.see(event.browserName, row);
    await I.see(event.operatingSystem, row);
    await I.dontSeeElement(`${row} .md-dashboard__notice-report`);
    Document.screenshotElement(row, '/redactor/admin/device-new-browser.png');

    await I.logout();
    await openDeviceEmail(I, TempMail, eventId, event.browserName);
    await I.see(i18n.get('New sign-in to WebJET CMS'), TempMail.getSubjectSelector());
    await I.see(event.browserName, TempMail.getContentSelector());
    await I.see(event.browserVersion, TempMail.getContentSelector());
    await I.see(event.operatingSystem, TempMail.getContentSelector());
    await I.see(event.ipAddress, TempMail.getContentSelector());
    Document.screenshotElement(TempMail.getContentSelector(), '/redactor/admin/device-new-browser-email.png', 1000, 760);
    // Matching the new device ID prevents an older notification from satisfying the test.
    const emailLink = `${TempMail.getContentSelector()} a[href*="/admin/v9/?securityEvent=${eventId}&deviceConfirmation="]`;
    await I.seeElement(emailLink);
    const confirmationUrl = new URL(await I.grabAttributeFrom(emailLink, 'href'));
    await I.assertTrue(/^[A-Za-z0-9_-]{43}$/.test(confirmationUrl.searchParams.get('deviceConfirmation')), 'Email confirmation requires a 256-bit secret.');
    // Local databases may retain a production canonical domain; follow the emailed path only on this test server.
    await I.openNewTab();
    await I.amOnPage(origin + confirmationUrl.pathname + confirmationUrl.search);
    await I.waitForVisible('#username', 10);
    await I.relogin('publishNotification', false);
    await I.waitForElement(dashboard, 30);
    await I.seeInCurrentUrl(`securityEvent=${eventId}`);
    await I.waitForText(i18n.get('The sign-in has been confirmed.'), 10, '.toast-success');
    await I.see(i18n.get('My active sessions'), '.toast-success .toast-title');
    await I.dontSeeElement(dialog);
    await I.dontSeeInCurrentUrl('deviceConfirmation=');
    await I.dontSeeElement(row);
    await I.amOnPage('/admin/v9/');
    await I.waitForElement(dashboard, 30);
    await I.dontSeeElement(row);
    const confirmed = await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}`);
    await I.assertTrue(confirmed.requestedSecurityEvent.confirmedAt > 0, 'Confirmation must be persisted for the device from the email.');
});

Scenario('The current browser requires the emailed six-digit code and rejects replay @screenshot', async ({ I, TempMail, Document, i18n }) => {
    const eventId = device.id;
    const row = `[data-notice-id="newDevice:${eventId}"]`;
    await I.clickCss(`${row} .md-dashboard__notice-confirm`);
    await I.waitForText(i18n.get('Enter the 6-digit code we sent you by email. It expires in 10 minutes.'), 10, row);
    await I.seeElement(row);
    Document.screenshotElement(row, '/redactor/admin/device-confirm-code.png');

    await I.openNewTab();
    const body = await openDeviceEmail(I, TempMail, eventId, i18n.get('To confirm this sign-in, enter this one-time code in the administration:'));
    await I.see(i18n.get('WebJET CMS sign-in confirmation code'), TempMail.getSubjectSelector());
    await I.seeElement(`${TempMail.getContentSelector()} a[href*="/admin/v9/?securityEvent=${eventId}"]`);
    const code = body.match(/(?:^|[^0-9])([0-9]{6})(?![0-9])/)?.[1];
    await I.assertTrue(Boolean(code), 'The verification email must contain a six-digit code.');
    Document.screenshotElement(TempMail.getContentSelector(), '/redactor/admin/device-confirm-code-email.png', 1000, 760);
    await I.closeCurrentTab();
    await I.fillField(`${row} input[name="deviceConfirmationCode"]`, code);
    await I.clickCss(`${row} .md-dashboard-device-confirmation [type="submit"]`);
    await I.waitForInvisible(row, 10);
    const replayStatus = await I.executeScript(async ({ id, code }) => {
        const response = await fetch(`/admin/rest/security/login-events/${id}/confirm`, {
            method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: JSON.stringify({ code })
        });
        return response.status;
    }, { id: eventId, code });
    await I.assertEqual(replayStatus, 400, 'A consumed email code must never be accepted again.');
    await I.amOnPage('/admin/v9/');
    await I.waitForElement(dashboard, 30);
    await I.dontSeeElement(row);
});

Scenario('A reported browser requires an email code before administration or REST access @screenshot', async ({ I, TempMail, Document, i18n }) => {
    const eventId = device.id;
    await I.amOnPage(`/admin/v9/?securityEvent=${eventId}`);
    await I.waitForElement(`${dialog} .md-dashboard-device-security__report`, 20);
    await I.waitForFunction(() => {
        const modal = document.querySelector('.md-dashboard-modal--device-security');
        return modal && getComputedStyle(modal).opacity === '1' && getComputedStyle(modal.querySelector('.modal-dialog')).transform === 'none';
    }, 10);
    Document.screenshotElement(`${dialog} .modal-content`, '/redactor/admin/device-block.png');
    await I.clickCss(`${dialog} .md-dashboard-device-security__report`);
    await I.waitForVisible('#username', 15);

    if (Document.isScreenshotsEnabled()) await I.resizeWindow(1280, 900);
    await I.relogin('publishNotification', false, false);
    await I.waitForVisible('#deviceCode', 15);
    await I.seeInCurrentUrl('/admin/logon/device/');
    await I.see(i18n.get('Verify blocked device'));
    Document.screenshot('/redactor/admin/logon-device-verification.png');
    const access = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/security/login-events/1');
        return { status: response.status, redirected: response.redirected, path: new URL(response.url).pathname };
    });
    await I.assertTrue(access.status === 401 || access.status === 403 || (access.redirected && access.path === '/admin/logon/device/'),
        'Pending identity must be rejected or redirected to verification instead of accessing administration REST.');
    await I.amOnPage('/admin/v9/');
    await I.waitForVisible('#deviceCode', 15);
    await I.openNewTab();
    const blockedEmailIntro = i18n.get('someone is trying to sign in to your account from a browser you blocked:');
    const body = await openDeviceEmail(I, TempMail, eventId, blockedEmailIntro);
    await I.see(blockedEmailIntro, TempMail.getContentSelector());
    const code = body.match(/(?:^|[^0-9])([0-9]{6})(?![0-9])/)?.[1];
    await I.assertTrue(Boolean(code), 'The unblock email must contain a six-digit code.');
    Document.screenshotElement(TempMail.getContentSelector(), '/redactor/admin/logon-device-verification-email.png', 1000, 760);
    await I.closeCurrentTab();
    await I.fillField('#deviceCode', code === '000000' ? '000001' : '000000');
    await I.clickCss('#deviceVerificationForm [value="verify"]');
    await I.waitForText(i18n.get('The code could not be verified. Check it or request a new one. It expires in 10 minutes and allows up to 5 attempts.'), 10, '[role="alert"]');
    Document.screenshotElement('[role="alert"]', '/redactor/admin/logon-device-verification-error.png');
    await I.fillField('#deviceCode', code);
    await I.seeInField('#deviceCode', code);
    await I.clickCss('#deviceVerificationForm [value="verify"]');
    await I.waitForElement(dashboard, 30);
    const confirmed = await I.executeScript(readDashboardBootstrap, `?securityEvent=${eventId}`);
    await I.assertTrue(confirmed.requestedSecurityEvent.confirmedAt > 0, 'Unblocking must confirm the device.');
    await I.assertEqual(confirmed.requestedSecurityEvent.reportedAt, null, 'Unblocking must clear the device block.');
    await I.wjSetDefaultWindowSize();
    await I.relogin('publishNotification');
    await I.waitForElement(dashboard, 30);
    await I.dontSeeElement('#deviceCode');
});


Scenario('My devices dialog on the full administration screen @screenshot @current', async ({ I, Document }) => {
    await I.relogin("admin");
    await I.waitForElement(dashboard, 30);
    const modal = '.md-dashboard-modal--sessions';
    const devices = `${modal} .md-dashboard-sessions__devices`;
    // Open the shared dialog independently of the user's dashboard layout.
    await I.executeScript(() => document.querySelector('webjet-overview-dashboard').noticeController.openSessions());
    await I.waitForVisible(modal, 10);
    await I.waitForFunction(() => {
        const modal = document.querySelector('.md-dashboard-modal--sessions');
        return modal && getComputedStyle(modal).opacity === '1' && getComputedStyle(modal.querySelector('.modal-dialog')).transform === 'none';
    }, 10);
    await I.clickCss(`${modal} .md-dashboard-sessions__tabs [id$="-devices"]`);
    await I.waitForElement(`${devices}[aria-busy="false"]`, 10);
    await I.seeElement(`${devices} .md-dashboard-sessions__current`);
    await I.dontSeeElement(`${devices} [role="alert"]`);
    if (Document.isScreenshotsEnabled()) {
        // Exclude automated browsers from documentation without changing stored devices.
        await I.executeScript(selector => {
            const panel = document.querySelector(selector);
            let removed = 0;
            panel.querySelectorAll('tr[data-device-id]').forEach(row => {
                if (!row.querySelector('.md-dashboard-sessions__device-name').textContent.includes('HeadlessChrome')) return;
                if (row.nextElementSibling?.classList.contains('md-dashboard-devices__confirmation-row')) row.nextElementSibling.remove();
                row.remove();
                removed++;
            });
            const tab = document.getElementById(panel.getAttribute('aria-labelledby'));
            tab.textContent = tab.textContent.replace(/\((\d+)\)$/, (_, count) => `(${Number(count) - removed})`);
        }, devices);
    }
    Document.screenshot('/redactor/admin/device-my-devices.png');
    await I.clickCss(`${modal} .modal-footer > button`);
    await I.waitForDetached(modal, 10);
});
