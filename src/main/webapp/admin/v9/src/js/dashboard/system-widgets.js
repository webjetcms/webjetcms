import { registerWidget } from './registry';
import { node, text, number, date, link, icon, table, empty, fetchData, pagePreview, containNativeScroll } from './widget-utils';
import { chartHost, mountChart } from './charts';
import { readMonitoringSnapshot, subscribeMonitoring } from './monitoring-live';

const moduleLinks = {
    'changed-pages': '/admin/v9/webpages/web-pages-list/',
    audit: '/admin/v9/apps/audit-search/',
    'server-memory': '/apps/server_monitoring/admin/',
    'server-cpu': '/apps/server_monitoring/admin/'
};

/** Renders a bounded activity preview with the author, timestamp and complete linked description. */
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

/** Builds a mail action without allowing the address to add URI headers or additional recipients. */
function adminMail(user, context) {
    const email = typeof user.email === 'string' ? user.email.trim() : '';
    if (!email || /[\s<>,;?&#%\\]/.test(email) || !/^[^@]+@[^@]+$/.test(email)) return null;
    const action = node('a', 'btn btn-sm md-dashboard-widget__admin-mail');
    action.href = `mailto:${encodeURIComponent(email).replace('%40', '@')}`;
    action.title = text(context, 'sendAdminEmail', user.fullName);
    action.setAttribute('aria-label', action.title);
    action.append(icon('ti-mail'));
    return action;
}

function monitoringMetrics(type) {
    return type === 'server-memory' ? [['used', 'memoryUsed'], ['free', 'memoryFree'], ['total', 'memoryTotal']]
        : [['process', 'cpuProcess'], ['system', 'cpuSystem']];
}

/** Maps the live endpoint to chart units without turning unavailable readings into zero. */
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

/** Keeps the current values and their precise sampling time in place as live samples arrive. */
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

/** Uses the shared chart lifecycle and exposes every real monitoring sample in an accessible table. */
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

/** Shares live updates and stale-data feedback between the chart and its collapsed numeric preview. */
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
        type, titleKey: `admin.dashboard.${type}.js`, descriptionKey: `admin.dashboard.${type}.description.js`, icon: widgetIcon,
        sizes: ['3x2', '3x3'], defaultSize: '3x3', headerLink: { href: moduleLinks[type], labelKey: 'admin.dashboard.allShort.js' },
        isAvailable: () => window.WJ.hasPermission(permission),
        async render({ container, instance, context, signal }) {
            const data = await fetchData(type, {}, signal); if (signal.aborted) return;
            if (!data.items.length) empty(container, context);
            else activityList(container, data.items, type, instance.size);
        },
        async renderCollapsed({ container, context, signal }) {
            const data = await fetchData(type, {}, signal); if (signal.aborted) return;
            const latest = data.items[0];
            if (latest) container.append(link(type === 'audit' ? latest.type : latest.title, latest.url), node('span', 'small text-muted', date(latest.date)));
            else empty(container, context);
        }
    });
    registerWidget({
        type: 'logged-admins', titleKey: 'admin.dashboard.logged-admins.js', descriptionKey: 'admin.dashboard.logged-admins.description.js', icon: 'ti-users',
        sizes: ['2x2', '2x3'], defaultSize: '2x2', isAvailable: () => window.WJ.hasPermission('welcomeShowLoggedAdmins'),
        async render({ container, context, signal }) {
            const data = await fetchData('logged-admins', {}, signal); if (signal.aborted) return;
            if (!data.items.length) { empty(container, context); return; }
            const list = node('ul', 'md-dashboard-widget__admins list-unstyled');
            list.tabIndex = 0;
            list.setAttribute('aria-label', text(context, 'logged-admins'));
            containNativeScroll(list, signal);
            data.items.forEach(user => {
                const row = node('li');
                row.append(icon('ti-user'), node('span', 'md-dashboard-widget__admin-name', user.fullName));
                const mail = adminMail(user, context);
                if (mail) row.append(mail);
                list.append(row);
            });
            container.append(list, node('p', 'md-dashboard-widget__footnote small', text(context, 'adminsCount', number(data.total))));
        },
        async renderCollapsed({ container, context, signal }) {
            const data = await fetchData('logged-admins', {}, signal); if (signal.aborted) return;
            container.append(node('p', 'small mb-0', `${text(context, 'logged-admins')}: ${number(data.total)}`));
        }
    });
    for (const type of ['server-memory', 'server-cpu']) registerWidget({
        type, titleKey: `admin.dashboard.${type}.js`, descriptionKey: `admin.dashboard.${type}.description.js`, icon: type === 'server-memory' ? 'ti-server' : 'ti-cpu',
        sizes: ['3x2', '3x3'], defaultSize: '3x3', headerLink: { href: moduleLinks[type] },
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
        },
        async renderCollapsed({ container, context, signal }) {
            const snapshot = await readMonitoringSnapshot(signal); if (signal.aborted) return;
            const updateSummary = monitoringSummary(container, monitoringPoint(snapshot, type), type, context);
            return liveMonitoring(container, type, context, signal, updateSummary);
        }
    });
}
