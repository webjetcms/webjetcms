const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const {createRequire} = require("node:module");

const adminDirectory = path.resolve(__dirname, "../../../main/webapp/admin/v9");
const adminRequire = createRequire(path.join(adminDirectory, "package.json"));
const {JSDOM} = adminRequire("jsdom");
const utilities = import("../../../main/webapp/admin/v9/npm_packages/webjetdatatables/jsoneditor-utils.mjs");

/**
 * Loads the browser module against the fixture's DOM and explicit imports.
 * Only module declarations are removed; the production implementation is unchanged.
 */
function loadBrowserModule(context, relativePath) {
    const filename = path.join(adminDirectory, relativePath);
    const source = fs.readFileSync(filename, "utf8")
        .replace(/^import .+;\r?$/gm, "")
        .replace(/^export /gm, "");
    vm.runInContext(source, context, {filename});
}

/**
 * Creates a real DataTables Editor field with the production JSON enhancements.
 */
async function createFixture(t, {entityDecode, required = false} = {}) {
    const dom = new JSDOM("<!doctype html><html><body></body></html>");
    const window = dom.window;
    const $ = adminRequire("jquery")(window);
    adminRequire("datatables.net")(window, $);
    adminRequire("datatables.net-editor")(window, $);
    const editor = new $.fn.dataTable.Editor({fields: [{name: "fieldA", entityDecode}]});
    editor.TABLE = {DATA: {id: "jsoneditorTest"}};
    const field = editor.field("fieldA");
    const originalEntityDecode = field.s.opts.entityDecode;
    window.document.body.append(field.node());
    const textarea = window.document.createElement("textarea");
    textarea.id = "DTE_Field_fieldA";
    field.s.opts._input.replaceWith(textarea);
    field.s.opts._input = $(textarea);

    const context = vm.createContext({
        document: window.document,
        Event: window.Event,
        $,
        WJ: {
            translate: key => key,
            escapeHtml: text => $("<div>").text(text).html()
        },
        ...await utilities
    });
    loadBrowserModule(context, "src/js/textarea-line-numbers.js");
    loadBrowserModule(context, "npm_packages/webjetdatatables/jsoneditor.js");
    context.resetJsonEditors(editor);
    context.initJsonEditor(editor, "fieldA", textarea, required);
    t.after(() => {
        editor.destroy();
        window.close();
    });
    return {window, editor, field, textarea, context, originalEntityDecode};
}

test("JSON field preserves literal HTML entities through the Editor API and restores decoding", async t => {
    for (const entityDecode of [undefined, true, false]) {
        const {field, textarea, editor, context, originalEntityDecode} = await createFixture(t, {entityDecode});
        const source = '{"html":"</textarea>&quot; &amp; &#10;","id":9007199254740993}';
        field.val(source);
        assert.equal(field.val(), source, "Editor setters must preserve JSON source text");
        assert.equal(textarea.value, source);
        assert.equal(context.validateJsonEditors(editor), true);
        assert.equal(field.s.opts.entityDecode, false);

        context.resetJsonEditors(editor);
        assert.equal(field.s.opts.entityDecode, originalEntityDecode, "Changing the custom field type must restore its decoding option");
        field.val("&quot;");
        assert.equal(field.val(), entityDecode === false ? "&quot;" : '"');
    }
});

test("Restoring distinct bulk values clears invalid hidden JSON input without changing submitted originals", async t => {
    const {window, field, textarea, editor, context} = await createFixture(t, {required: true});
    const originals = {"1": '{"value":1}', "2": '{"value":2}'};
    field.multiSet(originals);
    field.val("{");
    textarea.dispatchEvent(new window.Event("blur"));
    assert.equal(field.inError(), true);
    assert.equal(context.validateJsonEditors(editor), false);

    field.multiRestore();
    assert.equal(field.isMultiValue(), true);
    assert.equal(textarea.value, "{", "Editor retains the discarded common input internally");
    assert.equal(field.dom.inputControl.css("display"), "none");
    assert.deepEqual(field.multiGet(), originals);
    assert.equal(field.inError(), false, "Restoring individual values must clear the discarded input error");
    assert.equal(textarea.getAttribute("aria-invalid"), "false");
    assert.equal(context.validateJsonEditors(editor), true);
    assert.deepEqual(field.multiGet(), originals);
});

test("Line numbers refresh after Editor setters and repeated enhancement without adding another gutter", async t => {
    const {window, field, textarea, context} = await createFixture(t);
    field.val('{\n  "items": []\n}');
    await new Promise(resolve => setTimeout(resolve, 0));
    const gutter = textarea.parentElement.querySelector(".md-textarea-editor__lines");
    assert.equal(gutter.textContent, "1\n2\n3", "Editor change events must refresh the gutter");

    const firstHandle = context.initTextareaLineNumbers(textarea);
    textarea.value = "{}";
    const secondHandle = context.initTextareaLineNumbers(textarea);
    assert.equal(secondHandle, firstHandle);
    assert.equal(gutter.textContent, "1");
    assert.equal(field.node().querySelectorAll(".md-textarea-editor").length, 1);
    assert.equal(field.node().querySelectorAll(".md-textarea-editor__lines").length, 1);

    textarea.value = "{\n}";
    textarea.dispatchEvent(new window.Event("input", {bubbles: true}));
    assert.equal(gutter.textContent, "1\n2");
    textarea.scrollTop = 35;
    textarea.dispatchEvent(new window.Event("scroll"));
    assert.equal(gutter.scrollTop, textarea.scrollTop);
});

test("AI buttons preserve the complete line-number editor after repeated binding", async t => {
    const {window, editor, field, textarea, context} = await createFixture(t);
    textarea.classList.add("form-control");
    editor.TABLE.DATA.fields = [{name: "fieldA", type: "text", ai: [{}]}];
    context.WJ.hasPermission = () => true;
    loadBrowserModule(context, "npm_packages/webjetdatatables/editor-ai.js");
    context.editorAiFixture = {
        EDITOR: editor,
        _getEditorButton: () => context.$('<button type="button"><i class="ti-sparkles"></i></button>')
    };
    const wrapper = textarea.parentElement;
    textarea.focus();
    vm.runInContext("EditorAi.prototype.bindEditorButtons.call(editorAiFixture)", context);
    vm.runInContext("EditorAi.prototype.bindEditorButtons.call(editorAiFixture)", context);

    assert.equal(textarea.parentElement, wrapper, "AI must wrap the whole editor so textarea and gutter keep matching layout");
    assert.equal(wrapper.parentElement.className, "input-group");
    assert.equal(wrapper.querySelectorAll(".input-group").length, 0);
    assert.equal(field.node().querySelectorAll(".input-group").length, 1);
    assert.equal(field.node().querySelectorAll(".ti-sparkles").length, 1);
    assert.equal(window.document.activeElement, textarea, "AI wrapping must retain the active textarea focus");

    textarea.value = '{\n  "value": true\n}';
    textarea.dispatchEvent(new window.Event("input", {bubbles: true}));
    assert.equal(wrapper.querySelector(".md-textarea-editor__lines").textContent, "1\n2\n3");
});
