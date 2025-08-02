Feature('templates');

Before(({ I, login }) => {
    login('admin');
});

Scenario('templates', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();
    Document.screenshot("/frontend/templates/templates.png");
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=7");
    DTE.waitForEditor();
    Document.screenshot("/frontend/templates/templates-edit.png");
});

Scenario('temps-groups', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    DT.waitForLoader();
    Document.screenshot("/frontend/templates/temps-groups.png");
    I.click("Demo JET");
    DTE.waitForEditor();
    Document.screenshot("/frontend/templates/temps-groups-edit.png");
});

Scenario('news', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/templates/news/");
    DT.waitForLoader();
    Document.screenshot("/frontend/templates/news/news-temps-datatable.png");

    DT.filterEquals("name", "news01");
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor();

    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/frontend/templates/news/news-temps-editor.png");

    DTE.cancel();

    I.clickCss("button.buttons-create");
    DTE.waitForEditor();
    I.rightClick("#DTE_Field_templateCode");
    I.moveCursorTo("li.dropdown-submenu.velocity");
    I.moveCursorTo(locate("a").withText("Foreach-Cyklus #Foreach"));

    Document.screenshotElement(".DTE.modal-content.DTE_Action_Create", "/frontend/templates/news-temps-editor-2.png");

    I.click(locate("a").withText("Foreach-Cyklus #Foreach"));

    Document.screenshotElement(".DTE.modal-content.DTE_Action_Create", "/frontend/templates/news-temps-editor-3.png");

    I.amOnPage("/admin/v9/settings/translation-keys/");
    DT.filterContains("key", "news.template.");
    Document.screenshot("/frontend/templates/news/news/news-temps-translation-keys.png");
});