//Use to identified in URL what type of data we want
export const ChartType = {
    Not_Chart: "notChart",
    Line: "line",
    Pie: "pie",
    Bar: "bar"
}

//! string must stay as they are, because they are used as day format type
export const DateType = {
    Seconds: "second",
    Minutes: "minute",
    Hours: "hour",
    Days: "day",
    Weeks: "week",
    Months: "month",
    Day_1: "day_1",
    Auto: "auto"
}

/**
 * Object (chart form) representing LINE type chart
 */
export class LineChartForm {
    constructor(yAxeNames, xAxeName, chartTitle,
        chartDivId, chartData, dateType) {
        this.yAxeNames = yAxeNames;
        this.xAxeName = xAxeName;
        this.chartTitle = chartTitle;
        this.chartDivId = chartDivId;
        this.chartData = chartData;
        this.dateType = dateType;
        this.chart = undefined;
    }
}

/**
 * Object (chart form) representing BAR type chart
 */
export class BarChartForm {
    constructor(yAxeName, xAxeName, chartTitle,
        chartDivId, chartData) {
        this.yAxeName = yAxeName;
        this.xAxeName = xAxeName;
        this.chartTitle = chartTitle;
        this.chartDivId = chartDivId;
        this.chartData = chartData;
        this.chart = undefined;
    }
}

/**
 * Object (chart form) representing PIE type chart
 */
export class PieChartForm {
    constructor(yAxeName, xAxeName, chartTitle,
        chartDivId, chartData) {
        this.yAxeName = yAxeName;
        this.xAxeName = xAxeName;
        this.chartTitle = chartTitle;
        this.chartDivId = chartDivId;
        this.chartData = chartData;
        this.chart = undefined;
        this.chartLegend = undefined;
    }
}

/**
 * Object to store chart date axe configuration values 'timeUnit' & 'count'
 */
class DateAxisInterval {
    constructor(timeUnit, count) {
        this.timeUnit = timeUnit;
        this.count = count;
    }
}

//Set component visibility based on selected option
export function setVisibility(selectedOption) {
    if(selectedOption == "graph") {
        $("#tableDiv").toggle(false);
        $("#tableDiv_2").toggle(false);
        $("#graphsDiv").toggle(true);
        $("#graphsDiv_2").toggle(true);
    } else if(selectedOption == "table") {
        $("#tableDiv").toggle(true);
        $("#tableDiv_2").toggle(true);
        $("#graphsDiv").toggle(false);
        $("#graphsDiv_2").toggle(false);
        $("#loader").toggle(false);
    } else if(selectedOption == "tableGraph") {
        $("#tableDiv").toggle(true);
        $("#tableDiv_2").toggle(true);
        $("#graphsDiv").toggle(true);
        $("#graphsDiv_2").toggle(true);
    }
}

/**
 * Return daterange:timestamp-timestamp from top search bar
 */
export function getDateRange(defaultRangeDaysValue) {
    var daterange = "";

    var from = $("div.md-breadcrumb .dt-filter-from-dayDate").val();
    if (typeof from == "undefined") from = '';
    var to = $("div.md-breadcrumb .dt-filter-to-dayDate").val();
    if (typeof to == "undefined") to = '';

    if(from == '' && to == '') {
        //If set default date range is invalid, set ra nge to 30 days
        if(defaultRangeDaysValue == undefined || defaultRangeDaysValue == null || isNaN(defaultRangeDaysValue) || defaultRangeDaysValue < 1) defaultRangeDaysValue = 30;

        //Use default values
        let defaultEndDate = new Date().getTime();
        let defaultStartDate = defaultEndDate - (60*60*24*defaultRangeDaysValue*1000); //-30 dni
        from = WJ.formatDate(defaultStartDate);
        to = WJ.formatDate(defaultEndDate);
    }
    daterange = "daterange:";
    if (from != '') daterange += moment(from, "L HH:mm:ss").valueOf();

    if (to != '') {
        daterange += "-"
        daterange += moment(to, "L HH:mm:ss").valueOf();
    }

    return daterange;
}

/**
 * Save last search criteria to session storage, so all stats page will have same criteria when loaded
 * @param {*} DATA
 */
