Feature('apps.search');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

function searchTestMultidomain(I, isTest23, searchUrl="/apps/vyhladavanie/") {

    const testText9 = "Toto je search test result demotest.webjetcms.sk searchtestresult";
    const testText23 = "Toto je search test result test23.tau27.iway.sk searchtestresult";

    I.amOnPage(searchUrl);

    I.waitForElement("#searchWords");
    I.fillField('#searchWords', 'searchtestresult');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Približný počet výsledkov: 1", 10, ".totalResults");

    if (isTest23) {
        I.dontSee(testText9, "div.search");
        I.see(testText23, "div.search");
    } else {
        I.see(testText9, "div.search");
        I.dontSee(testText23, "div.search");
    }
}

/**
 * There was a bug where the search was not working properly when the user was in a multidomain environment
 * because the search was using filtration by file_name, which if every website has /English as root will
 * return results from all domains.
 */
Scenario("BUG search - multidomain", ({ I, Document }) => {
    searchTestMultidomain(I, false);

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    Document.switchDomain("test23.tau27.iway.sk");

    searchTestMultidomain(I, true);

    I.logout();
});

Scenario("BUG search - multidomain - lucene", ({ I, Document }) => {
    searchTestMultidomain(I, false, "/apps/vyhladavanie/vyhladavanie-lucene.html");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    Document.switchDomain("test23.tau27.iway.sk");

    searchTestMultidomain(I, true, "/apps/vyhladavanie/vyhladavanie-lucene.html");

    I.logout();
});

Scenario("BUG search - multidomain - logout", ({ I }) => {
    I.logout();
});