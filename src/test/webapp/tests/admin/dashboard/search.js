Feature('admin.dashboard.search');

const search = '[data-widget-type="search"]';
const input = `${search} input[type="search"]`;
const admin = `${search} label:has(input[value="admin"])`;
const docs = `${search} label:has(input[value="docs"])`;
const results = '.md-dashboard-widget__search-results';
const lookup = '**/admin/skins/webjet6/_doc_autocomplete.jsp?*';

async function assertSuggestionWidth(I) {
    const bounds = await I.executeScript(([menuSelector, inputSelector]) => {
        const menu = document.querySelector(menuSelector);
        const rect = menu.getBoundingClientRect();
        const field = document.querySelector(inputSelector).getBoundingClientRect();
        return { left: rect.left, right: rect.right, viewport: window.innerWidth, width: rect.width,
            fieldWidth: field.width, fieldLeft: field.left, clientWidth: menu.clientWidth, content: menu.scrollWidth };
    }, [results, input]);
    I.assertTrue(Math.abs(bounds.width - bounds.fieldWidth) <= 1, 'The menu width must match the text input.');
    I.assertTrue(Math.abs(bounds.left - bounds.fieldLeft) <= 1, 'The menu must align with the input.');
    I.assertTrue(bounds.left >= 0 && bounds.right <= bounds.viewport, 'Suggestions must remain within the viewport.');
    I.assertTrue(bounds.content <= bounds.clientWidth + 1, 'Suggestions must wrap without horizontal scrolling.');
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.clickCss(admin);
});

/**
 * Checks that changing search scope with an empty query does nothing, while a nonempty documentation query
 * opens a separate tab. Clicking the scope and pressing Enter must preserve the
 * entered text.
 */
Scenario('Empty scope switches do not submit and documentation clicks submit the current query', async ({ I }) => {
    I.executeScript(() => {
        window.autotestSearchPopups = [];
        window.open = (url, target, features) => window.autotestSearchPopups.push({ url, target, features });
    });
    for (const value of ['', '   ']) {
        I.fillField(input, value);
        I.clickCss(docs);
        I.seeElement(`${search} input[value="docs"]:checked`);
        I.clickCss(admin);
        I.seeElement(`${search} input[value="admin"]:checked`);
    }
    I.assertEqual(await I.executeScript(() => window.autotestSearchPopups.length), 0);
    I.seeInCurrentUrl('/admin/v9/');

    const query = 'formulár & prístupnosť autotest';
    I.fillField(input, `  ${query}  `);
    I.clickCss(docs);
    I.clickCss(docs);
    I.clickCss(input);
    I.pressKey('Enter');
    const popups = await I.executeScript(() => window.autotestSearchPopups);
    I.assertEqual(popups.length, 3, 'Switching, clicking the selected scope and Enter must each submit once.');
    for (const popup of popups) {
        I.assertEqual(new URL(popup.url).origin, 'https://docs.webjetcms.sk');
        I.assertEqual(new URL(popup.url).searchParams.get('q'), query);
        I.assertEqual(popup.target, '_blank');
        I.assertContain(popup.features, 'noopener');
    }
    I.dontSeeElement(results);
});

for (const fromDocs of [false, true]) {
    /**
     * Checks that choosing administration search submits the entered query both when switching from
     * documentation and when administration search is already selected.
     */
    Scenario(`Administration scope submits ${fromDocs ? 'when switching from documentation' : 'when already selected'}`, ({ I }) => {
        if (fromDocs) I.clickCss(docs);
        I.fillField(input, 'dashboard & search autotest');
        I.clickCss(admin);
        I.seeInCurrentUrl('/admin/v9/search/index/?text=dashboard%20%26%20search%20autotest');
    });
}

/**
 * Checks that page suggestions match titles with or without accents and show a thumbnail, folder path and
 * latest save time. Selecting a suggestion with the keyboard must open the correct page editor.
 */
