# 'JsTree Folder Opener'
Rozširujúca knižnica

---
**Dependencies (závislosti)**
- [Tools](tools.md)
---

Knižnica umožňuje automatizovanú manipuláciu (otváranie) s uzlami stromu,
ktorý generuje knižnica jsTree, na základe ID uzla (priečinku).

Rozširuje triedu `AbstractJsTreeOpener`
```javascript
import AbstractJsTreeOpener from "../abstract-opener/abstract-js-tree-opener";

export class JsTreeFolderOpener extends AbstractJsTreeOpener {}
```

---
## Vytvorenie inštancie:
**WebJET** vytvára inštanciu v súbore [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)
```javascript
import JsTreeFolderOpener from "./libs/js-tree-document-opener/js-tree-document-opener";

window.jsTreeFolderOpener = new JsTreeFolderOpener();
```
**Použitie:**

### Inicializácia:

Inicializáciu vykonajme ideálne vždy, keď je inicializovaný jstree zavolaním metódy [init()](#init)
```javascript
window.jsTreeFolderOpener.init();
```

Wo **WebJET** prebieha inicializácia v súbore [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)
```javascript
window.jstree = somStromcek.jstree({
    'core': {
        'check_callback': function (operation, node, parent, position, more) {},
        'data': function (obj, callback) {
            window.jsTreeFolderOpener.init();
        }
    }
});
```

Metóda [init()](#init) zabezpečí: <a id="init-algo"></a>
1. Vyparsuje z url adresy prehliadača pomocou [Tools.getUrlQuery()](tools.md#geturlquery) search query za otáznikom
2. Vyberie z vyparsovaného objektu kľúč `groupid` ak sme ho pred tým nezmenili pomocou [idKeyName](#idkeyname)
3. Hodnotu z `groupid` setne pomocou setteru [id](#id)
4. Setter [id](#id) resetne triedu do new stavu a overí vstup: <a id="id-algo"></a>
    1. Ak je vstup nevalidný, tak v DEV prostredí vypíše hlášku o nevalidnom vstupe a preruší sa vykonanie
        1. Trieda sa prepne do stavu `Not Ready & Iddle`, čaká na zavolanie setteru [id](#id)
    2. Ak je vstup validný vykoná sa request na API, ktorého defaultnú adresu môžeme zmeniť pomocou [apiUrl](#apiurl)
        1. Uložia sa prijaté dáta do triedy aby sa s nimi mohlo pracovať
        2. Trieda sa prepne do stavu `Ready & Iddle` a čaká na zavolanie [next()](#next) alebo [id](#id)

---

### Otváranie uzlov stromu:
Otváranie uzlov stromu sa vykonáva volaním metódy [next()](#next):
```javascript
window.jsTreeFolderOpener.next();
```

Wo **WebJET** prebieha volanie v súbore [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)
1. Po vyrederovaní stromu `/listener: loaded.jstree`
2. Po otvorení (_open node_) uzla `/listener: after_open.jstree`
3. Po vybratí (_select node_) uzla `/listener: select_node.jstree`

Podporované je zadanie špeciálnych značiek do parametra ```groupid```:

- ```SYSTEM``` - otvorí sa karta Systém
- ```TRASH``` - otvorí sa karta Kôš

Príklad takej URL adresy: ```/admin/v9/webpages/web-pages-list/?groupid=SYSTEM```.

---
**Upozornenie:**
V prípade ak chceme externe nastavovať [id](#id) je dobré pred tým použiť metódu [loaded()](#loaded),
v ktorej môžeme volať [next()](#next) a testovať [notFound](#notfound).
```javascript
/** @type {JsTreeFolderOpener} jstfo */
const jstfo = window.jsTreeFolderOpener.loaded((result, docId, selfClass) => {
    if (selfClass.notFound) {
        // Napr. nejaký alert pre používateľa
    } else {
        selfClass.next();
    }
});

jstfo.id = 4;
```

---

## Zoznam API
**(Kliknutím zobrazíš detail pre funkciu)**

| Metódy                            | Gettery               | Settery                   |
| -----------                       | -----------           | -----------               |
| [init()](#init)                   | [notFound](#notfound) | [id](#id)                 |
| [next()](#next)                   |                       | [apiUrl](#apiurl)         |
| [loaded()](#loaded)               |                       | [idKeyName](#idkeyname)   |
| [inputDataFrom()](#inputdatafrom) |                       |                           |
| [setInputValue()](#setinputvalue) |                       |                           |

---
### Detailný popis funkcií

#### init()
Vyvolá inicializáciu, ktorá zabezpečí [tento nasledovný postup](#init-algo)
```javascript
/**
 * @description Inicializácia po načítaní jstree
 * @returns {void}
 * @public
 */
window.jsTreeFolderOpener.init();
```
---
#### next()
Otvorí nasledujúci node v poradí.
```javascript
/**
 * @description Otvorí nasledujúci node v poradí.
 * @returns {void}
 * @public
 */
window.jsTreeFolderOpener.next();
```
---
#### loaded()
Nastaví callback, ktorý sa vykoná po zadaní nového [id](#id) a prijatí dát.
```javascript
/**
 * @description Nastaví callback, ktorý sa vykoná po zadaní nového DocID a prijatí dát.
 * @param {Function} callback [result, docId, JsTreeFolderOpener]
 * @returns {JsTreeFolderOpener}
 * @public
 */
window.jsTreeFolderOpener.loaded(callback);
```
---
#### inputDataFrom()
Pripojí zadaný input, ktorý musí byť vložený ako jQuery objekt `$('.input-css-trieda')`.
Input zabezpečuje zadanie nového ID
```javascript
/**
 * @description Pripojí zadaný input, ktorý musí byť vložený ako jQuery objekt `$('.input-css-trieda')`
 *              Input zabezpečuje zadanie nového ID
 * @param {jQuery} openerInput jQuery selector for input
 * @param {function} [withNotifyCallback] Callback, ktorý sa vykoná, pri chybnom vstupe.
 *                                        V callbacku je dostupný argument `value`
 * @returns {void}
 * @public
 */
window.jsTreeFolderOpener.inputDataFrom(openerInput, withNotifyCallback);
```
---

#### setInputValue()
Ak bol pripojený pomocou [inputDataFrom()](#inputdatafrom) input, tak danému inputu môžeme odkiaľkoľvek nastaviť hodnotu.
```javascript
/**
 * @description Ak bol pripojený pomocou inputDataFrom() input, tak danému inputu môžeme odkiaľkoľvek nastaviť value.
 * @see inputDataFrom
 * @param {string|number} value
 * @returns {void}
 * @public
 */
window.jsTreeFolderOpener.setInputValue(value);
```
---
#### id
Nastavenie nového ID priečinka.
Toto ID bude odoslané cez API

[Klik pre viac info, čo setter id robí](#id-algo)
```javascript
/**
 * @description Nastavenie nového ID priečinka.
 *              Toto ID bude odoslané cez API
 * @default groupid
 * @param {number|string} id
 * @returns {void}
 * @public
 * @setter
 */
window.jsTreeFolderOpener.docId = value;
```
---
#### apiUrl
Nastavenie novej API url
```javascript
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
window.jsTreeFolderOpener.apiUrl = value;
```
---
#### idKeyName
Nový názov URl query kľúča, v ktorom sa bude nachádzať ID uzla.
```javascript
/**
 * @description Nový názov URl query kľúča, v ktorom sa bude nachádzať ID uzla.
 * @param {string} value
 * @returns {void}
 * @public
 * @setter
 */
window.jsTreeFolderOpener.idKeyName = value;
```
---
#### notFound
Slúži na zistenie, či po prijatí dát, existuje v jsTree uzol, ktorý chceme otvoriť ako prvý.
Ak takýto uzol neexistuje (nebol nájdený / not found), tak sa nám vráti hodnota TRUE.
Ak takýto uzol existuje (bol nájdený / found), tak sa nám vráti hodnota FALSE.

Ideálne použitie tohto gettera je vo vnútri metódy [loaded()](#loaded)
```javascript
/**
 * @description Slúži na zistenie, či po prijatí dát, existuje v jsTree uzol, ktorý chceme otvoriť ako prvý.
 *              Ak takýto uzol neexistuje (nebol nájdený / not found), tak sa nám vráti hodnota TRUE
 *              Ak takýto uzol existuje (bol nájdený / found), tak sa nám vráti hodnota FALSE
 *              Ideálne použitie tohto gettera je vo vnútri metódy loaded()
 * @returns {boolean}
 * @public
 * @getter
 */
window.jsTreeFolderOpener.notFound;
```
---