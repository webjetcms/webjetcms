import $ from 'jquery';
import { AiChatTransport } from './ai-chat-transport';
import { chatRequestId, chatAttachmentUrl, fetchChatAttachment } from './chat-history-api';
import { setChatTooltip, initChatTooltips, disposeChatTooltips } from './chat-tooltips';

/**
 * A reusable AI conversation that keeps its history while its view is unmounted.
 * Hosts can supply request context, composer controls and additional answer or toolbar actions.
 */
export class AiChat {

    constructor(options) {
        this.options = options || {};
        this.assistant = this.options.assistant;
        this.state = this._initialState();
        this.messageViews = new Map();
        this.toolbarView = {};
        this.toolbarEndView = {};
        this.mounted = false;
        this._build();
    }

    _initialState() {
        return {
            messages: [], draft: "", attachments: [], conversationId: null, pending: null, needsNewChat: false,
            threadId: null, revision: null, serverBusy: false, readOnlyReason: null,
            scrollTop: 0, followLatest: true, notice: null, confirmation: null, disposed: false
        };
    }

    _build() {
        this.$root = $('<div class="ai-chat"></div>');
        this.element = this.$root[0];
        this.$toolbar = $('<div class="ai-chat-toolbar"></div>');
        this.$toolbarActions = $('<div class="ai-chat-toolbar-actions"></div>');
        this.$toolbarTitle = $('<strong class="ai-chat-toolbar-title" hidden></strong>');
        this.$toolbarEndActions = $('<div class="ai-chat-toolbar-end-actions"></div>');
        this.$toolbar.append(this.$toolbarActions, this.$toolbarTitle, this.$toolbarEndActions);
        this.$history = $('<div class="ai-chat-history" role="log" aria-live="polite" aria-relevant="additions text" tabindex="0"></div>')
            .attr("aria-label", this._text("conversation"));
        this.$empty = $('<div class="ai-chat-empty"></div>').text(this._text("empty"));
        this.$history.append(this.$empty).on("scroll", () => this._saveScroll());

        const $composer = $('<div class="ai-chat-composer"></div>');
        const $composerPanel = $('<div class="ai-chat-composer-panel"></div>');
        this.$attachments = $('<div class="ai-chat-attachments" role="list"></div>').hide();
        this.attachmentUrls = new Map();
        this.attachmentRequests = new Map();
        this.$notice = $('<div class="ai-chat-notice" role="status"></div>');
        this.$confirmation = $('<div class="ai-chat-confirmation" role="group"></div>').hide();
        this.renderedConfirmation = null;
        this.$input = $('<textarea class="form-control ai-chat-input" rows="2"></textarea>')
            .attr({ "aria-label": this._text("message"), placeholder: this._text("placeholder") });
        this.$input.on("input", () => {
            this.state.draft = this.$input.val();
            this._resizeInput();
            this._updateSendButton();
        });
        this.$input.on("keydown", event => {
            if (event.key === "Enter" && !event.shiftKey && !event.originalEvent?.isComposing && event.keyCode !== 229) {
                event.preventDefault();
                this._sendDraft();
            }
        });
        const $composerActions = $('<div class="ai-chat-composer-actions"></div>');
        if (this.options.showAttachment) {
            this.$fileInput = $('<input type="file" multiple hidden>')
                .attr('accept', '.png,.jpg,.jpeg,.webp,.pdf,.docx,.xlsx,.pptx,.txt,.md,.csv,.tsv,.json,.xml,.html,.htm,.css,.js,.java,.sql,.yaml,.yml,.log')
                .on('change', () => {
                    this._addAttachments(this.$fileInput[0].files);
                    this.$fileInput.val('');
                });
            this.$attachmentButton = this._iconButton(WJ.translate("datatables.upload.uploadFile.js"), "paperclip", "btn-ai-chat-attach")
                .on('click', () => this.$fileInput[0].click());
            $composerActions.append(this.$attachmentButton, this.$fileInput);
            this.$input.on('paste', event => {
                const files = event.originalEvent?.clipboardData?.files;
                if (files?.length && this._canAttach()) {
                    event.preventDefault();
                    this._addAttachments(files);
                }
            });
        }
        this.$composerControls = $('<div class="ai-chat-composer-controls"></div>');
        this.$send = this._iconButton(this._text("send"), "arrow-up", "btn-primary")
            .addClass("btn-ai-chat-send").on("click", () => this._sendDraft());
        this.$stop = this._iconButton(this._text("stop"), "player-stop-filled", "btn-primary")
            .addClass("btn-ai-chat-stop").on("click", () => {
                this.stop();
                this.$input[0].focus({ preventScroll: true });
            });
        $composerActions.append(this.$composerControls, this.$stop, this.$send);
        $composerPanel.append(this.$attachments, this.$input, $composerActions);
        $composer.append(this.$notice, this.$confirmation, $composerPanel);
        this.$root.append(this.$toolbar, this.$history, $composer);
    }

