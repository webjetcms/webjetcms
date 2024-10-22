# Knižnica 'StorageHandler'
na správu a používanie Storage

---
**Dependencies (závislosti)**
- [Tools](tools.md)
- [Store](store.md)
---

`StorageHandler` umožňuje manipulovať s `window.localStorage` a `window.sessionStorage` na úrovni vnorených objektov,
kde každý vytvorený master kľúč zadaný pomocou [setStoreName()](#setstorename) obsahuje objekt,
v ktorom môžeme jednoducho definovať [setStorageItem()](#setstorageitem) a vyberať [getStorageItem()](#getstorageitem) dáta na základe sub-kľúča.


Rozširuje triedu [Store](store.md).
```javascript
import Store from '../store/store';

class StorageHandler extends Store {}
```

### Opis fungovania:

V systéme **WebJET** je `StorageHandler` používaný, ako rozhranie v rozširujúcich triedach a knižniciach, pre ukladanie dát pod kľúčom používanej triedy alebo knižnice.

Príkladom takýchto použití sú napr. [Translator](translator.md) alebo [CellVisibilityService](cell-visibility-service.md)

Metódy [getStorageItem()](#getstorageitem) a [setStorageItem()](#setstorageitem) nepracujú priamo s kľúčom local/session Storage ale s kľúčom objektu,
ktorý sa nachádza pod master kľúčom zadaným pomocou [setStoreName()](#setstorename) pri vytváraní inštancie triedy.
Dáta sa ukladajú, ako JSON objekt. Preto sa pri každej zmene v objekte prepíše hodnota master kľúča novým záznamom celého JSON objektu.

## Vytvorenie inštancie:

```javascript
import {StorageHandler} from 'storage-handler/storage-handler';
```
**Použitie:**
```javascript
// Defaultne sa pracuje s localStorage
const store = new StorageHandler();
store.setStoreName('názov-master-kľúča');

store.setStorageItem('sub-kľúč-1', 'hodnota-1');
store.setStorageItem('sub-kľúč-2', 'hodnota-2');
```

## Zoznam API
**(Kliknutím zobrazíš detail pre funkciu)**

| Metódy                                                | Poznámka    |
| -----------                                           | ----------- |
| [setStoreName()](#setstorename)                       |
| [setSessionStorage()](store.md?id=setsessionstorage)  | Zdedená metóda z triedy [Store](store.md) [presmeruje do súboru [store.md](store.md)] |
| [setLocalStorage()](store.md?id=setlocalstorage)      | Zdedená metóda z triedy [Store](store.md) [presmeruje do súboru [store.md](store.md)] |
| [getStorageItem()](#getstorageitem)                   |
| [setStorageItem()](#setstorageitem)                   |
| [removeStorageItem()](#removestorageitem)             |
| [clearData()](#cleardata)                             |
| [destroy()](#destroy)                                 |
| [storeExist()](#storeexist)                           |

Podrobnejšie API sa nachádza v [GIT repozitári](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/storage-handler#storagehandler-kni%C5%BEnica-na-spr%C3%A1vu-a-pou%C5%BE%C3%ADvanie-storage)

### Detailný popis funkcií

#### setStoreName()
Nastaví triede nový názov kľúča, do ktorého sa budú ukladať dáta. Dá sa použiť za behu, a tak mať iba jednu inštanciu. Metóda vracia triedu ``StorageHandler``
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
---
#### getStorageItem()
Vráti hodnotu na základe názvu zadaného kľúča.
```javascript
/**
 * @description Vráti hodnotu na základe názvu zadaného kľúča.
 * @param {string} key
 * @returns {string|null}
 * @public
 */
getStorageItem(key);
```
---
#### setStorageItem()
Vytvorí zadaný v úložisku kľúč a uloží pod ním zadanú hodnotu.
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
---
#### removeStorageItem()
Odstráni položku na základe kľúča. Metóde môže byť vypnutá kontrola caseSensitive.
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
---
#### clearData()
Zmaže aktuálne používané úložisko a nechá ho prázdne – null. Ponechá však master kľúč.
```javascript
/**
 * @description Zmaže aktuálne používané úložisko a nechá ho prázdne - null. Ponechá však master kľúč.
 * @returns {void}
 * @public
 */
clearData();
```
---
#### destroy()
Úplne odstráni aktuálne používané úložisko.
```javascript
/**
 * @description Úplne odstráni aktuálne používané úložisko.
 * @returns {void}
 * @public
 */
destroy()
```
---
#### storeExist()
Otestuje, či existuje zadané úložisko.
```javascript
/**
 * @description Otestuje, či existuje zadané úložisko.
 * @param {string} [storeName]
 * @returns {boolean}
 * @public
 */
storeExist(storeName);
```
---