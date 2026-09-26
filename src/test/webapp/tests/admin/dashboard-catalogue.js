Feature('admin.dashboard-catalogue').tag('@singlethread');

let originalSettings;
const catalogue = [
    ['shortcut', '1x1'], ['recent-pages', '3x3'], ['approvals', '3x3'], ['publishing', '2x3'],
    ['forms', '3x3'], ['traffic', '3x3'], ['top-pages', '3x3'], ['search-terms', '2x3'],
    ['referrers', '3x3'], ['newsletter', '3x3'], ['errors', '3x3'], ['sessions', '2x3'],
    ['news', '3x2'], ['search', 'fullauto']
];

function waitForWidgets(I) {
    I.waitForFunction(() => Boolean(document.querySelector('.md-dashboard[data-loaded="true"]'))
        && [...document.querySelectorAll('.md-dashboard__widget-body')].every(body => body.getAttribute('aria-busy') === 'false'), 30);
}

function waitForSave(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

function showWidget(I, type) {
    I.executeScript(type => window.scrollbarMain.scrollIntoView(document.querySelector(`[data-widget-type="${type}"]`)), type);
    I.waitForFunction(([widgetType]) => {
        const bounds = document.querySelector(`[data-widget-type="${widgetType}"]`)?.getBoundingClientRect();
        return Boolean(bounds && bounds.top >= 0 && bounds.top + Math.min(bounds.height, window.innerHeight - 100) <= window.innerHeight);
    }, [type], 10);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});

Scenario('Render the complete catalogue using real authorized data', async ({ I }) => {
    originalSettings = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    const applied = await I.executeScript(async definitions => {
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        const next = JSON.parse(JSON.stringify(controller.settings));
        const missing = definitions.filter(([type]) => !next.items.some(item => item.type === type));
        if (next.items.length + missing.length > 32) return { saved: false, reason: 'Insufficient free widget slots for a nondestructive fixture.' };
        const chosen = [];
        for (const [type, size] of definitions) {
            let item = next.items.find(item => item.type === type);
            if (!item) {
                item = { id: `catalogue-autotest-${type}`, type, size, collapsed: false, options: {} };
                next.items.push(item);
            }
            item.size = size;
            item.collapsed = false;
            item.options = type === 'shortcut' ? { href: '/admin/v9/webpages/web-pages-list/', title: 'catalogue-autotest shortcut' }
                : type === 'traffic' ? { days: 7, metric: 'sessions' } : type === 'search' ? { scope: 'admin' }
                    : ['forms', 'top-pages', 'search-terms', 'referrers', 'errors'].includes(type) ? { days: 7 } : {};
            next.domainOptions[item.id] = {};
            chosen.push(item);
        }
        const selectedIds = chosen.map(item => item.id);
        next.items = [...chosen, ...next.items.filter(item => !selectedIds.includes(item.id))];
        next.acknowledgedNewsVersion = null;
        window.autotestDashboardRenderErrors = [];
        window.addEventListener('error', event => { if (event.message) window.autotestDashboardRenderErrors.push(event.message); });
        window.addEventListener('unhandledrejection', event => window.autotestDashboardRenderErrors.push(String(event.reason)));
        return { saved: await controller._commit(next) };
    }, catalogue);
    I.assertTrue(applied.saved, applied.reason || 'The complete widget fixture must be saved.');
    waitForWidgets(I);
    const state = await I.executeScript(() => ({
        types: [...document.querySelectorAll('.md-dashboard__widget[data-widget-type]')].map(card => card.dataset.widgetType),
        errors: [...document.querySelectorAll('.md-dashboard__widget-content > .text-danger')].map(error => ({ type: error.closest('[data-widget-type]').dataset.widgetType, text: error.textContent })),
        expectedDomainError: WJ.translate('admin.dashboard.domainUnavailable.js'),
        javascriptErrors: window.autotestDashboardRenderErrors
    }));
    for (const [type] of catalogue) I.assertContain(state.types, type, `${type} must render from the final catalogue.`);
    I.assertDeepEqual(state.javascriptErrors, [], 'Widget rendering must not throw JavaScript errors.');
    for (const error of state.errors) {
        I.assertEqual(error.type, 'errors', 'Only explicitly unavailable legacy 404 domain data may fail.');
        I.assertEqual(error.text, state.expectedDomainError);
    }
    I.seeElement('#toast-container-overview');
    I.seeElement('[data-widget-type="sessions"] .md-dashboard__widget-content button');
    I.seeNumberOfElements('[data-widget-type="news"] .md-dashboard-widget__list > li', 3);
    I.resizeWindow(1337, 1052);
    I.saveScreenshot('dashboard-catalogue-desktop.png', true);
    showWidget(I, 'traffic');
    I.saveScreenshot('dashboard-catalogue-statistics.png', true);
    showWidget(I, 'news');
    I.saveScreenshot('dashboard-catalogue-bottom.png', true);
    I.resizeWindow(390, 1052);
    if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
    I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
    showWidget(I, 'recent-pages');
    I.saveScreenshot('dashboard-catalogue-mobile.png', true);
    I.wjSetDefaultWindowSize();
});

