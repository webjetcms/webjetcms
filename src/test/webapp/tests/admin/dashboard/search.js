Feature('admin.dashboard.search');

const modal = '#admin-quick-search';
const input = '#admin-quick-search-input';
const scopes = '.md-quick-search__scopes';
const suggestions = '.md-quick-search__group[data-source="suggestions"]';
const lookup = '**/admin/skins/webjet6/_doc_autocomplete.jsp?*';
const fulltext = '**/admin/rest/search/all?*';
const documentationEndpoint = 'https://documentation.autotest.invalid/custom/semantic-search?collection=autotest';
const documentationRoute = 'https://documentation.autotest.invalid/custom/semantic-search?*';
const documentation = '.md-quick-search__group[data-source="documentation"]';
const documentationResult = `${documentation} .md-quick-search__documentation-result`;

/** Opens the global control through its public header button. */
function openSearch(I) {
    I.waitForVisible('button.js-search-toggler', 10);
    I.clickCss('button.js-search-toggler');
    I.waitForVisible(modal, 10);
    I.waitForFunction(() => document.activeElement.id === 'admin-quick-search-input', 10);
}

/** Measures the visible panel and its input independently of the modal wrapper. */
function searchPosition() {
    return {
        panel: document.querySelector('#admin-quick-search .modal-content').getBoundingClientRect().top,
        input: document.querySelector('#admin-quick-search-input').getBoundingClientRect().top
    };
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    // Each documentation scenario opts into a page-local endpoint with a mocked response.
    I.executeScript(() => window.adminSearchContext.documentationSearchUrl = '');
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
    I.executeScript(() => window.adminSearchContext.documentationSearchUrl = '');
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

/** Semantic results open the configured documentation site and remember only confirmed queries. */
Scenario('Documentation shows semantic results and history stores only confirmed searches', async ({ I }) => {
    const query = 'formulár & prístupnosť autotest ' + I.getRandomText();
    I.mockRoute(documentationRoute, route => route.fulfill({ status: 200, contentType: 'application/json',
        headers: { 'Access-Control-Allow-Origin': '*' }, body: JSON.stringify({ results: [{ title: 'autotest documentation result',
            url: '/latest/#/sk/redactor/apps/banner/', sourcePath: '/latest/sk/redactor/apps/banner/README.md',
            snippet: 'Use **banner campaigns** to publish your content.', score: 0.9 }] }) }));
    I.executeScript(endpoint => window.adminSearchContext.documentationSearchUrl = endpoint, documentationEndpoint);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.fillField(input, `  ${query}  `);
    const link = `${documentationResult} .md-quick-search__result`;
    I.waitForVisible(link, 10);
    const target = await I.executeScript(selector => {
        const anchor = document.querySelector(selector);
        return { href: anchor.href, target: anchor.target, rel: anchor.rel };
    }, link);
    I.assertEqual(target.href, 'https://documentation.autotest.invalid/latest/sk/redactor/apps/banner/');
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
    I.stopMockingRoute(documentationRoute);
});

/** Short trimmed queries require Enter, including a single character, without opening a result. */
Scenario('Documentation waits for Enter for short queries including a single character', async ({ I }) => {
    const requests = [];
    I.mockRoute(documentationRoute, route => {
        const query = new URL(route.request().url()).searchParams.get('query');
        requests.push(query);
        return route.fulfill({ status: 200, contentType: 'application/json', headers: { 'Access-Control-Allow-Origin': '*' },
            body: JSON.stringify({ results: [{ title: `autotest documentation ${query}`,
                url: '/latest/#/sk/redactor/apps/banner/', snippet: 'Documentation for the confirmed query.' }] }) });
    });
    I.executeScript(endpoint => window.adminSearchContext.documentationSearchUrl = endpoint, documentationEndpoint);
    I.fillField(input, '  banner  ');
    I.waitForVisible(`${scopes} [data-scope="users"] .md-quick-search__count`, 15);
    await I.waitForFunction(() => document.querySelector('#admin-quick-search-results').getAttribute('aria-busy') === 'false', 15);
    I.seeElement(`${documentation} .md-quick-search__note`);
    I.dontSeeElement(documentationResult);
    I.assertDeepEqual([...requests], [], 'A query shorter than eight trimmed characters must not automatically request documentation.');

    I.pressKey('Enter');
    await I.waitForText('autotest documentation banner', 10, documentationResult);
    I.assertDeepEqual([...requests], ['banner'], 'Enter must request documentation once using the trimmed query.');
    I.seeCurrentUrlEquals('/admin/v9/');
    I.seeElement(modal);

    I.fillField(input, ' b ');
    I.waitForVisible(`${documentation} .md-quick-search__note`, 10);
    I.pressKey('Enter');
    await I.waitForText('autotest documentation b', 10, documentationResult);
    I.assertDeepEqual([...requests], ['banner', 'b'], 'Enter must also search documentation for a single nonempty character.');
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'admin-quick-search-input',
        'Confirming a documentation search must preserve input focus.');
    I.seeCurrentUrlEquals('/admin/v9/');
    I.stopMockingRoute(documentationRoute);
});

