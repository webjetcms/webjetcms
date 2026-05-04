Feature("admin.swagger");

var restUrl = "/admin/rest/openapi/api-docs";
var swaggerUrl = "/admin/swagger-ui/index.html";
var title = "WebJET CMS API";

Before(({ I, login }) => {
    login('admin');
});

Scenario("base tests", async ({I, Document}) => {
    Document.setConfigValue("swaggerEnabled", "true");

    I.amOnPage(swaggerUrl);
    I.waitForText(title, 60, "h1.title");
    I.waitForElement(locate("button.opblock-summary-control").withText("/admin/rest/web-pages/{id}"), 60);

    I.amOnPage("/apps/prihlaseny-pouzivatel/zakaznicka-zona/csrf-logoff.html");
    let csrfToken = await I.grabValueFrom("form[name=userLogoffForm] input[name='__token']");
    I.say("CSRF token: "+csrfToken);
    let referer = await I.grabCurrentUrl();
    I.say("Referer: "+referer);
    let sessionId = (await I.grabCookie("JSESSIONID")).value;
    I.say("Session ID: "+sessionId);

    let response = await I.sendGetRequest(restUrl, {
        'Referer': referer,
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId,
        'X-CSRF-Token': csrfToken
    });
    I.seeResponseCodeIs(200);
    I.assertContain(response.data.openapi, "3.0", "OpenAPI version is not correct");
    I.assertContain(response.data.info.title, title, "API title is not correct");

    //not logged user
    I.logout();
    I.amOnPage(swaggerUrl);
    I.dontSee(title);
    I.see("Chyba 404");

    response = await I.sendGetRequest(restUrl, {
        'Referer': referer,
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId,
        'X-CSRF-Token': csrfToken
    });
    I.seeResponseCodeIs(404);

    //disable swagger
    I.relogin("admin");
    Document.setConfigValue("swaggerEnabled", "false");

    I.amOnPage(swaggerUrl);
    I.see("Chyba 404");
    response = await I.sendGetRequest(restUrl, {
        'Referer': referer,
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId,
        'X-CSRF-Token': csrfToken
    });
    I.seeResponseCodeIs(404);

    Document.setConfigValue("swaggerEnabled", "true");
});

Scenario("no permissions", async ({I, Document}) => {
    Document.setConfigValue("swaggerEnabled", "true");

    //try user without permissions
    I.relogin("tester2");

    I.amOnPage("/apps/prihlaseny-pouzivatel/zakaznicka-zona/csrf-logoff.html");
    let csrfToken = await I.grabValueFrom("form[name=userLogoffForm] input[name='__token']");
    I.say("CSRF token: "+csrfToken);
    let referer = await I.grabCurrentUrl();
    I.say("Referer: "+referer);
    let sessionId = (await I.grabCookie("JSESSIONID")).value;
    I.say("Session ID: "+sessionId);

    I.amOnPage(swaggerUrl);
    I.see("Chyba 404");
    await I.sendGetRequest(restUrl, {
        'Referer': referer,
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId,
        'X-CSRF-Token': csrfToken
    });
    I.seeResponseCodeIs(404);
});

Scenario("logout", ({I}) => {
    I.logout();
});