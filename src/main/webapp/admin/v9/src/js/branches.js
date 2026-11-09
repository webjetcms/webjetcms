import $ from 'jquery';
import {openBranchMerge} from './branch-merge';

// The server embeds the context of this screen; never refresh it silently from another tab.
const screen = window.branchScreen;
window.branchAssetUrl = value => {
    if (!screen?.enabled || !screen.branchId || typeof value !== 'string') return value;
    return /^\/(?:images|files|thumb\/(?:images|files))\//.test(value) ? `/admin/webpages/branch-preview/${screen.branchId}${value}` : value;
};
window.branchLogicalAssetUrl = value => {
    const prefix = `/admin/webpages/branch-preview/${screen?.branchId}`;
    return screen?.branchId && typeof value === 'string' && value.startsWith(prefix+'/') ? value.substring(prefix.length) : value;
};
window.branchFileDialogUrl = path => screen?.enabled ? WJ.urlAddParam(WJ.urlAddParam(path, 'branchId', screen.branchId), 'branchDomain', screen.domainName) : path;
window.initBranchFileUpload = finder => {
    if (!screen?.enabled || !screen.branchId) return;
    const picker = new URLSearchParams(window.location.search).get('volumes');
    const uploader = window.AdminUpload({element: '#dt-upload', uploadType: picker === 'images' ? 'image' : 'file', writeDirectlyToDestination: true});
    window.branchAdminUpload = uploader;
    const button = document.getElementById('branch-upload-button');
    if (!document.getElementById('branch-file-warning')) {
        const warning = document.createElement('p');
        warning.id = 'branch-file-warning';
        warning.className = 'alert alert-warning small m-2 p-2';
        warning.textContent = WJ.translate('branches.files.shared.js');
        button.before(warning);
    }
    button.classList.remove('d-none');
    const updateDestination = (folder = finder.cwd()) => {
        uploader.setDestinationFolder(folder.virtualPath || '');
        const types = finder.option('uploadMime')?.allow;
        uploader.options.acceptedFiles = Array.isArray(types) && !types.includes('all') ? types.map(type => type.endsWith('/') ? type+'*' : type).join(',') : null;
        const disabled = !folder.write || !/^\/(images|files)(\/|$)/.test(folder.virtualPath || '');
        uploader.setFileSelectionDisabled(disabled);
        button.disabled = disabled;
    };
    finder.bind('open', event => updateDestination(event.data.cwd));
    finder.bind('lazydone', () => updateDestination());
    button.addEventListener('click', () => { updateDestination(); uploader.hiddenFileInput.click(); });
    uploader.on('addedfile', file => {
        file.wjAdminUploadRequestParams = {destinationFolder: finder.cwd().virtualPath};
    });
    window.addEventListener('WJ.AdminUpload.success', () => finder.exec('reload'));
};
window.bindBranchContext = (target, context) => {
    if (!context?.enabled) return;
    target.branchScreen = Object.freeze({ ...context });
    const nativeFetch = target.fetch.bind(target);
    target.fetch = (input, init) => {
        const request = new Request(typeof input === 'string' ? new URL(input, target.location.href) : input, init);
        const url = new URL(request.url);
        if (url.origin === target.location.origin && url.pathname.includes('/admin/')) {
            const headers = new Headers(request.headers);
            if (!headers.has('X-WebJET-Branch')) headers.set('X-WebJET-Branch', String(context.branchId));
            if (!headers.has('X-WebJET-Branch-Domain')) headers.set('X-WebJET-Branch-Domain', context.domainName);
            if (!headers.has('X-CSRF-Token')) headers.set('X-CSRF-Token', target.csrfToken || window.csrfToken);
            return nativeFetch(new Request(request, { headers }));
        }
        return nativeFetch(request);
    };
    const jquery = target === window ? $ : (target.jQuery || target.$);
    jquery.ajaxPrefilter((options, original, xhr) => {
        const url = new URL(options.url, target.location.href);
        if (url.origin === target.location.origin && url.pathname.includes('/admin/')) {
            xhr.setRequestHeader('X-WebJET-Branch', String(context.branchId));
            xhr.setRequestHeader('X-WebJET-Branch-Domain', context.domainName);
        }
    });
};
window.bindBranchContext(window, screen);

