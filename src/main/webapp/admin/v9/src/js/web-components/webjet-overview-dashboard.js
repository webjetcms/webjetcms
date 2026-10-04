import { DashboardController } from '../dashboard/dashboard';
import { DashboardNotices } from '../dashboard/notices';
import { showActiveSessions } from '../dashboard/session-widgets';
import { registerDashboardWidgets, getDashboardDefaults } from '../dashboard/widgets';

/**
 * Configuration for the administration overview dashboard.
 *
 * @typedef {Object} WebjetOverviewDashboardOptions
 * @property {Object} [data={}] - Lightweight bootstrap context; widgets load uncached projections independently.
 * @property {Object[]} [data.dashboardMenu=[]] - Authorized administration navigation for shortcut selection.
 * @property {Object} data.settings - Current account's layout and active-domain preferences.
 * @property {Object[]} data.notices - System notices ready for immediate rendering.
 * @property {Object} data.currentSessions - Current user sessions, updated after a successful logout.
 * @property {string} [data.userName=""] - Current user's display name.
 * @property {number} data.statRootGroupId - Active domain root folder passed to the shared statistics API.
 * @property {string} [data.currentDomain=""] - Active domain's display name.
 * @property {Object.<string, string>} [labels={}] - Localized labels used by dashboard sections and widgets.
 * @property {Object} [config={}] - Runtime dashboard configuration.
 * @property {string} config.recentPagesGroupId - Configured systemPagesRecentPages ID used by the Web pages module.
 * @property {string} [config.statMode] - Statistics mode; `"none"` hides statistics cards.
 * @property {string} [config.overviewJsonUrl=""] - Base URL used to load localized WebJET news.
 * @property {string} [config.heroBackgroundImage] - Root-relative or HTTP(S) header image URL; an empty value hides it and omission retains the stylesheet default.
 */

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
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
        this.noticeController?.destroy();
        this.noticeController = null;
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
     * Rebuilds the dashboard shell and its independently loaded widgets.
     *
     * Emits a bubbling, non-cancelable `webjet-component-ready` event without detail
     * after the dashboard is rendered.
     */
    render() {
        this.disconnectedCallback();
        this.replaceChildren();
        const overview = element("div", "overview");
        const widgets = element("div");
        widgets.addEventListener("keydown", event => {
            // Keep the parent smooth scrollbar from cancelling native Space activation.
            if (event.key === " " && event.target.closest("button, summary")) event.stopPropagation();
        });
        overview.append(widgets);
        this.appendChild(overview);
        registerDashboardWidgets();
        const context = { data: this.data, labels: this.labels, config: this.config, overview: this, translate: (key, ...params) => WJ.translate(key, ...params) };
        context.config.dashboardDefaults ||= getDashboardDefaults(context);
        this.dashboardController = new DashboardController(widgets, context);
        this._renderNotices();
        this.dashboardReady = this.dashboardController.start();
        this.dataset.ready = "true";
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    /** Renders the system notices supplied by the dashboard page. */
    _renderNotices() {
        this.noticeController ||= new DashboardNotices(this.dashboardController.notices, this.data,
            () => showActiveSessions(this.dashboardController._widgetContext()));
        this.noticeController.render();
    }

    /**
     * Opens the feedback form, tracks uploaded files, and submits the completed feedback.
     */
    showFeedbackModal() {
        if (document.querySelector("#feedback_modal")) return;
        const trigger = document.activeElement;
        const modalElement = element("div", "modal fade DTED");
        modalElement.id = "feedback_modal";
        modalElement.tabIndex = -1;
        modalElement.setAttribute("aria-labelledby", "feedback-modal-title");
        modalElement.innerHTML = `<div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h2 id="feedback-modal-title" class="modal-title fs-5">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.title.js"))}</h2></div><form><div class="modal-body"><div class="modal-body-bg"><div class="DTE_Field form-group row required"><label class="col-sm-4 col-form-label" for="feedback-group-text">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.feedback_text.js"))}</label><div class="col-sm-7"><textarea id="feedback-group-text" class="form-control" rows="7" aria-required="true" aria-describedby="feedback-text-error"></textarea><div id="feedback-text-error" class="text-error form-text text-danger small invisible" role="alert">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.error.js"))}</div></div></div><div class="DTE_Field form-group row"><label class="col-sm-4 col-form-label">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.files.js"))}</label><div class="col-sm-7"><div id="feedback-upload" class="drop-zone-box dropzone"></div></div></div><div class="DTE_Field form-group row"><label class="col-sm-4 col-form-label" for="feedback-group-anonymous">${WJ.escapeHtml(WJ.translate("admin.welcome.feedback.dialog.send_anonym.js"))}</label><div class="col-sm-7"><input id="feedback-group-anonymous" type="checkbox" class="form-check-input"></div></div></div></div><div class="modal-footer"><div class="DTE_Form_Buttons"><button type="button" class="btn btn-outline-secondary btn-close-editor"><i class="ti ti-x" aria-hidden="true"></i> ${WJ.escapeHtml(WJ.translate("button.cancel"))}</button><button type="submit" class="btn btn-primary"><i class="ti ti-check" aria-hidden="true"></i> ${WJ.escapeHtml(WJ.translate("button.send"))}</button></div></div></form></div></div>`;
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
        const modal = new bootstrap.Modal(modalElement, { backdrop: "static" });
        const fileKeys = [];
        try { window.AdminUpload({ element: "#feedback-upload", destinationFolder: "/files/protected/feedback-form/", writeDirectlyToDestination: false }); } catch (error) { console.warn("Feedback upload initialization failed", error); }
        const uploadListener = event => fileKeys.push(event.detail.key);
        window.addEventListener("WJ.AdminUpload.success", uploadListener);
        this._feedbackListeners.push(["WJ.AdminUpload.success", uploadListener]);
        modalElement.addEventListener("hidden.bs.modal", () => {
            window.removeEventListener("WJ.AdminUpload.success", uploadListener);
            this._feedbackListeners = this._feedbackListeners.filter(([, listener]) => listener !== uploadListener);
            modal.dispose();
            modalElement.remove();
            if (trigger?.isConnected) trigger.focus();
        }, { once: true });
        const close = () => modal.hide();
        const textarea = modalElement.querySelector("#feedback-group-text");
        const error = modalElement.querySelector(".text-error");
        modalElement.addEventListener("shown.bs.modal", () => textarea.focus(), { once: true });
        textarea.addEventListener("input", () => {
            textarea.removeAttribute("aria-invalid");
            error.classList.add("invisible");
        });
        modalElement.querySelector(".btn-close-editor").addEventListener("click", close);
        modalElement.querySelector("form").addEventListener("submit", event => {
            event.preventDefault();
            if (!textarea.value.trim()) {
                error.classList.remove("invisible");
                textarea.setAttribute("aria-invalid", "true");
                textarea.focus();
                return;
            }
            $.ajax({ type: "POST", url: "/admin/rest/feedback", data: { data: { text: textarea.value, fileKeys, isAnonymous: modalElement.querySelector("#feedback-group-anonymous").checked } }, success: result => result === "OK" ? WJ.notifySuccess(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.success_notify.js"), 20000) : WJ.notifyError(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.error_notify.js"), 60000), error: () => WJ.notifyError(WJ.translate("admin.welcome.feedback.title.js"), WJ.translate("admin.welcome.feedback.dialog.error_notify.js"), 60000) });
            close();
        });
        modal.show();
    }


}

if (!customElements.get("webjet-overview-dashboard")) customElements.define("webjet-overview-dashboard", WebjetOverviewDashboardElement);
