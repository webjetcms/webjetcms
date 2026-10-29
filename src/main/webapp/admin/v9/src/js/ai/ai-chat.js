import $ from 'jquery';
import { AiChatTransport } from './ai-chat-transport';

/**
 * A reusable AI conversation that keeps its history while its view is unmounted.
 * Hosts can supply request context and additional answer or toolbar actions.
 */
export class AiChat {

    constructor(options) {
        this.options = options || {};
        this.assistant = this.options.assistant;
        this.state = this._initialState();
        this.messageViews = new Map();
        this.toolbarView = {};
        this.mounted = false;
        this._build();
    }

    _initialState() {
        return {
            messages: [], draft: "", conversationId: null, pending: null, needsNewChat: false,
            scrollTop: 0, followLatest: true, notice: null, confirmation: null, disposed: false
        };
    }

    _build() {
        this.$root = $('<div class="ai-chat"></div>');
        this.element = this.$root[0];
        this.$toolbar = $('<div class="ai-chat-toolbar"></div>');
        this.$history = $('<div class="ai-chat-history" role="log" aria-live="polite" aria-relevant="additions text" tabindex="0"></div>')
            .attr("aria-label", this._text("conversation"));
        this.$empty = $('<div class="ai-chat-empty"></div>').text(this._text("empty"));
        this.$history.append(this.$empty).on("scroll", () => this._saveScroll());

        const $composer = $('<div class="ai-chat-composer"></div>');
        this.$notice = $('<div class="ai-chat-notice" role="status"></div>');
        this.$confirmation = $('<div class="ai-chat-confirmation" role="group"></div>').hide();
        this.renderedConfirmation = null;
        this.$input = $('<textarea class="form-control ai-chat-input" rows="3"></textarea>')
            .attr({ "aria-label": this._text("message"), placeholder: this._text("placeholder") });
        this.$input.on("input", () => {
            this.state.draft = this.$input.val();
            this._updateSendButton();
        });
        this.$input.on("keydown", event => {
            if (event.key === "Enter" && !event.shiftKey && !event.originalEvent?.isComposing && event.keyCode !== 229) {
                event.preventDefault();
                this._sendDraft();
            }
        });
        const $composerActions = $('<div class="ai-chat-composer-actions"></div>');
        this.$send = this._button(this._text("send"), "send", "btn-primary")
            .addClass("btn-ai-chat-send").on("click", () => this._sendDraft());
        this.$stop = this._button(this._text("stop"), "square", "btn-outline-secondary")
            .addClass("btn-ai-chat-stop").on("click", () => this.stop());
        $composerActions.append(this.$stop, this.$send);
        $composer.append(this.$notice, this.$confirmation, this.$input, $composerActions);
        this.$root.append(this.$toolbar, this.$history, $composer);
    }

    /** Mounts this conversation in a host element and restores its draft and scroll position. */
    mount(container) {
        if (this.state.disposed) return;
        if (container?.nodeType !== 1) throw new TypeError("AI chat requires a container element.");
        this.unmount();
        container.appendChild(this.element);
        this.mounted = true;
        this.refresh();
        this.$input[0].focus({ preventScroll: true });
    }

    /** Removes only this component's view; an unfinished response continues into its state. */
    unmount() {
        if (this.mounted) {
            this.state.draft = this.$input.val();
            this._saveScroll();
        }
        this.mounted = false;
        this.$root.detach();
    }

