Feature('a11y.stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('button group', async ({ I, a11y }) => {
    I.amOnPage("/apps/stat/admin/top/");
    I.wait(1);
    await a11y.check();
});

Scenario('p38: graphs contrast', async ({ I, a11y }) => {
    I.amOnPage("/apps/stat/admin/browser/");
    I.wait(1);
    //TODO: check graph contrast on hover
    await a11y.check();
});