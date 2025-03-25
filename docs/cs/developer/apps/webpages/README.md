# Web stránky

Sekce web stránky je specifická v tom, že obsahuje více datatabulek:
- `webpagesDatatable` - hlavní datatabulka pro seznam web stránek
- `groupsDatatable` - skrytá (`display:none`) datatabulka pro editaci položky složky (synchronizovaná se stromovou strukturou)
- `mediaTable` - zobrazuje připojená média k web stránce v editoru stránky
- `historyTable` - zobrazuje historické záznamy (změny) v editoru web stránky

## Historie web stránek

Historie web stránek je implementována jako vnořená datatabulka v editoru stránek. Inicializována je ve třídě [DocEditorFields.java](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) na objektu `private List<DocHistoryDto> history;`.

### REST rozhraní

REST rozhraní je dostupné na URL adrese `/admin/rest/web-pages/history?docId={docId}&groupId={groupId}` a implementováno je ve třídě [DocHistoryRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryRestController.java).

Využívá [mapování](../../backend/mapstruct.md) objektů `DocDetails` na jednodušší DTO objekt. V datatabulce se zobrazují pouze základní údaje.

Přístup k historii je **jen ke čtení**, v REST službě je zápis zakázán v metodě `beforeDelete` a `beforeSave`.

### Frontend

Datatabulka je upravena poslechem události `WJ.DTE.innerTableInitialized`, ten se jmenuje pro každou datatabulku, ověřením přes `id` se kód provede pouze pro tabulku historie. Inicializovaný objekt je přiřazen do globální proměnné `historyTable`.

```javascript
window.addEventListener("WJ.DTE.innerTableInitialized", function(event) {
    if (event.detail.conf.id=="DTE_Field_editorFields.history") {
        historyTable = event.detail.conf.datatable;
        historyTable.hideButton("create");
        ...
    }
});
```

Po inicializaci jsou schovaná nepotřebná tlačítka voláním `historyTable.hideButton`.

Komplikovaná úprava je ve způsobu načítání dat z historie do editoru. Editoru je třeba podvrhnout JSON data načtená podle zvoleného ID z historie. Zajišťuje to následující kód tlačítka editace:

```javascript
historyTable.button().add(buttonCounter++, {
    text: '<i class="ti ti-pencil"></i>',
    action: function (e, dt, node) {
        //console.log("btn, e=",e,"dt=",dt,"node=",node);
        //ziskaj data selectnuteho riadku
        let selectedRows = dt.rows({ selected: true }).data();
        //console.log("selectedRows=", selectedRows);
        if (selectedRows.length>0) {
            let row = selectedRows[0];
            //HACK, potrebujeme ziskat dany zaznam ako JSON a podvrhnut ho do datatabulky
            $.ajax({
                url: "/admin/rest/web-pages/"+row.docId+"?historyId="+row.id,
                method: "GET",
                success: function(json) {
                    //console.log("Edit JSON", json);
                    let oldJson = webpagesDatatable.row("#"+row.docId).data();
                    webpagesDatatable.row("#"+row.docId).data(json);
                    webpagesDatatable.EDITOR.setJson(json);
                    webpagesDatatable.wjEdit(webpagesDatatable.row("#"+row.docId));
                    setTimeout(function() {
                        //console.log("returning oldJson=", oldJson);
                        webpagesDatatable.row("#"+row.docId).data(oldJson);
                    }, 100);
                }
            })
        }
    },
    init: function ( dt, node, config ) {
        $.fn.dataTable.Buttons.showIfOneRowSelected(this, dt);
    },
    className: 'btn btn-warning buttons-divider',
    attr: {
        'title': '[[\#{history.editPage}]]',
        'data-toggle': 'tooltip'
    }
});
```

Pomocí ajax požadavky se zadaným `historyId` se získají data web stránky z historie. Ty jsou nastaveny v datatabulce řádku podle `docId` (což by měl být aktuálně editován řádek). S novými daty je otevřený editor voláním funkce `wjEdit` a následně jsou do řádku datatabulky vrácena původní data uložená v objektu `oldJson` (jinak by se po zavření okna editoru zobrazila historická data v datatabulce web stránek).

V `init` je nastavena možnost `showIfOneRowSelected` která tlačítko zobrazí jen pokud je zvolen přesně jeden řádek.

