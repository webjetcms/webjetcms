const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

const tick = () => new Promise(resolve => setImmediate(resolve));
const week = 7 * 24 * 60 * 60 * 1000;
const notice = (id, severity) => ({ id, severity, title: `${id} autotest`, description: 'Notice explanation autotest', action: { type: 'link', url: '/admin/v9/', label: 'Open autotest' } });

/** Uses production notice code with account storage and a deterministic clock. */
function fixture(t, state, notices = [notice('warning', 'warning'), notice('info', 'info'), notice('error', 'error')]) {
    const dom = new JSDOM('<!doctype html><html lang="en"><body><div id="notices"></div><button id="outside">Outside</button></body></html>', { url: 'http://localhost/admin/v9/' });
    const { window } = dom;
    let now = Date.UTC(2026, 9, 2), nextId = 0, fail = false;
    const timers = new Map(), requests = [];
    window.setTimeout = (fn, delay) => { timers.set(++nextId, { fn, at: now + delay }); return nextId; };
    window.clearTimeout = id => timers.delete(id);
    window.currentUser = { adminSettings: { 'dashboard.notices': JSON.stringify(state || { dismissedUntil: {} }) } };
    const context = vm.createContext({ window, document: window.document, AbortController,
        Date: class extends Date { static now() { return now; } },
        WJ: { translate: (key, ...params) => `${key} ${params.join(',')}`.trim() },
        fetch: async (url, options) => { requests.push({ url, ...options }); return { ok: !fail, json: async () => !fail }; }
    });
    vm.runInContext(fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/dashboard/notices.js'), 'utf8').replace(/^export /gm, '') + '\nthis.Notices = DashboardNotices;', context);
    const data = { notices };
    const host = window.document.querySelector('#notices');
    const controller = new context.Notices(host, data);
    controller.render();
    const advance = milliseconds => {
        now += milliseconds;
        for (const [id, timer] of [...timers]) if (timer.at <= now) { timers.delete(id); timer.fn(); }
    };
    t.after(() => { controller.destroy(); window.close(); });
    return { window, controller, data, host, requests, timers, advance, fail: value => { fail = value; }, now: () => now };
}

test('Severity sorting and forged dismissals keep errors visible with inline actions', t => {
    const { host } = fixture(t, { dismissedUntil: { error: Date.now() + week } });
    assert.deepEqual([...host.querySelectorAll('[data-notice-id]')].map(row => row.dataset.noticeId), ['error', 'warning', 'info']);
    assert.equal(host.querySelector('[data-notice-id="error"] .md-dashboard__notice-dismiss'), null);
    assert.equal(host.querySelector('[data-notice-id="error"] .btn-link'), null);
    assert.equal(host.querySelectorAll('.md-dashboard__notice-action').length, 3);
    assert.equal(host.querySelector('.md-dashboard__notice-count').textContent, '3');
});

test('Saved dismissals are restored without writing to the account', t => {
    const { host, requests } = fixture(t, { dismissedUntil: { warning: Date.UTC(2026, 9, 9), info: 0 } });
    assert.deepEqual([...host.querySelectorAll('[data-notice-id]:not([hidden])')].map(row => row.dataset.noticeId), ['error']);
    assert.equal(host.querySelector('.md-dashboard__notice-count').textContent, '1');
    assert.equal(requests.length, 0, 'Loading preferences must not write to the account.');
});

test('Warning dismissal lasts seven days, returns while the page stays open and supports undo', async t => {
    const { controller, host, now, advance, requests } = fixture(t);
    await controller._dismiss(controller.data.notices[0], host.querySelector('[data-notice-id="warning"]'));
    assert.equal(JSON.parse(requests[0].body).label, 'dashboard.notices');
    const saved = JSON.parse(JSON.parse(requests[0].body).value);
    assert.equal(saved.dismissedUntil.warning, now() + week);
    assert.equal(host.querySelector('[data-notice-id="warning"]'), null);
    assert.ok(controller.toast.textContent.includes(new Date(now() + week).toLocaleDateString('en')));
    controller.toast.querySelector('button').click();
    await tick();
    assert.ok(host.querySelector('[data-notice-id="warning"]'));
    await controller._dismiss(controller.data.notices[0], host.querySelector('[data-notice-id="warning"]'));
    advance(week + 1);
    assert.ok(host.querySelector('[data-notice-id="warning"]'));
});

test('Information hides without an expiry', async t => {
    const { controller, host, advance } = fixture(t);
    await controller._dismiss(controller.data.notices[1], host.querySelector('[data-notice-id="info"]'));
    advance(week * 2);
    controller.render();
    assert.equal(host.querySelector('[data-notice-id="info"]'), null);
    assert.ok(host.querySelector('[data-notice-id="error"]'));
});

test('Save failure preserves notices and failed undo remains available', async t => {
    const { controller, host, fail } = fixture(t);
    fail(true);
    await controller._dismiss(controller.data.notices[0], host.querySelector('[data-notice-id="warning"]'));
    assert.ok(host.querySelector('[data-notice-id="warning"]'));
    assert.ok(host.querySelector('[role="alert"]').textContent.includes('saveError'));
    fail(false);
    await controller._dismiss(controller.data.notices[0], host.querySelector('[data-notice-id="warning"]'));
    fail(true);
    controller.toast.querySelector('button').click();
    await tick();
    assert.ok(controller.toast);
    assert.equal(controller.toast.querySelector('button').disabled, false);
});

test('Toast lifetime pauses for hover and focus and resumes with the remaining time', async t => {
    const { controller, host, window, advance, timers } = fixture(t);
    await controller._dismiss(controller.data.notices[0], host.querySelector('[data-notice-id="warning"]'));
    advance(3000);
    controller.toast.dispatchEvent(new window.MouseEvent('mouseenter'));
    advance(9000);
    assert.ok(controller.toast);
    controller.toast.querySelector('button').focus();
    controller.toast.dispatchEvent(new window.MouseEvent('mouseleave'));
    advance(9000);
    assert.ok(controller.toast);
    window.document.querySelector('#outside').focus();
    advance(4999);
    assert.ok(controller.toast);
    advance(1);
    assert.equal(controller.toast, null);
    controller.destroy();
    assert.equal(timers.size, 0);
});

test('Notice summaries render untrusted strings as text', t => {
    const { host } = fixture(t, undefined, [{ ...notice('text', 'info'), title: '<img src=x onerror=alert(1)>', description: '<script>alert(1)</script>' }]);
    assert.equal(host.querySelector('img, script'), null);
    assert.ok(host.textContent.includes('<script>'));
});
