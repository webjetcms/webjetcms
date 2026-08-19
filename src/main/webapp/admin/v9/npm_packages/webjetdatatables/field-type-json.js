import { createWebjetDteJsTree } from '../../src/js/web-components/webjet-dte-jstree';

export function typeJson() {

    function fixNullData(data, click) {
        //console.log("fixNullData, data=", data, "click=", click);
        //ak to je pole neriesime, ponechame bezo zmeny
        if (click.indexOf("-array")!=-1) return data;
        //ak to nie je pole, musime nafejkovat jeden objekt aby sa pole aspon zobrazilo (a dala sa zmenit hodnota)
        if (data.length==0) {
            let emptyItem = {
                fullPath: ""
            }
            if (click.indexOf("dt-tree-page")!=-1) emptyItem.docId = -1;
            else if (click.indexOf("dt-tree-group")!=-1) emptyItem.groupId = -1;
            else emptyItem.id = -1;

            return [emptyItem];
        }
        return data;
    }

    return {
        create: function ( conf ) {
            //console.log("Creating JSON field, conf=", conf, "editor=", this);
            var id = $.fn.dataTable.Editor.safeId( conf.id );
            //tato jquery konstrukcia vytvori len pole objektov, nie su to este normalne elementy
            var htmlCode = $('<textarea id="'+id+'" style="display: none;"></textarea><div class="webjet-component" id="editorApp'+id+'"></div>');
            conf._id = id;
            //htmlCode je pole elementov, input pole je prvy objekt v zapise (textarea)
            conf._input = $(htmlCode[0]);
            if (typeof conf.attr != undefined && conf.attr != null) {
                $.each(conf.attr, function( key, value ) {
                    //console.log("Setting attr: key=", key, " value=", value);
                    $(conf._input).attr(key, value);
                });
            }
            //Container used to initialize the Web Component.
            conf._el = htmlCode[1];
            conf.jsonData = [];
            conf.component = null;
            return htmlCode;
        },

        get: function ( conf ) {
            //console.log("vm=", conf.vm);
            var json = conf.component.getValue();
            //console.log("vm=", conf.vm, "json=", json);
            //console.log("Returning json ("+conf.className+"): ", json)
            if (conf.className.indexOf("dt-tree-dir-simple")!=-1) {
                //get value from input to allow change of value by user
                //return json[0].virtualPath;
                let val = $("#"+this.TABLE.DATA.id+"_modal #editorApp"+conf._id+" div.dt-tree-container div.input-group input.form-control").val();
                //console.log("#"+this.TABLE.DATA.id+"_modal #editorApp"+conf._id+" div.dt-tree-container div.input-group input.form-control=", val);
                return val;
            }
            if (conf.className.indexOf("-array")==-1) return json[0];
            return json;
        },

        set: function ( conf, val ) {
            //console.log("set, val=", val, " EDITOR=", this);
            var EDITOR = this;

            if (conf.className.indexOf("dt-tree-dir-simple")!=-1) {
                let json = {
                    virtualPath: val,
                    type: "DIR",
                    id: val
                }
                val = json;
            }

            //console.log("val v2=", val);

            //defaultne (pri vytvoreni noveho zaznamu) predpokladajme, ze to je POLE
            if ("" == val) val = [];

            var jsonString = JSON.stringify(val, undefined, 4);
            conf._input.val(jsonString);

            var value = JSON.parse(conf._input.val());
            //jsonData musia byt obalene do [] pre pole
            conf.jsonData = Array.isArray(value) ? JSON.parse(conf._input.val()) : [JSON.parse(conf._input.val())];
            //null sa vracia ako text pre prazdnu hodnotu, pripravime ako pole
            if ("null" == conf._input.val()) conf.jsonData = [];

            if (conf.component == null)
            {
                const dataTableName = this.s.table.slice(1);
                const mode = conf.className.split(" ").find(className => className.indexOf("dt-tree-") !== -1);
                conf.component = createWebjetDteJsTree({
                    inputElement: conf._input[0],
                    dataTableName,
                    dataTable: EDITOR.TABLE,
                    mode,
                    attributes: conf.attr,
                    value: fixNullData(conf.jsonData, conf.className)
                });
                conf._el.appendChild(conf.component);
            } else {
                let newJson = fixNullData(conf.jsonData, conf.className);
                conf.component.setValue(newJson);
            }
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
