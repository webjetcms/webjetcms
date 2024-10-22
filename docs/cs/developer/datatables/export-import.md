# Export a import

Při exportu a importu dat se používají možnosti exportu a importu v nabídce [Datové tabulky API](https://datatables.net/extensions/buttons/examples/initialisation/export). Knihovna slouží k práci se soubory aplikace Excel [SheetJS](https://docs.sheetjs.com/).

Po implementaci importu a exportu do datové tabulky nezapomeňte použít funkci [automatický test importu](../testing/datatable-import.md).

## Export dat

- Umožňuje export do **Excel(xlsx) a přímý tisk na tiskárně**
- Soubor je pojmenován podle aktuálního `title` a automaticky se přidá aktuální datum a čas.
- Export v tabulce se stránkováním serveru lze nastavit na datový typ (**aktuální/všechny stránky, filtrované/všechny řádky, třídění**).
- Při stránkování serveru se nejprve provede volání služby REST, ze které se provede **maximálně 50 000 záznamů**. Pokud potřebujete exportovat více záznamů, použijte vícenásobný export pomocí filtrování.
- Zpracování dat serveru probíhá mimo datovou tabulku (z výkonnostních důvodů), je znovu generováno. `datetime` sloupců a `radio buttony` (podporovány jsou `date-time, date, *-select, *-boolean`).
- Při exportu je v prvním řádku připraven seznam sloupců, **import proto není citlivý na pořadí sloupců**.
- Pro **výběrová pole** (select/digits) s **exportovat textovou hodnotu** a při importu se rekonstruuje zpět na ID. To umožňuje mít **různá ID vázaných záznamů** mezi prostředími (např. ID šablony webové stránky), pokud se název shoduje, je záznam správně spárován. V důsledku toho je v exportu místo hodnoty ID také lidsky čitelný text.

Implementace je v souboru `export-import.js` ve funkci `bindExportButton(TABLE, DATA) ` který je inicializován přímo v `index.js` při inicializaci datové tabulky. HTML kód dialogového okna pro export je v souboru `datatables-data-export.pug` která je prostřednictvím `include` vložené do `layout.pug`.

### Příprava dat před exportem

V datové tabulce se často načítají pouze základní data/sloupce (zejména při použití [editorFields](../datatables-editor/customfields.md) atribut). Proto je vždy nutné před exportem zavolat na server a získat kompletní data. Když je zjištěn export, místo `ProcessItemAction.GETALL` které se mají používat `ProcessItemAction.GETONE` jako při získávání jednoho záznamu. Vrácené entity tedy budou obsahovat všechna data.

Při použití `serverSide=false` není možné použít všechny možnosti exportu datové tabulky (např. pouze filtrovaná data), protože je nemusí být možné zpracovat na serveru. Tyto volby jsou proto skryty, když kliknete na tlačítko pro otevření dialogového okna pro export (implementované přímo v aplikaci `index.js` ve funkci dialogového tlačítka pro export).

### Provedení exportu

Možnosti exportu se nastavují v dialogovém okně. Po kliknutí na tlačítko exportu se skutečná data načtou ze serveru voláním funkce `getDataToExport(serverSide, TABLE, pageVal, searchVal, orderVal)` které jsou zpracovány po volání `.then(response => {`. Pro každý řádek získaného objektu JSON `formatedData = content.map(c => {` pole řádků je pro export dat generováno iterací přes pole editoru. `DATA.fields.forEach((dc) => {`.

Datová pole typu `json` jsou ošetřovány speciálně. Pro typy `dt-tree-page, dt-tree-group, dt-tree-dir` hodnota z je generována na výstupu `v.fullPath` (nebo `v.virtualPath` Pro `dt-tree-dir`). Výsledkem je, že export nebude obsahovat celý objekt JSON, ale hodnotu typu `["/Portal/Novinky", "/English/News"]`.

Výsledný objekt `formatedData` obsahuje dvourozměrné datové pole pro export. Ty se používají ve volání API datového pole tak, že simulují kliknutí na tlačítko pro export. `TABLE.button().add(0, {` zavoláním `$(".exportujem").click();`. Toto fiktivní tlačítko se po exportu přes `TABLE.buttons('.exportujem').remove();`. Takto simulujeme kliknutí na standardní tlačítko pro export datové tabulky - po kliknutí na tlačítko export v dialogovém okně se data připraví, do datové tabulky se přidá tlačítko pro export dat a na toto tlačítko se klikne. Po exportu se pak standardní tlačítko pro export skryje.

Konfigurace exportu je nastavena v `exportOptions`. Důležité je zpracování v `customizeData` kde se znovu vygeneruje záhlaví tabulky. Původně obsahuje seznam sloupců tabulky, ale při úpravě se použije seznam sloupců pro editor. Kromě názvu sloupců je v něm uveden i `dc.label` hodnoty generuje také název atributu (`dc.name`), protože někdy se používají stejná jména (např. v editaci uživatele je výchozí uživatelské jméno, ale také jméno v sekci Doručovací adresa).

### Zvláštní typ vývozu

Pokud potřebujete implementovat speciální typ exportu, stačí na webovou stránku přidat následující prvek:

```html
<div class="hidden" id="datatableExportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-extend-custom" value="custom" data-hide="#datatableExportModal .file-name,#pills-export-advanced-tab">
        <label class="form-check-label" for="dt-settings-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

v atributu `data-hide` je možné zadat seznam prvků, které se po nastavení výše uvedené možnosti automaticky skryjí.

Potřeba implementovat funkci JS `window.exportDialogCustomCallback(type, TABLE)` která je provedena v rámci této možnosti:

```javascript
function exportDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_export.jsp");
    return true;
}
```

## Import dat

- Povoleno **importovat data jako nová** (pro přidání do databáze) nebo **porovnat existující data podle vybraného sloupce** (např. název, adresa URL atd.). Při porovnávání nejprve vyhledá záznam v databázi a poté jej aktualizuje. Pokud neexistuje, vytvoří nový záznam.
- **Import z formátu xlsx**.
- Import se provádí **postupně v dávkách po 25 záznamech** aby nedošlo k přetížení serveru.

Implementace je v souboru `export-import.js` ve funkci `bindImportButton(TABLE, DATA) ` který je inicializován přímo v `index.js` při inicializaci datové tabulky. HTML kód dialogového okna importu je v souboru `datatables-data-import.pug` která je prostřednictvím `include` vložené do `layout.pug`.

V dialogovém okně importu se zobrazí seznam sloupců, podle kterých lze data importovat. Tento seznam je implementován přímo v `index.js` po kliknutí na tlačítko zobrazíte dialogové okno pro import. Možnosti jsou generovány pomocí seznamu `DATA.fields.forEach((col, index) => {`. Přeskočeny jsou atributy typu `hidden` nebo atributy s anotací `data-dt-import-hidden`.

### Převod z aplikace Excel

Převod ze souboru Excel na data se provádí přímo na straně klienta ve volání `document.getElementById('insert-file').addEventListener('change', e => {`, tedy hned po výběru souboru. Knihovna SheetJS se používá voláním [xlsx.read](https://docs.sheetjs.com/#parsing-options). V konfiguraci je zpracování data nastaveno pomocí atributu `cellDates: true`, který zajistí převod dat na `Date` objekt. Výsledkem čtení je objekt JSON excelData a vyvolání události `file-reader-done`.

Zpracování dat z aplikace Excel v objektu `excelData` se koná v `$( document ).on('file-reader-done', () => {` kde bude provedeno několik úprav:
- Je vygenerován objekt JSON, který jako název atributu použije hodnotu ze záhlaví tabulky za znakem |.
- Struktura výsledného objektu JSON je stejná jako při standardním načítání/ukládání dat do datové tabulky.
- Převádí data, textové hodnoty kódových dat na hodnotu ID a u polí typu JSON se provádí převod z řetězce na skutečný objekt JSON.

Import se provádí voláním stejné služby REST jako při použití standardního editoru záznamů. Odesílá se však více záznamů najednou, maximálně však do počtu `chunks`. Při velkém počtu záznamů se služba REST volá postupně. `/editor`, s maximální hodnotou `chunks` záznamů (výchozí 25, definováno v konf. proměnné `chunksQuantity`). Zobrazí se také ukazatel průběhu, podobně jako v galerii při nahrávání souboru.

### Aktualizace podle sloupce

Pokud je v dialogovém okně importu vybrána možnost aktualizovat podle sloupce, je metoda volána. `DatatableRestControllerV2.editItemByColumn(T entity, String updateByColumn)`. Ten nejprve vyhledá záznamy v databázi podle zadaného sloupce a hodnoty v daném řádku v excelu. Např. záznamy podle shody `email` atribut v Excelu. Zde je důležité si uvědomit, že takových záznamů může být v databázi více a aktualizace při importu bude provedena pro více záznamů.

Po nalezení odpovídajících záznamů v databázi je třeba upravit sloupec ID entity
&#x20;v importovaném řádku. Podle anotace `@Id` sloupec je identifikován (nemusí to být vždy `id`, může to být např. `userId`). Hodnota `Id` sloupec je nastaven na hodnotu existující entity v databázi a poté je záznam uložen voláním `editItem(entity, id)`.

Zjednodušeně řečeno, celý kód vyhledá existující záznam v databázi a nastaví importovanou entitu. `id` na hodnotu nalezeného záznamu.

> **Varování:** během implementace jsme zjistili, že pro třídy anotované pomocí Lombook není možné použít. `BeanUtils.setProperty` ani `BeanUtils.copyProperties`. Je nutné použít `BeanWrapperImpl` a `NullAwareBeanUtils.copyProperties`.

> **Varování:** je technicky možné importovat pouze některé sloupce, proto nepředpokládejte, že import bude vždy obsahovat všechna data. V opačném případě se zobrazí chyby jako např. `NullPointerException`. Zejména v `editorFields.toEntity` potřebu kontroly `null` hodnoty na atributech, aby nedošlo k selhání jejich přenosu.

### Podporované anotace

V anotaci `@DatatableColumn` lze použít následující možnosti

- `@DataTableColumnEditorAttr(key = "data-dt-import-updateByColumn", value = "PROPERTY")` - v dialogovém okně importu nastaví vyhledávání podle sloupce pro tento sloupec. `PROPERTY`. Povinné, pokud např. v `editorFields` přepíšete vlastnost (např. `login`), ale při importu je třeba hledat/párovat podle původního atributu v databázi. `login`.
- `@DataTableColumnEditorAttr(key = "data-dt-import-hidden", value = "true")` - takto označený atribut se v dialogovém okně importu nezobrazí.
- `@DataTableColumn(className = "not-export")` - sloupec s třídou CSS `not-export` nebudou exportovány.

### Zvláštní typ dovozu

Pokud potřebujete implementovat speciální typ importu, stačí na webovou stránku přidat následující prvek:

```html
<div class="hidden" id="datatableImportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-import-extend-custom" value="custom" data-hide="#datatableImportModal .file-name,#import-settings">
        <label class="form-check-label" for="dt-settings-import-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

v atributu `data-hide` je možné zadat seznam prvků, které se po nastavení výše uvedené možnosti automaticky skryjí.

Potřeba implementovat funkci JS `window.importDialogCustomCallback(type, TABLE)` která je provedena v rámci této možnosti:

```javascript
function importDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_import.jsp");
    return true;
}
```

## Poznámky k implementaci

Číselné hodnoty (select) jsou exportovány jako textová hodnota. V souboru [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js) jsou funkce `getOptionsTableExport` a `getOptionsTableImport` který připraví tabulku s klíčem `fieldName-value` (export) a `fieldName-label` (import) pro snadný převod mezi hodnotou a štítkem.

Nejsložitější částí je čtení dat ze serveru během stránkování serveru. Při požadavku na export musí být provedeno volání služby REST. To se provádí ve funkci `getDataToExport` kde se používá a upravuje `DATA.urlLatestParams` (parametry jako velikost, stránka atd. jsou nahrazeny). Data se načítají standardním ajaxovým voláním mimo API datového souboru (nechceme, aby datový soubor načítal tak velké množství dat - obava z jeho zhroucení). Trik s použitím takových dat je v tlačítku pro export ve volbě `customizeData: function(d)`, kde nahradíme data v datové tabulce nově získanými daty.

Při importu jsou data transformována z formátu xlsx na objekt JSON. Ten se pak použije pro standardní volání editoru DT, kdy se na server odešle najednou 25 záznamů. Import je impelentován ve funkci `importData`. Stávající ukazatel průběhu se používá pro nahrávání souborů voláním události `$( document ).trigger('initAddedFileFromImageOutside', file);`.

Konverze z formátu xlsx do formátu JSON je implementována v aplikaci `export-import.js`. Seznam sloupců je iterován a hodnota pro daný název sloupce je načtena z aplikace Excel. Do objektu JSON se vyplní pouze sloupce zadané v aplikaci Excel. Pro `-date` sloupce jsou formátovány pomocí data, protože z aplikace Excel se načítá pouze časové razítko.

Modální okno pro import/export je globální, pokud je na stránce více datových tabulek, je nutné určit, ve které z nich se import/export provádí. Existují proměnné:
- `window.datatableExportModal.tableId` - ID tabulky pro export (selektor)
- `window.datatableImportModal.tableId` - ID tabulky pro import (selektor)
- `window.datatableImportModal.TABLE` - instance tabulky pro import

Třída `DatatableRestControllerV2` obsahuje metody `isExporting()` pro detekci vývozu a `isImporting()` dovoz. Ty lze použít v implementaci řadiče REST, např. k ověření dat v metodě `validateEditor`.
