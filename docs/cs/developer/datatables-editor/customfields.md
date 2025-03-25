# Volitelná pole

Přes volitelná pole lze web stránce a adresáři nastavovat volitelné atributy (hodnoty, texty) dle potřeby zákazníka. Hodnoty je následně možné přenést a použít v šabloně stránky, viz [dokumentace pro Frontend programátora](../../frontend/webpages/customfields/README.md).

# Backend

Nastavení volitelných polí jsou závislá na použité šabloně, skupině šablon nebo domény (jelikož jsou řízena nastavením překladových klíčů). Možnosti je tedy z backend-u třeba odesílat pro každou editovanou web stránku samostatně. Přenos nastavení je genericky implementován [BaseEditorFields.getFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java):

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

volání `getFields` jak vidíte má anotaci `@JsonIgnore`. Metodu musíte implicitně zavolat pro přípravu pole objektů. Základní příklad použití:

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

Příklad v [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) kde je více operací a proto metoda `fromDocDetails` je implementována samostatně v `editorFields` objektu a volaná z `DocRestController.processFromEntity`.

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

v metodě `fromDocDetails` jsou nejprve nastaveny prefixy překladových klíčů pro vyhledávání podle skupiny šablon i podle ID šablony a následně je získán seznam `fieldsDefinition` (frontend implicitně tento seznam hledá v objektu `editorFields.fieldsDefinition`).

Pro funkčnost je třeba aby daný bean obsahoval atributy s názvem `fieldX`, což následně s voláním `getFields` umí přinést volitelná pole do libovolného beanu.

## Frontend

Integrace do editoru datatabulky je implementována v souboru [custom-fields.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/custom-fields.js). Pro každé pole z JSON objektu `editorFields.fieldsDefinition` se získá nastavení a nově se v DOM stromu vytvoří formulářová pole.

Klíčové je připojení formulářového pole ke stávajícímu editoru, to je zabezpečeno voláním:

```javascript
EDITOR.field("field"+keyUpper).s.opts._input = inputBox.find('input, select');
```

které z nového `inputBox` objektu získá formulářové pole a to nastaví editoru. Je použito interní API volání `.s.opts._input`, což je nebezpečné z pohledu změn v API v datatables editoru, ale jiné řešení jsme nenašli.

Vyvolání funkce je provedeno v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) při otevření okna.

```javascript
import * as CustomFields from './custom-fields';

EDITOR.on('open', function (e, mode, action) {
    ...
    CustomFields.update(EDITOR);
});
```

V případě použití `multiple select` tento ukládá hodnotu pole jako `Array`. Konverze na String oddělený `|` před odesláním formuláře je zajištěna pomocí metody `prepareCustomFieldsDataBeforeSend`, která se jmenuje v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

```
EDITOR.on('preSubmit', function (e, data, action) {
    ...
    prepareCustomFieldsDataBeforeSend(data)
    ...
});
```

## Přejmenování sloupců

Pokud potřebujete i přejmenovat zobrazené sloupce v tabulce podle volitelných polí stačí nastavit volbu `customFieldsUpdateColumns: true` při inicializaci datatabulky. Nastavení volitelných polí se získají z prvního záznamu `content[0].editorFields.fieldsDefinition` po každém načtení dat.

Sloupce s `null` v `label` se v tabulce schovají a schovají se iv nastavení zobrazení sloupců (jakoby neexistovaly).

```javascript
translationKeysTable = WJ.DataTable({
    url: "/admin/v9/settings/translation-keys",
    columns: columns,
    serverSide: true,
    customFieldsUpdateColumns: true
});
```

Nastavením `customFieldsUpdateColumnsPreserveVisibility` na hodnotu `true` se pro uživatele zachová nastavení zobrazení sloupců pro režim `customFieldsUpdateColumns`. Lze použít pouze v případě, kdy pro datatabulku nejsou měněny sloupce během zobrazení. Např. v sekci Překladové klíče se data nemění, lze nastavit na `true` a uživateli se zachová nastavení zobrazení sloupců. V sekci Číselníky se mění sloupce při změně číselníku (načtení dat), tam tato možnost není použitelná.

### Detaily implementace

Zpracování je v `index.js` ve funkci `updateOptionsFromJson`. Je-li zapnuta možnost `DATA.customFieldsUpdateColumns===true` a JSON objekt obsahuje v prvním záznamu obsahuje `editorFields?.fieldsDefinition` tak se změní názvy sloupců v hlavičce a také v `DATA` objektu. Sloupce s názvem `null` se schovají (to zabezpečuje konfigurace `colVis` ve funkci `columns` kde se sloupce s názvem `null` vynechají). Následně se vyvolá `$("#"+DATA.id).trigger("column-reorder.dt");` aby se aktualizovaly názvy sloupců v nastavení zobrazení sloupců (`colvis`).

V definici `buttons.colvis` je upraveno čtení `columnText` tak, aby vzalo vždy aktuální hodnotu z `DATA` definice a `columns` funkci, která definuje jaké sloupce se v nastavení zobrazí, se vrátí `true/false` podle toho, zda má sloupec název `null`. Takto se vždy v nastavení zobrazení sloupců zobrazí aktuální názvy sloupců a schovají se ty, které nemají definovaný název (napoužívají se).
