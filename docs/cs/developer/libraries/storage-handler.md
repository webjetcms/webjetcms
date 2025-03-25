# Knihovna 'StorageHandler'

pro správu a používání Storage

***

**Dependencies (závislosti)**

- [Tools](tools.md)
- [Store](store.md)

***

`StorageHandler` umožňuje manipulovat s `window.localStorage` a `window.sessionStorage` na úrovni vnořených objektů, kde každý vytvořený master klíč zadaný pomocí [setStoreName()](#setstorename) obsahuje objekt, ve kterém můžeme jednoduše definovat [setStorageItem()](#setstorageitem) a vybírat [getStorageItem()](#getstorageitem) data na základě sub-klíče.

Rozšiřuje třídu [Store](store.md).

```javascript
import Store from '../store/store';

class StorageHandler extends Store {}
```

### Popis fungování:

V systému **WebJET** je `StorageHandler` používaný, jako rozhraní v rozšiřujících třídách a knihovnách, pro ukládání dat pod klíčem používané třídy nebo knihovny.

Příkladem takových použití jsou například. [Translator](translator.md) nebo [CellVisibilityService](cell-visibility-service.md)

Metody [getStorageItem()](#getstorageitem) a [setStorageItem()](#setstorageitem) nepracují přímo s klíčem local/session Storage ale s klíčem objektu, který se nachází pod master klíčem zadaným pomocí [setStoreName()](#setstorename) při vytváření instance třídy. Data se ukládají, jako JSON objekt. Proto se při každé změně v objektu přepíše hodnota master klíče novým záznamem celého JSON objektu.

## Vytvoření instance:

```javascript
import {StorageHandler} from 'storage-handler/storage-handler';
```

**Použití:**

```javascript
// Defaultne sa pracuje s localStorage
const store = new StorageHandler();
store.setStoreName('názov-master-kľúča');

store.setStorageItem('sub-kľúč-1', 'hodnota-1');
store.setStorageItem('sub-kľúč-2', 'hodnota-2');
```

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

| Metody | Poznámka |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| [setStoreName()](#setstorename)                      |
| [setSessionStorage()](store.md?id=setsessionstorage) | Zděděná metoda ze třídy [Store](store.md) [přesměruje do souboru [store.md](store.md) ] |

 | [setLocalStorage()](store.md?id=setlocalstorage)     | Zděděná metoda ze třídy [Store](store.md) [přesměruje do souboru [store.md](store.md) ] |
 | [getStorageItem()](#getstorageitem)                  |
 | [setStorageItem()](#setstorageitem)                  |
 | [removeStorageItem()](#removestorageitem)            |
 | [clearData()](#cleardata)                            |
 | [destroy()](#destroy)                                |
 | [storeExist()](#storeexist)                          |

Podrobnější API se nachází v [GIT repozitáři](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/storage-handler#storagehandler-kni%C5%BEnica-na-spr%C3%A1vu-a-pou%C5%BE%C3%ADvanie-storage)

### Detailní popis funkcí

#### setStoreName()

Nastaví třídě nový název klíče, do kterého se budou ukládat data. Lze jej použít za běhu, a tak mít pouze jednu instanci. Metoda vrací třídu `StorageHandler`

```javascript
/**
 * @description Nastaví triede nový názov kľúča, do ktorého sa budú ukladať dáta.
 *              Dá sa použiť za behu a tak mať iba jednu inštanciu.
 *              Metóda vracia triedu ``StorageHandler``
 * @param {string} name
 * @returns {StorageHandler}
 * @public
 */
setStoreName(name);
```

***

#### getStorageItem()

Vrátí hodnotu na základě názvu zadaného klíče.

```javascript
/**
 * @description Vráti hodnotu na základe názvu zadaného kľúča.
 * @param {string} key
 * @returns {string|null}
 * @public
 */
getStorageItem(key);
```

***

#### setStorageItem()

Vytvoří zadaný v úložišti klíč a uloží pod ním zadanou hodnotu.

```javascript
/**
 * @description Vytvorí zadaný v úložisku kľúč a uloží pod ním zadanú hodnotu.
 * @param {string} key
 * @param {any|*} value
 * @returns {void}
 * @public
 */
setStorageItem(key, value);
```

***

#### removeStorageItem()

Odstraní položku na základě klíče. Metodě může být vypnuta kontrola caseSensitive.

```javascript
/**
 * @description Odstráni položku na základe kľúča.
 *              Metóde môže byť vypnutá kontrola caseSensitive.
 * @param {string} key
 * @param {boolean} [caseSensitive] true = is sensitive / false = is not sensitive
 * @returns {void}
 * @public
 */
removeStorageItem(key, caseSensitive = true);
```

***

#### clearData()

Smaže aktuálně používané úložiště a nechá jej prázdné – null. Ponechá však master klíč.

```javascript
/**
 * @description Zmaže aktuálne používané úložisko a nechá ho prázdne - null. Ponechá však master kľúč.
 * @returns {void}
 * @public
 */
clearData();
```

***

#### destroy()

Úplně odstraní aktuálně používané úložiště.

```javascript
/**
 * @description Úplne odstráni aktuálne používané úložisko.
 * @returns {void}
 * @public
 */
destroy()
```

***

#### storeExist()

Otestuje, zda existuje zadané úložiště.

```javascript
/**
 * @description Otestuje, či existuje zadané úložisko.
 * @param {string} [storeName]
 * @returns {boolean}
 * @public
 */
storeExist(storeName);
```

***
