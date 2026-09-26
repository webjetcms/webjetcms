import { registerWidget } from './registry';
import { node, text, number, date, link, icon, field, table, empty, footer, fetchData } from './widget-utils';
import { chartHost, mountChart } from './charts';

const moduleLinks = {
    approvals: '/admin/v9/webpages/web-pages-list/?show=toapprove', publishing: '/admin/v9/webpages/web-pages-list/',
    forms: '/apps/form/admin/', traffic: '/apps/stat/admin/', 'top-pages': '/apps/stat/admin/top/',
    'search-terms': '/apps/stat/admin/search-engines/', referrers: '/apps/stat/admin/referer/',
    errors: '/apps/stat/admin/error/', newsletter: '/apps/dmail/admin/'
};
const metricKey = metric => ({ views: 'visits', sessions: 'sessionsMetric', uniqueUsers: 'uniqueUsers' })[metric] || 'sessionsMetric';

/** Labels the actual returned interval, including whole-week error aggregates. */
function period(container, data, context) {
    if (data.from == null || data.to == null) return;
    container.append(node('p', 'md-dashboard-widget__period small text-muted mb-2', `${date(data.from, false)} – ${date(data.to, false)}`));
    if (data.granularity === 'week') container.append(node('p', 'small text-muted mb-2', text(context, 'weeklyRequests')));
}

/** Relative changes have an explicit unavailable state when the baseline is zero. */
export function change(current, previous) {
    if (previous == null || previous === 0) return null;
    const delta = (Number(current) - previous) / previous * 100;
    return `${delta > 0 ? '+' : ''}${Math.round(delta)} %`;
}

function summary(container, data, context, href, label) {
    const group = node('div', 'md-dashboard-widget__metric');
    const main = node('div', 'md-dashboard-widget__metric-main');
    const total = link(number(data.total), href, 'md-dashboard-widget__number');
    if (label) total.setAttribute('aria-label', `${label}: ${number(data.total)}`);
    main.append(total);
    if (label) main.append(node('span', 'md-dashboard-widget__metric-label small', label));
    group.append(main);
    if (data.previous != null) {
        const delta = change(data.total, data.previous);
        const direction = delta ? (Number(data.total) > Number(data.previous) ? 'positive' : Number(data.total) < Number(data.previous) ? 'negative' : 'neutral') : 'neutral';
        const comparison = node('span', `md-dashboard-widget__comparison md-dashboard-widget__comparison--${direction} small`, delta || text(context, 'noComparison'));
        comparison.title = `${text(context, 'previous')}: ${number(data.previous)}`;
        const comparisonGroup = node('div', 'md-dashboard-widget__metric-change');
        comparisonGroup.append(comparison, node('span', 'md-dashboard-widget__comparison-label small', text(context, 'previous')));
        group.append(comparisonGroup);
    }
    container.append(group);
}

function periodField(container, options, context, completed = true) {
    return field(container, text(context, 'period'), [7, 30, 90].map(days => [days, text(context, `${completed ? "days" : "formDays"}${days}`)]), options.days || 7);
}

/** Keeps an inaccessible saved selection visible instead of silently changing it. */
function selectionField(container, label, choices, value, context, allKey) {
    const values = [['', text(context, allKey)], ...choices.map(choice => [String(choice.id), choice.title])];
    if (value && !values.some(([id]) => id === String(value))) values.push([String(value), `${value} — ${text(context, 'unavailable')}`]);
    return field(container, label, values, value || '');
}

function statSettings({ container, options, context }, metric = false) {
    const days = periodField(container, options, context);
    const selectedMetric = metric ? field(container, text(context, 'metric'), ['sessions', 'views', 'uniqueUsers'].map(value => [value, text(context, metricKey(value))]), options.metric || 'sessions') : null;
    return { read: () => ({ options: { ...options, days: Number(days.value), ...(selectedMetric ? { metric: selectedMetric.value } : {}) } }) };
}

/** Keeps every chart value available to keyboard and screen-reader users as ordinary text. */
function chartTable(container, context, headers, rows) {
    const details = node('details', 'md-dashboard-widget__chart-data');
    details.append(node('summary', 'small', text(context, 'chartData')));
    table(details, headers, rows);
    container.append(details);
}

