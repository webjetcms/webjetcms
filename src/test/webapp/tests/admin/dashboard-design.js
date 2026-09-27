const { waitForWidgets } = require('../../helpers/dashboard-browser');

Feature('admin.dashboard-design').tag('@singlethread');

let originalSettings;
let previewSettings;
let noticesToken;

const settingsRoute = '**/admin/rest/dashboard/settings';
const noticesRoute = '**/admin/rest/dashboard/notices';
const sessionsRoute = '**/admin/rest/dashboard/data/sessions*';
const recentPagesRoute = '**/admin/rest/dashboard/recent-pages';
const searchTermsRoute = '**/admin/rest/dashboard/data/search-terms*';
const topPagesRoute = '**/admin/rest/dashboard/data/top-pages*';
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

Scenario('Pinned security, independent notices and edit mode keep the dashboard readable', async ({ I }) => {
    await waitForOverview(I);
    I.resizeWindow(1440, 1100);
    I.saveScreenshot('dashboard-implementation-desktop.png', true);
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        const top = document.querySelector('[data-widget-type="traffic"]').getBoundingClientRect().top;
        scrollbar.setPosition(0, scrollbar.offset.y + top - 64);
    });
    I.saveScreenshot('dashboard-implementation-traffic.png', false);
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        const sources = document.querySelector('[data-widget-type="referrers"]');
        if (sources) scrollbar.setPosition(0, scrollbar.offset.y + sources.getBoundingClientRect().top - 64);
    });
    I.saveScreenshot('dashboard-implementation-refined-row.png', false);
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        const queries = document.querySelector('[data-widget-type="search-terms"]');
        if (queries) scrollbar.setPosition(0, scrollbar.offset.y + queries.getBoundingClientRect().top - 64);
    });
    I.saveScreenshot('dashboard-implementation-ranked-row.png', false);
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
    I.saveScreenshot('dashboard-design-desktop.png', true);
});

