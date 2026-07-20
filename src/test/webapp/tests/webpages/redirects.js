Feature('webpages.redirects');

let randomNumber;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/settings/redirect/");

    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('redirects-zakladne testy @baseTest', async ({ I, DataTables }) => {
    I.see("Stará URL");
    await DataTables.baseTest({
        dataTable: 'redirectTable',
        requiredFields: ['oldUrl', 'newUrl'],
        perms: 'cmp_redirects'
    });
});

Scenario('export import testovanie', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();
    I.waitForElement(".dt-buttons button[data-dtbtn=export]");
    I.click({ css: '.dt-buttons button[data-dtbtn=export]' });
    I.click('Exportovať');
    I.refreshPage();
    I.wait(2);

    DT.waitForLoader();
    I.waitForElement(".dt-buttons button[data-dtbtn=export]");
    I.click({ css: '.dt-buttons button[data-dtbtn=export]' });
    //toto sposobi, ze zostane otvoreny dialog na vyber suboru
    //I.click({ css: 'input[aria-describedby=insert-file]' });
});

Scenario('Filter by actual domain', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/settings/redirect/");

    //demotest.webjetcms.sk is selected by default

    //No domain
    DT.filterContains("oldUrl", "/slovensky/");
    I.see("681");
    I.see("/slovensky/");
    I.see("/sk12345/");
    I.see("302");

    //Actual domain
    DT.filterContains("domainName", I.getDefaultDomainName());
    DT.filterContains("oldUrl", "/images/drag-drop-test/lighthouse.jpg");
    I.see("8387");
    I.see("/images/drag-drop-test/lighthouse.jpg");
    I.see("/images/drag-drop/lighthouse.jpg");
    I.see("301");

    //Other domain
    DT.filterContains("domainName", "mirroring.tau27.iway.sk");
    DT.filterContains("oldUrl", "");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Change domain
    Document.switchDomain("mirroring.tau27.iway.sk");

    //No domain -> same record ... should be visible everywhere, because domain is not set
    DT.filterContains("oldUrl", "/slovensky/");
    I.see("681");
    I.see("/slovensky/");
    I.see("/sk12345/");
    I.see("302");

    //This should not be seen in this domain
    DT.filterContains("oldUrl", "/images/drag-drop-test/lighthouse.jpg");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //This domain specific record
    DT.filterContains("oldUrl", "/files/sk_test/test_sub_sk/podadresar_test_sk/test_priecinok/logo1.jpg");
    I.see("4688");
    I.see("/files/sk_test/test_sub_sk/podadresar_test_sk/test_priecinok/logo1.jpg");
    I.see("/files/sk_test/test_sub_sk/podadresar_test_sk/test/test_priecinok/logo1.jpg");
    I.see("mirroring.tau27.iway.sk");
});

Scenario('odhlasenie', ({ I }) => {
    I.amOnPage('/logoff.do?forward=/admin/');
});

/**
 * Test onlyNew import - import only new records
 */
