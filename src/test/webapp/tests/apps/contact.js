Feature('apps.contact');
//Testy pre custom aplikaciu contacts

Before(({ I, login }) => {
    login('admin');
});

/*Scenario('MVC upload', ({I}) => {
    I.amOnPage("/apps/contact/admin/upload/");

    I.see("P1");
    I.see("Dokument");
    I.click("Potvrdiť");

    I.see("Pole P1 nemôže byť prázdne");
    I.see("Pole P2 musí byť medzi 10 a 20 znakmi");
    I.see("Dokument nemôže byť prázdny");

    I.amOnPage("/apps/contact/admin/upload/");

    I.fillField("P1", "test1");
    I.fillField("P2", "test1test1test1");
    I.attachFile("Dokument", 'tests/components/insert-script.xlsx');
    I.click("Potvrdiť");

    I.see("File successfully uploaded, fileName: insert-script.xlsx");
});*/

Scenario('Excel import', ({I, DT, DTE}) => {
    I.amOnPage("/apps/contact/admin/");
    DT.filter("name", "test5");
    I.dontSee("test5", "#dataTable");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.see("Záznamy 0 až 0 z 0");

    I.amOnPage("/apps/contact/admin/excelimport/");

    I.fillField("P1", "test1");
    I.fillField("P2", "test1test1test1");
    I.attachFile("Dokument", 'tests/apps/contact.xlsx');
    I.click("Potvrdiť");

    I.see("Úspešný import, ďakujeme");

    I.amOnPage("/apps/contact/admin/");
    DT.filter("name", "test5");
    I.see("test5", "#dataTable");
    I.see("Záznamy 1 až 1 z 1");

    //zobraz zaznam a over konverziu PSC
    I.click("test5", "#dataTable");
    DTE.waitForEditor("dataTable");
    I.seeInField("#DTE_Field_zip", "123456");
    I.dontSeeInField("#DTE_Field_zip", "123456.0");

    I.seeInField("#DTE_Field_phone", "+421 90");
});

Scenario('Excel import-zmazanie', ({I, DT}) => {
    I.amOnPage("/apps/contact/admin/");
    DT.filter("name", "test5");
    I.see("test5", "#dataTable");
    I.see("Záznamy 1 až 1 z 1");

    I.click(".buttons-select-all");
    I.click("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.waitForText("Naozaj chcete zmazať položku?", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.waitForInvisible("div.DTE_Action_Remove", 10);

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.see("Záznamy 0 až 0 z 0");
});

Scenario('testovanie app - Kontakty', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Kontakty', '#apps-contact-title');

    const defaultParams = {
        country: 'sk'
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see('ZOZNAM KONTAKTOV', 'h3')
    I.see('Bratislava', 'tr')

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        country:'cz',
    };

    DTE.selectOption('country', 'Česká republika');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see('ZOZNAM KONTAKTOV', 'h3');
    I.dontSee('Bratislava', 'tr');
    I.see('Praha', 'tr');
});

Scenario('testovanie app - Kontakty - close other tabs', async ({ I, Apps }) => {
    I.closeOtherTabs();
});

