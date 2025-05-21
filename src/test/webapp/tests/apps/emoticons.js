Feature('apps.emoticons');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Emoticons', async ({ I, Apps }) => {
    Apps.insertApp('Emotikony', '#components-emoticon-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    I.clickCss("#panel-body-dt-component-datatable-basic img[src*=biggrin]");
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    Apps.switchEditor("html");
    const inputString = await I.grabTextFrom('.CodeMirror-code');
    I.assertTrue(inputString.includes("/components/emoticon/admin-styles/biggrin.png"));
});

