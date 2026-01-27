# Datatables columns

JSON pole "columns" pre DataTables je možné vygenerovať anotáciami nad vlastnosťami Java objektu. JSON sa do pug súboru vloží pomocou volania:

```javascript
let columns = [(${layout.getDataTableColumns('sk.iway.iwcm.components.gallery.GalleryEntity')})];
```

Kde ```sk.iway.iwcm.components.gallery.GalleryEntity``` je objekt manažovaní cez DataTables.

Do JSON sa mapujú všetky polia objektu, ktoré majú anotáciu ```@DataTableColumn```.

Anotácia ```@DataTableColumn``` má rovnaké vlastnosti ako [pôvodný columns objekt](../datatables/README.md). Ďalej je možné nastaviť určité vlastnosti pomocou skratky inputType, ktoré sú definované ```enum dataTableColumnType```.

Vlastnosti pre editor sa nastavujú pomocou anotácie `DataTableColumnEditorAttr`. Hodnotu je možné zadať priamo, alebo pomocou prefixu `constant:` získať z konfiguračnej premennej:

```java
    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "basic", className = "dt-tree-dir-simple", title="components.file_archiv.target_directory", hidden = true, editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-root", value = "constant:fileArchivDefaultDirPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-skipFolders", value = "fileArchivInsertLaterDirPath"),
            }
        )
    })
    private String dir;
```

## Príklady

```java
//TranslationKeyDto
@DataTableColumn(inputType = DataTableColumnType.ID)
private Integer id;

@DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR)
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

Príklad vlastnej [render](https://datatables.net/reference/option/columns.render) funkcie:

```javascript
//@Column(name = "step_name")
//@DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.banner.primaryHeader", renderFunction = "renderStepName")
//private String stepName;

window.renderStepName = function(data, type, row, meta) {
        if(type === "display" || type === "filter") {
                //combine row number with prefix Step X and stepName (if not empty) and stepSubName (if not empty)
                let displayName = `<span class="text-muted small">[[#{components.form_items.step_title}]] ${meta.row + 1}</span>`;
                let secondRow = "";
                if(row.stepName && row.stepName.trim() !== "") {
                        secondRow += `${row.stepName}`;
                }
                if (row.stepSubName && row.stepSubName.trim() !== "") {
                        secondRow += ` (${row.stepSubName})`;
                }
                return displayName + (secondRow ? `<br/>${secondRow}` : "");
        }
        return data;
};
```

## Vlastnosti @DataTableColumn

Pôvodná dokumentácia na stránke [datatables.net](https://datatables.net/reference/option/columns:)

Povinné polia:

- ```inputType``` - skratka, ```enum DataTableColumnType``` - určuje typ dátového poľ. Možné je použiť aj spojenú hodnotu (napr.: ```inputType = { DataTableColumnType.OPEN_EDITOR, DataTableColumnType.JSON },```).
- ```title``` - ak nie je zadaný automaticky sa vygeneruje ako ```components.meno_beanu_bez_dto_alebo_bean_na_konci.name``` - https://datatables.net/reference/option/columns.title, viď dokumentácia k [prekladom](#preklady-názvov-stĺpcov). **Upozornenie:** ak je title prázdny (alebo tvrdá medzera) tak sa automaticky stĺpcu nastaví atribút ```hidden=true```.

Voliteľné polia:

- ```tab``` - skratka pre nastavenie ```{editor: {tab: String}}```
- ```className``` - doplnkový CSS štýl nastavený na ```TD``` elemente, ak chcete nahradiť CSS štýl nastavený pomocou ```inputType``` zadajte ho so znakom ```!``` na začiatku - https://datatables.net/reference/option/columns.className. Existujú špeciálne CSS triedy:
  - `disabled` - nastaví pole na šedú farbu, čo evokuje, že je ne-editovateľné.
  - `DTE_Field_Has_Checkbox` - nastaví odsadenie od spodu na hodnotu `-14px` aby nezostala medzera pred ďalším polom.
  - `hide-on-create` - schová pole pri vytváraní nového záznamu.
  - `hide-on-edit` - schová pole pri editácii záznamu.
  - `hide-on-duplicate` - schová pole pri duplikovaní záznamu.
  - `not-export` - pole sa nebude exportovať.
  - `show-html` - zobrazí sa HTML kód v hodnote vrátane entít typu `&#39;`, stĺpcu nastaví atribút `entityDecode: false`.
  - `wrap` - zapne zalamovanie textu, používa sa primárne v poliach typu `textarea`.
  - `multiweb-noteditable` - v MultiWeb inštalácii je pole zobrazené šedou farbou, čo evokuje, že je ne-editovateľné.
