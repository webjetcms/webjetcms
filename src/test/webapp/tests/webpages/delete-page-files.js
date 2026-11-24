Feature('webpages.delete-page-files');

const groupId = 59609;
const pagesUrl = '/admin/v9/webpages/web-pages-list/?groupid=' + groupId;
const modal = '#datatableInit_modal';
const checkbox = modal + ' .delete-page-files input';
const confirmButton = modal + ' .DTE_Form_Buttons button:not(.btn-close-editor)';
let title, files;

Before(({ I, login }) => {
    files = [];
    login('admin');
    title = 'cleanup-autotest-' + I.getRandomText();
});

async function selectPage(I, DT, name) {
    DT.filterEquals('title', name);
    const row = locate('#datatableInit tbody tr').withText(name).find('.dt-select-td');
    if (await I.grabNumberOfVisibleElements(row) !== 1) return false;
    I.click(row);
    I.assertDeepEqual(await I.executeScript(() => webpagesDatatable.rows({selected: true}).data().toArray().map(page => page.title)), [name]);
    return true;
}

function openPages(I, DT, trash) {
    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    if (trash) {
        I.clickCss('#pills-trash-tab');
        DT.waitForLoader();
    }
}

async function selectBothPages(I, DT) {
    DT.filterStartsWith('title', title);
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');
    const selected = await I.executeScript(() => webpagesDatatable.rows({selected: true}).data().toArray().map(page => ({title: page.title, path: page.fullPath})));
    I.assertDeepEqual(selected.map(page => page.title).sort(), [title, title + '-survivor']);
    return selected.map(page => page.path).sort();
}

async function fileCommand(I, command) {
    return I.executeScript(data => $.ajax({url: '/admin/elfinder-connector/', type: 'POST', dataType: 'json', data}),
        {volumes: 'link', groupId, ...command});
}

async function fileStatus(I, url) {
    return I.executeScript(async path => (await fetch(path, {cache: 'no-store'})).status, url);
}

async function saveFixture(I, id, pageTitle, links = []) {
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
    }, {id, groupId, pageTitle, links});
    I.assertFalse(Boolean(result.error), result.error || JSON.stringify(result.fieldErrors));
    I.assertAbove(result.data[0].id, 0);
    return result.data[0].id;
}

Scenario('Moving pages to Trash keeps their files and permanent deletion removes only unused files @screenshot', async ({ I, DT, DTE, Document, i18n }) => {
    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    const ownerId = await saveFixture(I, -1, title);
    const survivorId = await saveFixture(I, -1, title + '-survivor');
    for (const [docId, pageTitle, names] of [[ownerId, title, ['program.txt', 'kontakt.txt']], [survivorId, title + '-survivor', ['terminy.txt']]]) {
        const folder = await I.executeScript(data => $.getJSON('/admin/elFinder/gethash.jsp', data),
            {url: '/files:default', docId, groupId, title: pageTitle});
        for (const name of names) {
            const result = await fileCommand(I, {docId, title: pageTitle, cmd: 'mkfile', target: folder.hashParent, name});
            I.assertFalse(Boolean(result.error), JSON.stringify(result.error));
            files.push({docId, title: pageTitle, url: `${folder.uploadSubDir}/${result.added[0].name}`, hash: result.added[0].hash});
            I.assertEqual(await fileStatus(I, files.at(-1).url), 200);
        }
    }

    await saveFixture(I, ownerId, title, files.slice(0, 2));
    await saveFixture(I, survivorId, title + '-survivor', files.slice(1));

    openPages(I, DT, false);
    const originalPaths = await selectBothPages(I, DT);
    I.click(DT.btn.delete_button);
    I.waitForEnabled(confirmButton, 30);
    I.dontSeeElement(checkbox);
    i18n.click('Delete::2', modal + ' .DTE_Form_Buttons');
    I.waitForInvisible(modal, 30);
    for (const file of files) I.assertEqual(await fileStatus(I, file.url), 200);

    openPages(I, DT, true);
    await selectBothPages(I, DT);
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
    I.assertTrue(await selectPage(I, DT, title), 'The fixture page exists before deletion');
    I.click(DT.btn.delete_button);
    I.waitForVisible(checkbox);
    I.seeCheckboxIsChecked(checkbox);
    I.waitForVisible(locate(preview + ' li').withTextEquals(files[0].url), 120);
    I.dontSee(files[1].url, preview);
    i18n.click('Delete::2', modal + ' .DTE_Form_Buttons');
    i18n.waitForText('Errors checking or deleting files: 0', 120, '.toast-message');
    I.assertEqual(await fileStatus(I, files[0].url), 404);
    I.assertEqual(await fileStatus(I, files[1].url), 200);
});

After(async ({ I, DT }) => {
    if (!title) return;
    openPages(I, DT, false);
    for (const file of files) {
        if (await fileStatus(I, file.url) === 200) {
            const result = await fileCommand(I, {docId: file.docId, title: file.title, cmd: 'rm', 'targets[]': file.hash});
            I.assertFalse(Boolean(result.error), JSON.stringify(result.error));
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
