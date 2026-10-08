const { mockDashboardBootstrap, dashboardPageRoute, showWidget, readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.sessions').tag('@singlethread');

const modal = '.md-dashboard-modal--sessions';
let permissionsChanged = false;
const administratorsRoute = '**/admin/rest/sessions/administrators';
const historyRoute = '**/admin/rest/sessions/login-history*';
const logoutRoute = '**/admin/rest/sessions/logout';
const adminLogoutRoute = '**/admin/rest/sessions/logout-administrator';
const devicesRoute = '**/admin/rest/security/login-events?page=*';
const reportRoute = '**/admin/rest/security/login-events/*/report';
const confirmRoute = '**/admin/rest/security/login-events/*/confirm';
const codeRoute = '**/admin/rest/security/login-events/*/code';

function waitForSessionDialog(I) {
    I.waitForVisible(modal, 10);
    I.waitForFunction(() => {
        const root = document.querySelector('.md-dashboard-modal--sessions');
        return root && getComputedStyle(root).opacity === '1' && getComputedStyle(root.querySelector('.modal-dialog')).transform === 'none';
    }, 10);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});

After(async ({ I }) => {
    await I.stopMockingRoute(reportRoute);
    await I.stopMockingRoute(devicesRoute);
    await I.stopMockingRoute(confirmRoute);
    await I.stopMockingRoute(codeRoute);
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(historyRoute);
    await I.stopMockingRoute(logoutRoute);
    await I.stopMockingRoute(adminLogoutRoute);
    await I.stopMockingRoute(administratorsRoute);
    I.wjSetDefaultWindowSize();
    if (permissionsChanged) { I.logout(); permissionsChanged = false; }
});

/** Real bootstrap and audit data expose activity, own records and the same authorized administrators. */
Scenario('Real sessions and personal login history are available from the welcome panel', async ({ I }) => {
    I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    I.waitForText('Aktívne teraz', 10, modal);
    const bootstrap = await I.executeScript(readDashboardBootstrap);
    I.assertTrue(bootstrap.currentSessions.userSessions.some(cluster => cluster.userSessions.some(session => session.lastActivity > 0)),
        'The real session API must include the last activity timestamp.');
    const current = bootstrap.currentSessions.userSessions.flatMap(cluster => cluster.userSessions).find(session => session.sessionId === bootstrap.currentSessions.currentSessionId);
    I.assertTrue(Boolean(current?.operatingSystem) && !/\d/.test(current.browserName), 'New sessions must contain a browser family without its version and a separate operating system.');
    I.see(`${current.browserName} · ${current.operatingSystem}`, `${modal} .md-dashboard-sessions__mine`);
    I.assertTrue(!Object.hasOwn(bootstrap, 'loggedAdmins'), 'Administrator summaries must not be injected into the page.');
    const administrators = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/sessions/administrators', { headers: { 'X-CSRF-Token': window.csrfToken } });
        return response.ok ? response.json() : null;
    });
    I.assertTrue(Array.isArray(administrators), 'The real session REST endpoint must return authorized administrator DTOs.');
    const protectedResult = await I.executeScript(async sessionId => {
        const response = await fetch('/admin/rest/sessions/logout', {
            method: 'POST', headers: { 'X-CSRF-Token': window.csrfToken }, body: new URLSearchParams({ sessionId })
        });
        return { ok: response.ok, ...await response.json() };
    }, bootstrap.currentSessions.currentSessionId);
    I.assertTrue(protectedResult.ok && protectedResult.success === false && protectedResult.pending === false,
        'The session controller must reject the requesting session without invalidation.');
    if (administrators) {
        I.clickCss(`${modal} [role="tab"][id$="-admins"]`);
        I.waitForElement(`${modal} .md-dashboard-sessions__admins[aria-busy="false"]`, 10);
        I.assertEqual(await I.grabNumberOfVisibleElements(`${modal} .md-dashboard-sessions__admins-table tbody tr`), administrators.length,
            'The dialog must reuse the logged-administrator widget source.');
        I.see('Aktívne relácie', `${modal} .md-dashboard-sessions__admins`);
        I.saveScreenshot('dashboard-active-admins-real.png');
    }
    I.click('História (30 dní)', modal);
    I.waitForElement(`${modal} .md-dashboard-sessions__history[aria-busy="false"]`, 20);
    I.dontSeeElement(`${modal} .md-dashboard-sessions__history .text-danger`);
    const history = await I.executeScript(async () => {
        const result = await fetch('/admin/rest/sessions/login-history?userId=1&logType=20&days=365&size=999', { headers: { 'X-CSRF-Token': window.csrfToken } });
        if (!result.ok) throw new Error('Personal login history: HTTP ' + result.status);
        const body = await result.json();
        return { ok: result.ok, items: body.content, size: body.size };
    });
    const ownId = await I.executeScript(() => window.currentUser.userId);
    I.assertTrue(history.ok, 'Personal history must not require a request to the privileged audit endpoint.');
    I.assertEqual(history.size, 20);
    I.assertTrue(history.items.length > 0, 'The current login must provide a record for the ownership assertions.');
    I.assertTrue(history.items.every(item => item.userId === ownId && item.logType === 80 && new Date(item.createDate).getTime() >= Date.now() - 30 * 86400000),
        'Client-supplied audit filters must not broaden ownership, type or period.');
    I.saveScreenshot('dashboard-sessions-real-history.png');
    I.click('Zavrieť', `${modal} .modal-footer`);
    I.waitToHide(modal, 10);
    I.waitForFunction(() => document.activeElement.matches('[data-widget-type="sessions"] .md-dashboard__title-action'), 10);
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('[data-widget-type="sessions"] .md-dashboard__title-action')),
        'Closing must restore focus to the invoking heading.');
});

