import $ from 'jquery';

const selector = '[data-ai-tooltip]';

/** Sets a plain-text chat tooltip without enabling the browser's native title tooltip. */
export function setChatTooltip(element, label) {
    const value = label || '';
    if (element.getAttribute('data-ai-tooltip') === value) return;
    disposeChatTooltips(element);
    element.setAttribute('data-ai-tooltip', value);
    element.setAttribute('data-bs-title', value);
    initChatTooltips(element);
}

/** Initializes newly mounted controls and removes tooltips from hidden or disabled controls. */
export function initChatTooltips(container) {
    $(container).find(selector).addBack(selector).each((index, element) => {
        const active = element.isConnected && element.getAttribute('data-ai-tooltip')
            && !element.disabled && element.style.display !== 'none' && !element.closest('[hidden]');
        if (!active) disposeChatTooltips(element);
        else if (!window.bootstrap.Tooltip.getInstance(element)) WJ.initTooltip($(element), 'tooltip-ai');
    });
}

/** Releases tooltip timers and overlays before chat elements are removed or hidden. */
export function disposeChatTooltips(container) {
    $(container).find(selector).addBack(selector).each((index, element) => {
        window.bootstrap.Tooltip.getInstance(element)?.dispose();
        $(element).removeAttr('aria-describedby').off('.wjTooltipA11y');
    });
}
