import { menuEntries } from './admin-menu';
import { node, icon, localUrl, containNativeScroll, fetchJson } from './dashboard/widget-utils';
import { documentationUrl, documentationMarkdown } from './documentation-search';

const text = (key, ...params) => WJ.translate(`admin.quickSearch.${key}.js`, ...params);
const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase();
const pageLink = id => `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(id)}`;
const advancedLink = (scope, query) => `/admin/v9/search/index/?${new URLSearchParams({ scope, text: query })}`;
const sourceIcons = { suggestions: 'ti-file-text', docs: 'ti-file-text', files: 'ti-file', admin: 'ti-settings', users: 'ti-user', translationKeys: 'ti-language', documentation: 'ti-book' };
let searchDialog;

function button(label, className, action) {
    const result = node('button', `btn ${className}`, label);
    result.type = 'button';
    result.addEventListener('click', action);
    return result;
}

/** Installs the shared search trigger and forwards shortcuts from same-origin v9 frames. */
export function initQuickSearch() {
    if (window.openAdminQuickSearch) return;
    const trigger = document.querySelector('.js-search-toggler');
    const isMac = /Mac|iPhone|iPad/.test(navigator.platform);
    const open = () => {
        if (window !== window.top) {
            try { window.parent.openAdminQuickSearch?.(); } catch (error) { /* A foreign parent cannot host this dialog. */ }
            return;
        }
        if (!trigger || document.querySelector('.modal.show:not(#admin-quick-search), dialog[open]')) return;
        searchDialog ||= new QuickSearch(trigger);
        searchDialog.open();
    };
    window.openAdminQuickSearch = open;
    if (trigger) {
        trigger.querySelector('kbd').textContent = isMac ? '⌘ K' : 'Ctrl K';
        trigger.setAttribute('aria-keyshortcuts', isMac ? 'Meta+K' : 'Control+K');
        trigger.addEventListener('click', open);
    }
    document.addEventListener('keydown', event => {
        if (event.defaultPrevented || event.isComposing || event.repeat || event.altKey || event.shiftKey || event.key.toLowerCase() !== 'k') return;
        if (isMac ? !event.metaKey || event.ctrlKey : !event.ctrlKey || event.metaKey) return;
        if (event.target.closest('input, textarea, select, [contenteditable]:not([contenteditable="false"]), [role="textbox"], [role="dialog"], .modal.show')) return;
        if (window === window.top && !trigger) return;
        event.preventDefault();
        open();
    });
}

class QuickSearch {
    constructor(trigger) {
        this.trigger = trigger;
        this.scopes = ['all'];
        if (WJ.hasPermission('menuWebpages')) this.scopes.push('docs', 'files');
        this.scopes.push('admin');
        if (WJ.hasPermission('users_edit_admins') || WJ.hasPermission('users_edit_public_users')) this.scopes.push('users');
        if (WJ.hasPermission('edit_text')) this.scopes.push('translationKeys');
        this.scopes.push('documentation');
        this.menu = menuEntries({ data: { dashboardMenu: window.adminSearchContext?.menu || [] } });
        const userId = window.currentUser?.userId;
        const domain = window.adminSearchContext?.currentDomain;
        this.historyKey = userId != null && domain ? `webjet.quickSearch.v1:${userId}:${domain}` : null;
        this.generation = 0;
        this.root = node('div', 'modal fade md-quick-search');
        this.root.id = 'admin-quick-search';
        this.root.tabIndex = -1;
        this.root.setAttribute('aria-labelledby', 'admin-quick-search-title');
        const dialog = node('div', 'modal-dialog');
        const content = node('div', 'modal-content');
        content.append(node('h2', 'visually-hidden', text('title')));
        content.firstChild.id = 'admin-quick-search-title';
        const search = node('div', 'md-quick-search__input-row');
        this.input = node('input', 'form-control');
        this.input.id = 'admin-quick-search-input';
        this.input.type = 'search';
        this.input.maxLength = 500;
        this.input.autocomplete = 'off';
        this.input.placeholder = text('placeholder');
        this.input.setAttribute('aria-label', text('placeholder'));
        this.input.setAttribute('aria-describedby', 'admin-quick-search-hint');
        this.input.setAttribute('aria-controls', 'admin-quick-search-results');
        this.clear = button(text('clear'), 'btn-link md-quick-search__clear', () => { this.input.value = ''; this.changed(); this.input.focus(); });
        const close = button('', 'md-quick-search__close', () => this.modal.hide());
        close.setAttribute('aria-label', text('close'));
        close.setAttribute('aria-keyshortcuts', 'Escape');
        close.append(node('kbd', '', 'Esc'));
        search.append(icon('ti-search'), this.input, this.clear, close);
        this.scopeBar = node('div', 'md-quick-search__scopes');
        this.scopeBar.setAttribute('role', 'group');
        this.scopeBar.setAttribute('aria-label', text('title'));
        this.chips = new Map(this.scopes.map(scope => {
            const chip = button('', 'md-quick-search__scope', () => this.selectScope(scope));
            chip.dataset.scope = scope;
            this.scopeBar.append(chip);
            return [scope, chip];
        }));
        this.results = node('div', 'md-quick-search__results');
        this.results.id = 'admin-quick-search-results';
        this.results.setAttribute('role', 'region');
        this.results.setAttribute('aria-label', text('results'));
        const hint = node('p', 'md-quick-search__hint', text('keyboardHint'));
        hint.id = 'admin-quick-search-hint';
        this.status = node('div', 'visually-hidden');
        this.status.setAttribute('role', 'status');
        this.status.setAttribute('aria-live', 'polite');
        content.append(search, this.scopeBar, this.results, hint, this.status);
        dialog.append(content);
        this.root.append(dialog);
        document.body.append(this.root);
        this.modal = new bootstrap.Modal(this.root, { keyboard: true, backdrop: true });
        containNativeScroll(this.results, new AbortController().signal);
        this.input.addEventListener('input', () => this.changed());
        this.root.addEventListener('keydown', event => this.keydown(event), true);
        this.root.addEventListener('shown.bs.modal', () => this.input.focus());
        this.root.addEventListener('hide.bs.modal', () => this.cancel());
        this.root.addEventListener('hidden.bs.modal', () => this.trigger.focus());
    }