Scenario('Release notes collapse to a persistent summary and can be expanded again', async ({ I }) => {
    await waitForOverview(I);
    I.seeElement('.md-dashboard-widget__news-highlights');
    I.seeElement('.md-dashboard-widget__news-actions .md-dashboard-widget__news-more');
    I.seeElement('.md-dashboard-widget__news-actions .md-dashboard-widget__news-toggle');
    I.assertTrue(await I.executeScript(() => {
        const original = new DOMParser().parseFromString(document.querySelector('webjet-overview-dashboard').labels.changelog, 'text/html');
        return document.querySelector('.md-dashboard-widget__news-highlights').innerHTML === original.body.innerHTML;
    }), 'The full original Markdown announcement must be retained.');
    I.assertFalse(await I.executeScript(() => document.querySelector('.md-dashboard-widget__news-highlights').textContent.includes('\\n')), 'Translation paragraph escapes must render as Markdown line breaks.');
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

Scenario('Feedback toolbar and widget catalogue keep familiar dialog controls on desktop and mobile', async ({ I }) => {
    await waitForOverview(I);
    const feedback = '.md-dashboard__feedback';
    const addWidget = '.md-dashboard__toolbar-actions > .md-dashboard__edit-control:not(.md-dashboard__reset)';
    const resetWidget = '.md-dashboard__toolbar-actions > .md-dashboard__reset';
    const resetDialog = '#toast-container-webjet .toast[role="dialog"]';
    I.assertTrue(await I.executeScript(() => {
        const feedback = document.querySelector('.md-dashboard__feedback');
        return feedback.nextElementSibling.matches('.md-dashboard__edit-control[hidden]')
            && feedback.nextElementSibling.nextElementSibling.matches('.md-dashboard__reset[hidden]')
            && feedback.nextElementSibling.nextElementSibling.nextElementSibling.matches('button[aria-pressed]');
    }), 'Feedback must precede Add widget, Reset and the overview edit control.');
    I.dontSeeElementInDOM('.md-dashboard__legacy');
    I.seeNumberOfElements('.md-dashboard__feedback', 1);

    for (const width of [1337, 390]) {
        I.resizeWindow(width, 1052);
        if (width < 768 && await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
        if (width < 768) I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
        I.executeScript(() => {
            const scrollbar = window.scrollbarMain;
            scrollbar.setMomentum(0, 0);
            scrollbar.update();
            scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('.md-dashboard__toolbar').getBoundingClientRect().top - 80);
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
        I.assertTrue(await I.executeScript(() => {
            const feedback = document.querySelector('.md-dashboard__feedback');
            return feedback.nextElementSibling.matches('.md-dashboard__edit-control:not([hidden])')
                && feedback.nextElementSibling.nextElementSibling.matches('.md-dashboard__reset:not([hidden])')
                && feedback.nextElementSibling.nextElementSibling.nextElementSibling.matches('button[aria-pressed="true"]');
        }), 'Edit mode must keep Add widget and Reset between Feedback and Done.');
        I.clickCss(addWidget);
        I.waitForVisible('.md-dashboard-modal input[type="search"]', 10);
        I.waitForFunction(() => document.querySelector('.md-dashboard-modal')?.contains(document.activeElement), 10);
        const layout = await I.executeScript(() => {
            const modal = document.querySelector('.md-dashboard-modal');
            const header = modal.querySelector('.modal-header');
            const title = header.querySelector('.modal-title').getBoundingClientRect();
            const close = header.querySelector('button.btn-close');
            const closeBounds = close.getBoundingClientRect();
            const content = modal.querySelector('.modal-content').getBoundingClientRect();
            return {
                closeLabel: close.getAttribute('aria-label'), expectedLabel: WJ.translate('admin.dashboard.close.js'),
                closeIcon: Boolean(close.querySelector('.ti-x[aria-hidden="true"]')), closeText: close.textContent.trim(),
                closeRightOfTitle: closeBounds.left >= title.right,
                centersDifference: Math.abs(closeBounds.top + closeBounds.height / 2 - title.top - title.height / 2),
                horizontalOverflow: content.left < 0 || content.right > window.innerWidth,
                hasReset: Boolean(modal.querySelector('.md-dashboard__reset'))
            };
        });
        I.assertEqual(layout.closeLabel, layout.expectedLabel, 'The close icon must have a localized accessible name.');
        I.assertTrue(layout.closeIcon && layout.closeText === '', 'The header must use the standard X icon without a second text row.');
        I.assertTrue(layout.closeRightOfTitle && layout.centersDifference <= 2, `The title and close icon must share one aligned header row at ${width}px.`);
        I.assertFalse(layout.hasReset, 'Reset belongs in the overview toolbar instead of the widget catalogue.');
        I.assertFalse(layout.horizontalOverflow, `The catalogue must fit the ${width}px viewport.`);
        I.saveScreenshot(`dashboard-catalogue-${width}.png`, false);
        I.clickCss('.md-dashboard-modal .modal-header button.btn-close');
        I.waitForFunction(() => !document.querySelector('.md-dashboard-modal'), 10);
        I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), addWidget),
            'Closing the catalogue must return focus to Add widget.');
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
            const content = confirmation.getBoundingClientRect();
            return content.left >= 0 && content.right <= window.innerWidth
                && document.getElementById(confirmation.getAttribute('aria-describedby'))?.textContent.length > 0;
        }), `Reset must explain its scope in an accessible confirmation that fits the ${width}px viewport.`);
        I.saveScreenshot(`dashboard-reset-confirmation-${width}.png`, false);
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

Scenario('Compact metrics and scrollable recent pages align above three equal preview cards', async ({ I }) => {
    previewSettings.items = [
        { id: 'compact-autotest-traffic', type: 'traffic', size: '3x3', collapsed: false, options: { days: 7 } },
        ...['forms', 'approvals', 'errors'].map(type => ({ id: `compact-autotest-${type}`, type, size: '1x1', collapsed: false, options: { days: 7 } })),
        { id: 'compact-autotest-pages', type: 'recent-pages', size: '3x2', collapsed: false, options: {} },
        ...['referrers', 'publishing', 'newsletter'].map(type => ({ id: `compact-autotest-${type}`, type, size: '2x2', collapsed: false, options: { days: 7 } }))
    ];
    await I.mockRoute(recentPagesRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(
        Array.from({ length: 6 }, (_, index) => ({ docId: index + 1, title: `Recent page autotest ${index + 1}`,
            fullPath: `/Autotest section/Recent page autotest ${index + 1}`, saveDate: '26.09.2026 10:00', perexImage: '' }))
    ) }));
    I.resizeWindow(1337, 1052);
    I.refreshPage();
    await waitForOverview(I);
    const metrics = await I.executeScript(() => ['forms', 'approvals', 'errors'].map(type => {
        const card = document.querySelector(`[data-widget-type="${type}"]`);
        return { type, height: card.getBoundingClientRect().height, href: card.querySelector('.md-dashboard__title-link')?.getAttribute('href') };
    }));
    const targets = { forms: '/apps/form/admin/', approvals: '/admin/v9/webpages/web-pages-list/?show=toapprove', errors: '/apps/stat/admin/error/' };
    metrics.forEach(metric => {
        I.assertTrue(metric.height >= 120 && metric.height <= 145, `${metric.type} must fit the compact metric row without clipping (actual height: ${metric.height}px).`);
        I.assertEqual(metric.href, targets[metric.type], 'A metric heading must open the corresponding module.');
    });
    I.assertEqual(await I.grabTextFrom('[data-widget-type="forms"] .md-dashboard-widget__metric-label'),
        await I.executeScript(() => WJ.translate('admin.dashboard.formSubmissionsPeriod.js', 7)));
    I.assertTrue(await I.executeScript(() => document.querySelector('[data-widget-type="forms"] .md-dashboard-widget__period').getBoundingClientRect().height <= 1),
        'Exact form dates remain accessible without taking another visual row.');
    I.executeScript(() => document.querySelector('[data-widget-type="forms"] .md-dashboard__title-link').focus());
    I.assertTrue(await I.executeScript(() => document.activeElement.matches('[data-widget-type="forms"] .md-dashboard__title-link')), 'Metric header navigation must support keyboard focus.');
    const bounds = await I.executeScript(() => Object.fromEntries(['traffic', 'recent-pages', 'referrers', 'publishing', 'newsletter'].map(type => {
        const rect = document.querySelector(`[data-widget-type="${type}"]`).getBoundingClientRect();
        return [type, { top: rect.top, bottom: rect.bottom, width: rect.width, height: rect.height }];
    })));
    I.assertTrue(Math.abs(bounds.traffic.bottom - bounds['recent-pages'].bottom) <= 1, 'The traffic chart and recent-page list must share a clean bottom edge.');
    for (const type of ['publishing', 'newsletter']) {
        I.assertTrue(Math.abs(bounds.referrers.top - bounds[type].top) <= 1, `${type} must share the sources card row.`);
        I.assertTrue(Math.abs(bounds.referrers.width - bounds[type].width) <= 1, `${type} must have the same two-column width.`);
        I.assertTrue(Math.abs(bounds.referrers.height - bounds[type].height) <= 1, `${type} must have the same preview height.`);
    }
    for (const type of ['referrers', 'publishing', 'newsletter']) {
        I.seeElement(`[data-widget-type="${type}"] .md-dashboard__title-link`);
        I.dontSeeElement(`[data-widget-type="${type}"] .md-dashboard-widget__more`);
    }
    I.dontSeeElement('[data-widget-type="referrers"] details');
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
    I.saveScreenshot('dashboard-design-aligned-previews.png', false);
    I.resizeWindow(390, 1052);
    if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
    I.assertTrue(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__widget[data-size="1x1"]')]
        .every(card => card.scrollWidth <= card.clientWidth + 1)), 'Compact metric content must fit narrow cards.');
    I.resizeWindow(1337, 1052);
    await I.stopMockingRoute(recentPagesRoute);
});

