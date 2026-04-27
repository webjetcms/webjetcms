# Website

The website section is specific in that it contains multiple data tables:

- ```webpagesDatatable``` - ​​main data table for the list of web pages
- ```groupsDatatable``` - ​​hidden (```display:none```) data table for editing folder items (synchronized with the tree structure)
- ```mediaTable``` - ​​displays media attached to a web page in the page editor
- ```historyTable``` - ​​displays historical records (changes) in the website editor

## Website history

The history of web pages is implemented as a nested data table in the page editor. It is initialized in the class [DocEditorFields.java](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) on the object ```private List<DocHistoryDto> history;```.

### REST interface

The REST interface is available at the URL ```/admin/rest/web-pages/history?docId={docId}&groupId={groupId}``` and is implemented in the class [DocHistoryRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryRestController.java).

It uses [mapping](../../backend/mapstruct.md) ```DocDetails``` objects to a simpler DTO object. Only basic data is displayed in the datatable.

Access to history is **read-only**, in the REST service, writing is disabled in the ```beforeDelete``` and ```beforeSave``` methods.

### Frontend

The datatable is modified by listening to the event ```WJ.DTE.innerTableInitialized```, which is called for each datatable, by verifying via ```id``` the code is executed only for the history table. The initialized object is assigned to the global variable ```historyTable```.

```javascript
window.addEventListener("WJ.DTE.innerTableInitialized", function(event) {
    if (event.detail.conf.id=="DTE_Field_editorFields.history") {
        historyTable = event.detail.conf.datatable;
        historyTable.hideButton("create");
        ...
    }
});
```

After initialization, unnecessary buttons are hidden by calling ```historyTable.hideButton```.

The complicated modification is in the way of loading data from the history into the editor. The editor needs to be provided with JSON data loaded according to the selected ID from the history. This is ensured by the following code of the edit button:

```javascript
historyTable.button().add(buttonCounter++, {
    text: '<i class="ti ti-pencil"></i>',
    action: function (e, dt, node) {
        //console.log("btn, e=",e,"dt=",dt,"node=",node);
        //ziskaj data selectnuteho riadku
        let selectedRows = dt.rows({ selected: true }).data();
        //console.log("selectedRows=", selectedRows);
        if (selectedRows.length>0) {
            let row = selectedRows[0];
            //HACK, potrebujeme ziskat dany zaznam ako JSON a podvrhnut ho do datatabulky
            $.ajax({
                url: "/admin/rest/web-pages/"+row.docId+"?historyId="+row.id,
                method: "GET",
                success: function(json) {
                    //console.log("Edit JSON", json);
                    let oldJson = webpagesDatatable.row("#"+row.docId).data();
                    webpagesDatatable.row("#"+row.docId).data(json);
                    webpagesDatatable.EDITOR.setJson(json);
                    webpagesDatatable.wjEdit(webpagesDatatable.row("#"+row.docId));
                    setTimeout(function() {
                        //console.log("returning oldJson=", oldJson);
                        webpagesDatatable.row("#"+row.docId).data(oldJson);
                    }, 100);
                }
            })
        }
    },
    init: function ( dt, node, config ) {
        $.fn.dataTable.Buttons.showIfOneRowSelected(this, dt);
    },
    className: 'btn btn-warning buttons-divider',
    attr: {
        'title': '[[\#{history.editPage}]]',
        'data-toggle': 'tooltip'
    }
});
```

Using an ajax request with the specified ```historyId```, the web page data from the history is obtained. These are set in the data table of the row according to ```docId``` (which should be the currently edited row). With the new data, the editor is opened by calling the function ```wjEdit``` and then the original data stored in the object ```oldJson``` is returned to the data table row (otherwise, after closing the editor window, the historical data would be displayed in the web page data table).

In ```init```, the option ```showIfOneRowSelected``` is set which will display the button only if exactly one row is selected.

The buttons for displaying a page preview and comparing go through all selected rows and open a new window to preview or compare the page according to the original functionality in WebJET 8. Comparing pages always displays a comparison of the selected row against the current version (not a comparison of selected rows).

```javascript
action: function (e, dt, node) {
    //console.log("btn, e=",e,"dt=",dt,"node=",node);
    //ziskaj data selectnuteho riadku
    let selectedRows = dt.rows({ selected: true }).data();
    console.log("selectedRows=", selectedRows);
    for (let i=0; i<selectedRows.length; i++) {
        let row = selectedRows[i];
        window.open("/showdoc.do?docid="+row.docId+"&historyid="+row.id);
    }
}
```

## Display speed optimization

Since website management is a core functionality of the CMS system, we tried to optimize its loading and display speed. For this reason, specific functions and code are used.

### Calling REST services

Initialization JSON data is inserted directly into the page via the [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java) class. Therefore, when the page is first displayed, there is no need to wait for the REST service to be called. In the page code, they will be used as:

