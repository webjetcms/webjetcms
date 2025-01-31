# `Store` Třída rozšíření úložiště `localStorage, sessionStorage`

Třída `Store` upravuje funkčnost nativního [Úložiště](https://developer.mozilla.org/en-US/docs/Web/API/Storage). *[Přesměruje na oficiální dokumentaci]*

### Popis operace:

Jedná se o wrapper, který umožňuje přímé ukládání do adresáře `Storage` údaje různých typů `[number, string, object, array, boolean, null, undefined, ...]` s ošetřením proti nechtěnému vypadnutí aplikace při nesprávném zadání. Vstupy jsou automaticky převedeny na `JSON`.

!>**Varování:** Není možné ukládat `HTMLElementy`, `NodeListy` a `funkcie`.

V systému **WebJET** se globálně nepoužívá. Instanci této třídy byste měli vždy vytvořit v rámci nové třídy nebo knihovny.

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

## Seznam rozhraní API

**(Kliknutím zobrazíte detail funkce)**

| Metody | Poznámka | Gettery |
| ----------------------------------------- | ----------------------------------------------------------------------------------------------------------------- | ------------------- |
| [setLocalStorage()](#setlocalstorage)     | [Oficiální dokumentace - localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)     | [skladování](#skladování) |
| [setSessionStorage()](#setsessionstorage) | [Oficiální dokumentace - sessionStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage) |
| [getItem()](#getitem)                     |
| [setItem()](#setitem)                     |
| [removeItem()](#removeitem)               |
| [clear()](#přehledně)                         |
| [klíč()](#klíč)                             |
| [getKeys()](#getkeys)                     |
| [itemExist()](#itemexist)                 |

### Podrobný popis funkcí

#### getItem()

[getItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/getItem). *[Přesměruje na oficiální dokumentaci]*

**Popis rozšíření:** Výstup této metody bude automaticky převeden z formátu `JSON` do původního vstupního formátu, takže výstupy není třeba znovu zadávat do původního formátu. Pokud zadaný klíč neexistuje nebo pod zadaným klíčem nejsou žádná data, metoda vrátí hodnotu `null`

***

#### setItem()

[setItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/setItem). *[Přesměruje na oficiální dokumentaci]*

**Popis rozšíření:** Vstupní data této metody se automaticky převedou do formátu `JSON`. Pokud je vstup neplatný, uložená hodnota bude `null`

***

#### removeItem()

[removeItem()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/removeItem). *[Přesměruje na oficiální dokumentaci]*

***

#### clear()

[clear()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/clear). *[Přesměruje na oficiální dokumentaci]*

***

#### klíč()

[klíč()](https://developer.mozilla.org/en-US/docs/Web/API/Storage/key). *[Přesměruje na oficiální dokumentaci]*

***

#### setLocalStorage()

Nastaví typ úložiště na **localStorage**.

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

Nastaví typ úložiště na **sessionStorage**.

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

Vrací seznam (pole) všech klíčů v aktuálním Úložišti.

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

Ověří, zda zadaný klíč existuje v úložišti.

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

#### skladování

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
