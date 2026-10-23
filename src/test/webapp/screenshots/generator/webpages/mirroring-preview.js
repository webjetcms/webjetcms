Feature('webpages.mirroring-preview').tag('@singlethread');

const pageUrl = '/admin/v9/webpages/web-pages-list/';
const domain = 'mirroring.tau27.iway.sk';
const settingsFields = ['showid', 'showorder', 'showpages', 'showfolders-dt', 'showbadmirrorings'];
let originalSettings;

Before(({ login }) => {
    login('admin');
});

function openFolder(I, DT, id) {
    I.amOnPage(pageUrl + '?groupid=' + id);
    I.waitForElement(`#SomStromcek li[id="${id}"] > a.jstree-clicked`, 30);
    DT.waitForLoader();
}

function openSettings(I) {
    I.click('button.buttons-jstree-settings');
    I.waitForVisible('#jstreeSettingsModal', 10);
}

function saveSettings(I) {
    I.click('#jstree-settings-submit');
    I.waitForInvisible('#jstreeSettingsModal', 10);
    I.jstreeWaitForLoader();
}

Scenario('Prepare the existing mirroring screenshot fixtures', async ({ I, DT, Document }) => {
    I.amOnPage(pageUrl);
    if ((await I.grabTextFrom('div.js-domain-toggler div.bootstrap-select button')).trim() !== domain) {
        Document.switchDomain(domain);
    }
    DT.waitForLoader();
    openSettings(I);
    originalSettings = { width: await I.grabValueFrom('#jstree-settings-treeWidth'), checked: {} };
    for (const field of settingsFields) {
        const selector = '#jstree-settings-' + field;
        originalSettings.checked[field] = await I.grabCheckedElementStatus(selector);
    }
    for (const field of settingsFields) {
        const selector = '#jstree-settings-' + field;
        if (field === 'showbadmirrorings') I.checkOption(selector);
        else I.uncheckOption(selector);
    }
    I.selectOption('#jstree-settings-treeWidth', '6:6');
    saveSettings(I);
});

Scenario('Capture language navigation and the return button', ({ I, DT, Document }) => {
    // These existing language roots are also used by the mirroring examples.
    openFolder(I, DT, 9811);
    I.jstreeFilter('Slovensky');
    I.click('#SomStromcek li[id="9811"] > .jstree-mirrors a[data-group-id="9812"]');
    I.waitForElement('#SomStromcek li[id="9812"] > a.jstree-clicked', 30);
    DT.waitForLoader();
    I.seeInField('#tree-folder-search-input', '');
    I.seeElement('.buttons-mirror-back');
    I.seeElement('#SomStromcek li[id="9811"].jstree-mirror-peer');
    Document.screenshotElement('.tree-col', '/redactor/webpages/mirroring/tree-navigation.png', 1280, 440);
    I.click('.buttons-mirror-back');
    I.waitForElement('#SomStromcek li[id="9811"] > a.jstree-clicked', 30);
});

Scenario('Capture tree settings and diagnostic icons', ({ I, DT, Document }) => {
    // Reuse the deliberately invalid mappings from screenshots/generator/webpages/mirroring.js.
    openFolder(I, DT, 99802);
    I.waitForElement('#SomStromcek li[id="99802"] > a .ti-exclamation-circle.text-danger', 30);
    I.seeElement('#SomStromcek li[id="99888"] > a .ti-alert-triangle.text-warning');
    openSettings(I);
    Document.screenshotElement('#jstreeSettingsModal .modal-content', '/redactor/webpages/jstree-settings.png', 1280, 760);
    I.click('#jstreeSettingsModal .btn-close-editor');
    I.waitForInvisible('#jstreeSettingsModal', 10);

    Document.screenshotElement('#SomStromcek li[id="99799"]', '/redactor/webpages/mirroring/tree-problems.png', 1280, 680);
});

Scenario('Capture page and folder delete previews without deleting content', ({ I, DT, DTE, Document }) => {
    // This existing recovery fixture has active mirroring between its SK and EN roots.
    openFolder(I, DT, 113666);
    DT.filterEquals('title', 'sk_doc_recover');
    I.see('sk_doc_recover', '#datatableInit tbody');
    I.click('#datatableInit_wrapper button.buttons-select-all');
    I.click(DT.btn.delete_button);
    DTE.waitForEditor();
    I.waitForText('Zmazané budú aj nasledujúce zrkadlené webové stránky:', 30, '#datatableInit_modal .delete-mirrors-info');
    I.seeElement('#datatableInit_modal button.btn-primary:enabled');
    Document.screenshotElement('#datatableInit_modal .modal-content', '/redactor/webpages/mirroring/delete-pages.png', 1280, 600);
    DTE.cancel();

    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor('groups-datatable');
    I.waitForText('Zmazané budú aj nasledujúce zrkadlené priečinky:', 30, '#groups-datatable_modal .delete-mirrors-info');
    I.seeElement('#groups-datatable_modal button.btn-primary:enabled');
    Document.screenshotElement('#groups-datatable_modal .modal-content', '/redactor/webpages/mirroring/delete-groups.png', 1280, 600);
    DTE.cancel();
});

Scenario('Restore tree preferences and log out', ({ I }) => {
    if (originalSettings) {
        I.amOnPage(pageUrl);
        openSettings(I);
        for (const field of settingsFields) {
            const selector = '#jstree-settings-' + field;
            if (originalSettings.checked[field]) I.checkOption(selector);
            else I.uncheckOption(selector);
        }
        I.selectOption('#jstree-settings-treeWidth', originalSettings.width);
        saveSettings(I);
    }
    I.logout();
});
