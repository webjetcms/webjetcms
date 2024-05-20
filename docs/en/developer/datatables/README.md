# Datatables

Library [datatables.net](http://datatables.net) is an advanced table with a connection to REST services.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Datatables](#datatables)
	- [Basic initialization in cooperation with Spring REST](#základná-inicializácia-v-spolupráca-s-spring-rest)
	- [Configuration options](#možnosti-konfigurácie)
		- [Setting columns](#nastavenie-stĺpcov)
		- [HTML code display](#zobrazenie-html-kódu)
	- [Add/remove buttons](#pridanieodobratie-tlačidiel)
	- [Button to perform server action](#tlačidlo-pre-vykonanie-serverovej-akcie)
	- [Buttons by rights](#tlačidlá-podľa-práv)
	- [Line styling](#štýlovanie-riadku)
	- [Status icons](#stavové-ikony)
	- [View data based on rights](#zobrazenie-dát-na-základe-práv)
	- [Arrangement](#usporiadanie)
	- [Search](#vyhľadávania)
	- [External filter](#externý-filter)
	- [Export/import](#exportimport)
	- [API calls](#api-volania)
	- [Code samples](#ukážky-kódu)
		- [Listening for a table refresh event:](#počúvanie-udalosti-obnovenia-tabuľky)
<!-- /code_chunk_output -->

## Basic initialization in cooperation with Spring REST

The WebJET implementation of datatables is configured using the JSON object columns. This object contains the column definitions for both the datatable and the datatables editor. The WJ.DataTable constructor is then used to initialize the table.

But we recommend to always generate a columns object from [Java entity annotations](../datatables-editor/datatable-columns.md).

Basic example:

```javascript
script.
    let galleryTable = null;
    window.domReady.add(function () {

        //URL adresa REST sluzby
        let url = "/admin/rest/components/gallery";

        //definicia stlpcov
        let columns = [
            {
                data: "id",
                name: "id",
                title: "ID",
                defaultContent: '',
                className: 'dt-select-td',
                renderFormat: "dt-format-selector"
            },
            {
                data: "imageName",
                name: "",
                title: "",
                render: function ( data, type, row ) {
                    //specialna render funkcia pre zobrazenie obrazka galerie
                    return '<div class="img" style="background-image:url(' + row.imagePath + '/' + data +');"></div>';
                },
                className: "dt-image",
                renderFormat: "dt-format-none"
            },
            {
                data: "imageName",
                name: "imageName",
                title: "Nazov suboru",
                renderFormat: "dt-format-text",
                renderFormatLinkTemplate: "javascript:;",
                renderFormatPrefix: '<i class="far fa-pencil"></i> ',
                className: "dt-row-edit",

                editor: {
                    type:  "text"
                }
            },
            {
                data: "imagePath",
                name: "imagePath",
                title: "Adresar",
                renderFormat: "dt-format-text",

                editor: {
                    type:  "text",
                    attr: {
                        "data-dt-field-hr": "after"
                    }
                }
            }
        ];

        /*
        options pre DataTabulku
        {
            src: zdrojove data (objekt)
            url: URL adresa rest sevisu
            serverSide: ak je nastavene na true bude sa vyhladavanie/sortovanie/strankovanie posielat na server
            columns: definicia stlpcov
            tabs: definicia zaloziek pre Editor
            hideTable: boolean po nastaveni na true sa datatabulka nezobrazi
            noAll: boolean, po nastaveni na true sa nebude k url pridavat /all pre ziskanie vsetkych zaznamov
        }
        */
        galleryTable = WJ.DataTable( {
            url: url,
            serverSide: true,
            columns: columns,
        });

    });


<table class="datatableInit table cardView cardViewS"></table>
```

## Configuration options

```javascript
WJ.DataTable({
	Options,
});
```

Configuration options (`options`):

Minimum configuration:
- `url {string}` The URL of the REST service endpoint for retrieving the data. To this URL, the datatable adds `/all` to retrieve all data (unless the noAll option is set), `findByColumns` for searching, possibly `/editor` for data storage.
- `columns {json}` definition of columns, ideally from [Java annotations](../datatables-editor/datatable-columns.md).
**Other options:**
- `serverSide {boolean}` at the value of `true` expects to use paging and ordering on the server by calling REST services, at a value of `false` performs paging and ordering locally over the initially retrieved data.
- `tabs {json}` Definition [cards for the editor](../datatables-editor/README.md#karty-v-editore).
- `id {string}` the unique identifier of the datatable, if not specified the value is used `datatableInit`. Especially needed if you have multiple datatables in one web page.
- `editorId {string}` the unique identifier of the editor, if not specified the value is used `id`. Especially needed if you have multiple datatables in one web page.
- `onXhr {function}` JavaScript function that is called after [loading data](https://datatables.net/reference/event/xhr) in the form of `function ( TABLE, e, settings, json, xhr ) {}`.
- `onPreXhr(TABLE, e, settings, data) {function}` JavaScript function that is called [before loading data](https://datatables.net/reference/event/preXhr), allows you to add parameters to the sent data. These are entered with a prefix `fixed_` to distinguish them from standard datatable parameters. Example: `onPreXhr: function(TABLE, e, settings, data) { data.fixed_searchFilterBotsOut = $('#botFilterOut').is(':checked'); }`.
- `onEdit(TABLE, row, dataAfterFetch, dataBeforeFetch) {function}`: JavaScript function that is called when the record edit link is clicked. It receives as parameters: `TABLE` - datatable instance, `row` - jQuery object of the row that was clicked, `dataAfterFetch` - when the function is switched on `fetchOnEdit` json data retrieved after restoration, `dataBeforeFetch` the original JSON row data for the call to restore it. You can then open the standard editor by calling `TABLE.wjEdit(row);`. An example of use is in [web-pages-list.pug](../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug).
- `fetchOnCreate {boolean}` when set to true, a REST call with value -1 will be made to get the data of the new object before the new record is created. The values are set by calling `EDITOR.setJson(json)` implemented in `$.fn.dataTable.Editor.prototype.setJson` in the event `initCreate`.
- `fetchOnEdit {boolean}` when set to true, a REST call will be made before editing the record to retrieve the current data of the record being edited. When using a datatable, for example for a web page, the record is updated from the server before the editor is opened, so the latest version is always opened in the editor. Implemented via JS function `refreshRow` and customer button `$.fn.dataTable.ext.buttons.editRefresh` to replace the standard button `edit`.
- `idAutoOpener {boolean}` allows setting to `false` deactivate [automatic opening of the editor](../libraries/datatable-opener.md) by URL parameter and inserting the ID field into the table header.
- `hideTable {boolean}` after setting to `true` the datatable will not appear in the page (it will be hidden).
- `jsonField {function}` definition for array [of type json](../datatables-editor/field-json.md#použitie-špecifických-json-objektov).
- `order {array}` Default [method of arrangement](#usporiadanie) tables.
- `paging {boolean}` false disables datatable pagination (all returned data from the server will be displayed, the option to set the page size will not be displayed).
- `nestedModal {boolean}` if set to true, it is a datatable inserted as a field in the editor - [nested datatable](../datatables-editor/field-datatable.md), the nested table has an added CSS class `DTE_nested_modal`.
- `noAll {boolean}` by default adds to the set url `/all` to get all the data, by setting `noAll` at `false` with `/all` will not be added, but the search will not work either.
- `initialData {variable}` data for the initial view (without the need to call the REST service), see the documentation for [optimising display speed](../apps/webpages/README.md) list of websites. Technically, if this object is set up, the first time the REST service is called, the specified data is used.
- `initialData.forceData {boolean}` after setting to `true` the initial data is used regardless of its size, it is typically used when the initial data is an empty object because it is subsequently retrieved by some other means. To retrieve empty data, you can use the function `initialData: dtWJ.getEmptyData(true)`.
- `hideButtons {string}` comma-separated list of button names to be automatically hidden (not displayed) in the datatable, e.g. `create,edit,duplicate,remove,import,celledit`.
- `removeColumns {string}` comma-separated list of columns that should not be displayed, even if they are in the definition (for example, if you are displaying the datatable in multiple places and don't need all the columns). For example. `whenToPublish,datePublished`.
- `forceVisibleColumns` comma separated list of columns that will be displayed (overwrites the columns set by the user), typically used in a nested datatable where only some columns need to be displayed.
- `updateColumnsFunction` the name of the JavaScript function that will be used to edit the column list. It is typically used in a nested datatable where the displayed columns need to be added/edited (see sample below).
- `perms` sets [rights to view buttons](#tlačidlá-podľa-práv) to add, edit, duplicate and delete data
- `lastExportColumnName` if specified, displays in the export dialog the option to export data not yet exported (used in forms). The value represents the name of the column to be added as `NULL` condition to data selection (needs to be implemented correctly in the REST service).
- `byIdExportColumnName` if specified in the export dialog, enables export according to the selected rows. The value is the name of a column in the database with an ID value (typically id, used in forms). Filtering needs to be implemented as `predicates.add(root.get("id").in(idsList));` in the REST service.
- `editorButtons` an array of buttons that appear in the editor. Example `editorButtons: [ {title: "Uložiť", action: function() { this.submit(); } }, { title: ...} ]`. It uses the API for Datatables Editor.
- `keyboardSave {boolean}` - by setting it to `false` disable the option to save an entry in the editor with a keyboard shortcut `CTRL+S/CMS+S`.
- `stateSave {boolean}` - by setting it to `false` disable the ability to remember column order and table layout in the browser.
- `customFieldsUpdateColumns {boolean}` - by setting it to `true` shall be in obtaining [optional fields](../datatables-editor/customfields.md) also update the column names in the table and in the settings of the displayed columns (by default at the value `false` optional field names are updated only in the editor).
- `customFieldsUpdateColumnsPreserveVisibility {boolean}` - by setting it to `true` the column display setting for the mode is preserved for the user `customFieldsUpdateColumns`. It can only be used when the columns for the datatable are not changed during display. For example, in the Translation Keys section, the data is not changed, it can be set to `true`, but in the Dialers section the columns are also changed when changing the dialer, this option is not applicable there.
- `autoHeight {boolean}` - by default, the table calculates its height to make the most of the window space. By setting it to `false` the table will have the height according to the content (number of rows).
```javascript
let columns = [
    {
        data: "audit",
        name: "audit",
        title: "Auditované",

        renderFormat:   "dt-format-checkbox"
                        "dt-format-selector"
                        "dt-format-text"
                        "dt-format-text-wrap"
                        "dt-format-none"
                        "dt-format-date-time"
                        "dt-format-select" //moznosti bere z editor: { options: }

        renderFormatLinkTemplate:   "javascript:;",
                                     "/temps-list.html"
        renderFormatPrefix: '<i class="far fa-pencil"></i> ',
        renderHideValue: false, //TODO ???
        render: function ( data, type, row ) {
            //console.log("data", data, "type", type, "row", row);
            return '<div class="img" style="background-image:url(' + row.imagePath + '/' + data +');"></div>';
        },

        className: "dt-image",

        defaultContent: '',

        perms: "multiDomain" //stĺpec sa zobrazí len ak používateľ má právo multiDomain

    }
];

//Ukazka pouzitia updateColumnsFunction
//@DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"),
    function updateColumnsGroupDetails(columns) {
        //doplnenie kliknutia na stlpec fullPath
        WJ.DataTable.mergeColumns(columns, {
            name: "fullPath",
            renderFormatLinkTemplate: "javascript:openGroupDetails({{groupId}})"
        });
    }
    function openGroupDetails(groupId) {
        window.open("/admin/v9/webpages/web-pages-list/?groupid="+groupId);
    }
```

**Initiating a search:**

There may be cases when you need to initialize a (remembered) lookup immediately when displaying a table. This is used in the Statistics application, which remembers a range of set from-to dates. The search criteria are already applied the first time the REST service is called. The option is set by a JSON object in `options.defaultSearch`. It contains a list of selectors with a value to be applied to the filter before the first call to the REST service, e.g.:

```json
{
	".dt-filter-from-dayDate": "06.06.2022",
	".dt-filter-to-dayDate": "22.08.2022"
}
```

Example of use with memorization in `sessionStorage` browser:

```javascript
//inicializacia datatabulky
errorDataTable = WJ.DataTable({
	url: url,
	serverSide: false, //false lebo sa nevyužíva repositár
	columns: columns,
	id: "errorDataTable",
	idAutoOpener: false,
	defaultSearch: ChartTools.getSearchCriteria(),
	onPreXhr: function (TABLE, e, settings, data) {
		//console.log('onPreXhr, url=', $('#searchUrl').val());
		data.fixed_searchurl = $("#searchUrl").val();
	},
});
//Onchange events - update table
$("#errorDataTable_extfilter").on("click", "button.filtrujem", function () {
	//reload table values
	ChartTools.saveSearchCriteria(errorDataTable.DATA);
	errorDataTable.ajax.reload();
});

//appModule
/**
 * Save last search criteria to session storage, so all stats page will have same criteria when loaded
 * @param {*} DATA
 */
export function saveSearchCriteria(DATA) {
	var inputs = [".dt-filter-from-dayDate", ".dt-filter-to-dayDate", "#rootDir", "#botFilterOut", "#searchUrl", ".dt-filter-lastLogon"];
	var defaultSearch = {};

	for (const name of inputs) {
		var value = $("#" + DATA.id + "_extfilter " + name).val();
		if ("true" === value) {
			//it's checkbox
			value = $("#" + DATA.id + "_extfilter " + name).is(":checked");
		}
		if (value != "" && value != "-1" && value != "false") defaultSearch[name] = value;
	}
	var json = JSON.stringify(defaultSearch);
	if (json != "{}") window.sessionStorage.setItem("webjet.apps.stat.filter", json);
	else window.sessionStorage.removeItem("webjet.apps.stat.filter");
}

/**
 * Gets saved search criteria from session storage
 * @returns
 */
export function getSearchCriteria() {
	var defaultSearch = window.sessionStorage.getItem("webjet.apps.stat.filter");
	if ("{}" == defaultSearch) defaultSearch = null;
	if (defaultSearch != null) {
		defaultSearch = JSON.parse(defaultSearch);
		for (const property in defaultSearch) {
			var value = defaultSearch[property];
			if (property == "#rootDir") {
				var $property = $(property);
				$property.val(value);
				$property.selectpicker("val", value);
			}
			if (property == "#botFilterOut") {
				$("#botFilterOut").prop("checked", value);
			}
			if (property == "#searchUrl") {
				$("#searchUrl").val(value);
			}
		}
	}
	return defaultSearch;
}
```

### Setting columns

`renderFormat`:
- `dt-format-selector` - check box to mark the row, it should be as the first column
- `dt-format-none` - the column will have no options in the header
- `dt-format-text, dt-format-text-wrap` - standard text, `escapuje` HTML code
- `dt-format-select` - selection field
- `dt-format-checkbox` - HTML type `checkbox`
- `dt-format-boolean-true, dt-format-boolean-yes, dt-format-boolean-one` - `true/false` options
- `dt-format-number, dt-format-percentage` - display numbers
- `dt-format-number--decimal, dt-format-percentage--decimal`
- `dt-format-number--text` - displays the rounded number, for a higher number it prints in text form, e.g. `10 tis.` instead of `10000`
- `dt-format-date, dt-format-date-time, dt-format-date--text, dt-format-date-time--text` - date/time, the filter displays from-to
- `dt-format-link` - displays text as a link, option to use `renderFormatLinkTemplate`
- `dt-format-image` - displays a small preview of the image and a link to its full view, below the image is the text of the link to the image.
- `dt-format-mail` - display text as email link
- `dt-row-edit` - allows line editing

If you need the column to have a specific (maximum) width, you need to set this using CSS on both rows in the header using the CSS style `max-width`. Example:

```css
.datatableInit {
	thead tr {
		th.dt-th-editorFields-statusIcons {
			width: 75px;
			max-width: 75px;
		}
	}
}
```

Settings `max-width` ensures column width adjustment. The datatable will calculate the remaining widths. Caution, if the text exceeds the specified width will delay the other columns in the table itself, then the width of the header and the table do not match, it is necessary to adjust the width of the table on the given cell if necessary `overflow` Property. You can add the necessary CSS style to the cell by setting the attribute `className` in the annotation.

### HTML code display

Datatable by default `escapuje` HTML characters into entities to prevent unwanted execution of HTML code. If you need to display HTML code in a cell, it is possible to set in the annotation attribute `className` CSS style `allow-html`, which enables the execution of HTML code in the cell. However, be careful about using this so that an XSS type error cannot occur.

```java
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="[[#{admin.conf_editor.value}]]",
        className = "allow-html"
    )
    private String value;
```

## Add/remove buttons

Via API it is possible in `toolbare` remove/add buttons:

```javascript
//odstranenie tlacitka (kazde tlacitko ma atribut dt-dtbtn podla ktoreho viete zistit jeho meno)
galleryTable.hideButton("create");
galleryTable.hideButton("import");
galleryTable.hideButton("export");

//pridanie tlacitka na 5 poziciu
let buttonCounter = 5;
galleryTable.button().add(buttonCounter++, {
	text: "S",
	action: function (e, dt, node) {
		switchGallerySize(e, dt, node, "S");
	},
	className: "btn btn-outline-secondary btn-gallery-size active",
	attr: {
		title: "Size S",
	},
});

galleryTable.button().add(buttonCounter++, {
	text: '<i class="far fa-th-list"></i>',
	action: function (e, dt, node) {
		console.log("btn, e=", e, "dt=", dt, "node=", node);
		//ziskaj data selectnuteho riadku
		let selectedRows = dt.rows({ selected: true }).data();
	},
	init: function (dt, node, config) {
		//zobraz tlacidlo aktivne iba ked je oznaceny aspon jeden riadok
		$.fn.dataTable.Buttons.showIfRowSelected(this, dt);
		//ALEBO ked je oznaceny PRESNE jeden riadok
		//$.fn.dataTable.Buttons.showIfOneRowSelected(this, dt);
	},
	className: "btn btn-outline-secondary btn-gallery-size",
	attr: {
		//zobrazi tooltip po prechode mysou
		title: "Table view",
		"data-toggle": "tooltip",
	},
});

//wrapnutie 4 tlacitok do grupy (v galerii prepinanie velkosti SMLT)
$(".btn-gallery-size").wrapAll('<div class="btn-group-wrapper buttons-divider-both" data-toggle="tooltip" data-original-title="Veľkosť obrázkov"><div class="btn-group btn-group-toggle gallery-buttons-size" /></div>');

//znova zobrazenie tlacidla
galleryTable.showButton("export");
```

## Button to perform server action

The datatable offers the possibility to add a button to perform a server action (e.g. rotate an image, delete all records).

JS function `nejakaTable.executeAction(action, doNotCheckEmptySelection, confirmText, noteText, customData = null, forceIds = null)` has parameters:
- `action` (String) - the name of the action to be sent to the server for execution.
- `doNotCheckEmptySelection` (true) - by setting to `true` does not check if any rows are selected and the value -1 is sent to the REST service as the ID of the selected row. This is useful for buttons that don't need to have rows selected, e.g. Refresh All Records, etc.
- `confirmText` (String) - if specified, a confirmation is displayed before the action is performed (e.g. Are you sure you want to ...?).
- `noteText` (String) - additional text displayed above the buttons to confirm the action (e.g. the operation may take several minutes).
- `customData` - object added to the REST service call as a parameter `customData` (e.g. additional data required for the correct execution of the action).
- `forceIds` - a number or array of numbers with the value of the record ID for which the action is to be performed. Used when you need to click on the status icon to trigger an action (without having to select a row).
A call is made to the REST service on the server `/action/rotate` implemented in the method [DatatableRestControllerV2.processAction](../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java). A list of selected rows (their IDs) is sent to the REST service, which is processed in the DatatablesRestControllerV2.action method.

**Example of use** - added button to `toolbaru` above the datatable with the call to action:
```javascript
cacheObjectsTable.button().add(3, {
	extends: "remove",
	editor: cacheObjectsTable.EDITOR,
	text: '<i class="far fa-camera"></i>',
	action: function (e, dt, node) {
		cacheObjectsTable.executeAction("deletePictureCache", true, "[[#{components.data.deleting.imgcache.areYouSure}]]", "[[#{components.data.deleting.imgcache.areYouSureNote}]]");
	},
	className: "btn btn-danger",
	attr: {
		title: "[[#{components.memory_cleanup.deleteImageCache}]]",
		"data-toggle": "tooltip",
	},
});
```

Button also with a check that some line is selected (in init option):

```javascript
galleryTable.button().add(buttonCounter++, {
	extends: "remove",
	editor: galleryTable.EDITOR,
	text: '<i class="far fa-retweet"></i>',
	action: function (e, dt, node) {
		//console.log("Rotate, e=",e," dt=",dt," node=",node);
		galleryTable.executeAction("rotate");
	},
	init: function (dt, node, config) {
		$.fn.dataTable.Buttons.showIfRowSelected(this, dt);
	},
	className: "btn btn-outline-secondary",
	attr: {
		title: "Otočiť",
		"data-toggle": "tooltip",
	},
});
```

The action will trigger the following events:
- `WJ.DT.executeAction` - after successful execution of the action.
- `WJ.DT.executeActionCancel` - after the action has failed or when you click Cancel to confirm the action.

## Buttons by rights

If you need to display buttons according to rights (e.g. the Add button only if the user has a certain right) it is possible to add an attribute to the datatable configuration `perms`:

```javascript
webpagesDatatable = WJ.DataTable({
    url: webpagesInitialUrl,
    ...
    perms: {
        create: 'addPage',
        edit: 'pageSave',
        duplicate: 'pageSaveAs',
        remove: 'deletePage'
    }
});
```

definition in `perms` object defines a specific name right for each create operation (`create`), editing (`edit`), duplication (`duplicate`) and deletion (`remove`) of record.

Rights settings will stop showing the buttons in the toolbar and will also not show the save/add/delete button in the editor dialog (the buttons will be hidden when the editor window is displayed).

The table provides an API for verifying the right as `TABLE.hasPermission(action)`:

```javascript
if (webpagesDatatable.hasPermission("create")) {
    ...
}
```

**NOTICE:** don't just rely on checking the rights on the frontend, the rights need to be checked in the REST service or service class as well. You can use the methods [beforeSave or beforeDelete](restcontroller.md#zabránenie-zmazania--editácie-záznamu).
## Line styling

Sometimes it is necessary to set the CSS style of the whole line (e.g. bold font for the main page, or red for the inaccessible page). We use the transfer of nested attributes through the object to transfer this additional data [EditorFields](../datatables-editor/datatable-columns.md#vnorené-atribúty). We created a class [BaseEditorFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java)which has a method `addRowClass(String addClass)` to add a CSS class to the line.

An example of use is in [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java):

```java
...
public class DocEditorFields extends BaseEditorFields {
    public void fromDocDetails(DocDetails doc) {
        ...
        //hlavna stranka adresara
        if (groupDetails != null && doc.getDocId()>0 && groupDetails.getDefaultDocId()==doc.getDocId()) {
            addRowClass("is-default-page");
        }

        //vypnute zobrazovanie
        if (doc.isAvailable()==false) addRowClass("is-not-public");
    }
}
```

The following CSS line styles are available:
- `is-disabled` - represents an inactive item, shown in red.
- `is-disapproved` - represents an unapproved item, shown in red.
- `is-default-page` - represents the main web page of the directory, shown in bold.
- `is-not-public` - represents a non-public item, shown in red.
The CSS line style setting is implemented in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) using the option `rowCallback` datatable constructor. Verifies the existence of the property `data.editorFields.rowClass` and if it exists it applies the value to the line.

You can also set the line style in JavaScript code (e.g. based on attributes) using the `onRowCallback`. So you can easily mark lines as inactive with CSS style `is-not-public`.

```javascript
domainRedirectTable = WJ.DataTable({
	url: "/admin/rest/settings/domain-redirect",
	columns: columns,
	serverSide: false,
	editorId: "redirectId",
	onRowCallback: function (TABLE, row, data) {
		if (data.active === false) $(row).addClass("is-not-public");
	},
});
```

## Status icons

Sometimes it is necessary to display status icons of a record (e.g. in web pages icons Not shown in menu, Redirected page and so on). We use the transfer of nested attributes through the object to transfer these additional data [EditorFields](../datatables-editor/datatable-columns.md#vnorené-atribúty). We created a class [BaseEditorFields](../../../src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java)which has a method `addStatusIcon(String className)`. Icons are from FontAwesome.

![](../../redactor/webpages/status-icons.png)

An example of use is in [DocEditorFields](../../../src/main/java/sk/iway/iwcm/doc/DocEditorFields.java). It is necessary to define the attribute `statusIcons` s `@DataTableColumn` annotations to display the column. It is displayed as a selection box, in `options` attribute, we recommend defining an icon and descriptive text. Like `value` search conditions are transmitted (see below):

```java
...
public class DocEditorFields extends BaseEditorFields {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "webpages.icons.title",
        hiddenEditor = true, hidden = false, visible = true, sortAfter = "id", className = "allow-html", orderable = false,
        editor = { @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "<i class=\"fas fa-globe\"></i> [[#{webpages.icons.showInMenu}]]", value = "showInMenu:true"),
                @DataTableColumnEditorAttr(key = "<i class=\"far fa-globe\"></i> [[#{webpages.icons.notShowInMenu}]]", value = "showInMenu:false"),
                @DataTableColumnEditorAttr(key = "<i class=\"fas fa-lock\"></i> [[#{webpages.icons.onlyForLogged}]]", value = "passwordProtected:notEmpty"),
                @DataTableColumnEditorAttr(key = "<span style=\"color: #FF4B58\">[[#{webpages.icons.disabled}]]</span>", value = "available:false"),
                @DataTableColumnEditorAttr(key = "<i class=\"fas fa-external-link-alt\"></i> [[#{webpages.icons.externalLink}]]", value = "externalLink:notEmpty"),
                @DataTableColumnEditorAttr(key = "<i class=\"fas fa-eye-slash\"></i> [[#{webpages.icons.notSearchable}]]", value = "searchable:false")
            }
        )}
    )
    private String statusIcons;

    public void fromDocDetails(DocBasic doc, boolean loadSubQueries) {
        //ikony
        if (doc.isShowInMenu()) addStatusIcon("fas fa-globe");
        else addStatusIcon("fal fa-globe");
        if (Tools.isNotEmpty(doc.getExternalLink())) addStatusIcon("fas fa-external-link-alt");
        if (doc.isSearchable()==false) addStatusIcon("fas fa-eye-slash");
        if (Tools.isNotEmpty(doc.getPasswordProtected())) addStatusIcon("fas fa-lock");
    }

    public getStatusIcons() {
        return getStatusIconsHtml();
    }
}
```

if you need to programmatically add something to the status icons (in case of web pages it is a link to the page view), you can edit the status icons directly in the code (in this case do not implement the method `getStatusIcons`):

```java
    public void fromDocDetails(DocDetails doc) {
        ...
        StringBuilder iconsHtml = new StringBuilder();

        //pridaj odkaz na zobrazenie stranky
        Prop prop = Prop.getInstance();
        String link = "/showdoc.do?docid="+doc.getDocId();
        if (doc instanceof DocHistory) {
            //v history je otocene docid a historyid
            link = "/showdoc.do?docid="+doc.getId()+"&historyId="+doc.getDocId();
        }
        iconsHtml.append("<a href=\""+link+"\" target=\"_blank\" title=\""+ResponseUtils.filter(prop.getText("history.showPage"))+"\"><i class=\"far fa-eye\"></i></a> ");

        iconsHtml.append(getStatusIconsHtml());
        statusIcons = iconsHtml.toString();
        ...
    }
```

The search after selecting a filter option is implemented in `DatatableRestControllerV2.addSpecSearchStatusIcons` and is called automatically when the call is made `addSpecSearch` (if you extend this method, you must call it implicitly), the repository must extend `JpaSpecificationExecutor`. The following search options are currently supported:
- `property:true` - attribute value `property` Is `true`
- `property:false` - attribute value `property` Is `false`
- `property:notEmpty` - attribute value `property` is not empty
- `property:empty` - attribute value `property` is empty (null or '')
- `property:%text%` - attribute value `property` contains the specified text (`like` search)
- `property:!%text%` - attribute value `property` does not contain the specified text (`not like` search)

## View data based on rights

In the columns definition it is possible to set the required right for displaying the column in the datatable or in the editor using the attribute `perms`. Example in file [redirect.pug](../../../src/main/webapp/admin/v9/views/pages/settings/redirect.pug):

```javascript
{
    data: "domainName",
    name: "domainName",
    title: "[[\#{groupedit.domain}]]",
    editor: {
        type: "text"
    },
    renderFormat: "dt-format-text",
    renderFormatLinkTemplate: "javascript:;",
    renderFormatPrefix: '<i class="far fa-pencil"></i> ',
    className: "dt-row-edit",
    perms: "multiDomain" //stĺpec sa zobrazí len ak používateľ má právo multiDomain
},
```

WebJET generates a JS field in the HTML code when the page is displayed `nopermsJavascript` which lists the modules to which the user does not have rights. CSS style with classes is also generated `.noperms-menomodulu` with set `display: none`.

## Arrangement

Datatable supports setting the ordering by attribute [warrant:](https://datatables.net/reference/option/order). This can be transferred as `option` when initializing the table. But because of pugjs/thymeleaf parsing it is not possible to write the expression directly `[[0, 'asc']]`because Thymeleaf will execute it. It is necessary to prepare the layout array in a roundabout way via variable and push:

```javascript
var order = [];
order.push([5, "desc"]);

configurationDatatable = WJ.DataTable({
	url: "/admin/v9/settings/configuration",
	columns: columns,
	order: order,
});
```

so the Thymeleaf parser is "fooled" and the layout field is correctly defined.

## Search

**Filtering HTML tags**

Datatable at **local search** (not applicable to server searches) by default filters HTML tags and searches only in text (ignoring the content of HTML tags). This is an undesirable state for fields of type `textarea` where the HTML code is entered (e.g. script code in the Scripts application). Subsequently, the search will not find the expression in the HTML code.

A search type html-input is added to index.js, which does not filter HTML tags `$.fn.dataTableExt.ofnSearch['html-input'] = function(value)...`. V `columnDefs` is automatically set for columns with CSS style `dt-format-text-wrap` (set automatically by annotation `DataTableColumnType.TEXTAREA`) or `html-input`.

## External filter

In addition to displaying the filters in the header of each column of the table, you can add a separate filter field anywhere in the HTML code of the page. An example is [Deleting records in the database](../../../src/main/webapp/admin/v9/views/pages/settings/database-delete.pug) where the filter is moved directly to the page header to the title.

In the pug file you need to prepare the basic HTML structure by creating a div container with the ID `TABLEID_extfilter`. It searches for div elements with CSS class `dt-extfilter-title-FIELD` in which the column name is inserted and `dt-extfilter-FIELD` into which the search field is inserted.

```
div#dateDependentEntriesTable_extfilter
    div.row.datatableInit
        div.col-auto.dt-extfilter-title-from
        div.col-auto.dt-extfilter.dt-extfilter-from
```

**ATTENTION:** in the element for the search field there is a CSS class `.dt-extfilter` Also `.dt-extfilter-FIELD`, you need to use both. By CSS class `.dt-extfilter` the element is found after clicking on the magnifying glass, in the data attribute `data-column-index` the sequence number of the column is stored.
If you want to move the filter to the page header, you can simply move it using jQuery as in [database-delete.pug](../../../src/main/webapp/admin/v9/views/pages/settings/database-delete.pug).

**Implementation Notes:**
Internally, clicking the magnifying glass icon will transfer the specified filter text to the datatable filter and save it to the object as well `TABLE.DATA.columns[inputIndex].searchVal`. This is available for AJAX calls. In the function `datatable2SpringData` values are then searched `.searchVal` for the external filter and if they are set they are added to the search parameters for the AJAX request.

This solution was chosen because of the pre-use of existing code for calculating the search value (especially for dates), at the same time columns using an external filter can have the attribute set `filter=false` v `@DatatableColumn anotácii`.

## Export/import

Implemented a system for importing and exporting data between datatables. For each datatable, after creating and setting it up, verify the import and export functionality. Also verify all import options, including column-based matching. If you do not want to use export/import turn off the buttons with code (datatableInstance is the name of the datatable instance):

```javascript
datatableInstance.hideButton("import");
datatableInstance.hideButton("export");
```

If you need to omit a column from the export, just set/add it to `columns` attribute `className` value `not-export`:

```java
@DataTableColumn(
    inputType = DataTableColumnType.TEXT,
    title="[[#{components.banner.fieldName}]]",
    className = "not-export"
)
private String fieldName;
```

For more information, see the documentation for [Developer](export-import.md) or for [of the editor](../../redactor/datatables/export-import.md).

## API calls

```javascript
//zoznam selectnutych riadkov
galleryTable.rows({ selected: true }).data();
//zmena URL adresy
galleryTable.ajax.url("/admin/rest/nova-url");
//refresh dat
galleryTable.ajax.reload();
//nastavenie filtra a reload dat
galleryTable
	.columns(3)
	.search("^" + virtualPath + "$")
	.draw();

//nastavenie JSON dat do aktualneho editora
EDITOR.setJson(json);
//aktualne editovane data (json objekt)
EDITOR.currentJson;

//options z odpovede REST služby pre rendering (potrebujeme pre export číselníkových dáta)
TABLE.DATA.jsonOptions;
//kompletná URL adresa posledného REST volania
TABLE.DATA.urlLatest;
//všetky parametre posledného REST volania (aktuálna stránka, veľkosť stránky, filtre)
TABLE.DATA.urlLatestParams;

//schovanie/zobrazenie tlacidla - name je hodnota atributu data-dtbtn button elementu
TABLE.hideButton(name);
TABLE.hideButtons(["name1", "name2"]);
TABLE.showButton(name);

//deaktivuje rezim editacie bunky (ak je zapnuty) - ak cez karty prepinate obsah datatabulky vzdy deaktivujte rezim editacie bunky
TABLE.cellEditOff();

/**
 * Vypocita/prepocita velkost stranky (zobrazeny pocet zaznamov)
 * @param {*} updateLengthSelect - ak je true aj sa reloadnu udaje (napr. pri zmene velkosti obrazka v galerii)
 */
TABLE.calculateAutoPageLength(updateLengthSelect);
```

## Code samples

### Listening for a table refresh event:

Click on the button `reload` triggers an event `WJ.DTE.forceReload` to which you can listen and e.g. update the tree structure:

```javascript
window.addEventListener(
	"WJ.DTE.forceReload",
	(e) => {
		//console.log("FORCE RELOAD listener, e=", e);
		$("#SomStromcek").jstree(true).refresh();
	},
	false
);
```
