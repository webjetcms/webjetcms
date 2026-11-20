Feature('webpages.group-tree-refresh');

const editorRoute = /\/admin\/rest\/groups\/editor(?:\?|$)/;
const treeRoute = /\/admin\/rest\/groups\/tree\/tree(?:\?|$)/;
const parentId = 67;
const editorId = 'groups-datatable';

Before(({ login }) => login('admin'));
After(({ I }) => {
    I.stopMockingRoute(editorRoute);
    I.stopMockingRoute(treeRoute);
});

for (const forceReload of [false, true]) {
    Scenario(`Folder creation keeps the parent selected with forceReload=${forceReload}`, async ({ I, DT, DTE }) => {
        const selectedFolder = `#SomStromcek [id="${parentId}_anchor"][aria-selected="true"]`;
        const name = 'autotest folder ' + I.getRandomText();
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${parentId}`);
        I.waitForVisible(selectedFolder, 15);
        I.click(DT.btn.tree_add_button);
        DTE.waitForEditor(editorId);
        I.fillField('#DTE_Field_groupName', name);
        const defaults = await I.executeScript(() => groupsDatatable.EDITOR.currentJson);
        await I.mockRoute(editorRoute, route => route.fulfill({json: {
            forceReload,
            data: [{...defaults, groupId: 900001, groupName: name, navbarName: name}]
        }}));

        let releaseRoot;
        const rootPending = new Promise(resolve => { releaseRoot = resolve; });
        let rootRequests = 0;
        await I.mockRoute(treeRoute, async route => {
            if (Number(route.request().postDataJSON().id) === 0) {
                rootRequests++;
                await rootPending;
            }
            return route.continue();
        });

        try {
            // Keep the first tree response pending while the editor's reload timers run.
            await I.usePlaywrightTo('control the editor reload timers', async ({ page }) => page.clock.install());
            DTE.save(editorId, true);
            await I.waitForInvisible(`#${editorId}_modal`, 10);
            await I.usePlaywrightTo('run the pending editor reload callbacks', async ({ page }) => page.clock.runFor(1500));
        } finally {
            releaseRoot();
        }

        await I.waitForVisible(selectedFolder, 10);
        I.seeInField('#tree-folder-id', String(parentId));
        I.assertEqual(rootRequests, 1);
    });
}
