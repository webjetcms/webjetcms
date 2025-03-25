# Autocomplete

V editoru lze snadno doplnit **"našeptávač" k textovým polím** pro uživatelsky příjemnější získání psané hodnoty:

![](autocomplete.png)

## Konfigurace

Autocomplete pole se aktivuje nastavením atributů editoru pomocí anotace `@DataTableColumnEditorAttr`:

```java
@DataTableColumn(
    inputType = DataTableColumnType.TEXT,
    editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/v9/settings/configuration/autocomplete")
        })
    }
)
```

Podporovány jsou následující atributy, povinné je pouze `data-ac-url`:
- `data-ac-url` - URL adresa REST služby, která vrátí pole/list `String` hodnot.
- `data-ac-click` - jméno funkce, která se zavolá po kliknutí na možnost v autocompletu (pro nastavení dalších polí). Nastavením na hodnotu `fireEnter` je po zvolení hodnoty vyvolána událost stisknutí klávesy `Enter`.
- `data-ac-name` - jméno URL parametru, ve kterém se do REST služby zašle napsaná hodnota (výchozí term).
- `data-ac-min-length` - minimální počet znaků pro volání REST služby (výchozí 1).
- `data-ac-max-rows` - maximální počet zobrazených řádků (výchozí 30).
- `data-ac-params` - seznam selektorů polí, jejichž hodnoty se přidají do URL adresy volání REST služby, například. `#DTE_Field_templateInstallName,#DTE_Field_templatesGroupId`.
- `data-ac-select` - při nastavení na `true` se autocomplete chová podobně jako výběrové pole - po kliknutí myší do vstupního pole jsou načteny a zobrazeny všechny možnosti.
- `data-ac-collision` - umístění načtených možností vůči vstupnímu poli. Ve výchozím nastavení `flipfit` pro automatické umístění, pro možnost `select` je přednastaveno na `none` pro striktní umístění pod vstupní pole.
- `data-ac-render-item-fn` - název funkce, která specificky vygeneruje prvek seznamu dat

Příklad REST služby vracející údaje je v [ConfigurationController.getAutocomplete](../../../../src/main/java/sk/iway/iwcm/components/configuration/ConfigurationController.java), implementace je jednoduchá - na základě zadaného `term` parametru vrátí seznam `List<String>` vyhovujících záznamů:

```java
@GetMapping("/autocomplete")
public List<String> getAutocomplete(@RequestParam String term) {
    List<String> ac = new ArrayList<>();
    //na zaklade termu prehladaj zaznamy a do listu dopln len vyhovujuce
    if (...contains(term)) ac.add(...);
    return ac;
}
```

Jelikož na backendu se typicky používá LIKE vyhledávání je možné zadat do vyhledávání znak `%` pro zobrazení všech výsledků. To je ale pro uživatele netypické, proto při zadání mezery nebo znaku `*` se do vyhledávání hodnota nahradí za znak `%` pro zobrazení všech záznamů.

## Použití mimo datatabulky

`Autocompleter` lze využít i mimo datatabulky jednoduše jednoduchým nastavením `data-ac` atributů a CSS třídy `autocomplete`. Inicializace je automaticky aktivována v [app-init.js](../../../../src/main/webapp/admin/v9/src/js/app-init.js) na všechny `input` elementy s CSS třídou `autocomplete`. Příklad:

```html
<div id="docIdInputWrapper" class="col-auto col-pk-input">
    <label for="tree-doc-id">Doc ID: </label>
    <input type="text" autocomplete="off" class="js-tree-doc-id__input autocomplete" id="tree-doc-id" data-ac-name="docid" data-ac-url="/admin/skins/webjet6/_doc_autocomplete.jsp" data-ac-click="fireEnter"/>
</div>
```

## Poznámky k implementaci

Autocomplete používá [jQuery-ui-autocomplete](https://api.jqueryui.com/autocomplete/) funkce. Interně je zapouzdřen do JavaScript třídy [AutoCompleter](../../../../src/main/webapp/admin/v9/src/js/autocompleter.js). Ta je upravena z původní verze ve WebJET 8, zpětně by měla být kompatibilní (lze použít i URL adresy původních autocomplete služeb ve WebJET 8).

Doplněna je funkce `autobind()`, která převezme nastavení z data atributů zadaného input elementu. Inicializace autocomplete je implementována v index.js v kódu:

```javascript
//nastav autocomplete
$('#'+DATA.id+'_modal input.form-control[data-ac-url]').each(function() {
    var autocompleter = new AutoCompleter('#'+$(this).attr("id")).autobind();
    $(this).closest("div.DTE_Field").addClass("dt-autocomplete");
});
```

přičemž jak je vidět `div.DTE_Field` elementu se také nastaví CSS třída `dt-autocomplete` pro možnost budoucího stylování elementu.

Funkce nastavena přes `click` parametr se jmenuje se zpožděním 100ms, aby se nejprve nastavila hodnota v poli, kterou je následně možné získat a použít.

## Speciální generování prvků seznamu

Pomocí parametru `data-ac-render-item-fn` lze nastavit název funkce, která specificky vygeneruje prvek do seznamu dat. Aby to fungovalo musí být splněno :
- vygenerovaný prvek musí být `li` element (to co je v něm je už na vás)
- tento vygenerovaný element musí být vložen do listu `ul`
- zadaná funkce v `data-ac-render-item-fn` musí být definována pomocí `window` a musí mít vstupní parametry `ul` a `item`

Příklad vlastní funkce

```java
    @DataTableColumnEditorAttr(key = "data-ac-render-item-fn", value = "disableDeletedEnum")
```

příklad implementace takové funkce

```js
//Don't forget to add fn into windows AND use correct input params
window.disableDeletedEnum = function(ul, item) {
    var deletedPrefix = WJ.translate("enum_type.deleted_type_mark.js");
    if(deletedPrefix !== undefined && deletedPrefix !== null && deletedPrefix !== "" && item.label.startsWith(deletedPrefix)) {
        //Special element generation - with added "disabled" class
        return $("<li>")
            .append($("<div>").append(item.label))
            .appendTo(ul).addClass("disabled");
    }

    //Classic element generation
    return $("<li>")
        .append($("<div>").append(item.label))
        .appendTo(ul);
}
```

V tomto příkladu jsme při splnění podmínky přidali elementu třídu `disabled`. Autocomplete jsme nastavili tak, že data (prvky) označená třídou `disabled` se barevně zvýrazní a nelze je zvolit.
