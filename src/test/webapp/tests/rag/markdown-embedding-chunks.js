Feature('rag.markdown-embedding-chunks');

const WebjetDteJsTree = require('../../pages/WebjetDteJsTree');
const embeddingPage = '/admin/v9/settings/embedding-chunks/';
const markdownRoutes = /\/admin\/rest\/settings\/embedding-chunks\/markdown-/;
const markdownTableRoutes = /\/admin\/rest\/settings\/embedding-chunks\/(?:all|search\/findByColumns)(?:\?|$)/;
const folders = ['/admin/docs/autotest-webjetcms', 'file:/docs/sk/autotest-client'];
const folderInput = '#markdownFolderTree input.form-control';
const treeButton = '#markdownFolderTree .btn-webjet-jstree-item-edit';

Before(({ I, login }) => {
    login('admin');
    I.stopMockingRoute(markdownRoutes);
    I.stopMockingRoute(markdownTableRoutes);
});

Scenario('Markdown folder tree scopes the table and preserves the web-page tab', async ({ I, DT }) => {
    mockMarkdownApi(I);
    I.amOnPage(embeddingPage);
    DT.waitForLoader();
    I.waitForVisible('#editorApprootDir', 10);
    I.uncheckOption('#includeSubfolders');
    DT.waitForLoader();

    I.clickCss('#pills-markdown-tab');
    I.waitForVisible(treeButton + ':not(:disabled)', 10);
    I.seeInField(folderInput, folders[0]);
    I.assertTrue(await I.executeScript(() => embeddingChunksDataTable.column('sourcePath:name').visible()), 'Document paths must be visible for Markdown chunks.');
    I.assertFalse(await I.executeScript(() => embeddingChunksDataTable.column('entityId:name').visible()), 'Internal Markdown IDs must not be shown.');

    selectMarkdownFolder(I, [folders[1], 'admin']);
    I.seeInField(folderInput, folders[1] + '/admin');
    I.uncheckOption('#markdownIncludeSubfolders');
    DT.waitForLoader();
    await checkTableFolder(I, folders[1], 'admin', false);

    I.clickCss('#pills-document-tab');
    I.waitForVisible('#editorApprootDir', 10);
    DT.waitForLoader();
    I.dontSeeCheckboxIsChecked('#includeSubfolders');
    I.assertFalse(await I.executeScript(() => embeddingChunksDataTable.column('sourcePath:name').visible()), 'Markdown paths must be hidden for web pages.');
    I.assertTrue(await I.executeScript(() => embeddingChunksDataTable.column('entityId:name').visible()), 'Web page IDs must remain visible.');

    I.clickCss('#pills-markdown-tab');
    I.waitForVisible(treeButton, 10);
    I.seeInField(folderInput, folders[1] + '/admin');
    I.dontSeeCheckboxIsChecked('#markdownIncludeSubfolders');
    DT.waitForLoader();
    await checkTableFolder(I, folders[1], 'admin', false);
    I.stopMockingRoute(markdownRoutes);
    I.stopMockingRoute(markdownTableRoutes);
});

Scenario('Markdown actions use the selected subtree and recursion for indexing and removal', async ({ I, DT }) => {
    const api = mockMarkdownApi(I);
    I.amOnPage(embeddingPage + '#pills-markdown');
    I.waitForVisible(treeButton + ':not(:disabled)', 10);
    DT.waitForLoader();
    selectMarkdownFolder(I, [folders[1], 'admin']);
    I.uncheckOption('#markdownIncludeSubfolders');
    DT.waitForLoader();

    for (const action of ['index', 'delete']) {
        I.clickCss(action === 'index' ? 'button.btnAddIndex' : 'button.btnRemoveIndex');
        I.waitForVisible('#modalIframeIframeElement', 10);
        I.switchTo('#modalIframeIframeElement');
        I.waitForVisible(treeButton + ':not(:disabled)', 10);
        I.seeInField(folderInput, folders[1] + '/admin');
        I.dontSeeCheckboxIsChecked('#markdownIncludeSubfolders');
        await I.waitForText('2', 10, '#allDoc');
        I.seeTextEquals('0', '#queuedDoc');
        I.assertEqual(api.requests.length, action === 'index' ? 0 : 1, 'Opening a dialog must not enqueue any action.');

        if (action === 'index') I.checkOption('#markdownIncludeSubfolders');
        const count = action === 'index' ? 3 : 2;
        await I.waitForText(String(count), 10, '#allDoc');
        const scope = { folder: folders[1], directory: 'admin', includeSubfolders: String(action === 'index'), action: action.toUpperCase() };
        I.assertDeepEqual(api.stats, scope);

        I.switchTo();
        I.clickCss('#modalIframe .modal-footer button.btn-primary');
        I.switchTo('#modalIframeIframeElement');
        I.waitForVisible('#succ-msg-' + action + ':not(.d-none)', 10);
        I.see('(' + count + ')', '#succ-msg-' + action);
        await I.waitForText(String(count), 10, '#queuedDoc');
        I.seeTextEquals(String(count - 1), '#indexedDoc');
        I.assertDeepEqual(api.requests[api.requests.length - 1], { ...scope, method: 'POST' });

        I.switchTo();
        I.clickCss('#modalIframe .modal-header button.btn-close');
        I.waitForInvisible('#modalIframeIframeElement', 10);
    }
    await I.assertEqual(api.requests.length, 2, 'Exactly one request per explicit queue action is expected.');
    I.stopMockingRoute(markdownRoutes);
    I.stopMockingRoute(markdownTableRoutes);
});

