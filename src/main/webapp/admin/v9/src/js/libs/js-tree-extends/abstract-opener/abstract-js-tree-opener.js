import {Tools} from "../../tools/tools";

/**
 * @abstract class AbstractJsTreeOpener
 */
export default class AbstractJsTreeOpener {

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
     * @description Nastavenie novej API url
     * @param {string} value
     * @example
     *          API url bez ID
     *          `/admin/rest/web-pages/parents/`
     * @returns {void}
     * @public
     * @setter
     */
    set apiUrl(value) {
        this._apiUrl = value;
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
     * @description Slúži na zistenie, či po prijatí dát, existuje v jsTree uzol, ktorý chceme otvoriť ako prvý.
     *              Ak takýto uzol neexistuje (nebol nájdený / not found), tak sa nám vráti hodnota TRUE
     *              Ak takýto uzol existuje (bol nájdený / found), tak sa nám vráti hodnota FALSE
     *              Ideálne použitie tohto gettera je vo vnútri metódy loaded()
     * @returns {boolean}
     * @public
     * @getter
     */
    get notFound() {
        return this._notFound;
    }

    constructor() {

        /**
         * @description Zoznam povolených query ID kľúčov.
         * @type {string[]}
         * @private
         */
        this.idKeyWhiteList = ['docid', 'groupid'];

        /**
         * @description Hľadané koncové ID, ktoré chceme otvoriť.
         * @type {number}
         * @protected
         */
        this._currentId = -1;

        /**
         * @description Callback, ktorý sa nastavuje pomocou metódy loaded() v child triedach, ktoré volajú loaded()
         * @see loaded
         * @type {Function}
         * @protected
         */
        this.loadedCallback = null;

        /**
         * @description URL query kľúč, z ktorého sa vyberie ID
         * @type {string}
         * @protected
         */
        this._idKey = '';

        /**
         * @description Adresa pre API, ktorá bude zavolaná pre získanie dát.
         * @type {string}
         * @example /admin/rest/web-pages/parents/{this._currentId}
         * @see _currentId
         * @protected
         */
        this._apiUrl = '';

        /**
         * @description Ak neexistuje jsTree adresárový uzol
         * @type {boolean}
         * @protected
         */
        this._notFound = false;

        /**
         * @description jQuery element jsTree. Slúži na vyhľadávanie uzlov pre metódu nodeExist()
         * @see nodeExist
         * @private
         */
        this.jstreeElement = null;

        /**
         * @description JsTree objekt
         * @private
         */
        this.jstree = null;

        /**
         * @description DataTable objekt
         * @private
         */
        this.dataTable = null;

        /**
         * @description Ajax objekt
         * @type {jQuery|{done, fail, abort}}
         * @see load
         * @private
         */
        this.request = null;

        /**
         * @description Nastaví sa na TRUE pri inicializácii ak je zadané správne ID.
         * @type {boolean}
         * @private
         */
        this.initial = false;

        /**
         * @description Úložisko prijatých dát.
         * @type {number[]}
         * @private
         */
        this.treeNodesStore = [];

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

         /**
          * Originalna query pri inicializacii objektu
          */
         this.query = null;
    }

    /**
     * @description Inicializácia po načítaní jstree
     * @returns {void}
     * @public
     */
    init() {
        /** @type {{}} */
        this.query = Tools.getUrlQuery();
        //console.log("query=", this.query);
        /** @type {string|null|number} */
        let key = -1;
        if (!Tools.empty(this._idKey)) {
            key = this.query?.[this._idKey] ?? null;

            //we are injecting jstree from server side
            if (this._idKey=="docid" && key != null && key != "-1") {
                var docid = key;
                key = null;
                setTimeout(() => {
                    //console.log("Opening editor for docid=", docid);
                    this.openEditor(docid);
                }, 100);
            }

            if (key != null && this._idKey!="docid") {
                let docid = this.query?.["docid"] ?? null;
                if (docid != null && docid != "-1") {
                    //ak je zadane docid hodnotu groupid ignorujeme, ziska sa podla parentov docid
                    key = null;
                }
            }

            //console.log("mam key v URL, key=", key, "datatable=", this.dataTable);
            if ("SYSTEM" === key || "TRASH" === key) {
                //console.log("prepinam na kartu system");
                //klikni na kartu System a vyvolaj pokracovanie
                let tab = key;
                let self = this;
                setTimeout(() => {
                    this.switchTab(tab, false);
                    this.waitForJsTreeLoaded(function() {
                        self._currentId = 0;
                        self.next();
                    });
                    //console.log("after wait");
                }, 300);

                key = 0;
            }
            else if (key != null && key === "-1" && this.dataTable != null) {
                if (this._idKey!="docid") {
                    //pre samotne docid=-1 nerobime nic, pretoze to vyzaduje aj zadane groupid, vyvola sa to po skonceni loadingu stromu
                    //console.log("Som -1, otvaram add okno, id=", this.dataTable.DATA.id);
                    setTimeout(() => {
                        $("#"+this.dataTable.DATA.id+"_wrapper div.dt-buttons button.buttons-create").trigger("click");
                    }, 500);
                }
            } else {
                key = this.validateId(key);
            }
        }

        //groupid otvarame serverovo
        if (this._idKey == "groupid") {
            //over, ci je nod v initial data, ak ano, otvori sa sam
            if (window.treeInitialJson != null) {
                window.treeInitialJson.forEach((item) => {
                    if (item.id == key) {
                        //console.log("Mam v datach, key=", key, "item=", item);
                        key = -1;
                    }
                });
            }
        }

        if (key == 0) key = -1;
        //console.log("key=", key);
        //Tools.log('log', this.selfName(), 'init()');

        if (key < 0) {
            return;
        }
        //Tools.log('log', this.selfName(), 'Url query init.', 'Key name:', this._idKey);

        this.initial = true;
        this._currentId = key;
        this.id = key;
    }


    /**
     * @description Nastaví callback, ktorý sa vykoná po prijatí dát.
     * @param {Function} callback [result, docId, selfClass]
     * @returns {*|this}
     * @public
     */
    loaded(callback) {
        this.loadedCallback = null;
        if (typeof callback === 'function') {
            this.loadedCallback = callback;
        }
        return this;
    }


    /**
     * @description Ak bol pripojený pomocou inputDataFrom() input, tak danému inputu môžeme odkiaľkoľvek nastaviť value.
     * @see inputDataFrom
     * @param {string|number} value
     * @returns {void}
     * @public
     */
    setInputValue(value) {
        if (!Tools.empty(this.dataInput, value) && value != -1) {
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
        if (!Tools.isNull(this.request)) {
            this.request.abort();
            this.request = null;
        }
        this.setCurrentStore([]);
        this._currentId = -1;
        this.failsafeCounter = 0;
    }

    /**
     * @description Po zavolaní otvorí nasledujúci uzol v jsTree na základe jeho ID.
     * @param {number} id
     * @returns {void}
     * @protected
     */
    openNextNode(id) {
        //console.log("openNextNode, id=", id);
        if (!Tools.isNumeric(id)) {
            return;
        }

        this.waitForJsTreeLoaded(findAndOpen);
        let self = this;
        function findAndOpen()
        {
            //console.log("findAndOpen, store=", self.getCurrentStore(), "notFound=", self._notFound);
            if (!self._notFound) {
                if (Tools.empty(self.getCurrentStore())) {
                    //console.log("select node: ", id);
                    setTimeout(()=> {
                        self.jstree.deselect_all();
                        self.jstree.select_node(id);
                    }, 100);
                } else {
                    //console.log("open node: ", id);
                    let node = self.jstree.get_node(id);
                    //console.log("Node ", node.id, "loaded=", node?.state?.loaded);
                    if (node?.state?.opened === true && node?.state?.loaded === true) {
                        //nod uz je otvoreny, volaj dalsi
                        //console.log("Uz je otvoreny");
                        setTimeout(()=> {
                            self.jstree.trigger('after_open.jstree');
                        }, 10);
                    } else {
                        //console.log("Opening node, jstree=", self.jstree);
                        self.jstree.open_node(id);
                    }
                }
            } else {
                self.reset();
                self.loadedCallback = null;
            }
        }
    }

    /**
     * @description caka kym this.jstree != null a zavola callback
     * @param {function} callback
     * @returns {void}
     * @protected
     */
    waitForJsTreeLoaded(callback) {
        if (Tools.empty(this.jstree, this.dataTable)) {
            this.setTreeAndTable();
        }

        let treeEl = $('#SomStromcek');
        let self = this;

        // 30s = 30 000ms = 100 * 300
        let intervalMaxCount = 100;
        let intervalCount = 0;
        let interval = setInterval(() => {
            //console.log("waitForJsTreeLoaded, intervalCount=", intervalCount);
            intervalCount++;
            if (intervalCount >= intervalMaxCount) {
                Tools.log('info', self.selfName(), `Max interval count reached. JSTree not loaded`);
                clearInterval(interval);
                interval = null;
                return;
            }

            if (self.jstree == null || treeEl.hasClass("jstree-loading")) {
                //console.log("RETURNING, ma loading");
                return;
            }

            //console.log("Executing callback=", callback);
            if (interval != null) {
                clearInterval(interval);
                interval = null;
            }
            Tools.exec(callback);
        }, 300);
    }


    /**
     * @description caka kym sa nacita datatable a nasledne zavola callback
     * @param {int} id
     * @param {function} callback
     * @returns {void}
     * @protected
     */
    waitForDatatableRowLoaded(id, callback) {
        if (Tools.empty(this.jstree, this.dataTable)) {
            this.setTreeAndTable();
        }
        // 30s = 30 000ms = 100 * 300
        let intervalMaxCount = 100;
        let intervalCount = 0;
        let processingHiddenCount = 0;
        let datatableWrapperEl = $('#datatableInit_wrapper');
        let that = this;
        var interval = setInterval(() => {
            intervalCount++;

            if (intervalCount >= intervalMaxCount) {
                Tools.log('info', that.selfName(), `Max interval count reached. Datatable with row with id ${id} not loaded`);
                clearInterval(interval);
                interval = null;
                return;
            }

            //console.log("intervalCount=", intervalCount, "processing=", datatableWrapperEl.find("div.dataTables_processing:visible").length);
            if (datatableWrapperEl.find("div.dataTables_processing:visible").length>0) {
                //je zobrazene processing, nemozeme pokracovat, cakame (interval bezi)
                processingHiddenCount = 0;
                return;
            } else {
                if (processingHiddenCount++ < 3) {
                    //aspon 3 intervaly musi byt schovany loader aby sme pokracovali na callback
                    return;
                }
            }

            //zrus volanie intervalu a zavolaj callback
            if (interval != null) {
                clearInterval(interval);
                interval = null;
            }
            //console.log("Executing callback=", callback);
            Tools.exec(callback);
        }, 300);
    }

    /**
     * Edit webpage with fetch, handle historyid in parameter (load version from history)
     * @param {number} docId
     */
    wjEditFetchHistory(docId) {
        var datatableUrl = this.dataTable.ajax.url();
        var datatableUrlBeforeLoad = datatableUrl;

        //get historyId parameter from URL
        const url = window.location.href;
        const urlParams = new URLSearchParams(url);
        const historyId = urlParams.get('historyid');
        if (historyId) {
            //console.log("mam historyId=", historyId);
            datatableUrlBeforeLoad = window.WJ.urlAddParam(datatableUrl, "historyId", historyId);

            this.dataTable.ajax.url(datatableUrlBeforeLoad);
            this.dataTable.wjEditFetch($('.datatableInit tr[id=' + docId + ']'));
            this.dataTable.ajax.url(datatableUrl);

            //remove historyId from URL parameter
            Tools.updateUrlQuery("docid", docId, ["historyid", "docid"]);
        } else {
            this.dataTable.wjEditFetch($('.datatableInit tr[id=' + docId + ']'));
        }

    }

    /**
     * @description Otvorí dokument v dataTable na základe jeho ID a v prípade potreby skočí na podstránku kde sa nachádza.
     * @param {number} id
     * @returns {void}
     * @protected
     */
    openEditor(id) {
        if (!Tools.isNumeric(id)) {
            Tools.log('info', this.selfName(), `Id: ${id} is not numeric or dataTable is empty`);
            return;
        }

        let self = this;

        //najskor over, ci tam taku stranku rovno aj nemame
        let webpageRow = $("#datatableInit tr[id="+this._currentId+"]");
        //console.log("webpageRow=", webpageRow);
        if (webpageRow.length>0) {
            //console.log("Opening webpage, id=", this._currentId);
            this.wjEditFetchHistory(this._currentId);
            return;
        }

        this.waitForDatatableRowLoaded(id, setDrawEventAndRedraw);

        function setDrawEventAndRedraw() {
            //console.log("setDrawEventAndRedraw");
            self.dataTable.on('draw.dt', datatableDraw);
            try {
                datatableDraw({}, self.dataTable.settings()[0]);
            } catch (e) {
                self.dataTable.draw();
            }
        }

        /**
         * @param {{aIds: Object}} settings V settings.aIds sa nachádza zoznam objektov všetkých prijatých dokumentov pre aktuálnu tabuľku.
         * @see https://datatables.net/manual/events#highlighter_922734
         */
        function datatableDraw(evt, settings) {
            //POZOR: podobny kod je aj v datatable-opener.js, ak tu robis upravy je potrebne ich preniest aj tam
            //v abstract-js-tree-opener.js je naviac self.dataTable.off('draw.dt');
            if (!Tools.isNumeric(id) || Tools.empty(self.dataTable) || id<1) {
                Tools.log('info', self.selfName(), `Id: ${id} is not numeric or dataTable is empty or id < 1`);
                return;
            }

            //console.log("bindEvents 2, id=", id, "settings=", settings, "aids=", settings?.aIds, "property=", !settings.aIds.hasOwnProperty(id));
            if (Tools.empty(settings?.aIds) && !settings.aIds.hasOwnProperty(id)) {
                Tools.log('info', self.selfName(), `Settings aIds empty or settings.aIds.hasOwnProperty(id) false`);
                return;
            }
            /**
             * @type {{end: number length: number, page: number, pages: number, recordsDisplay: number, recordsTotal: number, serverSide: boolean, start: number}} info
             * @see https://datatables.net/reference/api/page.info()
             */
            const info = self.dataTable.page.info();
            /** @type {number} */
            const idIndex = Object.keys(settings.aIds).indexOf(id.toString());

            //self.dataTable.off('draw.dt');
            /**
             * @description Test `info.length < 0` je kvôli tomu, že ak je nastavené zobrazenie všetkých dát na stránku, tak je v length hodnota `-1`
             * @type {number} pageNumber
             */
             const pageNumber = info.length < 0 ? 0 : Math.floor(idIndex / info.length);
             const maxPageNumberSearch = 5;

             //console.log("idIndex=", idIndex, "info.page=", info.page, "info.pages=", info.pages, "pageNumber=", pageNumber, "failsafe=", self.failsafeCounter);
             if (idIndex >= 0 && pageNumber === info.page || (idIndex!=-1 && self.dataTable.DATA.serverSide===true)) {
                 //console.log("som na spravnej strane");
                 /**
                  * @description Musíme odstrániť event, pretože by afektoval ostatné tabuľky.
                  * @see https://datatables.net/reference/api/off()
                  */

                 //zrus hodnotu nastaveneho id
                 self.reset();
                 self.setInputValue(id);
                 self.dataTable.off('draw.dt');

                 /** @description Otvoríme dokument */
                 self.wjEditFetchHistory(id);
             } else if (idIndex >= 0) {
                 Tools.log('info', self.selfName(), `Document with id: ${id} was found on page ${pageNumber + 1}`);
                 /**
                  * @description Je to v timeoute, pretože to znejakého dôvodu ovplyvňovalo chovanie pagingu a vracalo to na stranu 1.
                  * @todo Treba prísť na to ako odstrániť ten setTimeout, je to škaredé.
                  * @see https://datatables.net/reference/api/page()
                  */
                 setTimeout(() => self.dataTable.page(pageNumber).draw('page'), 500);
             } else if (self.dataTable.DATA.serverSide===true) {
                 self.failsafeCounter++;
                 //console.log("Som server side, hladaj, failsafe=", self.failsafeCounter);
                 if (info.page+1 < info.pages && self.failsafeCounter<maxPageNumberSearch) {
                    var timeout = 100;
                    if (self.failsafeCounter>5) timeout = 300;
                    setTimeout(() => {
                        //console.log("idem na stranu ", info.page+1);
                        self.dataTable.page(info.page+1).draw('page');
                    }, timeout);
                 } else if (self.failsafeCounter<=maxPageNumberSearch) {
                    //console.log("Setting input field");
                    setTimeout(() => {
                        //self.dataTable.column(0).search(""+id).draw();
                        var $el = $("#" + self.dataTable.DATA.id + "_wrapper .dataTables_scrollHeadInner .filter-input-id");
                        $el.val(""+id);
                        $el.parent().find("button.filtrujem").trigger("click");
                    }, 100);
                 }
                 else {
                     Tools.log('warn', `Document with id: '${id}' not found on any page, reseting`);
                     self.reset();
                     self.dataTable.off('draw.dt');

                     //try to search for it

                 }
             } else {
                 Tools.log('warn', `Document with id: '${id}' not found`);
                 self.reset();
                 self.dataTable.off('draw.dt');
             }
        }
    }

    /**
     * @description Vráti všetky array prijatých ID ktoré ešte zozstali.
     * @returns {number[]}
     * @protected
     */
    getCurrentStore() {
        /** @type {number[]} */
        const store = this.treeNodesStore;
        return Tools.empty(store) || !Tools.isArray(store) ? [] : store;
    }

    /**
     * @description Vráti ďalšiu hodnotu zo zoznamu. Zároveň túto hodnotu zmaže zo zoznamu.
     * @returns {null|number}
     * @protected
     */
    getNextStoreItem() {
        return Tools.empty(this.treeNodesStore) ? null : this.treeNodesStore.shift();
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
        this.setCurrentStore([]);
        this.load(this.loadedCallback);
    }

    /**
     * @description Pripojí zadaný input, ktorý musí byť vložený ako jQuery objekt `$('.input-css-trieda')`
     *              Input zabezpečuje zadanie nového ID
     * @param {jQuery} openerInput
     * @param {*} forOpenerClass
     * @param {function} [withNotifyCallback]
     * @returns {void}
     * @protected
     */
    bindInput(openerInput, forOpenerClass, withNotifyCallback) {
        if (!Tools.isJQuery(openerInput)) {
            return;
        }
        this.dataInput = openerInput;
        /** @type {jQuery|{is, show, hide, text}} */
        this.dataInput.on('keyup', evt => {
            if (evt.key === 'Enter' || evt.keyCode === 13) {
                /** @type {string} */
                const value = evt.currentTarget?.value?.replace(/\s+/g, '');
                if (!Tools.empty(value)) {
                    forOpenerClass.loaded(() => {
                        this.notFound ? Tools.exec(withNotifyCallback, value) : forOpenerClass.next();
                    }).id = value;
                }
            }
        });
        this.dataInput.focus(() => this.dataInput.select());
    }

    switchTab(tab, forceSwitch=false) {
        //console.log("Swtiching tab to: ", tab);
        let treeMapping = {
            'FOLDER': '#pills-folders-tab',
            'SYSTEM': '#pills-system-tab',
            'TRASH': '#pills-trash-tab'
        },
        tabsEl = $('#pills-folders'),
        tabEl = tabsEl.find(treeMapping[tab]);

        //console.log("switchTab, tab=", tab, "forceSwitch=", forceSwitch);

        //console.log("Switching to: ", tab, "el=" , tabEl);
        if (tabEl.hasClass("active") && forceSwitch===false) {
            //console.log("Tab is allready active, returning");
            return false;
        }

        // aktivovanie spravneho tabu, ak to je system alebo kos musime kliknut aj na root, aby sme nasli stranku v roote aj ked je aktivny
        if (!tabEl.hasClass('active') || tab != "FOLDER") {
            tabsEl.find('.active').removeClass('active');
            tabEl.addClass('active');
            tabEl.trigger('click');
        }
        return true;
    }

    /**
     * @description Zavolá API a získa dáta.
     * @param {function} [callback]
     * @returns {void}
     * @private
     */
    load(callback) {
        if (Tools.empty($, $?.get)) {
            Tools.log('error', 'jQuery "$" or jQuery get "$.get()" not found.');
            return;
        }
        this.request = $.get(this._apiUrl + this._currentId);
        this.request.done(result => {
            if (Tools.isObject(result)) {
                let parents = result.parents;
                if (!result.found) {
                    // zobrazenie chybovej hlasky, ze stranku sme nenasli
                    this._notFound = true;
                    Tools.exec(callback);
                    this.reset();
                    return;
                }

                let selectedDomain = $('#header\\.currentDomain').val(),
                    domain = result.domain;

                //console.log("domain=", domain, "selectedDomain=", selectedDomain, "result=", result);
                if (domain != null && domain != "null" && domain != "" && domain !== selectedDomain) {
                    // presmerovenie, ak je stranka pre inu domenu
                    window.location.href = '/admin/v9/webpages/web-pages-list/?'+this._idKey+'='+this._currentId;
                    return;
                }

                let tab = result.tab;
                var forceSwitch = result.parents.length==0;
                this.switchTab(tab, forceSwitch);

                this.setCurrentStore(parents);
                this.waitForJsTreeLoaded(loadCallback);

                let self = this;
                function loadCallback() {
                    // oneskorenie pre nacitanie daneho tabu
                    if (!Tools.empty(self.jstree)) {
                            self.jstree.deselect_all();
                            //self.jstree.close_all();
                    }

                    self.resultValidation(parents);

                    Tools.exec(callback, parents, self._currentId, this);
                    self.loadedCallback = null;
                }
            } else {
                Tools.log('warn', 'Not found.', result);
            }
        });
        this.request.fail(xhr => {
            switch (xhr.status) {
                case 403:
                    Tools.log('error', 'JsTreeOpener.load()', 'Error:', xhr.status, 'Unauthorized: Check your CSRF Token', {'CSRFtoken': window?.csrfToken});
                    break;
                case 404:
                    Tools.log('error', 'JsTreeOpener.load()', 'Error:', xhr.status, 'Not Found.');
                    break;
                case 500:
                    Tools.log('error', 'JsTreeOpener.load()', 'Error:', xhr.status, 'Internal Server error.');
                    break;
                case 0:
                    Tools.log('info', 'JsTreeOpener.load()', 'Request abort by user.');
                    break;
                default:
                    Tools.log('error', 'JsTreeOpener.load()', 'Unexpected error.', 'Error:', xhr.status);
            }
        });
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
            if (!this.initial) {
                //Tools.log('warn', this.selfName(), 'Wrong ID format.', `'${id}'`, 'ID is set to -1');
            }
            return -1;
        } else if (!Tools.isNumber(id)) {
            id = parseInt(id, 10);
        }
        return Math.abs(id);
    }

    /**
     * @description Zvaliduje prijaté dáta z API a ak žiadne neprídu nastavý sa premnná notFound
     * @param {Array} result
     * @returns {void}
     * @private
     */
    resultValidation(result) {
        if (!Tools.empty(result)) {
            /** @type {number} */
            const id = result[0];
            this._notFound = !this.nodeExist(id);
            if (this._notFound) {
                Tools.log('warn', `Node with id '${id}' not found.`);
            } else {
                // Tools.log('info', response);
            }
        }
        if (this._notFound) {
            this.reset();
        }
    }

    /**
     * @description Overí, či existuje v jsTree uzol so zadaným ID
     * @param {number} id
     * @returns {boolean}
     * @private
     */
    nodeExist(id) {
        if (this.initial) {
            this.initial = false;
            return true;
        }
        return !Tools.empty(id) && this.jstreeElement.find('#' + id).length > 0;
    }

    /**
     * @description Zistí, či sú dostupné objekty a funkcie jsTree a DataTable a následne ich setne do triedy alebo vypíše error Log.
     * @returns {void}
     * @private
     */
    setTreeAndTable() {
        if (!Tools.empty(
            window?.jstree?.jstree(true)?.deselect_all,
            window?.jstree?.jstree(true)?.close_all,
            window?.jstree?.jstree(true)?.open_node,
            window?.jstree?.jstree(true)?.select_node
        )) {
            this.jstreeElement = window.jstree;
            this.jstree = window.jstree.jstree(true);
        } else {
            Tools.log('error', 'jsTree not found!');
        }

        if (!Tools.empty(window?.webpagesDatatable?.wjEditFetch)) {
            this.dataTable = window.webpagesDatatable;
        } else {
            Tools.log('warn', 'dataTables not found!')
        }
    }

    /**
     * @description Pridá do úložiska dáta. Ak úložisko neexistuje zadefinuje sa prázne.
     * @param {?array} data
     * @returns {void}
     * @private
     */
    setCurrentStore(data) {
        this.treeNodesStore = !Tools.isArray(data) ? [] : data;
    }

    /**
     * @returns {string}
     */
    selfName() {
        return `[${this.constructor.name}]`;
    }
}