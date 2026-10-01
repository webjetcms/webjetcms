const { readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.security').tag('@singlethread');

let originalSettings;
const dashboard = '.md-dashboard[data-loaded="true"]';
const modal = '.md-dashboard-modal';
const permissionCases = [
    { permission: 'cmp_stat', types: ['traffic', 'top-pages', 'search-terms', 'referrers', 'errors'] },
    { permission: 'menuWebpages', types: ['recent-pages', 'approvals', 'publishing', 'changed-pages'] },
    { permission: 'cmp_form', types: ['forms'] },
    { permission: 'menuEmail', types: ['newsletter'] },
    { permission: 'cmp_adminlog', types: ['audit', 'publishing', 'changed-pages'] },
    { permission: 'welcomeShowLoggedAdmins', types: ['logged-admins'] },
    { permission: 'cmp_server_monitoring', types: ['server-memory', 'server-cpu'] }
];
const sizes = { 'recent-pages': '3x2', publishing: '2x2', 'top-pages': '2x3', 'search-terms': '2x3', referrers: '2x2',
    newsletter: '2x2', 'changed-pages': '3x2', audit: '3x2', 'logged-admins': '2x2', 'server-memory': '3x2', 'server-cpu': '3x2' };
function loaded(I) {
    I.waitForElement(dashboard, 20);
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
});

/**
 * Saves the original layout and prepares widgets for every permission being tested. Existing widget
 * identities are retained so their saved options can be restored afterwards.
 */
Scenario('Preserve preferences and install all permission-controlled widgets', async ({ I }) => {
    loaded(I);
    originalSettings = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    const installed = await I.executeScript(async ({ types, sizes }) => {
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        const settings = JSON.parse(JSON.stringify(controller.settings));
        // Retain original instance IDs so saves also preserve their options in other domains.
        for (const type of types) {
            if (!settings.items.some(item => item.type === type)) {
                settings.items.push({ id: `autotest-security-${type}`, type, size: sizes[type] || '1x1', options: {} });
            }
        }
        settings.items.push({ id: 'autotest-security-menu', type: 'shortcut', size: '1x1',
            options: { source: 'menu', href: '/apps/stat/admin/', title: 'autotest statistics' } });
        settings.legacyBookmarksHandled = true;
        return controller._commit(settings);
    }, { types: permissionCases.flatMap(item => item.types), sizes });
    I.assertTrue(installed, 'The permission fixture must be saved before testing revocation.');
});

for (const { permission, types } of permissionCases) {
    /**
     * Checks that revoked permissions hide widgets and catalogue choices while retaining saved preferences.
     * Active-session controls must remain visible.
     */
    Scenario(`${permission}: hide widgets and catalogue entries while retaining preferences`, async ({ I }) => {
        loaded(I);
        I.assertTrue(await I.executeScript(permission => WJ.hasPermission(permission), permission), 'Logout must restore the real account permissions.');
        for (const type of types) I.seeElementInDOM(`${dashboard} [data-widget-type="${type}"]`);
        if (permission === 'welcomeShowLoggedAdmins') {
            I.assertTrue((await I.executeScript(readDashboardBootstrap)).loggedAdmins.length > 0, 'Authorized page data must contain logged-in administrators.');
        }
        I.amOnPage(`/admin/v9/?removePerm=${permission === 'cmp_stat' ? 'cmp_stat,cmp_abtesting' : permission === 'menuWebpages' ? 'menuWebpages,cmp_blog,cmp_blog_admin,cmp_news,cmp_abtesting,cmp_basket' : permission}`);
        loaded(I);
        I.assertFalse(await I.executeScript(permission => WJ.hasPermission(permission), permission));
        if (permission === 'welcomeShowLoggedAdmins') {
            I.assertFalse(Object.prototype.hasOwnProperty.call(await I.executeScript(readDashboardBootstrap), 'loggedAdmins'), 'The server must omit administrator data without permission.');
        }
        for (const type of types) I.dontSeeElementInDOM(`${dashboard} [data-widget-type="${type}"]`);
        I.seeElementInDOM(`${dashboard} [data-widget-type="sessions"]`);
        I.assertTrue(await I.executeScript(types => types.every(type => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.some(item => item.type === type)), types), 'Revoking access must retain the hidden preferences.');

        I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
        I.click('Pridať widget', '.md-dashboard__toolbar-actions');
        I.waitForVisible(modal, 10);
        for (const type of types) I.dontSeeElementInDOM(`${modal} [data-widget-type="${type}"]`);
        I.clickCss(`${modal} .btn-close`);
        I.waitForInvisible(modal, 10);
        if (permission === 'cmp_stat') {
            I.dontSeeElementInDOM('[data-instance-id="autotest-security-menu"] a');
            const menu = await I.executeScript(async () => {
                const response = await fetch('/admin/rest/dashboard/menu', { headers: { 'X-CSRF-Token': window.csrfToken } });
                const paths = [];
                const visit = items => items.forEach(item => { paths.push(item.href); visit(item.childrens || []); });
                visit(await response.json());
                return paths;
            });
            I.assertFalse(menu.includes('/apps/stat/admin/'), 'The shortcut menu must omit unauthorized statistics destinations.');
        }
    });

    // removePerm persists in the session even if the preceding scenario fails.
    /**
     * Signs out after temporarily removing a permission so the next scenario regains the account's normal
     * access, even if the preceding check failed.
     */
    Scenario(`${permission}: logout after removing session permission`, ({ I }) => {
        I.logout();
    });
}

