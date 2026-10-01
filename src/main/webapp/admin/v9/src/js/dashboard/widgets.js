import { getWidget, registerWidget } from './registry';
import { registerUtilityWidgets } from './utility-widgets';
import { registerDataWidgets } from './data-widgets';
import { registerSystemWidgets } from './system-widgets';
import { node, text, localUrl, shortcutUrl, link, icon, field, empty, date, containNativeScroll, pagePreview, fetchJson } from './widget-utils';

/**
 * A same-origin destination from the authorized administration menu.
 * @typedef {Object} MenuEntry
 * @property {string} href - Normalized destination URL.
 * @property {string} title - Menu label.
 * @property {string} [icon] - Own icon or the nearest inherited menu icon.
 */

/**
 * Flattens authorized navigation while retaining distinct submenu destinations.
 * Duplicate destinations use the last visited entry; placeholder roots and unsafe URLs are omitted.
 *
 * @param {Object} context - Menu bootstrap data.
 * @param {Object} context.data - Data containing the authorized navigation tree.
 * @param {Object[]} [context.data.dashboardMenu] - Root menu entries, with children in children or childrens.
 * @param {string} [inheritedIcon] - Fallback icon for roots without their own icon.
 * @returns {MenuEntry[]} Unique destinations in traversal insertion order.
 */
export function menuEntries(context, inheritedIcon) {
    const entries = new Map();
    const visit = (items, inheritedIcon) => (items || []).forEach(item => {
        const href = localUrl(item.href);
        const itemIcon = item.icon || inheritedIcon;
        if (href && !["/", "/admin/v9/#", "/admin/v9/"].includes(href) && item.text) {
            entries.set(href, { href, title: item.text, icon: itemIcon });
        }
        visit(item.childrens || item.children, itemIcon);
    });
    visit(context.data.dashboardMenu, inheritedIcon);
    return [...entries.values()];
}

/**
 * Groups safe destinations by the same main areas and sections as the authorized sidebar.
 * @param {import('./registry').WidgetContext} context - Context containing the authorized menu tree.
 * @returns {{title: string, sections: {title: string, entries: MenuEntry[]}[]}[]} Nonempty groups for cascading shortcut selectors.
 */
function shortcutMenuGroups(context) {
    return (context.data.dashboardMenu || []).map(root => {
        const children = root.childrens || root.children || [];
        const sections = (children.length ? children : [root]).map(section => ({
            title: section.text,
            entries: menuEntries({ data: { dashboardMenu: [section] } }, root.icon)
        })).filter(section => section.entries.length);
        return { title: root.text, sections };
    }).filter(group => group.sections.length);
}

const SHORTCUT_COLORS = [
    ['default', 'surface'], ['mint', 'mint'], ['lavender', 'lavender'], ['blue', 'search-surface'],
    ['amber', 'amber'], ['peach', 'publishing'], ['rose', 'errors']
];

function shortcutBackground(value) {
    return `var(--wj-dashboard-${(SHORTCUT_COLORS.find(([name]) => name === value) || SHORTCUT_COLORS[0])[1]})`;
}

/**
 * Accepts a Tabler name or a single prefixed class, never arbitrary class lists.
 * @param {string} value - User-entered icon name, trimmed before validation.
 * @returns {string|null} A normalized ti-prefixed class, an empty string for inheritance, or null for invalid input.
 */
function shortcutIcon(value) {
    const name = value.trim();
    if (!name) return '';
    const normalized = name.startsWith('ti-') ? name : `ti-${name}`;
    return normalized.length <= 80 && /^ti-[a-z0-9]+(?:-[a-z0-9]+)*$/.test(normalized) ? normalized : null;
}

/**
 * Arranges a traffic overview, compact metrics and content previews for new or reset profiles.
 * Filters registered types by availability and creates shortcuts only for authorized menu destinations.
 *
 * @param {Object} context - Page context with data, labels and config passed to availability callbacks.
 * @returns {{type: string, size?: string, options?: Object}[]} Ordered presets without instance IDs, ready for controller defaults.
 */