Scenario('Page suggestions match titles and URLs and keyboard selection opens the editor', async ({ I, DTE }) => {
    // Spaces and diacritics distinguish a title match from the hyphenated URL.
    I.fillField(input, 'obchodný úder');
    I.waitForVisible(`${results} li`, 10);
    I.see('McGregorov', results);
    I.see('/Jet portal 4/Zo sveta financií', `${results} .md-dashboard-widget__page-section`);
    I.waitForFunction(([selector]) => {
        const image = document.querySelector(`${selector} .md-dashboard-widget__page-image img`);
        return image?.complete && image.naturalWidth > 0;
    }, [results], 10);
    const changed = await I.grabTextFrom(`${results} .md-dashboard-widget__page-date`);
    I.assertTrue(/\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}/.test(changed), 'The preview must show the latest save date.');
    await assertSuggestionWidth(I);
    I.saveScreenshot('dashboard-search-desktop.png');
    I.fillField(input, 'obchodny uder');
    I.waitForVisible(`${results} li`, 10);
    I.pressKey('ArrowDown');
    I.seeInField(input, 'obchodny uder');
    I.pressKey('Enter');
    I.seeInCurrentUrl('/admin/v9/webpages/web-pages-list/?docid=13');
    DTE.waitForEditor();
    DTE.cancel();
});

/**
 * Checks that suggestion responses are delivered as data, not as an HTML page, so page titles cannot run
 * embedded markup when the suggestion address is opened directly.
 */
Scenario('Autocomplete responses use JSON so page titles cannot execute as HTML on direct navigation', async ({ I }) => {
    const responses = await I.executeScript(async () => {
        const results = [];
        for (const query of ['editable=true&docid=obchodny', 'docid=obchodny', 'url=obchodny', 'text=obchodny']) {
            const response = await fetch(`/admin/skins/webjet6/_doc_autocomplete.jsp?${query}`);
            results.push({ query, status: response.status, contentType: response.headers.get('content-type'),
                nosniff: response.headers.get('x-content-type-options'), items: await response.json() });
        }
        return results;
    });
    for (const response of responses) {
        I.assertEqual(response.status, 200, response.query);
        I.assertStartsWith(response.contentType, 'application/json', 'Raw page titles must never be served as an HTML document.');
        I.assertEqual(response.nosniff, 'nosniff');
        I.assertTrue(Array.isArray(response.items) && response.items.length > 0, 'The test must cover actual page suggestions.');
    }
});

/**
 * Checks that searching by a page URL offers the correct page, that suggestions fit a narrow screen and that
 * clicking a result opens its editor.
 */
Scenario('Mouse selection opens the page and suggestions fit a narrow viewport', async ({ I, DTE }) => {
    I.resizeWindow(390, 844);
    I.fillField(input, 'https://demo.webjetcms.sk/zo-sveta-financii/mcgregorov-obchodny-uder.html');
    I.waitForVisible(`${results} li`, 10);
    await assertSuggestionWidth(I);
    I.saveScreenshot('dashboard-search-mobile.png');
    I.resizeWindow(1024, 844);
    await assertSuggestionWidth(I);
    I.clickCss(`${results} .md-dashboard-widget__page[title*="/zo-sveta-financii/mcgregorov-obchodny-uder.html"]`);
    I.seeInCurrentUrl('/admin/v9/webpages/web-pages-list/?docid=33');
    DTE.waitForEditor();
    DTE.cancel();
    I.wjSetDefaultWindowSize();
});

/**
 * Checks that empty or failed suggestions do not prevent ordinary search. Suggestion titles are displayed as
 * plain text, and Escape closes the list without clearing the query.
 */