Tlačítka pro zobrazení náhledu stránky a porovnání procházejí všechny zvolené řádky a otevírají do nového okna náhled, nebo porovnání stránky podle původní funkcionality ve WebJET 8. Porovnání stránek vždy zobrazí porovnání zvoleného řádku vůči aktuální verzi (nikoli srovnání zvolených řádků).

```javascript
action: function (e, dt, node) {
    //console.log("btn, e=",e,"dt=",dt,"node=",node);
    //ziskaj data selectnuteho riadku
    let selectedRows = dt.rows({ selected: true }).data();
    console.log("selectedRows=", selectedRows);
    for (let i=0; i<selectedRows.length; i++) {
        let row = selectedRows[i];
        window.open("/showdoc.do?docid="+row.docId+"&historyid="+row.id);
    }
}
```

## Optimalizace rychlosti zobrazení

Jelikož správa web stránek je základní funkcionalita CMS systému snažili jsme se optimalizovat její rychlost načítání a zobrazení. Z toho důvodu jsou použity specifické funkce a kód.

### Volání REST služeb

Inicializační JSON data jsou vložena přímo do stránky přes třídu [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java). Při prvotním zobrazení stránky se tedy nemusí čekat na volání REST služeb. V kódu stránky se použijí jako:

```javascript
window.treeInitialJson = [(${treeInitialJson})];
let groupsInitialJson = [(${groupsInitialJson})];
let webpagesInitialJson = [(${webpagesInitialJson})];
...
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    initialData: webpagesInitialJson,
}
...
groupsDatatable = WJ.DataTable({
    id: 'groups-datatable',
    url: groupsUrl,
    initialData: groupsInitialJson,
}
```

`jstree` objekt automaticky hledá `window.treeInitialJson` a použije jej místo zadané URL adresy. Zadané URL adresy pro REST služby se použijí až pro další volání.

### Optimalizace načítání adresáře

Pro editaci adresáře web stránky se využívá schovaná datatabulka. Data se standardně čtou pro data `jstree` stromové struktury. Ty obsahují `original` objekt, ve kterém je fakticky `GroupDetails`. Tento objekt se uměle vkládá do datatabulky adresáře ve funkci `loadTablesForGroup` jako:

```javascript
if (groupsDatatable.rows().count()==0) {
    groupsDatatable.row.add(window.lastGroup);
} else {
    groupsDatatable.row(0).data(window.lastGroup);
}
groupsDatatable.draw();
```

nemusí se tedy jmenovat REST služba pro načítání dat. Toto samotné ale nepomůže, protože datatabulka nemá hodnoty pro výběrová pole, jelikož uměle vložený objekt tyto údaje neobsahuje.

Načtení je tedy implementováno jako volání obnovení datatabulky a následné otevření editace/přidání záznamu. Tlačítka pro editaci a přidání nového záznamu jsou zobrazena nad stromovou strukturou. Klepnutí na tato tlačítka vyvolá nastavení proměnné `groupsDatatableClickButtonAfterXhr` na hodnotu tlačítka, které se má zavolat po obnovení dat (např. `"buttons-create"`). `groupsDatatable` má nastavenou funkci `onXhr`, která po načtení údajů ověřuje stav proměnné `groupsDatatableClickButtonAfterXhr`. Pokud není `null` tak vyvolá kliknutí na zadané tlačítko.

Proces je tedy následující:
- při kliknutí na uzel ve stromové struktuře se nastaví objekt do datatabulky a nastaví se její nové URL podle `groupId`
- při klepnutí na tlačítko Přidat nebo Upravit složku nad stromovou strukturou se zapamatuje jméno tlačítka v proměnné `groupsDatatableClickButtonAfterXhr` a vyvolá se tlačítko `buttons-refresh` v datatabulce pro načtení dat ze serveru
- po načtení dat ze serveru se ověří stav v proměnné `groupsDatatableClickButtonAfterXhr` a pokud není `null`, tak se vyvolá zadané tlačítko v datatabulce (což otevře editor adresáře).

### Optimalizace načítání JavaScript souborů

Web stránky používají `ckeditor`, jehož JavaScript soubor je třeba načíst. Je to ovšem poměrně velký soubor a jeho zpracování zvyšuje zátěž procesoru a následně zpomaluje zobrazení web stránky. Tento soubor se tedy načte asynchronně po 2 sekundách po inicializaci DOM ve funkci `window.domReady.add`:

