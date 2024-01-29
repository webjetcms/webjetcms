# Export a import

Export a import dát využíva možnosti exportu a importu v [API datatables](https://datatables.net/extensions/buttons/examples/initialisation/export). Pre prácu s Excel súbormi sa používa knižnica [SheetJS](https://docs.sheetjs.com/).

Po implementácii importu a exportu do vašej datatabuľky nezabudnite použiť aj [automatizovaný test importu](../testing/datatable-import.md).

## Export dát

- Umožňuje export do **Excelu(xlsx) a priamu tlač na tlačiareň**
- Súboru sa nastaví meno podľa aktuálneho ```title``` stránky a automaticky sa doplní aktuálny dátum a čas.
- Exportu v tabuľke so serverovým stránkovaním je možné nastaviť typ dát (**aktuálna/všetky strany, filtrované/všetky riadky, zoradenie**).
- Pri serverovom stránkovaní sa najskôr vykoná volanie REST služby, z ktorej sa získa **maximálne 50 000 záznamov**. Pri potrebe exportovať viac záznamov použite viacnásobné exportovanie s využitím filtrovania.
- Spracovanie serverových dát prebieha mimo datatabuľky (kvôli výkonu), nanovo sa generujú ```datetime``` stĺpce a ```radio buttony``` (podporované sú ```date-time, date, *-select, *-boolean```).
- Pri exporte sa v prvom riadku pripraví zoznam stĺpcov, **import následne nie je citlivý na poradie stĺpcov**.
- Pre **výberové polia** (select/číselníky) sa **exportuje textová hodnota** a pri importe sa nazad rekonštruuje na ID. Umožňuje to mať **rozdielne ID naviazaných záznamov** medzi prostrediami (napr. ID šablóny pre web stránku), ak sa zhoduje meno, korektne sa záznam spáruje. V exporte je následne aj ľudsky zrozumiteľný text namiesto ID hodnoty.

Implementácia je v súbore ```export-import.js``` vo funkcii ```bindExportButton(TABLE, DATA) ```, ktorá je inicializovaná priamo v ```index.js``` pri inicializácii datatabuľky. HTML kód exportného dialógu je v súbore ```datatables-data-export.pug``` ktorý je cez ```include``` vkladaný do ```layout.pug```.

### Príprava dát pred exportom

V datatabuľke sa často načítajú len základné dáta/stĺpce (hlavne pri použití [editorFields](../datatables-editor/customfields.md) atribútu). Preto je pred exportom vždy potrebné volať server a získať kompletné dáta. Pri detegovaní exportu sa namiesto ```ProcessItemAction.GETALL``` použije ```ProcessItemAction.GETONE``` ako pri získaní jedného záznamu. Vrátené entity teda budú obsahovať všetky dáta.

Pri použití ```serverSide=false``` na datatabuľke nie je možné využiť všetky možnosti exportu (napr. len vyfiltrované dáta), pretože sa nemusia dať serverovo spracovať. Tieto možnosti sú teda schované pri kliknutí na tlačidlo otvorenia dialógového okna exportu (implementované priamo v ```index.js``` vo funkcii tlačidla export dialógu).

### Vykonanie exportu

V dialógovom okne sa nastavujú možnosti exportu. Po kliknutí na tlačidlo exportovať sa získajú aktuálne dáta zo servera volaním funkcie ```getDataToExport(serverSide, TABLE, pageVal, searchVal, orderVal)```, ktoré sa spracujú po volaní ```.then(response => {```. Pre každý riadok zo získaného JSON objektu ```formatedData = content.map(c => {``` sa generuje pole riadku pre export dát iteráciou cez polia editora ```DATA.fields.forEach((dc) => {```.

Dátové polia typu ```json``` sa spracovávajú špeciálne. Pre typy ```dt-tree-page, dt-tree-group, dt-tree-dir``` sa do výstupu generuje hodnota z ```v.fullPath``` (alebo ```v.virtualPath``` pre ```dt-tree-dir```). Výsledkom je, že v exporte nebude celý JSON objekt ale hodnota typu ```["/Portal/Novinky", "/English/News"]```.

Výsledný objekt ```formatedData``` obsahuje dvoj rozmerné pole údajov pre export. Tie sa použijú pri API volaní datatabuľky simulovaním kliknutia na tlačidlo exportu ```TABLE.button().add(0, {``` pomocou volania ```$(".exportujem").click();```. Toto fiktívne tlačidlo sa po exporte schová cez ```TABLE.buttons('.exportujem').remove();```. Takto simulujeme kliknutie na štandardné export tlačidlo datatabuľky - po kliknutí na exportovať v dialógovom okne sa pripravia dáta, pridá sa do datatabuľky tlačidlo pre export dát a klikne sa na toto tlačidlo. Následne po exporte sa štandardné tlačidlo pre export schová.

Konfigurácia exportu je nastavená v ```exportOptions```. Dôležité je spracovanie v ```customizeData``` kde sa nanovo generuje hlavička tabuľky. Pôvodne obsahuje zoznam stĺpcov v tabuľke, úprava ale použije zoznam stĺpcov pre editor. Do názvu stĺpca sa okrem ```dc.label``` hodnoty generuje aj meno atribútu (```dc.name```), pretože niekedy sú použité rovnaké názvy (napr. v editácii používateľa je štandardné meno používateľa ale aj meno v časti Adresa doručenia).

### Špeciálny typ exportu

Ak potrebujete implementovať špeciálny typ exportu stačí vo web stránke pridať nasledovný element:

```html
<div class="hidden" id="datatableExportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-extend-custom" value="custom" data-hide="#datatableExportModal .file-name,#pills-export-advanced-tab">
        <label class="form-check-label" for="dt-settings-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

v atribúte ```data-hide``` je možné špecifikovať zoznam elementov, ktoré sa automaticky schovajú po nastavení uvedenej možnosti.

Potrebné je implementovať JS funkciu ```window.exportDialogCustomCallback(type, TABLE)```, ktorá je vykonaná pri tejto možnosti:

```javascript
function exportDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_export.jsp");
    return true;
}
```

## Import dát

- Umožňuje **importovať dáta ako nové** (doplnia sa do databázy) alebo **párovať existujúce dáta podľa zvoleného stĺpca** (napr. meno, URL adresa a podobne). Pri párovaní najskôr pohľadá záznam v databáze a následne ho aktualizuje. Ak neexistuje, vytvorí nový záznam.
- **Importuje sa z formátu xlsx**.
- Import sa vykonáva **postupne v dávkach po 25 záznamoch**, aby nebol zaťažený server.

Implementácia je v súbore ```export-import.js``` vo funkcii ```bindImportButton(TABLE, DATA) ```, ktorá je inicializovaná priamo v ```index.js``` pri inicializácii datatabuľky. HTML kód importného dialógu je v súbore ```datatables-data-import.pug``` ktorý je cez ```include``` vkladaný do ```layout.pug```.

V dialógovom okne importu sa zobrazuje zoznam stĺpcov podľa ktorých je možné dáta importovať. Tento zoznam je implementovaný priamo v ```index.js``` po kliknutí na tlačidlo zobrazenia importného dialógu. Možnosti sa generujú so zoznamu ```DATA.fields.forEach((col, index) => {```. Preskočené sú atribúty typu ```hidden``` alebo atribúty s anotáciou ```data-dt-import-hidden```.

### Konverzia z Excelu

Konverzia z Excel súboru na dáta sa deje priamo na strane klienta vo volaní ```document.getElementById('insert-file').addEventListener('change', e => {```, čiže hneď po výbere súboru. Používa sa knižnica SheetJS volaním [xlsx.read](https://docs.sheetjs.com/#parsing-options). V konfigurácii je nastavené spracovanie dátumov pomocou atribútu ```cellDates: true```, čo zabezpečí konverziu dátumov na ```Date``` objekt. Výsledkom čítania je JSON objekt excelData a vyvolanie udalosti ```file-reader-done```.

Spracovanie údajov z Excelu v objekte ```excelData``` prebieha v ```$( document ).on('file-reader-done', () => {``` kde sa vykoná niekoľko úprav:

- Generuje sa JSON objekt, ako meno atribútu sa použije hodnota z hlavičky tabuľky za znakom |.
- Štruktúra výsledného JSON objektu je rovnaká ako pri štandardnom získaní/uložení dát v datatabuľke.
- Konvertuje sa dátumy, textové hodnoty číselníkových dát na ID hodnotu a pre polia typy JSON sa vykoná konverzia z reťazca na reálny JSON objekt.

Importovanie prebieha volaním rovnakej REST služby ako pri použití štandardného editora záznamu. Odosiela sa ale naraz viac záznamov, maximálne ale podľa hodnoty ```chunks```. Pre veľký počet záznamov sa teda postupne volá REST služba ```/editor```, pričom v jednom volaní je maximálne ```chunks``` záznamov (predvolene 25, definované v konf. premennej ```chunksQuantity```). Zobrazený je aj ukazateľ postupu podobne ako v galérii pri nahrávaní súboru.

### Aktualizácia podľa stĺpca

Ak je v dialógovom okne importu zvolená možnosť aktualizovať podľa stĺpca volá sa metóda ```DatatableRestControllerV2.editItemByColumn(T entity, String updateByColumn)```. Táto v prvom rade vyhľadá záznamy v databáze podľa zadaného stĺpca a hodnoty v danom riadku v exceli. Napr. záznamy podľa zhody ```email``` atribútu v exceli. Tu je dôležité si uvedomiť, že takých záznamov môže byť v databáze viac a aktualizácia počas importu sa vykoná na viacerých záznamoch.

Po nájdení zhodujúcich záznamov v databáze je potrebné modifikovať ID stĺpec entity importovaného riadku. Podľa anotácie ```@Id``` sa identifikuje stĺpec (nie vždy to musí byť ```id```, môže to byť napr. ```userId```). Hodnota ```Id``` stĺpca sa nastaví na hodnotu existujúcej entity v databáze a následne sa vykoná uloženie záznamu volaním ```editItem(entity, id)```.

V jednoduchosti celý kód vyhľadá existujúci záznam v databáze a importovanej entite nastaví ```id``` hodnotu na hodnotu nájdeného záznamu.

> UPOZORNENIE: pri implementácii sme identifikovali problém, že pre triedy anotované cez Lombook nie je možné používať ```BeanUtils.setProperty``` ani ```BeanUtils.copyProperties```. Je potrebné použiť ```BeanWrapperImpl``` a ```NullAwareBeanUtils.copyProperties```.

> UPOZORNENIE: importovať je technicky možné aj len niektoré stĺpce, nepredpokladajte teda, že import bude vždy obsahovať všetky dáta. Inak vám budú nastávať chyby typu ```NullPointerException```. Zvlášť v ```editorFields.toEntity``` je potrebné kontrolovať ```null``` hodnoty na atribútoch, aby ich prenos nepadal.

### Podporované anotácie

V anotácii ```@DatatableColumn``` je možné použiť nasledovné možnosti

- ```@DataTableColumnEditorAttr(key = "data-dt-import-updateByColumn", value = "PROPERTY")``` - v dialógu pre import nastaví pre tento stĺpec vyhľadávanie podľa stĺpca ```PROPERTY```. Potrebné ak napr. v ```editorFields``` prepisujete nejakú vlastnosť (napr. ```login```) ale pri importe potrebujete v databáze vyhľadávať/párovať podľa pôvodného atribútu ```login```.
- ```@DataTableColumnEditorAttr(key = "data-dt-import-hidden", value = "true")``` - takto anotovaný atribút sa v dialógu pre import nezobrazí.
- ```@DataTableColumn(className = "not-export")``` - stĺpec s CSS triedou ```not-export``` sa nebude exportovať.

### Špeciálny typ importu

Ak potrebujete implementovať špeciálny typ importu stačí vo web stránke pridať nasledovný element:

```html
<div class="hidden" id="datatableImportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-import-extend-custom" value="custom" data-hide="#datatableImportModal .file-name,#import-settings">
        <label class="form-check-label" for="dt-settings-import-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

v atribúte ```data-hide``` je možné špecifikovať zoznam elementov, ktoré sa automaticky schovajú po nastavení uvedenej možnosti.

Potrebné je implementovať JS funkciu ```window.importDialogCustomCallback(type, TABLE)```, ktorá je vykonaná pri tejto možnosti:

```javascript
function importDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_import.jsp");
    return true;
}
```

## Poznámky k implementácii

Číselníkové hodnoty (select) sa exportujú ako textová hodnota. V súbore [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js) sú funkcie ```getOptionsTableExport``` a ```getOptionsTableImport```, ktoré pripravia tabuľku s kľúčom ```fieldName-value``` (export) a ```fieldName-label``` (import) pre jednoduchý preklad medzi hodnotou a labelom.

Najzložitejšou časťou je čítanie dát zo servera pri serverovom stránkovaní. Pri požiadavke na export sa musí vykonať volanie REST služby. Deje sa to vo funkcii ```getDataToExport``` kde sa využíva a modifikuje ```DATA.urlLatestParams``` (nahradia sa parametre ako size, page atď). Dáta sa získajú štandardným ajax volaním mimo API datatabuľky (nechceme, aby datatabuľka načítala také veľké množstvo dát - obava pred jej padnutím). Finta s využitím takýchto dát je v export tlačidle v optione ```customizeData: function(d)```, kde nahradíme dáta v datatabuľke za novo získané.

Pri importe sa transformujú dáta z xlsx na JSON objekt. Ten sa následne použije pre štandardné volanie DT editora, kde sa naraz odošle na server 25 záznamov. Import je impelentovaný vo funkcii ```importData```. Využíva sa existujúci progress bar pre upload súborov volaním eventu ```$( document ).trigger('initAddedFileFromImageOutside', file);```.

Konverzia z xlsx na JSON formát je implementovaná v ```export-import.js```. Iteruje sa zoznam stĺpcov a z Excelu sa získava hodnota pre dané meno stĺpca. Do JSON objektu sa naplnia len stĺpce zadané v Exceli. Pre ```-date``` stĺpce sa formátuje dátum, keďže z Excelu sa získa len timestamp.

Modálne okno pre import/export je globálne, ak je na stránke viacero datatabuliek je potrebné určiť, v ktorej sa deje import/export. Existujú premenné:

- ```window.datatableExportModal.tableId``` - ID tabuľky pre export (selektor)
- ```window.datatableImportModal.tableId``` - ID tabuľky pre import (selektor)
- ```window.datatableImportModal.TABLE``` - inštancia tabuľky pre import

Trieda ```DatatableRestControllerV2``` obsahuje metódy ```isExporting()``` pre detegovanie exportu a ```isImporting()``` importu. Tieto je možné využiť v implementácii vášho REST controllera, napr. na validáciu dát v metóde ```validateEditor```.