import { getWidget, registerWidget } from './registry';
import { registerUtilityWidgets } from './utility-widgets';
import { registerDataWidgets } from './data-widgets';
import { node, text, localUrl, link, icon, field, table, empty, footer } from './widget-utils';

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

/** Initial layout is applied only when the account has no saved dashboard. */
export function getDashboardDefaults(context) {
    const items = [{ type: "search" }];
    if (window.WJ.hasPermission("menuWebpages")) items.push({ type: "recent-pages", size: "3x3" });
    items.push({ type: "sessions" });
    const menu = menuEntries(context);
    for (const href of ["/admin/v9/webpages/web-pages-list/", "/apps/form/admin/"]) {
        if (menu.some(item => item.href === href)) items.push({ type: "shortcut", options: { href } });
    }
    items.push({ type: "news" });
    return items;
}

/** Loads the permission- and domain-filtered preview without session caching. */
async function recentPages(signal) {
    const response = await fetch("/admin/rest/dashboard/recent-pages", { signal, credentials: "same-origin", headers: { Accept: "application/json", "X-CSRF-Token": window.csrfToken } });
    if (!response.ok) throw new Error(`Recent pages request failed (${response.status})`);
    return response.json();
}

/** Registers built-in widgets once, even when the overview is reconfigured. */
export function registerDashboardWidgets() {
    if (getWidget("shortcut")) return;
    registerWidget({
        type: "shortcut", titleKey: "admin.dashboard.shortcut.js", descriptionKey: "admin.dashboard.shortcut.description.js",
        icon: "ti-link", sizes: ["1x1"], multiple: true, defaultOptions: { href: "", title: "" },
        isAvailable: context => menuEntries(context).length > 0,
        getTitle: (instance, context) => instance.options?.title || menuEntries(context).find(item => item.href === instance.options?.href)?.title || text(context, "shortcut"),
        render({ container, options, context }) {
            const item = menuEntries(context).find(entry => entry.href === options.href);
            if (!item) { empty(container, context, "chooseModule"); return; }
            const target = link(options.title || item.title, item.href, "md-dashboard-widget__shortcut");
            target.prepend(icon(item.icon));
            container.append(target);
        },
        configure({ container, options, context }) {
            const entries = menuEntries(context);
            const module = field(container, text(context, "module"), entries.map(item => [item.href, item.title]), options.href || entries[0]?.href);
            const title = field(container, text(context, "customTitle"), [], options.title || "", "text");
            return { read: () => ({ options: { href: module.value, title: title.value.trim() } }) };
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
            else if (instance.size === "2x3") {
                const list = node("ul", "md-dashboard-widget__list");
                pages.slice(0, 5).forEach(page => {
                    const row = node("li");
                    const target = link(page.title, `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(page.docId)}`);
                    target.title = page.fullPath || page.title;
                    row.append(target);
                    list.append(row);
                });
                container.append(list);
            } else table(container, [text(context, "page"), text(context, "section"), text(context, "recentChanged")], pages.slice(0, 6).map(page => [
                link(page.title, `/admin/v9/webpages/web-pages-list/?docid=${encodeURIComponent(page.docId)}`),
                page.fullPath?.endsWith(`/${page.title}`) ? page.fullPath.slice(0, -(page.title.length + 1)) || '/' : page.fullPath,
                page.date || page.saveDate
            ]));
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
