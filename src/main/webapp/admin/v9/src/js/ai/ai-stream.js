/**
 * Reads AI NDJSON records independently of HTTP chunk boundaries.
 * A successful response requires a terminal result and a complete stream.
 * @param {Response} response HTTP response containing delta and result records.
 * @param {function(string): void} onDelta Receives generated text as it arrives.
 * @returns {Promise<object>} Final assistant response, including usage and any provider error.
 */
export async function readAiStream(response, onDelta) {
    if (!response.ok) {
        await response.body?.cancel();
        throw new Error(`HTTP ${response.status} ${response.statusText}`.trim());
    }
    if (response.headers.get("Content-Type")?.split(";")[0].trim() !== "application/x-ndjson") {
        await response.body?.cancel();
        throw new Error("Unexpected AI stream content type.");
    }
    if (!response.body) throw new Error("Missing AI response stream.");

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8", { fatal: true });
    let buffer = "";
    let result = null;
    let finished = false;
    const readMessage = line => {
        if (line.trim() === "") return;
        if (result !== null) throw new Error("Unexpected message after AI stream result.");
        const message = JSON.parse(line);
        if (message?.type === "delta" && typeof message.text === "string") {
            onDelta(message.text);
        } else if (message?.type === "result" && Number.isInteger(message.result?.totalTokens)
            && message.result.totalTokens >= 0
            && (message.result.error == null || typeof message.result.error === "string")) {
            result = message.result;
        } else {
            throw new Error("Invalid AI stream message.");
        }
    };

    try {
        while (!finished) {
            const { value, done } = await reader.read();
            finished = done;
            buffer += done ? decoder.decode() : decoder.decode(value, { stream: true });
            let newline;
            while ((newline = buffer.indexOf("\n")) !== -1) {
                const line = buffer.substring(0, newline);
                buffer = buffer.substring(newline + 1);
                readMessage(line);
            }
        }
        readMessage(buffer);
        if (result === null) throw new Error("Incomplete AI response stream.");
        return result;
    } finally {
        if (!finished) await reader.cancel().catch(() => {});
        reader.releaseLock();
    }
}
