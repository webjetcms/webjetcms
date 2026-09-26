import { getWidget, listWidgets, registerWidget } from './registry';
import { registerUtilityWidgets } from './utility-widgets';
import { registerDataWidgets } from './data-widgets';
import { node, text, localUrl, shortcutUrl, link, icon, field, empty, footer, date } from './widget-utils';

/** Flattens authorized navigation while retaining distinct submenu destinations. */
export function menuEntries(context) {
    const entries = new Map();
    const visit = items => (items || []).forEach(item => {
        const href = localUrl(item.href);
        if (href && !["/", "/admin/v9/#", "/admin/v9/"].includes(href) && item.text) {
            entries.set(href, { href, title: item.text, icon: item.icon });
        }
        visit(item.childrens || item.children);
    });
    visit(context.data.dashboardMenu);
    return [...entries.values()];
}

/** Arranges a traffic overview, compact metrics and content previews for new or reset profiles. */
export function getDashboardDefaults(context) {
    const items = [
        { type: "search" },
        { type: "sessions", size: "2x3" }, { type: "news", size: "3x2" },
        { type: "traffic", size: "3x3" }, { type: "forms", size: "1x1" },
        { type: "approvals", size: "1x1" }, { type: "errors", size: "1x1" },
        { type: "recent-pages", size: "3x3" }, { type: "referrers", size: "3x3" },
        { type: "publishing", size: "2x3" }, { type: "newsletter", size: "2x2" },
        { type: "search-terms", size: "2x3" }, { type: "top-pages", size: "3x3" }
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

/** Presents the authorized page thumbnail with a decorative file icon fallback. */
function pageThumbnail(page) {
    const thumbnail = node('span', 'md-dashboard-widget__page-image');
    const fallback = icon('ti-file-text');
    thumbnail.append(fallback);
    const source = typeof page.perexImage === 'string' && page.perexImage.startsWith('/') && !page.perexImage.startsWith('//') ? localUrl(page.perexImage) : null;
    if (source) {
        const image = node('img');
        image.alt = '';
        image.loading = 'lazy';
        image.width = 38;
        image.height = 38;
        image.addEventListener('error', () => { image.remove(); fallback.hidden = false; }, { once: true });
        fallback.hidden = true;
        image.src = `/thumb${source}?w=76&h=76&ip=5`;
        thumbnail.append(image);
    }
    return thumbnail;
}

/** Keeps titles, sections and real edit dates readable in both preview sizes. */
function recentPagesList(container, pages, size) {
    const list = node('ul', 'md-dashboard-widget__pages');
    pages.slice(0, size === '2x3' ? 5 : 6).forEach(page => {
        const row = node('li');
        const target = link('', `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(page.docId)}`, 'md-dashboard-widget__page');
        const content = node('span', 'md-dashboard-widget__page-content');
        const section = page.fullPath?.endsWith(`/${page.title}`) ? page.fullPath.slice(0, -(page.title.length + 1)) || '/' : page.fullPath;
        content.append(node('span', 'md-dashboard-widget__page-title', page.title), node('span', 'md-dashboard-widget__page-section', section));
        const changed = node('span', 'md-dashboard-widget__page-date', page.date == null ? page.saveDate : date(page.date));
        target.title = page.fullPath || page.title;
        target.append(pageThumbnail(page), content, changed);
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
            target.prepend(icon(item.icon));
            container.append(target);
        },
        configure({ container, options, context }) {
            const entries = menuEntries(context);
            const source = field(container, text(context, 'shortcutSource'), [['menu', text(context, 'shortcutSourceMenu')], ['url', text(context, 'shortcutSourceUrl')]], options.source === 'url' || !entries.length ? 'url' : 'menu');
            source.name = 'dashboardShortcutSource';
            source.options[0].disabled = !entries.length;
            const module = field(container, text(context, "module"), entries.map(item => [item.href, item.title]), options.source === 'url' ? entries[0]?.href : options.href || entries[0]?.href);
            module.name = 'dashboardShortcutMenu';
            const url = field(container, text(context, 'shortcutUrl'), [], options.source === 'url' ? options.href : '', 'text');
            url.name = 'dashboardShortcutUrl';
            url.maxLength = 1024;
            url.placeholder = 'https://…';
            const title = field(container, text(context, "customTitle"), [], options.title || "", "text");
            title.name = 'dashboardShortcutTitle';
            const update = () => {
                const custom = source.value === 'url';
                module.parentElement.hidden = custom;
                module.parentElement.classList.toggle('d-none', custom);
                module.disabled = custom;
                url.parentElement.hidden = !custom;
                url.parentElement.classList.toggle('d-none', !custom);
                url.disabled = !custom;
                title.required = custom;
            };
            source.addEventListener('change', update);
            update();
            return { read: () => {
                const custom = source.value === 'url';
                const href = custom ? shortcutUrl(url.value) : entries.find(entry => entry.href === module.value)?.href;
                if (!href) throw new Error(text(context, custom ? 'shortcutUrlInvalid' : 'chooseModule'));
                if (custom && !title.value.trim()) throw new Error(text(context, 'shortcutTitleRequired'));
                return { options: { source: custom ? 'url' : 'menu', href, title: title.value.trim() } };
            } };
        }
    });
    registerWidget({
        type: "recent-pages", titleKey: "admin.dashboard.recent-pages.js", descriptionKey: "admin.dashboard.recent-pages.description.js",
        icon: "ti-history", sizes: ["2x3", "3x3"], defaultSize: "3x3",
        isAvailable: () => window.WJ.hasPermission("menuWebpages"),
        async render({ container, instance, context, signal }) {
            const pages = await recentPages(signal);
            if (signal.aborted) return;
            if (!pages.length) empty(container, context);
            else recentPagesList(container, pages, instance.size);
            footer(container, context, "/admin/v9/webpages/web-pages-list/");
        },
        async renderCollapsed({ container, context, signal }) {
            const pages = await recentPages(signal);
            if (signal.aborted) return;
            if (pages.length) container.append(link(pages[0].title, `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(pages[0].docId)}`));
            else empty(container, context);
            footer(container, context, "/admin/v9/webpages/web-pages-list/");
        }
    });
    registerUtilityWidgets();
    registerDataWidgets();
}
