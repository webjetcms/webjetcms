Feature('webpages.editor-security');

let marker;
let allowed;
let forbidden;
let originalHistory;

Before(({ I, login }) => {
    login('admin');
    if (!marker) marker = `autotest-editor-security-${I.getRandomText()}`;
});

async function submit(I, action, page) {
    return I.executeScript(async ({ action, page }) => {
        const response = await fetch('/admin/rest/web-pages/editor?groupId=1', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: JSON.stringify({ action, data: { [page.id ?? 0]: page } })
        });
        return { status: response.status, body: await response.json() };
    }, { action, page });
}

async function readPage(I, id, historyId) {
    return I.executeScript(({ id, historyId }) => window.$.get(`/admin/rest/web-pages/${id}`, historyId ? { historyId } : {}), { id, historyId });
}

async function historyIds(I, id) {
    return I.executeScript(async id => {
        const result = await window.$.get(`/admin/rest/web-pages/history/all?docId=${id}`);
        return result.content.map(row => row.id).sort();
    }, id);
}

Scenario('Create disposable pages in permitted and forbidden folders', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=23');
    DT.waitForLoader();
    // tester2 has access to Test stavov (67), but not Uvodna stranka (23).
    for (const groupId of [67, 23]) {
        const page = await I.executeScript(id => window.$.get(`/admin/rest/web-pages/-1?groupId=${id}`), groupId);
        page.title = `${marker}-${groupId}`;
        page.navbar = page.title;
        page.data = `<p>${marker} original content</p>`;
        page.available = true;
        page.generateUrlFromTitle = true;
        page.editorFields.requestPublish = true;
        const response = await submit(I, 'create', page);
        I.assertFalse(Boolean(response.body.error), response.body.error || JSON.stringify(response.body.fieldErrors));
        const saved = response.body.data[0];
        I.assertAbove(saved.id, 0);
        if (groupId === 67) allowed = saved;
        else forbidden = saved;
    }
    originalHistory = await historyIds(I, forbidden.id);
});

Scenario('Reject a foreign page ID and an unauthorized move, but allow an ordinary edit', async ({ I, DT }) => {
    I.relogin('tester2');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    const editable = await readPage(I, allowed.id);
    I.assertEqual(editable.id, allowed.id);
    const inaccessible = await readPage(I, forbidden.id);
    I.assertTrue(Boolean(inaccessible.error), 'The foreign fixture must be outside the editor\'s permissions');

    for (const requestPublish of [false, true]) {
        const forged = JSON.parse(JSON.stringify(editable));
        forged.id = forbidden.id;
        forged.docId = forbidden.docId;
        forged.title = `${marker}-forged`;
        forged.data = `<p>${marker} forbidden overwrite</p>`;
        forged.editorFields.requestPublish = requestPublish;
        const response = await submit(I, 'edit', forged);
        I.assertEqual(response.status, 200);
        I.assertTrue(Boolean(response.body.error), 'A foreign page must be rejected even with a permitted destination');
        I.assertEmpty(response.body.data || []);
    }

    const move = JSON.parse(JSON.stringify(editable));
    move.groupId = forbidden.groupId;
    move.editorFields.groupDetails = forbidden.editorFields.groupDetails;
    const denied = await submit(I, 'edit', move);
    I.assertTrue(Boolean(denied.body.error), 'An editable page cannot be moved into a forbidden folder');

    editable.data = `<p>${marker} permitted edit</p>`;
    editable.editorFields.requestPublish = true;
    const saved = await submit(I, 'edit', editable);
    I.assertFalse(Boolean(saved.body.error));
    I.assertEqual(saved.body.data[0].id, allowed.id);
    allowed = saved.body.data[0];
    I.assertAbove(allowed.historyId, 0);
    I.assertContain(allowed.data, `${marker} permitted edit`);
});

Scenario('Rejected requests preserve the foreign page and its history', async ({ I, DT }) => {
    I.relogin('admin');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=23');
    DT.waitForLoader();
    const actual = await readPage(I, forbidden.id);
    for (const field of ['title', 'data', 'groupId', 'virtualPath', 'available']) {
        I.assertDeepEqual(actual[field], forbidden[field], `Rejected requests changed ${field}`);
    }
    I.assertDeepEqual(await historyIds(I, forbidden.id), originalHistory, 'Rejected requests must not create history');
    // tester2's ordinary edit may await approval; verify the version actually saved.
    const edited = await readPage(I, allowed.id, allowed.historyId);
    I.assertEqual(edited.groupId, allowed.groupId);
    I.assertContain(edited.data, `${marker} permitted edit`);
});

Scenario('Sanitize direct and legacy-encoded titles in the API, history and public HTML', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=23');
    DT.waitForLoader();
    for (const payload of [
        '<img src=x data-title-autotest="true" onerror="window.wjTitleXss=true">',
        '*|img src=x data-title-autotest="true" onerror="window.wjTitleXss=true"|*'
    ]) {
        const page = await readPage(I, forbidden.id);
        page.title = `${marker} ${payload} <strong>safe</strong>`;
        page.navbar = `${marker} <b onclick="window.wjTitleXss=true">menu</b>`;
        page.generateUrlFromTitle = false;
        page.urlInheritGroup = false;
        page.editorFields.requestPublish = true;
        const response = await submit(I, 'edit', page);
        I.assertFalse(Boolean(response.body.error));
        const saved = response.body.data[0];
        I.assertNotContain(saved.title, 'onerror');
        I.assertNotContain(saved.navbar, 'onclick');
        I.assertContain(saved.title, '<strong>safe</strong>');
        I.assertEqual(saved.virtualPath, forbidden.virtualPath);
        const history = await I.executeScript(({ id, historyId }) => window.$.get(`/admin/rest/web-pages/${id}?historyId=${historyId}`),
            { id: saved.id, historyId: saved.historyId });
        I.assertEqual(history.title, saved.title);
        I.assertEqual(history.navbar, saved.navbar);

        const rendered = await I.executeScript(async id => {
            const response = await fetch(`/showdoc.do?docid=${id}`, { credentials: 'omit' });
            const html = await response.text();
            const document = new DOMParser().parseFromString(html, 'text/html');
            const images = [...document.querySelectorAll('img[data-title-autotest]')];
            return {
                status: response.status,
                images: images.length,
                handlers: images.some(image => image.hasAttribute('onerror')),
                title: document.querySelector('h1')?.innerHTML || ''
            };
        }, saved.id);
        I.assertEqual(rendered.status, 200);
        I.assertAbove(rendered.images, 0, 'The public template must render the title fixture');
        I.assertFalse(rendered.handlers);
        I.assertContain(rendered.title, '<strong>safe</strong>');
    }
});

Scenario('Cleanup disposable security pages', async ({ I, DT }) => {
    I.relogin('admin');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=23');
    DT.waitForLoader();
    for (const fixture of [allowed, forbidden]) {
        if (!fixture) continue;
        const page = await readPage(I, fixture.id);
        const response = await submit(I, 'remove', page);
        I.assertFalse(Boolean(response.body.error));
    }
});
