# Voliteľné polia

Cez voliteľné polia je možné web stránke a adresáru nastavovať voliteľné atribúty (hodnoty, texty) podľa potreby zákazníka. Hodnoty je následne možné preniesť a použiť v šablóne stránky, viď [dokumentácia pre Frontend programátora](../../frontend/webpages/customfields/README.md).

## Backend

Nastavenia voliteľných polí sú závislé od použitej šablóny, skupiny šablón alebo domény (keďže sú riadené nastavením prekladových kľúčov). Možnosti je teda z backend-u potrebné odosielať pre každú editovanú web stránku samostatne. Prenos nastavení je genericky implementovaný [BaseEditorFields.getFields](../../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java):

```java
@JsonIgnore
/**
 * Vygeneruje definiciu volnych poli, presunute sem z EditorForm.getFields() pre moznost pouzitia aj v inych DT ako webpages
 * @param bean - java bean, musi obsahovat metody getFieldX
 * @param keyPrefix - prefix textovych klucov, napr. edior, alebo groupedit, nasledne sa hladaju kluce keyPrefix.field_X a keyPrefix.field_X.type
 * @param lastAlphabet - koncove pismeno (urcuje pocet volnych poli), nap. T aleb D
 * @return
 */
public List<Field> getFields(Object bean, String keyPrefix, char lastAlphabet) {

}
```

volanie ```getFields``` ako vidíte má anotáciu ```@JsonIgnore```. Metódu musíte implicitne zavolať pre prípravu poľa objektov. Základný príklad použitia:

```java
//vytvorte triedu, ktora extenduje BaseEditorFields, nemusi obsahovat nic dalsie (ak nepotrebujete v editore dodatocne polia)
//technicky by ste triedu ani nemuseli vytvarat a pouzit priamo BaseEditorFields vo vasej QuestionsAnswersEntity
public class QuestionsAnswersEditorFields extends BaseEditorFields {

}

//vo vasej entite pridajte pole s nazvom editorFields
@Entity
@Table(name = "questions_answers")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_QA_UPDATE)
public class QuestionsAnswersEntity implements Serializable {

    ...

	@Transient
    @DataTableColumnNested
	private QuestionsAnswersEditorFields editorFields = null;
}

//v REST controlleri implementujte metodu processFromEntity v ktorej doplnite do editorFields definiciu poli
@RestController
@RequestMapping("/admin/rest/qa")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuQa')")
@Datatable
public class QuestionsAnswersRestController extends DatatableRestControllerV2<QuestionsAnswersEntity, Long> {

    ...

    @Override
    public QuestionsAnswersEntity processFromEntity(QuestionsAnswersEntity entity, ProcessItemAction action) {

        QuestionsAnswersEditorFields ef = new QuestionsAnswersEditorFields();
        //definovanie volnych poli A-D s prefixom textoveho kluca components.qa
        ef.setFieldsDefinition(ef.getFields(entity, "components.qa", 'D'));
        entity.setEditorFields(ef);

        return entity;
    }
}
```

Príklad v [DocEditorFields](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) kde je viac operácii a preto metóda ```fromDocDetails``` je implementovaná samostatne v ```editorFields``` objekte a volaná z ```DocRestController.processFromEntity```.

```java

//zoznam volnych poli
public List<Field> fieldsDefinition;

/**
 * Nastavi hodnoty atributov z DocDetails objektu
 * @param doc
 */
public void fromDocDetails(DocDetails doc, boolean loadSubQueries) {

    if (loadSubQueries) {
        //nastav prefix prekladovych klucov podla sablony a skupiny sablon
        if (doc.getTempId() > 0)
        {
            //nastavenie prefixu klucov podla skupiny sablon
            TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
            if (temp != null && temp.getTemplatesGroupId()!=null && temp.getTemplatesGroupId().longValue() > 0) {
                TemplatesGroupBean tgb = TemplatesGroupDB.getInstance().getById(temp.getTemplatesGroupId());
                if (tgb != null && Tools.isNotEmpty(tgb.getKeyPrefix())) {
                    RequestBean.addTextKeyPrefix(tgb.getKeyPrefix(), false);
                }
            }

            RequestBean.addTextKeyPrefix("temp-"+doc.getTempId(), false);
        }

        //ziskaj zoznam volitelnych poli
        fieldsDefinition = getFields(doc, "editor", 'T');
    }

}
```

v metóde ```fromDocDetails``` sú najskôr nastavené prefixy prekladových kľúčov pre vyhľadávanie podľa skupiny šablón aj podľa ID šablóny a následne je získaný zoznam ```fieldsDefinition``` (frontend implicitne tento zoznam hľadá v objekte ```editorFields.fieldsDefinition```).

