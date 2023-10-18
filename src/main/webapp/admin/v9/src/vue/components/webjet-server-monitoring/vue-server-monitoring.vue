<template>
    <section class="server-monitoring-section">
        <header class="server-monitoring-header md-breadcrumb" v-if="iscomplex">
            <div id="secondsDropdown" class="dropdown">
                <div class="secondsSelectorWrapper" :title="`${$updateInterval}`">
                    <button class="btn btn-sm buttons-collection dropdown-toggle btn-outline-secondary buttons-settings" type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fal fa-clock"></i>
                        <span id="seconds-display">
                            5s
                        </span>
                    </button>
                    <ul class="dropdown-menu" aria-labelledby="dropdownMenuButton">
                        <li><a class="dropdown-item" href="#">5s</a></li>
                        <li><a class="dropdown-item" href="#">10s</a></li>
                        <li><a class="dropdown-item" href="#">20s</a></li>
                        <li><a class="dropdown-item" href="#">30s</a></li>
                        <li><a class="dropdown-item" href="#">40s</a></li>
                        <li><a class="dropdown-item" href="#">50s</a></li>
                        <li><a class="dropdown-item" href="#">60s</a></li>
                        <li><a class="dropdown-item" href="#">120s</a></li>
                    </ul>
                </div>
            </div>
        </header>
        <webjet-hardware-monitoring-tables v-if="iscomplex" ref="hardwareMonitoring" :obj="monitoringData"></webjet-hardware-monitoring-tables>
        <div class="amchart-monitoring-server row row-no-padding">
            <webjet-amchart-monitoring-server ref="memoryAmchart" type="memoryAmchart" :name="`${$occupancyMemory}`" chartId="serverMonitoring-lineChartMemory" :chartData="chartData" @stop-request="requestRefreh"></webjet-amchart-monitoring-server>
            <webjet-amchart-monitoring-server ref="cpuAmchart" type="cpuAmchart" :name="`${$cpuLoad}`" chartId="serverMonitoring-lineChartCpu" :chartData="chartData" @stop-request="requestRefreh"></webjet-amchart-monitoring-server>
        </div>
        <div class="server-monitoring-tables row row-no-padding" v-if="iscomplex">
            <webjet-server-monitoring-tables
                v-for="(table, index) in monitoringTablesData"
                :key="index"
                :keyIndex="index"
                :displayData="monitoringTablesData[index]"
            ></webjet-server-monitoring-tables>
        </div>
    </section>
</template>