/** Browser fixtures exercise all sizes, tab keyboard access, responsive layout and coordinated logout updates. */
Scenario('Session widgets and notices open the dialog and update after individual and bulk logout', async ({ I }) => {
    const removed = [];
    const now = Date.now();
    const data = {
        settings: { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true,
            items: [{ id: 'sessions-autotest-fixed', type: 'sessions', size: '2x3', options: {} },
                ...['1x1', '2x2', '2x3'].map(size => ({ id: `sessions-autotest-${size}`, type: 'my-sessions', size, options: {} }))], domainOptions: {} },
        notices: [],
        currentSessions: { currentSessionId: 'sessions-autotest-current', userSessions: [{ cluster: 'autotest-node', userSessions: [
            { sessionId: 'sessions-autotest-current', logonTime: now - 3600000, lastActivity: now, browserName: 'Chrome', operatingSystem: 'macOS', remoteAddr: '127.0.0.1' },
            { sessionId: 'sessions-autotest-other', logonTime: now - 7200000, deviceId: 43, deviceConfirmed: false, lastActivity: now - 12 * 60000, browserName: 'Firefox', operatingSystem: 'Windows', remoteAddr: '192.0.2.2' },
            { sessionId: 'sessions-autotest-third', logonTime: now - 86400000, lastActivity: now - 2 * 3600000, browserName: 'Safari', operatingSystem: 'iOS', remoteAddr: '192.0.2.3' }
        ] }] }
    };
    await mockDashboardBootstrap(I, () => data, () => ({ dismissedUntil: {} }));
    let administratorReads = 0;
    await I.mockRoute(administratorsRoute, route => {
        administratorReads++;
        return route.fulfill({ contentType: 'application/json', body: '[]' });
    });
    await I.mockRoute(logoutRoute, route => {
        removed.push(new URLSearchParams(route.request().postData()).get('sessionId'));
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify({ success: true, pending: false }) });
    });
    await I.mockRoute(historyRoute, route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({
        content: [], totalElements: 0, totalPages: 0, first: true, last: true
    }) }));
    I.refreshPage();
    I.waitForElement('[data-notice-id="multipleSessions"]', 10);
    for (const size of ['1x1', '2x2', '2x3']) {
        await showWidget(I, `sessions-autotest-${size}`);
        if (size === '1x1') {
            I.see('3', '[data-instance-id="sessions-autotest-1x1"] .md-dashboard-widget__metric');
            I.assertTrue(await I.executeScript(() => parseFloat(getComputedStyle(document.querySelector('[data-instance-id="sessions-autotest-1x1"] .md-dashboard-widget__metric')).fontSize) >= 30),
                'The compact count must use the dashboard metric typography.');
        }
        else I.assertEqual(await I.grabNumberOfVisibleElements(`[data-instance-id="sessions-autotest-${size}"] li`), 3);
    }
    I.seeElement('[data-instance-id="sessions-autotest-2x3"] .md-dashboard__title-action .ti-arrow-up-right');
    I.see('Nepotvrdené', '[data-instance-id="sessions-autotest-2x3"] .is-unconfirmed');
    I.clickCss('[data-instance-id="sessions-autotest-2x3"] .md-dashboard-widget__session-bulk');
    waitForSessionDialog(I);
    I.waitForVisible('.md-dashboard-modal--logout', 10);
    I.assertDeepEqual(removed, [], 'The widget must open confirmation before any logout.');
    I.click('Zrušiť', `${modal} .modal-footer`);
    I.waitForText('Moje prihlásenia (3)', 10, modal);
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard-sessions__summary button')),
        'Canceling widget bulk logout must focus the session action inside the dialog.');
    I.click('Zavrieť', `${modal} .modal-footer`);
    I.waitForDetached(modal, 10);
    I.saveScreenshot('dashboard-sessions-widgets.png');
    const widgetViolations = await I.runA11yCheck({ context: { include: ['[data-widget-type="my-sessions"]'] } });
    I.assertDeepEqual(widgetViolations.map(item => item.id), [], 'Session widget text, status and controls must pass the accessibility audit.');
    for (const width of [1100, 390]) {
        await I.resizeWindow(width, 850);
        await showWidget(I, 'sessions-autotest-2x3');
        await I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('[data-widget-type="my-sessions"]')].every(card => {
            const list = card.querySelector('.md-dashboard-widget__sessions');
            return card.scrollWidth <= card.clientWidth + 1 && (!list || list.clientHeight > 60);
        })), `Session lists must stay visible and fit their ${width}px layout.`);
    }
    await I.saveScreenshot('dashboard-session-widgets-mobile.png');
    await I.wjSetDefaultWindowSize();
    await showWidget(I, 'sessions-autotest-2x3');

    I.clickCss('[data-instance-id="sessions-autotest-2x3"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    I.see('Moje prihlásenia (3)', modal);
    I.see('Chrome · macOS', modal);
    I.see('pred 12 minútami', modal);
    I.saveScreenshot('dashboard-active-sessions-desktop.png');
    for (const width of [1100, 390]) {
        I.resizeWindow(width, 850);
        I.assertTrue(await I.executeScript(() => {
            const dialog = document.querySelector('.md-dashboard-modal--sessions .modal-content');
            const body = dialog.querySelector('.modal-body');
            const bounds = dialog.getBoundingClientRect();
            return bounds.left >= 0 && bounds.right <= innerWidth && body.scrollWidth <= body.clientWidth + 1;
        }), `The session dialog must fit the ${width}px viewport without horizontal scrolling.`);
    }
    I.saveScreenshot('dashboard-active-sessions-mobile.png');
    I.click('Odhlásiť všetky ostatné (2)', modal);
    I.waitForText('Odhlásiť všetky ostatné zariadenia?', 10, `${modal} .modal-title`);
    I.see('Odhlásime 2 zariadenia. Na tomto zariadení zostanete prihlásený.', modal);
    I.see('Firefox · Windows · 192.0.2.2', `${modal} .md-dashboard-sessions__logout-list`);
    I.see('Safari · iOS · 192.0.2.3', `${modal} .md-dashboard-sessions__logout-list`);
    I.dontSee('Chrome', `${modal} .md-dashboard-sessions__logout-list`);
    I.assertDeepEqual(removed, [], 'Opening confirmation must not submit any logout request.');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard-modal--logout .modal-footer .btn-outline-secondary')),
        'Confirmation must initially focus the cancel action.');
    I.assertTrue(await I.executeScript(() => {
        const body = document.querySelector('.md-dashboard-modal--logout .modal-body');
        const bounds = body.getBoundingClientRect();
        return bounds.left >= 0 && bounds.right <= innerWidth && body.scrollWidth <= body.clientWidth + 1;
    }), 'Bulk confirmation must fit a mobile viewport without horizontal scrolling.');
    I.saveScreenshot('dashboard-sessions-logout-confirm-mobile.png');
    I.click('Zrušiť', `${modal} .modal-footer`);
    I.waitForText('Moje prihlásenia (3)', 10, modal);
    I.assertDeepEqual(removed, [], 'Canceling confirmation must preserve every session.');
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('.md-dashboard-sessions__summary button')),
        'Canceling must restore focus to the bulk logout action.');
    I.wjSetDefaultWindowSize();
    I.click('Odhlásiť všetky ostatné (2)', modal);
    I.saveScreenshot('dashboard-sessions-logout-confirm-desktop.png');
    I.pressKey('Escape');
    I.waitForText('Moje prihlásenia (3)', 10, modal);
    I.assertDeepEqual(removed, [], 'Escape must cancel without logging out any session.');
    I.clickCss(`${modal} tbody tr:nth-child(2) button`);
    I.waitForText('Moje prihlásenia (2)', 10, modal);
    I.click('Odhlásiť všetky ostatné (1)', modal);
    I.waitForText('Odhlásime 1 zariadenie. Na tomto zariadení zostanete prihlásený.', 10, modal);
    I.click('Odhlásiť 1 zariadenie', `${modal} .modal-footer`);
    I.waitForText('Moje prihlásenia (1)', 10, modal);
    I.waitForText('Aktívne prihlásenia', 10, '#toast-container-webjet .toast-success .toast-title');
    I.see('Ostatné zariadenia sú odhlásené (1).', '#toast-container-webjet .toast-success');
    I.assertDeepEqual(removed, ['sessions-autotest-other', 'sessions-autotest-third'], 'Bulk logout must never submit the current session.');
    I.click('Zavrieť', `${modal} .modal-footer`);
    I.waitToHide(modal, 10);
    I.dontSeeElement('[data-notice-id="multipleSessions"]');
    await showWidget(I, 'sessions-autotest-1x1');
    I.see('Ste prihlásený v jednej relácii', '[data-instance-id="sessions-autotest-1x1"] .md-dashboard-widget__session-only');
    I.saveScreenshot('dashboard-session-only-widget.png');
    I.refreshPage();
    I.waitForElement('[data-notice-id="multipleSessions"]', 10);
    I.click('Aktívne prihlásenia', '[data-notice-id="multipleSessions"]');
    waitForSessionDialog(I);
    I.click('História (30 dní)', modal);
    I.waitForText('Za posledných 30 dní nemáte žiadne záznamy prihlásení.', 10, modal);
    I.pressKey('Escape');
    I.waitToHide(modal, 10);
    I.assertEqual(administratorReads, 0, 'A personal-only dashboard and inactive administrator tab must not load administrator data.');
});

