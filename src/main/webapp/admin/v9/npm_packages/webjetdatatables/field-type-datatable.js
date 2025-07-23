import WJ from "../../src/js/webjet";

export function typeDatatable() {

    const resizeDatatable = function(conf) {
        //console.log("RESIZE---");
        //console.log("conf=", conf);
        let container = $("#"+conf._elementId+"_wrapper");

        if (container.is(':visible')) {
            //console.log("Setting size, container=", container);
            window.DTEContainer=container;

            //vypocitaj vysku elementov/inputov mimo datatabulky (ked mame v tabe aj ine inputy)
            let otherElementsHeight = 0;
            container.closest("div.panel-body").children("div.DTE_Field, div.form-group").each(function(index) {
                let element = $(this);
                if (element.hasClass("DTE_Field_Type_datatable")) return;
                //console.log("Counting elements: ", element);
                otherElementsHeight += element.outerHeight(true);
            });

            const DTE_Field = container.closest('.DTE_Field');
            if (otherElementsHeight>0) {
                DTE_Field.css('margin-left', '0px');
                DTE_Field.css('margin-right', '0px');
                DTE_Field.css('margin-bottom', '0px');
                DTE_Field.css('border-top', '1px solid #DDDFE6');
                otherElementsHeight+=15;
            } else {
                DTE_Field.css('margin', '0px');
            }
            container.closest('div.tab-pane').addClass("tab-pane-nopadding");
            //toto su velkosti hlavneho DTE
            const DTE = $('div.DTED.show .DTE').first();
            //const t = DTE.outerHeight();
            const a = $('div.DTED.show .DTE_Footer').first().outerHeight();
            const d = $('div.DTED.show .DTE_Header').first().outerHeight();
            const headerMarginTop = parseInt($('.DTE_Header').first().css("margin-top"));
            const tmt = parseInt(DTE.parent().css("margin-top"));
            const tmb = parseInt(DTE.parent().css("margin-bottom"));

            //toto su velkosti vnorenej DT
            const h = container.find('.dt-header-row').outerHeight();
            const f = container.find('.dt-footer-row').outerHeight();
            const s = container.find('.dt-scroll-head').outerHeight();

            //toto je vyska row-full-headline
            let headline = DTE_Field.find('div.row-full-headline').outerHeight();
            if (typeof headline == "undefined") headline = 0;
            //console.log("row-full-headline=", headline);

            const wh = $(window).height();

            //var height = t-a-d-h-f-s+4;
            var height = wh - tmt - tmb - a - d - h - f - s - otherElementsHeight - headline - headerMarginTop;
            if (height < 200) {
                height = 200;
            }
            //console.log("Set height, a=",a,"d=",d,"tmt=",tmt,"tmb=",tmb,"h=",h,"f=",f,"s=",s,"otherElementsHeight=",otherElementsHeight,"height=",height);

            const scrollBody = container.find('.dt-scroll-body');
            scrollBody.attr('style', 'height:' + height + 'px !important;');
            scrollBody.css({'overflow-y': 'auto'});
            //console.log("WJtable set style, height=", height);
        }
    }

    const getUrlWithParams = function(EDITOR, conf) {
        let url = conf.attr["data-dt-field-dt-url"];
        //url += "?docid={docId}&groupId={groupId}";
        //nahrad parametre z json objektu
        if (typeof url != "undefined" && url!=null && url.indexOf("{")!==-1) {
            url = url.replace(/{(.*?)}/gi, function(a, b){
                //Chceme real aktuálnu hodnotu
                try {
                    if (EDITOR.fields().includes(b)) {
                        return EDITOR.field(b).val();
                    }
                } catch (e) {}
                return EDITOR.currentJson[b];
            });
        }
        return url;
    }

    /**
     * @param {any} value
     * @returns {boolean}
     */
    const empty = value => typeof value === 'undefined' || value === null || value === '';

    /**
     * @param {Object} evt
     * @param {Object} conf
     * @returns {boolean}
     */
    const isCurrentTab = (evt, conf) => evt.detail.id.indexOf('-' + conf.tab) !== -1;

    /**
     * @param {Object} evt
     * @param {Object} conf
     * @returns {string}
     */
    const getCurrentTabName = (evt, conf) => isCurrentTab(evt, conf) ? conf.tab : '';

    /** @type {{[string]: boolean}} */
    let tabDataLoaded = {};

    /**
     * @param {Object} evt
     * @param {Object} conf
     * @returns {boolean}
     */
    const isLoaded = function(evt, conf) {
        /** @type {string} */
        const tabName = getCurrentTabName(evt, conf);
        /** @type {boolean} */
        const loaded = empty(tabName) || (!empty(tabName) && tabDataLoaded[tabName] === true);
        if (!loaded) {
            tabDataLoaded[tabName] = true;
        }
        return loaded;
    }

    return {
        create: function ( conf ) {
            //console.log("Creating DATATABLE field, conf=", conf, "this=", this);
            const id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._id = id;
            conf._elementId = "datatableField"+id;

            //tato jquery konstrukcia vytvori len pole objektov, nie su to este normalne elementy
            const htmlCode = $(`<textarea id="${id}" style="display: none;"></textarea><div class="datatableFieldType"><table id="${conf._elementId}" class="datatableInit table"></table></div>`);
            //htmlCode je pole elementov, input pole je prvy objekt v zapise (textarea)
            conf._input = $(htmlCode[0]);
            if (!empty(conf.attr)) {
                $.each(conf.attr, function( key, value ) {
                    //console.log("Setting attr: key=", key, " value=", value);
                    if (key.indexOf("data-dt-field-dt-")!==-1) {
                        return;
                    }
                    $(conf._input).attr(key, value);
                });
            }
            //tu bude ulozena instancia datatabulky
            conf.datatable = null;
            return htmlCode;
        },

        get: function ( conf ) {
            //var json = conf.datatable.data;
            //vratime prazdnu hodnotu, kedze data sa posielaju v samostatnych REST volaniach vnorenej datatabulky
            const json = [];

            let isLocalJson = false;
            if (!empty(conf.attr)) {
                $.each(conf.attr, function (key, value) {
                    if (key === "data-dt-field-dt-localJson") {
                        isLocalJson = true;
                    }
                });
            }

            //console.log("isLocalJson=", isLocalJson, "conf=", conf);

            if (isLocalJson) {
                let datatable = conf.datatable;
                if (typeof datatable !== "undefined" && datatable != null) {
                    let indexes = datatable.rows({ order: 'applied' }).indexes();
                    for (let i = 0; i < indexes.length; i++) {
                        let rowData = datatable.row(indexes[i]).data();
                        json.push(rowData);
                    }
                    //console.log("Returning json for localJson: ", json);

                    //remove ID and rowOrder columns from json
                    for (let i = 0; i < json.length; i++) {
                        delete json[i].id;
                        delete json[i].rowOrder;
                    }

                    return json;
                } else {
                    console.log("Returning original value, datatable is not initialized", conf.originalValue);
                    //send original value, DT was not initialized
                    return(conf.originalValue);
                }
            }

            //console.log("Returning json ("+conf.className+"): ", json);
            return json;
        },

        set: function (conf, val) {
            const EDITOR = this;
            conf.originalValue = val;

            // Pri novom datatable draw() resetne loaded a zruší eventy
            tabDataLoaded = {};
            window.removeEventListener('WJ.DTE.tabclick', function (evt) {
                onTabClickInit(evt, conf);
            });
            window.removeEventListener('WJ.DTE.tabclick', function (evt) {
                onTabClickResize(evt, conf)
            });

            function onTabClickInit(evt, conf) {
                if (!isCurrentTab(evt, conf) || isLoaded(evt, conf)) {
                    return;
                }
                const url = getUrlWithParams(EDITOR, conf);

                //vue kompoennta este nebola inicializovana, pri opakovanom otvoreni DTED sa toto uz nevola
                //console.log("DATATABLE SET, conf=", conf);

                if (!empty(conf.datatable)) {
                    //console.log("docid=", EDITOR.currentJson["docId"]);
                    //pri opakovanom otvoreni DTED uz len reloadni data
                    //console.log("Setting URL:", url, "dted=", conf.datatable);
                    conf.datatable.rows().deselect();
                    conf.datatable.setAjaxUrl(url);
                    conf.datatable.ajax.reload();
                    conf.datatable.DATA.url = url;
                    conf.datatable.EDITOR.ajax().url = WJ.urlAddPath(url, '/editor');
                    return;
                }

                //console.log("currentJson=", EDITOR.currentJson);
                //toto nastava ked sa prvy krat inicializuje DTED a este nema data
                if (empty(EDITOR.currentJson)) {
                    return;
                }

                let columns = JSON.parse(conf.attr["data-dt-field-dt-columns"]);
                //console.log("WJ=", WJ, "element=", conf._elementId, "url=", url, "columns=", columns);
                $("#" + conf._elementId).on('init.dt', function () {
                    //console.log("SOM INIT......");
                    //console.log("$('.datatableFieldType').length=", $('.datatableFieldType').length);
                    resizeDatatable(conf);

                    //pre istotu nastav interval na 20 sekund na resize
                    setInterval(function () {
                        resizeDatatable(conf);
                    }, 20000);

                    //resizni datatable pri zmene velkosti okna
                    let resizeTimer;
                    $(window).on("resize", function () {
                        clearTimeout(resizeTimer);
                        resizeTimer = setTimeout(function () {
                            resizeDatatable(conf)
                        }, 100);
                    });

                    //resizni pri kliknuti na tab
                    window.addEventListener('WJ.DTE.tabclick', function (evt) {
                        onTabClickResize(evt, conf)
                    });
                });
                //console.log("Inner table, conf=", conf);

                const dtConf = {
                    id: conf._elementId,
                    url: url,
                    serverSide: true,
                    columns: columns,
                    nestedModal: true,
                    fetchOnEdit: true,
                    fetchOnCreate: true,
                    idAutoOpener: false,
                    autoHeight: false,
                    localJson: false
                };

                //dopln atributy nastavene z anotacie
                if (!empty(conf.attr)) {
                    $.each(conf.attr, function (key, value) {
                        const annotation = 'data-dt-field-dt-';
                        const name = key.substring(annotation.length);
                        if (key.indexOf(annotation + "columns-customize") !== -1) {
                            if (typeof window[value] === "function") {
                                columns = window[value](columns);
                                dtConf.columns = columns;
                            }
                            return;
                        }
                        if (key.indexOf(annotation + "url") !== -1 || key.indexOf(annotation + "columns") !== -1) {
                            return;
                        }
                        if (key.indexOf(annotation + "tabs") !== -1) {
                            //json needs to have " but in Java is simpler to use ', so replace it
                            value = value.replace(/'/gi, "\"");
                            var tabs = JSON.parse(value);
                            dtConf[name] = tabs;
                            return;
                        }
                        if (annotation + "order" === key) {
                            //order nam chodi ako string, ale potrebujeme ho mat ako objekt
                            const tokens = value.split(",");
                            if (tokens.length === 2) {
                                value = [[parseInt(tokens[0]), tokens[1]]];
                            }
                            else if (value=="") value = [];
                        }
                        if ("true" === value || "false" === value) {
                            value = eval(value);
                        }
                        //console.log("Setting dtConf: key=", key, "name=", name, " value=", value);
                        dtConf[name] = value;
                    });
                }

                //console.log("dtConf=", dtConf);

                if (true === dtConf.localJson) {
                    //console.log("its localJson, setting data, val=", val);
                    if (typeof val === "undefined" || val == null || val === "") val = [];
                    if (typeof val === "string") {
                        //if value is string, decode it
                        try {
                            //console.log("Parsing JSON string:", val);
                            val = JSON.parse(val);
                        } catch (e) {
                            console.log("Error parsing JSON string:", e);
                            val = [];
                        }
                    }

                    //console.log("decoded val=", val);

                    dtConf.url = null;
                    dtConf.src = {
                        data: val
                    }
                    dtConf.serverSide = false;
                    dtConf.fetchOnEdit = false;
                    dtConf.fetchOnCreate = false;
                    dtConf.rowReorder = true;

                    // and ROW_ORDER column as sedond item
                    dtConf.columns.unshift({
                        data: 'rowOrder',
                        name: 'rowOrder',
                        title: WJ.translate('datatables.rowReorder.js'),
                        renderFormat: "dt-format-row-reorder",
                        filter: false,
                        editor: {
                            type: "text",
                            attr: {
                                type: "number"
                            }
                        },
                        array: false
                    });

                    //add ID column as first item in dtConf.columns
                    dtConf.columns.unshift({
                        data: 'id',
                        name: 'id',
                        title: WJ.translate('datatables.id.js'),
                        renderFormat: "dt-format-selector",
                        className: "dt-select-td cell-not-editable",
                        editor: {
                            type: "hidden",
                            required: false
                        },
                        array: false
                    });
                }

                //console.log("Creating datatable, conf=", conf, "dtConf=", dtConf);

                conf.datatable = WJ.DataTable(dtConf);

                //if (typeof window.datatableInnerTable == "undefined") {window.datatableInnerTable = [];}
                let windowVariableName = "datatableInnerTable_" + conf.name;
                windowVariableName = windowVariableName.replace(/[- .]/gi, '_');
                //console.log("Setting global object, windowVariableName=", windowVariableName);
                window[windowVariableName] = conf.datatable;

                //firni event
                WJ.dispatchEvent('WJ.DTE.innerTableInitialized', {
                    conf: conf
                });
            }

            function onTabClickResize(evt, conf) {
                if (!isCurrentTab(evt, conf)) {
                    return;
                }
                //console.log("tabclick, evt=", evt, "tab=", conf.tab);

                //nieco pridava do filtrov form-control form-select co pokazi ich zobrazenie
                //deje sa to len pri druhom otvoreni okna
                let container = $("#"+conf._elementId+"_wrapper");
                container.find("div.dropdown.bootstrap-select.filter-input-prepend.binded").each(function() {
                    let $this = $(this);
                    $this.removeClass("form-control");
                    $this.removeClass("form-select");
                });

                //id=datatableInit-media ale conf.tab je len media, takze testujeme bez ohladu na meno parent datatabulky
                resizeDatatable(conf);
                //redrawni tabulku
                setTimeout(function () {
                    //console.log("Redrawing table");
                    conf.datatable.columns.adjust();
                }, 100);
            }

            window.addEventListener('WJ.DTE.tabclick', function (evt) {
                onTabClickInit(evt, conf);
            });
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