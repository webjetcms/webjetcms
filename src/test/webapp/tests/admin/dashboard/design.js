const { waitForWidgets, mockDashboardBootstrap, dashboardPageRoute, readDashboardBootstrap } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.design').tag('@singlethread');

let originalSettings;
let previewSettings;

const settingsRoute = '**/admin/rest/dashboard/settings';
const recentPagesRoute = '**/admin/rest/web-pages/all?*';
const searchTermsRoute = '**/admin/rest/stat/search-engines/search/findByColumns?*';
const topPagesRoute = '**/admin/rest/stat/top/search/findByColumns?*';
const missingThumbnailRoute = '**/thumb/images/autotest-dashboard-missing.jpg?*';
const editButton = '.md-dashboard__toolbar-actions > button[aria-pressed]';

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
 * Checks that release news stays in the welcome area, notice actions stay visible, and
 * arrangement controls appear only in edit mode. Cancelling keyboard movement must return focus without
 * hiding notice actions.
 */
Scenario('Pinned security, inline notices and edit mode keep the dashboard readable', async ({ I }) => {
    I.resizeWindow(1440, 1100);
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
    I.seeElement('.md-dashboard__news [data-widget-type="news"]');
    I.dontSeeElementInDOM('.md-dashboard__sessions');
    I.dontSeeElement('.md-dashboard__layout [data-widget-type="sessions"]');
    I.dontSeeElement('.md-dashboard__sessions .md-dashboard__widget-controls');
    I.dontSeeElement('.md-dashboard__edit-control');
    I.seeElement('.md-dashboard__shortcut-actions button[aria-pressed]');

    const firstNotice = '[data-notice-id="design-autotest-migration"]';
    const secondNotice = '[data-notice-id="design-autotest-security"]';
    I.see('Database migration autotest', `${firstNotice} .md-dashboard__notice-title`);
    I.see('Account protection autotest', `${secondNotice} .md-dashboard__notice-title`);
    I.see('Statistics require a conversion autotest.', firstNotice);
    I.see('Enable a second verification factor autotest.', secondNotice);
    // Inspect notice actions without executing real migration or account-security operations.
    I.see('Migration action autotest', `${firstNotice} .md-dashboard__notice-action`);
    I.see('Security action autotest', `${secondNotice} .md-dashboard__notice-action`);

    I.clickCss(editButton);
    I.waitForElement('.md-dashboard.is-editing', 10);
    I.seeElement('.md-dashboard__toolbar .md-dashboard__edit-control');
    I.seeElement('[data-instance-id="design-autotest-pages"] .md-dashboard__drag');
    I.seeElement('.md-dashboard__news [data-widget-type="news"]');
    const handle = '[data-instance-id="design-autotest-pages"] .md-dashboard__drag';
    I.executeScript(selector => document.querySelector(selector).focus(), handle);
    I.pressKey('Enter');
    I.waitForElement('.md-dashboard__widget-drag-helper', 10);
    I.pressKey('Escape');
    I.dontSeeElement('.md-dashboard__widget-drag-helper');
    I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), handle), 'Cancelling keyboard movement must return focus to its handle.');
    I.clickCss(editButton);
    I.dontSeeElement('.md-dashboard__edit-control');
    I.seeElement(`${firstNotice} .md-dashboard__notice-action`);
    I.seeElement(`${secondNotice} .md-dashboard__notice-action`);
});


/**
 * Checks that the welcome area, search, notices, widgets and shortcuts fit mobile, tablet and desktop widths
 * without extending beyond the dashboard.
 */
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
    I.dontSeeElement(addWidget);
    I.dontSeeElement(resetWidget);
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
        I.clickCss('#feedback_modal .md-feedback__cancel');
        I.waitForInvisible('#feedback_modal', 10);
        I.waitForFunction(selector => document.activeElement === document.querySelector(selector), [feedback], 10);
        I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), feedback),
            'Canceling feedback must restore focus to its toolbar action.');

        I.clickCss(editButton);
        I.dontSeeElement(feedback);
        I.seeElement(addWidget);
        I.seeElement(resetWidget);
        I.clickCss(addWidget);
        I.waitForVisible('.md-dashboard-modal input[type="search"]', 10);
        I.waitForFunction(() => document.querySelector('.md-dashboard-modal')?.contains(document.activeElement), 10);
        const layout = await I.executeScript(() => {
            const modal = document.querySelector('.md-dashboard-modal');
            const header = modal.querySelector('.modal-header');
            const close = header.querySelector('button.btn-close');
            const content = modal.querySelector('.modal-content').getBoundingClientRect();
            return {
                closeLabel: close.getAttribute('aria-label'), expectedLabel: WJ.translate('admin.dashboard.close.js'),
                horizontalOverflow: content.left < 0 || content.right > window.innerWidth,
                hasReset: Boolean(modal.querySelector('.md-dashboard__reset'))
            };
        });
        I.assertEqual(layout.closeLabel, layout.expectedLabel, 'The close icon must have a localized accessible name.');
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
        I.waitForFunction(selector => document.activeElement === document.querySelector(selector), [resetWidget], 10);
        I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), resetWidget),
            'Canceling reset must return focus to its toolbar action without changing preferences.');
        I.waitForInvisible('.tooltip.wj-tooltip-hoverable.show', 10);
        I.dontSeeElement('.tooltip.wj-tooltip-hoverable.show');
        I.clickCss(editButton);
    }
    I.wjSetDefaultWindowSize();
});

/**
 * Checks that compact counts and preview cards align on desktop and fit a narrow screen. All six recent
 * pages must remain reachable by scrolling their own list with the mouse or keyboard.
 */
Scenario('Compact metrics and scrollable recent pages align above three equal preview cards', async ({ I }) => {
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
        return { type, height: card.getBoundingClientRect().height, href: card.querySelector('.md-dashboard__title-link')?.getAttribute('href') };
    }));
    const targets = { forms: '/apps/form/admin/', approvals: '/admin/v9/webpages/web-pages-list/?show=toapprove', errors: '/apps/stat/admin/error/' };
    metrics.forEach(metric => {
        I.assertTrue(metric.height >= 120 && metric.height <= 145, `${metric.type} must fit the compact metric row without clipping (actual height: ${metric.height}px).`);
        I.assertEqual(metric.href, targets[metric.type], 'A metric heading must open the corresponding module.');
    });
    I.assertEqual(await I.grabTextFrom('[data-widget-type="forms"] .md-dashboard-widget__metric-label'),
        await I.executeScript(() => WJ.translate('admin.dashboard.totalSubmissions.js')));
    I.dontSeeElementInDOM('[data-widget-type="forms"] .md-dashboard-widget__period');
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

/**
 * Checks that search-term and popular-page cards keep long labels and numeric columns readable at different
 * widths. Page previews must show a thumbnail or a matching fallback icon when the image is missing.
 */
Scenario('Search queries and top pages share balanced cards and readable numeric columns', async ({ I }) => {
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


/**
 * Removes the temporary display data and simulated responses, restores the window size and confirms that the
 * real account preferences were never changed by the design checks.
 */
Scenario('Remove design fixtures and verify the account preferences were never changed', async ({ I }) => {
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
    I.assertDeepEqual(settings, originalSettings, 'Visual regression fixtures must never mutate the real dashboard preferences.');
});
