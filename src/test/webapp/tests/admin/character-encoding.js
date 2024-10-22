Feature('admin.character-encoding');

Before(({ I }) => {
    I.logout();
});

var testText = "text ľščťžýáíéäôúň koniec";

function testApp(I, url, isLogged, isJsp) {
    I.amOnPage(url);

    if (isJsp) I.see("JSP version", "h2");
    else I.see("HTML version", "h2");

    I.fillField("input[name=name]", testText);
    I.click("div.container button.btn-primary");
    I.see(testText, "p.name-request");
    I.see(testText, "p.name-model");
    I.see("UTF-8", "p.encoding");
    if (isLogged) {
        I.see("18", "p.userId");
    } else {
        I.see("-1", "p.userId");
    }

    if (isJsp) I.see("JSP version", "h2");
    else I.see("HTML version", "h2");
}

Scenario("thymeleaf filter", ({ I }) => {
    const url = "/apps/spring-app/character-encoding-thymeleaf.html";

    testApp(I, url, false, false);

    I.relogin("admin");

    testApp(I, url, true, false);
});

Scenario("jsp filter", ({ I }) => {
    const url = "/apps/spring-app/character-encoding-jsp.html";

    testApp(I, url, false, true);

    I.relogin("admin");

    testApp(I, url, true, true);
});

function testApiApp(I, data, isLogged) {
    I.say("Testing logged=" + isLogged);
    let i = data.indexOf('<p class="name-request">');
    if (i>0) data = data.substring(i);
    i = data.indexOf('</article>');
    if (i>0) data = data.substring(0, i).trim();

    I.assertContain(data, '<p class="name-request">'+testText+'</p>');
    I.assertContain(data, '<p class="name-model">'+testText+'</p>');
    I.assertContain(data, '<p class="encoding">UTF-8</p>');
    if (isLogged) {
        I.assertContain(data, '<p class="userId">18</p>');
    } else {
        I.assertContain(data, '<p class="userId">-1</p>');
    }
}

Scenario("API token auth @singlethread", async ({ I }) => {
    const url = '/apps/spring-app/character-encoding-thymeleaf.html?name=' + encodeURIComponent(testText);
    let response = await I.sendGetRequest(url);
    testApiApp(I, response.data, true);

    response = await I.sendGetRequest(url, {
        'x-auth-token': 'dGVzdGVyOmNrTzxIfXRid05bTEldXGx3OURUa2szQ1pOVnJ+Njg8'
    });
    testApiApp(I, response.data, false);
});

Scenario("API call wrong password wait 10s @singlethread", ({ I }) => {
    //after wrong password we must wait 10s to be able to log in in the next scenario
    I.wait(10);
});

Scenario("REST api call", ({ I }) => {
    const url = '/demo-test?name=' + encodeURIComponent(testText);
    I.amOnPage(url);
    I.see(testText, "p.name-request");
});

Scenario("404 page", ({ I }) => {
    const url = '//apps/spring-app/not-exist-url?name=' + encodeURIComponent(testText);
    I.amOnPage(url);
    I.waitForText("Stránka nenájdená ľščťžýáíé", 10, "article p");
    I.see(testText, "article p.name-param");
});