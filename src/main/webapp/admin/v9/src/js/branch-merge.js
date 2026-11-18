import {icon, node as element} from './dashboard/widget-utils';

const commonKeys = {close: 'button.close', cancel: 'button.cancel', yes: 'button.yes', no: 'button.no', revert: 'branches.revert.js', viewItem: 'button.show'};
const tr = (key, args = []) => WJ.translate(commonKeys[key] || `branches.merge.${key}.js`, ...args);
const changeActionIcons = {viewItem: 'ti-eye', resolve: 'ti-tools', compareItem: 'ti-git-compare'};
const writtenPhases = new Set(['DATA', 'REFERENCES', 'TRASH_WAIT', 'TRASH_ROOT', 'EFFECTS']);
export const previewStates = ['DRAFT', 'SCHEDULED', 'NEEDS_ATTENTION'];
export const publishStates = ['DRAFT', 'MERGE_FAILED'];

function button(text, action, primary = false) {
    const el = element('button', `btn btn-sm ${primary ? 'btn-primary' : 'btn-outline-secondary'}`, text);
    el.type = 'button';
    el.addEventListener('click', action);
    return el;
}

function link(text, href) {
    const el = element('a', 'btn btn-sm btn-outline-secondary', text);
    el.href = href;
    el.target = '_blank';
    el.rel = 'noopener';
    return el;
}

function equal(left, right) { return JSON.stringify(left ?? null) === JSON.stringify(right ?? null); }
const itemKey = item => `${item.type}:${item.objectKey}`;
const showError = error => WJ.notifyError(WJ.escapeHtml(error.message));

/** Reserves the preview tab on the click, then navigates to the URL authorized by the server. */
export async function openBranchPreview(api, id, docId, fallback) {
    const tab = window.open('', '_blank');
    if (tab) tab.opener = null;
    try {
        if (!docId) {
            const report = await api(`/${id}/progress`);
            docId = report.items.find(item => item.type === 'DOCUMENT' &&
                (item.choice === 'LIVE' || !item.writeRequired && !item.conflict ? item.live : item.work)?.state === 'ACTIVE')?.objectKey;
        }
        const {url} = await api(`/${id}/preview-link${docId ? `?docId=${encodeURIComponent(docId)}` : ''}`);
        if (tab) tab.location.href = url;
        else if (fallback?.isConnected) {
            const anchor = link(tr('preview'), url);
            fallback.append(anchor);
            anchor.focus();
        } else {
            location.assign(url);
        }
    } catch (error) {
        tab?.close();
        throw error;
    }
}

/** Adds rendering for the annotation-generated changes columns. */
export function customizeBranchChangeColumns(columns) {
    const selection = columns.find(column => column.data === 'id');
    selection.render = () => '';
    selection.renderFormat = 'dt-format-none';
    selection.width = '50px';
    const itemColumn = columns.find(column => column.data === 'label');
    itemColumn.width = '30%';
    itemColumn.render = (value, type, row) => {
        if (type !== 'display') return value;
        const content = element('span');
        content.append(element('span', '', value));
        if (row.folderPath) content.append(element('small', 'pg-branch-change-path', row.folderPath));
        return content.outerHTML;
    };
    columns.find(column => column.data === 'operation').render = value => tr(value);
    const status = columns.find(column => column.data === 'status');
    status.width = '22%';
    status.render = (value, type, row) => {
        if (type !== 'display') return tr(value);
        const badge = element('span', 'md-branch-state', tr(value));
        if (row.problem) badge.dataset.state = 'NEEDS_ATTENTION';
        else if (value === 'done') badge.dataset.state = 'MERGED';
        else if (value === 'applying') badge.dataset.state = 'MERGING';
        return badge.outerHTML;
    };
    const comparison = columns.find(column => column.data === 'comparison');
    comparison.searchable = false;
    comparison.className = [comparison.className, 'text-end'].filter(Boolean).join(' ');
    comparison.render = (value, type, row) => {
        if (type !== 'display') return tr(value);
        const label = tr(value);
        const action = element('button', 'btn btn-sm btn-outline-secondary pg-branch-change-action text-nowrap');
        action.type = 'button';
        action.dataset.branchCompare = row.objectKey;
        action.setAttribute('aria-label', `${label} · ${row.label}`);
        action.append(icon(changeActionIcons[value]), ` ${label}`);
        return action.outerHTML;
    };
    comparison.createdCell = (cell, value, row) => {
        cell.parentElement.dataset.branchType = row.type;
        cell.parentElement.dataset.branchKey = row.objectKey;
    };
    return columns;
}