export function saveSearchCriteria(DATA) {
    var inputs = [".dt-filter-from-dayDate", ".dt-filter-to-dayDate", "#rootDir", "#botFilterOut", "#searchUrl", ".dt-filter-lastLogon", "#searchEngineSelect", "#webPageSelect"];
    var defaultSearch = {};
    var isWebPageValueValid = true;
    var isSearchEngineValueValid = true;

    //!! it's impotatnt step, without merge we will lose set params from another page
    let oldDefaultSearch = JSON.parse( window.sessionStorage.getItem("webjet.apps.stat.filter") );
    if(oldDefaultSearch == null) oldDefaultSearch = {};

    for (const name of inputs) {
        var $input = $("#"+DATA.id+"_extfilter "+name);
        var value = $input.val();
        //console.log("name=", name, "value=", value, "input=", $input);
        if ("true"===value) {
            //it's checkbox
            value = $("#"+DATA.id+"_extfilter "+name).is(":checked");
        }
        if ("#rootDir"===name) {
            if (value == null) {
                value = "-1";
                $input.val(value);
            }
            //console.log("text=", $input.parent().find(".dt-tree-container .input-group input").val());
            defaultSearch[name+"-text"] = $input.attr("data-text");
            defaultSearch[name+"-domain"] = $("#header\\.currentDomain").val();
        }

        if("#webPageSelect" === name) {
            var $input = $(name);
            if($input.length>0) {
                let val = $input.find(":selected").val();
                let text = $input.find(":selected").text();
                //Both value and key must be valid
                if(isStringParamValid(text) && isNumberParamValid(val, false)) {
                    //Sect value if valid
                    defaultSearch[name + "-text"] = text;
                }
                //Else, selected value is NOT valid
                else isWebPageValueValid = false;
            } //Else, this page does NOT contain webPageSelect
        }

        if("#searchEngineSelect" === name) {
            var $input = $(name);
            if($input.length>0) {
                let val = $input.find(":selected").val();
                let text = $input.find(":selected").text();
                //Both value and key must be valid
                if(isStringParamValid(text) && isStringParamValid(val)) {
                    //Sect value if valid
                    defaultSearch[name + "-text"] = text;
                }
                //Else, selected value is NOT valid
                else isSearchEngineValueValid = false;
            } //Else, this page does NOT contain webPageSelect
        }

        //console.log("saveSearchForm, name=", name, "value=", value);
        if (value != null && value != "" && value != "-1" && value != "false") {
            defaultSearch[name] = value;
        }
    }

    //Inicialize
    if(oldDefaultSearch == undefined || oldDefaultSearch == null) oldDefaultSearch = {};
    //Merge old object with new
    let mergeDefaultSearch = Object.assign(oldDefaultSearch, defaultSearch);

    //We need manualy remove property (because merge will leave it there)
    if(!defaultSearch.hasOwnProperty("#rootDir") && mergeDefaultSearch.hasOwnProperty("#rootDir"))
        delete mergeDefaultSearch["#rootDir"];

    //
    if(!isWebPageValueValid) {
        if(mergeDefaultSearch.hasOwnProperty("#webPageSelect"))
            delete mergeDefaultSearch["#webPageSelect"];
        if(mergeDefaultSearch.hasOwnProperty("#webPageSelect-text"))
            delete mergeDefaultSearch["#webPageSelect-text"];
    }

    //
    if(!isSearchEngineValueValid) {
        if(mergeDefaultSearch.hasOwnProperty("#searchEngineSelect"))
            delete mergeDefaultSearch["#searchEngineSelect"];
        if(mergeDefaultSearch.hasOwnProperty("#searchEngineSelect-text"))
            delete mergeDefaultSearch["#searchEngineSelect-text"];
    }

    //Set new object
    var json = JSON.stringify(mergeDefaultSearch);
    if (json != "{}") window.sessionStorage.setItem("webjet.apps.stat.filter", json);
    else window.sessionStorage.removeItem("webjet.apps.stat.filter");
}

/**
 * Gets saved search criteria from session storage
 * @returns
 */
export function getSearchCriteria() {
    var defaultSearch = window.sessionStorage.getItem("webjet.apps.stat.filter");

    let webPageId = null;
    let webPageText = null;

    let searchEngineId = null;
    let searchEngineText = null;

    if ("{}"==defaultSearch) defaultSearch = null;
    if (defaultSearch != null) {
        defaultSearch = JSON.parse(defaultSearch);
        for (const property in defaultSearch) {
            var value = defaultSearch[property];
            if (property == "#rootDir") {
                //console.log("Setting rootDir, value=", value);
                var savedDomain = defaultSearch[property+"-domain"];
                var domain = $("#header\\.currentDomain").val();
                if (typeof savedDomain != "undefined" && savedDomain != domain) value = "-1";

                var $property = $(property);
                $property.val(value);

                var text = defaultSearch[property+"-text"];
                $property.attr("data-text", text);

                //console.log("get rootDir=", $property[0].outerHTML, "prop=", $property, "text=", text);
            }
            if (property == "#botFilterOut") {
                $("#botFilterOut").prop("checked", value);
            }

            if (property == "#searchUrl") {
                $("#searchUrl").val(value);
            }

            if(property == "#webPageSelect") {
                webPageId = value;
                webPageText = defaultSearch[property + "-text"];
            }

            if(property == "#searchEngineSelect") {
                searchEngineId = value;
                searchEngineText = defaultSearch[property + "-text"];
            }
        }

        if($("#webPageSelect").length) {
            if(webPageId != null && webPageText != null) {
                //Get object, select
                let webPageSelect = document.getElementById('webPageSelect');
                //Add new options
                webPageSelect.add(new Option(webPageText, webPageId));
                //Select value
                $("#webPageSelect").val(webPageId);
                //Refresh object
                $("#webPageSelect").selectpicker('refresh');
            }
        }

        if($("#searchEngineSelect").length) {
            if(searchEngineId != null && searchEngineText != null) {
                //Get object, select
                let searchEngineSelect = document.getElementById('searchEngineSelect');
                //Add new options
                searchEngineSelect.add(new Option(searchEngineText, searchEngineId));
                //Select value
                $("#searchEngineSelect").val(searchEngineId);
                //Refresh object
                $("#searchEngineSelect").selectpicker('refresh');
            }
        }

    }

    return defaultSearch;
}

/**
 * Prepare array of object used as input for line charts. Every  created object in array id combination of value we want show (from data)
 * and text (translated key if valid). Text used for naming series can be undefined (if input translate key was "" or undefined).
 * !! Length of both array must be same 1:1, or undefined will be returned.
 * @param {string} yAxeNames array of values from data object we want show
 * @param {string} nameToShowKey array of translate texts, used to name series (can be undefined)
 * @returns Array of objects
 */
export function getLineChartYAxeNameObjs(yAxeNames, nameToShowKey) {
    if(yAxeNames.length != nameToShowKey.length) {
        console.log("Number of input yAxeNames and nameToShows is not same !");
        return undefined;
    }

    var array = [];
    for(let i = 0; i < yAxeNames.length; i++)
        array[array.length] = getLineChartYAxeNameObj(yAxeNames[i], nameToShowKey[i]);

    return array;
}

