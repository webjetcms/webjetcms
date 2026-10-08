import { DashboardController } from '../dashboard/dashboard';
import { DashboardNotices } from '../dashboard/notices';
import { showActiveSessions } from '../dashboard/session-widgets';
import { showDeviceSecurity } from '../dashboard/device-security-dialog';
import { confirmSecurityEvent } from '../dashboard/security-events';
import { registerDashboardWidgets, getDashboardDefaults } from '../dashboard/widgets';
import { showFeedbackDialog } from '../feedback';

/**
 * Configuration for the administration overview dashboard.
 *
 * @typedef {Object} WebjetOverviewDashboardOptions
 * @property {Object} [data={}] - Lightweight bootstrap context; widgets load uncached projections independently.
 * @property {Object[]} [data.dashboardMenu=[]] - Authorized administration navigation for shortcut selection.
 * @property {Object} data.settings - Current account's layout and active-domain preferences.
 * @property {Object[]} data.notices - System notices ready for immediate rendering.
 * @property {boolean} [data.securityEventRequested=false] - Whether an email link requested a login detail.
 * @property {Object|null} [data.requestedSecurityEvent] - Owned login detail, or null when unavailable.
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
    }

    connectedCallback() {
        if (!this._configured) return;
        this.render();
    }

    disconnectedCallback() {
        this.deviceConfirmationRequest?.abort();
        this.dashboardController?.destroy();
        this.noticeController?.destroy();
        this.noticeController = null;
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
        if (this.data.securityEventRequested && !this.securityEventOpened) {
            this.securityEventOpened = true;
            const url = new URL(window.location.href);
            const confirmationToken = url.searchParams.get('deviceConfirmation');
            if (confirmationToken) {
                url.searchParams.delete('deviceConfirmation');
                window.history.replaceState(window.history.state, '', url.pathname + url.search + url.hash);
            }
            const sessionContext = this.dashboardController._widgetContext();
            const securityEvent = this.data.requestedSecurityEvent || null;
            if (confirmationToken && securityEvent) {
                const signal = (this.deviceConfirmationRequest = new AbortController()).signal;
                // Consume the email proof with an authenticated POST without opening the session dialog.
                confirmSecurityEvent(this.data, securityEvent.id, signal, { token: confirmationToken })
                    .then(() => {
                        if (signal.aborted) return;
                        this.dashboardController.refreshSessions();
                        WJ.notifySuccess(sessionContext.translate('sessions'), sessionContext.translate('admin.dashboard.newDevice.confirmed.js'), 10000);
                    })
                    .catch(() => {
                        if (!signal.aborted) WJ.notifyError(sessionContext.translate('sessions'), sessionContext.translate('admin.dashboard.newDevice.linkInvalid.js'), 10000);
                    });
            } else showDeviceSecurity(sessionContext, securityEvent);
        }
        this.dataset.ready = "true";
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    /** Renders the system notices supplied by the dashboard page. */
    _renderNotices() {
        this.noticeController ||= new DashboardNotices(this.dashboardController.notices, this.data,
            () => showActiveSessions(this.dashboardController._widgetContext()),
            event => showDeviceSecurity(this.dashboardController._widgetContext(), event, true));
        this.noticeController.render();
    }

    /** Opens the shared administration feedback dialog. */
    showFeedbackModal() {
        showFeedbackDialog();
    }


}

if (!customElements.get("webjet-overview-dashboard")) customElements.define("webjet-overview-dashboard", WebjetOverviewDashboardElement);
