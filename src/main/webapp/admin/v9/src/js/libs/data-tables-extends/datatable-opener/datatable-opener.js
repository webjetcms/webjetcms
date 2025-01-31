import {Tools} from "../../tools/tools";

/**
 * class DatatableOpener
 * zalozene na AbstractJsTreeOpener
 */
export class DatatableOpener {

    /**
     * @description Nastavenie nového ID. Toto ID bude odoslané cez API.
     * @default folderid
     * @param {number|string} id
     * @returns {void}
     * @public
     * @setter
     */
    set id(id) {
        this.setNewId(id);
    }

    /**
     * @description Nový názov kľúča, v ktorom sa bude nachádzať ID.
     * @param {string} value
     * @returns {void}
     * @public
     * @setter
     */
    set idKeyName(value) {
        if (!Tools.empty(value) && this.idKeyWhiteList.indexOf(value) < 0) {
            /** @see idKeyWhiteList */
            Tools.log('warn', this.selfName(), `Key name '${value}' is not allowed. Please update your whitelist.`);
            return;
        }
        this._idKey = value;
    }

    /**
     * @description Nova hodnota pre whitelist klucov.
     * @param {array} value
     * @returns {void}
     * @public
     * @setter
     */
    set whiteList(value) {
        this.idKeyWhiteList = value;
    }

    constructor() {

        /**
         * @description Zoznam povolených query ID kľúčov.
         * @type {string[]}
         * @private
         */
        this.idKeyWhiteList = ['id'];

        /**
         * @description Hľadané koncové ID, ktoré chceme otvoriť.
         * @type {number}
         * @protected
         */
        this._currentId = -1;

        /**
         * @description URL query kľúč, z ktorého sa vyberie ID
         * @type {string}
         * @protected
         */
        this._idKey = 'id';

        /**
         * @description DataTable objekt
         * @private
         */
        this.dataTable = null;

        /**
         * @description jQuery element inputu pre zadávanie ID na stránke
         * @type {jQuery|null}
         * @private
         */
        this.dataInput = null;

        /**
         * Pocitadlo requestov strankovania, aby nedoslo k zacykleniu
         */
        this.failsafeCounter = 0;
    }

    /**
     * @description Inicializácia po načítaní
     * @returns {void}
     * @public
     */
    init() {
        /** @type {{}} */
        const query = Tools.getUrlQuery();
        /** @type {string|null|number} */
        let key = -1;
        if (!Tools.empty(this._idKey)) {
            key = query?.[this._idKey] ?? null;
            //console.log("mam key v URL, key=", key);
            if (key != null && key === "-1") {
                //console.log("Som -1, otvaram add okno, id=", this.dataTable.DATA.id);
                setTimeout(() => {
                    //$("#"+this.dataTable.DATA.id+"_wrapper div.dt-buttons button.buttons-create").trigger("click");
                    this.dataTable.wjCreate();
                }, 500);
            } else {
                key = this.validateId(key);
            }
        }

        //Tools.log('log', this.selfName(), 'init()', 'key=', key);
        this.initHashFilter();
        this.appendInputElement();
        this.bindEvents();

        if (key < 0) {
            return;
        }
        //Tools.log('log', this.selfName(), 'Url query init.', 'Key name:', this._idKey);

        this._currentId = key;
        this.id = key;
    }

    initHashFilter() {
        this.dataTable.one('draw.dt', (evt, settings) => {
            setTimeout(()=> {
                this.filterTableByHashParameters();
            }, 500);
        });
    }

