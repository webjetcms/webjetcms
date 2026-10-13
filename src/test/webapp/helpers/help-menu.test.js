const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const pug = require('pug');
const { JSDOM } = require('jsdom');

const adminRoot = path.resolve(__dirname, '../../../main/webapp/admin/v9');
const tick = () => new Promise(resolve => setImmediate(resolve));
const announcement = '## WebJET CMS 2026.18\\n\\n- **First update** – First description.\\n- **Second update** – Second description.';
const readSource = file => fs.readFileSync(path.join(adminRoot, 'src/js', file), 'utf8')
    .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');

/** Runs the real menu template, URL builder and release state with browser-only boundaries supplied. */
function fixture(t, { language = 'sk', base = 'http://manual.intranet:8080', release = announcement, realTooltip = false, url = 'http://localhost/admin/v9/search/' } = {}) {
    const markup = pug.renderFile(path.join(adminRoot, 'views/partials/help-menu.pug'));
    const dom = new JSDOM(`<!doctype html><body><div class="header-title">Current page</div>${markup}</body>`, { url, runScripts: 'outside-only' });
    const { window } = dom;
    if (realTooltip) window.eval(fs.readFileSync(require.resolve('bootstrap/dist/js/bootstrap.bundle.js'), 'utf8'));
    window.userLng = language;
    window.currentUser = { adminSettings: {} };
    window.csrfToken = 'help-autotest-csrf';
    window.adminHelpContext = {
        documentationBaseUrl: base,
        announcement: release,
        supportUrl: 'www.webjetcms.sk',
        version: '2026.0-jakarta-SNAPSHOT.048 09.10.2026 11:18:54 Enterprise (node / full) server: tomcat_a4',
        systemInfo: 'WebJET CMS (Enterprise)\nVersion: 2026.18.28-jakarta.026, 23. 9. 2026 14:16:41\nServer: tomcat_a4\nDomain: demo.webjetcms.sk (DEV)',
        currentDomain: 'demo.webjetcms.sk',
        browser: 'Chrome 153 · macOS'
    };
    const feedbackCalls = [], requests = [], successes = [], errors = [];
    window.WJ = {
        translate: (key, ...params) => [key, ...params].join(' '),
        showFeedbackDialog: options => feedbackCalls.push({ options, focused: window.document.activeElement }),
        notifySuccess: (...args) => successes.push(args),
        notifyError: (...args) => errors.push(args),
        initTooltip() {}
    };
    const trigger = window.document.querySelector('#admin-help-toggle');
    const panel = window.document.querySelector('#admin-help-menu');
    panel.querySelector('.md-help-menu__copy').title = 'Copy WebJET CMS information';
    const dropdown = {
        shows: 0, hides: 0,
        show() {
            this.shows++;
            trigger.dispatchEvent(new window.Event('show.bs.dropdown'));
            trigger.focus();
            trigger.setAttribute('aria-expanded', 'true');
            panel.classList.add('show');
            trigger.dispatchEvent(new window.Event('shown.bs.dropdown'));
        },
        hide() {
            this.hides++;
            trigger.setAttribute('aria-expanded', 'false');
            panel.classList.remove('show');
        }
    };
    const scope = vm.createContext({
        window, document: window.document, navigator: window.navigator, DOMParser: window.DOMParser, URL,
        WJ: window.WJ,
        $: element => element,
        bootstrap: {
            Dropdown: { getOrCreateInstance: () => dropdown },
            Tooltip: realTooltip ? window.bootstrap.Tooltip : { getOrCreateInstance: () => ({ setContent() {}, hide() {} }) }
        },
        fetch: async (url, options) => {
            requests.push({ url, options });
            return { ok: true, json: async () => true };
        }
    });
    const webjet = readSource('webjet.js');
    const markdownStart = webjet.indexOf('    function parseMarkdown(');
    vm.runInContext(`${webjet.slice(markdownStart, webjet.indexOf('    function initTooltip(', markdownStart))}\nWJ.parseMarkdown = parseMarkdown;`, scope);
    for (const file of ['documentation-links.js', 'release-news.js', 'help-menu.js']) vm.runInContext(readSource(file), scope, { filename: file });
    scope.initHelpMenu();
    t.after(() => window.close());
    return { scope, window, trigger, panel, dropdown, feedbackCalls, requests, successes, errors };
}

