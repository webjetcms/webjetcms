Feature('apps.news');

const WebjetDteJsTree = require("../../pages/WebjetDteJsTree");

Before(({ login }) => {
    login('admin');
});

Scenario('Test editor logic', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Novinky', '#components-news-title', null, false);

    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.say('Check tabs');
        I.seeElement("#pills-dt-component-datatable-basic-tab");
        I.seeElement("#pills-dt-component-datatable-templates-tab");
        I.seeElement("#pills-dt-component-datatable-perex-tab");
        I.seeElement("#pills-dt-component-datatable-filter-tab");
        I.seeElement("#pills-dt-component-datatable-news-tab");
        I.seeElement("#pills-dt-component-datatable-commonSettings-tab");

    I.clickCss("#pills-dt-component-datatable-basic-tab");
        I.clickCss("button.btn-webjet-jstree-add");
        I.waitForVisible(WebjetDteJsTree.tree);
        I.click(locate('a.jstree-anchor').withText("Jet portal 4"));
        I.waitForInvisible(WebjetDteJsTree.tree);

        I.clickCss("button.btn-webjet-jstree-add");
        I.waitForVisible(WebjetDteJsTree.tree);
        I.click(locate('a.jstree-anchor').withText("Newsletter"));
        I.waitForInvisible(WebjetDteJsTree.tree);

        I.checkOption("#DTE_Field_alsoSubGroups_0");
        DTE.selectOption("publishType", "Nasledujúce (začiatok je v budúcnosti)");
        DTE.selectOption("order", "Ratingu");
        I.fillField("#DTE_Field_pageSize", 25);
        I.fillField("#DTE_Field_offset", 8);
        I.checkOption("#DTE_Field_checkDuplicity_0");

    I.clickCss("#pills-dt-component-datatable-templates-tab");
        I.click( locate("label.custom-template").withChild(locate("span").withText("news01")) );

    I.clickCss("#pills-dt-component-datatable-perex-tab");
        I.click( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("ďalšia perex skupina") ));
        I.click( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("kalendar-udalost") ));

        I.click( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("investícia") ));
        I.click( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("podnikanie") ));

    I.clickCss("#pills-dt-component-datatable-filter-tab");
        addFilter(I, "AUTHOR_ID", "<=", "666");
        addFilter(I, "DATE_CREATED", "=", "05.05.2025");
        addFilter(I, "DATA", "Končí na", 'Kokos, "je", king"');
        addFilter(I, "AVAILABLE", "=", "false");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const checkParams1 = {
        groupIds: '1+27',
        alsoSubGroups: 'true',
        publishType: 'next',
        order: 'rating',
        ascending: 'true',
        paging: 'false',
        pageSize: '25',
        offset: '8',
        perexNotRequired: 'false',
        loadData: 'false',
        checkDuplicity: 'true',
        docMode: '0',
        template: 'news01',
        perexGroup: '3+794',
        perexGroupNot: '1+2',
    };

    await Apps.assertParams(checkParams1);

    //Check filter values .. because of format I cant use assertParams
    Apps.switchEditor('html');
    I.see('filter[DATA_ew]=&quot;Kokos, \\&quot;je\\&quot;, king\\&quot;&quot;,');
    I.see("filter[AUTHORID_le]=666,");
    I.see("filter[DATECREATED_eq]=&quot;2025-05-05&quot;,");
    I.see("filter[AVAILABLE_eq]=false");

    Apps.switchEditor('standard');
    Apps.openAppEditor();

    I.clickCss("#pills-dt-component-datatable-basic-tab");
        I.seeInField(".DTE_Field_Name_groupIds .dt-tree-container .form-group:nth-child(1) input.form-control", '/Jet portal 4');
        I.seeInField(".DTE_Field_Name_groupIds .dt-tree-container .form-group:nth-child(2) input.form-control", '/Newsletter');

        //Remove first folder
        I.click( locate(".DTE_Field_Name_groupIds").find("button.btn-webjet-jstree-item-remove") );

    I.clickCss("#pills-dt-component-datatable-perex-tab");
        I.seeCheckboxIsChecked( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("ďalšia perex skupina") ));
        I.seeCheckboxIsChecked( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("kalendar-udalost") ));
        I.seeCheckboxIsChecked( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("investícia") ));
        I.seeCheckboxIsChecked( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("podnikanie") ));

        //Uncheck
        I.click( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("kalendar-udalost") ));
        I.click( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("investícia") ));

        //Check new one
        I.click( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("PerexWithGroup_A") ));
        I.click( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("PerexWithGroup_B") ));

    I.clickCss("#pills-dt-component-datatable-filter-tab");
        checkFilter(I, 1, "AUTHOR_ID", "<=", "666");
        checkFilter(I, 2, "DATE_CREATED", "=", "2025-05-05");
        checkFilter(I, 3, "DATA", "Končí na", 'Kokos, "je", king"');
        checkFilter(I, 4, "AVAILABLE", "false", null);

        I.say("Remove some filters");
            I.click( locate("#filtersTable > tbody > tr:nth-child(1)").find("input.filter-row-select"));
            I.click( locate("#filtersTable > tbody > tr:nth-child(2)").find("input.filter-row-select"));
            I.click( locate("#filtersDiv").find("button.btn-danger") );

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const checkParams2 = {
        groupIds: '27',
        alsoSubGroups: 'true',
        publishType: 'next',
        order: 'rating',
        ascending: 'true',
        paging: 'false',
        pageSize: '25',
        offset: '8',
        perexNotRequired: 'false',
        loadData: 'false',
        checkDuplicity: 'true',
        docMode: '0',
        template: 'news01',
        perexGroup: '3+625',
        perexGroupNot: '626+2'
    };

    await Apps.assertParams(checkParams2);

    //Check filter values .. because of format I cant use assertParams
    Apps.switchEditor('html');
    I.see('filter[DATA_ew]=&quot;Kokos, \\&quot;je\\&quot;, king\\&quot;&quot;,');
    I.dontSee("filter[AUTHORID_le]=666,");
    I.dontSee("filter[DATECREATED_eq]=&quot;2025-05-05&quot;,");
    I.see("filter[AVAILABLE_eq]=false");
});

