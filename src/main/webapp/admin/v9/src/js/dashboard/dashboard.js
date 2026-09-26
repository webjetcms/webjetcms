import { getWidget, listWidgets } from './registry';
import { MAX_WIDGETS, cloneSettings, createInstanceId, normalizeSettings, moveInstanceBefore, createLayoutSegments } from './model';
import { link } from './widget-utils';

function node(tag, className = "", text) {
    const result = document.createElement(tag);
    result.className = className;
    if (text != null) result.textContent = String(text);
    return result;
}

function icon(name) {
    const result = node("i", `ti ${name}`);
    result.setAttribute("aria-hidden", "true");
    return result;
}

function button(text, callback, className = "btn btn-sm btn-outline-secondary") {
    const result = node("button", className, text);
    result.type = "button";
    result.addEventListener("click", callback);
    return result;
}

function dispose(result) {
    if (typeof result === "function") result();
    else result?.destroy?.();
}

/**
 * Owns dashboard layout, accessible controls, server persistence and widget
 * lifecycles. It never replaces the surrounding system alerts or overview.
 */
export class DashboardController {
    constructor(host, context = {}) {
        this.host = host;
        this.context = context;
        this.settings = normalizeSettings();
        this.views = new Map();
        this.saving = false;
        this.destroyed = false;
        this.removed = null;
        this._dialogs = new Set();
        this._request = null;
        this._contextVersion = 0;
        this.editing = false;
        this._build();
    }

    _t(key, fallback = key, ...params) {
        const fullKey = key.includes(".") ? key : `admin.dashboard.${key}.js`;
        const translated = this.context.translate?.(fullKey, ...params) ?? window.WJ?.translate?.(fullKey, ...params);
        return translated && translated !== fullKey ? translated : fallback;
    }

