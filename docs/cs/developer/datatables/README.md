# Datové tabulky

Knihovna [datatables.net](http://datatables.net) je pokročilá tabulka s připojením ke službám REST.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Datové tabulky](#datové-tabulky)
  - [Základní inicializace ve spolupráci se Spring REST](#základní-inicializace-ve-spolupráci-s-pružinovým-odpočinkem)
  - [Možnosti konfigurace](#možnosti-konfigurace)
    - [Nastavení sloupců](#nastavení-sloupců)
    - [Zobrazení kódu HTML](#zobrazit-html-kód)
  - [Přidání/odebrání tlačítek](#přidání-nebo-odebrání-tlačítek)
  - [Tlačítko pro provedení akce serveru](#tlačítko-pro-provedení-akce-serveru)
  - [Tlačítka podle práv](#tlačítka-podle-práv)
  - [Stylování řádků](#stylování-řádků)
  - [Stavové ikony](#stavové-ikony)
  - [Zobrazení dat na základě práv](#zobrazení-dat-na-základě-práv)
  - [Uspořádání](#Uspořádání)
  - [Vyhledávání](#Vyhledávání)
  - [Externí filtr](#externí-filtr)
  - [Export/import](#exportimport)
  - [Volání API](#volání-api)
  - [Ukázky kódu](#ukázky-kódu)
    - [Naslouchání události obnovení tabulky:](#naslouchání-události-obnovení-tabulky)

<!-- /code_chunk_output -->

## Základní inicializace ve spolupráci se Spring REST

Implementace datových tabulek WebJET se konfiguruje pomocí sloupců objektu JSON. Tento objekt obsahuje definice sloupců pro datovou tabulku i editor datových tabulek. K inicializaci tabulky se pak použije konstruktor WJ.DataTable.

Doporučujeme však vždy vygenerovat objekt sloupců z [Anotace entit v jazyce Java](../datatables-editor/datatable-columns.md).

Základní příklad:

```javascript
script.
    let galleryTable = null;
    window.domReady.add(function () {

        //URL adresa REST sluzby
        let url = "/admin/rest/components/gallery";

        //definicia stlpcov
        let columns = [
            {
                data: "id",
                name: "id",
                title: "ID",
                defaultContent: '',
                className: 'dt-select-td',
                renderFormat: "dt-format-selector"
            },
            {
                data: "imageName",
                name: "",
                title: "",
                render: function ( data, type, row ) {
                    //specialna render funkcia pre zobrazenie obrazka galerie
                    return '<div class="img" style="background-image:url(' + row.imagePath + '/' + data +');"></div>';
                },
                className: "dt-image",
                renderFormat: "dt-format-none"
            },
            {
                data: "imageName",
                name: "imageName",
                title: "Nazov suboru",
                renderFormat: "dt-format-text",
                renderFormatLinkTemplate: "javascript:;",
                renderFormatPrefix: '<i class="ti ti-pencil"></i> ',
                className: "dt-row-edit",

                editor: {
                    type:  "text"
                }
            },
            {
                data: "imagePath",
                name: "imagePath",
                title: "Adresar",
                renderFormat: "dt-format-text",

                editor: {
                    type:  "text",
                    attr: {
                        "data-dt-field-hr": "after"
                    }
                }
            }
        ];

        /*
        options pre DataTabulku
        {
            src: zdrojove data (objekt)
            url: URL adresa rest sevisu
            serverSide: ak je nastavene na true bude sa vyhladavanie/sortovanie/strankovanie posielat na server
            columns: definicia stlpcov
            tabs: definicia zaloziek pre Editor
            hideTable: boolean po nastaveni na true sa datatabulka nezobrazi
            noAll: boolean, po nastaveni na true sa nebude k url pridavat /all pre ziskanie vsetkych zaznamov
        }
        */

        galleryTable = WJ.DataTable( {
            url: url,
            serverSide: true,
            columns: columns,
        });

    });


<table class="datatableInit table cardView cardViewS"></table>
```

## Možnosti konfigurace

```javascript
    WJ.DataTable( {
        Options
    });
```

Možnosti konfigurace (`options`):

Minimální konfigurace:
- `url {string}` Adresa URL koncového bodu služby REST pro načtení dat. K této adrese URL datový soubor přidá adresu `/all` načíst všechna data (pokud není nastavena možnost noAll), `findByColumns` pro vyhledávání nebo `/editor` pro ukládání dat.
- `columns {json}` definice sloupců, ideálně z [Anotace Java](../datatables-editor/datatable-columns.md).

**Další možnosti:**

- `serverSide {boolean}` v hodnotě `true` očekává použití stránkování a řazení na serveru pomocí volání služeb REST s hodnotou `false` provede lokální stránkování a řazení nad původně načtenými daty.
- `tabs {json}` Definice [karty pro redaktora](../datatables-editor/README.md#karty-v-editoru).
- `id {string}` jedinečný identifikátor datové tabulky, pokud není zadán, použije se hodnota `datatableInit`. Zvláště potřebné, pokud máte na jedné webové stránce více datových tabulek.
- `editorId {string}` jedinečný identifikátor editoru, pokud není zadán, použije se hodnota `id`. Zvláště potřebné, pokud máte na jedné webové stránce více datových tabulek.
- `onXhr {function}` Funkce JavaScriptu, která je volána po [načítání dat](https://datatables.net/reference/event/xhr) ve formě `function ( TABLE, e, settings, json, xhr ) {}`.
- `onPreXhr(TABLE, e, settings, data) {function}` Funkce JavaScriptu, která se volá [před načtením dat](https://datatables.net/reference/event/preXhr), umožňuje přidávat parametry k odesílaným datům. Ty se zadávají s předponou `fixed_` aby se odlišily od standardních parametrů datových souborů. Příklad: `onPreXhr: function(TABLE, e, settings, data) { data.fixed_searchFilterBotsOut = $('#botFilterOut').is(':checked'); }`.
- `onEdit(TABLE, row, dataAfterFetch, dataBeforeFetch) {function}`: Funkce JavaScriptu, která se vyvolá po kliknutí na odkaz pro úpravu záznamu. Jako parametry obdrží: `TABLE` - instance datové tabulky, `row` - objekt jQuery řádku, na který bylo kliknuto, `dataAfterFetch` - když je funkce zapnutá `fetchOnEdit` json data načtená po obnovení, `dataBeforeFetch` původní data řádku JSON pro volání k jejich obnovení. Standardní editor pak můžete otevřít voláním `TABLE.wjEdit(row);`. Příkladem použití je [web-pages-list.pug](../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug).
- `fetchOnCreate {boolean}` pokud je nastavena na hodnotu true, bude před vytvořením nového záznamu provedeno volání REST s hodnotou -1 pro získání dat nového objektu. Hodnoty se nastavují voláním `EDITOR.setJson(json)` zavedené v `$.fn.dataTable.Editor.prototype.setJson` v případě `initCreate`.
- `fetchOnEdit {boolean}` pokud je nastavena na hodnotu true, bude před úpravou záznamu provedeno volání REST pro načtení aktuálních dat upravovaného záznamu. Při použití datové tabulky, například pro webovou stránku, je záznam aktualizován ze serveru před otevřením editoru, takže v editoru bude vždy otevřena nejnovější verze. Implementováno prostřednictvím funkce JS `refreshRow` a zákaznické tlačítko `$.fn.dataTable.ext.buttons.editRefresh` nahradit standardní tlačítko `edit`.
- `idAutoOpener {boolean}` umožňuje nastavení na `false` deaktivovat [automatické otevření editoru](../libraries/datatable-opener.md) pomocí parametru URL a vložením pole ID do záhlaví tabulky.
- `hideTable {boolean}` po nastavení na `true` se datová tabulka na stránce nezobrazí (bude skryta).
- `jsonField {function}` definice pole [typu json](../datatables-editor/field-json.md#použití-specifických-objektů-json).
- `order {array}` Výchozí [způsob uspořádání](#Uspořádání) tabulky.
- `paging {boolean}` false zakáže stránkování datových souborů (zobrazí se všechna vrácená data ze serveru, možnost nastavení velikosti stránky se nezobrazí).
- `nestedModal {boolean}` pokud je nastaveno na true, jedná se o datovou tabulku vloženou jako pole v editoru - [vnořená datová tabulka](../datatables-editor/field-datatable.md), vnořená tabulka má přidanou třídu CSS `DTE_nested_modal`.
- `noAll {boolean}` ve výchozím nastavení přidá do sady url `/all` získat všechna data nastavením `noAll` na adrese `false` s `/all` nebude přidán, ale nebude fungovat ani vyhledávání.
- `initialData {variable}` data pro počáteční zobrazení (bez nutnosti volat službu REST), viz dokumentace k nástroji [optimalizace rychlosti zobrazení](../apps/webpages/README.md) seznam webových stránek. Technicky vzato, pokud je tento objekt nastaven, při prvním volání služby REST se použijí zadaná data.
- `initialData.forceData {boolean}` po nastavení na `true` počáteční data se používají bez ohledu na jejich velikost, obvykle se používá, když jsou počáteční data prázdným objektem, protože jsou následně získána jiným způsobem. Pro načtení prázdných dat lze použít funkci `initialData:  dtWJ.getEmptyData(true)`.
- `hideButtons {string}` čárkou oddělený seznam názvů tlačítek, která se mají v datové tabulce automaticky skrýt (nezobrazit), např. `create,edit,duplicate,remove,import,celledit`.
- `removeColumns {string}` čárkou oddělený seznam sloupců, které se nemají zobrazovat, i když jsou v definici (například pokud datovou tabulku zobrazujete na více místech a nepotřebujete všechny sloupce). Například. `whenToPublish,datePublished`.
- `forceVisibleColumns` seznam sloupců oddělených čárkou, které se zobrazí (přepíše sloupce nastavené uživatelem), obvykle se používá ve vnořené datové tabulce, kde je třeba zobrazit pouze některé sloupce.
- `updateColumnsFunction` název funkce JavaScriptu, která bude použita k úpravě seznamu sloupců. Typicky se používá ve vnořené datové tabulce, kde je třeba přidat/upravit zobrazené sloupce (viz ukázka níže).
- `perms` sady [práva na zobrazení tlačítek](#tlačítka-podle-práv) přidávat, upravovat, duplikovat a mazat data.
- `lastExportColumnName` pokud je zadán, zobrazí v dialogovém okně exportu možnost exportovat dosud neexportovaná data (použitá ve formulářích). Hodnota představuje název sloupce, který má být přidán jako `NULL` podmínka výběru dat (musí být správně implementována ve službě REST).
- `byIdExportColumnName` pokud je zadán v dialogovém okně exportu, umožňuje export podle vybraných řádků. Hodnota je název sloupce v databázi s hodnotou ID (typicky id, používané ve formulářích). Filtrování je třeba implementovat jako `predicates.add(root.get("id").in(idsList));` ve službě REST.
- `editorButtons` pole tlačítek, která se zobrazí v editoru. Příklad: `editorButtons: [ {title: "Uložiť", action: function() { this.submit(); } }, { title: ...} ]`. Používá rozhraní API pro Editor datových tabulek.
- `createButtons` pole tlačítek pro přidání nového záznamu, formát stejný jako pro `editorButtons`.
- `keyboardSave {boolean}` - nastavením na hodnotu `false` zakázat možnost uložit položku v editoru pomocí klávesové zkratky. `CTRL+S/CMS+S`.
- `stateSave {boolean}` - nastavením na hodnotu `false` zakázat možnost zapamatovat si pořadí sloupců a rozvržení tabulky v prohlížeči.
- `customFieldsUpdateColumns {boolean}` - nastavením na hodnotu `true` je v získávání [volitelná pole](../datatables-editor/customfields.md) také aktualizovat názvy sloupců v tabulce a v nastavení zobrazených sloupců (ve výchozím nastavení na hodnotu `false` volitelné názvy polí se aktualizují pouze v editoru).
- `customFieldsUpdateColumnsPreserveVisibility {boolean}` - nastavením na hodnotu `true` nastavení zobrazení sloupce pro režim je pro uživatele zachováno. `customFieldsUpdateColumns`. Lze ji použít pouze v případě, že se sloupce datové tabulky během zobrazení nemění. Například v sekci Překladové klíče se data nemění, lze ji nastavit na hodnotu `true`, ale v sekci Číselníky se při změně číselníku mění i sloupce, tato volba zde není použitelná.
- `autoHeight {boolean}` - ve výchozím nastavení tabulka vypočítá svou výšku tak, aby maximálně využila prostor v okně. Nastavením na hodnotu `false` výška tabulky bude odpovídat obsahu (počtu řádků).

```javascript
let columns = [
    {
        data: "audit",
        name: "audit",
        title: "Auditované",

        renderFormat:   "dt-format-checkbox"
                        "dt-format-selector"
                        "dt-format-text"
                        "dt-format-text-wrap"
                        "dt-format-none"
                        "dt-format-date-time"
                        "dt-format-select" //moznosti bere z editor: { options: }

        renderFormatLinkTemplate:   "javascript:;",
                                     "/temps-list.html"
        renderFormatPrefix: '<i class="ti ti-pencil"></i> ',
        renderHideValue: false, //TODO ???
        render: function ( data, type, row ) {
            //console.log("data", data, "type", type, "row", row);
            return '<div class="img" style="background-image:url(' + row.imagePath + '/' + data +');"></div>';
        },

        className: "dt-image",

        defaultContent: '',

        perms: "multiDomain" //stĺpec sa zobrazí len ak používateľ má právo multiDomain

    }
];

//Ukazka pouzitia updateColumnsFunction
//@DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"),
    function updateColumnsGroupDetails(columns) {
        //doplnenie kliknutia na stlpec fullPath
        WJ.DataTable.mergeColumns(columns, {
            name: "fullPath",
            renderFormatLinkTemplate: "javascript:openGroupDetails({{groupId}})"
        });
    }
    function openGroupDetails(groupId) {
        window.open("/admin/v9/webpages/web-pages-list/?groupid="+groupId);
    }
```

**Zahájení vyhledávání:**

Mohou nastat případy, kdy je třeba při zobrazení tabulky okamžitě inicializovat (zapamatované) vyhledávání. Toho se využívá v aplikaci Statistiky, která si pamatuje rozsah nastavených dat od-do. Kritéria vyhledávání se použijí již při prvním volání služby REST. Tato možnost se nastavuje pomocí objektu JSON v souboru `options.defaultSearch`. Obsahuje seznam selektorů s hodnotou, která se použije na filtr před prvním voláním služby REST, např.:

```json
{
    ".dt-filter-from-dayDate": "06.06.2022",
    ".dt-filter-to-dayDate": "22.08.2022"
}
```

Příklad použití při zapamatování v `sessionStorage` prohlížeč:

```javascript
//inicializacia datatabulky
errorDataTable = WJ.DataTable({
    url: url,
    serverSide: false, //false lebo sa nevyužíva repositár
    columns: columns,
    id: "errorDataTable",
    idAutoOpener: false,
    defaultSearch: ChartTools.getSearchCriteria(),
    onPreXhr: function(TABLE, e, settings, data) {
        //console.log('onPreXhr, url=', $('#searchUrl').val());
        data.fixed_searchurl = $('#searchUrl').val();
    }
});
//Onchange events - update table
$("#errorDataTable_extfilter").on("click", "button.filtrujem", function() {
    //reload table values
    ChartTools.saveSearchCriteria(errorDataTable.DATA);
    errorDataTable.ajax.reload();
});

//appModule
/**
 * Save last search criteria to session storage, so all stats page will have same criteria when loaded
 * @param {*} DATA
 */
export function saveSearchCriteria(DATA) {
    var inputs = [".dt-filter-from-dayDate", ".dt-filter-to-dayDate", "#rootDir", "#botFilterOut", "#searchUrl", ".dt-filter-lastLogon"];
    var defaultSearch = {};

    for (const name of inputs) {
        var value = $("#"+DATA.id+"_extfilter "+name).val();
        if ("true"===value) {
            //it's checkbox
            value = $("#"+DATA.id+"_extfilter "+name).is(":checked");
        }
        if (value != "" && value != "-1" && value != "false") defaultSearch[name] = value;
    }
    var json = JSON.stringify(defaultSearch);
    if (json != "{}") window.sessionStorage.setItem("webjet.apps.stat.filter", json);
    else window.sessionStorage.removeItem("webjet.apps.stat.filter");
}

/**
 * Gets saved search criteria from session storage
 * @returns
 */
export function getSearchCriteria() {
    var defaultSearch = window.sessionStorage.getItem("webjet.apps.stat.filter");
    if ("{}"==defaultSearch) defaultSearch = null;
    if (defaultSearch != null) {
        defaultSearch = JSON.parse(defaultSearch);
        for (const property in defaultSearch) {
            var value = defaultSearch[property];
            if (property == "#rootDir") {
                var $property = $(property)
                $property.val(value);
                $property.selectpicker("val", value);
            }
            if (property == "#botFilterOut") {
                $("#botFilterOut").prop("checked", value);
            }
            if (property == "#searchUrl") {
                $("#searchUrl").val(value);
            }
        }
    }
    return defaultSearch;
}
```

### Nastavení sloupců

`renderFormat`:
- `dt-format-selector` - zaškrtávací políčko pro označení řádku, měl by být jako první sloupec.
- `dt-format-none` - sloupec nebude mít v záhlaví žádné možnosti.
- `dt-format-text, dt-format-text-wrap` - standardní text, `escapuje` Kód HTML
- `dt-format-select` - výběrové pole
- `dt-format-checkbox` - Typ HTML `checkbox`
- `dt-format-boolean-true, dt-format-boolean-yes, dt-format-boolean-one` - `true/false` možnosti
- `dt-format-number, dt-format-percentage` - zobrazit čísla
- `dt-format-number--decimal, dt-format-percentage--decimal`
- `dt-format-number--text` - zobrazí zaokrouhlené číslo, vyšší číslo vypíše v textové podobě, např. `10 tis.` místo `10000`
- `dt-format-filesize` - formátování velikosti souboru jako `10,24 kB`
- `dt-format-date, dt-format-date-time, dt-format-date--text, dt-format-date-time--text` - datum/čas, filtr zobrazí od-do
- `dt-format-link` - zobrazí text jako odkaz, možnost použít `renderFormatLinkTemplate`
- `dt-format-image` - zobrazí malý náhled obrázku a odkaz na jeho plné zobrazení, pod obrázkem je text odkazu na obrázek.
- `dt-format-mail` - zobrazit text jako e-mailový odkaz
- `dt-row-edit` - umožňuje editaci řádků

Pokud potřebujete, aby měl sloupec určitou (maximální) šířku, musíte ji nastavit pomocí CSS na obou řádcích v záhlaví pomocí stylu CSS. `max-width`. Příklad:

```css
.datatableInit {
    thead tr {
        th.dt-th-editorFields-statusIcons {
            width: 75px;
            max-width: 75px;
        }
    }
}
```

Nastavení `max-width` zajišťuje nastavení šířky sloupce. Datová tabulka vypočítá zbývající šířky. Pozor, pokud text přesáhne zadanou šířku, zdrží ostatní sloupce v tabulce sám, pak se šířka záhlaví a tabulky neshodují, je nutné v případě potřeby upravit šířku tabulky na dané buňce `overflow` Nemovitost. Potřebný styl CSS můžete do buňky přidat nastavením atributu `className` v anotaci.

### Zobrazení kódu HTML

Datovatelné ve výchozím nastavení `escapuje` HTML do entit, aby se zabránilo nechtěnému spuštění kódu HTML. Pokud potřebujete v buňce zobrazit kód HTML, je možné v atributu anotace nastavit, aby se v buňce zobrazoval kód HTML. `className` Styl CSS `allow-html`, který umožňuje spuštění kódu HTML v buňce. Při jeho používání však buďte opatrní, aby nemohlo dojít k chybě typu XSS.

```java
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="[[#{admin.conf_editor.value}]]",
        className = "allow-html"
    )
    private String value;
```

## Přidání/odebrání tlačítek

Prostřednictvím rozhraní API je možné v `toolbare` odebrat/přidat tlačítka:

```javascript
//odstranenie tlacitka (kazde tlacitko ma atribut dt-dtbtn podla ktoreho viete zistit jeho meno)
galleryTable.hideButton("create");
galleryTable.hideButton("import");
galleryTable.hideButton("export");

//pridanie tlacitka na 5 poziciu
let buttonCounter = 5;
galleryTable.button().add(buttonCounter++, {
    text: 'S',
    action: function (e, dt, node) {
        switchGallerySize(e, dt, node, 'S');
    },
    className: 'btn btn-outline-secondary btn-gallery-size active',
    attr: {
        'title': 'Size S'
    }
});

galleryTable.button().add(buttonCounter++, {
    text: '<i class="ti ti-list-details"></i>',
    action: function (e, dt, node) {
        console.log("btn, e=",e,"dt=",dt,"node=",node);
        //ziskaj data selectnuteho riadku
        let selectedRows = dt.rows({ selected: true }).data();
    },
    init: function ( dt, node, config ) {
        //zobraz tlacidlo aktivne iba ked je oznaceny aspon jeden riadok
        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
        //ALEBO ked je oznaceny PRESNE jeden riadok
        //$.fn.dataTable.Buttons.showIfOneRowSelected(this, dt);
    },
    className: 'btn btn-outline-secondary btn-gallery-size',
    attr: {
        //zobrazi tooltip po prechode mysou
        title: 'Table view',
        'data-toggle': 'tooltip'
    }
});

//wrapnutie 4 tlacitok do grupy (v galerii prepinanie velkosti SMLT)
$('.btn-gallery-size').wrapAll('<div class="btn-group-wrapper buttons-divider-both" data-toggle="tooltip" data-original-title="Veľkosť obrázkov"><div class="btn-group btn-group-toggle gallery-buttons-size" /></div>');

//znova zobrazenie tlacidla
galleryTable.showButton("export");
```

V `init` lze použít následující volání:
- `$.fn.dataTable.Buttons.showIfRowSelected(this, dt);` - Tlačítko je aktivní pouze v případě, že je vybrán alespoň jeden řádek.
- `$.fn.dataTable.Buttons.showIfRowUnselected(this, dt);` - tlačítko je aktivní pouze v případě, že není vybrán žádný řádek
- `$.fn.dataTable.Buttons.showIfOneRowSelected(this, dt);` - tlačítko je aktivní, pouze pokud je vybrán přesně jeden řádek.

## Tlačítko pro provedení akce serveru

Datová tabulka nabízí možnost přidat tlačítko pro provedení akce serveru (např. otočení obrázku, odstranění všech záznamů).

Funkce JS `nejakaTable.executeAction(action, doNotCheckEmptySelection, confirmText, noteText, customData = null, forceIds = null)` má parametry:
- `action` (String) - název akce, která má být odeslána na server k provedení.
- `doNotCheckEmptySelection` (true) - nastavením na `true` nekontroluje, zda jsou vybrány nějaké řádky, a do služby REST je odeslána hodnota -1 jako ID vybraného řádku. To je užitečné pro tlačítka, která nepotřebují mít vybrané řádky, např. obnovit všechny záznamy atd.
- `confirmText` (String) - pokud je zadáno, zobrazí se před provedením akce potvrzení (např. Are you sure you want to ...?).
- `noteText` (řetězec) - doplňkový text zobrazený nad tlačítky pro potvrzení akce (např. operace může trvat několik minut).
- `customData` - objekt přidaný do volání služby REST jako parametr `customData` (např. další údaje potřebné pro správné provedení akce).
- `forceIds` - číslo nebo pole čísel s hodnotou ID záznamu, pro který má být akce provedena. Používá se v případě, kdy je třeba kliknutím na ikonu stavu spustit akci (aniž by bylo nutné vybrat řádek).

Na serveru je provedeno volání služby REST. `/action/rotate` implementované v metodě [DatatableRestControllerV2.processAction](../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java). Seznam vybraných řádků (jejich ID) je odeslán službě REST, která je zpracována v metodě DatatablesRestControllerV2.action.

**Příklad použití** - přidáno tlačítko `toolbaru` nad datovou tabulkou s výzvou k akci:

```javascript
cacheObjectsTable.button().add(3, {
    extends: 'remove',
    editor: cacheObjectsTable.EDITOR,
    text: '<i class="ti ti-camera"></i>',
    action: function (e, dt, node) {
        cacheObjectsTable.executeAction("deletePictureCache", true, "[[\#{components.data.deleting.imgcache.areYouSure}]]", "[[\#{components.data.deleting.imgcache.areYouSureNote}]]");
    },
    className: 'btn btn-danger',
    attr: {
        'title': '[[\#{components.memory_cleanup.deleteImageCache}]]',
        'data-toggle': 'tooltip'
    }
});
```

Tlačítko také s kontrolou, že je vybrán nějaký řádek (v možnosti init):

```javascript
galleryTable.button().add(buttonCounter++, {
    extends: 'remove',
    editor: galleryTable.EDITOR,
    text: '<i class="ti ti-repeat"></i>',
    action: function (e, dt, node) {
        //console.log("Rotate, e=",e," dt=",dt," node=",node);
        galleryTable.executeAction("rotate");
    },
    init: function ( dt, node, config ) {
        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
    },
    className: 'btn btn-outline-secondary',
    attr: {
        'title': 'Otočiť',
        'data-toggle': 'tooltip'
    }
});
```

Akce vyvolá následující události:
- `WJ.DT.executeAction` - po úspěšném provedení akce.
- `WJ.DT.executeActionCancel` - po neúspěšném provedení akce nebo po kliknutí na tlačítko Zrušit pro potvrzení akce.

## Tlačítka podle práv

Pokud potřebujete zobrazovat tlačítka podle práv (např. tlačítko Přidat pouze v případě, že uživatel má určité právo), je možné přidat atribut do konfigurace datové tabulky. `perms`:

```javascript
webpagesDatatable = WJ.DataTable({
    url: webpagesInitialUrl,
    ...
    perms: {
        create: 'addPage',
        edit: 'pageSave',
        duplicate: 'pageSaveAs',
        remove: 'deletePage'
    }
});
```

definice v `perms` objekt definuje specifické právo na jméno pro každou operaci vytvoření (`create`), editace (`edit`), duplikace (`duplicate`) a vymazání (`remove`) záznamu.

Při nastavení práv se přestanou zobrazovat tlačítka na panelu nástrojů a nezobrazí se ani tlačítko pro uložení/přidání/odstranění v dialogovém okně editoru (tlačítka se skryjí, když je zobrazeno okno editoru).

Tabulka poskytuje rozhraní API pro ověření práva jako `TABLE.hasPermission(action)`:

```javascript
if (webpagesDatatable.hasPermission("create")) {
    ...
}
```

**Varování:** nespoléhejte se pouze na kontrolu práv na frontendu, práva je třeba kontrolovat také ve službě REST nebo ve třídě služby. Můžete použít metody [beforeSave nebo beforeDelete](restcontroller.md#zabránění-vymazání---editace-záznamu.).

## Stylování řádků

Někdy je nutné nastavit styl CSS pro celý řádek (např. tučné písmo pro hlavní stránku nebo červené pro nedostupnou stránku). K přenosu těchto dodatečných údajů používáme přenos vnořených atributů prostřednictvím objektu [EditorFields](../datatables-editor/datatable-columns.md#vnořené-atributy). Vytvořili jsme třídu [BaseEditorFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java) která má metodu `addRowClass(String addClass)` přidat na řádek třídu CSS.

Příkladem použití je [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java):

```java
...
public class DocEditorFields extends BaseEditorFields {
    public void fromDocDetails(DocDetails doc) {
        ...
        //hlavna stranka adresara
        if (groupDetails != null && doc.getDocId()>0 && groupDetails.getDefaultDocId()==doc.getDocId()) {
            addRowClass("is-default-page");
        }

        //vypnute zobrazovanie
        if (doc.isAvailable()==false) addRowClass("is-not-public");
    }
}
```

K dispozici jsou následující styly řádků CSS:
- `is-disabled` - představuje neaktivní položku, která je zobrazena červeně.
- `is-disapproved` - představuje neschválenou položku, vyznačenou červeně.
- `is-default-page` - představuje hlavní webovou stránku adresáře, která je zobrazena tučně.
- `is-not-public` - představuje neveřejnou položku, vyznačenou červeně.

Nastavení stylu řádků CSS je implementováno v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) pomocí možnosti `rowCallback` konstruktor datového souboru. Ověřuje existenci vlastnosti `data.editorFields.rowClass` a pokud existuje, použije tuto hodnotu na řádek.

Styl řádku můžete nastavit také v kódu JavaScriptu (např. na základě atributů) pomocí příkazu `onRowCallback`. Řádky tak můžete snadno označit jako neaktivní pomocí stylu CSS. `is-not-public`.

```javascript
domainRedirectTable = WJ.DataTable({
    url: '/admin/rest/settings/domain-redirect',
    columns: columns,
    serverSide: false,
    editorId: "redirectId",
    onRowCallback: function(TABLE, row, data) {
        if (data.active === false) $(row).addClass("is-not-public");
    }
});
```

## Stavové ikony

Někdy je nutné zobrazit stavové ikony záznamu (např. na webových stránkách ikony Nezobrazeno v nabídce, Přesměrovaná stránka atd.). K přenosu těchto dodatečných údajů používáme přenos vnořených atributů prostřednictvím objektu [EditorFields](../datatables-editor/datatable-columns.md#vnořené-atributy). Vytvořili jsme třídu [BaseEditorFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java) která má metodu `addStatusIcon(String className)`. Ikony jsou od FontAwesome.

![](../../redactor/webpages/status-icons.png)

Příkladem použití je [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java). Je nutné definovat atribut `statusIcons` s `@DataTableColumn` anotace pro zobrazení sloupce. Zobrazí se jako výběrové pole, v `options` doporučujeme definovat ikonu a popisný text. Stejně jako `value` jsou přenášeny podmínky vyhledávání (viz níže):

```java
...
public class DocEditorFields extends BaseEditorFields {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "webpages.icons.title",
        hiddenEditor = true, hidden = false, visible = true, sortAfter = "id", className = "allow-html", orderable = false,
        editor = { @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "<i class=\"ti ti-map-pin\"></i> [[#{webpages.icons.showInMenu}]]", value = "showInMenu:true"),
                @DataTableColumnEditorAttr(key = "<i class=\"ti ti-map-pin-off\"></i> [[#{webpages.icons.notShowInMenu}]]", value = "showInMenu:false"),
                @DataTableColumnEditorAttr(key = "<i class=\"ti ti-lock-filled\"></i> [[#{webpages.icons.onlyForLogged}]]", value = "passwordProtected:notEmpty"),
                @DataTableColumnEditorAttr(key = "<span style=\"color: #FF4B58\">[[#{webpages.icons.disabled}]]</span>", value = "available:false"),
                @DataTableColumnEditorAttr(key = "<i class=\"ti ti-external-link\"></i> [[#{webpages.icons.externalLink}]]", value = "externalLink:notEmpty"),
                @DataTableColumnEditorAttr(key = "<i class=\"ti ti-eye\"></i> [[#{webpages.icons.notSearchable}]]", value = "searchable:false")
            }
        )}
    )
    private String statusIcons;

    public void fromDocDetails(DocBasic doc, boolean loadSubQueries) {
        //ikony
        if (doc.isShowInMenu()) addStatusIcon("ti ti-map-pin");
        else addStatusIcon("ti ti-map-pin-off");
        if (Tools.isNotEmpty(doc.getExternalLink())) addStatusIcon("ti ti-external-link");
        if (doc.isSearchable()==false) addStatusIcon("ti ti-eye-off");
        if (Tools.isNotEmpty(doc.getPasswordProtected())) addStatusIcon("ti ti-lock-filled");
    }

    public getStatusIcons() {
        return getStatusIconsHtml();
    }
}
```

pokud potřebujete do stavových ikon něco programově přidat (v případě webových stránek je to odkaz na zobrazení stránky), můžete stavové ikony upravit přímo v kódu (v tomto případě neimplementujte metodu `getStatusIcons`):

```java
    public void fromDocDetails(DocDetails doc) {
        ...
        StringBuilder iconsHtml = new StringBuilder();

        //pridaj odkaz na zobrazenie stranky
        Prop prop = Prop.getInstance();
        String link = "/showdoc.do?docid="+doc.getDocId();
        if (doc instanceof DocHistory) {
            //v history je otocene docid a historyid
            link = "/showdoc.do?docid="+doc.getId()+"&historyId="+doc.getDocId();
        }
        iconsHtml.append("<a href=\""+link+"\" target=\"_blank\" title=\""+ResponseUtils.filter(prop.getText("history.showPage"))+"\"><i class=\"ti ti-eye\"></i></a> ");

        iconsHtml.append(getStatusIconsHtml());
        statusIcons = iconsHtml.toString();
        ...
    }
```

Vyhledávání po výběru možnosti filtru je implementováno v aplikaci `DatatableRestControllerV2.addSpecSearchStatusIcons` a je automaticky vyvolán při volání `addSpecSearch` (pokud tuto metodu rozšiřujete, musíte ji volat implicitně), úložiště musí rozšiřovat `JpaSpecificationExecutor`. V současné době jsou podporovány následující možnosti vyhledávání:
- `property:true` - hodnota atributu `property` Je `true`
- `property:false` - hodnota atributu `property` Je `false`
- `property:notEmpty` - hodnota atributu `property` není prázdný
- `property:empty` - hodnota atributu `property` je prázdný (null nebo '')
- `property:%text%` - hodnota atributu `property` obsahuje zadaný text (`like` vyhledávání)
- `property:!%text%` - hodnota atributu `property` neobsahuje zadaný text (`not like` vyhledávání)

## Zobrazení dat na základě práv

V definici sloupců je možné nastavit požadované právo pro zobrazení sloupce v datové tabulce nebo v editoru pomocí atributu `perms`. Příklad v souboru [redirect.pug](../../../src/main/webapp/admin/v9/views/pages/settings/redirect.pug):

```javascript
{
    data: "domainName",
    name: "domainName",
    title: "[[\#{groupedit.domain}]]",
    editor: {
        type: "text"
    },
    renderFormat: "dt-format-text",
    renderFormatLinkTemplate: "javascript:;",
    renderFormatPrefix: '<i class="ti ti-pencil"></i> ',
    className: "dt-row-edit",
    perms: "multiDomain" //stĺpec sa zobrazí len ak používateľ má právo multiDomain
},
```

WebJET generuje pole JS v kódu HTML při zobrazení stránky. `nopermsJavascript` který obsahuje seznam modulů, k nimž uživatel nemá práva. Generuje se také styl CSS s třídami `.noperms-menomodulu` se sadou `display: none`.

## Uspořádání

Datatable podporuje nastavení řazení podle atributu [záruku:](https://datatables.net/reference/option/order). To lze převést jako `option` při inicializaci tabulky. Ale kvůli parsování pugjs/thymeleaf není možné zapsat výraz přímo. `[[0, 'asc']]` protože Thymeleaf ji provede. Je nutné připravit pole rozvržení oklikou přes proměnnou a push:

```javascript
var order = [];
order.push([5, 'desc']);

configurationDatatable = WJ.DataTable({
    url: "/admin/v9/settings/configuration",
    columns: columns,
    order: order
});
```

takže parser Thymeleaf je "oklamán" a pole layout je správně definováno.

## Vyhledávání

**Filtrování značek HTML**

Datovatelné na **místní vyhledávání** (neplatí pro vyhledávání na serveru) ve výchozím nastavení filtruje značky HTML a vyhledává pouze v textu (ignoruje obsah značek HTML). To je nežádoucí stav pro pole typu `textarea` kde se zadává kód HTML (např. kód skriptu v aplikaci Skripty). V důsledku toho vyhledávání nenajde výraz v kódu HTML.

Do souboru index.js je přidán typ vyhledávání html-input, který nefiltruje značky HTML. `$.fn.dataTableExt.ofnSearch['html-input'] = function(value)...`. V `columnDefs` je automaticky nastaven pro sloupce se stylem CSS `dt-format-text-wrap` (nastaveno automaticky pomocí anotace `DataTableColumnType.TEXTAREA`) nebo `html-input`.

## Externí filtr

Kromě zobrazení filtrů v záhlaví každého sloupce tabulky můžete přidat samostatné pole filtru kdekoli v kódu HTML stránky. Příkladem je [Odstranění záznamů v databázi](../../../src/main/webapp/admin/v9/views/pages/settings/database-delete.pug) kde je filtr přesunut přímo do záhlaví stránky k nadpisu.

V souboru pug je třeba připravit základní strukturu HTML vytvořením kontejneru div s ID `TABLEID_extfilter`. Vyhledává prvky div s třídou CSS `dt-extfilter-title-FIELD` do kterého se vloží název sloupce a `dt-extfilter-FIELD` do kterého je vyhledávací pole vloženo.

```
div#dateDependentEntriesTable_extfilter
    div.row.datatableInit
        div.col-auto.dt-extfilter-title-from
        div.col-auto.dt-extfilter.dt-extfilter-from
```

**Varování:** v prvku pro vyhledávací pole je třída CSS `.dt-extfilter` Také `.dt-extfilter-FIELD`, je třeba použít obě. Podle třídy CSS `.dt-extfilter` prvek se nachází po kliknutí na lupu v atributu data. `data-column-index` je uloženo pořadové číslo sloupce.

Pokud chcete filtr přesunout do záhlaví stránky, můžete jej jednoduše přesunout pomocí jQuery, jako v případě [database-delete.pug](../../../src/main/webapp/admin/v9/views/pages/settings/database-delete.pug).

**Poznámky k implementaci**

Kliknutím na ikonu lupy se zadaný text filtru přenese do datového filtru a uloží se do objektu. `TABLE.DATA.columns[inputIndex].searchVal`. Tato funkce je k dispozici pro volání AJAX. Ve funkci `datatable2SpringData` pak se hledají hodnoty `.searchVal` pro externí filtr, a pokud jsou nastaveny, jsou přidány do parametrů vyhledávání pro požadavek AJAX.

Toto řešení bylo zvoleno z důvodu předvyužití existujícího kódu pro výpočet hledané hodnoty (zejména pro data), zároveň sloupce využívající externí filtr mohou mít nastaven atribut `filter=false` v `@DatatableColumn anotácii`.

## Export/import

Implementoval systém pro import a export dat mezi datovými tabulkami. Pro každou datovou tabulku po jejím vytvoření a nastavení ověřte funkčnost importu a exportu. Ověřte také všechny možnosti importu, včetně porovnávání podle sloupců. Pokud nechcete používat export/import, vypněte tlačítka pomocí kódu (datatableInstance je název instance datové tabulky):

```javascript
datatableInstance.hideButton("import");
datatableInstance.hideButton("export");
```

Pokud potřebujete vynechat některý sloupec z exportu, stačí jej nastavit/přidat do položky `columns` atribut `className` hodnota `not-export`:

```java
@DataTableColumn(
    inputType = DataTableColumnType.TEXT,
    title="[[#{components.banner.fieldName}]]",
    className = "not-export"
)
private String fieldName;
```

Další informace naleznete v dokumentaci k [Vývojář](export-import.md) nebo pro [editora](../../redactor/datatables/export-import.md).

## Volání API

```javascript
//zoznam selectnutych riadkov
galleryTable.rows( { selected: true }).data();
//zmena URL adresy
galleryTable.ajax.url("/admin/rest/nova-url");
//refresh dat
galleryTable.ajax.reload();
//nastavenie filtra a reload dat
galleryTable.columns(3).search("^"+virtualPath+"$").draw();

//nastavenie JSON dat do aktualneho editora
EDITOR.setJson(json);
//aktualne editovane data (json objekt)
EDITOR.currentJson

//options z odpovede REST služby pre rendering (potrebujeme pre export číselníkových dáta)
TABLE.DATA.jsonOptions
//kompletná URL adresa posledného REST volania
TABLE.DATA.urlLatest
//všetky parametre posledného REST volania (aktuálna stránka, veľkosť stránky, filtre)
TABLE.DATA.urlLatestParams

//schovanie/zobrazenie tlacidla - name je hodnota atributu data-dtbtn button elementu
TABLE.hideButton(name);
TABLE.hideButtons(['name1', 'name2']);
TABLE.showButton(name);

//deaktivuje rezim editacie bunky (ak je zapnuty) - ak cez karty prepinate obsah datatabulky vzdy deaktivujte rezim editacie bunky
TABLE.cellEditOff()

/**
 * Vypocita/prepocita velkost stranky (zobrazeny pocet zaznamov)
 * @param {*} updateLengthSelect - ak je true aj sa reloadnu udaje (napr. pri zmene velkosti obrazka v galerii)
 */
TABLE.calculateAutoPageLength(updateLengthSelect)
```

## Ukázky kódu

### Naslouchání události obnovení tabulky:

Klikněte na tlačítko `reload` spustí událost `WJ.DTE.forceReload` kterému můžete naslouchat a např. aktualizovat stromovou strukturu:

```javascript
window.addEventListener('WJ.DTE.forceReload', (e) => {
    //console.log("FORCE RELOAD listener, e=", e);
    $('#SomStromcek').jstree(true).refresh();
}, false);
```