/** Owned records and all writes are mocked before any security action is activated. */
Scenario('Device tab separates retained browsers from sessions and supports confirmation and blocking', async ({ I }) => {
    const now = Date.now();
    const records = [
        { id: 42, browserName: 'Chrome', browserVersion: '131', operatingSystem: 'macOS', ipAddress: '192.0.2.1', createDate: now - 86400000, lastSeen: now },
        { id: 43, browserName: 'Firefox autotest', browserVersion: '131', operatingSystem: 'Windows 11', ipAddress: '192.0.2.2', createDate: now - 172800000, lastSeen: now - 3600000 },
        { id: 45, browserName: 'Safari autotest', browserVersion: '18', operatingSystem: 'iOS', ipAddress: '192.0.2.3', createDate: now - 172800000, lastSeen: now - 86400000, confirmedAt: now - 86400000 },
        { id: 46, browserName: 'Edge autotest', operatingSystem: 'Windows', ipAddress: '192.0.2.4', createDate: now - 259200000, lastSeen: now - 172800000, reportedAt: now - 86400000 }
    ];
    const data = {
        notices: [{ id: 'newDevice:43', kind: 'newDevice', severity: 'warning', title: 'New device autotest', securityEvent: records[1] }],
        currentSessions: { currentSessionId: 'devices-current', userSessions: [{ userSessions: [
            { sessionId: 'devices-current', browserName: 'Chrome', operatingSystem: 'macOS', logonTime: now, deviceId: 42, deviceConfirmed: false },
            { sessionId: 'devices-same-browser', browserName: 'Chrome', operatingSystem: 'macOS', logonTime: now - 1000, deviceId: 42, deviceConfirmed: false },
            { sessionId: 'devices-other', browserName: 'Firefox', operatingSystem: 'Windows', logonTime: now - 2000, deviceId: 43, deviceConfirmed: false }
        ] }] }
    };
    let reads = 0, failLoad = true, failConfirm = true;
    const confirmed = [], blocked = [];
    await mockDashboardBootstrap(I, () => data, () => ({ dismissedUntil: {} }));
    await I.mockRoute(devicesRoute, route => {
        reads++;
        return route.fulfill({ status: failLoad ? 503 : 200, contentType: 'application/json', body: JSON.stringify(failLoad ? {} : {
            content: records, totalElements: records.length, number: 0, totalPages: records.length ? 1 : 0, first: true, last: true
        }) });
    });
    await I.mockRoute(codeRoute, route => route.fulfill({ status: 204, body: '' }));
    await I.mockRoute(confirmRoute, route => {
        confirmed.push(route.request().postDataJSON());
        if (!failConfirm) { records[1].confirmedAt = Date.now(); data.notices = []; }
        return route.fulfill({ status: failConfirm ? 503 : 200, contentType: 'application/json', body: JSON.stringify(failConfirm ? {} : records[1]) });
    });
    await I.mockRoute(reportRoute, route => {
        const id = Number(route.request().url().match(/login-events\/(\d+)\/report/)[1]);
        blocked.push(id);
        const device = records.find(device => device.id === id);
        device.reportedAt = Date.now(); device.confirmedAt = null;
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify(device) });
    });
    await I.refreshPage();
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    const devices = `${modal} .md-dashboard-sessions__devices`;
    await I.see('Moje prihlásenia (3)', modal);
    await I.dontSeeElement(`${modal} .md-dashboard-sessions__mine .md-dashboard-sessions__confirm-device`);
    await I.dontSeeElement(`${modal} .md-dashboard-sessions__mine .md-dashboard-sessions__deny-device`);
    await I.assertEqual(reads, 0, 'Session views must not load device records.');
    await I.click('Moje zariadenia', modal);
    await I.waitForVisible(`${devices} [role="alert"]`, 10);
    failLoad = false;
    await I.clickCss(`${devices} button`);
    await I.waitForText('Moje zariadenia (4)', 10, modal);
    await I.assertEqual(await I.grabNumberOfVisibleElements(`${devices} tbody tr`), 4);
    await I.see('Toto zariadenie', `${devices} [data-device-id="42"]`);
    await I.see('Potvrdené', `${devices} [data-device-id="45"]`);
    await I.dontSeeElement(`${devices} [data-device-id="45"] .md-dashboard-sessions__confirm-device`);
    await I.seeElement(`${devices} [data-device-id="45"] .md-dashboard-sessions__deny-device`);
    await I.see('Zablokované', `${devices} [data-device-id="46"]`);
    await I.dontSeeElement(`${devices} [data-device-id="46"] button`);
    await I.saveScreenshot('dashboard-my-devices-desktop.png');
    await I.resizeWindow(390, 850);
    await I.assertTrue(await I.executeScript(() => {
        const content = document.querySelector('.md-dashboard-modal--sessions .modal-content');
        const bounds = content.getBoundingClientRect();
        const body = content.querySelector('.modal-body');
        return bounds.left >= 0 && bounds.right <= innerWidth && body.scrollWidth <= body.clientWidth + 1;
    }), 'The device table must fit a mobile viewport.');
    await I.saveScreenshot('dashboard-my-devices-mobile.png');
    for (const width of [320, 768]) {
        await I.resizeWindow(width, 850);
        await I.assertTrue(await I.executeScript(() => {
            const body = document.querySelector('.md-dashboard-modal--sessions .modal-body');
            return body.scrollWidth <= body.clientWidth + 1;
        }), `The device table must fit the ${width}px viewport.`);
    }
    await I.resizeWindow(390, 850);
    await I.clickCss(`${devices} [data-device-id="43"] .md-dashboard-sessions__confirm-device`);
    await I.waitForText('Zadajte 6-miestny kód', 10, devices);
    await I.assertTrue(await I.executeScript(() => {
        const body = document.querySelector('.md-dashboard-modal--sessions .modal-body');
        return body.scrollWidth <= body.clientWidth + 1;
    }), 'Device confirmation must fit on mobile.');
    await I.fillField(`${devices} input[name="deviceConfirmationCode"]`, '012345');
    await I.clickCss(`${devices} .md-dashboard-device-confirmation [type="submit"]`);
    await I.waitForText('Kód sa nepodarilo overiť', 10, devices);
    failConfirm = false;
    await I.clickCss(`${devices} .md-dashboard-device-confirmation [type="submit"]`);
    await I.waitForText('Potvrdené', 10, `${devices} [data-device-id="43"]`);
    await I.dontSeeElement('[data-notice-id="newDevice:43"]');
    await I.clickCss(`${devices} [data-device-id="45"] .md-dashboard-sessions__deny-device`);
    const securityDialog = '.md-dashboard-modal--device-security';
    await I.waitForText('Zariadenie je zablokované', 10, securityDialog);
    await I.dontSeeElement(modal);
    await I.see('Safari autotest 18 · iOS · 192.0.2.3 sme zablokovali.', securityDialog);
    await I.click('Neskôr', securityDialog);
    await I.waitForDetached(securityDialog, 10);
    await I.wjSetDefaultWindowSize();
    await I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    await I.see('Moje prihlásenia (3)', modal);
    await I.click('Moje zariadenia', modal);
    await I.waitForText('Zablokované', 10, `${devices} [data-device-id="45"]`);
    await I.dontSeeElement(`${devices} [data-device-id="45"] button`);
    await I.assertDeepEqual(confirmed, [{ code: '012345' }, { code: '012345' }]);
    await I.assertDeepEqual(blocked, [45], 'Only the selected retained device may be blocked.');
    records.splice(0);
    await I.clickCss(`${devices} .md-dashboard-sessions__summary button`);
    await I.waitForText('Nemáte žiadne zaznamenané zariadenia.', 10, devices);
    await I.click('Zavrieť', `${modal} .modal-footer`);
    await I.waitForDetached(modal, 10);
});

