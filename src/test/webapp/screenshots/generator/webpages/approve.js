Feature('webpages.approve');

Before(({ I, login }) => {
    login('admin');
});

Scenario('approving', ({I, DT, Document}) => {

    I.amOnPage("/admin/approve.jsp?historyid=6951&docid=6031");
    Document.screenshot("/redactor/webpages/approve/approve-form.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.click("#pills-waiting-tab");
    Document.screenshot("/redactor/webpages/approve/approve-tab.png");

});