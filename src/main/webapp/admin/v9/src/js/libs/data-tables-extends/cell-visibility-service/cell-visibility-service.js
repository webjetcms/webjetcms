import {StorageHandler} from "../../storage-handler/storage-handler";
import {Tools} from "../../tools/tools";

export class CellVisibilityService {

    constructor() {
        /**
         * @type {StorageHandler}
         * @private
         */
        this.store = new StorageHandler();
        this.store.setStoreName('CellVisibilityStore');

        /**
         * @type {string|null}
         * @private
         */
        this.dataTableId = null;
    }

    /**
     * @description Z objektu dataTable, vyberie zoznam dostupných fieldov a vyberie z nich názov fieldu (data) a atribút visible (bVisible).
     * @example
     *              Vo fielde chodia atribúty `visible` a `bVisible`.
     *
     *              `visible` - predošlá hodnota / tá nás nezaujíma /
     *              `bVisible` - aktuálna (nová) hodnota
     *
     * @param {Object} dataTable
     * @returns {void}
     * @public
     */
    buildConfigDataFromObject(dataTable) {
        if (
            Tools.empty(dataTable, dataTable?.context, dataTable?.DATA?.id)
            || !Tools.isArray(dataTable?.context)
            || Tools.empty(dataTable?.context[0]?.aoColumns)
        ) {
            return;
        }
        this.dataTableId = dataTable.DATA.id;
        /** @type {{aoColumns}} */
        const context = dataTable.context[0];
        /** @type {Object[]} */
        const columns = context.aoColumns;

        /** @type {{[string]: boolean}} */
        const dataTableColumnsVisibility = {};
        /** @param {{data, mData, name, sName, bVisible}} item */
        columns.forEach(item => {
            dataTableColumnsVisibility[item.data] = item.bVisible;
        });
        this.saveColumnConfig(dataTableColumnsVisibility);
    }

    /**
     * @description Aktualizuje existujúci column objekt uloženými dátami. Ak dáta neexistujú, metóda vráti jej vstup.
     * @param {string} dataTableID
     * @param {Object[]} configArray
     * @returns {Object[]}
     * @public
     */
    updateColumnConfig(dataTableID, configArray) {
        this.dataTableId = dataTableID;

        //uloz do configArray originalnu hodnotu
        configArray.map(object => {
            //sem ulozime originalnu hodnotu visible parametra
            let visibleOriginal = true;
            if (typeof object['visible'] != "undefined" && object['visible']===false) visibleOriginal = false;
            object['visibleOriginal'] = visibleOriginal;
        });

        /** @type {Object|{[string]: boolean}|null} */
        const loadedConfig = this.loadColumnConfig();
        /** @type {Object[]} */
        let tmpConfigArray = [];
        /**
         * @type {Object[]} */
        if (Tools.empty(configArray, loadedConfig)) {
            return configArray;
        }
        try {
            // Kópia Array-u aby sme nepracovali s referenciou
            tmpConfigArray = Tools.copy(configArray);
        } catch (e) {
            return configArray;
        }
        //console.log("updateColumnConfig: configArray=", configArray, "tmpConfigArray=", tmpConfigArray, "loadedConfig=", loadedConfig);
        tmpConfigArray = tmpConfigArray.map(object => {
            //tu nastavime podla ulozenej konfiguracie
            object['visible'] = loadedConfig[object.data];
            return object;
        });

        //dopln render funkcie z originalneho configArray objektu
        for (let i=0; i<tmpConfigArray.length; i++) {
            if (typeof configArray[i].render === "function") {
                //console.log("Copying render function for ", configArray[i]);
                tmpConfigArray[i].render = configArray[i].render;
            }
        }

        //console.log("Returning object: ", tmpConfigArray, "original=", configArray);

        return tmpConfigArray;
    }

    /**
     * @description Vygeruje unikátny reťazec, zložený z URL akutálne zobrazenej stránky, emailu prihláseného používateľa, ID užívateľa a ID tabuľky.
     *              Tento reťazec je následne ecryptovaný do formátu Base64.
     *              Reťazec sa vygeneruje vždy rovnaký ale je unikátny pre aktuálnu stránku, data tabuľku a zároveň aj pre používateľa.
     * @returns {string} Encoded Base64 format
     * @private
     */
    generateConfigStoreKey() {
        /** @type {{firstName:string, lastName:string, fullName:string, email:string, photo:string, userId:number, login:string}} */
        const user = window?.currentUser;
        /** @type {string} */
        const url = window.location.origin + window.location.pathname;
        /** @type {string} */
        const email = user?.email;
        /** @type {number} */
        const userId = user?.userId;
        /** @type {string} */
        const data = url + this.dataTableId + email + userId;
        return btoa(data);
    }

    /**
     * @description Získa dáta z localStorage
     * @returns {Object|{[string]: boolean}|null}
     * @private
     */
    loadColumnConfig() {
        /** @type {string} */
        const key = this.generateConfigStoreKey();
        return this.store.getStorageItem(key);
    }

    /**
     * @description Uloží dáta do localStorage
     * @param {Object|{[string]: boolean}} configDataObject
     * @returns {void}
     * @private
     */
    saveColumnConfig(configDataObject) {
        /** @type {string} */
        const key = this.generateConfigStoreKey();
        this.store.setStorageItem(key, configDataObject);
    }

    /**
     * @description zmaze dáta z localStorage
     * @param {Object} dataTable
     * @public
     */
    removeColumnConfig(dataTable) {
        //console.log("Remove column config");
        this.dataTableId = dataTable.DATA.id;
        /** @type {string} */
        const key = this.generateConfigStoreKey();
        this.store.removeStorageItem(key);

        //console.log("columns=", dataTable.DATA.columns);

        //prenastav visibility podla originalu
        dataTable.DATA.columns.map(column => {
            //console.log("Testing column: ", column);
            if (typeof column.visibleOriginal != "undefined") {
                //console.log("Setting visibility: column=", column);
                let columnObject = dataTable.column(column.data+":name");
                if (columnObject.visible() != column.visibleOriginal) columnObject.visible(column.visibleOriginal);
            }
        });
    }

}