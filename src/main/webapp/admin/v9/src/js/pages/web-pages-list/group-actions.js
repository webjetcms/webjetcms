/** Adds and removes a single perex or user group without replacing a page's other groups. */
export class GroupActions {
    constructor(table, groupType) {
        this.table = table;
        this.groupType = groupType;
        this.actionSuffix = groupType === 'user' ? 'UserGroup' : 'PerexGroup';
        this.icon = groupType === 'user' ? 'user' : 'tag';
        this.translationPrefix = 'editor.' + groupType + '_groups.';
    }

    addButtons() {
        for (const remove of [false, true]) {
            const action = (remove ? 'remove' : 'add') + this.actionSuffix;
            this.table.addMoreAction({
                text: `<i class="ti ti-${this.icon}-${remove ? 'minus' : 'plus'}" aria-hidden="true"></i>`,
                className: 'btn-outline-secondary buttons-' + action,
                attr: { title: WJ.translate(this.translationPrefix + action + '.js'), 'data-dtbtn': action },
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
        const fieldId = this.table.DATA.id + '_' + this.groupType + 'GroupAction';
        if (this.loading || document.getElementById(fieldId)) return;
        this.loading = true;

        $.ajax({
            url: WJ.urlAddParam(WJ.urlAddPath(this.table.DATA.url, '/' + this.groupType + '-groups'), 'remove', remove),
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
                WJ.notifyInfo(WJ.translate(this.translationPrefix + 'empty.js'));
                return;
            }
            const content = $('<div>');
            $('<p>').text(WJ.translate(this.translationPrefix + action + '.note.js', [ids.length])).appendTo(content);
            $('<label>', { for: fieldId, class: 'form-label text-body' }).text(WJ.translate(this.translationPrefix + 'group.js')).appendTo(content);
            const select = $('<select>', { id: fieldId, class: 'form-select ' + this.groupType + '-group-picker' }).appendTo(content);
            options.forEach(option => $('<option>', { value: option.value }).text(option.label).appendTo(select));
            WJ.confirm({
                title: WJ.translate(this.translationPrefix + action + '.js'),
                message: content.html(),
                success: () => this.table.executeAction(action, false, null, null, $('#' + fieldId).val(), ids),
                onHidden: () => {
                    // Toastr removes the select's jQuery data before onHidden, so retain the original instance.
                    pickerInstance.destroy();
                    WJ.focusWithoutTooltip(this.table.button('moreActions:name').node()[0]);
                }
            });
            const picker = $('#' + fieldId);
            WJ.initSelectPicker(picker);
            const pickerInstance = picker.data('selectpicker');
        }).always(() => { this.loading = false; });
    }
}
