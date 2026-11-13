import { readAiStream } from './ai-stream';
import { historyText, checkAiResponse } from './chat-history-api';
import { runToolSteps } from './ai-tool-transport';

/**
 * Sends a text request and returns the CMS response before editor or chat formatting.
 * @param {object} inputData Request data, including the caller's assistant ID and timestamp.
 * @param {object} [options={}] HTTP and streaming options.
 * @param {boolean} [options.streaming=false] Requests newline-delimited streaming responses.
 * @param {boolean} [options.toolsEnabled=false] Uses one JSON model response per step.
 * @param {AbortSignal} [options.signal] Cancels the browser's HTTP request.
 * @param {function(string): void} [options.onDelta] Receives raw text fragments as they arrive.
 * @param {function(string|null): void} [options.onToolActivity] Receives the tool name at dispatch and null when the call settles.
 * @param {string} [options.endpoint] Overrides the assistant endpoint for saved chat requests.
 * @param {function(object): void} [options.onMetadata] Receives saved thread/message IDs and the thread revision.
 * @returns {Promise<object>} CMS response containing text, usage, continuation and error metadata.
 */
export async function requestAiText(inputData, { toolsEnabled, onWrite, onToolActivity, ...options } = {}) {
    if (!toolsEnabled) return generate(inputData, options);
    const message = { ...inputData };
    let totalTokens = 0;
    return runToolSteps(message.assistantId, async toolResults => {
        const result = await generate({ ...message, toolResults }, { ...options, streaming: false });
        if (!options.endpoint && Number.isInteger(result.totalTokens)) {
            totalTokens += result.totalTokens;
            result.totalTokens = totalTokens;
        }
        if (result.toolCall && result.threadId) {
            options.onMetadata?.(result);
            message.threadId = result.threadId;
            message.revision = result.revision ?? message.revision;
            delete message.files;
        } else if (result.toolCall && result.conversationId) {
            message.conversationId = result.conversationId;
            delete message.files;
        } else if (result.toolCall && result.attachmentContextId) {
            message.attachmentContextId = result.attachmentContextId;
            delete message.files;
        }
        return result;
    }, options.signal, onWrite, onToolActivity);
}

async function generate(inputData, { streaming = false, signal, onDelta = () => {}, endpoint, onMetadata = () => {} }) {
    let body = JSON.stringify(inputData);
    const headers = {
        "Accept": streaming ? "application/x-ndjson" : "application/json",
        "X-CSRF-Token": window.csrfToken
    };
    const multipart = inputData.files?.length > 0;
    if (multipart) {
        const { files, ...message } = inputData;
        body = new FormData();
        body.append('message', JSON.stringify(message));
        for (const file of files) body.append('files', file, file.name);
    } else {
        headers['Content-Type'] = 'application/json; charset=utf-8';
    }
    const response = await fetch(endpoint || (streaming
        ? "/admin/rest/ai/assistant/response-stream/"
        : "/admin/rest/ai/assistant/response/"), {
        method: "POST",
        headers,
        body,
        signal
    });
    if (!response.ok) {
        let detail;
        if (endpoint || multipart) {
            try { detail = await response.json(); } catch (error) { /* Use the HTTP status if there is no JSON error. */ }
        } else await response.body?.cancel();
        checkAiResponse(response, typeof detail?.error === 'string'
            ? historyText(detail.error)
            : `HTTP ${response.status} ${response.statusText}`.trim());
    }

    let result;
    if (streaming) {
        result = await readAiStream(response, onDelta, onMetadata);
    } else {
        const contentType = response.headers.get("Content-Type")?.split(";")[0].trim().toLowerCase();
        if (contentType !== "application/json") {
            await response.body?.cancel();
            throw new Error("Unexpected AI response content type.");
        }
        result = await response.json();
    }
    if (result != null && typeof result.error === "string" && result.error.length > 0) return result;
    if (!streaming && Array.isArray(inputData.toolResults) && result?.toolCall) return result;
    if (result == null || !Number.isInteger(result.totalTokens) || result.totalTokens < 0
        || (result.error != null && typeof result.error !== "string")
        || (result.response != null && typeof result.response !== "string")
        || (!streaming && typeof result.response !== "string")) {
        throw new Error("Invalid AI response.");
    }
    return result;
}
