/**
 * Radio button with image. Image is get from original property of options.
 * @returns
 */
export function typeImageRadio() {

    var imageRadio = $.extend(true, {}, $.fn.dataTable.Editor.fieldTypes.radio, {
        _addOptions: function (conf, opts, append) {
            if (append === void 0) { append = false; }
            var jqInput = conf._input;
            var offset = 0;
            if (!append) {
                jqInput.empty();
            }
            else {
                offset = $('input', jqInput).length;
            }
            if (opts) {
                $.fn.dataTable.Editor.pairs(opts, conf.optionsPair, function (val, label, i, attr) {
                    jqInput.append('<div class="image_radio_item">' +
                        '<label for="' + $.fn.dataTable.Editor.safeId(conf.id) + '_' + (i + offset) + '">'
                            + '<div class="image_radio_item_inner">'
                                + '<input id="' + $.fn.dataTable.Editor.safeId(conf.id) + '_' + (i + offset) + '" type="radio" name="' + conf.name + '" />' +
                                + label
                            + '</div>'
                            + '<img src="' + opts[i].original + '"/>' +
                        '</label>' +
                        '</div>');
                    $('input:last', jqInput).attr('value', val)[0]._editor_val = val;
                    if (attr) {
                        $('input:last', jqInput).attr(attr);
                    }
                });
            }
        },
        create: function (conf) {
            conf._input = $('<div />');
            imageRadio._addOptions(conf, conf.options || conf.ipOpts);
            // this is ugly, but IE6/7 has a problem with radio elements that are created
            // and checked before being added to the DOM! Basically it doesn't check them. As
            // such we use the _preChecked property to set cache the checked button and then
            // check it again when the display is shown. This has no effect on other browsers
            // other than to cook a few clock cycles.
            this.on('open', function () {
                conf._input.find('input').each(function () {
                    if (this._preChecked) {
                        this.checked = true;
                    }
                });
            });
            return conf._input[0];
        },
        update: function (conf, options, append) {
            var currVal = imageRadio.get(conf);
            imageRadio._addOptions(conf, options, append);
            // Select the current value if it exists in the new data set, otherwise
            // select the first radio input so there is always a value selected
            var inputs = conf._input.find('input');
            imageRadio.set(conf, inputs.filter('[value="' + currVal + '"]').length ?
                currVal :
                inputs.eq(0).attr('value'));
        }
    });

    return imageRadio;
}