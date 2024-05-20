# Modifying the cell

Datatables support direct cell editing in edit mode `bubble`. To switch on the mode, click the **Modifying the cell** in the toolbar. Then click on the cell to display the dialog box with the editor of the selected cell.

## Mode initialization

The cell modification is initialized in [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) Code:

```javascript
// aktivuj rezim uprava bunky / bubble
$(dataTableInit).on('click', 'table.dataTable tbody td', function (e) {
    if ($("body").hasClass("datatable-cell-editing")) {

    }
}
```

i.e. after clicking on a table cell. Problematic is the combination of WebJET editor editing, which adds splitting fields into sheets while keeping the editor dialog in `DOM` the tree even after it is closed. In this combination, the display of the fields in the `bubble` editor was incorrect (all fields in the current sheet were displayed). For this reason, when editing a cell, the CSS class is set `show` only on the currently edited attribute.

Using CSS classes `div.DTE_Bubble` v [\_modal.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_modal.scss) is set to display all sheets `div.tab-pane` (not only active) and at the same time all `div.row`that do not have a set `show` Class. V [\_table.scss](../../../src/main/webapp/admin/v9/src/scss/3-base/_table.scss) cell highlighting is set at `:hover` in mode `body.datatable-cell-editing`.

Getting the name of the edited attribute was complicated, finally it is implemented by the following code:

```javascript
const colIndex = TABLE.cell(this).index().column;
let datatableColumn = TABLE.settings().init().columns[colIndex];
let columnName = datatableColumn.name;
```

the key is to get the sequence number of the cell from the currently clicked `TD` element. Into the variable `columnName` the name of the attribute is stored. Setting `show` class is subsequently secured by a code:

```javascript
$("div.DTE_Field").removeClass("show");
$("div.DTE_Field_Name_" + columnName).addClass("show");
```

Before opening the cell editor itself, the REST service is called to get the row data. Often the datatable in the list does not contain all the data needed for the editor (to optimize loading speed), the attribute is used `fetchOnEdit=true` in the datatable configuration. So after clicking on a cell, the function is always called `refreshRow` to get the current and complete row data (in the REST service it is used `ProcessItemAction.GETONE`).

Calling the editor itself via API `EDITOR.bubble($(this), {` is already standard, only the value of the option is modified `submit` to the value of `all` for sending all attributes of a row (not just the value of one attribute).

## Non-editable cells

Cells that cannot be edited (e.g. ID, read-only cells) have a CSS class set `cell-not-editable`. This class can be set in the annotation `@DatatableColumn.className`. Automatically set in [DatatableColumn.setCellNotEditable](../../../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumn.java) for fields of type:
- `hidden` - type fields `hidden`
- `textarea` - it seemed unreasonable to us in a small `bubble` editor to display a large text area
- `editor.disabled` - fields that have set `disabled` attribute to value `disabled`
