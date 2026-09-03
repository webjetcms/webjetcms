/**
 * Configuration for the live server-monitoring component.
 *
 * @typedef {Object} WebjetServerMonitoringOptions
 * @property {boolean|string} [complex=false] - Enables detailed monitoring when set to `true` or `"true"`.
 * @property {Object.<string, string>} [labels={}] - Localized chart, table, disk, and interval labels.
 */

const TABLE_KEYS = [
    ["wjVersion", "licenseExpirationDate", "serverActualTime", "serverStartTime", "serverRuntime", "remoteIP", "serverIP", "serverContry", "serverLanguage", "serverCpus", "clusterNodeName"],
    ["swRuntime", "swVmVersion", "swVmName", "swJavaVersion", "swJavaVendor", "swSpringVersion", "swSpringDataVersion", "swSpringSecurityVersion", "swServerName", "swServerOs", "swServerOsVersion"],
    ["dbTotal", "dbActive", "dbIdle", "dbWaiting", "dbServerName"],
    ["memTotal", "memFree", "memUsed", "memMax", "cacheItems", "sessionsTotal"],
    ["HTTP Response Encoding", "file.encoding", "native.encoding", "sun.jnu.encoding", "LANG", "LC_ALL", "LC_CTYPE"]
];

const STRONG_KEYS = new Set(["serverActualTime", "remoteIP", "serverIP", "cacheItems", "serverCpus", "dbIdle", "sessionsTotal"]);

/**
 * Displays live server memory and CPU charts with optional detailed monitoring data.
 */
export class WebjetServerMonitoringElement extends HTMLElement {
    constructor() {
        super();
        this.complex = false;
        this.labels = {};
        this.refreshInterval = 5000;
        this.interval = null;
        this.request = null;
        this.charts = {};
        this.chartData = null;
        this._initialized = false;
        this._configured = false;
    }

