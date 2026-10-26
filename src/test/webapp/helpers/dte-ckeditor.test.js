const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function fixture(editor, frame = null) {
    let readiness;
    const I = {
        waitForElement() {},
        waitForVisible() {},
        waitForFunction(predicate) { readiness = predicate; },
        clickCss() {},
        pressKey() {},
        wait() {},
        executeScript: async (callback, value) => callback(value)
    };
    const context = { module: { exports: {} }, inject: () => ({ I }), require: () => ({}),
        window: { ckEditorInstance: editor }, document: { getElementById: () => frame } };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../pages/DTE.js'), 'utf8'), context);
    return { DTE: context.module.exports, ready: () => readiness() };
}

test('Filling CKEditor waits for delayed dataReady before allowing a save', async () => {
    let complete;
    let saved = false;
    const editor = { setData(html, callback) { this.html = html; complete = callback; } };
    const { DTE } = fixture(editor);
    const fill = DTE.fillCkeditor('<p>Autotest</p>').then(() => { saved = true; });
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(saved, false, 'A slow setData must not race the next save step');
    assert.equal(editor.html, '<p>Autotest</p>');
    complete();
    await fill;
    assert.equal(saved, true);
});

test('A ready toolbar with an initializing or detached editable is not ready for input', () => {
    const body = { isConnected: false };
    const editor = { status: 'loaded', mode: 'wysiwyg', editable: () => ({ $: body }) };
    const { DTE, ready } = fixture(editor);
    DTE.waitForCkeditor();
    assert.equal(Boolean(ready()), false);
    editor.status = 'ready';
    assert.equal(Boolean(ready()), false);
    body.isConnected = true;
    assert.equal(ready(), true);
    editor.mode = 'source';
    assert.equal(ready(), true, 'Source mode remains supported');
});

test('Page Builder readiness follows its active frame, not the hidden source editor', () => {
    const editor = { status: 'ready', mode: 'source', editable: () => ({ $: { isConnected: true } }) };
    const frame = { getClientRects: () => [{}], contentWindow: { pageBuilderReady: false }, contentDocument: { body: { inert: true } } };
    const { DTE, ready } = fixture(editor, frame);
    DTE.waitForCkeditor();
    assert.equal(ready(), false);
    frame.contentWindow.pageBuilderReady = true;
    assert.equal(ready(), false);
    frame.contentDocument.body.inert = false;
    assert.equal(ready(), true);
});
