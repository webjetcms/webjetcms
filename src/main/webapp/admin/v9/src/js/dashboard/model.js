export const MAX_WIDGETS = 32;

export function cloneSettings(value) {
    return JSON.parse(JSON.stringify(value));
}

/** Creates a stable, server-compatible identifier for a widget instance. */
export function createInstanceId() {
    return globalThis.crypto?.randomUUID?.() || `w-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}

/** Keeps unavailable widget types in the user's profile across permission changes. */
export function normalizeSettings(settings = {}) {
    return {
        version: 1,
        configured: settings.configured === true,
        items: Array.isArray(settings.items) ? cloneSettings(settings.items) : [],
        domainOptions: settings.domainOptions && typeof settings.domainOptions === "object" ? cloneSettings(settings.domainOptions) : {},
        acknowledgedNewsVersion: settings.acknowledgedNewsVersion || null
    };
}

/** Returns a new ordered array, moving one instance before a target or to the end. */
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
