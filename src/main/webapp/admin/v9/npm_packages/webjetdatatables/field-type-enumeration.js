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
                <div id="${id}" class="input-group">
                    <input type="number" class="form-control options-value-1" value="" style="max-width: 80px;" />
                    <select class="form-select options-value-2">${options}</select>
                    <select class="form-select options-value-3">${options}</select>
                </div>
            `);

            return conf._wrapper[0];
        },

        get: function (conf) {
            const val1 = conf._wrapper.find(".options-value-1").val().trim();
            const val2 = conf._wrapper.find(".options-value-2 .filter-option-inner-inner").text().trim();
            const val3 = conf._wrapper.find(".options-value-3 .filter-option-inner-inner").text().trim();

            //console.log("val1", val1, " val2", val2, " val3", val3);

            if (val1.length === 0 || val2.length === 0 || val3.length === 0) {
                return "";
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
