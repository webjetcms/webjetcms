 //What kind page is calling
export const PageType = {
    Components: "components", //server_monitoring/admin/components/
    Documents: "documents", //server_monitoring/admin/documents/
    Sql: "sql" //server_monitoring/admin/sql/
}

/**
 * Based on PageType, filter list of columns from input . Filtred columns return.
 * info what columns are good to return for each page is defined inside of function.
 * @param {[Object]} inputColumns
 * @param {PageType} pageType
 * @returns
 */
export function getFilteredColumns(inputColumns, pageType) {
    let filteredColumns = [];
    let filterColumnNames;

    if(pageType == PageType.Sql) {
        filterColumnNames = [
            "whatWasExecuted",
            "numberOfHits",
            "averageExecutionTime",
            "maximumExecutionTime",
            "minimumExecutionTime",
            "totalTimeOfExecutions"
        ];
    } else if(pageType == PageType.Documents) {
        filterColumnNames = [
            "whatWasExecuted",
            "numberOfHits",
            "averageExecutionTime",
            "maximumExecutionTime",
            "minimumExecutionTime",
            "totalTimeOfExecutions",
            "totalMemoryConsumed",
            "memoryConsumptionPeek"
        ]
    } else if(pageType == PageType.Components) {
        filterColumnNames = [
            "whatWasExecuted",
            "numberOfHits",
            "averageExecutionTime",
            "numberOfCacheHits",
            "maximumExecutionTime",
            "totalTimeOfExecutions",
            "totalMemoryConsumed",
            "memoryConsumptionPeek"
        ];
    }

    for(let i = 0; i < inputColumns.length; i++)
        if(filterColumnNames.includes(inputColumns[i]["name"])) filteredColumns.push(inputColumns[i]);

    return filteredColumns;
}

/**
 * Our custom delete button show only if selected NODE is current node. (aka with value "Aktuálny uzol")
 */
export function changeButtonVisibility() {
    var selectedNodeValue = $('#nodesFilterSelect').find(":selected").text();
    var isEmpty = (selectedNodeValue == undefined || selectedNodeValue == null || selectedNodeValue == "") ? true : false;
    if(isEmpty || selectedNodeValue.includes(WJ.translate("monitoring.actual_node.js"))) {
        $("button.btn-danger").show();
        $("button.btn-danger.buttons-divider").hide(); //This idiot gonna show up (original delete button), so hide him
    } else
        $("button.btn-danger").hide();
}

/**
 * Tranform input list of strings into nodesFilterSelect options.
 * Firt options will be marked as "Aktuálny uzol", and this option will be auto selected.
 * Do a select refresh.
 * @param {[String]} listOfNodes
 */
export function setNodesFilterSelect(listOfNodes) {
    //
    let nodesFilterSelect = document.getElementById('nodesFilterSelect');
    //
    while(nodesFilterSelect.options.length > 1)
        nodesFilterSelect.remove(1);
    //
    for(let i = 0; i < listOfNodes.length; i++) {
        //First node is allways actual node
        if(i == 0) {
            let nodeName = listOfNodes[i];
            nodeName += " (" + WJ.translate("monitoring.actual_node.js") + ")";
            nodesFilterSelect.add(new Option(nodeName, listOfNodes[i]));
        } else
            nodesFilterSelect.add(new Option(listOfNodes[i], listOfNodes[i]));
    }
    //Refresh object
    $("#nodesFilterSelect").selectpicker('refresh');
}

/**
 * Prepare DataTable buttons.
 *
 * Add custom delete and custom refresh refresh button.
 *
 * @param {DataTable} dataTable
 * @param {PageType} pageType
 */