/** Documentation keeps one in-flight request, its results and its expanded AI answer across scopes. */
Scenario('Documentation reuses pending and completed results when changing scopes', async ({ I }) => {
    const answer = Array.from({ length: 8 }, (_, index) => `Paragraph ${index + 1}: **Banner campaigns** group advertising content and let editors set publication dates.`).join('\n\n');
    I.mockRoute(documentationRoute, route => route.fulfill({ status: 200, contentType: 'application/json',
        headers: { 'Access-Control-Allow-Origin': '*' }, body: JSON.stringify({ answer,
            results: Array.from({ length: 5 }, (_, index) => ({ title: `autotest cached documentation ${index}`,
                url: `/latest/#/sk/redactor/apps/banner/?result=${index}`, snippet: 'A reusable documentation result.' })) }) }));
    I.executeScript(endpoint => {
        window.adminSearchContext.documentationSearchUrl = endpoint;
        const originalFetch = window.fetch;
        window.autotestDocumentationRequests = 0;
        window.autotestDocumentationAborted = false;
        window.fetch = (url, options) => {
            if (!String(url).startsWith(endpoint)) return originalFetch(url, options);
            window.autotestDocumentationRequests++;
            return new Promise((resolve, reject) => {
                window.autotestReleaseDocumentation = () => resolve(originalFetch(url, options));
                options.signal.addEventListener('abort', () => {
                    window.autotestDocumentationAborted = true;
                    reject(new DOMException('Aborted', 'AbortError'));
                }, { once: true });
            });
        };
    }, documentationEndpoint);
    I.fillField(input, 'banner campaigns autotest');
    I.pressKey('Enter');
    I.waitForFunction(() => typeof window.autotestReleaseDocumentation === 'function', 15);
    I.seeElement(`${scopes} [data-scope="documentation"][aria-busy="true"] .md-quick-search__spinner`);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.clickCss(`${scopes} [data-scope="all"]`);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.assertEqual(await I.executeScript(() => window.autotestDocumentationRequests), 1,
        'Changing scopes must reuse the documentation request already in flight.');
    I.assertFalse(await I.executeScript(() => window.autotestDocumentationAborted),
        'Changing scopes must not abort the documentation request.');
    I.executeScript(() => window.autotestReleaseDocumentation());
    I.waitForVisible(documentationResult, 10);
    I.seeNumberOfElements(documentationResult, 5);
    I.seeElement(`${scopes} [data-scope="documentation"][aria-busy="false"] .md-quick-search__count`);
    I.dontSeeElement(`${scopes} [data-scope="documentation"] .md-quick-search__spinner`);
    I.clickCss(`${documentation} .md-quick-search__answer button[aria-expanded="false"]`);
    I.see('Paragraph 8:', `${documentation} .md-quick-search__answer`);
    I.clickCss(`${scopes} [data-scope="all"]`);
    I.seeNumberOfElements(documentationResult, 3);
    I.seeElement(`${documentation} .md-quick-search__answer button[aria-expanded="true"]`);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.seeNumberOfElements(documentationResult, 5);
    I.see('Paragraph 8:', `${documentation} .md-quick-search__answer`);
    I.assertEqual(await I.executeScript(() => window.autotestDocumentationRequests), 1,
        'Completed documentation results and the AI answer must be reused without another request.');
    I.stopMockingRoute(documentationRoute);
});

