import $ from 'jquery';

// The server embeds the context of this screen; never refresh it silently from another tab.
const screen = window.branchScreen;
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
    if (!response.ok) throw new Error(`${response.status}: ${await response.text()}`);
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
            try {
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
            changesButton.onclick = async () => {
                try {
                    const response = await fetch('/admin/rest/branch-content');
                    if (!response.ok) throw new Error(await response.text());
                    const changes = await response.json();
                    const modal = document.createElement('dialog'); modal.className = 'p-4 border rounded';
                    const title = document.createElement('h2'); title.textContent = WJ.translate('branches.changes.js'); modal.append(title);
                    const note = document.createElement('p'); note.textContent = WJ.translate('branches.revert.note.js'); modal.append(note);
                    for (const change of changes) {
                        const row = document.createElement('p'); row.textContent = `${change.type} #${change.id} · ${change.title} · ${change.operation} `;
                        const button = document.createElement('button'); button.type = 'button'; button.className = 'btn btn-sm btn-outline-secondary'; button.textContent = WJ.translate('branches.revert.js');
                        button.onclick = async () => {
                            try {
                                const result = await fetch('/admin/rest/branch-content/revert', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({type:change.type,id:change.id,hash:change.hash}) });
                                if (!result.ok) throw new Error(await result.text());
                                location.reload();
                            } catch (error) { showError(error); }
                        };
                        row.append(button); modal.append(row);
                    }
                    const close = document.createElement('button'); close.type = 'button'; close.className = 'btn btn-primary'; close.textContent = WJ.translate('button.close'); close.onclick = () => modal.close(); modal.append(close);
                    modal.onclose = () => modal.remove(); document.body.append(modal); modal.showModal();
                } catch (error) { showError(error); }
            };
        }

        async function activate(id) {
            const editorOpen = $('div.modal.DTED.show:visible').length > 0;
            if (editorOpen && !window.confirm('Zahodiť neuložené zmeny a prepnúť pracovnú verziu?')) return;
            // Close the explicitly discarded editor before changing the session selection.
            if (editorOpen) $('div.modal.DTED.show:visible').removeClass('show');
            await api('/context', { branchId: Number(id), expectedDomainName: screen.domainName });
            window.location.href = '/admin/v9/webpages/web-pages-list/';
        }

        function dialog(create) {
            const modal = document.createElement('dialog');
            modal.className = 'p-4 border rounded';
            modal.setAttribute('aria-labelledby', 'branch-dialog-title');
            modal.innerHTML = '<h2 id="branch-dialog-title">Pracovné verzie</h2><p class="branch-domain"></p><div class="branch-list"></div><form><label class="form-label">Názov <input name="name" class="form-control" required maxlength="255"></label><br><label class="form-label">Poznámka <textarea name="description" class="form-control"></textarea></label><p>Nezmenený obsah sa preberá z aktuálnej produkcie. Stránky a priečinky sa ukladajú do pracovnej verzie.</p><button class="btn btn-primary" type="submit">Vytvoriť</button> <button class="btn btn-outline-secondary branch-close" type="button">Zavrieť</button><p class="branch-result" role="status"></p></form>';
            modal.querySelector('.branch-domain').textContent = `Doména: ${screen.domainName}`;
            const list = modal.querySelector('.branch-list');
            if (!create) branches.forEach(branch => {
                const button = document.createElement('button');
                button.type = 'button'; button.className = 'btn btn-outline-secondary d-block mb-2';
                button.textContent = `${branch.name} · ${branch.state} — Upravovať web`;
                button.onclick = () => activate(branch.id).catch(showError); list.append(button);
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
            if (value === 'create' || value === 'manage') dialog(value === 'create');
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
        ['history','publishing', ...(group ? [] : ['media','attributes'])].forEach(tab => {
            $('#'+table.DATA.id+'_modal').find(`[id$="-${tab}-tab"]`).closest('li').hide();
        });
        const container = $('#'+table.DATA.id+'_modal');
        if (!container.find('.branch-editor-note').length) $('<p class="branch-editor-note alert alert-info m-3">').text(WJ.translate('branches.editor.note.js')).prependTo(container.find('.DTE_Body'));
    });
};
