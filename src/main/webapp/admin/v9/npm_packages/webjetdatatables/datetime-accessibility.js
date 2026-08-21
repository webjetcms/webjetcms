import WJ from '../../src/js/webjet';
import $ from 'jquery';

const FALLBACK_TRANSLATIONS = {
    cs: {
        calendar: 'Kalendář',
        dialog: 'Výběr data a času',
        instructions: 'Formát data a času: {1}. Šipkou dolů přejdete do kalendáře, mezi dny se pohybujete šipkami, výběr potvrdíte klávesou Enter nebo mezerníkem a kalendář zavřete klávesou Escape.',
        month: 'Měsíc',
        year: 'Rok'
    },
    en: {
        calendar: 'Calendar',
        dialog: 'Date and time picker',
        instructions: 'Date and time format: {1}. Press Arrow Down to enter the calendar, use arrow keys to move between days, confirm with Enter or Space, and close the calendar with Escape.',
        month: 'Month',
        year: 'Year'
    },
    sk: {
        calendar: 'Kalendár',
        dialog: 'Výber dátumu a času',
        instructions: 'Formát dátumu a času: {1}. Šípkou nadol prejdete do kalendára, medzi dňami sa pohybujete šípkami, výber potvrdíte klávesom Enter alebo medzerníkom a kalendár zatvoríte klávesom Escape.',
        month: 'Mesiac',
        year: 'Rok'
    }
};

function translate(key, fallbackKey, ...params) {
    const translated = WJ.translate(key, params);
    if (typeof translated === 'string' && translated.trim().length > 0 && translated !== key) return translated;

    const language = (window.userLng || document.documentElement.lang || 'sk').substring(0, 2);
    let fallback = (FALLBACK_TRANSLATIONS[language] || FALLBACK_TRANSLATIONS.sk)[fallbackKey];
    params.forEach((parameter, index) => {
        fallback = fallback.replaceAll(`{${index + 1}}`, parameter);
    });
    return fallback;
}

function appendAttributeId($element, attribute, id) {
    const ids = ($element.attr(attribute) || '').split(/\s+/).filter(Boolean);
    if (ids.includes(id) === false) ids.push(id);
    $element.attr(attribute, ids.join(' '));
}

function getDateLabel(instance, button) {
    const date = new Date(Date.UTC(
        Number(button.dataset.year),
        Number(button.dataset.month),
        Number(button.dataset.day)
    ));
    const locale = instance.c.locale || window.userLng || document.documentElement.lang || 'sk';

    return new Intl.DateTimeFormat(locale, {
        day: 'numeric',
        month: 'long',
        timeZone: 'UTC',
        weekday: 'long',
        year: 'numeric'
    }).format(date);
}

function getInputFormat(instance) {
    let format = instance.c.format;
    const language = (instance.c.locale || window.userLng || document.documentElement.lang || 'sk').substring(0, 2);
    const localeData = window.moment?.localeData(language);
    const localizedDateFormat = localeData?.longDateFormat?.('L') || (language === 'en' ? 'MM/DD/YYYY' : 'DD.MM.YYYY');
    const localizedTimeFormat = localeData?.longDateFormat?.('LT') || 'HH:mm';
    const localizedTimeWithSecondsFormat = localeData?.longDateFormat?.('LTS') || 'HH:mm:ss';

    return format
        .replace(/LTS/g, localizedTimeWithSecondsFormat)
        .replace(/LT/g, localizedTimeFormat)
        .replace(/L/g, localizedDateFormat);
}

function getFocusableElements(container) {
    return [...container.querySelectorAll('button:not([disabled]), select:not([disabled]), a[role="button"][tabindex="0"]')]
        .filter(element => element.getClientRects().length > 0 && getComputedStyle(element).visibility !== 'hidden');
}