/** The entire endpoint is configurable and each supported locale narrows both API filters. */
Scenario('Documentation sends localized filters to the configured endpoint without requiring an AI answer', async ({ I }) => {
    const requests = [];
    I.mockRoute(documentationRoute, route => {
        const request = new URL(route.request().url());
        requests.push({ query: request.searchParams.get('query'), language: request.searchParams.get('language'),
            directory: request.searchParams.get('directory'), collection: request.searchParams.get('collection') });
        return route.fulfill({ status: 200, contentType: 'application/json', headers: { 'Access-Control-Allow-Origin': '*' },
            body: JSON.stringify({ results: [{ title: `autotest ${request.searchParams.get('query')}`,
                url: `/latest/#/${request.searchParams.get('language')}/redactor/apps/banner/`, snippet: '**Localized** documentation.' }] }) });
    });
    I.executeScript(endpoint => window.adminSearchContext.documentationSearchUrl = endpoint, documentationEndpoint);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    for (const [adminLanguage, language] of [['sk', 'sk'], ['en', 'en'], ['cs', 'cs'], ['cz', 'cs']]) {
        const query = `banner & accessibility autotest ${adminLanguage}`;
        I.executeScript(value => window.userLng = value, adminLanguage);
        I.fillField(input, query);
        await I.waitForText(`autotest ${query}`, 10, documentationResult);
        I.assertDeepEqual(requests[requests.length - 1], { query, language, directory: `/${language}/`, collection: 'autotest' });
        I.dontSeeElement(`${documentation} .md-quick-search__answer`);
        I.seeElement(`${documentationResult} strong`);
    }
    const requestCount = requests.length;
    I.clickCss(`${scopes} [data-scope="admin"]`);
    I.fillField(input, 'administration autotest');
    await I.waitForFunction(() => document.querySelector('#admin-quick-search-results').getAttribute('aria-busy') === 'false', 10);
    I.assertEqual(requests.length, requestCount, 'Unrelated scopes must not send queries to the documentation service.');
    I.stopMockingRoute(documentationRoute);
});

