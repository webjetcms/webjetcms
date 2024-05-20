# Amcharts

Library [amcharts](http://amcharts.com) is used to display graphs. For WebJET we have purchased a commercial OEM version.

## Amcharts initialization

In the file [app.js](../../../../../src/main/webapp/admin/v9/src/js/app.js) asynchronous initialization of amcharts version 5 is ready. This will ensure that the library is loaded and initialized only when needed.

Initiation `Amchart5` is done by calling `window.initAmcharts()`. After initialization, the event is fired `WJ.initAmcharts.success` at `window` facility. It is possible to listen to this event and then already use the object `window.am5`, through which the library is accessible. But a better solution is to use `then` function as `window.initAmcharts().then(module => { ... } );`.

In addition to the building with the library itself, there are also objects `window.am5xy` a `window.am5percent`, which are indispensable for creating and working with amcharts.

In addition to setting the license, this file also sets the themes for the charts. These themes will affect the appearance of the charts, the animations as well as the color palette used. In addition to the amcharts themes used, we also use a custom theme to set the color palette to be used by the charts. This theme can be found in the file [amcharts.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/amcharts.js) as a class `WebjetTheme`.

# Working with graphs

To work with graphs we created a javascript file [chart-tools.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js), which is available as `window.ChartTools` object. This file contains custom functions and classes that provide simplified work with graphs created using the library `Amchart5`. The goal was to create modular code that can create/set/edit charts according to certain specifications. This code supports the creation of charts of type `Line`, `Pie` a `Bar` and takes care of all logical as well as graphical settings of the graphs.

## Create a new chart

The creation of a graph can be divided into several steps, where the first step is to initialize the amcharts library, by calling `window.initAmcharts()`. Once the library is initialized, we can use an ajax call to retrieve the data for the graph. We will use the data and other parameters to create **Form** graph (to be explained later), which we store in a pre-created variable. The last step is to create the graph using the prepared `createAmchart()` functions available via `window.ChartTools`.

Example of use:

```javascript
//Vytvorenie premennej, v ktorej bude udržiavať inštanciu grafu
let pieChartVisit;

//Vyvolanie udalosti na inicializáciu knižnice
window.initAmcharts().then((module) => {
	//Získanie dát pre graf pomocou ajax volania
	$.ajax({
		url: getGraphUrl(),
		success: function (result) {
			//Vytvorenie inštancie FORMULÁRU grafu a jej uloženie do premennej
			pieChartVisits = new ChartTools.PieChartForm("visits", "name", "[[#{stat.top.pieChart}]]", "top-pieVisits", result["content"]);

			//Vytvorenie grafu pomocou premennej s inštanciou FORMULÁRU grafu
			ChartTools.createAmchart(pieChartVisits);
		},
	});
});

//Div s id=graphsDiv môže obsahovať aj viacero grafov a je to kontainer na grafy s určitým nastaveným štýlom
//Div s id=top-pieVisits predstavuje elemnt ku ktorému bude priradený graf
<div id="graphsDiv">
	<div id="top-pieVisits" class="amcharts"></div>
</div>;
```

This was a demonstration of what creating/setting up a chart can look like. The important thing here is the class `PieChartForm`, which represents a graph of type `PIE`, its data and all the parameters needed to create and set up the chart correctly. The support for the 3 chart types is represented by the 3 classes (or as we mentioned forms) available from `window.ChartTools` :
- Class `PieChartForm`, represents graphs of the type `Pie`
- Class `BarChartForm`, represents graphs of the type `Bar`
- Class `LineChartForm`, represents graphs of the type `Line`
More information about what the individual parameters of these classes do, their format and impact on the generated graph are described in the file [Documentation](statjs.md).

### HTML graph element

As we could notice in the chart creation example, we need to add an HTML div element to contain this chart. When specifying the id, keep in mind that **each element representing a graph must have a unique ID across the entire project**since the am5 library needs the element id when creating the graph and remembers these id values at the global level. In situations where we try to create multiple graphs for an element with the same id (albeit in a different application), am5 will throw an error and the graph will not be created. We recommend to compose `id` chart element as "application name - chart name", e.g. `top-pieVisits`.

### Edit chart data

For some graphs, we can notice the modification of the extracted data before it is further assigned to the graph. This modification is related to the form of the data, filtering and so on. Most often we encounter it when creating/modifying a chart of type `Line`. If the data from the BackEnd part is returned in the correct (used) format, we can use the available `convertDataForLineChart()` function as shown in the following example. For more information on how this function works, see the file [chart-tools.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js).

```javascript
//Získanie dát pre graf pomocou Ajax
await $.ajax({
	url: getGraphUrl("lineVisits"),
	success: function (result) {
		//Úprava získaných dáta pre graf s použitím convertDataForLineChart() fn
		let convertedData = ChartTools.convertDataForLineChart(result);

		//Použitie upravených dát pri vytvárani grafu typu LINE
		lineChartVisits = new ChartTools.LineChartForm(ChartTools.getLineChartYAxeNameObjs(["visits"], [undefined]), "dayDate", "[[#{stat.top.lineChart}]]", "top-lineVisits", convertedData, ChartTools.DateType.Days);
		ChartTools.createAmchart(lineChartVisits);
	},
});
```

When editing your own data, it is important to understand what data format is supported by the chart, so it is recommended to check this in the library documentation.

## Updating an existing chart

Since charts always include an external filter whose role is to filter in the chart data, it is necessary to know how to edit the charts.

Example of use:

```javascript
//Príklad funkcie na aktualizáciu dát grafu
async function getDataAndUpdateAmcharts() {
	//Získanie nových dát pre graf
	await $.ajax({
		url: getGraphUrl("pieVisits"),
		success: function (result) {
			//Nastavenie nových dát grafu do formuláru
			pieChartVisits.chartData = result["content"];

			//Vyvolanie aktualizácie grafu
			ChartTools.updateChart(pieChartVisits);
		},
	});
}
```

As we could see in the example above, we can change the chart data at any time during the program run. We just need to replace the old graph data with the new one in the created instance of the graph form and use the available function `updateChart()` we will start the chart update, which will take care of everything that needs to be done.

## Updating the table

Sometimes when filtering chart data with an external filter we want to filter table data as well, but this can be a problem as the external filter may not be linked to the table (or we have multiple tables and only one of them can be linked to the external filter). Therefore, we need a way to filter these tables.

Example of use:

```javascript
//Pridanie parametrov do url
topDataTable.ajax.url(WJ.urlAddParam(url, "chartType", ChartType.Not_Chart));
topDataTable.ajax.url(WJ.urlAddParam(topDataTable.ajax.url(), "searchRootDir", $("#rootDir").val()));
topDataTable.ajax.url(WJ.urlAddParam(topDataTable.ajax.url(), "searchFilterBotsOut", $("#botFilterOut").is(":checked")));

//Refresh tabuľky
topDataTable.ajax.reload();
```

From the example we can see that sometimes it is not enough to restore the table, but we need to add parameters to the url address, or just edit them. Then it's the job of BackEnd to get these parameters from the url and return the appropriate data for the table.
