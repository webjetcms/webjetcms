Feature('apps.app-date');

Before(({ I, login }) => {
    login('admin');
});

function getDate(){
    let days = ["Nedeľa", "Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok", "Sobota"];
    let currentDate = new Date();
    let nameOfDay = (days[currentDate.getDay()]);
    let day = currentDate.getDate();
    let month = currentDate.getMonth() + 1;
    let year = currentDate.getFullYear();

    if (day < 10) {
        day = '0' + day;
    }
    if (month < 10) {
        month = '0' + month;
    }

    let formattedDate = `${nameOfDay} ${day}.${month}.${year}`;
    return formattedDate;
}

Scenario("Dátum - test zobrazenia", ({ I }) => {
    I.amOnPage("/apps/datum/");
    I.waitForElement(".dateapp");
    I.seeElement(locate("h1").withText("Dátum"));
    I.see(getDate());
});

Scenario('testovanie app - last modify', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Dátum a meniny', '#components-app-date-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');
    DTE.selectOption('field', 'Dátum poslednej modifikácie');

    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    const defaultParams = {
        aktualizovane: 'false',
        datum: 'false',
        cas: 'false'
    };

    await Apps.assertParams(defaultParams);

    Apps.openAppEditor();

    const changedParams = {
        aktualizovane: 'true',
        datum: 'true',
        cas: 'true'
    };

    DTE.clickSwitch('aktualizovane_0');
    DTE.clickSwitch('datum_0');
    DTE.clickSwitch('cas_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);
});

Scenario('testovanie app - current date', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Dátum a meniny', '#components-app-date-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');
    DTE.selectOption('field', 'dd.mm.rrrr');
    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    Apps.switchEditor('html');
    I.see('<p>!DATUM!</p>', '.CodeMirror-code');

    I.say('Visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.waitForText(getDate().split(' ')[1], 10);
});

Scenario('testovanie app - meniny', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Dátum a meniny', '#components-app-date-title' );

    const defaultParams = {
        'format' : 'long'
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.waitForText(getDate() + ', dnes má meniny', 10);

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        'format' : 'short'
    };
    DTE.clickSwitch('format_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.waitForText('Dnes má meniny');
    I.dontSee(getDate());
});