/* REFAKTORING DO ES6 CLASS */
// class DataTableInit {
//     #table;
//     #DATA = new Array();

//     constructor(options) {
//         this.#DATA.columns = filterColumnsByPerms(options.columns);
//         this.#DATA.src = options.src;
//         this.#DATA.fields = [];
//         this.#DATA.serverSide = options.serverSide ? options.serverSide : false;
//         this.#DATA.tabsFolders = options.tabs ? options.tabs : [];
//         this.#DATA.url = options.url;
//         this.#DATA.editorId = options.editorId ? options.editorId : "id";
//         this.#DATA.onXhr = options.onXhr ? options.onXhr : null;
//     }

//     filterColumnsByPerms(columns) {
//         var filtered = [];
//         $.each(columns, function( key, col ) {
//             //console.log("col: ", col);

//             //kontrola prav
//             if(typeof col.perms !== "undefined") {
//                 if (window.nopermsJavascript[col.perms]===true) {
//                     //console.log("noperms: ", col.perms);
//                     return;
//                 }
//             }

//             filtered.push(col);
//         });
//         return filtered;
//     }
// }

import WJ from '../../src/js/webjet';
import * as dtConfig from './datatables-config';
import * as fieldTypeJson from './field-type-json';
import * as fieldTypeDatatable from './field-type-datatable';
import * as fieldTypeElfinder from './field-type-elfinder';
import * as fieldTypeQuill from './field-type-quill';
import * as fieldTypeWysiwyg from './field-type-wysiwyg';
import * as fieldTypeJsTree from './field-type-jstree';
import * as fieldTypeSelectEditable from './field-type-select-editable';
import * as fieldTypeAttrs from './field-type-attrs';
import * as dtWJ from './datatables-wjfunctions';
import * as CustomFields from './custom-fields';
import * as ExportImport from './export-import';
import {DatatableOpener} from "../../src/js/libs/data-tables-extends/";

const bootstrap = window.bootstrap = require('bootstrap');
import $ from 'jquery';
//console.log("Setting jQuery object to window");
window.jQuery = $;
window.$ = $;

window.dtWJ = dtWJ;

require('datatables.net');
require('datatables.net-bs5');
require('datatables.net-editor-bs5');
require('datatables.net-autofill-bs5');
require('datatables.net-buttons-bs5');
require('datatables.net-buttons/js/buttons.colVis.js');
//WebJET fixnuta verzia - problem s prefixButtons a postfixButtons
require('./buttons.colVis');
require('datatables.net-buttons/js/buttons.flash.js');
require('datatables.net-buttons/js/buttons.html5.js');
require('datatables.net-buttons/js/buttons.print.js');
require('datatables.net-colreorder-bs5');
//require('datatables.net-fixedcolumns-bs5');
require('datatables.net-fixedheader-bs5');
require('datatables.net-keytable-bs5');
require('datatables.net-responsive-bs5');
require('datatables.net-rowgroup-bs5');
//require('datatables.net-rowreorder-bs5');
require('datatables.net-scroller-bs5');
require('datatables.net-select-bs5');
require('datatables.net-datetime');

