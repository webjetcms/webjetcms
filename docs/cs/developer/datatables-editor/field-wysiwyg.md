# Field Type - WYSIWYG

Pole zobrazí `WYSIWYG` editor stránky (HTML kódu), v našem případě je použit [ckeditor](https://ckeditor.com/ckeditor-4/). Aktuálně lze toto pole použít na stránce pouze jednou, zatím není podporováno vícenásobné použití.

## Použití anotace

Anotace se používá jako `DataTableColumnType.WYSIWYG`, kompletní příklad anotace:

```java
@DataTableColumn(inputType = DataTableColumnType.WYSIWYG, title="components.news.template_html",
    hidden = true, tab="content"
)
String data = "";
```

Pole se zobrazí v kartě na celou plochu, automaticky počítá výšku karty podle velikosti okna. V definici karet v pug souboru nastavte atribut `content` na prázdnou hodnotu pro vypnutí zobrazení pozadí `label` elementů:

```javascript
var tabs = [
    { id: 'content', title: '[[\#{editor.tab.editor}]]', selected: true, content: '' },
    ...
]
```

## Poznámky k implementaci

Implementace je v souboru [field-type-wysiwyg.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-wysiwyg) a v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) nastaveno jako `$.fn.dataTable.Editor.fieldTypes.wysiwyg = fieldTypeWysiwyg.typeWysiwyg();`.

Při zobrazení v editoru se v `create` funkci vygeneruje samostatný formulář s názvem `editorForm`, který obsahuje dodatečná pole `docId`, `groupId`, `virtualPath`, `title` s hodnotami naplněnými podle aktuálního JSON objektu. Tato pole se používají z historických důvodů v různých komponenách ckeditora pro získání ID aktuálně upravované stránky (např. pro zobrazení Média této stránky při výběru obrázku/souboru).

Z důvodu rychlosti zobrazení seznamu web stránek se ckeditor inicializuje až při prvním otevření dialogového okna. To je zabezpečeno kódem `EDITOR.on( 'open', function ( e, type ) {` ve kterém se testuje `conf.wjeditor` a při prvním otevření se inicializuje.

### Inicializace ckeditor

Samotná funkčnost ckeditora je upravena v souboru [datatables-ckeditor.js](../../../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) který upravuje standardní ckeditor pro požadavky WebJETu (integrace elfinder, upravená dialogová okna, přidány nové pluginy atd.). Základ vychází z původního kódu z verze 8, ale funkčnost je obalena do třídy `DatatablesCkEditor`. Jeho inicializace je zapouzdřena v `app.js` jako:

```javascript
const createDatatablesCkEditor = () => {
    return import(/* webpackChunkName: "ckeditor" */ './datatables-ckeditor');
};
window.createDatatablesCkEditor = createDatatablesCkEditor;
```

což vyvolá asynchronní načtení JS souboru a jeho inicializaci. Samotná úprava pro potřeby WebJETu se děje ve funkci `customizeEditor`.

### Nastavení dat

Editor pro svou funkčnost potřebuje znát nejen samotný HTML kód, ale i další data, jako šablonu (použité CSS soubory) a podobně. Celý JSON objekt je při otevření editoru zpracován ve funkci `setJson`

### Seznam regulárních výrazů pro formuláře

Seznam regulárních výrazů pro dialogová okna vlastností formulářových prvků se inicializuje AJAX voláním URL adresy `/admin/rest/forms-list/regexps`. Vrácený seznam je nastaven do atributu `regexps` az něj je použit při inicializaci dialogů.

### Výška editoru

Výška editoru je počítána funkcí `resizeEditor` v intervalu 3000 ms. Výška se počítá podle výšky okna prohlížeče tak, aby se maximalizovalo využití okna. Odpočítává se horní a dolní `margin` dialogového okna editoru a samozřejmě velikost hlavičky a patičky.
