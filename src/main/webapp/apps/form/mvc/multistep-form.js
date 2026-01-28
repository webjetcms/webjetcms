// ES module for Multistep Form app
// Exports class `MultistepForm` which encapsulates all logic and DOM rendering
/**
 * Multistep Form application controller.
 *
 * Renders the shell, loads individual steps from server, submits data,
 * and handles success/error UI states.
 */

export class MultistepForm {
    /**
     * Create a new MultistepForm instance.
     * @param {Object} [options] - Initialization options.
     * @param {string} [options.container] - CSS selector of the mount container (preferred).
     * @param {string} [options.mountSelector] - Alternative mount selector; defaults to Bootstrap container/body.
     * @param {string} [options.formName] - Form identifier used by the backend.
     * @param {string|number} [options.stepId] - Initial step identifier to load.
     * @param {string} [options.csrf] - CSRF token added to POST requests.
     */
    constructor(options = {}) {
        // Preferred container selector; defaults to body > article > div.container
        this.mountSelector = options.container || options.mountSelector || 'body > article > div.container';

        this.formName = options.formName || '';
        this.stepId = options.stepId || '';
        this.csrf = options.csrf || '';

        // Localized messages provided by the page (preferred), with safe fallbacks
        this.successMessage = options.successMessage || 'Operation completed successfully.';
        this.errorMessage = options.errorMessage || 'An error occurred while saving the step.';

        //console.log('MultistepForm initialized with options: ', options);

        this._renderShell();
    }

    /**
     * Start the flow by loading the configured form and step.
     */
    start() { this.loadStep(this.formName, this.stepId); }

    /**
     * Render the application shell (alerts + content holder) and mount it.
     * Creates `this.wrapper` for subsequent DOM operations.
     */
    _renderShell() {
        // Create wrapper that holds everything
        const wrapper = document.createElement('div');
        wrapper.id = 'multistep-form-wrapper-' + this.csrf;
        wrapper.className = 'multistep-form-app';

        // success alert
        const success = document.createElement('div');
        success.className = 'alert alert-success';
        success.style.display = 'none';
        const succP = document.createElement('p');
        // Use provided localized success message
        succP.textContent = this.successMessage;
        success.appendChild(succP);

        // error alert
        const danger = document.createElement('div');
        danger.className = 'alert alert-danger';
        danger.style.display = 'none';
        const errP = document.createElement('p');
        // Use provided localized error message
        errP.textContent = this.errorMessage;
        const errUl = document.createElement('ul');
        errUl.style.margin = '0px';
        danger.appendChild(errP);
        danger.appendChild(errUl);

        // content holder
        const content = document.createElement('div');
        content.id = 'multistepStepContent';

        wrapper.appendChild(success);
        wrapper.appendChild(danger);
        wrapper.appendChild(content);

        // Prefer to mount inside Bootstrap's container if available, fallback to provided root/body
        const container = document.querySelector(this.mountSelector) || document.querySelector('div.container');
        const mountParent = container || document.body;
        mountParent.appendChild(wrapper);
        this.wrapper = wrapper;
    }

    /**
     * Load and render a step's HTML from the backend.
     * @param {string} formName - The form name to query.
     * @param {string|number} stepId - The step identifier to load.
     * @returns {Promise<void>} Resolves when the step content is injected.
     */
    async loadStep(formName, stepId) {
        if (!formName || !stepId) {
            console.warn('Missing formName or stepId; skipping load.');
            return;
        }
        const url = `/rest/multistep-form/get-step?form-name=${encodeURIComponent(formName)}&step-id=${encodeURIComponent(stepId)}`;
        try {
            const r = await fetch(url, { method: 'GET', headers: { 'Accept': 'text/html,application/json' } });
            if (!r.ok) {
                const raw = await r.text();
                try {
                    let parsed;
                    try { parsed = JSON.parse(raw); } catch (_) { parsed = { raw }; }
                    await this.showGlobalErr(parsed);
                } catch (e) { /* ignore */ }
            }
            const html = await r.text();

            // hide previous errors
            this.hideErrors();

            // inject HTML (exec any inline scripts) using jQuery when available
            const holder = this.wrapper.querySelector('#multistepStepContent');
            if (holder) {
                if (window.$) {
                    $(holder).html(html);
                } else {
                    holder.innerHTML = html;
                }
            }

            // attach submit
            const form = this.wrapper.querySelector('#multistepStepContent > form');
            if (form) form.addEventListener('submit', async (event) => { await this.doValidationAndSave(event); });

            // init cleditor if needed
            window.setTimeout(() => {
                if (window.$ && $.fn && $.fn.cleditor) {
                    $(this.wrapper).find("textarea.formsimple-wysiwyg").cleditor({
                        width: '100%',
                        controls: 'bold italic underline bullets numbering outdent indent image link icon size color highlight pastetext',
                        bodyStyle: 'font: 11px  Arial, Helvetica, sans-serif;'
                    });
                }
            }, 100);
        } catch (err) {
            console.warn('Failed to load step:', err);
        }
    }