    /** Mounts this conversation in a host element and restores its draft and scroll position. */
    mount(container) {
        if (this.state.disposed) return;
        if (container?.nodeType !== 1) throw new TypeError("AI chat requires a container element.");
        this.unmount();
        container.appendChild(this.element);
        if (this.options.composerControls) this.$composerControls.append(this.options.composerControls);
        this.mounted = true;
        if (this.options.showAttachment) {
            this.$dropTarget = $(this.options.dropTarget || this.element);
            this.$dropTarget.on('dragover.aiChatAttachments', event => {
                if (this._canAttach() && Array.from(event.originalEvent?.dataTransfer?.types || []).includes('Files')) event.preventDefault();
            }).on('drop.aiChatAttachments', event => {
                if (!event.originalEvent?.dataTransfer?.files?.length) return;
                event.preventDefault();
                this._addAttachments(event.originalEvent.dataTransfer.files);
            });
        }
        this.refresh();
        this._resizeInput();
        this.$input[0].focus({ preventScroll: true });
    }

    /** Removes only this component's view; an unfinished response continues into its state. */
    unmount() {
        if (this.mounted) {
            this.state.draft = this.$input.val();
            this._saveScroll();
        }
        this.mounted = false;
        this.$dropTarget?.off('.aiChatAttachments');
        this.$dropTarget = null;
        disposeChatTooltips(this.element);
        this.$root.detach();
    }

    /** Reflects state and host-supplied actions without notifying the host of a state change. */
    refresh() {
        if (!this.mounted || this.state.disposed) return;
        const state = this.state;
        const followLatest = state.followLatest;
        const scrollTop = state.scrollTop;
        const lastMessage = state.messages.at(-1);

        const toolbarActions = this.options.getToolbarActions?.() || [];
        this._renderActions(this.$toolbarActions, this.toolbarView, [
            ...toolbarActions.filter(action => action.position === "start"),
            { id: "new", label: this._text("new"), icon: "plus", run: () => this.newChat() },
            ...toolbarActions.filter(action => action.position !== "start" && action.position !== "end")
        ]);
        this._renderActions(this.$toolbarEndActions, this.toolbarEndView, toolbarActions.filter(action => action.position === "end"));
        const title = this.options.getTitle?.();
        this.$toolbarTitle.text(title || "").prop("hidden", !title);
        setChatTooltip(this.$toolbarTitle[0], title);
        this.$empty.toggle(state.messages.length === 0);
        for (const [message, view] of this.messageViews) {
            if (!state.messages.includes(message)) {
                disposeChatTooltips(view.$article);
                view.$article.remove();
                this.messageViews.delete(message);
            }
        }
        for (const message of state.messages) {
            let view = this.messageViews.get(message);
            if (view == null) {
                view = this._createMessage(message);
                this.messageViews.set(message, view);
                this.$history.append(view.$article);
            }
            view.$label.text(message.role === "user" ? this._text("you") : this.assistant?.providerTitle || this.assistant?.description || "");
            this._updateMessageTime(message, view);
            if (view.text !== message.text) {
                if (message.role === "assistant") this._renderMarkdown(view.$text, message.text || "");
                else view.$text.text(message.text || "");
                view.text = message.text;
            }
            this._renderAttachments(view.$attachments, message.attachments || [], false);
            if (message.role === "assistant") this._updateAssistant(message, view, message === lastMessage);
        }
        this.$history.attr("aria-busy", state.pending != null ? "true" : "false");
        const notice = state.notice || state.readOnlyReason || this.options.notice;
        this.$notice.text(notice || "").toggle(Boolean(notice));
        this.$input.prop('disabled', Boolean(state.readOnlyReason));
        this._updateConfirmation();
        if (this.$input.val() !== state.draft) {
            this.$input.val(state.draft);
            this._resizeInput();
        }
        this._updateSendButton();
        this._renderAttachments(this.$attachments, state.attachments, true);
        this._releaseAttachmentUrls();
        initChatTooltips(this.element);
        if (this.element.isConnected) {
            this.$history[0].scrollTop = followLatest ? this.$history[0].scrollHeight : scrollTop;
            this._saveScroll();
        }
    }

