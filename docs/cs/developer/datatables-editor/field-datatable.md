# Field Type - datatable

Pole datatable umožňuje v editoru stránek zobrazit vnořenou datatabulku (např. seznam Médii v editoru stránek). Je třeba si uvědomit, že datatabulka je inicializována s vlastní URL adresou. V samotném objektu rodičovské tabulky není třeba data posílat ani je následně přijímat, data se mění automaticky při volání REST služby vnořené datatabulky. Technicky by bylo možné pracovat přímo s JSON daty z rodičovské tabulky, zatím tato možnost není implementována.

![](../../redactor/webpages/media.png)

Jelikož aktuálně vložená datatabulka pracuje se samostatnými REST službami vrácená data jsou prázdné pole `[]`.

## Použití anotace

Anotace se používá jako `DataTableColumnType.DATATABLE`, přičemž je třeba nastavit následující atributy editoru:
- `data-dt-field-dt-url` - URL adresa REST služby, může obsahovat makra pro vložení hodnoty z rodičovského editoru, např.: `/admin/rest/audit/notify?docid={docId}&groupId={groupId}`
- `data-dt-field-dt-columns` - jméno třídy (včetně packages) ze které se použije [definice sloupců datatabulky](datatable-columns.md) Např. `sk.iway.iwcm.system.audit.AuditNotifyEntity`
- `data-dt-field-dt-columns-customize` - jméno JavaScript funkce, která může být použita k úpravě`columns` objektu, například `removeEditorFields`. Funkce musí být dostupná přímo ve `windows` objektu, jak parametr dostane`columns` objekt a očekává se, že jej vrátí upravený. Příklad `function removeEditorFields(columns) { return columsn; }`.
- `data-dt-field-dt-tabs` - seznam karet pro editor v JSON formátu. Všechny názvy i hodnoty JSON objektu je třeba obalit do `'`, překlady jsou nahrazeny automaticky. Příklad: `@DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'fields', 'title': '[[#{editor.tab.fields}]]' }]")`.
- `data-dt-field-dt-localJson` - aktivuje režim, který pracuje s lokálním JSON objektem. Používá se primárně pro aplikace ve web stránce pro evidenci položek aplikace (např. položky slide show), které se navíc automaticky kódují do řetězce vhodného do `PageParams` objektu a jsou kódovaném v `Base64`.

Kompletní příklad anotace:

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

Pomocí data atributů lze nastavovat i další konfigurační možnosti datatabulky. Data jsou zasílána jako řetězec. Hodnoty `true` a `false` se konvertují na `boolean` objekt. Nastavení atributu `order` umožňuje nastavit uspořádání pouze pro jeden sloupec. Ostatní možnosti se přenesou jako řetězec.

K menu nastavované možnosti přidejte předponu `data-dt-field-dt-`, tedy pro nastavení možnosti `serverSide` použijte klíč `data-dt-field-dt-serverSide`. Příklad doplňkových anotací:

```java
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false"), //vypnutie serveroveho strankovania/vyhladavania
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "2,desc"), //nastavenie usporiadania podla 2. stlpca
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,import,celledit") //vypnutie zobrazenia uvedenych tlacidiel
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "groupId,fullPath"), //vynuti zobrazenie len uvedenych stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"), //JS funkcia ktora sa zavola pre upravu zoznamu stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.groups_title") //nadpis nad datatabulkou na celu sirku okna
```

**API a události**

Vytvořená datatabulka se zpřístupní jako:
- `conf.datatable` na původním `conf` objektu sloupce datatabulky
- `window` objekt s názvem `datatableInnerTable_fieldName` - objekt lze použít pro automatizované testování nebo jiné JavaScript operace.

Po vytvoření vnořené datatabulky je vyvolána událost `WJ.DTE.innerTableInitialized` kde v objektu `event.detail.conf` je přenesena konfigurace.

## Lokální JSON data

Aktivuje režim práce s lokálními JSON daty. Data jsou získána přímo z hodnoty pole, které může být typu `String` při kterém se provede volání `JSON.parse`.

Pro parametry aplikace ve web stránce se výsledek kóduje do `Base64` aby nedošlo k poškození JSON objektu. Zároveň se aktivuje rozšíření [Row Reorder](https://datatables.net/extensions/rowreorder/) pro možnost uspořádání seznamu pomocí funkce `Drag&Drop`.

V aplikaci tak bude možné upravovat seznam položek, měnit jejich pořadí atp. bez použití a definování REST služby. Výsledek se uloží zpět do JSON objektu a zakóduje přes `Base64`. Při inicializaci se doplní sloupec `ID` a `rowOrder`. Využívá se atribut `DATA.src` objektu `datatables` pro přímé nastavení dat pro tabulku.

Automaticky se aktivuje i funkce pro možnost změny pořadí řádků pomocí funkce `Drag&Drop`. Z důvodu konfliktů při přesunu řádků a jejich různého uspořádání je vypnuta možnost uspořádat seznam podle libovolného sloupce, seznam se pořádá automaticky podle pořadí uspořádání. Pro režim JSON editor se tento sloupec automaticky přidá - všimněte si, že třída `ImpressSlideshowItem` v příkladu níže neobsahuje ani `ID` ani `rowOrder` sloupec, jelikož technicky pro zobrazení dat nejsou nutné. Přidají se automaticky. Pokud potřebujete sloupec ručně zobrazit, použijte anotaci `DataTableColumnType.ROW_REORDER`.

Příklad použití:

```java
public class ImpressSlideshowApp extends WebjetComponentAbstract{
    ...
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "tabLink2", title="&nbsp;", className = "dt-json-editor",editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.appimpressslideshow.ImpressSlideshowItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-localJson", value = "true")
            }
        )})
    private String editorData = null;
}

public class ImpressSlideshowItem {
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "editor.perex.image",
        renderFormat = "dt-format-image-notext"
    )
    private String image;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, className="dt-row-edit", title = "components.app-cookiebar.cookiebar_title")
    private String title;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "editor.subtitle")
    private String subtitle;

    ...
}
```

## Poznámky k implementaci

Implementace je v souboru [field-type-datatable.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-datatable.js) a v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) nastaveno jako `$.fn.dataTable.Editor.fieldTypes.datatable = fieldTypeDatatable.typeDatatable();`.

Funkce `resizeDatatable` se používá pro výpočet velikosti datatabulky (aby scrolovaly jen řádky), přepočet je volán při inicializaci pole, intervalem každých 20 sekund (pro jistotu), při změně velikosti okna a při kliknutí na tab v editoru. Přepočet se provede jen když je pole viditelné.

Při kliknutí na tab v editoru se testuje jméno tabu vůči tabu kde je vložena datatabulka a pokud se shoduje provede se nastavení šířky sloupců voláním `conf.datatable.columns.adjust();`. Datatabulka se může znovu použít pro různá data a toto zajistí korektní nastavení šířky sloupců v hlavičce podle obsahu tabulky.

Funkce `getUrlWithParams` dokáže v URL adrese nahradit pole z json objektu. Pokud URL adresa datatabulky obsahuje `?docid={docId}&groupId={groupId}` tak hodnota `{docId}` a `{groupId}` je nahrazena hodnotami z JSON objektu `EDITOR.currentJson`.
