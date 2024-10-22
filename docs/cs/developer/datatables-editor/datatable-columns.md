# Sloupce datových tabulek

Pole JSON "sloupce" pro DataTables lze generovat pomocí anotací nad vlastnostmi objektu Java. JSON se vloží do souboru pug pomocí volání:

```javascript
let columns = [(${layout.getDataTableColumns('sk.iway.iwcm.components.gallery.GalleryEntity')})];
```

Kde: `sk.iway.iwcm.components.gallery.GalleryEntity` je objekt spravovaný prostřednictvím DataTables.

Všechna pole objektu, která mají anotaci, jsou namapována na JSON. `@DataTableColumn`.

Anotace `@DataTableColumn` má stejné vlastnosti jako [původní objekt sloupců](../datatables/README.md). Pomocí zkratky inputType je také možné nastavit některé vlastnosti, které jsou definovány. `enum dataTableColumnType`.

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

Původní dokumentace na webu [datatables.net](https://datatables.net/reference/option/columns:)

Povinná pole:
- `inputType` - Zkratka, `enum DataTableColumnType` - určuje typ datového pole. Je také možné použít zřetězenou hodnotu (např: `inputType = { DataTableColumnType.OPEN_EDITOR, DataTableColumnType.JSON },`).
- `title` - pokud není zadán, je automaticky generován jako `components.meno_beanu_bez_dto_alebo_bean_na_konci.name` - https://datatables.net/reference/option/columns.title, viz dokumentace pro [přeložil](#překlady-nadpisů-sloupců). **Varování:** pokud je title prázdný (nebo je v něm mezera), pak se atribut automaticky nastaví na sloupec `hidden=true`.

Volitelná pole:
- `tab` - zkratka pro nastavení `{editor: {tab: String}}`
- `className` - další styl CSS nastavený na `TD` pokud chcete nahradit styl CSS nastavený pomocí `inputType` zadejte ji pomocí znaménka `!` na začátku - https://datatables.net/reference/option/columns.className. Existují speciální třídy CSS:
  - `disabled` - nastaví pole na šedou barvu, což evokuje, že není editovatelné.
  - `DTE_Field_Has_Checkbox` - nastaví odsazení od spodního okraje na `-14px` aby před dalším polem nezůstala žádná mezera.
  - `hide-on-create` - skryje pole při vytváření nového záznamu.
  - `hide-on-edit` - skryje pole při úpravě záznamu.
  - `not-export` - pole nebude exportováno.
  - `show-html` - zobrazí se kód HTML v hodnotě, včetně entit typu `&#39;`, nastaví atribut na sloupec `entityDecode: false`.
  - `wrap` - umožňuje obtékání textu, používá se především v polích typu `textarea`.
  - `multiweb-noteditable` - v instalaci MultiWebu je pole zobrazeno šedě, což evokuje, že není editovatelné.
- `name` - https://datatables.net/reference/option/columns.name
- `data` - https://datatables.net/reference/option/columns.data
- `defaultContent` - https://datatables.net/reference/option/columns.defaultContent
- `orderable` - `true/false` hodnota pro povolení možnosti rozložení v datové tabulce
- `renderFormat` - https://datatables.net/reference/option/columns.renderFormat
- `renderFormatLinkTemplate` - https://datatables.net/reference/option/columns.renderFormatLinkTemplate
- `renderFormatPrefix` - https://datatables.net/reference/option/columns.renderFormatPrefix
- `sortAfter` - název pole, za které je toto pole přidáno v pořadí.
- `editor` - objekt `DataTableColumnEditor`
- `hidden` - pole se v datové tabulce nezobrazí a uživatel nemá možnost `visible` nelze zobrazit, pole lze použít v editoru.
- `hiddenEditor` - pokud `true` sloupec se v editoru nezobrazuje
- `visible` - pole je skryté, ale uživatel si ho může zobrazit https://datatables.net/reference/option/columns.visible
- `filter` - pokud false, filtr se v záhlaví tabulky nezobrazí.
- `perms` - hodnota pro kontrolu práv (např. multiDomain), pole se nezobrazí, pokud přihlášený uživatel nemá povoleno dané právo.
- `defaultValue` - výchozí hodnota pro nový záznam (použije se pouze v případě, že je nastaveno `fetchOnCreate` na adrese `false` protože v tomto případě budou vrácena data přednastavená ze serveru). Může obsahovat makra:
  - `{currentDomain}` - je nahrazena aktuálně vybranou doménou
  - `{currentDate}` - se nahradí aktuálním datem
  - `{currentDateTimeSeconds}` - je nahrazen aktuálním datem a časem včetně sekund.
  - `{currentTime}` - je nahrazen pro aktuální čas
- `alwaysCopyProperties` - při editaci záznamu se prázdný `null` hodnoty jsou zachovány a zkopírovány z existujícího objektu v databázi. To neplatí pro pole datum/čas, ta se přepisují automaticky. Pokud to potřebujete použít pro jiný typ pole a také přenést `null` nastavit hodnotu atributu na `true`, nebo na `false` pokud nechcete, aby se pole s datem automaticky přepisovala.

## Vlastnosti @DataTableColumnEditor

- `type` - typ `input` prvek
- `label` - překladový klíč názvu pole v editoru (pokud se liší od klíče `DatatableColumn.title`)
- `message` - překladový klíč pro zobrazení nápovědy, pokud není zadán, automaticky vyhledá překladový klíč podle `DatatableColumn.title.tooltip`. Podporuje zadávání formátování pomocí základních [Markdown](../frameworks/webjetjs.md#markdown-parser).
- `tab` - [karta, ve které se pole nachází](README.md#karty-v-editoru)
- `attr` - `HashMap` Atributy HTML, které mají být nastaveny pro vstupní pole
  - `data-dt-field-hr` (`before/after`) - přidá dělicí čáru před nebo za prvek
  - `data-dt-field-headline` (překladový klíč) - přidá nadpis před prvek
  - `data-dt-field-full-headline` (překladový klíč) - přidá nadpis na celou šířku před prvek (včetně šedé oblasti s názvy prvků), používá se pro nadpis před vnořenou datovou tabulkou v samostatné kartě.
- `multiple` - nastaví atribut `multiple` na poli HTML (používá se pro pole typu `MULTISELECT`).
- `separator` - nastaví oddělovací znak pro pole type `MULTISELECT`. Pokud jsou data prázdná, jsou odesílána a přijímána jako pole, pokud jsou nastavena jako řetězec oddělený zadaným znakem (obvykle čárkou).
- `data-dt-escape-slash` - nastavením na hodnotu `true` zapnout nahrazování znaků `/` pro entitu`&#47;`. Používá se v případě webové stránky a složky, kde je potřeba znak v názvu. `/` nahradit, protože se používá k oddělení silnice.

Překladový klíč pro nápovědu je automaticky vyhledáván podle překladového klíče `title` s koncovkou `.tooltip`. Pokud tedy máte anotaci `@DataTableColumn(title = "group.superior_directory"` automaticky vyhledá text překladu pomocí klíče `group.superior_directory.tooltip`. Pokud existuje, použije se.

## Vlastnosti DataTableColumnType

Nastaví typ pole, více v seznamu [standardní pole formuláře](standard-fields.md).
- `ID` - sloupec primárního klíče
- `OPEN_EDITOR` - automaticky vytvoří odkaz na sloupec pro otevření editoru, měl by být použit na hlavní textové pole, nejlépe první v pořadí.
- `TEXT` - standardní textové pole (jeden řádek)
- `TEXTAREA` - standardní pole pro zadávání více řádků textu
- `SELECT` - doporučujeme zasílat možnosti prostřednictvím [Služba REST](../datatables/restcontroller.md#číselníky-pro-výběrová-pole)
- `MULTISELECT` - výběrové pole pro vícenásobnou volbu
- `BOOLEAN` - zaškrtávací políčko s možnostmi `true/false`
- `CHECKBOX` - zaškrtávací políčko se speciální hodnotou, možnost pro vybranou a nevybranou hodnotu lze nastavit pomocí atributu editoru `@DataTableColumnEditorAttr(key = "unselectedValue", value = "")`
- `DISABLED` - zobrazené pole nebude možné upravovat

Číselné:
- `NUMBER` - číselné pole
- `TEXT_NUMBER` - zobrazí zaokrouhlené číslo, vyšší číslo vypíše v textové podobě, např. `10 tis.` místo `10000`
- `TEXT_NUMBER_INVISIBLE` - číselné pole, které se nezobrazuje v tabulce ani v editoru.

Datováno:
- `DATE` - kliknutím do pole pro zadání data zobrazíte výběr kalendáře.
- `DATETIME` - pole pro zadání data a času, po kliknutí do pole se zobrazí výběr kalendáře s možností zadat čas.
- `TIME_HM` - pole pro zadávání pouze času, po kliknutí do pole se zobrazí výběr s možností výběru hodin a minut.
- `TIME_HMS` - pole pro zadávání pouze času, po kliknutí do pole se zobrazí výběr s možností výběru hodin, minut a sekund.

Speciální:
- `GALLERY_IMAGE` - speciální typ pole pro zobrazení obrázku s vypnutým popiskem sloupce.
- `QUILL` - jednoduchý editor se základním formátováním kódu HTML, jako je tučné písmo, kurzíva atd.
- `WYSIWYG` - plnohodnotný editor kódu HTML pro webové stránky
- `JSON` - výběrové pole [adresář](field-json.md)
- `DATATABLE` - [vnořená datová tabulka](field-datatable.md)
- `ELFINDER` - [výběr odkazu](field-elfinder.md) do souboru/webové stránky

## Možnosti výběrového pole

Typ pole `DataTableColumnType.SELECT` můžete nastavit `option` hodnoty nad:
- [Služba REST](../datatables/restcontroller.md#Číselníky-pro-výběrová-pole) a nastavení voličů pro výběrová pole. Toto je preferované řešení pro standardní datové tabulky.
- Nastavení atributů možností přímo pomocí anotace `@DataTableColumnEditorAttr(key = "Slovensky", value = "sk")`.
- Voláním statické metody pomocí anotace `@DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value")`. V `key` atribut je specifikován předponou `method:` a metoda, která musí vracet `List` objekty. V atributu `value = "label:value"` anotaci je přiřazen název atributu pro popis a název atributu pro hodnotu výběrového pole (v příkladu se jmenuje `objekt.getLabel() a objekt.getValue()`).
- Připojení k aplikaci číselníků zadáním `@DataTableColumnEditorAttr(key = "enumeration:Okresne Mestá", value = "string1:string2")`. V `key` prefix je uveden v atributu `enumeration:` jméno nebo ID vytáčení. V atributu `value = "string1:string2"` anotace je uveden název atributu pro popis a název atributu pro hodnotu výběrového pole - v příkladu se jmenuje `objekt.getString1() a objekt.getString2()`.

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

Povinná pole lze opatřit poznámkami:
- `@NotEmpty` - nevyprázdní pole, neumožňuje zadat mezeru nebo tabulátor.
- `@NotBlank` - nevyprázdní pole, ale umožní zadat mezeru.

Další možnosti ověřování jsou popsány v dokumentaci pro. [restcontroller](../datatables/restcontroller.md#validace---povinná-pole).

## Ověřování

Standardní anotace jsou podporovány pro ověřování polí pomocí [javax.validation.Validator](https://www.baeldung.com/javax-validation). Příklad ověření délky pole:

```java
        @Size(min = 10, max = 20, message = "form.p2.size")
        private String p2;
```

překladové tlačítko pro zobrazení konkrétní nastavené hodnoty min a max:

```properties
form.p2.size=Pole P2 musí byť medzi {min} a {max} znakmi
```

## Pomocník JS pro rozšíření vlastností

Soubor `app-init.js` obsahuje funkci `WJ.DataTable.mergeColumns` přidat vlastnosti na základě názvu objektu (názvu pole). Projde pole objektů sloupců, najde objekt se stejným názvem jako argumentovaný objekt a použije příkaz `jQuery.extend` se tento objekt rozšíří.

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

Pokud potřebujete pouze přidat nový sloupec, můžete to provést jednoduchým přidáním do seznamu. V příkladu jde o přidání *Skryté stránky* pole (atribut `visible: false`):

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

Často je nutné přidat k entitě další atributy pro editor (např. `checkbox` pro použití změny na podřízené entity, další informační pole atd.). Za tímto účelem lze entitu rozšířit o nový atribut (který není uložen v databázi) obsahující další údaje. Obvykle jej nazýváme `editorFields` a implementovat potřebnou třídu pro danou entitu. Příklady jsou v [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) nebo [GroupEditorFields](../../../src/main/java/sk/iway/iwcm/doc/GroupEditorField.java). Ve třídách je pouze atribut editorField, např. `private DocEditorFields editorFields = null;`.

Implementovaná třída `EditorFields`, např. [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) obvykle obsahuje metody `fromDocDetails` pro nastavení atributů v `editorFields` třídy před úpravami a `toDocDetails` pro nastavení atributů zpět do `DocDetails` před uložením. Tyto metody je třeba v kódu v jazyce Java volat implicitně.

**Varování:** pokud je entita uložena v mezipaměti (jako např. [GroupDetails](../../../src/main/java/sk/iway/iwcm/doc/GroupDetails.java)) nastavení atributu `editorFields` zůstane také v mezipaměti a může zbytečně zabírat paměť a vytvářet zbytečně velká data při serializaci JSON. V `GroupDetails` v editorFields odkazuje na `parentGroupDetails`.

Při standardním postupu se každý `GroupDetails` sada objektů `editorFields` objekt. Při serializaci hluboce vnořeného adresáře se pak vnoří objekty editorFields.parentGroupDetails.editorFields.parentGroupDetails atd. Objekt GroupDetails prostě neměl potřebné první pole editorFields. Řešením je nejprve objekt `GroupDetails` naklonovat a nastavit ji `editorFields`. Příkladem je `GroupEditorField.fromGroupDetails` který objekt naklonuje a poté jej vrátí. Použití v kódu je pak následující `group = gef.fromGroupDetails(group);`.

Běžné metody pro datatable jsou ve třídě [BaseEditorFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java) které může vaše třída rozšířit. Obsahuje metody pro přidání řádku třídy CSS a přidání ikony do nadpisu. Viz dokumentaci k [stylování datové tabulky](../datatables/README.md#stylování-řádků).

Pro vložení anotace vnořených atributů je možné použít anotaci `@DatatableColumnNested` jako např. v [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) na atributu `editorFields`:

```java
@DataTableColumnNested
@Transient
private DocEditorFields editorFields = null;
```

anotovaný atribut bude prohledán kvůli anotaci. `@DatatableColumn` rekurzivní. Výsledkem je generování vnořené anotace. Všimněte si atributu data a name obsahujícího předponu `editorFields.`. Pokud potřebujete nastavit vlastní prefix (název proměnné), můžete použít parametr prefix. `@DataTableColumnNested(prefix = "menoPremennej")`. Prefix můžete také nastavit na prázdnou hodnotu, pak se prefix negeneruje.

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

Anotace `@Transient` Informuje entity JPA, že atribut není uložen v databázi.

Nastavení dat mezi entitou a `editorFields` je možné implementovat metody v řadiči REST. `processFromEntity` pro nastavení `editorFields` atributy nebo `processToEntity` pro nastavení atributů v entitě z `editorFields`. Příkladem může být [UserDetailsController](../../../src/main/java/sk/iway/iwcm/components/users/userdetail/UserDetailsController.java). Metody jsou automaticky volány při čtení všech záznamů, načítání jednoho záznamu, vyhledávání nebo ukládání dat.

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

## Třídění pořadí polí

Ve výchozím nastavení jsou pole uspořádána v pořadí, v jakém jsou zapsána ve zdrojovém kódu (ačkoli to specifikace anotací nezaručuje, funguje to tak). Pokud však používáte vnořené atributy, nelze pořadí nastavit podle pořadí v kódu.

Proto je možné použít atribut `sortAfter` do kterého zadáte atribut dat předchozího pole. Anotované pole se pak přidá do výstupu JSON za zadané pole. Logika je implementována v metodě [DataTableColumnsFactory.sortColumns](../../../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java).

V případě potřeby lze zadat zvláštní hodnotu `sortAfter = "FIRST"` přesunout pole na začátek seznamu. To by mělo být použito v případě rozšířených entit prostřednictvím `@MappedSuperclass` pokud i první `id` atribut je v této entitě.

Příklad použití:

```java
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "Povolit zmenu URL", tab = "basic",
        sortAfter = "virtualPath"
    )
    private Boolean allowChangeUrl=false;
```

## Překlady nadpisů sloupců

Jak bylo uvedeno výše, atribut `title` obsahuje název sloupce. Z důvodů překladu nelze použít přímo název sloupce, je třeba použít překladový klíč.

Pokud je atribut `title` nezadáte, překladový klíč se automaticky vyhledá ve formátu `components.meno_beanu_bez_dto_alebo_bean_na_konci.name`, např. `components.monitoring.date_insert`.

**Pokud upravujete existující aplikaci/komponentu.** z WebJET 8 do WebJET 2021, je nejlepší vyhledat původní překladové klíče. Ušetříte tak čas při překladu, protože WeBJET 8 je již přeložen do několika jazyků.

V souboru [src/main/webapp/files/translations.properties](../../../src/main/webapp/files/preklady.properties) je původní soubor překladu z WebJET 8 (jen pro příklad, nijak jej neupravujte). Můžete v něm vyhledat požadovaný text a do `title` atribut pro zadání nalezeného překladového klíče.

Další možností je zobrazit původní stránku s parametrem URL `?showTextKeys=true` což způsobí, že se před textem zobrazí překladové klávesy. Stránka bude z hlediska designu pravděpodobně nefunkční (protože text bude příliš dlouhý), ale na klíče se můžete podívat prostřednictvím inspektora.

Například:

```
http://iwcm.interway.sk/components/server_monitoring/admin_monitoring_all.jsp?showTextKeys=true
```

Pokud již formulář na stránce odešlete, zobrazí se samozřejmě původní texty, parametr, který přidáte jako `&showTextKeys=true`, ale pravděpodobně se zobrazí stránka 403/404 kvůli ochraně WebJET. Řešením je použití konzoly JavaScriptu, do které zadáte:

```javascript
window.location.href=window.location.href+"&showTextKeys=true";
```

správně projde ochranou WebJETu a klíče se vám zobrazí.

**Pokud jste vytvořili novou aplikaci nebo jste nenašli vhodný překladový klíč.** je třeba jej přidat do souboru [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties).

Po přidání překladu je třeba soubor znovu načíst. `text-webjet9.properties` WebJETom. Uděláte to tak, že zavoláte [domovská stránka s parametrem ?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true) nebo restartováním aplikačního serveru.
