import WJ from '../../src/js/webjet';
import {initTextareaLineNumbers} from '../../src/js/textarea-line-numbers';
import {validateJsonObject, formatJsonObject} from './jsoneditor-utils.mjs';

const editorFields = new WeakMap();

/**
 * Clears JSON controls from the previous record before custom fields are rebuilt.
 * @param {Object} editor DataTables Editor instance.
 */
export function resetJsonEditors(editor) {
    for (const entry of editorFields.get(editor) || []) {
        entry.field.error("");
        entry.field.s.opts.entityDecode = entry.entityDecode;
        $(entry.field.node()).off(".jsoneditor");
    }
    editorFields.set(editor, []);
}

/**
 * Enhances a custom field textarea and binds validation and lossless formatting.
 * @param {Object} editor DataTables Editor instance.
 * @param {string} name Editor field name.
 * @param {HTMLTextAreaElement} textarea Raw JSON input.
 * @param {boolean} required Whether the value is mandatory.
 */
export function initJsonEditor(editor, name, textarea, required) {
    const field = editor.field(name);
    const entityDecode = field.s.opts.entityDecode;
    field.s.opts.entityDecode = false;
    const lines = initTextareaLineNumbers(textarea);
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn btn-sm btn-outline-secondary md-jsoneditor-format";
    button.textContent = WJ.translate("settings.custom-fields.jsoneditor.format.js");
    button.disabled = textarea.disabled;
    textarea.parentElement.after(button);

    const error = $(field.node()).find('[data-dte-e="msg-error"]').first();
    const errorId = editor.TABLE.DATA.id + "_" + textarea.id + "_jsonerror";
    error.attr({id: errorId, "aria-live": "polite"});
    const describedBy = textarea.getAttribute("aria-describedby");
    textarea.setAttribute("aria-describedby", (describedBy ? describedBy + " " : "") + errorId);
    textarea.setAttribute("aria-required", String(required));

    const validate = () => {
        // Differing values in a bulk edit remain unchanged and are checked per record by the server.
        if (field.isMultiValue()) {
            field.error("");
            textarea.setAttribute("aria-invalid", "false");
            return true;
        }
        const result = validateJsonObject(textarea.value, required);
        let message = "";
        if (!result.valid) {
            message = result.error === "required"
                ? WJ.translate("datatables.field.required.error.js")
                : WJ.translate("settings.custom-fields.jsoneditor." + result.error + ".js");
            if (result.line != null) message += " " + WJ.translate("settings.custom-fields.jsoneditor.position.js", result.line, result.column);
        }
        field.error(WJ.escapeHtml(message));
        textarea.setAttribute("aria-invalid", String(!result.valid));
        return result.valid;
    };

    textarea.addEventListener("blur", validate);
    textarea.addEventListener("input", () => {
        if (textarea.getAttribute("aria-invalid") === "true") validate();
    });
    $(field.node()).on("change.jsoneditor", () => {
        lines.refresh();
        if (field.isMultiValue() || textarea.getAttribute("aria-invalid") === "true") validate();
    });
    button.addEventListener("click", () => {
        if (validate()) {
            textarea.value = formatJsonObject(textarea.value);
            textarea.dispatchEvent(new Event("input", {bubbles: true}));
            textarea.dispatchEvent(new Event("change", {bubbles: true}));
            lines.refresh();
        }
        textarea.focus();
    });
    editorFields.get(editor).push({field, textarea, validate, entityDecode});
}

/**
 * Validates every JSON control and reveals the first invalid field before submit.
 * @param {Object} editor DataTables Editor instance.
 * @returns {boolean} Whether all JSON fields are valid.
 */
export function validateJsonEditors(editor) {
    let firstInvalid;
    for (const entry of editorFields.get(editor) || []) {
        if (!entry.validate() && !firstInvalid) firstInvalid = entry.textarea;
    }
    if (!firstInvalid) return true;

    const panel = firstInvalid.closest('.dte-tab-pane');
    if (panel && !panel.classList.contains("active")) {
        const tab = document.getElementById(panel.getAttribute("aria-labelledby"));
        if (tab) $(tab).trigger("click");
    }
    firstInvalid.focus();
    return false;
}
