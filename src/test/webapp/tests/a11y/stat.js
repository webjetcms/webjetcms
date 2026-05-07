Feature('a11y.stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('button group', async ({ I, DT, a11y }) => {
    I.amOnPage("/apps/stat/admin/top/");
    I.wait(1);
    await a11y.check();

    I.amOnPage("/apps/stat/admin/error/");
    I.wait(1);
    await a11y.check();

    I.amOnPage("/apps/stat/admin/search-engines/");
    DT.waitForLoader();
    I.waitForElement(locate(".datatable-column-width a").withText("searchtestresult"), 5);
    I.click("searchtestresult", ".datatable-column-width a");
    I.wait(1);
    await a11y.check();
});

Scenario('p38: graphs contrast', async ({ I, a11y }) => {
    I.amOnPage("/apps/stat/admin/browser/");
    I.wait(1);
    //TODO: check graph contrast on hover
    await a11y.check();
});