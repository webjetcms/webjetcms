/**
 *
 */
export default class Store {

    /**
     * @description
     * @returns {Storage}
     * @public
     * @readonly
     */
    get storage() {
        return this._storage;
    }

    constructor() {
        /**
         * @type {Storage}
         * @private
         */
        this._storage = window.localStorage;
    }

    /**
     * @description Nastaví typ Storage na sessionStorage.
     * @returns {Store}
     * @public
     */
    setSessionStorage() {
        this._storage = window.sessionStorage;
        return this;
    }

    /**
     * @description Nastaví typ Storage na localStorage.
     * @returns {Store}
     * @public
     */
    setLocalStorage() {
        this._storage = window.localStorage;
        return this;
    }

    /**
     * @description
     * @param {string} key
     * @returns {null|any}
     * @public
     */
    getItem(key) {
        /** @type {null|*} */
        const value = this.storage.getItem(key);
        try {
            return JSON.parse(value);
        } catch (e) {
            return null;
        }
    }

    /**
     * @description
     * @param {string} key
     * @param {any|*} value
     * @returns {void}
     * @public
     */
    setItem(key, value = null) {
        try {
            value = JSON.stringify(value)
        } catch (e) {
            value = 'null';
        }
        this.storage.setItem(key, value);
    }

    /**
     * @description
     * @param {string} key
     * @returns {void}
     * @public
     */
    removeItem(key) {
        this.storage.removeItem(key);
    }

    /**
     * @description
     * @returns {void}
     * @public
     */
    clear() {
        this.storage.clear();
    }

    /**
     * @description
     * @param {number} indexNumber
     * @returns {string}
     * @public
     */
    key(indexNumber) {
        return this.storage.key(indexNumber);
    }

    /**
     * @description Vráti zoznam (array), všetkých kľúčov v aktuálnom Storage.
     * @returns {string[]}
     * @public
     */
    getKeys() {
        return Object.keys(this.storage);
    }

    /**
     * @description Overí, či v Storage existuje zadaný kľúč.
     * @param {string} key
     * @returns {boolean}
     * @public
     */
    itemExist(key) {
        return !!this.storage.getItem(key);
    }
}