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
        /**
         * Builds radio inputs with text-only button labels, preserving the group's disabled state.
         * Field attributes are applied first, followed by option attributes and the generated radio identity.
         *
         * @param {Object} conf - Editor field configuration initialized by create.
         * @param {*[]|Object<string, *>|null} [options] - Editor option entries or a label-to-value map.
         * @param {boolean} [append=false] - Appends options when true; otherwise replaces existing inputs.
         */
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

        /**
         * Creates a radiogroup with a unique input name and stores its DOM state on the configuration.
         *
         * @param {Object} conf - Editor field configuration, mutated to store the group ID and input wrapper.
         * @param {string} conf.id - Field DOM ID passed through Editor.safeId.
         * @param {string} conf.name - Field name used as the accessible label when no label is supplied.
         * @param {string} [conf.label] - Accessible group label.
         * @param {*[]|Object<string, *>|null} [conf.options] - Initial Editor options.
         * @param {*[]|Object<string, *>|null} [conf.ipOpts] - Fallback options when conf.options is falsy.
         * @param {Object} [conf.optionsPair] - Overrides the default label and value property names of option objects.
         * @param {Object} [conf.attr] - Attributes applied to each radio input before option attributes.
         * @returns {HTMLDivElement} The button-group container.
         */
        create: function (conf) {
            conf._buttonGroupId = Editor.safeId(conf.id) + "_buttonGroup_" + nextGroupId++;
            conf._input = $("<div>")
                .addClass("btn-group dt-button-group")
                .attr({ id: Editor.safeId(conf.id), role: "radiogroup", "aria-label": conf.label || conf.name });
            buttonGroup._addOptions(conf, conf.options || conf.ipOpts);
            return conf._input[0];
        },

        /**
         * Selects a value using Editor's radio comparison and change-event behavior.
         * A null value selects the literal "null" option when that option exists.
         *
         * @param {Object} conf - Editor field configuration containing the initialized input wrapper.
         * @param {*} value - Value to match against the stored option values.
         */
        set: function (conf, value) {
            if (value === null && conf._input.find("input").toArray().some(input => input._editor_val === "null")) {
                value = "null";
            }
            radio.set.call(this, conf, value);
        },

        /**
         * Refreshes options and retains the selection when its stored or string value still matches.
         * Otherwise selects the first option, leaving no selection when the resulting group is empty.
         *
         * @param {Object} conf - Editor field configuration containing the initialized input wrapper.
         * @param {*[]|Object<string, *>|null} [options] - Editor option entries or a label-to-value map.
         * @param {boolean} [append=false] - Appends options when true; otherwise replaces existing inputs.
         */
        update: function (conf, options, append) {
            const value = buttonGroup.get(conf);
            buttonGroup._addOptions(conf, options, append);
            const inputs = conf._input.find("input").toArray();
            // Refreshed options can replace a string value like "2" with numeric 2.
            // Also match the radio input's string value so this does not reset the selection to the first option.
            const current = inputs.find(input => input._editor_val === value || input.value === String(value));
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