Scenario('Empty and failed lookups leave ordinary search usable and results render as text', async ({ I }) => {
    I.mockRoute(lookup, route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
    I.fillField(input, 'missing-autotest');
    I.waitForFunction(([selector, term]) => {
        const autocomplete = window.$(selector).autocomplete('instance');
        return autocomplete.term === term && autocomplete.pending === 0;
    }, [input, 'missing-autotest'], 10);
    I.dontSeeElement(results);
    I.stopMockingRoute(lookup);
    I.mockRoute(lookup, route => route.fulfill({ status: 403, body: 'Forbidden' }));
    I.fillField(input, 'denied-autotest');
    I.waitForFunction(([selector, term]) => {
        const autocomplete = window.$(selector).autocomplete('instance');
        return autocomplete.term === term && autocomplete.pending === 0;
    }, [input, 'denied-autotest'], 10);
    I.dontSeeElement(results);
    I.stopMockingRoute(lookup);
    I.mockRoute(lookup, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{ doc_id: 13, value: 13, title: '<img src=x onerror=alert(1)>', label: '/autotest.html' }]) }));
    I.fillField(input, 'safe-title-autotest');
    I.waitForText('<img src=x onerror=alert(1)>', 10, results);
    I.dontSeeElement(`${results} img`);
    I.pressKey('Escape');
    I.dontSeeElement(results);
    I.seeInField(input, 'safe-title-autotest');
    I.stopMockingRoute(lookup);
    I.pressKey('Enter');
    I.seeInCurrentUrl('/admin/v9/search/index/?text=safe-title-autotest');
});

/**
 * Checks that an account without web-page permission has no page suggestions and cannot retrieve them by
 * requesting the search address directly.
 */
Scenario('Page autocomplete is unavailable without webpage permission', async ({ I }) => {
    I.amOnPage('/admin/v9/?removePerm=menuWebpages');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.dontSeeElement(`${input}.ui-autocomplete-input`);
    const deniedUrl = await I.executeScript(async () => {
        const response = await fetch('/admin/skins/webjet6/_doc_autocomplete.jsp?editable=true&docid=obchodny');
        return response.url;
    });
    I.assertEqual(new URL(deniedUrl).pathname, '/admin/403.jsp', 'The lookup must enforce webpage permission on the server.');
});

/**
 * Signs out after the permission check so the next scenario starts with the account's normal permissions.
 */
Scenario('Logout after removing webpage permission', ({ I }) => {
    I.logout();
});

/**
 * Checks that switching to documentation search cancels unfinished page suggestions. Refreshing the search
 * widget must remove the old suggestion menu and accessibility announcements before creating new controls.
 */
Scenario('Switching scope cancels pending suggestions and refreshing disposes the old autocomplete', async ({ I }) => {
    // Observe the AbortSignal directly without relying on network interception timing.
    I.executeScript(() => {
        const originalFetch = window.fetch;
        window.autotestLookupAborted = false;
        window.autotestLookupStarted = false;
        window.open = () => null;
        window.fetch = (url, options) => {
            if (!String(url).includes('/_doc_autocomplete.jsp')) return originalFetch(url, options);
            window.autotestLookupStarted = true;
            return new Promise((resolve, reject) => {
                options.signal.addEventListener('abort', () => {
                    window.autotestLookupAborted = true;
                    reject(new DOMException('Aborted', 'AbortError'));
                }, { once: true });
            });
        };
    });
    I.fillField(input, 'pending-autotest');
    I.waitForFunction(() => window.autotestLookupStarted, 10);
    I.clickCss(docs);
    I.waitForFunction(() => window.autotestLookupAborted, 10);
    I.waitForFunction(([selector]) => window.$(selector).autocomplete('instance').pending === 0, [input], 10);
    I.dontSeeElement(results);
    I.assertTrue(await I.executeScript(selector => window.$(selector).autocomplete('option', 'disabled'), input));
    const disposed = await I.executeScript(selector => {
        const oldInput = document.querySelector(selector);
        const autocomplete = window.$(oldInput).autocomplete('instance');
        const menu = autocomplete.menu.element[0];
        const liveRegion = autocomplete.liveRegion[0];
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        controller.refresh(oldInput.closest('[data-instance-id]').dataset.instanceId);
        return !menu.isConnected && !liveRegion.isConnected && !window.$(oldInput).autocomplete('instance');
    }, input);
    I.assertTrue(disposed, 'Refresh must dispose both the menu and its live region.');
    I.waitForElement(`${input}.ui-autocomplete-input`, 10);
    I.logout();
});
