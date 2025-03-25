# Datatables columns

JSON pole "columns" pro DataTables lze vygenerovat anotacemi nad vlastnostmi Java objektu. JSON se do pug souboru vloží pomocí volání:

```javascript
let columns = [(${layout.getDataTableColumns('sk.iway.iwcm.components.gallery.GalleryEntity')})];
```

Kde `sk.iway.iwcm.components.gallery.GalleryEntity` je objekt manageováni přes DataTables.

Do JSON se mapují všechna pole objektu, která mají anotaci `@DataTableColumn`.

Anotace `@DataTableColumn` má stejné vlastnosti jako [původní columns objekt](../datatables/README.md). Dále lze nastavit určité vlastnosti pomocí zkratky inputType, které jsou definovány `enum dataTableColumnType`.

## Příklady

```java
//TranslationKeyDto
@DataTableColumn(inputType = {DataTableColumnType.ID})
private Integer id;

@DataTableColumn(inputType = {DataTableColumnType.OPEN_EDITOR})
private String key;

@DataTableColumn(renderFormat = "dt-format-text-wrap", editor = {
        @DataTableColumnEditor(
                type = "textarea"
        )
})
private String value;

@DataTableColumn(renderFormat = "dt-format-text-wrap", editor = {
        @DataTableColumnEditor(
                type = "text",
                attr = {
                        @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
        )
})
private String oldValue;

@DataTableColumn(renderFormat =  "dt-format-date-time", editor = {
        @DataTableColumnEditor(
                type = "datetime",
                attr = {
                        @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
        )
})
private Date updateDate;


//GalleryEntity
@Size(max = 255)
@Column(name = "image_name")
@DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, tab = "metadata", title="[[#{components.gallery.fileName}]]")
private String imageName;

@Size(max = 255)
@Column(name = "image_path")
@DataTableColumn(inputType = DataTableColumnType.TEXT, title="admin.temp_group_list.directory", tab = "metadata",
    editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
                @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
        })
})
private String imagePath;

//nastavenie unselectedValue pre pole checkboxov a atributu sortAfter
@DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "[[#{editor.access_restrictions_enable}]]",
        tab = "access",
        sortAfter = "editorFields.loggedSitemap",
        editor = {
                @DataTableColumnEditor(
                attr = {
                        @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                        @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.access_restrictions"),
                        @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                },
                options = {
                        @DataTableColumnEditorAttr(key = "editor.menu_show", value = "true"),
                        @DataTableColumnEditorAttr(key = "editor.menu_hide", value = "false")
                }
                )
        }
)
private Integer[] passwordProtected;
```

## Vlastnosti @DataTableColumn

