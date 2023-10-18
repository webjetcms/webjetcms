Feature('admin.in-memory-logging');

/**
 * Otestovanie zobrazenie ERROR levelu v logovani v pamati
 * - vyvolanie chyby pri ukladani stranky
 * - kontrola logov ci sa tam nachadza dany log
 */

Before(({ I, login }) => {
    login('admin');
});

Scenario("logovanie v pamati s ERROR levelom @singlethread", ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?removePerm=addPage,pageSave,deletePage,pageSaveAs,forceShowButton&groupid=67");
    DT.waitForLoader();

    I.click("#datatableInit_wrapper button.buttons-create");
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("Názov web stránky", "Test");
    DTE.save();
    I.see("Pridať web stránku - nemáte právo na pridanie web stránky");
    DTE.cancel();

    I.amOnPage("/admin/v9/settings/in-memory-logging/");
    DT.filterSelect("level", "ERROR");
    I.see('ERROR');
    I.see('Pridať web stránku - nemáte právo na pridanie web stránky');
});