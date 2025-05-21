# 'JsTree Folder Opener'

Rozšiřující knihovna

***

**Dependencies (závislosti)**

- [Tools](tools.md)

***

Knihovna umožňuje automatizovanou manipulaci (otevírání) s uzly stromu, který generuje knihovna jsTree, na základě ID uzlu (složky).

Rozšiřuje třídu `AbstractJsTreeOpener`

```javascript
import AbstractJsTreeOpener from "../abstract-opener/abstract-js-tree-opener";

export class JsTreeFolderOpener extends AbstractJsTreeOpener {}
```

***

## Vytvoření instance:

**WebJET** vytváří instanci v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)

```javascript
import JsTreeFolderOpener from "./libs/js-tree-document-opener/js-tree-document-opener";

window.jsTreeFolderOpener = new JsTreeFolderOpener();
```

**Použití:**

### Inicializace:

Inicializaci provedeme ideálně vždy, když je inicializován jstree zavoláním metody [init()](#init)

```javascript
window.jsTreeFolderOpener.init();
```

Wo **WebJET** probíhá inicializace v souboru [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)

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

Metoda [init()](#init) zajistí: <a id="init-algo" />
- Vyparsuje z url adresy prohlížeče pomocí [Tools.getUrlQuery()](tools.md#geturlquery) search query za otazníkem
- Vybere z vyparsovaného objektu klíč `groupid` pokud jsme ho před tím nezměnili pomocí [idKeyName](#idkeyname)
- Hodnotu z `groupid` setne pomocí setteru [id](#id)
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
window.jsTreeFolderOpener.next();
```

Wo **WebJET** probíhá volání v souboru [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)

1. Po vyrederování stromu `/listener: loaded.jstree`

2. Po otevření (*open node*) uzlu `/listener: after_open.jstree`

3. Po vyjmutí (*select node*) uzlu `/listener: select_node.jstree`

Podporováno je zadání speciálních značek do parametru `groupid`:
- `SYSTEM` - otevře se karta Systém
- `TRASH` - otevře se karta Koš

Příklad takové URL adresy: `/admin/v9/webpages/web-pages-list/?groupid=SYSTEM`.

***

!>**Upozornění:** V případě, že chceme externě nastavovat [id](#id) je dobré před tím použít metodu [loaded()](#loaded), ve které můžeme volat [next()](#next) a testovat [notFound](#notfound).

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

***

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

| Metody | Gettery | Settery |
 | --------------------------------- | --------------------- | ----------------------- |
 | [init()](#init)                   | [notFound](#notfound) | [id](#id)               |
 | [next()](#next)                   | | [apiUrl](#apiurl)       |
 | [loaded()](#loaded)               | | [idKeyName](#idkeyname) |
| [inputDataFrom()](#inputdatafrom) | | |
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
window.jsTreeFolderOpener.init();
```

***

#### next()

Otevře následující nodě v pořadí.

```javascript
/**
 * @description Otvorí nasledujúci node v poradí.
 * @returns {void}
 * @public
 */
window.jsTreeFolderOpener.next();
```

***

#### loaded()

Nastaví callback, který se provede po zadání nového [id](#id) a přijetí dat.

```javascript
/**
 * @description Nastaví callback, ktorý sa vykoná po zadaní nového DocID a prijatí dát.
 * @param {Function} callback [result, docId, JsTreeFolderOpener]
 * @returns {JsTreeFolderOpener}
 * @public
 */
window.jsTreeFolderOpener.loaded(callback);
```

***

#### inputDataFrom()

Připojí zadaný input, který musí být vložen jako jQuery objekt `$('.input-css-trieda')`. Input zajišťuje zadání nového ID

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
window.jsTreeFolderOpener.setInputValue(value);
```

***

#### id

Nastavení nového ID složky. Toto ID bude odesláno přes API

[Klik pro více info, co setter id dělá](#id-algo)

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
window.jsTreeFolderOpener.apiUrl = value;
```

***

#### idKeyName

Nový název url query klíče, ve kterém se bude nacházet ID uzlu.

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
window.jsTreeFolderOpener.notFound;
```

***
