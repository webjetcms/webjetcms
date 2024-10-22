# Web stránky

Sekcia web stránky je špecifická v tom, že obsahuje viaceré datatabuľky:

- ```webpagesDatatable``` - hlavná datatabuľka pre zoznam web stránok
- ```groupsDatatable``` - skrytá (```display:none```) datatabuľka pre editáciu položky priečinka (synchronizovaná so stromovou štruktúrou)
- ```mediaTable``` - zobrazuje pripojené média k web stránke v editore stránky
- ```historyTable``` - zobrazuje historické záznamy (zmeny) v editore web stránky

## História web stránok

História web stránok je implementovaná ako vnorená datatabuľka v editore stránok. Inicializovaná je v triede [DocEditorFields.java](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) na objekte ```private List<DocHistoryDto> history;```.

### REST rozhranie

REST rozhranie je dostupné na URL adrese ```/admin/rest/web-pages/history?docId={docId}&groupId={groupId}``` a implementované je v triede [DocHistoryRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryRestController.java).

Využíva [mapovanie](../../backend/mapstruct.md) objektov ```DocDetails``` na jednoduchší DTO objekt. V datatabuľke sa zobrazujú len základné údaje.

Prístup k histórii je **len na čítanie**, v REST službe je zápis zakázaný v metóde ```beforeDelete``` a ```beforeSave```.

### Frontend

Datatabuľka je upravená počúvaním udalosti ```WJ.DTE.innerTableInitialized```, ten sa volá pre každú datatabuľku, overením cez ```id``` sa kód vykoná len pre tabuľku histórie. Inicializovaný objekt je priradený do globálnej premennej ```historyTable```.

```javascript
window.addEventListener("WJ.DTE.innerTableInitialized", function(event) {
    if (event.detail.conf.id=="DTE_Field_editorFields.history") {
        historyTable = event.detail.conf.datatable;
        historyTable.hideButton("create");
        ...
    }
});
```

Po inicializácii sú schované nepotrebné tlačidlá volaním ```historyTable.hideButton```.

Komplikovaná úprava je v spôsobe načítania údajov z histórie do editora. Editoru je potrebné podvrhnúť JSON dáta načítane podľa zvoleného ID z histórie. Zabezpečuje to nasledovný kód tlačidla editácie:

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

Pomocou ajax požiadavky so zadaným ```historyId``` sa získajú dáta web stránky z histórie. Tie sú nastavené v datatabuľke riadku podľa ```docId``` (čo by mal byť aktuálne editovaný riadok). S novými dátami je otvorený editor volaním funkcie ```wjEdit``` a následne sú do riadku datatabuľky vrátené pôvodné dáta uložené v objekte ```oldJson``` (inak by sa po zatvorení okna editora zobrazili historické dáta v datatabuľke web stránok).

V ```init``` je nastavená možnosť ```showIfOneRowSelected``` ktorá tlačidlo zobrazí len ak je zvolený presne jeden riadok.

Tlačidlá pre zobrazenie náhľadu stránky a porovnanie prechádzajú všetky zvolené riadky a otvárajú do nového okna náhľad, alebo porovnanie stránky podľa pôvodnej funkcionality vo WebJET 8. Porovnanie stránok vždy zobrazí porovnanie zvoleného riadku voči aktuálnej verzii (nie porovnanie zvolených riadkov).

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

## Optimalizácia rýchlosti zobrazenia

Keďže správa web stránok je základná funkcionalita CMS systému snažili sme sa optimalizovať jej rýchlosť načítania a zobrazenia. Z toho dôvodu sú použité špecifické funkcie a kód.

### Volanie REST služieb

Inicializačné JSON dáta sú vložené priamo do stránky cez triedu [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java). Pri prvotnom zobrazení stránky sa teda nemusí čakať na volanie REST služieb. V kóde stránky sa použijú ako:

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

```jstree``` objekt automaticky hľadá ```window.treeInitialJson``` a použije ho namiesto zadanej URL adresy. Zadané URL adresy pre REST služby sa použijú až pre ďalšie volania.

### Optimalizácia načítania adresára

Pre editáciu adresára web stránky sa využíva schovaná datatabuľka. Údaje sa štandardne čítajú pre dáta ```jstree``` stromovej štruktúry. Tie obsahujú ```original``` objekt, v ktorom je fakticky ```GroupDetails```. Tento objekt sa umelo vkladá do datatabuľky adresára vo funkcii ```loadTablesForGroup``` ako:

```javascript
if (groupsDatatable.rows().count()==0) {
    groupsDatatable.row.add(window.lastGroup);
} else {
    groupsDatatable.row(0).data(window.lastGroup);
}
groupsDatatable.draw();
```

