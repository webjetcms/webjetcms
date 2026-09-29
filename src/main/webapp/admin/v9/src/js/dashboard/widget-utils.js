/**
 * Creates text-only widget content; data and preferences never become HTML.
 * @param {string} tag - HTML element name.
 * @param {string} [className=""] - CSS classes assigned to the element.
 * @param {unknown} [text] - Value converted to text; null and undefined leave the element empty.
 * @returns {HTMLElement} A detached element.
 */
export function node(tag, className = "", text) {
    const result = document.createElement(tag);
    if (className) result.className = className;
    if (text !== undefined && text !== null) result.textContent = String(text);
    return result;
}

export const text = (context, key, ...params) => context.translate(`admin.dashboard.${key}.js`, ...params);

/**
 * Page fields shared by search, activity and recent-page previews.
 * @typedef {Object} PagePreview
 * @property {string} [title] - Page title rendered as text.
 * @property {string} [fullPath] - Page path, optionally ending in the title.
 * @property {string|null} [perexImage] - Local image path, if available.
 */

/**
 * Presents the authorized page thumbnail with a decorative file icon fallback.
 * @param {PagePreview} page - Page whose root-relative image is checked against the current origin.
 * @returns {HTMLSpanElement} Thumbnail wrapper retaining the fallback if the image is absent or fails to load.
 */
function pageThumbnail(page) {
    const thumbnail = node('span', 'md-dashboard-widget__page-image');
    const fallback = icon('ti-file-text');
    thumbnail.append(fallback);
    const source = typeof page.perexImage === 'string' && page.perexImage.startsWith('/') && !page.perexImage.startsWith('//') ? localUrl(page.perexImage) : null;
    if (source) {
        const image = node('img');
        image.alt = '';
        image.loading = 'lazy';
        image.width = 38;
        image.height = 38;
        image.addEventListener('error', () => { image.remove(); fallback.hidden = false; }, { once: true });
        fallback.hidden = true;
        image.src = `/thumb${source}?w=76&h=76&ip=6`;
        thumbnail.append(image);
    }
    return thumbnail;
}

/**
 * Combines the page image, title and parent path without duplicating its title in the path.
 * @param {PagePreview} page - Text and optional image to display.
 * @param {string} [href] - Local destination; omission or an invalid URL produces a non-link preview.
 * @returns {HTMLAnchorElement|HTMLSpanElement} A detached preview containing safe text and a thumbnail fallback.
 */
export function pagePreview(page, href) {
    const target = link('', href, 'md-dashboard-widget__page-preview');
    const content = node('span', 'md-dashboard-widget__page-content');
    const section = page.fullPath?.endsWith(`/${page.title}`) ? page.fullPath.slice(0, -(page.title.length + 1)) || '/' : page.fullPath;
    content.append(node('span', 'md-dashboard-widget__page-title', page.title));
    if (section) content.append(node('span', 'md-dashboard-widget__page-section', section));
    target.title = page.fullPath || page.title;
    target.append(pageThumbnail(page), content);
    return target;
}

/**
 * Lets a native list consume scroll gestures before the administration's smooth scrollbar.
 * Preserves default browser scrolling and removes the installed listeners when the signal aborts.
 *
 * @param {HTMLElement} list - Scrollable list or results menu.
 * @param {AbortSignal} signal - Active render signal governing listener cleanup.
 */
export function containNativeScroll(list, signal) {
    const overflows = () => list.scrollHeight > list.clientHeight;
    const wheel = event => { if (overflows()) event.stopPropagation(); };
    const keyboard = event => {
        const scrolling = ['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Home', 'End', ' '].includes(event.key);
        if (scrolling && (overflows() || (event.key === ' ' && event.target.closest('button')))) event.stopPropagation();
    };
    let ownsTouch = false;
    const touchStart = event => { ownsTouch = overflows(); if (ownsTouch) event.stopPropagation(); };
    const touchMove = event => { if (ownsTouch) event.stopPropagation(); };
    const touchEnd = event => { if (ownsTouch) event.stopPropagation(); if (!event.touches?.length) ownsTouch = false; };
    const listeners = [['wheel', wheel], ['keydown', keyboard], ['touchstart', touchStart], ['touchmove', touchMove], ['touchend', touchEnd], ['touchcancel', touchEnd]];
    listeners.forEach(([type, handler]) => list.addEventListener(type, handler, { passive: true }));
    signal.addEventListener('abort', () => listeners.forEach(([type, handler]) => list.removeEventListener(type, handler)), { once: true });
}

