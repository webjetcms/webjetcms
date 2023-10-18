export class Tools {

    /**
     * @description Overí, či sa nachádzame v developerskom prostredí.
     * @returns {boolean}
     * @public
     * @static
     */
    static isDevMode() {
        /** @type {string} */
        const id = 'isDev';
        if (!this.empty(window[id])) {
            return window[id] === true;
        }
        /**
         * @description Zoznam povolených developerských subdomén
         * @type {string[]}
         */
        const devSub = ['iwcm', 'cms'];
        /**
         * @description Zoznam povolených developerských domén
         * @type {string[]}
         */
        const devDom = ['interway', 'iway'];

        /** @type {string[]} */
        const domains = window.location.hostname.split('.');
        /** @type {boolean} */
        const isDev = (domains.length >= 3 && devSub.indexOf(domains[0]) >= 0 && devDom.indexOf(domains[1]) >= 0);
        if (isDev) {
            // Tu sa nesmie použiť metóda this.log() pretože by to mohlo vyvolať nechcenú rekurziu a zamrzol by browser!
            console.log('%c WebJET Developer Mode. For using call `window.' + id + '` ', 'background:#bada55;color:#f00;font-size:1rem');
        }
        window[id] = isDev;
        return isDev;
    }

    /**
     * @description Ak sme v developerskom prostredí, tak môžeme vypisovať logy
     * @param {string} type log|error|warn|info...
     * @param {...any} [data]
     * @returns {void}
     * @public
     * @static
     */
    static log(type, ...data) {
        if (this.isDevMode() === true) {
            /** @type {string} */
            const prefix = `[dev-log: ${type}]`;
            /** @type {string} */
            const color = '%c';
            /** @type {string} */
            const style = 'background:#000;color:#ff0';
            /** @var {Console} */
            const c = console;
            (this.empty(c[type]) || !c.hasOwnProperty(type)) ? c.log(color + prefix, style, ...data) : c[type](color + prefix, style, ...data);
        }
    }

    /**
     * @description Nahradí zo vstupného textu všetky znaky s diakritikou za znaky anglickej abecedy.
     * @param {string} text
     * @param {string} [replaceSpaceWithChar=''] Zamení medzery vstupnom texte za definovaný znak.
     * @returns {string}
     * @public
     * @static
     */
    static removeDiacritics(text, replaceSpaceWithChar = '') {
        /** @type {string} */
        let outputText = text.normalize('NFD').replace(/[\u0300-\u036f]/gi, '');
        if (!this.empty(replaceSpaceWithChar)) {
            outputText = outputText.replaceAll(' ', replaceSpaceWithChar);
        }
        return outputText;
    }

    /**
     * @description Vráti nám hodnotu css vlastnosti na danom elemente.
     *              Vráti aj computed css vlastnosti aj vlastnosti nastavené pomocou CSS a rovnako vráti aj inline CSS
     * @param {Element|HTMLElement} element
     * @param {string} propertyName
     * @example Tools.getCssValue(element, 'z-index')
     * @returns {string}
     * @public
     * @static
     */
    static getCssValue(element, propertyName) {
        if (!element || !propertyName) {
            return '';
        }
        /** @type {string} */
        let value = '';
        if (window.getComputedStyle) {
            value = document.defaultView.getComputedStyle(element, null).getPropertyValue(propertyName);
            /** @var {{currentStyle}} element */
        } else if (element.currentStyle) {
            propertyName = propertyName.toLowerCase().replace(/-([a-z])/gi, (s, group) => group.toUpperCase());
            value = element.currentStyle[propertyName];
        }
        return value;
    }

    /**
     * @description Vráti hodnotu najvyššieho `z-index` v danom kontexte ku ktorej pripočíta `+1`,
     *              a tak nám vznikne najvyššia ešte nepoužitá hodnota `z-index`, ktorú môžeme použiť.
     * @param {Document|Element|HTMLElement} [context=document] DOM scope, v ktorom sa má vyhľadávať.
     * @returns {number}
     * @public
     * @static
     */
    static getNextHighestZIndex(context = document) {
        /** @var {Array} elements */
        const elements = context.getElementsByTagName('*');
        /** @var {number} highest */
        let highest = 0;
        for (let i = 0; i < elements.length; i++) {
            /** @type {Element|HTMLElement} */
            const el = elements[i];
            /** @type {string} */
            let index = this.getCssValue(el, 'z-index');
            /** @type {number} */
            let currentIndex = parseInt(index, 10);
            currentIndex = isNaN(currentIndex) ? 0 : currentIndex;
            if (currentIndex > highest) {
                highest = currentIndex;
            }
        }
        return highest + 1;
    }

    /**
     * @description Vyparsuje z adresy GET query za otaznikom a vráti objekt s hodnotami {kľúč : hodnota}
     *              Ošetruje aj query umiestnenú za #hash atribútom.
     * @param {boolean} [fullDecode=false] Ak je TRUE dekóduje úplne všetky znaky.
     * @returns {Object}
     * @public
     * @static
     */
    static getUrlQuery(fullDecode = false) {
        /** @type {string} */
        let search = window.location.search.replaceAll(/^\?+/g, '');
        if (Tools.empty(search)) {
            /** @type {string} */
            const hash = window.location.hash.replaceAll(/^#+/g, '');
            /** @type {number} */
            const index = hash.indexOf('?');
            if (index >= 0) {
                search = hash.substring(index + 1);
            } else {
                return {};
            }
        }
        try {
            /** @type {{}} */
            const replaceObj = {'"': '\\"', '&': '","', '=': '":"'};
            /** @type {string} */
            const searchParser = this.multiReplace(decodeURI(search), replaceObj);
            let queryObject = `{"${searchParser}"}`;
            if (fullDecode === true) {
                queryObject = decodeURIComponent(queryObject);
            }
            return JSON.parse(queryObject);
        } catch (e) {
            return {};
        }
    }

    /**
     * @description Aktualizuje url search query vstupnými hodnotami.
     *              Ak sa v poli `excludeKeys` nachádzajú názvy kľúčov, tieto nebudú v aktualizácii pridané.
     * @param {string} queryKey
     * @param {string} queryValue
     * @param {string[]} [excludeKeys]
     * @returns {void}
     * @public
     * @static
     */
    static updateUrlQuery(queryKey, queryValue, excludeKeys = []) {
        if (this.empty(window?.history?.pushState, queryKey) || !this.isString(queryKey)) {
            if (this.empty(window?.history?.pushState)) {
                this.log('warn', '[updateUrlQuery]', 'window.history.pushState', 'Not found.');
            } else {
                this.log('warn', '[updateUrlQuery]', 'Invalid input value:', queryKey, queryValue);
            }
            return;
        }

        /** @type {string} */
        const searchString = window.location.search.replaceAll(/^\?+/g, '');

        /** @type {null|*} */
        let excludeCopy = this.copy(excludeKeys);
        if (!this.empty(excludeCopy)) {
            if (queryValue!=null && queryValue!="") {
                /**
                 * @description Zistíme, či sa aktuálny kľúč nachádza v excluded zozname.
                 *              Ak áno, tak ho z tohto zoznamu odstránime, aby nám ho neignorovalo pri update.
                 * @type {number}
                 */
                const excludeIndex = excludeCopy.indexOf(queryKey);
                if (excludeIndex > -1) {
                    excludeCopy.splice(excludeIndex, 1);
                }
            }
        } else {
            excludeCopy = [];
        }

        /** @type {{}} */
        let searchObject = {};
        try {
            /** @type {{}} */
            const replaceObj = {'"': '\\"', '&': '","', '=': '":"'};
            /** @type {string} */
            const searchParser = this.multiReplace(decodeURI(searchString), replaceObj);
            searchObject = JSON.parse(`{"${searchParser}"}`);
        } catch (e) {
        }

        if (queryValue!=null && queryValue!="") searchObject[queryKey] = queryValue;
        /** @type {string} */
        let updatedQuery = '';
        for (const key in searchObject) {
            if (searchObject.hasOwnProperty(key) && excludeCopy.indexOf(key) < 0) {
                updatedQuery += key + '=' + searchObject[key] + '&';
            }
        }
        updatedQuery = updatedQuery.replaceAll(/^&+|&+$/g, '').trim();
        updatedQuery = (!this.empty(updatedQuery) ? '?' : '') + updatedQuery;

        /** @type {string} */
        let url = window.location.protocol + '//' + window.location.host + window.location.pathname;
        /** @type {string} */
        let updatedUrl = url + updatedQuery + window.location.hash;

        window.history.pushState({path: updatedUrl}, '', updatedUrl);
    }

    /**
     * @description Bezpečne spustí callbak aj s argumentami a predíte, tak spadnutiu aplikácie.
     *              Ošetruje spustenie callbacku, aby sa zabránilo fatal erroru ak by nebol callback definovaný správne.
     * @param {Function|{callback: Function, exceptionCallback: Function}} callback
     * @param {...any} [args]
     * @returns {void}
     * @public
     * @static
     */
    static exec(callback, ...args) {
        try {
            if (typeof callback === 'function') {
                callback(...args);
            } else if (
                this.isObject(callback)
                && callback.hasOwnProperty('callback')
                && typeof callback.callback === 'function'
            ) {
                callback.callback(...args);
            }
        } catch (e) {
            if (
                this.isObject(callback)
                && callback.hasOwnProperty('exceptionCallback')
                && typeof callback.exceptionCallback === 'function'
            ) {
                callback.exceptionCallback(e, ...args);
            } else {
                console.error(e);
            }
        }
    }

    /**
     * @description Overí, či je vstupná hodnota prázdna.
     *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou a ak je aspoň jedna prázdna, tak metóda vráti TRUE
     * @example
     *          Testovacie scenáre:
     *          ---------------
     *          []               true, empty array
     *          {}               true, empty object
     *          null             true
     *          undefined        true
     *          ""               true, empty string
     *          ''               true, empty string
     *          ``               true, empty string
     *          undefined + 1    true, NaN
     *
     *          $('body')        false, jQuery
     *          0                false, number
     *          true             false, boolean
     *          false            false, boolean
     *          Date             false, function
     *          function         false, function
     *          ---------------
     *
     *          Dôležité!!!!
     *          Metóda nezabráni vykonávaniu funkcií vo vstupných argumetoch.
     *          V prípade použitia funkcií je potrebné použiť chain operátor `?.`
     *          ---------------
     *          const premenna = null;
     *
     *          // Nesprávne
     *          Tools.empty(premenna, premenna.replace(' ', '')); // Fatal Error
     *
     *          // Správne
     *          Tools.empty(premenna, premenna?.replace(' ', '')); // OK
     *          ---------------
     *
     *          Použitie:
     *          ---------------
     *          // Vráti TRUE, pretože je empty array
     *          Tools.empty([]);
     *
     *          // Vráti FALSE, pretože nie je empty string
     *          Tools.empty('Hello');
     *
     *          // Vráti TRUE, pretože jeden z argumentov je empty
     *          Tools.empty(1, '', {'a': 1});
     *          ---------------
     *
     * @param {...any} value Jeden alebo viac vstupných argumentov.
     * @returns {boolean}
     * @public
     * @static
     */
    static empty(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (
                        // undefined
                        typeof testValue === 'undefined'
                        // empty string
                        || testValue === ''
                        // null
                        || this.isNull(testValue)
                        // empty array
                        || this.isArray(testValue) && testValue.length <= 0
                        // empty object
                        || this.isObject(testValue) && Object.keys(testValue).length <= 0
                        // NaN
                        || !this.isFunction(testValue)
                        && !this.isString(testValue)
                        && !this.isJQuery(testValue)
                        && !this.isObject(testValue)
                        && !this.isArray(testValue)
                        && !this.isHTMLElement(testValue)
                        && isNaN(testValue)
                    ) {
                        return true;
                    }
                }
                return false;
            }
        } catch (e) {
        }
        return true;
    }

    /**
     * @description Otestuje, či je vstupná hodnota numerická. Či sa jedná o validné číslo vo formáte typu Number alebo String.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isNumeric(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (!isNaN(+testValue)) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota Array.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isArray(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (Object.prototype.toString.call(testValue).toLowerCase().indexOf('array') >= 0) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota Object.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isObject(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (Object.prototype.toString.call(testValue).toLowerCase().indexOf('object object') >= 0) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota null.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isNull(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (Object.prototype.toString.call(testValue).toLowerCase().indexOf('null') >= 0) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota object jQuery.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isJQuery(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (typeof jQuery !== 'undefined' && this.isObject(testValue) && value instanceof jQuery) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota string.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isString(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (typeof testValue === 'string') {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota number.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isNumber(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (typeof testValue === 'number') {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota number.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isBoolean(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (Object.prototype.toString.call(testValue).toLowerCase().indexOf('boolean') >= 0) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota function.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isFunction(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    if (Object.prototype.toString.call(testValue).toLowerCase().indexOf('function') >= 0) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Otestuje, či je vstupná hodnota HTMLElement.
     *              Je možné vložiť aj viac vstupných hodnôt súčastne oddlených čiarkou
     * @param {*} value
     * @returns {boolean}
     * @public
     * @static
     */
    static isHTMLElement(value) {
        try {
            if (arguments.length) {
                for (const testValue of arguments) {
                    /** @type {string} */
                    const test = Object.prototype.toString.call(testValue).toLowerCase();
                    if (test.indexOf('node') >= 0 || test.indexOf('element') >= 0) {
                        continue;
                    }
                    return false
                }
                return true;
            }
        } catch (e) {
        }
        return false;
    }

    /**
     * @description Vygeneruje UUID-4 (Universally Unique Identifier version 4) reťazec podľa špecifikácie RFC 4122.
     * @see https://tools.ietf.org/html/rfc4122
     * @returns {string}
     * @public
     * @static
     */
    static uuidv4() {
        /**
         * @todo crypto nie je možné vždy použiť, pretože vyžaduje Secure context a tiež ho nepodporujú niektoré prehliadače.
         * @see https://developer.mozilla.org/en-US/docs/Web/API/Crypto
         */
        if (!this.empty(window.crypto, window.crypto.getRandomValues, window.Uint8Array)) {
            return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
                (c ^ window.crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
            );
        }
        // Inak použijeme old-school riešenie
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            /** @type {number} */
            const r = Math.random() * 16 | 0;
            /** @type {number} */
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    /**
     * @description Skloňovanie slov na základe celého čísla. Funguje aj pri zadaní záporného čísla.
     * @example
     *          // Vstupné číslo          0 | 5+    1         2,3,4
     *          // declensionArray      ['ukážok', 'ukážka', 'ukážky']
     *
     *          const declensionArray = ['ukážok', 'ukážka', 'ukážky'];
     *
     *          let numberValue = 4;
     *          let str = numberValue + ' ' + Tools.declension(numberValue, declensionArray);
     *          console.log(str); // output: 4 ukážky
     *
     *          numberValue = 12;
     *          str = numberValue + ' ' + Tools.declension(numberValue, declensionArray);
     *          console.log(str); // output: 12 ukážok
     *
     *          numberValue = -2;
     *          str = numberValue + ' ' + Tools.declension(numberValue, declensionArray);
     *          console.log(str); // output: -2 ukážky
     *
     * @param {number|string} numberValue Číselná hodnota
     * @param {string[]} declensionArray Pole skloňovaných slov ``{string[(0|5+), (1), (2-4)]}``
     * @returns {string}
     * @public
     * @static
     */
    static declension(numberValue, declensionArray) {
        numberValue = isNaN(+numberValue) ? 0 : numberValue;
        if (this.empty(declensionArray) || !this.isArray(declensionArray)) {
            return numberValue.toString();
        }
        numberValue = Math.abs(numberValue);
        if (numberValue === 1) {
            return declensionArray[1];
        } else if (numberValue > 1 && numberValue < 5) {
            return declensionArray[2];
        }
        return declensionArray[0];
    }

    /**
     * @description Skontroluje, či existuje element na základe jeho css selektora.
     *              Ak neexistuje, tak metóda počúva a v prípade jeho vytvorenia zavolá `existCallback`.
     *              Na element sa čaká maximálne 25 sekúnd. Potom sa počúvanie preruší a zavolá sa `notExistCallback`.
     *
     * @async Metóda beží asynchrónne, takže nebrzdí render ani vykonávanie iných skriptov.
     * @param {string} selector
     * @param {Function} existCallback Optional result arguments [element, selector, [Tools]]
     * @param {Function} [notExistCallback] Optional result arguments [selector, [Tools]]
     * @param {{context?: Document|HTMLElement|Element, checkInterval?: number, maxChecks?: number}} [options]
     * @returns {void}
     * @public
     * @static
     */
    static elementExist(selector, existCallback, notExistCallback = null, options) {
        /** @type {null|number} */
        let timer = null;
        /** @type {number} */
        let maxChecks = options?.maxChecks || 100;
        /** @type {number} */
        const checkInterval = options?.checkInterval || 250;

        /**
         * @returns {void}
         */
        const abort = () => {
            clearTimeout(timer);
        };

        /**
         * @returns {void}
         */
        const reset = () => {
            abort();
            timer = null;
            maxChecks = options?.maxChecks || 100;
        };

        /**
         * @description Ak by prišiel nevalidný selector (číslo), pri ktorom padá querySelector.
         * @param {string} selector
         * @param {document|HTMLElement|Element|any} [context]
         * @returns {Element|HTMLElement|any}
         */
        const getElement = (selector, context = document) => {
            /** @type {string} */
            selector = selector.trim();
            try {
                return context.querySelector(selector);
            } catch (e) {
                /** @type {number} */
                const hashIndex = selector.indexOf('#');
                if (hashIndex === 0 || selector.indexOf('.') === 0) {
                    /** @type {string} */
                    const currSelector = selector.substring(1);
                    return hashIndex === 0 ? context.getElementById(currSelector) : context.getElementsByClassName(currSelector)[0];
                }
                return context.getElementsByTagName(selector)[0];
            }
        };

        /**
         * @param {string} selector
         * @param {Function} existCallback
         * @param {Function} notExistCallback
         * @param {document|Element|any} context
         * @returns {void}
         */
        const check = (selector, existCallback, notExistCallback, context = document) => {
            if (this.empty(selector, selector.trim())) {
                this.exec(notExistCallback, selector);
                return;
            }
            /** @type {Element|HTMLElement|*} */
            const element = getElement(selector, context);
            if (this.empty(element)) {
                if (maxChecks > 0) {
                    maxChecks--;
                    timer = setTimeout(() => {
                        abort();
                        check(selector, existCallback, notExistCallback, context);
                    }, checkInterval);
                } else {
                    reset();
                    this.exec(notExistCallback, selector, this);
                }
            } else {
                reset();
                this.exec(existCallback, element, selector, this);
            }
        };

        check(selector, existCallback, notExistCallback, options?.context);
    }

    /**
     * @description Vytvorí kópiu vstupu. Zabráni tak objektovým referenciám.
     * @param {any} value
     * @returns {null|any}
     * @public
     * @static
     */
    static copy(value) {
        try {
            return JSON.parse(JSON.stringify(value));
        } catch (e) {
            return null;
        }
    }

    /**
     * @description Umožní nahradiť viac rôznych častí vstupného reťazca za definované hodnoty.
     * @param {string} str
     * @param {{}} mapObj {find : replaceTo}
     * @param {boolean} [ignoreCaseSensitive]
     * @example
     *
     *          const str = 'Tango and Cash';
     *          const mapObj = {'and':'&', 's':'$'};
     *
     *          Tools.multiReplace(str, mapObj); // Tango & Ca$h
     *
     * @returns {string}
     * @public
     * @static
     */
    static multiReplace(str, mapObj, ignoreCaseSensitive = false) {
        const re = new RegExp(Object.keys(mapObj).join("|"), "gi");
        return str.replace(re, matched => {
            if (ignoreCaseSensitive) {
                return mapObj[matched.toLowerCase()];
            }
            return mapObj[matched];
        });
    }
}