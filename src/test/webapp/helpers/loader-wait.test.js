const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { chromium } = require('playwright');
const Playwright = require('codeceptjs/lib/helper/Playwright');

let browser;
test.before(async () => { browser = await chromium.launch({ headless: true }); });
test.after(async () => { await browser?.close(); });

for (const [source, method, cssClass] of [
    ['pages/DT.js', 'waitForLoader', 'dt-processing'],
    ['pages/DTE.js', 'waitForLoader', 'DTE_Processing_Indicator'],
    ['steps_file.js', 'dtWaitForLoader', 'dt-processing'],
    ['steps_file.js', 'dtEditorWaitForLoader', 'DTE_Processing_Indicator']
]) {
    test(`${source} ${method} waits past a hidden first indicator`, async t => {
        const page = await browser.newPage();
        t.after(() => page.close());
        await page.setContent(`<div class="${cssClass}" style="display:none">Idle editor</div>
            <div id="saving" class="${cssClass}">Saving another editor</div>`);

        const helper = Object.create(Playwright.prototype);
        helper.options = { waitForTimeout: 3000 };
        helper._getContext = async () => page;
        const steps = [];
        const I = {
            wait() {},
            waitForInvisible(selector) {
                steps.push(async () => {
                    await helper.waitForInvisible(selector, 3);
                    assert.equal(await page.locator('#saving').isVisible(), false,
                        'A hidden indicator must not let the wait skip another active request');
                });
            }
        };
        const context = vm.createContext({
            module: { exports: {} }, inject: () => ({ I }),
            require: () => ({}), actor: methods => methods
        });
        const filename = path.resolve(__dirname, '..', source);
        vm.runInContext(fs.readFileSync(filename, 'utf8'), context, { filename });
        const exported = context.module.exports;
        const methods = typeof exported === 'function' ? exported() : exported;
        methods[method].call(Object.assign({}, methods, I));

        await page.evaluate(() => {
            setTimeout(() => { document.querySelector('#saving').style.display = 'none'; }, 500);
        });
        for (const step of steps) await step();
    });
}