Pre funkčnosť je potrebné aby daný bean obsahoval atribúty s názvom ```fieldX```, čo následne s volaním ```getFields``` vie priniesť voliteľné polia do ľubovoľného bean-u.

## Priorita zdrojov konfigurácie

Konfigurácia voliteľných polí sa skladá z dvoch zdrojov:

1. prekladové kľúče (`editor.field_x`, `editor.field_x.type`, ...),
2. záznamy z tabuľky `custom_fields`.

Pri generovaní `fieldsDefinition` v `BaseEditorFields.getFields` sa aplikuje priorita:

1. prekladový kľúč
2. globálne nastavenie triedy (bez `entityId`),
3. špecifické nastavenie pre konkrétnu entitu (`entityId`),
4. bonus kontext (`bonusClassName` + `bonusEntityId`, napr. `TemplateDetails` pre `DocDetails`).

Vyššia úroveň vždy prepíše nižšiu pre rovnaké písmeno poľa (`alphabet`).

## Serializácia nastavení v `custom_fields.value`

Nastavenia špecifické pre typ poľa sa ukladajú do stĺpca `custom_fields.value`.

Používané formáty:

- `text` / `text-120` / `text-120, warningLength-80`
- `jsoneditor` (priama editácia JSON objektu)
- `label1:value1|label2:value2` (`select`)
- `multiple:label1:value1|label2:value2` (`multiselect`)
- `autocomplete:Možnosť 1|Možnosť 2`
- `docsIn_67` alebo `docsIn_67_null`
- `enumeration_2_string1_string2` alebo `enumeration_2_string1_string2_null`
- `json_group`/`json_doc` s voliteľným suffixom `_null`

Transformáciu medzi editor poľami a internou hodnotou zabezpečujú metódy `CustomFieldsService.toEntity` a `CustomFieldsService.fromEntity`.

## Editor JSON

Typ `jsoneditor` umožňuje priamo zadávať JSON objekt. V [nastaveniach voliteľných polí](../../frontend/webpages/customfields/custom-fields-settings.md) vyberte typ **Editor JSON**, alebo použite prekladové kľúče:

```properties
editor.field_a=MHUB data
editor.field_a.type=jsoneditor
```

Pre inú entitu použite jej prefix prekladových kľúčov. Na serveri typ reprezentuje hodnota `FieldType.JSONEDITOR`, v `editorFields.fieldsDefinition` sa odosiela `type: "jsoneditor"`. Typy `JSON`, `json_doc` a `json_group`, ktoré sa používajú na výber existujúcich záznamov, majú naďalej pôvodný význam.

### Zadávanie a formátovanie

Editor používa textovú oblasť s číslami riadkov, písmom s pevnou šírkou znakov a horizontálnym posuvníkom. Číslovanie sa posúva spolu s textom. Klávesy `Tab` a `Shift+Tab` zachovávajú bežný presun medzi formulárovými prvkami.

Nad textovou oblasťou je panel s tlačidlom **Formátovať JSON** bez rámika a dostupným AI asistentom vľavo a aktuálnou pozíciou kurzora vpravo, napríklad **Riadok 10, stĺpec 12**. Pozícia sa zobrazuje len počas focusu textovej oblasti a pri jeho strate sa skryje. Aktualizuje sa pri písaní, kliknutí a pohybe klávesnicou; pri označení textu zobrazuje aktívny koniec výberu. Riadky aj stĺpce sa počítajú od 1.

Tlačidlo **Formátovať JSON** najskôr overí vstup a potom ho odsadí dvoma medzerami. Mení iba biele znaky mimo reťazcov a komentárov; zachováva úvodzovky/apostrofy, číselné zápisy, poradie vlastností aj escape sekvencie. Komentáre za hodnotou zostávajú na rovnakom riadku; samostatné komentáre zostávajú na vlastnom riadku. Nepoužíva spätnú serializáciu parsovaných hodnôt, ktorá by mohla zaokrúhliť veľké číselné identifikátory. Otvorenie editora a uloženie záznamu text automaticky neformátuje.

Príklad platnej hodnoty:

```json
{
  "productId": 9007199254740993,
  "variants": [
    {"code": "blue", "available": true}
  ]
}
```

### Validácia a uloženie