/** Compares equal-length periods while retaining their actual dates in tooltips and the text table. */
async function lineChart(container, data, context, signal) {
    const series = data.series || [];
    if (!series.length) { empty(container, context); return; }
    const previous = data.previousSeries || [];
    const metric = text(context, metricKey(data.metric));
    const host = chartHost(container, `${metric}: ${number(data.total)}; ${text(context, 'previous')}: ${number(data.previous)}`);
    const legend = node('div', 'md-dashboard-widget__chart-legend');
    legend.append(node('span', 'md-dashboard-widget__chart-key md-dashboard-widget__chart-key--current', metric));
    if (previous.length) {
        const comparison = node('span', 'md-dashboard-widget__chart-key md-dashboard-widget__chart-key--previous', text(context, 'previous'));
        comparison.title = `${text(context, 'previous')}: ${number(data.previous)}`;
        legend.append(comparison);
    }
    container.append(legend);
    chartTable(container, context, [metric, text(context, 'previous')], series.map((point, index) => [
        `${date(point.date, false)}: ${number(point.value)}`,
        previous[index] ? `${date(previous[index].date, false)}: ${number(previous[index].value)}` : '—'
    ]));
    return mountChart(host, signal, (tools, chartDivId) => {
        const chartData = new Map([[metric, series.map(point => ({ dayDate: point.date, value: point.value, actualDate: date(point.date, false) }))]]);
        if (previous.length) chartData.set(text(context, 'previous'), previous.slice(0, series.length).map((point, index) => ({
            dayDate: series[index].date, value: point.value, actualDate: date(point.date, false)
        })));
        return new tools.LineChartForm({ yAxeNames: [{ yAxeName: 'value' }], xAxeName: 'dayDate', chartTitle: '',
            chartDivId, chartData, dateType: tools.DateType.Days, hideEmpty: false, colorScheme: 'set3' });
    }, form => {
        form.chart.yAxes.getIndex(0).setAll({ min: 0, maxPrecision: 0 });
        form.chart.series.each((line, index) => {
            if (index === 1) line.strokes.template.set('strokeDasharray', [5, 4]);
            line.get('tooltip').set('labelText', '{name}\n{actualDate}: [bold]{valueY}[/]');
        });
    });
}

async function rankedList(container, data, context, type, detailed, signal) {
    const items = (data.items || []).slice(0, detailed ? 6 : 5);
    if (!items.length) { empty(container, context); return; }
    if (detailed && type === 'top-pages') {
        table(container, [text(context, 'page'), text(context, 'section'), text(context, 'count'), text(context, 'change')], items.map(item => [
            link(item.title, item.url), item.section || '', number(item.value), change(item.value, item.previous) || '—'
        ]));
    } else if (type === 'referrers') {
        const host = chartHost(container, `${text(context, 'source')}: ${number(data.total)}`, true);
        if (!detailed) host.classList.add('md-dashboard-widget__chart--compact');
        const chartData = items.map(item => ({ title: item.title, value: item.value,
            share: `${number(Math.round((data.total > 0 ? item.value / data.total * 100 : 0) * 10) / 10)} %` }));
        container.append(node('p', 'small text-muted mb-1', text(context, 'observedShare')));
        chartTable(container, context, [text(context, 'source'), text(context, 'count'), text(context, 'observedShare')], chartData.map(item => [item.title, number(item.value), item.share]));
        return mountChart(host, signal, (tools, chartDivId) => new tools.BarChartForm({
            yAxeName: 'title', xAxeName: 'value', chartTitle: '', chartDivId, chartData, horizontal: true, colorScheme: 'set3'
        }), form => {
            form.chart.xAxes.getIndex(0).set('maxPrecision', 0);
            form.chart.yAxes.getIndex(0).get('renderer').labels.template.setAll({ maxWidth: detailed ? 140 : 95, oversizedBehavior: 'truncate', ignoreFormatting: true });
            const tooltip = form.chart.series.getIndex(0).get('tooltip');
            tooltip.set('labelText', '{title}: {valueX} ({share})');
            tooltip.label.set('ignoreFormatting', true);
        });
    } else table(container, [text(context, type === 'search-terms' ? 'query' : type === 'referrers' ? 'source' : 'page'), text(context, 'count')], items.map(item => [link(item.title, item.url), number(item.value)]));
}

