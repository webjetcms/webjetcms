/**
 * Fields consumed from the current server-monitoring response; the endpoint also supplies other metrics.
 * @typedef {Object} MonitoringSnapshot
 * @property {number} serverActualTime - Sampling time in epoch milliseconds.
 * @property {number} memUsed - Used JVM memory in bytes.
 * @property {number} memFree - Free JVM memory in bytes.
 * @property {number} memTotal - Allocated JVM memory in bytes.
 * @property {number} cpuUsageProcess - Process CPU percentage, or a negative value when unavailable.
 * @property {number} cpuUsage - System CPU percentage, or a negative value when unavailable.
 */

const POLL_INTERVAL = 5000;
const subscriptions = new Set();
let snapshotRequest = null;
let pollRequest = null;
let timer = null;

/**
 * Shares a current monitoring request while allowing each widget to cancel its own wait.
 * The underlying fetch is cancelled when its last consumer leaves; successful snapshots are not cached.
 *
 * @param {AbortSignal} signal - Lifetime of this consumer's wait.
 * @returns {Promise<MonitoringSnapshot>} Current server metrics; rejects with AbortError when this consumer aborts.
 * @throws {Error} If the monitoring endpoint returns an unsuccessful HTTP response; the promise rejects.
 */
export function readMonitoringSnapshot(signal) {
    if (signal.aborted) return Promise.reject(new DOMException('Monitoring request aborted', 'AbortError'));
    if (!snapshotRequest) {
        const request = { controller: new AbortController(), consumers: new Set() };
        snapshotRequest = request;
        request.promise = (async () => {
            const response = await fetch('/admin/rest/monitoring/actual', {
                signal: request.controller.signal, credentials: 'same-origin',
                headers: { Accept: 'application/json', 'X-CSRF-Token': window.csrfToken }
            });
            if (!response.ok) throw new Error(`Server monitoring request failed (${response.status})`);
            return response.json();
        })().finally(() => { if (snapshotRequest === request) snapshotRequest = null; });
    }
    const request = snapshotRequest;
    return new Promise((resolve, reject) => {
        const consumer = {};
        const release = () => {
            signal.removeEventListener('abort', abort);
            request.consumers.delete(consumer);
            if (!request.consumers.size && snapshotRequest === request) {
                snapshotRequest = null;
                request.controller.abort();
            }
        };
        const abort = () => { release(); reject(new DOMException('Monitoring request aborted', 'AbortError')); };
        request.consumers.add(consumer);
        signal.addEventListener('abort', abort, { once: true });
        request.promise.then(value => { release(); resolve(value); }, error => { release(); reject(error); });
    });
}

function activeSubscriptions() {
    if (document.visibilityState === 'hidden') return [];
    return [...subscriptions].filter(item => item.visible && item.container.isConnected && !item.signal.aborted);
}

/**
 * Keeps one non-overlapping timer for all visible monitoring cards.
 * Cancels polling when the document is hidden or no connected, visible subscription remains.
 *
 * @param {boolean} [immediate=false] - Whether to replace a pending timer with a zero-delay poll.
 */
function schedule(immediate = false) {
    if (!activeSubscriptions().length) {
        window.clearTimeout(timer);
        timer = null;
        pollRequest?.abort();
        return;
    }
    if (pollRequest || (timer !== null && !immediate)) return;
    window.clearTimeout(timer);
    timer = window.setTimeout(poll, immediate ? 0 : POLL_INTERVAL);
}

/**
 * Delivers one shared snapshot or error to active subscribers and schedules the next poll.
 * @returns {Promise<void>} Resolves after delivery and rescheduling; aborted polls do not notify subscribers.
 */
async function poll() {
    timer = null;
    if (!activeSubscriptions().length) return;
    const controller = new AbortController();
    pollRequest = controller;
    try {
        const snapshot = await readMonitoringSnapshot(controller.signal);
        if (!controller.signal.aborted) activeSubscriptions().forEach(item => item.onData(snapshot));
    } catch (error) {
        if (!controller.signal.aborted) activeSubscriptions().forEach(item => item.onError(error));
    } finally {
        if (pollRequest === controller) {
            pollRequest = null;
            schedule();
        }
    }
}

const visibilityChanged = () => schedule(document.visibilityState !== 'hidden');

/**
 * Pauses offscreen cards and releases observation, polling and requests when their render ends.
 * Active cards share five-second polling while the document is visible; this does not synchronously deliver a snapshot.
 *
 * @param {HTMLElement} container - Connected card content observed for viewport visibility.
 * @param {AbortSignal} signal - Render lifetime; aborting removes the subscription.
 * @param {function(MonitoringSnapshot): void} onData - Called synchronously for each successful poll while active; its return value is ignored.
 * @param {function(Error): void} onError - Called for failed polls while active; its return value is ignored.
 * @returns {function(): void} Idempotent unsubscribe function, including for an already-aborted signal.
 */
export function subscribeMonitoring(container, signal, onData, onError) {
    if (signal.aborted) return () => {};
    const subscription = { container, signal, onData, onError, visible: true };
    const observer = new IntersectionObserver(entries => {
        const wasVisible = subscription.visible;
        subscription.visible = entries.some(entry => entry.isIntersecting);
        schedule(subscription.visible && !wasVisible);
    });
    if (!subscriptions.size) document.addEventListener('visibilitychange', visibilityChanged);
    subscriptions.add(subscription);
    observer.observe(container);
    let disposed = false;
    const cleanup = () => {
        if (disposed) return;
        disposed = true;
        signal.removeEventListener('abort', cleanup);
        observer.disconnect();
        subscriptions.delete(subscription);
        if (!subscriptions.size) document.removeEventListener('visibilitychange', visibilityChanged);
        schedule();
    };
    signal.addEventListener('abort', cleanup, { once: true });
    schedule();
    return cleanup;
}
