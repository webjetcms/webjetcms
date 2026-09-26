import { registerWidget } from './registry';
import { node, text, date, number, field, fetchData, empty } from './widget-utils';

/** Returns individual sessions while retaining the originating cluster label. */
export function flattenSessions(data) {
    return (data.userSessions || []).flatMap(cluster => (cluster.userSessions || []).map(session => ({ ...session, cluster: cluster.cluster })))
        .sort((a, b) => Number(b.sessionId === data.currentSessionId) - Number(a.sessionId === data.currentSessionId) || b.logonTime - a.logonTime);
}

function sessionList(container, data, context, signal, limit) {
    const list = node('ul', 'md-dashboard-widget__sessions list-unstyled');
    flattenSessions(data).slice(0, limit).forEach(session => {
        const row = node('li', 'md-dashboard-widget__session');
        row.dataset.sessionLogon = String(session.logonTime);
        const device = node('i', 'ti ti-device-desktop md-dashboard-widget__session-device');
        device.setAttribute('aria-hidden', 'true');
        row.append(device, node('strong', 'md-dashboard-widget__session-name', session.browserName), node('span', 'md-dashboard-widget__session-detail', `${date(session.logonTime)} · ${session.remoteAddr || ''}`));
        row.title = [session.domainName, session.cluster].filter(Boolean).join(' · ');
        if (session.sessionId === data.currentSessionId) row.append(node('span', 'md-dashboard-widget__session-current', text(context, 'currentSession')));
        else {
            const logout = node('button', 'btn btn-sm md-dashboard-widget__session-logout', text(context, 'logoutSession'));
            logout.type = 'button';
            logout.addEventListener('click', async () => {
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
                        logout.replaceWith(node('span', 'small d-block text-muted', text(context, 'sessionPending')));
                    } else {
                        row.remove();
                        context.dashboard.refresh(context.settings.items.find(item => item.type === 'sessions')?.id || 'dashboard-fixed-sessions');
                    }
                } catch (error) {
                    if (signal.aborted) return;
                    logout.disabled = false;
                    let message = row.querySelector('[role="alert"]');
                    if (!message) { message = node('p', 'text-danger small'); message.setAttribute('role', 'alert'); row.append(message); }
                    message.textContent = text(context, 'sessionError');
                }
            });
            row.append(logout);
        }
        list.append(row);
    });
    container.append(list);
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
    const document = new DOMParser().parseFromString(context.labels.changelog || '', 'text/html');
    document.body.querySelectorAll('br').forEach(br => br.replaceWith(document.createTextNode('\n')));
    const blocks = [...document.body.querySelectorAll('p')];
    const content = blocks.length ? blocks.map(block => block.textContent).join('\n\n') : document.body.textContent || '';
    const paragraphs = content.replace(/\\n/g, '\n').split(/\n\s*\n/).map(value => value.trim()).filter(Boolean);
    const version = paragraphs.join(' ').match(/\b20\d{2}\.\d+(?:\.\d+)?\b/)?.[0] || context.config.releaseVersion || '';
    return { version, paragraphs };
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
            const { version, paragraphs } = releaseNews(context);
            if (!paragraphs.length) { empty(container, context); return; }
            const collapsed = Boolean(version && context.settings.acknowledgedNewsVersion === version);
            const header = node('div', 'md-dashboard-widget__news-header');
            header.append(node('strong', '', `WebJET CMS ${version}`));
            const toggle = node('button', 'btn btn-sm md-dashboard-widget__news-toggle', text(context, collapsed ? 'newsMore' : 'newsCollapse'));
            toggle.type = 'button';
            toggle.setAttribute('aria-expanded', String(!collapsed));
            toggle.addEventListener('click', async () => {
                const hadFocus = document.activeElement === toggle;
                const region = container.closest('[data-widget-type="news"]') || container;
                toggle.disabled = true;
                if (!await context.dashboard.acknowledgeNews(collapsed ? null : version)) toggle.disabled = false;
                if (hadFocus && (document.activeElement === document.body || document.activeElement === toggle)) {
                    region.querySelector('.md-dashboard-widget__news-toggle')?.focus({ preventScroll: true });
                }
            });
            if (!collapsed) {
                toggle.setAttribute('aria-label', text(context, 'newsCollapse'));
                toggle.textContent = '';
                const close = node('i', 'ti ti-x'); close.setAttribute('aria-hidden', 'true'); toggle.append(close);
            }
            const summary = node('p', 'md-dashboard-widget__news-summary', paragraphs[0]);
            container.classList.toggle('is-news-collapsed', collapsed);
            if (collapsed) header.append(summary);
            header.append(toggle);
            container.append(header);
            if (!collapsed) {
                const highlights = node('div', 'md-dashboard-widget__news-highlights');
                highlights.append(node('p', '', paragraphs[0]));
                container.append(highlights);
                const details = node('a', 'md-dashboard-widget__news-more', context.labels.seeCompleteChangelog || text(context, 'all'));
                details.href = docsUrl('CHANGELOG'); details.target = '_blank'; details.rel = 'noopener';
                container.append(details);
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
