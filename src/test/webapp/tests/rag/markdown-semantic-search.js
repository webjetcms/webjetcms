Feature('rag.markdown-semantic-search @singlethread');

// The Markdown index is currently available only on tau11. All scenarios use public, read-only search.
const documentationUrl = 'https://docs.tau11.iway.sk/latest/sk/';
const searchRoute = '**/rest/rag/markdown/search?*';
const input = '#documentation-query';
const dialogInput = '#documentation-dialog-query';
const dialog = '.documentation-search-dialog';
const submit = '.documentation-search-submit';
const dialogSubmit = '.documentation-search-dialog-submit';
const status = '.documentation-search-status';
const scope = '.documentation-search-scope';
const resultLinks = '.documentation-search-results > li > div > a';
const userQuestion = 'Ako vytvorím nového používateľa?';
let requests;
let lastSearchAt = Date.now();

/**
 * Opens the documentation with fresh request tracking before each scenario.
 * The route observer records query parameters and forwards requests to the real backend.
 * Only the request list is reset; the last search timestamp survives between scenarios
 * so their searches respect the same server cooldown without changing its configuration.
 */
Before(({ I }) => {
    I.stopMockingRoute(searchRoute);
    requests = [];
    // Observe the real requests without replacing the backend response or changing the index.
    I.mockRoute(searchRoute, route => {
        requests.push(Object.fromEntries(new URL(route.request().url()).searchParams));
        lastSearchAt = Date.now();
        return route.continue();
    });
    I.amOnPage(documentationUrl);
    I.wjSetDefaultWindowSize();
    I.waitForVisible('.markdown-section h1', 30);
    I.waitForVisible(input, 30);
});

/**
 * Verifies the complete search flow from the language root: trimming a question,
 * finding the user guide, opening it in a new tab, and refining the question in the dialog.
 * Accepts links to matching sections and checks that Escape restores focus to the sidebar.
 */
Scenario('Search finds relevant documents, opens a result and accepts a new question in the dialog', async ({ I }) => {
    await search(I, `  ${userQuestion}  `);
    await checkResults(I, userQuestion);
    I.see('Celá dokumentácia · Slovenčina', '.documentation-search-scope-label');
    I.dontSeeElement(scope);
    // A result can point directly to a matching section within the document.
    const links = await I.grabAttributeFromAll(resultLinks, 'href');
    const userUrl = links.find(url => new URL(url).pathname === '/latest/sk/admin/users/');
    I.assertTrue(Boolean(userUrl), 'A question about creating a user must find the user guide.');
    const link = locate(resultLinks).withAttr({ href: userUrl });
    I.seeAttributesOnElements(link, { target: '_blank', rel: 'noopener noreferrer' });
    const tabs = await I.grabNumberOfOpenTabs();
    I.click(link);
    I.waitForNumberOfTabs(tabs + 1, 10);
    I.switchToNextTab();
    I.waitForText('Zoznam používateľov', 30, '.markdown-section h1');
    I.closeCurrentTab();
    I.seeElement(dialog + '[open]');
    I.seeCurrentUrlEquals(documentationUrl);

    const question = 'Ako vytvorím skupinu používateľov?';
    await search(I, question, true);
    await checkResults(I, question);
    I.seeElement(resultLinks + '[href*="/admin/users/user-groups"]');
    I.pressKey('Escape');
    I.waitForInvisible(dialog, 10);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'documentation-query',
        'Closing the dialog must return focus to the sidebar search.');
    I.stopMockingRoute(searchRoute);
});

/**
 * Verifies that a parent folder includes results from its descendants.
 * Switching scope must rerun the same question across all Slovak documentation and then
 * restore the folder filter, while keeping the reader on the current document.
 */
Scenario('Parent-folder search includes descendants and can switch to all folders and back', async ({ I }) => {
    openDocument(I, 'admin/README', 'Manuál pre správcu');
    await search(I, userQuestion);
    await checkResults(I, userQuestion, '/sk/admin/');
    I.seeElement(resultLinks + '[href*="/admin/users/"]');
    I.see('/sk/admin/', '.documentation-search-scope-label');
    const pageUrl = await I.grabCurrentUrl();

    await waitForSearchCooldown(I);
    I.click(scope);
    await checkResults(I, userQuestion);
    I.see('Celá dokumentácia · Slovenčina', '.documentation-search-scope-label');
    I.seeElement(resultLinks + ':not([href*="/latest/sk/admin/"])');
    I.seeCurrentUrlEquals(pageUrl);

    await waitForSearchCooldown(I);
    I.click(scope);
    await checkResults(I, userQuestion, '/sk/admin/');
    I.see('/sk/admin/', '.documentation-search-scope-label');
    I.seeCurrentUrlEquals(pageUrl);
    I.stopMockingRoute(searchRoute);
});

/**
 * Verifies that sidebar navigation updates the directory used by subsequent searches.
 * Moving from users to scheduled tasks must replace the previous folder filter and results.
 */