- ```name``` - https://datatables.net/reference/option/columns.name
- ```data``` - https://datatables.net/reference/option/columns.data
- ```defaultContent``` - https://datatables.net/reference/option/columns.defaultContent
- `orderable` - `true/false` hodnota pre zapnutie možnosti usporiadania v datatabuľke
- ```renderFormat``` - https://datatables.net/reference/option/columns.renderFormat
- ```renderFormatLinkTemplate``` - https://datatables.net/reference/option/columns.renderFormatLinkTemplate
- ```renderFormatPrefix``` - https://datatables.net/reference/option/columns.renderFormatPrefix
- `renderFunction` - meno funkcie v JavaScripte, ktorá sa použije pre vlastné vykreslenie hodnoty stĺpca. Viac na [stránke DataTables](https://datatables.net/reference/option/columns.render).
- `sortAfter` - meno poľa za ktoré sa pridá toto pole v poradí
- ```editor``` - objekt ```DataTableColumnEditor```
- ```hidden``` - pole sa nezobrazí v datatabuľke a používateľ si ho na rozdiel od ```visible``` nemôže zobraziť, pole môže byť použité v editore
- ```hiddenEditor``` - ak je ```true``` nezobrazí sa stĺpec v editore
- ```visible``` - pole sa schová, ale používateľ si ho môže zobraziť https://datatables.net/reference/option/columns.visible
- ```filter``` - ak je false, nezobrazí sa v hlavičke tabuľky filter
- ```perms``` - hodnota pre kontrolu práv (napr. multiDomain), pole sa nezobrazí, pokiaľ prihlásený používateľ nemá uvedené právo povolené
- ```defaultValue``` - predvolená hodnota pre nový záznam (použije sa iba ak je nastavené ```fetchOnCreate``` na ```false```, pretože pre tento prípad sa vrátia dáta prednastavené zo servera). Môže obsahovať makrá:
  - ```{currentDomain}``` - nahradí sa za aktuálne zvolenú doménu
  - ```{currentDate}``` - nahradí sa za aktuálny dátum
  - ```{currentDateTimeSeconds}``` - nahradí sa za aktuálny dátum a čas vrátane sekúnd
  - ```{currentTime}``` - nahradí sa za aktuálny čas
- `alwaysCopyProperties` - pri editácii záznamu sa prázdne `null` hodnoty zachovajú a skopírujú z existujúceho objektu v databáze. Pre polia typu dátum/čas to neplatí, tie sa prepíšu automaticky. Ak potrebujete toto použiť aj pre iný typ poľa a preniesť aj `null` hodnotu nastavte atribút na `true`, prípadne na `false` ak nechcete automatický prepis pre dátumové polia.
- `ai` - nastavením na hodnotu `false` je možné vypnúť zobrazenie AI ikony pre všeobecné možnosti (preložiť, opraviť gramatiku...). AI ikona sa zobrazí len ak je asistent nastavený pre toto konkrétne pole.
- `disabled` - nastavením na `false` sa vstupnému poľu v editore nastaví atribút `disabled="disabled"`.

## Vlastnosti @DataTableColumnEditor

- ```type``` - typ ```input``` elementu
- ```label``` - prekladový kľúč názvu poľa v editore (ak sa odlišuje od ```DatatableColumn.title```)
- ```message``` - prekladový kľúč pre zobrazenie tooltipu, ak nie je zadané automaticky sa hľadá preklad pre kľúč podľa ```DatatableColumn.title.tooltip```. Podporuje zadanie formátovania pomocou základného [Markdown](../frameworks/webjetjs.md#markdown-parser).
- ```tab``` - [karta, v ktorom sa pole nachádza](README.md#karty-v-editore)
- ```attr``` - ```HashMap``` HTML atribútov, ktoré sa nastavia vstupnému poľu
  - ```data-dt-field-hr``` (```before/after```) - pridá rozdeľovaciu čiaru pred alebo za element
  - ```data-dt-field-headline``` (prekladový kľúč) - pridá pred element nadpis
  - ```data-dt-field-full-headline``` (prekladový kľúč) - pridá pred element nadpis na celú šírku (vrátane šedej plochy s názvami elementov), používa sa pre nadpis pred vnorenou datatabuľkou v samostatnej karte
- ```multiple``` - nastavuje atribút ```multiple``` na HTML poli (používa sa pre pole typu ```MULTISELECT```).
- ```separator``` - nastavuje oddeľovací znak pre pole typu ```MULTISELECT```. Ak je prázdne dáta sa posielajú a prijímajú ako pole, ak je nastavené tak ako reťazec oddelený uvedeným znakom (typicky čiarka).
- `data-dt-escape-slash` - nastavením na hodnotu `true` zapnete náhradu znaku `/` za entitu `&#47;`. Používa sa v prípade web stránky a priečinka, kde je potrebné v názve znak `/` nahradiť, keďže sa používa na oddelenie cesty.

Prekladový kľúč pre tooltip sa automaticky hľadá podľa prekladového kľúča ```title``` so suffixom ```.tooltip```. Ak teda máte anotáciu ```@DataTableColumn(title = "group.superior_directory"``` automaticky sa hľadá prekladový text s kľúčom ```group.superior_directory.tooltip```. Ak existuje, použije sa.

## Vlastnosti DataTableColumnType

Nastavuje typ poľa, viac v zozname [štandardných formulárových polí](standard-fields.md).

- ```ID``` - stĺpec s primárnym kľúčom
- ```OPEN_EDITOR``` - automaticky na stĺpci vytvorí odkaz na otvorenie editora, malo by sa použiť na hlavné textové pole, ideálne prvé v poradí
- ```TEXT``` - štandardné textové pole (jeden riadok)
- ```TEXTAREA``` - štandardné pole pre zadanie viacerých riadkov textu
- ```SELECT``` - výberové pole, možnosti odporúčame odosielať cez [REST službu](../datatables/restcontroller.md#číselníky-pre-select-boxy)
- ```MULTISELECT``` - výberové pole pre výber viacerých možností
- ```BOOLEAN``` - zaškrtávacie pole s možnosťami ```true/false```
- ```CHECKBOX``` - zaškrtávacie pole so špeciálnou hodnotou, možnosť pre vybranú aj nevybranú hodnotu je možné nastaviť atribútom editora ```@DataTableColumnEditorAttr(key = "unselectedValue", value = "")```
- ```DISABLED``` - zobrazené pole nebude editovateľné

Číselné:

- ```NUMBER``` - číselné pole
- ```TEXT_NUMBER``` - zobrazí zaokrúhlené číslo, pri vyššom čísle vypíše v textovej podobe, napr. ```10 tis.``` namiesto ```10000```
- ```TEXT_NUMBER_INVISIBLE``` - číselné pole, ktoré sa nezobrazí v tabuľke ani v editore

Dátumové:

- ```DATE``` - pole pre zadanie dátumu, po kliknutí do poľa zobrazí kalendárový výber
- ```DATETIME``` - pole pre zadanie dátumu a času, po kliknutí do poľa zobrazí kalendárový výber s možnosťou zadania časového údaju
- ```TIME_HM``` - pole pre zadanie výhradne času, po kliknutí do poľa sa zobrazí výber s možnosťou zvolenia hodín a minút
- ```TIME_HMS``` - pole pre zadanie výhradne času, po kliknutí do poľa sa zobrazí výber s možnosťou zvolenia hodín, minút a sekúnd

Špeciálne:

- ```GALLERY_IMAGE``` - špeciálny typ poľa pre zobrazenie obrázka s vypnutým titulkom stĺpca
- ```QUILL``` - jednoduchý editor so základným formátovaním HTML kódu ako tučné písmo, kurzíva a podobne
- ```WYSIWYG``` - plnohodnotný editor HTML kódu pre web stránku
- ```JSON``` - pole pre výber [adresára](field-json.md)
- ```DATATABLE``` - [vnorená datatabuľka](field-datatable.md)
- ```ELFINDER``` - [výber odkazu](field-elfinder.md) na súbor / web stránku
- ```UPLOAD``` - [nahratie súboru](field-file-upload.md)

## Možnosti výberového poľa

Poľu typu ```DataTableColumnType.SELECT``` môžete nastavovať ```option``` hodnoty cez:

- [REST službu](../datatables/restcontroller.md#Číselníky-pre-select-boxy) a nastavovanie číselníkov pre select boxy. Toto je preferované riešenie pre štandardné datatabuľky.
- Nastavením options atribútov priamo pomocou anotácie ```@DataTableColumnEditorAttr(key = "Slovensky", value = "sk")```.
- Volaním statickej metódy pomocou anotácie ```@DataTableColumnEditorAttr(key = "method:sk.iway.basecms.contact.ContactRestController.getCountries", value = "label:value")```. V ```key``` atribúte je zadaná prefixom ```method:``` triede a metóda,ktorá musí vrátiť ```List``` objektov. V atribúte ```value = "label:value"``` anotácie je zadané meno atribútu pre opis a meno atribútu pre hodnotu výberového poľa (v príklade sa teda volá ```objekt.getLabel() a objekt.getValue()```).
- Pridaním anotácie `@DataTableColumnEditor.optionMethods` so zadanou metódou na vykonanie (jedná sa o krajší zápis oproti predchádzajúcemu spôsobu). Hodnota labelProperty a valueProperty sa použije na získanie textu a hodnoty option prvku, ak nie je zadaná získa sa z `label` a `value` atribútu.
- Napojením na aplikáciu číselníky zadaním ```@DataTableColumnEditorAttr(key = "enumeration:Okresne Mestá", value = "string1:string2")```. V ```key``` atribúte je zadaný prefix ```enumeration:``` meno alebo ID číselníka. V atribúte ```value = "string1:string2"``` anotácie je zadané meno atribútu pre opis a meno atribútu pre hodnotu výberového poľa - v príklade sa teda volá ```objekt.getString1() a objekt.getString2()```.

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
        @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.formsimple.fieldType", editor = {
        @DataTableColumnEditor(
                optionMethods = {
                        @DataTableOptionMethod(
                                className = "sk.iway.iwcm.components.formsimple.FormSimpleApp",
                                methodName = "getFieldTypes",
                                labelProperty = "label",
                                valueProperty = "value"
                        )
                })
        })
        private String fieldType;
})
private String country;
```

## Povinné polia

Povinné polia je možné označiť anotáciami:

- ```@NotEmpty``` - neprázdne pole, neumožní zadať medzeru, alebo tabulátor
- ```@NotBlank``` - neprázdne pole, umožní ale zadať medzeru

Ďalšie možnosti validácie sú opísané v dokumentácii k [restcontrolleru](../datatables/restcontroller.md#validácia--povinné-polia).

## Validácie

Podporované sú štandardné anotácie pre validácie polí pomocou [javax.validation.Validator](https://www.baeldung.com/javax-validation). Príklad validácie dĺžky poľa:

```java
        @Size(min = 10, max = 20, message = "form.p2.size")
        private String p2;
```

prekladový kľúč pre zobrazenie konkrétnej nastavenej hodnoty pre min aj max:

```properties
form.p2.size=Pole P2 musí byť medzi {min} a {max} znakmi
```

## JS Helper pre rozšírenie vlastností

Súbor ```app-init.js``` obsahuje funkciu ```WJ.DataTable.mergeColumns``` pre doplnenie vlastností, na základne názvu objektu (pole name).
Preiteruje pole objektov columns, nájde objekt s rovnakým name ako argument objekt a pomocou ```jQuery.extend``` tento objekt rozšíri.

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

Ak potrebujete len doplniť nový stĺpec môžete to spraviť jednoducho pridaním do zoznamu. V príklade sa jedná o pridanie *skrytého* poľa (atribút ```visible: false```):

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

Ak potrebujete zmeniť poradie stĺpcov, môžete to spraviť pomocou funkcie ```WJ.DataTable.moveColumn```. V príklade sa stĺpec s názvom ```formSettings.recipients``` presunie za stĺpec s názvom ```formName```:

```javascript
filteredColumns = window.WJ.DataTable.moveColumn(filteredColumns, "formSettings.recipients", "formName");
```

## Vnorené atribúty

Často je potrebné k entite pridať pre editor doplnkové atribúty (napr. ```checkbox``` pre aplikovanie zmeny aj na podradené entity, doplnkové pole s informáciou atď). Pre tento účel je možné entitu rozšíriť o nový atribút (ktorý sa neukladá do databázy) obsahujúci doplnkové údaje. Typicky ho voláme ```editorFields``` a pre entitu implementujeme potrebnú triedu. Príklady sú v [DocEditorFields](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) alebo [GroupEditorFields](../../../../src/main/java/sk/iway/iwcm/doc/GroupEditorField.java). V triedach je následne len editorField atribút, napr. ```private DocEditorFields editorFields = null;```.

Implementovaná trieda ```EditorFields```, napr. [DocEditorFields](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) typicky obsahuje metódy ```fromDocDetails``` pre nastavenie atribútov v ```editorFields``` triede pred editáciou a ```toDocDetails``` pre spätné nastavenie atribútov v ```DocDetails``` pred uložení. Tieto metódy je potrebné implicitne volať vo vašom Java kóde.

!>**Upozornenie:** ak je entita ukladaná v cache (ako napr. [GroupDetails](../../../../src/main/java/sk/iway/iwcm/doc/GroupDetails.java)) nastavenie atribútu ```editorFields``` zostane aj v cache a môže zbytočne zaberať pamäť a vytvárať pri JSON serializácii zbytočne veľké dáta. V ```GroupDetails``` v editorFields odkazuje na ```parentGroupDetails```.

Pri štandardnom postupe sa postupne na každom ```GroupDetails``` objekte nastavil ```editorFields``` objekt. Pri serializácii hlboko vnoreného adresára sa následne vnárali objekty editorFields.parentGroupDetails.editorFields.parentGroupDetails atď. Objekt GroupDetails nemal len potrebný prvý editorFields. Riešením je najskôr objekt ```GroupDetails``` naklonovať a až tak do neho nastavit ```editorFields```. Príklad je v ```GroupEditorField.fromGroupDetails``` ktorý naklonuje objekt a následne ho vráti. Použitie v kóde je potom ako ```group = gef.fromGroupDetails(group);```.

Spoločné metódy pre datatabuľku sú v triede [BaseEditorFields](../../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java), ktorú môže vaša trieda rozšíriť. Obsahuje metódy pre pridanie CSS triedy riadku a pridanie ikony k titulku. Viac v dokumentácii k [štýlovaniu datatabuľky](../datatables/README.md#štýlovanie-riadku).

Pre vloženie anotácie vnorených atribútov je možné použiť anotáciu ```@DatatableColumnNested``` ako je napr. v [DocDetails](../../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) na atribúte ```editorFields```:

```java
@DataTableColumnNested
@Transient
private DocEditorFields editorFields = null;
```

takto anotovaný atribút bude prehľadaný na anotáciu ```@DatatableColumn``` rekurzívne. Výsledkom bude vygenerovanie vnorenej anotácie. Všimnite si data a name atribút obsahujúci prefix ```editorFields.```. Ak potrebujete nastaviť vlastný prefix (meno premennej) môžete použiť parameter prefix ```@DataTableColumnNested(prefix = "menoPremennej")```. Prefix môžete nastaviť aj na prázdnu hodnotu, vtedy sa prefix nevygeneruje.

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

Anotácia ```@Transient``` hovorí JPA entitám, že daný atribút nie je ukladaný do databázy.

Pre nastavenie údajov medzi entitou a ```editorFields``` v REST controlleri je možné implementovať metódy ```processFromEntity``` pre nastavenie ```editorFields``` atribútov alebo ```processToEntity``` pre nastavenie atribútov v entite z ```editorFields```. Príklad je vidno v [UserDetailsController](../../../../src/main/java/sk/iway/iwcm/components/users/userdetail/UserDetailsController.java). Metódy sa automaticky volajú pri čítaní všetkých záznamov, pri získaní jedného záznamu, vyhľadávaní alebo pri ukladaní údajov.

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

## Sortovanie poradia polí

Polia sú predvolene usporiadané v poradí ako sú zapísané v zdrojovom kóde (aj keď špecifikácia anotácie to negarantuje, funguje to tak). Ak ale používate vnorené atribúty poradie neviete nastaviť poradím v kóde.

Preto je možné využiť atribút ```sortAfter``` do ktorého zadáte data atribút predchádzajúceho poľa. Anotované pole sa následne do JSON výstupu pridá za uvedené pole. Logika je implementovaná v metóde [DataTableColumnsFactory.sortColumns](../../../../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java).

V prípade potreby je možné zadať špeciálnu hodnotu `sortAfter = "FIRST"` pre presun poľa na začiatok zoznamu. Je potrebné to použiť v prípade rozšírených entít cez `@MappedSuperclass` ak aj prvý `id` atribút je v tejto entite.

Príklad použitia:

```java
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "Povolit zmenu URL", tab = "basic",
        sortAfter = "virtualPath"
    )
    private Boolean allowChangeUrl=false;
