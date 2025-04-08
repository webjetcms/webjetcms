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

    DT.filterContains("subject", subject + "_1");
    DT.filterContains("question", question + " 1");

    //See the forum but dont see button, to recover deleted forum (recover from soft delete)
    I.see(subject + "_1");

    //if test fail it can be soft deleted, undelete it
    await I.clickIfVisible(locate('td .ti.ti-trash'));
    DT.waitForLoader();

    I.waitForInvisible(locate('td .ti.ti-trash'), 10);

    //Delete forum (soft delete)
    I.clickCss(".buttons-select-all");
    I.clickCss("button.buttons-remove");
    DTE.waitForEditor("forumDataTable");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose("forumDataTable_modal");
    I.wait(0.2);
    DT.waitForLoader();

    //See forum, BUT see allso recover button
    I.see(subject + "_1");
    I.waitForVisible(locate('td .ti.ti-trash'), 10);

    //Recover deleted forum
    I.forceClick(locate('td .ti.ti-trash'));
    DT.waitForLoader();
    I.wait(1);

    //Check, we see forum but dont see button to recover, because forum was recored succefull
    I.see(subject + "_1");
    I.waitForInvisible(locate('td .ti.ti-trash'), 10);

    /* Additional test of recover button (not in datatable but above table) */
        DT.filterContains("subject", subject + "_2");
        DT.filterContains("question", question + " 2");

        I.see(subject + "_2");
        I.dontSee(subject + "_1");

        //
        I.say("Delete forum (soft delete)");
        I.wait(1);
        I.clickCss(".buttons-select-all");
        I.wait(1);
        I.clickCss("button.buttons-remove");
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForModalClose("forumDataTable_modal");
        I.wait(1);

        I.see(subject + "_2");
        I.waitForVisible(locate('td .ti.ti-trash'), 10);

        //
        I.say("Use other recover button");
        //I.clickCss(".buttons-select-all");
        I.clickCss("#forumDataTable_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button:nth-child(6)");
        I.wait(1);

        I.see(subject + "_2");
        I.waitForInvisible(locate('td .ti.ti-trash'), 10);

    I.logout();
});

Scenario('Test of forum editor', ({I, DT, DTE}) => {
    I.relogin('tester_forum');

    I.amOnPage("/apps/forum/admin/");

    DT.filterContains("subject", subject + "_1");
    DT.filterContains("question", question + " 1");

    I.click(subject + "_1");
    DTE.waitForEditor("forumDataTable");

    I.fillField("#DTE_Field_subject", "");

    DTE.save();

    I.see("Povinné pole. Zadajte aspoň jeden znak.");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    I.fillField("#DTE_Field_subject", subject + "_1" + "_change");

    DTE.save();

    DT.filterContains("subject", subject + "_1" + "_change");
    I.see(subject + "_1" + "_change");

    I.click(subject + "_1");
    DTE.waitForEditor("forumDataTable");
    I.fillField("#DTE_Field_subject", subject + "_1");
    DTE.save();
    DT.filterContains("subject", subject + "_1");
    I.see(subject + "_1");
    I.dontSee(subject + "_1" + "_change");
 });

async function removeUserEditablePages(I, DT, DTE) {
    I.relogin('admin');
    I.amOnPage("/admin/v9/users/user-list/");
    DT.filterContains("login", userLogin);
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

    DT.filterContains("subject", subject);
    DT.filterContains("question", question);

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
    DT.filterContains("login", userLogin);
    I.click(userLogin);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-rightsTab-tab");
    I.wait(1);

    I.click(locate('.btn.btn-outline-secondary.btn-vue-jstree-add').withText('Pridať web stránku'));
    I.click(locate('.jstree-node.jstree-closed').withText(I.getDefaultDomainName()).find('.jstree-icon.jstree-ocl'));
    I.waitForElement(locate('.jstree-node.jstree-closed').withText('Aplikácie').find('.jstree-icon.jstree-ocl'), 10);
    I.click(locate('.jstree-node.jstree-closed').withText('Aplikácie').find('.jstree-icon.jstree-ocl'));
    I.waitForElement(locate('.jstree-node.jstree-closed').withText('Diskusia').find('.jstree-icon.jstree-ocl'), 10);
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
    I.clickCss("#forumContentDiv a.btn-primary");

    I.waitForElement("#forumForm");

    I.fillField("#forumForm #subject", subject);
    DTE.fillCleditor("#forum", body);
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
    DTE.fillCleditor("#forum", body + "_ANSWER");
    //SPAM LIMIT WAIT 30 seconds
    I.wait(30);

    I.click(".ui-dialog button.btn-primary");

    I.waitForElement("#forumContentDiv div.row2.even");
    I.see(body);

    //Check delete
    I.wait(3);
    I.click( locate("div.btn-group > span.deleteMessage > a.btn-danger") );

    I.amOnPage("/apps/forum/admin/");
    DT.filterContains("subject", subject);
    I.see(body);
    I.see(body + "_ANSWER");

    I.click("div.filter-input > button");
    I.click("a.dropdown-item > span > i.ti-trash");

    I.click({ css: "div.dt-scroll-headInner button.dt-filtrujem-" + "editorFields\\.statusIcons" });

    I.see(body);
    I.see(body + "_ANSWER");
});

