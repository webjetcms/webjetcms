import { localUrl } from './dashboard/widget-utils';

/**
 * A same-origin destination from the authorized administration menu.
 * @typedef {Object} MenuEntry
 * @property {string} href - Normalized destination URL.
 * @property {string} title - Menu label.
 * @property {string} [icon] - Own icon or the nearest inherited menu icon.
 * @property {string} path - Parent labels joined as a breadcrumb.
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
    const visit = (items, inheritedIcon, parents = []) => (items || []).forEach(item => {
        const href = localUrl(item.href);
        const itemIcon = item.icon || inheritedIcon;
        if (href && !["/", "/admin/v9/#", "/admin/v9/"].includes(href) && item.text) {
            const path = parents.filter((label, index) => label !== parents[index - 1]);
            if (path.at(-1) === item.text) path.pop();
            entries.set(href, { href, title: item.text, icon: itemIcon, path: path.join(' › ') });
        }
        visit(item.childrens || item.children, itemIcon, [...parents, item.text].filter(Boolean));
    });
    visit(context.data.dashboardMenu, inheritedIcon);
    return [...entries.values()];
}