nemusí sa teda volať REST služba pre načítanie dát. Toto samotné ale nepomôže, pretože datatabuľka nemá hodnoty pre výberové polia, keďže umelo vložený objekt tieto údaje neobsahuje.

Načítanie je teda implementované ako volanie obnovenia datatabuľky a následné otvorenie editácie/pridania záznamu. Tlačidlá pre editáciu a pridanie nového záznamu sú zobrazené nad stromovou štruktúrou. Kliknutie na tieto tlačidlá vyvolá nastavenie premennej ```groupsDatatableClickButtonAfterXhr``` na hodnotu tlačidla, ktoré sa má zavolať po obnovení dát (napr. ```"buttons-create"```). ```groupsDatatable``` má nastavenú funkciu ```onXhr```, ktorá po načítaní údajov overuje stav premennej ```groupsDatatableClickButtonAfterXhr```. Ak nie je ```null``` tak vyvolá kliknutie na zadané tlačidlo.

Proces je teda nasledovný:

- pri kliknutí na uzol v stromovej štruktúre sa nastaví objekt do datatabuľky a nastaví sa jej nové URL podľa ```groupId```
- pri kliknutí na tlačidlo Pridať alebo Upraviť priečinok nad stromovou štruktúrou sa zapamätá meno tlačidla v premennej ```groupsDatatableClickButtonAfterXhr``` a vyvolá sa tlačidlo ```buttons-refresh``` v datatabuľke pre načítanie dát zo servera
- po načítaní dát zo servera sa overí stav v premennej ```groupsDatatableClickButtonAfterXhr``` a ak nie je ```null```, tak sa vyvolá zadané tlačidlo v datatabuľke (čo otvorí editor adresára).

### Optimalizácia načítania JavaScript súborov

Web stránky používajú ```ckeditor```, ktorého JavaScript súbor je potrebné načítať. Je to ale pomerne veľký súbor a jeho spracovanie zvyšuje záťaž procesora a následne spomaľuje zobrazenie web stránky. Tento súbor sa teda načíta asynchrónne po 2 sekundách po inicializácii DOM vo funkcii ```window.domReady.add```:

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

Problémom ale následne môže byť otvorenie okna editora stránok pred načítaním ```ckeditor.js```, ošetrené to je teda priamo vo funkcii otvorenia okna, kde sa testuje objekt ```window.CKEDITOR``` a ak neexistuje, tak sa inicializácia pozdrží cez funkciu ```setTimeout```.

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

## Zobrazenie podľa docid parametra

Zobrazenie podľa zadaného ```docid``` URL parametra (alebo zadaného ```docid``` cez textové pole) využíva knižnicu [js-tree-auto-opener](../../libraries/js-tree-auto-opener.md). Automatické otvorenie získa zoznam ID adresárov volaním REST služby ```/admin/rest/web-pages/parents/{id}```. Získaný zoznam ID adresárov sa následne snaží otvoriť (postupným volaním REST služby na získanie údajov podadresárov). Po otvorení všetkých adresárov otvorí na editáciu web stránku.

Vstupné pole na zadanie ```docid``` v stránke je implementované priamo vo [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) z pred pripraveného HTML kódu ```#docIdInputWrapper```, ktorý je presunutý do kontajnera nástrojovej lišty datatabuľky. Po stlačení klávesy ```Enter``` je vyvolaná akcia otvorenia zadaného ID stránky.

Na vstupné pole je napojený ```autocomplete``` vyhľadávanie podľa názvu stránky alebo jej URL adresy nastavením ```data-ac``` atribútov.

!>**Upozornenie:** aktuálna implementácia nedokáže otvoriť web stránku na druhej strane zoznamu web stránok (keďže ju nenájde v zozname). Tiež ak je zadané ```docid``` v adresári Systém alebo Kôš a tento list nie je aktuálne zobrazený, tiež nedokáže adresár zobraziť.

## Zobrazenie karty Naposledy upravené

Po kliknutí na kartu Naposledy upravené sa načíta do datatabuľky zoznam naposledy upravených stránok od všetkých používateľov. Kliknutie na meno je spracované vo funkcii ```webpagesDatatable.onEdit```. Keďže stránky v zozname môžu byť v rôznych adresároch neotvoria sa priamo, ale ich ID je zadané do poľa ```Doc ID``` a následne načítané vrátane stromovej štruktúry.

Kliknutie na list načíta obsah datatabuľky akoby pre adresár nastavený v konfiguračnej premennej ```systemPagesRecentPages```, ktorý sa kontroluje v triede [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java) a ak sa ID adresára zhoduje volá sa načítanie zoznamu posledných stránok.

## Zobrazenie karty Čakajúce na schválenie

