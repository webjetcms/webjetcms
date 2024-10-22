# `Tools`

Balíček statických pomocných funkcí.

`Verzia 1.1.0`, [Seznam změn](#seznam-změn)

### Popis operace:

Balíček slouží jako malá knihovna, která obsahuje statické funkce ošetřující různé situace v kódu a jejich použití záleží na programátorovi a konkrétní situaci.

## Použití:

```javascript
import {Tools} from 'tools/tools';
```

**Varování:** Balíček není v systému implementován globálně, takže je třeba jej přidat všude, kde se používá.

Volání funkcí se provádí kdekoli v **WebJET** Soubory Javascript nebo `<script>` html značky pomocí volání:

```javascript
Tools.funkcia(parametre);
```

## Seznam rozhraní API

**(Kliknutím zobrazíte detail funkce)**

| [Systém](#systém)                   | [Řetězec](#řetězec)                       | [DOM](#home)                                     | [Test](#test)                     | [Další](#další)     |
| ----------------------------------- | --------------------------------------- | ----------------------------------------------- | --------------------------------- | ------------------- |
| [isDevMode()](#isdevmode)           | [removeDiacritics()](#removeiacritics) | [getCssValue()](#getcssvalue)                   | [isNumeric()](#isnumeric)         | [uuidv4()](#uuidv4) |
| [log()](#Přihlásit-se)                       | [deklinace()](#deklinace)             | [getNextHighestZIndex()](#getnexthighestzindex) | [empty()](#prázdný)                 | [kopírovat()](#kopírovat)     |
| [getUrlQuery()](#geturlquery)       | [multiReplace()](#multireplace)         | | [isArray()](#isarray)             |
| [updateUrlQuery()](#updateurlquery) | | | [isObject()](#isobject)           |
| [exec()](#exec)                     | | | [isNull()](#isnull)               |
| | | | [isJQuery()](#isjquery)           |
| | | | [isString()](#isstring)           |
| | | | [isNumber()](#isnumber)           |
| | | | [isBoolean()](#isboolean)         |
| | | | [isFunction()](#isfunction)       |
| | | | [isHTMLElement()](#ishtmlelement) |
| | | | [elementExist()](#prvky)   |

***

### Systém

#### `isDevMode()`

Ověří, zda se nacházíme ve vývojovém prostředí. Pokud ano, funkce vrátí **TRUE** pokud ne, vrátí se **FALSE**. Hodnota je k dispozici také v `window` pod klíčem `isDev` (`window.isDev`).

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

Přihlašování v prostředí DEV. Alternativa k `console.log` ale ve výrobě se neobjevuje.

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

zpracuje dotaz GET z adresy za otazníkem a vrátí objekt s hodnotami `{kľúč : hodnota}`. Zpracovává také dotaz umístěný za atributem #hash.

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

Aktualizuje vyhledávací dotaz url se vstupními hodnotami. Pokud pole `excludeKeys` nalezeny názvy klíčů, nebudou při aktualizaci přidány. Nastavení prázdné hodnoty pomocí `queryValue` způsobí odstranění parametru z adresy URL.

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

Zpracovává provedení zpětného volání, aby se zabránilo fatální chybě, pokud zpětné volání nebylo správně definováno.

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

### Řetězec

#### `removeDiacritics()`

Nahradí všechny znaky s diakritikou ze vstupního textu znaky anglické abecedy. K dispozici máme také volitelný argument pro vložení znaku nebo řetězce, který nahradí všechny mezery ve vstupním textu.

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

Hláskování slov na základě celého čísla. Funguje i při zadávání záporného čísla.

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

Umožňuje nahradit definované hodnoty více různými částmi vstupního řetězce.

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

Pokud potřebujeme pomocí Javascriptu nastavit nejvyšší dostupnou hodnotu pro prvek `z-index`, můžeme voláním funkce zjistit novou nejvyšší hodnotu.

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

Testuje zadanou vstupní hodnotu, zda je prázdná. Je možné zadat více vstupních hodnot najednou oddělených čárkou, a pokud je alespoň jedna z nich prázdná, metoda vrátí TRUE.

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

Ověří, zda je vstupní hodnota číselná. Zda je to číslo, zda je typu číslo nebo řetězec.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota typu Array.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota typu Object.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota nulová.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota objektem jQuery.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota řetězec.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota číslo.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota boolean.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda vstupní hodnota je funkce.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Testuje, zda je vstupní hodnota prvkem HTMLElement.

Je také možné zadat více vstupních hodnot najednou oddělených čárkou.

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

Zkontroluje, zda prvek existuje na základě jeho selektoru css. Pokud neexistuje, metoda naslouchá, a pokud existuje, zavolá metodu `existCallback`. Maximální čekací doba na prvek je 25 sekund. Poté je poslech přerušen a je provedeno volání `notExistCallback`

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

### Další

#### uuidv4()

Generuje náhodný řetězec univerzálního jedinečného identifikátoru verze 4 (UUID-4) podle specifikace RFC 4122.

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

#### kopírovat()

Vytvoří kopii vstupu. Tím se zabrání odkazům na objekty.

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

## Seznam změn

Verze 1.1.0

- [x] Upravená funkčnost metod is\*
- [x] Přidány nové metody

Verze 1.0.0

- [x] Přidat verzování
- [x] Správná gramatika

***

**[Todo list]**

Verze 2.0.0

- [ ] Rozdělte kategorie do samostatných tříd