    /** Sends a new prompt, or retries the last failed/stopped answer with its original input. */
    async send(prompt, retryMessage = null) {
        const state = this.state;
        if (state.disposed || state.pending != null || state.needsNewChat || state.serverBusy || state.readOnlyReason
            || typeof prompt !== "string" || (!prompt.trim() && !state.attachments.length)) return;
        if (!prompt.trim()) prompt = this._text('attachmentPrompt');
        if (retryMessage != null && (state.messages.at(-1) !== retryMessage || !this._canRetry(retryMessage))) return;

        let inputData;
        try {
            inputData = retryMessage?.inputData ? { ...retryMessage.inputData } : {
                inputValue: "", inputValueType: "text", outputValueType: "text",
                ...this.options.getInputData?.(prompt), userPrompt: prompt,
                ...(state.attachments.length ? { files: state.attachments.map(item => item.file) } : {})
            };
        } catch (error) {
            state.notice = error.message || this._text("unavailable");
            this._changed();
            return;
        }
        if (state.conversationId) inputData.conversationId = state.conversationId;
        else delete inputData.conversationId;
        if (this.options.persistent) {
            inputData = retryMessage?.inputData?.persistentChat && !retryMessage.confirmed
                ? { ...retryMessage.inputData }
                : { persistentChat: true, threadId: state.threadId, revision: state.revision,
                    requestId: chatRequestId(), userPrompt: prompt, retryMessageId: retryMessage?.id || null,
                    ...(this.options.context ? { context: this.options.context, inputValue: inputData.inputValue } : {}),
                    ...(retryMessage == null && state.attachments.length ? { files: state.attachments.map(item => item.file) } : {}) };
            state.threadId ||= inputData.requestId;
        }

        let message = retryMessage;
        if (message && this.options.persistent && message.confirmed) {
            message = { role: 'assistant', userPrompt: prompt };
            state.messages.push(message);
        }
        if (message == null) {
            state.messages.push({ role: "user", text: prompt, created: Date.now(), attachments: [...state.attachments] });
            message = { role: "assistant", userPrompt: prompt };
            state.messages.push(message);
        }
        Object.assign(message, {
            text: "", status: "pending", created: Date.now(), totalTokens: null, error: null,
            errorCategory: null, providerStatusCode: null, retryable: null, inputData: { ...inputData }
        });
        if (retryMessage == null) {
            state.draft = "";
            state.attachments = [];
        }
        state.notice = null;
        state.followLatest = true;

        const request = { message, transport: null };
        state.pending = request;
        const current = () => !state.disposed && state.pending === request;
        this._changed();
        if (!current()) return;
        try {
            request.transport = new AiChatTransport();
            const result = await request.transport.execute(this.assistant, inputData, {
                onUpdate: text => {
                    if (!current()) return;
                    message.text = text;
                    this.refresh();
                },
                onStatus: text => {
                    if (!current()) return;
                    state.notice = text;
                    this._changed();
                },
                onMetadata: metadata => {
                    if (!current()) return;
                    state.threadId = metadata.threadId;
                    state.revision = metadata.revision;
                    message.id = metadata.messageId;
                    message.turnId = metadata.userMessageId || message.turnId;
                    this.options.onMetadata?.(this);
                },
                onDownload: percent => {
                    if (!current()) return;
                    state.notice = WJ.translate("components.ai_assistants.browser.downloadingModel.js", [percent == null ? "" : percent + "%"]);
                    this._changed();
                },
                onConfirm: (text, buttonText) => this._requestConfirmation(request, text, buttonText)
            });
            if (!current()) return;
            message.confirmed = result.persisted === true;
            state.notice = null;
            message.errorCategory = result.errorCategory;
            message.providerStatusCode = result.providerStatusCode;
            message.retryable = result.retryable;
            if (result.stopped) message.status = "stopped";
            else if (result.success) {
                message.text = result.response ?? message.text;
                message.status = "complete";
                message.totalTokens = result.totalTokens;
                if (Array.isArray(result.attachments)) message.attachments = result.attachments;
                state.conversationId = result.conversationId || null;
            } else {
                message.status = "error";
                message.error = result.errorText || this._text("unavailable");
                state.needsNewChat = !this.options.persistent && result.errorCategory === "INVALID_CONTINUATION";
            }
        } catch (error) {
            if (current()) {
                state.notice = null;
                message.status = "error";
                message.error = error.message || String(error);
            }
        } finally {
            request.transport?.dispose();
            if (current()) {
                state.pending = null;
                state.confirmation?.resolve(false);
                this._changed();
                this.options.onSettled?.(this);
            }
        }
    }

