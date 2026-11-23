const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { EventEmitter } = require('node:events');

const key = 'webjet.multistepform.clipboard.7.autotest.example';
const source = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/apps/form/admin/form-steps.html'), 'utf8');

/** Tests clipboard callbacks; browser tests cover the real Editor and field controls. */
function fixture(t) {
    const stored = new Map();
    const window = Object.assign(new EventTarget(), {
        URL, location: { origin: 'http://autotest.example' },
        localStorage: {
            getItem: key => stored.get(key) ?? null, setItem: (key, value) => stored.set(key, value),
            removeItem: key => stored.delete(key), clear: () => stored.clear()
        }
    });
    const state = { now: 100000, stepId: 20, item: { id: 11, fieldType: 'text', label: 'Autotest', regexValidationArr: [7] } };
    const buttons = {};
    const events = new EventEmitter();
    const editor = {
        display: () => false, setJson: t.mock.fn(),
        on(names, handler) { names.split(' ').forEach(name => events.on(name, handler)); }
    };
    const table = {
        EDITOR: editor, DATA: { jsonOptions: { fieldType: [{ value: 'text' }] } },
        getAjaxUrl: () => '/admin/rest/form-items?stepId=' + state.stepId,
        rows: () => ({ data: () => ({ toArray: () => [{ id: 11 }] }), count: () => 1 }),
        on() {}, wjCreate: t.mock.fn(),
        button: name => name ? buttons[name.split(':')[0]] : {
            add(index, options) { buttons[options.name] = { ...options, enable(value) { this.enabled = value; } }; }
        }
    };
    window.setTimeout = callback => { state.expire = callback; };
    window.clearTimeout = () => {};
    const notifyError = t.mock.fn();
    vm.runInNewContext(source.slice(source.indexOf('    function initFormItemClipboard('), source.indexOf('\n    window.renderStepName')) +
        "\ninitFormItemClipboard(7, 'autotest.example');", {
        window, Date: { now: () => state.now }, urlFormName: 'destination-autotest', formItemsDataTable: table,
        formStepsDataTable: { on() {}, rows: () => ({ data: () => ({ toArray: () => [{ id: state.stepId }] }) }) },
        fetch: async () => ({ ok: !state.denied, json: async () => state.item }),
        WJ: { translate: key => key, urlAddPath: (url, suffix) => url.replace('?', suffix + '?'), notifySuccess() {}, notifyInfo() {}, notifyError }
    });
    const refresh = () => window.dispatchEvent(Object.assign(new Event('storage'), { key }));
    return { window, state, buttons, editor, notifyError, refresh, storage: window.localStorage,
        open: () => events.emit('preOpen', {}, 'main', 'create') };
}

test('Paste uses the captured snapshot and destination when the editor opens', async t => {
    const f = fixture(t);
    await f.buttons.copyField.action();
    const snapshot = JSON.parse(f.storage.getItem(key));
    assert.deepEqual(snapshot.item, { fieldType: 'text', label: 'Autotest', regexValidationArr: [7] });
    assert.equal(snapshot.expiresAt, f.state.now + 1800000);
    f.buttons.pasteField.action();
    assert.equal(f.editor.setJson.mock.callCount(), 0);
    assert.deepEqual(JSON.parse(f.storage.getItem(key)), snapshot, 'Paste must not consume or extend the snapshot');
    snapshot.item.label = 'Copied in another tab';
    f.storage.setItem(key, JSON.stringify(snapshot));
    f.refresh();
    f.state.stepId = 30;
    f.editor.currentJson = { sortPriority: 80 };
    f.open();
    const pasted = f.editor.setJson.mock.calls[0].arguments[0];
    assert.equal(pasted.label, 'Autotest');
    assert.equal(pasted.stepId, 20);
    assert.equal(pasted.sortPriority, 80);
    assert.deepEqual(Array.from(pasted.regexValidationArr), [7]);
});

test('Invalid, unsupported, differently scoped and expired snapshots disable paste', async t => {
    const f = fixture(t);
    await f.buttons.copyField.action();
    const snapshot = JSON.parse(f.storage.getItem(key));
    for (const value of ['{invalid', {}, { ...snapshot, version: 2 },
        { ...snapshot, item: { fieldType: 'unavailable' } },
        { ...snapshot, item: { fieldType: 'text', regexValidationArr: ['invalid'] } }]) {
        f.storage.setItem(key, typeof value === 'string' ? value : JSON.stringify(value));
        f.refresh();
        assert.equal(f.buttons.pasteField.enabled, false);
    }
    f.storage.clear();
    for (const otherKey of [key.replace('.7.', '.8.'), key.replace('autotest.example', 'other.example')]) {
        f.storage.setItem(otherKey, JSON.stringify(snapshot));
    }
    f.refresh();
    assert.equal(f.buttons.pasteField.enabled, false);
    await f.buttons.copyField.action();
    assert.equal(f.buttons.pasteField.enabled, true);
    f.state.now = snapshot.expiresAt;
    f.state.expire();
    assert.equal(f.buttons.pasteField.enabled, false);
    assert.equal(f.storage.getItem(key), null);
});

test('Denied access preserves the snapshot; blocked storage disables paste and reports an error', async t => {
    const f = fixture(t);
    await f.buttons.copyField.action();
    const snapshot = f.storage.getItem(key);
    f.state.denied = true;
    await f.buttons.copyField.action();
    assert.equal(f.storage.getItem(key), snapshot);
    f.state.denied = false;
    Object.defineProperty(f.window, 'localStorage', { get() { throw new Error('Autotest storage blocked'); } });
    await f.buttons.copyField.action();
    assert.equal(f.buttons.pasteField.enabled, false);
    assert.equal(f.buttons.copyField.enabled, true);
    assert.equal(f.notifyError.mock.callCount(), 2);
});
