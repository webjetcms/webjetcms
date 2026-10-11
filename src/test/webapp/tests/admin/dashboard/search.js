Feature('admin.dashboard.search');

const modal = '#admin-quick-search';
const input = '#admin-quick-search-input';
const scopes = '.md-quick-search__scopes';
const suggestions = '.md-quick-search__group[data-source="suggestions"]';
const lookup = '**/admin/skins/webjet6/_doc_autocomplete.jsp?*';
const fulltext = '**/admin/rest/search/all?*';

/** Opens the global control through its public header button. */
function openSearch(I) {
    I.waitForVisible('button.js-search-toggler', 10);
    I.clickCss('button.js-search-toggler');
    I.waitForVisible(modal, 10);
    I.waitForFunction(() => document.activeElement.id === 'admin-quick-search-input', 10);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    openSearch(I);
});

/** Checks global availability, native tab order, scope shortcuts and focus restoration. */
Scenario('Header search opens from the dashboard and app pages and restores keyboard focus', async ({ I }) => {
    I.dontSeeElement('[data-widget-type="search"]');
    I.seeElement(`${scopes} [data-scope="all"][aria-pressed="true"]`);
    I.pressKey(['Alt', 'ArrowRight']);
    I.seeElement(`${scopes} [data-scope="docs"][aria-pressed="true"]`);
    I.waitForFunction(() => document.activeElement.id === 'admin-quick-search-input', 5);
    I.pressKey(['Alt', 'ArrowLeft']);
    I.seeElement(`${scopes} [data-scope="all"][aria-pressed="true"]`);
    // macOS WebKit's default keyboard preference reaches buttons with Option+Tab.
    const macWebKit = process.env.CODECEPT_BROWSER === 'webkit' && await I.executeScript(() => /Mac/i.test(navigator.platform));
    I.pressKey(macWebKit ? ['Alt', 'Tab'] : 'Tab');
    I.waitForFunction(() => document.activeElement.id !== 'admin-quick-search-input'
        && document.querySelector('#admin-quick-search').contains(document.activeElement), 5);
    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    I.waitForFunction(() => document.activeElement.classList.contains('js-search-toggler'), 5);
    I.pressKey('Enter');
    I.waitForVisible(modal, 10);
    I.pressKey('Escape');

    I.amOnPage('/apps/form/admin/');
    I.waitForVisible('button.js-search-toggler', 10);
    const modifier = await I.executeScript(() => {
        document.querySelector('button.js-search-toggler').focus();
        return /Mac/i.test(navigator.platform) ? 'Meta' : 'Control';
    });
    I.pressKey([modifier, 'k']);
    I.waitForVisible(modal, 10);
    I.waitForFunction(() => document.activeElement.id === 'admin-quick-search-input', 10);
    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    I.waitForFunction(() => document.activeElement.classList.contains('js-search-toggler'), 5);
});

/** Documentation stays an explicit external action with the encoded query and no invented count. */
Scenario('Documentation offers an external link and history stores only confirmed searches', async ({ I }) => {
    const query = 'formulár & prístupnosť autotest ' + I.getRandomText();
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.fillField(input, `  ${query}  `);
    const link = `${modal} a[href^="https://docs.webjetcms.sk/"]`;
    I.waitForVisible(link, 10);
    const target = await I.executeScript(selector => {
        const anchor = document.querySelector(selector);
        return { href: anchor.href, target: anchor.target, rel: anchor.rel };
    }, link);
    I.assertEqual(new URL(target.href).searchParams.get('q'), query);
    I.assertEqual(target.target, '_blank');
    I.assertContain(target.rel, 'noopener');
    I.seeInField(input, `  ${query}  `);
    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    openSearch(I);
    const recent = locate('.md-quick-search__history').withText(query);
    I.dontSeeElement(recent);

    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.fillField(input, query);
    I.waitForVisible(link, 10);
    // Keep the real click and history handler, but prevent opening an external website.
    I.executeScript(selector => document.querySelector('#admin-quick-search').addEventListener('click', event => {
        if (event.target.closest(selector)) event.preventDefault();
    }, { once: true }), link);
    I.clickCss(link);
    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    openSearch(I);
    I.waitForVisible(recent, 10);
    I.click(recent.find('.md-quick-search__remove-history'));
    I.dontSeeElement(recent);
});

