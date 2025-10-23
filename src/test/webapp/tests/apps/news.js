Feature('apps.news');

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
        I.clickCss("button.btn-vue-jstree-add");
        I.waitForVisible("div#jsTree");
        I.click(locate('a.jstree-anchor').withText("Jet portal 4"));
        I.waitForInvisible("div#jsTree");

        I.clickCss("button.btn-vue-jstree-add");
        I.waitForVisible("div#jsTree");
        I.click(locate('a.jstree-anchor').withText("Newsletter"));
        I.waitForInvisible("div#jsTree");

        I.checkOption("#DTE_Field_alsoSubGroups_0");
        DTE.selectOption("publishType", "Nasledujúce (začiatok je v budúcnosti)");
        DTE.selectOption("order", "Ratingu");
        I.fillField("#DTE_Field_pageSize", 25);
        I.fillField("#DTE_Field_offset", 8);
        I.checkOption("#DTE_Field_checkDuplicity_0");
        I.fillField("#DTE_Field_contextClasses", 'iway"sk", test ",pokus"');

    I.clickCss("#pills-dt-component-datatable-templates-tab");
        I.click( locate("label.custom-template").withChild(locate("span").withText("news01")) );

    I.clickCss("#pills-dt-component-datatable-perex-tab");
        I.click( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("ďalšia perex skupina") ));
        I.click( locate(".DTE_Field_Name_perexGroup").find( locate("label").withText("kalendar-udalost") ));

        I.click( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("investícia") ));
        I.click( locate(".DTE_Field_Name_perexGroupNot").find( locate("label").withText("podnikanie") ));

    I.clickCss("#pills-dt-component-datatable-filter-tab");
        addFilter(I, "AUTHOR_ID", "<=", "666");
        addFilter(I, "DATE_CREATED", "=", "01.10.2025");
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
    I.see("filter[DATECREATED_eq]=&quot;2025-10-01&quot;,");
    I.see("filter[AVAILABLE_eq]=false");
    I.see('contextClasses=&quot;iway\\&quot;sk\\&quot;, test \\&quot;,pokus\\&quot;&quot;,');

    Apps.switchEditor('standard');
    Apps.openAppEditor();

    I.clickCss("#pills-dt-component-datatable-basic-tab");
        I.seeElement( locate(".DTE_Field_Name_groupIds").find("input[value='/Jet portal 4']") );
        I.seeElement( locate(".DTE_Field_Name_groupIds").find("input[value='/Newsletter']") );

        //Remove first folder
        I.click( locate(".DTE_Field_Name_groupIds").find("button.btn-vue-jstree-item-remove") );

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
        checkFilter(I, 2, "DATE_CREATED", "=", "2025-10-01");
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
    I.dontSee("filter[DATECREATED_eq]=&quot;2025-10-01&quot;,");
    I.see("filter[AVAILABLE_eq]=false");
    I.see('contextClasses=&quot;iway\\&quot;sk\\&quot;, test \\&quot;,pokus\\&quot;&quot;,');
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

Scenario('zoznam noviniek', ({ I, DT, DTE }) => {

    I.amOnPage("/apps/news/admin/");
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("/English/News"));

    I.see("McGregor sales force");
    I.see("News");
    I.dontSee("Čím je človek bohatší, tým má menej hotovosti")

    //
    var pageName = "Trhy sú naďalej vydesené";
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText("/Jet portal 4/Zo sveta financií"));
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
    I.clickCss("button.btn-vue-jstree-add");
    I.waitForVisible("div#jsTree");
    I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Jet portal 4").find('.jstree-icon.jstree-ocl'));
    I.click(locate('a.jstree-anchor').withText("Produktová stránka"));
    I.waitForInvisible("div#jsTree");

    //
    I.say("check pages from group 24 and 25");
    I.clickCss("#pills-dt-component-datatable-news-tab");
    I.wait(3);
    I.switchTo("#newsListIframe");
    I.waitForElement("th.dt-th-title input", 20);

    DT.waitForLoader("newsDataTable");
    I.see("Zo sveta financií");
    I.dontSee("Produktová stránka - B verzia");
    I.see("/Jet portal 4/Zo sveta financií", "#groupIdFilterSelect option");
    I.dontSee("/Newsletter", "#groupIdFilterSelect option");
    I.see("/Jet portal 4/Produktová stránka", "#groupIdFilterSelect option");

    I.switchTo();
});

Scenario('Test of permissions and filtering select by permissions', async ({ I, DT }) => {
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader();

    I.say("Check that user see folder to select");
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/blogger") ) );
    I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/English/News") ) );

    //
    I.say('Check filtering by permissions');
    I.relogin("tester2");
    I.amOnPage("/apps/news/admin/");
    DT.waitForLoader();

    I.say("Check that user see folder to select");
    I.clickCss('button[data-id="groupIdFilterSelect"]');
    I.seeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/Aplikácie/Blog/blogger") ) );
    I.dontSeeElement( locate("div.dropdown-menu").find( locate("a.dropdown-item > span").withText("/English/News") ) );

    //
    I.say("Check no perms");
    I.relogin("jtester");
    I.amOnPage("/apps/news/admin/");

    I.say("Check that permission error is showed");
    I.waitForElement("#toast-container-webjet");
    I.see("Prístup na adresár zamietnutý", "#toast-container-webjet > .toast-error > .toast-message");

    I.say("Check that user DONT have groups to select");
    I.clickCss('button[data-id="groupIdFilterSelect"]')
    const numberOfGroups = await I.grabNumberOfVisibleElements( locate("div.dropdown-menu").find( locate("a.dropdown-item > span") ) );
    I.assertEqual(0, numberOfGroups, "ERROR - User should not see any group to select");
});

Scenario("logout", ({ I }) => {
    I.logout();
});

function verifyDocMode(docMode, subGroupsDepth, I, Apps) {

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

    const newsPage = "Novinka 2025-01";
    const newsPageSubfolderLevel2 = "Novinka 2025Q2-01";
    const mainFolder = "2025 folder";
    const mainFolderLevel2 = "2025Q2 folder";

    if (docMode == 0) {
        //all pages include main
        I.waitForText(newsPage, 10, "h3 a");
        I.see(mainFolder, "p");
    } else if (docMode == 1) {
        //only main pages
        I.waitForText(mainFolder, 10, "p");
        I.dontSee(newsPage, "h3 a");
    } else if (docMode == 2) {
        //no main pages
        I.waitForText(newsPage, 10, "h3 a");
        I.dontSee(mainFolder, "p");
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

Scenario("logout2", ({ I }) => {
    I.switchTo();
    I.logout();
});