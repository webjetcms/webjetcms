const { waitForWidgets, mockDashboardBootstrap, dashboardPageRoute, readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.design').tag('@singlethread');

const settingsRoute = '**/admin/rest/dashboard/settings';
const responses = {
    '**/admin/rest/forms-list/all': { content: [
        { formName: 'Autotest contact form', count: 128, createDate: Date.UTC(2026, 8, 26, 10) },
        { formName: 'Autotest registration form with a longer name', count: 75, createDate: Date.UTC(2026, 8, 25, 10) }
    ] },
    '**/admin/rest/web-pages/all?*': { content: Array.from({ length: 6 }, (_, index) => ({
        docId: index + 1, title: `Autotest recent page ${index + 1}`, fullPath: `/Autotest section/Autotest recent page ${index + 1}`,
        dateCreated: Date.UTC(2026, 8, 26, 10), perexImage: ''
    })) },
    '**/admin/rest/stat/search-engines/search/findByColumns?*': { content: [
        { queryName: 'Autotest search phrase with a longer description', queryCount: 128 },
        { queryName: 'AutotestLongSearchPhraseWithoutSpaces', queryCount: 75 },
        { queryName: 'Autotest contact', queryCount: 6 }
    ] },
    '**/admin/rest/stat/top/search/findByColumns?*': { content: [
        { docId: 1, title: 'Autotest popular page with a longer title', name: '/Autotest section/Autotest popular page with a longer title', visits: 128, perexImage: '' },
        { docId: 2, title: 'AutotestLongPageTitleWithoutSpaces', name: '/Autotest section/AutotestLongPageTitleWithoutSpaces', visits: 75, perexImage: '' },
        { docId: 3, title: 'Autotest contact', name: '/Autotest section/Autotest contact', visits: 6, perexImage: '' }
    ] }
};
const settings = {
    version: 1, configured: true, legacyBookmarksHandled: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
        { id: 'visual-autotest-news', type: 'news', size: '3x2', options: {} },
        { id: 'visual-autotest-search', type: 'search', size: 'fullauto', options: { scope: 'admin' } },
        { id: 'visual-autotest-shortcut', type: 'shortcut', size: '1x1', options: { source: 'url', title: 'Autotest shortcut', href: 'https://example.com/autotest', icon: 'ti-heart', color: 'lavender' } },
        { id: 'visual-autotest-count', type: 'forms', size: '1x1', options: {} },
        { id: 'visual-autotest-forms', type: 'forms', size: '3x3', options: {} },
        { id: 'visual-autotest-pages', type: 'recent-pages', size: '3x2', options: {} },
        ...['top-pages', 'search-terms'].map(type => ({ id: `visual-autotest-${type}`, type, size: '2x3', options: { days: 7 } }))
    ]
};
let originalSettings;

Before(({ login }) => login('admin'));

/** Installs fixed display data and intercepts preferences so visual checks never save the fixture. */
Scenario('Prepare deterministic dashboard screenshots', async ({ I, Browser }) => {
    if (!Browser.isChromium()) return;
    I.amOnPage('/admin/v9/');
    originalSettings = (await I.executeScript(readDashboardBootstrap)).settings;
    await I.mockRoute(settingsRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(settings) }));
    for (const [pattern, response] of Object.entries(responses)) {
        await I.mockRoute(pattern, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(response) }));
    }
    await mockDashboardBootstrap(I, () => ({
        settings, userName: 'Autotest editor', statRootGroupId: 1,
        notices: [{ id: 'visual-autotest-notice', severity: 'warning', icon: 'ti-shield-lock', title: 'Autotest account protection', bodyHtml: '<p>Autotest security notice.</p>' }],
        currentSessions: { currentSessionId: 'visual-autotest-current', userSessions: [{ cluster: 'autotest', userSessions: [
            { sessionId: 'visual-autotest-current', browserName: 'Chrome', logonTime: Date.UTC(2026, 8, 27, 8), remoteAddr: '127.0.0.1' },
            { sessionId: 'visual-autotest-other', browserName: 'Firefox', logonTime: Date.UTC(2026, 8, 26, 8), remoteAddr: '127.0.0.2' }
        ] }] }
    }));
});

