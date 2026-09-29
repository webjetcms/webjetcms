Feature('admin.dashboard-autocomplete').tag('@singlethread');

let marker;
let query;
const folders = [];
const pages = { hidden: [], visible: [] };

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});

/** Uses the authenticated browser session for fixture creation and real JSP requests. */
async function request(I, url, body) {
    return I.executeScript(async ({ url, body }) => {
        const response = await fetch(url, {
            method: body ? 'POST' : 'GET',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: body ? JSON.stringify(body) : undefined
        });
        if (!response.ok) throw new Error(`Autocomplete fixture request failed: ${response.status} ${url}`);
        const result = await response.json();
        if (result.error || result.fieldErrors?.length) throw new Error(JSON.stringify(result));
        return result;
    }, { url, body });
}

Scenario('Create isolated autocomplete candidates with hidden matches before valid pages', async ({ I }) => {
    const suffix = I.getRandomText();
    marker = `dashboard-autocomplete-autotest-${suffix}`;
    query = `${marker}-page`;
    const urlPrefix = `/autotest-${suffix}`;
    for (const type of ['hidden', 'visible']) {
        const folder = await request(I, '/admin/rest/groups/-1?groupId=0');
        Object.assign(folder, { groupId: -1, groupName: `${marker}-${type}`, navbarName: `${marker}-${type}`, hiddenInAdmin: type === 'hidden' });
        const created = await request(I, '/admin/rest/groups/editor', { action: 'create', data: { 0: folder } });
        I.assertEqual(created.data.length, 1, 'Each fixture folder must be saved.');
        folders.push(created.data[0]);
        const groupId = created.data[0].groupId;
        const template = await request(I, `/admin/rest/web-pages/-1?groupId=${groupId}`);
        const count = type === 'hidden' ? 20 : 21;
        const data = Object.fromEntries(Array.from({ length: count }, (_, index) => {
            const number = String(index).padStart(2, '0');
            const title = `${query} ${type === 'hidden' || index === 0 ? 'target' : 'extra'} ${type} ${number}`;
            const url = `${urlPrefix}-${type === 'hidden' ? 'h' : 'visible'}-${number}.html`;
            return [index, { ...template, id: -1, docId: -1, groupId, title, navbar: title,
                available: false, searchable: false, data: '<p>Autocomplete autotest</p>',
                generateUrlFromTitle: false, urlInheritGroup: false, virtualPath: url, editorVirtualPath: url }];
        }));
        const saved = await request(I, '/admin/rest/web-pages/editor', { action: 'create', data });
        pages[type] = saved.data;
        I.assertEqual(saved.data.length, count, 'Every autocomplete candidate must be saved.');
    }
});

/** Checks both an otherwise empty preview and the twenty eligible result limit. */
async function verifySuggestions(I) {
    I.assertEqual(pages.hidden.length, 20);
    I.assertEqual(pages.visible.length, 21);
    const endpoint = '/admin/skins/webjet6/_doc_autocomplete.jsp?docid=';
    const targetQuery = encodeURIComponent(`${query} target`);
    const legacy = await request(I, endpoint + targetQuery);
    I.assertDeepEqual(legacy.slice(0, 20).map(page => page.doc_id), pages.hidden.map(page => page.docId),
        'The legacy lookup must retain the twenty excluded candidates before the valid match.');
    I.assertEqual(legacy.length, 21, 'The valid candidate must exist beyond the old editable cutoff.');
    const target = await request(I, endpoint + targetQuery + '&editable=true');
    I.assertDeepEqual(target.map(page => page.doc_id), [pages.visible[0].docId],
        'Excluded candidates must not hide a later valid match.');
    const limited = await request(I, endpoint + encodeURIComponent(query) + '&editable=true');
    I.assertDeepEqual(limited.map(page => page.doc_id), pages.visible.slice(0, 20).map(page => page.docId),
        'The limit must count twenty eligible results while preserving URL order.');
}

Scenario('Hidden folders do not consume the dashboard autocomplete limit', async ({ I }) => {
    const hidden = await request(I, `/admin/rest/groups/${folders[0].groupId}`);
    I.assertTrue(hidden.hiddenInAdmin, 'The fixture must exercise a hidden folder.');
    await verifySuggestions(I);
});

Scenario('Trashed folders do not consume the dashboard autocomplete limit', async ({ I }) => {
    const id = folders[0].groupId;
    const folder = await request(I, `/admin/rest/groups/${id}`);
    folder.hiddenInAdmin = false;
    await request(I, '/admin/rest/groups/editor', { action: 'edit', data: { [id]: folder } });
    await request(I, '/admin/rest/groups/editor', { action: 'remove', data: { [id]: { groupId: id } } });
    const trashed = await request(I, `/admin/rest/groups/${id}`);
    I.assertFalse(trashed.hiddenInAdmin, 'Trash exclusion must work independently of hiddenInAdmin.');
    I.assertNotEqual(trashed.fullPath, folder.fullPath, 'The fixture folder must have moved to trash.');
    await verifySuggestions(I);
});

Scenario('Remove only the autocomplete fixture folders and pages', async ({ I }) => {
    for (const folder of folders) {
        I.assertTrue(folder.groupName.startsWith(marker), 'Cleanup must only remove this test fixture.');
        const current = await request(I, `/admin/rest/groups/${folder.groupId}`);
        I.assertEqual(current.groupName, folder.groupName, 'The saved fixture identity must still match.');
        const removals = current.parentGroupId === 0 ? 2 : 1;
        for (let attempt = 0; attempt < removals; attempt++) {
            await request(I, '/admin/rest/groups/editor', {
                action: 'remove', data: { [folder.groupId]: { groupId: folder.groupId } }
            });
        }
    }
    const remaining = await request(I, '/admin/skins/webjet6/_doc_autocomplete.jsp?docid=' + encodeURIComponent(query));
    I.assertEmpty(remaining, 'Fixture pages must also be removed from trash and the document cache.');
});
