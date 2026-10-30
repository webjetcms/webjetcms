const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { EventEmitter } = require('node:events');

const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/datatables-ckeditor.js');
const Editor = vm.runInNewContext(fs.readFileSync(filename, 'utf8')
    .replace('import WJ from "./webjet";', '')
    .replace('import { CkEditorToolbarOverflow } from "./ckeditor-toolbar-overflow";', '')
    .replace('export class DatatablesCkEditor', 'class DatatablesCkEditor') + '\nDatatablesCkEditor;', { console });

test('A new page write waits for the previous iframe restoration and its own dataReady', async () => {
    const ck = new EventEmitter();
    ck.status = 'recreating';
    let html = '<p>Previous page</p>';
    let finishWrite;
    ck.setData = (value, callback) => {
        // CKEditor callbacks subscribe to the next dataReady, regardless of its origin.
        ck.once('dataReady', callback);
        finishWrite = () => { html = value; ck.emit('dataReady'); };
    };
    const editor = Object.assign(Object.create(Editor.prototype), { ckEditorInstance: ck });
    let ready = false;
    const write = editor.setData('<p>Selected page</p>').then(() => { ready = true; });
    ck.status = 'ready';
    ck.emit('mode');
    ck.emit('dataReady');
    await new Promise(setImmediate);
    assert.equal(ready, false, 'Restoring the previous iframe cannot complete the new page write');
    finishWrite();
    await write;
    assert.equal(html, '<p>Selected page</p>');
    assert.equal(ready, true);
});

test('A failed content write rejects readiness instead of allowing stale content to be saved', async () => {
    const error = new Error('Detached editable');
    const editor = Object.assign(Object.create(Editor.prototype), {
        ckEditorInstance: { status: 'ready', setData() { throw error; } }
    });
    await assert.rejects(editor.setData('<p>Selected page</p>'), error);
});
