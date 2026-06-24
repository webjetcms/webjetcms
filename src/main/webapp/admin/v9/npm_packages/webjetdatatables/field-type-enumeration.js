/**
 * Custom field type for DataTables Editor - single row with 3 inputs (integer + select + select) joined by "|"
 * @returns field type definition
 */
export function typeEnumeration() {

    const selectGroups = { string: 12, decimal: 4, boolean: 4, date: 4 };

    const selectValues = [];
    for (const [prefix, max] of Object.entries(selectGroups)) {
        for (let i = 1; i <= max; i++) {
            selectValues.push(prefix + i);
        }
    }

    function buildOptions() {
        let html = '<option value=""></option>';
        for (let i = 0; i < selectValues.length; i++) {
            html += '<option value="' + selectValues[i] + '">' + selectValues[i] + '</option>';
        }
        return html;
    }

    return {
        create: function (conf) {
            const id = $.fn.dataTable.Editor.safeId(conf.id);
            conf._id = id;

            const options = buildOptions();
            conf._wrapper = $(`
                <div id="${id}" class="options-field-wrapper">
                    <div class="input-group mb-2 options-input-row-header">
                        <span class="options-id">${WJ.translate("datatables.id.js")}</span>
                        <span class="options-label">${WJ.translate("editor.form.sl.name")}</span>
                        <span class="options-value">${WJ.translate("editor.form.sl.value")}</span>
                    </div>
                    <div class="input-group options-inputs">
                        <input type="number" class="form-control options-value-1" value="" style="max-width: 80px;" />
                        <select class="form-select options-value-2">${options}</select>
                        <select class="form-select options-value-3">${options}</select>
                    </div>
                </div>
            `);

            return conf._wrapper[0];
        },

        get: function (conf) {
            const wrapper = conf && conf._wrapper;
            if (!wrapper || wrapper.length === 0) {
                return "";
            }

            const val1Raw = wrapper.find(".options-value-1").val();
            let val2 = wrapper.find(".options-value-2 .filter-option-inner-inner").text();
            let val3 = wrapper.find(".options-value-3 .filter-option-inner-inner").text();

            const val1 = val1Raw == null ? "" : String(val1Raw).trim();
            if (val1.length === 0) { return ""; }

            val2 = val2 == null ? "" : String(val2).trim();
            val3 = val3 == null ? "" : String(val3).trim();

            if (val2.length === 0) {
                const fallbackVal2 = wrapper.find(".options-value-2").val();
                val2 = fallbackVal2 == null ? "" : String(fallbackVal2).trim();
            }

            if (val3.length === 0) {
                const fallbackVal3 = wrapper.find(".options-value-3").val();
                val3 = fallbackVal3 == null ? "" : String(fallbackVal3).trim();
            }

            //console.log("val1", val1, " val2", val2, " val3", val3);

            // string1 is default column of enumeration that will be used
            if (val2.length === 0) {
                val2 = "string1";
            }
            if (val3.length === 0) {
                val3 = "string1";
            }

            return "enumeration-options" + "|" + val1 + "|" + val2 + "|" + val3;
        },

        set: function (conf, val) {
            if (val && val.length > 0) {
                if (val.startsWith("enumeration-options|")) {
                    val = val.substring("enumeration-options|".length);
                }
                const parts = val.split("|");
                conf._wrapper.find(".options-value-1").val(parts[0] || "");
                conf._wrapper.find(".options-value-2").val(parts[1] || "");
                conf._wrapper.find(".options-value-3").val(parts[2] || "");
            } else {
                conf._wrapper.find(".options-value-1").val("");
                conf._wrapper.find(".options-value-2").val("");
                conf._wrapper.find(".options-value-3").val("");
            }
        },

        enable: function (conf) {
            conf._wrapper.find("input, select").prop("disabled", false);
        },

        disable: function (conf) {
            conf._wrapper.find("input, select").prop("disabled", true);
        },

        canReturnSubmit: function (conf, node) {
            return false;
        }
    };
}
