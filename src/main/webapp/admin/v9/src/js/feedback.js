let feedback;
const prefix = 'admin.welcome.feedback.dialog.';
const text = key => WJ.translate(prefix + key + '.js');
const html = key => WJ.escapeHtml(text(key));

/**
 * Opens feedback on any administration page and retains the draft when closed.
 * The future Help menu can pass a callback for its back button.
 *
 * @param {Object} [options={}] - Invocation options.
 * @param {Function} [options.onBack] - Restores the invoking Help menu after closing.
 */
export function showFeedbackDialog({ onBack } = {}) {
    feedback ||= new FeedbackDialog();
    feedback.open(onBack);
}

class FeedbackDialog {
    constructor() {
        this.files = new Map();
        this.screenshot = null;
        this.screenshotKey = null;
        this.sending = false;
        this.failed = false;
        this.element = document.createElement('div');
        this.element.id = 'feedback_modal';
        this.element.className = 'modal fade md-feedback';
        this.element.tabIndex = -1;
        this.element.setAttribute('aria-labelledby', 'feedback-modal-title');
        this.element.innerHTML = `
            <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable"><div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="btn md-feedback__back" aria-label="${html('back')}" hidden><i class="ti ti-arrow-back-up" aria-hidden="true"></i></button>
                    <h2 id="feedback-modal-title" class="modal-title fs-5">${WJ.escapeHtml(WJ.translate('admin.welcome.feedback.title.js'))}</h2>
                    <button type="button" class="btn md-feedback__close" aria-label="${WJ.escapeHtml(WJ.translate('button.close'))}"><i class="ti ti-x" aria-hidden="true"></i></button>
                </div>
                <form>
                    <div class="modal-body">
                        <fieldset class="md-feedback__fields">
                            <legend class="visually-hidden">${html('type')}</legend>
                            <div class="md-feedback__types">
                                ${[['idea', 'bulb'], ['problem', 'bug'], ['praise', 'heart']].map(([type, icon], index) => `
                                    <input class="btn-check" type="radio" name="feedback-type" id="feedback-type-${type}" value="${type}" ${index === 0 ? 'checked' : ''}>
                                    <label class="btn" for="feedback-type-${type}"><i class="ti ti-${icon}" aria-hidden="true"></i> ${html(type)}</label>`).join('')}
                            </div>
                            <label class="visually-hidden" for="feedback-group-text">${html('feedback_text')}</label>
                            <div class="md-feedback__editor">
                                <div id="feedback-toolbar">
                                    ${[['bold', 'bold'], ['italic', 'italic'], ['underline', 'underline'], ['list', 'listNumbers'], ['list', 'listBullets'], ['link', 'link']].map(([format, label], index) => `<button type="button" class="ql-${format}" ${format === 'list' ? `value="${index === 3 ? 'ordered' : 'bullet'}"` : ''} aria-label="${html('toolbar.' + label)}" title="${html('toolbar.' + label)}"></button>`).join('')}
                                </div>
                                <div id="feedback-group-text"></div>
                            </div>
                            <div class="md-feedback__screenshot-row" ${typeof navigator.mediaDevices?.getDisplayMedia === 'function' ? '' : 'hidden'}>
                                <div class="form-check"><input id="feedback-screenshot" type="checkbox" class="form-check-input"><label class="form-check-label" for="feedback-screenshot">${html('screenshot')}</label></div>
                                <img class="md-feedback__preview" alt="${html('screenshotPreview')}" hidden>
                            </div>
                            <p class="md-feedback__capture-error text-danger small" role="alert" hidden>${html('screenshotError')}</p>
                            <div class="md-feedback__upload">
                                <label class="form-label">${html('files')}</label>
                                <div id="feedback-upload" class="drop-zone-box dropzone"></div>
                                <div id="upload-wrapper" class="upload-wrapper" style="display:none">
                                    <div class="toast-container-progress"><span>${html('uploaded_files')}</span>${this.progressMarkup()}</div>
                                    <div id="toast-container-upload" class="toast-container-upload"></div>
                                </div>
                                <div id="upload-toastr-template" class="upload-toastr-template" style="display:none"><i class="ti ti-polaroid" aria-hidden="true"></i><span>{FILE_NAME}</span><i class="ti ti-circle-check float-end" aria-hidden="true"></i><i class="ti ti-alert-triangle float-end" aria-hidden="true"></i><i class="ti ti-loader-2 ti-spin float-end" aria-hidden="true"></i><i class="ti ti-alert-circle float-end" aria-hidden="true"></i>${this.progressMarkup()}<div class="toast-error-message"></div></div>
                            </div>
                            <div class="form-check md-feedback__anonymous"><input id="feedback-group-anonymous" type="checkbox" class="form-check-input"><label class="form-check-label" for="feedback-group-anonymous">${html('send_anonym')}</label></div>
                            <p class="md-feedback__context small text-muted"><i class="ti ti-info-circle" aria-hidden="true"></i> ${html('context')}</p>
                        </fieldset>
                        <p class="md-feedback__error" role="alert" hidden><i class="ti ti-alert-circle" aria-hidden="true"></i> ${html('sendError')}</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-link md-feedback__cancel">${WJ.escapeHtml(WJ.translate('button.cancel'))}</button>
                        <button type="submit" class="btn btn-primary md-feedback__send" disabled></button>
                    </div>
                </form>
                <div class="md-feedback__success" role="status" hidden>
                    <div class="md-feedback__success-icon"><i class="ti ti-check" aria-hidden="true"></i></div>
                    <h3>${html('successTitle')}</h3><p>${html('successText')}</p>
                    <div class="md-feedback__success-actions"><button type="button" class="btn btn-link md-feedback__another">${html('another')}</button><button type="button" class="btn btn-outline-secondary md-feedback__done">${WJ.escapeHtml(WJ.translate('button.close'))}</button></div>
                </div>
            </div></div>`;
        document.body.append(this.element);
        this.modal = new bootstrap.Modal(this.element);
        this.quill = new Quill(this.find('#feedback-group-text'), {
            theme: 'snow', placeholder: text('placeholder.idea'),
            formats: ['bold', 'italic', 'underline', 'list', 'link'],
            modules: { toolbar: '#feedback-toolbar' }
        });
        this.quill.root.setAttribute('role', 'textbox');
        this.quill.root.setAttribute('aria-label', text('feedback_text'));
        this.quill.root.setAttribute('aria-multiline', 'true');
        this.quill.root.setAttribute('aria-required', 'true');
        this.quill.root.setAttribute('aria-describedby', 'feedback-modal-title');
        this.quill.on('text-change', () => this.updateSend());
        this.uploader = window.AdminUpload({ element: '#feedback-upload', destinationFolder: '/files/protected/feedback-form/', writeDirectlyToDestination: false });
        window.addEventListener('WJ.AdminUpload.success', event => {
            if (event.detail.uploader !== this.uploader) return;
            this.files.set(event.detail.file, event.detail.key);
            this.updateSend();
        });
        this.uploader.on('removedfile', file => {
            const key = this.files.get(file);
            if (key) this.discardUpload(key);
            this.files.delete(file);
            file.toaster?.remove();
            this.updateSend();
        });
        this.uploader.on('addedfile', file => {
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'btn btn-link md-feedback__remove-file';
            remove.setAttribute('aria-label', text('removeFile') + ' ' + file.name);
            remove.innerHTML = '<i class="ti ti-x" aria-hidden="true"></i>';
            remove.addEventListener('click', () => this.uploader.removeFile(file));
            file.toaster?.find('.toast-message').append(remove);
        });
        ['addedfile', 'complete', 'queuecomplete', 'error'].forEach(event => this.uploader.on(event, () => this.updateSend()));
        this.element.addEventListener('change', event => {
            if (event.target.name === 'feedback-type') this.quill.root.dataset.placeholder = text('placeholder.' + event.target.value);
            if (event.target.id === 'feedback-screenshot') {
                if (event.target.checked) this.capture();
                else { this.removeScreenshot(); this.updateSend(); }
            }
        });
        this.find('form').addEventListener('submit', event => { event.preventDefault(); this.submit(); });
        this.find('.md-feedback__cancel').addEventListener('click', () => { this.reset(); this.modal.hide(); });
        this.find('.md-feedback__close').addEventListener('click', () => this.modal.hide());
        this.find('.md-feedback__done').addEventListener('click', () => this.modal.hide());
        this.find('.md-feedback__back').addEventListener('click', () => { this.returnToHelp = true; this.modal.hide(); });
        this.find('.md-feedback__another').addEventListener('click', () => { this.reset(); this.quill.focus(); });
        this.element.addEventListener('hide.bs.modal', event => { if (this.sending || this.capturing) event.preventDefault(); });
        this.element.addEventListener('hidden.bs.modal', () => {
            if (this.trigger?.isConnected) this.trigger.focus();
            if (this.returnToHelp) this.onBack?.();
            this.returnToHelp = false;
        });
        this.element.addEventListener('shown.bs.modal', () => this.sent ? this.find('.md-feedback__another').focus() : this.quill.focus());
        this.updateSend();
    }

