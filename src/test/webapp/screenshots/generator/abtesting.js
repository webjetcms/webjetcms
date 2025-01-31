Feature('abtesting');

Before(({ I, login }) => {
    login('admin');
});

Scenario('webpages', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=30452");
    DT.waitForLoader();

    I.click('.dt-filter-id');

    //ikona
    I.moveCursorTo('button.buttons-abtest');
    Document.screenshot("/redactor/apps/abtesting/datatable.png");
    I.wjSetDefaultWindowSize();
});

Scenario('Ab testing section screenshots', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/abtesting/admin/");
    Document.screenshot("/redactor/apps/abtesting/ab_test_page_list.png");

    I.amOnPage("/apps/stat/admin/top-details/?docId=35266");
    DT.waitForLoader();
    I.wait(3);
    Document.scrollTo("#graphsDiv_3");
    Document.screenshot("/redactor/apps/abtesting/stat-percent.png");


    I.amOnPage("/apps/abtesting/admin/config/");
    Document.screenshot("/redactor/apps/abtesting/ab_test_config_page.png");

    DT.filterContains("name", "ABTestingRatio");
    I.clickCss("td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor("abtestingConfDataTable");
    Document.screenshot("/redactor/apps/abtesting/ab_test_config_editor.png");
});