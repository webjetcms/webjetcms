const enhancedTextareas = new WeakMap();

/**
 * Adds a decorative line-number gutter while preserving native textarea navigation.
 * Repeated calls refresh the existing gutter after programmatic value changes.
 * @param {HTMLTextAreaElement|null} textarea Textarea to enhance.
 * @returns {{refresh: function(): void}|undefined} Gutter refresh handle.
 */
export function initTextareaLineNumbers(textarea) {
    if (!textarea) return;
    const existing = enhancedTextareas.get(textarea);
    if (existing) {
        existing.refresh();
        return existing;
    }

    const wrapper = document.createElement("div");
    wrapper.className = "md-textarea-editor";
    const gutter = document.createElement("div");
    gutter.className = "md-textarea-editor__lines";
    gutter.setAttribute("aria-hidden", "true");
    textarea.before(wrapper);
    wrapper.append(gutter, textarea);

    const refresh = () => {
        const count = textarea.value.split("\n").length;
        gutter.textContent = Array.from({length: count}, (_, index) => index + 1).join("\n");
        gutter.scrollTop = textarea.scrollTop;
    };
    textarea.addEventListener("input", refresh);
    textarea.addEventListener("change", refresh);
    textarea.addEventListener("scroll", () => { gutter.scrollTop = textarea.scrollTop; });
    const handle = {refresh};
    enhancedTextareas.set(textarea, handle);
    refresh();
    return handle;
}
