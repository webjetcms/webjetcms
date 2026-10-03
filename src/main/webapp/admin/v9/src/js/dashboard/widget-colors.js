import { node, text } from './widget-utils';
import 'color-dialog-box';

const WIDGET_BACKGROUND_COLORS = {
    white: '#ffffff', gray: 'var(--wj-nice-gray)',
    yellow: 'var(--wj-dashboard-widget-yellow)', 'figma-cream': 'var(--wj-dashboard-widget-cream)',
    peach: 'var(--wj-dashboard-widget-peach)', pink: 'var(--wj-dashboard-widget-pink)',
    'figma-lavender': 'var(--wj-dashboard-widget-lavender)', 'figma-blue': 'var(--wj-dashboard-widget-blue)',
    'light-blue': 'var(--wj-dashboard-widget-light-blue)', cyan: 'var(--wj-dashboard-widget-cyan)',
    'figma-mint': 'var(--wj-dashboard-widget-mint)', green: 'var(--wj-dashboard-widget-green)'
};
// Previously saved palette choices use the merged shade; absent choices still keep the widget's original CSS.
const WIDGET_BACKGROUND_ALIASES = {
    mint: 'figma-mint', lavender: 'figma-lavender', blue: 'figma-blue', amber: 'figma-cream', sand: 'figma-cream', rose: 'gray'
};
const WIDGET_HEX_COLOR = /^#[a-f0-9]{6}(?:[a-f0-9]{2})?$/i;

/** Returns an allowlisted background or an empty value that restores the widget's original CSS. */
export function widgetBackground(value) {
    if (typeof value !== 'string') return '';
    if (Object.hasOwn(WIDGET_BACKGROUND_ALIASES, value)) value = WIDGET_BACKGROUND_ALIASES[value];
    return WIDGET_HEX_COLOR.test(value) ? value.toLowerCase() : Object.hasOwn(WIDGET_BACKGROUND_COLORS, value) ? WIDGET_BACKGROUND_COLORS[value] : '';
}

/** Checks the dashboard's muted text against a custom color composited over the white page. */
function readableWidgetColor(value) {
    const alpha = value.length === 9 ? parseInt(value.slice(7), 16) / 255 : 1;
    const linear = channel => channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
    const channels = value.slice(1, 7).match(/.{2}/g).map(channel => linear((parseInt(channel, 16) * alpha + 255 * (1 - alpha)) / 255));
    const background = channels.reduce((sum, channel, index) => sum + channel * [0.2126, 0.7152, 0.0722][index], 0);
    const foreground = [96, 102, 121].reduce((sum, channel, index) => sum + linear(channel / 255) * [0.2126, 0.7152, 0.0722][index], 0);
    return (background + 0.05) / (foreground + 0.05) >= 4.5;
}

/**
 * Adds shared background settings with a live preview and the same custom picker used by shortcuts.
 * @param {Object} args - Settings dialog context.
 * @param {HTMLElement} args.container - Dialog body receiving the controls.
 * @param {import('./model').WidgetInstance} args.instance - Widget whose default surface is previewed.
 * @param {import('./registry').WidgetContext} args.context - Translation and widget context.
 * @returns {{read: function(): string, destroy: function(): void}} Selected background and picker cleanup.
 */
