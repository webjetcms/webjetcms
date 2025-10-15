import {Tools} from "../tools/tools";

/**
 * @description Obsluhuje spúšťanie callbackov v zadanom poradí.
 */
export default class ReadyExtender {
    /**
     * @description Získa vygenerovaný názov úložiska
     * @returns {string}
     * @public
     * @getter
     */
    get listName() {
        return this._listName;
    }

    /**
     * @description Vráti kompletný zoznam všetkých pridaných callbackov aj s ich poradovým číslom
     * @returns {Object}
     * @public
     * @getter
     * @readonly
     */
    get list() {
        this.createEmptyStoreObject();
        if (!this._listStore.hasOwnProperty(this.listName)) {
            return null;
        }
        return this._listStore[this.listName];
    }

    constructor() {
        /** @type {{}|null} */
        this._listStore = null;

        /** @type {string} */
        this._listName = Tools.uuidv4();
    }

    /**
     * @description Pridá do zoznamu nový callback.
     * @param {function} callback
     * @param {number} [orderId] Určuje poradie callbakov v akom sa majú spúšťať. Čísluje sa od 1. Ak nie je zadané, callback sa pridá na koniec zoznamu.
     * @param {boolean} [rewriteOrder] Pri nastavení TRUE sa pri existencii callbacku na danej pozícii na silu prepíše callback a predošlý sa pridá na najbližšie voľné miesto.
     * @returns {void}
     * @public
     */
    add(callback, orderId = 0, rewriteOrder = false) {
        orderId = orderId < 1 ? 1 : orderId;
        this.createEmptyStoreObject();
        if (typeof callback === 'function') {
            if (Tools.empty(this._listStore[this.listName]) || !Tools.isObject(this._listStore[this.listName])) {
                this._listStore[this.listName] = {};
            }
            if (this.keyExist(orderId)) {
                if (rewriteOrder === true) {
                    /** @type {number} */
                    const next = this.getNextEmptyKey(orderId);
                    if (next < 1) {
                        return;
                    }
                    this._listStore[this.listName][next] = this._listStore[this.listName][orderId];
                    this._listStore[this.listName][orderId] = null;
                } else {
                    orderId = this.getNextHighestKey();
                    if (orderId < 1) {
                        return;
                    }
                }
            }
            this._listStore[this.listName][orderId] = callback;
        }
    }

    /**
     * @description Odstráni callback zo zoznamu na základe čísla jeho poradia.
     * @param {number} orderId Číslo poradia callbacku
     * @returns {void}
     * @public
     */
    remove(orderId) {
        orderId = orderId < 1 ? 1 : orderId;
        this.createEmptyStoreObject();
        if (this._listStore.hasOwnProperty(this.listName) && this._listStore[this.listName].hasOwnProperty(orderId)) {
            this._listStore[this.listName][orderId] = null;
            delete this._listStore[this.listName][orderId];
        }
    }

    /**
     * @description Spustí vykonanie všetkých callbackov v zozname, zaradom podľa poradia.
     * @returns {void}
     * @public
     */
    fire() {
        this.createEmptyStoreObject();
        if (!this._listStore.hasOwnProperty(this.listName)) {
            return;
        }
        /** @type {string[]} */
        const keys = Object.keys(this._listStore[this.listName]);
        if (Tools.empty(keys)) {
            return;
        }
        /** @type {{}} */
        const ordered = keys.sort().reduce((obj, key) => {
            obj[key] = this._listStore[this.listName][key];
            return obj;
        }, {});

        /** @type {Function} callback */
        for (const [key, callback] of Object.entries(ordered)) {
            Tools.exec(callback, key);
        }
        this._listStore[this.listName] = null;
    }

    /**
     * @description
     * @param {number} key
     * @returns {boolean}
     * @private
     */
    keyExist(key) {
        key = key < 1 ? 1 : key;
        this.createEmptyStoreObject();
        return this._listStore.hasOwnProperty(this.listName)
            && this._listStore[this.listName].hasOwnProperty(key)
            && typeof this._listStore[this.listName][key] === 'function';
    }

    /**
     * @description
     * @returns {number}
     * @private
     */
    getNextHighestKey() {
        this.createEmptyStoreObject();
        if (!this._listStore.hasOwnProperty(this.listName)) {
            return 0;
        }
        /** @type {string} */
        const lastKey = Object.keys(this._listStore[this.listName])
            .map(Number)
            .filter(key => key < 900) // filter out keys that are too high (e.g. 900+), they are reserved for special purposes/to be last in order
            .sort((a, b) => a - b)
            .pop();
        return parseInt(lastKey, 10) + 1;
    }

    /**
     * @description
     * @param {number} key
     * @returns {number}
     * @private
     */
    getNextEmptyKey(key) {
        key = key < 1 ? 1 : key;
        this.createEmptyStoreObject();
        if (!this._listStore.hasOwnProperty(this.listName)) {
            return 0;
        }
        while (this._listStore[this.listName].hasOwnProperty(key)) {
            key++;
        }
        return key;
    }

    /**
     * @description
     * @returns {void}
     * @private
     */
    createEmptyStoreObject() {
        if (Tools.empty(this._listStore) || !Tools.isObject(this._listStore)) {
            this._listStore = {};
        }
    }
}
