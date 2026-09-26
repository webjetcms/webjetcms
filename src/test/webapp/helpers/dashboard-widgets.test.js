const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

function fixture(t, { pages = [], menu = [], allowed = true, ok = true, extraWidgets = false, data = {}, fetchResponse } = {}) {
    const dom = new JSDOM('<!doctype html><body><main></main></body>', { url: 'http://localhost/admin/v9/' });
    const window = dom.window;
    window.userLng = 'en';
    window.csrfToken = 'test-csrf-token';
    window.WJ = { hasPermission: () => allowed };
    const requests = [];
    const scope = vm.createContext({ window, document: window.document, Node: window.Node, DOMParser: window.DOMParser, URL, URLSearchParams, AbortController, console,
        fetch: async (url, options) => { requests.push({ url, options }); return fetchResponse ? fetchResponse(url, options) : { ok, status: ok ? 200 : 403, json: async () => url.includes('/data/') ? data : pages }; }
    });
    for (const file of ['registry.js', 'widget-utils.js', 'charts.js', 'utility-widgets.js', 'data-widgets.js', 'widgets.js']) {
        const source = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/dashboard', file), 'utf8')
            .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');
        vm.runInContext(source, scope, { filename: file });
    }
    scope.registerDashboardWidgets();
    if (extraWidgets && !scope.getWidget('sessions')) { scope.registerUtilityWidgets(); scope.registerDataWidgets(); }
    const context = { data: { dashboardMenu: menu }, labels: {}, settings: {}, config: {}, translate: key => key };
    t.after(() => window.close());
    return { scope, context, container: window.document.querySelector('main'), requests, window };
}

test('Shortcuts resolve authorized submenu targets and never execute user-controlled titles or URLs', t => {
    const { scope, context, container } = fixture(t, { menu: [{ text: 'Applications', childrens: [
        { text: '<img src=x onerror=alert(1)>', href: '/apps/form/admin/', icon: 'ti-forms' },
        { text: 'Unsafe', href: 'javascript:alert(1)' }, { text: 'External', href: 'https://other.test/' }
    ] }] });
    assert.equal(scope.menuEntries(context).length, 1);
    const widget = scope.getWidget('shortcut');
    widget.render({ container, context, options: { href: '/apps/form/admin/', title: '<script>bad()</script>' } });
    assert.equal(container.querySelectorAll('script,img').length, 0);
    assert.equal(container.querySelector('a').getAttribute('href'), '/apps/form/admin/');
    assert.match(container.textContent, /<script>bad\(\)<\/script>/);
    container.replaceChildren();
    widget.render({ container, context, options: { href: 'javascript:alert(1)' } });
    assert.equal(container.querySelector('a'), null);
});

test('Custom shortcuts require explicit URL mode and reject ambiguous or executable targets', t => {
    const { scope, context, container } = fixture(t);
    const widget = scope.getWidget('shortcut');
    const invalid = ['javascript:alert(1)', 'data:text/html,<script>alert(1)</script>', '//other.test/path', '/\\other.test/path',
        'https://user:secret@other.test/', 'https://other.test/\nscript', 'relative/path', 'https:other.test', 'https:///other.test'];
    for (const href of invalid) {
        assert.equal(scope.shortcutUrl(href), null, href);
        widget.render({ container, context, options: { source: 'url', href, title: 'Unsafe' } });
    }
    assert.equal(container.querySelector('a'), null);
    widget.render({ container, context, options: { href: 'https://other.test/', title: 'Legacy' } });
    assert.equal(container.querySelector('a'), null);
    for (const href of ['https://other.test/path?x=1#section', '/apps/form/admin/']) {
        widget.render({ container, context, options: { source: 'url', href, title: '<img src=x>' } });
        assert.equal(container.lastElementChild.getAttribute('href'), href);
    }
    assert.equal(container.querySelector('img'), null);
});

