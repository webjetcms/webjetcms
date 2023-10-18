# 'StorageHandler' knižnica na správu a používanie *Storage
###### localStorage, sessionStorage

Rozširuje knižnicu [Store](../store). 
Knižnica [Store](../store) upravuje funkcionalitu natívneho 
[Storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage). _[Presmeruje na oficiálnu dokumentáciu]_
```javascript
import Store from '../store/store';

class StorageHandler extends Store {}
```

Použitie:
```javascript
import {StorageHandler} from 'storage-handler/storage-handler';
```
```javascript
// Defaultne sa pracuje s localStorage
const store = new StorageHandler();
store.setStoreName('názov');

store.setStorageItem('key', 'value');
```

### Zoznam API:
###### (Kliknutím zobrazíš detail pre funkciu)
- [setStoreName()](#readme-setStoreName)
- [getStorageItem()](#readme-getStorageItem)
- [setStorageItem()](#readme-setStorageItem)
- [removeStorageItem()](#readme-removeStorageItem)
- [clearData()](#readme-clearData)
- [destroy()](#readme-destroy)
- [getData()](#readme-getData)
- [setData()](#readme-setData)
- [storeExist()](#readme-storeExist)
## Detailný popis funkcií


#### setStoreName() <a name="readme-setStoreName"></a>

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
#### getStorageItem() <a name="readme-getStorageItem"></a>

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
#### setStorageItem() <a name="readme-setStorageItem"></a>

```javascript
/**
 * @description Vytvorí zadaný kľúč a uloží do neho zadanú hodnotu.
 * @param {string} key
 * @param {string} value
 * @returns {void}
 * @public
 */
setStorageItem(key, value);
```
---
#### removeStorageItem() <a name="readme-removeStorageItem"></a>

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
#### clearData() <a name="readme-clearData"></a>

```javascript
/**
 * @description Zmaže aktuálne používané úložisko a nechá ho prázdne - null.
 * @returns {void}
 * @public
 */
clearData();
```
---
#### destroy() <a name="readme-destroy"></a>

```javascript
/**
 * @description Odstráni aktuálne používané úložisko.
 * @returns {void}
 * @public
 */
destroy()
```
---
#### setData() <a name="readme-setData"></a>

```javascript
/**
 * @description Uloží do aktuálne zvoleného úložiska zadané dáta. Dáta netreba konvertovať.
 * @param {Object|string|number} [data]
 * @returns {void}
 * @public
 */
setData(data = {});
```
---
#### getData() <a name="readme-getData"></a>

```javascript
/**
 * @description Vráti z aktuálne zvoleného úložiska všetky dáta.
 * @returns {Object}
 * @public
 */
getData();
```
---
#### storeExist() <a name="readme-storeExist"></a>

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