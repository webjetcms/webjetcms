Feature('webpages.planned-publishing @singlethread');

let title;
let docId;
let privateImage;
let plannedHistoryId;
let draftHistoryId;
const mediaTable = '#datatableFieldDTE_Field_editorFields-media';

Before(({ I, login }) => {
    login('admin');
    if (!title) title = `autotest-planned-${I.getRandomText()}`;
});

Scenario('Enable private planned publication', ({ Document }) => {
    Document.setConfigValue('plannedPublishingEnabled', 'true', true);
});

Scenario('Keep live content and an ordinary draft while preparing media', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.fillField('title', title);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    await DTE.fillCkeditor(`<p>${title} live</p>`);
    DTE.save();
    I.dontSeeElement('.DTE_Form_Error:not([style*="display: none"])');
    DT.filterContains('title', title);
    I.click(title, '#datatableInit_wrapper');
    DTE.waitForEditor();
    docId = await I.executeScript(() => window.webpagesDatatable.EDITOR.currentJson.docId);

    await DTE.fillCkeditor(`<p>${title} ordinary draft</p>`);
    I.checkOption('#webpagesSaveCheckbox');
    I.executeScript(() => window.webpagesDatatable.EDITOR.one('submitSuccess', (event, json) => {
        window.plannedTestDraftHistoryId = json.data[0].historyId;
    }));
    I.clickCss('#datatableInit_modal .DTE_Form_Buttons button.btn-primary');
    DTE.waitForLoader();
    draftHistoryId = await I.executeScript(() => window.plannedTestDraftHistoryId);
    I.assertTrue(draftHistoryId > 0);
    DTE.cancel();
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
    DTE.waitForEditor();
    await DTE.fillCkeditor(`<p>${title} future</p>`);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    DTE.fillField('publishStartDate', I.formatDateTime(Date.now() + 24 * 60 * 60 * 1000));
    I.pressKey('Tab');
    I.checkOption('#DTE_Field_editorFields-publishAfterStart_0');
    I.waitForText('Súbory a obrázky nahrávajte do plánovaných priečinkov.', 10, '.planned-change-notice');
    I.waitForText('Plánované médiá', 10, '#pills-dt-datatableInit-media-tab');
    I.click(locate('.DTE_Field_Name_perexImage').find('button.btn-outline-secondary').first());
    I.waitForElement('#modalIframeIframeElement', 10);
    I.switchTo('#modalIframeIframeElement');
    I.waitForText('Obrázky - plánované', 15, '.elfinder-navbar-wrapper');
    I.waitForElement(locate('.elfinder-navbar-dir.ui-state-active').withText('Obrázky'), 20);
    I.waitForInvisible('.elfinder-notify', 10);
    I.click(locate('.elfinder-navbar-dir').withText('Obrázky - plánované'));
    I.waitForElement(locate('.elfinder-navbar-dir.ui-state-active').withText('Obrázky - plánované'), 10);
    I.click('.elfinder-button-icon-mkdir');
    I.fillField('.elfinder-cwd-filename[title="Nový priečinok"] textarea', title);
    I.pressKey('Enter');
    I.waitForText(title, 10, '.elfinder-cwd-wrapper');
    I.doubleClick(locate('.elfinder-cwd-filename').withText(title));
    I.waitForElement(locate('.elfinder-navbar-dir.ui-state-active').withText(title), 10);
    I.click('.elfinder-button-icon-upload');
    I.attachFile('input[type=file]', 'tests/webpages/tree.webp');
    I.waitForElement('.elfinder-cwd-filename[title="tree.webp"]', 20);
    I.click('.elfinder-cwd-filename[title="tree.webp"]');
    I.switchTo();
    I.click(locate('#modalIframe button.btn-primary').withText('Potvrdiť'));
    I.waitForInvisible('#modalIframe', 10);
    privateImage = await I.grabValueFrom('.DTE_Field_Name_perexImage input.form-control');
    I.assertContain(privateImage, `/files/protected/planned-changes/${docId}/images/`);
    const warm = await I.executeScript(async url => {
        const response = await fetch('/thumb' + url + '?w=120&h=120');
        return {status: response.status, cache: response.headers.get('Cache-Control')};
    }, privateImage);
    I.assertEqual(warm.status, 200);
    I.assertEqual(warm.cache, 'no-store, no-cache, must-revalidate, max-age=0');
    for (const url of [privateImage, '/thumb' + privateImage + '?w=120&h=120']) {
        const denied = await I.executeScript(async path => {
            const response = await fetch(path, {credentials: 'omit'});
            return {status: response.status, cache: response.headers.get('Cache-Control')};
        }, url);
        I.assertEqual(denied.status, 404);
        I.assertEqual(denied.cache, 'no-store, no-cache, must-revalidate, max-age=0');
    }
    I.clickCss('#pills-dt-datatableInit-media-tab');
    I.clickCss(`${mediaTable}_wrapper button.buttons-create`);
    DTE.waitForEditor('datatableFieldDTE_Field_editorFields-media');
    I.fillField('#DTE_Field_mediaTitleSk', `${title} private media`);
    I.fillField('#DTE_Field_mediaLink', '/files/autotest-planned-existing.pdf');
    I.clickCss(`${mediaTable}_modal button.btn-primary`);
    DTE.waitForLoader('datatableFieldDTE_Field_editorFields-media');
    DT.waitForLoader();
    I.see(`${title} private media`, mediaTable);
    DTE.save();
    I.dontSeeElement('.DTE_Form_Error:not([style*="display: none"])');

    I.amOnPage(`/showdoc.do?docid=${docId}`);
    I.see(`${title} live`);
    I.dontSee(`${title} future`);
    I.dontSee(`${title} private media`);
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
    DTE.waitForEditor();
    await DTE.fillCkeditor(`<p>${title} live correction</p>`);
    DTE.save();
    I.amOnPage(`/showdoc.do?docid=${docId}`);
    I.see(`${title} live correction`);
    I.dontSee(`${title} future`);
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-media-tab');
    I.dontSee(`${title} private media`, mediaTable);
    I.clickCss(`${mediaTable}_wrapper button.buttons-create`);
    DTE.waitForEditor('datatableFieldDTE_Field_editorFields-media');
    I.fillField('#DTE_Field_mediaTitleSk', `${title} live media`);
    I.fillField('#DTE_Field_mediaLink', '/files/autotest-current.pdf');
    I.clickCss(`${mediaTable}_modal button.btn-primary`);
    DTE.waitForLoader('datatableFieldDTE_Field_editorFields-media');
    I.waitForText(`${title} live media`, 10, mediaTable);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    I.click('Otvoriť plánovanú verziu', '.planned-change-notice');
    I.waitForText('Plánované médiá', 10, '#pills-dt-datatableInit-media-tab');
    I.clickCss('#pills-dt-datatableInit-media-tab');
    I.waitForText(`${title} private media`, 10, mediaTable);
    I.seeNumberOfElements(`${mediaTable} tbody tr`, 1);
    I.dontSee(`${title} live media`, mediaTable);
    plannedHistoryId = await I.executeScript(() => window.webpagesDatatable.EDITOR.field('historyId').val());
    const historyResponse = await I.executeScript(async ({id, history}) => {
        const response = await fetch(`/showdoc.do?docid=${id}&historyid=${history}`, {credentials: 'omit'});
        return {status: response.status, cache: response.headers.get('Cache-Control')};
    }, {id: docId, history: plannedHistoryId});
    I.assertEqual(historyResponse.status, 404);
    I.assertEqual(historyResponse.cache, 'no-store, no-cache, must-revalidate, max-age=0');
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    DTE.fillField('publishStartDate', I.formatDateTime(Date.now() + 15000));
    I.pressKey('Tab');
    DTE.save();
    I.dontSeeElement('.DTE_Form_Error:not([style*="display: none"])');
    I.amOnPage('about:blank');
    I.wait(45);
    I.wait(35);
    I.amOnPage(`/showdoc.do?docid=${docId}`);
    I.waitForText(`${title} future`, 20);
    I.dontSee(`${title} live`);
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-media-tab');
    I.waitForText(`${title} private media`, 10, mediaTable);
    I.seeNumberOfElements(`${mediaTable} tbody tr`, 1);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    const publishedImage = await I.executeScript(() => {
        const decoder = document.createElement('textarea');
        decoder.innerHTML = document.querySelector('.DTE_Field_Name_perexImage input.form-control').value;
        return decoder.value;
    });
    I.assertEqual(publishedImage, privateImage.replace(`/files/protected/planned-changes/${docId}`, ''));
    const imageStatus = await I.executeScript(async url => (await fetch(url, {credentials: 'omit'})).status, publishedImage);
    I.assertEqual(imageStatus, 200);
    I.executeScript(({id, history}) => window.editFromHistory(id, history), {id: docId, history: draftHistoryId});
    I.waitForFunction(expected => window.ckEditorInstance.getData().includes(expected), [`${title} ordinary draft`], 10);
    DTE.cancel();
});

