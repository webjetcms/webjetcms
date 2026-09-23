Feature("webpages.dirty");

Before(({ I, login }) => {
    login('admin');
});

function checkDirty(docId, expectedDirtyState, isPagebuilder, I, DTE) {
    I.say("checkDirty, docId=" + docId + ", expectedDirtyState=" + expectedDirtyState, ", isPagebuilder=" + isPagebuilder);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
    DTE.waitForCkeditor();
    I.wait(10);

    I.amCancellingPopups();

    //cant't use DTE.cancel() because it waits for modal close which does not happen when dirty
    let cancelLocator = locate("div.DTED.show div.DTE_Footer.modal-footer button.btn-close-editor");

    if (expectedDirtyState) {
        if (isPagebuilder) {
            I.waitForElement("#DTE_Field_data-pageBuilderIframe");
            I.switchTo("#DTE_Field_data-pageBuilderIframe");
            I.waitForElement(locate("h3").withText("Etiam orci"), 10);
            I.click(locate("h3").withText("archiv"), null, {position: {x:130, y:10}});
        } else {
            I.waitForElement('#trEditor', 10);
            I.clickCss('#trEditor');
            I.wait(2);
            I.clickCss('#trEditor');
        }
        I.pressKey('x');
        I.pressKey('y');
        I.pressKey('z');
        I.switchTo();

        //
        I.say("Checking that dirty state is detected - editor should stay open on cancel");
        I.click(cancelLocator);
        I.wait(3);
        DTE.waitForEditor();

        //
        I.say("Checking for dirty - accpeting popup - editor should close");
        I.amAcceptingPopups();
        I.click(cancelLocator);
        DTE.waitForModalClose();

    } else {
        I.click(cancelLocator);
        DTE.waitForModalClose();
    }

}

Scenario("check dirty - normalpage", ({ I, DTE }) => {
    checkDirty(25, false, false, I, DTE);
    checkDirty(25, true, false, I, DTE);
});

Scenario("check dirty - pagebuilder", ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();
    checkDirty(57, false, true, I, DTE);
    checkDirty(57, true, true, I, DTE);
});

/** Waits until the current page has a normalized baseline and can accept input. */
function waitForSnapshot(I) {
    return I.waitForFunction(() => {
        const conf = window.webpagesDatatable?.EDITOR.field('data').s.opts;
        if (conf?.dirtyDataOriginal === undefined) return false;
        if (conf.wjeditor.editingMode !== 'pageBuilder') return true;
        const frame = document.getElementById('DTE_Field_data-pageBuilderIframe');
        return frame.contentWindow.pageBuilderReady === true && !frame.contentDocument.body.inert;
    }, 30);
}

Scenario('pagebuilder - slow initialization stays clean', async ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();
    I.amCancellingPopups();
    const routePattern = '**/*inlineEditorAdmin=true*';
    let delayed = false;
    const delayLoad = async route => {
        delayed = true;
        // Simulate network latency exceeding the former five-second dirty snapshot.
        await new Promise(resolve => setTimeout(resolve, 6000));
        await route.continue();
    };
    await I.mockRoute(routePattern, delayLoad, {times: 1});
    try {
        I.fillField('#tree-doc-id', '57');
        I.pressKey('Enter');
        await waitForSnapshot(I);
        const dirty = await I.executeScript(() => webpagesDatatable.EDITOR.field('data').isDirty());
        I.assertTrue(delayed, 'The PageBuilder navigation must be delayed');
        I.assertFalse(dirty, 'Initialization alone must not mark the page as changed');
        I.click('div.DTED.show .DTE_Header button.btn-close-editor');
        DTE.waitForModalClose();
        await I.dontSeeElement('div.DTED.show');
    } finally {
        await I.stopMockingRoute(routePattern, delayLoad);
    }
});

Scenario('pagebuilder - reopening ignores old readiness and detects the first edit', async ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();
    I.amCancellingPopups();
    I.fillField('#tree-doc-id', '57');
    I.pressKey('Enter');
    await waitForSnapshot(I);
    I.executeScript(() => {
        window.autotestPreviousPageBuilderDocument = document.getElementById('DTE_Field_data-pageBuilderIframe').contentDocument;
    });
    DTE.cancel();

    let releaseLoad;
    const loadGate = new Promise(resolve => { releaseLoad = resolve; });
    let navigationStarted;
    const navigation = new Promise(resolve => { navigationStarted = resolve; });
    let navigationTimeout;
    const routePattern = '**/*inlineEditorAdmin=true*';
    const holdLoad = async route => {
        navigationStarted();
        await loadGate;
        await route.continue();
    };
    await I.mockRoute(routePattern, holdLoad, {times: 1});
    try {
        I.fillField('#tree-doc-id', '57');
        await I.pressKey('Enter');
        // The handler records even a request that starts before the Enter step finishes.
        await Promise.race([
            navigation,
            new Promise((resolve, reject) => {
                navigationTimeout = setTimeout(() => reject(new Error('PageBuilder navigation did not start')), 30000);
            })
        ]);
        clearTimeout(navigationTimeout);
        const staleEventIgnored = await I.executeScript(() => {
            WJ.dispatchEvent('WJ.PageBuilder.ready', {document: window.autotestPreviousPageBuilderDocument});
            return webpagesDatatable.EDITOR.field('data').s.opts.dirtyDataOriginal === undefined;
        });
        I.assertTrue(staleEventIgnored, 'A previous iframe must not provide the new baseline');
        releaseLoad();
        await waitForSnapshot(I);
        const clean = await I.executeScript(() => !webpagesDatatable.EDITOR.field('data').isDirty());
        I.assertTrue(clean, 'Reopening without edits must stay clean');
        I.waitForElement('div.DTED.show[data-dte-focus-state="ready"]', 10);
        I.switchTo('#DTE_Field_data-pageBuilderIframe');
        I.click(locate('[data-ckeditor-instance][contenteditable="true"]').first());
        I.type('autotest early edit');
        I.see('autotest early edit', '[data-ckeditor-instance]');
        I.switchTo();
        const dirty = await I.executeScript(() => webpagesDatatable.EDITOR.field('data').isDirty());
        I.assertTrue(dirty, 'The first edit after readiness must be detected immediately');
        I.click('div.DTED.show .DTE_Footer button.btn-close-editor');
        I.cancelPopup();
        await I.seeElement('div.DTED.show');
    } finally {
        clearTimeout(navigationTimeout);
        releaseLoad();
        await I.stopMockingRoute(routePattern, holdLoad);
        I.switchTo();
        await I.executeScript(() => { delete window.autotestPreviousPageBuilderDocument; });
    }
    I.amAcceptingPopups();
    I.click('div.DTED.show .DTE_Footer button.btn-close-editor');
    I.acceptPopup();
    DTE.waitForModalClose();
});

Scenario('check dirty - HTML mode', async ({ I, DTE, Document }) => {
    Document.setEditorMode('html');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
    I.amCancellingPopups();
    await waitForSnapshot(I);
    const dirty = await I.executeScript(() => webpagesDatatable.EDITOR.field('data').isDirty());
    I.assertFalse(dirty, 'Opening source mode must not mark the page as changed');
    DTE.cancel();
    Document.resetPageBuilderMode();
});

Scenario("reset", ({ I }) => {
    I.amAcceptingPopups();
});
