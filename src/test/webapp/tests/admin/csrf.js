Feature('admin.csrf');

Scenario('logoffRequireCsrfToken', async ({ I, Document }) => {
    I.relogin("admin");
    Document.setConfigValue("logoffRequireCsrfToken", "true");
    I.amOnPage("/admin/v9/");
    I.waitForText("Vitajte, Tester Playwright", 10, ".overview__dashboard__title h2");

    //
    I.say("Direct link will not work");
    I.amOnPage("/logoff.do?forward=/admin/logon/");
    I.waitForText("Vitajte, Tester Playwright", 10, ".overview__dashboard__title h2");

    //
    I.say("wrong csrf token");
    await I.executeScript(function() {
        document.adminLogoffForm["__token"].value = "aaaa"
    });
    I.click(".js-logout-toggler");
    I.waitForText("Vitajte, Tester Playwright", 10, ".overview__dashboard__title h2");

    //
    I.say("click on button to correctly logoff");
    I.click(".js-logout-toggler");
    I.waitForText("Prihlásenie", 10, "h3.form-title");

    //
    I.say("Test OLD v8 JSP admin page");
    I.relogin("admin");
    I.amOnPage("/components/aceintegration/admin-example.jsp")

    I.say("click on button to correctly logoff");
    I.click(".js-logout-toggler");
    I.waitForText("Prihlásenie", 10, "h3.form-title");

    //
    I.relogin("admin");
    I.say("Webpage logoff link using !CSRF_INPUT! macro");
    I.amOnPage("/apps/prihlaseny-pouzivatel/zakaznicka-zona/csrf-logoff.html");
    I.waitForElement("#logoffButtonInput", 10);
    I.click("#logoffButtonInput");
    I.waitForText("Zadajte vaše prihlasovacie údaje", 10, "article .container");

    //
    I.relogin("admin");
    I.say("Webpage logoff link using !CSRF_TOKEN! macro");
    I.amOnPage("/apps/prihlaseny-pouzivatel/zakaznicka-zona/csrf-logoff.html");
    I.waitForElement("#logoffButtonToken", 10);
    I.click("#logoffButtonToken");
    I.waitForText("Zadajte vaše prihlasovacie údaje", 10, "article .container");

    //
    I.relogin("admin");
    I.say("Webpage logoff link using !CSRF_TOKEN! macro - invalid token");
    I.amOnPage("/apps/prihlaseny-pouzivatel/zakaznicka-zona/csrf-logoff.html");
    I.waitForElement("#logoffButtonToken", 10);
    await I.executeScript(function() {
        document.userLogoffForm["__token"].value = "aaaa"
    });
    I.click("#logoffButtonToken");
    I.waitForText("text sa zobrazí len prihlásenému používateľovi.", 10);
});

Scenario('logoffRequireCsrfToken - reset', async ({ I, Document }) => {
    I.amOnPage("/admin/v9/");

    //test if we are logged or not
    let numOfElements = await I.grabNumberOfVisibleElements('.js-logout-toggler');
    I.say("Number of logout buttons: "+numOfElements);

    if (numOfElements < 1) I.relogin("admin");

    Document.setConfigValue("logoffRequireCsrfToken", "false");
});

async function verifyCsrfToken(I, isCorrect, urlParameter=null, headers={}) {
    var url = "/private/rest/demo-test";
    if (urlParameter) url += "?"+urlParameter;

    I.say("   verifyCsrfToken: url="+url+" headers="+JSON.stringify(headers));

    let response = await I.sendGetRequest(url, headers);
    //console.log(response);

    I.say("   response: status="+response.status+" data="+JSON.stringify(response.data));

    if (isCorrect) {
        I.seeResponseCodeIs(200);
        I.assertContain(response.data.result, "Demo OK", "Response JSON is not correct");
    } else {
        I.seeResponseCodeIs(403);
    }
}

Scenario('csrfRequiredUrls', async ({ I, Document }) => {
    I.relogin("admin");
    I.amOnPage("/admin/v9/");
    Document.setConfigValue("csrfRequiredUrls", "");

    //
    I.say("Test rest service without CSRF token");
    await verifyCsrfToken(I, true);

    //
    I.say("Require CSRF token");
    Document.setConfigValue("csrfRequiredUrls", "/private/rest\n/logoff.do");

    await verifyCsrfToken(I, false);

    //
    I.say("Grab current CSRF token");
    I.amOnPage("/apps/prihlaseny-pouzivatel/zakaznicka-zona/csrf-logoff.html");
    let csrfToken = await I.grabValueFrom("form[name=userLogoffForm] input[name='__token']");
    I.say("CSRF token: "+csrfToken);

    let referer = await I.grabCurrentUrl();
    I.say("Referer: "+referer);

    I.amOnPage("/components/aceintegration/admin-example.jsp");
    let sessionId = (await I.grabCookie("JSESSIONID")).value;
    I.say("Session ID: "+sessionId);

    //auth token empty to use current session because of csrfToken
    I.say("Test rest service with CSRF token in URL parameter");
    await verifyCsrfToken(I, true, "__token="+csrfToken, {
        'Referer': referer,
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId
    });
    await verifyCsrfToken(I, false, "__token=aaaa", {
        'Referer': referer,
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId
    });

    //
    I.say("Test rest service with CSRF token in header");
    await verifyCsrfToken(I, true, null, {
        'Referer': referer,
        'x-csrf-token': csrfToken,
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId
    });
    await verifyCsrfToken(I, false, null, {
        'Referer': referer,
        'x-csrf-token': "aaa",
        'x-auth-token': '',
        'Cookie': "JSESSIONID="+sessionId
    });

    //
    I.say("Verify also logoff.do require CSRF token");
    I.amOnPage("/admin/v9/settings/configuration/");
    await I.executeScript(function() {
        document.adminLogoffForm["__token"].value = "aaaa"
    });
    I.click(".js-logout-toggler");
    I.waitForText("požadovaná stránka neexistuje", 10);
});

Scenario('csrfRequiredUrls - reset', async ({ I, Document }) => {
    I.amOnPage("/admin/v9/");

    //test if we are logged or not
    let numOfElements = await I.grabNumberOfVisibleElements('.js-logout-toggler');
    I.say("Number of logout buttons: "+numOfElements);

    if (numOfElements < 1) I.relogin("admin");

    Document.setConfigValue("csrfRequiredUrls", "");
});