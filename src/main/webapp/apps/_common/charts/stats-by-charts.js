const ChartType = {
    Pie_Donut: "pie_donut",
    Pie_Classic: "pie_classic",
    Bar_Vertical: "bar_vertical",
    Bar_Horizontal: "bar_horizontal",
    Table: "table"
};

export class StatsByCharts {

    constructor(options = {}) {
        this.targetSelector = options.targetSelector || null;
        this.id = options.id || "stats-by-charts";
        this.chartsInstances = {};
        this.chartSettingBtnFn = options.chartSettingBtnFn || null;
    }

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

    async updateChart(newChartsDefinitions) {
        newChartsDefinitions.forEach(async newChartDef => {
            const chartUniqueId = this.id + "_" + newChartDef.id;

            const destroyReponse = await this._destroyChartBeforeUpdate(newChartDef.id);
            if(destroyReponse === true) {
                if(newChartDef.type === ChartType.Pie_Donut || newChartDef.type === ChartType.Pie_Classic) {
                    this._renderPieChart(newChartDef, chartUniqueId);
                }
                else if(newChartDef.type === ChartType.Bar_Vertical || newChartDef.type === ChartType.Bar_Horizontal) {
                    this._renderBarChart(newChartDef, chartUniqueId);
                }
                else if(newChartDef.type === ChartType.Table) {
                    this._renderTableChart(newChartDef, chartUniqueId);
                }
            }
        });
    }

    async _destroyChartBeforeUpdate(chartId = null) {
        if(chartId && this.chartsInstances[chartId]) {
            // Remove chart instance from root (amchart)
            await ChartTools.destroyChart(this.chartsInstances[chartId]);

            // Remove allso headline, because it would stack up
            const headline = document.getElementById(this.id + "_" + chartId + "_container").querySelector(".amchart-header");
            if(headline) { headline.remove(); }

            return true;
        }
        return false;
    }

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

                if(chartDef.type === ChartType.Pie_Donut || chartDef.type === ChartType.Pie_Classic) {
                    this._renderPieChart(chartDef, chartUniqueId);
                }
                else if(chartDef.type === ChartType.Bar_Vertical || chartDef.type === ChartType.Bar_Horizontal) {
                    this._renderBarChart(chartDef, chartUniqueId);
                }
                else if(chartDef.type === ChartType.Table) {
                    this._renderTableChart(chartDef, chartUniqueId);
                }
            });
        }
    }

    _renderPieChart(chartDef, chartUniqueId) {
        const chartConfig = {
            yAxeName: "count",
            xAxeName: "name",
            chartTitle: chartDef.title,
            chartDivId: chartUniqueId,
            chartData: chartDef.values,
            innerRadius: chartDef.type === ChartType.Pie_Donut ? 75 : 0,
            leftLegendPosition: true,
            legendValueText: "[bold]{count}[/]",
            colorScheme: chartDef.chart_colorScheme
        }

        let pieChart = new ChartTools.PieChartForm(chartConfig);
        ChartTools.createAmchart(pieChart);
        this.chartsInstances[chartDef.id] = pieChart;
    }

    _renderBarChart(chartDef, chartUniqueId) {
            const chartConfig = {
                yAxeName: "name",
                xAxeName: "count",
                chartTitle: chartDef.title,
                chartDivId: chartUniqueId,
                chartData: chartDef.values,
                horizontal: chartDef.type === ChartType.Bar_Horizontal,
                colorScheme: chartDef.chart_colorScheme
            }

            let barChart = new ChartTools.BarChartForm(chartConfig);
            ChartTools.createAmchart(barChart);
            this.chartsInstances[chartDef.id] = barChart;
    }

    _renderTableChart(chartDef, chartUniqueId) {
        const chartConfig = {
            categoryName: "name",
            valueName: "count",
            chartTitle: chartDef.title,
            chartDivId: chartUniqueId,
            chartData: chartDef.values,
            colorScheme: chartDef.chart_colorScheme
        }

        let tableChart = new ChartTools.TableChartForm(chartConfig);
        ChartTools.createAmchart(tableChart);
        this.chartsInstances[chartDef.id] = tableChart;
    }
}