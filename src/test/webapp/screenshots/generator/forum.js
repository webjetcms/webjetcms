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
    I.switchTo();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=63768");
    DTE.waitForEditor();
    I.click('#trEditor', null, { position: { x: 177, y: 400 } });
    I.pressKey(['Ctrl', 'Q']);
    Document.screenshot("/redactor/apps/forum/message-board-upload.png");
});

Scenario('Forum list', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/forum/admin/");
    let confLng = I.getConfLng();

    DT.filter("subject", "diskusia testovací príspevok");
    Document.screenshot("/redactor/apps/forum/forum-list.png", 1500, 800);

    I.click("#forumDataTable_wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(2) > th.dt-format-select.dt-th-editorFields-statusIcons > form > div > div > button");
    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/forum/forum-list-statusSelect.png")

    DT.filter("question", "Toto je jednoduchá diskusia pod článkom");
    I.click("button.buttons-select-all");

    Document.screenshotElement("button.buttons-edit", "/redactor/apps/forum/editButton.png");
    Document.screenshotElement("button.buttons-remove", "/redactor/apps/forum/removeButton.png");
    Document.screenshotElement("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(5)", "/redactor/apps/forum/eyeButton.png");
    Document.screenshotElement("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(6)", "/redactor/apps/forum/recoverButton.png");
    Document.screenshotElement("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(7)", "/redactor/apps/forum/acceptButton.png");
    Document.screenshotElement("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(8)", "/redactor/apps/forum/rejectButton.png");

    I.click("button.buttons-edit");
    DTE.waitForEditor("forumDataTable");
    Document.screenshot("/redactor/apps/forum/forum-list-editor.png");
    I.click("#pills-dt-forumDataTable-advanced-tab");
    I.click("#DTE_Field_forumGroupEntity-messageConfirmation_0");
    Document.screenshot("/redactor/apps/forum/forum-list-editor-advanced.png", 1000, 1100);
    DTE.cancel();

    if("sk" === confLng) {
        I.amOnPage("/apps/diskusia/");
    } else if("en" === confLng) { 
        I.amOnPage("/apps/diskusia/?language=en");
    }

    I.waitForElement("#forumContentDiv", 15);
    Document.screenshot("/redactor/apps/forum/forum-list-forum.png");

    I.resizeWindow(1200, 900);
    I.click("#forumContentDiv > div.row > div > a");
    I.waitForElement("div.ui-dialog.ui-corner-all");
    Document.screenshot("/redactor/apps/forum/forum-list-forum-add.png");
    //Resize back
    I.resizeWindow(1280, 760);

    if("sk" === confLng) {
        I.amOnPage("/apps/message-board/skupina2/podskupina3.html");
    } else if("en" === confLng) { 
        I.amOnPage("/apps/message-board/skupina2/podskupina3.html?language=en");
    }

    I.waitForElement("#forumContentDiv");
    Document.screenshot("/redactor/apps/forum/forum-list-board.png", 1200, 1100);

    if("sk" === confLng) {
        I.amOnPage("/apps/message-board/skupina2/podskupina3.html?pId=809");
    } else if("en" === confLng) { 
        I.amOnPage("/apps/message-board/skupina2/podskupina3.html?pId=809&language=en");
    }

    I.waitForElement("#forumContentDiv");
    Document.screenshot("/redactor/apps/forum/forum-list-subBoard.png", 1200, 1100);

    I.amOnPage("/apps/forum/admin/");
    DT.filter("subject", "diskusia testovací príspevok");
    I.click("Re: Viactémová diskusia testovací príspevok");
    DTE.waitForEditor("forumDataTable");
    I.click("#pills-dt-forumDataTable-advanced-tab");
    I.click("#DTE_Field_forumGroupEntity-active_0");
    DTE.save();
    Document.screenshot("/redactor/apps/forum/forum-list-state-combination.png", 1000, 700);
        //UNDO this action
        I.click("Re: Viactémová diskusia testovací príspevok");
        DTE.waitForEditor("forumDataTable");
        I.click("#pills-dt-forumDataTable-advanced-tab");
        I.click("#DTE_Field_forumGroupEntity-active_0");
        DTE.save();
});