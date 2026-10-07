import { registerWidget } from './registry';
import { node, text, localUrl, shortcutUrl, icon, field, containNativeScroll } from './widget-utils';
import 'color-dialog-box';

/**
 * A same-origin destination from the authorized administration menu.
 * @typedef {Object} MenuEntry
 * @property {string} href - Normalized destination URL.
 * @property {string} title - Menu label.
 * @property {string} [icon] - Own icon or the nearest inherited menu icon.
 * @property {string} path - Parent labels joined as a breadcrumb.
 */

/**
 * Flattens authorized navigation while retaining distinct submenu destinations.
 * Duplicate destinations use the last visited entry; placeholder roots and unsafe URLs are omitted.
 *
 * @param {Object} context - Menu bootstrap data.
 * @param {Object} context.data - Data containing the authorized navigation tree.
 * @param {Object[]} [context.data.dashboardMenu] - Root menu entries, with children in children or childrens.
 * @param {string} [inheritedIcon] - Fallback icon for roots without their own icon.
 * @returns {MenuEntry[]} Unique destinations in traversal insertion order.
 */
export function menuEntries(context, inheritedIcon) {
    const entries = new Map();
    const visit = (items, inheritedIcon, parents = []) => (items || []).forEach(item => {
        const href = localUrl(item.href);
        const itemIcon = item.icon || inheritedIcon;
        if (href && !["/", "/admin/v9/#", "/admin/v9/"].includes(href) && item.text) {
            const path = parents.filter((label, index) => label !== parents[index - 1]);
            if (path.at(-1) === item.text) path.pop();
            entries.set(href, { href, title: item.text, icon: itemIcon, path: path.join(' › ') });
        }
        visit(item.childrens || item.children, itemIcon, [...parents, item.text].filter(Boolean));
    });
    visit(context.data.dashboardMenu, inheritedIcon);
    return [...entries.values()];
}

/**
 * Keeps the authorized menu hierarchy, with selectable URLs only on terminal cards.
 * Empty branches and unsafe destinations are omitted; cards inherit their nearest menu icon.
 * @param {Object} context - Bootstrap data containing the authorized dashboard menu.
 * @returns {Object[]} Main areas containing sections and terminal cards.
 */
function shortcutMenu(context) {
    const visit = (items, parents = [], inheritedIcon) => (items || []).flatMap(item => {
        const itemIcon = item.icon || inheritedIcon;
        const descendants = item.childrens || item.children || [];
        const children = visit(descendants, [...parents, item.text].filter(Boolean), itemIcon);
        if (!item.text) return children;
        const path = parents.filter((label, index) => label !== parents[index - 1]);
        if (path.at(-1) === item.text) path.pop();
        const entry = { title: item.text, icon: itemIcon, path: path.join(' › ') };
        if (children.length) return [{ ...entry, children }];
        const href = localUrl(item.href);
        return !descendants.length && href && !["/", "/admin/v9/#", "/admin/v9/"].includes(href) ? [{ ...entry, href }] : [];
    });
    return visit(context.data.dashboardMenu);
}

const SHORTCUT_COLORS = [
    'default', 'red', 'peach', 'amber', 'mint', 'cyan', 'blue', 'lavender', 'gray'
];
const SHORTCUT_HEX_COLOR = /^#[a-f0-9]{6}(?:[a-f0-9]{2})?$/i;

function shortcutColor(value) {
    return value === 'rose' ? 'red' : typeof value === 'string' && SHORTCUT_HEX_COLOR.test(value) ? value.toLowerCase() : SHORTCUT_COLORS.includes(value) ? value : 'default';
}

function shortcutBackground(value) {
    const color = shortcutColor(value);
    return color.startsWith('#') ? color : `var(--wj-dashboard-shortcut-${color})`;
}

function shortcutForeground(value) {
    const color = shortcutColor(value);
    if (color.startsWith('#')) {
        const alpha = color.length === 9 ? parseInt(color.slice(7), 16) / 255 : 1;
        const channels = color.slice(1, 7).match(/.{2}/g).map(channel => {
            const value = (parseInt(channel, 16) * alpha + 255 * (1 - alpha)) / 255;
            return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
        });
        const luminance = channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
        return luminance > 0.196 ? 'var(--wj-secondary)' : '#fff';
    }
    return ['default', 'amber'].includes(shortcutColor(value)) ? 'var(--wj-secondary)' : '#fff';
}

