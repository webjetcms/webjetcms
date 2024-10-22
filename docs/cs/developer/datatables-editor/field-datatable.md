# Typ pole - datová tabulka

Datové pole umožňuje zobrazit vnořenou datovou tabulku v editoru stránky (např. seznam Média v editoru stránky). Všimněte si, že datová tabulka je inicializována vlastní adresou URL. V samotném objektu nadřazené tabulky není třeba data odesílat ani dodatečně přijímat, data se změní automaticky při volání služby REST vnořeného datatabulky. Technicky by bylo možné pracovat přímo s daty JSON z nadřazené tabulky, ale tato možnost zatím není implementována.

Vzhledem k tomu, že aktuálně vložená datová tabulka pracuje se samostatnými službami REST, jsou vrácená data prázdným polem. `[]`.

## Použití anotace

Anotace se používá jako `DataTableColumnType.DATATABLE` a musí být nastaveny následující atributy editoru:
- `data-dt-field-dt-url` - URL služby REST, může obsahovat makra pro vkládání hodnot z nadřazeného editoru, např.: `/admin/rest/audit/notify?docid={docId}&groupId={groupId}`
- `data-dt-field-dt-columns` - název třídy (včetně balíčků), ze které se má použít. [definice datových sloupců](datatable-columns.md), např. `sk.iway.iwcm.system.audit.AuditNotifyEntity`
- `data-dt-field-dt-tabs` - seznam karet editoru ve formátu JSON. Všechny názvy a hodnoty objektu JSON je třeba zabalit do tvaru `'`, překlady se nahrazují automaticky. Příklad: `@DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'fields', 'title': '[[#{editor.tab.fields}]]' }]")`.

Příklad úplné anotace:

```java
@DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
    tab = "media",
    editor = { @DataTableColumnEditor(
        attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/audit/notify"),
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.system.audit.AuditNotifyEntity")
        }
    )
})
private List<Media> media;
```

Pomocí datových atributů můžete nastavit i další možnosti konfigurace datové tabulky. Data jsou odesílána jako řetězec. Hodnoty `true` a `false` jsou převedeny na `boolean` objekt. Nastavení atributu `order` umožňuje nastavit rozložení pouze pro jeden sloupec. Ostatní možnosti se přenášejí jako řetězec.

Přidání předpony do nabídky nastavené možnosti `data-dt-field-dt-`, tj. nastavit možnost `serverSide` použít klíč `data-dt-field-dt-serverSide`. Příklad dalších poznámek:

```java
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false"), //vypnutie serveroveho strankovania/vyhladavania
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "2,desc"), //nastavenie usporiadania podla 2. stlpca
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,import,celledit") //vypnutie zobrazenia uvedenych tlacidiel
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "groupId,fullPath"), //vynuti zobrazenie len uvedenych stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"), //JS funkcia ktora sa zavola pre upravu zoznamu stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.groups_title") //nadpis nad datatabulkou na celu sirku okna
```

**API a události**

Vytvořená datová tabulka je k dispozici jako:
- `conf.datatable` na původní `conf` objekt datového sloupce
- `window` objekt s názvem `datatableInnerTable_fieldName` - lze použít pro automatizované testování nebo jiné operace v JavaScriptu.

Po vytvoření vnořené datové tabulky je vyvolána událost `WJ.DTE.innerTableInitialized` kde v objektu `event.detail.conf` je přenesená konfigurace.

## Poznámky k implementaci

Implementace je v souboru [field-type-datatatable.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-datatable.js) a v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) nastavit jako `$.fn.dataTable.Editor.fieldTypes.datatable = fieldTypeDatatable.typeDatatable();`.

Funkce `resizeDatatable` se používá k výpočtu velikosti datové tabulky (pro posouvání pouze řádků), přepočet se volá při inicializaci pole, v intervalu každých 20 sekund (pro jistotu), při změně velikosti okna a při kliknutí na záložku v editoru. Přepočet se provádí pouze tehdy, je-li pole viditelné.

Po kliknutí na kartu v editoru se název karty porovná s kartou, do které je vložena datová tabulka, a pokud se shoduje, šířka sloupce se upraví voláním příkazu. `conf.datatable.columns.adjust();`. Datovou tabulku lze opakovaně použít pro různá data, což zajistí správné nastavení šířky sloupců v záhlaví podle obsahu tabulky.

Funkce `getUrlWithParams` může nahradit pole z objektu json v adrese URL. Pokud adresa URL datového souboru obsahuje `?docid={docId}&groupId={groupId}` takže hodnota `{docId}` a `{groupId}` je nahrazen hodnotami z objektu JSON `EDITOR.currentJson`.
