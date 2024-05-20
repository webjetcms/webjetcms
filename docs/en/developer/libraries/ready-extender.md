# Library 'Ready'

for managing and triggering callbacks.

***

**Dependencies**

- [Tools](tools.md)
***

### Description of operation:

`ReadyExtender` serves as a virtual repository or collector of functions collected during the loading of the site and this collection of functions is manually executed at the end of the render in the order in which it is in the repository (in the order in which the functions were added).

**The order of the added function can also be specified when adding it.**
At the end of loading the site, e.g. in `document.ready` call the method [fire()](#fire) and this will start the execution of the collected functions.

So we can specify when to run which function and we don't need to keep track of the order of execution of classic `document.ready` functions and we only need one function `document.ready` for the execution of [fire()](#fire)

## Creating an instance:

**WebJET** initializes the library in the file [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)
```javascript
import Ready from "./libs/ready-extender/ready-extender";

window.domReady = new Ready();
```

**Application:**
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

## List of APIs

**(Click to see the detail for the function)**
| Methods | Gettery | | ------------------- | --------------------- | | [add()](#add)       | [listName](#listName) | | [remove()](#remove) | [list](#list)         | | [fire()](#fire)     |

***
### Detailed description of functions

#### add()

Adds a new function to the list.

**Second input argument** determines the order of the added function. It is numbered from 1. If not specified, the function is added to the end of the list.

**Third input argument** set to TRUE will ensure that when the function exists at that position (`orderId`), the function is overwritten and the previous function is added to the next available position.
```javascript
/**
 * @description Pridá do zoznamu nový callback.
 * @param {function} callback
 * @param {number} [orderId] Určuje poradie callbakov v akom sa majú spúšťať. Čísluje sa od 1. Ak nie je zadané, callback sa pridá na koniec zoznamu.
 * @param {boolean} [rewriteOrder] Pri nastavení TRUE sa pri existencii callbacku na danej pozícii na silu prepíše callback a predošlý sa pridá na najbližšie voľné miesto.
 * @returns {void}
 * @public
 */
add(callback, (orderId = 0), (rewriteOrder = false));
```

***
#### remove()

Removes a callback from the list based on its sequence number.

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

Starts execution of all callbacks in the list, in order.

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

Get the generated repository name

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
#### list

Returns a complete list of all added functions with their serial number

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
