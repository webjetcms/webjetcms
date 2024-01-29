//staticke JS funkcie aby sme nemali bordel v index.js

import WJ from "../../src/js/webjet";

/**
 * Pripravi hashtabulku vsetkych kombinacii select fieldov pre export
 * @param {*} DATA
 */
export function getOptionsTableExport(DATA) {
    let optionsTable = [];
    //priprav hashtabulku options
    if (DATA.jsonOptions != null) {
        try {
            for (var fieldNameList in DATA.jsonOptions) {
                //fieldNameList moze mat hodnotu menuDocId,rightMenuDocId, nastavime na oba fieldy rovnaky zoznam
                var fieldNameArr = fieldNameList.split(",");
                for (var i=0; i<fieldNameArr.length; i++) {
                    var fieldName = fieldNameArr[i];
                    var options = DATA.jsonOptions[fieldNameList];
                    //console.log("Updating options, fieldName=", fieldName, " values=", options);
                    for (var j=0; j<options.length; j++) {
                        optionsTable[fieldName+"-"+options[j].value] = options[j].label;
                    }
                }
            }
        } catch (e) {console.log(e);}
    }
    //preiteruj aj cele DATA a dopln options z editora
    $.each(DATA.fields, function( key, col ) {
        //console.log("getOptionsTableImport, col=", col);
        var fieldName = col.data;
        if(col.hasOwnProperty("options") && col.options != null) {
            var options = col.options;
            for (var j=0; j<options.length; j++) {
                optionsTable[fieldName+"-"+options[j].value] = options[j].label;
            }
            optionsTable[fieldName+"true"] = WJ.translate('button.yes');
            optionsTable[fieldName+"false"] = WJ.translate('button.no');
        }
    });
    //console.log("optionsTableExport=", optionsTable, "DATA.jsonOptions=", DATA.jsonOptions);
    return optionsTable;
}

export function getOptionsTableImport(DATA) {
    let optionsTable = [];
    //priprav hashtabulku options
    if (DATA.jsonOptions != null) {
        try {
            for (var fieldNameList in DATA.jsonOptions) {
                //fieldNameList moze mat hodnotu menuDocId,rightMenuDocId, nastavime na oba fieldy rovnaky zoznam
                var fieldNameArr = fieldNameList.split(",");
                for (var i=0; i<fieldNameArr.length; i++) {
                    var fieldName = fieldNameArr[i];
                    var options = DATA.jsonOptions[fieldNameList];
                    //console.log("Updating options, fieldName=", fieldName, " values=", options);
                    for (var j=0; j<options.length; j++) {
                        optionsTable[fieldName+"-"+options[j].label] = options[j].value;
                    }
                }
            }
        } catch (e) {console.log(e);}
    }
    //preiteruj aj cele DATA a dopln options z editora
    $.each(DATA.fields, function( key, col ) {
        //console.log("getOptionsTableImport, col=", col);
        var fieldName = col.data;
        if(col.hasOwnProperty("options") && col.options != null) {
            var options = col.options;
            for (var j=0; j<options.length; j++) {
                optionsTable[fieldName+"-"+options[j].label] = options[j].value;
            }
            optionsTable[fieldName+"-áno"] = "true";
            optionsTable[fieldName+"-nie"] = "false";
            optionsTable[fieldName+"-Áno"] = "true";
            optionsTable[fieldName+"-Nie"] = "false";
            optionsTable[fieldName+WJ.translate('button.yes')] = "true";
            optionsTable[fieldName+WJ.translate('button.no')] = "false";
        }
    });
    //console.log("optionsTableImport=", optionsTable, "DATA.jsonOptions=", DATA.jsonOptions);
    return optionsTable;
}

/**
 * Bindne upozornenia o konflikte na editor (upozorni, ak iny pouzivatel edituje rovnaky zaznam)
 * @param {*} EDITOR
 */
