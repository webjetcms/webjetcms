# Knižnica 'Translator'
###### Dependencies (závislosti)
- [StorageHandler](../storage-handler)
- [jQuery Ajax](https://api.jquery.com/jquery.ajax/) _[Presmeruje na oficiálnu dokumentáciu]_

---

Knižnica slúži na implementáciu jazykových prekladov v systéme **WebJet**.
Použiť ju je možné ako v Javascript súboroch, 
tak aj priamo vo vyrenderovanom HTML dokumente, 
kde sú texty vkladané serverovo.

Použitie:

###### Vytvorenie inštancie:
```javascript
import {Translator} from 'translator/translator';

const translationService = new Translator();
// Spustenie načítania prekladov
translationService.load();
```
#### Preklady v kóde:
###### Inline (priamo v javascript súboroch)
```javascript
const preklad = translationService.translate('translation.key');
```
###### HTML stránka
```javascript
$(document).ready(() => {
    translationService.onBeforeLoad(instance => {
        instance.htmlTranslate();    
    }).load();
});
```
alebo
```html
<body>
.
.
.
    <!-- Umiestniť na koniec súboru pred uzatváraciu značku </body> -->
    <script>
        translationService.onBeforeLoad(instance => {
            instance.htmlTranslate();    
        }).load();
    </script>
</body>
```
---
### Zoznam API:
###### (Kliknutím zobrazíš detail pre funkciu)
- [load()](#readme-load)
- [onBeforeLoad()](#readme-onBeforeLoad)
- [onAfterLoad()](#readme-onAfterLoad)
- [translate()](#readme-translate)
- [htmlTranslate()](#readme-htmlTranslate)
- [loadTranslations()](#readme-loadTranslations)
- [updateTranslations()](#readme-updateTranslations)
##### setters
- [urlLoad](#readme-urlLoad)
- [urlUpdate](#readme-urlUpdate)
##### getters
- [language](#readme-language)
- [date](#readme-date)
---
## Detailný popis funkcií

#### load() <a name="readme-load"></a>
Zavolaním sa vykoná request na server API. 

_URL adresy je možné meniť pomocou [urlLoad](#readme-urlLoad) a [urlUpdate](#readme-urlUpdate)._ 

Volajú sa metódy [loadTranslations()](#readme-loadTranslations) a [updateTranslations()](#readme-updateTranslations) v danom poradí. 
Pri prvon zavolaní metódy load() sa vykoná [loadTranslations()](#readme-loadTranslations) a metóda [updateTranslations()](#readme-updateTranslations) sa vykoná až pri daľšom volaní metódy load(). 
Nikdy sa nespustia súčasne.
```javascript
/**
 * @description Spustí načítanie prekladov
 * @returns {void}
 * @public
 */
translationService.load();
```
---
#### onBeforeLoad() <a name="readme-onBeforeLoad"></a>
Metóda zabezpečuje pridávanie callbakov, 
ktoré sa vykonajú bezprostredne po zavolaní metód 
[load()](#readme-load), 
[loadTranslations()](#readme-loadTranslations), 
[updateTranslations()](#readme-updateTranslations)

Callbacky môžeme pridávať lubovoľne, 
ale vždy iba pred volaním vyššie spomenutých metód.

Voliteľný druhý atribút ``rewrite`` zabezpečí v prípade, 
že je nastavený na ``TRUE``, 
že sa vykoná iba naposledy pridaný callback.

Metóda vracia inštanciu triedy ``Translator`` a tak je možné za ňou pomocou ``DOT operátora`` volať ďalšie metódy.
```javascript
/**
 * @description Pridanie akcie, ktorá sa vykoná pred načítaním dát zo servera.
 * @param {function} callback
 * @param {boolean} [rewrite] Vypne ukladanie callbackov do zoznamu a bude sa vždy spúšťať iba jeden (naposledy pridaný) callback.
 * @returns {Translator}
 * @public
 */
translationService.onBeforeLoad(callback, rewrite = false);
```
---
#### onAfterLoad() <a name="readme-onAfterLoad"></a>
Metóda zabezpečuje pridávanie callbakov, 
ktoré sa vykonajú po úspešnom prijatí dát zo server API po zavolaní metód 
[load()](#readme-load), 
[loadTranslations()](#readme-loadTranslations), 
[updateTranslations()](#readme-updateTranslations)

Callbacky môžeme pridávať lubovoľne, 
ale vždy iba pred volaním vyššie spomenutých metód.

Voliteľný druhý atribút ``rewrite`` zabezpečí v prípade, 
že je nastavený na ``TRUE``, 
že sa vykoná iba naposledy pridaný callback.

Metóda vracia inštanciu triedy ``Translator`` a tak je možné za ňou pomocou ``DOT operátora`` volať ďalšie metódy.
```javascript
/**
 * @description Pridanie akcie, ktorá sa vykoná po úspešnom načítaní dát zo servera.
 * @param {function} callback
 * @param {boolean} [rewrite] Vypne ukladanie callbackov do zoznamu a bude sa vždy spúšťať iba jeden (naposledy pridaný) callback.
 * @returns {Translator}
 * @public
 */
translationService.onAfterLoad(callback, rewrite = false);
```
---
#### translate() <a name="readme-translate"></a>
Zavolaním tejto metódy získame preklad v aktuálne používanom jazyku na základe kľúča.

Ak preklad neexistuje alebo bol vložený neplatný kľúč, metóda vráti hodnotu tohto kľúča.
```javascript
/**
 * @description Vyhľadá a vráti text v aktuálne používanom jazyku na základe translate kľúča.
 * @param {string} translationFieldName
 * @returns {string}
 * @public
 */
translationService.translate(translationFieldName);
```
---
#### htmlTranslate() <a name="readme-htmlTranslate"></a>
Zavolaním tejto metódy na konci stránky 
alebo pomocou ``document.ready`` čiže po dokončení renderu webu zabezpečíme, 
že všetkým elementom na stránke, 
ktoré majú zadefinovaný data atribút ``data-translator`` budú zmenené 
textové hodnoty ``textContent`` na základe kľúča 
uvedeného v data atribúte ``data-translator``

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
translationService.htmlTranslate(scope = document);
```
---
#### loadTranslations() <a name="readme-loadTranslations"></a>
Metóda na hrubé zavolanie server API, 
ktoré načíta všetky dostupné preklady pre aktuálne používaný jazyk 
a uloží dáta do Store.

Adresu API je možné zmeniť pomcou [urlLoad](#readme-urlLoad)
```javascript
/**
 * @description Načíta nové preklady
 * @param {function} [storeExistCallback]
 * @returns {Translator}
 * @public
 */
translationService.loadTranslations(storeExistCallback);
```
---
#### updateTranslations() <a name="readme-updateTranslations"></a>
Metóda na hrubé zavolanie server API, 
ktoré načíta všetky dostupné aktualizované preklady pre aktuálne používaný jazyk 
a uloží dáta do Store.

Adresu API je možné zmeniť pomcou [urlUpdate](#readme-urlUpdate)
```javascript
/**
 * @description Načíta aktualizácie prekladov
 * @returns {Translator}
 * @public
 */
translationService.updateTranslations();
```
---
##### setters

---
#### urlLoad <a name="readme-urlLoad"></a>
Zadanie novej API adresy pre načítanie dosputných prekladov.
Adresu je potrebné meniť ešte pred zavolaním metód 
[load()](#readme-load),
[loadTranslations()](#readme-loadTranslations).
```javascript
translationService.urlLoad = 'Nová/url/adresa/pre/ziskanie/prekladov';
```
---
#### urlUpdate <a name="readme-urlUpdate"></a>
Zadanie novej API adresy pre načítanie dosputných prekladov.
Adresu je potrebné meniť ešte pred zavolaním metód 
[load()](#readme-load),
[updateTranslations()](#readme-updateTranslations).
```javascript
translationService.urlUpdate = 'Nová/url/adresa/pre/ziskanie/aktualizácií';
```
---
##### getters

---
#### language <a name="readme-language"></a>
Vracia používaný jazyk aplikácie priamo zo Store.
```javascript
const lang = translationService.language;
```
---
#### date <a name="readme-date"></a>
Vráti dátum poslednej aktualizácie vo formáte timestamp (milisekundy).
```javascript
const date = translationService.date;
```