/**
 * Resolves only HTTP administration links on the current origin.
 * @param {unknown} value - Candidate absolute or relative URL; non-string and blank values are rejected.
 * @returns {string|null} A local path with query and fragment, an absolute same-origin URL when needed to avoid a network-path reference, or null.
 */
export function localUrl(value) {
    if (typeof value !== "string" || !value.trim()) return null;
    try {
        const url = new URL(value, window.location.origin);
        if (url.origin !== window.location.origin || !["http:", "https:"].includes(url.protocol)) return null;
        const path = `${url.pathname}${url.search}${url.hash}`;
        // Keep the origin when dot-segment normalization leaves a network-path reference.
        return url.pathname.startsWith('//') ? `${url.origin}${path}` : path;
    } catch (error) { return null; }
}

/**
 * Accepts explicit local or HTTP(S) shortcuts without executable or ambiguous URL forms.
 * Trims surrounding whitespace and rejects credentials, control characters, backslashes,
 * protocol-relative URLs and destinations longer than 1024 characters.
 *
 * @param {unknown} value - Candidate shortcut destination.
 * @returns {string|null} A normalized destination, or null when the input is invalid.
 */
export function shortcutUrl(value) {
    if (typeof value !== "string" || /[\\\u0000-\u001f\u007f]/.test(value)) return null;
    const target = value.trim();
    if (!/^(?:https?:\/\/[^/]|\/(?!\/))/i.test(target) || target.length > 1024) return null;
    try {
        const url = new URL(target, window.location.origin);
        if (!["http:", "https:"].includes(url.protocol) || url.username || url.password) return null;
        return target.startsWith('/') ? localUrl(target) : url.href;
    } catch (error) { return null; }
}

/**
 * Creates a same-origin link, falling back to plain text when its destination is invalid.
 * @param {unknown} title - Value displayed as text.
 * @param {string} [href] - Candidate local destination.
 * @param {string} [className=""] - Classes applied to either the anchor or fallback span.
 * @returns {HTMLAnchorElement|HTMLSpanElement} A detached link or text wrapper.
 */
export function link(title, href, className = "") {
    const target = localUrl(href);
    if (!target) return node("span", className, title);
    const result = node("a", className, title);
    result.href = target;
    return result;
}

/**
 * Selects the first valid Tabler class and creates a decorative icon, defaulting to ti-link.
 * @param {string} [name] - Icon class or whitespace-separated class list.
 * @returns {HTMLElement} An icon hidden from assistive technology.
 */
export function icon(name) {
    const className = String(name || '').split(/\s+/).find(value => /^ti-[a-z0-9-]+$/.test(value)) || 'ti-link';
    const result = node("i", `ti ${className}`);
    result.setAttribute("aria-hidden", "true");
    return result;
}

/**
 * Formats a finite numeric value in the administration locale, or displays an em dash when unavailable.
 * @param {number|string|null|undefined} value - Value to coerce to a number; null is treated as unavailable.
 * @returns {string} The localized number or an em dash.
 */
export function number(value) {
    return Number.isFinite(Number(value)) && value !== null ? Number(value).toLocaleString((window.userLng === "cz" ? "cs" : window.userLng) || "sk") : "—";
}

/**
 * Formats a date in the administration locale while retaining unparseable values as text.
 * @param {number|string|Date|null} [value] - Epoch milliseconds or another Date-compatible value; empty values produce an empty string.
 * @param {boolean} [withTime=true] - Whether to include hours and minutes.
 * @returns {string} The localized date, original invalid value as text, or an empty string.
 */