function getLineChartYAxeNameObj(yAxeName, nameToShowKey) {
    let nameToShow = undefined;
    if(nameToShowKey !== undefined && nameToShowKey.length > 0) nameToShow = WJ.translate(nameToShowKey);
    return {
        yAxeName: yAxeName,
        nameToShow: nameToShow
    }
}

/**
 * Convert set data into LineChart format data.
 * !! Provided data (dataToConvert) must have format "Map<String, List<T>>" and T class must contain date field "dayDate", used for data SORT.
 * Output is Map of converted data where key's are same (as in set Map dataToConvert) and value is array of objects.
 * @param {Map<String, List<T>>} dataToConvert data we want to cnovert into LineChart data
 */
export function convertDataForLineChart(dataToConvert) {
    let convertedDataMap = new Map();
    let keys = Object.keys(dataToConvert);
    for(var key of keys) {
        convertedDataMap.set(key, dataToConvert[key]);
    }
    sortData(convertedDataMap);
    return convertedDataMap;
}

//Sort converted data using compare fn
export function sortData(convertedDataMap) {
    for (const [key, value] of convertedDataMap.entries())
        value.sort( compare );
    return 0;
}

//Sort by dayDate (date value (field) in object)
function compare(a, b) {
    if (a.dayDate < b.dayDate) return -1;
    if (a.dayDate > b.dayDate) return 1;
    return 0;
}

/**
 * From chart data compute step between values. Bz that step return DateAxisInterval with configuration params for date axe.
 *
 * @param {Map<String, List<T>>} chartData, basic chart data from chart form
 * @param {String} dateValueName, name of param that represent date value in object (mostly 'dayDate')
 * @returns Object DateAxisInterval with date axe config params 'timeUnit' & 'count'
 */
async function computeAxeInterval(chartData, dateValueName) {
    var second = 1000;
    var minute = 60 * second;
    var hour = 60 * minute;
    var day = 24 * hour;
    var week = 7 * day;
    var month = 4* week;

    for (const [dataSetName, dataSetData] of chartData.entries()) {
        if(dataSetData.length >= 2) {
            var stepA = dataSetData[0][dateValueName];
            var stepB = dataSetData[1][dateValueName];
            var diff = stepB - stepA;

            if(diff < minute) return new DateAxisInterval(DateType.Seconds, Math.round(diff / second));

            if(diff < hour) return new DateAxisInterval(DateType.Minutes, Math.round(diff / minute));

            if(diff < day) return new DateAxisInterval(DateType.Hours, Math.round(diff / hour));

            if(diff < week) return new DateAxisInterval(DateType.Days, Math.round(diff / day));

            if(diff < month) return new DateAxisInterval(DateType.Weeks, Math.round(diff / week));

            return new DateAxisInterval(DateType.Months, Math.round(diff / month));
        }
    }
    //Default value
    return new DateAxisInterval(DateType.Days, 1);
}

/**
 * By the type of input chartForm create new chart of that type.
 *
 * In this fn create root, set themes and call the right fn to create specific type of chart.
 *
 * @param {*} chartForm one of the ChartForm objects, LineChartForm / PieChartForm / BarChartForm
 * @param {Bolean} update only in the case of True - the header wont be added to chart div
 */
export async function createAmchart(chartForm, update) {
    //Create chart root
    var root = am5.Root.new(chartForm.chartDivId);
    //Hide amcharts logo
    root._logo.dispose();
    //Set themes
    root.setThemes([
        am5themes_Animated.new(root),
        am5_dark.new(root),
        WebjetTheme.new(root)
    ]);
    //Add title to chart div
    if(update !== true) {
        var htmlCode = '<h6 class="amchart-header">' + chartForm.chartTitle;
        $('#' + chartForm.chartDivId).before(htmlCode);
    }

    //By the type of input ChartForm create chart of that type
    if(chartForm instanceof BarChartForm) {
        //BAR type chart
        createBarChart(root, chartForm);
    } else if(chartForm instanceof PieChartForm) {
        //PIE type chart
        createPieChart(root, chartForm);
    } else if(chartForm instanceof LineChartForm) {
        //LINE type chart
        createLineChart(root, chartForm);
    }
}

/**
 * Create LINE type chart and set all setting around chart. The created chart is set in LineChartForm.chart param.
 *
 * @param {am5.Root} root
 * @param {LineChartForm} chartForm
 */
