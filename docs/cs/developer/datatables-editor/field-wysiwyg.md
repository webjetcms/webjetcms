# Typ pole - WYSIWYG

V poli se zobrazí `WYSIWYG` editor stránky (kód HTML), v našem případě se používá [ckeditor](https://ckeditor.com/ckeditor-4/). V současné době lze toto pole na stránce použít pouze jednou, vícenásobné použití zatím není podporováno.

## Použití anotace

Anotace se používá jako `DataTableColumnType.WYSIWYG`, kompletní příklad anotace:

```java
@DataTableColumn(inputType = DataTableColumnType.WYSIWYG, title="components.news.template_html",
    hidden = true, tab="content"
)
String data = "";
```

Pole se zobrazí na kartě na celou šířku, přičemž výška karty se automaticky vypočítá podle velikosti okna. V definici karty v souboru pug nastavte atribut `content` na prázdné pro vypnutí zobrazení na pozadí `label` prvky:

```javascript
var tabs = [
    { id: 'content', title: '[[\#{editor.tab.editor}]]', selected: true, content: '' },
    ...
]
```

## Poznámky k implementaci

Implementace je v souboru [field-type-wysiwyg.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-wysiwyg) a v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) nastavit jako `$.fn.dataTable.Editor.fieldTypes.wysiwyg = fieldTypeWysiwyg.typeWysiwyg();`.

Při zobrazení v editoru se `create` funkce generuje samostatný formulář s názvem `editorForm` který obsahuje další pole `docId`, `groupId`, `virtualPath`, `title` s hodnotami vyplněnými podle aktuálního objektu JSON. Tato pole se používají z historických důvodů v různých komponentách ckeditoru pro získání ID aktuálně upravované stránky (např. pro zobrazení Média této stránky při výběru obrázku/souboru).

Z důvodu rychlosti zobrazení seznamu webových stránek se ckeditor inicializuje pouze při prvním otevření dialogového okna. To zajišťuje kód `EDITOR.on( 'open', function ( e, type ) {` ve kterém se testuje `conf.wjeditor` a inicializuje se při prvním otevření.

### Inicializace ckeditoru

Funkce samotného ckeditoru je upravena v souboru [datatables-ckeditor.js](../../../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) který upravuje standardní ckeditor podle požadavků WebJET (integrace elfinderu, upravené dialogy, přidané nové pluginy atd.). Základ vychází z původního kódu z verze 8, ale funkčnost je zabalena do třídy `DatatablesCkEditor`. Jeho inicializace je zapouzdřena v `app.js` Stejně jako:

```javascript
const createDatatablesCkEditor = () => {
    return import(/* webpackChunkName: "ckeditor" */ './datatables-ckeditor');
};
window.createDatatablesCkEditor = createDatatablesCkEditor;
```

což způsobí asynchronní načtení souboru JS a jeho inicializaci. Vlastní úprava pro WebJET se provádí ve funkci `customizeEditor`.

### Nastavení dat

Editor potřebuje znát nejen samotný kód HTML, ale také další údaje, jako je šablona (použité soubory CSS) apod. Celý objekt JSON je zpracován ve funkci při otevření editoru `setJson`

### Seznam regulárních výrazů pro formuláře

Seznam regulárních výrazů pro dialogová okna vlastností prvků formuláře se inicializuje pomocí AJAXu voláním adresy URL. `/admin/rest/forms-list/regexps`. Vrácený seznam je nastaven na atribut `regexps` a z něj se používá při inicializaci dialogů.

### Výška editoru

Výška editoru se vypočítá pomocí funkce `resizeEditor` v intervalu 3000 ms. Výška se vypočítá podle výšky okna prohlížeče, aby se maximalizovalo využití okna. Horní a dolní `margin` dialogového okna editoru a samozřejmě velikost záhlaví a zápatí.