export function bindEditorLocking(EDITOR) {
    EDITOR.lockingEntityId = -1;
    EDITOR.lockingCallInterval = null;

    EDITOR.on('open', function (e, type, action) {
        if (typeof EDITOR.currentJson == "undefined") return;

        EDITOR.lockingEntityId = EDITOR.currentJson[EDITOR.TABLE.DATA.editorId];

        //call function
        callAddEditorLocking(EDITOR)

        //then set interval to call function
        EDITOR.lockingCallInterval = window.setInterval(callAddEditorLocking, 60000, EDITOR)
    })

    EDITOR.on('close', function (e, type) {
        //call function
        callRemoveEditorLocking(EDITOR);

        //clear interval
        clearInterval(EDITOR.lockingCallInterval);

        //resetni hodnotu
        EDITOR.lockingEntityId = -1;
    })
}

/**
 * Zavola REST sluzbu pre potvrdenia otvorenia editora
 * a zobrazi zoznam inych pouzivatelov editujucich dany zaznam
 * @param {*} EDITOR
 */
function callAddEditorLocking(EDITOR) {
    if(EDITOR.lockingEntityId != null && EDITOR.lockingEntityId !== -1) {
        $.ajax({
            url: '/admin/rest/editorlocking/open/' + EDITOR.lockingEntityId + '/' + getUniqueTableId(EDITOR.TABLE),
            success: function(data) {
                if(data.length > 0) {

                    let title = WJ.translate("editor_locking.conflict.js");
                    let introText = WJ.translate("editor_locking.currently_opened_by.js")+":<br/>";
                    let text = "";
                    for (let i = 0; i < data.length; i++) {
                        if (text != "") text += "<br/>";
                        text += data[i]['fullName'];
                      }

                    WJ.notifyInfo(title, introText + text, 20000)
                }
            },
            type: 'GET'
        })
    }
}

/**
 * Zavola REST sluzbu pri zatvoreni editora, odstrani so zoznamu pouzivatela pre dany zaznam
 * @param {*} EDITOR
 */
function callRemoveEditorLocking(EDITOR) {
    if(EDITOR.lockingEntityId != null && EDITOR.lockingEntityId !== -1) {
        $.ajax({
            url: '/admin/rest/editorlocking/close/' + EDITOR.lockingEntityId + '/' + getUniqueTableId(EDITOR.TABLE),
            type: 'GET'
        })
    }
}

/**
 * Vrati unikatne ID tabulky, zalozene na adrese REST rozhrania
 * @param {*} TABLE
 * @returns
 */
