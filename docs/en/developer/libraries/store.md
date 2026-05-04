# `Store` extending Storage class `localStorage, sessionStorage`

The `Store` class modifies the functionality of the native
[Storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage). _[Redirects to official documentation]_

### Description of operation:

This is a wrapper that allows you to directly store data of various types `Storage` with protection against unwanted application crashes due to incorrect input.
Inputs are automatically converted to `JSON` format.

!>**Warning:** It is not possible to save `HTMLElementy`, `NodeListy` and `funkcie`.

It is not used globally in **WebJET**. You must always create an instance of this class within your new class or library.

## Creating an instance:

```javascript
import Store from './store';

// Defaultne sa pracuje s localStorage
const store = new Store();
```
**Usage:**

```javascript
store.setItem('key', 'value');

const valueVar = store.getItem('key');
```

## API List

**(Click to view feature details)**

| Methods                                    | Note                                                                                                          | Getters               |
| -----------                               | -----------                                                                                                       | -----------           |
| [setLocalStorage()](#setlocalstorage)     | [Official documentation - localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)     | [storage](#storage)   |
| [setSessionStorage()](#setsessionstorage) | [Official documentation - sessionStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage) |
| [getItem()](#getitem)                     |
| [setItem()](#setitem)                     |
| [removeItem()](#removeitem)               |
| [clear()](#clear)                         |
| [key()](#key)                             |
| [getKeys()](#getkeys)                     |
| [itemExist()](#itemexist)                 |

### Detailed description of functions

#### getItem()

[getItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem). _[Redirects to official documentation]_

**Extension Description:** The output of this method will be automatically converted from the `JSON` format to the original input format, eliminating the need to cast the outputs back to their original form.
If the specified key does not exist or there is no data under the specified key, the method returns `null`

---
#### setItem()

[setItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/setItem). _[Redirects to official documentation]_

**Extension Description:** The input data of this method will be automatically converted to `JSON` format. If the input is invalid, the value `null` will be stored.

---
#### removeItem()

[removeItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/removeItem). _[Redirects to official documentation]_

---
#### clear()

[clear()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/clear). _[Redirects to official documentation]_

---
#### key()

[key()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/key). _[Redirects to official documentation]_

---

#### setLocalStorage()

Sets the Storage type to **localStorage**.
```javascript
/**
 * @description Nastaví typ Storage na localStorage.
 * @returns {Store}
 * @public
 */
setLocalStorage();
```
---
#### setSessionStorage()

Sets the Storage type to **sessionStorage**.
```javascript
/**
 * @description Nastaví typ Storage na sessionStorage.
 * @returns {Store}
 * @public
 */
setSessionStorage();
```
---
#### getKeys()

Returns a list (array) of all keys in the current Storage.
```javascript
/**
 * @description Vráti zoznam (array), všetkých kľúčov v aktuálnom Storage.
 * @returns {string[]}
 * @public
 */
getKeys();
```
---
#### itemExist()

Verifies whether the specified key exists in Storage.
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
#### storage

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