    find(selector) { return this.element.querySelector(selector); }

    progressMarkup() {
        return '<svg class="fa-progress-bar float-end" xmlns="http://www.w3.org/2000/svg" viewBox="-1 -1 34 34" aria-hidden="true"><circle cx="16" cy="16" r="15" class="fa-progress-bar__background"></circle><circle cx="16" cy="16" r="15" class="fa-progress-bar__progress" style="stroke-dashoffset:100px"></circle></svg>';
    }

    open(onBack) {
        this.trigger = document.activeElement;
        this.onBack = onBack;
        this.find('.md-feedback__back').hidden = !onBack;
        if (!this.quill.getText().trim() && !this.files.size && !this.sent) this.pageUrl = window.location.href;
        this.modal.show();
    }

    /** Captures one frame from the user-selected display and immediately stops sharing. */
    async capture() {
        if (this.capturing || this.sending) return;
        this.capturing = true;
        this.find('.md-feedback__capture-error').hidden = true;
        this.updateBusy();
        const backdrop = document.querySelector('.modal-backdrop.show');
        const backdropVisibility = backdrop?.style.visibility;
        const video = document.createElement('video');
        let stream;
        this.element.style.visibility = 'hidden';
        if (backdrop) backdrop.style.visibility = 'hidden';
        try {
            stream = await navigator.mediaDevices.getDisplayMedia({
                video: { displaySurface: 'browser' }, audio: false, preferCurrentTab: true,
                systemAudio: 'exclude', windowAudio: 'exclude', surfaceSwitching: 'exclude'
            });
            video.muted = true;
            video.playsInline = true;
            video.srcObject = stream;
            await video.play();
            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            canvas.getContext('2d').drawImage(video, 0, 0);
            const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
            if (!blob) throw new Error('Screenshot could not be encoded');
            this.removeScreenshot();
            this.screenshot = blob;
            this.previewUrl = URL.createObjectURL(blob);
            this.find('.md-feedback__preview').src = this.previewUrl;
            this.find('.md-feedback__preview').hidden = false;
        } catch (error) {
            if (error.name !== 'NotAllowedError' && error.name !== 'AbortError') {
                this.find('.md-feedback__capture-error').hidden = false;
                console.warn('Feedback screenshot failed', error);
            }
        } finally {
            stream?.getTracks().forEach(track => track.stop());
            video.srcObject = null;
            this.element.style.visibility = '';
            if (backdrop) backdrop.style.visibility = backdropVisibility;
            this.capturing = false;
            this.updateBusy();
            this.find('#feedback-screenshot').checked = Boolean(this.screenshot);
            this.find('#feedback-screenshot').focus();
        }
    }

