import {openBranchMerge} from './branch-merge';
import {icon, node} from './dashboard/widget-utils';
import {SHORTCUT_COLORS} from './shortcut-colors';

const tr = key => WJ.translate(`branches.${key}.js`);

/** Uses the shared DataTable editor for metadata and the existing publishing comparison for content. */
export function initBranchTable(columns, screen, api) {
    const stateColumn = columns.find(column => column.data === 'state');
    stateColumn.render = (value, type) => {
        const label = tr(`merge.state.${value}`);
        if (type !== 'display') return label;
        const badge = node('span', 'md-branch-state', label);
        badge.dataset.state = value;
        return badge.outerHTML;
    };
    const colorColumn = columns.find(column => column.data === 'color');
    colorColumn.render = (value, type) => {
        const color = SHORTCUT_COLORS.includes(value) ? value : 'cyan';
        const label = WJ.translate(`admin.dashboard.shortcutColor.${color}.js`);
        if (type !== 'display') return label;
        const caption = node('span', 'md-branch-table-color', label);
        const dot = node('span', 'md-branch-picker__dot');
        dot.dataset.branchColor = color;
        dot.setAttribute('aria-hidden', 'true');
        caption.prepend(dot);
        return caption.outerHTML;
    };
    const table = WJ.DataTable({
        id: 'branchesDataTable',
        url: '/admin/rest/branches/datatable',
        columns,
        serverSide: true,
        fetchOnCreate: true,
        fetchOnEdit: true,
        hideButtons: 'duplicate,remove,import,celledit',
        order: [[columns.findIndex(column => column.data === 'id'), 'desc']]
    });
    function selected() {
        const rows = table.rows({selected: true}).data().toArray();
        return rows.length === 1 ? rows[0] : null;
    }
    table.button().add(2, {
        extend: 'selectedSingle',
        text: `<i class="ti ti-list-check" aria-hidden="true"></i> ${WJ.escapeHtml(tr('merge.compare'))}`,
        className: 'btn btn-outline-secondary buttons-branch-compare',
        action: (event, dt, trigger) => {
            const branch = selected();
            if (branch) openBranchMerge(branch.id, api, screen, trigger[0]);
        }
    });
    table.button().add(3, {
        text: `<i class="ti ti-edit" aria-hidden="true"></i> ${WJ.escapeHtml(tr('merge.editWeb'))}`,
        className: 'btn btn-outline-secondary buttons-branch-open buttons-divider',
        enabled: false,
        action: async () => {
            const branch = selected();
            if (!branch || branch.state !== 'DRAFT') return;
            table.button('.buttons-branch-open').enable(false);
            try {
                await api('/context', {branchId: branch.id, expectedDomainId: screen.domainId});
                location.assign('/admin/v9/webpages/web-pages-list/');
            } catch (error) {
                WJ.notifyError(WJ.escapeHtml(error.message));
                updateActions();
            }
        }
    });
    function updateActions() {
        table.button('.buttons-branch-open').enable(selected()?.state === 'DRAFT');
    }
    table.on('select deselect draw', updateActions);
    window.addEventListener('WJ.Branch.updated', () => table.ajax.reload(null, false));

    table.EDITOR.on('open', () => {
        const editor = table.EDITOR;
        const field = editor.field('color').node();
        const body = editor.field('name').node().parentElement;
        let sample = body.querySelector('.md-branch-sample');
        if (!sample) {
            sample = node('div', 'md-branch-sample m-3');
            sample.setAttribute('aria-hidden', 'true');
            const caption = node('span');
            caption.append(node('small', '', tr('sample')), node('strong'));
            sample.append(icon('ti-git-branch'), caption);
            body.append(sample);
        }
        function preview() {
            const color = editor.field('color').val();
            sample.dataset.branchColor = SHORTCUT_COLORS.includes(color) ? color : 'cyan';
            sample.querySelector('strong').textContent = editor.field('name').val() || tr('new');
        }
        $(editor.field('name').input()).off('input.branchPreview').on('input.branchPreview', preview);
        $(field).off('change.branchPreview').on('change.branchPreview', preview);
        preview();
    });
    table.EDITOR.on('submitSuccess', (event, json) => {
        for (const branch of json.data || []) {
            if (Number(branch.id) === Number(screen.branchId)) {
                document.querySelector('.md-branch-bar')?.setAttribute('data-branch-color', branch.color);
                window.dispatchEvent(new CustomEvent('WJ.Branch.updated', {detail: branch}));
            }
        }
    });
    return table;
}
