# 'Store' rozširujúca trieda *Storage `localStorage, sessionStorage`

Trieda `Store` upravuje funkcionalitu natívneho
[Storage](https://developer.mozilla.org/en-US/docs/Web/API/Storage). _[Presmeruje na oficiálnu dokumentáciu]_

### Opis fungovania:

Jedná sa o wrapper, ktorý umožňuje priamo ukladať do `Storage` dáta rôznych typov `[number, string, object, array, boolean, null, undefined, ...]` s ošetrením proti nežiadúcemu padnutiu aplikácie pri nesprávnom vstupe.
Vstupy sú automaticky konvertované do formátu `JSON`.

**Upozornenie:** Nie je možné ukladať `HTMLElementy`, `NodeListy` a `funkcie`.

V systéme **WebJET** sa nepoužíva globálne. Je potrebné si inštanciu tejto triedu vždy vytvoriť vrámci svojej novej triedy alebo knižnice.

## Vytvorenie inštancie:
```javascript
import Store from './store';

// Defaultne sa pracuje s localStorage
const store = new Store();
```
**Použitie:**
```javascript
store.setItem('key', 'value');

const valueVar = store.getItem('key');
```

## Zoznam API
**(Kliknutím zobrazíš detail pre funkciu)**

| Metódy                                    | Poznámka                                                                                                          | Gettery               |
| -----------                               | -----------                                                                                                       | -----------           |
| [setLocalStorage()](#setlocalstorage)     | [Oficiálna dokumentácia - localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)     | [storage](#storage)   |
| [setSessionStorage()](#setsessionstorage) | [Oficiálna dokumentácia - sessionStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage) |
| [getItem()](#getitem)                     |
| [setItem()](#setitem)                     |
| [removeItem()](#removeitem)               |
| [clear()](#clear)                         |
| [key()](#key)                             |
| [getKeys()](#getkeys)                     |
| [itemExist()](#itemexist)                 |


### Detailný popis funkcií

#### getItem()
[getItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem) _[Presmeruje na oficiálnu dokumentáciu]_

**Popis rozšírenia:** Výstup tejto metódy bude automaticky konvertovaný z formátu `JSON` do pôvodného vstupného formátu, takže odpadá nutnosť výstupy pretypovať späť do pôvodnej formy.
Ak neexistuje zadaný kľúč alebo pod zadaným kľúčom neexistujú dáta, metóda vráti `null`

---
#### setItem()
[setItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/setItem) _[Presmeruje na oficiálnu dokumentáciu]_

**Popis rozšírenia:** Vstupné dáta tejto metódy budú automaticky konvertované do formátu `JSON`. Ak je vstup nevalidný, bude uložená hodnota `null`

---
#### removeItem()
[removeItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/removeItem) _[Presmeruje na oficiálnu dokumentáciu]_

---
#### clear()
[clear()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/clear) _[Presmeruje na oficiálnu dokumentáciu]_

---
#### key()
[key()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/key) _[Presmeruje na oficiálnu dokumentáciu]_

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
Vráti zoznam (array), všetkých kľúčov v aktuálnom Storage.
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
Overí, či v Storage existuje zadaný kľúč.
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