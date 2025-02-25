Feature('apps.forum');

Before(({ login }) => {
    login('admin');
});

Scenario('Forum types', async ({I, DTE, Document}) => {
    Document.screenshotAppEditor(65077, "/redactor/apps/forum/clasic-forum.png");
    Document.screenshotAppEditor(63761, "/redactor/apps/forum/message-board.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=63768");
    DTE.waitForEditor();
    I.clickCss('#trEditor', null, { position: { x: 177, y: 400 } });
    I.pressKey(['Ctrl', 'Q']);
    Document.screenshot("/redactor/apps/forum/message-board-upload.png");
});

Scenario('Forum list', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/forum/admin/");
    let confLng = I.getConfLng();

    DT.filterContains("subject", "diskusia testovací príspevok");
    Document.screenshot("/redactor/apps/forum/forum-list.png", 1500, 800);

    I.clickCss("#forumDataTable_wrapper > div:nth-child(2) > div > div > div.dt-scroll > div.dt-scroll-head > div > table > thead > tr:nth-child(2) > th.dt-format-select.dt-th-editorFields-statusIcons > form > div > div > button");
    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/forum/forum-list-statusSelect.png")

    DT.filterContains("question", "Toto je jednoduchá diskusia pod článkom");
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
    I.clickCss("#pills-dt-forumDataTable-advanced-tab");
    I.clickCss("#DTE_Field_forumGroupEntity-messageConfirmation_0");
    Document.screenshot("/redactor/apps/forum/forum-list-editor-advanced.png", 1000, 1100);
    DTE.cancel();

    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/diskusia/?NO_WJTOOLBAR=true");
            break;
        case 'en':
            I.amOnPage("/apps/diskusia/?NO_WJTOOLBAR=true&language=en");
            break;
        case 'cs':
            I.amOnPage("/apps/diskusia/?NO_WJTOOLBAR=true&language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    I.waitForElement("#forumContentDiv", 15);
    Document.screenshot("/redactor/apps/forum/forum-list-forum.png");

    I.resizeWindow(1200, 900);
    I.clickCss("#forumContentDiv > div.row > div > a");
    I.waitForElement("div.ui-dialog.ui-corner-all");
    Document.screenshot("/redactor/apps/forum/forum-list-forum-add.png");
    //Resize back
    I.resizeWindow(1280, 760);

    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/message-board/skupina2/podskupina3.html?NO_WJTOOLBAR=true");
            break;
        case 'en':
            I.amOnPage("/apps/message-board/skupina2/podskupina3.html?NO_WJTOOLBAR=true&language=en");
            break;
        case 'cs':
            I.amOnPage("/apps/message-board/skupina2/podskupina3.html?NO_WJTOOLBAR=true&language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }


    I.waitForElement("#forumContentDiv");
    Document.screenshot("/redactor/apps/forum/forum-list-main.png", 1200, 930);

    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/message-board/skupina2/podskupina3.html?NO_WJTOOLBAR=true&pId=809");
            break;
        case 'en':
            I.amOnPage("/apps/message-board/skupina2/podskupina3.html?NO_WJTOOLBAR=true&pId=809&language=en");
            break;
        case 'cs':
            I.amOnPage("/apps/message-board/skupina2/podskupina3.html?NO_WJTOOLBAR=true&pId=809&language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    I.waitForElement("#forumContentDiv");
    Document.screenshot("/redactor/apps/forum/forum-list-board.png", 1200, 650);

    if("sk" === confLng) {
        I.amOnPage("/apps/message-board/skupina1/podskupina1.html?NO_WJTOOLBAR=true&pId=366");
    } else if("en" === confLng) {
        I.amOnPage("/apps/message-board/skupina1/podskupina1.html?NO_WJTOOLBAR=true&pId=366&language=en");
    }

    I.waitForElement("#forumContentDiv");
    Document.screenshot("/redactor/apps/forum/forum-list-subBoard.png", 1200, 900);

    I.amOnPage("/apps/forum/admin/");
    DT.filterContains("subject", "diskusia testovací príspevok");
    I.click("Re: Viactémová diskusia testovací príspevok");
    DTE.waitForEditor("forumDataTable");
    I.clickCss("#pills-dt-forumDataTable-advanced-tab");
    I.clickCss("#DTE_Field_forumGroupEntity-active_0");
    DTE.save();
    Document.screenshot("/redactor/apps/forum/forum-list-state-combination.png", 1000, 700);
        //UNDO this action
        I.click("Re: Viactémová diskusia testovací príspevok");
        DTE.waitForEditor("forumDataTable");
        I.clickCss("#pills-dt-forumDataTable-advanced-tab");
        I.clickCss("#DTE_Field_forumGroupEntity-active_0");
        DTE.save();
});