import { registerWidget } from './registry';
import { node, text, number, date, link, icon, field, table, empty, fetchJson, pagePreview, containNativeScroll } from './widget-utils';
import { chartHost, mountChart } from './charts';

const moduleLinks = {
    approvals: '/admin/v9/webpages/web-pages-list/?show=toapprove', publishing: '/admin/v9/apps/audit-awaiting-publish-webpages/',
    forms: '/apps/form/admin/', traffic: '/apps/stat/admin/', 'top-pages': '/apps/stat/admin/top/',
    'search-terms': '/apps/stat/admin/search-engines/', referrers: '/apps/stat/admin/referer/',
    errors: '/apps/stat/admin/error/', newsletter: '/apps/dmail/admin/'
};
const metricKey = metric => ({ views: 'visits', sessions: 'sessionsMetric', uniqueUsers: 'uniqueUsers' })[metric] || 'sessionsMetric';
const formPreviewLimit = 10;

/**
 * Completed-day interval and the start of its equally long comparison period.
 * @typedef {{from: number, to: number, previousFrom: number}} StatisticsPeriod
 */

/**
 * Ranked module record with optional comparison and page-preview fields.
 * @typedef {Object} RankedItem
 * @property {string} title - Row label.
 * @property {string} url - Module or record destination.
 * @property {number} value - Recorded count.
 * @property {number} [previous] - Previous count, absent when the previous ranking has no matching row.
 * @property {number} [percentage] - Share supplied by the statistics module.
 * @property {string} [section] - Page path or section label.
 * @property {string|null} [perexImage] - Optional page thumbnail path.
 */

/**
 * Ranking preview with the module's actual interval and optional summary.
 * @typedef {StatisticsPeriod & {items: RankedItem[], total?: number, granularity?: string}} RankingData
 */

/**
 * Daily traffic samples and totals for two completed periods.
 * @typedef {StatisticsPeriod & {metric: string, series: {date: number, value: number}[], previousSeries: {date: number, value: number}[], total: number, previous: number}} TrafficData
 */

/**
 * Reuses the forms list and the selected form's paginated, date-filtered submissions.
 * Without a selection, returns the all-form total and recent form summaries; selected-form periods include today.
 *
 * @param {Object} options - Shared period and domain-specific selection.
 * @param {string} [options.formName] - Selected form, or an empty value for all forms.
 * @param {number} [options.days=7] - Calendar days through the current time for a selected form.
 * @param {boolean} [options.details] - Whether to load up to ten submissions and their column labels.
 * @param {AbortSignal} signal - Render or configuration lifetime for all requests.
 * @returns {Promise<{total: number, options: {id: string, title: string}[], items: {title: string, date: number, url: string}[], from?: number, to?: number}>} Counts, available forms and preview rows, with a period for a selected form.
 * @throws {Error} If a saved form is absent from the authorized list; the rejected error has dashboardReason set to selection-unavailable.
 */
async function fetchForms(options, signal) {
    const forms = (await fetchJson('/admin/rest/forms-list/all', signal)).content;
    const data = {
        total: forms.reduce((sum, form) => sum + (form.count || 0), 0),
        options: forms.map(form => ({ id: form.formName, title: form.formName })).sort((a, b) => a.title.localeCompare(b.title)),
        items: forms.filter(form => form.createDate != null).sort((a, b) => b.createDate - a.createDate).slice(0, formPreviewLimit)
            .map(form => ({ title: form.formName, date: form.createDate, url: `${moduleLinks.forms}detail/?formName=${encodeURIComponent(form.formName)}` }))
    };
    if (!options.formName || signal.aborted) return data;
    if (!forms.some(form => form.formName === options.formName)) {
        const error = new Error('Form selection is unavailable');
        error.dashboardReason = 'selection-unavailable';
        throw error;
    }
    const from = new Date(), to = from.getTime() - 1;
    from.setDate(from.getDate() - (options.days || 7) + 1);
    from.setHours(0, 0, 0, 0);
    const params = new URLSearchParams({ detail: true, formName: options.formName,
        searchCreateDate: `daterange:${from.getTime()}-${to}`, size: options.details ? formPreviewLimit : 1, page: 0, sort: 'createDate,desc' });
    const submissions = await fetchJson(`/admin/rest/forms-list/search/findByColumns?${params}`, signal);
    Object.assign(data, { total: submissions.totalElements, from: from.getTime(), to, items: [] });
    if (!options.details || !submissions.content.length || signal.aborted) return data;
    const fields = await fetchJson(`/admin/rest/forms-list/columns/${encodeURIComponent(options.formName)}`, signal);
    data.items = submissions.content.slice(0, formPreviewLimit).map(item => ({
        title: formSubmissionTitle(item, fields.columns), date: item.createDate,
        url: `${moduleLinks.forms}detail/?formName=${encodeURIComponent(options.formName)}&id=${encodeURIComponent(item.id)}`
    }));
    return data;
}

