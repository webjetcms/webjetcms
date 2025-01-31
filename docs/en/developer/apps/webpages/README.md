# Web pages

The web page section is specific in that it contains multiple datatables:
- `webpagesDatatable` - main datatable for the list of websites
- `groupsDatatable` - hidden (`display:none`) datatable for editing a folder entry (synchronized with the tree structure)
- `mediaTable` - displays the media attached to a web page in the page editor
- `historyTable` - displays historical records (changes) in the web page editor

## History of the website

The web page history is implemented as a nested datatable in the page editor. It is initialized in the class [DocEditorFields.java](../../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java) on the object `private List<DocHistoryDto> history;`.

### REST interface

The REST interface is available at the URL `/admin/rest/web-pages/history?docId={docId}&groupId={groupId}` and it is implemented in the class [DocHistoryRestController.java](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryRestController.java).

He uses [mapping](../../backend/mapstruct.md) facilities `DocDetails` to a simpler DTO object. Only basic data is displayed in the datatable.

Access to history is **read-only**, in the REST service, writing is disabled in the method `beforeDelete` a `beforeSave`.

### Frontend

The datatable is modified by listening to the event `WJ.DTE.innerTableInitialized`, the one called for each datatable, by verifying via `id` the code is executed only for the history table. The initialized object is assigned to a global variable `historyTable`.

```javascript
window.addEventListener("WJ.DTE.innerTableInitialized", function(event) {
    if (event.detail.conf.id=="DTE_Field_editorFields.history") {
        historyTable = event.detail.conf.datatable;
        historyTable.hideButton("create");
        ...
    }
});
```

After initialization, unnecessary buttons are hidden by calling `historyTable.hideButton`.

The complicated modification is in the way the history data is loaded into the editor. The editor needs to be fed JSON data loaded by the selected history ID. This is handled by the following edit button code:

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

Using an ajax request with the specified `historyId` the web page data is retrieved from the history. These are set in the datatable row by `docId` (which should be the currently edited line). With the new data, the editor is opened by calling the function `wjEdit` and then the original data stored in the object is returned to the datatable row `oldJson` (otherwise after closing the editor window the historical data would be displayed in the datatable of the web pages).

V `init` the option is set `showIfOneRowSelected` which button will only appear if exactly one row is selected.

The page preview and comparison buttons scroll through all selected rows and open a new page preview or comparison window according to the original functionality in WebJET 8. Page Compare always displays a comparison of the selected row against the current version (not a comparison of the selected rows).

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

## Optimising display speed

Since website management is a basic functionality of the CMS system, we tried to optimize its loading and display speed. For this reason, specific functions and code are used.

### Calling REST services

The JSON initialization data is embedded directly into the page via the class [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java). Thus, when the page is initially displayed, there is no need to wait for REST services to be called. In the page code they are used as:

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

`jstree` the object automatically searches `window.treeInitialJson` and use it instead of the URL you entered. The URLs entered for REST services will only be used for subsequent calls.

### Optimizing directory loading

A hidden datatable is used for editing the web page directory. Data is read by default for data `jstree` tree structure. These contain `original` the object in which the factually `GroupDetails`. This object is artificially inserted into the directory datatable in the `loadTablesForGroup` Like:

```javascript
if (groupsDatatable.rows().count()==0) {
    groupsDatatable.row.add(window.lastGroup);
} else {
    groupsDatatable.row(0).data(window.lastGroup);
}
groupsDatatable.draw();
```

so it does not need to call the REST service to retrieve the data. But this alone does not help, because the datatable does not have values for the selection fields, since the artificially inserted object does not contain this data.

Thus, loading is implemented as a call to refresh the datatable and then opening the edit/add record. Buttons for editing and adding a new record are displayed above the tree structure. Clicking on these buttons will trigger the setting of the variable `groupsDatatableClickButtonAfterXhr` to the value of the button to be called after data refresh (e.g. `"buttons-create"`). `groupsDatatable` has a set function `onXhr`, which verifies the state of the variable after reading the data `groupsDatatableClickButtonAfterXhr`. If there is no `null` so it triggers a click on the specified button.

The process is thus as follows:
- when clicking on a node in the tree structure, the object is set to the datatable and its new URL is set according to `groupId`
- when clicking on the Add or Edit folder button above the tree structure, the name of the button in the variable is remembered `groupsDatatableClickButtonAfterXhr` and the button will be called `buttons-refresh` in the datatable to retrieve data from the server
- after reading the data from the server, the state in the variable is checked `groupsDatatableClickButtonAfterXhr` and if it is not `null`, the specified button in the datatable is called (which opens the directory editor).

### Optimizing the loading of JavaScript files

Website used by `ckeditor` whose JavaScript file needs to be loaded. However, it is a relatively large file and its processing increases the load on the processor and consequently slows down the display of the web page. So this file is loaded asynchronously after 2 seconds after DOM initialization in the function `window.domReady.add`:

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

But the problem can be opening the page editor window before loading `ckeditor.js`, so it is treated directly in the window opening function where the object is tested `window.CKEDITOR` and if it does not exist, the initialization is delayed via the function `setTimeout`.

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