    /**
     * Starts chart initialization and server polling after the configured element is connected.
     *
     * When monitoring permission is available, emits a bubbling, non-cancelable
     * `webjet-component-ready` event without detail after initialization starts.
     */
    connectedCallback() {
        if (!this._configured) return;
        if (this._initialized) return;
        this._initialized = true;
        this.render();
        if (window.nopermsJavascript?.cmp_server_monitoring === true) {
            this.hidden = true;
            return;
        }
        this._initializeCharts();
        this._startInterval();
        this.dataset.ready = "true";
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    /**
     * Stops polling, aborts the active request, and disposes chart resources.
     */
    disconnectedCallback() {
        clearInterval(this.interval);
        this.request?.abort?.();
        Object.values(this.charts).forEach(chart => chart?.dispose?.());
        this.charts = {};
        this._initialized = false;
    }

    /**
     * Applies the display mode and labels, restarting monitoring when already connected.
     *
     * @param {WebjetServerMonitoringOptions} [options={}] - Monitoring options.
     * @returns {WebjetServerMonitoringElement} The configured element.
     */
    configure({ complex = false, labels = {} } = {}) {
        this.complex = complex === true || complex === "true";
        this.labels = labels;
        this._configured = true;
        if (this.isConnected) {
            this.disconnectedCallback();
            this.connectedCallback();
        }
        return this;
    }

    /**
     * Builds live chart containers and the optional detailed monitoring controls and tables.
     */
    render() {
        const intervals = [5, 10, 20, 30, 40, 50, 60, 120];
        this.innerHTML = `
            <section class="server-monitoring-section">
                ${this.complex ? `<header class="server-monitoring-header md-breadcrumb"><div id="secondsDropdown" class="dropdown"><div class="secondsSelectorWrapper" title="${WJ.escapeHtml(this.labels.updateInterval || "")}"><button class="btn btn-sm buttons-collection dropdown-toggle btn-outline-secondary buttons-settings" type="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="ti ti-clock align-middle"></i><span id="seconds-display">5s</span></button><ul class="dropdown-menu">${intervals.map(value => `<li><a class="dropdown-item" href="#" data-seconds="${value}">${value}s</a></li>`).join("")}</ul></div></div></header><section class="monitoring-disk-space col" hidden></section>` : ""}
                <div class="amchart-monitoring-server row row-no-padding">
                    <section class="col-lg-6"><h6 class="amchart-header">${WJ.escapeHtml(this.labels.occupancyMemory || "")}</h6><div id="serverMonitoring-lineChartMemory" class="amcharts" style="height:300px"></div></section>
                    <section class="col-lg-6"><h6 class="amchart-header">${WJ.escapeHtml(this.labels.cpuLoad || "")}</h6><div id="serverMonitoring-lineChartCpu" class="amcharts" style="height:300px"></div></section>
                </div>
                ${this.complex ? `<div class="server-monitoring-tables row row-no-padding"></div>` : ""}
            </section>`;

        if (this.complex) {
            this.querySelectorAll("[data-seconds]").forEach(link => link.addEventListener("click", event => {
                event.preventDefault();
                const seconds = Number(link.dataset.seconds);
                this.refreshInterval = seconds * 1000;
                const display = document.querySelector("#seconds-display");
                if (display) display.textContent = `${seconds}s`;
                this._startInterval();
            }));
            const filterTab = document.querySelector("#pills-translation-keys-language-tab");
            const dropdown = this.querySelector("#secondsDropdown");
            if (filterTab && dropdown) filterTab.replaceWith(dropdown);
            this.querySelector("header.server-monitoring-header")?.remove();
            $("div.secondsSelectorWrapper").tooltip();
        }
    }

    /**
     * Restarts polling with an immediate refresh followed by the configured interval.
     */
    _startInterval() {
        clearInterval(this.interval);
        this._updateData();
        this.interval = setInterval(() => this._updateData(), this.refreshInterval);
    }

    /**
     * Fetches the current server snapshot unless another monitoring request is active.
     */
    _updateData() {
        if (this.request) return;
        this.request = $.getJSON("/admin/rest/monitoring/actual")
            .done(data => {
                this.chartData = {
                    serverActualTime: data.serverActualTime,
                    memUsed: data.memUsed,
                    memTotal: data.memTotal,
                    memFree: data.memFree,
                    cpuUsage: data.cpuUsage,
                    cpuUsageProcess: data.cpuUsageProcess
                };
                if (this.complex) {
                    this._renderDisk(data);
                    this._renderTables(data);
                }
                this._updateCharts();
            })
            .always(() => { this.request = null; });
    }

    _initializeCharts() {
        window.initAmcharts().then(() => Promise.all([
            ChartTools.createServerMonitoringChart("serverMonitoring-lineChartMemory", "memoryAmchart"),
            ChartTools.createServerMonitoringChart("serverMonitoring-lineChartCpu", "cpuAmchart")
        ])).then(([memory, cpu]) => {
            this.charts.memoryAmchart = memory;
            this.charts.cpuAmchart = cpu;
            this._updateCharts();
        }).catch(error => console.error("Unable to initialize server monitoring charts", error));
    }

    /**
     * Pushes the latest server snapshot to every initialized live chart.
     */
    _updateCharts() {
        if (!this.chartData) return;
        for (const [type, chart] of Object.entries(this.charts)) {
            if (chart) ChartTools.addData(chart.series.values, chart.xAxes.values[0], this.chartData, type);
        }
    }

    _renderDisk(data) {
        const section = this.querySelector(".monitoring-disk-space");
        if (!section || data.storageTotal == null) return;
        const free = Math.round(data.storageFree / 1000000000);
        const total = Math.round(data.storageTotal / 10000000000) * 10;
        const percentage = total > 0 ? free / total * 100 : 0;
        const step = total / 10;
        section.hidden = false;
        section.innerHTML = `<p class="empty-space-header">${WJ.escapeHtml(this.labels.emptySpaceOnDisk || "")}</p><div class="empty-space"><div class="monitoring-container empty-space-ticks-container"><div class="empty-space-ticks"></div></div><div class="monitoring-container full-space-container"><div class="full-space" title="${free} GB" style="width:${percentage}%"></div></div></div><div class="monitoring-container space-numbers-container"><div class="space-numbers"><span></span>${Array.from({length: 9}, (_, index) => `<span>${step * (index + 1)} GB</span>`).join("")}<span></span></div></div>`;
        $(section).find(".full-space").tooltip();
    }

    _renderTables(data) {
        const container = this.querySelector(".server-monitoring-tables");
        if (!container) return;
        const formatted = { ...data };
        formatted.serverRuntime = this._msToDays(data.serverRuntime);
        formatted.serverStartTime = moment(data.serverStartTime).format("DD.MM.YYYY HH:mm:ss");
        formatted.serverActualTime = moment(data.serverActualTime).format("DD.MM.YYYY HH:mm:ss");
        formatted.serverIP = Array.isArray(data.serverIP) ? data.serverIP[0] : data.serverIP;
        ["memTotal", "memFree", "memUsed", "memMax"].forEach(key => formatted[key] = this._bytesToSize(data[key]));
        Object.assign(formatted, data.characterEncoding || {});
        const titles = [this.labels.generalInformation, this.labels.softwareInformation, this.labels.dbPool, this.labels.memInfo, this.labels.characterEncoding];
        const icons = ["ti-info-square", "ti-server", "ti-database", "ti-cpu-2", "ti-language"];
        const columns = [document.createElement("div"), document.createElement("div")];
        columns.forEach(column => column.className = "col-md-6");

        TABLE_KEYS.forEach((keys, index) => {
            const wrapper = document.createElement("div");
            if (index === 2 || index === 3) wrapper.className = "server-monitoring-table-numeric";
            const heading = document.createElement("h6");
            heading.className = "server-monitoring-table-header";
            heading.innerHTML = `<i class="ti ${icons[index]}"></i> `;
            heading.append(document.createTextNode(titles[index] || ""));
            const table = document.createElement("table");
            table.className = "monitoring-table";
            const values = [...keys.map(key => [key, formatted[key]])];
            if (index === 3 && Array.isArray(data.sessionsList)) data.sessionsList.forEach(item => values.push([`- ${item.label}`, item.value]));
            values.forEach(([key, value], rowIndex) => {
                if (value == null) return;
                const row = table.insertRow();
                row.className = "table-data";
                row.dataset.index = rowIndex;
                const name = row.insertCell();
                name.style.whiteSpace = "nowrap";
                name.textContent = key.startsWith("-") ? key : (this.labels[key] || key);
                const content = row.insertCell();
                const element = document.createElement(STRONG_KEYS.has(key) ? "strong" : "span");
                element.textContent = value;
                content.appendChild(element);
            });
            wrapper.append(heading, table);
            columns[index % columns.length].appendChild(wrapper);
        });
        container.replaceChildren(...columns);
    }

    _msToDays(ms = 0) {
        const days = Math.floor(ms / 86400000);
        const hours = Math.floor((ms % 86400000) / 3600000);
        const minutes = Math.floor((ms % 3600000) / 60000);
        const seconds = Math.floor((ms % 60000) / 1000);
        return (this.labels.uptime || "{1} {2} {3}").replace("{1}", days).replace("{2}", hours).replace("{3}", minutes) + " " + (this.labels.uptimeSeconds || "{1}").replace("{1}", seconds);
    }

    _bytesToSize(bytes) {
        return !bytes ? "0 MB" : `${(bytes / 1024 / 1024).toFixed(2)} MB`;
    }
}

if (!customElements.get("webjet-server-monitoring")) customElements.define("webjet-server-monitoring", WebjetServerMonitoringElement);
