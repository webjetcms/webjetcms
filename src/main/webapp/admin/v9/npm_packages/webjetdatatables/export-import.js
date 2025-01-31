//potrebuje to import
var Buffer = require('buffer/').Buffer;

export function bindExportButton(TABLE, DATA) {

    //console.log("bindExportButton, TABLE=", TABLE, "DATA=", DATA);

    async function getDataToExport(serverSide, TABLE, pageVal, searchVal, orderVal, fromLastExport, searchIds) {
        //console.log("getDataToExport, serverSide=", serverSide);

        //const {name} = window.VueTools.getRouter().history.current.params;
        //const all = name ? '' : '/all';
        //const allData = exportAllData ? '?size=999999&page=0&export=true' : '';
        //const url = `${TABLE.getAjax.url()}${all}${allData}`;

        let url = DATA.urlLatest;
        let restParams = [];
        restParams.push({name: "export", value: true});
        if (fromLastExport != null) restParams.push({name: "fromLastExport", value: fromLastExport});
        if (searchIds != null) {
            restParams.push({name: "id", value: searchIds});
        }

        var maxRows = window.datatablesExportMaxRows;
        if (typeof maxRows == "undefined") maxRows = 50000;

        if (serverSide==false) {
            //get data from datatable
            var localData = {
                "content": TABLE.rows().data()
            };
            return localData;
        } else {
            for (let i = 0; i < DATA.urlLatestParams.length; i++) {
                let param = DATA.urlLatestParams[i];
                if ("all"==pageVal) {
                    if ("size" === param.name) {
                        param.value = maxRows;
                    }
                    if ("page" === param.name) {
                        param.value = 0;
                    }
                }
                if ("none" === searchVal && param.name.indexOf("search") === 0) {
                    //odstranujeme search parametre pre moznost Exportovat vsetky data
                    continue;
                }
                if ("original" === orderVal && param.name.indexOf("sort") === 0) {
                    //odstranujeme sort parametre pre moznost Zakladne zoradenie dat
                    continue;
                }
                restParams.push(param);
            }
        }

        //skontroluj externe filtre
        try {
            for (var i = 0; i < DATA.columns.length; i++) {
                var column = DATA.columns[i];
                //over, ci existuje input v extfiltri
                if ($("#" + DATA.id + "_extfilter .dt-extfilter-" + column.name).length > 0) {
                    var value = column.searchVal;
                    if (typeof value !== "undefined" && value !== "") {
                        restParams.push({name: "search" + column.name, value: value});
                    }
                }
            }
        } catch (e) {console.log(e);}

        //console.log("restParams=", restParams, "url=", url);

        const data = await $.get({
            "dataType": 'json',
            "type": "GET",
            "url": url,
            "data": restParams
        });

        return data;
    }

    function fixValue(value, dc, optionsTable) {
        if (dc.hasOwnProperty("renderFormat") && typeof dc.renderFormat != "undefined") {
            //konverzia hodnoty, je to takto schvalne, aby to bolo podstatne vykonnejsie ako pouzitie columnsDef z DT
            if (dc.renderFormat.indexOf("date-time") !== -1) {
                value = moment(value).format("L HH:mm:ss");
            } else if (dc.renderFormat.indexOf("date") !== -1) {
                value = moment(value).format("L");
            } else if (dc.renderFormat.indexOf("-select") !== -1 || dc.renderFormat.indexOf("-checkbox") !== -1) {
                var hashtableValue = optionsTable[dc.data + "-" + value];
                //console.log("hashtableValue=", hashtableValue, " value=", value);
                if (typeof hashtableValue !== "undefined") value = hashtableValue;
            } else if (dc.renderFormat.indexOf("-boolean") !== -1) {
                if (value === true) value = WJ.translate('button.yes');
                else value = WJ.translate('button.no');
            }

            if (typeof value === "string" && value.match(/^\-?[\d,]+$/) && dc.renderFormat.indexOf("dt-format-text") !== -1) {
                //if it's number and type is text add space at the beginning, it will be trimmed durring import
                //http://mail.datatables.net/forums/discussion/46132/excel-export-convert-number-to-string-while-exporting
                value = "\0"+value;
            }
        }
        return value;
    }

    function isNotExportable(dc) {
        if ("datatable"==dc.type || (typeof dc.className != "undefined" && dc.className.indexOf("not-export")!=-1)) return true;
        //for translation-keys/custom-fields will not export columns with null label
        if (dc.label == null || dc.label == "null") return true; //NOSONAR

        return false;
    }

    var exportButtonsEventsBinded = false;
    $("#datatableExportModal").off("show.bs.modal");
    $("#datatableExportModal").on("show.bs.modal", function () {
        if (exportButtonsEventsBinded==false) {
            exportButtonsEventsBinded = true;

            //append custom export options
            var customOptions = $("#datatableExportModalCustomOptions");
            //console.log("customOptions=", customOptions);
            if (customOptions.length > 0) {
                $("#datatableExportModalOptions").prepend(customOptions);
                customOptions.show();
            }
        }
        $("#datatableExportModalOptions").find("input").first().trigger("click");
    });

    $(".dt-settings-export-printsettings").toggle(false);
    $("#datatableExportModal input[name=dt-settings-extend]").off("click");
    $("#datatableExportModal input[name=dt-settings-extend]").on("click", function (e) {
        let value = e.target.value;
        //console.log("CLICK value=", value);
        if ("print" === value) {
            $(".dt-settings-export-printsettings").toggle(true);
        } else {
            $(".dt-settings-export-printsettings").toggle(false);
            $(".dt-settings-message-top").val("");
            $(".dt-settings-message-bottom").val("");
        }
    });

    //NO checked by tableId $('#datatableExportModal').off("click", ".btn-primary");
    $('#datatableExportModal').on("click", ".btn-primary", function () {
        //console.log("EXPORT MODAL CLICK, id=", window.datatableExportModal.tableId, "tableID=", TABLE.DATA.id);

        //export modal je staticky, ak mame viac DT robime export len pre konkretnu, hodnota sa nastavuje pri kliknuti na export tlacidlo
        if (window.datatableExportModal.tableId !== TABLE.DATA.id) {
            return;
        }

        var exportToastrTitle = WJ.translate('datatables.export.title.js');

        var pageVal = $("#datatableExportModal input[name=dt-settings-page]:checked").val();
        var searchVal = $("#datatableExportModal input[name=dt-settings-search]:checked").val();
        var orderVal = $("#datatableExportModal input[name=dt-settings-order]:checked").val();
        var columnsVal = $("#datatableExportModal input[name=dt-settings-columns]:checked").val();
        var extendVal = $("#datatableExportModal input[name=dt-settings-extend]:checked").val();
        var orientationVal = $("#datatableExportModal input[name=dt-settings-orientation]:checked").val();
        var selectedVal = false;
        var fromLastExport = null;
        var searchIds = null;

        if (extendVal.indexOf("custom")==0 && typeof window.exportDialogCustomCallback === "function") {
            var closeDialog = window.exportDialogCustomCallback(extendVal, TABLE);
            if (closeDialog) $("#datatableExportModal").modal("hide");
            return;
        }

        if ("lastExport" === searchVal) {
            searchVal = "none";
            fromLastExport = DATA.lastExportColumnName;
        }

        if ("selected" === searchVal) {
            pageVal = "current";
            searchVal = "applied";
            selectedVal = true;

            let selectedRows = TABLE.rows({ selected: true }).data();
            for (let i=0; i<selectedRows.length; i++) {
                let row = selectedRows[i];
                let id = row[DATA.byIdExportColumnName];
                //console.log("Getting rows to export, row=", row, "id=", id);
                if (searchIds == null) searchIds = [];
                searchIds.push(id);
                //console.log("searchIds=", searchIds);
            }
        }

        Array.prototype.insert = function (index, item) {
            this.splice(index, 0, item);
        };
        let formatedData = null;
        let optionsTable = dtWJ.getOptionsTableExport(DATA);
        //console.log("Export optionsTable=", optionsTable);
        //console.log("extendVal=", extendVal, "columnsVal=", columnsVal, "pageVal=", pageVal, "searchVal=", searchVal, "orderVal=", orderVal, "selectedVal=", selectedVal, " title=", $(".dt-settings-title").val(), " messageTop=", $(".dt-settings-message-top").val(), " messageBottom=", $(".dt-settings-message-bottom").val());

        TABLE.button().add(0, {
            extend: extendVal,
            text: WJ.translate('datatables.button.export.js'),
            title: function () {
                //title je zakomentovane v HTML, nepouziva sa, vratime teda NULL aby sa zbytocne negeneroval riadok do XLS
                return $(".dt-settings-title").val() || "";
            },
            messageTop: function () {
                return $(".dt-settings-message-top").val();
            },
            messageBottom: function () {
                return $(".dt-settings-message-bottom").val();
            },
            filename: function () {
                return $(".dt-settings-filename").val();
            },
            orientation: orientationVal,
            className: 'exportujem',
            exportOptions: {
                columns: columnsVal,
                modifier: {
                    selected: selectedVal,
                    page: pageVal,
                    search: searchVal,
                    order: orderVal
                },
                customizeData: function (d) {
                    //console.log("customizeData, pageVal=", pageVal, "serverSide=", DATA.serverSide, "d=", d, "formatedData=", formatedData, "count=", d.headerStructure[0].length);
                    if (formatedData != null) {
                        while (d.body.length > 0) {
                            d.body.pop();
                        }
                        d.body.push.apply(d.body, formatedData);
                    }
                    //uprav header na polia podla editora
                    while (d.header.length > 0) {
                        d.header.pop();
                        d.headerStructure[0].pop();
                    }
                    while (d.headerStructure[0].length > 0) {
                        d.headerStructure[0].pop();
                    }
                    DATA.fields.forEach((dc, index) => {
                        //console.log(index, "header dc=", dc);
                        if (isNotExportable(dc)) return;
                        //ak sa jedna o excel dopln do headra aj meno fieldu
                        let headerTitle = dc.label;
                        if (extendVal === "excelHtml5" || extendVal === "csvHtml5") headerTitle += "|" + dc.name;
                        d.header.push(headerTitle);
                        d.headerStructure[0].push({
                            title: headerTitle,
                            colspan: 1,
                            rowspan: 1
                        });
                    });
                    d.headerStructure.splice(1, 1);
                    //console.log("d fixed=", d);
                }
            },
            autoFilter: true,
            createEmptyCells: true
        });

        if (DATA.serverSide) {
            WJ.notifyInfo(exportToastrTitle, WJ.translate('datatables.export.loading.info.js'), 5000);
        }

        getDataToExport(DATA.serverSide, TABLE, pageVal, searchVal, orderVal, fromLastExport, searchIds).then(response => {

            //console.log("Data to export: ", response, "formatedData=", formatedData);

            if (response) {
                //stiahli sa vsetky data zo servera
                WJ.notifyInfo(exportToastrTitle, WJ.translate('datatables.export.format_data.info.js'), 5000);

                const {content} = response;
                //console.log("response=", response, " content=", content);
                formatedData = content.map(c => {
                    let arr = new Array(DATA.fields.length);

                    let rowData = c;

                    if (DATA.id === 'form-detail') {
                        //console.log("c.columnNamesAndValues=", c.columnNamesAndValues);
                        //pre formulare su data v columnNamesAndValues
                        Object.keys(c.columnNamesAndValues).forEach(function(key,index) {
                            //console.log("fixing column names, key=", key, "value=", c.columnNamesAndValues[key]);
                            var value = c.columnNamesAndValues[key];
                            try {
                                //if value contains only numbers, comma, dash convert comma to dot
                                if (typeof value === "string" && value.match(/^\-?[\d,]+$/) && value.indexOf(",")!=-1) {
                                    value = value.replace(",", ".");
                                }

                                //if the value is number convert it to number object, skip if it starts with 0 or contains + (probably phone number)
                                if (typeof value === "string" && value.indexOf("0")!=0 && value.indexOf("+")==-1 && !isNaN(value)) {
                                    var converted = Number.parseFloat(value);
                                    //console.log("Converting to number["+index+"]: ", value, "converted=", converted);
                                    if (!isNaN(converted)) value = converted;
                                }
                            } catch (e) {
                                console.log(e);
                            }
                            if ("0"===value) value = 0;

                            rowData["col_"+key] = value;
                        });

                        //odstran NULL hodnoty, pretoze tie su potom nahradene za vyraz nevyplnene, co vo formoch nechceme
                        Object.keys(rowData).forEach(function(key,index) {
                            if (rowData[key] == null) rowData[key] = "";
                        });

                        delete rowData.columnNamesAndValues;
                        //console.log("Export rowData:", rowData);
                    }

                    let index = 0;
                    DATA.fields.forEach((dc) => {
                        //console.log("dc=", dc, "index=", index);
                        if (isNotExportable(dc)) return;

                        let key = dc.data;

                        if ((":not(.not-export)" === columnsVal || TABLE.column(dc.name + ":name").visible())) {
                            let value = WJ.getJsonProperty(rowData, key);
                            //window.rowData = rowData;
                            //console.log("rowData=", rowData);
                            //console.log("key=", key, " arr, index=", index, " value=", value, " visible=", TABLE.column(dc.name+":name").visible(), "dc=", dc, "typeof value=", typeof value);

                            if (dc.hasOwnProperty("type") && "json" === dc.type) {
                                //console.log("json export, dc=", dc, "value=", value);
                                let className = dc.className;
                                if (typeof className == "undefined" || className == null) className = "";

                                if (value != null ) {
                                    let stringData = [];
                                    let iterated = false;

                                    //skonvertuj aby to bolo vzdy pole aj pre jednu polozku
                                    let valueArr = value;
                                    if (className.indexOf("-array")==-1) valueArr = [value];

                                    if (className.indexOf("dt-tree-page")!=-1 || className.indexOf("dt-tree-group")!=-1 || className.indexOf("dt-tree-dir")!=-1) {
                                        valueArr.forEach( (v, i) => {
                                            //console.log("ITERATING className=", className, "v=", v);

                                            var domainName = (v.domainName && v.domainName != '' && className.indexOf('alldomains')!=-1 ? v.domainName+':' : '');

                                            if (className.indexOf("dt-tree-dir")!=-1) stringData.push(domainName+v.virtualPath);
                                            else stringData.push(domainName+v.fullPath);

                                            iterated = true;
                                        });
                                    }

                                    //ak sa vyhodnotila podmienka setni data, tie co nepozname tak pridu ako JSON
                                    if (iterated) {
                                        //ak je to array daj cele pole, inak prvu polozku, alebo null ak je prazdne
                                        if (className.indexOf("-array")!=-1) value = stringData;
                                        else if (stringData.length>0) value = stringData[0];
                                        else value = null;
                                    }
                                }

                                value = JSON.stringify(value);
                            } else {
                                if (dc.hasOwnProperty("array") && dc.array == true) {
                                    if (value != null) {
                                        let valueString = "";
                                        value.forEach( (v, i) => {
                                            //console.log("IS ARRAY, v=", v);
                                            v = fixValue(v, dc, optionsTable);
                                            if (valueString == "") valueString = v;
                                            else valueString += ","+v;
                                        });
                                        value = valueString;
                                    }
                                } else {
                                    value = fixValue(value, dc, optionsTable);
                                    if (value===null) value = WJ.translate('datatables.export.empty.js');
                                }
                            }
                            //console.log("key=", key, " arr[", index, "]=", value);
                            arr[index] = value;
                            index++;
                        }
                    })
                    //console.log("arr1=", arr, "empty=", WJ.translate('datatables.export.empty.js'));
                    //toto odmazalo prazdne hodnoty a potom nesedeli stlpce
                    //arr = arr.filter(el => el === null ? WJ.translate('datatables.export.empty.js') : el);
                    //console.log("arr2=", arr);
                    return arr;
                });
                //console.log("formatedData2=", formatedData);
            }
            //console.log(".exportujem btn=", $( ".exportujem" ));
            $(".exportujem").trigger("click");
            WJ.notifySuccess(exportToastrTitle, WJ.translate('datatables.export.ready.js'), 5000);
            setTimeout(function () {
                //je to cez timeout, aby to nepadalo neskor, ze tam chce zrusit ajax indikator
                TABLE.buttons('.exportujem').remove();
                $("#datatableExportModal").modal("hide");
            }, 500);
        });
    });

    $('#datatableExportModalOptions').off("click", "input");
    $('#datatableExportModalOptions').on("click", "input", function (event) {
        //console.log("click, event=", event, "this=", this);
        var $this = $(this);
        $("#datatableExportModal").find(".hidden").removeClass("hidden");
        var hideElements = $this.attr("data-hide");
        if (typeof hideElements !== "undefined" && hideElements !== false) {
            var hideElementsArr = hideElements.split(",");
            hideElementsArr.forEach(function (e) {
                $(e).addClass("hidden");
            });
        }
    });

}


