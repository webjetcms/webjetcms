import './webjet-server-monitoring';
import { DashboardController } from '../dashboard/dashboard';
import { registerDashboardWidgets, getDashboardDefaults } from '../dashboard/widgets';

/**
 * Configuration for the administration overview dashboard.
 *
 * @typedef {Object} WebjetOverviewDashboardOptions
 * @property {Object} [data={}] - Lightweight bootstrap context; widgets load uncached projections independently.
 * @property {Object[]} [data.dashboardMenu=[]] - Authorized administration navigation for shortcut selection.
 * @property {string} [data.userName=""] - Current user's display name.
 * @property {string} [data.currentDomain=""] - Active domain's display name.
 * @property {Object.<string, string>} [labels={}] - Localized labels used by dashboard sections and server monitoring.
 * @property {Object} [config={}] - Runtime dashboard configuration.
 * @property {string} [config.statMode] - Statistics mode; `"none"` hides statistics cards.
 * @property {string} [config.overviewJsonUrl=""] - Base URL used to load localized WebJET news.
 */

/**
 * Bookmark persisted by the overview dashboard.
 *
 * @typedef {Object} WebjetOverviewBookmark
 * @property {string} name - Display name.
 * @property {string} path - Administration URL.
 * @property {boolean} [baseline] - Whether the bookmark is protected from removal.
 */

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
}

/**
 * Creates a dashboard side card with an icon, title, and action container.
 *
 * @param {string} icon - Icon CSS class.
 * @param {string} title - Card title.
 * @param {string} extraClass - Card-specific CSS class.
 * @returns {HTMLDivElement} The card wrapper.
 */
function createOverviewCard(icon, title, extraClass) {
    const wrapper = element("div", `overview-logged ${extraClass}`);
    const head = element("div", "overview-logged__head");
    const iconWrapper = element("div", "overview-logged__head__icon");
    iconWrapper.innerHTML = `<i class="ti ${icon} fs-4" aria-hidden="true"></i>`;
    head.append(iconWrapper, element("span", "", title), element("div", "overview-logged__head__more"));
    wrapper.appendChild(head);
    return wrapper;
}

/**
 * Renders the administration overview from server-provided bootstrap data.
 */
export class WebjetOverviewDashboardElement extends HTMLElement {
    constructor() {
        super();
        this.data = {};
        this.labels = {};
        this.config = {};
        this._configured = false;
        this._feedbackListeners = [];
    }

    connectedCallback() {
        if (!this._configured) return;
        this.render();
    }

    disconnectedCallback() {
        this.dashboardController?.destroy();
        this._noticesRequest?.abort();
        this._legacyRequest?.abort();
        this._feedbackListeners.forEach(([name, listener]) => window.removeEventListener(name, listener));
        this._feedbackListeners = [];
    }

    /**
     * Applies dashboard data, labels, and runtime configuration.
     *
     * @param {WebjetOverviewDashboardOptions} [options={}] - Dashboard options.
     * @returns {WebjetOverviewDashboardElement} The configured element.
     */
    configure({ data = {}, labels = {}, config = {} } = {}) {
        this.data = data;
        this.labels = labels;
        this.config = config;
        this._configured = true;
        if (this.isConnected) this.render();
        return this;
    }

