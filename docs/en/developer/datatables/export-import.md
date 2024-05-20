# Export and import

Exporting and importing data uses the export and import options in [API datatables](https://datatables.net/extensions/buttons/examples/initialisation/export). For working with Excel files the library is used [SheetJS](https://docs.sheetjs.com/).

After implementing import and export in your datatable, don't forget to use [automated import test](../testing/datatable-import.md).

## Export of data

- Allows export to **Excel(xlsx) and direct printing to the printer**
- The file is named according to the current `title` page and the current date and time will be automatically added.
- The export in a table with server pagination can be set to the data type (**current/all pages, filtered/all rows, sorting**).
- In server paging, a call is first made to the REST service from which the **maximum 50 000 records**. If you need to export multiple records, use multiple export using filtering.
- Server data processing takes place outside the datatable (for performance reasons), it is re-generated `datetime` columns and `radio buttony` (supported are `date-time, date, *-select, *-boolean`).
- When exporting, a list of columns is prepared in the first row, **the import is consequently not sensitive to the order of the columns**.
- For **selection fields** (select/digits) with **export text value** and reconstructs back to the ID on import. This allows to have **different IDs of bound records** between environments (e.g. template ID for a web page), if the name matches, the record is correctly paired. Consequently, there is also human readable text in the export instead of the ID value.
The implementation is in the file `export-import.js` in the function of `bindExportButton(TABLE, DATA) `which is initialized directly in the `index.js` when initializing the datatable. The HTML code of the export dialog is in the file `datatables-data-export.pug` which is through `include` inserted into `layout.pug`.

### Data preparation before export

Often only basic data/columns are loaded in a datatable (especially when using [editorFields](../datatables-editor/customfields.md) attribute). Therefore, it is always necessary to call the server and get the complete data before exporting. When the export is detected, instead of `ProcessItemAction.GETALL` to be used `ProcessItemAction.GETONE` as in obtaining a single record. Thus, the returned entities will contain all the data.

When using `serverSide=false` it is not possible to use all export options on the datatable (e.g. only filtered data), because they may not be server-processable. These options are therefore hidden when you click on the button to open the export dialog (implemented directly in the `index.js` in the function of the dialog export button).

### Performing an export

The export options are set in the dialog box. After clicking the export button, the current data is retrieved from the server by calling the function `getDataToExport(serverSide, TABLE, pageVal, searchVal, orderVal)`that are processed after the call `.then(response => {`. For each row of the obtained JSON object `formatedData = content.map(c => {` row array is generated for data export by iterating over the editor fields `DATA.fields.forEach((dc) => {`.

Data fields of type `json` are treated specially. For types `dt-tree-page, dt-tree-group, dt-tree-dir` the value of z is generated in the output `v.fullPath` (or `v.virtualPath` For `dt-tree-dir`). The result is that the export will not contain the whole JSON object but the value of type `["/Portal/Novinky", "/English/News"]`.

Resulting object `formatedData` contains a two-dimensional data field for export. These are used in the datatable API call by simulating a click on the export button `TABLE.button().add(0, {` by calling `$(".exportujem").click();`. This dummy button is hidden after export via `TABLE.buttons('.exportujem').remove();`. This is how we simulate clicking the standard datatable export button - after clicking export in the dialog box, the data is prepared, a button is added to the datatable to export the data, and the button is clicked. Then, after the export, the standard export button is hidden.

The export configuration is set in `exportOptions`. Important is the processing in `customizeData` where the table header is re-generated. It originally contains a list of columns in the table, but the modification will use the column list for the editor. In addition to the column name, the `dc.label` values also generates the attribute name (`dc.name`), because sometimes the same names are used (e.g. in the user edit there is a default user name but also a name in the Delivery address section).

### Special export type

If you need to implement a special export type, just add the following element in the web page:

```html
<div class="hidden" id="datatableExportModalCustomOptions">
	<div class="form-check">
		<input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-extend-custom" value="custom" data-hide="#datatableExportModal .file-name,#pills-export-advanced-tab" />
		<label class="form-check-label" for="dt-settings-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
	</div>
</div>
```

in the attribute `data-hide` it is possible to specify a list of elements that are automatically hidden after setting the above option.

Need to implement JS function `window.exportDialogCustomCallback(type, TABLE)`that is made under this option:

```javascript
function exportDialogCustomCallback(type, TABLE) {
	WJ.openPopupDialog("/admin/conf_export.jsp");
	return true;
}
```

## Import of data

- Enables **import data as new** (to be added to the database) or **match existing data according to the selected column** (e.g. name, URL, etc.). When matching, it first looks up the record in the database and then updates it. If it does not exist, it creates a new record.
- **Imported from xlsx format**.
- Import is carried out **successively in batches of 25 records**so as not to overload the server.
The implementation is in the file `export-import.js` in the function of `bindImportButton(TABLE, DATA) `which is initialized directly in the `index.js` when initializing the datatable. The HTML code of the import dialog is in the file `datatables-data-import.pug` which is through `include` inserted into `layout.pug`.

The import dialog box displays a list of columns by which data can be imported. This list is implemented directly in `index.js` when you click the button to display the import dialog. Options are generated with the list `DATA.fields.forEach((col, index) => {`. Skipped are attributes of type `hidden` or attributes with annotation `data-dt-import-hidden`.

### Conversion from Excel

The conversion from Excel file to data is done directly on the client side in the call `document.getElementById('insert-file').addEventListener('change', e => {`, that is, right after selecting the file. The SheetJS library is used by calling [xlsx.read](https://docs.sheetjs.com/#parsing-options). In the configuration the date processing is set using the attribute `cellDates: true`, which will ensure the conversion of dates to `Date` object. The result of reading is a JSON excelData object and raising the event `file-reader-done`.

Processing data from Excel in the object `excelData` takes place in `$( document ).on('file-reader-done', () => {` where several adjustments will be made:
- A JSON object is generated, using the value from the table header after the | character as the attribute name.
- The structure of the resulting JSON object is the same as the standard retrieval/deposition of data in a datatable.
- It converts dates, text values of code data to ID value and for fields of JSON types, the conversion from string to real JSON object is performed.
Importing is done by calling the same REST service as when using the standard record editor. However, multiple records are sent at the same time, up to a maximum of `chunks`. For a large number of entries, the REST service is called sequentially. `/editor`, with a maximum of `chunks` records (default 25, defined in conf. variable `chunksQuantity`). A progress bar is also displayed, similar to the one in the gallery when uploading a file.

### Update by column

If the option to update by column is selected in the import dialog, the method is called `DatatableRestControllerV2.editItemByColumn(T entity, String updateByColumn)`. This first of all searches for records in the database according to the specified column and value in the given row in excel. E.g. records by matching `email` attribute in excel. Here it is important to note that there may be multiple such records in the database and the update during import will be performed on multiple records.

After finding matching records in the database, you need to modify the entity ID column of the imported row. According to the annotation `@Id` the column is identified (it does not always have to be `id`, it can be e.g. `userId`). Value `Id` column is set to the value of an existing entity in the database and then the record is saved by calling `editItem(entity, id)`.

In simple terms, all the code searches for an existing record in the database and sets the imported entity `id` value to the value of the found record.

> WARNING: during the implementation we have identified a problem that for classes annotated via Lombook it is not possible to use `BeanUtils.setProperty` nor `BeanUtils.copyProperties`. It is necessary to use `BeanWrapperImpl` a `NullAwareBeanUtils.copyProperties`.

> NOTE: it is technically possible to import only some columns, so do not assume that the import will always contain all the data. Otherwise you will get errors like `NullPointerException`. Especially in `editorFields.toEntity` need to control `null` values on attributes so that their transmission does not crash.

### Supported annotations

In the annotation `@DatatableColumn` the following options can be used

- `@DataTableColumnEditorAttr(key = "data-dt-import-updateByColumn", value = "PROPERTY")` - in the import dialog sets up a search by column for this column `PROPERTY`. Required if e.g. in `editorFields` you override a property (e.g. `login`) but when importing you need to search/pair by the original attribute in the database `login`.
- `@DataTableColumnEditorAttr(key = "data-dt-import-hidden", value = "true")` - the attribute annotated in this way will not be displayed in the import dialog.
- `@DataTableColumn(className = "not-export")` - column with CSS class `not-export` will not be exported.

### Special import type

If you need to implement a special import type, just add the following element in the web page:

```html
<div class="hidden" id="datatableImportModalCustomOptions">
	<div class="form-check">
		<input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-import-extend-custom" value="custom" data-hide="#datatableImportModal .file-name,#import-settings" />
		<label class="form-check-label" for="dt-settings-import-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
	</div>
</div>
```

in the attribute `data-hide` it is possible to specify a list of elements that are automatically hidden after setting the above option.

Need to implement JS function `window.importDialogCustomCallback(type, TABLE)`that is made under this option:

```javascript
function importDialogCustomCallback(type, TABLE) {
	WJ.openPopupDialog("/admin/conf_import.jsp");
	return true;
}
```

## Notes on implementation

Numeric values (select) are exported as a text value. In the file [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js) are functions `getOptionsTableExport` a `getOptionsTableImport`which will prepare a table with the key `fieldName-value` (export) and `fieldName-label` (import) for easy translation between value and label.

The most complicated part is reading data from the server during server paging. When an export request is made, a REST service call must be made. This is done in the function `getDataToExport` where it is used and modified `DATA.urlLatestParams` (parameters such as size, page, etc are replaced). The data is retrieved by a standard ajax call outside the datatable API (we don't want the datatable to load such a large amount of data - fear of crashing it). The trick with using such data is in the export button in the option `customizeData: function(d)`, where we replace the data in the datatable with the newly obtained data.

When importing, the data is transformed from xlsx to a JSON object. This is then used for a standard DT editor call, where 25 records are sent to the server at once. The import is impelented in the function `importData`. The existing progress bar is used for uploading files by calling an event `$( document ).trigger('initAddedFileFromImageOutside', file);`.

Conversion from xlsx to JSON format is implemented in `export-import.js`. The list of columns is iterated and the value for the given column name is retrieved from Excel. Only the columns specified in Excel are populated into the JSON object. For `-date` columns are formatted with the date, since only the timestamp is retrieved from Excel.

The modal window for import/export is global, if there are multiple datatables on the page it is necessary to specify in which one the import/export is happening. There are variables:
- `window.datatableExportModal.tableId` - Table ID for export (selector)
- `window.datatableImportModal.tableId` - Table ID for import (selector)
- `window.datatableImportModal.TABLE`
Class `DatatableRestControllerV2``isExporting()``isImporting()``validateEditor`.
