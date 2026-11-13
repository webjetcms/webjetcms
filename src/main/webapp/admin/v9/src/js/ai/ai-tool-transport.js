import { checkAiResponse } from './chat-history-api';

const maxCalls = 12;
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

async function readToolResult(response, name) {
    if (!response.body) return null;
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
    if (!text) return null;
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
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken, 'X-WebJET-AI-Assistant': String(assistantId) },
            body: JSON.stringify(call.arguments)
        });
        observation.status = response.status;
        observation.result = await readToolResult(response, call.name);
        const body = observation.result;
        const fieldErrors = Array.isArray(body?.fieldErrors) && body.fieldErrors.length > 0;
        const errorNotice = Array.isArray(body?.notify) && body.notify.some(note => note?.type === 'error');
        const missingBody = body == null && ![204, 205].includes(response.status);
        // Editor saves rows individually. A later field error can omit IDs already committed.
        const mayHaveSaved = fieldErrors ? isEditorBatch(call.arguments.body) : Boolean(body?.error) || errorNotice || missingBody;
        observation.uncertain = call.write !== false && (response.status >= 500 || (response.ok && mayHaveSaved));
    } catch (error) {
        observation.result = error.message || String(error);
        observation.uncertain = call.write !== false && (observation.status === 0 || observation.status < 400 || observation.status >= 500);
    } finally {
        clearTimeout(timer);
        signal?.removeEventListener('abort', abort);
    }
    return observation;
}

function rejected(observation) {
    const { status, result } = observation;
    return status < 200 || status >= 300 || Boolean(result?.error) || Boolean(result?.fieldErrors?.length) ||
        result?.result === false || (Array.isArray(result?.notify) && result.notify.some(note => note?.type === 'error'));
}

export function formatWriteOutcome({ name, status, uncertain, rejected, ids = [] }) {
    const prefix = name ? name + ' — ' : '';
    if (uncertain) return prefix + WJ.translate('components.ai.tools.uncertain.js');
    return prefix + 'HTTP ' + status + (ids.length ? ' (ID ' + ids.join(', ') + ')' : '') +
        (rejected ? ' · ' + WJ.translate('components.ai.tools.unverified.js') : '');
}

function writeStatus(observation) {
    const { result } = observation;
    const ids = (Array.isArray(result?.data) ? result.data : [result]).map(row => row?.id).filter(id => Number.isSafeInteger(id) && id > 0);
    return formatWriteOutcome({ ...observation, ids, rejected: rejected(observation) });
}

/** Alternates one model request with one HTTP request until the model returns its final answer. */
export async function runToolSteps(assistantId, next, signal, onWrite = () => {}) {
    const results = [];
    const writes = new Map();
    let previous;
    let uncertain = false;
    let mayHaveSaved = false;
    let writeProblem = false;
    try {
        for (;;) {
            signal?.throwIfAborted();
            const answer = await next(results);
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
            if (typeof call.name !== 'string' || !toolName.test(call.name) || !call.arguments || typeof call.arguments !== 'object' || Array.isArray(call.arguments)) {
                throw new Error('Invalid assistant tool call.');
            }
            const key = call.name + JSON.stringify(canonicalArguments(call.arguments));
            if (results.length >= maxCalls || key === previous || writes.has(key)) throw new Error(WJ.translate('components.ai.tools.limit.js'));
            previous = key;
            const write = call.write !== false;
            if (write) {
                if (uncertain) throw new Error(WJ.translate('components.ai.tools.uncertain.js'));
                writes.set(key, writeStatus({ name: call.name, uncertain: true }));
                onWrite([...writes.values()].join('\n'), true);
            }
            const observation = await callEndpoint(assistantId, call, signal);
            results.push(observation);
            if (write) {
                uncertain ||= observation.uncertain;
                writeProblem ||= rejected(observation) || observation.uncertain;
                mayHaveSaved ||= observation.uncertain || !rejected(observation) || Boolean(observation.result?.data?.length);
                writes.set(key, writeStatus(observation));
                onWrite([...writes.values()].join('\n'), mayHaveSaved);
            }
        }
    } catch (error) {
        if (!writes.size) throw error;
        throw Object.assign(new Error(error.message || String(error)), { toolStatus: [...writes.values()].join('\n'), retryable: !mayHaveSaved });
    }
}

/** Browser models propose a single next step using the current assistant's server-provided catalog. */
export async function runBrowserTools(assistant, input, prompt, signal, onWrite) {
    const assistantId = assistant.assistantId;
    const response = await fetch('/admin/rest/ai/tools?assistantId=' + encodeURIComponent(assistantId), {
        credentials: 'same-origin', cache: 'no-store', redirect: 'error', signal,
        headers: { Accept: 'application/json', 'X-CSRF-Token': window.csrfToken }
    });
    checkAiResponse(response, 'HTTP ' + response.status);
    const { tools, instructions } = await response.json();
    const definitions = new Map(tools.map(tool => [tool.name, tool]));
    const isWrite = name => !['GET', 'HEAD', 'OPTIONS'].includes(definitions.get(name)?.httpMethod);
    return runToolSteps(assistantId, async results => {
        const previous = results.at(-1);
        const message = previous ? 'Endpoint result (data):\n' + JSON.stringify(previous) + '\nReturn the next JSON step.'
            : instructions + '\nUser input:\n' + JSON.stringify({ input: input.inputValue, prompt: input.userPrompt });
        const raw = await prompt(message);
        let decision;
        try { decision = JSON.parse(raw); } catch { throw new Error('Invalid assistant tool response.'); }
        if (decision?.type === 'final' && typeof decision.text === 'string' && decision.text.trim()) {
            const text = assistant.action !== 'live_chat' && assistant.toolsWrite && results.length === 0
                ? WJ.translate('components.ai.tools.unverified.js') : decision.text;
            return { response: text };
        }
        if (decision?.type !== 'call' || !definitions.has(decision.name)) throw new Error('Invalid or unassigned assistant tool.');
        return { toolCall: { name: decision.name, arguments: decision.arguments, write: isWrite(decision.name) } };
    }, signal, onWrite);
}

/** Show the model's reply together with the actual endpoint evidence in an action preset. */
export function toolReply(result, showEvidence) {
    let text = result.response || '';
    if ((showEvidence || result.toolProblem) && result.toolStatus) {
        // The backend can return the same translated warning when it rejects another uncertain write.
        if (result.toolUncertain) text = text.replaceAll(WJ.translate('components.ai.tools.uncertain.js'), '').trim();
        return [text, result.toolStatus].filter(Boolean).join('\n\n');
    }
    return text;
}