- Okrem štandardného JSON je podporovaný rozšírený zápis: jednoduché úvodzovky (apostrofy), názvy vlastností bez úvodzoviek a komentáre `//` aj `/* … */`. Neúvodzovkovaný názov začína písmenom, `_` alebo `$`; ďalej môže obsahovať aj číslice a pomlčky, napríklad `data-toggle`. Pomlčka bez úvodzoviek je rozšírením tohto editora, nie štandardnou syntaxou JavaScriptu.
- Povolený je práve jeden JSON objekt v zložených zátvorkách `{}`. Vnorené objekty a polia sú povolené; samotné pole `[]`, reťazec, číslo, `true`, `false` a `null` na koreni sa odmietnu.
- Kontroluje sa celý vstup. Koncová čiarka, chýbajúce zátvorky alebo druhý objekt za prvým sú neplatné. Funkcie, volania JavaScriptu, `undefined`, `NaN` a `Infinity` nie sú povolené. Parser kód nikdy nespúšťa.
- Komentár `//` pokračuje až po koniec riadka. Uzatváracie zátvorky objektu preto musia byť na ďalšom riadku; v jednoriadkovom zápise použite komentár `/* … */`.
- Prázdny vstup vrátane samotných medzier je povolený, ak je vypnuté **Povinné pole**. Pri zapnutej povinnosti sa musí zadať objekt; prázdny objekt `{}` je platná hodnota.
- V prehliadači sa vstup kontroluje pri opustení poľa aj pred uložením. Chyba sa zobrazí pri poli a pri pokuse o uloženie sa otvorí jeho karta. Ak parser poskytne polohu syntaktickej chyby, hlásenie obsahuje riadok a stĺpec.
- Server vykonáva rovnakú kontrolu nezávisle od JavaScriptu pri ukladaní cez DataTables Editor, priamy REST aj import. Konfiguráciu typu a povinnosti načíta zo servera podľa entity, šablóny a domény; definícia poľa odoslaná klientom nemôže validáciu vypnúť. Pri čiastočnej úprave sa overí výsledná hodnota vrátane zachovaných údajov z existujúceho záznamu.
- Ukladanie webových stránok overí hodnoty aj v `EditorService.saveEditedDoc()` pred zápisom stránky a jej histórie. Neplatná hodnota zablokuje uloženie a nevytvorí novú historickú verziu.

Validácia kontroluje syntax a koreňový objekt. Neoveruje prítomnosť ani význam konkrétnych MHUB atribútov podľa JSON Schema.

Príklad podporovaného rozšíreného zápisu:

```text
{
  title: 'test',
  data-toggle: 'tooltip',
  'event': 'action.questionDropdown.FAQ',
  'action': {
    'questionDropdown': {
      'content': '{Sú volania v Go paušáloch naozaj neobmedzené?}' // text otázky
    }
  }
}
```

Rozšírený zápis sa ukladá v pôvodnej podobe vrátane apostrofov a komentárov. Nie je automaticky prevedený na striktný JSON pre `JSON.parse`; aplikácia, ktorá hodnotu spracúva, musí podporovať použitú syntax.

Pre vlastné REST controllery odvodené od `DatatableRestControllerV2` sa prekladové kľúče pre validáciu predvolene odvodia z `@DataTableColumn.title` na atribútoch `fieldA` až `fieldZ`. Ak vaša aplikácia používa iný prefix, než vyplýva z anotácií, prekryte serverový hook `protected String getCustomFieldsKeyPrefix(T entity)` tak, aby vracal rovnaký prefix ako volanie `BaseEditorFields.getFields()`:

```java
@Override
protected String getCustomFieldsKeyPrefix(QuestionsAnswersEntity entity) {
    return "components.qa";
}
```

Predvolená hodnota `null` ponechá odvodenie z anotácií. Kontext konfigurácie v tabuľke `custom_fields`, napríklad väzbu na rodičovskú entitu, naďalej určuje existujúci hook `getCustomFieldsSearchDto(T entity)`. Oba hooky vychádzajú zo serverových údajov a vyhodnocujú sa pre konkrétny ukladaný záznam.

### Kapacita databázy

Hodnota zostáva textom v existujúcom atribúte `fieldA` až `fieldT` a v príslušnom databázovom stĺpci `field_a` až `field_t`. Typ `jsoneditor` nemení databázový typ ani automaticky nerozširuje stĺpce.

Základná schéma pre voliteľné polia webových stránok používa dĺžku 255 znakov. Pred nasadením pre JSON s veľkosťou niekoľko KB overte skutočnú kapacitu konkrétneho stĺpca a prípadne ju rozšírte **v tabuľke `documents` aj `documents_history`**. Ide o samostatnú úpravu zákazníckej inštalácie. Syntakticky platný JSON musí zároveň spĺňať obmedzenia dĺžky uložených údajov.

## Frontend

