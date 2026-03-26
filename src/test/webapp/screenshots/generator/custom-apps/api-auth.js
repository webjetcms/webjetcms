Feature('api-auth');

Before(({ I, login }) => {
    login('admin');
});

Scenario('key generation and swagger', ({ I, DTE, Document }) => {

    I.amOnPage("/admin/v9/users/user-list/?id=2");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_apiKey", "*");
    DTE.save();
    I.moveCursorTo("div.toast-title");
    Document.screenshot("/custom-apps/spring/api-key-notification.png");

    I.amOnPage("/admin/swagger-ui/index.html#/WebpagesRestController");
    I.wait(10);
    I.click(locate("button.opblock-summary-control").withText("/admin/rest/web-pages/{id}"));
    I.wait(2);

    Document.screenshot("/custom-apps/spring/swagger.png", 1280, 1200);
});