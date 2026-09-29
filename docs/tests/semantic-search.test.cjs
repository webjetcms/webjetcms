const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const { chromium } = require('../../src/test/webapp/node_modules/playwright');
const docs = path.resolve(__dirname, '..');
const deployment = '/cms/admin/docs/webjetcms/';

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
  context.on('page', page => page.on('pageerror', error => errors.push(error.message)));
  const page = await context.newPage();
  const base = 'http://docs.test';

  // Serve the actual shell and Markdown locally; only optional CDN plugins are stubbed.
  await context.route('**/*', route => {
    const url = new URL(route.request().url());
    if (url.origin === base) {
      const root = [deployment, '/latest/', '/'].find(prefix => url.pathname.startsWith(prefix));
      let relative = decodeURIComponent(url.pathname.substring(root.length)) || 'index.html';
      if (!path.extname(relative)) relative = 'index.html';
      const file = path.join(docs, relative);
      return fs.existsSync(file) ? route.fulfill({ path: file }) : route.fulfill({ status: 404, body: '' });
    }
    if (url.href === 'https://cdn.jsdelivr.net/npm/docsify@4' || url.href === 'http://cdn.jsdelivr.net/npm/docsify@4') {
      return route.fulfill({ path: path.join(docs, 'node_modules/docsify/lib/docsify.js') });
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
    { title: '<img src=x onerror=alert(1)>', url: deployment + '#/en/redactor/',
      sourcePath: '/admin/docs/webjetcms/en/redactor/README.md', snippet: '<script>alert(1)</script>' },
    { title: 'External result', url: 'https://example.com/', snippet: 'Excluded' },
    { title: 'Other collection', url: '/cms/admin/docs/other/#/en/', snippet: 'Excluded' }
  ], answer: 'Answer with <script> text.' } };
  await page.route('**/rest/rag/markdown/search?**', route => {
    requestUrl = new URL(route.request().url());
    return route.fulfill(response);
  });

  await page.goto(base + deployment + '#/en/');
  await page.locator('.sidebar-nav a[href$="/redactor/README"]').click();
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(new URL(page.url()).hash, '#/en/redactor/README');
  await page.locator('#documentation-query').fill('Edit a page');
  await page.locator('.documentation-search button').click();
  await page.waitForFunction(() => document.querySelector('.documentation-search-results').children.length === 1);
  assert.equal(requestUrl.pathname, '/cms/rest/rag/markdown/search');
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), {
    query: 'Edit a page', language: 'en', directory: '/en/redactor/'
  });
  assert.equal(await page.locator('.documentation-search input[type="checkbox"], dialog input[type="checkbox"]').count(), 0);
  assert.equal(await page.locator('dialog h2').textContent(), 'Search results');
  assert.equal(await page.locator('.documentation-search-subtitle').textContent(), 'In /en/redactor/ and its subdirectories, search all directories');
  const result = page.locator('.documentation-search-results a');
  assert.equal(await result.textContent(), '<img src=x onerror=alert(1)>');
  assert.equal(await page.locator('.documentation-search-results li p').textContent(), '<script>alert(1)</script>');
  assert.equal(await page.locator('.documentation-search-results img, .documentation-search-results script').count(), 0);
  assert.equal(await page.locator('.documentation-search-answer div').textContent(), 'Answer with <script> text.');
  assert.equal(await page.locator('.documentation-search-answer').isVisible(), true);

  const popupEvent = page.waitForEvent('popup');
  await result.click();
  const popup = await popupEvent;
  await popup.waitForURL(base + deployment + '#/en/redactor/');
  await waitForDocument(popup, 'en/redactor/README.md');
  assert.equal(await popup.evaluate(() => window.opener), null);
  assert.equal(await page.locator('dialog').isVisible(), true);
  await popup.close();

  // Scope links open fresh results for the same query without changing the documentation route.
  await page.getByRole('link', { name: 'search all directories', exact: true }).click();
  await page.waitForFunction(() => !document.querySelector('.documentation-search button').disabled);
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Edit a page', language: 'en' });
  assert.equal(await page.locator('.documentation-search-subtitle').textContent(), 'In all directories, search the current directory /en/redactor/');
  assert.equal(new URL(page.url()).hash, '#/en/redactor/README');
  response = { json: { results: [], answer: null } };
  await page.getByRole('link', { name: 'search the current directory /en/redactor/', exact: true }).focus();
  await page.keyboard.press('Enter');
  await page.waitForFunction(() => document.querySelector('.documentation-search-status').textContent === 'No results found.');
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), {
    query: 'Edit a page', language: 'en', directory: '/en/redactor/'
  });
  assert.equal(await page.locator('.documentation-search-results li').count(), 0);
  assert.equal(await page.locator('.documentation-search-answer').isVisible(), false);
  assert.equal(await page.locator('dialog[open]').count(), 1);
  await page.getByRole('link', { name: 'search all directories', exact: true }).click();
  await page.waitForFunction(() => !document.querySelector('.documentation-search button').disabled);
  await page.keyboard.press('Escape');
  await page.waitForFunction(() => document.activeElement.id === 'documentation-query');
  assert.equal(await page.locator('dialog').isVisible(), false);

  // New searches reset the scope; HTTP errors and static HTML fallback get distinct messages.
  for (const [failure, message] of [
    [{ status: 401, json: {} }, 'Sign in to search documentation.'],
    [{ status: 403, json: {} }, 'You do not have permission to search this documentation.'],
    [{ contentType: 'text/html', body: '<html>Unavailable API</html>' }, 'Documentation search is currently unavailable.']
  ]) {
    response = failure;
    await page.locator('.documentation-search button').click();
    await page.waitForFunction(expected => document.querySelector('.documentation-search-status').textContent === expected, message);
    assert.equal(requestUrl.searchParams.get('directory'), '/en/redactor/');
    await page.keyboard.press('Escape');
  }

  // New searches default to the current directory after changing language.
  await page.goto(base + deployment + '#/sk/');
  response = { json: { results: [
    { title: 'Admin', url: null, sourcePath: 'file:/docs/sk/admin/README.md', snippet: 'Admin guide' },
    { title: 'Editor', url: null, sourcePath: 'file:/docs/sk/redactor/README.md', snippet: 'Editor guide' },
    { title: 'Other collection', url: '/cms/admin/docs/other/#/sk/guide', sourcePath: '/admin/docs/other/sk/guide.md', snippet: 'Other guide' }
  ] } };
  await page.locator('#documentation-query').fill('Documentation');
  await page.locator('.documentation-search button').click();
  await page.waitForFunction(() => document.querySelector('.documentation-search-results').children.length === 3);
  assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Documentation', language: 'sk', directory: '/sk/' });
  assert.deepEqual(await page.locator('.documentation-search-results a').evaluateAll(links => links.map(link => link.href)), [
    base + deployment + '#/sk/admin/', base + deployment + '#/sk/redactor/', base + '/cms/admin/docs/other/#/sk/guide'
  ]);
  await page.keyboard.press('Escape');

  // The local preview derives its directory from the route, including README and heading URLs.
  for (const route of ['/#/sk/admin/README?id=users', '/#/sk/admin/']) {
    await page.goto(base + route);
    await waitForDocument(page, 'sk/admin/README.md');
    await page.locator('#documentation-query').fill('Users');
    await page.locator('.documentation-search button').click();
    await page.waitForFunction(() => !document.querySelector('.documentation-search button').disabled);
    assert.deepEqual(Object.fromEntries(requestUrl.searchParams), { query: 'Users', language: 'sk', directory: '/sk/admin/' });
    await page.keyboard.press('Escape');
  }

  // Keep one legacy-router smoke check without repeating the deployment/language matrix.
  await page.goto(base + '/latest/en/');
  await page.locator('.sidebar-nav a[href$="/redactor/README"]').click();
  await waitForDocument(page, 'en/redactor/README.md');
  assert.equal(new URL(page.url()).pathname, '/latest/en/redactor/README');
  response = { json: { results: [{ title: 'Editor guide', url: '/latest/#/en/redactor/', snippet: 'Guide' }], answer: null } };
  await page.locator('#documentation-query').fill('Editor guide');
  await page.locator('.documentation-search button').click();
  await page.waitForFunction(() => document.querySelector('.documentation-search-results').children.length === 1);
  assert.equal(requestUrl.searchParams.get('directory'), '/en/redactor/');
  assert.equal(await result.getAttribute('href'), base + '/latest/en/redactor/');
  assert.deepEqual(errors, []);
});
