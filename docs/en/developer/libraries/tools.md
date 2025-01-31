# `Tools`

Static auxiliary functions package.

`Verzia 1.1.0`, [Changelog](#changelog)

### Description of operation:

The package serves as a small library that contains static functions treating different situations in the code, and their use is up to the programmer and the specific situation.

## Application:

```javascript
import {Tools} from 'tools/tools';
```

!>**Warning:** The package is not implemented globally in the system, so it needs to be added wherever it is used.

Function calls are made anywhere in the **WebJET** Javascript files or `<script>` html tags using call:

```javascript
Tools.funkcia(parametre);
```

## List of APIs

**(Click to see the detail for the function)**

| [System](#system)                   | [String](#string)                       | [DOM](#home)                                     | [Test](#test)                     | [Other](#other)     |
| ----------------------------------- | --------------------------------------- | ----------------------------------------------- | --------------------------------- | ------------------- |
| [isDevMode()](#isdevmode)           | [removeDiacritics()](#removediacritics) | [getCssValue()](#getcssvalue)                   | [isNumeric()](#isnumeric)         | [uuidv4()](#uuidv4) |
| [log()](#log)                       | [declension()](#declension)             | [getNextHighestZIndex()](#getnexthighestzindex) | [empty()](#empty)                 | [copy()](#copy)     |
| [getUrlQuery()](#geturlquery)       | [multiReplace()](#multireplace)         | | [isArray()](#isarray)             |
| [updateUrlQuery()](#updateurlquery) | | | [isObject()](#isobject)           |
| [exec()](#exec)                     | | | [isNull()](#isnull)               |
| | | | [isJQuery()](#isjquery)           |
| | | | [isString()](#isstring)           |
| | | | [isNumber()](#isnumber)           |
| | | | [isBoolean()](#isboolean)         |
| | | | [isFunction()](#isfunction)       |
| | | | [isHTMLElement()](#ishtmlelement) |
| | | | [elementExist()](#elements)   |

***

### System

#### `isDevMode()`

It will verify that we are in a development environment. If so, the function returns **TRUE** if not it will return **FALSE**. The value is also available in `window` under the key `isDev` (`window.isDev`).

```javascript
/**
 * @description Overí, či sa nachádzame v developerskom prostredí.
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isDevMode();
```

***

#### `log()`

Logging in DEV environment. Alternative to `console.log` but it does not appear on the production.

```javascript
/**
 * @description Ak sme v developerskom prostredí, tak môžeme vypisovať logy
 * @param {string} type log|error|warn|info...
 * @param {any} [data]
 * @returns {void}
 * @public
 * @static
 */
Tools.log(type, ...data);

/** @example */

Tools.log('error', 'Nejaká error hláška', 'Alebo ďalší text');
```

#### `getUrlQuery()`

Parses the GET query from the address after the question mark and returns an object with the values `{kľúč : hodnota}`. It also handles the query placed after the #hash attribute.

```javascript
/**
 * @description Vyparsuje z adresy GET query za otaznikom a vráti objekt s hodnotami {kľúč : hodnota}
                Ošetruje aj query umiestnenú za #hash atribútom.
 * @param {boolean} [fullDecode] Ak je TRUE dekóduje úplne všetky znaky.
 * @returns {Object}
 * @public
 * @static
 */
Tools.getUrlQuery(fullDecode = false);
```

***

#### `updateUrlQuery()`

Updates the url search query with input values. If the field `excludeKeys` key names are found, these will not be added in the update. Setting an empty value using `queryValue` causes the parameter to be removed from the URL.

```javascript
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
Tools.updateUrlQuery(queryKey, queryValue, excludeKeys = []);
```

***

#### `exec()`

Handles callback execution to prevent a fatal errore if the callback was not defined correctly.

```javascript
/**
 * @description Bezpečne spustí callbak aj s argumentami a predíte, tak spadnutiu aplikácie.
 * @param {Function|{callback: Function, exceptionCallback: Function}} callback
 * @param {*} [args]
 * @returns {void}
 * @public
 * @static
 */
Tools.exec(callback, ...args);
```

***

### String

#### `removeDiacritics()`

Replaces all accented characters from the input text with characters from the English alphabet. We also have an optional character or string insertion argument that will replace all spaces in the input text.

```javascript
/**
 * @description Nahradí zo vstupného textu všetky znaky s diakritikou za znaky anglickej abecedy.
 * @param {string} text
 * @param {string} [replaceSpaceWithChar] Zamení medzery v stupnom texte za definovaný znak.
 * @returns {string}
 * @public
 * @static
 */
Tools.removeDiacritics(text, replaceSpaceWithChar = '');
```

***

#### `declension()`

Spelling words based on the whole number. Works even when entering a negative number.

```javascript
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
Tools.declension(numberValue, declensionArray);
```

***

#### `multiReplace()`

Allows you to substitute multiple different parts of the input string for defined values.

```javascript
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
Tools.multiReplace(str, mapObj, ignoreCaseSensitive = false);
```

***

### DOM

#### `getCssValue()`

```javascript
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
Tools.getCssValue(element, propertyName);
```

***

#### `getNextHighestZIndex()`

If we need to use Javascript to set the highest available value for an element `z-index`, we can find out the new highest by calling the function.

```javascript
/**
 * @description Vráti hodnotu najvyššieho `z-index` v danom contexte ku ktorej pripočíta `+1`
 *              a tak nám vznikne najvyššia ešte nepoužitá hodnota `z-index`, ktorú môžeme použiť.
 * @param {Document|Element|HTMLElement} [context]
 * @returns {number}
 * @public
 * @static
 */
Tools.getNextHighestZIndex(context = document);
```

***

### Test

#### `empty()`

Tests the specified input value to see if it is empty. It is possible to enter multiple input values at the same time separated by a comma and if at least one is empty, the method will return TRUE.

```javascript
/**
 * @description Overí, či je vstupná hodnota prázdna.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou a ak je aspoň jedna prázdna, tak metóda vráti TRUE.
 *
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
Tools.empty(value);
```

***

#### `isNumeric()`

Verifies that the input value is numeric. Whether it is a number, whether it is of type number or string.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Overí, či je vstupná hodnota numerická. Či sa jedná o číslo, či už sa jedná o number alebo string.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {any} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isNumeric(value);
```

***

#### `isArray()`

Tests if the input value is of type Array.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota typu Array.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isArray(value);
```

***

#### `isObject()`

Tests if the input value is of type Object.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota typu Object.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isObject(value);
```

***

#### `isNull()`

Tests if the input value is null.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota null.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isNull(value);
```

***

#### `isJQuery()`

Tests if the input value is a jQuery object.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota object jQuery.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isJQuery(value);
```

***

#### `isString()`

Tests if the input value is a string.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota string.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isString(value);
```

***

#### `isNumber()`

Tests if the input value is a number.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota number.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isNumber(value);
```

***

#### `isBoolean()`

Tests if the input value is a boolean.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota boolean.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isBoolean(value);
```

***

#### `isFunction()`

Tests whether the input value is a function.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota function.
 *              Je možné vložiť aj viac vstupných hodnôt súčasne oddelených čiarkou
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isFunction(value);
```

***

#### `isHTMLElement()`

Tests whether the input value is an HTMLElement.

It is also possible to enter multiple input values at the same time separated by a comma.

```javascript
/**
 * @description Otestuje, či je vstupná hodnota HTMLElement.
 * @param {*} value
 * @returns {boolean}
 * @public
 * @static
 */
Tools.isHTMLElement(value);
```

***

#### `elementExist()`

Checks if an element exists based on its css selector. If it doesn't exist, the method listens and if it does, it calls `existCallback`. The maximum waiting time for an element is 25 seconds. Then the listening is interrupted and a call is made `notExistCallback`

```javascript
/**
 * @description Skontroluje, či existuje element na základe jeho css selectora.
 *              Ak neexistuje, tak metóda počúva a v prípade jeho vytvorenia zavolá `existCallback`.
 *              Na element sa čaká maximálne 25 sekúnd. Potom sa počúvanie preruší a zavolá sa `notExistCallback`.
 *
 * @async Metóda beží asynchrónne, takže nebrzdí render ani vykonávanie iných scriptov.
 * @param {string} selector
 * @param {Function} existCallback Optional result arguments [element, selector, [Tools]]
 * @param {Function} [notExistCallback] Optional result arguments [selector, [Tools]]
 * @param {{context?: Document|HTMLElement|Element, checkInterval?: number, maxChecks?: number}} [options]
 * @returns {void}
 * @public
 * @static
 */
Tools.elementExist(selector, existCallback, notExistCallback = null, options);
```

***

### Other

#### uuidv4()

Generates a random Universally Unique Identifier version 4 (UUID-4) string according to the RFC 4122 specification.

```javascript
/**
 * @description Vygeneruje náhodný UUID-4 (Universally Unique IDentifier version 4) reťazec podľa špecifikácie RFC 4122.
 * @see https://tools.ietf.org/html/rfc4122
 * @returns {string}
 * @public
 * @static
 */
Tools.uuidv4();
```

***

#### copy()

Creates a copy of the input. This prevents object references.

```javascript
/**
 * @description Vytvorí kópiu vstupu. Zabráni tak objektovým referenciám.
 * @param {any} value
 * @returns {null|any}
 * @public
 * @static
 */
Tools.copy(value);
```

***

***

***

## Changelog

Version 1.1.0

- [x] Modified functionality of is\* methods
- [x] New methods added

Version 1.0.0

- [x] Add versioning
- [x] Correct grammar

***

**[Todo list]**

Version 2.0.0

- [ ] Separate categories into separate classes