Scenario("basic table tests", async ({I, DT, DTE}) => {
    I.relogin('admin');
    I.amOnPage("/apps/forum/admin/");
    DT.waitForLoader();
    I.see("/Aplikácie/Diskusia/Diskusia", "#forumDataTable td.dt-tree-page a");
    I.see("/Aplikácie/Message Board/Skupina2/podskupina3", "#forumDataTable td.dt-tree-page a");

    DT.filterContains("docDetails", "podskupina3");
    I.dontSee("/Aplikácie/Diskusia/Diskusia", "#forumDataTable td.dt-tree-page a");
    I.see("/Aplikácie/Message Board/Skupina2/podskupina3", "#forumDataTable td.dt-tree-page a");
});


Scenario("sending notifications to email", async ({I, DTE, TempMail, Document}) => {
    I.relogin('admin');
    Document.setConfigValue('spamProtectionTimeout', 1);
    var random = I.getRandomText();
    var subject = "Test notifikácia "+random;
    var test_body = "Toto je testovaci prispevok. "+random;

    I.relogin('tester_forum1');

    I.say('Creating a new post')
    I.amOnPage("/apps/diskusia/?NO_WJTOOLBAR=true");
    I.click("Nový príspevok");
    I.waitForElement("#forum");
    I.see("Poslať notifikáciu pri odpovedi na príspevok");
    I.waitForElement(".cleditorMain", 10);
    I.fillField("subject", subject);
    I.dontSeeCheckboxIsChecked('#sendAnswerNotif1');
    I.checkOption('#sendAnswerNotif1');
    DTE.fillCleditor("#forum", test_body);
    I.clickCss("div.ui-dialog button.btn.btn-primary");
    I.waitForText("Príspevok je uložený", 10);
    I.see("Príspevok je uložený");

    I.say('Checking if the dialog is hidden');
    I.waitForInvisible("div.ui-dialog");
    I.dontSee("div.ui-dialog");
    I.dontSee("Poslať notifikáciu pri odpovedi na príspevok");

    I.say('Checking if post is visible');
    I.waitForText(subject, 10);
    I.see(subject);
    I.see(test_body);

    I.say('Creating own response to a new post');
    I.click(locate('.media-body').withText(subject).find('a').withText('[Odpovedať]'));
    I.waitForElement(".cleditorMain", 10);
    DTE.fillCleditor("#forum", "Toto je VLASTNA odpoved na testovaci prispevok. "+random);
    I.clickCss("div.ui-dialog button.btn.btn-primary");
    I.waitForText("Príspevok je uložený", 10);
    I.dontSee('Prekročili ste limit, pri ktorom sa považujete za autora spamu.');
    I.wait(0.5);
    I.waitForText("Toto je VLASTNA odpoved na testovaci prispevok. "+random, 10)

    I.say('Checking if the dialog was NOT received');
    TempMail.login('webjetcms1');
    if (!await TempMail.isInboxEmpty()){
        TempMail.openLatestEmail();
        I.dontSee(subject);
    }

    I.say('Creating a response to a new post');
    I.relogin('tester_forum2');
    I.amOnPage("/apps/diskusia/?NO_WJTOOLBAR=true");
    I.click(locate('.media-body').withText(subject).find('a').withText('[Odpovedať]'));
    DTE.fillCleditor("#forum", "Toto je odpoved na testovaci prispevok. "+random);
    I.clickCss("div.ui-dialog button.btn.btn-primary");

    I.say('Checking if the dialog was received');
    TempMail.login('webjetcms1');
    TempMail.openLatestEmail();
    I.see('Do diskusie bola pridaná odpoveď na váš príspevok');
    I.see(subject);
    TempMail.deleteCurrentEmail();

});