async function createLineChart(root, chartForm) {
    //Create chart instance
    var chart = root.container.children.push(
        am5xy.XYChart.new(root, {
            panX: false,
            panY: false,
            wheelX: "none",
            wheelY: "none",
            height: am5.percent(95)
        })
    );

    //!! set created chart into LineChartForm.chart
    chartForm.chart = chart;

    // Create X axes depending on type of input
    var xAxis;
    if(chartForm.dateType === DateType.Day_1) {
        //Day_1 is special type where xAxe isnt date type axe but value type axe in range from 0-23 (like hours in one day)
        xAxis = chart.xAxes.push(
            am5xy.ValueAxis.new(root, {
                min: 0,
                max: 23,
                strictMinMax: true,
                renderer: am5xy.AxisRendererX.new(root, {})
            })
        );
    } else {
        //All other DateType's are represented as date type axe
        var chartTimeUnit;
        var chartCount;
        if(chartForm.dateType === DateType.Auto) {
            //Auto type means we need compute timeUnit and count (granuality) for this chart
            var axeInterval = await computeAxeInterval(chartForm.chartData, chartForm.xAxeName);
            chartTimeUnit = axeInterval.timeUnit;
            chartCount = axeInterval.count;
        } else {
            //The chart timeUnit is set manualy by dateType value, count is default set as 1
            chartTimeUnit = chartForm.dateType;
            chartCount = 1;
        }

        //Create xAxe
        xAxis = chart.xAxes.push(am5xy.DateAxis.new(root, {
            maxDeviation: 0,
            baseInterval: {
                timeUnit: chartTimeUnit,
                count: chartCount
            },
            renderer: am5xy.AxisRendererX.new(root, {}),
            tooltip: am5.Tooltip.new(root, {})
        }));
    }

    //Create yAxe representing values in time
    var yAxis = chart.yAxes.push(
        am5xy.ValueAxis.new(root, {
            renderer: am5xy.AxisRendererY.new(root, {})
        })
    );

    //Loop through map of data. Every item in map is representing a different dataset
    //Maybe want show visits stat's on different pages, key param visit is same, but every page stat's are represented by different dataset
    for (const [dataSetName, dataSetData] of chartForm.chartData.entries()) {

        //Maybe we want show more than just one param, for example for one dataset of data we want to show params 'visit' / 'view' / 'count' at same time
        chartForm.yAxeNames.forEach(async yAxeNameObj => {

            //Combine dataSet name and series name (aka what param are we showing in chart) to create final series name
            //Mostly inportant in legend, so we can tell difference between series
            let seriesName = "";
            if(dataSetName === undefined || dataSetName.length < 1) {
                if(yAxeNameObj['nameToShow'] !== undefined && yAxeNameObj['nameToShow'].length > 0)
                    seriesName = yAxeNameObj['nameToShow'];
            } else {
                seriesName = dataSetName;
                if(yAxeNameObj['nameToShow'] !== undefined && yAxeNameObj['nameToShow'].length > 0)
                    seriesName += " - " + yAxeNameObj['nameToShow'];
            }

            // x axe name (param name that is representing this axe in object) -  for DateType Day_1 it's default "hour"
            var xAxeValue = chartForm.dateType === DateType.Day_1 ? "hour" : chartForm.xAxeName;

            //Create series
            var series = chart.series.push(am5xy.LineSeries.new(root, {
                name: seriesName,
                xAxis: xAxis,
                yAxis: yAxis,
                valueYField: yAxeNameObj['yAxeName'],
                valueXField: xAxeValue,
                legendValueText: "{valueY}"
            }));

            //Make line wider
            series.strokes.template.setAll({
                strokeWidth: 2
            });

            //Add tooltip
            var tooltip = am5.Tooltip.new(root, {
                pointerOrientation: "horizontal",
                labelText: "{name}: [bold]{valueY}[/]",
                autoTextColor: false,
                tooltipIntervalOffset: 0
            });
            tooltip.label.setAll({
                fill: am5.color("#FFFFFF"),
                textAlign: "middle",
                textValign: "middle"
            });
            series.set("tooltip", tooltip);

            //Add data to series
            series.data.setAll(dataSetData);

            //Make stuff animate on load
            series.appear();
        });
    }

    // Add cursor - with both X/Y lines visible
    var cursor = chart.set("cursor",
        am5xy.XYCursor.new(root, {
            behavior: "zoomX"
        })
    );
    cursor.lineY.set("visible", true);
    cursor.lineX.set("visible", true);

    // Add X scrollbar
    let scrollbarX = am5.Scrollbar.new(root, {
        orientation: "horizontal",
    });
    chart.set("scrollbarX", scrollbarX);

    // Add Y scrollbar
    let scrollbarY = am5.Scrollbar.new(root, {
        orientation: "vertical"
    });
    chart.set("scrollbarY", scrollbarY);

    // Add legend
    var legend = chart.children.push(
        am5.Legend.new(root, {
            centerX: am5.percent(50),
            y: am5.percent(103),
            x: am5.percent(50),
            layout: root.horizontalLayout,
            useDefaultMarker: true
        })
    );

    //So legend wont show actual value when we point on graph (for this purpose we have tooltip)
    legend.valueLabels.template.set("forceHidden", true);

    // When legend item container is hovered, dim all the series except the hovered one
    legend.itemContainers.template.events.on("pointerover", function(e) {
        var itemContainer = e.target;

        // As series list is data of a legend, dataContext is series
        var series = itemContainer.dataItem.dataContext;

        chart.series.each(function(chartSeries) {
            if (chartSeries != series) {
                chartSeries.strokes.template.setAll({
                    strokeOpacity: 0.25
                });
            } else {
                chartSeries.strokes.template.setAll({
                    strokeWidth: 3
                });
            }
        })
    })

    // When legend item container is unhovered, make all series as they are
    legend.itemContainers.template.events.on("pointerout", function(e) {
        var itemContainer = e.target;

        chart.series.each(function(chartSeries) {
          chartSeries.strokes.template.setAll({
            strokeOpacity: 1,
            strokeWidth: 2,
            stroke: chartSeries.get("fill")
          });
        });
    })

    // It's is important to set legend data after all the events are set on template, otherwise events won't be copied
    legend.data.setAll(chart.series.values);

    // Make stuff animate on load
    chart.appear(1000, 100);
}


/**
 * Create BAR type chart and set all setting around chart. The created chart is set in BarChartForm.chart param.
 *
 * @param {am5.Root} root
 * @param {BarChartForm} chartForm
 */
