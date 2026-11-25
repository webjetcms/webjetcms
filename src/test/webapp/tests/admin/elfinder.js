Feature('admin.elFinder');

Before(({ I, login }) => {
    login('admin');
});

Scenario('folder tree search preserves hierarchy and clears empty results', async ({ I }) => {
    const form = '.elfinder-tree-search';
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_1_');
    I.waitForVisible(form, 15);
    I.fillField(form + ' input', 'bannery');
    I.pressKey('Enter');
    I.waitForVisible(form + '[aria-busy="false"]', 45);
    I.see('bannery', '.elfinder-tree-search-match');
    I.see('images', '.elfinder-tree');
    I.assertTrue(await I.executeScript(() => $('.elfinder-tree-search-match').first().parents('.elfinder-navbar-subtree').length >= 2));
    I.click(locate('.elfinder-tree-search-match').first());
    // The selected folder is exposed by elFinder rather than a form field.
    I.waitForFunction(() => elFinderInstance.cwd().name === 'bannery', [], 10);
    I.seeInField(form + ' input', 'bannery');
    I.fillField(form + ' input', 'autotest-no-such-folder-938274');
    I.pressKey('Enter');
    I.waitForVisible(form + '[aria-busy="false"]', 45);
    I.dontSeeElement('.elfinder-tree .elfinder-navbar-dir');
    I.seeElement('.elfinder-tree-search-status');
    I.clickCss(form + ' .btn-clear');
    I.seeInField(form + ' input', '');
    I.seeElement('.elfinder-tree .elfinder-navbar-dir');
});