/** Preserves the frequent title/URL lookup and its keyboard route to the page editor. */
Scenario('Cached suggestions match accented titles and keyboard selection opens the editor', async ({ I, DTE }) => {
    I.clickCss(`${scopes} [data-scope="docs"]`);
    I.fillField(input, 'obchodný úder');
    I.waitForVisible(`${suggestions} .md-quick-search__result`, 10);
    I.see('McGregorov', suggestions);
    I.see('/Jet portal 4/Zo sveta financií', suggestions);
    I.saveScreenshot('quick-search-desktop.png');
    I.fillField(input, 'obchodny uder');
    I.waitForVisible(`${suggestions} .md-quick-search__result`, 10);
    I.clickCss(input);
    I.pressKey('ArrowDown');
    I.seeInField(input, 'obchodny uder');
    I.pressKey('Enter');
    I.seeInCurrentUrl('/admin/v9/webpages/web-pages-list/?docid=13');
    DTE.waitForEditor();
    DTE.cancel();
});

/** Checks cache-only lookup responses still enforce their JSON response contract. */
Scenario('Cached autocomplete is JSON and retains title and URL matching', async ({ I }) => {
    const responses = await I.executeScript(async () => {
        const output = [];
        for (const query of ['obchodny uder', 'https://demo.webjetcms.sk/zo-sveta-financii/mcgregorov-obchodny-uder.html']) {
            const response = await fetch('/admin/skins/webjet6/_doc_autocomplete.jsp?editable=true&cacheOnly=true&url=' + encodeURIComponent(query));
            output.push({ status: response.status, contentType: response.headers.get('content-type'),
                nosniff: response.headers.get('x-content-type-options'), items: await response.json() });
        }
        return output;
    });
    for (const response of responses) {
        I.assertEqual(response.status, 200);
        I.assertStartsWith(response.contentType, 'application/json');
        I.assertEqual(response.nosniff, 'nosniff');
        I.assertTrue(Array.isArray(response.items) && response.items.length > 0, 'Both title and URL lookup must return real pages.');
    }
});

/** Checks the modal and a long URL result remain usable on a narrow viewport. */
Scenario('URL suggestions open the page editor on mobile without horizontal overflow', async ({ I, DTE }) => {
    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    I.resizeWindow(390, 844);
    const triggerVisible = await I.executeScript(() => {
        const bounds = document.querySelector('button.js-search-toggler').getBoundingClientRect();
        return bounds.width > 0 && bounds.height > 0 && bounds.left >= 0 && bounds.top >= 0
            && bounds.right <= innerWidth && bounds.bottom <= innerHeight;
    });
    I.assertTrue(triggerVisible, 'The mobile header search button must be inside the viewport before opening the dialog.');
    openSearch(I);
    I.clickCss(`${scopes} [data-scope="docs"]`);
    I.fillField(input, 'https://demo.webjetcms.sk/zo-sveta-financii/mcgregorov-obchodny-uder.html');
    const result = `${suggestions} .md-quick-search__result[href*="docid=33"]`;
    I.waitForVisible(result, 10);
    const bounds = await I.executeScript(() => {
        const dialog = document.querySelector('#admin-quick-search .modal-dialog');
        const rectangle = dialog.getBoundingClientRect();
        return { left: rectangle.left, right: rectangle.right, viewport: innerWidth,
            width: dialog.clientWidth, content: dialog.scrollWidth };
    });
    I.assertTrue(bounds.left >= 0 && bounds.right <= bounds.viewport, 'The dialog must remain within the viewport.');
    I.assertTrue(bounds.content <= bounds.width + 1, 'Search results must wrap without horizontal scrolling.');
    I.saveScreenshot('quick-search-mobile.png');
    I.clickCss(result);
    I.seeInCurrentUrl('/admin/v9/webpages/web-pages-list/?docid=33');
    DTE.waitForEditor();
    DTE.cancel();
    I.wjSetDefaultWindowSize();
});

