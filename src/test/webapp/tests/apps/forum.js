Feature('apps.forum');

var randomNumber;
var question = "Test filtrovania a ikon, diskusia";
var subject = "Forum_filter_diskusia";
var userLogin = "tester_forum";

Before(({ I }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Test of filtration by current user group/page permissions', async ({I, DT, DTE}) => {
    await removeUserEditablePages(I, DT, DTE);
    clearAllCaches(I);

    I.relogin('tester_forum');
    testForumsVisibility(true, true, I, DT);

    //Now as Admin add permision to only ONE page
    I.relogin('admin');
    addPagePermision(I, DT, DTE, "docId-38480_anchor");
    clearAllCaches(I);

    I.relogin('tester_forum');
    testForumsVisibility(true, false, I, DT);

    //Now as Admin add permision to only second page
    I.relogin('admin');
    addPagePermision(I, DT, DTE, "docId-65077_anchor");
    clearAllCaches(I);

    I.relogin('tester_forum');
    testForumsVisibility(true, true, I, DT);

    I.logout();
});

Scenario('Test soft delete and then recover of entity', async ({I, DT, DTE}) => {

    I.relogin('tester_forum');

    I.amOnPage("/apps/forum/admin/");

    DT.filter("subject", subject + "_1");
    DT.filter("question", question + " 1");

    //See the forum but dont see button, to recover deleted forum (recover from soft delete)
    I.see(subject + "_1");

    //if test fail it can be soft deleted, undelete it
    await I.clickIfVisible(locate('//*[@id="391"]/td[2]/div/a[2]/i'));
    DT.waitForLoader();

    I.dontSeeElement(locate('//*[@id="391"]/td[2]/div/a[2]/i'));

    //Delete forum (soft delete)
    I.clickCss(".buttons-select-all");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose("forumDataTable_modal");
    DT.waitForLoader();

    //See forum, BUT see allso recover button
    I.see(subject + "_1");
    I.seeElement(locate('//*[@id="391"]/td[2]/div/a[2]/i'));

    //Recover deleted forum
    I.forceClick(locate('//*[@id="391"]/td[2]/div/a[2]/i'));
    DT.waitForLoader();
    I.wait(1);

    //Check, we see forum but dont see button to recover, because forum was recored succefull
    I.see(subject + "_1");
    I.dontSeeElement(locate('//*[@id="391"]/td[2]/div/a[2]/i'));

    /* Additional test of recover button (not in datatable but above table) */
        DT.filter("subject", subject + "_2");
        DT.filter("question", question + " 2");

        I.see(subject + "_2");
        I.dontSee(subject + "_1");

        //Delete forum (soft delete)
        I.clickCss(".buttons-select-all");
        I.clickCss("button.buttons-remove");
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.wait(1);

        I.see(subject + "_2");
        I.seeElement(locate('//*[@id="392"]/td[2]/div/a[2]/i'));

        //Use other recover button
        I.clickCss(".buttons-select-all");
        I.clickCss("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(6)");
        I.wait(1);

        I.see(subject + "_2");
        I.dontSeeElement(locate('//*[@id="392"]/td[2]/div/a[2]/i'));

    I.logout();
});

Scenario('Test of forum editor', ({I, DT, DTE}) => {
    I.relogin('tester_forum');

    I.amOnPage("/apps/forum/admin/");

    DT.filter("subject", subject + "_1");
    DT.filter("question", question + " 1");

    I.click(subject + "_1");
    DTE.waitForEditor("forumDataTable");

    I.fillField("#DTE_Field_subject", "");

    DTE.save();

    I.see("Povinné pole. Zadajte aspoň jeden znak.");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    I.fillField("#DTE_Field_subject", subject + "_1" + "_change");

    DTE.save();

    DT.filter("subject", subject + "_1" + "_change");
    I.see(subject + "_1" + "_change");

    I.click(subject + "_1");
    DTE.waitForEditor("forumDataTable");
    I.fillField("#DTE_Field_subject", subject + "_1");
    DTE.save();
    DT.filter("subject", subject + "_1");
    I.see(subject + "_1");
    I.dontSee(subject + "_1" + "_change");
 });

