const { waitForWidgets, mockDashboardBootstrap, dashboardPageRoute, readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.interactions').tag('@singlethread');

let originalSettings;
let previewSettings;

const settingsRoute = '**/admin/rest/dashboard/settings';
const recentPagesRoute = '**/admin/rest/web-pages/all?*';
const searchTermsRoute = '**/admin/rest/stat/search-engines/search/findByColumns?*';
const topPagesRoute = '**/admin/rest/stat/top/search/findByColumns?*';
const missingThumbnailRoute = '**/thumb/images/autotest-dashboard-missing.jpg?*';
const editButton = '.md-dashboard__toolbar-actions > button[aria-pressed]';
const newsToggle = '.md-dashboard-widget__news-toggle';

async function waitForOverview(I) {
    await waitForWidgets(I);
    return I.waitForFunction(() => document.querySelector('.md-dashboard__notice-list')?.getAttribute('aria-busy') === 'false', 30);
}

function waitForSave(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});

/**
 * Checks that active sessions stay in the welcome area, notices can be expanded independently, and
 * arrangement controls appear only in edit mode. Closing the keyboard move dialog must return focus without
 * collapsing open notices.
 */
Scenario('Pinned security, independent notices and edit mode keep the dashboard readable', async ({ I }) => {
    await waitForOverview(I);
    originalSettings = (await I.executeScript(readDashboardBootstrap)).settings;

    // Preference mutations stay in this intercepted fixture and never reach the account's stored profile.
    previewSettings = {
        version: 1, configured: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: 'design-autotest-pages', type: 'recent-pages', size: '3x3', options: {} },
            { id: 'design-autotest-publishing', type: 'publishing', size: '2x3', options: {} },
            { id: 'design-autotest-session', type: 'sessions', size: '2x3', options: {} },
            { id: 'design-autotest-news', type: 'news', size: '3x2', options: {} },
            { id: 'design-autotest-search', type: 'search', size: 'fullauto', options: { scope: 'admin' } },
            { id: 'design-autotest-shortcut', type: 'shortcut', size: '1x1', options: { href: '/admin/v9/webpages/web-pages-list/', title: 'Web pages autotest' } }
        ]
    };
    await I.mockRoute(settingsRoute, route => {
        if (route.request().method() === 'PUT') previewSettings = { ...route.request().postDataJSON(), configured: true };
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(previewSettings) });
    });
    const notices = [
        { id: 'design-autotest-migration', severity: 'warning', icon: 'ti-database', title: 'Database migration autotest', bodyHtml: '<p>Statistics require a conversion autotest.</p>', action: { type: 'link', url: '/admin/v9/', label: 'Migration action autotest' } },
        { id: 'design-autotest-security', severity: 'warning', icon: 'ti-shield-lock', title: 'Account protection autotest', bodyHtml: '<p>Enable a second verification factor autotest.</p>', action: { type: 'popup', url: '/admin/2factorauth.jsp', label: 'Security action autotest' } }
    ];
    await mockDashboardBootstrap(I, () => ({ settings: previewSettings, notices }));
    I.refreshPage();
    await waitForOverview(I);
    I.seeElement('.md-dashboard__sessions [data-widget-type="sessions"] .md-dashboard-widget__session');
    I.seeElement('.md-dashboard__sessions span.md-dashboard-widget__session-count');
    I.dontSeeElement('.md-dashboard__layout [data-widget-type="sessions"]');
    I.dontSeeElement('.md-dashboard__sessions .md-dashboard__widget-controls');
    I.dontSeeElement('.md-dashboard__edit-control');
    I.seeElement('.md-dashboard__shortcut-actions button[aria-pressed]');

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
});

/**
 * Checks that feedback, widget selection and reset confirmation use readable dialogs on desktop and mobile.
 * Closing or cancelling returns focus to the opening button; feedback is not sent and reset is not
 * confirmed.
 */
