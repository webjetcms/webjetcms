const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');
const jquery = require('jquery');

/** Loads the production options field with its real inputs and serialization methods. */
function fixture(t, className = 'allowEmptyOption') {
    const dom = new JSDOM('<!doctype html><body></body>');
    t.after(() => dom.window.close());
    const $ = jquery(dom.window);
    $.fn.dataTable = { Editor: { safeId: id => id } };
    const context = vm.createContext({ $, WJ: { translate: key => key } });
    for (const filename of ['field-type-options-base.js', 'field-type-options.js']) {
        const source = fs.readFileSync(path.resolve(__dirname,
            '../../../main/webapp/admin/v9/npm_packages/webjetdatatables', filename), 'utf8')
            .replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');
        vm.runInContext(source, context);
    }
    const field = context.typeOptions();
    const conf = { id: 'autotest-options', className };
    const wrapper = $(field.create(conf));
    return { field, conf, wrapper };
}

for (const [label, value] of [['I agree', 'yes'], ['Sales, Europe', 'sales'], ['Consent', 'yes please'], ['Region', 'Europe, West']]) {
    test(`Removing the empty option preserves a single choice across saves: ${label} / ${value}`, t => {
        const { field, conf, wrapper } = fixture(t);
        const option = `${label}:${value}`;
        field.set(conf, `:|${option}`);
        wrapper.find('.options-empty-option-btn').prop('checked', false).trigger('change');

        for (let save = 0; save < 2; save++) {
            const serialized = field.get(conf);
            assert.equal(serialized, `${option}|`);
            field.set(conf, serialized);
            assert.equal(wrapper.find('.options-input-row').length, 1);
            assert.equal(wrapper.find('.options-value-1').val(), label);
            assert.equal(wrapper.find('.options-value-2').val(), value);
            assert.equal(wrapper.find('.options-empty-option-btn').prop('checked'), false);
        }

        // The server retains the separator when it removes a stored empty-option sentinel.
        field.set(conf, `|${option}`);
        assert.equal(wrapper.find('.options-input-row').length, 1);
        assert.equal(field.get(conf), `${option}|`);
        wrapper.find('.options-empty-option-btn').prop('checked', true).trigger('change');
        assert.equal(field.get(conf), `:|${option}`);
    });
}

test('Multiple choices and legacy comma-separated choices preserve order and empty-option state', t => {
    const { field, conf, wrapper } = fixture(t);
    for (const options of [':|First:one|Second:two', ':,First:one,Second:two']) {
        field.set(conf, options);
        assert.equal(wrapper.find('.options-input-row').length, 2);
        assert.equal(wrapper.find('.options-empty-option-btn').prop('checked'), true);
        assert.equal(field.get(conf), ':|First:one|Second:two');
    }
});

test('Empty fields retain one editable row without inventing a choice', t => {
    const { field, conf, wrapper } = fixture(t);
    for (const options of ['', '|']) {
        field.set(conf, options);
        assert.equal(wrapper.find('.options-input-row').length, 1);
        assert.equal(wrapper.find('.options-empty-option-btn').prop('checked'), false);
        assert.equal(field.get(conf), '');
    }
    field.set(conf, ':');
    field.set(conf, field.get(conf));
    assert.equal(wrapper.find('.options-input-row').length, 1);
    assert.equal(wrapper.find('.options-empty-option-btn').prop('checked'), true);
    assert.equal(field.get(conf), ':|');
});

test('Options fields without allowEmptyOption keep their existing serialization', t => {
    const { field, conf, wrapper } = fixture(t, '');
    field.set(conf, 'First:one');
    assert.equal(wrapper.find('.options-empty-option-btn').length, 0);
    assert.equal(field.get(conf), 'First:one');
});