    /** Cancels the current turn while retaining the transcript and last successful context. */
    stop() {
        const request = this.state.pending;
        if (request == null) return;
        this.state.pending = null;
        request.message.status = "stopped";
        this.state.notice = null;
        this.state.confirmation?.resolve(false);
        request.transport?.cancel();
        request.transport?.dispose();
        this._changed();
        this.options.onSettled?.(this);
    }

    /** Clears this conversation in place so hosts can retain a reference to its state. */
    newChat() {
        if (this.state.disposed) return;
        if (this.options.onNewChat) { this.options.onNewChat(this); return; }
        this.stop();
        Object.assign(this.state, this._initialState());
        this.options.onReset?.(this);
        this._changed();
        if (this.mounted) this.$input[0].focus({ preventScroll: true });
    }

    /** Starts fresh after an invalid continuation, keeping the visible transcript and unsent prompt. */
    _restartConversation(message) {
        const state = this.state;
        if (state.disposed || state.pending != null || !state.needsNewChat || state.messages.at(-1) !== message) return;
        state.conversationId = null;
        delete message.inputData.conversationId;
        state.needsNewChat = false;
        if (!state.draft.trim() && !state.attachments.length) {
            state.draft = message.userPrompt;
            const question = state.messages[state.messages.indexOf(message) - 1];
            state.attachments = [...(question?.attachments || [])];
        }
        state.notice = this._text("restarted");
        this._changed();
        if (this.mounted) this.$input[0].focus({ preventScroll: true });
    }

    /** Cancels pending work and permanently removes the component. */
    destroy() {
        if (this.state.disposed) return;
        this.state.disposed = true;
        this.stop();
        this.unmount();
        this.$root.remove();
        this.messageViews.clear();
        Object.assign(this.state, this._initialState(), { disposed: true });
        this._releaseAttachmentUrls();
    }

    _changed() {
        this.options.onChange?.(this);
        this.refresh();
    }

    _requestConfirmation(request, text, buttonText) {
        return new Promise(resolve => {
            if (this.state.disposed || this.state.pending !== request) { resolve(false); return; }
            this.state.confirmation?.resolve(false);
            let settled = false;
            const confirmation = {
                text, buttonText,
                resolve: confirmed => {
                    if (settled) return;
                    settled = true;
                    if (this.state.confirmation === confirmation) {
                        this.state.confirmation = null;
                        this._changed();
                    }
                    resolve(Boolean(confirmed) && !this.state.disposed && this.state.pending === request);
                }
            };
            this.state.confirmation = confirmation;
            this._changed();
        });
    }

    _text(key, ...params) {
        return WJ.translate("components.ai_assistants.chat." + key + ".js", params);
    }

    _button(label, icon, classes = "btn-outline-secondary btn-sm") {
        const $button = $('<button type="button" class="btn"></button>').addClass(classes);
        if (icon) $button.append($('<i aria-hidden="true"></i>').addClass("ti ti-" + icon), document.createTextNode(" "));
        return $button.append($("<span></span>").text(label));
    }

    _sendDraft() {
        this.send(this.state.draft);
        this.$input[0].focus({ preventScroll: true });
    }

    _iconButton(label, icon, classes) {
        const $button = this._button(label, icon, classes).addClass("ai-chat-icon-button");
        $button.find("span").addClass("visually-hidden");
        setChatTooltip($button[0], label);
        return $button;
    }

