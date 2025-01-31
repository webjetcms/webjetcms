Feature('admin.api-auth');

let basicAuthEnabled = null;

Before(({ I }) => {
    I.logout();
});

function setApiTokenAuthConf(I, Document, allowed=true) {
    I.relogin("admin");
    Document.setConfigValue("springSecurityAllowedAuths", allowed ? "basic,api-token" : "basic");
    I.logout();
}

async function isBasicAuthEnabled(I) {
    if (basicAuthEnabled!==null) return basicAuthEnabled;

    let response = await I.sendGetRequest('/rest/basic-auth-enabled', {
        'x-auth-token': ''
    });
    I.say("basic-auth-allowed: "+response.status+" "+response.data.result);
    basicAuthEnabled = response.data.result;
    I.say("Basic auth is "+basicAuthEnabled);
    //to see it in the console even for npm run all
    console.log("Basic auth is "+basicAuthEnabled);
    return basicAuthEnabled;
}

Scenario("API volanie", ({ I, Document }) => {

    setApiTokenAuthConf(I, Document, true);

    I.amOnPage("/admin/v9/");
    I.see("PRIHLÁSENIE");
    I.see("Zabudnuté heslo");

    //default x-auth-token je v codecept.conf.js v casti REST
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsKeys(['numberOfElements', 'totalPages']);
    I.seeResponseContainsJson({
        content: [
            {
                "id":11,
                "groupId":25,
                "title":"Produktová stránka"
            }
        ]
    });

    //po API pristupne nesmie zostat prihlaseny
    I.say("Testujem, ze nezostal prihlaseny");
    I.amOnPage("/admin/v9/");
    I.see("PRIHLÁSENIE");
    I.see("Zabudnuté heslo");
});

Scenario("API volanie - disabled", async ({ I, Document }) => {

    await isBasicAuthEnabled(I);

    setApiTokenAuthConf(I, Document, false);

    I.amOnPage("/admin/v9/");
    I.see("PRIHLÁSENIE");
    I.see("Zabudnuté heslo");

    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25');
    if (basicAuthEnabled) I.seeResponseCodeIs(401);
    else I.seeResponseCodeIs(403);
});

Scenario("API token auth - reset", ({ I, Document }) => {
    setApiTokenAuthConf(I, Document, true);
});

Scenario("API volanie zle heslo @singlethread", async ({ I }) => {
    await isBasicAuthEnabled(I);

    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25', {
        'x-auth-token': 'dGVzdGVyOmNrTzxIfXRid05bTEldXGx3OURUa2szQ1pOVnJ+Njg8'
    });
    if (basicAuthEnabled) I.seeResponseCodeIs(401);
    else I.seeResponseCodeIs(403);

    I.say("Testing logon blocking");
    I.wait(2);
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25', {
        'x-auth-token': 'aaaksjdhfkashdflaksdhj'
    });
    if (basicAuthEnabled) I.seeResponseCodeIs(401);
    else I.seeResponseCodeIs(403);

    I.wait(10);

    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25');
    I.seeResponseCodeIs(200);

});

Scenario("API volanie zle heslo-cakanie 10s @singlethread", ({ I }) => {
    //po zlom hesle musime 10s cakat, aby sa dalo prihlasit v dalsom scenari
    I.wait(10);
});

Scenario("Prihlasenie cez wjlogontoken @singlethread", ({ I }) => {
    I.amOnPage("/admin/v9/");
    I.see("PRIHLÁSENIE");
    I.see("Zabudnuté heslo");

    //I.sendGetRequest('/admin/v9/', {'x-auth-token': '','wjlogontoken': 'FdGVzdGVyOlpaVlZydUNBdEhuM3F5Yzg|'});
    //I.seeResponseCodeIs(200);

    I.executeScript(function() {
        $.ajax({
            url: "/admin/v9",
            headers: {
                'wjlogontoken': 'FdGVzdGVyOlpaVlZydUNBdEhuM3F5Yzg|'
            }
        });
    });

    //musime cakat kym dobehne ajax na prihlasenie
    I.wait(5);

    I.amOnPage("/admin/v9/");
    I.dontSee("PRIHLÁSENIE");
    I.dontSee("Zabudnuté heslo");
    I.see("Vitajte, Tester Playwright");

    I.logout();

    //zle zadane heslo
    I.executeScript(function() {
        $.ajax({
            url: "/admin/v9",
            headers: {
                'wjlogontoken': 'FdXxxxxXXdGVyOlpaVlZydUNBdEhuM3F5Yzg|'
            }
        });
    });

    I.wait(5);

    I.amOnPage("/admin/v9/");
    I.see("PRIHLÁSENIE");
    I.see("Zabudnuté heslo");
    I.dontSee("Vitajte, Tester Playwright");
});

