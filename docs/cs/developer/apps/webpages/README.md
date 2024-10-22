# Webové stránky

Sekce webové stránky je specifická tím, že obsahuje více datových tabulek:
- `webpagesDatatable` - hlavní datová tabulka pro seznam webových stránek
- `groupsDatatable` - skrytý (`display:none`) datová tabulka pro úpravu položky složky (synchronizovaná se stromovou strukturou)
- `mediaTable` - zobrazí média připojená k webové stránce v editoru stránky.
- `historyTable` - zobrazuje historické záznamy (změny) v editoru webové stránky.

## Historie webových stránek

Historie webové stránky je implementována jako vnořená datová tabulka v editoru stránky. Je inicializována ve třídě [DocEditorFields.java](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) na objektu `private List<DocHistoryDto> history;`.

### Rozhraní REST

Rozhraní REST je k dispozici na adrese URL `/admin/rest/web-pages/history?docId={docId}&groupId={groupId}` a je implementována ve třídě [DocHistoryRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryRestController.java).

Používá [mapování](../../backend/mapstruct.md) zařízení `DocDetails` na jednodušší objekt DTO. V datové tabulce se zobrazují pouze základní údaje.

Přístup k historii je **pouze pro čtení**, ve službě REST je zápis zakázán v metodě `beforeDelete` a `beforeSave`.

### Frontend

Datová tabulka je upravena nasloucháním události `WJ.DTE.innerTableInitialized`, který se volá pro každou datovou tabulku, ověřením prostřednictvím `id` kód se provede pouze pro tabulku historie. Inicializovaný objekt je přiřazen do globální proměnné `historyTable`.

```javascript
window.addEventListener("WJ.DTE.innerTableInitialized", function(event) {
    if (event.detail.conf.id=="DTE_Field_editorFields.history") {
        historyTable = event.detail.conf.datatable;
        historyTable.hideButton("create");
        ...
    }
});
```

Po inicializaci se nepotřebná tlačítka skryjí voláním `historyTable.hideButton`.

Složitá úprava spočívá ve způsobu načítání dat historie do editoru. Do editoru se musí načítat data JSON podle vybraného ID historie. O to se stará následující kód editačního tlačítka:

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

Použití požadavku ajax se zadaným `historyId` data webové stránky se načítají z historie. Ty jsou nastaveny v datovém řádku pomocí `docId` (což by měl být aktuálně upravený řádek). S novými daty se editor otevře voláním funkce `wjEdit` a poté jsou původní data uložená v objektu vrácena do řádku datové tabulky. `oldJson` (jinak by se po zavření okna editoru historická data zobrazovala v datové tabulce webových stránek).

V `init` možnost je nastavena `showIfOneRowSelected` které se zobrazí pouze v případě, že je vybrán přesně jeden řádek.

Tlačítka náhledu stránky a porovnání procházejí všechny vybrané řádky a otevírají nové okno náhledu stránky nebo porovnání podle původní funkce v aplikaci WebJET 8. Funkce Porovnání stránky vždy zobrazí porovnání vybraného řádku s aktuální verzí (nikoli porovnání vybraných řádků).

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

Protože správa webových stránek je základní funkcí systému CMS, snažili jsme se optimalizovat rychlost jejich načítání a zobrazování. Z tohoto důvodu jsou použity specifické funkce a kód.

### Volání služeb REST

Inicializační data JSON jsou vložena přímo do stránky prostřednictvím třídy [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java). Při prvním zobrazení stránky tedy není třeba čekat na volání služeb REST. V kódu stránky se používají jako:

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

`jstree` objekt automaticky vyhledá `window.treeInitialJson` a použijte ji místo zadané adresy URL. Zadané adresy URL pro služby REST se použijí pouze pro následná volání.

### Optimalizace načítání adresářů

K úpravě adresáře webové stránky se používá skrytá datová tabulka. Data se ve výchozím nastavení načítají pro data `jstree` stromovou strukturu. Ty obsahují `original` objekt, ve kterém je fakticky `GroupDetails`. Tento objekt je uměle vložen do datové tabulky adresáře v adresáři `loadTablesForGroup` Stejně jako:

```javascript
if (groupsDatatable.rows().count()==0) {
    groupsDatatable.row.add(window.lastGroup);
} else {
    groupsDatatable.row(0).data(window.lastGroup);
}
groupsDatatable.draw();
```