    _build() {
        this.host.classList.add("md-dashboard");
        this.hero = node("div", "md-dashboard__hero");
        const welcome = node("div", "md-dashboard__welcome");
        const language = window.userLng === "cz" ? "cs" : window.userLng || "sk";
        welcome.append(node("p", "md-dashboard__eyebrow", new Intl.DateTimeFormat(language, { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(new Date())));
        welcome.append(node("h1", "md-dashboard__greeting", `${this._t("welcomeBack", "Welcome back,")} ${this.context.data?.userName || ""}`.trim()));
        this.news = node("div", "md-dashboard__news");
        welcome.append(this.news);
        this.sessions = node("div", "md-dashboard__sessions");
        this.hero.append(welcome, this.sessions);
        this.notices = node("div", "md-dashboard__notices");
        this.notices.id = "toast-container-overview";
        this.search = node("div", "md-dashboard__search");
        this.toolbar = node("div", "md-dashboard__toolbar");
        this.toolbar.append(node("h2", "md-dashboard__title", this._t("title", "My overview")));
        const actions = node("div", "md-dashboard__toolbar-actions");
        this.editButton = button(this._t("editOverview", "Edit overview"), () => this.setEditing(!this.editing), "btn btn-sm btn-outline-secondary md-dashboard__control");
        this.editButton.setAttribute("aria-pressed", "false");
        this.editButton.prepend(icon("ti-adjustments-horizontal"));
        this.addButton = button(this._t("add", "Add widget"), () => this.showCatalogue(), "btn btn-sm btn-primary md-dashboard__control md-dashboard__edit-control");
        this.addButton.prepend(icon("ti-plus"));
        this.addButton.hidden = true;
        actions.append(this.addButton, this.editButton);
        this.toolbar.append(actions);
        this.status = node("div", "md-dashboard__status");
        this.status.setAttribute("role", "status");
        this.status.setAttribute("aria-live", "polite");
        this.status.tabIndex = -1;
        this.undoContainer = node("div", "md-dashboard__undo");
        this.undoContainer.hidden = true;
        this.layout = node("div", "md-dashboard__layout");
        this.dropEnd = node("div", "md-dashboard__drop-end", this._t("moveEnd", "At the end"));
        this.dropEnd.setAttribute("aria-hidden", "true");
        this.shortcuts = node("section", "md-dashboard__shortcuts");
        const shortcutsHeader = node("div", "md-dashboard__shortcuts-header");
        const addShortcut = button(this._t("addShortcut", "Add shortcut"), () => this.showAddWidget("shortcut"), "btn btn-sm btn-outline-secondary md-dashboard__control");
        addShortcut.prepend(icon("ti-plus"));
        shortcutsHeader.append(node("h2", "md-dashboard__title", this._t("shortcuts", "Your shortcuts")), addShortcut);
        this.shortcutList = node("div", "md-dashboard__shortcut-list");
        this.shortcuts.append(shortcutsHeader, this.shortcutList);
        this.host.replaceChildren(this.hero, this.notices, this.search, this.toolbar, this.status, this.undoContainer, this.layout, this.dropEnd, this.shortcuts);
        this._setBusy(true);
    }

    /** Reveals arrangement controls without changing or saving widget preferences. */
    setEditing(editing) {
        this.editing = Boolean(editing);
        this.host.classList.toggle("is-editing", this.editing);
        this.editButton.textContent = this._t(this.editing ? "finishEditing" : "editOverview", this.editing ? "Done" : "Edit overview");
        this.editButton.prepend(icon(this.editing ? "ti-check" : "ti-adjustments-horizontal"));
        this.editButton.setAttribute("aria-pressed", String(this.editing));
        this.host.querySelectorAll(".md-dashboard__edit-control").forEach(control => { control.hidden = !this.editing; });
        for (const view of this.views.values()) window.bootstrap?.Dropdown?.getInstance(view.header.querySelector('[data-bs-toggle="dropdown"]'))?.hide();
        this._bindDrag();
    }

    _region(instance) {
        return ["sessions", "news", "search"].includes(instance.type) ? instance.type : instance.type === "shortcut" ? "shortcut" : "grid";
    }

    /** Keeps fixed utilities visible without rewriting saved instances or their preferences. */
    _displayItems() {
        const items = this.settings.items.map(instance => this._region(instance) === "grid" || instance.type === "shortcut" ? instance : { ...instance, collapsed: false });
        for (const type of ["sessions", "news", "search"]) {
            const definition = getWidget(type);
            if (definition && !items.some(item => item.type === type)) items.push({ ...this._newInstance(definition), id: `dashboard-fixed-${type}` });
        }
        return items.filter(instance => {
            const definition = getWidget(instance.type);
            return definition && this._available(definition) && (this._region(instance) !== "grid" || this._visible(definition, instance));
        });
    }

    /** Loads the current account's layout and the active domain's widget filters. */
    async start() {
        this._request?.abort();
        const request = this._request = new AbortController();
        this.host.dataset.loaded = "false";
        this.addButton.disabled = true;
        this.status.textContent = this._t("loading", "Loading overview…");
        try {
            const response = await fetch("/admin/rest/dashboard/settings", { signal: request.signal, credentials: "same-origin", headers: { "X-CSRF-Token": window.csrfToken || "" } });
            if (!response.ok) throw new Error(`Dashboard settings: ${response.status}`);
            const data = await response.json();
            if (request.signal.aborted || this.destroyed) return;
            this.settings = normalizeSettings(data);
            if (!this.settings.configured) this._addDefaults();
            this._ensureMandatory();
            this.status.textContent = "";
            this._setBusy(false);
            this.layout.hidden = false;
            this._render();
            this.host.dataset.loaded = "true";
        } catch (error) {
            if (request.signal.aborted || this.destroyed) return;
            this._render();
            this._setBusy(true);
            this._showFailure("loadError", "The overview could not be loaded.", () => this.start());
        }
    }

    _newInstance(definition, values = {}) {
        return { id: createInstanceId(), type: definition.type, size: values.size || definition.defaultSize, collapsed: false, options: { ...cloneSettings(definition.defaultOptions), ...values.options } };
    }

    _addDefaults() {
        for (const preset of this.context.config?.dashboardDefaults || []) {
            const definition = getWidget(preset.type);
            if (!definition || !this._available(definition) || this.settings.items.length >= MAX_WIDGETS) continue;
            if (!definition.multiple && this.settings.items.some(item => item.type === definition.type)) continue;
            const item = this._newInstance(definition, preset);
            this.settings.items.push(item);
            this.settings.domainOptions[item.id] = { ...cloneSettings(definition.defaultDomainOptions), ...preset.domainOptions };
        }
    }

    _ensureMandatory() {
        for (const definition of listWidgets().filter(widget => widget.mandatory)) {
            if (this.settings.items.some(item => item.type === definition.type)) continue;
            this.settings.items.push(this._newInstance(definition));
        }
    }

    _available(definition) {
        return !definition.isAvailable || definition.isAvailable(this._widgetContext());
    }

    _visible(definition, instance) {
        return !definition.isVisible || definition.isVisible(instance, this._widgetContext());
    }

    _widgetContext() {
        return { ...this.context, dashboard: this, settings: this.settings, translate: (key, ...params) => this._t(key, key, ...params) };
    }

    _title(instance) {
        const definition = getWidget(instance.type);
        return String(definition?.getTitle?.(instance, this._widgetContext()) || this._t(definition?.titleKey || instance.type));
    }

    _showFailure(key, fallback, retry) {
        this.status.replaceChildren(node("span", "text-danger", this._t(key, fallback)));
        if (retry) this.status.append(button(this._t("retry", "Try again"), retry));
    }

    /**
     * Saves before applying a change, keeping the last confirmed layout on failure.
     * Widget content and security actions stay usable while preferences are saved.
     */
    async _commit(next) {
        if (this.saving || this.destroyed || this.host.dataset.loaded !== "true") return false;
        if (next.items.length > MAX_WIDGETS) {
            this.status.textContent = this._t("limit", "The overview can contain at most 32 widgets.");
            return false;
        }
        this.saving = true;
        this._setBusy(true);
        this.status.textContent = this._t("saving", "Saving…");
        const request = this._request = new AbortController();
        try {
            const response = await fetch("/admin/rest/dashboard/settings", {
                method: "PUT", credentials: "same-origin", signal: request.signal,
                headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken || "" },
                body: JSON.stringify(next)
            });
            if (!response.ok) throw new Error(`Dashboard settings: ${response.status}`);
            const saved = await response.json();
            if (this.destroyed || request.signal.aborted) return false;
            this.settings = normalizeSettings(saved);
            this.settings.configured = true;
            this.removed = null;
            this.undoContainer.hidden = true;
            this.status.textContent = this._t("saved", "Saved.");
            this._render();
            return true;
        } catch (error) {
            if (!this.destroyed && !request.signal.aborted) this._showFailure("saveError", "The change could not be saved. Your previous settings were kept.");
            return false;
        } finally {
            this.saving = false;
            if (!this.destroyed) this._setBusy(false);
        }
    }

    _setBusy(busy) {
        this.host.setAttribute("aria-busy", String(busy));
        this.host.querySelectorAll(".md-dashboard__control").forEach(control => { control.disabled = busy; });
    }

    _render() {
        const visible = this._displayItems();
        const ids = new Set(visible.map(item => item.id));
        for (const [id, view] of this.views) {
            if (!ids.has(id)) {
                this._disposeView(view);
                this.views.delete(id);
            }
        }
        const focused = document.activeElement;
        const restoreFocus = this.host.contains(focused);
        const fragment = document.createDocumentFragment();
        const refreshIds = [];
        const updateView = instance => {
            let view = this.views.get(instance.id);
            if (!view) {
                view = this._createView(instance);
                this.views.set(instance.id, view);
            }
            view.instance = instance;
            view.titleText.textContent = this._title(instance);
            view.title.title = view.titleText.textContent;
            view.card.dataset.size = instance.size;
            view.card.classList.toggle("is-collapsed", Boolean(instance.collapsed));
            view.collapse.textContent = this._t(instance.collapsed ? "expand" : "collapse", instance.collapsed ? "Expand" : "Collapse");
            view.collapse.setAttribute("aria-expanded", String(!instance.collapsed));
            view.collapse.hidden = instance.size === "1x1";
            const signature = JSON.stringify([instance.type, instance.size, instance.collapsed, instance.options, this.settings.domainOptions[instance.id], this._contextVersion, instance.type === "news" ? this.settings.acknowledgedNewsVersion : null]);
            if (view.signature !== signature) {
                view.signature = signature;
                refreshIds.push(instance.id);
            }
            return view.card;
        };
        const gridItems = visible.filter(instance => this._region(instance) === "grid");
        for (const segment of createLayoutSegments(gridItems)) {
            const grid = node("div", segment.full ? "md-dashboard__full" : "md-dashboard__grid");
            segment.items.forEach(instance => grid.append(updateView(instance)));
            fragment.append(grid);
        }
        for (const region of ["sessions", "news", "search", "shortcut"]) {
            const container = region === "shortcut" ? this.shortcutList : this[region];
            container.replaceChildren(...visible.filter(instance => this._region(instance) === region).map(updateView));
        }
        this.shortcuts.hidden = !getWidget("shortcut");
        if (!gridItems.length) fragment.append(node("p", "md-dashboard__empty", this._t("empty", "Add widgets to create your overview.")));
        this.layout.replaceChildren(fragment);
        refreshIds.forEach(id => this.refresh(id));
        if (restoreFocus) {
            if (this.host.contains(focused)) focused.focus({ preventScroll: true });
            else if (focused.classList.contains("md-dashboard-widget__news-toggle")) this.news.querySelector(".md-dashboard-widget__news-toggle")?.focus({ preventScroll: true });
            else this.status.focus({ preventScroll: true });
        }
        this._bindDrag();
        this._setBusy(this.saving);
        this.host.dispatchEvent(new CustomEvent("webjet-dashboard-rendered", { bubbles: true }));
    }

    _createView(instance) {
        const definition = getWidget(instance.type);
        const card = node("section", "md-dashboard__widget");
        card.dataset.instanceId = instance.id;
        card.dataset.widgetType = instance.type;
        card.tabIndex = -1;
        const fixed = ["sessions", "news", "search"].includes(this._region(instance));
        card.classList.toggle("is-fixed", fixed);
        const header = node("div", "md-dashboard__widget-header");
        const title = node(fixed ? "h2" : "h3", "md-dashboard__widget-title");
        const titleText = node("span");
        if (definition.headerLink && !definition.headerLink.labelKey) {
            const titleLink = link("", definition.headerLink.href, "md-dashboard__title-link");
            titleLink.append(titleText, icon("ti-arrow-up-right"));
            title.append(titleLink);
        } else title.append(titleText);
        title.id = `dashboard-title-${instance.id}`;
        card.setAttribute("aria-labelledby", title.id);
        if (definition.icon) header.append(icon(definition.icon));
        header.append(title);
        if (definition.headerLink?.labelKey) {
            const headerLink = link(this._t(definition.headerLink.labelKey), definition.headerLink.href, "md-dashboard__header-link");
            headerLink.append(icon("ti-arrow-up-right"));
            header.append(headerLink);
        }
        const drag = button(this._t("move", "Move widget"), () => this.showMove(instance.id), "btn btn-sm md-dashboard__drag md-dashboard__control");
        drag.replaceChildren(icon("ti-grip-vertical"));
        drag.setAttribute("aria-label", this._t("move", "Move widget"));
        const dropdown = node("div", "dropdown");
        const menuButton = button("", () => {}, "btn btn-sm md-dashboard__control");
        menuButton.append(icon("ti-dots-vertical"));
        menuButton.setAttribute("aria-label", this._t("actions", "Widget actions"));
        menuButton.setAttribute("data-bs-toggle", "dropdown");
        menuButton.setAttribute("aria-expanded", "false");
        const menu = node("div", "dropdown-menu dropdown-menu-end");
        const menuItem = (key, fallback, action) => {
            const control = button(this._t(key, fallback), () => {
                window.bootstrap?.Dropdown?.getInstance(menuButton)?.hide();
                menuButton.focus({ preventScroll: true });
                action();
            }, "dropdown-item md-dashboard__control");
            control.dataset.dashboardAction = key;
            return control;
        };
        menu.append(menuItem("refresh", "Refresh", () => this.refresh(instance.id)));
        if (definition.configure || definition.sizes.length > 1) menu.append(menuItem("settings", "Settings", () => this.showSettings(instance.id)));
        const collapse = menuItem("collapse", "Collapse", () => this.updateInstance(instance.id, { collapsed: !this._instance(instance.id).collapsed }));
        menu.append(collapse, menuItem("move", "Move widget", () => this.showMove(instance.id)));
        if (!definition.mandatory) menu.append(menuItem("remove", "Remove", () => this.remove(instance.id)));
        dropdown.append(menuButton, menu);
        const controls = node("div", "md-dashboard__widget-controls md-dashboard__edit-control");
        controls.hidden = !this.editing;
        controls.append(drag, dropdown);
        if (!fixed) header.append(controls);
        if (["news", "search"].includes(instance.type)) header.hidden = true;
        const body = node("div", "md-dashboard__widget-body");
        body.id = `dashboard-body-${instance.id}`;
        collapse.setAttribute("aria-controls", body.id);
        card.append(header, body);
        return { card, header, title, titleText, body, collapse, instance, abort: null, cleanup: null, signature: null };
    }

    _instance(id) {
        return this.settings.items.find(item => item.id === id);
    }

    _disposeView(view) {
        view.abort?.abort();
        if (view.cleanup) {
            try { dispose(view.cleanup); } catch (error) { console.warn("Dashboard widget cleanup failed", error); }
            view.cleanup = null;
        }
        window.bootstrap?.Dropdown?.getInstance(view.header.querySelector('[data-bs-toggle="dropdown"]'))?.dispose();
        const $ = window.jQuery;
        if ($?.fn.draggable && $(view.card).data("ui-draggable")) $(view.card).draggable("destroy");
        if ($?.fn.droppable && $(view.card).data("ui-droppable")) $(view.card).droppable("destroy");
    }

    /** Refreshes one widget and disposes stale results even when a request ignores abort. */
    async refresh(id) {
        const view = this.views.get(id);
        if (!view || this.destroyed) return;
        view.abort?.abort();
        if (view.cleanup) {
            try { dispose(view.cleanup); } catch (error) { console.warn("Dashboard widget cleanup failed", error); }
            view.cleanup = null;
        }
        const abort = view.abort = new AbortController();
        const instance = view.instance;
        const definition = getWidget(instance.type);
        const renderer = instance.collapsed ? definition.renderCollapsed : definition.render;
        const content = node("div", "md-dashboard__widget-content");
        view.body.replaceChildren(content);
        view.body.hidden = !renderer;
        if (!renderer) return;
        const loading = node("span", "md-dashboard__loading", this._t("loading", "Loading…"));
        view.body.prepend(loading);
        view.body.setAttribute("aria-busy", "true");
        try {
            const result = await renderer({
                container: content, instance: cloneSettings(instance), options: cloneSettings(instance.options || {}),
                domainOptions: cloneSettings(this.settings.domainOptions[id] || definition.defaultDomainOptions),
                context: this._widgetContext(), signal: abort.signal,
                refresh: () => this.refresh(id), saveOptions: values => this.saveOptions(id, values)
            });
            if (abort.signal.aborted || this.destroyed) dispose(result);
            else view.cleanup = result;
        } catch (error) {
            if (abort.signal.aborted || this.destroyed) return;
            const reasonKey = { "domain-unavailable": "domainUnavailable", "selection-unavailable": "selectionUnavailable", "period-unavailable": "periodUnavailable" }[error.dashboardReason] || "widgetError";
            content.replaceChildren(node("p", "text-danger", this._t(reasonKey, "This widget could not be loaded.")), button(this._t("retry", "Try again"), () => this.refresh(id)));
        } finally {
            if (!abort.signal.aborted && !this.destroyed) {
                loading.remove();
                view.body.setAttribute("aria-busy", "false");
            }
        }
    }

    async add(type) {
        const definition = getWidget(type);
        if (!definition || !this._available(definition)) return false;
        if (!definition.multiple && this.settings.items.some(item => item.type === type)) return false;
        const next = cloneSettings(this.settings);
        const instance = this._newInstance(definition);
        next.items.push(instance);
        next.domainOptions[instance.id] = cloneSettings(definition.defaultDomainOptions);
        const saved = await this._commit(next);
        if (saved) this._focusInstance(instance.id);
        return saved;
    }

    async updateInstance(id, values) {
        const next = cloneSettings(this.settings);
        const item = next.items.find(instance => instance.id === id);
        if (!item) return false;
        const definition = getWidget(item.type);
        if (["sessions", "news", "search"].includes(this._region(item))) return false;
        if (values.size && !definition.sizes.includes(values.size)) return false;
        Object.assign(item, values);
        if (item.size === "1x1") item.collapsed = false;
        const saved = await this._commit(next);
        if (saved) this._focusInstance(id);
        return saved;
    }

    /** Persists shared options and filters for only the server's active domain. */
    async saveOptions(id, { options, domainOptions } = {}) {
        const next = cloneSettings(this.settings);
        const item = next.items.find(instance => instance.id === id);
        if (!item) return false;
        if (options !== undefined) item.options = cloneSettings(options);
        if (domainOptions !== undefined) next.domainOptions[id] = cloneSettings(domainOptions);
        return this._commit(next);
    }

    async remove(id) {
        const instance = this._instance(id);
        if (!instance || getWidget(instance.type)?.mandatory || ["sessions", "news", "search"].includes(this._region(instance))) return false;
        const previous = { instance: cloneSettings(instance), index: this.settings.items.indexOf(instance), domainOptions: cloneSettings(this.settings.domainOptions[id] || {}) };
        const next = cloneSettings(this.settings);
        next.items = next.items.filter(item => item.id !== id);
        delete next.domainOptions[id];
        if (!await this._commit(next)) return false;
        this.removed = previous;
        this.undoContainer.hidden = false;
        this.undoContainer.replaceChildren(node("span", "", this._t("removed", "Widget removed.")), button(this._t("undo", "Undo"), () => this.undoRemove(), "btn btn-sm btn-outline-secondary md-dashboard__control"));
        this.undoContainer.querySelector("button").focus();
        return true;
    }

    async undoRemove() {
        if (!this.removed) return false;
        const removed = this.removed;
        const next = cloneSettings(this.settings);
        if (next.items.some(item => item.id === removed.instance.id)) return false;
        const definition = getWidget(removed.instance.type);
        if (!definition.multiple && next.items.some(item => item.type === removed.instance.type)) return false;
        next.items.splice(Math.min(removed.index, next.items.length), 0, removed.instance);
        next.domainOptions[removed.instance.id] = removed.domainOptions;
        if (!await this._commit(next)) return false;
        this.removed = null;
        this.undoContainer.hidden = true;
        this._focusInstance(removed.instance.id);
        return true;
    }

    async moveBefore(id, beforeId = null) {
        const instance = this._instance(id);
        if (!instance || !["grid", "shortcut"].includes(this._region(instance))) return false;
        if (beforeId && this._region(this._instance(beforeId) || {}) !== this._region(instance)) return false;
        const next = cloneSettings(this.settings);
        next.items = moveInstanceBefore(next.items, id, beforeId);
        const saved = await this._commit(next);
        if (saved) this._focusInstance(id);
        return saved;
    }

    _focusInstance(id) {
        const view = this.views.get(id);
        (this.editing ? view?.header.querySelector("button") : view?.card)?.focus({ preventScroll: true });
    }

    async acknowledgeNews(version) {
        const next = cloneSettings(this.settings);
        next.acknowledgedNewsVersion = version;
        return this._commit(next);
    }

    /** Resets only dashboard preferences, applying defaults after the server confirms deletion. */
    async reset() {
        if (this.saving || this.destroyed) return false;
        this.saving = true;
        this._setBusy(true);
        this.status.textContent = this._t("saving", "Saving…");
        const request = this._request = new AbortController();
        try {
            const response = await fetch("/admin/rest/dashboard/settings", {
                method: "DELETE", credentials: "same-origin", signal: request.signal,
                headers: { "X-CSRF-Token": window.csrfToken || "" }
            });
            if (!response.ok) throw new Error(`Dashboard reset: ${response.status}`);
            const data = await response.json();
            if (this.destroyed || request.signal.aborted) return false;
            this.settings = normalizeSettings(data);
            this._addDefaults();
            this._ensureMandatory();
            this.removed = null;
            this.undoContainer.hidden = true;
            this.status.textContent = this._t("resetDone", "The default overview has been restored.");
            this._render();
            return true;
        } catch (error) {
            if (!this.destroyed && !request.signal.aborted) this._showFailure("saveError", "The change could not be saved. Your previous settings were kept.");
            return false;
        } finally {
            this.saving = false;
            if (!this.destroyed) this._setBusy(false);
        }
    }

    _dialog(title, trigger = document.activeElement) {
        const root = node("div", "modal fade md-dashboard-modal");
        root.tabIndex = -1;
        root.setAttribute("role", "dialog");
        root.setAttribute("aria-modal", "true");
        const dialog = node("div", "modal-dialog");
        const content = node("div", "modal-content");
        const header = node("div", "modal-header");
        const heading = node("h2", "modal-title fs-5", title);
        heading.id = `dashboard-dialog-${createInstanceId()}`;
        root.setAttribute("aria-labelledby", heading.id);
        const body = node("div", "modal-body");
        const footer = node("div", "modal-footer");
        const lifecycle = new AbortController();
        let modal = null;
        let cleanup;
        let removed = false;
        let shown = false;
        let closeRequested = false;
        let openingFocus = null;
        const finish = () => {
            if (removed) return;
            removed = true;
            lifecycle.abort();
            dispose(cleanup);
            modal?.dispose();
            root.remove();
            this._dialogs.delete(api);
            if (trigger?.isConnected) trigger.focus({ preventScroll: true });
        };
        const close = () => {
            if (!modal) finish();
            else if (shown) modal.hide();
            else closeRequested = true;
        };
        header.append(heading, button(this._t("close", "Close"), close, "btn btn-sm btn-outline-secondary"));
        content.append(header, body, footer);
        dialog.append(content);
        root.append(dialog);
        root.addEventListener("focusin", event => {
            if (!shown && event.target !== root) openingFocus = event.target;
        });
        root.addEventListener("shown.bs.modal", () => {
            shown = true;
            if (closeRequested) modal.hide();
            else if (openingFocus?.isConnected) openingFocus.focus({ preventScroll: true });
        }, { once: true });
        root.addEventListener("hidden.bs.modal", finish, { once: true });
        root.addEventListener("keydown", event => {
            if (event.key === "Escape") { event.preventDefault(); close(); }
        });
        document.body.append(root);
        if (window.bootstrap?.Modal) modal = new window.bootstrap.Modal(root);
        const api = { root, body, footer, signal: lifecycle.signal, close, destroy: finish, setCleanup: value => { cleanup = value; } };
        this._dialogs.add(api);
        if (modal) modal.show();
        else { root.classList.add("show"); root.style.display = "block"; }
        return api;
    }

    /** Opens an accessible widget dialog with the dashboard's cleanup lifecycle. */
    showDialog(title) {
        return this._dialog(title);
    }

    showCatalogue() {
        const dialog = this._dialog(this._t("add", "Add widget"));
        const search = node("input", "form-control mb-3");
        search.type = "search";
        search.setAttribute("aria-label", this._t("searchWidgets", "Search widgets"));
        search.placeholder = this._t("searchWidgets", "Search widgets");
        const list = node("div", "md-dashboard__catalogue");
        const error = node("p", "text-danger mb-0");
        error.setAttribute("role", "alert");
        if (this.settings.items.length >= MAX_WIDGETS) error.textContent = this._t("limit", "The overview can contain at most 32 widgets.");
        dialog.footer.append(error);
        const resetDetails = node("div", "w-100");
        resetDetails.hidden = true;
        const resetDescription = node("p", "mb-2", this._t("resetDescription", "Restore the default widgets, sizes and order? Widget filters in all domains and read news will also be reset. Other account settings and bookmarks will be kept."));
        resetDescription.id = `dashboard-reset-${createInstanceId()}`;
        const confirmReset = button(this._t("resetConfirm", "Restore defaults"), async () => {
            confirmReset.disabled = true;
            if (await this.reset()) dialog.close();
            else {
                confirmReset.disabled = false;
                error.textContent = this._t("saveError", "The change could not be saved. Your previous settings were kept.");
            }
        }, "btn btn-sm btn-primary");
        confirmReset.setAttribute("aria-describedby", resetDescription.id);
        const reset = button(this._t("reset", "Reset"), () => {
            resetDetails.hidden = !resetDetails.hidden;
            reset.setAttribute("aria-expanded", String(!resetDetails.hidden));
            if (!resetDetails.hidden) confirmReset.focus();
        }, "btn btn-sm btn-outline-secondary md-dashboard__reset");
        reset.setAttribute("aria-expanded", "false");
        resetDetails.append(resetDescription, confirmReset);
        dialog.footer.append(reset, resetDetails);
        const render = () => {
            list.replaceChildren();
            for (const definition of listWidgets()) {
                if (!this._available(definition) || ["sessions", "news", "search"].includes(definition.type)) continue;
                const existing = this.settings.items.find(item => item.type === definition.type);
                const reveal = !definition.multiple && existing && !this._visible(definition, existing) && definition.reveal;
                if (!definition.multiple && existing && !reveal) continue;
                const title = this._t(definition.titleKey);
                if (!title.toLocaleLowerCase().includes(search.value.toLocaleLowerCase())) continue;
                const item = node("div", "md-dashboard__catalogue-item");
                item.dataset.widgetType = definition.type;
                const text = node("div");
                text.append(node("strong", "", title));
                if (definition.descriptionKey) text.append(node("p", "mb-0 small text-muted", this._t(definition.descriptionKey)));
                const add = button(reveal ? this._t("show", "Show") : this._t("add", "Add widget"), async () => {
                    if (definition.type === "shortcut") {
                        if (window.bootstrap?.Modal) dialog.root.addEventListener("hidden.bs.modal", () => this.showAddWidget(definition.type), { once: true });
                        dialog.close();
                        if (!window.bootstrap?.Modal) this.showAddWidget(definition.type);
                        return;
                    }
                    add.disabled = true;
                    try {
                        const saved = reveal ? await definition.reveal(cloneSettings(existing), this._widgetContext()) : await this.add(definition.type);
                        if (saved) dialog.close();
                        else { add.disabled = false; error.textContent = this._t("saveError", "The change could not be saved."); }
                    } catch (failure) {
                        add.disabled = false;
                        error.textContent = this._t("saveError", "The change could not be saved.");
                    }
                });
                add.disabled = !reveal && this.settings.items.length >= MAX_WIDGETS;
                item.append(text, add);
                list.append(item);
            }
            if (!list.children.length) list.append(node("p", "", this._t("noWidgets", "No matching widgets are available.")));
        };
        search.addEventListener("input", render);
        dialog.body.append(search, list);
        render();
        dialog.root.addEventListener("shown.bs.modal", () => search.focus(), { once: true });
    }

    showMove(id) {
        const instance = this._instance(id);
        if (!instance) return;
        const dialog = this._dialog(this._t("move", "Move widget"));
        const label = node("label", "form-label", this._t("moveBefore", "Move before"));
        const select = node("select", "form-select");
        select.id = `dashboard-move-${id}`;
        label.htmlFor = select.id;
        this.settings.items.filter(item => item.id !== id && this.views.has(item.id) && this._region(item) === this._region(instance)).forEach(item => {
            const option = node("option", "", this._title(item));
            option.value = item.id;
            select.append(option);
        });
        const end = node("option", "", this._t("moveEnd", "At the end"));
        end.value = "";
        select.append(end);
        const following = this.settings.items.slice(this.settings.items.indexOf(instance) + 1).find(item => this.views.has(item.id) && this._region(item) === this._region(instance));
        select.value = following?.id || "";
        dialog.body.append(label, select);
        const save = button(this._t("move", "Move widget"), async () => {
            save.disabled = true;
            if (await this.moveBefore(id, select.value || null)) dialog.close();
            else save.disabled = false;
        }, "btn btn-primary");
        dialog.footer.append(save);
    }

    async showSettings(id) {
        const instance = this._instance(id);
        if (!instance) return;
        return this._showSettings(instance);
    }

    /** Configures a new instance before its first atomic save. Cancelling adds nothing. */
    async showAddWidget(type) {
        const definition = getWidget(type);
        if (!definition || !this._available(definition) || this.settings.items.length >= MAX_WIDGETS) {
            this.status.textContent = this._t("limit", "The overview can contain at most 32 widgets.");
            return;
        }
        if (!definition.multiple && this.settings.items.some(item => item.type === type)) return;
        return this._showSettings(this._newInstance(definition), true);
    }

    async _showSettings(instance, adding = false) {
        const id = instance.id;
        const definition = getWidget(instance.type);
        const dialog = this._dialog(this._t(adding ? "addShortcut" : "settings", adding ? "Add shortcut" : "Widget settings"));
        const sizeLabel = node("label", "form-label", this._t("size", "Size"));
        const size = node("select", "form-select mb-3");
        size.id = `dashboard-size-${id}`;
        sizeLabel.htmlFor = size.id;
        definition.sizes.forEach(value => {
            const option = node("option", "", value === "fullauto" ? this._t("fullWidth", "Full width") : value.replace("x", " × "));
            option.value = value;
            size.append(option);
        });
        size.value = instance.size;
        const preview = node("figure", "md-dashboard__size-preview");
        const previewGrid = node("div", "md-dashboard__size-grid");
        previewGrid.setAttribute("aria-hidden", "true");
        const previewCaption = node("figcaption", "small text-muted");
        const updatePreview = () => {
            const dimensions = size.value === "fullauto" ? [6, 3] : size.value.split("x").map(Number);
            previewGrid.replaceChildren(...Array.from({ length: 18 }, (_, index) => node("span", index % 6 < dimensions[0] && Math.floor(index / 6) < dimensions[1] ? "is-selected" : "")));
            previewCaption.textContent = `${this._t("sizePreview", "Size preview")}: ${size.selectedOptions[0].textContent}`;
        };
        size.addEventListener("change", updatePreview);
        updatePreview();
        preview.append(previewGrid, previewCaption);
        if (definition.sizes.length > 1) dialog.body.append(sizeLabel, size, preview);
        const fields = node("div", "md-dashboard__settings");
        const error = node("p", "text-danger");
        error.setAttribute("role", "alert");
        dialog.body.append(fields, error);
        let configuration;
        const save = button(this._t("save", "Save"), async () => {
            error.textContent = "";
            try {
                const values = await configuration?.read?.() || {};
                const next = cloneSettings(this.settings);
                if (adding) {
                    next.items.push(cloneSettings(instance));
                    next.domainOptions[id] = cloneSettings(definition.defaultDomainOptions);
                }
                const updated = next.items.find(item => item.id === id);
                if (!updated) return;
                updated.size = size.value;
                if (updated.size === "1x1") updated.collapsed = false;
                if (values.options !== undefined) updated.options = cloneSettings(values.options);
                if (values.domainOptions !== undefined) next.domainOptions[id] = cloneSettings(values.domainOptions);
                save.disabled = true;
                if (await this._commit(next)) dialog.close();
                else { error.textContent = this._t("saveError", "The change could not be saved."); save.disabled = false; }
            } catch (failure) {
                error.textContent = failure.message || this._t("invalidSettings", "Check the widget settings.");
            }
        }, "btn btn-primary");
        dialog.footer.append(save);
        if (definition.configure) {
            save.disabled = true;
            try {
                configuration = await definition.configure({ container: fields, instance: cloneSettings(instance), options: cloneSettings(instance.options || {}), domainOptions: cloneSettings(this.settings.domainOptions[id] || definition.defaultDomainOptions), context: this._widgetContext(), signal: dialog.signal });
                if (dialog.signal.aborted) dispose(configuration);
                else { dialog.setCleanup(configuration); save.disabled = false; }
            } catch (failure) {
                if (!dialog.signal.aborted) error.textContent = this._t("widgetError", "This widget could not be loaded.");
            }
        }
    }

    _bindDrag() {
        const $ = window.jQuery;
        if (!$?.fn.draggable || !$.fn.droppable) return;
        if (!$(this.dropEnd).data("ui-droppable")) $(this.dropEnd).droppable({
            accept: ".md-dashboard__widget", tolerance: "pointer",
            drop: () => { if (this._dragged) this.moveBefore(this._dragged); }
        });
        for (const view of this.views.values()) {
            if (!["grid", "shortcut"].includes(this._region(view.instance))) continue;
            if ($(view.card).data("ui-draggable")) $(view.card).draggable("option", "disabled", !this.editing);
            if (!this.editing) continue;
            if (!$(view.card).data("ui-draggable")) $(view.card).draggable({
                handle: ".md-dashboard__drag", appendTo: "body", zIndex: 1100, distance: 8,
                cancel: "input, textarea, select, option",
                helper: () => {
                    const helper = $(view.card).clone().removeAttr("data-instance-id").attr({ "aria-hidden": "true", inert: "" });
                    helper.find("[id]").removeAttr("id");
                    return helper;
                },
                start: () => { if (this.saving || !this.editing) return false; this._dragged = view.instance.id; this.host.classList.add("is-dragging"); },
                stop: () => { this._dragged = null; this.host.classList.remove("is-dragging"); this.host.querySelectorAll(".is-drop-target").forEach(card => card.classList.remove("is-drop-target")); },
                revert: "invalid"
            });
            if (!$(view.card).data("ui-droppable")) $(view.card).droppable({
                accept: dragged => this._region(this._instance(dragged[0].dataset.instanceId) || {}) === this._region(view.instance), tolerance: "pointer",
                over: () => { if (this._dragged !== view.instance.id) view.card.classList.add("is-drop-target"); },
                out: () => view.card.classList.remove("is-drop-target"),
                drop: () => { if (this._dragged && this._dragged !== view.instance.id) this.moveBefore(this._dragged, view.instance.id); }
            });
        }
    }

    /** Reloads active-domain options without replacing the overview's security UI. */
    async setContext(context) {
        this.context = context;
        this._contextVersion++;
        this.layout.hidden = true;
        for (const view of this.views.values()) this._disposeView(view);
        for (const dialog of this._dialogs) dialog.destroy();
        this.removed = null;
        this.undoContainer.hidden = true;
        await this.start();
    }

    destroy() {
        this.destroyed = true;
        this._request?.abort();
        for (const view of this.views.values()) this._disposeView(view);
        for (const dialog of this._dialogs) dialog.destroy();
        const $ = window.jQuery;
        if ($?.fn.droppable && $(this.dropEnd).data("ui-droppable")) $(this.dropEnd).droppable("destroy");
        this.views.clear();
    }
}