Scenario("restore changes", ({I, Document}) => {
    I.relogin('admin');
    Document.setConfigValue('spamProtectionTimeout', 30);
});


Scenario("image attachment verification", async ({I, DTE, Document}) => {
    var random = I.getRandomText();
    var test_body = "Toto je testovaci prispevok imagetest "+random;
    var test_forum = 'image test' + random;

    I.relogin('admin');
    I.amOnPage("/apps/message-board/skupina2/podskupina3.html");
    I.wait(1);
    I.clickCss('.btn-forum-new-topic');
    I.waitForElement("div.ui-dialog", 10);
    I.waitForElement(".cleditorMain", 10);
    I.wait(2);
    I.fillField('#subject', test_forum);
    DTE.fillCleditor("#forum", test_body);
    I.clickCss("div.ui-dialog button.btn.btn-primary");
    //I.waitForText("Príspevok je uložený", 10, "div.ui-dialog");
    I.waitForInvisible("div.ui-dialog");
    I.wait(2);
    I.waitForElement(locate('a').withText(test_forum), 20);
    I.amOnPage("/apps/message-board/skupina2/podskupina3.html");

    I.click(locate('a').withText(test_forum));
    I.clickCss('.btn.btn-default');
    I.switchToNextTab();

    I.attachFile('input', 'tests/apps/penguin.jpg');
    I.click('input[type="submit"][value="Odoslať"]');
    I.switchToNextTab();

    I.wait(5);
    I.clickCss('li img[src="/components/_common/mime/jpg.gif"] + a');
    const imageUrl = await I.grabCurrentUrl();
    await Document.compareScreenshotElement("img", "autotest-penguin.png", null,null, 5);

    I.say('Waiting for files update');
    I.wait(4);

    I.say('Deleting uploaded file');

    I.amOnPage('/admin/v9/files/index/#elf_iwcm_1_L2ltYWdlcy9hcHBzL2ZvcnVt');
    I.waitForElement("#nav-iwcm_1_L2ltYWdlcy9hcHBzL2ZvcnVt.elfinder-navbar-expanded", 10);
    I.wait(1);

    const imageSelector = '.elfinder-cwd-filename[title*="penguin.jpg"]';
    let numVisible;
    do {
        numVisible = await I.grabNumberOfVisibleElements(imageSelector);
        I.say("Number of visible elements: " + numVisible);
        if (numVisible) {
            I.waitForVisible(imageSelector);
            I.clickCss(imageSelector);
            I.pressKey('Delete');
            I.clickCss('.elfinder-confirm-accept');
            I.waitForInvisible(imageSelector, 10);
        }
    } while (numVisible);

    let response = await I.sendGetRequest(imageUrl + '?_disableCache=true');
    //console.log(response);
    I.assertEqual(response.status, 404, "Image was NOT deleted");
});

Scenario("Verification of special actions", async ({I, DT, DTE, Document}) => {
    I.relogin('tester_forum');

    I.amOnPage("/apps/diskusia/");
    I.see(subject + "_1", 'h4.media-heading');

    I.amOnPage("/apps/forum/admin/");
    DT.filterContains("subject", subject + "_1");
    DT.filterContains("question", question + " 1");

    I.clickCss(".buttons-select-all");
    I.clickCss('.reject-forum');

    I.amOnPage("/apps/diskusia/");
    I.dontSee(subject + "_1", 'h4.media-heading');

    I.amOnPage("/apps/forum/admin/");
    DT.filterContains("subject", subject + "_1");
    DT.filterContains("question", question + " 1");

    I.clickCss(".buttons-select-all");
    I.clickCss('.approve-forum');

    I.amOnPage("/apps/diskusia/");
    I.see(subject + "_1", 'h4.media-heading');
});
