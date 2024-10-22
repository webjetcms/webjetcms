Feature('apps.send_link');

Before(({ I, login }) => {
    login('admin');
});

Scenario("Poslať stránku emailom - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/poslat-stranku-emailom/");
    I.waitForElement(".sendLink");
    I.waitForElement(locate('a').withText("Poslať stránku e-mailom"));
    I.click("Poslať stránku e-mailom");
    I.waitForElement('.ui-dialog');
    I.see("Poslať stránku e-mailom");
    I.see("Vaše meno:");
    I.see("Váš e-mail:");
    I.see("E-mail príjemcu:");
    I.see("Predmet:");
    I.see("Zadajte text z obrázku:");
    I.click('Odoslať');
    within('#ajaxFormResultContainer', () => {
        I.see('Prosím, opravte nasledovné chyby:');
        I.see('E-mail príjemcu:');
    });
    I.fillField('#fromName1', 'Meno Odosielateľa');
    I.fillField('#fromEmail1', 'odosielatel@balat.sk');
    I.fillField('#toEmail1', 'prijemca@prijemca.sk');
    I.fillField('#subject1', 'Predmet e-mailu');
    I.fillField('#wjcaptcha1', 'kód z obrázku');
    I.click('Odoslať');

    within('#ajaxFormResultContainer', () => {
        I.see('Prosím, opravte nasledovné chyby:');
        I.see('Zadajte text z obrázku:');
    });
});


Scenario('testovanie app - Poslat stranku emailom', async ({ I, DTE, Apps }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=77774');
    DTE.waitForEditor();
    I.wait(5);
    
    const defaultParams = {
        sendType:'link'
    };

    await Apps.assertParams(defaultParams);
    
    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see('Poslať stránku e-mailom');
  
    I.switchToPreviousTab();
    I.closeOtherTabs();
    
    Apps.openAppEditor();

    const changedParams = {
        sendType:'page'
    };
    DTE.clickSwitch('sendType_0');
    
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')
    
    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    
    I.see('Poslať stránku e-mailom');
});