```javascript
setTimeout(()=> {
    //nechceme zatazit CPU hned na zaciatku, takze ckeditor.js nacitame cez timenout mierne neskor
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = "/admin/skins/webjet8/ckeditor/dist/ckeditor.js";

    head.appendChild(script);
}, 2000);
```

Problémem ale následně může být otevření okna editoru stránek před načtením `ckeditor.js`, ošetřeno to je tedy přímo ve funkci otevření okna, kde se testuje objekt `window.CKEDITOR` a pokud neexistuje, tak se inicializace pozdrží přes funkci `setTimeout`.

```javascript
webpagesDatatable.EDITOR.on( 'open', function ( e, type ) {
    ...
    if (typeof window.CKEDITOR == "undefined") {
        //este nedobehol load ckeditora
        setTimeout(() => {
            if (typeof window.CKEDITOR != "undefined") wjeditor = new module.DatatablesCkEditor(options);
        }, 2100);
    } else {
        wjeditor = new module.DatatablesCkEditor(options);
    }
    ...
}
```

## Zobrazení podle docid parametru

Zobrazení podle zadaného `docid` URL parametru (nebo zadaného `docid` přes textové pole) využívá knihovnu [js-tree-auto-opener](../../libraries/js-tree-auto-opener.md). Automatické otevření získá seznam ID adresářů voláním REST služby `/admin/rest/web-pages/parents/{id}`. Získaný seznam ID adresářů se následně snaží otevřít (postupným voláním REST služby pro získání údajů podadresářů). Po otevření všech adresářů otevře k editaci web stránku.

Vstupní pole pro zadání `docid` ve stránce je implementováno přímo ve [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) z před připraveného HTML kódu `#docIdInputWrapper`, který je přesunut do kontejneru nástrojové lišty datatabulky. Po stisku klávesy `Enter` je vyvolána akce otevření zadaného ID stránky.

Na vstupní pole je napojen `autocomplete` vyhledávání podle názvu stránky nebo její URL adresy nastavením `data-ac` atributů.

!>**Upozornění:** aktuální implementace nedokáže otevřít web stránku na druhé straně seznamu web stránek (jelikož ji nenajde v seznamu). Také pokud je zadáno `docid` v adresáři Systém nebo Koš a tento list není aktuálně zobrazen, také nedokáže adresář zobrazit.

## Zobrazení karty Naposledy upraveno

Po kliknutí na kartu Naposledy upravené se načte do datatabulky seznam naposledy upravených stránek od všech uživatelů. Klepnutí na jméno je zpracováno ve funkci `webpagesDatatable.onEdit`. Jelikož stránky v seznamu mohou být v různých adresářích neotevře se přímo, ale jejich ID je zadáno do pole `Doc ID` a následně načteno včetně stromové struktury.

Klepnutí na list načte obsah datatabulky jakoby pro adresář nastavený v konfigurační proměnné `systemPagesRecentPages`, který se kontroluje ve třídě [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java) a pokud se ID adresáře shoduje jmenuje se načtení seznamu posledních stránek.

## Zobrazení karty Čekající na schválení

Zobrazení seznamu v kartě čekající na schválení je komplikovanější. Na backendu se kontroluje, zda pro aktuálně přihlášeného uživatel existují stránky, které má schválit. Pokud ano, nastaví se ve třídě [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java) do modelu atribut `hasPagesToApprove` na hodnotu `TRUE`. Tento se testuje ve [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) a je-li `FALSE` tak se list skryje.

Po kliknutí na kartu se do datatabulky načte jakoby adresář s ID nastaveným v konfigurační proměnné `systemPagesDocsToApprove`, co se kontroluje ve třídě [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java). Načtení komplikuje fakt, že jako unikátní ID se používá hodnota `docId`. Pro schvalování ale může existovat vícero záznamů pro stejné `docId`. Jako nejjednodušší řešení jsme otočili hodnotu `docId` a `historyId`, které se navzájem v přenášených datech vymění.

Problémem je zavolání obnovení dat po kliknutí, které volá `WebpagesRestController.getOne`, kde je zaslána hodnota `historyId` jak `id`. Podle zaslaného `id` (což je ve skutečnosti `historyId`) získáme dotazem do databáze korektní hodnotu `docId` pro získání obsahu podle kombinace `docId, historyId`.