/**
 * Prefers contact fields by label or name, falling back to the first three populated form columns.
 * @param {{columnNamesAndValues?: Object<string, unknown>, formName: string, id: number|string}} item - Submission values and fallback identity.
 * @param {{value: string, label?: string}[]} [columns] - Field definitions in display order; omitted definitions use the submitted field order.
 * @returns {string} Combined contact values, other populated values, or a form-name and ID fallback.
 */
function formSubmissionTitle(item, columns) {
    const values = item.columnNamesAndValues || {};
    const normalize = value => String(value || '').replace(/\([^)]*\)/g, '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z]/g, '');
    const fields = (columns || Object.keys(values).map(value => ({ value })))
        .filter(column => values[column.value] != null && String(values[column.value]).trim() !== '');
    const preferred = [
        ['meno', 'krstnemeno', 'jmeno', 'name', 'firstname', 'givenname', 'fullname', 'vorname'],
        ['priezvisko', 'prijmeni', 'surname', 'lastname', 'familyname', 'nachname'],
        ['email', 'emailaddress', 'emailovaadresa', 'emailadresse']
    ].map(names => fields.find(column => names.includes(normalize(column.label)) || names.includes(normalize(column.value)))).filter(Boolean);
    const selected = preferred.length ? [...new Set(preferred)] : fields.slice(0, 3);
    return selected.map(column => String(values[column.value]).trim()).join(' ') || `${item.formName} #${item.id}`;
}

/**
 * Uses recent module campaigns, loading an older saved selection separately when needed.
 * @param {string|number} selectedId - Saved campaign ID, or an empty value to select the newest campaign.
 * @param {import('./registry').WidgetContext} context - Supplies the localized active-campaign status.
 * @param {AbortSignal} signal - Cancels campaign requests when the owning render or dialog ends.
 * @returns {Promise<{options: {id: string, title: string}[], active: boolean, items: {title: string, status: string, sent: number, recipients: number, url: string}[]}>} Campaign choices and up to three previews with the selection first; active excludes future scheduled sends.
 */
async function fetchNewsletter(selectedId, context, signal) {
    const data = await fetchJson('/admin/rest/dmail/campaings/all?size=100&page=0&sort=id%2Cdesc', signal);
    const campaigns = data.content;
    if (selectedId && !campaigns.some(campaign => String(campaign.id) === String(selectedId))) {
        campaigns.unshift(await fetchJson(`/admin/rest/dmail/campaings/${encodeURIComponent(selectedId)}`, signal));
    }
    const selected = selectedId ? campaigns.find(campaign => String(campaign.id) === String(selectedId)) : campaigns[0];
    const ordered = selected ? [selected, ...campaigns.filter(campaign => campaign !== selected)] : [];
    return { options: campaigns.map(campaign => ({ id: String(campaign.id), title: campaign.subject })),
        active: Boolean(selected && selected.editorFields.status === context.labels.newsletterActive && !(selected.sendAt > new Date().getTime())),
        items: ordered.slice(0, 3).map(campaign => ({ title: campaign.subject, status: campaign.editorFields.status,
            sent: campaign.countOfSentMails, recipients: campaign.countOfRecipients, url: `/apps/dmail/admin/?id=${encodeURIComponent(campaign.id)}` })) };
}

/**
 * Selects the nearest publication and expiration events from the audit module's schedule.
 * @param {AbortSignal} signal - Cancels the schedule request.
 * @returns {Promise<{items: {title: string, kind: string, date: number, url: string}[]}>} Up to six distinct future events, ordered by epoch-millisecond event time.
 */
async function fetchPublishing(signal) {
    const data = await fetchJson('/admin/rest/web-pages/history/all?auditVersion=true', signal);
    const now = new Date().getTime(), seen = new Set(), items = [];
    for (const page of data.content) {
        for (const [kind, enabled, value] of [['publish', page.publicable, page.publishStartDate], ['expire', page.disableAfterEnd, page.publishEndDate]]) {
            const date = new Date(value).getTime();
            const id = `${page.docId}-${kind}-${date}`;
            if (!enabled || !(date >= now) || seen.has(id)) continue;
            seen.add(id);
            items.push({ title: page.title, kind, date, url: `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(page.docId)}` });
        }
    }
    return { items: items.sort((a, b) => a.date - b.date).slice(0, 6) };
}

/**
 * Uses calendar days so equal-length completed periods also work across daylight-saving changes.
 * @param {number} days - Number of completed calendar days in each period.
 * @param {Date} [now=new Date()] - Reference time; the current day is excluded without mutating this date.
 * @returns {StatisticsPeriod} Epoch-millisecond bounds with an inclusive end for the current period.
 */
function statisticsPeriod(days, now = new Date()) {
    const until = new Date(now); until.setHours(0, 0, 0, 0);
    const from = new Date(until); from.setDate(from.getDate() - days);
    const previousFrom = new Date(from); previousFrom.setDate(previousFrom.getDate() - days);
    return { from: from.getTime(), to: until.getTime() - 1, previousFrom: previousFrom.getTime() };
}

