Feature('webpages.multigroup');

var masterDocId = 70837;
var slaveDocId = 70838;
var baseTitle = "Multi page";
var randomNumber;
var docIds;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
        docIds = [masterDocId, slaveDocId];
    }
});

function openPage(docId, I, DTE) {
    I.fillField("#tree-doc-id", docId);
    I.pressKey("Enter");

    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.wait(0.5);
}

Scenario('multigroup - change URL by title', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+masterDocId);
    DTE.waitForEditor();

    var title = baseTitle + " " + randomNumber;
    //
    I.say("Changing master title to "+title);
    I.click("#pills-dt-datatableInit-basic-tab");
    DTE.fillField("title", title);
    DTE.fillField("navbar", title);
    I.checkOption({css: "#DTE_Field_generateUrlFromTitle_0"});
    I.wait(1);
    DTE.save();

    //
    I.say("Verify URL change");
    for (var i = 0; i < docIds.length; i++) {
        var docId = docIds[i];
        openPage(docId, I, DTE);
        I.seeInField("#DTE_Field_title", title);
        I.seeInField("#DTE_Field_navbar", title);

        var url = "/test-stavov/multigroup/"
        if (docId == masterDocId) url += "master";
        else url += "slave";
        url += "/"+title.toLowerCase().replace(/ /g, "-");
        I.seeInField("#DTE_Field_virtualPath", url);

        DTE.cancel();
    }
});

Scenario('multigroup - preserve URL', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+masterDocId);
    DTE.waitForEditor();

    //
    I.say("Reseting title");
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    DTE.fillField("title", baseTitle);
    DTE.fillField("navbar", baseTitle);
    I.checkOption({css: "#DTE_Field_generateUrlFromTitle_0"});
    DTE.save();

    //
    I.say("Changing master URL");
    openPage(masterDocId, I, DTE);

    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/multigroup/master/multi-page.html");
    I.uncheckOption({css: "#DTE_Field_generateUrlFromTitle_0"});
    I.wait(1);
    var masterUrl = "/test-stavov/multigroup/master/multi-page-change.html";
    DTE.fillField("virtualPath", masterUrl);
    DTE.save();

    openPage(masterDocId, I, DTE);
    I.seeInField("#DTE_Field_virtualPath", masterUrl);
    DTE.cancel();

    I.say("Testing slave URL is not changed");
    openPage(slaveDocId, I, DTE);
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/multigroup/slave/multi-page.html");

    //
    I.say("Changing slave URL");
    var slaveUrl = "/test-stavov/multigroup/slave/multi-page-change.html";
    DTE.fillField("virtualPath", slaveUrl);
    DTE.save();

    openPage(masterDocId, I, DTE);
    I.seeInField("#DTE_Field_virtualPath", masterUrl);
    DTE.cancel();

    openPage(slaveDocId, I, DTE);
    I.seeInField("#DTE_Field_virtualPath", slaveUrl);
    DTE.cancel();

});

Scenario('multigroup - preserve sort order', ({ I, DTE }) => {
    //
    I.say("Changing master sort order");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+masterDocId);
    DTE.seeInField('virtualPath', '/test-stavov/multigroup/master/multi-page-change.html');
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    DTE.fillField("sortPriority", "440");
    DTE.save();

    //
    I.say("Changing slave sort order");
    openPage(slaveDocId, I, DTE);
    DTE.seeInField('virtualPath', '/test-stavov/multigroup/slave/multi-page-change.html');
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    DTE.fillField("sortPriority", "500");
    DTE.save();

    //
    I.say("Verify sort order");
    openPage(masterDocId, I, DTE);
    DTE.seeInField('virtualPath', '/test-stavov/multigroup/master/multi-page-change.html');
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    I.seeInField("#DTE_Field_sortPriority", "440");
    DTE.cancel();

    openPage(slaveDocId, I, DTE);
    DTE.seeInField('virtualPath', '/test-stavov/multigroup/slave/multi-page-change.html');
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    I.seeInField("#DTE_Field_sortPriority", "500");
    DTE.cancel();
});

function selectSlave(I) {
    I.clickCss("#editorAppDTE_Field_editorFields-groupCopyDetails > section > div > div > button.btn-vue-jstree-add");
    I.click(locate('.jstree-node.jstree-closed').withText('Test stavov').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jstree-node.jstree-closed').withText('Multigroup').find('.jstree-icon.jstree-ocl'));
    I.click(locate('#jsTree').find('.jstree-node.jstree-leaf').withText('Slave').find('a.jstree-anchor'));
}

function deletePage(I, DT, DTE) {
    DT.waitForLoader();
    I.clickCss("#datatableInit_wrapper table thead button.buttons-select-all");
    I.see("Záznamy 1 až 1 z 1", "#datatableInit_wrapper .dt-footer-row");
    I.see("1 riadok označený", "#datatableInit_wrapper .dt-footer-row");
    I.click(DT.btn.delete_button);
    DTE.waitForEditor();
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 20, "#datatableInit_wrapper");
}

Scenario('multigroup - delete slaves after master hard delete', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=57591");

    var docName = "autotest-multi-delete-" + randomNumber;

    //
    I.say("Create master with copy (slave)");
        I.click(DT.btn.add_button);
        DTE.waitForEditor();
        I.waitForElement("#DTE_Field_title");
        I.click("#DTE_Field_title");
        I.fillField("#DTE_Field_title", docName);
        selectSlave(I);
        DTE.save();

    //
    I.say("Check and delete slva + delete check -soft");
        I.jstreeClick("Slave");
        DT.filterContains("title", docName);
        I.see(docName);
        deletePage(I, DT, DTE);

    //
    I.say("Hard delete of slave");
        I.click("#pills-trash-tab");
        DT.waitForLoader();
        I.see(docName);
        deletePage(I, DT, DTE);
        I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=57591");

    //
    I.say("Add slave again but this time delete MASTER + check that slave still works (not deleted)");
        DT.filterContains("title", docName);
        I.clickCss("#datatableInit_wrapper button.buttons-select-all");
        I.click(DT.btn.edit_button)
        DTE.waitForEditor();

        I.click("#pills-dt-datatableInit-basic-tab");
        selectSlave(I);
        DTE.save();

        I.jstreeClick("Slave");
        I.see(docName, "#datatableInit_wrapper");

        I.jstreeClick("Master");
        deletePage(I, DT, DTE);

        //Check that page still works
        I.jstreeClick("Slave");
        I.see(docName, "#datatableInit_wrapper");
        I.click(docName);
        DTE.waitForEditor();
        DTE.cancel();

    //
    I.say("Master hard delete - should be allso slave hard deleted");
        I.click("#pills-trash-tab");
        DT.waitForLoader();
        DT.waitForLoader();
        DT.filterContains("title", docName);
        deletePage(I, DT, DTE);

        //
        I.say("Check soft delete")
        I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=57592");
        DT.filterContains("title", docName);
        I.dontSee(docName);
        I.see("Nenašli sa žiadne vyhovujúce záznamy");

        //
        I.say("Check hard delete");
        I.click("#pills-trash-tab");
        DT.waitForLoader();
        DT.filterContains("title", docName);
        I.dontSee(docName);
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
});