export function getDashboardDefaults(context) {
    const items = [
        { type: "search" },
        { type: "sessions", size: "2x3" }, { type: "news", size: "3x2" },
        { type: "traffic", size: "3x3" }, { type: "forms", size: "1x1" },
        { type: "approvals", size: "1x1" }, { type: "errors", size: "1x1" },
        { type: "recent-pages", size: "3x2" }, { type: "referrers", size: "2x2" },
        { type: "publishing", size: "2x2" }, { type: "newsletter", size: "2x2" }
    ];
    const menu = menuEntries(context);
    const shortcuts = ["/admin/v9/webpages/web-pages-list/", "/apps/form/admin/"].filter(href => menu.some(item => item.href === href));
    if (!shortcuts.length && menu.length) shortcuts.push(menu[0].href);
    shortcuts.forEach(href => items.push({ type: "shortcut", options: { href } }));
    return items.filter(item => {
        const definition = getWidget(item.type);
        return definition && (!definition.isAvailable || definition.isAvailable(context));
    });
}

/**
 * Loads the same recent-page list as the Web pages module.
 * @param {import('./registry').WidgetContext} context - Supplies the recent-pages group ID in config.
 * @param {AbortSignal} signal - Render lifetime for the module request.
 * @returns {Promise<Object[]>} Up to six page records ordered by descending creation date.
 */
async function recentPages(context, signal) {
    const params = new URLSearchParams({ groupId: context.config.recentPagesGroupId, size: 6, page: 0, sort: 'dateCreated,desc' });
    const data = await fetchJson(`/admin/rest/web-pages/all?${params}`, signal);
    return data.content;
}

/**
 * Keeps all preview rows accessible when a compact card needs native scrolling.
 * @param {HTMLElement} container - Parent to receive the recent-page list.
 * @param {Object[]} pages - Page records with docId, title, fullPath, perexImage and dateCreated.
 * @param {import('./registry').WidgetContext} context - Supplies list labels.
 * @param {AbortSignal} signal - Removes scroll containment listeners when the render ends.
 */
function recentPagesList(container, pages, context, signal) {
    const list = node('ul', 'md-dashboard-widget__pages');
    list.tabIndex = 0;
    list.setAttribute('aria-label', text(context, 'recent-pages'));
    containNativeScroll(list, signal);
    pages.slice(0, 6).forEach(page => {
        const row = node('li');
        const target = pagePreview(page, `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(page.docId)}`);
        target.classList.add('md-dashboard-widget__page');
        const changed = node('span', 'md-dashboard-widget__page-date', date(page.dateCreated));
        target.append(changed);
        row.append(target);
        list.append(row);
    });
    container.append(list);
}

