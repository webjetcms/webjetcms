Feature('a11y.stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('button group', async ({ I, a11y }) => {
    I.amOnPage("/apps/stat/admin/");
    await a11y.check();
});

Scenario('p38: graphs contrast', async ({ I, a11y }) => {
    I.amOnPage("/apps/stat/admin/");
    //TODO: check graph contrast on hover
    await a11y.check();
});