Zobrazenie zoznamu v karte čakajúce na schválenie je komplikovanejšie. Na backende sa kontroluje, či pre aktuálne prihláseného používateľ existujú stránky, ktoré má schváliť. Ak áno nastaví sa v triede [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java) do modelu atribút ```hasPagesToApprove``` na hodnotu ```TRUE```. Tento sa testuje vo [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) a ak je ```FALSE``` tak sa list skryje.

Po kliknutí na kartu sa do datatabuľky načíta akoby adresár s ID nastavenom v konfiguračnej premennej ```systemPagesDocsToApprove```, čo sa kontroluje v triede [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java). Načítanie komplikuje fakt, že ako unikátne ID sa používa hodnota ```docId```. Pre schvaľovanie ale môže existovať viacero záznamov pre rovnaké ```docId```. Ako najjednoduchšie riešenie sme otočili hodnotu ```docId``` a ```historyId```, ktoré sa navzájom v prenášaných dátach vymenia.

Problémom je zavolanie obnovenia dát po kliknutí, ktoré volá ```WebpagesRestController.getOne```, kde je zaslaná hodnota ```historyId``` ako ```id```. Podľa zaslaného ```id``` (čo je v skutočnosti ```historyId```) získame dotazom do databázy korektnú hodnotu ```docId``` pre získanie obsahu podľa kombinácie ```docId, historyId```.

Kliknutie je obslúžené vo funkcii ```webpagesDatatable.onEdit``` otvorením nového okna ```/admin/approve.jsp``` so schvaľovacím dialógom.

Pre tabuľku naposledy upravené aj čakajúce na schválenie sa schovanú tlačidlá pre pridanie/editáciu/zmazanie záznamu. Je to ošetrené počúvaním udalosti prepnutia karty ```$('#pills-pages a[data-toggle="pill"]').on('shown.bs.tab', function (e) {```.

## Náhľad stránky

Náhľad stránky implementuje trieda ```EditorPreview``` v súbore ```src/js/pages/web-pages-list/preview.js```. Funkcia ```window.previewPage``` nastavuje atribút ```isPreviewClicked``` a vykoná API volanie pre uloženie editora. Zároveň počúva udalosť ```preSubmit``` pri ktorej deteguje stav atribútu ```isPreviewClicked```.

Ak je nastavený na ```true``` získa JSON dáta z editora a uloží ich do session objektu ```session.setAttribute("ShowdocAction.showDocData", entity)``` volaním ```/admin/rest/web-pages/preview/```. Následne otvorí novú kartu v prehliadači s adresou ```/admin/webpages/preview/?docid=+jsonData.id```, ktorú obsluhuje trieda ```PreviewController.java```. Tá získa zo session DocDetails objekt ```DocDetails doc = (DocDetails)request.getSession().getAttribute("ShowdocAction.showDocData");```, nastaví potrebné ```request``` objekty a vykoná volanie ```/showdoc.do``` pre zobrazenie web stránky. Toto ale nezíska aktuálny objekt z databázy, ale použije objekt ```request.getAttribute("ShowdocAction.showDocData")``` pre zobrazenie údajov.

Zároveň počúva udalosť ```webpagesDatatable.EDITOR.on('postSubmit'```, pomocou ktorej aktualizuje obsah okna náhľadu pri uložení web stránky (entita sa uloží do ```session``` volaním ```WebpagesRestController.afterSave```). Toto sa vykoná len ak je okno náhľadu stále otvorené, to sa testuje pomocou ```self.previewWindow.name != ""```. Toto zostane prázdne, ak okno používateľ zatvorí.

Tlačidlo Náhľad je do pätičky okna vložené pri udalosti ```webpagesDatatable.EDITOR.on('open'```.

## Uloženie klávesovou skratkou

Web stránka sa dá uložiť pomocou klávesovej skratky ```CTRL+s/CMS+s```, čo je inicializované priamo v datatabuľke funkciou ```bindKeyboardSave```. Technicky po stlačení klávesovej skratky je vyvolaná udalosť ```WJ.DTE.save``` na ktorú sa počúva a táto udalosť vykoná fyzické uloženie (volanie ```EDITOR.submit```). Pri takomto uložení je okno ponechané v otvorenom stave, aby používateľ mohol pokračovať v práci.

Pri web stránkach ale nastal problém v tom, že samotná editácia prebieha v ```iframe``` elemente. Udalosť teda nebola korektne nastavená. Udalosť je potrebné pridať aj pre ckeditor aj pre PageBuilder manuálne a vyvolať udalosť v rodičovskom rámci.

V ```/admin/v9/src/js/datatables-ckeditor.js afterInit``` je pridaná udalosť:

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

podobne pre PageBuilder v ```/admin/inline/inline.js.jsp```:

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

Takáto inicializácia korektne nastaví počúvanie udalosti stlačenia klávesovej skratky.