/** Reads the actual owned list without modifying devices or sessions. */
Scenario('Device REST listing exposes owned records with bounded pagination and no secrets', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/security/login-events?page=-1&userId=999&size=999&sort=userId', { headers: { 'X-CSRF-Token': window.csrfToken } });
        return { status: response.status, data: await response.json() };
    });
    await I.assertEqual(result.status, 200);
    await I.assertEqual(result.data.size, 20);
    await I.assertEqual(result.data.number, 0);
    await I.assertTrue(result.data.content.every(device => device.id > 0 && device.lastSeen > 0));
    await I.assertTrue(result.data.content.every(device => !['userId', 'tokenHash', 'confirmationHash', 'codeHash', 'codeAttempts'].some(key => Object.hasOwn(device, key))));
    await I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    await I.click('Moje zariadenia', modal);
    await I.waitForElement(`${modal} .md-dashboard-sessions__devices[aria-busy="false"]`, 10);
    await I.dontSeeElement(`${modal} .md-dashboard-sessions__devices [role="alert"]`);
    await I.click('Zavrieť', `${modal} .modal-footer`);
});

/** Every administrator logout is intercepted before interaction; no real administrator sessions are invalidated. */
Scenario('Administrator session summaries match the design and coordinate authorized logout', async ({ I }) => {
    const removed = [];
    const now = Date.now();
    const data = {
        settings: { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true,
            items: ['2x2', '2x3'].map(size => ({ id: `admins-autotest-${size}`, type: 'logged-admins', size, options: {} })), domainOptions: {} },
        notices: [],
        currentSessions: { currentSessionId: 'admins-autotest-current', userSessions: [{ userSessions: [
            { sessionId: 'admins-autotest-current', browserName: 'Chrome', operatingSystem: 'macOS', logonTime: now, lastActivity: now }
        ] }] },
        administrators: [
            { userId: 900001, fullName: 'Autotest Current User', login: 'autotest-current', current: true, sessionCount: 1, lastActivity: now, clients: ['Chrome · macOS'] },
            { userId: 900002, fullName: 'Autotest Other Administrator', login: 'autotest-other', email: 'autotest@example.test', sessionCount: 2, lastActivity: now - 300000, clients: ['Chrome · Windows', 'Safari · iOS'] },
            { userId: 900003, fullName: 'Autotest Remote Administrator', login: 'autotest-remote', sessionCount: 1, lastActivity: now - 2400000, clients: ['Firefox · Linux'] }
        ]
    };
    let administrators = data.administrators;
    delete data.administrators;
    let reads = 0;
    await mockDashboardBootstrap(I, () => data, () => ({ dismissedUntil: {} }));
    await I.mockRoute(administratorsRoute, route => {
        reads++;
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify(administrators) });
    });
    await I.mockRoute(adminLogoutRoute, route => {
        const userId = new URLSearchParams(route.request().postData()).get('userId');
        removed.push(userId);
        if (userId === '900002') administrators = administrators.filter(user => user.userId !== 900002);
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify({ success: true, pending: userId === '900003' }) });
    });
    I.refreshPage();
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.assertTrue(await I.executeScript(() => WJ.hasPermission('users.edit_admins')), 'This fixture account must have administrator management permission.');
    for (const size of ['2x2', '2x3']) {
        await showWidget(I, `admins-autotest-${size}`);
        await I.see('4', `[data-instance-id="admins-autotest-${size}"] .md-dashboard-widget__admin-total`);
        await I.see('Prihlásených administrátorov: 3', `[data-instance-id="admins-autotest-${size}"] .md-dashboard-widget__admin-summary`);
        await I.seeElement(`[data-instance-id="admins-autotest-${size}"] .md-dashboard__title-action .ti-arrow-up-right`);
    }
    await I.saveScreenshot('dashboard-admin-widgets.png');
    const widgetViolations = await I.runA11yCheck({ context: { include: ['[data-widget-type="logged-admins"]'] } });
    await I.assertDeepEqual(widgetViolations.map(item => item.id), [], 'Administrator summaries and title controls must pass the accessibility audit.');
    for (const width of [1100, 390]) {
        await I.resizeWindow(width, 850);
        await showWidget(I, 'admins-autotest-2x3');
        await I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('[data-widget-type="logged-admins"]')].every(card =>
            card.scrollWidth <= card.clientWidth + 1)), `Administrator widgets must fit their ${width}px layout.`);
    }
    await I.saveScreenshot('dashboard-admin-widgets-mobile.png');
    await I.wjSetDefaultWindowSize();
    await showWidget(I, 'admins-autotest-2x3');

    await I.clickCss('[data-instance-id="admins-autotest-2x3"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    await I.seeElement(`${modal} [role="tab"][id$="-admins"][aria-selected="true"]`);
    I.waitForText('Prihlásení administrátori (3)', 10, modal);
    I.see('autotest-current', `${modal} [data-admin-user-id="900001"]`);
    I.see('Chrome · Windows, Safari · iOS', modal);
    I.dontSee('Nové zariadenie', modal);
    I.dontSee('Zobraziť automatizovaných klientov', modal);
    I.assertTrue(await I.executeScript(() => getComputedStyle(document.querySelector('.md-dashboard-sessions__avatar')).backgroundColor !== 'rgba(0, 0, 0, 0)'),
        'Administrator initials must have their visible themed avatar background.');
    I.saveScreenshot('dashboard-active-admins-desktop.png');
    administrators[1].sessionCount = 3;
    I.click('Obnoviť údaje', `${modal} .md-dashboard-sessions__admins`);
    I.waitForText('3', 10, `${modal} [data-admin-user-id="900002"] .md-dashboard-sessions__admin-connections > div`);
    for (const width of [1100, 390]) {
        I.resizeWindow(width, 850);
        I.assertTrue(await I.executeScript(() => {
            const dialog = document.querySelector('.md-dashboard-modal--sessions .modal-content');
            const body = dialog.querySelector('.modal-body');
            const bounds = dialog.getBoundingClientRect();
            return bounds.left >= 0 && bounds.right <= innerWidth && body.scrollWidth <= body.clientWidth + 1;
        }), `The administrator table must fit the ${width}px viewport.`);
    }
    I.saveScreenshot('dashboard-active-admins-mobile.png');
    I.wjSetDefaultWindowSize();
    I.click('Moje prihlásenia', `${modal} [data-admin-user-id="900001"]`);
    I.see('Chrome · macOS', `${modal} .md-dashboard-sessions__mine`);
    I.clickCss(`${modal} [role="tab"][id$="-admins"]`);
    I.waitForText('Prihlásení administrátori (3)', 10, modal);
    I.click('Odhlásiť', `${modal} [data-admin-user-id="900002"]`);
    I.waitForText('Prihlásení administrátori (2)', 10, modal);
    I.dontSeeElement(`${modal} [data-admin-user-id="900002"]`);
    I.click('Odhlásiť', `${modal} [data-admin-user-id="900003"]`);
    I.waitForText('Odhlásenie bolo prijaté.', 10, `${modal} [data-admin-user-id="900003"]`);
    I.assertDeepEqual(removed, ['900002', '900003'], 'Only the selected fictional administrators may reach the intercepted route.');
    I.click('Zavrieť', `${modal} .modal-footer`);
    I.waitToHide(modal, 10);
    await showWidget(I, 'admins-autotest-2x2');
    I.dontSee('Autotest Other Administrator', '[data-instance-id="admins-autotest-2x2"]');
    I.see('Autotest Remote Administrator', '[data-instance-id="admins-autotest-2x2"]');
    I.assertTrue(reads >= 4, 'Widget and dialog must read fresh summaries after logout.');
});

