# 'JsTree Folder Opener'

Rozšiřování knihovny

***

**Závislosti**

- [Nástroje](tools.md)

***

Knihovna umožňuje automatizovanou manipulaci (otevírání) uzlů stromů generovaných knihovnou jsTree na základě ID uzlu (složky).

Rozšiřuje třídu `AbstractJsTreeOpener`

```javascript
import AbstractJsTreeOpener from "../abstract-opener/abstract-js-tree-opener";

export class JsTreeFolderOpener extends AbstractJsTreeOpener {}
```

***

## Vytvoření instance:

**WebJET** vytvoří instanci v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)

```javascript
import JsTreeFolderOpener from "./libs/js-tree-document-opener/js-tree-document-opener";

window.jsTreeFolderOpener = new JsTreeFolderOpener();
```

**Použití:**

### Iniciace:

V ideálním případě se inicializace provede vždy, když se inicializuje strom jstree voláním metody [init()](#init)

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

Metoda [init()](#init) zajistit: <a id="init-algo" />
- Parsuje z url prohlížeče pomocí [Tools.getUrlQuery()](tools.md#geturlquery) vyhledávací dotaz za otazníkem
- Odstraní klíč z vypreparovaného objektu `groupid` pokud jsme ji nezměnili dříve pomocí [idKeyName](#idkeyname)
- Hodnota z `groupid` prasnice se setrem [id](#id)
- Setter [id](#id) uvede třídu do nového stavu a ověří platnost vstupu: <a id="id-algo" />
  - Pokud je vstup neplatný, prostředí DEV vypíše zprávu o neplatném vstupu a přeruší provádění.
    - Třída se přepne do stavu `Not Ready & Iddle`, čekající na výzvu zadavatele [id](#id)
  - Pokud je vstup platný, je odeslán požadavek na rozhraní API, jehož výchozí adresu lze změnit pomocí příkazu [apiUrl](#apiurl)
    - Přijatá data jsou uložena ve třídě, aby s nimi bylo možné pracovat.
    - Třída se přepne do stavu `Ready & Iddle` a čeká na zavolání [next()](#další) nebo [id](#id)

***

### Otevírání uzlů stromu:

Otevírání uzlů stromu se provádí voláním metody [next()](#další):

```javascript
window.jsTreeFolderOpener.next();
```

Wo **WebJET** v souboru probíhá volání [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js)

1. Po prořezání stromu `/listener: loaded.jstree`

2. Po otevření (*otevřený uzel*) uzlu `/listener: after_open.jstree`

3. Po výběru (*vybrat uzel*) uzlu `/listener: select_node.jstree`

Zadávání speciálních značek v parametru je podporováno `groupid`:
- `SYSTEM` - otevře se karta Systém
- `TRASH` - otevře se karta Košík

Příklad takové adresy URL: `/admin/v9/webpages/web-pages-list/?groupid=SYSTEM`.

***

!>**Varování:** V případě, že chceme externě nastavit [id](#id) je dobré použít metodu před [loaded()](#Převzato-z) ve kterém můžeme volat [next()](#další) a testovat [notFound](#notfound).

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

## Seznam rozhraní API

**(Kliknutím zobrazíte detail funkce)**

| Metody | Gettery | Settery |
| --------------------------------- | --------------------- | ----------------------- |
| [init()](#init)                   | [notFound](#notfound) | [id](#id)               |
| [next()](#další)                   | | [apiUrl](#apiurl)       |
| [loaded()](#Převzato-z)               | | [idKeyName](#idkeyname) |
| [inputDataFrom()](#vstupní-data-z) | | |
| [setInputValue()](#setinputvalue) | | |

***

### Podrobný popis funkcí

#### init()

vyvolá inicializaci, která poskytuje [následující postup](#init-algo)

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

Otevře další uzel v pořadí.

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

Nastaví zpětné volání, které se provede po vytvoření nového [id](#id) a přijímání dat.

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

Připojí zadaný vstup, který musí být vložen jako objekt jQuery `$('.input-css-trieda')`. Vstup zajišťuje zadání nového ID

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

Pokud byl připojen pomocí [inputDataFrom()](#vstupní-data-z) můžeme nastavit hodnotu vstupu odkudkoli.

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

Nastavte nové ID složky. Toto ID bude odesláno prostřednictvím rozhraní API

[Klikněte pro více informací o tom, co dělá setter id](#id-algo)

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

Nastavení nové url adresy API

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

Nový název klíče dotazu URl, který bude obsahovat ID uzlu.

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

Slouží k určení, zda po přijetí dat existuje ve stromu jsTree uzel, který chceme otevřít jako první. Pokud takový uzel neexistuje (nebyl nalezen), vrátí se hodnota TRUE. Pokud takový uzel existuje (byl nalezen), vrací se hodnota FALSE.

Ideální použití tohoto getteru je uvnitř metody [loaded()](#Převzato-z)

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
