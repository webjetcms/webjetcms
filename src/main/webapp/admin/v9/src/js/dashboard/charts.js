import { node } from './widget-utils';

let dashboardChartSequence = 0;

/** Gives each render its own root, including concurrent renders of the same widget. */
export function chartHost(container, label, bars = false) {
    const host = node('div', `md-dashboard-widget__chart${bars ? ' md-dashboard-widget__chart--bars' : ''}`);
    host.id = `dashboard-chart-${++dashboardChartSequence}`;
    host.setAttribute('role', 'img');
    host.setAttribute('aria-label', label);
    container.append(host);
    return host;
}

/** Keeps preview charts compact; the module link provides the full interactive report. */
function compactChart(form, host) {
    const chart = form.chart;
    const traffic = host.classList.contains('md-dashboard-widget__chart--traffic');
    const styles = window.getComputedStyle(host);
    const color = (name, fallback) => {
        const value = styles.getPropertyValue(`--wj-dashboard-chart-${name}`).trim() || styles.getPropertyValue(fallback).trim();
        // Sass mix() can emit fractional RGB channels; AmCharts only parses integer RGB strings.
        const rgb = value.match(/^rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)/i);
        return value ? window.am5.color(rgb ? `rgb(${rgb.slice(1).map(channel => Math.round(Number(channel))).join(',')})` : value) : undefined;
    };
    const primary = color('primary', '--wj-primary');
    const comparison = color('comparison', '--wj-gray-text');
    const grid = color('grid', '--wj-nice-gray-100');
    const label = color('label', '--wj-gray-text');
    const surface = color('surface', '--wj-dashboard-mint');
    chart.root.setThemes([window.WebjetTheme.new(chart.root)]);
    chart.setAll({ height: window.am5.percent(100), paddingTop: 5, paddingBottom: 0, paddingLeft: 0, paddingRight: 5,
        interpolationDuration: 0, stateAnimationDuration: 0 });
    for (const key of ['scrollbarX', 'scrollbarY']) {
        const scrollbar = chart.get(key);
        chart.set(key, undefined);
        scrollbar?.dispose();
    }
    chart.get('cursor')?.set('behavior', 'none');
    chart.zoomOutButton?.set('forceHidden', true);
    [...chart.children.values].filter(child => child.get('verticalScrollbar')).forEach(child => child.dispose());
    [...chart.xAxes.values, ...chart.yAxes.values].forEach(axis => {
        axis.setAll({ zoomable: false, interpolationDuration: 0, stateAnimationDuration: 0 });
        const renderer = axis.get('renderer');
        renderer.labels.template.setAll({ fontSize: 11, ...(label ? { fill: label } : {}) });
        renderer.grid?.template.setAll({ strokeOpacity: 0.45, ...(grid ? { stroke: grid } : {}) });
    });
    if (traffic) {
        const xAxis = chart.xAxes.getIndex(0);
        const xRenderer = xAxis.get('renderer');
        const yAxis = chart.yAxes.getIndex(0);
        const yRenderer = yAxis.get('renderer');
        xAxis.setAll({
            startLocation: 0.5, endLocation: 0.5, markUnitChange: false,
            dateFormats: { day: 'd. M.', week: 'd. M.', month: 'd. M.', year: 'yyyy' },
            gridIntervals: [{ timeUnit: 'day', count: 1 }, { timeUnit: 'day', count: 2 },
                { timeUnit: 'day', count: 7 }, { timeUnit: 'day', count: 14 }, { timeUnit: 'month', count: 1 }]
        });
        xRenderer.set('minGridDistance', 90);
        xRenderer.grid.template.set('forceHidden', true);
        xRenderer.labels.template.setAll({ paddingTop: 10, paddingBottom: 0, minPosition: 0, maxPosition: 1 });
        // Multi-day labels otherwise sit at midnight, before the first data point at the cell centre.
        xRenderer.labels.template.adapters.add('multiLocation', location => {
            const interval = xAxis.getPrivate('gridInterval');
            return interval?.timeUnit === 'day' ? 0.5 / interval.count : location;
        });
        const points = chart.series.getIndex(0).data.values;
        xRenderer.labels.template.adapters.add('centerX', (center, axisLabel) => {
            const value = axisLabel.dataItem?.get('value');
            if (value === points[0]?.[form.xAxeName]) return 0;
            if (value === points[points.length - 1]?.[form.xAxeName]) return window.am5.percent(100);
            return center;
        });
        yAxis.setAll({ min: 0, maxPrecision: 0, numberFormat: '#.#a' });
        yRenderer.set('minGridDistance', 65);
        yRenderer.labels.template.setAll({ paddingRight: 8 });
        yRenderer.grid.template.set('strokeOpacity', 0.3);
        chart.get('cursor')?.lineY.set('visible', false);
    }
    if (primary) chart.get('colors')?.set('colors', [primary]);
    chart.series.each((series, index) => {
        const seriesColor = index === 1 ? comparison : primary;
        if (seriesColor) series.setAll({ stroke: seriesColor, fill: seriesColor });
        series.strokes?.template.setAll({ strokeWidth: index === 1 ? 1.5 : traffic ? 2 : 2.5 });
        series.fills?.template.setAll({ visible: index === 0, fillOpacity: 0.08 });
        series.columns?.template.setAll({ height: 12, cornerRadiusTL: 4, cornerRadiusBL: 4, cornerRadiusTR: 4, cornerRadiusBR: 4 });
        series.setAll({ interpolationDuration: 0, stateAnimationDuration: 0 });
        series.get('tooltip')?.set('animationDuration', 0);
        if (traffic && index === 0) {
            series.set('maskBullets', false);
            series.bullets.push((root, line, dataItem) => {
                if (dataItem !== line.dataItems[line.dataItems.length - 1]) return;
                return window.am5.Bullet.new(root, { sprite: window.am5.Circle.new(root, {
                    radius: 4, fill: line.get('stroke'), stroke: surface, strokeWidth: 2
                }) });
            });
        }
        series.appear(0, 0);
    });
    if (host.classList.contains('md-dashboard-widget__chart--referrers')) {
        const xAxis = chart.xAxes.getIndex(0), yAxis = chart.yAxes.getIndex(0);
        xAxis.setAll({ min: 0, max: 100, strictMinMax: true, extraMax: 0, height: 0 });
        yAxis.set('width', 0);
        for (const axis of [xAxis, yAxis]) {
            const renderer = axis.get('renderer');
            renderer.labels.template.set('forceHidden', true);
            renderer.grid.template.set('forceHidden', true);
        }
        chart.setAll({ paddingTop: 0, paddingRight: 0 });
        chart.get('cursor')?.set('visible', false);
        const series = chart.series.getIndex(0);
        series.setAll({ clustered: false, maskBullets: false });
        series.columns.template.setAll({ height: 5, dy: 10 });
        series.columns.template.adapters.remove('fill');
        series.columns.template.adapters.remove('stroke');
        series.columns.template.setAll({ fill: primary, strokeOpacity: 0 });
        const track = chart.series.unshift(window.am5xy.ColumnSeries.new(chart.root, {
            xAxis, yAxis, valueXField: 'track', categoryYField: form.yAxeName, clustered: false, maskBullets: false
        }));
        track.columns.template.setAll({ height: 5, dy: 10, fill: grid, strokeOpacity: 0,
            cornerRadiusTL: 3, cornerRadiusBL: 3, cornerRadiusTR: 3, cornerRadiusBR: 3 });
        track.data.setAll(form.chartData.map(item => ({ ...item, track: 100 })));
        for (const right of [false, true]) track.bullets.push(root => {
            const caption = window.am5.Label.new(root, {
                text: right ? '{share}' : `{${form.yAxeName}}`, populateText: true, ignoreFormatting: true,
                centerX: window.am5.percent(right ? 100 : 0), centerY: window.am5.percent(100), dy: 2,
                paddingLeft: 0, paddingRight: 0, paddingTop: 0, paddingBottom: 4,
                fontSize: 11, fill: label, oversizedBehavior: 'truncate', textAlign: right ? 'right' : 'left'
            });
            if (!right) caption.adapters.add('maxWidth', () => Math.max(0, chart.plotContainer.width() - 54));
            return window.am5.Bullet.new(root, { locationX: right ? 1 : 0, sprite: caption });
        });
    }
    chart.appear(0, 0);
}

/** Loads the shared chart bundle and disposes roots on abort, replacement, failure, or stale completion. */
export async function mountChart(host, signal, createForm, customize) {
    await window.initAmcharts();
    if (signal.aborted || !host.isConnected) return;
    const tools = window.ChartTools;
    const form = createForm(tools, host.id);
    let disposed = false;
    const cleanup = () => {
        if (disposed) return;
        disposed = true;
        signal.removeEventListener('abort', cleanup);
        tools.destroyChart(form);
    };
    signal.addEventListener('abort', cleanup, { once: true });
    try {
        await tools.createAmchart(form);
        if (disposed) { tools.destroyChart(form); return cleanup; }
        if (signal.aborted || !host.isConnected) { cleanup(); return cleanup; }
        if (host.previousElementSibling?.classList.contains('amchart-header')) host.previousElementSibling.remove();
        compactChart(form, host);
        customize?.(form);
        return cleanup;
    } catch (error) {
        cleanup();
        throw error;
    }
}
