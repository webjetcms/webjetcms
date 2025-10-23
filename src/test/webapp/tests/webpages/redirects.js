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