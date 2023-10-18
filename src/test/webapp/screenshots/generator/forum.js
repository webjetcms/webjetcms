Feature('apps.forum');

Before(({ login }) => {
    login('admin');
});

Scenario('Forum types', async ({I, DTE, Document}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=65077");
    DTE.waitForEditor();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.waitForElement("iframe.wj_component", 10);
    I.wait(2);
    I.click("iframe.wj_component");
    I.wait(2);

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");

    Document.screenshotElement("#editorComponent", "/redactor/apps/forum/clasic-forum.png");
    I.switchTo();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=63761");
    DTE.waitForEditor();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.waitForElement("iframe.wj_component", 10);
    I.wait(2);
    I.click("iframe.wj_component");
    I.wait(2);

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");

    Document.screenshotElement("#editorComponent", "/redactor/apps/forum/message-board.png");
});


Scenario('Foum list', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/forum/admin/");

    Document.screenshot("/redactor/apps/forum/forum-list.png", 1500, 800);

    I.click("#forumDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-select.dt-th-editorFields-statusIcons > form > div > div > button");

    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/forum/forum-list-statusSelect.png")

    DT.filter("subject", "Forum_autotest_diskusia_1");

    I.click("button.buttons-select-all");

    Document.screenshotElement("button.buttons-edit", "/redactor/apps/forum/editButton.png");
    Document.screenshotElement("button.buttons-remove", "/redactor/apps/forum/removeButton.png");
    Document.screenshotElement("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(5)", "/redactor/apps/forum/eyeButton.png");
    Document.screenshotElement("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(6)", "/redactor/apps/forum/settingsButton.png");
    Document.screenshotElement("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(7)", "/redactor/apps/forum/recoverButton.png");

    I.click("button.buttons-edit");

    DTE.waitForEditor("forumDataTable");

    Document.screenshot("/redactor/apps/forum/forum-list-editor.png");

    DTE.cancel();

    I.click("/Aplikácie/Diskusia/Diskusia");
    I.wait(2);
    I.switchToNextTab();
    I.wait(15);
    Document.screenshot("/redactor/apps/forum/forum-list-forum.png");
    I.closeCurrentTab();

    DT.filter("subject", "Nova tema v podskupina 1");

    I.click("/Aplikácie/Message Board/Skupina1/podskupina1");
    I.wait(5);
    I.switchToNextTab();
    Document.screenshot("/redactor/apps/forum/forum-list-board.png", 1500, 800);
    I.click("Nova tema v podskupina 1");
    I.wait(2);
    Document.screenshot("/redactor/apps/forum/forum-list-subBoard.png", 1500, 800);
    I.closeCurrentTab();

    DT.filter("question", "Este jedna odpoved cislo DVA...");
    I.wait(1);
    I.click("button.buttons-select-all");

    I.click("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(6)");

    I.switchToNextTab();
    I.wait(2);
    Document.screenshot("/redactor/apps/forum/forum-list-admin.png", 800, 1000);
});