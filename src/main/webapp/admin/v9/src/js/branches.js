import $ from 'jquery';
import {initBranchSwitcher} from './branch-switcher';

// The server embeds the context of this screen; never refresh it silently from another tab.
const screen = window.branchScreen;
const selectedBranchId = screen?.selectedBranchId ?? screen?.branchId;
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

// Ordinary menu clicks keep the current screen behind the same dialog that also handles direct URLs.
if (selectedBranchId) {
    let loadingDialog = false;
    document.addEventListener('click', async event => {
        const link = event.target.closest?.('a[data-production-dialog]');
        if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey ||
            event.shiftKey || event.altKey || link.target && link.target !== '_self') return;
        const url = new URL(link.href);
        if (url.origin !== location.origin) return;
        event.preventDefault();
        event.stopPropagation();
        if (loadingDialog) return;
        loadingDialog = true;
        try {
            const response = await fetch(url, {headers: {
                'X-WebJET-Production-Navigation': 'dialog',
                'X-WebJET-Branch': String(selectedBranchId),
                'X-WebJET-Branch-Domain': screen.domainName
            }});
            if (!response.ok) throw new Error(WJ.translate('branches.stale.js'));
            const page = new DOMParser().parseFromString(await response.text(), 'text/html');
            const prompt = page.querySelector('#branch-section-dialog');
            if (!prompt) { location.assign(url.href); return; }
            const dialog = document.importNode(prompt, true);
            dialog.removeAttribute('open');
            dialog.style.width = 'min(30rem, calc(100vw - 2rem))';
            const proceed = dialog.querySelector('[data-continue]');
            proceed.onclick = () => location.assign(new URL(proceed.dataset.continue + url.hash, url).href);
            const back = dialog.querySelector('.btn-outline-secondary');
            if (screen.branchId) back.onclick = event => { event.preventDefault(); dialog.close(); };
            dialog.addEventListener('close', () => dialog.remove(), {once: true});
            document.body.append(dialog);
            dialog.showModal();
        } catch (error) {
            WJ.notifyError(error.message);
        } finally {
            loadingDialog = false;
        }
    }, true);
}

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

$(() => {
    if (screen?.allowed) initBranchSwitcher(screen, api);
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