function addFilter(I, docField, operator, value) {
    I.say("Adding filter");
    I.clickCss("button.btn-success");
    I.selectOption( locate("#filtersTable > tbody > tr:last-child").find("select.fieldSelect") , docField);
    I.selectOption( locate("#filtersTable > tbody > tr:last-child").find("td.operatorTd > select") , operator);
    if(value != null) {
        if ("true" === value || "false" === value) {
            I.selectOption( locate("#filtersTable > tbody > tr:last-child").find("td.valueTd > select") , value);
        } else {
            I.fillField( locate("#filtersTable > tbody > tr:last-child").find("td.valueTd > input") , value);
        }
    }

}

function checkFilter(I, position, docField, operator, value) {
    I.say("Checking filter");
    I.seeInField( locate("#filtersTable > tbody > tr:nth-child(" + position + ")").find("select.fieldSelect"),  docField);
    I.seeInField( locate("#filtersTable > tbody > tr:nth-child(" + position + ")").find("td.operatorTd > select"),  operator);
    if(value != null) { I.seeInField( locate("#filtersTable > tbody > tr:nth-child(" + position + ")").find("td.valueTd > input"),  value); }
}

Scenario('zoznam noviniek', async ({ I, DT, DTE }) => {

    I.amOnPage("/apps/news/admin/");
    I.waitForElement('#SomStromcek .jstree-anchor');
    I.clickCss('#SomStromcek a[title="/English/News"]');
    DT.waitForLoader("newsDataTable");

    I.see("McGregor sales force");
    I.see("News");
    I.dontSee("Čím je človek bohatší, tým má menej hotovosti")

    //
    var pageName = "Trhy sú naďalej vydesené";
    I.clickCss('#SomStromcek a[title="/Jet portal 4/Zo sveta financií"]');
    DT.waitForLoader();
    I.dontSee("McGregor sales force");
    I.see(pageName);
    I.see("Čím je človek bohatší, tým má menej hotovosti")

    //
    I.say("Checking ckeditor");
    I.click(pageName);
    DTE.waitForEditor("newsDataTable");
    I.see(pageName, "#newsDataTable_modal h5.modal-title");

    I.switchTo("iframe.cke_wysiwyg_frame.cke_reset");
    I.see("Na trhoch minulý týždeň pretrvávala volatilita", "h2");

    I.switchTo();

    //
    I.say("Check permissions");
    I.amOnPage("/apps/news/admin/?removePerm=cmp_news");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
    const treeStatus = await I.executeScript(async () => {
        const response = await fetch("/admin/rest/news/news-list/tree", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ id: "0" })
        });
        return response.status;
    });
    I.assertEqual(treeStatus, 403, "The tree endpoint requires the News permission");
});

Scenario('logoff', ({ I }) => {
    I.logout();
});

