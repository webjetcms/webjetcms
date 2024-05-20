Feature('apps.send_link');

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