const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

function fixture(t, { pages = [], menu = [], allowed = true, ok = true, extraWidgets = false, data = {}, actual, fetchResponse } = {}) {
    const dom = new JSDOM('<!doctype html><body><main></main></body>', { url: 'http://localhost/admin/v9/' });
    const window = dom.window;
    window.userLng = 'en';
    window.csrfToken = 'test-csrf-token';
    window.WJ = { hasPermission: () => allowed };
    const requests = [];
    const latest = data.series?.at(-1);
    const snapshot = actual || { serverActualTime: latest?.date ?? 123, memUsed: latest?.used == null ? null : latest.used * 1048576,
        memFree: latest?.free == null ? null : latest.free * 1048576, memTotal: latest?.total == null ? null : latest.total * 1048576,
        cpuUsageProcess: latest?.process ?? null, cpuUsage: latest?.system ?? null };
    const scope = vm.createContext({ window, document: window.document, Node: window.Node, DOMParser: window.DOMParser, DOMException: window.DOMException, URL, URLSearchParams, AbortController, console,
        IntersectionObserver: class { observe() {} disconnect() {} },
        fetch: async (url, options) => { requests.push({ url, options }); return fetchResponse ? fetchResponse(url, options) : { ok, status: ok ? 200 : 403, json: async () => url.includes('/monitoring/actual') ? snapshot : url.includes('/data/') ? data : pages }; }
    });
    for (const file of ['registry.js', 'widget-utils.js', 'charts.js', 'utility-widgets.js', 'data-widgets.js', 'monitoring-live.js', 'system-widgets.js', 'widgets.js']) {
        const source = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/dashboard', file), 'utf8')
            .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');
        const exports = { 'system-widgets.js': ['registerSystemWidgets'], 'monitoring-live.js': ['readMonitoringSnapshot', 'subscribeMonitoring'] }[file];
        const script = exports ? `(function () { ${source}\n${exports.map(name => `this.${name} = ${name};`).join('\n')} }).call(this);` : source;
        vm.runInContext(script, scope, { filename: file });
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
    const source = container.querySelector('[name="dashboardShortcutSource"]');
    const module = container.querySelector('[name="dashboardShortcutMenu"]');
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
    assert.deepEqual(JSON.parse(JSON.stringify(settings.read())), { options: { source: 'url', href: 'https://other.test/', title: 'My site', icon: 'ti-link', color: 'default' } });
});

const groupedShortcutMenu = [
    { text: 'Overviews', childrens: [{ text: 'Traffic', href: '/apps/stat/admin/', childrens: [
        { text: 'Visits', href: '/apps/stat/admin/' }, { text: 'Top pages', href: '/apps/stat/admin/top/' }
    ] }] },
    { text: 'Applications', childrens: [
        { text: 'Banner system', href: '/apps/banner/admin/', icon: 'ti ti-ad', childrens: [
            { text: 'Banner list', href: '/apps/banner/admin/' }, { text: 'Banner statistics', href: '/apps/banner/admin/banner-stat/' }
        ] },
        { text: 'Forms', href: '/apps/form/admin/' },
        { text: 'Unsafe', href: 'javascript:alert(1)' }
    ] }
];

test('Shortcut selection follows main areas, sections and tabs without duplicate parent destinations', t => {
    const { scope, context, container, window } = fixture(t, { menu: groupedShortcutMenu });
    const settings = scope.getWidget('shortcut').configure({ container, context, options: {} });
    const group = container.querySelector('[name="dashboardShortcutGroup"]');
    const section = container.querySelector('[name="dashboardShortcutSection"]');
    const tab = container.querySelector('[name="dashboardShortcutMenu"]');
    const select = (field, value) => { field.value = value; field.dispatchEvent(new window.Event('change')); };
    assert.deepEqual([...group.options].slice(1).map(option => option.textContent), ['Overviews', 'Applications']);
    assert.equal(group.value, '', 'Do not silently choose the first administration destination');
    assert.equal(section.disabled, true);
    assert.equal(tab.disabled, true);
    assert.throws(() => settings.read(), /shortcutChooseTarget/);
    select(group, '1');
    assert.deepEqual([...section.options].slice(1).map(option => option.textContent), ['Banner system', 'Forms']);
    select(section, '0');
    assert.deepEqual([...tab.options].slice(1).map(option => option.textContent), ['Banner list', 'Banner statistics']);
    assert.equal(tab.value, '');
    select(tab, '/apps/banner/admin/banner-stat/');
    assert.equal(settings.read().options.href, '/apps/banner/admin/banner-stat/');
    select(group, '0');
    assert.equal(section.value, '0', 'A sole authorized section needs no extra choice');
    assert.equal(tab.value, '', 'Changing the main area must discard the old tab');
    assert.throws(() => settings.read(), /shortcutChooseTarget/);
    select(group, '1');
    select(section, '1');
    assert.equal(tab.parentElement.hidden, true, 'A direct section link needs no redundant tab choice');
    assert.equal(settings.read().options.href, '/apps/form/admin/');
});

test('Editing preselects the stored hierarchy and unavailable targets cannot silently change', t => {
    const { scope, context, container, window } = fixture(t, { menu: groupedShortcutMenu });
    const original = { source: 'menu', href: '/apps/banner/admin/banner-stat/', title: 'Autotest banners', icon: 'ti-ad', color: 'default' };
    const settings = scope.getWidget('shortcut').configure({ container, context, options: original });
    assert.equal(container.querySelector('[name="dashboardShortcutGroup"]').selectedOptions[0].textContent, 'Applications');
    assert.equal(container.querySelector('[name="dashboardShortcutSection"]').selectedOptions[0].textContent, 'Banner system');
    assert.equal(container.querySelector('[name="dashboardShortcutMenu"]').selectedOptions[0].textContent, 'Banner statistics');
    assert.deepEqual(JSON.parse(JSON.stringify(settings.read().options)), original);
    const source = container.querySelector('[name="dashboardShortcutSource"]');
    source.value = 'url';
    source.dispatchEvent(new window.Event('change'));
    for (const name of ['Group', 'Section', 'Menu']) assert.equal(container.querySelector(`[name="dashboardShortcut${name}"]`).parentElement.hidden, true);
    source.value = 'menu';
    source.dispatchEvent(new window.Event('change'));
    assert.deepEqual(JSON.parse(JSON.stringify(settings.read().options)), original);
    container.replaceChildren();
    const unavailable = scope.getWidget('shortcut').configure({ container, context, options: { ...original, href: '/no-longer-authorized/' } });
    assert.equal(container.querySelector('[name="dashboardShortcutGroup"]').value, '');
    assert.throws(() => unavailable.read(), /shortcutChooseTarget/);
});