Scenario('Markdown actions are disabled when no folders are configured', ({ I }) => {
    I.mockRoute(markdownRoutes, route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
    I.amOnPage(embeddingPage + '#pills-markdown');
    I.waitForVisible(folderInput, 10);
    I.waitForText('ragMarkdownFolders', 10, '#embeddingChunksDataTable_extfilter');
    I.verifyDisabled(treeButton);
    I.verifyDisabled('button.btnAddIndex');
    I.verifyDisabled('button.btnRemoveIndex');
    I.stopMockingRoute(markdownRoutes);
});

Scenario('Markdown embedding administration requires embeddingChunks permission', ({ I, DT }) => {
    I.amOnPage(embeddingPage);
    DT.waitForLoader();
    DT.checkPerms('embeddingChunks', embeddingPage);
});

/**
 * Replaces Markdown endpoints so browser tests never enqueue work or call an AI provider.
 *
 * @param {CodeceptJS.I} I - Actor used to register browser route mocks.
 * @returns {{requests: Object<string, string>[], stats: Object<string, string>|null}} Mutable request log and latest statistics scope.
 */
function mockMarkdownApi(I) {
    const api = { requests: [], stats: null };
    const queued = {};
    I.mockRoute(markdownTableRoutes, route => {
        const request = route.request();
        const url = new URL(request.url());
        if (request.method() !== 'GET' || url.searchParams.get('entityType') !== 'MARKDOWN' || !folders.includes(url.searchParams.get('sourceRoot'))) {
            return route.continue();
        }
        return route.fulfill({ json: { content: [], totalElements: 0, totalPages: 0, numberOfElements: 0, empty: true, options: {} } });
    });
    I.mockRoute(markdownRoutes, route => {
        const request = route.request();
        const url = new URL(request.url());
        let response = folders;
        if (url.pathname.endsWith('/markdown-folders/tree')) {
            response = { result: true, items: getFolderTreeNodes(String(JSON.parse(request.postData()).id)) };
        } else if (url.pathname.endsWith('/markdown-stat')) {
            const scope = Object.fromEntries(url.searchParams);
            api.stats = scope;
            const totalDocuments = scope.includeSubfolders === 'true' ? 3 : 2;
            response = { totalDocuments, indexedDocuments: totalDocuments - 1, queuedDocuments: queued[JSON.stringify(scope)] || 0 };
        } else if (url.pathname.endsWith('/markdown-action')) {
            const scope = Object.fromEntries(new URLSearchParams(request.postData()));
            api.requests.push({ ...scope, method: request.method() });
            response = scope.includeSubfolders === 'true' ? 3 : 2;
            queued[JSON.stringify(scope)] = response;
        }
        return route.fulfill({ json: response });
    });
    return api;
}

/**
 * Builds fixture nodes for the configured roots or the second root's admin subdirectory.
 *
 * @param {string} parent - Requested tree parent ID; '#' and '-1' request root nodes.
 * @returns {{id: string, parent: string, text: string, fullPath: string, sourceRoot: string, directory: string, children: boolean, icon: string, state: Object}[]} Child nodes, or an empty array for unknown parents.
 */
function getFolderTreeNodes(parent) {
    if (parent === '-1' || parent === '#') return folders.map((folder, index) => ({
        id: 'root-' + index, parent: '#', text: folder, fullPath: folder,
        sourceRoot: folder, directory: '', children: index === 1, icon: 'ti ti-folder', state: {}
    }));
    if (parent === 'root-1') return [{
        id: 'root-1-admin', parent, text: 'admin', fullPath: folders[1] + '/admin',
        sourceRoot: folders[1], directory: 'admin', children: false, icon: 'ti ti-folder', state: {}
    }];
    return [];
}

/**
 * Queues tree interactions that expand ancestors, select the final node, and wait for the picker to close.
 *
 * @param {CodeceptJS.I} I - Actor used to queue browser interactions.
 * @param {string[]} nodes - Non-empty sequence of visible node labels from root to the target folder.
 */
function selectMarkdownFolder(I, nodes) {
    I.clickCss(treeButton);
    I.waitForVisible(WebjetDteJsTree.tree, 10);
    nodes.forEach((name, index) => {
        const anchor = locate(WebjetDteJsTree.anchors).withTextEquals(name);
        I.waitForElement(anchor, 10);
        if (index === nodes.length - 1) {
            I.click(anchor);
        } else {
            I.click({ xpath: anchor.toXPath() + "/preceding-sibling::i[contains(@class, 'jstree-ocl')]" });
        }
    });
    I.waitForInvisible(WebjetDteJsTree.tree, 10);
}

/**
 * Reads the table request URL and queues an assertion that its Markdown scope matches the selection.
 *
 * @param {CodeceptJS.I} I - Actor used to inspect the page and assert filter values.
 * @param {string} folder - Expected configured source root.
 * @param {string} directory - Expected root-relative directory.
 * @param {boolean} includeSubfolders - Expected recursion flag.
 * @returns {Promise<void>} Resolves after reading the URL and scheduling the scope assertion.
 */
async function checkTableFolder(I, folder, directory, includeSubfolders) {
    const params = await I.executeScript(() => {
        const url = new URL(embeddingChunksDataTable.getAjaxUrl(), location.origin);
        return { entityType: url.searchParams.get('entityType'), sourceRoot: url.searchParams.get('sourceRoot'), directory: url.searchParams.get('directory'), includeSubfolders: url.searchParams.get('includeSubfolders') };
    });
    I.assertDeepEqual(params, { entityType: 'MARKDOWN', sourceRoot: folder, directory, includeSubfolders: String(includeSubfolders) });
}
