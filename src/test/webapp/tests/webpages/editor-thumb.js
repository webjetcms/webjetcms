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
    openThumbnailDialog(I, DTE, Document);

    const select = locate('.cke_dialog_ui_select').withText('Povolený rozmer').find('select');
    I.waitForVisible(select, 10);
    I.seeNumberOfVisibleElements('.cke_dialog select', 1);
    I.dontSeeElement('.cke_dialog input');

    const options = await I.grabTextFromAll(select.find('option'));
    I.assertDeepEqual(options.map(option => option.trim()), [
        '',
        '96 x 96 (0 - Maximálne rozmery)',
        '150 x 150 (1 - Fixná šírka)',
        '160 x 160 (5 - Centrovaný s pomerom strán - zmenšený)',
        '180 x 180 (0 - Maximálne rozmery)',
        '200 x 200 (5 - Centrovaný s pomerom strán - zmenšený)',
        '300 x 300 (5 - Centrovaný s pomerom strán - zmenšený)',
        '310 x 310 (1 - Fixná šírka)',
        '400 x 300 (3 - Fixná šírka a výška vyplnená farbou), Farba pozadia: #ff0000',
        '500 x 500 (4 - Fixná šírka a výška vyplnená farbou - centrované), Farba pozadia: #00ff00, Vypnúť bod záujmu',
        '730 x 401 (5 - Centrovaný s pomerom strán - zmenšený)'
    ], 'Allowed thumbnail sizes must have readable labels and numeric ordering, including the original image option');

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
