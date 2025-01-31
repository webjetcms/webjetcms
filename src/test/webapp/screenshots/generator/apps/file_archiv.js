Feature('apps.file_archiv');

Before(({ login }) => {
    login('admin');
});

Scenario('file archiv - list', ({ I, Document, i18n }) => {

    I.resizeWindow(1500, 800);

    I.amOnPage("/components/file_archiv/file_list.jsp");
    Document.screenshot("/redactor/apps/file_archiv/file_list.png");
    Document.screenshotElement("img.aFileUpload","/redactor/apps/file_archiv/upload.png");
    Document.screenshotElement(`img[alt='${i18n.get("Edit")}']`,"/redactor/apps/file_archiv/edit.png");
    Document.screenshotElement(`img[alt='${i18n.get("Rename")}']`,"/redactor/apps/file_archiv/rename.png");
    Document.screenshotElement("img.aFileHistory","/redactor/apps/file_archiv/file_history.png");
    Document.screenshotElement(`img[alt='${i18n.get("Undo last change of sleep")}']`,"/redactor/apps/file_archiv/rollback.png");
    Document.screenshotElement(`img[alt='${i18n.get("Delete file")}']`,"/redactor/apps/file_archiv/delete.png");
    Document.screenshotElement({name:"fileArchivFilterForm"},"/redactor/apps/file_archiv/filter.png");
    Document.screenshotElement(".btn.default.imageButton", "/redactor/apps/file_archiv/file_insert.png");

    //Awaiting files
    I.click( locate("ul.nav.nav-tabs > li > a").withText(i18n.get("Pending files")) );
    I.waitForInvisible("form.zobrazenie");
    Document.screenshot("/redactor/apps/file_archiv/file_list_awaiting.png");

    //Go back to list
    I.click( locate("ul.nav.nav-tabs > li > a").withText(i18n.get("Filter")) );
    I.waitForVisible("form.zobrazenie");

    I.clickCss(".btn.default.imageButton");
    I.switchToNextTab();
    I.resizeWindow(650, 900);
    I.checkOption("#uploadLaterCheckboxId");
    Document.screenshot("/redactor/apps/file_archiv/dialog.png");

    I.switchTo();
    I.closeOtherTabs();
});

Scenario('file archive - inserting an app', ({ I, DT, DTE, Document, i18n }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    
    I.waitForElement(".cke_wysiwyg_frame.cke_reset");
    I.wait(1);

    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.clickCss('.cke_button.cke_button__components.cke_button_off');
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');
    I.waitForElement('#search', 10);
    I.wait(1);
    i18n.fillField('#search', 'File archive');

    I.waitForInvisible('div.promoApp', 10);
    I.waitForElement("#components-file_archiv-title", 10);
    I.wait(1);
    I.clickCss("#components-file_archiv-title");
    I.waitForInvisible("div.appStore > div.block-header", 30);
    I.wait(1);
    Document.screenshot('/redactor/apps/file_archiv/apps-insert.png');
});

Scenario('file archiv - webpage', ({ I, DT, Document }) => {
    Document.screenshotAppEditor(77668, "/redactor/apps/file_archiv/editor.png");

    //Screenshot webpage with app
    I.amOnPage("/apps/archiv-suborov/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/file_archiv/file_archiv.png");
});
