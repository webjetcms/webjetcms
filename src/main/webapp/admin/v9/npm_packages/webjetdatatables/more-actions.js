/** A lazily created DataTables collection for less frequently used toolbar actions. */
export class MoreActions {
    constructor(table) {
        this.table = table;
        this.count = 0;
    }

    /**
     * Adds a standard DataTables button, preserving its action and selection handlers.
     * Use text for the icon and attr.title for the visible, escaped action label.
     * @param {object} config DataTables button configuration.
     * @returns {object} DataTables API for the added button.
     */
    addButton(config) {
        if (this.count === 0) {
            const title = WJ.translate('datatables.button.more_actions.js');
            this.table.button().add(null, {
                extend: 'collection',
                name: 'moreActions',
                text: '<i class="ti ti-dots" aria-hidden="true"></i>',
                className: 'btn-outline-secondary buttons-more-actions',
                attr: { title, 'aria-label': title, 'aria-haspopup': 'dialog', 'data-toggle': 'tooltip', 'data-dtbtn': 'moreActions' },
                autoClose: true,
                clickBlurs: false,
                dropIcon: false,
                background: false,
                fade: 0,
                align: 'button-right',
                collectionLayout: 'dt-more-actions-menu',
                action: function(event, dt, node, config) {
                    if (!config._collection.parents('body').length) {
                        // Older WebJET collections override aria-haspopup, so Buttons cannot detect them.
                        // Close them before opening this popup, including their outside-click handlers.
                        const expanded = dt.buttons('.buttons-collection[aria-expanded="true"]').nodes();
                        this.popover(false, config);
                        expanded.attr('aria-expanded', 'false');
                    }
                    $.fn.dataTable.ext.buttons.collection.action.call(this, event, dt, node, config);
                    $(node).tooltip('hide');
                    if (node.attr('aria-expanded') !== 'true') return;
                    const collection = config._collection;
                    collection.attr({ 'aria-label': title, tabindex: -1 });
                    collection.children('ul').attr('role', 'presentation');
                    const enabledButtons = () => collection.find('button:not(:disabled)').filter(':visible');
                    collection.off('.wjMoreActions').on('keydown.wjMoreActions', event => {
                        const buttons = enabledButtons();
                        const index = buttons.index(document.activeElement);
                        if (event.key === 'Escape') {
                            event.preventDefault();
                            event.stopPropagation();
                            this.popover(false, config);
                            WJ.focusWithoutTooltip(node[0]);
                        } else if (event.key === 'Tab') {
                            // This non-modal popup must allow Tab to leave, unlike the vendor focus trap.
                            event.stopPropagation();
                            if (event.shiftKey && index <= 0 || !event.shiftKey && index === buttons.length - 1) {
                                this.popover(false, config);
                                WJ.focusWithoutTooltip(node[0]);
                                if (event.shiftKey) event.preventDefault();
                            }
                        } else if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key) && buttons.length) {
                            event.preventDefault();
                            event.stopPropagation();
                            const next = event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1
                                : (index + (event.key === 'ArrowUp' ? -1 : 1) + buttons.length) % buttons.length;
                            buttons.eq(next).trigger('focus');
                        }
                    }).on('pointermove.wjMoreActions', event => {
                        // Return pointer users to the opener so a previous keyboard focus is not painted beside hover.
                        if (collection[0].contains(document.activeElement) && event.originalEvent?.pointerType !== 'touch') {
                            WJ.focusWithoutTooltip(node[0]);
                        }
                    });
                    node.off('keydown.wjMoreActions').on('keydown.wjMoreActions', event => {
                        if (node.attr('aria-expanded') !== 'true') return;
                        if (event.key === 'Tab') {
                            event.stopPropagation();
                            if (event.shiftKey || enabledButtons().length === 0) this.popover(false, config);
                        } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                            event.preventDefault();
                            const buttons = enabledButtons();
                            (event.key === 'ArrowUp' ? buttons.last() : buttons.first()).trigger('focus');
                        }
                    });
                    if (event.type === 'keypress' || event.originalEvent?.detail === 0) {
                        const first = enabledButtons().first();
                        (first.length ? first : collection).trigger('focus');
                    } else {
                        WJ.focusWithoutTooltip(node[0]);
                    }
                },
                buttons: []
            });
            this.table.on('buttons-action.dt.wjMoreActions', () => {
                const opener = this.table.button('moreActions:name').node();
                if (opener.attr('aria-expanded') !== 'true') {
                    // Buttons uses the incoming collection's fade duration when closing this menu.
                    // Finish it now so outgoing actions cannot intercept clicks on the new menu.
                    opener.siblings('.dt-more-actions-menu').finish();
                }
            });
            this.table.one('destroy.dt.wjMoreActions', () => this.table.off('.wjMoreActions'));
        }

        const attr = { ...config.attr };
        const label = $('<span>').text(attr.title || '').html();
        delete attr['data-toggle'];
        const button = this.table.button().add(this.table.button('moreActions:name').index() + '-' + this.count++, {
            ...config,
            text: function(...args) {
                const text = typeof config.text === 'function' ? config.text.apply(this, args) : config.text;
                return (text || '') + ' ' + label;
            },
            className: (config.className || 'btn-outline-secondary') + ' buttons-more-action',
            attr
        });
        this.updateVisibility();
        return button;
    }

    /** Hides an empty collection, including when its items are detached from the DOM. */
    updateVisibility() {
        if (this.count === 0) return;
        const visible = this.table.buttons('.buttons-more-action').nodes().toArray()
            .some(node => node.style.display !== 'none');
        this.table.button('moreActions:name').node().parent('.btn-group').toggle(visible);
    }
}