    _resizeInput() {
        const input = this.$input[0];
        input.style.height = "auto";
        input.style.height = input.scrollHeight + "px";
    }

    _updateSendButton() {
        this.$send.prop("disabled", this.state.pending != null || this.state.serverBusy || this.state.readOnlyReason
            || this.state.needsNewChat || (!this.state.draft.trim() && !this.state.attachments.length)).toggle(this.state.pending == null);
        this.$stop.toggle(this.state.pending != null);
        if (this.$attachmentButton) {
            this.$attachmentButton.prop('disabled', !this._canAttach());
            setChatTooltip(this.$attachmentButton[0], this.options.persistent || this.options.allowAttachments
                ? WJ.translate('datatables.upload.uploadFile.js') : this._text('attachmentsUnavailable'));
        }
        initChatTooltips(this.$send.add(this.$stop).add(this.$attachmentButton));
    }

    _canAttach() {
        return this.options.showAttachment && (this.options.persistent || this.options.allowAttachments) && !this.state.disposed
            && !this.state.pending && !this.state.serverBusy && !this.state.readOnlyReason && !this.state.needsNewChat;
    }

    /** Keeps selected files in this conversation until they are sent, removed or reset. */
    _addAttachments(files) {
        if (!this._canAttach()) return;
        const selected = Array.from(files || []);
        const all = [...this.state.attachments.map(item => item.file), ...selected];
        if (all.length > 5 || all.reduce((total, file) => total + file.size, 0) > 10 * 1024 * 1024) {
            this.state.notice = this._text('attachmentLimit');
        } else if (selected.some(file => !file.size || !/\.(png|jpe?g|webp|pdf|docx|xlsx|pptx|txt|md|csv|tsv|json|xml|html?|css|js|java|sql|ya?ml|log)$/i.test(file.name))) {
            this.state.notice = this._text('attachmentType');
        } else {
            this.state.attachments = [...this.state.attachments, ...selected.map(file => ({ file, name: file.name, size: file.size,
                mediaType: /\.jpe?g$/i.test(file.name) ? 'image/jpeg'
                    : /\.(png|webp)$/i.test(file.name) ? 'image/' + file.name.split('.').at(-1).toLowerCase() : 'application/octet-stream' }))];
            this.state.notice = null;
        }
        this._changed();
    }

    _attachmentUrl(attachment) {
        if (attachment.file) {
            if (!['image/png', 'image/jpeg', 'image/webp'].includes(attachment.mediaType)
                || !/\.(png|jpe?g|webp)$/i.test(attachment.name)) return null;
            if (!this.attachmentUrls.has(attachment.file) && typeof URL.createObjectURL === 'function') {
                this.attachmentUrls.set(attachment.file, URL.createObjectURL(attachment.file.slice(0, attachment.file.size, attachment.mediaType)));
            }
            return this.attachmentUrls.get(attachment.file);
        }
        // Saved attachments are served exclusively by the owner-scoped chat API.
        return chatAttachmentUrl(attachment);
    }

    _renderAttachments($container, attachments, editable) {
        if ($container[0].attachmentItems === attachments && $container[0].attachmentEditable === this._canAttach()) return;
        $container[0].attachmentItems = attachments;
        $container[0].attachmentEditable = this._canAttach();
        disposeChatTooltips($container);
        $container.empty().toggle(attachments.length > 0);
        for (const attachment of attachments) {
            const $item = $('<div class="ai-chat-attachment" role="listitem"></div>');
            const url = this._attachmentUrl(attachment);
            const saved = url && !attachment.file;
            let $label;
            if (saved) {
                $label = $('<button type="button" class="ai-chat-attachment-link"></button>')
                    .on('click', () => this.downloadAttachment(attachment, $label));
            } else {
                $label = url ? $('<a target="_blank" rel="noopener"></a>').attr('href', url) : $('<span></span>');
            }
            if (url && attachment.mediaType?.startsWith('image/')) {
                const $image = $('<img alt="" class="ai-chat-attachment-preview">');
                $label.append($image);
                if (saved) {
                    this._loadAttachment(attachment).then(blobUrl => $image.attr('src', blobUrl))
                        .catch(error => this._attachmentError(error));
                } else $image.attr('src', url);
            } else $label.append($('<i class="ti ti-file-text ai-chat-file-icon" aria-hidden="true"></i>'));
            $label.append($('<span></span>').text(attachment.name));
            $item.append($label);
            if (saved) {
                const label = this._text('downloadAttachment', attachment.name);
                const $download = this._iconButton(label, 'download', 'btn-outline-secondary')
                    .addClass('ai-chat-attachment-download').on('click', () => this.downloadAttachment(attachment, $download));
                $item.append($download);
            }
            if (editable) {
                $item.append(this._iconButton(this._text('removeAttachment', attachment.name), 'x', 'btn-outline-secondary')
                    .prop('disabled', !this._canAttach()).on('click', () => {
                        this.state.attachments = this.state.attachments.filter(item => item !== attachment);
                        this._changed();
                        this.$input[0].focus({ preventScroll: true });
                    }));
            }
            $container.append($item);
        }
    }

