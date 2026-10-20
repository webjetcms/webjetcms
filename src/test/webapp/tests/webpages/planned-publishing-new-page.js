Feature('webpages.planned-publishing-new-page @singlethread');

let title;
let originalEnabled;
let plannedDocId;
let legacyDocId;
const modal = '#datatableInit_modal';
const schedule = '#DTE_Field_editorFields-publishAfterStart_0';
const mediaTable = '#datatableFieldDTE_Field_editorFields-media';
const confirm = '#toast-container-webjet .toast[role="dialog"]';
const saveFirst = 'Uložiť nezverejnenú a pokračovať';

Before(({ I, login }) => {
    login('admin');
    if (!title) title = `autotest-planned-new-${I.getRandomText()}`;
});

Scenario('Enable planned publication for new pages', async ({ I, DT, Document }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    originalEnabled = await I.executeScript(async () => {
        const page = await window.$.get('/admin/rest/web-pages/-1?groupId=67');
        return page.editorFields.plannedPublishingEnabled;
    });
    Document.setConfigValue('plannedPublishingEnabled', 'true', true);
});

Scenario('Save a new private page and reopen planning with its persistent ID', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.fillField('title', title);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    await DTE.fillCkeditor(`<p>${title} content</p>`);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    const date = I.formatDateTime(Date.now() + 24 * 60 * 60 * 1000);
    DTE.fillField('publishStartDate', date);
    I.pressKey('Tab');

    // Cancelling must leave the unsaved page intact and scheduling unchecked.
    I.clickCss(schedule);
    I.waitForText(saveFirst, 10, confirm);
    I.see('Najskôr uložte novú stránku', confirm);
    I.saveScreenshot('planned-new-page-confirm.png');
    I.click('Zrušiť', confirm);
    I.waitForInvisible(confirm, 10);
    I.dontSeeCheckboxIsChecked(schedule);
    I.assertEqual(await I.executeScript(() => window.webpagesDatatable.EDITOR.currentJson.docId), -1);

    // A rejected save must not close the editor or change the original visibility setting.
    const saveRoute = '**/admin/rest/web-pages/editor*';
    await I.mockRoute(saveRoute, route => route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({error: 'autotest-save-failed', data: []})
    }));
    I.clickCss(schedule);
    I.waitForText(saveFirst, 10, confirm);
    I.clickCss(`${confirm} button.btn-primary`);
    I.waitForInvisible(confirm, 10);
    DTE.waitForLoader();
    I.waitForText('autotest-save-failed', 10, `${modal} .DTE_Form_Error`);
    I.assertEqual(await I.executeScript(() => window.webpagesDatatable.EDITOR.mode()), 'create');
    I.assertTrue(await I.executeScript(() => document.querySelector('#DTE_Field_available_0').checked));
    I.seeInField('#DTE_Field_publishStartDate', date);
    await I.stopMockingRoute(saveRoute);

    // Retry through the same action and record the real close/open transition.
    I.executeScript(() => {
        window.plannedNewPageClosed = false;
        window.webpagesDatatable.EDITOR.one('close', () => { window.plannedNewPageClosed = true; });
    });
    I.clickCss(schedule);
    I.waitForText(saveFirst, 10, confirm);
    I.clickCss(`${confirm} button.btn-primary`);
    I.waitForInvisible(confirm, 10);
    DTE.waitForLoader();
    DT.waitForLoader();
    I.waitForFunction(() => window.plannedNewPageClosed &&
        window.webpagesDatatable.EDITOR.mode() === 'edit' &&
        window.webpagesDatatable.EDITOR.currentJson.docId > 0, 20);
    DTE.waitForEditor();
    I.waitForText('Plánované médiá', 10, '#pills-dt-datatableInit-media-tab');
    I.seeCheckboxIsChecked(schedule);
    I.seeInField('#DTE_Field_publishStartDate', date);
    I.seeElement('#pills-dt-datatableInit-perex-tab.active');
    I.saveScreenshot('planned-new-page-reopened.png');
    I.assertContain(await I.executeScript(() => window.ckEditorInstance.getData()), `${title} content`);
    plannedDocId = await I.executeScript(() => window.webpagesDatatable.EDITOR.currentJson.docId);
    const saved = await I.executeScript(async ({id, marker}) => {
        const page = await window.$.get(`/admin/rest/web-pages/${id}`);
        const publicResponse = await fetch(`/showdoc.do?docid=${id}`, {credentials: 'omit'});
        return {available: page.available, plannedHistoryId: page.editorFields.plannedHistoryId, visible: (await publicResponse.text()).includes(marker)};
    }, {id: plannedDocId, marker: `${title} content`});
    I.assertFalse(saved.available);
    I.assertTrue(saved.plannedHistoryId == null || saved.plannedHistoryId < 1);
    I.assertFalse(saved.visible);

    // The file picker and nested media table must both use the newly assigned page ID.
    I.click(locate('.DTE_Field_Name_perexImage').find('button.btn-outline-secondary').first());
    I.waitForElement('#modalIframeIframeElement', 10);
    I.switchTo('#modalIframeIframeElement');
    I.waitForText('Obrázky - plánované', 15, '.elfinder-navbar-wrapper');
    I.waitForText('Súbory - plánované', 15, '.elfinder-navbar-wrapper');
    I.switchTo();
    I.clickCss('#modalIframe button.btn-close');
    I.waitForInvisible('#modalIframe', 10);

    I.clickCss('#pills-dt-datatableInit-media-tab');
    I.clickCss(`${mediaTable}_wrapper button.buttons-create`);
    DTE.waitForEditor('datatableFieldDTE_Field_editorFields-media');
    I.fillField('#DTE_Field_mediaTitleSk', `${title} media`);
    I.fillField('#DTE_Field_mediaLink', '/files/autotest-planned-existing.pdf');
    DTE.save('datatableFieldDTE_Field_editorFields-media');
    I.waitForText(`${title} media`, 10, mediaTable);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    DTE.save();
    I.waitForInvisible(modal, 10);

    DT.filterContains('title', title);
    I.seeNumberOfElements('#datatableInit tbody tr', 1);
    I.seeElement('#datatableInit tbody tr.is-not-public');
    const plan = await I.executeScript(async id => {
        const page = await window.$.get(`/admin/rest/web-pages/${id}`);
        const history = await window.$.get(`/admin/rest/web-pages/${id}?historyId=${page.editorFields.plannedHistoryId}`);
        return {available: page.available, historyId: page.editorFields.plannedHistoryId, data: history.data, publicable: history.publicable};
    }, plannedDocId);
    I.assertFalse(plan.available);
    I.assertTrue(plan.historyId > 0);
    I.assertTrue(plan.publicable);
    I.assertContain(plan.data, `${title} content`);
});

