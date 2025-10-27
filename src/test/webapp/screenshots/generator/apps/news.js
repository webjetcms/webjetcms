Feature('apps.news');

Before(({ login }) => {
    login('admin');
});

Scenario('novinky', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader();

    I.clickCss("#groupSelect_wrapper > div > button");
    I.waitForElement(".dropdown-menu.show .dropdown-menu.inner.show");
    I.click( locate("a.dropdown-item > span").withText("/English/News") );
    DT.waitForLoader();

    Document.screenshot("/redactor/apps/news/admin-dt.png");

    I.click("McGregor sales force");
    DTE.waitForEditor("newsDataTable");
    I.wait(10);
    I.toastrClose();
    Document.screenshot("/redactor/apps/news/admin-edit.png");

    //editor
    Document.screenshotAppEditor(10, "/redactor/apps/news/editor-dialog.png", function(Document, I, DT, DTE) {
        I.clickCss("#pills-dt-component-datatable-perex-tab");
        Document.screenshot("/redactor/apps/news/editor-dialog-perex.png");

        I.clickCss("#pills-dt-component-datatable-templates-tab");
        I.scrollTo(locate("label.custom-template").withChild(locate("span").withText("novinky")));
        I.click( locate("label.custom-template").withChild(locate("span").withText("novinky")) );
        Document.screenshot("/redactor/apps/news/editor-dialog-templates.png");

        I.clickCss("#pills-dt-component-datatable-filter-tab");
        addFilter(I, "AUTHOR_ID", "<=", "123");
        addFilter(I, "DATE_CREATED", "=", "01.10.2025");
        addFilter(I, "DATA", "Začína na", "This is first ");
        addFilter(I, "AVAILABLE", "=", "false");
        I.clickCss("td.valueTd > input");
        Document.screenshot("/redactor/apps/news/editor-dialog-filter.png");

        I.clickCss("#pills-dt-component-datatable-news-tab");
        Document.screenshot("/redactor/apps/news/editor-dialog-newslist.png");

        I.clickCss("#pills-dt-component-datatable-basic-tab");
    }, 1280, 800);

    I.amOnPage("/zo-sveta-financii/?NO_WJTOOLBAR");
    Document.screenshot("/redactor/apps/news/news.png");
});

function addFilter(I, docField, operator, value) {
    I.say("Adding filter");
    I.clickCss("button.btn-success");
    I.selectOption( locate("#filtersTable > tbody > tr:last-child").find("select.fieldSelect") , docField);
    I.selectOption( locate("#filtersTable > tbody > tr:last-child").find("td.operatorTd > select") , operator);
    if(value != null) {
        if ("true" === value || "false" === value) {
            I.selectOption( locate("#filtersTable > tbody > tr:last-child").find("td.valueTd > select") , value);
        } else {
            I.fillField( locate("#filtersTable > tbody > tr:last-child").find("td.valueTd > input") , value);
        }
    }
}
