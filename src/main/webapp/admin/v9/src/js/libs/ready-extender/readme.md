# 'Ready' knižnica pre správu a spúštanie callbackov. 

```javascript
import Ready from './libs/ready-extender/ready-extender';

const domReady = new Ready();
```

**Použitie:**
```javascript
domReady.add(() => {
    // Toto je callback funkcia A
    // Ktorá bude pridaná automaticky ako prvá v poradí, pretože ešte v zozname nemáme nič.
});

domReady.add(() => {
    // Toto je callback funkcia B
    // Ktorá bude pridaná pod číslom 5 a bude spustená ako tretia v poradí.
}, 5);

domReady.add(() => {
    // Toto je callback funkcia C
    // Ktorá bude pridaná ako druhá v poradí.
}, 2);

domReady.add(() => {
    // Toto je callback funkcia D
    // Ktorá bude pridaná ako štvrtá (posledná) v poradí, 
    // pretože číslo 2 už v zozname máme a automaticky ju hodí na koniec zoznamu.
}, 2);


// Spustíme vykonanie callbackov
// Spustia sa v poradí A, C, B, D
domReady.fire();
```

### Zoznam API:
###### (Kliknutím zobrazíš detail pre funkciu)
+ Metódy
    - [add()](#readme-add)
    - [remove()](#readme-remove)
    - [fire()](#readme-fire)
+ GETTER / SETTER
    - [listStore](#readme-listStore)
    - [listName](#readme-listName)
    - [list](#readme-list)

---

## Detailný popis funkcií

#### add() <a name="readme-add"></a>
Text
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
#### remove() <a name="readme-remove"></a>
Text
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
#### fire() <a name="readme-fire"></a>
Text
```javascript
/**
 * @description Spustí vykonanie všetkých callbackov v zozname, zaradom podľa poradia.
 * @returns {void}
 * @public
 */
fire();
```

---
#### listStore <a name="readme-listStore"></a>
Text
```javascript
/**
 * @description
 * @param {Object|{}} value
 * @returns {void}
 * @public
 */

//SETTER

domReady.listStore = {};
```

---
#### listName <a name="readme-listName"></a>
Text
```javascript
/**
 * @description
 * @returns {string|void}
 * @public
 * @readonly
 */

// GETTER
const názov = domReady.listName;
```

---
#### list <a name="readme-list"></a>
Text
```javascript
/**
 * @description
 * @returns {Object}
 * @public
 * @readonly
 */

// GETTER
const zoznamCallbackov = domReady.list;
```

---