test('Contextual Help follows the current page value without focusing an item when opened', t => {
    const { window, trigger, panel, dropdown } = fixture(t, { release: '' });
    const row = panel.querySelector('.md-help-menu__page-row');
    const link = panel.querySelector('.md-help-menu__page');
    const subtitle = panel.querySelector('.md-help-menu__page-title');
    for (const value of [undefined, null, '', '  ', {}, false]) {
        window.helpLink = value;
        dropdown.show();
        assert.equal(row.hidden, true);
        assert.equal(window.document.activeElement, trigger);
    }
    window.helpLink = ' /redactor/admin/welcome ';
    dropdown.show();
    assert.equal(row.hidden, false);
    assert.equal(link.href, 'http://manual.intranet:8080/latest/sk/redactor/admin/welcome');
    assert.equal(subtitle.textContent, 'Current page');
    assert.equal(window.document.activeElement, trigger);
    window.helpLink = '/redactor/apps/gallery';
    window.document.querySelector('.header-title').textContent = ' Gallery ';
    dropdown.show();
    assert.equal(link.href, 'http://manual.intranet:8080/latest/sk/redactor/apps/gallery');
    assert.equal(subtitle.textContent, 'Gallery');
    window.document.querySelector('.header-title').textContent = '';
    dropdown.show();
    assert.equal(subtitle.hidden, true);
    delete window.helpLink;
    dropdown.show();
    assert.equal(row.hidden, true);
});

test('WJ.showHelpWindow preserves explicit and contextual routes on the configured documentation origin', t => {
    const { scope, window } = fixture(t, { language: 'cz' });
    const webjet = readSource('webjet.js');
    const start = webjet.indexOf('    function showHelpWindow(');
    vm.runInContext(webjet.slice(start, webjet.indexOf('    function changeDomainImpl(', start)), scope);
    const opened = [];
    window.open = (...args) => opened.push(args);
    scope.showHelpWindow();
    window.helpLink = '/redactor/admin/welcome';
    scope.showHelpWindow();
    scope.showHelpWindow('redactor/admin/keyboard-shortcuts');
    scope.showHelpWindow('/');
    assert.deepEqual(opened, [
        ['http://manual.intranet:8080/latest/cs/', '_blank', 'noopener'],
        ['http://manual.intranet:8080/latest/cs/redactor/admin/welcome', '_blank', 'noopener'],
        ['http://manual.intranet:8080/latest/cs/redactor/admin/keyboard-shortcuts', '_blank', 'noopener'],
        ['http://manual.intranet:8080/latest/cs/', '_blank', 'noopener']
    ]);
});

test('Documentation, keyboard shortcuts and changelog follow the configured origin and user language', t => {
    const { scope, window, panel, dropdown } = fixture(t, { release: '' });
    for (const [language, expected] of [['sk', 'sk'], ['cs', 'cs'], ['cz', 'cs'], ['en', 'en'], ['de', 'en'], ['hu', 'en'], [undefined, 'en']]) {
        window.userLng = language;
        dropdown.show();
        for (const link of panel.querySelectorAll('[data-help-path]')) {
            assert.equal(link.href, `http://manual.intranet:8080/latest/${expected}${link.dataset.helpPath}`);
            assert.equal(link.target, '_blank');
            assert.equal(link.rel, 'noopener');
        }
        assert.equal(scope.getDocumentationUrl(' /CHANGELOG-2026?id=_202618#details '), `http://manual.intranet:8080/latest/${expected}/CHANGELOG-2026?id=_202618#details`);
    }
    window.adminHelpContext.documentationBaseUrl = 'https://customer.example';
    window.userLng = 'sk';
    dropdown.show();
    assert.equal(panel.querySelector('[data-help-path="/"]').href, 'https://customer.example/latest/sk/');
    assert.equal(panel.querySelector('[data-help-path="/redactor/admin/keyboard-shortcuts"]').href, 'https://customer.example/latest/sk/redactor/admin/keyboard-shortcuts');
});

