Feature('admin.api-auth');

Before(({ I }) => {
    I.logout();
});

Scenario("API volanie", ({ I }) => {
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

Scenario("API volanie zle heslo @singlethread", ({ I }) => {
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25', {
        'x-auth-token': 'dGVzdGVyOmNrTzxIfXRid05bTEldXGx3OURUa2szQ1pOVnJ+Njg8'
    });
    I.seeResponseCodeIs(401);

    I.say("Testing logon blocking");
    I.wait(2);
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25', {
        'x-auth-token': 'aaaksjdhfkashdflaksdhj'
    });
    I.seeResponseCodeIs(401);

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