/** Rich external Markdown remains safe, readable and contained on desktop and mobile. */
Scenario('Documentation formats Markdown and expandable AI answers without horizontal overflow', async ({ I }) => {
    I.executeScript(() => localStorage.removeItem('translate'));
    I.amOnPage('/admin/v9/?userlngr=true');
    I.executeScript(() => window.adminSearchContext.documentationSearchUrl = '');
    openSearch(I);
    const answer = Array.from({ length: 12 }, (_, index) => `Paragraph ${index + 1}: **Banner campaigns** group related advertising content and let editors set publication dates.`).join('\n\n');
    const snippet = '## Campaign setup\n\nChoose **banner campaigns** and follow these steps:\n\n- Add a campaign\n- Set its publication dates\n\n'
        + '| Setting | Value |\n| --- | --- |\n| Campaign | autotest |\n\n```json\n{"campaign": "' + 'autotest-'.repeat(20) + '"}\n```\n\n'
        + '[Documentation link](/latest/#/sk/redactor/apps/banner/)\n\n<script>window.autotestDocumentationUnsafe = true</script>'
        + '<img src=x onerror="window.autotestDocumentationUnsafe = true">\n\n[Unsafe link](javascript:alert(1))\n\n'
        + '[Injected attributes](/latest/sk/redactor/apps/banner/" onclick="window.autotestDocumentationUnsafe = true" style="color:red)\n\n'
        + '[Encoded script](java&#x73;cript:alert(1))\n\n![Campaign preview](/autotest-preview.png)';
    I.mockRoute(documentationRoute, route => route.fulfill({ status: 200, contentType: 'application/json',
        headers: { 'Access-Control-Allow-Origin': '*' }, body: JSON.stringify({ answer, results: [{ title: 'autotest banner campaign setup',
            url: '/latest/#/sk/redactor/apps/banner/', sourcePath: '/latest/sk/redactor/apps/banner/README.md', snippet, score: 0.9 }] }) }));
    I.executeScript(endpoint => window.adminSearchContext.documentationSearchUrl = endpoint, documentationEndpoint);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.fillField(input, 'banner campaigns autotest');
    I.waitForVisible(`${documentationResult} .md-quick-search__markdown strong`, 10);
    for (const element of ['h2', 'ul', 'pre code']) I.seeElement(`${documentationResult} .md-quick-search__markdown ${element}`);
    I.see('| Setting | Value |', `${documentationResult} .md-quick-search__markdown`);
    I.see('| Campaign | autotest |', `${documentationResult} .md-quick-search__markdown`);
    I.see('Campaign preview', `${documentationResult} .md-quick-search__markdown`);
    I.seeElement(`${documentation} .md-quick-search__answer strong`);
    I.see('Prehľad od AI', `${documentation} .md-quick-search__answer h4`);
    I.dontSee('**', `${documentation} .md-quick-search__answer`);
    I.dontSeeElement(`${documentation} script`);
    I.dontSeeElement(`${documentation} img`);
    I.dontSeeElement(`${documentation} [onclick], ${documentation} [onerror], ${documentation} [style]`);
    I.dontSeeElement(`${documentation} a[href^="javascript:"]`);
    I.assertFalse(await I.executeScript(() => window.autotestDocumentationUnsafe === true), 'External Markdown must not execute HTML.');
    const expand = `${documentation} .md-quick-search__answer button[aria-expanded="false"]`;
    I.waitForVisible(expand, 10);
    I.clickCss(expand);
    I.seeElement(`${documentation} .md-quick-search__answer button[aria-expanded="true"]`);
    I.see('Paragraph 12:', `${documentation} .md-quick-search__answer`);
    I.clickCss(`${documentation} .md-quick-search__answer button[aria-expanded="true"]`);

    for (const [width, height, screenshot] of [[1280, 900, 'quick-search-documentation-desktop.png'], [390, 844, 'quick-search-documentation-mobile.png']]) {
        I.resizeWindow(width, height);
        const bounds = await I.executeScript(() => {
            const dialog = document.querySelector('#admin-quick-search .modal-dialog');
            const rectangle = dialog.getBoundingClientRect();
            return { left: rectangle.left, right: rectangle.right, viewport: innerWidth,
                containers: [...document.querySelectorAll('#admin-quick-search .modal-content, #admin-quick-search-results, .md-quick-search__documentation-result')]
                    .map(element => ({ width: element.clientWidth, content: element.scrollWidth })) };
        });
        I.assertTrue(bounds.left >= 0 && bounds.right <= bounds.viewport, 'The documentation dialog must fit the viewport.');
        I.assertTrue(bounds.containers.every(container => container.content <= container.width + 1), 'Markdown must not overflow the documentation cards or dialog.');
        I.saveScreenshot(screenshot);
    }
    I.wjSetDefaultWindowSize();
    I.stopMockingRoute(documentationRoute);
});

/** Clearing the optional endpoint retains the original documentation search link. */
Scenario('Documentation offers the external search fallback when its endpoint is empty', async ({ I }) => {
    const query = 'banner & accessibility autotest';
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.fillField(input, query);
    const link = `${documentation} a[href^="https://docs.webjetcms.sk/"]`;
    I.waitForVisible(link, 10);
    const target = await I.executeScript(selector => {
        const anchor = document.querySelector(selector);
        return { href: anchor.href, target: anchor.target, rel: anchor.rel };
    }, link);
    I.assertEqual(new URL(target.href).searchParams.get('q'), query);
    I.assertEqual(target.target, '_blank');
    I.assertContain(target.rel, 'noopener');
    I.dontSeeElement(documentationResult);
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
    I.pressKey('Enter');
    I.seeCurrentUrlEquals('/admin/v9/');
    I.seeElement(modal);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'admin-quick-search-input',
        'Enter in the search input must keep focus without opening a result.');
    I.pressKey('ArrowDown');
    I.seeInField(input, 'obchodny uder');
    I.pressKey('Enter');
    I.seeInCurrentUrl('/admin/v9/webpages/web-pages-list/?docid=13');
    DTE.waitForEditor();
    DTE.cancel();
});