test('The documentation shortcut is a normal link and does not install a question-mark keyboard binding', t => {
    const { window, dropdown, requests } = fixture(t);
    const input = window.document.createElement('textarea');
    window.document.body.append(input);
    for (const target of [window.document.body, input]) {
        target.focus();
        const event = new window.KeyboardEvent('keydown', { key: '?', code: 'Slash', shiftKey: true, bubbles: true, cancelable: true });
        target.dispatchEvent(event);
        assert.equal(event.defaultPrevented, false);
    }
    assert.equal(dropdown.shows, 0);
    assert.equal(requests.length, 0, 'The menu must not read or save news before it is opened.');
});

test('Release metadata links to its documentation section and reuses the account reading record', async t => {
    const { scope, window, panel, dropdown, requests } = fixture(t);
    assert.equal(requests.length, 0);
    dropdown.show();
    const release = panel.querySelector('.md-help-menu__release');
    assert.equal(release.parentElement.hidden, false);
    assert.equal(release.href, 'http://manual.intranet:8080/latest/sk/CHANGELOG-2026?id=_202618');
    assert.equal(panel.querySelector('.md-help-menu__release-title').textContent, 'admin.help.release.js 2026.18');
    assert.equal(panel.querySelector('.md-help-menu__release-count').textContent, 'admin.help.releaseCount.js 2');
    assert.equal(panel.querySelector('.md-help-menu__new').hidden, false);
    dropdown.show();
    await tick();
    assert.equal(requests.length, 1);
    assert.equal(requests[0].url, '/admin/rest/admin-settings/');
    assert.equal(requests[0].options.headers['X-CSRF-Token'], 'help-autotest-csrf');
    const saved = JSON.parse(window.currentUser.adminSettings['dashboard.news.sk']);
    assert.equal(saved.fingerprint, scope.newsFingerprint(scope.getReleaseNews().html));
    saved.firstSeen = Date.now() - 31 * 86400000;
    window.currentUser.adminSettings['dashboard.news.sk'] = JSON.stringify(saved);
    dropdown.show();
    assert.equal(panel.querySelector('.md-help-menu__new').hidden, true);
    assert.equal(requests.length, 1, 'Reopening expired news must not restart its thirty-day badge.');
    window.adminHelpContext.announcement += '\\n- **Third update** – Changed content.';
    dropdown.show();
    await tick();
    assert.equal(panel.querySelector('.md-help-menu__new').hidden, false);
    assert.equal(panel.querySelector('.md-help-menu__release-count').textContent, 'admin.help.releaseCount.js 3');
    assert.equal(requests.length, 2);
});

test('Empty and unversioned announcements hide release details without saving a reading window', t => {
    const { window, panel, dropdown, requests } = fixture(t, { release: '' });
    for (const value of ['', '- **Update** – A description without a release version.']) {
        window.adminHelpContext.announcement = value;
        dropdown.show();
        assert.equal(panel.querySelector('.md-help-menu__release-row').hidden, true);
    }
    assert.equal(requests.length, 0);
    assert.equal(panel.querySelector('[data-help-path="/CHANGELOG"]').href, 'http://manual.intranet:8080/latest/sk/CHANGELOG');
});

test('Feedback closes Help, restores its invoking control and returns focus to its menu item', t => {
    const { scope, window, trigger, panel, dropdown, feedbackCalls } = fixture(t, { release: '' });
    scope.initHelpMenu();
    dropdown.show();
    const feedback = panel.querySelector('.md-help-menu__feedback');
    feedback.focus();
    feedback.click();
    assert.equal(feedbackCalls.length, 1, 'Reinitialization must not register duplicate menu actions.');
    assert.equal(dropdown.hides, 1);
    assert.equal(panel.classList.contains('show'), false);
    assert.equal(feedbackCalls[0].focused, trigger);
    feedbackCalls[0].options.onBack();
    assert.equal(panel.classList.contains('show'), true);
    assert.equal(window.document.activeElement, feedback);
    assert.equal(window.document.querySelector('webjet-overview-dashboard'), null);
});