Scenario('Feedback toolbar and widget catalogue keep familiar dialog controls on desktop and mobile', async ({ I }) => {
    await waitForOverview(I);
    const feedback = '.md-dashboard__feedback';
    const addWidget = '.md-dashboard__toolbar-actions > .md-dashboard__edit-control:not(.md-dashboard__reset)';
    const resetWidget = '.md-dashboard__toolbar-actions > .md-dashboard__reset';
    const resetDialog = '#toast-container-webjet .toast[role="dialog"]';
    I.dontSeeElementInDOM('.md-dashboard__legacy');
    I.seeNumberOfElements('.md-dashboard__feedback', 1);
    I.dontSeeElement(addWidget);
    I.dontSeeElement(resetWidget);

    for (const width of [1337, 390]) {
        I.resizeWindow(width, 1052);
        if (width < 768 && await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
        if (width < 768) I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
        I.executeScript(() => {
            const scrollbar = window.scrollbarMain;
            scrollbar.setMomentum(0, 0);
            scrollbar.update();
            const top = document.querySelector('.md-dashboard__toolbar').getBoundingClientRect().top;
            if (scrollbar.limit.y > 0) scrollbar.setPosition(0, scrollbar.offset.y + top - 80);
            else window.scrollTo(0, window.scrollY + top - 80);
        });
        I.clickCss(feedback);
        I.waitForVisible('#feedback_modal #feedback-group-text', 10);
        I.waitForFunction(() => document.querySelector('#feedback_modal')?.contains(document.activeElement), 10);
        I.seeElement('#feedback_modal #feedback-upload');
        I.seeElement('#feedback_modal #feedback-group-anonymous');
        // Inspect the original form and cancel without sending feedback or uploading a file.
        I.clickCss('#feedback_modal .btn-close-editor');
        I.waitForFunction(() => !document.querySelector('#feedback_modal'), 10);
        I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), feedback),
            'Canceling feedback must restore focus to its toolbar action.');

        I.clickCss(editButton);
        I.seeElement(addWidget);
        I.seeElement(resetWidget);
        I.clickCss(addWidget);
        I.waitForVisible('.md-dashboard-modal input[type="search"]', 10);
        I.waitForFunction(() => document.querySelector('.md-dashboard-modal')?.contains(document.activeElement), 10);
        I.assertEqual(await I.grabAttributeFrom('.md-dashboard-modal button.btn-close', 'aria-label'),
            await I.executeScript(() => WJ.translate('admin.dashboard.close.js')), 'The close button must have a localized accessible name.');
        I.clickCss('.md-dashboard-modal .modal-header button.btn-close');
        I.waitForFunction(() => !document.querySelector('.md-dashboard-modal'), 10);
        I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), addWidget),
            'Closing the catalogue must return focus to Add widget.');
        // moveCursorTo only moves the pointer; it does not scroll a wrapped mobile toolbar into view.
        I.waitForFunction(selector => {
            const button = document.querySelector(selector);
            const bounds = button.getBoundingClientRect();
            if (bounds.top < 64 || bounds.bottom > window.innerHeight) {
                const scrollbar = window.scrollbarMain;
                scrollbar.setMomentum(0, 0);
                scrollbar.update();
                if (scrollbar.limit.y > 0) scrollbar.setPosition(0, scrollbar.offset.y + bounds.top - 80);
                else window.scrollTo(0, window.scrollY + bounds.top - 80);
                return false;
            }
            return button.contains(document.elementFromPoint(bounds.left + bounds.width / 2, bounds.top + bounds.height / 2));
        }, [resetWidget], 10);
        I.moveCursorTo(resetWidget);
        I.waitForFunction(() => {
            const tooltipId = document.querySelector('.md-dashboard__reset').getAttribute('aria-describedby');
            return Boolean(tooltipId && document.getElementById(tooltipId)?.classList.contains('show'));
        }, 10);
        I.clickCss(resetWidget);
        I.waitForVisible(`${resetDialog} button[id^="confirmationYes"]`, 10);
        I.waitForInvisible('.tooltip.wj-tooltip-hoverable.show', 10);
        I.dontSeeElement('.tooltip.wj-tooltip-hoverable.show');
        I.assertTrue(await I.executeScript(() => {
            const confirmation = document.querySelector('#toast-container-webjet .toast[role="dialog"]');
            return document.getElementById(confirmation.getAttribute('aria-describedby'))?.textContent.length > 0;
        }), 'Reset must explain its scope in an accessible confirmation.');
        I.clickCss(`${resetDialog} button[id^="confirmationNo"]`);
        I.waitForInvisible(resetDialog, 10);
        I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), resetWidget),
            'Canceling reset must return focus to its toolbar action without changing preferences.');
        I.waitForInvisible('.tooltip.wj-tooltip-hoverable.show', 10);
        I.dontSeeElement('.tooltip.wj-tooltip-hoverable.show');
        I.clickCss(editButton);
    }
    I.wjSetDefaultWindowSize();
});