/**
 * Reuses the statistics module's date, folder and bot filters and DataTable error handling.
 * @param {string} type - Statistics endpoint suffix; error statistics omit the root-folder filter.
 * @param {number} from - Inclusive start in epoch milliseconds.
 * @param {number} to - Inclusive end in epoch milliseconds.
 * @param {import('./registry').WidgetContext} context - Supplies the authorized statistics root group.
 * @param {AbortSignal} signal - Cancels the request.
 * @param {number} size - Requested page size; module-side caps still apply.
 * @param {Object<string, string|number|boolean>} [filters={}] - Extra query parameters overriding shared defaults.
 * @returns {Promise<Object>} The statistics module's DataTable response.
 * @throws {Error} If a non-error request lacks a domain root; dashboardReason is domain-unavailable.
 */
function statisticsRequest(type, from, to, context, signal, size, filters = {}) {
    if (type !== 'error' && context.data.statRootGroupId == null) {
        const error = new Error('Statistics domain is unavailable');
        error.dashboardReason = 'domain-unavailable';
        throw error;
    }
    const params = new URLSearchParams({ searchDayDate: `daterange:${from}-${to}`,
        ...(type === 'error' ? {} : { searchRootDir: context.data.statRootGroupId }),
        searchFilterBotsOut: true, statType: 'days', size, page: 0, sort: 'order,asc', pagination: true, ...filters });
    return fetchJson(`/admin/rest/stat/${type}/search/findByColumns?${params}`, signal);
}

/**
 * Maps the module's daily values into the current and previous chart periods.
 * @param {Object} options - Shared traffic preferences.
 * @param {number|string} [options.days=7] - Completed-day count; unsupported values fall back to seven.
 * @param {string} [options.metric='sessions'] - One of views, sessions or uniqueUsers; unknown values use sessions.
 * @param {import('./registry').WidgetContext} context - Supplies the current statistics domain root.
 * @param {AbortSignal} signal - Cancels the request.
 * @returns {Promise<TrafficData>} Ordered daily samples and their sums for both periods.
 */
async function fetchTraffic(options, context, signal) {
    const days = [7, 30, 90].includes(Number(options.days)) ? Number(options.days) : 7;
    const range = statisticsPeriod(days);
    const data = await statisticsRequest('views', range.previousFrom, range.to, context, signal, days * 2);
    const metric = ['views', 'sessions', 'uniqueUsers'].includes(options.metric) ? options.metric : 'sessions';
    const field = metric === 'views' ? 'visits' : metric;
    const points = data.content.map(row => ({ date: Number(row.dayDate), value: row[field] })).sort((a, b) => a.date - b.date);
    const series = points.filter(point => point.date >= range.from && point.date <= range.to);
    const previousSeries = points.filter(point => point.date >= range.previousFrom && point.date < range.from);
    return { ...range, metric, series, previousSeries,
        total: series.reduce((sum, point) => sum + point.value, 0), previous: previousSeries.reduce((sum, point) => sum + point.value, 0) };
}

/**
 * Compares the current six pages with the statistics module's bounded previous ranking.
 * @param {{days?: number|string}} options - Completed-day count of 7, 30 or 90; defaults to seven.
 * @param {import('./registry').WidgetContext} context - Supplies the current statistics domain root.
 * @param {AbortSignal} signal - Cancels both ranking requests.
 * @returns {Promise<RankingData>} Current pages with previous counts when available and period-specific detail links.
 */
async function fetchTopPages(options, context, signal) {
    const days = [7, 30, 90].includes(Number(options.days)) ? Number(options.days) : 7;
    const range = statisticsPeriod(days);
    const [current, previous] = await Promise.all([
        statisticsRequest('top', range.from, range.to, context, signal, 6),
        statisticsRequest('top', range.previousFrom, range.from - 1, context, signal, 100)
    ]);
    const previousValues = new Map(previous.content.map(row => [row.docId, row.visits]));
    return { ...range, items: current.content.slice(0, 6).map(row => ({
        title: row.title, section: row.name, perexImage: row.perexImage,
        value: row.visits, previous: previousValues.get(row.docId),
        url: `/apps/stat/admin/top-details/?docId=${encodeURIComponent(row.docId)}&dateRange=${encodeURIComponent(`daterange:${range.from}-${range.to}`)}`
    })) };
}

/**
 * Uses the module's bounded ranking and percentages before taking the six-row preview.
 * @param {'search-terms'|'referrers'} type - Ranking and filters to request.
 * @param {{days?: number|string}} options - Completed-day count of 7, 30 or 90; defaults to seven.
 * @param {import('./registry').WidgetContext} context - Supplies the current statistics domain root.
 * @param {AbortSignal} signal - Cancels the request.
 * @returns {Promise<RankingData>} Up to six rows, their module percentages and a total across all returned ranking rows.
 */
async function fetchStatisticsList(type, options, context, signal) {
    const range = statisticsPeriod([7, 30, 90].includes(Number(options.days)) ? Number(options.days) : 7);
    const queries = type === 'search-terms';
    const data = await statisticsRequest(queries ? 'search-engines' : 'referer', range.from, range.to, context, signal, 6,
        queries ? { searchWebPage: -1, searchEngine: '' } : { searchChartType: 'not_chart' });
    return { ...range, total: data.content.reduce((sum, row) => sum + (queries ? row.queryCount : row.visits), 0),
        items: data.content.slice(0, 6).map(row => ({ title: queries ? row.queryName : row.serverName,
            value: queries ? row.queryCount : row.visits, percentage: row.percentage, url: moduleLinks[type] })) };
}

