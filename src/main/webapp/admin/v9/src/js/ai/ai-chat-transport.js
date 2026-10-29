import { AiBrowserExecutor } from '../../../npm_packages/webjetdatatables/ai/ai-browser-executor';
import { readAiStream } from './ai-stream';

/** Executes chat requests independently of their container and content-insertion behavior. */
export class AiChatTransport {

    static lastTimestamp = 0;
    request = null;
    disposed = false;

    /**
     * Sends one turn, preserving the provider's raw text and opaque continuation ID.
     * @param {object} assistant Assistant configuration, including provider and streaming preference.
     * @param {object} inputData Current prompt, optional context and previous conversation ID.
     * @param {object} callbacks Callbacks for updates, translated statuses, download progress and confirmation.
     * @param {function(string): void} [callbacks.onUpdate] Receives the accumulated raw response.
     * @param {function(string): void} [callbacks.onStatus] Receives a translated status message.
     * @param {function(number|string): void} [callbacks.onDownload] Receives browser model download progress.
     * @param {function(string, string): Promise<boolean>} [callbacks.onConfirm] Requests browser activation or download approval.
     * @returns {Promise<object>} Raw response, usage, continuation ID and execution status.
     */
    async execute(assistant, inputData, callbacks = {}) {
        const executionResult = {
            success: false, response: "", totalTokens: 0, conversationId: null, errorText: null, stopped: false
        };
        if (this.disposed) return { ...executionResult, stopped: true };
        if (this.request != null) return { ...executionResult, errorText: "A chat request is already running." };

        AiChatTransport.lastTimestamp = Math.max(Date.now(), AiChatTransport.lastTimestamp + 1);
        const request = {
            assistantId: assistant.assistantId,
            timestamp: AiChatTransport.lastTimestamp,
            controller: new AbortController(),
            browser: null,
            cancelled: false,
            text: "",
            resolveCancellation: null,
            cancelConfirmation: null
        };
        this.request = request;
        const current = () => !this.disposed && !request.cancelled && this.request === request;
        const update = text => {
            if (!current()) return;
            request.text = typeof text === "string" ? text : String(text ?? "");
            callbacks.onUpdate?.(request.text);
        };
        const cancellation = new Promise(resolve => { request.resolveCancellation = () => resolve(null); });

        try {
            const operation = assistant.provider === "browser"
                ? this._executeBrowser(assistant, inputData, request, current, update, callbacks)
                : this._executeRest(assistant, inputData, request, update);
            const result = await Promise.race([operation, cancellation]);
            if (!current()) return { ...executionResult, response: request.text, stopped: true };
            return { ...executionResult, ...result };
        } catch (error) {
            if (!current()) return { ...executionResult, response: request.text, stopped: true };
            return { ...executionResult, response: request.text, errorText: error.message || String(error) };
        } finally {
            this._destroyBrowser(request);
            if (this.request === request) this.request = null;
        }
    }

    async _executeRest(assistant, inputData, request, update) {
        const data = { ...inputData, assistantId: request.assistantId, timestamp: request.timestamp };
        if (typeof data.conversationId !== "string" || data.conversationId.length === 0) delete data.conversationId;
        const streaming = assistant.useStreaming === true;
        const response = await fetch(streaming
            ? "/admin/rest/ai/assistant/response-stream/"
            : "/admin/rest/ai/assistant/response/", {
            method: "POST",
            headers: {
                "Content-Type": "application/json; charset=utf-8",
                "Accept": streaming ? "application/x-ndjson" : "application/json",
                "X-CSRF-Token": window.csrfToken
            },
            body: JSON.stringify(data),
            signal: request.controller.signal
        });
        let result;
        if (streaming) {
            result = await readAiStream(response, text => update(request.text + text));
        } else {
            if (!response.ok) {
                await response.body?.cancel();
                throw new Error(`HTTP ${response.status} ${response.statusText}`.trim());
            }
            const contentType = response.headers.get("Content-Type")?.split(";")[0].trim().toLowerCase();
            if (contentType !== "application/json") {
                await response.body?.cancel();
                throw new Error("Unexpected AI response content type.");
            }
            result = await response.json();
        }
        if (result != null && typeof result.error === "string" && result.error.length > 0) {
            return { response: request.text, errorText: result.error };
        }
        if (result == null || !Number.isInteger(result.totalTokens) || result.totalTokens < 0
            || (result.error != null && typeof result.error !== "string")
            || (result.response != null && typeof result.response !== "string")
            || (!streaming && typeof result.response !== "string")) {
            throw new Error("Invalid AI response.");
        }
        return {
            success: true,
            response: typeof result.response === "string" ? result.response : request.text,
            totalTokens: result.totalTokens,
            conversationId: typeof result.conversationId === "string" && result.conversationId.length > 0 ? result.conversationId : null
        };
    }

    async _executeBrowser(assistant, inputData, request, current, update, callbacks) {
        const bridge = {
            setCurrentStatus: (key, pulsate, ...params) => {
                if (current()) callbacks.onStatus?.(WJ.translate(key, params));
            },
            setDownloadStatus: percent => {
                if (current()) callbacks.onDownload?.(percent);
            },
            requestBrowserConfirmation: (text, buttonText) => new Promise((resolve, reject) => {
                if (!current()) { resolve(false); return; }
                const cancel = () => resolve(false);
                request.cancelConfirmation = cancel;
                Promise.resolve().then(() => current() && callbacks.onConfirm != null
                    ? callbacks.onConfirm(text, buttonText) : false
                ).then(confirmed => resolve(current() && confirmed === true), reject).finally(() => {
                    if (request.cancelConfirmation === cancel) request.cancelConfirmation = null;
                });
            })
        };
        request.browser = new AiBrowserExecutor(null, bridge);
        if (!request.browser.isAvailable(assistant)) {
            throw new Error(WJ.translate("components.ai_assistants.browser.api_not_available.js", [request.browser.getApiName(assistant)]));
        }
        const result = await request.browser.execute(assistant, inputData, false, update);
        return {
            success: result.success,
            response: request.text,
            totalTokens: result.totalTokens,
            stopped: result.stopped,
            errorText: result.errorText || (result.statusKey ? WJ.translate(result.statusKey, result.statusKeyParams || []) : null)
        };
    }

    /** Stops the current turn without changing the caller's successful continuation. */
    cancel() {
        const request = this.request;
        if (request == null || request.cancelled) return;
        request.cancelled = true;
        this.request = null;
        request.resolveCancellation();
        request.cancelConfirmation?.();
        request.controller.abort();
        if (request.browser != null) {
            this._destroyBrowser(request);
        } else {
            const parameters = new URLSearchParams({ assistantId: request.assistantId, timestamp: request.timestamp });
            fetch("/admin/rest/ai/assistant/stop-assistant/?" + parameters, {
                method: "GET", headers: { "X-CSRF-Token": window.csrfToken }, keepalive: true
            }).catch(() => {});
        }
    }

    _destroyBrowser(request) {
        if (request.browser == null) return;
        try {
            request.browser.destroy();
        } catch (error) {
            console.warn("Could not dispose the browser AI instance:", error);
        }
    }

    /** Cancels pending work and prevents this transport from accepting another turn. */
    dispose() {
        this.disposed = true;
        this.cancel();
    }
}