Scenario('Search queries and top pages share balanced cards and readable numeric columns', async ({ I }) => {
    const previousItems = previewSettings.items;
    previewSettings.items = ['search-terms', 'top-pages'].map(type => ({ id: `ranked-autotest-${type}`, type, size: '3x3', collapsed: false, options: { days: 7 } }));
    const from = Date.UTC(2026, 8, 19), to = Date.UTC(2026, 8, 25);
    await I.mockRoute(searchTermsRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ from, to, items: [
        { title: 'Autotest vyhľadávací výraz s veľmi dlhým opisným názvom', value: 128, url: '/apps/stat/admin/search-engines/' },
        { title: 'AutotestDlhýVýrazBezMedzierOverujúciZalomenieTextu', value: 75, url: '/apps/stat/admin/search-engines/' },
        { title: 'Autotest kontakt', value: 6, url: '/apps/stat/admin/search-engines/' }
    ] }) }));
    await I.mockRoute(topPagesRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ from, to, items: [
        { title: 'Autotest veľmi dlhý názov najnavštevovanejšej stránky', section: '/Autotest sekcia/Podrobné informácie', perexImage: '/images/zo-sveta-financii/konsolidacia-napriec-trhmi/oil-pump.jpg', value: 128, previous: 100, url: '/admin/v9/webpages/web-pages-list/?docid=1' },
        { title: 'AutotestDlhýNázovStránkyBezMedzier', section: '/AutotestSekciaBezMedzier', perexImage: '', value: 75, previous: 90, url: '/admin/v9/webpages/web-pages-list/?docid=2' },
        { title: 'Autotest kontakt', section: '/Autotest', perexImage: '/images/autotest-dashboard-missing.jpg', value: 6, previous: 6, url: '/admin/v9/webpages/web-pages-list/?docid=3' }
    ] }) }));
    await I.mockRoute(missingThumbnailRoute, route => route.fulfill({ status: 404, contentType: 'text/plain', body: 'Missing autotest thumbnail' }));
    I.resizeWindow(1337, 1052);
    I.refreshPage();
    await waitForOverview(I);
    const targets = { 'search-terms': '/apps/stat/admin/search-engines/', 'top-pages': '/apps/stat/admin/top/' };
    for (const type of Object.keys(targets)) {
        I.assertEqual(await I.grabAttributeFrom(`[data-widget-type="${type}"] .md-dashboard__title-link`, 'href'), targets[type]);
        I.dontSeeElement(`[data-widget-type="${type}"] .md-dashboard-widget__more`);
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
    I.assertEqual(await I.grabAttributeFrom(`${pagesTable} tbody tr:first-child img`, 'src'), '/thumb/images/zo-sveta-financii/konsolidacia-napriec-trhmi/oil-pump.jpg?w=76&h=76&ip=6');
    I.assertEqual(await I.grabAttributeFrom(`${pagesTable} tbody tr:first-child img`, 'alt'), '');
    I.seeElement(`${pagesTable} tbody tr:nth-child(2) .ti-file-text:not([hidden])`);
    I.seeElement(`${pagesTable} tbody tr:nth-child(3) .ti-file-text:not([hidden])`);
    I.assertTrue(await I.executeScript(selector => [...document.querySelectorAll(`${selector} .md-dashboard-widget__page-image`)].every(image => {
        const rect = image.getBoundingClientRect();
        return Math.abs(rect.width - 38) <= 1 && Math.abs(rect.height - 38) <= 1;
    }), pagesTable), 'Real images and file-icon fallbacks must occupy the same compact thumbnail area.');
    I.saveScreenshot('dashboard-design-page-previews.png', false);
    for (const width of [1337, 1000, 390]) {
        I.resizeWindow(width, 1052);
        if (width < 768 && await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
        if (width < 768) I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
        const cards = await I.executeScript(() => ['search-terms', 'top-pages'].map(type => {
            const card = document.querySelector(`[data-widget-type="${type}"]`);
            const table = card.querySelector('.md-dashboard-widget__table--ranked');
            const rect = card.getBoundingClientRect();
            const titleWidth = table.querySelector('tbody td:first-child').getBoundingClientRect().width;
            return { type, width: rect.width, top: rect.top, background: getComputedStyle(card).backgroundColor,
                fits: card.scrollWidth <= card.clientWidth + 1 && table.scrollWidth <= table.clientWidth + 1
                    && [...table.querySelectorAll('th, td')].every(cell => cell.scrollWidth <= cell.clientWidth + 1),
                numbers: [...table.querySelectorAll('.md-dashboard-widget__table-number')].map(cell => ({
                    tag: cell.tagName, rightAligned: getComputedStyle(cell).textAlign === 'right', verticallyCentered: getComputedStyle(cell).verticalAlign === 'middle',
                    noWrap: getComputedStyle(cell).whiteSpace === 'nowrap', narrower: cell.getBoundingClientRect().width < titleWidth
                })) };
        }));
        I.assertTrue(Math.abs(cards[0].width - cards[1].width) <= 1, `Both ranked cards must have equal widths at ${width}px.`);
        if (width === 1337) I.assertTrue(Math.abs(cards[0].top - cards[1].top) <= 1, 'The ranked cards must share a desktop row.');
        I.assertNotEqual(cards[0].background, cards[1].background, 'Search queries must retain their subtle blue surface.');
        for (const card of cards) {
            I.assertTrue(card.fits, `${card.type} must wrap long titles and sections without horizontal overflow at ${width}px.`);
            I.assertEqual(card.numbers.length, card.type === 'top-pages' ? 8 : 4, 'Numeric styling must cover the header and every data cell.');
            I.assertTrue(card.numbers.every(cell => cell.rightAligned && cell.noWrap && cell.narrower), `${card.type} numeric columns must stay compact, right aligned and unwrapped at ${width}px.`);
            if (card.type === 'top-pages') I.assertTrue(card.numbers.filter(cell => cell.tag === 'TD').every(cell => cell.verticallyCentered), 'Page counts and changes must remain vertically centered beside the thumbnail preview.');
        }
    }
    previewSettings.items.find(item => item.type === 'top-pages').size = '2x3';
    I.resizeWindow(1337, 1052);
    I.refreshPage();
    await waitForOverview(I);
    I.seeNumberOfElements(`${pagesTable} thead th`, 3);
    I.seeNumberOfElements(`${pagesTable} tbody .md-dashboard-widget__page-preview`, 3);
    I.seeNumberOfElements(`${pagesTable} tbody .md-dashboard-widget__table-number`, 6);
    previewSettings.items = previousItems;
    await I.stopMockingRoute(searchTermsRoute);
    await I.stopMockingRoute(topPagesRoute);
    await I.stopMockingRoute(missingThumbnailRoute);
    I.resizeWindow(1337, 1052);
});

