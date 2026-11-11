import $ from 'jquery';
import {openBranchMerge} from './branch-merge';
import {icon, node} from './dashboard/widget-utils';
import {SHORTCUT_COLORS} from './shortcut-colors';

const tr = (key, ...args) => WJ.translate(`branches.${key}.js`, ...args);
const stateName = state => tr(`merge.state.${state}`);
const previewStates = ['DRAFT', 'SCHEDULED', 'NEEDS_ATTENTION'];

function button(label, className, onClick) {
    const result = node('button', className, label);
    result.type = 'button';
    result.addEventListener('click', onClick);
    return result;
}

function dot(color) {
    const result = node('span', 'md-branch-picker__dot');
    result.dataset.branchColor = color;
    result.setAttribute('aria-hidden', 'true');
    return result;
}

function suggestColor(branches) {
    const colors = ['cyan', 'lavender', 'blue', 'amber', 'mint', 'peach', 'red', 'gray'];
    const usage = new Map(colors.map(color => [color, 0]));
    for (const {color} of branches) usage.set(color, (usage.get(color) || 0) + 1);
    return colors.reduce((best, color) => usage.get(color) < usage.get(best) ? color : best);
}

function dialog(titleText, domain, trigger) {
    const modal = node('dialog', 'md-branch-dialog');
    modal.setAttribute('aria-labelledby', 'branch-dialog-title');
    const header = node('header', 'd-flex justify-content-between align-items-start gap-3 mb-3');
    const titles = node('div');
    const title = node('h2', 'h4 mb-1', titleText);
    title.id = 'branch-dialog-title';
    titles.append(title, node('span', 'branch-domain text-muted small', tr('domain', domain)));
    const close = button('', 'btn-close branch-close', () => modal.close());
    close.setAttribute('aria-label', WJ.translate('button.close'));
    header.append(titles, close);
    modal.append(header);
    modal.addEventListener('close', () => {
        modal.remove();
        if (!document.querySelector('dialog[open]')) trigger.focus();
    }, {once: true});
    document.body.append(modal);
    return modal;
}

function createDialog(branches, screen, trigger, api, confirmSwitch, activate) {
    const modal = dialog(tr('new'), screen.domainName, trigger);
    const form = node('form');
    const fields = node('fieldset');
    const nameLabel = node('label', 'form-label d-block mb-3', tr('name'));
    const name = node('input', 'form-control');
    Object.assign(name, {name: 'name', required: true, maxLength: 255, autocomplete: 'off', autofocus: true});
    nameLabel.append(name);
    const noteLabel = node('label', 'form-label d-block mb-3', tr('optionalNote'));
    const note = node('textarea', 'form-control');
    note.name = 'description';
    note.rows = 2;
    noteLabel.append(note);
    const colors = node('fieldset', 'mb-3');
    colors.append(node('legend', 'form-label', WJ.translate('admin.dashboard.shortcutColor.js')));
    const swatches = node('div', 'md-branch-colors');
    const sample = node('div', 'md-branch-sample mb-3');
    const sampleText = node('span', 'd-flex flex-column');
    const sampleName = node('strong', 'text-truncate', tr('new'));
    sampleText.append(node('small', '', tr('sample')), sampleName);
    sample.append(icon('ti-git-branch'), sampleText);
    sample.dataset.branchColor = suggestColor(branches);
    for (const color of SHORTCUT_COLORS) {
        const label = node('label', 'md-branch-swatch');
        label.dataset.branchColor = color;
        label.title = WJ.translate(`admin.dashboard.shortcutColor.${color}.js`);
        const radio = node('input', 'visually-hidden');
        Object.assign(radio, {type: 'radio', name: 'color', value: color, checked: color === sample.dataset.branchColor});
        radio.setAttribute('aria-label', label.title);
        radio.addEventListener('change', () => { sample.dataset.branchColor = color; });
        const mark = node('span');
        mark.append(icon('ti-check'));
        label.append(radio, mark);
        swatches.append(label);
    }
    name.addEventListener('input', () => {
        name.setCustomValidity('');
        sampleName.textContent = name.value.trim() || tr('new');
    });
    colors.append(swatches);
    fields.append(nameLabel, noteLabel, colors, sample);
    const status = node('p', 'branch-result text-danger');
    status.setAttribute('role', 'alert');
    const footer = node('footer', 'd-flex justify-content-end gap-2');
    const cancel = button(WJ.translate('button.cancel'), 'btn btn-outline-secondary', () => modal.close());
    const submit = node('button', 'btn btn-primary', tr('createOpen'));
    submit.type = 'submit';
    footer.append(cancel, submit);
    form.append(fields, status, footer);
    modal.append(node('p', '', tr('createNote')), form);
    let created;
    modal.addEventListener('cancel', event => { if (submit.disabled) event.preventDefault(); });
    // A failed context switch can be retried without creating a second version.
    form.onsubmit = async event => {
        event.preventDefault();
        if (submit.disabled) return;
        if (!created && !name.value.trim()) {
            name.setCustomValidity(tr('nameRequired'));
            name.reportValidity();
            return;
        }
        if (!confirmSwitch()) return;
        submit.disabled = cancel.disabled = modal.querySelector('.branch-close').disabled = true;
        status.textContent = '';
        try {
            if (!created) {
                created = await api('', {name: name.value.trim(), description: note.value, color: form.elements.color.value, expectedDomainName: screen.domainName});
                branches.unshift(created);
                fields.disabled = true;
                submit.textContent = tr('merge.editWeb');
            }
            await activate(created.id);
        } catch (error) {
            status.textContent = created ? tr('openFailed', created.name, error.message) : error.message;
            submit.disabled = cancel.disabled = modal.querySelector('.branch-close').disabled = false;
        }
    };
    modal.showModal();
}