test('Shortcut settings toggle authorized menu and explicit URL fields and validate before persistence', t => {
    const { scope, context, container, window } = fixture(t, { menu: [{ text: 'Forms', href: '/apps/form/admin/' }] });
    const settings = scope.getWidget('shortcut').configure({ container, context, options: {} });
    const [source, module] = container.querySelectorAll('select');
    const [url, title] = container.querySelectorAll('input');
    assert.equal(url.parentElement.hidden, true);
    assert.equal(settings.read().options.href, '/apps/form/admin/');
    source.value = 'url';
    source.dispatchEvent(new window.Event('change'));
    assert.equal(module.parentElement.hidden, true);
    assert.equal(url.parentElement.hidden, false);
    url.value = '//other.test/';
    assert.throws(() => settings.read(), /shortcutUrlInvalid/);
    url.value = 'https://other.test';
    assert.throws(() => settings.read(), /shortcutTitleRequired/);
    title.value = ' My site ';
    assert.deepEqual(JSON.parse(JSON.stringify(settings.read())), { options: { source: 'url', href: 'https://other.test/', title: 'My site' } });
});

test('Recent pages use a bounded preview and expose server-filtered titles safely in both variants', async t => {
    const pages = Array.from({ length: 8 }, (_, index) => ({ docId: index + 1, title: index ? `Page ${index}` : '<img src=x>', fullPath: '/Section', saveDate: '26.09.2026 10:00' }));
    const { scope, context, container, requests } = fixture(t, { pages });
    const widget = scope.getWidget('recent-pages');
    const signal = new AbortController().signal;
    await widget.render({ container, context, signal, instance: { size: '2x3' } });
    assert.equal(container.querySelectorAll('li').length, 5);
    assert.equal(container.querySelector('img'), null);
    container.replaceChildren();
    await widget.render({ container, context, signal, instance: { size: '3x3' } });
    assert.equal(container.querySelectorAll('li').length, 6);
    assert.equal(container.querySelectorAll('.md-dashboard-widget__page-section').length, 6);
    assert.match(container.querySelector('.md-dashboard-widget__page-date').textContent, /26.09.2026/);
    assert.equal(requests[0].options.signal, signal);
    assert.equal(requests[0].options.headers['X-CSRF-Token'], 'test-csrf-token');
});

test('Recent page thumbnails use supplied local perex images and restore an icon when loading fails', async t => {
    const { scope, context, container, window } = fixture(t, { pages: [
        { docId: 1, title: 'Page', fullPath: '/Section/Page', perexImage: '/images/news/photo.jpg', saveDate: '02.03.2026 14:30:22' },
        { docId: 2, title: 'External image', perexImage: '//external.test/photo.jpg' },
        { docId: 3, title: 'Executable image', perexImage: 'javascript:alert(1)' }
    ] });
    await scope.getWidget('recent-pages').render({ container, context, signal: new AbortController().signal, instance: { size: '3x3' } });
    assert.equal(container.querySelectorAll('img').length, 1);
    const thumbnail = container.querySelector('.md-dashboard-widget__page-image');
    const image = thumbnail.querySelector('img');
    assert.equal(image.alt, '');
    assert.equal(image.width, 38);
    assert.equal(image.getAttribute('src'), '/thumb/images/news/photo.jpg?w=76&h=76&ip=5');
    assert.equal(thumbnail.querySelector('i').hidden, true);
    assert.equal(container.querySelector('.md-dashboard-widget__page-section').textContent, '/Section');
    assert.equal(container.querySelector('.md-dashboard-widget__page-date').textContent, '02.03.2026 14:30:22');
    image.dispatchEvent(new window.Event('error'));
    assert.equal(thumbnail.querySelector('img'), null);
    assert.equal(thumbnail.querySelector('i').hidden, false);
});

test('A denied recent-page request remains an error instead of an empty result', async t => {
    const { scope, context, container } = fixture(t, { ok: false });
    await assert.rejects(scope.getWidget('recent-pages').render({ container, context, instance: { size: '3x3' }, signal: new AbortController().signal }), /403/);
    assert.equal(container.textContent, '');
});

test('Default widgets follow permissions and authorized menu destinations', t => {
    const { scope, context } = fixture(t, { allowed: false, menu: [] });
    assert.deepEqual(JSON.parse(JSON.stringify(scope.getDashboardDefaults(context))).map(item => item.type), ['search', 'sessions', 'news']);
    assert.equal(scope.getWidget('recent-pages').isAvailable(context), false);
    assert.equal(scope.getWidget('shortcut').isAvailable(context), true);
});

