import $ from 'jquery';
import {openBranchMerge} from './branch-merge';
import {icon, node} from './dashboard/widget-utils';

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
            await api('/context', {branchId: Number(id), expectedDomainId: screen.domainId});
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
    const manage = node('a', 'md-branch-picker__option', tr('manage'));
    manage.href = '/admin/v9/webpages/branches/';
    manage.dataset.branchAction = 'manage';
    manage.prepend(icon('ti-settings'));
    footer.append(manage);
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
            const buttons = Array.from(picker.querySelectorAll('button, a[href]'));
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
