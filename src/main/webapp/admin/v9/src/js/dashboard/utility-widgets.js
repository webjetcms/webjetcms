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
        const row = node('li', 'mb-2');
        row.dataset.sessionLogon = String(session.logonTime);
        row.append(node('strong', 'd-block', session.browserName), node('span', 'small d-block', `${date(session.logonTime)} · ${session.remoteAddr || ''}`));
        row.title = [session.domainName, session.cluster].filter(Boolean).join(' · ');
        if (session.sessionId === data.currentSessionId) row.append(node('span', 'badge bg-light text-dark', text(context, 'currentSession')));
        else {
            const logout = node('button', 'btn btn-sm btn-outline-secondary', text(context, 'logoutSession'));
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
                        context.dashboard.refresh(context.settings.items.find(item => item.type === 'sessions')?.id);
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

async function renderSessions({ container, context, signal, instance }) {
    const result = await fetchData('sessions', {}, signal);
    if (signal.aborted) return;
    const data = result.currentSessions;
    const sessions = flattenSessions(data);
    if (!instance.collapsed) sessionList(container, data, context, signal, 3);
    const manage = node('button', 'btn btn-sm btn-outline-secondary', `${text(context, 'manageSessions')} (${number(sessions.length)})`);
    manage.type = 'button';
    manage.addEventListener('click', async () => {
        const dialog = context.dashboard.showDialog(text(context, 'manageSessions'));
        try {
            const fresh = await fetchData('sessions', {}, dialog.signal);
            if (!dialog.signal.aborted) sessionList(dialog.body, fresh.currentSessions, context, dialog.signal, Infinity);
        } catch (error) { if (!dialog.signal.aborted) empty(dialog.body, context, 'unavailable'); }
    });
    container.append(manage);
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
        isVisible: (instance, context) => !releaseNews(context).version || context.settings.acknowledgedNewsVersion !== releaseNews(context).version,
        reveal: (instance, context) => context.dashboard.acknowledgeNews(null),
        renderCollapsed({ container, context }) {
            const { version, paragraphs } = releaseNews(context);
            container.append(node('p', 'small mb-0', `WebJET CMS ${version} · ${paragraphs[0]?.slice(0, 120) || ''}${paragraphs[0]?.length > 120 ? '…' : ''}`));
        },
        render({ container, context }) {
            const { version, paragraphs } = releaseNews(context);
            const show = () => {
                container.replaceChildren(node('strong', 'd-block mb-2', `WebJET CMS ${version}`));
                const list = node('ul', 'md-dashboard-widget__list');
                paragraphs.slice(0, 3).forEach(paragraph => list.append(node('li', '', paragraph.length > 200 ? `${paragraph.slice(0, 197)}…` : paragraph)));
                container.append(list);
                const details = node('a', 'd-inline-block mt-2 me-2', context.labels.seeCompleteChangelog || text(context, 'all'));
                details.href = docsUrl('CHANGELOG'); details.target = '_blank'; details.rel = 'noopener';
                const acknowledge = node('button', 'btn btn-sm btn-outline-secondary mt-2', text(context, 'acknowledge'));
                acknowledge.type = 'button';
                acknowledge.addEventListener('click', async () => { acknowledge.disabled = true; if (!await context.dashboard.acknowledgeNews(version)) acknowledge.disabled = false; });
                container.append(details, acknowledge);
            };
            if (version && context.settings.acknowledgedNewsVersion === version) {
                empty(container, context, 'newsRead');
                const button = node('button', 'btn btn-sm btn-outline-secondary', text(context, 'showNews'));
                button.type = 'button'; button.addEventListener('click', show); container.append(button);
            } else if (!paragraphs.length) empty(container, context);
            else show();
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
            const switcher = node('fieldset', 'd-flex gap-3 mb-2');
            switcher.append(node('legend', 'visually-hidden', text(context, 'search')));
            const input = node('input', 'form-control'); input.type = 'search'; input.required = true; input.maxLength = 500;
            let scope = options.scope === 'docs' ? 'docs' : 'admin';
            const hint = () => { input.placeholder = text(context, scope === 'docs' ? 'searchDocsHint' : 'searchAdminHint'); input.setAttribute('aria-label', input.placeholder); };
            for (const value of ['admin', 'docs']) {
                const label = node('label', 'd-flex align-items-center gap-1');
                const radio = node('input', 'form-check-input mt-0'); radio.type = 'radio'; radio.name = `scope-${instance.id}`; radio.value = value; radio.checked = scope === value;
                radio.addEventListener('change', () => { scope = value; hint(); });
                label.append(radio, document.createTextNode(text(context, value === 'admin' ? 'adminSearch' : 'docsSearch'))); switcher.append(label);
            }
            hint();
            const group = node('div', 'input-group');
            const submit = node('button', 'btn btn-primary', text(context, 'searchButton')); submit.type = 'submit'; group.append(input, submit);
            form.append(switcher, group);
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
