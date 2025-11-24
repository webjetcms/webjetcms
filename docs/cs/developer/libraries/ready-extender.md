# Knihovna 'Ready'

pro správu a spouštění callbacků.

***

**Dependencies (závislosti)**

- [Tools](tools.md)

***

### Popis fungování:

`ReadyExtender` slouží jako virtuální úložiště resp. collector funkcí, sesbíraných při načítání webu a tato kolekce funkcí se na konci renderu manuálně provede v pořadí v jakém se nachází v úložišti (v pořadí v jakém byly funkce přidány).

**Pořadí přidané funkce lze také určit při jejím přidávání.**

Na konci načítání webu, například. v `document.ready` zavoláme metodu [fire()](#fire) a tím se spustí vykonávání sesbíraných funkcí.

Můžeme si tedy určit, kdy se má, která funkce spustit a odpadá nám potřeba sledovat pořadí provádění klasických `document.ready` funkcí a stačí nám pouze jedna funkce `document.ready` pro provedení [fire()](#fire)

## Vytvoření instance:

**WebJET** inicializuje knihovnu v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)

```javascript
import Ready from './libs/ready-extender/ready-extender';

window.domReady = new Ready();
```

**Použití:**

```javascript
window.domReady.add(() => {
    // Toto je callback funkcia A
    // Ktorá bude pridaná automaticky ako prvá v poradí, pretože ešte v zozname nemáme nič.
});

window.domReady.add(() => {
    // Toto je callback funkcia B
    // Ktorá bude pridaná pod číslom 5 a bude spustená ako tretia v poradí.
}, 5);

window.domReady.add(() => {
    // Toto je callback funkcia C
    // Ktorá bude pridaná ako druhá v poradí.
}, 2);

window.domReady.add(() => {
    // Toto je callback funkcia D
    // Ktorá bude pridaná ako štvrtá (posledná) v poradí,
    // pretože číslo 2 už v zozname máme a automaticky ju hodí na koniec zoznamu.
}, 2);


// Spustíme vykonanie callbackov
// Spustia sa v poradí A, C, B, D
window.domReady.fire();
```

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

| Metody | Gettery |
| ------------------- | --------------------- |
| [add()](#add)       | [listName](#listName) |
| [remove()](#remove) | [dopis](#dopis)         |
| [fire()](#fire)     |

***

### Detailní popis funkcí

#### add()

Přidá do seznamu novou funkci.

**Druhý vstupní argument** určuje pořadí přidávané funkce. Čísluje se od 1. Pokud není zadáno, funkce se přidá na konec seznamu. Hodnoty pořadí >= 900 se neberou v úvahu, předpokládá se, že ty musí být na vždy konci.

**Třetí vstupní argument** nastaven na TRUE zajistí, že se při existenci funkce na dané pozici (`orderId`), na sílu přepíše funkce a předešlá se přidá na nejbližší volné místo.

```javascript
/**
 * @description Pridá do zoznamu nový callback.
 * @param {function} callback
 * @param {number} [orderId] Určuje poradie callbakov v akom sa majú spúšťať. Čísluje sa od 1. Ak nie je zadané, callback sa pridá na koniec zoznamu.
 * @param {boolean} [rewriteOrder] Pri nastavení TRUE sa pri existencii callbacku na danej pozícii na silu prepíše callback a predošlý sa pridá na najbližšie voľné miesto.
 * @returns {void}
 * @public
 */
add(callback, orderId = 0, rewriteOrder = false);
```

***

#### remove()

Odstraní callback ze seznamu na základě čísla jeho pořadí.

```javascript
/**
 * @description Odstráni callback zo zoznamu na základe čísla jeho poradia.
 * @param {number} orderId Číslo poradia callbacku
 * @returns {void}
 * @public
 */
remove(orderId);
```

***

#### fire()

Spustí provedení všech callbacků v seznamu, zařadím podle pořadí.

```javascript
/**
 * @description Spustí vykonanie všetkých callbackov v zozname, zaradom podľa poradia.
 * @returns {void}
 * @public
 */
fire();
```

***

#### listName

Získá vygenerovaný název úložiště

```javascript
/**
 * @description Získa vygenerovaný názov úložiska
 * @returns {string|void}
 * @public
 * @getter
 * @readonly
 */

// GETTER
const IdReadyCallbackListu = window.domReady.listName;
```

***

#### dopis

Vrátí kompletní seznam všech přidaných funkcí is jejich pořadovým číslem

```javascript
/**
 * @description Vráti kompletný zoznam všetkých pridaných callbackov aj s ich poradovým číslom
 * @returns {Object}
 * @public
 * @getter
 * @readonly
 */

// GETTER
const zoznamCallbackov = window.domReady.list;
```

***