/** A summary is capped at three rows, while choosing its scope reveals further matches. */
Scenario('All scopes show a short preview and scoped pagination preserves keyboard focus', async ({ I }) => {
    I.mockRoute(fulltext, route => {
        const request = new URL(route.request().url());
        const files = request.searchParams.get('searchType') === 'files';
        const size = Number(request.searchParams.get('size') || 20);
        const page = Number(request.searchParams.get('page') || 0);
        const rows = files ? Array.from({ length: 25 }, (_, index) => ({ id: index + 1, docId: index + 1,
            title: `autotest-file-${index}.txt`, externalLink: `/files/autotest-file-${index}.txt`, dateCreated: 1700000000000 }))
            : [{ docId: 13, title: 'autotest-fulltext', fullPath: '/autotest' }];
        const content = rows.slice(page * size, (page + 1) * size);
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content,
            totalElements: rows.length, number: page, size, totalPages: Math.ceil(rows.length / size), last: (page + 1) * size >= rows.length }) });
    });
    I.fillField(input, 'autotest-file');
    const group = '.md-quick-search__group[data-source="files"]';
    I.waitForVisible(`${group} .md-quick-search__result`, 10);
    I.assertEqual(await I.grabNumberOfVisibleElements(`${group} .md-quick-search__result`), 3);
    I.clickCss(`${group} .md-quick-search__group-heading button`);
    I.waitForFunction(() => document.querySelectorAll('.md-quick-search__group[data-source="files"] .md-quick-search__result').length === 20, 10);
    I.seeElement(`${scopes} [data-scope="files"][aria-pressed="true"]`);
    I.clickCss(`${group} .md-quick-search__more`);
    I.waitForFunction(() => document.querySelectorAll('.md-quick-search__group[data-source="files"] .md-quick-search__result').length === 25, 10);
    I.waitForFunction(() => document.activeElement.getAttribute('href') === '/files/autotest-file-20.txt', 10);
    I.dontSeeElement(`${group} .md-quick-search__more`);

    // A late source response must preserve which group owns a focused duplicate page.
    I.mockRoute(lookup, route => route.fulfill({ status: 200, contentType: 'application/json',
        body: JSON.stringify([{ doc_id: 13, title: 'autotest-cache-copy', label: '/autotest.html' }]) }));
    I.executeScript(() => {
        const originalFetch = window.fetch;
        window.fetch = (url, options) => {
            if (!String(url).includes('/admin/rest/users/all?')) return originalFetch(url, options);
            return new Promise(resolve => {
                window.autotestResolveUsers = () => resolve(new Response(JSON.stringify({ content: [], totalElements: 0, last: true }),
                    { status: 200, headers: { 'Content-Type': 'application/json' } }));
            });
        };
    });
    I.clickCss(`${scopes} [data-scope="docs"]`);
    I.waitForVisible(`${suggestions} .md-quick-search__result`, 10);
    I.waitForVisible('.md-quick-search__group[data-source="docs"] .md-quick-search__result', 10);
    I.waitForFunction(() => typeof window.autotestResolveUsers === 'function', 10);
    I.executeScript(() => {
        document.querySelector('.md-quick-search__group[data-source="docs"] .md-quick-search__result').focus();
        window.autotestResolveUsers();
    });
    I.waitForFunction(() => document.querySelector('#admin-quick-search-results').getAttribute('aria-busy') === 'false', 10);
    I.assertEqual(await I.executeScript(() => document.activeElement.closest('[data-source]')?.dataset.source), 'docs',
        'A later count response must not move focus from fulltext to its duplicate cache suggestion.');
    I.stopMockingRoute(lookup);
    I.stopMockingRoute(fulltext);
});

