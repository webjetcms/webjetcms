/** Supported, named dashboard footprints. Pixel positions are never persisted. */
export const WIDGET_SIZES = Object.freeze(["1x1", "2x2", "2x3", "3x2", "3x3", "fullauto"]);

const definitions = new Map();

/**
 * Registers a widget type. Renderers receive an abort signal and return a cleanup
 * function or an object with a destroy method, synchronously or asynchronously.
 *
 * @param {Object} definition Widget metadata and lifecycle callbacks.
 * @returns {Object} The registered definition.
 */
export function registerWidget(definition) {
    if (!definition?.type || typeof definition.render !== "function") throw new Error("A dashboard widget needs a type and renderer");
    if (definitions.has(definition.type)) throw new Error(`Dashboard widget already registered: ${definition.type}`);
    const sizes = definition.sizes || ["2x2"];
    if (!sizes.length || sizes.some(size => !WIDGET_SIZES.includes(size))) throw new Error("Unsupported dashboard widget size");
    const defaultSize = definition.defaultSize || sizes[0];
    if (!sizes.includes(defaultSize)) throw new Error("The default dashboard size must be supported by the widget");
    const registered = Object.freeze({ multiple: false, mandatory: false, defaultOptions: {}, defaultDomainOptions: {}, ...definition, sizes: [...sizes], defaultSize });
    definitions.set(registered.type, registered);
    return registered;
}

export function getWidget(type) {
    return definitions.get(type);
}

export function listWidgets() {
    return [...definitions.values()];
}
