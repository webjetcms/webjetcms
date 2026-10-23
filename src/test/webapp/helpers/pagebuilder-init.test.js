const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM, VirtualConsole } = require('jsdom');

const webapp = path.resolve(__dirname, '../../../main/webapp');
const read = file => fs.readFileSync(path.join(webapp, file), 'utf8');

/** Runs the real CKEditor and Page Builder bootstrap in their browser script order. */
async function fixture(t, html) {
    const errors = [];
    const virtualConsole = new VirtualConsole();
    virtualConsole.on('jsdomError', error => errors.push(error));
    const dom = new JSDOM('<!doctype html><html><body><div id="wjInline-docdata">' + html + '</div></body></html>', {
        runScripts: 'outside-only', url: 'http://localhost/', virtualConsole
    });
    const window = dom.window;
    t.after(() => window.close());
    Object.defineProperty(window.navigator, 'userAgent', {
        value: 'Mozilla/5.0 AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36'
    });
    const $ = window.$ = window.jQuery = require('jquery')(window);
    window.WJ = { translate: key => key, dispatchEvent() {} };
    window.userLng = 'sk';
    window.ckeditorRegexps = {};
    // Grid discovery is independent of editor creation; keep only its editable-region markers.
    $.fn.ninjaPageBuilder = function() {
        this.find('.column-content').addClass('pb-content');
        this.data('plugin_ninjaPageBuilder', { prepare_application_blocks() {} });
        return this;
    };
    window.eval(read('admin/v9/src/js/datatables-ckeditor.js')
        .replace('import WJ from "./webjet";', '')
        .replace('export class DatatablesCkEditor', 'window.DatatablesCkEditor = class DatatablesCkEditor'));
    window.datatablesCkEditorModule = { DatatablesCkEditor: window.DatatablesCkEditor };

    const toolbar = read('admin/inline/inline_page_toolbar_v9.jsp')
        .replace(/<%--[\s\S]*?--%>/g, '')
        .replaceAll('<%=pbPrefix%>', 'pb')
        .replaceAll('<%=pageBuilderGrid%>', '')
        .replace(/<%=Constants.getInt\("bootstrapColumns", 12\)%>/g, '12')
        .replace(/<%=JsonTools.objectToJSON[\s\S]*?%>/g, '[]')
        .replace(/<%[\s\S]*?%>/g, '')
        .replace('${ninja.temp.group.templatesGroupBean.templatesGroupId}', '0');
    const ckeErrors = [];
    for (const match of toolbar.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>|<iwcm:combine type="js" set="adminInlineJs"\/>/g)) {
        if (match[1] !== undefined) window.eval(match[1]);
        else {
            window.eval(read('admin/skins/webjet8/ckeditor/dist/ckeditor.js'));
            window.CKEDITOR.on('log', event => ckeErrors.push(event.data.errorCode));
        }
    }
    // jQuery ready follows CKEditor's native DOM-ready auto-inline handler.
    await new Promise(resolve => $(window.document).ready(resolve));
    // CKEditor applies the per-instance configuration in the next timer turn.
    await new Promise(resolve => window.setTimeout(resolve, 0));
    return { window, $, errors, ckeErrors };
}

for (const persistedAttributes of ['', ' contenteditable="true" role="textbox" tabindex="0"']) {
    test(`Page Builder creates one configured editor per region${persistedAttributes ? ' with persisted editor attributes' : ''}`, async t => {
        const f = await fixture(t, '<div class="column-content"><p>First</p></div>' +
            '<div class="column-content"' + persistedAttributes + '><p>Second <span contenteditable="false">Protected</span></p></div>');
        assert.deepEqual(f.ckeErrors, [], 'Automatic inline creation must not conflict with Page Builder');
        assert.deepEqual(f.errors, []);
        const elements = f.$('.column-content').toArray();
        const instances = Object.values(f.window.CKEDITOR.instances);
        assert.equal(instances.length, elements.length);
        for (const element of elements) {
            const editor = f.window.CKEDITOR.instances[element.getAttribute('data-ckeditor-instance')];
            assert.ok(editor, 'Every region must belong to a Page Builder editor');
            assert.equal(editor.element.$, element);
            assert.equal(editor.config.customConfig, '/admin/skins/webjet8/ckeditor/config.jsp?inline=true&inlineMode=pageBuilder');
        }
        assert.equal(f.$('span').attr('contenteditable'), 'false', 'Intentional noneditable content must remain protected');
    });
}

test('A failed CKEditor creation stops before scheduling instance callbacks', async t => {
    const f = await fixture(t, '');
    const element = f.window.document.createElement('div');
    element.id = 'failed-editor';
    f.window.document.body.appendChild(element);
    const scheduled = [];
    f.window.setTimeout = callback => scheduled.push(callback);
    assert.throws(() => new f.window.DatatablesCkEditor({
        window: f.window, ckEditorElementId: element, ckEditorInitFunction: () => null
    }), /CKEditor initialization failed/);
    assert.equal(scheduled.length, 0, 'A missing instance must not reach the asynchronous afterInit callback');
});
