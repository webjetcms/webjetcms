# Field Type - WYSIWYG

The field will display ```WYSIWYG``` page editor (HTML code), in our case [ckeditor](https://ckeditor.com/ckeditor-4/) is used. Currently, this field can only be used once on a page, multiple use is not supported yet.

## Using annotation

The annotation is used as ```DataTableColumnType.WYSIWYG```, complete example of the annotation:

```java
@DataTableColumn(inputType = DataTableColumnType.WYSIWYG, title="components.news.template_html",
    hidden = true, tab="content"
)
String data = "";
```

The field will be displayed in the full-screen tab, automatically calculating the tab height based on the window size. In the tab definition in the pug file, set the ```content``` attribute to an empty value to disable the display of the background of ```label``` elements:

```javascript
var tabs = [
    { id: 'content', title: '[[\#{editor.tab.editor}]]', selected: true, content: '' },
    ...
]
```

## Implementation notes

The implementation is in the file [field-type-wysiwyg.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-wysiwyg) and in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) set as ```$.fn.dataTable.Editor.fieldTypes.wysiwyg = fieldTypeWysiwyg.typeWysiwyg();```.

When displayed in the editor, a separate form named ```editorForm``` is generated in the ```create``` function, which contains additional fields ```docId```, ```groupId```, ```virtualPath```, ```title``` with values ​​filled according to the current JSON object. These fields are used for historical reasons in various ckeditor components to obtain the ID of the currently edited page (e.g. to display the Media of this page when selecting an image/file).

To speed up the display of the list of web pages, ckeditor is initialized only when the dialog box is first opened. This is ensured by the code ```EDITOR.on( 'open', function ( e, type ) {``` in which ```conf.wjeditor``` is tested and initialized when the dialog box is first opened.

### Initializing ckeditor

The ckeditor functionality itself is modified in the file [datatables-ckeditor.js](../../../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) which modifies the standard ckeditor for WebJET requirements (elfinder integration, modified dialogs, added new plugins, etc.). The basis is based on the original code from version 8, but the functionality is wrapped in the ```DatatablesCkEditor``` class. Its initialization is encapsulated in ```app.js``` as:

```javascript
const createDatatablesCkEditor = () => {
    return import(/* webpackChunkName: "ckeditor" */ './datatables-ckeditor');
};
window.createDatatablesCkEditor = createDatatablesCkEditor;
```

which will trigger an asynchronous loading of the JS file and its initialization. The actual modification for WebJET's needs is done in the ```customizeEditor``` function.

### Data setup

For its functionality, the editor needs to know not only the HTML code itself, but also other data, such as the template (used CSS files), etc. The entire JSON object is processed in the ```setJson``` function when the editor is opened.

### List of regular expressions for forms

The list of regular expressions for the form element properties dialogs is initialized by AJAX calling the URL ```/admin/rest/forms-list/regexps```. The returned list is set to the ```regexps``` attribute and used from there to initialize the dialogs.

### Editor height

The editor height is calculated by the ```resizeEditor``` function at an interval of 3000 ms. The height is calculated based on the browser window height to maximize window usage. The top and bottom ```margin``` of the editor dialog box are deducted, and of course the header and footer sizes.