/**
 * Accepts a Tabler name or a single prefixed class, never arbitrary class lists.
 * @param {string} value - User-entered icon name, trimmed before validation.
 * @returns {string|null} A normalized ti-prefixed class, an empty string for inheritance, or null for invalid input.
 */
function shortcutIcon(value) {
    const name = value.trim();
    if (!name) return '';
    const normalized = name.startsWith('ti-') ? name : `ti-${name}`;
    return normalized.length <= 80 && /^ti-[a-z0-9]+(?:-[a-z0-9]+)*$/.test(normalized) ? normalized : null;
}

export function registerShortcutWidget() {
    let colorPicker;
    registerWidget({
        type: "shortcut", titleKey: "admin.dashboard.shortcut.js", descriptionKey: "admin.dashboard.shortcut.description.js",
        icon: "ti-link", sizes: ["1x1"], multiple: true, defaultOptions: { source: "menu", href: "", title: "" },
        isAvailable: () => true,
        getTitle: (instance, context) => instance.options?.title || menuEntries(context).find(item => item.href === instance.options?.href)?.title || text(context, "shortcut"),
        render({ container, options, context }) {
            const customTarget = options.source === 'url' ? shortcutUrl(options.href) : null;
            const item = options.source === 'url' ? (customTarget && options.title?.trim() ? { href: customTarget, title: options.title, icon: 'ti-link' } : null)
                : menuEntries(context).find(entry => entry.href === options.href);
            const label = options.title || item?.title || text(context, 'shortcut');
            const target = node('a', 'md-dashboard-widget__shortcut');
            const title = node('span', 'md-dashboard-widget__shortcut-label', label);
            target.append(icon(options.icon || item?.icon), title);
            if (item) target.href = item.href;
            else {
                target.classList.add('is-unavailable');
                target.setAttribute('aria-disabled', 'true');
                target.tabIndex = 0;
            }
            if (!item) target.title = text(context, 'shortcutUnavailable');
            target.setAttribute('data-bs-toggle', 'tooltip');
            target.style.setProperty('--wj-dashboard-shortcut-bg', shortcutBackground(options.color));
            target.style.setProperty('--wj-dashboard-shortcut-color', shortcutForeground(options.color));
            container.append(target);
            if (window.jQuery && window.bootstrap?.Tooltip) {
                target.addEventListener('show.bs.tooltip', event => {
                    if (item && title.scrollWidth <= title.clientWidth) event.preventDefault();
                });
                new window.bootstrap.Tooltip(target, { html: false, title: item ? label : target.title, placement: 'top', trigger: 'hover focus', customClass: 'wj-tooltip-hoverable', delay: { show: 300, hide: 150 } });
                window.WJ.initTooltip?.(window.jQuery(target));
            }
            return () => {
                window.bootstrap?.Tooltip?.getInstance(target)?.dispose();
                window.jQuery?.(target).off('.wjTooltipA11y .wjFocusWithoutTooltip');
            };
        },
        /**
         * Builds the shared add/edit form with an accessible destination combobox and live appearance preview.
         * @param {import('./registry').WidgetArguments} args - Dialog content, saved options, authorized menu and lifecycle.
         * @returns {import('./registry').WidgetConfiguration} Validated settings reader and lookup cleanup.
         */
        configure({ container, options, context, signal, adding = false }) {
            const menu = menuEntries(context);
            const tree = shortcutMenu(context);
            const tabs = new Map();
            const collect = items => items.forEach(item => item.children ? collect(item.children) : tabs.set(item.href, item));
            collect(tree);
            let trail = [];
            const pageHref = /^\/admin\/v9\/webpages\/web-pages-list\/\?docid=\d+$/;
            let selected = options.source === 'url'
                ? pageHref.test(options.href) ? { href: options.href, title: options.title, path: '', icon: 'ti-file-text', source: 'url' } : null
                : menu.find(item => item.href === options.href);
            let custom = options.source === 'url' && !selected;
            let active = 0, results = [];
            const search = field(container, text(context, 'shortcutDestination'), [], selected ? [selected.path, selected.title].filter(Boolean).join(' › ') : '', 'text');
            search.name = 'dashboardShortcutSearch';
            search.placeholder = text(context, 'shortcutSearchPlaceholder');
            search.autocomplete = 'off';
            search.maxLength = 255;
            search.setAttribute('role', 'combobox');
            search.setAttribute('aria-autocomplete', 'list');
            search.setAttribute('aria-expanded', 'false');
            const searchGroup = node('div', 'md-dashboard__shortcut-search');
            search.before(searchGroup);
            searchGroup.append(icon('ti-search'), search);
            const suggestions = node('div', 'md-dashboard__shortcut-suggestions');
            suggestions.hidden = true;
            suggestions.id = search.id + '-results';
            suggestions.setAttribute('role', 'listbox');
            suggestions.setAttribute('aria-label', text(context, 'shortcutSuggestions'));
            const matches = node('div', 'md-dashboard__shortcut-matches');
            matches.setAttribute('role', 'presentation');
            search.setAttribute('aria-controls', suggestions.id);
            searchGroup.after(suggestions);
            const lookupStatus = node('div', 'visually-hidden');
            lookupStatus.setAttribute('role', 'status');
            searchGroup.after(lookupStatus);

            const url = field(container, text(context, 'shortcutUrl'), [], custom ? options.href : '', 'text');
            url.name = 'dashboardShortcutUrl';
            url.maxLength = 1024;
            url.placeholder = 'https://…';
            const back = node('button', 'btn btn-link md-dashboard__shortcut-back', text(context, 'shortcutChooseMenu'));
            back.type = 'button';
            url.after(back);
            const title = field(container, text(context, 'shortcutName'), [], options.title || '', 'text');
            title.name = 'dashboardShortcutTitle';
            const optional = node('span', 'md-dashboard__shortcut-optional', text(context, 'shortcutOptional'));
            title.previousElementSibling.append(optional);
            const titleHint = node('small', 'form-text', text(context, 'shortcutNameHint'));
            title.after(titleHint);
            titleHint.id = title.id + '-hint';
            title.setAttribute('aria-describedby', titleHint.id);

            const icons = node('fieldset', 'md-dashboard__shortcut-icons');
            icons.append(node('legend', 'form-label', text(context, 'shortcutIcon')));
            const iconChoices = node('div', 'md-dashboard__shortcut-icon-choices');
            icons.append(iconChoices);
            container.append(icons);
            const iconInput = field(container, text(context, 'shortcutIconName'), [], '', 'text');
            iconInput.name = 'dashboardShortcutIcon';
            iconInput.maxLength = 80;
            iconInput.placeholder = text(context, 'shortcutIconPlaceholder');
            const customIconField = iconInput.parentElement;
            const iconGroup = node('div', 'md-dashboard__shortcut-custom-icon');
            const iconPreview = node('span', 'md-dashboard__shortcut-icon-preview');
            const iconState = node('span', 'md-dashboard__shortcut-icon-state');
            iconInput.before(iconGroup);
            iconGroup.append(iconPreview, iconInput, iconState);
            const iconHint = node('small', 'form-text');
            iconHint.id = iconInput.id + '-hint';
            iconInput.setAttribute('aria-describedby', iconHint.id);
            customIconField.append(iconHint);
            const autoHint = node('small', 'form-text', text(context, 'shortcutIconAutoHint'));
            icons.append(autoHint);
            const autoIcon = () => icon(custom ? 'ti-link' : selected?.icon || 'ti-forms').classList[1];
            const presetNames = ['auto', 'ti-file-text', 'ti-files', 'ti-photo', 'ti-users', 'ti-mail', 'ti-chart-line', 'ti-calendar-event', 'custom'];
            const presetLabels = ['shortcutIconAuto', 'shortcutIcon.file', 'shortcutIcon.files', 'shortcutIcon.photo', 'shortcutIcon.users', 'shortcutIcon.mail', 'shortcutIcon.chart', 'shortcutIcon.calendar', 'shortcutIconCustom'];
            const chooseIcon = name => {
                name = name ? icon(name).classList[1] : '';
                const value = !name || name === autoIcon() ? 'auto' : presetNames.includes(name) ? name : 'custom';
                iconChoices.querySelector('input[value="' + value + '"]').checked = true;
                iconInput.value = (name || autoIcon()).replace(/^ti-/, '');
            };
            presetNames.forEach((value, index) => {
                const label = node('label', 'md-dashboard__shortcut-icon-choice');
                const radio = node('input', 'visually-hidden');
                radio.type = 'radio';
                radio.name = iconInput.id + '-choice';
                radio.value = value;
                radio.setAttribute('aria-label', text(context, presetLabels[index]));
                const face = node('span');
                face.title = text(context, presetLabels[index]);
                face.append(icon(value === 'auto' ? autoIcon() : value === 'custom' ? 'ti-pencil' : value));
                if (value === 'custom') face.append(document.createTextNode(text(context, 'shortcutIconCustom')));
                label.append(radio, face);
                iconChoices.append(label);
            });

            const colors = node('fieldset', 'md-dashboard__shortcut-colors');
            const colorLegend = node('legend', 'form-label', text(context, 'shortcutColor'));
            colorLegend.append(node('span', 'md-dashboard__shortcut-optional', text(context, 'shortcutOptional')));
            colors.append(colorLegend);
            // New shortcuts start with a random palette color; saved colors stay unchanged.
            let previousColor = adding
                ? SHORTCUT_COLORS[1 + Math.floor(Math.random() * (SHORTCUT_COLORS.length - 1))]
                : shortcutColor(options.color);
            let customColor = previousColor.startsWith('#') ? previousColor : '#0063fb';
            for (const value of SHORTCUT_COLORS) {
                const label = node('label', 'md-dashboard__shortcut-swatch');
                const radio = node('input', 'visually-hidden');
                radio.type = 'radio';
                radio.name = iconInput.id + '-color';
                radio.value = value;
                radio.checked = value === previousColor;
                const name = text(context, 'shortcutColor.' + value);
                radio.setAttribute('aria-label', name);
                const swatch = node('span');
                swatch.title = name;
                swatch.style.backgroundColor = shortcutBackground(value);
                label.append(radio, swatch);
                colors.append(label);
            }
            const customColorLabel = node('label', 'md-dashboard__shortcut-icon-choice md-dashboard__shortcut-custom-color');
            const customColorRadio = node('input', 'visually-hidden');
            customColorRadio.type = 'radio';
            customColorRadio.name = iconInput.id + '-color';
            customColorRadio.value = 'custom';
            customColorRadio.checked = previousColor.startsWith('#');
            customColorRadio.setAttribute('aria-label', text(context, 'shortcutIconCustom'));
            customColorRadio.setAttribute('aria-haspopup', 'dialog');
            const customColorFace = node('span');
            const customColorSwatch = node('span', 'md-dashboard__shortcut-custom-color-swatch');
            customColorFace.append(customColorSwatch, document.createTextNode(text(context, 'shortcutIconCustom')));
            customColorLabel.append(customColorRadio, customColorFace);
            colors.append(customColorLabel);
            if (!colorPicker) {
                // Set translated attributes before the custom element is upgraded on insertion.
                const template = node('template');
                template.innerHTML = '<color-picker></color-picker>';
                colorPicker = template.content.firstElementChild;
                for (const [attribute, key] of [['title', 'title'], ['hue', 'hue'], ['saturation', 'saturation'], ['lightness', 'lightness'], ['opacity', 'alpha'], ['ok', 'ok']]) {
                    colorPicker.setAttribute('label-' + attribute, context.translate('datatables.field.color.' + key + '.js'));
                }
            }
            colorPicker.id = iconInput.id + '-color-picker';
            colors.append(colorPicker);
            colors.append(node('small', 'form-text', text(context, 'shortcutColorHint')));
            container.append(colors);
            const colorDialog = colorPicker.shadowRoot?.querySelector('dialog');
            const colorCancel = colorDialog?.querySelector('[part="cancel"]');
            const colorHeading = colorDialog?.querySelector('h3');
            if (colorHeading) {
                colorHeading.id = colorPicker.id + '-title';
                colorDialog.setAttribute('aria-labelledby', colorHeading.id);
            }
            const previewPanel = node('div', 'md-dashboard__shortcut-preview-panel');
            const preview = node('div', 'md-dashboard__shortcut-preview md-dashboard-widget__shortcut');
            previewPanel.append(node('span', 'md-dashboard__shortcut-preview-label', text(context, 'shortcutPreview')), preview);
            container.append(previewPanel);
            const color = () => customColorRadio.checked ? customColor : colors.querySelector('input:checked').value;
            const iconValue = () => {
                const value = iconChoices.querySelector('input:checked').value;
                return value === 'auto' ? autoIcon() : value === 'custom' ? shortcutIcon(iconInput.value) : value;
            };
            const validateIcon = () => {
                const value = iconValue();
                iconPreview.replaceChildren(icon(value || 'ti-search'));
                const content = value && window.getComputedStyle(iconPreview.firstElementChild, '::before').content;
                const valid = !!value && !!content && !['none', 'normal', '""', "''"].includes(content);
                const isCustom = iconChoices.querySelector('input:checked').value === 'custom';
                const invalid = isCustom && !valid;
                iconInput.setAttribute('aria-invalid', String(invalid));
                iconGroup.classList.toggle('is-invalid', invalid && !!iconInput.value.trim());
                iconState.replaceChildren(icon(valid ? 'ti-check' : 'ti-alert-circle'));
                iconState.hidden = !iconInput.value.trim();
                iconState.classList.toggle('text-danger', invalid);
                iconHint.replaceChildren();
                if (invalid && iconInput.value.trim()) iconHint.textContent = text(context, 'shortcutIconUnknown', iconInput.value.trim());
                else {
                    iconHint.append(document.createTextNode(text(context, 'shortcutIconFind') + ' '));
                    const library = node('a', 'md-dashboard__shortcut-icon-library', 'tabler.io/icons');
                    library.href = 'https://tabler.io/icons';
                    library.target = '_blank';
                    library.rel = 'noopener noreferrer';
                    library.append(icon('ti-external-link'));
                    iconHint.append(library);
                    if (valid) iconHint.append(document.createTextNode(' · ' + text(context, 'shortcutIconLoaded')));
                }
                iconHint.classList.toggle('text-danger', invalid);
                return !isCustom || valid;
            };
            const updatePreview = () => {
                const name = iconValue();
                const isCustom = iconChoices.querySelector('input:checked').value === 'custom';
                customIconField.hidden = !isCustom;
                autoHint.hidden = isCustom;
                validateIcon();
                customColorSwatch.style.backgroundColor = customColor;
                preview.replaceChildren(icon(name || 'ti-link'), node('span', 'md-dashboard-widget__shortcut-label', title.value.trim() || (!custom && selected?.title) || text(context, 'shortcut')));
                preview.style.setProperty('--wj-dashboard-shortcut-bg', shortcutBackground(color()));
                preview.style.setProperty('--wj-dashboard-shortcut-color', shortcutForeground(color()));
            };
            let originalColor, originalCustomColor;
            const openColorPicker = () => {
                originalColor = previousColor;
                originalCustomColor = customColor;
                colorPicker.setAttribute('hex', customColor);
                colorPicker.setAttribute('open', 'true');
                colorDialog?.querySelector('[part="hex-input"]')?.focus({ preventScroll: true });
                updatePreview();
            };
            const updateColor = event => {
                if (typeof event.detail?.hex !== 'string' || !SHORTCUT_HEX_COLOR.test(event.detail.hex)) return;
                customColor = shortcutColor(event.detail.hex);
                updatePreview();
            };
            const closeColorPicker = () => {
                previousColor = color();
                colorPicker.removeAttribute('open');
                if (customColorRadio.isConnected) customColorRadio.focus({ preventScroll: true });
            };
            const cancelColorPicker = () => {
                customColor = originalCustomColor;
                colorPicker.setAttribute('hex', customColor);
                colors.querySelector('input[value="' + (originalColor.startsWith('#') ? 'custom' : originalColor) + '"]').checked = true;
                updatePreview();
            };
            const colorKeydown = event => { if (event.key === 'Escape') event.stopPropagation(); };
            customColorRadio.addEventListener('click', openColorPicker);
            colorPicker.addEventListener('update-color', updateColor);
            colorDialog?.addEventListener('close', closeColorPicker);
            colorDialog?.addEventListener('cancel', cancelColorPicker);
            colorCancel?.addEventListener('click', cancelColorPicker);
            colorDialog?.addEventListener('keydown', colorKeydown);
            const updateSource = () => {
                search.closest('.md-dashboard__field').hidden = custom;
                url.parentElement.hidden = !custom;
                url.disabled = !custom;
                title.required = custom;
                optional.hidden = custom;
                titleHint.hidden = custom;
                title.placeholder = custom ? text(context, 'customTitle') : selected?.title || text(context, 'shortcutNamePlaceholder');
                iconChoices.querySelector('input[value="auto"] + span').replaceChildren(icon(autoIcon()));
                iconChoices.querySelectorAll('label').forEach(label => { label.hidden = label.querySelector('input').value === autoIcon(); });
                updatePreview();
            };
            const close = () => {
                suggestions.hidden = true;
                search.setAttribute('aria-expanded', 'false');
                search.removeAttribute('aria-activedescendant');
            };
            const highlight = index => {
                active = Math.max(0, Math.min(results.length - 1, index));
                [...suggestions.querySelectorAll('[role="option"]')].forEach((row, index) => row.setAttribute('aria-selected', String(index === active)));
                const row = suggestions.querySelectorAll('[role="option"]')[active];
                if (row) {
                    search.setAttribute('aria-activedescendant', row.id);
                    row.scrollIntoView?.({ block: 'nearest' });
                }
            };
            const select = item => {
                if (item.children || item.back) {
                    if (item.back) trail.pop();
                    else trail.push(item);
                    selected = null;
                    search.value = '';
                    updateSource();
                    search.focus();
                    browse();
                    return;
                }
                close();
                if (item.custom) {
                    custom = true;
                    updateSource();
                    url.focus();
                } else {
                    custom = false;
                    selected = item;
                    search.value = [item.path, item.title].filter(Boolean).join(' › ');
                    chooseIcon(item.icon);
                    updateSource();
                    search.focus();
                }
            };
            const renderResults = (entries, heading, parent) => {
                const previous = results[active];
                matches.replaceChildren();
                suggestions.replaceChildren(matches);
                const goBack = parent ? { back: true, title: context.translate('button.back'), path: trail.map(item => item.title).join(' › '), icon: 'ti-arrow-left' } : null;
                results = [...(goBack ? [goBack] : []), ...entries, { custom: true }];
                let index = 0;
                const group = node('div');
                group.setAttribute('role', 'group');
                group.setAttribute('aria-label', heading);
                const caption = node('div', 'md-dashboard__shortcut-result-heading', heading);
                caption.setAttribute('role', 'presentation');
                group.append(caption);
                for (const item of results.slice(0, -1)) {
                    const row = node('div', 'md-dashboard__shortcut-result');
                    if (item.back) row.classList.add('md-dashboard__shortcut-result-back');
                    row.id = suggestions.id + '-' + index++;
                    row.setAttribute('role', 'option');
                    const copy = node('span', 'md-dashboard__shortcut-result-text');
                    copy.append(node('span', '', item.title));
                    if (item.path) copy.append(node('small', '', item.path));
                    row.append(icon(item.icon), copy, node('small', 'md-dashboard__shortcut-enter', 'Enter'));
                    row.addEventListener('click', () => select(item));
                    group.append(row);
                }
                matches.append(group);
                if (!entries.length) matches.append(node('p', 'md-dashboard__shortcut-result-message', text(context, 'shortcutNoResults')));
                const ownUrl = node('div', 'md-dashboard__shortcut-result md-dashboard__shortcut-result-url');
                ownUrl.id = suggestions.id + '-' + index;
                ownUrl.setAttribute('role', 'option');
                ownUrl.append(icon('ti-link'), document.createTextNode(text(context, 'shortcutUseUrl')));
                ownUrl.addEventListener('click', () => select({ custom: true }));
                suggestions.append(ownUrl);
                suggestions.hidden = false;
                search.setAttribute('aria-expanded', 'true');
                highlight(results.includes(previous) ? results.indexOf(previous) : goBack && entries.length ? 1 : 0);
                lookupStatus.textContent = heading + '. ' + text(context, 'shortcutResultCount', entries.length);
            };
            const browse = () => {
                const parent = trail.at(-1);
                const heading = text(context, !parent ? 'shortcutGroup' : trail.length === 1 ? 'shortcutSection' : 'shortcutChooseTab');
                renderResults(parent ? parent.children : tree, heading, parent);
            };
            const searchTargets = () => {
                const term = search.value.trim();
                if (selected || !term) { browse(); return; }
                const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase();
                const words = normalize(term).split(/\s+/);
                const titleMatches = item => words.every(word => normalize(item.title).includes(word));
                const entries = [...tabs.values()].filter(item => words.every(word => normalize(item.title + ' ' + item.path).includes(word)))
                    .sort((a, b) => Number(titleMatches(b)) - Number(titleMatches(a))).slice(0, 8);
                renderResults(entries, text(context, 'shortcutChooseTab'));
            };
            search.addEventListener('input', () => { selected = null; trail = []; searchTargets(); updateSource(); });
            search.addEventListener('focus', () => { if (!search.value.trim() && !custom) searchTargets(); });
            search.addEventListener('click', () => { search.select(); searchTargets(); });
            search.addEventListener('keydown', event => {
                if (event.key === 'Escape' && !suggestions.hidden) { event.preventDefault(); event.stopPropagation(); close(); return; }
                if (event.key === 'Tab') { close(); return; }
                if (!['ArrowDown', 'ArrowUp', 'Enter'].includes(event.key)) return;
                if (event.key === 'Enter' && suggestions.hidden) return;
                event.preventDefault();
                if (suggestions.hidden) { searchTargets(); return; }
                if (event.key === 'Enter') select(results[active]);
                else highlight(active + (event.key === 'ArrowDown' ? 1 : -1));
            });
            suggestions.addEventListener('mousedown', event => event.preventDefault());
            const outside = event => { if (!event.composedPath().includes(search.closest('.md-dashboard__field'))) close(); };
            container.addEventListener('focusin', outside);
            container.addEventListener('click', outside);
            back.addEventListener('click', () => { custom = false; updateSource(); search.focus(); search.select(); searchTargets(); });
            iconChoices.addEventListener('change', () => { updatePreview(); if (!customIconField.hidden) iconInput.focus(); });
            iconInput.addEventListener('input', updatePreview);
            title.addEventListener('input', updatePreview);
            colors.addEventListener('change', () => { previousColor = color(); updatePreview(); });
            chooseIcon(options.icon);
            updateSource();
            let disposed = false;
            const dispose = () => {
                if (disposed) return;
                disposed = true;
                close();
                colorDialog?.removeEventListener('close', closeColorPicker);
                colorDialog?.removeEventListener('cancel', cancelColorPicker);
                colorCancel?.removeEventListener('click', cancelColorPicker);
                colorDialog?.removeEventListener('keydown', colorKeydown);
                colorPicker.removeEventListener('update-color', updateColor);
                if (colorDialog?.open) colorDialog.close();
                colorPicker.removeAttribute('open');
                colorPicker.remove();
            };
            signal?.addEventListener('abort', dispose, { once: true });
            if (signal) containNativeScroll(suggestions, signal);
            const modal = container.closest('.modal');
            modal?.addEventListener('shown.bs.modal', () => {
                if (document.activeElement === modal) (custom ? url : search).focus();
            }, { once: true });
            return {
                dispose,
                read: () => {
                    const href = custom ? shortcutUrl(url.value) : selected?.href;
                    if (!href) { (custom ? url : search).focus(); throw new Error(text(context, custom ? 'shortcutUrlInvalid' : 'shortcutChooseTarget')); }
                    if (custom && !title.value.trim()) { title.focus(); throw new Error(text(context, 'shortcutTitleRequired')); }
                    if (!validateIcon()) { iconInput.focus(); throw new Error(text(context, 'shortcutIconInvalid')); }
                    return { options: { source: custom ? 'url' : selected.source || 'menu', href, title: title.value.trim() || (selected?.source === 'url' ? selected.title : ''), icon: iconValue(), color: color() } };
                }
            };
        }
    });
}
