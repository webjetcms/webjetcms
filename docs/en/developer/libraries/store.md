# 'Store' extension class \*Storage `localStorage, sessionStorage`

Class `Store` modifies the functionality of the native [Storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage). *\[Redirects to official documentation]*

### Description of operation:

This is a wrapper that allows you to directly save to `Storage` data of different types `[number, string, object, array, boolean, null, undefined, ...]` with treatment against unwanted dropping of the application during incorrect entry. Inputs are automatically converted to `JSON`.

**Warning:** It is not possible to store `HTMLElementy`, `NodeListy` a `funkcie`.
In the system **WebJET** is not used globally. You should always create an instance of this class within your new class or library.

## Creating an instance:

```javascript
import Store from "./store";

// Defaultne sa pracuje s localStorage
const store = new Store();
```

**Application:**
```javascript
store.setItem("key", "value");

const valueVar = store.getItem("key");
```

## List of APIs

**(Click to see the detail for the function)**
| Methods | Note | Gettery | | ----------------------------------------- | ----------------------------------------------------------------------------------------------------------------- | ------------------- | | [setLocalStorage()](#setlocalstorage)     | [Official documentation - localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)     | [storage](#storage) | | [setSessionStorage()](#setsessionstorage) | [Official documentation - sessionStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage) | | [getItem()](#getitem)                     | | [setItem()](#setitem)                     | | [removeItem()](#removeitem)               | | [clear()](#clear)                         | | [key()](#key)                             | | [getKeys()](#getkeys)                     | | [itemExist()](#itemexist)                 |

### Detailed description of functions

#### getItem()

[getItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem)\[Redirects to official documentation]*Description of the extension:*

** The output of this method will be automatically converted from the format ** to the original input format, so there is no need to retype the outputs back to the original format. If the specified key does not exist or there is no data under the specified key, the method returns `JSON`setItem()`null`
***
#### setItem()

[\[Redirects to official documentation\]](https://developer.mozilla.org/en-US/docs/Web/API/Storage/setItem)Description of the extension:**

****removeItem()`JSON`removeItem()`null`
***
#### \[Redirects to official documentation]

[clear()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/removeItem)clear()*\[Redirects to official documentation]*
***
#### key()

[key()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/clear)\[Redirects to official documentation]*setLocalStorage()*

***
####

[](https://developer.mozilla.org/en-US/docs/Web/API/Storage/key).*setSessionStorage()*
***
####

**.**getKeys()
```javascript
/**
 * @description Nastaví typ Storage na localStorage.
 * @returns {Store}
 * @public
 */
setLocalStorage();
```

***
####

itemExist()****storage
```javascript
/**
 * @description Nastaví typ Storage na sessionStorage.
 * @returns {Store}
 * @public
 */
setSessionStorage();
```

***
####



```javascript
/**
 * @description Vráti zoznam (array), všetkých kľúčov v aktuálnom Storage.
 * @returns {string[]}
 * @public
 */
getKeys();
```

***
####



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
####

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
