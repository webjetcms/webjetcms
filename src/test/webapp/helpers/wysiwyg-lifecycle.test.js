const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Uses the real Editor lifecycle with controllable asynchronous rich-text operations. */
function fixture(t, editExisting = false) {
    const dom = new JSDOM('<!doctype html><html><body><div id="modal"></div></body></html>');
    const window = dom.window;
    const $ = require('jquery')(window);
    // jsdom has no layout; expose attached form messages as visible and finish animations immediately.
    $.expr.pseudos.visible = element => element.isConnected && element.style.display !== 'none';
    $.fx.off = true;
    require('datatables.net')(window, $);
    require('datatables.net-editor')(window, $);
    const writes = [];
    const errors = [];
    const requests = [];
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
    const table = editExisting ? $('<table>').appendTo(window.document.body).DataTable({
        data: [{ DT_RowId: 'row_1', data: '<p>Original content</p>' }], columns: [{ data: 'data' }],
        paging: false, searching: false, info: false
    }) : null;
    editor = new $.fn.dataTable.Editor({
        fields: [{ name: 'data', type: 'wysiwyg', entityDecode: false }], display: 'autotest',
        table: table ? table.table().node() : undefined,
        ajax(method, url, data, success) {
            requests.push(data);
            success({ data: Object.values(data.data).map(row => ({ DT_RowId: 'row_1', ...row })) });
        }
    });
    editor.TABLE = {};
    const field = editor.field('data');
    const conf = field.s.opts;
    t.after(() => window.close());
    return {
        editor, field, conf, writes, errors, requests, window, $,
        usePageBuilder() {
            const source = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/datatables-ckeditor.js');
            const Editor = vm.runInContext(fs.readFileSync(source, 'utf8')
                .replace('import WJ from "./webjet";', '')
                .replace('export class DatatablesCkEditor', 'class DatatablesCkEditor') + '\nDatatablesCkEditor;', context);
            conf.wjeditor = Object.assign(Object.create(Editor.prototype), {
                editingMode: 'pageBuilder', options: { fieldid: conf._id },
                ckEditorInstance: { getData: () => '<p>Stale hidden content</p>' }
            });
            const frame = window.document.getElementById(conf._id + '-pageBuilderIframe');
            frame.contentWindow.pageBuilderReady = true;
            frame.contentWindow.getSaveData = () => ({ editable: [{ wjAppField: 'doc_data', data: '<p>Visible Page Builder content</p>' }] });
            return frame;
        },
        open(value) {
            editor.one(editExisting ? 'initEdit' : 'initCreate', () => {
                editor.currentJson = { data: value, editorFields: {} };
                field.val(value);
            });
            if (editExisting) editor.edit(0, { submit: 'changed' });
            else editor.create();
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

test('Invalid content also blocks saving an existing row with changed-only submission', async t => {
    const f = fixture(t, true);
    await f.open('<p>Original content</p>');
    f.conf.wjeditor.value = undefined;
    f.editor.submit();
    assert.equal(f.requests.length, 0);
    assert.equal(f.editor.display(), 'main', 'An invalid read must not be treated as an unchanged row');
    assert.equal(f.field.inError(), true);
    assert.equal(f.editor.s.processing, false);
});

const pageBuilderFailures = {
    'initialization unfinished': frame => { frame.contentWindow.pageBuilderReady = false; },
    'serializer throws': frame => { frame.contentWindow.getSaveData = () => { throw new Error('Autotest failed serialization'); }; },
    'missing doc_data': frame => { frame.contentWindow.getSaveData = () => ({ editable: [] }); },
    'undefined doc_data': frame => { frame.contentWindow.getSaveData = () => ({ editable: [{ wjAppField: 'doc_data' }] }); },
    'literal undefined doc_data': frame => { frame.contentWindow.getSaveData = () => ({ editable: [{ wjAppField: 'doc_data', data: 'undefined' }] }); }
};
for (const [name, fail] of Object.entries(pageBuilderFailures)) {
    test(`Page Builder ${name} blocks saving instead of using hidden content`, async t => {
        const f = fixture(t);
        await f.open('<p>Original</p>');
        fail(f.usePageBuilder());
        f.editor.submit();
        assert.equal(f.requests.length, 0);
        assert.equal(f.editor.display(), 'main');
        assert.equal(f.field.inError(), true);
        assert.equal(f.editor.s.processing, false);
    });
}

test('A ready Page Builder submits its visible content', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    f.usePageBuilder();
    f.editor.submit();
    assert.equal(f.requests.length, 1);
    assert.equal(f.requests[0].data[0].data, '<p>Visible Page Builder content</p>');
});

for (const value of [undefined, 'undefined', ' \nundefined\t', null]) {
    test(`Invalid content (${JSON.stringify(value)}) blocks submission and preserves the open form`, async t => {
        const f = fixture(t);
        await f.open('<p>Original content</p>');
        f.conf.wjeditor.value = value;
        let laterHandlerCalled = false;
        f.editor.on('initSubmit', () => { laterHandlerCalled = true; return true; });
        f.editor.submit();
        assert.equal(f.requests.length, 0);
        assert.equal(laterHandlerCalled, false, 'Later handlers cannot override cancellation');
        assert.equal(f.editor.display(), 'main');
        assert.equal(f.editor.s.processing, false, 'Save must remain usable after an error');
        assert.equal(f.field.inError(), true);
        assert.match(f.$('#modal .DTE_Form_Error').text(), /editor.content.read_error.js/);
        assert.equal(f.conf._input.val(), '<p>Original content</p>', 'Invalid reads cannot erase the last valid input');

        f.conf.wjeditor.value = '<p>Recovered content</p>';
        f.editor.submit();
        assert.equal(f.requests.length, 1, 'A corrected value can be saved without reopening');
        assert.equal(f.requests[0].data[0].data, '<p>Recovered content</p>');
    });
}

test('A getter exception is reported without sending a request or leaving Save busy', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    f.conf.wjeditor.getData = () => { throw new Error('Autotest serialization failed'); };
    f.editor.submit();
    assert.equal(f.requests.length, 0);
    assert.equal(f.editor.s.processing, false);
    assert.equal(f.field.inError(), true);
});

test('A failure while collecting the payload is stopped before preview or save handlers', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    let calls = 0;
    f.conf.wjeditor.getData = () => ++calls === 1 ? '<p>Valid at initSubmit</p>' : undefined;
    let previewCalled = false;
    f.editor.on('preSubmit', () => { previewCalled = true; return true; });
    f.editor.submit();
    assert.equal(f.requests.length, 0);
    assert.equal(previewCalled, false);
    assert.equal(f.editor.s.processing, false);
    assert.equal(f.field.inError(), true);
});

