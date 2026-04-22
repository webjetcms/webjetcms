# DatatableOpener

The ```DatatableOpener``` class provides for displaying the ID of the currently opened record in the browser URL (parameter ```id```), displaying the ID in the datatable header, and searching for the specified ID in the datatable header. The class is implemented according to the [AbstractJsTreeOpener](js-tree-document-opener.md) class.

![](datatable-opener.png)

The class is initialized directly in the ```index.js``` datatable. The Opener can be disabled by setting the ```idAutoOpener: false``` configuration parameter.

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

The ID value is set to the URL parameter only while the editor is open, after it is closed the parameter ```id``` is deleted from the URL address. This behavior seems more natural and describes the current state.

After initialization in the datatable header, the class creates an input field for entering the ID by calling the function ```bindInput```, in which it waits for the key ```Enter``` to be pressed. It then sets the entered value to the attribute ```this.id``` and calls ```this.dataTable.draw();``` to start the editor display process, similar to initialization from the URL parameter.

## Setting the ID in the URL address

After opening the editor (using the ```this.dataTable.EDITOR.on( 'open', ( e, type ) => {``` event), the class obtains the current JSON object ```this.dataTable.EDITOR.currentJson``` from which it obtains the ID value according to the column in ```this.dataTable.DATA.editorId``` (the ID value is not always in the id column, it can be, for example, ```insertScriptId```). It sets the obtained value by calling ```setInputValue``` to the input field and to the URL parameter id.

## Opening the editor based on a URL parameter

After initialization in ```index.js```, the value from the URL parameter is set to internal objects. The event ```this.dataTable.on('draw.dt', (evt, settings) => {```, i.e. the rendering of the table, is listened to. The corresponding row is obtained from it based on the ID and the editor opening function ```this.dataTable.wjEditFetch($('.datatableInit tr[id=' + id + ']'));``` is called.

## Searching for the specified ID

The problem with opening the editor is when the specified ID is not on the currently displayed page of the data table. Here we also have to distinguish between the server and client paging states. Calling ```const idIndex = Object.keys(settings.aIds).indexOf(id.toString());``` will get the ordinal index in the current data for the specified ID. At the same time, the page on which the record should be located is calculated by calculating ```const pageNumber = info.length < 0 ? 0 : Math.floor(idIndex / info.length);```.

If it is the current page (or the record was found in the data during server paging), the editor display is invoked by calling ```this.dataTable.wjEditFetch($('.datatableInit tr[id=' + id + ']'));```.

If the record is on another page, the display of that page is invoked by calling ```setTimeout(() => this.dataTable.page(pageNumber).draw('page'), 500);```.

However, with server paging, we cannot simply determine on the client side the page on which the record is located. Therefore, sequential paging of data is started by calling the server's REST service. To avoid congestion, paging is called via the ```setTimeout``` function at 500ms intervals. To avoid looping, the server call is counted in the ```failsafeCounter``` attribute, where a limit of 30 calls is set. **The search therefore finds the specified ID in the first 30 pages at most**.

In the future, we are considering implementing page retrieval in the server REST service, which would eliminate the problem of sequential paging of data on the client side.

## Filtering by hash parameters

The library also provides the ability to filter the table according to the parameters specified in the hash expression, e.g. `/admin/v9/users/user-list/#dt-filter-id=3`. The parameters specified in `window.location.hash` starting with `dt-filter-` are set to the appropriate filter fields in the header after the table is initialized. Then the search icon is clicked next to the first field.

If the hash expression also contains the value `dt-select=true`, then after loading the records, the rows are marked. This means that it is easy to perform an action such as user approval by clicking a button, etc.

If the hash expression also contains the value `dt-open-editor=true`, the editor will also open after marking the lines (the lines will also be marked automatically, the `dt-select=true` parameter is not required).

The implementation is in function `filterTableByHashParameters`, which is called on event `this.dataTable.one('draw.dt', (evt, settings) => {`.