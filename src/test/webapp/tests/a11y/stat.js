Feature('a11y.stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('button group', async ({ I, a11y }) => {
    I.amOnPage("/apps/stat/admin/");
    await a11y.check();
});