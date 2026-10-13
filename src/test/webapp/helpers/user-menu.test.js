const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const pug = require('pug');
const { JSDOM } = require('jsdom');

const root = path.resolve(__dirname, '../../../main/webapp/admin/v9');
const tick = () => new Promise(resolve => setImmediate(resolve));

/** Runs the production menu against the real template with only popup and HTTP boundaries mocked. */
function fixture(t, { withTwoFactor = true } = {}) {
    const dom = new JSDOM(pug.renderFile(path.join(root, 'views/partials/user-menu.pug')), { url: 'http://localhost/admin/v9/search/' });
    t.after(() => dom.window.close());
    const { window } = dom;
    window.csrfToken = 'autotest-csrf';
    const trigger = window.document.querySelector('#dropdownMenuUser');
    const panel = window.document.querySelector('#admin-user-menu');
    const action = panel.querySelector('.md-user-menu__two-factor');
    const badge = panel.querySelector('.md-user-menu__two-factor-badge');
    if (!withTwoFactor) action.parentElement.remove();
    const calls = [], requests = [];
    let response = { ok: true, json: async () => ({ sessionCount: 3, deviceCount: 25, twoFactorEnabled: true, newLoginCount: 2 }) };
    let fail = false;
    const dropdown = { hide: () => trigger.dispatchEvent(new window.Event('hide.bs.dropdown')) };
    const scope = vm.createContext({
        document: window.document, window,
        bootstrap: { Dropdown: { getOrCreateInstance: () => dropdown } },
        WJ: { translate: (key, count) => key.includes('.enabled.') ? 'Enabled' : key.includes('.disabled.') ? 'Disabled' : `${key}:${count ?? ''}`,
            openPopupDialog: url => { calls.push(url); return {}; } },
        fetch: async (url, options) => {
            assert.equal(options.headers['X-CSRF-Token'], 'autotest-csrf');
            assert.equal(options.cache, 'no-store');
            requests.push(url);
            if (fail) throw new Error('Network unavailable');
            return response;
        }
    });
    vm.runInContext(fs.readFileSync(path.join(root, 'src/js/user-menu.js'), 'utf8').replace(/^export /gm, ''), scope);
    scope.initUserMenu();
    return { scope, window, trigger, panel, action, badge, calls, requests,
        open: () => trigger.dispatchEvent(new window.Event('show.bs.dropdown')),
        respond: value => { response = value; },
        fail: value => { fail = value; } };
}

test('Rendering makes no requests; each opening refreshes all account data', async t => {
    const view = fixture(t);
    assert.deepEqual(view.requests, []);
    assert.equal(view.badge.textContent, '');
    view.open();
    await tick();
    assert.deepEqual(view.requests, ['/admin/rest/user-menu']);
    assert.equal(view.badge.dataset.enabled, 'true');
    assert.equal(view.panel.querySelector('.md-user-menu__session-count').textContent, '3');
    assert.equal(view.panel.querySelector('.md-user-menu__session-hint').textContent, 'admin.userMenu.devices.js:25');
    assert.equal(view.panel.querySelector('.md-user-menu__new-logins').textContent, 'admin.userMenu.newLogins.js:2');
    view.open();
    await tick();
    assert.equal(view.requests.length, 2);
});

test('Slow requests leave empty placeholders and do not cancel opening or duplicate requests', async t => {
    const view = fixture(t);
    let resolve;
    view.respond({ ok: true, json: () => new Promise(done => { resolve = done; }) });
    const event = new view.window.Event('show.bs.dropdown', { cancelable: true });
    view.trigger.dispatchEvent(event);
    view.open();
    await tick();
    assert.equal(event.defaultPrevented, false);
    assert.equal(view.panel.getAttribute('aria-busy'), 'true');
    assert.equal(view.badge.textContent, '');
    assert.equal(view.badge.classList.contains('is-disabled'), false);
    assert.equal(view.requests.length, 1);
    resolve({ sessionCount: 2, deviceCount: null, twoFactorEnabled: false, newLoginCount: 0 });
    await tick();
    assert.equal(view.panel.getAttribute('aria-busy'), 'false');
    assert.equal(view.badge.textContent, 'Disabled');
    assert.equal(view.panel.querySelector('.md-user-menu__session-hint').textContent, 'admin.userMenu.sessionsHint.js:');
    assert.equal(view.panel.querySelector('.md-user-menu__new-logins').textContent, '');
});

test('Settings popup keeps focus behavior and reopening reads the newly saved status', async t => {
    const view = fixture(t);
    view.scope.initUserMenu();
    view.open();
    await tick();
    view.action.click();
    assert.deepEqual(view.calls, ['/admin/2factorauth.jsp']);
    assert.equal(view.window.document.activeElement, view.trigger);
    view.respond({ ok: true, json: async () => ({ sessionCount: 1, deviceCount: 1, twoFactorEnabled: false, newLoginCount: 0 }) });
    view.open();
    await tick();
    assert.equal(view.requests.length, 2);
    assert.equal(view.badge.dataset.enabled, 'false');
    assert.equal(view.badge.classList.contains('is-enabled'), false);
    assert.equal(view.badge.classList.contains('is-disabled'), true);
});

test('Failed requests show unavailable status and retry on the next opening', async t => {
    const view = fixture(t);
    view.open();
    await tick();
    view.fail(true);
    view.open();
    await tick();
    assert.equal(view.badge.textContent, '—');
    assert.equal(view.badge.dataset.enabled, undefined);
    assert.equal(view.badge.classList.contains('is-disabled'), false);
    assert.equal(view.panel.querySelector('.md-user-menu__session-count').textContent, '—');
    view.fail(false);
    view.open();
    await tick();
    assert.equal(view.badge.textContent, 'Enabled');
    assert.equal(view.requests.length, 3);
});

test('HTTP errors and a login HTML response never imply disabled verification', async t => {
    const view = fixture(t);
    for (const response of [{ ok: false }, { ok: true, json: async () => { throw new SyntaxError('HTML response'); } }]) {
        view.respond(response);
        view.open();
        await tick();
        assert.equal(view.badge.textContent, '—');
        assert.equal(view.badge.dataset.enabled, undefined);
        assert.equal(view.panel.getAttribute('aria-busy'), 'false');
    }
});

test('Installations without two-factor authentication still refresh session and device counts', async t => {
    const view = fixture(t, { withTwoFactor: false });
    view.open();
    await tick();
    assert.deepEqual(view.requests, ['/admin/rest/user-menu']);
    assert.equal(view.panel.querySelector('.md-user-menu__session-count').textContent, '3');
});
