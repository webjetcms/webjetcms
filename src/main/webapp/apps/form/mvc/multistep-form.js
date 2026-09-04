let autocompleteAssetsPromise;

/**
 * Details emitted with the `WJ.multistepForm.stepShown` window event.
 *
 * @typedef {Object} MultistepFormStepShownDetail
 * @property {HTMLElement} wrapper - Root element of the multistep form instance.
 * @property {HTMLElement} stepElement - Element containing the rendered step.
 * @property {HTMLFormElement|null} form - Form element rendered for the step.
 * @property {string} formName - Backend form identifier.
 * @property {string|number} stepId - Identifier of the rendered step.
 * @property {string} language - Language used to render the step.
 * @property {string} domIdPrefix - Instance-specific prefix used by form controls.
 * @property {boolean} isInitialStep - Whether this is the first step shown by the controller instance.
 */

/**
 * Multistep Form application controller.
 *
 * Renders the shell, loads individual steps from server, submits data,
 * and handles success/error UI states. After rendering each step it dispatches
 * `WJ.multistepForm.stepShown` as a non-cancelable `CustomEvent` on `window`.
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
     * @param {string} [options.language] - Language code appended to backend requests.
     */
    constructor(options = {}) {
        // Preferred container selector; defaults to body > article > div.container
        this.mountSelector = options.container || options.mountSelector || 'body > article > div.container';

        this.formName = options.formName || '';
        this.stepId = options.stepId || '';
        this.csrf = options.csrf || '';
        this.language = options.language || '';

        // Localized messages provided by the page (preferred), with safe fallbacks
        this.successMessage = options.successMessage || 'Operation completed successfully.';
        this.errorMessage = options.errorMessage || 'An error occurred while saving the step.';

        // Store submitted field values from previous steps for cross-step visibility conditions
        this.submittedValues = {};

        // Maps logical form item IDs used by metadata/data to instance-specific DOM IDs.
        this.domIdPrefix = '';

        this._hasShownStep = false;

        // Centralized map: element -> array of conditions (parsed once from data-visibility-condition attributes)
        this.visibilityConditions = new Map();

        // Centralized map: element -> array of conditions for conditional requirement
        this.requirementConditions = new Map();

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
        const wrapper = document.getElementById('multistep-form-wrapper-' + this.csrf);
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
        content.className = 'multistepStepContent';

        wrapper.appendChild(success);
        wrapper.appendChild(danger);
        wrapper.appendChild(content);

        this.wrapper = wrapper;
    }

    /**
     * Load and render a step's HTML from the backend.
     * @param {string} formName - The form name to query.
     * @param {string|number} stepId - The step identifier to load.
     * @param {boolean} [scrollToForm=false] - Whether to scroll to the newly rendered form.
     * @returns {Promise<void>} Resolves when the step content is injected and the step-shown event is dispatched.
     */
    async loadStep(formName, stepId, scrollToForm = false) {
        if (!formName || !stepId) {
            console.warn('Missing formName or stepId; skipping load.');
            return;
        }
        const url = `/rest/multistep-form/get-step?form-name=${encodeURIComponent(formName)}&step-id=${encodeURIComponent(stepId)}&language=${encodeURIComponent(this.language || '')}`;
        try {
            const r = await fetch(url, { method: 'GET', headers: { 'Accept': 'application/json', "X-CSRF-Token": this.csrf } });
            if (!r.ok) {
                const raw = await r.text();
                try {
                    let parsed;
                    try { parsed = JSON.parse(raw); } catch (_) { parsed = { raw }; }
                    await this.showGlobalErr(parsed);
                } catch (e) { /* ignore */ }
                return;
            }
            const json = await r.json();
            const html = json.html || '';
            this.domIdPrefix = typeof json.domIdPrefix === 'string' ? json.domIdPrefix : '';
            const visibilityConditions = json.visibilityConditions || {};
            const requirementConditions = json.requirementConditions || {};

            // hide previous errors
            this.hideErrors();

            // inject HTML (exec any inline scripts) using jQuery when available
            const holder = this.wrapper.querySelector('.multistepStepContent');
            if (holder) {
                formTooltip.dispose(holder);
                if (window.$) {
                    $(holder).html(html);
                } else {
                    holder.innerHTML = html;
                }

                formTooltip.init(holder);
            }

            // attach submit
            const form = this.wrapper.querySelector('.multistepStepContent > form');
            if (form) form.addEventListener('submit', async (event) => { await this.doValidationAndSave(event); });

            // Initialize remote autocomplete inputs rendered in this step
            this._initAutocompleteFields();

            // Initialize conditional field visibility from server-provided map
            this._initConditionalVisibility(visibilityConditions);

            // Initialize conditional field requirement from server-provided map
            this._initConditionalRequirement(requirementConditions);

            const isInitialStep = this._hasShownStep === false;
            this.formName = formName;
            this.stepId = stepId;
            this._hasShownStep = true;
            if (holder) this._dispatchStepShown(holder, form, isInitialStep);

            if (scrollToForm && form) {
                form.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }

            // init cleditor if needed
            window.setTimeout(() => {
                if (window.$ && $.fn?.cleditor) {
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
     * Notify public-page integrations that a form step is available in the DOM.
     *
     * @param {HTMLElement} stepElement - Element containing the rendered step.
     * @param {HTMLFormElement|null} form - Form element rendered for the step.
     * @param {boolean} isInitialStep - Whether this is the first rendered step.
     */
    _dispatchStepShown(stepElement, form, isInitialStep) {
        const detail = {
            wrapper: this.wrapper,
            stepElement,
            form,
            formName: this.formName,
            stepId: this.stepId,
            language: this.language,
            domIdPrefix: this.domIdPrefix,
            isInitialStep
        };

        window.dispatchEvent(new CustomEvent('WJ.multistepForm.stepShown', { detail }));
    }

    /**
     * Bind remote autocomplete inputs using the standard data-ac-* attributes.
     * The jQuery UI JavaScript dependency is loaded only when the current step contains such a field.
     */
    async _initAutocompleteFields() {
        const inputs = Array.from(this.wrapper.querySelectorAll('input.form-control[data-ac-url]'));
        if (inputs.length === 0) return;

        try {
            await this._ensureAutocompleteAssets();
        } catch (error) {
            console.warn('Failed to initialize autocomplete:', error);
            return;
        }

        inputs.forEach(input => {
            if (input.dataset.acInitialized === 'true' || input.isConnected === false || this.wrapper.contains(input) === false) return;

            const $input = $(input);
            const minLength = Number.parseInt(input.dataset.acMinLength || '1', 10);
            const maxRows = Number.parseInt(input.dataset.acMaxRows || '30', 10);
            const openOnFocus = input.dataset.acSelect === 'true';
            const sourceUrl = input.dataset.acUrl;

            $input.autocomplete({
                appendTo: this.wrapper,
                source: (request, response) => {
                    const url = new URL(sourceUrl, window.location.origin);
                    url.searchParams.set('term', request.term === '*' ? '%' : request.term);

                    fetch(url.toString(), {
                        method: 'GET',
                        headers: {
                            'Accept': 'application/json',
                            'X-CSRF-Token': this.csrf
                        }
                    })
                    .then(result => result.ok ? result.json() : [])
                    .then(options => response(Array.isArray(options) ? options.slice(0, maxRows) : []))
                    .catch(() => response([]));
                },
                delay: 800,
                minLength: Number.isNaN(minLength) ? 1 : minLength,
                open: () => $input.autocomplete('widget').outerWidth($input.outerWidth()),
                position: { my: 'left top+2', at: 'left bottom', collision: 'flipfit' }
            });

            const autocompleteInstance = $input.autocomplete('instance');
            autocompleteInstance.liveRegion.appendTo(this.wrapper);

            input.dataset.acInitialized = 'true';

            if (openOnFocus) {
                $input.on('focus.multistepAutocomplete', () => {
                    window.setTimeout(() => $input.autocomplete('search', '*'), 50);
                });
            }
        });
    }

    /**
     * Load jQuery UI autocomplete once per page.
     * @returns {Promise<void>} resolves when autocomplete is available
     */
    _ensureAutocompleteAssets() {
        if (window.$ && typeof $.fn?.autocomplete === 'function') return Promise.resolve();
        if (!window.$) return Promise.reject(new Error('jQuery is not available.'));

        if (!autocompleteAssetsPromise) {
            autocompleteAssetsPromise = new Promise((resolve, reject) => {
                const script = document.createElement('script');
                script.src = '/components/_common/javascript/jqui/jquery-ui-core.min.js';
                script.dataset.multistepAutocomplete = 'true';
                script.onload = () => typeof $.fn?.autocomplete === 'function' ? resolve() : reject(new Error('Autocomplete is not available.'));
                script.onerror = () => reject(new Error('Could not load autocomplete assets.'));
                document.head.appendChild(script);
            });
        }

        return autocompleteAssetsPromise;
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

        // Generate reCaptcha V3 token if the captcha widget is present
        const recaptchaInput = form.querySelector('input[name="g-recaptcha-response"][data-type="V3"]');
        if (recaptchaInput && window.grecaptcha && typeof window.wjFormSubmit === 'function') {
            await new Promise((resolve) => {
                window.wjFormSubmit(form, resolve, recaptchaInput);
            });
        }

        const url = new URL(form.getAttribute('action'), window.location.origin);
        url.searchParams.set('language', this.language || '');

        const result = {};
        form.querySelectorAll('input, textarea, select').forEach(el => {

            // Checkbox/radio options of a group share one name but have unique ids
            // (id="${id}-${value}"), so collect them by name to keep grouped values
            // together. Other fields are matched by id after the NAME->ID change, with
            // a name fallback for id-less elements (e.g. the multiupload dropzone inputs).
            const isGrouped = el.type === 'checkbox' || el.type === 'radio';
            const isCaptchaResponse = el.name === 'g-recaptcha-response';
            const domKey = isGrouped || isCaptchaResponse ? (el.name || el.id) : (el.id || el.name);
            if (!domKey) return;
            const key = this._toLogicalFieldId(domKey);
            // Skip fields hidden by visibility conditions
            if (this._isFieldHidden(el.closest('.form-group') || el.parentElement)) return;
            if (el.type === 'checkbox' || el.type === 'radio') {
                if (!el.checked) return;
            }
            const value = el.value;
            if (Object.hasOwn(result, key)) {
                if (Array.isArray(result[key])) {
                    result[key].push(value);
                } else {
                    result[key] = [result[key], value];
                }
            } else {
                result[key] = value;
            }
        });

        try {
            const resp = await fetch(url.toString(), {
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

            if (resp.ok === true) {
                // Store submitted values for cross-step visibility conditions
                Object.assign(this.submittedValues, result);
                await this.postSaveAction(parsed);
            } else {
                const errRedirect = parsed.err_redirect || null;
                if (errRedirect) {
                    window.location.href = errRedirect;
                    return;
                }
                const endTry = parsed.end_try || false;
                if (endTry) {
                    const holder = this.wrapper.querySelector('.multistepStepContent');
                    if (holder) holder.innerHTML = '';
                }
                await this.showGlobalErr(parsed);
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
        danger.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
     * Map a logical form item ID to the ID/name rendered for this form instance.
     * @param {string} logicalId - Unprefixed form item identifier.
     * @returns {string} Instance-specific DOM identifier.
     */
    _toDomFieldId(logicalId) {
        if (!logicalId || !this.domIdPrefix || logicalId.startsWith(this.domIdPrefix)) return logicalId;
        return this.domIdPrefix + logicalId;
    }

    /**
     * Map an instance-specific DOM ID/name back to its logical form item ID.
     * @param {string} domId - Rendered form item identifier.
     * @returns {string} Unprefixed logical identifier.
     */
    _toLogicalFieldId(domId) {
        if (!domId || !this.domIdPrefix || !domId.startsWith(this.domIdPrefix)) return domId;
        return domId.substring(this.domIdPrefix.length);
    }

    /**
     * Find DOM elements belonging to a logical form item ID.
     * @param {string} logicalId - Unprefixed form item identifier.
     * @returns {NodeListOf<Element>} Matching form controls.
     */
    _getFieldElements(logicalId) {
        const domId = this._toDomFieldId(logicalId);
        return this.wrapper.querySelectorAll(`[name="${domId}"], [id="${domId}"]`);
    }

    /**
     * Resolve a field wrapper (.form-group) by itemFormId.
     * Supports standard inputs as well as label-only rows rendered without inputs.
     * @param {string} itemFormId - Form item identifier.
     * @returns {HTMLElement|null} Matched field wrapper or null.
     */
    _resolveFieldWrapper(itemFormId) {
        if (!itemFormId) return null;

        const domId = this._toDomFieldId(itemFormId);
        const input = this.wrapper.querySelector(`[name="${domId}"], [id="${domId}"]`);
        if (input) return input.closest('.form-group') || input.parentElement;

        const label = this.wrapper.querySelector(`label[for="${domId}"]`);
        if (label) return label.closest('.form-group') || label.parentElement;

        return null;
    }

    /**
     * Initialize conditional visibility for fields in the current step.
     * Uses the server-provided visibilityConditions map (itemFormId -> {conditions, hidden})
     * instead of parsing from DOM attributes.
     * Attaches change listeners to referenced fields on the same step.
     * Conditions are AND logic - all must be met for the field to be visible.
     * @param {Object} visibilityConditionsMap - Map of itemFormId to {conditions: [{fieldId, operator, value}], hidden: boolean}
     */
    _initConditionalVisibility(visibilityConditionsMap) {
        // Clear conditions from any previous step
        this.visibilityConditions.clear();

        if (!visibilityConditionsMap || Object.keys(visibilityConditionsMap).length === 0) return;

        // For each entry, find the target wrapper element via the field's name/id
        for (const [itemFormId, entry] of Object.entries(visibilityConditionsMap)) {
            const conditions = entry.conditions;
            if (!Array.isArray(conditions) || conditions.length === 0) continue;

            const field = this._resolveFieldWrapper(itemFormId);
            if (!field) continue;

            // Mark as animation-capable for CSS transitions
            field.classList.add('mf-visibility-animated');

            // Apply server-side hidden state for cross-step conditions
            if (entry.hidden) {
                this._setFieldVisibility(field, false, false);
            }

            this.visibilityConditions.set(field, conditions);
        }

        // Attach listeners and evaluate initial state from the centralized map
        this.visibilityConditions.forEach((conditions, field) => {
            conditions.forEach(cond => {
                const fieldId = cond.fieldId;
                if (!fieldId) return;

                // Find the referenced input/select/textarea on the current step
                const referencedElements = this._getFieldElements(fieldId);

                referencedElements.forEach(el => {
                    const eventType = (el.type === 'checkbox' || el.type === 'radio') ? 'change' : 'input';
                    el.addEventListener(eventType, () => this._evaluateConditions(field, conditions));
                    // Also listen to 'change' for select elements
                    if (el.tagName === 'SELECT') {
                        el.addEventListener('change', () => this._evaluateConditions(field, conditions));
                    }
                });
            });

            // Evaluate conditions initially
            this._evaluateConditions(field, conditions);
        });
    }

    /**
     * Evaluate all conditions for a conditional field and show/hide it.
     * @param {HTMLElement} field - The field wrapper element with data-visibility-condition.
     * @param {Array} conditions - Array of condition objects [{fieldId, operator, value, joinOperator}].
     */
    _evaluateConditions(field, conditions) {
        let combinedResult = null;
        let prevJoinOperator = 'AND';

        for (const cond of conditions) {
            const fieldId = cond.fieldId;
            const operator = cond.operator || 'equals';
            const requiredValue = cond.value || '';
            const caseInsensitive = cond.caseInsensitive === true;
            if (!fieldId) continue;

            // Get the current value of the referenced field
            const currentValue = this._getFieldValue(fieldId);
            const met = this._evaluateOperator(operator, currentValue, requiredValue, caseInsensitive);

            if (combinedResult === null) {
                combinedResult = met;
            } else if (prevJoinOperator === 'OR') {
                // Use joinOperator from the PREVIOUS condition (postfix operator)
                combinedResult = combinedResult || met;
            } else {
                combinedResult = combinedResult && met;
            }
            prevJoinOperator = cond.joinOperator || 'AND';
        }

        const allMet = combinedResult == null ? true : combinedResult;

        this._setFieldVisibility(field, allMet, true);

        // Re-evaluate requirement conditions since visibility may affect them
        this.requirementConditions.forEach((reqConds, reqField) => {
            this._evaluateRequirementConditions(reqField, reqConds);
        });
    }

    /**
     * Initialize conditional requirement for fields in the current step.
     * Uses the server-provided requirementConditions map (itemFormId -> {conditions, required}).
     * @param {Object} requirementConditionsMap - Map of itemFormId to {conditions: [{fieldId, operator, value}], required: boolean}
     */
    _initConditionalRequirement(requirementConditionsMap) {
        // Clear conditions from any previous step
        this.requirementConditions.clear();

        if (!requirementConditionsMap || Object.keys(requirementConditionsMap).length === 0) return;

        for (const [itemFormId, entry] of Object.entries(requirementConditionsMap)) {
            const conditions = entry.conditions;
            if (!Array.isArray(conditions) || conditions.length === 0) continue;

            const field = this._resolveFieldWrapper(itemFormId);
            if (!field) continue;

            // Apply server-side requirement state for cross-step conditions
            if (entry.required) {
                this._setFieldRequired(field, true);
            }

            this.requirementConditions.set(field, conditions);
        }

        // Attach listeners and evaluate initial state
        this.requirementConditions.forEach((conditions, field) => {
            conditions.forEach(cond => {
                const fieldId = cond.fieldId;
                if (!fieldId) return;

                const referencedElements = this._getFieldElements(fieldId);

                referencedElements.forEach(el => {
                    const eventType = (el.type === 'checkbox' || el.type === 'radio') ? 'change' : 'input';
                    el.addEventListener(eventType, () => this._evaluateRequirementConditions(field, conditions));
                    if (el.tagName === 'SELECT') {
                        el.addEventListener('change', () => this._evaluateRequirementConditions(field, conditions));
                    }
                });
            });

            // Evaluate conditions initially
            this._evaluateRequirementConditions(field, conditions);
        });
    }

    /**
     * Evaluate all requirement conditions for a field and toggle its required state.
     * @param {HTMLElement} field - The field wrapper element.
     * @param {Array} conditions - Array of condition objects [{fieldId, operator, value, joinOperator}].
     */
    _evaluateRequirementConditions(field, conditions) {
        let combinedResult = null;
        let prevJoinOperator = 'AND';

        for (const cond of conditions) {
            const fieldId = cond.fieldId;
            const operator = cond.operator || 'equals';
            const requiredValue = cond.value || '';
            const caseInsensitive = cond.caseInsensitive === true;
            if (!fieldId) continue;

            const currentValue = this._getFieldValue(fieldId);
            const met = this._evaluateOperator(operator, currentValue, requiredValue, caseInsensitive);

            if (combinedResult === null) {
                combinedResult = met;
            } else if (prevJoinOperator === 'OR') {
                // Use joinOperator from the PREVIOUS condition (postfix operator)
                combinedResult = combinedResult || met;
            } else {
                combinedResult = combinedResult && met;
            }
            prevJoinOperator = cond.joinOperator || 'AND';
        }

        let allMet = combinedResult == null ? true : combinedResult;

        // If field is hidden by visibility conditions, don't make it required
        if (this._isFieldHidden(field)) {
            allMet = false;
        }

        this._setFieldRequired(field, allMet);
    }

    /**
     * Set field visibility with optional fade/scale animation.
     * Hidden fields are marked so they are skipped during submit.
     * @param {HTMLElement} field - The field wrapper element (.form-group).
     * @param {boolean} visible - Whether the field should be visible.
     * @param {boolean} [animate=true] - Whether to animate the transition.
     */
    _setFieldVisibility(field, visible, animate = true) {
        if (!field) return;

        const transitionDurationMs = 260;
        const currentlyHidden = this._isFieldHidden(field);

        // Avoid replaying animations when visibility state has not changed.
        if (visible && !currentlyHidden) return;
        if (!visible && currentlyHidden) return;

        if (field._visibilityTimeoutId) {
            window.clearTimeout(field._visibilityTimeoutId);
            field._visibilityTimeoutId = null;
        }

        if (visible) {
            field.style.display = '';
            field.classList.remove('mf-hide', 'mf-collapsed', 'mf-hidden');

            if (!animate) {
                field.classList.remove('mf-enter', 'mf-collapsed');
                field.style.maxHeight = '';
                return;
            }

            // Start from collapsed state and smoothly expand to content height.
            field.classList.add('mf-collapsed', 'mf-enter');
            field.style.maxHeight = '0px';
            field.getBoundingClientRect();

            const targetHeight = field.scrollHeight;
            field.style.maxHeight = `${targetHeight}px`;

            window.requestAnimationFrame(() => {
                field.classList.remove('mf-collapsed', 'mf-enter');
            });

            field._visibilityTimeoutId = window.setTimeout(() => {
                field.style.maxHeight = '';
                if (field._visibilityTimeoutId) {
                    window.clearTimeout(field._visibilityTimeoutId);
                    field._visibilityTimeoutId = null;
                }
            }, transitionDurationMs);

            return;
        }

        if (!animate || field.style.display === 'none') {
            field.style.display = 'none';
            field.classList.remove('mf-hide', 'mf-enter');
            field.classList.add('mf-collapsed');
            field.style.maxHeight = '0px';
            return;
        }

        // Animate collapse so following fields move up smoothly.
        const currentHeight = field.scrollHeight;
        field.style.maxHeight = `${currentHeight}px`;
        field.getBoundingClientRect();

        field.classList.remove('mf-enter');
        field.classList.add('mf-hide', 'mf-collapsed');
        field.style.maxHeight = '0px';

        const finishHide = () => {
            field.style.display = 'none';
            field.classList.remove('mf-hide');
            field.classList.add('mf-hidden');
            field.style.maxHeight = '0px';
            if (field._visibilityTimeoutId) {
                window.clearTimeout(field._visibilityTimeoutId);
                field._visibilityTimeoutId = null;
            }
        };

        field._visibilityTimeoutId = window.setTimeout(finishHide, transitionDurationMs);
    }

    /**
     * Determine whether a field should be treated as hidden.
     * @param {HTMLElement} field - The field wrapper element.
     * @returns {boolean} true if hidden or in hide transition.
     */
    _isFieldHidden(field) {
        if (!field) return true;
        return field.style.display === 'none' || field.classList.contains('mf-hide') || field.classList.contains('mf-collapsed');
    }

    /**
     * Set or remove the required state on a field's inputs and update the label.
     * @param {HTMLElement} field - The field wrapper element (.form-group).
     * @param {boolean} required - Whether the field should be required.
     */
    _setFieldRequired(field, required) {
        const inputs = field.querySelectorAll('input, select, textarea');
        const label = field.querySelector('label');

        inputs.forEach(input => {
            if (required) {
                input.setAttribute('data-requirement-required', 'true'); //NOSONAR
            } else {
                input.removeAttribute('data-requirement-required'); //NOSONAR
            }
        });

        // Update visual indicator on label
        if (label) {
            const existingMark = label.querySelector('.requirement-mark');
            if (required) {
                const mark = existingMark || document.createElement('span');
                mark.className = 'requirement-mark';
                mark.textContent = ' * ';
                mark.style.color = 'red';
                if (!existingMark) {
                    label.appendChild(mark);
                } else if (label.lastChild !== mark) {
                    label.appendChild(mark);
                }
            } else if (existingMark) {
                existingMark.remove();
            }
        }
    }

    /**
     * Evaluate a single condition operator.
     * @param {string} operator - The operator (equals, not_equals, contains, not_contains, empty, not_empty).
     * @param {string} actualValue - The actual field value.
     * @param {string} requiredValue - The expected/required value.
     * @param {boolean} [caseInsensitive=false] - Whether text comparison should ignore case.
     * @returns {boolean} true if the condition is met.
     */
    _evaluateOperator(operator, actualValue, requiredValue, caseInsensitive = false) {
        if (actualValue == null) actualValue = '';
        if (requiredValue == null) requiredValue = '';

        if (caseInsensitive) {
            actualValue = String(actualValue).toLowerCase();
            requiredValue = String(requiredValue).toLowerCase();
        }

        switch (operator) {
            case 'not_equals':
                return actualValue !== requiredValue;
            case 'contains':
                return actualValue.includes(requiredValue);
            case 'not_contains':
                return !actualValue.includes(requiredValue);
            case 'empty':
                return actualValue === '';
            case 'not_empty':
                return actualValue !== '';
            case 'starts_with':
                return actualValue.startsWith(requiredValue)
            case 'ends_with':
                return actualValue.endsWith(requiredValue)
            case 'equals':
            default:
                return actualValue === requiredValue;
        }
    }

    /**
     * Get the current value of a form field by its name/id.
     * Handles radio buttons, checkboxes, and standard inputs.
     * @param {string} fieldId - The name or id of the field.
     * @returns {string} The current value of the field.
     */
    _getFieldValue(fieldId) {
        const domId = this._toDomFieldId(fieldId);

        // Check radio buttons first (radios share name, IDs are unique per option)
        const radios = this.wrapper.querySelectorAll(`input[type="radio"][name="${domId}"]`);
        if (radios.length > 0) {
            for (const radio of radios) {
                if (radio.checked) return radio.value;
            }
            return '';
        }

        // Check checkboxes (checkboxes share name, IDs are unique per option)
        const checkboxes = this.wrapper.querySelectorAll(`input[type="checkbox"][name="${domId}"]`);
        if (checkboxes.length > 0) {
            const checked = [];
            checkboxes.forEach(cb => { if (cb.checked) checked.push(cb.value); });
            return checked.join(',');
        }

        // Standard input/select/textarea
        const el = this.wrapper.querySelector(`[name="${domId}"], [id="${domId}"]`);
        if (el) return el.value || '';

        // Fallback to stored values from previous steps
        if (Object.hasOwn(this.submittedValues, fieldId)) {
            const val = this.submittedValues[fieldId];
            return Array.isArray(val) ? val.join(',') : (val || '');
        }
        return '';
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

        const forwardUrl = response.forward;
        if (forwardUrl) {
            window.location.href = forwardUrl;
            return;
        }

        const fieldErrors = response.fieldErrors || {};
        if (fieldErrors && Object.keys(fieldErrors).length > 0) {
            for (const [fieldName, errorMsg] of Object.entries(fieldErrors)) {
                if (window.$) {
                    const errDiv = $(this.wrapper).find('div.cs-error-' + this._toDomFieldId(fieldName));
                    const errorMsgArr = String(errorMsg).split('\n');
                    errDiv.html('');
                    let html = "<ul class='mf-error-list'>";
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
                const holder = this.wrapper.querySelector('.multistepStepContent');
                if (holder) holder.remove();
                await this.showGlobalSuccess();
            } else {
                const danger = this.wrapper.querySelector('div.alert.alert-danger');
                if (danger) danger.style.display = 'none';
                await this.loadStep(formName, stepId, true);
            }
        }
    }
}


/**
 * Manages accessible Bootstrap tooltips rendered by multistep forms.
 */
export class FormTooltip {

    static EVENT_NAMESPACE = '.wjFormTooltip';
    static INITIALIZED_ATTRIBUTE = 'data-wj-form-tooltip-initialized';
    static DESCRIPTION_ID_ATTRIBUTE = 'data-wj-form-tooltip-description-id';
    static TRIGGER_SELECTOR = ".popover-link[data-toggle='tooltip'], .popover-link[data-bs-toggle='tooltip']";
    static CONFIG = {
        trigger: 'hover focus',
        html: true,
        delay: { show: 0, hide: 150 }
    };

    constructor() {
        this.adapters = new WeakMap();
        this.tooltipElements = new WeakMap();
        this.activationBindings = new WeakSet();
        this.nativeLifecycleBindings = new WeakSet();
        this.hoverBindings = new WeakSet();
        this.forcedHideTriggers = new WeakSet();
        this.escapeHandlerBound = false;
        this._hideActiveTooltips = this._hideActiveTooltips.bind(this);
    }

    /**
     * Return the native Bootstrap Tooltip class when available.
     * @returns {Function|null} Bootstrap Tooltip class.
     */
    _getNativeTooltipClass() {
        const Tooltip = window.bootstrap?.Tooltip;
        return typeof Tooltip === 'function' ? Tooltip : null;
    }

    /**
     * Return jQuery when its Bootstrap tooltip plugin is available.
     * @returns {jQuery|null} jQuery instance or null when the legacy plugin is unavailable.
     */
    _getLegacyTooltipJQuery() {
        const jQueryInstance = window.jQuery || window.$;
        if (!jQueryInstance || !jQueryInstance.fn || typeof jQueryInstance.fn.tooltip !== 'function') return null;
        return jQueryInstance;
    }

    /**
     * Resolve the Bootstrap tooltip currently associated with a trigger.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     * @returns {HTMLElement|null} Generated tooltip element.
     */
    _getTooltipElement(trigger) {
        const rememberedTooltip = this.tooltipElements.get(trigger);
        if (rememberedTooltip?.isConnected) return rememberedTooltip;
        this.tooltipElements.delete(trigger);

        const stableDescriptionId = trigger.getAttribute(FormTooltip.DESCRIPTION_ID_ATTRIBUTE);
        const tooltipIds = (trigger.getAttribute('aria-describedby') || '').trim().split(/\s+/).filter(Boolean);
        for (const tooltipId of tooltipIds) {
            const tooltip = trigger.ownerDocument.getElementById(tooltipId);
            if (tooltipId !== stableDescriptionId && tooltip?.matches('.tooltip[role="tooltip"]')) return tooltip;
        }
        return null;
    }

    /**
     * Restore the field-specific description that must remain available before and after display.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     */
    _restoreDescription(trigger) {
        const descriptionId = trigger.getAttribute(FormTooltip.DESCRIPTION_ID_ATTRIBUTE);
        if (descriptionId && trigger.ownerDocument.getElementById(descriptionId)) {
            trigger.setAttribute('aria-describedby', descriptionId);
        }
    }

    /**
     * Remember Bootstrap's generated visual tooltip before restoring the stable description relation.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     */
    _captureTooltipElement(trigger) {
        const descriptionId = trigger.getAttribute(FormTooltip.DESCRIPTION_ID_ATTRIBUTE);
        const tooltipIds = (trigger.getAttribute('aria-describedby') || '').trim().split(/\s+/).filter(Boolean);
        for (const tooltipId of tooltipIds) {
            const tooltip = trigger.ownerDocument.getElementById(tooltipId);
            if (tooltipId !== descriptionId && tooltip?.matches('.tooltip[role="tooltip"]')) {
                this.tooltipElements.set(trigger, tooltip);
                break;
            }
        }
        this._restoreDescription(trigger);
    }

    /**
     * Check whether Bootstrap's generated tooltip is currently visible.
     * @param {HTMLElement|null} tooltip - Generated tooltip element.
     * @returns {boolean} True when the tooltip is rendered and visible.
     */
    _isTooltipVisible(tooltip) {
        if (!tooltip?.isConnected) return false;
        const style = tooltip.ownerDocument.defaultView.getComputedStyle(tooltip);
        return style.display !== 'none' && style.visibility !== 'hidden' && parseFloat(style.opacity || '1') > 0;
    }

    /**
     * Dispatch a native mouse transition consumed by both older and newer Bootstrap versions.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     * @param {string} type - Mouse event type.
     * @param {EventTarget|null} relatedTarget - Element related to the transition.
     */
    _dispatchMouseTransition(trigger, type, relatedTarget) {
        const ownerDocument = trigger.ownerDocument;
        const view = ownerDocument.defaultView || window;
        let event;

        try {
            event = new view.MouseEvent(type, {
                bubbles: true,
                cancelable: true,
                relatedTarget
            });
        } catch (error) {
            event = ownerDocument.createEvent('MouseEvents');
            event.initMouseEvent(type, true, true, view, 0, 0, 0, 0, 0, false, false, false, false, 0, relatedTarget);
        }

        trigger.dispatchEvent(event);
    }

    /**
     * Keep an open tooltip visible while the pointer moves from its trigger to its content.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     */
    _makeTooltipHoverable(trigger) {
        const tooltip = this._getTooltipElement(trigger);
        if (!tooltip) return;

        tooltip.classList.add('wj-form-tooltip-hoverable');
        tooltip.style.pointerEvents = 'auto';

        if (this.hoverBindings.has(tooltip)) return;

        tooltip.addEventListener('mouseenter', () => {
            this._dispatchMouseTransition(trigger, 'mouseover', tooltip);
        });
        tooltip.addEventListener('mouseleave', event => {
            this._dispatchMouseTransition(trigger, 'mouseout', event.relatedTarget || tooltip);
        });
        this.hoverBindings.add(tooltip);
    }

    /**
     * Prevent Bootstrap's delayed hide while the pointer is over the tooltip content.
     * @param {Event|jQuery.Event} event - Bootstrap hide event.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     */
    _keepHoveredTooltipVisible(event, trigger) {
        if (this.forcedHideTriggers.has(trigger)) return;

        const tooltip = this._getTooltipElement(trigger);
        if (tooltip?.matches(':hover')) event.preventDefault();
    }

    /**
     * Check whether a key event should activate a non-native button.
     * @param {KeyboardEvent|jQuery.Event} event - Keyboard event.
     * @returns {boolean} True for Enter and Space.
     */
    _isActivationKey(event) {
        return event.key === 'Enter' || event.key === ' ' || event.key === 'Spacebar' || event.which === 13 || event.which === 32;
    }

    /**
     * Create a common adapter for native Bootstrap 5 and legacy jQuery tooltips.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     * @returns {Object|null} Tooltip adapter or null when neither API is available.
     */
    _createAdapter(trigger) {
        const Tooltip = this._getNativeTooltipClass();
        if (Tooltip) {
            const instance = typeof Tooltip.getOrCreateInstance === 'function'
                ? Tooltip.getOrCreateInstance(trigger, FormTooltip.CONFIG)
                : (typeof Tooltip.getInstance === 'function' && Tooltip.getInstance(trigger)) || new Tooltip(trigger, FormTooltip.CONFIG);
            return {
                kind: 'native',
                show: () => instance.show(),
                hide: () => instance.hide(),
                dispose: () => instance.dispose()
            };
        }

        const jQueryInstance = this._getLegacyTooltipJQuery();
        if (!jQueryInstance) return null;

        const $trigger = jQueryInstance(trigger);
        $trigger.tooltip(FormTooltip.CONFIG);
        return {
            kind: 'jquery',
            $trigger,
            show: () => $trigger.tooltip('show'),
            hide: () => $trigger.tooltip('hide'),
            dispose: () => {
                try {
                    $trigger.tooltip('dispose');
                } catch (error) {
                    $trigger.tooltip('destroy');
                }
            }
        };
    }

    /**
     * Activate a tooltip through a non-native key press or a keyboard-generated native button click.
     * @param {KeyboardEvent|MouseEvent} event - Activation event.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     */
    _activate(event, trigger) {
        const isNativeButton = trigger.tagName.toLowerCase() === 'button';
        const tooltip = this._getTooltipElement(trigger);

        if (isNativeButton) {
            if (event.type !== 'click' || event.detail !== 0 || this._isTooltipVisible(tooltip)) return;
            event.preventDefault();
            this.adapters.get(trigger)?.show();
            return;
        }

        if (trigger.getAttribute('role') !== 'button' || !this._isActivationKey(event)) return;

        event.preventDefault();
        if (!this._isTooltipVisible(tooltip)) this.adapters.get(trigger)?.show();
    }

    /**
     * Bind tooltip lifecycle events for either the native or jQuery Bootstrap API.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     * @param {Object} adapter - Tooltip API adapter.
     */
    _bindLifecycle(trigger, adapter) {
        if (!this.activationBindings.has(trigger)) {
            trigger.addEventListener('keydown', event => this._activate(event, trigger));
            if (trigger.tagName.toLowerCase() === 'button') {
                trigger.addEventListener('click', event => this._activate(event, trigger));
            }
            this.activationBindings.add(trigger);
        }

        if (adapter.kind === 'native') {
            if (this.nativeLifecycleBindings.has(trigger)) return;
            const captureAndMakeHoverable = () => {
                this._captureTooltipElement(trigger);
                this._makeTooltipHoverable(trigger);
            };
            const restoreDescription = () => {
                this.tooltipElements.delete(trigger);
                this._restoreDescription(trigger);
            };
            trigger.addEventListener('inserted.bs.tooltip', captureAndMakeHoverable);
            trigger.addEventListener('shown.bs.tooltip', captureAndMakeHoverable);
            trigger.addEventListener('hide.bs.tooltip', event => this._keepHoveredTooltipVisible(event, trigger));
            trigger.addEventListener('hidden.bs.tooltip', restoreDescription);
            this.nativeLifecycleBindings.add(trigger);
            return;
        }

        adapter.$trigger.off(FormTooltip.EVENT_NAMESPACE)
            .on(`inserted.bs.tooltip${FormTooltip.EVENT_NAMESPACE} shown.bs.tooltip${FormTooltip.EVENT_NAMESPACE}`, () => {
                this._captureTooltipElement(trigger);
                this._makeTooltipHoverable(trigger);
            })
            .on(`hide.bs.tooltip${FormTooltip.EVENT_NAMESPACE}`, event => this._keepHoveredTooltipVisible(event, trigger))
            .on(`hidden.bs.tooltip${FormTooltip.EVENT_NAMESPACE}`, () => {
                this.tooltipElements.delete(trigger);
                this._restoreDescription(trigger);
            });
    }

    /**
     * Initialize one form tooltip trigger.
     * @param {HTMLElement} trigger - Tooltip trigger element.
     * @returns {boolean} True when a supported Bootstrap tooltip API is available.
     */
    _initializeTrigger(trigger) {
        this._restoreDescription(trigger);
        const adapter = this._createAdapter(trigger);
        if (!adapter) return false;

        this.adapters.set(trigger, adapter);
        trigger.setAttribute(FormTooltip.INITIALIZED_ATTRIBUTE, 'true');
        trigger.classList.add('wj-form-tooltip-trigger');
        this._bindLifecycle(trigger, adapter);
        this._makeTooltipHoverable(trigger);
        return true;
    }

    /**
     * Dismiss every visible initialized form tooltip when Escape is pressed.
     * @param {KeyboardEvent} event - Captured keyboard event.
     */
    _hideActiveTooltips(event) {
        const isEscape = event.key === 'Escape' || event.key === 'Esc' || event.which === 27;
        if (!isEscape) return;

        const activeTriggers = [];
        document.querySelectorAll(FormTooltip.TRIGGER_SELECTOR).forEach(trigger => {
            if (trigger.getAttribute(FormTooltip.INITIALIZED_ATTRIBUTE) !== 'true') return;
            const tooltip = this._getTooltipElement(trigger);
            if (tooltip?.getAttribute('role') === 'tooltip' && this._isTooltipVisible(tooltip)) activeTriggers.push(trigger);
        });

        if (activeTriggers.length === 0) return;

        event.preventDefault();
        event.stopPropagation();
        activeTriggers.forEach(trigger => {
            this.forcedHideTriggers.add(trigger);
            try {
                this.adapters.get(trigger)?.hide();
            } catch (error) {
                console.warn('Failed to hide form tooltip:', error);
            } finally {
                this.forcedHideTriggers.delete(trigger);
            }
        });
    }

    /**
     * Initialize accessible Bootstrap tooltips below the supplied root.
     * @param {Document|Element|jQuery} [root=document] - Root to scan.
     * @returns {boolean} True when the Bootstrap tooltip plugin is available.
     */
    init(root = document) {
        const rootElement = root?.jquery ? root[0] : root;
        if (!rootElement) return false;

        const triggers = [];
        if (rootElement.matches?.(FormTooltip.TRIGGER_SELECTOR)) triggers.push(rootElement);
        triggers.push(...rootElement.querySelectorAll(FormTooltip.TRIGGER_SELECTOR));

        let tooltipApiAvailable = Boolean(this._getNativeTooltipClass() || this._getLegacyTooltipJQuery());
        triggers.forEach(trigger => {
            try {
                tooltipApiAvailable = this._initializeTrigger(trigger) || tooltipApiAvailable;
            } catch (error) {
                console.warn('Failed to initialize form tooltip:', error);
            }
        });

        if (tooltipApiAvailable && !this.escapeHandlerBound) {
            document.addEventListener('keydown', this._hideActiveTooltips, true);
            this.escapeHandlerBound = true;
        }

        return tooltipApiAvailable;
    }

    /**
     * Dispose initialized Bootstrap tooltip instances below the supplied root before its DOM is replaced.
     * @param {Document|Element|jQuery} [root=document] - Root containing tooltip triggers.
     */
    dispose(root = document) {
        const rootElement = root?.jquery ? root[0] : root;
        if (!rootElement) return;

        const triggers = [];
        if (rootElement.matches?.(FormTooltip.TRIGGER_SELECTOR)) triggers.push(rootElement);
        triggers.push(...rootElement.querySelectorAll(FormTooltip.TRIGGER_SELECTOR));

        triggers.forEach(trigger => {
            const adapter = this.adapters.get(trigger);
            try {
                adapter?.dispose?.();
            } catch (error) {
                console.warn('Failed to dispose form tooltip:', error);
            }
            this.adapters.delete(trigger);
            this.tooltipElements.delete(trigger);
        });
    }
}

export const formTooltip = new FormTooltip();

window.WebJETFormTooltip = window.WebJETFormTooltip || {};
window.WebJETFormTooltip.init = root => formTooltip.init(root);
window.WebJETFormTooltip.dispose = root => formTooltip.dispose(root);
