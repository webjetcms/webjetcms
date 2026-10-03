import { getWidget, listWidgets } from './registry';
import { MAX_WIDGETS, cloneSettings, createInstanceId, normalizeSettings, moveInstanceBefore, createLayoutSegments } from './model';
import { link, localUrl, shortcutUrl, containNativeScroll } from './widget-utils';
import { DashboardEditor } from './editor';

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
 * Bootstrap context before the controller adds widget-specific services.
 * @typedef {Omit<import('./registry').WidgetContext, 'dashboard'|'settings'>} DashboardContext
 */

/**
 * DOM and resource state retained for one displayed widget instance.
 * @typedef {Object} DashboardView
 * @property {HTMLElement} card - Widget section observed for visibility and drag actions.
 * @property {HTMLElement} header - Title and arrangement controls.
 * @property {HTMLElement} title - Accessible heading.
 * @property {HTMLElement} titleText - Text span inside the heading.
 * @property {HTMLElement} body - Container replaced during refresh.
 * @property {import('./model').WidgetInstance} instance - Current effective instance.
 * @property {AbortController|null} abort - Controller for the current render.
 * @property {import('./registry').WidgetCleanup|null} cleanup - Resources returned by the current renderer.
 * @property {string|null} signature - Serialized inputs used to detect a required refresh.
 * @property {(function(): void)|null} [onVisible] - Pending viewport-entry completion callback.
 */

/**
 * Shared modal API with abort and resource cleanup tied to its lifetime.
 * @typedef {Object} DashboardDialog
 * @property {HTMLDivElement} root - Modal root attached to the document body.
 * @property {HTMLDivElement} body - Container for dialog controls.
 * @property {HTMLDivElement} footer - Container for dialog actions.
 * @property {AbortSignal} signal - Aborted when the dialog is destroyed or finishes closing.
 * @property {function(): void} close - Requests a close after any opening transition.
 * @property {function(): void} destroy - Immediately releases resources and restores focus to the trigger when connected.
 * @property {function(import('./registry').WidgetCleanup|import('./registry').WidgetConfiguration): void} setCleanup - Replaces the resource callback or object disposed when the dialog closes.
 */

/**
 * Owns dashboard layout, accessible controls, server persistence and widget
 * lifecycles. It never replaces the surrounding system alerts or overview.
 * Layout updates emit webjet-dashboard-rendered on the host; the event bubbles,
 * is not cancelable and has no detail payload. Widget data may still be loading.
 */
export class DashboardController {
    /**
     * Builds the dashboard shell in the supplied host and prepares lazy widget rendering.
     * @param {HTMLElement} host - Dashboard-owned element whose children are replaced during construction.
     * @param {Partial<DashboardContext>} [context={}] - Initial page context; supply bootstrap data before calling start.
     */
    constructor(host, context = {}) {
        this.host = host;
        this.context = context;
        this.settings = normalizeSettings();
        this.views = new Map();
        this._visibilityObserver = typeof window.IntersectionObserver === "function" ? new window.IntersectionObserver(entries => {
            for (const entry of entries) {
                const view = this.views.get(entry.target.dataset.instanceId);
                if (entry.isIntersecting && view?.card === entry.target) view.onVisible?.();
            }
        }) : null;
        this.saving = false;
        this.destroyed = false;
        this.removed = null;
        this._dialogs = new Set();
        this._request = null;
        this._contextVersion = 0;
        this.editing = false;
        this.editingShortcuts = false;
        this._build();
    }

    /**
     * Resolves a full translation key or dashboard suffix, using fallback text for missing translations.
     * @param {string} key - Full key when it contains a dot, otherwise a dashboard key suffix.
     * @param {string} [fallback=key] - Text used when neither translator produces a translated value.
     * @param {...(string|number)} params - Substitutions forwarded to the selected translator.
     * @returns {string} Translated or fallback text.
     */
    _t(key, fallback = key, ...params) {
        const fullKey = key.includes(".") ? key : `admin.dashboard.${key}.js`;
        const translated = this.context.translate?.(fullKey, ...params) ?? window.WJ?.translate?.(fullKey, ...params);
        return translated && translated !== fullKey ? translated : fallback;
    }

    get status() {
        return this.editingShortcuts ? this.shortcutStatus : this.overviewStatus;
    }