Scenario('set groupIds parameter in webpage', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=10");
    DTE.waitForEditor();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.waitForElement("iframe.wj_component", 10);
    I.wait(2);
    I.clickCss("iframe.wj_component");
    I.wait(2);

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.waitForElement("#pills-dt-component-datatable-basic-tab")
    I.wait(3);

    //
    I.say("check pages only from group 24");
    I.clickCss("#pills-dt-component-datatable-news-tab");
    I.switchTo("#newsListIframe");
    DT.waitForLoader("newsDataTable");
    I.see("Zo sveta financií");
    I.dontSee("Produktová stránka - B verzia");

    //
    I.say("set groupIds to 24,25");

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.clickCss("#pills-dt-component-datatable-basic-tab");
    I.clickCss("button.btn-webjet-jstree-add");
    I.waitForVisible(WebjetDteJsTree.tree);
    I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Jet portal 4").find('.jstree-icon.jstree-ocl'));
    I.click(locate('a.jstree-anchor').withText("Produktová stránka"));
    I.waitForInvisible(WebjetDteJsTree.tree);

    //
    I.say("check pages from group 24 and 25");
    I.clickCss("#pills-dt-component-datatable-news-tab");
    I.wait(3);
    I.switchTo("#newsListIframe");
    I.waitForElement("th.dt-th-title input", 20);

    DT.waitForLoader("newsDataTable");
    I.see("Zo sveta financií");
    I.dontSee("Produktová stránka - B verzia");
    I.seeElement('#SomStromcek a[title="/Jet portal 4/Zo sveta financií"]');
    I.dontSeeElement('#SomStromcek a[title="/Newsletter"]');
    I.seeElement('#SomStromcek a[title="/Jet portal 4/Produktová stránka"]');

    I.switchTo();
});

Scenario('News tree permissions and empty state', async ({ I, DT }) => {
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader("newsDataTable");
    I.seeElement('#SomStromcek a[title="/Aplikácie/Blog/blogger"]');
    I.seeElement('#SomStromcek a[title="/English/News"]');

    I.relogin("tester2");
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader("newsDataTable");
    I.seeElement('#SomStromcek a[title="/Aplikácie/Blog/blogger"]');
    I.dontSeeElement('#SomStromcek a[title="/English/News"]');
    I.fillField("#tree-folder-search-input", "News");
    I.clickCss("#tree-folder-search-button");
    I.waitForInvisible("#SomStromcek .jstree-loading");
    I.dontSeeElement('#SomStromcek a[title="/English/News"]');

    I.relogin("jtester");
    I.amOnPage("/apps/news/admin/");
    I.waitForVisible("#news-folders-empty");
    DT.waitForLoader("newsDataTable");
    I.dontSeeElement("#SomStromcek .jstree-anchor");
    I.dontSeeElement("#newsDataTable_wrapper .buttons-create:not(.disabled)");
    const count = await I.executeScript(() => newsDataTable.page.info().recordsTotal);
    I.assertEqual(count, 0, "An unavailable News folder must not load unrelated articles");
});

Scenario('News tree shared parents', async ({ I, DT }) => {
    const include = "!INCLUDE(/components/news/news-velocity.jsp, groupIds=24+25)!";
    await I.amOnPageAsync("/apps/news/admin/?include=" + encodeURI(include).replace(/\+/g, "%2B") + "#1");
    I.waitForElement('#SomStromcek [id="24_anchor"].jstree-clicked');
    I.seeElement('#SomStromcek [id="1_anchor"].jstree-disabled .ti-folders');
    I.see("Jet portal 4", '#SomStromcek [id="1_anchor"]');
    DT.waitForLoader("newsDataTable");

    const hierarchy = await I.executeScript(() => {
        const tree = $("#SomStromcek").jstree(true);
        return {
            children: tree.get_node("1").children.sort(),
            parent: tree.get_node("24").parent,
            parentFilter: tree.get_node("1").original.groupIdList || null,
            articleFilter: new URL(newsDataTable.getAjaxUrl(), location.origin).searchParams.get("groupIdList")
        };
    });
    I.assertDeepEqual(hierarchy.children, ["24", "25"], "A navigation parent exposes only the scoped News branches");
    I.assertEqual(hierarchy.parent, "1", "News folders share their actual parent");
    I.assertEqual(hierarchy.parentFilter, null, "A navigation parent has no article filter");
    I.assertEqual(hierarchy.articleFilter, "24", "A parent hash falls back to a selectable News folder");

    I.clickCss('#SomStromcek [id="1"] > .jstree-ocl');
    I.waitForInvisible('#SomStromcek [id="24_anchor"]');
    I.clickCss('#SomStromcek [id="1"] > .jstree-ocl');
    I.waitForVisible('#SomStromcek [id="24_anchor"].jstree-clicked');
    I.clickCss('#SomStromcek [id="25_anchor"]');
    DT.waitForLoader("newsDataTable");
    I.assertEqual(await I.executeScript(() => new URL(newsDataTable.getAjaxUrl(), location.origin).searchParams.get("groupIdList")), "25", "A News folder remains selectable below a navigation parent");
});

