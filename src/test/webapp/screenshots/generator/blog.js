Feature('apps.blog');

const section = path => '#SomStromcek a[title="' + path + '"]';

Scenario('Blog articles', ({ I, DTE, Document, DT }) => {
    I.relogin("bloggerPerm");
    I.amOnPage("/apps/blog/admin/");

    I.waitForElement('#SomStromcek [id="-1_anchor"].jstree-clicked');
    DT.waitForLoader("bloggerArticlesDataTable");
    Document.screenshot("/redactor/apps/blog/blogger-blog.png");
    Document.screenshotElement(".tree-col", "/redactor/apps/blog/groupFilter_defaultValue.png");

    I.clickCss(section("/Aplikácie/Blog/bloggerPerm"));
    I.pressKey("ArrowRight");
    I.waitForElement(section("/Aplikácie/Blog/bloggerPerm/Nezaradené"));
    I.clickCss('#SomStromcek [id="-1_anchor"]');
    Document.screenshotElement(".tree-col", "/redactor/apps/blog/groupFilter_allValues.png");

    I.clickCss("button.buttons-add-folder");
    I.waitForElement("#toast-container-webjet > .toast-warning");
    I.moveCursorTo("#toast-container-webjet > .toast-warning");
    Document.screenshotElement("#toast-container-webjet > .toast-warning", "/redactor/apps/blog/adding_folder_warning.png");

    I.toastrClose();
    I.clickCss(section("/Aplikácie/Blog/bloggerPerm"));
    I.clickCss("button.buttons-add-folder");
    DTE.waitForEditor("blogSectionTable");
    DTE.save("blogSectionTable");
    I.waitForElement("#blogSectionTable_modal .DTE_Field_Name_groupName.is-invalid");
    Document.screenshotElement("#blogSectionTable_modal .modal-content", "/redactor/apps/blog/adding_folder_error.png");
    DTE.cancel("blogSectionTable");

    I.clickCss("button.buttons-add-folder");
    DTE.waitForEditor("blogSectionTable");
    I.fillField("#blogSectionTable_modal #DTE_Field_groupName", "NewSubFolder-autotest");
    Document.screenshotElement("#blogSectionTable_modal .modal-content", "/redactor/apps/blog/adding_folder_info.png");
    Document.screenshotElement("#blogSectionTable_modal .DTE_Footer button.btn-primary", "/redactor/apps/blog/adding_folder_info_button.png");
    DTE.save("blogSectionTable", true);

    I.waitForElement("#toast-container-webjet > .toast-success");
    I.moveCursorTo("#toast-container-webjet > .toast-success");
    Document.screenshotElement("#toast-container-webjet > .toast-success", "/redactor/apps/blog/adding_folder_success.png");

    I.waitForElement(section("/Aplikácie/Blog/bloggerPerm/NewSubFolder-autotest"));
    I.clickCss(section("/Aplikácie/Blog/bloggerPerm/NewSubFolder-autotest"));
    DT.waitForLoader("bloggerArticlesDataTable");
    Document.screenshotElement(".tree-col", "/redactor/apps/blog/groupFilter_allValues_withNew.png");

    I.say("remove added folder");
    I.relogin("tester");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=63847");
    I.jstreeClick("NewSubFolder-autotest");
    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.clickCss("div.DTE_Action_Remove div.DTE_Footer div.DTE_Form_Buttons button.btn-primary");
    DTE.waitForLoader();
    I.dontSeeElement(locate("a.jstree-anchor").withText("NewSubFolder-autotest"));

    I.logout();
    I.amOnPage("/apps/blog/blogger/webjet-cms/");
    Document.screenshot("/redactor/apps/blog/blog-news-list.png");
    I.amOnPage("/apps/blog/blogger/webjet-cms/nova-verzia-seo-aplikacie.html");
    Document.screenshot("/redactor/apps/blog/blog-page-detail.png", 1280, 1100);

    I.relogin("blogger");
    I.amOnPage("/apps/blog/admin/?docId=81958");
    DTE.waitForEditor("bloggerArticlesDataTable");
    Document.screenshot("/redactor/apps/blog/editor-text.png");
    I.clickCss("#pills-dt-bloggerArticlesDataTable-perex-tab");
    Document.screenshot("/redactor/apps/blog/editor-perex.png");
});

Scenario('Blog bloggers', ({ I, DT, DTE, Document }) => {
    I.relogin("tester");
    I.amOnPage("/apps/blog/admin/bloggers/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("bloggerDataTable");
    Document.screenshotElement(".DTE_Action_Create", "/redactor/apps/blog/blogger_create.png");
});