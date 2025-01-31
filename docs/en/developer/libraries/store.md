# `Store` Storage extension class `localStorage, sessionStorage`

Class `Store` modifies the functionality of the native [Storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage). *[Redirects to official documentation]*

### Description of operation:

This is a wrapper that allows you to directly save to `Storage` data of different types `[number, string, object, array, boolean, null, undefined, ...]` with treatment against unwanted dropping of the application during incorrect entry. Inputs are automatically converted to `JSON`.

!>**Warning:** It is not possible to store `HTMLElementy`, `NodeListy` a `funkcie`.

In the system **WebJET** is not used globally. You should always create an instance of this class within your new class or library.

## Creating an instance:

```javascript
import Store from './store';

// Defaultne sa pracuje s localStorage
const store = new Store();
```

**Application:**

```javascript
store.setItem('key', 'value');

const valueVar = store.getItem('key');
```

## List of APIs

**(Click to see the detail for the function)**

| Methods | Note | Gettery |
| ----------------------------------------- | ----------------------------------------------------------------------------------------------------------------- | ------------------- |
| [setLocalStorage()](#setlocalstorage)     | [Official documentation - localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)     | [storage](#storage) |
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

[getItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem). *[Redirects to official documentation]*

**Description of the extension:** The output of this method will be automatically converted from the format `JSON` to the original input format, so there is no need to retype the outputs back to the original format. If the specified key does not exist or there is no data under the specified key, the method returns `null`

***

#### setItem()

[setItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/setItem). *[Redirects to official documentation]*

**Description of the extension:** The input data of this method will be automatically converted into the format `JSON`. If the input is non-valid, the stored value will be `null`

***

#### removeItem()

[removeItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/removeItem). *[Redirects to official documentation]*

***

#### clear()

[clear()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/clear). *[Redirects to official documentation]*

***

#### key()

[key()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/key). *[Redirects to official documentation]*

***

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

***

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

***

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

***

#### itemExist()

Verifies that the specified key exists in Storage.

```javascript
/**
 * @description Overí, či v Storage existuje zadaný kľúč.
 * @param {string} key
 * @returns {boolean}
 * @public
 */
itemExist(key);
```

***

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

***
