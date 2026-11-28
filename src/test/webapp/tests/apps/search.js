Feature('apps.search');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Vyhľadávanie - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/vyhladavanie/");

    I.waitForElement("#searchWords");
    I.fillField('#searchWords', 'Kontakt');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Približný počet výsledkov", 20);

    I.fillField('#searchWords', 'Nothing');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Časový limit medzi dvoma hľadaniami je 10 sekúnd. Počkajte a skúste vyhľadanie neskôr.", 10);

    I.wait(11);
    I.fillField('#searchWords', 'Nothing');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Neboli nájdené žiadne stránky vyhovujúce zadaným kritériám.", 10);
});

async function submitLuceneSearch(I) {
    I.waitForVisible("form.smallSearchForm #searchWords");
    I.fillField("form.smallSearchForm #searchWords", "Kontakt");
    I.clickCss("form.smallSearchForm .smallSearchSubmit");
    I.waitForVisible(".searchResultsH1");
    const url = new URL(await I.grabCurrentUrl());
    if (url.pathname === "/showdoc.do") I.assertEqual(url.searchParams.get("docid"), "77875");
    else I.assertEqual(url.pathname, "/apps/vyhladavanie/");
}

Scenario("Lucene autocomplete returns JSON suggestions", async ({ I }) => {
    async function getSuggestions(query) {
        const result = await I.executeScript(async query => {
            const response = await fetch("/components/search/ac.jsp?" + query);
            return { status: response.status, type: response.headers.get("content-type"), suggestions: await response.json() };
        }, query);
        I.assertEqual(result.status, 200, query);
        I.assertStartsWith(result.type, "application/json", query);
        return result.suggestions;
    }

    for (const query of ["", "lang=sk", "words=kontakt", "lang=sk&words=", "words=kontakt&lang=%20"]) {
        I.assertDeepEqual(await getSuggestions(query), [], query);
    }
    I.assertDeepEqual(await getSuggestions("lang=sk&words=zzzzzzzzzzzzzzzz"), []);
    const suggestions = await getSuggestions("lang=sk&words=webjett");
    I.assertTrue(Array.isArray(suggestions));
    for (const suggestion of suggestions) I.assertEqual(typeof suggestion, "string", suggestion);
});

Scenario("Lucene form submits to the current document without a previous page", async ({ I }) => {
    // Open the component before visiting a public page that would set last_doc_id.
    I.amOnPage("/components/search/lucene_search.jsp?docid=77875");
    await submitLuceneSearch(I);
});

Scenario("Lucene preview paginates on the current document and submits to the configured one", async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=116601");
    DTE.waitForEditor();
    await DTE.fillCkeditor("!INCLUDE(/components/search/lucene_search.jsp, perpage=1, rootGroup=1+15257, resultsDocId=77875)!");
    I.clickCss("button.btn-preview");
    await Document.waitForTab();
    I.switchToNextTab();
    I.executeScript(() => window.location.assign("/admin/webpages/preview/?docid=116601&words=WebJET&index=0"));
    I.clickCss(".search .navigation a[href*='index=1']");
    I.waitForElement(".search .navigation a[href*='index=0']");
    I.seeInCurrentUrl("/admin/webpages/preview/");
    I.seeInCurrentUrl("docid=116601");
    await submitLuceneSearch(I);
    I.closeCurrentTab();
});

Scenario("Lucene pagination keeps the current document", async ({ I }) => {
    // The session points to a different document than the one being rendered.
    I.amOnPage("/apps/vyhladavanie/vyhladavanie-lucene.html");
    I.executeScript(() => window.location.assign("/components/search/lucene_search.jsp?docid=77875&rootGroup=1%2B15257&perpage=1&words=WebJET&index=1"));
    const navigation = ".search .navigation a";
    I.waitForElement(navigation);
    const pageUrl = await I.grabCurrentUrl();
    const action = await I.grabAttributeFrom("form.smallSearchForm", "action");
    const expectedDocIds = new URL(action, pageUrl).pathname === "/showdoc.do" ? ["77875"] : [];
    const links = await I.grabAttributeFromAll(navigation, "href");
    I.assertLengthOf(links, 2);
    for (const href of links) {
        const url = new URL(href, pageUrl);
        I.assertDeepEqual(url.searchParams.getAll("docid"), expectedDocIds);
    }
    I.clickCss(navigation + "[href*='index=2']");
    I.waitForVisible(".searchResultsH1");
    I.seeInCurrentUrl("index=2");
});

Scenario('testovanie app - Vyhladavanie', async ({ I, DTE, Apps, Document }) => {
    I.closeOtherTabs();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=77875');
    DTE.waitForEditor();
    I.wait(5);

    const defaultParams = {
        rootGroup: '1+15257',
        perpage: '10',
        orderType: 'sortPriority',
        order: 'asc',
        sForm: 'complete'
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement('#searchWords', 10);
    I.fillField('#searchWords', 'Kontakt');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Približný počet výsledkov: 1", 10);

    I.switchToPreviousTab();
    I.closeOtherTabs();
    I.switchTo();

    Apps.openAppEditor();

    const changedParams = {
        rootGroup: '15257',
        perpage: '2',
        orderType: 'title',
        order: 'desc',
        sForm: 'form'
    };

    I.click(locate('.btn-webjet-jstree-item-remove').first());
    DTE.fillField('perpage', changedParams.perpage);
    DTE.clickSwitch('checkDuplicity_0');
    DTE.selectOption('orderType','Názvu');
    DTE.clickSwitch('order_1');
    DTE.clickSwitch('sForm_0');
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    DTE.save()
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=77875');
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement('#searchWords', 10);
    I.fillField('#searchWords', 'Kontakt');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForElement('#searchWords', 10);
    I.dontSee("Približný počet výsledkov: 1");

    I.closeOtherTabs();
});

Scenario('Revert changes', async ({ I, DTE, Apps }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=77875');
    DTE.waitForEditor();
    Apps.clearPageContent();
    const snippet = '!INCLUDE(/components/search/search.jsp, rootGroup=1+15257, perpage=10, checkDuplicity=false, orderType=sortPriority, order=asc, sForm=complete)!'
    await DTE.fillCkeditor(snippet);
    DTE.save();
});

function searchTestMultidomain(I, isTest23, searchUrl="/apps/vyhladavanie/") {

    const testText9 = "Toto je search test result "+I.getDefaultDomainName()+" searchtestresult";
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

    I.wait(10);
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
