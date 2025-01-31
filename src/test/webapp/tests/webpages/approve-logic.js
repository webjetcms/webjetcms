Feature('webpages.approve-logic');

var randomNumber;
var deletePrefix = "[DELETE] ";
var changePostfix = "_change";
var awaitingApproveFolderLink = "/admin/v9/webpages/web-pages-list/?groupid=5343";

var urlCakajuceNaSchvalenieZmenaTitulku = "/admin/v9/webpages/web-pages-list/?groupid=5343";

Before(({ I }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('creating/editing pages that need to be approved - logic', async ({I, DTE, Document, DT}) => {
    var pageNameA = "onelevel-approve-autotest-" + randomNumber;
    var pageNameB = pageNameA + changePostfix;

    /* PART OF CREATING AND APPROVING logic tests */

    I.say("Creating NEW page")
        //Login as tester, no admin
        I.relogin("tester2");
        I.say("Logged as Tester2");

        I.amOnPage(awaitingApproveFolderLink);

        I.click(DT.btn.add_button);
        setPageName(I, DTE, pageNameA, true);

        //Save
        DTE.save();
        Document.notifyCheckAndClose("Žiadosť o schválenie vytvorenia novej web stránky dostal: Tester Playwright");

    I.say("Until we are logged, we must see page");
        DT.filterContains("title", pageNameA);
        I.amOnPage("/test-stavov/cakajuce-schvalenie-zmena-titulku/" + pageNameA.toLowerCase() + ".html");
        I.see(pageNameA.toUpperCase());

    I.say("Logoff and check that page return 404 , because its not published yet");
        checkCreatedPage(I, pageNameA, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", false, null);

    //Switch to admin user
    I.relogin("admin");
    I.say("Logged as admin (Tester)");

    I.amOnPage(awaitingApproveFolderLink);

    I.say("Check that page is waiting for approve");
        checkWaitingTab(I, DT, pageNameA, true);
    I.click(pageNameA);

    approvePage(I, pageNameA);

    I.amOnPage(awaitingApproveFolderLink);
    checkWaitingTab(I, DT, pageNameA, false);

    //
    I.say("Check that page was approved by admin");
        I.clickCss("#pills-pages-tab");
        I.click(pageNameA);
        DTE.waitForEditor();
        I.clickCss("#pills-dt-datatableInit-history-tab");
        I.waitForElement("#pills-dt-datatableInit-history");
        await within("#pills-dt-datatableInit-history", () => {
            DT.filterContains("historyApprovedByName", "Tester Playwright");
            I.see(pageNameA);
            I.see("Tester Playwright");

            //Check that disapproved is not set
            DT.filterContains("historyDisapprovedByName", "Tester Playwright");
            I.see("Nenašli sa žiadne vyhovujúce záznamy");
        });
        DTE.cancel();

    I.say("Logoff and check that page is still visible (was published)");
        checkCreatedPage(I, pageNameA, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    /* PART OF EDITING AND DisAPPROVING logic tests */

    //log as no admin
    I.say("Edit page name");
        I.relogin("tester2");
        I.amOnPage(awaitingApproveFolderLink);
        DT.filterContains("title", pageNameA);
        I.see(pageNameA);
        I.click(pageNameA);
        setPageName(I, DTE, pageNameB);
        //Save
        DTE.save();
        Document.notifyCheckAndClose("Žiadosť o schválenie web stránky dostal: Tester Playwright");

    I.say("Check that page hasnt change");
        checkCreatedPage(I, pageNameA, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    I.say("Check history tab of page");
        //Log as admin
        I.relogin("admin");
        I.amOnPage(awaitingApproveFolderLink);
        DT.filterContains("title", pageNameA);
        I.see(pageNameA);
        I.click(pageNameA);
        DTE.waitForEditor();
        Document.notifyCheckAndClose("Existuje rozpracovaná alebo neschválená verzia tejto stránky. Skontrolujte kartu História.");
        I.click("#pills-dt-datatableInit-history-tab", null, { position: { x: 0, y: 0 } }); //because after toastr close cursor stays in close button tooltip

        within("#pills-dt-datatableInit-history", () => {
            DT.filterContains("historyApprovedByName", "neschválené");
            I.see(pageNameA);
        });
        DTE.cancel();

    I.say("Check waiting tab");
        checkWaitingTab(I, DT, pageNameB, true);

    I.say("Go Dis-Approved page");
        I.click(pageNameB);
        disApprovePage(I, pageNameB);

    I.amOnPage(awaitingApproveFolderLink);
    checkWaitingTab(I, DT, pageNameB, false);

    I.say("Check history tab again (that history record is Dis-Approved corretly)");
    I.say("Beware, we are still working with name pageNameA because pageNameB was rejected");
        I.clickCss("#pills-pages-tab");
        DT.filterContains("title", pageNameA);
        I.click(pageNameA);
        DTE.waitForEditor();
        Document.notifyCheckAndClose("Existuje rozpracovaná alebo neschválená verzia tejto stránky. Skontrolujte kartu História.");
        I.click("#pills-dt-datatableInit-history-tab", null, { position: { x: 0, y: 0 } }); //because after toastr close cursor stays in close button tooltip

        within("#pills-dt-datatableInit-history", () => {
            DT.filterContains("historyDisapprovedByName", "Tester Playwright");
            I.see(pageNameB);
        });

    I.say("Logoff and check page");
        checkCreatedPage(I, pageNameA, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    //Delete page as admin(approver), so no approve is needed
    I.say("Delete page");

    I.relogin('admin');
    I.amOnPage(awaitingApproveFolderLink);

    deletePage(I, DT, pageNameA, true);

    I.say("Check that page was deleted");
        checkCreatedPage(I, pageNameA, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", false, null);

});

/**
 * Try and verify creating / editing page in folder that dont need approve.
 * Try it as NON-Admin !! (admin has auto-approve so we need to test it as non-admin)
 */
Scenario('creating/editing/deleting page thatDONT need to be approved - logic', async ({I, DTE, DT}) => {
    var folderName = "Aplikácie";
    var subFolderName = "Schvaľovanie stránok";
    var newPageName = "not-reqired-approve-autotest-" + randomNumber;

    /* TEST creating non - approve page */

    I.say("Create page");
        //Logg as NON-Admin
        I.relogin("tester2");
        //Set folder
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
        I.jstreeNavigate([folderName, subFolderName]);
        DT.waitForLoader();
        //Add new page
        I.click(DT.btn.add_button);
        setPageName(I, DTE, newPageName, true);
        //Save
        DTE.save();
        I.say("Logoff and check that page is still visble (auto publish because its without approve)");
            checkCreatedPage(I, newPageName, "/apps/schvalovanie-stranok/", true, null);

    //
    I.say("Logg as admin and do checks");
        I.relogin('admin');
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
        I.jstreeNavigate([folderName, subFolderName]);
        I.say("Check its not waiting or approve");
            checkWaitingTab(I, DT, newPageName, false);

    //
    I.say("Check that page was auto - approved (Better say, not approved at all) !!");
    I.say("Admin has auto - approve (thats diff)");
        I.clickCss("#pills-pages-tab");
        I.click(newPageName);
        DTE.waitForEditor();
        I.clickCss("#pills-dt-datatableInit-history-tab");

        within("#pills-dt-datatableInit-history", () => {
            DT.filterContains("historyApprovedByName", "neschvaľovalo sa");
            I.see(newPageName);
            I.see("Tester2 Playwright2");
        });

        DTE.cancel();

    I.logout();

    /* TEST editing non - approve page */

    I.say("Edit page");
        I.relogin("tester2");
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
        I.jstreeNavigate([folderName, subFolderName]);
        //Change
        DT.filterContains("title", newPageName);
        I.click(newPageName);
        setPageName(I, DTE, newPageName + changePostfix);
        //Save change
        DTE.save();
        I.dontSee("Žiadosť o schválenie web stránky dostal");
        I.say("Logoff and check change");
            checkCreatedPage(I, newPageName, "/apps/schvalovanie-stranok/", true, (newPageName + changePostfix));

    //
    I.say("Log as admin and do checks");
    I.relogin("admin");
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
    I.jstreeNavigate([folderName, subFolderName]);

    //
    I.say("Chck that is no waiting for approve");
    checkWaitingTab(I, DT, newPageName + changePostfix, false);

    I.clickCss("#pills-pages-tab");
    I.click(newPageName + changePostfix);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");

    within("#pills-dt-datatableInit-history", () => {
        DT.filterContains("historyApprovedByName", "neschvaľovalo sa");
        I.see(newPageName + changePostfix);
        I.see("Tester2 Playwright2");
    });

    DTE.cancel();

    /* DELETE WITHOUT APPROVE */

    I.say("Delete do not need approve, so even not approver should delete page without need of approve");
        I.logout();
        I.relogin("tester2");
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
        I.jstreeNavigate([folderName, subFolderName]);
        deletePage(I, DT, newPageName + changePostfix, true);
        I.say("Check that page was deleted");
            checkCreatedPage(I, newPageName, "/apps/schvalovanie-stranok/", false, (newPageName + changePostfix));
});

/**
 * In case of user that is page approver, page will be approve automaticly by this user
 */
Scenario('creating/editing/deleting page that NEED approve but it is automatic - logic', ({I, DT, DTE}) => {
    var newPageName = "self-approve-autotest-" + randomNumber;

    /* CREATE new page */
    I.say("Create page");

    I.relogin("admin");

    I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);

    //Add new page
    I.click(DT.btn.add_button);
    setPageName(I, DTE, newPageName, true);

    //Save
    DTE.save();
    I.dontSee("Žiadosť o schválenie web stránky dostal");

    //Check its not waiting or approve
    checkWaitingTab(I, DT, newPageName, false);

    I.clickCss("#pills-pages-tab");
    I.click(newPageName);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");

    within("#pills-dt-datatableInit-history", () => {
        DT.filterContains("historyApprovedByName", "Tester Playwright");
        I.see(newPageName);
    });

    I.say("Check auto publish");
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    /* EDIT page */

    I.say("Editing page");
        //Log back
        I.relogin("admin");
        I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);
        DT.filterContains("title", newPageName);
        I.click(newPageName);
        setPageName(I, DTE, newPageName + changePostfix);

    //Save change
    DTE.save();

    //Check its not waiting or approve
    checkWaitingTab(I, DT, newPageName + changePostfix, false);

    I.clickCss("#pills-pages-tab");
    I.click(newPageName + changePostfix);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");

    within("#pills-dt-datatableInit-history", () => {
        DT.filterContains("historyApprovedByName", "Tester Playwright");
        I.see(newPageName + changePostfix);
    });

    I.say("Logoff and check change");
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, (newPageName + changePostfix));

    /* DELETE page  */

    //Page needs approve but we are admin and delete should be approved automaticly (self - approve)
    I.say("Deleteing page");
        I.relogin("admin");
        I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);
        deletePage(I, DT, newPageName + changePostfix, true);
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", false, (newPageName + changePostfix));

});

Scenario('deleting page that NEED approve - logic', ({I, DT, DTE}) => {
    var newPageName = "approve-delete-autotest-" + randomNumber;
    /* Create page as approver */
    I.relogin('admin');
    I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);

    //Add new page
    I.click(DT.btn.add_button);
    setPageName(I, DTE, newPageName, true);
    //Save
    DTE.save();

    I.say("Test page");
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    /* Try delete page as non approver and then REJECT request as approver */
    I.relogin("tester2");
    I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);
    deletePage(I, DT, newPageName, false);

    //Log as admin and reject delete request
    I.logout();

    I.say("Test page it should be still available");
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    I.relogin('admin');
    I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);
    checkWaitingTab(I, DT, deletePrefix + newPageName, true);
    //Cant click at name ... probably problem with [] in name
    I.clickCss("#datatableInit td.dt-row-edit a");

    disApproveDelete(I, newPageName);

    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    checkWaitingTab(I, DT, deletePrefix + newPageName, false);

    I.say("Test page - should be still there");
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    /* Try delete page as non approver and then ACCEPT request as approver */
    I.relogin("tester2");
    I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);
    deletePage(I, DT, newPageName, false);

    //Log as admin and accept delete request
    I.logout();

    I.say("Test page it should be still available");
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", true, null);

    I.relogin('admin');
    I.amOnPage(urlCakajuceNaSchvalenieZmenaTitulku);
    checkWaitingTab(I, DT, deletePrefix + newPageName, true);
    //Cant click at name ... probably problem with [] in name
    I.clickCss("#datatableInit td.dt-row-edit a");

    approveDelete(I, newPageName);

    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    checkWaitingTab(I, DT, deletePrefix + newPageName, false);

    //Test among pages
    I.clickCss("#pills-pages-tab");
    I.dontSee(newPageName);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Test page - should NOT be still there");
        checkCreatedPage(I, newPageName, "/test-stavov/cakajuce-schvalenie-zmena-titulku/", false, null);

});

