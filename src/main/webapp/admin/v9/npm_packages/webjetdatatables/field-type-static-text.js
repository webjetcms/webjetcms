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

            var allowsHtml = false;
            const classNamesStr = conf.className;
            if(classNamesStr) {
                const classNamesArr = classNamesStr.split(" ");
                for(let i = 0; i < classNamesArr.length; i++) {
                    if("allow-html" === classNamesArr[i]) allowsHtml = true;
                }
            }

            if(allowsHtml === true) {
                conf._input.html(conf._input.attr("data-value"));
            } else {
                conf._input.html(WJ.parseMarkdown(conf._input.attr("data-value")));
            }

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