/**
 * Checks that the server refuses unsafe shortcut addresses, icons and colors even when they bypass the
 * settings dialog. Every rejected attempt must leave saved preferences unchanged.
 */
Scenario('Reject unsafe shortcut URLs and appearance values through REST without changing preferences', async ({ I }) => {
    loaded(I);
    const before = (await I.executeScript(readDashboardBootstrap)).settings;
    const results = await I.executeScript(async before => {
        const headers = { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken };
        const path = '/admin/rest/dashboard/settings';
        const invalid = [
            ...['javascript:window.autotestDashboardXss=1', 'JaVaScRiPt:alert(1)', 'java\nscript:alert(1)',
                'data:text/html,<script>alert(1)</script>', 'vbscript:msgbox(1)', '//example.com/autotest', '/\\example.com/autotest',
                'https://user:password@example.com/', 'https:example.com', 'https://example.com/\r\n', 'relative/autotest']
                .map(href => ({ href })),
            { icon: 'ti-link" onmouseover="alert(1)' }, { color: 'url(javascript:alert(1))' },
            { source: 'invalid' }, { href: 123 }, { title: {} }
        ];
        const results = [];
        for (const options of invalid) {
            const settings = JSON.parse(JSON.stringify(before));
            settings.items.push({ id: 'autotest-security-invalid', type: 'shortcut', size: '1x1',
                options: { source: 'url', href: '/admin/v9/', title: 'autotest unsafe shortcut', ...options } });
            const response = await fetch(path, { method: 'PUT', headers, body: JSON.stringify(settings) });
            results.push({ options, status: response.status });
        }
        return results;
    }, before);
    const after = (await I.executeScript(readDashboardBootstrap)).settings;
    for (const { options, status } of results) I.assertEqual(status, 400, `Reject unsafe shortcut options: ${JSON.stringify(options)}`);
    I.assertDeepEqual(after, before, 'Rejected payloads must leave the saved profile unchanged.');
});

/**
 * Checks the real dashboard template after saving titles with markup, script terminators, quotes and
 * Unicode line separators. Local paths must stay on the same site, and executable addresses are rejected.
 */
