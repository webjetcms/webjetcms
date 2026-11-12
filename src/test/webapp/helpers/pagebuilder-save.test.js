const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

/** Loads the actual Page Builder serializer with its normal cleanup configuration. */
function fixture(t, html) {
    const dom = new JSDOM('<!doctype html><div id="page">' + html + '</div>', { runScripts: 'outside-only' });
    const window = dom.window;
    t.after(() => window.close());
    const $ = window.$ = window.jQuery = require('jquery')(window);
    window.CKEDITOR = { instances: {} };
    const file = path.resolve(__dirname, '../../../main/webapp/admin/webpages/page-builder/scripts/ninja-page-builder.js.jsp');
    window.eval(fs.readFileSync(file, 'utf8')
        .replace(/<%=[\s\S]*?%>/g, 'null')
        .replace('$.fn[ pluginName ] = function', 'window.PageBuilderPlugin = Plugin;\n    $.fn[ pluginName ] = function'));
    const builder = Object.create(window.PageBuilderPlugin.prototype);
    builder.options = builder.get_defaults();
    builder.options.grid = { section: 'section', container: '.container', row: '.row', column: '.col-12', column_content: '.column-content' };
    builder.set_settings();
    builder.set_settings_after();
    builder.$wrapper = $('#page');
    return { window, $, builder };
}

test('Page Builder saves malformed sections and stale editor markers without losing their HTML', t => {
    const html = '<section data-pb-id="heading"><div class="container">' +
        '<h1 class="display-3">Nastavenia podľa operačného systému</h1><p>Hlasové a&nbsp;dátové služby.</p></div></section>' +
        '<section><div class="container"><div class="row"><div class="col-12"><div class="column-content">' +
        '<div data-ckeditor-instance="editor5" role="textbox" spellcheck="true" tabindex="0" aria-readonly="false" aria-multiline="true">' +
        '<h3>Hľadám pomoc</h3><a href="/apple">iOS</a><img alt="Android" src="/android.webp"></div>' +
        '</div></div></div></div></section>';
    const { $, builder } = fixture(t, html);
    const original = builder.$wrapper.html();
    const saved = builder.getSaveNode();

    assert.equal(saved.find('h1').text(), 'Nastavenia podľa operačného systému');
    assert.equal(saved.find('p').text(), 'Hlasové a\u00a0dátové služby.');
    assert.equal(saved.find('h3').text(), 'Hľadám pomoc');
    assert.equal(saved.find('a').attr('href'), '/apple');
    assert.equal(saved.find('img').attr('src'), '/android.webp');
    assert.equal(saved.find('[data-pb-id="heading"] .row, [data-pb-id="heading"] .column-content').length, 0);
    assert.equal(saved.find('[data-ckeditor-instance], [contenteditable], [role="textbox"], [spellcheck], [tabindex], [aria-readonly], [aria-multiline]').length, 0);
    assert.equal(builder.$wrapper.html(), original, 'Saving must leave the live editor unchanged');
    assert.equal($('[data-ckeditor-instance="editor5"]').length, 1);
});

test('Page Builder preserves a section without rows or columns', t => {
    const html = '<section><div class="container"><h1>Heading</h1><p>Text</p></div></section>';
    const { builder } = fixture(t, html);
    assert.equal(builder.getSaveNode().html(), html);
});

test('A stale editor marker alone must not prevent serialization', t => {
    const { builder } = fixture(t, '<div data-ckeditor-instance="missing"><p>Text</p></div>');
    assert.equal(builder.getSaveNode().html(), '<div><p>Text</p></div>');
});

test('A stale editor name must not replace its content with another live editor', t => {
    const { window, $, builder } = fixture(t, '<div id="live"><p>Live</p></div>' +
        '<div id="stale" data-ckeditor-instance="editor1"><p>Original content</p></div>');
    window.CKEDITOR.instances.editor1 = {
        _: { pageBuilderOutputFilter: true },
        element: { $: $('#live')[0] },
        getData() { assert.fail('The editor belongs to a different element'); }
    };
    const saved = builder.getSaveNode();
    assert.equal(saved.find('#stale').html(), '<p>Original content</p>');
    assert.equal(saved.find('[data-ckeditor-instance]').length, 0);
});

for (const blockOnly of [false, true]) {
    test(`Page Builder exports current editor data and cleans stale nested markers (${blockOnly ? 'block' : 'page'})`, t => {
        const { window, $, builder } = fixture(t, '<div id="live" class="column-content editableElement" data-ckeditor-instance="editor1"><p>Old</p></div>');
        const host = $('#live')[0];
        window.CKEDITOR.instances.editor1 = {
            _: { pageBuilderOutputFilter: true },
            element: { $: host },
            getData: () => '<p>Edited</p><div class="lead cke_editable" data-ckeditor-instance="missing" role="textbox">Preserved</div>'
        };
        const saved = builder.getSaveNode(blockOnly ? host : undefined);
        assert.equal(saved.find('#live').html(), '<p>Edited</p><div class="lead">Preserved</div>');
        assert.equal($('#live').html(), '<p>Old</p>', 'Export must not change the live host');
        assert.equal($('#live').attr('data-ckeditor-instance'), 'editor1');
    });
}

test('An unexpected error in a live editor still stops the save', t => {
    const { window, $, builder } = fixture(t, '<div id="live" data-ckeditor-instance="editor1"><p>Original</p></div>');
    window.CKEDITOR.instances.editor1 = {
        _: { pageBuilderOutputFilter: true },
        element: { $: $('#live')[0] },
        getData() { throw new Error('Editor read failure'); }
    };
    assert.throws(() => builder.getSaveNode(), /Editor read failure/);
});