//L2 Approve testing
//L2 approve is more than clasic approve. If user is L2 approver for folder it's work for that folder but allso for CHILD folder !! so this test must cover both situation's

//tester - L1 approver for folder l2_child
//tester2 - standard user whose changes need to be approved
//tester3 - L2 approver for l2_parent and l2_child folder

Scenario('L2 approve logic test', ({I, DT, DTE}) => {

    var pageNameB = "approver-child-autotest-" + randomNumber;

    I.say("L2  testing LOGIC for folder: l2_parent - there is no first level approver set");
    //l2TestingLogic(I, DT, DTE, pageNameA, "/admin/v9/webpages/web-pages-list/?groupid=54452", "/apps/schvalovanie-stranok/l2_parent/", false);

    I.say("L2  testing LOGIC for folder: l2_parent/l2_child - there is first level approver set");
    l2TestingLogic(I, DT, DTE, pageNameB, "/admin/v9/webpages/web-pages-list/?groupid=54453", "/apps/schvalovanie-stranok/l2_parent/l2_child/", true);
 });

 Scenario('reset table settings', ({I, DT}) => {
    I.relogin("admin");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.resetTable();
 });

 function l2TestingLogic(I, DT, DTE, pageName, groupUrl, baseUrl, includeFirstStepApprove) {
    //Test 1 - Parent folder
    I.say("----> L2 testing - Creating");

        I.relogin('tester2');
        I.amOnPage(groupUrl);

        //Add new page
        I.click(DT.btn.add_button);
        setPageName(I, DTE, pageName, true);

        //Save
        DTE.save();

        var approverName = "Tester_L2 Playwright"
        if (includeFirstStepApprove) {
            approverName = "Tester Playwright";
        }
        I.see("Žiadosť o schválenie vytvorenia novej web stránky dostal: "+approverName);

        //Check we see page
        I.say("Until we are logged, we must see page");
            DT.filterContains("title", pageName);
            I.amOnPage(baseUrl + pageName.toLowerCase() + ".html");
            I.see(pageName.toUpperCase());

        //Check page is not publish
        I.say("Logoff and check that page return 404 , because its not published yet");
            checkCreatedPage(I, pageName, baseUrl, false, null);

        if (includeFirstStepApprove) {
            checkAndApprovePage(I, 'tester', groupUrl, pageName, DT, true);
        }
        checkAndApprovePage(I, 'tester3', groupUrl, pageName, DT);

    I.say("----> L2 testing - Updating");
        I.say("Do page change");
            I.relogin('tester2');
            I.amOnPage(groupUrl);

            I.click(pageName);
            setPageName(I, DTE, pageName + "_change");
            DTE.save();
            I.see("Žiadosť o schválenie web stránky dostal: "+approverName);

            //Check page is not publish
            I.say("Logoff and check that page name wasnt changed");
                checkEditedPage(I, baseUrl, pageName, pageName + "_change");

        I.say("L2 testing - Updating - reject by approver");
            if (includeFirstStepApprove) {
                checkAndApprovePage(I, 'tester', groupUrl, pageName + "_change", DT, true);
            }
            I.relogin('tester3');
            I.amOnPage(groupUrl);
            checkWaitingTab(I, DT, pageName + "_change", true);

        I.say("Go Dis-Approved page update");
            I.click(pageName + "_change");
            disApprovePage(I, pageName);

            I.amOnPage(groupUrl);

            //No histiory records no tab
            I.dontSeeElement("#pills-waiting-tab");

        I.say("Check that page was disapproved by admin L2");
            I.clickCss("#pills-pages-tab");
            I.dontSee(pageName + "_change");
            DT.filterContains("title", pageName);
            I.click(pageName);
            DTE.waitForEditor();
            I.toastrClose();
            I.click("#pills-dt-datatableInit-history-tab", null, { position: { x: 0, y: 0 } }); //because after toastr close cursor stays in close button tooltip

            within("#pills-dt-datatableInit-history", () => {
                // disapprove is always Tester_L2 Playwright no matter of includeFirstStepApprove
                DT.filterContains("historyDisapprovedByName", "Tester_L2 Playwright");
                I.see(pageName + "_change");
                I.see("Tester_L2 Playwright");
            });
            DTE.cancel();

            //Check page wasnt changed
            I.say("Logoff and check that page name wasnt changed");
                checkEditedPage(I, baseUrl, pageName, pageName + "_change");

        I.say("----> L2 testing - Updating - TRY 2");
            I.relogin('tester2');
            I.amOnPage(groupUrl);

            I.click(pageName);
            setPageName(I, DTE, pageName + "_change_2");
            DTE.save();
            I.see("Žiadosť o schválenie web stránky dostal: "+approverName);

            //Check page is not publish
            I.say("Logoff and check that page name wasnt changed");
                checkEditedPage(I, baseUrl, pageName, pageName + "_change_2");

        I.say("----> L2 testing - Updating - approve by L2 approver");
            if (includeFirstStepApprove) {
                checkAndApprovePage(I, 'tester', groupUrl, pageName + "_change_2", DT, true);
            }
            I.relogin('tester3');
            I.amOnPage(groupUrl);
            checkWaitingTab(I, DT, pageName + "_change_2", true);

                I.say("Approve page change");
                    I.click(pageName + "_change_2");
                    approvePage(I, pageName);

                I.say("Page change not waiting for approve anymore.");
                    I.amOnPage(groupUrl);
                    //No histiory records no tab
                    I.dontSeeElement("#pills-waiting-tab");
                    I.see(pageName + "_change_2");

                //Check page was changed
                I.say("Logoff and check that page name was changed");
                    checkCreatedPage(I, pageName, baseUrl, true, pageName + "_change_2");

    I.say("----> DELETE PHASE");

        I.say("Try delete");
            I.relogin('tester2');
            I.amOnPage(groupUrl);
            deletePage(I, DT, pageName + "_change_2", false);
            I.wait(3);
            I.see("Žiadosť o zmazanie web stránky dostal: "+approverName);

        //Check page was changed
        I.say("Logoff and check that page wasnt deleted");
            checkCreatedPage(I, pageName, baseUrl, true, pageName + "_change_2");

        I.say("Approve delete as L2");
            if (includeFirstStepApprove) {
                I.relogin('tester');
                I.amOnPage(groupUrl);
                checkWaitingTab(I, DT, deletePrefix + pageName + "_change_2", true);
                //Cant click at name ... probably problem with [] in name
                I.clickCss("#datatableInit td.dt-row-edit a");

                //Approve delete
                approveDelete(I, pageName + "_change_2", true);
            }

            I.relogin('tester3');
            I.amOnPage(groupUrl);
            checkWaitingTab(I, DT, deletePrefix + pageName + "_change_2", true);
            //Cant click at name ... probably problem with [] in name
            I.clickCss("#datatableInit td.dt-row-edit a");

            //Approve delete
            approveDelete(I, pageName + "_change_2");

            I.amOnPage(groupUrl);
            I.dontSeeElement("#pills-waiting-tab");

        I.say("Logoff and check that page is gone");
            checkCreatedPage(I, pageName, baseUrl, false, pageName + "_change_2");
 }

 function checkAndApprovePage(I, loginName, groupUrl, pageName, DT, hasSecondLevelApprove=false) {
    I.say("checkAndApprovePage as "+loginName);
        I.relogin(loginName);
        I.amOnPage(groupUrl);

        //Check user see awaiting (because is L2);
        I.say("Check that page is waiting for approve");
            checkWaitingTab(I, DT, pageName, true);

        //Approve page
        I.click(pageName);
        approvePage(I, pageName, hasSecondLevelApprove);

        I.amOnPage(groupUrl);

        //No histiory records no tab (tester user always has awaiting tab from another tests)
        if ("tester"==loginName) {
            checkWaitingTab(I, DT, pageName, false);
        } else {
            I.dontSeeElement("#pills-waiting-tab");
        }
        I.logout();
 }

 function checkEditedPage(I, baseUrl, seeName, dontSeeName) {
    I.say("checkEditedPage "+seeName);
    I.logout();
    I.amOnPage(baseUrl + seeName.toLowerCase() + ".html");
    I.see(seeName.toUpperCase());
    I.dontSee(dontSeeName.toUpperCase());
 }

 function checkCreatedPage(I, pageName, baseUrl, isPublic, changedName) {
    I.say("checkCreatedPage "+pageName);
    I.logout();
    I.amOnPage(baseUrl + pageName.toLowerCase() + ".html");

    //But after loading, because pageName (original) is needed for link
    if(changedName != null) pageName = changedName;

    I.say("isPublic: " + isPublic);

    if(!isPublic) {
        I.dontSee(pageName.toUpperCase());
        I.see("Chyba 404 - požadovaná stránka neexistuje");
    } else {
        I.see(pageName.toUpperCase());
        I.dontSee("Chyba 404 - požadovaná stránka neexistuje");
    }
 }