    /** Builds the welcome, fixed-widget and grid regions, replacing host content and initially disabling preference controls. */
    _build() {
        this.host.classList.add("md-dashboard");
        this.hero = node("div", "md-dashboard__hero");
        if (this.context.config?.heroBackgroundImage !== undefined) {
            const backgroundImage = shortcutUrl(this.context.config.heroBackgroundImage);
            this.hero.style.setProperty("--wj-dashboard-hero-image", backgroundImage ? `url(${JSON.stringify(backgroundImage)})` : "none");
        }
        const welcome = node("div", "md-dashboard__welcome");
        const language = window.userLng === "cz" ? "cs" : window.userLng || "sk";
        const meta = node("div", "md-dashboard__welcome-meta");
        meta.append(node("p", "md-dashboard__eyebrow", new Intl.DateTimeFormat(language, { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(new Date())));
        welcome.append(meta);
        const welcomeHeading = this.welcomeHeading = node("div", "md-dashboard__welcome-heading");
        welcomeHeading.append(node("h1", "md-dashboard__greeting", `${this._t("welcomeBack", "Welcome back,")} ${this.context.data?.userName || ""}`.trim()));
        welcome.append(welcomeHeading);
        this.news = node("div", "md-dashboard__news");
        welcome.append(this.news);
        this.sessions = node("div", "md-dashboard__sessions");
        this.hero.append(welcome, this.sessions);
        this.notices = node("div", "md-dashboard__notices");
        this.notices.id = "toast-container-overview";
        this.search = node("div", "md-dashboard__search");
        this.toolbar = node("div", "md-dashboard__toolbar");
        const toolbarHeading = node("div", "md-dashboard__toolbar-heading");
        this.toolbarTitle = node("h2", "md-dashboard__title", this._t("title", "My overview"));
        this.editHint = node("p", "md-dashboard__edit-hint", this._t("editHint", "Drag widgets by their grips; change their size in settings."));
        this.editHint.hidden = true;
        toolbarHeading.append(this.toolbarTitle, this.editHint);
        this.toolbar.append(toolbarHeading);
        const actions = node("div", "md-dashboard__toolbar-actions");
        this.editButton = button(this._t("editOverview", "Edit overview"), () => this.editing ? this.saveEditing() : this.setEditing(true), "btn btn-sm btn-outline-secondary md-dashboard__control");
        this.editButton.setAttribute("aria-pressed", "false");
        this.editButton.prepend(icon("ti-adjustments-horizontal"));
        this.addButton = button(this._t("add", "Add widget"), () => this.showCatalogue(), "btn btn-sm btn-outline-secondary md-dashboard__control md-dashboard__edit-control");
        this.addButton.prepend(icon("ti-plus"));
        this.addButton.hidden = true;
        this.resetButton = button(this._t("resetConfirm", "Restore defaults"), event => this.showReset(event.shiftKey), "btn btn-sm btn-outline-secondary md-dashboard__control md-dashboard__edit-control md-dashboard__reset");
        this.resetButton.prepend(icon("ti-refresh"));
        this.resetButton.title = this._t("resetTooltip", "Restore the standard widgets, sizes and order. Hold Shift to show all widgets in every size.");
        this.resetButton.hidden = true;
        if (this.context.overview?.showFeedbackModal) {
            const feedback = button(this._t("admin.welcome.feedback.sendButton.js", "Send feedback"), () => this.context.overview.showFeedbackModal(), "btn btn-sm btn-outline-secondary md-dashboard__feedback");
            feedback.prepend(icon("ti-message-2"));
            actions.append(feedback);
        }
        this.cancelButton = button(this._t("button.cancel", "Cancel"), () => this.cancelEditing(), "btn btn-sm btn-outline-secondary md-dashboard__control md-dashboard__edit-control md-dashboard__cancel");
        this.cancelButton.hidden = true;
        actions.append(this.resetButton, this.addButton, this.cancelButton, this.editButton);
        this.toolbar.append(actions);
        this.overviewStatus = node("div", "md-dashboard__status");
        this.status.setAttribute("role", "status");
        this.status.setAttribute("aria-live", "polite");
        this.status.tabIndex = -1;
        this.undoContainer = node("div", "md-dashboard__undo");
        this.undoContainer.hidden = true;
        this.layout = node("div", "md-dashboard__layout");
        this.widgetMoveHint = node("p", "visually-hidden", this._t("widgetMoveHint", "Space lifts the widget, arrows move it, Enter drops it, Escape cancels."));
        this.widgetMoveHint.id = `dashboard-widget-move-${createInstanceId()}`;
        this.widgetMoveStatus = node("div", "visually-hidden");
        this.widgetMoveStatus.setAttribute("role", "status");
        this.widgetMoveStatus.setAttribute("aria-live", "polite");
        this.widgetMoveStatus.setAttribute("aria-atomic", "true");
        this.shortcuts = node("section", "md-dashboard__shortcuts");
        this.shortcuts.setAttribute("aria-label", this._t("shortcuts", "Your shortcuts"));
        this.shortcutActions = node("div", "md-dashboard__shortcut-actions");
        this.addShortcutButton = button(this._t("addShortcut", "Add shortcut"), () => this.showAddWidget("shortcut"), "btn btn-sm btn-outline-secondary md-dashboard__control md-dashboard__shortcut-add");
        this.addShortcutButton.prepend(icon("ti-plus"));
        this.editShortcutsButton = button(this._t("editShortcuts", "Edit shortcuts"), () => this.setEditingShortcuts(!this.editingShortcuts), "btn btn-sm btn-outline-secondary md-dashboard__control md-dashboard__shortcut-edit");
        this.editShortcutsButton.replaceChildren(icon("ti-pencil"), node("span", "visually-hidden", this._t("editShortcuts", "Edit shortcuts")));
        this.editShortcutsButton.title = this._t("editShortcuts", "Edit shortcuts");
        // Finish tooltip cleanup before moving the focused control to the heading.
        this.editShortcutsButton.setAttribute("data-bs-animation", "false");
        this.editShortcutsButton.setAttribute("aria-pressed", "false");
        this.shortcutActions.append(this.addShortcutButton, this.editShortcutsButton);
        this.shortcutStatus = node("div", "md-dashboard__status");
        this.shortcutStatus.setAttribute("role", "status");
        this.shortcutStatus.setAttribute("aria-live", "polite");
        this.shortcutStatus.tabIndex = -1;
        this.shortcutList = node("div", "md-dashboard__shortcut-list");
        this.shortcuts.append(this.shortcutList, this.shortcutStatus);
        const moveHint = node("p", "visually-hidden", this._t("shortcutMoveHint", "Space lifts the shortcut, Left and Right move it, Enter drops it, Escape cancels."));
        moveHint.id = `dashboard-shortcut-move-${createInstanceId()}`;
        this.shortcutMoveHint = moveHint;
        this.shortcuts.append(moveHint);
        welcome.insertBefore(this.shortcuts, this.news);
        this.host.replaceChildren(this.hero, this.notices, this.search, this.toolbar, this.overviewStatus, this.undoContainer, this.layout, this.widgetMoveHint, this.widgetMoveStatus);
        if (window.jQuery && window.WJ?.initTooltip) {
            for (const control of [this.resetButton, this.editShortcutsButton]) window.WJ.initTooltip(window.jQuery(control));
        }
        this._setBusy(true);
    }

    /**
     * Reveals arrangement controls without changing or saving widget preferences.
     * Entering overview edit mode exits shortcut edit mode.
     * @param {boolean} editing - Whether overview arrangement controls should be visible.
     */
    setEditing(editing) {
        if (Boolean(editing) === this.editing) return;
        if (editing && this.editingShortcuts) this.setEditingShortcuts(false);
        if (editing) this.editor = new DashboardEditor(this);
        else {
            for (const dialog of this._dialogs) dialog.destroy();
            this.editor?.destroy();
            this.editor = null;
        }
        this.editing = Boolean(editing);
        if (!this.editing) window.bootstrap?.Tooltip?.getInstance(this.resetButton)?.hide();
        this.host.classList.toggle("is-editing", this.editing);
        this.editButton.textContent = this._t(this.editing ? "save" : "editOverview", this.editing ? "Save" : "Edit overview");
        this.editButton.prepend(icon(this.editing ? "ti-check" : "ti-adjustments-horizontal"));
        this.editButton.setAttribute("aria-pressed", String(this.editing));
        this.editButton.classList.toggle("btn-primary", this.editing);
        this.editButton.classList.toggle("btn-outline-secondary", !this.editing);
        this.toolbarTitle.textContent = this._t(this.editing ? "editOverview" : "title", this.editing ? "Edit overview" : "My overview");
        this.editHint.hidden = !this.editing;
        this.widgetMoveStatus.textContent = "";
        this.toolbar.querySelectorAll(".md-dashboard__edit-control").forEach(control => { control.hidden = !this.editing; });
        this._updateEditing();
        this.editor?.positionToolbar();
    }

    /** Atomically saves the widget draft, retaining it for retry if the server rejects it. */
    async saveEditing() {
        if (!this.editor || this.saving) return false;
        this.editor.finishMove(true);
        for (const dialog of this._dialogs) dialog.destroy();
        if (this.editor.dirty && !await this._commit(cloneSettings(this.settings), this.overviewStatus, { notify: false, draft: false, overviewSave: true })) return false;
        this.setEditing(false);
        this.editButton.focus({ preventScroll: true });
        window.WJ.notifySuccess(this._t("overviewSaved", "The overview has been saved."), "", 10000);
        return true;
    }

    /** Confirms discarding a dirty widget draft before closing it or entering shortcut editing. */
    cancelEditing(after) {
        if (!this.editor || this.saving) return;
        this.editor.finishMove(false);
        const cancel = () => {
            this.settings = cloneSettings(this.editor.original);
            this.setEditing(false);
            this._render();
            this.overviewStatus.textContent = "";
            this.editButton.focus({ preventScroll: true });
            after?.();
        };
        if (!this.editor.dirty) { cancel(); return; }
        const dialog = this._dialog(this._t("discardConfirm", "Discard unsaved changes?"));
        dialog.root.classList.add("md-dashboard-modal--confirm");
        dialog.body.append(node("p", "mb-0", this._t("discardDescription", "Widget positions, sizes and settings changed during this edit will be discarded. Your shortcuts will be kept.")));
        const keep = button(this._t("continueEditing", "Continue editing"), () => dialog.close(), "btn btn-outline-secondary");
        const discard = button(this._t("discardChanges", "Discard changes"), () => { dialog.close(); cancel(); }, "btn btn-danger");
        dialog.footer.append(keep, discard);
        dialog.root.addEventListener("shown.bs.modal", () => keep.focus({ preventScroll: true }), { once: true });
        keep.focus({ preventScroll: true });
    }

    /**
     * Shortcuts own their edit mode independently of the overview widgets.
     * Entering shortcut edit mode exits overview edit mode.
     * @param {boolean} editing - Whether shortcut arrangement controls should be visible.
     */
    setEditingShortcuts(editing) {
        if (editing && this.editing) { this.cancelEditing(() => this.setEditingShortcuts(true)); return; }
        this.editingShortcuts = Boolean(editing);
        if (!this.editingShortcuts) this._finishShortcutMove(false);
        this.host.classList.toggle("is-editing-shortcuts", this.editingShortcuts);
        const label = this._t(editing ? "finishEditing" : "editShortcuts", editing ? "Done" : "Edit shortcuts");
        const restoreFocus = document.activeElement === this.editShortcutsButton;
        const tooltip = window.bootstrap?.Tooltip?.getInstance(this.editShortcutsButton);
        window.jQuery?.(this.editShortcutsButton).off(".wjFocusWithoutTooltip");
        tooltip?.hide();
        if (editing) tooltip?.disable();
        else tooltip?.enable();
        this.editShortcutsButton.removeAttribute("title");
        this.editShortcutsButton.replaceChildren(icon(editing ? "ti-check" : "ti-pencil"), node("span", editing ? "" : "visually-hidden", label));
        this.editShortcutsButton.setAttribute("aria-pressed", String(this.editingShortcuts));
        (editing ? this.welcomeHeading : this.shortcutActions).append(this.editShortcutsButton);
        if (restoreFocus) {
            if (!editing && window.WJ?.focusWithoutTooltip) window.WJ.focusWithoutTooltip(this.editShortcutsButton);
            else this.editShortcutsButton.focus({ preventScroll: true });
        }
        this._updateEditing();
    }

    _isEditing(instance) {
        return instance.type === "shortcut" ? this.editingShortcuts : this.editing;
    }

    _updateEditing() {
        for (const view of this.views.values()) {
            view.card.querySelectorAll(".md-dashboard__edit-control").forEach(control => { control.hidden = !this._isEditing(view.instance); });
            if (view.instance.type === "shortcut") this._updateShortcutLink(view);
        }
        for (const view of this.views.values()) {
            window.bootstrap?.Dropdown?.getInstance(view.header.querySelector('[data-bs-toggle="dropdown"]'))?.hide();
            for (const target of view.header.querySelectorAll('[data-dashboard-tooltip]')) window.bootstrap?.Tooltip?.getInstance(target)?.hide();
        }
        this._bindDrag();
    }

    _region(instance) {
        return ["sessions", "news", "search"].includes(instance.type) ? instance.type : instance.type === "shortcut" ? "shortcut" : "grid";
    }

    /**
     * Keeps fixed utilities visible without rewriting saved instances or their preferences.
     * @returns {import('./model').WidgetInstance[]} Available instances, including transient fixed utilities and only visible grid widgets.
     */
    _displayItems() {
        const items = [...this.settings.items];
        for (const type of ["sessions", "news", "search"]) {
            const definition = getWidget(type);
            if (definition && !items.some(item => item.type === type)) items.push({ ...this._newInstance(definition), id: `dashboard-fixed-${type}` });
        }
        return items.filter(instance => {
            const definition = getWidget(instance.type);
            return definition && this._available(definition) && (this._region(instance) !== "grid" || this._visible(definition, instance));
        });
    }

    /**
     * Renders the current account's layout and active-domain filters supplied by the page.
     * Applies defaults for an unconfigured profile, ensures mandatory types and attempts legacy bookmark import.
     * @returns {Promise<void>} Resolves after bookmark import finishes; individual widget renders may still be pending.
     */
    async start() {
        this._request?.abort();
        this.settings = normalizeSettings(this.context.data.settings);
        if (!this.settings.configured) this._addDefaults();
        this._ensureMandatory();
        this.status.textContent = "";
        this.shortcutStatus.textContent = "";
        this._setBusy(false);
        this.layout.hidden = false;
        this._render();
        this.host.dataset.loaded = "true";
        await this.importLegacyBookmarks();
    }

    /**
     * Creates an instance with a fresh ID, the selected size and copied shared defaults.
     * @param {import('./registry').WidgetDefinition} definition - Registered widget with resolved defaults.
     * @param {Partial<import('./model').WidgetInstance>} [values={}] - Size and shared-option overrides; identity and type are generated from the definition.
     * @returns {import('./model').WidgetInstance} A new instance without domain options or persistence side effects.
     */
    _newInstance(definition, values = {}) {
        return { id: createInstanceId(), type: definition.type, size: values.size || definition.defaultSize, options: { ...cloneSettings(definition.defaultOptions), ...values.options } };
    }

    /** Adds available configured presets up to the instance limit, preserving configured shortcuts and singleton instances. */
    _addDefaults() {
        for (const preset of this.context.config?.dashboardDefaults || []) {
            if (preset.type === "shortcut" && this.settings.shortcutsConfigured) continue;
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

    _headerHref(instance) {
        const href = getWidget(instance.type)?.headerLink?.href;
        return typeof href === "function" ? href(instance, this._widgetContext()) : href;
    }

    _showFailure(key, fallback) {
        this.status.replaceChildren(node("span", "text-danger", this._t(key, fallback)));
    }

    /**
     * Stages widget changes during editing or saves independent preferences immediately.
     * A failed overview save retains the draft; independent saves preserve the confirmed widget layout.
     *
     * @param {import('./model').DashboardSettings} next - Complete proposed layout and current-domain options.
     * @param {HTMLElement} [status=this.status] - Region receiving progress and persistence errors.
     * @param {Object} [options={}] - Controls staging, final overview persistence, notification and shortcut animation.
     * @returns {Promise<boolean>} True after staging or applying the response; false for a blocked, invalid, aborted or failed save.
     */
    async _commit(next, status = this.status, { notify = true, animateShortcuts = false, draft = true, overviewSave = false } = {}) {
        if (this.saving || this.destroyed || this.host.dataset.loaded !== "true") return false;
        this._finishShortcutMove(false);
        if (next.items.length > MAX_WIDGETS) {
            status.textContent = this._t("limit", "The overview can contain at most 48 widgets.");
            return false;
        }
        if (this.editor && draft) return this.editor.stage(next);
        const independent = this.editor && !overviewSave;
        if (independent) next = this.editor.mergeIndependent(this.editor.original, next);
        this.saving = true;
        this._setBusy(true);
        status.textContent = this._t("saving", "Saving…");
        const request = this._request = new AbortController();
        try {
            const response = await fetch(overviewSave && this.editor.reset ? "/admin/rest/dashboard/settings/reset" : "/admin/rest/dashboard/settings", {
                method: "PUT", credentials: "same-origin", signal: request.signal,
                headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken || "" },
                body: JSON.stringify(next)
            });
            if (!response.ok) throw new Error(`Dashboard settings: ${response.status}`);
            const saved = await response.json();
            if (this.destroyed || request.signal.aborted) return false;
            this.context.data.settings = saved;
            if (independent) this.editor.sync(normalizeSettings(saved));
            else this.settings = normalizeSettings(saved);
            this.settings.configured = true;
            this.removed = null;
            this.undoContainer.hidden = true;
            this._clearShortcutUndo();
            status.textContent = "";
            const positions = animateShortcuts ? new Map([...this.shortcutList.querySelectorAll('[data-instance-id]')].map(card => [card.dataset.instanceId, card.getBoundingClientRect()])) : null;
            this._render();
            if (positions && !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
                for (const [id, previous] of positions) {
                    const card = this.views.get(id)?.card;
                    if (!card) continue;
                    const current = card.getBoundingClientRect();
                    if (current.left !== previous.left || current.top !== previous.top) card.animate?.([
                        { transform: `translate(${previous.left - current.left}px, ${previous.top - current.top}px)` }, { transform: 'none' }
                    ], { duration: 180, easing: 'ease-out' });
                }
            }
            if (notify) window.WJ.notifySuccess(this._t("saved", "Saved."), "", 10000);
            return true;
        } catch (error) {
            if (!this.destroyed && !request.signal.aborted) status.replaceChildren(node("span", "text-danger", this._t(overviewSave ? "draftSaveError" : "saveError", overviewSave ? "The overview could not be saved. Your changes are still available; try again." : "The change could not be saved. Your previous settings were kept.")));
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

    /**
     * Reconciles displayed instances, reuses matching views and refreshes changed inputs while preserving focus.
     * Dispatches a bubbling, noncancelable webjet-dashboard-rendered event without detail on the host.
     */
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
            const navigation = view.header.querySelector('.md-dashboard__title-link, .md-dashboard__header-link');
            if (navigation) {
                const nextLink = link("", this._headerHref(instance), navigation.className);
                if (nextLink.tagName === navigation.tagName) {
                    if (nextLink.hasAttribute("href")) navigation.setAttribute("href", nextLink.getAttribute("href"));
                } else {
                    nextLink.append(...navigation.childNodes);
                    navigation.replaceWith(nextLink);
                }
            }
            view.card.dataset.size = instance.size;
            const signature = JSON.stringify([instance.type, instance.size, instance.options, this.settings.domainOptions[instance.id], this._contextVersion, instance.type === "news" ? this.settings.acknowledgedNewsVersion : null]);
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
        if (!this.shortcutList.children.length) this.shortcutList.append(node("p", "md-dashboard__shortcuts-empty", this._t("shortcutsEmpty", "No shortcuts.")));
        this.shortcutList.append(this.shortcutActions);
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

    /**
     * Builds an accessible widget shell and binds its navigation and preference actions.
     * @param {import('./model').WidgetInstance} instance - Available instance with a registered definition.
     * @returns {DashboardView} A detached view whose content has not yet been rendered.
     */
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
            const titleLink = link("", this._headerHref(instance), "md-dashboard__title-link");
            titleLink.append(titleText, icon("ti-arrow-up-right"));
            title.append(titleLink);
        } else title.append(titleText);
        title.id = `dashboard-title-${instance.id}`;
        card.setAttribute("aria-labelledby", title.id);
        if (definition.icon) header.append(icon(definition.icon));
        header.append(title);
        if (definition.headerLink?.labelKey) {
            const headerLink = link(this._t(definition.headerLink.labelKey), this._headerHref(instance), "md-dashboard__header-link");
            headerLink.append(icon("ti-arrow-up-right"));
            header.append(headerLink);
        }
        const drag = button(this._t("move", "Move widget"), () => this.showMove(instance.id), "btn btn-sm md-dashboard__drag md-dashboard__control");
        drag.replaceChildren(icon("ti-grip-vertical"));
        drag.setAttribute("aria-label", this._t("move", "Move widget"));
        drag.addEventListener("click", event => {
            if (this.editor?.suppressClick) { event.stopImmediatePropagation(); this.editor.suppressClick = false; }
        }, { capture: true });
        if (instance.type === "shortcut") {
            card.classList.add("md-dashboard__shortcut-card");
            header.classList.add("visually-hidden");
            drag.classList.add("md-dashboard__edit-control");
            drag.setAttribute("aria-label", this._t("moveShortcut", "Move shortcut"));
            drag.setAttribute("aria-describedby", this.shortcutMoveHint.id);
            drag.setAttribute("aria-pressed", "false");
            drag.addEventListener("keydown", event => this._shortcutMoveKey(event, instance.id));
            const remove = button("", () => this.remove(instance.id), "btn btn-sm md-dashboard__shortcut-remove md-dashboard__control md-dashboard__edit-control");
            remove.append(icon("ti-x"));
            remove.setAttribute("aria-label", this._t("removeShortcut", "Remove shortcut"));
            remove.title = this._t("removeShortcut", "Remove shortcut");
            drag.hidden = remove.hidden = !this.editingShortcuts;
            const body = node("div", "md-dashboard__widget-body");
            body.id = `dashboard-body-${instance.id}`;
            const edit = event => {
                if (!this.editingShortcuts || !event.target.closest("a")) return;
                event.preventDefault();
                if (!this.saving) {
                    this._finishShortcutMove(false);
                    this.showSettings(instance.id);
                }
            };
            body.addEventListener("click", edit);
            body.addEventListener("keydown", event => { if (event.key === "Enter" || event.key === " ") edit(event); });
            card.append(header, drag, body, remove);
            return { card, header, title, titleText, body, instance, abort: null, cleanup: null, signature: null };
        }
        const dropdown = node("div", "dropdown");
        drag.classList.add("md-dashboard__edit-control");
        drag.hidden = !this.editing;
        drag.setAttribute("aria-describedby", this.widgetMoveHint.id);
        drag.setAttribute("aria-pressed", "false");
        drag.addEventListener("keydown", event => this.editor?.moveKey(event, instance.id));
        drag.addEventListener("pointerdown", event => {
            window.bootstrap?.Tooltip?.getInstance(drag)?.hide();
            this.editor?.pointerDown(event, this.views.get(instance.id));
        });
        const menuButton = button("", () => {}, "btn btn-sm btn-outline-primary md-dashboard__control");
        menuButton.append(icon("ti-adjustments-horizontal"));
        menuButton.setAttribute("aria-label", this._t("actions", "Widget actions"));
        menuButton.setAttribute("data-bs-toggle", "dropdown");
        menuButton.setAttribute("aria-expanded", "false");
        const menu = node("div", "dropdown-menu dropdown-menu-end");
        const menuItem = (key, fallback, iconName, action) => {
            const control = button(this._t(key, fallback), () => {
                window.bootstrap?.Dropdown?.getInstance(menuButton)?.hide();
                menuButton.focus({ preventScroll: true });
                action();
            }, "dropdown-item md-dashboard__control");
            control.prepend(icon(iconName));
            control.dataset.dashboardAction = key;
            return control;
        };
        menu.append(menuItem("refresh", "Refresh", "ti-refresh", () => this.refresh(instance.id)));
        if (definition.configure || definition.sizes.length > 1) menu.append(menuItem("settings", "Settings", "ti-settings", () => this.showSettings(instance.id)));
        const keyboardMove = menuItem("keyboardMove", "Move with keyboard", "ti-arrows-move", () => this.showMove(instance.id));
        keyboardMove.dataset.dashboardAction = "move";
        menu.append(keyboardMove);
        if (!definition.mandatory) {
            menu.append(node("hr", "dropdown-divider"));
            const remove = menuItem("removeFromOverview", "Remove from overview", "ti-trash", () => this.remove(instance.id));
            remove.dataset.dashboardAction = "remove";
            remove.classList.add("text-danger");
            menu.append(remove);
        }
        dropdown.append(menuButton, menu);
        const controls = node("div", "md-dashboard__widget-controls md-dashboard__edit-control");
        controls.hidden = !this._isEditing(instance);
        controls.append(dropdown);
        if (!fixed) {
            header.prepend(drag);
            header.append(controls);
            menuButton.addEventListener("show.bs.dropdown", () => window.bootstrap?.Tooltip?.getInstance(dropdown)?.hide());
            menuButton.addEventListener("keydown", event => {
                if (event.key === "Escape") window.bootstrap?.Dropdown?.getInstance(menuButton)?.hide();
            });
            drag.addEventListener("hidden.bs.tooltip", () => drag.setAttribute("aria-describedby", this.widgetMoveHint.id));
            // Bootstrap stores one plugin per element, so the menu tooltip belongs to its wrapper.
            for (const [target, control] of [[drag, drag], [dropdown, menuButton]]) {
                target.title = control.getAttribute("aria-label");
                target.setAttribute("data-dashboard-tooltip", "");
                target.setAttribute("data-bs-animation", "false");
                target.addEventListener("show.bs.tooltip", event => {
                    if (!this.editing || this.editor?.move || this.editor?.stopPointer || this._dialogs.size || control.matches(":active") || menuButton.getAttribute("aria-expanded") === "true") event.preventDefault();
                });
                if (window.jQuery && window.WJ?.initTooltip) window.WJ.initTooltip(window.jQuery(target));
            }
        }
        if (["news", "search"].includes(instance.type)) header.hidden = true;
        const body = node("div", "md-dashboard__widget-body");
        body.id = `dashboard-body-${instance.id}`;
        card.append(header, body);
        return { card, header, title, titleText, body, instance, abort: null, cleanup: null, signature: null };
    }

    _instance(id) {
        return this.settings.items.find(item => item.id === id);
    }

    /**
     * Aborts a view's render and releases renderer, dropdown and drag resources without removing its DOM.
     * @param {DashboardView} view - View being removed, replaced or reused with a new context.
     */
    _disposeView(view) {
        view.abort?.abort();
        if (view.cleanup) {
            try { dispose(view.cleanup); } catch (error) { console.warn("Dashboard widget cleanup failed", error); }
            view.cleanup = null;
        }
        window.bootstrap?.Dropdown?.getInstance(view.header.querySelector('[data-bs-toggle="dropdown"]'))?.dispose();
        for (const target of view.header.querySelectorAll('[data-dashboard-tooltip]')) {
            window.bootstrap?.Tooltip?.getInstance(target)?.dispose();
            window.jQuery?.(target).off(".wjTooltipA11y .wjFocusWithoutTooltip");
        }
        const $ = window.jQuery;
        if ($?.fn.draggable && $(view.card).data("ui-draggable")) $(view.card).draggable("destroy");
        if ($?.fn.droppable && $(view.card).data("ui-droppable")) $(view.card).droppable("destroy");
    }

    /**
     * Waits for a grid card to enter the viewport, releasing the observer on entry or abort.
     * @param {DashboardView} view - Card to observe with the controller's existing IntersectionObserver.
     * @param {AbortSignal} signal - Active render signal that also resolves the wait on abort.
     * @returns {Promise<void>} Resolves on viewport entry or abort; callers must check the signal before rendering.
     */
    _waitForVisibility(view, signal) {
        return new Promise(resolve => {
            const finish = () => {
                this._visibilityObserver.unobserve(view.card);
                view.onVisible = null;
                signal.removeEventListener("abort", finish);
                resolve();
            };
            view.onVisible = finish;
            signal.addEventListener("abort", finish, { once: true });
            this._visibilityObserver.observe(view.card);
        });
    }

    /**
     * Refreshes one widget and disposes stale results even when a request ignores abort.
     * Grid renders wait for viewport entry; fixed utilities render immediately. Failures are displayed with retry controls.
     *
     * @param {string} id - Displayed instance ID, including transient fixed-widget IDs.
     * @returns {Promise<void>} Resolves after rendering, cancellation or handled failure, or immediately if the view is absent.
     */
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
        const content = node("div", "md-dashboard__widget-content");
        view.body.replaceChildren(content);
        view.body.setAttribute("aria-busy", "true");
        const loading = node("span", "md-dashboard__loading", this._t("loading", "Loading…"));
        view.body.prepend(loading);
        try {
            if (this._visibilityObserver && this._region(instance) === "grid") {
                await this._waitForVisibility(view, abort.signal);
                if (abort.signal.aborted || this.destroyed) return;
            }
            const result = await definition.render({
                container: content, instance: cloneSettings(instance), options: cloneSettings(instance.options || {}),
                domainOptions: cloneSettings(this.settings.domainOptions[id] || definition.defaultDomainOptions),
                context: this._widgetContext(), signal: abort.signal,
                refresh: () => this.refresh(id), saveOptions: values => this.saveOptions(id, values)
            });
            if (abort.signal.aborted || this.destroyed) dispose(result);
            else view.cleanup = result;
        } catch (error) {
            console.error("Dashboard widget render failed", error);
            if (abort.signal.aborted || this.destroyed) return;
            const reasonKey = { "domain-unavailable": "domainUnavailable", "selection-unavailable": "selectionUnavailable", "permission-denied": "permissionDenied" }[error.dashboardReason] || "widgetError";
            content.replaceChildren(node("p", "text-danger", this._t(reasonKey, "This widget could not be loaded.")), button(this._t("retry", "Try again"), () => this.refresh(id)));
        } finally {
            if (!abort.signal.aborted && !this.destroyed) {
                loading.remove();
                view.body.setAttribute("aria-busy", "false");
                if (instance.type === "shortcut") this._updateShortcutLink(view);
            }
        }
    }

    /**
     * Adds an instance with registered defaults to the widget draft or saved shortcut list, then focuses it.
     * @param {string} type - Registered widget type to add.
     * @returns {Promise<boolean>} Whether the instance was saved; false for unavailable types, duplicate singletons or failed saves.
     */
    async add(type) {
        const definition = getWidget(type);
        if (!definition || !this._available(definition)) return false;
        if (!definition.multiple && this.settings.items.some(item => item.type === type)) return false;
        const next = cloneSettings(this.settings);
        const instance = this._newInstance(definition);
        next.items.push(instance);
        next.domainOptions[instance.id] = cloneSettings(definition.defaultDomainOptions);
        const saved = await this._commit(next, this.status, { draft: type !== "shortcut" });
        if (saved) this._focusInstance(instance.id);
        return saved;
    }

    /**
     * Stages grid options during editing or saves independent preferences for the active domain.
     * @param {string} id - Persisted instance to update.
     * @param {import('./registry').WidgetOptionsUpdate} [values={}] - Replacement maps; omitted maps retain their previous values.
     * @returns {Promise<boolean>} Whether persistence succeeded, or false when the instance does not exist.
     */
    async saveOptions(id, { options, domainOptions } = {}) {
        const next = cloneSettings(this.settings);
        const item = next.items.find(instance => instance.id === id);
        if (!item) return false;
        if (options !== undefined) item.options = cloneSettings(options);
        if (domainOptions !== undefined) next.domainOptions[id] = cloneSettings(domainOptions);
        return this._commit(next, this.status, { draft: this._region(item) === "grid" });
    }

    /**
     * Removes an optional widget from the draft or saves shortcut removal, retaining options for undo.
     * @param {string} id - Instance to remove; mandatory widgets and fixed utilities cannot be removed.
     * @returns {Promise<boolean>} Whether removal was applied and an undo action made available.
     */
    async remove(id) {
        const instance = this._instance(id);
        if (!instance || getWidget(instance.type)?.mandatory || ["sessions", "news", "search"].includes(this._region(instance))) return false;
        const previous = { instance: cloneSettings(instance), index: this.settings.items.indexOf(instance), domainOptions: cloneSettings(this.settings.domainOptions[id] || {}) };
        const next = cloneSettings(this.settings);
        next.items = next.items.filter(item => item.id !== id);
        delete next.domainOptions[id];
        const shortcut = instance.type === "shortcut";
        const following = shortcut ? this.shortcutList.querySelector(`[data-instance-id="${id}"]`)?.nextElementSibling?.dataset.instanceId : null;
        if (!await this._commit(next, shortcut ? this.shortcutStatus : this.status, { notify: !shortcut && !this.editor, animateShortcuts: shortcut, draft: !shortcut })) return false;
        if (shortcut) {
            this._showShortcutUndo(previous, "remove");
            if (following) this._focusInstance(following);
            else this.addShortcutButton.focus({ preventScroll: true });
            return true;
        }
        if (this.editor) {
            this.editor.showRemoval(id);
            const nextView = [...this.views.values()].find(view => this._region(view.instance) === "grid");
            if (nextView) this._focusInstance(nextView.instance.id);
            else this.addButton.focus({ preventScroll: true });
            return true;
        }
        this.removed = previous;
        this.layout.before(this.undoContainer);
        this.undoContainer.hidden = false;
        this.undoContainer.replaceChildren(node("span", "", this._t("removed", "Widget removed.")), button(this._t("undo", "Undo"), () => this.undoRemove(), "btn btn-sm btn-outline-secondary md-dashboard__control"));
        this.undoContainer.querySelector("button").focus();
        return true;
    }

    /**
     * Restores the most recently removed instance and its active-domain options at its previous position.
     * @returns {Promise<boolean>} Whether restoration was saved; false when no undo exists, it conflicts with current instances or saving fails.
     */
    async undoRemove() {
        if (this.editor) return this.editor.undo();
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

    /**
     * Stages a grid order or persists a shortcut order and focuses the moved instance.
     * @param {string} id - Grid widget or shortcut to move.
     * @param {string|null} [beforeId=null] - Instance in the same region, or null to move to the end.
     * @returns {Promise<boolean>} Whether the resulting order was saved; false for unsupported moves or persistence failure.
     */
    async moveBefore(id, beforeId = null) {
        const instance = this._instance(id);
        if (!instance || !["grid", "shortcut"].includes(this._region(instance))) return false;
        if (beforeId && this._region(this._instance(beforeId) || {}) !== this._region(instance)) return false;
        const next = cloneSettings(this.settings);
        next.items = moveInstanceBefore(next.items, id, beforeId);
        const saved = await this._commit(next, this._region(instance) === "shortcut" ? this.shortcutStatus : this.status, { animateShortcuts: instance.type === "shortcut", draft: instance.type !== "shortcut" });
        if (saved) this._focusInstance(id);
        return saved;
    }

    _focusInstance(id) {
        const view = this.views.get(id);
        (view && this._isEditing(view.instance) ? view.card.querySelector(".md-dashboard__drag") : view?.card)?.focus({ preventScroll: true });
    }

    /**
     * Persists the release version whose announcement should be collapsed.
     * @param {string|null} version - Release version to acknowledge, or null to expand the announcement.
     * @returns {Promise<boolean>} Whether the acknowledgement was saved and applied.
     */
    async acknowledgeNews(version) {
        const next = cloneSettings(this.settings);
        next.acknowledgedNewsVersion = version;
        return this._commit(next, this.status, { draft: false });
    }

    /** Makes shortcut activation open settings instead of navigating while its section is being edited. */
    _updateShortcutLink(view) {
        for (const target of view.body.querySelectorAll("a")) {
            if (this.editingShortcuts) {
                if (target.hasAttribute("href")) target.dataset.shortcutHref = target.getAttribute("href");
                target.removeAttribute("href");
                target.setAttribute("role", "button");
                target.removeAttribute("aria-disabled");
                target.tabIndex = 0;
            } else {
                if (target.dataset.shortcutHref) target.setAttribute("href", target.dataset.shortcutHref);
                target.removeAttribute("role");
                target.removeAttribute("tabindex");
                if (target.classList.contains("is-unavailable")) {
                    target.setAttribute("aria-disabled", "true");
                    target.tabIndex = 0;
                }
            }
        }
    }

    /** Shows the last shortcut change as an eight-second, keyboard-accessible undo notification. */
    _showShortcutUndo(previous, action) {
        this._clearShortcutUndo();
        const container = node("div", "toast-container md-dashboard__shortcut-toast");
        container.id = `dashboard-shortcut-undo-${createInstanceId()}`;
        document.body.append(container);
        window.WJ.notify("info", this._t(action === "remove" ? "shortcutRemoved" : "shortcutUpdated", action === "remove" ? "Shortcut removed." : "Shortcut updated."), "", 8000, null, false, container.id);
        const undo = button(this._t("shortcutUndo", "Undo"), () => this._undoShortcutChange(), "btn btn-sm btn-outline-light");
        undo.dataset.dashboardShortcutUndo = "true";
        container.querySelector(".toast").append(undo);
        container.addEventListener("click", event => {
            if (event.target.closest(".toast-close-button")) this._clearShortcutUndo();
        });
        this._shortcutUndo = { previous, action, container, undo, timer: window.setTimeout(() => this._clearShortcutUndo(), 8000) };
    }

    _clearShortcutUndo() {
        if (!this._shortcutUndo) return;
        const { container, timer } = this._shortcutUndo;
        window.clearTimeout(timer);
        if (container.contains(document.activeElement)) this.addShortcutButton.focus({ preventScroll: true });
        container.remove();
        this._shortcutUndo = null;
    }

    /** Restores only the changed shortcut, preserving the current widgets and other shortcut settings. */
    async _undoShortcutChange() {
        if (!this._shortcutUndo || this.saving) return false;
        const { previous, action, undo, timer } = this._shortcutUndo;
        const next = cloneSettings(this.settings);
        const index = next.items.findIndex(item => item.id === previous.instance.id);
        if (action === "remove" && index === -1) next.items.splice(Math.min(previous.index, next.items.length), 0, cloneSettings(previous.instance));
        else if (action === "edit" && index >= 0) next.items[index] = cloneSettings(previous.instance);
        else return false;
        next.domainOptions[previous.instance.id] = cloneSettings(previous.domainOptions);
        window.clearTimeout(timer);
        undo.disabled = true;
        if (!await this._commit(next, this.shortcutStatus, { animateShortcuts: true, draft: false })) {
            if (!this.destroyed) this._showShortcutUndo(previous, action);
            return false;
        }
        this._focusInstance(previous.instance.id);
        return true;
    }

    /** Clones a compact shortcut outside the transformed dashboard, retaining its theme and dimensions. */
    _shortcutDragHelper(view) {
        const bounds = view.card.getBoundingClientRect();
        const style = window.getComputedStyle(view.card);
        const helper = view.card.cloneNode(true);
        helper.classList.add("md-dashboard__shortcut-drag-helper");
        helper.removeAttribute("data-instance-id");
        helper.setAttribute("aria-hidden", "true");
        helper.setAttribute("inert", "");
        helper.querySelectorAll("[id]").forEach(element => element.removeAttribute("id"));
        Object.assign(helper.style, { width: `${bounds.width}px`, height: `${bounds.height}px`, backgroundColor: style.backgroundColor, borderColor: style.borderColor, color: style.color, fontSize: style.fontSize });
        for (const property of Array.from(style)) {
            if (property.startsWith("--wj-dashboard-")) helper.style.setProperty(property, style.getPropertyValue(property));
        }
        return helper;
    }

    /** Starts a provisional move; the persisted order changes only after a successful drop. */
    _beginShortcutMove(id, keyboard = false) {
        if (this.saving || !this.editingShortcuts) return false;
        this._finishShortcutMove(false);
        const view = this.views.get(id);
        const cards = [...this.shortcutList.querySelectorAll('[data-instance-id]')];
        const following = cards[cards.indexOf(view.card) + 1]?.dataset.instanceId || null;
        const marker = node("span", "md-dashboard__shortcut-drop-marker");
        marker.setAttribute("aria-hidden", "true");
        const helper = keyboard ? this._shortcutDragHelper(view) : null;
        if (helper) {
            helper.style.position = "fixed";
            document.body.append(helper);
        }
        this._shortcutMove = { id, beforeId: following, originalBeforeId: following, marker, helper };
        this.shortcutList.append(marker);
        view.card.classList.add("is-shortcut-placeholder");
        view.card.querySelector(".md-dashboard__drag").setAttribute("aria-pressed", "true");
        this._showShortcutDrop(following);
        return true;
    }

    _showShortcutDrop(beforeId) {
        const move = this._shortcutMove;
        if (!move) return;
        move.beforeId = beforeId;
        const target = beforeId ? this.views.get(beforeId)?.card : this.shortcutActions;
        if (!target) return;
        const bounds = target.getBoundingClientRect();
        const region = this.shortcutList.getBoundingClientRect();
        Object.assign(move.marker.style, { left: `${bounds.left - region.left - 5}px`, top: `${bounds.top - region.top}px`, height: `${bounds.height}px` });
        move.marker.hidden = false;
        if (move.helper) {
            Object.assign(move.helper.style, { left: `${bounds.left}px`, top: `${bounds.top - 8}px` });
            const others = [...this.shortcutList.querySelectorAll('[data-instance-id]')].filter(card => card.dataset.instanceId !== move.id);
            const position = beforeId ? others.findIndex(card => card.dataset.instanceId === beforeId) + 1 : others.length + 1;
            this.shortcutStatus.textContent = this._t("shortcutMovePosition", "Position {1} of {2}. Enter drops, Escape cancels.", position, others.length + 1).replace("{1}", position).replace("{2}", others.length + 1);
        }
    }

    /** Tracks horizontal insertion positions across wrapped shortcut rows. */
    _shortcutPointerMove(event) {
        const move = this._shortcutMove;
        if (!move) return;
        const region = this.shortcutList.getBoundingClientRect();
        const x = event.clientX, y = event.clientY;
        if (x < region.left || x > region.right || y < region.top || y > region.bottom) { move.marker.hidden = true; return; }
        const target = [...this.shortcutList.querySelectorAll('[data-instance-id]')].find(card => {
            if (card.dataset.instanceId === move.id) return false;
            const bounds = card.getBoundingClientRect();
            return y < bounds.top || y <= bounds.bottom && x < bounds.left + bounds.width / 2;
        });
        this._showShortcutDrop(target?.dataset.instanceId || null);
    }

    /** Implements lift, provisional arrow movement, drop and cancellation on the shortcut grip. */
    _shortcutMoveKey(event, id) {
        if (event.repeat) { event.preventDefault(); return; }
        if (![" ", "Enter", "ArrowLeft", "ArrowRight", "Escape", "Tab"].includes(event.key)) return;
        if (event.key === "Tab") { this._finishShortcutMove(false); return; }
        if (!this._shortcutMove && ![" ", "Enter"].includes(event.key)) return;
        event.preventDefault();
        if (!this._shortcutMove) { this._beginShortcutMove(id, true); return; }
        if (event.key === "Escape") { this._finishShortcutMove(false); return; }
        if (event.key === "Enter" || event.key === " ") { this._finishShortcutMove(true); return; }
        const others = [...this.shortcutList.querySelectorAll('[data-instance-id]')].map(card => card.dataset.instanceId).filter(value => value !== id);
        const index = this._shortcutMove.beforeId ? others.indexOf(this._shortcutMove.beforeId) : others.length;
        const next = Math.max(0, Math.min(others.length, index + (event.key === "ArrowLeft" ? -1 : 1)));
        this._showShortcutDrop(others[next] || null);
    }

    _finishShortcutMove(save) {
        const move = this._shortcutMove;
        if (!move) return;
        this._shortcutMove = null;
        move.marker.remove();
        move.helper?.remove();
        const card = this.views.get(move.id)?.card;
        card?.classList.remove("is-shortcut-placeholder");
        card?.querySelector(".md-dashboard__drag").setAttribute("aria-pressed", "false");
        this.shortcutStatus.textContent = "";
        if (save && move.beforeId !== move.originalBeforeId) return this.moveBefore(move.id, move.beforeId);
    }

    /**
     * Reads legacy URL bookmarks, retaining the exact source until persistence succeeds.
     * Safe destinations are deduplicated; invalid entries are counted so import can reject the entire source.
     * @returns {{items: {source: string, href: string, title: string}[], invalid: number, raw: string|null}} Parsed shortcuts, invalid-entry count and unchanged storage text; missing or inaccessible storage produces an empty result.
     */
    _legacyBookmarks() {
        const result = { items: [], invalid: 0, raw: null };
        try { result.raw = window.localStorage.getItem("bookmarks"); }
        catch (error) { return result; }
        if (!result.raw) return result;
        try {
            const bookmarks = JSON.parse(result.raw);
            if (!Array.isArray(bookmarks)) return { ...result, invalid: 1 };
            const seen = new Set();
            for (const bookmark of bookmarks) {
                const href = shortcutUrl(bookmark?.path);
                const title = typeof bookmark?.name === "string" ? bookmark.name.trim() : "";
                if (!href || !title || title.length > 120) { result.invalid++; continue; }
                const target = localUrl(href) || href;
                if (seen.has(target)) continue;
                seen.add(target);
                result.items.push({ source: "url", href: target, title });
            }
        } catch (error) { result.invalid = 1; }
        return result;
    }

    /**
     * Replaces shortcuts once on load, removing the legacy source only after a successful save.
     * The raw storage value is removed only if it has not changed during persistence.
     * @returns {Promise<boolean>} True for successful import or when none is needed; false for invalid data or a failed save.
     */
    async importLegacyBookmarks() {
        if (this.settings.legacyBookmarksHandled) return true;
        const legacy = this._legacyBookmarks();
        if (legacy.invalid) {
            this.shortcutStatus.replaceChildren(node("span", "text-danger", this._t("legacyBookmarksInvalid", "The original bookmarks contain an invalid name or URL and could not be imported. The original browser data and current shortcuts were kept.")));
            return false;
        }
        const definition = getWidget("shortcut");
        if (!legacy.items.length || !definition) return true;
        const next = cloneSettings(this.settings);
        for (const item of next.items.filter(item => item.type === "shortcut")) delete next.domainOptions[item.id];
        next.items = [...next.items.filter(item => item.type !== "shortcut"), ...legacy.items.map(options => this._newInstance(definition, { options }))];
        next.legacyBookmarksHandled = true;
        const saved = await this._commit(next, this.shortcutStatus);
        if (saved) {
            try {
                if (window.localStorage.getItem("bookmarks") === legacy.raw) window.localStorage.removeItem("bookmarks");
            } catch (error) { /* The server marker prevents a repeated import when browser storage is unavailable. */ }
        } else if (!this.destroyed) {
            this.shortcutStatus.append(button(this._t("retry", "Try again"), () => this.importLegacyBookmarks()));
        }
        return saved;
    }

    /**
     * Stages defaults or every available size variant during editing, preserving shortcuts.
     * Final Save clears old domain filters and news acknowledgement atomically.
     * Outside editing, reset uses the existing immediate reset endpoints.
     *
     * @param {boolean} [allSizes=false] - Uses all available size variants instead of normal defaults.
     * @returns {Promise<boolean>} Whether the reset was staged or saved; false for busy, destroyed, oversized, aborted or failed operations.
     */
    async reset(allSizes = false) {
        if (this.saving || this.destroyed) return false;
        let variants;
        if (allSizes) {
            variants = normalizeSettings({ items: this.settings.items.filter(item => item.type === "shortcut") });
            for (const definition of listWidgets()) {
                if (definition.type === "shortcut" || !this._available(definition)) continue;
                for (const size of definition.sizes) {
                    const item = this._newInstance(definition, { size });
                    variants.items.push(item);
                    variants.domainOptions[item.id] = cloneSettings(definition.defaultDomainOptions);
                }
            }
            if (variants.items.length > MAX_WIDGETS) {
                this.status.textContent = this._t("limit", "The overview can contain at most 48 widgets.");
                return false;
            }
        }
        if (this.editor) {
            if (!variants) {
                const previous = this.settings;
                this.settings = normalizeSettings({ ...previous, configured: false, shortcutsConfigured: true,
                    items: previous.items.filter(item => item.type === "shortcut"),
                    domainOptions: Object.fromEntries(previous.items.filter(item => item.type === "shortcut").map(item => [item.id, cloneSettings(previous.domainOptions[item.id] || {})])),
                    acknowledgedNewsVersion: null });
                this._addDefaults();
                this._ensureMandatory();
                variants = this.settings;
                this.settings = previous;
            }
            return this.editor.stage(variants, true);
        }
        this.saving = true;
        this._setBusy(true);
        this.status.textContent = this._t("saving", "Saving…");
        const request = this._request = new AbortController();
        try {
            const response = await fetch(allSizes ? "/admin/rest/dashboard/settings/reset" : "/admin/rest/dashboard/settings", {
                method: allSizes ? "PUT" : "DELETE", credentials: "same-origin", signal: request.signal,
                headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken || "" },
                body: allSizes ? JSON.stringify(variants) : undefined
            });
            if (!response.ok) throw new Error(`Dashboard reset: ${response.status}`);
            const data = await response.json();
            if (this.destroyed || request.signal.aborted) return false;
            this.context.data.settings = data;
            this.settings = normalizeSettings(data);
            if (!this.settings.configured) this._addDefaults();
            this._ensureMandatory();
            this._clearShortcutUndo();
            this.removed = null;
            this.undoContainer.hidden = true;
            this.status.textContent = "";
            this._render();
            window.WJ.notifySuccess(allSizes ? this._t("resetAllDone", "All available widget sizes have been added.") : this._t("resetDone", "The default overview has been restored."), "", 10000);
            return true;
        } catch (error) {
            if (!this.destroyed && !request.signal.aborted) this._showFailure("saveError", "The change could not be saved. Your previous settings were kept.");
            return false;
        } finally {
            this.saving = false;
            if (!this.destroyed) this._setBusy(false);
        }
    }

    /**
     * Opens a modal with focus restoration, an abort signal and cleanup of controls and owned resources.
     * @param {string} title - Visible and accessible dialog title.
     * @param {HTMLElement|null} [trigger=document.activeElement] - Element to refocus after closing if still connected.
     * @returns {DashboardDialog} Attached dialog containers and lifecycle controls.
     */
    _dialog(title, trigger = document.activeElement) {
        this._finishShortcutMove(false);
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
            if (window.jQuery?.fn?.selectpicker) window.jQuery(root).find('select').each(function () {
                const select = window.jQuery(this);
                if (select.data('selectpicker')) select.selectpicker('destroy');
            });
            modal?.dispose();
            root.remove();
            this._dialogs.delete(api);
            if (trigger?.isConnected) {
                if (window.WJ?.focusWithoutTooltip) window.WJ.focusWithoutTooltip(trigger);
                else trigger.focus({ preventScroll: true });
            }
        };
        const close = () => {
            if (!modal) finish();
            else if (shown) modal.hide();
            else closeRequested = true;
        };
        const closeButton = button("", close, "btn btn-outline-secondary btn-close");
        closeButton.setAttribute("aria-label", this._t("close", "Close"));
        closeButton.append(icon("ti-x"));
        header.append(heading, closeButton);
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
            if (event.key === "Escape") {
                event.preventDefault();
                const pickerButton = root.querySelector('.bootstrap-select > button.dropdown-toggle[aria-expanded="true"]');
                if (pickerButton) {
                    event.stopImmediatePropagation();
                    window.jQuery(pickerButton.parentElement.querySelector('select')).selectpicker('toggle');
                    pickerButton.focus();
                } else close();
            }
        });
        document.body.append(root);
        if (window.bootstrap?.Modal) modal = new window.bootstrap.Modal(root);
        const api = { root, body, footer, signal: lifecycle.signal, close, destroy: finish, setCleanup: value => { cleanup = value; } };
        this._dialogs.add(api);
        if (modal) modal.show();
        else { root.classList.add("show"); root.style.display = "block"; }
        return api;
    }

    /**
     * Opens an accessible widget dialog with the dashboard's cleanup lifecycle.
     * @param {string} title - Visible and accessible dialog title.
     * @returns {DashboardDialog} Containers, abort signal and controls for closing and registering cleanup.
     */
    showDialog(title) {
        return this._dialog(title);
    }

    /**
     * Confirms the full reset scope before invoking the shared atomic preference reset.
     * @param {boolean} [allSizes=false] - Whether acceptance should install every available size variant.
     */
    showReset(allSizes = false) {
        window.jQuery?.(this.resetButton).off(".wjFocusWithoutTooltip");
        const tooltip = window.bootstrap?.Tooltip?.getInstance(this.resetButton);
        tooltip?.disable();
        tooltip?.hide();
        this.resetButton.focus({ preventScroll: true });
        window.WJ.confirm({
            title: allSizes ? this._t("resetAllConfirm", "Show all") : this._t("resetConfirm", "Restore defaults"),
            message: (allSizes ? this._t("resetAllDescription", "Replace the overview with all available widgets in every supported size? You can then remove the variants you do not want. Widget filters in all domains and read news will also be reset. Your shortcuts and other account settings will be kept.") : this._t("resetDescription", "Restore the default widgets, sizes and order? Widget filters in all domains and read news will also be reset. Your shortcuts and other account settings will be kept.")) + (this.editor ? ` ${this._t("draftResetHint", "Changes will be saved only when you select Save in the edit toolbar.")}` : ""),
            btnOkText: allSizes ? this._t("resetAllConfirm", "Show all") : this._t("resetConfirm", "Restore defaults"),
            success: () => this.reset(allSizes),
            onHidden: () => window.bootstrap?.Tooltip?.getInstance(this.resetButton)?.enable()
        });
    }

    /** Opens the permission-filtered grid catalogue, retaining filters and focus while widgets are added. */
    showCatalogue() {
        const dialog = this._dialog(this._t("add", "Add widget"));
        dialog.root.classList.add("md-dashboard-modal--catalogue");
        dialog.root.querySelector(".modal-dialog").classList.add("modal-lg", "modal-dialog-centered", "modal-dialog-scrollable");
        const tools = node("div", "md-dashboard__catalogue-tools");
        const searchField = node("div", "md-dashboard__catalogue-search");
        const search = node("input", "form-control");
        search.type = "search";
        search.setAttribute("aria-label", this._t("searchWidgets", "Search widgets"));
        search.placeholder = this._t("searchWidgets", "Search widgets");
        searchField.append(icon("ti-search"), search);
        const categories = { all: this._t("catalogueAll", "All"), content: this._t("catalogueContent", "Content"), traffic: this._t("catalogueTraffic", "Traffic"), system: this._t("catalogueSystem", "System") };
        const definitions = listWidgets().filter(definition => this._available(definition) && !["sessions", "news", "search", "shortcut"].includes(definition.type));
        const filters = node("div", "md-dashboard__catalogue-filters");
        filters.setAttribute("role", "group");
        filters.setAttribute("aria-label", this._t("catalogueCategories", "Widget categories"));
        let category = "all";
        const list = node("div", "md-dashboard__catalogue");
        const empty = node("p", "md-dashboard__catalogue-empty mb-0", this._t("noWidgets", "No matching widgets are available."));
        const error = node("p", "text-danger mb-0");
        error.setAttribute("role", "alert");
        const announcement = node("p", "visually-hidden");
        announcement.setAttribute("role", "status");
        const added = new Set();
        const addedTimers = new Map();
        dialog.setCleanup(() => { for (const timer of addedTimers.values()) window.clearTimeout(timer); });
        const cards = [];
        const filter = () => {
            const query = search.value.trim().toLocaleLowerCase();
            for (const card of cards) card.item.hidden = (category !== "all" && card.category !== category) || !card.searchText.includes(query);
            empty.hidden = cards.some(card => !card.item.hidden);
            for (const control of filters.children) control.setAttribute("aria-pressed", String(control.dataset.category === category));
        };
        const updateStates = () => {
            const full = this.settings.items.length >= MAX_WIDGETS;
            if (full) error.textContent = this._t("limit", "The overview can contain at most 48 widgets.");
            for (const card of cards) {
                const exists = this.settings.items.some(item => item.type === card.definition.type);
                card.badge.hidden = !exists;
                card.item.classList.toggle("is-added", added.has(card.definition.type));
                const recentlyAdded = addedTimers.has(card.definition.type);
                const label = recentlyAdded ? this._t("catalogueAdded", "Added") : exists ? this._t("catalogueAddAnother", "Add another") : this._t("catalogueAdd", "Add");
                card.add.replaceChildren(icon(recentlyAdded ? "ti-check" : "ti-plus"), document.createTextNode(label));
                card.add.setAttribute("aria-label", `${card.add.textContent} · ${card.title}`);
                card.add.disabled = full;
            }
        };
        for (const [value, label] of Object.entries(categories)) {
            const count = value === "all" ? definitions.length : definitions.filter(definition => (definition.category || "system") === value).length;
            const control = button("", () => { category = value; filter(); }, "btn btn-sm md-dashboard__catalogue-filter");
            control.dataset.category = value;
            control.append(document.createTextNode(label), node("span", "md-dashboard__catalogue-count", count));
            filters.append(control);
        }
        for (const definition of definitions) {
            const title = this._t(definition.titleKey);
            const description = definition.descriptionKey ? this._t(definition.descriptionKey) : "";
            const widgetCategory = definition.category || "system";
            const item = node("div", "md-dashboard__catalogue-item");
            item.dataset.widgetType = definition.type;
            item.dataset.category = widgetCategory;
            const emblem = node("span", "md-dashboard__catalogue-icon");
            emblem.append(icon(definition.icon || "ti-layout-grid"));
            const text = node("div", "md-dashboard__catalogue-text");
            const heading = node("div", "md-dashboard__catalogue-heading");
            const badge = node("span", "md-dashboard__catalogue-badge", this._t("catalogueOnOverview", "On overview"));
            heading.append(node("strong", "", title), badge);
            const size = definition.defaultSize === "fullauto" ? this._t("fullWidth", "Full width") : definition.defaultSize.replace("x", "×");
            text.append(heading, node("p", "md-dashboard__catalogue-description", description), node("p", "md-dashboard__catalogue-meta", `${categories[widgetCategory]} · ${this._t("catalogueDefaultSize", "Default size")} ${size}`));
            const actions = node("div", "md-dashboard__catalogue-actions");
            const add = button("", async () => {
                add.disabled = true;
                error.textContent = "";
                try {
                    if (await this.add(definition.type)) {
                        if (dialog.signal.aborted) return;
                        added.add(definition.type);
                        window.clearTimeout(addedTimers.get(definition.type));
                        addedTimers.set(definition.type, window.setTimeout(() => {
                            addedTimers.delete(definition.type);
                            updateStates();
                        }, 5000));
                        announcement.textContent = `${title} · ${this._t("catalogueAdded", "Added")}`;
                    } else error.textContent = this._t("saveError", "The change could not be saved.");
                } catch (failure) { error.textContent = this._t("saveError", "The change could not be saved."); }
                if (dialog.signal.aborted) return;
                updateStates();
                (add.disabled ? dialog.footer.querySelector("button") : add).focus({ preventScroll: true });
            });
            actions.append(add);
            item.append(emblem, text, actions);
            list.append(item);
            cards.push({ definition, item, badge, add, title, category: widgetCategory, searchText: `${title} ${description}`.toLocaleLowerCase() });
        }
        tools.append(searchField, filters);
        dialog.body.before(tools);
        dialog.body.append(list, empty, announcement);
        containNativeScroll(dialog.body, dialog.signal);
        dialog.footer.append(error, node("p", "md-dashboard__catalogue-hint", this._t("catalogueHint", "Added widgets appear at the end of the overview. They are saved with your other changes.")), button(this._t("catalogueDone", "Done"), dialog.close, "btn btn-primary"));
        search.addEventListener("input", filter);
        updateStates();
        filter();
        dialog.root.addEventListener("shown.bs.modal", () => search.focus(), { once: true });
    }

    /**
     * Lifts an editing grid widget for keyboard movement or opens the shortcut position selector.
     * @param {string} id - Instance to move; unknown IDs are ignored.
     */
    showMove(id) {
        const instance = this._instance(id);
        if (!instance) return;
        if (this.editor && this._region(instance) === "grid") { this.editor.beginMove(id); return; }
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

    /**
     * Configures a new instance before its first atomic save. Cancelling adds nothing.
     * @param {string} type - Available registered widget type that does not violate capacity or singleton rules.
     * @returns {Promise<void>} Resolves once dialog configuration finishes, not when the user saves or cancels.
     */
    async showAddWidget(type) {
        if (type === "shortcut" && this.editor) { this.cancelEditing(() => this.showAddWidget(type)); return; }
        const definition = getWidget(type);
        if (!definition || !this._available(definition) || this.settings.items.length >= MAX_WIDGETS) {
            this.status.textContent = this._t("limit", "The overview can contain at most 48 widgets.");
            return;
        }
        if (!definition.multiple && this.settings.items.some(item => item.type === type)) return;
        return this._showSettings(this._newInstance(definition), true);
    }

    /**
     * Opens existing size and widget settings, staging grid changes when Apply is selected.
     * Handles asynchronous configuration failure and disposes configuration that completes after the dialog closes.
     *
     * @param {import('./model').WidgetInstance} instance - Existing instance or new unsaved instance.
     * @param {boolean} [adding=false] - Whether the first successful save must insert the instance.
     * @returns {Promise<void>} Resolves after initializing configuration controls; user interaction continues independently.
     */
    async _showSettings(instance, adding = false) {
        const id = instance.id;
        const definition = getWidget(instance.type);
        const shortcut = instance.type === "shortcut";
        const shortcutLink = shortcut ? this.views.get(id)?.body.querySelector('a') : null;
        const shortcutTooltip = shortcutLink && window.bootstrap?.Tooltip?.getInstance(shortcutLink);
        shortcutTooltip?.disable();
        shortcutTooltip?.hide();
        const editingWidget = !adding && this.editing && this._region(instance) === "grid";
        const dialog = this._dialog(this._t(adding ? "addShortcut" : shortcut ? "editShortcut" : "settings", adding ? "Add shortcut" : shortcut ? "Edit shortcut" : "Widget settings"));
        if (shortcut) dialog.root.classList.add("md-dashboard-modal--shortcut");
        if (editingWidget) {
            dialog.root.classList.add("md-dashboard-modal--settings");
            dialog.root.querySelector(".modal-dialog").classList.add("modal-dialog-centered");
        }
        if (shortcut) dialog.root.addEventListener("hidden.bs.modal", () => {
            if (shortcutLink) window.bootstrap?.Tooltip?.getInstance(shortcutLink)?.enable();
            if (this.views.has(id)) this.views.get(id).body.querySelector("a")?.focus({ preventScroll: true });
            else this.addShortcutButton.focus({ preventScroll: true });
        }, { once: true });
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
        const save = button(this._t(shortcut ? adding ? "addShortcut" : "saveShortcutChanges" : this.editing ? "apply" : "save", shortcut ? adding ? "Add shortcut" : "Save changes" : this.editing ? "Apply" : "Save"), async () => {
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
                const previous = { instance: cloneSettings(updated), index: next.items.indexOf(updated), domainOptions: cloneSettings(next.domainOptions[id] || {}) };
                updated.size = size.value;
                if (values.options !== undefined) updated.options = cloneSettings(values.options);
                if (values.domainOptions !== undefined) next.domainOptions[id] = cloneSettings(values.domainOptions);
                save.disabled = true;
                if (await this._commit(next, shortcut ? this.shortcutStatus : this.status, { notify: !shortcut || adding, draft: !shortcut })) {
                    if (shortcut && !adding) this._showShortcutUndo(previous, "edit");
                    dialog.close();
                }
                else { error.textContent = this._t("saveError", "The change could not be saved."); save.disabled = false; }
            } catch (failure) {
                error.textContent = failure.message || this._t("invalidSettings", "Check the widget settings.");
            }
        }, "btn btn-primary");
        if (shortcut && adding) save.prepend(icon("ti-plus"));
        if (shortcut || editingWidget) {
            if (shortcut && !adding) {
                const remove = button(this._t("removeShortcut", "Remove shortcut"), async () => {
                    remove.disabled = save.disabled = true;
                    if (await this.remove(id)) dialog.close();
                    else { remove.disabled = save.disabled = false; error.textContent = this._t("saveError", "The change could not be saved."); }
                }, "btn btn-link text-danger me-auto");
                remove.dataset.dashboardAction = "removeShortcut";
                remove.prepend(icon("ti-trash"));
                dialog.footer.append(remove);
            }
            dialog.footer.append(button(this._t("button.cancel", "Cancel"), () => dialog.close(), "btn btn-outline-secondary"));
        }
        dialog.footer.append(save);
        if (editingWidget) dialog.footer.prepend(node("p", "md-dashboard__settings-hint", this._t("draftSettingsHint", "Apply changes here, then save the overview in the edit toolbar.")));
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
        if (!dialog.signal.aborted) window.WJ.initSelectPicker?.(dialog.body);
        if (editingWidget && !dialog.signal.aborted) {
            dialog.setCleanup(configuration);
            (dialog.body.querySelector(".bootstrap-select > button, select, input, button") || save).focus({ preventScroll: true });
        }
    }

    /** Keeps existing shortcut pointer movement independent of the widget edit session. */
    _bindDrag() {
        const $ = window.jQuery;
        if (!$?.fn.draggable || !$.fn.droppable) return;
        if (!$(this.shortcutList).data("ui-droppable")) $(this.shortcutList).droppable({
            accept: dragged => this.editingShortcuts && this._instance(dragged[0].dataset.instanceId)?.type === "shortcut",
            tolerance: "pointer",
            drop: () => this._finishShortcutMove(true)
        });
        for (const view of this.views.values()) {
            if (view.instance.type !== "shortcut") continue;
            if ($(view.card).data("ui-draggable")) $(view.card).draggable("option", "disabled", !this.editingShortcuts);
            if (!this.editingShortcuts || $(view.card).data("ui-draggable")) continue;
            $(view.card).draggable({
                handle: ".md-dashboard__drag", appendTo: "body", zIndex: 1100, distance: 8,
                cursorAt: { left: 0, top: -8 }, cancel: "input, textarea, select, option",
                helper: () => $(this._shortcutDragHelper(view)),
                start: () => {
                    if (this.saving || !this.editingShortcuts) return false;
                    this._beginShortcutMove(view.instance.id);
                },
                drag: event => this._shortcutPointerMove(event),
                stop: () => this._finishShortcutMove(false),
                revert: "invalid"
            });
        }
    }

    /**
     * Applies supplied active-domain options without replacing the overview's security UI.
     * Disposes active renders and dialogs, clears removal undo and restarts from the new bootstrap data.
     *
     * @param {DashboardContext} context - Complete replacement page context, including active-domain settings.
     * @returns {Promise<void>} Resolves after restarting and attempting legacy bookmark import.
     */
    async setContext(context) {
        this.setEditing(false);
        this._finishShortcutMove(false);
        this._clearShortcutUndo();
        this.context = context;
        this._contextVersion++;
        this.layout.hidden = true;
        for (const view of this.views.values()) this._disposeView(view);
        for (const dialog of this._dialogs) dialog.destroy();
        this.removed = null;
        this.undoContainer.hidden = true;
        await this.start();
    }

    /** Stops persistence and rendering work, closes dialogs and releases observers and UI resources without removing the host content. */
    destroy() {
        this.setEditing(false);
        this._finishShortcutMove(false);
        this._clearShortcutUndo();
        this.destroyed = true;
        for (const control of [this.resetButton, this.editShortcutsButton]) {
            window.bootstrap?.Tooltip?.getInstance(control)?.dispose();
            window.jQuery?.(control).off(".wjTooltipA11y .wjFocusWithoutTooltip");
        }
        this._request?.abort();
        for (const view of this.views.values()) this._disposeView(view);
        this._visibilityObserver?.disconnect();
        for (const dialog of this._dialogs) dialog.destroy();
        const $ = window.jQuery;
        if ($?.fn.droppable && $(this.shortcutList).data("ui-droppable")) $(this.shortcutList).droppable("destroy");
        this.views.clear();
    }
}
