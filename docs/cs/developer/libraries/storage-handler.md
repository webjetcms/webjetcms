# Knihovna 'StorageHandler'

pro správu a používání úložiště

***

**Závislosti**

- [Nástroje](tools.md)
- [Obchod](store.md)

***

`StorageHandler` umožňuje manipulovat s `window.localStorage` a `window.sessionStorage` na úrovni vnořených objektů, kde každý vytvořený hlavní klíč zadaný parametrem [setStoreName()](#setstorename) obsahuje objekt, ve kterém můžeme snadno definovat [setStorageItem()](#setstorageitem) a vyberte [getStorageItem()](#getstorageitem) data na základě dílčího klíče.

Rozšiřuje třídu [Obchod](store.md).

```javascript
import Store from '../store/store';

class StorageHandler extends Store {}
```

### Popis operace:

V systému **WebJET** Je `StorageHandler` používá se jako rozhraní v rozšiřujících třídách a knihovnách pro ukládání dat pod klíčem používané třídy nebo knihovny.

Příklady takových použití jsou např.. [Translator](translator.md) nebo [CellVisibilityService](cell-visibility-service.md)

Metody [getStorageItem()](#getstorageitem) a [setStorageItem()](#setstorageitem) nepracují přímo s místním klíčem/klíčem relace úložiště, ale s klíčem objektu, který je pod hlavním klíčem zadaným příkazem [setStoreName()](#setstorename) při vytváření instance třídy. Data jsou uložena jako objekt JSON. Proto se při každé změně objektu přepíše hodnota hlavního klíče novým záznamem celého objektu JSON.

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

## Seznam rozhraní API

**(Kliknutím zobrazíte detail funkce)**

| Metody | Poznámka
| ---------------------------------------------------- | ------------------------------------------------------------------------------------- |
| [setStoreName()](#setstorename)                      |
| [setSessionStorage()](store.md?id=setsessionstorage) | Zděděná metoda ze třídy [Obchod](store.md) [přesměruje na soubor [store.md](store.md) ] |
| [setLocalStorage()](store.md?id=setlocalstorage)     | Zděděná metoda ze třídy [Obchod](store.md) [přesměruje na soubor [store.md](store.md) ] |
| [getStorageItem()](#getstorageitem)                  |
| [setStorageItem()](#setstorageitem)                  |
| [removeStorageItem()](#odstranit-skladovací-položku)            |
| [clearData()](#cleardata)                            |
| [destroy()](#zničit)                                |
| [storeExist()](#storeexist)                          |

Podrobnější rozhraní API najdete v [Úložiště GIT](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/storage-handler#storagehandler-kni%C5%BEnica-na-spr%C3%A1vu-a-pou%C5%BE%C3%ADvanie-storage)

### Podrobný popis funkcí

#### setStoreName()

Nastaví třídu na nový název klíče, do kterého budou data uložena. Může být použita za běhu, a mít tak pouze jednu instanci. Metoda vrací třídu `StorageHandler`

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

Vytvoří klíč zadaný v úložišti a uloží pod něj zadanou hodnotu.

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

Odebere položku na základě klíče. Metoda může mít vypnutou kontrolu caseSensitive.

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

Odstraní aktuálně používané úložiště a ponechá ho prázdné - null. Hlavní klíč však zůstane zachován.

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

Testuje, zda zadané úložiště existuje.

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
