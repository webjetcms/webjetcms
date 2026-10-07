import { registerWidget } from './registry';
import { node, text, date, number, icon, containNativeScroll, fetchJson } from './widget-utils';
import { adminMail, fetchLoggedAdministrators } from './system-widgets';
import { showDeviceConfirmation } from './security-events';

function sessionButton(label, action, className) {
    const control = node('button', className, label);
    control.type = 'button';
    control.addEventListener('click', event => {
        control.focus({ preventScroll: true });
        action(event);
    });
    return control;
}

/**
 * Returns copied session records with their cluster, placing the current session first.
 * @param {Object} data - Bootstrap containing currentSessionId and clustered userSessions.
 * @returns {Object[]} Sessions sorted by current identity and descending login time.
 */
export function flattenSessions(data) {
    return (data?.userSessions || []).flatMap(cluster => (cluster.userSessions || []).map(session => ({ ...session, cluster: cluster.cluster })))
        .sort((a, b) => Number(b.sessionId === data.currentSessionId) - Number(a.sessionId === data.currentSessionId) || b.logonTime - a.logonTime);
}

/** Joins the API's browser and operating system without showing absent or unknown platform data. */
function sessionClient(session) {
    const system = /^unknown$/i.test(session.operatingSystem || '') ? '' : session.operatingSystem;
    return [session.browserName, system].filter(Boolean).join(' · ');
}