Scenario('Navigating between documentation subfolders updates the search scope', async ({ I }) => {
    openDocument(I, 'admin/users/README', 'Zoznam používateľov');
    await search(I, userQuestion);
    await checkResults(I, userQuestion, '/sk/admin/users/');
    I.click('.documentation-search-close');
    I.waitForInvisible(dialog, 10);

    I.click(locate('.sidebar-nav a').withText('Automatizované úlohy'));
    I.waitForText('Automatizované úlohy', 30, '.markdown-section h1');
    const question = 'Ako nastavím automatizované úlohy?';
    await search(I, question);
    await checkResults(I, question, '/sk/admin/settings/cronjob/');
    I.see('/sk/admin/settings/cronjob/', '.documentation-search-scope-label');
    I.dontSeeElement(resultLinks + '[href*="/admin/users/"]');
    I.stopMockingRoute(searchRoute);
});

/**
 * Verifies the empty-results state and its action to search all Slovak documentation.
 * A nonexistent folder reliably produces no matches without modifying indexed chunks
 * or depending on a question falling below the semantic similarity threshold.
 */
Scenario('Empty folder results offer expansion to the complete documentation', async ({ I }) => {
    // A missing subtree has no indexed sources and does not depend on semantic similarity thresholds.
    I.amOnPage(documentationUrl + 'autotest-missing-search-folder/');
    I.waitForVisible('.markdown-section h1', 30);
    I.waitForVisible(input, 30);
    await search(I, userQuestion);
    await waitForSearch(I);
    I.see('Neboli nájdené žiadne výsledky.', status);
    I.seeNumberOfElements('.documentation-search-results > li', 0);
    I.dontSeeElement('.documentation-search-answer');
    I.dontSeeElement(scope);
    I.assertDeepEqual(requests[requests.length - 1], {
        query: userQuestion, language: 'sk', directory: '/sk/autotest-missing-search-folder/'
    });

    await waitForSearchCooldown(I);
    I.click('.documentation-search-expand');
    await checkResults(I, userQuestion);
    I.see('Celá dokumentácia · Slovenčina', '.documentation-search-scope-label');
    I.seeElement(resultLinks + '[href*="/admin/users/"]');
    I.stopMockingRoute(searchRoute);
});

/**
 * Verifies required input and the 1000-character typing limit in both search forms.
 * Empty or whitespace-only questions must cause no API request, and an invalid dialog
 * submission must preserve the previous results. The oversized input is never submitted.
 */
Scenario('Search inputs reject blank questions and enforce their length limit', async ({ I }) => {
    I.click(submit);
    I.dontSeeElement(dialog);
    I.fillField(input, '   ');
    await I.click(submit);
    I.dontSeeElement(dialog);
    I.assertLengthOf(requests, 0, 'Blank questions must not call the search API.');
    I.seeAttributesOnElements(input, { maxlength: '1000', required: true });
    I.fillField(input, 'a'.repeat(1001));
    I.assertEqual((await I.grabValueFrom(input)).length, 1000, 'The sidebar must cap typed queries at 1000 characters.');

    await search(I, userQuestion);
    await checkResults(I, userQuestion);
    const requestCount = requests.length;
    I.seeAttributesOnElements(dialogInput, { maxlength: '1000', required: true });
    I.fillField(dialogInput, 'a'.repeat(1001));
    I.assertEqual((await I.grabValueFrom(dialogInput)).length, 1000, 'The dialog must cap typed queries at 1000 characters.');
    I.fillField(dialogInput, '');
    I.click(dialogSubmit);
    I.fillField(dialogInput, '   ');
    await I.click(dialogSubmit);
    I.assertLengthOf(requests, requestCount, 'Blank dialog questions must not submit or discard the previous results.');
    I.seeElement(resultLinks);
    I.stopMockingRoute(searchRoute);
});

/**
 * Verifies the live rate-limit message, preservation of the question, and later recovery.
 * Reusing a cached question makes the response quick enough to submit again inside the
 * ten-second window. That deliberate repeat bypasses the helper's normal cooldown;
 * the final submission waits for it and must return results again.
 */
Scenario('Rapid repeated searches show the rate limit and recover after the cooldown', async ({ I }) => {
    await search(I, userQuestion);
    await checkResults(I, userQuestion);
    // Warm the cache so the immediately following submission is inside the server's ten-second window.
    await search(I, userQuestion, true);
    await checkResults(I, userQuestion);
    I.click(dialogSubmit);
    await waitForSearch(I);
    I.see('Dosiahli ste limit vyhľadávaní. Skúste to znova neskôr.', status);
    I.seeNumberOfElements('.documentation-search-results > li', 0);
    I.dontSeeElement('.documentation-search-answer');
    I.seeInField(dialogInput, userQuestion);

    await search(I, userQuestion, true);
    await checkResults(I, userQuestion);
    I.stopMockingRoute(searchRoute);
});

/**
 * Verifies that one submission produces one successful public search response with at
 * most ten results and snippets of at most 350 characters plus a truncation ellipsis.
 * Uses the response body because rendered Markdown can change the visible text length.
 * Multiple relevant matches from the same source file are allowed.
 */