/**
 * Checks that metric headings link to their modules and all six recent pages remain reachable by scrolling
 * their own list with the mouse or keyboard.
 */
Scenario('Metric links and recent-page scrolling remain usable with mouse and keyboard', async ({ I }) => {
    previewSettings.items = [
        { id: 'compact-autotest-traffic', type: 'traffic', size: '3x3', options: { days: 7 } },
        ...['forms', 'approvals', 'errors'].map(type => ({ id: `compact-autotest-${type}`, type, size: '1x1', options: { days: 7 } })),
        { id: 'compact-autotest-pages', type: 'recent-pages', size: '3x2', options: {} },
        ...['referrers', 'publishing', 'newsletter'].map(type => ({ id: `compact-autotest-${type}`, type, size: '2x2', options: { days: 7 } }))
    ];
    await I.mockRoute(recentPagesRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content:
        Array.from({ length: 6 }, (_, index) => ({ docId: index + 1, title: `Recent page autotest ${index + 1}`,
            fullPath: `/Autotest section/Recent page autotest ${index + 1}`, dateCreated: Date.UTC(2026, 8, 26, 10), perexImage: '' }))
    }) }));
    I.resizeWindow(1337, 1052);
    I.refreshPage();
    await waitForOverview(I);
    const metrics = await I.executeScript(() => ['forms', 'approvals', 'errors'].map(type => {
        const card = document.querySelector(`[data-widget-type="${type}"]`);
        return { type, href: card.querySelector('.md-dashboard__title-link')?.getAttribute('href') };
    }));
    const targets = { forms: '/apps/form/admin/', approvals: '/admin/v9/webpages/web-pages-list/?show=toapprove', errors: '/apps/stat/admin/error/' };
    metrics.forEach(metric => {
        I.assertEqual(metric.href, targets[metric.type], 'A metric heading must open the corresponding module.');
    });
    I.assertEqual(await I.grabTextFrom('[data-widget-type="forms"] .md-dashboard-widget__metric-label'),
        await I.executeScript(() => WJ.translate('admin.dashboard.totalSubmissions.js')));
    I.dontSeeElementInDOM('[data-widget-type="forms"] .md-dashboard-widget__period');
    I.executeScript(() => document.querySelector('[data-widget-type="forms"] .md-dashboard__title-link').focus());
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('[data-widget-type="forms"] .md-dashboard__title-link')), 'Metric header navigation must support keyboard focus.');
    for (const type of ['referrers', 'publishing', 'newsletter']) {
        I.seeElement(`[data-widget-type="${type}"] .md-dashboard__title-link`);
    }
    const list = '[data-widget-type="recent-pages"] .md-dashboard-widget__pages';
    I.seeNumberOfElements(`${list} > li`, 6);
    I.assertTrue(await I.executeScript(selector => {
        const node = document.querySelector(selector);
        return node.tabIndex === 0 && Boolean(node.getAttribute('aria-label')) && node.scrollHeight > node.clientHeight;
    }, list), 'All six pages must remain available inside a labeled, keyboard-focusable scroll area.');
    const pageOffset = await I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('[data-widget-type="traffic"]').getBoundingClientRect().top - 64);
        return scrollbar.offset.y;
    });
    // Standard scroll helpers do not generate the wheel events consumed by smooth-scrollbar.
    await I.usePlaywrightTo('wheel over the recent-page preview', async ({ page }) => {
        const rect = await page.locator(list).boundingBox();
        await page.mouse.move(rect.x + rect.width / 2, rect.y + 30);
        await page.mouse.wheel(0, 130);
    });
    I.waitForFunction(selector => document.querySelector(selector).scrollTop > 0, [list], 10);
    I.assertTrue(Math.abs(await I.executeScript(() => window.scrollbarMain.offset.y) - pageOffset) <= 1, 'Wheel scrolling must stay in the recent-page list.');
    I.executeScript(selector => { const node = document.querySelector(selector); node.focus({ preventScroll: true }); node.scrollTop = 0; }, list);
    I.pressKey('PageDown');
    I.waitForFunction(selector => document.querySelector(selector).scrollTop > 0, [list], 10);
    I.assertTrue(Math.abs(await I.executeScript(() => window.scrollbarMain.offset.y) - pageOffset) <= 1, 'Keyboard scrolling must stay in the recent-page list.');
    I.executeScript(selector => { const node = document.querySelector(selector); node.blur(); node.scrollTop = 0; }, list);
    await I.stopMockingRoute(recentPagesRoute);
});

