# Export and import

Exporting and importing data uses the export and import capabilities of the [datatables API](https://datatables.net/extensions/buttons/examples/initialisation/export). The [SheetJS](https://docs.sheetjs.com/) library is used to work with Excel files.

After implementing import and export in your datatable, don't forget to also use [automated import test](../testing/datatable-import.md).

## Data export

- Allows export to **Excel(xlsx) and direct printing to printer**
- The file will be named according to the current ```title``` page and the current date and time will be automatically added.
- It is possible to set the data type for the export in a table with server paging (**current/all pages, filtered/all rows, sorting**).
- With server paging, a REST service call is first made, from which **a maximum of 50,000 records** are retrieved. If you need to export more records, use multiple exports using filtering.
- Server data processing takes place outside the data table (due to performance), ```datetime``` columns and ```radio buttony``` are newly generated (```date-time, date, *-select, *-boolean``` are supported).
- When exporting, a list of columns is prepared in the first row, **the import is then not sensitive to the order of the columns**.
- For **select fields** (select/numbers), the **text value** is exported and reconstructed back to an ID when imported. This allows you to have **different IDs of linked records** between environments (e.g. template ID for a web page), if the name matches, the record is correctly matched. The export then also contains human-readable text instead of the ID value.

The implementation is in the file ```export-import.js``` in the function ```bindExportButton(TABLE, DATA) ```, which is initialized directly in ```index.js``` when initializing the data table. The HTML code of the export dialog is in the file ```datatables-data-export.pug``` which is inserted into ```layout.pug``` via ```include```.

### Preparing data before export

In a datatable, only basic data/columns are often loaded (especially when using the [editorFields](../datatables-editor/customfields.md) attribute). Therefore, before exporting, it is always necessary to call the server and get the complete data. When detecting an export, ```ProcessItemAction.GETONE``` is used instead of ```ProcessItemAction.GETALL``` as when retrieving a single record. Thus, the returned entities will contain all the data.

When using ```serverSide=false``` on a datatable, it is not possible to use all export options (e.g. only filtered data) because they may not be server-processable. These options are therefore hidden when clicking the export dialog box open button (implemented directly in ```index.js``` in the export dialog button function).

### Performing an export

The export options are set in the dialog box. After clicking the export button, the current data is obtained from the server by calling the function ```getDataToExport(serverSide, TABLE, pageVal, searchVal, orderVal)```, which is processed after calling ```.then(response => {```. For each line from the obtained JSON object ```formatedData = content.map(c => {```, a row field is generated for data export by iterating through the editor fields ```DATA.fields.forEach((dc) => {```.

Data fields of type ```json``` are handled specially. For types ```dt-tree-page, dt-tree-group, dt-tree-dir```, a value from ```v.fullPath``` (or ```v.virtualPath``` for ```dt-tree-dir```) is generated in the output. As a result, the export will not contain the entire JSON object, but a value of type ```["/Portal/Novinky", "/English/News"]```.

The resulting object ```formatedData``` contains a two-dimensional array of data for export. These will be used in the datatable API call by simulating a click on the export button ```TABLE.button().add(0, {``` using the call ```$(".exportujem").click();```. This dummy button will be hidden after the export via ```TABLE.buttons('.exportujem').remove();```. This is how we simulate clicking on the standard export button of the datatable - after clicking on export in the dialog box, the data is prepared, a button for exporting data is added to the datatable and this button is clicked. Subsequently, after the export, the standard export button will be hidden.

The export configuration is set in ```exportOptions```. The important thing is the processing in ```customizeData``` where the table header is regenerated. It originally contains the list of columns in the table, but the editing uses the column list for the editor. In addition to the ```dc.label``` value, the attribute name (```dc.name```) is also generated for the column name, because sometimes the same names are used (e.g. in user editing there is a standard user name but also a name in the Delivery address section).

### Special type of export

If you need to implement a special type of export, just add the following element to your web page:

```html
<div class="hidden" id="datatableExportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-extend-custom" value="custom" data-hide="#datatableExportModal .file-name,#pills-export-advanced-tab">
        <label class="form-check-label" for="dt-settings-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

In the ```data-hide``` attribute, it is possible to specify a list of elements that will be automatically hidden after setting the above option.

It is necessary to implement the JS function ```window.exportDialogCustomCallback(type, TABLE)```, which is executed with this option:

```javascript
function exportDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_export.jsp");
    return true;
}
```

## Data import

- Allows **import data as new** (added to the database) or **match existing data according to the selected column** (e.g. name, URL address, etc.). When matching, it first looks for a record in the database and then updates it. If it does not exist, it creates a new record.
- **Importing from xlsx format**.
- The import is performed **gradually in batches of 25 records** to avoid overloading the server.

The implementation is in the file ```export-import.js``` in the function ```bindImportButton(TABLE, DATA) ```, which is initialized directly in ```index.js``` when initializing the datatable. The HTML code of the import dialog is in the file ```datatables-data-import.pug``` which is inserted into ```layout.pug``` via ```include```.

The import dialog box displays a list of columns by which data can be imported. This list is implemented directly in ```index.js``` after clicking the button to display the import dialog. The options are generated from the list ```DATA.fields.forEach((col, index) => {```. Attributes of type ```hidden``` or attributes with the annotation ```data-dt-import-hidden``` are skipped.

### Conversion from Excel

The conversion from an Excel file to data is done directly on the client side in the ```document.getElementById('insert-file').addEventListener('change', e => {``` call, i.e. immediately after selecting the file. The SheetJS library is used by calling [xlsx.read](https://docs.sheetjs.com/#parsing-options). The configuration sets the date processing using the ```cellDates: true``` attribute, which ensures the conversion of dates to a ```Date``` object. The result of the reading is a JSON object excelData and the event ```file-reader-done``` is triggered.

Processing of Excel data in object ```excelData``` takes place in ```$( document ).on('file-reader-done', () => {``` where several modifications are made:

- A JSON object is generated, the value from the table header after the | character is used as the attribute name.
- The structure of the resulting JSON object is the same as with standard data retrieval/storage in a data table.
- Converts dates, text values ​​of numeric data to an ID value, and for JSON type fields, conversion is performed from a string to a real JSON object.

Importing is done by calling the same REST service as when using the standard record editor. However, more records are sent at once, but at most according to the value of ```chunks```. For a large number of records, the REST service ```/editor``` is called sequentially, with a maximum of ```chunks``` records in one call (by default 25, defined in the conf. variable ```chunksQuantity```). A progress indicator is also displayed, similar to the one in the gallery when uploading a file.

### Update by column

If the update by column option is selected in the import dialog box, the ```DatatableRestControllerV2.editItemByColumn(T entity, String updateByColumn)``` method is called. This first searches for records in the database according to the specified column and the value in the given row in Excel. For example, records according to the ```email``` attribute match in Excel. It is important to note that there may be more such records in the database and the update during import will be performed on multiple records.

After finding matching records in the database, it is necessary to modify the ID column of the entity of the imported row. According to the annotation ```@Id```, the column is identified (it does not always have to be ```id```, it can be, for example, ```userId```). The value ```Id``` of the column is set to the value of the existing entity in the database and then the record is saved by calling ```editItem(entity, id)```.

In simple terms, the entire code searches for an existing record in the database and sets the ```id``` value of the imported entity to the value of the found record.

> **Warning:** We identified an implementation issue where it is not possible to use ```BeanUtils.setProperty``` or ```BeanUtils.copyProperties``` for classes annotated via Lombook. You need to use ```BeanWrapperImpl``` and ```NullAwareBeanUtils.copyProperties```.

> **Warning:** It is technically possible to import only some columns, so do not assume that the import will always contain all the data. Otherwise, you will encounter errors of the ```NullPointerException``` type. Especially in ```editorFields.toEntity``` it is necessary to check the ```null``` values ​​on the attributes so that their transfer does not fail.

### Supported annotations

The following options can be used in the ```@DatatableColumn``` annotation:

- ```@DataTableColumnEditorAttr(key = "data-dt-import-updateByColumn", value = "PROPERTY")``` - ​​in the import dialog, sets the search for this column according to the column ```PROPERTY```. Necessary if, for example, in ```editorFields``` you are overwriting some property (e.g. ```login```) but when importing you need to search/match in the database according to the original attribute ```login```.
- ```@DataTableColumnEditorAttr(key = "data-dt-import-hidden", value = "true")``` - ​​an attribute annotated in this way will not be displayed in the import dialog.
- ```@DataTableColumn(className = "not-export")``` - ​​column with CSS class ```not-export``` will not be exported.

### Special import type

If you need to implement a special type of import, just add the following element to your web page:

```html
<div class="hidden" id="datatableImportModalCustomOptions">
    <div class="form-check">
        <input class="form-check-input" type="radio" name="dt-settings-extend" id="dt-settings-import-extend-custom" value="custom" data-hide="#datatableImportModal .file-name,#import-settings">
        <label class="form-check-label" for="dt-settings-import-extend-custom">[[\#{admin.conf_editor.custom-xml-export}]]</label>
    </div>
</div>
```

In the ```data-hide``` attribute, it is possible to specify a list of elements that will be automatically hidden after setting the above option.

It is necessary to implement the JS function ```window.importDialogCustomCallback(type, TABLE)```, which is executed with this option:

```javascript
function importDialogCustomCallback(type, TABLE) {
    WJ.openPopupDialog("/admin/conf_import.jsp");
    return true;
}
```

## Implementation notes

Numeric values ​​(select) are exported as text values. In the file [datatables-wjfunctions.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js) there are functions ```getOptionsTableExport``` and ```getOptionsTableImport``` that prepare a table with key ```fieldName-value``` (export) and ```fieldName-label``` (import) for easy translation between value and label.

The most difficult part is reading data from the server during server paging. When requesting an export, a REST service call must be made. This is done in the ```getDataToExport``` function where ```DATA.urlLatestParams``` is used and modified (parameters such as size, page, etc. are replaced). The data is obtained by a standard ajax call outside the datatable API (we don't want the datatable to read such a large amount of data - fear of its crashing). The trick with using such data is in the export button in the ```customizeData: function(d)``` option, where we replace the data in the datatable with the newly obtained data.

During import, data from xlsx is transformed into a JSON object. This object is then used for a standard DT editor call, where 25 records are sent to the server at once. The import is implemented in the ```importData``` function. The existing progress bar is used for file upload by calling the ```$( document ).trigger('initAddedFileFromImageOutside', file);``` event.

Conversion from xlsx to JSON format is implemented in ```export-import.js```. The list of columns is iterated and the value for the given column name is retrieved from Excel. Only the columns specified in Excel are populated into the JSON object. For ```-date``` columns, the date is formatted, since only the timestamp is retrieved from Excel.

The modal window for import/export is global, if there are multiple data tables on the page, it is necessary to determine which one the import/export is taking place in. There are variables:

- ```window.datatableExportModal.tableId``` - ​​Table ID for export (selector)
- ```window.datatableImportModal.tableId``` - ​​Table ID for import (selector)
- ```window.datatableImportModal.TABLE``` - ​​table instance for import

The ```DatatableRestControllerV2``` class contains methods ```isExporting()``` for detecting exports and ```isImporting()``` for importing. These can be used in your REST controller implementation, e.g. for data validation in method ```validateEditor```.