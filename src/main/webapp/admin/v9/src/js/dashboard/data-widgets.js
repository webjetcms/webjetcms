import { registerWidget } from './registry';
import { node, text, number, date, link, field, table, empty, footer, fetchData } from './widget-utils';

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
    container.append(node('p', 'small text-muted mb-2', `${date(data.from, false)} – ${date(data.to, false)}`));
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
    group.append(link(number(data.total), href, 'md-dashboard-widget__number'));
    if (label) group.append(node('span', 'small', label));
    if (data.previous != null) {
        const delta = change(data.total, data.previous);
        const comparison = node('span', 'small text-muted', delta || text(context, 'noComparison'));
        comparison.title = `${text(context, 'previous')}: ${number(data.previous)}`;
        group.append(comparison);
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

function svgNode(tag, attributes, textContent) {
    const result = document.createElementNS('http://www.w3.org/2000/svg', tag);
    Object.entries(attributes).forEach(([key, value]) => result.setAttribute(key, value));
    if (textContent != null) result.textContent = textContent;
    return result;
}

/** A responsive SVG comparison with textual totals and point tooltips. */
function lineChart(container, data, context) {
    const series = data.series || [];
    if (!series.length) { empty(container, context); return; }
    const previous = data.previousSeries || [];
    const maximum = Math.max(1, ...series.map(point => point.value), ...previous.map(point => point.value));
    const svg = svgNode('svg', { viewBox: '0 0 480 155', role: 'img', class: 'md-dashboard-widget__chart' });
    const title = `${text(context, metricKey(data.metric))}: ${number(data.total)}; ${text(context, 'previous')}: ${number(data.previous)}`;
    svg.append(svgNode('title', {}, title));
    svg.setAttribute('aria-label', title);
    svg.append(svgNode('line', { x1: 35, y1: 130, x2: 475, y2: 130, stroke: 'currentColor', 'stroke-opacity': '.25' }));
    svg.append(svgNode('text', { x: 0, y: 15, 'font-size': 12 }, number(maximum)), svgNode('text', { x: 20, y: 132, 'font-size': 12 }, '0'));
    [previous, series].forEach((points, index) => {
        const coordinates = points.map((point, position) => [35 + position / Math.max(1, points.length - 1) * 440, 130 - point.value / maximum * 115]);
        svg.append(svgNode('polyline', { points: coordinates.map(point => point.join(',')).join(' '), fill: 'none', stroke: index ? 'var(--wj-primary)' : 'var(--wj-gray-text)', 'stroke-width': 2, ...(index ? {} : { 'stroke-dasharray': '5 4' }) }));
        coordinates.forEach(([x, y], position) => {
            const point = svgNode('circle', { cx: x, cy: y, r: points.length > 35 ? 1.5 : 3, fill: index ? 'var(--wj-primary)' : 'var(--wj-gray-text)' });
            point.append(svgNode('title', {}, `${date(points[position].date, false)}: ${number(points[position].value)}`)); svg.append(point);
        });
    });
    container.append(svg, node('p', 'small text-muted mb-1', `${text(context, 'previous')}: ${number(data.previous)} · ${text(context, 'chartPrevious')}`));
}

function rankedList(container, data, context, type, detailed) {
    const items = (data.items || []).slice(0, detailed ? 6 : 5);
    if (!items.length) { empty(container, context); return; }
    if (detailed && type === 'top-pages') {
        table(container, [text(context, 'page'), text(context, 'section'), text(context, 'count'), text(context, 'change')], items.map(item => [
            link(item.title, item.url), item.section || '', number(item.value), change(item.value, item.previous) || '—'
        ]));
    } else if (detailed && type === 'referrers') {
        const list = node('ul', 'list-unstyled md-dashboard-widget__bars');
        items.forEach(item => {
            const share = data.total > 0 ? item.value / data.total * 100 : 0;
            const row = node('li', 'mb-2');
            row.append(node('span', 'd-block small', `${item.title}: ${number(item.value)} (${share.toFixed(1)} %)`));
            const bar = node('div', 'md-dashboard-widget__bar');
            bar.style.width = `${Math.max(0, Math.min(100, share))}%`;
            row.append(bar); list.append(row);
        });
        container.append(list, node('p', 'small text-muted', text(context, 'observedShare')));
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
            summary(container, data, context, moduleLinks.approvals);
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
                const list = node('ul', 'list-unstyled');
                data.items.slice(0, 5).forEach(item => {
                    const row = node('li', 'mb-2'); row.append(link(item.title, item.url), node('span', 'd-block small text-muted', `${text(context, item.kind === 'expire' ? 'expire' : 'publish')} · ${date(item.date)}`)); list.append(row);
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
            summary(container, data, context, href, `${text(context, 'submissions')} · ${domainOptions.formName || text(context, 'allForms')}`);
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
            if (type === 'traffic' || type === 'errors') summary(container, data, context, moduleLinks[type], type === 'traffic' ? text(context, metricKey(data.metric)) : null);
            period(container, data, context);
            if (instance.size !== '1x1') {
                if (type === 'traffic') lineChart(container, data, context);
                else rankedList(container, data, context, type, instance.size === '3x3');
                footer(container, context, moduleLinks[type]);
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
                container.append(link(campaign.title, campaign.url, 'fw-bold d-block'), node('p', 'small mb-1', text(context, campaign.status === 'sending' ? 'active' : campaign.status)));
                const progress = node('progress', 'w-100'); progress.max = Math.max(1, campaign.recipients); progress.value = campaign.sent;
                progress.setAttribute('aria-label', text(context, 'sent')); container.append(progress, node('p', 'small mb-1', `${text(context, 'sent')}: ${number(campaign.sent)} / ${number(campaign.recipients)}`));
                for (const [key, label] of [['failed', 'failed'], ['opens', 'opened'], ['clicks', 'clicked']]) {
                    if (campaign[key] != null && (key === 'failed' || campaign.status === 'completed')) container.append(node('span', 'small d-block', `${text(context, label)}: ${number(campaign[key])}`));
                }
            }
            footer(container, context, moduleLinks.newsletter);
            return pollNewsletter(data, container, signal, refresh);
        }
    });
}