test('Support accepts translated hosts and HTTP(S) destinations while hiding unsafe destinations', t => {
    const { scope, window, panel, dropdown } = fixture(t, { release: '' });
    const support = panel.querySelector('.md-help-menu__support');
    for (const [value, expected] of [['www.webjetcms.sk', 'https://www.webjetcms.sk/'], ['support.intranet:8443/help', 'https://support.intranet:8443/help'], ['https://support.example/help', 'https://support.example/help'], ['http://support.intranet:8080/', 'http://support.intranet:8080/']]) {
        window.adminHelpContext.supportUrl = value;
        dropdown.show();
        assert.equal(scope.supportUrl(value), expected);
        assert.equal(support.parentElement.hidden, false);
        assert.equal(support.href, expected);
    }
    for (const value of [undefined, null, '', '  ', 'javascript:alert(1)', 'ftp://support.example', 'https://user:password@support.example/', 'not a host']) {
        window.adminHelpContext.supportUrl = value;
        dropdown.show();
        assert.equal(scope.supportUrl(value), null);
        assert.equal(support.parentElement.hidden, true);
    }
});

test('Copy uses the Clipboard API when available and keeps the Help menu open', async t => {
    const { window, panel, dropdown, successes, errors } = fixture(t, { release: '' });
    const copied = [];
    Object.defineProperty(window.navigator, 'clipboard', { value: { writeText: async value => copied.push(value) } });
    window.document.execCommand = () => { throw new Error('The fallback must not run after Clipboard API success.'); };
    dropdown.show();
    const copy = panel.querySelector('.md-help-menu__copy');
    let reset, delay;
    window.setTimeout = (callback, milliseconds) => { reset = callback; delay = milliseconds; };
    copy.focus();
    copy.click();
    await tick();
    assert.deepEqual(copied, [window.adminHelpContext.systemInfo + '\nadmin.help.cmsDomain.js: http://localhost\nadmin.help.pagePath.js: /admin/v9/search/\nadmin.help.browser.js: Chrome 153 · macOS']);
    assert.equal(panel.classList.contains('show'), true);
    assert.equal(dropdown.hides, 0);
    assert.equal(window.document.activeElement, copy);
    assert.equal(copy.classList.contains('is-copied'), true);
    assert.equal(copy.querySelector('.ti').classList.contains('ti-check'), true);
    assert.equal(copy.getAttribute('aria-label'), 'admin.help.copied.js');
    assert.equal(copy.getAttribute('data-bs-original-title'), 'admin.help.copied.js');
    assert.equal(panel.querySelector('[role="status"]').textContent, 'admin.help.copied.js');
    assert.equal(delay, 2000);
    reset();
    assert.equal(copy.classList.contains('is-copied'), false);
    assert.equal(copy.querySelector('.ti').classList.contains('ti-copy'), true);
    assert.equal(copy.getAttribute('data-bs-original-title'), 'admin.help.copy.js');
    assert.equal(panel.querySelector('[role="status"]').textContent, '');
    assert.equal(successes.length, 0);
    assert.equal(errors.length, 0);
    assert.equal(window.document.querySelector('textarea'), null);
});

test('Diagnostics refresh the page address and preserve query parameters and fragments', t => {
    const { scope, window } = fixture(t, { release: '' });
    window.history.replaceState({}, '', '/admin/v9/webpages/?docid=123&value=a%2Fb#editor%23tab');
    const result = scope.getSystemInfo();
    assert.ok(result.includes('\nadmin.help.pagePath.js: /admin/v9/webpages/?docid=123&value=a%2Fb#editor%23tab\n'));
    assert.ok(result.includes('Domain: demo.webjetcms.sk (DEV)\nadmin.help.cmsDomain.js: http://localhost\n'));
});

test('CMS diagnostics include the browser protocol and non-default port', t => {
    const { scope } = fixture(t, { release: '', url: 'https://localhost:8443/admin/v9/search/' });
    assert.ok(scope.getSystemInfo().includes('\nadmin.help.cmsDomain.js: https://localhost:8443\n'));
});

