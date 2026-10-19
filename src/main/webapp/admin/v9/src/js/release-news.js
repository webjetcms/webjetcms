const pendingNewsSaves = new WeakMap();
let cachedAnnouncement;
let cachedReleaseNews;

/**
 * Extracts previews while retaining the complete rendered Markdown for release details.
 * Older paragraph announcements remain readable until their translations are converted to lists.
 * @param {string} html - Localized announcement rendered by WebJET's Markdown renderer.
 * @param {string} [releaseVersion=''] - Version used when the announcement has no version heading.
 * @returns {{version: string, items: {title: string, description: string}[], html: string}} Release content.
 */
export function releaseNews(html, releaseVersion = '') {
    const parsed = new DOMParser().parseFromString(html, 'text/html');
    const version = parsed.body.textContent.match(/\b20\d{2}\.\d+(?:\.\d+)?\b/)?.[0] || releaseVersion;
    const items = [...parsed.body.querySelectorAll('li')].filter(item => !item.parentElement.closest('li')).map(item => {
        const copy = item.cloneNode(true);
        const heading = copy.querySelector('b, strong');
        const title = heading?.textContent.trim() || '';
        heading?.remove();
        return { title, description: copy.textContent.trim().replace(/^[–—:-]\s*/, '') };
    });
    if (!items.length) {
        parsed.body.querySelectorAll('br').forEach(br => br.replaceWith(parsed.createTextNode('\n')));
        parsed.body.querySelectorAll('p, h1, h2, h3, h4, h5, h6, blockquote').forEach(block => block.append(parsed.createTextNode('\n\n')));
        for (const paragraph of parsed.body.textContent.replace(/\\n/g, '\n').split(/\n\s*\n/).map(value => value.trim()).filter(Boolean)) {
            items.push({ title: '', description: paragraph });
        }
    }
    return { version, items, html };
}

/**
 * Renders shared layout announcements once so the Help menu and dashboard use identical fingerprints.
 * @returns {{version: string, items: {title: string, description: string}[], html: string}} Current release content.
 */
export function getReleaseNews() {
    const announcement = window.adminHelpContext?.announcement || '';
    if (announcement !== cachedAnnouncement) {
        cachedReleaseNews = releaseNews(window.WJ.parseMarkdown('\n' + announcement.replace(/\\n/g, '\n') + '\n\n', { link: true, imgSrcPrefix: '' }));
        cachedAnnouncement = announcement;
    }
    return cachedReleaseNews;
}

/**
 * Produces a non-security FNV-1a fingerprint without requiring HTTPS-only Web Crypto APIs.
 * @param {string} content - Complete localized announcement, including formatting changes.
 * @returns {string} Stable 64-bit content fingerprint.
 */
export function newsFingerprint(content) {
    let hash = 14695981039346656037n;
    for (let index = 0; index < content.length; index++) hash = BigInt.asUintN(64, (hash ^ BigInt(content.charCodeAt(index))) * 1099511628211n);
    return hash.toString(16);
}

/**
 * Checks whether the signed-in account has acknowledged this exact localized text.
 * Legacy firstSeen records describe exposure, not reading, and require one explicit acknowledgement.
 * @param {string} html - Current announcement HTML.
 * @param {number} [now=Date.now()] - Current time in milliseconds.
 * @returns {{key: string, value: string, changed: boolean, isNew: boolean}} State to persist only after displaying the full text.
 */
export function newsSeenState(html, now = Date.now()) {
    const key = `dashboard.news.${window.userLng || 'sk'}`;
    const fingerprint = newsFingerprint(html);
    let saved;
    try { saved = JSON.parse(window.currentUser?.adminSettings?.[key] || 'null'); }
    catch (error) { /* Invalid preferences cannot acknowledge the current text. */ }
    const changed = saved?.fingerprint !== fingerprint || !Number.isFinite(saved?.readAt) || saved.readAt <= 0;
    return { key, value: JSON.stringify({ fingerprint, readAt: now }), changed, isNew: changed };
}

/**
 * Persists an explicit acknowledgement once per account, language and content, independently of the widget's lifecycle.
 * Writes for changed text are serialized so a delayed response cannot restore an older fingerprint.
 * Failed writes leave account settings unchanged and can be retried on the next expansion.
 * @param {{key: string, value: string, changed: boolean}} state - Reading state from newsSeenState.
 * @returns {Promise<boolean>} Whether the record is already saved or was saved successfully.
 */
export function saveNewsSeen(state) {
    if (!state.changed) return Promise.resolve(true);
    const user = window.currentUser;
    if (!user?.adminSettings) return Promise.resolve(false);
    const fingerprint = JSON.parse(state.value).fingerprint;
    let pending = pendingNewsSaves.get(user);
    if (!pending) {
        pending = new Map();
        pendingNewsSaves.set(user, pending);
    }
    const previous = pending.get(state.key);
    if (previous?.fingerprint === fingerprint) return previous.promise;
    const persist = () => {
        if (window.currentUser !== user) return Promise.resolve(false);
        let saved;
        try { saved = JSON.parse(user.adminSettings[state.key] || 'null'); }
        catch (error) { /* Invalid preferences are replaced by the valid reading state. */ }
        if (saved?.fingerprint === fingerprint && Number.isFinite(saved.readAt) && saved.readAt > 0) return Promise.resolve(true);
        return persistNewsSeen(state, user);
    };
    const request = (previous ? previous.promise.then(persist) : persist()).finally(() => {
        if (pending.get(state.key)?.promise === request) pending.delete(state.key);
    });
    pending.set(state.key, { fingerprint, promise: request });
    return request;
}

/** Saves one reading record and reports a shared failure only once. */
async function persistNewsSeen(state, user) {
    try {
        const response = await fetch('/admin/rest/admin-settings/', {
            method: 'POST', credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken || '' },
            body: JSON.stringify({ label: state.key, value: state.value })
        });
        if (!response.ok || await response.json() !== true) throw new Error('Release preferences could not be saved');
        if (user?.adminSettings) user.adminSettings[state.key] = state.value;
        return true;
    } catch (error) {
        window.WJ.notifyError?.(window.WJ.translate('admin.dashboard.news.js'), window.WJ.translate('admin.dashboard.saveError.js'));
        return false;
    }
}