```

## Preklady názvov stĺpcov

Ako je uvedené vyššie, atribút ```title``` obsahuje meno stĺpca. Z dôvodu prekladov nemôžete použiť priamo slovenský názov stĺpca, je potrebné použiť prekladový kľúč.

Keď atribút ```title``` nezadáte, automaticky sa hľadá prekladový kľúč vo formáte ```components.meno_beanu_bez_dto_alebo_bean_na_konci.name```, čiže napr. ```components.monitoring.date_insert```.

**Ak upravujete existujúcu aplikáciu/komponentu** z WebJET 8 do WebJET 2021 je najlepšie vyhľadať pôvodné prekladové kľúče. Ušetrí sa tak čas pri prekladoch, keďže WeBJET 8 je už preložený do viacerých jazykov.

V súbore [src/main/webapp/files/text.properties](../../../../src/main/webapp/WEB-INF/classes/text.properties) je pôvodný prekladový súbor z WebJET 8 (len pre ukážku, v žiadnom prípade ho nemodifikujte). Môžete vyhľadať požadovaný text v ňom a do ```title``` atribútu zadať nájdený prekladový kľúč.

Inou možnosťou je zobraziť pôvodnú stránku s URL parametrom ```?showTextKeys=true``` čo spôsobí zobrazenie prekladových kľúčov pred textom. Stránka bude pravdepodobne rozbitá z dizajnového pohľadu (keďže texty budú príliš dlhé), ale cez inšpektor sa viete na kľúče pozrieť.

Napríklad:

```txt
http://iwcm.interway.sk/components/server_monitoring/admin_monitoring_all.jsp?showTextKeys=true
```

Ak už odošlete formulár na stránke, samozrejme sa vám zobrazia pôvodné texty, parameter pridáte ako ```&showTextKeys=true```, ale pravdepodobne sa vám zobrazí stránka 403/404 z dôvodu ochrany WebJETu. Riešenie je použiť JavaScript konzolu, kde zadáte:

```javascript
window.location.href=window.location.href+"&showTextKeys=true";
```

to korektne prejde ochranou WebJETu a kľúče sa vám zobrazia.

**Ak ste vytvorili novú aplikáciu, alebo ste nenašli vhodný prekladový kľúč** je potrebné ho pridať do súboru [text-webjet9.properties](../../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties).

Po pridaní prekladu je potrebné znova načítať súbor ```text-webjet9.properties``` WebJETom. To vykonáte volaním [úvodnej stránky s parametrom ?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true) alebo reštartom aplikačného servera.
