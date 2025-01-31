# DatatableOpener

Class `DatatableOpener` ensures that the ID of the currently opened record is displayed in the URL address of the browser (parameter `id`), displaying the ID in the datatable header and searching for the specified ID in the datatable header. The class is implemented according to the class [AbstractJsTreeOpener](js-tree-document-opener.md).

![](datatable-opener.png)

The class is initialized directly in `index.js` DATATables. The opener can be disabled by setting the configuration parameter `idAutoOpener: false`.

```javascript
...
import {DatatableOpener} from "../../src/js/libs/data-tables-extends/";
...
DATA.idAutoOpener = (typeof options.idAutoOpener !== "undefined") ? options.idAutoOpener : true;
...

//inicializacia openera
if (DATA.idAutoOpener===true) {
    //inicializuj DT opener
    TABLE.datatableOpener = new DatatableOpener();
    TABLE.datatableOpener.dataTable = TABLE;
    TABLE.datatableOpener.init();
}

//ukazka vypnutia openera nastavenim idAutoOpener: false
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    columns: webpageColumns,
    tabs: tabs,
    editorId: "docId",
    idAutoOpener: false
});
```

The ID value is set in the URL parameter only while the editor is open, after the editor is closed the parameter is removed from the URL `id` erases. Such behaviour seems to us more natural and representative of the current state of affairs.

After initialization in the datatable header, the class creates a function call `bindInput` input field for entering the ID in which the key is waiting to be pressed `Enter`. It then sets the specified value to the attribute `this.id` and will trigger `this.dataTable.draw();` to start the process of displaying the editor, similar to initializing from a URL parameter.

## Setting the ID in the URL

The class after opening the editor (using the event `this.dataTable.EDITOR.on( 'open', ( e, type ) => {`) gets the current JSON object `this.dataTable.EDITOR.currentJson` from which the ID gets the value according to the column in `this.dataTable.DATA.editorId` (the ID is not always the value in the id column, it can be e.g. `insertScriptId`). It sets the obtained value by calling `setInputValue` in the input field and in the URL parameter id.

## Open the editor based on the URL parameter

After initialization in `index.js` sets the value from the URL parameter to the internal objects. The event is listened to `this.dataTable.on('draw.dt', (evt, settings) => {`, i.e. rendering the table. From it, the corresponding row is retrieved based on the ID and the function to open the editor is invoked `this.dataTable.wjEditFetch($('.datatableInit tr[id=' + id + ']'));`.

## Search for a specified ID

The problem with opening the editor is the condition where the specified ID is not on the currently displayed datatable page. Here we also need to distinguish between server and client paging state. By calling `const idIndex = Object.keys(settings.aIds).indexOf(id.toString());` the ordinal index in the current data for the specified id is obtained. At the same time, the page on which the record should be located is calculated by calculating `const pageNumber = info.length < 0 ? 0 : Math.floor(idIndex / info.length);`.

If it is the current page (or the record was found in the data during server paging), the editor view is called by calling `this.dataTable.wjEditFetch($('.datatableInit tr[id=' + id + ']'));`.

If the record is on a different page, the display of that page is invoked by calling `setTimeout(() => this.dataTable.page(pageNumber).draw('page'), 500);`.

However, with server-side paging, we cannot easily determine on the client side the page on which the record is located. It is therefore triggered by a sequential paging of the data by calling the server's REST service. To avoid overwhelm, paging is called via the function `setTimeout` at 500ms interval. To avoid looping, the server call in the atrium is counted `failsafeCounter`, where the limit of 30 calls is set. **The search will therefore find the entered ID in the first 30 pages at most**.

In the future, we are considering implementing page retrieval in the server-side REST service, which would eliminate the problem of incremental paging of data on the client side.

## Filter by hash parameters

The library also provides the ability to filter the table according to the parameters specified in the hash expression, e.g. `/admin/v9/users/user-list/#dt-filter-id=3`. Parameters entered in `window.location.hash` beginning with `dt-filter-` are set to the appropriate filter fields in the header after table initialization. Subsequently, a click on the search icon next to the first field is performed.

If there is also a value in the hash expression `dt-select=true`, so after the records are loaded, the rows are marked. Thus, it is easy to perform an action like user approval by clicking a button and so on.

If there is also a value in the hash expression `dt-open-editor=true` the editor is opened after marking the rows (the rows are automatically marked as well, no parameter is needed `dt-select=true`).

The implementation is in function `filterTableByHashParameters` that is triggered by an event `this.dataTable.one('draw.dt', (evt, settings) => {`.