    /**
     * @description Filter table by hash parameters #dt-filter-NAME=value&dt-filter-NAME=value...
     * @returns {void}
     * @public
     */
    filterTableByHashParameters() {
        try {
            let hash = window.location.hash;
            if (hash != "" && hash.length>5) {
                let array = hash.substring(1).split("&");
                let values;

                let firstInputName = null;
                let openEditor = false;
                let autoSelect = false;
                for (var i = 0; i < array.length; i += 1) {
                    if ("dt-open-editor=true"===array[i]) {
                        openEditor = true;
                        autoSelect = true;
                        continue;
                    }
                    if ("dt-select=true"===array[i]) {
                        autoSelect = true;
                        continue;
                    }

                    values = array[i].split("=");
                    if (values.length>0 && values[0].startsWith("dt-filter-")) {
                        let input = $("#"+this.dataTable.DATA.id+"_wrapper div.dt-scroll-headInner table thead .input-group input."+values[0]);
                        input.val(values[1]);
                        if (firstInputName==null) firstInputName = input;
                    }
                }
                if (firstInputName != null) {

                    this.dataTable.one('draw.dt', (evt, settings) => {
                        setTimeout(()=> {
                            if (autoSelect) {
                                $("#"+this.dataTable.DATA.id+"_wrapper div.dt-scroll-headInner table thead .input-group button.buttons-select-all").trigger("click");
                            }
                            if (openEditor) {
                                $("#"+this.dataTable.DATA.id+"_wrapper div.dt-buttons button.buttons-edit").trigger("click");
                            }
                        }, 100);
                    });
                    firstInputName.parent().find("button.filtrujem").trigger("click");
                }
            }
        } catch (e) {
            console.log(e);
        }
    }

    /**
     * @description Ak bol pripojený pomocou inputDataFrom() input, tak danému inputu môžeme odkiaľkoľvek nastaviť value.
     * @see inputDataFrom
     * @param {string|number} value
     * @returns {void}
     * @public
     */
    setInputValue(value) {
        if (!Tools.empty(this.dataInput, value)) {
            this.dataInput.val(value);
            Tools.updateUrlQuery(this._idKey, value, this.idKeyWhiteList);
        }
    }

    /**
     * @description Zmaže aktuálny zoznam a nastavý ID na -1
     * @returns {void}
     * @protected
     */
    reset() {
        this._currentId = -1;
        this.failsafeCounter = 0;
    }

    /**
     * @description nabinduje DT event na otvaranie datatabulky
     * @param {number} id
     * @returns {void}
     * @protected
     */
    bindEvents() {
        //console.log("bindEvents INIT");

        /**
         * @param {{aIds: Object}} settings V settings.aIds sa nachádza zoznam objektov všetkých prijatých dokumentov pre aktuálnu tabuľku.
         * @see https://datatables.net/manual/events#highlighter_922734
         */
        this.dataTable.on('draw.dt', (evt, settings) => {
            //POZOR: podobny kod je aj v abstract-js-tree-opener.js, ak tu robis upravy je potrebne ich preniest aj tam
            //v abstract-js-tree-opener.js je naviac this.dataTable.off('draw.dt');

            let id = this._currentId;
            if (!Tools.isNumeric(id) || Tools.empty(this.dataTable) || id<1) {
                return;
            }

            //console.log("bindEvents 2, id=", id, "settings=", settings, "aids=", settings?.aIds, "property=", !settings.aIds.hasOwnProperty(id));

            if (Tools.empty(settings?.aIds) && !settings.aIds.hasOwnProperty(id)) {
                return;
            }
            /**
             * @type {{end: number length: number, page: number, pages: number, recordsDisplay: number, recordsTotal: number, serverSide: boolean, start: number}} info
             * @see https://datatables.net/reference/api/page.info()
             */
            const info = this.dataTable.page.info();
            /** @type {number} */

            let rowForEditing = $('#' + this.dataTable.DATA.id + '_wrapper .datatableInit tr[id=' + id + ']');
            const maxPageNumberSearch = 5;

            if (rowForEditing.length>0) {
                //console.log("som na spravnej strane");
                /**
                 * @description Musíme odstrániť event, pretože by afektoval ostatné tabuľky.
                 * @see https://datatables.net/reference/api/off()
                 */

                //zrus hodnotu nastaveneho id
                this.reset();
                this.setInputValue(id);

                /** @description Otvoríme dokument */
                this.dataTable.wjEditFetch(rowForEditing);
            } else {
                //console.log("Som server side, hladaj");
                this.failsafeCounter++;
                if (info.page+1 < info.pages && this.failsafeCounter<maxPageNumberSearch) {
                    setTimeout(() => this.dataTable.page(info.page+1).draw('page'), 500);
                } else if (this.failsafeCounter<=maxPageNumberSearch) {
                    //console.log("Setting input field");
                    setTimeout(() => {
                        var $el = $("#" + this.dataTable.DATA.id + "_wrapper .dt-scroll-headInner .filter-input-id");
                        $el.val(""+id);
                        $el.parent().find("button.filtrujem").trigger("click");
                    }, 100);
                }
                else {
                    Tools.log('warn', `Record with id: '${id}' not found on any page, reseting`);
                    this.reset();
                }
            }
        });

        this.dataTable.EDITOR.on( 'open', ( e, type ) => {
            //console.log("Editor opened, data=", this.dataTable.EDITOR.currentJson, "editorId=", this.dataTable.DATA.editorId);
            if (typeof this.dataTable.EDITOR.currentJson != "undefined") {
                try { this.setInputValue(this.dataTable.EDITOR.currentJson[this.dataTable.DATA.editorId]); } catch (e) {}
            }
        });

        this.dataTable.EDITOR.on('preClose', () => {
            //odstran ?id z URL
            Tools.updateUrlQuery(this._idKey, "", this.idKeyWhiteList);
        });
    }

