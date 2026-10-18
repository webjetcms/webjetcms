import { newsFingerprint } from '../release-news';

const DAY = 86400000;
const REQUEST_TIMEOUT = 8000;
const CACHE_VERSION = 1;
const MAX_ITEMS = 100;
const MAX_FEED_BYTES = 256 * 1024;
const feedMemory = new Map();
const feedRequests = new Map();
const readingWrites = new WeakMap();
const pendingReadingWrites = new WeakMap();

/**
 * A validated public announcement. All visible fields are plain text.
 * @typedef {Object} ExternalNewsItem
 * @property {string} id - Stable identity shared across translations.
 * @property {string} title - Plain title.
 * @property {string} description - Plain summary.
 * @property {string} descriptionLong - Optional longer plain summary for expanded cards, empty when unavailable.
 * @property {string} link - HTTP(S) destination.
 * @property {'webjet'|'youtube'|'facebook'} source - Origin of the announcement.
 * @property {string|null} publishedAt - ISO publication date when supplied.
 * @property {string|null} image - Optional HTTPS thumbnail or image on an explicitly configured HTTP feed origin.
 */

/** Accepts explicit HTTP(S) or root-relative URLs without credentials or ambiguous separators. */
function safeUrl(value, base) {
    if (typeof value !== 'string' || value.length > 2048 || /[\\\u0000-\u001f\u007f]/.test(value)) return null;
    const trimmed = value.trim();
    if (!/^(?:https?:\/\/[^/]|\/(?!\/))/i.test(trimmed)) return null;
    try {
        const url = new URL(trimmed, base);
        if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) return null;
        return url.href;
    } catch (error) { return null; }
}

