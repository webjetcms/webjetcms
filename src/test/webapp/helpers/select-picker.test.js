const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');
const jquery = require('jquery');

/** Exercises the shared initialization contract without loading Bootstrap's layout-dependent plugin. */
function fixture(t) {
    const dom = new JSDOM('<body><select id="page"><option>One</option></select><select class="no-picker"></select><div class="modal"><select class="form-select" id="dynamic" data-live-search="false"><option>Two</option></select></div>');
    const { window } = dom;
    const $ = window.jQuery = jquery(window);
    const calls = [];
    $.fn.selectpicker = function (options) {
        this.each(function () {
            calls.push({ element: this, options, disabled: this.disabled, count: this.options.length });
            if (typeof options === 'object') $(this).data('selectpicker', { options });
        });
        return this;
    };
    $.fn.selectpicker.Constructor = {};
    const source = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/select-picker.js'), 'utf8').replace(/^export /gm, '');
    const scope = vm.createContext({ window, document: window.document });
    vm.runInContext(source, scope);
    t.after(() => window.close());
    return { window, $, calls, init: scope.initSelectPicker };
}

test('Standard select initialization preserves page defaults, opt-outs and modal search focus', t => {
    const { window, $, calls, init } = fixture(t);
    init();
    assert.equal(calls.length, 2);
    assert.equal($.fn.selectpicker.Constructor.BootstrapVersion, '5');
    assert.equal(calls[0].options.container, 'body');
    assert.equal(calls[0].options.style, 'dropdown bootstrap-select btn-outline-secondary');
    assert.equal(calls[0].options.liveSearch, true);
    assert.equal(calls[1].options.container, window.document.querySelector('.modal'));
    assert.equal(calls[1].options.liveSearch, false);
    assert.ok(calls[1].element.classList.contains('form-control'), 'Bootstrap-select must not inherit a second native select border');
});

test('Dynamic option updates refresh existing pickers instead of duplicating their wrappers', t => {
    const { window, calls, init } = fixture(t);
    const select = window.document.querySelector('#dynamic');
    init(select, { width: '100%' });
    assert.equal(calls[0].options.width, '100%');
    select.add(new window.Option('Three', 'three'));
    select.disabled = true;
    init(select);
    assert.equal(calls.length, 2);
    assert.equal(calls[1].options, 'refresh');
    assert.equal(calls[1].count, 2);
    assert.equal(calls[1].disabled, true);
});
