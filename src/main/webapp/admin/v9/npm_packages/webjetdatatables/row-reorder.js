//static functions for row-reorder functionality

export function setRowReorderData(DATA) {
    //check if we have row-reorder column
    DATA.rowReorder = false;
    DATA.rowReorderDataSrc = null;
    var colIndex = 0;
    $.each(DATA.columns, function (key, col) {
        if (typeof col.renderFormat !== "undefined" && col.renderFormat != null && col.renderFormat.indexOf("dt-format-row-reorder")!=-1) {
            DATA.rowReorder = true;
            DATA.rowReorderDataSrc = col.data;
            DATA.order = [[colIndex, 'asc']]; //set order by row-reorder column
            return false; //break the loop
        }
        colIndex++;
    });

    if (DATA.rowReorder === true) {
        //disable row ordering on all columns
        $.each(DATA.columns, function (key, col) {
            col.orderable = false;
        });

        //add window opened listener
        window.addEventListener("WJ.DTE.opened", function(e) {
            if (e.detail.dte.TABLE.DATA.id === DATA.id) {
                //console.log("WJ.DTE.opened, rowReorderDataSrc=", DATA.rowReorderDataSrc);
                setNewReorderValue(e.detail.dte.TABLE);
            }
        });
    }
}

export function getRowReorderConfig(DATA) {
    //configure DATA object for row-reorder
    setRowReorderData(DATA);

    let rowReorder = {
        enable: false
    };

    //if we have row-reorder column, then enable row-reorder
    if (true === DATA.rowReorder) {
        rowReorder = {
            enable: true,
            selector: 'span.row-reorder-handler',
            //this will break drag cancelable: true,
            snapX: true,
            dataSrc: DATA.rowReorderDataSrc
        };
    }
    return rowReorder;
}

export function renderRowReorder(td, type, rowData, row, dtConfig) {
    //add reorder handler for drag drop reordering of rows
    return '<span class="row-reorder-handler"><i class="ti ti-caret-up-down"></i></span>' + dtConfig.renderTd(row, td, rowData);
}

/**
 * Set rowOrder value for new item to be added as last item in the table.
 * @param {*} TABLE
 */
export function setNewReorderValue(TABLE, force=false) {
    if (TABLE.DATA.rowReorder === true) {
        let fieldValue = TABLE.EDITOR.field(TABLE.DATA.rowReorderDataSrc).get();
        if (true === force || (fieldValue != null && typeof fieldValue !== "undefined" && fieldValue == "")) {
            //iterate datatable rows and get new rowOrder value
            let rowOrder = 0;
            let maxRowOrder = 0;
            TABLE.rows().every(function (rowIdx, tableLoop, rowLoop) {
                //get row data
                let rowData = this.data();
                //set rowOrder value
                if (typeof rowData[TABLE.DATA.rowReorderDataSrc] !== "undefined") {
                    rowOrder = parseInt(rowData[TABLE.DATA.rowReorderDataSrc]);
                    if (!isNaN(rowOrder) && rowOrder > maxRowOrder) {
                        maxRowOrder = rowOrder; //get max rowOrder value
                    }
                }
            });
            //set datatable editor field to new rowOrder
            TABLE.EDITOR.field(TABLE.DATA.rowReorderDataSrc).set(maxRowOrder + 10);
        }
    }
}