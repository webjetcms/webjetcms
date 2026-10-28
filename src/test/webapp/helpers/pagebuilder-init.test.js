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
    window.eval(read('admin/v9/src/js/ckeditor-toolbar-overflow.js')
        .replace('export class CkEditorToolbarOverflow', 'window.CkEditorToolbarOverflow = class CkEditorToolbarOverflow'));
    window.eval(read('admin/v9/src/js/datatables-ckeditor.js')
        .replace('import WJ from "./webjet";', '')
        .replace('import { CkEditorToolbarOverflow } from "./ckeditor-toolbar-overflow";', '')
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

test('Page Builder ignores unsupported inline hosts without losing their content', async t => {
    const f = await fixture(t, '');
    const page = f.$('#wjInline-docdata');
    page.html('<div class="pb-editable"><h2>Mobile network</h2>' +
        '<ul class="pb-editable unsupported"><li class="pb-editable unsupported">First item</li>' +
        '<li class="pb-content unsupported">Second item</li></ul>' +
        '<table class="pb-editable unsupported"><tbody><tr><td class="pb-editable unsupported">Cell</td></tr></tbody></table></div>' +
        '<p class="pb-editable">Following paragraph</p>' +
        '<span class="pb-editable">Supported span</span><a class="pb-editable" href="#">Supported link</a>');
    const unsupported = page.find('.unsupported').toArray();
    const originalText = unsupported.map(element => element.textContent);

    assert.doesNotThrow(() => f.window.initPageBuilderEditors(page));
    assert.equal(Object.keys(f.window.CKEDITOR.instances).length, 4, 'Supported hosts after the invalid markup must still initialize');
    assert.deepEqual(unsupported.map(element => element.textContent), originalText, 'Skipped hosts must retain their content');
    for (const element of unsupported) {
        assert.equal(element.className, 'unsupported', 'Only redundant editor marker classes may be removed');
        assert.equal(element.hasAttribute('contenteditable'), false);
        assert.equal(element.hasAttribute('data-ckeditor-instance'), false);
    }
    assert.equal(page.find('[data-ckeditor-instance]').length, 4);
    assert.ok(page.find('li').first().closest('[data-ckeditor-instance]').is('div'), 'List items remain inside their parent editor');

    assert.doesNotThrow(() => f.window.initPageBuilderEditors(page));
    assert.equal(Object.keys(f.window.CKEDITOR.instances).length, 4, 'Repeated grid initialization must not create duplicate editors');
});

test('Page Builder removes nested editor markers before the parent captures its HTML', async t => {
    const f = await fixture(t, '');
    const page = f.$('#wjInline-docdata');
    page.html('<div id="parent" class="pb-editable"><h2>Mobile network</h2>' +
        '<p id="nested" class="lead pb-editable">Coverage</p>' +
        '<div class="pb-content"><p class="pb-editable">Nested content</p></div></div>' +
        '<p id="standalone" class="pb-editable">Standalone paragraph</p>' +
        '<ul class="pb-editable"><li><p id="inside-unsupported" class="pb-editable">Independent paragraph</p></li></ul>');

    f.window.initPageBuilderEditors(page);
    assert.equal(Object.keys(f.window.CKEDITOR.instances).length, 3, 'Only independent supported regions may create editors');
    assert.equal(page.find('[data-ckeditor-instance] [data-ckeditor-instance]').length, 0);
    assert.equal(page.find('#parent .pb-editable, #parent .pb-content').length, 0, 'Inner content must not carry editor markers into Enter or list commands');
    assert.equal(page.find('#nested').attr('class'), 'lead', 'Formatting classes must remain intact');
    assert.equal(page.find('#standalone.pb-editable, #inside-unsupported.pb-editable').length, 2, 'Independent supported regions must remain editable');
    const parent = f.window.CKEDITOR.instances[page.find('#parent').attr('data-ckeditor-instance')];
    assert.doesNotMatch(parent.getData(), /pb-editable|pb-content/, 'The initial CKEditor data must already be free of nested markers');

    page.find('#parent').append('<p class="pb-editable lead">Inserted content</p>');
    f.window.initPageBuilderEditors(page);
    assert.equal(Object.keys(f.window.CKEDITOR.instances).length, 3, 'Grid updates must not create an editor inside an existing editor');
    assert.equal(page.find('#parent .pb-editable').length, 0);
    assert.equal(page.find('#parent p.lead').length, 2);
});

test('Page Builder resolves readiness when all marked hosts are unsupported', async t => {
    const f = await fixture(t, '');
    const page = f.$('#wjInline-docdata');
    const html = '<ul><li class="pb-editable">Standalone item</li></ul>';
    page.html(html);

    await f.window.initPageBuilderEditors(page);
    assert.equal(Object.keys(f.window.CKEDITOR.instances).length, 0);
    assert.equal(page.html(), html);
});

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
