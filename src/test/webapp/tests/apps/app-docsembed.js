Feature('apps.app-docsembed');

const normalUrl = 'https://docs.webjetcms.sk/v2023/_media/manuals/WebJET_pre_redaktorov.docx';
var base64Url;

Before(async ({ I, login }) => {
    login('admin');
    if (typeof base64Url == "undefined"){
        base64Url = await I.executeScript((normalUrl) => {
            return WJ.base64encode((normalUrl));
        }, normalUrl);
    }
});

Scenario("Vlozenie dokumentu - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/vlozenie-dokumentu/");
    I.waitForElement('iframe');
    I.waitForInvisible('.loader', 120);
    I.wait(1);
    I.waitForElement("#docsembed-1 > iframe", 20);
    I.wait(1);
    I.switchTo("#docsembed-1 > iframe");
    I.wait(1);
    I.waitForText("Používateľská príručka", 20, "p");
    I.switchTo();
});

Scenario('testovanie app - Vlozenie dokumentu', async ({ I, DTE, Apps, Document }) => {
    I.switchTo();
    Apps.insertApp('Vloženie dokumentu', '#components-app-docsembed-title');
    const defaultParams = {
        url: '',
        height: '900',
        width: '100%',
        device: '',
        cacheMinutes: ''
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.wait(2);

    I.switchToNextTab();

    let texts = ["No preview available", "Ukážka nie je k dispozícii"];
    let visibleTexts = 0;
    let failsafe = 0;
    I.waitForElement("#docsembed-1 > iframe");
    I.switchTo("#docsembed-1 > iframe");
    while (visibleTexts < 2 && failsafe++ < 120) {
        visibleTexts = 0;
        for (let text of texts) {
            visibleTexts += await I.grabNumberOfVisibleElements(locate("div").withText(text));
        }
        I.say(`Visible texts: ${visibleTexts}, failsafe: ${failsafe}`);
        if (visibleTexts > 0) {
            break;
        }
        I.wait(1);
    }
    I.switchTo();
    I.assertAbove(visibleTexts, 0, "No preview available text is not visible");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    I.say("Base64 URL: " + base64Url);

    const changedParams = {
        url: base64Url,
        height: '1200',
        width: '80%',
        device: '',
        cacheMinutes: ''
    };

    I.fillField('div.DTE_Field_Type_elfinder div.input-group input.form-control', "**");
    I.fillField('div.DTE_Field_Type_elfinder div.input-group input.form-control', normalUrl);
    DTE.fillField('height', changedParams.height);
    DTE.fillField('width', changedParams.width);
    I.wait(2);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForInvisible('.loader', 120);
    I.wait(1);
    within({ frame: 'iframe[src*="docs.webjetcms.sk"]' }, () => {
        I.see("Používateľská príručka");
    });
});

Scenario('testovanie app -  Vloženie dokumentu - close tabs', ({ I }) => {
    I.closeOtherTabs();
});