    /**
     * Validate and submit the current step form via AJAX.
     * Collects all input/select/textarea values and posts JSON to the server.
     * @param {SubmitEvent} event - The submit event from the step form.
     * @returns {Promise<void>} Resolves after handling response actions.
     */
    async doValidationAndSave(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const url = form.action;

        const result = {};
        form.querySelectorAll('input, textarea, select').forEach(el => {
            if (!el.name) return;
            if (el.type === 'checkbox' || el.type === 'radio') {
                if (!el.checked) return;
            }
            const value = el.value;
            if (Object.prototype.hasOwnProperty.call(result, el.name)) {
                if (Array.isArray(result[el.name])) {
                    result[el.name].push(value);
                } else {
                    result[el.name] = [result[el.name], value];
                }
            } else {
                result[el.name] = value;
            }
        });

        try {
            const resp = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    'X-CSRF-Token': this.csrf
                },
                body: JSON.stringify(result)
            });

            const text = await resp.text();
            let parsed;
            try { parsed = JSON.parse(text); } catch (_) { parsed = { raw: text }; }

            if (!resp.ok) {
                const errRedirect = parsed.err_redirect || null;
                if (errRedirect) {
                    window.location.href = errRedirect;
                    return;
                }
                const endTry = parsed.end_try || false;
                if (endTry) {
                    const holder = this.wrapper.querySelector('#multistepStepContent');
                    if (holder) holder.innerHTML = '';
                }
                await this.showGlobalErr(parsed);
            } else {
                await this.postSaveAction(parsed);
            }
        } catch (err) {
            console.error('Network/JS error submitting form', err);
        }
    }

    /**
     * Show a global success alert with a message.
     * @param {string} [message] - Message to display; default is a generic text.
     * @returns {Promise<void>} Resolves after the UI is updated.
     */
    async showGlobalSuccess(message) {
        const successMsg = message || this.successMessage;
        const success = this.wrapper.querySelector('div.alert.alert-success');
        if (!success) return;
        success.style.display = '';
        const p = success.querySelector('p');
        if (p) p.innerHTML = `<span>${successMsg}</span>`;
    }

    /**
     * Show a global error alert using a structured response.
     * @param {Object} response - Server response containing error details.
     * @param {string} [response.err_msg] - Human-readable error message.
     * @returns {Promise<void>} Resolves after the UI is updated.
     */
    async showGlobalErr(response) {
        const errorMsg = response.err_msg || this.errorMessage;
        const danger = this.wrapper.querySelector('div.alert.alert-danger');
        if (!danger) return;
        danger.style.display = '';
        const p = danger.querySelector('p');
        if (p) p.textContent = this.errorMessage;
        const ul = danger.querySelector('ul');
        if (ul) ul.innerHTML = `<li><span>${errorMsg}</span></li>`;
    }

    /**
     * Hide and clear any field/global error messages in the UI.
     */
    hideErrors() {
        if (window.$) {
            $(this.wrapper).find('div.cs-error').text('');
        }
        const danger = this.wrapper.querySelector('div.alert.alert-danger');
        if (danger) {
            const ul = danger.querySelector('ul');
            if (ul) ul.innerHTML = '';
            danger.style.display = 'none';
        }
    }

    /**
     * Handle server response after a successful POST.
     * Can redirect, render field errors, or load the next step.
     * @param {Object} response - Parsed JSON from the server.
     * @param {string} [response.forward] - URL to redirect to on success.
     * @param {Object<string,string>} [response.fieldErrors] - Map of fieldName to error message(s).
     * @param {string} [response['form-name']] - Current form name.
     * @param {string|number} [response['step-id']] - Next step id; -1 means finished.
     * @returns {Promise<void>} Resolves after performing the required action.
     */
    async postSaveAction(response) {
        this.hideErrors();

        //debugger;

        const forwardUrl = response.forward;
        if (forwardUrl) {
            window.location.href = forwardUrl;
            return;
        }

        const fieldErrors = response.fieldErrors || {};
        if (fieldErrors && Object.keys(fieldErrors).length > 0) {
            for (const [fieldName, errorMsg] of Object.entries(fieldErrors)) {
                if (window.$) {
                    const errDiv = $(this.wrapper).find('div.cs-error-' + fieldName);
                    const errorMsgArr = String(errorMsg).split('\n');
                    errDiv.html('');
                    let html = "<ul style='margin:0px; padding-left: 20px;'>";
                    for (const msg of errorMsgArr) html += `<li>${msg}</li>`;
                    html += '</ul>';
                    errDiv.html(html);
                }
            }
            return;
        }

        const formName = response['form-name'];
        const stepId = response['step-id'];
        if (formName && (stepId !== undefined && stepId !== null)) {
            if (stepId === -1 || stepId === '-1') {
                const holder = this.wrapper.querySelector('#multistepStepContent');
                if (holder) holder.remove();
                await this.showGlobalSuccess();
            } else {
                const danger = this.wrapper.querySelector('div.alert.alert-danger');
                if (danger) danger.style.display = 'none';
                await this.loadStep(formName, stepId);
            }
        }
    }
}