/** Opens translation results in their native table and reapplies the key filter from that page. */
Scenario('Translation key results open and filter the translation table from any admin page', ({ I, DT }) => {
    const firstKey = 'admin.dashboard.notice.browserMigration.description.js';
    const secondKey = 'admin.dashboard.shortcutIcon.chart.js';
    const translationPage = '/admin/v9/settings/translation-keys/';
    const group = '.md-quick-search__group[data-source="translationKeys"]';
    const keyFilter = '#datatableInit_wrapper .dt-scroll-headInner input.dt-filter-key';
    const firstUrl = `${translationPage}?key=${encodeURIComponent(firstKey)}`;
    const secondUrl = `${translationPage}?key=${encodeURIComponent(secondKey)}`;

    I.fillField(input, 'štatistika');
    I.clickCss(`${scopes} [data-scope="translationKeys"]`);
    I.waitForVisible(`${group} a[href="${firstUrl}"]`, 15);
    I.clickCss(`${group} a[href="${firstUrl}"]`);
    I.waitUrlEquals(firstUrl, 10);
    I.waitForValue(keyFilter, firstKey, 10);
    DT.waitForLoader();
    I.waitForText(firstKey, 10, '#datatableInit tbody');
    I.seeInField(keyFilter, firstKey);
    I.seeNumberOfElements('#datatableInit tbody tr', 1);
    I.dontSeeElement(modal);

    I.executeScript(() => window.adminSearchContext.documentationSearchUrl = '');
    openSearch(I);
    I.fillField(input, secondKey);
    I.seeElement(`${scopes} [data-scope="translationKeys"][aria-pressed="true"]`);
    I.waitForVisible(`${group} a[href="${secondUrl}"]`, 15);
    I.clickCss(`${group} a[href="${secondUrl}"]`);
    I.waitUrlEquals(secondUrl, 10);
    I.waitForValue(keyFilter, secondKey, 10);
    DT.waitForLoader();
    I.waitForText(secondKey, 10, '#datatableInit tbody');
    I.seeInField(keyFilter, secondKey);
    I.seeNumberOfElements('#datatableInit tbody tr', 1);
    I.dontSee(firstKey, '#datatableInit tbody');
    I.dontSeeElement(modal);
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
    I.see('Esc', `${modal} .md-quick-search__close`);
    I.clickCss(`${modal} .md-quick-search__close`);
    I.waitForInvisible(modal, 10);
    I.waitForFunction(() => document.activeElement.classList.contains('js-search-toggler'), 5);
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

/** Keeps the input stationary as results load and scopes expand, while preserving keyboard focus. */
Scenario('All scopes show a short preview and scoped pagination preserves keyboard focus', async ({ I }) => {
    const emptyPosition = await I.executeScript(searchPosition);
    I.saveScreenshot('quick-search-palette.png');
    I.mockRoute(fulltext, route => {
        const request = new URL(route.request().url());
        const files = request.searchParams.get('searchType') === 'files';
        const size = Number(request.searchParams.get('size') || 10);
        const page = Number(request.searchParams.get('page') || 0);
        const rows = files ? Array.from({ length: 25 }, (_, index) => ({ id: index + 1, docId: index + 1,
            title: `autotest-file-${index}.txt`, externalLink: `/files/autotest-file-${index}.txt`, dateCreated: 1700000000000 }))
            : [{ docId: 13, title: 'autotest-fulltext', fullPath: '/autotest' }];
        const content = rows.slice(page * size, (page + 1) * size);
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content,
            totalElements: rows.length, number: page, size, totalPages: Math.ceil(rows.length / size), last: (page + 1) * size >= rows.length }) });
    });
    I.executeScript(() => {
        const originalFetch = window.fetch;
        window.autotestFileRequests = [];
        window.fetch = (url, options) => {
            if (!String(url).includes('/admin/rest/search/all?') || !String(url).includes('searchType=files')) return originalFetch(url, options);
            const request = new URL(url, location.href);
            window.autotestFileRequests.push({ size: Number(request.searchParams.get('size')), page: Number(request.searchParams.get('page')) });
            if (window.autotestFileRequests.length > 1) return originalFetch(url, options);
            return new Promise(resolve => {
                window.autotestReleaseFiles = () => {
                    resolve(originalFetch(url, options));
                };
            });
        };
    });
    I.fillField(input, 'autotest-file');
    I.waitForFunction(() => typeof window.autotestReleaseFiles === 'function', 10);
    for (const source of ['files', 'users']) {
        I.seeElement(`${scopes} [data-scope="${source}"][aria-busy="true"] .md-quick-search__spinner`);
    }
    I.saveScreenshot('quick-search-loading-desktop.png');
    I.resizeWindow(390, 844);
    I.saveScreenshot('quick-search-loading-mobile.png');
    I.wjSetDefaultWindowSize();
    const loadingPosition = await I.executeScript(searchPosition);
    I.executeScript(() => window.autotestReleaseFiles());
    const group = '.md-quick-search__group[data-source="files"]';
    I.waitForVisible(`${group} .md-quick-search__result`, 10);
    const resultPosition = await I.executeScript(searchPosition);
    I.assertEqual(await I.grabNumberOfVisibleElements(`${group} .md-quick-search__result`), 3);
    I.clickCss(`${group} .md-quick-search__group-heading button`);
    I.waitForFunction(() => document.querySelectorAll('.md-quick-search__group[data-source="files"] .md-quick-search__result').length === 10, 10);
    I.assertDeepEqual(await I.executeScript(() => window.autotestFileRequests), [{ size: 10, page: 0 }],
        'Opening a scope must reveal the ten cached rows without another request.');
    const expandedPosition = await I.executeScript(searchPosition);
    for (const [phase, position] of [['loading', loadingPosition], ['results', resultPosition], ['expanded scope', expandedPosition]]) {
        I.assertTrue(Math.abs(position.panel - emptyPosition.panel) <= 1, `The panel must keep its opening position during ${phase}.`);
        I.assertTrue(Math.abs(position.input - emptyPosition.input) <= 1, `The search input must remain stationary during ${phase}.`);
    }
    I.seeElement(`${scopes} [data-scope="files"][aria-pressed="true"]`);
    I.seeElement(`${scopes} [data-scope="files"][aria-busy="false"] .md-quick-search__count`);
    I.dontSeeElement(`${scopes} [data-scope="files"] .md-quick-search__spinner`);
    I.waitForVisible(`${group} .md-quick-search__more[aria-disabled="false"]`, 10);
    I.clickCss(`${group} .md-quick-search__more`);
    I.waitForFunction(() => document.querySelectorAll('.md-quick-search__group[data-source="files"] .md-quick-search__result').length === 20, 10);
    I.waitForFunction(() => document.activeElement.getAttribute('href') === '/files/autotest-file-10.txt', 10);
    I.waitForVisible(`${group} .md-quick-search__more[aria-disabled="false"]`, 10);
    I.clickCss(`${group} .md-quick-search__more`);
    I.waitForFunction(() => document.querySelectorAll('.md-quick-search__group[data-source="files"] .md-quick-search__result').length === 25, 10);
    I.waitForFunction(() => document.activeElement.getAttribute('href') === '/files/autotest-file-20.txt', 10);
    I.dontSeeElement(`${group} .md-quick-search__more`);
    I.clickCss(`${scopes} [data-scope="all"]`);
    I.seeNumberOfElements(`${group} .md-quick-search__result`, 3);
    I.clickCss(`${scopes} [data-scope="files"]`);
    I.seeNumberOfElements(`${group} .md-quick-search__result`, 25);
    I.assertDeepEqual(await I.executeScript(() => window.autotestFileRequests), [
        { size: 10, page: 0 }, { size: 10, page: 1 }, { size: 10, page: 2 }
    ], 'Pagination must request ten rows at a time and preserve loaded pages across scope changes.');

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
    I.fillField(input, 'autotest-duplicate');
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
    I.waitForVisible(`${modal} .md-quick-search__retry[aria-disabled="false"]`, 10);
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

