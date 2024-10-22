import WJ from "../../src/js/webjet";

/**
 * Simple static text field for DataTables Editor
 * @returns
 */
export function typeStaticText() {
    return {
        create: function ( conf ) {
            const id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._input = $('<div/>').attr( $.extend( {
                id: id,
                type: 'text'
            }, conf.attr || {} ) );

            conf._input.html(WJ.parseMarkdown(conf._input.attr("data-value")));

            return conf._input[0];
        },

        get: function ( conf ) {
            return '';
        },

        set: function ( conf, val ) {
            // do nothing
        }
    }
}