    /** Shares authenticated image previews and downloads, retaining blobs only while their chat retains the attachment. */
    _loadAttachment(attachment) {
        const key = attachment.file || attachment.url;
        if (this.attachmentUrls.has(key)) return Promise.resolve(this.attachmentUrls.get(key));
        if (attachment.file) {
            const file = attachment.file;
            const url = URL.createObjectURL(file.slice(0, file.size, attachment.mediaType));
            this.attachmentUrls.set(key, url);
            return Promise.resolve(url);
        }
        let request = this.attachmentRequests.get(key);
        if (request) return request.promise;
        request = { controller: new AbortController() };
        this.attachmentRequests.set(key, request);
        request.promise = fetchChatAttachment(attachment, request.controller.signal).then(blob => {
            if (this.state.disposed || this.attachmentRequests.get(key) !== request) {
                throw Object.assign(new Error('Attachment request cancelled'), { name: 'AbortError' });
            }
            const url = URL.createObjectURL(blob);
            this.attachmentUrls.set(key, url);
            return url;
        }).finally(() => {
            if (this.attachmentRequests.get(key) === request) this.attachmentRequests.delete(key);
        });
        return request.promise;
    }

    /** Downloads a local upload or a saved attachment using the protected chat connection. */
    async downloadAttachment(attachment, button) {
        const $button = $(button);
        if (this.state.disposed || $button.prop('disabled')) return;
        $button.prop('disabled', true);
        try {
            const url = await this._loadAttachment(attachment);
            if (this.state.disposed) return;
            const link = document.createElement('a');
            link.href = url;
            link.download = attachment.name;
            this.element.appendChild(link);
            link.click();
            link.remove();
        } catch (error) {
            this._attachmentError(error);
        } finally {
            $button.prop('disabled', false);
        }
    }

    _attachmentError(error) {
        if (this.state.disposed || error.name === 'AbortError') return;
        this.state.notice = this._text('history.attachmentDownloadFailed');
        this._changed();
    }

    _releaseAttachmentUrls() {
        const retained = new Set([...this.state.attachments, ...this.state.messages.flatMap(message => message.attachments || [])]
            .map(attachment => attachment.file || attachment.url));
        for (const [key, request] of this.attachmentRequests) {
            if (!retained.has(key)) {
                this.attachmentRequests.delete(key);
                request.controller.abort();
            }
        }
        for (const [file, url] of this.attachmentUrls) {
            if (!retained.has(file)) {
                URL.revokeObjectURL(url);
                this.attachmentUrls.delete(file);
            }
        }
    }

    _updateConfirmation() {
        if (this.renderedConfirmation === this.state.confirmation) return;
        this.renderedConfirmation = this.state.confirmation;
        this.$confirmation.empty().toggle(this.renderedConfirmation != null);
        if (this.renderedConfirmation == null) return;
        const confirmation = this.renderedConfirmation;
        const $actions = $('<div class="ai-chat-message-actions"></div>');
        const $confirm = $('<button class="btn btn-primary btn-sm" type="button"></button>')
            .text(confirmation.buttonText).on("click", () => confirmation.resolve(true));
        const $cancel = $('<button class="btn btn-outline-secondary btn-sm" type="button"></button>')
            .text(WJ.translate("button.cancel")).on("click", () => confirmation.resolve(false));
        $actions.append($confirm, $cancel);
        this.$confirmation.append($("<div></div>").text(confirmation.text), $actions);
    }