function approvePage(I, pageName, hasSecondLevelApprove=false) {
    I.say("approvePage "+pageName);
    I.wait(1);
    //Switch to tab
    I.switchToNextTab();
    I.closeOtherTabs();
    I.switchTo();

    //Check we see page name

    I.switchTo("#right");
        I.see(pageName.toUpperCase());
    I.switchTo();
    //Approve
    I.switchTo("#approveFormId");
        I.fillField("textarea[name=notes]", "Suhlasim");
        let approveBtn = locate("#tabMenu1 .btn.green")
        I.waitForElement(approveBtn, 10);
        I.click(approveBtn);
        //I.click("Schváliť");

        if (hasSecondLevelApprove)
            I.waitForText("Žiadosť o schválenie je posunutá na druhú úroveň. Žiadosť dostal Tester_L2 Playwright.");
        else
            I.waitForText("Web stránka bola schválená, je publikovaná na verejnej časti web stránky", 10);
    I.switchTo();

    //Close tab
    I.wait(2);
    I.closeOtherTabs();
    I.switchTo();
}

function disApprovePage(I, pageName) {
    I.say("disApprovePage "+pageName);
    I.wait(2);

    //Switch to tab
    I.switchToNextTab();

    //Check original name
    within({frame: "#right"}, () => {
    I.see(pageName.toUpperCase());
    });
    //Dis-approve page
    within({frame: "#approveFormId"}, () => {
        I.fillField("textarea[name=notes]", "Zamietam");
        I.clickCss("#tabMenu1 input.btn.red");
        I.waitForText("Pripomienky k web stránke boli zaslané, stránka ešte nie je publikovaná na verejnej časti web stránky.", 50);
    });

    //Close tab
    I.wait(2);
    I.closeOtherTabs();
}