for (const throws of [false, true]) {
    test(`A ${throws ? 'throwing' : 'missing return in a'} custom HTML callback blocks saving`, async t => {
        const f = fixture(t);
        await f.open('<p>Original</p>');
        f.window.wysiwygGetCallback = () => {
            if (throws) throw new Error('Autotest callback failed');
        };
        f.editor.submit();
        assert.equal(f.requests.length, 0);
        assert.equal(f.field.inError(), true);
        assert.equal(f.editor.s.processing, false);
    });
}

for (const value of ['', '<p>undefined is a JavaScript value</p>']) {
    test(`Valid content (${JSON.stringify(value)}) can still be saved`, async t => {
        const f = fixture(t);
        await f.open(value);
        f.editor.submit();
        assert.equal(f.requests.length, 1);
        assert.equal(f.requests[0].data[0].data, value);
    });
}

test('Editor attributes are removed on input and output without rewriting unrelated HTML', async t => {
    const f = fixture(t);
    const original = `<section CONTENTEDITABLE = "true" class='hero'>
<div contenteditable='false'><b contenteditable=plaintext-only>Text</b></div>
<p contenteditable contenteditable="">Empty value</p>
<template><span contenteditable="false">Template</span></template>
<img data-contenteditable="true" title='Keep contenteditable="true" >' src='/img?a=1&amp;b=2' />
<!-- <div contenteditable="true">Comment</div> -->
<script contenteditable="false">const html = '<b contenteditable="true">';</script>
<style>.a::after { content: '<i contenteditable="true">'; }</style>
<textarea><div contenteditable="true">Example</div></textarea>
&lt;b contenteditable="true"&gt;
!INCLUDE(/components/example.jsp, text="contenteditable='true'")!
</section>`;
    const cleaned = `<section class='hero'>
<div><b>Text</b></div>
<p>Empty value</p>
<template><span>Template</span></template>
<img data-contenteditable="true" title='Keep contenteditable="true" >' src='/img?a=1&amp;b=2' />
<!-- <div contenteditable="true">Comment</div> -->
<script>const html = '<b contenteditable="true">';</script>
<style>.a::after { content: '<i contenteditable="true">'; }</style>
<textarea><div contenteditable="true">Example</div></textarea>
&lt;b contenteditable="true"&gt;
!INCLUDE(/components/example.jsp, text="contenteditable='true'")!
</section>`;
    await f.open(original);
    assert.equal(f.conf.wjeditor.value, cleaned, 'Persisted attributes must be removed before initialization');
    // Simulate attributes typed into source mode after set(), including callback-generated HTML.
    f.conf.wjeditor.value = original;
    f.window.wysiwygGetCallback = html => html + '<p contenteditable=false>Callback</p>';
    f.editor.submit();
    assert.equal(f.requests.length, 1);
    assert.equal(f.requests[0].data[0].data, cleaned + '<p>Callback</p>');
    assert.equal(f.conf.wjeditor.value, original, 'Serialization must not modify the live editable DOM');
});

test('Uppercase CONTENTEDITABLE is removed even without any lowercase occurrence', async t => {
    const f = fixture(t);
    await f.open('<p CONTENTEDITABLE="true">Content</p>');
    assert.equal(f.conf.wjeditor.value, '<p>Content</p>');
    f.conf.wjeditor.value = '<p CONTENTEDITABLE="false">Content</p>';
    f.editor.submit();
    assert.equal(f.requests[0].data[0].data, '<p>Content</p>');
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
