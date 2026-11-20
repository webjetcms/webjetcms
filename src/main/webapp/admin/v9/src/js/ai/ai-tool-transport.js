const maxCalls = 12;
const maxResultBytes = 32 * 1024;
const maxCatalogBytes = 1024 * 1024;
const catalogTools = new Set(['ai_tools_preview', 'ai_tools_details', 'ai_tools_load_skill']);
const toolName = /^[A-Za-z0-9_-]{1,64}$/;

function canonicalArguments(value) {
    if (Array.isArray(value)) return value.map(canonicalArguments);
    if (value && typeof value === 'object') {
        return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonicalArguments(value[key])]));
    }
    return value;
}

function isEditorBatch(body) {
    return ['create', 'edit', 'remove'].includes(body?.action) && body.data != null &&
        typeof body.data === 'object' && !Array.isArray(body.data) && Object.keys(body.data).length > 1;
}

function emptyToolResult(response, responseType) {
    if (response.ok && responseType === 'empty') return null;
    if (response.ok && responseType === 'text') return '';
    throw new Error('Endpoint returned an empty response body.');
}

async function readToolResult(response, name, responseType) {
    if (response.status === 204 || response.status === 205) return null;
    if (!response.body) return emptyToolResult(response, responseType);
    const limit = catalogTools.has(name) ? maxCatalogBytes : maxResultBytes;
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let size = 0;
    let text = '';
    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        size += value.length;
        if (size > limit) {
            await reader.cancel();
            throw new Error('Endpoint response exceeded ' + (limit / 1024) + ' KiB.');
        }
        text += decoder.decode(value, { stream: true });
    }
    text += decoder.decode();
    if (!text) return emptyToolResult(response, responseType);
    const type = response.headers.get('Content-Type')?.split(';')[0].trim().toLowerCase() || '';
    if (type === 'application/json' || type.endsWith('+json')) return JSON.parse(text);
    if (type.startsWith('text/') && type !== 'text/html') return text;
    throw new Error('Unsupported endpoint response type.');
}

/** Calls the registered mapping once with the same session and CSRF token as the admin UI. */
async function callEndpoint(assistantId, call, signal) {
    const observation = { name: call.name, arguments: call.arguments, status: 0, result: null, uncertain: false };
    const delivery = new AbortController();
    const abort = () => delivery.abort();
    if (signal?.aborted) throw signal.reason;
    signal?.addEventListener('abort', abort, { once: true });
    const timer = setTimeout(abort, 60000);
    try {
        const response = await fetch('/admin/rest/ai/tools/' + call.name + '/call', {
            method: 'POST', signal: delivery.signal, credentials: 'same-origin', redirect: 'error', cache: 'no-store',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken, 'X-WebJET-AI-Assistant': String(assistantId),
                'X-WebJET-AI-Chat': call.chat ?? 'unsaved' },
            body: JSON.stringify(call.arguments)
        });
        observation.status = response.status;
        const responseType = response.headers.get('X-WebJET-AI-Tool-Response');
        observation.result = await readToolResult(response, call.name, responseType);
        // Only the authorized facade marks a schema rejection before invoking the registered handler.
        if (response.status === 400 && responseType === 'invalid-arguments' && typeof observation.result === 'string') {
            observation.result = { error: observation.result };
        }
        const body = observation.result;
        const fieldErrors = Array.isArray(body?.fieldErrors) && body.fieldErrors.length > 0;
        const errorNotice = Array.isArray(body?.notify) && body.notify.some(note => note?.type === 'error');
        const missingBody = body == null && responseType !== 'empty' && ![204, 205].includes(response.status);
        // Editor saves rows individually. A later field error can omit IDs already committed.
        const mayHaveSaved = fieldErrors ? isEditorBatch(call.arguments.body) : Boolean(body?.error) || errorNotice || missingBody;
        observation.uncertain = call.write !== false && (response.status >= 500 || (response.ok && mayHaveSaved));
    } catch (error) {
        observation.result = { error: error.message || String(error) };
        observation.uncertain = call.write !== false && (observation.status === 0 || observation.status < 400 || observation.status >= 500);
    } finally {
        clearTimeout(timer);
        signal?.removeEventListener('abort', abort);
    }
    return observation;
}

