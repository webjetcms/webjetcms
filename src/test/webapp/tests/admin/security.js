Feature('admin.security');

Before(({ I, login }) => {
    login('admin');
});

Scenario('kontrola vyzadovania CSRF tokenu pri REST volaniach', ({ I }) => {
    //TODO: po uprave na vratenie 403 toto failne, zatial som nenasiel sposob aby I.amOnPage nefaillo
    /*
    I.amOnPage("/admin/rest/refresher");
    I.see("HTTP ERROR 403");
    //aby zbehol nasledne korektne test prihlaseneho usera
    I.amOnPage("/admin/v9/");
    */
});

Scenario('kontrola URL s bodkociarkou', ({ I }) => {
    I.logout();

    I.amOnPage("/admin/mem.jsp");
    I.see("java.runtime.name");

    I.amOnPage("/;/admin/mem.jsp");
    I.see("Chyba 404 - požadovaná stránka neexistuje");

    I.amOnPage("/admin/;mem.jsp");
    I.see("Chyba 404 - požadovaná stránka neexistuje");

    I.amOnPage("/admin/;/mem.jsp");
    I.see("Chyba 404 - požadovaná stránka neexistuje");

    I.amOnPage("/files/archiv/;/mem.jsp");
    I.see("Chyba 404 - požadovaná stránka neexistuje");
});

Scenario('mozne XSS v textarea replaceall', ({ I }) => {
    I.amOnPage("/admin/replaceall.jsp");

    var xss = "1</textarea><ScRiPt >kxyQ(9031)</ScRiPt>";

    I.fillField("textarea[name=oldText]", xss);
    I.fillField("textarea[name=newText]", xss);
    I.click("input[name=replace]");

    I.seeInField("textarea[name=oldText]", xss);
    I.seeInField("textarea[name=newText]", xss);
});

Scenario('mozne XSS v textarea replaceall-db', ({ I }) => {
    I.amOnPage("/admin/replaceall-db.jsp");

    var xss = "1</textarea><ScRiPt >kxyQ(9031)</ScRiPt>";

    I.fillField("textarea[name=oldText]", xss);
    I.fillField("textarea[name=newText]", xss);
    I.click("input[name=replace]");

    I.seeInField("textarea[name=oldText]", xss);
    I.seeInField("textarea[name=newText]", xss);

    I.see("Text "+xss+" nahradeny");
    I.see("za "+xss+" v data_asc");
});

Scenario("Ninja URL with double quotes", ({ I }) => {
    var link = `/files/archiv/filter-xss/file.pdf”><audio>">`;
    I.amOnPage(link);
    I.see("Chyba 404 - požadovaná stránka neexistuje");
    I.see("archiv-xss-404".toUpperCase(), "h1");
    I.dontSeeInSource(link);
    //check properly escaped URL in source
    I.seeInSource(`/files/archiv/filter-xss/file.pdf%E2%80%9D%3E%3Caudio%3E%22%3E`)
});