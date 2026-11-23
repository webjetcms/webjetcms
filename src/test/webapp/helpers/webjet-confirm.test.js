const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Exercises the shared confirmation lifecycle with the real production callback. */
function fixture(t) {
    const dom = new JSDOM('<button id="trigger">Reset</button>');
    t.after(() => dom.window.close());
    const trigger = dom.window.document.querySelector('button');
    trigger.focus();
    const calls = [];
    let toastOptions;
    const WJ = {
        translate: key => key,
        focusWithoutTooltip: element => { calls.push(['focus', element]); element.focus(); }
    };
    const context = vm.createContext({ document: dom.window.document, WJ, toastr: {
        info: (message, title, options) => {
            toastOptions = options;
            return { off: namespace => calls.push(['off', namespace]) };
        }
    } });
    const source = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/webjet.js'), 'utf8');
    const start = source.indexOf('    function confirm(options) {');
    vm.runInContext(source.slice(start, source.indexOf('\n    /**', start + 1)), context);
    return { trigger, calls, confirm: context.confirm, hidden: () => toastOptions.onHidden() };
}

test('Shared confirmation runs optional close cleanup before restoring tooltip-safe focus', t => {
    const { trigger, calls, confirm, hidden } = fixture(t);
    confirm({ title: 'Restore defaults', onHidden: () => calls.push(['cleanup']) });
    hidden();
    assert.deepEqual(calls, [['off', '.wjConfirmA11y'], ['cleanup'], ['focus', trigger]]);
});

test('Existing confirmation callers retain focus restoration without a lifecycle callback', t => {
    const { trigger, calls, confirm, hidden } = fixture(t);
    confirm({ title: 'Existing confirmation' });
    hidden();
    assert.deepEqual(calls, [['off', '.wjConfirmA11y'], ['focus', trigger]]);
});

test('Confirmation cleanup still runs after its original trigger was removed', t => {
    const { trigger, calls, confirm, hidden } = fixture(t);
    confirm({ onHidden: () => calls.push(['cleanup']) });
    trigger.remove();
    hidden();
    assert.deepEqual(calls, [['off', '.wjConfirmA11y'], ['cleanup']]);
});