    removeScreenshot() {
        if (this.screenshotKey) this.discardUpload(this.screenshotKey);
        this.screenshotKey = null;
        this.screenshot = null;
        if (this.previewUrl) URL.revokeObjectURL(this.previewUrl);
        this.previewUrl = null;
        this.find('.md-feedback__preview').removeAttribute('src');
        this.find('.md-feedback__preview').hidden = true;
        this.find('#feedback-screenshot').checked = false;
        this.find('.md-feedback__capture-error').hidden = true;
    }

    updateBusy() {
        const busy = this.sending || this.capturing;
        if (busy) this.find('form').setAttribute('aria-busy', 'true');
        else this.find('form').removeAttribute('aria-busy');
        this.find('.md-feedback__fields').disabled = busy;
        if (busy) this.uploader.disable();
        else this.uploader.enable();
        this.quill.enable(!busy);
        this.element.querySelectorAll('.md-feedback__close, .md-feedback__back, .md-feedback__cancel').forEach(button => { button.disabled = busy; });
        this.updateSend();
    }

    updateSend() {
        const uploading = this.uploader?.files.some(file => !this.files.has(file));
        this.find('.md-feedback__send').disabled = this.sending || this.capturing || !this.quill.getText().trim() || uploading;
        const label = this.sending ? 'sending' : this.failed ? 'retry' : 'send';
        this.find('.md-feedback__send').innerHTML = `<i class="ti ti-${this.sending ? 'loader-2 ti-spin' : this.failed ? 'refresh' : 'send'}" aria-hidden="true"></i> ${html(label)}`;
    }