/** Both real POSTs target only the requesting account, which the service always refuses to log out. */
Scenario('Administrator list and logout enforce independent permissions through REST', async ({ I }) => {
    permissionsChanged = true;
    I.amOnPage('/admin/v9/?removePerm=welcomeShowLoggedAdmins');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.assertTrue(await I.executeScript(() => WJ.hasPermission('users.edit_admins')), 'List revocation must retain administrator management.');
    let result = await I.executeScript(async () => {
        const list = await fetch('/admin/rest/sessions/administrators', { headers: { 'X-CSRF-Token': window.csrfToken } });
        const logout = await fetch('/admin/rest/sessions/logout-administrator', {
            method: 'POST', headers: { 'X-CSRF-Token': window.csrfToken }, body: new URLSearchParams({ userId: window.currentUser.userId })
        });
        return { list: list.status, logout: logout.status };
    });
    I.assertDeepEqual(result, { list: 403, logout: 400 }, 'Management alone must not expose the list; an own-account logout must still be rejected.');
    I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    I.dontSeeElement(`${modal} .md-dashboard-sessions__admins`);
    I.click('Zavrieť', `${modal} .modal-footer`);
    I.logout();
    I.relogin('admin', false);
    I.amOnPage('/admin/v9/?removePerm=users.edit_admins');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.assertTrue(await I.executeScript(() => WJ.hasPermission('welcomeShowLoggedAdmins')), 'Management revocation must retain list permission.');
    result = await I.executeScript(async () => {
        const list = await fetch('/admin/rest/sessions/administrators', { headers: { 'X-CSRF-Token': window.csrfToken } });
        const logout = await fetch('/admin/rest/sessions/logout-administrator', {
            method: 'POST', headers: { 'X-CSRF-Token': window.csrfToken }, body: new URLSearchParams({ userId: window.currentUser.userId })
        });
        return { list: list.status, logout: logout.status };
    });
    I.assertDeepEqual(result, { list: 200, logout: 403 }, 'Displaying administrator summaries must not grant logout access.');
});
