# Library 'Translator'

***

**Dependencies**

- [StorageHandler](storage-handler.md)
- [Tools](tools.md)
- [jQuery Ajax](https://api.jquery.com/jquery.ajax/), *[Redirects to official documentation]*

***

The library is used to implement language translations in the system **WebJET**. It can be used in Javascript files, as well as directly in rendered HTML document, where the texts are inserted server-side.

### Description of operation:

When creating an instance of a class `Translator` is created in its constructor [StorageHandler](storage-handler.md) to which the storage key is set. In the DEV environment, the message in the js console will alert us to this fact `Store name was set successfully to: translate` Where `translate` is the name of the repository, which is set directly in the class constructor `Translator`.

Then the instance waits for the methods to be called [load()](#Retrieved-from), or [onBeforeLoad()](#onbeforeload), [onAfterLoad()](#shefterload)

Method [load()](#Retrieved-from) checks for the existence of local (existing) translations:
- If **do not exist**, it executes the API request and saves the translations to the repository and the instance is in the state `DONE & READY`.
- If **exists**, it will check if the stored translations are up to date by comparing the last update date stored in the repository and the date we have access to in `window` in the variable `window.propertiesLastModified`
  - If the dates are **differently**, the API request is executed and the obtained translations are stored in the repository and the instance is in the state `DONE & READY`.
  - If the dates are **Same**, no further action is performed and the instance is in the state `DONE & READY`.

Methods [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#shefterload) adds the function defined by us to the callback stack, which is executed as described in the method details [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#shefterload).

Method [translate()](#translate) takes an input argument (translation key) and tries to search the translation repository for a specific translation.

- If the translation **exists**, so the method returns it as its result.
- If the translation **does not exist**, or a non-valid translation key was inserted, the method returns the input key as its result.

## Creating an instance:

**WebJET** initializes the library in the file [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js).

```javascript
/* webjetTranslationService */
import {Translator} from "./libs/translator/translator";

window.webjetTranslationService = new Translator();
```

It then implements it in a file [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js), where using the function [load()](#Retrieved-from) load translations from the server if they don't exist locally yet or if there is an update.

Feature [load()](#Retrieved-from) checks itself for the currentness of the existing translations and decides whether to make a request to the API.

```javascript
// Spustenie načítania prekladov
window.webjetTranslationService.onAfterLoad(() => {
    // ...
}, true).load();
```

## Translations in code:

### Inline (directly in javascript files)

In Javascript files, we use the function `WJ.translate()` which implements the method [translate()](#translate), so the method [translate()](#translate) from the library is never used directly unless there are implementations in other classes that will use the API from `Translate`.

**In WebJET files we use:**

```javascript
const preklad = WJ.translate('translation.key'); // V premennej preklad máme preložený text alebo ak text neeixstuje, tak samotnú hodnotu kľúča
```

### For full HTML page

**The implementation in WebJET CMS does not require this functionality because all translations are handled during page rendering.**

For translations directly in the rendered page, html data attributes are used **data-translator**, `data-translator="prekladový.kľúč"`.

The data attribute can be placed on any HTML tag but must be immediately on the tag that contains the text to be translated. If it is placed on an HTML tag that contains additional HTML code, that HTML code must also be included in the translation itself, otherwise the existing HTML will be replaced with the value from the translation.

!>**Warning:**, *When setting the translated text, the function removes all existing events that are on the element and its descendants.*

```html
<span data-translator="components.datatables.data.insertDate">Dátum vloženia</span>
```

Then you need to add the translation function call to the HTML page.

```javascript
$(document).ready(() => {
    window.webjetTranslationService.onBeforeLoad(instance => {
        instance.htmlTranslate();
    }).load();
});
```

or place at the bottom of the page

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

***

## List of APIs

**(Click to see the detail for the function)**

Methods:
- [load()](#Retrieved-from)
- [onBeforeLoad()](#onbeforeload)
- [onAfterLoad()](#shefterload)
- [translate()](#translate)
- [htmlTranslate()](#htmltranslate)

Getters:
- [language](#language)
- [date](#date)

Setters:
- [urlLoad](#urlload)
- [urlUpdate](#urlupdate)

A more detailed API can be found in [GIT repositories](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/translator#kni%C5%BEnica-translator)

***

### Detailed description of functions

#### load()

The call makes a request to the API server.

*URLs can be changed using [urlLoad](#urlload) a [urlUpdate](#urlupdate).*

```javascript
/**
 * @description Spustí načítanie prekladov
 * @returns {void}
 * @public
 */
window.webjetTranslationService.load();
```

#### onBeforeLoad()

The method provides for adding callbacks that are executed immediately after the method is called [load()](#Retrieved-from).

Callbacks can be added arbitrarily, but always only before calling the above method.

Optional second attribute `rewrite` ensure if it is set to `TRUE` that only the last added callback is executed.

The method returns an instance of the class `Translator` and so it is possible to follow it using `DOT operátora` call other methods.

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

***

#### onAfterLoad()

The method provides for the addition of callbacks that are executed upon successful receipt of data from the server API after a method call [load()](#Retrieved-from).

Callbacks can be added arbitrarily, but always only before calling the above method.

Optional second attribute `rewrite` ensure if it is set to `TRUE` that only the last added callback is executed.

The method returns an instance of the class `Translator` and so it is possible to follow it using `DOT operátora` call other methods.

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

***

#### translate()

By calling this method, we get the translation in the currently used language based on the key.

If the translation does not exist or an invalid key has been inserted, the method returns the value of this key.

```javascript
/**
 * @description Vyhľadá a vráti text v aktuálne používanom jazyku na základe translate kľúča.
 * @param {string} translationFieldName
 * @returns {string}
 * @public
 */
window.webjetTranslationService.translate(translationFieldName);
```

***

#### htmlTranslate()

By calling this method at the bottom of the page or using `document.ready` that is, after the web render is complete, we ensure that all elements on the page that have the data attribute defined `data-translator` text values will be changed `innerHTML` based on the key specified in the data attribute `data-translator`.

!>**Warning:**, *When setting the translated text, the function removes all existing events that are on the element and its descendants.*

[Click for more info on use](#for-the-whole-html-page)

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

***

**getters**

#### language

Returns the language used by the app directly from the Store.

```javascript
/**
 * @description Vráti aktuálne používaný jazyk.
 * @returns {string}
 * @public
 * @getter
 */
const lang = window.webjetTranslationService.language;
```

***

#### date

Returns the last update date in timestamp format (milliseconds).

```javascript
/**
 * @description Vráti uložený timestamp poslednej aktualizácie. Ak neexistuje, tak vráti aktuálny.
 * @returns {number}
 * @public
 * @getter
 */
const date = window.webjetTranslationService.date;
```

***

**setters**

#### urlLoad

Specifying a new API address to retrieve available translations. The address must be changed before calling the method [load()](#Retrieved-from)

```javascript
/**
 * @description Zadanie novej API adresy pre načítanie dostupných prekladov.
 * @param {string} value
 * @public
 * @setter
 */
window.webjetTranslationService.urlLoad = 'Nová/url/adresa/pre/ziskanie/prekladov';
```
