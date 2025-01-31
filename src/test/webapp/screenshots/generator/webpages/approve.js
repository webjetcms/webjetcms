Feature('webpages.approve');

Before(({ I, login }) => {
    login('admin');
});

Scenario('approving', ({I, DT, Document}) => {

    if("sk" === I.getConfLng()) {
        I.amOnPage("/admin/approve.jsp?historyid=6951&docid=6031");
    } else if("en" === I.getConfLng()) {
        I.amOnPage("/admin/approve.jsp?docid=81853&historyid=129353");
    } else if("cs" === I.getConfLng()) {
        I.amOnPage("/admin/approve.jsp?docid=100207&historyid=164174")
    }
    
    Document.screenshot("/redactor/webpages/approve/approve-form.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.click("#pills-waiting-tab");
    DT.waitForLoader();
    Document.screenshot("/redactor/webpages/approve/approve-tab.png");

});