function updateDateTimeContent(instance) {
    const container = instance.dom.container[0];
    const input = instance.dom.input[0];
    if (container == null || input == null) return;

    const namespace = instance.s.namespace;
    const containerId = namespace + '-dialog';
    const instructionsId = namespace + '-instructions';
    const liveId = namespace + '-live';
    let instructions = document.getElementById(instructionsId);
    let liveRegion = container.querySelector('#' + liveId);

    if (instructions == null) {
        instructions = document.createElement('p');
        instructions.id = instructionsId;
        instructions.className = 'visually-hidden wj-datetime-instructions';
        input.insertAdjacentElement('afterend', instructions);
    }
    instructions.textContent = translate('datatables.datetime.instructions.js', 'instructions', getInputFormat(instance));

    if (liveRegion == null) {
        liveRegion = document.createElement('p');
        liveRegion.id = liveId;
        liveRegion.className = 'visually-hidden wj-datetime-live';
        liveRegion.setAttribute('aria-live', 'polite');
        liveRegion.setAttribute('aria-atomic', 'true');
        container.append(liveRegion);
    }

    container.id = containerId;
    container.setAttribute('role', 'dialog');
    container.setAttribute('aria-label', translate('datatables.datetime.dialog.js', 'dialog'));
    container.setAttribute('aria-describedby', instructionsId);

    const isVisible = instance.dom.container.is(':visible');
    instance.dom.input.attr({
        'aria-expanded': isVisible ? 'true' : 'false',
        'aria-haspopup': 'dialog',
        'role': 'combobox'
    });
    if (isVisible) instance.dom.input.attr('aria-controls', containerId);
    else instance.dom.input.removeAttr('aria-controls');
    appendAttributeId(instance.dom.input, 'aria-describedby', instructionsId);

    instance.dom.previous.children('button').attr('aria-label', instance.c.i18n.previous);
    instance.dom.next.children('button').attr('aria-label', instance.c.i18n.next);
    container.querySelector('.dt-datetime-month')?.setAttribute('aria-label', translate('datatables.datetime.month.js', 'month'));
    container.querySelector('.dt-datetime-year')?.setAttribute('aria-label', translate('datatables.datetime.year.js', 'year'));

    const calendar = container.querySelector('.dt-datetime-calendar table');
    if (calendar != null) {
        calendar.setAttribute('role', 'grid');
        calendar.setAttribute('aria-label', translate('datatables.datetime.calendar.js', 'calendar'));
    }

    const dayButtons = [...container.querySelectorAll('.dt-datetime-calendar button[data-year]')];
    dayButtons.forEach(button => {
        const cell = button.closest('td');
        const isSelected = cell.classList.contains('selected');
        const isDisabled = cell.classList.contains('disabled');

        cell.setAttribute('role', 'gridcell');
        cell.setAttribute('aria-selected', isSelected ? 'true' : 'false');
        button.setAttribute('aria-label', getDateLabel(instance, button));
        button.tabIndex = -1;
        button.disabled = isDisabled;
        if (cell.classList.contains('now')) button.setAttribute('aria-current', 'date');
        else button.removeAttribute('aria-current');
    });

    const activeDay = dayButtons.find(button => button.closest('td').classList.contains('selected')) ||
        dayButtons.find(button => button.closest('td').classList.contains('now')) ||
        dayButtons.find(button => button.disabled === false);
    if (activeDay != null) activeDay.tabIndex = 0;

    [...container.querySelectorAll('.dt-datetime-time button[data-unit]')].forEach(button => {
        const heading = button.closest('table')?.querySelector('th')?.textContent.trim() || '';
        button.setAttribute('aria-label', `${heading} ${button.textContent.trim()}`.trim());
        button.setAttribute('aria-pressed', button.closest('td').classList.contains('selected') ? 'true' : 'false');
        if (button.closest('td').classList.contains('disabled') && button.closest('td').classList.contains('range') === false) {
            button.disabled = true;
        }
    });

    [...container.querySelectorAll('.dt-datetime-buttons a')].forEach(link => {
        link.setAttribute('role', 'button');
        link.tabIndex = 0;
    });
}

function focusDay(instance, date) {
    if (instance.s.display.getUTCFullYear() !== date.getUTCFullYear() || instance.s.display.getUTCMonth() !== date.getUTCMonth()) {
        instance.display(date.getUTCFullYear(), date.getUTCMonth() + 1);
    }

    updateDateTimeContent(instance);
    const selector = `.dt-datetime-calendar button[data-year="${date.getUTCFullYear()}"][data-month="${date.getUTCMonth()}"][data-day="${date.getUTCDate()}"]`;
    const target = instance.dom.container[0].querySelector(selector);
    if (target == null || target.disabled) return;

    instance.dom.container.find('.dt-datetime-calendar button').attr('tabindex', '-1');
    target.tabIndex = 0;
    target.focus();
}

function focusActiveDay(instance) {
    const container = instance.dom.container[0];
    const input = instance.dom.input[0];
    if (instance.c.alwaysVisible || input.type === 'hidden') return;

    setTimeout(() => {
        if (instance.dom.container.is(':visible') === false) return;
        if (document.activeElement !== input && container.contains(document.activeElement) === false) return;

        const target = container.querySelector('.dt-datetime-calendar button[tabindex="0"]') || getFocusableElements(container)[0];
        target?.focus({preventScroll: true});
    }, 0);
}

function closeAndRestoreFocus(instance) {
    const input = instance.dom.input[0];
    if (input.isConnected && input.disabled === false) input.focus({preventScroll: true});
    instance.hide();
}