/**
 * Checks that ranked cards link to their modules and page previews show a loaded thumbnail or a fallback
 * when the image is absent or cannot be loaded.
 */
Scenario('Ranked cards link to their modules and handle missing thumbnails', async ({ I }) => {
    const previousItems = previewSettings.items;
    previewSettings.items = ['search-terms', 'top-pages'].map(type => ({ id: `ranked-autotest-${type}`, type, size: '3x3', options: { days: 7 } }));
    await I.mockRoute(searchTermsRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content: [
        { queryName: 'Autotest vyhľadávací výraz s veľmi dlhým opisným názvom', queryCount: 128 },
        { queryName: 'AutotestDlhýVýrazBezMedzierOverujúciZalomenieTextu', queryCount: 75 },
        { queryName: 'Autotest kontakt', queryCount: 6 }
    ] }) }));
    await I.mockRoute(topPagesRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content: [
        { docId: 1, title: 'Autotest veľmi dlhý názov najnavštevovanejšej stránky', name: '/Autotest sekcia/Podrobné informácie/Autotest veľmi dlhý názov najnavštevovanejšej stránky', perexImage: '/images/zo-sveta-financii/konsolidacia-napriec-trhmi/oil-pump.jpg', visits: 128 },
        { docId: 2, title: 'AutotestDlhýNázovStránkyBezMedzier', name: '/AutotestSekciaBezMedzier/AutotestDlhýNázovStránkyBezMedzier', perexImage: '', visits: 75 },
        { docId: 3, title: 'Autotest kontakt', name: '/Autotest/Autotest kontakt', perexImage: '/images/autotest-dashboard-missing.jpg', visits: 6 }
    ] }) }));
    await I.mockRoute(missingThumbnailRoute, route => route.fulfill({ status: 404, contentType: 'text/plain', body: 'Missing autotest thumbnail' }));
    I.resizeWindow(1337, 1052);
    I.refreshPage();
    await waitForOverview(I);
    const targets = { 'search-terms': '/apps/stat/admin/search-engines/', 'top-pages': '/apps/stat/admin/top/' };
    for (const type of Object.keys(targets)) {
        I.assertEqual(await I.grabAttributeFrom(`[data-widget-type="${type}"] .md-dashboard__title-link`, 'href'), targets[type]);
        I.seeNumberOfElements(`[data-widget-type="${type}"] tbody tr`, 3);
    }
    const pagesTable = '[data-widget-type="top-pages"] .md-dashboard-widget__table--pages';
    I.seeNumberOfElements(`${pagesTable} thead th`, 3);
    I.seeNumberOfElements(`${pagesTable} tbody .md-dashboard-widget__page-preview`, 3);
    I.seeNumberOfElements(`${pagesTable} tbody td:first-child .md-dashboard-widget__page-title`, 3);
    I.seeNumberOfElements(`${pagesTable} tbody td:first-child .md-dashboard-widget__page-section`, 3);
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('[data-widget-type="top-pages"]').getBoundingClientRect().top - 64);
    });
    I.waitForFunction(selector => {
        const image = document.querySelector(`${selector} tbody tr:first-child img`);
        return image?.complete && image.naturalWidth > 0 && !document.querySelector(`${selector} tbody tr:last-child img`);
    }, [pagesTable], 10);
    I.assertEqual(await I.grabAttributeFrom(`${pagesTable} tbody tr:first-child img`, 'alt'), '');
    I.seeElement(`${pagesTable} tbody tr:nth-child(2) .ti-file-text:not([hidden])`);
    I.seeElement(`${pagesTable} tbody tr:nth-child(3) .ti-file-text:not([hidden])`);
    previewSettings.items.find(item => item.type === 'top-pages').size = '2x3';
    I.resizeWindow(1337, 1052);
    I.refreshPage();
    await waitForOverview(I);
    I.seeNumberOfElements(`${pagesTable} thead th`, 3);
    I.seeNumberOfElements(`${pagesTable} tbody .md-dashboard-widget__page-preview`, 3);
    previewSettings.items = previousItems;
    await I.stopMockingRoute(searchTermsRoute);
    await I.stopMockingRoute(topPagesRoute);
    await I.stopMockingRoute(missingThumbnailRoute);
    I.resizeWindow(1337, 1052);
});

