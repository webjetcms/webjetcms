Feature('webpages.delete-page-files');

const groupId = 59609;
const pagesUrl = '/admin/v9/webpages/web-pages-list/?groupid=' + groupId;
const modal = '#datatableInit_modal';
const checkbox = modal + ' .delete-page-files input';
const confirmButton = modal + ' .DTE_Form_Buttons button:not(.btn-close-editor)';
const cleanupSetting = 'editorDeleteUnusedPageFilesDefault';
let title, files, savedSetting, folderFixture;

Before(({ I, login }) => {
    files = [];
    savedSetting = undefined;
    folderFixture = null;
    login('admin');
    title = 'cleanup-autotest-' + I.getRandomText();
});

function openPages(I, DT, trash) {
    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    if (trash) {
        I.clickCss('#pills-trash-tab');
        DT.waitForLoader();
    }
}

async function selectFixturePages(I, DT, names) {
    if (names.length === 1) DT.filterEquals('title', names[0]);
    else DT.filterStartsWith('title', title);
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');
    const selected = await I.executeScript(() => webpagesDatatable.rows({selected: true}).data().toArray().map(page => ({title: page.title, path: page.fullPath})));
    I.assertDeepEqual(selected.map(page => page.title).sort(), names);
    return selected.map(page => page.path).sort();
}

async function fileCommand(I, command) {
    return I.executeScript(data => $.ajax({url: '/admin/elfinder-connector/', type: 'POST', dataType: 'json', data}),
        {volumes: 'link', groupId, ...command});
}

async function fileStatus(I, url) {
    return I.executeScript(async path => (await fetch(path, {cache: 'no-store'})).status, url);
}

function fileTarget(I, url) {
    return I.executeScript(data => $.getJSON('/admin/elFinder/gethash.jsp', data), {url, groupId, docId: -1});
}

async function remainingFiles(I, url, names) {
    const target = await fileTarget(I, url);
    const result = await fileCommand(I, {cmd: 'ls', target: target.hashParent, docId: -1, 'intersect[]': names});
    I.assertFalse(Boolean(result.error), JSON.stringify(result.error));
    return result.list.sort();
}

async function saveFixture(I, id, pageTitle, links = [], parentId = groupId) {
    const result = await I.executeScript(async ({id, groupId, pageTitle, links}) => {
        const page = await $.getJSON(`/admin/rest/web-pages/${id}?groupId=${groupId}`);
        page.title = pageTitle;
        page.navbar = pageTitle;
        page.data = links.map(file => `<p><a href="${file.url}">Download</a></p>`).join('') || '<p>Test</p>';
        page.available = true;
        page.generateUrlFromTitle = true;
        page.editorFields.requestPublish = true;
        return $.ajax({url: '/admin/rest/web-pages/editor?groupId=' + groupId, type: 'POST', contentType: 'application/json',
            data: JSON.stringify({action: id > 0 ? 'edit' : 'create', data: {[id]: page}})});
    }, {id, groupId: parentId, pageTitle, links});
    I.assertFalse(Boolean(result.error), result.error || JSON.stringify(result.fieldErrors));
    I.assertAbove(result.data[0].id, 0);
    return result.data[0].id;
}

async function createFiles(I, parentId, docId, pageTitle, names) {
    const context = {docId, groupId: parentId, title: pageTitle};
    const folder = await I.executeScript(data => $.getJSON('/admin/elFinder/gethash.jsp', data), {url: '/files:default', ...context});
    for (const name of names) {
        const result = await fileCommand(I, {...context, cmd: 'mkfile', target: folder.hashParent, name});
        I.assertFalse(Boolean(result.error), JSON.stringify(result.error));
        files.push({title: pageTitle, url: `${folder.uploadSubDir}/${result.added[0].name}`});
        I.assertEqual(await fileStatus(I, files.at(-1).url), 200);
    }
}

Scenario('Moving pages to Trash keeps their files and permanent deletion removes only unused files @screenshot', async ({ I, DT, DTE, Document, i18n }) => {
    openPages(I, DT, false);
    const ownerId = await saveFixture(I, -1, title);
    const survivorId = await saveFixture(I, -1, title + '-survivor');
    await createFiles(I, groupId, ownerId, title, ['program.txt', 'kontakt.txt']);
    await createFiles(I, groupId, survivorId, title + '-survivor', ['terminy.txt']);

    await saveFixture(I, ownerId, title, files.slice(0, 2));
    await saveFixture(I, survivorId, title + '-survivor', files.slice(1));

    openPages(I, DT, false);
    const originalPaths = await selectFixturePages(I, DT, [title, title + '-survivor']);
    I.click(DT.btn.delete_button);
    I.waitForEnabled(confirmButton, 30);
    I.dontSeeElement(checkbox);
    i18n.click('Delete::2', modal + ' .DTE_Form_Buttons');
    I.waitForInvisible(modal, 30);
    for (const file of files) I.assertEqual(await fileStatus(I, file.url), 200);

    openPages(I, DT, true);
    await selectFixturePages(I, DT, [title, title + '-survivor']);
    I.click(DT.btn.delete_button);
    const preview = modal + ' .delete-page-files';
    I.waitForVisible(locate(preview + ' li').withTextEquals(files[1].url), 120);
    const paths = await I.grabTextFromAll(preview + ' strong');
    I.assertDeepEqual(paths.sort(), originalPaths);
    const groups = locate(preview + ' [role=status] > div');
    for (const file of files) {
        const path = paths.find(path => path.endsWith('/' + file.title));
        I.see(file.url, groups.withChild(locate('strong').withTextEquals(path)));
    }
    I.seeCheckboxIsChecked(checkbox);
    Document.screenshotElement(modal + ' .modal-content', '/redactor/webpages/delete-page-files.png', 1280, 760);
    DTE.cancel();

    openPages(I, DT, true);
    await selectFixturePages(I, DT, [title]);
    I.click(DT.btn.delete_button);
    I.waitForVisible(checkbox);
    I.seeCheckboxIsChecked(checkbox);
    I.waitForVisible(locate(preview + ' li').withTextEquals(files[0].url), 120);
    I.dontSee(files[1].url, preview);
    i18n.click('Delete::2', modal + ' .DTE_Form_Buttons');
    i18n.waitForText('Errors checking or deleting files: 0', 120, '.toast-message');
    I.assertEqual(await fileStatus(I, files[0].url), 404);
    I.assertEqual(await fileStatus(I, files[1].url), 200);

    // The remaining fixture page is deleted before the invalid second row interrupts the batch.
    const interrupted = await I.executeScript(id => $.ajax({
        url: '/admin/rest/web-pages/editor', type: 'POST', contentType: 'application/json',
        data: JSON.stringify({action: 'remove', data: {[id]: {editorFields: {deleteUnusedPageFiles: true}}, '-1': {}}})
    }), survivorId);
    I.assertTrue(Boolean(interrupted.error));
    I.assertTrue(interrupted.notify.some(notice => notice.type === 'warning' && notice.text.includes(String(survivorId))));
    I.assertDeepEqual(await remainingFiles(I, files[2].url, ['terminy.txt']), []);
});

