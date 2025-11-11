import { VueTools } from '../../src/js/libs/tools/vuetools';
import WJ from '../../src/js/webjet';

import * as fieldTypeQuill from './field-type-quill';

function getEmptyStringFieldValue() {
    return "";
}

function isNumeric(str) {
    if (typeof str != "string") return false // we only process strings!
    return !isNaN(str) && // use type coercion to parse the _entirety_ of the string (`parseFloat` alone does not do this)...
           !isNaN(parseFloat(str)) // ...and ensure strings of whitespace fail
  }

function getFieldValue(value, action, fieldType) {
    //
    if(fieldType === "number") {
        if(isNumeric(value)) return Number(value);
        else return "";
    } else if(fieldType === "none") {
        return "";
    } else if(fieldType === "boolean" || fieldType === "boolean_text") {
        if(value === "1" || value === 1 || (typeof value==="string" && value.toUpperCase() === "TRUE")) return true;
        else return false;
    } else if(fieldType === "quill") {
        value = value.replaceAll(/&lt;/gi, "<");
        value = value.replaceAll(/&gt;/gi, ">");
        return value;
    } else if(fieldType === "textarea") {
        console.log(value, action, fieldType);
        return value
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    } else {
        value = value.replace(/"/gi, "&quot;");
        if(action === "create") return getEmptyStringFieldValue();
        else if(value == null) return getEmptyStringFieldValue();
        else return value;
    }
}

function disableField(isDisabled) {
    if(isDisabled === undefined || isDisabled === null || isDisabled === false) return '';
    return 'disabled="disabled"';
}

function isDateEmpty(dateValue) {
    if(dateValue === undefined || dateValue === null || dateValue.length < 1 || (dateValue.trim()).length < 1) return true;
    else return false;
}

function generateUUID() {
    let uuid = null;
    try {
        uuid = crypto.randomUUID();
        //console.log("Generated uuid API: ", uuid);
    } catch (e) {
        //generator is not abailable, use fallback
    }

    if (uuid == null || uuid.length<3) {
        var d = new Date().getTime();
        var d2 = ((typeof performance !== 'undefined') && performance.now && (performance.now()*1000)) || 0;//Time in microseconds since page-load or 0 if unsupported
        uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random() * 16;//random number between 0 and 16
            if(d > 0){//Use timestamp until depleted
                r = (d + r)%16 | 0;
                d = Math.floor(d/16);
            } else {//Use microseconds since page-load if supported
                r = (d2 + r)%16 | 0;
                d2 = Math.floor(d2/16);
            }
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
        //console.log("Generated uuid random: ", uuid);
    }
    return uuid;
}

export function update(EDITOR, action) {

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

    let datatable = EDITOR.TABLE;
    let json = EDITOR.currentJson;
    let booleanTextFields = [];

    if (typeof json == "undefined") {
        //je to novy zaznam, ziskaj nastavenia z prveho zaznamu
        try {
            json = EDITOR.TABLE.DATA.json.data[0];
            //zduplikuj
            json = JSON.parse(JSON.stringify(json));
            //console.log("json=", json);
            for (let field of json?.editorFields?.fieldsDefinition) {
                //vymaz values
                field.value = null;
            }
        } catch (e) {
            //console.log(e);
        }
    }

    if (typeof json == "undefined" || json == null || typeof json.editorFields == "undefined" || json.editorFields == null || typeof json.editorFields.fieldsDefinition == "undefined" || json.editorFields.fieldsDefinition == null) return;

    //WJ.log("CustomFields.update, json=", json);

    //pomen mena poli
    var textTemplate = '<input id="DTE_Field_{customPrefix}{identifier}" maxlength="{maxlength}" data-warningLength="{warninglength}" data-warningMessage="{warningMessage}" value="{value}" {disabled} class="form-control" type="text">';
    var textAreaTemplate = '<textarea id="DTE_Field_{customPrefix}{identifier}" {disabled} class="form-control">{value}</textarea>';
    var autocompleteTemplate = '<div class="input-group"> <span class="input-group-text"><i class="ti ti-search"></i></span> <input type="text" class="form-control autocomplete" name="field{identifier}" value="{value}" id="DTE_Field_field{identifier}"/> </div>';
    var selectTemplate = '<select id="DTE_Field_field{identifier}" class="form-control form-select">{options}</select>';
    var labelTemplate = '<div class="input-group"> <span class="input-group-text noborders field-type-label">{value}</span> <input value="{value}" id="DTE_Field_{customPrefix}{identifier}" class="form-control" type="hidden"></div>';
    var numberTemplate = '<input id="DTE_Field_{customPrefix}{identifier}" type="number" value="{value}" {disabled} class="form-control">';
    var booleanTemplate = '<div><div class="custom-control form-switch"><input id="DTE_Field_{customPrefix}{identifier}" type="checkbox" {disabled} class="form-check-input"><label for="DTE_Field_{customPrefix}{identifier}" class="form-check-label">Áno</label></div></div>';
    var booleanTextTemplate = '<div><div class="custom-control form-switch"><input id="DTE_Field_{customPrefix}{identifier}" type="checkbox" {disabled} class="form-check-input"><label for="DTE_Field_{customPrefix}{identifier}" class="form-check-label">{label_value}</label></div></div>';
    var dateTemplate = '<input id="DTE_Field_{customPrefix}{identifier}" type="text" autocomplete="off" class="form-control">';
    var uuidTemplate = '<input id="DTE_Field_{customPrefix}{identifier}" maxlength="255" value="{value}" class="form-control field-type-uuid" type="text">';
    var colorTemplate = `
        <div class="input-group">
            <span class="input-group-text color-preview" style="background-color: {value}"></span>
            <input id="DTE_Field_{customPrefix}{identifier}" value="{value}" {disabled} class="form-control" type="text"/>
            <button class="btn btn-outline-secondary btn-clear" type="button"><i class="ti ti-circle-x"></i></button>
        </div>
        <color-picker id="DTE_Field_{customPrefix}{identifier}_picker" label-title="${WJ.translate("datatables.field.color.title.js")}" label-hue="${WJ.translate("datatables.field.color.hue.js")}" label-saturation="${WJ.translate("datatables.field.color.saturation.js")}" label-lightness="${WJ.translate("datatables.field.color.lightness.js")}" label-opacity="${WJ.translate("datatables.field.color.alpha.js")}" label-ok="${WJ.translate("datatables.field.color.ok.js")}"></color-picker>
    `

    //JICH - add
    var hiddenTemplate = '<input value="{value}" id="DTE_Field_field{identifier}" class="form-control" type="hidden"><div id="fieldDisplay{identifier}"></div>';
    //JICH - add end
    var fields = json.editorFields.fieldsDefinition;

    action = (action === undefined || action === null) ? "" : action;

    //console.log("Fields=", fields);

    $.each(fields, function(i, v) {
        //If we use numberIdentifier no change, or we use alphabet identifier then upperCase
        var identifier = v.key;
        try {
            identifier = v.key.toUpperCase();
        } catch (e) {}

        //if ("B"==identifier) console.log("Updating field "+identifier, v);

        //Custom prefix can change field prefix name, if this prefix is not set, use default value "field"
        var customPrefix = v.customPrefix;
        if(customPrefix === undefined || customPrefix === null || customPrefix.length < 1) customPrefix = "field";

        //
        var value = v.value;
        if(value == null) {
            //nebolo poslane v datach, ziskajme priamo z JSONu
            value = json[customPrefix+identifier];
        }
        if(value == null || value == "null") {
            value = getEmptyStringFieldValue();
        }
        let valueUnescaped = value;
        if(v.type !== "number" && v.type !== "boolean" && v.type !== "boolean_text" && v.type !== "date" && v.type !== "none")
            value = value.replace(/"/gi, "&quot;");

        if("uuid" === v.type) {
            if(value == null || value.length<3) {
                value = generateUUID();
            }
        }

        //console.log("Custom-fields type=", v.type, "v=", v, "value=", value);

        //nastav label
        //console.log("#" + datatable.DATA.id + "_modal .DTE_Field_Name_" + customPrefix + identifier + " label");
        //toto nastane v buble editacii
        if ($("#" + datatable.DATA.id + "_modal .DTE_Field_Name_" + customPrefix + identifier + " label").length < 1) return;

        $("#"+datatable.DATA.id + "_modal .DTE_Field_Name_" + customPrefix + identifier + " label").contents().first()[0].textContent = v.label;

        //over typ pola a zmen ak treba
        var	container = $("#" + datatable.DATA.id + "_modal .DTE_Field_Name_" + customPrefix + identifier),
        input = container.find('input, select, textarea'),
        inputBox = input.closest('.DTE_Field_InputControl');

        container.show();

        //if ("B"==identifier) console.log("container=", container, "input=", input, "inputBox=", inputBox);

        var maxlength = v.maxlength;
        var warninglength;
        var warningMessage;

        if (v.warninglength <= 0) {
            warninglength = "";
        } else {
            warninglength = v.warninglength;
        }
        if (v.warningMessage == null) {
            warningMessage = "";
        } else {
            warningMessage = v.warningMessage;
        }

        //DEFAULT template, "textTemplate"
        var template = textTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), getFieldValue(value, action, v.type)).replace(new RegExp('{maxlength}', 'g'), maxlength).replace(new RegExp('{warninglength}', 'g'), warninglength).replace(new RegExp('{warningMessage}', 'g'), warningMessage).replace(new RegExp('{disabled}', 'g'), disableField(v.disabled));

        if (v.type == 'textarea') {
            template = textAreaTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), getFieldValue(value, action, v.type)).replace(new RegExp('{disabled}', 'g'), disableField(v.disabled));
        } else if(v.type == 'number') {
            template = numberTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), getFieldValue(value, action, v.type)).replace(new RegExp('{disabled}', 'g'), disableField(v.disabled));
        } else if(v.type == 'boolean') {
            template = booleanTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{disabled}', 'g'), disableField(v.disabled));
        } else if(v.type == 'boolean_text') {
            template = booleanTextTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{disabled}', 'g'), disableField(v.disabled)).replace(new RegExp('{label_value}', 'g'), v.label);
            let id = "DTE_Field_{customPrefix}{identifier}".replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier);
            booleanTextFields.push(id);
        } else if(v.type == 'uuid') {
            template = uuidTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), getFieldValue(value, action, v.type)).replace(new RegExp('{disabled}', 'g'), disableField(v.disabled));
        } else if(v.type == 'date') {
            //console.log("DATE");
            template = dateTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier);
        } else if (v.type == 'select') {
            var options = '';
            $.each(v.typeValues, function(it, val){
                var selected = v.multiple ? value.split("|").indexOf(val.value) > -1 : val.value == value;
                options += '<option ' + (selected ? ' selected="true"' : "") + ' value="' + val.value + '">' + val.label + '</option>';
            });

            template = selectTemplate.replace('{options}', options).replace(new RegExp('{identifier}', 'g'), identifier);
            if(v.multiple) {
                template = template.replace("<select", "<select multiple");
            }
        } else if (v.type == 'autocomplete') {
            template = autocompleteTemplate.replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), value);
        } else if (v.type == 'label') {
            template = labelTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), WJ.escapeHtml(valueUnescaped));
        } else if (v.type == 'image') {
            let backgroundImage = "none";
            let prependClassName = "";
            if (value != "") {
                backgroundImage = "url("+value+")";
                prependClassName = " has-image";
            }
            template = '<div class="input-group"> <span class="input-group-text'+prependClassName+'" style="background-image: '+backgroundImage+';"><i class="ti ti-photo"></i></span> ' + template + ' <button class="btn btn-outline-secondary" type="button" onclick="WJ.openElFinderButton(this);"><i class="ti ti-focus-2"></i></button> </div>';
        } else if (v.type == 'link') {
            template = '<div class="input-group"> ' + template + ' <button class="btn btn-outline-secondary" type="button" onclick="WJ.openElFinderButton(this);"><i class="ti ti-focus-2"></i></button> </div>';
        } else if (v.type == 'dir') {
            template = '<div> ' + template + ' <div class="vueComponent" id="DTE_Field_' + customPrefix + identifier + '"><webjet-dte-jstree :data-table-name="dataTableName" :data-table="dataTable" :click="click" :id-key="idKey" :data="data" :attr="attr"></webjet-dte-jstree></div> </div>';
        } else if (v.type == 'json_group' || v.type == 'json_doc') {
            template = '<div> ' + template + ' <div class="vueComponent" id="DTE_Field_' + customPrefix + identifier + '"><webjet-dte-jstree :data-table-name="dataTableName" :data-table="dataTable" :click="click" :id-key="idKey" :data="data" :attr="attr"></webjet-dte-jstree></div> </div>';
        } else if (v.type == 'none') {
            // LPA
            container.hide();
        } else if (v.type == 'hidden') {
            //JICH - add
            var button = "";
            if (v.typeValues[0] != null) {
                var dialogScript = v.typeValues[0].label;
                if (dialogScript.indexOf("?") > 0) {
                    dialogScript += "&";
                } else {
                    dialogScript += "?";
                }
                var dialogScript2 = dialogScript + "fieldName=fieldInput" + identifier + "&fieldValue=" + value;

                if (v.typeValues[0].value == null || v.typeValues[0].value == "") {
                    //kdyz nemame displayScript prepneme na textTemplate
                    button = "<span type=\"button\" id=\"fieldButton" + identifier + "\" class=\"btn green input-group-addon\"><i class=\"ti ti-focus-2\"></i></span>";
                    hiddenTemplate = textTemplate;
                    hiddenTemplate = hiddenTemplate + button;
                } else {
                    button = "<span type=\"button\" id=\"fieldButton" + identifier + "\" class=\"float-end btn green\"><i class=\"ti ti-focus-2\"></i></span>";
                    hiddenTemplate = button + hiddenTemplate;
                }
            }
            template = hiddenTemplate.replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), value);
            //JICH - add end
        } else if (v.type == 'quill') {
            let id = "DTE_Field_{customPrefix}{identifier}";
            id = id.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier);

            let conf = {"id" : id};
            let quill = fieldTypeQuill.typeQuill();
            template = quill.create(conf);
            quill.set(conf, getFieldValue(value, action, v.type));
        } else if (v.type == 'color') {
            template = colorTemplate.replace(new RegExp('{customPrefix}', 'g'), customPrefix).replace(new RegExp('{identifier}', 'g'), identifier).replace(new RegExp('{value}', 'g'), getFieldValue(value, action, v.type)).replace(new RegExp('{maxlength}', 'g'), maxlength).replace(new RegExp('{warninglength}', 'g'), warninglength).replace(new RegExp('{warningMessage}', 'g'), warningMessage).replace(new RegExp('{disabled}', 'g'), disableField(v.disabled));
        }

        inputBox.html(template);

        //For every field, remove params s.opts._input AND s.opts.renderFormat -> they can be still set from previous field initialization
        //If previous field was quill, it will make problem with saving
        EDITOR.field(customPrefix + identifier).s.opts._input = "";
        EDITOR.field(customPrefix + identifier).s.opts.renderFormat = "";

        if(v.type == 'boolean' || v.type == 'boolean_text') {
            var origType = EDITOR.field(customPrefix + identifier).s.opts["type"];
            var booleanValue = getFieldValue(value, action, v.type);
            var input;

            //If origType is TEXT, we changing text fiel to checkbox field
            //If origType is CHECKBOX, we are using same field type
            if(origType == "text") {
                //It's checkbox input inside text column
                input = inputBox.find('input');

                //Need to add ON-CHANGE event to set value
                input.on("change", function() {
                    var $this = $(this);
                    $this.prop("value", ""+$this.is(":checked"));
                });

                //Firts time we must set value, or "ON" string will be saved (TEXT value)
                input.prop("value", "" +  booleanValue);

                //!! For som reason, boolean does not change it's value on true inside web page tab, with nested table Media
                //Looks like duplicite code because we set checked value below with "attr" but it must stay here
                input.prop("checked", booleanValue);

            } else {
                //In original editor it's set to parent/parent div wrapper
                input = inputBox.find('input').parent().parent();
            }

            //Set prepared input in editor field
            EDITOR.field(customPrefix + identifier).s.opts._input = input;

            //set field value for checked state
            inputBox.find('input')[0]._editor_val = true;

            //Set booelan via jQuery because we cant set checkbox throu html template
            $('#DTE_Field_' + customPrefix + identifier).attr('checked', booleanValue);
        } else if(v.type == 'date') {
            var opts = EDITOR.field(customPrefix + identifier).s.opts;
            opts._input = inputBox.find('input');

            input._picker = new $.fn.dataTable.DateTime(opts._input, $.extend({
                format: 'L',
                momentLocale: window.userLng,
                locale: window.userLng,
                i18n: EDITOR.i18n.datetime
            }, opts));

            //FIX - if string (aka date) is valid, set this value into input
            if(!isDateEmpty(value)) {
                $(opts._input).val(value);
            }

        } else if(v.type == 'quill') {
            EDITOR.field(customPrefix + identifier).s.opts._input = inputBox.find(".ql-editor");
            EDITOR.field(customPrefix + identifier).s.opts.renderFormat = "dt-format-quill";

        } else {
            EDITOR.field(customPrefix + identifier).s.opts._input = inputBox.find('input, select, textarea');
        }

        if (v.type == 'select') {
            inputBox.find('select').selectpicker(EDITOR.DT_SELECTPICKER_OPTS_EDITOR);
        }
        else if (v.type == 'autocomplete') {

            new AutoCompleter("#"+datatable.DATA.id+"_modal .DTE_Field_Name_field" + identifier + " input.autocomplete").setUrl('/admin/FCKeditor/_editor_autocomplete.jsp?keyPrefix=' + json.editorFields?.fieldsDefinitionKeyPrefix + '&template=' + json.tempId + '&field=' + identifier).transform();

        } else if (v.type == "dir") {
            let conf = {};
            let id = 'DTE_Field_' + customPrefix + identifier;

            //There must by allso prefix of datatable.DATA.id, because table can be nested in another table with same columns
            //And first-child because it's text input to hide and second child will be VUE component
            var textFieldInput  = $("#" + datatable.DATA.id + "_modal #" + id + ":first-child");
            textFieldInput.hide();

            conf._id = id;
            conf._el = inputBox.find('div.vueComponent')[0];
            conf.className = "dt-tree-dir-simple";
            let dataTableName = datatable.DATA.id;
            conf.jsonData = [{
                virtualPath: value,
                type: "DIR",
                id: value
            }];
            const vm = window.VueTools.createApp({
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
                    remove(id) {
                        //console.log("REMOVE impl, id=", id, "click=", this.click);
                        let that = this;
                        this.data = this.data.filter(function( obj ) {
                            //console.log("Testing ", obj.groupId+" doc=", obj.docId);
                            if (that.click.indexOf("dt-tree-page")!=-1) return obj.docId !== id;
                            else if (that.click.indexOf("dt-tree-group")!=-1) return obj.groupId !== id;
                            else return obj.id !== id;
                        });
                        window.$(textFieldInput).val(JSON.stringify(this.data, undefined, 4));
                    }
                }
            });
            VueTools.setDefaultObjects(vm);

            vm.component('webjet-dte-jstree', window.VueTools.getComponent('webjet-dte-jstree'));
            vm.mount(conf._el);
        } else if ("uuid"==v.type) {
            //console.log("inputBox=", inputBox);
            var inputField = inputBox.find("input.field-type-uuid");
            inputField = inputBox.find("input.field-type-uuid").on("blur", function() {
                if (this.value=="") this.value=generateUUID();
            });
            setTimeout(() => {
                //there was problem that new page overwrite value, so we generate new uuid
                if (inputField.val()=="") inputField.val(generateUUID());
            }, 1000);
        } else if ("color"==v.type) {
            var conf = {};
            var htmlCode = inputBox;
            conf._preview = htmlCode.find(".color-preview");
            conf._input = htmlCode.find("input");
            conf._clear = htmlCode.find(".btn-clear");
            conf._picker = htmlCode.find("color-picker")[0];

            function setColor(conf, val) {
                //console.log("Update color, val=", val);
                conf._input.val(val);
                conf._preview.css("background-color", val);
            }

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
        } else if (v.type == "json_group" || v.type == "json_doc") {
            let conf = {};
            let id = 'DTE_Field_' + customPrefix + identifier;

            //There must by allso prefix of datatable.DATA.id, because table can be nested in another table with same columns
            //And first-child because it's text input to hide and second child will be VUE component
            var textFieldInput  = $("#" + datatable.DATA.id + "_modal #" + id + ":first-child");
            textFieldInput.hide();

            conf._id = id;
            conf._el = inputBox.find('div.vueComponent')[0];

            //Prepare className
            if(v.className == undefined || v.className == null || v.className.length < 1) {
                if(v.type == "json_group") {
                    conf.className = "dt-tree-groupid";
                } else if(v.type == "json_doc") {
                    conf.className = "dt-tree-pageid";
                }
            } else {
                conf.className = v.className;
            }

            let preSetData = null;
            if(valueUnescaped != undefined && valueUnescaped != null && value.length) {
                preSetData = JSON.parse(valueUnescaped);

                //Can be returned only ID, we need check and use json data
                if(preSetData == null || (typeof preSetData) != "object" || ("id" in preSetData) == false || ("fullPath" in preSetData) == false) {
                    preSetData = json[customPrefix + identifier];
                }
            } else {
                preSetData = json[customPrefix + identifier];
            }

            //DO check again, if preSetData do not contain needed keys, set null
            if(preSetData == null || (typeof preSetData) != "object" || ("id" in preSetData) == false || ("fullPath" in preSetData) == false) {
                preSetData = null;
            }

            if(conf.className.indexOf("dt-tree-groupid") != -1 ) {
                // GROUP jsonData init
                if(preSetData == null) {
                    conf.jsonData = [{
                        "groupId": "",
                        "fullPath": ""
                    }];
                } else {
                    conf.jsonData = [{
                        "groupId": preSetData["id"],
                        "fullPath": preSetData["fullPath"]
                    }];
                }
            } else if(conf.className.indexOf("dt-tree-pageid") != -1 ) {
                // DOC jsonData init
                if(preSetData == null) {
                    conf.jsonData = [{
                        "docId": "",
                        "fullPath": ""
                    }];
                } else {
                    conf.jsonData = [{
                        "docId": preSetData["id"],
                        "fullPath": preSetData["fullPath"]
                    }];
                }
            } else {
                // Base jsonData init
                conf.jsonData = [{
                    virtualPath: value,
                    id: value
                }];
            }

            let dataTableName = datatable.DATA.id;
            const vm = window.VueTools.createApp({
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
                    remove(id) {
                        //console.log("REMOVE impl, id=", id, "click=", this.click);
                        let that = this;
                        this.data = this.data.filter(function( obj ) {
                            //console.log("Testing ", obj.groupId+" doc=", obj.docId);
                            if (that.click.indexOf("dt-tree-page")!=-1) return obj.docId !== id;
                            else if (that.click.indexOf("dt-tree-group")!=-1) return obj.groupId !== id;
                            else return obj.id !== id;
                        });
                        window.$(textFieldInput).val(JSON.stringify(this.data, undefined, 4));
                    }
                }
            });
            VueTools.setDefaultObjects(vm);

            vm.component('webjet-dte-jstree', window.VueTools.getComponent('webjet-dte-jstree'));
            vm.mount(conf._el);

            //return original docId value to field instead of JSON string
            if (typeof v.originalValue != "undefined" && v.originalValue != null) textFieldInput.val(v.originalValue);
        }

        //JICH - add
        //musime AJAXem zavolat skript pro zobrazeni custom obsahu
        if (v.type == 'hidden') {
            //console.log("jch, v=", v);
            if (v.typeValues[0] != null) {
                var displayScript = v.typeValues[0].value
                if (displayScript.indexOf("?") > 0) {
                    displayScript += "&";
                } else {
                    displayScript += "?";
                }
                displayScript += "fieldName=" + identifier + "&fieldValue=" + value;
                $.ajax({
                    type : "GET",
                    url : displayScript,
                    success : function(data) {
                        $("#fieldDisplay" + identifier).html($.trim(data));
                        //upravime tlacitku click udalost
                        $("#fieldButton" + identifier).on("click", function() {
                            var dialogScript2 = dialogScript + "fieldName=field" + identifier + "&fieldValue=" + value;
                            //WJ.openPopupDialog(dialogScript2, 800, 500);
                            WJ.openIframeModal({
                                url: dialogScript2,
                                width: 800,
                                height: 500,
                                okclick: function() {
                                    document.getElementById("modalIframeIframeElement").contentWindow.Ok();
                                    return false;
                                }
                            });
                        });
                    },
                    async : false
                });
            } else {
                $("#fieldButton" + identifier).on("click", function() {
                    var dialogScript2 = dialogScript + "fieldName=field" + identifier + "&fieldValue=" + value;
                    //WJ.openPopupDialog(dialogScript2, 800, 500);
                    WJ.openIframeModal({
                        url: dialogScript2,
                        width: 800,
                        height: 500,
                        okclick: function() {
                            document.getElementById("modalIframeIframeElement").contentWindow.Ok();
                            return false;
                        }
                    });
                });
            }
        }
        //JICH - add end
    });

    //Init tooltip on AI buttons
    setTimeout(function() {
        $("#"+datatable.DATA.id+"_modal button.btn-ai[data-toggle*='tooltip']").each(function(){
            let buttons = $(this);
            for (let i=0; i<buttons.length; i++) {
                //console.log("Init tooltip for button: ", $(buttons[i]));
                let button = $(buttons[i]);
                WJ.initTooltip(button, 'tooltip-ai');
            }
        });
    }, 1300);

    //Find label of booleanText field and set empty string
    if(booleanTextFields.length > 0) {
        booleanTextFields.forEach(function(item) {
            $("#"+datatable.DATA.id+"_modal label[for='"+item+"'].col-form-label").html("&nbsp;");
        });
    }

    //zapni zobrazenie varovania pre warninglength
    $("#"+datatable.DATA.id+"_modal input.form-control").each(function(){
        var input = $(this);
        var dataWarningLength = $(this).attr('data-warninglength');
        var dataWarningMessage = $(this).attr('data-warningmessage');
        if (dataWarningLength > 0) {
            input.on('input', function(){
                if (dataWarningLength <= input.val().length) {
                    toastr.remove();
                    WJ.notifyWarning(dataWarningMessage, null, 5000, null);
                } else {
                    toastr.remove();
                }
            })
        }
    });
}