test('New profiles include each available type and no more than two authorized shortcut destinations', t => {
    const menu = [
        { text: 'Pages', href: '/admin/v9/webpages/web-pages-list/' }, { text: 'Forms', href: '/apps/form/admin/' },
        { text: 'Newsletter', href: '/apps/dmail/admin/' }, { text: 'Unsafe', href: 'javascript:alert(1)' }
    ];
    const { scope, context } = fixture(t, { menu });
    const defaults = JSON.parse(JSON.stringify(scope.getDashboardDefaults(context)));
    const expected = Array.from(scope.listWidgets()).filter(widget => !widget.isAvailable || widget.isAvailable(context));
    for (const definition of expected) {
        assert.equal(defaults.filter(item => item.type === definition.type).length, definition.type === 'shortcut' ? 2 : 1);
    }
    assert.deepEqual(defaults.filter(item => item.type === 'shortcut').map(item => item.options.href), menu.slice(0, 2).map(item => item.href));
    assert.equal(defaults.length, expected.length + 1);
});

test('Default shortcuts fall back to the first authorized module when pages and forms are unavailable', t => {
    const { scope, context } = fixture(t, { menu: [
        { text: 'External', href: 'https://other.test/' }, { text: 'Newsletter', href: '/apps/dmail/admin/' },
        { text: 'Statistics', href: '/apps/stat/admin/' }
    ] });
    const shortcuts = JSON.parse(JSON.stringify(scope.getDashboardDefaults(context))).filter(item => item.type === 'shortcut');
    assert.deepEqual(shortcuts.map(item => item.options.href), ['/apps/dmail/admin/']);
});

function chartRuntime(window, { load = async () => {}, create } = {}) {
    const forms = [];
    const roots = new Set();
    const destroyed = [];
    const settings = (initial = {}) => ({
        values: { ...initial },
        set(key, value) { this.values[key] = value; },
        setAll(values) { Object.assign(this.values, values); },
        get(key) { return this.values[key]; },
        dispose() { this.disposed = true; },
        appear(duration) { this.appearanceDuration = duration; }
    });
    const list = values => ({ values, getIndex: index => values[index], each: callback => values.forEach(callback) });
    const axis = () => settings({ renderer: { labels: { template: settings() }, grid: { template: settings() } } });
    class LineChartForm { constructor(config) { Object.assign(this, config); } }
    class BarChartForm { constructor(config) { Object.assign(this, config); } }
    const makeChart = form => {
        const series = Array.from({ length: form instanceof LineChartForm ? form.chartData.size : 1 }, () => {
            const tooltip = Object.assign(settings(), { label: settings() });
            return Object.assign(settings({ tooltip }), { strokes: { template: settings() }, fills: { template: settings() } });
        });
        const chart = Object.assign(settings({ cursor: settings(), scrollbarX: settings(), scrollbarY: settings(), colors: settings() }), {
            root: { setThemes(themes) { this.themes = themes; } },
            series: list(series), xAxes: list([axis()]), yAxes: list([axis()]),
            children: list([settings({ verticalScrollbar: settings() })]), zoomOutButton: settings()
        });
        form.chart = chart;
        roots.add(form.chartDivId);
    };
    let loads = 0;
    window.initAmcharts = async () => { loads++; await load(); };
    window.am5 = { percent: value => value, color: value => value };
    window.WebjetTheme = { new: () => ({ theme: 'WebJET' }) };
    window.ChartTools = {
        LineChartForm, BarChartForm, DateType: { Days: 'day' },
        async createAmchart(form) {
            forms.push(form);
            if (create) await create(form, makeChart);
            else makeChart(form);
        },
        destroyChart(form) { roots.delete(form.chartDivId); destroyed.push(form.chartDivId); }
    };
    return { forms, roots, destroyed, loads: () => loads };
}

const trafficData = {
    total: 7, previous: 4, metric: 'sessions',
    series: [{ date: Date.UTC(2026, 8, 24), value: 3 }, { date: Date.UTC(2026, 8, 25), value: 4 }],
    previousSeries: [{ date: Date.UTC(2026, 8, 22), value: 1 }, { date: Date.UTC(2026, 8, 23), value: 3 }]
};