export function prepareButtons(dataTable, pageType) {
    //vypni zobrazenie tlacidiel import, create, edit ...
    dataTable.hideButton("import");
    dataTable.hideButton("create");
    dataTable.hideButton("edit");
    dataTable.hideButton("duplicate");
    dataTable.hideButton("celledit");
    dataTable.hideButton("remove");
    dataTable.hideButton("reload");

    var buttonCounter = 1;
    //Reset actual data button
    dataTable.button().add(buttonCounter++, {
        text: '<i class="ti ti-trash"></i>',
        action: function (e, dt, node) {
            var selectedNodeValue = $('#nodesFilterSelect').val();
            $.ajax({url: urlBase + "/resetData?showType=" + pageType + "&selectedNode=" + selectedNodeValue, success: function() {
                dataTable.ajax.reload();
            }});
        },
        attr: {
            title: WJ.translate('button.delete'),
            "data-toggle": "tooltip",
            "data-dtbtn": "remove"
        },
        className: 'btn btn-sm buttons-selected buttons-remove btn-danger'
    });

    //Get actual data button
    dataTable.button().add(buttonCounter++, {
        text: '<i class="ti ti-refresh"></i>',
        action: function (e, dt, node) {
            //refresh data
            refreshData(dataTable, pageType);
        },
        attr: {
            title: WJ.translate('datatables.button.reload.js'),
            "data-toggle": "tooltip",
            "data-dtbtn": "reload"
        },
        className: 'btn btn-sm btn-outline-secondary buttons-refresh'
    });
}

/**
 * Call a refresh cluster based on selected node.
 */
function clusterRefresh() {
    var selectedNodeValue = $('#nodesFilterSelect').val();
    //console.log("Calling cluster refresh - START");
    $.ajax({url: urlBase + "/refreshData?selectedNode=" + selectedNodeValue,
        success: function() {
            //console.log("Calling cluster refresh - END (succesfull)");
        },
        error: function () {
            //console.log("Calling cluster refresh - FAILED");
        }
    });
}

/**
 * Do a refresh based on selectedNode and PageType.
 * Call for data untill obtain them, every 10 seconds (in interval)-
 * If selectedNode ins§t current, do allso cluster refresh request.
 * Repeat all necessary steps until obtain ony data.
 * @param {DataTable} dataTable
 * @param {PageType} pageType
 * @returns
 */
function refreshData(dataTable, pageType) {
    var selectedNodeValue = $('#nodesFilterSelect').val();
    let numberOfRefshes = 0;

    //If it's actual node, just get actual data, no need to refresh cluster
    if($('#nodesFilterSelect').find(":selected").text().includes(WJ.translate("monitoring.actual_node.js"))) {
        getActualData(null, pageType, selectedNodeValue, dataTable);
        return;
    }

    //Show loader and notification about getting data
    WJ.showLoader();
    WJ.notifyInfo(WJ.translate("monitoring.get_data_title.js"), WJ.translate("monitoring.get_data_message.js", clusterRefreshTime), 10000);

    //Refresh cluster and remove actual data from DataTable
    clusterRefresh();
    dataTable.DATA.json = [];
    dataTable.ajax.reload();

    //Do reload every 10 seconds
    var interval = setInterval(function() {
        //Every 1 minute do cluster refresh (ask for actual data)
        //This is for case when diff node return no data (we wil ask until we get at least something)
        if(numberOfRefshes != 0 && (numberOfRefshes % 10) == 0) clusterRefresh();

        //console.log("Calling for data - START");
        getActualData(interval, pageType, selectedNodeValue, dataTable);

        //Increment
        numberOfRefshes++;
    }, 10000);
}

/**
 * Get actual data based on pageType and selectedNodeValue.
 * If data.length > 0, insert data into dataTable and refresh table. Clear intervla.
 * Else nothing.
 * @param {setInterval} interval created intervla
 * @param {PageType} pageType
 * @param {String} selectedNodeValue
 * @param {DataTable} dataTable
 */
function getActualData(interval, pageType, selectedNodeValue, dataTable) {
    $.ajax({url: urlBase + "/all?showType=" + pageType + "&selectedNode=" + selectedNodeValue,
        success: function(response) {
            //Check if we get at least 1 record to show
            if(response["content"].length > 0) {
                //console.log("Calling for data - ACQUIRED");

                WJ.hideLoader();

                if(interval != null)
                    clearInterval(interval);

                //console.log("***INTERVAL STOPED***");

                dataTable.DATA.json = response["content"];
                dataTable.ajax.reload();
            } else {
                //No data for us
                //console.log("Calling for data - EMPTY");
            }
        },
        error: function () {
            //console.log("Calling for data - FAILED");
        }
    });
}