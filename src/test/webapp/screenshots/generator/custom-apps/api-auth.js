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

    I.amOnPage("/admin/swagger-ui/#/webpages-rest-controller/editUsingPOST_85");
    I.wait(10);

    Document.screenshot("/custom-apps/spring/swagger.png");
});