test('Traffic uses shared AmCharts line forms with aligned comparison points and actual accessible dates', async t => {
    const { scope, context, container, window } = fixture(t, { data: trafficData });
    const runtime = chartRuntime(window);
    const controller = new AbortController();
    const cleanup = await scope.getWidget('traffic').render({ container, context, options: {}, instance: { size: '3x3' }, signal: controller.signal });
    const form = runtime.forms[0];
    assert.ok(form instanceof window.ChartTools.LineChartForm);
    const [current, previous] = Array.from(form.chartData.values());
    assert.deepEqual(Array.from(current, point => point.dayDate), trafficData.series.map(point => point.date));
    assert.deepEqual(Array.from(previous, point => point.dayDate), trafficData.series.map(point => point.date));
    assert.equal(previous[0].actualDate, scope.date(trafficData.previousSeries[0].date, false));
    assert.deepEqual(Array.from(form.chart.series.getIndex(1).strokes.template.get('strokeDasharray')), [5, 4]);
    assert.match(form.chart.series.getIndex(1).get('tooltip').get('labelText'), /actualDate/);
    assert.equal(form.chart.get('scrollbarX'), undefined);
    assert.equal(form.chart.appearanceDuration, 0);
    assert.equal(container.querySelectorAll('details tbody tr').length, 2);
    assert.match(container.querySelector('details').textContent, /9\/22\/2026/);
    assert.equal(container.querySelector('svg'), null);
    controller.abort();
    cleanup();
    assert.equal(runtime.roots.size, 0);
    assert.equal(runtime.destroyed.length, 1);
});

test('Chart colors come from runtime CSS properties while data and lifecycle stay in ChartTools', async t => {
    const { scope, context, container, window } = fixture(t, { data: trafficData });
    const runtime = chartRuntime(window, { load: async () => {
        const chart = container.querySelector('.md-dashboard-widget__chart');
        chart.style.setProperty('--wj-dashboard-chart-primary', 'rgb(6.84, 129.16, 111.48)');
        chart.style.setProperty('--wj-dashboard-chart-comparison', '#686f83');
        chart.style.setProperty('--wj-dashboard-chart-grid', '#dddfe6');
        chart.style.setProperty('--wj-dashboard-chart-label', '#272727');
    } });
    const cleanup = await scope.getWidget('traffic').render({ container, context, options: {}, instance: { size: '3x3' }, signal: new AbortController().signal });
    const chart = runtime.forms[0].chart;
    assert.equal(chart.series.getIndex(0).get('stroke'), 'rgb(7,129,111)');
    assert.equal(chart.series.getIndex(1).get('stroke'), '#686f83');
    assert.equal(chart.series.getIndex(0).fills.template.get('fillOpacity'), 0.08);
    assert.equal(chart.xAxes.getIndex(0).get('renderer').labels.template.get('fill'), '#272727');
    assert.equal(chart.yAxes.getIndex(0).get('renderer').grid.template.get('stroke'), '#dddfe6');
    cleanup();
    assert.equal(runtime.roots.size, 0);
});

test('Detailed referrers use horizontal AmCharts and retain literal labels and shares of the full total', async t => {
    const data = { total: 100, items: Array.from({ length: 8 }, (_, index) => ({ title: index ? `Source ${index}` : '<img src=x>[bold]', value: 10 - index })) };
    const { scope, context, container, window } = fixture(t, { data });
    const runtime = chartRuntime(window);
    const cleanup = await scope.getWidget('referrers').render({ container, context, options: {}, instance: { size: '3x3' }, signal: new AbortController().signal });
    const form = runtime.forms[0];
    assert.ok(form instanceof window.ChartTools.BarChartForm);
    assert.equal(form.horizontal, true);
    assert.equal(form.chartData.length, 6);
    assert.equal(form.chartData[0].share, '10 %');
    assert.equal(form.chart.series.getIndex(0).get('tooltip').label.get('ignoreFormatting'), true);
    assert.equal(container.querySelector('img'), null);
    assert.match(container.querySelector('details').textContent, /<img src=x>\[bold\]/);
    assert.equal(container.querySelectorAll('details tbody tr').length, 6);
    cleanup();
    assert.equal(runtime.roots.size, 0);
});

