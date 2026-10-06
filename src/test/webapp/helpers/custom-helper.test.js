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
