import { registerWidget } from './registry';
import { node, text, number, date, link, icon, table, empty, fetchJson, pagePreview, containNativeScroll } from './widget-utils';
import { chartHost, mountChart } from './charts';
import { readMonitoringSnapshot, subscribeMonitoring } from './monitoring-live';

const moduleLinks = {
    'changed-pages': '/admin/v9/apps/audit-changed-webpages/',
    audit: '/admin/v9/apps/audit-search/',
    'server-memory': '/apps/server_monitoring/admin/',
    'server-cpu': '/apps/server_monitoring/admin/'
};

/**
 * Page or audit entry displayed in the activity list.
 * @typedef {import('./widget-utils').PagePreview & {type?: string, description?: string, userFullName: string, date: number, url: string}} ActivityItem
 */

/**
 * Reuses the audit page list with its ordering, authors and page previews.
 * @param {AbortSignal} signal - Cancels the module request when the render ends.
 * @returns {Promise<{items: ActivityItem[]}>} Changed-page records with editor links and author timestamps.
 */
async function fetchChangedPages(signal) {
    const data = await fetchJson('/admin/rest/web-pages/all?auditVersion=true&size=6&page=0&sort=dateCreated%2Cdesc', signal);
    return { items: data.content.map(page => ({ title: page.title, fullPath: page.fullPath, perexImage: page.perexImage,
        userFullName: page.authorName, date: page.dateCreated, url: `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(page.docId)}` })) };
}

/**
 * Maps audit rows and the module's localized event names into the activity preview.
 * @param {AbortSignal} signal - Cancels the module request when the render ends.
 * @returns {Promise<{items: ActivityItem[]}>} Audit records with localized types and links to their details.
 */
async function fetchAudit(signal) {
    const data = await fetchJson('/admin/rest/audit/log/all?size=6&page=0&sort=id%2Cdesc', signal);
    const types = new Map(data.options.logType.map(option => [String(option.value), option.label]));
    return { items: data.content.map(row => ({ type: types.get(String(row.logType)), description: row.description,
        userFullName: row.userFullName, date: row.createDate, url: `/admin/v9/apps/audit-search/?id=${encodeURIComponent(row.id)}` })) };
}

/**
 * Renders a bounded activity preview with the author, timestamp and complete linked description.
 * @param {HTMLElement} container - Parent to receive the activity list.
 * @param {ActivityItem[]} items - Ordered page or audit records.
 * @param {'changed-pages'|'audit'} type - Chooses page thumbnails or audit descriptions.
 * @param {string} size - A 3x2 footprint displays two entries; other sizes display up to four.
 */
function activityList(container, items, type, size) {
    const list = node('ul', 'md-dashboard-widget__activity list-unstyled');
    items.slice(0, size === '3x2' ? 2 : 4).forEach(item => {
        const row = node('li');
        const target = type === 'changed-pages' ? pagePreview(item, item.url) : link('', item.url, 'md-dashboard-widget__page-preview');
        if (type === 'audit') {
            const image = node('span', 'md-dashboard-widget__page-image');
            image.append(icon('ti-shield-search'));
            const content = node('span', 'md-dashboard-widget__page-content');
            content.append(node('span', 'md-dashboard-widget__page-title', item.type), node('span', 'md-dashboard-widget__page-section', item.description));
            target.append(image, content);
        }
        const detail = node('span', 'md-dashboard-widget__activity-detail');
        detail.append(node('span', '', item.userFullName), node('span', '', date(item.date)));
        row.append(target, detail);
        list.append(row);
    });
    container.append(list);
}

/**
 * Builds a mail action without allowing the address to add URI headers or additional recipients.
 * @param {{email?: string, fullName: string}} user - Administrator address and label.
 * @param {import('./registry').WidgetContext} context - Supplies the accessible action label.
 * @returns {HTMLAnchorElement|null} A detached mail action, or null when the address fails validation.
 */