test('Compact referrers retain shared horizontal charts, text values, and abort cleanup', async t => {
    const data = { total: 200, items: Array.from({ length: 6 }, (_, index) => ({ title: `Source ${index}`, value: 20 - index })) };
    const { scope, context, container, window } = fixture(t, { data });
    const runtime = chartRuntime(window);
    const controller = new AbortController();
    const cleanup = await scope.getWidget('referrers').render({ container, context, options: {}, instance: { size: '2x3' }, signal: controller.signal });
    assert.ok(runtime.forms[0] instanceof window.ChartTools.BarChartForm);
    assert.equal(runtime.forms[0].chartData.length, 5);
    assert.equal(runtime.forms[0].chartData[0].share, '10 %');
    assert.ok(container.querySelector('.md-dashboard-widget__chart--compact'));
    assert.equal(container.querySelectorAll('details tbody tr').length, 5);
    controller.abort();
    cleanup();
    assert.equal(runtime.roots.size, 0);
    assert.equal(runtime.destroyed.length, 1);
});

test('Publishing calendars retain full dates and mark only publication events as ready', async t => {
    const data = { items: [
        { title: '<img src=x>', kind: 'publish', date: Date.UTC(2026, 8, 28, 8), url: '/admin/v9/webpages/web-pages-list/?docid=1' },
        { title: 'Expiry', kind: 'expire', date: Date.UTC(2027, 0, 2, 9), url: '/admin/v9/webpages/web-pages-list/?docid=2' }
    ] };
    const { scope, context, container } = fixture(t, { data });
    await scope.getWidget('publishing').render({ container, context, signal: new AbortController().signal });
    const rows = container.querySelectorAll('.md-dashboard-widget__publication');
    assert.equal(rows[0].querySelector('time').dateTime, '2026-09-28T08:00:00.000Z');
    assert.match(rows[0].querySelector('time').getAttribute('aria-label'), /2026/);
    assert.match(rows[1].querySelector('time').getAttribute('aria-label'), /2027/);
    assert.equal(rows[0].querySelector('time strong').textContent, '28');
    assert.match(rows[0].querySelector('.md-dashboard-widget__publication-time').textContent, /publish/);
    assert.match(rows[0].querySelector('.md-dashboard-widget__publication-status').textContent, /draft/);
    assert.equal(rows[1].querySelector('.md-dashboard-widget__publication-status'), null);
    assert.match(rows[1].querySelector('.md-dashboard-widget__publication-time').textContent, /expire/);
    assert.equal(container.querySelector('img'), null);
});

test('Newsletter progress preserves actual status and counts without inventing a percentage for an empty audience', async t => {
    const campaign = { title: '<img src=x>', status: 'paused', sent: 0, recipients: 0, failed: 0, opens: 12, clicks: 2, url: '/apps/dmail/admin/' };
    const { scope, context, container } = fixture(t, { data: { items: [campaign] } });
    const args = { container, context, instance: { size: '2x2' }, domainOptions: {}, signal: new AbortController().signal };
    await scope.getWidget('newsletter').render(args);
    assert.match(container.querySelector('.md-dashboard-widget__newsletter-status').textContent, /paused/);
    assert.equal(container.querySelector('.md-dashboard-widget__newsletter-percent').textContent, '—');
    assert.equal(container.querySelector('progress').value, 0);
    assert.equal(container.querySelector('.md-dashboard-widget__newsletter-metric strong').textContent, '0');
    assert.doesNotMatch(container.querySelector('.md-dashboard-widget__newsletter-details').textContent, /opened|clicked/);
    assert.equal(container.querySelector('img'), null);
    container.replaceChildren();
    Object.assign(campaign, { status: 'completed', sent: 99, recipients: 100, failed: 1 });
    await scope.getWidget('newsletter').render(args);
    assert.match(container.querySelector('.md-dashboard-widget__newsletter-status').textContent, /completed/);
    assert.equal(container.querySelector('.md-dashboard-widget__newsletter-percent').textContent, '99 %');
    assert.equal(container.querySelector('progress').max, 100);
    assert.equal(container.querySelector('progress').value, 99);
    assert.match(container.querySelector('.md-dashboard-widget__newsletter-details').textContent, /opened.*12.*clicked.*2/);
});

test('Numeric, collapsed, and empty traffic previews do not initialize charts', async t => {
    const { scope, context, container, window } = fixture(t, { data: { ...trafficData, series: [] } });
    const runtime = chartRuntime(window);
    const args = { container, context, options: {}, instance: { size: '1x1', type: 'traffic' }, signal: new AbortController().signal };
    const widget = scope.getWidget('traffic');
    await widget.render(args);
    await widget.renderCollapsed(args);
    await widget.render({ ...args, instance: { size: '3x3' } });
    assert.equal(runtime.loads(), 0);
    assert.equal(container.querySelector('.md-dashboard-widget__chart'), null);
});

