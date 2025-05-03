# Typy polí pro editor

V editoru lze použít všechna standardní formulářová pole.

## ID

Reprezentuje sloupec s primárním klíčem, příklad:

```java
public class CalendarEventsEntity {

    @Id
    @Column(name = "calendar_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_calendar")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

}
```

## OPEN\_EDITOR

Textový typ pole který zobrazí odkaz na otevření editoru záznamu, příklad:

```java
    @Column(name = "title")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="calendar.name",
        tab = "basic"
    )
    @NotBlank
    private String title;
```

## TEXT

Standardní textové pole, příklad:

```java
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.street")
    private String street;
```

![](../../frontend/webpages/customfields/webpages-text.png)

## NUMBER/TEXT\_NUMBER

Textové pole `type="number"`, prohlížeč typicky zobrazí v poli i šipky pro zvětšení/zmenšení zadané hodnoty. Příklad:

```java
    @Column(name = "priority")
	@NotNull
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.banner.priority",
		tab = "main"
    )
	private Integer priority;
```

Rozdíl mezi `NUMBER` a `TEXT_NUMBER` je v zobrazení v datatabulce. `TEXT_NUMBER` zobrazí zaokrouhlené číslo, při vyšším čísle vypíše v textové podobě. `10 tis.` místo `10000`. V editoru je chování stejné (zobrazí se přesná hodnota).

## PASSWORD

Textové pole `type="password"` pro zadání hesla.

```java
    @DataTableColumn(
        inputType = DataTableColumnType.PASSWORD,
        title = "components.user.password",
        tab = "personalInfo",
        className = "required"
    )
    @Transient //toto nechceme citat z DB
    private String password;
```

## TEXTAREA

Více řádkové textové pole. Dlouhý text se nezalamuje, pokud chcete zalomit dlouhý text do více řádků nastavte `className = "wrap"`:

```java
    @Column(name = "gallery_perex")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "editor.tab.perex",
        tab = "basic"
    )
    private String perex = "";

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        tab = "basic",
        title="components.app-cookiebar.text",
        className = "wrap",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(
                    key = "placeholder",
                    value = "components.app-cookiebar.cookie_text")
            }
        )
    })
    private String cookie_text;
```

## DATE

Výběr data, po kliknutí do pole zobrazí okno pro výběr data.

Všimněte si nastavení `@Temporal(TemporalType.TIMESTAMP)` pro korektní konverzi pole do databázového sloupce (vyžaduje to JPA).

```java
    @Column(name = "date_from")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="calendar.begin",
        tab = "basic"
    )
	private Date dateFrom;
```

## DATETIME

Podobné pole jako `DATE` ale navíc umožňuje také výběr času.

```java
    @Column(name = "date_to")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.banner.dateTo",
		visible = false,
		tab = "restrictions"
    )
	private Date dateTo;
```

## TIME\_HM a TIME\_HMS

Podobná pole jako `DATETIME` ale umožňují POUZE výběr času. Verze `TIME_HM` je výběr času pomocí hodin a minut. Verze `TIME_HMS` umožňuje výběr času pomocí hodin, minut a vteřin.

Všimněte si nastavení `@Convert(converter = DefaultTimeValueConverter.class)`, které nastaví pro každý zvolený čas stejné datum a to `01.01.2000` přičemž výběr času se nezmění. Toto nastavení stejného data není vidět, ale je důležité z hlediska fungování při filtrování záznamů pomocí těchto časových polí.

Třeba si uvědomit že třída `DefaultTimeValueConverter` implementuje `AttributeConverter`, což vlastní znamená, že tento konvertor funguje pouze v případě, když se jedná o sloupec v entitě, který reprezentuje sloupec databáze. Pokud vámi vybraný sloupec toto nesplňuje, musíte nastavit jeho hodnotu pomocí jedné ze statických funkcí, které jsou obsaženy ve třídě `DefaultTimeValueConverter`.

```java
    @Column(name = "reservation_time_to")
    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "basic"
    )
    @Convert(converter = DefaultTimeValueConverter.class)
    private Date reservationTimeTo;
```

## SELECT

Výběrové pole, nastavuje se typem `DataTableColumnType.SELECT`.

![](../../frontend/webpages/customfields/webpages-select.png)

