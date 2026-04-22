# Amcharts

The [amcharts](http://amcharts.com) library is used to display charts. We have purchased a commercial OEM version for WebJET.

## Amcharts initialization

In the file [app.js](../../../../../../src/main/webapp/admin/v9/src/js/app.js) an asynchronous initialization of amcharts version 5 is prepared. This ensures that the library is loaded and initialized only when needed.

Initialization of `Amchart5` is done by calling ```window.initAmcharts()```. After initialization, event ```WJ.initAmcharts.success``` is called on ```window``` object. This event can be listened to and then used by object ```window.am5```, through which the library is accessible. However, a better solution is to use ```then``` function like ```window.initAmcharts().then(module => { ... } );```.

In addition to the object with the library itself, the objects ```window.am5xy```, ```window.am5percent``` and ```window.am5wc``` are also available, which are necessary for creating and working with amcharts charts.

In addition to setting the license, this file also sets the themes for the charts. These themes will affect the appearance of the charts and animation. In addition to the used amcharts themes, we also use our own theme for various graphic adjustments of the elements in the charts. This theme is located in the file [amcharts.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/amcharts.js) as the class `WebjetTheme`.

## Working with charts

To work with charts, we created a javascript file [chart-tools.js](../../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js), which is available as an ```window.ChartTools``` object. This file contains custom functions and classes that provide simplified work with charts created using the ```Amchart5``` library. The goal was to create modular code that will be able to create/setup/edit charts according to certain specifications.

This code supports the creation of graphs of the type:

- `LINE`
- `BAR_VERTICAL`
- `BAR_HORIZONTAL`
- `PIE_CLASSIC`
- `PIE_DONUT`
- `DOUBLE_PIE`
- `WORD_CLOUD`
- `TABLE`

and takes care of all logical and graphical chart settings.

!>**Warning:** Graphs of type `TABLE` do not actually use the `Amchart5` library, but are used through the same file to be together.

## Create a new chart

We can divide the creation of the chart into several steps, where the first step is to initialize the amcharts library, by calling ```window.initAmcharts()```. Once the library is initialized, we can use an ajax call to get the data for the chart. We will use the data and other parameters to create a **Form** of the chart (to be explained later), which we will store in a pre-created variable. The last step is to create the chart using the ready-made ```createAmchart()``` function accessible via ```window.ChartTools```.

Example of use:

```javascript
//Vytvorenie premennej, v ktorej bude udržiavať inštanciu grafu
let pieChartVisit;

//Vyvolanie udalosti na inicializáciu knižnice
window.initAmcharts().then(module => {
    //Získanie dát pre graf pomocou ajax volania
    $.ajax({url: getGraphUrl(), success: function(result) {

        // Vytvorenie objektu konfigurácie grafu (obsahuje všetky potrebné parametre k vytvoreniu grafu)
        const pieConfig = {
            yAxeName: "visits",
            xAxeName: "name",
            chartTitle: '[[#{stat.top.pieChart}]]',
            chartDivId: "top-pieVisits",
            chartData: result['content']
        };

        //Vytvorenie inštancie FORMULÁRU grafu a jej uloženie do premennej
        pieChartVisits = new ChartTools.PieChartForm(pieConfig);

        //Vytvorenie grafu pomocou premennej s inštanciou FORMULÁRU grafu
        ChartTools.createAmchart(pieChartVisits);
    }});
});

//Div s id=graphsDiv môže obsahovať aj viacero grafov a je to kontajner na grafy s určitým nastaveným štýlom
//Div s id=top-pieVisits predstavuje element ku ktorému bude priradený graf
<div id="graphsDiv">
    <div id="top-pieVisits" class="amcharts"></div>
</div>
```

This was a demonstration of how creating/setting up a chart might look. The important class here is ```PieChartForm```, which represents a chart of type ```PIE_DONUT```, its data, and all the parameters needed to properly create and set up the chart. Support for different chart types is represented through separate classes (or as we mentioned forms) available from ```window.ChartTools``` :

- class `LineChartForm`, represents graphs of type `LINE`
- class `BarChartForm`, represents graphs of type `BAR_VERTICAL` and `BAR_HORIZONTAL`
- class `PieChartForm`, represents graphs of type `PIE_CLASSIC` and `PIE_DONUT`
- class `DoublePieChartForm`, represents graphs of type `DOUBLE_PIE` (so essentially a double graph of type `PIE_DONUT`)
- class `WordCloudForm`, represents graphs of type `WORD_CLOUD`

More detailed information about what the individual parameters of these classes do, what their format is, and their impact on the generated graph are described in the [documentation] file (statjs.md).

!>**Note:** Historically, a notation without a configuration object was used, where parameters were entered directly into the constructor of the chart form class. This method is still supported, but we do not recommend using it. Example of such notation:

```javascript
    pieChartVisits = new ChartTools.PieChartForm("visits", "name", '[[#{stat.top.pieChart}]]', "top-pieVisits", result['content']);
```

### HTML chart element

As we could see in the example of creating a graph, it is necessary to add an HTML div element that will contain this graph. When specifying the id, it is necessary to keep in mind that **each element representing the graph must have a unique ID across the entire project**, since the am5 library needs the element's id when creating a graph and remembers these id values ​​at the global level. In a situation where we try to create multiple graphs for an element with the same id (even in a different application), am5 throws an error and the graph is not created. We recommend composing the `id` of the graph element as "application name - graph name", e.g. ```top-pieVisits```.

### Editing chart data

In some charts, we can notice the modification of the obtained data before it is assigned to the chart. This modification concerns the data form, filtering, etc. We most often encounter it when creating/editing a chart of type ```Line```. If the data from the BackEnd part is included in the correct (usable) format, we can use the available ```convertDataForLineChart()``` function as shown in the following example. More information on how this function works can be found in the file [chart-tools.js](../../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js).

```javascript
    //Získanie dát pre graf pomocou Ajax
    await $.ajax({url: getGraphUrl("lineVisits"), success: function(result) {
        //Úprava získaných dáta pre graf s použitím convertDataForLineChart() fn
        let convertedData = ChartTools.convertDataForLineChart(result);

        //
        const lineConfig = {
            yAxeNames: ChartTools.getLineChartYAxeNameObjs(["visits"], [undefined]),
            xAxeName: "dayDate",
            chartTitle: '[[#{stat.top.lineChart}]]',
            chartDivId: "top-lineVisits",
            chartData: convertedData,
            dateType: ChartTools.DateType.Days
        };

        //Použitie upravených dát pri vytváraní grafu typu LINE
        lineChartVisits = new ChartTools.LineChartForm(lineConfig);
        ChartTools.createAmchart(lineChartVisits);
    }});
```

When editing data yourself, it is important to understand what data format the chart supports, so we recommend checking the library documentation.

## Updating an existing chart

Since graphs always include an external filter whose task is to filter the graph data, it is necessary to know how to edit graphs.

Example of use:

```javascript
    //Príklad funkcie na aktualizáciu dát grafu
    async function getDataAndUpdateAmcharts() {
        //Získanie nových dát pre graf
        await $.ajax({url: getGraphUrl("pieVisits"), success: function(result) {
            //Nastavenie nových dát grafu do formuláru
            pieChartVisits.chartData =  result['content'];

            //Vyvolanie aktualizácie grafu
            ChartTools.updateChart(pieChartVisits);
        }});
    }
```

As we could see in the example above, we can change the chart data at any time during the program's runtime. All we need to do is replace the old chart data with the new one in the created graph form instance and use the available function ```updateChart()``` to start the chart update, which will take care of everything necessary.

## Updating a data table

Sometimes when filtering chart data using an external filter, we want to filter table data as well, but this can be a problem, as the external filter may not be linked to the table (or we have multiple tables and only one of them can be linked to the external filter). Therefore, we need a way to filter these tables.

Example of use:

```javascript
//Pridanie parametrov do url
topDataTable.setAjaxUrl(WJ.urlAddParam(url, "chartType", ChartType.Not_Chart));
topDataTable.setAjaxUrl(WJ.urlAddParam(topDataTable.getAjaxUrl(), "searchRootDir", $('#rootDir').val()));
topDataTable.setAjaxUrl(WJ.urlAddParam(topDataTable.getAjaxUrl(), "searchFilterBotsOut", $('#botFilterOut').is(':checked')));

//Refresh tabuľky
topDataTable.ajax.reload();
```

From the example we can see that sometimes it is not enough to just refresh the table, but we need to add parameters to the URL address, or just edit them. Then it is the job of the BackEnd which receives these parameters from the URL and returns the appropriate data for the table.