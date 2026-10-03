import { registerWidget } from './registry';
import { node, text, date, number, icon, containNativeScroll, fetchJson } from './widget-utils';
import { renderLoggedAdmins } from './system-widgets';

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
    const response = await fetch('/admin/rest/removeSession', {
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
        row.append(device, node('strong', 'md-dashboard-widget__session-name', session.browserName),
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

/** Opens the shared session dialog from a linked heading or a system notice. */
export function showActiveSessions(context) {
    if (document.querySelector('.md-dashboard-modal--sessions')) return;
    const dialog = context.dashboard.showDialog(text(context, 'activeSessions'));
    dialog.root.classList.add('md-dashboard-modal--sessions');
    dialog.root.querySelector('.modal-dialog').classList.add('modal-lg', 'modal-dialog-centered', 'modal-dialog-scrollable');
    const tabs = node('div', 'nav nav-tabs md-dashboard-sessions__tabs');
    tabs.setAttribute('role', 'tablist');
    tabs.setAttribute('aria-label', text(context, 'activeSessions'));
    dialog.body.before(tabs);
    const mine = node('section', 'md-dashboard-sessions__mine');
    const admins = node('section', 'md-dashboard-sessions__admins');
    const history = node('section', 'md-dashboard-sessions__history');
    const tabDefinitions = [['mine', 'mySessions', mine]];
    if (Array.isArray(context.data.loggedAdmins) && window.WJ.hasPermission('welcomeShowLoggedAdmins')) {
        tabDefinitions.push(['admins', 'sessionAdmins', admins]);
    }
    tabDefinitions.push(['history', 'sessionHistory', history]);
    let historyLoaded = false;
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
        if (tabDefinitions[index][0] === 'history' && !historyLoaded) { historyLoaded = true; loadHistory(0); }
    }

    const status = node('span', 'md-dashboard-sessions__status text-danger small');
    status.setAttribute('role', 'alert');
    let busy = false;
    function renderMine() {
        const data = context.data.currentSessions;
        const sessions = flattenSessions(data);
        tabButtons[0].textContent = `${text(context, 'mySessions')} (${number(sessions.length)})`;
        mine.replaceChildren();
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
            const name = node('div', 'md-dashboard-sessions__device-name', session.browserName);
            const current = session.sessionId === data.currentSessionId;
            if (current) name.append(node('span', 'md-dashboard-sessions__current', text(context, 'currentSession')));
            details.append(name, node('small', '', text(context, 'sessionLoggedAt', date(session.logonTime))));
            identity.append(glyph, details); device.append(identity);
            const action = node('td', 'md-dashboard-sessions__action');
            if (current) action.append(node('span', 'text-muted', '—'));
            else if (session.pending) action.append(node('span', 'small text-muted', text(context, 'sessionPending')));
            else {
                const logout = sessionButton(text(context, 'sessionLogout'), () => remove([session]), 'btn btn-sm btn-link');
                logout.prepend(icon('ti-logout')); logout.disabled = busy;
                logout.setAttribute('aria-label', `${text(context, 'sessionLogout')}: ${session.browserName}, ${session.remoteAddr || ''}`);
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
        tabButtons[0].focus({ preventScroll: true });
    }

    async function loadHistory(page) {
        history.replaceChildren(node('p', '', text(context, 'loading')));
        history.setAttribute('aria-busy', 'true');
        try {
            const data = await fetchJson(`/rest/audit/my-login-history?page=${page}`, dialog.signal);
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
    if (tabDefinitions.some(([id]) => id === 'admins')) {
        const index = tabDefinitions.findIndex(([id]) => id === 'admins');
        tabButtons[index].textContent += ` (${number(context.data.loggedAdmins.length)})`;
        renderLoggedAdmins({ container: admins, context, signal: dialog.signal });
    }
    dialog.footer.append(node('small', 'text-muted me-auto', text(context, 'sessionsImmediate')), status,
        sessionButton(text(context, 'close'), dialog.close, 'btn btn-sm btn-outline-secondary'));
    selectTab(0);
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