Klepnutí je obslouženo ve funkci `webpagesDatatable.onEdit` otevřením nového okna `/admin/approve.jsp` se schvalovacím dialogem.

Pro tabulku naposledy upravené i čekající na schválení se schovanou tlačítka pro přidání/editaci/smazání záznamu. Je to ošetřeno poslechem události přepnutí karty `$('#pills-pages a[data-toggle="pill"]').on('shown.bs.tab', function (e) {`.

## Náhled stránky

Náhled stránky implementuje třída `EditorPreview` v souboru `src/js/pages/web-pages-list/preview.js`. Funkce `window.previewPage` nastavuje atribut `isPreviewClicked` a provede API volání pro uložení editoru. Zároveň poslouchá událost `preSubmit` při které detekuje stav atributu `isPreviewClicked`.

Je-li nastaven na `true` získá JSON data z editoru a uloží je do session objektu `session.setAttribute("ShowdocAction.showDocData", entity)` voláním `/admin/rest/web-pages/preview/`. Následně otevře novou kartu v prohlížeči s adresou `/admin/webpages/preview/?docid=+jsonData.id`, kterou obsluhuje třída `PreviewController.java`. Ta získá ze session DocDetails objekt `DocDetails doc = (DocDetails)request.getSession().getAttribute("ShowdocAction.showDocData");`, nastaví potřebné `request` objekty a provede volání `/showdoc.do` pro zobrazení web stránky. Toto ale nezíská aktuální objekt z databáze, ale použije objekt `request.getAttribute("ShowdocAction.showDocData")` pro zobrazení údajů.

Zároveň poslouchá událost `webpagesDatatable.EDITOR.on('postSubmit'`, pomocí které aktualizuje obsah okna náhledu při uložení web stránky (entita se uloží do `session` voláním `WebpagesRestController.afterSave`). Toto se provede pouze pokud je okno náhledu stále otevřeno, to se testuje pomocí `self.previewWindow.name != ""`. Toto zůstane prázdné, pokud okno uživatel zavře.

Tlačítko Náhled je do patičky okna vloženo při události `webpagesDatatable.EDITOR.on('open'`.

## Uložení klávesovou zkratkou

Web stránka lze uložit pomocí klávesové zkratky `CTRL+s/CMS+s`, což je inicializováno přímo v datatabulce funkcí `bindKeyboardSave`. Technicky po stisku klávesové zkratky je vyvolána událost `WJ.DTE.save` na kterou se poslouchá a tato událost provede fyzické uložení (volání `EDITOR.submit`). Při takovém uložení je okno ponecháno v otevřeném stavu, aby uživatel mohl pokračovat v práci.

U web stránek ale nastal problém v tom, že samotná editace probíhá v `iframe` elemente. Událost tedy nebyla korektně nastavena. Událost je třeba přidat jak pro ckeditor, tak pro PageBuilder manuálně a vyvolat událost v rodičovském rámci.

V `/admin/v9/src/js/datatables-ckeditor.js afterInit` je přidána událost:

```javascript
//zachytenie CTRL+S/CMD+S
var editor = this.ckEditorInstance;
editor.on( 'contentDom', function(e) {
    var editable = editor.editable();
    //console.log("Som contentDown");
    editable.attachListener( editable, "keydown", function(evt) {
        //console.log("keydown, evt=", evt);
        var keyEvent = evt.data.$;
        if ((window.navigator.platform.match("Mac") ? keyEvent.metaKey : keyEvent.ctrlKey)  && keyEvent.key === 's') {
            //console.log("IFRAME CTRL+S, evt=", evt);

            keyEvent.preventDefault();
            try {
                window.top.WJ.dispatchEvent("WJ.DTE.save", {});
            } catch (ex) {}
        }
    });
});
```

podobně pro PageBuilder v `/admin/inline/inline.js.jsp`:

```javascript
$(document).ready(function() {
	document.addEventListener("keydown", function(e) {
		//zachytenie CTRL+S/CMD+S
		if ((window.navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)  && e.key === 's') {
			e.preventDefault();
			//console.log("Dispatching WJ.DTE.save");
			try {
				window.top.WJ.dispatchEvent("WJ.DTE.save", {});
			} catch (ex) {}
		}
	}, false);
});
```

Taková inicializace korektně nastaví poslech události stisku klávesové zkratky.
