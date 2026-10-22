Feature('webpages.editor-thumb');

let originalConfig;
const sizeSelect = locate('.cke_dialog_ui_select').withText('Rozmer miniatúry').find('select');
const modeSelect = locate('.cke_dialog_ui_select').withText('Režim').find('select');

Before(({ I, login }) => {
    I.switchTo();
    login('admin');
});

function openThumbnailDialog(I, DTE, Document, thumbnail = true) {
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
    DTE.waitForEditor();
    I.waitForElement('#DTE_Field_data-pageBuilderIframe', 10);
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    const image = locate('div').withChild(locate('h3').withText('Etiam orci')).find('img');
    I.waitForVisible(image, 10);
    I.click(image);
    I.waitForVisible('.cke_dialog', 10);
    if (thumbnail) I.click(locate('.cke_dialog_tab').withText('Miniatúra'));
}

function withoutCacheVersion(src) {
    const url = new URL(src, 'http://iwcm.interway.sk');
    url.searchParams.delete('v');
    return url.href;
}

async function selectSize(I, value) {
    const [label] = await I.grabTextFromAll(sizeSelect.find(`option[value="${value}"]`));
    I.selectOption(sizeSelect, label.trim());
}

async function closeImageDialog(I, DTE) {
    I.amAcceptingPopups();
    I.clickCss('.cke_dialog_ui_button_cancel');
    I.switchTo();
    DTE.cancel();
    if (await I.grabPopupText()) I.acceptPopup();
}

Scenario('capture thumbnail configuration @singlethread', async ({ I, DTE, Document }) => {
    // Refresh cached property files and the active language before reading new labels.
    I.amOnPage('/admin/v9/?userlngr=true');
    I.amOnPage('/admin/v9/?userlngr=true');
    I.executeScript(() => localStorage.removeItem('translate'));
    openThumbnailDialog(I, DTE, Document, false);
    originalConfig = await I.executeScript(() => {
        const config = CKEDITOR.dialog.getCurrent().getParentEditor().config;
        return { mode: config.thumbServletAllowedSizeMode, sizes: config.thumbServletAllowedSizes };
    });
    await closeImageDialog(I, DTE);
});

Scenario('strict mode shows allowed thumbnail sizes @singlethread', async ({ I, DTE, Document }) => {
    Document.setConfigValue('thumbServletAllowedSizeMode', 'strict', true);
    Document.setConfigValue('thumbServletAllowedSizes', [
        '100x100ip5',
        '1200x1200ip1',
        '1280x1280ip1',
        '150x150ip1',
        '160x160ip5',
        '200x200',
        '200x200ip1',
        '200x200ip2',
        '200x200ip5',
        '200x200ip6',
        '265x225ip5',
        '300x200ip3',
        '300x200ip4cffff00',
        '300x200ip4ncffff00q90',
        '300x200ip5',
        '300x300ip5',
        '300x300ip5q80',
        '300x400ip5',
        '310x310ip1',
        '350x250ip4',
        '36x36ip5',
        '370x330ip4',
        '400x300ip3cff0000',
        '400x300ip5',
        '400x300ip6',
        '400x400ip4cffff00',
        '400x400ip5',
        '445x360ip5',
        '480x96',
        '490x96',
        '500x400ip5',
        '500x500ip4nc00ff00',
        '540x226ip5',
        '600x400ip4nq90',
        '60x60ip4',
        '700x400ip6',
        '70x70ip5',
        '730x400ip5',
        '730x401ip5',
        '800x320ip5',
        '900x360ip5',
        '96x96'
    ].join('\n'), true);
    openThumbnailDialog(I, DTE, Document);

    const select = sizeSelect;
    I.waitForVisible(select, 10);
    I.seeNumberOfVisibleElements('.cke_dialog select', 1);
    I.dontSeeElement('.cke_dialog input');

    const options = (await I.grabTextFromAll(select.find('option'))).map(option => option.trim());
    I.assertEqual(options[0], 'Nepoužiť miniatúru', 'The first thumbnail option must keep the original image');
    const expectedOptions = [
        '96 × 96 px – Maximálne rozmery (ip0)',
        '150 px – Fixná šírka (ip1)',
        '400 × 300 px – Fixná šírka a výška vyplnená farbou (ip3), #ff0000',
        '500 × 500 px – Fixná šírka a výška vyplnená farbou - centrované (ip4), #00ff00, Vypnúť bod záujmu'
    ];
    for (const option of expectedOptions) {
        I.assertContain(options, option, 'Representative thumbnail sizes must have readable labels');
    }

    I.assertEqual(await I.grabValueFrom(select), '160x160ip5', 'The existing thumbnail must select the matching preset');
    await selectSize(I, '300x200ip4ncffff00q90');
    I.clickCss('.cke_dialog_ui_button_ok');
    I.waitForInvisible('.cke_dialog', 10);
    const image = locate('div').withChild(locate('h3').withText('Etiam orci')).find('img');
    const src = await I.grabAttributeFrom(image, 'src');
    I.assertContain(src, '/thumb/', 'The selected preset must generate a thumbnail URL');
    for (const param of ['w=300', 'h=200', 'ip=4', 'c=ffff00', 'noip=true', 'q=90']) I.assertContain(src, param, 'The complete preset must survive confirmation');
    I.click(image);
    I.waitForVisible('.cke_dialog', 10);
    I.click(locate('.cke_dialog_tab').withText('Miniatúra'));
    I.assertEqual(await I.grabValueFrom(select), '300x200ip4ncffff00q90', 'Reopening must preserve every preset parameter');
    I.saveScreenshot('autotest-thumbnail-presets.png');
    await selectSize(I, '');
    I.clickCss('.cke_dialog_ui_button_ok');
    I.waitForInvisible('.cke_dialog', 10);
    I.assertNotContain(await I.grabAttributeFrom(image, 'src'), '/thumb/', 'Choosing the original must remove thumbnail processing');
    I.assertNotContain(await I.grabAttributeFrom(image, 'class'), 'fixedSize-', 'Choosing the original must remove the size class');
    I.doubleClick(image);
    I.waitForVisible('.cke_dialog', 10);
    await closeImageDialog(I, DTE);
});

