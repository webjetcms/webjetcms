const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

/** Loads the helper without starting the CodeceptJS browser container. */
function fixture(playwright) {
    const module = { exports: {} };
    const source = fs.readFileSync(path.join(__dirname, 'custom_helper.js'), 'utf8');
    vm.runInNewContext(source, { module, codeceptjs: { Helper: class {} }, console });
    const helper = new module.exports();
    helper.helpers = { Playwright: playwright };
    return helper;
}

test('clickIfVisible waits for the browser click before resolving', async () => {
    let finishClick;
    let completed = false;
    const helper = fixture({
        grabNumberOfVisibleElements: async () => 1,
        click: () => new Promise(resolve => { finishClick = resolve; })
    });
    const result = helper.clickIfVisible('.toast-close-button').then(value => {
        completed = true;
        return value;
    });
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(typeof finishClick, 'function');
    assert.equal(completed, false, 'Subsequent actions must wait while Playwright is still clicking');
    finishClick();
    assert.equal(await result, true);
});

test('clickIfVisible skips an absent element without starting a click', async () => {
    const helper = fixture({
        grabNumberOfVisibleElements: async () => 0,
        click: () => assert.fail('An absent element must not be clicked')
    });
    assert.equal(await helper.clickIfVisible('.toast-close-button'), false);
});

test('screenshots after a failed named session ignore closed pages from the previous test', async t => {
    const Playwright = require('codeceptjs/lib/helper/Playwright');
    const outputDirectory = Object.getOwnPropertyDescriptor(global, 'output_dir');
    global.output_dir = __dirname;
    t.after(() => {
        if (outputDirectory) Object.defineProperty(global, 'output_dir', outputDirectory);
        else delete global.output_dir;
    });
    let screenshots = 0;
    const page = { screenshot: async () => { screenshots++; } };
    const playwright = {
        page, options: {}, debug() {},
        activeSessionName: 'failed session',
        sessionPages: { 'failed session': { screenshot: async () => { throw new Error('Target page has been closed'); } } }
    };
    const screenshot = () => Playwright.prototype.saveScreenshot.call(playwright, 'autotest-session.png');
    await assert.rejects(screenshot, /Target page has been closed/);

    fixture(playwright)._before();
    await screenshot();
    assert.equal(screenshots, 2, 'The live page must still be captured after clearing previous sessions');
    assert.equal(playwright.page, page, 'The active Playwright page must remain unchanged');

    playwright.activeSessionName = 'new session';
    playwright.sessionPages['new session'] = page;
    await screenshot();
    assert.equal(screenshots, 4, 'Named sessions created in the current test must still be captured');
    page.screenshot = async () => { throw new Error('Current page has been closed'); };
    await assert.rejects(screenshot, /Current page has been closed/, 'A real failure in the current page must remain visible');
});
