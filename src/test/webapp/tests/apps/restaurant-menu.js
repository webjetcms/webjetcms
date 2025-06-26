Feature('apps.restaurant-menu');


Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie aplikácie - Reštauračné menu', async ({ I, Apps, Document }) => {
    Apps.insertApp('Reštauračné menu', '#components-restaurant_menu-title');

    const defaultParams = {
        mena: "&euro;",
        style: "01"
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText("Vyberte týždeň", 10);
    I.see("Tento týždeň nieje žiadne menu");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        mena: "$",
        style: "02"
    };

    I.fillField("#DTE_Field_mena", "$");
    I.clickCss(".image_radio_item > [for=DTE_Field_style_1]");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForText("Vyberte týždeň", 10);
    I.see("Tento týždeň nieje žiadne menu");
});