function initializeDateTimeInstance(instance) {
    if (instance._wjAccessibilityInitialized === true) return;
    instance._wjAccessibilityInitialized = true;

    const container = instance.dom.container[0];
    const input = instance.dom.input[0];

    input.addEventListener('keydown', event => {
        if (event.key === 'ArrowDown' && instance.dom.container.is(':visible')) {
            event.preventDefault();
            const target = container.querySelector('.dt-datetime-calendar button[tabindex="0"]') || getFocusableElements(container)[0];
            target?.focus();
        }
        else if (event.key === 'Escape' && instance.dom.container.is(':visible')) {
            event.preventDefault();
            closeAndRestoreFocus(instance);
        }
    });

    container.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            event.preventDefault();
            event.stopPropagation();
            closeAndRestoreFocus(instance);
            return;
        }

        const dayButton = event.target.closest('.dt-datetime-calendar button[data-year]');
        const dayOffset = {
            ArrowDown: 7,
            ArrowLeft: -1,
            ArrowRight: 1,
            ArrowUp: -7
        }[event.key];
        if (dayButton != null && dayOffset != null) {
            event.preventDefault();
            event.stopPropagation();
            const date = new Date(Date.UTC(
                Number(dayButton.dataset.year),
                Number(dayButton.dataset.month),
                Number(dayButton.dataset.day) + dayOffset
            ));
            focusDay(instance, date);
            return;
        }

        if (event.target.matches('a[role="button"]') && (event.key === 'Enter' || event.key === ' ')) {
            event.preventDefault();
            event.stopPropagation();
            event.target.click();
            return;
        }

        if (event.target.matches('button') && (event.key === 'Enter' || event.key === ' ')) {
            event.stopPropagation();
            return;
        }

        if (event.key === 'Tab') {
            event.stopPropagation();
            const focusableElements = getFocusableElements(container);
            const firstElement = focusableElements[0];
            const lastElement = focusableElements[focusableElements.length - 1];

            if (event.shiftKey && document.activeElement === firstElement) {
                event.preventDefault();
                lastElement.focus();
            }
            else if (event.shiftKey === false && document.activeElement === lastElement) {
                event.preventDefault();
                firstElement.focus();
            }
        }
    });

    container.addEventListener('focusin', event => {
        const dayButton = event.target.closest('.dt-datetime-calendar button[data-year]');
        if (dayButton == null) return;

        instance.dom.container.find('.dt-datetime-calendar button').attr('tabindex', '-1');
        dayButton.tabIndex = 0;
        const liveRegion = container.querySelector('.wj-datetime-live');
        if (liveRegion != null) liveRegion.textContent = dayButton.getAttribute('aria-label');
    });

    container.addEventListener('click', event => {
        if (event.detail !== 0) return;
        const button = event.target.closest('button');
        if (button == null) return;

        const data = {...button.dataset};
        const isNavigation = button.parentElement.classList.contains('dt-datetime-iconLeft') ||
            button.parentElement.classList.contains('dt-datetime-iconRight');
        if (isNavigation === false && data.unit == null && data.year == null) return;

        setTimeout(() => {
            if (instance.dom.container.is(':visible') === false) return;

            let target = button;
            if (data.unit != null) {
                target = container.querySelector(`button[data-unit="${data.unit}"][data-value="${data.value}"]`);
            }
            else if (data.year != null) {
                target = container.querySelector(`button[data-year="${data.year}"][data-month="${data.month}"][data-day="${data.day}"]`);
            }
            target?.focus();
        }, 0);
    });

    updateDateTimeContent(instance);
}

function wrapPrototypeMethod(prototype, methodName, callback) {
    const originalMethod = prototype[methodName];
    prototype[methodName] = function(...args) {
        const result = originalMethod.apply(this, args);
        callback(this);
        return result;
    };
}

export function initDateTimeAccessibility() {
    const DateTime = $.fn.dataTable.DateTime || $.fn.dataTable.Editor.DateTime;
    if (DateTime == null || DateTime.prototype._wjAccessibilityPatched === true) return;

    const prototype = DateTime.prototype;
    prototype._wjAccessibilityPatched = true;

    wrapPrototypeMethod(prototype, '_constructor', initializeDateTimeInstance);
    wrapPrototypeMethod(prototype, '_setCalander', updateDateTimeContent);
    wrapPrototypeMethod(prototype, '_setTime', updateDateTimeContent);
    wrapPrototypeMethod(prototype, '_show', instance => {
        updateDateTimeContent(instance);
        instance.dom.input.attr('aria-expanded', 'true');
        focusActiveDay(instance);
    });
    wrapPrototypeMethod(prototype, '_hide', instance => {
        instance.dom.input.attr('aria-expanded', 'false');
    });
}