aby nemusel volat službu REST pro načtení dat. To však samo o sobě nepomůže, protože datová tabulka nemá hodnoty pro výběrová pole, protože uměle vložený objekt tato data neobsahuje.

Načítání je tedy realizováno jako volání pro obnovení datové tabulky a následné otevření záznamu pro úpravu/přidání. Tlačítka pro editaci a přidání nového záznamu se zobrazují nad stromovou strukturou. Kliknutí na tato tlačítka vyvolá nastavení proměnné `groupsDatatableClickButtonAfterXhr` na hodnotu tlačítka, které má být vyvoláno po obnovení dat (např. `"buttons-create"`). `groupsDatatable` má funkci množiny `onXhr`, který po načtení dat ověří stav proměnné. `groupsDatatableClickButtonAfterXhr`. Pokud není `null` aby se spustilo kliknutí na zadané tlačítko.

Postup je tedy následující:
- při kliknutí na uzel ve stromové struktuře se objekt nastaví na datovou tabulku a jeho nová adresa URL se nastaví podle následujícího postupu `groupId`
- při kliknutí na tlačítko Přidat nebo Upravit složku nad stromovou strukturou se zapamatuje název tlačítka v proměnné. `groupsDatatableClickButtonAfterXhr` a tlačítko se bude jmenovat `buttons-refresh` v datové tabulce pro načtení dat ze serveru
- po načtení dat ze serveru se zkontroluje stav proměnné. `groupsDatatableClickButtonAfterXhr` a pokud není `null`, je vyvoláno zadané tlačítko v datové tabulce (které otevře editor adresáře).

### Optimalizace načítání souborů JavaScript

Webové stránky používané `ckeditor` jehož soubor JavaScript je třeba načíst. Jedná se však o poměrně velký soubor, jehož zpracování zvyšuje zatížení procesoru a následně zpomaluje zobrazení webové stránky. Proto se tento soubor načítá asynchronně po 2 sekundách po inicializaci DOM ve funkci `window.domReady.add`:

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

Problémem však může být otevření okna editoru stránky před načtením `ckeditor.js`, takže je ošetřena přímo ve funkci pro otevření okna, kde je objekt testován. `window.CKEDITOR` a pokud neexistuje, je inicializace odložena pomocí funkce `setTimeout`.

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

## Zobrazení podle parametru docid

Zobrazení podle zadání `docid` parametr URL (nebo zadaný `docid` prostřednictvím textového pole) používá knihovnu [js-tree-auto-opener](../../libraries/js-tree-auto-opener.md). Automatické otevírání získá seznam ID adresářů voláním služby REST. `/admin/rest/web-pages/parents/{id}`. Poté se pokusí otevřít získaný seznam ID adresářů (postupným voláním služby REST pro získání údajů o podadresářích). Po otevření všech adresářů otevře webovou stránku pro editaci.

Vstupní pole pro zadávání `docid` ve stránce je implementován přímo v [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) z předpřipraveného kódu HTML `#docIdInputWrapper`, který je přesunut do kontejneru datového panelu nástrojů. Po stisknutí tlačítka `Enter` je vyvolána akce otevření zadaného ID stránky.

Vstupní pole je připojeno k `autocomplete` vyhledávání podle názvu stránky nebo adresy URL nastavením `data-ac` Atributy.

**Varování:** aktuální implementace nemůže otevřít webovou stránku na druhé straně seznamu webových stránek (protože ji v seznamu nenajde). Také pokud je `docid` v adresáři Systém nebo Koš a tento list není aktuálně zobrazen, nezobrazí se ani tento adresář.

## Zobrazit kartu Naposledy upraveno

Kliknutím na kartu Naposledy změněné se do datové tabulky načte seznam naposledy změněných stránek od všech uživatelů. Kliknutí na jméno se zpracovává v záložce `webpagesDatatable.onEdit`. Protože stránky v seznamu mohou být v různých adresářích, neotevírají se přímo, ale jejich ID se zadává do pole `Doc ID` a následně načteny včetně stromové struktury.

Kliknutím na list se načte obsah datové tabulky jako pro adresář nastavený v konfigurační proměnné. `systemPagesRecentPages` který je kontrolován ve třídě [Webové stránkySlužba](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java) a pokud se shoduje s ID adresáře, je zavolán pro načtení seznamu posledních stránek.

