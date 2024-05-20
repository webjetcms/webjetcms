Feature('apps.file_archiv');

Scenario("Archív súborov - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/archiv-suborov/");
    I.waitForElement(".documents");
    I.seeElement(locate("h2").withText(("Dokumenty")));

    within(".documents > ul", () => {
        I.seeElement( locate("li > a.download-link > span.media-body").withText("WebJET CMS Marketing Promo") );
        I.seeElement( locate("li > a.download-link > span.media-body").withText("WebJET CMS Manuals") );
        I.seeElement( locate("li > a.download-link > span.media-body").withText("WebJET CMS FAQ") );
    });

    I.click( locate("li > a.download-link > span.media-body").withText("WebJET CMS Marketing Promo") );
    I.dontSee("Chyba 404 - požadovaná stránka neexistuje");
});