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
    const confirmations = [];
    const notifications = [];
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
    window.WJ = {
        translate: key => key, initTooltip() {}, setAdminSetting() {},
        confirm: options => confirmations.push(options),
        notifyError: message => notifications.push(message)
    };
    $.fn.selectpicker = function(action, value) {
        if (action === 'val') this.val(value);
        return this;
    };
    const context = vm.createContext({
        window, document: window.document, $, console: { ...console, error: (...args) => errors.push(args) },
        setTimeout: window.setTimeout.bind(window),
        WJ: window.WJ
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
            success({ data: Object.entries(data.data).map(([id, row]) => ({ DT_RowId: id === '0' ? 'row_1' : id, ...row })) });
        }
    });
    editor.TABLE = {};
    const field = editor.field('data');
    const conf = field.s.opts;
    t.after(() => window.close());
    return {
        editor, field, conf, writes, errors, requests, confirmations, notifications, window, $,
        usePreview() {
            const source = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/pages/web-pages-list/preview.js');
            const Preview = vm.runInContext(fs.readFileSync(source, 'utf8')
                .replace('export class EditorPreview', 'class EditorPreview') + '\nEditorPreview;', context);
            const preview = new Preview({ datatable: { EDITOR: editor, hasPermission: () => false } });
            preview.bindEvents();
            editor.currentJson.docId = -1;
            const previewRequests = [];
            $.ajax = options => { previewRequests.push(options); };
            return { preview, requests: previewRequests };
        },
        usePageBuilder() {
            const toolbar = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/ckeditor-toolbar-overflow.js');
            vm.runInContext(fs.readFileSync(toolbar, 'utf8').replace(/^export /gm, ''), context, { filename: toolbar });
            const source = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/datatables-ckeditor.js');
            const Editor = vm.runInContext(fs.readFileSync(source, 'utf8')
                .replace('import WJ from "./webjet";', '')
                .replace('import { CkEditorToolbarOverflow } from "./ckeditor-toolbar-overflow";', '')
                .replace('export class DatatablesCkEditor', 'class DatatablesCkEditor') + '\nDatatablesCkEditor;', context);
            let hiddenHtml = '<p>Stale hidden content</p>';
            conf.wjeditor = Object.assign(Object.create(Editor.prototype), {
                editingMode: 'pageBuilder', options: { fieldid: conf._id },
                setJson: RichTextEditor.prototype.setJson,
                resizeEditor() {},
                ckEditorInstance: {
                    mode: 'source',
                    getData: () => hiddenHtml,
                    setData(value, done) { hiddenHtml = value; if (done) done(); },
                    setMode(mode, done) { this.mode = mode; if (done) done(); }
                }
            });
            const frame = window.document.getElementById(conf._id + '-pageBuilderIframe');
            frame.contentWindow.pageBuilderReady = true;
            frame.contentWindow.getSaveData = () => ({ editable: [{ wjAppField: 'doc_data', data: '<p>Visible Page Builder content</p>' }] });
            return frame;
        },
        switchMode(mode) {
            const select = window.document.getElementById(conf._id + '-editorTypeSelect-select');
            select.value = mode;
            return window.switchEditorType(select);
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
        openMultiple(submit = 'all') {
            editor.add({ name: 'title' });
            table.row.add({ DT_RowId: 'row_2', data: '<p>Second page</p>' }).draw();
            editor.currentJson = { data: '<p>Original content</p>', editorFields: {} };
            editor.edit([0, 1], { submit });
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

for (const submit of ['all', 'changed']) {
    test(`Bulk metadata edits preserve distinct HTML with ${submit} submission`, async t => {
        const f = fixture(t, true);
        await f.openMultiple(submit);
        assert.equal(f.field.isMultiValue(), true);
        assert.equal(f.field.get(), undefined, 'Distinct values have no common HTML');
        f.editor.field('title').val('Updated title');
        f.editor.submit();
        assert.equal(f.requests.length, 1);
        const rows = f.requests[0].data;
        assert.equal(rows.row_1.title, 'Updated title');
        assert.equal(rows.row_2.title, 'Updated title');
        if (submit === 'all') {
            assert.equal(rows.row_1.data, '<p>Original content</p>');
            assert.equal(rows.row_2.data, '<p>Second page</p>');
        } else {
            assert.equal(Object.hasOwn(rows.row_1, 'data'), false);
            assert.equal(Object.hasOwn(rows.row_2, 'data'), false);
        }
    });
}

test('Bulk edits reuse one validated HTML value for every page', async t => {
    const f = fixture(t, true);
    await f.openMultiple();
    f.field.val('<p>Shared content</p>');
    await f.conf.wjeditorReadyPromise;
    assert.equal(f.field.isMultiValue(), false);
    let reads = 0;
    f.conf.wjeditor.getData = () => ++reads === 2 ? undefined : '<p>Shared content</p>';
    f.editor.submit();
    assert.equal(reads, 1);
    assert.equal(f.requests.length, 1);
    assert.equal(f.requests[0].data.row_1.data, '<p>Shared content</p>');
    assert.equal(f.requests[0].data.row_2.data, '<p>Shared content</p>');
});

for (const value of [undefined, 'undefined']) {
    test(`Bulk edits reject an invalid individual value (${JSON.stringify(value)})`, async t => {
        const f = fixture(t, true);
        await f.openMultiple();
        f.field.multiSet({ row_1: '<p>Original content</p>', row_2: value });
        f.editor.field('title').val('Updated title');
        assert.equal(f.field.isMultiValue(), true);
        f.editor.submit();
        assert.equal(f.requests.length, 0);
        assert.equal(f.field.inError(), true);
        assert.equal(f.editor.s.processing, false);
    });

    test(`Bulk edits reject invalid shared HTML (${JSON.stringify(value)}) and recover after restoring individual values`, async t => {
        const f = fixture(t, true);
        await f.openMultiple();
        f.field.val('<p>Shared content</p>');
        await f.conf.wjeditorReadyPromise;
        f.conf.wjeditor.value = value;
        assert.equal(f.field.isMultiValue(), false);
        f.editor.submit();
        assert.equal(f.requests.length, 0);
        assert.equal(f.field.inError(), true);
        assert.equal(f.editor.s.processing, false);

        f.field.multiRestore();
        assert.equal(f.field.isMultiValue(), true);
        f.editor.field('title').val('Updated title');
        f.editor.submit();
        assert.equal(f.requests.length, 1, 'The previous read error must not block restored values');
        assert.equal(f.requests[0].data.row_1.data, '<p>Original content</p>');
        assert.equal(f.requests[0].data.row_2.data, '<p>Second page</p>');
    });
}

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

for (const failure of ['initialization', 'serialization']) {
    test(`HTML recovery after Page Builder ${failure} failure requires confirmation and preserves the latest available content`, async t => {
        const f = fixture(t);
        await f.open('<p>Original</p>');
        const frame = f.usePageBuilder();
        let expected = '<p>Original</p>';
        if (failure === 'initialization') {
            frame.contentWindow.pageBuilderReady = false;
        } else {
            expected = f.field.get();
            frame.contentWindow.getSaveData = () => { throw new Error('Autotest serialization failed'); };
        }
        await f.switchMode('html');
        assert.equal(f.confirmations.length, 1);
        assert.equal(f.conf.wjeditor.editingMode, 'pageBuilder');
        assert.equal(f.conf.wjeditor.ckEditorInstance.getData(), '<p>Stale hidden content</p>');
        f.editor.submit();
        assert.equal(f.requests.length, 0, 'Saving must stay blocked before recovery is confirmed');

        await f.confirmations[0].success();
        assert.equal(f.conf.wjeditor.editingMode, 'html');
        assert.equal(f.window.editorTypeForced, 'html');
        assert.equal(f.field.get(), expected);
        f.editor.submit();
        assert.equal(f.requests.length, 1);
        assert.equal(f.requests[0].data[0].data, expected);
    });
}

test('Dismissing HTML recovery preserves Page Builder and resets the mode selector', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    f.usePageBuilder().contentWindow.pageBuilderReady = false;
    f.window.editorTypeForced = 'pageBuilder';
    await f.switchMode('html');
    assert.equal(f.confirmations.length, 1);
    // Dismissing WJ.confirm does not invoke its success callback.
    assert.equal(f.conf.wjeditor.editingMode, 'pageBuilder');
    assert.equal(f.window.document.getElementById(f.conf._id + '-editorTypeSelect-select').value, 'pageBuilder');
    assert.equal(f.window.editorTypeForced, 'pageBuilder');
    assert.equal(f.conf._input.val(), '<p>Original</p>');
    f.editor.submit();
    assert.equal(f.requests.length, 0);
});

for (const when of ['before prompting', 'before confirmation']) {
    test(`HTML recovery rejects an invalid preserved value ${when}`, async t => {
        const f = fixture(t);
        await f.open('<p>Original</p>');
        f.usePageBuilder().contentWindow.pageBuilderReady = false;
        if (when === 'before prompting') f.conf._input.val('undefined');
        await f.switchMode('html');
        if (when === 'before confirmation') {
            assert.equal(f.confirmations.length, 1);
            f.conf._input.val('undefined');
            await f.confirmations[0].success();
        } else {
            assert.equal(f.confirmations.length, 0);
        }
        assert.equal(f.conf.wjeditor.editingMode, 'pageBuilder');
        assert.equal(f.notifications.length, 1);
        assert.equal(f.conf.wjeditor.ckEditorInstance.getData(), '<p>Stale hidden content</p>');
    });
}

test('HTML recovery accepts intentionally empty content', async t => {
    const f = fixture(t);
    await f.open('');
    f.usePageBuilder().contentWindow.pageBuilderReady = false;
    await f.switchMode('html');
    await f.confirmations[0].success();
    assert.equal(f.field.get(), '');
});

for (const change of ['close', 'reopen', 'history', 'new request']) {
    test(`HTML recovery ignores a stale confirmation after ${change}`, async t => {
        const f = fixture(t);
        await f.open('<p>Original</p>');
        f.usePageBuilder().contentWindow.pageBuilderReady = false;
        await f.switchMode('html');
        const confirmation = f.confirmations[0];
        if (change === 'close' || change === 'reopen') f.editor.close();
        if (change === 'reopen') await f.open('<p>Another page</p>');
        if (change === 'history') {
            f.field.setJson({ data: '<p>History</p>', editorFields: {} });
            f.field.val('<p>History</p>');
            await f.conf.wjeditorReadyPromise;
        }
        if (change === 'new request') await f.switchMode('html');
        const html = f.conf.wjeditor.ckEditorInstance.getData();
        await confirmation.success();
        assert.equal(f.conf.wjeditor.editingMode, 'pageBuilder');
        assert.equal(f.conf.wjeditor.ckEditorInstance.getData(), html);
        assert.equal(f.window.editorTypeForced, undefined);
    });
}

test('A page opened during HTML recovery waits for the old content write to finish', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    f.usePageBuilder().contentWindow.pageBuilderReady = false;
    await f.switchMode('html');
    const recovery = f.confirmations[0].success();
    f.editor.close();
    const reopened = f.open('<p>Another page</p>');
    await Promise.all([recovery, reopened]);
    assert.equal(f.conf.wjeditor.ckEditorInstance.getData(), '<p>Another page</p>');
    assert.equal(f.conf._input.val(), '<p>Another page</p>');
    assert.equal(f.window.editorTypeForced, undefined);
});

test('A ready Page Builder switches to HTML with its live content without prompting', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    f.usePageBuilder();
    await f.switchMode('html');
    assert.equal(f.confirmations.length, 0);
    assert.equal(f.conf.wjeditor.editingMode, 'html');
    assert.equal(f.field.get(), '<p>Visible Page Builder content</p>');
});

