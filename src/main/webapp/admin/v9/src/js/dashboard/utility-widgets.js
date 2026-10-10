import { registerWidget } from './registry';
import { registerSessionWidgets } from './session-widgets';
import { registerNewsWidget } from './news-widget';
import { node, text, field, containNativeScroll, pagePreview } from './widget-utils';

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

/** Registers session widgets and the fixed release/search utilities. */
export function registerUtilityWidgets() {
    registerSessionWidgets();
    registerNewsWidget();
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
            const group = node('div', 'md-dashboard-widget__search-input');
            const searchIcon = node('i', 'ti ti-search md-dashboard-widget__search-icon'); searchIcon.setAttribute('aria-hidden', 'true');
            group.append(input, searchIcon);
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