    /** Uploads the screenshot through the existing temporary-file upload endpoint. */
    async uploadScreenshot() {
        const data = new FormData();
        data.append('file', this.screenshot, 'feedback-screenshot.png');
        data.append('destinationFolder', '/files/protected/feedback-form/');
        data.append('writeDirectlyToDestination', 'false');
        const response = await fetch('/admin/upload/chunk', { method: 'POST', headers: { 'X-CSRF-TOKEN': window.csrfToken }, body: data });
        const result = await response.json();
        if (!response.ok || result.error || !result.key) throw new Error('Screenshot upload failed');
        this.screenshotKey = result.key;
    }

    /** Retains the editor and attachments on failure so a retry sends the same content. */
    async submit() {
        if (this.find('.md-feedback__send').disabled) return;
        this.sending = true;
        this.updateBusy();
        this.find('.md-feedback__error').hidden = true;
        this.updateSend();
        try {
            const fileKeys = [...this.files.values()];
            if (this.screenshot) {
                if (!this.screenshotKey) await this.uploadScreenshot();
                fileKeys.push(this.screenshotKey);
            }
            const result = await $.ajax({ type: 'POST', url: '/admin/rest/feedback', data: { data: {
                text: this.quill.getSemanticHTML(), isHtml: true, fileKeys,
                type: this.find('input[name="feedback-type"]:checked').value,
                pageUrl: this.pageUrl, isAnonymous: this.find('#feedback-group-anonymous').checked
            } } });
            if (result !== 'OK') throw new Error('Feedback was not accepted');
            // The server deletes attachments after successful delivery.
            this.files.clear();
            this.screenshotKey = null;
            this.reset();
            this.sent = true;
            this.find('form').hidden = true;
            this.find('.md-feedback__success').hidden = false;
            this.find('.md-feedback__another').focus();
        } catch (error) {
            this.failed = true;
            this.find('.md-feedback__error').hidden = false;
            this.find('.md-feedback__error').scrollIntoView({ block: 'nearest' });
        } finally {
            this.sending = false;
            this.updateBusy();
        }
    }

    discardUpload(key) {
        $.post('/admin/upload/skipkey', { fileKey: key });
    }

    reset() {
        this.sent = false;
        this.failed = false;
        this.removeScreenshot();
        this.find('.md-feedback__error').hidden = true;
        this.uploader.removeAllFiles(true);
        this.find('#toast-container-upload').replaceChildren();
        this.find('#upload-wrapper').style.display = 'none';
        this.find('form').reset();
        this.find('form').hidden = false;
        this.find('.md-feedback__success').hidden = true;
        this.quill.setText('');
        this.quill.history.clear();
        this.quill.root.dataset.placeholder = text('placeholder.idea');
        this.pageUrl = window.location.href;
        this.updateSend();
    }
}
