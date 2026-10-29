import { readAiStream } from './ai-stream';

/**
 * Sends a text request and returns the CMS response before editor or chat formatting.
 * @param {object} inputData Request data, including the caller's assistant ID and timestamp.
 * @param {object} [options={}] HTTP and streaming options.
 * @param {boolean} [options.streaming=false] Requests newline-delimited streaming responses.
 * @param {AbortSignal} [options.signal] Cancels the browser's HTTP request.
 * @param {function(string): void} [options.onDelta] Receives raw text fragments as they arrive.
 * @returns {Promise<object>} CMS response containing text, usage, continuation and error metadata.
 */
export async function requestAiText(inputData, { streaming = false, signal, onDelta = () => {} } = {}) {
    const response = await fetch(streaming
        ? "/admin/rest/ai/assistant/response-stream/"
        : "/admin/rest/ai/assistant/response/", {
        method: "POST",
        headers: {
            "Content-Type": "application/json; charset=utf-8",
            "Accept": streaming ? "application/x-ndjson" : "application/json",
            "X-CSRF-Token": window.csrfToken
        },
        body: JSON.stringify(inputData),
        signal
    });
    if (!response.ok) {
        await response.body?.cancel();
        if (response.status === 401) WJ.keepSessionShowLogoffMessage();
        else if (response.status === 403) {
            try {
                WJ.keepSessionShowTokenMessage(window.csrfError);
            } catch (error) {
                window.alert(window.csrfError);
            }
        }
        throw new Error(`HTTP ${response.status} ${response.statusText}`.trim());
    }

    let result;
    if (streaming) {
        result = await readAiStream(response, onDelta);
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