    /** Starts each visit with the section's scope and an empty query. */
    open() {
        if (this.root.classList.contains('show')) { this.input.focus(); return; }
        const path = window.location.pathname;
        let scope = 'all';
        if (path.startsWith('/admin/v9/webpages/')) scope = 'docs';
        else if (path.startsWith('/admin/v9/files/')) scope = 'files';
        else if (path.startsWith('/admin/v9/users/')) scope = 'users';
        else if (path.startsWith('/admin/v9/settings/translation-keys/')) scope = 'translationKeys';
        else if (path.startsWith('/admin/v9/search/')) {
            scope = { 'pills-webPages-tab': 'docs', 'pills-files-tab': 'files', 'pills-translationKeys-tab': 'translationKeys' }[document.querySelector('#pills-tabsFilter .active')?.id] || 'all';
        }
        this.scope = this.scopes.includes(scope) ? scope : 'all';
        this.input.value = '';
        this.query = '';
        this.states = {};
        this.history = this.readHistory();
        this.render();
        this.modal.show();
    }

    cancel() {
        clearTimeout(this.timer);
        clearTimeout(this.suggestionsTimer);
        clearTimeout(this.documentationTimer);
        this.controller?.abort();
        this.generation++;
    }

    changed() {
        this.query = this.input.value.trim();
        if (this.query.length > 0) this.search();
        else {
            this.cancel();
            this.states = {};
            this.render();
        }
    }

    selectScope(scope) {
        if (scope === this.scope) return;
        this.scope = scope;
        if (this.query.length > 0) this.prepareScopeSources();
        this.render();
        this.input.focus();
    }

    /** Debounces autocomplete by 300 ms, REST by 1000 ms and eligible documentation queries by 2000 ms. */
    search() {
        this.cancel();
        this.controller = new AbortController();
        this.states = {};
        const words = normalize(this.query).split(/\s+/);
        const entries = this.menu.filter(item => words.every(word => normalize(`${item.title} ${item.path}`).includes(word)))
            .sort((a, b) => Number(words.every(word => normalize(b.title).includes(word))) - Number(words.every(word => normalize(a.title).includes(word))));
        if (this.query.length >= 2) this.states.admin = { items: entries, total: entries.length };
        const sources = this.query.length >= 2 ? this.scopes.filter(scope => ['docs', 'files', 'users', 'translationKeys'].includes(scope)) : [];
        sources.forEach(source => { this.states[source] = { items: [], loading: true, pending: true }; });
        this.prepareScopeSources(true);
        this.render();
        this.timer = setTimeout(() => this.loadRest(), 1000);
    }