export function date(value, withTime = true) {
    if (value === undefined || value === null || value === "") return "";
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return String(value);
    return parsed.toLocaleString((window.userLng === "cz" ? "cs" : window.userLng) || "sk", withTime
        ? { day: "numeric", month: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit" }
        : { day: "numeric", month: "numeric", year: "numeric" });
}

export function empty(container, context, key = "empty") {
    container.append(node("p", "text-muted mb-2", text(context, key)));
}

let nextFieldId = 0;

/**
 * Adds a labeled setting without sharing input identifiers between instances.
 * @param {HTMLElement} container - Parent to receive the field wrapper.
 * @param {string} label - Visible label associated with the generated input ID.
 * @param {(string|number)[][]} values - Select choices as value/title pairs; ignored for other input types.
 * @param {string|number|null|undefined} value - Initial value; null or undefined becomes an empty string.
 * @param {string} [inputType="select"] - Select mode or an HTML input type; inputs start with a 120-character limit.
 * @returns {HTMLSelectElement|HTMLInputElement} The attached control for reading values and adding listeners.
 */
export function field(container, label, values, value, inputType = "select") {
    const wrapper = node("div", "md-dashboard__field mb-3");
    const caption = node("label", "form-label d-block", label);
    const input = node(inputType === "select" ? "select" : "input", inputType === "select" ? "form-select" : "form-control");
    input.id = `dashboard-field-${++nextFieldId}`;
    caption.htmlFor = input.id;
    wrapper.append(caption);
    if (inputType === "select") {
        values.forEach(([id, title]) => {
            const option = node("option", "", title);
            option.value = id;
            input.append(option);
        });
    } else {
        input.type = inputType;
        input.maxLength = 120;
    }
    input.value = value == null ? "" : String(value);
    wrapper.append(input);
    container.append(wrapper);
    return input;
}

/**
 * Builds an accessible compact preview table with no nested scrolling.
 * @param {HTMLElement} container - Parent to receive the table.
 * @param {string[]} headers - Column labels rendered as text with column scope.
 * @param {unknown[][]} rows - Cell values; DOM nodes are moved into cells, other values become text, and nullish values become empty text.
 * @param {number[]} [numericColumns=[]] - Zero-based columns receiving numeric alignment classes.
 * @returns {HTMLTableElement} The attached table.
 */
export function table(container, headers, rows, numericColumns = []) {
    const result = node("table", "table table-sm md-dashboard-widget__table");
    const head = node("thead");
    const heading = node("tr");
    headers.forEach((label, index) => {
        const th = node("th", numericColumns.includes(index) ? "md-dashboard-widget__table-number" : "", label);
        th.scope = "col";
        heading.append(th);
    });
    head.append(heading);
    const body = node("tbody");
    rows.forEach(values => {
        const row = node("tr");
        values.forEach((value, index) => {
            const cell = node("td", numericColumns.includes(index) ? "md-dashboard-widget__table-number" : "");
            cell.append(value instanceof Node ? value : document.createTextNode(value == null ? "" : String(value)));
            row.append(cell);
        });
        body.append(row);
    });
    result.append(head, body);
    container.append(result);
    return result;
}

/**
 * Fetches an existing module endpoint with the shared request and error handling.
 * Sends same-origin credentials and the CSRF header. Error responses retain their reason as
 * dashboardReason, with permission and missing-selection failures normalized for widget feedback.
 *
 * @param {string} url - Module endpoint returning a JSON object or array.
 * @param {AbortSignal} signal - Signal used to cancel the request and response reading.
 * @returns {Promise<Object|Object[]>} The parsed successful response.
 * @throws {Error} If the HTTP response is unsuccessful or its JSON contains a truthy error field.
 */
export async function fetchJson(url, signal) {
    const response = await fetch(url, {
        credentials: "same-origin", signal, headers: { Accept: "application/json", "X-CSRF-Token": window.csrfToken }
    });
    const details = response.ok ? await response.json() : await response.json().catch(() => ({}));
    if (!response.ok || details.error) {
        const error = new Error(details.error || `Dashboard data request failed (${response.status})`);
        error.dashboardReason = details.reason;
        if (response.status === 403 || details.error === 'Access Denied' || details.error === 'Access is denied') error.dashboardReason = 'permission-denied';
        if (response.status === 404) error.dashboardReason = 'selection-unavailable';
        throw error;
    }
    return details;
}