async function createBarChart(root, chartForm) {
    //Create chart instance
    var chart = root.container.children.push(
        am5xy.XYChart.new(root, {
            panX: false,
            panY: false,
            wheelX: "none",
            wheelY: "none"
        }
    ));

    //!! set created chart into BarChartForm.chart
    chartForm.chart = chart;

    //Define render specification (we need reverse render type)
    var yRenderer = am5xy.AxisRendererY.new(root, {
        minGridDistance: 30,
        inversed: true
    });
    yRenderer.grid.template.set("location", 1);

    //Create Y axe
    var yAxis = chart.yAxes.push(
        am5xy.CategoryAxis.new(root, {
            maxDeviation: 0,
            categoryField: chartForm.yAxeName,
            renderer: yRenderer
        })
    );

    //Create X axe
    var xAxis = chart.xAxes.push(
        am5xy.ValueAxis.new(root, {
            maxDeviation: 0,
            min: 0,
            extraMax: 0.1,
            renderer: am5xy.AxisRendererX.new(root, {
                strokeOpacity: 0.1
            })
        })
    );

    //Cretate series
    var series = chart.series.push(am5xy.ColumnSeries.new(root, {
        xAxis: xAxis,
        yAxis: yAxis,
        valueXField: chartForm.xAxeName,
        categoryYField: chartForm.yAxeName
    }));

    //Create tooltip
    var tooltip = am5.Tooltip.new(root, {
        labelText: "{" + chartForm.yAxeName + "}: {" + chartForm.xAxeName + "}",
        autoTextColor: false
    })
    tooltip.label.setAll({
        fill: am5.color("#FFFFFF"),
        textAlign: "middle",
        textValign: "middle"
    });
    series.set("tooltip", tooltip);

    // Rounded corners for columns
    series.columns.template.setAll({
        cornerRadiusTR: 5,
        cornerRadiusBR: 5,
        strokeOpacity: 0
    });

    // Make each column to be of a different color
    series.columns.template.adapters.add("fill", function(fill, target) {
        return chart.get("colors").getIndex(series.columns.indexOf(target));
    });
    series.columns.template.adapters.add("stroke", function(stroke, target) {
        return chart.get("colors").getIndex(series.columns.indexOf(target));
    });

    //Set data (for BAR is pecific to set data in series and in Y axe at same time)
    yAxis.data.setAll(chartForm.chartData);
    series.data.setAll(chartForm.chartData);

    //Add cursor
    var cursor = chart.set("cursor", am5xy.XYCursor.new(root, {}));
    cursor.lineY.set("visible", false);
    cursor.lineX.set("visible", false);
    chart.set("cursor", cursor);

    // Make stuff animate on load
    series.appear(1000);
    chart.appear(1000, 100)
}

/**
 * Create PIE type chart and set all setting around chart. The created chart is set in PieChartForm.chart param.
 *
 * @param {am5.Root} root
 * @param {PieChartForm} chartForm
 */
async function createPieChart(root, chartForm) {
    //Create chart instance
    var chart = root.container.children.push(
        am5percent.PieChart.new(root, {
            innerRadius: am5.percent(50),
            layout: root.verticalLayout
        })
    );

    //!! set created chart into PieChartForm.chart
    chartForm.chart = chart;

    //Create series
    var series = chart.series.push(
        am5percent.PieSeries.new(root, {
            valueField: chartForm.yAxeName,
            categoryField: chartForm.xAxeName,
            legendLabelText: "{category}",
            legendValueText: "[bold]{valuePercentTotal.formatNumber('0.0')}%[/]" //Format legend
        })
    );

    //Format labels
    series.labels.template.set("text", "{category}: [bold]{valuePercentTotal.formatNumber('0.0')}%[/]");

    //We are setting data in series only if data length is more than 0, or error occur
    if(chartForm.chartData != undefined && chartForm.chartData.length > 0)
        series.data.setAll(chartForm.chartData);

    //Create legend
    var legend = chart.children.push(am5.Legend.new(root, {
        centerX: am5.percent(50),
        x: am5.percent(50),
        layout: root.gridLayout
    }));
    legend.data.setAll(series.dataItems);

    //!! Set ticks color
    //If zou dont know (like me), TICKS are that stupid lines that connect pie slice with label
    series.ticks.template.setAll({
        fill: am5.color("#FFFFFF"),
        fillOpacity: 1,
        opacity: 1,
        stroke: am5.color("#FFFFFF"),
        strokeOpacity: 1,
        //strokeWidth: 1.5
    })

    //Create and format tooltip
    var tooltip = am5.Tooltip.new(root, {
        labelText: "{" + chartForm.xAxeName + "}: {valuePercentTotal.formatNumber('#.#')}% ({" + chartForm.yAxeName + "})",
        autoTextColor: false
    })
    tooltip.label.setAll({
        fill: am5.color("#FFFFFF")
    });
    series.set("tooltip", tooltip);

    //!! PieChartFrom is specific in way, that store Legend instance
    //Its must because if we change PIE chart data, Legend will not update itself, we must do it manually, so we need Legend instance
    chartForm.chartLegend = legend;
}

/**
 * Update chart set in entered ChartForm. The chart data must be allready updated in this ChartForm object.
 *
 * @param {*} chartForm one of the ChartForm objects, LineChartForm / PieChartForm / BarChartForm
 */