export function bindImportButton(TABLE, DATA) {

    ////NO checked by tableId $("#datatableImportModal").off("click", ".btn-primary");
    $("#datatableImportModal").on("click", ".btn-primary", async () => {
        //console.log("IMPORT MODAL CLICK, id=", window.datatableImportModal.tableId, "tableID=", TABLE.DATA.id);

        //import modal je staticky, ak mame viac DT robime export len pre konkretnu, hodnota sa nastavuje pri kliknuti na export tlacidlo
        if (window.datatableImportModal.tableId !== TABLE.DATA.id) return;

        var extendVal = $("#datatableImportModal input[name=dt-settings-extend]:checked").val();
        if (extendVal.indexOf("custom")==0 && typeof window.importDialogCustomCallback === "function") {
            var closeDialog = window.importDialogCustomCallback(extendVal, TABLE);
            if (closeDialog) $("#datatableImportModal").modal("hide");
            return;
        }

        $('#submit-import').prop('disabled', true);
        $("#import-status").html("0%");
        $('#submit-import i').attr("class", "ti ti-refresh ti-spin");

        function sendAjax() {
            var fixedUrl = url;
            if (dzchunkindex === 1 && importMode === "deleteimport") fixedUrl = WJ.urlAddParam(fixedUrl, "deleteOldData", "true");
            return $.ajax({
                type: 'POST',
                url: fixedUrl,
                data: JSON.stringify(formData),
                processData: false,
                /* s contentType na false to nefungovalo vobec vracalo unsuported media type (415) */
                // contentType: false,
                contentType: "application/json; charset=utf-8"
            });
        }

        async function importData() {
            let index = 0;
            for (let data of mainData) {
                //mainData.forEach( async (data, index) => {
                //console.log(counter);
                counter += 1;
                countedData.push(data);
                //console.log("counter="+counter+" chunks="+chunks+" index="+index+" mainData.length - 1="+(mainData.length - 1));

                //data posielame postupne, maximalne naraz chunks objektov
                if (counter === chunks || (index === mainData.length - 1 && counter < chunks)) {
                    counter = 0;
                    const readyData = {...countedData};

                    formData['action'] = action;
                    formData['data'] = readyData;
                    formData['dzchunkindex'] = dzchunkindex;
                    //console.log("stringify=", JSON.stringify(readyData.data), "readyData=", readyData);
                    formData['dzchunksize'] = Buffer.byteLength(JSON.stringify(readyData));
                    formData['importedColumns'] = importedColumns;
                    formData['importMode'] = importMode;
                    formData["updateByColumn"] = updateByColumn;
                    formData["skipWrongData"] = skipWrongData;

                    dzchunkindex += 1;
                    //console.log("countedData"+dzchunkindex+"=", countedData);

                    const response = await sendAjax(countedData);
                    //console.log("After await");
                    finishCounter += 1;
                    file.upload.progress = Math.round(((index + 1) / (mainData.length)) * 100);
                    //console.log("progress=", file.upload.progress, " file=", file, " sizeOfChunk=", sizeOfChunk, "index=", index, "mainData.length=", mainData.length);

                    setTimeout(()=> {
                        $("#import-status").html((index+1)+"/"+mainData.length+", "+file.upload.progress+"%");
                    }, 100);
                    //console.log(file.upload.progress+"%");

                    $(document).trigger('initUploadProgressFromOutside', file);
                    if (response.error) {
                        TABLE.ajax.reload();

                        let errorMessage = WJ.parseMarkdown(response.error, false);

                        //dogeneruj fieldErrors ak existuju
                        if (response.fieldErrors) {
                            response.fieldErrors.forEach((item, index) => {
                                if (errorMessage.indexOf(item.status)==-1) {
                                    var fieldError = item.name+" - "+item.status
                                    errorMessage += "<br/>"+fieldError;
                                }
                            });
                        }

                        $("#datatableImportModal div.DTE_Form_Error").html(errorMessage);
                        $('#submit-import i').attr("class", "ti ti-check");

                        throw new Error(response.error);
                    }
                    if (response.notify) {
                        response.notify.forEach((item, index) => {
                            //console.log("iterating, item=", item);
                            WJ.notify(item.type, item.title, item.text, item.timeout, item.buttons, true);
                        });
                    }
                    //console.log("finishCounter="+finishCounter+" formData['dztotalchunkcount']="+formData['dztotalchunkcount']);
                    if (finishCounter === formData['dztotalchunkcount']) {
                        //console.log("FINISHING, file=", file, " file.upload=", file.upload);
                        file.upload.progress = 100;
                        $(document).trigger('initUploadProgressFromOutside', file);
                        $('#datatableImportModal').modal('hide');
                        //console.log("Reloading datatable");
                        TABLE.ajax.reload();
                        $('#submit-import i').attr("class", "ti ti-check");
                    }
                    //console.log("Reseting countedData");
                    countedData = new Array();
                }
                index++;
            }
        }

        //const newData = $('input[id=dt-settings-new-data]:checked').is(":checked");
        //const deleteAndNew = $('input[id=dt-settings-delete-and-new]:checked').is(":checked");
        const importMode = $("#datatableImportModal input[name=dt-settings-import]:checked").val();
        const updateByColumn = $('#dt-settings-update-by-column').val();
        const skipWrongData = $('#skip-wrong-data').is(":checked");

        const formData = {};
        let countedData = new Array();
        let counter = 0;
        let finishCounter = 0;
        let dzchunkindex = 0;
        let action = 'edit';
        let url = WJ.urlAddPath(TABLE.getAjaxUrl(), "/editor");
        const chunks = window.chunksQuantity || 25;

        //console.log("mainData=", mainData, "importMode=", importMode, "updateByColumn=", updateByColumn);

        if (!mainData) {
            WJ.notifyError(WJ.translate('datatables.upload.file.error.js'));
            return;
        }

        //FIX - in onlyNew we can't nullify ID (because we use it for pairing by ID)
        if(importMode === "append") {
            action = 'create';
            //zresetuj ID hodnotu v datach
            mainData.forEach(data => data[DATA.columns[0].data] = 0);
        } else if(importMode === "update") {
            action = 'edit';
        } else if(importMode === "onlyNew") {
            action = 'create';
        }

        let fileName = $("#insert-file").val();
        let separator = fileName.lastIndexOf("/");
        if (separator === -1) separator = fileName.lastIndexOf("\\");
        if (separator > 0) fileName = fileName.substring(separator + 1);
        const file = new File([formData], fileName, {type: 'object'});
        file.upload = {progress: 0};
        let dztotalchunkcount = formData['dztotalchunkcount'];
        if (typeof dztotalchunkcount === "undefined") dztotalchunkcount = 1;
        const sizeOfChunk = 100 / dztotalchunkcount;
        //console.log("formData['dztotalchunkcount']", formData['dztotalchunkcount'], " dztotalchunkcount=", dztotalchunkcount, "sizeOfChunk=", sizeOfChunk);
        $(document).trigger('initAddedFileFromImageOutside', file);

        formData['name'] = fileName;
        formData['dztotalchunkcount'] = Math.ceil(mainData.length / chunks);
        formData['dztotalfilesize'] = Buffer.byteLength(JSON.stringify(mainData));

        //console.log("fileName=", fileName);

        await importData();
    });

    let xlsx;
    let excelData;
    let mainData;
    let importedColumns = [];

    let importModalListenersBinded = false;
    $('#datatableImportModal').on('show.bs.modal', function () {
        if (importModalListenersBinded == false) {
            //append custom import options
            var customOptions = $("#datatableImportModalCustomOptions");
            if (customOptions.length > 0) {
                $("#datatableImportModalOptions").prepend(customOptions);
                customOptions.show();
            } else {
                $("#datatableImportModal div.export-options").hide();
            }
        }
        $("#datatableImportModalOptions").find("input").first().trigger("click");
    });

    $('#datatableImportModal').on('shown.bs.modal', function () {

        $('#insert-file-label').text(WJ.translate('datatables.upload.file.prompt.js'));
        $('#insert-file-name').val('');
        $('#insert-file').val('');
        $("#import-status").html("");
        $('#submit-import i').attr("class", "ti ti-check");

        if (importModalListenersBinded == false) {
            $('#dt-settings-new-data').on('change', () => {
                $('#update-by-column').show();
                $('.modal-body').css('padding-bottom', '30px');
                $('#dt-settings-delete-and-new').prop('checked', false);
            });

            $('#dt-settings-delete-and-new').on('change', () => {
                $('#update-by-column').hide();
                $('.modal-body').css('padding-bottom', '15px');
                $('#dt-settings-new-data').prop('checked', false);
            });

            XLSX().then(module => {
                //console.log("xlsx module=", module);
                xlsx = module;
            });


            document
            .getElementById('insert-file')
            .addEventListener('change', e => {
                //console.log("insert-file-change, e=", e);
                $('#insert-file-label').text(e.target.files[0].name);
                const selectedFile = e.target.files[0];
                const fileReader = new FileReader();
                mainData = null;
                importedColumns = [];
                fileReader.onload = function(e) {
                    const data = e.target.result;
                    //console.log("xlsx=", xlsx);
                    const excelFile = xlsx.read(data, { type: 'binary', cellDates: true });
                    // forEach tam je len pre istotu keby sa tam vyskytlo viac excel suborov v SheetNames (nemalo by sa vyskytnut ale)
                    excelFile.SheetNames.forEach(sheet => {
                        let rowObject = xlsx.utils.sheet_to_row_object_array(excelFile.Sheets[sheet]);
                        //let jsonObject = JSON.stringify(rowObject);
                        //console.log("excel rowObject=", rowObject);
                        excelData = rowObject;
                    });
                    $( document ).trigger('file-reader-done');
                };
                fileReader.readAsBinaryString(selectedFile);
                $('#submit-import').prop('disabled', false);
                $('#submit-import i').attr("class", "ti ti-check");
            });

            $( document ).on('file-reader-done', () => {
                let importTable = window.datatableImportModal.TABLE;
                //console.log("excelData=", excelData, "importTable=", importTable);
                //hashtabulka ciselnikov na prevod nazvo hodnoty na ID
                let optionsTable = dtWJ.getOptionsTableImport(importTable.DATA);
                mainData = excelData.map(d => {
                    let row = {};
                    row.__rowNum__ = d.__rowNum__;
                    for (let index in importTable.DATA.fields) {
                        let col = importTable.DATA.fields[index];
                        let value = d[col.label+"|"+col.data];
                        //console.log("index: ", index, " col.data=", col.data, "col=", col, " value=", value, "d=", d);
                        //if (col.data.indexOf(".")!=-1) console.log("col=", col);

                        //skus cele lower case
                        if (typeof value == "undefined" && col.label!=null && col.data!=null) {
                            value = d[col.label.toLowerCase()+"|"+col.data.toLowerCase()];

                            if (typeof value == "undefined") {
                                //check value by iterating over all data in d
                                let name;
                                for (let key in d) {
                                    //key is in format label|name, check only name, label is probably changed
                                    let i = key.indexOf("|");
                                    if (i>0) name = key.substring(i+1);
                                    else name = key; //label is missing, check whole key as name

                                    if (col.name === name) {
                                        value = d[key];
                                    }
                                }
                            }
                        }

                        //console.log("value=", value, "label=", col.label, "data=", col.data, "d=", d);

                        //nemame hodnotu v exceli, preskocime
                        if (typeof value == "undefined") continue;

                        //sprav TRIM hodnoty
                        try {
                            if (value != null && typeof value == "string") {
                                //console.log("value=", value, "typeof=", typeof value);
                                value = value.trim();
                            }
                        } catch (e) {}

                        //teoreticky je mozne hodnotam nastavit NULL a tym padom aktualizovat podla stlpca len tie, ktore zadam
                        if ("NULL"===value) value = null;

                        if (1==row.__rowNum__) {
                            importedColumns.push(col.data);
                        }

                        if (typeof col.renderFormat != "undefined" && col.renderFormat != null) {
                            //uprav hodnoty
                            if (col.renderFormat.indexOf("-date-time")!=-1) {
                                //console.log("PARSING DATETIME, col=", col, " value=", value);
                                value = parseInt(moment(value, "L HH:mm:ss").valueOf());
                                //console.log("parsed datetime=", value, " moment.format=", moment(value).format("L HH:mm:ss"));
                            }
                            else if (col.renderFormat.indexOf("-date")!=-1) {
                                //console.log("PARSING DATE, col=", col, " value=", value);
                                value = parseInt(moment(value, "L").valueOf());
                                //console.log("parsed date=", value, " moment.format=", moment(value).format("L HH:mm:ss"));
                            }
                        }

                        //preved options hodnotu nazad na IDecko
                        let hashtableValue = optionsTable[col.data+"-"+value];
                        if (typeof hashtableValue != "undefined") {
                            value = hashtableValue;
                            //console.log("hashtableValue=", hashtableValue, "col.data-value=", col.data+"-"+value, "optionsTable=", optionsTable);
                        }

                        //ak className je array, je to pole JSON objektov
                        if (col.hasOwnProperty("className") && col.className != null && col.className.indexOf("array")!=-1 && value=="") value = null;
                        //ak je to array musis to poslat ako arrat
                        //console.log("col.array=", col.array);
                        if (col.hasOwnProperty("array") && col.array == true) {
                            //console.log("------------------");
                            if (value == null || value == "" || value == "[null]" || value == "null") value = [];
                            else {
                                let valueArray = [];
                                //console.log("TYPEOF VALUE: ", typeof value);
                                if (typeof value === "string") {
                                    let ids = value.split(",");

                                    ids.forEach((v,i) => {
                                        //console.log(v, i);

                                        //skus skonvertovat z ciselnika
                                        hashtableValue = optionsTable[col.data+"-"+v];
                                        if (typeof hashtableValue != "undefined") v = hashtableValue;

                                        if (isNaN(v)==false) {
                                            v = Number.parseFloat(v);
                                        }

                                        valueArray.push(v);
                                    });
                                } else {
                                    valueArray.push(value);
                                }

                                value = valueArray;
                            }
                        }

                        //json field
                        if (col.hasOwnProperty("type")) {
                            if ("json" === col.type) {
                                try {
                                    value = JSON.parse(value);
                                } catch (e) {
                                    console.log("ERROR PARSING JSON: ", value);
                                    WJ.notifyError(WJ.translate('datatables.import.json.error.js', (row.__rowNum__ + 1)), value);
                                }
                            }
                        }

                        if (value == WJ.translate('datatables.export.empty.js')) value = null;

                        //console.log("Setting property, col.data=", col.data, "value=", value);
                        WJ.setJsonProperty(row, col.data, value);
                    }
                    //console.log("row=", row);
                    return row;
                });
                //console.log("mainData=", mainData);
            });

            $("#datatableImportModal input[name=dt-settings-import]").off("click");
            $("#datatableImportModal input[name=dt-settings-import]").on("click", function (e) {
                $("#dt-import-update-by-column").toggle("update" === e.target.value||"onlyNew" === e.target.value);
            });
        }

        importModalListenersBinded = true;
    });

    $('#datatableImportModalOptions').off("click", "input");
    $('#datatableImportModalOptions').on("click", "input", function (event) {
        //console.log("click, event=", event, "this=", this);
        var $this = $(this);
        $("#datatableImportModal").find(".hidden").removeClass("hidden");
        $('#submit-import').prop('disabled', true);

        var hideElements = $this.attr("data-hide");
        if (typeof hideElements !== "undefined" && hideElements !== false) {
            var hideElementsArr = hideElements.split(",");
            hideElementsArr.forEach(function (e) {
                if ("#datatableImportModal .file-name"==e) {
                    $('#submit-import').prop('disabled', false);
                }
                $(e).addClass("hidden");
            });
        }
    });

}