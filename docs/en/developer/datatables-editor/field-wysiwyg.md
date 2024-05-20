# Field Type - WYSIWYG

The field displays `WYSIWYG` page editor (HTML code), in our case it is used [ckeditor](https://ckeditor.com/ckeditor-4/). Currently this field can only be used once on a page, multiple use is not yet supported.

## Use of annotation

Annotation is used as `DataTableColumnType.WYSIWYG`, a complete annotation example:

```java
@DataTableColumn(inputType = DataTableColumnType.WYSIWYG, title="components.news.template_html",
    hidden = true, tab="content"
)
String data = "";
```

The field appears in a full-width tab, automatically calculating the height of the tab according to the window size. In the tab definition in the pug file, set the attribute `content` to blank to turn off background display `label` elements:

```javascript
var tabs = [
    { id: 'content', title: '[[\#{editor.tab.editor}]]', selected: true, content: '' },
    ...
]
```

## Notes on implementation

The implementation is in the file [field-type-wysiwyg.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-wysiwyg) and in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) set as `$.fn.dataTable.Editor.fieldTypes.wysiwyg = fieldTypeWysiwyg.typeWysiwyg();`.

When displayed in the editor, the `create` function generates a separate form called `editorForm`which contains additional fields `docId`, `groupId`, `virtualPath`, `title` with values populated according to the current JSON object. These fields are used for historical reasons in various ckeditor components to get the ID of the currently edited page (e.g. to display the Media of that page when selecting an image/file).

For the sake of speed of displaying the list of web pages, the ckeditor is initialized only when the dialog box is first opened. This is ensured by the code `EDITOR.on( 'open', function ( e, type ) {` in which it is tested `conf.wjeditor` and is initialized the first time it is opened.

### Initializing ckeditor

The functionality of the ckeditor itself is modified in the file [datatables-ckeditor.js](../../../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) which modifies the standard ckeditor for WebJET requirements (elfinder integration, modified dialogs, added new plugins, etc.). The base is based on the original code from version 8, but the functionality is wrapped in a class `DatatablesCkEditor`. Its initialization is encapsulated in `app.js` Like:

```javascript
const createDatatablesCkEditor = () => {
	return import(/* webpackChunkName: "ckeditor" */ "./datatables-ckeditor");
};
window.createDatatablesCkEditor = createDatatablesCkEditor;
```

which causes asynchronous loading of the JS file and its initialization. The actual modification for WebJET is done in the function `customizeEditor`.

### Data settings

The editor needs to know not only the HTML code itself, but also other data such as the template (CSS files used) and so on. The whole JSON object is processed in the function when the editor is opened `setJson`

### List of regular expressions for forms

The list of regular expressions for form element property dialogs is initialized by AJAX by calling the URL `/admin/rest/forms-list/regexps`. The returned list is set to the attribute `regexps` and from it is used in the initialization of dialogues.

### Height of the editor

The height of the editor is calculated by the function `resizeEditor` at an interval of 3000 ms. The height is calculated according to the height of the browser window to maximize window usage. The upper and lower `margin` of the editor dialog and of course the size of the header and footer.
