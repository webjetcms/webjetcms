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
import * as fieldTypeIframe from './field-type-iframe';
import * as fieldTypeColor from './field-type-color';
import * as fieldTypeBase64 from './field-type-base64';
import * as fieldTypeStaticText from './field-type-static-text';
import * as fieldTypeWjupload from './field-type-wjupload';
import * as fieldTypeImageRadio from './field-type-imageradio';
import * as fieldTypeIcon from './field-type-icon';
import * as dtWJ from './datatables-wjfunctions';
import * as CustomFields from './custom-fields';
import * as ExportImport from './export-import';
import * as RowReorder from './row-reorder';
import * as FooterSum from './footer-sum';
import {DatatableOpener} from "../../src/js/libs/data-tables-extends/";
import {EditorAi} from './editor-ai'

const bootstrap = window.bootstrap = require('bootstrap');
import $ from 'jquery';
//console.log("Setting jQuery object to window");
window.jQuery = $;
window.$ = $;

window.dtWJ = dtWJ;

require('datatables.net');
require('datatables.net-bs5');
require('datatables.net-editor-bs5');
//require('datatables.net-autofill-bs5');
require('datatables.net-buttons-bs5');
require('datatables.net-buttons/js/buttons.colVis.js');
//WebJET fixnuta verzia - problem s prefixButtons a postfixButtons
require('./buttons.colVis');
require('datatables.net-buttons/js/buttons.html5.js');
require('datatables.net-buttons/js/buttons.print.js');
require('datatables.net-colreorder-bs5');
//require('datatables.net-fixedcolumns-bs5');
//require('datatables.net-fixedheader-bs5');
//require('datatables.net-keytable-bs5');
//require('datatables.net-responsive-bs5');
//require('datatables.net-rowgroup-bs5');
require('datatables.net-rowreorder-bs5');
//require('datatables.net-scroller-bs5');
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
    //local JSON data (do not use REST service)
    DATA.src = options.src ? options.src : null;
    DATA.fields = []; //polia pre DT editor
    DATA.serverSide = options.serverSide ? options.serverSide : false;
    DATA.tabsFolders = options.tabs ? options.tabs : [
        //always show basic tab
        { id: 'basic', title: WJ.translate('datatable.tab.basic'), selected: true }
    ];
    DATA.url = options.url ? options.url : null;
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
    DATA.editorButtons = options.editorButtons ? options.editorButtons : '<i class="ti ti-check"></i> ' + WJ.translate('button.save');
    DATA.createButtons = options.createButtons ? options.createButtons : '<i class="ti ti-check"></i> ' + WJ.translate('button.add');
    DATA.keyboardSave = (typeof options.keyboardSave !== "undefined") ? options.keyboardSave : true;
    DATA.stateSave = (typeof options.stateSave !== "undefined") ? options.stateSave : true;
    DATA.autoHeight = (typeof options.autoHeight !== "undefined") ? options.autoHeight : true;
    DATA.customFieldsUpdateColumns = (typeof options.customFieldsUpdateColumns !== "undefined") ? options.customFieldsUpdateColumns : false;
    DATA.customFieldsUpdateColumnsPreserveVisibility = (typeof options.customFieldsUpdateColumnsPreserveVisibility !== "undefined") ? options.customFieldsUpdateColumnsPreserveVisibility : false;
    //If we have editor that we dont close after save, but we want to update editor data after save
    DATA.updateEditorAfterSave = (typeof options.updateEditorAfterSave !== "undefined") ? options.updateEditorAfterSave : false;

    //false - we don't want to hash filter for this datatable
    DATA.initHashFilter = (typeof options.initHashFilter !== "undefined") ? options.initHashFilter : true;

    //nastavenie predvoleneho vyhladavania
    DATA.defaultSearch = (typeof options.defaultSearch !== "undefined") ? options.defaultSearch : null;

    //uklada automaticku velkost strany (prva polozka)
    DATA.autoPageLength = 10;
    DATA.autoPageLengthTitle = DATA.autoPageLength;

    //allow editor locking notifications
    DATA.editorLocking = (typeof options.editorLocking !== "undefined") ? options.editorLocking : true;

    // setting to generate sum of selected columns
    DATA.summary = (typeof options.summary !== "undefined" && typeof options.summary == "object") ? options.summary : null;

    //local data do not use REST services
    DATA.isLocalJson = false;
    if (typeof DATA.src !== "undefined" && DATA.src != null) DATA.isLocalJson = true;

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
        noneSelectedText: '\xa0', //nbsp
        multipleSeparator: " + "
    };

    var DT_SELECTPICKER_OPTS_NOSEARCH = {
        container: "body",
        style: "btn btn-sm btn-outline-secondary",
        width: "100%",
        noneSelectedText: '\xa0', //nbsp
        multipleSeparator: " + "
    };

    var DT_SELECTPICKER_OPTS_EDITOR = {
        container: "body",
        style: "btn btn-outline-secondary",
        liveSearch: true,
        showSubtext: true,
        noneSelectedText: '\xa0', //nbsp
        multipleSeparator: " + "
    };

    const DIALOG_BUTTONS = '<div class="dialog-buttons"><span class="show-help" onclick="WJ.showHelpWindow()"><i class="ti ti-help" title="' + WJ.translate('button.help') + '" data-toggle="tooltip"></i></span><span class="maximize"><i class="ti ti-arrows-maximize" title="'+WJ.translate("datatables.modal.maximize.js")+'" data-toggle="tooltip"></i></span><span class="minimize"><i class="ti ti-arrows-minimize" title="'+WJ.translate("datatables.modal.minimize.js")+'" data-toggle="tooltip"></i></span></div>';

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
    function prepareCustomFieldsDataBeforeSend(data, me) {
        //Array of fields (field names)
        var fields = me.fields();

        var letters = 'ABCDEFGHIJKLMNOPQRST';
        for(var i in data.data) {
            for(var letter in letters) {
                if(Array.isArray(data.data[i]['field' + letters[letter]])) {
                    data.data[i]['field' + letters[letter]] = data.data[i]['field' + letters[letter]].join('|')
                }

                let fieldName = 'field' + letters[letter];
                if(fields.includes(fieldName)) {
                    let field = me.field(fieldName);
                    let renderFormat = field.s.opts.renderFormat;

                    //Its quill editor - get html content and set into data as value
                    if("dt-format-quill" == renderFormat) {
                        let input = field.s.opts._input;
                        let value = input.html();
                        if ("<p><br></p>"==value) value = "";
                        data.data[i][fieldName] = value;
                }
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
        var url = TABLE.getAjaxUrl();
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
                    dtWJ.updateFilterSelect(DATA, fieldName);
                }
            }
        }

        //console.log("Fixing options width")
        //fixni sirky stlpcov ked sa zmenia sirky optionov
        setTimeout(function () {
            dtWJ.adjustColumns(TABLE);
        }, 100);

        if (DATA.customFieldsUpdateColumns===true && Array.isArray(json.data) && json.data.length>0) {
            let fieldsDefinition = json.data[0]?.editorFields?.fieldsDefinition;
            if (typeof fieldsDefinition != "undefined" && fieldsDefinition != null) {
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
                    $(column.header()).find(".dt-column-title").text(""+customField.label);
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

        //update checkboxes
        setTimeout(()=> {
            //console.log("Updating checkboxes:", $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]'));
            $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').parent("div").addClass("custom-control form-switch");
            $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').addClass("form-check-input");
            $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').siblings("label").addClass("form-check-label");

            //refresh selectpickers
            $('#' + DATA.id + '_modal .DTE_Form_Content').find('select').selectpicker('refresh');
        }, 100);
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
            var breadcrumbHeight = 0;
            var breadcrumbElemets = $("div.md-breadcrumb");
            if (breadcrumbElemets.length>0) {
                //iterate all breadcrumbs and get total sum of outerHeight
                breadcrumbElemets.each(function() {
                    var $this = $(this);

                    //console.log("display=", $this.css("display"));
                    if ($this.css("display")=="none") return;

                    //do not count if $this has .tree-col parent
                    if ($this.parents(".tree-col").length>0) {
                        //console.log("breadcrumb has tree-col parent");
                        return;
                    }

                    //console.log("breadcrumb=", $this);
                    breadcrumbHeight += $this.outerHeight();
                });
            }

            if ($("html").hasClass("in-iframe-show-table")) breadcrumbHeight = -18; //finta aby sa nam tam standardne vosiel o riadok viac, 18 kvoli galerii

            //fixed values, because datatable is not constructed yet
            var dtToolbarRowHeight = 48;
            var dtScrollHeadHeight = 66;
            var dtFooterHeight = 48;

            if (DATA.autoHeight === true && typeof DATA.defaultSearch === "object" && DATA.defaultSearch != null) {
                dtScrollHeadHeight = dtScrollHeadHeight + 31;
            }

            if ($(window).width()<1200) {
                headerHeight = 0;
                dtFooterHeight = 38;
            }

            var scrollbarWidth = dtWJ.getScrollbarWidth();

            height = height - headerHeight - breadcrumbHeight - dtToolbarRowHeight - dtScrollHeadHeight - dtFooterHeight - scrollbarWidth;
            //console.log("height=", height, "headerHeight=", headerHeight, "breadcrumbHeight=", breadcrumbHeight, "dtToolbarRowHeight=", dtToolbarRowHeight, "dtScrollHeadHeight=", dtScrollHeadHeight, "dtFooterHeight=", dtFooterHeight, "scrollbarWidth=", scrollbarWidth);

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

                    var colWidth = $("#"+DATA.id+"_wrapper .dt-scroll-body").width();
                    if (typeof colWidth == "undefined") colWidth = $("#"+DATA.id).width() + 15 + 15; //before render it has 15 px padding on left and right

                    var columns = Math.floor(colWidth / imgWidth);
                    var rows = Math.floor((height + dtScrollHeadHeight) / imgHeight);
                    //console.log("colWidth=", colWidth, "imgWidth=", imgWidth, "dtScrollHeadHeight=", dtScrollHeadHeight, "height=", height, "imgHeight=", imgHeight, "columns=", columns, "rows=", rows, "breadcrumbHeight=", breadcrumbHeight);

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
                if (typeof col.ai != "undefined") col.editor.ai = col.ai;
                if (typeof col.entityDecode != "undefined") col.editor.entityDecode = col.entityDecode;

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
            var fieldName = col.data.replace(/\./gi, "-");
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
                    dte: dte,
                    id: dte.TABLE.DATA.id,
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
                close: $('<button class="close btn-close-editor" data-toggle="tooltip"><i class="ti ti-x"></i>')
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

            //If showOnlyEditor, set it as full screen and hide close/minimalize button's
            if(window.location.href.indexOf("showOnlyEditor=true") != -1) {
                $(dom.content).find("div.modal-dialog").addClass("modal-fullscreen");
                if (false === DATA.nestedModal) {
                    //hide close button for main dialog window
                    $(append).find("button.btn-close-editor").hide();
                }
                $(append).find("div.dialog-buttons").hide();
            }

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
                dte: dte,
                id: dte.TABLE.DATA.id
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
                //console.log("Setting val for=", fields[i], " val=", value);
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
        $.fn.dataTable.Editor.fieldTypes.iframe = fieldTypeIframe.typeIframe();
        $.fn.dataTable.Editor.fieldTypes.color = fieldTypeColor.typeColor();
        $.fn.dataTable.Editor.fieldTypes.base64 = fieldTypeBase64.typeBase64();
        $.fn.dataTable.Editor.fieldTypes.staticText = fieldTypeStaticText.typeStaticText();
        $.fn.dataTable.Editor.fieldTypes.wjupload = fieldTypeWjupload.typeWjupload();
        $.fn.dataTable.Editor.fieldTypes.imageRadio = fieldTypeImageRadio.typeImageRadio();
        $.fn.dataTable.Editor.fieldTypes.icon = fieldTypeIcon.typeIcon();

        fieldTypeSelectEditable.typeSelectEditable();

        $.fn.dataTable.ext.buttons.edit.action = function (e, dt, node, config) {
            //console.log("edit.action config=", config, "e=", e);
            var editor = config.editor;
            var rows = $("#"+editor.TABLE.DATA.id+" tr.selected");
            //fetch will be done before this call, so use just wjEdit
            editor.TABLE.wjEdit(rows);
        };

        $.fn.dataTable.ext.buttons.create.action = function (e, dt, node, config) {
            //console.log("create.action config=", config, "e=", e);
            var editor = config.editor;
            var rows = $("#"+editor.TABLE.DATA.id+" tr.selected");
            editor.TABLE.wjCreate(rows);
        };

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
            dt.on('select.dt.DT deselect.dt.DT draw.dt.DT', function () {
                button.enable(dt.rows({selected: true}).any());
            });

            button.disable();
        };

        //funkcia pre enable/disable tlacitka ked nie je nic oznacene
        $.fn.dataTable.Buttons.showIfRowUnselected = function (button, dt) {
            dt.on('select.dt.DT deselect.dt.DT draw.dt.DT', function () {
                button.enable(!dt.rows({selected: true}).any());
            });

            button.enable();
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

        let ajaxConf;
        if (DATA.isLocalJson === false) {
            ajaxConf = {
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
            }
        }

        var EDITOR = new $.fn.dataTable.Editor({
            ajax: ajaxConf,
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
                    title: '<div class="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.add') + '</h5></div><div class="col-sm-8" id="dt-header-tabs-' + DATA.id + '"></div></div>'+DIALOG_BUTTONS,
                    submit: '<i class="ti ti-check"></i> ' + WJ.translate('button.add')
                },
                edit: {
                    button: WJ.translate('button.edit'),
                    title: '<div class="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.edit') + '</h5></div><div class="col-sm-8" id="dt-header-tabs-' + DATA.id + '"></div></div>'+DIALOG_BUTTONS,
                    submit: '<i class="ti ti-check"></i> ' + WJ.translate('button.save')
                },
                remove: {
                    button: WJ.translate('button.delete'),
                    title: '<clas="row"><div class="col-sm-4"><h5 class="modal-title">' + WJ.translate('button.delete') + '</h5></div><div class="col-sm-8"></div></div>',
                    submit: '<i class="ti ti-trash"></i> ' + WJ.translate('button.delete'),
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
                    title: '<i class="ti ti-pencil"></i> ' + WJ.translate('datatables.values.edit.title.js'),
                    info: WJ.translate('datatables.values.edit.info.js'),
                    restore: '<i class="ti ti-reload"></i> ' + WJ.translate('datatables.values.edit.restore.js')
                }
            }
        })

        EDITOR.on('preSubmit', function (e, data, action) {

            const me = this;

            // upravi multiple volne polia
            prepareCustomFieldsDataBeforeSend(data, me);

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
            const eventSubmit = new CustomEvent('WJ.DTE.submitclick', {
                detail: {
                    element: this,
                    action: 'submit'
                }
            });
            window.dispatchEvent(eventSubmit);
        });
        EDITOR.on('close', function (e) {
            //console.log("Editor.on close, editor=", EDITOR, "url=", EDITOR.TABLE.DATA.url, "close=", EDITOR.close, "e=", e);

            //pre istotu zatvor, niekedy pri nested dialogu zostava otvoreny kvoli nejakemu bugu
            if (typeof EDITOR._bootstrapDisplay != "undefined" && $("#" + EDITOR._bootstrapDisplay.id).hasClass("show")) {
                //console.log("IS NOT CLOSED, CLOSING MANUALLY");
                $.fn.dataTable.Editor.display.bootstrap.close(EDITOR, null);
            }
            setTimeout(()=> {
                dtWJ.adjustColumns(e.currentTarget.TABLE);
            }, 1000);
            const eventClose = new CustomEvent('WJ.DTE.closeclick', {
                detail: {
                    element: this,
                    action: 'close'
                }
            });
            window.dispatchEvent(eventClose);
        });
        EDITOR.on('submitSuccess', function (e, json, data, action) {
            //console.log("Editor.on submitSuccess, json=", json, "DATA=", DATA, "data=", data, "action=", action);
            if (DATA.isLocalJson === false) {
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

                    if(DATA.updateEditorAfterSave == true) {
                        EDITOR.setJson(data);
                    }

                    if(typeof json.notify != "undefined" && json.notify != null) {
                        showNotify(json.notify);
                    }

                    //nastav checkboxy, toto treba ppo kazdom SUBMIT-e, pretoze sa nam menia moznosti select boxov
                    $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').parent("div").addClass("custom-control form-switch");
                    $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').addClass("form-check-input");
                    $('#' + DATA.id + '_modal .DTE_Form_Content').find('input[type="checkbox"]').siblings("label").addClass("form-check-label");
                }, 300);
            }
        });

        EDITOR.on('submitUnsuccessful', function (e, json) {
            //console.log("Editor.on submitUnsuccessful, json=", json);

            if(typeof json.notify != "undefined" && json.notify != null) {
                showNotify(json.notify);
            }
        });

        if (DATA.fetchOnCreate) {
            //ak je zapnute volanie servera pre novy zaznam zavolame ajax a pockame na data pre novy zaznam (napr. pre web stranku)
            EDITOR.on('initCreate', function (e) {
                return new Promise(function (resolve, reject) {
                    let url = TABLE.getAjaxUrl();
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
                    tabsHtml += '<div class="md-tabs">';
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
                $('#' + DATA.id + '_modal div.DTE_Header div.dialog-buttons .maximize, #' + DATA.id + '_modal div.DTE_Header div.dialog-buttons .minimize').on("click", function() {
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
                            $(el).parents('[data-dte-e="input"]').after('<div class="col-sm-1 form-group-tooltip"><button type="button" tabindex="-1" class="btn btn-link btn-tooltip" data-toggle="tooltip" title="' + tooltipText + '" data-html="true"><i class="ti ti-info-circle"></i></button></div>');
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
                $('#' + DATA.id + '_modal .DTE_Form_Buttons').prepend('<span class="buttons-footer-left"></span> <button type="button" class="btn btn-outline-secondary btn-close-editor"><i class="ti ti-x"></i> ' + WJ.translate('button.cancel') + '</button>');

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

                    $.each($('#' + DATA.id + '_modal .DTE_Body').find('[data-dt-field-headline]'), function (key, el) {

                        var headlineText = $(el).attr("data-dt-field-headline");

                        if (headlineText.length > 0) {
                            $(el).parents('.DTE_Field').before('<div class="form-group row row-headline"><div class="col-sm-7 offset-sm-4"><h5>' + headlineText + '</h5></div></div>');
                        }
                    });

                    $.each($('#' + DATA.id + '_modal .DTE_Body').find('[data-dt-field-full-headline]'), function (key, el) {

                        var headlineText = $(el).attr("data-dt-field-full-headline");

                        if (headlineText.length > 0) {
                            $(el).parents('.DTE_Field').prepend('<div class="form-group row row-full-headline"><div class="col-sm-12"><h5>' + headlineText + '</h5></div></div>');
                        }
                    });

                    $('#' + DATA.id + '_modal .DTE_Body [data-toggle*="tooltip"]').tooltip({
                        placement: 'top',
                        trigger: 'hover',
                        html: true
                    });

                    $('#' + DATA.id + '_modal div.DTE_Field_InputControl select').each(function () {
                        const $select = $(this);
                        const separator = $select.attr('separator');
                        let opts = { ...DT_SELECTPICKER_OPTS_EDITOR };
                        if (separator) {
                            opts.multipleSeparator = separator;
                        }
                        //console.log("Setting selectpicker, opts=", opts, " el=", $select);
                        $select.selectpicker(opts);
                    });
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
                    //console.log("Refreshing selectpicker");
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

                if (typeof TABLE === "undefined") return true;

                var isOk = true;

                $.each($('#' + TABLE.DATA.id + '_wrapper .input-group[data-filter-type="number"]'), function (key, group) {

                    var min = parseInt($(group).find('.min').val()),
                        max = parseInt($(group).find('.max').val()),
                        index = $(group).parents("th").attr("data-dt-column"),
                        val = parseInt(data[index]) || 0;

                    if (!dtConfig.isRangeOk(min, max, val)) isOk = false;

                });

                $.each($('#' + TABLE.DATA.id + '_wrapper .input-group[data-filter-type="number-decimal"]'), function (key, group) {

                    var min = parseFloat($(group).find('.min').val(), 10),
                        max = parseFloat($(group).find('.max').val(), 10),
                        index = $(group).parents("th").attr("data-dt-column"),
                        val = parseFloat(data[index], 10).toFixed(2) || 0;

                    if (!dtConfig.isRangeOk(min, max, val)) isOk = false;
                });

                //console.log("Settings: ", settings, " dataIndex=", dataIndex);

                $.each($('#' + TABLE.DATA.id + '_wrapper .input-group[data-filter-type="boolean"]'), function (key, group) {

                    var option = $(group).find('option:selected').val(),
                        index = $(group).parents("th").attr("data-dt-column"),
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

                $.each($('#' + TABLE.DATA.id + '_wrapper .dt-scroll-headInner .input-group[data-filter-type="date"], #' + TABLE.DATA.id + '_wrapper .dt-scroll-headInner .input-group[data-filter-type="datetime"], #' +
                + TABLE.DATA.id + '_wrapper .dt-scroll-headInner .input-group[data-filter-type="timehm"], #' + + TABLE.DATA.id + '_wrapper .dt-scroll-headInner .input-group[data-filter-type="timehms"]'), function (key, group) {

                    var min = $(group).find('.min').val(),
                        max = $(group).find('.max').val(),
                        index = $(group).parents("th").attr("data-dt-column"),
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
                text: "<i class='ti ti-plus'></i>",
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
                text: "<i class='ti ti-pencil'></i>",
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
                text: "<i class='ti ti-copy'></i>",
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
                                        title: '<div class="row"><div class="col-12"><h5 class="modal-title">' + WJ.translate('button.duplicate') + '</h5></div><div class="col-12" id="dt-header-tabs-' + DATA.id + '"></div></div>'+DIALOG_BUTTONS,
                                        buttons: '<i class="ti ti-copy"></i> ' + WJ.translate('button.duplicate')
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
                                title: '<div class="row"><div class="col-12"><h5 class="modal-title">' + WJ.translate('button.duplicate') + '</h5></div><div class="col-12" id="dt-header-tabs-' + DATA.id + '"></div></div>'+DIALOG_BUTTONS,
                                buttons: '<i class="ti ti-check"></i> ' + WJ.translate('button.duplicate')
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
                text: "<i class='ti ti-trash'></i>",
                editor: EDITOR,
                className: 'btn-danger buttons-divider',
                attr: {
                    title: WJ.translate('button.delete'),
                    "data-toggle": "tooltip",
                    "data-dtbtn": "remove"
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

        //RIGHT SIDE, floated right, so REVERSE ORDER
        buttonsList.push({
            extend: 'collection',
            text: '<i class="ti ti-adjustments-horizontal"></i>',
            className: 'btn-outline-secondary buttons-settings',
            attr: {
                title: WJ.translate('datatables.button.settings.js'),
                "data-toggle": "tooltip",
                "data-dtbtn": "settings"
            },
            buttons: [
                {
                    extend: 'colvis',
                    text: '<i class="ti ti-columns"></i> ' + WJ.translate('datatables.button.cellvisibility.js'),
                    autoClose: false,
                    attr: {
                        title: WJ.translate('datatables.button.cellvisibility.js'),
                        "data-toggle": "tooltip",
                        "data-dtbtn": "cellvisibility"
                    },
                    columnText: function ( dt, idx, title ) {
                        //console.log("columnText, dt=", dt, "idx=", idx, "title=", title, "columns=", DATA.columns);
                        let columnText = title;

                        //find original index in DATA.columns
                        let dataIdx = idx;
                        for (let i = 0; i < DATA.columns.length; i++) {
                            if (DATA.columns[i].data == dt.column(idx).dataSrc()) {
                                dataIdx = i;
                                break;
                            }
                        }

                        try {
                            //zober aktualny title namiesto fieldA...
                            //columnText = title+"-"+DATA.columns[dataIdx].title+"-"+dataIdx+"-"+idx;
                            columnText = DATA.columns[dataIdx].title;
                            //console.log("columnText=", columnText, "data=", DATA.columns[idx]);
                        } catch (e) {}

                        let tab = DATA.columns[dataIdx]?.editor?.tab;
                        //console.log("tab=", tab);

                        let tabTitle = "";
                        //use default tab title if there is just one tab as default
                        if (DATA.tabsFolders.length==1) tabTitle = WJ.translate('datatable.tab.basic');
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

                        let headline = "";

                        //search backward, find headline on current tab
                        for (let i = dataIdx; i >= 0; i--) {
                            if (tab == DATA.columns[i]?.editor?.tab && DATA.columns[i]?.editor?.attr) headline =  DATA.columns[i].editor.attr["data-dt-field-headline"];
                            if (typeof headline != "undefined" && headline != null && headline!="") break;
                        }
                        //console.log("headline=", headline);
                        if (typeof headline == "undefined") headline = "";
                        if (tabTitle == headline) headline = "";
                        if (columnText == headline) headline = "";

                        //tooltip
                        let tooltipHtml = "";
                        let tooltipText = "";
                        if (DATA.columns[dataIdx]?.editor?.message) tooltipText =  DATA.columns[dataIdx].editor.message;
                        if (typeof tooltipText != "undefined" && tooltipText != "") {
                            tooltipHtml = `<span class="btn btn-link btn-tooltip" data-toggle="tooltip" title="${tooltipText}"><i class="ti ti-info-circle"></i></span>`;
                        }

                        if (headline != "") headline = headline + "&nbsp;";

                        columnText = `<span class="tab-title">${tabTitle}</span><span class="tab-columntext"><span class="tab-headline">${headline}</span><span class="column-title">${columnText}</span></span><span class="btn-tooltip">${tooltipHtml}</span>`;

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
                            text: '<i class="ti ti-square-check"></i> ' + WJ.translate('datatables.button.show_all.js'),
                            show: ':hidden',
                            className: 'colvis-postfix btn-outline-secondary'
                        },
                        {
                            extend: 'colvisGroup',
                            text: '<i class="ti ti-square"></i> ' + WJ.translate('datatables.button.hide_all.js'),
                            hide: ':visible:not(.test)',
                            className: 'colvis-postfix btn-outline-secondary'
                        },
                        {
                            extend: 'colvisRestore',
                            text: '<i class="ti ti-refresh"></i> ' + WJ.translate('datatables.button.restore.js'),
                            className: 'colvis-postfix btn-outline-secondary dt-close-modal',
                            action: function(e, dt, node, config) {
                                //console.log("Colvis restore, TABLE=", TABLE, "this=", this, "dt=", dt);
                                window.dataTableCellVisibilityService.removeColumnConfig(dt.editor().TABLE);
                                dtWJ.stateReset(dt.editor().TABLE);
                            }
                        },
                        {
                            text: '<i class="ti ti-check"></i> ' + WJ.translate('button.save'),
                            className: 'colvis-postfix btn-primary dt-close-modal',
                            action: function(e, dt, node, config) {
                                window.dataTableCellVisibilityService.buildConfigDataFromObject(dt.editor().TABLE);
                                //console.log("Zmena stlpcov, fixujem selecty, dt=", dt.editor(), "config=", config);
                                dtWJ.initializeHeaderFilters('#' + dt.editor().TABLE.DATA.id + '_wrapper', false, dt.editor().TABLE.DATA, dt.editor().TABLE);
                            }
                        },
                        {
                            text: '<i class="ti ti-x"></i> ' + WJ.translate('button.cancel'),
                            className: 'colvis-postfix btn btn-outline-secondary dt-close-modal'
                        }
                    ]
                },
                {
                    name: "pageLengthMenu",
                    extend: 'pageLength',
                    text: '<i class="ti ti-layout-rows"></i> ' + WJ.translate('datatables.button.pagelength.js'),
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
                            text: '<i class="ti ti-check"></i> ' + WJ.translate('button.save'),
                            className: 'btn-primary dt-close-modal'
                        },
                        {
                            text: '<i class="ti ti-x"></i> ' + WJ.translate('button.cancel'),
                            className: 'btn btn-outline-secondary dt-close-modal'
                        }
                    ]
                },
            ]
        });

        buttonsList.push({
            name: "reload",
            text: "<i class='ti ti-refresh'></i>",
            editor: EDITOR,
            className: 'btn-outline-secondary buttons-refresh buttons-right',
            attr: {
                title: WJ.translate('datatables.button.reload.js'),
                "data-toggle": "tooltip",
                "data-dtbtn": "reload"
            },
            action: function (e, dt, node, config) {
                dt.ajax.reload();
            }
        });

        if (hasPermission("create") || hasPermission("edit")) {
            buttonsList.push({
                text: "<i class='ti ti-download'></i>",
                className: 'btn-outline-secondary  btn-import-dialog buttons-divider buttons-right',
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
                        if (typeof col.attr != "undefined" && col.attr["data-dt-import-hidden"] === "true") {
                            //toto nechceme zobrazovat v moznosti parovania importu
                            if (col.name === "id" && select.options.length === 1) {
                                //remove also ID option
                                select.options.length = 0;
                            }
                            return;
                        }

                        //skip non exportable columns
                        if(typeof col.className != "undefined" && col.className.indexOf("not-export") != -1) return;

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

        buttonsList.push({
            text: "<i class='ti ti-upload'></i>",
            className: 'btn-outline-secondary btn-export-dialog buttons-right',
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


        //console.log("buttonsList=", buttonsList);

        calculateAutoPageLength(false);

        $.fn.dataTable.ext.errMode = function(settings, tn, msg) {
            console.error("DataTables error: ", msg, "tn=", tn, "settings=", settings);
            WJ.notifyWarning(WJ.translate("text.warning"), msg, 10000);
            dtWJ.stateResetLocalStorage(settings);
            dtWJ.stateReset(TABLE);
            if (msg.indexOf("ColReorder - column count mismatch") != -1) {
                //console.log("Invalid JSON response, reloading table");
                TABLE.ajax.reload();
            }
        }

        $.extend(true, $.fn.dataTable.defaults, {

            //Dom tree
            dom: "<'dt-header-row clearfix'<'row'<'col-auto'B><'col ps-0 pe-0'<'dt-filter-labels'>>>>" +
                "<'row'<'col'<'rounded-bg'tr>>>" +
                "<'row dt-footer-row'<'col'i><'col-auto col-sum'><'col-auto g-0'p>>",

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
                    sFirst: '<i class="ti ti-chevrons-left"></i>',
                    sLast: '<i class="ti ti-chevrons-right"></i>',
                    sNext: '<i class="ti ti-chevron-right"></i>',
                    sPrevious: '<i class="ti ti-chevron-left"></i>'
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

                const timeout = DATA.isLocalJson ? 100 : 0;

                //we need to have timeout because on localData TABLE is not yet initialized
                //with REMOTE data it is initialized because of REST service call
                setTimeout(()=> {

                    //console.log("init, this=", this, "settings=", settings, "json=", json);
                    //console.log("initComplete, TABLE=", TABLE.DATA.id);
                    dtWJ.fixDatatableHeaderInputs(TABLE);

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
                    $('.dt-container select').selectpicker(DT_SELECTPICKER_OPTS);*/

                    $('#' + DATA.id + '_wrapper').on('click', '.buttons-select-all', function (e) {
                        if (TABLE.rows({selected:true}).count()>0) {
                            TABLE.rows().deselect();
                        } else {
                            TABLE.rows({"filter": "applied", "page": "current"}).select();
                        }
                        return false;
                    });
                }, timeout);
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
                    targets: "dt-format-filesize",
                    className: "dt-style-number",
                    type: "num",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderNumberDecimal(td, type, rowData, row, "0.00 b");
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
                    targets: "dt-format-image-notext",
                    className: "dt-style-image",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderImage(td, type, rowData, row, false);
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
                },
                {
                    targets: "dt-format-row-reorder",
                    render: function (td, type, rowData, row) {
                        return RowReorder.renderRowReorder(td, type, rowData, row, dtConfig);
                    }
                },
                {
                    targets: "dt-format-color",
                    className: "dt-style-color",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderColor(td, type, rowData, row);
                    }

                },
                {
                    targets: "dt-format-icon",
                    className: "dt-style-icon",
                    render: function (td, type, rowData, row) {
                        return dtConfig.renderIcon(td, type, rowData, row);
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
        if (extfilterExists) {
            //we must construct filters before DT initialization to move it to extfilters, because the column could be hidden after initialization
            dtWJ.initializeHeaderFilters(dataTableSelector, true, DATA, null);
        }
        runDataTables();
        dtWJ.initializeHeaderFilters(dataTableSelector+"_wrapper div.dt-scroll-head table ", false, DATA, TABLE);

        function datatable2SpringData(data, fnCallback, oSettings) {

            var sSource = DATA.url;

            //console.log("datatable2SpringData, data:", data, "\nfnCallback:", fnCallback, "\noSettings:", oSettings, "\nTABLE:", TABLE, "\nsSource:", sSource);

            if (typeof DATA.defaultSearch === "object" && DATA.defaultSearch != null) {
                //nastav hodnoty default search fieldov
                for (const property in DATA.defaultSearch) {
                    const value = DATA.defaultSearch[property];
                    //console.log("Setting default search, property=", property, "value=", value, "input=", $(property).length);
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
                        var $th = $('#' + DATA.id + '_wrapper .dt-scroll-headInner tr:last-child th[data-dt-field-name="' + propertyName + '"]');
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
                //sSource = TABLE.ajax.url();
            }

            var serverSide = "true" === this.attr("data-server-side");
            var url = WJ.urlAddPath(sSource, urlSuffix);
            if (typeof TABLE !== 'undefined') {
                var ajaxUrl = TABLE.getAjaxUrl();
                //url = WJ.urlAddPath(ajaxUrl, urlSuffix);
            }

            var isAnyColumnSearch = false;
            var hasFixedParams = false;
            var restParams = [];
            var searchColumnParams = [];

            if (typeof data.columns !== "undefined") {
                // prechod vsetkymi vstupnymi parametrami od datatables
                for (var col of data.columns) {

                    // ak je to vyhladavacie policko naplnime prislusny parameter (id ignorujeme)
                    //console.log("search test name=", col.name," col=",col);
                    if (col.search.value != "") {

                        isAnyColumnSearch = true;
                        var colName = col.name;
                        //we need to have colName with upper case first letter
                        colName = colName.charAt(0).toUpperCase() + colName.slice(1);
                        searchColumnParams.push({"name": "search" + colName, "value": col.search.value});
                        //console.log("Search " + col.name + " value: '" + col.search.value + "'");
                    }
                }
            }

            //i need to iterate over data json object and find fixed_ columns
            for (var key in data) {
                if (key.startsWith("fixed_")) {
                    hasFixedParams = true;
                    isAnyColumnSearch = true;
                    searchColumnParams.push({"name": key.substring(6), "value": data[key]});
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

            //console.log("Datatable url: '" + url + "' sSource: "+sSource, "restParams:", restParams);

            //page calculations
            var pageSize = data.length;
            var start = data.start;
            var pageNum = (start === 0) ? 0 : (start / pageSize);

            //chceme vsetky zaznamy - aby necrashol chrome dame max podla konf. premennej datatablesExportMaxRows
            if (pageSize === -1) pageSize = window.datatablesExportMaxRows;

            //create new json structure for parameters for REST request
            if (serverSide) {
                restParams.push({"name": "size", "value": pageSize});
                restParams.push({"name": "page", "value": pageNum});
                if (typeof data.order !== "undefined" && data.order != null) {
                    for (var sort of data.order) {
                        let sortName = sort.name;

                        //iterate over DATA.columns, search by .data field for custom orderProperty
                        for (var column of DATA.columns) {
                            if (column.data === sortName) {
                                if (column.hasOwnProperty("orderProperty") && column.orderProperty != null && column.orderProperty != "") {
                                    //console.log("Found custom orderProperty for column: ", column.data, " orderProperty=", column.orderProperty);
                                    sortName = column.orderProperty;
                                    break;
                                }
                            }
                        }
                        //orderProperty name can have multiple columns, split it by , and order by all values
                        for (let name of sortName.split(",")) {
                            restParams.push({"name": "sort", "value": name + "," + sort.dir});
                        }
                    }
                }
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
                var data = {};
                data.data = sourceData.content;

                data.recordsTotal = totalElements;
                data.recordsFiltered = totalElements;
                data.options = sourceData.options || {};

                //zresetuj, aby sa nabuduce uz pouzilo ajax volanie
                DATA.initialData = null;

                setTimeout(()=> {
                    //console.log("initialData: ", data);
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
                            if ("Access is denied" === sourceData.error || "Access Denied" === sourceData.error) {
                                WJ.notifyError(WJ.translate("datatables.accessDenied.title.js"), WJ.translate("datatables.accessDenied.desc.js"));
                                return;
                            } else {
                                WJ.notifyError(WJ.translate("datatables.error.title.js"), sourceData.error);
                                return;
                            }
                        }

                        var totalElements = sourceData.totalElements;
                        var data = {};
                        data.data = sourceData.content;

                        data.recordsTotal = totalElements;
                        data.recordsFiltered = totalElements;
                        data.options = sourceData.options || {};
                        data.notify = sourceData.notify || null;

                        //WJ.log("fnCallback2, data=", data);
                        fnCallback(data);
                    }
                });
            }
        }

        FooterSum.bindEvents(TABLE);

        function runDataTables() {

            //console.log("runDataTables, DATA 1: ", DATA);

            let $datatableInit = $(dataTableInit);

            //console.log("runDataTables, $datatableInit=", $datatableInit, "serverSide=", DATA.serverSide, "data=", DATA);
            $datatableInit.attr("data-server-side", DATA.serverSide);

            //get rowReorder config object
            let rowReorder = RowReorder.getRowReorderConfig(DATA);

            //DT options: https://datatables.net/reference/option/
            if (typeof DATA.url === "string") {
                //src je URL adresa rest endpointu
                TABLE = $datatableInit
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
                        ajax: datatable2SpringData,
                        columns: DATA.columns,
                        serverSide: DATA.serverSide,
                        processing: true, //Feature control the processing indicator.
                        rowId: DATA.editorId,
                        order: DATA.order,
                        paging: DATA.paging,
                        rowReorder: rowReorder,
                        rowCallback: function (row, data, displayNum) {
                            //pozor, tato funkcia je tu 2x pre ajax aj normal load
                            //console.log("createdRow, displayNum=", displayNum, " data=", data, "row=", row);

                            //console.log("Setting class: ", data.editorFields.rowClass);
                            let selected = $(row).hasClass("selected");
                            let highlight = $(row).hasClass("highlight");
                            if (displayNum % 2 == 0) $(row).attr("class", "odd");
                            else $(row).attr("class", "even");
                            if (selected) $(row).addClass("selected");
                            if (highlight) $(row).addClass("highlight");

                            if (data.hasOwnProperty("editorFields") && data.editorFields !== null && data.editorFields.hasOwnProperty("rowClass")) {


                                if (data.editorFields.rowClass !== null) $(row).addClass(data.editorFields.rowClass);
                            }
                            if (DATA.onRowCallback!=null && typeof DATA.onRowCallback == "function") DATA.onRowCallback(TABLE, row, data);
                        },
                        stateSave: DATA.stateSave,
                        stateDuration: 0,
                        stateSaveCallback: dtWJ.stateSaveCallback,
                        stateLoadCallback: dtWJ.stateLoadCallback,
                        footerCallback: function (row, data, start, end, display) {
                            setTimeout(() => {
                                FooterSum.footerCallback(TABLE);
                            }, 100);
                        }
                    });
            } else {
                //src su skutocne data v JS objekte
                //console.log("Initializing DT from local data, src=", DATA.src, "serverSide: ", DATA.serverSide);
                DATA.editorLocking = false;
                TABLE = $datatableInit.DataTable({
                    data: DATA.src.data,
                    serverSide: false,
                    rowId: DATA.editorId,
                    columns: DATA.columns,
                    order: DATA.order,
                    rowReorder: rowReorder,
                    rowCallback: function (row, data, displayNum) {

                        if (displayNum % 2 == 0) $(row).attr("class", "odd");
                        else $(row).attr("class", "even");

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

            let colvisTimeout = null;
            $(dataTableInit).on('column-visibility.dt', function (e, settings, column, state) {
                //console.log("colvis changed, e=", e);
                //rerender header filters
                if (colvisTimeout != null) clearTimeout(colvisTimeout);
                colvisTimeout = setTimeout(() => {
                    dtWJ.initializeHeaderFilters(dataTableSelector+"_wrapper div.dt-scroll-head table ", false, DATA, TABLE);
                }, 50);
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
                                    text: "<i class='ti ti-check'></i>",
                                    className: 'btn btn-primary',
                                    action: function () {
                                        this.submit();
                                    }
                                },
                                {
                                    text: "<i class='ti ti-x'></i>",
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
                        var json = TABLE.DATA.json.data[TABLE.row(this).id()];
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

                var index = $(this).attr("data-dt-column");
                var th = $('#' + DATA.id + '_wrapper .dt-scroll-headInner tr:last-child th[data-dt-column="' + index + '"]');

                //console.log("Reset filter for column index=", index, " th=", th);

                $(th).find(".form-control-sm").val("");
                //console.log("Selectpickers=", $(th).find("select"), " index=", index);
                $(th).find("select").selectpicker("val", "");
                $(th).find("select").selectpicker("val", "contains");
                $(th).find(".min").val("");
                $(th).find(".max").val("");

                //reset extfilter
                var extfilter = $('#' + DATA.id + '_extfilter div[data-dt-column="' + index + '"]')
                if (extfilter.length > 0) {
                    var extfilterSelect = extfilter.find("select");
                    extfilterSelect.val("");
                    //console.log("Resetting extfilter=", extfilter, " selectpicker=", extfilterSelect.data("selectpicker"));
                    if (typeof extfilterSelect.data("selectpicker") !== "undefined") {
                        //console.log("Updating selectpicker 2");
                        extfilterSelect.selectpicker('refresh');
                    }
                }

                TABLE.column(index).search("");
                var searchBtn = $(th).find(".filtrujem");
                if (searchBtn.length > 0) {
                    searchBtn.trigger("click")
                } else if (extfilter.length > 0) {
                    //try extfilter
                    extfilter.find(".filtrujem").trigger("click");
                }

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
        $("#"+DATA.id+"_wrapper .dt-processing").show();

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

    TABLE.wjCreate = function () {
        //console.log("wjCreate");
        EDITOR.create({
                title: '<div class="row"><div class="col-12"><h5 class="modal-title">' + WJ.translate('button.add') + '</h5></div><div class="col-12" id="dt-header-tabs-' + DATA.id + '"></div></div>'+DIALOG_BUTTONS,
                buttons: DATA.createButtons
            }
        );
    }

    TABLE.wjEdit = function (row) {
        //console.log("wjEdit, row=", row);
        EDITOR.edit(row, {
            title: '<div class="row"><div class="col-12"><h5 class="modal-title">' + WJ.translate('button.edit') + '</h5></div><div class="col-12" id="dt-header-tabs-' + DATA.id + '"></div></div>'+DIALOG_BUTTONS,
            buttons: DATA.editorButtons
        });
    }

    TABLE.wjEditFetch = function (row) {
        //console.log("wjEditFetch, EDITING ROW, row=", row, "id=", row[0].id);
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
    TABLE.updateOptionsFromJson = updateOptionsFromJson;

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
        if(DATA.initHashFilter == true) {
            //initialize just hash filter on table
            TABLE.datatableOpener.initHashFilter();
        }
    }

    if (DATA.isLocalJson) {
        TABLE.hideButton("import");
        TABLE.hideButton("export");
        TABLE.hideButton("reload");
    }

    //nastav tooltip na export a import tlacidlo, BS5 nevie mat naraz toggle dialog aj title
    setTimeout(function() {
        new bootstrap.Tooltip($(".btn-export-dialog"));
        new bootstrap.Tooltip($(".btn-import-dialog"));
    }, 500);

    //bindni upozornenie o konflikte editacie zaznamu viacerymi pouzivatelmi
    if (DATA.editorLocking) dtWJ.bindEditorLocking(EDITOR);

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
                //data-action is set after this, we must do this in timeout
                setTimeout(()=> {
                    //title vypiseme len pri editacii jedneho zaznamu
                    let title = dtWJ.getTitle(EDITOR);
                    if (title != null && title != "") {
                        let dataAction = $("#"+TABLE.DATA.id+"_modal").attr("data-action");
                        let mainTitleKey = "button.edit";
                        if ("duplicate"===dataAction) {
                            mainTitleKey = "button.duplicate";

                            RowReorder.setNewReorderValue(TABLE, true);
                            if (DATA.isLocalJson === true) {
                                //unselect rows in datatable, because there is bug in render (it's not shown as selected but it is internally)
                                TABLE.rows({ selected: true }).deselect();
                            }
                        }

                        $("#"+TABLE.DATA.id+"_modal div.DTE_Header_Content h5.modal-title").text(WJ.translate(mainTitleKey)+": "+title);
                    }
                }, 100);
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

    TABLE.on('select.dt.DT deselect.dt.DT draw.dt.DT', function () {
        var anySelected = TABLE.rows({selected: true}).any();
        //change select-all icon depending on selected rows
        var icon = $("#"+DATA.id+"_wrapper div.dt-scroll-head table thead th.dt-format-selector button.buttons-select-all i.ti");
        if (anySelected) {
            icon.removeClass("ti-square-check");
            icon.addClass("ti-square");
        } else {
            icon.removeClass("ti-square");
            icon.addClass("ti-square-check")
        }
    });

    window.addEventListener("WJ.DTE.opened", function() {
        dtWJ.resizeTabContent(EDITOR);
    });
    dtWJ.bindOnResize(TABLE, DATA);
    dtWJ.bindDialogDragDrop(TABLE);
    dtWJ.bindColumnReorder(TABLE);

    TABLE.setAjaxUrl = function(newUrl) {
        try {
            TABLE.rows().deselect();
            TABLE.rows().invalidate();

            //remove all items in TABLE.context[0]._select_set array
            TABLE.context[0]._select_set.length = 0;
        } catch (e) {console.log(e);}
        TABLE.DATA.url = newUrl;
    }
    TABLE.getAjaxUrl = function() {
        return TABLE.DATA.url;
    }

    var editorAi = new EditorAi(EDITOR);

    return TABLE;
}
