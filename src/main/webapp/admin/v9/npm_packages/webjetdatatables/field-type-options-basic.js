import WJ from "../../src/js/webjet";
import { createOptionsFieldType } from "./field-type-options-base";

/**
 * Custom field type for DataTables Editor - dynamic list of single string inputs joined by "|"
 * Unlike OPTIONS, this has only a single value per row (no key:value pair).
 * @returns field type definition
 */
export function typeOptionsBasic() {
    return createOptionsFieldType({
        prefix: "options-basic",

        createRowHtml: function (conf, value) {
            const row = $(`
                <div class="input-group mb-2 options-basic-input-row">
                    <span class="input-group-text options-basic-drag-handle" style="cursor: grab;"><i class="ti ti-caret-up-down"></i></span>
                    <input type="text" class="form-control options-basic-value" value="" />
                    <button class="btn btn-outline-danger options-basic-remove-btn" type="button" title="${WJ.translate("button.delete")}">
                        <i class="ti ti-trash"></i>
                    </button>
                </div>
            `);
            row.find(".options-basic-value").val(value || "");
            return row;
        },

        clearRow: function (row) {
            row.find(".options-basic-value").val("");
        },

        getRowValue: function (row) {
            const raw = row.find(".options-basic-value").val();
            const val = (raw == null ? "" : String(raw)).trim();
            return val.length > 0 ? val : null;
        },

        parseValue: function (part) {
            return [part || ""];
        },

        emptyRowArgs: function () {
            return [""];
        }
    });
}
