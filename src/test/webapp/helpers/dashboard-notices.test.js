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
    let now = Date.UTC(2026, 9, 2), nextId = 0, fail = false, responseValue = true;
    const timers = new Map(), requests = [];
    window.setTimeout = (fn, delay) => { timers.set(++nextId, { fn, at: now + delay }); return nextId; };
    window.clearTimeout = id => timers.delete(id);
    window.currentUser = { adminSettings: { 'dashboard.notices': JSON.stringify(state || { dismissedUntil: {} }) } };
    const context = vm.createContext({ window, document: window.document, AbortController,
        Date: class extends Date { static now() { return now; } },
        WJ: { translate: (key, ...params) => `${key} ${params.join(',')}`.trim() },
        fetch: async (url, options) => { requests.push({ url, ...options }); return { ok: !fail, json: async () => fail ? false : responseValue }; }
    });
    const utilities = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/dashboard/widget-utils.js'), 'utf8').replace(/^export /gm, '');
    vm.runInContext(`{ ${utilities}; this.date = date; }`, context);
    vm.runInContext(fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/dashboard/security-events.js'), 'utf8').replace(/^export /gm, ''), context);
    vm.runInContext(fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/dashboard/notices.js'), 'utf8').replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '') + '\nthis.Notices = DashboardNotices;', context);
    const data = { notices };
    const host = window.document.querySelector('#notices');
    const controller = new context.Notices(host, data);
    controller.render();
    const advance = milliseconds => {
        now += milliseconds;
        for (const [id, timer] of [...timers]) if (timer.at <= now) { timers.delete(id); timer.fn(); }
    };
    t.after(() => { controller.destroy(); window.close(); });
    return { window, controller, data, host, requests, timers, advance, fail: value => { fail = value; }, respond: value => { responseValue = value; }, now: () => now };
}

test('Severity sorting and forged dismissals keep errors visible with inline actions', t => {
    const { host } = fixture(t, { dismissedUntil: { error: Date.now() + week } });
    assert.deepEqual([...host.querySelectorAll('[data-notice-id]')].map(row => row.dataset.noticeId), ['error', 'warning', 'info']);
    assert.equal(host.querySelector('[data-notice-id="error"] .md-dashboard__notice-dismiss'), null);
    assert.equal(host.querySelector('[data-notice-id="error"] .btn-link'), null);
    assert.equal(host.querySelectorAll('.md-dashboard__notice-action').length, 3);
    assert.equal(host.querySelector('.md-dashboard__notice-count').textContent, '3');
});

test('Multiple-session warning opens the dialog and disappears as soon as only one session remains', t => {
    const { controller, data, host } = fixture(t, null, []);
    data.currentSessions = { userSessions: [{ userSessions: [{ sessionId: 'autotest-current' }, { sessionId: 'autotest-other' }] }] };
    let opened = 0;
    controller.openSessions = () => opened++;
    controller.render();
    const notice = host.querySelector('[data-notice-id="multipleSessions"]');
    assert.ok(notice);
    assert.match(notice.textContent, /description.js 2/);
    notice.querySelector('.md-dashboard__notice-action').click();
    assert.equal(opened, 1);
    data.currentSessions.userSessions[0].userSessions.pop();
    controller.render();
    assert.equal(host.querySelector('[data-notice-id="multipleSessions"]'), null);
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

function securityNotice(id = 42) {
    const createDate = Date.UTC(2026, 9, 2);
    return { ...notice(`newDevice:${id}`, 'warning'), kind: 'newDevice', securityEvent: {
        id, createDate, expiresAt: createDate + week, confirmedAt: null, reportedAt: null,
        browserName: '<img src=x> Firefox', browserVersion: '123.0', operatingSystem: 'Linux', ipAddress: '127.0.0.1'
    } };
}

test('New-device notices ignore dismissals, precede errors and expire from the login time', async t => {
    const security = securityNotice();
    const { host, controller, advance, requests } = fixture(t, { dismissedUntil: { [security.id]: Date.UTC(2027, 0, 1) } }, [notice('error', 'error'), security]);
    assert.equal(host.querySelector('[data-notice-id]').dataset.noticeId, security.id);
    const row = host.querySelector(`[data-notice-id="${security.id}"]`);
    const details = row.querySelector('.md-dashboard__notice-description').textContent;
    assert.match(details, /<img src=x> Firefox 123 · Linux/);
    assert.match(details, /127\.0\.0\.1/);
    assert.match(details, /2026/);
    assert.equal(row.querySelector('img'), null, 'Login metadata must remain plain text.');
    assert.equal(row.querySelectorAll('button').length, 2);
    assert.equal(row.querySelector('.md-dashboard__notice-dismiss'), null);
    await controller._dismiss(security, row);
    assert.equal(requests.length, 0, 'Generic dismissal must not save a security notice.');
    let opened;
    controller.openSecurityEvent = event => { opened = event; };
    row.querySelector('.md-dashboard__notice-report').click();
    assert.equal(opened, security.securityEvent);
    assert.equal(requests.length, 0, 'Reviewing a login must not report or dismiss it.');
    advance(week - 1);
    assert.ok(host.querySelector('.md-dashboard__notice-confirm'));
    advance(2);
    assert.equal(host.querySelector('.md-dashboard__notice-confirm'), null);
    assert.ok(host.querySelector('[data-severity="error"]'));
});

test('Blocked device notices stay hidden when loaded or rendered again', t => {
    const blocked = securityNotice('blocked');
    blocked.securityEvent.reportedAt = Date.UTC(2026, 9, 2);
    const pending = securityNotice('pending');
    const { controller, host, requests } = fixture(t, null, [blocked, pending]);
    assert.equal(host.querySelector(`[data-notice-id="${blocked.id}"]`), null);
    assert.ok(host.querySelector(`[data-notice-id="${pending.id}"]`));
    assert.equal(host.querySelector('.md-dashboard__notice-count').textContent, '1');
    pending.securityEvent.reportedAt = blocked.securityEvent.reportedAt;
    controller.render();
    assert.equal(host.querySelector('[data-notice-id]'), null);
    assert.equal(host.querySelector('.md-dashboard__notice-list').hidden, true);
    assert.equal(requests.length, 0);
});

test('Login confirmation sends a code first and changes shared state only after valid verification', async t => {
    const security = securityNotice('autotest/login');
    const { controller, host, requests, fail, respond, window, data, now } = fixture(t, null, [security]);
    data.currentSessions = { userSessions: [{ userSessions: [{ deviceId: security.securityEvent.id, deviceConfirmed: false }] }] };
    window.csrfToken = 'autotest-csrf';
    fail(true);
    controller._confirmSecurityEvent(security);
    await tick();
    const form = host.querySelector('.md-dashboard-device-confirmation');
    assert.match(form.textContent, /newDevice.codeSendError/);
    assert.equal(requests[0].url, '/admin/rest/security/login-events/autotest%2Flogin/code');
    assert.equal(data.currentSessions.userSessions[0].userSessions[0].deviceConfirmed, false);
    fail(false);
    form.querySelector('[type="button"]').click();
    await tick();
    assert.match(form.textContent, /newDevice.codeSent/);
    const submit = async () => {
        form.querySelector('input').value = '012345';
        form.dispatchEvent(new window.Event('submit', { cancelable: true }));
        await tick();
    };
    respond({ id: 'another-account', confirmedAt: now() });
    await submit();
    assert.ok(host.querySelector('.md-dashboard__notice-confirm'));
    assert.match(form.textContent, /newDevice.codeInvalid/);
    data.requestedSecurityEvent = security.securityEvent;
    respond({ ...security.securityEvent, confirmedAt: now() });
    await submit();
    assert.equal(host.querySelector('.md-dashboard__notice-confirm'), null);
    assert.equal(data.requestedSecurityEvent.confirmedAt, now());
    assert.equal(data.currentSessions.userSessions[0].userSessions[0].deviceConfirmed, true);
    assert.equal(requests.at(-1).url, '/admin/rest/security/login-events/autotest%2Flogin/confirm');
    assert.deepEqual(JSON.parse(requests.at(-1).body), { code: '012345' });
    assert.equal(requests[0].method, 'POST');
    assert.equal(requests[0].headers['X-CSRF-Token'], 'autotest-csrf');
    assert.equal(controller.state.dismissedUntil[security.id], undefined);
});

test('The current browser has neutral wording and no not-me action, based only on its bound device ID', t => {
    const security = securityNotice(42);
    const { controller, host, data } = fixture(t, null, [security]);
    data.currentSessions = { currentSessionId: 'current', userSessions: [{ userSessions: [{ sessionId: 'current', deviceId: 42 }] }] };
    controller.render();
    assert.equal(host.querySelector('[data-notice-id]').dataset.severity, 'info');
    assert.match(host.textContent, /newDevice.currentTitle/);
    assert.match(host.textContent, /newDevice.currentDetails/);
    assert.equal(host.querySelector('.md-dashboard__notice-report'), null);
    assert.ok(host.querySelector('.md-dashboard__notice-confirm'));
    data.currentSessions.userSessions[0].userSessions[0].deviceId = 43;
    controller.render();
    assert.equal(host.querySelector('[data-notice-id]').dataset.severity, 'warning');
    assert.ok(host.querySelector('.md-dashboard__notice-report'));
});

test('Multiple notice code forms remain usable independently and all requests abort on teardown', async t => {
    const notices = [securityNotice(42), securityNotice(43)];
    const { controller, host, requests } = fixture(t, null, notices);
    for (const notice of notices) controller._confirmSecurityEvent(notice);
    await tick();
    assert.equal(host.querySelectorAll('.md-dashboard-device-confirmation').length, 2);
    assert.equal(requests.length, 2);
    assert.ok(requests.every(request => !request.signal.aborted));
    controller.destroy();
    assert.ok(requests.every(request => request.signal.aborted));
});
