/** Rejects relative or unreadable declarations instead of mistaking resolved viewport units for fixed pixels. */
function hasFixedPixelWidth(container, property, resolvedWidth) {
    const doc = container.ownerDocument;
    const view = doc.defaultView;
    let found = false;
    function declaration(style) {
        const value = style.getPropertyValue(property).trim();
        if (style.getPropertyValue("all")) return false;
        if (!value) return true;
        found ||= Math.abs(parseFloat(value) - resolvedWidth) <= 0.5;
        return /^\d+(?:\.\d+)?px$/.test(value);
    }
    function rules(list) {
        for (const rule of list) {
            if (rule.media && !view.matchMedia(rule.media.mediaText).matches) continue;
            if (rule.constructor.name === "CSSSupportsRule" && !view.CSS.supports(rule.conditionText)) continue;
            if (rule.selectorText && rule.style && (rule.style.getPropertyValue(property) || rule.style.getPropertyValue("all"))) {
                if (rule.selectorText.includes("&") || (container.matches(rule.selectorText) && !declaration(rule.style))) return false;
            }
            if (rule.styleSheet && !rules(rule.styleSheet.cssRules)) return false;
            if (rule.cssRules && !rules(rule.cssRules)) return false;
        }
        return true;
    }
    try {
        if (!declaration(container.style)) return false;
        for (const sheet of [...doc.styleSheets, ...(doc.adoptedStyleSheets || [])]) {
            if (sheet.disabled || (sheet.media.mediaText && !view.matchMedia(sheet.media.mediaText).matches)) continue;
            if (!rules(sheet.cssRules)) return false;
        }
        return found;
    } catch (error) {
        // Cross-origin stylesheets and unsupported selector syntax cannot prove a fixed width.
        return false;
    }
}

/** Only a fixed pixel width centered in the document can share a horizontal origin. */
export function fixedContainerWidth(container, rect) {
    const doc = container.ownerDocument.documentElement;
    const view = container.ownerDocument.defaultView;
    const style = view.getComputedStyle(container);
    const pageWidth = doc.clientWidth;
    if (rect.width <= 0 || rect.height <= 0 || style.animationName !== "none"
            || Math.abs(rect.left + view.scrollX - (pageWidth - rect.width) / 2) > 0.5
            || doc.scrollWidth > pageWidth) return 0;
    const extra = style.boxSizing === "border-box" ? 0 : parseFloat(style.paddingLeft) + parseFloat(style.paddingRight)
        + parseFloat(style.borderLeftWidth) + parseFloat(style.borderRightWidth);
    const fixed = ["max-width", "width"].some(property => {
        const value = style.getPropertyValue(property);
        return value.endsWith("px") && Math.abs(rect.width - parseFloat(value) - extra) <= 0.5
            && hasFixedPixelWidth(container, property, parseFloat(value));
    });
    if (!fixed) return 0;
    return Math.round(rect.width);
}