for (const mode of ['learn', 'check', 'allow']) {
    Scenario(`${mode} offers presets and custom sizes @singlethread`, async ({ I, DTE, Document }) => {
        Document.setConfigValue('thumbServletAllowedSizeMode', mode, true);
        openThumbnailDialog(I, DTE, Document);
        I.waitForVisible(sizeSelect, 10);
        I.dontSeeElement(modeSelect);
        await selectSize(I, 'custom');
        I.waitForVisible(modeSelect, 10);
        I.selectOption(modeSelect, '2 - Fixná výška');
        I.seeElement(locate('.cke_dialog_ui_text').withText('Výška').find('input'));
        I.dontSeeElement(locate('.cke_dialog_ui_text').withText('Šírka').find('input'));
        if (mode === 'allow') {
            I.fillField(locate('.cke_dialog_ui_text').withText('Výška').find('input'), '97');
            I.clickCss('.cke_dialog_ui_button_ok');
            I.waitForInvisible('.cke_dialog', 10);
            const image = locate('div').withChild(locate('h3').withText('Etiam orci')).find('img');
            I.assertContain(await I.grabAttributeFrom(image, 'src'), 'h=97', 'Custom dimensions must be written to the URL');
            I.click(image);
            I.waitForVisible('.cke_dialog', 10);
            I.click(locate('.cke_dialog_tab').withText('Miniatúra'));
            I.assertEqual(await I.grabValueFrom(sizeSelect), 'custom', 'An existing size outside the list must reopen as custom');
            I.seeInField(locate('.cke_dialog_ui_text').withText('Výška').find('input'), '97');
        }
        await selectSize(I, '200x200ip6');
        I.dontSeeElement(modeSelect);
        await closeImageDialog(I, DTE);
    });
}

Scenario('strict rejects existing sizes outside the list @singlethread', async ({ I, DTE, Document }) => {
    Document.setConfigValue('thumbServletAllowedSizeMode', 'strict', true);
    Document.setConfigValue('thumbServletAllowedSizes', '', true);
    openThumbnailDialog(I, DTE, Document);
    I.assertEqual(await I.grabValueFrom(sizeSelect), 'unavailable', 'An unsupported existing size must require an explicit choice');
    I.amAcceptingPopups();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.seeInPopup('Vyberte povolený rozmer miniatúry');
    I.acceptPopup();
    I.seeElement('.cke_dialog');
    await selectSize(I, '');
    I.clickCss('.cke_dialog_ui_button_ok');
    I.waitForInvisible('.cke_dialog', 10);
    I.doubleClick(locate('div').withChild(locate('h3').withText('Etiam orci')).find('img'));
    I.waitForVisible('.cke_dialog', 10);
    I.click(locate('.cke_dialog_tab').withText('Miniatúra'));
    I.seeNumberOfElements(sizeSelect.find('option'), 1);
    await closeImageDialog(I, DTE);
});

Scenario('empty list keeps custom fields outside strict mode @singlethread', async ({ I, DTE, Document }) => {
    Document.setConfigValue('thumbServletAllowedSizeMode', 'allow', true);
    openThumbnailDialog(I, DTE, Document);
    I.dontSeeElement(sizeSelect);
    I.seeElement(modeSelect);
    I.seeElement(locate('.cke_dialog_ui_text').withText('Šírka').find('input'));
    await closeImageDialog(I, DTE);
});

Scenario('deny hides the tab and preserves existing thumbnail @singlethread', async ({ I, DTE, Document }) => {
    Document.setConfigValue('thumbServletAllowedSizeMode', 'deny', true);
    openThumbnailDialog(I, DTE, Document, false);
    I.dontSeeElement(locate('.cke_dialog_tab').withText('Miniatúra'));
    const image = locate('div').withChild(locate('h3').withText('Etiam orci')).find('img');
    const before = await I.grabAttributeFrom(image, 'src');
    const classes = await I.grabAttributeFrom(image, 'class');
    I.clickCss('.cke_dialog_ui_button_ok');
    I.waitForInvisible('.cke_dialog', 10);
    I.assertEqual(withoutCacheVersion(await I.grabAttributeFrom(image, 'src')), withoutCacheVersion(before), 'Deny must preserve thumbnail parameters apart from the editor cache version');
    I.assertEqual(await I.grabAttributeFrom(image, 'class'), classes, 'Deny must preserve the existing thumbnail class');
    I.click(image);
    I.waitForVisible('.cke_dialog', 10);
    await closeImageDialog(I, DTE);
});

Scenario('restore thumbnail configuration @singlethread', ({ Document }) => {
    if (!originalConfig) return;
    Document.setConfigValue('thumbServletAllowedSizeMode', originalConfig.mode, true);
    Document.setConfigValue('thumbServletAllowedSizes', originalConfig.sizes, true);
});
