Feature('apps.news');

Before(({ login }) => {
    login('admin');
});

Scenario('novinky', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader();

    I.clickCss("#groupId_extfilter > div > div > div > button.dropdown-toggle");
    I.waitForElement("body > div.bs-container.dropdown.bootstrap-select > div.dropdown-menu");
    I.click( locate("a.dropdown-item > span").withText("/English/News") );
    DT.waitForLoader();

    Document.screenshot("/redactor/apps/news/admin-dt.png");

    I.click("McGregor sales force");
    DTE.waitForEditor("newsDataTable");
    Document.screenshot("/redactor/apps/news/admin-edit.png");

    //editor
    Document.screenshotAppEditor(10, "/redactor/apps/news/editor-dialog.png", function(Document, I, DT, DTE) {
        I.clickCss("#tabLink2");
        I.scrollTo("div[data-key='news.template.news01']");
        Document.screenshot("/redactor/apps/news/editor-dialog-templates.png");

        I.clickCss("#tabLink3");
        Document.screenshot("/redactor/apps/news/editor-dialog-perex.png");

        I.clickCss("#tabLink4");
        Document.screenshot("/redactor/apps/news/editor-dialog-filter.png");

        I.clickCss("#tabLink5");
        Document.screenshot("/redactor/apps/news/editor-dialog-newslist.png");

        I.clickCss("#tabLink1");
    }, 1280, 800);

    I.amOnPage("/zo-sveta-financii/?NO_WJTOOLBAR");
    Document.screenshot("/redactor/apps/news/news.png");
});