    /**
     * Rebuilds all dashboard sections and embedded server monitoring.
     *
     * Emits a bubbling, non-cancelable `webjet-component-ready` event without detail
     * after the dashboard is rendered.
     */
    render() {
        this.disconnectedCallback();
        this.replaceChildren();
        const overview = element("div", "overview");
        const widgets = element("div");
        overview.append(widgets);
        if (this.config.dashboardLegacy !== false) {
            const legacy = element("details", "md-dashboard__legacy");
            legacy.append(element("summary", "", WJ.translate("admin.dashboard.legacy.js")));
            const content = element("div", "md-dashboard__legacy-content");
            legacy.append(content);
            legacy.addEventListener("toggle", () => {
                if (legacy.open && !legacy.dataset.loaded) this._loadLegacy(legacy, content);
            });
            overview.append(legacy);
        }
        this.appendChild(overview);
        registerDashboardWidgets();
        const context = { data: this.data, labels: this.labels, config: this.config, overview: this, translate: (key, ...params) => WJ.translate(key, ...params) };
        context.config.dashboardDefaults ||= getDashboardDefaults(context);
        this.dashboardController = new DashboardController(widgets, context);
        this._loadNotices();
        this.dashboardReady = this.dashboardController.start();
        this.dataset.ready = "true";
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    /** Loads system notices independently so uncached checks never delay the dashboard shell. */
    async _loadNotices() {
        this._noticesRequest?.abort();
        const request = this._noticesRequest = new AbortController();
        const host = this.dashboardController.notices;
        let container = host.querySelector(".md-dashboard__notice-list");
        if (!container) {
            container = element("section", "md-dashboard__notice-list");
            host.prepend(container);
        }
        container.setAttribute("aria-label", WJ.translate("admin.dashboard.notices.js"));
        container.setAttribute("aria-busy", "true");
        container.replaceChildren(element("p", "md-dashboard__loading", WJ.translate("admin.dashboard.loading.js")));
        try {
            const response = await fetch("/admin/rest/dashboard/notices", { signal: request.signal, credentials: "same-origin", headers: { Accept: "application/json", "X-CSRF-Token": window.csrfToken } });
            if (!response.ok) throw new Error(`Dashboard notices: ${response.status}`);
            const notices = await response.json();
            if (request.signal.aborted) return;
            container.replaceChildren();
            if (notices.length) {
                const heading = element("p", "md-dashboard__notices-heading", WJ.translate("admin.dashboard.notices.js"));
                heading.append(element("span", "md-dashboard__notice-count", notices.length));
                container.append(heading);
            }
            for (const notice of notices) {
                const details = element("details", "md-dashboard__notice");
                details.dataset.noticeId = notice.id;
                details.dataset.severity = notice.severity;
                const summary = element("summary");
                const icon = element("i", `ti ${/^ti-[a-z0-9-]+$/.test(notice.icon) ? notice.icon : "ti-info-circle"}`);
                icon.setAttribute("aria-hidden", "true");
                const chevron = element("i", "ti ti-chevron-down md-dashboard__notice-chevron");
                chevron.setAttribute("aria-hidden", "true");
                summary.append(icon, element("span", "md-dashboard__notice-title", notice.title), chevron);
                const body = element("div", "md-dashboard__notice-body");
                // This HTML is produced by the authorized server notice service, never by widget preferences.
                body.innerHTML = notice.bodyHtml || "";
                if (notice.action) {
                    const action = element("button", "btn btn-sm btn-outline-secondary", notice.action.label);
                    action.type = "button";
                    action.addEventListener("click", () => {
                        if (notice.action.type === "popup") WJ.openPopupDialog(notice.action.url);
                        else if (notice.action.type === "help") WJ.showHelpWindow(notice.action.url);
                        else if (notice.action.type === "link") window.open(notice.action.url, "_blank", "noopener");
                    });
                    body.append(action);
                }
                details.append(summary, body);
                container.append(details);
            }
        } catch (error) {
            if (request.signal.aborted) return;
            const retry = element("button", "btn btn-sm btn-outline-secondary", WJ.translate("admin.dashboard.retry.js"));
            retry.type = "button";
            retry.addEventListener("click", () => this._loadNotices());
            const message = element("p", "text-danger mb-0", WJ.translate("admin.dashboard.noticesError.js"));
            message.setAttribute("role", "alert");
            container.replaceChildren(message, retry);
        } finally {
            if (!request.signal.aborted) container.setAttribute("aria-busy", "false");
        }
    }

    /** Requests legacy projections only when the user first opens the additional overview. */
    async _loadLegacy(legacy, content) {
        if (this._legacyRequest && !this._legacyRequest.signal.aborted) return;
        const request = this._legacyRequest = new AbortController();
        content.replaceChildren(element("p", "md-dashboard__loading", WJ.translate("admin.dashboard.loading.js")));
        try {
            const response = await fetch("/admin/rest/dashboard/legacy-data", { signal: request.signal, credentials: "same-origin", headers: { Accept: "application/json", "X-CSRF-Token": window.csrfToken } });
            if (!response.ok) throw new Error(`Dashboard legacy data: ${response.status}`);
            const data = await response.json();
            if (request.signal.aborted) return;
            Object.assign(this.data, data);
            const row = element("div", "row");
            const main = element("div", "col-lg-9");
            const side = element("div", "col-lg-3 pl-0-lg");
            main.append(this._renderWebsites());
            const monitoring = document.createElement("webjet-server-monitoring");
            monitoring.configure({ complex: false, labels: this.labels });
            main.append(monitoring);
            side.append(this._renderUsers(), this._renderBookmarks(), this._renderFeedback());
            row.append(main, side);
            content.replaceChildren(row);
            legacy.dataset.loaded = "true";
        } catch (error) {
            if (request.signal.aborted) return;
            const retry = element("button", "btn btn-sm btn-outline-secondary", WJ.translate("admin.dashboard.retry.js"));
            retry.type = "button";
            retry.addEventListener("click", () => this._loadLegacy(legacy, content));
            content.replaceChildren(element("p", "text-danger", WJ.translate("admin.dashboard.widgetError.js")), retry);
        } finally {
            if (!request.signal.aborted) this._legacyRequest = null;
        }
    }

    _renderWebsites() {
        const wrapper = element("div", "overview__websites");
        const active = WJ.hasPermission("menuWebpages") ? "websites" : "audit";
        const tabs = [["websites", "menuWebpages", this.labels.changedWebPages], ["audit", "cmp_adminlog", this.labels.audit]];
        const nav = element("nav");
        const tabList = element("div", "nav nav-tabs");
        tabList.setAttribute("role", "tablist");
        const content = element("div", "tab-content");
        tabs.forEach(([id, permission, title]) => {
            const selected = id === active;
            const tab = element("a", `nav-item nav-link noperms-${permission}${selected ? " active" : ""}`, title || "");
            tab.id = `nav-${id}-tab`;
            tab.href = `#nav-${id}`;
            tab.dataset.bsToggle = "tab";
            tab.setAttribute("role", "tab");
            tab.setAttribute("aria-controls", `nav-${id}`);
            tab.setAttribute("aria-selected", String(selected));
            tabList.append(tab);
            const pane = element("div", `tab-pane fade overview__websites-list${selected ? " show active" : ""}`);
            pane.id = `nav-${id}`;
            pane.setAttribute("role", "tabpanel");
            pane.setAttribute("aria-labelledby", tab.id);
            content.append(pane);
        });
        nav.append(tabList);
        wrapper.append(nav, content);
        this._renderPageList(wrapper.querySelector("#nav-websites"), this.data.changedPages || [], "changed");
        this._renderPageList(wrapper.querySelector("#nav-audit"), this.data.adminLog || [], "audit");
        return wrapper;
    }

    /**
     * Appends changed-page or audit entries to a dashboard list.
     *
     * @param {HTMLElement} container - Element that receives the generated list.
     * @param {Object[]} items - Page or audit entries to render.
     * @param {string} type - Entry type: `"changed"` or `"audit"`.
     */
    _renderPageList(container, items, type) {
        const list = element("ul");
        items.forEach(item => {
            const li = element("li");
            const link = element("a", "overview__websites-list__link text-truncate");
            link.href = type === "audit" ? `/admin/v9/apps/audit-search/?id=${item.logId}` : `/admin/v9/webpages/web-pages-list/?docid=${item.docId}`;
            link.innerHTML = `<i class="ti ${type === "audit" ? "ti-shield-search" : "ti-pencil fs-5"}"></i>`;
            if (item.createdByUserId > 0) link.append(element("span", "user", `${item.createdByUserName}: `));
            link.append(element("span", type === "audit" ? "type" : "title", type === "audit" ? item.type : item.title));
            if (type === "audit") link.append(element("span", "description", item.description));
            else link.append(document.createElement("br"), element("span", "path", item.fullPath));
            link.append(element("span", "date", item.date || item.saveDate));
            li.appendChild(link);
            list.appendChild(li);
        });
        container.appendChild(list);
    }

    _renderUsers() {
        const wrapper = createOverviewCard("ti-users", this.labels.loggedAdmins || "", "users noperms-welcomeShowLoggedAdmins");
        const adminsContainer = element("div", "overview-logged__content overview-logged__admins noperms-welcomeShowLoggedAdmins");
        const adminsList = element("ul");
        const admins = this.data.admins || [];
        const renderAdmins = count => {
            adminsList.querySelectorAll("li:not(.subheading)").forEach(item => item.remove());
            admins.slice(0, count).forEach(user => {
                const li = element("li");
                const userWrapper = element("span");
                if (user.photo) {
                    const img = element("img");
                    img.src = user.photo.startsWith("http") ? user.photo : `/thumb${user.photo}?w=30&h=30&ip=5`;
                    img.alt = user.fullName;
                    userWrapper.appendChild(img);
                } else userWrapper.appendChild(element("span", "no-photo ti ti-user fs-3"));
                const name = element("a", "name", user.fullName);
                name.href = `mailto:${user.email}`;
                userWrapper.appendChild(name);
                const email = element("a", "float-end btn btn-sm");
                email.href = `mailto:${user.email}`;
                email.setAttribute("aria-label", WJ.translate("admin.welcome.logins.sendEmail.js"));
                email.innerHTML = '<i class="ti ti-mail fs-6" aria-hidden="true"></i>';
                li.append(userWrapper, email);
                adminsList.appendChild(li);
            });
            if (count < admins.length) {
                const more = element("li", "show-more");
                const moreButton = element("button", "btn btn-outline p-0", `+${admins.length - count}`);
                moreButton.addEventListener("click", () => renderAdmins(admins.length));
                more.appendChild(moreButton);
                adminsList.appendChild(more);
            }
        };
        renderAdmins(4);
        adminsContainer.appendChild(adminsList);
        wrapper.append(adminsContainer);
        return wrapper;
    }

    /**
     * Loads persisted bookmarks, falling back to localized defaults unless storage contains a non-empty array.
     *
     * @returns {WebjetOverviewBookmark[]} The stored non-empty bookmark list or default bookmarks.
     */
    _getBookmarks() {
        const defaults = [{ name: WJ.translate("admin.welcome.bookmarks.default.webPages.js"), path: "/admin/v9/webpages/web-pages-list/" }, { name: WJ.translate("admin.welcome.bookmarks.default.forms.js"), path: "/apps/form/admin/" }];
        try {
            const stored = JSON.parse(localStorage.getItem("bookmarks"));
            return Array.isArray(stored) && stored.length ? stored : defaults;
        } catch (error) {
            return defaults;
        }
    }

    _renderBookmarks() {
        const wrapper = createOverviewCard("ti-bookmarks", WJ.translate("admin.welcome.bookmarks.title.js"), "bookmark");
        const add = element("button", "btn btn-outline p-0");
        add.type = "button";
        add.setAttribute("aria-label", WJ.translate("button.add"));
        add.innerHTML = '<i class="ti ti-plus" aria-hidden="true"></i>';
        add.addEventListener("click", () => this._showBookmarkModal(wrapper));
        wrapper.querySelector(".overview-logged__head__more").appendChild(add);
        const content = element("div", "overview-logged__content");
        const list = element("ul");
        const render = () => {
            list.replaceChildren();
            this._getBookmarks().forEach((bookmark, index) => {
                const li = element("li");
                const link = element("a", "", bookmark.name);
                link.href = bookmark.path;
                li.appendChild(link);
                if (!bookmark.baseline) {
                    const remove = element("button", "float-end btn btn-sm buttons-selected buttons-remove buttons-divider");
                    remove.type = "button";
                    remove.setAttribute("aria-label", WJ.translate("button.delete"));
                    remove.innerHTML = '<span><i class="ti ti-trash fs-6" aria-hidden="true"></i></span>';
                    remove.addEventListener("click", () => {
                        const bookmarks = this._getBookmarks();
                        bookmarks.splice(index, 1);
                        localStorage.setItem("bookmarks", JSON.stringify(bookmarks));
                        render();
                    });
                    li.appendChild(remove);
                }
                list.appendChild(li);
            });
        };
        render();
        content.appendChild(list);
        wrapper.appendChild(content);
        wrapper._renderBookmarks = render;
        return wrapper;
    }

    _showBookmarkModal(wrapper) {
        if (document.querySelector("#bookmark_modal")) return;
        const modalElement = element("div", "modal fade DTED");
        modalElement.id = "bookmark_modal";
        modalElement.setAttribute("role", "dialog");
        modalElement.innerHTML = `<div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">${WJ.escapeHtml(WJ.translate("admin.welcome.bookmarks.dialog.title.js"))}</h5></div><div class="modal-body"><div class="modal-body-bg"><form class="form-horizontal"><div class="DTE_Field form-group row required"><label class="col-sm-4 col-form-label" for="bookmark-group-name">${WJ.escapeHtml(WJ.translate("admin.welcome.bookmarks.dialog.name.js"))}</label><div class="col-sm-7"><input id="bookmark-group-name" class="form-control"><div class="name-error form-text text-danger small invisible">${WJ.escapeHtml(WJ.translate("admin.welcome.bookmarks.dialog.requiredField.js"))}</div></div></div><div class="DTE_Field form-group row required"><label class="col-sm-4 col-form-label" for="bookmark-group-path">${WJ.escapeHtml(WJ.translate("admin.welcome.bookmarks.dialog.urlAddress.js"))}</label><div class="col-sm-7"><input id="bookmark-group-path" class="form-control" value="/admin/v9/"><div class="path-error form-text text-danger small invisible">${WJ.escapeHtml(WJ.translate("admin.welcome.bookmarks.dialog.requiredField.js"))}</div></div></div></form></div></div><div class="modal-footer"><div class="DTE_Form_Buttons"><button type="button" class="btn btn-outline-secondary btn-close-editor"><i class="ti ti-x"></i> ${WJ.escapeHtml(WJ.translate("button.cancel"))}</button><button type="button" class="btn btn-primary"><i class="ti ti-check"></i> ${WJ.escapeHtml(WJ.translate("button.add"))}</button></div></div></div></div>`;
        document.body.appendChild(modalElement);
        const modal = new bootstrap.Modal(modalElement, { keyboard: false, backdrop: "static" });
        const close = () => { modal.hide(); modalElement.addEventListener("hidden.bs.modal", () => modalElement.remove(), { once: true }); };
        modalElement.querySelector(".btn-close-editor").addEventListener("click", close);
        modalElement.querySelector(".btn-primary").addEventListener("click", () => {
            const name = modalElement.querySelector("#bookmark-group-name");
            const path = modalElement.querySelector("#bookmark-group-path");
            let valid = true;
            [[name, ".name-error"], [path, ".path-error"]].forEach(([input, selector]) => {
                const error = modalElement.querySelector(selector);
                error.classList.toggle("invisible", input.value.trim() !== "");
                if (!input.value.trim()) valid = false;
            });
            if (!valid) return;
            let bookmarkPath = path.value.trim();
            if (bookmarkPath.startsWith(window.location.origin)) bookmarkPath = bookmarkPath.substring(window.location.origin.length);
            const bookmarks = this._getBookmarks();
            bookmarks.push({ name: name.value.trim(), path: bookmarkPath, baseline: false });
            localStorage.setItem("bookmarks", JSON.stringify(bookmarks));
            wrapper._renderBookmarks();
            close();
        });
        modal.show();
    }

    _renderFeedback() {
        const wrapper = createOverviewCard("ti-message-2", WJ.translate("admin.welcome.feedback.title.js"), "feedback");
        const open = element("button", "btn btn-outline p-0");
        open.type = "button";
        open.setAttribute("aria-label", WJ.translate("admin.welcome.feedback.sendButton.js"));
        open.innerHTML = '<i class="ti ti-writing" aria-hidden="true"></i>';
        open.addEventListener("click", () => this._showFeedbackModal());
        wrapper.querySelector(".overview-logged__head__more").appendChild(open);
        const content = element("div", "overview-logged__content");
        content.append(element("p", "perex", WJ.translate("admin.welcome.feedback.intro.js")));
        const actions = element("p", "text-end");
        const send = element("button", "btn btn-primary", WJ.translate("admin.welcome.feedback.sendButton.js"));
        send.type = "button";
        send.addEventListener("click", () => this._showFeedbackModal());
        actions.appendChild(send);
        content.appendChild(actions);
        wrapper.appendChild(content);
        return wrapper;
    }

    /**
     * Opens the feedback form, tracks uploaded files, and submits the completed feedback.
     */
    _showFeedbackModal() {
        if (document.querySelector("#feedback_modal")) return;
        const modalElement = element("div", "modal fade DTED");
        modalElement.id = "feedback_modal";
        modalElement.innerHTML = `<div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.title.js"))}</h5></div><form><div class="modal-body"><div class="modal-body-bg"><div class="DTE_Field form-group row required"><label class="col-sm-4 col-form-label" for="feedback-group-text">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.feedback_text.js"))}</label><div class="col-sm-7"><textarea id="feedback-group-text" class="form-control" rows="7" aria-describedby="feedback-text-error"></textarea><div id="feedback-text-error" class="text-error form-text text-danger small invisible" role="alert">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.error.js"))}</div></div></div><div class="DTE_Field form-group row"><label class="col-sm-4 col-form-label">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.files.js"))}</label><div class="col-sm-7"><div id="feedback-upload" class="drop-zone-box dropzone"></div></div></div><div class="DTE_Field form-group row"><label class="col-sm-4 col-form-label" for="feedback-group-anonymous">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.send_anonym.js"))}</label><div class="col-sm-7"><input id="feedback-group-anonymous" type="checkbox" class="form-check-input"></div></div></div></div><div class="modal-footer"><div class="DTE_Form_Buttons"><button type="button" class="btn btn-outline-secondary btn-close-editor"><i class="ti ti-x"></i> ${WJ.escapeHtml(WJ.translate("button.cancel"))}</button><button type="submit" class="btn btn-primary"><i class="ti ti-check"></i> ${WJ.escapeHtml(WJ.translate("button.send"))}</button></div></div></form></div></div>`;
        const uploadProgressIndicator = `<svg class="fa-progress-bar float-end" xmlns="http://www.w3.org/2000/svg" viewBox="-1 -1 34 34" aria-hidden="true"><circle cx="16" cy="16" r="15" class="fa-progress-bar__background"></circle><circle cx="16" cy="16" r="15" class="fa-progress-bar__progress" style="stroke-dashoffset: 100px"></circle></svg>`;
        const uploadContainer = modalElement.querySelector("#feedback-upload").parentElement;
        const uploadWrapper = element("div", "upload-wrapper");
        uploadWrapper.id = "upload-wrapper";
        uploadWrapper.style.display = "none";
        const uploadProgress = element("div", "toast-container-progress");
        uploadProgress.append(element("span", "", WJ.translate("admin.welcome.feedback.dialog.uploaded_files.js")));
        uploadProgress.insertAdjacentHTML("beforeend", uploadProgressIndicator);
        const toastContainer = element("div", "toast-container-upload");
        toastContainer.id = "toast-container-upload";
        uploadWrapper.append(uploadProgress, toastContainer);
        const uploadTemplate = element("div", "upload-toastr-template");
        uploadTemplate.id = "upload-toastr-template";
        uploadTemplate.style.display = "none";
        uploadTemplate.innerHTML = `<i class="ti ti-polaroid" aria-hidden="true"></i><span>{FILE_NAME}</span><i class="ti ti-circle-check float-end" aria-hidden="true"></i><i class="ti ti-alert-triangle float-end" aria-hidden="true"></i><i class="ti ti-loader-2 ti-spin float-end" aria-hidden="true"></i><i class="ti ti-alert-circle float-end" aria-hidden="true"></i>${uploadProgressIndicator}<div class="toast-error-message"></div>`;
        uploadContainer.append(uploadWrapper, uploadTemplate);
        document.body.appendChild(modalElement);
        const modal = new bootstrap.Modal(modalElement, { keyboard: false, backdrop: "static" });
        const fileKeys = [];
        try { window.AdminUpload({ element: "#feedback-upload", destinationFolder: "/files/protected/feedback-form/", writeDirectlyToDestination: false }); } catch (error) { console.warn("Feedback upload initialization failed", error); }
        const uploadListener = event => fileKeys.push(event.detail.key);
        window.addEventListener("WJ.AdminUpload.success", uploadListener);
        this._feedbackListeners.push(["WJ.AdminUpload.success", uploadListener]);
        const close = () => {
            window.removeEventListener("WJ.AdminUpload.success", uploadListener);
            this._feedbackListeners = this._feedbackListeners.filter(([, listener]) => listener !== uploadListener);
            modal.hide();
            modalElement.addEventListener("hidden.bs.modal", () => modalElement.remove(), { once: true });
        };
        modalElement.querySelector(".btn-close-editor").addEventListener("click", close);
        modalElement.querySelector("form").addEventListener("submit", event => {
            event.preventDefault();
            const textarea = modalElement.querySelector("#feedback-group-text");
            const error = modalElement.querySelector(".text-error");
            if (!textarea.value.trim()) { error.classList.remove("invisible"); return; }
            $.ajax({ type: "POST", url: "/admin/rest/feedback", data: { data: { text: textarea.value, fileKeys, isAnonymous: modalElement.querySelector("#feedback-group-anonymous").checked } }, success: result => result === "OK" ? WJ.notifySuccess(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.success_notify.js"), 20000) : WJ.notifyError(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.error_notify.js"), 60000), error: () => WJ.notifyError(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.error_notify.js"), 60000) });
            close();
        });
        modal.show();
    }


}

if (!customElements.get("webjet-overview-dashboard")) customElements.define("webjet-overview-dashboard", WebjetOverviewDashboardElement);
