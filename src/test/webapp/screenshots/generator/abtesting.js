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