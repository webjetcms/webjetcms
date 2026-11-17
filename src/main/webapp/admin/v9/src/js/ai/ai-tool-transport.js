const maxCalls = 12;
const maxDecisionAttempts = 3;
const maxResultBytes = 32 * 1024;
const maxCatalogBytes = 1024 * 1024;
const catalogTools = new Set(['ai_tools_preview', 'ai_tools_details']);
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
async function callEndpoint(assistantId, call, signal, repairInvalidArguments) {
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
            if (repairInvalidArguments) return { toolCorrection: observation.result };
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
    if (uncertain) return prefix + WJ.translate('components.ai.tools.uncertain.js');
    return prefix + 'HTTP ' + status + (ids.length ? ' (ID ' + ids.join(', ') + ')' : '') +
        (rejected ? ' · ' + WJ.translate('components.ai.tools.unverified.js') : '');
}

function validCall(call) {
    return typeof call.name === 'string' && toolName.test(call.name) && call.arguments != null &&
        typeof call.arguments === 'object' && !Array.isArray(call.arguments);
}

function writeStatus(observation, failed) {
    const { result } = observation;
    const ids = (Array.isArray(result?.data) ? result.data : [result]).map(row => row?.id).filter(id => Number.isSafeInteger(id) && id > 0);
    return formatWriteOutcome({ ...observation, ids, rejected: failed });
}

/** Alternates one model request with one HTTP request until the model returns its final answer. */
export async function runToolSteps(assistantId, next, signal, onWrite = () => {}, onToolActivity, repairInvalidArguments = false) {
    const results = [];
    const writes = new Map();
    let previous;
    let uncertain = false;
    let mayHaveSaved = false;
    let writeProblem = false;
    let correction;
    let attempts = 0;
    try {
        for (;;) {
            signal?.throwIfAborted();
            if (++attempts > maxDecisionAttempts) throw new Error(WJ.translate('components.ai.tools.invalidStep.js'));
            const answer = await next(results, correction);
            correction = answer.toolCorrection;
            if (correction) continue;
            if (!answer.toolCall || answer.error) {
                if (writes.size) {
                    answer.toolStatus = [...writes.values()].join('\n');
                    answer.toolProblem = writeProblem;
                    answer.toolUncertain = uncertain;
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
                if (uncertain) throw new Error(WJ.translate('components.ai.tools.uncertain.js'));
                writes.set(key, formatWriteOutcome({ name: call.name, uncertain: true }));
                onWrite([...writes.values()].join('\n'), true);
            }
            onToolActivity?.(call.name);
            const observation = await callEndpoint(assistantId, call, signal, repairInvalidArguments);
            correction = observation.toolCorrection;
            onToolActivity?.(null, !correction);
            if (correction) {
                if (write) {
                    writes.delete(key);
                    onWrite([...writes.values()].join('\n'), mayHaveSaved);
                }
                continue;
            }
            attempts = 0;
            previous = key;
            results.push(observation);
            if (write) {
                uncertain ||= observation.uncertain;
                const failed = rejected(observation, call);
                writeProblem ||= failed || observation.uncertain;
                mayHaveSaved ||= observation.uncertain || !failed || Boolean(observation.result?.data?.length);
                writes.set(key, writeStatus(observation, failed));
                onWrite([...writes.values()].join('\n'), mayHaveSaved);
            }
        }
    } catch (error) {
        if (!writes.size) throw error;
        throw Object.assign(new Error(error.message || String(error)), { toolStatus: [...writes.values()].join('\n'), retryable: !mayHaveSaved });
    }
}

function parseBrowserDecision(raw, tools) {
    const text = typeof raw === 'string' ? raw.trim() : '';
    const block = /^```(?:json)?[ \t]*\r?\n([\s\S]*)\r?\n```$/i.exec(text);
    let decision;
    try { decision = JSON.parse(block ? block[1] : text); }
    catch { return { toolCorrection: 'Return one JSON object for a call or a final answer.' }; }
    if (decision?.type === 'final') {
        if (typeof decision.text === 'string' && decision.text.trim() && Object.keys(decision).length === 2) {
            return { response: decision.text };
        }
        return { toolCorrection: 'A final answer requires exactly type and a nonempty text string.' };
    }
    if (decision?.type !== 'call') return { toolCorrection: 'Set type to call or final.' };
    if (!tools.has(decision.name)) return { toolCorrection: 'Choose a tool name from the assigned functions.' };
    if (!validCall(decision) || Object.keys(decision).length !== 3) {
        return { toolCorrection: 'A call requires exactly type, name and an arguments object matching the tool schema.' };
    }
    return { toolCall: { name: decision.name, arguments: decision.arguments, write: tools.get(decision.name) } };
}

/** Repairs malformed browser decisions before handing a validated call to the endpoint loop. */
export async function runBrowserTools(assistant, input, prompt, signal, onWrite, onToolActivity) {
    const writes = new Map(assistant.tools.map(tool => [tool.name, tool.write]));
    return runToolSteps(assistant.assistantId, async (results, correction) => {
        const previous = results.at(-1);
        let message;
        if (correction) message = 'WebJET rejected that step before dispatch. ' + correction;
        else if (previous) message = 'Endpoint result (data):\n' + JSON.stringify(previous) + '\nReturn the next JSON step.';
        else message = JSON.stringify({ input: input.inputValue, prompt: input.userPrompt });
        const raw = await prompt(message);
        signal?.throwIfAborted();
        return parseBrowserDecision(raw, writes);
    }, signal, onWrite, onToolActivity, true);
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