/** Checks configured environment names, custom icons and hiding an empty label. */
Scenario('Environment badge follows configured identity and disappears when empty', async ({ I }) => {
    await waitForOverview(I);
    const badge = '.md-dashboard__welcome-meta .md-dashboard__environment';
    const original = await I.executeScript(() => ({ ...document.querySelector('webjet-overview-dashboard').config }));
    const configureEnvironment = async config => {
        I.executeScript(config => {
            const dashboard = document.querySelector('webjet-overview-dashboard');
            dashboard.configure({ data: dashboard.data, labels: dashboard.labels, config: { ...dashboard.config, ...config } });
        }, config);
        await waitForOverview(I);
    };
    await configureEnvironment({ environmentName: 'PROD/autotest-node', environmentIcon: 'auto' });
    I.see('PROD/autotest-node', badge);
    await configureEnvironment({ environmentName: 'DEV/' });
    I.assertEqual(await I.grabTextFrom(badge), 'DEV', 'An empty cluster name must not leave a trailing slash.');
    await configureEnvironment({ environmentName: 'Autotest environment', environmentType: 'INT', environmentIcon: 'ti-server' });
    I.see('Autotest environment', badge);
    I.seeElement(`${badge} .ti-server`);
    await configureEnvironment({ environmentName: '   ' });
    I.dontSeeElement(badge);
    I.seeElement('.md-dashboard__eyebrow');
    I.seeElement('.md-dashboard__greeting');
    await configureEnvironment(original);
});

/**
 * Checks that every active session remains reachable as the welcome area changes height and on mobile.
 * Scrolling stays inside the session list, and keyboard users can read and dismiss the current-session and
 * logout hints.
 */