/**
 * Keeps the module's weekly error coverage and full summary while requesting six rows.
 * @param {{days?: number|string}} options - Requested 7-, 30- or 90-day range, expanded to weeks and capped at the current time.
 * @param {import('./registry').WidgetContext} context - Current module context.
 * @param {AbortSignal} signal - Cancels the request.
 * @returns {Promise<RankingData>} Error rows, the full module summary and the actual weekly interval.
 */
async function fetchErrors(options, context, signal) {
    const range = statisticsPeriod([7, 30, 90].includes(Number(options.days)) ? Number(options.days) : 7);
    const from = new Date(range.from), until = new Date(range.to);
    from.setDate(from.getDate() - (from.getDay() + 6) % 7);
    until.setHours(0, 0, 0, 0);
    until.setDate(until.getDate() + 7 - (until.getDay() + 6) % 7);
    range.from = from.getTime();
    range.to = Math.min(new Date().getTime(), until.getTime()) - 1;
    const data = await statisticsRequest('error', range.from, range.to, context, signal, 6,
        { searchFilterBotsOut: false, searchurl: '', sort: 'count,desc' });
    return { ...range, granularity: 'week', total: data.summary.count,
        items: data.content.map(row => ({ title: row.url, value: row.count, url: moduleLinks.errors })) };
}

/**
 * Labels the actual returned interval, including whole-week error aggregates.
 * @param {HTMLElement} container - Parent to receive period labels.
 * @param {{from?: number, to?: number, granularity?: string}} data - Actual interval and optional week granularity.
 * @param {import('./registry').WidgetContext} context - Supplies the weekly-aggregation label.
 * @param {boolean} [compact=false] - Uses a short visible range and retains the full interval in the title.
 * @returns {HTMLParagraphElement|undefined} The attached range label, or undefined when either bound is absent.
 */
function period(container, data, context, compact = false) {
    if (data.from == null || data.to == null) return;
    const full = `${date(data.from, false)} – ${date(data.to, false)}`;
    const label = node('p', 'md-dashboard-widget__period small text-muted mb-2', compact ? shortPeriod(data.from, data.to) : full);
    if (compact) label.title = full;
    container.append(label);
    if (data.granularity === 'week') {
        const weekly = text(context, 'weeklyRequests');
        if (compact) {
            label.title = `${full} · ${weekly}`;
            label.append(node('span', 'visually-hidden', ` · ${weekly}`));
        } else container.append(node('p', 'small text-muted mb-2', weekly));
    }
    return label;
}

/**
 * Uses compact localized dates without hiding a different calendar year.
 * @param {number} from - Start time in epoch milliseconds.
 * @param {number} to - End time in epoch milliseconds.
 * @returns {string} Localized date range, including years when it spans years or starts outside the current year.
 */
function shortPeriod(from, to) {
    const start = new Date(from), end = new Date(to);
    const locale = (window.userLng === 'cz' ? 'cs' : window.userLng) || 'sk';
    const year = start.getFullYear() !== new Date().getFullYear() || end.getFullYear() !== start.getFullYear();
    return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'numeric', ...(year ? { year: 'numeric' } : {}) }).formatRange(start, end);
}

/**
 * Relative changes have an explicit unavailable state when the baseline is zero.
 * @param {number|string} current - Current count, converted to a number.
 * @param {number|null|undefined} previous - Comparison count; zero and missing values have no relative change.
 * @returns {string|null} Rounded percentage with a plus sign for positive changes, or null without a usable baseline.
 */
export function change(current, previous) {
    if (previous == null || previous === 0) return null;
    const delta = (Number(current) - previous) / previous * 100;
    return `${delta > 0 ? '+' : ''}${Math.round(delta)} %`;
}

/**
 * Appends a linked total and an optional previous-period comparison with accessible labels.
 * @param {HTMLElement} container - Parent to receive the summary.
 * @param {{total: number, previous?: number}} data - Current total and optional comparison baseline.
 * @param {import('./registry').WidgetContext} context - Supplies comparison and unavailable-state labels.
 * @param {string} href - Local destination for the complete report.
 * @param {string} [label] - Visible and accessible metric label.
 * @param {string} [comparisonLabel] - Explicit period label; omission uses the generic previous-period text.
 */
function summary(container, data, context, href, label, comparisonLabel) {
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
        if (comparisonLabel && delta) comparison.prepend(icon(direction === 'positive' ? 'ti-arrow-up-right' : direction === 'negative' ? 'ti-arrow-down-right' : 'ti-arrow-right'));
        comparison.title = `${text(context, 'previous')}: ${number(data.previous)}`;
        const comparisonGroup = node('div', 'md-dashboard-widget__metric-change');
        comparisonGroup.append(comparison, node('span', 'md-dashboard-widget__comparison-label small', comparisonLabel || text(context, 'previous')));
        group.append(comparisonGroup);
    }
    container.append(group);
}

