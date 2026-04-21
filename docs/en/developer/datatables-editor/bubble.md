# Edit in grid view

Datatables supports direct cell editing in ```bubble``` editing mode. This mode is enabled by clicking the **Edit in Grid View** button in the toolbar. Then, clicking on a cell displays a dialog box with the editor for the selected cell.

## Mode initialization

Edit in grid view is initialized in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) with the code:

```javascript
// aktivuj rezim uprava bunky / bubble
$(dataTableInit).on('click', 'table.dataTable tbody td', function (e) {
    if ($("body").hasClass("datatable-cell-editing")) {

    }
}
```

i.e. after clicking on a table cell. The combination of WebJET editor modifications that adds division of fields into sheets and at the same time preserves the editor dialog in the ```DOM``` tree even after it is closed is problematic. In this combination, the display of fields in the ```bubble``` editor was incorrect (all fields in the current sheet were displayed). For this reason, when editing a cell, the CSS class ```show``` is set only to the attribute that is currently being edited.

Using the CSS class ```div.DTE_Bubble``` in [_modal.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_modal.scss) the display of all ```div.tab-pane``` sheets (not only the active one) is set and all ```div.row``` that do not have the ```show``` class set are hidden. In [_table.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_table.scss) the cell highlighting is set for ```:hover``` in ```body.datatable-cell-editing``` mode.

Getting the name of the edited attribute was complicated, but it is finally implemented with the following code:

```javascript
const colIndex = TABLE.cell(this).index().column;
let datatableColumn = TABLE.settings().init().columns[colIndex];
let columnName = datatableColumn.name;
```

The key is to get the cell serial number from the currently clicked ```TD``` element. The attribute name is stored in the ```columnName``` variable. The ```show``` class setting is then provided by the code:

```javascript
$("div.DTE_Field").removeClass("show");
$("div.DTE_Field_Name_"+columnName).addClass("show");
```

Before opening the cell editor itself, a REST service is called to retrieve the row data. Often, the data table in the list does not contain all the data needed for the editor (to optimize loading speed), the ```fetchOnEdit=true``` attribute is used in the data table configuration. Therefore, after clicking on a cell, the ```refreshRow``` function is always called to retrieve the current and complete row data (in the REST service, ```ProcessItemAction.GETONE``` is used).

The editor itself is called via the ```EDITOR.bubble($(this), {``` API and is now standard, only the value of the ```submit``` option has been changed to the value ```all``` for sending all row attributes (not just the value of one attribute).

## Non-editable cells

Cells that cannot be edited (e.g. ID, read-only cells) have the CSS class ```cell-not-editable``` set. This class can be set in the annotation ```@DatatableColumn.className```. It is automatically set in [DatatableColumn.setCellNotEditable](../../../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumn.java) for fields of type:

- ```hidden``` - ​​fields of type ```hidden```
- ```textarea``` - ​​it seemed unreasonable to us to display a large text area in a small ```bubble``` editor
- ```editor.disabled``` - ​​fields that have the ```disabled``` attribute set to the value ```disabled```