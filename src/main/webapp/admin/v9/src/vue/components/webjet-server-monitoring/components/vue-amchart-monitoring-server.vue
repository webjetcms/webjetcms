<template>
    <section class="col-lg-6">
        <h6 class="amchart-header">{{ name }}
        </h6>
        <div :id="chartId" ref="chartdiv" class="amcharts" style="height: 300px;"></div>
    </section>
</template>

<script lang="js">
    export default {
        name: 'webjet-amchart-server-monitoring-app',
        props: ['name', 'additionalData', 'type', 'chartId', 'chartData'],

        data() {
            return {
                maxChartDataLength: 120,
                allreadyCreated: false
            }
        },

        mounted() {
            this.createAmchart();
        },

        beforeDestroy() {
            if (this.chart) {
                this.chart.dispose();
            }
        },

        methods: {
            updateAmchart() {
                var dis = this;

                if (typeof this.chart != "undefined") ChartTools.addData(this.chart.series.values, this.chart.xAxes.values[0],  this.chartData, this.type);
            },
            createAmchart() {
                window.initAmcharts().then(module => {

                    if(this.allreadyCreated != true) {
                        this.allreadyCreated = true;
                        if(this.type == "memoryAmchart") {
                            ChartTools.createServerMonitoringChart("serverMonitoring-lineChartMemory", this.type).then((chart) => {
                                this.chart = chart;
                            });
                        } else if(this.type == "cpuAmchart") {
                            ChartTools.createServerMonitoringChart("serverMonitoring-lineChartCpu", this.type).then((chart) => {
                                this.chart = chart;
                                this.allreadyCreated = true;
                            });
                        }
                    }

                });
            }
        }
    }
</script>