Důležité je definovat možnosti výběrového pole:

### Přenosem údajů z REST služby

Při čtení datatabulky je možné kromě záznamů přenést také tzv. datatabulky. `option` atributy. Ty se následně nastaví i jako možnosti zadaných polí. Možnosti lze nastavit přepsáním metody `getOptions`.

```java
public class ContactRestController extends DatatableRestControllerV2<ContactEntity, Long> {

    @Override
    public void getOptions(DatatablePageImpl<ContactEntity> page) {

        //pridaj zoznam pre pole country
        page.addOptions("country", getCountries(), "label", "value", false);
    }

    /**
     * Vrati zoznam vyberoveho pola pre krajinu
     * @return
     */
    public static List<LabelValue> getCountries() {
        //vytvor zoznam krajin, toto by sa idealne malo citat z nejakeho ciselnika
        List<LabelValue> countries = new ArrayList<>();
        countries.add(new LabelValue("Slovenská republika", "sk"));
        countries.add(new LabelValue("Česká republika", "cz"));
        countries.add(new LabelValue("Rakúsko", "at"));

        return countries;
    }
}


public class ContactEntity {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title="components.contact.property.country")
    private String country;

}
```

Upozornění: pokud byste přepsali i metodu `getAll(Pageable pageable)` je třeba volání `getOptions` provést ve vašem kódu metody `getAll`.

### Propojením na číselník

V aplikaci číselníky lze snadno vytvářet uživatelsky upravitelné seznamy údajů. Ty lze využít jako možnosti výběrového pole. To se nastavuje hodnotou `enumeration:MENO-CISELNIKA`:

```java
public class ContactEntity {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", editor = {
        @DataTableColumnEditor(
            options = {
                //ukazka napojenia na ciselnik, mozne je zadat meno alebo ID ciselnika, vo value su mena property pre text a hodnotu option pola
                @DataTableColumnEditorAttr(key = "enumeration:Okresne Mestá", value = "string1:string2")
            }
        )
    })
    private String country;

}
```

### Propojením na Java třídu

Pokud jsou data získávána speciálním způsobem víte získat seznam možností voláním Java třídy, která vrátí seznam údajů. To se nastavuje hodnotou `method:PACKAGE.TRIEDA.METODA`, přičemž do `value` je možné zadat jména atributů pro získání textu a hodnoty ve formátu `LABEL-PROPERTY:VALUE-PROPERTY`.

```java
public class ContactRestController extends DatatableRestControllerV2<ContactEntity, Long> {
    /**
     * Vrati zoznam vyberoveho pola pre krajinu
     * @return
     */
    public static List<LabelValue> getCountries() {
        //vytvor zoznam krajin, toto by sa idealne malo citat z nejakeho ciselnika
        List<LabelValue> countries = new ArrayList<>();
        countries.add(new LabelValue("Slovenská republika", "sk"));
        countries.add(new LabelValue("Česká republika", "cz"));
        countries.add(new LabelValue("Rakúsko", "at"));

        return countries;
    }
}

public class ContactEntity {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", editor = {
        @DataTableColumnEditor(
            options = {
                //ukazka ziskania zoznamu krajin volanim statickej metody, vo value su mena property pre text a hodnotu option pola
                @DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value")
            }
        )
    })
    private String country;

}
```

### Jmenováním možností přímo v entitě

Jednotlivé možnosti můžete přímo jmenovat pomocí seznamu `@DataTableColumnEditorAttr`, jako zobrazený text (atribut `key`) můžete použít překladový klíč:

```java
	@Column(name = "banner_type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="[[#{components.banner.banner_type}]]",
		tab = "main",
		editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "components.banner.picture", value = "1"),
					@DataTableColumnEditorAttr(key = "components.banner.html", value = "3"),
					@DataTableColumnEditorAttr(key = "components.banner.content_banner", value = "4")
				}
			)
		}
    )
	private Integer bannerType;
```

Všimněte si, že i když hodnoty do value zadáváte jako řetězec korektně se přetypují na číslo (technicky v HTML kódu je všechno řetězec, ale při odeslání i zobrazení to Spring korektně konvertuje).

## MULTISELECT