function getUniqueTableId(TABLE) {
    let url = TABLE.DATA.url;

    //odstran cokolvek za znakom ?
    let i = url.indexOf("?");
    if (i > 0) url = url.substring(0, i);

    //skrat, vacsina zacina na /admin/rest/
    url = url.replace("/admin/rest/", "");

    //ak zacina na / odstran, vznika nam potom na zaciatku -
    if (url.indexOf("/")==0 && url.length>2) url = url.substring(1);

    url = url.replace(/\//gi, '-');
    return url;
}

/**
 * Ked su vybrate nejake filtrovacie kriteria je potrebne upravit velkost tabulky, pretoze hlavicka je vyssia
 * @param {*} TABLE
 */
export function fixTableSize(TABLE) {
    //console.log("Fixing table size");
    let labels = $('#' + TABLE.DATA.id + '_wrapper .dt-filter-labels a');
    //console.log("labels=", labels);
    if (labels.length > 0) {
        $("body").addClass("datatable-labels-shown");
    } else {
        $("body").removeClass("datatable-labels-shown");
    }
}

/**
 * Nastavi klavesovu skratku CTRL+S/CMD+S na volanie ulozenia editora
 * @param {*} EDITOR
 */
export function bindKeyboardSave(EDITOR) {
    if (EDITOR.TABLE.DATA.keyboardSave === true) {
        //console.log("Binding CTRL+S to editor", EDITOR);
        //mame to cez takyto globalny event, aby sa to dalo vyvolat aj z editora
        window.addEventListener("WJ.DTE.save", function(e) {

            if ($("#"+EDITOR.TABLE.DATA.id+"_modal").hasClass("show") && $("div.modal.show").length==1) {
                //console.log("Saving editor, editor=", EDITOR, "displayed=", EDITOR.displayed());

                let title = getTitle(EDITOR);

                //console.log("title=", title);

                let editorAction = EDITOR.s.action;
                //console.log("editorAction=", editorAction);
                EDITOR.submit(
                    function() {
                        EDITOR.s.action = editorAction;
                        //console.log("SAVED");
                        WJ.notifySuccess(WJ.translate("datatables.saved.title.js"), WJ.translate("datatables.saved.text.js", title), 2000);
                    },
                    null,
                    null,
                    false
                );
            }
        });

        //zachytenie CTRL+S/CMD+S
        document.addEventListener("keydown", function(e) {
            if ((window.navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)  && e.key === 's') {
                e.preventDefault();
                // Process the event here (such as click on submit button)
                //console.log("Dispatching WJ.DTE.save");
                WJ.dispatchEvent("WJ.DTE.save", {});
            }
        }, false);
    }
}

/**
 * Ziska sirku scrollbaru, pre MacOS podla typu zobrazenia (ak je mys, vrati cislo, ak iba touchbar tak vrati 0)
 * https://stackoverflow.com/questions/13382516/getting-scroll-bar-width-using-javascript
 * @returns
 */
export function getScrollbarWidth() {

    // Creating invisible container
    const outer = document.createElement('div');
    outer.style.visibility = 'hidden';
    outer.style.overflow = 'scroll'; // forcing scrollbar to appear
    outer.style.msOverflowStyle = 'scrollbar'; // needed for WinJS apps
    document.body.appendChild(outer);

    // Creating inner element and placing it in the container
    const inner = document.createElement('div');
    outer.appendChild(inner);

    // Calculating difference between container's full width and the child width
    const scrollbarWidth = (outer.offsetWidth - inner.offsetWidth);

    // Removing temporary elements from the DOM
    outer.parentNode.removeChild(outer);

    return scrollbarWidth;

}

/**
 * Pokusi sa ziskat hodnotu v editore podla stlpca dt-row-edit, ked nenajde vrati prazdnu hodnotu
 * @param {*} EDITOR
 * @param {*} row - riadok z DT alebo NULL pre ziskanie zaznamu priamo z DTE
 * @returns
 */
export function getTitle(EDITOR, row = null) {
    let title = "";
    try {
        if (row == null) row = EDITOR.currentJson;
        //console.log("getTitle, row=", row);
        //skus najst property, ktora sa pouziva na kliknutie
        $.each(EDITOR.TABLE.DATA.columns, function (key, col) {
            //console.log("key=", key, "col=", col, "row=", row);
            if (typeof col.className != "undefined" && col.className?.indexOf("dt-row-edit")!=-1) {
                title = row[col.data];
                try {
                    if ("[object Object]"==title && "group"==col.data) {
                        title = row[col.data].fullPath;
                    }
                } catch (e) { }
                return false;
            }
        });
    } catch (ex) {console.log(ex);}
    title = WJ.htmlToText(title);
    if (title != null && typeof title == "string") title = title.replaceAll("&#47;", "/");
    if ("[object Object]"==title) title = "";
    //console.log("Returning title=", title);
    return title;
}

/**
 * Nastavi elementom .dte-tab-autoheight vysku podla okna editora
 * @param {*} EDITOR
 * @returns
 */
export function resizeTabContent(EDITOR) {
    var editorId = EDITOR.TABLE.DATA.id;
    if ($("#"+editorId+"_modal").hasClass("show")==false) {
        //console.log("Editor is not opened, id=", "#"+editorId+"_modal");
        return;
    }

    var windowInnerHeight = window.innerHeight;
    var dialogMarginTop = parseInt($("#"+editorId+"_modal > div.modal-dialog").css("margin-top"));
    var dialogMarginBottom = parseInt($("#"+editorId+"_modal > div.modal-dialog").css("margin-bottom"));
    var headerHeight = parseInt($("#"+editorId+"_modal div.DTE_Header").css("height"));
    var headerMarginTop = parseInt($("#"+editorId+"_modal div.DTE_Header").css("margin-top"));
    var footerHeight = parseInt($("#"+editorId+"_modal div.DTE_Footer").css("height"));

    //console.log("id=", editorId, "windowInnerHeight=", windowInnerHeight, "dialogMarginTop=", dialogMarginTop, "dialogMarginBottom=", dialogMarginBottom, "headerHeight=", headerHeight, "footerHeight=", footerHeight);
    //console.log("modal=", $("#"+editorId+"_modal"));

    var editorHeight = windowInnerHeight - dialogMarginTop - dialogMarginBottom - headerHeight - headerMarginTop - footerHeight - 4; //4 je safe konstanta
    if (editorHeight < 300) editorHeight = 300;

    if (typeof EDITOR.editorHeightLatest === "undefined") EDITOR.editorHeightLatest = 0;
    //console.log("Resizing editor, latest=", EDITOR.editorHeightLatest, " new=", editorHeight);
    if (editorHeight != EDITOR.editorHeightLatest) {
        EDITOR.editorHeightLatest = editorHeight;
        //console.log("RESIZING ", EDITOR.TABLE.DATA.id, ", editorHeight=", editorHeight);
        $("#"+editorId+"_modal div.dte-tab-autoheight").each(function( index ) {
            var $this = $(this);
            var offset = $this.data("dt-autoheight-offset");
            if (typeof offset === "undefined") offset = 0;
            $this.css("height", editorHeight + offset +"px");
        });
    }

    WJ.dispatchEvent('WJ.DTE.resize', {
        dte: EDITOR
    });
}

/**
 * Calculate height of the table based on window height and all toolbars
 */
function calculateAutoHeight(DATA) {
    const scrollBody = $('#' + DATA.id + '_wrapper').find('.dataTables_scrollBody');
    const inIframe = $("html").hasClass("in-iframe-show-table");

    var vh = document.documentElement.clientHeight;
    var lyHeader = 0;
    var breadcrumb = 0;

    if (inIframe==false) {
        if ("fixed"==$(".ly-header").css("position")) lyHeader = $(".ly-header").outerHeight();
        var breadcrumb = $('#' + DATA.id + '_wrapper').parent().find(".md-breadcrumb").outerHeight();
        if (breadcrumb == undefined) breadcrumb = $(".md-breadcrumb").outerHeight();
    } else {
        //restaurant-menu has show-in-iframe class on breadcrumb because of the date selector
        var breadcumbEl = $('#' + DATA.id + '_wrapper').parent().find(".md-breadcrumb");
        if (breadcumbEl.length>0 && breadcumbEl.hasClass("show-in-iframe")) breadcrumb = breadcumbEl.outerHeight();
    }

    var dtHeaderRow = $('#' + DATA.id + '_wrapper .dt-header-row').outerHeight();
    var dtFilterRow = $('#' + DATA.id + '_wrapper div.dataTables_scrollHeadInner').outerHeight();
    var dtFooterRow = $('#' + DATA.id + '_wrapper .dt-footer-row').outerHeight();
    if (dtFooterRow < 30) dtFooterRow = dtFooterRow + 31; //footer not initialized/empty, add text height

    var height = vh - lyHeader - breadcrumb - dtHeaderRow - dtFooterRow - dtFilterRow;

    //console.log(DATA.id+" vh=", vh, "lyHeader=", lyHeader, "breadcrumb=", breadcrumb, "dtHeaderRow=", dtHeaderRow, "dtFilterRow=", dtFilterRow, "dtFooterRow=", dtFooterRow, "height=", height);

    scrollBody.css("height", height + "px");

    //set also jstree height
    if (inIframe==false) breadcrumb = $(".tree-col .md-breadcrumb").outerHeight();
    dtHeaderRow = $('.tree-col .dt-header-row').outerHeight();
    height = vh - lyHeader - breadcrumb - dtHeaderRow;

    //console.log("vh=", vh, "lyHeader=", lyHeader, "breadcrumb=", breadcrumb, "dtHeaderRow=", dtHeaderRow, "height=", height);
    $("#SomStromcek").css("height", height + "px");
}

/**
 * Inicializuje pocuvanie udalosti na zmenu velkosti okna pre prepocet velkosti .dte-tab-autoheight
 * @param {*} TABLE
 */
export function bindOnResize(TABLE, DATA) {
    $(window).on("resize", function() {
        clearTimeout(TABLE.resizedFinished);
        TABLE.resizedFinished = setTimeout(function(){
            //console.log('Resized finished., id=', TABLE.DATA.id);
            resizeTabContent(TABLE.EDITOR);

            if(DATA.autoHeight === true) calculateAutoHeight(DATA);
        }, 250);
    });

    if(DATA.autoHeight === true) {
        window.setInterval(()=> {
            calculateAutoHeight(DATA);
        }, 5000);
        setTimeout(()=>{
            calculateAutoHeight(DATA);
        }, 300);
        setTimeout(()=>{
            calculateAutoHeight(DATA);
        }, 1000);

        //resize on draw event
        TABLE.on( 'draw', function () {
            calculateAutoHeight(DATA);
        } );
    }
}

/**
 * Inicializuje moznost drag & drop dialogoveho okna editora
 * @param {*} TABLE
 */
export function bindDialogDragDrop(TABLE) {
    if ($("html").hasClass("in-iframe")) return; //in iframe disable drag & drop, window is full screen

    $("body").on("mousedown", "#"+TABLE.DATA.id+"_modal .DTE_Header", function (mousedownEvt) {
        var $draggable = $(this);
        var x = mousedownEvt.pageX - $draggable.offset().left,
            y = mousedownEvt.pageY - $draggable.offset().top;

        //console.log("drag, x=", x, "y=", y);
        var modalContent = $("#"+TABLE.DATA.id+"_modal div.modal-content");

        $("body").on("mousemove.draggable", function (mousemoveEvt) {
            //console.log("draggable, left=", mousemoveEvt.pageX - x, "top=", mousemoveEvt.pageY - y);
            //console.log("modalContent=", modalContent);
            modalContent.offset({
                "left": mousemoveEvt.pageX - x,
                "top": mousemoveEvt.pageY - y
            });
        });
        $("body").one("mouseup", function () {
            $("body").off("mousemove.draggable");
        });
    });

    window.addEventListener('WJ.DTE.resize', function (e) {
        var modalContent = $("#"+TABLE.DATA.id+"_modal div.modal-content");
        //console.log("WJ DTE RESIZE, modalContent=", modalContent);
        modalContent.css("left", "auto");
        modalContent.css("top", "auto");
    });
}

/**
 * Pocuva na zmenu poradia stlpcov a opravi data-column-index na vyhladavacich poliach
 * @param {*} TABLE
 */
export function bindColumnReorder(TABLE) {
    TABLE.on( 'column-reorder', function ( e, settings, details ) {
        //console.log("Som column reorder, details=", details);

        setTimeout( function () {
            //fixni atribut data-column-index v druhom riadku hlavicky
            $("#"+TABLE.DATA.id+"_wrapper .dataTables_scrollHead table.dataTable thead tr:first th").each(function(i){
                let $this = $(this);
                let columnIndex = $this.data("column-index");
                //console.log(i+":", this, "index=", columnIndex);
                //najdi rovnaky v druhom riadku
                let filter = $("#"+TABLE.DATA.id+"_wrapper .dataTables_scrollHead table.dataTable thead tr:nth-child(2) th:nth-child("+(i+1)+")");
                //console.log("filter=", filter.data("column-index"));
                filter.data("column-index", columnIndex);
                filter.attr("data-column-index", columnIndex);

                //najdi extfilter, ak existuje
                filter = $("div.dt-extfilter-"+$this.data("dt-field-name"));
                //console.log("EXT filter=", filter.data("column-index"));
                if (filter.length>0) {
                    filter.data("column-index", columnIndex);
                    filter.attr("data-column-index", columnIndex);
                }
            })
        }, 200 );
    } );
}


/**
 * Vrati unikatny kluc do localStorage pre ulozenie stavu datatabulky
 * @param {*} settings
 * @returns
 */
function getStateSaveKey(settings) {
    let key = 'DT_state_' + settings.sInstance+"_"+window.location.pathname;
    return key;
}

/**
 * Returns actual datetime timestamp (number)
 * @returns
 */
function getNow() {
    var now = new Date().getTime();
    return now;
}

/**
 * Check if it's posiible to save table state. It's possible to save it at 800ms after table load.
 * It prevent's to save tableState if table has order option set.
 * We wuold like to save only states after some user interaction.
 */
var loadCallbackTime = null;
function isStateSaveEnabled() {
    var now = getNow();
    if (loadCallbackTime != null && (loadCallbackTime+800)<now) {
        return true;
    }
    return false;
}

/**
 * Enable saveState function to save table state after user interaction
 */
function enableStateSave() {
    if (loadCallbackTime == null) {
        loadCallbackTime = getNow();
    }
}

/**
 * Ulozenie stavu datatabulky do localStorage, uklada len:
 * - poradie stlpcov
 * - datum zmeny
 * @param {*} settings
 * @param {*} data
 */
export function stateSaveCallback(settings,data) {
    if (isStateSaveEnabled()==false) return;

    let key = getStateSaveKey(settings);
    //console.log("stateSaveCallback, settings=",settings," key=", key,", data=", data);

    let newData = {};
    newData.ColReorder = data.ColReorder;
    newData.order = data.order;
    newData.time = data.time;

    if (data.length != settings.aLengthMenu[0][0]) {
        //ak sa nejedna o automaticku velkost uloz aj velkost stranky
        newData.length = data.length;
    }

    //console.log("stateSaveCallback, newData=", newData);

    localStorage.setItem(key , JSON.stringify(newData) );
}

/**
 * Nacitanie stavu datatabulky z localStorage
 * @param {*} settings
 * @returns
 */
export function stateLoadCallback(settings) {
    let key = getStateSaveKey(settings);
    let data = JSON.parse( localStorage.getItem( key ) );
    //console.log("stateLoadCallback, key=", key," data=", data, "settings=", settings);
    enableStateSave();
    return data;
}

/**
 * Zmaze nastavenie datatabulky
 * @param {*} settings
 */
export function stateReset(TABLE) {
    //console.log("stateReset, table=", TABLE);
    TABLE.colReorder.reset().draw();
    TABLE.order(TABLE.DATA.order).draw();
}


export function filtrujemClick(button, TABLE, DATA, isDefaultSearch) {

    var input = $(button).parents(".input-group").find("input.filter-input,select.filter-input");
    var index = parseInt($(button).parents("th,div.dt-extfilter").attr("data-column-index"));
    var regExval = $(button).parents(".input-group").find("option:selected").val();

    if (isNaN(index)) return;

    //console.log("Filtrujem, index=",index," $(input).length=",$(input).length, "input=", input);

    //check all filters, user can fill multiple fields and click just once on search button
    $.each($(button).parents("tr,div#" + DATA.id + "_extfilter").find('.input-group'), function (key, group) {

        var val = "",
            valMin = "",
            valMax = "",
            valMinMax = "",
            valServerSideRange = null,
            allowRegex = false;

        //console.log("Filter type=", $(group).attr("data-filter-type"));

        if ($(group).attr("data-filter-type") === "boolean") {
            val = $(group).find('option:selected').val();
            //boolean is locally compared to true/false value in dt-config, so value is correct even for local tables
        } else if ($(group).attr("data-filter-type") === "select") {
            val = $(group).find('option:selected').val();
            //console.log("Val select=", val);

            //resetSearchValue = false;
            if (DATA.serverSide===false) val = $(group).find('option:selected').text();

        } else if ($(group).attr("data-filter-type") === "number" || $(group).attr("data-filter-type") === "number-decimal" || $(group).attr("data-filter-type") === "date" || $(group).attr("data-filter-type") === "datetime") {

            var isDateRange = $(group).attr("data-filter-type") === "date" || $(group).attr("data-filter-type") === "datetime";

            //aby sa pripadne zresetovala hodnota po vymazani hodnoty z od-do fieldu
            valServerSideRange = "";

            if ($(group).find('.min').val() !== "") {
                var value = $(group).find('.min').val();
                valMin = WJ.translate('datatables.input.from.js').toLowerCase() + ' ' + value;
                //console.log("valMin=", valMin, "value=", value, " ");
                if (isDateRange) value = moment(value, "L HH:mm:ss").valueOf();
                //console.log("value moment=", value);
                valServerSideRange = value;
            }

            if ($(group).find('.max').val() !== "") {
                var value = $(group).find('.max').val();
                valMax = WJ.translate('datatables.input.to.js').toLowerCase() + ' ' + value;
                if (isDateRange) value = moment(value, "L HH:mm:ss").valueOf();
                valServerSideRange += "-" + value;
            }

            if ($(group).find('.min').val() !== "" && $(group).find('.max').val() !== "") {
                valMinMax = " ";
            }

            if ($(group).find('.min').val() !== "" || $(group).find('.max').val() !== "") {
                val = valMin + valMinMax + valMax;
            }
        } else {
            input = $(group).find("input");

            var regExSearch = "";

            //console.log("val1=", $(input).val(), " input=", input);
            if ($(input).val() !== "") {


                val = $(input).val();

                //console.log("input=", input);

                //pre lokalne vyhladavanie je potrebne do hladania vlozit TEXT optionu, nie hodnotu
                if (DATA.serverSide === false && typeof input !== "undefined" && typeof input.prop("tagName") !== "undefined" && "SELECT" === input.prop("tagName").toUpperCase()) {
                    //neplati pre column-type-boolean
                    if (val !== "true" && val !== "false") {
                        //chceme equal, musime pridat regex vyraz
                        val = '^' + input[0].options[input[0].selectedIndex].text + '$';
                    }
                    //console.log("serverSide val=", val, " input=", input);
                }

                if (regExval === "contains") {
                    regExSearch = val;
                } else if (regExval === "startwith") {
                    allowRegex = true;
                    regExSearch = '^' + val;
                } else if (regExval === "endwith") {
                    allowRegex = true;
                    regExSearch = val + '$';
                } else if (regExval === "equals") {
                    allowRegex = true;
                    regExSearch = '^' + val + '$';
                } else if (regExval === "regex") {
                    allowRegex = true;
                    if (DATA.serverSide) regExSearch = 'regex:' + val;
                    else regExSearch = val;
                } else {
                    allowRegex = true;
                    regExSearch = val;
                }
            }

            val = regExSearch;
        }

        var inputIndex = parseInt($(group).parents("th,div.dt-extfilter").attr("data-column-index"));
        if (isNaN(inputIndex)) return;

        //console.log("inputIndex=", inputIndex);
        var prefix = "range:";
        if (isDateRange) prefix = "daterange:";
        if (valServerSideRange !== null && DATA.serverSide && !isDefaultSearch) {
            //console.log("Searching column ",inputIndex, " val=",valServerSideRange);

            if (valServerSideRange !== "") TABLE.column(inputIndex).search(prefix + valServerSideRange);
            else TABLE.column(inputIndex).search("");
        }

        //tvrda medzera, ide z prazdnych selectov
        if (val === '\xa0') val = "";
        if (typeof val === "undefined" || "undefined" === val) val = "";

        //console.log("val=",val, "valServerSideRange=", prefix+valServerSideRange, "inputIndex=",inputIndex);

        //ulozime aktualny search aj do columns objektu, pouziva sa to specificky pre kombinaciu server side a client side filtrovania/sortovania
        DATA.columns[inputIndex].searchVal = "";
        if (valServerSideRange !== null && valServerSideRange !== "") DATA.columns[inputIndex].searchVal = prefix + valServerSideRange;
        else if (val !== "") DATA.columns[inputIndex].searchVal = val;

        //set all search values even on other columns than clicked search button
        //console.log("Searching[", inputIndex, "]=", DATA.columns[inputIndex].searchVal, "current=", TABLE.column(inputIndex).search());
        let searchVal = DATA.columns[inputIndex].searchVal;
        //daterange: and range: is not possible to set this way
        if (searchVal=="" || searchVal.indexOf("range:")==-1) {
            //console.log("Searching[", inputIndex, "]=", DATA.columns[inputIndex].searchVal, "current=", TABLE.column(inputIndex).search());
            if (!isDefaultSearch) TABLE.column(inputIndex).search(searchVal, allowRegex, false);
        }

        if (val !== "") {
            //console.log("Setting filter label, val=", val);

            var headline = DATA.columns[inputIndex].title;

            if ($('#' + DATA.id + '_wrapper .dt-filter-labels__link[data-column-index="' + inputIndex + '"]').length < 1) {
                $('#' + DATA.id + '_wrapper .dt-filter-labels').append('<a href="javascript:;" class="btn btn-sm btn-outline-secondary dt-filter-labels__link" id="dt-filter-labels-link-' + DATA.columns[inputIndex].data + '" data-column-index="' + inputIndex + '"><span  class="dt-filter-labels__link__headline">' + headline + '</span><i class="far fa-times"></i></a>');
            }

            $('#' + DATA.id + '_wrapper th[data-column-index="' + inputIndex + '"]').addClass("has-filter-active");

        } else {

            $('#' + DATA.id + '_wrapper .dt-filter-labels__link[data-column-index="' + inputIndex + '"]').remove();
            $('#' + DATA.id + '_wrapper th[data-column-index="' + inputIndex + '"]').removeClass("has-filter-active");
        }
    });

    if (!isDefaultSearch) {
        dtWJ.fixTableSize(TABLE);

        //console.log("TABLE.draw");
        TABLE.draw();
    }

    if (typeof input !== "undefined" && typeof input.prop("tagName") !== "undefined" && "SELECT" === input.prop("tagName").toUpperCase() && input.val() === "") {
        //musime reatachnut selectpicker, lebo DT ho posaha detachnutim ked uz nie je selectnuta ziadna hodnota
        //zaujimave je, ze to nerobi ked len hodnotu prepinam
        //3 hodiny debugovania...
        //console.log("Reataching selectpicker, input=", input);
        input.selectpicker('refresh');
    }
}

/**
 * Returns empty data for DT, used by initialData option. It's used to prevent DT to call ajax on init.
 * @param {boolean} forceData - set to true to force to use data without rows number check
 * @returns
 */
export function getEmptyData(forceData=true) {
    var emptyData = {
        "content": [],
        "pageable": "INSTANCE",
        "options": null,
        "notify": null,
        "last": true,
        "totalPages": 1,
        "totalElements": 0,
        "first": true,
        "sort": {
            "unsorted": true,
            "sorted": false,
            "empty": true
        },
        "numberOfElements": 0,
        "size": 0,
        "number": 0,
        "empty": true,
        "forceData": forceData
    }
    return emptyData;
}