/** Connects the version's changes and publication controls to the lifetime of one Editor session. */
export function mountBranchChanges(editor, api, screen, confirmOnOpen) {
    const {id, state: initialState, revision: initialRevision} = editor.currentJson;
    const modal = document.getElementById('branchesDataTable_modal');
    const changes = document.getElementById('panel-body-dt-branchesDataTable-changes');
    const publishing = document.getElementById('branch-publishing-panel');
    const events = new AbortController();
    const {signal} = events;
    let report, timer, busy = false, stale = false, table, tableLoading = false, problemCheck;
    let tabClickId;
    let conflictNoticeShown = false;
    let activeTab = modal.querySelector('[role="tab"].active')?.dataset.tabId;
    let detailDialog, detailItem, expanded, confirmation;
    let scheduleForm, scheduleButton, scheduleKey;
    const settings = () => ['name', 'color', 'description'].map(name => editor.field(name).val());
    const savedSettings = settings();
    const dirty = () => !equal(settings(), savedSettings);
    const ready = () => Boolean(report?.ready && report.preparationHash && !stale);
    const items = () => report?.items || [];

    const footer = modal.querySelector('.DTE_Form_Buttons');
    const save = footer.querySelector('button.btn-primary');
    const publish = button(tr('publish'), () => {
        if (!ready() || busy || dirty()) return;
        confirmOnOpen = true;
        selectChanges();
        confirmWhenReady();
    });
    publish.id = 'branch-merge-publish';
    publish.classList.remove('btn-sm');
    const publishLabel = document.createTextNode('');
    publish.replaceChildren(icon('ti-world-upload'), publishLabel);
    footer.querySelector('.buttons-footer-left').append(publish);
    const cancel = footer.querySelector('.btn-close-editor');
    const cancelLabel = cancel.lastChild;
    const defaultCancel = cancelLabel.textContent;
    const title = modal.querySelector('.modal-title');
    const stateBadge = element('span', 'md-branch-state', tr(`state.${initialState}`));
    stateBadge.dataset.state = initialState;
    title.after(stateBadge);
    title.parentElement.classList.add('pg-branch-modal-heading');
    title.parentElement.id = 'branch-merge-title';

    const changesNotices = element('div', 'form-group pg-branch-changes-notices d-none');
    const messages = element('div', 'pg-branch-panel__messages');
    const settingsPending = element('p', 'alert alert-warning', tr('settingsPending'));
    settingsPending.hidden = true;
    messages.setAttribute('aria-live', 'polite');
    const loading = element('p', 'text-muted mb-0', tr('loading'));
    loading.setAttribute('role', 'status');
    const changesError = element('div', 'alert alert-danger d-none');
    const scheduleError = element('div', 'alert alert-danger d-none');
    const detailError = element('div', 'alert alert-danger d-none');
    for (const box of [changesError, scheduleError, detailError]) box.setAttribute('role', 'alert');
    changesNotices.append(changesError, messages, settingsPending);
    changes.prepend(changesNotices);
    const scheduleSummary = element('div');
    scheduleSummary.append(loading);
    const scheduleBody = element('div');
    publishing.replaceChildren(scheduleError, scheduleSummary, scheduleBody);

    function connectChangesTable(nested) {
        table = nested;
        const tableNode = table.table().node();
        tableNode.setAttribute('aria-label', tr('table'));
        const container = table.table().container();
        if (!container.querySelector('#branch-merge-problems')) {
            const problemsLabel = tr('problemsOnly');
            const label = element('label', 'form-check-label');
            label.htmlFor = 'branch-merge-problems';
            const input = element('input', 'form-check-input');
            input.id = label.htmlFor;
            input.type = 'checkbox';
            input.setAttribute('aria-label', problemsLabel);
            table.button().add(0, {
                name: 'branchRevert',
                text: icon('ti-arrow-back-up').outerHTML,
                className: 'btn-outline-secondary buttons-branch-revert',
                attr: {title: tr('revert'), 'aria-label': tr('revert'), 'data-toggle': 'tooltip'},
                enabled: false
            });
            const filter = element('div', 'custom-control form-switch buttons-divider-left pg-branch-problems');
            filter.title = problemsLabel;
            filter.append(input, label);
            table.button('branchRevert:name').node()[0].after(filter);
            WJ.initTooltip($(filter));
        }
        const revertButton = table.button('branchRevert:name');
        revertButton.action((event, dt, trigger) => {
            const row = selectedChange();
            if (!row?.revertible || busy || tableLoading || stale) return;
            const item = items().find(candidate => itemKey(candidate) === itemKey(row));
            if (item) confirmRevert(item, trigger[0]);
        });
        problemCheck = container.querySelector('#branch-merge-problems');
        problemCheck.checked = false;
        problemCheck.addEventListener('change', () => table.draw(), {signal});
        table.search.fixed('branch-problems', (search, row) => !problemCheck.checked || row.problem);
        table.on('select.dt.branchChanges deselect.dt.branchChanges draw.dt.branchChanges', updateTableControls);
        table.on('preXhr.dt.branchChanges', () => {
            tableLoading = true;
            clearConfirmation(false);
            updateControls();
        });
        table.on('xhr.dt.branchChanges', (event, settings, json, xhr) => {
            const nextReport = xhr?.responseJSON?.report;
            if (Number(nextReport?.id) !== Number(id)) return;
            tableLoading = false;
            changesError.classList.add('d-none');
            scheduleError.classList.add('d-none');
            stale = false;
            acceptReport(nextReport);
            render();
        });
        table.on('processing.dt.branchChanges', (event, settings, processing) => {
            if (!processing && tableLoading) {
                tableLoading = false;
                stale = true;
                setError(new Error(WJ.translate('datatables.error.network.js')), changesError);
                render();
            }
        });
        updateTableControls();
    }

    function selectedChange() {
        const selected = table?.rows({selected: true});
        if (selected?.count() !== 1) return null;
        return table.row({selected: true, search: 'applied'}).data();
    }

    function updateTableControls() {
        if (!table) return;
        table.button('branchRevert:name').enable(Boolean(!busy && !tableLoading && !stale && report?.state === 'DRAFT' && selectedChange()?.revertible));
        table.button('reload:name').enable(!busy);
        problemCheck.disabled = !report?.items?.length;
        for (const action of table.table().body().querySelectorAll('[data-branch-compare]')) {
            action.disabled = busy || tableLoading;
        }
    }

    window.addEventListener('WJ.DTE.innerTableInitialized', ({detail}) => {
        if (detail.conf.id !== 'DTE_Field_changes') return;
        tableLoading = true;
        connectChangesTable(detail.conf.datatable);
        updateControls();
    }, {signal});
    if (window.datatableInnerTable_changes) connectChangesTable(window.datatableInnerTable_changes);

    changes.addEventListener('click', event => {
        const action = event.target.closest('[data-branch-compare]');
        if (!action) return;
        const row = table?.row(action.closest('tr')).data();
        if (row) openDetail(itemKey(row), action);
    }, {signal});

    function selectChanges() {
        if (activeTab !== 'branchesDataTable-changes') modal.querySelector('#pills-dt-branchesDataTable-changes-tab').click();
    }

    window.addEventListener('WJ.DTE.tabclick', ({detail}) => {
        if (modal.contains(detail.element)) tabClickId = detail.id;
    }, {signal});
    modal.addEventListener('show.bs.tab', () => { tabClickId = null; }, {signal});
    modal.addEventListener('shown.bs.tab', event => {
        const tab = event.target;
        if (!tab.dataset.tabId?.startsWith('branchesDataTable-')) return;
        if (activeTab !== tab.dataset.tabId) clearConfirmation(false);
        activeTab = tab.dataset.tabId;
        modal.querySelector('.DTE_Body').classList.toggle('has-content', tab.dataset.hasContent === 'true');
        if (activeTab === 'branchesDataTable-changes') {
            // Bootstrap keyboard navigation does not use the wrapper's click handler.
            const id = activeTab;
            queueMicrotask(() => {
                if (!signal.aborted && activeTab === id && tabClickId !== id) {
                    WJ.dispatchEvent('WJ.DTE.tabclick', {element: tab, id});
                }
            });
        }
        updateControls();
        confirmWhenReady();
        poll();
    }, {signal});

    function updateControls() {
        const state = report?.state || initialState;
        const basic = activeTab === 'branchesDataTable-basic';
        const hasDraftSettings = dirty();
        save.hidden = !basic;
        save.disabled = busy;
        cancelLabel.textContent = basic || hasDraftSettings ? defaultCancel : ' ' + tr('close');
        publish.hidden = !publishStates.includes(state) || activeTab === 'branchesDataTable-publishing';
        publish.disabled = busy || tableLoading || !ready() || hasDraftSettings;
        publish.title = hasDraftSettings ? tr('settingsPending') : ready() ? '' : tr('publicationBlocked');
        publish.classList.toggle('btn-outline-secondary', basic);
        publish.classList.toggle('btn-primary', !basic);
        if (basic && publish.parentNode === footer) footer.querySelector('.buttons-footer-left').append(publish);
        if (!basic && publish.parentNode !== footer) cancel.after(publish);
        publishLabel.textContent = ' ' + tr(state === 'MERGE_FAILED' ? 'resume' : 'publish');
        settingsPending.hidden = !hasDraftSettings;
        if (scheduleButton) {
            scheduleButton.disabled = busy || tableLoading || !ready() || hasDraftSettings;
            scheduleForm.querySelector('.pg-branch-panel__settings-pending').hidden = !hasDraftSettings;
        }
        const saveChoice = detailDialog?.querySelector('#branch-merge-save-choice');
        if (saveChoice) saveChoice.disabled = busy || stale || !detailDialog.querySelector('input[name="branch-merge-choice"]:checked');
        updateTableControls();
        updateChangesNotices();
        changes.setAttribute('aria-busy', String(busy || tableLoading));
    }

    modal.addEventListener('input', updateControls, {signal});
    modal.addEventListener('change', updateControls, {signal});
    editor.on('preSubmit.branchChanges', () => activeTab === 'branchesDataTable-basic' && !busy);

    function setError(error, box) {
        box.textContent = error.message;
        box.classList.remove('d-none');
        if (box === detailError) detailDialog?.querySelector('[data-branch-retry]').classList.remove('d-none');
    }

    async function perform(path = '/progress', body, errorBox = changesError) {
        if (busy || signal.aborted) return;
        clearTimeout(timer);
        const active = document.activeElement;
        busy = true;
        updateControls();
        publishing.setAttribute('aria-busy', 'true');
        for (const box of [changesError, scheduleError, detailError]) box.classList.add('d-none');
        try {
            const nextReport = await api(`/${id}${path}`, body);
            stale = false;
            acceptReport(nextReport);
            if (!signal.aborted && table && activeTab === 'branchesDataTable-changes') {
                table.ajax.reload(null, false);
            }
            return report;
        } catch (error) {
            stale = true;
            if (!signal.aborted) {
                setError(error, errorBox);
                if (!body && errorBox === changesError) {
                    setError(error, scheduleError);
                    const retry = button(tr('refresh'), () => refresh());
                    retry.classList.add('ms-2');
                    scheduleError.append(' ', retry);
                }
            }
        } finally {
            busy = false;
            if (!signal.aborted) {
                publishing.removeAttribute('aria-busy');
                render();
                if (active instanceof HTMLElement && active.isConnected && (document.activeElement === document.body || document.activeElement === modal)) active.focus();
            }
        }
    }

    function acceptReport(nextReport) {
        const previousState = report?.state ?? initialState;
        const previousRevision = report?.revision ?? initialRevision;
        if (Number(nextReport.revision) < Number(previousRevision)) return;
        report = nextReport;
        if (previousState !== report.state || previousRevision !== report.revision) {
            window.dispatchEvent(new CustomEvent('WJ.Branch.updated', {detail: report}));
        }
    }

    function refresh() {
        clearConfirmation(false);
        if (table && activeTab === 'branchesDataTable-changes') table.ajax.reload(null, false);
        else return perform();
    }

    function poll() {
        clearTimeout(timer);
        if (stale || confirmation || detailDialog) return;
        if (report?.state === 'MERGING') timer = setTimeout(refresh, 1500);
        if (report?.state === 'SCHEDULED') timer = setTimeout(refresh, 5000);
    }

    function render() {
        loading.remove();
        stateBadge.dataset.state = report?.state || initialState;
        stateBadge.textContent = tr(`state.${stateBadge.dataset.state}`);
        if (report) {
            renderReport();
            renderSchedule();
            if (detailDialog && !stale) renderDetail();
        }
        updateControls();
        confirmWhenReady();
        poll();
    }

    function notice(parent, type, text) {
        const message = element('p', `alert alert-${type}`, text);
        parent.append(message);
        return message;
    }

    function renderReport() {
        const rows = items();
        const {state} = report;
        messages.replaceChildren();
        if (state === 'NEEDS_ATTENTION') {
            const warning = notice(messages, 'warning', tr('attentionNote'));
            const reopen = button(tr('reopen'), () => confirmState('reopen', messages, reopen));
            reopen.id = 'branch-merge-reopen';
            warning.append(reopen);
        }
        if (['MERGING', 'MERGE_FAILED', 'MERGED'].includes(state)) {
            const done = rows.filter(item => item.applyState === 'DONE').length;
            const errors = rows.filter(item => item.applyState === 'ERROR' || item.error).length;
            const progress = notice(messages, state === 'MERGE_FAILED' ? 'warning' : 'info',
                tr(state === 'MERGE_FAILED' ? 'partial' : state === 'MERGED' ? 'finished' : 'running'));
            progress.id = 'branch-merge-progress';
            progress.prepend(element('strong', 'd-block mb-1', tr('progress', [done, rows.length, errors, rows.length - done - errors])));
        }
        if (!conflictNoticeShown && rows.some(item => item.conflict)) {
            conflictNoticeShown = true;
            WJ.notifyWarning(WJ.translate('text.warning'), tr('resolveBeforePublish'), 7000);
        }
        if (rows.length && report.errors?.length) {
            const list = element('ul', 'alert alert-danger ps-5');
            list.setAttribute('aria-label', tr('blockers'));
            report.errors.forEach(error => list.append(element('li', '', error)));
            messages.append(list);
        }
        if (report.lastError) notice(messages, 'danger', report.lastError);
    }

    function updateChangesNotices() {
        changesNotices.classList.toggle('d-none', !messages.childElementCount && settingsPending.hidden &&
            changesError.classList.contains('d-none') && confirmation?.panel.parentElement !== changesNotices);
    }

    function focusItem(type, key) {
        const row = table?.rows({page: 'current'}).nodes().toArray()
            .find(row => row.dataset.branchType === type && row.dataset.branchKey === key);
        (row?.querySelector('[data-branch-compare]') || table?.button('reload:name').node()[0])?.focus();
    }

    function clearConfirmation(restoreFocus = true) {
        if (!confirmation) return;
        const {panel, invoker} = confirmation;
        confirmation = null;
        panel.remove();
        updateChangesNotices();
        if (restoreFocus) {
            (invoker.isConnected && !invoker.disabled ? invoker : table?.button('reload:name').node()[0])?.focus();
            poll();
        }
    }

    function confirmAction(parent, invoker, {heading, note, summary, label, confirmId, action}) {
        if (confirmation) return confirmation.panel.focus();
        clearTimeout(timer);
        const panel = element('section', 'alert alert-warning pg-branch-panel__confirmation');
        panel.id = 'branch-merge-confirmation';
        panel.tabIndex = -1;
        panel.setAttribute('aria-labelledby', 'branch-merge-confirmation-title');
        const title = element('h3', 'h5', heading);
        title.id = 'branch-merge-confirmation-title';
        const actions = element('div', 'd-flex flex-wrap gap-2');
        const cancel = button(tr('cancel'), () => clearConfirmation());
        const confirm = button(label, async () => {
            if (busy) return;
            confirm.disabled = cancel.disabled = true;
            const result = await action();
            if (signal.aborted) return;
            clearConfirmation(false);
            if (!result) (parent === scheduleSummary ? scheduleError : changesError).focus();
            else (parent === scheduleSummary ? scheduleSummary : messages).focus();
            poll();
        }, true);
        confirm.id = confirmId;
        actions.append(cancel, confirm);
        panel.append(title, element('p', '', note));
        if (summary) panel.append(element('p', 'fw-semibold', summary));
        panel.append(actions);
        parent.before(panel);
        confirmation = {panel, invoker};
        updateChangesNotices();
        panel.focus();
    }

    for (const node of [changesError, scheduleError, messages, scheduleSummary]) node.tabIndex = -1;

    function confirmPublication() {
        const resume = report.state === 'MERGE_FAILED';
        const count = items().filter(item => item.writeRequired && item.applyState !== 'DONE').length;
        confirmAction(messages, publish, {
            heading: tr(resume ? 'resume' : 'publish'),
            note: tr('publishNote'),
            summary: tr('publishCount', [count]),
            label: tr(resume ? 'resumeConfirm' : 'publishConfirm'),
            confirmId: 'branch-merge-confirm',
            action: () => perform(resume ? '/resume' : '/merge', {
                expectedRevision: report.revision, preparationHash: report.preparationHash
            })
        });
    }

    function confirmWhenReady() {
        if (!confirmOnOpen || !report || busy || tableLoading || activeTab !== 'branchesDataTable-changes') return;
        confirmOnOpen = false;
        if (ready() && publishStates.includes(report.state)) confirmPublication();
        else if (messages.childElementCount) messages.focus();
    }

    function confirmRevert(item, invoker) {
        confirmAction(messages, invoker, {
            heading: tr('revert'),
            note: tr('revertConfirm', [item.label || item.objectKey]),
            summary: item.type === 'GROUP' && item.operation === 'DELETE' ? tr('revertFolderNote') : '',
            label: tr('revert'),
            confirmId: 'branch-merge-revert-confirm',
            action: () => perform('/changes/revert', {
                type: item.type, id: Number(item.objectKey), hash: item.workHash
            })
        });
    }

    function confirmState(action, parent, invoker) {
        confirmAction(parent, invoker, {
            heading: tr(action),
            note: tr(action + 'Note'),
            label: tr(action),
            confirmId: `branch-merge-${action}-confirm`,
            action: () => perform('/' + action, {expectedRevision: report.revision}, parent === scheduleSummary ? scheduleError : changesError)
        });
    }

    function formatTerm(value) {
        return `${new Date(value).toLocaleString(document.documentElement.lang || undefined, {timeZone: report.timeZone})} (${report.timeZone})`;
    }

    // Native datetime-local fields contain a wall time without an offset. Resolve it in the publisher's zone.
    function wallTime(value) {
        const parts = new Intl.DateTimeFormat('en-CA', {
            timeZone: report.timeZone, year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
        }).formatToParts(new Date(value));
        const part = type => parts.find(item => item.type === type).value;
        return `${part('year')}-${part('month')}-${part('day')}T${part('hour')}:${part('minute')}`;
    }

    function scheduledInstant(value) {
        const utc = Date.parse(`${value}:00Z`);
        if (!Number.isFinite(utc)) return null;
        let instant = utc;
        for (let i = 0; i < 3; i++) instant += utc - Date.parse(`${wallTime(instant)}:00Z`);
        // Repeated or missing local times around a DST change need an unambiguous term.
        if (wallTime(instant) !== value || [-7200000, -3600000, 3600000, 7200000].some(offset => wallTime(instant + offset) === value)) return null;
        return instant;
    }

    function renderSchedule() {
        const scheduled = report.state === 'SCHEDULED';
        const canSchedule = scheduled || report.state === 'DRAFT';
        const key = `${report.state}:${report.publishAt}`;
        if (scheduleKey !== key) {
            scheduleKey = key;
            scheduleBody.replaceChildren();
            scheduleSummary.replaceChildren();
            scheduleForm = scheduleButton = null;
            if (scheduled) {
                const plan = notice(scheduleSummary, 'info', tr('scheduledNote'));
                plan.id = 'branch-merge-scheduled';
                plan.prepend(element('strong', 'd-block mb-1', tr('scheduledAt', [formatTerm(report.publishAt)])));
                const unschedule = button(tr('unschedule'), () => confirmState('unschedule', scheduleSummary, unschedule));
                unschedule.id = 'branch-merge-unschedule';
                scheduleBody.append(unschedule);
            }
            if (canSchedule) {
                scheduleForm = makeScheduleForm(scheduled);
                scheduleBody.prepend(scheduleForm);
            } else {
                notice(scheduleSummary, 'secondary', tr('schedulingUnavailable'));
            }
        }
        if (scheduleForm) {
            const reason = scheduleForm.querySelector('.pg-branch-panel__schedule-reason');
            reason.hidden = ready();
            scheduleForm.querySelector('input').disabled = !ready();
        }
    }

    function makeScheduleForm(scheduled) {
        const form = element('form', 'pg-branch-panel__schedule');
        form.id = 'branch-merge-schedule-form';
        const label = element('label', 'form-label fw-semibold', tr('publishAt'));
        const input = element('input', 'form-control');
        input.type = 'datetime-local';
        input.required = true;
        input.step = '60';
        input.id = 'branch-merge-publish-at';
        label.htmlFor = input.id;
        if (report.publishAt) input.value = wallTime(report.publishAt);
        const zone = element('p', 'form-text mb-0', tr('timeZone', [report.timeZone]));
        zone.id = 'branch-merge-time-zone';
        input.setAttribute('aria-describedby', zone.id);
        const feedback = element('p', 'text-danger mb-0');
        feedback.setAttribute('role', 'alert');
        input.addEventListener('input', () => {
            input.setCustomValidity('');
            input.removeAttribute('aria-invalid');
            feedback.textContent = '';
        });
        const fields = element('div', 'pg-branch-panel__schedule-fields mb-3');
        fields.append(label, input, zone, feedback);
        const reason = element('p', 'alert alert-warning pg-branch-panel__schedule-reason', tr('publicationBlocked') + ' ');
        const review = button(tr('changesTab'), selectChanges);
        reason.append(review);
        scheduleButton = button(tr(scheduled ? 'reschedule' : 'schedule'), () => {}, true);
        scheduleButton.type = 'submit';
        scheduleButton.id = 'branch-merge-schedule-confirm';
        const settingsPending = element('p', 'alert alert-warning pg-branch-panel__settings-pending', tr('settingsPending'));
        form.append(reason, settingsPending, fields, element('p', 'text-muted', tr('scheduleHint')), scheduleButton);
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (busy || !ready() || dirty()) return;
            const publishAt = scheduledInstant(input.value);
            if (publishAt === null || publishAt <= Date.now()) {
                const message = tr(publishAt === null ? 'invalidWallTime' : 'futureTime');
                feedback.textContent = message;
                input.setCustomValidity(message);
                input.setAttribute('aria-invalid', 'true');
                input.reportValidity();
                input.focus();
                return;
            }
            const result = await perform('/schedule', {expectedRevision: report.revision, preparationHash: report.preparationHash, publishAt}, scheduleError);
            if (result && !signal.aborted) scheduleSummary.focus();
        });
        return form;
    }

    function openDetail(key, invoker) {
        const item = items().find(item => itemKey(item) === key);
        if (busy || tableLoading || !item) return;
        detailItem = item;
        expanded = false;
        detailError.classList.add('d-none');
        const dialog = detailDialog = element('dialog', 'pg-branch-merge');
        dialog.setAttribute('aria-labelledby', 'branch-item-title');
        const header = element('header', 'pg-branch-merge__header');
        const title = element('h2', 'h4 mb-0');
        title.id = 'branch-item-title';
        const close = button(tr('close'), () => dialog.close());
        header.append(title, close);
        const content = element('div', 'pg-branch-merge__content');
        const retry = button(tr('refresh'), () => perform('/progress', undefined, detailError));
        retry.dataset.branchRetry = '';
        retry.classList.add('d-none', 'mb-3');
        content.append(detailError, retry, element('div', 'pg-branch-merge__item'));
        dialog.append(header, content);
        dialog.addEventListener('keydown', event => event.stopPropagation());
        dialog.addEventListener('close', () => {
            detailDialog = null;
            dialog.remove();
            if (!signal.aborted) {
                if (invoker.isConnected) invoker.focus();
                else focusItem(item.type, item.objectKey);
                poll();
            }
        }, {once: true});
        modal.append(dialog);
        renderDetail(true);
        dialog.showModal();
        close.focus();
        poll();
    }

    function renderDetail(force = false) {
        const item = items().find(row => itemKey(row) === itemKey(detailItem));
        if (!item) { detailDialog.close(); return; }
        if (!force && equal(detailItem, item)) return;
        detailItem = item;
        detailDialog.classList.toggle('pg-branch-merge--single', item.workOnly === true);
        detailDialog.querySelector('#branch-item-title').textContent = `${item.label || item.objectKey} · ${tr(`type.${item.type}`)}`;
        detailDialog.querySelector('[data-branch-retry]').classList.add('d-none');
        const content = detailDialog.querySelector('.pg-branch-merge__item');
        const focused = content.contains(document.activeElement) ? document.activeElement.id : null;
        content.replaceChildren(detail(item));
        if (focused) detailDialog.querySelector('#' + CSS.escape(focused))?.focus();
    }

    async function edit(item) {
        if (report.state !== 'DRAFT') return;
        try {
            await api('/context', {branchId: id, expectedDomainId: screen.domainId});
            const fields = item.work?.fields || {};
            const query = item.type === 'DOCUMENT' ? `docid=${encodeURIComponent(item.objectKey)}`
                : item.type === 'MEDIA' ? `docid=${encodeURIComponent(fields.media_fk_id)}`
                : item.type === 'GROUP' ? `groupid=${encodeURIComponent(item.objectKey)}` : '';
            location.href = `/admin/v9/webpages/web-pages-list/?${query}`;
        } catch (error) { showError(error); }
    }

    function detail(item) {
        const workOnly = item.workOnly === true;
        const section = element('section', 'pg-branch-merge__detail');
        section.tabIndex = -1;
        section.setAttribute('aria-label', tr(workOnly ? 'work' : 'comparison'));
        const preview = previewStates.includes(report.state) && item.type === 'DOCUMENT'
            ? button(tr('preview'), () => openBranchPreview(api, id, item.objectKey, section).catch(showError)) : null;
        if (!workOnly) section.append(element('p', 'small', tr('wholeItem')));
        if (item.error) section.append(element('p', 'alert alert-danger', item.error));
        if (item.applyPhase === 'TRASH_WAIT') section.append(element('p', 'alert alert-warning', tr('trashWaitNote')));
        else if (item.applyState === 'ERROR' && writtenPhases.has(item.applyPhase)) section.append(element('p', 'alert alert-warning', tr('contentWrittenNote')));
        if (item.applyPhase === 'FILE_STARTED') section.append(element('p', 'alert alert-warning', tr('fileStateNote')));
        if (item.type === 'DOCUMENT' && item.choice === 'LIVE' && item.writeRequired) section.append(element('p', 'alert alert-info', tr('liveMoveNote')));
        if (item.base?.state !== 'ABSENT' && item.base?.fields?.virtual_path !== item.work?.fields?.virtual_path && item.type === 'DOCUMENT') section.append(element('p', 'alert alert-warning', tr('redirectNote')));
        const versions = workOnly ? ['work'] : ['base', 'live', 'work'];
        const allFields = [...new Set(['title', 'group_name', 'data', ...versions.flatMap(name => Object.keys(item[name]?.fields || {}))])]
            .filter(key => versions.some(name => Object.hasOwn(item[name]?.fields || {}, key)));
        const differing = key => !equal(item.base?.fields?.[key], item.live?.fields?.[key]) || !equal(item.base?.fields?.[key], item.work?.fields?.[key]);
        if (!workOnly) {
            const show = element('label', 'form-check-label mb-3');
            const check = element('input', 'form-check-input me-2'); check.id = 'branch-merge-all-fields'; check.type = 'checkbox'; check.checked = expanded;
            check.onchange = () => { expanded = check.checked; renderDetail(true); detailDialog?.querySelector('#branch-merge-all-fields').focus(); };
            show.append(check, document.createTextNode(tr('allFields'))); section.append(show);
        }
        const visibleFields = workOnly ? allFields.filter(key => item.work.fields[key] != null && item.work.fields[key] !== '')
            : allFields.filter(key => expanded || differing(key) || ['title', 'group_name', 'data'].includes(key));
        const columns = element('div', 'pg-branch-merge__versions');
        for (const version of versions) {
            const snapshot = item[version] || {state: 'ABSENT', fields: {}};
            const panel = element('section', 'pg-branch-merge__version');
            panel.dataset.branchVersion = version.toUpperCase();
            panel.append(element('h4', 'h6', tr(version)), element('p', 'small', tr(`existence.${snapshot.state}`)));
            if (workOnly && preview) panel.append(preview);
            if (item.type === 'FILE' && snapshot.state !== 'ABSENT' && snapshot.fields.entry_kind !== 'DIRECTORY') {
                const url = `/admin/rest/branches/${id}/file-version?path=${encodeURIComponent(item.objectKey)}&version=${version.toUpperCase()}`;
                if (/\.(png|jpe?g|gif|webp|avif)$/i.test(item.objectKey)) {
                    const image = element('img', 'pg-branch-merge__image'); image.src = url; image.alt = `${tr(version)} · ${item.objectKey}`; panel.append(image);
                }
                panel.append(link(tr('openFile'), url));
            }
            const fields = element('dl', 'mb-0');
            for (const field of visibleFields) {
                const changed = !workOnly && version !== 'base' && !equal(item.base?.fields?.[field], snapshot.fields?.[field]);
                const label = report.fieldLabels?.[item.type]?.[field] || field.replaceAll('_', ' ');
                const dt = element('dt', 'small mt-3', `${label}${changed ? ` · ${tr('changed')}` : ''}`);
                const dd = element('dd', changed ? 'pg-branch-merge__changed' : '');
                const value = snapshot.fields?.[field];
                if (snapshot.state === 'ABSENT' || value === null || value === undefined || value === '') dd.textContent = '—';
                else if (['data', 'html_head', 'html_data'].includes(field)) {
                    // Readable text is never inserted as HTML; scripts and remote resources cannot run here.
                    const doc = new DOMParser().parseFromString(String(value), 'text/html');
                    doc.querySelectorAll('script,style,template').forEach(node => node.remove());
                    doc.querySelectorAll('br,p,div,li,h1,h2,h3,h4,h5,h6').forEach(node => node.append(document.createTextNode('\n')));
                    dd.append(element('pre', 'pg-branch-merge__text', doc.body.textContent.trim()));
                    const source = element('details'); source.append(element('summary', 'small', tr('htmlSource')), element('pre', 'pg-branch-merge__text', String(value))); dd.append(source);
                } else if (typeof value === 'boolean') dd.textContent = tr(value ? 'yes' : 'no');
                else if (['date_created', 'event_date'].includes(field) && typeof value === 'number') dd.textContent = new Date(value).toLocaleString(document.documentElement.lang || undefined);
                else dd.textContent = Array.isArray(value) ? value.join(', ') || '—' : String(value);
                fields.append(dt, dd);
            }
            panel.append(fields); columns.append(panel);
        }
        section.append(columns);
        const canResolve = item.resolvable ?? (item.editable && report.state === 'DRAFT');
        if (canResolve) {
            const choices = element('fieldset', 'border rounded p-3 mt-3');
            choices.append(element('legend', 'float-none w-auto h6', tr('result')));
            if (workOnly) choices.append(element('p', 'small', tr('wholeItem')));
            for (const choice of ['LIVE', 'WORK']) {
                const label = element('label', 'form-check-label d-block mb-2');
                const radio = element('input', 'form-check-input me-2'); radio.type = 'radio'; radio.name = 'branch-merge-choice'; radio.value = choice; radio.checked = item.choice === choice;
                const deleted = item[choice.toLowerCase()]?.state !== 'ACTIVE';
                const key = workOnly ? (choice === 'LIVE' ? 'skipItem' : 'publishItem')
                    : choice === 'LIVE' ? 'useLive' : deleted ? 'useDelete' : item.live?.state !== 'ACTIVE' ? 'useRestore' : 'useWork';
                label.append(radio, document.createTextNode(tr(key))); choices.append(label);
            }
            const save = button(tr('saveChoice'), async () => {
                if (busy || stale) return;
                const choice = choices.querySelector('input:checked')?.value;
                if (!choice) return;
                await perform('/resolve', {expectedRevision: report.revision, type: item.type, objectKey: item.objectKey, choice, liveHash: item.liveHash, workHash: item.workHash}, detailError);
            }, true);
            save.disabled = !item.choice;
            save.id = 'branch-merge-save-choice'; choices.append(save); section.append(choices);
        }
        if (report.state === 'DRAFT' && item.editable) {
            const actions = element('div', 'd-flex flex-wrap gap-2 mt-3');
            actions.append(button(tr(item.type === 'FILE' ? 'editWeb' : 'editItem'), () => edit(item)));
            section.append(actions, element('p', 'small mt-2', tr(item.type === 'GROUP' ? 'editGroupNote' : item.type === 'MEDIA' ? 'editMediaNote' : item.type === 'FILE' ? 'editFileNote' : 'editNote')));
        }
        if (item.type === 'DOCUMENT' && item.live?.state === 'ACTIVE') section.append(link(tr('production'), `/showdoc.do?docid=${encodeURIComponent(item.objectKey)}`));
        if (!workOnly && preview) section.append(preview);
        return section;
    }

    updateControls();
    if (activeTab !== 'branchesDataTable-changes') perform();
    return () => {
        events.abort();
        clearTimeout(timer);
        detailDialog?.close();
        table?.off('.branchChanges');
        table?.button('branchRevert:name').action(() => {});
        changesNotices.remove();
        changes.removeAttribute('aria-busy');
        publishing.replaceChildren();
        editor.off('preSubmit.branchChanges');
    };
}
