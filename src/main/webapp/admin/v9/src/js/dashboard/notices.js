import { date } from './widget-utils';

const SETTINGS_KEY = "dashboard.notices";
const WEEK = 7 * 24 * 60 * 60 * 1000;

function node(tag, className, text) {
    const element = document.createElement(tag);
    element.className = className;
    if (text != null) element.textContent = text;
    return element;
}

function button(label, action, className = "btn btn-sm btn-link") {
    const element = node("button", className, label);
    element.type = "button";
    element.addEventListener("click", action);
    return element;
}

/** Owns only system-notice rendering and account preferences, independently of the personal grid. */
export class DashboardNotices {
    constructor(host, data, openSessions, openSecurityEvent) {
        this.openSessions = openSessions;
        this.openSecurityEvent = openSecurityEvent;
        this.host = host;
        this.data = data;
        this.state = { dismissedUntil: {} };
        try {
            const saved = JSON.parse(window.currentUser?.adminSettings?.[SETTINGS_KEY] || "null");
            if (saved && typeof saved.dismissedUntil === "object" && saved.dismissedUntil) {
                this.state.dismissedUntil = saved.dismissedUntil;
            }
        } catch (error) { /* Invalid preferences leave all active notices visible. */ }
        this.container = node("section", "md-dashboard__notice-list");
        this.container.setAttribute("aria-label", this._t("notices"));
        this.container.setAttribute("aria-busy", "false");
        host.prepend(this.container);
    }

    _t(key, ...params) { return WJ.translate(`admin.dashboard.${key}.js`, ...params); }

    /** Sorts active notices without allowing a stored dismissal to suppress an error. */
    render() {
        if (this.destroyed) return;
        window.clearTimeout(this.expiryTimer);
        const rank = { error: 0, warning: 1, info: 2 };
        const now = Date.now();
        const active = [...(this.data.notices || [])];
        const sessionCount = (this.data.currentSessions?.userSessions || []).reduce((count, cluster) => count + (cluster.userSessions || []).length, 0);
        if (sessionCount > 1) active.push({
            id: "multipleSessions", severity: "warning", icon: "ti-devices",
            title: this._t("notice.multipleSessions"), description: this._t("notice.multipleSessions.description", sessionCount),
            action: { type: "sessions", label: this._t("activeSessions") }
        });
        const notices = active.filter(notice => {
            if (notice.kind === "newDevice") return !notice.securityEvent.confirmedAt && notice.securityEvent.expiresAt > now;
            const until = this.state.dismissedUntil[notice.id];
            return notice.severity === "error" || !(until > now || notice.severity === "info" && until === 0);
        }).sort((first, second) => Number(second.kind === "newDevice") - Number(first.kind === "newDevice")
            || (rank[first.severity] ?? 2) - (rank[second.severity] ?? 2));
        this.container.replaceChildren();
        if (notices.length) {
            const heading = node("h2", "md-dashboard__notices-heading", this._t("notices"));
            heading.append(node("span", "md-dashboard__notice-count", notices.length));
            this.container.append(heading);
            for (const notice of notices) this.container.append(this._row(notice));
        }
        this.status = node("div", "md-dashboard__notice-status text-danger");
        this.status.setAttribute("role", "alert");
        this.container.append(this.status);
        this.container.hidden = !notices.length;
        this._setBusy(this.busy);
        const expiry = [...Object.values(this.state.dismissedUntil),
            ...notices.filter(notice => notice.kind === "newDevice").map(notice => notice.securityEvent.expiresAt)]
            .filter(until => until > now).sort((a, b) => a - b)[0];
        if (expiry) this.expiryTimer = window.setTimeout(() => this.render(), Math.min(expiry - now + 1, 2147483647));
        window.scrollbarMain?.update();
    }

