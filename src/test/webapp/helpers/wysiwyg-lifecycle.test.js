const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Uses the real Editor lifecycle with controllable asynchronous rich-text operations. */
function fixture(t) {
    const dom = new JSDOM('<!doctype html><html><body><div id="modal"></div></body></html>');
    const window = dom.window;
    const $ = require('jquery')(window);
    require('datatables.net')(window, $);
    require('datatables.net-editor')(window, $);
    const writes = [];
    const errors = [];
    let nextMode;
    let editor;
    class RichTextEditor {
        constructor(options) {
            this.value = '';
            this.ckEditorInstance = {};
            queueMicrotask(options.onReady);
        }
        setJson(json) {
            this.json = json;
            const ready = nextMode || Promise.resolve();
            nextMode = null;
            return ready;
        }
        setData(value) {
            writes.push({ value, open: editor.display(), attached: editor.field('data').node().isConnected });
            this.value = value;
            return Promise.resolve();
        }
        getData() { return this.value; }
    }
    window.CKEDITOR = {};
    window.currentUser = { login: 'autotest' };
    window.createDatatablesCkEditor = async () => ({ DatatablesCkEditor: RichTextEditor });
    const context = vm.createContext({
        window, document: window.document, $, console: { ...console, error: (...args) => errors.push(args) },
        setTimeout: window.setTimeout.bind(window),
        WJ: { translate: key => key, initTooltip() {} }
    });
    const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-wysiwyg.js');
    vm.runInContext(fs.readFileSync(filename, 'utf8').replace(/^export /gm, ''), context, { filename });
    $.fn.dataTable.Editor.fieldTypes.wysiwyg = context.typeWysiwyg();
    $.fn.dataTable.Editor.display.autotest = {
        captureFocus: false,
        init() { return this; },
        open(editor, form, callback) {
            $('#modal').children().detach();
            $('#modal').append(form).show();
            callback();
        },
        close(editor, callback) { $('#modal').hide(); callback(); },
        node() { return window.document.querySelector('#modal'); }
    };
    editor = new $.fn.dataTable.Editor({ fields: [{ name: 'data', type: 'wysiwyg' }], display: 'autotest' });
    editor.TABLE = {};
    const field = editor.field('data');
    const conf = field.s.opts;
    t.after(() => window.close());
    return {
        editor, field, conf, writes, errors,
        open(value) {
            editor.one('initCreate', () => {
                editor.currentJson = { data: value, editorFields: {} };
                field.val(value);
            });
            editor.create();
            return conf.wjeditorReadyPromise.then(() => new Promise(setImmediate));
        },
        holdNextMode() {
            let release;
            nextMode = new Promise(resolve => { release = resolve; });
            return release;
        }
    };
}

test('Initial rich-text content waits for the editing mode to finish', async t => {
    const f = fixture(t);
    const release = f.holdNextMode();
    const ready = f.open('<p>Autotest initial content</p>');
    await new Promise(setImmediate);
    assert.deepEqual(f.writes, [], 'The mode change must finish before setData starts');
    release();
    await ready;
    assert.equal(f.field.val(), '<p>Autotest initial content</p>');
    assert.equal(f.conf.dirtyDataOriginal, '<p>Autotest initial content</p>');
});

test('Reopening applies defaults only after the form is attached and open', async t => {
    const f = fixture(t);
    await f.open('<p>Autotest previous page</p>');
    f.editor.close();
    const previousWrites = f.writes.length;
    f.field.val('<p>Autotest hidden defaults</p>');
    assert.equal(f.field.val(), '<p>Autotest hidden defaults</p>', 'A hidden field must return its stored value');
    assert.equal(f.writes.length, previousWrites, 'Hidden defaults must not write to the old iframe');
    await f.open('<p>Autotest new page</p>');
    assert.equal(f.field.val(), '<p>Autotest new page</p>');
    assert.ok(f.writes.every(write => write.open && write.attached));
    assert.equal(f.conf.dirtyDataOriginal, '<p>Autotest new page</p>');
});

test('History content waits for its template mode change', async t => {
    const f = fixture(t);
    await f.open('<p>Autotest original</p>');
    const release = f.holdNextMode();
    f.field.setJson({ data: '<p>Autotest history</p>', editorFields: {} });
    f.field.val('<p>Autotest history</p>');
    await new Promise(setImmediate);
    assert.equal(f.conf.wjeditor.getData(), '<p>Autotest original</p>');
    release();
    await f.conf.wjeditorReadyPromise;
    assert.equal(f.field.val(), '<p>Autotest history</p>');
});

