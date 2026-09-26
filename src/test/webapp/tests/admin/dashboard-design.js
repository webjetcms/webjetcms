Feature('admin.dashboard-design').tag('@singlethread');

let originalSettings;
let previewSettings;
let noticesToken;

const settingsRoute = '**/admin/rest/dashboard/settings';
const noticesRoute = '**/admin/rest/dashboard/notices';
const editButton = '.md-dashboard__toolbar-actions > button[aria-pressed]';
const newsToggle = '.md-dashboard-widget__news-toggle';

function waitForOverview(I) {
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    return I.waitForFunction(() => document.querySelector('.md-dashboard__notice-list')?.getAttribute('aria-busy') === 'false'
        && [...document.querySelectorAll('.md-dashboard__widget-body')].every(body => body.getAttribute('aria-busy') === 'false'), 30);
}

function waitForSave(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});

Scenario('Pinned security, independent notices and edit mode keep the dashboard readable', async ({ I }) => {
    await waitForOverview(I);
    I.resizeWindow(1440, 1100);
    I.saveScreenshot('dashboard-implementation-desktop.png', true);
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        scrollbar.setPosition(0, scrollbar.limit.y);
    });
    I.saveScreenshot('dashboard-implementation-widgets.png', true);
    I.executeScript(() => window.scrollbarMain.setPosition(0, 0));
    I.resizeWindow(390, 1052);
    if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
    I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
    I.saveScreenshot('dashboard-implementation-mobile-top.png', false);
    I.saveScreenshot('dashboard-implementation-mobile.png', true);
    I.resizeWindow(1440, 1100);
    originalSettings = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/dashboard/settings', { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        if (!response.ok) throw new Error('The original dashboard preferences must be readable before testing.');
        return response.json();
    });
    // Preference mutations stay in this intercepted fixture and never reach the account's stored profile.
    previewSettings = {
        version: 1, configured: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: 'design-autotest-pages', type: 'recent-pages', size: '3x3', collapsed: false, options: {} },
            { id: 'design-autotest-publishing', type: 'publishing', size: '2x3', collapsed: false, options: {} },
            { id: 'design-autotest-session', type: 'sessions', size: '2x3', collapsed: true, options: {} },
            { id: 'design-autotest-news', type: 'news', size: '3x2', collapsed: true, options: {} },
            { id: 'design-autotest-search', type: 'search', size: 'fullauto', collapsed: true, options: { scope: 'admin' } },
            { id: 'design-autotest-shortcut', type: 'shortcut', size: '1x1', collapsed: false, options: { href: '/admin/v9/webpages/web-pages-list/', title: 'Web pages autotest' } }
        ]
    };
    await I.mockRoute(settingsRoute, route => {
        if (route.request().method() === 'PUT') previewSettings = { ...route.request().postDataJSON(), configured: true };
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(previewSettings) });
    });
    await I.mockRoute(noticesRoute, route => {
        noticesToken = route.request().headers()['x-csrf-token'];
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([
            { id: 'design-autotest-migration', severity: 'warning', icon: 'ti-database', title: 'Database migration autotest', bodyHtml: '<p>Statistics require a conversion autotest.</p>', action: { type: 'link', url: '/admin/v9/', label: 'Migration action autotest' } },
            { id: 'design-autotest-security', severity: 'warning', icon: 'ti-shield-lock', title: 'Account protection autotest', bodyHtml: '<p>Enable a second verification factor autotest.</p>', action: { type: 'popup', url: '/admin/2factorauth.jsp', label: 'Security action autotest' } }
        ]) });
    });
    I.refreshPage();
    await waitForOverview(I);
    I.assertTrue(Boolean(noticesToken), 'The independent system-notice request must include CSRF protection.');
    I.seeElement('.md-dashboard__sessions [data-widget-type="sessions"] .md-dashboard-widget__session');
    I.seeElement('.md-dashboard__sessions .md-dashboard-widget__session-manage');
    I.dontSeeElement('.md-dashboard__layout [data-widget-type="sessions"]');
    I.dontSeeElement('.md-dashboard__sessions .md-dashboard__widget-controls');
    I.dontSeeElement('.md-dashboard__edit-control');
    I.seeElement('.md-dashboard__shortcuts-header button');

    const firstNotice = '[data-notice-id="design-autotest-migration"]';
    const secondNotice = '[data-notice-id="design-autotest-security"]';
    I.see('Database migration autotest', `${firstNotice} summary`);
    I.see('Account protection autotest', `${secondNotice} summary`);
    I.dontSeeElement(`${firstNotice} .md-dashboard__notice-body`);
    I.executeScript(selector => document.querySelector(selector).focus(), `${firstNotice} summary`);
    I.pressKey('Enter');
    I.waitForVisible(`${firstNotice}[open] .md-dashboard__notice-body`, 10);
    I.see('Statistics require a conversion autotest.', firstNotice);
    I.dontSeeElement(`${secondNotice} .md-dashboard__notice-body`);
    I.clickCss(`${secondNotice} summary`);
    I.seeElement(`${firstNotice}[open]`);
    I.seeElement(`${secondNotice}[open]`);
    // Inspect notice actions without executing real migration or account-security operations.
    I.see('Migration action autotest', `${firstNotice} button`);
    I.see('Security action autotest', `${secondNotice} button`);

    I.clickCss(editButton);
    I.waitForElement('.md-dashboard.is-editing', 10);
    I.seeElement('.md-dashboard__toolbar .md-dashboard__edit-control');
    I.seeElement('[data-instance-id="design-autotest-pages"] .md-dashboard__drag');
    I.seeElement('.md-dashboard__sessions .md-dashboard-widget__session');
    const handle = '[data-instance-id="design-autotest-pages"] .md-dashboard__drag';
    I.executeScript(selector => document.querySelector(selector).focus(), handle);
    I.pressKey('Enter');
    I.waitForVisible('.md-dashboard-modal select', 10);
    I.waitForFunction(() => document.querySelector('.md-dashboard-modal')?.contains(document.activeElement), 10);
    I.pressKey('Escape');
    // Bootstrap removes the backdrop before hidden.bs.modal restores the invoking control's focus.
    I.waitForFunction(() => !document.querySelector('.md-dashboard-modal'), 10);
    I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), handle), 'Closing keyboard movement must return focus to its handle.');
    I.clickCss(editButton);
    I.dontSeeElement('.md-dashboard__edit-control');
    I.seeElement(`${firstNotice}[open]`);
    I.clickCss(`${firstNotice} summary`);
    I.clickCss(`${secondNotice} summary`);
    I.saveScreenshot('dashboard-design-desktop.png', true);
});

