Feature('webpages.webpage-history-delete');

const historyTable = '#datatableFieldDTE_Field_editorFields-history';
const historyWrapper = historyTable + '_wrapper';
const deleteButton = historyWrapper + ' .buttons-history-remove';
let randomNumber, pageId;

Before(({ I, login }) => {
    login('admin');
    if (randomNumber === undefined) randomNumber = I.getRandomText();
});

async function openHistory(I, DTE, DT, removePermission = false) {
    I.assertTrue(Number.isInteger(pageId) && pageId > 0, 'Prepare the disposable page before testing its history');
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${pageId}${removePermission ? '&removePerm=cmp_data_deleting' : ''}`);
    DTE.waitForEditor();
    await I.clickIfVisible('.toast-close-button');
    I.clickCss('#pills-dt-datatableInit-history-tab');
    DT.waitForLoader(historyTable.substring(1));
    I.waitForVisible(historyTable);
}

function selectVersion(I, id) {
    I.clickCss(`${historyTable} tr[id="${id}"] td.dt-select-td`);
}

async function getVersions(I) {
    return I.executeScript(selector => $(selector).DataTable().rows().data().toArray(), historyTable);
}

async function requestDeletion(I, row) {
    return I.executeScript(async data => {
        const response = await fetch('/admin/rest/web-pages/history/' + data.id, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: JSON.stringify({ ...data, deleteAllowed: true, historyActual: false, publicable: true })
        });
        return { status: response.status, body: await response.json() };
    }, row);
}

Scenario('prepare disposable page versions', async ({ I, DTE, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
    I.createFolderStructure(randomNumber, false);
    I.createNewWebPage(randomNumber, false, false);
    const title = 'webPage-autotest-' + randomNumber;
    pageId = await I.executeScript(name => webpagesDatatable.rows().data().toArray().find(row => row.title === name).docId, title);
    I.assertAbove(pageId, 0, 'The disposable page must have a persisted ID');

    for (const version of [2, 3]) {
        I.click(title, '#datatableInit_wrapper');
        DTE.waitForEditor();
        await DTE.fillCkeditor(`<p>autotest history version ${version}</p>`);
        DTE.save();
    }

    I.click(title, '#datatableInit_wrapper');
    DTE.waitForEditor();
    await DTE.fillCkeditor('<p>autotest scheduled version</p>');
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    DTE.fillField('publishStartDate', I.formatDateTime(Date.now() + 86400000));
    I.pressKey('Tab');
    I.checkOption('#DTE_Field_editorFields-publishAfterStart_0');
    DTE.save();
    DT.waitForLoader();
});

Scenario('ordinary editor can cancel publication but cannot delete historical versions', async ({ I, DTE, DT }) => {
    await openHistory(I, DTE, DT, true);
    const rows = await getVersions(I);
    const old = rows.find(row => !row.historyActual && !row.publicable);
    const scheduled = rows.find(row => row.publicable && new Date(row.publishStartDate).getTime() > Date.now());
    I.assertTrue(Boolean(old && scheduled), 'Both historical and scheduled fixtures must exist');
    I.assertFalse(old.deleteAllowed, 'Historical cleanup requires the database cleanup permission');
    I.assertTrue(scheduled.deleteAllowed, 'Ordinary editors can still cancel future publication');

    selectVersion(I, old.id);
    I.seeElement(deleteButton + '.disabled');
    const denied = await requestDeletion(I, old);
    I.assertEqual(denied.status, 200, 'The history endpoint must handle the request');
    I.assertFalse(denied.body.result, 'Forged client flags must not authorize historical deletion');
    selectVersion(I, old.id);
    selectVersion(I, scheduled.id);
    I.seeElement(deleteButton + ':not(.disabled)');
    I.clickCss(deleteButton);
    I.waitForText('Vybrané verzie budú trvalo odstránené z histórie.', 10, 'div.DTE_Action_Remove');
    I.click('Zmazať', 'div.DTE_Action_Remove');
    DTE.waitForLoader();
    DT.waitForLoader(historyTable.substring(1));
    I.waitForInvisible(`${historyTable} tr[id="${scheduled.id}"]`);
    I.assertFalse((await getVersions(I)).some(row => row.id === scheduled.id), 'The cancelled schedule must disappear');
    DTE.cancel();
    I.logout();
});

Scenario('cleanup permission enables bulk deletion while protecting the current version', async ({ I, DTE, DT }) => {
    await openHistory(I, DTE, DT);
    const rows = await getVersions(I);
    const current = rows.find(row => row.historyActual);
    const old = rows.filter(row => !row.historyActual);
    I.assertEqual(old.length, 2, 'Two historical versions must be available for bulk cleanup');
    I.assertTrue(old.every(row => row.deleteAllowed), 'Cleanup permission must enable historical deletion');
    I.assertFalse(current.deleteAllowed, 'The currently published version must remain protected');

    selectVersion(I, current.id);
    I.seeElement(deleteButton + '.disabled');
    const denied = await requestDeletion(I, current);
    I.assertFalse(denied.body.result, 'The server must protect the current version despite forged client flags');
    for (const row of old) selectVersion(I, row.id);
    I.seeElement(deleteButton + '.disabled');
    selectVersion(I, current.id);
    I.seeElement(deleteButton + ':not(.disabled)');
    I.clickCss(deleteButton);
    I.waitForText('Po odstránení ich nebude možné obnoviť ani porovnať.', 10, 'div.DTE_Action_Remove');
    I.click('Zmazať', 'div.DTE_Action_Remove');
    DTE.waitForLoader();
    DT.waitForLoader(historyTable.substring(1));
    I.waitForText('Záznamy 1 až 1 z 1', 10, historyWrapper);
    I.assertDeepEqual((await getVersions(I)).map(row => row.id), [current.id], 'Only the published version must remain');
    DTE.cancel();

    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${pageId}`);
    DTE.waitForEditor();
    I.switchTo('.cke_wysiwyg_frame');
    I.see('autotest history version 3');
    I.switchTo();
    DTE.cancel();
});

Scenario('cleanup disposable page structure', ({ I }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.jstreeNavigate(['name-autotest-' + randomNumber]);
    I.deleteFolderStructure(randomNumber);
    I.logout();
});
