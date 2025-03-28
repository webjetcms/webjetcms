# 'JsTree Document Opener'

Rozšiřující knihovna

***

**Dependencies (závislosti)**

- [Tools](tools.md)

***

Knihovna umožňuje automatizovanou manipulaci (otevírání) s uzly stromu, který generuje knihovna jsTree a otevírání dokumentu na základě jeho ID v knihovně dataTables.

Rozšiřuje třídu `AbstractJsTreeOpener`

```javascript
import AbstractJsTreeOpener from "../abstract-opener/abstract-js-tree-opener";

export class JsTreeDocumentOpener extends AbstractJsTreeOpener {}
```

***

## Vytvoření instance:

**WebJET** vytváří instanci v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)

```javascript
import JsTreeDocumentOpener from "./libs/js-tree-document-opener/js-tree-document-opener";

window.jsTreeDocumentOpener = new JsTreeDocumentOpener();
```

**Použití:**

### Inicializace:

Inicializaci provedeme ideálně vždy, když je inicializován jstree zavoláním metody [init()](#init)

```javascript
window.jsTreeDocumentOpener.init();
```

Wo **WebJET** probíhá inicializace v souboru [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)

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

Metoda [init()](#init) zajistí: <a id="init-algo" />
- Vyparsuje z url adresy prohlížeče pomocí [Tools.getUrlQuery()](tools.md#geturlquery) search query za otazníkem
- Vybere z vyparsovaného objektu klíč `docid` pokud jsme ho před tím nezměnili pomocí [idKeyName](#idkeyname)
- Hodnotu z `docid` setne pomocí setteru [id](#id)
- Setter [id](#id) resetuje třídu do new stavu a ověří vstup: <a id="id-algo" />
  - Pokud je vstup nevalidní, tak v DEV prostředí vypíše hlášku o nevalidním vstupu a přeruší se provedení
    - Třída se přepne do stavu `Not Ready & Iddle`, čeká na zavolání setteru [id](#id)
  - Pokud je vstup validní provede se request na API, jehož defaultní adresu můžeme změnit pomocí [apiUrl](#apiurl)
    - Uloží se přijatá data do třídy aby se s nimi mohlo pracovat
    - Třída se přepne do stavu `Ready & Iddle` a čeká na zavolání [next()](#next) nebo [id](#id)

***

### Otevírání uzlů stromu:

Otevírání uzlů stromu se provádí voláním metody [next()](#next):

```javascript
window.jsTreeDocumentOpener.next();
```

Wo **WebJET** probíhá volání v souboru [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)

1. Po vyrederování stromu `/listener: loaded.jstree`

2. Po otevření (*open node*) uzlu `/listener: after_open.jstree`

3. Po vyjmutí (*select node*) uzlu `/listener: select_node.jstree`

***

!>**Upozornění:** V případě, že chceme externě nastavovat [id](#id) je dobré před tím použít metodu [loaded()](#loaded), ve které můžeme volat [next()](#next) a testovat [notFound](#notfound).

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

Problémem při implementaci bylo čekání na načtení stromové struktury a synchronizace datatabulky. Původní implementace používala události `this.dataTable.on('draw.dt', (evt, settings) => `, což ale nebylo vhodné pro použití při přepínání karet Složky, Systém a Koš, protože tam docházelo ihned k načtení datatabulky po přepnutí karty.

Kód používá funkci `setInterval` s počítáním volání ve funkci `waitForDatatableRowLoaded` a čekáním na nezobrazení `div.dt-processing:visible`. Když není zobrazeno, počítá ještě 3 intervaly nezobrazení a následně pokračuje v hledání web stránky. Podobně je řešeno i načítání stromové struktury, čekání je implementováno ve funkci `waitForJsTreeLoaded`. Funkce jsou implementovány přímo v `abstract-js-tree-opener.js`.

Hledání je také komplikované z důvodu stránkování a rozdílů mezi klientským a serverovým stránkováním. V obou případech postupně prochází jednotlivé stránky a hledá element se zadaným ID. Je-li nalezeno otevře editor.

Pokud je jako `docid` parametr zadaná hodnota `-1` vyvolá se otevření nového editoru simulovaným kliknutím na tlačítko Přidat.

Při kombinaci zadaného parametru `groupid` jsou možné následující situace:
- je zadané kladné `docid` - parametr `groupid` je ignorován, adresář se otevře podle adresáře zadané web stránky
- je zadáno `docid=-1`, parametr `groupid` se použije pro otevření adresářové struktury a následně se vyvolá otevření nové web stránky

***

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

| Metody | Gettery | Settery |
| --------------------------------- | --------------------- | ----------------------- |
| [init()](#init)                   | [notFound](#notfound) | [id](#id)               |
| [next()](#další)                   | | [apiUrl](#apiurl)       |
| [loaded()](#Převzato-z)               | | [idKeyName](#idkeyname) |
| [inputDataFrom()](#vstupní-data-z) | | |
| [setInputValue()](#setinputvalue) | | |

***

### Detailní popis funkcí

#### init()

Vyvolá inicializaci, která zajistí [tento následující postup](#init-algo)

```javascript
/**
 * @description Inicializácia po načítaní jstree
 * @returns {void}
 * @public
 */
window.jsTreeDocumentOpener.init();
```

***

#### next()

Otevře následující nodě v pořadí. Pokud již neexistuje v seznamu žádný další node, otevře dataTable dokument s naším ID.

```javascript
/**
 * @description Otvorí nasledujúci node v poradí.
 *              Ak už neexistuje v zozname žiadny ďalší node, otvorý dataTable dokument s našim ID.
 * @returns {void}
 * @public
 */
window.jsTreeDocumentOpener.next();
```

***

#### loaded()

Nastaví callback, který se provede po zadání nového [id](#id) a přijetí dat.

```javascript
/**
 * @description Nastaví callback, ktorý sa vykoná po zadaní nového DocID a prijatí dát.
 * @param {Function} callback [result, docId, JsTreeDocumentOpener]
 * @returns {JsTreeDocumentOpener}
 * @public
 */
window.jsTreeDocumentOpener.loaded(callback);
```

***

#### inputDataFrom()

Připojí zadaný input, který musí být vložen jako jQuery objekt `$('.input-css-trieda')`. Input zajišťuje zadání nového ID

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

***

#### setInputValue()

Pokud byl připojen pomocí [inputDataFrom()](#inputdatafrom) input, tak danému inputu můžeme odkudkoli nastavit hodnotu.

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

***

#### id

Nastavení nového ID dokumentu. Toto ID bude odesláno přes API a zároveň bude použito k otevření dokumentu po úspěšném otevření příslušných uzlů stromu.

[Klik pro více info, co setter id dělá](#id-algo)

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

***

#### apiUrl

Nastavení nové API url

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

***

#### idKeyName

Nový název URl query klíče, ve kterém se bude nacházet ID dokumentu.

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

***

#### notFound

Slouží ke zjištění, zda po přijetí dat, existuje v jsTree uzel, který chceme otevřít jako první. Pokud takový uzel neexistuje (nebyl nalezen / not found), tak se nám vrátí hodnota TRUE. Pokud takový uzel existuje (byl nalezen / found), tak se nám vrátí hodnota FALSE.

Ideální použití tohoto gettera je uvnitř metody [loaded()](#loaded)

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

***