test('A user mode switch waits for the initial content to finish loading', async t => {
    const f = fixture(t);
    const release = f.holdNextMode();
    const opened = f.open('<p>Original</p>');
    await new Promise(setImmediate);
    const calls = [];
    f.conf.wjeditor.switchEditingMode = mode => { calls.push(mode); };
    const switched = f.switchMode('html');
    await new Promise(setImmediate);
    assert.deepEqual(calls, []);
    release();
    await Promise.all([opened, switched]);
    assert.deepEqual(calls, ['html']);
    assert.equal(f.field.get(), '<p>Original</p>');
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

for (const editExisting of [false, true]) {
    test(`${editExisting ? 'Changed-only edits' : 'New pages'} submit the validated HTML without another live read`, async t => {
        const f = fixture(t, editExisting);
        await f.open('<p>Original content</p>');
        let reads = 0;
        f.conf.wjeditor.getData = () => ++reads === 1 ? '<p>Updated content</p>' : undefined;
        f.editor.submit(null, null, null, false);
        assert.equal(reads, 1);
        assert.equal(f.requests.length, 1);
        assert.equal(Object.values(f.requests[0].data)[0].data, '<p>Updated content</p>');
        assert.equal(f.editor.s.processing, false);
        assert.equal(f.field.inError(), false);
        f.conf.wjeditor.getData = () => '<p>Further edits</p>';
        assert.equal(f.field.get(), '<p>Further edits</p>', 'Live reads resume after submission');
    });
}

test('Editor formatting is applied once to the captured HTML', async t => {
    const f = fixture(t);
    await f.open('<p>Content</p>');
    f.conf.getFormatter = html => html + '<!-- formatted -->';
    f.editor.submit();
    assert.equal(f.requests[0].data[0].data, '<p>Content</p><!-- formatted -->');
});

test('An unchanged submission releases the validated HTML without preSubmit', async t => {
    const f = fixture(t, true);
    await f.open('<p>Original content</p>');
    f.editor.submit(null, null, null, false);
    assert.equal(f.requests.length, 0);
    f.conf.wjeditor.value = '<p>Further edits</p>';
    assert.equal(f.field.get(), '<p>Further edits</p>');
});

test('An asynchronous initSubmit cancellation releases the validated HTML', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    let release;
    f.editor.one('initSubmit', () => new Promise(resolve => { release = resolve; }));
    f.editor.submit();
    f.conf.wjeditor.value = '<p>Further edits</p>';
    release(false);
    await new Promise(setImmediate);
    assert.equal(f.requests.length, 0);
    assert.equal(f.field.get(), '<p>Further edits</p>');
    f.editor.submit();
    assert.equal(f.requests[0].data[0].data, '<p>Further edits</p>');
});

for (const failureStage of ['initSubmit', 'preSubmit']) {
    test(`A preview cancelled at ${failureStage} does not intercept the next save`, async t => {
        const f = fixture(t);
        await f.open('<p>Original</p>');
        if (failureStage === 'initSubmit') f.conf.wjeditor.getData = () => undefined;
        else {
            f.editor.one('preSubmit', event => {
                f.field.error('Autotest validation failed');
                event.stopImmediatePropagation();
                return false;
            });
        }
        const p = f.usePreview();
        f.window.previewPage();
        assert.equal(f.requests.length, 0);
        assert.equal(p.requests.length, 0);
        assert.equal(f.field.inError(), true);
        assert.equal(f.editor.s.processing, false);
        assert.equal(p.preview.isPreviewClicked, false);

        f.conf.wjeditor.getData = () => '<p>Recovered</p>';
        f.editor.submit();
        assert.equal(f.requests.length, 1);
        assert.equal(f.requests[0].data[0].data, '<p>Recovered</p>');
        assert.equal(p.requests.length, 0, 'Save must not send a preview request');
    });
}

test('Closing an editor clears a pending preview before another page is opened', async t => {
    const f = fixture(t);
    await f.open('<p>Original</p>');
    const p = f.usePreview();
    let release;
    f.editor.one('initSubmit', () => new Promise(resolve => { release = resolve; }));
    f.window.previewPage();
    assert.equal(p.preview.isPreviewClicked, true);
    f.editor.close();
    const pendingAfterClose = p.preview.isPreviewClicked;
    release(false);
    await new Promise(setImmediate);
    assert.equal(pendingAfterClose, false, 'Closing must clear the intent even before processing ends');

    await f.open('<p>Another page</p>');
    f.editor.currentJson.docId = -1;
    f.editor.submit();
    assert.equal(f.requests.length, 1);
    assert.equal(f.requests[0].data[0].data, '<p>Another page</p>');
    assert.equal(p.requests.length, 0);
});

for (const asynchronous of [false, true]) {
    test(`A valid preview remains a preview with ${asynchronous ? 'asynchronous' : 'synchronous'} submission`, async t => {
        const f = fixture(t);
        await f.open('<p>Preview content</p>');
        const p = f.usePreview();
        let reads = 0;
        f.conf.wjeditor.getData = () => ++reads === 1 ? '<p>Preview content</p>' : undefined;
        let release;
        if (asynchronous) {
            f.editor.one('initSubmit', () => new Promise(resolve => { release = resolve; }));
        }
        f.window.previewPage();
        if (asynchronous) {
            assert.equal(p.preview.isPreviewClicked, true);
            assert.equal(p.requests.length, 0);
            release(true);
            await new Promise(setImmediate);
        }
        assert.equal(f.requests.length, 0, 'Preview must not save the page');
        assert.equal(reads, 1);
        assert.equal(p.requests.length, 1);
        assert.equal(p.requests[0].url, '/admin/rest/web-pages/preview/');
        assert.equal(JSON.parse(p.requests[0].data).data, '<p>Preview content</p>');
        assert.equal(p.preview.isPreviewClicked, false);
        assert.equal(f.editor.s.processing, false);
    });
}

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
