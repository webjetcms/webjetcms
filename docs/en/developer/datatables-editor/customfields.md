# Optional fields

Via optional fields it is possible to set optional attributes (values, texts) to the web page and directory according to the customer's needs. The values can then be transferred and used in the page template, see [documentation for Frontend programmer](../../frontend/webpages/customfields/README.md).

# Backend

The settings for the optional fields depend on the template, template group, or domain being used (since they are controlled by the translation key settings). Thus, the options need to be sent from the backend for each edited web page separately. The transfer of settings is generically implemented [BaseEditorFields.getFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java):

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

Call `getFields` as you can see it has an annotation `@JsonIgnore`. You must implicitly call the method to prepare the object array. Basic usage example:

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

Example in [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) where there are more operations and therefore the method `fromDocDetails` is implemented separately in `editorFields` object and called from `DocRestController.processFromEntity`.

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

in the method `fromDocDetails` translation key prefixes are first set for searching by both template group and template ID, and then the list is retrieved `fieldsDefinition` (the frontend implicitly looks for this list in the object `editorFields.fieldsDefinition`).

For functionality it is necessary that the bean contains attributes named `fieldX`, which in turn with the call `getFields` can bring optional fields to any bean.

## Frontend

Integration into the datatable editor is implemented in the file [custom-fields.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/custom-fields.js). For each field of the JSON object `editorFields.fieldsDefinition` the settings are obtained and the form fields are re-created in the DOM tree.

The key is to connect the form field to an existing editor, this is provided by the call:

```javascript
EDITOR.field("field" + keyUpper).s.opts._input = inputBox.find("input, select");
```

which from the new `inputBox` object gets the form field and sets it to the editor. An internal API call is used `.s.opts._input`which is dangerous in terms of API changes in the datatables editor, but we have not found any other solution.

The function call is made in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) when you open the window.

```javascript
import * as CustomFields from './custom-fields';

EDITOR.on('open', function (e, mode, action) {
    ...
    CustomFields.update(EDITOR);
});
```

In case of use `multiple select` this stores the value of the field as `Array`. Conversion to String separated `|` before sending the form is secured using the method `prepareCustomFieldsDataBeforeSend`which is called in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

```
EDITOR.on('preSubmit', function (e, data, action) {
    ...
    prepareCustomFieldsDataBeforeSend(data)
    ...
});
```

## Renaming columns

If you also need to rename the displayed columns in the table according to the optional fields just set the option `customFieldsUpdateColumns: true` when initializing the datatable. The settings of the optional fields are obtained from the first record `content[0].editorFields.fieldsDefinition` after each data retrieval.

Columns with `null` v `label` are hidden in the table and are also hidden in the column display settings (as if they did not exist).

```javascript
translationKeysTable = WJ.DataTable({
	url: "/admin/v9/settings/translation-keys",
	columns: columns,
	serverSide: true,
	customFieldsUpdateColumns: true,
});
```

By setting up `customFieldsUpdateColumnsPreserveVisibility` to the value of `true` the column display setting for the mode is preserved for the user `customFieldsUpdateColumns`. It can only be used when the columns for the datatable are not changed during display. For example, in the Translation Keys section, the data is not changed, it can be set to `true` and the user will retain the column display settings. In the Dials section, the columns are changed when the dial is changed (data is loaded), this option is not applicable there.

### Implementation details

Processing is in `index.js` in the function of `updateOptionsFromJson`. If the option is turned on `DATA.customFieldsUpdateColumns===true` and the JSON object contains in the first record contains `editorFields?.fieldsDefinition` so the column names in the header and also in the `DATA` facility. Columns named `null` are hidden (this is ensured by the configuration `colVis` in the function of `columns` where the columns named `null` omit). Then the `$("#"+DATA.id).trigger("column-reorder.dt");` to update the column names in the column view settings (`colvis`).

In the definition `buttons.colvis` is a modified reading `columnText` so that it always takes the current value from `DATA` definitions and `columns` function, which defines what columns are displayed in the settings, returns `true/false` according to whether the column has the name `null`. This way, the current column names are always shown in the column display settings and those that do not have a defined name are hidden (they are used).