Scenario('Session scrolling stays inside its list and compact controls expose accessible tooltips', async ({ I }) => {
    I.resizeWindow(1337, 1052);
    const sessionSettings = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings);
    sessionSettings.configured = true;
    sessionSettings.acknowledgedNewsVersion = null;
    // Keep collapse/expand mutations isolated even when this scenario runs without the earlier design fixtures.
    await I.mockRoute(settingsRoute, route => {
        if (route.request().method() === 'PUT') Object.assign(sessionSettings, route.request().postDataJSON());
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(sessionSettings) });
    });
    const currentSessions = {
        currentSessionId: 'session-autotest-0', userSessions: [{ cluster: 'autotest', userSessions: Array.from({ length: 9 }, (_, index) => ({
            sessionId: `session-autotest-${index}`, browserName: ['Chrome 153', 'Safari 18', 'Firefox 131'][index % 3],
            logonTime: Date.now() - index * 60000, remoteAddr: '127.0.0.1'
        })) }]
    };
    await mockDashboardBootstrap(I, () => ({ settings: sessionSettings, currentSessions }));
    I.refreshPage();
    await waitForOverview(I);
    I.executeScript(() => {
        const dashboard = document.querySelector('webjet-overview-dashboard');
        dashboard.configure({ data: dashboard.data,
            config: { ...dashboard.config, heroBackgroundImage: '/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg' },
            labels: { ...dashboard.labels, changelog:
            '<p>WebJET CMS 2026.18 autotest release includes approval workflows for folders and accessibility checks for published content.</p>'
            + '<p>Autotest editors can maximize application dialogs, configure accessible labels and continue editing their pages with the latest administration tools.</p>'
            + '<p>Autotest security updates include additional authentication providers, passkeys and improvements for installations running on several cluster nodes.</p>'
        } });
    });
    await waitForOverview(I);
    const list = '.md-dashboard__sessions .md-dashboard-widget__sessions';
    I.seeNumberOfElements(`${list} > li`, 9);
    I.seeElement(`${list} .ti-brand-chrome`);
    I.seeElement(`${list} .ti-brand-safari`);
    I.seeElement(`${list} .ti-brand-firefox`);
    I.seeElement(`${list} > li:first-child .md-dashboard-widget__session-current[aria-label]`);
    I.dontSeeElement(`${list} > li:first-child button`);
    I.seeNumberOfElements(`${list} .md-dashboard-widget__session-logout`, 8);
    I.clickCss(newsToggle);
    waitForSave(I);
    I.waitForVisible('.is-news-collapsed .md-dashboard-widget__news-summary', 10);
    I.seeNumberOfElements(`${list} > li`, 9);
    I.clickCss(newsToggle);
    waitForSave(I);
    I.waitForVisible('.md-dashboard-widget__news-highlights', 10);
    // Save notifications overlap the session list's pointer target until dismissed.
    I.toastrClose();
    I.waitForInvisible('#toast-container-webjet .toast-success', 15);
    I.executeScript(() => { window.scrollbarMain.setMomentum(0, 0); window.scrollbarMain.setPosition(0, 0); });
    // Standard scroll helpers do not generate wheel events, which trigger the smooth-scrollbar regression.
    await I.usePlaywrightTo('wheel over the native session list', async ({ page }) => {
        const bounds = await page.locator(list).boundingBox();
        await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + 30);
        await page.mouse.wheel(0, 130);
    });
    I.waitForFunction(selector => document.querySelector(selector).scrollTop > 0, [list], 10);
    I.assertEqual(await I.executeScript(() => window.scrollbarMain.offset.y), 0, 'Wheel input must not scroll the dashboard behind the session list.');
    I.executeScript(selector => { const node = document.querySelector(selector); node.focus({ preventScroll: true }); node.scrollTop = 0; }, list);
    I.pressKey('PageDown');
    I.waitForFunction(selector => document.querySelector(selector).scrollTop > 0, [list], 10);
    I.assertEqual(await I.executeScript(() => window.scrollbarMain.offset.y), 0, 'Keyboard scrolling must stay inside the focused list.');
    const current = `${list} .md-dashboard-widget__session-current`;
    I.executeScript(selector => { const node = document.querySelector(selector); node.closest('ul').scrollTop = 0; node.focus({ preventScroll: true }); }, current);
    I.waitForVisible('.tooltip.show', 10);
    I.see(await I.grabAttributeFrom(current, 'aria-label'), '.tooltip.show');
    I.pressKey('Escape');
    I.waitForInvisible('.tooltip.show', 10);
    const logout = `${list} .md-dashboard-widget__session-logout`;
    I.executeScript(selector => document.querySelector(selector).focus({ preventScroll: true }), logout);
    I.waitForVisible('.tooltip.show', 10);
    I.see(await I.grabAttributeFrom(`${list} > li:nth-child(2) .md-dashboard-widget__session-logout`, 'aria-label'), '.tooltip.show');
    I.pressKey('Escape');
    I.waitForInvisible('.tooltip.show', 10);
    I.resizeWindow(390, 1052);
    if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
    I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
    I.seeNumberOfElements(`${list} > li`, 9);
    I.executeScript(selector => document.querySelector(selector).focus(), list);
    I.pressKey('End');
    I.waitForFunction(selector => document.querySelector(selector).scrollTop > 0, [list], 10);
    const lastLogout = `${list} > li:last-child .md-dashboard-widget__session-logout`;
    I.executeScript(selector => document.querySelector(selector).focus(), lastLogout);
    I.waitForVisible('.tooltip.show', 10);
    I.see(await I.grabAttributeFrom(lastLogout, 'aria-label'), '.tooltip.show');
    I.pressKey('Escape');
    I.waitForInvisible('.tooltip.show', 10);
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(dashboardPageRoute);
});

