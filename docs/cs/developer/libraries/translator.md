# Knihovna 'Translator'

***

**Závislosti**

- [StorageHandler](storage-handler.md)
- [Nástroje](tools.md)
- [jQuery Ajax](https://api.jquery.com/jquery.ajax/), *[Přesměruje na oficiální dokumentaci]*

***

Knihovna slouží k implementaci jazykových překladů v systému. **WebJET**. Lze jej použít v souborech Javascript i přímo ve vykreslovaném dokumentu HTML, kde se texty vkládají na straně serveru.

### Popis operace:

Při vytváření instance třídy `Translator` je vytvořen ve svém konstruktoru [StorageHandler](storage-handler.md) na kterou je nastaven klíč úložiště. V prostředí DEV nás na tuto skutečnost upozorní zpráva v konzoli js. `Store name was set successfully to: translate` Kde: `translate` je název úložiště, který se nastavuje přímo v konstruktoru třídy `Translator`.

Poté instance čeká na volání metod. [load()](#Převzato-z), nebo [onBeforeLoad()](#onbeforeload), [onAfterLoad()](#zatížení-šelfu)

Metoda [load()](#Převzato-z) kontroluje existenci místních (existujících) překladů:
- Pokud **neexistují**, provede požadavek API a uloží překlady do úložiště a instance je ve stavu `DONE & READY`.
- Pokud **existuje**, zkontroluje, zda jsou uložené překlady aktuální, a to porovnáním data poslední aktualizace uloženého v úložišti a data, ke kterému máme přístup v databázi. `window` v proměnné `window.propertiesLastModified`
  - Pokud jsou data **jinak**, požadavek API je proveden a získané překlady jsou uloženy v úložišti a instance je ve stavu `DONE & READY`.
  - Pokud jsou data **Stejné**, není provedena žádná další akce a instance je ve stavu `DONE & READY`.

Metody [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#zatížení-šelfu) přidá námi definovanou funkci do zásobníku zpětných volání, která se provede tak, jak je popsáno v podrobnostech metody. [onBeforeLoad()](#onbeforeload) a [onAfterLoad()](#zatížení-šelfu).

Metoda [přeložit()](#přeložit) přijme vstupní argument (překladový klíč) a pokusí se vyhledat konkrétní překlad v úložišti překladů.

- Pokud je překlad **existuje**, takže metoda jej vrací jako výsledek.
- Pokud je překlad **neexistuje**, nebo byl vložen neplatný překladový klíč, metoda vrátí jako výsledek vstupní klíč.

## Vytvoření instance:

**WebJET** inicializuje knihovnu v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js).

```javascript
/* webjetTranslationService */
import {Translator} from "./libs/translator/translator";

window.webjetTranslationService = new Translator();
```

Poté jej implementuje do souboru [app-init.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app-init.js), kde pomocí funkce [load()](#Převzato-z) načíst překlady ze serveru, pokud ještě neexistují lokálně nebo pokud došlo k aktualizaci.

Funkce [load()](#Převzato-z) sám zkontroluje aktuálnost existujících překladů a rozhodne, zda má odeslat požadavek na rozhraní API.

```javascript
// Spustenie načítania prekladov
window.webjetTranslationService.onAfterLoad(() => {
    // ...
}, true).load();
```

## Překlady v kódu:

### Inline (přímo v souborech javascript)

V souborech Javascript používáme funkci `WJ.translate()` která implementuje metodu [přeložit()](#přeložit), takže metoda [přeložit()](#přeložit) z knihovny se nikdy nepoužívá přímo, pokud neexistují implementace v jiných třídách, které budou používat API z `Translate`.

**V souborech WebJET používáme:**

```javascript
const preklad = WJ.translate('translation.key'); // V premennej preklad máme preložený text alebo ak text neeixstuje, tak samotnú hodnotu kľúča
```

### Úplná stránka HTML

**Implementace v systému WebJET CMS tuto funkci nevyžaduje, protože všechny překlady jsou zpracovávány během vykreslování stránky.**

Pro překlady přímo ve vykreslované stránce se používají datové atributy html. **data-translator**, `data-translator="prekladový.kľúč"`.

Atribut data lze umístit na libovolnou značku HTML, ale musí se nacházet bezprostředně u značky, která obsahuje překládaný text. Pokud je umístěn na značce HTML, která obsahuje další kód HTML, musí být tento kód HTML zahrnut i do samotného překladu, jinak bude stávající kód HTML nahrazen hodnotou z překladu.

!>**Varování:**, *Při nastavování přeloženého textu funkce odstraní všechny existující události, které se nacházejí na daném prvku a jeho potomcích.*

```html
<span data-translator="components.datatables.data.insertDate">Dátum vloženia</span>
```

Pak je třeba přidat volání funkce překladu do stránky HTML.

```javascript
$(document).ready(() => {
    window.webjetTranslationService.onBeforeLoad(instance => {
        instance.htmlTranslate();
    }).load();
});
```

nebo umístěte na konec stránky

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

## Seznam rozhraní API

**(Kliknutím zobrazíte detail funkce)**

Metody:
- [load()](#Převzato-z)
- [onBeforeLoad()](#onbeforeload)
- [onAfterLoad()](#zatížení-šelfu)
- [přeložit()](#přeložit)
- [htmlTranslate()](#htmltranslate)

Gettery:
- [jazyk](#jazyk)
- [datum](#datum)

Nastavovači:
- [urlLoad](#urlload)
- [urlUpdate](#urlupdate)

Podrobnější rozhraní API najdete v [Úložiště GIT](https://gitlab.web.iway.local/webjet/webjet8v9/-/tree/master/src/main/webapp/admin/v9/src/js/libs/translator#kni%C5%BEnica-translator)

***

### Podrobný popis funkcí

#### load()

Volání provede požadavek na server API.

*Adresy URL lze změnit pomocí [urlLoad](#urlload) a [urlUpdate](#urlupdate).*

```javascript
/**
 * @description Spustí načítanie prekladov
 * @returns {void}
 * @public
 */
window.webjetTranslationService.load();
```

#### onBeforeLoad()

Metoda umožňuje přidat zpětná volání, která se provedou ihned po zavolání metody. [load()](#Převzato-z).

Zpětná volání lze přidávat libovolně, ale vždy pouze před voláním výše uvedené metody.

Nepovinný druhý atribut `rewrite` zajistit, aby byla nastavena na hodnotu `TRUE` že se provede pouze poslední přidané zpětné volání.

Metoda vrací instanci třídy `Translator` a je tedy možné ji sledovat pomocí `DOT operátora` volat další metody.

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

Metoda umožňuje přidání zpětných volání, která se provedou po úspěšném přijetí dat z rozhraní API serveru po volání metody. [load()](#Převzato-z).

Zpětná volání lze přidávat libovolně, ale vždy pouze před voláním výše uvedené metody.

Nepovinný druhý atribut `rewrite` zajistit, aby byla nastavena na hodnotu `TRUE` že se provede pouze poslední přidané zpětné volání.

Metoda vrací instanci třídy `Translator` a je tedy možné ji sledovat pomocí `DOT operátora` volat další metody.

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

#### přeložit()

Zavoláním této metody získáme překlad do aktuálně používaného jazyka na základě klíče.

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

Zavoláním této metody v dolní části stránky nebo pomocí funkce `document.ready` to znamená, že po dokončení vykreslování webu zajistíme, aby všechny prvky na stránce, které mají definovaný atribut data. `data-translator` textové hodnoty budou změněny `innerHTML` na základě klíče zadaného v atributu dat. `data-translator`.

!>**Varování:**, *Při nastavování přeloženého textu funkce odstraní všechny existující události, které se nacházejí na daném prvku a jeho potomcích.*

[Klikněte pro více informací o použití](#pro-celou-stránku-html)

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

**gettery**

#### jazyk

Vrátí jazyk používaný aplikací přímo z obchodu.

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

#### datum

Vrací datum poslední aktualizace ve formátu časové značky (milisekundy).

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

**nastavovače**

#### urlLoad

Zadání nové adresy API pro načtení dostupných překladů. Adresa musí být změněna před voláním metody [load()](#Převzato-z)

```javascript
/**
 * @description Zadanie novej API adresy pre načítanie dostupných prekladov.
 * @param {string} value
 * @public
 * @setter
 */
window.webjetTranslationService.urlLoad = 'Nová/url/adresa/pre/ziskanie/prekladov';
```
