/** Creates text-only widget content; data and preferences never become HTML. */
export function node(tag, className = "", text) {
    const result = document.createElement(tag);
    if (className) result.className = className;
    if (text !== undefined && text !== null) result.textContent = String(text);
    return result;
}

export const text = (context, key) => context.translate(`admin.dashboard.${key}.js`);

/** Resolves only HTTP administration links on the current origin. */
export function localUrl(value) {
    if (typeof value !== "string" || !value.trim()) return null;
    try {
        const url = new URL(value, window.location.origin);
        if (url.origin !== window.location.origin || !["http:", "https:"].includes(url.protocol)) return null;
        return `${url.pathname}${url.search}${url.hash}`;
    } catch (error) { return null; }
}

/** Accepts explicit local or HTTP(S) shortcuts without executable or ambiguous URL forms. */
export function shortcutUrl(value) {
    if (typeof value !== "string" || /[\\\u0000-\u001f\u007f]/.test(value)) return null;
    const target = value.trim();
    if (!/^(?:https?:\/\/[^/]|\/(?!\/))/i.test(target) || target.length > 1024) return null;
    try {
        const url = new URL(target, window.location.origin);
        if (!["http:", "https:"].includes(url.protocol) || url.username || url.password) return null;
        return target.startsWith('/') ? `${url.pathname}${url.search}${url.hash}` : url.href;
    } catch (error) { return null; }
}

export function link(title, href, className = "") {
    const target = localUrl(href);
    if (!target) return node("span", className, title);
    const result = node("a", className, title);
    result.href = target;
    return result;
}

export function icon(name) {
    const className = String(name || '').split(/\s+/).find(value => /^ti-[a-z0-9-]+$/.test(value)) || 'ti-link';
    const result = node("i", `ti ${className}`);
    result.setAttribute("aria-hidden", "true");
    return result;
}

export function number(value) {
    return Number.isFinite(Number(value)) && value !== null ? Number(value).toLocaleString((window.userLng === "cz" ? "cs" : window.userLng) || "sk") : "—";
}

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

export function footer(container, context, href, key = "all") {
    container.append(link(text(context, key), href, "md-dashboard-widget__more"));
}

/** Adds a labeled setting without sharing input identifiers between instances. */
export function field(container, label, values, value, inputType = "select") {
    const wrapper = node("label", "d-block mb-3");
    wrapper.append(node("span", "form-label d-block", label));
    const input = node(inputType === "select" ? "select" : "input", inputType === "select" ? "form-select" : "form-control");
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

/** Builds an accessible compact preview table with no nested scrolling. */
export function table(container, headers, rows) {
    const result = node("table", "table table-sm md-dashboard-widget__table");
    const head = node("thead");
    const heading = node("tr");
    headers.forEach(label => {
        const th = node("th", "", label);
        th.scope = "col";
        heading.append(th);
    });
    head.append(heading);
    const body = node("tbody");
    rows.forEach(values => {
        const row = node("tr");
        values.forEach(value => {
            const cell = node("td");
            cell.append(value instanceof Node ? value : document.createTextNode(value == null ? "" : String(value)));
            row.append(cell);
        });
        body.append(row);
    });
    result.append(head, body);
    container.append(result);
    return result;
}

/** Fetches an authorized dashboard projection; aborts when the widget is replaced. */
export async function fetchData(type, options = {}, signal) {
    const params = new URLSearchParams();
    Object.entries(options).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") params.set(key, value);
    });
    const response = await fetch(`/admin/rest/dashboard/data/${encodeURIComponent(type)}?${params}`, {
        credentials: "same-origin", signal, headers: { Accept: "application/json", "X-CSRF-Token": window.csrfToken }
    });
    if (!response.ok) {
        const details = await response.json().catch(() => ({}));
        const error = new Error(`Dashboard data request failed (${response.status})`);
        error.dashboardReason = response.status === 404 ? 'selection-unavailable' : details.reason;
        throw error;
    }
    return response.json();
}
