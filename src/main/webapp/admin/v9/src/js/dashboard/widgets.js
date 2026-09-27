import { getWidget, listWidgets, registerWidget } from './registry';
import { registerUtilityWidgets } from './utility-widgets';
import { registerDataWidgets } from './data-widgets';
import { registerSystemWidgets } from './system-widgets';
import { node, text, localUrl, shortcutUrl, link, icon, field, empty, date, containNativeScroll, pagePreview } from './widget-utils';

/** Flattens authorized navigation while retaining distinct submenu destinations. */
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

/** Groups safe destinations by the same main areas and sections as the authorized sidebar. */
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

/** Accepts a Tabler name or a single prefixed class, never arbitrary class lists. */
function shortcutIcon(value) {
    const name = value.trim();
    if (!name) return '';
    const normalized = name.startsWith('ti-') ? name : `ti-${name}`;
    return normalized.length <= 80 && /^ti-[a-z0-9]+(?:-[a-z0-9]+)*$/.test(normalized) ? normalized : null;
}

/** Arranges a traffic overview, compact metrics and content previews for new or reset profiles. */
export function getDashboardDefaults(context) {
    const items = [
        { type: "search" },
        { type: "sessions", size: "2x3" }, { type: "news", size: "3x2" },
        { type: "traffic", size: "3x3" }, { type: "forms", size: "1x1" },
        { type: "approvals", size: "1x1" }, { type: "errors", size: "1x1" },
        { type: "recent-pages", size: "3x2" }, { type: "referrers", size: "2x2" },
        { type: "publishing", size: "2x2" }, { type: "newsletter", size: "2x2" },
        { type: "search-terms", size: "3x3" }, { type: "top-pages", size: "3x3" },
        { type: "changed-pages", size: "3x3" }, { type: "audit", size: "3x3" },
        { type: "server-memory", size: "3x3" }, { type: "server-cpu", size: "3x3" },
        { type: "logged-admins", size: "2x2" }
    ];
    const menu = menuEntries(context);
    const shortcuts = ["/admin/v9/webpages/web-pages-list/", "/apps/form/admin/"].filter(href => menu.some(item => item.href === href));
    if (!shortcuts.length && menu.length) shortcuts.push(menu[0].href);
    shortcuts.forEach(href => items.push({ type: "shortcut", options: { href } }));
    for (const definition of listWidgets()) {
        if (!items.some(item => item.type === definition.type) && definition.type !== "shortcut") items.push({ type: definition.type });
    }
    return items.filter(item => {
        const definition = getWidget(item.type);
        return definition && (!definition.isAvailable || definition.isAvailable(context));
    });
}

/** Loads the permission- and domain-filtered preview without session caching. */
async function recentPages(signal) {
    const response = await fetch("/admin/rest/dashboard/recent-pages", { signal, credentials: "same-origin", headers: { Accept: "application/json", "X-CSRF-Token": window.csrfToken } });
    if (!response.ok) throw new Error(`Recent pages request failed (${response.status})`);
    return response.json();
}

/** Keeps all preview rows accessible when a compact card needs native scrolling. */
function recentPagesList(container, pages, context, signal) {
    const list = node('ul', 'md-dashboard-widget__pages');
    list.tabIndex = 0;
    list.setAttribute('aria-label', text(context, 'recent-pages'));
    containNativeScroll(list, signal);
    pages.slice(0, 6).forEach(page => {
        const row = node('li');
        const target = pagePreview(page, `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(page.docId)}`);
        target.classList.add('md-dashboard-widget__page');
        const changed = node('span', 'md-dashboard-widget__page-date', page.date == null ? page.saveDate : date(page.date));
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
        icon: "ti-history", sizes: ["2x3", "3x2", "3x3"], defaultSize: "3x2",
        headerLink: { href: "/admin/v9/webpages/web-pages-list/", labelKey: "admin.dashboard.allShort.js" },
        isAvailable: () => window.WJ.hasPermission("menuWebpages"),
        async render({ container, context, signal }) {
            const pages = await recentPages(signal);
            if (signal.aborted) return;
            if (!pages.length) empty(container, context);
            else recentPagesList(container, pages, context, signal);
        },
        async renderCollapsed({ container, context, signal }) {
            const pages = await recentPages(signal);
            if (signal.aborted) return;
            if (pages.length) container.append(link(pages[0].title, `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(pages[0].docId)}`));
            else empty(container, context);
        }
    });
    registerUtilityWidgets();
    registerDataWidgets();
    registerSystemWidgets();
}
