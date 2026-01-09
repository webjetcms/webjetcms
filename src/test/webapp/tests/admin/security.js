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
    I.waitForElement("textarea[name=newText]", 5);

    var xss = "1</textarea><ScRiPt >kxyQ(9031)</ScRiPt>";

    I.fillField("textarea[name=oldText]", xss);
    I.fillField("textarea[name=newText]", xss);
    I.click("input[name=replace]");

    I.waitForText("Text "+xss+" nahradeny", 10, "div.content-wrapper");
    I.waitForText("za "+xss+" v data_asc", 10, "div.content-wrapper");

    I.waitForValue("textarea[name=oldText]", xss, 10);
    I.waitForValue("textarea[name=newText]", xss, 10);
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

Scenario("pathFilterBlockedPaths", ({ I, Document }) => {
    let random = I.getRandomTextShort();
    let pathFilterBlockedPaths = ".DS_Store,debug.,config.properties,Thumbs.db,.git,.svn,/WEB-INF/,./";
    //set default values
    Document.setConfigValue("pathFilterBlockedPaths", pathFilterBlockedPaths);

    I.amOnPage("/templates/aceintegration/jet/config.properties?v=1-"+random);
    I.see("Chyba 404 - požadovaná stránka neexistuje");

    I.amOnPage("/WEB-INF./web.xml");
    I.see("Chyba 404 - požadovaná stránka neexistuje");

    //allow config.properties
    Document.setConfigValue("pathFilterBlockedPaths", pathFilterBlockedPaths.replace("config.properties", ""));

    I.amOnPage("/templates/aceintegration/jet/config.properties?v=2-"+random);
    I.see("ninjaDebug=false");

    //restore default
    Document.setConfigValue("pathFilterBlockedPaths", pathFilterBlockedPaths);
});