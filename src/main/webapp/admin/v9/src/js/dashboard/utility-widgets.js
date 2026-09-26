import { registerWidget } from './registry';
import { node, text, date, number, field, fetchData, empty } from './widget-utils';

/** Returns individual sessions while retaining the originating cluster label. */
export function flattenSessions(data) {
    return (data.userSessions || []).flatMap(cluster => (cluster.userSessions || []).map(session => ({ ...session, cluster: cluster.cluster })))
        .sort((a, b) => Number(b.sessionId === data.currentSessionId) - Number(a.sessionId === data.currentSessionId) || b.logonTime - a.logonTime);
}

/** Resolves the installed Tabler browser glyph without trusting a CSS class from session data. */
function sessionBrowserIcon(browserName) {
    const name = String(browserName || '');
    if (/edge|edg\//i.test(name)) return 'ti-brand-edge';
    if (/firefox|fxios/i.test(name)) return 'ti-brand-firefox';
    if (/chrome|chromium|crios/i.test(name)) return 'ti-brand-chrome';
    if (/safari/i.test(name)) return 'ti-brand-safari';
    return 'ti-device-desktop';
}

/** Lets the native list consume scroll gestures before the administration's smooth scrollbar. */
function containSessionScroll(list, signal) {
    const overflows = () => list.scrollHeight > list.clientHeight;
    const wheel = event => { if (overflows()) event.stopPropagation(); };
    const keyboard = event => {
        const scrolling = ['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' '].includes(event.key);
        if (scrolling && (overflows() || (event.key === ' ' && event.target.closest('button')))) event.stopPropagation();
    };
    let ownsTouch = false;
    const touchStart = event => { ownsTouch = overflows(); if (ownsTouch) event.stopPropagation(); };
    const touchMove = event => { if (ownsTouch) event.stopPropagation(); };
    const touchEnd = event => { if (ownsTouch) event.stopPropagation(); if (!event.touches?.length) ownsTouch = false; };
    const listeners = [['wheel', wheel], ['keydown', keyboard], ['touchstart', touchStart], ['touchmove', touchMove], ['touchend', touchEnd], ['touchcancel', touchEnd]];
    listeners.forEach(([type, handler]) => list.addEventListener(type, handler, { passive: true }));
    signal.addEventListener('abort', () => listeners.forEach(([type, handler]) => list.removeEventListener(type, handler)), { once: true });
}

/** Uses the shared hover/focus/Escape tooltip behavior and releases instances with the widget. */
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

function sessionList(container, data, context, signal, limit) {
    const list = node('ul', 'md-dashboard-widget__sessions list-unstyled');
    list.tabIndex = 0;
    list.setAttribute('aria-label', text(context, 'sessions'));
    containSessionScroll(list, signal);
    flattenSessions(data).slice(0, limit).forEach(session => {
        const row = node('li', 'md-dashboard-widget__session');
        row.dataset.sessionLogon = String(session.logonTime);
        const device = node('i', `ti ${sessionBrowserIcon(session.browserName)} md-dashboard-widget__session-device`);
        device.setAttribute('aria-hidden', 'true');
        row.append(device, node('strong', 'md-dashboard-widget__session-name', session.browserName), node('span', 'md-dashboard-widget__session-detail', `${date(session.logonTime)} · ${session.remoteAddr || ''}`));
        row.title = [session.domainName, session.cluster].filter(Boolean).join(' · ');
        if (session.sessionId === data.currentSessionId) {
            const current = node('span', 'md-dashboard-widget__session-current');
            current.tabIndex = 0;
            current.setAttribute('role', 'img');
            current.setAttribute('aria-label', text(context, 'currentSession'));
            current.setAttribute('title', text(context, 'currentSession'));
            current.setAttribute('data-bs-toggle', 'tooltip');
            row.append(current);
        }
        else {
            const logout = node('button', 'btn btn-sm md-dashboard-widget__session-logout');
            logout.type = 'button';
            logout.setAttribute('aria-label', text(context, 'logoutSession'));
            logout.setAttribute('title', text(context, 'logoutSession'));
            logout.setAttribute('data-bs-toggle', 'tooltip');
            const logoutIcon = node('i', 'ti ti-logout'); logoutIcon.setAttribute('aria-hidden', 'true'); logout.append(logoutIcon);
            logout.addEventListener('click', async () => {
                window.bootstrap?.Tooltip?.getInstance(logout)?.hide();
                logout.disabled = true;
                try {
                    const response = await fetch('/admin/rest/removeSession', {
                        method: 'POST', signal, credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8', 'X-CSRF-Token': window.csrfToken },
                        body: new URLSearchParams({ sessionId: session.sessionId })
                    });
                    const result = await response.text();
                    if (!response.ok || !/\b["']?success["']?\s*:\s*true\b/.test(result)) throw new Error('Session removal failed');
                    if (/\b["']?pending["']?\s*:\s*true\b/.test(result)) {
                        window.bootstrap?.Tooltip?.getInstance(logout)?.dispose();
                        logout.replaceWith(node('span', 'md-dashboard-widget__session-feedback small text-muted', text(context, 'sessionPending')));
                    } else {
                        row.remove();
                        context.dashboard.refresh(context.settings.items.find(item => item.type === 'sessions')?.id || 'dashboard-fixed-sessions');
                    }
                } catch (error) {
                    if (signal.aborted) return;
                    logout.disabled = false;
                    let message = row.querySelector('[role="alert"]');
                    if (!message) { message = node('p', 'md-dashboard-widget__session-feedback text-danger small'); message.setAttribute('role', 'alert'); row.append(message); }
                    message.textContent = text(context, 'sessionError');
                }
            });
            row.append(logout);
        }
        list.append(row);
    });
    container.append(list);
    sessionTooltips(list, signal);
}

async function renderSessions({ container, context, signal }) {
    const result = await fetchData('sessions', {}, signal);
    if (signal.aborted) return;
    const data = result.currentSessions;
    const sessions = flattenSessions(data);
    const manage = node('button', 'btn btn-sm md-dashboard-widget__session-manage', `${text(context, 'manage')} (${number(sessions.length)})`);
    manage.type = 'button';
    manage.setAttribute('aria-label', `${text(context, 'manageSessions')} (${number(sessions.length)})`);
    manage.addEventListener('click', async () => {
        const dialog = context.dashboard.showDialog(text(context, 'manageSessions'));
        try {
            const fresh = await fetchData('sessions', {}, dialog.signal);
            if (!dialog.signal.aborted) sessionList(dialog.body, fresh.currentSessions, context, dialog.signal, Infinity);
        } catch (error) { if (!dialog.signal.aborted) empty(dialog.body, context, 'unavailable'); }
    });
    container.append(manage);
    sessionList(container, data, context, signal, Infinity);
}

/** Uses the announcement's release number, so development rebuilds do not reset acknowledgement. */
export function releaseNews(context) {
    const html = context.labels.changelog || '';
    const document = new DOMParser().parseFromString(html, 'text/html');
    document.body.querySelectorAll('br').forEach(br => br.replaceWith(document.createTextNode('\n')));
    document.body.querySelectorAll('p, h1, h2, h3, h4, h5, h6, li, blockquote').forEach(block => block.append(document.createTextNode('\n\n')));
    const content = document.body.textContent || '';
    const paragraphs = content.replace(/\\n/g, '\n').split(/\n\s*\n/).map(value => value.trim()).filter(Boolean);
    const version = paragraphs.join(' ').match(/\b20\d{2}\.\d+(?:\.\d+)?\b/)?.[0] || context.config.releaseVersion || '';
    return { version, paragraphs, html };
}

function docsUrl(path = '') {
    const language = ['sk', 'cs', 'en'].includes(window.userLng) ? window.userLng : window.userLng === 'cz' ? 'cs' : 'en';
    return `https://docs.webjetcms.sk/latest/${language}/${path}`;
}

/** Registers mandatory security and optional release/help widgets. */
export function registerUtilityWidgets() {
    registerWidget({
        type: 'sessions', titleKey: 'admin.dashboard.sessions.js', icon: 'ti-devices', sizes: ['2x3'], mandatory: true,
        render: renderSessions, renderCollapsed: renderSessions
    });
    registerWidget({
        type: 'news', titleKey: 'admin.dashboard.news.js', icon: 'ti-sparkles', sizes: ['3x2'],
        render({ container, context }) {
            const { version, paragraphs, html } = releaseNews(context);
            if (!html.trim()) { empty(container, context); return; }
            const collapsed = Boolean(version && context.settings.acknowledgedNewsVersion === version);
            const header = node('div', 'md-dashboard-widget__news-header');
            header.append(node('strong', '', `WebJET CMS ${version}`));
            const toggle = node('button', 'btn btn-sm md-dashboard-widget__news-toggle', text(context, collapsed ? 'newsMore' : 'newsCollapse'));
            toggle.type = 'button';
            toggle.setAttribute('aria-expanded', String(!collapsed));
            const toggleIcon = node('i', `ti ti-chevron-${collapsed ? 'down' : 'up'}`);
            toggleIcon.setAttribute('aria-hidden', 'true');
            toggle.prepend(toggleIcon);
            toggle.addEventListener('click', async () => {
                const hadFocus = document.activeElement === toggle;
                const region = container.closest('[data-widget-type="news"]') || container;
                toggle.disabled = true;
                if (!await context.dashboard.acknowledgeNews(collapsed ? null : version)) toggle.disabled = false;
                if (hadFocus && (document.activeElement === document.body || document.activeElement === toggle)) {
                    region.querySelector('.md-dashboard-widget__news-toggle')?.focus({ preventScroll: true });
                }
            });
            const summary = node('p', 'md-dashboard-widget__news-summary', paragraphs[0]);
            container.classList.toggle('is-news-collapsed', collapsed);
            if (collapsed) header.append(summary, toggle);
            container.append(header);
            if (!collapsed) {
                const highlights = node('div', 'md-dashboard-widget__news-highlights');
                // The translated announcement has already passed through WJ.parseMarkdown in overview.pug.
                highlights.innerHTML = html;
                container.append(highlights);
                const actions = node('div', 'md-dashboard-widget__news-actions');
                const details = node('a', 'md-dashboard-widget__news-more', context.labels.seeCompleteChangelog || text(context, 'all'));
                details.href = docsUrl('CHANGELOG'); details.target = '_blank'; details.rel = 'noopener';
                actions.append(details, toggle);
                container.append(actions);
            }
        }
    });
    registerWidget({
        type: 'search', titleKey: 'admin.dashboard.search.js', icon: 'ti-search', sizes: ['fullauto'], defaultOptions: { scope: 'admin' },
        renderCollapsed({ container, options, context }) {
            container.append(node('p', 'small mb-0', text(context, options.scope === 'docs' ? 'searchDocsHint' : 'searchAdminHint')));
        },
        configure({ container, options, context }) {
            const scope = field(container, text(context, 'search'), [['admin', text(context, 'adminSearch')], ['docs', text(context, 'docsSearch')]], options.scope || 'admin');
            return { read: () => ({ options: { scope: scope.value } }) };
        },
        render({ container, options, context, instance }) {
            const form = node('form', 'md-dashboard-widget__search');
            const switcher = node('fieldset', 'md-dashboard-widget__search-scope');
            switcher.append(node('legend', 'visually-hidden', text(context, 'search')));
            const input = node('input', 'form-control'); input.type = 'search'; input.required = true; input.maxLength = 500;
            let scope = options.scope === 'docs' ? 'docs' : 'admin';
            const hint = () => { input.placeholder = text(context, scope === 'docs' ? 'searchDocsHint' : 'searchAdminHint'); input.setAttribute('aria-label', input.placeholder); };
            for (const value of ['admin', 'docs']) {
                const label = node('label', 'md-dashboard-widget__search-option');
                const radio = node('input', 'visually-hidden'); radio.type = 'radio'; radio.name = `scope-${instance.id}`; radio.value = value; radio.checked = scope === value;
                radio.addEventListener('change', () => { scope = value; hint(); });
                label.append(radio, node('span', 'md-dashboard-widget__search-label', text(context, value === 'admin' ? 'adminSearch' : 'docsSearch'))); switcher.append(label);
            }
            hint();
            const group = node('div', 'input-group md-dashboard-widget__search-input');
            const submit = node('button', 'btn md-dashboard-widget__search-submit');
            submit.type = 'submit'; submit.setAttribute('aria-label', text(context, 'searchButton'));
            const searchIcon = node('i', 'ti ti-search'); searchIcon.setAttribute('aria-hidden', 'true'); submit.append(searchIcon);
            group.append(input, submit);
            form.append(group, switcher);
            form.addEventListener('submit', event => {
                event.preventDefault();
                const query = input.value.trim(); if (!query) { input.focus(); return; }
                if (scope === 'docs') window.open(`${docsUrl()}?q=${encodeURIComponent(query)}`, '_blank', 'noopener');
                else window.location.assign(`/admin/v9/search/index/?text=${encodeURIComponent(query)}`);
            });
            container.append(form);
        }
    });
}
