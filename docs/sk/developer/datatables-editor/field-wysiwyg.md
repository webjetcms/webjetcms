# Field Type - WYSIWYG

Pole zobrazí ```WYSIWYG``` editor stránky (HTML kódu), v našom prípade je použitý [ckeditor](https://ckeditor.com/ckeditor-4/). Aktuálne je možné toto pole použiť na stránke len jeden krát, zatiaľ nie je podporované viacnásobné použitie.

## Použitie anotácie

Anotácia sa používa ako ```DataTableColumnType.WYSIWYG```, kompletný príklad anotácie:

```java
@DataTableColumn(inputType = DataTableColumnType.WYSIWYG, title="components.news.template_html",
    hidden = true, tab="content"
)
String data = "";
```

Pole sa zobrazí v karte na celú plochu, automaticky počíta výšku karty podľa veľkosti okna. V definícii kariet v pug súbore nastavte atribút ```content``` na prázdnu hodnotu pre vypnutie zobrazenia pozadia ```label``` elementov:

```javascript
var tabs = [
    { id: 'content', title: '[[\#{editor.tab.editor}]]', selected: true, content: '' },
    ...
]
```

## Poznámky k implementácii

Implementácia je v súbore [field-type-wysiwyg.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-wysiwyg) a v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) nastavené ako ```$.fn.dataTable.Editor.fieldTypes.wysiwyg = fieldTypeWysiwyg.typeWysiwyg();```.

Pri zobrazení v editore sa v ```create``` funkcii vygeneruje samostatný formulár s názvom ```editorForm```, ktorý obsahuje dodatočné polia ```docId```, ```groupId```, ```virtualPath```, ```title``` s hodnotami naplnenými podľa aktuálneho JSON objektu. Tieto polia sa používajú z historických dôvodov v rôznych komponenách ckeditora pre získanie ID aktuálne upravovanej stránky (napr. pre zobrazenie Média tejto stránky pri výbere obrázka/súboru).

Z dôvodu rýchlosti zobrazenia zoznamu web stránok sa ckeditor inicializuje až pri prvom otvorení dialógového okna. To je zabezpečené kódom ```EDITOR.on( 'open', function ( e, type ) {``` v ktorom sa testuje ```conf.wjeditor``` a pri prvom otvorení sa inicializuje.

### Inicializácia ckeditor

Samotná funkčnosť ckeditora je upravená v súbore [datatables-ckeditor.js](../../../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) ktorý upravuje štandardný ckeditor pre požiadavky WebJETu (integrácia elfinder, upravené dialógové okná, pridané nové pluginy atď.). Základ vychádza z pôvodného kódu z verzie 8, ale funkčnosť je obalená do triedy ```DatatablesCkEditor```. Jeho inicializácia je zapuzdrená v ```app.js``` ako:

```javascript
const createDatatablesCkEditor = () => {
    return import(/* webpackChunkName: "ckeditor" */ './datatables-ckeditor');
};
window.createDatatablesCkEditor = createDatatablesCkEditor;
```

čo vyvolá asynchrónne načítanie JS súboru a jeho inicializáciu. Samotná úprava pre potreby WebJETu sa deje vo funkcii ```customizeEditor```.

### Nastavenie údajov

Editor pre svoju funkčnosť potrebuje poznať nielen samotný HTML kód ale aj ďalšie dáta, ako šablónu (použité CSS súbory) a podobne. Celý JSON objekt je pri otvorení editora spracovaný vo funkcii ```setJson```

### Zoznam regulárnych výrazov pre formuláre

Zoznam regulárnych výrazov pre dialógové okná vlastností formulárových prvkov sa inicializuje AJAX volaním URL adresy ```/admin/rest/forms-list/regexps```. Vrátený zoznam je nastavený do atribútu ```regexps``` a z neho je použitý pri inicializácii dialógov.

### Výška editora

Výška editora je počítaná funkciou ```resizeEditor``` v intervale 3000 ms. Výška sa počíta podľa výšky okna prehliadača tak, aby sa maximalizovalo využitie okna. Odpočítava sa horný a dolný ```margin``` dialógového okna editora a samozrejme veľkosť hlavičky a pätičky.
