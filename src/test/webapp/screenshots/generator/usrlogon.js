Feature('apps.usrlogon');

Before(({ login }) => {
    login("admin");
});

Scenario('zaheslovana-zona-screen-y', ({ I, DT, DTE, Document }) => {
    //user list
    I.amOnPage("/admin/v9/users/user-list/");
    I.forceClick("table.datatableInit tr:nth-child(3) td.dt-select-td");
    I.moveCursorTo("button.btn-auth-user-no-gen", 5, 5);
    Document.screenshot("/redactor/zaheslovana-zona/user-list-page.png");
    I.click("td.dt-select-td.sorting_1");
    Document.screenshotElement('//*[@id="datatableInit_wrapper"]/div[1]/div/div[1]/div/button[5]', "/redactor/zaheslovana-zona/user-list-page-approve_1.png");
    Document.screenshotElement('//*[@id="datatableInit_wrapper"]/div[1]/div/div[1]/div/button[6]', "/redactor/zaheslovana-zona/user-list-page-approve_2.png");

    //usr group page
    I.amOnPage('/admin/v9/users/user-groups/');
    Document.screenshot("/redactor/zaheslovana-zona/user-groups-page.png");
    //Document.screenshotElement("button.buttons-create", "/redactor/zaheslovana-zona/user-groups-create-button.png");
    I.click("button.buttons-create");
    DTE.waitForEditor("userGroupsDataTable");
    Document.screenshot("/redactor/zaheslovana-zona/user-groups-page-editor.png");
    //Document.screenshotElement('//*[@id="userGroupsDataTable_modal"]/div/div/div[4]/div[3]/button[2]', "/redactor/zaheslovana-zona/user-groups-create-button2.png");
    DTE.cancel();
    DT.filterContains("userGroupName", "noApprove_allowUserEdit");
    I.click("noApprove_allowUserEdit_1");
    DTE.waitForEditor("userGroupsDataTable");
    I.clickCss("#pills-dt-userGroupsDataTable-folders-tab");
    I.wait(1);
    Document.screenshot("/redactor/zaheslovana-zona/user-groups-page-editor-folders.png");
    I.clickCss("#pills-dt-userGroupsDataTable-sites-tab");
    I.wait(1);
    Document.screenshot("/redactor/zaheslovana-zona/user-groups-page-editor-pages.png");

    //docs
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=64547");
    DTE.waitForEditor();
    I.wait(5);
    Document.screenshot("/redactor/zaheslovana-zona/docs-login.png");
    DTE.cancel();
    I.click(locate("a.jstree-anchor").withText("Prihlásený používateľ"));
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-access-tab");
    Document.screenshot("/redactor/zaheslovana-zona/set-login-page.png");
    DTE.cancel();

    I.click(locate("a.jstree-anchor").withText("Zákaznícka zóna"));
    I.click("button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-access-tab");
    Document.screenshot("/redactor/zaheslovana-zona/set-user-groups.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=64542");
    DTE.waitForEditor();
    I.wait(5);
    Document.screenshot("/redactor/zaheslovana-zona/docs-register-1.png");
    I.clickCss("#cke_1_contents");
    I.wait(1);
    Document.screenshot("/redactor/zaheslovana-zona/docs-register-2.png", 1500, 800);
});