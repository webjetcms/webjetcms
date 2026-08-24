import WJ from '../../src/js/webjet';

function setExpanded(conf, expanded) {
    const value = expanded ? 'true' : 'false';
    conf._input.attr('aria-expanded', value);
    conf._preview.attr('aria-expanded', value);
}

function focusInitialControl(dialog) {
    const initialControl = dialog?.querySelector('[part="hex-input"]')
        || dialog?.querySelector('button:not([disabled]), input:not([disabled]), [tabindex="0"]');
    if (dialog?.open && initialControl) {
        initialControl.focus({preventScroll: true});
    }
}

export function initColorPickerAccessibility(conf) {
    if (!conf._picker || conf._picker.wjAccessibilityInitialized === true) return;
    conf._picker.wjAccessibilityInitialized = true;

    const openLabel = WJ.translate('datatables.field.color.title.js');
    const resetLabel = WJ.translate('button.reset');
    const dialog = conf._picker.shadowRoot?.querySelector('dialog');

    conf._preview.attr({
        'aria-expanded': 'false',
        'aria-haspopup': 'dialog',
        'aria-label': openLabel,
        'title': openLabel
    });
    conf._input.attr({
        'aria-expanded': 'false',
        'aria-haspopup': 'dialog'
    });
    conf._clear.attr({
        'aria-label': resetLabel,
        'title': resetLabel
    });
    conf._clear.find('i').attr('aria-hidden', 'true');

    if (dialog) {
        const heading = dialog.querySelector('h3');
        if (heading) {
            heading.id = `${conf._picker.id}-title`;
            dialog.setAttribute('aria-labelledby', heading.id);
        }

        dialog.addEventListener('close', function() {
            const opener = conf._colorPickerOpener;
            conf._colorPickerOpener = null;
            conf._picker.removeAttribute('open');
            setExpanded(conf, false);

            window.requestAnimationFrame(function() {
                if (opener?.isConnected && opener.disabled !== true) {
                    opener.focus({preventScroll: true});
                }
            });
        });
    }

    const openPicker = function(opener) {
        conf._colorPickerOpener = opener;
        conf._picker.setAttribute('hex', conf._input.val());
        conf._picker.setAttribute('open', 'true');
        setExpanded(conf, true);

        window.requestAnimationFrame(function() {
            focusInitialControl(dialog);
        });
    };

    conf._input.on('click', function() {
        openPicker(this);
    });
    conf._preview.on('click', function() {
        openPicker(this);
    });
}
