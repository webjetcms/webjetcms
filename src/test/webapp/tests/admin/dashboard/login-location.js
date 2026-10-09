const { readDashboardBootstrap, showWidget } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.login-location').tag('@singlethread');

Scenario('Browser location appears above the backend IP in sessions and devices', async ({ I }) => {
    const provider = /^https:\/\/(ipwho\.is|ipwhois\.pro)\//;
    await I.mockRoute(provider, route => route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ success: true, city: 'Bratislava', country_code: 'SK' })
    }));
    await I.amOnPage('/logoff.do?forward=/admin/logon/');
    const originalCookie = await I.grabCookie('wjdevice');
    const origin = await I.executeScript(() => location.origin);
    try {
        await I.setCookie({ name: 'wjdevice', value: 'autotest-expired', domain: new URL(origin).hostname, path: '/', expires: 1 });
        const userAgent = await I.executeScript(() => navigator.userAgent);
        await I.setPlaywrightRequestHeaders({ 'User-Agent': `${userAgent} WebJET-autotest-login-location` });
        await I.waitForElement('script[data-save-url="/admin/logon/location/"]', 10);
        // Complete the staged hint before submitting, so this also verifies session replacement during login.
        await I.executeScript(() => window.webjetLoginLocationRequest);
        await I.relogin('admin', false);
        await I.waitForElement('.md-dashboard[data-loaded="true"]', 30);
        const data = await I.executeScript(readDashboardBootstrap);
        const current = data.currentSessions.userSessions.flatMap(cluster => cluster.userSessions)
            .find(session => session.sessionId === data.currentSessions.currentSessionId);
        await I.assertEqual(current.location, 'Bratislava, SK');
        await I.assertTrue(current.deviceId > 0, 'The login must record a browser device.');
        const widgetId = await I.grabAttributeFrom('[data-widget-type="sessions"]', 'data-instance-id');
        await showWidget(I, widgetId);
        await I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
        const sessionCell = '.md-dashboard-sessions__mine tr:has(.md-dashboard-sessions__current) .md-dashboard-sessions__ip';
        await I.see('Bratislava, SK', `${sessionCell} > span`);
        await I.see(current.remoteAddr, `${sessionCell} > small.text-muted`);
        await I.clickCss('.md-dashboard-sessions__tabs [id$="-devices"]');
        const deviceCell = `.md-dashboard-sessions__devices tr[data-device-id="${current.deviceId}"] .md-dashboard-sessions__ip`;
        await I.waitForElement(deviceCell, 10);
        await I.see('Bratislava, SK', `${deviceCell} > span`);
        await I.see(current.remoteAddr, `${deviceCell} > small.text-muted`);
    } finally {
        await I.setPlaywrightRequestHeaders({});
        await I.amOnPage('/logoff.do?forward=/');
        if (originalCookie) await I.setCookie(originalCookie);
        else await I.setCookie({ name: 'wjdevice', value: 'autotest-expired', domain: new URL(origin).hostname, path: '/', expires: 1 });
        await I.stopMockingRoute(provider);
    }
});