export async function updateChart(chartForm) {
    if(chartForm instanceof BarChartForm) {
        //BAR chart dont need remove series, just set new data to series
        //our bar hart allways support only 1 series
        chartForm.chart.series.values[0].data.setAll(chartForm.chartData);

        //!! its also MUST to set new data into CategoryAxis
        chartForm.chart.yAxes.values[0].data.setAll(chartForm.chartData);
    } else if(chartForm instanceof PieChartForm) {
        //PIE chart dont need remove series, just set new data to series
        //this type of charts cant have more series
        chartForm.chart.series.values[0].data.setAll(chartForm.chartData);

        //!! its also MUST to set new data into Legend
        chartForm.chartLegend.data.setAll(chartForm.chart.series.values[0].dataItems);
    } else if(chartForm instanceof LineChartForm) {
        //Find our root element (in case of line chart, we need to dispose chart and generate new one)
        am5.array.each(am5.registry.rootElements, function(root) {
            if (root.dom.id == chartForm.chartDivId) {
                //remove created root element
                root.dispose();

                //Create new root with chart, update param in createAmchart is set as true, because chart div header is allready generated
                createAmchart(chartForm, true);
            }
        });
    }
}

/**
 * Support fn to generate data for actual data - monitoring server specific type chart.
 * From actual daterTime generate 100 empty values with grid every 5 seconds to past.
 *
 * @param {String} type of monitoring server chart. It can be 'memoryAmchart' or 'cpuAmchart'
 * @returns return set of generated data
 */
function generateChartData(type) {
    var chartData = [];
    var firstDate = new Date();

    //Generate 100 emptz values with 5 sec grid
    if(type == "memoryAmchart") {
        for (var i = 1; i < 100; i++) {
            var newDate = new Date(firstDate);
            newDate.setSeconds(newDate.getSeconds() - (5 * i));

            //memoryAmchart contain 3 different series
            chartData.push({
                serverActualTime: newDate.getTime(),
                memFree: 0,
                memUsed: 0,
                memTotal: 0
            });
        }
    } else if(type == "cpuAmchart") {
        for (var i = 1; i < 100; i++) {
            var newDate = new Date(firstDate);
            newDate.setSeconds(newDate.getSeconds() - (5 * i));

            //cpuAmchart chart contain only 1 series
            chartData.push({
                serverActualTime: newDate.getTime(),
                cpuUsage: 0
            });
        }
    }
    //data need to by reversed or chart will generate them in wrong way (from now to past)
    return chartData.reverse();
}

/**
 * Support fn to to add new live data in server-monitoring chart's + create animation to show
 * movemet from old value to new value in chart.
 *
 * @param {am5xy.LineSeries} allSeries all series of chart instance, to witch new data are inserted
 * @param {am5xy.DateAxis} xAxis x axis instance, to witch new date is inserted
 * @param {Object} data object with new live data
 * @param {String} type of monitoring server chart. It can be 'memoryAmchart' or 'cpuAmchart'
 */
export async function addData(allSeries, xAxis, data, type) {

    //Continue only if entered data object isnt undefined
    if(JSON.stringify(data) == undefined) {
        //console.log("Undefined data");
        return;
    }

    //Loop throu all series of chart
    allSeries.forEach(series => {
        //Get from series last item (soooo last set value and time when)
        var lastDataItem = series.dataItems[series.dataItems.length - 1];
        var lastValue = lastDataItem.get("valueY");
        var easing = am5.ease.linear;
        var newValue;
        var seriesName = series._settings.name;

        //Depending on chart type, set new object of values and new actual server time

        if(type == "memoryAmchart") {
            //Pick new value for series, depending on what series it is
            if(seriesName == WJ.translate("components.monitoring.used_memory.js"))
                newValue = Math.round(data.memFree / 1000000);
            else if(seriesName == WJ.translate("components.monitoring.free_memory.js"))
                newValue = Math.round(data.memUsed / 1000000);
            else if(seriesName == WJ.translate("components.monitoring.total_memory.js"))
                newValue = Math.round(data.memTotal / 1000000);

            //memoryAmchart chart has 3 param's = 3 series
            series.data.removeIndex(0);
            series.data.push({
                serverActualTime: data.serverActualTime,
                memFree: Math.round(data.memFree / 1000000),
                memUsed: Math.round(data.memUsed / 1000000),
                memTotal: Math.round(data.memTotal / 1000000)
            })
        } else if(type == "cpuAmchart") {
            newValue = data.cpuUsage;

            //cpuAmchart chart got only 1 param = 1 series
            series.data.removeIndex(0);
            series.data.push({
                serverActualTime: data.serverActualTime,
                cpuUsage: newValue
            })
        }

        //New data item, from old value to new value
        var newDataItem = series.dataItems[series.dataItems.length - 1];
        newDataItem.animate({
            key: "valueYWorking",
            to: newValue,
            from: lastValue,
            duration: 600,
            easing: easing
        });

        //Create animation for this shit
        var animation = newDataItem.animate({
            key: "locationX",
            to: 0.5,
            from: -0.5,
            duration: 600
        });
        if (animation) { //Allso update tooltip
            var tooltip = xAxis.get("tooltip");
            if (tooltip && !tooltip.isHidden()) {
                animation.events.on("stopped", function () {
                    xAxis.updateTooltip();
                })
            }
        }
    });
}

/**
 * By given entered params generate into chart new series and set chart data into it.
 *
 * @param {String} seriesName
 * @param {am5xy.XYChart} chart
 * @param {am5.Root} root
 * @param {Object} data
 * @param {am5xy.DateAxis} xAxis
 * @param {am5xy.ValueAxis} yAxis
 * @param {String} yField
 * @param {String} type
 */