    /** Adds optional sources once per query, preserving cached results and active requests across scopes. */
    prepareScopeSources(debounce = false) {
        if (!this.states.documentation && window.adminSearchContext?.documentationSearchUrl && ['all', 'documentation'].includes(this.scope)) {
            const waiting = this.query.length < 8;
            const state = { items: [], pending: true, ready: false, waiting, loading: !waiting };
            if (this.scope === 'documentation') this.states = { documentation: state, ...this.states };
            else this.states.documentation = state;
            if (!waiting) this.documentationTimer = setTimeout(() => this.loadRest(true), 2000);
        }
        if (!this.states.suggestions && this.query.length >= 2 && this.scopes.includes('docs') && ['all', 'docs'].includes(this.scope)) {
            this.states.suggestions = { items: [], loading: true, pending: true };
            if (debounce) this.suggestionsTimer = setTimeout(() => this.load('suggestions'), 300);
            else this.load('suggestions');
        }
    }

    /** Drains ready sources sequentially; Enter or the documentation timer releases the AI request. */
    async loadRest(includeDocumentation = false) {
        clearTimeout(this.timer);
        if (includeDocumentation && this.states.documentation?.pending) {
            clearTimeout(this.documentationTimer);
            Object.assign(this.states.documentation, { ready: true, waiting: false, loading: true });
            this.render();
        }
        const generation = this.generation;
        if (this.restGeneration === generation || Object.entries(this.states).some(([source, state]) => source !== 'suggestions' && state.loading && !state.pending)) return;
        this.restGeneration = generation;
        try {
            while (generation === this.generation) {
                const next = Object.entries(this.states).find(([source, state]) => source !== 'suggestions' && state.pending
                    && (source !== 'documentation' || state.ready));
                if (!next) break;
                await this.load(next[0]);
            }
        } finally {
            if (this.restGeneration === generation) this.restGeneration = null;
        }
    }

    requestUrl(source, page) {
        if (source === 'documentation') {
            const url = new URL(window.adminSearchContext.documentationSearchUrl, window.location.origin);
            if (!documentationUrl(url.href, url.href)) throw new Error('Invalid documentation search URL');
            const language = window.userLng === 'cz' ? 'cs' : ['sk', 'cs', 'en'].includes(window.userLng) ? window.userLng : 'en';
            url.searchParams.set('query', this.query);
            url.searchParams.set('language', language);
            url.searchParams.set('directory', `/${language}/`);
            return url.href;
        }
        const params = new URLSearchParams({ searchText: this.query, size: '10', page: String(page), sort: 'id,asc' });
        if (source === 'suggestions') return `/admin/skins/webjet6/_doc_autocomplete.jsp?${new URLSearchParams({ editable: 'true', cacheOnly: 'true', url: this.query })}`;
        if (source === 'users') return `/admin/rest/users/all?${params}`;
        if (source === 'translationKeys') { params.set('isSearchVersion', 'true'); return `/admin/v9/settings/translation-keys/all?${params}`; }
        params.set('searchType', source);
        if (source === 'docs') params.set('searchRootGroupId', '-1');
        return `/admin/rest/search/all?${params}`;
    }

    /** Ignores responses from prior queries or closed dialogs, including failed requests. */
    async load(source, page = 0) {
        const generation = this.generation;
        const state = this.states[source];
        state.pending = false;
        const controlKey = state.error ? `retry-${source}` : page ? `more-${source}` : null;
        const firstNewRow = page ? this.results.querySelector(`[data-source="${source}"]`)?.querySelectorAll('.md-quick-search__result').length || 0 : 0;
        state.retrying = state.error;
        state.loading = true;
        state.error = false;
        state.requestedPage = page;
        this.render();
        try {
            let data;
            if (source === 'documentation') {
                // Public documentation must not receive administration credentials or trigger a CSRF preflight.
                const response = await fetch(this.requestUrl(source, page), { signal: this.controller.signal, credentials: 'omit', headers: { Accept: 'application/json' } });
                if (!response.ok) throw new Error('Documentation search failed');
                data = await response.json();
                if (data.error) throw new Error('Documentation search failed');
            } else data = await fetchJson(this.requestUrl(source, page), this.controller.signal);
            const items = source === 'suggestions' ? data : source === 'documentation' ? data.results : data.content;
            if (!Array.isArray(items)) throw new Error('Invalid search response');
            if (source === 'documentation' && items.some(item => !item || typeof item !== 'object')) throw new Error('Invalid documentation result');
            if (generation !== this.generation) return;
            state.items = page ? [...state.items, ...items] : items;
            state.total = source === 'suggestions' ? null : source === 'documentation' ? items.length : data.totalElements;
            if (source === 'documentation') state.answer = typeof data.answer === 'string' ? data.answer.trim() : '';
            state.page = page;
            state.last = ['suggestions', 'documentation'].includes(source) || data.last === true || state.items.length >= state.total || items.length === 0;
        } catch (error) {
            if (generation !== this.generation || error.name === 'AbortError') return;
            state.error = true;
        }
        if (generation !== this.generation) return;
        const keepControlFocus = controlKey && this.results.contains(document.activeElement) && document.activeElement.dataset.focusKey === controlKey;
        state.loading = false;
        state.retrying = false;
        this.render(keepControlFocus ? { source, index: firstNewRow } : null);
        if (source !== 'suggestions') this.loadRest();
    }