Scenario('News tree navigation, search, recursion and creation defaults', async ({ I, DT, DTE }) => {
    const include = "!INCLUDE(/components/news/news-velocity.jsp, groupIds=1, alsoSubGroups=true)!";
    const url = "/apps/news/admin/?include=" + encodeURI(include).replace(/\+/g, "%2B");
    await I.amOnPageAsync(url + "#24");
    I.waitForElement('#SomStromcek [id="24_anchor"].jstree-clicked');
    DT.waitForLoader("newsDataTable");
    I.dontSeeElement("#groupIdFilterSelect");
    I.dontSeeElement(".tree-col .buttons-create");
    I.assertEqual(await I.executeScript(() => new URL(newsDataTable.getAjaxUrl(), location.origin).searchParams.get("groupIdList")), "24", "A child uses its own folder filter");

    I.clickCss("#newsDataTable_wrapper .buttons-create");
    DTE.waitForEditor("newsDataTable");
    I.clickCss("#pills-dt-newsDataTable-basic-tab");
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input", "/Jet portal 4/Zo sveta financií");
    DTE.cancel();

    I.fillField("#tree-folder-search-input", "Produktová");
    I.pressKey("Enter");
    I.waitForVisible('#SomStromcek [id="25_anchor"]');
    I.clickCss('#SomStromcek [id="25_anchor"]');
    DT.waitForLoader("newsDataTable");
    I.clickCss("#tree-folder-search-clear-button");
    I.waitForVisible('#SomStromcek [id="25_anchor"].jstree-clicked');
    I.clickCss(".tree-col .buttons-refresh");
    I.waitForVisible('#SomStromcek [id="25_anchor"].jstree-clicked');
    I.assertEqual(await I.executeScript(() => new URL(newsDataTable.getAjaxUrl(), location.origin).searchParams.get("groupIdList")), "25", "Search and refresh preserve selection");

    await I.amOnPageAsync(url + "#1*");
    I.waitForElement('#SomStromcek [id="1_anchor"].jstree-clicked');
    DT.waitForLoader("newsDataTable");
    I.assertEqual(await I.executeScript(() => new URL(newsDataTable.getAjaxUrl(), location.origin).searchParams.get("groupIdList")), "1*", "A configured root preserves recursive filtering");
    await I.amOnPageAsync(url + "#999999999");
    I.waitForElement('#SomStromcek [id="1_anchor"].jstree-clicked');
    DT.waitForLoader("newsDataTable");
    I.assertEqual(await I.executeScript(() => location.hash), "#1*", "An invalid hash selects the first permitted root");

    const emptyInclude = "!INCLUDE(/components/news/news-velocity.jsp, groupIds=999999999)!";
    await I.amOnPageAsync("/apps/news/admin/?include=" + encodeURI(emptyInclude));
    I.waitForVisible("#news-folders-empty");
    I.waitForElement("#newsDataTable_wrapper .buttons-create.disabled");
    DT.waitForLoader("newsDataTable");
    I.assertEqual(await I.executeScript(() => newsDataTable.page.info().recordsTotal), 0, "An empty configuration cannot show unrelated articles");
});

Scenario('News tree responsive layout and keyboard navigation', async ({ I, DT }) => {
    const include = "!INCLUDE(/components/news/news-velocity.jsp, groupIds=1)!";
    await I.amOnPageAsync("/apps/news/admin/?include=" + encodeURI(include));
    I.waitForElement('#SomStromcek [id="1_anchor"].jstree-clicked');
    DT.waitForLoader("newsDataTable");
    I.clickCss('#SomStromcek [id="1_anchor"]');
    I.pressKey("ArrowRight");
    I.waitForElement('#SomStromcek [id="24_anchor"]');
    I.pressKey("ArrowDown");
    I.pressKey("Enter");
    const keyboardSelection = await I.executeScript(() => ({
        focused: document.activeElement.id,
        selected: document.querySelector("#SomStromcek .jstree-clicked").id,
        outline: getComputedStyle(document.activeElement).outlineStyle
    }));
    I.assertEqual(keyboardSelection.focused, keyboardSelection.selected, "Enter selects the keyboard-focused folder");
    I.assertNotEqual(keyboardSelection.selected, "1_anchor", "Arrow keys move to a child folder");
    I.assertNotEqual(keyboardSelection.outline, "none", "Keyboard focus remains visible");
    DT.waitForLoader("newsDataTable");

    for (const width of [1280, 1100, 640]) {
        I.resizeWindow(width, 800);
        // Let the shared admin shell finish its debounced resize and sidebar transition.
        I.wait(0.5);
        const layout = await I.executeScript(() => {
            const tree = document.querySelector(".tree-col").getBoundingClientRect();
            const table = document.querySelector(".datatable-col").getBoundingClientRect();
            return { treeRight: tree.right, treeBottom: tree.bottom, tableLeft: table.left, tableTop: table.top };
        });
        if (width >= 768) I.assertAbove(layout.tableLeft, layout.treeRight - 2, "The article table sits beside the tree");
        else I.assertAbove(layout.tableTop, layout.treeBottom - 2, "The article table stacks below the tree");
        I.saveScreenshot("autotest-news-tree-" + width + ".png");
    }
    I.wjSetDefaultWindowSize();
});

