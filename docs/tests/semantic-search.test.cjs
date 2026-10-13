const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const { chromium } = require('../../src/test/webapp/node_modules/playwright');
const docs = path.resolve(__dirname, '..');

/**
 * Waits until Docsify renders the heading from the requested local Markdown document.
 *
 * @param {import('playwright').Page} page - Viewer page to inspect.
 * @param {string} source - Markdown file path relative to the documentation directory.
 * @returns {Promise<void>} Resolves when the rendered heading matches the file's first line.
 */
async function waitForDocument(page, source) {
  const heading = fs.readFileSync(path.join(docs, source), 'utf8').split('\n')[0].replace(/^# /, '');
  await page.waitForFunction(expected => document.querySelector('.markdown-section h1')?.textContent.trim() === expected, heading);
}

test('Docsify navigation, scoped search, safe results and request errors', async t => {
  const browser = await chromium.launch({ headless: true });
  t.after(() => browser.close());
  const context = await browser.newContext({ locale: 'en-US' });
  const errors = [];
  const scriptRequests = [];
  let releaseSearchScript;
  const pendingSearchScript = new Promise(resolve => { releaseSearchScript = resolve; });
  t.after(() => releaseSearchScript());
  let searchScriptMode = 'pending';
  context.on('page', page => page.on('pageerror', error => errors.push(error.message)));
  const page = await context.newPage();
  const base = 'http://iwcm.interway.sk';
  const latestBase = 'http://docs.test';

  // Serve the actual shell and Markdown locally; only optional CDN plugins are stubbed.
  await context.route('**/*', async route => {
    const url = new URL(route.request().url());
    if (url.origin === base || url.origin === latestBase) {
      if (url.pathname.endsWith('/semantic-search.js')) {
        scriptRequests.push(url.pathname + url.search);
        if (searchScriptMode === 'pending') await pendingSearchScript;
        if (searchScriptMode === 'failed') return route.abort('failed');
      }
      const root = url.pathname.startsWith('/latest/') ? '/latest/' : '/';
      let relative = decodeURIComponent(url.pathname.substring(root.length)) || 'index.html';
      if (!path.extname(relative)) relative = 'index.html';
      const file = path.join(docs, relative);
      return fs.existsSync(file) ? route.fulfill({ path: file }) : route.fulfill({ status: 404, body: '' });
    }
    if (url.href === 'https://cdn.jsdelivr.net/npm/docsify@4' || url.href === 'http://cdn.jsdelivr.net/npm/docsify@4') {
      return route.fulfill({ path: path.join(docs, 'node_modules/docsify/lib/docsify.js') });
    }
    if (url.pathname.endsWith('docsify-themeable.min.js')) {
      // The theme initializes the Markdown configuration used by the viewer's link renderer.
      return route.fulfill({ contentType: 'text/javascript', body: 'window.$docsify.plugins.unshift(function (hook) { hook.init(function () { window.$docsify.markdown = { renderer: {} }; }); });' });
    }
    if (url.pathname.endsWith('mermaid.esm.min.mjs')) {
      return route.fulfill({ contentType: 'text/javascript', body: 'export default { initialize() {} };' });
    }
    if (url.pathname.endsWith('theme-simple-dark.css')) {
      return route.fulfill({ path: path.join(docs, 'node_modules/docsify/lib/themes/dark.css') });
    }
    return route.fulfill({ body: '', contentType: url.pathname.endsWith('.css') ? 'text/css' : 'text/javascript' });
  });

  let requestUrl;
  let response = { json: { results: [
    { title: '<img src=x onerror=alert(1)>', url: '/#/en/redactor/',
      sourcePath: 'file:/docs/en/redactor/README.md', snippet: [
        '## Editor', 'Read **bold**, *italic* and `<code>`.', '- First\n- Second', '> Quote',
        '```html\n<img src=x onerror=alert(1)>\n```',
        '[Link](javascript:alert(1)) ![Image](https://example.com/image.png)',
        '<script>alert(1)</script>', 'Inline <code><img src=x onerror=alert(1)></code>'
      ].join('\n\n') },
    { title: 'External result', url: 'https://example.com/', snippet: 'Excluded' },
    { title: 'Invalid path', url: '/%00/en/', snippet: 'Excluded' }
  ], answer: 'Answer with <script> text.' } };
  await page.route('**/rest/rag/markdown/search?**', async route => {
    requestUrl = new URL(route.request().url());
    assert.equal(await page.locator('#documentation-dialog-query').isDisabled(), true);
    assert.equal(await page.locator('.documentation-search-progress').isVisible(), true);
    return route.fulfill(response);
  });

  await page.goto(base + '/en/', { waitUntil: 'domcontentloaded' });
  await page.locator('.sidebar-nav a[href$="/redactor/README"]').click();
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(new URL(page.url()).pathname, '/en/redactor/README');
  assert.equal(await page.locator('#documentation-query').count(), 0);
  searchScriptMode = 'normal';
  releaseSearchScript();
  await page.locator('#documentation-query').fill('Edit a page');
  await page.locator('.documentation-search button').click();
  await page.waitForFunction(() => document.querySelector('.documentation-search-results').children.length === 1);
  assert.equal(requestUrl.pathname, '/rest/rag/markdown/search');
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), {
    query: 'Edit a page', language: 'en', directory: '/en/redactor/'
  });
  assert.equal(await page.locator('.documentation-search input[type="checkbox"], dialog input[type="checkbox"]').count(), 0);
  const dialogQuery = page.locator('#documentation-dialog-query');
  assert.equal(await dialogQuery.isEnabled(), true);
  assert.equal(await dialogQuery.inputValue(), 'Edit a page');
  assert.equal(await page.locator('.documentation-search-scope-label').textContent(), 'In /en/redactor/ and its subdirectories · English');
  const result = page.locator('.documentation-search-results a');
  assert.equal(await result.textContent(), '<img src=x onerror=alert(1)>');
  const snippet = page.locator('.documentation-search-snippet');
  assert.equal(await snippet.locator('h2').textContent(), 'Editor');
  assert.equal(await snippet.locator('h2').getAttribute('id'), null);
  assert.equal(await snippet.locator('strong').textContent(), 'bold');
  assert.equal(await snippet.locator('em').textContent(), 'italic');
  assert.equal(await snippet.locator('p > code').textContent(), '<code>');
  assert.deepEqual(await snippet.locator('ul > li').allTextContents(), ['First', 'Second']);
  assert.equal(await snippet.locator('blockquote').textContent().then(text => text.trim()), 'Quote');
  assert.equal(await snippet.locator('pre code').textContent(), '<img src=x onerror=alert(1)>\n');
  assert.match(await snippet.textContent(), /Link Image/);
  assert.match(await snippet.textContent(), /<script>alert\(1\)<\/script>/);
  assert.match(await snippet.textContent(), /Inline <code><img src=x onerror=alert\(1\)><\/code>/);
  assert.equal(await snippet.locator('a, img, script').count(), 0);
  assert.equal(await snippet.locator('li').first().evaluate(item => getComputedStyle(item).borderTopWidth), '0px');
  assert.equal(await page.locator('.documentation-search-results img, .documentation-search-results script').count(), 0);
  assert.equal(await page.locator('.documentation-search-answer div').textContent(), 'Answer with <script> text.');
  assert.equal(await page.locator('.documentation-search-answer').isVisible(), true);

  const popupEvent = page.waitForEvent('popup');
  await result.click({ modifiers: [process.platform === 'darwin' ? 'Meta' : 'Control'] });
  const popup = await popupEvent;
  await popup.waitForURL(base + '/en/redactor/');
  await waitForDocument(popup, 'en/redactor/README.md');
  assert.equal(await page.locator('dialog').isVisible(), true);
  await popup.close();

  await result.click();
  await page.waitForURL(base + '/en/redactor/');
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(context.pages().length, 1);
  assert.equal(await page.locator('dialog[open]').count(), 0);
  await page.goBack();
  await page.waitForURL(base + '/en/redactor/README');
  await waitForDocument(page, 'en/redactor/README.md');
  await page.locator('#documentation-query').fill('Edit a page');
  await page.locator('.documentation-search button').click();
  await page.waitForFunction(() => !document.querySelector('.documentation-search-dialog-submit').disabled);

  // Editing the dialog question preserves the current scope and synchronizes the sidebar.
  await dialogQuery.fill('Update a page');
  await dialogQuery.press('Enter');
  await page.waitForFunction(() => !document.querySelector('.documentation-search-dialog-submit').disabled);
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Update a page', language: 'en', directory: '/en/redactor/' });
  assert.equal(await page.locator('#documentation-query').inputValue(), 'Update a page');
  await dialogQuery.fill('Edit a page');

  // Scope links use the edited question without changing the documentation route.
  response.json.answerHtml = '<p>Open <strong>Settings</strong>.</p><ol><li>Create a task.</li></ol>' +
    '<table><tr><th>Name</th><td>Task label</td></tr></table><pre><code>example|parameter</code></pre>';
  await page.getByRole('link', { name: 'search all directories', exact: true }).click();
  await page.waitForFunction(() => !document.querySelector('.documentation-search button').disabled);
  assert.equal(await page.locator('.documentation-search-answer p > strong').textContent(), 'Settings');
  assert.equal(await page.locator('.documentation-search-answer ol > li').textContent(), 'Create a task.');
  assert.equal(await page.locator('.documentation-search-answer pre').isVisible(), true);
  assert.equal(await page.locator('.documentation-search-answer td').textContent(), 'Task label');
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Edit a page', language: 'en' });
  assert.equal(await page.locator('.documentation-search-scope-label').textContent(), 'All documentation · English');
  assert.equal(new URL(page.url()).pathname, '/en/redactor/README');
  await dialogQuery.fill('Find a page');
  await page.locator('.documentation-search-dialog-submit').click();
  await page.waitForFunction(() => !document.querySelector('.documentation-search-dialog-submit').disabled);
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Find a page', language: 'en' });
  assert.equal(await page.locator('dialog[open]').count(), 1);
  await dialogQuery.fill('Edit a page');
  response = { json: { results: [], answer: null } };
  await page.getByRole('link', { name: 'search the current directory /en/redactor/', exact: true }).focus();
  await page.keyboard.press('Enter');
  await page.waitForFunction(() => document.querySelector('.documentation-search-state strong')?.textContent === 'No results found.');
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), {
    query: 'Edit a page', language: 'en', directory: '/en/redactor/'
  });
  assert.equal(await page.locator('.documentation-search-results > li').count(), 0);
  assert.equal(await page.locator('.documentation-search-answer').isVisible(), false);
  assert.equal(await page.locator('dialog[open]').count(), 1);
  await page.getByRole('button', { name: 'Search all documentation', exact: true }).click();
  await page.waitForFunction(() => !document.querySelector('.documentation-search button').disabled);
  assert.equal(requestUrl.searchParams.has('directory'), false);
  assert.equal(await page.locator('.documentation-search-expand').isVisible(), false);
  await page.keyboard.press('Escape');
  await page.waitForFunction(() => document.activeElement.id === 'documentation-query');
  assert.equal(await page.locator('dialog').isVisible(), false);

  // New searches reset the scope; HTTP errors and static HTML fallback get distinct messages.
  for (const [failure, message] of [
    [{ status: 401, json: {} }, 'Sign in to search documentation.'],
    [{ status: 403, json: {} }, 'You do not have permission to search this documentation.'],
    [{ status: 429, json: {} }, 'Search limit reached. Please try again later.'],
    [{ contentType: 'text/html', body: '<html>Unavailable API</html>' }, 'Documentation search is currently unavailable.']
  ]) {
    response = failure;
    await page.locator('.documentation-search button').click();
    await page.waitForFunction(expected => document.querySelector('.documentation-search-status').textContent === expected, message);
    assert.equal(requestUrl.searchParams.get('directory'), '/en/redactor/');
    assert.equal(await page.locator('#documentation-query').inputValue(), 'Edit a page');
    assert.equal(await page.locator('.documentation-search button').isEnabled(), true);
    assert.equal(await dialogQuery.isEnabled(), true);
    assert.equal(await page.locator('.documentation-search-state').count(), 0);
    await page.keyboard.press('Escape');
  }

  // Language-root searches omit the directory filter and scope action.
  const searchForm = await page.locator('.documentation-search').elementHandle();
  await page.locator('.app-nav a[href="/sk/"]').evaluate(link => link.click());
  await waitForDocument(page, 'sk/README.md');
  assert.equal(await searchForm.evaluate(form => form === document.querySelector('.documentation-search')), true);
  assert.equal(await page.locator('html').getAttribute('lang'), 'sk');
  response = { json: { results: [
    { title: 'Admin', url: null, sourcePath: 'file:/docs/sk/admin/README.md', snippet: 'Admin guide' },
    { title: 'Editor', url: null, sourcePath: 'file:/docs/sk/redactor/README.md', snippet: 'Editor guide' }
  ] } };
  await page.locator('#documentation-query').fill('Documentation');
  await page.locator('.documentation-search button').click();
  await page.waitForFunction(() => document.querySelector('.documentation-search-results').children.length === 2);
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Documentation', language: 'sk' });
  assert.equal(await page.locator('.documentation-search-scope').isVisible(), false);
  assert.deepEqual(await page.locator('.documentation-search-results a').evaluateAll(links => links.map(link => link.href)), [
    base + '/sk/admin/', base + '/sk/redactor/'
  ]);
  response = { json: { results: [], answer: null } };
  await page.locator('.documentation-search-dialog-submit').click();
  await page.waitForFunction(() => !document.querySelector('.documentation-search-dialog-submit').disabled);
  assert.equal(await page.locator('.documentation-search-state').isVisible(), true);
  assert.equal(await page.locator('.documentation-search-expand, .documentation-search-scope').evaluateAll(items => items.every(item => item.hidden)), true);

  // Returning to the previous route closes and relocalizes the existing search controls.
  await page.goBack();
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(await page.locator('dialog').isVisible(), false);
  assert.equal(await page.locator('#documentation-query').getAttribute('placeholder'), 'Enter your question…');
  assert.equal(await searchForm.evaluate(form => form === document.querySelector('.documentation-search')), true);

  // History routes derive the directory from README and heading URLs.
  for (const route of ['/sk/admin/README?id=users', '/sk/admin/']) {
    await page.goto(base + route);
    await waitForDocument(page, 'sk/admin/README.md');
    await page.locator('#documentation-query').fill('Users');
    await page.locator('.documentation-search button').click();
    await page.waitForFunction(() => !document.querySelector('.documentation-search button').disabled);
    assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Users', language: 'sk', directory: '/sk/admin/' });
    await page.keyboard.press('Escape');
  }

  // Existing root-level language URLs keep their document, root assets, and history navigation.
  for (const language of ['sk', 'en', 'cs']) {
    const documentUrl = base + '/' + language + '/admin/users/README';
    await page.goto(documentUrl);
    await waitForDocument(page, language + '/admin/users/README.md');
    assert.equal(page.url(), documentUrl);
    assert.equal(await page.evaluate(() => window.$docsify.routerMode), 'history');
    const version = await page.evaluate(() => window.version);
    assert.equal(await page.locator('link[href*="style.css?v="]').evaluate(link => link.href), base + '/style.css?v=' + version);
    assert.equal(await page.locator('script[src*="assets/semantic-search.js"]').getAttribute('src'), '/assets/semantic-search.js?v=' + version);
    await page.locator('.sidebar-nav a[href$="/admin/users/user-groups"]').click();
    await waitForDocument(page, language + '/admin/users/user-groups.md');
    assert.equal(page.url(), base + '/' + language + '/admin/users/user-groups');
    await page.reload();
    await waitForDocument(page, language + '/admin/users/user-groups.md');
  }

  // The published viewer retains its /latest/ history routes and search asset location.
  scriptRequests.length = 0;
  await page.goto(latestBase + '/latest/en/');
  await page.locator('#documentation-query').waitFor();
  const version = await page.evaluate(() => window.version);
  assert.deepEqual(scriptRequests, ['/latest/assets/semantic-search.js?v=' + version]);
  assert.equal(await page.locator('link[href*="style.css?v="]').evaluate(link => link.href), latestBase + '/latest/style.css?v=' + version);
  await page.locator('.sidebar-nav a[href$="/redactor/README"]').click();
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(new URL(page.url()).pathname, '/latest/en/redactor/README');
  response = { json: { results: [
    { title: 'Editor guide', url: '/latest/#/en/redactor/', snippet: 'Guide > Editing pages\n\nEdit a page.' },
    { title: 'Other collection', url: '/cms/admin/docs/other/#/en/guide', sourcePath: '/admin/docs/other/en/guide.md', snippet: 'Other guide' }
  ], answer: null } };
  await page.locator('#documentation-query').fill('Editor guide');
  await page.locator('.documentation-search button').click();
  await page.waitForFunction(() => document.querySelector('.documentation-search-results').children.length === 2);
  assert.equal(requestUrl.searchParams.get('directory'), '/en/redactor/');
  assert.deepEqual(await result.evaluateAll(links => links.map(link => link.href)), [
    latestBase + '/latest/en/redactor/?id=editing-pages', latestBase + '/cms/admin/docs/other/en/guide'
  ]);

  // Delay the module and sidebar to verify initialization before Docsify mounts.
  let releaseModule, releaseSidebar;
  const pendingModule = new Promise(resolve => { releaseModule = resolve; });
  const pendingSidebar = new Promise(resolve => { releaseSidebar = resolve; });
  t.after(() => { releaseModule(); releaseSidebar(); });
  await page.route('**/mermaid.esm.min.mjs', async route => {
    await pendingModule;
    return route.fallback();
  });
  await page.route('**/_sidebar.md', async route => {
    await pendingSidebar;
    return route.fallback();
  });
  await page.addInitScript(() => {
    document.addEventListener('load', event => {
      if (event.target.src?.includes('/assets/semantic-search.js')) window.searchScriptLoaded = true;
    }, true);
  });
  await page.goto(latestBase + '/latest/en/redactor/README', { waitUntil: 'commit' });
  await page.waitForFunction(() => window.searchScriptLoaded === true);
  assert.equal(await page.locator('.sidebar').count(), 0);
  releaseModule();
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(await page.locator('#documentation-query').count(), 1);
  assert.equal(await page.locator('.documentation-search-dialog').count(), 1);
  releaseSidebar();
  await page.locator('.sidebar-nav a').first().waitFor();
  assert.equal(await page.locator('#documentation-query').count(), 1);
  assert.equal(await page.locator('.documentation-search-dialog').count(), 1);

  // A failed search download must not prevent reading or navigating documentation.
  searchScriptMode = 'failed';
  await page.goto(latestBase + '/latest/en/');
  await page.locator('.sidebar-nav a[href$="/redactor/README"]').click();
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(await page.locator('#documentation-query').count(), 0);
  assert.deepEqual(errors, []);
});