<script lang="js">
    import ServerMonitoringTables from './components/vue-server-monitoring-tables.vue';
    import HardwareMonitoringTables from './components/vue-hardware-monitoring-tables.vue';
    import AmchartMonitoringServer from './components/vue-amchart-monitoring-server.vue';

    export default {
        name: 'webjet-server-monitoring-app',
        props: ['iscomplex'],
        data() {
            return {
                stopRequest: false,
                globalThis: globalThis,
                refreshInterval: 5000,
                interval: null,
                monitoringTablesData: [],
                monitoringData: Object,
                chartData: Object,
                firstTableItems: ['serverRuntime', 'serverActualTime', 'serverStartTime', 'remoteIP', 'serverIP', 'serverContry', 'serverLanguage', 'dbActive', 'dbIdle', 'serverCpus', 'cacheItems', 'sessionsTotal', 'sessionsList'],
                secondTableItems: ['swJavaVendor', 'swRuntime', 'swVmName', 'swJavaVersion', 'swVmVersion', 'swServerName', 'swServerOs', 'swServerOsVersion']
            }
        },
        components: {
            'webjet-server-monitoring-tables': ServerMonitoringTables,
            'webjet-hardware-monitoring-tables': HardwareMonitoringTables,
            'webjet-amchart-monitoring-server': AmchartMonitoringServer
        },
        created() {
            //console.log('webjet server monitoring was created')
        },
        mounted() {
            if (window.nopermsJavascript["cmp_server_monitoring"]===true) {
                //user nema monitoring prava, schovame grafy, toto sa deje na uvodnej stranke
                $("section.server-monitoring-section").hide();
            } else {
                this.updateData();
                this.startInterval();
                const dhis = this;
                $("#pills-translation-keys-language-tab").replaceWith($("#secondsDropdown").html());
                $("header.server-monitoring-header").remove();
                $('li.nav-item .dropdown-menu a').click(function(e){
                    const val = parseInt($(e.target).html());
                    //console.log("val=", val);
                    $('#seconds-display').html(`${val}s`);
                    globalThis.clearInterval(dhis.interval);
                    dhis.refreshInterval = +val + '000';
                    dhis.startInterval();
                });
                //console.log()
                $("div.secondsSelectorWrapper").tooltip();
                $("div.full-space").tooltip();
            }
        },
        methods: {
            requestRefreh(e) {
                this.stopRequest = e;
            },
            updateData() {
                var dis = this;

                if (this.iscomplex) {
                    this.$refs.hardwareMonitoring.updateSpace(Math.floor(Math.random() * 20));
                }

                $.getJSON("/admin/rest/monitoring/actual", function(data){
                    dis.monitoringData = data;

                    dis.chartData = {
                        "serverActualTime": data['serverActualTime'],
                        "memUsed": data["memUsed"],
                        "memTotal": data["memTotal"],
                        "memFree": data["memFree"],
                        "cpuUsage": data["cpuUsage"]
                    }

                    dis.createTableJson();
                }).done(function(){
                    if (!dis.stopRequest) {
                        dis.$refs.memoryAmchart.updateAmchart();
                        dis.$refs.cpuAmchart.updateAmchart();
                    }
                });
            },
            startInterval() {
                this.updateData();
                this.interval = setInterval(() => {
                    this.updateData();
                }, +this.refreshInterval);
            },
            createTableJson() {
                var dis = this;
                var t = {};
                var d = {};

                $.each(this.monitoringData, function(name, value){
                    if (dis.firstTableItems.includes(name)) {
                        t["tableName"] = window.vueMonitoringApp.config.globalProperties.$vseobecneInformacie;
                        if (name == "serverIP") {
                            t[name] = value[0];

                            return;
                        }
                        t[name] = value;
                    } else if (dis.secondTableItems.includes(name)) {
                        d["tableName"] = window.vueMonitoringApp.config.globalProperties.$informacieOsoftveri;
                        d[name] = value;
                    }
                });

                dis.monitoringTablesData = [];
                dis.monitoringTablesData.push(t, d);
                dis.formatData();
            },
            formatData() {
                this.monitoringTablesData[0].serverRuntime = this.msToDays(this.monitoringTablesData[0].serverRuntime);
                this.monitoringTablesData[0].serverStartTime = moment(this.monitoringTablesData[0].serverStartTime).format('DD.MM.YYYY HH:mm:ss');
                this.monitoringTablesData[0].serverActualTime = moment(this.monitoringTablesData[0].serverActualTime).format('DD.MM.YYYY HH:mm:ss');
            },
            msToDays(ms) {
                var days  = Math.floor(ms / (24*60*60*1000)),
                daysms    = ms % (24*60*60*1000),
                hours     = Math.floor((daysms)/(60*60*1000)),
                hoursms   = ms % (60*60*1000),
                minutes   = Math.floor((hoursms)/(60*1000)),
                minutesms = ms % (60*1000),
                sec       = Math.floor((minutesms)/(1000));

                var text = window.vueMonitoringApp.config.globalProperties.$uptime;
                text = text.replace("{1}", days);
                text = text.replace("{2}", hours);
                text = text.replace("{3}", minutes);
                var secText = window.vueMonitoringApp.config.globalProperties.$uptimeSeconds;
                secText = secText.replace("{1}", sec);

                return text + " " + secText;
            }
        }
    }
</script>