async function api(path, body) {
    const response = await fetch(`/admin/rest/branches${path}`, {
        method: body ? 'POST' : 'GET',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
        body: body ? JSON.stringify(body) : undefined,
    });
    if (!response.ok) {
        let message = await response.text();
        try { const error = JSON.parse(message); message = error.detail || error.message || message; } catch (ignored) { /* Keep a plain server response. */ }
        throw new Error(`${response.status}: ${message}`);
    }
    return response.json();
}

$(async () => {
    if (!screen?.allowed) return;
    const selector = document.getElementById('branch-selection');
    const label = document.getElementById('branch-active-label');
    if (!selector) return;
    const showError = error => window.WJ?.notifyError ? window.WJ.notifyError(error.message) : window.alert(error.message);
    try {
        const branches = await api('');
        branches.forEach(branch => selector.add(new Option(`${branch.name} · ${branch.state} (#${branch.id})`, branch.id)));
        selector.add(new Option('+ Nová pracovná verzia', 'create'));
        selector.add(new Option('Spravovať pracovné verzie', 'manage'));
        selector.value = String(screen.branchId);
        const active = branches.find(branch => branch.id === screen.branchId);
        label.textContent = active ? `Pracovná verzia: ${active.name}${active.state === 'DRAFT' ? '' : ' · iba čítanie'}` : 'Produkcia';
        label.title = label.textContent;
        if (active) {
            if (!document.getElementById('branch-file-warning')) {
                const warning = document.createElement('p');
                warning.id = 'branch-file-warning';
                warning.className = 'alert alert-warning m-3';
                warning.textContent = WJ.translate('branches.files.shared.js');
                document.querySelector('main, .ly-content, #main-content')?.prepend(warning);
            }
            if (active.state === 'DRAFT') try {
                const link = await api(`/${active.id}/preview-link`);
                const preview = document.getElementById('branch-preview-link');
                if (preview) { preview.href = link.url; preview.classList.remove('d-none'); }
            } catch (error) { label.title += ' · Doména nemá dostupnú predvolenú stránku náhľadu.'; }
        }

        if (active) {
            const changesButton = document.createElement('button');
            changesButton.type = 'button'; changesButton.className = 'btn btn-sm btn-outline-secondary';
            changesButton.textContent = WJ.translate('branches.changes.js');
            selector.parentElement.append(changesButton);
            changesButton.onclick = () => openBranchMerge(active.id, api, screen, changesButton);
        }

        const compareId = new URLSearchParams(location.hash.substring(1)).get('branchCompare') || new URLSearchParams(location.search).get('branchCompare');
        if (compareId && /^[1-9][0-9]*$/.test(compareId)) openBranchMerge(Number(compareId), api, screen, selector);

        async function activate(id) {
            const editorOpen = $('div.modal.DTED.show:visible').length > 0;
            if (editorOpen && !window.confirm('Zahodiť neuložené zmeny a prepnúť pracovnú verziu?')) return;
            // Close the explicitly discarded editor before changing the session selection.
            if (editorOpen) $('div.modal.DTED.show:visible').removeClass('show');
            await api('/context', { branchId: Number(id), expectedDomainName: screen.domainName });
            window.location.href = '/admin/v9/webpages/web-pages-list/';
        }

        async function dialog(create) {
            const listedBranches = create ? branches : await api('?includeClosed=true');
            const modal = document.createElement('dialog');
            modal.className = 'p-4 border rounded';
            modal.setAttribute('aria-labelledby', 'branch-dialog-title');
            modal.innerHTML = '<h2 id="branch-dialog-title">Pracovné verzie</h2><p class="branch-domain"></p><div class="branch-list"></div><form><label class="form-label">Názov <input name="name" class="form-control" required maxlength="255"></label><br><label class="form-label">Poznámka <textarea name="description" class="form-control"></textarea></label><p>Nezmenený obsah sa preberá z aktuálnej produkcie. Stránky a priečinky sa ukladajú do pracovnej verzie.</p><button class="btn btn-primary" type="submit">Vytvoriť</button> <button class="btn btn-outline-secondary branch-close" type="button">Zavrieť</button><p class="branch-result" role="status"></p></form>';
            modal.querySelector('.branch-domain').textContent = `Doména: ${screen.domainName}`;
            const list = modal.querySelector('.branch-list');
            if (!create) listedBranches.forEach(branch => {
                const row = document.createElement('p');
                row.className = 'd-flex flex-wrap gap-2 align-items-center';
                row.append(document.createTextNode(`${branch.name} · ${branch.state} `));
                const detail = document.createElement('button');
                detail.type = 'button'; detail.className = 'btn btn-outline-secondary';
                detail.textContent = WJ.translate('branches.merge.compare.js');
                detail.onclick = () => { modal.close(); openBranchMerge(branch.id, api, screen, selector); };
                row.append(detail);
                if (branch.state === 'DRAFT') {
                    const edit = document.createElement('button');
                    edit.type = 'button'; edit.className = 'btn btn-outline-secondary';
                    edit.textContent = WJ.translate('branches.merge.editWeb.js');
                    edit.onclick = () => activate(branch.id).catch(showError); row.append(edit);
                }
                list.append(row);
            });
            modal.querySelector('.branch-close').onclick = () => modal.close();
            modal.onclose = () => { modal.remove(); selector.focus(); };
            modal.querySelector('form').onsubmit = async event => {
                event.preventDefault();
                const form = event.target;
                try {
                    const branch = await api('', { name: form.elements.name.value, description: form.elements.description.value, expectedDomainName: screen.domainName });
                    const result = modal.querySelector('.branch-result');
                    result.textContent = `Vytvorená verzia „${branch.name}“. Výber zostáva nezmenený. `;
                    const activateButton = document.createElement('button'); activateButton.type = 'button'; activateButton.className = 'btn btn-primary'; activateButton.textContent = 'Upravovať web';
                    activateButton.onclick = () => activate(branch.id).catch(showError); result.append(activateButton);
                    try {
                        const link = await api(`/${branch.id}/preview-link`);
                        const preview = document.createElement('a'); preview.href = link.url; preview.target = '_blank'; preview.rel = 'noopener'; preview.className = 'btn btn-outline-secondary'; preview.textContent = 'Otvoriť náhľad'; result.append(preview);
                    } catch (error) { result.append(document.createTextNode(' Náhľad nie je dostupný: doména nemá dostupnú predvolenú stránku.')); }
                    branches.unshift(branch); selector.add(new Option(`${branch.name} · ${branch.state} (#${branch.id})`, branch.id), 1);
                    form.querySelector('[type=submit]').disabled = true;
                } catch (error) { modal.querySelector('.branch-result').textContent = error.message; }
            };
            document.body.append(modal); modal.showModal();
        }
        selector.onchange = async () => {
            const value = selector.value; selector.value = String(screen.branchId);
            if (value === 'create' || value === 'manage') dialog(value === 'create').catch(showError);
            else try { await activate(value); } catch (error) { showError(error); }
        };
        selector.disabled = false;
    } catch (error) { showError(error); }
});