Scenario('Persisted shortcut titles remain text and local paths cannot become external links', async ({ I }) => {
    loaded(I);
    const title = 'autotest <img src=x onerror="window.autotestDashboardXss=1"><svg onload="window.autotestDashboardXss=1">';
    const scriptTitle = 'autotest </script><script>window.autotestDashboardXss=1</script>"\'\\\u2028\u2029';
    const installed = await I.executeScript(async ({ title, scriptTitle }) => {
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        const settings = JSON.parse(JSON.stringify(controller.settings));
        settings.items.push({ id: 'autotest-security-xss', type: 'shortcut', size: '1x1',
            options: { source: 'url', href: '/admin/v9/?autotest=%22%3E%3Csvg%20onload%3Dalert(1)%3E', title } });
        settings.items.push({ id: 'autotest-security-script', type: 'shortcut', size: '1x1',
            options: { source: 'url', href: '/admin/v9/', title: scriptTitle } });
        for (const [index, href] of ['/.//example.com/autotest', '/%2e//example.com/autotest', '/folder/..//example.com/autotest', '/folder/%2e%2e//example.com/autotest'].entries()) {
            settings.items.push({ id: `autotest-security-path-${index}`, type: 'shortcut', size: '1x1', options: { source: 'url', href, title: 'autotest local link' } });
        }
        return controller._commit(settings);
    }, { title, scriptTitle });
    I.assertTrue(installed);
    I.refreshPage();
    loaded(I);
    I.assertEqual(await I.executeScript(() => document.querySelector('[data-instance-id="autotest-security-script"] a').textContent),
        scriptTitle, 'The server-rendered bootstrap must preserve script terminators, quotes, backslashes and Unicode separators as text.');
    I.dontSeeElementInDOM('[data-instance-id="autotest-security-script"] script');
    I.see(title, '[data-instance-id="autotest-security-xss"]');
    I.dontSeeElementInDOM('[data-instance-id="autotest-security-xss"] img, [data-instance-id="autotest-security-xss"] svg');
    I.assertTrue(await I.executeScript(() => window.autotestDashboardXss === undefined), 'The stored title must not execute markup.');
    I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('[data-instance-id^="autotest-security-path-"] a')].length === 4
        && [...document.querySelectorAll('[data-instance-id^="autotest-security-path-"] a')].every(link => link.origin === location.origin)), 'Dot segments must not turn a local shortcut into a different origin.');

    I.clickCss('.md-dashboard__shortcut-actions button[aria-pressed="false"]');
    I.clickCss('[data-instance-id="autotest-security-xss"] .dropdown > button');
    I.clickCss('[data-instance-id="autotest-security-xss"] [data-dashboard-action="settings"]');
    I.waitForVisible(modal, 10);
    I.seeInField(`${modal} [name="dashboardShortcutTitle"]`, title);
    I.dontSeeElementInDOM(`${modal} .md-dashboard__shortcut-preview img, ${modal} .md-dashboard__shortcut-preview svg`);
    I.fillField(`${modal} [name="dashboardShortcutUrl"]`, 'javascript:window.autotestDashboardXss=1');
    I.clickCss(`${modal} .modal-footer .btn-primary`);
    I.waitForVisible(`${modal} [role="alert"]`, 10);
    I.seeElement(modal);
    I.assertTrue(await I.executeScript(() => window.autotestDashboardXss === undefined));
    I.clickCss(`${modal} .btn-close`);
    I.waitForInvisible(modal, 10);
});

/**
 * Checks that changing user or domain parameters in a request cannot select another account's dashboard or
 * session list. The action for ending other sessions must also refuse to end the current session.
 */
Scenario('Ownership parameters cannot select another dashboard or session owner', async ({ I }) => {
    loaded(I);
    const data = await I.executeScript(readDashboardBootstrap);
    const forged = await I.executeScript(readDashboardBootstrap, '?userId=-1&domainId=-1&domainKey=autotest');
    I.assertDeepEqual(forged.settings, data.settings, 'Settings ownership and domain must come from the session.');
    I.assertDeepEqual(forged.currentSessions, data.currentSessions, 'Session ownership must come from the signed-in account.');
    const result = await I.executeScript(async id => {
        const response = await fetch(`/admin/rest/removeSession?sessionId=${encodeURIComponent(id)}&userId=-1`, {
            method: 'POST', headers: { 'X-CSRF-Token': window.csrfToken }
        });
        return { status: response.status, removal: await response.json() };
    }, data.currentSessions.currentSessionId);
    I.assertEqual(result.status, 200);
    I.assertFalse(result.removal.success, 'The current session must not be removed through the other-session action.');
    I.assertEqual((await I.executeScript(readDashboardBootstrap)).currentSessions.currentSessionId, data.currentSessions.currentSessionId);
});