/** Resolves only known browser glyphs, never a CSS class supplied by session data. */
function sessionBrowserIcon(browserName) {
    const name = String(browserName || '');
    if (/edge|edg\//i.test(name)) return 'ti-brand-edge';
    if (/firefox|fxios/i.test(name)) return 'ti-brand-firefox';
    if (/chrome|chromium|crios/i.test(name)) return 'ti-brand-chrome';
    if (/safari/i.test(name)) return 'ti-brand-safari';
    return 'ti-device-desktop';
}

/** Formats the API's activity timestamp in the administration language; older cluster records may omit it. */
function sessionActivity(session, currentSessionId, context) {
    if (session.sessionId === currentSessionId) return text(context, 'sessionActiveNow');
    if (!(session.lastActivity > 0)) return '—';
    const minutes = Math.max(0, Math.floor((Date.now() - session.lastActivity) / 60000));
    if (minutes < 1) return text(context, 'sessionActiveNow');
    const locale = window.userLng === 'cz' ? 'cs' : window.userLng || 'en';
    const format = new Intl.RelativeTimeFormat(locale, { numeric: 'always' });
    if (minutes < 60) return format.format(-minutes, 'minute');
    if (minutes < 1440) return format.format(-Math.floor(minutes / 60), 'hour');
    return format.format(-Math.floor(minutes / 1440), 'day');
}

/** Installs hover/focus/Escape tooltips and releases them with the renderer. */
function sessionTooltips(list, signal) {
    if (!window.WJ?.initTooltip || !window.$) return;
    const targets = list.querySelectorAll('[data-bs-toggle="tooltip"]');
    window.WJ.initTooltip(window.$(targets));
    window.$(targets).off('keydown.wjTooltipA11y').on('keydown.wjTooltipA11y', function(event) {
        if (event.key !== 'Escape') return;
        const visible = document.getElementById(this.getAttribute('aria-describedby'))?.classList.contains('show');
        window.bootstrap?.Tooltip?.getInstance(this)?.hide();
        if (visible) { event.preventDefault(); event.stopPropagation(); }
    });
    signal.addEventListener('abort', () => targets.forEach(target => {
        window.bootstrap?.Tooltip?.getInstance(target)?.dispose();
        window.$(target).off('.wjTooltipA11y');
    }), { once: true });
}

/**
 * Signs out an owned session through the existing API and updates the shared bootstrap only on success.
 * Remote sessions remain visible as pending until the next page reload supplies synchronized data.
 * @param {Object} session - Other session selected for logout.
 * @param {import('./registry').WidgetContext} context - Mutable session data and translations.
 * @param {AbortSignal} signal - Cancels the request when its widget or dialog closes.
 * @returns {Promise<boolean>} Whether cluster synchronization is still pending.
 */
async function logoutSession(session, context, signal) {
    const data = context.data.currentSessions;
    if (session.sessionId === data.currentSessionId) throw new Error('The current session cannot be removed');
    const response = await fetch('/admin/rest/sessions/logout', {
        method: 'POST', signal, credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8', 'X-CSRF-Token': window.csrfToken },
        body: new URLSearchParams({ sessionId: session.sessionId })
    });
    const result = await response.json();
    if (!response.ok || result.success !== true) throw new Error('Session removal failed');
    if (signal.aborted) return false;
    const pending = result.pending === true;
    for (const cluster of data.userSessions) {
        if (pending) {
            const item = cluster.userSessions.find(item => item.sessionId === session.sessionId);
            if (item) item.pending = true;
        } else cluster.userSessions = cluster.userSessions.filter(item => item.sessionId !== session.sessionId);
    }
    return pending;
}

/** Refreshes both session widgets and the independent system notices after a logout. */
function refreshSessions(context) {
    context.dashboard.refreshSessions();
    context.dashboard.refreshLoggedAdmins?.();
}

/** Shares the complete, scrollable session list between the welcome panel and personal widgets. */
function sessionList(container, data, context, signal) {
    const list = node('ul', 'md-dashboard-widget__sessions list-unstyled');
    list.tabIndex = 0;
    list.setAttribute('aria-label', text(context, 'sessions'));
    containNativeScroll(list, signal);
    flattenSessions(data).forEach(session => {
        const row = node('li', 'md-dashboard-widget__session');
        row.dataset.sessionLogon = String(session.logonTime);
        const device = icon(sessionBrowserIcon(session.browserName));
        device.classList.add('md-dashboard-widget__session-device');
        row.append(device, node('strong', 'md-dashboard-widget__session-name', sessionClient(session)),
            node('span', 'md-dashboard-widget__session-detail', `${date(session.logonTime)} · ${session.remoteAddr || ''}`));
        row.title = [session.domainName, session.cluster].filter(Boolean).join(' · ');
        if (session.sessionId === data.currentSessionId) {
            const current = node('span', 'md-dashboard-widget__session-current');
            current.tabIndex = 0;
            current.setAttribute('role', 'img');
            current.setAttribute('aria-label', text(context, 'currentSession'));
            current.setAttribute('title', text(context, 'currentSession'));
            current.setAttribute('data-bs-toggle', 'tooltip');
            row.append(current);
        } else if (session.pending) {
            row.append(node('span', 'md-dashboard-widget__session-feedback small text-muted', text(context, 'sessionPending')));
        } else {
            const logout = sessionButton('', async () => {
                window.bootstrap?.Tooltip?.getInstance(logout)?.hide();
                logout.disabled = true;
                try {
                    const pending = await logoutSession(session, context, signal);
                    if (signal.aborted) return;
                    if (pending) {
                        window.bootstrap?.Tooltip?.getInstance(logout)?.dispose();
                        logout.replaceWith(node('span', 'md-dashboard-widget__session-feedback small text-muted', text(context, 'sessionPending')));
                    } else row.remove();
                    refreshSessions(context);
                    container.closest('[data-widget-type]')?.querySelector('.md-dashboard__title-action')?.focus({ preventScroll: true });
                } catch (error) {
                    if (signal.aborted) return;
                    logout.disabled = false;
                    let message = row.querySelector('[role="alert"]');
                    if (!message) { message = node('p', 'md-dashboard-widget__session-feedback text-danger small'); message.setAttribute('role', 'alert'); row.append(message); }
                    message.textContent = text(context, 'sessionError');
                }
            }, 'btn btn-sm md-dashboard-widget__session-logout');
            logout.setAttribute('aria-label', text(context, 'logoutSession'));
            logout.setAttribute('title', text(context, 'logoutSession'));
            logout.setAttribute('data-bs-toggle', 'tooltip');
            logout.append(icon('ti-logout'));
            row.append(logout);
        }
        list.append(row);
    });
    container.append(list);
    sessionTooltips(list, signal);
}

/**
 * Opens the session dialog, optionally guiding the user through an unfamiliar login.
 * @param {import('./registry').WidgetContext} context - Dashboard data and dialog owner.
 * @param {Object|null} [securityEvent] - Owned login event; null displays an unavailable email link.
 */
export function showActiveSessions(context, securityEvent) {
    if (document.querySelector('.md-dashboard-modal--sessions')) return;
    const dialog = context.dashboard.showDialog(text(context, 'activeSessions'));
    dialog.root.classList.add('md-dashboard-modal--sessions');
    dialog.root.querySelector('.modal-dialog').classList.add('modal-lg', 'modal-dialog-centered', 'modal-dialog-scrollable');
    if (securityEvent === null) {
        dialog.body.append(node('p', '', text(context, 'newDevice.unavailable')));
        dialog.footer.append(sessionButton(text(context, 'close'), dialog.close, 'btn btn-sm btn-outline-secondary'));
        return;
    }
    const tabs = node('div', 'nav nav-tabs md-dashboard-sessions__tabs');
    tabs.setAttribute('role', 'tablist');
    tabs.setAttribute('aria-label', text(context, 'activeSessions'));
    dialog.body.before(tabs);
    const mine = node('section', 'md-dashboard-sessions__mine');
    const admins = node('section', 'md-dashboard-sessions__admins');
    const history = node('section', 'md-dashboard-sessions__history');
    const tabDefinitions = [['mine', 'mySessions', mine]];
    if (window.WJ.hasPermission('welcomeShowLoggedAdmins')) {
        tabDefinitions.push(['admins', 'sessionAdmins', admins]);
    }
    tabDefinitions.push(['history', 'sessionHistory', history]);
    let historyLoaded = false;
    let adminsLoaded = false;
    let adminsLoading = false;
    let administrators = [];
    const pendingAdministrators = new Set();
    const tabButtons = tabDefinitions.map(([id, label, panel], index) => {
        const tab = sessionButton(text(context, label), () => selectTab(index), 'nav-link');
        tab.id = `${dialog.root.getAttribute('aria-labelledby')}-${id}`;
        panel.id = `${tab.id}-panel`;
        tab.setAttribute('role', 'tab');
        tab.setAttribute('aria-controls', panel.id);
        panel.setAttribute('role', 'tabpanel');
        panel.setAttribute('aria-labelledby', tab.id);
        panel.tabIndex = 0;
        tab.addEventListener('keydown', event => {
            let next;
            if (event.key === 'ArrowRight') next = (index + 1) % tabDefinitions.length;
            if (event.key === 'ArrowLeft') next = (index + tabDefinitions.length - 1) % tabDefinitions.length;
            if (event.key === 'Home') next = 0;
            if (event.key === 'End') next = tabDefinitions.length - 1;
            if (next == null) return;
            event.preventDefault(); selectTab(next); tabButtons[next].focus();
        });
        tabs.append(tab);
        dialog.body.append(panel);
        return tab;
    });
    function selectTab(index) {
        tabButtons.forEach((tab, i) => {
            tab.classList.toggle('active', i === index);
            tab.setAttribute('aria-selected', String(i === index));
            tab.tabIndex = i === index ? 0 : -1;
            tabDefinitions[i][2].hidden = i !== index;
        });
        if (tabDefinitions[index][0] === 'admins' && !adminsLoaded) loadAdmins();
        if (tabDefinitions[index][0] === 'history' && !historyLoaded) { historyLoaded = true; loadHistory(0); }
    }

    const status = node('span', 'md-dashboard-sessions__status text-danger small');
    status.setAttribute('role', 'alert');
    let busy = false;
    let adminBusy = false;
    let reporting = false;
    let securityDetails = securityEvent ? node('section', 'md-dashboard-sessions__security alert alert-warning') : null;

    function renderSecurityEvent() {
        if (!securityDetails) return;
        securityDetails.replaceChildren();
        const client = [securityEvent.browserName, securityEvent.browserVersion].filter(Boolean).join(' ');
        const device = [client, securityEvent.operatingSystem].filter(Boolean).join(' · ');
        securityDetails.append(node('strong', '', text(context, 'newDevice.title')),
            node('p', 'mb-2', text(context, 'newDevice.details', device || '—', securityEvent.ipAddress || '—', date(securityEvent.createDate))));
        if (securityEvent.reportedAt) {
            const reported = node('p', 'mb-0', text(context, 'newDevice.reported'));
            reported.tabIndex = -1;
            reported.setAttribute('role', 'status');
            securityDetails.append(reported);
        } else {
            securityDetails.append(node('p', 'mb-2', text(context, 'newDevice.reportAdvice')));
            const report = sessionButton(text(context, 'newDevice.report'), reportSecurityEvent, 'btn btn-sm btn-white text-danger');
            report.classList.add('md-dashboard-sessions__report');
            report.disabled = reporting || busy;
            securityDetails.append(report);
        }
    }

    async function reportSecurityEvent() {
        if (reporting || busy) return;
        reporting = true;
        status.textContent = '';
        renderMine();
        try {
            const response = await fetch(`/admin/rest/security/login-events/${encodeURIComponent(securityEvent.id)}/report`, {
                method: 'POST', credentials: 'same-origin', signal: dialog.signal,
                headers: { 'X-CSRF-Token': window.csrfToken || '' }
            });
            if (!response.ok) throw new Error('Login reporting failed');
            const updated = await response.json();
            if (updated.id !== securityEvent.id || !updated.reportedAt) throw new Error('Login report was not saved');
            if (dialog.signal.aborted) return;
            Object.assign(securityEvent, updated);
            for (const cluster of context.data.currentSessions?.userSessions || []) {
                for (const session of cluster.userSessions || []) {
                    if (session.deviceId === updated.id) session.deviceConfirmed = false;
                }
            }
            const notices = context.data.notices ||= [];
            for (const notice of notices) {
                if (notice.securityEvent?.id === updated.id) Object.assign(notice.securityEvent, updated);
            }
            if (!updated.confirmedAt && updated.expiresAt > Date.now() && !notices.some(notice => notice.securityEvent?.id === updated.id)) {
                notices.push({ id: `newDevice:${updated.id}`, kind: 'newDevice', severity: 'warning', icon: 'ti-shield-lock',
                    title: text(context, 'newDevice.title'), description: '', securityEvent: updated });
            }
            if (context.data.requestedSecurityEvent?.id === updated.id) context.data.requestedSecurityEvent = updated;
            context.overview?.noticeController?.render();
        } catch (error) {
            if (!dialog.signal.aborted) status.textContent = text(context, 'newDevice.saveError');
        } finally {
            reporting = false;
            if (!dialog.signal.aborted) {
                renderMine();
                securityDetails.querySelector('button, [role="status"]')?.focus({ preventScroll: true });
            }
        }
    }

    function renderMine() {
        const data = context.data.currentSessions;
        const sessions = flattenSessions(data);
        tabButtons[0].textContent = `${text(context, 'mySessions')} (${number(sessions.length)})`;
        mine.replaceChildren();
        if (securityDetails) {
            renderSecurityEvent();
            mine.append(securityDetails);
        }
        const twoFactor = (context.data.notices || []).find(notice => notice.id === 'twoFactor');
        if (twoFactor) {
            const warning = node('div', 'md-dashboard-sessions__two-factor');
            warning.append(icon('ti-shield-lock'), node('span', '', text(context, 'sessionTwoFactor')),
                sessionButton(text(context, 'sessionEnableTwoFactor'), () => window.WJ.openPopupDialog(twoFactor.action.url), 'btn btn-sm btn-link'));
            mine.append(warning);
        }
        const summary = node('div', 'md-dashboard-sessions__summary');
        const explanation = node('div');
        const count = node('strong', '', text(context, sessions.length === 1 ? 'sessionsSummarySingle' : 'sessionsSummary', number(sessions.length)));
        count.setAttribute('role', 'status');
        explanation.append(count, node('p', '', text(context, 'sessionsAdvice')));
        summary.append(explanation);
        const others = sessions.filter(session => session.sessionId !== data.currentSessionId && !session.pending);
        if (others.length) {
            const all = sessionButton(text(context, 'logoutOtherSessions', number(others.length)), () => remove(others), 'btn btn-sm btn-outline-secondary text-danger');
            all.prepend(icon('ti-logout'));
            all.disabled = busy;
            summary.append(all);
        }
        mine.append(summary);
        const table = node('table', 'md-dashboard-sessions__table');
        const head = node('thead');
        const heading = node('tr');
        ['sessionBrowser', 'sessionIp', 'sessionLastActivity', 'sessionActions'].forEach((key, index) => {
            const cell = node('th'); cell.scope = 'col';
            cell.append(node('span', index === 3 ? 'visually-hidden' : '', text(context, key)));
            heading.append(cell);
        });
        head.append(heading);
        const body = node('tbody');
        sessions.forEach(session => {
            const row = node('tr');
            const device = node('td');
            const identity = node('div', 'md-dashboard-sessions__device');
            const glyph = node('span', 'md-dashboard-sessions__device-icon'); glyph.append(icon(sessionBrowserIcon(session.browserName)));
            const details = node('div');
            const name = node('div', 'md-dashboard-sessions__device-name', sessionClient(session));
            const current = session.sessionId === data.currentSessionId;
            const unconfirmed = session.deviceId > 0 && session.deviceConfirmed === false;
            row.classList.toggle('is-new', unconfirmed);
            if (current) name.append(node('span', 'md-dashboard-sessions__current', text(context, 'currentSession')));
            if (unconfirmed) name.append(node('span', 'md-dashboard-sessions__new', text(context, 'newDevice.badge')));
            details.append(name, node('small', '', text(context, 'sessionLoggedAt', date(session.logonTime))));
            identity.append(glyph, details); device.append(identity);
            const action = node('td', 'md-dashboard-sessions__action');
            if (session.pending) action.append(node('span', 'small text-muted', text(context, 'sessionPending')));
            else if (unconfirmed) {
                const controls = node('div', 'md-dashboard-sessions__device-actions');
                const confirm = sessionButton(text(context, 'newDevice.confirm'), () => confirmDevice(session, action),
                    'btn btn-sm btn-white md-dashboard-sessions__confirm-device');
                const deny = sessionButton(text(context, 'newDevice.notMe'), () => {
                    if (current) document.forms.namedItem('adminLogoffForm')?.requestSubmit();
                    else remove([session]);
                }, 'btn btn-sm btn-danger md-dashboard-sessions__deny-device');
                confirm.disabled = deny.disabled = busy || reporting;
                controls.append(confirm, deny);
                action.append(controls);
            } else if (current && securityEvent) {
                const logout = sessionButton(text(context, 'newDevice.logoutCurrent'), () => document.forms.namedItem('adminLogoffForm')?.requestSubmit(), 'btn btn-sm btn-link');
                logout.disabled = busy;
                action.append(logout);
            } else if (current) action.append(node('span', 'text-muted', '—'));
            else {
                const logout = sessionButton(text(context, 'sessionLogout'), () => remove([session]), 'btn btn-sm btn-link');
                logout.prepend(icon('ti-logout')); logout.disabled = busy;
                logout.setAttribute('aria-label', `${text(context, 'sessionLogout')}: ${sessionClient(session)}, ${session.remoteAddr || ''}`);
                action.append(logout);
            }
            const activity = node('td', current ? 'md-dashboard-sessions__activity is-current' : 'md-dashboard-sessions__activity', sessionActivity(session, data.currentSessionId, context));
            if (session.lastActivity > 0) activity.title = date(session.lastActivity);
            row.append(device, node('td', 'md-dashboard-sessions__ip', session.remoteAddr || '—'), activity, action);
            body.append(row);
        });
        table.append(head, body);
        mine.append(table);
    }

    /** Uses the same code entry as dashboard notices, including for this session. */
    function confirmDevice(session, host) {
        if (busy || reporting) return;
        showDeviceConfirmation({ data: context.data, deviceId: session.deviceId, host, signal: dialog.signal,
            translate: key => text(context, key), onConfirmed: deviceConfirmed });
    }

    function deviceConfirmed(updated) {
        status.textContent = text(context, 'newDevice.confirmed');
        status.classList.replace('text-danger', 'text-success');
        if (securityEvent?.id === updated.id) {
            Object.assign(securityEvent, updated);
            securityDetails = null;
        }
        refreshSessions(context);
        renderMine();
        tabButtons[0].focus({ preventScroll: true });
    }

    async function remove(sessions) {
        if (busy) return;
        busy = true;
        status.textContent = '';
        mine.querySelectorAll('button').forEach(control => { control.disabled = true; });
        for (const session of sessions) {
            try { await logoutSession(session, context, dialog.signal); }
            catch (error) { if (!dialog.signal.aborted) status.textContent = text(context, 'sessionError'); }
            if (dialog.signal.aborted) return;
        }
        busy = false;
        refreshSessions(context);
        renderMine();
        if (adminsLoaded) await loadAdmins();
        tabButtons[0].focus({ preventScroll: true });
    }

    /** Loads only on tab activation, explicit refresh, or a logout after this tab has already loaded. */
    async function loadAdmins() {
        if (adminsLoading || dialog.signal.aborted) return;
        adminsLoading = true;
        admins.replaceChildren(node('p', '', text(context, 'loading')));
        admins.setAttribute('aria-busy', 'true');
        try {
            const users = await fetchLoggedAdministrators(dialog.signal);
            if (dialog.signal.aborted) return;
            administrators = users;
            adminsLoaded = true;
            renderAdmins();
        } catch (error) {
            if (dialog.signal.aborted) return;
            const message = node('p', 'text-danger', text(context, 'unavailable'));
            message.setAttribute('role', 'alert');
            admins.replaceChildren(message, sessionButton(text(context, 'retry'), loadAdmins, 'btn btn-sm btn-outline-secondary'));
        } finally {
            adminsLoading = false;
            admins.setAttribute('aria-busy', 'false');
        }
    }

    /** Renders the fresh REST summary, retaining feedback for accepted remote invalidations. */
    function renderAdmins() {
        const index = tabDefinitions.findIndex(([id]) => id === 'admins');
        if (index < 0) return;
        const users = [...administrators].sort((a, b) => Number(Boolean(b.current)) - Number(Boolean(a.current)));
        tabButtons[index].textContent = `${text(context, 'sessionAdmins')} (${number(users.length)})`;
        admins.replaceChildren();
        const summary = node('div', 'md-dashboard-sessions__summary');
        const explanation = node('div');
        explanation.append(node('strong', '', text(context, 'sessionAdminsHeading')), node('p', '', text(context, 'sessionAdminsDescription')));
        const refresh = sessionButton(text(context, 'refresh'), loadAdmins, 'btn btn-sm btn-outline-secondary');
        refresh.prepend(icon('ti-refresh')); refresh.disabled = adminBusy;
        summary.append(explanation, refresh); admins.append(summary);
        if (!users.length) { admins.append(node('p', 'text-muted', text(context, 'empty'))); return; }
        const table = node('table', 'md-dashboard-sessions__table md-dashboard-sessions__admins-table');
        const head = node('thead'), heading = node('tr');
        ['sessionUser', 'sessionActiveSessions', 'sessionLastActivity', 'sessionActions'].forEach((key, index) => {
            const cell = node('th'); cell.scope = 'col';
            cell.append(node('span', index === 3 ? 'visually-hidden' : '', text(context, key))); heading.append(cell);
        });
        head.append(heading); table.append(head);
        const body = node('tbody');
        users.forEach(user => {
            const row = node('tr');
            row.dataset.adminUserId = String(user.userId);
            const identity = node('td');
            const device = node('div', 'md-dashboard-sessions__device');
            const initials = String(user.fullName || '').trim().split(/\s+/).filter(Boolean).map(word => word[0]).slice(0, 2).join('').toLocaleUpperCase();
            const avatar = node('span', 'md-dashboard-sessions__avatar', initials); avatar.setAttribute('aria-hidden', 'true');
            const name = node('div'); name.append(node('div', 'md-dashboard-sessions__device-name', user.fullName), node('small', '', user.login));
            device.append(avatar, name); identity.append(device);
            const clients = user.clients || [];
            const count = user.sessionCount;
            const connections = node('td', 'md-dashboard-sessions__admin-connections');
            connections.dataset.label = text(context, 'sessionActiveSessions');
            connections.append(node('div', '', count == null ? '—' : number(count)), node('small', '', clients.filter(Boolean).join(', ')));
            const activity = node('td', `md-dashboard-sessions__activity${user.current ? ' is-current' : ''}`,
                sessionActivity({ lastActivity: user.lastActivity, sessionId: user.current ? 'current' : '' }, 'current', context));
            if (user.lastActivity > 0) activity.title = date(user.lastActivity);
            const actions = node('td', 'md-dashboard-sessions__action');
            if (user.current) actions.append(sessionButton(text(context, 'mySessions'), () => selectTab(0), 'btn btn-sm btn-link'));
            else {
                const mail = adminMail(user, context); if (mail) actions.append(mail);
                if (window.WJ.hasPermission('users.edit_admins')) {
                    if (pendingAdministrators.has(user.userId)) actions.append(node('span', 'small text-muted', text(context, 'sessionPending')));
                    else {
                        const logout = sessionButton(text(context, 'sessionLogout'), () => removeAdministrator(user), 'btn btn-sm btn-link text-danger');
                        logout.prepend(icon('ti-logout')); logout.disabled = adminBusy;
                        logout.setAttribute('aria-label', `${text(context, 'sessionLogout')}: ${user.fullName}`);
                        actions.append(logout);
                    }
                }
            }
            row.append(identity, connections, activity, actions); body.append(row);
        });
        table.append(body); admins.append(table);
    }

    async function removeAdministrator(user) {
        if (adminBusy || user.current || !window.WJ.hasPermission('users.edit_admins')) return;
        adminBusy = true; status.textContent = '';
        admins.querySelectorAll('button').forEach(control => { control.disabled = true; });
        try {
            const response = await fetch('/admin/rest/sessions/logout-administrator', {
                method: 'POST', signal: dialog.signal, credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8', 'X-CSRF-Token': window.csrfToken },
                body: new URLSearchParams({ userId: user.userId })
            });
            const result = await response.json();
            if (!response.ok || result.success !== true) throw new Error('Administrator logout failed');
            if (dialog.signal.aborted) return;
            if (result.pending) pendingAdministrators.add(user.userId);
            context.dashboard.refreshLoggedAdmins();
            adminBusy = false;
            await loadAdmins();
        } catch (error) {
            if (dialog.signal.aborted) return;
            status.textContent = text(context, 'sessionAdminLogoutError');
            adminBusy = false;
            renderAdmins();
        }
        tabButtons[tabDefinitions.findIndex(([id]) => id === 'admins')].focus({ preventScroll: true });
    }

    async function loadHistory(page) {
        history.replaceChildren(node('p', '', text(context, 'loading')));
        history.setAttribute('aria-busy', 'true');
        try {
            const data = await fetchJson(`/admin/rest/sessions/login-history?page=${page}`, dialog.signal);
            if (dialog.signal.aborted) return;
            history.replaceChildren();
            if (!data.content.length) history.append(node('p', 'text-muted', text(context, 'sessionHistoryEmpty')));
            else {
                const table = node('table', 'md-dashboard-sessions__table md-dashboard-sessions__history-table');
                const head = node('thead'), row = node('tr');
                ['sessionDate', 'sessionIp', 'sessionEvent'].forEach(key => { const cell = node('th', '', text(context, key)); cell.scope = 'col'; row.append(cell); });
                head.append(row); table.append(head);
                const body = node('tbody');
                data.content.forEach(item => { const row = node('tr'); row.append(node('td', '', date(item.createDate)), node('td', '', item.ip || '—'), node('td', '', item.description)); body.append(row); });
                table.append(body); history.append(table);
            }
            const pagination = node('div', 'md-dashboard-sessions__pagination');
            pagination.append(node('span', 'small text-muted', text(context, 'sessionHistoryCount', number(data.totalElements))));
            if (data.totalPages > 1) {
                const previous = sessionButton(text(context, 'sessionPrevious'), () => loadHistory(page - 1), 'btn btn-sm btn-outline-secondary');
                const next = sessionButton(text(context, 'sessionNext'), () => loadHistory(page + 1), 'btn btn-sm btn-outline-secondary');
                previous.disabled = data.first; next.disabled = data.last;
                pagination.append(previous, node('span', 'small', `${page + 1} / ${data.totalPages}`), next);
            }
            history.append(pagination);
        } catch (error) {
            if (dialog.signal.aborted) return;
            const message = node('p', 'text-danger', text(context, 'unavailable')); message.setAttribute('role', 'alert');
            history.replaceChildren(message, sessionButton(text(context, 'retry'), () => loadHistory(page), 'btn btn-sm btn-outline-secondary'));
        } finally { history.setAttribute('aria-busy', 'false'); }
    }

    renderMine();
    const password = sessionButton(text(context, 'newDevice.changePassword'), () => {
        password.disabled = true;
        // Wait for dialog cleanup before opening the profile to preserve focus and the modal backdrop.
        dialog.signal.addEventListener('abort', () => window.setTimeout(() => window.openProfileDialog(window.currentUser.userId, true), 0), { once: true });
        dialog.close();
    }, 'btn btn-sm btn-outline-secondary md-dashboard-sessions__password');
    const passwordHelp = node('button', 'btn btn-sm btn-link text-secondary md-dashboard-sessions__password-help');
    passwordHelp.type = 'button';
    passwordHelp.setAttribute('aria-label', text(context, 'newDevice.passwordHelp'));
    passwordHelp.setAttribute('title', text(context, 'newDevice.externalPassword'));
    passwordHelp.setAttribute('data-bs-toggle', 'tooltip');
    passwordHelp.append(icon('ti-info-circle'));
    const passwordControls = node('div', 'd-flex align-items-center gap-1 me-auto');
    passwordControls.append(password, passwordHelp);
    dialog.footer.append(passwordControls, status,
        sessionButton(text(context, 'close'), dialog.close, 'btn btn-sm btn-outline-secondary'));
    sessionTooltips(dialog.footer, dialog.signal);
    selectTab(0);
    dialog.root.addEventListener('shown.bs.modal', () => {
        if (document.activeElement === dialog.root) tabButtons[0].focus({ preventScroll: true });
    }, { once: true });
    tabButtons[0].focus({ preventScroll: true });
}

/** Registers the fixed welcome-panel list and the optional personal session widget. */
export function registerSessionWidgets() {
    const headerAction = (instance, context) => showActiveSessions(context);
    registerWidget({
        type: 'sessions', titleKey: 'admin.dashboard.sessions.js', icon: 'ti-devices', sizes: ['2x3'], mandatory: true, headerAction,
        render({ container, context, signal }) {
            const data = context.data.currentSessions;
            container.append(node('span', 'badge md-dashboard-widget__session-count', number(flattenSessions(data).length)));
            sessionList(container, data, context, signal);
        }
    });
    registerWidget({
        type: 'my-sessions', titleKey: 'admin.dashboard.sessions.js', descriptionKey: 'admin.dashboard.my-sessions.description.js', icon: 'ti-devices',
        sizes: ['1x1', '2x2', '2x3'], defaultSize: '2x2', multiple: true, headerAction,
        render({ container, context, signal, instance }) {
            const data = context.data.currentSessions;
            const count = flattenSessions(data).length;
            if (instance.size === '1x1') {
                const metric = sessionButton(number(count), () => showActiveSessions(context), 'md-dashboard-widget__metric md-dashboard-widget__number md-dashboard-sessions__metric');
                metric.setAttribute('aria-label', text(context, 'sessionsCount', number(count)));
                container.append(metric, node('p', 'md-dashboard-widget__footnote small', text(context, 'activeSessions')));
            } else {
                sessionList(container, data, context, signal);
                container.append(node('p', 'md-dashboard-widget__footnote small', text(context, 'sessionsCount', number(count))));
            }
        }
    });
}