Scenario("wjlogontoken volanie zle heslo-cakanie 10s @singlethread", ({ I }) => {
    //po zlom hesle musime 10s cakat, aby sa dalo prihlasit v dalsom scenari
    I.wait(10);
});

async function getResponse(I, password) {
    let username = "tester";
    //send get request with basic auth
    let response = await I.sendGetRequest('/private/rest/demo-test', {
        'x-auth-token': '',
        'Authorization': 'Basic ' + Buffer.from(username + ':' + password).toString('base64')
    });
    I.say("Response status: "+response.status);
    return response;
}

function setBasicAuthConf(I, Document, allowed=true) {
    I.relogin("admin");
    Document.setConfigValue("springSecurityAllowedAuths", allowed ? "basic,api-token" : "api-token");
    I.logout();
}

Scenario("basic auth @singlethread", async ({ I, Document }) => {

    await isBasicAuthEnabled(I);

    let password = secret("*********");

    //
    I.say("Testing basic auth");
    response = await getResponse(I, password);

    if (basicAuthEnabled===true) {
        I.seeResponseCodeIs(200);
        I.assertContain(response.data.result, "Demo OK", "Response JSON is not correct");
    } else {
        I.seeResponseCodeIs(403);
        I.assertContain(response.data, "<title>403</title>", "Response is not 403 forbidden");
    }

    //
    I.say("Testing basic auth - wrong password");
    response = await getResponse(I, "wrongpassword");
    if (basicAuthEnabled===true) I.seeResponseCodeIs(401);
    else I.seeResponseCodeIs(403);
    I.assertContain(response.data, "<title>403</title>", "Response is not 403 forbidden");

    //
    I.say("wrong password wait 10s");
    I.wait(10);

    //
    I.say("change basic auth for next run, it requires restart of the server");
    setBasicAuthConf(I, Document, !basicAuthEnabled);
});

async function sendPostRequest(I, id, referer) {
    let response = await I.sendPostRequest('/private/rest/demo-post',
        {
            "id": id,
            "title": "autotest"
        }, {
            'Referer': referer,
            'content-type': 'application/json'
        }
    );
    //console.log(response);
    return response;
}

Scenario("Referer test with POST", async ({ I, Document }) => {
    let id = Math.floor(Math.random() * 1000);

    let referer = await I.grabCurrentUrl();
    I.say("Referer: "+referer);

    //send post request to /private/rest/demo-test-post with random ID parameter
    let response = await sendPostRequest(I, id, referer);
    I.assertContain(response.data.result, "Demo OK", "Response JSON is not correct");
    I.assertEqual(response.data.id, id, "Response ID is not correct");

    //add subdomain to referer and test again
    referer = referer.replace("://", "://subdomain.");
    I.say("Referer with subdomain: "+referer);
    response = await sendPostRequest(I, id, referer);
    I.assertEqual(response.status, 403, "Response status is not forbidden");

    //
    referer = "http://www.fejsbuuk.com/admin/v9/";
    I.say("not valid referer"+referer);
    response = await sendPostRequest(I, id, referer);
    I.assertEqual(response.status, 403, "Response status is not forbidden");

    //
    referer = "";
    I.say("empty referer"+referer);
    response = await sendPostRequest(I, id, referer);
    I.assertEqual(response.status, 403, "Response status is not forbidden");
});