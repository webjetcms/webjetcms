# `Tools`

Balíček statických pomocných funkcí.

`Verzia 1.1.0`, [Changelog](#changelog)

### Popis fungování:

Balíček slouží jako malá knihovna, která obsahuje statické funkce ošetřující různé situace v kódu a jejich použití je na programátorovi a konkrétní situaci.

## Použití:

```javascript
import {Tools} from 'tools/tools';
```

!>**Upozornění:** Balíček není implementován v systému globálně, proto je třeba jej přidat všude, kde se používá.

Volání funkcí se zajišťuje kdekoli ve **WebJET** Javascript souborech nebo `<script>` html značkách za pomoci volání:

```javascript
Tools.funkcia(parametre);
```

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

| [System](#systém)                   | [String](#string)                       | [DŮM](#dům)                                     | [Test](#test)                     | [Other](#other)     |
| ----------------------------------- | --------------------------------------- | ----------------------------------------------- | --------------------------------- | ------------------- |
| [isDevMode()](#isdevmode)           | [removeDiacritics()](#removediacritics) | [getCssValue()](#getcssvalue)                   | [isNumeric()](#isnumeric)         | [uuidv4()](#uuidv4) |
| [log()](#log)                       | [declension()](#declension)             | [getNextHighestZIndex()](#getnexthighestzindex) | [empty()](#empty)                 | [copy()](#copy)     |
| [getUrlQuery()](#geturlquery)       | [multiReplace()](#multireplace)         |                                                 | [isArray()](#isarray)             |
| [updateUrlQuery()](#updateurlquery) |                                         |                                                 | [isObject()](#isobject)           |
| [exec()](#exec)                     |                                         |                                                 | [isNull()](#isnull)               |
|                                     |                                         |                                                 | [isJQuery()](#isjquery)           |
|                                     |                                         |                                                 | [isString()](#isstring)           |
|                                     |                                         |                                                 | [isNumber()](#isnumber)           |
|                                     |                                         |                                                 | [isBoolean()](#isboolean)         |
|                                     |                                         |                                                 | [isFunction()](#isfunction)       |
|                                     |                                         |                                                 | [isHTMLElement()](#ishtmlelement) |

 | | | | [elementExist()](#elementexist)   |

***

### Systém

#### `isDevMode()`

Ověří, zda se nacházíme v developerském prostředí. Pokud ano, funkce vrátí **TRUE** pokud ne tak vrátí **FALSE**. Hodnota je zároveň zpřístupněna i ve `window` pod klíčem `isDev` (`window.isDev`).

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

Vypisování logů v DEV prostředí. Alternativa ke `console.log` ale nezobrazuje se na produkci.

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

Vyparsuje z adresy GET query za otazníkem a vrátí objekt s hodnotami `{kľúč : hodnota}`. Ošetřuje také query umístěnou za #hash atributem.

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

Aktualizuje url search query vstupními hodnotami. Pokud se v poli `excludeKeys` nacházejí názvy klíčů, tyto nebudou v aktualizaci přidány. Nastavení prázdné hodnoty pomocí `queryValue` způsobí odstranění parametru z URL adresy.

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

Ošetřuje spuštění callbacku, aby se zabránilo fatal erroru pokud by nebyl callback definován správně.

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

Nahradí ze vstupního textu všechny znaky s diakritikou za znaky anglické abecedy. Zároveň máme k dispozici volitelný argument vložení znaku nebo řetězce, kterým budou ve vstupním textu nahrazeny všechny mezery.

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

Skloňování slov na základě celého čísla. Funguje i při zadání záporného čísla.

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

Umožní nahradit více různých částí vstupního řetězce za definované hodnoty.

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

### DŮM

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

Pokud potřebujeme pomocí Javascriptu nastavit nějakému elementu úplně nejvyšší dostupný `z-index`, tak si tento nový nejvyšší můžeme zjistit zavoláním dané funkce.

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

Otestuje zadanou vstupní hodnotu, zda je prázdná. Je možné vložit i více vstupních hodnot současně oddělených čárkou a je-li alespoň jedna prázdná, tak metoda vrátí TRUE.

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

Ověří, zda je vstupní hodnota numerická. Ať se jedná o číslo, ať už se jedná o typ number nebo string.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota typu Array.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota typu Object.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota null.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota object jQuery.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota string.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota number.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota boolean.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota function.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Otestuje, zda je vstupní hodnota HTMLElement.

Je možné vložit i více vstupních hodnot současně oddělených čárkou.

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

Zkontroluje, zda existuje element na základě jeho css selektoru. Pokud neexistuje, tak metoda poslouchá a v případě jeho vytvoření zavolá `existCallback`. Na element se čeká maximálně 25 sekund. Potom se poslech přeruší a zavolá se `notExistCallback`

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

Vygeneruje náhodný UUID-4 (Universally Unique Identifier version 4) řetězec podle specifikace RFC 4122.

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

Vytvoří kopii vstupu. Zabrání tak objektovým referencím.

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

Verze 1.1.0

- [x] Upravená funkcionalita is\* metod
- [x] Přidány nové metody

Verze 1.0.0

- [x] Přidat verzi
- [x] Opravit gramatiku

***

**[Toto dopis]**

Verze 2.0.0

- [ ] Oddělit kategorie do samostatných tříd