export const dataTableInit = options => {

    //console.log("Initializing DT, options=", options);

    /*
    options
    {
        id: podla ktoreho sa nabinduje datatable, pokial nie je zadefinovane nabinduje sa na css triedu datatableInit
        src: zdrojove data (objekt)
        url: URL adresa rest sevisu
        serverSide: ak je nastavene na true bude sa vyhladavanie/sortovanie/strankovanie posielat na server
        columns: definicia stlpcov
        tabs: definicia zaloziek pre Editor
        hideTable: boolean kvoli hidnutiu datatables
        noAll: suffix pre url pridavanie "+ '/all'"
    }
    */
    var TABLE;

    var DATA = [];
    DATA.id = options.id ? options.id : null;
    DATA.removeColumns = options.removeColumns ? options.removeColumns : null;
    DATA.src = options.src;
    DATA.fields = []; //polia pre DT editor
    DATA.serverSide = options.serverSide ? options.serverSide : false;
    DATA.tabsFolders = options.tabs ? options.tabs : [];
    DATA.url = options.url;
    DATA.editorId = options.editorId ? options.editorId : "id";
    DATA.onXhr = options.onXhr ? options.onXhr : null;
    DATA.onPreXhr = options.onPreXhr ? options.onPreXhr : null;
    DATA.json = null;
    DATA.jsonOptions = null;
    DATA.noAll = options.noAll;
    DATA.hideTable = options.hideTable || false;
    DATA.fetchOnCreate = options.fetchOnCreate ? options.fetchOnCreate : false;
    DATA.fetchOnEdit = options.fetchOnEdit ? options.fetchOnEdit : false;
    DATA.jsonField = options.jsonField ? options.jsonField : null;
    DATA.order = options.order ? options.order : [[0, 'asc']];
    DATA.paging = (typeof options.paging !== "undefined") ? options.paging : true;
    DATA.nestedModal = options.nestedModal ? options.nestedModal : false;
    DATA.initialData = options.initialData ? options.initialData : null;
    DATA.hideButtons = options.hideButtons ? options.hideButtons : null;
    DATA.onEdit = options.onEdit ? options.onEdit : null;
    DATA.onRowCallback = options.onRowCallback ? options.onRowCallback : null;
    DATA.forceVisibleColumns = options.forceVisibleColumns ? options.forceVisibleColumns : null;
    DATA.updateColumnsFunction = options.updateColumnsFunction ? options.updateColumnsFunction : null;
    DATA.idAutoOpener = (typeof options.idAutoOpener !== "undefined") ? options.idAutoOpener : true;
    DATA.perms = (typeof options.perms !== "undefined") ? options.perms : null;
    DATA.lastExportColumnName = (typeof options.lastExportColumnName !== "undefined") ? options.lastExportColumnName : null;
    DATA.byIdExportColumnName = (typeof options.byIdExportColumnName !== "undefined") ? options.byIdExportColumnName : null;
    DATA.editorButtons = options.editorButtons ? options.editorButtons : '<i class="fal fa-check"></i> ' + WJ.translate('button.save');
    DATA.keyboardSave = (typeof options.keyboardSave !== "undefined") ? options.keyboardSave : true;
    DATA.stateSave = (typeof options.stateSave !== "undefined") ? options.stateSave : true;
    DATA.autoHeight = (typeof options.autoHeight !== "undefined") ? options.autoHeight : true;
    DATA.customFieldsUpdateColumns = (typeof options.customFieldsUpdateColumns !== "undefined") ? options.customFieldsUpdateColumns : false;
    DATA.customFieldsUpdateColumnsPreserveVisibility = (typeof options.customFieldsUpdateColumnsPreserveVisibility !== "undefined") ? options.customFieldsUpdateColumnsPreserveVisibility : false;

    //nastavenie predvoleneho vyhladavania
    DATA.defaultSearch = (typeof options.defaultSearch !== "undefined") ? options.defaultSearch : null;

    //uklada automaticku velkost strany (prva polozka)
    DATA.autoPageLength = 10;
    DATA.autoPageLengthTitle = DATA.autoPageLength;

    //console.log("options=", options);

    //pre podporu multi tabuliek a editora potrebujeme mat unikatne ID pre kazdu DT a editor
    if (DATA.id === null) {
        DATA.id = "datatableInit"
        let datatables = $(".datatableInit");
        datatables.each(function(index, element) {
            var $this = $(this);
            var id = $this.attr("id");
            //console.log("id=", id, "DATA.id=", DATA.id, "equals=", (DATA.id===id));

            //ak je tam len jedna datatabulka setni bez ohladu na aktualne ID, asi je len zle zapisane
            if (datatables.length==1 || typeof id=="undefined" || id==="") $this.attr("id", DATA.id);
        })
    }

    //nastav stlpcom visible na true
    if (DATA.forceVisibleColumns != null) {
        let visibleColumns = options.forceVisibleColumns.split(",");

        $.each(options.columns, function (key, col) {
            for (let i=0; i<visibleColumns.length; i++) {
                if (col.data == visibleColumns[i] || col.name == visibleColumns[i]) {
                    col.visible = true;
                    col.hidden = false;
                    return;
                }
            }
            col.visible = false;
        });
    }

    //ak je nastavena update/merge funkcia zavolaj ju
    if (DATA.updateColumnsFunction != null) {
        window[DATA.updateColumnsFunction](options.columns);
    }

    DATA.columns = window.dataTableCellVisibilityService.updateColumnConfig(DATA.id, filterColumnsByPerms(options.columns));

    const dataTableSelector = `#${DATA.id}`;
    const urlSuffix = DATA.noAll ? '' : '/all';

    var DT_SELECTPICKER_OPTS = {
        container: "body",
        style: "btn btn-sm btn-outline-secondary",
        width: "100%",
        liveSearch: true,
        noneSelectedText: '\xa0' //nbsp
    };

    var DT_SELECTPICKER_OPTS_NOSEARCH = {
        container: "body",
        style: "btn btn-sm btn-outline-secondary",
        width: "100%",
        noneSelectedText: '\xa0' //nbsp
    };

    var DT_SELECTPICKER_OPTS_EDITOR = {
        container: "body",
        style: "btn btn-outline-secondary",
        liveSearch: true,
        showSubtext: true,
        noneSelectedText: '\xa0' //nbsp
    };

    const MAXIMIZE_HTML = '<div class="maximize"><i class="fas fa-window-maximize" title="'+WJ.translate("datatables.modal.maximize.js")+'" data-toggle="tooltip"></i><i class="fas fa-window-minimize" title="'+WJ.translate("datatables.modal.minimize.js")+'" data-toggle="tooltip"></i></div>';

    function filterColumnsByPerms(columns) {
        var filtered = [];
        var removeColumnsArr = null;
        if (DATA.removeColumns!=null) removeColumnsArr = DATA.removeColumns.split(",");

        //WJ.log("removeColumnsArr=", removeColumnsArr);
        $.each(columns, function (key, col) {
            //console.log("col: ", col);

            //kontrola prav
            if (typeof col.perms !== "undefined") {
                if (window.nopermsJavascript[col.perms] === true) {
                    //console.log("noperms: ", col.perms);
                    return;
                }
            }
            if (removeColumnsArr != null) {
                for (var i=0; i<removeColumnsArr.length; i++) {
                    if (col.data == removeColumnsArr[i]) {
                        //console.log("removing column, col=", col);
                        return;
                    }
                }
            }

            filtered.push(col);
        });
        return filtered;
    };

    /**
     * Upravi multiple select polia osahujuce ako hodnotu pole na string oddeleny pomocou |
     * @param data data z preSubmit
     */
    function prepareCustomFieldsDataBeforeSend(data) {
        var letters = 'ABCDEFGHIJKLMNOPQRST';
        for(var i in data.data) {
            for(var letter in letters) {
                if(Array.isArray(data.data[i]['field' + letters[letter]])) {
                    data.data[i]['field' + letters[letter]] = data.data[i]['field' + letters[letter]].join('|')
                }
            }
        }
    }

    /**
     * Opravi datove typy JSON objektu options, kde sa boolean posiela ako String
     * @param {*} options
     */
    function fixOptionsValueType(options) {
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === "true") options[i].value = true;
            if (options[i].value === "false") options[i].value = false;
        }
    }

    function filterColumnsByHidden(columns) {
        var filtered = [];
        $.each(columns, function (key, col) {
            //console.log("col: ", col);

            //kontrola prav
            if (typeof col.hidden !== "undefined" && col.hidden === true) {
                //console.log("hidden: ", col);
                return;
            }

            filtered.push(col);
        });
        return filtered;
    };

    function refreshRow(id, callback) {
        var url = TABLE.ajax.url();
        var q = url.indexOf("?");
        if (q === -1) url = url + "/" + id;
        else url = url.substring(0, q) + "/" + id + url.substring(q);

        //console.log("RefreshRow, url=", url, "id=", id);

        $.ajax({
            url: url,
            success: function (json) {
                //console.log("refreshSuccess, TABLE=", TABLE, "row=", TABLE.row, "id=", DATA.editorId);
                //console.log("refreshRow, id=", json[DATA.editorId], " json=", json);
                if (typeof json.error != "undefined" && json.error != null && json.error!="") {
                    WJ.notifyError(json.error);
                    return;
                }
                TABLE.row("#" + json[DATA.editorId]).data(json);

                try {
                    if (typeof json.editorFields != "undefined" && json.editorFields!=null && typeof json.editorFields.notify != "undefined" && json.editorFields.notify!=null) {
                        var notifyList = json.editorFields.notify;
                        showNotify(notifyList);
                    }
                } catch (e) {console.log(e);}

                //toto sa nemoze volat, pretoze to vyvola ajax request na server (pri serverSide: true) TABLE.draw(false);
                callback(json);
            }
        });
    }

    function showNotify(notifyList) {
        if (notifyList == null || typeof notifyList!="object") return;
        try {
            //console.log("showNotify, list=", notifyList);
            notifyList.forEach((item, index) => {
                //console.log("iterating, item=", item);
                WJ.notify(item.type, item.title, item.text, item.timeout, item.buttons);
            });
        } catch (e) {
            console.log(e);
        }
    }

    /**
     * Aktualizuje select v hlavicke/editore, nastavi optiony podla posledneho ajax requestu
     * @param {*} fieldName
     * @returns
     */
    function updateFilterSelect(fieldName) {
        var fieldNameSelector = fieldName;
        if (fieldNameSelector.indexOf(".")!=-1) fieldNameSelector = fieldNameSelector.replace(/\./gi, "\\.");
        var select = $("select.dt-filter-" + fieldNameSelector)[0];
        var currentValue = $(select).val();
        //console.log("updateFilterSelect, fieldNameSelector=", fieldNameSelector, " select=", select, "currentValue=", currentValue);

        if (typeof select != "undefined") {
            //zrus vsetky options
            select.options.length = 0;
            select.add(new Option("", ""));
            //pridaj options podla DATA objektu
            for (var index in DATA.columns) {
                //console.log("index: ", index);
                if (DATA.columns[index].data === fieldName) {
                    for (var optionIndex in DATA.columns[index].editor.options) {
                        var dataOption = DATA.columns[index].editor.options[optionIndex];
                        //prazdnu hodnotu sme pridali uz hore, preskoc
                        if (optionIndex==0 && dataOption.label=="" && dataOption.value=="") continue;
                        //console.log("option 2: ", dataOption);
                        var option = new Option(dataOption.label, dataOption.value);
                        if ("editorFields.statusIcons"===fieldName) {
                            option.setAttribute("data-content", dataOption.label);
                            //console.log("Set data attribute, option=", option);
                        }
                        select.add(option);
                    }
                    break;
                }
            }

            if (currentValue != null) $(select).val(currentValue);

            if (typeof $(select).data("selectpicker") !== "undefined") {
                //console.log("Updating selectpicker 2");
                $(select).selectpicker('refresh');
            }
        }

        let dteSelect = $("#DTE_Field_"+fieldNameSelector);
        //console.log("dteSelect=", dteSelect);
        if (typeof dteSelect.data("selectpicker") !== "undefined") {
            setTimeout(()=>{
                //console.log("Updating selectpicker DTE timeout ", dteSelect);
                dteSelect.selectpicker('refresh');
            }, 100);
        }
    }

    /**
     * Nastavi select/date pickre na DT hlavicke a vyvola jej pozicie podla DT (sirky stlpcov)
     */
    function fixDatatableHeaderInputs() {
        //console.log("fixDatatableHeaderInputs, DATA=", DATA);

        $('#' + DATA.id + '_wrapper select.filter-input').each(function(index, element) {
            //console.log("testing select, this=", this);
            let $this = $(this);

            if ($this.hasClass("selectpickerbinded")) return;
            $this.addClass("selectpickerbinded");

            //console.log("Setting selectpicker ", this);
            $this.selectpicker(DT_SELECTPICKER_OPTS);

            //mozno sa len pridal do hlavicky, nastav optiony
            //console.log("Aktualizujem optiony, this=", this.options.length);
            if (this.options.length<1) {
                let name = $this.data("dt-name");
                //console.log("name=", name);
                if (typeof name != "undefined" && name != null) {
                    updateFilterSelect(name);
                }
            }
        });
        $('#' + DATA.id + '_wrapper select.filter-input-prepend, #' + DATA.id + '_extfilter select.filter-input-prepend').each(function(index, element) {
            let $this = $(this);
            if ($this.hasClass("selectpickerbinded")) return;
            $this.addClass("selectpickerbinded");

            //console.log("Setting selectpicker ", this);
            $this.selectpicker(DT_SELECTPICKER_OPTS_NOSEARCH);
        });

        $('#' + DATA.id + '_wrapper .datepicker, #' + DATA.id + '_extfilter .datepicker').each(function (key, dateInput) {
            let $this = $(dateInput);
            if ($this.hasClass("datepickerbinded")) return;
            $this.addClass("datepickerbinded");

            $this.on("change", function() {
                if ($this.val() != "") $this.addClass("has-value");
                else $this.removeClass("has-value");

                TABLE.columns.adjust();
                TABLE.fixedHeader.adjust();
            });

            //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
            new $.fn.dataTable.Editor.DateTime($this, {
                format: 'L',
                momentLocale: window.userLng,
                locale: window.userLng,
                keyInput: false,
                i18n: EDITOR.i18n.datetime,
                onChange: function() {
                    $this.trigger("change");
                }
            });
        });

        $('#' + DATA.id + '_wrapper .datetimepicker, #' + DATA.id + '_extfilter .datetimepicker').each(function (key, dateInput) {
            let $this = $(dateInput);
            if ($this.hasClass("datepickerbinded")) return;
            $this.addClass("datepickerbinded");

            $this.on("change", function() {
                //to prevent UI change with clicking on filter button
                setTimeout(()=> {
                    if ($this.val() != "") $this.addClass("has-value");
                    else $this.removeClass("has-value");

                    TABLE.columns.adjust();
                    TABLE.fixedHeader.adjust();
                }, 100);
            });

            //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
            new $.fn.dataTable.Editor.DateTime($this, {
                format: 'L HH:mm',
                momentLocale: window.userLng,
                locale: window.userLng,
                keyInput: false,
                i18n: EDITOR.i18n.datetime,
                onChange: function() {
                    $this.trigger("change");
                }
            });
        });

        $('.timehmpicker').each(function (key, dateInput) {
            let $this = $(dateInput);
            if ($this.hasClass("datepickerbinded")) return;
            $this.addClass("datepickerbinded");

            $this.on("change", function() {
                if ($this.val() != "") $this.addClass("has-value");
                else $this.removeClass("has-value");

                TABLE.columns.adjust();
                TABLE.fixedHeader.adjust();
            });

            //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
            new $.fn.dataTable.Editor.DateTime($this, {
                format: 'HH:mm',
                momentLocale: window.userLng,
                locale: window.userLng,
                keyInput: false,
                i18n: EDITOR.i18n.datetime,
                onChange: function() {
                    $this.trigger("change");
                }
            });
        });

        $('.timehmspicker').each(function (key, dateInput) {
            let $this = $(dateInput);
            if ($this.hasClass("datepickerbinded")) return;
            $this.addClass("datepickerbinded");

            $this.on("change", function() {
                if ($this.val() != "") $this.addClass("has-value");
                else $this.removeClass("has-value");

                TABLE.columns.adjust();
                TABLE.fixedHeader.adjust();
            });

            //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
            new $.fn.dataTable.Editor.DateTime($this, {
                format: 'HH:mm:ss',
                momentLocale: window.userLng,
                locale: window.userLng,
                keyInput: false,
                i18n: EDITOR.i18n.datetime,
                onChange: function() {
                    $this.trigger("change");
                }
            });
        });

        TABLE.columns.adjust();
        TABLE.fixedHeader.adjust();
    }

    function updateOptionsFromJson(json) {
        //console.log("updateOptionsFromJson, json=", json);
        //spracuj prichadzajuce options
        if (json.hasOwnProperty("options")) {
            for (var fieldNameList in json.options) {
                //je to az vo for, lebo niekedy options prislo prazdne a zle sa prepisalo
                DATA.jsonOptions = json.options;
                //console.log("Updating jsonOptions v2=", DATA.jsonOptions);

                //fieldNameList moze mat hodnotu menuDocId,rightMenuDocId, nastavime na oba fieldy rovnaky zoznam
                var fieldNameArr = fieldNameList.split(",");
                for (var i = 0; i < fieldNameArr.length; i++) {
                    var fieldName = fieldNameArr[i];
                    var options = json.options[fieldNameList];
                    //console.log("Updating options 2, fieldName=", fieldName, " values=", options);

                    //zmen String true/false hodnoty na realne true/false
                    fixOptionsValueType(options);

                    //aktualizuj DT
                    for (var j = 0; j < DATA.columns.length; j++) {
                        if (DATA.columns[j].data === fieldName) {
                            DATA.columns[j].editor.options = options;
                            break;
                        }
                    }
                    //aktualizuj DT editor
                    try {
                        TABLE.EDITOR.field(fieldName).update(options);
                    } catch (e) {
                        //asi dany field v editore neexistuje
                        //console.log(e);
                    }
                    //aktualizuj select box v hlavicke
                    updateFilterSelect(fieldName);
                }
            }

            //console.log("Fixing options width")
            //fixni sirky stlpcov ked sa zmenia sirky optionov
            setTimeout(function () {
                TABLE.columns.adjust();
            }, 100);
        }

        if (DATA.customFieldsUpdateColumns===true && json.length>0) {
            let fieldsDefinition = json[0]?.editorFields?.fieldsDefinition;
            if (typeof fieldsDefinition != "undefined") {
                //je to zoznam nazvov volnych poli
                let fieldName, column, dataColumn;
                let isChange = false;
                for (var customField of fieldsDefinition) {
                    //podla null textu filtrujeme aj zoznam dostupnych stlpcov v nastaveni
                    if (customField.label==null) customField.label = "null";

                    //console.log("customField=", customField);
                    fieldName = "field"+customField.key.toUpperCase();
                    if (customField.customPrefix!=null) fieldName = customField.customPrefix+customField.key.toUpperCase();

                    column = TABLE.column(fieldName+":name");
                    var currentText = $(column.header()).text();
                    if (currentText === customField.label) continue;

                    isChange = true;

                    //console.log("column.header()=", $(column.header()).text(), "new=", customField.label);
                    $(column.header()).text(""+customField.label);
                    if (customField.label===null || customField.label=="null") {
                        if (column.header().className.indexOf("dt-format-date")!=-1) {
                            //date field muset be hidden in setTimeout to initialize datepicker
                            setTimeout(function(mycolumn) {
                                mycolumn.visible(false);
                            }, 30, column);
                        } else {
                            column.visible(false);
                        }
                    }
                    else {
                        if (DATA.customFieldsUpdateColumnsPreserveVisibility===true) {
                            //preserve column visibility set by user
                        } else {
                            if (customField.disabled===true) column.visible(false);
                            else column.visible(true);
                        }
                    }

                    dataColumn = TABLE.DATA.columns[column.index()];
                    if (typeof dataColumn != "undefined" && dataColumn.name==fieldName) {
                        dataColumn.title = customField.label;
                        dataColumn.sTitle = customField.label;
                        //console.log("Setting title: ", fieldName, " ", customField.label, "dataColumn=", dataColumn);
                    }
                    for (var editorField of TABLE.DATA.fields) {
                        if (fieldName == editorField.name) {
                            editorField.label = customField.label;
                        }
                    }
                }
                if (isChange) $("#"+DATA.id).trigger("column-reorder.dt");
            }
        }
    }

    /**
     * Overi, ci ma pouzivatel prava na zadanu funkciu
     * @param {} action - create, edit, duplicate, remove
     * @returns
     */
    function hasPermission(action) {
        //console.log("hasPermission, action=", action, "DATA.perms=", DATA.perms, "TABLE.data=", this);
        if (DATA.perms == null) return true;
        //console.log("permsName=", DATA.perms[action]);
        if (typeof DATA.perms[action] === "undefined") return true;

        //pri testovani backendu potrebujeme zobrazit tlacidla, aby sa dala DT akoze editovat, ale backend ma vyhlasit chybu
        //riesi sa to pridanim ?removePerm=XXX,forceShowButton ktore sposobi, ze tlacidla natvrdo zobrazim
        if (window.currentUser.login.indexOf("tester")==0) {
            if (WJ.hasPermission("forceShowButton")===false) return true;
        }

        //console.log("WJ.hasPermission["+action+"]=", WJ.hasPermission(DATA.perms[action]), "perms=", DATA.perms[action]);
        //v DATA.perms[action] je meno prava, ktore mame pre tuto akciu kontrolovat
        if (WJ.hasPermission(DATA.perms[action])) return true;

        return false;
    }

    /**
     * Vypocita velkost stranky (zobrazeny pocet zaznamov)
     * @param {*} updateLengthSelect - ak je true aj sa reloadnu udaje (napr. pri zmene velkosti obrazka v galerii)
     */
    function calculateAutoPageLength(updateLengthSelect) {
        var pageLength = DATA.autoPageLength;
        var pageLengthTitle = DATA.autoPageLengthTitle;
        //console.log("height=", $("#"+DATA.id+"_wrapper div.dataTables_scroll").height(), "width=", $("#"+DATA.id+"_wrapper div.dataTables_scroll").width(), "id=", DATA.id, "window.height=", $(window).height(), "nested=", DATA.nestedModal);
        if (false === DATA.nestedModal) {
            //urci dynamicky pocet zaznamov
            var height = $(window).height();

            var headerHeight = $("div.ly-header").outerHeight();
            var breadcrumbHeight = $("div.md-breadcrumb").outerHeight();
            if ($("html").hasClass("in-iframe-show-table")) breadcrumbHeight = -18; //finta aby sa nam tam standardne vosiel o riadok viac, 18 kvoli galerii
            var dtToolbarRowHeight = 60;
            var dtScrollHeadHeight = 66;
            var dtFooterHeight = 60;

            if ($(window).width()<1200) {
                headerHeight = 0;
                dtToolbarRowHeight = 50;
                dtFooterHeight = 38;
            }

            var scrollbarWidth = dtWJ.getScrollbarWidth();

            height = height - headerHeight - breadcrumbHeight - dtToolbarRowHeight - dtScrollHeadHeight - dtFooterHeight - scrollbarWidth;
            //console.log("Computed height: ", height, "scrollbarWidth=", scrollbarWidth, "breadcrumbHeight=", breadcrumbHeight);

            pageLength = Math.floor(height / 41);
            //console.log("Computed pageLength=", pageLength);

            //zaokruhli na 5
            //pageLength = Math.ceil(pageLength/5)*5;
            //console.log("Rounded pageLength=", pageLength);

            if (pageLength < 6) pageLength = 6;
            if (pageLength > 25) pageLength = 25;

            //in inner table always set pageLength to 25 to better performance with paging search
            if (window.location.href.indexOf("showOnlyEditor=true")!=-1) pageLength = 25;

            if ("galleryTable"===DATA.id) {
                var $galeryTable = $("#galleryTable");

                if ($galeryTable.hasClass("cardView")) {
                    //vypocitaj pocet riadkov a stlpcov
                    var imgWidth = 150;
                    var imgHeight = 100;

                    if ($galeryTable.hasClass("cardViewM")) {
                        imgWidth = 225;
                        imgHeight = 150;
                    } else if ($galeryTable.hasClass("cardViewL")) {
                        imgWidth = 300;
                        imgHeight = 200;
                    }

                    imgWidth = 6 + imgWidth + 6;
                    imgHeight = 6 + 24 + imgHeight + 6;

                    var colWidth = 15 + $("#"+DATA.id).width() + 15;
                    var columns = Math.floor(colWidth / imgWidth);
                    var rows = Math.floor((height + dtScrollHeadHeight) / imgHeight);
                    //console.log("colWidth=", colWidth, "imgWidth=", imgWidth);

                    pageLength = columns * rows;
                }
            }

            pageLengthTitle = WJ.translate("datatables.pageLength.auto.js", pageLength);

            DATA.autoPageLength = pageLength;
            DATA.autoPageLengthTitle = pageLengthTitle;

            if (updateLengthSelect) {
                //console.log("Setting new page size: ", pageLength, " - ", pageLengthTitle);
                //tabulka uz je vykonstruovana, aktualizuj moznosti v select boxe
                TABLE.settings()[0].aLengthMenu[0][0]=pageLength;
                TABLE.settings()[0].aLengthMenu[1][0]=pageLengthTitle;
                TABLE.page.len(pageLength).draw();
            }
        }
    }

    if ($(dataTableSelector).length > 0) {

        var dataTableInit = $(dataTableSelector);
        dataTableInit.attr("data-server-side", DATA.serverSide);

        $.each(DATA.columns, function (key, col) {

            //console.log("col=", col, " hiddenEditor=", col.hiddenEditor);

            //over zobrazenie stlpca v editore
            var hiddenEditor = false;
            if (typeof col.hiddenEditor !== "undefined") hiddenEditor = col.hiddenEditor;

            if (typeof col.editor !== "undefined" && hiddenEditor === false) {
                col.editor['name'] = col.name;
                col.editor['data'] = col.data;
                col.editor['label'] = col.editor.label || col.title;
                col.editor.className = col.className;
                col.editor.renderFormat = col.renderFormat;
                col.editor.array = col.array;


                if ("datetime" === col.editor.type || "date" === col.editor.type ||  "timehm" === col.editor.type || "timehms" === col.editor.type) {
                    let defaultFormat = "L HH:mm:ss";
                    if ("date" === col.editor.type) defaultFormat = "L";
                    if ("timehm" === col.editor.type) defaultFormat = "HH:mm";
                    if ("timehms" === col.editor.type) defaultFormat = "HH:mm:ss";
                    col.editor.type = "datetime"; //musime nastavit takto, aby sa date spravalo rovnako ako datetime len malo iny format
                    col.editor.format = col.editor.format || defaultFormat;
                    col.editor.displayFormat = col.editor.format;
                    col.editor.wireFormat = col.editor.wireFormat || "x";
                    if (col.editor.hasOwnProperty("opts") === false) {
                        //locale potrebuje nastavit DTED DateTime trieda, super, ze to nepreberaju z momentLocale...
                        //stalo to 3 hodiny debugovania
                        col.editor.opts = {momentLocale: window.userLng, locale: window.userLng};
                    } else {
                        col.editor.opts.momentLocale = col.editor.opts.momentLocale || window.userLng;
                        col.editor.opts.locale = col.editor.opts.momentLocale;
                    }
                    //console.log("setting datetimeformat, col=", col, "defaultFormat=", defaultFormat);
                }

                if ("checkbox" === col.editor.type) {
                    //console.log("Checkbox, options=", col.editor.options);

                    if (col.editor.hasOwnProperty("options") === false) {
                        col.editor.options = [
                            {label: WJ.translate('button.yes'), value: true}
                        ];
                    }

                    fixOptionsValueType(col.editor.options);

                    if (col.editor.hasOwnProperty("falseValue") === false) {
                        col.editor.falseValue = WJ.translate('button.no');
                    }
                    if (col.editor.hasOwnProperty("attr") && col.editor.attr.hasOwnProperty("unselectedValue")) {
                        col.editor.unselectedValue = col.editor.attr.unselectedValue;
                    }
                    if (col.editor.hasOwnProperty("unselectedValue") === false) {
                        col.editor.unselectedValue = false;
                    }
                    if ("dt-format-boolean-true" === col.renderFormat) {
                        //ked dame DT editoru stringovu hodnotu bude data posielat ako pole a nie ako bezny true/false, musime to teda takto upravit
                        if (col.editor.options && col.editor.options.length > 0 && col.editor.options[0].value === "true") col.editor.options[0].value = true;
                    }
                    //console.log("checkbox: ", col.editor.options);
                }

                if ("select" === col.editor.type || "radio" === col.editor.type) {
                    //ak mame true/false select box, aj tomu musime zmenit value na true/false namiesto String hodnot
                    if (col.editor.options) {
                        fixOptionsValueType(col.editor.options);
                    }
                }

                //def hodnota chodi ako String, pre true/false zmen na boolean
                if ("true" === col.editor.def) col.editor.def = true;
                else if ("false" === col.editor.def) col.editor.def = false;

                if (col.editor.hasOwnProperty("attr") && col.editor.attr.hasOwnProperty("entityDecode")) {
                    col.editor.entityDecode = ("true"===col.editor.attr.entityDecode);
                }

                DATA.fields.push(col.editor);
            }

        });

        //odfiltruj stlpce, ktore nemaju byt v datatabulke
        DATA.columns = filterColumnsByHidden(DATA.columns);
        //console.log("COLUMNS=", DATA.columns);

        var ths = "";
        $.each(DATA.columns, function (key, col) {

            var linkTemplate = "";
            if (typeof col.renderFormatLinkTemplate !== "undefined") {
                linkTemplate = col.renderFormatLinkTemplate;
            }
            var fieldName = col.name.replace(/\./gi, "-");
            ths += `<th class="${col.renderFormat} dt-th-${fieldName}" data-dt-field-name="${fieldName}" data-format-link-template="${linkTemplate}">${col.title}</th>`;
        });

        $(dataTableInit).html("").append("<thead><tr>" + ths + "</tr></thead>");

        $.extend(true, $.fn.dataTable.Editor.classes, {
            "header": {
                "wrapper": "DTE_Header modal-header",
                title: {
                    tag: '',
                    class: ''
                }
            },
            "body": {
                "wrapper": "DTE_Body modal-body"
            },
            "footer": {
                "wrapper": "DTE_Footer modal-footer"
            },
            "field": {
                "wrapper": "DTE_Field form-group row",
                "label": "col-sm-4 col-form-label",
                "input": "col-sm-7"
            }
        });

        /**
         * There was BUG in DTE - for datefields it returns timestamp converted to UTC,
         * but it should return timestamp in local timezone
         * @param {*} conf
         * @returns
         */
        $.fn.dataTable.Editor.fieldTypes.datetime.get = function (conf) {
            /*var ret = conf.wireFormat
                ? conf._picker.valFormat(conf.wireFormat)
                : conf._input.val();*/

            var val = conf._input.val();
            if (! val) {
                return val;
            }

            var ret = moment(val, conf.format).format(conf.wireFormat || "x");
            return ret;
        }

        if (typeof $.fn.dataTable.Editor.fieldTypes.select.wjfixed == "undefined") {
            $.fn.dataTable.Editor.fieldTypes.select.wjfixed = true;
            var selectSetOriginalFunction = $.fn.dataTable.Editor.fieldTypes.select.set;
            $.fn.dataTable.Editor.fieldTypes.select.set = function (conf, val, localUpdate) {
                //console.log("select.set, conf=", conf, "val=", val, "typeof val=", typeof val, "localUpdate=", localUpdate);
                //console.log("has class=", conf._input.parents("div.modal.DTED").hasClass("show"));
                if (typeof val === "number" && val != 0) {
                    //check options if it's there
                    var found = false;
                    for (var option of conf._input[0].options) {
                        if (val === parseInt(option.value)) {
                            found = true;
                            break;
                        }
                    }
                    if (found === false) {
                        //console.log("select.set NOT FOUND, conf=", conf, "val=", val, "typeof val=", typeof val, "localUpdate=", localUpdate);
                        var options = [
                            {
                                label: "id: "+val,
                                value: ""+val
                            }
                        ];
                        $.fn.dataTable.Editor.fieldTypes.select._addOptions(conf, options, true);
                        //by timeout, because set function is also called during paging
                        setTimeout(()=>{
                            if (conf._input.parents("div.modal.DTED").hasClass("show")) {
                                WJ.notifyWarning(
                                    WJ.translate("datatable.select.error.js", conf.data),
                                    WJ.translate("datatable.select.options-missing.js", conf.label, conf.data, val),
                                    15000
                                );
                            }
                        }, 1000);
                    }
                }
                selectSetOriginalFunction(conf, val, localUpdate);
            }
        }

        if (typeof $.fn.dataTable.Editor.fieldTypes.text.wjfixed == "undefined") {
            $.fn.dataTable.Editor.fieldTypes.text.wjfixed = true;
            var textSetOriginalFunction = $.fn.dataTable.Editor.fieldTypes.text.set;
            var textGetOriginalFunction = $.fn.dataTable.Editor.fieldTypes.text.get;
            $.fn.dataTable.Editor.fieldTypes.text.set = function (conf, val) {
                try {
                    if (conf?.attr?.["data-dt-escape-slash"]==="true") {
                        val = val.replaceAll("&#47;", "/");
                    }
                } catch (e) {
                    //console.log(e);
                }
                textSetOriginalFunction(conf, val);
            }
            $.fn.dataTable.Editor.fieldTypes.text.get = function (conf) {
                var val = textGetOriginalFunction(conf);
                try {
                    if (conf?.attr?.["data-dt-escape-slash"]==="true") {
                        val = val.replaceAll("/", "&#47;");
                        val = val.replaceAll("&#47;>", "/>");
                    }
                } catch (e) {
                    //console.log(e);
                }
                return val;
            }
        }

        /**
         * Upraveny BS open, pri DRUHOM volani uz len zobrazime existujuci dialog
         */
        $.fn.dataTable.Editor.display.bootstrap.open = function ( dte, append, callback ) {

            //WebJET ZMENY START
            //console.log("Open, dte=", dte, "mode=", dte.mode());

            if (typeof dte._bootstrapDisplay != "undefined") {
                //okno uz bolo raz otvorene, len ho teraz znova showni
                $("#" + dte._bootstrapDisplay.id).modal("show");
                //firni event
                WJ.dispatchEvent('WJ.DTE.open', {
                    dte: dte
                });
                setTimeout(()=> {
                    WJ.dispatchEvent('WJ.DTE.opened', {
                        dte: dte,
                        id: dte.TABLE.DATA.id,
                        action: dte.mode()
                    });
                }, 100);
                return;
            }

            let modal;
            let _bs = window.bootstrap;
            let shown = false;
            let fullyShown = false;

            let nestedModalClass = "";
            if (dte.TABLE.DATA.nestedModal) {
                nestedModalClass = "DTE_nested_modal"
            }

            //console.log("1, DATA.id=", dte.TABLE.DATA.id);
            let dom = {
                content: $(
                '<div id="' + dte.TABLE.DATA.id + '_modal" class="modal fade DTED ' + nestedModalClass + '">' +
                    '<div class="modal-dialog modal-dialog-scrollable" />' +
                '</div>'
                ),
                close: $('<button class="close btn-close-editor" data-toggle="tooltip"><i class="far fa-times-circle"></i>')
            }
            dom.close.on('click', function () {
                dte.close('icon');
            });
            dte._bootstrapDisplay = {
                dom: dom,
                id: dte.TABLE.DATA.id + "_modal"
            }
            //WebJET ZMENY KONIEC

            if (! modal) {
                modal = new _bs.Modal(dom.content[0], {
                    backdrop: "static",
                    keyboard: false,
                    //toto musi byt false, inak nejde pisat text do inputov ckeditor dialogov, 2 hodiny debugovania...
                    focus: false
                });
            }

            $(append).addClass('modal-content');

            // Special class for DataTable buttons in the form
            $(append).find('div.DTE_Field_Type_datatable div.dt-buttons')
                .removeClass('btn-group')
                .addClass('btn-group-vertical');

            // Setup events on each show
            dom.close
                .attr('title', dte.i18n.close)
                .off('click.dte-bs5')
                .on('click.dte-bs5', function () {
                    dte.close('icon');
                })
                .appendTo($('div.modal-header', append));

            // This is a bit horrible, but if you mousedown and then drag out of the modal container, we don't
            // want to trigger a background action.
            let allowBackgroundClick = false;
            $(document)
                .off('mousedown.dte-bs5')
                .on('mousedown.dte-bs5', 'div.modal', function (e) {
                    allowBackgroundClick = $(e.target).hasClass('modal') && shown
                        ? true
                        : false;
                } );

            $(document)
                .off('click.dte-bs5')
                .on('click.dte-bs5', 'div.modal', function (e) {
                    if ( $(e.target).hasClass('modal') && allowBackgroundClick ) {
                        dte.background();
                    }
                } );

            var content = dom.content.find('div.modal-dialog');
            content.children().detach();
            content.append( append );

            if ( shown ) {
                if ( callback ) {
                    callback();
                }
                return;
            }

            shown = true;
            fullyShown = false;

            $(dom.content)
                .one('shown.bs.modal', function () {
                    // Can only give elements focus when shown
                    if ( dte.s.setFocus ) {
                        dte.s.setFocus.focus();
                    }

                    fullyShown = true;

                    if ( callback ) {
                        callback();
                    }
                })
                .one('hidden', function () {
                    shown = false;
                })
                .appendTo( 'body' );

            modal.show();

            //firni event
            WJ.dispatchEvent('WJ.DTE.open', {
                dte: dte
            });
        }

        /**
         * Kvoli ckeditoru nemozeme detachovat bootstrap dialog DTED, prepiseme teda standardny close
         * na jednoduche schovanie DTED dialogu
         */
        $.fn.dataTable.Editor.display.bootstrap.close = function (dte, callback) {
            //console.log("DT CLOSE, id=", "#" + dte._bootstrapDisplay.id, "callback=", callback);
            //console.log("Calling custom close on DTED, dte=", dte);
            //console.log("Hiding modal: ", dte._bootstrapDisplay.id);
            $("#" + dte._bootstrapDisplay.id).modal("hide");

            //zmaz titulok typu Naozaj chcete zmazat polozku
            $("#" + dte._bootstrapDisplay.id + " div.DTE_Form_Info").html("");
            //zmaz stare error hlasenia
            $("#" + dte._bootstrapDisplay.id + " div.DTE_Field div.text-danger").text("");
            //zmaz stare info hlasenia
            $("#" + dte._bootstrapDisplay.id + " div.DTE_Field div[data-dte-e=msg-info]").text("");
            //vymaz poslednu error hlasku v spodnej casti
            $('#' + DATA.id + '_modal .DTE_Form_Error').html("");

            //toto sa deje pri vynutenom close, kvoli bugu nested dialogu, vtedy posielame null
            //if (typeof callback == "undefined" || callback == null) return;

            //firni event
            WJ.dispatchEvent('WJ.DTE.close', {
                dte: dte
            });
        }

        //funkcia na setnutie hodnot do editora podla zadaneho JSON objektu
        $.fn.dataTable.Editor.prototype.setJson = function (json) {
            //console.log("Editor.setJson, json=", json, " this=", this);
            this.currentJson = json;

            const resolvePath = (object, path, defaultValue) => path
                .split('.')
                .reduce((o, p) => o ? o[p] : defaultValue, object);

            const fields = this.order();
            for (let i = 0; i < fields.length; i++) {
                const value = resolvePath(json, fields[i], "");
                //console.log("Setting val for=", fields[i], " val=", val);
                this.val(fields[i], value);
            }
        }

        //fix prace s datumami, DTED z nepochopitelnych dovodov pouzije moment len pre string hodnotu
        let originalDateTimeSetFunction = $.fn.dataTable.Editor.fieldTypes.datetime.set;
        $.fn.dataTable.Editor.fieldTypes.datetime.set = function (conf, val) {
            val = ""+val;
            //console.log("Fixed typeof val=", typeof val, " val=", val);
            originalDateTimeSetFunction(conf, val);
        }



        //datovy typ JSON a Datatable
        //console.log("Idem inicializovat JSON, TABLE=", TABLE, " DATA=", DATA);
        $.fn.dataTable.Editor.fieldTypes.json = fieldTypeJson.typeJson();
        $.fn.dataTable.Editor.fieldTypes.datatable = fieldTypeDatatable.typeDatatable();
        $.fn.dataTable.Editor.fieldTypes.elfinder = fieldTypeElfinder.typeElfinder();
        $.fn.dataTable.Editor.fieldTypes.quill = fieldTypeQuill.typeQuill();
        $.fn.dataTable.Editor.fieldTypes.wysiwyg = fieldTypeWysiwyg.typeWysiwyg();
        $.fn.dataTable.Editor.fieldTypes.jsTree = fieldTypeJsTree.typeJsTree();
        $.fn.dataTable.Editor.fieldTypes.attrs = fieldTypeAttrs.typeAttrs();

        fieldTypeSelectEditable.typeSelectEditable();

        //editRefresh button
        $.fn.dataTable.ext.buttons.editRefresh = {
            extend: 'edit',
            text: WJ.translate('button.edit'),
            action: function (e, dt, node, config) {
                this.processing(true);

                // Get currently selected row ids
                const selectedRows = dt.rows({selected: true}).ids();
                //console.log("selectedRows=", selectedRows);
                const that = this;
                const rowsLength = selectedRows.length;

                for (let i=0; i<rowsLength; i++) {
                    refreshRow(selectedRows[i], function (json) {
                        //console.log("fetched, i=", i, "json=", json);
                        if ((i+1)>=rowsLength) {
                            //console.log("Finished, opening editor");
                            that.processing(false);
                            const event = new CustomEvent('WJ.DTE.xhrfetch', {
                                detail: {
                                    json: json
                                }
                            });
                            //console.log("Dispatching event: ", event);
                            window.dispatchEvent(event);
                            setTimeout(()=> {
                                $.fn.dataTable.ext.buttons.edit.action.call(that, e, dt, node, config);
                            }, 100);
                        }
                    });
                }
            }
        };

        //funkcia pre enable/disable tlacitka iba ked je nieco selectnute
        $.fn.dataTable.Buttons.showIfRowSelected = function (button, dt) {
            dt.on('select.dt.DT deselect.dt.DT', function () {
                button.enable(dt.rows({selected: true}).any());
            });

            button.disable();
        };

        //funkcia pre enable/disable tlacitka iba ked je JEDEN RIADOK selectnuty
        $.fn.dataTable.Buttons.showIfOneRowSelected = function (button, dt) {
            dt.on('select.dt.DT deselect.dt.DT', function () {
                button.enable(dt.rows({selected: true}).ids().length == 1);
            });

            button.disable();
        };

        //vyhladavanie v poliach typu HTML (cize hladanie aj v HTML kode)
        $.fn.dataTableExt.ofnSearch['html-input'] = function (value) {
            //console.log("html-input search, value=", value);
            if (value === null) return "";
            //return $(value).val();
            return value;
        };

        //pocet poloziek v strankovani (v paticke)
        //sirku ratame podla kontajnera, lebo tabulka este nema _wrapper a ked ma vela stlpcov je siroka
        let tableWidth = $("#"+DATA.id).parents("div.ly-container.container,div.col-md-8").width();
        //console.log("tableWidth=", tableWidth, "id=", DATA.id);
        $.fn.DataTable.ext.pager.numbers_length = 10;
        if (false === DATA.nestedModal && tableWidth>800) {
            //ak to nie je vnorena tabulka a mame dost miesta nastav na 15 poloziek
            //if (tableWidth>800) $.fn.DataTable.ext.pager.numbers_length = 15;
            tableWidth = tableWidth - 250; //Záznamy 1 až 12 z 4,709
            let pages = Math.floor(tableWidth / 36); //36 je sirka pre 4 miestne cislo napr 2131
            //console.log("pages=", pages);
            if (pages > 12) {
                $.fn.DataTable.ext.pager.numbers_length = pages;
            }
        }

        // console.log("DATA.fields", DATA.fields);
        // console.log("DATA.columns", DATA.columns);

        var EDITOR = new $.fn.dataTable.Editor({
            ajax: {
                url: WJ.urlAddPath(DATA.url, '/editor'),
                contentType: 'application/json',
                data: function (d) {
                    //console.log("d=", d);
                    //[{"id":1,"groupId":39,"domainId":1,"insertScriptGrId":1},{"id":2,"groupId":25,"domainId":1,"insertScriptGrId":2}]
                    //[{"id":0,"groupId":39,"domainId":1,"insertScriptGrId":0},{"id":0,"groupId":25,"domainId":1,"insertScriptGrId":0}]
                    //["groupId":39,"domainId":1},{"groupId":25,"domainId":1}]
                    var json = JSON.stringify(d);
                    //console.log("json 1: ", json);
                    //fix na checkboxy
                    json = json.replace(/:\[true\]/gi, ":true");
                    json = json.replace(/:\[false\]/gi, ":false");
                    //console.log("json 2: ", json);
                    return json
                },
                error: function (xhr, text, err) {
                    //console.log("error, xhr=", xhr, "text=", text, "err=", err);
                    if ("timeout"===text || "abort"===text) {
                        WJ.notifyError(WJ.translate("session.logoff.info.js"), WJ.translate("datatables.error.network.js"));
                    }
                },
            },
            table: dataTableSelector,
            idSrc: DATA.editorId,
            display: "bootstrap",
            fields: DATA.fields,
            formOptions: {
                main: {
                    onEsc: false,
                    onBackground: false,
                    onReturn: false
                }
            },
            i18n: {
                close: WJ.translate("datatables.modal.close.js"),
                create: {
                    button: WJ.translate('button.add'),
                    title: '<div class="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.add') + '</h5></div><div class="col-sm-8" id="dt-header-tabs-' + DATA.id + '"></div></div>'+MAXIMIZE_HTML,
                    submit: '<i class="fal fa-check"></i> ' + WJ.translate('button.add')
                },
                edit: {
                    button: WJ.translate('button.edit'),
                    title: '<div class="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.edit') + '</h5></div><div class="col-sm-8" id="dt-header-tabs-' + DATA.id + '"></div></div>'+MAXIMIZE_HTML,
                    submit: '<i class="fal fa-check"></i> ' + WJ.translate('button.save')
                },
                remove: {
                    button: WJ.translate('button.delete'),
                    title: '<clas="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.delete') + '</h5></div><div class="col-sm-8"></div></div>',
                    submit: '<i class="fal fa-check"></i> ' + WJ.translate('button.delete'),
                    confirm: {
                        _: '<div class="form-group row"><div class="col-sm-7 offset-sm-4"><h5>' + WJ.translate('datatables.prompt.delete.files.js') + '</h5></div></div>',
                        1: '<div class="form-group row"><div class="col-sm-7 offset-sm-4"><h5>' + WJ.translate('datatables.prompt.delete.file.js') + '</h5></div></div>'
                    }
                },
                error: {
                    system: WJ.translate('datatables.error.system.js')
                },
                datetime: {
                    previous: WJ.translate('datatables.button.prev.js'),
                    next: WJ.translate('datatables.button.next.js'),
                    months: [
                        WJ.translate('component.calendar.month.1'),
                        WJ.translate('component.calendar.month.2'),
                        WJ.translate('component.calendar.month.3'),
                        WJ.translate('component.calendar.month.4'),
                        WJ.translate('component.calendar.month.5'),
                        WJ.translate('component.calendar.month.6'),
                        WJ.translate('component.calendar.month.7'),
                        WJ.translate('component.calendar.month.8'),
                        WJ.translate('component.calendar.month.9'),
                        WJ.translate('component.calendar.month.10'),
                        WJ.translate('component.calendar.month.11'),
                        WJ.translate('component.calendar.month.12'),
                    ],
                    weekdays: [
                        WJ.translate('datatables.calendar.weekday.short.1.js'),
                        WJ.translate('datatables.calendar.weekday.short.2.js'),
                        WJ.translate('datatables.calendar.weekday.short.3.js'),
                        WJ.translate('datatables.calendar.weekday.short.4.js'),
                        WJ.translate('datatables.calendar.weekday.short.5.js'),
                        WJ.translate('datatables.calendar.weekday.short.6.js'),
                        WJ.translate('datatables.calendar.weekday.short.7.js'),
                    ],
                    hours:    WJ.translate('datatables.calendar.hours.js'),
                    minutes:  WJ.translate('datatables.calendar.minutes.js'),
                    seconds:  WJ.translate('datatables.calendar.seconds.js'),
                    today:    WJ.translate('datatables.calendar.today.js')
                },
                multi: {
                    title: '<i class="far fa-pencil"></i> ' + WJ.translate('datatables.values.edit.title.js'),
                    info: WJ.translate('datatables.values.edit.info.js'),
                    restore: '<i class="far fa-redo"></i> ' + WJ.translate('datatables.values.edit.restore.js')
                }
            }
        })

        EDITOR.on('preSubmit', function (e, data, action) {

            const me = this;

            // upravi multiple volne polia
            prepareCustomFieldsDataBeforeSend(data)

            /*if (action !== 'remove') {

                $.each($(e.target.dom.wrapper).find("[data-dt-validation]"), function (k, v) {
                    const name = $(v).attr("id").replace("DTE_Field_", "");

                    const field = me.field(name);

                    if (!field.val()) {
                        console.log("Mam error, name=", name, "field=", field);
                        //TODO -  spravit povinnu kontrolu
                        field.error(
                            WJ.translate('datatables.field.required.error.js')
                        );
                    }
                });

                // If any error was reported, cancel the submission so it can be corrected
                if (me.inError()) {
                    console.log("Nasiel som chybu: me=", me);
                    return false;
                }
            }*/
        });

        EDITOR.on('opened', function (e, type, action) {
            setTimeout(()=> {
                WJ.dispatchEvent('WJ.DTE.opened', {
                    dte: EDITOR,
                    id: DATA.id,
                    action: action
                });
            }, 100);
        });
        EDITOR.on('submit', function () {
            TABLE.columns.adjust();
            TABLE.fixedHeader.adjust();
            const eventSubmit = new CustomEvent('WJ.DTE.submitclick', {
                detail: {
                    element: this,
                    action: 'submit'
                }
            });
            window.dispatchEvent(eventSubmit);
        });
        EDITOR.on('close', function () {
            //console.log("Editor.on close, editor=", EDITOR, "url=", EDITOR.TABLE.DATA.url, "close=", EDITOR.close);

            //pre istotu zatvor, niekedy pri nested dialogu zostava otvoreny kvoli nejakemu bugu
            if (typeof EDITOR._bootstrapDisplay != "undefined" && $("#" + EDITOR._bootstrapDisplay.id).hasClass("show")) {
                //console.log("IS NOT CLOSED, CLOSING MANUALLY");
                $.fn.dataTable.Editor.display.bootstrap.close(EDITOR, null);
            }

            TABLE.columns.adjust();
            TABLE.fixedHeader.adjust();
            const eventClose = new CustomEvent('WJ.DTE.closeclick', {
                detail: {
                    element: this,
                    action: 'close'
                }
            });
            window.dispatchEvent(eventClose);
        });
        EDITOR.on('submitSuccess', function (e, json, data, action) {
            //console.log("Editor.on submitSuccess, json=", json);
            setTimeout(function() {
                if (json.forceReload === true) {
                    //serverSide is reloading by datatable directly
                    if (EDITOR.TABLE.DATA.serverSide===false) TABLE.ajax.reload();

                    //publishni reload event, napr. pre jstree
                    const eventReload = new CustomEvent('WJ.DTE.forceReload', {
                        detail: {
                            e: e,
                            json: json,
                            data: data,
                            action: action,
                            EDITOR: EDITOR,
                            TABLE: TABLE
                        }
                    });
                    window.dispatchEvent(eventReload);
                }

                if(typeof json.notify != "undefined" && json.notify != null) {
                    showNotify(json.notify);
                }
            }, 300);
        });

        if (DATA.fetchOnCreate) {
            //ak je zapnute volanie servera pre novy zaznam zavolame ajax a pockame na data pre novy zaznam (napr. pre web stranku)
            EDITOR.on('initCreate', function (e) {
                return new Promise(function (resolve, reject) {
                    let url = TABLE.ajax.url();
                    const q = url.indexOf("?");
                    if (q === -1) url = url + "/-1";
                    else url = url.substring(0, q) + "/-1" + url.substring(q);

                    $.ajax({
                        url: url,
                        success: function (json) {
                            //console.log("EDITOR=", EDITOR, " json=", json);
                            //EDITOR.vals( json );
                            EDITOR.setJson(json);
                            resolve(); // Editor continues after this
                        }
                    });
                });
            });
        }

        EDITOR.on('initEdit', function (e, node, data, items, type) {
            if (typeof data === "undefined") {
                console.log("ERROR: data is undefined, data=", data, "items=", items);
                return;
            }

            console.log("EDITOR initEdit"); //DO NOT DELETE otherwise tests will fails (not a joke)
            EDITOR.currentJson = data;
            //console.log("Title=", data.title);
        });

        let editorWasOpened = false;
        let editorHasTabs = false;

        EDITOR.on('open', function (e, mode, action) {

            //console.log("EDITOR OPEN, mode=", mode, "editorWasOpened=", editorWasOpened, "e=", e, "action=", action);

            $("#" + DATA.id + "_modal div.DTE_Body.modal-body").removeClass("has-content");

            if (mode === 'main') {

                //create tabs html
                //console.log("tabsFolders", DATA.tabsFolders);

                if (DATA.tabsFolders && DATA.tabsFolders.length > 0) {
                    //console.log("tabsFolders 1", DATA.tabsFolders);
                    editorHasTabs = true;

                    //taby
                    let tabsHtml = "";
                    tabsHtml += '<div class="md-breadcrumb">';
                    tabsHtml += '<ul class="nav" id="pills-dt-editor-' + DATA.id + '" role="tablist">';
                    for (let i = 0; i < DATA.tabsFolders.length; i++) {
                        var tab = DATA.tabsFolders[i];

                        if (typeof tab.perms != "undefined") {
                            //skontroluj prava
                            let tabPerm = nopermsJavascript[tab.perms];
                            //console.log("tab.perms=", tab.perms, "noperms=", tabPerm);
                            if (true === tabPerm) continue;
                        }

                        if (true === tab.hideOnCreate && "create"==action) continue;
                        if (true === tab.hideOnEdit && "edit"==action) continue;

                        var hasContent = tab.hasOwnProperty("content");
                        var classNameAppend = "";
                        if (typeof tab.className != "undefined") classNameAppend = " "+tab.className;
                        tabsHtml += '<li class="nav-item' + classNameAppend + '">';
                        tabsHtml += '<a class="nav-link' + (tab.selected ? ' active' : '') + '" data-has-content="' + hasContent + '" data-tab-id="' + DATA.id + '-' + tab.id + '" id="pills-dt-' + DATA.id + '-' + tab.id + '-tab" href="#pills-dt-' + DATA.id + '-' + tab.id + '" aria-selected="' + tab.selected + '" aria-controls="pills-dt-' + DATA.id + '-' + tab.id + '" data-toogle="pill" role="tab">' + tab.title + '</a>';
                        tabsHtml += '</li>';
                    }
                    tabsHtml += '</ul>';
                    tabsHtml += '</div>';
                    //toto musime setnut vzdy, pretoze sa meni HTML kod hlavicky
                    $('#dt-header-tabs-' + DATA.id).html(tabsHtml);
                    setTimeout(function () {
                        $('#pills-dt-editor-' + DATA.id + ' a').on('click', function (e) {
                            e.preventDefault();

                            //manualne nastav taby, kedze contentove su mimo standardneho priestoru a nesetne sa to spravne
                            $('#' + DATA.id + '_modal div.dte-tab-pane').removeClass("active show");
                            $(this).removeClass("active");

                            $(this).tab('show');

                            //pre taby kde mame custom content nastav CSS triedu
                            const hasContent = $(this).data("has-content");
                            if (hasContent) $('#' + DATA.id + '_modal div.DTE_Body.modal-body').addClass("has-content");
                            else $('#' + DATA.id + '_modal div.DTE_Body.modal-body').removeClass("has-content");

                            //firni event
                            const event = new CustomEvent('WJ.DTE.tabclick', {
                                detail: {
                                    element: this,
                                    id: $(this).data("tab-id")
                                }
                            });

                            window.dispatchEvent(event);
                        })
                    }, 100);

                    //ak je content na prvom tabe musime aj setnut dialogu has-content
                    if (DATA.tabsFolders[0].hasOwnProperty("content")) $("#" + DATA.id + "_modal div.DTE_Body.modal-body").addClass("has-content");
                    //pri reopene zobraz prvy tab
                    if (editorWasOpened) {
                        //console.log("#pills-dt-"+DATA.id+"-"+DATA.tabsFolders[0].id+"-tab");
                        setTimeout(function () {
                            //znova klikni na prvy tab
                            $("#pills-dt-" + DATA.id + "-" + DATA.tabsFolders[0].id + "-tab").trigger("click");
                        }, 100);
                    }

                    //prve otvorenie editora
                    if (editorWasOpened === false) {
                        const containerHtml = '<div class="tab-content" id="pills-dt-editor-' + DATA.id + '-Content"></div>';
                        //wrapper pre persistent taby (napr. ckeditor)
                        const containerHtmlPersistent = '<div class="tab-content" id="pills-dt-container-' + DATA.id + '-HtmlPersistent"></div>';
                        $('#' + DATA.id + '_modal div.DTE_Form_Content').append(containerHtml);
                        $('#' + DATA.id + '_modal div.DTE_Form_Content').parent().parent().parent().append(containerHtmlPersistent);

                        for (let i = 0; i < DATA.tabsFolders.length; i++) {
                            const tab = DATA.tabsFolders[i];
                            let content = "";
                            //console.log("i=",i," hasContent=",tab.hasOwnProperty("content"));
                            if (tab.hasOwnProperty("content")) {
                                content = tab.content;
                            }
                            tabsHtml = '<div id="pills-dt-' + DATA.id + '-' + tab.id + '" class="dte-tab-pane tab-pane fade' + (tab.selected ? ' show active' : '') + '" role="tabpanel" aria-labelledby="pills-dt-' + DATA.id + '-' + tab.id + '-tab">';
                            tabsHtml += '<div class="panel-body" id="panel-body-dt-' + DATA.id + '-' + tab.id + '">' + content + '</div>';
                            tabsHtml += '</div>';

                            //add html to dom
                            if (tab.hasOwnProperty("content")) $('#pills-dt-container-' + DATA.id + '-HtmlPersistent').append(tabsHtml);
                            else $('#pills-dt-editor-' + DATA.id + '-Content').append(tabsHtml);
                        }

                        //$( EDITOR.node( ['descriptionShortSk','descriptionLongSk'] )).appendTo('.tab-2');

                        //console.log("data", DATA.columns);

                        for (let i = 0; i < DATA.fields.length; i++) {
                            var data = DATA.fields[i];
                            //console.log(data);

                            //console.log("MAM TAB", data);
                            var tab = DATA.tabsFolders[0].id;
                            if (data.tab) tab = data.tab;
                            //console.log("moving node ", data.name, " to "+('#panel-body-dt-'+DATA.id+'-'+tab));
                            $(EDITOR.node(data.name)).appendTo('#panel-body-dt-' + DATA.id + '-' + tab);


                            //pre checkboxy a radio buttony DT editor nepodporuje attr, takze to nema data atributy,
                            //ktore neskor potrebujeme, musime ich manualne pridat na prvy chcekbox/radion
                            if ((data.type === "checkbox" || data.type === "radio") && data.attr) {
                                //console.log("data checkbox=", data);

                                //zial kedze DT editor zle generuje CSS triedu pre editorFields.passwordProtected (ako DTE_Field_Name_editorFields.passwordProtected)
                                //co sa neda najst musime to spravit cez workaround
                                $("div.DTE_Field_Type_" + data.type).each(function () {
                                    if ($(this).attr("class").indexOf(data.data) !== -1) {
                                        //nastav attr prvemu elementu
                                        var firstInput = $(this).find("input:first");
                                        if (firstInput.length > 0) {
                                            //prekopiruj attr
                                            $.each(data.attr, function (name, value) {
                                                //console.log("data=",data," name=",name," value=",value);
                                                //nastavujeme len data atributy
                                                if (name.startsWith("data-")) firstInput.attr(name, value);
                                            });
                                        }
                                    }
                                });
                            }

                            //prenesieme hodnotu className aj do DT editora do prislusneho riadku
                            if (data.hasOwnProperty("className")) {
                                //console.log("Setujem className, data=", data.className);
                                //zial kedze DT editor zle generuje CSS triedu pre editorFields.passwordProtected (ako DTE_Field_Name_editorFields.passwordProtected)
                                //co sa neda najst musime to spravit cez workaround
                                $("div.DTE_Field").each(function () {
                                    if ($(this).hasClass("DTE_Field_Name_" + data.data)) {
                                        //console.log("Nasiel som dany div class=", $(this).attr("class"), " data.data=", data.data, " this=", this);
                                        $(this).addClass(data.className);
                                    }
                                });
                            }
                        }

                        /**
                         * tato funkcia v DT sposobuje, ze sa detachnu vsetky elementy z div.form-content, preusporiadaju a attachnu
                         * ked ale mame taby, tak to sposobuje ich detachnutie a zrusenie content elementu, kde je napr. ckeditor
                         * ten ale nechceme reinicializovat, ale len pouzivat
                         */
                        EDITOR._displayReorder = function (includeFields) {
                            //console.log("_displayReorder v3");
                            return;
                        }
                    }
                }

                if (editorHasTabs === false) {
                    $('#' + DATA.id + '_modal div.DTE_Body.modal-body').addClass("modal-body-notabs");
                }

                //prepinanie maximalizacie okna, je to vzdy, kedze HTML kod headeru sa replacne pri kazdom otvoreni
                $('#' + DATA.id + '_modal div.DTE_Header div.maximize').on("click", function() {
                    $('#' + DATA.id + '_modal div.modal-dialog').toggleClass("modal-fullscreen");
                    dtWJ.resizeTabContent(EDITOR);
                    WJ.dispatchEvent('WJ.DTE.resize', {
                        dte: EDITOR
                    });
                });

                if (editorWasOpened === false) {
                    //vygeneruj tooltip elementy
                    $.each($('#' + DATA.id + '_modal .DTE_Form_Content').find('[data-dte-e="msg-message"]'), function (key, el) {

                        var tooltipText = $(el).html();

                        if (tooltipText.match("^data:")) {
                            var fieldName = tooltipText.replace("data:", "");

                            try {
                                tooltipText = WJ.escapeHtml(EDITOR.field(fieldName).val());
                            } catch (error) {
                                console.error("this=", this, "fieldName=", fieldName, " error:", error);
                            }

                        }

                        if (tooltipText.length > 0 && !tooltipText.match("^data:")) {
                            //console.log("Tooltiptext=", tooltipText);
                            tooltipText = WJ.parseMarkdown(tooltipText);
                            tooltipText = WJ.escapeHtml(tooltipText);
                            //console.log("Tooltiptext parsed=", tooltipText);
                            $(el).parents('[data-dte-e="input"]').after('<div class="col-sm-1 form-group-tooltip"><button type="button" tabindex="-1" class="btn btn-link btn-tooltip" data-toggle="tooltip" title="' + tooltipText + '" data-html="true"><i class="far fa-info-circle"></i></button></div>');
                        }

                        $(el).hide();
                    });
                }

                if (action === 'edit') {
                    $('#' + DATA.id + '_modal .DTE_Form_Content .disable-on-edit').each(function () {
                        $(this).find('.DTE_Field_InputControl .form-control').prop('disabled', true);
                    });
                } else {
                    $('#' + DATA.id + '_modal .DTE_Form_Content .disable-on-edit').each(function () {
                        $(this).find('.DTE_Field_InputControl .form-control').prop('disabled', false);
                    });
                }


                if (action === 'create') {
                    $('#' + DATA.id + '_modal .DTE_Form_Content .disable-on-create').each(function () {
                        $(this).find('.DTE_Field_InputControl .form-control').prop('disabled', true);
                    });
                } else {
                    $('#' + DATA.id + '_modal .DTE_Form_Content .disable-on-create').each(function () {
                        $(this).find('.DTE_Field_InputControl .form-control').prop('disabled', false);
                    });
                }

                $('#' + DATA.id + '_modal .DTE_Form_Buttons').find(".btn").addClass("btn-primary");
                $('#' + DATA.id + '_modal .DTE_Form_Buttons').prepend('<span class="buttons-footer-left"><button type="button" class="btn btn-link" onclick="WJ.showHelpWindow()"><i class="far fa-question-circle"></i>' + WJ.translate('button.help') + '</button></span> <button type="button" class="btn btn-outline-secondary btn-close-editor"><i class="fal fa-times"></i> ' + WJ.translate('button.cancel') + '</button>');

                //nastav checkboxy, toto treba pri kazdom otvoreni, pretoze sa nam menia moznosti select boxov
                $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').parent("div").addClass("custom-control form-switch");
                $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').addClass("form-check-input");
                $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').siblings("label").addClass("form-check-label");

                if (editorWasOpened === false || editorHasTabs === false) {
                    //ked editor nema taby, tak sa resetnu inputy v DOM strome, takze HT musime znova vytvorit
                    $('#' + DATA.id + '_modal .DTE_Form_Content div.row-hr').remove();
                    $.each($('#' + DATA.id + '_modal .DTE_Form_Content').find('[data-dt-field-hr]'), function (key, el) {

                        var hrPosition = $(el).attr("data-dt-field-hr");

                        if (hrPosition.length > 0) {

                            if (hrPosition === "before") {
                                $(el).parents('.DTE_Field').before('<div class="form-group row row-hr"><div class="col-sm-7 offset-sm-4"><hr></div></div>');
                            }
                            if (hrPosition === "after") {
                                $(el).parents('.DTE_Field').after('<div class="form-group row row-hr"><div class="col-sm-7 offset-sm-4"><hr></div></div>');
                            }
                        }
                    });
                }

                //console.log("editorWasOpened=", editorWasOpened);
                if (editorWasOpened === false || DATA.tabsFolders == null || DATA.tabsFolders.length<1) {
                    //toto musime nanovo vykonat aj ked nemame karty, lebo si to DTed pregeneruje

                    $.each($('#' + DATA.id + '_modal .DTE_Form_Content').find('[data-dt-field-headline]'), function (key, el) {

                        var headlineText = $(el).attr("data-dt-field-headline");

                        if (headlineText.length > 0) {
                            $(el).parents('.DTE_Field').before('<div class="form-group row row-headline"><div class="col-sm-7 offset-sm-4"><h5>' + headlineText + '</h5></div></div>');
                        }
                    });

                    $.each($('#' + DATA.id + '_modal .DTE_Form_Content').find('[data-dt-field-full-headline]'), function (key, el) {

                        var headlineText = $(el).attr("data-dt-field-full-headline");

                        if (headlineText.length > 0) {
                            $(el).parents('.DTE_Field').prepend('<div class="form-group row row-full-headline"><div class="col-sm-12"><h5>' + headlineText + '</h5></div></div>');
                        }
                    });

                    $('#' + DATA.id + '_modal .DTE_Form_Content [data-toggle*="tooltip"]').tooltip({
                        placement: 'top',
                        trigger: 'hover',
                        html: true
                    });

                    //console.log("Setting selectpicker");
                    $('#' + DATA.id + '_modal div.DTE_Field_InputControl select').selectpicker(DT_SELECTPICKER_OPTS_EDITOR);
                }

                //console.log("Setting tooltips header");
                $('#' + DATA.id + '_modal .DTE_Header [data-toggle*="tooltip"]').tooltip({
                    placement: 'top',
                    trigger: 'hover',
                    html: true
                });

                if (editorWasOpened === false) {

                    if (!$('#' + DATA.id + '_modal .modal.DTED').hasClass("i-was-here")) {
                        $('#' + DATA.id + '_modal .modal.DTED').addClass("i-was-here");

                        $('#' + DATA.id + '_modal .DTE_Body_Content').addClass('modal-body-bg');
                    }

                    setTimeout(function () {
                        //console.log("Initializing btn-close-editor handler id=", "#"+DATA.id+"_modal", " el=", $("#"+DATA.id+"_modal .btn-close-editor"));
                        //musi byt bindnute dynamicky, kedze button.btn-close-editor sa dynamicky pridava/odobera
                        $("#" + DATA.id + "_modal").on("click", ".btn-close-editor", function (e) {

                            //console.log("Close click, e=", e);
                            var dted = $(e.target).closest(".DTED");
                            //console.log("dted=", dted);
                            //pri mediach je vnoreny dalsi modal, toto zabezpeci ze sa nezavriet editacia stranky ale len modalu a ostane zachovana funkcnost
                            if (dted.hasClass("DTE_nested_modal")) {
                                //musi to byt vnorene takto a nie cez && kvoli viacnasobnej inicializacii eventu
                                if (dted.hasClass("show")) {
                                    //console.log("Closing NESTED modal");
                                    dted.modal('hide').hide().removeClass("show");
                                }
                            } else {
                                //console.log("Closing EDITOR");
                                EDITOR.close();
                                //ked som klikol na editor web stranky, zrusil, potom na editor adresara, zrusil a potom na editor web starnky a zrusil zostal backdrop
                                //console.log("Hiding modal else")
                                $("#" + DATA.id + "_modal").modal("hide");
                            }
                        });
                    }, 200);

                    //nastav autocomplete
                    //console.log("Setting autocomplete");
                    $('#' + DATA.id + '_modal input.form-control[data-ac-url]').each(function () {
                        //console.log("autocomplete", this);
                        var autocompleter = new AutoCompleter('#' + $(this).attr("id")).autobind();
                        $(this).closest("div.DTE_Field").addClass("dt-autocomplete");
                    });
                } else {
                    $('#' + DATA.id + '_modal div.DTE_Field_InputControl select').selectpicker('refresh');
                }

                if ("remove"===action) {
                    $('#' + DATA.id + '_modal > div.modal-dialog').removeClass("modal-fullscreen");
                }

                editorWasOpened = true;
            }

            CustomFields.update(EDITOR, action);
        });

        $.fn.dataTable.ext.search.push(
            function (settings, data, dataIndex) {

                var isOk = true;

                $.each($('#' + TABLE.DATA.id + '_wrapper .input-group[data-filter-type="number"]'), function (key, group) {

                    var min = parseInt($(group).find('.min').val()),
                        max = parseInt($(group).find('.max').val()),
                        index = $(group).parents("th").attr("data-column-index"),
                        val = parseInt(data[index]) || 0;

                    if (!dtConfig.isRangeOk(min, max, val)) isOk = false;

                });

                $.each($('#' + TABLE.DATA.id + '_wrapper .input-group[data-filter-type="number-decimal"]'), function (key, group) {

                    var min = parseFloat($(group).find('.min').val(), 10),
                        max = parseFloat($(group).find('.max').val(), 10),
                        index = $(group).parents("th").attr("data-column-index"),
                        val = parseFloat(data[index], 10).toFixed(2) || 0;

                    if (!dtConfig.isRangeOk(min, max, val)) isOk = false;
                });

                //console.log("Settings: ", settings, " dataIndex=", dataIndex);

                $.each($('#' + TABLE.DATA.id + '_wrapper .input-group[data-filter-type="boolean"]'), function (key, group) {

                    var option = $(group).find('option:selected').val(),
                        index = $(group).parents("th").attr("data-column-index"),
                        val = data[index];

                    //if (index===8) console.log("Searching boolean, option=",option," index=", index, " val=",val);

                    if (option !== "") {

                        if (option === "true") {
                            if (val === option) {
                            } else {
                                isOk = false;
                            }
                        }

                        if (option === "false") {
                            if (val === option) {
                            } else {
                                isOk = false;
                            }
                        }

                        if (option === "empty") {
                            if (val === "") {
                            } else {
                                isOk = false;
                            }
                        }
                    }
                });

                $.each($('#' + TABLE.DATA.id + '_wrapper .dataTables_scrollHeadInner .input-group[data-filter-type="date"], #' + TABLE.DATA.id + '_wrapper .dataTables_scrollHeadInner .input-group[data-filter-type="datetime"], #' +
                + TABLE.DATA.id + '_wrapper .dataTables_scrollHeadInner .input-group[data-filter-type="timehm"], #' + + TABLE.DATA.id + '_wrapper .dataTables_scrollHeadInner .input-group[data-filter-type="timehms"]'), function (key, group) {

                    var min = $(group).find('.min').val(),
                        max = $(group).find('.max').val(),
                        index = $(group).parents("th").attr("data-column-index"),
                        val = parseInt(data[index]) || 0;

                    if (min === null) {
                        min = NaN;
                    } else {
                        //console.log("Parsing min:",min," locale=",moment.locale());
                        min = parseInt(moment(min, "L").valueOf());
                    }

                    if (max === null) {
                        max = NaN;
                    } else {
                        max = parseInt(moment(max, "L").add(1, 'days').valueOf());
                    }

                    //console.log("Filter date, min=",min," max=",max," val=",val," key=",key, "index=",index, " group=",group, "data=", data);

                    if (!dtConfig.isRangeOk(min, max, val)) isOk = false;
                });

                return isOk;

            }
        );

        var editButtonExtends = "edit";
        if (DATA.fetchOnEdit) editButtonExtends = "editRefresh";


        var buttonsList = [];

        if (hasPermission("create")) {
            buttonsList.push({
                extend: "create",
                text: "<i class='far fa-plus'></i>",
                className: 'btn-success buttons-divider',
                editor: EDITOR,
                attr: {
                    title: WJ.translate('button.add'),
                    "data-toggle": "tooltip",
                    "data-dtbtn": "create"
                }
            });
        }
        if (hasPermission("edit")) {
            buttonsList.push({
                extend: editButtonExtends,
                text: "<i class='far fa-pencil'></i>",
                editor: EDITOR,
                className: 'btn-warning',
                attr: {
                    title: WJ.translate('button.edit'),
                    "data-toggle": "tooltip",
                    "data-dtbtn": "edit"
                }
            });
        }
        if (hasPermission("create") && hasPermission("duplicate")) {
            buttonsList.push({
                extend: 'selected',
                text: "<i class='far fa-copy'></i>",
                editor: EDITOR,
                className: 'btn-duplicate',
                attr: {
                    title: WJ.translate('button.duplicate'),
                    "data-toggle": "tooltip",
                    "data-dtbtn": "duplicate"
                },
                action: function ( e, dt, node, config ) {
                    if (DATA.fetchOnEdit) {
                        TABLE.buttons(".buttons-edit").processing(true);
                        const selectedRows = dt.rows({selected: true}).ids();
                        let counter = 0;

                        for (let i=0; i<selectedRows.length; i++) {
                            let id = selectedRows[i];
                            //console.log("Refreshing row, id=", id);
                            refreshRow(id, function (json) {
                                counter++;
                                //console.log("counter=", counter, "length=", selectedRows.length)
                                if (counter >= selectedRows.length) {
                                    TABLE.buttons(".buttons-edit").processing(false);
                                    EDITOR
                                    .edit( TABLE.rows( {selected: true} ).indexes(), {
                                        title: '<div class="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.duplicate') + '</h5></div><div class="col-sm-8" id="dt-header-tabs-' + DATA.id + '"></div></div>'+MAXIMIZE_HTML,
                                        buttons: '<i class="fal fa-check"></i> ' + WJ.translate('button.duplicate')
                                    } )
                                    .mode( 'create' );
                                    $("#"+EDITOR.TABLE.DATA.id+"_modal").attr("data-action", "duplicate");
                                }
                            });
                        }
                    }
                    else {
                        // Start in edit mode, and then change to create
                        EDITOR
                            .edit( TABLE.rows( {selected: true} ).indexes(), {
                                title: '<div class="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.duplicate') + '</h5></div><div class="col-sm-8" id="dt-header-tabs-' + DATA.id + '"></div></div>'+MAXIMIZE_HTML,
                                buttons: '<i class="fal fa-check"></i> ' + WJ.translate('button.duplicate')
                            } )
                            .mode( 'create' );
                            $("#"+EDITOR.TABLE.DATA.id+"_modal").attr("data-action", "duplicate");
                    }
                }
            });
        }
        if (hasPermission("remove")) {
            buttonsList.push({
                extend: "remove",
                text: "<i class='far fa-trash-alt'></i>",
                editor: EDITOR,
                className: 'btn-danger buttons-divider',
                attr: {
                    title: WJ.translate('button.delete'),
                    "data-toggle": "tooltip",
                    "data-dtbtn": "remove"
                }
            });
        }
        buttonsList.push({
            name: "reload",
            text: "<i class='fa-duotone fa-rotate'></i>",
            editor: EDITOR,
            className: 'btn-outline-secondary buttons-refresh',
            attr: {
                title: WJ.translate('datatables.button.reload.js'),
                "data-toggle": "tooltip",
                "data-dtbtn": "reload"
            },
            action: function (e, dt, node, config) {
                dt.ajax.reload();
            }
        });
        buttonsList.push({
            text: "<i class='far fa-upload'></i>",
            className: 'btn-outline-secondary btn-export-dialog',
            attr: {
                title: WJ.translate('datatables.button.export.js'),
                "data-dtbtn": "export"
            },
            action: function () {
                window.datatableExportModal.tableId = TABLE.DATA.id;
                //nastav title exportu podla title stranky
                var title = document.title;
                var pipe = title.indexOf(" | ");
                if (pipe > 0) title = title.substring(0, pipe);
                title = title.toLowerCase();
                title = title + "-" + moment().format("YYYYMMDD-HHmmss");
                title = title.replace(/\./gi, "_").replace(/:/gi, "_").replace(/ /gi, "-");
                //export modal je staticky, potrebuje vediet aku tabulku exportujeme
                $("#datatableExportModal input.dt-settings-filename").val(title);
                //prepni na prvy tab
                $("#pills-export-basic-tab")[0].click();
                //inicializuj pdfmake kniznicu
                if (createPdfMake) {
                    createPdfMake().then(module => {
                        window.pdfMake = module;
                        createPdfFonts().then(module => {
                            const pdfFonts = module;
                            window.pdfMake.vfs = pdfFonts.pdfMake.vfs;
                        });
                    });
                }
                //console.log("serverSide=", DATA.serverSide);
                if (DATA.serverSide===false) {
                    //schovaj nepodporovane moznosti
                    $("#datatablesExportWhichData").hide();
                    $("#datatablesExportSortOrder").hide();
                    $("#dt-settings-extend1").prop("checked", true);
                    $("#dt-settings-page1").prop("checked", true);
                    $("#pills-export-advanced-tab").hide();
                }

                if (DATA.lastExportColumnName != null) $("#exportLastExportOption").show();
                else $("#exportLastExportOption").hide();

                if (DATA.byIdExportColumnName != null) $("#exportByIdExportOption").show();
                else $("#exportByIdExportOption").hide();

                var exportModal = bootstrap.Modal.getOrCreateInstance(document.getElementById("datatableExportModal"), {
                    keyboard: false
                });
                exportModal.show();
                $(exportModal._backdrop._element).css("z-index", $("#datatableExportModal").css("z-index")-1);
            }
        });
        if (hasPermission("create") || hasPermission("edit")) {
            buttonsList.push({
                text: "<i class='far fa-download'></i>",
                className: 'btn-outline-secondary  btn-import-dialog buttons-divider',
                attr: {
                    title: WJ.translate('datatables.button.import.js'),
                    "data-dtbtn": "import"
                },
                action: function () {
                    window.datatableImportModal.tableId = TABLE.DATA.id;
                    //nastav importu aktualnu datatable
                    window.datatableImportModal.TABLE = TABLE;
                    var select = document.getElementById("dt-settings-update-by-column");
                    select.options.length = 0;

                    //pridaj ID option
                    var option = new Option("ID", "id");
                    select.options[select.options.length] = option;

                    //pridaj optiony podla columns definicie editora
                    DATA.fields.forEach((col, index) => {
                        //console.log("col=", col, "index: ", index);

                        let data = col.data;
                        if (typeof col.attr != "undefined" && typeof col.attr["data-dt-import-updateByColumn"] != "undefined") {
                            //tu nastavujeme iny stlpec pre parovanie v importe, pretoze to moze byt v editorFields.login, ale hladat a parovat budeme podla login atributu
                            data = col.attr["data-dt-import-updateByColumn"];
                        }
                        if (typeof col.attr != "undefined" && typeof col.attr["data-dt-import-hidden"] == "true") {
                            //toto nechceme zobrazovat v moznosti parovania importu
                            return;
                        }

                        //editorFields.nieco nemozeme pouzit, lebo podla toho nevieme hladat
                        if (data.indexOf(".")!=-1) return;
                        //hidden fieldy tiez nezobrazime pre moznost vyhladania
                        if ("hidden"===col.type) return;

                        //ako label davame povodne col.data (lebo tak to moze byt v xls ako editorFields.login) ale value (napr. login) je pripadne upravena hodnota z data-dt-import-updateByColumn
                        var option = new Option(col.label+" - "+col.data, data);
                        select.options[select.options.length] = option;
                    });
                    $(select).selectpicker('refresh');
                    $("#datatableImportModal div.DTE_Form_Error").text("");

                    var importModal = bootstrap.Modal.getOrCreateInstance(document.getElementById("datatableImportModal"), {
                        keyboard: false
                    });
                    importModal.show();
                    $(importModal._backdrop._element).css("z-index", $("#datatableImportModal").css("z-index")-1);
                    //console.log("Import modal=", importModal);
                }
            });
        }
        if (hasPermission("edit")) {
            buttonsList.push({
                tag: "div",
                text: ` <input type="checkbox" class="form-check-input" id="dtAllowCellEdit" value="true"/>
                        <label class="form-check-label is-icon-arrows-v" for="dtAllowCellEdit"></label>`,
                className: 'custom-control form-switch buttons-select-cel',
                attr: {
                    title: WJ.translate('datatables.button.celledit.js'),
                    "data-toggle": "tooltip",
                    "data-dtbtn": "celledit"
                },
                action: function (e, node, el) {
                    //console.log("action, el=", el, "disbled=", $(el).hasClass("is-disabled"));
                    if ($(el).hasClass("enabled")) {
                        $('body').removeClass("datatable-cell-editing");
                        $(el).removeClass("enabled");
                    } else {
                        $('body').addClass("datatable-cell-editing");
                        $(el).addClass("enabled");
                    }
                }
            });
        }
        buttonsList.push({
            extend: 'collection',
            text: '<i class="far fa-wrench"></i>',
            className: 'btn-outline-secondary buttons-settings',
            attr: {
                title: WJ.translate('datatables.button.settings.js'),
                "data-toggle": "tooltip",
                "data-dtbtn": "settings"
            },
            buttons: [
                {
                    extend: 'colvis',
                    text: '<i class="far fa-grip-lines-vertical"></i> ' + WJ.translate('datatables.button.cellvisibility.js'),
                    autoClose: false,
                    attr: {
                        title: WJ.translate('datatables.button.cellvisibility.js'),
                        "data-toggle": "tooltip",
                        "data-dtbtn": "cellvisibility"
                    },
                    columnText: function ( dt, idx, title ) {
                        //console.log("columnText, dt=", dt, "idx=", idx, "title=", title, "columns=", DATA.columns);
                        let columnText = title;
                        try {
                            //zober aktualny title
                            columnText = DATA.columns[idx].title;
                            //console.log("columnText=", columnText, "data=", DATA.columns[idx]);
                        } catch (e) {}

                        let tab = DATA.columns[idx]?.editor?.tab;
                        //console.log("tab=", tab);

                        if (idx == 0) {
                            window.colvisLastTab = "";
                            window.colvisLastHeadline = "";
                        }
                        let tabTitle = "";
                        if (typeof tab != "undefined") {
                            //ziskaj meno tabu
                            tabTitle = tab;
                            for (let i = 0; i < DATA.tabsFolders.length; i++) {
                                if (DATA.tabsFolders[i].id == tab) {
                                    tabTitle = DATA.tabsFolders[i].title;
                                    break;
                                }
                            }
                        }
                        if (window.colvisLastTab != tabTitle) {
                            //ked sa zmeni tab resetni lastHeadline
                            window.colvisLastTab = tabTitle;
                            window.colvisLastHeadline = "";
                        }

                        //nadpis
                        let headline = "";
                        if (DATA.columns[idx]?.editor?.attr) headline =  DATA.columns[idx].editor.attr["data-dt-field-headline"];
                        //console.log("headline=", headline);
                        if (typeof headline == "undefined") headline = "";
                        if (tabTitle == headline) headline = "";
                        if (columnText == headline) headline = "";
                        if (headline != "") window.colvisLastHeadline = headline;

                        //tooltip
                        let tooltipHtml = "";
                        let tooltipText = "";
                        if (DATA.columns[idx]?.editor?.message) tooltipText =  DATA.columns[idx].editor.message;
                        if (typeof tooltipText != "undefined" && tooltipText != "") {
                            tooltipHtml = `<span class="btn btn-link btn-tooltip" data-toggle="tooltip" title="${tooltipText}"><i class="far fa-info-circle"></i></span>`;
                        }

                        let colvisLastHeadline = window.colvisLastHeadline;
                        if (colvisLastHeadline != "") colvisLastHeadline = colvisLastHeadline + "&nbsp;";

                        columnText = `<span class="tab-title">${tabTitle}</span><span class="tab-columntext"><span class="tab-headline">${colvisLastHeadline}</span><span class="column-title">${columnText}</span></span><span class="btn-tooltip">${tooltipHtml}</span>`;

                        return columnText;
                    },
                    columns: function ( idx, data, node ) {
                        //console.log("ColVis columns, idx=", idx, "data=", data, "node=", node);
                        if ($(node).text()=="null") return false;
                        return true;
                    },
                    prefixButtons: [
                        {
                            text: '<span class="tab-title">'+WJ.translate("datatables.colvis.tab.js")+'</span><span class="tab-columntext"><span class="tab-headline">'+WJ.translate("datatables.colvis.headline.js")+'&nbsp;</span> '+WJ.translate("datatables.colvis.column.js")+'</span><span class="btn-tooltip"></span>',
                            className: 'btn buttons-colvisGroup colvis-prefix btn-outline-secondary'
                        }
                    ],
                    postfixButtons: [
                        {
                            extend: 'colvisGroup',
                            text: '<i class="far fa-check-square"></i> ' + WJ.translate('datatables.button.show_all.js'),
                            show: ':hidden',
                            className: 'colvis-postfix btn-outline-secondary'
                        },
                        {
                            extend: 'colvisGroup',
                            text: '<i class="far fa-square"></i> ' + WJ.translate('datatables.button.hide_all.js'),
                            hide: ':visible:not(.test)',
                            className: 'colvis-postfix btn-outline-secondary'
                        },
                        {
                            extend: 'colvisRestore',
                            text: '<i class="far fa-redo"></i> ' + WJ.translate('datatables.button.restore.js'),
                            className: 'colvis-postfix btn-outline-secondary dt-close-modal',
                            action: function(e, dt, node, config) {
                                //console.log("Colvis restore, TABLE=", TABLE, "this=", this, "dt=", dt);
                                window.dataTableCellVisibilityService.removeColumnConfig(dt.editor().TABLE);
                                dtWJ.stateReset(dt.editor().TABLE);
                            }
                        },
                        {
                            text: '<i class="far fa-check"></i> ' + WJ.translate('button.save'),
                            className: 'colvis-postfix btn-primary dt-close-modal',
                            action: function(e, dt, node, config) {
                                window.dataTableCellVisibilityService.buildConfigDataFromObject(dt.editor().TABLE);
                                //console.log("Zmena stlpcov, fixujem selecty");
                                fixDatatableHeaderInputs();
                            }
                        },
                        {
                            text: '<i class="fal fa-times"></i> ' + WJ.translate('button.cancel'),
                            className: 'colvis-postfix btn btn-outline-secondary dt-close-modal'
                        }
                    ]
                },
                {
                    name: "pageLengthMenu",
                    extend: 'pageLength',
                    text: '<i class="far fa-grip-lines"></i> ' + WJ.translate('datatables.button.pagelength.js'),
                    autoClose: false,
                    attr: {
                        title: WJ.translate('datatables.button.pagelength.js'),
                        "data-toggle": "tooltip",
                        "data-dtbtn": "pagelength"
                    },
                    postfixButtons: [
                        {
                            className: 'dropdown-divider',
                            tag: 'div'
                        },
                        {
                            text: '<i class="far fa-check"></i> ' + WJ.translate('button.save'),
                            className: 'btn-primary dt-close-modal'
                        },
                        {
                            text: '<i class="fal fa-times"></i> ' + WJ.translate('button.cancel'),
                            className: 'btn btn-outline-secondary dt-close-modal'
                        }
                    ]
                },
            ]
        });

        //console.log("buttonsList=", buttonsList);

        calculateAutoPageLength(false);

        $.extend(true, $.fn.dataTable.defaults, {

            //Dom tree
            dom: "<'dt-header-row clearfix'<'row'<'col-auto'B><'col ps-0 pe-0'<'dt-filter-labels'>>>>" +
                "<'row'<'col'<'rounded-bg'tr>>>" +
                "<'row dt-footer-row'<'col'i><'col-auto g-0'p>>",

            // fixedColumns
            //jeeff: toto ratame v _table.scss kedze to ma viac podmienok pre cardView scrollY: "calc(100vh - 332px)",
            scrollX: true,
            scrollCollapse: false,

            autoWidth: true,

            //colReorder
            colReorder: {
                realtime: false
            },

            orderCellsTop: true,

            //autoFill
            autoFill: false,

            //pagination
            paging: true,
            pagingType: "simple_numbers",

            //keytable
            keys: false,

            pageLength: DATA.autoPageLength,

            lengthMenu: [[DATA.autoPageLength, 25, 50, 100, 500, 1000, 2000, -1], [DATA.autoPageLengthTitle, 25, 50, 100, 500, 1000, 2000, WJ.translate('datatables.defaults.length_menu.js')]],

            //language SK
            language: {
                sEmptyTable: WJ.translate('datatables.defaults.empty_table.js'),
                sInfo: WJ.translate('datatables.defaults.info.js'),
                sInfoEmpty: WJ.translate('datatables.defaults.info_empty.js'),
                sInfoFiltered: '(' + WJ.translate('datatables.defaults.info_filtered.js') + ')',
                sInfoPostFix: '',
                sInfoThousands: ',',
                sLengthMenu: '_MENU_',
                sLoadingRecords: WJ.translate('datatables.defaults.loading_records.js') + '...',
                sProcessing: WJ.translate('datatables.defaults.processing.js') + '...',
                sSearch: WJ.translate('datatables.defaults.search.js') + ':',
                sZeroRecords: WJ.translate('datatables.defaults.zero_records.js'),
                oPaginate: {
                    sFirst: '<i class="far fa-angle-double-left"></i>',
                    sLast: '<i class="far fa-angle-double-right"></i>',
                    sNext: '<i class="far fa-angle-right"></i>',
                    sPrevious: '<i class="far fa-angle-left"></i>'
                },
                oAria: {
                    sSortAscending: WJ.translate('datatables.defaults.sort.asc.js'),
                    sSortDescending: WJ.translate('datatables.defaults.sort.desc.js')
                },
                select: {
                    rows: {
                        _: '%d ' + WJ.translate('datatables.defaults.rows.0.js'),
                        0: '',
                        1: '1 ' + WJ.translate('datatables.defaults.rows.1.js'),
                        2: '%d ' + WJ.translate('datatables.defaults.rows.2.js'),
                        3: '%d ' + WJ.translate('datatables.defaults.rows.3.js'),
                        4: '%d ' + WJ.translate('datatables.defaults.rows.4.js'),
                    }
                }
            },

            // //responsive
            responsive: false,

            // fixedColumns: {
            //     leftColumns: 2,
            //     rightColumns: 0
            // },

            select: {
                style: 'multi',
                selector: 'td.dt-select-td'
            },

            //buttons
            buttons: {
                dom: {
                    container: {
                        className: 'dt-buttons'
                    },
                    button: {
                        tag: 'button',
                        className: 'btn  btn-sm'
                    },
                    collection: {
                        className: 'dropdown-menu dt-dropdown-menu',
                        button: {
                            tag: 'button',
                            className: 'btn'
                        },
                    }
                },
                buttons: buttonsList
            },

            initComplete: function (settings, json) {

                //console.log("initComplete, selectPicker");
                fixDatatableHeaderInputs();

                $('#' + DATA.id + '_wrapper [data-toggle*="tooltip"]').tooltip({
                    placement: 'top',
                    trigger: 'hover'
                });

                $.each($('#' + DATA.id + '_wrapper [data-toggle*="modal"]'), function (key, item) {
                    $(item).on("click", function () {
                        $($(item).data("target")).modal({
                            backdrop: 'static',
                            keyboard: false
                        });
                    });
                });

                /*
                console.log("select picker 3");
                $('.dataTables_wrapper select').selectpicker(DT_SELECTPICKER_OPTS);*/

                $('#' + DATA.id + '_wrapper .buttons-select-all').on('click', function (e) {
                    if (TABLE.rows({selected:true}).count()>0) {
                        TABLE.rows().deselect();
                    } else {
                        TABLE.rows({"filter": "applied", "page": "current"}).select();
                    }
                    return false;
                });
            },

            columnDefs: [
                {
                    targets: "dt-format-selector",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderNumber(td, type, rowData, row, "0");
                    }
                },
                {
                    targets: "dt-format-text",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderText(td, type, rowData, row);
                    }
                },
                {
                    targets: "html-input",
                    type: "html-input"
                },
                {
                    targets: "dt-format-text-wrap",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderTextHtmlInput(td, type, rowData, row);
                    },
                    type: "html-input"
                },
                {
                    targets: "dt-format-select",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderSelect(td, type, rowData, row);
                    }
                },
                {
                    targets: "dt-format-checkbox",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderCheckbox(td, type, rowData, row);
                    }
                },
                {
                    targets: "dt-format-radio",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderRadio(td, type, rowData, row);
                    }
                },
                {
                    targets: "dt-format-boolean-true",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderBoolean(td, type, rowData, row, "true", "false");
                    }
                },
                {
                    targets: "dt-format-boolean-yes",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderBoolean(td, type, rowData, row, "yes", "no");
                    }
                },
                {
                    targets: "dt-format-boolean-one",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderBoolean(td, type, rowData, row, "1", "0");
                    }
                },
                {
                    targets: "dt-format-number",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderNumber(td, type, rowData, row, "0,0");
                    }
                },
                {
                    targets: "dt-format-number--text",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderNumber(td, type, rowData, row, "0,0 a");
                    }
                },
                {
                    targets: "dt-format-number--decimal",
                    className: "dt-style-number",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderNumberDecimal(td, type, rowData, row, "0,0.00");
                    }
                },
                {
                    targets: "dt-format-number--decimal--text",
                    className: "dt-style-number",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderNumberDecimal(td, type, rowData, row, "0,0.00 a");
                    }
                },
                {
                    targets: "dt-format-percentage",
                    className: "dt-style-number",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderPercentage(td, type, rowData, row, "0 %");
                    }
                },
                {
                    targets: "dt-format-percentage--decimal",
                    className: "dt-style-number",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderPercentage(td, type, rowData, row, "0.00 %");
                    }
                },
                {
                    targets: "dt-format-date",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderDate(td, type, rowData, row, "L");
                    }
                },
                {
                    targets: "dt-format-date-time",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderDate(td, type, rowData, row, "L HH:mm:ss");
                    }
                },
                {
                    targets: "dt-format-time-hm",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderDate(td, type, rowData, row, "HH:mm");
                    }
                },
                {
                    targets: "dt-format-time-hms",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderDate(td, type, rowData, row, "HH:mm:ss");
                    }
                },
                {
                    targets: "dt-format-date--text",
                    className: "dt-style-date",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderDate(td, type, rowData, row, "ll");
                    }
                },
                {
                    targets: "dt-format-date-time--text",
                    className: "dt-style-date",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderDate(td, type, rowData, row, "ll HH:mm:ss");
                    }
                },
                {
                    targets: "dt-format-link",
                    className: "dt-style-link",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderLink(td, type, rowData, row);
                    }

                },
                {
                    targets: "dt-format-image",
                    className: "dt-style-image",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderImage(td, type, rowData, row);
                    }

                },
                {
                    targets: "dt-format-mail",
                    className: "dt-style-link",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderMail(td, type, rowData, row);
                    }
                },
                {
                    targets: "dt-format-json",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderJson(td, type, rowData, row);
                    }
                },
                {
                    targets: "dt-format-attrs",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderAttrs(td, type, rowData, row);
                    }
                }
            ]

        });

        //setni este raz buttony, tie sa prepisuju, nie extenduju
        $.fn.dataTable.defaults.buttons.buttons = buttonsList;

        $(dataTableSelector + ' thead tr').clone(true).appendTo(dataTableSelector + ' thead');
        $(dataTableSelector + ' thead tr:eq(0) th').each(function (i) {
            $(this).wrapInner('<div class="datatable-column-width"></div>');
        });

        //console.log("serverSide=", DATA.serverSide);
        var extfilterExists = $("#" + DATA.id + "_extfilter").length > 0;

        $(dataTableSelector + ' thead tr:eq(1) th').each(function (i) {

            $(this).attr("data-column-index", i);
            //console.log("Iterating, i=", i, " col=", DATA.columns[i]);
            var fieldName = DATA.columns[i].name;

            var inputGroupBefore = `
                <form>
                    <div class="input-group" data-filter-type="text">`;
            var inputGroupAfter = `

                            <button class="filtrujem btn btn-sm btn-outline-secondary dt-filtrujem-${fieldName}" type="submit">
                                <i class="far fa-search"></i>
                            </button>

                    </div>
                </form>`;
            var html = `
                    <select class="filter-input-prepend">
                        <option value="contains" selected data-content="<i class=\'far fa-arrows-h\'></i><small>${WJ.translate('datatables.select.contains.js')}</small>">${WJ.translate('datatables.select.contains.js')}</option>
                        <option value="startwith" data-content="<i class=\'far fa-arrow-from-left\'></i><small>${WJ.translate('datatables.select.startwith.js')}</small>">${WJ.translate('datatables.select.startwith.js')}</option>
                        <option value="endwith" data-content="<i class=\'far fa-arrow-from-right\'></i><small>${WJ.translate('datatables.select.endwith.js')}</small>">${WJ.translate('datatables.select.endwith.js')}</option>
                        <option value="equals" data-content="<i class=\'far fa-equals\'></i><small>${WJ.translate('datatables.select.equals.js')}</small>"><i class="far fa-arrow-from-right"></i> ${WJ.translate('datatables.select.equals.js')}</option>`;
            //regex nie je dobre podporovany v Spring data, takze sa nemoze pouzit pri server side
            //regex disabled, most tables are server side
            /*if (DATA.serverSide === false) {
                html += `
                        <option value="regex" data-content="<i class=\'far fa-brackets\'></i><small>${WJ.translate('datatables.select.regex.js')}</small>"><i class="far fa-brackets"></i> ${WJ.translate('datatables.select.regex.js')}</option>`;
            }*/
            html += `
                    </select>
                    <input class="form-control form-control-sm filter-input dt-filter-${fieldName}" type="text" />`;

            if ($(this).hasClass("dt-format-selector")) {
                html = `
                <button class="buttons-select-all btn btn-sm btn-outline-secondary dt-filter-${fieldName}">
                    <i class="far fa-check-square"></i>
                </button>
                <input class="form-control form-control-sm filter-input min max filter-input-id dt-filter-${fieldName}" type="text" />
                `;
            }

            if ($(this).hasClass("dt-format-boolean-true") || $(this).hasClass("dt-format-boolean-yes") || $(this).hasClass("dt-format-boolean-one")) {
                inputGroupBefore = '<form><div class="input-group" data-filter-type="boolean">';
                html = `
                <select class="filter-input dt-filter-${fieldName}" data-dt-name="${fieldName}">
                    <option value="">${WJ.translate('datatables.select.all.js')}</option>
                    <option value="true">${WJ.translate('button.yes')}</option>
                    <option value="false">${WJ.translate('button.no')}</option>
                </select>`;
            }

            //Fix - number inputs (others formats stay as string due custom components/styling)
            if ($(this).hasClass("dt-format-number") || $(this).hasClass("dt-format-percentage")) {
                inputGroupBefore = '<form><div class="input-group" data-filter-type="number">';
                html = `
                <input class="min form-control form-control-sm dt-filter-from-${fieldName}" type="number" placeholder="${WJ.translate('datatables.input.from.js')}"/>
                <input class="max form-control form-control-sm dt-filter-to-${fieldName}" type="number" placeholder="${WJ.translate('datatables.input.to.js')}"/>`;
            }

            if ($(this).hasClass("dt-format-number--decimal") || $(this).hasClass("dt-format-percentage--decimal") || $(this).hasClass("dt-format-number--text")) {
                inputGroupBefore = '<form><div class="input-group" data-filter-type="number-decimal">';
                html = `
                <input class="min form-control form-control-sm dt-filter-from-${fieldName}" type="number" placeholder="${WJ.translate('datatables.input.from.js')}"/>
                <input class="max form-control form-control-sm dt-filter-to-${fieldName}" type="number" placeholder="${WJ.translate('datatables.input.to.js')}"/>`;
            }

            if ($(this).hasClass("dt-format-date") || $(this).hasClass("dt-format-date-time") || $(this).hasClass("dt-format-date--text")
            || $(this).hasClass("dt-format-date-time--text") || $(this).hasClass("dt-format-time-hm") || $(this).hasClass("dt-format-time-hms")) {
                let dateFormat = "datepicker";
                let filterType = "date";
                if ($(this).hasClass("dt-format-date-time")) {
                    dateFormat = "datetimepicker";
                    filterType = "datetime";
                }

                if ($(this).hasClass("dt-format-time-hm")) {
                    dateFormat = "timehmpicker";
                    filterType = "datetime";
                }

                if ($(this).hasClass("dt-format-time-hms")) {
                    dateFormat = "timehmspicker";
                    filterType = "datetime";
                }
                inputGroupBefore = `<form><div class="input-group" data-filter-type="${filterType}">`;


                html = `
                <input class="${dateFormat} min form-control form-control-sm dt-filter-from-${fieldName}" type="text" placeholder="${WJ.translate('datatables.input.from.js')}"/>
                <input class="${dateFormat} max form-control form-control-sm dt-filter-to-${fieldName}" type="text" placeholder="${WJ.translate('datatables.input.to.js')}"/>`;
            }

            if ($(this).hasClass("dt-format-none")) {
                inputGroupBefore = '';
                inputGroupAfter = '';
                html = ``;
            }

            if ($(this).hasClass("dt-format-select") || $(this).hasClass("dt-format-radio")) {
                inputGroupBefore = '<form><div class="input-group" data-filter-type="select">';
                html = `<select class="filter-input dt-filter-${fieldName}" data-dt-name="${fieldName}">`;
                //hodnoty sa setnu volanim updateFilterSelect po dobehnuti ajax requestu
                let options = DATA.columns[i].editor.options;
                //console.log("Options=", options);
                if (typeof options != "undefined" && options != null && options.length>0) {
                    //ak mame definovane data priamo v JSON definicii musime ich inicializovat
                    setTimeout(function() {
                        updateFilterSelect(fieldName);
                    }, 300);
                }
                html += '</select>';
            }

            // var popOver = '<a class="btn btn-sm btn-outline-secondary row-menu" data-placement="bottom" data-content="" role="button"><i class="far fa-ellipsis-v"></i></a>';

            var filterHtml = inputGroupBefore + html + inputGroupAfter;
            //console.log("filter["+i+"] typeof=", typeof DATA.columns[i].filter);
            if (typeof DATA.columns[i].filter === "undefined" || DATA.columns[i].filter === true) $(this).html(filterHtml);
            else $(this).html("");

            if (extfilterExists) {
                //console.log("Setting ext filter for ", fieldName, " html: ", filterHtml);
                $("#" + DATA.id + "_extfilter .dt-extfilter-title-" + fieldName).text(DATA.columns[i].title);
                $("#" + DATA.id + "_extfilter .dt-extfilter-" + fieldName).attr("data-column-index", i);
                $("#" + DATA.id + "_extfilter .dt-extfilter-" + fieldName).html(filterHtml);
            }

            $(this).find("input.filter-input").on("keypress", function(e) {
                if (e.key === "Enter") {
                    //console.log("Mam keypress, e=", e);
                    e.preventDefault();
                    $(e.target).parent().find("button.filtrujem").trigger("click");
                }
            })

        }).promise().done(function () {
            runDataTables();
        });

        function datatable2SpringData(sSource, aoData, fnCallback) {

            //console.log("datatable2SpringData, sSource: ",sSource, " aoData: ", aoData, " TABLE: ", TABLE);

            if (typeof DATA.defaultSearch === "object" && DATA.defaultSearch != null) {
                //nastav hodnoty default search fieldov
                for (const property in DATA.defaultSearch) {
                    const value = DATA.defaultSearch[property];
                    //console.log("Setting default search, property=", property, "value=", value);
                    $(property).val(value);

                    //over hodnotu v selecte, ak je null, nenachadza sa tam, skus setnut -1, je to typicky rootGroupId
                    if ($(property).val()==null) $(property).val("-1");

                    //[55537] - FIX of filtering using EXT filter
                    //The problem was value set in table filter, then EXT filter value was ignored

                    //Find property name
                    let propertyName = property.split("-");
                    propertyName = propertyName[propertyName.length - 1];
                    if(propertyName !== undefined && propertyName !== null) {
                        //Find table filter and reset values
                        var $th = $('#' + DATA.id + '_wrapper .dataTables_scrollHeadInner tr:last-child th[data-dt-field-name="' + propertyName + '"]');
                        $th.find(".form-control-sm").val("");
                        $th.find("select").selectpicker("val", "");
                        $th.find("select").selectpicker("val", "contains");
                        $th.find(".min").val("");
                        $th.find(".max").val("");

                        //Disable filter
                        $th.find(".min").prop('disabled', true);
                        $th.find(".max").prop('disabled', true);
                        $th.find(".filtrujem").prop('disabled', true);
                        $th.find("select").prop('disabled', true);

                        //Set disabled CSS style
                        $th.find(".input-group").css("background-color", "#F3F3F6");
                        $th.find(".input-group").css("border-colorr", "#868EA5");
                    }
                }

                //$("#topDataTable_extfilter button.dt-filtrujem-dayDate").trigger("click");
                //@TODO doplnit konkretny filter
                dtWJ.filtrujemClick($("#"+DATA.id+"_extfilter button.dt-filtrujem-dayDate")[0], TABLE, DATA, true);
                DATA.defaultSearch = null;
            }

            if (typeof TABLE != "undefined") {
                //zober aktualnu URL tabulky, ktora sa mohla medzi tym zmenit
                sSource = TABLE.ajax.url();
            }

            var serverSide = "true" === this.attr("data-server-side");
            var url = WJ.urlAddPath(sSource, urlSuffix);
            if (typeof TABLE !== 'undefined') {
                var ajaxUrl = TABLE.ajax.url();
                url = WJ.urlAddPath(ajaxUrl, urlSuffix);
            }
            var columnName;
            var isAnyColumnSearch = false;
            var hasFixedParams = false;
            var restParams = [];
            var searchColumnParams = [];
            var paramMap = {};

            //console.log("aoData=", aoData);

            // prechod vsetkymi vstupnymi parametrami od datatables
            for (var i = 0; i < aoData.length; i++) {

                // extract name/value pairs into a simpler map for use later
                paramMap[aoData[i].name] = aoData[i].value;

                // zistenie nazvu stlpca (vsetky nasledujuce parametre sa viazu k nemu)
                if (aoData[i].name.includes('mDataProp_')) {
                    columnName = aoData[i].value;
                }

                // ak je to vyhladavacie policko naplnime prislusny parameter (id ignorujeme)
                //console.log("search test, i=",i," name=",aoData[i].name," aoData=",aoData[i], " columnName=", columnName);
                if (aoData[i].name.includes('sSearch_') && columnName !== null) {

                    if (!isAnyColumnSearch && aoData[i].value) {
                        isAnyColumnSearch = true;
                    }
                    columnName = columnName.charAt(0).toUpperCase() + columnName.slice(1);
                    if (columnName !== "" && aoData[i].value !== "") {
                        searchColumnParams.push({"name": "search" + columnName, "value": aoData[i].value});
                        //console.log("Search["+i+"]: '" + columnName + "' value: '" + aoData[i].value + "'");
                    }
                }

                if (aoData[i].name.indexOf('fixed_')==0) {
                    //fixne parametre nastavene cez DATA.onPreXhr funkciu
                    searchColumnParams.push({"name": aoData[i].name.substring(6), "value": aoData[i].value});
                    hasFixedParams = true;
                    //its search param, switch to search URL
                    if (aoData[i].name.indexOf("fixed_search")==0) isAnyColumnSearch = true;
                }
            }

            //skontroluj externe filtre
            try {
                for (var i = 0; i < DATA.columns.length; i++) {
                    var column = DATA.columns[i];
                    //over, ci existuje input v extfiltri
                    if ($("#" + DATA.id + "_extfilter .dt-extfilter-" + column.name).length > 0) {
                        var value = column.searchVal;
                        if (typeof value !== "undefined" && value !== "") {
                            isAnyColumnSearch = true;

                            //If value for this columns is allreadz in map, repalce value (ext filter have bigger priority) -: OR add new value
                            let extColumnsName = "search" + column.name.charAt(0).toUpperCase() + column.name.slice(1);
                            let isThere = false;
                            //console.log("Testing ext filter, searchColumnParams=", searchColumnParams);
                            for(let obj of searchColumnParams) {
                                if(extColumnsName == obj.name) {
                                    isThere = true;
                                    //update value
                                    obj.value = value;
                                    break;
                                }
                            }
                            //add parameter
                            if(!isThere) { searchColumnParams.push({"name": extColumnsName, "value": value}); }
                        }
                    }
                }
            } catch (e) {}

            // zmena url a restovych parametrov ak sa ide vyhladavat
            if (isAnyColumnSearch) {
                url = WJ.urlAddPath(sSource, "/search/findByColumns");
                restParams = searchColumnParams;
            } else if (hasFixedParams) {
                //ak pre volanie /add pridavame fixed params
                restParams = searchColumnParams;
            }

            //console.log("Datatable url: '" + url + "' sSource: "+sSource);

            //page calculations
            var pageSize = paramMap.iDisplayLength;
            var start = paramMap.iDisplayStart;
            var pageNum = (start === 0) ? 0 : (start / pageSize);

            //chceme vsetky zaznamy
            if (pageSize === -1) pageSize = 999999;

            // extract sort information
            var sortCol = paramMap.iSortCol_0;
            var pageSort = paramMap['mDataProp_' + sortCol] + "," + paramMap.sSortDir_0;

            //console.log("Datatable sortCol="+sortCol+" pageSort: '" + pageSort + "'");
            //console.log(paramMap);

            //create new json structure for parameters for REST request
            if (serverSide) {
                restParams.push({"name": "size", "value": pageSize});
                restParams.push({"name": "page", "value": pageNum});
                restParams.push({"name": "sort", "value": pageSort});
            }

            if (typeof breadcrumbLanguage !== 'undefined') {
                restParams.push({"name": "breadcrumbLanguage", "value": breadcrumbLanguage});
            }

            DATA.urlLatest = url;
            DATA.urlLatestParams = restParams;

            //ak mame definovane initialData, tak pouzi najskor tie
            if (typeof DATA.initialData != "undefined" && DATA.initialData != null) {
                //musime najskor overit pageLength, defaultne to posiela len 10 zaznamov
                //console.log("initialData=", DATA.initialData, "autoPageLength=", DATA.autoPageLength);
                if (DATA.autoPageLength<DATA.initialData.content.length) {
                    //reduce DATA.initialData.content to autoPageLength
                    //console.log(DATA.id+" reducing initialData, length=", DATA.initialData.content.length, "autoPageLength=", DATA.autoPageLength);
                    var content = DATA.initialData.content;
                    var newContent = [];
                    for (var i=0; i<DATA.autoPageLength; i++) {
                        newContent.push(content[i]);
                    }
                    DATA.initialData.content = newContent;
                } else if (DATA.autoPageLength>DATA.initialData.size && "groups-datatable"!=DATA.id && (typeof DATA.initialData.forceData=="undefined" || false===DATA.initialData.forceData)) {
                    //in groups-datatable we are sending all data
                    //console.log(DATA.id+" reseting initialData, length=", DATA.initialData.size, "autoPageLength=", DATA.autoPageLength, "data=", DATA.initialData);
                    DATA.initialData = null
                }
            }
            if (typeof DATA.initialData != "undefined" && DATA.initialData != null) {
                //console.log(DATA.id+" initial data: ", DATA.initialData);

                var sourceData = DATA.initialData;
                var totalElements = sourceData.totalElements;
                var data = sourceData.content;

                data.iTotalRecords = totalElements;
                data.iTotalDisplayRecords = totalElements;
                data.options = sourceData.options || {};

                //zresetuj, aby sa nabuduce uz pouzilo ajax volanie
                DATA.initialData = null;

                setTimeout(()=> {
                    fnCallback(data);
                }, 10);

                setTimeout(()=> {
                    //porovnaj sortovanie, ak nesedi voci inicialnemu, musis reloadnut
                    //console.log("sort=", TABLE.order(), "init=", TABLE.DATA.order);
                    if (JSON.stringify(TABLE.order())!=JSON.stringify(TABLE.DATA.order)) {
                        //console.log("Musim reloadnut");
                        TABLE.ajax.reload();
                    }
                }, 200);
            }
            else {
                //console.log(DATA.id+" url=", url, "data=", restParams);

                //finally, make the request
                $.ajax({
                    "dataType": 'json',
                    "type": "GET",
                    "url": url,
                    "data": restParams,
                    "success": function (sourceData) {
                        //console.log("sourceData=", sourceData);

                        if (sourceData.hasOwnProperty("error") && sourceData.error !== null && sourceData.error !== "") {
                            if ("Access is denied" === sourceData.error) {
                                WJ.notifyError(WJ.translate("datatables.accessDenied.title.js"), WJ.translate("datatables.accessDenied.desc.js"));
                                return;
                            } else {
                                WJ.notifyError(WJ.translate("datatables.error.title.js"), sourceData.error);
                                return;
                            }
                        }

                        var totalElements = sourceData.totalElements;
                        var data = sourceData.content;

                        data.iTotalRecords = totalElements;
                        data.iTotalDisplayRecords = totalElements;
                        data.options = sourceData.options || {};
                        data.notify = sourceData.notify || null;

                        //WJ.log("fnCallback2, data=", data);
                        fnCallback(data);
                    }
                });
            }
        }

        function runDataTables() {

            //console.log("runDataTables, DATA 1: ", DATA);
            //DT options: https://datatables.net/reference/option/
            if (typeof DATA.url === "string") {
                //src je URL adresa rest endpointu
                TABLE = $(dataTableInit)
                    .on('xhr.dt', function (e, settings, json, xhr) {

                        //ak neprisli options (napr. pri vyhladavani) zachovaj povodne
                        //console.log("DATA.json=", DATA.json, "json=", json);
                        //if (DATA.json != null) console.log("DATA.json.options=", DATA.json.options);
                        if (DATA.json != null && typeof DATA.json.options != "undefined" && DATA.json.options != null) {
                            let lastOptions = DATA.json.options;
                            //console.log("lastOptions=", lastOptions, "json.options=", json.options, "hsPropertyOptions=", json.hasOwnProperty("options"));
                            if (json.hasOwnProperty("options")==false || json.options == null || $.isEmptyObject(json.options)) {
                                //console.log("Setting lastOptions");
                                json.options = lastOptions;
                            }
                        }

                        DATA.json = json;
                        //console.log("Updating jsonOptions=", json.options, " typeof=", (typeof json.options));

                        updateOptionsFromJson(json);

                        if (json.hasOwnProperty("notify")) {
                            showNotify(json.notify);
                        }

                        if (DATA.onXhr !== null) {
                            DATA.onXhr(TABLE, e, settings, json, xhr);
                        }
                    })
                    .on('preXhr.dt', function ( e, settings, data ) {
                        //console.log("preXhr.dt, data=", data);
                        if (DATA.onPreXhr !== null) {
                            DATA.onPreXhr(TABLE, e, settings, data);
                        }
                    })
                    .DataTable({
                        sAjaxSource: DATA.url,
                        sAjaxDataProp: "",
                        fnDrawCallback: function (oSettings) {
                            //datatables_globalConfig.fn_drawCallback(this.api());
                        },
                        columns: DATA.columns,
                        serverSide: DATA.serverSide,
                        processing: true, //Feature control the processing indicator.
                        fnServerData: datatable2SpringData,
                        rowId: DATA.editorId,
                        order: DATA.order,
                        paging: DATA.paging,
                        rowCallback: function (row, data) {
                            //pozor, tato funkcia je tu 2x pre ajax aj normal load
                            //console.log("createdRow, data=", data.title);
                            if (data.hasOwnProperty("editorFields") && data.editorFields !== null && data.editorFields.hasOwnProperty("rowClass")) {
                                //console.log("Setting class: ", data.editorFields.rowClass);
                                let selected = $(row).hasClass("selected");
                                let highlight = $(row).hasClass("highlight");
                                if ($(row).hasClass("odd")) $(row).attr("class", "odd");
                                else $(row).attr("class", "even");
                                if (selected) $(row).addClass("selected");
                                if (highlight) $(row).addClass("highlight");

                                if (data.editorFields.rowClass !== null) $(row).addClass(data.editorFields.rowClass);
                            }
                            if (DATA.onRowCallback!=null && typeof DATA.onRowCallback == "function") DATA.onRowCallback(TABLE, row, data);
                        },
                        stateSave: DATA.stateSave,
                        stateDuration: 0,
                        stateSaveCallback: dtWJ.stateSaveCallback,
                        stateLoadCallback: dtWJ.stateLoadCallback
                    });
            } else {
                //src su skutocne data v JS objekte
                TABLE = $(dataTableInit).DataTable({
                    data: DATA.src.data,
                    columns: DATA.columns,
                    order: DATA.order,
                    rowCallback: function (row, data) {
                        if (data.hasOwnProperty("editorFields") && data.editorFields !== null && data.editorFields.hasOwnProperty("rowClass")) {
                            //console.log("Setting class: ", data.editorFields.rowClass);
                            if ($(row).hasClass("odd")) $(row).attr("class", "odd");
                            else $(row).attr("class", "even");

                            if (data.editorFields.rowClass !== null) $(row).addClass(data.editorFields.rowClass);
                        }
                        if (DATA.onRowCallback!=null && typeof DATA.onRowCallback == "function") DATA.onRowCallback(TABLE, row, data);
                    }
                });
            }

            if (DATA.paging === false) {
                //schovaj nastavenie velkosti stranky, inak ako takto skaredo som to cez DT API nedokazal spravit
                $("#" + DATA.id + "_wrapper button.buttons-settings").on("click", function () {
                    //console.log("Vypinam: ", (DATA.id+"_wrapper button.buttons-page-length"));
                    $("#" + DATA.id + "_wrapper button.buttons-page-length").parent().hide();
                });
            }

            if (DATA.hideTable) {
                const wrapper = dataTableSelector + '_wrapper';
                //console.log(wrapper);
                //document.getElementById(wrapper.substring(1)).style.display = 'none !important';
                $(wrapper).hide();
            }

            $(dataTableInit).DataTable().columns.adjust();

            $(dataTableSelector).on('click', 'tr', function (e) {
                //Nenasli sa ziadne data, ked kliknem data su undefined
                if (typeof TABLE.row(this).data() == "undefined") return;

                //const {availableGroups, forward} = TABLE.row(this).data();
                //console.log({ id: availableGroups, path: forward })

                const arr = e.target.className.split('-');
                const isSelectBox = arr[arr.length - 1];
                const isCheck = arr[arr.length - 2];
                if (isSelectBox === 'square' && isCheck === 'check') {
                    TABLE.rows().deselect();
                    TABLE.rows({search: 'applied', "page": "current"}).select();
                    // Kontrola ktore su selectnute
                    // const selected = TABLE.rows( {selected:true} ).data();
                }
            });

            $(dataTableInit).on('destroy.dt', function () {
                $('#datatableExportModal').off();
                $('#datatableImportModal').off();
                $(dataTableInit).off();

                // Kontrola ci su na selektore este nejake eventy
                // $.each($._data($(dataTableInit)[0], "events"), function(i, event) {
                //     console.log('EVENTS', i);
                // });
            });

            $(dataTableInit).on('column-visibility.dt', function (e, settings, column, state) {
                //console.log("on data visibility select picker");
                $('select.filter-input').selectpicker(DT_SELECTPICKER_OPTS);
            });

            // aktivuj rezim uprava bunky / bubble
            $(dataTableInit).on('click', 'tbody td', function (e) {

                //console.log("click on tbody, e=", e);

                if ($("body").hasClass("datatable-cell-editing")) {

                    //window.aaa=TABLE.row(this);
                    //console.log("e=", e, "this=", this, "dt=", TABLE.cell(this).index().column);
                    //console.log("e=", e, "row=", TABLE.row(this));

                    if ($(this).hasClass("cell-not-editable")) return;

                    var that = this;

                    TABLE.buttons(".buttons-edit").processing(true);


                    function editCell(json) {

                        TABLE.buttons(".buttons-edit").processing(false);

                        const colIndex = TABLE.cell(that).index().column;
                        let columnName = $(TABLE.column(colIndex).header()).data("dt-field-name")
                        //let datatableColumn = TABLE.settings().init().columns[colIndex];
                        //let columnName = datatableColumn.name;
                        //console.log("columnName=", columnName);

                        //console.log("colIndex=", colIndex, "datatableColumn=", datatableColumn, "columnName=", columnName, "cell=", TABLE.cell(that));

                        EDITOR.bubble($(that), {
                            focus: null,
                            buttons: [
                                {
                                    text: "<i class='far fa-check'></i>",
                                    className: 'btn btn-primary',
                                    action: function () {
                                        this.submit();
                                    }
                                },
                                {
                                    text: "<i class='far fa-times'></i>",
                                    className: 'btn btn-outline-secondary',
                                    action: function () {
                                        this.close();
                                    }
                                }
                            ],
                            onBlur: 'none',
                            submit: 'all'
                        });

                        $("div.DTE_Field").removeClass("show");
                        let selector = "div.DTE_Field_Name_"+columnName.replace(/\./gi, "\\.");
                        selector = selector.replace("_editorFields-", "_editorFields\\.");
                        //console.log("selector=", selector, "columnName=", columnName);
                        $(selector).addClass("show");
                        setTimeout(function() {
                            //presun kurzor do inputu
                            let selector = "div.DTE_Field_Name_"+columnName.replace(/\./gi, "\\.")+".show input";
                            let element = $(selector);
                            //console.log("Done, selector=", selector, " element=", element);
                            if (element.length>0) {
                                try {
                                    element[0].focus();
                                    element[0].setSelectionRange(0,0);
                                } catch (e) {}
                            }
                        }, 700);
                        //reposition the bubble according to element size
                        var liner = $("div.DTE_Bubble_Liner");
                        if (liner.find("div.DTE_Field_Type_quill").length>0) liner.addClass("type_quill");
                        else liner.removeClass("type_quill");

                        EDITOR.bubblePosition();

                        var parentTop = parseInt(liner.parent("div").css("top"));
                        var topPosition = parentTop + parseInt(liner.css("top"));
                        //console.log("topPosition=", topPosition);
                        if (topPosition < 10) {
                            //liner.css("bottom", "auto");
                            liner.css("bottom", topPosition+"px");
                        }
                    }

                    if (TABLE.DATA.serverSide === true && TABLE.DATA.fetchOnEdit === true) {
                        refreshRow(TABLE.row(this).id(), function (json) {
                            editCell(json);
                        });
                    } else {
                        //console.log(TABLE.row(this).id());
                        var json = TABLE.DATA.json[TABLE.row(this).id()];
                        //console.log("json=", json);
                        editCell(json);
                    }

                }

                /*console.log("DTE_Bubble select picker");
                $('.DTE_Bubble select').selectpicker({
                    container: "body",
                    style: "btn btn-outline-secondary",
                    width: "100%"
                });*/


            });

            $("#" + DATA.id + "_wrapper").on("click", ".dt-close-modal", function () {
                $(".dt-button-background").click();
            });

            $("#" + DATA.id + "_wrapper").on("click", ".dt-row-edit a", function () {
                //console.log("dt-row-edit click");
                var row = $(this).parents("tr");

                TABLE.wjEditFetch(row);
            });

            ExportImport.bindImportButton(TABLE, DATA);

            ExportImport.bindExportButton(TABLE, DATA);

            $("#" + DATA.id + "_wrapper, #" + DATA.id + "_extfilter").on('click', '.filtrujem', function (event) {

                //console.log("Filtrujem click");

                event.preventDefault();

                dtWJ.filtrujemClick(this, TABLE, DATA, false);
            });

            //Reset field after removed
            $('#' + DATA.id + '_wrapper .dt-filter-labels').on('click', '.dt-filter-labels__link', function () {
                var index = $(this).attr("data-column-index");
                var th = $('#' + DATA.id + '_wrapper .dataTables_scrollHeadInner tr:last-child th[data-column-index="' + index + '"]');

                $(th).find(".form-control-sm").val("");
                //console.log("Selectpickers=", $(th).find("select"), " index=", index);
                $(th).find("select").selectpicker("val", "");
                $(th).find("select").selectpicker("val", "contains");
                $(th).find(".min").val("");
                $(th).find(".max").val("");
                TABLE.column(index).search("");
                $(th).find(".filtrujem").click();

                dtWJ.fixTableSize(TABLE);
            });

            //Block filter form send
            $(dataTableSelector + ' form').submit(function () {
                return false;
            });

        }
    }

    /**
     * Privatna funkcia na vykonanie AJAX dotazu na zadany action
     * @param {*} action
     * @param {*} ids
     */
    function _executeAction(action, ids, customData = null) {
        $("#"+DATA.id+"_wrapper .dataTables_processing").show();

        $.post({
            url: WJ.urlAddPath(DATA.url, "/action/" + action),
            data: {
                ids: ids,
                customData: customData
            },
            success: function (json) {
                //console.log("Done, json=", json);
                if (typeof json.error != "undefined" && json.error != null && json.error!="") {
                    WJ.notifyError(json.error);
                    WJ.dispatchEvent('WJ.DT.executeActionCancel', {
                        action: action,
                        ids: ids,
                        tableId: DATA.id
                    });
                    return;
                }
                if(typeof json.notify != "undefined" && json.notify != null) {
                    showNotify(json.notify);
                }
                window.setTimeout(function () {
                    //aby mal filesystem cas na zmenu (napr. obrazkov) pred reloadom
                    TABLE.ajax.reload();
                }, 500);
                WJ.dispatchEvent('WJ.DT.executeAction', {
                    action: action,
                    ids: ids,
                    tableId: DATA.id
                });
            },
            error: function (xhr, ajaxOptions, thrownError) {
                //console.log("Error, xhr=", xhr, " ajaxOptions=", ajaxOptions, " thrownError=", thrownError);
                WJ.dispatchEvent('WJ.DT.executeActionCancel', {
                    action: action,
                    ids: ids,
                    tableId: DATA.id
                });
            },
        });
    };

    /**
     * Vyvolanie akcie na serveri
     * @param {String} action - meno akcie
     * @param {boolean} doNotCheckEmptySelection - ak je true nevykona sa kontrola oznacenia aspon jedneho riadku (na server sa posle ID riadku -1)
     * @param {String} confirmText - ak je zadane zobrazi sa pred vykonanim akcie potvrdzovaci dialog so zadanym textom
     */
    TABLE.executeAction = function (action, doNotCheckEmptySelection, confirmText, noteText, customData = null, forceIds = null) {
        //console.log("Execute action="+action);
        var selectedRows = this.rows({selected: true}).data();
        var ids = [];

        if (typeof forceIds != "undefined" && forceIds != null) {
            if (typeof forceIds == "number") {
                ids.push(forceIds);
            } else if (typeof forceIds == "object") {
                ids = forceIds;
            }
        } else {
            for (var i = 0; i < selectedRows.length; i++) {
                var row = selectedRows[i];
                var id = row.id;
                if (typeof id == "undefined") {
                    id = row[TABLE.DATA.columns[0].data];
                }
                ids.push(id);
            }
        }

        if (ids.length === 0) {
            //pre niektore akcie (napr. refresh) nemame nic selectnute, aby nam nepadali Spring metody tak posleme zaporne IDecko
            if (doNotCheckEmptySelection) ids.push(-1);
            else {
                WJ.notifyError(WJ.translate('datatables.execute.prompt.js'));
                return;
            }
        }

        if (typeof confirmText === "undefined" || confirmText === null || confirmText === "") {
            _executeAction(action, ids, customData);
        } else {
            WJ.confirm({
                title: confirmText,
                message: noteText,
                success: function () {
                    _executeAction(action, ids, customData);
                },
                cancel: function () {
                    WJ.dispatchEvent('WJ.DT.executeActionCancel', {
                        action: action,
                        ids: ids,
                        tableId: DATA.id
                    });
                }
            });
        }
    };

    TABLE.hideButtons = function (names) {
        for (let i = 0; i < names.length; i++) {
            TABLE.hideButton(names[i]);
        }
    };

    TABLE.hideButton = function (name) {
        $('#' + DATA.id + '_wrapper [data-dtbtn=' + name + ']').hide();
    };

    TABLE.showButton = function (name) {
        $('#' + DATA.id + '_wrapper [data-dtbtn=' + name + ']').show();
    };

    //vypne rezim editacie bunky (ak je nahodou zapnuty)
    TABLE.cellEditOff = function() {
        //console.log("cellEditOff");
        $('body').removeClass("datatable-cell-editing");
        $('#' + DATA.id + '_wrapper [data-dtbtn=celledit]').removeClass("enabled");
    }

    TABLE.wjEdit = function (row) {
        //console.log("wjEdit, row=", row);
        EDITOR.edit(row, {
            title: '<div class="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.edit') + '</h5></div><div class="col-sm-8" id="dt-header-tabs-' + DATA.id + '"></div></div>'+MAXIMIZE_HTML,
            buttons: DATA.editorButtons
        });
    }

    TABLE.wjEditFetch = function (row) {
        //console.log("EDITING ROW, row=", row, "id=", row[0].id);
        let dataBeforeFetch = null;
        if (typeof row[0].id != "undefined" && row[0].id!="") dataBeforeFetch = TABLE.row("#"+row[0].id).data();
        if (DATA.fetchOnEdit) {
            TABLE.buttons(".buttons-edit").processing(true);
            refreshRow(row[0].id, function (json) {
                TABLE.buttons(".buttons-edit").processing(false);
                let dataAfterFetch = TABLE.row("#"+row[0].id).data();
                if (DATA.onEdit!=null && typeof DATA.onEdit == "function") DATA.onEdit(TABLE, row, dataAfterFetch, dataBeforeFetch);
                else TABLE.wjEdit(row);
            });
        } else {
            if (DATA.onEdit!=null && typeof DATA.onEdit == "function") DATA.onEdit(TABLE, row, dataBeforeFetch, dataBeforeFetch);
            else TABLE.wjEdit(row);
        }
    }

    if (DATA.hideButtons != null) {
        let buttons = DATA.hideButtons.split(",");
        buttons.forEach(item => {
            //console.log("Hiding button ", item);
            TABLE.hideButton(item);
        });
    }

    TABLE.wjUpdateOptions = function(url=null, callback=null) {
        //console.log("wjUpdateOptions, href=", window.location.href);
        if (url == null) url = WJ.urlAddPath(DATA.url, urlSuffix);
        $.get({
            url: url,
            success: function(json) {
                //console.log("wjUpdateOptions success, json=", json);
                updateOptionsFromJson(json);
                if (callback != null) callback();
            }
        });
    }

    TABLE.calculateAutoPageLength = calculateAutoPageLength;

    EDITOR.TABLE = TABLE;
    TABLE.EDITOR = EDITOR;
    TABLE.DATA = DATA;
    EDITOR.DT_SELECTPICKER_OPTS = DT_SELECTPICKER_OPTS;
    EDITOR.DT_SELECTPICKER_OPTS_NOSEARCH = DT_SELECTPICKER_OPTS_NOSEARCH;
    EDITOR.DT_SELECTPICKER_OPTS_EDITOR = DT_SELECTPICKER_OPTS_EDITOR;

    //inicializuj DT opener
    TABLE.datatableOpener = new DatatableOpener();
    TABLE.datatableOpener.dataTable = TABLE;
    if ("id"!=DATA.editorId) {
        //console.log("Setting ID opener key");
        TABLE.datatableOpener.whiteList = [DATA.editorId];
        TABLE.datatableOpener.idKeyName = DATA.editorId;
    }
    if (DATA.idAutoOpener===true) {
        TABLE.datatableOpener.init();
    } else {
        //initialize just hash filter on table
        TABLE.datatableOpener.initHashFilter();
    }

    //nastav tooltip na export a import tlacidlo, BS5 nevie mat naraz toggle dialog aj title
    setTimeout(function() {
        new bootstrap.Tooltip($(".btn-export-dialog"));
        new bootstrap.Tooltip($(".btn-import-dialog"));
    }, 500);

    //bindni upozornenie o konflikte editacie zaznamu viacerymi pouzivatelmi
    dtWJ.bindEditorLocking(EDITOR);

    //bindni CTRL+S na ulozenie
    dtWJ.bindKeyboardSave(EDITOR);

    TABLE.hasPermission = function(action) {
        return hasPermission(action)
    }

    //schovaj tlacidla ak user nema prava
    EDITOR.on('open', function (e, mode, action) {
        //console.log("Open, e=", e, "mode=", mode, "action=", action, "hasPerms=", TABLE.hasPermission(action));
        var hideButtons = false;
        if (TABLE.hasPermission(action)===false) {
            hideButtons = true;
        }
        //console.log("hideButtons=", hideButtons, "selector=", "#"+TABLE.DATA.id+"_modal div.DTE_Footer button.btn-primary")

        if (hideButtons) {
            $("#"+TABLE.DATA.id+"_modal div.DTE_Footer button.btn-primary").hide();
            $("#"+TABLE.DATA.id+"_modal div.DTE_Footer div.edit-append").hide();
        }

        $("#"+TABLE.DATA.id+"_modal").attr("data-mode", mode);
        $("#"+TABLE.DATA.id+"_modal").attr("data-action", action);

        if ("edit"===action) {
            var selectedRows = TABLE.rows({ selected: true }).data();
            if (selectedRows.length<2) {
                //title vypiseme len pri editacii jedneho zaznamu
                let title = dtWJ.getTitle(EDITOR);
                if (title != null && title != "") {
                    $("#"+TABLE.DATA.id+"_modal div.DTE_Header_Content h5.modal-title").text(title);
                }
            }
        }
        if ("remove"===action) {
            var selectedRows = TABLE.rows({ selected: true }).data();
            var titles = $("<ul>");
            if (selectedRows.length>0) {
                for (var i=0; i<selectedRows.length; i++) {
                    //titles += "<li>"+dtWJ.getTitle(EDITOR, selectedRows[i])+"</li>";
                    var li = $("<li>");
                    var title = dtWJ.getTitle(EDITOR, selectedRows[i]);
                    if (title == null || title == "") continue;
                    li.text(title);
                    titles.append(li);
                }
                $("#"+TABLE.DATA.id+"_modal div.DTE_Body div.DTE_Form_Info div.col-sm-7.offset-sm-4").append(titles);
            }
        }
    });

    TABLE.on( 'buttons-action', function ( e, buttonApi, dataTable, node, config ) {
        //console.log( 'Button ', buttonApi, 'config=', config );
        if (typeof config.className !="undefined" && config.className.indexOf("buttons-colvis")!=-1 && $('#' + DATA.id + '_wrapper button.buttons-columnVisibility').parents("div.colvisbtn_wrapper").length<1) {

            $('#' + DATA.id + '_wrapper button.buttons-columnVisibility span[data-toggle*="tooltip"]').tooltip({
                placement: 'top',
                trigger: 'hover',
                html: true
            });

            $('#' + DATA.id + '_wrapper button.buttons-columnVisibility').wrapAll( "<div class='colvisbtn_wrapper' />");
            $('#' + DATA.id + '_wrapper button.colvis-postfix').wrapAll( "<div class='colvispostfix_wrapper' />");
            $('#' + DATA.id + '_wrapper button.colvis-prefix').wrapAll( "<div class='colvisprefix_wrapper' />");

            try {
                //with column reorder there are duplicate buttons, remove it
                $('#' + DATA.id + '_wrapper .colvisprefix_wrapper button:nth-last-child(2)').remove();

                $('#' + DATA.id + '_wrapper .colvispostfix_wrapper button:nth-last-child(6)').remove();
                $('#' + DATA.id + '_wrapper .colvispostfix_wrapper button:nth-last-child(6)').remove();
                $('#' + DATA.id + '_wrapper .colvispostfix_wrapper button:nth-last-child(6)').remove();
                $('#' + DATA.id + '_wrapper .colvispostfix_wrapper button:nth-last-child(6)').remove();
                $('#' + DATA.id + '_wrapper .colvispostfix_wrapper button:nth-last-child(6)').remove();
            } catch (e) {}
        }
    } );

    window.addEventListener("WJ.DTE.opened", function() {
        dtWJ.resizeTabContent(EDITOR);
    });
    dtWJ.bindOnResize(TABLE, DATA);
    dtWJ.bindDialogDragDrop(TABLE);
    dtWJ.bindColumnReorder(TABLE);

    return TABLE;
}
