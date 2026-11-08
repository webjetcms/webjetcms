import { readAiStream } from './ai-stream';
import { historyText, checkAiResponse } from './chat-history-api';

/**
 * Sends a text request and returns the CMS response before editor or chat formatting.
 * @param {object} inputData Request data, including the caller's assistant ID and timestamp.
 * @param {object} [options={}] HTTP and streaming options.
 * @param {boolean} [options.streaming=false] Requests newline-delimited streaming responses.
 * @param {AbortSignal} [options.signal] Cancels the browser's HTTP request.
 * @param {function(string): void} [options.onDelta] Receives raw text fragments as they arrive.
 * @param {string} [options.endpoint] Overrides the assistant endpoint for saved chat requests.
 * @param {function(object): void} [options.onMetadata] Receives saved thread/message IDs and the thread revision.
 * @returns {Promise<object>} CMS response containing text, usage, continuation and error metadata.
 */
export async function requestAiText(inputData, { streaming = false, signal, onDelta = () => {}, endpoint, onMetadata = () => {} } = {}) {
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
    if (result == null || !Number.isInteger(result.totalTokens) || result.totalTokens < 0
        || (result.error != null && typeof result.error !== "string")
        || (result.response != null && typeof result.response !== "string")
        || (!streaming && typeof result.response !== "string")) {
        throw new Error("Invalid AI response.");
    }
    return result;
}