/** Resolves thumbnails against the feed directory while preserving legacy absolute HTTPS images. */
function safeImageUrl(value, base) {
    if (typeof value !== 'string' || value.length > 2048 || /[\\\u0000-\u001f\u007f]/.test(value)) return null;
    const trimmed = value.trim();
    if (!trimmed || trimmed.startsWith('//') || /^[?#]/.test(trimmed)) return null;
    if (/^[a-z][a-z\d+.-]*:/i.test(trimmed) && !/^https?:\/\/[^/]/i.test(trimmed)) return null;
    try {
        const feed = new URL(base, window.location.origin);
        const url = new URL(trimmed, feed);
        if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) return null;
        if (url.protocol === 'http:' && (feed.protocol !== 'http:' || url.origin !== feed.origin)) return null;
        return url.href;
    } catch (error) { return null; }
}

/**
 * Resolves the established localized feed URL and its refresh interval.
 * @param {Object} config - Dashboard configuration, including overviewJsonUrl and socialRefreshDays.
 * @param {string} language - Administration language; Czech uses the historical cs filename.
 * @returns {{enabled:boolean, url:string|null, refreshDays:number}} Effective loading configuration.
 */
export function socialNewsOptions(config = {}, language = 'sk') {
    const refreshDays = Number(config.socialRefreshDays ?? 3);
    const base = safeUrl(config.overviewJsonUrl, window.location.origin);
    if (!Number.isFinite(refreshDays) || refreshDays <= 0 || !base) return { enabled: false, url: null, refreshDays: 0 };
    const address = new URL(base);
    if (address.search || address.hash) {
        return { enabled: false, url: null, refreshDays: 0 };
    }
    const locale = language === 'en' ? 'en' : ['cs', 'cz'].includes(language) ? 'cs' : 'sk';
    address.pathname = `${address.pathname.replace(/\/$/, '')}/wjnews.${locale}.json`;
    return { enabled: true, url: address.href, refreshDays };
}

/** Converts legacy Markdown to text without parsing remote HTML or creating resource elements. */
function plainSummary(value) {
    if (typeof value !== 'string') return '';
    const text = value.replace(/\\n/g, '\n')
        .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
        .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
        .replace(/<[^>]*>/g, '')
        .replace(/(^|\n)\s{0,3}(?:#{1,6}\s+|>\s*|[-*+]\s+)/g, '$1')
        .replace(/(\*\*|__|`+)(.*?)\1/g, '$2')
        .replace(/(^|\s)\*([^*\n]+)\*(?=\s|[.,!?;:]|$)/g, '$1$2')
        .replace(/\s+/g, ' ').trim();
    const decoder = document.createElement('textarea');
    decoder.innerHTML = text;
    return decoder.value;
}

/** Returns a language-neutral legacy changelog identity, retaining its release anchor. */
function releaseIdentity(link) {
    const url = new URL(link);
    if (!/^\/latest\/(?:sk|cs|cz|en)\/CHANGELOG(?:-\d{4})?\/?$/i.test(url.pathname)) return null;
    const anchor = url.searchParams.get('id') || url.hash.slice(1);
    return `release:${url.hostname}${url.pathname.replace(/^\/latest\/(?:sk|cs|cz|en)\//i, '/latest/').replace(/\/$/, '')}:${anchor}`;
}

/**
 * Validates old and enriched news records while preserving publisher order.
 * @param {Object} payload - Existing {news:[{title,perex,link,...}]} feed.
 * @param {string} url - Feed URL used to resolve root-relative links and relative thumbnails.
 * @returns {ExternalNewsItem[]} Bounded, deduplicated announcements.
 * @throws {Error} If the envelope is malformed or every supplied record is invalid.
 */
export function normalizeFeed(payload, url) {
    return normalizeRecords(payload, url, plainSummary);
}

/** Validates the wire format and previously normalized cache without decoding cached text twice. */
function normalizeRecords(payload, url, summary) {
    if (!payload || !Array.isArray(payload.news)) throw new Error('Invalid news feed');
    const items = [];
    const identities = new Set();
    for (const record of payload.news.slice(0, MAX_ITEMS)) {
        if (!record || typeof record !== 'object') continue;
        const link = safeUrl(record.link, url);
        const title = typeof record.title === 'string' ? record.title.trim().slice(0, 300) : '';
        if (!link || !title || typeof record.perex !== 'string') continue;
        const source = ['youtube', 'facebook'].includes(record.source) ? record.source : 'webjet';
        const canonical = source === 'webjet' ? releaseIdentity(link) : null;
        const explicitId = typeof record.id === 'string' && record.id.trim() && record.id.length <= 256 ? record.id.trim() : null;
        const id = canonical || explicitId || link;
        if (identities.has(id)) continue;
        identities.add(id);
        const date = typeof record.publishedAt === 'string' ? Date.parse(record.publishedAt) : NaN;
        const descriptionLong = typeof record.perexLong === 'string' ? summary(record.perexLong).trim() : '';
        items.push({ id, title, description: summary(record.perex).slice(0, 1200),
            descriptionLong: Array.from(descriptionLong).slice(0, 1000).join(''), link, source,
            publishedAt: Number.isFinite(date) ? new Date(date).toISOString() : null,
            image: safeImageUrl(record.image, url) });
    }
    if (payload.news.length && !items.length) throw new Error('News feed contains no valid records');
    return items;
}

/**
 * Removes the installed release's duplicate external entry and bounds expanded cards.
 * @param {ExternalNewsItem[]} items - Validated feed records in publisher order.
 * @param {string} releaseVersion - Installed release version, for example 2026.18.
 * @returns {ExternalNewsItem[]} At most six external cards.
 */
export function filterExternalNews(items, releaseVersion) {
    const version = /^(20\d{2})\.(\d+)(?:\.(\d+))?$/.exec(String(releaseVersion || ''));
    return items.filter(item => {
        if (!version || item.source !== 'webjet' || !releaseIdentity(item.link)) return true;
        const url = new URL(item.link);
        const anchor = (url.searchParams.get('id') || url.hash.slice(1)).replace(/^_/, '');
        const compact = version[1] + version[2] + (version[3] || '');
        return anchor !== compact && !(Number(version[2]) === 0 && !version[3] && anchor === `${version[1]}00`);
    }).slice(0, 6);
}

/** Revalidates storage contents before letting cached values reach the widget. */
function cachedFeed(url) {
    const key = `webjet.externalNews.v${CACHE_VERSION}.${newsFingerprint(url)}`;
    try {
        const cache = feedMemory.get(url) || JSON.parse(window.localStorage.getItem(key) || 'null');
        if (cache?.version !== CACHE_VERSION || !Number.isFinite(cache.fetchedAt) || cache.fetchedAt <= 0 || cache.fetchedAt > Date.now() + 60000 || !Array.isArray(cache.items)) return null;
        const items = normalizeRecords({ news: cache.items.map(item => ({ ...item, perex: item.description, perexLong: item.descriptionLong })) }, url, value => value);
        return { ...cache, items };
    } catch (error) { return null; }
}

/** Stores only successfully validated responses; restricted browser storage remains optional. */
function rememberFeed(url, items) {
    const cache = { version: CACHE_VERSION, fetchedAt: Date.now(), items };
    feedMemory.set(url, cache);
    try { window.localStorage.setItem(`webjet.externalNews.v${CACHE_VERSION}.${newsFingerprint(url)}`, JSON.stringify(cache)); }
    catch (error) { /* Private browsing and storage quotas retain the in-memory cache. */ }
}

function abortError() {
    const error = new Error('News request was cancelled');
    error.name = 'AbortError';
    return error;
}

/** Reads a bounded response before parsing JSON, including chunked bodies without a length header. */
async function readFeedResponse(response) {
    const declaredSize = Number(response.headers?.get('content-length'));
    if (declaredSize > MAX_FEED_BYTES) {
        await response.body?.cancel();
        throw new Error('News feed exceeds the size limit');
    }
    let text = '';
    if (response.body?.getReader) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let size = 0;
        try {
            while (true) {
                const chunk = await reader.read();
                if (chunk.done) break;
                size += chunk.value.byteLength;
                if (size > MAX_FEED_BYTES) {
                    await reader.cancel();
                    throw new Error('News feed exceeds the size limit');
                }
                text += decoder.decode(chunk.value, { stream: true });
            }
            text += decoder.decode();
        } finally { reader.releaseLock(); }
    } else {
        text = await response.text();
        if (text.length > MAX_FEED_BYTES || new TextEncoder().encode(text).byteLength > MAX_FEED_BYTES) throw new Error('News feed exceeds the size limit');
    }
    return JSON.parse(text);
}

/** Starts one shared request, bounded even when a fetch implementation ignores abort. */
function requestFeed(url) {
    const controller = new window.AbortController();
    const job = { controller, subscribers: new Set(), promise: null };
    let timeout;
    let aborted;
    const interrupted = new Promise((resolve, reject) => {
        aborted = () => reject(abortError());
        controller.signal.addEventListener('abort', aborted, { once: true });
        timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT);
    });
    const response = (async () => {
        const result = await fetch(url, { signal: controller.signal, credentials: 'omit', referrerPolicy: 'no-referrer', cache: 'no-cache' });
        if (!result.ok) throw new Error('News feed is unavailable');
        const items = normalizeFeed(await readFeedResponse(result), url);
        if (controller.signal.aborted) throw abortError();
        rememberFeed(url, items);
        return items;
    })();
    job.promise = Promise.race([response, interrupted]).finally(() => {
        window.clearTimeout(timeout);
        controller.signal.removeEventListener('abort', aborted);
        if (feedRequests.get(url) === job) feedRequests.delete(url);
    });
    feedRequests.set(url, job);
    return job;
}

/** Allows each widget lifecycle to leave a shared request independently. */
function subscribe(job, signal) {
    return new Promise((resolve, reject) => {
        const token = {};
        job.subscribers.add(token);
        const cleanup = () => {
            signal?.removeEventListener('abort', abort);
            job.subscribers.delete(token);
        };
        const abort = () => {
            cleanup();
            if (!job.subscribers.size) job.controller.abort();
            reject(abortError());
        };
        signal?.addEventListener('abort', abort, { once: true });
        job.promise.then(items => { cleanup(); resolve(items); }, error => { cleanup(); reject(error); });
        if (signal?.aborted) abort();
    });
}

/**
 * Creates a cache-first feed reader bound to a widget's lifetime.
 * @param {{url:string, refreshDays:number, signal:AbortSignal}} options - Public URL, interval and cleanup signal.
 * @returns {{readCache:()=>ExternalNewsItem[]|null, refresh:(options?:{force?:boolean})=>Promise<ExternalNewsItem[]>}} Reader API.
 */
export function createSocialNewsClient({ url, refreshDays, signal }) {
    const enabled = Boolean(safeUrl(url, window.location.origin)) && Number.isFinite(refreshDays) && refreshDays > 0;
    return {
        readCache: () => enabled && !signal?.aborted ? cachedFeed(url)?.items || null : null,
        refresh({ force = false } = {}) {
            if (!enabled) return Promise.resolve([]);
            if (signal?.aborted) return Promise.reject(abortError());
            const cache = cachedFeed(url);
            if (!force && cache && Date.now() - cache.fetchedAt < refreshDays * DAY) return Promise.resolve(cache.items);
            const current = feedRequests.get(url);
            return subscribe(current && !current.controller.signal.aborted ? current : requestFeed(url), signal);
        }
    };
}

/** Namespaces account acknowledgements by the feed base, independently of its language filename. */
function readingKey(feedUrl) {
    const url = new URL(feedUrl, window.location.origin);
    url.pathname = url.pathname.replace(/wjnews\.[a-z]+\.json$/i, '');
    url.hash = ''; url.search = '';
    return `dashboard.externalNews.${newsFingerprint(url.href)}`;
}

/** Reads a bounded list of content identities without disturbing release-news preferences. */
function readIdentities(user, key) {
    try {
        const saved = JSON.parse(user?.adminSettings?.[key] || 'null');
        return Array.isArray(saved?.seen) ? saved.seen.filter(value => typeof value === 'string' && /^[a-f0-9]{1,16}$/.test(value)).slice(-MAX_ITEMS) : [];
    } catch (error) { return []; }
}

/**
 * Checks whether the account has opened every currently displayed external announcement.
 * @param {ExternalNewsItem[]} items - Cards actually presented by the widget.
 * @param {string} feedUrl - Localized feed URL.
 * @returns {{isNew:boolean,key:string,value:string,changed:boolean}} Account state ready for acknowledgement.
 */
export function getExternalNewsState(items, feedUrl) {
    const key = readingKey(feedUrl);
    const saved = readIdentities(window.currentUser, key);
    const hashes = items.map(item => newsFingerprint(item.id));
    const isNew = hashes.some(hash => !saved.includes(hash));
    const seen = [...new Set([...saved.filter(hash => !hashes.includes(hash)), ...hashes])].slice(-MAX_ITEMS);
    return { key, value: JSON.stringify({ seen, readAt: Date.now() }), isNew, changed: isNew };
}

/**
 * Acknowledges displayed cards through the existing account preferences endpoint.
 * Serialized writes merge identities so simultaneous widget updates cannot lose an acknowledgement.
 * @param {ExternalNewsItem[]} items - Successfully displayed cards to acknowledge.
 * @param {string} feedUrl - Localized feed URL.
 * @returns {Promise<boolean>} Whether acknowledgement was already stored or saved successfully.
 */
export function saveExternalNewsSeen(items, feedUrl) {
    const user = window.currentUser;
    if (!user?.adminSettings || !items.length) return Promise.resolve(true);
    const pendingKey = `${readingKey(feedUrl)}:${[...new Set(items.map(item => newsFingerprint(item.id)))].sort().join(',')}`;
    let pending = pendingReadingWrites.get(user);
    if (!pending) {
        pending = new Map();
        pendingReadingWrites.set(user, pending);
    }
    if (pending.has(pendingKey)) return pending.get(pendingKey);
    const previous = readingWrites.get(user) || Promise.resolve();
    const request = previous.then(async () => {
        if (window.currentUser !== user) return false;
        const state = getExternalNewsState(items, feedUrl);
        if (!state.changed) return true;
        try {
            const response = await fetch('/admin/rest/admin-settings/', {
                method: 'POST', credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken || '' },
                body: JSON.stringify({ label: state.key, value: state.value })
            });
            if (!response.ok || await response.json() !== true) throw new Error('News preferences could not be saved');
            user.adminSettings[state.key] = state.value;
            return true;
        } catch (error) {
            window.WJ.notifyError?.(window.WJ.translate('admin.dashboard.news.js'), window.WJ.translate('admin.dashboard.saveError.js'));
            return false;
        }
    });
    readingWrites.set(user, request);
    pending.set(pendingKey, request);
    request.finally(() => {
        if (readingWrites.get(user) === request) readingWrites.delete(user);
        pending.delete(pendingKey);
        if (!pending.size) pendingReadingWrites.delete(user);
    });
    return request;
}
