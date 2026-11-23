/**
 * Initializes standard administration selects, including dynamically inserted fields.
 * Existing pickers are refreshed after option or disabled-state changes.
 * @param {Element|Document|jQuery|string} root Select element or container to initialize.
 * @param {Object} overrides Optional bootstrap-select settings for new pickers.
 */
export function initSelectPicker(root = document, overrides = {}) {
    const $ = window.jQuery;
    if (!$?.fn.selectpicker) return;
    $.fn.selectpicker.Constructor.BootstrapVersion = '5';
    $(root).find('select').addBack('select').not('.no-picker').each(function () {
        const $select = $(this);
        if ($select.hasClass('form-select')) $select.addClass('form-control');
        const syncAria = () => {
            const picker = $select.data('selectpicker');
            if (!picker) return;
            picker.$button.attr('aria-controls', picker.$menuInner.attr('id'));
            picker.$searchbox.attr('aria-expanded', picker.$button.attr('aria-expanded') || 'false');
        };
        $select.off('.wjSelectPickerA11y').on('shown.bs.select.wjSelectPickerA11y hidden.bs.select.wjSelectPickerA11y refreshed.bs.select.wjSelectPickerA11y', syncAria);
        if ($select.data('selectpicker')) {
            $select.selectpicker('refresh');
            syncAria();
            return;
        }
        const options = {
            container: $select.closest('.modal').get(0) || 'body',
            style: 'dropdown bootstrap-select btn-outline-secondary',
            liveSearch: true,
            showSubtext: true,
            noneSelectedText: '\u00a0',
            iconBase: 'ti',
            ...overrides
        };
        const liveSearch = $select.data('live-search');
        if (liveSearch !== undefined) options.liveSearch = liveSearch;
        $select.selectpicker(options);
        syncAria();
    });
}