async function createFolder(I, parent, name) {
    const result = await I.executeScript(async ({parent, name}) => {
        const folder = await $.getJSON('/admin/rest/groups/-1', {groupId: parent});
        Object.assign(folder, {groupName: name, navbarName: name});
        return $.ajax({url: '/admin/rest/groups/editor', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({action: 'create', data: {0: folder}})});
    }, {parent, name});
    I.assertFalse(Boolean(result.error), result.error);
    I.assertEqual(result.data[0].parentGroupId, parent);
    return result.data[0];
}

async function removeFolder(I, folder) {
    const result = await I.executeScript(async ({groupId, groupName}) => {
        const current = await $.getJSON('/admin/rest/groups/' + groupId);
        if (current.groupName !== groupName) throw new Error('Unexpected fixture folder');
        return $.ajax({url: '/admin/rest/groups/editor', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({action: 'remove', data: {[groupId]: {groupId}}})});
    }, folder);
    I.assertFalse(Boolean(result.error), result.error);
}

for (const enabled of [true, false]) {
    Scenario(`Hard folder deletion with automatic file cleanup set to ${enabled} @singlethread`, async ({ I, DT, Document }) => {
        openPages(I, DT, false);
        const config = await I.executeScript(name => $.getJSON('/admin/rest/settings/configuration/autocomplete/detail', {name}), cleanupSetting);
        const current = String(config.displayValue ?? config.value);
        I.assertTrue(['true', 'false'].includes(current));
        if (current !== String(enabled)) {
            savedSetting = current;
            Document.setConfigValue(cleanupSetting, String(enabled), true);
        }
        folderFixture = await createFolder(I, groupId, title);
        const child = await createFolder(I, folderFixture.groupId, title + '-child');
        const pageId = await saveFixture(I, -1, title, [], child.groupId);
        await createFiles(I, child.groupId, pageId, title, ['program.txt', 'kontakt.txt']);
        await saveFixture(I, pageId, title, files, child.groupId);
        await saveFixture(I, -1, title + '-survivor', files.slice(1));

        await removeFolder(I, folderFixture);
        const trashed = await I.executeScript(id => $.getJSON('/admin/rest/groups/' + id), folderFixture.groupId);
        I.assertNotEqual(trashed.fullPath, folderFixture.fullPath);
        for (const file of files) I.assertEqual(await fileStatus(I, file.url), 200);

        await removeFolder(I, folderFixture);
        const children = await I.executeScript(id => $.getJSON('/admin/rest/web-pages/all', {groupId: id}), child.groupId);
        I.assertEmpty(children.content);
        folderFixture = null;
        const names = ['kontakt.txt', 'program.txt'];
        I.assertDeepEqual(await remainingFiles(I, files[0].url, names), enabled ? ['kontakt.txt'] : names);
    });
}

After(async ({ I, DT, Document }) => {
    if (!title) return;
    if (savedSetting !== undefined) Document.setConfigValue(cleanupSetting, savedSetting, true);
    openPages(I, DT, false);
    for (const file of files) {
        if (await fileStatus(I, file.url) === 200) {
            const target = await fileTarget(I, file.url);
            const result = await fileCommand(I, {docId: -1, cmd: 'rm', 'targets[]': target.hash});
            I.assertFalse(Boolean(result.error), JSON.stringify(result.error));
        }
    }
    if (folderFixture) {
        const current = await I.executeScript(id => $.getJSON('/admin/rest/groups/' + id), folderFixture.groupId);
        if (current.groupName === folderFixture.groupName) {
            if (current.parentGroupId === groupId) await removeFolder(I, folderFixture);
            await removeFolder(I, folderFixture);
        }
    }
    for (const trash of [false, true]) {
        openPages(I, DT, trash);
        DT.filterStartsWith('title', title);
        const error = await I.executeScript(async title => {
            const data = {};
            for (const page of webpagesDatatable.rows().data().toArray()) {
                if (page.title === title || page.title === title + '-survivor') data[page.id] = {};
            }
            if (!Object.keys(data).length) return null;
            const result = await $.ajax({url: '/admin/rest/web-pages/editor', type: 'POST', contentType: 'application/json',
                data: JSON.stringify({action: 'remove', data})});
            return result.error;
        }, title);
        I.assertFalse(Boolean(error), error);
    }
});