```javascript
window.treeInitialJson = [(${treeInitialJson})];
let groupsInitialJson = [(${groupsInitialJson})];
let webpagesInitialJson = [(${webpagesInitialJson})];
...
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    initialData: webpagesInitialJson,
}
...
groupsDatatable = WJ.DataTable({
    id: 'groups-datatable',
    url: groupsUrl,
    initialData: groupsInitialJson,
}
```

```jstree``` objekt automaticky hľadá ```window.treeInitialJson``` a použije ho namiesto zadanej URL adresy. Zadané URL adresy pre REST služby sa použijú až pre ďalšie volania.

### Optimalizácia načítania adresára

Pre editáciu adresára web stránky sa využíva schovaná datatabuľka. Údaje sa štandardne čítajú pre dáta ```jstree``` stromovej štruktúry. Tie obsahujú ```original``` objekt, v ktorom je fakticky ```GroupDetails```. Tento objekt sa umelo vkladá do datatabuľky adresára vo funkcii ```loadTablesForGroup``` ako:

```javascript
if (groupsDatatable.rows().count()==0) {
    groupsDatatable.row.add(window.lastGroup);
} else {
    groupsDatatable.row(0).data(window.lastGroup);
}
groupsDatatable.draw();
```

so there is no need to call the REST service to load the data. However, this alone does not help, because the data table does not have values ​​for the selection fields, since the artificially inserted object does not contain this data.

Loading is therefore implemented as a call to refresh the data table and then opening the edit/add record. The buttons for editing and adding a new record are displayed above the tree structure. Clicking on these buttons causes the variable ```groupsDatatableClickButtonAfterXhr``` to be set to the value of the button to be called after data refresh (e.g. ```"buttons-create"```). ```groupsDatatable``` has a function ```onXhr``` set, which verifies the state of the variable ```groupsDatatableClickButtonAfterXhr``` after loading the data. If ```null``` is not present, it causes the specified button to be clicked.

So the process is as follows:

- when clicking on a node in the tree structure, the object is set to the data table and its new URL is set according to ```groupId```
- when clicking the Add or Edit folder button above the tree structure, the button name is remembered in the variable ```groupsDatatableClickButtonAfterXhr``` and the button ```buttons-refresh``` in the data table is called to load data from the server
- after loading data from the server, the status in the variable ```groupsDatatableClickButtonAfterXhr``` is checked and if it is not ```null```, the specified button in the data table is called (which opens the directory editor).

### Optimizing JavaScript file loading

The web pages use ```ckeditor```, whose JavaScript file needs to be loaded. However, it is a relatively large file and its processing increases the processor load and consequently slows down the display of the web page. So this file is loaded asynchronously 2 seconds after DOM initialization in the ```window.domReady.add``` function:

```javascript
setTimeout(()=> {
    //nechceme zatazit CPU hned na zaciatku, takze ckeditor.js nacitame cez timenout mierne neskor
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = "/admin/skins/webjet8/ckeditor/dist/ckeditor.js";

    head.appendChild(script);
}, 2000);
```

However, the problem may subsequently be opening the page editor window before loading ```ckeditor.js```, so this is handled directly in the window opening function, where the ```window.CKEDITOR``` object is tested and if it does not exist, the initialization is delayed through the ```setTimeout``` function.

```javascript
webpagesDatatable.EDITOR.on( 'open', function ( e, type ) {
    ...
    if (typeof window.CKEDITOR == "undefined") {
        //este nedobehol load ckeditora
        setTimeout(() => {
            if (typeof window.CKEDITOR != "undefined") wjeditor = new module.DatatablesCkEditor(options);
        }, 2100);
    } else {
        wjeditor = new module.DatatablesCkEditor(options);
    }
    ...
}
```

## Display by docid parameter

The display according to the specified ```docid``` URL parameter (or specified ```docid``` via the text field) uses the [js-tree-auto-opener](../../libraries/js-tree-auto-opener.md) library. Automatic opening obtains a list of directory IDs by calling the REST service ```/admin/rest/web-pages/parents/{id}```. The obtained list of directory IDs is then tried to be opened (by successively calling the REST service to obtain subdirectory data). After opening all directories, it opens the web page for editing.

The input field for entering ```docid``` in the page is implemented directly in [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) from the pre-prepared HTML code ```#docIdInputWrapper```, which is moved to the datatable toolbar container. After pressing the ```Enter``` key, the action of opening the specified page ID is invoked.

The input field is connected to ```autocomplete``` search by page name or its URL address by setting ```data-ac``` attributes.

!>**Warning:** The current implementation cannot open a web page on the second page of the web page list (because it cannot find it in the list). Also, if ```docid``` is entered in the System or Trash directory and this list is not currently displayed, it cannot display the directory either.

## View the Last Modified tab

After clicking on the Last modified tab, a list of the most recently modified pages by all users is loaded into the data table. Clicking on a name is processed in the ```webpagesDatatable.onEdit``` function. Since the pages in the list can be in different directories, they are not opened directly, but their ID is entered in the ```Doc ID``` field and then loaded, including the tree structure.