Scenario('Acknowledge release news across reload and reveal it from the catalogue', async ({ I }) => {
    waitForWidgets(I);
    I.waitForVisible('[data-widget-type="news"] .md-dashboard__widget-content button', 10);
    I.clickCss('[data-widget-type="news"] .md-dashboard__widget-content button');
    waitForSave(I);
    I.waitForInvisible('[data-widget-type="news"]', 10);
    const acknowledged = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.acknowledgedNewsVersion);
    I.assertTrue(typeof acknowledged === 'string' && acknowledged.length > 0);
    I.refreshPage();
    waitForWidgets(I);
    I.dontSeeElement('[data-widget-type="news"]');
    I.clickCss('.md-dashboard__toolbar > button');
    I.waitForVisible('.md-dashboard__catalogue-item[data-widget-type="news"] button', 10);
    I.clickCss('.md-dashboard__catalogue-item[data-widget-type="news"] button');
    I.waitForInvisible('.md-dashboard-modal', 10);
    waitForSave(I);
    I.waitForVisible('[data-widget-type="news"]', 10);
    I.seeNumberOfElements('[data-widget-type="news"] .md-dashboard-widget__list > li', 3);
});

Scenario('Documentation search switches scope and opens the encoded query without an external request', async ({ I }) => {
    waitForWidgets(I);
    const scope = '[data-widget-type="search"]';
    const query = 'formulár & prístupnosť autotest';
    I.clickCss(`${scope} input[type="radio"][value="docs"]`);
    I.fillField(`${scope} input[type="search"]`, query);
    await I.executeScript(() => {
        window.autotestOriginalOpen = window.open;
        window.autotestSearchPopup = null;
        window.open = (url, target, features) => { window.autotestSearchPopup = { url, target, features }; return null; };
    });
    I.clickCss(`${scope} button[type="submit"]`);
    const popup = await I.executeScript(() => {
        try { return window.autotestSearchPopup; }
        finally { window.open = window.autotestOriginalOpen; delete window.autotestOriginalOpen; }
    });
    I.assertTrue(Boolean(popup));
    I.assertEqual(new URL(popup.url).origin, 'https://docs.webjetcms.sk');
    I.assertEqual(new URL(popup.url).searchParams.get('q'), query);
    I.assertEqual(popup.target, '_blank');
    I.assertContain(popup.features, 'noopener');
    I.clickCss(`${scope} input[type="radio"][value="admin"]`);
    I.seeElement(`${scope} input[type="radio"][value="admin"]:checked`);
});

Scenario('Pending approvals open their supported dashboard destination', async ({ I }) => {
    const total = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/dashboard/data/approvals', { headers: { 'X-CSRF-Token': window.csrfToken }, credentials: 'same-origin' });
        if (!response.ok) throw new Error(`Approvals request failed: ${response.status}`);
        return (await response.json()).total;
    });
    if (!total) {
        I.say('No pending approvals in this account/domain; the deep-link condition does not apply.');
        return;
    }
    I.clickCss('[data-widget-type="approvals"] .md-dashboard-widget__more');
    I.waitForElement('#pills-pages #pills-waiting-tab.active', 20);
    I.waitForFunction(() => {
        const tabs = document.querySelector('#pills-pages_sub');
        return Boolean(tabs && getComputedStyle(tabs.parentElement).display !== 'none');
    }, 10);
});

Scenario('Restore the original account dashboard and current-domain filters', async ({ I }) => {
    if (!originalSettings) return;
    const status = await I.executeScript(async settings => {
        const response = await fetch('/admin/rest/dashboard/settings', {
            method: 'PUT', credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: JSON.stringify(settings)
        });
        return response.status;
    }, originalSettings);
    I.assertEqual(status, 200, 'The original account preferences must be restored.');
    I.refreshPage();
    waitForWidgets(I);
    const restored = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings);
    I.assertDeepEqual(restored.items, originalSettings.items);
    I.assertDeepEqual(restored.domainOptions, originalSettings.domainOptions);
    I.assertEqual(restored.acknowledgedNewsVersion, originalSettings.acknowledgedNewsVersion);
});