    /**
     * @description Nastaví nové store ID
     * @param {number} id
     * @protected
     */
    setNewId(id) {
        id = this.validateId(id);
        if (id < 0) {
            return;
        }
        this._currentId = id;
    }

    /**
     * @description Pripojí zadaný input, ktorý musí byť vložený ako jQuery objekt `$('.input-css-trieda')`
     *              Input zabezpečuje zadanie nového ID
     * @param {jQuery} openerInput
     * @returns {void}
     * @protected
     */
    bindInput(openerInput) {
        //console.log("bindInput, opener=", openerInput);
        if (!Tools.isJQuery(openerInput)) {
            return;
        }
        this.dataInput = openerInput;
        /** @type {jQuery|{is, show, hide, text}} */
        this.dataInput.on('keyup', evt => {
            if (evt.key === 'Enter' || evt.keyCode === 13) {
                //console.log("Bind input enter");
                /** @type {string} */
                const value = evt.currentTarget?.value?.replace(/\s+/g, '');
                this.id = value;
                //TODO: vyvolaj draw aby sa zavolal event
                this.dataTable.draw();
            }
        });
        this.dataInput.focus(() => this.dataInput.select());
    }

    /**
     * @description Zvaliduje a pretypuje vstupné ID
     * @param {string|null|number} id
     * @returns {number}
     * @private
     */
    validateId(id) {
        this.reset();
        if (Tools.empty(id) || !Tools.isNumeric(id)) {
            //Tools.log('warn', this.selfName(), 'Wrong ID format.', `'${id}'`, 'ID is set to -1');
            return -1;
        } else if (!Tools.isNumber(id)) {
            id = parseInt(id, 10);
        }
        return id;
    }

    /**
     * @returns {string}
     */
    selfName() {
        return `[${this.constructor.name}]`;
    }

    appendInputElement() {
        //console.log("appendInputElement");
        //console.log("appendInputElement, id=", this.dataTable.DATA.id);
        //console.log("row=", $("#"+this.dataTable.DATA.id+"_wrapper div.dt-header-row div.row"));
        let inputId = "datatable-"+this.dataTable.DATA.id+"-id";
        let label = window.WJ.translate("datatables.id.js");
        $("#"+this.dataTable.DATA.id+"_wrapper div.dt-header-row div.row div.col-auto .dt-buttons").prepend($(`
            <div id="${this.dataTable.DATA.id}InputWrapper" class="col-auto col-pk-input">
                <label for="${inputId}">${label} : </label>
                <input type="text" id="${inputId}"/>
            </div>
        `));
        //console.log("done");
        this.bindInput($("#"+inputId));
    }
}