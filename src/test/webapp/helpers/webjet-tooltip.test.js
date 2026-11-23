const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');
const jquery = require('jquery');

/** Runs the shared tooltip keyboard handler with the real Bootstrap tooltip plugin. */
function fixture(t) {
    const dom = new JSDOM('<!doctype html><div class="modal show"><button data-bs-title="autotest tooltip">Control</button></div>',
        { runScripts: 'outside-only' });
    t.after(() => dom.window.close());
    const { window } = dom;
    const $ = jquery(window);
    window.jQuery = $;
    window.eval(fs.readFileSync(require.resolve('bootstrap/dist/js/bootstrap.bundle.js'), 'utf8'));
    window.document.dispatchEvent(new window.Event('DOMContentLoaded'));
    const source = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/webjet.js'), 'utf8');
    const start = source.indexOf('    function initTooltip(');
    const initTooltip = vm.runInNewContext(source.slice(start, source.indexOf('\n    /**', start)) + '\ninitTooltip;',
        { $, document: window.document, WJ: {} });
    const button = window.document.querySelector('button');
    initTooltip($(button));
    const tooltip = window.bootstrap.Tooltip.getInstance(button);
    t.after(() => tooltip.dispose());
    let escapes = 0;
    button.parentElement.addEventListener('keydown', event => { if (event.key === 'Escape') escapes++; });
    const escape = () => {
        const event = new window.KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true });
        button.dispatchEvent(event);
        return event;
    };
    return { window, tooltip, escape, escapes: () => escapes };
}

test('Escape reaches the parent dialog when a focused control has no visible tooltip', t => {
    const { escape, escapes } = fixture(t);
    assert.equal(escape().defaultPrevented, false);
    assert.equal(escapes(), 1);
});

test('Escape dismisses a visible tooltip and the next Escape reaches the parent dialog', t => {
    const { window, tooltip, escape, escapes } = fixture(t);
    tooltip.show();
    assert.ok(window.document.querySelector('.tooltip.show'));
    assert.equal(escape().defaultPrevented, true);
    assert.equal(escapes(), 0);
    assert.equal(window.document.querySelector('.tooltip.show'), null);
    assert.equal(escape().defaultPrevented, false);
    assert.equal(escapes(), 1);
});
