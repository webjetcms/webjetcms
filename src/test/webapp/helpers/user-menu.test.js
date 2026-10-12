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
function fixture(t, { enabled = false, withTwoFactor = true } = {}) {
    const dom = new JSDOM(pug.renderFile(path.join(root, 'views/partials/user-menu.pug')), { url: 'http://localhost/admin/v9/search/' });
    t.after(() => dom.window.close());
    const { window } = dom;
    const trigger = window.document.querySelector('#dropdownMenuUser');
    const panel = window.document.querySelector('#admin-user-menu');
    const action = panel.querySelector('.md-user-menu__two-factor');
    const badge = panel.querySelector('.md-user-menu__two-factor-badge');
    if (!withTwoFactor) action.parentElement.remove();
    badge.dataset.enabled = String(enabled);
    badge.classList.add(enabled ? 'is-enabled' : 'is-disabled');
    badge.textContent = enabled ? 'Enabled' : 'Disabled';
    const popup = { closed: false };
    const calls = [], requests = [];
    let response = { ok: true, text: async () => 'true' };
    let fail = false;
    const dropdown = { hide: () => trigger.dispatchEvent(new window.Event('hide.bs.dropdown')) };
    const scope = vm.createContext({
        document: window.document, window,
        bootstrap: { Dropdown: { getOrCreateInstance: () => dropdown } },
        WJ: { translate: key => key.includes('.enabled.') ? 'Enabled' : 'Disabled',
            openPopupDialog: url => { calls.push(url); return popup; } },
        fetch: async url => {
            requests.push(url);
            if (fail) throw new Error('Network unavailable');
            return response;
        }
    });
    vm.runInContext(fs.readFileSync(path.join(root, 'src/js/user-menu.js'), 'utf8').replace(/^export /gm, ''), scope);
    scope.initUserMenu();
    return { scope, window, trigger, panel, action, badge, popup, calls, requests,
        open: () => trigger.dispatchEvent(new window.Event('show.bs.dropdown')),
        respond: value => { response = value; },
        fail: value => { fail = value; } };
}

test('Rendering and ordinary menu openings make no HTTP requests', async t => {
    const view = fixture(t);
    view.open(); view.open();
    await tick();
    assert.deepEqual(view.requests, []);
    assert.equal(view.trigger.dataset.userInitialized, 'true');
    assert.equal(view.badge.textContent, 'Disabled');
    assert.equal(view.panel.querySelector('.md-user-menu__sessions').getAttribute('href'), '/admin/v9/?activeSessions=true');
});

test('A closed settings popup refreshes the existing status once on reopening the menu', async t => {
    const view = fixture(t);
    view.scope.initUserMenu();
    view.action.click();
    assert.deepEqual(view.calls, ['/admin/2factorauth.jsp']);
    assert.equal(view.window.document.activeElement, view.trigger);
    view.open();
    await tick();
    assert.deepEqual(view.requests, []);
    view.popup.closed = true;
    view.open(); view.open();
    await tick();
    assert.deepEqual(view.requests, ['/admin/users/2factorauth']);
    assert.equal(view.badge.dataset.enabled, 'true');
    assert.equal(view.badge.textContent, 'Enabled');
    assert.equal(view.badge.classList.contains('is-enabled'), true);
    assert.equal(view.badge.classList.contains('is-disabled'), false);
    view.open();
    await tick();
    assert.equal(view.requests.length, 1);
});

test('Disabling verification updates text and semantic badge state', async t => {
    const view = fixture(t, { enabled: true });
    view.respond({ ok: true, text: async () => '' });
    view.action.click();
    view.popup.closed = true;
    view.open();
    await tick();
    assert.equal(view.badge.dataset.enabled, 'false');
    assert.equal(view.badge.textContent, 'Disabled');
    assert.equal(view.badge.classList.contains('is-enabled'), false);
    assert.equal(view.badge.classList.contains('is-disabled'), true);
});

test('Failed status refresh retains the confirmed value and retries on the next opening', async t => {
    const view = fixture(t);
    view.action.click();
    view.popup.closed = true;
    view.fail(true);
    view.open();
    await tick();
    assert.equal(view.badge.textContent, 'Disabled');
    view.fail(false);
    view.open();
    await tick();
    assert.equal(view.badge.textContent, 'Enabled');
    assert.equal(view.requests.length, 2);
});

test('Unexpected responses never turn an enabled badge into a disabled badge', async t => {
    const view = fixture(t, { enabled: true });
    view.action.click();
    view.popup.closed = true;
    for (const response of [{ ok: false }, { ok: true, text: async () => '<html>Sign in</html>' }]) {
        view.respond(response);
        view.open();
        await tick();
        assert.equal(view.badge.textContent, 'Enabled');
        assert.equal(view.badge.dataset.enabled, 'true');
    }
});

test('Installations without two-factor authentication keep a static initialized menu', async t => {
    const view = fixture(t, { withTwoFactor: false });
    view.open();
    await tick();
    assert.deepEqual(view.requests, []);
    assert.equal(view.trigger.dataset.userInitialized, 'true');
});
