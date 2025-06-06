<template>
    <div class="col-md-6">
        <h6 class="server-monitoring-table-header mt-2"><i :class="displayData.icon" style="margin: 0px 10px"></i> {{ displayData.tableName }}</h6>
        <table class="monitoring-table">
            <tr v-for="(data, key, index) in sortedData" :key="index" class="table-data" :data-index="index">
                <td v-text="translate(key)" style="white-space: nowrap"></td>
                <td>
                    <strong v-if="['serverActualTime', 'remoteIP', 'serverIP', 'cacheItems', 'serverCpus', 'dbIdle', 'sessionsTotal'].includes(key)">{{ data }}</strong>
                    <span v-else>{{ data }}</span>
                </td>
            </tr>
        </table>
    </div>
</template>

<script lang="js">

    export default {
        name: 'server-monitoring-tables',
        props: ['displayData', 'keyIndex'],
        data() {
            return {
                formatedData: null,
                sortedData: null
            }
        },

        watch: {
            'displayData.serverRuntime': {
                handler(newVal, oldVal) {
                    this.formatData();
                }
            },
            'displayData.serverActualTime': {
                handler(newVal, oldVal) {
                    this.formatData();
                }
            },
            'displayData.serverStartTime': {
                handler(newVal, oldVal) {
                    this.formatData();
                }
            }
        },

        created() {
            this.formatData();
        },

        methods: {
            formatData() {
                this.formatedData = Object.keys(this.displayData)
                .filter(key => key != 'tableName')
                .reduce((obj, key) => {
                    obj[key] = this.displayData[key];
                    return obj;
                }, {});

                if (this.keyIndex == 0) {
                    this.sortedData = {
                        'wjVersion': this.formatedData.wjVersion,
                        'licenseExpirationDate': this.formatedData.licenseExpirationDate,
                        'serverActualTime': this.formatedData.serverActualTime,
                        'serverStartTime': this.formatedData.serverStartTime,
                        'serverRuntime': this.formatedData.serverRuntime,
                        'remoteIP': this.formatedData.remoteIP,
                        'serverIP': this.formatedData.serverIP,
                        'serverContry': this.formatedData.serverContry,
                        'serverLanguage': this.formatedData.serverLanguage,
                        'serverCpus': this.formatedData.serverCpus,
                        'clusterNodeName': this.formatedData.clusterNodeName
                    }
                } else if (this.keyIndex == 1) {
                    this.sortedData = {
                        'swRuntime': this.formatedData.swRuntime,
                        'swVmVersion': this.formatedData.swVmVersion,
                        'swVmName': this.formatedData.swVmName,
                        'swJavaVersion': this.formatedData.swJavaVersion,
                        'swJavaVendor': this.formatedData.swJavaVendor,
                        'swSpringVersion': this.formatedData.swSpringVersion,
                        'swSpringDataVersion': this.formatedData.swSpringDataVersion,
                        'swSpringSecurityVersion': this.formatedData.swSpringSecurityVersion,
                        'swServerName': this.formatedData.swServerName,
                        'swServerOs': this.formatedData.swServerOs,
                        'swServerOsVersion': this.formatedData.swServerOsVersion
                    }
                } else if (this.keyIndex == 2) {
                    this.sortedData = {
                        'dbTotal': this.formatedData.dbTotal,
                        'dbActive': this.formatedData.dbActive,
                        'dbIdle': this.formatedData.dbIdle,
                        'dbWaiting': this.formatedData.dbWaiting,
                    }
                } else if (this.keyIndex == 3) {
                    this.sortedData = {
                        'memTotal': this.formatedData.memTotal,
                        'memFree': this.formatedData.memFree,
                        'memUsed': this.formatedData.memUsed,
                        'memMax': this.formatedData.memMax,
                        'cacheItems': this.formatedData.cacheItems,
                        'sessionsTotal': this.formatedData.sessionsTotal
                    }
                    if (typeof this.formatedData.sessionsList != "undefined" && this.formatedData.sessionsList!=null) {
                        let that = this;

                        //console.log(that.sortedData);
                        this.formatedData.sessionsList.forEach(function(obj) {
                            //console.log(obj.label);
                            that.sortedData["- "+obj.label] = obj.value;
                        });
                    }
                }
            },
            translate(key) {
                if (key.startsWith("-")) return key;
                var translated = "";

                //console.log("this=", this, "preklad=", this.$cpuLoad, "2=", this["$serverContry"]);
                translated = this["$"+key];

                if (typeof translated == "undefined" || translated === "") return key;
                return translated;
            }
        }
    }
</script>