Scenario('Environment badge uses configured identity and readable colors, and disappears when empty', async ({ I }) => {
    await waitForOverview(I);
    const badge = '.md-dashboard__welcome-meta .md-dashboard__environment';
    const original = await I.executeScript(() => {
        const config = document.querySelector('webjet-overview-dashboard').config;
        return { environmentName: config.environmentName, environmentType: config.environmentType, environmentIcon: config.environmentIcon, environmentColor: config.environmentColor };
    });
    I.assertTrue(/^DEV(?:\/.*)?$/.test(original.environmentName), 'The server must expand the environment and optional current node macros.');
    I.assertEqual(original.environmentType, 'DEV', 'The server must detect DEV for iwcm.interway.sk.');
    I.see('DEV', badge);
    I.seeElement(`${badge} .ti-code`);
    const configureEnvironment = async config => {
        I.executeScript(config => {
            const dashboard = document.querySelector('webjet-overview-dashboard');
            dashboard.configure({ data: dashboard.data, labels: dashboard.labels, config: { ...dashboard.config, ...config } });
        }, config);
        await waitForOverview(I);
    };
    const readColors = () => I.executeScript(selector => {
        const style = getComputedStyle(document.querySelector(selector));
        const canvas = document.createElement('canvas');
        canvas.width = canvas.height = 1;
        const context = canvas.getContext('2d', { willReadFrequently: true });
        const luminance = color => {
            context.fillStyle = color;
            context.fillRect(0, 0, 1, 1);
            return [...context.getImageData(0, 0, 1, 1).data].slice(0, 3)
                .map(channel => channel / 255).map(channel => channel <= .04045 ? channel / 12.92 : ((channel + .055) / 1.055) ** 2.4)
                .reduce((value, channel, index) => value + channel * [.2126, .7152, .0722][index], 0);
        };
        const background = luminance(style.backgroundColor), foreground = luminance(style.color);
        return { background: style.backgroundColor, borderDarker: luminance(style.borderTopColor) < background,
            borderVisible: parseFloat(style.borderTopWidth) > 0 && style.borderTopStyle !== 'none',
            contrast: (Math.max(background, foreground) + .05) / (Math.min(background, foreground) + .05) };
    }, badge);
    for (const [environment, expected, icon] of [
        ['PROD', 'rgb(214, 245, 239)', 'ti-server'],
        ['UAT', 'rgb(255, 242, 201)', 'ti-clipboard-check'],
        ['INT', 'rgb(255, 224, 178)', 'ti-git-merge'],
        ['DEV', 'rgb(255, 217, 222)', 'ti-code']
    ]) {
        await configureEnvironment({ environmentName: `${environment}/autotest-node`, environmentColor: 'auto', environmentIcon: 'auto' });
        I.see(`${environment}/autotest-node`, badge);
        I.seeElement(`${badge} .${icon}`);
        const colors = await readColors();
        I.assertEqual(colors.background, expected, 'Automatic colors must match the environment.');
        I.assertTrue(colors.contrast >= 4.5 && colors.borderDarker && colors.borderVisible, 'Automatic colors must remain readable and outlined.');
    }
    await configureEnvironment({ environmentName: 'DEV/' });
    I.assertEqual(await I.grabTextFrom(badge), 'DEV', 'An empty cluster name must not leave a trailing slash.');
    await configureEnvironment({ environmentName: 'Custom environment', environmentType: 'INT' });
    I.seeElement(`${badge} .ti-git-merge`);
    I.assertEqual((await readColors()).background, 'rgb(255, 224, 178)', 'Custom labels must use the environment detected by the server.');
    for (const [color, expected] of [['#ffe082', 'rgb(255, 224, 130)'], ['#183153', 'rgb(24, 49, 83)']]) {
        await configureEnvironment({ environmentName: 'autotest INT', environmentIcon: 'ti-server', environmentColor: color });
        I.see('autotest INT', badge);
        I.seeElement(`${badge} .ti-server`);
        I.dontSeeElement(`${badge} .ti-database`);
        const colors = await readColors();
        I.assertEqual(colors.background, expected, 'The environment background must use the configured color.');
        I.assertTrue(colors.borderDarker && colors.borderVisible, 'A visibly darker border must define the environment label.');
        I.assertTrue(colors.contrast >= 4.5, 'Environment text must remain readable on both light and dark configured backgrounds.');
    }
    await configureEnvironment({ environmentColor: 'auto' });
    const fallback = await readColors();
    await configureEnvironment({ environmentColor: 'invalid-autotest-color' });
    I.assertDeepEqual(await readColors(), fallback, 'An invalid configured color must fall back to the readable default appearance.');
    for (const icon of ['', 'invalid-autotest-icon']) {
        await configureEnvironment({ environmentIcon: icon });
        I.seeElement(`${badge} .ti-git-merge`);
    }
    await configureEnvironment({ environmentName: 'autotest integration database', environmentIcon: 'ti-server' });
    I.resizeWindow(390, 1052);
    if (await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
    I.assertTrue(await I.executeScript(selector => {
        const badge = document.querySelector(selector), welcome = badge.closest('.md-dashboard__welcome');
        const bounds = badge.getBoundingClientRect(), container = welcome.getBoundingClientRect();
        return bounds.left >= container.left && bounds.right <= container.right && badge.scrollWidth <= badge.clientWidth + 1;
    }, badge), 'A longer environment name must fit the welcome section on a narrow screen.');
    await configureEnvironment({ environmentName: '   ' });
    I.dontSeeElement(badge);
    I.seeElement('.md-dashboard__eyebrow');
    I.seeElement('.md-dashboard__greeting');
    I.wjSetDefaultWindowSize();
    await configureEnvironment(original);
});

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
    await I.mockRoute(sessionsRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ currentSessions: {
        currentSessionId: 'session-autotest-0', userSessions: [{ cluster: 'autotest', userSessions: Array.from({ length: 9 }, (_, index) => ({
            sessionId: `session-autotest-${index}`, browserName: ['Chrome 153', 'Safari 18', 'Firefox 131'][index % 3],
            logonTime: Date.now() - index * 60000, remoteAddr: '127.0.0.1'
        })) }]
    } }) }));
    I.refreshPage();
    await waitForOverview(I);
    I.executeScript(() => {
        const dashboard = document.querySelector('webjet-overview-dashboard');
        dashboard.configure({ data: dashboard.data, config: dashboard.config, labels: { ...dashboard.labels, changelog:
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
    const heroBounds = () => I.executeScript(() => {
        const bounds = selector => {
            const rect = document.querySelector(selector).getBoundingClientRect();
            return { top: rect.top, right: rect.right, bottom: rect.bottom, height: rect.height };
        };
        const list = document.querySelector('.md-dashboard__sessions .md-dashboard-widget__sessions');
        return { hero: bounds('.md-dashboard__hero'), pane: bounds('.md-dashboard__sessions'),
            listHeight: list.clientHeight, contentHeight: list.scrollHeight, rowCount: list.children.length };
    });
    const assertFlushPane = bounds => {
        for (const edge of ['top', 'right', 'bottom']) I.assertTrue(Math.abs(bounds.hero[edge] - bounds.pane[edge]) <= 1,
            `The security pane must meet the hero's ${edge} edge without an outer padding gap.`);
        I.assertEqual(bounds.rowCount, 9, 'Resizing the security pane must retain every active session.');
        I.assertTrue(bounds.contentHeight > bounds.listHeight, 'Sessions beyond the available height must remain in a native scroll area.');
    };
    const expanded = await heroBounds();
    assertFlushPane(expanded);
    I.clickCss(newsToggle);
    waitForSave(I);
    I.waitForVisible('.is-news-collapsed .md-dashboard-widget__news-summary', 10);
    const collapsed = await heroBounds();
    assertFlushPane(collapsed);
    I.assertTrue(expanded.hero.height > collapsed.hero.height + 40, 'Collapsing release notes must reduce the whole hero height.');
    I.assertTrue(expanded.listHeight > collapsed.listHeight + 40, 'The session list must give up the same vertical space when release notes collapse.');
    I.see('9', '.md-dashboard__sessions span.md-dashboard-widget__session-count');
    I.clickCss(newsToggle);
    waitForSave(I);
    I.waitForVisible('.md-dashboard-widget__news-highlights', 10);
    const reopened = await heroBounds();
    assertFlushPane(reopened);
    I.assertTrue(Math.abs(expanded.listHeight - reopened.listHeight) <= 1, 'Reopening release notes must restore the space available to sessions.');
    I.assertTrue(await I.executeScript(() => {
        const current = document.querySelector('.md-dashboard__sessions .md-dashboard-widget__session-current');
        const row = current.closest('li');
        const marker = current.getBoundingClientRect();
        const name = row.querySelector('.md-dashboard-widget__session-name').getBoundingClientRect();
        const logout = row.nextElementSibling.querySelector('.md-dashboard-widget__session-logout').getBoundingClientRect();
        const dot = getComputedStyle(current, '::before');
        const color = dot.backgroundColor.match(/[\d.]+/g).map(Number);
        return marker.left >= name.right && Math.abs(marker.right - logout.right) <= 1
            && parseFloat(dot.width) > 0 && parseFloat(dot.height) > 0 && color[1] > color[0] && color[1] > color[2]
            && !row.querySelector('button') && Boolean(current.getAttribute('aria-label'));
    }), 'The current login must keep its accessible green dot in the logout-action column, without offering to log itself out.');
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
    I.dontSeeElement('.md-dashboard__sessions .md-dashboard-widget__session-manage');
    await I.stopMockingRoute(sessionsRoute);
    await I.stopMockingRoute(settingsRoute);
});

Scenario('Dragging preserves the widget surface, outline and dimensions', async ({ I }) => {
    let settingsWrites = 0;
    const dragSettings = {
        version: 1, configured: true, acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: 'drag-autotest-forms', type: 'forms', size: '1x1', collapsed: false, options: { days: 7 } },
            { id: 'drag-autotest-traffic', type: 'traffic', size: '3x3', collapsed: false, options: { days: 7 } }
        ]
    };
    // Interception keeps drag verification independent of the account's saved layout.
    await I.mockRoute(settingsRoute, route => {
        if (route.request().method() !== 'GET') settingsWrites++;
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(dragSettings) });
    });
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
        const appearance = await I.executeScript(({ source, helper }) => {
            const read = selector => {
                const card = document.querySelector(selector);
                const style = getComputedStyle(card);
                const bounds = card.getBoundingClientRect();
                return { background: style.backgroundColor, border: style.borderTop, width: bounds.width, height: bounds.height };
            };
            return { source: read(source), helper: read(helper) };
        }, { source, helper });
        I.assertNotEqual(appearance.helper.background, 'rgba(0, 0, 0, 0)', 'The dragged card must have a visible surface.');
        I.assertEqual(appearance.helper.background, appearance.source.background, `${type} must keep its original background while dragging.`);
        I.assertEqual(appearance.helper.border, appearance.source.border, `${type} must keep the edit-mode outline while dragging.`);
        for (const dimension of ['width', 'height']) I.assertTrue(Math.abs(appearance.helper[dimension] - appearance.source[dimension]) <= 1,
            `${type} must keep its original ${dimension} while dragging.`);
        I.saveScreenshot(`dashboard-drag-${type}.png`, false);
        // The pointer remains over the original card, so releasing it must not reorder widgets.
        await I.usePlaywrightTo('release the dashboard widget over its original position', async ({ page }) => { await page.mouse.up(); });
        I.waitForInvisible(helper, 10);
        I.waitForFunction(() => !document.querySelector('.md-dashboard.is-dragging'), 10);
    }
    I.assertEqual(settingsWrites, 0, 'Inspecting a drag without changing its position must not save preferences.');
    I.clickCss(editButton);
    await I.stopMockingRoute(settingsRoute);
    I.wjSetDefaultWindowSize();
});

Scenario('Remove design fixtures and verify the account preferences were never changed', async ({ I }) => {
    await I.stopMockingRoute(settingsRoute);
    await I.stopMockingRoute(noticesRoute);
    await I.stopMockingRoute(sessionsRoute);
    await I.stopMockingRoute(recentPagesRoute);
    await I.stopMockingRoute(searchTermsRoute);
    await I.stopMockingRoute(topPagesRoute);
    await I.stopMockingRoute(missingThumbnailRoute);
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
