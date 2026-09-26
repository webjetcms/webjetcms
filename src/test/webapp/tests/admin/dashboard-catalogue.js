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
    I.executeScript(type => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        const top = document.querySelector(`[data-widget-type="${type}"]`).getBoundingClientRect().top;
        if (scrollbar.limit.y > 0) scrollbar.setPosition(scrollbar.offset.x, scrollbar.offset.y + top - 64);
        else window.scrollTo(0, window.scrollY + top - 64);
    }, type);
    I.waitForFunction(([widgetType]) => {
        const bounds = document.querySelector(`[data-widget-type="${widgetType}"]`)?.getBoundingClientRect();
        return Boolean(bounds && bounds.top >= 0 && bounds.top + Math.min(bounds.height, window.innerHeight - 100) <= window.innerHeight);
    }, [type], 10);
}

async function widgetAction(I, id, action) {
    await I.clickIfVisible('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    I.waitForElement('.md-dashboard.is-editing', 10);
    I.clickCss(`[data-instance-id="${id}"] .dropdown > button`);
    I.waitForVisible(`[data-instance-id="${id}"] [data-dashboard-action="${action}"]`, 10);
    I.forceClick(`[data-instance-id="${id}"] [data-dashboard-action="${action}"]`);
}

function waitForChart(I, type) {
    I.waitForFunction(([type]) => {
        const host = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        return Boolean(host && host.querySelector('canvas') && window.am5?.registry.rootElements.some(root => root.dom === host));
    }, [type], 20);
}

async function rememberChart(I, type) {
    return I.executeScript(type => {
        const host = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        window.autotestDashboardChart = window.am5.registry.rootElements.find(root => root.dom === host);
        return host.id;
    }, type);
}

async function assertDisposedChart(I) {
    I.assertTrue(await I.executeScript(() => window.autotestDashboardChart.isDisposed()
        && !window.am5.registry.rootElements.includes(window.autotestDashboardChart)), 'Replaced charts must release their AmCharts root.');
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
    I.seeNumberOfElements('#toast-container-overview', 1);
    I.seeElement('[data-widget-type="sessions"] .md-dashboard__widget-content button');
    I.seeElement('[data-widget-type="recent-pages"] .md-dashboard__widget-header .md-dashboard__header-link[href="/admin/v9/webpages/web-pages-list/"]');
    I.dontSeeElement('[data-widget-type="recent-pages"] .md-dashboard-widget__more');
    I.assertEqual(await I.grabTextFrom('[data-widget-type="traffic"] .md-dashboard-widget__metric-label'),
        await I.executeScript(() => WJ.translate('admin.dashboard.trafficSessions.js', 7)), 'Traffic descriptions must include the selected number of days.');
    I.assertTrue(await I.executeScript(() => {
        const source = new DOMParser().parseFromString(document.querySelector('webjet-overview-dashboard').labels.changelog, 'text/html');
        return document.querySelector('.md-dashboard-widget__news-highlights').innerHTML === source.body.innerHTML;
    }), 'Release notes must preserve the entire rendered Markdown announcement.');
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

Scenario('AmCharts renders accessible data and disposes roots on refresh, collapse, resize and removal', async ({ I, a11y }) => {
    waitForWidgets(I);
    waitForChart(I, 'traffic');
    waitForChart(I, 'referrers');
    const ids = await I.executeScript(() => Object.fromEntries(['traffic', 'referrers'].map(type => [type,
        document.querySelector(`[data-widget-type="${type}"]`).dataset.instanceId])));
    for (const type of ['traffic', 'referrers']) {
        I.seeElement(`[data-widget-type="${type}"] .md-dashboard-widget__chart[role="img"][aria-label]`);
        if (type === 'traffic') {
            I.dontSeeElement('[data-widget-type="traffic"] details.md-dashboard-widget__chart-data');
            I.seeElementInDOM('[data-widget-type="traffic"] .visually-hidden .md-dashboard-widget__table');
            I.seeElement('[data-widget-type="traffic"] .md-dashboard__title-link[href="/apps/stat/admin/"]');
            I.dontSeeElement('[data-widget-type="traffic"] .md-dashboard-widget__more');
            continue;
        }
        I.seeElement(`[data-widget-type="${type}"] details.md-dashboard-widget__chart-data summary`);
        I.clickCss(`[data-widget-type="${type}"] details.md-dashboard-widget__chart-data summary`);
        I.seeElement(`[data-widget-type="${type}"] details[open] .md-dashboard-widget__table`);
        I.clickCss(`[data-widget-type="${type}"] details.md-dashboard-widget__chart-data summary`);
    }
    const firstTraffic = await rememberChart(I, 'traffic');
    await widgetAction(I, ids.traffic, 'refresh');
    waitForWidgets(I);
    waitForChart(I, 'traffic');
    await assertDisposedChart(I);
    I.assertNotEqual(await I.grabAttributeFrom(`[data-instance-id="${ids.traffic}"] .md-dashboard-widget__chart`, 'id'), firstTraffic);

    await rememberChart(I, 'traffic');
    await widgetAction(I, ids.traffic, 'collapse');
    waitForSave(I);
    waitForWidgets(I);
    await assertDisposedChart(I);
    I.dontSeeElement(`[data-instance-id="${ids.traffic}"] .md-dashboard-widget__chart`);
    I.seeElement(`[data-instance-id="${ids.traffic}"] .md-dashboard__title-link`);
    await widgetAction(I, ids.traffic, 'collapse');
    waitForSave(I);
    waitForChart(I, 'traffic');
    await rememberChart(I, 'traffic');
    await widgetAction(I, ids.traffic, 'settings');
    I.waitForVisible('.md-dashboard-modal select', 10);
    I.selectOption('.md-dashboard-modal select[id^="dashboard-size-"]', '1 × 1');
    I.clickCss('.md-dashboard-modal .modal-footer .btn-primary');
    I.waitForInvisible('.md-dashboard-modal', 10);
    waitForSave(I);
    waitForWidgets(I);
    await assertDisposedChart(I);
    I.dontSeeElement(`[data-instance-id="${ids.traffic}"] .md-dashboard-widget__chart`);
    await widgetAction(I, ids.traffic, 'settings');
    I.waitForVisible('.md-dashboard-modal select', 10);
    I.selectOption('.md-dashboard-modal select[id^="dashboard-size-"]', '3 × 3');
    I.clickCss('.md-dashboard-modal .modal-footer .btn-primary');
    I.waitForInvisible('.md-dashboard-modal', 10);
    waitForSave(I);
    waitForChart(I, 'traffic');

    await rememberChart(I, 'referrers');
    await widgetAction(I, ids.referrers, 'remove');
    waitForSave(I);
    I.waitForInvisible(`[data-instance-id="${ids.referrers}"]`, 10);
    await assertDisposedChart(I);
    I.clickCss('.md-dashboard__undo button');
    waitForSave(I);
    waitForChart(I, 'referrers');
    const rootsMatchHosts = await I.executeScript(() => {
        const hosts = [...document.querySelectorAll('.md-dashboard-widget__chart')];
        const roots = window.am5.registry.rootElements.filter(root => root.dom.id.startsWith('dashboard-chart-'));
        return hosts.length === roots.length && hosts.every(host => roots.filter(root => root.dom === host).length === 1);
    });
    I.assertTrue(rootsMatchHosts, 'Every displayed chart must own exactly one live root with no orphan dashboard roots.');
    await a11y.check('.md-dashboard');
    showWidget(I, 'traffic');
    I.saveScreenshot('dashboard-amcharts.png', true);
});

Scenario('Collapse release news across reload and expand it from the compact summary', async ({ I }) => {
    waitForWidgets(I);
    I.waitForVisible('[data-widget-type="news"] .md-dashboard__widget-content button', 10);
    I.clickCss('[data-widget-type="news"] .md-dashboard__widget-content button');
    waitForSave(I);
    I.waitForVisible('[data-widget-type="news"] .md-dashboard-widget__news-toggle[aria-expanded="false"]', 10);
    I.seeElement('[data-widget-type="news"] .md-dashboard-widget__news-summary');
    I.dontSeeElement('[data-widget-type="news"] .md-dashboard-widget__news-highlights');
    const acknowledged = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.acknowledgedNewsVersion);
    I.assertTrue(typeof acknowledged === 'string' && acknowledged.length > 0);
    I.refreshPage();
    waitForWidgets(I);
    I.seeElement('[data-widget-type="news"] .md-dashboard-widget__news-summary');
    I.clickCss('[data-widget-type="news"] .md-dashboard-widget__news-toggle');
    waitForSave(I);
    I.waitForVisible('[data-widget-type="news"]', 10);
    I.assertTrue(await I.executeScript(() => {
        const source = new DOMParser().parseFromString(document.querySelector('webjet-overview-dashboard').labels.changelog, 'text/html');
        return document.querySelector('.md-dashboard-widget__news-highlights').innerHTML === source.body.innerHTML;
    }), 'Expanding release notes must restore all original Markdown formatting.');
});

Scenario('Documentation search switches scope and opens the encoded query without an external request', async ({ I }) => {
    waitForWidgets(I);
    const scope = '[data-widget-type="search"]';
    const query = 'formulár & prístupnosť autotest';
    I.clickCss(`${scope} label:has(input[value="docs"])`);
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
    I.clickCss(`${scope} label:has(input[value="admin"])`);
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
    I.clickCss('[data-widget-type="approvals"] .md-dashboard__title-link');
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