function periodField(container, options, context, completed = true) {
    return field(container, text(context, 'period'), [7, 30, 90].map(days => [days, text(context, `${completed ? "days" : "formDays"}${days}`)]), options.days || 7);
}

/**
 * Keeps an inaccessible saved selection visible instead of silently changing it.
 * @param {HTMLElement} container - Parent to receive the select field.
 * @param {string} label - Field label.
 * @param {{id: string|number, title: string}[]} choices - Currently available form or campaign choices.
 * @param {string|number|null|undefined} value - Saved selection, or an empty value for the all/latest option.
 * @param {import('./registry').WidgetContext} context - Supplies placeholder and unavailable labels.
 * @param {string} allKey - Dashboard translation suffix for the empty selection.
 * @returns {HTMLSelectElement} The attached select, retaining an unavailable saved value as a choice.
 */
function selectionField(container, label, choices, value, context, allKey) {
    const values = [['', text(context, allKey)], ...choices.map(choice => [String(choice.id), choice.title])];
    if (value && !values.some(([id]) => id === String(value))) values.push([String(value), `${value} — ${text(context, 'unavailable')}`]);
    return field(container, label, values, value || '');
}

/**
 * Builds a statistics period selector and an optional traffic-metric selector.
 * @param {import('./registry').WidgetArguments} args - Configuration container, shared options and labels.
 * @param {boolean} [metric=false] - Whether to include a sessions/views/unique-users choice.
 * @returns {import('./registry').WidgetConfiguration} Reader that preserves unrelated shared options.
 */
function statSettings({ container, options, context }, metric = false) {
    const days = periodField(container, options, context);
    const selectedMetric = metric ? field(container, text(context, 'metric'), ['sessions', 'views', 'uniqueUsers'].map(value => [value, text(context, metricKey(value))]), options.metric || 'sessions') : null;
    return { read: () => ({ options: { ...options, days: Number(days.value), ...(selectedMetric ? { metric: selectedMetric.value } : {}) } }) };
}

/**
 * Provides chart values as text, optionally hidden visually while remaining available to screen readers.
 * @param {HTMLElement} container - Parent to receive the data region.
 * @param {import('./registry').WidgetContext} context - Supplies the data-table caption or disclosure label.
 * @param {string[]} headers - Column labels.
 * @param {unknown[][]} rows - Text values or DOM nodes accepted by the shared table helper.
 * @param {boolean} [visuallyHidden=false] - Uses a visually hidden captioned table instead of a visible disclosure.
 */
function chartTable(container, context, headers, rows, visuallyHidden = false) {
    const region = node(visuallyHidden ? 'div' : 'details', `md-dashboard-widget__chart-data${visuallyHidden ? ' visually-hidden' : ''}`);
    if (!visuallyHidden) region.append(node('summary', 'small', text(context, 'chartData')));
    const values = table(region, headers, rows);
    if (visuallyHidden) values.prepend(node('caption', '', text(context, 'chartData')));
    container.append(region);
}

/**
 * Compares equal-length periods while retaining their actual dates in tooltips and the text table.
 * @param {HTMLElement} container - Parent to receive the traffic chart and its text equivalent.
 * @param {TrafficData} data - Current and previous samples; comparison points align by index.
 * @param {import('./registry').WidgetContext} context - Supplies metric and period labels.
 * @param {AbortSignal} signal - Chart render lifetime.
 * @returns {Promise<(function(): void)|undefined>} Chart cleanup, or undefined when there are no current samples or chart creation is skipped.
 */
async function lineChart(container, data, context, signal) {
    const series = data.series || [];
    if (!series.length) { empty(container, context); return; }
    const previous = data.previousSeries || [];
    const metric = text(context, metricKey(data.metric));
    const host = chartHost(container, `${metric}: ${number(data.total)}; ${text(context, 'previous')}: ${number(data.previous)}`);
    host.classList.add('md-dashboard-widget__chart--traffic');
    const legend = node('div', 'md-dashboard-widget__chart-legend');
    const currentLabel = node('span', 'md-dashboard-widget__chart-key md-dashboard-widget__chart-key--current', shortPeriod(series[0].date, series[series.length - 1].date));
    currentLabel.title = metric;
    legend.append(currentLabel);
    if (previous.length) {
        const comparison = node('span', 'md-dashboard-widget__chart-key md-dashboard-widget__chart-key--previous', shortPeriod(previous[0].date, previous[previous.length - 1].date));
        comparison.title = `${text(context, 'previous')}: ${number(data.previous)}`;
        legend.append(comparison);
    }
    container.append(legend);
    chartTable(container, context, [metric, text(context, 'previous')], series.map((point, index) => [
        `${date(point.date, false)}: ${number(point.value)}`,
        previous[index] ? `${date(previous[index].date, false)}: ${number(previous[index].value)}` : '—'
    ]), true);
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
            line.get('tooltip').setAll({ labelText: '{name}\n{actualDate}: [bold]{valueY}[/]', labelAriaLabel: '{name}, {actualDate}: {valueY}' });
            line.get('tooltip').label.set('ariaHidden', true);
        });
    });
}

