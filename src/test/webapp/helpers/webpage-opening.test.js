const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Exercises the page opening listener and subsequent tab interaction with delayed callbacks. */
function fixture(t, docId = -1) {
    const dom = new JSDOM(`<div id="datatableInit_modal" class="show" data-action="create">
        <button id="pills-dt-datatableInit-content-tab" class="active">Content</button>
        <button id="pills-dt-datatableInit-basic-tab">Basic</button>
        <input id="DTE_Field_title" value="New page">
    </div>`);
    t.after(() => dom.window.close());
    const { window } = dom;
    const $ = require('jquery')(window);
    const timers = [];
    $('button').on('click', function() {
        $('button').removeClass('active');
        $(this).addClass('active');
        this.focus();
    });
    window.$ = $;
    const context = vm.createContext({ window, $, WJ: { translate: key => key },
        setTimeout: callback => timers.push(callback) });
    const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/pages/web-pages-list/web-pages-datatable.js');
    // Expose the real listener registration without initializing unrelated page services.
    const source = fs.readFileSync(filename, 'utf8').replace(/^import .*$/gm, '')
        .replace('export class WebPagesDatatable {', 'class WebPagesDatatable { bindOpenForTest() { this.#bindOpen(); }');
    const WebPagesDatatable = vm.runInContext(source + '\nWebPagesDatatable;', context, { filename });
    const instance = new WebPagesDatatable({});
    instance.webpagesDatatable = {
        DATA: { id: 'datatableInit' },
        EDITOR: { currentJson: { docId }, on() { return this; } }
    };
    instance.bindOpenForTest();
    return {
        $, window, instance,
        open() { window.dispatchEvent(new window.CustomEvent('WJ.DTE.opened', { detail: { id: 'datatableInit' } })); },
        flushTimers() { while (timers.length) timers.shift()(); }
    };
}

test('New page initialization does not override a subsequent selection of the Content tab', t => {
    const f = fixture(t);
    f.open();
    f.$('#pills-dt-datatableInit-content-tab').trigger('click');
    f.flushTimers();
    assert.equal(f.$('#pills-dt-datatableInit-content-tab').hasClass('active'), true);
    assert.equal(f.window.document.activeElement.id, 'pills-dt-datatableInit-content-tab');
});

test('A new page initially opens the Basic tab and focuses its title', t => {
    const f = fixture(t);
    f.open();
    f.flushTimers();
    assert.equal(f.$('#pills-dt-datatableInit-basic-tab').hasClass('active'), true);
    assert.equal(f.window.document.activeElement.id, 'DTE_Field_title');
});

test('Opening an existing page preserves the Content tab', t => {
    const f = fixture(t, 42);
    f.open();
    f.flushTimers();
    assert.equal(f.$('#pills-dt-datatableInit-content-tab').hasClass('active'), true);
    assert.notEqual(f.window.document.activeElement.id, 'DTE_Field_title');
});

test('A pending new page callback cannot change a subsequently opened existing page', t => {
    const f = fixture(t);
    f.open();
    f.instance.webpagesDatatable.EDITOR.currentJson = { docId: 42 };
    f.$('#pills-dt-datatableInit-content-tab').trigger('click');
    f.open();
    f.flushTimers();
    assert.equal(f.$('#pills-dt-datatableInit-content-tab').hasClass('active'), true);
    assert.equal(f.window.document.activeElement.id, 'pills-dt-datatableInit-content-tab');
});
