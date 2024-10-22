# Připravenost knihovny

pro správu a spouštění zpětných volání.

***

**Závislosti**

- [Nástroje](tools.md)

***

### Popis operace:

`ReadyExtender` slouží jako virtuální úložiště nebo sběrač funkcí shromážděných během načítání webu a tato sbírka funkcí je na konci vykreslování ručně spuštěna v pořadí, v jakém je v úložišti (v pořadí, v jakém byly funkce přidány).

**Při přidávání funkce lze také zadat její pořadí.**

Na konci načítání webu, např. v okně `document.ready` zavolat metodu [fire()](#požár) a tím se spustí provádění shromážděných funkcí.

Můžeme tedy určit, kdy se má která funkce spustit, a nemusíme sledovat pořadí provádění klasických funkcí. `document.ready` a potřebujeme pouze jednu funkci `document.ready` pro provedení [fire()](#požár)

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

## Seznam rozhraní API

**(Kliknutím zobrazíte detail funkce)**

| Metody | Gettery
| ------------------- | --------------------- |
| [add()](#přidat)       | [listName](#listName) |
| [remove()](#odstranit) | [list](#list)         |
| [fire()](#požár)     |

***

### Podrobný popis funkcí

#### add()

Přidá do seznamu novou funkci.

**Druhý vstupní argument** určuje pořadí přidané funkce. Je číslováno od 1. Pokud není zadáno, je funkce přidána na konec seznamu.

**Třetí vstupní argument** nastavená na TRUE zajistí, že pokud funkce na dané pozici existuje (`orderId`), funkce se přepíše a předchozí funkce se přidá na další volnou pozici.

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

Odebere zpětné volání ze seznamu na základě jeho pořadového čísla.

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

Spustí provedení všech zpětných volání v seznamu v daném pořadí.

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

Získání vygenerovaného názvu úložiště

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

Vrátí úplný seznam všech přidaných funkcí s jejich pořadovým číslem.

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
