import Store from '../store/store'
import {Tools} from "../tools/tools";

/**
 *
 */
export class StorageHandler extends Store {

    constructor() {
        super();

        /** @type {string} */
        this.storeName = '';
    }

    /**
     * @description Nastaví triede nový názov kľúča, do ktorého sa budú ukladať dáta.
     *              Dá sa použiť za behu, a tak mať iba jednu inštanciu.
     *              Metóda vracia triedu ``StorageHandler``
     * @param {string} name
     * @returns {StorageHandler}
     * @public
     */
    setStoreName(name) {
        if (!Tools.empty(name, name?.replaceAll(' ', ''))) {
            this.storeName = name;
            Tools.log('info', 'Store name was set successfully to:', name);
        } else {
            Tools.log('warn', 'Invalid store name format:', name);
        }
        return this;
    }

    /**
     * @description Vytvorí zadaný v úložisku kľúč a uloží pod ním zadanú hodnotu.
     * @param {string} key
     * @param {any|*} value
     * @returns {void}
     * @public
     */
    setStorageItem(key, value) {
        if (Tools.empty(this.storeName)) {
            this.setStoreName(key);
        }
        /** @type {Object} */
        let data = this.getData();
        if (Tools.empty(data)) {
           data = {};
        }
        data[key] = value;

        this.setData(data);
    }

    /**
     * @description Vráti hodnotu na základe názvu zadaného kľúča.
     * @param {string} key
     * @returns {any|null}
     * @public
     */
    getStorageItem(key) {
        if (Tools.empty(this.storeName)) {
            this.setStoreName(key);
        }
        /** @type {Object} */
        const data = this.getData();
        return this.getFrom(data, key);
    }

    /**
     * @description Odstráni položku na základe kľúča.
     *              Metóde môže byť vypnutá kontrola caseSensitive.
     * @param {string} key
     * @param {boolean} [caseSensitive] true = is sensitive / false = is not sensitive
     * @returns {void}
     * @public
     */
    removeStorageItem(key, caseSensitive = true) {
        if (Tools.empty(this.storeName)) {
            Tools.log('warn', 'removeStorageItem()', 'Store name is not set.');
            return;
        }
        /** @type {Object} */
        const data = this.getData();
        if (caseSensitive && this.keyExist(data, key)) {
            delete data[key];
        } else if (this.keyExist(data, key, caseSensitive)) {
            for (const keyName in data) {
                if (keyName.toLowerCase() === key.toLowerCase()) {
                    delete data[keyName];
                    break;
                }
            }
        }
        this.setData(data);
    }

    /**
     * @description Zmaže aktuálne používané úložisko a nechá ho prázdne – null. Ponechá však master kľúč.
     * @returns {void}
     * @public
     */
    clearData() {
        if (Tools.empty(this.storeName)) {
            Tools.log('warn', 'clearData()', 'Store name is not set.');
            return;
        }
        this.setItem(this.storeName, null);
    }

    /**
     * @description Úplne odstráni aktuálne používané úložisko.
     * @returns {void}
     * @public
     */
    destroy() {
        if (Tools.empty(this.storeName)) {
            Tools.log('warn', 'destroy()', 'Store name is not set.');
            return;
        }
        this.removeItem(this.storeName);
        this.storeName = '';
    }

    /**
     * @description Uloží do aktuálne zvoleného úložiska zadané dáta. Dáta netreba konvertovať.
     * @param {Object|string|number} [data]
     * @returns {void}
     * @public
     */
    setData(data = {}) {
        if (Tools.empty(this.storeName)) {
            Tools.log('warn', 'setData()', 'Store name is not set.');
            return;
        }
        this.clearData();
        this.setItem(this.storeName, data);
    }

    /**
     * @description Vráti z aktuálne zvoleného úložiska všetky dáta.
     * @returns {Object}
     * @public
     */
    getData() {
        if (Tools.empty(this.storeName)) {
            Tools.log('warn', 'getData()', 'Store name is not set.');
            return null;
        }
        return this.getItem(this.storeName);
    }

    /**
     * @description Otestuje existenciu kľúča.
     *              Metóde môže byť vypnutá kontrola caseSensitive.
     * @param {Object} data
     * @param {string} keyName
     * @param {boolean} [caseSensitive] true = is sensitive / false = is not sensitive
     * @returns {boolean}
     * @private
     */
    keyExist(data, keyName, caseSensitive = true) {
        if (Tools.empty(data) || !Tools.isObject(data)) {
            return false;
        }
        if (caseSensitive) {
            return data.hasOwnProperty(keyName);
        }
        for (const key in data) {
            if (keyName.toLowerCase() === key.toLowerCase()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @description Vyberie z prvej úrovne objektu hodnotu na základe jej kľúča.
     *              Pred tým sa overí existencia kľúča a ak neexistuje vráti null.
     * @param {Object} from
     * @param {string} key
     * @returns {any|null}
     * @private
     */
    getFrom(from, key) {
        return this.keyExist(from, key) ? from[key] : null;
    }

    /**
     * @description
     * @param {string} [storeName]
     * @returns {boolean}
     * @public
     */
    storeExist(storeName = this.storeName) {
        return this.itemExist(storeName);
    }
}