# Upravit v zobrazení mřížky

Datatables podporují přímou úpravu buňky v režimu editace `bubble`. Režim se zapne klepnutím na tlačítko **Upravit v zobrazení mřížky** v nástrojové liště. Následně kliknutím na buňku se zobrazí dialogové okno s editorem zvolené buňky.

## Inicializace režimu

Upravit v zobrazení mřížky se inicializuje v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) kódem:

```javascript
// aktivuj rezim uprava bunky / bubble
$(dataTableInit).on('click', 'table.dataTable tbody td', function (e) {
    if ($("body").hasClass("datatable-cell-editing")) {

    }
}
```

neboli po kliknutí na buňku tabulky. Problematická je kombinace WebJET úprav editoru, která přidává rozdělení polí do listů a zároveň zachování dialogu editoru v `DOM` stromu i po jeho zavření. V této kombinaci bylo zobrazení polí v `bubble` editoru nekorektní (zobrazila se všechna pole v aktuálním listu). Z toho důvodu je při editaci buňky nastavena CSS třída `show` jen na právě editovaný atribut.

Pomocí CSS třídy `div.DTE_Bubble` v [\_modal.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_modal.scss) je nastaveno zobrazení všech listů `div.tab-pane` (nejen aktivního) a zároveň jsou schovány všechny `div.row`, které nemají nastavenou `show` třídu. V [\_table.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_table.scss) je nastaveno zvýraznění buňky při `:hover` v režimu `body.datatable-cell-editing`.

Získání jména editovaného atributu bylo komplikované, nakonec je implementováno následujícím kódem:

```javascript
const colIndex = TABLE.cell(this).index().column;
let datatableColumn = TABLE.settings().init().columns[colIndex];
let columnName = datatableColumn.name;
```

klíčové je získání pořadového čísla buňky z aktuálně kliknutého `TD` elementu. Do proměnné `columnName` je uloženo jméno atributu. Nastavení `show` třídy je následně zabezpečeno kódem:

```javascript
$("div.DTE_Field").removeClass("show");
$("div.DTE_Field_Name_"+columnName).addClass("show");
```

Před otevřením samotného editoru buňky je volána REST služba pro získání dat řádku. Často datatabulka v seznamu neobsahuje všechna data potřebná pro editor (z důvodu optimalizace rychlosti načítání), používá se atribut `fetchOnEdit=true` v konfiguraci datatabulky. Po kliknutí na buňku je tedy vždy volána funkce `refreshRow` pro získání aktuálních a kompletních údajů řádku (v REST službě se použije `ProcessItemAction.GETONE`).

Samotné vyvolání editoru přes API `EDITOR.bubble($(this), {` je již standardní, upravena je jen hodnota možnosti `submit` na hodnotu `all` pro odesílání všech atributů řádku (nejen hodnoty jednoho atributu).

## Ne editovatelné buňky

Buňky, které nelze upravovat (např. ID, buňky pouze pro čtení) mají nastavenou CSS třídu `cell-not-editable`. Tuto třídu lze nastavit v anotaci `@DatatableColumn.className`. Automaticky se nastaví v [DatatableColumn.setCellNotEditable](../../../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumn.java) pro pole typu:
- `hidden` - pole typu `hidden`
- `textarea` - zdálo se nám nerozumné v malém `bubble` editoru zobrazovat velkou textovou oblast
- `editor.disabled` - pole, která mají nastaven `disabled` atribut na hodnotu `disabled`