test('An aborted lazy chart load never creates an AmCharts root', async t => {
    const { scope, context, container, window } = fixture(t, { data: trafficData });
    let finishLoad;
    const runtime = chartRuntime(window, { load: () => new Promise(resolve => { finishLoad = resolve; }) });
    const controller = new AbortController();
    const rendering = scope.getWidget('traffic').render({ container, context, options: {}, instance: { size: '3x3' }, signal: controller.signal });
    await new Promise(resolve => setImmediate(resolve));
    controller.abort();
    finishLoad();
    await rendering;
    assert.equal(runtime.forms.length, 0);
    assert.equal(container.querySelector('.md-dashboard-widget__more'), null);
});

test('Late chart creation after an abort disposes the stale root instead of retaining detached resources', async t => {
    const { scope, context, container, window } = fixture(t, { data: trafficData });
    let finishCreate;
    const runtime = chartRuntime(window, { create: (form, makeChart) => new Promise(resolve => { finishCreate = () => { makeChart(form); resolve(); }; }) });
    const controller = new AbortController();
    const rendering = scope.getWidget('traffic').render({ container, context, options: {}, instance: { size: '3x3' }, signal: controller.signal });
    await new Promise(resolve => setImmediate(resolve));
    controller.abort();
    finishCreate();
    const cleanup = await rendering;
    cleanup();
    assert.equal(runtime.roots.size, 0);
});

test('Chart initialization failures release partially created roots and keep the framework error path', async t => {
    const { scope, context, container, window } = fixture(t, { data: trafficData });
    const runtime = chartRuntime(window, { create: async (form, makeChart) => { makeChart(form); throw new Error('chart failure'); } });
    await assert.rejects(scope.getWidget('traffic').render({ container, context, options: {}, instance: { size: '3x3' }, signal: new AbortController().signal }), /chart failure/);
    assert.equal(runtime.roots.size, 0);
});

test('Concurrent graph instances own distinct roots and disposing one leaves the other intact', async t => {
    const { scope, context, container, window } = fixture(t, { data: trafficData });
    const runtime = chartRuntime(window);
    const second = window.document.createElement('section');
    container.append(second);
    const widget = scope.getWidget('traffic');
    const firstCleanup = await widget.render({ container, context, options: {}, instance: { size: '3x3' }, signal: new AbortController().signal });
    const secondCleanup = await widget.render({ container: second, context, options: {}, instance: { size: '3x3' }, signal: new AbortController().signal });
    assert.notEqual(runtime.forms[0].chartDivId, runtime.forms[1].chartDivId);
    firstCleanup();
    assert.equal(runtime.roots.size, 1);
    assert.ok(runtime.roots.has(runtime.forms[1].chartDivId));
    secondCleanup();
    assert.equal(runtime.roots.size, 0);
});

test('Forms keep the selected form in domain options and preserve unavailable selections', async t => {
    const { scope, context, container, requests } = fixture(t, { extraWidgets: true, data: { options: [{ id: 'Contact', title: 'Contact' }] } });
    const widget = scope.getWidget('forms');
    const settings = await widget.configure({ container, context, options: { days: 30 }, domainOptions: { formName: 'Unavailable form' }, signal: new AbortController().signal });
    const value = JSON.parse(JSON.stringify(settings.read()));
    assert.deepEqual(value, { options: { days: 30 }, domainOptions: { formName: 'Unavailable form' } });
    assert.match(requests[0].url, /days=30/);
    assert.equal(requests[0].options.headers['X-CSRF-Token'], 'test-csrf-token');
});