    _row(notice) {
        const wrapper = node("div", "md-dashboard__notice");
        wrapper.dataset.noticeId = notice.id;
        wrapper.dataset.severity = notice.severity;
        const row = node("div", "md-dashboard__notice-row");
        const icon = node("i", `ti ${/^ti-[a-z0-9-]+$/.test(notice.icon) ? notice.icon : "ti-info-circle"}`);
        icon.setAttribute("aria-hidden", "true");
        const text = node("div", "md-dashboard__notice-text");
        const body = document.createElement("div");
        // Older embedded notices can supply trusted HTML; the compact row renders its text only.
        body.innerHTML = notice.bodyHtml || "";
        let description = notice.description || body.textContent;
        if (notice.kind === "newDevice") {
            const event = notice.securityEvent;
            const browser = [event.browserName, event.browserVersion].filter(Boolean).join(" ");
            const device = [browser, event.operatingSystem].filter(Boolean).join(" · ");
            description = this._t("newDevice.details", device || "—", event.ipAddress || "—", date(event.createDate));
        }
        text.append(node("strong", "md-dashboard__notice-title", notice.title),
            node("span", "md-dashboard__notice-description", description));
        const actions = node("div", "md-dashboard__notice-actions");
        if (notice.kind === "newDevice") {
            actions.append(button(this._t("newDevice.confirm"), () => this._confirmSecurityEvent(notice),
                "btn btn-sm btn-outline-secondary md-dashboard__notice-confirm"));
            actions.append(button(this._t("newDevice.notMe"), () => this.openSecurityEvent?.(notice.securityEvent),
                "btn btn-sm btn-link text-danger md-dashboard__notice-report"));
        } else if (notice.action) {
            actions.append(button(notice.action.label, event => {
                if (notice.action.type === "popup") WJ.openPopupDialog(notice.action.url);
                else if (notice.action.type === "help") WJ.showHelpWindow(notice.action.url);
                else if (notice.action.type === "link") window.open(notice.action.url, "_blank", "noopener");
                else if (notice.action.type === "sessions") {
                    event.currentTarget.focus({ preventScroll: true });
                    this.openSessions?.();
                }
            }, "btn btn-sm btn-outline-secondary md-dashboard__notice-action"));
        }
        if (notice.kind !== "newDevice" && notice.severity !== "error") {
            if (notice.severity === "warning") actions.append(button(this._t("notice.later"), () => this._dismiss(notice, wrapper)));
            const close = button("", () => this._dismiss(notice, wrapper), "btn btn-sm btn-link md-dashboard__notice-dismiss");
            const label = this._t(notice.severity === "warning" ? "notice.later" : "notice.hide");
            close.setAttribute("aria-label", `${label}: ${notice.title}`);
            close.title = label;
            const cross = node("i", "ti ti-x");
            cross.setAttribute("aria-hidden", "true");
            close.append(cross);
            actions.append(close);
        }
        row.append(icon, text, actions);
        wrapper.append(row);
        return wrapper;
    }

    _setBusy(busy) {
        this.container.querySelectorAll("button").forEach(control => { control.disabled = !!busy; });
        this.container.setAttribute("aria-busy", String(!!busy));
    }

    /** Acknowledges a login on the server without using the ordinary notice-dismissal preferences. */
    async _confirmSecurityEvent(notice) {
        if (this.busy || this.destroyed) return;
        this.busy = true;
        this._setBusy(true);
        this.request = new AbortController();
        try {
            const response = await fetch(`/admin/rest/security/login-events/${encodeURIComponent(notice.securityEvent.id)}/confirm`, {
                method: "POST", credentials: "same-origin", signal: this.request.signal,
                headers: { "X-CSRF-Token": window.csrfToken || "" }
            });
            if (!response.ok) throw new Error("Login confirmation failed");
            const event = await response.json();
            if (event.id !== notice.securityEvent.id || !event.confirmedAt) throw new Error("Login confirmation was not saved");
            if (this.destroyed) return;
            this.data.notices = this.data.notices.filter(item => item.id !== notice.id);
            if (this.data.requestedSecurityEvent?.id === event.id) this.data.requestedSecurityEvent = event;
            this.busy = false;
            this.render();
            (this.container.querySelector("button") || this.host.closest("webjet-overview-dashboard")?.querySelector("button"))?.focus({ preventScroll: true });
            WJ.notifySuccess?.(this._t("newDevice.confirmed"), "", 10000);
        } catch (error) {
            if (!this.destroyed) this.status.textContent = this._t("newDevice.saveError");
        } finally {
            this.busy = false;
            if (!this.destroyed) this._setBusy(false);
        }
    }