test('Diagnostics omit the CMS domain when the browser and content domains match', t => {
    const { scope, window } = fixture(t, { release: '' });
    for (const domain of ['localhost', 'LOCALHOST']) {
        window.adminHelpContext.currentDomain = domain;
        assert.ok(!scope.getSystemInfo().includes('admin.help.cmsDomain.js'));
    }
});

test('The real Bootstrap tooltip updates after copying and restores its original label', async t => {
    const { window, panel } = fixture(t, { release: '', realTooltip: true });
    Object.defineProperty(window.navigator, 'clipboard', { value: { writeText: async () => {} } });
    const copy = panel.querySelector('.md-help-menu__copy');
    const tooltip = window.bootstrap.Tooltip.getInstance(copy);
    const setTimeout = window.setTimeout.bind(window);
    let reset;
    window.setTimeout = (callback, delay, ...args) => {
        if (delay === 2000) { reset = callback; return; }
        return setTimeout(callback, delay, ...args);
    };
    tooltip.show();
    assert.equal(window.document.querySelector('.tooltip-inner').textContent, 'Copy WebJET CMS information');
    copy.click();
    await tick();
    assert.equal(window.document.querySelector('.tooltip-inner').textContent, 'admin.help.copied.js');
    reset();
    assert.equal(window.document.querySelector('.tooltip-inner').textContent, 'admin.help.copy.js');
    tooltip.dispose();
});

test('Repeated copies restart the confirmation timeout', async t => {
    const { window, panel } = fixture(t, { release: '' });
    Object.defineProperty(window.navigator, 'clipboard', { value: { writeText: async () => {} } });
    const scheduled = new Map();
    let now = 0, nextId = 0;
    window.setTimeout = (callback, delay) => { scheduled.set(++nextId, { callback, at: now + delay }); return nextId; };
    window.clearTimeout = id => scheduled.delete(id);
    const advance = milliseconds => {
        now += milliseconds;
        for (const [id, timer] of scheduled) if (timer.at <= now) {
            scheduled.delete(id);
            timer.callback();
        }
    };
    const copy = panel.querySelector('.md-help-menu__copy');
    copy.click();
    await tick();
    advance(1000);
    copy.click();
    await tick();
    advance(1000);
    assert.equal(copy.classList.contains('is-copied'), true);
    advance(1000);
    assert.equal(copy.classList.contains('is-copied'), false);
});

test('HTTP and denied clipboard access use a selected textarea, clean it up and preserve focus', async t => {
    for (const denied of [false, true]) {
        const { scope, window, trigger } = fixture(t, { release: '' });
        if (denied) Object.defineProperty(window.navigator, 'clipboard', { value: { writeText: async () => { throw new Error('Permission denied'); } } });
        const copied = [];
        window.document.execCommand = command => {
            const input = window.document.querySelector('textarea');
            assert.equal(command, 'copy');
            assert.equal(input.readOnly, true);
            assert.equal(input.selectionStart, 0);
            assert.equal(input.selectionEnd, input.value.length);
            copied.push(input.value);
            return true;
        };
        trigger.focus();
        await scope.copySystemInfo('Diagnostic text');
        assert.deepEqual(copied, ['Diagnostic text']);
        assert.equal(window.document.querySelector('textarea'), null);
        assert.equal(window.document.activeElement, trigger);
    }
});

test('Clipboard fallback failures clean temporary controls and report an error without success', async t => {
    for (const throws of [false, true]) {
        const { window, panel, successes, errors } = fixture(t, { release: '' });
        window.document.execCommand = () => { if (throws) throw new Error('Clipboard unavailable'); return false; };
        const copy = panel.querySelector('.md-help-menu__copy');
        copy.focus();
        copy.click();
        await tick();
        assert.equal(window.document.querySelector('textarea'), null);
        assert.equal(window.document.activeElement, copy);
        assert.equal(successes.length, 0);
        assert.equal(copy.classList.contains('is-copied'), false);
        assert.equal(copy.querySelector('.ti').classList.contains('ti-copy'), true);
        assert.deepEqual(errors, [['admin.help.copyError.js']]);
    }
});