Scenario('folder tree is disabled while searching and recovers after clear or failure', async ({ I }) => {
    let release, fail = false;
    const pending = new Promise(resolve => { release = resolve; });
    await I.mockRoute('**/admin/elfinder-connector/**', async route => {
        const params = new URLSearchParams(route.request().postData());
        if (!params.has('treeSearchValue')) return route.fallback();
        if (!fail) await pending;
        await route.fulfill({ json: fail ? { error: 'errUnknown' } : { tree: [], treeSearchMatches: [], treeSearch: true } });
    });
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_1_');
    const folder = locate('.elfinder-tree .elfinder-navbar-dir').withText('Obrázky');
    I.waitForVisible(folder, 15);
    I.moveCursorTo(folder);
    const bounds = await I.grabElementBoundingRect(folder);
    I.fillField('.elfinder-tree-search input', 'autotest-pending');
    I.pressKey('Enter');
    I.waitForElement('.elfinder-tree[inert][aria-busy="true"]', 5);
    I.dontSeeElement('.elfinder-tree .ui-droppable:not(.ui-droppable-disabled)');
    // Locator clicks wait for inert to clear; a physical click verifies blocked navigation.
    await I.usePlaywrightTo('click the disabled tree', async ({ page }) => page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
    I.assertEqual(await I.executeScript(() => elFinderInstance.cwd().hash), 'iwcm_1_');
    await I.clickCss('.elfinder-tree-search .btn-clear');
    await I.dontSeeElement('.elfinder-tree[inert]');
    release();
    await I.usePlaywrightTo('open the same folder after clearing', async ({ page }) => page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
    await I.waitForFunction(() => elFinderInstance.cwd().hash !== 'iwcm_1_', [], 10);
    fail = true;
    I.fillField('.elfinder-tree-search input', 'autotest-error');
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-tree-search[aria-busy="false"]', 10);
    I.dontSeeElement('.elfinder-tree[inert]');
    I.seeElement('.elfinder-tree-search-status');
    I.stopMockingRoute('**/admin/elfinder-connector/**');
});

Data([
    { page: 'index', view: 'icons' },
    { page: 'index', view: 'list' },
    { page: 'dialog', view: 'icons' }
]).Scenario('drag selection shows its rectangle and keeps selected files after release', async ({ I, current }) => {
    I.amOnPage('/admin/v9/files/' + current.page + '/#elf_iwcm_1_');
    I.waitForElement(locate('.elfinder-cwd-filename').withText('Foto galéria'), 10);
    I.executeScript(view => { if (elFinderInstance.viewType !== view) elFinderInstance.exec('view'); }, current.view);
    I.waitForVisible('.elfinder-cwd-view-' + current.view, 5);
    let end;
    // Hold a physical drag open to inspect its rectangle before releasing the mouse.
    await I.usePlaywrightTo('start drag selection', async ({ page }) => {
        const bounds = await page.locator('.elfinder-cwd-wrapper').boundingBox();
        const first = await page.locator('.elfinder-cwd-file').first().boundingBox();
        const last = await page.locator('.elfinder-cwd-file').last().boundingBox();
        end = { x: last.x + last.width + 2, y: first.y + 5 };
        await page.mouse.move(bounds.x + 5, last.y + last.height + 35);
        await page.mouse.down();
        await page.mouse.move(bounds.x + 13, last.y + last.height + 27);
    });
    I.seeElement('.ui-selectable-helper');
    I.assertNotEqual(await I.grabCssPropertyFrom('.ui-selectable-helper', 'border-style'), 'none');
    I.usePlaywrightTo('finish drag selection', async ({ page }) => {
        await page.mouse.move(end.x, end.y, { steps: 20 });
        await page.mouse.up();
    });
    I.dontSeeElement('.ui-selectable-helper');
    I.assertTrue((await I.executeScript(() => elFinderInstance.selected())).length > 1);
});

Data([
    { page: 'index', folder: 'iwcm_2_L2FwcHMvYWRtaW4_E', file: 'editor-component.html' },
    { page: 'dialog', folder: 'iwcm_1_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E', file: 'empty.txt' }
]).Scenario('file editor fills resized and maximized dialogs', async ({ I, current }) => {
    I.amOnPage('/admin/v9/files/' + current.page + '/#elf_' + current.folder);
    const file = locate('.elfinder-cwd-file').withDescendant(locate('.elfinder-cwd-filename').withTextEquals(current.file));
    const dialog = '.elfinder-dialog:has(.CodeMirror)';
    I.waitForVisible(file, 10);
    I.click(file);
    I.click('Nástroje');
    I.clickCss('.elfinder-toolbar .elfinder-button-icon-wjeditswitch');
    I.waitForVisible(dialog + ' .CodeMirror', 10);
    // Wait for editor dimensions to settle after resize/maximize animation frames.
    const checkLayout = () => I.waitForFunction(() => {
        const dialog = $('.elfinder-dialog:has(.CodeMirror)'), content = dialog.children('.ui-dialog-content');
        const editor = content.find('.CodeMirror'), footer = dialog.children('.ui-dialog-buttonpane');
        return Math.abs(editor.outerHeight() - content.height()) <= 2 && Math.abs(editor.outerWidth() - content.width()) <= 2 &&
            Math.abs(footer.offset().top + footer.outerHeight() - dialog.offset().top - dialog.innerHeight()) <= 2;
    }, [], 5);
    const original = await I.grabElementBoundingRect(dialog);
    I.dragAndDrop(dialog + ' .ui-resizable-se', dialog, { targetPosition: { x: original.width - 160, y: original.height - 100 } });
    I.assertTrue((await I.grabElementBoundingRect(dialog)).height < original.height - 50);
    checkLayout();
    I.clickCss(dialog + ' .elfinder-titlebar-full');
    I.waitForElement(dialog + '.elfinder-maximized', 5);
    for (const width of [700, 1280]) {
        I.resizeWindow(width, 760);
        checkLayout();
    }
    I.clickCss(dialog + ' .elfinder-titlebar-full');
    I.waitForInvisible(dialog + '.elfinder-maximized', 5);
    checkLayout();
    I.clickCss(dialog + ' .ui-dialog-titlebar-close');
});

Scenario('maximized file preview keeps its title bar and close button', ({ I }) => {
    const file = locate('.elfinder-cwd-file').withDescendant(locate('.elfinder-cwd-filename').withTextEquals('editor-component.html'));
    const preview = '.elfinder-quicklook';
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2FwcHMvYWRtaW4_E');
    I.waitForVisible(file, 10);
    I.click(file);
    I.click('Nástroje');
    I.clickCss('.elfinder-toolbar .elfinder-button-icon-quicklook');
    I.waitForText('appBeforeXhr', 10, preview + ' .elfinder-quicklook-preview');
    I.clickCss(preview + ' .elfinder-quicklook-navbar-icon-fullscreen');
    I.waitForElement(preview + '.elfinder-quicklook-fullscreen', 5);
    for (const width of [1280, 700]) {
        I.resizeWindow(width, 760);
        // Geometry settles asynchronously after the viewport resize.
        I.waitForFunction(() => {
            const title = document.querySelector('.elfinder-quicklook-titlebar').getBoundingClientRect();
            const content = document.querySelector('.elfinder-quicklook-preview').getBoundingClientRect();
            return title.top >= 0 && title.height > 0 && content.top >= title.bottom - 1 && content.bottom <= innerHeight + 1;
        }, [], 5);
    }
    I.waitForVisible(preview + ' .elfinder-quicklook-cover', 8);
    I.clickCss(preview + ' .elfinder-quicklook-titlebar .elfinder-icon-close');
    I.waitForInvisible(preview, 5);
});

function getFileUsage(I, DT, loader) {

    I.clickCss("#pills-dt-datatableInit-usage-tab");

    I.waitForText("Použitie", 300, ".nav-link.active");

    DT.waitForLoader(loader);

    I.see("Použitie", ".nav-link.active");

    I.see("Test file usage");

    I.switchTo();
}

Scenario('dir usage', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcw_E_E");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5');

    I.click(locate('#finder .elfinder-contextmenu-item').withText("Nastavenie priečinka"));
    I.waitForVisible("#modalIframe", 10);

    I.switchTo("#modalIframeIframeElement");

    I.waitForText("Indexovať súbory pre vyhľadávanie", 20);
    I.see("Obchodní partneri");

    getFileUsage(I, DT, "#datatableFieldDTE_Field_editorFields-docDetailsList_processing");
});

Scenario('file usage', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E');

    I.click(locate('#finder .elfinder-contextmenu-item').withText("Nastavenie súboru"));
    I.waitForVisible("#modalIframe", 10);

    //I.switchTo("#modalIframe");
    I.switchTo("#modalIframeIframeElement");

    I.waitForText("Názov súboru", 20);
    I.see("Zmeniť názov vo všetkých súboroch");

    getFileUsage(I, DT, "#datatableFieldDTE_Field_docDetailsList_processing");
})