export function adminMail(user, context) {
    const email = typeof user.email === 'string' ? user.email.trim() : '';
    if (!email || /[\s<>,;?&#%\\]/.test(email) || !/^[^@]+@[^@]+$/.test(email)) return null;
    const action = node('a', 'btn btn-sm md-dashboard-widget__admin-mail');
    action.href = `mailto:${encodeURIComponent(email).replace('%40', '@')}`;
    action.title = text(context, 'sendAdminEmail', user.fullName);
    action.setAttribute('aria-label', action.title);
    action.append(icon('ti-mail'));
    return action;
}

/** Reads a fresh, server-authorized administrator summary for a visible widget or active dialog tab. */
export function fetchLoggedAdministrators(signal) {
    return fetchJson('/admin/rest/sessions/administrators', signal);
}

/** Loads the administrator list when the shared widget lifecycle starts or refreshes this render. */
export async function renderLoggedAdmins({ container, context, signal }) {
    const admins = await fetchLoggedAdministrators(signal);
    if (signal.aborted) return;
    if (!admins.length) { empty(container, context); return; }
    const list = node('ul', 'md-dashboard-widget__admins list-unstyled');
    list.tabIndex = 0;
    list.setAttribute('aria-label', text(context, 'logged-admins'));
    containNativeScroll(list, signal);
    admins.forEach(user => {
        const row = node('li');
        row.append(icon('ti-user'), node('span', 'md-dashboard-widget__admin-name', user.fullName));
        const mail = adminMail(user, context);
        if (mail) row.append(mail);
        list.append(row);
    });
    container.append(list, node('p', 'md-dashboard-widget__footnote small', text(context, 'adminsCount', number(admins.length))));
}

function monitoringMetrics(type) {
    return type === 'server-memory' ? [['used', 'memoryUsed'], ['free', 'memoryFree'], ['total', 'memoryTotal']]
        : [['process', 'cpuProcess'], ['system', 'cpuSystem']];
}

/**
 * A chart sample containing either memory values in MiB or CPU percentages.
 * @typedef {Object} MonitoringPoint
 * @property {number} date - Server sampling time in epoch milliseconds.
 * @property {number|null} [used] - Used memory, or null when unavailable.
 * @property {number|null} [free] - Free memory, or null when unavailable.
 * @property {number|null} [total] - Allocated memory, or null when unavailable.
 * @property {number|null} [process] - Process CPU usage, or null when unavailable.
 * @property {number|null} [system] - System CPU usage, or null when unavailable.
 */

/**
 * Maps the live endpoint to chart units without turning unavailable readings into zero.
 * @param {import('./monitoring-live').MonitoringSnapshot} snapshot - Current server metrics.
 * @param {'server-memory'|'server-cpu'} type - Selects memory or CPU measurements.
 * @returns {MonitoringPoint} A sample with negative, nonnumeric or nonfinite readings replaced by null.
 */
function monitoringPoint(snapshot, type) {
    const measurement = (value, divisor = 1) => typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value / divisor : null;
    return type === 'server-memory' ? { date: snapshot.serverActualTime,
        used: measurement(snapshot.memUsed, 1024 * 1024), free: measurement(snapshot.memFree, 1024 * 1024), total: measurement(snapshot.memTotal, 1024 * 1024) }
        : { date: snapshot.serverActualTime, process: measurement(snapshot.cpuUsageProcess), system: measurement(snapshot.cpuUsage) };
}

function monitoringDate(value) {
    return new Date(value).toLocaleString((window.userLng === 'cz' ? 'cs' : window.userLng) || 'sk', {
        day: 'numeric', month: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
}

/**
 * Keeps the current values and their precise sampling time in place as live samples arrive.
 * @param {HTMLElement} container - Parent to receive metric values and the sampling-time label.
 * @param {MonitoringPoint} latest - Initial values to display.
 * @param {'server-memory'|'server-cpu'} type - Selects metric names and units.
 * @param {import('./registry').WidgetContext} context - Supplies metric and time labels.
 * @returns {function(MonitoringPoint): void} Updates the existing value and time elements with a new sample.
 */
function monitoringSummary(container, latest, type, context) {
    const unit = type === 'server-memory' ? 'MB' : '%';
    const values = node('dl', 'md-dashboard-widget__monitoring-values');
    const readings = new Map();
    for (const [key, label] of monitoringMetrics(type)) {
        const value = node('div');
        const reading = node('dd');
        readings.set(key, reading);
        value.append(node('dt', '', text(context, label)), reading);
        values.append(value);
    }
    const time = node('p', 'md-dashboard-widget__monitoring-time small');
    const update = point => {
        readings.forEach((reading, key) => { reading.textContent = `${number(point[key])} ${unit}`; });
        time.textContent = text(context, 'measuredAt', monitoringDate(point.date));
    };
    update(latest);
    container.append(values, time);
    return update;
}

/**
 * Uses the shared chart lifecycle and exposes every real monitoring sample in an accessible table.
 * @param {HTMLElement} container - Parent to receive the chart, legend and visually hidden table.
 * @param {MonitoringPoint[]} points - Initial samples in chronological order.
 * @param {'server-memory'|'server-cpu'} type - Selects metric names, units and axis limits.
 * @param {import('./registry').WidgetContext} context - Supplies translated labels.
 * @param {AbortSignal} signal - Chart render lifetime.
 * @returns {Promise<{destroy: (function(): void)|undefined, update: function(MonitoringPoint[]): void}>} Cleanup and a sample-replacement callback; updates are ignored after abort or skipped chart creation.
 */
async function monitoringChart(container, points, type, context, signal) {
    const metrics = monitoringMetrics(type);
    const unit = type === 'server-memory' ? 'MB' : '%';
    const host = chartHost(container, `${text(context, type)} (${unit})`);
    host.classList.add('md-dashboard-widget__chart--monitoring');
    const legend = node('div', 'md-dashboard-widget__chart-legend');
    metrics.forEach(([, label], index) => legend.append(node('span', `md-dashboard-widget__chart-key md-dashboard-widget__chart-key--monitoring-${index}`, text(context, label))));
    const dataRegion = node('div', 'md-dashboard-widget__chart-data visually-hidden');
    const updateTable = samples => {
        dataRegion.replaceChildren();
        table(dataRegion, [text(context, 'date'), ...metrics.map(([, label]) => `${text(context, label)} (${unit})`)], samples.map(point => [monitoringDate(point.date), ...metrics.map(([key]) => number(point[key]))]))
            .prepend(node('caption', '', text(context, 'chartData')));
    };
    const chartData = samples => new Map(metrics.map(([key, label]) => [text(context, label), samples.map(point => ({ date: point.date, value: point[key] }))]));
    updateTable(points);
    container.append(legend, dataRegion);
    let liveForm;
    const destroy = await mountChart(host, signal, (tools, chartDivId) => new tools.LineChartForm({
        yAxeNames: [{ yAxeName: 'value' }], xAxeName: 'date', chartTitle: '', chartDivId,
        chartData: chartData(points),
        dateType: tools.DateType.Seconds, hideEmpty: false, colorScheme: 'set3'
    }), form => {
        liveForm = form;
        form.chart.yAxes.getIndex(0).setAll({ min: 0, ...(type === 'server-cpu' ? { max: 100, strictMinMax: true } : {}) });
        form.chart.xAxes.getIndex(0).set('baseInterval', { timeUnit: 'second', count: 5 });
        form.chart.xAxes.getIndex(0).get('renderer').set('minGridDistance', 80);
        form.chart.series.each((series, index) => {
            if (index === 2) {
                const color = window.am5.color(window.getComputedStyle(host).getPropertyValue('--wj-dashboard-chart-total').trim());
                series.setAll({ stroke: color, fill: color });
            }
            if (index) series.strokes.template.set('strokeDasharray', index === 1 ? [5, 4] : [2, 3]);
            series.fills.template.set('visible', false);
            series.get('tooltip').setAll({ labelText: `{name}: [bold]{valueY} ${unit}[/]`, labelAriaLabel: `{name}: {valueY} ${unit}` });
            // The equivalent table supplies accessible values without AmCharts' duplicate unpopulated tooltip nodes.
            series.get('tooltip').label.setAll({ ariaHidden: true, role: 'presentation' });
            series.set('maskBullets', false);
            series.bullets.push((root, line, item) => {
                if (item !== line.dataItems[line.dataItems.length - 1]) return;
                return window.am5.Bullet.new(root, { sprite: window.am5.Circle.new(root, { radius: 4, fill: series.get('stroke') }) });
            });
        });
    });
    return { destroy, update(samples) {
        if (signal.aborted || !liveForm) return;
        liveForm.chartData = chartData(samples);
        const data = [...liveForm.chartData.values()];
        liveForm.chart.series.each((series, index) => series.data.setAll(data[index]));
        updateTable(samples);
    } };
}

/**
 * Subscribes monitoring charts and their summaries to live updates and stale-data feedback.
 * @param {HTMLElement} container - Observed card content receiving a status message.
 * @param {'server-memory'|'server-cpu'} type - Selects the measurements passed to update.
 * @param {import('./registry').WidgetContext} context - Supplies the stale-data message.
 * @param {AbortSignal} signal - Removes the subscription when the render ends.
 * @param {function(MonitoringPoint): void} update - Updates the chart and summary after a successful poll; its return value is ignored.
 * @returns {function(): void} Unsubscribes the card from shared monitoring polling.
 */
function liveMonitoring(container, type, context, signal, update) {
    const status = node('p', 'small text-danger mb-0');
    status.setAttribute('role', 'status');
    status.hidden = true;
    container.append(status);
    return subscribeMonitoring(container, signal, snapshot => {
        update(monitoringPoint(snapshot, type));
        status.hidden = true;
        status.textContent = '';
    }, () => {
        status.textContent = text(context, 'monitoringStale');
        status.hidden = false;
    });
}

/** Registers the activity, online-administrator and monitoring cards that replace the legacy section. */
export function registerSystemWidgets() {
    for (const [type, permission, widgetIcon] of [['changed-pages', 'menuWebpages', 'ti-pencil'], ['audit', 'cmp_adminlog', 'ti-shield-search']]) registerWidget({
        type, titleKey: `admin.dashboard.${type}.js`, descriptionKey: `admin.dashboard.${type}.description.js`, category: type === 'changed-pages' ? 'content' : 'system', icon: widgetIcon,
        multiple: true, sizes: ['3x2', '3x3'], defaultSize: '3x3', headerLink: { href: moduleLinks[type], labelKey: 'admin.dashboard.allShort.js' },
        isAvailable: () => window.WJ.hasPermission(permission) && window.WJ.hasPermission('cmp_adminlog'),
        async render({ container, instance, context, signal }) {
            const data = type === 'changed-pages' ? await fetchChangedPages(signal) : await fetchAudit(signal); if (signal.aborted) return;
            if (!data.items.length) empty(container, context);
            else activityList(container, data.items, type, instance.size);
        }
    });
    registerWidget({
        type: 'logged-admins', titleKey: 'admin.dashboard.logged-admins.js', descriptionKey: 'admin.dashboard.logged-admins.description.js', icon: 'ti-users',
        multiple: true, sizes: ['2x2', '2x3'], defaultSize: '2x2', isAvailable: () => window.WJ.hasPermission('welcomeShowLoggedAdmins'),
        render: renderLoggedAdmins
    });
    for (const type of ['server-memory', 'server-cpu']) registerWidget({
        type, titleKey: `admin.dashboard.${type}.js`, descriptionKey: `admin.dashboard.${type}.description.js`, icon: type === 'server-memory' ? 'ti-server' : 'ti-cpu',
        multiple: true, sizes: ['3x2', '3x3'], defaultSize: '3x3', headerLink: { href: moduleLinks[type] },
        isAvailable: () => window.WJ.hasPermission('cmp_server_monitoring'),
        async render({ container, context, signal }) {
            const snapshot = await readMonitoringSnapshot(signal); if (signal.aborted) return;
            const points = [monitoringPoint(snapshot, type)];
            const updateSummary = monitoringSummary(container, points[0], type, context);
            const chart = await monitoringChart(container, points, type, context, signal);
            if (signal.aborted) return chart.destroy;
            const unsubscribe = liveMonitoring(container, type, context, signal, point => {
                if (point.date === points[points.length - 1].date) points[points.length - 1] = point;
                else points.push(point);
                if (points.length > 100) points.shift();
                updateSummary(point);
                chart.update(points);
            });
            return () => { unsubscribe(); chart.destroy?.(); };
        }
    });
}
