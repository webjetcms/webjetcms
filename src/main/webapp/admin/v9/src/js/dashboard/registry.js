/**
 * Data and services shared with widget lifecycle callbacks.
 * @typedef {Object} WidgetContext
 * @property {Object} data - Server bootstrap data for the current administrator and domain.
 * @property {Object<string, string>} labels - Translated module labels and trusted release announcement HTML.
 * @property {Object} config - Dashboard configuration and default widget presets.
 * @property {Object} [overview] - Owning overview component, including optional feedback actions.
 * @property {import('./dashboard').DashboardController} dashboard - Controller for preference and dialog actions.
 * @property {import('./model').DashboardSettings} settings - Current effective dashboard settings.
 * @property {function(string, ...(string|number)): string} translate - Resolves a translation key with optional substitutions.
 */

/**
 * Preference maps returned by configuration readers or passed to saveOptions.
 * @typedef {Object} WidgetOptionsUpdate
 * @property {Object} [options] - Replacement shared preferences; omission preserves existing values.
 * @property {Object} [domainOptions] - Replacement active-domain preferences; omission preserves existing values.
 */

/**
 * Isolated inputs for a widget render or configuration dialog.
 * @typedef {Object} WidgetArguments
 * @property {HTMLElement} container - Element to populate with safe DOM content.
 * @property {import('./model').WidgetInstance} instance - Copy of the widget instance.
 * @property {Object} options - Copy of shared preferences.
 * @property {Object} domainOptions - Copy of current-domain preferences or their registered defaults.
 * @property {WidgetContext} context - Current dashboard services and page data.
 * @property {AbortSignal} signal - Aborted when the render or dialog is replaced or disposed.
 * @property {boolean} [adding=false] - Configuration-only flag for a new, unsaved widget.
 * @property {function(): Promise<void>} [refresh] - Render-only callback that reloads this instance.
 * @property {function(WidgetOptionsUpdate): Promise<boolean>} [saveOptions] - Render-only callback resolving to whether persistence succeeded.
 */

/**
 * Resources returned by a renderer and disposed after abort, replacement or stale completion.
 * @typedef {(function(): void)|{destroy: function(): void}|void} WidgetCleanup
 */

/**
 * Configuration controls read when the user saves and released when the dialog closes.
 * @typedef {Object} WidgetConfiguration
 * @property {function(): (WidgetOptionsUpdate|Promise<WidgetOptionsUpdate>)} [read] - Reads replacements; may throw or reject with a localized validation error.
 * @property {function(): void} [destroy] - Releases resources owned by the configuration controls.
 */

/**
 * Widget metadata and callbacks consumed by the dashboard controller.
 * @typedef {Object} WidgetDefinition
 * @property {string} type - Unique type identifier accepted by the settings backend.
 * @property {string} [titleKey] - Translation key for the default title.
 * @property {string} [descriptionKey] - Translation key for the catalogue description.
 * @property {"content"|"traffic"|"system"} [category="system"] - Catalogue category.
 * @property {string} [icon] - Tabler icon class.
 * @property {string[]} [sizes] - Supported footprints; defaults to a single 2x2 size.
 * @property {string} [defaultSize] - Initial footprint; defaults to the first supported size.
 * @property {boolean} [multiple=false] - Whether users may add more than one instance.
 * @property {boolean} [mandatory=false] - Whether the controller ensures an instance exists and prevents removal.
 * @property {Object} [defaultOptions] - Initial shared preferences; defaults to an empty object.
 * @property {Object} [defaultDomainOptions] - Initial domain preferences; defaults to an empty object.
 * @property {function(WidgetContext): boolean} [isAvailable] - Determines UI availability, independently of server authorization.
 * @property {function(import('./model').WidgetInstance, WidgetContext): boolean} [isVisible] - Controls visibility of persisted grid instances.
 * @property {function(import('./model').WidgetInstance, WidgetContext): (boolean|Promise<boolean>)} [reveal] - Restores a hidden singleton from the catalogue and reports success.
 * @property {function(import('./model').WidgetInstance, WidgetContext): string} [getTitle] - Supplies the instance's text title.
 * @property {Object} [headerLink] - Navigation shown in the widget header.
 * @property {string|function(import('./model').WidgetInstance, WidgetContext): string} headerLink.href - Local destination or resolver for the current instance.
 * @property {string} [headerLink.labelKey] - Translation key for a separate link; omission links the title.
 * @property {function(WidgetArguments): (WidgetCleanup|Promise<WidgetCleanup>)} render - Populates content and optionally returns resources to dispose.
 * @property {function(WidgetArguments): (WidgetConfiguration|Promise<WidgetConfiguration>)} [configure] - Populates settings controls and returns their reader and optional cleanup.
 */

/** Supported, named dashboard footprints. Pixel positions are never persisted. */
export const WIDGET_SIZES = Object.freeze(["1x1", "2x2", "2x3", "3x2", "3x3", "fullauto"]);

const definitions = new Map();

/**
 * Registers a widget type. Renderers receive an abort signal and return a cleanup
 * function or an object with a destroy method, synchronously or asynchronously.
 *
 * @param {WidgetDefinition} definition Widget metadata and lifecycle callbacks.
 * @returns {WidgetDefinition} A shallow-frozen definition with defaults and a copied sizes array.
 * @throws {Error} If the type or renderer is missing, the type is already registered, or the sizes are invalid.
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
