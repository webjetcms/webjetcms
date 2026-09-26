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

Scenario('Session scrolling stays inside its list and compact controls expose accessible tooltips', async ({ I }) => {
    I.resizeWindow(1337, 1052);
    await I.mockRoute(sessionsRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ currentSessions: {
        currentSessionId: 'session-autotest-0', userSessions: [{ cluster: 'autotest', userSessions: Array.from({ length: 9 }, (_, index) => ({
            sessionId: `session-autotest-${index}`, browserName: ['Chrome 153', 'Safari 18', 'Firefox 131'][index % 3],
            logonTime: Date.now() - index * 60000, remoteAddr: '127.0.0.1'
        })) }]
    } }) }));
    I.refreshPage();
    await waitForOverview(I);
    const list = '.md-dashboard__sessions .md-dashboard-widget__sessions';
    I.seeNumberOfElements(`${list} > li`, 9);
    I.seeElement(`${list} .ti-brand-chrome`);
    I.seeElement(`${list} .ti-brand-safari`);
    I.seeElement(`${list} .ti-brand-firefox`);
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
    I.clickCss('.md-dashboard__sessions .md-dashboard-widget__session-manage');
    I.waitForVisible('.md-dashboard-modal .md-dashboard-widget__session-current', 10);
    I.executeScript(() => document.querySelector('.md-dashboard-modal .md-dashboard-widget__session-current').focus());
    I.waitForVisible('.tooltip.show', 10);
    I.pressKey('Escape');
    I.waitForInvisible('.tooltip.show', 10);
    I.seeElement('.md-dashboard-modal');
    I.pressKey('Escape');
    I.waitForFunction(() => !document.querySelector('.md-dashboard-modal'), 10);
    await I.stopMockingRoute(sessionsRoute);
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