## Zobrazení karty Čekající na schválení

Zobrazení seznamu na kartě čekajících je složitější. Backend kontroluje, zda existují stránky, které má aktuálně přihlášený uživatel schválit. Pokud ano, je to nastaveno ve třídě [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java) atribut v modelu `hasPagesToApprove` na hodnotu `TRUE`. To se testuje v [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) a pokud `FALSE` aby se písmeno skrylo.

Po kliknutí na záložku se do datové tabulky načte adresář s ID nastaveným v konfigurační proměnné, jako kdyby `systemPagesDocsToApprove` co se kontroluje ve třídě [Webové stránkySlužba](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java). Načítání je komplikováno tím, že hodnota se používá jako jedinečné ID. `docId`. Může však existovat více záznamů pro stejnou položku. `docId`. Jako nejjednodušší řešení jsme změnili hodnotu `docId` a `historyId` které jsou vyměňovány v přenášených datech.

Problémem je volání obnovení dat po kliknutí, které volá `WebpagesRestController.getOne` kde je hodnota odeslána `historyId` Stejně jako `id`. Podle zaslaných `id` (což je ve skutečnosti `historyId`) získáme správnou hodnotu dotazem do databáze. `docId` získat obsah podle kombinace `docId, historyId`.

Kliknutí se provádí ve funkci `webpagesDatatable.onEdit` otevřením nového okna `/admin/approve.jsp` se schvalovacím dialogem.

V tabulce posledních změn i v tabulce čekajících záznamů byla skryta tlačítka přidat/upravit/odstranit záznam. To je řešeno nasloucháním události přepnutí karty `$('#pills-pages a[data-toggle="pill"]').on('shown.bs.tab', function (e) {`.

## Náhled stránky

Náhled stránky je implementován třídou `EditorPreview` v souboru `src/js/pages/web-pages-list/preview.js`. Funkce `window.previewPage` nastaví atribut `isPreviewClicked` a provede volání API pro uložení editoru. Také naslouchá události `preSubmit` kde zjišťuje stav atributu `isPreviewClicked`.

Pokud je nastavena na `true` získá data JSON z editoru a uloží je do objektu relace `session.setAttribute("ShowdocAction.showDocData", entity)` zavoláním `/admin/rest/web-pages/preview/`. V prohlížeči se pak otevře nová karta s adresou `/admin/webpages/preview/?docid=+jsonData.id` provozované třídou `PreviewController.java`. Získá objekt DocDetails z relace DocDetails `DocDetails doc = (DocDetails)request.getSession().getAttribute("ShowdocAction.showDocData");`, nastaví potřebné `request` objekty a volá `/showdoc.do` zobrazit webové stránky. To však nezíská aktuální objekt z databáze, ale použije objekt `request.getAttribute("ShowdocAction.showDocData")` pro zobrazení dat.

Současně posloucháte událost `webpagesDatatable.EDITOR.on('postSubmit'`, který aktualizuje obsah okna náhledu při uložení webové stránky (entita je uložena do adresáře `session` zavoláním `WebpagesRestController.afterSave`). To se provádí pouze v případě, že je okno náhledu stále otevřené, což se testuje pomocí `self.previewWindow.name != ""`. Pokud uživatel okno zavře, zůstane prázdné.

Tlačítko Náhled je vloženo do zápatí okna při události `webpagesDatatable.EDITOR.on('open'`.

## Ukládání klávesových zkratek

Webovou stránku lze uložit pomocí klávesové zkratky `CTRL+s/CMS+s`, který je inicializován přímo v datové tabulce funkcí `bindKeyboardSave`. Technicky vzato se po stisknutí klávesové zkratky spustí událost. `WJ.DTE.save` a tato událost provede fyzické uložení (volání `EDITOR.submit`). Při ukládání tímto způsobem zůstane okno otevřené, takže uživatel může pokračovat v práci.

Problémem webových stránek je však to, že samotné úpravy probíhají v prostředí `iframe` prvek. Událost tedy nebyla správně nastavena. Událost je třeba přidat pro ckeditor i PageBuilder ručně a událost je třeba vyvolat v nadřazeném frameworku.

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

Tato inicializace správně nastaví událost stisknutí klávesy, které se má naslouchat.
