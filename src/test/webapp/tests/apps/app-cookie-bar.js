Feature('apps.app-cookie-bar');


Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - CookieBar', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Cookie lišta', '#components-app-cookiebar-title');

    const defaultParams = {
        checkbox_title: 'true',
        cookie_title: '',
        cookie_text: '',
        cookie_ButtonText: '',
        cookie_ButtonTextDecline: '',
        showLink: 'false',
        color_background: '',
        color_title: '',
        color_text: '',
        color_button: '',
        color_buttonText: '',
        position: 'top',
        padding_top: '25',
        padding_bottom: '25',
        device: '',
        cacheMinutes: ''
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see(defaultParams.cookie_title);
    I.see(defaultParams.cookie_text);
    I.see(defaultParams.cookie_ButtonText);
    I.see(defaultParams.cookie_ButtonTextDecline);
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1', 'background-color')), '#e2e3e5');
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 h2', 'color')), '#383d41');
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 p', 'color')), '#383d41');
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 button.btn.btn-primary.cookie-btn', 'background-color')), '#00a3e0');
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 button.btn.btn-primary.cookie-btn', 'color')), '#ffffff');
    I.assertEqual(await I.grabCssPropertyFrom('#cookiebar-1 div.container', 'padding-top'), `${defaultParams.padding_top}px`);
    I.assertEqual(await I.grabCssPropertyFrom('#cookiebar-1 div.container', 'padding-bottom'), `${defaultParams.padding_bottom}px`);

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        checkbox_title: 'true',
        cookie_title: 'Používanie cookies-chan.ge',
        cookie_text: 'Náš web používa súbory cookies, ktoré sú potrebné k jeho fungovaniu-chan.ge',
        cookie_ButtonText: 'Akceptovať-chan.ge',
        cookie_ButtonTextDecline: 'Zamietnuť-chan.ge',
        showLink: 'true',
        color_background: '#f0f4f8',
        color_title: '#2c3e50',
        color_text: '#34495e',
        color_button: '#27ae60',
        color_buttonText: '#ffffff',
        position: 'bottom',
        padding_top: '30',
        padding_bottom: '30',
        device: '',
        cacheMinutes: ''
    };

    DTE.fillField('cookie_title', changedParams.cookie_title);
    DTE.fillField('cookie_text', changedParams.cookie_text);
    DTE.fillField('cookie_ButtonText', changedParams.cookie_ButtonText);
    DTE.fillField('cookie_ButtonTextDecline', changedParams.cookie_ButtonTextDecline);
    DTE.clickSwitch('showLink_0');
    Apps.switchToTabByIndex(1);
    DTE.fillField('color_background', changedParams.color_background);
    DTE.fillField('color_title', changedParams.color_title);
    DTE.fillField('color_text', changedParams.color_text);
    DTE.fillField('color_button', changedParams.color_button);
    DTE.fillField('color_buttonText', changedParams.color_buttonText);
    DTE.selectOption('position', 'Dole');
    DTE.fillField('padding_top', changedParams.padding_top);
    DTE.fillField('padding_bottom',changedParams.padding_bottom);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see(changedParams.cookie_title);
    I.see(changedParams.cookie_text);
    I.see(changedParams.cookie_ButtonText);
    I.see(changedParams.cookie_ButtonTextDecline);
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1', 'background-color')), changedParams.color_background);
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 h2', 'color')), changedParams.color_title);
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 p', 'color')), changedParams.color_text);
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 button.btn.btn-primary.cookie-btn', 'background-color')), changedParams.color_button);
    I.assertEqual(rgbToHex(await I.grabCssPropertyFrom('#cookiebar-1 button.btn.btn-primary.cookie-btn', 'color')), changedParams.color_buttonText);
    I.assertEqual(await I.grabCssPropertyFrom('#cookiebar-1 div.container', 'padding-top'), `${changedParams.padding_top}px`);
    I.assertEqual(await I.grabCssPropertyFrom('#cookiebar-1 div.container', 'padding-bottom'), `${changedParams.padding_bottom}px`);
});


function rgbToHex(rgb) {

    const result = rgb.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
    if (result) {
        const r = parseInt(result[1]);
        const g = parseInt(result[2]);
        const b = parseInt(result[3]);

        function componentToHex(c) {
            var hex = c.toString(16);
            return hex.length === 1 ? "0" + hex : hex;
        }

        return "#" + componentToHex(r) + componentToHex(g) + componentToHex(b);
    } else {
        throw new Error("Invalid RGB color format");
    }
}