export function createWidgetColorSettings({ container, instance, context }) {
    const colors = node('fieldset', 'md-dashboard__widget-colors md-dashboard__shortcut-colors');
    colors.append(node('legend', 'form-label', text(context, 'backgroundColor')));
    const stored = instance.options?.backgroundColor;
    let previous = widgetBackground(stored) ? WIDGET_BACKGROUND_ALIASES[stored] || stored : 'default';
    let custom = WIDGET_HEX_COLOR.test(previous) ? previous.toLowerCase() : '#f5f2ff';
    const choice = (value, className) => {
        const label = node('label', className);
        const radio = node('input', 'visually-hidden');
        radio.type = 'radio';
        radio.name = `dashboard-background-${instance.id}`;
        radio.value = value;
        radio.checked = value === (WIDGET_HEX_COLOR.test(previous) ? 'custom' : previous);
        radio.setAttribute('aria-label', text(context, 'backgroundColor.' + value));
        const face = node('span');
        face.title = radio.getAttribute('aria-label');
        label.append(radio, face);
        colors.append(label);
        return { radio, face };
    };
    const defaultChoice = choice('default', 'md-dashboard__shortcut-icon-choice md-dashboard__shortcut-custom-color');
    const defaultSwatch = node('span', 'md-dashboard__shortcut-custom-color-swatch');
    defaultChoice.face.append(defaultSwatch, document.createTextNode(text(context, 'backgroundColor.default')));
    for (const value of Object.keys(WIDGET_BACKGROUND_COLORS)) {
        choice(value, 'md-dashboard__shortcut-swatch').face.style.backgroundColor = widgetBackground(value);
    }
    const customChoice = choice('custom', 'md-dashboard__shortcut-icon-choice md-dashboard__shortcut-custom-color');
    customChoice.radio.setAttribute('aria-haspopup', 'dialog');
    customChoice.radio.setAttribute('aria-expanded', 'false');
    const customSwatch = node('span', 'md-dashboard__shortcut-custom-color-swatch');
    customChoice.face.append(customSwatch, document.createTextNode(text(context, 'shortcutIconCustom')));
    const template = node('template');
    template.innerHTML = '<color-picker></color-picker>';
    const picker = template.content.firstElementChild;
    for (const [attribute, key] of [['title', 'title'], ['hue', 'hue'], ['saturation', 'saturation'], ['lightness', 'lightness'], ['opacity', 'alpha'], ['ok', 'ok']]) {
        picker.setAttribute('label-' + attribute, context.translate('datatables.field.color.' + key + '.js'));
    }
    picker.id = `dashboard-background-picker-${instance.id}`;
    colors.append(picker, node('small', 'form-text', text(context, 'backgroundColorHint')));
    const preview = node('div', 'md-dashboard__widget md-dashboard__color-preview');
    preview.dataset.widgetType = instance.type;
    preview.append(node('strong', '', text(context, 'backgroundColorPreview')), node('small', '', text(context, 'backgroundColorPreviewText')));
    const warning = node('p', 'form-text text-danger');
    warning.setAttribute('aria-live', 'polite');
    container.append(colors, preview, warning);
    defaultSwatch.style.backgroundColor = window.getComputedStyle(preview).backgroundColor;
    const dialog = picker.shadowRoot?.querySelector('dialog');
    const heading = dialog?.querySelector('h3');
    if (heading) {
        heading.id = picker.id + '-title';
        dialog.setAttribute('aria-labelledby', heading.id);
    }
    const selected = () => customChoice.radio.checked ? custom : colors.querySelector('input:checked').value;
    const update = () => {
        preview.style.backgroundColor = widgetBackground(selected());
        customSwatch.style.backgroundColor = custom;
        warning.textContent = customChoice.radio.checked && !readableWidgetColor(custom) ? text(context, 'backgroundColorLight') : '';
    };
    let original, originalCustom;
    const open = () => {
        original = previous;
        originalCustom = custom;
        picker.setAttribute('hex', custom);
        picker.setAttribute('open', 'true');
        customChoice.radio.setAttribute('aria-expanded', 'true');
        dialog?.querySelector('[part="hex-input"]')?.focus({ preventScroll: true });
        update();
    };
    const change = event => {
        if (typeof event.detail?.hex !== 'string' || !WIDGET_HEX_COLOR.test(event.detail.hex)) return;
        custom = event.detail.hex.toLowerCase();
        update();
    };
    const cancel = () => {
        custom = originalCustom;
        picker.setAttribute('hex', custom);
        colors.querySelector(`input[value="${WIDGET_HEX_COLOR.test(original) ? 'custom' : original}"]`).checked = true;
        update();
    };
    const close = () => {
        previous = selected();
        picker.removeAttribute('open');
        customChoice.radio.setAttribute('aria-expanded', 'false');
        if (customChoice.radio.isConnected) customChoice.radio.focus({ preventScroll: true });
    };
    const keydown = event => { if (event.key === 'Escape') event.stopPropagation(); };
    const selectionChanged = () => { previous = selected(); update(); };
    const listeners = [[colors, 'change', selectionChanged], [customChoice.radio, 'click', open], [picker, 'update-color', change],
        [dialog, 'cancel', cancel], [dialog?.querySelector('[part="cancel"]'), 'click', cancel], [dialog, 'close', close], [dialog, 'keydown', keydown]];
    listeners.forEach(([target, event, handler]) => target?.addEventListener(event, handler));
    update();
    return {
        read: () => {
            if (customChoice.radio.checked && !readableWidgetColor(custom)) throw new Error(text(context, 'backgroundColorLight'));
            return selected();
        },
        destroy: () => {
            listeners.forEach(([target, event, handler]) => target?.removeEventListener(event, handler));
            if (dialog?.open) dialog.close();
            picker.removeAttribute('open');
        }
    };
}