    render(focusResult) {
        const focusKey = this.results.contains(document.activeElement) ? document.activeElement.dataset.focusKey : null;
        this.clear.hidden = !this.input.value;
        for (const [scope, chip] of this.chips) {
            const state = this.states[scope];
            const loading = scope === 'all' ? Object.values(this.states).some(result => result.loading)
                : Boolean(state?.loading || (scope === 'docs' && this.states.suggestions?.loading));
            if (!loading || !chip.querySelector('.md-quick-search__spinner')) {
                chip.replaceChildren(node('span', '', text(scope)));
                if (loading) {
                    const spinner = node('span', 'spinner-border md-quick-search__spinner');
                    spinner.setAttribute('aria-hidden', 'true');
                    chip.append(spinner, node('span', 'visually-hidden', text('loading')));
                } else if (state?.total != null && !state.error) chip.append(node('span', 'md-quick-search__count', state.total));
            }
            chip.setAttribute('aria-busy', String(loading));
            chip.setAttribute('aria-pressed', String(scope === this.scope));
            chip.classList.toggle('md-quick-search__scope--empty', state?.total === 0 && !state.error && !state.answer && !loading);
            if (scope === 'docs') chip.title = text('countsHint');
        }
        this.results.replaceChildren();
        if (!this.query) this.renderHistory();
        else if (this.query.length < 2 && (!this.states.documentation || !['all', 'documentation'].includes(this.scope))) this.results.append(node('p', 'md-quick-search__empty', text('minLength')));
        else {
            const sources = this.query.length < 2 ? ['documentation'] : this.scope === 'all' ? ['suggestions', ...this.scopes.filter(scope => scope !== 'all')] : this.scope === 'docs' ? ['suggestions', 'docs'] : [this.scope];
            let hasResults = false;
            let unsettled = false;
            sources.forEach(source => {
                if (source === 'documentation' && !window.adminSearchContext?.documentationSearchUrl) return;
                const state = this.states[source];
                if (!state) { if (this.scopes.includes(source)) unsettled = true; return; }
                hasResults ||= state.items.length > 0 || Boolean(state.answer);
                unsettled ||= state.loading || state.error || state.waiting;
                if (state.items.length || state.answer || state.loading || state.error || state.waiting) this.renderGroup(source, state);
            });
            if (!hasResults && !unsettled && (this.scope !== 'documentation' || window.adminSearchContext?.documentationSearchUrl)) this.renderEmpty();
            if (!Object.keys(this.states).length && this.scope !== 'documentation') this.results.append(node('p', 'md-quick-search__empty', text('loading')));
            if (sources.includes('documentation') && !window.adminSearchContext?.documentationSearchUrl) this.renderDocumentation();
            if (['docs', 'files', 'users', 'translationKeys'].includes(this.scope)) {
                const href = this.scope === 'users' ? `/admin/v9/users/user-list/?${new URLSearchParams({ searchText: this.query })}` : advancedLink(this.scope, this.query);
                const link = this.resultLink(text('handoff', text(this.scope)), href, 'md-quick-search__handoff');
                this.results.append(link);
            }
        }
        if (focusKey || focusResult) {
            let replacement;
            if (focusResult) {
                const section = this.results.querySelector(`[data-source="${focusResult.source}"]`);
                const rows = [...(section?.querySelectorAll('.md-quick-search__result') || [])];
                replacement = section?.querySelector('.md-quick-search__retry') || rows[focusResult.index] || rows.at(-1);
            } else {
                replacement = [...this.results.querySelectorAll('[data-focus-key]')].find(item => item.dataset.focusKey === focusKey);
            }
            (replacement || this.input).focus({ preventScroll: !focusResult });
        }
        const busy = Object.values(this.states).some(state => state.loading);
        this.results.setAttribute('aria-busy', String(busy));
        this.status.textContent = busy ? text('loading') : Object.entries(this.states).filter(([, state]) => state.total != null && !state.error).map(([source, state]) => text('resultCount', text(source), state.total)).join('. ');
    }

