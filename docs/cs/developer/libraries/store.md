# `Store` rozšiřující třída Storage `localStorage, sessionStorage`

Třída `Store` upravuje funkcionalitu nativního
[Storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage). _[Přesměruje na oficiální dokumentaci]_

### Popis fungování:

Jedná se o wrapper, který umožňuje přímo ukládat do `Storage` data různých typů `[number, string, object, array, boolean, null, undefined, ...]` s ošetřením proti nežádoucímu padnutí aplikace při nesprávném vstupu.
Vstupy jsou automaticky konvertovány do formátu `JSON`.

!>**Upozornění:** Nelze ukládat `HTMLElementy`, `NodeListy` a `funkcie`.

V systému **WebJET** se nepoužívá globálně. Je třeba si instanci této třídu vždy vytvořit v rámci své nové třídy nebo knihovny.

## Vytvoření instance:

```javascript
import Store from './store';

// Defaultne sa pracuje s localStorage
const store = new Store();
```
**Použití:**

```javascript
store.setItem('key', 'value');

const valueVar = store.getItem('key');
```

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

| Metody                                    | Poznámka                                                                                                          | Gettery               |
| -----------                               | -----------                                                                                                       | -----------           |
| [setLocalStorage()](#setlocalstorage)     | [Oficiální dokumentace - localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)     | [storage](#storage)   |
| [setSessionStorage()](#setsessionstorage) | [Oficiální dokumentace - sessionStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage) |
| [getItem()](#getitem)                     |
| [setItem()](#setitem)                     |
| [removeItem()](#removeitem)               |
| [clear()](#clear)                         |
| [key()](#key)                             |
| [getKeys()](#getkeys)                     |
| [itemExist()](#itemexist)                 |

### Detailní popis funkcí

#### getItem()

[getItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem). _[Přesměruje na oficiální dokumentaci]_

**Popis rozšíření:** Výstup této metody bude automaticky konvertován z formátu `JSON` do původního vstupního formátu, takže odpadá nutnost výstupy přetypovat zpět do původní formy.
Pokud neexistuje zadaný klíč nebo pod zadaným klíčem neexistují data, metoda vrátí `null`

---
#### setItem()

[setItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/setItem). _[Přesměruje na oficiální dokumentaci]_

**Popis rozšíření:** Vstupní data této metody budou automaticky konvertována do formátu `JSON`. Pokud je vstup nevalidní, bude uložena hodnota `null`

---
#### removeItem()

[removeItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/removeItem). _[Přesměruje na oficiální dokumentaci]_

---
#### clear()

[clear()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/clear). _[Přesměruje na oficiální dokumentaci]_

---
#### key()

[key()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/key). _[Přesměruje na oficiální dokumentaci]_

---

#### setLocalStorage()

Nastaví typ Storage na **localStorage**.
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

Nastaví typ Storage na **sessionStorage**.
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

Vrátí seznam (array), všech klíčů v aktuálním Storage.
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

Ověří, zda v Storage existuje zadaný klíč.
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