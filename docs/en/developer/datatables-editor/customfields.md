# Optional fields

Optional fields allow you to set optional attributes (values, texts) for the website and directory according to the customer's needs. The values ​​can then be transferred and used in the page template, see [Frontend programmer documentation](../../frontend/webpages/customfields/README.md).

# Backend

The settings of optional fields depend on the used template, template group or domain (since they are controlled by the translation key settings). Therefore, the options need to be sent from the backend for each edited web page separately. The transfer of settings is generically implemented [BaseEditorFields.getFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java):

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

The call ```getFields``` as you can see has the annotation ```@JsonIgnore```. You must implicitly call the method to prepare the array of objects. Basic usage example:

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

Example in [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) where there are multiple operations and therefore the method ```fromDocDetails``` is implemented separately in the ```editorFields``` object and called from ```DocRestController.processFromEntity```.

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

In the ```fromDocDetails``` method, the translation key prefixes are first set for searching by template group and template ID, and then the list ```fieldsDefinition``` is obtained (the frontend implicitly searches for this list in the ```editorFields.fieldsDefinition``` object).

For functionality, it is necessary that the given bean contains attributes named ```fieldX```, which can then bring optional fields to any bean with a call to ```getFields```.

## Frontend

Integration into the datatable editor is implemented in the file [custom-fields.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/custom-fields.js). For each field, the settings are retrieved from the JSON object ```editorFields.fieldsDefinition``` and the form fields are recreated in the DOM tree.

The key is to connect the form field to the existing editor, this is done by calling:

```javascript
EDITOR.field("field"+keyUpper).s.opts._input = inputBox.find('input, select');
```

which gets the form field from the new ```inputBox``` object and sets it to the editor. An internal API call ```.s.opts._input``` is used, which is dangerous from the point of view of changes in the API in the datatables editor, but we have not found another solution.

The function call is made in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) when the window is opened.

```javascript
import * as CustomFields from './custom-fields';

EDITOR.on('open', function (e, mode, action) {
    ...
    CustomFields.update(EDITOR);
});
```

In case of using ```multiple select``` this stores the field value as ```Array```. Conversion to String separated by ```|``` before submitting the form is provided by the method ```prepareCustomFieldsDataBeforeSend```, which is called in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

```
EDITOR.on('preSubmit', function (e, data, action) {
    ...
    prepareCustomFieldsDataBeforeSend(data)
    ...
});
```

## Renaming columns

If you also need to rename the displayed columns in the table according to the optional fields, just set the ```customFieldsUpdateColumns: true``` option when initializing the datatable. The optional field settings are obtained from the first record ```content[0].editorFields.fieldsDefinition``` after each data load.

Columns with ```null``` in ```label``` are hidden in the table and are also hidden in the column display settings (as if they did not exist).

```javascript
translationKeysTable = WJ.DataTable({
    url: "/admin/v9/settings/translation-keys",
    columns: columns,
    serverSide: true,
    customFieldsUpdateColumns: true
});
```

Setting `customFieldsUpdateColumnsPreserveVisibility` to the value `true` will preserve the column display setting for the `customFieldsUpdateColumns` mode for the user. It can only be used if the columns for the data table are not changed during display. For example, in the Translation keys section, the data does not change, it can be set to `true` and the user will preserve the column display setting. In the Codebooks section, the columns change when the codebook is changed (data is loaded), this option is not applicable there.

### Implementation details

The processing is in ```index.js``` in the function ```updateOptionsFromJson```. If the option ```DATA.customFieldsUpdateColumns===true``` is enabled and the JSON object contains ```editorFields?.fieldsDefinition``` in the first record, the column names in the header and also in the ```DATA``` object will be changed. Columns with the name ```null``` are hidden (this is ensured by the configuration of ```colVis``` in the function ```columns``` where columns with the name ```null``` are omitted). Subsequently, ```$("#"+DATA.id).trigger("column-reorder.dt");``` is called to update the column names in the column display settings (```colvis```).

In the definition ```buttons.colvis```, the reading ```columnText``` is modified so that it always takes the current value from the ```DATA``` definition and the ```columns``` function that defines which columns will be displayed in the setting returns ```true/false``` depending on whether the column has the name ```null```. This way, the current column names are always displayed in the column display setting and those that do not have a defined name (are in use) are hidden.
