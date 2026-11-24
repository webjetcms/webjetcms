Feature('webpages.pagebuilder-history');

const groupId = 34495;
const frame = '#DTE_Field_data-pageBuilderIframe';
let title;

Before(({ I, login }) => {
    login('admin');
    if (!title) title = 'autotest-pb-history-' + I.getRandomText();
});

Scenario('Restore a PageBuilder revision whose URL no longer resolves', async ({ I, DT, DTE, Document }) => {
    const legacyUrl = '/' + title + '/Stará správa.html';
    const restoredUrl = '/' + title + '/stara-sprava.html';
    const historicalText = title + ' historical content';
    const liveText = title + ' published content';

    Document.setEditorMode('standard');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=' + groupId);
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    DTE.fillField('title', title);
    DTE.fillField('navbar', title);
    I.uncheckOption('#DTE_Field_generateUrlFromTitle_0');
    DTE.fillField('virtualPath', '/' + title + '/live.html');
    I.clickCss('#pills-dt-datatableInit-content-tab');
    await DTE.fillCkeditor('<section><p><strong>' + historicalText + '</strong></p></section>');
    DTE.save();

    DT.filterEquals('title', title);
    I.click(title, '#datatableInit_wrapper');
    DTE.waitForEditor();
    const docId = Number(await I.grabAttributeFrom('#datatableInit tbody tr[id]', 'id'));
    I.clickCss('#pills-dt-datatableInit-content-tab');
    await DTE.fillCkeditor('<section><p>' + liveText + '</p></section>');
    DTE.save();
    I.click(title, '#datatableInit_wrapper');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-history-tab');
    const historyTable = '#datatableFieldDTE_Field_editorFields-history';
    DT.waitForLoader('datatableFieldDTE_Field_editorFields-history');
    I.seeNumberOfElements(historyTable + ' tbody tr[id]', 2);
    const historyId = Number(await I.grabAttributeFrom(historyTable + ' tbody tr:last-child', 'id'));
    I.assertAbove(docId, 0);
    I.assertAbove(historyId, 0);
    DTE.cancel();

    // Simulate a revision written by an older editor. Today's editor normalizes the URL on save.
    I.amOnPage('/admin/updatedb.jsp');
    I.fillField('#query', `UPDATE documents_history SET virtual_path='${legacyUrl}' WHERE doc_id=${docId} AND history_id=${historyId} AND title='${title}'`);
    I.clickCss('#querySubmit');
    I.waitForText('Total time:', 10);

    Document.resetPageBuilderMode();
    const oldStatus = await I.executeScript(async url => (await fetch(url)).status, legacyUrl);
    I.assertEqual(oldStatus, 404, 'The historical path must be unavailable before restoration');
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + docId + '&historyid=' + historyId);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.seeInField('#DTE_Field_virtualPath', legacyUrl);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.waitForFunction(([selector, id]) => {
        const win = document.querySelector(selector)?.contentWindow;
        return win?.pageBuilderReady === true && new URL(win.location.href).searchParams.get('historyid') === String(id);
    }, [frame, historyId], 20);
    I.switchTo(frame);
    I.waitForText(historicalText, 10, '#wjInline-docdata strong');
    I.dontSee(liveText, '#wjInline-docdata');
    I.switchTo();
    DTE.save();

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + docId);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.seeInField('#DTE_Field_virtualPath', restoredUrl);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.switchTo(frame);
    I.waitForText(historicalText, 10, '#wjInline-docdata strong');
    I.dontSee(liveText, '#wjInline-docdata');
    I.switchTo();
    DTE.cancel();
});

Scenario('Clean up the PageBuilder history page', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=' + groupId);
    DT.filterEquals('title', title);
    I.seeNumberOfElements('#datatableInit tbody tr[id]', 1);
    DT.deleteAll();
    I.amOnPage('/admin/v9/settings/redirect/');
    DT.filterEquals('oldUrl', '/' + title + '/live.html');
    if (await I.getTotalRows() > 0) DT.deleteAll('redirectTable');
});
