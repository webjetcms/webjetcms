const test = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');

const previousCodeceptjs = global.codeceptjs;
global.codeceptjs = require('codeceptjs');
const CustomWebjetHelper = require('./custom_helper');
if (previousCodeceptjs === undefined) delete global.codeceptjs;
else global.codeceptjs = previousCodeceptjs;

/** Exercises the real browser cookie jar without connecting to a CMS or sending HTTP requests. */
test('Session cookie lookup ignores other domains and inapplicable paths', async t => {
    const browser = await chromium.launch({ headless: true });
    t.after(() => browser.close());
    const context = await browser.newContext();
    await context.addCookies([
        { name: 'JSESSIONID', value: 'autotest-other-session', url: 'https://autotest-other.example/', httpOnly: true },
        { name: 'JSESSIONID', value: 'autotest-private-session', domain: 'autotest-current.example', path: '/private', secure: true, httpOnly: true },
        { name: 'JSESSIONID', value: 'autotest-current-session', url: 'https://autotest-current.example/', httpOnly: true }
    ]);
    const helper = Object.create(CustomWebjetHelper.prototype);
    Object.defineProperty(helper, 'helpers', { value: { Playwright: {
        browserContext: context,
        page: { url: () => 'https://autotest-current.example/admin/v9/webpages/' }
    } } });

    const cookie = await helper.grabCookieForCurrentUrl('JSESSIONID');
    assert.equal(cookie.value, 'autotest-current-session');
    assert.equal(cookie.httpOnly, true);
    await context.clearCookies();
    await context.addCookies([
        { name: 'JSESSIONID', value: 'autotest-other-session', url: 'https://autotest-other.example/', httpOnly: true }
    ]);
    await assert.rejects(helper.grabCookieForCurrentUrl('JSESSIONID'), /missing for the current page URL/);
});