Původní dokumentace na stránce [datatables.net](https://datatables.net/reference/option/columns:)

Povinná pole:
- `inputType` - zkratka, `enum DataTableColumnType` - určuje typ datového pol. Možné je použít i spojenou hodnotu (např.: `inputType = { DataTableColumnType.OPEN_EDITOR, DataTableColumnType.JSON },`).
- `title` - pokud není zadán automaticky se vygeneruje jako `components.meno_beanu_bez_dto_alebo_bean_na_konci.name` - https://datatables.net/reference/option/columns.title, viz dokumentace k [překladem](#překlady-názvů-sloupců). **Upozornění:** pokud je title prázdný (nebo tvrdá mezera) tak se automaticky sloupci nastaví atribut `hidden=true`.

Volitelná pole:
- `tab` - zkratka pro nastavení `{editor: {tab: String}}`
- `className` - doplňkový CSS styl nastaven na `TD` elemente, chcete-li nahradit CSS styl nastavený pomocí `inputType` zadejte jej se znakem `!` na začátku - https://datatables.net/reference/option/columns.className. Existují speciální CSS třídy:
  - `disabled` - nastaví pole na šedou barvu, což evokuje, že je needitovatelné.
  - `DTE_Field_Has_Checkbox` - nastaví odsazení od spodu na hodnotu `-14px` aby nezůstala mezera před dalším polem.
  - `hide-on-create` - schová pole při pořizování nového záznamu.
  - `hide-on-edit` - schová pole při editaci záznamu.
  - `hide-on-duplicate` - schová pole při duplikování záznamu.
  - `not-export` - pole nebude exportováno.
  - `show-html` - zobrazí se HTML kód v hodnotě včetně entit typu `&#39;`, sloupci nastaví atribut `entityDecode: false`.
  - `wrap` - zapne zalamování textu, používá se primárně v polích typu `textarea`.
  - `multiweb-noteditable` - v MultiWeb instalaci je pole zobrazeno šedou barvou, což evokuje, že je needitovatelné.
- `name` - https://datatables.net/reference/option/columns.name
- `data` - https://datatables.net/reference/option/columns.data
- `defaultContent` - https://datatables.net/reference/option/columns.defaultContent
- `orderable` - `true/false` hodnota pro zapnutí možnosti uspořádání v datatabulce
- `renderFormat` - https://datatables.net/reference/option/columns.renderFormat
- `renderFormatLinkTemplate` - https://datatables.net/reference/option/columns.renderFormatLinkTemplate
- `renderFormatPrefix` - https://datatables.net/reference/option/columns.renderFormatPrefix
- `sortAfter` - jméno pole za které se přidá toto pole v pořadí
- `editor` - objekt `DataTableColumnEditor`
- `hidden` - pole se nezobrazí v datatabulce a uživatel si jej na rozdíl od `visible` nemůže zobrazit, pole může být použito v editoru
- `hiddenEditor` - pokud je `true` nezobrazí se sloupec v editoru
- `visible` - pole se schová, ale uživatel si jej může zobrazit https://datatables.net/reference/option/columns.visible
- `filter` - je-li false, nezobrazí se v hlavičce tabulky filtr
- `perms` - hodnota pro kontrolu práv (např. multiDomain), pole se nezobrazí, pokud přihlášený uživatel nemá uvedené právo povoleno
- `defaultValue` - výchozí hodnota pro nový záznam (použije se pouze pokud je nastaveno `fetchOnCreate` na `false`, protože pro tento případ se vrátí data přednastavená ze serveru). Může obsahovat makra:
  - `{currentDomain}` - nahradí se za aktuálně zvolenou doménu
  - `{currentDate}` - nahradí se za aktuální datum
  - `{currentDateTimeSeconds}` - nahradí se za aktuální datum a čas včetně sekund
  - `{currentTime}` - nahradí se za aktuální čas
- `alwaysCopyProperties` - při editaci záznamu se prázdné `null` hodnoty zachovají a zkopírují ze stávajícího objektu v databázi. Pro pole typu datum/čas to neplatí, ty se přepíší automaticky. Pokud potřebujete toto použít i pro jiný typ pole a přenést i `null` hodnotu nastavte atribut na `true`, případně na `false` pokud nechcete automatický přepis pro datová pole.

## Vlastnosti @DataTableColumnEditor

- `type` - typ `input` elementu
- `label` - překladový klíč názvu pole v editoru (pokud se odlišuje od `DatatableColumn.title`)
- `message` - překladový klíč pro zobrazení tooltipu, není-li zadáno automaticky se hledá překlad pro klíč podle `DatatableColumn.title.tooltip`. Podporuje zadání formátování pomocí základního [Markdown](../frameworks/webjetjs.md#markdown-parser).
- `tab` - [karta, ve kterém se pole nachází](README.md#karty-v-editoru)
- `attr` - `HashMap` HTML atributů, které se nastaví vstupnímu poli
  - `data-dt-field-hr` (`before/after`) - přidá rozdělovací čáru před nebo za element
  - `data-dt-field-headline` (překladový klíč) - přidá před element nadpis
  - `data-dt-field-full-headline` (překladový klíč) - přidá před element nadpis na celou šířku (včetně šedé plochy s názvy elementů), používá se pro nadpis před vnořenou datatabulkou v samostatné kartě
- `multiple` - nastavuje atribut `multiple` na HTML poli (používá se pro pole typu `MULTISELECT`).
- `separator` - nastavuje oddělovací znak pro pole typu `MULTISELECT`. Pokud je prázdná data se posílají a přijímají jako pole, pokud je nastaveno tak jako řetězec oddělený uvedeným znakem (typicky čárka).
- `data-dt-escape-slash` - nastavením na hodnotu `true` zapnete náhradu znaku `/` za entitu `&#47;`. Používá se v případě web stránky a složky, kde je třeba v názvu znak `/` nahradit, jelikož se používá k oddělení cesty.

Překladový klíč pro tooltip se automaticky hledá podle překladového klíče `title` se suffixem `.tooltip`. Máte-li tedy anotaci `@DataTableColumn(title = "group.superior_directory"` automaticky se hledá překladový text s klíčem `group.superior_directory.tooltip`. Pokud existuje, použije se.

## Vlastnosti DataTableColumnType

Nastavuje typ pole, více v seznamu [standardních formulářových polí](standard-fields.md).
- `ID` - sloupec s primárním klíčem
- `OPEN_EDITOR` - automaticky na sloupci vytvoří odkaz na otevření editoru, mělo by se použít na hlavní textové pole, ideálně první v pořadí
- `TEXT` - standardní textové pole (jeden řádek)
- `TEXTAREA` - standardní pole pro zadání více řádků textu
- `SELECT` - výběrové pole, možnosti doporučujeme odesílat přes [REST službu](../datatables/restcontroller.md#číselníky-pro-select-boxy)
- `MULTISELECT` - výběrové pole pro výběr více možností
- `BOOLEAN` - zaškrtávací pole s možnostmi `true/false`
- `CHECKBOX` - zaškrtávací pole se speciální hodnotou, možnost pro vybranou i nevybranou hodnotu lze nastavit atributem editoru `@DataTableColumnEditorAttr(key = "unselectedValue", value = "")`
- `DISABLED` - zobrazené pole nebude editovatelné

Číselné:
- `NUMBER` - číselné pole
- `TEXT_NUMBER` - zobrazí zaokrouhlené číslo, při vyšším čísle vypíše v textové podobě. `10 tis.` místo `10000`
- `TEXT_NUMBER_INVISIBLE` - číselné pole, které se nezobrazí v tabulce ani v editoru

Datové:
- `DATE` - pole pro zadání data, po kliknutí do pole zobrazí kalendářový výběr
- `DATETIME` - pole pro zadání data a času, po kliknutí do pole zobrazí kalendářový výběr s možností zadání časového údaje
- `TIME_HM` - pole pro zadání výhradně času, po kliknutí do pole se zobrazí výběr s možností zvolení hodin a minut
- `TIME_HMS` - pole pro zadání výhradně času, po kliknutí do pole se zobrazí výběr s možností zvolení hodin, minut a sekund

Speciální:
- `GALLERY_IMAGE` - speciální typ pole pro zobrazení obrázku s vypnutým titulkem sloupce
- `QUILL` - jednoduchý editor se základním formátováním HTML kódu jako tučné písmo, kurzíva a podobně
- `WYSIWYG` - plnohodnotný editor HTML kódu pro web stránku
- `JSON` - pole pro výběr [adresáře](field-json.md)
- `DATATABLE` - [vnořená datatabulka](field-datatable.md)
- `ELFINDER` - [výběr odkazu](field-elfinder.md) na soubor / web stránku

## Možnosti výběrového pole

Polu typu `DataTableColumnType.SELECT` můžete nastavovat `option` hodnoty přes:
- [REST službu](../datatables/restcontroller.md#Číselníky-pro-select-boxy) a nastavování číselníků pro select boxy. Toto je preferované řešení pro standardní datatabulky.
- Nastavením options atributů přímo pomocí anotace `@DataTableColumnEditorAttr(key = "Slovensky", value = "sk")`.
- Voláním statické metody pomocí anotace `@DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value")`. V `key` atributu je zadaná prefixem `method:` třídě a metoda, která musí vrátit `List` objektů. V atributu `value = "label:value"` anotace je zadáno jméno atributu pro popis a jméno atributu pro hodnotu výběrového pole (v příkladu se tedy jmenuje `objekt.getLabel() a objekt.getValue()`).
- Napojením na aplikaci číselníky zadáním `@DataTableColumnEditorAttr(key = "enumeration:Okresne Mestá", value = "string1:string2")`. V `key` atributu je zadaný prefix `enumeration:` jméno nebo ID číselníku. V atributu `value = "string1:string2"` anotace je zadáno jméno atributu pro popis a jméno atributu pro hodnotu výběrového pole - v příkladu se tedy jmenuje `objekt.getString1() a objekt.getString2()`.

```java
@DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", editor = {
        @DataTableColumnEditor(
                options = {
                //klasicky option tag
                @DataTableColumnEditorAttr(key = "Slovensky", value = "sk"),
                @DataTableColumnEditorAttr(key = "Česky", value = "cz"),

                //ukazka ziskania zoznamu krajin volanim statickej metody, vo value su mena property pre text a hodnotu option pola
                @DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value"),

                //ukazka napojenia na ciselnik, mozne je zadat meno alebo ID ciselnika, vo value su mena property pre text a hodnotu option pola
                @DataTableColumnEditorAttr(key = "enumeration:Okresne Mestá", value = "string1:string2")
                }
        )
})
private String country;
```

## Povinná pole

Povinná pole lze označit anotacemi:
- `@NotEmpty` - neprázdné pole, neumožní zadat mezeru nebo tabulátor
- `@NotBlank` - neprázdné pole, umožní ale zadat mezeru

Další možnosti validace jsou popsány v dokumentaci k [restcontrolleru](../datatables/restcontroller.md#validace--povinná-pole).

## Validace

Podporovány jsou standardní anotace pro validace polí pomocí [javax.validation.Validator](https://www.baeldung.com/javax-validation). Příklad validace délky pole:

```java
        @Size(min = 10, max = 20, message = "form.p2.size")
        private String p2;
```

překladový klíč pro zobrazení konkrétní nastavené hodnoty pro min i max:

```properties
form.p2.size=Pole P2 musí byť medzi {min} a {max} znakmi
```

## JS Helper pro rozšíření vlastností

Soubor `app-init.js` obsahuje funkci `WJ.DataTable.mergeColumns` pro doplnění vlastností, na základny názvu objektu (pole name). Přeiteruje pole objektů columns, najde objekt se stejným name jako argument objekt a pomocí `jQuery.extend` tento objekt rozšíří.

```javascript
WJ.DataTable.mergeColumns(columns, {
    name: "datatableImage",
    render: function (data, type, row) {
        return '<div class="img" style="background-image:url(/thumb' + row.imagePath + '/' + row.imageName + '?w=600&h=400&q=90&v=' + (new Date()).getTime() + ');"></div>';
    },
    className: "dt-image",
    renderFormat: "dt-format-none"
});
```

Pokud potřebujete jen doplnit nový sloupec můžete to provést jednoduše přidáním do seznamu. V příkladu se jedná o přidání *skrytého* pole (atribut `visible: false`):

```javascript
columns.push({
        data : "availableGroups",
        name : "availableGroups",
        title : "[[\#{admin.temp.edit.showForDir}]]",
        visible: false,
        editor: {
                type : "text",
                tab : "accessTab"
        }
});
```

## Vnořené atributy

Často je nutné k entitě přidat pro editor doplňkové atributy (např. `checkbox` pro aplikování změny i na podřazené entity, doplňkové pole s informací atd.). Pro tento účel lze entitu rozšířit o nový atribut (který se neukládá do databáze) obsahující doplňkové údaje. Typicky ho voláme `editorFields` a pro entitu implementujeme potřebnou třídu. Příklady jsou v [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) nebo [GroupEditorFields](../../../src/main/java/sk/iway/iwcm/doc/GroupEditorField.java). Ve třídách je následně jen editorField atribut. `private DocEditorFields editorFields = null;`.

Implementovaná třída `EditorFields` Např. [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) typicky obsahuje metody `fromDocDetails` pro nastavení atributů v `editorFields` třídě před editací a `toDocDetails` pro zpětné nastavení atributů v `DocDetails` před uložení. Tyto metody je třeba implicitně volat ve vašem Java kódu.

!>**Upozornění:** pokud je entita ukládána v cache (např. [GroupDetails](../../../src/main/java/sk/iway/iwcm/doc/GroupDetails.java)) nastavení atributu `editorFields` zůstane iv cache a může zbytečně zabírat paměť a vytvářet při JSON serializaci zbytečně velká data. V `GroupDetails` v editorFields odkazuje na `parentGroupDetails`.

Při standardním postupu se postupně na každém `GroupDetails` objektu nastavil `editorFields` objekt. Při serializaci hluboce vnořeného adresáře se následně vnořovaly objekty editorFields.parentGroupDetails.editorFields.parentGroupDetails atp. Objekt GroupDetails neměl jen potřebný první editorFields. Řešením je nejprve objekt `GroupDetails` naklonovat a až tak do něj nastavit `editorFields`. Příklad je v `GroupEditorField.fromGroupDetails` který naklonuje objekt a následně jej vrátí. Použití v kódu je pak jako `group = gef.fromGroupDetails(group);`.

Společné metody pro datatabulku jsou ve třídě [BaseEditorFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java), kterou může vaše třída rozšířit. Obsahuje metody pro přidání CSS třídy řádku a přidání ikony k titulku. Více v dokumentaci k [stylování datatabulky](../datatables/README.md#stylování-řádku).

Pro vložení anotace vnořených atributů lze použít anotaci `@DatatableColumnNested` jako je např. v [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) na atributu `editorFields`:

```java
@DataTableColumnNested
@Transient
private DocEditorFields editorFields = null;
```

takto anotovaný atribut bude prohledán k anotaci `@DatatableColumn` rekurzivní. Výsledkem bude vygenerování vnořené anotace. Všimněte si data a name atribut obsahující prefix `editorFields.`. Pokud potřebujete nastavit vlastní prefix (jméno proměnné) můžete použít parametr prefix `@DataTableColumnNested(prefix = "menoPremennej")`. Prefix můžete nastavit i na prázdnou hodnotu, tehdy se prefix nevygeneruje.

```javascript
{
  "data" : "editorFields.allowChangeUrl",
  "name" : "editorFields.allowChangeUrl",
  "title" : "Povolit zmenu URL",
  "renderFormat" : "dt-format-boolean-true",
  "editor" : {
    "type" : "checkbox",
    "tab" : "basic"
  }
}, {
  "data" : "editorFields.test",
  "name" : "editorFields.test",
  "title" : "TeST hodNOTa",
  "renderFormat" : "dt-format-text",
  "editor" : {
    "type" : "text",
    "tab" : "basic"
  }
}
```

Anotace `@Transient` říká JPA entitám, že daný atribut není ukládán do databáze.

Pro nastavení údajů mezi entitou a `editorFields` v REST controlleru lze implementovat metody `processFromEntity` pro nastavení `editorFields` atributů nebo `processToEntity` pro nastavení atributů v entitě z `editorFields`. Příklad je vidět v [UserDetailsController](../../../src/main/java/sk/iway/iwcm/components/users/userdetail/UserDetailsController.java). Metody se automaticky volají při čtení všech záznamů, při získání jednoho záznamu, vyhledávání nebo při ukládání dat.

```java
    /**
         * Vykona upravy v entite pred vratenim cez REST rozhranie
         * napr. vyvola potrebne editorFields nastavenia (from entity to editorFields)
         * @param entity
         * @param action - typ zmeny - create,edit,getall...
         */
    @Override
    public UserDetailsEntity processFromEntity(UserDetailsEntity entity, ProcessItemAction action) {
        boolean loadSubQueries = false;
        if (ProcessItemAction.GETONE.equals(action)) {
            loadSubQueries = true;
            if (entity == null) entity = new UserDetailsEntity();
        }

        if(entity != null && entity.getEditorFields() == null) {
            UserDetailsEditorFields udef = new UserDetailsEditorFields();
            udef.fromUserDetailsEntity(entity, loadSubQueries, getRequest());
        }
        return entity;
    }

    /**
	 * Vykona upravy v entite pri zapise cez REST rozhranie
	 * napr. vyvola potrebne editorFields nastavenia (from editorFields to entity)
	 * @param entity
	 * @param action - typ zmeny - create,edit,getall,
	 */
    @Override
    public UserDetailsEntity processToEntity(UserDetailsEntity entity, ProcessItemAction action) {
        if(entity != null) {
            //Call toUserDetailsEntity to set new entity values from EditorFields
            UserDetailsEditorFields udef = new UserDetailsEditorFields();
            udef.toUserDetailsEntity(entity, getRequest());
        }
        return entity;
    }
```

## Sortování pořadí polí

Pole jsou ve výchozím nastavení uspořádána v pořadí, jak jsou zapsána ve zdrojovém kódu (i když specifikace anotace to negarantuje, funguje to tak). Pokud ale používáte vnořené atributy pořadí neumíte nastavit pořadím v kódu.

Proto je možné využít atribut `sortAfter` do kterého zadáte data atribut předchozího pole. Anotované pole se následně do JSON výstupu přidá za uvedené pole. Logika je implementována v metodě [DataTableColumnsFactory.sortColumns](../../../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java).

V případě potřeby lze zadat speciální hodnotu `sortAfter = "FIRST"` pro přesun pole na začátek seznamu. Je třeba to použít v případě rozšířených entit přes `@MappedSuperclass` i první `id` atribut je v této entitě.

Příklad použití:

```java
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "Povolit zmenu URL", tab = "basic",
        sortAfter = "virtualPath"
    )
    private Boolean allowChangeUrl=false;
```

## Překlady názvů sloupců

Jak je uvedeno výše, atribut `title` obsahuje jméno sloupce. Z důvodu překladů nemůžete použít přímo český název sloupce, je třeba použít překladový klíč.

Když atribut `title` nezadáte, automaticky se hledá překladový klíč ve formátu `components.meno_beanu_bez_dto_alebo_bean_na_konci.name`, neboli například. `components.monitoring.date_insert`.

**Pokud upravujete existující aplikaci/komponentu** z WebJET 8 do WebJET 2021 je nejlepší vyhledat původní překladové klíče. Ušetří se tak čas při překladech, protože WeBJET 8 je již přeložen do více jazyků.

V souboru [src/main/webapp/files/text.properties](../../../../src/main/webapp/WEB-INF/classes/text.properties) je původní překladový soubor z WebJET 8 (pouze pro ukázku, v žádném případě jej nemodifikujte). Můžete vyhledat požadovaný text v něm a do `title` atributu zadat nalezený překladový klíč.

Jinou možností je zobrazit původní stránku s URL parametrem `?showTextKeys=true` což způsobí zobrazení překladových klíčů před textem. Stránka bude pravděpodobně rozbitá z designového pohledu (jelikož texty budou příliš dlouhé), ale přes inspektor se umíte na klíče podívat.

Například:

```
http://iwcm.interway.sk/components/server_monitoring/admin_monitoring_all.jsp?showTextKeys=true
```

Pokud již odešlete formulář na stránce, samozřejmě se vám zobrazí původní texty, parametr přidáte jako `&showTextKeys=true`, ale pravděpodobně se vám zobrazí stránka 403/404 z důvodu ochrany WebJETu. Řešení je použít JavaScript konzoli, kde zadáte:

```javascript
window.location.href=window.location.href+"&showTextKeys=true";
```

to korektně projde ochranou WebJETu a klíče se vám zobrazí.

**Pokud jste vytvořili novou aplikaci, nebo jste nenašli vhodný překladový klíč** je třeba jej přidat do souboru [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties).

Po přidání překladu je třeba znovu načíst soubor `text-webjet9.properties` WebJETem. To provedete voláním [úvodní stránky s parametrem ?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true) nebo restartem aplikačního serveru.