/** Opens a fresh preview so a failed comparison cannot leave a dialog open for the next scenario. */
async function openPreview(I, width = 1337) {
    I.resizeWindow(width, 1052);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.executeScript(() => {
        // Freeze only Date; the CodeceptJS navigation helper still needs native performance.timing.
        const NativeDate = Date;
        const now = Date.UTC(2026, 8, 27, 10);
        window.Date = class extends NativeDate {
            constructor(...args) { super(...(args.length ? args : [now])); }
            static now() { return now; }
        };
        const dashboard = document.querySelector('webjet-overview-dashboard');
        dashboard.configure({ data: dashboard.data,
            config: { ...dashboard.config, environmentName: 'DEV/autotest', environmentType: 'DEV', environmentIcon: 'auto', environmentColor: 'auto',
                heroBackgroundImage: '/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg' },
            labels: { ...dashboard.labels, changelog: '<p>WebJET CMS 2026.18 autotest release.</p><p>Autotest editors can manage pages, forms and their personal dashboard.</p>' }
        });
    });
    await waitForWidgets(I);
    I.waitForFunction(() => document.fonts.status === 'loaded', 10);
    I.waitForFunction(() => document.querySelector('.md-dashboard__notice-list')?.getAttribute('aria-busy') === 'false', 10);
    I.dontSeeElement('.md-dashboard__widget-content > .text-danger');
    I.seeElement('[data-instance-id="visual-autotest-shortcut"] a[href="https://example.com/autotest"]');
    if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
}

for (const width of [390, 768, 1337]) {
    /** Compares the dashboard using the same content at each representative viewport. */
    Scenario(`Dashboard matches the reference screenshot at ${width}px`, async ({ I, Browser, Document }) => {
        if (!Browser.isChromium()) return;
        await openPreview(I, width);
        // Give the element enough room for capture without clipping it inside the transformed admin scroller.
        const height = await I.executeScript(() => Math.ceil(document.querySelector('.md-dashboard').getBoundingClientRect().height) + 200);
        I.resizeWindow(width, Math.max(1052, height));
        I.executeScript(() => { window.scrollbarMain.setMomentum(0, 0); window.scrollbarMain.setPosition(0, 0); window.scrollTo(0, 0); });
        I.moveCursorTo('.ly-header');
        await Document.compareScreenshotElement('.md-dashboard', `dashboard/overview-${width}.png`, null, null, 2);
    });
}

Scenario('Widget catalogue matches the reference screenshot', async ({ I, Browser, Document }) => {
    if (!Browser.isChromium()) return;
    await openPreview(I);
    I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    I.clickCss('.md-dashboard__toolbar-actions > .md-dashboard__edit-control:not(.md-dashboard__reset)');
    I.waitForVisible('.md-dashboard-modal input[type="search"]', 10);
    I.waitForFunction(() => document.querySelector('.md-dashboard-modal')?.contains(document.activeElement), 10);
    await Document.compareScreenshotElement('.md-dashboard-modal .modal-content', 'dashboard/catalogue.png', null, null, 2);
});

Scenario('Shortcut settings match the reference screenshot', async ({ I, Browser, Document }) => {
    if (!Browser.isChromium()) return;
    await openPreview(I);
    I.clickCss('.md-dashboard__shortcut-actions button[aria-pressed="false"]');
    I.clickCss('[data-instance-id="visual-autotest-shortcut"] .dropdown > button');
    I.clickCss('[data-instance-id="visual-autotest-shortcut"] [data-dashboard-action="settings"]');
    I.waitForVisible('.md-dashboard-modal [name="dashboardShortcutTitle"]', 10);
    I.waitForFunction(() => document.querySelector('.md-dashboard-modal')?.contains(document.activeElement), 10);
    await Document.compareScreenshotElement('.md-dashboard-modal .modal-content', 'dashboard/shortcut.png', null, null, 2);
});

/** Reloads without the simulated data or fixed date and verifies the original account preferences. */
Scenario('Restore the dashboard after visual checks', async ({ I, Browser }) => {
    if (!Browser.isChromium()) return;
    for (const pattern of [settingsRoute, dashboardPageRoute, ...Object.keys(responses)]) await I.stopMockingRoute(pattern);
    I.wjSetDefaultWindowSize();
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    if (originalSettings) I.assertDeepEqual((await I.executeScript(readDashboardBootstrap)).settings, originalSettings,
        'Visual fixtures must never change the real account preferences.');
});
