Feature('webpages.delete-page-files');

const groupId = 59609;
const pagesUrl = '/admin/v9/webpages/web-pages-list/?groupid=' + groupId;
const modal = '#datatableInit_modal';
const checkbox = modal + ' .delete-page-files input';
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

async function fileCommand(I, command) {
    return I.executeScript(data => $.ajax({url: '/admin/elfinder-connector/', type: 'POST', dataType: 'json', data}),
        {volumes: 'link', docId: -1, groupId, title, ...command});
}

async function fileStatus(I, url) {
    return I.executeScript(async path => (await fetch(path, {cache: 'no-store'})).status, url);
}

Scenario('Opting in deletes an unused file and keeps the file another page links to', async ({ I, DT, DTE }) => {
    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    const folder = await I.executeScript(data => $.getJSON('/admin/elFinder/gethash.jsp', data),
        {url: '/files:default', docId: -1, groupId, title});
    for (const role of ['unused', 'shared']) {
        const name = `${title}-${role}.txt`;
        const result = await fileCommand(I, {cmd: 'mkfile', target: folder.hashParent, name});
        I.assertFalse(Boolean(result.error), JSON.stringify(result.error));
        files.push({url: `${folder.uploadSubDir}/${name}`, hash: result.added[0].hash});
        I.assertEqual(await fileStatus(I, files.at(-1).url), 200);
    }

    for (const [pageTitle, links] of [[title, files], [title + '-survivor', files.slice(1)]]) {
        I.click(DT.btn.add_button);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        DTE.fillField('title', pageTitle);
        DTE.fillField('navbar', pageTitle);
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor(links.map(file => `<p><a href="${file.url}">Download</a></p>`).join(''));
        DTE.save();
    }

    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    I.assertTrue(await selectPage(I, DT, title), 'The fixture page exists before deletion');
    I.click(DT.btn.delete_button);
    I.waitForVisible(checkbox);
    I.dontSeeCheckboxIsChecked(checkbox);
    I.checkOption(checkbox);
    I.click('Zmazať', modal + ' .DTE_Form_Buttons');
    I.waitForText('Chyby pri kontrole alebo mazaní: 0', 120, '.toast-message');
    I.assertEqual(await fileStatus(I, files[0].url), 404);
    I.assertEqual(await fileStatus(I, files[1].url), 200);
});

After(async ({ I, DT }) => {
    if (!files?.length) return;
    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    for (const file of files) {
        if (await fileStatus(I, file.url) === 200) {
            const result = await fileCommand(I, {cmd: 'rm', 'targets[]': file.hash});
            I.assertFalse(Boolean(result.error), JSON.stringify(result.error));
        }
    }
    for (const name of [title, title + '-survivor']) {
        if (!(await selectPage(I, DT, name))) continue;
        I.click(DT.btn.delete_button);
        I.click('Zmazať', modal + ' .DTE_Form_Buttons');
        I.waitForInvisible(modal, 30);
    }
});