Výběrové pole s možností volby více možností, typ `DataTableColumnType.MULTISELECT`. Umožňuje pracovat s polem objektů nebo s čárkou odděleným seznamem (po nastavení atributu `separator` ve kterém uvedete oddělovací znak).

![](../../frontend/webpages/customfields/webpages-select-multi.png)

Příklady použití:

```java
public class ContactEntity {

    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title="multiple", filter = true, editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value")
            }
        )
    })
    private String[] multiple;

    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title="multipleInt", filter = true, editor = {
        @DataTableColumnEditor(
            options = {
                    @DataTableColumnEditorAttr(key = "1-Jedna", value = "1"),
                    @DataTableColumnEditorAttr(key = "2-Dva", value = "2"),
                    @DataTableColumnEditorAttr(key = "3-Tri", value = "3"),
                    @DataTableColumnEditorAttr(key = "4-Styri", value = "4"),
                    @DataTableColumnEditorAttr(key = "5-Pat", value = "5"),
            }
        )
    })
    private Integer[] multipleInt;


    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title="multipleOddeleneCiarkou", filter = true, editor = {
        @DataTableColumnEditor(
            options = {
                    @DataTableColumnEditorAttr(key = "1-Jedna", value = "Jedna"),
                    @DataTableColumnEditorAttr(key = "2-Dva", value = "Dva"),
                    @DataTableColumnEditorAttr(key = "3-Tri", value = "Tri"),
                    @DataTableColumnEditorAttr(key = "4-Styri", value = "Styri"),
                    @DataTableColumnEditorAttr(key = "5-Pat", value = "Pat"),
            },
            separator = ","
        )
    })
    private String multipleOddeleneCiarkou;

}
```

Pro atribut `multipleOddeleneCiarkou` se hodnoty oddělují pomocí znaku definovaného v atributu `separator`. Hodnota po odeslání tedy bude vypadat jako např. `"Tri,Pat"`.

Hodnoty oddělené čárkou je třeba použít pro `MULTISELECT` pole použitá pro nastavení aplikace (používající anotaci `@WebjetAppStore`):

```java
@WebjetComponent("sk.iway.basecms.contact.ContactApp")
@WebjetAppStore(nameKey = "Kontakty", descKey = "Ukazkova aplikacia so zoznamom kontaktov", imagePath = "ti ti-id", galleryImages = "/components/map/screenshot-1.jpg,/components/gdpr/screenshot-2.png,/components/gallery/screenshot-3.jpg")
@Getter
@Setter
public class ContactApp extends WebjetComponentAbstract {

    /**
     * Privatne vlastnosti s get/set-rami slúžia na prenesenie parametrov pageParams z !INCLUDE()! do triedy
     * Pomocou anotacie @DataTableColumn vytvarame pole pre nastavenie aplikacie
     */
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value")
            },
            separator = ","
        )
    })
    private String countries;
}
```

## CHECKBOX

Standardní zaškrtávací pole, umožňuje pracovat is polem objektů (zvoleno více možností). Pomocí atributu `unselectedValue` se nastavuje hodnota pokud pole není zaškrtnuté.

Můžete využít i možnosti nastavení atributu `data-dt-field-headline` pro samostatný nadpis nad seznamem polí.

```java
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "editor.perex.group",
        sortAfter = "publishType",
        tab="filter",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
                }
            )
        }
    )
	private String[] perexGroupsIds;


    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "user.permissions.label", tab = "groupsTab", visible = false, editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.admin.editUserGroups"),
            @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] permisions;
```

## RADIO

Standardní pole pro výběr jedné z možnosti.

```java
    @Column(name = "sex_male")
    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.user.newuser.sexMale",
        tab = "personalInfo",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "reguser.male", value = "true"),
                    @DataTableColumnEditorAttr(key = "reguser.female", value = "false")
                }
            )
        }
    )
    private Boolean sexMale;
```

## BOOLEAN

Reprezentuje zjednodušený zápis pole typu `CHECKBOX` pro binární volbu ano/ne.

```java
    @Column(name = "require_approve")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "groupedit.require_approve",
        tab = "basic"
    )
    private Boolean requireApprove;
```

## BOOLEAN\_TEXT

Reprezentuje typ `BOOLEAN` pro binární volbu ano/ne s titulkem napravo místo nalevo a možnosti Ano při zaškrtávacím poli.