Scenario('XLS import onlynew', async ({I, DT, DTE}) => {

    var oldUrl = "/oldurl-onlynew-135";
    var oldUrlSecond = "/oldurl-onlynew-2";
    var newUrl = "/oldurl-onlynew-164";
    var updateBy = "Stará URL - oldUrl";
    var excelFile = "tests/components/redirects-onlynew.xlsx";

    //
    I.say("delete all old/failed records");
    DT.filterContains("oldUrl", "/oldurl-onlynew-");

    //
    I.say("Delete old data");
    var totalRows = await I.getTotalRows();
    if (totalRows > 0) {
        DT.deleteAll();
    }

    I.dontSee(oldUrl, "#datatableInit_wrapper .dt-scroll-body");

    //
    I.say("Import excel as APPEND")
    I.clickCss("div.dt-buttons button.btn-import-dialog");
    DTE.waitForModal("datatableImportModal");

    I.attachFile('#insert-file', excelFile);
    I.waitForEnabled("#submit-import", 5);
    I.clickCss("#submit-import");

    DT.waitForLoader();

    //Check inserted names, records and statuses
    I.waitForText(oldUrl, 15, "#datatableInit_wrapper .dt-scroll-body");
    I.see(oldUrlSecond, "#datatableInit_wrapper .dt-scroll-body");
    I.see(newUrl+"-xls", "#datatableInit_wrapper .dt-scroll-body");


    //
    I.say("Edit newUrl");
    DT.filterContains("oldUrl", oldUrl);
    I.click(oldUrl, "#datatableInit_wrapper .dt-scroll-body");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_newUrl", newUrl+"-edited");
    DTE.save();

    DT.waitForLoader();
    I.dontSee(newUrl+"-xls", "#datatableInit_wrapper .dt-scroll-body");

    //
    I.say("Delete second row to verify it will be imported again");
    DT.filterContains("oldUrl", oldUrlSecond);
    I.see(oldUrlSecond, "#datatableInit_wrapper .dt-scroll-body");
    DT.deleteAll();
    I.dontSee(oldUrlSecond, "#datatableInit_wrapper .dt-scroll-body");

    //
    I.say("Reimport in onlyNew mode");
    I.clickCss("div.dt-buttons button.btn-import-dialog");
    DTE.waitForModal("datatableImportModal");

    I.attachFile('#insert-file', excelFile);
    I.waitForEnabled("#submit-import", 5);
    I.click("Importovať iba nové záznamy");
    I.waitForVisible("#dt-import-update-by-column");
    I.click({ css: "button[data-id=dt-settings-update-by-column]" });
    I.waitForElement(locate('div.dropdown-menu.show .dropdown-item').withText(updateBy), 5);
    I.forceClick(locate('div.dropdown-menu.show .dropdown-item').withText(updateBy));
    I.wait(0.5);
    I.clickCss("#submit-import");

    DT.waitForLoader();
    DT.filterContains("oldUrl", "/oldurl-onlynew-");

    //Check inserted names, records and statuses
    I.waitForText(oldUrl, 15, "#datatableInit_wrapper .dt-scroll-body");
    I.see(newUrl+"-edited", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee(newUrl+"-xls", "#datatableInit_wrapper .dt-scroll-body");
    I.see(oldUrlSecond, "#datatableInit_wrapper .dt-scroll-body");


    //refresh data
    I.clickCss("div.dt-buttons button.buttons-refresh");
    DT.waitForLoader();
    I.waitForText(oldUrl, 15, "#datatableInit_wrapper .dt-scroll-body");
    I.see(newUrl+"-edited", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee(newUrl+"-xls", "#datatableInit_wrapper .dt-scroll-body");
    I.see(oldUrlSecond, "#datatableInit_wrapper .dt-scroll-body");

    //
    I.say("Delete old data");
    DT.filterContains("oldUrl", "/oldurl-onlynew-");
    DT.deleteAll();
 });

 //TODO: testy funkcnosti presmerovani
 Scenario('Test 404.jsp', ({ I }) => {

    //url redirects from to
    let redirects = [
        ["/admin/editor.do", "/admin/v9/webpages/web-pages-list/"],
        ["/admin/webpages/", "/admin/v9/webpages/web-pages-list/"],
        ["/components/tooltip/admin_list.jsp", "/apps/tooltip/admin/"],
        ["/admin/", "/admin/v9/"],
        ["/admin", "/admin/v9/"],
        ["/novy-adresar-01/volne-polia/", "/novy-adresar-01/volitelne-polia/"],
        ["/novy-adresar-01/volne-polia", "/novy-adresar-01/volitelne-polia/"]
    ];

    //iterate over redirects array and test each redirect
    redirects.forEach(redirect => {
        I.amOnPage(redirect[0]);
        I.seeInCurrentUrl(redirect[1]);
    });

    // regexp:^\/archiv\/(.+) -> /files/archiv/$1
    let regexpRedirects = [
        ["/archiv/part", "/files/archiv/part"],
        ["/archiv/part/", "/files/archiv/part/"],
        ["/archiv/2021/01/01/12345", "/files/archiv/2021/01/01/12345"]
    ];

    regexpRedirects.forEach(redirect => {
        I.amOnPage(redirect[0]);
        I.seeInCurrentUrl(redirect[1]);
    });

 });

const baseUrl = "/tseer/ai-buttons-test.html";
const toRedirectUrl_prefix = "/tseer/ai-buttons-test-redirected-"
Scenario('Test publish, validation logic WITHOUT cache', ({ I, DT, DTE, Document }) => {
    const toRedirectUrl = toRedirectUrl_prefix + randomNumber + "-no_cache.html";
    testPublishingAndValidity(I, DT, DTE, Document, toRedirectUrl, false);
});

Scenario('Test publish, validation logic WITH cache', ({ I, DT, DTE, Document }) => {
    const toRedirectUrl = toRedirectUrl_prefix + randomNumber + "-with_cache.html";
    testPublishingAndValidity(I, DT, DTE, Document, toRedirectUrl, true);
});

Scenario('Post publish, validation', async ({ I, DT, Document }) => {
    I.say("Set redirect cache to false");
    Document.setConfigValue('cacheUrlRedirects', false);

    I.say("Remove autotest redirects");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterStartsWith("oldUrl", toRedirectUrl_prefix + randomNumber);
    const rowCount = await I.grabNumberOfVisibleElements('#datatableInit > tbody > tr > td.dt-row-edit');
    if(rowCount > 0) {
        I.clickCss("button.buttons-select-all");
        I.clickCss("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waiForLoader();
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
});

function testPublishingAndValidity(I, DT, DTE, Document, toRedirectUrl, useCache) {
    const shift5m = (5 * 60 * 1000);

    Document.setConfigValue('cacheUrlRedirects', useCache);

    I.say("Test url's");
        I.amOnPage(baseUrl);
        I.see("AI BUTTONS TEST");

        I.amOnPage(toRedirectUrl);
        I.see("Chyba 404 - požadovaná stránka neexistuje");

    I.say("Create redirect bean");
        I.amOnPage("/admin/v9/settings/redirect/");
        I.clickCss("button.buttons-create");
        DTE.waitForEditor();

        I.fillField("#DTE_Field_oldUrl", toRedirectUrl);
        I.fillField("#DTE_Field_newUrl", baseUrl);
        DTE.save();

    I.say("Test redirect works");
        I.amOnPage(toRedirectUrl);
        I.seeInCurrentUrl(baseUrl);
        I.see("AI BUTTONS TEST");

    I.say("Set valid publish in PAST - redirection must work");
        updateEntityAndTest(I, DT, DTE, toRedirectUrl, (new Date()).getTime() - shift5m, null, true);

    I.say("Set valid publish in FUTURE - redirection must NOT work");
        updateEntityAndTest(I, DT, DTE, toRedirectUrl, (new Date()).getTime() + shift5m, null, false);

    I.say("Set valid to in PAST - redirection must NOT work");
        updateEntityAndTest(I, DT, DTE, toRedirectUrl, null, (new Date()).getTime() - shift5m, false);

    I.say("Set valid to in FUTURE - redirection must work");
        updateEntityAndTest(I, DT, DTE, toRedirectUrl, null, (new Date()).getTime() + shift5m, true);

    I.say("Delete redirect and test it do not work");
        I.amOnPage("/admin/v9/settings/redirect/");
        DT.filterEquals("oldUrl", toRedirectUrl);

        I.clickCss("td.dt-select-td.sorting_1");
        I.clickCss("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.dontSee(toRedirectUrl);

        I.amOnPage(toRedirectUrl);
        I.seeInCurrentUrl(toRedirectUrl);
        I.see("Chyba 404 - požadovaná stránka neexistuje");
}

function updateEntityAndTest(I, DT, DTE, toRedirectUrl, publishDate, validToDate, shouldRedirect) {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterEquals("oldUrl", toRedirectUrl);
    I.click(toRedirectUrl);
    DTE.waitForEditor();

    if(publishDate !== null) {
        I.fillField("#DTE_Field_publishDate", I.formatDateTime(publishDate) );
    } else {
        I.fillField("#DTE_Field_publishDate", "" );
    }
    I.clickCss(".DTE_Field_Name_oldUrl");

    if(validToDate != null) {
        I.fillField("#DTE_Field_validTo", I.formatDateTime(validToDate) );
    } else {
        I.fillField("#DTE_Field_validTo", "" );
    }
    I.clickCss(".DTE_Field_Name_oldUrl");

    DTE.save();

    if(shouldRedirect === true) {
        I.amOnPage(toRedirectUrl);
        I.seeInCurrentUrl(baseUrl);
        I.see("AI BUTTONS TEST");
    } else {
        I.amOnPage(toRedirectUrl);
        I.seeInCurrentUrl(toRedirectUrl);
        I.see("Chyba 404 - požadovaná stránka neexistuje");
    }
}

function createRedirect(I, DTE, oldUrl, newUrl) {
    I.clickCss("#datatableInit_wrapper button.buttons-create");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_oldUrl", oldUrl);
    I.fillField("#DTE_Field_newUrl", newUrl);
    DTE.save();
}

function createDatedRedirect(I, DTE, oldUrl, newUrl, publishDate, validTo) {
    I.clickCss("#datatableInit_wrapper button.buttons-create");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_oldUrl", oldUrl);
    I.fillField("#DTE_Field_newUrl", newUrl);
    if (publishDate !== null) {
        I.fillField("#DTE_Field_publishDate", I.formatDateTime(publishDate));
        I.clickCss(".DTE_Field_Name_oldUrl");
    }
    if (validTo !== null) {
        I.fillField("#DTE_Field_validTo", I.formatDateTime(validTo));
        I.clickCss(".DTE_Field_Name_oldUrl");
    }
    DTE.save();
}

Scenario('Redirect cleaning preview and execution @singlethread @current', async ({ I, DT, DTE, Document }) => {

    // Use an isolated named domain. The unnamed scope is intentionally excluded from this E2E cleanup.

    Document.switchDomain("test23.tau27.iway.sk");

    const cleanupPrefix = "/autotest-redirect-clearing-";
    const prefix = cleanupPrefix + randomNumber;
    const finalUrl = "/tseer/ai-buttons-test.html";
    const regexOldUrl = "regexp:^" + prefix + "-ignored-regex/(.+)";
    const regexNewUrl = prefix + "-ignored-regex-target/$1";
    const publishDateOldUrl = prefix + "-ignored-publish-date";
    const publishDateNewUrl = prefix + "-ignored-publish-date-target";
    const validToOldUrl = prefix + "-ignored-valid-to";
    const validToNewUrl = prefix + "-ignored-valid-to-target";
    const futureDate = Date.now() + (24 * 60 * 60 * 1000);

    I.say("Remove leftover redirect cleaning test data");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterContains("oldUrl", cleanupPrefix);
    if (await I.getTotalRows() > 0) {
        DT.deleteAll();
    }

    I.say("Verify there are no unrelated redirect cleaning actions");
    I.amOnPage("/admin/v9/settings/redirect-clearing/");
    DT.waitForLoader("redirectClearingTable");
    I.seeElement("#redirectClearingIncludeUnnamed");
    I.dontSeeCheckboxIsChecked("#redirectClearingIncludeUnnamed");
    I.clickCss("#redirectClearingTable_wrapper button.buttons-analyze-redirects");
    I.waitForText("Analýza dokončená: 0 aktualizácií, 0 zmazaní", 40, "#toast-container-webjet");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 40, "#redirectClearingTable_wrapper");
    I.seeElement("#redirectClearingTable_wrapper button.buttons-execute-clearing.disabled");
    const initialSummary = await I.grabTextFrom("#toast-container-webjet");
    const ignoredCountMatch = initialSummary.match(/(\d+) ignorovaných záznamov/);
    I.assertTrue(ignoredCountMatch !== null, "The analysis summary must contain the ignored redirect count");
    const initialIgnoredCount = Number(ignoredCountMatch[1]);
    I.toastrClose();

    I.say("Create actionable and ignored redirects");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();
    createRedirect(I, DTE, prefix + "-old", prefix + "-old-first");
    createRedirect(I, DTE, prefix + "-old", prefix + "-old-final");
    createRedirect(I, DTE, prefix + "-duplicate", finalUrl);
    createRedirect(I, DTE, prefix + "-duplicate", finalUrl);
    createRedirect(I, DTE, prefix + "-chain-a", prefix + "-chain-b");
    createRedirect(I, DTE, prefix + "-chain-b", finalUrl);
    createRedirect(I, DTE, prefix + "-cycle-a", prefix + "-cycle-b");
    createRedirect(I, DTE, prefix + "-cycle-b", prefix + "-cycle-a");
    createRedirect(I, DTE, regexOldUrl, regexNewUrl);
    createDatedRedirect(I, DTE, publishDateOldUrl, publishDateNewUrl, futureDate, null);
    createDatedRedirect(I, DTE, validToOldUrl, validToNewUrl, null, futureDate);

    I.say("Analyze and verify the read-only preview");
    I.amOnPage("/admin/v9/settings/redirect-clearing/");
    DT.waitForLoader("redirectClearingTable");
    I.dontSeeElement("#redirectClearingTable_wrapper button.buttons-create");
    I.clickCss("#redirectClearingTable_wrapper button.buttons-analyze-redirects");
    I.waitForText(prefix + "-old-first", 40, "#redirectClearingTable_wrapper");
    I.see("Zmazať starú verziu", "#redirectClearingTable_wrapper");
    I.see("Zmazať duplikát", "#redirectClearingTable_wrapper");
    I.see("Skrátiť reťazec", "#redirectClearingTable_wrapper");
    I.see("Zmazať krok cyklu", "#redirectClearingTable_wrapper");
    I.waitForText((initialIgnoredCount + 3) + " ignorovaných záznamov", 40, "#toast-container-webjet");
    I.dontSee(regexOldUrl, "#redirectClearingTable_wrapper");
    I.dontSee(publishDateOldUrl, "#redirectClearingTable_wrapper");
    I.dontSee(validToOldUrl, "#redirectClearingTable_wrapper");
    I.seeElement("#redirectClearingTable_wrapper button.buttons-execute-clearing:not(.disabled)");
    I.dontSeeCheckboxIsChecked("#redirectClearingIncludeUnnamed");

    I.say("Reopen the page and verify the shared cached preview");
    I.amOnPage("/admin/v9/settings/redirect-clearing/");
    DT.waitForLoader("redirectClearingTable");
    I.waitForText(prefix + "-old-first", 40, "#redirectClearingTable_wrapper");
    I.dontSeeCheckboxIsChecked("#redirectClearingIncludeUnnamed");
    I.seeElement("#redirectClearingTable_wrapper button.buttons-execute-clearing:not(.disabled)");

    I.say("Execute the complete snapshot");
    I.clickCss("#redirectClearingTable_wrapper button.buttons-execute-clearing");
    I.waitForText("Vykonať čistenie presmerovaní?", 10, "#toast-container-webjet");
    I.see("Zmení sa 1 a zmaže 3 záznamov", "#toast-container-webjet");
    I.click("Potvrdiť", "div.toastr-buttons");
    I.waitForText("Čistenie dokončené", 40, "#toast-container-webjet");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 40, "#redirectClearingTable_wrapper");

    I.say("Verify the resulting redirects in the main table and in a request");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterEquals("oldUrl", prefix + "-chain-a");
    I.waitForText(finalUrl.substring(1), 10, "#datatableInit_wrapper");
    I.amOnPage(prefix + "-chain-a");
    I.seeInCurrentUrl(finalUrl);

    I.say("Verify ignored redirects remain unchanged");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterEquals("oldUrl", regexOldUrl);
    I.waitForText(prefix.substring(1) + "-ignored-regex-target", 10, "#datatableInit_wrapper");
    DT.filterEquals("oldUrl", publishDateOldUrl);
    I.waitForText(publishDateNewUrl.substring(1), 10, "#datatableInit_wrapper");
    DT.filterEquals("oldUrl", validToOldUrl);
    I.waitForText(validToNewUrl.substring(1), 10, "#datatableInit_wrapper");

    I.say("Delete redirect cleaning test data");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterContains("oldUrl", cleanupPrefix);
    if (await I.getTotalRows() > 0) {
        DT.deleteAll();
    }
});

Scenario('Redirect cleaning permissions @current', ({ I, DT }) => {
    DT.checkPerms("cmp_redirects", "/admin/v9/settings/redirect-clearing/", "redirectClearingTable");

    // logout to refresh domain to default one
    I.logout();
});
