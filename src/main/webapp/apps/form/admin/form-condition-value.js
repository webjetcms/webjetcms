/** A condition value is either free text or one of the source field's configured choices. */
$.fn.dataTable.Editor.fieldTypes.formConditionValue = {
    create(conf) {
        conf._text = $('<input type="text" class="form-control">').attr(conf.attr || {});
        conf._select = $('<select class="form-select"></select>');
        conf._choices = $('<div></div>').append(conf._select).hide();
        conf._input = conf._text;
        conf._text.attr('id', conf.id);
        conf._select.attr('id', conf.id + '_choices');
        conf._select.on('change', () => conf._text.val(conf._select[0].value));
        return $('<div></div>').append(conf._text, conf._choices);
    },

    get(conf) {
        return conf._text.val();
    },

    set(conf, value) {
        const text = value == null ? '' : String(value);
        conf._text.val(text);
        if (conf._options) {
            conf._select.find('option[data-previous-value]').remove();
            if (text !== '' && conf._options.some(option => option.value === text) === false) {
                // Preserve old conditions (including partial matches) until explicitly changed.
                conf._select.append($('<option></option>').val(text).text(text)
                    .attr('data-previous-value', 'true').prop('disabled', true));
            }
            conf._select.val(text);
            if (conf._select.data('selectpicker')) conf._select.selectpicker('refresh');
        }
    },

    update(conf, options) {
        conf._options = Array.isArray(options) ? options : null;
        const hasOptions = conf._options !== null;
        conf._text.toggle(hasOptions === false).attr('id', conf.id + (hasOptions ? '_text' : ''));
        conf._choices.toggle(hasOptions);
        conf._select.attr('id', conf.id + (hasOptions ? '' : '_choices'));
        conf._input = hasOptions ? conf._select : conf._text;
        conf._select.empty().append($('<option></option>').val('').text(''));
        for (const option of conf._options || []) {
            conf._select.append($('<option></option>').val(option.value).text(option.label));
        }
        $.fn.dataTable.Editor.fieldTypes.formConditionValue.set(conf, conf._text.val());
    },

    input(conf) {
        return conf._input;
    },

    enable(conf) {
        conf._text.add(conf._select).prop('disabled', false);
    },

    disable(conf) {
        conf._text.add(conf._select).prop('disabled', true);
    }
};