```java
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title="components.news.paging")
    private boolean pagination = true;
```

## HIDDEN

Skryté pole, v editoru nebude zobrazeno.

```java
	@Column(name = "domain_id")
	@DataTableColumn(
        inputType = DataTableColumnType.HIDDEN,
		tab="basic"
    )
	private Integer domainId;
```

## DISABLED

Zobrazí textové pole, jehož hodnotu nelze měnit. V příkladu si všimněte, že typy polí lze kombinovat. Nastavení `DISABLED` nastaví HTML atribut `disabled="disabled"`.

```java
    @Size(max = 255)
    @Column(name = "image_name")
    @DataTableColumn(inputType = {DataTableColumnType.OPEN_EDITOR, DataTableColumnType.DISABLED}, tab = "metadata", title="components.gallery.fileName")
    private String imageName;
```

## QUILL

Zobrazí jednoduchý HTML editor, který umožňuje základní formátování textu jako tučné písmo/kurzíva/podtržené, nadpisy, seznamy a odkaz.

Všimněte si použití konvertoru `@javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)`, který povolí odeslat pouze [bezpečný HTML kód](../backend/security.md) (bez vložených JavaScript elementů a podobně).

```java
    @Column(name = "l_description_sk")
    @DataTableColumn(
            inputType = DataTableColumnType.QUILL,
            title = "[[#{gallery.l_description}]] <span class='lang-shortcut'>sk</span>",
            tab = "description"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String descriptionLongSk;
```

![](../../redactor/apps/tooltip/tooltip-editor.png)

## WYSIWYG

Zobrazí plnohodnotný editor HTML jak se používá pro editaci webových stránek.

```java
    @Column(name = "description")
    @DataTableColumn(
        inputType = DataTableColumnType.WYSIWYG,
        title = "calendar_edit.description",
        tab = "description",
        hidden = true
    )
    private String description;
```

pole musí být použito v samostatné kartě, která má nastavený atribut `content: ''`:

```javascript
    var tabs = [
        { id: 'basic', title: /*[[#{calendar_events.tab.basic}]]*/ 'basic', selected: true},
        { id: 'description', title: /*[[#{calendar_events.tab.description}]]*/ 'desc', content: '' },
        { id: 'advanced', title: /*[[#{calendar_events.tab.advanced}]]*/ 'adv' },
        { id: 'notification', title: /*[[#{calendar_events.tab.notification}]]*/ 'notify' }
    ];
```

## JSON

Pole [přidávání adresářů a web stránek](field-json.md).

```java
    @DataTableColumn(inputType = DataTableColumnType.JSON, title="admin.temp.edit.showForDir", className = "dt-tree-group-array")
    private List<GroupDetails> availableGroups;
```

![](field-json-group-array.png)

## DATATABLE

Pole [vnořená datatabulka v editoru](field-datatable.md).

```java
    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "folders",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/groups?userGroupId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.doc.GroupDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "groupId,groupName,fullPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.groups_title"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    private List<GroupDetails> groupDetailsList;
```

## ELFINDER

Pole pro [výběr souboru](field-elfinder.md) (odkazu na soubor).

```java
    @Column(name = "watermark")
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title = "components.gallery.watermark",
        tab = "watermark"
    )
    private String watermark;
```

![](../../frontend/webpages/customfields/webpages-image.png)

## JSTREE

Pole pro výběr [stromové struktury](field-jstree.md).

```java
    @DataTableColumn(inputType = DataTableColumnType.JSTREE, title = "components.user.righrs.user_group_rights", tab = "rightsTab", hidden = true,
    editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.user.permissions"),
                @DataTableColumnEditorAttr(key = "data-dt-field-jstree-name", value = "jstreePerms") }) })
    private String[] enabledItems;

    @DataTableColumn(inputType = DataTableColumnType.JSTREE, title = "user.permgroups.permissions.title", tab = "perms", hidden = true, editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-jstree-name", value = "jstreePerms") }) })
    private String[] permissions;
```

![](field-type-jstree.png)

## COLOR

Pole pro výběr barvy v `HEX` formátu včetně průsvitnosti, například. `#FF0000FF`:

```java
    @DataTableColumn(inputType = DataTableColumnType.COLOR, tab = "basic", title="components.app-cookiebar.textColor")
    private String color_text;
```

