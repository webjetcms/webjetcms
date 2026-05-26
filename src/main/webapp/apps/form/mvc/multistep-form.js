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

        // Store submitted field values from previous steps for cross-step visibility conditions
        this.submittedValues = {};

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
     * @returns {Promise<void>} Resolves when the step content is injected.
     */
    async loadStep(formName, stepId) {
        if (!formName || !stepId) {
            console.warn('Missing formName or stepId; skipping load.');
            return;
        }
        const url = `/rest/multistep-form/get-step?form-name=${encodeURIComponent(formName)}&step-id=${encodeURIComponent(stepId)}`;
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
            const visibilityConditions = json.visibilityConditions || {};
            const requirementConditions = json.requirementConditions || {};

            // hide previous errors
            this.hideErrors();

            // inject HTML (exec any inline scripts) using jQuery when available
            const holder = this.wrapper.querySelector('.multistepStepContent');
            if (holder) {
                if (window.$) {
                    $(holder).html(html);
                } else {
                    holder.innerHTML = html;
                }
            }

            // attach submit
            const form = this.wrapper.querySelector('.multistepStepContent > form');
            if (form) form.addEventListener('submit', async (event) => { await this.doValidationAndSave(event); });

            // Initialize conditional field visibility from server-provided map
            this._initConditionalVisibility(visibilityConditions);

            // Initialize conditional field requirement from server-provided map
            this._initConditionalRequirement(requirementConditions);

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
            // Skip fields hidden by visibility conditions
            if (this._isFieldHidden(el.closest('.form-group') || el.parentElement)) return;
            if (el.type === 'checkbox' || el.type === 'radio') {
                if (!el.checked) return;
            }
            const value = el.value;
            if (Object.hasOwn(result, el.name)) {
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
                    const holder = this.wrapper.querySelector('.multistepStepContent');
                    if (holder) holder.innerHTML = '';
                }
                await this.showGlobalErr(parsed);
            } else {
                // Store submitted values for cross-step visibility conditions
                Object.assign(this.submittedValues, result);
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
     * Resolve a field wrapper (.form-group) by itemFormId.
     * Supports standard inputs as well as label-only rows rendered without inputs.
     * @param {string} itemFormId - Form item identifier.
     * @returns {HTMLElement|null} Matched field wrapper or null.
     */
    _resolveFieldWrapper(itemFormId) {
        if (!itemFormId) return null;

        const input = this.wrapper.querySelector(`[name="${itemFormId}"], [id="${itemFormId}"]`);
        if (input) return input.closest('.form-group') || input.parentElement;

        const label = this.wrapper.querySelector(`label[for="${itemFormId}"]`);
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
                const referencedElements = this.wrapper.querySelectorAll(
                    `[name="${fieldId}"], [id="${fieldId}"]`
                );

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
            } else {
                // Use joinOperator from the PREVIOUS condition (postfix operator)
                if (prevJoinOperator === 'OR') {
                    combinedResult = combinedResult || met;
                } else {
                    combinedResult = combinedResult && met;
                }
            }
            prevJoinOperator = cond.joinOperator || 'AND';
        }

        const allMet = combinedResult !== null ? combinedResult : true;

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

                const referencedElements = this.wrapper.querySelectorAll(
                    `[name="${fieldId}"], [id="${fieldId}"]`
                );

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
            } else {
                // Use joinOperator from the PREVIOUS condition (postfix operator)
                if (prevJoinOperator === 'OR') {
                    combinedResult = combinedResult || met;
                } else {
                    combinedResult = combinedResult && met;
                }
            }
            prevJoinOperator = cond.joinOperator || 'AND';
        }

        let allMet = combinedResult !== null ? combinedResult : true;

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
                input.setAttribute('data-requirement-required', 'true');
            } else {
                input.removeAttribute('data-requirement-required');
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
                const popoverLink = label.querySelector('i.popover-link');
                if (!existingMark) {
                    if (popoverLink) {
                        label.insertBefore(mark, popoverLink);
                    } else {
                        label.appendChild(mark);
                    }
                } else if (popoverLink && mark.nextSibling !== popoverLink) {
                    label.insertBefore(mark, popoverLink);
                } else if (!popoverLink && label.lastChild !== mark) {
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
        // Check radio buttons first
        const radios = this.wrapper.querySelectorAll(`input[type="radio"][name="${fieldId}"]`);
        if (radios.length > 0) {
            for (const radio of radios) {
                if (radio.checked) return radio.value;
            }
            return '';
        }

        // Check checkboxes
        const checkboxes = this.wrapper.querySelectorAll(`input[type="checkbox"][name="${fieldId}"]`);
        if (checkboxes.length > 0) {
            const checked = [];
            checkboxes.forEach(cb => { if (cb.checked) checked.push(cb.value); });
            return checked.join(',');
        }

        // Standard input/select/textarea
        const el = this.wrapper.querySelector(`[name="${fieldId}"], [id="${fieldId}"]`);
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
                const holder = this.wrapper.querySelector('.multistepStepContent');
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