Scenario('Keep direct scheduling of new pages when planned publication is disabled', async ({ I, DT, DTE, Document }) => {
    Document.setConfigValue('plannedPublishingEnabled', 'false', true);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.fillField('title', `${title}-legacy`);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    await DTE.fillCkeditor(`<p>${title} legacy content</p>`);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    DTE.fillField('publishStartDate', I.formatDateTime(Date.now() + 24 * 60 * 60 * 1000));
    I.pressKey('Tab');
    I.checkOption(schedule);
    I.seeCheckboxIsChecked(schedule);
    I.dontSeeElement(confirm);
    I.dontSee('Plánované médiá', '#pills-dt-datatableInit-media-tab');
    I.assertEqual(await I.executeScript(() => window.webpagesDatatable.EDITOR.currentJson.docId), -1);
    I.executeScript(() => window.webpagesDatatable.EDITOR.one('submitSuccess', (event, json) => {
        window.plannedNewPageLegacy = json.data[0];
    }));
    DTE.save();
    I.waitForInvisible(modal, 10);
    const saved = await I.executeScript(() => {
        const page = window.plannedNewPageLegacy;
        return {docId: page.docId, historyId: page.historyId, publicable: page.publicable};
    });
    legacyDocId = saved.docId;
    I.assertTrue(legacyDocId > 0);
    I.assertTrue(saved.historyId > 0);
    I.assertTrue(saved.publicable);
    DT.filterContains('title', `${title}-legacy`);
    I.seeNumberOfElements('#datatableInit tbody tr', 1);
    I.seeElement('#datatableInit tbody tr.is-not-public');
    I.assertFalse(await I.executeScript(async ({id, marker}) => {
        const response = await fetch(`/showdoc.do?docid=${id}`, {credentials: 'omit'});
        return (await response.text()).includes(marker);
    }, {id: legacyDocId, marker: `${title} legacy content`}));
});

Scenario('Cleanup new planned pages and restore configuration', async ({ I, DT, Document }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
    DT.waitForLoader();
    DT.filterContains('title', title);
    DT.deleteAll();
    if (originalEnabled !== undefined) Document.setConfigValue('plannedPublishingEnabled', String(originalEnabled), true);
});
