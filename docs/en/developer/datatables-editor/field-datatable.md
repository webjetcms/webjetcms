# Field Type - datatable

The datatable field allows you to display a nested datatable in the page editor (e.g. the Media list in the page editor). Note that the datatable is initialized with its own URL. In the parent table object itself, there is no need to send data or receive it afterwards, the data is changed automatically when the nested datatable's REST service is called. Technically it would be possible to work directly with JSON data from the parent table, but this option is not yet implemented.

Since the currently inserted datatable works with separate REST services the returned data is an empty array `[]`.

## Use of annotation

Annotation is used as `DataTableColumnType.DATATABLE`, and the following editor attributes must be set:
- `data-dt-field-dt-url` - URL of the REST service, can contain macros for inserting values from the parent editor, e.g.: `/admin/rest/audit/notify?docid={docId}&groupId={groupId}`
- `data-dt-field-dt-columns` - the name of the class (including packages) from which it is to be used [definition of datatable columns](datatable-columns.md), e.g. `sk.iway.iwcm.system.audit.AuditNotifyEntity`
- `data-dt-field-dt-tabs` - list of tabs for the editor in JSON format. All names and values of the JSON object need to be wrapped in `'`, translations are replaced automatically. Example: `@DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'fields', 'title': '[[#{editor.tab.fields}]]' }]")`.

Full annotation example:

```java
@DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
    tab = "media",
    editor = { @DataTableColumnEditor(
        attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/audit/notify"),
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.system.audit.AuditNotifyEntity")
        }
    )
})
private List<Media> media;
```

You can also set other configuration options of the datatable using data attributes. The data is sent as a string. Values `true` a `false` are converted to `boolean` object. Attribute setting `order` allows you to set the layout for only one column. The other options are transferred as a string.

Add a prefix to the menu of the set option `data-dt-field-dt-`, i.e. to set the option `serverSide` use the key `data-dt-field-dt-serverSide`. Example of additional annotations:

```java
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false"), //vypnutie serveroveho strankovania/vyhladavania
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "2,desc"), //nastavenie usporiadania podla 2. stlpca
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,import,celledit") //vypnutie zobrazenia uvedenych tlacidiel
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "groupId,fullPath"), //vynuti zobrazenie len uvedenych stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"), //JS funkcia ktora sa zavola pre upravu zoznamu stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.groups_title") //nadpis nad datatabulkou na celu sirku okna
```

**API and events**

The created datatable is made available as:
- `conf.datatable` on the original `conf` datatable column object
- `window` object called `datatableInnerTable_fieldName` - object can be used for automated testing or other JavaScript operations.
After the nested datatable is created, the event is fired `WJ.DTE.innerTableInitialized` where in the object `event.detail.conf` is the transferred configuration.

## Notes on implementation

The implementation is in the file [field-type-datatable.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-datatable.js) and in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) set as `$.fn.dataTable.Editor.fieldTypes.datatable = fieldTypeDatatable.typeDatatable();`.

Feature `resizeDatatable`

`conf.datatable.columns.adjust();`

Feature `getUrlWithParams``?docid={docId}&groupId={groupId}``{docId}` a `{groupId}``EDITOR.currentJson`.