/** Polls only an active newsletter visible in the current browser tab. */
function pollNewsletter(data, container, signal, refresh) {
    if (!data.active) return;
    let visible = false;
    const observer = new IntersectionObserver(entries => { visible = entries.some(entry => entry.isIntersecting); });
    observer.observe(container);
    const timer = window.setInterval(() => { if (visible && document.visibilityState === 'visible' && !signal.aborted) refresh(); }, 30000);
    return () => { observer.disconnect(); window.clearInterval(timer); };
}

/** Keeps a meaningful authorized summary when a data card is collapsed. */
async function renderDataSummary({ container, instance, options, domainOptions, context, signal, refresh }) {
    const type = instance.type;
    const params = type === 'newsletter' ? { campaignId: domainOptions.campaignId }
        : type === 'forms' ? { days: options.days || 7, formName: domainOptions.formName }
        : type === 'approvals' ? {} : { days: type === 'publishing' ? 30 : options.days || 7, ...(type === 'traffic' ? { metric: options.metric || 'sessions' } : {}) };
    const data = await fetchData(type, params, signal);
    if (signal.aborted) return;
    if (type === 'newsletter') {
        const campaign = data.items[0];
        if (campaign) container.append(node('p', 'small mb-1', `${campaign.title} · ${text(context, campaign.status === 'sending' ? 'active' : campaign.status)} · ${number(campaign.sent)} / ${number(campaign.recipients)}`));
        else empty(container, context);
    } else {
        const label = type === 'traffic' ? text(context, metricKey(data.metric)) : type === 'forms' ? text(context, 'submissions') : text(context, 'count');
        container.append(node('p', 'small mb-1', `${label}: ${number(data.total)}`));
        if (type === 'forms') container.append(node('span', 'small', domainOptions.formName || text(context, 'allForms')));
        period(container, data, context);
    }
    const href = type === 'forms' && domainOptions.formName ? `${moduleLinks.forms}detail/?formName=${encodeURIComponent(domainOptions.formName)}` : moduleLinks[type];
    footer(container, context, href);
    if (type === 'newsletter') return pollNewsletter(data, container, signal, refresh);
}

