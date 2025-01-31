Feature('admin.editor-locking');

Scenario('Kontrolova editor locking hlasenia', ({ I, DT, DTE }) => {

    I.relogin('tester2');

    //Prihlásený ako - Playwright
    I.amOnPage("/apps/qa/admin/");
    I.fillField("input.dt-filter-question", "Koľko nôh ma pavúk ?");
    I.pressKey('Enter', "input.dt-filter-question");
    DT.waitForLoader();
    I.click("Koľko nôh ma pavúk ?");
    DTE.waitForEditor("qaDataTable");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=274");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-media-tab");
    I.wait(5);
    I.click("asdfasdfasdf");
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");

    //presmerovania domen nemaju .id, id sa musi ziskat spravne podla editorId fieldu
    I.amOnPage("/admin/v9/settings/domain-redirect/?redirectId=62");
    DTE.waitForEditor();
    I.wait(5);

    //Odhlás sa
    I.relogin('tester');

    I.amOnPage("/apps/qa/admin/");
    I.fillField("input.dt-filter-question", "Koľko nôh ma pavúk ?");
    I.pressKey('Enter', "input.dt-filter-question");
    DT.waitForLoader();
    I.click("Koľko nôh ma pavúk ?");
    DTE.waitForEditor("qaDataTable");
    //Vidím hlasenie že tento zaznam prave teraz upravuje iný uživateľ
    I.see("Záznam má v editore otvorený aj:", "div.toast-message");
    I.see("Tester2 Playwright2", "div.toast-message");

    //prejdi na iny zaznam s rovnakym ID=1 a over, ze sa hlasenie nezobrazi
    I.amOnPage("/apps/gdpr/admin/?id=1");
    DTE.waitForEditor("cookiesDataTable");
    I.dontSee("Záznam má v editore otvorený aj");

    //over zobrazenie medii
    I.amOnPage("/admin/v9/webpages/media/");
    DT.filterContains("mediaTitleSk", "asdfasdfasdf");
    I.click("asdfasdfasdf");
    DTE.waitForEditor("mediaTable");
    I.see("Záznam má v editore otvorený aj:", "div.toast-message");
    I.see("Tester2 Playwright2", "div.toast-message");

    //prejdi nazad na QA
    I.amOnPage("/apps/qa/admin/?id=1");
    DTE.waitForEditor("qaDataTable");
    I.see("Záznam má v editore otvorený aj:", "div.toast-message");
    I.see("Tester2 Playwright2", "div.toast-message");

    //over v presmerovaniach
    I.amOnPage("/admin/v9/settings/domain-redirect/?redirectId=62");
    DTE.waitForEditor();
    I.see("Záznam má v editore otvorený aj:", "div.toast-message");
    I.see("Tester2 Playwright2", "div.toast-message");

    //pockaj 2 minuty na exspirovanie cache
    I.wait(120);

    I.amOnPage("/apps/qa/admin/?id=1");
    DTE.waitForEditor("qaDataTable");
    I.dontSee("Záznam má v editore otvorený aj:");

    //over ne zobrazenie medii
    I.amOnPage("/admin/v9/webpages/media/");
    DT.filterContains("mediaTitleSk", "asdfasdfasdf");
    I.click("asdfasdfasdf");
    DTE.waitForEditor("mediaTable");
    I.dontSee("Záznam má v editore otvorený aj:");
    I.dontSee("Tester2 Playwright2");

    //over v presmerovaniach
    I.amOnPage("/admin/v9/settings/domain-redirect/?redirectId=62");
    DTE.waitForEditor();
    I.dontSee("Záznam má v editore otvorený aj:");
    I.dontSee("Tester2 Playwright2");

    I.logout();
});

Scenario('bug otvorenia noveho okna bez volania REST sluzby pre ziskanie noveho zaznamu', ({I, DTE}) => {

    I.relogin('tester');

    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_forward", "*");
    I.waitForVisible("ul.dt-autocomplete-select");
    //over, ze sa nacitali
    I.see("default/subpage.jsp", "ul.dt-autocomplete-select")
    I.see("default/homepage.jsp", "ul.dt-autocomplete-select")

    I.logout();

});