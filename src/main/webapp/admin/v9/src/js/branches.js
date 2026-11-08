import $ from 'jquery';

// The server embeds the context of this screen; never refresh it silently from another tab.
const screen = window.branchScreen;
if (screen?.enabled) {
    const nativeFetch = window.fetch.bind(window);
    window.fetch = (input, init) => {
        const request = new Request(input, init);
        const url = new URL(request.url);
        if (url.origin === location.origin && url.pathname.includes('/admin/')) {
            const headers = new Headers(request.headers);
            if (!headers.has('X-WebJET-Branch')) headers.set('X-WebJET-Branch', String(screen.branchId));
            if (!headers.has('X-WebJET-Branch-Domain')) headers.set('X-WebJET-Branch-Domain', screen.domainName);
            if (!headers.has('X-CSRF-Token')) headers.set('X-CSRF-Token', window.csrfToken);
            return nativeFetch(new Request(request, { headers }));
        }
        return nativeFetch(request);
    };
    $.ajaxPrefilter((options, original, xhr) => {
        const url = new URL(options.url, window.location.href);
        if (url.origin === location.origin && url.pathname.includes('/admin/')) {
            xhr.setRequestHeader('X-WebJET-Branch', String(screen.branchId));
            xhr.setRequestHeader('X-WebJET-Branch-Domain', screen.domainName);
        }
    });
}

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
        label.textContent = active ? `Pracovná verzia: ${active.name} · iba čítanie` : 'Produkcia';
        label.title = label.textContent;
        if (active) {
            try {
                const link = await api(`/${active.id}/preview-link`);
                const preview = document.getElementById('branch-preview-link');
                if (preview) { preview.href = link.url; preview.classList.remove('d-none'); }
            } catch (error) { label.title += ' · Doména nemá dostupnú predvolenú stránku náhľadu.'; }
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
            modal.innerHTML = '<h2 id="branch-dialog-title">Pracovné verzie</h2><p class="branch-domain"></p><div class="branch-list"></div><form><label class="form-label">Názov <input name="name" class="form-control" required maxlength="255"></label><br><label class="form-label">Poznámka <textarea name="description" class="form-control"></textarea></label><p>Nezmenený obsah sa preberá z aktuálnej produkcie. Pracovná verzia je zatiaľ iba na čítanie.</p><button class="btn btn-primary" type="submit">Vytvoriť</button> <button class="btn btn-outline-secondary branch-close" type="button">Zavrieť</button><p class="branch-result" role="status"></p></form>';
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
