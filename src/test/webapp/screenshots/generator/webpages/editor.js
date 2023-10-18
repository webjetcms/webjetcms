Feature('webpages.editor');

Before(({ I, login }) => {
    login('admin');
});

Scenario('editor', ({I, DT, Document}) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
    Document.screenshot("/redactor/webpages/editor/tab-content.png");

    I.clickCss("#pills-dt-datatableInit-basic-tab");
    Document.screenshot("/redactor/webpages/editor/tab-basic.png");

    I.clickCss("#pills-dt-datatableInit-menu-tab");
    Document.screenshot("/redactor/webpages/editor/tab-menu.png");

    I.clickCss("#pills-dt-datatableInit-access-tab");
    Document.screenshot("/redactor/webpages/editor/tab-access.png");

    I.clickCss("#pills-dt-datatableInit-perex-tab");
    Document.screenshot("/redactor/webpages/editor/tab-perex.png");

});