    _saveScroll() {
        if (!this.mounted || !this.element.isConnected) return;
        const history = this.$history[0];
        this.state.scrollTop = history.scrollTop;
        this.state.followLatest = history.scrollHeight - history.scrollTop - history.clientHeight <= 40;
    }

    /** Renders assistant Markdown with inert formatting and validated links only. */
    _renderMarkdown($container, text) {
        const template = document.createElement('template');
        template.innerHTML = WJ.parseMarkdown(`\n${text}\n\n`, { imgSrcPrefix: null, removeLastBr: true });
        const content = document.createDocumentFragment();
        const tags = new Set(['B', 'I', 'BR', 'H1', 'H2', 'H3', 'BLOCKQUOTE', 'UL', 'LI', 'TABLE', 'THEAD', 'TBODY', 'TR', 'TH', 'TD', 'A']);
        const append = (parent, source) => {
            if (source.nodeType === 3) { parent.append(document.createTextNode(source.textContent)); return; }
            if (source.nodeType !== 1 || source.namespaceURI !== 'http://www.w3.org/1999/xhtml') return;
            if (source.tagName === 'IMG') { parent.append(document.createTextNode(source.getAttribute('alt') || '')); return; }
            if (['DIV', 'SPAN'].includes(source.tagName) && (source.classList.contains('code') || source.classList.contains('code-inline'))) {
                source.querySelectorAll('br').forEach(lineBreak => lineBreak.replaceWith('\n'));
                const code = document.createElement('code');
                code.textContent = source.textContent;
                if (source.tagName === 'DIV') {
                    const pre = document.createElement('pre');
                    pre.append(code);
                    parent.append(pre);
                } else parent.append(code);
                return;
            }
            if (!tags.has(source.tagName)) return;
            const target = document.createElement(source.tagName.toLowerCase());
            if (source.tagName === 'A') {
                let url;
                try { url = new URL(source.getAttribute('href'), window.location.href); } catch (error) { /* Retain the label of an invalid link. */ }
                if (!url || !['http:', 'https:', 'mailto:'].includes(url.protocol) || url.username || url.password) {
                    source.childNodes.forEach(child => append(parent, child));
                    return;
                }
                target.href = url.href;
                target.target = '_blank';
                target.rel = 'noopener noreferrer';
            }
            if (['TH', 'TD'].includes(source.tagName)) {
                for (const alignment of ['text-start', 'text-center', 'text-end']) {
                    if (source.classList.contains(alignment)) target.classList.add(alignment);
                }
            }
            source.childNodes.forEach(child => append(target, child));
            if (source.tagName === 'TD' && /^[+\-−]?\d[\d\s.,]*(?:[%×x])?$/.test(target.textContent.trim())) {
                target.classList.add('ai-chat-number');
            }
            parent.append(target);
        };
        template.content.childNodes.forEach(child => append(content, child));
        while (content.firstChild?.nodeName === 'BR') content.firstChild.remove();
        while (content.lastChild?.nodeName === 'BR') content.lastChild.remove();
        $container.empty().append(content);
    }

    _createMessage(message) {
        const $article = $('<div class="ai-chat-message"></div>').addClass(message.role === "user" ? "ai-chat-user" : "ai-chat-assistant");
        const $heading = $('<div class="ai-chat-message-heading"></div>');
        const $label = $('<div class="ai-chat-message-label"></div>');
        const $time = $('<time class="ai-chat-message-time"></time>').hide();
        const $text = $('<div class="ai-chat-message-text"></div>');
        const $attachments = $('<div class="ai-chat-attachments" role="list"></div>').hide();
        const $status = $('<div class="ai-chat-message-status"></div>').hide();
        const $usage = $('<div class="ai-chat-usage"></div>').hide();
        const $actions = $('<div class="ai-chat-message-actions"></div>').hide();
        const $footer = $('<div class="ai-chat-message-footer"></div>').append($actions, $usage).hide();
        if (message.role === "assistant") {
            $article.append($('<span class="ai-chat-avatar" aria-hidden="true"><i class="ti ti-sparkles"></i></span>'));
        }
        $heading.append($label, $time);
        $article.append($heading, $attachments, $text, $status, $footer);
        return { $article, $label, $time, $text, $attachments, $status, $usage, $actions, $footer, text: null, actionKey: null, actions: [] };
    }