Scenario('Release notes collapse to a persistent summary and can be expanded again', async ({ I }) => {
    await waitForOverview(I);
    I.seeElement('.md-dashboard-widget__news-highlights');
    I.clickCss(newsToggle);
    waitForSave(I);
    I.waitForVisible('.is-news-collapsed .md-dashboard-widget__news-summary', 10);
    I.dontSeeElement('.md-dashboard-widget__news-highlights');
    I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), newsToggle), 'Collapsing release notes must keep keyboard focus on the replacement toggle.');
    I.refreshPage();
    await waitForOverview(I);
    I.seeElement('.is-news-collapsed .md-dashboard-widget__news-summary');
    I.seeElement('.md-dashboard__sessions .md-dashboard-widget__session');
    I.clickCss(newsToggle);
    waitForSave(I);
    await I.waitForVisible('.md-dashboard-widget__news-highlights', 10);
    I.assertEqual(previewSettings.acknowledgedNewsVersion, null);
    I.assertTrue(previewSettings.items.find(item => item.type === 'sessions').collapsed, 'The fixed security presentation must preserve legacy preferences.');
});

Scenario('Dashboard header, notices, widgets and shortcuts fit the responsive viewport', async ({ I }) => {
    await waitForOverview(I);
    for (const width of [390, 768, 1337]) {
        I.resizeWindow(width, 1052);
        if (width < 768 && await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
        if (width < 768) I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
        const overflow = await I.executeScript(() => {
            const dashboard = document.querySelector('.md-dashboard');
            const boundary = dashboard.getBoundingClientRect();
            return [...dashboard.querySelectorAll('.md-dashboard__hero, .md-dashboard__search, .md-dashboard__notice-list, .md-dashboard__layout, .md-dashboard__shortcuts')]
                .map(element => ({ className: element.className, left: element.getBoundingClientRect().left, right: element.getBoundingClientRect().right, width: element.clientWidth, contentWidth: element.scrollWidth }))
                .filter(element => element.left < boundary.left - 1 || element.right > boundary.right + 1 || element.contentWidth > element.width + 1);
        });
        I.assertDeepEqual(overflow, [], `Dashboard regions must remain within their available width at ${width}px.`);
        if (width === 390) I.saveScreenshot('dashboard-design-mobile.png', true);
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Remove design fixtures and verify the account preferences were never changed', async ({ I }) => {
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(noticesRoute);
    I.wjSetDefaultWindowSize();
    I.refreshPage();
    await waitForOverview(I);
    if (!originalSettings) return;
    const settings = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/dashboard/settings', { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        return response.json();
    });
    I.assertDeepEqual(settings, originalSettings, 'Visual regression fixtures must never mutate the real dashboard preferences.');
});