async function setSeries(seriesName, chart, root, data, xAxis, yAxis, yField, type) {
    //Create series
    var series = chart.series.push(
        am5xy.LineSeries.new(root, {
            name: type == "memoryAmchart" ? WJ.translate(seriesName) : "cpuSeries", //memoryAmchart series names need to bz translate from keys
            xAxis: xAxis,
            yAxis: yAxis,
            valueYField: yField,
            valueXField: "serverActualTime", //It is hard set
            tooltip: am5.Tooltip.new(root, {
                pointerOrientation: "horizontal",
                labelText: "{valueY} " + (type == "memoryAmchart" ? "MB" : "%") //Changed label postfix depending on chart type
            })
        })
    );
    //Make line wider
    series.strokes.template.setAll({
        strokeWidth: 2
    });
    series.data.setAll(data);
}

/**
 * Fn to create specific live charts for vue component server-monitoring.
 *
 * Support 2 tzpes of chart's.
 *
 * @param {*} rootName unique name for chart root element
 * @param {*} type of server monitoring chart. It can be 'memoryAmchart' or 'cpuAmchart'
 * @returns insance of created new chart
 */
export async function createServerMonitoringChart(rootName, type) {
    //Create root
    var root = am5.Root.new(rootName);
    //Hide amcharts5 logo
    root._logo.dispose();
    //Set themes
    root.setThemes([
        am5themes_Animated.new(root),
        am5_dark.new(root),
        WebjetTheme.new(root)
    ]);

    //Generate chart default data
    var data = generateChartData(type);

    // Create chart
    var chart = root.container.children.push(
        am5xy.XYChart.new(root, {
            panX: false,
            panY: false,
            wheelX: "none",
            wheelY: "none"
        }
    ));

    // Create X axe
    var xAxis = chart.xAxes.push(
        am5xy.DateAxis.new(root, {
            maxDeviation: 0.5,
            baseInterval: {
                timeUnit: "second",
                count: 5
            },
            renderer: am5xy.AxisRendererX.new(root, {
                minGridDistance: 50
            }),
            tooltip: am5.Tooltip.new(root, {})
        })
    );

    // Create Y axe
    var yAxis = chart.yAxes.push(
        am5xy.ValueAxis.new(root, {
            renderer: am5xy.AxisRendererY.new(root, {})
        })
    );

    if(type == "memoryAmchart") {
        // Add series
        setSeries("components.monitoring.used_memory.js", chart, root, data, xAxis, yAxis, "memFree", type);
        setSeries("components.monitoring.free_memory.js", chart, root, data, xAxis, yAxis, "memUsed", type);
        setSeries("components.monitoring.total_memory.js", chart, root, data, xAxis, yAxis, "memTotal", type);

        // Add legend
        var legend = chart.children.push(
            am5.Legend.new(root, {
                centerX: am5.percent(50),
                y: am5.percent(100),
                x: am5.percent(50),
                layout: root.horizontalLayout,
                useDefaultMarker: true
            })
        );
        // It's is important to set legend data after all the events are set on template, otherwise events won't be copied
        legend.data.setAll(chart.series.values);

    } else if(type == "cpuAmchart") {
        // Add series
        setSeries("Series_1", chart, root, data, xAxis, yAxis, "cpuUsage", type);
    }

    //Create cursor
    var cursor = chart.set("cursor",
        am5xy.XYCursor.new(root, {
            xAxis: xAxis,
            behavior: "zoomXY"
        }
    ));
    cursor.lineX.set("visible", true);
    cursor.lineY.set("visible", true);

    // Make stuff animate on load
    chart.appear(1000, 100);

    //Return created chart instance
    return chart;
}


/* SUPPORT LOGIC, like methods that work with ext filters etc. */

export async function initGroupIdSelect() {
    $("input.webjet-dte-jstree").each(function(index) {
        var $element = $(this);
        //console.log("html=", $element[0].outerHTML, "val=", $element.val(), "text=", $element.data("text"));
        var id = $element.attr("id");
        var htmlCode = $('<div class="vueComponent" id="editorApp'+id+'"><webjet-dte-jstree :data-table-name="dataTableName" :data-table="dataTable" :click="click" :id-key="idKey" :data="data" :attr="attr" @remove-item="onRemoveItem"></webjet-dte-jstree></div>');
        htmlCode.insertAfter($element);

        const conf = {
            jsonData: [{
                "groupId": $element.val(),
                "fullPath": $element.data("text")
            }],
            className: "dt-tree-groupid-root",
            _id: id
        };
        //console.log("conf=", conf);
        const app = window.VueTools.createApp({
            components: {},
            data() {
                return {
                    data: null,
                    idKey: null,
                    dataTable: null,
                    dataTableName: null,
                    click: null,
                    attr: null
                }
            },
            created() {
                this.data = fixNullData(conf.jsonData, conf.className);
                //console.log("JS created, data=", this.data, " conf=", conf, " val=", conf._input.val());
                this.idKey = conf._id;
                //co sa ma stat po kliknuti prenasame z atributu className datatabulky (pre jednoduchost zapisu), je to hodnota obsahujuca dt-tree-
                //priklad: className: "dt-row-edit dt-style-json dt-tree-group", click=dt-tree-group
                const confClassNameArr = conf.className.split(" ");
                for (var i=0; i<confClassNameArr.length; i++) {
                    let className = confClassNameArr[i];
                    if (className.indexOf("dt-tree-")!=-1) this.click = className;
                }
                //console.log("click=", this.click);
                if (typeof(conf.attr)!="undefined") this.attr = conf.attr;

                this.dataTable = {
                    DATA: {

                    }
                }
            },
            methods: {
                onRemoveItem(id){
                }
            }
        });
        VueTools.setDefaultObjects(app);
        app.component('webjet-dte-jstree', window.VueTools.getComponent('webjet-dte-jstree'));
        const vm = app.mount($element.parent().find("div.vueComponent")[0]);
        //console.log("Setting vm, input=", element, "vm=", vm);
        $element.data("vm", vm);
        //console.log("set vm=", $element.data("vm"));
        $element.hide();
    });
}

