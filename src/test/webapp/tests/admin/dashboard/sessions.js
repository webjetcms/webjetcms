const { mockDashboardBootstrap, dashboardPageRoute, showWidget, readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.sessions').tag('@singlethread');

const modal = '.md-dashboard-modal--sessions';
const historyRoute = '**/rest/audit/my-login-history*';
const logoutRoute = '**/admin/rest/removeSession';

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
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(historyRoute);
    await I.stopMockingRoute(logoutRoute);
    I.wjSetDefaultWindowSize();
});

/** Real bootstrap and audit data expose activity, own records and the same authorized administrators. */
Scenario('Real sessions and personal login history are available from the welcome panel', async ({ I }) => {
    I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    I.waitForText('Aktívne teraz', 10, modal);
    const bootstrap = await I.executeScript(readDashboardBootstrap);
    I.assertTrue(bootstrap.currentSessions.userSessions.some(cluster => cluster.userSessions.some(session => session.lastActivity > 0)),
        'The real session API must include the last activity timestamp.');
    if (bootstrap.loggedAdmins) {
        I.clickCss(`${modal} [role="tab"]:nth-child(2)`);
        I.assertEqual(await I.grabNumberOfVisibleElements(`${modal} .md-dashboard-widget__admins > li`), bootstrap.loggedAdmins.length,
            'The dialog must reuse the logged-administrator widget source.');
    }
    I.click('História (30 dní)', modal);
    I.waitForElement(`${modal} .md-dashboard-sessions__history[aria-busy="false"]`, 20);
    I.dontSeeElement(`${modal} .md-dashboard-sessions__history .text-danger`);
    const history = await I.executeScript(async () => {
        const result = await fetch('/rest/audit/my-login-history?userId=1&logType=20&days=365&size=999');
        const body = await result.json();
        return { ok: result.ok, items: body.content, size: body.size };
    });
    const ownId = await I.executeScript(() => window.currentUser.userId);
    I.assertTrue(history.ok, 'Personal history must not require a request to the privileged audit endpoint.');
    I.assertEqual(history.size, 20);
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
            { sessionId: 'sessions-autotest-current', logonTime: now - 3600000, lastActivity: now, browserName: 'Chrome 154', remoteAddr: '127.0.0.1' },
            { sessionId: 'sessions-autotest-other', logonTime: now - 7200000, lastActivity: now - 12 * 60000, browserName: 'Firefox 131', remoteAddr: '192.0.2.2' },
            { sessionId: 'sessions-autotest-third', logonTime: now - 86400000, lastActivity: now - 2 * 3600000, browserName: 'Safari', remoteAddr: '192.0.2.3' }
        ] }] }
    };
    await mockDashboardBootstrap(I, () => data, () => ({ dismissedUntil: {} }));
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
    I.saveScreenshot('dashboard-sessions-widgets.png');
    I.clickCss('[data-instance-id="sessions-autotest-2x3"] .md-dashboard__title-action');
    waitForSessionDialog(I);
    I.see('Moje prihlásenia (3)', modal);
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
    I.wjSetDefaultWindowSize();
    I.clickCss(`${modal} tbody tr:nth-child(2) button`);
    I.waitForText('Moje prihlásenia (2)', 10, modal);
    I.click('Odhlásiť všetky ostatné (1)', modal);
    I.waitForText('Moje prihlásenia (1)', 10, modal);
    I.assertDeepEqual(removed, ['sessions-autotest-other', 'sessions-autotest-third'], 'Bulk logout must never submit the current session.');
    I.click('Zavrieť', `${modal} .modal-footer`);
    I.waitToHide(modal, 10);
    I.dontSeeElement('[data-notice-id="multipleSessions"]');
    await showWidget(I, 'sessions-autotest-1x1');
    I.see('1', '[data-instance-id="sessions-autotest-1x1"] .md-dashboard-widget__metric');
    I.refreshPage();
    I.waitForElement('[data-notice-id="multipleSessions"]', 10);
    I.click('Aktívne prihlásenia', '[data-notice-id="multipleSessions"]');
    waitForSessionDialog(I);
    I.click('História (30 dní)', modal);
    I.waitForText('Za posledných 30 dní nemáte žiadne záznamy prihlásení.', 10, modal);
    I.pressKey('Escape');
    I.waitToHide(modal, 10);
});