/** Registers built-in widgets once, even when the overview is reconfigured. */
export function registerDashboardWidgets() {
    if (getWidget("shortcut")) return;
    registerWidget({
        type: "shortcut", titleKey: "admin.dashboard.shortcut.js", descriptionKey: "admin.dashboard.shortcut.description.js",
        icon: "ti-link", sizes: ["1x1"], multiple: true, defaultOptions: { source: "menu", href: "", title: "" },
        isAvailable: () => true,
        getTitle: (instance, context) => instance.options?.title || menuEntries(context).find(item => item.href === instance.options?.href)?.title || text(context, "shortcut"),
        render({ container, options, context }) {
            const customTarget = options.source === 'url' ? shortcutUrl(options.href) : null;
            const item = options.source === 'url' ? (customTarget && options.title?.trim() ? { href: customTarget, title: options.title, icon: 'ti-link' } : null)
                : menuEntries(context).find(entry => entry.href === options.href);
            if (!item) { empty(container, context, "chooseModule"); return; }
            const target = node('a', 'md-dashboard-widget__shortcut', options.title || item.title);
            target.href = item.href;
            target.prepend(icon(options.icon || item.icon));
            (container.closest('.md-dashboard__widget') || target).style.setProperty('--wj-dashboard-shortcut-bg', shortcutBackground(options.color));
            container.append(target);
        },
        /**
         * Builds cascading menu selectors and custom-URL controls with icon and color previews.
         * @param {import('./registry').WidgetArguments} args - Dialog container, shared options and current menu context.
         * @returns {import('./registry').WidgetConfiguration} Reader that validates the destination, custom title and icon before returning shared options.
         */
        configure({ container, options, context }) {
            const groups = shortcutMenuGroups(context);
            const source = field(container, text(context, 'shortcutSource'), [['menu', text(context, 'shortcutSourceMenu')], ['url', text(context, 'shortcutSourceUrl')]], options.source === 'url' || !groups.length ? 'url' : 'menu');
            source.name = 'dashboardShortcutSource';
            source.options[0].disabled = !groups.length;
            const group = field(container, text(context, 'shortcutGroup'), [], '');
            group.name = 'dashboardShortcutGroup';
            const section = field(container, text(context, 'shortcutSection'), [], '');
            section.name = 'dashboardShortcutSection';
            const module = field(container, text(context, 'shortcutTab'), [], '');
            module.name = 'dashboardShortcutMenu';
            const url = field(container, text(context, 'shortcutUrl'), [], options.source === 'url' ? options.href : '', 'text');
            url.name = 'dashboardShortcutUrl';
            url.maxLength = 1024;
            url.placeholder = 'https://…';
            const title = field(container, text(context, 'customTitle'), [], options.title || '', 'text');
            title.name = 'dashboardShortcutTitle';
            const iconInput = field(container, text(context, 'shortcutIcon'), [], '', 'text');
            iconInput.name = 'dashboardShortcutIcon';
            iconInput.maxLength = 80;
            iconInput.placeholder = 'file-text';
            const iconGroup = node('div', 'input-group');
            const iconPreview = node('span', 'input-group-text');
            iconInput.before(iconGroup);
            iconGroup.append(iconPreview, iconInput);
            const hint = node('small', 'text-muted', text(context, 'shortcutIconHint'));
            hint.id = `${iconInput.id}-hint`;
            iconInput.setAttribute('aria-describedby', hint.id);
            iconGroup.after(hint);
            const colors = node('fieldset', 'md-dashboard__shortcut-colors mb-3');
            colors.append(node('legend', 'form-label', text(context, 'shortcutColor')));
            for (const [value] of SHORTCUT_COLORS) {
                const label = node('label', 'md-dashboard__shortcut-swatch');
                const radio = node('input', 'visually-hidden');
                radio.type = 'radio';
                radio.name = `${iconInput.id}-color`;
                radio.value = value;
                radio.checked = value === (SHORTCUT_COLORS.some(([name]) => name === options.color) ? options.color : 'default');
                const name = text(context, `shortcutColor.${value}`);
                radio.setAttribute('aria-label', name);
                const swatch = node('span');
                swatch.title = name;
                swatch.style.backgroundColor = shortcutBackground(value);
                swatch.append(icon('ti-check'));
                label.append(radio, swatch);
                colors.append(label);
            }
            container.append(colors);
            const preview = node('div', 'md-dashboard__shortcut-preview md-dashboard-widget__shortcut');
            preview.setAttribute('aria-label', text(context, 'shortcutPreview'));
            container.append(preview);
            const sections = () => groups[group.value]?.sections || [];
            const destinations = () => sections()[section.value]?.entries || [];
            const destination = () => destinations().find(entry => entry.href === module.value);
            const selectedIcon = () => icon(source.value === 'menu' ? destination()?.icon : 'ti-link').classList[1];
            const color = () => colors.querySelector('input:checked').value;
            const updatePreview = () => {
                const name = shortcutIcon(iconInput.value) || selectedIcon();
                iconPreview.replaceChildren(icon(name));
                preview.replaceChildren(icon(name), document.createTextNode(title.value.trim() || (source.value === 'menu' ? destination()?.title : '') || text(context, 'shortcut')));
                preview.style.setProperty('--wj-dashboard-shortcut-bg', shortcutBackground(color()));
            };
            const updateIcon = () => {
                iconInput.value = selectedIcon().replace(/^ti-/, '');
                updatePreview();
            };
            const fill = (select, values, selected, placeholder) => {
                select.replaceChildren(...[['', text(context, placeholder)], ...values].map(([value, label]) => {
                    const option = node('option', '', label);
                    option.value = value;
                    return option;
                }));
                select.value = selected ?? (values.length === 1 ? values[0][0] : '');
            };
            const update = () => {
                const custom = source.value === 'url';
                for (const select of [group, section, module]) {
                    const hidden = custom || select === module && destinations().length === 1;
                    select.closest('.md-dashboard__field').hidden = hidden;
                    select.closest('.md-dashboard__field').classList.toggle('d-none', hidden);
                    select.disabled = custom || (select === group ? !groups.length : select === section ? !sections().length : !destinations().length);
                }
                url.parentElement.hidden = !custom;
                url.parentElement.classList.toggle('d-none', !custom);
                url.disabled = !custom;
                title.required = custom;
                window.WJ.initSelectPicker?.(container);
            };
            const updateTabs = selected => {
                fill(module, destinations().map(item => [item.href, item.title]), selected, 'shortcutChooseTab');
                update();
            };
            const updateSections = (selected, href) => {
                fill(section, sections().map((item, index) => [String(index), item.title]), selected, 'shortcutChooseSection');
                updateTabs(href);
            };
            const href = options.source === 'url' ? '' : options.href;
            const groupIndex = groups.findIndex(group => group.sections.some(section => section.entries.some(item => item.href === href)));
            const sectionIndex = groups[groupIndex]?.sections.findIndex(section => section.entries.some(item => item.href === href));
            fill(group, groups.map((item, index) => [String(index), item.title]), href ? (groupIndex < 0 ? '' : String(groupIndex)) : undefined, 'shortcutChooseGroup');
            updateSections(sectionIndex >= 0 ? String(sectionIndex) : undefined, href || undefined);
            iconInput.value = (options.icon || selectedIcon()).replace(/^ti-/, '');
            updatePreview();
            group.addEventListener('change', () => { updateSections(); updateIcon(); });
            section.addEventListener('change', () => { updateTabs(); updateIcon(); });
            module.addEventListener('change', updateIcon);
            source.addEventListener('change', () => { update(); if (source.value === 'menu') updateIcon(); else updatePreview(); });
            iconInput.addEventListener('input', updatePreview);
            title.addEventListener('input', updatePreview);
            colors.addEventListener('change', updatePreview);
            return { read: () => {
                const custom = source.value === 'url';
                const href = custom ? shortcutUrl(url.value) : destinations().find(entry => entry.href === module.value)?.href;
                if (!href) throw new Error(text(context, custom ? 'shortcutUrlInvalid' : 'shortcutChooseTarget'));
                if (custom && !title.value.trim()) throw new Error(text(context, 'shortcutTitleRequired'));
                const icon = shortcutIcon(iconInput.value);
                if (icon === null) throw new Error(text(context, 'shortcutIconInvalid'));
                return { options: { source: custom ? 'url' : 'menu', href, title: title.value.trim(), icon, color: color() } };
            } };
        }
    });
    registerWidget({
        type: "recent-pages", titleKey: "admin.dashboard.recent-pages.js", descriptionKey: "admin.dashboard.recent-pages.description.js",
        icon: "ti-history", multiple: true, sizes: ["2x3", "3x2", "3x3"], defaultSize: "3x2",
        headerLink: { href: "/admin/v9/webpages/web-pages-list/", labelKey: "admin.dashboard.allShort.js" },
        isAvailable: () => window.WJ.hasPermission("menuWebpages"),
        async render({ container, context, signal }) {
            const pages = await recentPages(context, signal);
            if (signal.aborted) return;
            if (!pages.length) empty(container, context);
            else recentPagesList(container, pages, context, signal);
        }
    });
    registerUtilityWidgets();
    registerDataWidgets();
    registerSystemWidgets();
}