function fixNullData(data, click) {
    //console.log("fixNullData, data=", data, "click=", click);
    //ak to je pole neriesime, ponechame bezo zmeny
    if (click.indexOf("-array")!=-1) return data;
    //ak to nie je pole, musime nafejkovat jeden objekt aby sa pole aspon zobrazilo (a dala sa zmenit hodnota)
    if (data.length==0) {
        let emptyItem = {
            fullPath: ""
        }
        if (click.indexOf("dt-tree-page")!=-1) emptyItem.docId = -1;
        else if (click.indexOf("dt-tree-group")!=-1) emptyItem.groupId = -1;
        else emptyItem.id = -1;

        return [emptyItem];
    }
    return data;
}

export function bindFilter(callback) {
    $("#rootDir").on("change", function() {
        let value = $("#rootDir").val();
        //console.log("value=", value);
        if (value == "")
        {
            window.lastSelect = $("#rootDir")[0];
            //WJ.openPopupDialog("/admin/grouptree.jsp", 300, 450);
            WJ.openIframeModal({
                url: "/admin/grouptree.jsp",
                width: 450,
                height: 350,
                closeButtonPosition: "close-button-over",
                buttonTitleKey: "button.cancel",
                okclick: function() {
                    //console.log("OK clicked");
                    WJ.closeIframeModal();
                }
            })
        } else {
            callback();
        }
    });

    $("#botFilterOut").on("change", function() {
        callback();
    });
}

/**
 *
 * @param {Array<String>} wantedColumns - name's of wanted columns to be returned
 * @param {Array<Object>} allColumns - dataTable columns
 * @return array of filtered columns
 */
export function filterColumns(wantedColumns, allColumns) {
    if(wantedColumns == undefined || wantedColumns == null) return [];
    if(allColumns == undefined || allColumns == null) return [];

    return allColumns.filter(column => wantedColumns.includes(column["name"]));
}

/**
 *
 * @param {Array<String>} unWanted - name's of unWanted columns to NOT be returned
 * @param {Array<Object>} allColumns - dataTable columns
 * @return array of filtered columns
 */
export function filterColumnsOut(unWanted, allColumns) {
    if(unWanted == undefined || unWanted == null) return [];
    if(allColumns == undefined || allColumns == null) return [];

    return allColumns.filter(column => !unWanted.includes(column["name"]));
}

/**
 * Get and set list of options, for searchEngines select. Auto select value.
 *
 * @param {String} dataUrl - url of endpoint where we obtain data for select
 * @param {*} valueToSelect - value that will be auto selected (if is in the list of obtained data)
 */
export async function setSearchEnginesSelect(dataUrl, valueToSelect) {
    await $.ajax({url: dataUrl, success: function(inputData) {
        let searchEngineSelect =  document.getElementById("searchEngineSelect");
        //Remove all options except the default one
        while(searchEngineSelect.options.length > 1)
            searchEngineSelect.remove(1);

        //Add new options
        let isInList = false;
        for(let i = 0; i < inputData.length; i++) {
            searchEngineSelect.add(new Option(inputData[i], inputData[i]));
            if(inputData[i] === valueToSelect) isInList = true;
        }

        //If valueToSelect is in list of valued, select it
        if(isInList) $("#searchEngineSelect").val(valueToSelect);

        //Refresh object
        $("#searchEngineSelect").selectpicker('refresh');
    }});
}

/**
 * Get and set list of options, for webPages select. Auto select value.
 *
 * @param {String} dataUrl - url of endpoint where we obtain data for select
 * @param {*} valueToSelect - value that will be auto selected (if is in the list of obtained data)
 */
export async function setWebPagesSelect(dataUrl, valueToSelect) {
    await $.ajax({url: dataUrl, success: function(mapOfDirs) {
        //Get object, select
        let webPageSelect = document.getElementById('webPageSelect');
        //Remove all options except the default one
        while(webPageSelect.options.length > 1)
            webPageSelect.remove(1);

        //Add new options
        let isInList = false;
        for (const [key, value] of Object.entries(mapOfDirs)) {
            webPageSelect.add(new Option(value, key));
            if(valueToSelect == key) isInList = true;
        }

        //If valueToSelect is in list of valued, select it
        if(isInList) $("#webPageSelect").val(valueToSelect);

        //Refresh object
        $("#webPageSelect").selectpicker('refresh');
    }});
}

/**
 * Return true if input value is valid String, that is not empty, else false.
 *
 * @param {any} value - value to check
 * @returns true/false
 */
function isStringParamValid(value) {
    //Must be type of string
    if(typeof value !== "string") return false;
    //Test of emptiness
    if(value === undefined || value === null || value === "") return false;
    //String is valied
    return true;
}

/**
 * Return true is input value is valid Number value (> 0, if cantBeNegative = true), else false.
 *
 * @param {any} value - value to check
 * @param {Boolean} cantBeNegative - true, control that value is > 0
 * @returns true/false
 */
function isNumberParamValid(value, cantBeNegative = true) {
    //Is value type of number ?
    if(typeof value !== "number") {
        //If type is NOT number but it's string, we can convert
        if(typeof value === "string")
            value = parseInt(value); //Do a convert
        else return false;
    }
    //Test of emptiness
    if(value === undefined || value === null || isNaN(value)) return false;
    //Check of negative value
    if(!cantBeNegative && value < 0) return false;
    //Number is valid
    return true;
}