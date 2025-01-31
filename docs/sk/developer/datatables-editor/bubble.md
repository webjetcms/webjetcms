# Upraviť v zobrazení mriežky

Datatables podporujú priamu úpravu bunky v režime editácie ```bubble```. Režim sa zapne kliknutím na tlačidlo **Upraviť v zobrazení mriežky** v nástrojovej lište. Následne kliknutím na bunku sa zobrazí dialógové okno s editorom zvolenej bunky.

## Inicializácia režimu

Upraviť v zobrazení mriežky sa inicializuje v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) kódom:

```javascript
// aktivuj rezim uprava bunky / bubble
$(dataTableInit).on('click', 'table.dataTable tbody td', function (e) {
    if ($("body").hasClass("datatable-cell-editing")) {

    }
}
```

čiže po kliknutí na bunku tabuľky. Problematická je kombinácia WebJET úprav editora, ktorá pridáva rozdelenie polí do listov a zároveň zachovanie dialógu editora v ```DOM``` strome aj po jeho zatvorení. V tejto kombinácii bolo zobrazenie polí v ```bubble``` editore nekorektné (zobrazili sa všetky polia v aktuálnom liste). Z toho dôvodu je pri editácii bunky nastavená CSS trieda ```show``` len na práve editovaný atribút.

Pomocou CSS triedy ```div.DTE_Bubble``` v [_modal.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_modal.scss) je nastavené zobrazenie všetkých listov ```div.tab-pane``` (nielen aktívneho) a zároveň sú schované všetky ```div.row```, ktoré nemajú nastavenú ```show``` triedu. V [_table.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_table.scss) je nastavené zvýraznenie bunky pri ```:hover``` v režime ```body.datatable-cell-editing```.

Získanie mena editovaného atribútu bolo komplikované, nakoniec je implementované nasledovným kódom:

```javascript
const colIndex = TABLE.cell(this).index().column;
let datatableColumn = TABLE.settings().init().columns[colIndex];
let columnName = datatableColumn.name;
```

kľúčové je získanie poradového čísla bunky z aktuálne kliknutého ```TD``` elementu. Do premennej ```columnName``` je uložené meno atribútu. Nastavenie ```show``` triedy je následne zabezpečené kódom:

```javascript
$("div.DTE_Field").removeClass("show");
$("div.DTE_Field_Name_"+columnName).addClass("show");
```

Pred otvorením samotného editora bunky je volaná REST služba pre získanie dát riadku. Často datatabuľka v zozname neobsahuje všetky údaje potrebné pre editor (z dôvodu optimalizácie rýchlosti načítania), používa sa atribút ```fetchOnEdit=true``` v konfigurácii datatabuľky. Po kliknutí na bunku je teda vždy volaná funkcia ```refreshRow``` pre získanie aktuálnych a kompletných údajov riadku (v REST službe sa použije ```ProcessItemAction.GETONE```).

Samotné vyvolanie editora cez API ```EDITOR.bubble($(this), {``` je už štandardné, upravená je len hodnota možnosti ```submit``` na hodnotu ```all``` pre odosielanie všetkých atribútov riadku (nielen hodnoty jedného atribútu).

## Ne editovateľné bunky

Bunky, ktoré nie je možné upravovať (napr. ID, bunky iba na čítanie) majú nastavenú CSS triedu ```cell-not-editable```. Túto triedu je možné nastaviť v anotácii ```@DatatableColumn.className```. Automaticky sa nastaví v [DatatableColumn.setCellNotEditable](../../../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumn.java) pre polia typu:

- ```hidden``` - polia typu ```hidden```
- ```textarea``` - zdalo sa nám nerozumné v malom ```bubble``` editore zobrazovať veľkú textovú oblasť
- ```editor.disabled``` - polia, ktoré majú nastavený ```disabled``` atribút na hodnotu ```disabled```