test('Release acknowledgement folds the matching release into a visible summary that can be expanded again', async t => {
    const { scope, context, container } = fixture(t, { extraWidgets: true });
    context.labels.changelog = '<p>WebJET CMS <strong>2026.18</strong> release.</p><p>Second feature.</p>';
    context.settings.acknowledgedNewsVersion = '2026.18';
    let acknowledged;
    context.dashboard = { acknowledgeNews: async version => { acknowledged = version; return true; } };
    const widget = scope.getWidget('news');
    widget.render({ container, context });
    assert.match(container.querySelector('.md-dashboard-widget__news-summary').textContent, /2026.18/);
    assert.equal(container.querySelector('.md-dashboard-widget__news-highlights'), null);
    assert.equal(container.querySelector('button').getAttribute('aria-expanded'), 'false');
    container.querySelector('button').click();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(acknowledged, null);
    container.replaceChildren();
    context.labels.changelog = '<p>WebJET CMS <strong>2026.19</strong> release.</p>';
    widget.render({ container, context });
    assert.equal(container.querySelector('button').getAttribute('aria-expanded'), 'true');
    assert.match(container.querySelector('.md-dashboard-widget__news-highlights').textContent, /2026.19/);
    container.querySelector('button').click();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(acknowledged, '2026.19');
});

test('Visitor comparisons do not fabricate percentage growth from a zero baseline', t => {
    const { scope } = fixture(t, { extraWidgets: true });
    assert.equal(scope.change(20, 0), null);
    assert.equal(scope.change(20, undefined), null);
    assert.equal(scope.change(20, 10), '+100 %');
    assert.equal(scope.change(5, 10), '-50 %');
});

test('The mandatory sessions widget retains active login details and management even for a stored collapsed layout', async t => {
    const { scope, context, container } = fixture(t, { extraWidgets: true, data: { currentSessions: { currentSessionId: 'current', userSessions: [{ cluster: 'node1', userSessions: [
        { sessionId: 'current', logonTime: 1000, browserName: 'Browser', remoteAddr: '127.0.0.1' }
    ] }] } } });
    const widget = scope.getWidget('sessions');
    assert.equal(widget.mandatory, true);
    await widget.renderCollapsed({ container, context, instance: { collapsed: true }, signal: new AbortController().signal });
    assert.match(container.querySelector('.md-dashboard-widget__session-manage').getAttribute('aria-label'), /manageSessions.*\(1\)/);
    assert.match(container.querySelector('li').textContent, /Browser.*127.0.0.1/);
});

test('Search exposes separate scopes and changes its accessible hint', t => {
    const { scope, context, container, window } = fixture(t, { extraWidgets: true });
    scope.getWidget('search').render({ container, context, options: { scope: 'admin' }, instance: { id: 'search-one' } });
    const radios = container.querySelectorAll('[type=radio]');
    assert.equal(radios.length, 2);
    assert.ok([...radios].every(radio => radio.closest('label')?.textContent.trim()), 'Native search scopes retain their visible accessible labels.');
    assert.match(container.querySelector('[type=submit]').getAttribute('aria-label'), /searchButton/);
    assert.match(container.querySelector('[type=search]').getAttribute('aria-label'), /searchAdminHint/);
    radios[1].checked = true;
    radios[1].dispatchEvent(new window.Event('change'));
    assert.match(container.querySelector('[type=search]').getAttribute('aria-label'), /searchDocsHint/);
});


test('An HTTP 200 session-removal rejection leaves the session visible and reports failure', async t => {
    const data = { currentSessions: { currentSessionId: 'current', userSessions: [{ cluster: 'node1', userSessions: [
        { sessionId: 'other', logonTime: 1000, browserName: 'Other browser', remoteAddr: '127.0.0.1' }
    ] }] } };
    const { scope, context, container, requests } = fixture(t, { extraWidgets: true, fetchResponse: async () => ({
        ok: true, status: 200, json: async () => data, text: async () => '{success: false}'
    }) });
    let refreshed = false;
    context.dashboard = { refresh: () => { refreshed = true; } };
    await scope.getWidget('sessions').render({ container, context, instance: { collapsed: false }, signal: new AbortController().signal });
    const logout = container.querySelector('li button');
    logout.click();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(container.querySelectorAll('li').length, 1);
    assert.equal(logout.disabled, false);
    assert.match(container.querySelector('[role=alert]').textContent, /sessionError/);
    assert.equal(refreshed, false);
    assert.equal(requests[1].options.headers['X-CSRF-Token'], 'test-csrf-token');
    assert.match(requests[1].options.body.toString(), /sessionId=other/);
});