Scenario('folder info counts nested files and file info keeps its size', async ({ I }) => {
    const target = 'iwcm_2_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E';
    const requests = [];
    await I.mockRoute('**/admin/elfinder-connector/**', async route => {
        if (new URLSearchParams(route.request().postData()).get('cmd') === 'foldersize') requests.push(route.request());
        await route.fallback();
    });
    I.amOnPage('/admin/v9/files/index/#elf_' + target);
    await I.waitForText('monitory', 10, '.elfinder-cwd-wrapper');
    I.assertEqual(requests.length, 0);
    I.executeScript(hash => elFinderInstance.exec('info', [hash]), target);
    // The fixture contains only monitory/apple-5k/monitor.jpg.
    I.waitForElement('.elfinder-folder-size [role=status][title="19822 B"]', 15);
    I.clickCss('.elfinder-dialog-info .ui-dialog-titlebar-close');
    I.amOnPage('/admin/elFinder/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5');
    const image = 'iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E';
    I.waitForElement('#' + image, 10);
    I.executeScript(hash => $('#finder').elfinder('instance').exec('info', [hash]), image);
    I.waitForText('Veľkosť', 10, '.elfinder-dialog-info .elfinder-info-tb');
    I.dontSeeElement('.elfinder-folder-size');
    I.stopMockingRoute('**/admin/elfinder-connector/**');
});

Scenario('folder info reports progress, cancels on close and retries a failed scan', async ({ I }) => {
    const target = 'iwcm_2_L2ltYWdlcy9iYW5uZXJ5', actions = [];
    let starts = 0;
    await I.mockRoute('**/admin/elfinder-connector/**', async route => {
        const params = new URLSearchParams(route.request().postData());
        if (params.get('cmd') !== 'foldersize') return route.fallback();
        actions.push(params.get('action'));
        if (params.get('action') === 'start') starts++;
        const state = starts === 1 ? 'running' : starts === 2 ? 'failed' : 'complete';
        const starting = starts === 1 && params.get('action') === 'start';
        await route.fulfill({json: {scanId: 'scan-' + starts, state, bytes: starting ? '0' : '8388608', folders: starting ? 0 : 2582}});
    });
    I.amOnPage('/admin/v9/files/index/#elf_' + target);
    I.waitForText('banner-iwayday.png', 10, '.elfinder-cwd-wrapper');
    I.executeScript(hash => elFinderInstance.exec('info', [hash]), target);
    I.waitForText('Zatiaľ 8 MB · skontrolované priečinky: 2\u00a0582', 10, '.elfinder-folder-size');
    I.clickCss('.elfinder-dialog-info .ui-dialog-titlebar-close');
    I.executeScript(hash => elFinderInstance.exec('info', [hash]), target);
    I.waitForText('Veľkosť sa nepodarilo vypočítať.', 10, '.elfinder-folder-size');
    I.click('Skúsiť znova', '.elfinder-folder-size');
    await I.waitForElement('.elfinder-folder-size [role=status][title="8388608 B"]', 10);
    I.assertTrue(actions.includes('status') && actions.includes('cancel'));
    I.assertEqual(starts, 3);
    I.stopMockingRoute('**/admin/elfinder-connector/**');
});

Scenario('file update accepts one file and does not show progress for validation errors', async ({ I }) => {
    I.amOnPage('/admin/elFinder/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5');
    I.waitForElement('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E', 10);
    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E');
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjfileupdate');
    I.waitForElement('.elfinder-upload-dialog-wrapper input[type=file]', 10);
    I.dontSeeElement('.elfinder-upload-dialog-wrapper .elfinder-upload-dirselect');

    const acceptsMultipleFiles = await I.executeScript(() =>
        document.querySelector('.elfinder-upload-dialog-wrapper input[type=file]').hasAttribute('multiple'));
    I.assertFalse(acceptsMultipleFiles, 'The update file picker must accept only one file');

    I.executeScript(() =>
        document.querySelector('.elfinder-upload-dialog-wrapper input[type=file]').dispatchEvent(new Event('change', { bubbles: true })));
    I.dontSeeElement('.elfinder-notify-customErrorDialog');

    I.attachFile('.elfinder-upload-dialog-wrapper input[type=file]', 'tests/apps/file-archive/docs/archive_replace.pdf');
    I.waitForElement('.elfinder-notify-customErrorDialog', 10);
    const error = await I.executeScript(() => {
        const finder = $('#finder').elfinder('instance');
        const message = document.querySelector('.elfinder-notify-customErrorDialog .elfinder-notify-msg').textContent;
        return { message, expected: finder.i18n('wjfileupdate-typeMismatch') + 'image/png' };
    });
    I.assertEqual(error.message, error.expected, 'A wrong file type must show the type mismatch error');
    I.dontSeeElement('.elfinder-notify-customErrorDialog .elfinder-notify-progressbar');
});

