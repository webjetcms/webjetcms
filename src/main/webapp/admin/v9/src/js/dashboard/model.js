/**
 * A persisted widget whose position is determined by its index in the layout.
 * @typedef {Object} WidgetInstance
 * @property {string} id - Stable instance identifier shared by layout and domain options.
 * @property {string} type - Registered widget type.
 * @property {string} size - Named footprint supported by the widget.
 * @property {Object} [options] - JSON-serializable preferences shared across domains.
 */

/**
 * Shared account layout with options for the active domain only.
 * @typedef {Object} DashboardSettings
 * @property {number} version - Settings schema version, currently 1.
 * @property {boolean} configured - Whether a personal layout has been saved.
 * @property {boolean} shortcutsConfigured - Whether shortcuts were explicitly saved, including an empty list.
 * @property {boolean} legacyBookmarksHandled - Whether browser bookmarks have already been imported.
 * @property {WidgetInstance[]} items - Ordered widget instances, including unavailable types.
 * @property {Object<string, Object>} domainOptions - Active-domain preferences keyed by instance ID.
 * @property {string|null} acknowledgedNewsVersion - Legacy announcement preference retained for compatibility with stored profiles.
 */

export const MAX_WIDGETS = 48;

export function cloneSettings(value) {
    return JSON.parse(JSON.stringify(value));
}

/**
 * Creates a stable, server-compatible identifier for a widget instance.
 * @returns {string} A UUID when supported, otherwise a timestamp and random identifier.
 */
export function createInstanceId() {
    return globalThis.crypto?.randomUUID?.() || `w-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}

/**
 * Keeps unavailable widget types in the user's profile across permission changes.
 * Clones the items and domain options, removes legacy collapsed flags and supplies configuration defaults.
 *
 * @param {Partial<DashboardSettings>} [settings={}] - Profile to normalize without mutating it.
 * @returns {DashboardSettings} An independent settings object using schema version 1.
 */
export function normalizeSettings(settings = {}) {
    const items = Array.isArray(settings.items) ? cloneSettings(settings.items) : [];
    // Older profiles can contain the removed minimization preference.
    items.forEach(item => { delete item.collapsed; });
    return {
        version: 1,
        configured: settings.configured === true,
        shortcutsConfigured: settings.shortcutsConfigured ?? (settings.configured === true),
        legacyBookmarksHandled: settings.legacyBookmarksHandled === true,
        items,
        domainOptions: settings.domainOptions && typeof settings.domainOptions === "object" ? cloneSettings(settings.domainOptions) : {},
        acknowledgedNewsVersion: settings.acknowledgedNewsVersion || null
    };
}

/**
 * Returns a new ordered array, moving one instance before a target or to the end.
 * Unknown IDs or moving an item before itself preserve the original order; item objects are shared.
 *
 * @param {WidgetInstance[]} items - Current instance order.
 * @param {string} id - Instance to move.
 * @param {string|null} [beforeId=null] - Target instance, or null to append at the end.
 * @returns {WidgetInstance[]} A reordered shallow copy of the array.
 */
export function moveInstanceBefore(items, id, beforeId = null) {
    const source = items.find(item => item.id === id);
    if (!source || beforeId === id) return [...items];
    const result = items.filter(item => item.id !== id);
    const position = beforeId == null ? result.length : result.findIndex(item => item.id === beforeId);
    if (position < 0) return [...items];
    result.splice(position, 0, source);
    return result;
}

/**
 * Divides ordered widgets at natural-height, full-width widgets. Regular grids
 * use sparse row placement so visual and keyboard order remain identical.
 *
 * @param {WidgetInstance[]} items - Widgets in persisted display order.
 * @returns {{full: boolean, items: WidgetInstance[]}[]} Contiguous grid groups and individual full-width widgets.
 */
export function createLayoutSegments(items) {
    const segments = [];
    let current = [];
    for (const item of items) {
        if (item.size === "fullauto") {
            if (current.length) segments.push({ full: false, items: current });
            segments.push({ full: true, items: [item] });
            current = [];
        } else current.push(item);
    }
    if (current.length) segments.push({ full: false, items: current });
    return segments;
}
