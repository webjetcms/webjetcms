import { VueTools } from '../../src/js/libs/tools/vuetools';

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
            var htmlCode = $('<textarea id="'+id+'" style="display: none;"></textarea><div class="vueComponent" id="editorApp'+id+'"><webjet-dte-jstree :data-table-name="dataTableName" :data-table="dataTable" :click="click" :id-key="idKey" :data="data" :attr="attr" @remove-item="onRemoveItem"></webjet-dte-jstree></div>');
            conf._id = id;
            //htmlCode je pole elementov, input pole je prvy objekt v zapise (textarea)
            conf._input = $(htmlCode[0]);
            if (typeof conf.attr != undefined && conf.attr != null) {
                $.each(conf.attr, function( key, value ) {
                    //console.log("Setting attr: key=", key, " value=", value);
                    $(conf._input).attr(key, value);
                });
            }
            //odkaz na DIV element, v ktorom sa inicializuje VUE komponenta
            conf._el = htmlCode[1];
            conf.jsonData = [];
            conf.vm = null;
            return htmlCode;
        },

        get: function ( conf ) {
            //console.log("vm=", conf.vm);
            var json = conf.vm.data;
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

            //vue kompoennta este nebola inicializovana, pri opakovanom otvoreni DTED sa toto uz nevola
            //console.log("conf.vm=", conf.vm, "conf.jsonData=", conf.jsonData);
            if (conf.vm == null)
            {
                const dataTableName = this.s.table.slice(1);
                const app = window.VueTools.createApp({
                    components: {},
                    data() {
                        return {
                            data: null,
                            idKey: null,
                            dataTable: null,
                            dataTableName: null,
                            click: null,
                            attr: null
                        }
                    },
                    created() {
                        this.data = fixNullData(conf.jsonData, conf.className);
                        //console.log("JS created, data=", this.data, " conf=", conf, " val=", conf._input.val());
                        this.idKey = conf._id;
                        this.dataTableName = dataTableName;
                        //co sa ma stat po kliknuti prenasame z atributu className datatabulky (pre jednoduchost zapisu), je to hodnota obsahujuca dt-tree-
                        //priklad: className: "dt-row-edit dt-style-json dt-tree-group", click=dt-tree-group
                        const confClassNameArr = conf.className.split(" ");
                        for (var i=0; i<confClassNameArr.length; i++) {
                            let className = confClassNameArr[i];
                            if (className.indexOf("dt-tree-")!=-1) this.click = className;
                        }
                        //console.log("click=", this.click);
                        this.dataTable = EDITOR.TABLE;
                        if (typeof(conf.attr)!="undefined") this.attr = conf.attr;
                    },
                    methods: {
                        onRemoveItem(id){
                            //console.log("REMOVE impl, id=", id, "click=", this.click);
                            let that = this;
                            this.data = this.data.filter(function( obj ) {
                                //console.log("Testing ", obj.groupId+" doc=", obj.docId);
                                if (that.click.indexOf("dt-tree-page")!=-1) return obj.docId !== id;
                                else if (that.click.indexOf("dt-tree-group")!=-1) return obj.groupId !== id;
                                else return obj.id !== id;
                            });

                            window.$("#"+this.idKey).val(JSON.stringify(this.data, undefined, 4));
                        }
                    }
                });
                VueTools.setDefaultObjects(app);
                app.component('webjet-dte-jstree', window.VueTools.getComponent('webjet-dte-jstree'));
                const vm = app.mount(conf._el);

                conf.vm = vm;
            } else {
                //pri opakovanom otvoreni DTED uz len nastav data
                let newJson = fixNullData(conf.jsonData, conf.className);
                //conf.vm._instance.data.data = newJson;
                conf.vm.data = newJson;
                //console.log("Updated, newJson=", newJson, ", data=", conf.vm._instance.data.data);
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