/** Limits the existing editor to the supported P04 fields. The server enforces the same boundary. */
window.applyBranchEditor = (table, group = false) => {
    if (!screen?.enabled) return;
    table.EDITOR.on('submitError.branch', (event, xhr) => {
        if (xhr.status === 409) table.EDITOR.error(WJ.translate('branches.stale.js'));
    });
    if (!screen.branchId) return;
    for (const name of ['duplicate', 'import', 'export', 'celledit', 'editPages', 'addPerexGroup', 'removePerexGroup', 'addUserGroup', 'removeUserGroup']) table.hideButton(name);
    table.EDITOR.on('preSubmit.branch', (event, data, action) => {
        if (action === 'remove') return;
        const reason = table.EDITOR.currentJson.editorFields?.branchReadOnlyReason;
        if (reason) { table.EDITOR.error(reason); return false; }
        // Disabled widgets omit values. Submit the version displayed in this form, never a fresh LIVE read.
        const original = table.EDITOR.currentJson;
        const readonly = group ? ['domainName', 'syncId', 'syncStatus', 'installName']
            : ['publishStartDate', 'publishEndDate', 'syncId', 'syncStatus', 'publishAfterStart', 'disableAfterEnd'];
        Object.values(data.data).forEach(record => {
            readonly.forEach(name => { if (Object.hasOwn(original, name)) record[name] = original[name]; });
            record.editorFields ||= {};
            record.editorFields.branchHash = original.editorFields.branchHash;
            if (!group) {
                record.editorFields.publishAfterStart = original.editorFields.publishAfterStart;
                record.editorFields.disableAfterEnd = original.editorFields.disableAfterEnd;
            }
        });
    });
    let readonlyFields = [];
    table.EDITOR.on('close.branch', () => {
        readonlyFields.forEach(name => table.EDITOR.field(name).enable());
        readonlyFields = [];
    });
    table.EDITOR.on('open', () => {
        const editor = table.EDITOR;
        const reason = editor.currentJson?.editorFields?.branchReadOnlyReason;
        const modal = $('#'+table.DATA.id+'_modal');
        modal.find('.branch-readonly-reason').remove();
        if (reason) {
            modal.find('.DTE_Footer button.btn-primary').prop('disabled', true);
            readonlyFields = editor.fields().filter(name => editor.field(name).enabled());
            readonlyFields.forEach(name => editor.field(name).disable());
            $('<p class="branch-readonly-reason alert alert-warning m-3">').text(reason).prependTo(modal.find('.DTE_Body'));
        }
        const names = group ? ['internal','domainName','syncId','syncStatus','editorFields.publishPlan','editorFields.publishDate','editorFields.forceDomainNameChange','editorFields.forcePriorityRecalculation','editorFields.forceTemplateToSubgroupsAndPages','editorFields.forceNewPageDocIdTemplateSubFolders','editorFields.passwordProtectedSubFolders','editorFields.forceMenuTypeSubfolders','editorFields.forceLoggedMenuTypeSubfolders','editorFields.forceNavbarSubfolders','editorFields.forceSitemapSubfolders','editorFields.forceLoggedNavbarSubfolders','editorFields.forceLoggedSitemapSubfolders','editorFields.forceLngToSubFolders']
            : ['publishStartDate','publishEndDate','syncId','syncStatus','editorFields.publishAfterStart','editorFields.disableAfterEnd','editorFields.requestPublish','editorFields.groupCopyDetails','editorFields.redactorNote','editorFields.attrs'];
        names.forEach(name => { if (editor.fields().includes(name)) editor.field(name).disable(); });
        if (!group) ['editorFields.groupCopyDetails', 'editorFields.redactorNote', 'editorFields.attrs'].forEach(name => { if (editor.fields().includes(name)) editor.field(name).hide(); });
        ['history','publishing', ...(group ? [] : ['attributes'])].forEach(tab => {
            $('#'+table.DATA.id+'_modal').find(`[id$="-${tab}-tab"]`).closest('li').hide();
        });
        const container = $('#'+table.DATA.id+'_modal');
        if (!container.find('.branch-editor-note').length) $('<p class="branch-editor-note alert alert-info m-3">').text(WJ.translate('branches.editor.note.js')).prependTo(container.find('.DTE_Body'));
    });
};
