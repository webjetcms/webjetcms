export function typeIcon() {

    function setIcon(conf, val) {
        //console.log("Update color, val=", val);
        conf._input.val(val);
        //set CSS tabler icon to preview element
        conf._preview.attr("class", "ti ti-"+val);
    }

    return {
        create: function ( conf ) {
            //console.log("Creating COLOR field, conf=", conf, "this=", this);
            const id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._id = id;

            //tato jquery konstrukcia vytvori len pole objektov, nie su to este normalne elementy
            const htmlCode = $(`
                <div class="input-group">
                    <span class="input-group-text icon-preview"><i class="ti"></i></span>
                    <input type="text" id="${id}" value="" class="form-control" />
                    <button class="btn btn-outline-secondary btn-clear" type="button"><i class="ti ti-circle-x"></i></button>
                </div>
                <div class="text-body-secondary small">
                    ${WJ.translate("datatable.fields.icon.tooltip.js")}
                </div>
            `);
            conf._preview = htmlCode.find(".icon-preview i");
            conf._input = htmlCode.find("input");
            conf._clear = htmlCode.find(".btn-clear");

            setTimeout(function() {
                conf._input.on("change keyup", function() {
                    const val = conf._input.val();
                    setIcon(conf, val);
                });

                conf._clear.on("click", function() {
                    setIcon(conf, "");
                });
            }, 500);

            return htmlCode;
        },

        get: function ( conf ) {
            return conf._input.val();
        },

        set: function (conf, val) {
            conf._input.val(val);
            setIcon(conf, val);
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