/**
 * Checks that saving or resetting the dashboard and ending a session require the current request-security
 * token. Missing or invalid tokens must be refused without changing the saved layout.
 */
Scenario('Settings mutations and session removal require a valid CSRF token', async ({ I }) => {
    loaded(I);
    const before = (await I.executeScript(readDashboardBootstrap)).settings;
    const result = await I.executeScript(async before => {
        const path = '/admin/rest/dashboard/settings';
        const requests = [
            { path, method: 'PUT', body: JSON.stringify(before) }, { path, method: 'DELETE' },
            { path: `${path}/reset`, method: 'PUT', body: JSON.stringify(before) },
            { path: '/admin/rest/removeSession?sessionId=autotest-nonexistent-session', method: 'POST' }
        ];
        const statuses = [];
        for (const request of requests) {
            for (const token of [null, 'autotest-invalid-token']) {
                const response = await fetch(request.path, { method: request.method, body: request.body,
                    headers: { 'Content-Type': 'application/json', ...(token ? { 'X-CSRF-Token': token } : {}) } });
                statuses.push({ path: request.path, method: request.method, token, status: response.status });
            }
        }
        return statuses;
    }, before);
    const after = (await I.executeScript(readDashboardBootstrap)).settings;
    // PathFilter rejects with 403; its JSP error forward can return 405 for PUT/DELETE.
    for (const request of result) I.assertTrue([403, 405].includes(request.status), `${request.method} ${request.path} must reject a missing or invalid token.`);
    I.assertDeepEqual(after, before, 'Rejected CSRF requests must not change settings.');
});

/**
 * Restores the layout saved before the security checks and verifies it after reloading the dashboard.
 */
Scenario('Restore preferences after security tests', async ({ I }) => {
    if (!originalSettings) return;
    I.assertTrue(await I.executeScript(async settings => {
        // Restore through REST even if the tested payload prevented the dashboard or shell script from starting.
        const csrfToken = window.csrfToken || document.documentElement.innerHTML.match(/window\.csrfToken\s*=\s*'([^']+)'/)[1];
        const response = await fetch('/admin/rest/dashboard/settings', { method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(settings) });
        return response.ok;
    }, originalSettings));
    I.refreshPage();
    loaded(I);
    I.assertDeepEqual(await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings))),
        { ...originalSettings, configured: true, shortcutsConfigured: true });
});

/**
 * Signs out and checks that the dashboard, its data and its change actions all require login. Requests must
 * be refused or directed to the login page.
 */
Scenario('Unauthenticated requests cannot read dashboard data or mutate preferences', async ({ I }) => {
    I.logout();
    const paths = ['/admin/v9/', '/admin/rest/dashboard/menu'];
    const results = await I.executeScript(async paths => {
        const requests = [...paths.map(path => ({ path, method: 'GET' })),
            { path: '/admin/rest/dashboard/settings', method: 'PUT', body: '{}' },
            { path: '/admin/rest/dashboard/settings', method: 'DELETE' },
            { path: '/admin/rest/dashboard/settings/reset', method: 'PUT', body: '{}' },
            { path: '/admin/rest/removeSession?sessionId=autotest-nonexistent-session', method: 'POST' }];
        const results = [];
        for (const request of requests) {
            const response = await fetch(request.path, { method: request.method, body: request.body, headers: { 'Content-Type': 'application/json' } });
            results.push({ path: request.path, method: request.method, status: response.status, redirected: response.redirected,
                contentType: response.headers.get('content-type') || '', url: response.url });
        }
        return results;
    }, paths);
    for (const result of results) {
        const loginRedirect = result.redirected && new URL(result.url).pathname.startsWith('/admin/logon') && !result.contentType.includes('json');
        const denied = [401, 403].includes(result.status) || (result.method !== 'GET' && result.status === 405);
        I.assertTrue(denied || loginRedirect, `${result.method} ${result.path} must require login.`);
    }
});