test('Empty or unsafe-only menu groups offer explicit URLs instead of empty navigation choices', t => {
    const { scope, context, container } = fixture(t, { menu: [{ text: 'Empty', childrens: [] }, { text: 'Unsafe', childrens: [{ text: 'Script', href: 'javascript:alert(1)' }] }] });
    scope.getWidget('shortcut').configure({ container, context, options: {} });
    const source = container.querySelector('[name="dashboardShortcutSource"]');
    assert.equal(source.value, 'url');
    assert.equal(source.options[0].disabled, true);
    assert.equal(container.querySelector('[name="dashboardShortcutGroup"]').parentElement.hidden, true);
});

test('Shortcut icons inherit section icons, allow overrides and follow a newly selected destination', t => {
    const { scope, context, container, window } = fixture(t, { menu: groupedShortcutMenu });
    const widget = scope.getWidget('shortcut');
    const options = { source: 'menu', href: '/apps/banner/admin/banner-stat/', title: 'Autotest', icon: 'ti-star', color: 'mint' };
    const settings = widget.configure({ container, context, options });
    const iconInput = container.querySelector('[name="dashboardShortcutIcon"]');
    const tab = container.querySelector('[name="dashboardShortcutMenu"]');
    assert.equal(iconInput.value, 'star', 'Editing preserves the personal icon');
    assert.equal(container.querySelector('.md-dashboard__shortcut-preview i').className, 'ti ti-star');
    assert.equal(container.querySelector('input[type="radio"]:checked').value, 'mint');
    tab.value = '/apps/banner/admin/';
    tab.dispatchEvent(new window.Event('change'));
    assert.equal(iconInput.value, 'ad', 'Tabs without their own icon inherit the section icon');
    iconInput.value = 'chart-bar';
    iconInput.dispatchEvent(new window.Event('input'));
    assert.equal(settings.read().options.icon, 'ti-chart-bar');
    assert.equal(settings.read().options.color, 'mint');
    container.replaceChildren();
    widget.render({ container, context, options: { href: '/apps/banner/admin/banner-stat/' } });
    assert.equal(container.querySelector('i').className, 'ti ti-ad', 'Existing shortcuts also inherit the menu icon');
    container.replaceChildren();
    widget.render({ container, context, options });
    assert.equal(container.querySelector('i').className, 'ti ti-star');
    assert.equal(container.querySelector('a').style.getPropertyValue('--wj-dashboard-shortcut-bg'), 'var(--wj-dashboard-mint)');
});

test('URL shortcuts validate icon input and use only predefined background colors', t => {
    const { scope, context, container, window } = fixture(t);
    const options = { source: 'url', href: 'https://example.com', title: 'Autotest', icon: 'ti-heart', color: 'rose' };
    const widget = scope.getWidget('shortcut');
    const settings = widget.configure({ container, context, options });
    const iconInput = container.querySelector('[name="dashboardShortcutIcon"]');
    const color = container.querySelector('input[value="lavender"]');
    color.checked = true;
    color.dispatchEvent(new window.Event('change', { bubbles: true }));
    assert.equal(settings.read().options.color, 'lavender');
    assert.equal(container.querySelector('.md-dashboard__shortcut-preview').style.getPropertyValue('--wj-dashboard-shortcut-bg'), 'var(--wj-dashboard-lavender)');
    for (const invalid of ['ti-star other-class', '<img src=x>', 'url(evil)', 'a'.repeat(81)]) {
        iconInput.value = invalid;
        assert.throws(() => settings.read(), /shortcutIconInvalid/);
    }
    iconInput.value = '';
    assert.equal(settings.read().options.icon, '', 'An empty icon restores the automatic fallback');
    container.replaceChildren();
    widget.render({ container, context, options: { ...options, icon: '<img src=x>', color: 'url(evil)' } });
    assert.equal(container.querySelector('img'), null);
    assert.equal(container.querySelector('i').className, 'ti ti-link');
    assert.equal(container.querySelector('a').style.getPropertyValue('--wj-dashboard-shortcut-bg'), 'var(--wj-dashboard-surface)');
});

test('Recent pages retain six server-filtered rows safely in every supported size', async t => {
    const pages = Array.from({ length: 8 }, (_, index) => ({ docId: index + 1, title: index ? `Page ${index}` : '<img src=x>', fullPath: '/Section', saveDate: '26.09.2026 10:00' }));
    const { scope, context, container, requests } = fixture(t, { pages });
    const widget = scope.getWidget('recent-pages');
    const signal = new AbortController().signal;
    assert.deepEqual(Array.from(widget.sizes), ['2x3', '3x2', '3x3']);
    assert.equal(widget.defaultSize, '3x2');
    for (const size of widget.sizes) {
        container.replaceChildren();
        await widget.render({ container, context, signal, instance: { size } });
        assert.equal(container.querySelectorAll('li').length, 6);
        assert.equal(container.querySelector('img'), null);
        assert.equal(container.querySelectorAll('.md-dashboard-widget__page-section').length, 6);
        assert.match(container.querySelector('.md-dashboard-widget__page-date').textContent, /26.09.2026/);
    }
    assert.equal(requests[0].options.signal, signal);
    assert.equal(requests[0].options.headers['X-CSRF-Token'], 'test-csrf-token');
});

test('Recent pages isolate native scrolling, keep links keyboard accessible and clean up on abort', async t => {
    const pages = Array.from({ length: 6 }, (_, index) => ({ docId: index + 1, title: `Page ${index}`, fullPath: '/Section' }));
    const { scope, context, container, window } = fixture(t, { pages });
    const controller = new AbortController();
    await scope.getWidget('recent-pages').render({ container, context, signal: controller.signal, instance: { size: '3x2' } });
    const list = container.querySelector('.md-dashboard-widget__pages');
    assert.equal(list.tabIndex, 0);
    assert.equal(list.getAttribute('aria-label'), 'admin.dashboard.recent-pages.js');
    assert.equal(list.querySelectorAll('a[href]').length, 6);
    let overflow = true, bubbled = 0;
    Object.defineProperty(list, 'scrollHeight', { get: () => overflow ? 400 : 200 });
    Object.defineProperty(list, 'clientHeight', { value: 200 });
    for (const type of ['wheel', 'keydown', 'touchstart', 'touchmove', 'touchend']) container.addEventListener(type, () => bubbled++);
    const wheel = new window.WheelEvent('wheel', { bubbles: true, cancelable: true, deltaY: 60 });
    list.dispatchEvent(wheel);
    list.querySelector('a').dispatchEvent(new window.KeyboardEvent('keydown', { bubbles: true, key: 'PageDown' }));
    assert.equal(bubbled, 0);
    assert.equal(wheel.defaultPrevented, false, 'The browser retains native list scrolling.');
    list.dispatchEvent(new window.Event('touchstart', { bubbles: true }));
    overflow = false;
    list.dispatchEvent(new window.Event('touchmove', { bubbles: true }));
    list.dispatchEvent(new window.Event('touchend', { bubbles: true }));
    assert.equal(bubbled, 0, 'The complete gesture stays in the list even if its content changes.');
    list.dispatchEvent(new window.WheelEvent('wheel', { bubbles: true }));
    assert.equal(bubbled, 1, 'A list that fits must allow page scrolling.');
    list.dispatchEvent(new window.KeyboardEvent('keydown', { bubbles: true, key: 'Tab' }));
    assert.equal(bubbled, 2, 'Tab navigation remains unhandled.');
    overflow = true;
    controller.abort();
    list.dispatchEvent(new window.WheelEvent('wheel', { bubbles: true }));
    assert.equal(bubbled, 3, 'Removed widgets no longer capture scroll events.');
});

