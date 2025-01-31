# Library 'StorageHandler'

for the management and use of Storage

***

**Dependencies**

- [Tools](tools.md)
- [Store](store.md)

***

`StorageHandler` allows you to manipulate `window.localStorage` a `window.sessionStorage` at the level of nested objects, where each created master key specified by [setStoreName()](#setstorename) contains an object in which we can easily define [setStorageItem()](#setstorageitem) and choose [getStorageItem()](#getstorageitem) data based on the sub-key.

Expands the class [Store](store.md).

```javascript
import Store from '../store/store';

class StorageHandler extends Store {}
```

### Description of operation:

In the system **WebJET** Is `StorageHandler` used as an interface in extension classes and libraries to store data under the key of the class or library being used.

Examples of such uses are e.g. [Translator](translator.md) or [CellVisibilityService](cell-visibility-service.md)

Methods [getStorageItem()](#getstorageitem) a [setStorageItem()](#setstorageitem) do not work directly with the local/session Storage key, but with the object key that is under the master key specified by [setStoreName()](#setstorename) when creating a class instance. The data is stored as a JSON object. Therefore, each time a change is made to the object, the value of the master key is overwritten with a new record of the entire JSON object.

## Creating an instance:

```javascript
import {StorageHandler} from 'storage-handler/storage-handler';
```

**Application:**

```javascript
// Defaultne sa pracuje s localStorage
const store = new StorageHandler();
store.setStoreName('názov-master-kľúča');

store.setStorageItem('sub-kľúč-1', 'hodnota-1');
store.setStorageItem('sub-kľúč-2', 'hodnota-2');
```

## List of APIs

**(Click to see the detail for the function)**

| Methods | Note |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------- |
| [setStoreName()](#setstorename)                      |
| [setSessionStorage()](store.md?id=setsessionstorage) | Inherited method from class [Store](store.md) [redirects to file [store.md](store.md) ] |
| [setLocalStorage()](store.md?id=setlocalstorage)     | Inherited method from class [Store](store.md) [redirects to file [store.md](store.md) ] |
| [getStorageItem()](#getstorageitem)                  |
| [setStorageItem()](#setstorageitem)                  |
| [removeStorageItem()](#removestorageitem)            |
| [clearData()](#cleardata)                            |
| [destroy()](#destroy)                                |
| [storeExist()](#storeexist)                          |

A more detailed API can be found in [GIT repositories](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/storage-handler#storagehandler-kni%C5%BEnica-na-spr%C3%A1vu-a-pou%C5%BE%C3%ADvanie-storage)

### Detailed description of functions

#### setStoreName()

Sets the class to a new key name where the data will be stored. It can be used on the fly and thus have only one instance. The method returns the class `StorageHandler`

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

Returns a value based on the name of the specified key.

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

Creates the key specified in the repository and stores the specified value under it.

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

Removes the item based on the key. The method can have caseSensitive checking disabled.

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

Deletes the currently used repository and leaves it empty - null. However, the master key is retained.

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

Completely removes the currently used repository.

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

Tests whether the specified repository exists.

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
