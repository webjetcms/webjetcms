Feature('webpages.editor-thumb');

Before(({ I, login }) => {
    I.switchTo();
    login('admin');
});

function openThumbnailDialog(I, DTE, Document) {
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
    DTE.waitForEditor();
    I.waitForElement('#DTE_Field_data-pageBuilderIframe', 10);
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    const image = locate('div').withChild(locate('h3').withText('Etiam orci')).find('img');
    I.waitForVisible(image, 10);
    I.click(image);
    I.waitForVisible('.cke_dialog', 10);
    I.click(locate('.cke_dialog_tab').withText('Miniatúra'));
}

Scenario('strict mode shows allowed thumbnail sizes @singlethread', async ({ I, DTE, Document }) => {
    Document.setConfigValue('thumbServletAllowedSizeMode', 'strict');
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
        '300x200ip4ncffff00',
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
    ].join('\n'));
    openThumbnailDialog(I, DTE, Document);

    const select = locate('.cke_dialog_ui_select').withText('Povolený rozmer').find('select');
    I.waitForVisible(select, 10);
    I.seeNumberOfVisibleElements('.cke_dialog select', 1);
    I.dontSeeElement('.cke_dialog input');

    const options = (await I.grabTextFromAll(select.find('option'))).map(option => option.trim());
    I.assertEqual(options[0], '', 'The first thumbnail option must keep the original image');
    const expectedOptions = [
        '96 x 96 (0 - Maximálne rozmery)',
        '150 x 150 (1 - Fixná šírka)',
        '400 x 300 (3 - Fixná šírka a výška vyplnená farbou), Farba pozadia: #ff0000',
        '500 x 500 (4 - Fixná šírka a výška vyplnená farbou - centrované), Farba pozadia: #00ff00, Vypnúť bod záujmu'
    ];
    for (const option of expectedOptions) {
        I.assertContain(options, option, 'Representative thumbnail sizes must have readable labels');
    }

    I.amAcceptingPopups();
    I.clickCss('.cke_dialog_ui_button_cancel');
    I.switchTo();
    DTE.cancel();
    if (await I.grabPopupText()) I.acceptPopup();
});

// Restore the shared configuration even if the strict-mode scenario fails.
Scenario('restore learn mode and free thumbnail fields @singlethread', async ({ I, DTE, Document }) => {
    Document.setConfigValue('thumbServletAllowedSizeMode', 'learn');
    openThumbnailDialog(I, DTE, Document);

    I.dontSeeElement(locate('.cke_dialog_ui_select').withText('Povolený rozmer'));
    I.seeElement(locate('.cke_dialog_ui_select').withText('Režim').find('select'));
    I.seeElement(locate('.cke_dialog_ui_text').withText('Šírka').find('input'));
    I.seeElement(locate('.cke_dialog_ui_text').withText('Výška').find('input'));
    I.seeElement(locate('.cke_dialog_ui_checkbox').withText('Vypnúť bod záujmu').find('input'));

    I.amAcceptingPopups();
    I.clickCss('.cke_dialog_ui_button_cancel');
    I.switchTo();
    DTE.cancel();
    if (await I.grabPopupText()) I.acceptPopup();
});