## IFRAME

Pole pro vložení jiné stránky do `iframe` elementu, používá se v aplikacích v editoru pro vložení např. foto galerie:

```java
@WebjetComponent("sk.iway.iwcm.components.gallery.GalleryApp")
@WebjetAppStore(nameKey = "components.gallery.title", descKey = "components.gallery.desc", itemKey="menuGallery", imagePath = "/components/gallery/editoricon.png", galleryImages = "/components/gallery/", componentPath = "/components/gallery/gallery.jsp")
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "componentIframe", title = "components.gallery.images")
})
@Getter
@Setter
public class GalleryApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframe", title="&nbsp;")
    private String iframe  = "/admin/v9/apps/gallery/?dir={dir}";

}
```

### BASE64

Pole, které kóduje a dekóduje hodnotu pomocí algoritmu `base64`, zobrazeno jako `textarea`. Používá se primárně jako pole pro aplikaci v editoru pro zachování speciálních znaků vložené hodnoty. Pokud potřebujete použít `base64` i na jiný typ pole můžete nastavit `className = "dt-style-base64"`.

!>**Upozornění:** JavaScript funkce `btoa` podporuje jen `ASCII` znaky.

```java
    @DataTableColumn(
        inputType = DataTableColumnType.BASE64,
        tab = "basic",
        title="components.app-docsembed.editor_components.url",
    )
    private String url;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        tab = "basic",
        title="components.app-docsembed.editor_components.url",
        className = "dt-style-base64"
    )
    private String url;
```

### STATIC\_TEXT

Zobrazení statického textu na pozici běžného vstupního pole, tedy v pravé části. V překladovém klíči je podporována markdown syntax.

```java
    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.app-vyhladavanie_info"
    )
    private String explain;
```

### IMAGE\_RADIO

Zobrazení výběru jedné z možností na základě obrázku. Používá se například v aplikaci Anketa. Obrázky se získají jako seznam ze souborového systému, je třeba je plnit do objektu `options` odpovědi REST služby. Odkaz na obrázek se zadává do objektu `OptionDto.original`.

Nastavením `className = "image-radio-horizontal"` lze přepnout zobrazení možností na horizontální v řádku. Přidáním CSS třídy `image-radio-fullwidth` se přepne zobrazení popisku a výběru do řádku namísto do sloupce, v takovém případě doporučujeme v kartě nemít jiná pole a nastavit v definici karty hodnotu `content = ""` aby karta neměla v pozadí šedý pruh podbarvující popisky.

Implementace je v souboru [field-type-imageradio.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-imageradio.js).

```java
@WebjetComponent("sk.iway.iwcm.components.inquiry.InquiryApp")
@WebjetAppStore(
    nameKey = "components.inquiry.title",
    descKey = "components.inquiry.desc",
    itemKey= "cmp_inquiry",
    imagePath = "/components/inquiry/editoricon.png",
    galleryImages = "/components/inquiry/",
    componentPath = "/components/inquiry/inquiry.jsp"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "styleSelectArea", title = "components.roots.new.style", content = ""),
    @DataTableTab(id = "componentIframeWindowTabList", title = "menu.inquiry", content = ""),
})
@Getter
@Setter
public class InquiryApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.IMAGE_RADIO,
        title = "components.roots.new.style",
        tab = "styleSelectArea",
        className = "image-radio-horizontal image-radio-fullwidth"
    )
    private String style = "01";

    @DataTableColumn(
        inputType = DataTableColumnType.IMAGE_RADIO,
        title = "components.catalog.color",
        tab = "styleSelectArea",
        className = "image-radio-horizontal image-radio-fullwidth"
    )
    private String color = "01";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        //style & color options
        options.put("style", DatatableTools.getImageRadioOptions("/components/inquiry/admin-styles/"));
        options.put("color", DatatableTools.getImageRadioOptions("/components/inquiry/admin-colors/"));

        return options;
    }

}
```

## UPLOAD

Typ pole umožňující [nahrání souboru](field-file-upload.md).

![](field-uploadFile.png)

```java
  @DataTableColumn(
        inputType = DataTableColumnType.UPLOAD,
        tab = "basic",
        title = "fbrowse.file"
    )
    private String file = "";
```
