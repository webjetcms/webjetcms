Feature('a11y.monitoring');

Before(({ I, login }) => {
    login('admin');
});

Scenario('top filter', async ({ I, a11y }) => {
    I.amOnPage("/apps/server_monitoring/admin/records/");
    await a11y.check();
});