Feature('webpages.redirects');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/settings/redirect");
    //pause();
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

Scenario('Filter by actual doimain', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/settings/redirect/");

    //demotest.webjetcms.sk is selected by default

    //No domain
    DT.filter("oldUrl", "/slovensky/");
    I.see("681");
    I.see("/slovensky/");
    I.see("/sk12345/");
    I.see("302");

    //Actual domain
    DT.filter("domainName", "demotest.webjetcms.sk");
    DT.filter("oldUrl", "/images/drag-drop-test/lighthouse.jpg");
    I.see("8387");
    I.see("/images/drag-drop-test/lighthouse.jpg");
    I.see("/images/drag-drop/lighthouse.jpg");
    I.see("301");

    //Other domain
    DT.filter("domainName", "mirroring.tau27.iway.sk");
    DT.filter("oldUrl", "");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Change domain
    Document.switchDomain("mirroring.tau27.iway.sk");

    //No domain -> same record ... should be visible everzwhere, because domain is not set
    DT.filter("oldUrl", "/slovensky/");
    I.see("681");
    I.see("/slovensky/");
    I.see("/sk12345/");
    I.see("302");

    //This should not be seen in this domain
    DT.filter("oldUrl", "/images/drag-drop-test/lighthouse.jpg");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //This domain specific record
    DT.filter("oldUrl", "/files/sk_test/test_sub_sk/podadresar_test_sk/test_priecinok/logo1.jpg");
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
    DT.filter("oldUrl", "/oldurl-onlynew-");

    //
    I.say("Delete old data");
    var totalRows = await I.getTotalRows();
    if (totalRows > 0) {
        DT.deleteAll();
    }

    I.dontSee(oldUrl, "#datatableInit_wrapper .dataTables_scrollBody");

    //
    I.say("Import excel as APPEND")
    I.clickCss("div.dt-buttons button.btn-import-dialog");
    DTE.waitForModal("datatableImportModal");

    I.attachFile('#insert-file', excelFile);
    I.waitForEnabled("#submit-import", 5);
    I.clickCss("#submit-import");

    DT.waitForLoader();

    //Check inserted names, records and statuses
    I.waitForText(oldUrl, 15, "#datatableInit_wrapper .dataTables_scrollBody");
    I.see(oldUrlSecond, "#datatableInit_wrapper .dataTables_scrollBody");
    I.see(newUrl+"-xls", "#datatableInit_wrapper .dataTables_scrollBody");


    //
    I.say("Edit newUrl");
    DT.filter("oldUrl", oldUrl);
    I.click(oldUrl, "#datatableInit_wrapper .dataTables_scrollBody");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_newUrl", newUrl+"-edited");
    DTE.save();

    DT.waitForLoader();
    I.dontSee(newUrl+"-xls", "#datatableInit_wrapper .dataTables_scrollBody");

    //
    I.say("Delete second row to verify it will be imported again");
    DT.filter("oldUrl", oldUrlSecond);
    I.see(oldUrlSecond, "#datatableInit_wrapper .dataTables_scrollBody");
    DT.deleteAll();
    I.dontSee(oldUrlSecond, "#datatableInit_wrapper .dataTables_scrollBody");

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
    DT.filter("oldUrl", "/oldurl-onlynew-");

    //Check inserted names, records and statuses
    I.waitForText(oldUrl, 15, "#datatableInit_wrapper .dataTables_scrollBody");
    I.see(newUrl+"-edited", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee(newUrl+"-xls", "#datatableInit_wrapper .dataTables_scrollBody");
    I.see(oldUrlSecond, "#datatableInit_wrapper .dataTables_scrollBody");


    //refresh data
    I.clickCss("div.dt-buttons button.buttons-refresh");
    DT.waitForLoader();
    I.waitForText(oldUrl, 15, "#datatableInit_wrapper .dataTables_scrollBody");
    I.see(newUrl+"-edited", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee(newUrl+"-xls", "#datatableInit_wrapper .dataTables_scrollBody");
    I.see(oldUrlSecond, "#datatableInit_wrapper .dataTables_scrollBody");

    //
    I.say("Delete old data");
    DT.filter("oldUrl", "/oldurl-onlynew-");
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