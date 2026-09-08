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
            translate: (key, line, column) => key === "settings.custom-fields.jsoneditor.cursor.js" ? `Line ${line}, column ${column}` : key,
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

test("DataTable renders JSON containing an incomplete HTML tag as literal text", async t => {
    const dom = new JSDOM("<!doctype html><html><body><div id='result'></div></body></html>");
    const window = dom.window;
    const $ = adminRequire("jquery")(window);
    const context = vm.createContext({
        $,
        WJ: {
            escapeHtml: text => $("<div>").text(text).html(),
            htmlToText: text => text,
            translate: key => key
        },
        ...await utilities
    });
    loadBrowserModule(context, "npm_packages/webjetdatatables/datatables-config.js");
    t.after(() => window.close());

    const source = '{"entity":"&lt;","x":"<img src=x onerror=window.__jsonEditorXss=true//"}';
    const row = {
        col: 0,
        settings: {
            aoColumns: [{name: "fieldA", editor: {type: "text"}, className: "allow-html"}]
        }
    };
    const rowData = {
        editorFields: {
            fieldsDefinition: [{key: "a", type: "jsoneditor"}]
        }
    };
    const result = window.document.querySelector("#result");
    result.innerHTML = context.renderText(source, "display", rowData, row);

    assert.equal(result.textContent, source, "Rendering must preserve the displayed JSON source");
    assert.equal(result.querySelector("img"), null, "JSON text must not create executable HTML elements");
});

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

test("AI buttons remain in the JSON toolbar after repeated binding", async t => {
    const {window, editor, field, textarea, context} = await createFixture(t);
    textarea.classList.add("form-control");
    editor.TABLE.DATA.fields = [{name: "fieldA", type: "text", ai: [{}]}];
    context.WJ.hasPermission = () => true;
    loadBrowserModule(context, "npm_packages/webjetdatatables/editor-ai.js");
    let aiClicks = 0;
    context.editorAiFixture = {
        EDITOR: editor,
        aiUserInterface: {generateAssistentOptions: () => aiClicks++}
    };
    vm.runInContext("editorAiFixture._getEditorButton = EditorAi.prototype._getEditorButton", context);
    const wrapper = textarea.parentElement;
    textarea.focus();
    vm.runInContext("EditorAi.prototype.bindEditorButtons.call(editorAiFixture)", context);
    vm.runInContext("EditorAi.prototype.bindEditorButtons.call(editorAiFixture)", context);

    assert.equal(textarea.parentElement, wrapper, "AI must preserve the textarea and gutter layout");
    assert.equal(wrapper.parentElement.className, "md-jsoneditor-control");
    assert.equal(wrapper.querySelectorAll(".input-group").length, 0);
    assert.equal(field.node().querySelectorAll(".input-group").length, 0);
    assert.equal(field.node().querySelectorAll(".md-jsoneditor-toolbar .ti-sparkles").length, 1);
    assert.equal(field.node().querySelectorAll(".ti-sparkles").length, 1);
    assert.equal(window.document.activeElement, textarea, "AI wrapping must retain the active textarea focus");

    field.node().querySelector(".md-jsoneditor-toolbar .btn-ai").click();
    assert.equal(aiClicks, 1, "The toolbar AI action must keep its original click handler");
    textarea.value = '{\n  "value": true\n}';
    textarea.dispatchEvent(new window.Event("input", {bubbles: true}));
    assert.equal(wrapper.querySelector(".md-textarea-editor__lines").textContent, "1\n2\n3");
});

test("JSON toolbar tracks the active caret through selection, input and formatting", async t => {
    const {window, field, textarea} = await createFixture(t);
    const toolbar = field.node().querySelector(".md-jsoneditor-toolbar");
    const position = toolbar.querySelector(".md-jsoneditor-position");
    const button = toolbar.querySelector(".md-jsoneditor-format");
    assert.equal(toolbar.nextElementSibling, textarea.parentElement, "The toolbar must precede the textarea editor");
    assert.equal(position.hidden, true, "Coordinates must be hidden before focus");
    field.val("{\n  title: 'test'\n}");
    textarea.focus();
    assert.equal(position.hidden, false, "Focus must reveal coordinates");
    textarea.setSelectionRange(5, 5);
    textarea.dispatchEvent(new window.Event("keyup"));
    assert.equal(position.textContent, "Line 2, column 4");

    textarea.setSelectionRange(0, 5, "backward");
    textarea.dispatchEvent(new window.Event("select"));
    assert.equal(position.textContent, "Line 1, column 1", "Backward selection must report its active end");
    textarea.setSelectionRange(0, 5, "forward");
    textarea.dispatchEvent(new window.Event("select"));
    assert.equal(position.textContent, "Line 2, column 4");

    textarea.value = "{}";
    textarea.dispatchEvent(new window.Event("input"));
    assert.equal(position.textContent, "Line 1, column 3");
    button.focus();
    assert.equal(position.hidden, true, "Toolbar focus must hide textarea coordinates");
    field.val("{title:'test'}");
    assert.equal(position.hidden, true, "A programmatic update must not reveal inactive coordinates");
    button.click();
    assert.equal(position.hidden, false, "Formatting must restore focus and coordinates");
    assert.equal(textarea.value, "{\n  title: 'test'\n}");
    assert.equal(position.textContent, "Line 3, column 2");
    assert.equal(window.document.activeElement, textarea);
});