/** Prevents executable result text and preserves in-flight suggestions until the dialog closes. */
Scenario('Suggestions render as text and scope changes reuse pending lookup', async ({ I }) => {
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
        window.autotestLookupRequests = 0;
        window.fetch = (url, options) => {
            if (!String(url).includes('/_doc_autocomplete.jsp')) return originalFetch(url, options);
            window.autotestLookupRequests++;
            return new Promise((resolve, reject) => options.signal.addEventListener('abort', () => {
                window.autotestLookupAborted = true;
                reject(new DOMException('Aborted', 'AbortError'));
            }, { once: true }));
        };
    });
    I.fillField(input, 'pending-autotest');
    I.waitForFunction(() => window.autotestLookupRequests === 1, 10);
    I.clickCss(`${scopes} [data-scope="documentation"]`);
    I.dontSeeElement(suggestions);
    I.assertFalse(await I.executeScript(() => window.autotestLookupAborted), 'Changing scopes must keep the pending lookup alive.');
    I.clickCss(`${scopes} [data-scope="docs"]`);
    I.assertEqual(await I.executeScript(() => window.autotestLookupRequests), 1, 'Returning to pages must reuse the pending lookup.');
    I.pressKey('Escape');
    I.waitForInvisible(modal, 10);
    I.waitForFunction(() => window.autotestLookupAborted, 10);
    openSearch(I);
    I.seeElement(`${scopes} [data-scope="all"][aria-pressed="true"]`);
});