/** Distinguishes an unavailable source from a successful empty response and verifies retry. */
Scenario('Failed file search can be retried without losing the query', async ({ I }) => {
    I.mockRoute(fulltext, route => route.fulfill({ status: 503, body: 'Unavailable' }));
    I.clickCss(`${scopes} [data-scope="files"]`);
    I.fillField(input, 'for-bankers.txt');
    I.waitForVisible(`${modal} .md-quick-search__retry`, 10);
    I.see('Vyhľadávanie je dočasne nedostupné', modal);
    I.stopMockingRoute(fulltext);
    I.clickCss(`${modal} .md-quick-search__retry`);
    I.waitForVisible('.md-quick-search__group[data-source="files"] .md-quick-search__result', 15);
    I.see('for-bankers.txt', modal);
    I.seeInField(input, 'for-bankers.txt');
    I.dontSeeElement(`${modal} .md-quick-search__retry`);

    I.mockRoute(fulltext, route => route.fulfill({ status: 200, contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0, last: true }) }));
    I.fillField(input, 'missing-files-autotest');
    I.waitForVisible(`${modal} .md-quick-search__empty p`, 10);
    I.see('missing-files-autotest', `${modal} .md-quick-search__empty`);
    I.see('Súbory', `${modal} .md-quick-search__empty`);
    I.dontSeeElement(`${modal} .md-quick-search__retry`);
    I.click('Hľadať všade', `${modal} .md-quick-search__empty`);
    I.seeElement(`${scopes} [data-scope="all"][aria-pressed="true"]`);
    I.stopMockingRoute(fulltext);
});

/** Prevents executable result text and verifies in-flight suggestions are cancelled when leaving their scope. */
Scenario('Suggestions render as text and leaving the page scope aborts pending lookup', async ({ I }) => {
    I.mockRoute(lookup, route => route.fulfill({ status: 200, contentType: 'application/json',
        body: JSON.stringify([{ doc_id: 13, value: 13, title: '<img src=x onerror=alert(1)>', label: '/autotest.html' }]) }));
    I.clickCss(`${scopes} [data-scope="docs"]`);
    I.fillField(input, 'safe-title-autotest');
    I.waitForText('<img src=x onerror=alert(1)>', 10, suggestions);
    I.dontSeeElement(`${suggestions} img[src="x"]`);
    I.stopMockingRoute(lookup);
    I.executeScript(() => {
        const originalFetch = window.fetch;
        window.autotestLookupAborted = false;
        window.autotestLookupStarted = false;
        window.fetch = (url, options) => {
            if (!String(url).includes('/_doc_autocomplete.jsp')) return originalFetch(url, options);
            window.autotestLookupStarted = true;
            return new Promise((resolve, reject) => options.signal.addEventListener('abort', () => {
                window.autotestLookupAborted = true;
                reject(new DOMException('Aborted', 'AbortError'));
            }, { once: true }));
        };
    });
    I.fillField(input, 'pending-autotest');
    I.waitForFunction(() => window.autotestLookupStarted, 10);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.waitForFunction(() => window.autotestLookupAborted, 10);
    I.dontSeeElement(suggestions);
    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    openSearch(I);
    I.seeElement(`${scopes} [data-scope="all"][aria-pressed="true"]`);
});

/** Search remains available for menu/docs while page scopes and their endpoint enforce permission. */
Scenario('Content permissions hide unavailable sources without hiding global search', async ({ I }) => {
    I.amOnPage('/admin/v9/?removePerm=menuWebpages,users.edit_admins,users.edit_public_users');
    openSearch(I);
    I.dontSeeElement(`${scopes} [data-scope="docs"]`);
    I.dontSeeElement(`${scopes} [data-scope="files"]`);
    I.dontSeeElement(`${scopes} [data-scope="users"]`);
    I.seeElement(`${scopes} [data-scope="admin"]`);
    I.seeElement(`${scopes} [data-scope="documentation"]`);
    const deniedUrl = await I.executeScript(async () => {
        const response = await fetch('/admin/skins/webjet6/_doc_autocomplete.jsp?editable=true&cacheOnly=true&url=obchodny');
        return response.url;
    });
    I.assertEqual(new URL(deniedUrl).pathname, '/admin/403.jsp', 'The cached lookup must enforce webpage permission.');
});

Scenario('Logout after removing content permissions', ({ I }) => {
    I.logout();
});