    /** Displays the message creation time without inventing dates for older transcripts. */
    _updateMessageTime(message, view) {
        if (view.created === message.created) return;
        view.created = message.created;
        const created = message.created == null || message.created === "" ? null : new Date(message.created);
        const valid = created != null && !Number.isNaN(created.getTime());
        view.$time.text(valid ? WJ.formatDateTime(created) : "")
            .attr("datetime", valid ? created.toISOString() : null).toggle(valid);
    }

    _updateAssistant(message, view, isLast) {
        let status = "";
        if (message.status === "pending") status = this._text("working");
        else if (message.status === "stopped") status = this._text("stopped");
        else if (message.status === "error") {
            const hint = message.errorCategory === "INVALID_CONTINUATION" ? "invalidContinuation"
                : message.errorCategory === "UNSUPPORTED_OPERATION" ? "unsupportedOperation"
                : message.retryable === false ? "changeRequest" : null;
            status = hint ? this._text(hint) + " " + (message.error || "") : message.error || this._text("unavailable");
        }
        view.$status.text(status).toggle(Boolean(status)).toggleClass("text-danger", message.status === "error");
        const hasUsage = message.status === "complete" && message.totalTokens != null;
        view.$usage.text(hasUsage ? this._text("tokens", message.totalTokens) : "").toggle(hasUsage);

        let actions = [];
        if (message.status === "complete") {
            actions = [
                ...(this.options.getAnswerActions?.(message) || []),
                { id: "copy", label: this._text("copy"), icon: "copy", iconOnly: true, run: $button => this._copy(message.text, $button) }
            ];
        } else if (isLast && this.state.pending == null && this.state.needsNewChat) {
            actions.push({ id: "restart", label: this._text("restart"), icon: "plus", run: () => this._restartConversation(message) });
        } else if (isLast && this.state.pending == null && this._canRetry(message)) {
            actions.push({ id: "retry", label: this._text("retry"), icon: "refresh", run: () => this.send(message.userPrompt, message) });
        }
        this._renderActions(view.$actions, view, actions);
        view.$actions.toggle(actions.length > 0);
        view.$footer.toggle(hasUsage || actions.length > 0);
    }

    _canRetry(message) {
        if (this.state.readOnlyReason || this.state.serverBusy) return false;
        return message.status === "stopped" || (message.status === "error" && message.retryable !== false
            && !["INVALID_CONTINUATION", "UNSUPPORTED_OPERATION"].includes(message.errorCategory));
    }

    _renderActions($container, view, actions) {
        view.actions = actions;
        const key = JSON.stringify(actions.map(action => [action.id, action.label, action.icon, Boolean(action.iconOnly), Boolean(action.disabled), action.expanded]));
        if (view.actionKey === key) return;
        view.actionKey = key;
        disposeChatTooltips($container);
        $container.empty();
        actions.forEach((action, index) => {
            const $button = action.iconOnly ? this._iconButton(action.label, action.icon) : this._button(action.label, action.icon);
            $button.attr("data-ai-action", action.id);
            $button.prop("disabled", Boolean(action.disabled));
            if (action.controls) $button.attr({ 'aria-controls': action.controls, 'aria-expanded': String(Boolean(action.expanded)) });
            $button.on("click", () => view.actions[index]?.run?.($button));
            $container.append($button);
        });
    }

    /** Copies the original reply, including on hosts without the Clipboard API. */
    async _copy(text, $button) {
        try {
            if (navigator.clipboard?.writeText != null) {
                await navigator.clipboard.writeText(text || "");
            } else {
                const $copyInput = $('<textarea class="ai-chat-copy-input" aria-hidden="true" tabindex="-1"></textarea>').val(text || "");
                this.$root.append($copyInput);
                try {
                    $copyInput[0].select();
                    if (!document.execCommand("copy")) throw new Error("Clipboard is unavailable");
                } finally {
                    $copyInput.remove();
                    if ($button[0].isConnected) $button.trigger("focus");
                }
            }
            if (!this.state.disposed && $button[0].isConnected) {
                $button.find("span").text(this._text("copied"));
                setChatTooltip($button[0], this._text("copied"));
            }
        } catch (error) {
            if (this.state.disposed) return;
            this.state.notice = this._text("copyFailed");
            this._changed();
        }
    }
}