function approveDelete(I, pageName, hasSecondLevelApprove=false) {
    I.say("approveDelete "+pageName);
    I.wait(2);
    //Switch to tab
    I.switchToNextTab();

    //Check page name
    within({frame :"#bodyFrameId"}, () => {
    I.see(pageName.toUpperCase());
    });
    //Approve delete button
    within({frame : "#approveDelFormId"}, () => {
        I.fillField("textarea[name=notes]", "Suhlasim so zmazanim");
        I.clickCss("#tabMenu1 input.btn.green");
        if (hasSecondLevelApprove)
            I.waitForText("Žiadosť o schválenie je posunutá na druhú úroveň. Žiadosť dostal Tester_L2 Playwright.");
        else
            I.waitForText("Stránka bola vymazaná", 10);
    });

    //Close tab
    I.wait(3);
    I.closeOtherTabs();
}

function disApproveDelete(I, pageName) {
    I.say("disApproveDelete "+pageName);
    I.wait(2);
    //Switch to tab
    I.switchToNextTab();

    //Check
    within({frame : "#bodyFrameId"}, () => {
        I.see(pageName.toUpperCase());
    });
    //Reject button
    within({frame:"#approveDelFormId"}, () => {
        I.fillField("textarea[name=notes]", "Zamietam zmazanie");
        I.clickCss("#tabMenu1 input.btn.red");
        I.waitForText("Vymazanie stránky zamietnuté", 10);
    });

    //Close tab
    I.wait(3);
    I.closeOtherTabs();
}