    /** Reflects state and host-supplied actions without notifying the host of a state change. */
    refresh() {
        if (!this.mounted || this.state.disposed) return;
        const state = this.state;
        const followLatest = state.followLatest;
        const scrollTop = state.scrollTop;
        const lastMessage = state.messages.at(-1);

        this._renderActions(this.$toolbar, this.toolbarView, [
            { id: "new", label: this._text("new"), icon: "plus", run: () => this.newChat() },
            ...(this.options.getToolbarActions?.() || [])
        ]);
        this.$empty.toggle(state.messages.length === 0);
        for (const [message, view] of this.messageViews) {
            if (!state.messages.includes(message)) {
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
            if (view.text !== message.text) {
                view.$text.text(message.text || "");
                view.text = message.text;
            }
            if (message.role === "assistant") this._updateAssistant(message, view, message === lastMessage);
        }
        this.$history.attr("aria-busy", state.pending != null ? "true" : "false");
        this.$notice.text(state.notice || "").toggle(Boolean(state.notice));
        this._updateConfirmation();
        if (this.$input.val() !== state.draft) this.$input.val(state.draft);
        this._updateSendButton();
        if (this.element.isConnected) {
            this.$history[0].scrollTop = followLatest ? this.$history[0].scrollHeight : scrollTop;
            this._saveScroll();
        }
    }

    /** Sends a new prompt, or retries the last failed/stopped answer with its original input. */
    async send(prompt, retryMessage = null) {
        const state = this.state;
        if (state.disposed || state.pending != null || state.needsNewChat || typeof prompt !== "string" || !prompt.trim()) return;
        if (retryMessage != null && (state.messages.at(-1) !== retryMessage || !this._canRetry(retryMessage))) return;

        let inputData;
        try {
            inputData = retryMessage?.inputData ? { ...retryMessage.inputData } : {
                inputValue: "", inputValueType: "text", outputValueType: "text",
                ...this.options.getInputData?.(prompt), userPrompt: prompt
            };
        } catch (error) {
            state.notice = error.message || this._text("unavailable");
            this._changed();
            return;
        }
        if (state.conversationId) inputData.conversationId = state.conversationId;
        else delete inputData.conversationId;

        let message = retryMessage;
        if (message == null) {
            state.messages.push({ role: "user", text: prompt });
            message = { role: "assistant", userPrompt: prompt };
            state.messages.push(message);
        }
        Object.assign(message, {
            text: "", status: "pending", totalTokens: null, error: null,
            errorCategory: null, providerStatusCode: null, retryable: null, inputData: { ...inputData }
        });
        if (retryMessage == null) state.draft = "";
        state.notice = null;
        state.followLatest = true;

        const request = { message, transport: null, disposed: false };
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
                onDownload: percent => {
                    if (!current()) return;
                    state.notice = WJ.translate("components.ai_assistants.browser.downloadingModel.js", [percent == null ? "" : percent + "%"]);
                    this._changed();
                },
                onConfirm: (text, buttonText) => this._requestConfirmation(request, text, buttonText)
            });
            if (!current()) return;
            state.notice = null;
            message.errorCategory = result.errorCategory;
            message.providerStatusCode = result.providerStatusCode;
            message.retryable = result.retryable;
            if (result.stopped) message.status = "stopped";
            else if (result.success) {
                message.text = result.response ?? message.text;
                message.status = "complete";
                message.totalTokens = result.totalTokens;
                state.conversationId = result.conversationId || null;
            } else {
                message.status = "error";
                message.error = result.errorText || this._text("unavailable");
                state.needsNewChat = result.errorCategory === "INVALID_CONTINUATION";
            }
        } catch (error) {
            if (current()) {
                state.notice = null;
                message.status = "error";
                message.error = error.message || String(error);
            }
        } finally {
            this._disposeRequest(request);
            if (current()) {
                state.pending = null;
                state.confirmation?.resolve(false);
                this._changed();
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
        this._disposeRequest(request);
        this._changed();
    }

    /** Clears this conversation in place so hosts can retain a reference to its state. */
    newChat() {
        if (this.state.disposed) return;
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
        if (!state.draft.trim()) state.draft = message.userPrompt;
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
    }

    _disposeRequest(request) {
        if (request.disposed) return;
        request.disposed = true;
        request.transport?.dispose();
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

    _updateSendButton() {
        this.$send.prop("disabled", this.state.pending != null || this.state.needsNewChat || !this.state.draft.trim()).toggle(this.state.pending == null);
        this.$stop.toggle(this.state.pending != null);
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

    _createMessage(message) {
        const $article = $('<div class="ai-chat-message"></div>').addClass(message.role === "user" ? "ai-chat-user" : "ai-chat-assistant");
        const $label = $('<div class="ai-chat-message-label"></div>');
        const $text = $('<div class="ai-chat-message-text"></div>');
        const $status = $('<div class="ai-chat-message-status"></div>').hide();
        const $usage = $('<div class="ai-chat-usage"></div>').hide();
        const $actions = $('<div class="ai-chat-message-actions"></div>').hide();
        $article.append($label, $text, $status, $usage, $actions);
        return { $article, $label, $text, $status, $usage, $actions, text: null, actionKey: null, actions: [] };
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
                { id: "copy", label: this._text("copy"), icon: "copy", run: $button => this._copy(message.text, $button) }
            ];
        } else if (isLast && this.state.pending == null && this.state.needsNewChat) {
            actions.push({ id: "restart", label: this._text("restart"), icon: "plus", run: () => this._restartConversation(message) });
        } else if (isLast && this.state.pending == null && this._canRetry(message)) {
            actions.push({ id: "retry", label: this._text("retry"), icon: "refresh", run: () => this.send(message.userPrompt, message) });
        }
        this._renderActions(view.$actions, view, actions);
        view.$actions.toggle(actions.length > 0);
    }

    _canRetry(message) {
        return message.status === "stopped" || (message.status === "error" && message.retryable !== false
            && !["INVALID_CONTINUATION", "UNSUPPORTED_OPERATION"].includes(message.errorCategory));
    }

    _renderActions($container, view, actions) {
        view.actions = actions;
        const key = JSON.stringify(actions.map(action => [action.id, action.label, action.icon, Boolean(action.disabled)]));
        if (view.actionKey === key) return;
        view.actionKey = key;
        $container.empty();
        actions.forEach((action, index) => {
            const $button = this._button(action.label, action.icon).prop("disabled", Boolean(action.disabled));
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
            if (!this.state.disposed && $button[0].isConnected) $button.find("span").text(this._text("copied"));
        } catch (error) {
            if (this.state.disposed) return;
            this.state.notice = this._text("copyFailed");
            this._changed();
        }
    }
}
