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
async function waitForSnapshot(page) {
    await page.waitForFunction(() => {
        const conf = window.webpagesDatatable?.EDITOR.field('data').s.opts;
        if (conf?.dirtyDataOriginal === undefined) return false;
        if (conf.wjeditor.editingMode !== 'pageBuilder') return true;
        const frame = document.getElementById('DTE_Field_data-pageBuilderIframe');
        return frame.contentWindow.pageBuilderReady === true && !frame.contentDocument.body.inert;
    }, null, {timeout: 30000});
}

Scenario('pagebuilder - slow initialization stays clean', async ({ I, Document }) => {
    Document.resetPageBuilderMode();
    I.amCancellingPopups();
    const result = await I.usePlaywrightTo('delay PageBuilder beyond the former snapshot timeout', async ({ page }) => {
        page.setDefaultTimeout(30000);
        const routePattern = '**/*inlineEditorAdmin=true*';
        let delayed = false;
        const delayLoad = async route => {
            delayed = true;
            // Simulate network latency exceeding the former five-second dirty snapshot.
            await new Promise(resolve => setTimeout(resolve, 6000));
            await route.continue();
        };
        await page.route(routePattern, delayLoad, {times: 1});
        try {
            await page.evaluate(() => editPage(57));
            await waitForSnapshot(page);
            const dirty = await page.evaluate(() => webpagesDatatable.EDITOR.field('data').isDirty());
            await page.locator('div.DTED.show .DTE_Header button.btn-close-editor').click();
            await page.locator('div.DTED.show').waitFor({state: 'hidden'});
            return {delayed, dirty};
        } finally {
            await page.unroute(routePattern, delayLoad);
        }
    });
    I.assertTrue(result.delayed, 'The PageBuilder navigation must be delayed');
    I.assertFalse(result.dirty, 'Initialization alone must not mark the page as changed');
});

Scenario('pagebuilder - reopening ignores old readiness and detects the first edit', async ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();
    I.amCancellingPopups();
    const result = await I.usePlaywrightTo('reopen with a late event from the previous iframe', async ({ page }) => {
        page.setDefaultTimeout(30000);
        await page.evaluate(() => editPage(57));
        await waitForSnapshot(page);
        await page.evaluate(() => {
            window.autotestPreviousPageBuilderDocument = document.getElementById('DTE_Field_data-pageBuilderIframe').contentDocument;
        });
        const cancel = page.locator('div.DTED.show .DTE_Footer button.btn-close-editor');
        await cancel.click();
        await page.locator('div.DTED.show').waitFor({state: 'hidden'});

        let releaseLoad;
        const loadGate = new Promise(resolve => { releaseLoad = resolve; });
        const navigation = page.waitForRequest(request => request.isNavigationRequest() && request.url().includes('inlineEditorAdmin=true'));
        const routePattern = '**/*inlineEditorAdmin=true*';
        const holdLoad = async route => {
            await loadGate;
            await route.continue();
        };
        await page.route(routePattern, holdLoad, {times: 1});
        try {
            await page.evaluate(() => editPage(57));
            await navigation;
            const staleEventIgnored = await page.evaluate(() => {
                WJ.dispatchEvent('WJ.PageBuilder.ready', {document: window.autotestPreviousPageBuilderDocument});
                return webpagesDatatable.EDITOR.field('data').s.opts.dirtyDataOriginal === undefined;
            });
            releaseLoad();
            await waitForSnapshot(page);
            const clean = await page.evaluate(() => !webpagesDatatable.EDITOR.field('data').isDirty());
            const frame = await (await page.$('#DTE_Field_data-pageBuilderIframe')).contentFrame();
            const editable = frame.locator('[data-ckeditor-instance][contenteditable="true"]').first();
            await editable.click();
            await page.keyboard.type('autotest early edit');
            const dirty = await page.evaluate(() => webpagesDatatable.EDITOR.field('data').isDirty());
            await cancel.click();
            const stayedOpen = await page.locator('div.DTED.show').isVisible();
            return {staleEventIgnored, clean, dirty, stayedOpen};
        } finally {
            releaseLoad();
            await page.unroute(routePattern, holdLoad);
            await page.evaluate(() => { delete window.autotestPreviousPageBuilderDocument; });
        }
    });
    I.assertTrue(result.staleEventIgnored, 'A previous iframe must not provide the new baseline');
    I.assertTrue(result.clean, 'Reopening without edits must stay clean');
    I.assertTrue(result.dirty, 'The first edit after readiness must be detected immediately');
    I.assertTrue(result.stayedOpen, 'Dismissing the warning must keep the editor open');
    I.cancelPopup();
    I.amAcceptingPopups();
    I.click('div.DTED.show .DTE_Footer button.btn-close-editor');
    I.acceptPopup();
    DTE.waitForModalClose();
});

Scenario('check dirty - HTML mode', async ({ I, DTE, Document }) => {
    Document.setEditorMode('html');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
    I.amCancellingPopups();
    await I.usePlaywrightTo('wait for HTML mode to finish loading its source', async ({ page }) => {
        await waitForSnapshot(page);
    });
    const dirty = await I.executeScript(() => webpagesDatatable.EDITOR.field('data').isDirty());
    I.assertFalse(dirty, 'Opening source mode must not mark the page as changed');
    DTE.cancel();
    Document.resetPageBuilderMode();
});

Scenario("reset", ({ I }) => {
    I.amAcceptingPopups();
});