test('Queued writes from a closed dialog cannot overwrite the next page', async t => {
    const f = fixture(t);
    await f.open('<p>Autotest first page</p>');
    const release = f.holdNextMode();
    f.field.setJson({ data: '<p>Autotest obsolete page</p>', editorFields: {} });
    f.field.val('<p>Autotest obsolete page</p>');
    await new Promise(setImmediate);
    f.editor.close();
    const ready = f.open('<p>Autotest next page</p>');
    release();
    await ready;
    assert.equal(f.field.val(), '<p>Autotest next page</p>');
    assert.ok(f.writes.every(write => write.value !== '<p>Autotest obsolete page</p>'));
});

test('Content and template updates during initial construction are retained', async t => {
    const f = fixture(t);
    const initialReady = f.open('<p>Autotest initial content</p>');
    assert.equal(f.conf.wjeditor, null, 'The rich-text instance must still be loading');
    const history = { data: '<p>Autotest latest content</p>', editorFields: { editingMode: 'html' } };
    f.field.setJson(history);
    f.field.val(history.data);
    await initialReady;
    await f.conf.wjeditorReadyPromise;
    assert.equal(f.conf.wjeditor.json, history, 'The latest template must be applied');
    assert.equal(f.field.val(), history.data, 'The latest content must replace the initial value');
    assert.equal(f.conf.dirtyDataOriginal, history.data);
});

for (const synchronous of [false, true]) {
    test(`A ${synchronous ? 'thrown' : 'rejected'} content write does not block later writes`, async t => {
        const f = fixture(t);
        await f.open('<p>Autotest original</p>');
        const error = new Error('Autotest failed content write');
        const setData = f.conf.wjeditor.setData.bind(f.conf.wjeditor);
        f.conf.wjeditor.setData = () => {
            if (synchronous) throw error;
            return Promise.reject(error);
        };
        f.field.val('<p>Autotest failed write</p>');
        await assert.rejects(f.conf.wjeditorReadyPromise, error);
        await new Promise(setImmediate);
        assert.equal(f.conf.dirtyDataOriginal, undefined, 'A failed write must not establish a clean snapshot');
        assert.ok(f.errors.some(args => args.includes(error)), 'The failure must be reported');
        f.conf.wjeditor.setData = setData;
        f.field.val('<p>Autotest recovered content</p>');
        await f.conf.wjeditorReadyPromise;
        await new Promise(setImmediate);
        assert.equal(f.field.val(), '<p>Autotest recovered content</p>');
        assert.equal(f.conf.dirtyDataOriginal, '<p>Autotest recovered content</p>');
    });
}

test('A failed template blocks dependent content until a valid template is applied', async t => {
    const f = fixture(t);
    await f.open('<p>Autotest original</p>');
    const error = new Error('Autotest failed template');
    const setJson = f.conf.wjeditor.setJson.bind(f.conf.wjeditor);
    f.conf.wjeditor.setJson = () => Promise.reject(error);
    f.field.setJson({ data: '<p>Autotest invalid history</p>', editorFields: {} });
    f.field.val('<p>Autotest invalid history</p>');
    await assert.rejects(f.conf.wjeditorReadyPromise, error);
    f.field.val('<p>Autotest still invalid</p>');
    await assert.rejects(f.conf.wjeditorReadyPromise, error);
    await new Promise(setImmediate);
    assert.equal(f.conf.wjeditor.getData(), '<p>Autotest original</p>');
    assert.equal(f.conf.dirtyDataOriginal, undefined);
    f.conf.wjeditor.setJson = setJson;
    f.field.setJson({ data: '<p>Autotest valid history</p>', editorFields: {} });
    f.field.val('<p>Autotest valid history</p>');
    await f.conf.wjeditorReadyPromise;
    assert.equal(f.field.val(), '<p>Autotest valid history</p>');
});

test('Reopening recovers from a synchronous template initialization failure', async t => {
    const f = fixture(t);
    await f.open('<p>Autotest original</p>');
    f.editor.close();
    const error = new Error('Autotest failed initialization');
    const setJson = f.conf.wjeditor.setJson.bind(f.conf.wjeditor);
    f.conf.wjeditor.setJson = () => { throw error; };
    await assert.rejects(f.open('<p>Autotest failed page</p>'), error);
    await new Promise(setImmediate);
    assert.equal(f.conf.dirtyDataOriginal, undefined);
    f.editor.close();
    f.conf.wjeditor.setJson = setJson;
    await f.open('<p>Autotest recovered page</p>');
    assert.equal(f.field.val(), '<p>Autotest recovered page</p>');
    assert.equal(f.conf.dirtyDataOriginal, '<p>Autotest recovered page</p>');
    assert.ok(f.writes.every(write => write.value !== '<p>Autotest failed page</p>'));
});
