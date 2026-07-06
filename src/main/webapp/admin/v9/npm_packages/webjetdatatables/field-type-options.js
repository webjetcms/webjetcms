import WJ from "../../src/js/webjet";
import { createOptionsFieldType } from "./field-type-options-base";

/**
 * Custom field type for DataTables Editor - dynamic list of key:value inputs joined by "|"
 * @returns field type definition
 */
export function typeOptions() {
    return createOptionsFieldType({
        prefix: "options",

        createHeader: function () {
            return `<div class="input-group mb-2 options-input-row-header">
                        <span class="options-label">${WJ.translate("editor.form.sl.name")}</span>
                        <span class="options-value">${WJ.translate("editor.form.sl.value")}</span>
                    </div>`;
        },

        createRowHtml: function (conf, value1, value2) {
            const row = $(`
                <div class="input-group mb-2 options-input-row">
                    <span class="input-group-text options-drag-handle" style="cursor: grab;"><i class="ti ti-caret-up-down"></i></span>
                    <input type="text" class="form-control options-value-1" value="" />
                    <input type="text" class="form-control options-value-2" value="" />
                    <button class="btn btn-outline-danger options-remove-btn" type="button" title="${WJ.translate("button.delete")}">
                        <i class="ti ti-trash"></i>
                    </button>
                </div>
            `);
            row.find(".options-value-1").val(value1 || "");
            row.find(".options-value-2").val(value2 || "");
            return row;
        },

        clearRow: function (row) {
            row.find(".options-value-1").val("");
            row.find(".options-value-2").val("");
        },

        getRowValue: function (row) {
            const raw1 = row.find(".options-value-1").val();
            const raw2 = row.find(".options-value-2").val();

            const val1 = (raw1 == null ? "" : String(raw1)).trim();
            let val2 = (raw2 == null ? "" : String(raw2)).trim();

            if (val1.length > 0 || val2.length > 0) {
                if (val1.length === 0) {
                    return val2 + ":" + val2;
                } else {
                    if (val2.length === 0) val2 = val1;
                    return val1 + ":" + val2;
                }
            }
            return null;
        },

        parseValue: function (part) {
            const pair = part.split(":");
            if (pair.length === 1) pair.push(pair[0]);
            return [pair[0] || "", pair[1] || ""];
        },

        emptyRowArgs: function () {
            return ["", ""];
        }
    });
}
