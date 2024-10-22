# Volitelná pole

Prostřednictvím volitelných polí je možné nastavit volitelné atributy (hodnoty, texty) webové stránky a adresáře podle potřeb zákazníka. Tyto hodnoty lze poté přenést a použít v šabloně stránky, viz. [dokumentace pro Frontend programátora](../../frontend/webpages/customfields/README.md).

# Backend

Nastavení nepovinných polí závisí na použité šabloně, skupině šablon nebo doméně (protože jsou řízeny nastavením překladového klíče). Proto je třeba volitelná pole odeslat z backendu pro každou upravovanou webovou stránku zvlášť. Přenos nastavení je realizován obecně [BaseEditorFields.getFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java):

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

Volejte `getFields` jak vidíte, má anotaci `@JsonIgnore`. Je nutné implicitně zavolat metodu pro přípravu pole objektů. Základní příklad použití:

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

Příklad v [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) kde je více operací, a proto metoda `fromDocDetails` je implementován samostatně v `editorFields` a volán z `DocRestController.processFromEntity`.

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

v metodě `fromDocDetails` nejprve se nastaví předpony překladového klíče pro vyhledávání podle skupiny šablon i ID šablony a poté se načte seznam. `fieldsDefinition` (frontend implicitně hledá tento seznam v objektu `editorFields.fieldsDefinition`).

Pro funkčnost je nutné, aby fazole obsahovala atributy s názvem `fieldX`, který zase s voláním `getFields` může do libovolné fazole vnést nepovinná pole.

## Frontend

Integrace do editoru datových souborů je implementována v souboru [custom-fields.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/custom-fields.js). Pro každé pole objektu JSON `editorFields.fieldsDefinition` jsou získána nastavení a pole formuláře jsou znovu vytvořena ve stromu DOM.

Klíčové je propojit formulářové pole s existujícím editorem, což zajistí volání:

```javascript
EDITOR.field("field"+keyUpper).s.opts._input = inputBox.find('input, select');
```

které z nového `inputBox` získá pole formuláře a nastaví jej do editoru. Používá se interní volání API `.s.opts._input` což je nebezpečné z hlediska změn API v editoru datových tabulek, ale nenašli jsme jiné řešení.

Volání funkce se provádí v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) při otevření okna.

```javascript
import * as CustomFields from './custom-fields';

EDITOR.on('open', function (e, mode, action) {
    ...
    CustomFields.update(EDITOR);
});
```

V případě použití `multiple select` uloží hodnotu pole jako `Array`. Převod na řetězce oddělené `|` před odesláním je formulář zabezpečen pomocí metody `prepareCustomFieldsDataBeforeSend` který se volá v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

```
EDITOR.on('preSubmit', function (e, data, action) {
    ...
    prepareCustomFieldsDataBeforeSend(data)
    ...
});
```

## Přejmenování sloupců

Pokud potřebujete také přejmenovat zobrazené sloupce v tabulce podle volitelných polí, stačí nastavit možnost `customFieldsUpdateColumns: true` při inicializaci datové tabulky. Nastavení nepovinných polí se získávají z prvního záznamu `content[0].editorFields.fieldsDefinition` po každém načtení dat.

Sloupce s `null` v `label` jsou v tabulce skryty a jsou skryty i v nastavení zobrazení sloupců (jako by neexistovaly).

```javascript
translationKeysTable = WJ.DataTable({
    url: "/admin/v9/settings/translation-keys",
    columns: columns,
    serverSide: true,
    customFieldsUpdateColumns: true
});
```

Nastavením `customFieldsUpdateColumnsPreserveVisibility` na hodnotu `true` nastavení zobrazení sloupce pro režim je pro uživatele zachováno. `customFieldsUpdateColumns`. Lze ji použít pouze v případě, že se sloupce datové tabulky během zobrazení nemění. Například v sekci Překladové klíče se data nemění, lze ji nastavit na hodnotu `true` a uživatel zachová nastavení zobrazení sloupců. V sekci Číselníky se sloupce mění při změně číselníku (načtení dat), tato volba zde neplatí.

### Podrobnosti o provádění

Zpracování probíhá v `index.js` ve funkci `updateOptionsFromJson`. Pokud je tato možnost zapnutá `DATA.customFieldsUpdateColumns===true` a objekt JSON obsažený v prvním záznamu obsahuje `editorFields?.fieldsDefinition` takže názvy sloupců v záhlaví a také v tabulce `DATA` zařízení. Sloupce s názvem `null` jsou skryty (to je zajištěno konfigurací `colVis` ve funkci `columns` kde sloupce s názvem `null` vynechat). Pak se `$("#"+DATA.id).trigger("column-reorder.dt");` aktualizovat názvy sloupců v nastavení zobrazení sloupců (`colvis`).

V definici `buttons.colvis` je upravené čtení `columnText` aby vždy přebírala aktuální hodnotu z `DATA` definice a `columns` funkce, která určuje, jaké sloupce se zobrazí v nastavení, vrací hodnotu `true/false` podle toho, zda má sloupec název `null`. Tímto způsobem se v nastavení zobrazení sloupců vždy zobrazí aktuální názvy sloupců a ty, které nemají definovaný název, se skryjí (jsou použity).
