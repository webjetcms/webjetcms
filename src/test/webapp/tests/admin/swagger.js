Feature("admin.swagger");

Before(({ I, login }) => {
    login('admin');
});

Scenario("base tests", ({I}) => {
    I.amOnPage("/admin/swagger-ui/index.html");
    I.waitForText("WebJet API", 60, "h2.title");
    I.amOnPage("/v2/api-docs");
    I.waitForText("WebJet API", 60);

    I.logout();
    I.amOnPage("/admin/swagger-ui/index.html");
    I.dontSee("WebJet API");
    I.see("Chyba 404");
    I.amOnPage("/v2/api-docs");
    I.dontSee("WebJet API");
    I.see("Chyba 404");

    //TODO: try non admin login
});