Scenario("logout", ({ I }) => {
    I.logout();
});

function verifyDocMode(docMode, subGroupsDepth, I, Apps, checkNewsMain = false) {

    let option = "";
    if(docMode === 0) { option = "Zobraziť všetky stránky vrátane hlavných stránok priečinkov"; }
    else if(docMode === 1) { option = "Zobraziť iba hlavné stránky priečinkov"; }
    else if(docMode === 2) { option = "Vylúčiť hlavné stránky priečinkov"; }

    Apps.openAppEditor(null, "pills-dt-component-datatable-basic");
    I.clickCss('#pills-dt-component-datatable-basic');
    I.selectOption('#DTE_Field_docMode', option);
    I.fillField('#DTE_Field_subGroupsDepth', subGroupsDepth);
    Apps.confirm();

    I.switchTo(".cke_wysiwyg_frame.cke_reset");
    I.waitForElement("iframe.wj_component");
    I.switchTo("iframe.wj_component");

    const newsMainPerex = "Novinky main page perex";
    const newsPage = "Novinka 2025-01";
    const newsPageSubfolderLevel2 = "Novinka 2025Q2-01";
    const mainFolder = "2025 folder";
    const mainFolderLevel2 = "2025Q2 folder";

    if (docMode == 0) {
        //all pages include main
        I.waitForText(newsPage, 10, "h3 a");
        I.see(mainFolder, "p");
        if (checkNewsMain) I.see(newsMainPerex, "p");
    } else if (docMode == 1) {
        //only main pages
        I.waitForText(mainFolder, 10, "p");
        I.dontSee(newsPage, "h3 a");
        if (checkNewsMain) I.see(newsMainPerex, "p");
    } else if (docMode == 2) {
        //no main pages
        I.waitForText(newsPage, 10, "h3 a");
        I.dontSee(mainFolder, "p");
        if (checkNewsMain) I.dontSee(newsMainPerex, "p");
    }

    //test subGroupsDepth
    if (docMode == 0 || docMode == 1) {
        if (subGroupsDepth == -1) {
            I.see(mainFolder, "p");
            I.see(mainFolderLevel2, "p");
            if (docMode == 0) I.see(newsPageSubfolderLevel2, "h3 a");
        } else {
            I.see(mainFolder, "p");
            I.dontSee(mainFolderLevel2, "p");
            I.dontSee(newsPageSubfolderLevel2, "h3 a");
        }
    } else {
        //docMode == 2 (no main pages)
        if (subGroupsDepth == -1) {
            I.dontSee(mainFolder, "p");
            I.dontSee(mainFolderLevel2, "p");
            I.see(newsPageSubfolderLevel2, "h3 a");
        } else {
            I.dontSee(mainFolder, "p");
            I.dontSee(mainFolderLevel2, "p");
            I.dontSee(newsPageSubfolderLevel2, "h3 a");
        }
    }

    I.switchTo();
}

Scenario("docMode and subGroupsDepth", ({ I, Apps }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=146543");

    //all pages include main
    verifyDocMode(0, -1, I, Apps);
    verifyDocMode(0, 1, I, Apps);
    //only main pages
    verifyDocMode(1, -1, I, Apps);
    //no main pages
    verifyDocMode(2, -1, I, Apps);
});

Scenario("BUG: news from other folders and docMode", ({ I, Apps }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=150415");

    //all pages include main
    verifyDocMode(0, -1, I, Apps, true);
    verifyDocMode(0, 1, I, Apps, true);
    //only main pages
    verifyDocMode(1, -1, I, Apps, true);
    //no main pages
    verifyDocMode(2, -1, I, Apps, true);
});

Scenario("logout2", ({ I }) => {
    I.switchTo();
    I.logout();
});
