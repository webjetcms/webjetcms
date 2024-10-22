export function typeBase64() {

    return {
        create: function ( conf ) {
            const id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._input = $('<textarea/>').attr( $.extend( {
                id: id,
                type: 'text'
            }, conf.attr || {} ) );

            return conf._input[0];
        },

        get: function ( conf ) {
            let val = conf._input.val();
            if(val == undefined || val == null || val.length < 1) val = "";
            val = WJ.base64encode(val);
            return val;
        },

        set: function ( conf, val ) {
            if (val != null) val = WJ.base64decode(val);
            if (val != null && val.startsWith("utf8:")) {
                //old way to encode utf8 with base64
                val = decodeURIComponent(val.substring(5));
            }
            conf._input
                .val( val )
                .trigger( 'change' );
        },

        enable: function ( conf ) {
            conf._input.prop( 'disabled', false );
        },

        disable: function ( conf ) {
            conf._input.prop( 'disabled', true );
        },

        canReturnSubmit: function ( conf, node ) {
            return false;
        }
    }
}