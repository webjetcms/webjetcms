const commonKeys = {close: 'button.close', cancel: 'button.cancel', yes: 'button.yes', no: 'button.no'};
const tr = (key, args = []) => WJ.translate(commonKeys[key] || `branches.merge.${key}.js`, ...args);
const types = ['DOCUMENT', 'GROUP', 'MEDIA', 'FILE'];
const writtenPhases = new Set(['DATA', 'REFERENCES', 'TRASH_WAIT', 'TRASH_ROOT', 'EFFECTS']);

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

function button(text, action, primary = false) {
    const node = element('button', `btn btn-sm ${primary ? 'btn-primary' : 'btn-outline-secondary'}`, text);
    node.type = 'button';
    node.addEventListener('click', action);
    return node;
}

function link(text, href) {
    const node = element('a', 'btn btn-sm btn-outline-secondary', text);
    node.href = href;
    node.target = '_blank';
    node.rel = 'noopener';
    return node;
}

function equal(left, right) { return JSON.stringify(left ?? null) === JSON.stringify(right ?? null); }

/** Opens the whole-package comparison without changing the administrator's editing context. */
export async function openBranchMerge(id, api, screen, invoker) {
    document.getElementById('branch-merge-dialog')?.close();
    const dialog = element('dialog', 'pg-branch-merge');
    dialog.id = 'branch-merge-dialog';
    dialog.setAttribute('aria-labelledby', 'branch-merge-title');
    const header = element('header', 'pg-branch-merge__header');
    const title = element('h2', 'h4 mb-0', tr('title'));
    title.id = 'branch-merge-title';
    const close = button(tr('close'), () => dialog.close());
    header.append(title, close);
    const content = element('div', 'pg-branch-merge__content');
    const status = element('p', 'small mb-0');
    status.setAttribute('role', 'status');
    status.setAttribute('aria-live', 'polite');
    const errorBox = element('div', 'alert alert-danger d-none');
    errorBox.setAttribute('role', 'alert');
    const overview = element('div');
    content.append(status, errorBox, overview);
    dialog.append(header, content);
    let report, timer, busy = false, prepared = false, filter = '', problemsOnly = false, selection = null;
    const expanded = new Set();
    dialog.addEventListener('close', () => {
        clearTimeout(timer);
        dialog.remove();
        if (invoker?.isConnected) invoker.focus();
        const url = new URL(location.href);
        const fragment = new URLSearchParams(url.hash.substring(1));
        if (fragment.has('branchCompare')) {
            fragment.delete('branchCompare');
            url.hash = fragment.toString();
        }
        if (url.searchParams.has('branchCompare')) {
            url.searchParams.delete('branchCompare');
        }
        history.replaceState(null, '', url);
    });
    document.body.append(dialog);
    dialog.showModal();
    close.focus();

    function failure(error) {
        prepared = false;
        errorBox.classList.remove('d-none');
        errorBox.textContent = `${error.message} ${tr('compareAgain')}`;
    }

    async function perform(path, body, isPreparation = false) {
        if (busy) return;
        const activeId = document.activeElement?.id;
        busy = true;
        errorBox.classList.add('d-none');
        content.setAttribute('aria-busy', 'true');
        overview.querySelectorAll('button').forEach(node => { node.disabled = true; });
        try {
            report = await api(`/${id}${path}`, body);
            prepared = isPreparation;
            window.dispatchEvent(new CustomEvent('WJ.Branch.updated', {detail: report}));
        } catch (error) { failure(error); }
        finally {
            busy = false;
            content.removeAttribute('aria-busy');
            if (dialog.open && report) {
                render();
                if (activeId) {
                    const target = dialog.querySelector(`#${CSS.escape(activeId)}`)
                        || overview.querySelector('#branch-merge-schedule, #branch-merge-publish, #branch-merge-reopen') || close;
                    target.focus();
                }
            }
        }
    }

    function compare() {
        return perform('/prepare', {expectedRevision: report.revision}, true);
    }

    function itemStatus(item) {
        if (item.applyState === 'DONE') return tr(item.applyPhase === 'NOOP' ? 'noop' : 'done');
        if (item.applyState === 'ERROR' && writtenPhases.has(item.applyPhase)) return tr('contentWrittenError');
        if (item.error || item.applyState === 'ERROR') return tr('error');
        if (item.applyState === 'APPLYING') return tr('applying');
        if (item.conflict) return tr('conflict');
        return tr(item.writeRequired ? 'willPublish' : 'noChange');
    }

    function operation(item) {
        return tr(item.operation === 'DELETE' ? 'deleted' : item.base?.state === 'ABSENT' ? 'new' : 'updated');
    }

    async function edit(item) {
        if (!report || report.state !== 'DRAFT') return;
        try {
            await api('/context', {branchId: id, expectedDomainId: screen.domainId});
            const fields = item.work?.fields || {};
            const query = item.type === 'DOCUMENT' ? `docid=${encodeURIComponent(item.objectKey)}`
                : item.type === 'MEDIA' ? `docid=${encodeURIComponent(fields.media_fk_id)}`
                : item.type === 'GROUP' ? `groupid=${encodeURIComponent(item.objectKey)}` : '';
            location.href = `/admin/v9/webpages/web-pages-list/?${query}`;
        } catch (error) { failure(error); }
    }

    async function preview(docId, parent) {
        // Open the tab within the user gesture, then obtain the authorized, effective URL.
        const tab = window.open('', '_blank');
        if (tab) tab.opener = null;
        try {
            const result = await api(`/${id}/preview-link${docId ? `?docId=${encodeURIComponent(docId)}` : ''}`);
            if (tab) tab.location.href = result.url;
            else { const anchor = link(tr('preview'), result.url); parent.append(anchor); anchor.focus(); }
        } catch (error) { tab?.close(); failure(error); }
    }

    function render() {
        clearTimeout(timer);
        title.textContent = `${report.name} · ${tr(`state.${report.state}`)}`;
        overview.replaceChildren();
        const items = report.items || [];
        const done = items.filter(item => item.applyState === 'DONE').length;
        const errors = items.filter(item => item.applyState === 'ERROR' || item.error).length;
        const pending = items.length - done - errors;
        const conflicts = items.filter(item => item.conflict).length;
        const running = report.state === 'MERGING';
        const failed = report.state === 'MERGE_FAILED';
        const draft = report.state === 'DRAFT';
        const scheduled = report.state === 'SCHEDULED';
        const attention = report.state === 'NEEDS_ATTENTION';
        status.textContent = `${report.domainName} · ${tr('items', [items.length])} · ${tr('conflicts', [conflicts])}`;
        const toolbar = element('div', 'd-flex flex-wrap gap-2 my-3');
        toolbar.append(button(tr('compare'), compare));
        if (draft) toolbar.append(button(tr('editWeb'), () => edit({type: 'FILE'})));
        const publicItem = items.find(item => item.type === 'DOCUMENT' && item.live?.state === 'ACTIVE');
        if (publicItem) toolbar.append(link(tr('production'), `/showdoc.do?docid=${encodeURIComponent(publicItem.objectKey)}`));
        if (draft || scheduled || attention) {
            const target = items.find(item => item.type === 'DOCUMENT' && (item.choice === 'LIVE' || !item.writeRequired && !item.conflict ? item.live : item.work)?.state === 'ACTIVE');
            toolbar.append(button(tr('preview'), () => preview(target?.objectKey, toolbar)));
        }
        if (draft || failed) {
            const publish = button(tr(failed ? 'resume' : 'publish'), () => confirmPublish(items, failed), true);
            publish.id = 'branch-merge-publish';
            publish.disabled = !prepared || !report.ready || !report.preparationHash || conflicts > 0;
            toolbar.append(publish);
        }
        if (draft || scheduled) {
            const schedule = button(tr(scheduled ? 'reschedule' : 'schedule'), () => scheduleForm(items));
            schedule.id = 'branch-merge-schedule';
            schedule.disabled = !prepared || !report.ready || !report.preparationHash || conflicts > 0;
            toolbar.append(schedule);
        }
        if (scheduled) {
            const cancel = button(tr('unschedule'), () => confirmState('unschedule', 'unscheduleNote'));
            cancel.id = 'branch-merge-unschedule'; toolbar.append(cancel);
        }
        if (attention) {
            const reopen = button(tr('reopen'), () => confirmState('reopen', 'reopenNote'));
            reopen.id = 'branch-merge-reopen'; toolbar.append(reopen);
        }
        overview.append(toolbar);
        if (scheduled) {
            const plan = element('div', 'alert alert-info'); plan.id = 'branch-merge-scheduled';
            plan.append(element('p', 'fw-bold mb-1', tr('scheduledAt', [formatTerm(report.publishAt)])));
            plan.append(element('p', 'mb-0', tr('scheduledNote'))); overview.append(plan);
        }
        if (attention) overview.append(element('p', 'alert alert-warning', tr('attentionNote')));
        if (failed || running || report.state === 'MERGED') {
            const progress = element('div', `alert ${failed ? 'alert-warning' : 'alert-info'}`);
            progress.id = 'branch-merge-progress';
            progress.append(element('p', 'mb-1', tr('progress', [done, items.length, errors, pending])));
            progress.append(element('p', 'mb-0', tr(failed ? 'partial' : running ? 'running' : 'finished')));
            overview.append(progress);
        }
        if (report.errors?.length) {
            const list = element('ul', 'alert alert-danger ps-5');
            list.setAttribute('aria-label', tr('blockers'));
            report.errors.forEach(error => list.append(element('li', '', error)));
            overview.append(list);
        } else if (draft && prepared && report.ready) {
            overview.append(element('p', 'alert alert-success', tr('ready')));
        }
        if (report.lastError) overview.append(element('p', 'alert alert-danger', report.lastError));
        const filters = element('div', 'd-flex flex-wrap gap-3 align-items-center my-3');
        const typeLabel = element('label', 'form-label mb-0', tr('filter'));
        const typeSelect = element('select', 'form-select form-select-sm w-auto');
        typeSelect.id = 'branch-merge-type';
        typeLabel.htmlFor = typeSelect.id;
        typeSelect.add(new Option(tr('all'), ''));
        types.forEach(type => typeSelect.add(new Option(tr(`type.${type}`), type)));
        typeSelect.value = filter;
        typeSelect.onchange = () => { filter = typeSelect.value; render(); dialog.querySelector('#branch-merge-type').focus(); };
        const problemLabel = element('label', 'form-check-label');
        const problemCheck = element('input', 'form-check-input me-2');
        problemCheck.id = 'branch-merge-problems'; problemCheck.type = 'checkbox'; problemCheck.checked = problemsOnly;
        problemCheck.onchange = () => { problemsOnly = problemCheck.checked; render(); dialog.querySelector('#branch-merge-problems').focus(); };
        problemLabel.append(problemCheck, document.createTextNode(tr('problemsOnly')));
        filters.append(typeLabel, typeSelect, problemLabel);
        overview.append(filters);
        const tableWrap = element('div', 'table-responsive');
        const table = element('table', 'table table-hover pg-branch-merge__table');
        const caption = element('caption', 'visually-hidden', tr('table'));
        const head = element('thead');
        const heading = element('tr');
        ['typeLabel', 'item', 'operation', 'result', 'action'].forEach(key => {
            const th = element('th', '', tr(key)); th.scope = 'col'; heading.append(th);
        });
        head.append(heading);
        const body = element('tbody');
        items.filter(item => (!filter || item.type === filter) && (!problemsOnly || item.conflict || item.error || item.applyState === 'ERROR')).forEach(item => {
            const row = element('tr');
            row.dataset.branchType = item.type; row.dataset.branchKey = item.objectKey;
            const cell = text => element('td', '', text);
            row.append(cell(tr(`type.${item.type}`)), cell(item.label || item.objectKey), cell(operation(item)), cell(itemStatus(item)));
            const actions = cell('');
            const show = button(tr(item.conflict ? 'resolve' : 'compareItem'), () => {
                selection = `${item.type}:${item.objectKey}`;
                render();
                overview.querySelector('.pg-branch-merge__detail')?.focus();
            });
            show.dataset.branchCompare = item.objectKey;
            actions.append(show); row.append(actions); body.append(row);
        });
        if (!body.children.length) { const row = element('tr'); const cell = element('td', '', tr('empty')); cell.colSpan = 5; row.append(cell); body.append(row); }
        table.append(caption, head, body); tableWrap.append(table); overview.append(tableWrap);
        const current = items.find(item => `${item.type}:${item.objectKey}` === selection);
        if (current) overview.append(detail(current));
        if (running) timer = setTimeout(() => perform('/progress'), 1500);
        else if (scheduled) timer = setTimeout(() => perform('/progress', undefined, prepared), 5000);
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

    function closePanel(panel, focusId) {
        panel.remove(); render(); overview.querySelector(`#${focusId}`)?.focus();
    }

    function confirmState(action, note) {
        clearTimeout(timer);
        const existing = overview.querySelector('#branch-merge-confirmation');
        if (existing) { existing.focus(); return; }
        const panel = element('section', 'alert alert-warning');
        panel.id = 'branch-merge-confirmation'; panel.tabIndex = -1;
        panel.append(element('h3', 'h5', tr(action)), element('p', '', tr(note)));
        panel.append(button(tr('cancel'), () => closePanel(panel, `branch-merge-${action}`)), document.createTextNode(' '));
        const confirm = button(tr(action), async () => {
            await perform(`/${action}`, {expectedRevision: report.revision});
            if (report.state === 'DRAFT') await compare();
        }, true);
        confirm.id = `branch-merge-${action}-confirm`; panel.append(confirm);
        overview.prepend(panel); panel.focus();
    }

    function scheduleForm(items) {
        clearTimeout(timer);
        const existing = overview.querySelector('#branch-merge-schedule-form');
        if (existing) { existing.querySelector('input').focus(); return; }
        overview.querySelector('#branch-merge-confirmation')?.remove();
        const form = element('form', 'pg-branch-merge__schedule alert alert-info');
        form.id = 'branch-merge-schedule-form';
        const heading = element('h3', 'h5', tr(report.state === 'SCHEDULED' ? 'reschedule' : 'schedule'));
        heading.id = 'branch-merge-schedule-title'; form.setAttribute('aria-labelledby', heading.id);
        const fields = element('div', 'pg-branch-merge__schedule-fields');
        const label = element('label', 'form-label', tr('publishAt'));
        const input = element('input', 'form-control'); input.type = 'datetime-local'; input.required = true;
        input.id = 'branch-merge-publish-at'; input.name = 'publishAt'; input.step = '60'; label.htmlFor = input.id;
        input.value = wallTime(report.publishAt || Math.ceil((Date.now() + 300000) / 60000) * 60000);
        const zone = element('p', 'small mb-0', tr('timeZone', [report.timeZone])); zone.id = 'branch-merge-time-zone';
        input.setAttribute('aria-describedby', `${zone.id} branch-merge-schedule-note`);
        const feedback = element('p', 'text-danger mb-0'); feedback.id = 'branch-merge-schedule-error'; feedback.setAttribute('role', 'alert');
        input.addEventListener('input', () => { input.setCustomValidity(''); input.removeAttribute('aria-invalid'); feedback.textContent = ''; });
        fields.append(label, input, zone);
        const note = element('p', 'mt-3', tr('scheduleNote', [items.length])); note.id = 'branch-merge-schedule-note';
        const actions = element('div', 'd-flex flex-wrap gap-2');
        actions.append(button(tr('cancel'), () => closePanel(form, 'branch-merge-schedule')));
        const submit = button(tr(report.state === 'SCHEDULED' ? 'reschedule' : 'schedule'), () => {}, true);
        submit.type = 'submit'; submit.id = 'branch-merge-schedule-confirm'; actions.append(submit);
        form.append(heading, fields, note, feedback, actions);
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const publishAt = scheduledInstant(input.value);
            if (publishAt === null || publishAt <= Date.now()) {
                const message = tr(publishAt === null ? 'invalidWallTime' : 'futureTime');
                feedback.textContent = message; input.setCustomValidity(message); input.setAttribute('aria-invalid', 'true'); input.reportValidity(); input.focus(); return;
            }
            await perform('/schedule', {expectedRevision: report.revision, preparationHash: report.preparationHash, publishAt});
            if (report.state === 'SCHEDULED') await compare();
        });
        overview.prepend(form); input.focus();
    }

    function confirmPublish(items, resume) {
        const old = overview.querySelector('#branch-merge-confirmation');
        if (old) { old.focus(); return; }
        const panel = element('section', 'alert alert-warning');
        panel.id = 'branch-merge-confirmation'; panel.tabIndex = -1;
        panel.append(element('h3', 'h5', tr(resume ? 'resume' : 'publish')));
        panel.append(element('p', '', tr('publishNote')));
        const counts = types.map(type => `${tr(`type.${type}`)}: ${items.filter(item => item.type === type && item.writeRequired && item.applyState !== 'DONE').length}`).join(' · ');
        panel.append(element('p', '', counts));
        panel.append(button(tr('cancel'), () => panel.remove()), document.createTextNode(' '));
        const confirm = button(tr(resume ? 'resumeConfirm' : 'publishConfirm'), () => perform(resume ? '/resume' : '/merge', {
            expectedRevision: report.revision, preparationHash: report.preparationHash
        }), true);
        confirm.id = 'branch-merge-confirm'; panel.append(confirm);
        overview.prepend(panel); panel.focus();
    }

    function detail(item) {
        const section = element('section', 'pg-branch-merge__detail mt-4');
        section.tabIndex = -1;
        section.setAttribute('aria-label', tr('comparison'));
        section.append(element('h3', 'h5', `${item.label || item.objectKey} · ${tr(`type.${item.type}`)} ${item.objectKey}`));
        section.append(element('p', 'small', tr('wholeItem')));
        if (item.error) section.append(element('p', 'alert alert-danger', item.error));
        if (item.applyPhase === 'TRASH_WAIT') section.append(element('p', 'alert alert-warning', tr('trashWaitNote')));
        else if (item.applyState === 'ERROR' && writtenPhases.has(item.applyPhase)) section.append(element('p', 'alert alert-warning', tr('contentWrittenNote')));
        if (item.applyPhase === 'FILE_STARTED') section.append(element('p', 'alert alert-warning', tr('fileStateNote')));
        if (item.type === 'DOCUMENT' && item.choice === 'LIVE' && item.writeRequired) section.append(element('p', 'alert alert-info', tr('liveMoveNote')));
        if (item.base?.fields?.virtual_path !== item.work?.fields?.virtual_path && item.type === 'DOCUMENT') section.append(element('p', 'alert alert-warning', tr('redirectNote')));
        const allFields = [...new Set(['title', 'group_name', 'data', ...['base', 'live', 'work'].flatMap(name => Object.keys(item[name]?.fields || {}))])]
            .filter(key => ['base', 'live', 'work'].some(name => Object.hasOwn(item[name]?.fields || {}, key)));
        const differing = key => !equal(item.base?.fields?.[key], item.live?.fields?.[key]) || !equal(item.base?.fields?.[key], item.work?.fields?.[key]);
        const showAll = expanded.has(selection);
        const show = element('label', 'form-check-label mb-3');
        const check = element('input', 'form-check-input me-2'); check.id = 'branch-merge-all-fields'; check.type = 'checkbox'; check.checked = showAll;
        check.onchange = () => { if (check.checked) expanded.add(selection); else expanded.delete(selection); render(); dialog.querySelector('#branch-merge-all-fields').focus(); };
        show.append(check, document.createTextNode(tr('allFields'))); section.append(show);
        const columns = element('div', 'pg-branch-merge__versions');
        for (const version of ['base', 'live', 'work']) {
            const snapshot = item[version] || {state: 'ABSENT', fields: {}};
            const panel = element('section', 'pg-branch-merge__version');
            panel.dataset.branchVersion = version.toUpperCase();
            panel.append(element('h4', 'h6', tr(version)), element('p', 'small', tr(`existence.${snapshot.state}`)));
            if (item.type === 'FILE' && snapshot.state !== 'ABSENT' && snapshot.fields.entry_kind !== 'DIRECTORY') {
                const url = `/admin/rest/branches/${id}/file-version?path=${encodeURIComponent(item.objectKey)}&version=${version.toUpperCase()}`;
                if (/\.(png|jpe?g|gif|webp|avif)$/i.test(item.objectKey)) {
                    const image = element('img', 'pg-branch-merge__image'); image.src = url; image.alt = `${tr(version)} · ${item.objectKey}`; panel.append(image);
                }
                panel.append(link(tr('openFile'), url));
            }
            const fields = element('dl', 'mb-0');
            for (const field of allFields.filter(key => showAll || differing(key) || ['title', 'group_name', 'data'].includes(key))) {
                const changed = version !== 'base' && !equal(item.base?.fields?.[field], snapshot.fields?.[field]);
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
            for (const choice of ['LIVE', 'WORK']) {
                const label = element('label', 'form-check-label d-block mb-2');
                const radio = element('input', 'form-check-input me-2'); radio.type = 'radio'; radio.name = 'branch-merge-choice'; radio.value = choice; radio.checked = item.choice === choice;
                const deleted = item[choice.toLowerCase()]?.state !== 'ACTIVE';
                const key = choice === 'LIVE' ? 'useLive' : deleted ? 'useDelete' : item.live?.state !== 'ACTIVE' ? 'useRestore' : 'useWork';
                label.append(radio, document.createTextNode(tr(key))); choices.append(label);
            }
            const save = button(tr('saveChoice'), async () => {
                const choice = choices.querySelector('input:checked')?.value;
                if (!choice) return;
                await perform('/resolve', {expectedRevision: report.revision, type: item.type, objectKey: item.objectKey, choice, liveHash: item.liveHash, workHash: item.workHash}, true);
            }, true);
            save.disabled = !item.choice;
            choices.addEventListener('change', () => { save.disabled = false; });
            save.id = 'branch-merge-save-choice'; choices.append(save); section.append(choices);
        }
        if (report.state === 'DRAFT' && item.editable) {
            const actions = element('div', 'd-flex flex-wrap gap-2 mt-3');
            actions.append(button(tr(item.type === 'FILE' ? 'editWeb' : 'editItem'), () => edit(item)));
            if (['DOCUMENT', 'GROUP'].includes(item.type) && Number(screen.branchId) === Number(id)) {
                actions.append(button(WJ.translate('branches.revert.js'), async () => {
                    try {
                        const response = await fetch('/admin/rest/branch-content/revert', {method: 'POST', headers: {'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken}, body: JSON.stringify({type: item.type, id: Number(item.objectKey), hash: item.workHash})});
                        if (!response.ok) throw new Error(await response.text());
                        selection = null;
                        await perform(''); await compare();
                    } catch (error) { failure(error); }
                }));
            }
            section.append(actions, element('p', 'small mt-2', tr(item.type === 'GROUP' ? 'editGroupNote' : item.type === 'MEDIA' ? 'editMediaNote' : item.type === 'FILE' ? 'editFileNote' : 'editNote')));
        }
        if (item.type === 'DOCUMENT' && item.live?.state === 'ACTIVE') section.append(link(tr('production'), `/showdoc.do?docid=${encodeURIComponent(item.objectKey)}`));
        if (['DRAFT', 'SCHEDULED', 'NEEDS_ATTENTION'].includes(report.state) && item.type === 'DOCUMENT') section.append(button(tr('preview'), () => preview(item.objectKey, section)));
        return section;
    }

    try {
        status.textContent = tr('loading');
        report = await api(`/${id}/progress`);
        if (!dialog.open) return;
        render();
        if (['DRAFT', 'SCHEDULED', 'MERGE_FAILED'].includes(report.state)) await compare();
    } catch (error) { failure(error); }
}
