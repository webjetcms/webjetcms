# DatatableOpener

Třída `DatatableOpener` zabezpečuje zobrazení ID aktuálně otevřeného záznamu v URL adrese prohlížeče (parametr `id`), zobrazení ID v hlavičce datatabulky a vyhledání zadaného ID v hlavičce datatabulky. Třída je implementována podle třídy [AbstractJsTreeOpener](js-tree-document-opener.md).

![](datatable-opener.png)

Třída je inicializována přímo v `index.js` datatabulky. Opener lze vypnout nastavením parametru konfigurace `idAutoOpener: false`.

```javascript
...
import {DatatableOpener} from "../../src/js/libs/data-tables-extends/";
...
DATA.idAutoOpener = (typeof options.idAutoOpener !== "undefined") ? options.idAutoOpener : true;
...

//inicializacia openera
if (DATA.idAutoOpener===true) {
    //inicializuj DT opener
    TABLE.datatableOpener = new DatatableOpener();
    TABLE.datatableOpener.dataTable = TABLE;
    TABLE.datatableOpener.init();
}

//ukazka vypnutia openera nastavenim idAutoOpener: false
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    columns: webpageColumns,
    tabs: tabs,
    editorId: "docId",
    idAutoOpener: false
});
```

Hodnota ID se do URL parametru nastaví pouze během otevřeného editoru, po jeho zavření se z URL adresy parametr `id` smaže. Takového chování se nám zdá přirozenější a vystihující aktuální stav.

Třída po inicializaci v hlavičce datatabulky vytvoří voláním funkce `bindInput` vstupní pole pro zadání ID, ve kterém čeká na stisk klávesy `Enter`. Následně zadanou hodnotu nastaví do atributu `this.id` a vyvolá `this.dataTable.draw();`, aby se spustil proces zobrazení editoru podobně jako při inicializaci z URL parametru.

## Nastavení ID do URL adresy

Třída po otevření editoru (pomocí události `this.dataTable.EDITOR.on( 'open', ( e, type ) => {`) získá aktuální JSON objekt `this.dataTable.EDITOR.currentJson` ze kterého získá ID hodnotu podle sloupce v `this.dataTable.DATA.editorId` (ne vždy je ID hodnota ve sloupci id, může to být například. `insertScriptId`). Získanou hodnotu nastaví voláním `setInputValue` do vstupního pole a do URL parametru id.

## Otevření editoru na základě URL parametru

Po inicializování v `index.js` se nastaví hodnota z URL parametru do interních objektů. Poslouchá se událost `this.dataTable.on('draw.dt', (evt, settings) => {`, neboli vykreslení tabulky. Z ní se získá na základě ID příslušný řádek a vyvolá se funkce otevření editoru `this.dataTable.wjEditFetch($('.datatableInit tr[id=' + id + ']'));`.

## Vyhledávání zadaného ID

Problémem otevření editoru je stav, kdy zadané ID není na aktuálně zobrazené straně datatabulky. Zde také musíme rozlišovat stav serverového a klientského stránkování. Voláním `const idIndex = Object.keys(settings.aIds).indexOf(id.toString());` se získá pořadový index v aktuálních datech pro zadané id. Zároveň se vypočítá strana, na které by se měl záznam nacházet výpočtem `const pageNumber = info.length < 0 ? 0 : Math.floor(idIndex / info.length);`.

Jedná-li se o aktuální stránku (nebo byl záznam nalezen v datech při serverovém stránkování) vyvolá se zobrazení editoru voláním `this.dataTable.wjEditFetch($('.datatableInit tr[id=' + id + ']'));`.

Pokud záznam je na jiné straně vyvolá se zobrazení této strany voláním `setTimeout(() => this.dataTable.page(pageNumber).draw('page'), 500);`.

Při serverovém stránkování ale neumíme jednoduše na klientské straně určit stranu, na které se záznam nachází. Je proto spuštěno postupné stránkování údajů voláním REST služby serveru. Aby nedošlo k zahlcení je stránkování voláno přes funkci `setTimeout` v 500ms intervalu. Aby nedošlo k zacyklení je počítáno volání serveru v atrium `failsafeCounter`, kde je nastaven limit 30 volání. **Vyhledávání tedy najde zadané ID v maximálně prvních 30 stranách**.

Do budoucna zvažujeme implementovat získání strany v serverové REST službě, což by eliminovalo problém postupného stránkování údajů na klientské straně.

## Filtrování podle hash parametrů

Knihovna poskytuje také možnost filtrování tabulky podle zadaných parametrů v hash výrazu, tedy např. `/admin/v9/users/user-list/#dt-filter-id=3`. Parametry zadané ve `window.location.hash` začínající na `dt-filter-` jsou po inicializaci tabulky nastaveny do příslušných filtrovacích polí v hlavičce. Následně je provedeno kliknutí na ikonu vyhledávání při prvním poli.

Pokud je v hash výrazu i hodnota `dt-select=true`, tak po načtení záznamů jsou řádky označeny. Je tedy snadno možné provést akci typu schválení uživatele kliknutím na tlačítko a podobně.

Pokud je v hash výrazu i hodnota `dt-open-editor=true` otevře se po označení řádků i editor (řádky se automaticky i označí, není potřebný i parametr `dt-select=true`).

Implementace je ve funkci `filterTableByHashParameters`, která je vyvolána při události `this.dataTable.one('draw.dt', (evt, settings) => {`.