    group(source, title) {
        const section = node('section', 'md-quick-search__group');
        section.dataset.source = source;
        const heading = node('div', 'md-quick-search__group-heading');
        heading.append(node('h3', '', title));
        section.append(heading);
        this.results.append(section);
        return section;
    }

    renderGroup(source, state) {
        const section = this.group(source, text(source === 'docs' ? 'contentResults' : source));
        const loading = source === 'suggestions' ? state.loading
            : Object.entries(this.states).some(([name, result]) => name !== 'suggestions' && result.loading);
        if (this.scope === 'all' && state.total > 3) {
            const all = button(text('allResults', state.total), 'btn-link', () => this.selectScope(source));
            all.dataset.focusKey = `scope-${source}`;
            section.firstChild.append(all);
        }
        const items = this.scope === 'all' ? state.items.slice(0, 3) : state.items;
        if (source === 'documentation') this.renderDocumentationResults(section, state, items);
        else items.forEach(item => {
            const row = this.row(source, item);
            if (row) section.append(row);
        });
        if (source === 'suggestions' && this.scope === 'docs' && state.items.length === 20) section.append(node('p', 'md-quick-search__note', text('suggestionsLimit')));
        if (state.loading) section.append(node('p', 'md-quick-search__note', text('loading')));
        if (state.waiting) section.append(node('p', 'md-quick-search__note', text('documentationMinLength')));
        if (state.error || state.retrying) {
            const error = node('div', 'md-quick-search__error');
            if (state.error) {
                error.setAttribute('role', 'alert');
                error.append(icon('ti-alert-circle'), node('span', '', text('unavailable')));
            }
            if (source === 'documentation') {
                let href;
                try {
                    const endpoint = new URL(window.adminSearchContext.documentationSearchUrl, window.location.origin);
                    href = documentationUrl('/', endpoint.href);
                } catch (error) { /* An invalid endpoint has no documentation origin to open. */ }
                if (href) {
                    const link = this.resultLink(text('documentationAll'), href, 'btn btn-outline-secondary btn-sm md-quick-search__documentation-link', true);
                    link.append(node('span', 'visually-hidden', text('newTab')));
                    error.append(link);
                }
            } else {
                const retry = button(text('retry'), 'btn-outline-secondary btn-sm md-quick-search__retry', () => { if (!loading) this.load(source, state.requestedPage); });
                retry.dataset.focusKey = `retry-${source}`;
                retry.setAttribute('aria-disabled', String(loading));
                error.append(retry);
            }
            section.append(error);
        }
        if (this.scope === source && !state.last && !state.error && !state.retrying && state.page != null) {
            const more = button(text('more'), 'btn-link md-quick-search__more', () => { if (!loading) this.load(source, state.page + 1); });
            more.dataset.focusKey = `more-${source}`;
            more.setAttribute('aria-disabled', String(loading));
            section.append(more);
        }
    }

