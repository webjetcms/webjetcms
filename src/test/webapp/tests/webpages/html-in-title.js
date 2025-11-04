Feature('webpages.html-in-title');

var groupTitle, pageTitle;

Before(({ I, login }) => {
    login('admin');
    var random = I.getRandomTextShort();
    if (typeof groupTitle == "undefined") {
        groupTitle = "Suprový <strong>priečinok</strong><sup>TB</sup>" + random;
        pageTitle = "Suprová <strong>stránka</strong><sup>TB</sup>" + random;
    }
});

async function htmlToText(htmlString, I) {
    const result = await I.executeScript(({htmlString}) => {
        return WJ.htmlToText(htmlString);
    }, {htmlString});
    return result;
}

async function verifyPageData(title, I, DT, checkNavbar=false, newsTitle=null, newsTitleNoHtml=null) {

    if (title.indexOf("-autotest")==-1) title = "name-autotest-" + title;
    var titleNoHtml = await htmlToText(title, I);

    I.closeOtherTabs();
    DT.filterContains("title", title);
    DT.waitForLoader();
    I.see(title, "#datatableInit_wrapper td.dt-row-edit a");
    I.click(locate("#datatableInit_wrapper td.allow-html a.preview-page-link"));

    I.switchToNextTab();

    I.waitForText(titleNoHtml.toUpperCase(), 10, "article div.container h1");
    var pageTitle = await I.grabHTMLFrom("article div.container h1");
    I.assertEqual(pageTitle.trim(), title);

    if (checkNavbar === true) {
        //
        I.say("check navbar, title="+title);
        var navbarText = await I.grabHTMLFrom("p.test-navbar");
        I.assertContain(navbarText.trim(), title);
    }

    if (newsTitle !== null) {
        I.say("check content, newsTitle="+newsTitle+", newsTitleNoHtml="+newsTitleNoHtml.toUpperCase());
        I.see(newsTitleNoHtml.toUpperCase(), "div.news .col-sm-9 h3");
        let newsHTML = await I.grabHTMLFrom('div.news');
        I.assertContain(newsHTML, newsTitle);
    }

    //
    I.say("check seo title");
    I.seeInSource(title);
    I.seeInSource(`<meta property="og:title" content="${titleNoHtml}"`)
    I.seeInSource(`<title>${titleNoHtml} | InterWay</title>`);

    I.switchToPreviousTab();
    I.closeOtherTabs();
}

Scenario('Create group with HTML in title', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=50020');
    I.createFolderStructure(groupTitle, false, false);

    //
    I.say("verify group exists");
    I.seeElement(locate("a.jstree-anchor").withText(groupTitle));
    I.jstreeClick(groupTitle);
    DT.waitForLoader();
    I.see(groupTitle, "#datatableInit_wrapper td.dt-row-edit a");

    await verifyPageData(groupTitle, I, DT);

    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("groups-datatable");
    I.seeInField("#DTE_Field_groupName", groupTitle);
    I.seeInField("#DTE_Field_navbarName", groupTitle);

    //
    I.say("Rename group and verify");
    var newGroupTitle = "name-autotest-" + groupTitle + " - RENAMED";
    I.fillField("#DTE_Field_groupName", newGroupTitle);
    I.fillField("#DTE_Field_navbarName", newGroupTitle);
    DTE.save();
    DT.waitForLoader();
    I.seeElement(locate("a.jstree-anchor").withText(newGroupTitle));
    I.see(newGroupTitle, "#datatableInit_wrapper td.dt-row-edit a");

    //
    I.say("Edit webpage and verify");
    I.click(locate("#datatableInit_wrapper td.dt-row-edit a").withText(newGroupTitle));
    DTE.waitForLoader();

    await DTE.fillCkeditor(`
        <p class="test-navbar">!REQUEST(navbar)!</p>
        !INCLUDE(/components/news/news-velocity.jsp, groupIds=&quot;50020&quot;, alsoSubGroups=&quot;true&quot;, subGroupsDepth=&quot;-1&quot;, publishType=&quot;all&quot;, docMode=&quot;0&quot;, order=&quot;save_date&quot;, ascending=&quot;true&quot;, paging=&quot;false&quot;, pageSize=&quot;10&quot;, offset=&quot;0&quot;, perexNotRequired=&quot;true&quot;, loadData=&quot;false&quot;, checkDuplicity=&quot;false&quot;, contextClasses=&quot;&quot;, cacheMinutes=&quot;10&quot;, template=&quot;news.template.news01&quot;, perexGroup=&quot;&quot;, perexGroupNot=&quot;&quot;)!
    `);

    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_groupName", newGroupTitle);
    I.seeInField("#DTE_Field_navbarName", newGroupTitle);

    DTE.save();

    await verifyPageData(newGroupTitle, I, DT, true);

    DT.clearFilter("title");

    //
    I.say("Add webpage with HTML in title");
    I.createNewWebPage(pageTitle, false, false);
    pageTitle = "webPage-autotest-" + pageTitle;
    I.waitForText(pageTitle, 10, "#datatableInit_wrapper td.dt-row-edit a");
    I.click(locate("#datatableInit_wrapper td.dt-row-edit a").withText(pageTitle));
    DTE.waitForLoader();

    await DTE.fillCkeditor(`
        <p class="test-navbar">!REQUEST(navbar)!</p>
    `);

    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_title", pageTitle);
    I.seeInField("#DTE_Field_navbar", pageTitle);
    DTE.save();

    //
    I.say("verify page navbar");
    await verifyPageData(pageTitle, I, DT, true);

    //
    I.say("verify page is shown as news");
    var pageTitleNoHtml = await htmlToText(pageTitle, I);
    await verifyPageData(newGroupTitle, I, DT, true, pageTitle, pageTitleNoHtml);
});


Scenario('delete group', ({ I }) => {
    I.deleteFolderStructure(groupTitle, 50020);
    //I.jstreeReset();
});