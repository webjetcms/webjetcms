Feature('editor-locking');


Scenario('editor locking hlasenia', ({ I, login, DT, DTE, Document }) => {

    login('admin');

    //Prihlásený ako - Playwright
    I.amOnPage("/apps/qa/admin/");
    I.fillField("input.dt-filter-question", "Koľko nôh ma pavúk ?");
    I.pressKey('Enter', "input.dt-filter-question");
    DT.waitForLoader();
    I.click("Koľko nôh ma pavúk ?");
    DTE.waitForEditor("qaDataTable");

    //Odhlás sa
    I.amOnPage("/logoff.do?forward=/admin/");

    login('tester2');

    I.amOnPage("/apps/qa/admin/");
    I.fillField("input.dt-filter-question", "Koľko nôh ma pavúk ?");
    I.pressKey('Enter', "input.dt-filter-question");
    DT.waitForLoader();
    I.click("Koľko nôh ma pavúk ?");
    DTE.waitForEditor("qaDataTable");
    //Vidím hlasenie že tento zaznam prave teraz upravuje iný uživateľ

    I.moveCursorTo("div.toast-title");
    I.wait(5);

    Document.screenshot("/developer/datatables-editor/editor-locking.png", 1280, 450);

});

Scenario('odhlasenie', ({I}) => {
    //lebo sme pred tym v teste zmenili domenu
    I.amOnPage('/logoff.do?forward=/admin/');
});