test('Recent page thumbnails use supplied local perex images and restore an icon when loading fails', async t => {
    const { scope, context, container, window } = fixture(t, { pages: [
        { docId: 1, title: 'Page', fullPath: '/Section/Page', perexImage: '/images/news/photo.jpg', saveDate: '02.03.2026 14:30:22' },
        { docId: 2, title: 'External image', perexImage: '//external.test/photo.jpg' },
        { docId: 3, title: 'Executable image', perexImage: 'javascript:alert(1)' },
        { docId: 4, title: 'No image', perexImage: '' }
    ] });
    await scope.getWidget('recent-pages').render({ container, context, signal: new AbortController().signal, instance: { size: '3x3' } });
    assert.equal(container.querySelectorAll('img').length, 1);
    const thumbnail = container.querySelector('.md-dashboard-widget__page-image');
    const image = thumbnail.querySelector('img');
    assert.equal(image.alt, '');
    assert.equal(image.width, 38);
    assert.equal(image.getAttribute('src'), '/thumb/images/news/photo.jpg?w=76&h=76&ip=6');
    assert.equal(container.querySelectorAll('.md-dashboard-widget__page-image i:not([hidden])').length, 3);
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

test('New profiles include only curated widgets and no more than two authorized shortcut destinations', t => {
    const menu = [
        { text: 'Pages', href: '/admin/v9/webpages/web-pages-list/' }, { text: 'Forms', href: '/apps/form/admin/' },
        { text: 'Newsletter', href: '/apps/dmail/admin/' }, { text: 'Unsafe', href: 'javascript:alert(1)' }
    ];
    const { scope, context } = fixture(t, { menu });
    const defaults = JSON.parse(JSON.stringify(scope.getDashboardDefaults(context)));
    assert.deepEqual(defaults.map(item => item.type), [
        'search', 'sessions', 'news', 'traffic', 'forms', 'approvals', 'errors',
        'recent-pages', 'referrers', 'publishing', 'newsletter', 'shortcut', 'shortcut'
    ]);
    assert.deepEqual(defaults.filter(item => item.type === 'shortcut').map(item => item.options.href), menu.slice(0, 2).map(item => item.href));
    const catalogue = Array.from(scope.listWidgets()).filter(widget => !widget.isAvailable || widget.isAvailable(context));
    for (const type of ['search-terms', 'top-pages', ...Object.keys(migratedPermissions)]) {
        assert.ok(catalogue.some(widget => widget.type === type), `${type} remains available for manual addition`);
    }
});

test('Default shortcuts fall back to the first authorized module when pages and forms are unavailable', t => {
    const { scope, context } = fixture(t, { menu: [
        { text: 'External', href: 'https://other.test/' }, { text: 'Newsletter', href: '/apps/dmail/admin/' },
        { text: 'Statistics', href: '/apps/stat/admin/' }
    ] });
    const shortcuts = JSON.parse(JSON.stringify(scope.getDashboardDefaults(context))).filter(item => item.type === 'shortcut');
    assert.deepEqual(shortcuts.map(item => item.options.href), ['/apps/dmail/admin/']);
});

test('Default content previews complete the traffic row and use an even three-card second row', t => {
    const { scope, context } = fixture(t);
    const defaults = JSON.parse(JSON.stringify(scope.getDashboardDefaults(context)));
    const grid = defaults.filter(item => !['search', 'sessions', 'news', 'shortcut'].includes(item.type));
    assert.deepEqual(grid, [
        { type: 'traffic', size: '3x3' }, { type: 'forms', size: '1x1' },
        { type: 'approvals', size: '1x1' }, { type: 'errors', size: '1x1' },
        { type: 'recent-pages', size: '3x2' }, { type: 'referrers', size: '2x2' },
        { type: 'publishing', size: '2x2' }, { type: 'newsletter', size: '2x2' }
    ]);
    for (const item of grid) assert.ok(scope.getWidget(item.type).sizes.includes(item.size), item.type);
});

const migratedPermissions = {
    'changed-pages': 'menuWebpages', audit: 'cmp_adminlog', 'logged-admins': 'welcomeShowLoggedAdmins',
    'server-memory': 'cmp_server_monitoring', 'server-cpu': 'cmp_server_monitoring'
};

test('Optional system widgets follow their exact permissions without joining the default layout', t => {
    const { scope, context, window } = fixture(t);
    for (const permission of new Set(Object.values(migratedPermissions))) {
        window.WJ.hasPermission = value => value === permission;
        for (const [type, required] of Object.entries(migratedPermissions)) {
            assert.equal(scope.getWidget(type).isAvailable(context), permission === required, `${type} requires ${required}`);
            assert.equal(scope.getWidget(type).multiple, true);
        }
        const visibleDefaults = Array.from(scope.getDashboardDefaults(context), item => item.type);
        for (const type of Object.keys(migratedPermissions)) assert.equal(visibleDefaults.includes(type), false);
    }
});

test('Changed pages and audit render bounded text-only activity with their supplied destinations and dates', async t => {
    const items = Array.from({ length: 6 }, (_, index) => ({
        title: '<img src=x onerror=alert(1)>', type: '<script>audit()</script>', description: '<b>Changed setting</b>',
        fullPath: '/Section', userFullName: '<svg onload=alert(1)>', date: Date.UTC(2026, 8, 26, 10),
        url: index === 0 ? '/admin/v9/webpages/web-pages-list/?docid=12' : 'javascript:alert(1)'
    }));
    const { scope, context, container, requests } = fixture(t, { data: { items } });
    const signal = new AbortController().signal;
    for (const type of ['changed-pages', 'audit']) {
        const widget = scope.getWidget(type);
        for (const [size, count] of [['3x2', 2], ['3x3', 4]]) {
            container.replaceChildren();
            await widget.render({ container, context, signal, instance: { size }, options: { arbitrary: 'autotest' } });
            assert.equal(container.querySelectorAll('.md-dashboard-widget__activity > li').length, count);
            assert.equal(container.querySelectorAll('a[href]').length, 1);
            assert.equal(container.querySelector('a').getAttribute('href'), items[0].url);
            assert.equal(container.querySelector('script,img,svg,b'), null);
            assert.match(container.textContent, /<svg onload=alert\(1\)>/);
            assert.match(container.querySelector('.md-dashboard-widget__activity-detail').textContent, /2026/);
            assert.match(container.textContent, type === 'audit' ? /<b>Changed setting<\/b>/ : /<img src=x onerror=alert\(1\)>/);
        }
    }
    for (const request of requests) {
        assert.equal(request.options.signal, signal);
        assert.equal(request.options.headers['X-CSRF-Token'], 'test-csrf-token');
        assert.equal(new URL(request.url, 'http://localhost').search, '', 'Activity requests must not forward stored arbitrary options.');
    }
});

test('Logged administrators retain every authorized name with safe email actions in both sizes', async t => {
    const emails = ['valid+autotest@example.com', 'autotest@example.com?bcc=other@example.com', 'autotest@example.com\r\nBcc:other@example.com',
        'autotest@example.com,other@example.com', 'autotest@example.com%0aBcc:other@example.com', ''];
    const items = emails.map((email, userId) => ({ userId, email, fullName: '<img src=x onerror=alert(1)>' }));
    const { scope, context, container, window } = fixture(t, { data: { items, total: items.length } });
    context.translate = (key, ...values) => `${key}:${values.join(',')}`;
    const widget = scope.getWidget('logged-admins');
    const controller = new AbortController();
    const args = { container, context, signal: controller.signal };
    for (const size of ['2x2', '2x3']) {
        container.replaceChildren();
        await widget.render({ ...args, instance: { size } });
        assert.equal(container.querySelectorAll('.md-dashboard-widget__admins > li').length, items.length);
        assert.equal(container.querySelector('img'), null);
        assert.equal(container.querySelectorAll('a').length, 1);
        assert.equal(container.querySelector('a').getAttribute('href'), 'mailto:valid%2Bautotest@example.com');
        assert.match(container.querySelector('a').getAttribute('aria-label'), /<img src=x onerror=alert\(1\)>/);
        assert.match(container.querySelector('.md-dashboard-widget__footnote').textContent, /:6$/);
    }
    const list = container.querySelector('.md-dashboard-widget__admins');
    assert.equal(list.tabIndex, 0);
    assert.match(list.getAttribute('aria-label'), /logged-admins/);
    Object.defineProperty(list, 'scrollHeight', { value: 400 });
    Object.defineProperty(list, 'clientHeight', { value: 200 });
    let bubbled = 0;
    container.addEventListener('wheel', () => bubbled++);
    list.dispatchEvent(new window.WheelEvent('wheel', { bubbles: true }));
    assert.equal(bubbled, 0);
    controller.abort();
    list.dispatchEvent(new window.WheelEvent('wheel', { bubbles: true }));
    assert.equal(bubbled, 1, 'Removed administrator cards must release their native-scroll listeners.');
});

test('Migrated provider failures stay errors and aborted responses do not append stale content', async t => {
    for (const type of Object.keys(migratedPermissions)) {
        const denied = fixture(t, { ok: false });
        const args = { container: denied.container, context: denied.context, signal: new AbortController().signal, instance: { size: '3x3' } };
        await assert.rejects(denied.scope.getWidget(type).render(args), /403/, type);
        assert.equal(denied.container.textContent, '');
        let finish;
        const aborted = fixture(t, { fetchResponse: () => new Promise(resolve => { finish = resolve; }) });
        const controller = new AbortController();
        const rendering = aborted.scope.getWidget(type).render({ ...args, container: aborted.container, context: aborted.context, signal: controller.signal });
        controller.abort();
        finish({ ok: true, json: async () => ({ items: [{ title: 'Stale', fullName: 'Stale' }], series: [{ date: 123, used: 7 }], total: 1 }) });
        if (type.startsWith('server-')) await assert.rejects(rendering, error => error.name === 'AbortError');
        else await rendering;
        assert.equal(aborted.container.textContent, '', type);
        assert.equal(aborted.requests[0].options.signal.aborted, true);
    }
});

test('Ranked previews keep numeric columns marked and navigate through their headers in both sizes', async t => {
    const { scope, context, container } = fixture(t, { data: { total: 12, items: [
        { title: '<b>Long title</b>', section: '/Section/subsection', value: 12, previous: 24, url: '/apps/stat/admin/' }
    ] } });
    for (const type of ['search-terms', 'top-pages']) {
        const widget = scope.getWidget(type);
        assert.equal(widget.defaultSize, '3x3');
        assert.equal(widget.headerLink.href, type === 'top-pages' ? '/apps/stat/admin/top/' : '/apps/stat/admin/search-engines/');
        for (const size of ['2x3', '3x3']) {
            container.replaceChildren();
            const args = { container, context, instance: { type, size }, options: {}, domainOptions: {}, signal: new AbortController().signal };
            await widget.render(args);
            const numeric = type === 'top-pages' ? 2 : 1;
            assert.equal(container.querySelectorAll('th.md-dashboard-widget__table-number').length, numeric);
            assert.equal(container.querySelectorAll('td.md-dashboard-widget__table-number').length, numeric);
            assert.equal(container.querySelector(type === 'top-pages' ? '.md-dashboard-widget__page-title' : 'td').textContent, '<b>Long title</b>');
            assert.equal(container.querySelector('b'), null);
            assert.equal(container.querySelector('.md-dashboard-widget__more'), null);
        }
    }
});

test('Top pages combine safe thumbnails and parent paths while retaining statistics destinations and metrics', async t => {
    const { scope, context, container, window } = fixture(t, { data: { total: 1248, items: [
        { title: 'Page', section: '/News/Page', perexImage: '/images/news/photo.jpg', value: 1248, previous: 624, url: '/apps/stat/admin/top-details/?docId=12&dateRange=week' },
        { title: 'No image', section: '/News/No image', perexImage: '', value: 5, previous: 0, url: '/apps/stat/admin/top-details/?docId=13' },
        { title: 'Unsafe image', perexImage: '//external.test/photo.jpg', value: 2, previous: 4, url: '/apps/stat/admin/top/' }
    ] } });
    for (const size of ['2x3', '3x3']) {
        container.replaceChildren();
        await scope.getWidget('top-pages').render({ container, context, instance: { type: 'top-pages', size }, options: {}, signal: new AbortController().signal });
        assert.equal(container.querySelectorAll('thead th').length, 3);
        const rows = container.querySelectorAll('tbody tr');
        assert.equal(rows[0].querySelector('.md-dashboard-widget__page-preview').getAttribute('href'), '/apps/stat/admin/top-details/?docId=12&dateRange=week');
        assert.equal(rows[0].querySelector('.md-dashboard-widget__page-section').textContent, '/News');
        assert.equal(rows[0].querySelectorAll('td')[2].textContent, '+100 %');
        assert.equal(rows[1].querySelectorAll('td')[2].textContent, '—');
        assert.equal(container.querySelectorAll('img').length, 1);
        const image = rows[0].querySelector('img');
        assert.equal(image.getAttribute('src'), '/thumb/images/news/photo.jpg?w=76&h=76&ip=6');
        assert.equal(image.alt, '');
        assert.equal(rows[1].querySelector('.ti-file-text').hidden, false);
        image.dispatchEvent(new window.Event('error'));
        assert.equal(rows[0].querySelector('img'), null);
        assert.equal(rows[0].querySelector('.ti-file-text').hidden, false);
    }
});

function chartRuntime(window, { load = async () => {}, create } = {}) {
    const forms = [];
    const roots = new Set();
    const destroyed = [];
    const settings = (initial = {}) => ({
        values: { ...initial },
        adapters: { callbacks: {}, add(name, callback) { this.callbacks[name] = callback; }, remove(name) { delete this.callbacks[name]; } },
        set(key, value) { this.values[key] = value; },
        setAll(values) { Object.assign(this.values, values); },
        get(key) { return this.values[key]; },
        dispose() { this.disposed = true; },
        appear(duration) { this.appearanceDuration = duration; }
    });
    const list = values => ({ values, getIndex: index => values[index], each: callback => values.forEach(callback), unshift: value => { values.unshift(value); return value; } });
    const axis = () => settings({ tooltip: settings(), renderer: Object.assign(settings(), { labels: { template: settings() }, grid: { template: settings() } }) });
    class LineChartForm { constructor(config) { Object.assign(this, config); } }
    class BarChartForm { constructor(config) { Object.assign(this, config); } }
    const makeChart = form => {
        const series = Array.from({ length: form instanceof LineChartForm ? form.chartData.size : 1 }, (_, index) => {
            const tooltip = Object.assign(settings(), { label: settings() });
            return Object.assign(settings({ tooltip }), { strokes: { template: settings() }, fills: { template: settings() }, columns: { template: settings() }, bullets: [], data: {
                values: form instanceof LineChartForm ? Array.from(form.chartData.values())[index] : form.chartData,
                setAll(values) { this.values = values; }
            } });
        });
        const chart = Object.assign(settings({ cursor: Object.assign(settings(), { lineY: settings() }), scrollbarX: settings(), scrollbarY: settings(), colors: settings() }), {
            root: { container: { width: () => 480, height: () => 260 }, setThemes(themes) { this.themes = themes; } },
            series: list(series), xAxes: list([axis()]), yAxes: list([axis()]),
            children: list([settings({ verticalScrollbar: settings() })]), zoomOutButton: settings()
        });
        form.chart = chart;
        roots.add(form.chartDivId);
    };
    let loads = 0;
    window.initAmcharts = async () => { loads++; await load(); };
    window.am5 = { percent: value => value, color: value => value };
    window.am5xy = { ColumnSeries: { new: () => Object.assign(settings(), { columns: { template: settings() }, bullets: [], data: { setAll(values) { this.values = values; } } }) } };
    window.WebjetTheme = { new: () => ({ theme: 'WebJET' }) };
    window.ChartTools = {
        LineChartForm, BarChartForm, DateType: { Days: 'day', Seconds: 'second' },
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

test('Monitoring cards preserve timestamped measurements, units, unavailable readings and chart cleanup', async t => {
    const series = [
        { date: Date.UTC(2026, 8, 26, 10, 0, 31), used: 520, free: 248, total: 768, process: 0, system: null }
    ];
    for (const type of ['server-memory', 'server-cpu']) {
        const { scope, context, container, window } = fixture(t, { data: { series } });
        const runtime = chartRuntime(window);
        const controller = new AbortController();
        const cleanup = await scope.getWidget(type).render({ container, context, signal: controller.signal });
        const form = runtime.forms[0];
        assert.equal(form.dateType, window.ChartTools.DateType.Seconds);
        assert.equal(form.xAxeName, 'date');
        assert.deepEqual(JSON.parse(JSON.stringify(form.chart.xAxes.getIndex(0).get('baseInterval'))), { timeUnit: 'second', count: 5 });
        const metrics = type === 'server-memory' ? ['used', 'free', 'total'] : ['process', 'system'];
        const charts = Array.from(form.chartData.values());
        assert.equal(charts.length, metrics.length);
        for (const [index, metric] of metrics.entries()) {
            assert.deepEqual(Array.from(charts[index], point => ({ date: point.date, value: point.value })), series.map(point => ({ date: point.date, value: point[metric] })));
        }
        assert.equal(container.querySelectorAll('.visually-hidden table tbody tr').length, series.length);
        assert.match(container.querySelector('.md-dashboard-widget__monitoring-values').textContent, type === 'server-memory' ? /MB/ : /%/);
        assert.match(container.querySelector('.md-dashboard-widget__chart').getAttribute('aria-label'), type === 'server-memory' ? /MB/ : /%/);
        const tooltipBounds = form.chart.xAxes.getIndex(0).get('tooltip').adapters.callbacks.bounds;
        assert.deepEqual(JSON.parse(JSON.stringify(tooltipBounds())), { left: 1, top: 1, right: 479, bottom: 259 });
        form.chart.root.container.height = () => 180;
        assert.equal(tooltipBounds().bottom, 179, 'Date tooltip bounds must follow the actual chart height after resizing.');
        form.chart.series.each(line => {
            const tooltip = line.get('tooltip');
            assert.equal(tooltip.label.get('role'), 'presentation', 'Canvas tooltips must not create duplicate DOM labels with unresolved template values.');
            assert.equal(tooltip.label.get('ariaHidden'), true);
            assert.doesNotMatch(tooltip.get('labelAriaLabel'), /\[bold\]/);
        });
        if (type === 'server-cpu') {
            assert.match(container.querySelector('.visually-hidden table').textContent, /—/);
            assert.equal(form.chart.yAxes.getIndex(0).get('max'), 100);
        }
        controller.abort();
        cleanup();
        assert.equal(runtime.roots.size, 0);
        assert.equal(runtime.destroyed.length, 1, 'Abort and cleanup must share idempotent disposal.');
    }
});

function monitoringClock(scope, window) {
    const timers = new Map();
    const observers = [];
    let sequence = 0, visibility = 'visible';
    Object.defineProperty(window.document, 'visibilityState', { get: () => visibility });
    window.setTimeout = (callback, delay) => { const id = ++sequence; timers.set(id, { callback, delay }); return id; };
    window.clearTimeout = id => timers.delete(id);
    scope.IntersectionObserver = class {
        constructor(callback) { this.callback = callback; observers.push(this); }
        observe(container) { this.container = container; }
        disconnect() { this.disconnected = true; }
    };
    return {
        timers, observers,
        tick() {
            const [id, timer] = timers.entries().next().value || [];
            assert.ok(timer, 'A visible monitoring subscription must schedule a refresh.');
            timers.delete(id);
            return timer.callback();
        },
        visibility(value) { visibility = value; window.document.dispatchEvent(new window.Event('visibilitychange')); },
        visible(container, value) { observers.find(observer => observer.container === container).callback([{ isIntersecting: value }]); }
    };
}

test('Live monitoring starts without history and updates both existing charts, values and bounded tables from shared snapshots', async t => {
    let sample = 0;
    const { scope, context, container, window, requests } = fixture(t, { fetchResponse: async () => ({ ok: true, json: async () => ({
        serverActualTime: 1789900000000 + sample * 5000, memUsed: (128 + sample) * 1048576, memFree: (384 - sample) * 1048576,
        memTotal: 512 * 1048576, cpuUsageProcess: sample, cpuUsage: sample + 10
    }) }) });
    const clock = monitoringClock(scope, window);
    const runtime = chartRuntime(window);
    const cpu = window.document.createElement('section');
    window.document.body.append(cpu);
    const memoryController = new AbortController(), cpuController = new AbortController();
    const cleanup = await Promise.all([
        scope.getWidget('server-memory').render({ container, context, signal: memoryController.signal }),
        scope.getWidget('server-cpu').render({ container: cpu, context, signal: cpuController.signal })
    ]);
    assert.equal(requests.length, 1, 'Both initial cards must share the actual snapshot request.');
    assert.equal(requests[0].url, '/admin/rest/monitoring/actual');
    assert.equal(requests[0].options.headers['X-CSRF-Token'], 'test-csrf-token');
    const reading = container.querySelector('dd');
    const chart = container.querySelector('.md-dashboard-widget__chart');
    assert.equal(reading.textContent, '128 MB');
    assert.equal(container.querySelectorAll('tbody tr').length, 1, 'A single actual sample must render without historical records.');
    assert.equal(clock.timers.size, 1);
    assert.equal([...clock.timers.values()][0].delay, 5000);
    sample++;
    await clock.tick();
    assert.equal(container.querySelector('dd'), reading, 'Current readings update without replacing their DOM node.');
    assert.equal(reading.textContent, '129 MB');
    assert.equal(cpu.querySelector('dd').textContent, '1 %');
    assert.equal(container.querySelector('.md-dashboard-widget__chart'), chart);
    assert.equal(runtime.forms.length, 2, 'Live samples must update the two existing roots rather than allocate new charts.');
    assert.deepEqual(Array.from(runtime.forms[0].chart.series.getIndex(0).data.values, point => point.value), [128, 129]);
    assert.equal(container.querySelectorAll('tbody tr').length, 2);
    await clock.tick();
    assert.equal(container.querySelectorAll('tbody tr').length, 2, 'A repeated server timestamp replaces its current sample.');
    for (sample = 2; sample <= 101; sample++) await clock.tick();
    assert.equal(container.querySelectorAll('tbody tr').length, 100, 'A long-lived overview must bound its sample history.');
    assert.equal(runtime.forms[0].chart.series.getIndex(0).data.values.length, 100);
    assert.equal(reading.textContent, '229 MB');
    memoryController.abort(); cleanup[0]();
    assert.equal(runtime.roots.size, 1);
    cpuController.abort(); cleanup[1]();
    assert.equal(runtime.roots.size, 0);
    assert.equal(clock.timers.size, 0, 'Removing both monitoring cards must cancel the shared refresh timer.');
    assert.ok(clock.observers.every(observer => observer.disconnected));
});

test('Shared monitoring requests do not overlap and release the final request when every consumer is aborted', async t => {
    let finish;
    const { scope, container, window, requests } = fixture(t, { fetchResponse: () => new Promise(resolve => { finish = resolve; }) });
    const first = new AbortController(), second = new AbortController();
    const initial = scope.readMonitoringSnapshot(first.signal);
    const shared = scope.readMonitoringSnapshot(second.signal);
    assert.equal(requests.length, 1);
    first.abort();
    await assert.rejects(initial, error => error.name === 'AbortError');
    assert.equal(requests[0].options.signal.aborted, false, 'One canceled card must not abort another card’s snapshot.');
    finish({ ok: true, json: async () => ({ serverActualTime: 1 }) });
    assert.equal((await shared).serverActualTime, 1);

    const clock = monitoringClock(scope, window);
    const controller = new AbortController();
    let updates = 0;
    scope.subscribeMonitoring(container, controller.signal, () => updates++, () => {});
    const pending = clock.tick();
    assert.equal(requests.length, 2);
    assert.equal(clock.timers.size, 0, 'The next tick must be scheduled only after its current response completes.');
    clock.visible(container, false);
    clock.visible(container, true);
    assert.equal(clock.timers.size, 0, 'Visibility changes must not create an overlapping request.');
    controller.abort();
    assert.equal(requests[1].options.signal.aborted, true);
    finish({ ok: true, json: async () => ({ serverActualTime: 2 }) });
    await pending;
    assert.equal(updates, 0, 'A late response must not update a removed widget.');
    assert.equal(clock.timers.size, 0);
});

test('Monitoring suspends hidden pages, resumes visible cards and reports failures without erasing current data', async t => {
    let failing = false, value = 1;
    const { scope, context, container, window } = fixture(t, { fetchResponse: async () => ({ ok: !failing, status: failing ? 503 : 200,
        json: async () => ({ serverActualTime: value * 5000, memUsed: value * 1048576, memFree: 512 * 1048576, memTotal: 1024 * 1048576 }) }) });
    const clock = monitoringClock(scope, window);
    chartRuntime(window);
    const controller = new AbortController();
    const cleanup = await scope.getWidget('server-memory').render({ container, context, signal: controller.signal });
    clock.visibility('hidden');
    assert.equal(clock.timers.size, 0);
    clock.visibility('visible');
    assert.equal([...clock.timers.values()][0].delay, 0);
    failing = true;
    await clock.tick();
    assert.equal(container.querySelector('dd').textContent, '1 MB');
    assert.equal(container.querySelector('[role="status"]').hidden, false);
    failing = false; value = 2;
    await clock.tick();
    assert.equal(container.querySelector('dd').textContent, '2 MB');
    assert.equal(container.querySelector('[role="status"]').hidden, true);
    clock.visible(container, false);
    assert.equal(clock.timers.size, 0);
    controller.abort(); cleanup();
});

test('Monitoring cards aborted during lazy bundle loading never create detached chart roots', async t => {
    const { scope, context, container, window } = fixture(t, { data: { series: [{ date: 123, used: 64, free: 64, total: 128 }] } });
    let finishLoad;
    const runtime = chartRuntime(window, { load: () => new Promise(resolve => { finishLoad = resolve; }) });
    const controller = new AbortController();
    const rendering = scope.getWidget('server-memory').render({ container, context, signal: controller.signal });
    await new Promise(resolve => setImmediate(resolve));
    controller.abort();
    finishLoad();
    await rendering;
    assert.equal(runtime.forms.length, 0);
    assert.equal(runtime.roots.size, 0);
});

test('Traffic descriptions follow the selected metric and period and preserve cross-year dates', async t => {
    const data = { ...trafficData, metric: 'uniqueUsers', from: Date.UTC(2025, 11, 20), to: Date.UTC(2026, 0, 18) };
    const { scope, context, container } = fixture(t, { data });
    context.translate = (key, days) => `${key}:${days}`;
    await scope.getWidget('traffic').render({ container, context, options: { days: 30 }, instance: { size: '1x1' }, signal: new AbortController().signal });
    assert.equal(container.querySelector('.md-dashboard-widget__metric-label').textContent, 'admin.dashboard.trafficUsers.js:30');
    assert.equal(container.querySelector('.md-dashboard-widget__comparison-label').textContent, 'admin.dashboard.trafficComparison.js:30');
    assert.match(container.querySelector('.md-dashboard-widget__period').textContent, /2025/);
    assert.match(container.querySelector('.md-dashboard-widget__period').textContent, /2026/);
    assert.equal(container.querySelector('.md-dashboard-widget__more'), null);
});

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
    assert.equal(container.querySelector('details'), null);
    assert.equal(container.querySelectorAll('.visually-hidden tbody tr').length, 2);
    assert.match(container.querySelector('.visually-hidden').textContent, /9\/22\/2026/);
    assert.equal(container.querySelector('.md-dashboard-widget__more'), null);
    assert.equal(form.chart.series.getIndex(0).get('tooltip').label.get('ariaHidden'), true);
    assert.match(container.querySelector('.md-dashboard-widget__chart-key--current').textContent, /24/);
    assert.match(container.querySelector('.md-dashboard-widget__chart-key--previous').textContent, /22/);
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
    assert.equal(form.chart.series.getIndex(1).get('tooltip').label.get('ignoreFormatting'), true);
    assert.equal(form.chart.xAxes.getIndex(0).get('max'), 100);
    assert.equal(container.querySelector('img'), null);
    assert.equal(container.querySelector('details'), null);
    assert.match(container.querySelector('.visually-hidden').textContent, /<img src=x>\[bold\]/);
    assert.equal(container.querySelectorAll('.visually-hidden tbody tr').length, 6);
    cleanup();
    assert.equal(runtime.roots.size, 0);
});

test('Compact referrers retain shared horizontal charts, text values, and abort cleanup', async t => {
    const data = { total: 200, items: Array.from({ length: 6 }, (_, index) => ({ title: `Source ${index}`, value: 20 - index })) };
    const { scope, context, container, window } = fixture(t, { data });
    const runtime = chartRuntime(window);
    const controller = new AbortController();
    const cleanup = await scope.getWidget('referrers').render({ container, context, options: {}, instance: { size: '2x2' }, signal: controller.signal });
    assert.ok(runtime.forms[0] instanceof window.ChartTools.BarChartForm);
    assert.equal(runtime.forms[0].chartData.length, 3);
    assert.equal(runtime.forms[0].chartData[0].share, '10 %');
    assert.equal(runtime.forms[0].chartData[0].percentage, 10);
    assert.ok(container.querySelector('.md-dashboard-widget__chart--referrers'));
    assert.equal(container.querySelectorAll('.visually-hidden tbody tr').length, 3);
    controller.abort();
    cleanup();
    assert.equal(runtime.roots.size, 0);
    assert.equal(runtime.destroyed.length, 1);
});

test('Publishing calendars retain full years and distinguish publication from expiration', async t => {
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
    assert.equal(rows[1].querySelector('.md-dashboard-widget__publication-year').textContent, '2027');
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
    assert.match(container.querySelector('.md-dashboard-widget__newsletter-status').textContent, /sendingCompleted/);
    assert.equal(container.querySelector('.md-dashboard-widget__newsletter-percent').textContent, '99 %');
    assert.equal(container.querySelector('progress').max, 100);
    assert.equal(container.querySelector('progress').value, 99);
    assert.match(container.querySelector('.md-dashboard-widget__newsletter-details').textContent, /opened.*12.*clicked.*2/);
});

test('Numeric and empty traffic previews do not initialize charts', async t => {
    const { scope, context, container, window } = fixture(t, { data: { ...trafficData, series: [] } });
    const runtime = chartRuntime(window);
    const args = { container, context, options: {}, instance: { size: '1x1', type: 'traffic' }, signal: new AbortController().signal };
    const widget = scope.getWidget('traffic');
    await widget.render(args);
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

test('Compact forms describe the selected period and retain its exact accessible dates', async t => {
    const { scope, context, container } = fixture(t, { data: { total: 128, from: Date.UTC(2026, 8, 20), to: Date.UTC(2026, 8, 26), items: [] } });
    context.translate = (key, days) => key.endsWith('formSubmissionsPeriod.js') ? `submissions · ${days} days` : key;
    const widget = scope.getWidget('forms');
    for (const days of [7, 30, 90]) {
        container.replaceChildren();
        await widget.render({ container, context, options: { days }, domainOptions: {}, instance: { size: '1x1' }, signal: new AbortController().signal });
        assert.equal(container.querySelector('.md-dashboard-widget__metric-label').textContent, `submissions · ${days} days`);
        assert.ok(container.querySelector('.md-dashboard-widget__period.visually-hidden').textContent.includes('2026'));
        assert.equal(container.querySelector('.md-dashboard-widget__more'), null);
    }
    assert.equal(widget.headerLink.href({ id: 'forms' }, context), '/apps/form/admin/');
    context.settings.domainOptions = { forms: { formName: 'Contact / EN' } };
    assert.equal(widget.headerLink.href({ id: 'forms' }, context), '/apps/form/admin/detail/?formName=Contact%20%2F%20EN');
    assert.equal(scope.getWidget('approvals').headerLink.href, '/admin/v9/webpages/web-pages-list/?show=toapprove');
    assert.equal(scope.getWidget('errors').headerLink.href, '/apps/stat/admin/error/');
});

test('Publishing navigation respects the separate audit permission', t => {
    const { scope, window } = fixture(t);
    const widget = scope.getWidget('publishing');
    assert.equal(widget.headerLink.href(), '/admin/v9/apps/audit-awaiting-publish-webpages/');
    window.WJ.hasPermission = permission => permission === 'menuWebpages';
    assert.equal(widget.isAvailable(), true);
    assert.equal(widget.headerLink.href(), '/admin/v9/webpages/web-pages-list/');
});

test('Compact error totals keep their actual weekly coverage without an extra visible explanation row', async t => {
    const { scope, context, container } = fixture(t, { data: { total: 77, from: Date.UTC(2026, 8, 14), to: Date.UTC(2026, 8, 26), granularity: 'week', items: [] } });
    await scope.getWidget('errors').render({ container, context, options: { days: 7 }, instance: { size: '1x1' }, signal: new AbortController().signal });
    const period = container.querySelector('.md-dashboard-widget__period');
    assert.match(period.title, /2026/);
    assert.match(period.title, /weeklyRequests/);
    assert.match(period.querySelector('.visually-hidden').textContent, /weeklyRequests/);
    assert.equal(container.querySelectorAll('p').length, 1);
    assert.equal(container.querySelector('.md-dashboard-widget__more'), null);
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

test('The mandatory sessions widget retains active login details and a static count', async t => {
    const { scope, context, container } = fixture(t, { extraWidgets: true, data: { currentSessions: { currentSessionId: 'current', userSessions: [{ cluster: 'node1', userSessions: [
        { sessionId: 'current', logonTime: 1000, browserName: 'Browser', remoteAddr: '127.0.0.1' }
    ] }] } } });
    const widget = scope.getWidget('sessions');
    assert.equal(widget.mandatory, true);
    await widget.render({ container, context, signal: new AbortController().signal });
    assert.equal(container.querySelector('span.md-dashboard-widget__session-count').textContent, '1');
    assert.equal(container.querySelector('button'), null);
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
    await scope.getWidget('sessions').render({ container, context, signal: new AbortController().signal });
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
    await scope.getWidget('sessions').render({ container, context, signal: new AbortController().signal });
    const rows = container.querySelectorAll('li');
    assert.equal(rows.length, 4);
    assert.match(rows[0].textContent, /Current browser/);
    assert.match(rows[0].querySelector('.md-dashboard-widget__session-current').getAttribute('aria-label'), /currentSession/);
    assert.equal(rows[0].querySelector('button'), null);
});

test('Session rows use installed browser icons and labeled compact status and logout controls', async t => {
    const names = ['Chrome 153', 'HeadlessChrome 131', 'Safari 18', 'Firefox 131', 'Microsoft Edge 131', '<img src=x>'];
    const { scope, context, container } = fixture(t, { extraWidgets: true, data: { currentSessions: {
        currentSessionId: 'browser-0', userSessions: [{ userSessions: names.map((browserName, index) => ({ sessionId: `browser-${index}`, browserName, logonTime: 6000 - index })) }]
    } } });
    await scope.getWidget('sessions').render({ container, context, signal: new AbortController().signal });
    assert.deepEqual([...container.querySelectorAll('.md-dashboard-widget__session-device')].map(icon => icon.classList[1]),
        ['ti-brand-chrome', 'ti-brand-chrome', 'ti-brand-safari', 'ti-brand-firefox', 'ti-brand-edge', 'ti-device-desktop']);
    const status = container.querySelector('.md-dashboard-widget__session-current');
    assert.equal(status.tabIndex, 0);
    assert.equal(status.getAttribute('title'), status.getAttribute('aria-label'));
    const logout = container.querySelector('.md-dashboard-widget__session-logout');
    assert.match(logout.getAttribute('aria-label'), /logoutSession/);
    assert.ok(logout.querySelector('.ti-logout'));
    assert.equal(container.querySelector('img'), null);
});

test('Native session scrolling contains wheel, touch and keyboard events and releases listeners on abort', async t => {
    const { scope, context, container, window } = fixture(t, { extraWidgets: true, data: { currentSessions: {
        currentSessionId: 'current', userSessions: [{ userSessions: [{ sessionId: 'other', browserName: 'Chrome' }] }]
    } } });
    const controller = new AbortController();
    await scope.getWidget('sessions').render({ container, context, signal: controller.signal });
    const list = container.querySelector('ul');
    let overflow = true, bubbled = 0;
    Object.defineProperty(list, 'scrollHeight', { get: () => overflow ? 300 : 100 });
    Object.defineProperty(list, 'clientHeight', { value: 100 });
    for (const type of ['wheel', 'keydown', 'touchstart', 'touchmove', 'touchend']) container.addEventListener(type, () => bubbled++);
    const wheel = new window.WheelEvent('wheel', { bubbles: true, cancelable: true, deltaY: 60 });
    list.dispatchEvent(wheel);
    list.dispatchEvent(new window.KeyboardEvent('keydown', { bubbles: true, key: 'ArrowDown' }));
    assert.equal(bubbled, 0);
    assert.equal(wheel.defaultPrevented, false, 'The browser must retain native scrolling.');
    list.dispatchEvent(new window.Event('touchstart', { bubbles: true }));
    overflow = false;
    list.dispatchEvent(new window.Event('touchmove', { bubbles: true }));
    list.dispatchEvent(new window.Event('touchend', { bubbles: true }));
    list.querySelector('button').dispatchEvent(new window.KeyboardEvent('keydown', { bubbles: true, key: ' ' }));
    assert.equal(bubbled, 0, 'The complete native gesture and button activation stay isolated.');
    list.dispatchEvent(new window.WheelEvent('wheel', { bubbles: true }));
    assert.equal(bubbled, 1, 'An unscrollable list must not trap page scrolling.');
    overflow = true;
    controller.abort();
    list.dispatchEvent(new window.WheelEvent('wheel', { bubbles: true }));
    assert.equal(bubbled, 2);
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
    await scope.getWidget('sessions').render({ container, context, signal: new AbortController().signal });
    container.querySelector('li button').click();
    await new Promise(resolve => setImmediate(resolve));
    assert.match(container.querySelector('li').textContent, /sessionPending/);
    assert.equal(container.querySelector('li button'), null);
    assert.equal(refreshed, false);
});


test('Release announcements preserve every Markdown-rendered feature and place collapse beside the changelog link', t => {
    const { scope, context, container } = fixture(t, { extraWidgets: true });
    context.labels.changelog = 'WebJET CMS <strong>2026.18</strong> first feature.<br><br>Second feature.<br><br>Third feature.';
    const news = scope.releaseNews(context);
    assert.equal(news.paragraphs.length, 3);
    assert.equal(news.paragraphs[1], 'Second feature.');
    scope.getWidget('news').render({ container, context });
    assert.equal(container.querySelector('.md-dashboard-widget__news-highlights').innerHTML, context.labels.changelog);
    assert.equal(container.querySelector('.md-dashboard-widget__news-header button'), null);
    const actions = container.querySelector('.md-dashboard-widget__news-actions');
    assert.equal(actions.children.length, 2);
    assert.equal(actions.querySelector('button').textContent, 'admin.dashboard.newsCollapse.js');
    assert.equal(actions.querySelector('button i').getAttribute('aria-hidden'), 'true');
    assert.equal(container.querySelector('.md-dashboard-widget__news-more').getAttribute('href'), 'https://docs.webjetcms.sk/latest/en/CHANGELOG');
});

test('Expanded release announcements retain headings, lists, emphasis and links from the Markdown renderer', t => {
    const { scope, context, container } = fixture(t, { extraWidgets: true });
    context.labels.changelog = '<h2>WebJET CMS 2026.18</h2><p>A <strong>complete</strong> announcement.</p><ul><li>First feature</li><li>Second <em>feature</em></li></ul><p>Read <a href="/release-details/">the details</a>.</p>';
    assert.equal(scope.releaseNews(context).version, '2026.18');
    scope.getWidget('news').render({ container, context });
    assert.equal(container.querySelector('.md-dashboard-widget__news-highlights').innerHTML, context.labels.changelog);
});
