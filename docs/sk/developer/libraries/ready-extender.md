# Knižnica 'Ready'
pre správu a spúšťanie callbackov.

---
**Dependencies (závislosti)**
- [Tools](tools.md)
---

### Opis fungovania:

`ReadyExtender` slúži ako virtuálne úložisko resp. collector funkcií, zozbieraných pri načítavaní webu
a táto kolekcia funkcií sa na konci renderu manuálne vykoná v poradí v akom sa nachádza v úložisku (v poradí v akom boli funkcie pridané).

**Poradie pridanej funkcie, je možné aj určiť pri jej pridávaní.**

Na konci načítavania webu, napr. v `document.ready` zavoláme metódu [fire()](#fire) a tým sa spustí vykonávanie zozbieraných funkcií.

Môžeme si teda určiť, kedy sa má, ktorá funkcia spustiť a odpadá nám potreba sledovať poradie vykonávania klasických `document.ready` funkcií
a stačí nám iba jedna funkcia `document.ready` pre vykonanie [fire()](#fire)

## Vytvorenie inštancie:
**WebJET** inicializuje knižnicu v súbore [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)
```javascript
import Ready from './libs/ready-extender/ready-extender';

window.domReady = new Ready();
```

**Použitie:**
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

## Zoznam API
**(Kliknutím zobrazíš detail pre funkciu)**

| Metódy                | Gettery               |
| -----------           | -----------           |
| [add()](#add)         | [listName](#listName) |
| [remove()](#remove)   | [list](#list)         |
| [fire()](#fire)       |

---

### Detailný popis funkcií

#### add()
Pridá do zoznamu novú funkciu.

**Druhý vstupný argument** určuje poradie pridávanej funkcie. Čísluje sa od 1. Ak nie je zadané, funkcia sa pridá na koniec zoznamu.

**Tretí vstupný argument** nastavený na TRUE zabezpečí, že sa pri existencii funkcie na danej pozícii (`orderId`), na silu prepíše funkcia a predošlá sa pridá na najbližšie voľné miesto.
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

---
#### remove()
Odstráni callback zo zoznamu na základe čísla jeho poradia.
```javascript
/**
 * @description Odstráni callback zo zoznamu na základe čísla jeho poradia.
 * @param {number} orderId Číslo poradia callbacku
 * @returns {void}
 * @public
 */
remove(orderId);
```

---
#### fire()
Spustí vykonanie všetkých callbackov v zozname, zaradom podľa poradia.
```javascript
/**
 * @description Spustí vykonanie všetkých callbackov v zozname, zaradom podľa poradia.
 * @returns {void}
 * @public
 */
fire();
```

---
#### listName
Získa vygenerovaný názov úložiska
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

---
#### list
Vráti kompletný zoznam všetkých pridaných funkcií aj s ich poradovým číslom
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

---