/** Registers content, analytics, and newsletter widgets using authorized projections. */
export function registerDataWidgets() {
    registerWidget({
        type: 'approvals', titleKey: 'admin.dashboard.approvals.js', icon: 'ti-checkup-list', sizes: ['1x1', '3x3'], defaultSize: '3x3',
        isAvailable: () => window.WJ.hasPermission('menuWebpages'), renderCollapsed: renderDataSummary,
        async render({ container, instance, context, signal }) {
            const data = await fetchData('approvals', {}, signal); if (signal.aborted) return;
            summary(container, data, context, moduleLinks.approvals, text(context, 'pendingPages'));
            if (instance.size !== '1x1') {
                if (!data.items.length) empty(container, context);
                else table(container, [text(context, 'page'), text(context, 'requester'), text(context, 'waitingSince')], data.items.slice(0, 6).map(item => [link(item.title, item.url), item.section, date(item.date)]));
                footer(container, context, moduleLinks.approvals);
            }
        }
    });
    registerWidget({
        type: 'publishing', titleKey: 'admin.dashboard.publishing.js', icon: 'ti-calendar-event', sizes: ['2x3'],
        isAvailable: () => window.WJ.hasPermission('menuWebpages'), renderCollapsed: renderDataSummary,
        async render({ container, context, signal }) {
            const data = await fetchData('publishing', { days: 30 }, signal); if (signal.aborted) return;
            period(container, data, context);
            if (!data.items.length) empty(container, context);
            else {
                const list = node('ul', 'md-dashboard-widget__publishing list-unstyled');
                data.items.slice(0, 5).forEach(item => {
                    const row = node('li', `md-dashboard-widget__publication${item.kind === 'expire' ? ' md-dashboard-widget__publication--expire' : ''}`);
                    const locale = (window.userLng === 'cz' ? 'cs' : window.userLng) || 'sk';
                    const parsed = item.date == null ? null : new Date(item.date);
                    const validDate = parsed && !Number.isNaN(parsed.getTime());
                    if (validDate) {
                        const calendar = node('time', 'md-dashboard-widget__publication-calendar');
                        calendar.dateTime = parsed.toISOString();
                        calendar.setAttribute('aria-label', date(item.date));
                        calendar.append(node('small', '', parsed.toLocaleString(locale, { month: 'short' })), node('strong', '', parsed.toLocaleString(locale, { day: 'numeric' })));
                        row.append(calendar);
                    }
                    const content = node('div', 'md-dashboard-widget__publication-content');
                    const scheduledTime = validDate ? parsed.toLocaleString(locale, { hour: '2-digit', minute: '2-digit' }) : date(item.date);
                    content.append(link(item.title, item.url), node('span', 'md-dashboard-widget__publication-time small', `${text(context, item.kind === 'expire' ? 'expire' : 'publish')} · ${scheduledTime}`));
                    if (item.kind === 'publish') content.append(node('span', 'md-dashboard-widget__publication-status small', text(context, 'draft')));
                    row.append(content);
                    list.append(row);
                }); container.append(list);
            }
            footer(container, context, moduleLinks.publishing);
        }
    });
    registerWidget({
        type: 'forms', titleKey: 'admin.dashboard.forms.js', icon: 'ti-forms', sizes: ['1x1', '3x3'], defaultSize: '3x3', multiple: true,
        defaultOptions: { days: 7 }, defaultDomainOptions: { formName: '' }, isAvailable: () => window.WJ.hasPermission('cmp_form'), renderCollapsed: renderDataSummary,
        async configure({ container, options, domainOptions, context, signal }) {
            const data = await fetchData('forms', { days: options.days || 7 }, signal);
            const days = periodField(container, options, context, false);
            const form = selectionField(container, text(context, 'formName'), data.options || [], domainOptions.formName, context, 'allForms');
            return { read: () => ({ options: { days: Number(days.value) }, domainOptions: { formName: form.value } }) };
        },
        async render({ container, instance, options, domainOptions, context, signal }) {
            const data = await fetchData('forms', { days: options.days || 7, formName: domainOptions.formName }, signal); if (signal.aborted) return;
            const href = domainOptions.formName ? `${moduleLinks.forms}detail/?formName=${encodeURIComponent(domainOptions.formName)}` : moduleLinks.forms;
            summary(container, data, context, href, text(context, 'submissions'));
            if (domainOptions.formName) container.append(node('span', 'small text-muted', domainOptions.formName));
            period(container, data, context);
            if (instance.size !== '1x1') {
                if (!data.items.length) empty(container, context);
                else table(container, [text(context, 'formName'), text(context, 'date')], data.items.slice(0, 6).map(item => [link(item.title, item.url), date(item.date)]));
                footer(container, context, href);
            }
        }
    });
    const definitions = [
        ['traffic', 'ti-chart-line', ['1x1', '3x3']], ['top-pages', 'ti-chart-bar', ['2x3', '3x3']],
        ['search-terms', 'ti-search', ['2x3']], ['referrers', 'ti-route', ['2x3', '3x3']], ['errors', 'ti-error-404', ['1x1', '3x3']]
    ];
    definitions.forEach(([type, icon, sizes]) => registerWidget({
        type, titleKey: `admin.dashboard.${type}.js`, icon, sizes, defaultSize: sizes[sizes.length - 1], multiple: true,
        defaultOptions: { days: 7, ...(type === 'traffic' ? { metric: 'sessions' } : {}) },
        isAvailable: context => window.WJ.hasPermission('cmp_stat') && context.config.statMode !== 'none',
        configure: args => statSettings(args, type === 'traffic'), renderCollapsed: renderDataSummary,
        async render({ container, instance, options, context, signal }) {
            const data = await fetchData(type, { days: options.days || 7, ...(type === 'traffic' ? { metric: options.metric || 'sessions' } : {}) }, signal); if (signal.aborted) return;
            if (type === 'traffic' || type === 'errors') summary(container, data, context, moduleLinks[type], type === 'traffic' ? text(context, metricKey(data.metric)) : text(context, 'requests'));
            period(container, data, context);
            if (instance.size !== '1x1') {
                const cleanup = type === 'traffic' ? await lineChart(container, data, context, signal)
                    : await rankedList(container, data, context, type, instance.size === '3x3', signal);
                if (!signal.aborted) footer(container, context, moduleLinks[type]);
                return cleanup;
            }
        }
    }));
    registerWidget({
        type: 'newsletter', titleKey: 'admin.dashboard.newsletter.js', icon: 'ti-mail', sizes: ['2x2', '3x3'], multiple: true,
        defaultDomainOptions: { campaignId: '' }, isAvailable: () => window.WJ.hasPermission('menuEmail'), renderCollapsed: renderDataSummary,
        async configure({ container, domainOptions, context, signal }) {
            const data = await fetchData('newsletter', {}, signal);
            const campaign = selectionField(container, text(context, 'campaign'), data.options || [], domainOptions.campaignId, context, 'automatic');
            return { read: () => ({ domainOptions: { campaignId: campaign.value } }) };
        },
        async render({ container, instance, domainOptions, context, signal, refresh }) {
            const data = await fetchData('newsletter', { campaignId: domainOptions.campaignId }, signal); if (signal.aborted) return;
            if (!data.items.length) empty(container, context);
            else if (instance.size === '3x3') {
                table(container, [text(context, 'campaign'), text(context, 'status'), text(context, 'sent'), text(context, 'failed')], data.items.slice(0, 3).map(item => [
                    link(item.title, item.url), text(context, item.status === 'sending' ? 'active' : item.status), `${number(item.sent)} / ${number(item.recipients)}`, number(item.failed)
                ]));
            } else {
                const campaign = data.items[0];
                const state = ['sending', 'scheduled', 'completed', 'draft', 'paused'].includes(campaign.status) ? campaign.status : 'unknown';
                const status = node('p', `md-dashboard-widget__newsletter-status md-dashboard-widget__newsletter-status--${state} small`);
                status.append(icon(state === 'completed' ? 'ti-circle-check' : state === 'sending' ? 'ti-send' : state === 'paused' ? 'ti-player-pause' : 'ti-clock'), document.createTextNode(text(context, campaign.status === 'sending' ? 'active' : campaign.status)));
                container.append(status, link(campaign.title, campaign.url, 'md-dashboard-widget__newsletter-title'));
                const metric = node('div', 'md-dashboard-widget__newsletter-metric');
                const count = node('span');
                count.append(node('strong', '', number(campaign.sent)), document.createTextNode(` / ${number(campaign.recipients)}`));
                metric.append(count, node('span', 'md-dashboard-widget__newsletter-percent', campaign.recipients > 0 ? `${number(Math.round(campaign.sent / campaign.recipients * 100))} %` : '—'));
                const progress = node('progress', 'md-dashboard-widget__newsletter-progress w-100'); progress.max = Math.max(1, campaign.recipients); progress.value = campaign.sent;
                progress.setAttribute('aria-label', text(context, 'sent'));
                metric.setAttribute('aria-label', `${text(context, 'sent')}: ${number(campaign.sent)} / ${number(campaign.recipients)}`);
                container.append(metric, progress);
                const details = node('div', 'md-dashboard-widget__newsletter-details');
                for (const [key, label] of [['failed', 'failed'], ['opens', 'opened'], ['clicks', 'clicked']]) {
                    if (campaign[key] != null && (key === 'failed' || campaign.status === 'completed')) details.append(node('span', 'small d-block', `${text(context, label)}: ${number(campaign[key])}`));
                }
                container.append(details);
            }
            footer(container, context, moduleLinks.newsletter);
            return pollNewsletter(data, container, signal, refresh);
        }
    });
}
