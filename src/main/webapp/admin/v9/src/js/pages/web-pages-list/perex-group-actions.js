/** Adds and removes a single perex group without replacing a page's other groups. */
export class PerexGroupActions {
    constructor(table) {
        this.table = table;
    }

    addButtons() {
        for (const remove of [false, true]) {
            const action = remove ? 'removePerexGroup' : 'addPerexGroup';
            this.table.addMoreAction({
                text: `<i class="ti ti-tag-${remove ? 'minus' : 'plus'}" aria-hidden="true"></i>`,
                className: 'btn-outline-secondary buttons-' + action,
                attr: { title: WJ.translate('editor.perex_groups.' + action + '.js'), 'data-dtbtn': action },
                init: function(dt) {
                    $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                },
                action: () => this.#open(remove, action)
            });
        }
    }

    /** Loads choices for the selected pages and keeps the same selection until confirmation. */
    #open(remove, action) {
        const ids = this.table.rows({ selected: true }).data().toArray().map(row => row.docId);
        if (ids.length === 0) return;
        const fieldId = this.table.DATA.id + '_perexGroupAction';
        if (this.loading || document.getElementById(fieldId)) return;
        this.loading = true;

        $.ajax({
            url: WJ.urlAddParam(WJ.urlAddPath(this.table.DATA.url, '/perex-groups'), 'remove', remove),
            method: 'POST',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify(ids)
        }).done(options => {
            if (options.error) {
                WJ.notifyError(options.error);
                return;
            }
            if (options.length === 0) {
                WJ.notifyInfo(WJ.translate('editor.perex_groups.empty.js'));
                return;
            }
            const content = $('<div>');
            $('<p>').text(WJ.translate('editor.perex_groups.' + action + '.note.js', [ids.length])).appendTo(content);
            $('<label>', { for: fieldId, class: 'form-label text-body' }).text(WJ.translate('editor.perex_groups.group.js')).appendTo(content);
            const select = $('<select>', { id: fieldId, class: 'form-select perex-group-picker' }).appendTo(content);
            options.forEach(option => $('<option>', { value: option.value }).text(option.label).appendTo(select));
            WJ.confirm({
                title: WJ.translate('editor.perex_groups.' + action + '.js'),
                message: content.html(),
                success: () => this.table.executeAction(action, false, null, null, $('#' + fieldId).val(), ids),
                onHidden: () => {
                    picker.selectpicker('destroy');
                    WJ.focusWithoutTooltip(this.table.button('moreActions:name').node()[0]);
                }
            });
            const picker = $('#' + fieldId);
            WJ.initSelectPicker(picker);
        }).always(() => { this.loading = false; });
    }
}
