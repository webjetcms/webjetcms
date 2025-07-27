//static functions for footer summary functionality

export function bindEvents(TABLE) {
    //Bind footerCallback to table
    TABLE.on('column-reorder.dt', function () {
        //After column re-oder, we need to set columns SUM in footer again
        runFooterCallback(TABLE);
    });
    TABLE.on('column-visibility.dt', function (e, settings, column, state) {
        runFooterCallback(TABLE);
    });
}

export function runFooterCallback(TABLE) {
    //Invoke data draw action on table, so footerCallback will be called
    TABLE.draw(false);
}

export function footerCallback(TABLE) {
    //not initialized yet, skip
    if (typeof TABLE === "undefined" || TABLE == null) return;

    //Are summary setting set ?
    var DATA = TABLE.DATA;
    if(typeof DATA != "undefined" && DATA.summary !== null) {
        const mode = DATA.summary.mode;
        const columnsToSum = DATA.summary.columns;
        const title = DATA.summary.title;

        //create tr element
        let tr;
        if("all" === mode && DATA.serverSide === true) {
            //This one is tricky situation, BECAUSE we need all data but we are using server side processing
            //We need to call the server to get sum of needed columns
            tr = getTableFooterRowServerSide(TABLE, title, columnsToSum);
        } else {
            //Current DATA are sufficient, we can use them
            tr = getTableFooterRow(TABLE, title, columnsToSum, mode);
        }

        let tfoot = $("#" + DATA.id + "_wrapper").find(".dt-scroll-footInner > table > tfoot");
        if(tfoot != undefined && tfoot.length > 0) {
            tfoot.html(tr);
        }
    }
}

/**
 * Get the sum of a specific column in the DataTable
 * @param {*} api - datatable api instance
 * @param {*} columnId - index of the column to sum
 * @param {*} mode - mode of summation all (all data) / visible (only current page data)
 * @returns - the sum of the column values
 */
function getSum(api, columnId, mode) {
    // Remove the formatting to get integer data for summation
    let intVal = function (i) {
        return typeof i === 'string'
            ? i.replace(/[\$,]/g, '') * 1
            : typeof i === 'number'
            ? i
            : 0;
    };

    if("all" === mode) {
        return api
            .column(columnId)
            .data()
            .reduce((a, b) => intVal(a) + intVal(b), 0);
    } else if("visible" === mode) {
        return api
            .column(columnId, { page: 'current' })
            .data()
            .reduce((a, b) => intVal(a) + intVal(b), 0);
    } else {
        return "";
    }
}

/**
 * Return TR element aka footer row for datatable, that contain SUM of selected columns. SUM of column match column position.
 * If columnsToSum is empty, it will return empty row. Selected columns MUST have className dt-style-number.
 * !!! it only SUM datata available in datatable !!!
 *
 * @param {*} api - datatable api instance
 * @param {*} title - title for ID column, if set, it will be added position of ID column
 * @param {*} columnsToSum - array of column IDs to sum, if empty, it will return empty row
 * @param {*} mode - mode of summation, can be "all" (sum all data) or "visible" (sum only current page data)
 * @returns
 */
function getTableFooterRow(api, title, columnsToSum, mode) {
    //Table is not INIT yet, skip
    if(typeof api === "undefined" || api == null) return;

    let tr = $("<tr></tr>");
    let counter = 0;
    api.columns(':visible').every(function() {
        var column = this;
        var columnId = column.dataSrc();
        let td = $("<td></td>");

        if(counter === 0 && title !== undefined && title !== null && title !== "") {
            td.text(title);
        } else {
            //Check - we can SUM only number columns
            if(column.header().className.indexOf("dt-style-number") != -1) {
                //Check, that this column is selected to SUM
                if(columnsToSum != null && columnsToSum.length > 0 && columnsToSum.includes(columnId) == true) {
                    //OKK column is number, and belongs to DATA.summary.columns - return SUM of column
                    let b = $("<b></b>");
                    b.text(getSum(api, column.index(), mode));
                    td.append(b);
                    tr.append(td);
                    return;
                }
            }
        }

        //Is not number column or column to sum
        tr.append(td);
        counter++;
    });

    return tr;
}

/**
 * Return TR element aka footer row for datatable, that contain SUM of selected columns. SUM of column match column position.
 * If columnsToSum is empty, it will return empty row. Selected columns MUST have className dt-style-number.
 *
 * !!! DATA are obtained from BE (datatabse) !!!
 *
 * @param {*} api - datatable api instance
 * @param {*} title - title for ID column, if set, it will be added position of ID column
 * @param {*} columnsToSum - array of column IDs to sum, if empty, it will return empty row
 * @returns
 */
function getTableFooterRowServerSide(api, title, columnsToSum) {
    //Table is not INIT yet, skip
    if(typeof api === "undefined" || api == null) return;

    let realColumnsToSum = [];
    //Count only visible columns that have className dt-style-number
    api.columns(':visible').every(function() {
        var column = this;
        var columnId = this.dataSrc();

        //Check - we can SUM only number columns
        if(column.header().className.indexOf("dt-style-number") != -1) {
            //Check, that this column is selected to SUM
            if(columnsToSum != null && columnsToSum.length > 0 && columnsToSum.includes(columnId) == true) {
                //OKK column is number, and belongs to DATA.summary.columns - return SUM of column
                realColumnsToSum.push(columnId);
            }
        }
    });

    //Get columns SUM value from BE
    //console.log("getTableFooterRow2, realColumnsToSum=", realColumnsToSum, "api.DATA=", api.DATA);
    let URL = api.DATA.url;
    URL = WJ.urlAddPath(URL, "/sumAll");

    let tr = $("<tr></tr>");
    $.get({
        url: URL,
        data: {
            columns: realColumnsToSum
        },
        success: function (json) {
            json = JSON.parse(json);

            api.columns(':visible').every(function() {
                var column = this;
                var columnId = column.dataSrc();
                let td = $("<td></td>");

                if("id" === columnId) {
                    //Special column, if TITLE is set, we add it
                    if(title !== undefined && title !== null) {
                        td.text(title);
                        tr.append(td);
                    } else {
                        //If TITLE is not set, we add an empty cell
                        tr.append(td);
                    }
                    return;
                }

                if(columnId in json) {
                    let b = $("<b></b>");
                    b.text(json[columnId]);
                    td.append(b);
                    tr.append(td);
                    return;
                } else {
                    tr.append(td);
                }
            });

        },
        error: function (xhr, ajaxOptions, thrownError) {
            console.log("Error, xhr=", xhr, " ajaxOptions=", ajaxOptions, " thrownError=", thrownError);
        },
    });

    return tr;
}