    /** Saves just the notice record through the existing administration settings API. */
    async _save(next) {
        if (this.busy || this.destroyed) return false;
        this.busy = true;
        this._setBusy(true);
        this.request = new AbortController();
        try {
            const value = JSON.stringify(next);
            const response = await fetch("/admin/rest/admin-settings/", {
                method: "POST", credentials: "same-origin", signal: this.request.signal,
                headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken || "" },
                body: JSON.stringify({ label: SETTINGS_KEY, value })
            });
            if (!response.ok || await response.json() !== true) throw new Error("Notice preferences could not be saved");
            if (this.destroyed) return false;
            this.state = next;
            if (window.currentUser?.adminSettings) window.currentUser.adminSettings[SETTINGS_KEY] = value;
            return true;
        } catch (error) {
            if (!this.destroyed) this.status.textContent = this._t("saveError");
            return false;
        } finally {
            this.busy = false;
            this._setBusy(false);
        }
    }

    async _dismiss(notice, row) {
        if (notice.kind === "newDevice" || notice.severity === "error" || this.busy) return;
        const keyboard = row.contains(document.activeElement) && document.activeElement.matches(":focus-visible");
        const previous = this.state.dismissedUntil[notice.id];
        const until = notice.severity === "warning" ? Date.now() + WEEK : 0;
        if (!await this._save({ ...this.state, dismissedUntil: { ...this.state.dismissedUntil, [notice.id]: until } })) return;
        this.busy = true;
        this._setBusy(true);
        if (row.animate && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            this.animation = row.animate([
                { height: `${row.getBoundingClientRect().height}px`, opacity: 1, marginBottom: "6px" },
                { height: "0px", opacity: 0, marginBottom: "0px" }
            ], { duration: 200, easing: "ease-out", fill: "forwards" });
            try { await this.animation.finished; } catch (error) { /* Disconnect cancels the animation. */ }
        }
        if (this.destroyed) return;
        this.busy = false;
        this.render();
        this._showUndo(notice, previous, until, keyboard);
    }

    /** Shows an eight-second undo toast whose remaining time pauses on hover and keyboard focus. */
    _showUndo(notice, previous, until, keyboard) {
        this._clearToast();
        const toast = node("div", "md-dashboard__notice-toast");
        const message = until ? this._t("notice.reminder", new Date(until).toLocaleDateString(document.documentElement.lang || undefined)) : this._t("notice.hidden");
        const status = node("span", "", message);
        status.setAttribute("role", "status");
        const undo = button(this._t("notice.undo"), async () => {
            const dismissedUntil = { ...this.state.dismissedUntil };
            if (previous == null) delete dismissedUntil[notice.id];
            else dismissedUntil[notice.id] = previous;
            undo.disabled = true;
            if (await this._save({ ...this.state, dismissedUntil })) {
                this._clearToast();
                this.render();
                this.container.querySelector(`[data-notice-id="${notice.id}"] button`)?.focus({ preventScroll: true });
            } else undo.disabled = false;
        });
        const close = button("×", () => this._clearToast());
        close.setAttribute("aria-label", this._t("notice.closeToast"));
        toast.append(status, undo, close);
        document.body.append(toast);
        this.toast = toast;
        let remaining = 8000, started;
        const resume = () => {
            if (toast.matches(":hover") || toast.contains(document.activeElement)) return;
            started = Date.now();
            this.toastTimer = window.setTimeout(() => this._clearToast(), remaining);
        };
        const pause = () => {
            if (this.toastTimer) remaining = Math.max(0, remaining - (Date.now() - started));
            window.clearTimeout(this.toastTimer);
            this.toastTimer = null;
        };
        toast.addEventListener("mouseenter", pause);
        toast.addEventListener("mouseleave", resume);
        toast.addEventListener("focusin", pause);
        toast.addEventListener("focusout", event => { if (!toast.contains(event.relatedTarget)) resume(); });
        toast.addEventListener("keydown", event => { if (event.key === "Escape") this._clearToast(); });
        // The removed action's focus moves to undo so keyboard users can reverse the change.
        if (keyboard) undo.focus({ preventScroll: true });
        resume();
    }

    _clearToast() {
        window.clearTimeout(this.toastTimer);
        this.toastTimer = null;
        if (this.toast?.contains(document.activeElement)) this.container.querySelector("button")?.focus({ preventScroll: true });
        this.toast?.remove();
        this.toast = null;
    }

    destroy() {
        this.destroyed = true;
        this.request?.abort();
        this.animation?.cancel();
        window.clearTimeout(this.expiryTimer);
        this._clearToast();
        this.container.remove();
    }
}
