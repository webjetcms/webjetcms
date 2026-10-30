const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Exercises the header preview with real Editor fields, including fields omitted by configuration. */
function fixture(t, hidden = []) {
    const dom = new JSDOM('<div id="datatableInit_modal"><a id="pills-basic-tab"></a><div id="pills-basic" class="tab-pane"></div></div>');
    t.after(() => dom.window.close());
    const { window } = dom;
    const $ = require('jquery')(window);
    $.expr.pseudos.visible = element => element.isConnected && element.style.display !== 'none';
    require('datatables.net')(window, $);
    require('datatables.net-editor')(window, $);
    const stored = {
        docId: 42, groupId: 67, title: 'Stored title', navbar: 'Stored menu', tempId: 1,
        virtualPath: '/stored/', editorVirtualPath: 'stored', generateUrlFromTitle: true,
        urlInheritGroup: false, searchable: true, followLinks: true, externalLink: '', cacheable: false,
        editorFields: { groupDetails: { groupId: 67 } }
    };
    const names = [...Object.keys(stored).filter(name => !['docId', 'groupId', 'editorFields'].includes(name)), 'editorFields.groupDetails'];
    const editor = new $.fn.dataTable.Editor({
        fields: names.filter(name => !hidden.includes(name)).map(name => ({ name,
            ...(name === 'editorFields.groupDetails' ? { getFormatter: JSON.parse } : {})
        }))
    });
    editor.currentJson = stored;
    for (const name of editor.fields()) {
        editor.field(name).val(name === 'editorFields.groupDetails' ? JSON.stringify(stored.editorFields.groupDetails) : stored[name]);
        $('#pills-basic').append(editor.field(name).node());
    }
    const requests = [];
    $.ajax = options => {
        requests.push(JSON.parse(options.data));
        return { done() { return this; }, fail() { return this; }, always() { return this; }, abort() {} };
    };
    window.$ = $;
    window.WJ = { translate: key => key, initTooltip() {}, urlAddPath: (url, suffix) => url + suffix };
    const context = vm.createContext({ window, $, WJ: window.WJ });
    const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/pages/web-pages-list/web-pages-datatable.js');
    // Expose only the entry point to this private behavior; execute its unchanged source.
    const source = fs.readFileSync(filename, 'utf8').replace(/^import .*$/gm, '')
        .replace('export class WebPagesDatatable {', 'class WebPagesDatatable { bindHeadersForTest() { this.#bindHttpHeaders(); }');
    const WebPagesDatatable = vm.runInContext(source + '\nWebPagesDatatable;', context, { filename });
    const instance = new WebPagesDatatable({});
    instance.webpagesDatatable = { DATA: { id: 'datatableInit', url: '/admin/rest/web-pages' }, EDITOR: editor };
    return { instance, editor, requests, $ };
}

test('Header preview retains stored values for configured-out fields and uses live visible fields', t => {
    const f = fixture(t, ['title', 'editorFields.groupDetails']);
    f.editor.field('navbar').val('Edited menu');
    f.instance.bindHeadersForTest();
    assert.equal(f.requests.length, 1);
    assert.equal(f.requests[0].title, 'Stored title');
    assert.equal(f.requests[0].navbar, 'Edited menu');
    assert.equal(f.requests[0].groupId, 67);
    assert.equal(f.requests[0].docId, 42);
});

test('Header preview is inserted even when its cacheable anchor is configured out', t => {
    const f = fixture(t, ['cacheable']);
    f.instance.bindHeadersForTest();
    assert.equal(f.$('#pills-basic .webpage-http-headers').length, 1);
    assert.equal(f.requests.length, 1);
});

test('Header preview is not created when the Basic tab is absent', t => {
    const f = fixture(t, ['cacheable']);
    f.$('#pills-basic').remove();
    f.instance.bindHeadersForTest();
    assert.equal(f.requests.length, 0);
});