/** Skips the denied source while the remaining real REST searches finish one after another. */
Scenario('Sequential search completes allowed sources without translation permission', async ({ I }) => {
    I.amOnPage('/admin/v9/?removePerm=edit_text');
    I.executeScript(() => window.adminSearchContext.documentationSearchUrl = '');
    openSearch(I);
    I.assertFalse(await I.executeScript(() => WJ.hasPermission('edit_text')),
        'The translation permission must be removed for this scenario.');
    I.dontSeeElement(`${scopes} [data-scope="translationKeys"]`);
    I.executeScript(() => {
        const originalFetch = window.fetch;
        window.autotestSearchRequests = [];
        window.fetch = (url, options) => {
            const request = new URL(url, location.href);
            const source = request.pathname === '/admin/rest/search/all' ? request.searchParams.get('searchType')
                : request.pathname === '/admin/rest/users/all' ? 'users'
                : request.pathname === '/admin/v9/settings/translation-keys/all' ? 'translationKeys' : null;
            if (!source) return originalFetch(url, options);
            window.autotestSearchRequests.push({ source, event: 'start' });
            return originalFetch(url, options).then(response => {
                window.autotestSearchRequests.push({ source, event: 'complete', status: response.status });
                return response;
            });
        };
    });
    I.fillField(input, 'banner');
    I.pressKey('Enter');
    I.waitForVisible('.md-quick-search__group[data-source="docs"] .md-quick-search__result', 20);
    I.waitForFunction(() => document.querySelector('#admin-quick-search-results').getAttribute('aria-busy') === 'false', 20);
    I.assertDeepEqual(await I.executeScript(() => window.autotestSearchRequests), [
        { source: 'docs', event: 'start' },
        { source: 'docs', event: 'complete', status: 200 },
        { source: 'files', event: 'start' },
        { source: 'files', event: 'complete', status: 200 },
        { source: 'users', event: 'start' },
        { source: 'users', event: 'complete', status: 200 }
    ], 'Allowed REST sources must complete in order without requesting translation keys.');
    for (const source of ['docs', 'files', 'users']) {
        I.seeElement(`${scopes} [data-scope="${source}"] .md-quick-search__count`);
    }
    I.dontSeeElement(`${modal} .md-quick-search__retry`);
    I.seeCurrentUrlEquals('/admin/v9/?removePerm=edit_text');
});

Scenario('Logout after removing translation permission', ({ I }) => {
    I.logout();
});

/** Search remains available for menu/docs while page scopes and their endpoint enforce permission. */
Scenario('Content permissions hide unavailable sources without hiding global search', async ({ I }) => {
    I.amOnPage('/admin/v9/?removePerm=menuWebpages,users.edit_admins,users.edit_public_users');
    I.executeScript(() => window.adminSearchContext.documentationSearchUrl = '');
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
