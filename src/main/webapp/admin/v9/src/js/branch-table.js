import {customizeBranchChangeColumns, mountBranchChanges, openBranchPreview, previewStates, publishStates} from './branch-merge';
import {icon, node} from './dashboard/widget-utils';
import {SHORTCUT_COLORS} from './shortcut-colors';

const tr = key => WJ.translate(`branches.${key}.js`);

/** Uses the shared DataTable editor for metadata and the existing publishing comparison for content. */
export function initBranchTable(columns, screen, api) {
    window.customizeBranchChangeColumns = customizeBranchChangeColumns;
    const query = new URLSearchParams(location.search);
    const deepLink = query.get('tab') === 'changes' ? query.get('id') : null;
    let opening = deepLink ? {id: deepLink} : null;
    let confirmOnOpen = false;
    let closeChanges;
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
        url: '/admin/rest/branches',
        columns,
        tabs: [
            {id: 'basic', title: WJ.translate('datatable.tab.basic'), selected: true},
            {id: 'changes', title: tr('merge.changesTab'), hideOnCreate: true},
            {id: 'publishing', title: tr('merge.publishingTab'), hideOnCreate: true, content: '<div class="pg-branch-panel" id="branch-publishing-panel"></div>'}
        ],
        onEdit: (dt, row, branch) => {
            const showChanges = opening && Number(opening.id) === Number(branch.id);
            confirmOnOpen = Boolean(showChanges && opening.confirm);
            if (showChanges) opening = null;
            for (const tab of dt.DATA.tabsFolders) tab.selected = tab.id === (showChanges ? 'changes' : 'basic');
            dt.wjEdit(row);
        },
        serverSide: true,
        fetchOnCreate: true,
        fetchOnEdit: true,
        hideButtons: 'duplicate,export,import,celledit',
        order: [[columns.findIndex(column => column.data === 'id'), 'desc']]
    });
    function selected() {
        const rows = table.rows({selected: true}).data().toArray();
        return rows.length === 1 ? rows[0] : null;
    }
    function addAction(index, name, glyph, label, action, extraClass = '') {
        table.button().add(index, {
            text: icon(glyph).outerHTML,
            className: `btn btn-outline-secondary buttons-branch-${name} ${extraClass}`,
            attr: {title: label, 'aria-label': label, 'data-toggle': 'tooltip'},
            enabled: false,
            action: () => {
                const branch = selected();
                if (branch) action(branch);
            }
        });
    }
    function openChanges(branch, confirm = false) {
        if (table.button('.buttons-edit').processing()) return;
        opening = {id: branch.id, confirm};
        table.wjEditFetch($(table.row({selected: true}).node()));
    }
    const showError = error => WJ.notifyError(WJ.escapeHtml(error.message));
    addAction(4, 'compare', 'ti-list-check', tr('changes'), branch => openChanges(branch));
    addAction(5, 'preview', 'ti-external-link', tr('merge.preview'), branch => {
        openBranchPreview(api, branch.id).catch(showError);
    });
    addAction(6, 'publish', 'ti-world-upload', tr('merge.publish'), branch => openChanges(branch, true));
    addAction(7, 'open', 'ti-git-branch', tr('merge.switchToEdit'), async branch => {
        if (branch.state !== 'DRAFT') return;
        table.button('.buttons-branch-open').enable(false);
        try {
            await api('/context', {branchId: branch.id, expectedDomainId: screen.domainId});
            location.assign('/admin/v9/webpages/web-pages-list/');
        } catch (error) {
            showError(error);
            updateActions();
        }
    }, 'buttons-divider');
    function updateActions() {
        const branch = selected();
        table.button('.buttons-branch-compare').enable(Boolean(branch));
        table.button('.buttons-branch-preview').enable(previewStates.includes(branch?.state));
        table.button('.buttons-branch-publish').enable(publishStates.includes(branch?.state));
        table.button('.buttons-branch-open').enable(branch?.state === 'DRAFT' && Number(branch.id) !== Number(screen.branchId));
    }
    table.on('select deselect draw', updateActions);
    window.addEventListener('WJ.Branch.updated', () => table.ajax.reload(null, false));

    table.EDITOR.on('initCreate', () => {
        for (const tab of table.DATA.tabsFolders) tab.selected = tab.id === 'basic';
    });
    table.EDITOR.on('close', () => {
        for (const tab of table.DATA.tabsFolders) tab.selected = tab.id === 'basic';
        closeChanges?.();
        closeChanges = null;
        const url = new URL(location.href);
        if (url.searchParams.get('tab') === 'changes') {
            url.searchParams.delete('tab');
            history.replaceState(history.state, '', url);
        }
    });
    table.EDITOR.on('open', (event, mode, action) => {
        if (mode !== 'main' || !['edit', 'create'].includes(action)) return;
        const editor = table.EDITOR;
        if (action === 'edit' && editor.ids().length === 1) {
            closeChanges = mountBranchChanges(editor, api, screen, confirmOnOpen);
        } else {
            // Publishing actions apply to one working version at a time.
            $('#branchesDataTable_modal [data-tab-id="branchesDataTable-changes"], #branchesDataTable_modal [data-tab-id="branchesDataTable-publishing"]').closest('li').hide();
        }
        confirmOnOpen = false;
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