Scenario('Search responses respect result and snippet limits', async ({ I }) => {
    I.startRecordingTraffic();
    await search(I, userQuestion);
    await checkResults(I, userQuestion);
    const traffic = (await I.grabRecordedNetworkTraffics()).filter(request => request.url.includes('/rest/rag/markdown/search?'));
    I.stopRecordingTraffic();
    I.stopMockingRoute(searchRoute);
    await I.assertEqual(traffic.length, 1, 'Submitting a question must send exactly one search request.');
    I.assertEqual(traffic[0].response.status, 200, 'Public documentation search must succeed without an admin login.');
    const { results } = traffic[0].response.body;
    // Multiple relevant results may come from the same source document.
    I.assertTrue(results.length <= 10, 'The search API must return at most ten results.');
    I.assertTrue(results.every(result => result.snippet.length <= 350 ||
        (result.snippet.length === 351 && result.snippet.endsWith('…'))),
        'Snippets must contain at most 350 characters plus the truncation ellipsis.');
});

/**
 * Queues navigation and waits for the expected document heading before using its search scope.
 *
 * @param {CodeceptJS.I} I - Actor used to queue browser interactions.
 * @param {string} path - Document path relative to the Slovak documentation root.
 * @param {string} heading - Expected heading used to confirm that the document has loaded.
 */
function openDocument(I, path, heading) {
    I.amOnPage(documentationUrl + path);
    I.waitForText(heading, 30, '.markdown-section h1');
    I.waitForVisible(input, 30);
}

/**
 * Waits for the documented search throttle without changing shared server configuration.
 * Uses eleven seconds since the last observed request to allow a one-second margin
 * beyond the server's ten-second window, including between separate scenarios.
 *
 * @param {CodeceptJS.I} I - Actor used to wait in the browser.
 * @returns {Promise<void>} Resolves when another search can be submitted after the cooldown.
 */
async function waitForSearchCooldown(I) {
    await I.waitForFunction(deadline => Date.now() > deadline, [lastSearchAt + 11000], 15);
}

/**
 * Waits for the cooldown, then queues a question through the sidebar or the open dialog.
 * Uses a click in the sidebar and Enter in the dialog to exercise both submission methods.
 * Call waitForSearch or checkResults afterward to wait for the response.
 *
 * @param {CodeceptJS.I} I - Actor used to queue browser interactions.
 * @param {string} question - Input text, including any whitespace being tested.
 * @param {boolean} [inDialog=false] - Whether to submit through the already open search dialog.
 * @returns {Promise<void>} Resolves after the cooldown and submission steps have been queued.
 */
async function search(I, question, inDialog = false) {
    await waitForSearchCooldown(I);
    I.fillField(inDialog ? dialogInput : input, question);
    if (inDialog) I.pressKey('Enter');
    else I.click(submit);
}

/**
 * Waits for the dialog to open and its submit button to become enabled after a response.
 * Allows up to 120 seconds for the live search and optional AI answer; completion can
 * mean success, no matches, or an error, so callers must check the resulting state.
 *
 * @param {CodeceptJS.I} I - Actor used to wait for the search controls.
 * @returns {Promise<void>} Resolves when the dialog is open and submission is enabled again.
 */
async function waitForSearch(I) {
    I.waitForVisible(dialog + '[open]', 10);
    await I.waitForEnabled(dialogSubmit, 120);
}

/**
 * Checks live results and the actual requested scope without relying on AI wording or result order.
 * Verifies the trimmed question, Slovak language, result count, links, and one snippet per result.
 * A directory filter must include only that folder and its descendants; omitting it searches
 * all Slovak documentation. Multiple results may refer to the same source document.
 *
 * @param {CodeceptJS.I} I - Actor used to inspect the page and queue assertions.
 * @param {string} question - Submitted question, compared after trimming surrounding whitespace.
 * @param {string} [directory] - Expected API folder filter, such as /sk/admin/, or no filter if omitted.
 * @returns {Promise<void>} Resolves after the response is ready and result checks have been queued.
 */
async function checkResults(I, question, directory) {
    await waitForSearch(I);
    I.see('Nájdené dokumenty', status);
    I.seeInField(input, question.trim());
    I.seeInField(dialogInput, question.trim());
    const expected = { query: question.trim(), language: 'sk' };
    if (directory) expected.directory = directory;
    I.assertDeepEqual(requests[requests.length - 1], expected, 'The search request must match the selected language and folder scope.');

    const links = await I.grabAttributeFromAll(resultLinks, 'href');
    I.assertAbove(links.length, 0, 'A relevant documentation question must return documents.');
    I.assertTrue(links.length <= 10, 'Search must return at most ten documents.');
    I.seeTextEquals(String(links.length), '.documentation-search-count');
    const paths = links.map(url => new URL(url).pathname);
    I.assertTrue(links.every(url => url.startsWith(documentationUrl)), 'Results must stay in the Slovak documentation on tau11.');
    if (directory) I.assertTrue(paths.every(path => path.startsWith('/latest' + directory)),
        'Folder search must exclude sibling folders while allowing all descendants.');
    I.seeNumberOfElements('.documentation-search-snippet', links.length);
}
