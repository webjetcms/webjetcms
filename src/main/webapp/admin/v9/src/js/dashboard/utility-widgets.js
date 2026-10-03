import { registerWidget } from './registry';
import { registerSessionWidgets } from './session-widgets';
import { node, text, field, empty, containNativeScroll, pagePreview } from './widget-utils';

/**
 * Uses the announcement's release number, so development rebuilds do not reset acknowledgement.
 * Preserves the original HTML and extracts plain-text paragraphs for the collapsed summary.
 *
 * @param {import('./registry').WidgetContext} context - Supplies changelog HTML and the configured release-version fallback.
 * @returns {{version: string, paragraphs: string[], html: string}} Announcement content and its acknowledgement key, which may be empty.
 */
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

/**
 * Builds a documentation URL using a supported administration language, falling back to English.
 * @param {string} [path=''] - Path relative to the localized documentation root.
 * @returns {string} Absolute URL under the latest documentation version.
 */
function docsUrl(path = '') {
    const language = ['sk', 'cs', 'en'].includes(window.userLng) ? window.userLng : window.userLng === 'cz' ? 'cs' : 'en';
    return `https://docs.webjetcms.sk/latest/${language}/${path}`;
}

/**
 * Reuses page lookup with cancellable requests and releases the menu with its widget.
 * Selecting a result navigates to the page editor; failed lookups provide an empty suggestion list.
 *
 * @param {HTMLInputElement} input - Search control enhanced with jQuery UI autocomplete.
 * @param {HTMLElement} group - Parent in which the results menu is placed.
 * @param {AbortSignal} signal - Cancels requests and destroys autocomplete and observers on abort.
 * @returns {(function(boolean): void)|null} A toggle that cancels pending lookup before enabling or disabling suggestions, or null when unavailable.
 */
function pageAutocomplete(input, group, signal) {
    if (!window.WJ.hasPermission('menuWebpages') || !window.$?.fn?.autocomplete) return null;
    const $input = window.$(input);
    let request;
    const cancel = () => { request?.abort(); $input.autocomplete('close'); };
    $input.autocomplete({
        appendTo: group, minLength: 2, delay: 300,
        position: { my: 'left top+2', at: 'left bottom', collision: 'flipfit' },
        async source({ term }, respond) {
            request?.abort();
            if (!term.trim()) { respond([]); return; }
            const current = new AbortController();
            request = current;
            try {
                const response = await fetch(`/admin/skins/webjet6/_doc_autocomplete.jsp?editable=true&docid=${encodeURIComponent(term.trim())}`, {
                    signal: current.signal, credentials: 'same-origin'
                });
                if (!response.ok) throw new Error('Page lookup failed');
                const items = await response.json();
                respond(current.signal.aborted ? [] : items.slice(0, 20));
            } catch (error) { respond([]); }
        },
        focus: () => false,
        select(event, { item }) {
            event.preventDefault();
            window.location.assign(`/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(item.doc_id)}`);
        }
    });
    const autocomplete = $input.autocomplete('instance');
    autocomplete.liveRegion.addClass('visually-hidden');
    autocomplete.menu.element.addClass('md-dashboard-widget__search-results');
    autocomplete._renderItem = (list, item) => {
        const row = pagePreview(item);
        row.classList.add('md-dashboard-widget__page');
        row.title = [item.fullPath, item.label].filter(Boolean).join('\n');
        row.append(node('span', 'md-dashboard-widget__page-date', item.saveDate));
        return window.$(node('li')).append(row).appendTo(list);
    };
    autocomplete._resizeMenu = () => autocomplete.menu.element.outerWidth(input.getBoundingClientRect().width);
    const resize = new ResizeObserver(() => {
        if (!autocomplete.menu.element.is(':visible')) return;
        autocomplete._resizeMenu();
        autocomplete.menu.element.position({ ...autocomplete.options.position, of: $input });
    });
    resize.observe(input);
    containNativeScroll(autocomplete.menu.element[0], signal);
    input.addEventListener('input', cancel, { signal });
    signal.addEventListener('abort', () => { request?.abort(); resize.disconnect(); $input.autocomplete('destroy'); }, { once: true });
    return enabled => { cancel(); $input.autocomplete('option', 'disabled', !enabled); };
}

/** Registers mandatory security and optional release/help widgets. */
export function registerUtilityWidgets() {
    registerSessionWidgets();
    registerWidget({
        type: 'news', titleKey: 'admin.dashboard.news.js', icon: 'ti-sparkles', sizes: ['3x2'],
        render({ container, context }) {
            const { version, paragraphs, html } = releaseNews(context);
            if (!html.trim()) { empty(container, context); return; }
            const collapsed = Boolean(version && context.settings.acknowledgedNewsVersion === version);
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
            container.classList.toggle('is-news-collapsed', collapsed);
            if (collapsed) {
                const header = node('div', 'md-dashboard-widget__news-header');
                header.append(node('p', 'md-dashboard-widget__news-summary', paragraphs[0]), toggle);
                container.append(header);
            } else {
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
        configure({ container, options, context }) {
            const scope = field(container, text(context, 'search'), [['admin', text(context, 'adminSearch')], ['docs', text(context, 'docsSearch')]], options.scope || 'admin');
            return { read: () => ({ options: { scope: scope.value } }) };
        },
        render({ container, options, context, instance, signal }) {
            const form = node('form', 'md-dashboard-widget__search');
            const switcher = node('fieldset', 'md-dashboard-widget__search-scope');
            switcher.append(node('legend', 'visually-hidden', text(context, 'search')));
            const input = node('input', 'form-control'); input.type = 'search'; input.required = true; input.maxLength = 500;
            let scope = options.scope === 'docs' ? 'docs' : 'admin';
            let autocomplete;
            const hint = () => { input.placeholder = text(context, scope === 'docs' ? 'searchDocsHint' : 'searchAdminHint'); input.setAttribute('aria-label', input.placeholder); };
            for (const value of ['admin', 'docs']) {
                const label = node('label', 'md-dashboard-widget__search-option');
                const radio = node('input', 'visually-hidden'); radio.type = 'radio'; radio.name = `scope-${instance.id}`; radio.value = value; radio.checked = scope === value;
                radio.addEventListener('change', () => { scope = value; hint(); autocomplete?.(scope === 'admin'); });
                radio.addEventListener('click', () => {
                    scope = value; hint(); autocomplete?.(scope === 'admin');
                    if (input.value.trim()) form.requestSubmit();
                });
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
            autocomplete = pageAutocomplete(input, group, signal);
            autocomplete?.(scope === 'admin');
        }
    });
}