Scenario('Deny guessed private asset URLs to anonymous visitors', async ({ I }) => {
    I.logout();
    for (const prefix of ['', '/thumb']) {
        const response = await I.executeScript(async path => {
            const result = await fetch(path, {credentials: 'omit'});
            return {status: result.status, cache: result.headers.get('Cache-Control')};
        }, `${prefix}${privateImage || `/files/protected/planned-changes/${docId}/images/secret.webp`}`);
        I.assertEqual(response.status, 404);
        I.assertEqual(response.cache, 'no-store, no-cache, must-revalidate, max-age=0');
    }
});

Scenario('Cleanup planned publication page and configuration', async ({ I, DT, DTE, Document }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    DT.filterContains('title', title);
    I.clickCss('#datatableInit_wrapper button.buttons-select-all');
    I.clickCss('#datatableInit_wrapper button.buttons-remove');
    DTE.waitForEditor();
    I.click('Zmazať', '#datatableInit_modal');
    DTE.waitForLoader();
    await Document.setConfigValue('plannedPublishingEnabled', 'false', true);
    if (docId > 0 && privateImage && title.startsWith('autotest-planned-')) {
        const fs = require('node:fs');
        const path = require('node:path');
        const webRoot = path.resolve(__dirname, '../../../../main/webapp');
        fs.rmSync(path.join(webRoot, 'files/protected/planned-changes', String(docId)), {recursive: true, force: true});
        fs.rmSync(path.join(webRoot, 'images/test-stavov', title), {recursive: true, force: true});
    }
});