async function removeUserEditablePages(I, DT, DTE) {
    I.relogin('admin');
    I.amOnPage("/admin/v9/users/user-list/");
    DT.filter("login", userLogin);
    I.click(userLogin);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-rightsTab-tab");
    I.wait(1);

    I.waitForElement("#editorAppDTE_Field_editorFields-editableGroups div.dt-tree-container", 10);
    I.wait(1);
    await I.clickIfVisible(locate("button.btn-vue-jstree-item-remove"));
    I.wait(1);
    await I.clickIfVisible(locate("button.btn-vue-jstree-item-remove"));
    I.wait(1);
    await I.clickIfVisible(locate("button.btn-vue-jstree-item-remove"));
    I.wait(1);
    await I.clickIfVisible(locate("button.btn-vue-jstree-item-remove"));
    I.wait(1);
    await I.clickIfVisible(locate("button.btn-vue-jstree-item-remove"));

    DTE.save();
}

function testForumsVisibility(iSeeFirst, iSeeSecond, I, DT) {
    I.amOnPage("/apps/forum/admin/");

    DT.filter("subject", subject);
    DT.filter("question", question);

    if(iSeeFirst) I.see(subject + "_1");
    else I.dontSee(subject + "_1");

    if(iSeeSecond) I.see(subject + "_2");
    else I.dontSee(subject + "_2");
}

function clearAllCaches(I) {
    I.amOnPage("/admin/v9/settings/cache-objects/");
    I.clickCss("#datatableInit_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(3)");
    I.click(locate("button.btn-primary").withText("Potvrdiť"));
}

function addPagePermision(I, DT, DTE, elelentId) {
    I.amOnPage("/admin/v9/users/user-list/");
    DT.filter("login", userLogin);
    I.click(userLogin);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-rightsTab-tab");
    I.wait(1);

    I.click(locate('.btn.btn-outline-secondary.btn-vue-jstree-add').withText('Pridať web stránku'));
    I.click(locate('.jstree-node.jstree-closed').withText('demotest.webjetcms.sk').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jstree-node.jstree-closed').withText('Aplikácie').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jstree-node.jstree-closed').withText('Diskusia').find('.jstree-icon.jstree-ocl'));
    I.clickCss("#" + elelentId);

    DTE.save();
}

Scenario('Message board', async ({I, DT, DTE}) => {
    let subject = "messageBoard_autotest_" + randomNumber;
    let body = "this_is_nice_body_" + randomNumber;

    I.relogin('admin');
    I.amOnPage("/apps/message-board/skupina2/podskupina3.html");

    //Add new theme
    I.click("#forumContentDiv a.btn-primary");

    I.waitForElement("#forumForm");
    I.switchTo("#forumForm");

    I.fillField("#subject", subject);
    DTE.fillCleditor("#forum", body);
    I.switchTo();
    I.click("button.btn-primary");

    //Check that theme was created
    I.waitForElement( locate("#forumContentDiv a").withText(subject), 10);

    //Open
    I.click(subject);
    I.waitForElement("#forumContentDiv");
    I.see(body);

    //Answer
    I.click( locate("div.btn-group > span > a.btn-info") );
    I.waitForElement("#forumForm");
    I.switchTo("#forumForm");
    DTE.fillCleditor("#forum", body + "_ANSWER");

    //SPAM LIMIT WAIT 30 seconds
    I.wait(30);

    I.click("button.btn-primary");
    I.switchTo();

    I.waitForElement("#forumContentDiv div.row2.even");
    I.see(body);

    //Check delete
    I.wait(3);
    I.click( locate("div.btn-group > span.deleteMessage > a.btn-danger") );

    I.amOnPage("/apps/forum/admin/");
    DT.filter("subject", subject);
    I.see(body);
    I.see(body + "_ANSWER");

    I.click("div.filter-input > button");
    I.click("a.dropdown-item > span > i.fa-trash-can-undo");

    I.click({ css: "div.dataTables_scrollHeadInner button.dt-filtrujem-" + "editorFields\\.statusIcons" });

    I.see(body);
    I.see(body + "_ANSWER");
});

Scenario("basic table tests", async ({I, DT, DTE}) => {
    I.relogin('admin');
    I.amOnPage("/apps/forum/admin/");
    DT.waitForLoader();
    I.see("/Aplikácie/Diskusia/Diskusia", "#forumDataTable td.dt-tree-page a");
    I.see("/Aplikácie/Message Board/Skupina2/podskupina3", "#forumDataTable td.dt-tree-page a");

    DT.filter("docDetails", "podskupina3");
    I.dontSee("/Aplikácie/Diskusia/Diskusia", "#forumDataTable td.dt-tree-page a");
    I.see("/Aplikácie/Message Board/Skupina2/podskupina3", "#forumDataTable td.dt-tree-page a");
});