    /** Maps only existing destinations; response strings are always rendered as text. */
    row(source, item) {
        let title, detail, href, image, external = false;
        if (source === 'suggestions' || source === 'docs') {
            const id = item.doc_id ?? item.docId ?? item.id;
            if (id == null) return null;
            title = item.title;
            detail = [item.fullPath, item.label || item.virtualPath].filter(Boolean).join(' · ');
            href = pageLink(id);
            image = item.perexImage;
        } else if (source === 'files') {
            title = item.navbar || item.title;
            detail = item.externalLink;
            if (item.publishStart) {
                const date = new Date(item.publishStart);
                if (!Number.isNaN(date.getTime())) detail += ` · ${date.toLocaleDateString(window.userLng === 'cz' ? 'cs' : window.userLng || 'sk')}`;
            }
            href = localUrl(item.externalLink);
            if (!href) return null;
            external = true;
        } else if (source === 'users') {
            if (item.id == null) return null;
            title = [item.firstName, item.lastName].filter(Boolean).join(' ') || item.login;
            detail = [item.login, item.email].filter(Boolean).join(' · ');
            href = `/admin/v9/users/user-list/?id=${encodeURIComponent(item.id)}`;
        } else if (source === 'translationKeys') {
            title = item.key;
            detail = item.fieldA || item.originalValueA || '';
            href = `/admin/v9/settings/translation-keys/?key=${encodeURIComponent(item.key)}`;
        } else {
            title = item.title;
            detail = item.path;
            href = item.href;
        }
        const row = this.resultLink('', href, 'md-quick-search__result', external);
        row.dataset.focusKey = `${source}:${href}`;
        const thumbnail = node('span', 'md-quick-search__icon');
        thumbnail.append(icon(item.icon || sourceIcons[source]));
        const imagePath = typeof image === 'string' && image.startsWith('/') ? localUrl(image) : null;
        if (imagePath) {
            const preview = node('img');
            preview.alt = '';
            preview.loading = 'lazy';
            preview.src = `/thumb${imagePath}?w=76&h=76&ip=6`;
            preview.addEventListener('error', () => preview.remove(), { once: true });
            thumbnail.append(preview);
        }
        const content = node('span', 'md-quick-search__content');
        content.append(node('span', 'md-quick-search__title', title));
        if (detail) content.append(node('span', 'md-quick-search__detail', detail));
        row.append(thumbnail, content);
        if (external) row.append(node('span', 'visually-hidden', text('newTab')));
        return row;
    }

    resultLink(title, href, className, external = false) {
        const link = node('a', className, title);
        link.href = href;
        link.dataset.focusKey = href;
        if (external) { link.target = '_blank'; link.rel = 'noopener'; }
        link.addEventListener('click', () => this.remember());
        return link;
    }

    renderDocumentation() {
        const section = this.group('documentation', `${text('documentation')} · ${text('newTab')}`);
        const lng = window.userLng === 'cz' ? 'cs' : ['sk', 'cs', 'en'].includes(window.userLng) ? window.userLng : 'en';
        const link = this.resultLink(text('documentationSearch'), `https://docs.webjetcms.sk/latest/${lng}/?q=${encodeURIComponent(this.query)}`, 'md-quick-search__result', true);
        link.prepend(icon('ti-book'));
        section.append(link);
    }

    /** Displays the optional AI answer and Markdown excerpts without nesting interactive links. */
    renderDocumentationResults(section, state, items) {
        if (!state.answer && !items.length) return;
        const endpoint = new URL(window.adminSearchContext.documentationSearchUrl, window.location.origin).href;
        if (state.answer) {
            const answer = node('div', 'md-quick-search__answer');
            const heading = node('h4', '');
            heading.append(icon('ti-sparkles'), document.createTextNode(text('documentationAnswer')));
            const previewLength = 450;
            const collapsed = state.answer.length > previewLength && !state.answerExpanded;
            const paragraphEnd = state.answer.lastIndexOf('\n\n', previewLength);
            const preview = collapsed && paragraphEnd > 0 ? `${state.answer.slice(0, paragraphEnd)}\n\n…` : state.answer;
            const content = documentationMarkdown(preview, endpoint);
            // A single long paragraph uses a text preview so truncation cannot expose unfinished Markdown.
            if (collapsed && paragraphEnd <= 0) content.replaceChildren(node('p', '', `${content.textContent.slice(0, previewLength)}…`));
            answer.append(heading, content);
            if (state.answer.length > previewLength) {
                const toggle = button(text(state.answerExpanded ? 'documentationHideAnswer' : 'documentationShowAnswer'), 'btn-link md-quick-search__answer-toggle', () => {
                    state.answerExpanded = !state.answerExpanded;
                    this.render();
                });
                toggle.setAttribute('aria-expanded', String(Boolean(state.answerExpanded)));
                toggle.dataset.focusKey = 'documentation-answer';
                answer.append(toggle);
            }
            section.append(answer);
        }
        items.forEach((item, index) => {
            const href = documentationUrl(item.url, endpoint);
            const card = node('article', 'md-quick-search__documentation-result');
            const heading = node('h4');
            const title = typeof item.title === 'string' && item.title.trim() ? item.title : text('documentation');
            if (href) {
                const link = this.resultLink(title, href, 'md-quick-search__result', true);
                link.dataset.focusKey = `documentation:${index}:${href}`;
                link.prepend(icon('ti-book'));
                link.append(icon('ti-external-link'), node('span', 'visually-hidden', text('newTab')));
                heading.append(link);
            } else heading.append(node('span', 'md-quick-search__documentation-title', title));
            card.append(heading, documentationMarkdown(item.snippet, documentationUrl(item.sourcePath, endpoint) || href || endpoint));
            section.append(card);
        });
        section.querySelectorAll('.md-quick-search__markdown a').forEach((link, index) => {
            link.dataset.focusKey = `documentation-markdown:${index}:${link.href}`;
            link.addEventListener('click', () => this.remember());
            link.append(node('span', 'visually-hidden', text('newTab')));
        });
    }

