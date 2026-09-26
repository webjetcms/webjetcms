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
function compactChart(form) {
    const chart = form.chart;
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
    [...chart.xAxes.values, ...chart.yAxes.values].forEach(axis => axis.setAll({
        zoomable: false, interpolationDuration: 0, stateAnimationDuration: 0
    }));
    chart.series.each(series => {
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
        compactChart(form);
        customize?.(form);
        return cleanup;
    } catch (error) {
        cleanup();
        throw error;
    }
}
