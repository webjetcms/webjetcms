Feature('search');

Before(({ I, login }) => {
    login('admin');
});

Scenario('search', async ({I, DT, Document}) => {

    I.amOnPage("/apps/gdpr/admin/search/");

    I.fillField("input.dt-filter-value", "test");
    I.pressKey("Enter", "input.dt-filter-value");
    DT.waitForLoader("searchDataTable");

    //Search data table
    Document.screenshot("/redactor/apps/gdpr/search-dataTable.png");


    //Search externý filter
    Document.screenshotElement("div.dt-extfilter-value", "/redactor/apps/gdpr/search-extFilter.png");
});