/**
 * Renders a bounded ranking as a table or a referrer percentage chart with a text equivalent.
 * @param {HTMLElement} container - Parent to receive the ranking.
 * @param {RankingData} data - Ranked rows and optional summary.
 * @param {import('./registry').WidgetContext} context - Supplies headings and empty-state text.
 * @param {string} type - Widget type selecting page, search, error or referrer presentation.
 * @param {boolean} detailed - Selects a six-row default instead of five rows.
 * @param {AbortSignal} signal - Render lifetime for the optional chart.
 * @param {number} [limit] - Maximum rows; defaults to six when detailed and five otherwise.
 * @returns {Promise<(function(): void)|undefined>} Referrer-chart cleanup, or undefined for tables, empty results or skipped chart creation.
 */
async function rankedList(container, data, context, type, detailed, signal, limit = detailed ? 6 : 5) {
    const items = (data.items || []).slice(0, limit);
    if (!items.length) { empty(container, context); return; }
    if (type === 'top-pages') {
        table(container, [text(context, 'page'), text(context, 'count'), text(context, 'change')], items.map(item => [
            pagePreview({ title: item.title, fullPath: item.section, perexImage: item.perexImage }, item.url), number(item.value), change(item.value, item.previous) || '—'
        ]), [1, 2]).classList.add('md-dashboard-widget__table--ranked', 'md-dashboard-widget__table--pages');
    } else if (type === 'referrers') {
        const host = chartHost(container, `${text(context, 'source')}: ${number(data.total)}`, true);
        host.classList.add('md-dashboard-widget__chart--referrers');
        host.style.height = `${items.length * 48}px`;
        const chartData = items.map(item => ({ title: item.title, value: item.value,
            percentage: item.percentage,
            share: `${number(Math.round(item.percentage * 10) / 10)} %` }));
        chartTable(container, context, [text(context, 'source'), text(context, 'count'), text(context, 'observedShare')], chartData.map(item => [item.title, number(item.value), item.share]), true);
        return mountChart(host, signal, (tools, chartDivId) => new tools.BarChartForm({
            yAxeName: 'title', xAxeName: 'percentage', chartTitle: '', chartDivId, chartData, horizontal: true, colorScheme: 'set3'
        }), form => {
            const tooltip = form.chart.series.getIndex(1).get('tooltip');
            tooltip.set('labelText', '{title}: {value} ({share})');
            tooltip.label.setAll({ ignoreFormatting: true, ariaHidden: true });
        });
    } else table(container, [text(context, type === 'search-terms' ? 'query' : type === 'referrers' ? 'source' : 'page'), text(context, 'count')], items.map(item => [link(item.title, item.url), number(item.value)]), [1]).classList.add('md-dashboard-widget__table--ranked');
}

/**
 * Polls only an active newsletter visible in the current browser tab.
 * @param {{active: boolean}} data - Whether the selected campaign is currently sending.
 * @param {HTMLElement} container - Card observed for viewport visibility.
 * @param {AbortSignal} signal - Prevents refresh calls once the render is aborted.
 * @param {function(): Promise<void>} refresh - Invoked every thirty seconds while visible; its return value is ignored.
 * @returns {(function(): void)|undefined} Observer and timer cleanup for the renderer to return, or undefined for an inactive campaign.
 */
function pollNewsletter(data, container, signal, refresh) {
    if (!data.active) return;
    let visible = false;
    const observer = new IntersectionObserver(entries => { visible = entries.some(entry => entry.isIntersecting); });
    observer.observe(container);
    const timer = window.setInterval(() => { if (visible && document.visibilityState === 'visible' && !signal.aborted) refresh(); }, 30000);
    return () => { observer.disconnect(); window.clearInterval(timer); };
}

/**
 * Combines the existing page and folder approval queues into a six-item preview.
 * @param {AbortSignal} signal - Cancels both module requests.
 * @returns {Promise<{total: number, items: {title: string, icon: string, section: string, date: number, url: string}[]}>} Combined queue count and the newest requests with their approval destinations.
 */
async function fetchApprovals(signal) {
    const query = '?size=6&page=0&sort=saveDate,desc';
    const [pages, groups] = await Promise.all([
        fetchJson(`/admin/rest/webpages/toapprove/all${query}`, signal),
        fetchJson(`/admin/rest/groups/toapprove/all${query}`, signal)
    ]);
    const items = pages.content.map(page => {
        const action = page.isDelete || page.title?.startsWith('[DELETE]') ? 'approve_delete' : 'approve';
        // The page approval REST service swaps docId and historyId for its DataTable.
        return { title: page.title, icon: 'ti-article', section: page.authorName, date: page.saveDate,
            url: `/admin/${action}.jsp?docid=${page.historyId}&historyid=${page.docId}` };
    });
    groups.content.forEach(group => {
        items.push({ title: group.groupName, icon: 'ti-folder-filled', section: group.userFullName, date: group.saveDate,
            url: `/admin/v9/webpages/web-pages-list/?groupid=${group.groupId}&scheduleId=${group.schedulerId}${group.isDelete ? '&act=delete' : ''}` });
    });
    items.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    return { total: pages.totalElements + groups.totalElements, items: items.slice(0, 6) };
}