function manageDialog(branches, screen, trigger, compare, switchTo) {
    const modal = dialog(tr('versions'), screen.domainName, trigger);
    const list = node('div', 'branch-list');
    for (const branch of branches) {
        const row = node('p', 'd-flex gap-2 align-items-center flex-wrap');
        row.append(dot(branch.color), node('strong', '', branch.name), node('span', 'small text-muted', stateName(branch.state)),
            button(tr('merge.compare'), 'btn btn-sm btn-outline-secondary', () => { modal.close(); compare(branch.id, trigger); }));
        if (branch.state === 'DRAFT') row.append(button(tr('merge.editWeb'), 'btn btn-sm btn-outline-secondary', () => switchTo(branch.id)));
        list.append(row);
    }
    if (!branches.length) list.append(node('p', 'text-muted', tr('noVersions')));
    modal.append(list);
    modal.showModal();
}

/** Opens the domain's version picker from the main header icon or the active working version bar. */
export function initBranchSwitcher(screen, api) {
    const trigger = document.getElementById('branch-switcher');
    if (!trigger) return;
    let branches = [];
    const currentId = Number(screen.branchId);
    const preview = document.getElementById('branch-preview-link');
    const changes = document.getElementById('branch-changes');
    const compare = (id, invoker) => openBranchMerge(id, api, screen, invoker);
    const showError = error => WJ.notifyError(WJ.escapeHtml(error.message));
    const confirmSwitch = () => !$('div.modal.DTED.show:visible').length || window.confirm(tr('discardConfirm'));
    let switching = false;
    async function activate(id) {
        if (switching) return;
        switching = true;
        try {
            await api('/context', {branchId: Number(id), expectedDomainName: screen.domainName});
            $('div.modal.DTED.show:visible').removeClass('show');
            window.location.assign('/admin/v9/webpages/web-pages-list/');
        } catch (error) {
            switching = false;
            throw error;
        }
    }
    function switchTo(id) {
        if (confirmSwitch()) activate(id).catch(showError);
    }

    let previewState;
    async function updateBand(branch) {
        if (!currentId || Number(branch.id) !== currentId) return;
        document.getElementById('branch-active-label').textContent = branch.name;
        const state = document.getElementById('branch-state');
        state.hidden = branch.state === 'DRAFT';
        state.textContent = state.hidden ? '' : `${stateName(branch.state)} · ${tr('readonly')}`;
        trigger.title = branch.name;
        trigger.setAttribute('aria-label', `${branch.name} · ${tr('switch')}`);
        if (branch.state === previewState) return;
        previewState = branch.state;
        preview.classList.add('d-none');
        preview.removeAttribute('href');
        if (previewStates.includes(branch.state)) {
            const requestedState = branch.state;
            try {
                const link = await api(`/${currentId}/preview-link`);
                if (previewState === requestedState) {
                    preview.href = link.url;
                    preview.classList.remove('d-none');
                }
            } catch {
                if (previewState === requestedState) trigger.title = `${branch.name} · ${tr('previewUnavailable')}`;
            }
        }
    }
    if (currentId) {
        updateBand({id: currentId, name: screen.name, state: screen.state});
        changes.classList.remove('d-none');
        changes.onclick = () => compare(currentId, changes);
    }
    const compareId = new URLSearchParams(location.hash.slice(1)).get('branchCompare') || new URLSearchParams(location.search).get('branchCompare');
    if (/^[1-9]\d*$/.test(compareId)) compare(Number(compareId), trigger);

    const picker = node('div', 'md-branch-picker');
    picker.id = 'branch-picker';
    picker.hidden = true;
    picker.setAttribute('role', 'dialog');
    picker.setAttribute('aria-label', tr('versions'));
    const header = node('div', 'md-branch-picker__header');
    header.append(node('strong', '', tr('versions')), node('small', 'd-block text-muted', screen.domainName));
    const search = node('input', 'form-control form-control-sm mt-2');
    search.type = 'search';
    search.placeholder = tr('search');
    search.setAttribute('aria-label', tr('search'));
    header.append(search);
    const options = node('div', 'md-branch-picker__options');
    const footer = node('div', 'md-branch-picker__footer');
    picker.append(header, options, footer);
    document.body.append(picker);

    function closePicker(restoreFocus = false) {
        picker.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        if (restoreFocus) trigger.focus();
    }
    function position() {
        if (picker.hidden) return;
        const anchor = trigger.getBoundingClientRect();
        const edge = 8;
        picker.style.left = Math.max(edge, Math.min(anchor.left, document.documentElement.clientWidth - picker.offsetWidth - edge)) + 'px';
        picker.style.top = anchor.bottom + 4 + 'px';
        picker.style.maxHeight = Math.min(480, Math.max(0, window.innerHeight - anchor.bottom - edge - 4)) + 'px';
    }
    function renderOptions() {
        options.replaceChildren();
        const match = search.value.trim().toLocaleLowerCase();
        let count = 0;
        function option(id, name, subtext, color) {
            if (match && !`${name} ${subtext}`.toLocaleLowerCase().includes(match)) return;
            count++;
            const select = button('', 'md-branch-picker__option', () => {
                closePicker(true);
                if (id !== Number(screen.selectedBranchId ?? currentId) || id !== currentId) switchTo(id);
            });
            select.dataset.branchId = id;
            const caption = node('span', 'md-branch-picker__text');
            caption.append(node('strong', '', name), node('small', '', subtext));
            select.append(id === 0 ? icon('ti-world') : dot(color), caption);
            if (id === currentId) {
                select.setAttribute('aria-current', 'true');
                select.append(icon('ti-check'));
            }
            options.append(select);
        }
        option(0, tr('main'), tr('mainHint'), 'default');
        for (const branch of branches) option(branch.id, branch.name, `${stateName(branch.state)} · #${branch.id}`, branch.color);
        if (!count) options.append(node('p', 'small text-muted m-3', tr('noMatches')));
    }
    search.addEventListener('input', renderOptions);
    function action(key, label, glyph, handler) {
        const item = button(label, 'md-branch-picker__option', async () => {
            closePicker(true);
            try { await handler(); } catch (error) { showError(error); }
        });
        item.prepend(icon(glyph));
        item.dataset.branchAction = key;
        footer.append(item);
    }
    action('create', tr('new'), 'ti-plus', () => createDialog(branches, screen, trigger, api, confirmSwitch, activate));
    action('manage', tr('manage'), 'ti-list-details', async () => manageDialog(await api('?includeClosed=true'), screen, trigger, compare, switchTo));
    document.addEventListener('pointerdown', event => {
        if (!picker.hidden && !picker.contains(event.target) && !trigger.contains(event.target)) closePicker();
    });
    document.addEventListener('focusin', event => {
        if (!picker.hidden && !picker.contains(event.target) && event.target !== trigger) closePicker();
    });
    picker.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            event.preventDefault();
            closePicker(true);
        } else if (['ArrowDown', 'ArrowUp'].includes(event.key)) {
            event.preventDefault();
            const buttons = Array.from(picker.querySelectorAll('button'));
            const index = buttons.indexOf(document.activeElement);
            buttons[(index + (event.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length].focus();
        }
    });
    window.addEventListener('resize', position);
    window.addEventListener('scroll', position, true);

    window.addEventListener('WJ.Branch.updated', ({detail}) => {
        const branch = branches.find(item => item.id === detail.id);
        if (branch) Object.assign(branch, {name: detail.name, state: detail.state});
        updateBand(detail);
        if (!picker.hidden) renderOptions();
    });

    trigger.disabled = false;
    trigger.onclick = async () => {
        if (!picker.hidden) { closePicker(true); return; }
        trigger.disabled = true;
        try { branches = await api(''); }
        catch (error) { showError(error); return; }
        finally { trigger.disabled = false; }
        search.value = '';
        search.hidden = branches.length < 5;
        renderOptions();
        picker.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        position();
        (search.hidden ? options.querySelector('[aria-current]') || options.firstElementChild : search).focus();
    };
}