/**
 * Checks that releasing a dragged widget over its original position does not save a layout change.
 */
Scenario('Releasing a dragged widget in its original position does not save preferences', async ({ I }) => {
    let settingsWrites = 0;
    const dragSettings = {
        version: 1, configured: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: 'drag-autotest-forms', type: 'forms', size: '1x1', options: { days: 7 } },
            { id: 'drag-autotest-traffic', type: 'traffic', size: '3x3', options: { days: 7 } }
        ]
    };
    // Interception keeps drag verification independent of the account's saved layout.
    await I.mockRoute(settingsRoute, route => {
        if (route.request().method() !== 'GET') settingsWrites++;
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(dragSettings) });
    });
    await mockDashboardBootstrap(I, () => ({ settings: dragSettings }));
    I.resizeWindow(1337, 1052);
    I.refreshPage();
    await waitForOverview(I);
    I.clickCss(editButton);
    I.waitForElement('.md-dashboard.is-editing', 10);
    const helper = 'body > .md-dashboard__widget.ui-draggable-dragging';
    for (const type of ['forms', 'traffic']) {
        const source = `.md-dashboard__layout [data-instance-id="drag-autotest-${type}"]`;
        I.executeScript(selector => {
            const scrollbar = window.scrollbarMain;
            scrollbar.setMomentum(0, 0);
            scrollbar.update();
            scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector(selector).getBoundingClientRect().top - 160);
        }, source);
        // The standard drag helper releases the pointer before the in-flight card can be inspected.
        await I.usePlaywrightTo('hold a dashboard widget during dragging', async ({ page }) => {
            const handle = await page.locator(`${source} .md-dashboard__drag`).boundingBox();
            await page.mouse.move(handle.x + handle.width / 2, handle.y + handle.height / 2);
            await page.mouse.down();
            await page.mouse.move(handle.x + handle.width / 2 - 60, handle.y + handle.height / 2 + 40, { steps: 8 });
        });
        I.waitForVisible(helper, 10);
        // The pointer remains over the original card, so releasing it must not reorder widgets.
        await I.usePlaywrightTo('release the dashboard widget over its original position', async ({ page }) => { await page.mouse.up(); });
        I.waitForInvisible(helper, 10);
        I.waitForFunction(() => !document.querySelector('.md-dashboard.is-dragging'), 10);
    }
    I.assertEqual(settingsWrites, 0, 'Inspecting a drag without changing its position must not save preferences.');
    I.clickCss(editButton);
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(dashboardPageRoute);
    I.wjSetDefaultWindowSize();
});

/**
 * Removes the temporary display data and simulated responses, restores the window size and confirms that the
 * real account preferences were never changed by the interaction checks.
 */
Scenario('Remove interaction fixtures and verify the account preferences were never changed', async ({ I }) => {
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(dashboardPageRoute);
    await I.stopMockingRoute(recentPagesRoute);
    await I.stopMockingRoute(searchTermsRoute);
    await I.stopMockingRoute(topPagesRoute);
    await I.stopMockingRoute(missingThumbnailRoute);
    I.wjSetDefaultWindowSize();
    I.refreshPage();
    await waitForOverview(I);
    if (!originalSettings) return;
    const settings = (await I.executeScript(readDashboardBootstrap)).settings;
    I.assertDeepEqual(settings, originalSettings, 'Interaction fixtures must never mutate the real dashboard preferences.');
});
