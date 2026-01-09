// ES module for Multistep Form app
// Exports class `MultistepForm` which encapsulates all logic and DOM rendering

export class MultistepForm {
    constructor(options = {}) {
        // Preferred container selector; defaults to body > article > div.container
        this.mountSelector = options.container || options.mountSelector || 'body > article > div.container';

        this.formName = options.formName || '';
        this.stepId = options.stepId || '';
        this.csrf = options.csrf || '';

        console.log('MultistepForm initialized with options: ', options);

        this._renderShell();
    }

    start() {
        this.loadStep(this.formName, this.stepId);
    }

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
        succP.setAttribute('data-th-text', '#{multistep_form.form_save_succ}');
        success.appendChild(succP);

        // error alert
        const danger = document.createElement('div');
        danger.className = 'alert alert-danger';
        danger.style.display = 'none';
        const errP = document.createElement('p');
        errP.setAttribute('data-th-text', '#{multistep_form.step_saving_err}');
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

    async showGlobalSuccess(message) {
        const successMsg = message || 'Operation completed successfully.';
        const success = this.wrapper.querySelector('div.alert.alert-success');
        if (!success) return;
        success.style.display = '';
        const p = success.querySelector('p');
        if (p) p.innerHTML = `<span>${successMsg}</span>`;
    }

    async showGlobalErr(response) {
        const errorMsg = response.err_msg || 'Unknown error occurred.';
        const danger = this.wrapper.querySelector('div.alert.alert-danger');
        if (!danger) return;
        danger.style.display = '';
        const ul = danger.querySelector('ul');
        if (ul) ul.innerHTML = `<li><span>${errorMsg}</span></li>`;
    }

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
                await this.showGlobalSuccess('#{multistep_form.form_save_succ.js}' || null);
            } else {
                const danger = this.wrapper.querySelector('div.alert.alert-danger');
                if (danger) danger.style.display = 'none';
                await this.loadStep(formName, stepId);
            }
        }
    }
}
