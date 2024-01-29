# Knižnica 'Translator'

---
**Dependencies (závislosti)**
- [StorageHandler](storage-handler.md)
- [Tools](tools.md)
- [jQuery Ajax](https://api.jquery.com/jquery.ajax/) _[Presmeruje na oficiálnu dokumentáciu]_
---
Knižnica slúži na implementáciu jazykových prekladov v systéme **WebJET**.
Použiť ju je možné v Javascript súboroch, tak aj priamo vo vyrenderovanom HTML dokumente, kde sú texty vkladané serverovo.

### Opis fungovania:

Pri vytvorení inštancie triedy `Translator` sa v jej konštruktore vytvorí [StorageHandler](storage-handler.md), ktorému je nastavený kľúč úložiska.
O tejto skutočnosti nás v DEV prostredí upozorní v js konzole hláška `Store name was set successfully to: translate` kde `translate` je názov úložiska,
ktorý je nastavený priamo v konštruktore triedy `Translator`.

Následne inštancia čaká na zavolanie metód [load()](#load), alebo [onBeforeLoad()](#onbeforeload), [onAfterLoad()](#onafterload)

Metóda [load()](#load) skontroluje existenciu lokálnych (už existujúcich) prekladov:
1. Ak **neexistujú**, tak vykoná API request a získané preklady uloží do úložiska a inštancia je v stave `DONE & READY`.
2. Ak **existujú**, tak skontroluje, či sú uložené preklady aktuálne na základe porovnania dátumu poslednej aktualizácie uloženom v úložisku a dátumu,
ktorý máme prístupný vo `window` v premennej `window.propertiesLastModified`
    - Ak sú dátumy **rozdielne**, tak sa vykoná API request a získané preklady uloží do úložiska a inštancia je v stave `DONE & READY`.
    - Ak sú dátumy **rovnaké**, tak sa nevykoná žiadna ďalšia akcia a inštancia je v stave `DONE & READY`.


Metódy [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#onafterload) pridajú nami zadefinovanú funkciu do callback stacku,
ktorý sa vykoná tak, ako je popísané v detaile metód [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#onafterload).

Metóda [translate()](#translate) prevezme vstupný argument (prekladový kľúč) a na základe neho sa pokúsi vyhľadať v úložisku prekladov konkrétny preklad.
1. Ak preklad **existuje**, tak ho metóda vráti, ako svoj result.
2. Ak preklad **neexistuje**, alebo bol vložený nevalidný prekladový kľúč, tak metóda vráti ten vstupný kľúč, ako svoj result.

## Vytvorenie inštancie:

**WebJET** inicializuje knižnicu v súbore [app.js](https://gitlab.web.iway.local/webjet/webjet8v9/-/blob/master/src/main/webapp/admin/v9/src/js/app.js)

```javascript
/* webjetTranslationService */
import {Translator} from "./libs/translator/translator";

window.webjetTranslationService = new Translator();
```

následne ju implementuje v súbore [app-init.js](https://gitlab.web.iway.local/webjet/webjet8v9/-/blob/master/src/main/webapp/admin/v9/src/js/app-init.js),
kde pomocou funkcie [load()](#load) načítame preklady zo servera ak ešte neexistujú lokálne alebo ak existuje update.

Funkcia [load()](#load) si sama skontroluje aktuálnosť existujúcich prekladov a rozhodne sa, či má vykonať request na API.
```javascript
// Spustenie načítania prekladov
window.webjetTranslationService.onAfterLoad(() => {
    // ...
}, true).load();
```

## Preklady v kóde:
### Inline (priamo v javascript súboroch)
V Javascript súboroch, používame funkciu `WJ.translate()`, ktorá implementuje metódu [translate()](#translate),
takže metódu [translate()](#translate) z knižnice nepoužívame nikdy priamo ak sa nejedná o implementácie v iných triedach,
ktoré budú využívať API z `Translate`.

**Vo WebJET súboroch používame:**
```javascript
const preklad = WJ.translate('translation.key'); // V premennej preklad máme preložený text alebo ak text neeixstuje, tak samotnú hodnotu kľúča
```

### Pre celú HTML stránku
**Implementácia vo WebJET CMS nevyžaduje túto funkcionalitu, pretože sa všetky preklady riešia už pri rendere stránky.**

Pre preklady priamo vo vyrenderovanej stránke sa používajú html data atribúty **data-translator**
`data-translator="prekladový.kľúč"`

data atribút môže byť umiestnený na akúkoľvek HTML značku ale musí byť bezprostredne na značne, ktorá obsahuje prekladaný text.
Ak je umiestnený na HTML značke, ktorá obsahuje ďalší HTML kód, musí byť tento HTML kód obsiahnutý aj v samotnom preklade, inak bude stávajúci HTML nahradený hodnotou z prekladu.

**Upozornenie:** _Funkcia pri nastavovaní prekladaného textu odstraňuje všetky existujúce eventy, ktoré sú na danom elemente a na jeho potomkoch._
```html
<span data-translator="components.datatables.data.insertDate">Dátum vloženia</span>
```
Následne je potrebné pridať do HTML stránky volanie prekladovej funkcie.
```javascript
$(document).ready(() => {
    window.webjetTranslationService.onBeforeLoad(instance => {
        instance.htmlTranslate();
    }).load();
});
```
alebo umiestniť na koniec stránky
```html
<body>
.
.
.
    <!-- Umiestniť na koniec súboru pred uzatváraciu značku </body> -->
    <script>
        window.webjetTranslationService.onBeforeLoad(instance => {
            instance.htmlTranslate();
        }).load();
    </script>
</body>
```
---
## Zoznam API
**(Kliknutím zobrazíš detail pre funkciu)**

| Metódy                            | Gettery               | Settery                   |
| -----------                       | -----------           | -----------               |
| [load()](#load)                   | [language](#language) | [urlLoad](#urlload)       |
| [onBeforeLoad()](#onbeforeload)   | [date](#date)         | [urlUpdate](#urlupdate)   |
| [onAfterLoad()](#onafterload)     |
| [translate()](#translate)         |
| [htmlTranslate()](#htmltranslate) |

Podrobnejšie API sa nachádza v [GIT repozitári](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/translator#kni%C5%BEnica-translator)

---
### Detailný popis funkcií

#### load()
Zavolaním sa vykoná request na server API.

_URL adresy je možné meniť pomocou [urlLoad](#urlload) a [urlUpdate](#urlupdate)._

```javascript
/**
 * @description Spustí načítanie prekladov
 * @returns {void}
 * @public
 */
window.webjetTranslationService.load();
```
---
#### onBeforeLoad()
Metóda zabezpečuje pridávanie callbakov,
ktoré sa vykonajú bezprostredne po zavolaní metódy [load()](#load)

Callbacky môžeme pridávať ľubovoľne, ale vždy iba pred volaním vyššie spomenutej metódy.

Voliteľný druhý atribút ``rewrite`` zabezpečí v prípade, ak je nastavený na ``TRUE``, že sa vykoná iba naposledy pridaný callback.

Metóda vracia inštanciu triedy ``Translator`` a tak je možné za ňou pomocou ``DOT operátora`` volať ďalšie metódy.
```javascript
/**
 * @description Pridanie akcie, ktorá sa vykoná pred načítaním dát zo servera.
 * @param {function} callback
 * @param {boolean} [rewrite] Vypne ukladanie callbackov do zoznamu a bude sa vždy spúšťať iba jeden (naposledy pridaný) callback.
 * @returns {Translator}
 * @public
 */
window.webjetTranslationService.onBeforeLoad(callback, rewrite = false);
```
---
#### onAfterLoad()
Metóda zabezpečuje pridávanie callbakov,
ktoré sa vykonajú po úspešnom prijatí dát zo server API po zavolaní metódy [load()](#load)

Callbacky môžeme pridávať ľubovolne, ale vždy iba pred volaním vyššie spomenutej metódy.

Voliteľný druhý atribút ``rewrite`` zabezpečí v prípade, ak je nastavený na ``TRUE``, že sa vykoná iba naposledy pridaný callback.

Metóda vracia inštanciu triedy ``Translator`` a tak je možné za ňou pomocou ``DOT operátora`` volať ďalšie metódy.
```javascript
/**
 * @description Pridanie akcie, ktorá sa vykoná po úspešnom načítaní dát zo servera.
 * @param {function} callback
 * @param {boolean} [rewrite] Vypne ukladanie callbackov do zoznamu a bude sa vždy spúšťať iba jeden (naposledy pridaný) callback.
 * @returns {Translator}
 * @public
 */
window.webjetTranslationService.onAfterLoad(callback, rewrite = false);
```
---
#### translate()
Zavolaním tejto metódy získame preklad v aktuálne používanom jazyku na základe kľúča.

Ak preklad neexistuje alebo bol vložený neplatný kľúč, metóda vráti hodnotu tohto kľúča.
```javascript
/**
 * @description Vyhľadá a vráti text v aktuálne používanom jazyku na základe translate kľúča.
 * @param {string} translationFieldName
 * @returns {string}
 * @public
 */
window.webjetTranslationService.translate(translationFieldName);
```
---
#### htmlTranslate()
Zavolaním tejto metódy na konci stránky
alebo pomocou ``document.ready`` čiže po dokončení renderu webu zabezpečíme,
že všetkým elementom na stránke, ktoré majú zadefinovaný data atribút ``data-translator`` budú zmenené
textové hodnoty ``innerHTML`` na základe kľúča uvedeného v data atribúte ``data-translator``

**Upozornenie:** _Funkcia pri nastavovaní prekladaného textu odstraňuje všetky existujúce eventy, ktoré sú na danom elemente a na jeho potomkoch._

[Klikni pre viac info o použití](#pre-celú-html-stránku)

```html
<span data-translator="components.datatables.data.insertDate">
    Dátum vloženia
</span>
```

```javascript
/**
 * @description Nájde všetky elementy na aktuálnej stránke s atribútom pre preklad textu v elemente
 *              a nahradí aktuálny text prekladom.
 * @notice Metódu je pre správne fungovanie zavolať až po úplnom načítaní stránky - document.onLoad
 * @example ``<span data-translator="components.datatables.data.insertDate">Dátum vloženia</span>``
 * @param {Document|HTMLElement|Element} [scope]
 * @returns {Translator}
 * @public
 */
window.webjetTranslationService.htmlTranslate(scope = document);
```
---
**getters**

#### language
Vracia používaný jazyk aplikácie priamo zo Store.
```javascript
/**
 * @description Vráti aktuálne používaný jazyk.
 * @returns {string}
 * @public
 * @getter
 */
const lang = window.webjetTranslationService.language;
```
---
#### date
Vráti dátum poslednej aktualizácie vo formáte timestamp (milisekundy).
```javascript
/**
 * @description Vráti uložený timestamp poslednej aktualizácie. Ak neexistuje, tak vráti aktuálny.
 * @returns {number}
 * @public
 * @getter
 */
const date = window.webjetTranslationService.date;
```
---
**setters**

#### urlLoad
Zadanie novej API adresy pre načítanie dostupných prekladov.
Adresu je potrebné meniť ešte pred zavolaním metódy [load()](#load)
```javascript
/**
 * @description Zadanie novej API adresy pre načítanie dostupných prekladov.
 * @param {string} value
 * @public
 * @setter
 */
window.webjetTranslationService.urlLoad = 'Nová/url/adresa/pre/ziskanie/prekladov';
```
---
#### urlUpdate
Zadanie novej API adresy pre načítanie dostupných prekladov.
Adresu je potrebné meniť ešte pred zavolaním metódy [load()](#load)
```javascript
/**
 * @description Zadanie novej API adresy pre aktualizáciu dostupných prekladov.
 * @param {string} value
 * @public
 * @setter
 */
window.webjetTranslationService.urlUpdate = 'Nová/url/adresa/pre/ziskanie/aktualizácií';
```
