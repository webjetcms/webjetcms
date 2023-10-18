# 'Store' rozširujúca knižnica *Storage
###### localStorage, sessionStorage

Knižnica Store upravuje funkcionalitu natívneho 
[Storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage). _[Presmeruje na oficiálnu dokumentáciu]_

Použitie:
```javascript
import Store from './store';
```
```javascript
// Defaultne sa pracuje s localStorage
const store = new Store();

store.setItem('key', 'value');
```

### Zoznam API:
###### (Kliknutím zobrazíš detail pre funkciu)
- [getItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem) _[Presmeruje na oficiálnu dokumentáciu]_
- [setItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/setItem) _[Presmeruje na oficiálnu dokumentáciu]_
- [removeItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/removeItem) _[Presmeruje na oficiálnu dokumentáciu]_
- [clear()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/clear) _[Presmeruje na oficiálnu dokumentáciu]_
- [key()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/key) _[Presmeruje na oficiálnu dokumentáciu]_
- [setLocalStorage()](#readme-setLocalStorage)
- [setSessionStorage()](#readme-setSessionStorage)
- [getKeys()](#readme-getKeys)
- [itemExist()](#readme-itemExist)
- [storage](#readme-storage)

## Detailný popis funkcií

#### setLocalStorage() <a name="readme-setLocalStorage"></a>

```javascript
/**
 * @description Nastaví typ Storage na localStorage.
 * @returns {Store}
 * @public
 */
setLocalStorage();
```
---
#### setSessionStorage() <a name="readme-setSessionStorage"></a>

```javascript
/**
 * @description Nastaví typ Storage na sessionStorage.
 * @returns {Store}
 * @public
 */
setSessionStorage();
```
---
#### getKeys() <a name="readme-getKeys"></a>

```javascript
/**
 * @description Vráti zoznam (array), všetkých kľúčov v aktuálnom Storage.
 * @returns {string[]}
 * @public
 */
getKeys();
```
---
#### itemExist() <a name="readme-itemExist"></a>

```javascript
/**
 * @description Overí, či v Storage existuje zadaný kľúč. 
 * @param {string} key
 * @returns {boolean}
 * @public
 */
itemExist(key);
```
---
#### storage <a name="readme-storage"></a>

```javascript
/**
 * @description Vráti aktuálne používaný Storage.
 * @returns {Storage}
 * @public
 * @readonly
 */
const currentStorage = store.storage | this.storage;
```
---