Scenario('link dialog marks archive volume paths read-only', async ({ I, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=16');
    DTE.waitForEditor();
    I.clickCss('#trEditor');
    I.clickCss('.cke_button_icon.cke_button__link_icon');
    I.waitForText('Informácie o odkaze', 10);
    I.switchTo('#wjLinkIframe');
    I.waitForElement('#finder .elfinder-navbar', 20);

    const markers = await I.executeScript(() => {
        const readOnlyMarker = $('#finder').elfinder('instance').options.readOnlyMarker;
        const file = { read: 1, write: 0, notfound: false };
        return {
            archiveRoot: readOnlyMarker({ ...file, virtualPath: '//files/archiv' }),
            archiveChild: readOnlyMarker({ ...file, virtualPath: '//files//archiv/category' }),
            otherFolder: readOnlyMarker({ ...file, virtualPath: '//files/archiv-other' }),
            writableFile: readOnlyMarker({ ...file, virtualPath: '//files/archiv/file.pdf', write: 1 })
        };
    });
    I.assertTrue(markers.archiveRoot, 'The archive volume root must show the read-only marker');
    I.assertTrue(markers.archiveChild, 'Archive subfolders must show the read-only marker');
    I.assertFalse(markers.otherFolder, 'Folders outside the archive must keep their usual marker');
    I.assertFalse(markers.writableFile, 'Writable files must not show the read-only marker');
});

Scenario('search files', ({I}) => {

    //all media/images/apps/
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_L2ltYWdlcy9hcHBz");
    //atributy-stranky
    I.waitForText("atributy-stranky", 10, "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

    //
    I.say("Searching in current directory");
    I.fillField("div.elfinder-button-search input", "monitor");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwd]");
    I.waitForLoader(".WJLoaderDiv");
    //apps-monitor.jpg - there ist &zeroWidthSpace; in the name
    I.see("apps-monitor", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2FwcHMtbW9uaXRvci5qcGc_E");
    //atributy-stranky
    I.dontSee("atributy-stranky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

    //
    I.say("Searching in current directory and subdirectories");
    I.fillField("div.elfinder-button-search input", "monitor");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwdRecursive]");
    I.waitForLoader(".WJLoaderDiv");
    //apps-monitor.jpg
    I.see("apps-monitor", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2FwcHMtbW9uaXRvci5qcGc_E");
    //monitor
    I.see("monitor", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3kvbW9uaXRvcnkvYXBwbGUtNWsvbW9uaXRvci5qcGc_E");

    //atributy-stranky
    I.dontSee("atributy-stranky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

})

Scenario('recover from an empty folder search', ({ I }) => {
    const input = '.elfinder-button-search input[name="wjsearch"]';
    const empty = '.elfinder-search-empty';
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E');
    I.waitForText('monitory', 10, '.elfinder-cwd-wrapper');
    I.fillField(input, 'monitor.jpg');
    I.pressKey('Enter');
    I.waitForText('Nenašli sa žiadne súbory.', 10, empty);
    I.dontSeeElement('.elfinder-empty-folder');
    I.click('Hľadať aj v podpriečinkoch', empty);
    I.waitForVisible('#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3kvbW9uaXRvcnkvYXBwbGUtNWsvbW9uaXRvci5qcGc_E', 15);
    I.fillField(input, 'autotest-missing-search-result');
    I.pressKey('Enter');
    I.waitForVisible(empty, 10);
    I.dontSeeElement(empty + ' [data-action="subfolders"]');
    I.click('Zrušiť vyhľadávanie', empty);
    I.waitForText('monitory', 10, '.elfinder-cwd-wrapper');
    I.seeInField(input, '');
});

Scenario('search webpages', ({I}) => {

    //webpages/Aplikacie/
    I.amOnPage("/admin/elFinder/#elf_iwcm_doc_group_volume_L2dyb3VwOjE1MjU3");

    //atributy-stranky
    I.waitForText("Atribúty stránky", 10, "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2dyb3VwOjQ5MzM0");

    //
    I.say("Searching in current directory");
    I.fillField("div.elfinder-button-search input", "galer");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwd]");
    I.waitForLoader(".WJLoaderDiv");
    //Galery
    I.see("Galery", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2RvYzo1OTA3MQ_E_E");
    //Atribúty stránky
    I.dontSee("Atribúty stránky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_doc_group_volume_L2dyb3VwOjQ5MzM0");

    //
    I.say("Searching in current directory and subdirectories");
    I.fillField("div.elfinder-button-search input", "galer");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwdRecursive]");
    I.waitForLoader(".WJLoaderDiv");
    //Galery
    I.see("Galery", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2RvYzo1OTA3MQ_E_E");
    //Galeria
    I.see("Galéria", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2RvYzo0NTkyNg_E_E");
    //Atribúty stránky
    I.dontSee("Atribúty stránky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_doc_group_volume_L2dyb3VwOjQ5MzM0");
})

Scenario('remember last elfinder dir in webpages', ({I, DTE}) => {
    const imageButton = '.cke_button__image:not(.cke_button_disabled):visible';
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=16");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.clickCss('#trEditor');
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    //
    I.say("Opening image dialog and navigating to test folder");

    I.waitForVisible(imageButton, 20);
    I.clickCss(imageButton);
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), 20);
    I.wait(1);
    I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), null, { position: { x: 20, y: 5 } });
    I.waitForText('Foto galéria', 10, ".elfinder-cwd-file");
    I.waitForVisible(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'), 10);
    I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'));
    I.waitForVisible(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'), 20);
    I.wait(1);
    I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'));
    I.waitForVisible("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L3Rlc3QvZGVzZXJ0LmpwZw_E_E", 10); //test/desert.jpg
    I.waitForVisible(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee'), 10);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    //
    I.say("Checking if test folder is selected after reopening image dialog");

    I.waitForVisible(imageButton, 20);
    I.clickCss(imageButton);
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement(locate("div.elfinder-navbar-wrapper span.elfinder-navbar-dir.ui-state-active").withText("test"), 10);
    I.seeElement("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L3Rlc3Qva29hbGEuanBn"); //koala.jpg

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();

    //
    I.say("Opening another page, image dialog should be in Media tejto stranky folder");

    I.click("Trhy sú naďalej vydesené");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.click(locate('#trEditor'), null, { position: { x: 50, y: 250 } });
    I.click(locate('#trEditor'), null, { position: { x: 50, y: 250 } });
    I.pressKey('ArrowDown');

    I.waitForVisible(imageButton, 20);
    I.clickCss(imageButton);
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement("#nav-iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS90cmh5LXN1LW5hZGFsZWotdnlkZXNlbmU_E.ui-state-active", 10);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();
})

Scenario('folder for new page by title', ({I, DTE, Browser}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=24");

    I.clickCss("#datatableInit_wrapper div.dt-buttons button.buttons-create");
    DTE.waitForEditor();
    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    DTE.fillField("title", "Utorok 02");
    I.clickCss("#pills-dt-datatableInit-content-tab");

    //
    I.say("Open image dialog and check if it is in the right folder");

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement("#iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS91dG9yb2stMDIvc2F2ZS13YXJuaW5nLnBuZw_E_E", 10); ///images/zo-sveta-financii/utorok-02/save-warning.png

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    //
    I.say("Change title and check it's reloaded");

    I.clickCss("#pills-dt-datatableInit-basic-tab");
    DTE.fillField("title", "Pondelok");
    DTE.fillField("navbar", "Pondelok");
    I.clickCss("#pills-dt-datatableInit-content-tab");

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement("#iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS9wb25kZWxvay9tYXhyZXNkZWZhdWx0LmpwZw_E_E", 10); ///images/zo-sveta-financii/pondelok/maxresdefault.jpg

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();
})

Scenario('CVE-2022-26960', async ({I}) => {
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    ///files/protected/bankari/test-forward.txt
    await I.executeScript(function(){
        window.location.href="/admin/elfinder-connector/?cmd=file&volumes=files&target=iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9iYW5rYXJpL3Rlc3QtZm9yd2FyZC50eHQ_E";
    });
    I.waitForText("This is test file for fileforward after logon", 10);

    I.amOnPage("/admin/v9/apps/audit-search/");
    await I.executeScript(function(){
        // ../../../../../../../../../../../../../../www/tomcat_au27/conf/server.xml
        window.location.href="/admin/elfinder-connector/?cmd=file&volumes=files&target=iwcm_1_Li4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vd3d3L3RvbWNhdF9hdTI3L2NvbmYvc2VydmVyLnhtbA==";
    });
    I.dontSee("This is test file for fileforward after logon");
    I.dontSee("Server");

    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    ///files/../protected/bankari/test-forward.txt
    await I.executeScript(function(){
        window.location.href="/admin/elfinder-connector/?cmd=file&volumes=files&target=iwcm_1_L2ZpbGVzLy4uL3Byb3RlY3RlZC9iYW5rYXJpL3Rlc3QtZm9yd2FyZC50eHQ=";
    });
    I.dontSee("This is test file for fileforward after logon");
})

Scenario('Explorer - file diacritics test', async ({ I }) => {
    await deleteTestFiles(I);
    await testFileDiacritics(I, true);
    await deleteTestFiles(I);
    await testFileDiacritics(I, false);
})

async function testFileDiacritics(I, allowDiacritics) {
    const pageUrl = allowDiacritics
        ? '/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVz'
        : '/admin/v9/files/index/?removePerm=fbrowser_allow_diacritics#elf_iwcm_2_L2ZpbGVz';
    const uploadedFileName = allowDiacritics ? 'ľščťú žýáíéô.png' : 'lsctu-zyaieo.png';
    const renamedFileName = allowDiacritics ? 'ľščťú-žýáíéô.png' : 'lsctu-zyaieo.png';
    const createdFolderName = allowDiacritics ? 'ľščťú žýáíéô' : 'lsctu-zyaieo';
    const renamedFolderName = allowDiacritics ? 'ľščťú-žýáíéô' : 'lsctu-zyaieo';

    I.amOnPage(pageUrl);
    I.waitForVisible('#nav-iwcm_2_L2ZpbGVz', 20);

    I.say('Uploading file with diacritics in name');
    I.click('.elfinder-navbar-wrapper span[id^="nav-iwcm_2_"][title*="files"] .elfinder-navbar-icon');
    I.click('.elfinder-button-icon.elfinder-button-icon-upload');
    I.attachFile('input[type=file]', 'tests/admin/ľščťú žýáíéô.png');
    waitForUpload(I);

    I.say(`Checking uploaded file name with${allowDiacritics ? '' : 'out'} diacritics permission`);
    checkCorrectTitle(I, uploadedFileName);

    I.say('Try to rename a file to name with diacritics');
    searchFile(I, uploadedFileName);
    within('.elfinder-cwd-wrapper', () => {
        I.waitForText(uploadedFileName, 10);
        I.rightClick(uploadedFileName);
    });
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rename');

    I.fillField({ css: `.elfinder-cwd-filename[title="${uploadedFileName}"] textarea` }, 'ľščťú-žýáíéô.png');
    I.pressKey('Enter');
    I.clickCss('.elfinder-button-search > .ui-icon-close');
    checkCorrectTitle(I, renamedFileName);
    await deleteTestFiles(I);

    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    I.amOnPage(pageUrl);
    I.waitForVisible('#nav-iwcm_2_L2ZpbGVz', 20);
    I.clickCss("#nav-iwcm_2_L2ZpbGVz"); //files
    createFolder(I, 'ľščťú žýáíéô');
    checkCorrectTitle(I, createdFolderName);

    I.say('Try to rename a folder to the name with diacritics');
    I.rightClick(locate(".elfinder-cwd-file").withText(createdFolderName));

    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rename');

    I.fillField({ css: `.elfinder-cwd-filename[title="${createdFolderName}"] textarea` }, 'ľščťú-žýáíéô');
    I.pressKey('Enter');
    I.clickCss('.elfinder-button-search > .ui-icon-close');
    checkCorrectTitle(I, renamedFolderName);
}

Scenario('Explorer - file diacritics delete', async ({ I }) => {
    await deleteTestFiles(I);
})

Scenario('Elfinder - Move Confirm with config value true', async ({ I, Document }) => {
    Document.setConfigValue('elfinderMoveConfirm', 'true');
    await deleteTestFiles(I);
    await checkMoveConfirm(I, true);
})

Scenario('Elfinder - Move Confirm with config value false', async ({ I, Document }) => {
    Document.setConfigValue('elfinderMoveConfirm', 'false');
    await deleteTestFiles(I);
    await checkMoveConfirm(I, false);
})

Scenario('revert changes - Move Confirm with config value false', async ({ I, Document }) => {
    await deleteTestFiles(I);
    Document.setConfigValue('elfinderMoveConfirm', 'true');
})

function searchFile(I, fileName) {
    I.clickCss('.elfinder-button-search > input[type="text"]');
    //I.type(fileName);
    I.fillField('.elfinder-button-search > input[type="text"]', fileName);
    I.clickCss('label[for = elfinder-finderSearchFromCwd]');
}

async function checkMoveConfirm(I, elfinderMoveConfirm) {
    //to be sure link is working with hash
    I.amOnPage('/admin/v9/');
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');
    I.clickCss('#nav-iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');

    I.say('Testing with drag and drop.');
    createFolder(I, 'outer');
    createFolder(I, 'inner');

    I.dragAndDrop(getFileSelector('inner'), getFileSelector('outer'), { force: true });

    handleConfirmationDialog(elfinderMoveConfirm, I);

    I.seeElement(getFileSelector('outer'));
    I.waitForInvisible(getFileSelector('inner'));
    I.dontSeeElement(getFileSelector('inner'));

    await deleteFile(I, 'outer');

    I.say('Testing with context menu');

    I.clickCss('#nav-iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');
    createFolder(I, 'outer');
    createFolder(I, 'inner');

    I.rightClick(getFileSelector('inner'));
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-button-icon.elfinder-button-icon-cut.elfinder-contextmenu-icon');

    I.rightClick(getFileSelector('outer'));
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-button-icon.elfinder-button-icon-paste.elfinder-contextmenu-icon');

    handleConfirmationDialog(elfinderMoveConfirm, I);

    I.seeElement(getFileSelector('outer'));
    I.waitForInvisible(getFileSelector('inner'));
    I.dontSeeElement(getFileSelector('inner'));
}

function handleConfirmationDialog(elfinderMoveConfirm, I) {
    if (elfinderMoveConfirm) {
        I.waitForVisible('div.ui-dialog.ui-widget.elfinder-dialog-confirm', 10);
        I.see('Naozaj chcete presunúť položky do');
        I.clickCss('button.elfinder-confirm-accept');
    }
    else {
        I.dontSeeElement('div.ui-dialog.ui-widget.elfinder-dialog-confirm');
        I.dontSee('Naozaj chcete presunúť položky do');
        I.waitForVisible('.ui-front.toast-success.elfinder-frontmost', 10);
        I.wait(2);
    }
}

function createFolder(I, folderName) {
    I.say('Creating a folder');
    I.click('.elfinder-button-icon.elfinder-button-icon-mkdir');
    I.fillField({ css: '.elfinder-cwd-filename[title="Nový priečinok"] textarea' }, folderName);
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-toast', 10);
    I.wait(2);
}

function waitForUpload(I) {
    I.clickIfVisible('.elfinder-confirm-accept');
    I.waitForVisible('.elfinder-notify-chunkmerge', 10);
    I.waitForInvisible('.elfinder-notify-upload', 80);
    I.waitForInvisible('.elfinder-notify-chunkmerge', 10);
}

function checkCorrectTitle(I, expectedTitle) {
    I.waitForElement(getFileSelector(expectedTitle), 80);
}

async function deleteTestFiles(I) {
    I.say('Deleting uploaded files');
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVz');
    I.waitForVisible('#nav-iwcm_2_L2ZpbGVz.ui-state-active', 20);
    let fileNames = ['lsctu-zyaieo.png', 'ľščťú-žýáíéô.png', 'ľščťú žýáíéô.png', 'lsctu zyaieo.png',
        'lsctu-zyaieo', 'ľščťú-žýáíéô', 'ľščťú žýáíéô', 'lsctu zyaieo'];
    for (const fileName of fileNames) {
        await deleteFile(I, fileName);
    }

    //otherwise hash change will not work
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E");
    I.waitForVisible('#nav-iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E.ui-state-active', 20);
    fileNames = ['outer', 'inner'];
    for (const fileName of fileNames) {
        await deleteFile(I, fileName);
    }
}

function getFileSelector(fileName) {
    return `.elfinder-cwd-filename[title="${fileName}"]`;
}

async function deleteFile(I, fileName) {
    const numVisible = await I.grabNumberOfVisibleElements(`.elfinder-cwd-filename[title="${fileName}"]`);
    if (numVisible) {
        I.rightClick(fileName, ".elfinder-cwd-wrapper");
        I.waitForVisible('.elfinder-contextmenu', 10);
        I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rm');
        I.clickCss('.elfinder-confirm-accept');
        I.waitForInvisible(fileName);
    }
}

Scenario('BUG 57669 - file size in dir', async ({ I }) => {
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9nYWxsZXJ5L3Rlc3QtdmVsYS1mb3Rv");
    I.waitForText("Položky: 327", 20, ".elfinder-statusbar .elfinder-stat-size");
    // Image processing can change fixture sizes; compare the total with the listed files.
    const expected = await I.executeScript(() => {
        const finder = $('#finder').elfinder('instance');
        const files = Object.values(finder.files(finder.cwd().hash));
        const bytes = files.reduce((sum, file) => sum + Number(file.size), 0);
        return { bytes, formatted: finder.formatSize(bytes) };
    });
    I.assertAbove(expected.bytes, 0);
    I.waitForText("Súčet: " + expected.formatted, 10, ".elfinder-statusbar .elfinder-stat-size.elfinder-stat-size-recursive");
});

Scenario('home menu lists every accessible root including the current volume', async ({ I }) => {
    const roots = ['Médiá všetkých stránok', 'Všetky súbory', 'Webové stránky'];
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5');
    I.waitForVisible('.elfinder-path-roots', 10);
    for (const root of roots) {
        I.focus('.elfinder-path-roots');
        I.pressKey('Enter');
        I.waitForVisible('.elfinder-contextmenu', 5);
        I.assertDeepEqual(await I.grabTextFromAll('.elfinder-contextmenu-item'), roots);
        I.click(locate('.elfinder-contextmenu-item').withText(root));
        I.waitForText(root, 10, '.elfinder-path-cwd');
        I.seeNumberOfElements('.elfinder-path-dir', 1);
    }
});

Scenario('explorer breadcrumbs and info actions', ({ I }) => {
    const images = '#iwcm_2_L2ltYWdlcw_E_E .elfinder-cwd-icon';
    const banners = '#iwcm_2_L2ltYWdlcy9iYW5uZXJ5 .elfinder-cwd-icon';
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5');
    I.waitForVisible('.elfinder-workzone-path .elfinder-copy-button', 10);
    I.clickCss('.elfinder-workzone-path .elfinder-copy-button');
    I.waitForElement('.elfinder-workzone-path button[aria-label="Skopírované"]', 5);
    I.focus(locate('.elfinder-path-dir').withText('images'));
    I.pressKey('Enter');
    I.waitForText('images', 10, '.elfinder-path-cwd');
    I.seeElement(banners);
    I.click(locate('.elfinder-path-dir').withText('Všetky súbory'));
    I.waitForVisible(images, 10);
    I.click(images);
    I.pressKey(['Control', 'ArrowDown']);
    I.waitForVisible(banners, 10);
    I.click(banners);
    I.pressKey(['Control', 'ArrowDown']);
    const image = '#iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E .elfinder-cwd-icon';
    I.waitForVisible(image, 10);
    I.rightClick(image);
    I.see('banner-iwayday.png', '.elfinder-contextmenu-header');
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-info');
    I.waitForVisible('.elfinder-dialog-info', 10);
    I.seeElement('.elfinder-dialog-info button[aria-label="Kopírovať odkaz"]');
    I.seeElement('.elfinder-info-technical:not([open])');
    I.click('.elfinder-info-technical summary');
    I.seeElement('.elfinder-info-technical[open] .elfinder-info-hash');
    I.clickCss('.elfinder-dialog-info .ui-dialog-titlebar-close');
});

Data([{page: 'index', volume: 'iwcm_2_'}, {page: 'dialog', volume: 'iwcm_1_'}])
.Scenario('file tabs keep selection and tools active in both views', async ({ I, current }) => {
    const hash = current.volume + 'L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E';
    const info = '.elfinder-toolbar .elfinder-button:not(.ui-state-disabled):has(.elfinder-button-icon-info)';
    I.amOnPage('/admin/v9/files/' + current.page + '/#elf_' + current.volume + 'L2ltYWdlcy9iYW5uZXJ5');
    I.waitForVisible('#' + hash, 15);
    for (const view of ['list', 'icons']) {
        I.executeScript(view => { if (elFinderInstance.viewType !== view) elFinderInstance.exec('view'); }, view);
        I.waitForVisible('.elfinder-cwd-view-' + view, 5);
        I.clickCss('#' + hash + ' .elfinder-cwd-icon');
        I.click('Nástroje');
        I.waitForVisible(info, 5);
        I.assertDeepEqual(await I.executeScript(() => elFinderInstance.selected()), [hash]);
        I.clickCss(info);
        I.waitForText('banner-iwayday.png', 5, '.elfinder-dialog-info');
        I.clickCss('.elfinder-dialog-info .ui-dialog-titlebar-close');
        if (current.page === 'index') {
            I.focus(locate('.nav-link').withTextEquals('Súbor'));
            I.pressKey('Enter');
        } else {
            I.click('Súbor');
        }
        I.waitForVisible('.elfinder-toolbar .elfinder-button-icon-copy', 5);
        I.pressKey(['Control', 'a']);
        const selected = await I.executeScript(() => elFinderInstance.selected());
        I.assertTrue(selected.length > 1);
        I.click('Nástroje');
        I.waitForVisible(info, 5);
        I.assertDeepEqual(await I.executeScript(() => elFinderInstance.selected()), selected);
        I.click('Súbor');
    }
});

/** Supplies isolated folder contents without creating or changing server files. */
async function mockElfinderContents(I, state) {
    const target = 'iwcm_2_L2ltYWdlcy9iYW5uZXJ5';
    await I.mockRoute('**/admin/elfinder-connector/**', async route => {
        const params = new URLSearchParams(route.request().postData());
        if (params.get('cmd') !== 'open' || params.get('target') !== target) return route.fallback();
        const data = await (await route.fetch()).json();
        data.cwd.write = state.writable === false ? 0 : 1;
        data.files = data.files.filter(file => file.phash !== target);
        data.files.forEach(file => { if (file.hash === target) file.write = data.cwd.write; });
        data.options.disabled = (data.options.disabled || []).filter(command => command !== 'upload');
        if (state.uploadDisabled) data.options.disabled.push('upload');
        (state.files || []).forEach((file, index) => data.files.push({
            hash: 'iwcm_2_autotestcaption' + index, phash: target, mime: 'application/octet-stream',
            size: 1024, ts: 1795550400, read: 1, write: 1, locked: 0, ...file
        }));
        await route.fulfill({ json: data });
    });
    return '/admin/v9/files/index/#elf_' + target;
}

Scenario('icon captions preserve extensions and full names after rename cancellation', async ({ I }) => {
    const names = ['autotest-produktovy-katalog-Žilina-東京-2027.pdf', 'autotest-zaloha.tar.gz',
        'autotest-<img src=x onerror=alert(1)>.png', '.autotest', 'autotest-bez-pripony', 'autotest.folder.with.dots'];
    I.amOnPage(await mockElfinderContents(I, {files: names.map((name, index) => ({name, mime: index === 5 ? 'directory' : 'application/octet-stream'}))}));
    const caption = '#iwcm_2_autotestcaption0 .elfinder-cwd-filename';
    I.waitForElement(caption, 10);
    I.executeScript(() => { if (elFinderInstance.viewType !== 'icons') elFinderInstance.exec('view'); });
    I.waitForElement(caption + ' .elfinder-cwd-filename-extension', 5);
    I.resizeWindow(600, 800);
    I.seeTextEquals('.pdf', caption + ' .elfinder-cwd-filename-extension');
    I.seeTextEquals('.tar.gz', '#iwcm_2_autotestcaption1 .elfinder-cwd-filename-extension');
    I.dontSeeElement('#iwcm_2_autotestcaption2 img');
    for (const index of [3, 4, 5]) I.dontSeeElement('#iwcm_2_autotestcaption' + index + ' .elfinder-cwd-filename-extension');
    const rendered = await I.executeScript(() => [...document.querySelectorAll('.elfinder-cwd-filename')]
        .map(node => ({title: node.title, text: node.textContent.replace(/\u200b/g, '')})));
    for (const name of names) I.assertTrue(rendered.some(item => item.title === name && item.text === name));
    I.executeScript(() => { elFinderInstance.exec('rename', ['iwcm_2_autotestcaption0']); });
    I.waitForElement(caption + ' textarea', 5);
    I.seeInField(caption + ' textarea', names[0]);
    I.pressKey('Escape');
    I.waitForElement(caption + ' .elfinder-cwd-filename-extension', 5);
    I.executeScript(() => elFinderInstance.exec('view'));
    I.waitForElement('.elfinder-cwd-view-list', 5);
    I.seeTextEquals(names[0], caption);
    I.dontSeeElement('.elfinder-cwd-filename-extension');
    I.executeScript(() => elFinderInstance.exec('view'));
    I.wjSetDefaultWindowSize();
    I.stopMockingRoute('**/admin/elfinder-connector/**');
});

Scenario('empty folder prompt opens upload and respects permissions', async ({ I }) => {
    const state = {writable: true, files: []};
    const prompt = '.elfinder-empty-folder', button = prompt + ' [data-action=upload]';
    I.amOnPage(await mockElfinderContents(I, state));
    I.waitForVisible(button, 10);
    I.focus(button);
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-dialog-upload input[type=file]', 5);
    await I.click({css: '.elfinder-dialog-upload .ui-dialog-titlebar-close'});
    for (const permissions of [{writable: false}, {writable: true, uploadDisabled: true}]) {
        Object.assign(state, permissions);
        I.refreshPage();
        I.waitForVisible(prompt, 10);
        I.waitForInvisible(button, 5);
        await I.waitForInvisible(prompt + ' .elfinder-empty-folder-hint', 5);
    }
    Object.assign(state, {uploadDisabled: false, files: [{name: 'autotest-nonempty.txt'}]});
    I.refreshPage();
    I.waitForElement('#iwcm_2_autotestcaption0', 10);
    I.dontSeeElement(prompt);
    I.stopMockingRoute('**/admin/elfinder-connector/**');
});
