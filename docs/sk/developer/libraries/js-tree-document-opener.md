# 'JsTree Document Opener'
Rozširujúca knižnica

---
**Dependencies (závislosti)**
- [Tools](tools.md)
---

Knižnica umožňuje automatizovanú manipuláciu (otváranie) s uzlami stromu,
ktorý generuje knižnica jsTree a otváranie dokumentu na základe jeho ID v knižnici dataTables.

Rozširuje triedu `AbstractJsTreeOpener`
```javascript
import AbstractJsTreeOpener from "../abstract-opener/abstract-js-tree-opener";

export class JsTreeDocumentOpener extends AbstractJsTreeOpener {}
```

---
## Vytvorenie inštancie:
**WebJET** vytvára inštanciu v súbore [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)
```javascript
import JsTreeDocumentOpener from "./libs/js-tree-document-opener/js-tree-document-opener";

window.jsTreeDocumentOpener = new JsTreeDocumentOpener();
```
**Použitie:**

### Inicializácia:

Inicializáciu vykonajme ideálne vždy, keď je inicializovaný jstree zavolaním metódy [init()](#init)
```javascript
window.jsTreeDocumentOpener.init();
```

Wo **WebJET** prebieha inicializácia v súbore [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)
```javascript
window.jstree = somStromcek.jstree({
    'core': {
        'check_callback': function (operation, node, parent, position, more) {},
        'data': function (obj, callback) {
            window.jsTreeDocumentOpener.init();
        }
    }
});
```

Metóda [init()](#init) zabezpečí: <a id="init-algo"></a>
- Vyparsuje z url adresy prehliadača pomocou [Tools.getUrlQuery()](tools.md#geturlquery) search query za otáznikom
- Vyberie z vyparsovaného objektu kľúč `docid` ak sme ho pred tým nezmenili pomocou [idKeyName](#idkeyname)
- Hodnotu z `docid` setne pomocou setteru [id](#id)
- Setter [id](#id) resetne triedu do new stavu a overí vstup: <a id="id-algo"></a>
    - Ak je vstup nevalidný, tak v DEV prostredí vypíše hlášku o nevalidnom vstupe a preruší sa vykonanie
        - Trieda sa prepne do stavu `Not Ready & Iddle`, čaká na zavolanie setteru [id](#id)
    - Ak je vstup validný vykoná sa request na API, ktorého defaultnú adresu môžeme zmeniť pomocou [apiUrl](#apiurl)
        - Uložia sa prijaté dáta do triedy aby sa s nimi mohlo pracovať
        - Trieda sa prepne do stavu `Ready & Iddle` a čaká na zavolanie [next()](#next) alebo [id](#id)

---

### Otváranie uzlov stromu:
Otváranie uzlov stromu sa vykonáva volaním metódy [next()](#next):
```javascript
window.jsTreeDocumentOpener.next();
```

Wo **WebJET** prebieha volanie v súbore [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)
1. Po vyrederovaní stromu `/listener: loaded.jstree`
2. Po otvorení (_open node_) uzla `/listener: after_open.jstree`
3. Po vybratí (_select node_) uzla `/listener: select_node.jstree`

---
!>**Upozornenie:**
V prípade ak chceme externe nastavovať [id](#id) je dobré pred tým použiť metódu [loaded()](#loaded),
v ktorej môžeme volať [next()](#next) a testovať [notFound](#notfound).
```javascript
/** @type {JsTreeDocumentOpener} jstdo */
const jstdo = window.jsTreeDocumentOpener.loaded((result, docId, selfClass) => {
    if (selfClass.notFound) {
        // Napr. nejaký alert pre používateľa
    } else {
        selfClass.next();
    }
});

jstdo.id = 4;
```

Problémom pri implementácii bolo čakanie na načítanie stromovej štruktúry a synchronizácia datatabuľky. Pôvodná implementácia používala udalosti ```this.dataTable.on('draw.dt', (evt, settings) => ```, čo ale nebolo vhodné na použitie pri prepínaní kariet Priečinky, Systém a Kôš, pretože tam dochádzalo ihneď k načítaniu datatabuľky po prepnutí karty.

Kód používa funkciu ```setInterval``` s počítaním volania vo funkcii ```waitForDatatableRowLoaded``` a čakaním na nezobrazenie ```div.dt-processing:visible```. Keď nie je zobrazené, počíta ešte 3 intervaly nezobrazenia a následne pokračuje v hľadaní web stránky. Podobne je riešené aj načítavanie stromovej štruktúry, čakanie je implementované vo funkcii ```waitForJsTreeLoaded```. Funkcie sú implementované priamo v ```abstract-js-tree-opener.js```.

Hľadanie je tiež komplikované z dôvodu stránkovania a rozdielov medzi klientskym a serverovým stránkovaním. V oboch prípadoch postupne prechádza jednotlivé stránky a hľadá element so zadaným ID. Ak je nájdené otvorí editor.

Ak je ako ```docid``` parameter zadaná hodnota ```-1``` vyvolá sa otvorenie nového editora simulovaným kliknutím na tlačidlo Pridať.

Pri kombinácii zadaného parametra ```groupid``` sú možné nasledovné situácie:

- je zadané kladné ```docid``` - parameter ```groupid``` je ignorovaný, adresár sa otvorí podľa adresára zadanej web stránky
- je zadané ```docid=-1```, parameter ```groupid``` sa použije pre otvorenie adresárovej štruktúry a následne sa vyvolá otvorenie novej web stránky

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
window.jsTreeDocumentOpener.init();
```
---
#### next()
Otvorí nasledujúci node v poradí. Ak už neexistuje v zozname žiadny ďalší node, otvorí dataTable dokument s našim ID.
```javascript
/**
 * @description Otvorí nasledujúci node v poradí.
 *              Ak už neexistuje v zozname žiadny ďalší node, otvorý dataTable dokument s našim ID.
 * @returns {void}
 * @public
 */
window.jsTreeDocumentOpener.next();
```
---
#### loaded()
Nastaví callback, ktorý sa vykoná po zadaní nového [id](#id) a prijatí dát.
```javascript
/**
 * @description Nastaví callback, ktorý sa vykoná po zadaní nového DocID a prijatí dát.
 * @param {Function} callback [result, docId, JsTreeDocumentOpener]
 * @returns {JsTreeDocumentOpener}
 * @public
 */
window.jsTreeDocumentOpener.loaded(callback);
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
 * @param {function} [withNotifyCallback] Callback, ktorý sa vykoná, pri chybnom vstupe alebo nenájdenom dokumente.
 *                                        V callbacku je dostupný argument `value`
 * @returns {void}
 * @public
 */
window.jsTreeDocumentOpener.inputDataFrom(openerInput, withNotifyCallback);
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
window.jsTreeDocumentOpener.setInputValue(value);
```
---
#### id
Nastavenie nového ID dokumentu.
Toto ID bude odoslané cez API a zároveň bude použité na otvorenie dokumentu po úspešnom otvorení príslušných uzlov stromu.

[Klik pre viac info, čo setter id robí](#id-algo)
```javascript
/**
 * @description Nastavenie nového ID dokumentu.
 *              Toto ID bude odoslané cez API a zároveň bude použité na otvorenie dokumentu po úspešnom otvorení príslušných uzlov stromu.
 * @default docid
 * @param {number|string} id
 * @returns {void}
 * @public
 * @setter
 */
window.jsTreeDocumentOpener.docId = value;
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
window.jsTreeDocumentOpener.apiUrl = value;
```
---
#### idKeyName
Nový názov URl query kľúča, v ktorom sa bude nachádzať ID dokumentu.
```javascript
/**
 * @description Nový názov URl query kľúča, v ktorom sa bude nachádzať ID dokumentu.
 * @param {string} value
 * @returns {void}
 * @public
 * @setter
 */
window.jsTreeDocumentOpener.idKeyName = value;
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
window.jsTreeDocumentOpener.notFound;
```
---