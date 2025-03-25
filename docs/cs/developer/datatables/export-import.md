# Export a import

Export a import dat využívá možnosti exportu a importu v [API datatables](https://datatables.net/extensions/buttons/examples/initialisation/export). Pro práci s Excel soubory se používá knihovna [SheetJS](https://docs.sheetjs.com/).

Po implementaci importu a exportu do vaší datatabulky nezapomeňte použít i [automatizovaný test importu](../testing/datatable-import.md).

## Export dat

- Umožňuje export do **Excelu(xlsx) a přímý tisk na tiskárnu**
- Souboru se nastaví jméno podle aktuálního `title` stránky a automaticky se doplní aktuální datum a čas.
- Exportu v tabulce se serverovým stránkováním lze nastavit typ dat (**aktuální/všechny strany, filtrované/všechny řádky, řazení**).
- Při serverovém stránkování se nejprve provede volání REST služby, ze které se získá **maximálně 50 000 záznamů**. Při potřebě exportovat více záznamů použijte vícenásobné exportování s využitím filtrování.
- Zpracování serverových dat probíhá mimo datatabulky (kvůli výkonu), nově se generují `datetime` sloupce a `radio buttony` (podporovány jsou `date-time, date, *-select, *-boolean`).
- Při exportu se v prvním řádku připraví seznam sloupců, **import následně není citlivý na pořadí sloupců**.
- Pro **výběrová pole** (select/číselníky) se **exportuje textová hodnota** a při importu se zpět rekonstruuje na ID. Umožňuje to mít **rozdílné ID navázaných záznamů** mezi prostředími (např. ID šablony pro web stránku), pokud se shoduje jméno, korektně se záznam spáruje. V exportu je následně i lidsky srozumitelný text namísto ID hodnoty.

Implementace je v souboru `export-import.js` ve funkci `bindExportButton(TABLE, DATA) `, která je inicializována přímo v ` index.js` při inicializaci datatabulky. HTML kód exportního dialogu je v souboru `datatables-data-export.pug` který je přes `include` vkládaný do `layout.pug`.

### Příprava dat před exportem

V datatabulce se často načítají jen základní data/sloupce (hlavně při použití [editorFields](../datatables-editor/customfields.md) atributu). Proto je před exportem vždy třeba volat server a získat kompletní data. Při detekování exportu se místo `ProcessItemAction.GETALL` použije `ProcessItemAction.GETONE` jako při získání jednoho záznamu. Vrácené entity tedy budou obsahovat všechna data.

Při použití `serverSide=false` na datatabulce nelze využít všech možností exportu (např. jen vyfiltrovaná data), protože se nemusí dát serverově zpracovat. Tyto možnosti jsou tedy schovány při kliknutí na tlačítko otevření dialogového okna exportu (implementováno přímo v `index.js` ve funkci tlačítka export dialogu).

### Provedení exportu

V dialogovém okně se nastavují možnosti exportu. Po kliknutí na tlačítko exportovat se získají aktuální data ze serveru voláním funkce `getDataToExport(serverSide, TABLE, pageVal, searchVal, orderVal)`, které se zpracují po volání `.then(response => {`. Pro každý řádek ze získaného JSON objektu `formatedData = content.map(c => {` se generuje pole řádku pro export dat iterací přes pole editoru `DATA.fields.forEach((dc) => {`.

Datová pole typu `json` se zpracovávají speciálně. Pro typy `dt-tree-page, dt-tree-group, dt-tree-dir` se do výstupu generuje hodnota z `v.fullPath` (nebo `v.virtualPath` pro `dt-tree-dir`). Výsledkem je, že v exportu nebude celý JSON objekt, ale hodnota typu `["/Portal/Novinky", "/English/News"]`.

Výsledný objekt `formatedData` obsahuje dvourozměrné pole dat pro export. Ty se použijí při API volání datatabulky simulováním kliknutí na tlačítko exportu `TABLE.button().add(0, {` pomocí volání `$(".exportujem").click();`. Toto fiktivní tlačítko se po exportu schová přes `TABLE.buttons('.exportujem').remove();`. Takto simulujeme kliknutí na standardní export tlačítko datatabulky – po kliknutí na exportovat v dialogovém okně se připraví data, přidá se do datatabulky tlačítko pro export dat a klikne se na toto tlačítko. Následně po exportu se standardní tlačítko pro export schová.

Konfigurace exportu je nastavena v `exportOptions`. Důležité je zpracování v `customizeData` kde se nově generuje hlavička tabulky. Původně obsahuje seznam sloupců v tabulce, úprava ale použije seznam sloupců pro editor. Do názvu sloupce se kromě `dc.label` hodnoty generuje i jméno atributu (`dc.name`), protože někdy jsou použity stejné názvy (např. v editaci uživatele je standardní uživatelské jméno, ale i jméno v části Adresa doručení).

### Speciální typ exportu

Pokud potřebujete implementovat speciální typ exportu stačí ve web stránce přidat následující element:

```html
<div class="hidden" id="datatableExportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-extend-custom" value="custom" data-hide="#datatableExportModal .file-name,#pills-export-advanced-tab">
        <label class="form-check-label" for="dt-settings-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

v atributu `data-hide` je možné specifikovat seznam elementů, které se automaticky schovají po nastavení uvedené možnosti.

Potřebné je implementovat JS funkci `window.exportDialogCustomCallback(type, TABLE)`, která je provedena při této možnosti:

```javascript
function exportDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_export.jsp");
    return true;
}
```

## Import dat

- Umožňuje **importovat data jako nová** (doplní se do databáze) nebo **párovat existující data podle zvoleného sloupce** (např. jméno, URL adresa a podobně). Při párování nejprve pohledá záznam v databázi a následně jej aktualizuje. Pokud neexistuje, vytvoří nový záznam.
- **Importuje se z formátu xlsx**.
- Import se provádí **postupně v dávkách po 25 záznamech**, aby nebyl zatížen server.

Implementace je v souboru `export-import.js` ve funkci `bindImportButton(TABLE, DATA) `, která je inicializována přímo v ` index.js` při inicializaci datatabulky. HTML kód importního dialogu je v souboru `datatables-data-import.pug` který je přes `include` vkládaný do `layout.pug`.

V dialogovém okně importu se zobrazuje seznam sloupců podle kterých lze data importovat. Tento seznam je implementován přímo v `index.js` po kliknutí na tlačítko zobrazení importního dialogu. Možnosti se generují ze seznamu `DATA.fields.forEach((col, index) => {`. Přeskočeny jsou atributy typu `hidden` nebo atributy s anotací `data-dt-import-hidden`.

### Konverze z Excelu

Konverze z Excel souboru na data se děje přímo na straně klienta ve volání `document.getElementById('insert-file').addEventListener('change', e => {`, neboli hned po výběru souboru. Používá se knihovna SheetJS voláním [xlsx.read](https://docs.sheetjs.com/#parsing-options). V konfiguraci je nastaveno zpracování dat pomocí atributu `cellDates: true`, což zajistí konverzi dat na `Date` objekt. Výsledkem čtení je JSON objekt excelData a vyvolání události `file-reader-done`.

Zpracování dat z Excelu v objektu `excelData` probíhá v `$( document ).on('file-reader-done', () => {` kde se provede několik úprav:
- Generuje se JSON objekt, jako jméno atributu se použije hodnota z hlavičky tabulky za znakem |.
- Struktura výsledného JSON objektu je stejná jako při standardním získání/uložení dat v datatabulce.
- Konvertuje se data, textové hodnoty číselníkových dat na ID hodnotu a pro pole typy JSON se provede konverze z řetězce na reálný JSON objekt.

Importování probíhá voláním stejné REST služby jako při použití standardního editoru záznamu. Odesílá se ale najednou více záznamů, maximálně ale podle hodnoty `chunks`. Pro velký počet záznamů se tedy postupně jmenuje REST služba `/editor`, přičemž v jednom volání je maximálně `chunks` záznamů (výchozí 25, definované v konf. proměnné `chunksQuantity`). Zobrazen je i ukazatel postupu podobně jako v galerii při nahrávání souboru.

### Aktualizace podle sloupce

Pokud je v dialogovém okně importu zvolena možnost aktualizovat podle sloupce jmenuje se metoda `DatatableRestControllerV2.editItemByColumn(T entity, String updateByColumn)`. Tato v první řadě vyhledá záznamy v databázi podle zadaného sloupce a hodnoty v daném řádku v excelu. Např. záznamy podle shody `email` atributu v excelu. Zde je důležité si uvědomit, že takových záznamů může být v databázi více a aktualizace během importu se provede na více záznamech.

Po nalezení shodujících záznamů v databázi je třeba modifikovat ID sloupec entity importovaného řádku. Podle anotace `@Id` se identifikuje sloupec (ne vždy to musí být `id`, může to být například. `userId`). Hodnota `Id` sloupce se nastaví na hodnotu existující entity v databázi a následně se provede uložení záznamu voláním `editItem(entity, id)`.

V jednoduchosti celý kód vyhledá existující záznam v databázi a importované entitě nastaví `id` hodnotu na hodnotu nalezeného záznamu.

> **Upozornění:** při implementaci jsme identifikovali problém, že pro třídy anotované přes Lombook nelze používat `BeanUtils.setProperty` ani `BeanUtils.copyProperties`. Je třeba použít `BeanWrapperImpl` a `NullAwareBeanUtils.copyProperties`.

> **Upozornění:** importovat je technicky možné i jen některé sloupce, nepředpokládejte tedy, že import bude vždy obsahovat všechna data. Jinak vám budou nastávat chyby typu `NullPointerException`. Zvlášť v `editorFields.toEntity` je třeba kontrolovat `null` hodnoty na atributech, aby jejich přenos nepadal.

### Podporované anotace

V anotaci `@DatatableColumn` lze použít následující možnosti

- `@DataTableColumnEditorAttr(key = "data-dt-import-updateByColumn", value = "PROPERTY")` - v dialogu pro import nastaví pro tento sloupec vyhledávání podle sloupce `PROPERTY`. Potřebné pokud v `editorFields` přepisujete nějakou vlastnost (např. `login`) ale při importu potřebujete v databázi vyhledávat/párovat podle původního atributu `login`.
- `@DataTableColumnEditorAttr(key = "data-dt-import-hidden", value = "true")` - takto anotovaný atribut se v dialogu pro import nezobrazí.
- `@DataTableColumn(className = "not-export")` - sloupec s CSS třídou `not-export` se nebude exportovat.

### Speciální typ importu

Pokud potřebujete implementovat speciální typ importu stačí ve web stránce přidat následující element:

```html
<div class="hidden" id="datatableImportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-import-extend-custom" value="custom" data-hide="#datatableImportModal .file-name,#import-settings">
        <label class="form-check-label" for="dt-settings-import-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

v atributu `data-hide` je možné specifikovat seznam elementů, které se automaticky schovají po nastavení uvedené možnosti.

Potřebné je implementovat JS funkci `window.importDialogCustomCallback(type, TABLE)`, která je provedena při této možnosti:

```javascript
function importDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_import.jsp");
    return true;
}
```

## Poznámky k implementaci

Číselníkové hodnoty (select) se exportují jako textová hodnota. V souboru [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js) jsou funkce `getOptionsTableExport` a `getOptionsTableImport`, které připraví tabulku s klíčem `fieldName-value` (export) a `fieldName-label` (import) pro snadný překlad mezi hodnotou a labelem.

Nejsložitější částí je čtení dat ze serveru při serverovém stránkování. Při požadavku na export se musí provést volání REST služby. Děje se to ve funkci `getDataToExport` kde se využívá a modifikuje `DATA.urlLatestParams` (nahradí se parametry jako size, page atd.). Data se získají standardním ajax voláním mimo API datatabulky (nechceme, aby datatabulka načítala tak velké množství dat - obava před jejím padnutím). Finta s využitím takových dat je v export tlačítku v optionu `customizeData: function(d)`, kde nahradíme data v datatabulce za nově získaná.

Při importu se transformují data z xlsx na JSON objekt. Ten se následně použije pro standardní volání DT editoru, kde se najednou odešle na server 25 záznamů. Import je impelentován ve funkci `importData`. Využívá se stávající progress bar pro upload souborů voláním eventu `$( document ).trigger('initAddedFileFromImageOutside', file);`.

Konverze z xlsx na JSON formát je implementována v `export-import.js`. Iteruje se seznam sloupců a z Excelu se získává hodnota pro dané jméno sloupce. Do JSON objektu se naplní pouze sloupce zadané v Excelu. Pro `-date` sloupce se formátuje datum, protože z Excelu se získá jen timestamp.

Modální okno pro import/export je globální, pokud je na stránce více datatabulek je třeba určit, ve které se děje import/export. Existují proměnné:
- `window.datatableExportModal.tableId` - ID tabulky pro export (selektor)
- `window.datatableImportModal.tableId` - ID tabulky pro import (selektor)
- `window.datatableImportModal.TABLE` - instance tabulky pro import

Třída `DatatableRestControllerV2` obsahuje metody `isExporting()` pro detekování exportu a `isImporting()` importu. Ty lze využít v implementaci vašeho REST controlleru, například. pro validaci dat v metodě `validateEditor`.
