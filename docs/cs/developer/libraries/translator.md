# Knihovna 'Translator'

***

**Dependencies (závislosti)**

- [StorageHandler](storage-handler.md)
- [Tools](tools.md)
- [jQuery Ajax](https://api.jquery.com/jquery.ajax/), *[Přesměruje na oficiální dokumentaci]*

***

Knihovna slouží k implementaci jazykových překladů v systému **WebJET**. Použít ji lze jak v Javascript souborech, tak přímo ve vyrenderovaném HTML dokumentu, kde jsou texty vkládány serverově.

### Popis fungování:

Při vytvoření instance třídy `Translator` se v jejím konstruktoru vytvoří [StorageHandler](storage-handler.md), kterému je nastaven klíč úložiště. O této skutečnosti nás v DEV prostředí upozorní v js konzole hláška `Store name was set successfully to: translate` kde `translate` je název úložiště, který je nastaven přímo v konstruktoru třídy `Translator`.

Následně instance čeká na zavolání metod [load()](#load), nebo [onBeforeLoad()](#onbeforeload), [onAfterLoad()](#onafterload)

Metoda [load()](#load) zkontroluje existenci lokálních (již existujících) překladů:
- Pokud **neexistují**, tak provede API request a získané překlady uloží do úložiště a instance je ve stavu `DONE & READY`.
- Pokud **existují**, tak zkontroluje, zda jsou uložené překlady aktuální na základě porovnání data poslední aktualizace uloženém v úložišti a data, které máme přístupné ve `window` v proměnné `window.propertiesLastModified`
  - Jsou-li data **rozdílně**, tak se provede API request a získané překlady uloží do úložiště a instance je ve stavu `DONE & READY`.
  - Jsou-li data **stejné**, tak se neprovede žádná další akce a instance je ve stavu `DONE & READY`.

Metody [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#onafterload) přidají námi zadefinovanou funkci do callback stacku, který se provede tak, jak je popsáno v detailu metod [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#onafterload).

Metoda [translate()](#translate) převezme vstupní argument (překladový klíč) a na základě něj se pokusí vyhledat v úložišti překladů konkrétní překlad.

- Pokud překlad **existuje**, tak jej metoda vrátí, jak svůj result.
- Pokud překlad **neexistuje**, nebo byl vložen nevalidní překladový klíč, tak metoda vrátí ten vstupní klíč, jako svůj result.

## Vytvoření instance:

**WebJET** inicializuje knihovnu v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js).

```javascript
/* webjetTranslationService */
import {Translator} from "./libs/translator/translator";

window.webjetTranslationService = new Translator();
```

Následně ji implementuje v souboru [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js), kde pomocí funkce [load()](#load) načteme překlady ze serveru pokud ještě neexistují lokálně nebo pokud existuje update.

Funkce [load()](#load) si sama zkontroluje aktuálnost stávajících překladů a rozhodne se, zda má provést request na API.

```javascript
// Spustenie načítania prekladov
window.webjetTranslationService.onAfterLoad(() => {
    // ...
}, true).load();
```

## Překlady v kódu:

### Inline (přímo v javascript souborech)

V Javascript souborech, používáme funkci `WJ.translate()`, která implementuje metodu [translate()](#translate), takže metodu [translate()](#translate) z knihovny nepoužíváme nikdy přímo pokud se nejedná o implementace v jiných třídách, které budou využívat API z `Translate`.

**Ve WebJET souborech používáme:**

```javascript
const preklad = WJ.translate('translation.key'); // V premennej preklad máme preložený text alebo ak text neeixstuje, tak samotnú hodnotu kľúča
```

### Pro celou HTML stránku

**Implementace ve WebJET CMS nevyžaduje tuto funkcionalitu, protože se všechny překlady řeší již při renderu stránky.**

Pro překlady přímo ve vyrenderované stránce se používají html data atributy **data-translator**, `data-translator="prekladový.kľúč"`.

Data atribut může být umístěn na jakoukoli HTML značku ale musí být bezprostředně na značně, která obsahuje překládaný text. Pokud je umístěn na HTML značce, která obsahuje další HTML kód, musí být tento HTML kód obsažen i v samotném překladu, jinak bude stávající HTML nahrazen hodnotou z překladu.

!>**Upozornění:**, *Funkce při nastavování překládaného textu odstraňuje všechny existující eventy, které jsou na daném elementu a na jeho potomcích.*

```html
<span data-translator="components.datatables.data.insertDate">Dátum vloženia</span>
```

Následně je třeba přidat do HTML stránky volání překladové funkce.

```javascript
$(document).ready(() => {
    window.webjetTranslationService.onBeforeLoad(instance => {
        instance.htmlTranslate();
    }).load();
});
```

nebo umístit na konec stránky

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

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

Metody:
- [load()](#load)
- [onBeforeLoad()](#onbeforeload)
- [onAfterLoad()](#onafterload)
- [translate()](#translate)
- [htmlTranslate()](#htmltranslate)

Gettery:
- [language](#language)
- [date](#date)

Settery:
- [urlLoad](#urlload)
- [urlUpdate](#urlupdate)

Podrobnější API se nachází v [GIT repozitáři](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/translator#kni%C5%BEnica-translator)

***

### Detailní popis funkcí

#### load()

Zavoláním se provede request na server API.

*URL adresy lze měnit pomocí [urlLoad](#urlload) a [urlUpdate](#urlupdate).*

```javascript
/**
 * @description Spustí načítanie prekladov
 * @returns {void}
 * @public
 */
window.webjetTranslationService.load();
```

#### onBeforeLoad()

Metoda zajišťuje přidávání callbaků, které se provedou bezprostředně po zavolání metody [load()](#load).

Callbacky můžeme přidávat libovolně, ale vždy jen před voláním výše zmíněné metody.

Volitelný druhý atribut `rewrite` zajistí v případě, je-li nastaven na `TRUE`, že se provede pouze naposledy přidaný callback.

Metoda vrací instanci třídy `Translator` a tak je možné za ní pomocí `DOT operátora` volat další metody.

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

Metoda zajišťuje přidávání callbaků, které se provedou po úspěšném přijetí dat ze server API po zavolání metody [load()](#load).

Callbacky můžeme přidávat libovolně, ale vždy jen před voláním výše zmíněné metody.

Volitelný druhý atribut `rewrite` zajistí v případě, je-li nastaven na `TRUE`, že se provede pouze naposledy přidaný callback.

Metoda vrací instanci třídy `Translator` a tak je možné za ní pomocí `DOT operátora` volat další metody.

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

Zavoláním této metody získáme překlad v aktuálně používaném jazyce na základě klíče.

Pokud překlad neexistuje nebo byl vložen neplatný klíč, metoda vrátí hodnotu tohoto klíče.

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

Zavoláním této metody na konci stránky nebo pomocí `document.ready` neboli po dokončení renderu webu zajistíme, že všem elementům na stránce, které mají zadefinovaný data atribut `data-translator` budou změněny textové hodnoty `innerHTML` na základě klíče uvedeného v data atributu `data-translator`.

!>**Upozornění:**, *Funkce při nastavování překládaného textu odstraňuje všechny existující eventy, které jsou na daném elementu a na jeho potomcích.*

[Klikni pro více informací o použití](#pro-celou-html-stránku)

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

Vrací používaný jazyk aplikace přímo ze Store.

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

Vrátí datum poslední aktualizace ve formátu timestamp (milisekundy).

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

Zadání nové API adresy pro načtení dostupných překladů. Adresu je třeba měnit ještě před zavoláním metody [load()](#load)

```javascript
/**
 * @description Zadanie novej API adresy pre načítanie dostupných prekladov.
 * @param {string} value
 * @public
 * @setter
 */
window.webjetTranslationService.urlLoad = 'Nová/url/adresa/pre/ziskanie/prekladov';
```
