# Úprava buňky

Datové tabulky podporují přímé úpravy buněk v režimu úprav `bubble`. Chcete-li režim zapnout, klikněte na **Úprava buňky** na panelu nástrojů. Poté klikněte na buňku a zobrazí se dialogové okno s editorem vybrané buňky.

## Inicializace režimu

Modifikace buňky je inicializována v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) Kód:

```javascript
// aktivuj rezim uprava bunky / bubble
$(dataTableInit).on('click', 'table.dataTable tbody td', function (e) {
    if ($("body").hasClass("datatable-cell-editing")) {

    }
}
```

tj. po kliknutí na buňku tabulky. Problematická je kombinace editace v editoru WebJET, která přidává rozdělení polí do listů při zachování dialogového okna editoru v režimu `DOM` stromu i po jeho uzavření. V této kombinaci se zobrazí pole ve stromu. `bubble` editor byl nesprávný (zobrazovala se všechna pole aktuálního listu). Z tohoto důvodu se při úpravě buňky nastaví třída CSS `show` pouze na aktuálně upravovaný atribut.

Použití tříd CSS `div.DTE_Bubble` v [\_modal.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_modal.scss) je nastaveno zobrazení všech listů `div.tab-pane` (nejen aktivní) a současně všechny `div.row` které nemají sadu `show` Třída. V [\_table.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_table.scss) zvýraznění buněk je nastaveno na `:hover` v režimu `body.datatable-cell-editing`.

Získání názvu upravovaného atributu bylo komplikované, nakonec je realizováno následujícím kódem:

```javascript
const colIndex = TABLE.cell(this).index().column;
let datatableColumn = TABLE.settings().init().columns[colIndex];
let columnName = datatableColumn.name;
```

klíčem je získat pořadové číslo buňky z aktuálně kliknuté buňky. `TD` prvek. Do proměnné `columnName` je uložen název atributu. Nastavení `show` třída je následně zabezpečena kódem:

```javascript
$("div.DTE_Field").removeClass("show");
$("div.DTE_Field_Name_"+columnName).addClass("show");
```

Před otevřením samotného editoru buněk se zavolá služba REST, aby se získala data řádků. Často datová tabulka v seznamu neobsahuje všechna data potřebná pro editor (kvůli optimalizaci rychlosti načítání), používá se atribut `fetchOnEdit=true` v konfiguraci datového souboru. Po kliknutí na buňku se tedy vždy zavolá funkce `refreshRow` pro získání aktuálních a úplných dat řádku (ve službě REST se používá `ProcessItemAction.GETONE`).

Volání samotného editoru prostřednictvím rozhraní API `EDITOR.bubble($(this), {` je již standardní, mění se pouze hodnota volby. `submit` na hodnotu `all` pro odeslání všech atributů řádku (nikoli pouze hodnoty jednoho atributu).

## Neupravitelné buňky

Buňky, které nelze upravovat (např. ID, buňky pouze pro čtení), mají nastavenou třídu CSS. `cell-not-editable`. Tuto třídu lze nastavit v anotaci `@DatatableColumn.className`. Automatické nastavení v [DatatableColumn.setCellNotEditable](../../../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumn.java) pro pole typu:
- `hidden` - typová pole `hidden`
- `textarea` - se nám zdálo nerozumné v malém `bubble` editoru pro zobrazení velké textové oblasti
- `editor.disabled` - pole, která mají nastavené `disabled` atribut na hodnotu `disabled`
