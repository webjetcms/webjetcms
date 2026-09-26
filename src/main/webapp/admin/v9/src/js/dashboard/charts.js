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
    if (primary) chart.get('colors')?.set('colors', [primary]);
    chart.series.each((series, index) => {
        const seriesColor = index === 1 ? comparison : primary;
        if (seriesColor) series.setAll({ stroke: seriesColor, fill: seriesColor });
        series.strokes?.template.setAll({ strokeWidth: index === 1 ? 1.5 : 2.5 });
        series.fills?.template.setAll({ visible: index === 0, fillOpacity: 0.08 });
        series.columns?.template.setAll({ height: 12, cornerRadiusTL: 4, cornerRadiusBL: 4, cornerRadiusTR: 4, cornerRadiusBR: 4 });
        series.setAll({ interpolationDuration: 0, stateAnimationDuration: 0 });
        series.get('tooltip')?.set('animationDuration', 0);
        series.appear(0, 0);
    });
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