function rejected(observation, call) {
    const { status, result } = observation;
    return status < 200 || status >= 300 || Boolean(result?.error) || Boolean(result?.fieldErrors?.length) ||
        (call.rowReorder && result === false) || result?.result === false ||
        (Array.isArray(result?.notify) && result.notify.some(note => note?.type === 'error'));
}

export function formatWriteOutcome({ name, status, uncertain, rejected, ids = [] }) {
    const prefix = name ? name + ' — ' : '';
    const records = ids.length ? ' (ID ' + ids.join(', ') + ')' : '';
    if (uncertain) return prefix + WJ.translate('components.ai.tools.uncertain.js') + records;
    return prefix + 'HTTP ' + status + records +
        (rejected ? ' · ' + WJ.translate('components.ai.tools.unverified.js') : '');
}

function validCall(call) {
    return typeof call.name === 'string' && toolName.test(call.name) && call.arguments != null &&
        typeof call.arguments === 'object' && !Array.isArray(call.arguments);
}

function writeOutcome(observation, call) {
    const { name, status, uncertain, result } = observation;
    const ids = (Array.isArray(result?.data) ? result.data : [result]).map(row => row?.id).filter(id => Number.isSafeInteger(id) && id > 0);
    return { name, status, uncertain, ids, rejected: rejected(observation, call) };
}

/** Alternates one model request with one HTTP request until the model returns its final answer. */
export async function runToolSteps(assistantId, next, signal, onWrite = () => {}, onToolActivity) {
    const results = [];
    const writes = new Map();
    const writeStatus = () => Array.from(writes.values(), formatWriteOutcome).join('\n');
    const mayHaveSaved = () => [...writes.values()].some(outcome => outcome.uncertain || !outcome.rejected || outcome.ids.length > 0);
    let previous;
    try {
        for (;;) {
            signal?.throwIfAborted();
            const answer = await next(results);
            if (!answer.toolCall || answer.error) {
                if (writes.size) {
                    const outcomes = [...writes.values()];
                    answer.toolStatus = writeStatus();
                    answer.toolUncertain = outcomes.some(outcome => outcome.uncertain);
                    answer.toolProblem = answer.toolUncertain || outcomes.some(outcome => outcome.rejected);
                }
                return answer;
            }
            const call = answer.toolCall;
            signal?.throwIfAborted();
            if (!validCall(call)) {
                throw new Error(WJ.translate('components.ai.tools.invalidStep.js'));
            }
            const key = call.name + JSON.stringify(canonicalArguments(call.arguments));
            if (results.length >= maxCalls || key === previous || writes.has(key)) throw new Error(WJ.translate('components.ai.tools.limit.js'));
            const write = call.write !== false;
            if (write) {
                const blocked = [...writes.values()].find(outcome => outcome.uncertain || (outcome.rejected && outcome.ids.length > 0));
                if (blocked) throw new Error(WJ.translate(blocked.uncertain ? 'components.ai.tools.uncertain.js' : 'components.ai.tools.unverified.js'));
                writes.set(key, { name: call.name, uncertain: true });
                onWrite(writeStatus(), true);
            }
            onToolActivity?.(call.name);
            const observation = await callEndpoint(assistantId, call, signal);
            onToolActivity?.(null);
            previous = key;
            results.push(observation);
            if (write) {
                writes.set(key, writeOutcome(observation, call));
                onWrite(writeStatus(), mayHaveSaved());
            }
        }
    } catch (error) {
        if (!writes.size) throw error;
        throw Object.assign(new Error(error.message || String(error)), { toolStatus: writeStatus(), retryable: !mayHaveSaved() });
    }
}

/** Distinguishes an ordinary answer from a write whose outcome could not be verified. */
export function writePresetStatus(result) {
    if (result.toolProblem) return 'components.ai.tools.unverified.js';
    if (!result.toolStatus) return 'components.ai_assistants.tools.no_action_executed.js';
    return null;
}

/** Show the model's reply together with the actual endpoint evidence in an action preset. */
export function toolReply(result) {
    let text = result.response || '';
    if (result.toolStatus) {
        // The backend can return the same translated warning when it rejects another uncertain write.
        if (result.toolUncertain) text = text.replaceAll(WJ.translate('components.ai.tools.uncertain.js'), '').trim();
        return [text, result.toolStatus].filter(Boolean).join('\n\n');
    }
    return text;
}
