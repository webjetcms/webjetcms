import {StorageHandler} from '../storage-handler/storage-handler';
import {Tools} from "../tools/tools";

/**
 *
 */
export class Translator {

    /**
     * @description Vráti aktuálne používaný jazyk.
     * @returns {string}
     * @public
     * @getter
     */
    get language() {
        /** @var {string} window.userLng */
        if (!Tools.empty(window.userLng, window.userLng?.replaceAll(' ', ''))) {
            return window.userLng;
        }
        return 'sk';
    }

    /**
     * @description Vráti uložený timestamp poslednej aktualizácie. Ak neexistuje, tak vráti aktuálny.
     * @returns {number}
     * @public
     * @getter
     */
    get date() {
        /** @type {number} */
        const lastUpdate = parseInt(this.storage.getStorageItem('lastmodified'));
        if (isNaN(lastUpdate)) {
            return +new Date;
        }
        return lastUpdate;
    }

    /**
     * @description Zadanie novej API adresy pre načítanie dostupných prekladov.
     * @param {string} value
     * @public
     * @setter
     */
    set urlLoad(value) {
        this.loadUrl = value;
    }

    /**
     * @description Zadanie novej API adresy pre aktualizáciu dostupných prekladov.
     * @param {string} value
     * @public
     * @setter
     */
    set urlUpdate(value) {
        this.updateUrl = value;
    }

    constructor() {
        /** @type {StorageHandler} */
        this.storage = (new StorageHandler()).setStoreName('translate');

        /** @type {string} */
        this.loadUrl = `/admin/rest/properties/${this.language}/`;

        /**
         * @description Očakáva GET parameter 'since' s hodnotou 'timestamp'
         * @example ?since=123456789
         * @type {string}
         */
        this.updateUrl = `/admin/rest/properties/${this.language}/`;

        /** @type {string} */
        this.htmlTranslationIdentifier = 'translator';

        /** @type {Object} */
        this.onLoadCallbackList = {};

        /** @type {string} */
        this.languageKeyName = 'currentLanguage';
    }

    /**
     * @description Vyhľadá a vráti text v aktuálne používanom jazyku na základe translate kľúča.
     * @param {string} translationFieldName
     * @returns {string}
     * @public
     */
    translate(translationFieldName) {
        /** @type {string|null} */
        let translation = this.storage.getStorageItem(translationFieldName);
        if (Tools.empty(translation, translation?.replaceAll(' ', ''))) {
            Tools.log('warn', `Translation key "${translationFieldName}" has wrong format or key does not exist.`);
            translation = translationFieldName;
        }
        return translation;
    }

    /**
     * @description Nájde všetky elementy na aktuálnej stránke s atribútom pre preklad textu v elemente
     *              a nahradí aktuálny text prekladom.
     * @notice Metódu je pre správne fungovanie zavolať až po úplnom načítaní stránky - document.onLoad
     * @example ``<span data-translator="components.datatables.data.insertDate">Dátum vloženia</span>``
     * @param {Document|HTMLElement|Element} [scope]
     * @returns {Translator}
     * @public
     */
    htmlTranslate(scope = document) {
        /** @type {string} */
        const dataAttribute = `data-${this.htmlTranslationIdentifier}`;
        /** @type {string} */
        const selector = `[${dataAttribute}]`;
        /** @type {NodeListOf<HTMLElementTagNameMap[string]> | NodeListOf<Element> | NodeListOf<SVGElementTagNameMap[string]>} */
        const translationElementsList = scope.querySelectorAll(selector);
        for (let i = 0; i < translationElementsList.length; i++) {
            /** @type {HTMLElementTagNameMap|Element|SVGElementTagNameMap} */
            const dataElement = translationElementsList[i];
            /** @type {string} */
            const dataValue = dataElement.getAttribute(dataAttribute);
            dataElement.innerHTML = this.translate(dataValue);
        }
        return this;
    }

    /**
     * @description Pridanie akcie, ktorá sa vykoná pred načítaním dát zo servera.
     * @param {function} callback
     * @param {boolean} [rewrite] Vypne ukladanie callbackov do zoznamu a bude sa vždy spúšťať iba jeden (naposledy pridaný) callback.
     * @returns {Translator}
     * @public
     */
    onBeforeLoad(callback, rewrite = false) {
        return this.onLoadCollector('onBeforeLoad', callback, rewrite);
    }

