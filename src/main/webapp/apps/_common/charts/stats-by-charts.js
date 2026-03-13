export class StatsByCharts {

    /**
     * Creates a new StatsByCharts instance.
     * @param {Object} options - Configuration options.
     * @param {string} options.targetSelector - CSS selector of the element where charts will be rendered.
     * @param {string} [options.id="stats-by-charts"] - Unique ID prefix used for chart DOM elements.
     * @param {Function} [options.chartSettingBtnFn] - Callback invoked when a chart's settings button is clicked.
     */
    constructor(options = {}) {
        this.targetSelector = options.targetSelector || null;
        this.id = options.id || "stats-by-charts";
        this.chartsInstances = {};
        this.chartSettingBtnFn = options.chartSettingBtnFn || null;
    }

    /**
     * Initialises the chart wrapper and renders all charts defined in the provided array.
     * Waits for the amCharts library to load before rendering.
     * @param {Object[]} chartsDefinitions - Array of chart definition objects to render.
     */
    createCharts(chartsDefinitions) {
        if (!this.targetSelector) {
            // Secure
            return;
        }

        const target = document.querySelector(this.targetSelector);
        if(target) {
            // Create wrapper that holds everything
            const wrapper = document.createElement("div");
            wrapper.id = this.id;
            target.appendChild(wrapper);

            this.wrapper = wrapper;

            window.initAmcharts().then(module => {
                this._renderCharts(chartsDefinitions);
            });
        }
    }

    /**
     * Updates one or more existing charts with new definitions.
     * Destroys each matching chart instance before re-rendering it with the new data.
     * @param {Object[]} newChartsDefinitions - Array of chart definition objects containing updated data.
     */
    async updateChart(newChartsDefinitions) {
        newChartsDefinitions.forEach(async newChartDef => {
            const chartUniqueId = this.id + "_" + newChartDef.id;

            const destroyReponse = await this._destroyChartBeforeUpdate(newChartDef.id);
            if(destroyReponse === true) {
                if(newChartDef.type === ChartTools.ChartType.Pie_Donut || newChartDef.type === ChartTools.ChartType.Pie_Classic) {
                    this._renderPieChart(newChartDef, chartUniqueId);
                }
                else if(newChartDef.type === ChartTools.ChartType.Bar_Vertical || newChartDef.type === ChartTools.ChartType.Bar_Horizontal) {
                    this._renderBarChart(newChartDef, chartUniqueId);
                }
                else if(newChartDef.type === ChartTools.ChartType.Table) {
                    this._renderTableChart(newChartDef, chartUniqueId);
                }
                else if(newChartDef.type === ChartTools.ChartType.Word_Cloud) {
                    this._renderWordCloudChart(newChartDef, chartUniqueId);
                }
                else if(newChartDef.type === ChartTools.ChartType.Double_Pie) {
                    this._renderDoublePieChart(newChartDef, chartUniqueId);
                }
            }
        });
    }

    /**
     * Destroys an existing chart instance and removes its header element from the DOM.
     * @param {string|null} chartId - The ID of the chart to destroy.
     * @returns {Promise<boolean>} Resolves to `true` if the chart was found and destroyed, `false` otherwise.
     */
    async _destroyChartBeforeUpdate(chartId = null) {
        if(chartId && this.chartsInstances[chartId]) {
            // Remove chart instance from root (amchart)
            await ChartTools.destroyChart(this.chartsInstances[chartId]);

            // Remove also headline, because it would stack up
            const headline = document.getElementById(this.id + "_" + chartId + "_container").querySelector(".amchart-header");
            if(headline) { headline.remove(); }

            return true;
        }
        return false;
    }

    /**
     * Creates DOM containers and settings buttons for each chart, then delegates rendering
     * to the appropriate type-specific render method.
     * @param {Object[]} chartsDefinitions - Array of chart definition objects to render.
     */
    _renderCharts(chartsDefinitions) {
        if(this.wrapper && chartsDefinitions) {
            chartsDefinitions.forEach(chartDef => {

                const chartUniqueId = this.id + "_" + chartDef.id;

                const chartContainer = document.createElement("div");
                chartContainer.id = chartUniqueId + "_container";
                chartContainer.classList.add("stat-chart-wrapper");
                this.wrapper.appendChild(chartContainer);

                 // Here will be inserted chart
                const chartDiv = document.createElement("div");
                chartDiv.id = chartUniqueId;
                chartDiv.classList.add("amcharts");
                chartContainer.appendChild(chartDiv);

                const button = document.createElement("button");
                button.classList.add("btn", "btn-sm", "btn-outline-secondary", "chart-more-btn");

                button.addEventListener("click", this.chartSettingBtnFn ? this.chartSettingBtnFn.bind(this, chartDef) : () => {
                    console.log("click on chart settings", chartDef.id);
                });

                const spanIcon = document.createElement("span");
                const icon = document.createElement("i");
                icon.classList.add("ti", "ti-settings");
                spanIcon.appendChild(icon);
                button.appendChild(spanIcon);

                chartContainer.prepend(button);

                if(chartDef.type === ChartTools.ChartType.Pie_Donut || chartDef.type === ChartTools.ChartType.Pie_Classic) {
                    this._renderPieChart(chartDef, chartUniqueId);
                }
                else if(chartDef.type === ChartTools.ChartType.Bar_Vertical || chartDef.type === ChartTools.ChartType.Bar_Horizontal) {
                    this._renderBarChart(chartDef, chartUniqueId);
                }
                else if(chartDef.type === ChartTools.ChartType.Table) {
                    this._renderTableChart(chartDef, chartUniqueId);
                }
                else if(chartDef.type === ChartTools.ChartType.Word_Cloud) {
                    this._renderWordCloudChart(chartDef, chartUniqueId);
                }
                else if(chartDef.type === ChartTools.ChartType.Double_Pie) {
                    this._renderDoublePieChart(chartDef, chartUniqueId);
                }
            });
        }
    }

    /**
     * Renders a pie or donut chart using the provided chart definition.
     * @param {Object} chartDef - Chart definition object (type, title, values, axis names, colour scheme).
     * @param {string} chartUniqueId - Unique DOM element ID for the chart.
     */
    _renderPieChart(chartDef, chartUniqueId) {
        const chartConfig = {
            yAxeName: this._ifInvalidReturnDefault(chartDef.yAxeName, "count"),
            xAxeName: this._ifInvalidReturnDefault(chartDef.xAxeName, "name"),
            chartTitle: chartDef.title,
            chartDivId: chartUniqueId,
            chartData: chartDef.values,
            innerRadius: chartDef.type === ChartTools.ChartType.Pie_Donut ? 75 : 0,
            leftLegendPosition: true,
            legendValueText: "[bold]{count}[/]",
            colorScheme: chartDef.chart_colorScheme
        }

        let pieChart = new ChartTools.PieChartForm(chartConfig);
        ChartTools.createAmchart(pieChart);
        this.chartsInstances[chartDef.id] = pieChart;
    }

    /**
     * Renders a vertical or horizontal bar chart using the provided chart definition.
     * @param {Object} chartDef - Chart definition object (type, title, values, axis names, colour scheme).
     * @param {string} chartUniqueId - Unique DOM element ID for the chart.
     */
    _renderBarChart(chartDef, chartUniqueId) {
            const chartConfig = {
                yAxeName: this._ifInvalidReturnDefault(chartDef.yAxeName, "name"),
                xAxeName: this._ifInvalidReturnDefault(chartDef.xAxeName, "count"),
                chartTitle: chartDef.title,
                chartDivId: chartUniqueId,
                chartData: chartDef.values,
                horizontal: chartDef.type === ChartTools.ChartType.Bar_Horizontal,
                colorScheme: chartDef.chart_colorScheme
            }

            let barChart = new ChartTools.BarChartForm(chartConfig);
            ChartTools.createAmchart(barChart);
            this.chartsInstances[chartDef.id] = barChart;
    }

    /**
     * Renders a table chart using the provided chart definition.
     * Falls back to ["name", "count"] column names when `paramsNames` is missing or invalid.
     * @param {Object} chartDef - Chart definition object (title, values, paramsNames, colour scheme).
     * @param {string} chartUniqueId - Unique DOM element ID for the chart.
     */
    _renderTableChart(chartDef, chartUniqueId) {
        let paramsNames = chartDef.paramsNames == null || chartDef.paramsNames === undefined || Array.isArray(chartDef.paramsNames) == false ? ["name", "count"] : chartDef.paramsNames;

        const chartConfig = {
            paramsNames: paramsNames,
            chartTitle: chartDef.title,
            chartDivId: chartUniqueId,
            chartData: chartDef.values,
            colorScheme: chartDef.chart_colorScheme
        }

        let tableChart = new ChartTools.TableChartForm(chartConfig);
        ChartTools.createAmchart(tableChart);
        this.chartsInstances[chartDef.id] = tableChart;
    }

    /**
     * Renders a word-cloud chart using the provided chart definition.
     * @param {Object} chartDef - Chart definition object (title, values, axis names).
     * @param {string} chartUniqueId - Unique DOM element ID for the chart.
     */
    _renderWordCloudChart(chartDef, chartUniqueId) {
        const chartConfig = {
            chartDivId: chartUniqueId,
            chartData: chartDef.values,
            chartTitle: chartDef.title,
            mode: "line",
            xAxeName: this._ifInvalidReturnDefault(chartDef.xAxeName, "name"),
            yAxeName: this._ifInvalidReturnDefault(chartDef.yAxeName, "count")
        }

        let wordCloudChart = new ChartTools.WordCloudChartForm(chartConfig);
        ChartTools.createAmchart(wordCloudChart);
        this.chartsInstances[chartDef.id] = wordCloudChart;
    }

    /**
     * Renders a double (nested) pie chart using the provided chart definition.
     * @param {Object} chartDef - Chart definition object (title, values, inner/outer axis names).
     * @param {string} chartUniqueId - Unique DOM element ID for the chart.
     */
    _renderDoublePieChart(chartDef, chartUniqueId) {
        const chartConfig = {
            yAxeName_inner: this._ifInvalidReturnDefault(chartDef.yAxeName_inner, "count"),
            yAxeName_outer: this._ifInvalidReturnDefault(chartDef.yAxeName_outer, "count"),
            xAxeName: this._ifInvalidReturnDefault(chartDef.xAxeName, "name"),
            chartDivId: chartUniqueId,
            chartData: chartDef.values,
            chartTitle: chartDef.title
        }

        let doublePieChart = new ChartTools.DoublePieChartForm(chartConfig);
        ChartTools.createAmchart(doublePieChart);
        this.chartsInstances[chartDef.id] = doublePieChart;
    }

    /**
     * Returns `defaultValue` when `value` is `null`, `undefined`, or an empty string;
     * otherwise returns `value` unchanged.
     * @param {*} value - The value to check.
     * @param {*} defaultValue - The fallback value to use when `value` is invalid.
     * @returns {*} The original value or the default.
     */
    _ifInvalidReturnDefault(value, defaultValue) {
        if(value === undefined || value === null || value.length < 1) {
            return defaultValue;
        } else {
            return value;
        }
    }
}