function deletePage(I, DT, pageName, checkDelete) {
    I.say("deletePage "+pageName)
    //Filter page
    DT.filterContains("title", pageName);
    //Check we see page
    I.see(pageName);
    //Bevare of delete, normal selector will delete whole folder
    I.clickCss("#datatableInit_wrapper div.dt-scroll-head button.buttons-select-all.dt-filter-id");
    I.click(DT.btn.delete_button);
    I.click("Zmazať", "div.DTE_Action_Remove");

    if(checkDelete) {
        //Check we dont see page
        I.dontSee(pageName);
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
}

function setPageName(I, DTE, pageName, setNavbar=false) {
    I.say("setPageName "+pageName);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.waitForElement('#DTE_Field_title');
    DTE.fillField('title', pageName);
    if (setNavbar) DTE.fillField("navbar", pageName);
}

function checkWaitingTab(I, DT, pageName, shouldBeThere) {
    I.say("checkWaitingTab "+pageName);
    I.clickCss("#pills-waiting-tab");
    DT.waitForLoader();
    DT.filterContains("title", pageName);

    if(shouldBeThere) {
        I.see(pageName, "#datatableInit_wrapper div.dt-scroll-body");
        I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    } else {
        I.dontSee(pageName, "#datatableInit_wrapper div.dt-scroll-body");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
}

Scenario('Testing of approving where user have NONE mode', async ({I, DT, DTE}) => {
    let noneApprove = "I am tester with set mode NONE";
    let mustApprove = "I am tester3 without any rights. I MUST be approved.";
    let approver = "I am tester2 and I am APPROVER";

    let parentPage = "Test_samo_schvalenia_none_parent";
    let childPage = "Test_samo_schvalenia_none_child";

    I.relogin('tester'); //right NONE so can do what he want
    await updatePagesLogic(I, DTE, noneApprove, false, null);

    I.relogin('tester3'); //dont have anz right, need approve
    await updatePagesLogic(I, DTE, mustApprove, true, noneApprove);

    I.relogin('tester'); //right NONE so can do what he want, BUT he cant see awaiting to approve changes
    checkAwaiting(I, DT, false);

    I.relogin('tester2'); //THIS is approver

    //must see approving request's
    checkAwaiting(I, DT, true);
    I.click( locate("div.datatable-column-width > a").withText(parentPage) );
    disApprovePage(I, parentPage);

    checkAwaiting(I, DT, true);
    I.click( locate("div.datatable-column-width > a").withText(childPage) );
    disApprovePage(I, childPage);

    await updatePagesLogic(I, DTE, approver, false, noneApprove);
});

Scenario("logoff", ({I}) => {
    I.logout();
});

async function checkAwaiting(I, DT, shouldBeThere) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=59157");
    I.clickCss("#pills-waiting-tab");
    DT.filterContains("title", "Test_samo_schvalenia_none_");
    DT.filterContains("authorName", "Tester_L2 Playwright");

    if(shouldBeThere) {
        I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    } else {
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
}

async function updatePagesLogic(I, DTE, insertText, shouldBeApproved, controlText) {
    //Parent
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=73276");
    DTE.waitForEditor();
    await updatePagesLogic2(I, DTE, insertText, shouldBeApproved, controlText);

    //Child
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=73277");
    DTE.waitForEditor();
    await updatePagesLogic2(I, DTE, insertText, shouldBeApproved, controlText);
}

async function updatePagesLogic2(I, DTE, insertText, shouldBeApproved, controlText) {
    //Check previous set text
    if(controlText != null) {
        I.waitForElement('#cke_data');
        I.switchTo("iframe.cke_reset");
        I.see(controlText);
        I.switchTo();
        I.switchTo();
    }

    //close notification (if exists) about latest page change
    await I.clickIfVisible("#toast-container-webjet div.toast-warning button.toast-close-button")

    //Fill page
    await DTE.fillCkeditor(insertText);
    DTE.save();

    if(shouldBeApproved) {
        //Must be generated approve notification
        I.seeElement("div.toast-container div.toast");
        I.see("Žiadosť o schválenie web stránky dostal: Tester2 Playwright2");
        //close it
        I.forceClickCss("div.toast-container .toast-close-button");
    } else {
        //Must NOT be generated approve notification
        I.dontSeeElement("div.toast-container div.toast");
        I.dontSee("Žiadosť o schválenie web stránky dostal: Tester2 Playwright2");
    }
}