/** Registers content, analytics, and newsletter widgets using authorized projections. */
export function registerDataWidgets() {
    registerWidget({
        type: 'approvals', titleKey: 'admin.dashboard.approvals.js', icon: 'ti-checkup-list', multiple: true, sizes: ['1x1', '3x3'], defaultSize: '3x3',
        headerLink: { href: moduleLinks.approvals },
        isAvailable: () => window.WJ.hasPermission('menuWebpages'),
        async render({ container, instance, context, signal }) {
            const data = await fetchApprovals(signal); if (signal.aborted) return;
            summary(container, data, context, moduleLinks.approvals, text(context, 'pendingPages'));
            if (instance.size !== '1x1') {
                if (!data.items.length) empty(container, context);
                else table(container, [text(context, 'page'), text(context, 'requester'), text(context, 'waitingSince')], data.items.map(item => {
                    const approval = link(item.title, item.url);
                    const typeIcon = icon(item.icon);
                    typeIcon.classList.add('me-1');
                    approval.prepend(typeIcon);
                    approval.target = '_blank';
                    approval.rel = 'noopener';
                    return [approval, item.section, date(item.date)];
                })).classList.add('md-dashboard-widget__table--approvals');
            }
        }
    });
    registerWidget({
        type: 'publishing', titleKey: 'admin.dashboard.publishing.js', icon: 'ti-calendar-event', multiple: true, sizes: ['2x2', '2x3'], defaultSize: '2x2',
        headerLink: { href: moduleLinks.publishing },
        isAvailable: () => window.WJ.hasPermission('menuWebpages') && window.WJ.hasPermission('cmp_adminlog'),
        async render({ container, instance, context, signal }) {
            const data = await fetchPublishing(signal); if (signal.aborted) return;
            if (!data.items.length) empty(container, context);
            else {
                const status = node('p', 'md-dashboard-widget__publishing-status small');
                status.append(icon('ti-clock'), document.createTextNode(text(context, 'scheduledChanges')));
                container.append(status);
                const list = node('ul', 'md-dashboard-widget__publishing list-unstyled');
                data.items.slice(0, instance?.size === '2x2' ? 2 : 5).forEach(item => {
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
                    const timing = node('span', 'md-dashboard-widget__publication-time small', `${text(context, item.kind === 'expire' ? 'expire' : 'publish')} · ${scheduledTime}`);
                    if (validDate && parsed.getFullYear() !== new Date().getFullYear()) timing.append(document.createTextNode(' · '), node('span', 'md-dashboard-widget__publication-year', parsed.getFullYear()));
                    content.append(link(item.title, item.url), timing);
                    row.append(content);
                    list.append(row);
                }); container.append(list);
            }
            container.append(node('p', 'md-dashboard-widget__footnote small', text(context, 'nextScheduled')));
        }
    });
    registerWidget({
        type: 'forms', titleKey: 'admin.dashboard.forms.js', icon: 'ti-forms', sizes: ['1x1', '3x3'], defaultSize: '3x3', multiple: true,
        headerLink: { href: (instance, context) => {
            const formName = context.settings.domainOptions?.[instance.id]?.formName;
            return formName ? `${moduleLinks.forms}detail/?formName=${encodeURIComponent(formName)}` : moduleLinks.forms;
        } },
        defaultOptions: { days: 7 }, defaultDomainOptions: { formName: '' }, isAvailable: () => window.WJ.hasPermission('cmp_form'),
        async configure({ container, options, domainOptions, context, signal }) {
            const data = await fetchForms({}, signal);
            const form = selectionField(container, text(context, 'formName'), data.options || [], domainOptions.formName, context, 'allForms');
            const days = periodField(container, options, context, false);
            const daysField = days.parentElement;
            const updatePeriod = () => { daysField.hidden = !form.value; };
            form.addEventListener('change', updatePeriod, { signal });
            updatePeriod();
            return { read: () => ({ options: { days: Number(days.value) }, domainOptions: { formName: form.value } }) };
        },
        async render({ container, instance, options, domainOptions, context, signal }) {
            const compact = instance.size === '1x1';
            const data = await fetchForms({ days: options.days, formName: domainOptions.formName, details: !compact }, signal); if (signal.aborted) return;
            const href = domainOptions.formName ? `${moduleLinks.forms}detail/?formName=${encodeURIComponent(domainOptions.formName)}` : moduleLinks.forms;
            const label = domainOptions.formName ? (compact ? text(context, 'formSubmissionsPeriod', options.days || 7) : text(context, 'submissions')) : text(context, 'totalSubmissions');
            summary(container, data, context, href, label);
            if (domainOptions.formName) container.append(node('span', 'small text-muted', domainOptions.formName));
            const interval = period(container, data, context);
            if (compact) interval?.classList.add('visually-hidden');
            if (instance.size !== '1x1') {
                if (!data.items.length) empty(container, context);
                else {
                    const list = node('div', 'md-dashboard-widget__forms');
                    list.tabIndex = 0;
                    list.setAttribute('aria-label', text(context, 'forms'));
                    containNativeScroll(list, signal);
                    table(list, [text(context, 'formName'), text(context, 'date')], data.items.slice(0, formPreviewLimit).map(item => [link(item.title, item.url), date(item.date)]));
                    container.append(list);
                }
            }
        }
    });
    const definitions = [
        ['traffic', 'ti-chart-line', ['1x1', '3x3']], ['top-pages', 'ti-chart-bar', ['2x3', '3x3']],
        ['search-terms', 'ti-search', ['2x3', '3x3']], ['referrers', 'ti-route', ['2x2', '2x3', '3x3']], ['errors', 'ti-error-404', ['1x1', '3x3']]
    ];
    definitions.forEach(([type, icon, sizes]) => registerWidget({
        type, titleKey: `admin.dashboard.${type}.js`, icon, sizes, defaultSize: type === 'referrers' ? '2x2' : sizes[sizes.length - 1], multiple: true,
        headerLink: { href: moduleLinks[type] },
        defaultOptions: { days: 7, ...(type === 'traffic' ? { metric: 'sessions' } : {}) },
        isAvailable: context => window.WJ.hasPermission('cmp_stat') && context.config.statMode !== 'none',
        configure: args => statSettings(args, type === 'traffic'),
        async render({ container, instance, options, context, signal }) {
            const data = type === 'traffic' ? await fetchTraffic(options, context, signal)
                : type === 'top-pages' ? await fetchTopPages(options, context, signal)
                : type === 'errors' ? await fetchErrors(options, context, signal) : await fetchStatisticsList(type, options, context, signal);
            if (signal.aborted) return;
            if (type === 'traffic') {
                const days = String(options.days || 7);
                const label = text(context, `traffic${data.metric === 'views' ? 'Views' : data.metric === 'uniqueUsers' ? 'Users' : 'Sessions'}`, days);
                summary(container, data, context, moduleLinks.traffic, label, text(context, 'trafficComparison', days));
            } else if (type === 'errors') summary(container, data, context, moduleLinks.errors, text(context, 'requests'));
            if (type !== 'referrers') period(container, data, context, type === 'traffic' || (type === 'errors' && instance.size === '1x1'));
            if (instance.size !== '1x1') {
                const cleanup = type === 'traffic' ? await lineChart(container, data, context, signal)
                    : await rankedList(container, data, context, type, instance.size === '3x3', signal, type === 'referrers' && instance.size === '2x2' ? 3 : undefined);
                if (!signal.aborted && type === 'referrers') {
                    const caption = node('p', 'md-dashboard-widget__footnote small', text(context, 'observedSourcesPeriod', options.days || 7));
                    if (data.from != null && data.to != null) caption.title = `${date(data.from, false)} – ${date(data.to, false)}`;
                    container.append(caption);
                }
                return cleanup;
            }
        }
    }));
    registerWidget({
        type: 'newsletter', titleKey: 'admin.dashboard.newsletter.js', icon: 'ti-send', sizes: ['2x2', '3x3'], multiple: true,
        headerLink: { href: moduleLinks.newsletter },
        defaultDomainOptions: { campaignId: '' }, isAvailable: () => window.WJ.hasPermission('menuEmail'),
        async configure({ container, domainOptions, context, signal }) {
            const data = await fetchNewsletter('', context, signal);
            const campaign = selectionField(container, text(context, 'campaign'), data.options, domainOptions.campaignId, context, 'latestCampaign');
            return { read: () => ({ domainOptions: { campaignId: campaign.value } }) };
        },
        async render({ container, instance, domainOptions, context, signal, refresh }) {
            const data = await fetchNewsletter(domainOptions.campaignId, context, signal); if (signal.aborted) return;
            if (!data.items.length) empty(container, context);
            else if (instance.size === '3x3') {
                table(container, [text(context, 'campaign'), text(context, 'status'), text(context, 'sent')], data.items.map(item => [
                    link(item.title, item.url), item.status, `${number(item.sent)} / ${number(item.recipients)}`
                ]));
            } else {
                const campaign = data.items[0];
                const status = node('p', 'md-dashboard-widget__newsletter-status small');
                status.append(icon(data.active ? 'ti-send' : 'ti-clock'), document.createTextNode(campaign.status));
                container.append(status, link(campaign.title, campaign.url, 'md-dashboard-widget__newsletter-title'));
                const metric = node('div', 'md-dashboard-widget__newsletter-metric');
                const count = node('span');
                count.append(node('strong', '', number(campaign.sent)), document.createTextNode(` / ${number(campaign.recipients)}`));
                metric.append(count, node('span', 'md-dashboard-widget__newsletter-percent', campaign.sent != null && campaign.recipients > 0 ? `${number(Math.round(campaign.sent / campaign.recipients * 100))} %` : '—'));
                const progress = node('progress', 'md-dashboard-widget__newsletter-progress w-100'); progress.max = Math.max(1, campaign.recipients); progress.value = campaign.sent;
                progress.setAttribute('aria-label', text(context, 'sent'));
                metric.setAttribute('aria-label', `${text(context, 'sent')}: ${number(campaign.sent)} / ${number(campaign.recipients)}`);
                container.append(metric, progress);
            }
            return pollNewsletter(data, container, signal, refresh);
        }
    });
}
