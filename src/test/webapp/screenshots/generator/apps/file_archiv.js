Feature('apps.file_archiv');

Before(({ login }) => {
    login('admin');
});

Scenario('file archiv - list', ({ I, Document }) => {
    I.amOnPage("/components/file_archiv/file_list.jsp");
    Document.screenshot("/redactor/apps/file_archiv/file_list.png");
    Document.screenshotElement("img.aFileUpload","/redactor/apps/file_archiv/upload.png");
    Document.screenshotElement("img[alt='Editovať']","/redactor/apps/file_archiv/edit.png");
    Document.screenshotElement("img[alt='Premenovať']","/redactor/apps/file_archiv/rename.png");
    Document.screenshotElement("img.aFileHistory","/redactor/apps/file_archiv/file_history.png");
    Document.screenshotElement("img[alt='Vrátenie poslednej zmeny spať']","/redactor/apps/file_archiv/rollback.png");
    Document.screenshotElement("img[alt='Zmazať súbor']","/redactor/apps/file_archiv/delete.png");
    Document.screenshotElement({name:"fileArchivFilterForm"},"/redactor/apps/file_archiv/filter.png");
    Document.screenshotElement(".btn.default.imageButton", "/redactor/apps/file_archiv/file_insert.png");

    //Awaiting files
    I.click( locate("ul.nav.nav-tabs > li > a").withText("Čakajúce súbory") );
    I.waitForInvisible("form.zobrazenie");
    Document.screenshot("/redactor/apps/file_archiv/file_list_awaiting.png");

    //Go back to list
    I.click( locate("ul.nav.nav-tabs > li > a").withText("Filter") );
    I.waitForVisible("form.zobrazenie");

    I.clickCss(".btn.default.imageButton");
    I.switchToNextTab();
    I.resizeWindow(650, 900);
    I.checkOption("#uploadLaterCheckboxId");
    Document.screenshot("/redactor/apps/file_archiv/dialog.png");

    I.switchTo();
    I.closeOtherTabs();
});

Scenario('file archiv - webpage', ({ I, DT, DTE, Document }) => {
    Document.screenshotAppEditor(77668, "/redactor/apps/file_archiv/editor.png");

    //Screenshot webpage with app
    I.amOnPage("/apps/archiv-suborov/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/file_archiv/file_archiv.png");
});