    /**
     * @description Pridanie akcie, ktorá sa vykoná po úspešnom načítaní dát zo servera.
     * @param {function} callback
     * @param {boolean} [rewrite] Vypne ukladanie callbackov do zoznamu a bude sa vždy spúšťať iba jeden (naposledy pridaný) callback.
     * @returns {Translator}
     * @public
     */
    onAfterLoad(callback, rewrite = false) {
        return this.onLoadCollector('onAfterLoad', callback, rewrite);
    }

    /**
     * @description Spustí načítanie prekladov
     * @returns {void}
     * @public
     */
    load() {
        this.loadTranslations(() => {
            this.updateTranslations();
        });
    }

    /**
     * @description Načíta nové preklady
     * @param {function} [storeExistCallback]
     * @returns {Translator}
     * @public
     */
    loadTranslations(storeExistCallback) {
        if (this.storage.storeExist() && this.storage.getStorageItem('currentLanguage') === this.language) {
            Tools.exec(storeExistCallback, this);
            return this;
        }
        /** @var {Object} data */
        this.sendAndLoad(this.loadUrl, data => {
            if (Object.keys(data).length) {
                this.createTranslationList(data)
                Tools.log('info', 'Translation list was created successfully.');
            } else {
                Tools.log('warn', 'Translation list was not created.', 'No DATA!');
            }
        });
        return this;
    }

    /**
     * @description Načíta aktualizácie prekladov
     * @returns {Translator}
     * @public
     */
    updateTranslations() {
        /** @type {number} */
        const timeStamp = this.date;
        /** @type {boolean} */
        const update = !Tools.empty(window?.propertiesLastModified)
            && Tools.isNumeric(window.propertiesLastModified)
            && window.propertiesLastModified > timeStamp;

        if (update === true) {
            /** @var {Object} data */
            this.sendAndLoad(this.updateUrl, data => {
                if (Object.keys(data).length) {
                    this.updateTranslationList(data)
                    Tools.log('info', 'Translation list was updated successfully.');
                } else {
                    Tools.log('info', 'No updates for Translation list.');
                }
            }, {since: timeStamp});
        } else {
            this.onLoadFire('onAfterLoad');
        }
        return this;
    }

    /**
     * @description Aktualizuje Dáta v Storage
     * @param {Object} data
     * @returns {void}
     * @private
     */
    updateTranslationList(data) {
        data[this.languageKeyName] = this.language;
        this.storage.setData(data);
    }

    /**
     * @description Pridá Dáta do Storage
     * @param {Object} data
     * @returns {void}
     * @private
     */
    createTranslationList(data) {
        data[this.languageKeyName] = this.language;
        this.storage.setData(data);
    }

    /**
     * @description
     * @param {string} url
     * @param {function|null} [callback]
     * @param {Object} [data]
     * @returns {void}
     * @private
     */
    sendAndLoad(url, callback = null, data = {}) {
        if (Tools.empty($, $?.ajax)) {
            Tools.log('error', 'sendAndLoad()', 'jQuery "$" or jQuery Ajax "$.ajax()" not found.');
            return;
        }
        this.onLoadFire('onBeforeLoad');
        $.ajax({
            url: url,
            method: 'get',
            contentType: 'application/json',
            data,
            success: responseData => {
                Tools.exec(callback, responseData);
                this.onLoadFire('onAfterLoad');
            }
        });
    }

    /**
     * @description
     * @param {string} callbackType
     * @returns {void}
     * @private
     */
    onLoadFire(callbackType) {
        if (this.onLoadCallbackList.hasOwnProperty(callbackType)
            && this.onLoadCallbackList[callbackType].length) {
            this.onLoadCallbackList[callbackType].map(callbackItem => {
                if (typeof callbackItem === 'function') {
                    Tools.exec(callbackItem, this, this.storage);
                }
            });
        }
    }

    /**
     * @description
     * @param {string} callbackType
     * @param {function} callback
     * @param {boolean} [rewrite]
     * @returns {Translator}
     * @private
     */
    onLoadCollector(callbackType, callback, rewrite = false) {
        if (typeof callback === 'function') {
            if (!this.onLoadCallbackList.hasOwnProperty(callbackType)
                || (rewrite && this.onLoadCallbackList[callbackType].length)) {
                this.onLoadCallbackList[callbackType] = [];
            }
            this.onLoadCallbackList[callbackType].push(callback);
        }
        return this;
    }
}