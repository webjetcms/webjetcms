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
    const scrollBody = $('#' + DATA.id + '_wrapper').find('.dt-scroll-body');
    const inIframe = $("html").hasClass("in-iframe-show-table");

    var vh = document.documentElement.clientHeight;
    var lyHeader = 0;
    var breadcrumb = 0;

    if (inIframe==false) {
        if ("fixed"==$(".ly-header").css("position")) lyHeader = $(".ly-header").outerHeight();
        var breadcrumbElemets = $('#' + DATA.id + '_wrapper').parent().find(".md-breadcrumb");
        //console.log("breadcrumbElemets=", breadcrumbElemets);
        if (breadcrumbElemets.length>0) {
            //iterate all breadcrumbs and get total sum of outerHeight
            breadcrumbElemets.each(function() {
                let $this = $(this);
                //if display is none skip
                if ($this.css("display")=="none") return;
                breadcrumb += $this.outerHeight();
            });
        } else {
            var breadcrumbElement = $(".md-breadcrumb:first");
            //console.log("breadcrumbElement=", breadcrumbElement);
            if (breadcrumbElement.length>0) {
                if (breadcrumbElement.css("display")!="none") breadcrumb = breadcrumbElement.outerHeight();;
            }
        }
    } else {
        //restaurant-menu has show-in-iframe class on breadcrumb because of the date selector
        var breadcumbEl = $('#' + DATA.id + '_wrapper').parent().find(".md-breadcrumb");
        if (breadcumbEl.length>0 && breadcumbEl.hasClass("show-in-iframe")) breadcrumb = breadcumbEl.outerHeight();
    }

    var dtHeaderRow = $('#' + DATA.id + '_wrapper .dt-header-row').outerHeight();
    var dtFilterRow = $('#' + DATA.id + '_wrapper div.dt-scroll-headInner').outerHeight();
    var dtFooterRow = $('#' + DATA.id + '_wrapper .dt-footer-row').outerHeight();
    if (dtFooterRow < 30) dtFooterRow = dtFooterRow + 31; //footer not initialized/empty, add text height

    var dtFooterSummary = 0;
    var dtFooterSummaryEl = $('#' + DATA.id + '_wrapper .dt-scroll-foot')
    if (dtFooterSummaryEl.is(":visible")) dtFooterSummary = dtFooterSummaryEl.outerHeight();

    var height = vh - lyHeader - breadcrumb - dtHeaderRow - dtFooterRow - dtFilterRow - dtFooterSummary;

    //console.log(DATA.id+" vh=", vh, "lyHeader=", lyHeader, "breadcrumb=", breadcrumb, "dtHeaderRow=", dtHeaderRow, "dtFilterRow=", dtFilterRow, "dtFooterRow=", dtFooterRow, "height=", height);

    scrollBody.css("height", height + "px");

    //set also jstree height
    if (inIframe==false) breadcrumb = $(".tree-col .md-breadcrumb").outerHeight();
    dtHeaderRow = $('.tree-col .dt-header-row').outerHeight();
    let filterHeight = $("div.tree-col .datatableInit").outerHeight();
    height = vh - lyHeader - dtHeaderRow - filterHeight;

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

    if(window.location.href.indexOf("showOnlyEditor=true") != -1) return; //showOnlyEditor sets the window to full screen, disable drag & drop

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
            $("#"+TABLE.DATA.id+"_wrapper .dt-scroll-head table.dataTable thead tr:first th").each(function(i){
                let $this = $(this);
                let columnIndex = $this.attr("data-dt-column");
                //console.log(i+":", this, "index=", columnIndex);

                //najdi extfilter, ak existuje
                let filter = $("div.dt-extfilter-"+$this.data("dt-field-name"));
                //console.log("EXT filter=", filter.data("column-index"));
                if (filter.length>0) {
                    filter.attr("data-dt-column", columnIndex);
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
    //DT v1 compatibility
    newData.ColReorder = data.colReorder;
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
    if ("form-detail"==settings.sInstance || "component-datatable"==settings.sInstance) return null;

    let key = getStateSaveKey(settings);
    let data = JSON.parse( localStorage.getItem( key ) );
    //DT v1 compatibility
    if (data != null) data.colReorder = data.ColReorder;
    //console.log("stateLoadCallback, key=", key," data=", data, "settings=", settings);
    enableStateSave();
    return data;
}

/**
 * Reset table state in local storage (eg. after table error)
 * @param {*} settings - DT settings
 */
export function stateResetLocalStorage(settings) {
    let key = getStateSaveKey(settings);
    localStorage.removeItem(key);
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
    var index = parseInt($(button).parents("th,div.dt-extfilter").attr("data-dt-column"));

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
            if (typeof $(input).val() != "undefined" && $(input).val() !== "") {

                val = $(input).val();

                var regExval = $(input).parents(".input-group").find("option:selected").val();

                //console.log("input=", input, " val=", val, " regExval=", regExval);

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

        var inputIndex = parseInt($(group).parents("th,div.dt-extfilter").attr("data-dt-column"));
        if (isNaN(inputIndex)) return;
        var columnName = $(group).parents("th,div.dt-extfilter").attr("data-dt-field-name");
        //console.log("columnName=", columnName, "inputIndex=", inputIndex);
        var dataIndex = getDataIndex(columnName, DATA);

        //console.log("inputIndex=", inputIndex, "dataIndex=", dataIndex);
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
        DATA.columns[dataIndex].searchVal = "";
        if (valServerSideRange !== null && valServerSideRange !== "") DATA.columns[dataIndex].searchVal = prefix + valServerSideRange;
        else if (val !== "") DATA.columns[dataIndex].searchVal = val;

        //set all search values even on other columns than clicked search button
        //console.log("Searching[", inputIndex, "]=", DATA.columns[dataIndex].searchVal, "current=", TABLE.column(inputIndex).search());
        let searchVal = DATA.columns[dataIndex].searchVal;
        //daterange: and range: is not possible to set this way
        if (searchVal=="" || searchVal.indexOf("range:")==-1) {
            //console.log("Searching[", inputIndex, "]=", DATA.columns[dataIndex].searchVal, "current=", TABLE.column(inputIndex).search());
            if (!isDefaultSearch) {
                TABLE.column(inputIndex).search(searchVal, allowRegex, false);
            }
        }

        if (val !== "") {
            //console.log("Setting filter label, val=", val);

            var headline = DATA.columns[dataIndex].title;

            if ($('#' + DATA.id + '_wrapper .dt-filter-labels__link[data-dt-column="' + inputIndex + '"]').length < 1) {
                $('#' + DATA.id + '_wrapper .dt-filter-labels').append('<a href="javascript:;" class="btn btn-sm btn-outline-secondary dt-filter-labels__link" id="dt-filter-labels-link-' + DATA.columns[dataIndex].data + '" data-dt-column="' + inputIndex + '"><span  class="dt-filter-labels__link__headline">' + headline + '</span><i class="ti ti-x" style="font-size: 0.9rem"></i></a>');
            }

            $('#' + DATA.id + '_wrapper th[data-dt-column="' + inputIndex + '"]').addClass("has-filter-active");

        } else {

            $('#' + DATA.id + '_wrapper .dt-filter-labels__link[data-dt-column="' + inputIndex + '"]').remove();
            $('#' + DATA.id + '_wrapper th[data-dt-column="' + inputIndex + '"]').removeClass("has-filter-active");
        }
    });

    if (!isDefaultSearch) {
        dtWJ.fixTableSize(TABLE);

        //console.log("TABLE.draw");
        TABLE.draw();
    }

    if (typeof input !== "undefined" && typeof input.prop("tagName") !== "undefined" && "SELECT" === input.prop("tagName").toUpperCase() && input.val() === "" && typeof input.data("selectpicker") !== "undefined") {
        //musime reatachnut selectpicker, lebo DT ho posaha detachnutim ked uz nie je selectnuta ziadna hodnota
        //zaujimave je, ze to nerobi ked len hodnotu prepinam
        //3 hodiny debugovania...
        //console.log("Reataching selectpicker, input=", input);
        input.selectpicker('refresh');
    }
}

/**
 * Returns index of column in DATA.columns object based on column name
 * @param {*} columnName
 * @param {*} DATA
 * @returns
 */
function getDataIndex(columnName, DATA) {
    var index = 0;
    columnName = columnName.replace("editorFields-", "editorFields.");
    //console.log("getDataIndex, columnName=", columnName, "DATA=", DATA);
    $.each(DATA.columns, function (key, column) {
        if (column.data === columnName) {
            index = key;
            return false;
        }
    });
    return index;
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

/**
 * Nastavi select/date pickre na DT hlavicke a vyvola jej pozicie podla DT (sirky stlpcov)
 */
export function fixDatatableHeaderInputs(tableInstance) {
    var dataId = tableInstance.DATA.id;

    //console.log("fixDatatableHeaderInputs, dataId=", dataId, " tableInstance=", tableInstance);

    $('#' + dataId + '_wrapper select.filter-input, #' + dataId + '_extfilter select.filter-input').each(function(index, element) {
        //console.log("testing select, this=", this);
        let $this = $(this);

        if ($this.hasClass("selectpickerbinded")) return;
        $this.addClass("selectpickerbinded");

        //console.log("Setting selectpicker ", this);
        $this.selectpicker(tableInstance.EDITOR.DT_SELECTPICKER_OPTS);

        //mozno sa len pridal do hlavicky, nastav optiony
        //console.log("Aktualizujem optiony, this=", this.options.length);
        if (this.options.length<1) {
            let name = $this.data("dt-name");
            //console.log("name=", name);
            if (typeof name != "undefined" && name != null) {
                dtWJ.updateFilterSelect(tableInstance.DATA, name);
            }
        }
    });
    $('#' + dataId + '_wrapper select.filter-input-prepend, #' + dataId + '_extfilter select.filter-input-prepend').each(function(index, element) {
        let $this = $(this);
        if ($this.hasClass("selectpickerbinded")) return;
        $this.addClass("selectpickerbinded");

        //console.log("Setting selectpicker ", this);
        $this.selectpicker(tableInstance.EDITOR.DT_SELECTPICKER_OPTS_NOSEARCH);
    });

    $('#' + dataId + '_wrapper .datepicker, #' + dataId + '_extfilter .datepicker').each(function (key, dateInput) {
        let $this = $(dateInput);
        if ($this.hasClass("datepickerbinded")) return;
        $this.addClass("datepickerbinded");

        $this.on("change", function() {
            if ($this.val() != "") $this.addClass("has-value");
            else $this.removeClass("has-value");

            tableInstance.columns.adjust();
        });

        //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
        new $.fn.dataTable.Editor.DateTime($this, {
            format: 'L',
            momentLocale: window.userLng,
            locale: window.userLng,
            keyInput: false,
            i18n: tableInstance.EDITOR.i18n.datetime,
            onChange: function() {
                $this.trigger("change");
            }
        });
    });

    $('#' + dataId + '_wrapper .datetimepicker, #' + dataId + '_extfilter .datetimepicker').each(function (key, dateInput) {
        let $this = $(dateInput);
        if ($this.hasClass("datepickerbinded")) return;
        $this.addClass("datepickerbinded");

        $this.on("change", function() {
            //to prevent UI change with clicking on filter button
            setTimeout(()=> {
                if ($this.val() != "") $this.addClass("has-value");
                else $this.removeClass("has-value");

                tableInstance.columns.adjust();
            }, 100);
        });

        //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
        new $.fn.dataTable.Editor.DateTime($this, {
            format: 'L HH:mm',
            momentLocale: window.userLng,
            locale: window.userLng,
            keyInput: false,
            i18n: tableInstance.EDITOR.i18n.datetime,
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

            tableInstance.columns.adjust();
        });

        //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
        new $.fn.dataTable.Editor.DateTime($this, {
            format: 'HH:mm',
            momentLocale: window.userLng,
            locale: window.userLng,
            keyInput: false,
            i18n: tableInstance.EDITOR.i18n.datetime,
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

            tableInstance.columns.adjust();
        });

        //console.log("Setting datepicker to sk: ", dateInput, " i18n: ", EDITOR.i18n);
        new $.fn.dataTable.Editor.DateTime($this, {
            format: 'HH:mm:ss',
            momentLocale: window.userLng,
            locale: window.userLng,
            keyInput: false,
            i18n: tableInstance.EDITOR.i18n.datetime,
            onChange: function() {
                $this.trigger("change");
            }
        });
    });

    $('#' + dataId + '_wrapper .dt-format-number input.form-control, #' + dataId + '_extfilter .dt-format-number input.form-control, '+
      '#' + dataId + '_wrapper .dt-format-number--decimal input.form-control, #' + dataId + '_extfilter .dt-format-number--decimal input.form-control'
    ).each(function (key, input) {
        let $this = $(input);
        $this.on("change", function() {
            if ($this.val() != "") $this.addClass("has-value");
            else $this.removeClass("has-value");

            tableInstance.columns.adjust();
        });
    });

    tableInstance.columns.adjust();
}

/**
* Aktualizuje select v hlavicke/editore, nastavi optiony podla posledneho ajax requestu
* @param {*} fieldName
* @returns
*/
export function updateFilterSelect(DATA, fieldName) {
    var fieldNameSelector = fieldName;
    if (fieldNameSelector.indexOf(".")!=-1) fieldNameSelector = fieldNameSelector.replace(/\./gi, "\\.");

    var selects = [];
    var select = $("#" + DATA.id + "_wrapper select.dt-filter-" + fieldNameSelector)[0];
    if (typeof select !== "undefined") selects.push(select);

    var extfilter = $("#" + DATA.id + "_extfilter .dt-extfilter-" + fieldName + " select.dt-filter-" + fieldNameSelector);
    if (extfilter.length>0) selects.push(extfilter[0]);

    for (var i = 0; i < selects.length; i++) {
        select = selects[i];
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
 * Initialize header filter for datatable
 * @param {*} dataTableSelector - header selector prefix
 * @param {*} extfilterExists - true if there is external filter
 * @param {*} DATA
 * @param {*} TABLE - optional, if set we will also call fixDatatableHeaderInputs to adhist datatables header selects
 */
export function initializeHeaderFilters(dataTableSelector, extfilterExists, DATA, TABLE=null) {
    //console.log("initializeHeaderFilters, dataTableSelector=", dataTableSelector, "extfilterExists=", extfilterExists, "DATA=", DATA, "TABLE=", TABLE);
    $(dataTableSelector + ' thead tr:eq(1) th').each(function (index) {

        //remove colreorder listener - disable drag drop on filter
        $(this).off("selectstart.colReorder");
        $(this).off("mousedown.colReorder touchstart.colReorder");

        var i = $(this).attr("data-dt-column");
        if (typeof i === "undefined" || i === null) i = index;
        var fieldName = DATA.columns[i].data;

        //console.log("Iterating, i=", i, "fieldName=", fieldName, " col=", DATA.columns[i], "this=", this);

        var inputGroupBefore = `
            <form>
                <div class="input-group" data-filter-type="text">`;
        var inputGroupAfter = `

                        <button class="filtrujem btn btn-sm btn-outline-secondary dt-filtrujem-${fieldName}" type="submit">
                            <i class="ti ti-search"></i>
                        </button>

                </div>
            </form>`;
        var html = `
                <select class="filter-input-prepend">
                    <option value="contains" selected data-content="<i class=\'ti ti-arrows-horizontal\'></i><small>${WJ.translate('datatables.select.contains.js')}</small>">${WJ.translate('datatables.select.contains.js')}</option>
                    <option value="startwith" data-content="<i class=\'ti ti-arrow-right-bar\'></i><small>${WJ.translate('datatables.select.startwith.js')}</small>">${WJ.translate('datatables.select.startwith.js')}</option>
                    <option value="endwith" data-content="<i class=\'ti ti-arrow-left-bar\'></i><small>${WJ.translate('datatables.select.endwith.js')}</small>">${WJ.translate('datatables.select.endwith.js')}</option>
                    <option value="equals" data-content="<i class=\'ti ti-equal\'></i><small>${WJ.translate('datatables.select.equals.js')}</small>">${WJ.translate('datatables.select.equals.js')}</option>`;
        //regex nie je dobre podporovany v Spring data, takze sa nemoze pouzit pri server side
        //regex disabled, most tables are server side
        /*if (DATA.serverSide === false) {
            html += `
                    <option value="regex" data-content="<i class=\'ti ti-brackets\'></i><small>${WJ.translate('datatables.select.regex.js')}</small>"><i class="ti ti-brackets"></i> ${WJ.translate('datatables.select.regex.js')}</option>`;
        }*/
        html += `
                </select>
                <input class="form-control form-control-sm filter-input dt-filter-${fieldName}" type="text" />`;

        if ($(this).hasClass("dt-format-selector")) {
            html = `
            <button class="buttons-select-all btn btn-sm btn-outline-secondary dt-filter-${fieldName}">
                <i class="ti ti-square-check"></i>
            </button>`;
            if (DATA.serverSide === false) {
                html += `<div style="display: none">
                    <select class="filter-input-prepend">
                        <option value="equals" data-content="<i class=\'ti ti-equal\'></i><small>${WJ.translate('datatables.select.equals.js')}</small>">${WJ.translate('datatables.select.equals.js')}</option>
                    </select>
                </div>`;
            }
            html += `<input class="form-control form-control-sm filter-input min max filter-input-id dt-filter-${fieldName}" type="text" />
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
                    dtWJ.updateFilterSelect(DATA, fieldName);
                }, 300);
            }
            html += '</select>';
        }

        // var popOver = '<a class="btn btn-sm btn-outline-secondary row-menu" data-placement="bottom" data-content="" role="button"><i class="ti ti-dots-vertical"></i></a>';

        var filterHtml = inputGroupBefore + html + inputGroupAfter;
        //console.log("filter["+i+"] typeof=", typeof DATA.columns[i].filter, " isTrue=", DATA.columns[i].filter === true);
        if (typeof DATA.columns[i].filter === "undefined" || DATA.columns[i].filter === true) {
            //console.log("Setting filter for ", fieldName, " html:", filterHtml, "this:", this);
            $(this).html(filterHtml);
        } else {
            if ($(this).hasClass("dt-format-selector")) {
                html = `
                <button class="buttons-select-all btn btn-sm btn-outline-secondary dt-filter-${fieldName}">
                    <i class="ti ti-square-check"></i>
                </button>
                `;
                $(this).html(inputGroupBefore + html + "</div></form>");
            } else {
                $(this).html("");
            }
        }

        if (extfilterExists) {
            //console.log("Setting ext filter for ", fieldName, " html: ", filterHtml);
            $("#" + DATA.id + "_extfilter .dt-extfilter-title-" + fieldName).text(DATA.columns[i].title);
            $("#" + DATA.id + "_extfilter .dt-extfilter-" + fieldName).attr("data-dt-column", i);
            $("#" + DATA.id + "_extfilter .dt-extfilter-" + fieldName).attr("data-dt-field-name", fieldName);
            $("#" + DATA.id + "_extfilter .dt-extfilter-" + fieldName).html(filterHtml);
        }

        $(this).find("input.filter-input").on("keypress", function(e) {
            if (e.key === "Enter") {
                //console.log("Mam keypress, e=", e);
                e.preventDefault();
                $(e.target).parent().find("button.filtrujem").trigger("click");
            }
        })

    });
    if (typeof TABLE != "undefined" && TABLE!=null && typeof TABLE.DATA != "undefined") fixDatatableHeaderInputs(TABLE);
}

/**
 * Adjust width of columns/filter inputs in header (sync table columns width with filter inputs)
 * @param {*} TABLE
 */
export function adjustColumns(TABLE) {
    try {
        //reset maxLenString - tooks me 10 hours to find this bug
        for (var column of TABLE.context[0].aoColumns) {
            column.maxLenString = null;
        }
        TABLE.columns.adjust();
    } catch (e) {
        console.error("Error adjusting columns: ", e);
    }
}