import WJ from "../../src/js/webjet";
//https://github.com/jmetaxas/color-dialog-box/
//npm install color-dialog-box
import 'color-dialog-box';

export function typeColor() {

    function setColor(conf, val) {
        //console.log("Update color, val=", val);
        conf._input.val(val);
        conf._preview.css("background-color", val);
    }

    //https://stackoverflow.com/questions/49974145/how-to-convert-rgba-to-hex-color-code-using-javascript
    function RGBAToHexA(rgba, forceRemoveAlpha = false) {
        return "#" + rgba.replace(/^rgba?\(|\s+|\)$/g, '') // Get's rgba / rgb string values
          .split(',') // splits them at ","
          .filter((string, index) => !forceRemoveAlpha || index !== 3)
          .map(string => parseFloat(string)) // Converts them to numbers
          .map((number, index) => index === 3 ? Math.round(number * 255) : number) // Converts alpha to 255 number
          .map(number => number.toString(16)) // Converts numbers to hex
          .map(string => string.length === 1 ? "0" + string : string) // Adds 0 when length of one number is 1
          .join("") // Puts the array to togehter to a string
      }

    return {
        create: function ( conf ) {
            //console.log("Creating COLOR field, conf=", conf, "this=", this);
            const id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._id = id;

            //tato jquery konstrukcia vytvori len pole objektov, nie su to este normalne elementy
            const htmlCode = $(`
                <div class="input-group">
                    <span class="input-group-text color-preview"></span>
                    <input type="text" id="${id}" value="" class="form-control" />
                    <button class="btn btn-outline-secondary btn-clear" type="button"><i class="ti ti-circle-x"></i></button>
                </div>
                <color-picker id="${id}_picker" label-title="${WJ.translate("datatables.field.color.title.js")}" label-hue="${WJ.translate("datatables.field.color.hue.js")}" label-saturation="${WJ.translate("datatables.field.color.saturation.js")}" label-lightness="${WJ.translate("datatables.field.color.lightness.js")}" label-opacity="${WJ.translate("datatables.field.color.alpha.js")}" label-ok="${WJ.translate("datatables.field.color.ok.js")}"></color-picker>
            `);
            conf._preview = htmlCode.find(".color-preview");
            conf._input = htmlCode.find("input");
            conf._clear = htmlCode.find(".btn-clear");
            conf._picker = htmlCode[2];

            setTimeout(function() {
                conf._input.on("click", function() {
                    conf._picker.setAttribute('open', true);
                    conf._picker.setAttribute('hex', conf._input.val());
                });
                conf._input.parent().find(".color-preview").on("click", function() {
                    conf._picker.setAttribute('open', true);
                    conf._picker.setAttribute('hex', conf._input.val());
                });
                conf._input.on("change", function() {
                    const val = conf._input.val();
                    conf._preview.css("background-color", val);
                });

                conf._clear.on("click", function() {
                    setColor(conf, "");
                });

                conf._picker.addEventListener('update-color', function(e) {
                    setColor(conf, e.detail.hex);
                });
            }, 500);

            return htmlCode;
        },

        get: function ( conf ) {
            return conf._input.val();
        },

        set: function (conf, val) {
            if (val != null && val.indexOf("rgba(")!=-1) {
                val = RGBAToHexA(val, false);
            }
            conf._input.val(val);
            conf._picker.setAttribute('hex', val);
            setColor(conf, val);
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