# 'Translator' library

---
**Dependencies**

- [StorageHandler](storage-handler.md)
- [Tools](tools.md)
- [jQuery Ajax](https://api.jquery.com/jquery.ajax/), _[Redirects to official documentation]_
---
The library is used to implement language translations in the **WebJET** system.
It can be used in Javascript files, as well as directly in a rendered HTML document, where texts are inserted server-side.

### Description of operation:

When an instance of the `Translator` class is created, a [StorageHandler](storage-handler.md) is created in its constructor, which is set to the storage key.
In the DEV environment, we will be notified of this fact in the js console by the message `Store name was set successfully to: translate` where `translate` is the name of the repository,
which is set directly in the constructor of the `Translator` class.

Then the instance waits for the [load()](#load), or [onBeforeLoad()](#onbeforeload), [onAfterLoad()](#onafterload) methods to be called.

The [load()](#load) method checks for the existence of local (already existing) translations:
- If **they do not exist**, it executes an API request and saves the obtained translations to the repository and the instance is in the `DONE & READY` state.
- If **they exist**, it checks whether the saved translations are up to date by comparing the last update date stored in the repository with the date,
which we have access to in `window` in the variable `window.propertiesLastModified`

  - If the dates are **different**, an API request is made and the obtained translations are saved to the repository and the instance is in state `DONE & READY`.
  - If the dates are **the same**, then no further action is taken and the instance is in state `DONE & READY`.

The [onBeforeLoad()](#onbeforeload) and [onAfterLoad()](#onafterload) methods add the function we defined to the callback stack,
which is executed as described in the details of the [onBeforeLoad()](#onbeforeload) and [onAfterLoad()](#onafterload) methods.

The [translate()](#translate) method takes an input argument (translation key) and attempts to search the translation repository for a specific translation based on it.

- If the translation **exists**, the method returns it as its result.
- If the translation **does not exist**, or an invalid translation key was entered, the method returns that input key as its result.

## Creating an instance:

**WebJET** initializes the library in the [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js) file.

```javascript
/* webjetTranslationService */
import {Translator} from "./libs/translator/translator";

window.webjetTranslationService = new Translator();
```

It is then implemented in the file [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js),
where we use the function [load()](#load) to load translations from the server if they do not yet exist locally or if there is an update.

The [load()](#load) function itself checks the currentness of existing translations and decides whether to make a request to the API.

```javascript
// Spustenie načítania prekladov
window.webjetTranslationService.onAfterLoad(() => {
    // ...
}, true).load();
```

## Translations in code:

### Inline (directly in javascript files)
In Javascript files, we use the function `WJ.translate()`, which implements the [translate()](#translate) method,
so we never use the [translate()](#translate) method from the library directly unless it is implemented in other classes,
which will use the API from `Translate`.

**In WebJET files we use:**

```javascript
const preklad = WJ.translate('translation.key'); // V premennej preklad máme preložený text alebo ak text neeixstuje, tak samotnú hodnotu kľúča
```

### For the entire HTML page

**Implementation in WebJET CMS does not require this functionality, as all translations are handled during page rendering.**

For translations directly in the rendered page, the html data attributes **data-translator** are used,
`data-translator="prekladový.kľúč"`.

The data attribute can be placed on any HTML tag but must be immediately after the element that contains the translated text.
If it is placed on an HTML tag that contains additional HTML code, this HTML code must also be included in the translation itself, otherwise the existing HTML will be replaced with the value from the translation.

!>**Warning:**, _When setting the translated text, the function removes all existing events on the given element and its descendants._

```html
<span data-translator="components.datatables.data.insertDate">Dátum vloženia</span>
```

Next, you need to add a translation function call to the HTML page.

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

---
## API List

**(Click to view feature details)**

Methods:
- [load()](#load)
- [onBeforeLoad()](#onbeforeload)
- [onAfterLoad()](#onafterload)
- [translate()](#translate)
- [htmlTranslate()](#htmltranslate)

Getters:
- [language](#language)
- [date](#date)

Setters:
- [urlLoad](#urlload)
- [urlUpdate](#urlupdate)

A more detailed API can be found in the [GIT repository](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/translator#kni%C5%BEnica-translator)

---
### Detailed description of functions

#### load()

The call makes a request to the API server.

_URL addresses can be changed using [urlLoad](#urlload) and [urlUpdate](#urlupdate)._

```javascript
/**
 * @description Spustí načítanie prekladov
 * @returns {void}
 * @public
 */
window.webjetTranslationService.load();
```

#### onBeforeLoad()

The method ensures the addition of callbacks that are executed immediately after the [load()](#load) method is called.

We can add callbacks as desired, but always only before calling the above method.

The optional second attribute ``rewrite`` ensures that only the most recently added callback is executed when set to ``TRUE``.

The method returns an instance of the class ``Translator`` and so it is possible to call other methods after it using ``DOT operátora``.

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

The method ensures the addition of callbacks that are executed after successfully receiving data from the server API after calling the [load()](#load) method.

We can add callbacks as desired, but always only before calling the above method.

The optional second attribute ``rewrite`` ensures that only the most recently added callback is executed when set to ``TRUE``.

The method returns an instance of the class ``Translator`` and so it is possible to call other methods after it using ``DOT operátora``.

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

By calling this method, we get the translation in the currently used language based on the key.

If the translation does not exist or an invalid key was entered, the method returns the value of that key.

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

By calling this method at the end of the page
or using ``document.ready``, so after the website render is complete, we will ensure that
that all elements on the page that have the data attribute ``data-translator`` defined will be changed
text values ​​``innerHTML`` based on the key specified in the data attribute ``data-translator``.

!>**Warning:**, _When setting the translated text, the function removes all existing events on the given element and its descendants._

[Click for more usage info](#for-entire-html-page)

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

Returns the language used by the application directly from the Store.

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

Returns the date of the last update in timestamp format (milliseconds).

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

Enter a new API address to retrieve available translations.
The address must be changed before calling the [load()](#load) method.
```javascript
/**
 * @description Zadanie novej API adresy pre načítanie dostupných prekladov.
 * @param {string} value
 * @public
 * @setter
 */
window.webjetTranslationService.urlLoad = 'Nová/url/adresa/pre/ziskanie/prekladov';
```