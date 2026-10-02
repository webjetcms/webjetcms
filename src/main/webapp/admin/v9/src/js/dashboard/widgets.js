import { getWidget, registerWidget } from './registry';
import { registerUtilityWidgets } from './utility-widgets';
import { registerDataWidgets } from './data-widgets';
import { registerSystemWidgets } from './system-widgets';
import { menuEntries, registerShortcutWidget } from './shortcut-widget';
import { node, text, empty, date, containNativeScroll, pagePreview, fetchJson } from './widget-utils';

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
    registerShortcutWidget();
    registerWidget({
        type: "recent-pages", titleKey: "admin.dashboard.recent-pages.js", descriptionKey: "admin.dashboard.recent-pages.description.js",
        icon: "ti-history", multiple: true, sizes: ["2x3", "3x2", "3x3"], defaultSize: "3x2",
        headerLink: { href: "/admin/v9/webpages/web-pages-list/" },
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
