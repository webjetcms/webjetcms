Feature('apps.file_archiv');

const selector = ".table-responsive-file-archive td.td-file-archive-name a";

Before(({ login, I }) => {
    login('admin');
    if (typeof entityName == "undefined") {
        entityName = "name-autotest-" + I.getRandomText();
    }
});

Scenario("Vlozit novy subor do archivu a filtrovanie", async ({ I }) =>{

    I.closeOtherTabs();

    I.amOnPage("/components/file_archiv/file_list.jsp");
    I.say('Nahratie suboru');
    I.waitForElement(".btn.default.imageButton", 10);
    I.clickCss(".btn.default.imageButton");
    I.switchToNextTab();
    fileName = 'tests/users/wrong-empty-data-user-list.xlsx'
    I.attachFile('#file', fileName);

    I.fillField('#virtual_file_name', entityName);
    I.clickCss('#btnOk');
    I.wait(3);
    await I.clickIfVisible('.button150.no_action');
    I.switchToNextTab();
    I.waitForElement(".btn.default.imageButton", 10);
    I.dontSee("Nenašli sa žiadne záznamy.");

    I.waitForElement('#file_bean_id', 10);
    I.fillField("#file_bean_id", "0");
    I.clickCss(".button50");
    I.see("Nenašli sa žiadne záznamy.");
});


Scenario("Archív súborov - test zobrazovania",  ({ I }) => {
    I.say("Test aplikácie");
    I.amOnPage("/apps/archiv-suborov/");
    I.waitForElement(".table-responsive-file-archive");

    I.waitForElement( locate(selector).withText("WebJET CMS Marketing Promo"), 10);
    I.seeElement( locate(selector).withText("WebJET CMS Manuals") );
    I.seeElement( locate(selector).withText("WebJET CMS FAQ") );

    I.click( locate(selector).withText("WebJET CMS Marketing Promo") );
    I.dontSee("Chyba 404 - požadovaná stránka neexistuje");

    I.switchTo();
    I.closeOtherTabs();
});


Scenario("Vymazanie vlozeneho suboru", ({ I }) => {
    I.say("Filtrujem subor na zmazanie");
    I.amOnPage("/components/file_archiv/file_list.jsp");
    I.fillField("#file_bean_id", "");
    I.fillField('#virtualName', entityName);
    I.clickCss(".button50");

    I.waitForElement('table#row.sort_table tbody');
    within('table#row.sort_table tbody', () => {
        I.see(entityName);
    });

    I.say("Vymazavam subor" + entityName);
    I.click(locate('.deleteOneFiledeleteStructure').first())
    I.acceptPopup();
    I.see("Súbor(y) boli úspešne zmazané");

    I.say("Overujem, ze zmazany subor nevidim")
    I.amOnPage("/apps/archiv-suborov/");
    I.waitForElement(selector);
    I.dontSeeElement( locate(selector).withText(entityName));
});

Scenario("Export archivu", ({ I }) => {
    I.amOnPage('/components/file_archiv/export_archiv.jsp');
    I.see('Pre spustenie exportu, kliknite na tlačidlo OK');
    I.clickCss('#btnOk');
    I.see('Súbor s archívom stiahnete na tomto linku');
    I.clickCss('.button100');
    I.see('bol úspešne vymazaný');
});

Scenario("Import archivu", ({ I }) => {
    I.amOnPage('/components/file_archiv/import_archiv.jsp');
    I.see('Vložte zip súbor z exportu archívu súborov.');
    I.seeElement('#xmlFile');
    I.seeElement('#saveFileForm');
});