Clicking on the sheet loads the contents of the datatable as if for the directory set in the configuration variable ```systemPagesRecentPages```, which is checked in the [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java) class and if the directory ID matches, loading the list of recent pages is called.

## View the Pending Approval tab

Displaying a list in a tab awaiting approval is more complicated. The backend checks whether there are pages for the currently logged in user that need to be approved. If so, the attribute ```hasPagesToApprove``` is set to the value ```TRUE``` in the [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java) class in the model. This is tested in [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) and if it is ```FALSE``` then the list is hidden.

After clicking on the tab, a directory with the ID set in the configuration variable ```systemPagesDocsToApprove``` is loaded into the data table, as if it were a directory, which is controlled in the class [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java). Loading is complicated by the fact that the value ```docId``` is used as a unique ID. However, for approval purposes, there may be multiple records for the same ```docId```. As the simplest solution, we reversed the value ```docId``` and ```historyId```, which are exchanged in the transmitted data.

The problem is the call to refresh data after clicking, which calls ```WebpagesRestController.getOne```, where the value ```historyId``` is sent as ```id```. According to the sent ```id``` (which is actually ```historyId```), we query the database to get the correct value ```docId``` to get the content according to the combination ```docId, historyId```.

The click is handled in the ```webpagesDatatable.onEdit``` function by opening a new window ```/admin/approve.jsp``` with an approval dialog.

For the table "Last modified" and "Pending approval" the buttons for adding/editing/deleting a record are hidden. This is handled by listening to the tab switch event ```$('#pills-pages a[data-toggle="pill"]').on('shown.bs.tab', function (e) {```.

## Page preview

The page preview is implemented by the ```EditorPreview``` class in the ```src/js/pages/web-pages-list/preview.js``` file. The ```window.previewPage``` function sets the ```isPreviewClicked``` attribute and makes an API call to save the editor. It also listens for the ```preSubmit``` event to detect the state of the ```isPreviewClicked``` attribute.

If set to ```true```, it gets JSON data from the editor and stores it in the session object ```session.setAttribute("ShowdocAction.showDocData", entity)``` by calling ```/admin/rest/web-pages/preview/```. It then opens a new tab in the browser with the address ```/admin/webpages/preview/?docid=+jsonData.id```, which is handled by the ```PreviewController.java``` class. This gets the ```DocDetails doc = (DocDetails)request.getSession().getAttribute("ShowdocAction.showDocData");``` object from the DocDetails session, sets the necessary ```request``` objects, and calls ```/showdoc.do``` to display the web page. However, this does not get the current object from the database, but uses the ```request.getAttribute("ShowdocAction.showDocData")``` object to display the data.

It also listens for the ```webpagesDatatable.EDITOR.on('postSubmit'``` event, which updates the preview window content when the web page is saved (the entity is saved to ```session``` by calling ```WebpagesRestController.afterSave```). This is only done if the preview window is still open, which is tested with ```self.previewWindow.name != ""```. This will remain empty if the user closes the window.

The Preview button is inserted into the window footer at the ```webpagesDatatable.EDITOR.on('open'``` event.

## Save with a keyboard shortcut

A web page can be saved using the hotkey ```CTRL+s/CMS+s```, which is initialized directly in the datatable by the function ```bindKeyboardSave```. Technically, after pressing the hotkey, the event ```WJ.DTE.save``` is called, which is listened to, and this event performs the physical save (calling ```EDITOR.submit```). When saving in this way, the window is left open so that the user can continue working.

However, with web pages, the problem was that the editing itself takes place in the ```iframe``` element. So the event was not set correctly. The event needs to be added for both ckeditor and PageBuilder manually and the event must be triggered in the parent frame.

An event is added in ```/admin/v9/src/js/datatables-ckeditor.js afterInit```:

```javascript
//zachytenie CTRL+S/CMD+S
var editor = this.ckEditorInstance;
editor.on( 'contentDom', function(e) {
    var editable = editor.editable();
    //console.log("Som contentDown");
    editable.attachListener( editable, "keydown", function(evt) {
        //console.log("keydown, evt=", evt);
        var keyEvent = evt.data.$;
        if ((window.navigator.platform.match("Mac") ? keyEvent.metaKey : keyEvent.ctrlKey)  && keyEvent.key === 's') {
            //console.log("IFRAME CTRL+S, evt=", evt);

            keyEvent.preventDefault();
            try {
                window.top.WJ.dispatchEvent("WJ.DTE.save", {});
            } catch (ex) {}
        }
    });
});
```

similarly for PageBuilder in ```/admin/inline/inline.js.jsp```:

```javascript
$(document).ready(function() {
	document.addEventListener("keydown", function(e) {
		//zachytenie CTRL+S/CMD+S
		if ((window.navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)  && e.key === 's') {
			e.preventDefault();
			//console.log("Dispatching WJ.DTE.save");
			try {
				window.top.WJ.dispatchEvent("WJ.DTE.save", {});
			} catch (ex) {}
		}
	}, false);
});
```

Such initialization correctly sets up listening for the hotkey press event.