test('Active newsletter refreshes only while visible and releases its observer and timer', async t => {
    const { scope, context, container, window } = fixture(t, { extraWidgets: true, data: { active: true, items: [{
        id: 1, title: 'Campaign', status: 'sending', sent: 3, recipients: 10, failed: 1, url: '/apps/dmail/admin/?id=1'
    }] } });
    let callback, interval, disconnected = false, cleared = false, refreshed = 0;
    scope.IntersectionObserver = class {
        constructor(fn) { callback = fn; }
        observe() {}
        disconnect() { disconnected = true; }
    };
    window.setInterval = (fn, delay) => { interval = fn; assert.equal(delay, 30000); return 42; };
    window.clearInterval = id => { assert.equal(id, 42); cleared = true; };
    Object.defineProperty(window.document, 'visibilityState', { configurable: true, value: 'visible' });
    const controller = new AbortController();
    const cleanup = await scope.getWidget('newsletter').render({ container, context, instance: { size: '2x2' }, domainOptions: {}, signal: controller.signal, refresh: () => refreshed++ });
    interval(); assert.equal(refreshed, 0);
    callback([{ isIntersecting: true }]); interval(); assert.equal(refreshed, 1);
    Object.defineProperty(window.document, 'visibilityState', { configurable: true, value: 'hidden' });
    interval(); assert.equal(refreshed, 1);
    Object.defineProperty(window.document, 'visibilityState', { configurable: true, value: 'visible' });
    controller.abort(); interval(); assert.equal(refreshed, 1);
    cleanup(); assert.equal(disconnected, true); assert.equal(cleared, true);
});

test('Every active login stays visible with the current session first even when newer sessions exist', async t => {
    const { scope, context, container } = fixture(t, { extraWidgets: true, data: { currentSessions: {
        currentSessionId: 'current', userSessions: [{ cluster: 'node1', userSessions: [
            { sessionId: 'other-1', logonTime: 5000 }, { sessionId: 'other-2', logonTime: 4000 },
            { sessionId: 'other-3', logonTime: 3000 }, { sessionId: 'current', logonTime: 1000, browserName: 'Current browser' }
        ] }]
    } } });
    await scope.getWidget('sessions').render({ container, context, instance: { collapsed: false }, signal: new AbortController().signal });
    const rows = container.querySelectorAll('li');
    assert.equal(rows.length, 4);
    assert.match(rows[0].textContent, /Current browser.*currentSession/);
    assert.equal(rows[0].querySelector('button'), null);
});

test('Accepted cluster logout stays pending instead of claiming immediate invalidation', async t => {
    const data = { currentSessions: { currentSessionId: 'current', userSessions: [{ cluster: 'remote', userSessions: [
        { sessionId: 'remote-own', logonTime: 1000, browserName: 'Remote browser' }
    ] }] } };
    const { scope, context, container } = fixture(t, { extraWidgets: true, fetchResponse: async () => ({
        ok: true, status: 200, json: async () => data, text: async () => '{"success":true,"pending":true}'
    }) });
    let refreshed = false;
    context.dashboard = { refresh: () => { refreshed = true; } };
    await scope.getWidget('sessions').render({ container, context, instance: { collapsed: false }, signal: new AbortController().signal });
    container.querySelector('li button').click();
    await new Promise(resolve => setImmediate(resolve));
    assert.match(container.querySelector('li').textContent, /sessionPending/);
    assert.equal(container.querySelector('li button'), null);
    assert.equal(refreshed, false);
});


test('Release announcements preserve paragraph breaks and show a concise introduction with a full changelog link', t => {
    const { scope, context, container } = fixture(t, { extraWidgets: true });
    context.labels.changelog = 'WebJET CMS <strong>2026.18</strong> first feature.<br><br>Second feature.<br><br>Third feature.';
    const news = scope.releaseNews(context);
    assert.equal(news.paragraphs.length, 3);
    assert.equal(news.paragraphs[1], 'Second feature.');
    scope.getWidget('news').render({ container, context });
    assert.equal(container.querySelectorAll('.md-dashboard-widget__news-highlights p').length, 1);
    assert.match(container.querySelector('.md-dashboard-widget__news-highlights').textContent, /first feature/);
    assert.equal(container.querySelector('.md-dashboard-widget__news-more').getAttribute('href'), 'https://docs.webjetcms.sk/latest/en/CHANGELOG');
});
