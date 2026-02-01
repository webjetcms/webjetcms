Feature('components.news-templates');

var randomNumber;
var templateName;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        templateName = "autotestTemplate_" + randomNumber;
    }
});

Scenario('news-templates zakladne-testy @baseTest', async ({ I, DataTables }) => {
    I.amOnPage("/admin/v9/templates/news/");
    await DataTables.baseTest({
        dataTable: 'newsTempsDataTable',
        perms: 'components.news.edit_templates',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

const templateMenu = {
    seeItems : [
        ["velocity", "Podmienky a cykly"],
        ["doc", "Vlastnosti web stránky"],
        ["group", "Vlastnosti adresáru"]
    ],
    dontSeeItems : [
        ["paging", "Stránkovanie"]
    ]
};

const pagingMenu = {
    seeItems : [
        ["velocity", "Podmienky a cykly"],
        ["paging", "Stránkovanie"]
    ],
    dontSeeItems : [
        ["doc", "Vlastnosti web stránky"],
        ["group", "Vlastnosti adresáru"]
    ]
};

const velocitySubmenu = [
    "If Else-Podmienka #If #Else",
    "Foreach-Cyklus #Foreach"
];

//Not all options
const docSubmenu = [
    "ID šablóny - ID použitej šablóny",
    "Dostupnosť - Vráti hodnotu true/false či je webstránka dostupná",
    "Navigačná lišta -",
    "ID hlavičky - ID webstránky s hlavičkou"
];

//Not all options
const groupSubmenu = [
    "Názov adresára - Názov aktuálneho adresára ",
    "Meno domény -",
    "Voľné pole A - Obsah voľného poľa A",
    "Voľné pole B - Obsah voľného poľa B"
];

const pagingSubmenu = [
    "Ďaľšia stránka - Link s ďaľšou stránkou ",
    "Predchádzajúca stránka - Link s predchádzajúcou stránkou ",
    "Cyklus medzi stránok - Cyklus s linkami na jednotlive stránky ",
    "Prvá stránka - Link s prvou stránkou ",
    "Posledná stránka - Link s poslednou stránkou "
];

Scenario('news-templates test context-menu compozition', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/templates/news/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor();

    I.say("Check template html context menu");
    I.click("#pills-dt-datatableInit-code-tab");
    I.rightClick("#DTE_Field_templateCode");
    testContextMenu(I, templateMenu);

    I.say("Test velocity sub-menu");
    testSubMenu(I, "velocity", "templateCode", velocitySubmenu);

    I.say("Test doc sub-menu");
    testSubMenu(I, "doc", "templateCode", docSubmenu);

    I.say("Test group sub-menu");
    testSubMenu(I, "group", "templateCode", groupSubmenu);

    I.say("Check paging html context menu");
    I.click("#pills-dt-datatableInit-paging-tab");
    I.rightClick("#DTE_Field_pagingCode");
    testContextMenu(I, pagingMenu);

    I.say("Test velocity sub-menu");
    testSubMenu(I, "velocity", "pagingCode", velocitySubmenu);

    I.say("Test paging sub-menu");
    testSubMenu(I, "paging", "pagingCode", pagingSubmenu);
});

Scenario('news-templates test context-menu handling', async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/templates/news/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor();

    I.fillField("#DTE_Field_name", templateName);

    I.say("Set template html value via context menu");
    I.click("#pills-dt-datatableInit-code-tab");
    addContextCode(I, "templateCode", "velocity", velocitySubmenu[1]);
    addContextCode(I, "templateCode", "doc", docSubmenu[0]);
    addContextCode(I, "templateCode", "group", groupSubmenu[0]);

    I.say("Set paging html value via context menu");
    I.click("#pills-dt-datatableInit-paging-tab");
    addContextCode(I, "pagingCode", "velocity", velocitySubmenu[0]);
    addContextCode(I, "pagingCode", "paging", pagingSubmenu[0]);

    DTE.save();

    I.say("Check the values");
    DT.filterEquals("name", templateName);
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor();

    I.click("#pills-dt-datatableInit-code-tab");
    const templateHtmlValue = await I.grabValueFrom("#DTE_Field_templateCode");
    I.click("#pills-dt-datatableInit-paging-tab");
    const pagingHtmlValue = await I.grabValueFrom("#DTE_Field_pagingCode");

    const templateHtmlReferenceValue = "<table> \n #foreach( $doc in $news ) \n <tr><td>$foreach.count</td><td>$doc.title</td></tr> \n #end \n </table>$doc.tempId$doc.group.groupName";
    const pagingHtmlReferenceValue = "#if($foo == $bar) it's true! #{else} it's not! #end$nextPage.link";

    I.assertEqual(templateHtmlReferenceValue, templateHtmlValue);
    I.assertEqual(pagingHtmlReferenceValue, pagingHtmlValue);
});

Scenario('news-templates delete entity', ({ I, DT }) => {
    I.amOnPage("/admin/v9/templates/news/");
    DT.filterEquals("name", templateName);
    I.clickCss("td.sorting_1");

    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

function addContextCode(I, fieldId, menuClass, submenuText) {
    I.rightClick("#DTE_Field_" + fieldId);
    I.moveCursorTo("li.dropdown-submenu." + menuClass);
    I.click(locate("a").withText(submenuText));
}

function testContextMenu(I, inputObject) {
    let seeItems = inputObject.seeItems;
    let dontSeeItems = inputObject.dontSeeItems;

    seeItems.forEach(item => {
        I.seeElement(locate("li.dropdown-submenu." + item[0]).withText(item[1]));
    });

    dontSeeItems.forEach(item => {
        I.dontSeeElement(locate("li.dropdown-submenu." + item[0]).withText(item[1]));
    });
}

function testSubMenu(I, id, fieldId, submenuItems) {
    //reset context menu
    I.rightClick("#DTE_Field_" + fieldId);

    submenuItems.forEach(item => {
        I.dontSeeElement(locate("a").withText(item));
    });

    I.moveCursorTo("li.dropdown-submenu." + id);

    submenuItems.forEach(item => {
        I.seeElement(locate("a").withText(item));
    });
}

Scenario("contextClasses", async ({ I, DT, DTE }) => {
    //cleanup
    I.amOnPage("/admin/v9/templates/news/?id=111");
    DTE.waitForEditor();
    DTE.fillField("contextClasses", "");
    DTE.save();

    I.amOnPage("/zo-sveta-financii/");
    dataXls = await I.grabAttributeFrom("section.md-news-subpage.v2", "data-xls");
    I.assertEqual(dataXls, '$FileTools.getFileIcon("xls")');
    dataForum = await I.grabAttributeFrom("section.md-news-subpage.v2", "data-forum");
    I.assertEqual(dataForum, "$ForumDB.isActive($docDetails.getDocId())");

    //check contextClasses is generated into template
    I.amOnPage("/admin/v9/templates/news/?id=111");
    DTE.waitForEditor();
    DTE.fillField("contextClasses", "sk.iway.iwcm.FileTools\nsk.iway.iwcm.forum.ForumDB");
    DTE.save();

    //verify attributes are generated into page
    I.amOnPage("/zo-sveta-financii/");
    var dataXls = await I.grabAttributeFrom("section.md-news-subpage.v2", "data-xls");
    I.assertEqual(dataXls, "/components/_common/mime/xls.gif");
    var dataForum = await I.grabAttributeFrom("section.md-news-subpage.v2", "data-forum");
    I.assertEqual(dataForum, "true");
});