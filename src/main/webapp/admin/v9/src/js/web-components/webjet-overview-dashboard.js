import './webjet-server-monitoring';

/**
 * Configuration for the administration overview dashboard.
 *
 * @typedef {Object} WebjetOverviewDashboardOptions
 * @property {Object} [data={}] - Bootstrap data for statistics, pages, users, sessions, and audit entries.
 * @property {Object} [data.backData={}] - Aggregate dashboard statistics.
 * @property {Object[]} [data.admins=[]] - Logged-in administrators.
 * @property {Object[]} [data.recentPages=[]] - Pages recently edited by the current user.
 * @property {Object[]} [data.changedPages=[]] - Recently changed pages.
 * @property {Object[]} [data.adminLog=[]] - Recent audit entries.
 * @property {Object} [data.currentSessions={}] - Current-session ID and clustered user sessions.
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
        const row = element("div", "row");
        const main = element("div", "col-lg-9");
        const side = element("div", "col-lg-3 pl-0-lg");
        main.append(element("div", "toast-container", null));
        main.firstChild.id = "toast-container-overview";
        main.append(this._renderInfo(), this._renderWebsites());

        const monitoring = document.createElement("webjet-server-monitoring");
        monitoring.configure({ complex: false, labels: this.labels });
        main.appendChild(monitoring);

        side.append(this._renderUsers(), this._renderBookmarks(), this._renderFeedback(), this._renderNews());
        row.append(main, side);
        overview.appendChild(row);
        this.appendChild(overview);
        this.dataset.ready = "true";
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    _renderInfo() {
        const section = element("section", "overview__dashboard");
        const title = element("div", "overview__dashboard__title");
        title.append(element("h2", "", `${this.labels.welcome || ""}, ${window.currentUser?.fullName || ""}`));
        const changelog = element("p");
        changelog.innerHTML = this.labels.changelog || "";
        const link = element("a", "btn btn-primary", this.labels.seeCompleteChangelog || "");
        link.href = `http://docs.webjetcms.sk/latest/${window.userLng}/CHANGELOG`;
        link.target = "_blank";
        title.append(changelog, link);
        section.appendChild(title);

        const cards = element("div", "row");
        const stats = this.data.backData || {};
        const items = [];
        if (WJ.hasPermission("cmp_stat") && this.config.statMode !== "none") items.push([this.labels.overviewViews, "navstevy", "ti-chart-area-line", "#0063fb", stats.statViewsNumber, "/apps/stat/admin/"]);
        if (WJ.hasPermission("cmp_form")) items.push([this.labels.overviewForms, "formulare", "ti-forms", "#007f5e", `+${stats.fillFormsNumber}`, "/apps/form/admin/"]);
        if (WJ.hasPermission("cmp_diskusia")) items.push([this.labels.overviewForum, "foto", "ti-messages", "#c000d5", `+${stats.documentForumNumber}`, "/apps/forum/admin/"]);
        if (WJ.hasPermission("cmp_stat") && this.config.statMode !== "none") items.push([this.labels.overviewErrors, "dokumenty", "ti-face-id-error", "#d90575", `+${stats.statErrorNumber}`, "/apps/stat/admin/error/"]);
        items.forEach(([itemTitle, itemClass, icon, color, number, href]) => {
            const col = element("div", "col-md-3");
            const item = element("div", `${itemClass} overview__dashboard__item`);
            item.append(element("p", "overview__dashboard__item__title", itemTitle || ""));
            const anchor = element("a", "overview__dashboard__item__info");
            anchor.href = href;
            const iconElement = element("i", `ti ${icon} fs-1`);
            iconElement.style.color = color;
            anchor.append(iconElement, element("div", "overview__dashboard__item__info__number", number));
            item.appendChild(anchor);
            col.appendChild(item);
            cards.appendChild(col);
        });
        section.appendChild(cards);
        return section;
    }

    _renderWebsites() {
        const wrapper = element("div", "overview__websites");
        wrapper.innerHTML = `<nav><div class="nav nav-tabs" id="nav-tab" role="tablist"><a class="nav-item nav-link active noperms-menuWebpages" id="nav-mysites-tab" data-bs-toggle="tab" href="#nav-mysites" role="tab">${WJ.escapeHtml(this.labels.myLastPages || "")}</a><a class="nav-item nav-link noperms-menuWebpages" id="nav-websites-tab" data-bs-toggle="tab" href="#nav-websites" role="tab">${WJ.escapeHtml(this.labels.changedWebPages || "")}</a><a class="nav-item nav-link noperms-cmp_adminlog" id="nav-audit-tab" data-bs-toggle="tab" href="#nav-audit" role="tab">${WJ.escapeHtml(this.labels.audit || "")}</a></div></nav><div class="tab-content" id="nav-tabContent"><div class="tab-pane fade show active overview__websites-list" id="nav-mysites"></div><div class="tab-pane fade overview__websites-list" id="nav-websites"></div><div class="tab-pane fade overview__websites-list" id="nav-audit"></div></div>`;
        this._renderPageList(wrapper.querySelector("#nav-mysites"), this.data.recentPages || [], "recent");
        this._renderPageList(wrapper.querySelector("#nav-websites"), this.data.changedPages || [], "changed");
        this._renderPageList(wrapper.querySelector("#nav-audit"), this.data.adminLog || [], "audit");
        return wrapper;
    }

    /**
     * Appends recent-page, changed-page, or audit entries to a dashboard list.
     *
     * @param {HTMLElement} container - Element that receives the generated list.
     * @param {Object[]} items - Page or audit entries to render.
     * @param {string} type - Entry type: `"recent"`, `"changed"`, or `"audit"`.
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
        const wrapper = createOverviewCard("ti-users", WJ.translate("admin.welcome.logins.title.js"), "users");
        const sessionsContainer = element("div", "overview-logged__content overview-logged__sessions");
        const sessionsList = element("ul");
        sessionsList.append(element("li", "subheading", WJ.translate("admin.welcome.active_sessions.title.js")));
        const currentSessions = this.data.currentSessions || {};
        const sessions = (currentSessions.userSessions || []).flatMap(cluster => (cluster.userSessions || []).map(session => ({ ...session, cluster: cluster.cluster }))).sort((a, b) => b.logonTime - a.logonTime);
        sessions.forEach(session => {
            const li = element("li");
            const entry = element("span", "active-session-entry", `${WJ.formatTimeSeconds(session.logonTime)} (${session.browserName}, ${session.remoteAddr})`);
            entry.title = this._sessionTooltip(session);
            li.appendChild(entry);
            if (currentSessions.currentSessionId === session.sessionId) {
                const current = element("span", "float-end");
                current.setAttribute("role", "img");
                current.setAttribute("aria-label", WJ.translate("admin.welcome.active_sessions.current_session.js"));
                current.innerHTML = '<i class="ti ti-current-location fs-6" aria-hidden="true"></i>';
                li.appendChild(current);
            } else {
                const logout = element("button", "float-end btn btn-sm");
                logout.type = "button";
                logout.setAttribute("aria-label", WJ.translate("menu.logout", { domain: session.domainName }));
                logout.innerHTML = '<i class="ti ti-logout fs-6" aria-hidden="true"></i>';
                logout.addEventListener("click", () => this._removeSession(session.sessionId, li));
                li.appendChild(logout);
            }
            sessionsList.appendChild(li);
        });
        sessionsContainer.appendChild(sessionsList);

        const adminsContainer = element("div", "overview-logged__content overview-logged__admins noperms-welcomeShowLoggedAdmins");
        const adminsList = element("ul");
        adminsList.append(element("li", "subheading", this.labels.loggedAdmins || ""));
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
        wrapper.append(sessionsContainer, adminsContainer);
        return wrapper;
    }

    /**
     * Invalidates an administrator session and removes its row after a successful response.
     *
     * @param {string} sessionId - Server session identifier.
     * @param {HTMLElement} row - Session row to remove.
     * @returns {Promise<void>} A promise that settles after the removal request completes.
     */
    async _removeSession(sessionId, row) {
        const response = await fetch("/admin/rest/removeSession", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded; charset=utf-8", "X-CSRF-Token": window.csrfToken }, body: new URLSearchParams({ sessionId }) });
        if (response.ok) row.remove();
    }

    _sessionTooltip(session) {
        return [["admin.welcome.active_sessions.userAgent.js", session.browserName], ["admin.welcome.active_sessions.remoteAddr.js", session.remoteAddr], ["admin.welcome.active_sessions.logonTime.js", session.logonTime ? WJ.formatDateTimeSeconds(new Date(session.logonTime).toUTCString()) : null], ["admin.welcome.active_sessions.domainName.js", session.domainName], ["admin.welcome.active_sessions.server.js", session.cluster]].filter(([, value]) => value).map(([key, value]) => `${WJ.translate(key)}:\n\t${value}`).join("\n");
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

    _renderNews() {
        const wrapper = element("div", "overview__news");
        wrapper.innerHTML = `<div class="overview__news__head"><div class="overview__news__head__icon"><i class="ti ti-rss"></i></div><span>${WJ.escapeHtml(this.labels.newsInWebJET || "")}</span></div><div class="overview__news__content"><ul></ul></div>`;
        const language = window.userLng === "en" ? "en" : window.userLng === "cs" ? "cs" : "sk";
        $.get({ url: `${this.config.overviewJsonUrl || ""}wjnews.${language}.json`, success: data => {
            const list = wrapper.querySelector("ul");
            (data?.news || []).slice(0, 3).forEach(news => {
                const li = element("li");
                const link = element("a", "overview__news__content__link");
                link.href = news.link;
                link.target = "_blank";
                link.title = news.title;
                link.append(element("span", "title", news.title));
                const perex = element("span", "perex");
                perex.innerHTML = WJ.parseMarkdown(news.perex);
                link.appendChild(perex);
                li.appendChild(link);
                list.appendChild(li);
            });
        }});
        return wrapper;
    }
}

if (!customElements.get("webjet-overview-dashboard")) customElements.define("webjet-overview-dashboard", WebjetOverviewDashboardElement);
