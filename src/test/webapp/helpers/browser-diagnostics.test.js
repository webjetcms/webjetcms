const test = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');

const previousCodeceptjs = global.codeceptjs;
global.codeceptjs = require('codeceptjs');
const CustomWebjetHelper = require('./custom_helper');
if (previousCodeceptjs === undefined) delete global.codeceptjs;
else global.codeceptjs = previousCodeceptjs;

/** Captures real browser failure events without requesting a CMS or exposing request credentials. */
test('Failure diagnostics retain HTTP, network and script errors without query strings or duplicate listeners', async t => {
    const browser = await chromium.launch({ headless: true });
    t.after(() => browser.close());
    const context = await browser.newContext();
    const page = await context.newPage();
    const helper = Object.create(CustomWebjetHelper.prototype);
    Object.defineProperty(helper, 'helpers', { value: { Playwright: { page, browserContext: context } } });
    await context.route('**/*', route => {
        const url = new URL(route.request().url());
        if (url.pathname === '/translations') return route.fulfill({ status: 503, body: 'Unavailable' });
        if (url.pathname === '/broken') return route.abort('failed');
        return route.fulfill({ contentType: 'text/html', body: `<script>
            Promise.allSettled([fetch('/translations?token=private'), fetch('/broken?token=private')])
                .then(() => window.finished = true);
            setTimeout(() => { throw new Error('Missing initialization data at https://autotest.example/app.js?token=private'); }, 0);
        </script>` });
    });

    for (let attempt = 0; attempt < 2; attempt++) {
        helper._before();
        await page.goto('https://autotest.example/');
        await page.waitForFunction(() => window.finished === true);
        assert.equal(helper._diagnostics.filter(item => item.status === 503).length, 1);
        assert.ok(helper._diagnostics.some(item => item.failure === 'net::ERR_FAILED'));
        assert.ok(helper._diagnostics.some(item => item.error?.includes('Missing initialization data')));
        assert.ok(!JSON.stringify(helper._diagnostics).includes('private'));
        assert.ok(!JSON.stringify(helper._diagnostics).includes('?'));
    }
});