    renderEmpty() {
        const empty = node('div', 'md-quick-search__empty');
        empty.append(icon('ti-search'), node('p', '', text('empty', this.query, text(this.scope))));
        if (this.scope !== 'all') empty.append(button(text('everywhere'), 'btn-outline-secondary btn-sm', () => this.selectScope('all')));
        this.results.append(empty);
    }

    /** Stores confirmed terms only; storage is optional and scoped to the signed-in account/domain. */
    readHistory() {
        try {
            const saved = this.historyKey ? JSON.parse(localStorage.getItem(this.historyKey) || '[]') : [];
            return Array.isArray(saved) ? saved.filter(value => typeof value === 'string' && value.trim().length >= 2 && value.length <= 500).slice(0, 10) : [];
        } catch (error) { return []; }
    }

    saveHistory() {
        try { if (this.historyKey) localStorage.setItem(this.historyKey, JSON.stringify(this.history)); } catch (error) { /* Search remains available without browser storage. */ }
    }

    remember() {
        if (this.query.length < 2) return;
        this.history = [this.query, ...this.history.filter(value => normalize(value) !== normalize(this.query))].slice(0, 10);
        this.saveHistory();
    }

    renderHistory() {
        if (this.history.length) {
            const section = this.group('history', text('recent'));
            section.firstChild.append(button(text('clearHistory'), 'btn-link', () => { this.history = []; this.saveHistory(); this.render(); this.input.focus(); }));
            this.history.forEach(query => {
                const row = node('div', 'md-quick-search__history');
                const choose = button(query, 'md-quick-search__result', () => { this.input.value = query; this.changed(); this.input.focus(); });
                choose.prepend(icon('ti-history'));
                const remove = button('', 'md-quick-search__remove-history', () => { this.history = this.history.filter(value => value !== query); this.saveHistory(); this.render(); this.input.focus(); });
                remove.setAttribute('aria-label', text('removeHistory', query));
                remove.append(icon('ti-x'));
                row.append(choose, remove);
                section.append(row);
            });
        }
        const destinations = ['/admin/v9/webpages/web-pages-list/', '/admin/v9/files/index/', '/admin/v9/users/user-list/'];
        const links = destinations.map(href => this.menu.find(item => item.href === href)).filter(Boolean);
        if (links.length) {
            const section = this.group('quickLinks', text('quickLinks'));
            links.forEach(item => section.append(this.row('admin', item)));
        }
    }

    keydown(event) {
        if (event.isComposing) return;
        if (event.target === this.input && event.altKey && ['ArrowLeft', 'ArrowRight'].includes(event.key)) {
            event.preventDefault();
            const offset = event.key === 'ArrowRight' ? 1 : -1;
            this.selectScope(this.scopes[(this.scopes.indexOf(this.scope) + offset + this.scopes.length) % this.scopes.length]);
        } else if (!event.altKey && !event.ctrlKey && !event.metaKey && ['ArrowDown', 'ArrowUp'].includes(event.key) && (event.target === this.input || event.target.matches('.md-quick-search__result'))) {
            const rows = [...this.results.querySelectorAll('.md-quick-search__result')];
            if (!rows.length) return;
            event.preventDefault();
            const index = rows.indexOf(event.target);
            const next = event.key === 'ArrowDown' ? index + 1 : index < 0 ? rows.length - 1 : index - 1;
            if (next < 0 || next >= rows.length) this.input.focus();
            else rows[next].focus();
        } else if (event.target === this.input && event.key === 'Enter') {
            event.preventDefault();
            if (!event.repeat && this.query.length > 0) this.loadRest(['all', 'documentation'].includes(this.scope));
        }
    }
}
