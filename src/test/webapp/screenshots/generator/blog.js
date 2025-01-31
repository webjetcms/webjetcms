Feature('apps.blog');

Scenario('Blog articles', ({ I, DTE, Document, DT }) => {
    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/");

    Document.screenshot("/redactor/apps/blog/blogger-blog.png");
    Document.screenshotElement("#bloggerArticlesDataTable_extfilter", "/redactor/apps/blog/groupFilter_defaultValue.png");

    Document.screenshotElement("button.buttons-create", "/redactor/apps/blog/add_article.png");
    Document.screenshotElement("button.buttons-add-folder", "/redactor/apps/blog/add_folder.png");

    I.clickCss("#bloggerArticlesDataTable_extfilter > div > div > div > button.dropdown-toggle");
    Document.screenshotElement("body > div.bs-container.dropdown.bootstrap-select > div", "/redactor/apps/blog/groupFilter_allValues.png");

    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-warning");
    I.moveCursorTo("#toast-container-webjet > .toast-warning");
    Document.screenshotElement("#toast-container-webjet > .toast-warning", "/redactor/apps/blog/adding_folder_warning.png");

    I.clickCss("#bloggerArticlesDataTable_extfilter > div > div > div > button.dropdown-toggle");
    I.click( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm") );
    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-info");
    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");
    I.waitForElement("#toast-container-webjet > .toast-error");
    I.moveCursorTo("#toast-container-webjet > .toast-error");
    Document.screenshotElement("#toast-container-webjet > .toast-error", "/redactor/apps/blog/adding_folder_error.png");

    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-info");
    I.fillField("#toast-container-webjet > div > div.toast-message > div.toastr-input > input", "NewSubFolder");
    I.moveCursorTo("#toast-container-webjet > .toast-info");
    Document.screenshotElement("#toast-container-webjet > .toast-info", "/redactor/apps/blog/adding_folder_info.png");
    Document.screenshotElement("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary", "/redactor/apps/blog/adding_folder_info_button.png");
    I.clickCss("#toast-container-webjet > div > div.toast-message > div.toastr-buttons > button.btn.btn-primary");

    I.waitForElement("#toast-container-webjet > .toast-success");
    I.moveCursorTo("#toast-container-webjet > .toast-success");
    Document.screenshotElement("#toast-container-webjet > .toast-success", "/redactor/apps/blog/adding_folder_success.png");

    I.clickCss("#bloggerArticlesDataTable_extfilter > div > div > div > button.dropdown-toggle");
    I.click( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/bloggerPerm/NewSubFolder") );
    I.wait(1);
    I.clickCss("#bloggerArticlesDataTable_extfilter > div > div > div > button.dropdown-toggle");
    Document.screenshotElement("body > div.bs-container.dropdown.bootstrap-select > div", "/redactor/apps/blog/groupFilter_allValues_withNew.png");

    I.say("remove added folder");
    I.relogin("tester");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=63847");
    I.jstreeClick("NewSubFolder");
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.clickCss("div.DTE_Action_Remove div.DTE_Footer div.DTE_Form_Buttons button.btn-primary");
    DTE.waitForLoader();
    I.dontSeeElement(locate("a.jstree-anchor").withText("NewSubFolder"));

    I.logout();
    I.amOnPage("/apps/blog/blogger/webjet-cms/");
    Document.screenshot("/redactor/apps/blog/blog-news-list.png");
    I.amOnPage("/apps/blog/blogger/webjet-cms/nova-verzia-seo-aplikacie.html");
    Document.screenshot("/redactor/apps/blog/blog-page-detail.png", 1280, 1100);

    I.relogin("blogger");
    I.amOnPage("/apps/blog/admin/?docId=81958");
    DTE.waitForEditor("bloggerArticlesDataTable");
    Document.screenshot("/redactor/apps/blog/editor-text.png");
    I.click("#pills-dt-bloggerArticlesDataTable-perex-tab");
    Document.screenshot("/redactor/apps/blog/editor-perex.png");
});

Scenario('Blog bloggers', ({ I, DT, DTE, Document }) => {
    I.relogin("tester");
    I.amOnPage("/apps/blog/admin/bloggers/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("bloggerDataTable");
    Document.screenshotElement(".DTE_Action_Create", "/redactor/apps/blog/blogger_create.png");
});