Display by specified `docid` URL parameter (or the specified `docid` via text field) uses the library [js-tree-auto-opener](../../libraries/js-tree-auto-opener.md). Auto-open obtains a list of directory IDs by calling the REST service `/admin/rest/web-pages/parents/{id}`. It then tries to open the obtained list of directory IDs (by successively calling the REST service to get the subdirectory data). After opening all the directories, it opens a web page for editing.

Input field for input `docid` in the page is implemented directly in [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) from the pre-prepared HTML code `#docIdInputWrapper`, which is moved to the datatable toolbar container. After pressing the `Enter` the action of opening the specified page ID is invoked.

The input field is connected to `autocomplete` search by page name or URL by setting `data-ac` Attributes.

!>**Warning:** the current implementation cannot open the web page on the other side of the list of web pages (as it cannot find it in the list). Also, if a `docid` in the System or Recycle Bin directory and this sheet is not currently displayed, it also fails to display the directory.

## View tab Last modified

Clicking the Last Modified tab loads a list of the most recently modified pages from all users into the datatable. Clicking on a name is handled in the `webpagesDatatable.onEdit`. Since the pages in the list can be in different directories, they are not opened directly, but their ID is entered in the field `Doc ID` and subsequently loaded including the tree structure.

Clicking on a leaf loads the contents of the datatable as if for the directory set in the configuration variable `systemPagesRecentPages` which is checked in class [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java) and if the directory ID matches it is called to retrieve the list of last pages.

## View Pending tab

Viewing the list in the pending tab is more complicated. The backend checks to see if there are pages for the currently logged in user to approve. If so it is set in the class [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java) attribute to the model `hasPagesToApprove` to the value of `TRUE`. This is tested in [web-pages-list.pug](../../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug) and if `FALSE` so the letter will be hidden.

When the tab is clicked, a directory with the ID set in the configuration variable is loaded into the datatable as if `systemPagesDocsToApprove` what is controlled in the classroom [WebpagesService](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesService.java). Loading is complicated by the fact that the value is used as a unique ID `docId`. However, there may be multiple entries for the same `docId`. As the simplest solution, we turned the value `docId` a `historyId` that are exchanged in the transmitted data.

The problem is calling data refresh after a click that calls `WebpagesRestController.getOne` where the value is sent `historyId` Like `id`. According to the sent `id` (which is in fact `historyId`) we get the correct value by querying the database `docId` to get the content according to the combination `docId, historyId`.

The click is served in the function `webpagesDatatable.onEdit` by opening a new window `/admin/approve.jsp` with approval dialogue.

For both the last modified and pending table, the add/edit/delete record buttons have been hidden. This is handled by listening to the tab switch event `$('#pills-pages a[data-toggle="pill"]').on('shown.bs.tab', function (e) {`.

## Page preview

Page preview is implemented by class `EditorPreview` in the file `src/js/pages/web-pages-list/preview.js`. Feature `window.previewPage` sets the attribute `isPreviewClicked` and makes an API call to save the editor. It also listens for an event `preSubmit` in which it detects the state of the attribute `isPreviewClicked`.

If it is set to `true` gets JSON data from the editor and stores it in the session object `session.setAttribute("ShowdocAction.showDocData", entity)` by calling `/admin/rest/web-pages/preview/`. It then opens a new tab in the browser with the address `/admin/webpages/preview/?docid=+jsonData.id` operated by the class `PreviewController.java`. It gets the DocDetails object from the session DocDetails `DocDetails doc = (DocDetails)request.getSession().getAttribute("ShowdocAction.showDocData");`, sets the necessary `request` objects and makes the call `/showdoc.do` to view the website. However, this does not get the current object from the database, but uses the object `request.getAttribute("ShowdocAction.showDocData")` to view the data.

At the same time listening to the event `webpagesDatatable.EDITOR.on('postSubmit'`, which updates the content of the preview window when the web page is saved (the entity is saved to `session` by calling `WebpagesRestController.afterSave`). This is only done if the preview window is still open, this is tested using `self.previewWindow.name != ""`. This will remain empty if the user closes the window.

The Preview button is inserted in the window footer at the event `webpagesDatatable.EDITOR.on('open'`.

## Shortcut key saving

Web page can be saved using a keyboard shortcut `CTRL+s/CMS+s`, which is initialized directly in the datatable by the function `bindKeyboardSave`. Technically, after pressing a keyboard shortcut, an event is triggered `WJ.DTE.save` that is listened to and this event performs a physical save (calling `EDITOR.submit`). When saving in this way, the window is left open so that the user can continue working.

But the problem with web pages is that the editing itself takes place in `iframe` element. So the event was not set correctly. The event needs to be added for both ckeditor and PageBuilder manually and the event needs to be fired in the parent framework.

V `/admin/v9/src/js/datatables-ckeditor.js afterInit` event is added:

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

similar for PageBuilder in `/admin/inline/inline.js.jsp`:

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

This initialization correctly sets the keystroke event to be listened to.
