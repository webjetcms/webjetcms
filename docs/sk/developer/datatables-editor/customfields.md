# Voliteľné polia

Cez voliteľné polia je možné web stránke a adresáru nastavovať voliteľné atribúty (hodnoty, texty) podľa potreby zákazníka. Hodnoty je následne možné preniesť a použiť v šablóne stránky, viď [dokumentácia pre Frontend programátora](../../frontend/webpages/customfields/README.md).

# Backend

Nastavenia voliteľných polí sú závislé od použitej šablóny, skupiny šablón alebo domény (keďže sú riadené nastavením prekladových kľúčov). Možnosti je teda z backend-u potrebné odosielať pre každú editovanú web stránku samostatne. Prenos nastavení je genericky implementovaný [BaseEditorFields.getFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java):

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

Príklad v [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) kde je viac operácii a preto metóda ```fromDocDetails``` je implementovaná samostatne v ```editorFields``` objekte a volaná z ```DocRestController.processFromEntity```.

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

## Frontend

Integrácia do editora datatabuľky je implementovaná v súbore [custom-fields.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/custom-fields.js). Pre každé pole z JSON objektu ```editorFields.fieldsDefinition``` sa získa nastavenie a nanovo sa v DOM strome vytvoria formulárové polia.

Kľúčové je pripojenie formulárového poľa k existujúcemu editoru, to je zabezpečené volaním:

```javascript
EDITOR.field("field"+keyUpper).s.opts._input = inputBox.find('input, select');
```

ktoré z nového ```inputBox``` objektu získa formulárové pole a to nastaví editoru. Je použité interné API volanie ```.s.opts._input```, čo je nebezpečné z pohľadu zmien v API v datatables editore, ale iné riešenie sme nenašli.

Vyvolanie funkcie je vykonané v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) pri otvorení okna.

```javascript
import * as CustomFields from './custom-fields';

EDITOR.on('open', function (e, mode, action) {
    ...
    CustomFields.update(EDITOR);
});
```

V prípade použitia ```multiple select``` tento ukladá hodnotu poľa ako ```Array```. Konverzia na String oddelený ```|``` pred odoslaním formuláru je zabezpečená pomocou metódy ```prepareCustomFieldsDataBeforeSend```, ktorá sa volá v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

```
EDITOR.on('preSubmit', function (e, data, action) {
    ...
    prepareCustomFieldsDataBeforeSend(data)
    ...
});
```

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
