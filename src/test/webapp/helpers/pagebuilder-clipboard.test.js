const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

/** Loads the real paste helper and widget generators without starting the editor UI. */
function fixture(t, idAttribute) {
    const dom = new JSDOM(`<section><div class="container">
        <ul class="pb-autotabs"></ul><div class="tab-content">
            <div class="tab-pane" ${idAttribute} title="Clipboard tab autotest"><p>Tab content</p></div>
        </div><a class="placeholder" href="#">Placeholder</a></div></section>`, { runScripts: 'outside-only' });
    const window = dom.window;
    t.after(() => window.close());
    const $ = window.$ = window.jQuery = require('jquery')(window);
    window.WJ = { fixFileName: value => value.toLowerCase().replace(/ /g, '-') };
    const directory = path.resolve(__dirname, '../../../main/webapp/admin/webpages/page-builder/scripts');
    const source = fs.readFileSync(path.join(directory, 'ninja-page-builder.js.jsp'), 'utf8')
        .replace(/<%=[\s\S]*?%>/g, 'null')
        .replace('$.fn[ pluginName ] = function', 'window.PageBuilderPlugin = Plugin;\n    $.fn[ pluginName ] = function');
    window.eval(source);
    window.eval(fs.readFileSync(path.join(directory, 'pagesupport.js'), 'utf8'));
    const builder = Object.create(window.PageBuilderPlugin.prototype);
    let counter = 0;
    builder.user_style = { counter: () => 'autotest-' + ++counter };
    return { window, $, builder };
}

for (const idAttribute of ['', 'id=""']) {
    test(`Pasted tabs remain independent before widget initialization (${idAttribute || 'no ID attribute'})`, t => {
        const { window, $, builder } = fixture(t, idAttribute);
        const original = $('section');
        for (let copy = 0; copy < 2; copy++) {
            const pasted = original.clone();
            builder.prepare_workbench_paste_ids(pasted);
            $('body').append(pasted);
        }
        window.pbAutotabs();
        const panes = [...window.document.querySelectorAll('.tab-pane')];
        const ids = panes.map(pane => pane.id);
        assert.equal(new Set(ids).size, 3, 'Every pasted tab must own an independent ID');
        for (const section of window.document.querySelectorAll('section')) {
            assert.equal(section.querySelector('.pb-autotabs a').getAttribute('href'), '#' + section.querySelector('.tab-pane').id);
            assert.equal(section.querySelector('.placeholder').getAttribute('href'), '#', 'An empty original ID must not rewrite placeholder links');
        }
        window.pbAutotabs();
        assert.deepEqual(panes.map(pane => pane.id), ids, 'Periodic regeneration must preserve pasted IDs');
    });
}

test('Pasting initialized tabs keeps their title and remaps local links and ARIA references', t => {
    const { window, $, builder } = fixture(t, 'id="authored-tab"');
    window.pbAutotabs();
    const originalId = $('.tab-pane').attr('id');
    $('section').append(`<a class="local-link" href="#${originalId}" aria-controls="${originalId}">Open tab</a>`);
    const pasted = $('section').clone();
    builder.prepare_workbench_paste_ids(pasted);
    $('body').append(pasted);
    window.pbAutotabs();
    const pastedId = pasted.find('.tab-pane').attr('id');
    assert.notEqual(pastedId, originalId);
    assert.equal(pasted.find('.local-link').attr('href'), '#' + pastedId);
    assert.equal(pasted.find('.local-link').attr('aria-controls'), pastedId);
    assert.equal(pasted.find('.tab-pane').attr('title'), 'Clipboard tab autotest');
    assert.equal(pasted.find('.pb-autotabs a').attr('href'), '#' + pastedId);
    assert.equal($('section').first().find('.tab-pane').attr('id'), originalId);
});