Integrácia do editora datatabuľky je implementovaná v súbore [custom-fields.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/custom-fields.js). Pre každé pole z JSON objektu ```editorFields.fieldsDefinition``` sa získa nastavenie a nanovo sa v DOM strome vytvoria formulárové polia.

Kľúčové je pripojenie formulárového poľa k existujúcemu editoru, to je zabezpečené volaním:

```javascript
EDITOR.field("field"+keyUpper).s.opts._input = inputBox.find('input, select, textarea');
```

ktoré z nového ```inputBox``` objektu získa formulárové pole a to nastaví editoru. Je použité interné API volanie ```.s.opts._input```, čo je nebezpečné z pohľadu zmien v API v datatables editore, ale iné riešenie sme nenašli.

Vyvolanie funkcie je vykonané v [index.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) pri otvorení okna.

```javascript
import * as CustomFields from './custom-fields';

EDITOR.on('open', function (e, mode, action) {
    ...
    CustomFields.update(EDITOR);
});
```

V prípade použitia ```multiple select``` tento ukladá hodnotu poľa ako ```Array```. Konverzia na String oddelený ```|``` pred odoslaním formuláru je zabezpečená pomocou metódy ```prepareCustomFieldsDataBeforeSend```, ktorá sa volá v [index.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

```javascript
EDITOR.on('preSubmit', function (e, data, action) {
    ...
    prepareCustomFieldsDataBeforeSend(data)
    ...
});
```

Pri type `autocomplete` sa odosiela request na `/admin/FCKeditor/_editor_autocomplete.jsp` aj s parametrami `className` a `objectId`. Endpoint preto vie uprednostniť konfiguráciu z `custom_fields` pred generickým prekladovým kľúčom.

Pri typoch `select` a `multiselect` je podporovaný formát `label:value`. Ak hodnota neobsahuje presne jednu dvojbodku, použije sa rovnaký text pre `label` aj `value`.

## Premenovanie stĺpcov

Ak potrebujete aj premenovať zobrazené stĺpce v tabuľke podľa voliteľných polí stačí nastaviť voľbu ```customFieldsUpdateColumns: true``` pri inicializácii datatabuľky. Nastavenia voliteľných polí sa získajú z prvého záznamu ```content[0].editorFields.fieldsDefinition``` po každom načítaní dát.

Stĺpce s ```null``` v ```label``` sa v tabuľke schovajú a schovajú sa aj v nastavení zobrazenia stĺpcov (akoby neexistovali).

```javascript
translationKeysTable = WJ.DataTable({
    url: "/admin/v9/settings/translation-keys",
    columns: columns,
    serverSide: true,
    customFieldsUpdateColumns: true
});
```

Nastavením `customFieldsUpdateColumnsPreserveVisibility` na hodnotu `true` sa pre používateľa zachová nastavenie zobrazenia stĺpcov pre režim `customFieldsUpdateColumns`. Je možné použiť len v prípade, kedy pre datatabuľku nie sú menené stĺpce počas zobrazenia. Napr. v sekcii Prekladové kľúče sa dáta nemenia, je možné nastaviť na `true` a používateľovi sa zachová nastavenie zobrazenia stĺpcov. V sekcii Číselníky sa menia stĺpce pri zmene číselníka (načítaní dát), tam táto možnosť nie je použiteľná.

### Detaily implementácie

Spracovanie je v ```index.js``` vo funkcii ```updateOptionsFromJson```. Ak je zapnutá možnosť ```DATA.customFieldsUpdateColumns===true``` a JSON objekt obsahuje v prvom zázname obsahuje ```editorFields?.fieldsDefinition``` tak sa zmenia názvy stĺpcov v hlavičke a aj v ```DATA``` objekte. Stĺpce s názvom ```null``` sa schovajú (to zabezpečuje konfigurácia ```colVis``` vo funkcii ```columns``` kde sa stĺpce s názvom ```null``` vynechajú). Následne sa vyvolá ```$("#"+DATA.id).trigger("column-reorder.dt");``` aby sa aktualizovali názvy stĺpcov v nastavení zobrazenia stĺpcov (```colvis```).

V definícii ```buttons.colvis``` je upravené čítanie ```columnText``` tak, aby zobralo vždy aktuálnu hodnotu z ```DATA``` definície a ```columns``` funkcii, ktorá definuje aké stĺpce sa v nastavení zobrazia, sa vráti ```true/false``` podľa toho, či má stĺpec názov ```null```. Takto sa vždy v nastavení zobrazenia stĺpcov zobrazia aktuálne názvy stĺpcov a schovajú sa tie, ktoré nemajú definovaný názov (napoužívajú sa).
