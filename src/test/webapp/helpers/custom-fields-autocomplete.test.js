const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Renders a custom autocomplete field with the real Editor and captures its autocomplete binding. */
function fixture(t, { row, editorId = 'id', contextId = null, autocompleteUrl, action = 'edit' }) {
    const dom = new JSDOM('<!doctype html><body><div id="autocompleteTest_modal"></div></body>');
    const $ = require('jquery')(dom.window);
    require('datatables.net')(dom.window, $);
    require('datatables.net-editor')(dom.window, $);
    const editor = new $.fn.dataTable.Editor({ fields: [{ name: 'fieldA', label: 'Suggestions' }] });
    editor.TABLE = { DATA: { id: 'autocompleteTest', editorId } };
    editor.currentJson = {
        ...row,
        tempId: 71,
        editorFields: {
            fieldsDefinitionClassName: 'sk.iway.iwcm.doc.GroupDetails',
            fieldsDefinitionKeyPrefix: 'groupedit',
            fieldsDefinitionEntityId: contextId,
            fieldsDefinitionAutocompleteUrl: autocompleteUrl,
            fieldsDefinition: [{ key: 'a', label: 'Suggestions', type: 'autocomplete', value: '' }]
        }
    };
    $('#autocompleteTest_modal').append(editor.field('fieldA').node());
    t.after(() => {
        editor.destroy();
        dom.window.close();
    });

    const bindings = [];
    class AutoCompleter {
        constructor(target) { this.target = target; }
        setName(name) { this.name = name; return this; }
        setUrl(url) { this.url = url; return this; }
        transform() { bindings.push(this); }
    }
    const context = vm.createContext({
        $, AutoCompleter, WJ: { translate: key => key }, resetJsonEditors() {},
        setTimeout: dom.window.setTimeout.bind(dom.window)
    });
    const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/custom-fields.js');
    const source = fs.readFileSync(filename, 'utf8')
        .replace(/^import .+;\r?$/gm, '')
        .replace(/^export \{.+\} from .+;\r?$/gm, '')
        .replace(/^export /gm, '');
    vm.runInContext(source, context, { filename });
    context.update(editor, action);

    assert.equal(bindings.length, 1, 'Rendering must bind the autocomplete field');
    assert.equal($(bindings[0].target).length, 1, 'Autocomplete must target the rendered input');
    return { binding: bindings[0], url: new URL(bindings[0].url, 'http://localhost') };
}

test('Folder autocomplete uses groupId when no explicit context ID is provided', t => {
    const { url } = fixture(t, { row: { groupId: 42 }, editorId: 'groupId' });
    assert.equal(url.pathname, '/admin/FCKeditor/_editor_autocomplete.jsp');
    assert.equal(url.searchParams.get('objectId'), '42');
    assert.equal(url.searchParams.get('className'), 'sk.iway.iwcm.doc.GroupDetails');
    assert.equal(url.searchParams.get('field'), 'A');
});

test('Autocomplete retains the standard entity ID fallback', t => {
    const { url } = fixture(t, { row: { id: 51 } });
    assert.equal(url.searchParams.get('objectId'), '51');
});

for (const contextId of [7, 0]) {
    test(`Explicit autocomplete context ${contextId} takes precedence over the edited record ID`, t => {
        const { binding, url } = fixture(t, {
            row: { id: 51 }, contextId,
            autocompleteUrl: '/admin/rest/enumeration/enumeration-data/autocomplete-field?source=type'
        });
        assert.equal(url.searchParams.get('objectId'), String(contextId));
        assert.equal(url.searchParams.get('source'), 'type');
        assert.equal(binding.name, 'term');
    });
}

test('A new folder sends the new-record ID instead of undefined', t => {
    const { url } = fixture(t, { row: { groupId: -1 }, editorId: 'groupId', action: 'create' });
    assert.equal(url.searchParams.get('objectId'), '-1');
});
