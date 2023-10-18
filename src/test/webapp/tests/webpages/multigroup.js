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

Scenario('multigroup - preserve sort order', ({ I, DT, DTE }) => {
    //
    I.say("Changing master sort order");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+masterDocId);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    DTE.fillField("sortPriority", "440");
    DTE.save();

    //
    I.say("Changing slave sort order");
    openPage(slaveDocId, I, DTE);
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    DTE.fillField("sortPriority", "500");
    DTE.save();

    //
    I.say("Verify sort order");
    openPage(masterDocId, I, DTE);
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    I.seeInField("#DTE_Field_sortPriority", "440");
    DTE.cancel();

    openPage(slaveDocId, I, DTE);
    I.clickCss("#pills-dt-datatableInit-menu-tab");
    I.seeInField("#DTE_Field_sortPriority", "500");
    DTE.cancel();
});

