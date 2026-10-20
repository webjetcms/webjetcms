let nextGroupId = 0;

/**
 * Creates a single-choice Bootstrap button group using the Editor radio value contract.
 * Options use the same label/value pairs as RADIO, including a "null" option for nullable fields.
 * @returns {Object} DataTables Editor field type definition.
 */
export function typeButtonGroup() {
    const Editor = $.fn.dataTable.Editor;
    const radio = Editor.fieldTypes.radio;
    const buttonGroup = $.extend({}, radio, {
        _addOptions: function (conf, options, append = false) {
            if (!append) conf._input.empty();
            const offset = conf._input.find("input").length;
            Editor.pairs(options || [], conf.optionsPair, function (value, label, index, attr) {
                const id = conf._buttonGroupId + "_" + (offset + index);
                const input = $("<input>")
                    .attr(conf.attr || {})
                    .attr(attr || {})
                    .attr({ type: "radio", id, name: conf._buttonGroupId, autocomplete: "off" })
                    .addClass("btn-check")
                    .val(value);
                input[0]._editor_val = value;
                if (conf._buttonGroupDisabled) input.prop("disabled", true);
                conf._input.append(input, $("<label>")
                    .attr("for", id)
                    .addClass("btn btn-outline-secondary")
                    .text(label));
            });
        },

        create: function (conf) {
            conf._buttonGroupId = Editor.safeId(conf.id) + "_buttonGroup_" + nextGroupId++;
            conf._input = $("<div>")
                .addClass("btn-group dt-button-group")
                .attr({ id: Editor.safeId(conf.id), role: "radiogroup", "aria-label": conf.label || conf.name });
            buttonGroup._addOptions(conf, conf.options || conf.ipOpts);
            return conf._input[0];
        },

        set: function (conf, value) {
            if (value === null && conf._input.find("input").toArray().some(input => input._editor_val === "null")) {
                value = "null";
            }
            radio.set.call(this, conf, value);
        },

        update: function (conf, options, append) {
            const value = buttonGroup.get(conf);
            buttonGroup._addOptions(conf, options, append);
            const inputs = conf._input.find("input").toArray();
            const current = inputs.find(input => input._editor_val === value);
            buttonGroup.set.call(this, conf, current ? value : inputs[0]?._editor_val);
        },

        disable: function (conf) {
            conf._buttonGroupDisabled = true;
            radio.disable.call(this, conf);
        },

        enable: function (conf) {
            conf._buttonGroupDisabled = false;
            radio.enable.call(this, conf);
        }
    });
    return buttonGroup;
}
